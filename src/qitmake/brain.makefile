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
# 
# Overview: 
# 
#   The diffusion brain mapping pipeline for single-subject analysis.  This 
#   includes a variety of methods for voxel-based and tractography-based 
#   analysis. Anatomical segmentation is performed with reference to annotation 
#   in a standard atlas.  Freesurfer can also be used to segment regions from 
#   a T1-weighted volume.  The results include reconstructinos of anatomy using 
#   either volumetric masks or geometric curve models, as well as quantitative 
#   measures reflecting either the morphometry or microstructure of the anatomy.
#   These quantatative results are stored as key-value pairs in directories 
#   ending in "map", and they can be aggregated across subjects using the
#   "group" analysis pipeline. 
#
# Required input files:
#
#   input/dwi.nii.gz: the diffusion-weighted volume
#   input/bvecs.txt: the gradient b-vectors
#   input/bvals.txt: the gradient b-values
#   input/mask.nii.gz: the brain mask
#
# Optional input directory:
#
#   input/t1.nii.gz: a T1 image for Freesurfer analysis (not always necessary)
#
# Example usage:
#
#     qitmake <target>
#
#   with some example <target> values:
#   
#     diff.tract/bundles.map: metrics from bundle-specific analysis
#     diff.tract/bundles.dti.map: metrics from bundle-specific analysis
#     diff.region/jhu.labels.dti.map: metrics from JHU ROI analysis 
#     diff.region/jhu.labels.dti.tbss.map: metrics from JHU ROI TBSS analysis 
#
#  Note: the pipeline supports advanced diffusion models for multi-shell data.
#        To use them, you can replace dti with one of the following options:
#
#    fwdti: free-water elimated DTI (uses a fixed diffusivity ball)
#    noddi: neurite orientation dispersion and density imaging
#    mcsmt: multi-compartment spherical mean technique from Kaden et al.
#
#   More documentation can be found on the web at http://cabeen.io/qitwiki
# 
# Author: Ryan Cabeen, cabeen@gmail.com
# 
################################################################################

WHEREAMI = $(dir $(abspath $(word $(words $(MAKEFILE_LIST)), $(MAKEFILE_LIST))))
QIT_ROOT ?= $(shell dirname $(shell dirname $(WHEREAMI)))

TMP               := tmp.$(shell date +%s)
BCK               := bck.$(shell date +%s)

QIT_MEMORY        ?= 5G
QIT_BIN           ?= $(QIT_ROOT)/bin/qit
QIT_FLAGS         ?= --verbose --debug --rseed 42 
QIT_CMD           := $(QIT_BIN) -Xmx$(QIT_MEMORY) $(QIT_FLAGS)
QIT_ATLAS         := $(QIT_ROOT)/share/data
QIT_THREADS       ?= 6

$(info $(shell $(QIT_BIN) --version))

################################################################################
# Parameters 
################################################################################

GPU               ?=            # enable GPU multi-fiber fitting (any value)
NO_BEDPOST        ?=            # use the QIT version of multi-fiber fitting

BET_FRAC          ?= 0.3        # the brain extraction threshold (see BET)

DTI_FITTER        ?= WLLS       # the single tensor fitting routing
BTI_FITTER        ?= Isotropic  # the bi-tensor fitting routine 
FWDTI_FITTER      ?= FWLLS      # the FWE-DTI fitting routine
NODDI_FITTER      ?= FullSMT    # the NODDI fitting routine
NODDI_GM_DIFF     ?= 0.0011     # the gray matter intra-neurite diffusivity
NODDI_SHRINK      ?= 0.95       # the shrinkage prior for noddi

ROI_THRESH        ?= 0.2        # the optional FA threshold for roi analysis 
ROI_ERODE         ?= 1          # the optional amount erosion for roi analysis 

NETWORK_FA        ?= 0.2        # the minimum FA for whole brain seeding 
NETWORK_SAMPLES   ?= 2          # the number of samples per voxel
NETWORK_MIN       ?= 0.075      # the minimum volume fraction for tracking
NETWORK_ANGLE     ?= 45         # the maximum turning angle
NETWORK_STEP      ?= 1.0        # the step size
NETWORK_INTERP    ?= Trilinear  # the interpolation method
NETWORK_MIN_LEN   ?= 10         # the minimum track length
NETWORK_MAX_LEN   ?= 10000      # the maximum track length
NETWORK_LIMIT     ?= 1000000    # the maximum number of tracks
NETWORK_MODEL     ?= xfib       # the diffusion model for network tractography

BUNDLE_METHOD     ?= Hybrid     # tracking method (Hybrid Prior Determ Prob)
BUNDLE_INTERP     ?= Trilinear  # the interpolation mode
BUNDLE_MODEL      ?= xfib       # the diffusion model for bundle tractography

BUNDLE_FACTOR     ?= 5          # the seed count multiplier
BUNDLE_ANGLE      ?= 45         # the maximum turning angle
BUNDLE_MIN        ?= 0.075      # the minimum volume fraction for tracking
BUNDLE_STEP       ?= 1.0        # the tracking stepsize
BUNDLE_DISPERSE   ?= 0.05       # the amount of tracking dispersion
BUNDLE_MAXLEN     ?= 10000      # the maximum track length
BUNDLE_LIMIT      ?= 1000000    # the maximum number of tracks

BUNDLE_SMOOTH     ?= -0.75      # the hybrid smoothing (compute from voxel dim)
BUNDLE_HYBFAC     ?= 20         # the seed count multiplier for hybrid tracking
BUNDLE_HYBSUM     ?= 0.01       # the minimum total fiber volume fraction 
BUNDLE_HYBMIN     ?= 0.025      # the minimum compartment volume fraction 
BUNDLE_HYBDISP    ?= 0.15       # the dispersion used in hybrid tracking
BUNDLE_HYBMIN     ?= 0.01       # the minimum fraction for hybrid tracking
BUNDLE_HYBANGLE   ?= 65         # the maximum angle for hybrid tracking

PROJECT_SIGMA     ?= -0.75      # compute from the voxel dimension
PROJECT_ANGLE     ?= 45         # the minimum angle for prior projection
PROJECT_NORM      ?= 0.01       # the minimum norm for inclusion
PROJECT_FRAC      ?= 0.01       # the minimum single fraction for inclusion
PROJECT_FSUM      ?= 0.05       # the minimum total fraction for inclusion

PROJECT_FLAGS     ?= --smooth --mrf --restrict

ALONG_FLAGS       ?=            # the along tract parameters, e.g. --outlier 
ALONG_ITERS       ?= 5          # the number of along tract smoothing iters

SIMPLE_COUNT      ?= 10000      # the maximum number in a simplified bundle
SIMPLE_DIST       ?= 1          # the distance threshold for simplification
TERM_NUM          ?= 2          # the number of points for termination masks

################################################################################
# Constants
################################################################################

MODELS             ?= dti xfib
SPACES             := atlas diff latlas tone 

FS_RGN_NAMES       := 
FS_RGN_NAMES       += ccwm scgm wbbm 
FS_RGN_NAMES       += lbwm lbgm lbbm
FS_RGN_NAMES       += dkwm dkgm dkbm
FS_RGN_NAMES       += lh.subfields rh.subfields

ATLAS_RGN_NAMES    :=
ATLAS_RGN_NAMES    += skeleton
ATLAS_RGN_NAMES    += ini.bstem fic
ATLAS_RGN_NAMES    += hox.sub hox.cort
ATLAS_RGN_NAMES    += cit.sub cit.amy cit.alm
ATLAS_RGN_NAMES    += jhu.labels jhu.tracts
ATLAS_RGN_NAMES    += super.coarse super.medium super.fine
ATLAS_RGN_NAMES    += fsa.ccwm fsa.scgm fsa.wbbm
ATLAS_RGN_NAMES    += fsa.lbbm fsa.lbwm fsa.lbgm
ATLAS_RGN_NAMES    += fsa.dkbm fsa.dkwm fsa.dkgm

QIT_RGN_NAMES      := $(ATLAS_RGN_NAMES)
QIT_RGN_NAMES      += fs.ccwm fs.scgm fs.wbbm
QIT_RGN_NAMES      += fs.lbbm fs.lbwm fs.lbgm
QIT_RGN_NAMES      += fs.dkbm fs.dkwm fs.dkgm

BUNDLE_ALL         ?= # enable this option to include all possible bundles
BUNDLE_BASE        ?= tract
BUNDLE_DIR         ?= $(QIT_ATLAS)/tract/bundles
BUNDLE_LIST        ?= $(if $(BUNDLE_ALL), \
                        $(QIT_ATLAS)/tract/bundles.all.txt, \
                        $(QIT_ATLAS)/tract/bundles.txt)

FIBERS_FIT         ?= $(if $(NO_BEDPOST), \
											  VolumeFibersFit --threads $(QIT_THREADS), \
                        $(if $(GPU), VolumeFibersFitFslGpu, \
												  VolumeFibersFitFsl))

TONE_DIFF_CMD      ?= $(if $(INTRA_NOREG), \
                        VolumeRegisterIdentity, \
                        $(if $(INTRA_DEFORM), \
  											  VolumeRegisterDeformAnts $(ANTS_FLAGS), \
                          $(if $(INTRA_AFFINE), \
	  											  VolumeRegisterLinearAnts --deform $(ANTS_FLAGS), \
    										    VolumeRegisterLinearAnts --deform --rigid $(ANTS_FLAGS))))

################################################################################
# Targets
################################################################################

INPUT_DWI          ?= input/dwi.nii.gz # strictly necessary
INPUT_BVECS        ?= input/bvecs.txt  # strictly necessary
INPUT_BVALS        ?= input/bvals.txt  # strictly necessary
INPUT_TONE         ?= input/t1.nii.gz # necessary only if using freesurfer

INPUT_MASK         ?= input/mask.nii.gz
ROUND_BVECS        ?= input/bvecs.round.txt
ROUND_BVALS        ?= input/bvals.round.txt 
BASELINE           ?= input/baseline.nii.gz
TONE_FS            ?= tone.fs

DIFF_DTI           ?= diff.models.dti
DIFF_FWDTI         ?= diff.models.fwdti
DIFF_BTI           ?= diff.models.bti
DIFF_XFIB          ?= diff.models.xfib
DIFF_NODDI         ?= diff.models.noddi
DIFF_GM_NODDI      ?= diff.models.gm.noddi
DIFF_MCSMT         ?= diff.models.mcsmt

