#! /usr/bin/env qit
################################################################################
#
# Quantitative Imaging Toolkit (QIT) (c) 2012-2022 Ryan Cabeen
# All rights reserved.
#
# The Software remains the property of Ryan Cabeen ("the Author").
#
# The Software is distributed "AS IS" under this Licence solely for
# non-commercial use in the hope that it will be useful, but in order
# that the Author as a charitable foundation protects its assets for
# the benefit of its educational and research purposes, the Author
# makes clear that no condition is made or to be implied, nor is any
# warranty given or to be implied, as to the accuracy of the Software,
# or that it will be suitable for any particular purpose or for use
# under any specific conditions. Furthermore, the Author disclaims
# all responsibility for the use which is made of the Software. It
# further disclaims any liability for the outcomes arising from using
# the Software.
#
# The Licensee agrees to indemnify the Author and hold the
# Author harmless from and against any and all claims, damages and
# liabilities asserted by third parties (including claims for
# negligence) which arise directly or indirectly from the use of the
# Software or the sale of any products based on the Software.
#
# No part of the Software may be reproduced, modified, transmitted or
# transferred in any form or by any means, electronic or mechanical,
# without the express permission of the Author. The permission of
# the Author is not required if the said reproduction, modification,
# transmission or transference is done without financial return, the
# conditions of this Licence are imposed upon the receiver of the
# product, and all original and amended source code is included in any
# transmitted product. You may be held legally responsible for any
# copyright infringement that is caused or encouraged by your failure to
# abide by these terms and conditions.
#
# You are not permitted under this Licence to use this Software
# commercially. Use for which any financial return is received shall be
# defined as commercial use, and includes (1) integration of all or part
# of the source code or the Software into a product for sale or license
# by or on behalf of Licensee to third parties or (2) use of the
# Software or any derivative of it for research with the final aim of
# developing software products for sale or license to a third party or
# (3) use of the Software or any derivative of it for research with the
# final aim of developing non-software products for sale or license to a
# third party, or (4) use of the Software to provide any service to an
# external organisation for which payment is received.
#
################################################################################

"""  Transform a diffusion tensor volume."""

from common import *

