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

"""  Register diffusion tensor images with DTI-TK.  Note: diffusivities are
assumed to be 10^-3 mm^2 s^-1.  Correction factors must be set when this is not
the case.  The default scaling is 1000 to support FSL style units."""

from common import *

def main():
    usage = "qit TensorVolumeRegister [opts] in_dir target_dir out_dir"
    parser = OptionParser(usage=usage, description=__doc__)

    parser.add_option("--mask", metavar="<fn>", \
        help="specify a target volume mask")
    parser.add_option("--scale", metavar="<val>", default="1000.0", \
        help="specify an input diffusivity scaling")
    parser.add_option("--target-scale", metavar="<val>", default="1000.0", \
        help="specify a target diffusivity scaling")
    parser.add_option("--outlier", metavar="<val>", default="100", \
        help="specify a threshold for outlier detection")
    parser.add_option("--affine", action="store_true", \
        help="peform affine registration (default is rigid)")
    parser.add_option("--intermediate", action="store_true", \
        help="keep intermediate files")
    parser.add_option("--thresh", metavar="<thresh>", default="0.15", \
        help="the fa threshold for tracking")

    (opts, pos) = parser.parse_args()

    if len(pos) != 3:
        parser.print_help()
        return

    Logging.info("started")

    in_dir, target_dir, out_dir = pos
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

    mask = join(itmp_dir, "mask.nii.gz")
    target = join(itmp_dir, "target.nii.gz")
    target_norm = join(itmp_dir, "target_norm.nii.gz")
    target_filt = join(itmp_dir, "target_filt.nii.gz")
    target_tr = join(itmp_dir, "target_tr.nii.gz")
    tensors = join(itmp_dir, "tensors.nii.gz")
    tensors_norm = join(itmp_dir, "tensors_norm.nii.gz")
    tensors_filt = join(itmp_dir, "tensors_filt.nii.gz")
    tensors_aff = join(itmp_dir, "tensors_aff.nii.gz")
    tensors_diffeo = join(itmp_dir, "tensors_aff_diffeo.nii.gz")
    tensors_combined = join(itmp_dir, "tensors_combined.nii.gz")
    xfm_aff = join(itmp_dir, "tensors.aff")
    xfm_diffeo = join(itmp_dir, "tensors_aff_diffeo.df.nii.gz")
    xfm_combined = join(itmp_dir, "tensors_combined.df.nii.gz")
    xfm_aff_inv = join(itmp_dir, "tensors_inv.aff")
    xfm_diffeo_inv = join(itmp_dir, "tensors_aff_diffeo.df_inv.nii.gz")
    xfm_combined_inv = join(itmp_dir, "tensors_combined.df_inv.nii.gz")
    xfm_diffeo_jac = join(itmp_dir, "tensors_aff_diffeo.df_jac.nii.gz")
    xfm_diffeo_inv_jac = join(itmp_dir, "tensors_aff_diffeo.df_inv_jac.nii.gz")
    tensors_out = "%s.nii.gz" % tmp_bn

    cmds = []

    cmd = ["TVFromEigenSystem", "-basename", join(in_dir, "dti")]
    cmd += ["-out", tensors, "-type", "FSL"]
    cmds.append(("importing tensors", cmd))

    cmd = ["TVAdjustVoxelspace", "-origin", "0", "0", "0"]
    cmd += ["-in", tensors, "-out", tensors]
    cmds.append(("removing origin", cmd))

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

    cmd = ["TVFromEigenSystem", "-basename", join(target_dir, "dti")]
    cmd += ["-out", target, "-type", "FSL"]
    cmds.append(("importing target tensors", cmd))

    cmd = ["TVtool", "-in", target, "-scale", opts.target_scale, "-out", target]
    cmds.append(("scaling diffusivity", cmd))

    cmd = ["TVtool", "-in", target, "-norm"]
    cmds.append(("computing tensor norm", cmd))

    cmd = ["BinaryThresholdImageFilter", target_norm, target_filt]
    cmd += ["0", opts.outlier, "1", "0"]
    cmds.append(("computing norm filter", cmd))

    cmd = ["TVtool", "-in", target, "-mask", target_filt, "-out", target]
    cmds.append(("filtering outliers", cmd))

    cmd = ["TVtool", "-in", target, "-spd", "-out", target]
    cmds.append(("converting tensors to semi-positive definite", cmd))

    if opts.mask:
        cmds.append(("importing mask", ["cp", opts.mask, mask]))
    else:
        cmd = ["TVtool", "-in", target, "-tr"]
        cmds.append(("computing trace", cmd))

        cmd = ["BinaryThresholdImageFilter", target_tr, mask]
        cmd += ["0.01", "100", "1", "0"]
        cmds.append(("making mask", cmd))

    cmd = ["dti_rigid_reg", target, tensors, "EDS", "4", "4", "4", "0.01"]
    cmds.append(("performing rigid registration", cmd))

    if opts.affine:
			cmd = ["dti_affine_reg", target, tensors, "EDS"]
			cmd += ["4", "4", "4", "0.01", "1", "inTrans"]
			cmds.append(("performing affine registration", cmd))

    cmd = ["affineSymTensor3DVolume", "-in", tensors, "-trans"]
    cmd += [xfm_aff, "-target", target, "-out", tensors_combined] 
    cmds.append(("remapping tensors with transform", cmd))

    cmd = ["affine3Dtool", "-in", xfm_aff, "-invert", "-out", xfm_aff_inv] 
    cmds.append(("inverting affine warp", cmd))

    cmd = ["cp", tensors_combined, tensors_out]
    cmds.append(("copying transformed tensors", cmd))

    invscale = str(1.0 / float(opts.scale))
    cmd = ["TVtool", "-in", tensors_out, "-scale", invscale]
    cmd += ["-out", tensors_out]
    cmds.append(("scaling diffusivity", cmd))

    cmd = ["TVEigenSystem", "-in", tensors_out, "-type", "FSL"]
    cmds.append(("exporting tensors to eigen system", cmd))

    for cmd in cmds:
        Logging.info(cmd[0])
        if call(cmd[1], shell=False):
            Logging.error("failed to run %s" % cmd[0])

    ref = Volume.read(baseline)
    rsamp = ref.getSampling()

    stdize = VolumeStandardize()
    stdize.input = ref
    stdize.run()
    cxfm = stdize.xfm
    cinvxfm = stdize.invxfm
    cxfm.write(join(tmp_dir, "coord.xfm"))
    cinvxfm.write(join(tmp_dir, "coord_inv.xfm"))

    tensors = Volume.read(tmp_dir)
    mask = Mask.read(mask)

    out = {}
    for name in Tensor.FEATURES: 
        Logging.info("computing %s" % name)
        func = ModelUtils.feature(Tensor(), name)
        out[name] = VolumeFunction(func).withInput(tensors).withMask(mask).run()

    Logging.info("thresholding fa")
    func = VectFunctionSource.thresh(float(opts.thresh))
    fa = out[Tensor.FEATURES_FA]
    out["track"] = VolumeFunction(func).withInput(fa).run()

    for name in out.keys():
        fn = join(tmp_dir, "dti_%s.nii.gz" % name)
        if not exists(fn):
            Logging.info("writing %s" % name)
            out[name].write(fn)

    move(xfm_aff, join(tmp_dir, "xfm.aff"))
    move(xfm_aff_inv, join(tmp_dir, "invxfm.aff"))

    Logging.info("cleaning up")
    remove(tensors_out)
    if not opts.intermediate:
        rmtree(itmp_dir) 
    move(tmp_dir, out_dir)

    Logging.info("finished")

if __name__ == "__main__":
    main()