TONE_DTI           ?= tone.models.dti
TONE_FWDTI         ?= tone.models.fwdti
TONE_BTI           ?= tone.models.bti
TONE_XFIB          ?= tone.models.xfib
TONE_NODDI         ?= tone.models.noddi
TONE_GM_NODDI      ?= tone.models.gm.noddi
TONE_MCSMT         ?= tone.models.mcsmt

ATLAS_DTI          ?= atlas.models.dti
ATLAS_FWDTI        ?= atlas.models.fwdti
ATLAS_BTI          ?= atlas.models.bti
ATLAS_XFIB         ?= atlas.models.xfib
ATLAS_NODDI        ?= atlas.models.noddi
ATLAS_GM_NODDI     ?= atlas.models.gm.noddi
ATLAS_MCSMT        ?= atlas.models.mcsmt

TBSS_DTI           ?= atlas.tbss.dti
TBSS_FWDTI         ?= atlas.tbss.fwdti
TBSS_BTI           ?= atlas.tbss.bti
TBSS_NODDI         ?= atlas.tbss.noddi
TBSS_MCSMT         ?= atlas.tbss.mcsmt

DIFF_FS_BRAIN      ?= diff.fs.brain
DIFF_FS_REGIONS    ?= diff.fs.region
DIFF_FS_SURFS      ?= diff.fs.surfaces

ATLAS_FS_BRAIN     ?= atlas.fs.brain
ATLAS_FS_REGIONS   ?= atlas.fs.region
ATLAS_FS_SURFS     ?= atlas.fs.surfaces

TONE_FS_BRAIN      ?= tone.fs.brain
TONE_FS_REGIONS    ?= tone.fs.region
TONE_SURF_BASE     ?= tone
TONE_FS_SURFS      ?= $(TONE_SURF_BASE).fs.surfaces
TONE_CORTEX        ?= $(TONE_SURF_BASE).cortex

DIFF_BRAIN         ?= diff.brain/brain.nii.gz
DIFF_DTI_GM        ?= diff.brain/dti_gm.nii.gz
DIFF_DTI_WM        ?= diff.brain/dti_wm.nii.gz
DIFF_DTI_LABELS    ?= diff.brain/dti_labels.nii.gz

TONE_BRAIN         ?= tone.brain/brain.nii.gz
TONE_DTI_GM        ?= tone.brain/dti_gm.nii.gz
TONE_DTI_WM        ?= tone.brain/dti_wm.nii.gz
TONE_DTI_LABELS    ?= tone.brain/dti_labels.nii.gz

ATLAS_BRAIN        ?= atlas.brain/brain.nii.gz
ATLAS_DTI_GM       ?= atlas.brain/dti_gm.nii.gz
ATLAS_DTI_WM       ?= atlas.brain/dti_wm.nii.gz
ATLAS_DTI_LABELS   ?= atlas.brain/dti_labels.nii.gz

DIFF_MASK          ?= diff.brain/mask.nii.gz
TONE_MASK          ?= tone.brain/mask.nii.gz
ATLAS_MASK         ?= atlas.brain/mask.nii.gz

ATLAS_DIFF_REG     ?= atlas.diff.reg
ATLAS_DIFF_XFM     ?= $(ATLAS_DIFF_REG)/atlas2diff.nii.gz
DIFF_ATLAS_XFM     ?= $(ATLAS_DIFF_REG)/diff2atlas.nii.gz

TONE_DIFF_REG      ?= tone.diff.reg
TONE_DIFF_XFM      ?= $(TONE_DIFF_REG)/tone2diff.nii.gz
DIFF_TONE_XFM      ?= $(TONE_DIFF_REG)/diff2tone.nii.gz

ATLAS_TONE_REG     ?= atlas.tone.reg
ATLAS_TONE_XFM     ?= $(ATLAS_TONE_REG)/atlas2tone.nii.gz
TONE_ATLAS_XFM     ?= $(ATLAS_TONE_REG)/tone2atlas.nii.gz

ATLAS_SELF_XFM     ?= atlas.self.reg/ident.xfm
DIFF_SELF_XFM      ?= diff.self.reg/ident.xfm
TONE_SELF_XFM      ?= tone.self.reg/ident.xfm

ATLAS_PROJECT      ?= atlas.project.xfib
ATLAS_SPHERE       ?= $(QIT_ATLAS)/meshes/sphere.vtk.gz

################################################################################
# Helper Functions 
################################################################################

get.space = $(if $(findstring tbss, $(1)), atlas.tbss,\
               $(if $(findstring atlas, $(1)), atlas,\
                 $(if $(findstring diff, $(1)), diff,\
                   $(if $(findstring tone, $(1)), tone))))

mask.xfm  = $(QIT_CMD) MaskTransform \
               --input $(1) \
               --reference $(2) \
               --deform $(3) \
               --output $(4)

tbss      = $(FSLDIR)/bin/tbss_skeleton \
               -i $(QIT_ATLAS)/models.dti/dti_FA.nii.gz \
               -p $(shell cat $(QIT_ATLAS)/skeleton/thresh.txt) \
               $(QIT_ATLAS)/skeleton/mean_FA_skeleton_mask_dst.nii.gz \
               $(FSLDIR)/data/standard/LowerCingulum_1mm \
               $(ATLAS_DTI)/dti_FA.nii.gz $2 -a $1

mask.ms   = $(QIT_CMD) MaskRegionsMeasure --basic \
               --regions $(1)/rois.nii.gz \
               --lookup $(1)/rois.csv \
               --output $(5) \
               --volume $(4)=$(2)/$(3).nii.gz

tbss.ms   = $(QIT_CMD) MaskRegionsMeasure --basic \
               --regions $(1)/rois.nii.gz \
               --lookup $(1)/rois.csv \
               --mask $(QIT_ATLAS)/skeleton/mean_FA_skeleton_mask.nii.gz \
               --output $(5) \
               --volume $(4)=$(2)/$(3).nii.gz

whole.vertex.ms = $(QIT_CMD) CurvesMeasureBatch \
               --attrs $(5) \
               --names $(2) \
               --input $(1)/%s/curves.vtk.gz \
               --volume $(5)=$(3)/$(4).nii.gz \
               --output $(6)

whole.voxel.ms = $(QIT_CMD) CurvesMeasureBatch \
               --voxel \
               --attrs $(5) \
               --names $(2) \
               --input $(1)/%s/curves.vtk.gz \
               --volume $(5)=$(3)/$(4).nii.gz \
               --output $(6)

whole.core.ms = $(QIT_CMD) CurvesMeasureBatch \
               --attrs $(5) \
               --names $(2) \
               --input $(1)/%s/core.vtk.gz \
               --volume $(5)=$(3)/$(4).nii.gz \
               --output $(6)

along.vertex.ms = $(QIT_CMD) CurvesMeasureAlongBatch \
               --iters $(ALONG_ITERS) \
               --attrs $(5) \
               --names $(2) \
               --input $(1)/%s/curves.vtk.gz \
               --volume $(5)=$(3)/$(4).nii.gz \
               --output $(6)

along.voxel.ms = $(QIT_CMD) CurvesMeasureAlongBatch \
               --voxel \
               --attrs $(5) \
               --names $(2) \
               --input $(1)/%s/curves.vtk.gz \
               --volume $(5)=$(3)/$(4).nii.gz \
               --output $(6)

along.core.ms  = $(QIT_CMD) CurvesMeasureAlongBatch \
               --iters $(ALONG_ITERS) \
               --attrs $(5) \
               --names $(2) \
               --input $(1)/%s/core.vtk.gz \
               --volume $(5)=$(3)/$(4).nii.gz \
               --output $(6)

density.ms     = $(QIT_CMD) VolumeMeasureBatch \
               --density --thresh 0.01 \
               --input $(1)/%s.nii.gz \
               --names $(2) \
               --volume $(5)=$(3)/$(4).nii.gz \
               --output $(6)

################################################################################
# Parameter Estimation 
################################################################################

$(BASELINE): | $(INPUT_DWI) $(INPUT_BVECS)
	$(QIT_CMD) VolumeDwiBaseline \
    --input $(word 1,$|) \
    --gradients $(word 2,$|) \
    --mean $@ 

$(ROUND_BVECS): | $(INPUT_BVECS)
	$(QIT_CMD) GradientsTransform \
    --round \
    --input $(word 1,$|) \
    --output $@ 

$(INPUT_MASK): | $(BASELINE)
	$(QIT_CMD) VolumeBrainExtract \
    --input $(word 1,$|) \
    --frac $(BET_FRAC) \
    --output $@ 

$(DIFF_MASK): | $(INPUT_DWI) $(INPUT_MASK) $(INPUT_BVECS)
	-mkdir -p $(dir $@)
	$(QIT_CMD) VolumeDwiCleanMask \
    --input $(word 1, $|) \
    --mask $(word 2, $|) \
    --gradients $(word 3, $|) \
    --output $@

$(DIFF_DTI): | $(INPUT_DWI) $(DIFF_MASK) $(INPUT_BVECS) $(INPUT_BVALS)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) VolumeTensorFit $(DTIFIT_FLAGS) \
    --bestb \
    --clamp 0 \
    --method $(DTI_FITTER) \
    --threads $(QIT_THREADS) \
    --input $(word 1,$|) \
    --mask $(word 2,$|) \
    --gradients $(word 3,$|) \
    --output $@

$(DIFF_FWDTI): | $(INPUT_DWI) $(DIFF_MASK) $(ROUND_BVECS) $(ROUND_BVALS)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) VolumeTensorFit $(FWDTIFIT_FLAGS) \
    --method $(FWDTI_FITTER) \
    --threads $(QIT_THREADS) \
    --input $(word 1,$|) \
    --mask $(word 2,$|) \
    --gradients $(word 3,$|) \
    --output $@

$(DIFF_BTI): | $(INPUT_DWI) $(DIFF_MASK) $(ROUND_BVECS) $(ROUND_BVALS)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) VolumeBiTensorFit $(BTIFIT_FLAGS) \
    --method $(BTI_FITTER) \
    --threads $(QIT_THREADS) \
    --input $(word 1,$|) \
    --mask $(word 2,$|) \
    --gradients $(word 3,$|) \
    --output $@