def main():
    usage = "qit TensorVolumeTransform [opts] input xfm target output"
    parser = OptionParser(usage=usage, description=__doc__)

    parser.add_option("--affine", action="store_true", \
        help="perform affine registration")
    parser.add_option("--scale", metavar="<val>", default="1000.0", \
        help="specify an input diffusivity scaling")
    parser.add_option("--outlier", metavar="<val>", default="100", \
        help="specify a threshold for outlier detection")
    parser.add_option("--intermediate", action="store_true", \
        help="keep intermediate files")
    parser.add_option("--thresh", metavar="<thresh>", default="0.15", \
        help="the fa threshold for tracking")

    (opts, pos) = parser.parse_args()

    if len(pos) != 4:
        parser.print_help()
        return

    Logging.info("started")

    in_dir, xfm, target, out_dir = pos
    tmp_dir = "%s.tmp.%d.dti" % (out_dir, int(time()))
    tmp_bn = join(tmp_dir, "dti")
    itmp_dir = join(tmp_dir, "intermediate")

    if not exists(tmp_dir):
        makedirs(tmp_dir)

    if not exists(itmp_dir):
        makedirs(itmp_dir)

    Logging.info("using temporary directory %s" % tmp_dir)

    baseline = join(in_dir, "dti_S0.nii.gz")
    baseline_out = join(tmp_dir, "dti_S0.nii.gz")

    rmse = join(in_dir, "dti_RMSE.nii.gz")
    rmse_out = join(tmp_dir, "dti_RMSE.nii.gz")

    tensors = join(itmp_dir, "tensors.nii.gz")
    tensors_norm = join(itmp_dir, "tensors_norm.nii.gz")
    tensors_filt = join(itmp_dir, "tensors_filt.nii.gz")
    tensors_aff = join(itmp_dir, "tensors_aff.nii.gz")
    tensors_diffeo = join(itmp_dir, "tensors_aff_diffeo.nii.gz")
    tensors_combined = join(itmp_dir, "tensors_combined.nii.gz")
    tensors_out = "%s.nii.gz" % tmp_bn

    cmds = []

    cmd = ["TVFromEigenSystem", "-basename", join(in_dir, "dti")]
    cmd += ["-out", tensors, "-type", "FSL"]
    cmds.append(("importing tensors", cmd))

    cmd = ["TVtool", "-in", tensors, "-scale", opts.scale]
    cmd += ["-out", tensors]
    cmds.append(("scaling diffusivity", cmd))

    cmd = ["TVtool", "-in", tensors, "-norm"]
    cmds.append(("computing tensor norm", cmd))

    cmd = ["BinaryThresholdImageFilter", tensors_norm, tensors_filt]
    cmd += ["0", opts.outlier, "1", "0"]
    cmds.append(("computing norm filter", cmd))

    cmd = ["TVtool", "-in", tensors, "-mask", tensors_filt, "-out", tensors]
    cmds.append(("filtering outliers", cmd))

    cmd = ["TVtool", "-in", tensors, "-spd", "-out", tensors]
    cmds.append(("converting tensors to semi-positive definite", cmd))

    if opts.affine:
        if xfm.endswith("xfm"):
          lines = [line.strip().split(" ")  for line in open(xfm)]
          xfm = join(itmp_dir, "xfm.aff")
          f = open(xfm, "w")
          f.write("MATRIX\n")
          f.write("%s\n" % " ".join(lines[0][0:3]))
          f.write("%s\n" % " ".join(lines[1][0:3]))
          f.write("%s\n" % " ".join(lines[2][0:3]))
          f.write("VECTOR\n")
          f.write("%s %s %s\n" % (lines[0][3], lines[1][3], lines[2][3]))
          f.close()

        cmd = ["affineSymTensor3DVolume", "-in", tensors, "-trans"]
        cmd += [xfm, "-target", target, "-out", tensors_out] 
        cmds.append(("remapping tensors with affine", cmd))

        cmd = ["affineScalarVolume", "-in", baseline, "-trans"]
        cmd += [xfm, "-target", target, "-out", baseline_out] 
        cmds.append(("remapping baseline with affine", cmd))

        if exists(rmse):
            cmd = ["affineScalarVolume", "-in", rmse, "-trans"]
            cmd += [xfm, "-target", target, "-out", rmse_out] 
            cmds.append(("remapping rmse with affine", cmd))

    else:
        cmd = ["deformationSymTensor3DVolume", "-in", tensors, "-trans"]
        cmd += [xfm, "-target", target, "-out", tensors_out] 
        cmds.append(("remapping tensors with deformation", cmd))

        cmd = ["deformationScalarVolume", "-in", baseline, "-trans"]
        cmd += [xfm, "-target", target, "-out", baseline_out] 
        cmds.append(("remapping baseline with deformation", cmd))

        if exists(rmse):
            cmd = ["deformationScalarVolume", "-in", rmse, "-trans"]
            cmd += [xfm, "-target", target, "-out", rmse_out] 
            cmds.append(("remapping rmse with deformation", cmd))

    invscale = str(1.0 / float(opts.scale))
    cmd = ["TVtool", "-in", tensors_out, "-scale", invscale]
    cmd += ["-out", tensors_out]
    cmds.append(("scaling diffusivity", cmd))

    cmd = ["TVEigenSystem", "-in", tensors_out, "-type", "FSL"]
    cmds.append(("exporting tensors to eigen system", cmd))

    for cmd in cmds:
        Logging.info(cmd[0])
        Logging.info(" ".join(cmd[1]))
        if call(cmd[1], shell=False):
            Logging.error("failed to run %s" % cmd[0])

    tensors = Volume.read(tmp_dir)
    out = {}
    for name in Tensor.FEATURES: 
        Logging.info("computing %s" % name)
        func = ModelUtils.feature(Tensor(), name)
        out[name] = VolumeFunction(func).withInput(tensors).run()

    Logging.info("thresholding fa")
    func = VectFunctionSource.thresh(float(opts.thresh))
    fa = out[Tensor.FEATURES_FA]
    out["track"] = VolumeFunction(func).withInput(fa).run()

    for name in out.keys():
        fn = join(tmp_dir, "dti_%s.nii.gz" % name)
        if not exists(fn):
            Logging.info("writing %s" % name)
            out[name].write(fn)

    Logging.info("cleaning up")
    remove(tensors_out)
    if not opts.intermediate:
        rmtree(itmp_dir) 
    move(tmp_dir, out_dir)

    Logging.info("finished")

if __name__ == "__main__":
    main()
