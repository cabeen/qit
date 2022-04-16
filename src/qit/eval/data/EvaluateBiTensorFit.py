#! /usr/bin/env qit
################################################################################
# 
#  Quantitative Imaging Toolkit (QIT) (c) 2012-2022 Ryan Cabeen
#  All rights reserved.
# 
#  The Software remains the property of Ryan Cabeen ("the Author").
# 
#  The Software is distributed "AS IS" under this Licence solely for
#  non-commercial use in the hope that it will be useful, but in order
#  that the Author as a charitable foundation protects its assets for
#  the benefit of its educational and research purposes, the Author
#  makes clear that no condition is made or to be implied, nor is any
#  warranty given or to be implied, as to the accuracy of the Software,
#  or that it will be suitable for any particular purpose or for use
#  under any specific conditions. Furthermore, the Author disclaims
#  all responsibility for the use which is made of the Software. It
#  further disclaims any liability for the outcomes arising from using
#  the Software.
# 
#  The Licensee agrees to indemnify the Author and hold the
#  Author harmless from and against any and all claims, damages and
#  liabilities asserted by third parties (including claims for
#  negligence) which arise directly or indirectly from the use of the
#  Software or the sale of any products based on the Software.
# 
#  No part of the Software may be reproduced, modified, transmitted or
#  transferred in any form or by any means, electronic or mechanical,
#  without the express permission of the Author. The permission of
#  the Author is not required if the said reproduction, modification,
#  transmission or transference is done without financial return, the
#  conditions of this Licence are imposed upon the receiver of the
#  product, and all original and amended source code is included in any
#  transmitted product. You may be held legally responsible for any
#  copyright infringement that is caused or encouraged by your failure to
#  abide by these terms and conditions.
# 
#  You are not permitted under this Licence to use this Software
#  commercially. Use for which any financial return is received shall be
#  defined as commercial use, and includes (1) integration of all or part
#  of the source code or the Software into a product for sale or license
#  by or on behalf of Licensee to third parties or (2) use of the
#  Software or any derivative of it for research with the final aim of
#  developing software products for sale or license to a third party or
#  (3) use of the Software or any derivative of it for research with the
#  final aim of developing non-software products for sale or license to a
#  third party, or (4) use of the Software to provide any service to an
#  external organisation for which payment is received.
#
################################################################################

"""

Evaluate the optimization parameters for bitensor model fitting

"""

from common import *
from qit.data.utils.mri import CostType

def make(base, frac, tDiff, fDiff, tAniso, fAniso, aligned):

  tVec1 = VectSource.randomUnit() 
  tVec2 = tVec1.perp()
  tVec3 = tVec1.cross(tVec2)

  tVal1 = (1.0 + tAniso) * tDiff
  tVal2 = (1.0 - 0.5 * tAniso) * tDiff
  tVal3 = (1.0 - 0.5 * tAniso) * tDiff

  fVal1 = (1.0 + fAniso) * fDiff
  fVal2 = (1.0 - 0.5 * fAniso) * fDiff
  fVal3 = (1.0 - 0.5 * fAniso) * fDiff

  if aligned:
    fVec1 = tVec1
    fVec2 = tVec2
    fVec3 = tVec3
  else:
    fVec1 = VectSource.randomUnit() 
    fVec2 = fVec1.perp()
    fVec3 = fVec1.cross(fVec2)

  model = BiTensor()
  model.setBaseline(base)
  model.setFraction(frac)
  model.setTissueVec(0, tVec1)
  model.setTissueVec(1, tVec2)
  model.setTissueVec(2, tVec3)
  model.setTissueVal(0, tVal1)
  model.setTissueVal(1, tVal2)
  model.setTissueVal(2, tVal3)
  model.setFluidVec(0, fVec1)
  model.setFluidVec(1, fVec2)
  model.setFluidVec(2, fVec3)
  model.setFluidVal(0, fVal1)
  model.setFluidVal(1, fVal2)
  model.setFluidVal(2, fVal3)
  
  return model

