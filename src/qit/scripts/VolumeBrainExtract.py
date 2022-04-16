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

"""brain extraction with FSL"""

from common import *

def main():
    parser = OptionParser(usage="qit bet [opts]", description=__doc__)

    parser.add_option("--input", metavar="<fn>", help="the input volume")
    parser.add_option("--output", metavar="<fn>", help="the output mask")
    parser.add_option("--frac", metavar="<val>", help="the fraction threshold")

    (opts, pos) = parser.parse_args()

    if len(pos) != 0 or len(args) == 1:
        parser.print_help()
        return

    Logging.info("started")

    if not opts.input or not opts.output:
        Logging.error("input and output must be specified")

    in_vol = opts.input
    out_fn = opts.output

    tmp_id = int(time())
    tmp_dir = join(dirname(out_fn), "bet.tmp.%d" % int(time()))
    if not exists(tmp_dir):
        makedirs(tmp_dir)
    Logging.info("using temporary directory %s" % tmp_dir)

    if not in_vol.endswith("nii.gz"):
        Logging.info("importing diffusion volume to nifti")
        vol = Volume.read(in_vol)
        import_vol = join(tmp_dir, "bet_vol.nii.gz")
        vol.write(import_vol)
    else:
        import_vol = in_vol

    Logging.info("running FSL bet")
    bet_bn = join(tmp_dir, "brain")
    cmd = ["bet", import_vol, bet_bn, "-m"]
    if opts.frac:
        cmd += ["-f", opts.frac]
    if call(cmd, shell=False):
        Logging.error("failed to execute bet")

    Logging.info("cleaning up")
    move("%s_mask.nii.gz" % bet_bn, out_fn)
    rmtree(tmp_dir)

    Logging.info("finished")

if __name__ == "__main__":
    main()
