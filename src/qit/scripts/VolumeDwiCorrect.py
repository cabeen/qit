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

"""Basic motion and eddy current correction with FSL.  This will perform affine
registration of each DWI to the baseline using mutual information and
subsequently rotate each b-vector using the rotational component of the
transform.  The gradients should be in a right-handed coordinate system."""

from common import *

def main():
    usage = "qit VolumeDwiCorrect [opts]"
    parser = OptionParser(usage=usage, description=__doc__)

    parser.add_option("--input-dwi", metavar="<fn>", \
        help="specify an input dwi")
    parser.add_option("--input-bvecs", metavar="<fn>", \
        help="specify input b-vectors")
    parser.add_option("--input-bvals", metavar="<fn>", \
        help="specify input b-values")
    parser.add_option("--output-dwi", metavar="<fn>", \
        help="specify an output dwi")
    parser.add_option("--output-bvecs", metavar="<fn>", \
        help="specify output b-vectors")
    parser.add_option("--output-bvals", metavar="<fn>", \
        help="specify output b-values")
    parser.add_option("--script", metavar="<fn>", default="eddy_correct", \
        help="specify a path to the motion correction script")
    parser.add_option("--keep", \
        help="keep intermediate files", action="store_true")

    (opts, pos) = parser.parse_args()

    if len(pos) != 0 or len(argv) == 1:
        parser.print_help()
        return

    if not opts.input_dwi or not opts.output_dwi:
        Logging.error("input and output dwi must be specified")
    if not opts.input_bvecs or not opts.output_bvecs:
        Logging.error("input and output bvecs must be specified")
    if not opts.input_bvals or not opts.output_bvals:
        Logging.error("input and output bvals must be specified")

    Logging.info("started")
    tmp_dir = join(dirname(opts.output_dwi), "ecc.tmp.%d" % int(time()))
    if not exists(tmp_dir):
        makedirs(tmp_dir)
    Logging.info("using temporary directory %s" % tmp_dir)

    dwi = Volume.read(opts.input_dwi)
    if not opts.input_dwi.endswith("nii.gz"):
        Logging.info("importing diffusion volume")
        import_dwi = join(tmp_dir, "input.nii.gz")
        dwi.write(import_dwi)
    else:
        import_dwi = opts.input_dwi

    Logging.info("importing gradients")
    grads = Gradients.read(dwi.getDim(), opts.input_bvecs, opts.input_bvals)
    grads.bvecs.write(join(tmp_dir, "input_bvecs.txt"))
    VectsUtils.transpose(grads.bvecs).write(join(tmp_dir, "input_fsl_bvecs.txt"))

    VectsUtils.transpose(grads.bvals).write(join(tmp_dir, "fsl_bvals.txt"))
    grads.bvals.write(join(tmp_dir, "bvals.txt"))

    Logging.info("running FSL eddy_correct")
    cmd = [opts.script, import_dwi, join(tmp_dir, "dwi.nii.gz"), "0"]
    Logging.info("cmd: %s" % " ".join(cmd))
    if call(cmd, shell=False):
        Logging.error("failed to execute eddy_correct")

    Logging.info("running FSL rotbvecs")
    ecclog = join(tmp_dir, "dwi.ecclog")
    script = join(Global.getRoot(), "share", "fsl", "rotbvecs")
    cmd = ["bash", script, join(tmp_dir, "input_fsl_bvecs.txt"), \
           join(tmp_dir, "fsl_bvecs.txt"), ecclog]
    if call(cmd, shell=False):
        Logging.error("failed to execute rotbvecs")

    Logging.info("transposing gradients")
    VectsUtils.transpose(Vects.read(join(tmp_dir, "fsl_bvecs.txt")))\
        .write(join(tmp_dir, "bvecs.txt"))

    Logging.info("cleaning up")
    move(join(tmp_dir, "dwi.nii.gz"), opts.output_dwi)
    move(join(tmp_dir, "bvecs.txt"), opts.output_bvecs)
    move(join(tmp_dir, "bvals.txt"), opts.output_bvals)

    if not opts.keep:
        rmtree(tmp_dir)

    Logging.info("finished")

if __name__ == "__main__":
    main()
