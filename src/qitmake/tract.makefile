default: all

ifndef INPUT
$(error usage: make INPUT=curves.vtk.gz REF=ref.nii.gz OUTPUT=outdir)
endif

ifndef REF 
$(error usage: make INPUT=curves.vtk.gz REF=ref.nii.gz OUTPUT=outdir)
endif

ifndef OUTPUT
$(error usage: make INPUT=curves.vtk.gz REF=ref.nii.gz OUTPUT=outdir)
endif

CURVES        := $(OUTPUT)/curves.vtk.gz
PROTO         := $(OUTPUT)/proto.vtk.gz
SEED_MASK     := $(OUTPUT)/seed.nii.gz
SEED_VECTS    := $(OUTPUT)/seeds.txt.gz
INCLUDE_MASK  := $(OUTPUT)/include.nii.gz
EXCLUDE_MASK  := $(OUTPUT)/exclude.nii.gz
END_MASK      := $(OUTPUT)/end.nii.gz
TOM           := $(OUTPUT)/tom.nii.gz

EXTEND        ?= 5
DILATE        ?= 3
SMOOTH        ?= -1 
CLUSTER       ?= 1
DENSITY       ?= 5
SEED          ?= 1 

$(SEED_MASK): $(INPUT) $(REF)
	mkdir -p $(dir $@)
	qit --verbose --debug CurvesMask \
    --input $(word 1, $+) \
    --refvolume $(word 2, $+) \
    --output $@
	qit --verbose --debug MaskDilate \
    --input $@ \
    --num $(DILATE) \
    --output $@

$(EXCLUDE_MASK): $(SEED_MASK)
	qit --verbose --debug MaskDilate \
    --input $(word 1, $+) \
    --num $(DILATE) \
    --output $@
	qit --verbose --debug MaskInvert \
    --input $@ \
    --output $@

$(SEED_VECTS): $(SEED_MASK)
	qit --verbose --debug MaskSampleVects \
    --input $(word 1, $+) \
    --count $(SEED) \
    --limit 100000 \
    --output $@

$(END_MASK): $(INPUT) $(REF)
	mkdir -p $(dir $@)
	qit --verbose --debug CurvesEndpointMask \
    --input $(word 1, $+) \
    --reference $(word 2, $+) \
    --num $(EXTEND) \
    --dilate $(DILATE) \
    --output $@

$(INCLUDE_MASK): $(INPUT) $(REF)
	mkdir -p $(dir $@)
	qit --verbose --debug CurvesMidpointMask \
    --input $(word 1, $+) \
    --reference $(word 2, $+) \
    --num $(EXTEND) \
    --dilate $(DILATE) \
    --output $@

$(TOM): $(INPUT) $(REF)
	mkdir -p $(dir $@)
	qit --verbose --debug CurvesOrientationMap \
    --input $(word 1, $+) \
    --refvolume $(word 2, $+) \
    --smooth \
    --sigma $(SMOOTH) \
		--vector \
		--orient \
    --output $@

$(CURVES): $(TOM) $(INCLUDE_MASK) $(END_MASK)
	mkdir -p $(dir $@)
	qit --verbose --debug VolumeModelTrackStreamline \
    --interp Trilinear \
    --input $(word 1, $+) \
    --includeMask $(word 2, $+) \
    --includeAddMask $(word 3, $+) \
    --output $@
	qit --verbose --debug CurvesClusterQuickBundle \
    --input $@ \
    --thresh $(CLUSTER) \
    --centers $@
	qit CurvesSegmentAlong \
    --input $@ \
    --density 5 \
    --outputProto $(PROTO) \
    --output $@
	qit CurvesAttributes \
    --input $@ \
    --retain coord,label \
    --output $@

all: $(INCLUDE_MASK) $(EXCLUDE_MASK) $(END_MASK) $(SEED_MASK) $(SEED_VECTS) $(TOM) $(CURVES)