$(DIFF_XFIB): | $(INPUT_DWI) $(DIFF_MASK) $(ROUND_BVECS) $(ROUND_BVALS)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) $(FIBERS_FIT) $(XFIBFIT_FLAGS) \
    --input $(word 1,$|) \
    --mask $(word 2,$|) \
    --gradients $(word 3,$|) \
    --output $@

$(DIFF_NODDI): | $(INPUT_DWI) $(DIFF_MASK) $(ROUND_BVECS) $(ROUND_BVALS)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) VolumeNoddiFit $(NODDIFIT_FLAGS) \
    --prior $(NODDI_SHRINK) \
    --method $(NODDI_FITTER) \
    --threads $(QIT_THREADS) \
    --input $(word 1,$|) \
    --mask $(word 2,$|) \
    --gradients $(word 3,$|) \
    --output $@

$(DIFF_GM_NODDI): | $(INPUT_DWI) $(DIFF_MASK) $(ROUND_BVECS) $(ROUND_BVALS)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) VolumeNoddiFit $(NODDIFIT_FLAGS) \
    --prior $(NODDI_SHRINK) \
    --dpar $(NODDI_GM_DIFF) \
    --method $(NODDI_FITTER) \
    --threads $(QIT_THREADS) \
    --input $(word 1,$|) \
    --mask $(word 2,$|) \
    --gradients $(word 3,$|) \
    --output $@

$(DIFF_MCSMT): | $(INPUT_DWI) $(DIFF_MASK) $(ROUND_BVECS) $(ROUND_BVALS)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) VolumeMcsmtFit $(MCSMTFIT_FLAGS) \
    --threads $(QIT_THREADS) \
    --input $(word 1,$|) \
    --mask $(word 2,$|) \
    --gradients $(word 3,$|) \
    --output $@

$(DIFF_DTI_LABELS): | $(DIFF_FWDTI)
	$(QIT_CMD) VolumeTensorSegmentTissue \
    --input $(word 1, $|) \
    --labels $@ \
    --white $(DIFF_DTI_WM) \
    --gray $(DIFF_DTI_GM)
$(DIFF_DTI_GM): | $(DIFF_DTI_LABELS)
$(DIFF_DTI_WM): | $(DIFF_DTI_LABELS)

$(TONE_DTI_LABELS): | $(TONE_FWDTI)
	$(QIT_CMD) VolumeTensorSegmentTissue \
    --input $(word 1, $|) \
    --labels $@ \
    --white $(TONE_DTI_WM) \
    --gray $(TONE_DTI_GM)
$(TONE_DTI_GM): | $(TONE_DTI_LABELS)
$(TONE_DTI_WM): | $(TONE_DTI_LABELS)

$(ATLAS_DTI_LABELS): | $(ATLAS_FWDTI)
	$(QIT_CMD) VolumeTensorSegmentTissue \
    --input $(word 1, $|) \
    --labels $@ \
    --white $(ATLAS_DTI_WM) \
    --gray $(ATLAS_DTI_GM)
$(ATLAS_DTI_GM): | $(ATLAS_DTI_LABELS)
$(ATLAS_DTI_WM): | $(ATLAS_DTI_LABELS)

################################################################################
# Spatial Normalization 
################################################################################

$(DIFF_BRAIN): | $(BASELINE)
	-mkdir -p $(dir $@)
	cp $(word 1, $|) $@

$(ATLAS_BRAIN): | $(QIT_ATLAS)/tone.nii.gz
	-mkdir -p $(dir $@)
	cp $(word 1, $|) $@

$(TONE_BRAIN): | $(TONE_FS_BRAIN)
	-mkdir -p $(dir $@)
	cp $(word 1, $|)/brain.nii.gz $@

$(ATLAS_SELF_XFM):
	-mkdir $(dir $@)
	$(QIT_CMD) MatrixCreate --identity 4 --output $@

$(TONE_SELF_XFM):
	-mkdir $(dir $@)
	$(QIT_CMD) MatrixCreate --identity 4 --output $@

$(DIFF_SELF_XFM):
	-mkdir $(dir $@)
	$(QIT_CMD) MatrixCreate --identity 4 --output $@

$(TONE_DIFF_REG): | $(TONE_BRAIN) $(DIFF_BRAIN)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) $(TONE_DIFF_CMD) $(ANTS_FLAGS) \
    --ref $(word 1,$|) \
    --input $(word 2,$|) \
    --output $@
	cp $@/xfm.nii.gz $(TONE_DIFF_XFM)
	cp $@/invxfm.nii.gz $(DIFF_TONE_XFM)
$(TONE_DIFF_XFM): | $(TONE_DIFF_REG)
$(DIFF_TONE_XFM): | $(TONE_DIFF_REG)

$(ATLAS_TONE_REG): | $(TONE_BRAIN) $(QIT_ATLAS)/crop/tone.nii.gz
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) VolumeRegisterDeformAnts $(ANTS_FLAGS) \
    --input $(word 1,$|) \
    --ref $(word 2,$|) \
    --output $@
	mv $@/xfm.nii.gz $(ATLAS_TONE_XFM)
	mv $@/invxfm.nii.gz $(TONE_ATLAS_XFM)
$(TONE_ATLAS_XFM): | $(ATLAS_TONE_REG)
$(ATLAS_TONE_XFM): | $(ATLAS_TONE_REG)

$(ATLAS_DIFF_REG): | $(DIFF_DTI) $(QIT_ATLAS)/crop/dti_FA.nii.gz
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) VolumeRegisterDeformAnts $(ANTS_FLAGS) \
    --input $(word 1,$|)/dti_FA.nii.gz \
    --ref  $(word 2,$|) \
    --output $@
	mv $@/xfm.nii.gz $(ATLAS_DIFF_XFM)
	mv $@/invxfm.nii.gz $(DIFF_ATLAS_XFM)
$(ATLAS_DIFF_XFM): | $(ATLAS_DIFF_REG)
$(DIFF_ATLAS_XFM): | $(ATLAS_DIFF_REG)

$(DIFF_FS_BRAIN): | $(TONE_FS_BRAIN) $(DIFF_TONE_XFM) $(DIFF_MASK)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	mkdir -p $@.$(TMP) 
	$(QIT_CMD) VolumeTransform \
    --input $(word 1,$|)/brain.nii.gz \
    --deform $(word 2,$|) \
    --reference $(word 3,$|) \
    --output $@.$(TMP)/brain.nii.gz
	$(QIT_CMD) VolumeTransform \
    --input $(word 1,$|)/T1.nii.gz \
    --deform $(word 2,$|) \
    --reference $(word 3,$|) \
    --output $@.$(TMP)/T1.nii.gz
	$(QIT_CMD) VolumeTransform \
    --input $(word 1,$|)/orig.nii.gz \
    --deform $(word 2,$|) \
    --reference $(word 3,$|) \
    --output $@.$(TMP)/orig.nii.gz
	$(QIT_CMD) MaskTransform \
    --input $(word 1,$|)/brainmask.nii.gz \
    --deform $(word 2,$|) \
    --reference $(word 3,$|) \
    --output $@.$(TMP)/brainmask.nii.gz
	$(QIT_CMD) MaskTransform \
    --input $(word 1,$|)/ribbon.nii.gz \
    --deform $(word 2,$|) \
    --reference $(word 3,$|) \
    --output $@.$(TMP)/ribbon.nii.gz
	cp $(word 1,$|)/ribbon.csv $@.$(TMP)/ribbon.csv
	$(QIT_CMD) MaskRegionsExtract \
    --regions $@.$(TMP)/ribbon.nii.gz \
    --lookup $@.$(TMP)/ribbon.csv \
    --output-mask $@.$(TMP)/%s.nii.gz
	$(QIT_CMD) MaskSet \
    --input $@.$(TMP)/lh_gm.nii.gz \
    --mask $@.$(TMP)/rh_gm.nii.gz \
    --output $@.$(TMP)/gm.nii.gz
	$(QIT_CMD) MaskSet \
    --input $@.$(TMP)/lh_wm.nii.gz \
    --mask $@.$(TMP)/rh_wm.nii.gz \
    --output $@.$(TMP)/wm.nii.gz
	mv $@.$(TMP) $@

$(ATLAS_FS_BRAIN): | $(TONE_FS_BRAIN) $(ATLAS_TONE_XFM) $(ATLAS_BRAIN)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	mkdir -p $@.$(TMP) 
	$(QIT_CMD) VolumeTransform \
    --input $(word 1,$|)/brain.nii.gz \
    --deform $(word 2,$|) \
    --reference $(word 3,$|) \
    --output $@.$(TMP)/brain.nii.gz
	$(QIT_CMD) MaskTransform \
    --input $(word 1,$|)/ribbon.nii.gz \
    --deform $(word 2,$|) \
    --reference $(word 3,$|) \
    --output $@.$(TMP)/ribbon.nii.gz
	cp $(word 1,$|)/ribbon.csv $@.$(TMP)/ribbon.csv
	$(QIT_CMD) MaskRegionsExtract \
    --regions $@.$(TMP)/ribbon.nii.gz \
    --lookup $@.$(TMP)/ribbon.csv \
    --output-mask $@.$(TMP)/%s.nii.gz
	$(QIT_CMD) MaskSet \
    --input $@.$(TMP)/lh_gm.nii.gz \
    --mask $@.$(TMP)/rh_gm.nii.gz \
    --output $@.$(TMP)/gm.nii.gz
	$(QIT_CMD) MaskSet \
    --input $@.$(TMP)/lh_wm.nii.gz \
    --mask $@.$(TMP)/rh_wm.nii.gz \
    --output $@.$(TMP)/wm.nii.gz
	mv $@.$(TMP) $@

$(TONE_DTI): | $(DIFF_DTI) $(TONE_DIFF_XFM) $(TONE_BRAIN)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) VolumeTensorTransform \
    --input $(word 1, $|) \
    --deform $(word 2, $|) \
    --reference $(word 3, $|) \
    --output $@
$(foreach i, FA MD RD AD CP CL CS, $(TONE_DTI)/dti_$(i).nii.gz): | $(TONE_DTI)

$(TONE_NODDI): | $(DIFF_NODDI) $(TONE_DIFF_XFM) $(TONE_BRAIN)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) VolumeNoddiTransform $(NODDIXFM_FLAGS) \
    --input $(word 1,$|) \
    --deform $(word 2,$|) \
    --reference $(word 3,$|) \
    --output $@