def main():
  usage = "qit %s [opts]" % args[0]
  parser = OptionParser(usage=usage, description=__doc__)
  parser.add_option("--input", metavar="<fn>", \
      help="specify the input gradients (bvals or bvecs)")
  parser.add_option("--scaleFrac", metavar="<vals>", default="1.0", \
      help="specify the fraction scaling")
  parser.add_option("--scaleTissue", metavar="<vals>", default="250", \
      help="specify the tissue diffusivity scaling")
  parser.add_option("--scaleFluid", metavar="<vals>", default="50", \
      help="specify the fluid diffusivity scaling")
  parser.add_option("--scaleAngle", metavar="<vals>", default="0.01", \
      help="specify the fluid angle scaling")
  parser.add_option("--rhobeg", metavar="<vals>", default="0.005", \
      help="specify the rhobeg, e.g. 0.0001,0.001,0.01")
  parser.add_option("--baseline", metavar="<val>", default="1.0", \
      help="specify the baseline")
  parser.add_option("--fraction", metavar="<val>", default="0.2", \
      help="specify the fraction")
  parser.add_option("--tissueDiff", metavar="<val>", default="0.0005", \
      help="specify the tissue diffusivity")
  parser.add_option("--fluidDiff", metavar="<val>", default="0.0025", \
      help="specify the fluid diffusivity")
  parser.add_option("--maxiters", metavar="<val>", default="100000", \
      help="specify the maximum iterations")
  parser.add_option("--rhoend", metavar="<val>", default="1e-7", \
      help="specify the ending rho")
  parser.add_option("--trials", metavar="<val>", default="1", \
      help="specify the number of trials")
  parser.add_option("--output", metavar="<fn>", \
      help="specify the output CSV report")

  (opts, pos) = parser.parse_args()

  if len(pos) != 0 or len(args) < 3:
      parser.print_help()
      return

  if not opts.input:
      Logging.error("no input specified")

  if not opts.output:
      Logging.error("no output specified")

  Logging.info("started")

  Logging.info("reading the input/output filenames")
  grads_fn = opts.input
  output_fn = opts.output

  Logging.info("reading the input gradients")
  gradients = Gradients.read(grads_fn)

  Logging.info("creating the reference model")
  tDiff = float(opts.tissueDiff)
  fDiff = float(opts.fluidDiff)
  base = float(opts.baseline)
  aligned = True

  fracs = [float(f) for f in opts.fraction.split(",")]

  Logging.info("creating functional structures")
  factory = FitBiTensorSimplexNLLS()
  factory.cost = CostType.NRMSE
  factory.iprint = 1
  factory.verbose = True 
  factory.weighted = False
  factory.gradients = gradients
  factory.maxiters = int(opts.maxiters)
  factory.rhoend = float(opts.rhoend)

  synther = BiTensor.synth(gradients)

  features = []
  features.append(BiTensor.FEATURES_FRAC)
  features.append(BiTensor.FEATURES_TMD)
  features.append(BiTensor.FEATURES_FMD)
  features.append(BiTensor.FEATURES_TFA)
  features.append(BiTensor.FEATURES_FFA)

  def run_trial(method, ref_model):

    factory.method = method
    fitter = factory.create() 
    ref_signal = synther.apply(ref_model.getEncoding())
    fit_model = BiTensor(fitter.apply(ref_signal))
    fit_signal = synther.apply(fit_model.getEncoding())

    record = Record()

    for feature in features:
        record.with("ref_%s" % feature, ref_model.feature(feature).get(0))
        record.with("fit_%s" % feature, fit_model.feature(feature).get(0))

    ref_tv = ref_model.getTissueVec(0)
    ref_fv = ref_model.getFluidVec(0)
    ref_angle = ref_tv.angleLineDeg(ref_fv)

    fit_tv = fit_model.getTissueVec(0)
    fit_fv = fit_model.getFluidVec(0)
    fit_angle = fit_tv.angleLineDeg(fit_fv)

    record.with("ref_angle", ref_angle)
    record.with("fit_angle", fit_angle)

    record.with("nrmse", ModelUtils.nrmse(gradients, ref_signal, fit_signal))
    record.with("fitting", method)

    return record

  methods = []
  methods.append(FitBiTensorSimplexNLLS.BiTensorFitType.DTI)
  methods.append(FitBiTensorSimplexNLLS.BiTensorFitType.DTIFWE)
  methods.append(FitBiTensorSimplexNLLS.BiTensorFitType.BothIsotropic)
  methods.append(FitBiTensorSimplexNLLS.BiTensorFitType.Isotropic)
  methods.append(FitBiTensorSimplexNLLS.BiTensorFitType.AlignedZeppelin)
  methods.append(FitBiTensorSimplexNLLS.BiTensorFitType.Zeppelin)
  methods.append(FitBiTensorSimplexNLLS.BiTensorFitType.Anisotropic)

  models = []

  for frac in fracs:
    models.append(("IsoIso", make(base, frac, tDiff, fDiff, 0.0, 0.0, True)))

  for frac in fracs:
    models.append(("ZepIso", make(base, frac, tDiff, fDiff, 0.3, 0.0, True)))

  for frac in fracs:
    models.append(("ZepZep", make(base, frac, tDiff, fDiff, 0.3, 0.15, True)))

  for frac in fracs:
    models.append(("ZepDZep", make(base, frac, tDiff, fDiff, 0.3, 0.15, False)))

  rhobegs = [float(f) for f in opts.rhobeg.split(",")]
  scaleFracs = [float(f) for f in opts.scaleFrac.split(",")]
  scaleTissues = [float(f) for f in opts.scaleTissue.split(",")]
  scaleFluids = [float(f) for f in opts.scaleFluid.split(",")]
  scaleAngles = [float(f) for f in opts.scaleAngle.split(",")]

  report = None

  for modelName, model in models:
    for method in methods:
      for scaleTissue in scaleTissues:
        for scaleFluid in scaleFluids:
          for scaleFrac in scaleFracs:
            for scaleAngle in scaleAngles:
              for rhobeg in rhobegs:
                for trial in range(int(opts.trials)):
                  factory.scaleFrac = scaleFrac
                  factory.scaleTissue = scaleTissue
                  factory.scaleFluid = scaleFluid
                  factory.scaleAngle= scaleAngle
                  factory.rhobeg = rhobeg
            
                  Logging.info("running trial")
                  record = run_trial(method, model)
                  record.with("trial", trial)
                  record.with("synth", modelName)
                  record.with("scaleFrac", scaleFrac)
                  record.with("scaleTissue", scaleTissue)
                  record.with("scaleFluid", scaleFluid)
                  record.with("scaleAngle", scaleAngle)
                  record.with("rhobeg", rhobeg)
  
                  if not report:
                    report = Table().withFields(record.keys())
            
                  report.addRecord(record)

  Logging.info("writing output report")
  report.write(output_fn)

  Logging.info("finished")

if __name__ == "__main__":
    main()

