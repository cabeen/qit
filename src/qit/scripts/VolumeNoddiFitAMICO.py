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

"""  fit a NODDI volume with AMICO (installed from https://github.com/daducci/AMICO)"""

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
    parser.add_option("--output", metavar="<dir>", \
        help="specify an output NODDI directory (required)")

    parser.add_option("--shells", metavar="<a,b,c,...>", \
        help="specify a subset of gradient shells (comma-separated)")
    parser.add_option("--which", metavar="<a,b,c,...>", \
        help="specify a subset of gradients to use (comma-separated)")
    parser.add_option("--exclude", metavar="<a,b,c,...>", \
        help="specify a subset of gradients to exclude (comma-separated)")
    parser.add_option("--dpar", metavar="<float>", \
        help="specify parallel diffusivity parameter (default is 1.7e-3)")
    parser.add_option("--diso", metavar="<float>", \
        help="specify isotropic diffusivity parameter (default is 3.0e-3)")
    parser.add_option("--exvivo", \
        help="include a dot compartment to support ex-vivo data", action="store_true")
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

    tmp_dir = abspath("%s.tmp.%d" % (opts.output, int(time())))
    Logging.info("using temporary directory %s" % tmp_dir)

    if not exists(tmp_dir):
        makedirs(tmp_dir)

    Logging.info("preparing AMICO")
    # stuff that AMICO expects to find
    study_dir = join(tmp_dir, "study")
    subj_dir = join(study_dir, "subject")
    if not exists(subj_dir):
        makedirs(subj_dir)

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

    script_fn = join(tmp_dir, "run_amico.py")
    dwi_fn = join(subj_dir, "dwi.nii.gz")
    mask_fn = join(subj_dir, "mask.nii.gz")
    bvecs_fn = join(subj_dir, "dwi.bvec")
    bvals_fn = join(subj_dir, "dwi.bval")

    # AMICO expects FSL style gradients
    dwi.write(dwi_fn)
    mask.write(mask_fn)

    bvecs = grads.getBvecs()
    if not opts.noflip: 
      bvecs = VectsUtils.apply(bvecs, VectFunctionSource.scale(-1.0, 1.0, 1.0))
    VectsUtils.transpose(bvecs).write(bvecs_fn)
    VectsUtils.transpose(grads.getBvals()).write(bvals_fn)

    sf = open(script_fn, "w")
    sf.write('#! /usr/bin/env python\n')
    sf.write('import numpy\n')
    sf.write('import amico\n')
    sf.write('import os\n')
    sf.write('os.chdir("%s")\n' % tmp_dir)
    sf.write('amico.core.setup()\n')
    sf.write('ae = amico.Evaluation("study", "subject")\n')
    sf.write('amico.util.fsl2scheme("study/subject/dwi.bval", "study/subject/dwi.bvec")\n')
    sf.write('ae.load_data(dwi_filename="dwi.nii.gz", scheme_filename="dwi.scheme", mask_filename="mask.nii.gz", b0_thr=0)\n')
    sf.write('ae.set_model("NODDI")\n')

    # see https://github.com/daducci/AMICO/issues/5
    if opts.exvivo:
      dpar = "ae.model.dPar"
      diso = "ae.model.dIso"
      vfs = "ae.model.IC_VFs"
      ods = "ae.model.IC_ODs"

      if opts.dpar:
        dpar = opts.dpar

      if opts.diso:
        diso = opts.diso

      Logging.info("setting amico ex vivo option") 
      sf.write('ae.model.set(%s, %s, %s, %s, True)\n' % (dpar, diso, vfs, ods))

    sf.write('ae.generate_kernels(regenerate = True)\n')
    sf.write('ae.load_kernels()\n')
    sf.write('ae.fit()\n')
    sf.write('ae.save_results()\n')
    sf.close()

    Logging.info("running AMICO")
    if call(["python", script_fn], shell=False):
      print("AMICO failed")

    Logging.info("cleaning up")
    pairs = []
    pairs.append(("ICVF","ficvf"))
    pairs.append(("ISOVF","fiso"))
    pairs.append(("OD","odi"))
    pairs.append(("dir","dir"))

    if opts.exvivo:
      pairs.append(("dot", "fdot"))

    for pair in pairs:
      fn = join(tmp_dir, "noddi_%s.nii.gz" % pair[1])
      move(join(subj_dir, "AMICO", "NODDI", "FIT_%s.nii.gz" % pair[0]), fn)
      # fix an unusual header (only sform, but set to "Scannar Anat")
      if call(["fslorient", "-setqformcode", "1", fn], shell=False):
        print("failed to fix qform")

    dirVolume = Volume.read(join(tmp_dir, "noddi_dir.nii.gz"))
    subset = VolumeSubset()
    subset.input = dirVolume
    subset.which = "0" 
    subset.run().output.write(join(tmp_dir, "noddi_fibredirs_xvec.nii.gz"))
    subset.which = "1" 
    subset.run().output.write(join(tmp_dir, "noddi_fibredirs_yvec.nii.gz"))
    subset.which = "2" 
    subset.run().output.write(join(tmp_dir, "noddi_fibredirs_zvec.nii.gz"))

    baser = VolumeDwiBaseline() 
    baser.input = dwi 
    baser.gradients = grads
    baser.mask = mask
    baser.run().mean.write(join(tmp_dir, "noddi_baseline.nii.gz"))

    if not opts.save:
      Logging.info("cleaning up")
      rmtree(study_dir)
      remove(script_fn)

    if exists(opts.output):
      if opts.clobber:
          Logging.info("clobbering existing results")
          rmtree(opts.output)
      else:  
          bck_dir = "%s.bck.%d" % (opts.output, int(time()))
          Logging.info("backing up existing results to %s" % bck_dir)
          move(opts.output, bck_dir)

    if opts.output.endswith("noddi"):
      move(tmp_dir, opts.output)
    else:
      names = []
      names.append("ficvf")
      names.append("odi")
      names.append("fiso")
      names.append("fibredirs_xvec")
      names.append("fibredirs_yvec")
      names.append("fibredirs_zvec")
      names.append("baseline")

      if opts.exvivo:
        names.append("fdot")

      for name in names:
        move(join(tmp_dir, "noddi_%s.nii.gz" % name), "%s_%s.nii.gz" % (opts.output, name))
      rmtree(tmp_dir)

    Logging.info("finished")

if __name__ == "__main__":
    main()