$(TONE_GM_NODDI): | $(DIFF_GM_NODDI) $(TONE_DIFF_XFM) $(TONE_BRAIN)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) VolumeNoddiTransform $(NODDIXFM_FLAGS) \
    --input $(word 1,$|) \
    --deform $(word 2,$|) \
    --reference $(word 3,$|) \
    --output $@

$(TONE_BTI): | $(DIFF_BTI) $(TONE_DIFF_XFM) $(TONE_BRAIN)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) VolumeBiTensorTransform $(BTIXFM_FLAGS) \
    --input $(word 1,$|) \
    --deform $(word 2,$|) \
    --reference $(word 3,$|) \
    --output $@

$(TONE_MCSMT): | $(DIFF_MCSMT) $(TONE_DIFF_XFM) $(TONE_BRAIN)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) VolumeMcsmtTransform $(MCSMTXFM_FLAGS) \
    --input $(word 1,$|) \
    --deform $(word 2,$|) \
    --reference $(word 3,$|) \
    --output $@

$(TONE_FWDTI): | $(DIFF_FWDTI) $(TONE_DIFF_XFM) $(TONE_BRAIN)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) VolumeTensorTransform $(FWDTIXFM_FLAGS) \
    --input $(word 1,$|) \
    --deform $(word 2,$|) \
    --reference $(word 3,$|) \
    --output $@

$(TONE_XFIB): | $(DIFF_XFIB) $(TONE_DIFF_XFM) $(TONE_BRAIN)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) VolumeFibersTransform $(XFIBXFM_FLAGS) \
    --input $(word 1,$|) \
    --deform $(word 2,$|) \
    --reference $(word 3,$|) \
    --output $@

$(ATLAS_DTI): | $(DIFF_DTI) $(ATLAS_DIFF_XFM) $(ATLAS_BRAIN)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) VolumeTensorTransform  \
    --input $(word 1,$|) \
    --deform $(word 2,$|) \
    --reference $(word 3,$|) \
    --output $@
$(foreach i, FA MD RD AD CP CL CS, $(ATLAS_DTI)/dti_$(i).nii.gz): | $(ATLAS_DTI)

$(ATLAS_FWDTI): | $(DIFF_FWDTI) $(ATLAS_DIFF_XFM) $(ATLAS_BRAIN)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) VolumeTensorTransform  \
    --input $(word 1,$|) \
    --deform $(word 2,$|) \
    --reference $(word 3,$|) \
    --output $@

$(ATLAS_BTI): | $(DIFF_BTI) $(ATLAS_DIFF_XFM) $(ATLAS_BRAIN)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) VolumeBiTensorTransform  \
    --input $(word 1,$|) \
    --deform $(word 2,$|) \
    --reference $(word 3,$|) \
    --output $@

$(ATLAS_XFIB): | $(DIFF_XFIB) $(ATLAS_DIFF_XFM) $(ATLAS_BRAIN)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) VolumeFibersTransform $(XFIBXFM_FLAGS) \
    --input $(word 1,$|) \
    --deform $(word 2,$|) \
    --reference $(word 3,$|) \
    --output $@

$(ATLAS_NODDI): | $(DIFF_NODDI) $(ATLAS_DIFF_XFM) $(ATLAS_BRAIN)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) VolumeNoddiTransform  \
    --input $(word 1,$|) \
    --deform $(word 2,$|) \
    --reference $(word 3,$|) \
    --output $@

$(ATLAS_GM_NODDI): | $(DIFF_GM_NODDI) $(ATLAS_DIFF_XFM) $(ATLAS_BRAIN)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) VolumeNoddiTransform  \
    --input $(word 1,$|) \
    --deform $(word 2,$|) \
    --reference $(word 3,$|) \
    --output $@

$(ATLAS_MCSMT): | $(DIFF_MCSMT) $(ATLAS_DIFF_XFM) $(ATLAS_BRAIN)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) VolumeMcsmtTransform  \
    --input $(word 1,$|) \
    --deform $(word 2,$|) \
    --reference $(word 3,$|) \
    --output $@

$(ATLAS_PROJECT): | $(ATLAS_XFIB) $(QIT_ATLAS)/models.xfib
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) VolumeFibersProject \
    --soft \
    --presmooth 2 \
    --input $(word 1,$|) \
    --reference $(word 2,$|) \
    --output $@
	
$(ATLAS_MASK): | $(DIFF_MASK) $(ATLAS_DIFF_XFM) $(ATLAS_BRAIN)
	-mkdir -p $(dir $@)
	$(QIT_CMD) MaskTransform \
    --input $(word 1,$|) \
    --deform $(word 2,$|) \
    --reference $(word 3,$|) \
    --output $@
	
$(TONE_MASK): | $(DIFF_MASK) $(TONE_DIFF_XFM) $(TONE_BRAIN)
	-mkdir -p $(dir $@)
	$(QIT_CMD) MaskTransform \
    --input $(word 1,$|) \
    --deform $(word 2,$|) \
    --reference $(word 3,$|) \
    --output $@

################################################################################
# Region-based Segmentation 
################################################################################

define region.copy
$(eval MY_INPUT  := $(1))
$(eval MY_OUTPUT := $(2))

$(MY_OUTPUT): | $(MY_INPUT)
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	mkdir -p $$@
	cp -r $$(word 1, $$|)/rois.nii.gz $$@
	cp -r $$(word 1, $$|)/rois.csv $$@
endef

define region.reg
$(eval INPUT  := $(1))
$(eval REF    := $(2))
$(eval DEFORM := $(3))
$(eval OUTPUT := $(4))

$(OUTPUT): | $(INPUT) $(REF) $(DEFORM)
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	mkdir -p $$@.$$(TMP) 
	$$(call mask.xfm, $$(word 1, $$|)/rois.nii.gz, $$(word 2, $$|), $$(word 3, $$|), $$@.$$(TMP)/rois.nii.gz)
	-cp $$(word 1, $$|)/rois.csv $$@.$$(TMP)/rois.csv
	mv $$@.$$(TMP) $$@
endef

REGION_TARS :=

define region.seg 
$(eval MY_REGIONS := $(1))

$(eval MY_SPACE   := $(call get.space, $(MY_REGIONS)))
$(eval MY_THRESH  := $(MY_REGIONS).thresh)
$(eval MY_ERODE   := $(MY_REGIONS).erode)

$(MY_THRESH): | $(MY_REGIONS) $(MY_SPACE).models.dti
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	mkdir -p $$@.$$(TMP)
	$$(QIT_CMD) VolumeThreshold \
    --input $$(word 2, $$|)/dti_FA.nii.gz \
    --threshold $$(ROI_THRESH) \
    --output $$@.$$(TMP)/thresh.nii.gz
	$$(QIT_CMD) VolumeMask \
    --input $$(word 1, $$|)/rois.nii.gz \
    --mask $$@.$$(TMP)/thresh.nii.gz \
    --output $$@.$$(TMP)/rois.nii.gz
	cp $$(word 1, $$|)/rois.csv $$@.$$(TMP)/rois.csv
	mv $$@.$$(TMP) $$@

$(MY_ERODE): | $(MY_REGIONS)
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	mkdir -p $$@.$$(TMP)
	$$(QIT_CMD) MaskErode \
    --input $$(word 1, $$|)/rois.nii.gz \
    --num $(ROI_ERODE) \
    --output $$@.$$(TMP)/rois.nii.gz
	cp $$(word 1, $$|)/rois.csv $$@.$$(TMP)/rois.csv
	mv $$@.$$(TMP) $$@

REGION_TARS += $(MY_REGIONS)
REGION_TARS += $(MY_THRESH)
REGION_TARS += $(MY_ERODE)
endef

$(TONE_FS): | $(INPUT_TONE)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) VolumeFreeSurfer --input $(word 1,$|) --output $@

$(TONE_FS_REGIONS): | $(TONE_FS)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) FreesurferImport \
    --input $| \
    --output $@ \
    --subfields \
    --segmentations

$(foreach p, $(FS_RGN_NAMES), \
  $(eval $(TONE_FS_REGIONS)/$(p): | $(TONE_FS_REGIONS)))

$(foreach r, $(FS_RGN_NAMES), \
  $(eval $(call region.copy, $(TONE_FS_REGIONS)/$(r), tone.region/fs.$(r))))

$(foreach i, $(FS_RGN_NAMES), \
  $(eval $(call region.reg, \
    tone.region/fs.$(i), $(DIFF_BRAIN), $(DIFF_TONE_XFM), diff.region/fs.$(i))) \
  $(eval $(call region.reg, \
    tone.region/fs.$(i), $(ATLAS_BRAIN), $(ATLAS_TONE_XFM), atlas.region/fs.$(i))))

$(foreach r, $(ATLAS_RGN_NAMES), \
  $(eval $(call region.copy, $(QIT_ATLAS)/region/$(r), atlas.region/$(r))))

$(foreach p, $(ATLAS_RGN_NAMES), \
  $(eval $(call region.reg, \
    $(QIT_ATLAS)/region/$(p), $(DIFF_BRAIN), $(DIFF_ATLAS_XFM), diff.region/$(p))) \
  $(eval $(call region.reg, \
    $(QIT_ATLAS)/region/$(p), $(TONE_BRAIN), $(TONE_ATLAS_XFM), tone.region/$(p))))

$(TBSS_DTI): | $(ATLAS_DTI) $(ATLAS_DTI)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	mkdir -p $@.$(TMP)
	$(call tbss, $(word 1,$|)/dti_FA.nii.gz, $@.$(TMP)/dti_FA.nii.gz)
	$(call tbss, $(word 1,$|)/dti_MD.nii.gz, $@.$(TMP)/dti_MD.nii.gz)
	$(call tbss, $(word 1,$|)/dti_AD.nii.gz, $@.$(TMP)/dti_AD.nii.gz)
	$(call tbss, $(word 1,$|)/dti_RD.nii.gz, $@.$(TMP)/dti_RD.nii.gz)
	$(call tbss, $(word 1,$|)/dti_CP.nii.gz, $@.$(TMP)/dti_CP.nii.gz)
	$(call tbss, $(word 1,$|)/dti_CL.nii.gz, $@.$(TMP)/dti_CL.nii.gz)
	$(call tbss, $(word 1,$|)/dti_CS.nii.gz, $@.$(TMP)/dti_CS.nii.gz)
	mv $@.$(TMP) $@

