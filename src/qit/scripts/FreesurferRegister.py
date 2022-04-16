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

"""register freesurfer data to diffusion space"""

from common import *

def main():
    parser = OptionParser(usage="qit FreesurferRegister [opts]", description=__doc__)

    parser.add_option("--input", metavar="<dir>", \
        help="the imported freesurfer dir")
    parser.add_option("--target", metavar="<fn>", \
        help="the target volume")
    parser.add_option("--output", metavar="<dir>", \
        help="the output directory")
    parser.add_option("--interp", metavar="<name>", \
        default="Trilinear", help="the interpolation method")
    parser.add_option("--cost", metavar="<name>", default="mutualinfo", \
        help="the registration metric (see flirt)")
    parser.add_option("--dof", metavar="<name>", default="6", \
        help="the transform degrees of freedom (see flirt)")
    parser.add_option("--bins", metavar="<num>", default="256", \
        help="the number of bins (see flirt)")

    (opts, pos) = parser.parse_args()

    if len(pos) != 0 or len(args) == 1:
        parser.print_help()
        return

    if not opts.input:
        Logging.error("no input specified")

    if not opts.target:
        Logging.error("no target specified")

    if not opts.output:
        Logging.error("no output specified")

    Logging.info("started")

    tmp_dir = "%s.tmp.%d" % (opts.output, int(time())) 
    if not exists(tmp_dir):
        makedirs(tmp_dir)
    Logging.info("using temporary directory %s" % tmp_dir)

    diff_fn = opts.target
    fs_fn = opts.input

    fsl_diff2tone_fn = join(tmp_dir, "fsl_diff2tone.xfm")

    Logging.info("computing diff2tone registration")
    cmd = ["flirt"]
    cmd += ["-cost", opts.cost]
    cmd += ["-dof", opts.dof, "-bins", opts.bins]
    cmd += ["-in", diff_fn, "-usesqform"]
    cmd += ["-ref", fs_fn, "-omat", fsl_diff2tone_fn]
    cmd += ["-searchrx", "-180", "180"]
    cmd += ["-searchry", "-180", "180"]
    cmd += ["-searchrz", "-179", "180"]
    if call(cmd, shell=False):
        Logging.error("failed to compute diff2tone transform")

    # it's hard to tell how FSL stores transforms, so let's be sure...
    Logging.info("mapping image coordinates")
    coords_fn = join(Global.getRoot(), "share", "fsl", "coords.txt")
    cmd = ["img2imgcoord", coords_fn, "-mm"]
    cmd += ["-src", diff_fn, "-dest", fs_fn, "-xfm", fsl_diff2tone_fn]
    pipe = subprocess.Popen(cmd, stdout=subprocess.PIPE)
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

    diff2tone = Matrix([row0, row1, row2, row3])

    Logging.info("writing transforms")
    diff2tone.write(join(tmp_dir, "diff2tone.xfm"))
    diff2tone.inv().write(join(tmp_dir, "tone2diff.xfm"))

    Logging.info("registering freesurfer")
    xfm = Affine(diff2tone)
    ref = Volume.read(diff_fn).getSampling()
    vol = Volume.read(fs_fn)
    func = xfm.compose(VolumeUtils.interp(opts.interp, vol))
    VolumeSample()\
        .withSampling(ref)\
        .withFunction(func)\
        .getOutput()\
        .write(join(tmp_dir, "brain.nii.gz"))

    Logging.info("finalizing")
    move(tmp_dir, opts.output)

    Logging.info("finished")

if __name__ == "__main__":
    main()
