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

"""  fit the ball and sticks model with FSL XFIBRES"""

from common import *

class Xfibres:
    def __init__(self):
        self.dwi_fn = None
        self.bvecs_fn = None
        self.bvals_fn = None
        self.mask_fn = None
        self.out_dir = None
        self.fudge = "1"
        self.nj = "1250" 
        self.bi = "1000"
        self.model = "2"
        self.se = "25"
        self.upe = "24"
        self.nfibres = "3"
        self.seed = None
        self.nonlinear = False
        self.cnonlinear = True
        self.rician = False
        self.nospat = False
        self.allard = False
        self.noard = False
        self.f0 = False 
        self.ardf0 = False

    def withDwi(self, fn):
        self.dwi_fn = fn
        return self

    def withBvecs(self, fn):
        self.bvecs_fn = fn
        return self

    def withBvals(self, fn):
        self.bvals_fn = fn
        return self

    def withMask(self, fn):
        self.mask_fn = fn
        return self

    def withOutput(self, dirname):
        self.out_dir = dirname 
        return self

    def run(self):
        cmd = ["xfibres"]
        cmd += ["--data=%s" % self.dwi_fn]
        cmd += ["--mask=%s" % self.mask_fn]
        cmd += ["--bvecs=%s" % self.bvecs_fn]
        cmd += ["--bvals=%s" % self.bvals_fn]
        cmd += ["--forcedir", "--logdir=%s" % self.out_dir]
        cmd += ["--fudge=%s" % self.fudge]
        cmd += ["--nj=%s" % self.nj]
        cmd += ["--bi=%s" % self.bi]
        cmd += ["--model=%s" % self.model]
        cmd += ["--se=%s" % self.se]
        cmd += ["--upe=%s" % self.upe]
        cmd += ["--nfibres=%s" % self.nfibres]

        if self.seed:
            cmd += ["--seed=%s" % self.seed]

        if self.cnonlinear:
            cmd += ["--cnonlinear"]

        if self.nonlinear:
            cmd += ["--nonlinear"]

        if self.rician:
            cmd += ["--rician"]

        if self.nospat:
            cmd += ["--nospat"]

        if self.noard:
            cmd += ["--noard"]

        if self.allard:
            cmd += ["--allard"]

        if self.f0:
            cmd += ["--f0"]

        if self.ardf0:
            cmd += ["--ardf0"]

        if call(cmd, shell=False):
            print("xfibres failed")
            exit(1)

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

    parser.add_option("--fudge", metavar="<value>", \
        help="specify fudge parameter in fsl xfibres")
    parser.add_option("--nj", metavar="<value>", \
        help="specify nj in fsl xfibres")
    parser.add_option("--bi", metavar="<value>", \
        help="specify bi in fsl xfibres")
    parser.add_option("--model", metavar="<value>", \
        help="specify a model in fsl xfibres")
    parser.add_option("--se", metavar="<value>", \
        help="specify se in fsl xfibres")
    parser.add_option("--upe", metavar="<value>", \
        help="specify upe in fsl xfibres")
    parser.add_option("--nfibres", metavar="<value>", \
        help="specify nfibres in fsl xfibres")
    parser.add_option("--seed", metavar="<value>", \
        help="specify a random seed in fsl xfibres")
    parser.add_option("--nonlinear", action="store_true", \
        help="specify nonlinear fitting in fsl xfibres")
    parser.add_option("--no-cnonlinear", action="store_true", \
        help="specify no constrained nonlinear ftting in fsl xfibres")
    parser.add_option("--rician", action="store_true", \
        help="specify rician noise model in fsl xfibres")
    parser.add_option("--nospat", action="store_true", \
        help="specify no spatial fitting in fsl xfibres")
    parser.add_option("--allard", action="store_true", \
        help="apply ARD on all compartments in fsl xfibres")
    parser.add_option("--noard", action="store_true", \
        help="specify noard in fsl xfibres")
    parser.add_option("--f0", action="store_true", \
        help="use a f0 compartment in fsl xfibres")
    parser.add_option("--ardf0", action="store_true",\
        help="use ARD with f0 in fsl xfibres")

    (opts, pos) = parser.parse_args()

    if len(pos) != 0 or len(argv) == 1:
        parser.print_help()
        return

    Global.assume(opts.input is not None, "no input found")
    Global.assume(opts.gradients is not None, "no gradients found")
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

    dwi_fn = join(tmp_dir, "dwi.nii.gz")
    mask_fn = join(tmp_dir, "mask.nii.gz")
    bvecs_fn = join(tmp_dir, "bvecs.txt")
    bvals_fn = join(tmp_dir, "bvals.txt")

    dwi.write(dwi_fn)
    mask.write(mask_fn)
    grads.write(bvecs_fn, bvals_fn)

    fit = Xfibres()\
        .withDwi(dwi_fn)\
        .withMask(mask_fn)\
        .withBvals(bvals_fn)\
        .withBvecs(bvecs_fn)\
        .withOutput(tmp_dir)

    if opts.fudge:
        fit.fudge = opts.fudge

    if opts.nj:
        fit.nj = opts.nj

    if opts.bi:
        fit.bi = opts.bi

    if opts.model:
        fit.model = opts.model

    if opts.se:
        fit.se = opts.se

    if opts.upe:
        fit.upe = opts.upe

    if opts.nfibres:
        fit.nfibres = opts.nfibres

    if opts.seed:
        fit.seed = opts.seed

    if opts.no_cnonlinear:
        fit.cnonlinear = False 

    if opts.nonlinear:
        fit.nonlinear = True

    if opts.rician:
        fit.rician = True

    if opts.nospat:
        fit.nospat = True

    if opts.noard:
        fit.noard = True

    if opts.allard:
        fit.allard = True

    if opts.f0:
        fit.f0 = True

    if opts.ardf0:
        fit.ardf0 = True

    Logging.info("fitting ball and stick model")
    fit.run()

    if exists(opts.output):
      if opts.clobber:
          Logging.info("clobbering existing results")
          rmtree(opts.output)
      else:  
          bck_dir = "%s.bck.%d" % (opts.output, int(time()))
          Logging.info("backing up existing results to %s" % bck_dir)
          move(opts.output, bck_dir)

    Logging.info("finalizing")
    move(tmp_dir, opts.output)

    Logging.info("finished")

if __name__ == "__main__":
    main()