$(TBSS_FWDTI): | $(ATLAS_FWDTI) $(ATLAS_DTI)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	mkdir -p $@.$(TMP)
	$(call tbss, $(word 1,$|)/dti_FA.nii.gz, $@.$(TMP)/dti_FA.nii.gz)
	$(call tbss, $(word 1,$|)/dti_MD.nii.gz, $@.$(TMP)/dti_MD.nii.gz)
	$(call tbss, $(word 1,$|)/dti_AD.nii.gz, $@.$(TMP)/dti_AD.nii.gz)
	$(call tbss, $(word 1,$|)/dti_RD.nii.gz, $@.$(TMP)/dti_RD.nii.gz)
	$(call tbss, $(word 1,$|)/dti_FW.nii.gz, $@.$(TMP)/dti_FW.nii.gz)
	mv $@.$(TMP) $@

$(TBSS_BTI): | $(ATLAS_BTI) $(ATLAS_DTI)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	mkdir -p $@.$(TMP)
	$(call tbss, $(word 1,$|)/bitensor_tFA.nii.gz, $@.$(TMP)/bitensor_tFA.nii.gz)
	$(call tbss, $(word 1,$|)/bitensor_tMD.nii.gz, $@.$(TMP)/bitensor_tMD.nii.gz)
	$(call tbss, $(word 1,$|)/bitensor_tAD.nii.gz, $@.$(TMP)/bitensor_tAD.nii.gz)
	$(call tbss, $(word 1,$|)/bitensor_tRD.nii.gz, $@.$(TMP)/bitensor_tRD.nii.gz)
	$(call tbss, $(word 1,$|)/bitensor_fMD.nii.gz, $@.$(TMP)/bitensor_fMD.nii.gz)
	$(call tbss, $(word 1,$|)/bitensor_frac.nii.gz, $@.$(TMP)/bitensor_frac.nii.gz)
	mv $@.$(TMP) $@

$(TBSS_NODDI): | $(ATLAS_NODDI) $(ATLAS_DTI)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	mkdir -p $@.$(TMP)
	$(call tbss, $(word 1,$|)/noddi_ficvf.nii.gz, $@.$(TMP)/noddi_ficvf.nii.gz)
	$(call tbss, $(word 1,$|)/noddi_odi.nii.gz, $@.$(TMP)/noddi_odi.nii.gz)
	$(call tbss, $(word 1,$|)/noddi_fiso.nii.gz, $@.$(TMP)/noddi_fiso.nii.gz)
	mv $@.$(TMP) $@

$(TBSS_MCSMT): | $(ATLAS_MCSMT) $(ATLAS_DTI)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	mkdir -p $@.$(TMP)
	$(call tbss, $(word 1,$|)/mcsmt_frac.nii.gz, $@.$(TMP)/mcsmt_frac.nii.gz)
	$(call tbss, $(word 1,$|)/mcsmt_diff.nii.gz, $@.$(TMP)/mcsmt_diff.nii.gz)
	mv $@.$(TMP) $@

$(foreach p, $(QIT_RGN_NAMES), \
 $(eval $(call region.seg, atlas.region/$(p))) \
 $(eval $(call region.seg, diff.region/$(p))) \
 $(eval $(call region.seg, tone.region/$(p))))

################################################################################
# Surface-based Segmentation 
################################################################################

define reg.mesh
$(eval MY_MESHES := $(1))
$(eval MY_XFM    := $(2))
$(eval MY_TAR    := $(3))

