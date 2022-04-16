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

"""  export a tensor volume to a different format (typically DTI-TK)"""

from common import *

def main():
    usage="qit %s [opts]" % argv[0]
    parser = OptionParser(usage=usage, description=__doc__)

    parser.add_option("--input", metavar="<file>", \
        help="specify an input tensor volume")
    parser.add_option("--output", metavar="<dir>", \
        help="specify an output tensor volume")
    parser.add_option("--thresh", metavar="<float>", default="100", \
        help="specify a threshold for tensor cleaning")
    parser.add_option("--scale", metavar="<float>", default="1.0", \
        help="specify a scale for converting tensors (a multiplicative factor to match the diffusivity units of a template image, for example)")

    (opts, pos) = parser.parse_args()

    if len(pos) != 0 or len(argv) == 1:
        parser.print_help()
        return

    Logging.info("started")

    Global.assume(opts.input is not None, "no input found")
    Global.assume(opts.output is not None, "no output found")

    Logging.info("reading input")
    volume = Volume.read(opts.input)

    tmp_dir = abspath("%s.tmp.%d" % (opts.output, int(time())))
    Logging.info("using temporary directory %s" % tmp_dir)

    if not exists(tmp_dir):
        makedirs(tmp_dir)

    Logging.info("writing input tensors to FSL-style format")
    volume.write(join(tmp_dir, "models.dti"))

    dti_bn = join(tmp_dir, "models.dti", "dti")
    tensors = join(tmp_dir, "tensors.nii.gz")
    tensors_norm = join(tmp_dir, "tensors_norm.nii.gz")
    tensors_filt = join(tmp_dir, "tensors_filt.nii.gz")

    cmds = []

    cargs = ["TVFromEigenSystem", "-basename", dti_bn]
    cargs += ["-out", tensors, "-type", "FSL"]
    cmds.append(("importing tensors", cargs))

    cargs = ["TVtool", "-in", tensors, "-scale", opts.scale, "-out", tensors]
    cmds.append(("scaling diffusivity", cargs))

    cargs = ["TVtool", "-in", tensors, "-norm"]
    cmds.append(("computing tensor norm", cargs))

    cargs = ["BinaryThresholdImageFilter", tensors_norm]
    cargs += [tensors_filt, "0", opts.thresh, "1", "0"]
    cmds.append(("computing norm filter", cargs))

    cargs = ["TVtool", "-in", tensors, "-mask", tensors_filt, "-out", tensors]
    cmds.append(("filtering outliers", cargs))

    cargs = ["TVtool", "-in", tensors, "-spd", "-out", tensors]
    cmds.append(("converting tensors to semi-positive definite", cargs))

    for cmd in cmds:
        Logging.info(cmd[0])
        if call(cmd[1], shell=False):
            Logging.error("failed when s" % cmd[0])

    Logging.info("cleaning up")
    move(join(tmp_dir, "tensors.nii.gz"), opts.output)
    rmtree(tmp_dir)

    Logging.info("finished")

if __name__ == "__main__":
    main()
