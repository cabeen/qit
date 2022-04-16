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

"""  a catch-all converter for exchanging data between packages"""

from common import *
import subprocess

def main():
    parser = OptionParser(usage="qit volume [opts] [input_fn(s)]", description=__doc__)

    parser.add_option("--dtitk", action="store_true", \
      help="convert dti results to the dti-tk format")
    parser.add_option("--line-fsl", action="store_true", \
      help="convert a line image to the FSL format")
    parser.add_option("--xfib-fsl", action="store_true", \
      help="convert lines in an fibers directory")
    parser.add_option("--xfm-fsl", action="store_true", \
      help="convert an xfm from the FSL format")
    parser.add_option("--set-forms", action="store_true", \
      help="set the nifti forms")
    parser.add_option("--voxel-table", action="store_true", \
      help="set the nifti forms")
    parser.add_option("--bvecs-camino", action="store_true", \
      help="convert camino bvecs")
    parser.add_option("--bvecs-fsl", action="store_true", \
      help="convert fsl bvecs")

    (opts, pos) = parser.parse_args()

    if len(pos) == 0:
        parser.print_help()
        return

    Logging.info("started")  

    if opts.bvecs_camino:
        in_fn = pos[0]
        out_fn = pos[1]

        lines = [line.strip() for line in open(in_fn)]
        num = int(lines[0])

        out = open(out_fn, "w")
        for i in range(num):
            x = float(lines[3 * i + 1])
            y = float(lines[3 * i + 2])
            z = float(lines[3 * i + 3])
            out.write("%g,%g,%g\n" % (x, y, z))
        out.close()
    elif opts.bvecs_fsl:
        in_fn = pos[0]
        out_fn = pos[1]

        lines = [line.strip().split() for line in open(in_fn)]
        num = len(lines[0])

        out = open(out_fn, "w")
        for i in range(num):
            x = float(lines[0][i])
            y = float(lines[1][i])
            z = float(lines[2][i])
            out.write("%g,%g,%g\n" % (x, y, z))
        out.close()

    elif opts.xfib_fsl:
        for d in pos:
            for i in ["1", "2", "3"]:
                for bn in ["dyads%s" % i, "lines%s" % i]:
                    in_fn = join(d, bn + ".nii.gz")
                    out_fn = join(d, bn + ".fsl.nii.gz")
                    if exists(in_fn):
                        Logging.info("reading input")
                        volume = Volume.read(in_fn)
                        Logging.info("transforming")
                        func = VectFunctionSource.scale(-1, 1, 1).compose(VectFunctionSource.normalize())
                        volume = VolumeFunction(func).withInput(volume).run()
                        volume.write(out_fn)
                        Logging.info("writing output")
    elif opts.line_fsl:
        for in_fn in pos:
            bn = in_fn.split(".nii")[0]
            out_fn = bn + ".fsl.nii.gz"
            if exists(in_fn):
                Logging.info("reading input")
                volume = Volume.read(in_fn)
                Logging.info("transforming")
                func = VectFunctionSource.scale(-1, 1, 1).compose(VectFunctionSource.normalize())
                volume = VolumeFunction(func).withInput(volume).run()
                volume.write(out_fn)
                Logging.info("writing output")
    elif opts.xfm_fsl:
        src_fn = pos[0]
        dest_fn = pos[1]
        xfm_fn = pos[2]
        Logger.error("fix the below line!")
        coords_fn = join(Global.getRoot(), "share", "fsl", "coords.txt")
        out_fn = pos[3]

        Logging.info("mapping image coordinates")
        arglist = ["img2imgcoord", coords_fn, "-mm"]
        arglist += ["-src", src_fn, "-dest", dest_fn, "-xfm", xfm_fn]
        pipe = subprocess.Popen(arglist, stdout=subprocess.PIPE)
        lines = pipe.stdout.readlines()

        Logging.info("parsing transformed coordinates")
        p0 = map(float, lines[1].strip().split())
        p1 = map(float, lines[2].strip().split())
        p2 = map(float, lines[3].strip().split())
        p3 = map(float, lines[4].strip().split())

        Logging.info("computing world coordinate transform")
        m03 = p0[0]
        m13 = p0[1]
        m23 = p0[2]
        m00 = p1[0] - p0[0]
        m10 = p1[1] - p0[1]
        m20 = p1[2] - p0[2]
        m01 = p2[0] - p0[0]
        m11 = p2[1] - p0[1]
        m21 = p2[2] - p0[2]
        m02 = p3[0] - p0[0]
        m12 = p3[1] - p0[1]
        m22 = p3[2] - p0[2]

        row0 = [m00, m01, m02, m03] 
        row1 = [m10, m11, m12, m13] 
        row2 = [m20, m21, m22, m23] 
        row3 = [0, 0, 0, 1]

        mat = [row0, row1, row2, row3]

        Logging.info("writing world coordinate transform")
        f = open(out_fn, "w")
        for row in mat:
            f.write("%s\n" % " ".join(map(str, row)))
        f.close()
    elif opts.voxel_table:
        mask = None
        if "--mask" in pos:
            Logging.info("reading mask")
            idx = pos.index("--mask")
            mask = Mask.read(pos[idx+1])
            del pos[idx]
            del pos[idx]

        vol_fns = pos[0:-1]
        out_fn = pos[-1]

        header = ["index"]
        vols = [] 
        for fn in vol_fns:
            Logging.info("reading %s" % fn)
            header.append(fn)
            vols.append(Volume.read(fn))

        size = vols[0].getSampling().size()
        step = size / 50
       
        Logging.info("writing table %s" % out_fn)
        f = open(out_fn, "w")
        f.write(",".join(header) + "\n")
        rcount = 0
        for idx in range(size):
            if not mask or mask.get(idx, 0) > 1e-3:
                row = [str(idx)]    
                for vol in vols:
                    row.append(str(vol.get(idx, 0)))
                f.write(",".join(row) + "\n")
                rcount += 1
                if idx % step == 0:
                    Logging.info("... wrote %d of %d voxels" % (idx,size)) 
        Logging.info("included %d voxels" % rcount) 
        f.close()
    elif opts.dtitk:
        dti_bn = pos[0]
        scale = pos[1]
        tensors = pos[2]
        thresh = "100"

        tensors_norm = join(dirname(tensors), basename(tensors)[:-7] + "_norm.nii.gz")
        tensors_filt = join(dirname(tensors), basename(tensors)[:-7] + "_filt.nii.gz")
       
        cmds = [] 

        cargs = ["TVFromEigenSystem", "-basename", dti_bn]
        cargs += ["-out", tensors, "-type", "FSL"]
        cmds.append(("importing tensors", cargs))

        cargs = ["TVtool", "-in", tensors, "-scale", scale, "-out", tensors]
        cmds.append(("scaling diffusivity", cargs))

        cargs = ["TVtool", "-in", tensors, "-norm"]
        cmds.append(("computing tensor norm", cargs))

        cargs = ["BinaryThresholdImageFilter", tensors_norm]
        cargs += [tensors_filt, "0", thresh, "1", "0"]
        cmds.append(("computing norm filter", cargs))

        cargs = ["TVtool", "-in", tensors, "-mask", tensors_filt, "-out", tensors]
        cmds.append(("filtering outliers", cargs))

        cargs = ["TVtool", "-in", tensors, "-spd", "-out", tensors]
        cmds.append(("converting tensors to semi-positive definite", cargs))

        for cmd in cmds:
            Logging.info(cmd[0])
            if call(cmd[1], shell=False):
                Logging.error("failed when s" % cmd[0])

    Logging.info("finished")

if __name__ == "__main__":
    main()