$(MY_TAR): | $(MY_MESHES) $(MY_XFM)
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	$$(QIT_CMD) --batch --batch-product \
    --batch-var h=lh,rh \
    --batch-var b=native,reg \
    MeshTransform \
    --input $$(word 1, $$|)/'$$$${h}.$$$${b}'.vtk.gz \
    --attrs coord,white,middle,pial,smoothwm \
    --deform $$(word 2, $$|) \
    --output $$@.$$(TMP)/'$$$${h}.$$$${b}'.vtk.gz
	cp $$(word 1, $|+)/*.txt $$@.$$(TMP)
	mv $$@.$$(TMP) $$@
endef

$(TONE_FS_BRAIN): | $(TONE_FS)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) FreesurferImport \
    --brain \
    --input $| \
    --output $@.$(TMP)
	$(QIT_CMD) MaskRegionsExtract \
    --regions $@.$(TMP)/ribbon.nii.gz \
    --lookup $@.$(TMP)/ribbon.csv \
    --output-mask $@.$(TMP)/%s.nii.gz
	mv $@.$(TMP) $@
$(TONE_FS_BRAIN)/brain.nii.gz: | $(TONE_FS_BRAIN)

$(TONE_FS_SURFS): | $(TONE_FS)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	$(QIT_CMD) FreesurferImport \
    --surfaces \
    --resample $(ATLAS_SPHERE) \
    --input $| \
    --output $@ 

$(eval $(call reg.mesh, \
  $(TONE_FS_SURFS), $(TONE_DIFF_XFM), $(DIFF_FS_SURFS)))

$(eval $(call reg.mesh, \
  $(DIFF_FS_SURFS), $(DIFF_ATLAS_XFM), $(ATLAS_FS_SURFS)))

$(TONE_CORTEX)/morph: | $(TONE_FS_SURFS)
	-mkdir -p $@.$(TMP)
	$(foreach p, area curv sulc thickness volume, \
    cp -rf $(word 1, $|)/lh.reg.attr.$(p).txt.gz $@.$(TMP)/lh_$(p).txt.gz; \
    cp -rf $(word 1, $|)/rh.reg.attr.$(p).txt.gz $@.$(TMP)/rh_$(p).txt.gz;)
	mv $@.$(TMP) $@

$(TONE_CORTEX)/dti: | $(TONE_DTI) $(TONE_DTI_GM) $(TONE_FS_SURFS)
	-mkdir -p $@.$(TMP)
	$(foreach h,lh rh, $(foreach p,FA MD AD RD, \
    $(QIT_CMD) VolumeSampleCortex \
      --input $(word 1, $|)/dti_$(p).nii.gz \
      --gm $(word 2, $|) \
      --name $(p) \
      --mesh $(word 3, $|)/$(h).reg.vtk.gz  \
      --num $@.$(TMP)/$(h)_dti_$(p)_num.txt.gz \
      --median $@.$(TMP)/$(h)_dti_$(p)_median.txt.gz \
      --mean $@.$(TMP)/$(h)_dti_$(p)_mean.txt.gz \
      --std $@.$(TMP)/$(h)_dti_$(p)_std.txt.gz \
      --min $@.$(TMP)/$(h)_dti_$(p)_min.txt.gz \
      --max $@.$(TMP)/$(h)_dti_$(p)_max.txt.gz; ))
	mv $@.$(TMP) $@

$(TONE_CORTEX)/fwdti: | $(TONE_FWDTI) $(TONE_DTI_GM) $(TONE_FS_SURFS)
	-mkdir -p $@.$(TMP)
	$(foreach h,lh rh, $(foreach p,FA MD AD RD FW, \
    $(QIT_CMD) VolumeSampleCortex \
      --input $(word 1, $|)/dti_$(p).nii.gz \
      --gm $(word 2, $|) \
      --name $(p) \
      --mesh $(word 3, $|)/$(h).reg.vtk.gz  \
      --num $@.$(TMP)/$(h)_fwdti_$(p)_num.txt.gz \
      --median $@.$(TMP)/$(h)_fwdti_$(p)_median.txt.gz \
      --mean $@.$(TMP)/$(h)_fwdti_$(p)_mean.txt.gz \
      --std $@.$(TMP)/$(h)_fwdti_$(p)_std.txt.gz \
      --min $@.$(TMP)/$(h)_fwdti_$(p)_min.txt.gz \
      --max $@.$(TMP)/$(h)_fwdti_$(p)_max.txt.gz; ))
	mv $@.$(TMP) $@

$(TONE_CORTEX)/bti: | $(TONE_BTI) $(TONE_DTI_GM) $(TONE_FS_SURFS)
	-mkdir -p $@.$(TMP)
	$(foreach h,lh rh, $(foreach p,tFA tMD tAD tRD frac, \
    $(QIT_CMD) VolumeSampleCortex \
      --input $(word 1, $|)/bitensor_$(p).nii.gz \
      --gm $(word 2, $|) \
      --name $(p) \
      --mesh $(word 3, $|)/$(h).reg.vtk.gz  \
      --num $@.$(TMP)/$(h)_bti_$(p)_num.txt.gz \
      --median $@.$(TMP)/$(h)_bti_$(p)_median.txt.gz \
      --mean $@.$(TMP)/$(h)_bti_$(p)_mean.txt.gz \
      --std $@.$(TMP)/$(h)_bti_$(p)_std.txt.gz \
      --min $@.$(TMP)/$(h)_bti_$(p)_min.txt.gz \
      --max $@.$(TMP)/$(h)_bti_$(p)_max.txt.gz; ))
	mv $@.$(TMP) $@

$(TONE_CORTEX)/noddi: | $(TONE_GM_NODDI) $(TONE_DTI_GM) $(TONE_FS_SURFS)
	-mkdir -p $@.$(TMP)
	$(foreach h,lh rh, $(foreach p,odi ficvf fiso, \
    $(QIT_CMD) VolumeSampleCortex \
      --input $(word 1, $|)/noddi_$(p).nii.gz \
      --gm $(word 2, $|) \
      --name $(p) \
      --mesh $(word 3, $|)/$(h).reg.vtk.gz  \
      --num $@.$(TMP)/$(h)_noddi_$(p)_num.txt.gz \
      --median $@.$(TMP)/$(h)_noddi_$(p)_median.txt.gz \
      --mean $@.$(TMP)/$(h)_noddi_$(p)_mean.txt.gz \
      --std $@.$(TMP)/$(h)_noddi_$(p)_std.txt.gz \
      --min $@.$(TMP)/$(h)_noddi_$(p)_min.txt.gz \
      --max $@.$(TMP)/$(h)_noddi_$(p)_max.txt.gz; ))
	mv $@.$(TMP) $@

$(TONE_CORTEX)/mcsmt: | $(TONE_MCSMT) $(TONE_DTI_GM) $(TONE_FS_SURFS)
	-mkdir -p $@.$(TMP)
	$(foreach h,lh rh, $(foreach p,frac diff, \
    $(QIT_CMD) VolumeSampleCortex \
      --input $(word 1, $|)/mcsmt_$(p).nii.gz \
      --gm $(word 2, $|) \
      --name $(p) \
      --mesh $(word 3, $|)/$(h).reg.vtk.gz  \
      --num $@.$(TMP)/$(h)_mcsmt_$(p)_num.txt.gz \
      --median $@.$(TMP)/$(h)_mcsmt_$(p)_median.txt.gz \
      --mean $@.$(TMP)/$(h)_mcsmt_$(p)_mean.txt.gz \
      --std $@.$(TMP)/$(h)_mcsmt_$(p)_std.txt.gz \
      --min $@.$(TMP)/$(h)_mcsmt_$(p)_min.txt.gz \
      --max $@.$(TMP)/$(h)_mcsmt_$(p)_max.txt.gz; ))
	mv $@.$(TMP) $@

################################################################################
# Tractography-based Segmentation 
################################################################################

define entire.seg
$(eval MY_SPACE    := $(1))

$(eval MY_MASK     := $(MY_SPACE).brain/mask.nii.gz)
$(eval MY_SEED     := $(MY_SPACE).tract/entire/seed.nii.gz)
$(eval MY_CURVES   := $(MY_SPACE).tract/entire/curves.vtk.gz)
$(eval MY_DENSITY  := $(MY_SPACE).tract/entire/density.nii.gz)

$(MY_SEED): | $(MY_SPACE).models.dti
	mkdir -p $$@.$$(TMP) 
	$(eval MY_SEED_TMP := $(MY_SEED).$(TMP).nii.gz)
	$$(QIT_CMD) VolumeFilterMedian \
    --input $$(word 1, $$|)/dti_FA.nii.gz \
    --output $$(MY_SEED_TMP)
	$$(QIT_CMD) VolumeThreshold \
    --input $$(MY_SEED_TMP) \
    --threshold $$(NETWORK_FA) \
    --output $$(MY_SEED_TMP)
	$$(QIT_CMD) MaskOpen \
    --input $$(MY_SEED_TMP) \
    --largest \
    --output $$(MY_SEED_TMP)
	mv $$(MY_SEED_TMP) $$@

$(MY_CURVES): | $(MY_SPACE).models.$(NETWORK_MODEL) $(MY_SEED) $(MY_MASK)
	mkdir -p $$(dir $$@)
	$(QIT_CMD) VolumeModelTrackStreamline $(NETWORK_FLAGS) \
    --step $(NETWORK_STEP) \
    --minlen $(NETWORK_MIN_LEN) \
    --maxlen $(NETWORK_MAX_LEN) \
    --interp $(NETWORK_INTERP) \
    --samplesMask $(NETWORK_SAMPLES) \
    --min $(NETWORK_MIN) \
    --angle $(NETWORK_ANGLE) \
    --threads $(QIT_THREADS) \
    --input $$(word 1, $$|) \
    --seedMask $$(word 2, $$|) \
    --trackMask $$(word 3, $$|) \
    --output $$@

$(MY_DENSITY): | $(MY_CURVES) $(MY_SEED)
	-mkdir -p $$(dir $$@)
	$(QIT_CMD) CurvesDensity \
    --input $$(word 1, $$|) \
    --reference $$(word 2, $$|) \
    --output $$@

$(MY_LIST): | $(MY_DENSITY)
endef

define network.seg
$(eval MY_SPACE   := $(1))
$(eval MY_NAME    := $(2))

$(eval MY_ROIS    := $(MY_SPACE).region/$(MY_NAME))
$(eval MY_CURVES  := $(MY_SPACE).tract/entire/curves.vtk.gz)
$(eval MY_NETWORK := $(MY_SPACE).tract/network.$(MY_NAME))

$(MY_NETWORK): | $(MY_CURVES) $(MY_ROIS)
	mkdir -p $$(dir $$@)
	$(QIT_CMD) CurvesNetwork $(NETWORK_FLAGS) \
    --curves $$(word 1, $$|) \
    --regions $$(word 2, $$|)/rois.nii.gz \
    --lookup $$(word 2, $$|)/rois.csv \
    --output $$@
endef

define bundle.seg
$(eval MY_OUT      := $(1))
$(eval MY_WARP     := $(2))
$(eval MY_INVWARP  := $(3))
$(eval MY_DIR      := $(4))
$(eval MY_NAME     := $(5))

$(eval MY_SPACE    := $(call get.space, $(MY_OUT)))
$(eval MY_MASK     := $(MY_SPACE).brain/mask.nii.gz)
$(eval MY_DATA     := $(MY_DIR)/$(MY_NAME))
$(eval MY_BUNDLE   := $(MY_OUT)/bundles/$(MY_NAME))
$(eval MY_LIST     := $(MY_OUT)/bundles.txt)

$(MY_BUNDLE)/curves.vtk.gz: | $(MY_SPACE).models.$(BUNDLE_MODEL) $(MY_MASK) $(MY_WARP) $(MY_INVWARP) $(MY_DATA)
	$(QIT_ROOT)/bin/qitruntract \
    --models $$(word 1, $$|) \
    --mask $$(word 2, $$|) \
    --warp $$(word 3, $$|) \
    --invwarp $$(word 4, $$|) \
    --atlas $$(word 5, $$|) \
    --memory $(QIT_MEMORY) \
    --threads $(QIT_THREADS) \
    --method $(BUNDLE_METHOD) \
    --interp $(BUNDLE_INTERP)  \
    --factor $(BUNDLE_FACTOR)  \
    --angle $(BUNDLE_ANGLE)  \
    --min $(BUNDLE_MIN) \
    --step $(BUNDLE_STEP) \
    --maxlen $(BUNDLE_MAXLEN) \
    --disperse $(BUNDLE_DISPERSE)  \
    --hybridFactor $(BUNDLE_HYBFAC)  \
    --hybridAngle $(BUNDLE_HYBANGLE)  \
    --hybridMin $(BUNDLE_HYBMIN) \
    --hybridDisperse ${BUNDLE_HYBDISP} \
    --hybridFsum $(BUNDLE_HYBSUM) \
    --hybridSmooth $(BUNDLE_SMOOTH) \
    --projectAngle $(PROJECT_ANGLE) \
    --projectNorm $(PROJECT_NORM) \
    --projectFrac $(PROJECT_FRAC) \
    --projectFsum $(PROJECT_FSUM) \
    --projectSigma $(PROJECT_SIGMA) \
    --termNum $(TERM_NUM) \
    --simpleCount $(SIMPLE_COUNT) \
    --simpleDist $(SIMPLE_DIST) \
    --output $(MY_BUNDLE)

$(MY_LIST): | $(MY_BUNDLE)/curves.vtk.gz
endef

define bundle.project
$(eval MY_OUT      := $(1))
$(eval MY_WARP     := $(2))
$(eval MY_DIR      := $(3))
$(eval MY_NAME     := $(4))

$(eval MY_SPACE    := $(call get.space, $(MY_OUT)))
$(eval MY_MASK     := $(MY_SPACE).brain/mask.nii.gz)
$(eval MY_TOM      := $(MY_DIR)/$(MY_NAME)/tom.nii.gz)
$(eval MY_PROJECT  := $(MY_OUT)/project/$(MY_NAME).nii.gz)
$(eval MY_LIST     := $(MY_OUT)/project.txt)

$(MY_PROJECT): | $(MY_SPACE).models.$(BUNDLE_MODEL) $(MY_MASK) $(MY_WARP) $(MY_TOM)
	$(QIT_CMD) VolumeFibersProjectVectorSoft \
    --smooth \
    --input $$(word 1, $$|) \
    --mask $$(word 2, $$|) \
    --deform $$(word 3, $$|) \
    --reference $$(word 4, $$|) \
    --output $$@

$(MY_LIST): | $(MY_PROJECT)
endef

$(eval $(call entire.seg, diff))
$(eval $(call entire.seg, tone))
$(eval $(call entire.seg, atlas))

$(foreach r, fsa.dkbm fsa.dkgm fs.dkbm fs.dkgm, \
		$(eval $(call network.seg, diff,  $(r))) \
		$(eval $(call network.seg, tone,  $(r))) \
		$(eval $(call network.seg, atlas, $(r))))

$(foreach n, $(shell cat $(BUNDLE_LIST)), $(eval $(call bundle.seg, \
  tone.$(BUNDLE_BASE), $(ATLAS_TONE_XFM), $(TONE_ATLAS_XFM), $(BUNDLE_DIR), $(n))))

$(foreach n, $(shell cat $(BUNDLE_LIST)), $(eval $(call bundle.seg, \
  diff.$(BUNDLE_BASE), $(ATLAS_DIFF_XFM), $(DIFF_ATLAS_XFM), $(BUNDLE_DIR), $(n))))

$(foreach n, $(shell cat $(BUNDLE_LIST)), $(eval $(call bundle.project, \
  tone.$(BUNDLE_BASE), $(TONE_ATLAS_XFM), $(BUNDLE_DIR), $(n))))

$(foreach n, $(shell cat $(BUNDLE_LIST)), $(eval $(call bundle.project, \
  diff.$(BUNDLE_BASE), $(DIFF_ATLAS_XFM), $(BUNDLE_DIR), $(n))))

################################################################################
# Statistical Mapping
################################################################################

define network.map
$(eval MY_NETWORK := $(1))
$(eval MY_NETMAP  := $(MY_NETWORK)/matrices.map)

$(MY_NETMAP): | $(MY_NETWORK)
	qbctmeas $$(word 1, $$|)/matrices $$(word 1, $$|)/nodes.txt $$@
endef

define cortex.map
$(eval MY_PARC := $(1))

$(TONE_CORTEX)/morph.$(MY_PARC).map: | $(TONE_CORTEX)/morph
	-mkdir -p $$@.$(TMP) && mv $$@ $$@.$(BCK)
	$$(foreach h,lh rh, \
    $$(foreach m,area curv sulc thickness volume, \
      $(QIT_CMD) VectsMeasureRegion \
        --input $$(word 1, $$|)/$$(h)_$$(m).txt.gz \
        --name $$(h)_$$(m) \
        --lookup $(QIT_ATLAS)/meshes/$$(h).$(MY_PARC).csv \
        --labels $(QIT_ATLAS)/meshes/$$(h).$(MY_PARC).txt.gz \
        --output $$@.$$(TMP) && )) mv $$@.$$(TMP) $$@

$(TONE_CORTEX)/dti.$(MY_PARC).map: | $(TONE_CORTEX)/dti
	-mkdir -p $$@.$(TMP) && mv $$@ $$@.$(BCK)
	$$(foreach h,lh rh, \
    $$(foreach m,FA MD AD RD, \
      $(QIT_CMD) VectsMeasureRegion \
        --input $$(word 1, $$|)/$$(h)_dti_$$(m)_mean.txt.gz \
        --name $$(h)_fwdti_$$(m) \
        --lookup $(QIT_ATLAS)/meshes/$$(h).$(MY_PARC).csv \
        --labels $(QIT_ATLAS)/meshes/$$(h).$(MY_PARC).txt.gz \
        --output $$@.$$(TMP) && )) mv $$@.$$(TMP) $$@

$(TONE_CORTEX)/fwdti.$(MY_PARC).map: | $(TONE_CORTEX)/fwdti
	-mkdir -p $$@.$(TMP) && mv $$@ $$@.$(BCK)
	$$(foreach h,lh rh, \
    $$(foreach m,FA MD AD RD FW, \
      $(QIT_CMD) VectsMeasureRegion \
        --input $$(word 1, $$|)/$$(h)_fwdti_$$(m)_mean.txt.gz \
        --name $$(h)_fwdti_$$(m) \
        --lookup $(QIT_ATLAS)/meshes/$$(h).$(MY_PARC).csv \
        --labels $(QIT_ATLAS)/meshes/$$(h).$(MY_PARC).txt.gz \
        --output $$@.$$(TMP) && )) mv $$@.$$(TMP) $$@

$(TONE_CORTEX)/noddi.$(MY_PARC).map: | $(TONE_CORTEX)/noddi
	-mkdir -p $$@.$(TMP) && mv $$@ $$@.$(BCK)
	$$(foreach h,lh rh, \
    $$(foreach m,ficvf fiso odi, \
      $(QIT_CMD) VectsMeasureRegion \
        --input $$(word 1, $$|)/$$(h)_noddi_$$(m)_mean.txt.gz \
        --name $$(h)_noddi_$$(m) \
        --lookup $(QIT_ATLAS)/meshes/$$(h).$(MY_PARC).csv \
        --labels $(QIT_ATLAS)/meshes/$$(h).$(MY_PARC).txt.gz \
        --output $$@.$$(TMP) && )) mv $$@.$$(TMP) $$@

$(TONE_CORTEX)/mcsmt.$(MY_PARC).map: | $(TONE_CORTEX)/mcsmt
	-mkdir -p $$@.$(TMP) && mv $$@ $$@.$(BCK)
	$$(foreach h,lh rh, \
    $$(foreach m,frac diff, \
      $(QIT_CMD) VectsMeasureRegion \
        --input $$(word 1, $$|)/$$(h)_mcsmt_$$(m)_mean.txt.gz \
        --name $$(h)_mcsmt_$$(m) \
        --lookup $(QIT_ATLAS)/meshes/$$(h).$(MY_PARC).csv \
        --labels $(QIT_ATLAS)/meshes/$$(h).$(MY_PARC).txt.gz \
        --output $$@.$$(TMP) && )) mv $$@.$$(TMP) $$@
endef

define region.map
$(eval MY_REGIONS := $(1))

$(eval MY_SPACE   := $(call get.space, $(MY_REGIONS)))
$(eval DTI        := $(MY_SPACE).models.dti)
$(eval FWDTI      := $(MY_SPACE).models.fwdti)
$(eval BTI        := $(MY_SPACE).models.bti)
$(eval NODDI      := $(MY_SPACE).models.noddi)
$(eval MCSMT      := $(MY_SPACE).models.mcsmt)

$(MY_REGIONS).dti.map: | $(MY_REGIONS) $(DTI)
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	$$(call mask.ms,$(MY_REGIONS),$(DTI),dti_FA,FA,$$@.$$(TMP))
	$$(call mask.ms,$(MY_REGIONS),$(DTI),dti_MD,MD,$$@.$$(TMP))
	$$(call mask.ms,$(MY_REGIONS),$(DTI),dti_RD,RD,$$@.$$(TMP))
	$$(call mask.ms,$(MY_REGIONS),$(DTI),dti_AD,AD,$$@.$$(TMP))
	$$(call mask.ms,$(MY_REGIONS),$(DTI),dti_CP,CP,$$@.$$(TMP))
	$$(call mask.ms,$(MY_REGIONS),$(DTI),dti_CS,CS,$$@.$$(TMP))
	$$(call mask.ms,$(MY_REGIONS),$(DTI),dti_CL,CL,$$@.$$(TMP))
	mv $$@.$$(TMP) $$@

$(MY_REGIONS).fwdti.map: | $(MY_REGIONS) $(FWDTI)
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	$$(call mask.ms,$(MY_REGIONS),$(FWDTI),dti_FA,tFA,$$@.$$(TMP))
	$$(call mask.ms,$(MY_REGIONS),$(FWDTI),dti_MD,tMD,$$@.$$(TMP))
	$$(call mask.ms,$(MY_REGIONS),$(FWDTI),dti_RD,tRD,$$@.$$(TMP))
	$$(call mask.ms,$(MY_REGIONS),$(FWDTI),dti_AD,tAD,$$@.$$(TMP))
	$$(call mask.ms,$(MY_REGIONS),$(FWDTI),dti_FW,FW,$$@.$$(TMP))
	mv $$@.$$(TMP) $$@

$(MY_REGIONS).bti.map: | $(MY_REGIONS) $(BTI)
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	$$(call mask.ms,$(MY_REGIONS),$(BTI),bitensor_tFA,tFA,$$@.$$(TMP))
	$$(call mask.ms,$(MY_REGIONS),$(BTI),bitensor_tMD,tMD,$$@.$$(TMP))
	$$(call mask.ms,$(MY_REGIONS),$(BTI),bitensor_tRD,tRD,$$@.$$(TMP))
	$$(call mask.ms,$(MY_REGIONS),$(BTI),bitensor_tAD,tAD,$$@.$$(TMP))
	$$(call mask.ms,$(MY_REGIONS),$(BTI),bitensor_fMD,fMD,$$@.$$(TMP))
	$$(call mask.ms,$(MY_REGIONS),$(BTI),bitensor_frac,FRAC,$$@.$$(TMP))
	mv $$@.$$(TMP) $$@

$(MY_REGIONS).noddi.map: | $(MY_REGIONS) $(NODDI)
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	$$(call mask.ms,$(MY_REGIONS),$(NODDI),noddi_ficvf,nFICVF,$$@.$$(TMP))
	$$(call mask.ms,$(MY_REGIONS),$(NODDI),noddi_fiso,nFISO,$$@.$$(TMP))
	$$(call mask.ms,$(MY_REGIONS),$(NODDI),noddi_odi,nODI,$$@.$$(TMP))
	mv $$@.$$(TMP) $$@

$(MY_REGIONS).mcsmt.map: | $(MY_REGIONS) $(MCSMT)
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	$$(call mask.ms,$(MY_REGIONS),$(MCSMT),mcsmt_frac,sFRAC,$$@.$$(TMP))
	$$(call mask.ms,$(MY_REGIONS),$(MCSMT),mcsmt_diff,sDIFF,$$@.$$(TMP))
	mv $$@.$$(TMP) $$@
endef

define region.tbss.map
$(eval MY_REGIONS := $(1))

$(eval DTI        := atlas.models.dti)
$(eval FWDTI      := atlas.models.fwdti)
$(eval BTI        := atlas.models.bti)
$(eval NODDI      := atlas.models.noddi)
$(eval MCSMT      := atlas.models.mcsmt)

$(MY_REGIONS).tbss.dti.map: | $(MY_REGIONS) $(DTI)
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	$$(call tbss.ms,$(MY_REGIONS),$(DTI),dti_FA,FA,$$@.$$(TMP))
	$$(call tbss.ms,$(MY_REGIONS),$(DTI),dti_MD,MD,$$@.$$(TMP))
	$$(call tbss.ms,$(MY_REGIONS),$(DTI),dti_RD,RD,$$@.$$(TMP))
	$$(call tbss.ms,$(MY_REGIONS),$(DTI),dti_AD,AD,$$@.$$(TMP))
	$$(call tbss.ms,$(MY_REGIONS),$(DTI),dti_CP,CP,$$@.$$(TMP))
	$$(call tbss.ms,$(MY_REGIONS),$(DTI),dti_CL,CL,$$@.$$(TMP))
	$$(call tbss.ms,$(MY_REGIONS),$(DTI),dti_CS,CS,$$@.$$(TMP))
	mv $$@.$$(TMP) $$@

$(MY_REGIONS).tbss.fwdti.map: | $(MY_REGIONS) $(FWDTI)
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	$$(call tbss.ms,$(MY_REGIONS),$(FWDTI),dti_FA,tFA,$$@.$$(TMP))
	$$(call tbss.ms,$(MY_REGIONS),$(FWDTI),dti_MD,tMD,$$@.$$(TMP))
	$$(call tbss.ms,$(MY_REGIONS),$(FWDTI),dti_RD,tRD,$$@.$$(TMP))
	$$(call tbss.ms,$(MY_REGIONS),$(FWDTI),dti_AD,tAD,$$@.$$(TMP))
	$$(call tbss.ms,$(MY_REGIONS),$(FWDTI),dti_FW,FW,$$@.$$(TMP))
	mv $$@.$$(TMP) $$@

$(MY_REGIONS).tbss.noddi.map: | $(MY_REGIONS) $(NODDI)
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	$$(call tbss.ms,$(MY_REGIONS),$(NODDI),noddi_ficvf,nFICVF,$$@.$$(TMP))
	$$(call tbss.ms,$(MY_REGIONS),$(NODDI),noddi_fiso,nFISO,$$@.$$(TMP))
	$$(call tbss.ms,$(MY_REGIONS),$(NODDI),noddi_odi,nODI,$$@.$$(TMP))
	mv $$@.$$(TMP) $$@

$(MY_REGIONS).tbss.mcsmt.map: | $(MY_REGIONS) $(MCSMT)
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	$$(call tbss.ms,$(MY_REGIONS),$(MCSMT),mcsmt_frac,sFRAC,$$@.$$(TMP))
	$$(call tbss.ms,$(MY_REGIONS),$(MCSMT),mcsmt_diff,sDIFF,$$@.$$(TMP))
	mv $$@.$$(TMP) $$@
endef

diff.$(BUNDLE_BASE)/project.txt: | $(BUNDLE_LIST)
	mkdir -p $(dir $@)
	cp $(word 1, $|) $@

tone.$(BUNDLE_BASE)/project.txt: | $(BUNDLE_LIST)
	mkdir -p $(dir $@)
	cp $(word 1, $|) $@

define bundles.map
$(eval MY_IN     := $(1))
$(eval MY_LIST   := $(2))

$(MY_IN).txt: | $(MY_LIST)
	mkdir -p $$(dir $$@)
	cp $$(word 1, $$|) $$@	

$(MY_IN).map: | $(MY_IN).txt $(MY_LIST)
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	$(QIT_CMD) CurvesMeasureBatch \
    --threads $(QIT_THREADS) \
    --attrs volume num_curves diff_mean \
            thick_mean thick_std thick_max \
            thick_head thick_tail thick_ends thick_mid \
            density_mean density_median density_sum \
            length_mean length_median length_sum \
            frac_mean frac_median frac_sum \
            mag_mean mag_median mag_sum \
    --names $(MY_LIST) \
    --input $(MY_IN)/%s/curves.vtk.gz \
    --output $$@.$$(TMP)
	mv $$@.$$(TMP) $$@
endef

define bundles.core.map
$(eval MY_IN     := $(1))
$(eval MY_LIST   := $(2))

$(MY_IN).core.map: | $(MY_IN).txt $(MY_LIST)
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	$(QIT_CMD) CurvesMeasureBatch \
    --threads $(QIT_THREADS) \
    --attrs diff_mean length_mean \
            frac_mean frac_median frac_sum \
            mag_mean mag_median mag_sum \
    --names $(MY_LIST) \
    --input $(MY_IN)/%s/core.vtk.gz \
    --output $$@.$$(TMP)
	mv $$@.$$(TMP) $$@
endef

define bundles.along.map
$(eval MY_IN     := $(1))
$(eval MY_LIST   := $(2))

$(MY_IN).along.map: | $(MY_IN).txt $(MY_LIST)
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	$(QIT_CMD) CurvesMeasureAlongBatch \
    --attrs diff_mean frac_mean thick \
    --names $(MY_LIST) \
    --input $(MY_IN)/%s/curves.vtk.gz \
    --output $$@.$$(TMP)
	mv $$@.$$(TMP) $$@
endef

define bundles.param.map
$(eval MY_IN     := $(1))
$(eval MY_LIST   := $(2))
$(eval MY_TYPE   := $(3))

$(eval MY_SPACE  := $(call get.space, $(MY_IN)))
$(eval DTI       := $(MY_SPACE).models.dti)
$(eval FWDTI     := $(MY_SPACE).models.fwdti)
$(eval BTI       := $(MY_SPACE).models.bti)
$(eval NODDI     := $(MY_SPACE).models.noddi)
$(eval MCSMT     := $(MY_SPACE).models.mcsmt)

$(MY_IN).$(MY_TYPE).dti.map: | $(MY_IN).txt $(MY_LIST) $(DTI)
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(DTI),dti_FA,FA,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(DTI),dti_MD,MD,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(DTI),dti_AD,AD,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(DTI),dti_RD,RD,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(DTI),dti_CP,CP,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(DTI),dti_CL,CL,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(DTI),dti_CS,CS,$$@.$$(TMP))
	mv $$@.$$(TMP) $$@

$(MY_IN).$(MY_TYPE).fwdti.map: | $(MY_IN).txt $(MY_LIST) $(FWDTI)
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(FWDTI),dti_FA,tFA,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(FWDTI),dti_MD,tMD,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(FWDTI),dti_AD,tAD,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(FWDTI),dti_RD,tRD,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(FWDTI),dti_FW,FW,$$@.$$(TMP))
	mv $$@.$$(TMP) $$@

$(MY_IN).$(MY_TYPE).bti.map: | $(MY_IN).txt $(MY_LIST) $(BTI)
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(BTI),bitensor_tFA,tFA,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(BTI),bitensor_tMD,tMD,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(BTI),bitensor_tAD,tAD,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(BTI),bitensor_tRD,tRD,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(BTI),bitensor_fMD,fMD,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(BTI),bitensor_frac,FRAC,$$@.$$(TMP))
	mv $$@.$$(TMP) $$@

$(MY_IN).$(MY_TYPE).noddi.map: | $(MY_IN).txt $(MY_LIST) $(NODDI)
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(NODDI),noddi_ficvf,nFICVF,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(NODDI),noddi_fiso,nFISO,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(NODDI),noddi_odi,nODI,$$@.$$(TMP))
	mv $$@.$$(TMP) $$@

$(MY_IN).$(MY_TYPE).mcsmt.map: | $(MY_IN).txt $(MY_LIST) $(MCSMT)
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(MCSMT),mcsmt_frac,sFRAC,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(MCSMT),mcsmt_diff,sDIFF,$$@.$$(TMP))
	mv $$@.$$(TMP) $$@
endef

define project.param.map
$(eval MY_IN     := $(1))
$(eval MY_LIST   := $(2))
$(eval MY_TYPE   := $(3))

$(eval MY_SPACE  := $(call get.space, $(MY_IN)))
$(eval DTI       := $(MY_SPACE).models.dti)
$(eval FWDTI     := $(MY_SPACE).models.fwdti)
$(eval BTI       := $(MY_SPACE).models.bti)
$(eval NODDI     := $(MY_SPACE).models.noddi)
$(eval MCSMT     := $(MY_SPACE).models.mcsmt)

$(MY_IN).$(MY_TYPE).dti.map: | $(MY_IN).txt $(MY_LIST) $(DTI)
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(DTI),dti_FA,FA,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(DTI),dti_MD,MD,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(DTI),dti_AD,AD,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(DTI),dti_RD,RD,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(DTI),dti_CP,CP,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(DTI),dti_CL,CL,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(DTI),dti_CS,CS,$$@.$$(TMP))
	mv $$@.$$(TMP) $$@

$(MY_IN).$(MY_TYPE).fwdti.map: | $(MY_IN).txt $(MY_LIST) $(FWDTI)
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(FWDTI),dti_FA,tFA,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(FWDTI),dti_MD,tMD,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(FWDTI),dti_AD,tAD,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(FWDTI),dti_RD,tRD,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(FWDTI),dti_FW,FW,$$@.$$(TMP))
	mv $$@.$$(TMP) $$@

$(MY_IN).$(MY_TYPE).bti.map: | $(MY_IN).txt $(MY_LIST) $(BTI)
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(BTI),bitensor_tFA,tFA,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(BTI),bitensor_tMD,tMD,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(BTI),bitensor_tAD,tAD,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(BTI),bitensor_tRD,tRD,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(BTI),bitensor_fMD,fMD,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(BTI),bitensor_frac,FRAC,$$@.$$(TMP))
	mv $$@.$$(TMP) $$@

$(MY_IN).$(MY_TYPE).noddi.map: | $(MY_IN).txt $(MY_LIST) $(NODDI)
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(NODDI),noddi_ficvf,nFICVF,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(NODDI),noddi_fiso,nFISO,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(NODDI),noddi_odi,nODI,$$@.$$(TMP))
	mv $$@.$$(TMP) $$@

$(MY_IN).$(MY_TYPE).mcsmt.map: | $(MY_IN).txt $(MY_LIST) $(MCSMT)
	-@[ -e $$@ ] && mv -f $$@ $$@.$$(BCK)
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(MCSMT),mcsmt_frac,sFRAC,$$@.$$(TMP))
	$$(call $(MY_TYPE).ms,$(MY_IN),$(MY_LIST),$(MCSMT),mcsmt_diff,sDIFF,$$@.$$(TMP))
	mv $$@.$$(TMP) $$@
endef

tone.fs.map: | $(TONE_FS)
	-@[ -e $@ ] && mv -f $@ $@.$(BCK)
	qfsmeas $(word 1,$|) $@

$(foreach t, $(REGION_TARS), \
	$(eval $(call region.map, $(t))))

$(foreach p, aparc aparc.a2009s hcp.mmp, \
  $(eval $(call cortex.map, $(p))))

$(foreach p, $(QIT_RGN_NAMES), \
  $(eval $(call region.tbss.map,atlas.region/$(p))))

$(foreach r, fsa.dkbm fsa.dkgm fs.dkbm fs.dkgm, \
  $(eval $(call network.map, diff.tract/network.$(r))) \
  $(eval $(call network.map, tone.tract/network.$(r))))

$(foreach s, tone diff, \
	$(eval $(call bundles.map, $(s).$(BUNDLE_BASE)/bundles, $(BUNDLE_LIST))) \
	$(eval $(call bundles.core.map, $(s).$(BUNDLE_BASE)/bundles, $(BUNDLE_LIST))) \
	$(eval $(call bundles.along.map, $(s).$(BUNDLE_BASE)/bundles, $(BUNDLE_LIST))) \
	$(foreach p, whole.vertex whole.voxel whole.core along.vertex along.voxel along.core density, \
		$(eval $(call bundles.param.map, $(s).$(BUNDLE_BASE)/bundles, $(BUNDLE_LIST), $(p)))) \
  $(eval $(call bundles.param.map, $(s).$(BUNDLE_BASE)/project, $(BUNDLE_LIST), density)))

################################################################################
# Meta-targets
################################################################################

list:
	@$(MAKE) -pRrq -f $(lastword $(MAKEFILE_LIST)) : 2>/dev/null | awk -v RS= -F: '/^# File/,/^# Finished Make data base/ {if ($$1 !~ "^[#.]") {print $$1}}' | sort | egrep -v -e '^[^[:alnum:]]' -e '^$@$$' | xargs

dust:
	find . -name '*.tmp.*' -exec rm -rf {} \;

scrub: | dust
	find . -name '*.bck.*' -exec rm -rf {} \;

.PHONY: | list all dust scrub
.DELETE_ON_ERROR:

################################################################################
# End of file 
################################################################################
