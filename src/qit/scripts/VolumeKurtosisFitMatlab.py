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

"""  fit a diffusion kurtosis model using the package at https://github.com/NYU-DiffusionMRI/Diffusion-Kurtosis-Imaging"""

from common import *

def main():
    usage="qit %s [opts]" % argv[0]
    parser = OptionParser(usage=usage, description=__doc__)

    parser.add_option("--input", metavar="<file>", \
        help="specify an input DWI (required)")
    parser.add_option("--gradients", metavar="<file>", \
        help="specify the input gradients (required)")
    parser.add_option("--mask", metavar="<file>", \
        help="specify a mask (required)")
    parser.add_option("--output", metavar="<fn>", \
        help="specify an output kurtosis volume (required)")
    parser.add_option("--shells", metavar="<a,b,c,...>", \
        help="specify a subset of gradient shells (comma-separated)")
    parser.add_option("--which", metavar="<a,b,c,...>", \
        help="specify a subset of gradients to use (comma-separated)")
    parser.add_option("--exclude", metavar="<a,b,c,...>", \
        help="specify a subset of gradients to exclude (comma-separated)")
    parser.add_option("--noflip", \
        help="do not flip gradients in x", action="store_true")
    parser.add_option("--save", \
        help="save the temporary files", action="store_true")
    parser.add_option("--clobber", \
        help="clobber existing output", action="store_true")

    (opts, pos) = parser.parse_args()

    if len(pos) != 0 or len(argv) == 1:
        parser.print_help()
        return

    Logging.info("started")

    Global.assume(opts.input is not None, "no input found")
    Global.assume(opts.gradients is not None, "no gradients found")
    Global.assume(opts.output is not None, "no output found")

    Logging.info("reading input")
    dwi = Volume.read(opts.input)
    grads = Gradients.read(opts.gradients)

    Logging.info("generating mask")
    if opts.mask:
        mask = Mask.read(opts.mask)
    else:
        mask = MaskSource.create(dwi.getSampling(), 1)

    output = abspath(opts.output)
    tmp_dir = abspath("%s.tmp.%d" % (opts.output, int(time())))
    Logging.info("using temporary directory %s" % tmp_dir)

    if not exists(tmp_dir):
        makedirs(tmp_dir)

    if opts.which or opts.shells or opts.exclude:
      Logging.info("subsetting dwi and gradients")
      
      reducer = VolumeDwiReduce()
      reducer.dwi = dwi
      reducer.gradients = grads
      reducer.shells = opts.shells
      reducer.which = opts.which
      reducer.exclude = opts.exclude
      reducer.run()

      dwi = reducer.outputDwi
      grads = reducer.outputGradients

    Logging.info("writing data")

    script_fn = join(tmp_dir, "run.sh")
    dwi_fn = join(tmp_dir, "dwi.nii.gz")
    mask_fn = join(tmp_dir, "mask.nii.gz")
    bvecs_fn = join(tmp_dir, "bvecs.txt")
    bvals_fn = join(tmp_dir, "bvals.txt")

    # Mrtrix expects FSL style gradients
    dwi.write(dwi_fn)
    mask.write(mask_fn)

    bvecs = grads.getBvecs()
    if not opts.noflip: 
      bvecs = VectsUtils.apply(bvecs, VectFunctionSource.scale(-1.0, 1.0, 1.0))
    bvecs.write(bvecs_fn)
    grads.getBvals().write(bvals_fn)

    Logging.info("preparing script")
    root = Global.getRoot()
    runm = "run.m" 
    logm = "log.txt"
    dkim = join(root, "lib", "matlab", "dki.m")
    dki = join(root, "lib", "matlab", "dki")
    nifti = join(root, "lib", "matlab", "nifti")

    sf = open(script_fn, "w")
    sf.write('#! /usr/bin/env bash\n')
    sf.write('cd %s\n' % tmp_dir)
    sf.write('echo "addpath(\'%s\')" > %s\n' % (dki, runm))
    sf.write('echo "addpath(\'%s\')" >> %s\n' % (nifti, runm))
    sf.write('cat %s >> %s\n' % (dkim, runm))
    sf.write('matlab -nodesktop -nosplash -r run > %s\n' % logm)
    sf.close()

    Logging.info("running script")

    if call(["bash", script_fn], shell=False):
      print("scriptfailed")

    if exists(opts.output):
      if opts.clobber:
          Logging.info("clobbering existing results")
          rmtree(opts.output)
      else:  
          bck_dir = "%s.bck.%d" % (opts.output, int(time()))
          Logging.info("backing up existing results to %s" % bck_dir)
          move(opts.output, bck_dir)

    move(join(tmp_dir, "models.dki"), opts.output) 

    if not opts.save:
      Logging.info("cleaning up")
      rmtree(tmp_dir)
    elif exists(opts.output):
      Logging.info("saving intermediate data")
      move(tmp_dir, join(opts.output, "intermediate"))

    Logging.info("finished")

if __name__ == "__main__":
    main()
