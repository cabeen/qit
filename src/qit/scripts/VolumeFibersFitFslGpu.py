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

"""  fit the ball and sticks model with the GPU version of FSL bedpostx"""

from common import *

def main():
    usage="qit %s [opts]" % argv[0]
    parser = OptionParser(usage=usage, description=__doc__)

    parser.add_option("--input", metavar="<file>", \
        help="specify a dwi (required)")
    parser.add_option("--gradients", metavar="<file>", \
        help="specify a gradients file (required)")
    parser.add_option("--mask", metavar="<file>", \
        help="specify a mask (required)")
    parser.add_option("--output", metavar="<file>", \
        help="specify an output directory (required)")

    parser.add_option("--shells", metavar="<a,b,c,...>", \
        help="specify a subset of gradient shells (comma-separated)")
    parser.add_option("--which", metavar="<a,b,c,...>", \
        help="specify a subset of gradients to use (comma-separated)")
    parser.add_option("--exclude", metavar="<a,b,c,...>", \
        help="specify a subset of gradient to exclude (comma-separated)")
    parser.add_option("--clobber", \
        help="clobber existing output", action="store_true")

    parser.add_option("-Q", metavar="<value>", \
        help="specify the name of the GPU queue, default is cuda.q")
    parser.add_option("--NJOBS", metavar="<value>", \
        help="specify the number of jobs to queue, default is 4")
    parser.add_option("-n", metavar="<value>", \
        help="specify the number of fibers per voxel, default: 3")
    parser.add_option("-w", metavar="<value>", \
        help="specify the ARD weight, more weight means less secondary fibres per voxel, default 1")
    parser.add_option("-b", metavar="<value>", \
        help="specify the burnin period, default: 1000")
    parser.add_option("-j", metavar="<value>", \
        help="specify the number of jumps, default: 1250")
    parser.add_option("-s", metavar="<value>", \
        help="specify the sample every, default: 25")
    parser.add_option("--model", metavar="<value>", \
        help="specify the model, 1: with sticks, 2: with sticks with a range of diffusivities (default), 3: with zeppelins")

    (opts, pos) = parser.parse_args()

    if len(pos) != 0 or len(argv) == 1:
        parser.print_help()
        return

    Global.assume(opts.input is not None, "no dwi found")
    Global.assume(opts.gradients is not None, "no gradients found")
    Global.assume(opts.mask is not None, "no mask found")
    Global.assume(opts.output is not None, "no output found")

    Logging.info("started")

    Logging.info("reading input")
    dwi = Volume.read(opts.input)
    grads = Gradients.read(opts.gradients)

    Logging.info("generating mask")
    if opts.mask:
        mask = Mask.read(opts.mask)
    else:
        mask = MaskSource.create(dwi.getSampling(), 1)

    tmp_dir = "%s.tmp.%d" % (opts.output, int(time())) 
    Logging.info("using temporary directory %s" % tmp_dir)
    if not exists(tmp_dir):
        makedirs(tmp_dir)

    diff_dir = join(tmp_dir, "diff")
    if not exists(diff_dir):
        makedirs(diff_dir)

    if opts.which or opts.shells or opts.exclude:
      Logging.info("subsetting dwi and gradients")
      
      reducer = VolumeDwiReduce()
      reducer.dwi = dwi
      reducer.gradients = grads
      reducer.which = opts.which
      reducer.shells = opts.shells
      reducer.exclude = opts.exclude
      reducer.run()

      dwi = reducer.outputDwi
      grads = reducer.outputGradients

    dwi_fn = join(diff_dir, "data.nii.gz")
    mask_fn = join(diff_dir, "nodif_brain_mask.nii.gz")
    bvecs_fn = join(diff_dir, "bvecs")
    bvals_fn = join(diff_dir, "bvals")

    dwi.write(dwi_fn)
    mask.write(mask_fn)
    grads.write(bvecs_fn, bvals_fn)

    cmd =  ["bedpostx_gpu", diff_dir]

    if opts.Q:
      cmd += ["-Q", opts.Q]

    if opts.NJOBS:
      cmd += ["-NJOBS", opts.NJOBS]

    if opts.n:
      cmd += ["-n", opts.n]

    if opts.w:
      cmd += ["-w", opts.w]

    if opts.b:
      cmd += ["-b", opts.b]

    if opts.j:
      cmd += ["-j", opts.j]

    if opts.s:
      cmd += ["-s", opts.s]

    if opts.model:
      cmd += ["-model", opts.model]

    Logging.info("fitting ball and stick model")
    if call(cmd, shell=False):
        print("bedpostx_gpu failed")

    if exists(opts.output):
      if opts.clobber:
          Logging.info("clobbering existing results")
          rmtree(opts.output)
      else:  
          bck_dir = "%s.bck.%d" % (opts.output, int(time()))
          Logging.info("backing up existing results to %s" % bck_dir)
          move(opts.output, bck_dir)

    Logging.info("finalizing")
    move(join(tmp_dir, "diff.bedpostX"), opts.output)
    rmtree(tmp_dir)

    Logging.info("finished")

if __name__ == "__main__":
    main()
