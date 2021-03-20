# Automated image analysis with QIT

Welcome! This is a guide for automated quantitative analysis of diffusion MRI
data using QIT.   If you have collected many diffusion MRI scans in an imaging
study, this workflow can help you extract quantitative measures of structural
anatomical characteristics across your group and ultimately use these measures
to explore their statistical relationship with demographic and behavioral
variables.  

## Overview

The workflow is an end-to-end tool that supports a variety of ways to analyze
diffusion MRI data, in which you start from raw imaging data and produce
geometric models and quantitative summaries of tissue microstructure,
morphometry, and connectivity.  Specifically, you can perform region-based and
tractography-based analysis and combine these with tissue parameter mapping
using diffusion tensor imaging and advanced multi-shell approaches.  In
addition to diffusion MRI, the workflow has components that streamline the use
of state-of-the-art packages for morphometric analysis using T1-weighted MRI
data, and further, allow them to be combined with diffusion MRI data.  If you
are new to diffusion MRI and would like to learn more, [Diffusion Tensor
Imaging: a Practical Handbook](https://www.springer.com/gp/book/9781493931170)
and [NMR in Biomedicine: Special Issue on Diffusion MRI of the
brain](https://onlinelibrary.wiley.com/toc/10991492/2019/32/4) are both good
starting points.

In the following sections, this guide will cover how to install the necessary
software on your computer, how to prepare your imaging data, how to run various
analyses, and how to combine the results across subjects.

## Installing software

This section will guide you through the installation of QIT and its
dependencies.  The workflow is designed for the command line, and if you need
need to learn about it, you can check out the tutorials at [Software
Carpentry](https://swcarpentry.github.io/shell-novice/).   Note: while the
workflow primarily uses QIT, it also depends on several other 3rd party tools
that are designed to run only on Linux and Mac.  So while QIT itself can run on
Windows, to run the complete workflow on Windows you would need a
virtualization solution, such as
[VMware](https://www.vmware.com/products.html),
[VirtualBox](https://www.virtualbox.org), or the [Windows Subsystem for
Linux](https://docs.microsoft.com/en-us/windows/wsl/install-win10).

### Installing QIT

You should first follow the instructions on the [Installation](install.md) page, and make sure that the `bin` directory is included in your shell `PATH` variable.  You can read [https://linuxize.com/post/how-to-add-directory-to-path-in-linux/ this page] to learn more about adding a directory to your path.  After installing, you can check that things worked by running the following command and seeing if it produces something analogous (your version should be this one or later):

```
$ qit --version <br>
QIT build revision: 2005\:2019, build time: 03/25/2020 05\:09 PM`
```

### Installing dependencies

Next, you should install the other software packages required by the workflow,
which are two common and publicly available 3rd party neuroimaging tools:
[DTI-TK](http://dti-tk.sourceforge.net/pmwiki/pmwiki.php) and
[FSL](https://fsl.fmrib.ox.ac.uk/fsl/fslwiki).  You can follow the installation
instructions on the linked pages, and afterwards, you can test that they are
correctly installed by trying these commands:

```
$ which bedpostx
/opt/share/fsl-5.0.10/bin/bedpostx
```

```
$ which TVMean
/opt/share/dtitk/bin/TVMean
```

Note: there is a [GPU version of FSL
bedpostx](https://users.fmrib.ox.ac.uk/~moisesf/Bedpostx_GPU/) that can
dramatically speed things up.  If you install it using the instructions on the
linked page, you can enable it in the QIT workflow by adding `GPU=1` to the
`qitdiff` command.

### Installing more dependencies (optional)

As an optional step, if want to use T1 MRI data in the analysis, you should also install [https://surfer.nmr.mgh.harvard.edu/ FreeSurfer] and [https://stnava.github.io/ANTs/ ANTs].  These are not required for most parts of the pipeline, but they can offer more precise segmentation of certain brain structures, which are described in more detail later in the guide.  You can follow the installation instructions on the linked pages, and you can test that they are correctly installed by trying these commands:

```
$ which recon-all
/opt/share/freesurfer/bin/recon-all
```

```
$ which antsRegistrationSyn.sh
/opt/share/ANTs-install/bin/antsRegistrationSyn.sh
```

## Preparing your data

This section provides instructions for converting your data into a format that
is compatible with the workflow.  You may already have data in a suitable
format, so not all of these steps will be necessary.  The workflow expects the
input to be a diffusion-weighted MRI in the
[NIfTI](https://nifti.nimh.nih.gov/) and associated text files for the
b-vectors and b-values.  If you need data to try out, you can download this example clinical quality single shell dataset:

[![Download](images/download-icon.png){: style="height:25px;width:25px"} http://cabeen.io/download/dmri.tutorial.data.zip](http://cabeen.io/download/dmri.tutorial.data.zip)

If you'd like to skip the processing, you can also check out these example
results:

[![Download](images/download-icon.png){: style="height:25px;width:25px"}
http://cabeen.io/download/qitdiff.demo.tar.gz](http://cabeen.io/download/qitdiff.demo.tar.gz)

### File format conversion

Data from the scanner typically is stored in the [DICOM file
format](https://www.dicomstandard.org/), which stores the all data from each
session in a directory that combines imaging, sequence parameters, and patient
information across many files contained within.  You will need to convert DICOM
data to the [NIfTI file format](https://nifti.nimh.nih.gov/), a simpler format
that is more amenable to imaging research.  There are a few tools available for
converting between NIfTI and DICOM, but a good starting point is the
[dcm2niix](https://github.com/rordenlab/dcm2niix) tool.  The linked page shares
instructions for installing the software and applying it to your DICOM data to
produce NIfTI volumes.  Afterwards, you should be left with `nii.gz` files from
your session, one of which will be a multi-channel diffusion MRI.  You should
also find `bvecs` and `bvals` files, which list the b-vectors and b-values
sequence parameters from the scan, respectively.

### Artifact Correction

There are a variety of artifact that can be corrected in diffusion MRI, and it
is recommended that you apply any corrections that are compatible with your
data collection scheme.  QIT offers some basic corrections for denoising,
signal drift correction, and motion correction, and these will be noted in the
next section.   However, if possible, it is recommended that you use advanced
preprocessing tools when possible.  In particular, if you have collected data
with an additional set of reversed-phase encoding scans, you can use [FSL
EDDY](https://fsl.fmrib.ox.ac.uk/fsl/fslwiki/eddy) to greatly improve the
quality of your data by simultaneously correcting for motion, eddy-current,
susceptibility-induced geometric distortion.  You should explore using an
OpenMP accelerated version, as it can be quite time consuming. To apply it to
your data, you can use the [FSL](https://fsl.fmrib.ox.ac.uk/fsl/fslwiki/eddy)
or the [HCP
pipelines](https://www.humanconnectome.org/software/hcp-mr-pipelines).  Once
you've finished processing it, you should find the corrected diffusion MRI and
b-vectors, which will be named `eddy_out.nii.gz` and
`eddy_out.eddy_rotated_bvecs`, respectively.  The b-values are not changed by
the correction, so you should use the raw values.

If you are interested in other artifact correction features of QIT, you can
check out these modules: `VolumeFilterNLM`, `VolumeDwiCorrectDrift`, and
`VolumeDwiCorrect`, which provide denoising, drift correction, and motion
correction, respectively.

## General considerations

In this section and the following three, we describe the different ways that you can run the QIT diffusion workflow.  We'll start by discussing the general interface to the script, and then describe specific types of analysis in detail.  The primary interface to the QIT diffusion workflow is a program named `qitdiff`.  If you run this command without any arguments, you'll find a usage page, like so:

```
$ qitdiff

  Name: qitdiff

  Description:

    Process diffusion-weighted imaging data using the qit diffusion workflow.
    The first time you run this script, you must provide the DWI, bvecs, and
    bvals for your subject.  After that, you only need to specify the targets. 

  Usage: 

    qitdiff [opts] --subject subject [targets]

  Input Parameters (must be include in the first run, but not afterwards):

     --dwi dwi.nii.gz:   the diffusion weighted MRI file, stored in NIFTI
     --bvecs bvecs:      the diffusion b-vectors file, stored as text
     --bvals bvals:      the diffusion b-values file, stored as text
     --subject subject:  the subject directory where the results will be saved

  Optional Parameters (may be included in the first run, but not afterwards):

     --denoise <num>:    denoise the dwi with the given noise level
     --mask <fn>:        use the given precomputed brain mask
     --bet <num>:        use the given FSL BET fraction parameter 
     --erode <num>:      erode the brain mask by the given number of voxels
     --motion:           correct for motion and create a summary report
     --nomatch:          skip gradient matching step
     --tone <fn>:        specify a T1-weighted MRI
     --freesurfer <dir>: use the given precomputed freesurfer results
     --tracts <dir>:     include user-defined tracts in the workflow 

  Author: Ryan Cabeen
```

There are three flags for specifying the input to the workflow: `--dwi`,
`--bvecs`, and `--bvals`.  The b-vectors and b-values can be organized in
column or row form, and QIT will automatically determine the coordinate
transform necessary to align the b-vectors to the imaging data (so there is no
need to flip or transpose b-vectors).  You must also specify a subject
directory with `--subject`, but if you omit this flag, the current working
directory will be used.  There are several other optional flags that are listed
after the required flags.  You may skip these for now if you like.

Besides the input data, you must also specify one or more **targets**.  A
target is a string that identifies what you want the workflow to produce, e.g.
regional summaries of FA or fiber bundle geometry.  In the next section of the
tutorial we'll describe the different targets that are available.  But to give
a concrete example, you could produce region DTI parameter statistics with the
JHU atlas using a command like so:

```
$ qitdiff --subject qitsubject --dwi scan/dwi.nii.gz --bvecs scan/bvecs.txt --bvals scan/bvals.txt diff.region/jhu.labels.dti.map
```

One of the features of the QIT workflow is that you can add additional analyses and re-use the previous work you did.  So for example, if you wanted to add TBSS to your analysis, you could simply run a command like this:

```
$ qitdiff --subject qitsubject atlas.region/jhu.labels.tbss.dti.map
```

For example, this will skip the data import and atlas registration steps, as
they were already completed.   Note that you can also omit the flags for the
input data, since they have already been imported.

## Region-of-interest analysis

Region-of-interest (ROI) analysis is a simple and widely used way to summarize
diffusion MRI data.   In this approach, the data is spatially normalized with
an atlas that contains a parcellation of brain areas, stored in a 3D mask.
Then, diffusion model parameters are statistically summarized in each ROI of
the atlas; for example, by computing the average fractional anisotropy in each
brain area.  There are quite a few variations on this approach, and QIT
supports many of them.  For example, you can combine the ROI approach with
[Tract-based Spatial Statistics
(TBSS)](https://fsl.fmrib.ox.ac.uk/fsl/fslwiki/TBSS), you can apply erosion of
the masks, and you can compute the statistics in either native or atlas space.
Each of these possibilities is available with a different **target** that you
can provide to `qitdiff`.  Here is a table summarizing the different kinds of
ROI targets that are available:

- **diff.region/jhu.labels.dti.map**:
    - DTI parameters statistics for each region in native space
- **diff.region/jhu.labels.erode.dti.map**: 
    - DTI parameters statistics for each region in native space with additional erosion step
- **atlas.region/jhu.labels.dti.map**: 
    - DTI parameters statistics for each region in atlas space
- **atlas.region/jhu.labels.erode.dti.map**: 
    - DTI parameters statistics for each region in atlas space with an additional erosion step
- **atlas.region/jhu.labels.tbss.dti.map**: 
    - DTI parameters statistics for each region in atlas space with additional TBSS processing

The above examples use the Johns Hopkins University white matter atlas, but
there are many other alternatives in `qitdiff`.  Below is a table summarizing
these other options.  There are targets available for these, in which you
simply replace `jhu.labels` in the targets above.

- **jhu.labels**: Johns Hopkins University deep white matter atlas)
- **jhu.tracts**: Johns Hopkins University white matter tract atlas
- **fsa.ccwm**: FreeSurfer-based corpus callosum parcellation
- **fsa.scgm**: FreeSurfer-based subcortical gray matter
- **fsa.dkwm**: FreeSurfer-based superficial white matter
- **hox.sub**: Harvard-Oxford subcortical parcellation
- **ini.bstem**: USC INI brainstem parcellation
- **cit.amy**: Caltech amygdala subfields
- **cit.sub**: Caltech subcortical parcellation

Furthermore, there are many types of diffusion parameters that can be used
in an ROI analysis.  The examples listed above used diffusion tensor imaging
(DTI) parameters, but if you have multi-shell data, there are other
possibilities supported by `qitdiff`. The table below lists these other
possibilities, and you can create targets for them by substituting `dti` with
the appropriate model identifier.

- **dti **:  Diffusion Tensor Imaging (DTI)
- **fwdti**:  Free-water Elimination DTI (with a fixed diffusivity ball)
- **dki**:  Diffusion Kurtosis Imaging
- **noddi**:  Neurite Orientation Dispersion and Density Imaging
- **mcsmt**:  Multi-compartment Spherical Mean Technique
- **bti**:  Bi-tensor Imaging (DTI with an unconstrained ball)

For a concrete example, if you wanted to compute NODDI parameters in subcortical brain areas, you could run something like so:

```
$ qitdiff --subject qitsubject --dwi scan/dwi.nii.gz --bvecs scan/bvecs.txt --bvals scan/bvals.txt diff.region/fsa.scgm.noddi.map
```

With all of these options, you may be wondering what to pick, and in this case,
a good starting point is the target `atlas.region/jhu.region.tbss.dti.map`.
This is a fairly standardized approach that summarizes DTI parameters in white
matter areas from the JHU atlas with preprocessing using TBSS.  For example,
this is similar to the protocol used in the [ENIGMA
network](http://enigma.ini.usc.edu/protocols/dti-protocols/) (note: the
template and registration algorithm are not identical).

## Tractography-based analysis

Tractography analysis is an alternative approach for diffusion MRI analysis
that is focused on modeling connectivity as opposed to regions.  In this
approach, geometric models of white matter connectivity are reconstructed from
fiber orientation data estimated from diffusion MRI.  With sufficiently high
quality data, this approach can extract the major pathways of the brain, known
as fiber bundles.  The QIT diffusion workflow includes such a bundle-specific
analysis, enabling the quantitative characterization of both whole bundles and
subdivisions along their length.  These can be run using the following
`qitdiff` targets:

- **diff.tract/bundles.map**:
    - whole bundle parameters, such as volume, length, etc.
- **diff.tract/bundles.dti.map**:  
    - whole bundle DTI parameters, such as FA, MD, etc.
- **diff.tract/bundles.along.dti.map**: 
    - along-bundle DTI parameters, e.g. FA measured at each of a sequence of bundle subdivisions

Similar to ROI analysis, you can also extract multi-shell diffusion MRI
parameters using any of the model identifiers from the previous section.  For a
concrete example, if you wanted to compute NODDI parameters along each bundle,
you could run something like so:

```
$ qitdiff --subject qitsubject --dwi scan/dwi.nii.gz --bvecs scan/bvecs.txt --bvals scan/bvals.txt diff.tract/bundles.along.noddi.map
```

## Multi-modal analysis

Besides diffusion MRI, the QIT workflow has integrated morphometric analysis
using T1-weighted MRI.  These steps require ANTs and FreeSurfer to be installed
(as described above).  After that, you can perform cortical surface based
analysis using FreeSurfer using the following command:

```
$ qitdiff --subject qitsubject --tone t1.nii.gz tone.fs.map
```

If you've already run FreeSurfer, you can also import the previous results, e.g.

```
$ qitdiff --subject qitsubject --freesurfer fs_subject_dir tone.fs.map
```

You can also perform a multi-modal image analysis that combines the T1-weighted
and diffusion MRI data.  For example, you could compute DTI parameter
statistics in subject-specific subcortical and superficial white matter ROIs
using this command:

```
$ qitdiff --subject qitsubject --dwi scan/dwi.nii.gz --bvecs scan/bvecs.txt --bvals scan/bvals.txt --tone t1.nii.gz tone.region/fs.scgm.dti.map tone.region/fs.dkwm.dti.map
```

There are various other T1-related targets available, which are summarized here:

- **tone.fs.brain**:
    - Freesurfer brain image and tissue types converted into nifti
- **tone.fs.region**:
    - Freesurfer regions-of-interest converted into nifti
- **tone.fs.surfaces**:
    - Freesurfer meshes converted into VTK
- **diff.fs.brain**:
    - Freesurfer brain image and tissue types deformed into native diffusion space
- **diff.fs.region**:
    - Freesurfer regions-of-interest deformed into native diffusion space
- **diff.fs.surfaces**:
    - Freesurfer meshes deformed into native diffusion space
- **tone.region/fs.scgm.dti.map**:
    - DTI parameters statistics for each Freesurfer subcortical ROI
- **tone.region/fs.dkwm.dti.map**:
    - DTI parameters statistics for each Freesurfer Desikan white matter ROI
- **tone.region/fs.dkgm.dti.map**:
    - DTI parameters statistics for each Freesurfer Desikan gray matter ROI

## Aggregating results

Once you've finished processing your data, you may want to combine the results into data tables, that is, aggregating metrics from all of the research subjects into a spreadsheet.  The QIT workflow was designed to make this easy by saving results in a standardized format.  You may notice that each target ends in `map`; this indicates that the target is a directory that contains summary statistics.  Each `map` directory contains an array of CSV files store simple name-value pairs.  For example, the bundle-specific analysis produces this CSV file among many others:

```
$ head diff.tract/bundles.map/volume.csv 
name,value
lh_acoustic,13570.0
lh_thal_tempinf,9663.0
rh_arc_ant,51518.0
rh_thal_pma,21428.0
rh_med_lem,11603.0
rh_aft,6183.0
...
```

Suppose you have many subjects, and you have organized the data such that each subject has a QIT workflow subject directory, e.g. `test_103818`, inside a directory named `subjects`.  Further, you have a list of subject identifiers that correspond to the subject directory names, and these names are stored one-per-line in a text file named `sids.txt`.  Then, you can create a data table and inspect the output using these commands:

```
$ qit --verbose MapCat --pattern subjects/%{subject}/diff.tract/bundles.map/volume.csv  --vars subject=sids.txt --skip --output bundles.volume.csv
$ cat bundles.volume.csv | grep lh_acoustic | head
test_103818,lh_acoustic,13386.0
test_105923,lh_acoustic,17255.0
test_111312,lh_acoustic,12189.0
test_114823,lh_acoustic,15710.0
test_115320,lh_acoustic,14599.0
...
```

The resulting table is in a *long* format, but you can also convert it to a
*wide* one:

```
$ qit TableWiden --input bundles.volume.csv --output bundles.volume.wide.csv
$ head bundles.volume.wide.csv
subject,lh_acoustic,lh_arc,...
test_103818,13386.0,8950.0,...
test_105923,17255.0,13213.0,...
test_111312,12189.0,11033.0,...
...
```

In this table, each column is a different brain area, and each row is a
subject.  We used bundle volume in the example, but the same approach works for
all of the other metrics stored in `map` directories.  If you would like to
create tables for many different variables, there is a more complex but
powerful script called `qitmaketables` that you can check out.

## Acknowledgements

If you find QIT is valuable in your work, we ask that you clearly
[acknowledge](citation.md) it any publications or grant proposals.  This
is greatly appreciated, as it improves the reproducibility of your findings,
and it helps our team maintain resources for continued support and development.
You can cite QIT by including a link to the main website at
[http://cabeen.io/qitwiki](http://cabeen.io/qitwiki) and including the
following citation in the manuscript:

```lang-none
Cabeen RP, Laidlaw DH, Toga AW. 2018. Quantitative Imaging Toolkit: Software
for Interactive 3D Visualization, Data Exploration, and Computational Analysis
of Neuroimaging Datasets. Proceedings of the Annual Meeting of the Society for
Magnetic Resonance in Medicine (ISMRM).  Paris, France 2018 (p. 2854)
```

If you use the region-of-interest analysis, you can include the following reference, which describes and evaluates these components:

```lang-none
Cabeen, R.P., Bastin, M.E. and Laidlaw, D.H., 2017. A Comparative evaluation of
voxel-based spatial mapping in diffusion tensor imaging. Neuroimage, 146,
pp.100-112.
```

If you used the tractography-based analysis, you can include the following references, which describe and evaluate the methods for tractography and template construction:

```lang-none
Cabeen, R.P., Bastin, M.E. and Laidlaw, D.H., 2016. Kernel regression
estimation of fiber orientation mixtures in diffusion MRI. Neuroimage, 127,
pp.158-172.
```

```lang-none
Cabeen, R.P., Toga, A.W., 2020. Reinforcement tractography: a hybrid approach
for robust segmentation of complex fiber bundles. International Symposium on
Biomedical Imaging (ISBI) 2020
```

Besides QIT references, you should also please cite the other dependent tools and resources where appropriate:

```lang-none 
Jenkinson, M., Beckmann, C.F., Behrens, T.E., Woolrich, M.W. and Smith, S.M.,
2012. Fsl. Neuroimage, 62(2), pp.782-790.
```

```lang-none 
Behrens, T.E., Berg, H.J., Jbabdi, S., Rushworth, M.F. and Woolrich, M.W.,
2007. Probabilistic diffusion tractography with multiple fibre orientations:
What can we gain?. Neuroimage, 34(1), pp.144-155.
```

```lang-none 
Zhang, H., Yushkevich, P.A., Alexander, D.C. and Gee, J.C., 2006. Deformable
registration of diffusion tensor MR images with explicit orientation
optimization. Medical image analysis, 10(5), pp.764-785.
```

```lang-none 
Zhang, S., Peng, H., Dawe, R.J. and Arfanakis, K., 2011. Enhanced ICBM
diffusion tensor template of the human brain. Neuroimage, 54(2), pp.974-984.
```

```lang-none 
Zhang, H., Yushkevich, P.A., Alexander, D.C. and Gee, J.C., 2006. Deformable
registration of diffusion tensor MR images with explicit orientation
optimization. Medical image analysis, 10(5), pp.764-785.
```

```lang-none 
Basser, P.J., Mattiello, J. and LeBihan, D., 1994. MR diffusion tensor
spectroscopy and imaging. Biophysical journal, 66(1), pp.259-267.
```

```lang-none 
Zhang, H., Schneider, T., Wheeler-Kingshott, C.A. and Alexander, D.C., 2012.
NODDI: practical in vivo neurite orientation dispersion and density imaging of
the human brain. Neuroimage, 61(4), pp.1000-1016.
```

```lang-none 
Hoy, A.R., Koay, C.G., Kecskemeti, S.R. and Alexander, A.L., 2014. Optimization
of a free water elimination two-compartment model for diffusion tensor imaging.
Neuroimage, 103, pp.323-333.
```

```lang-none
Kaden, E., Kelm, N.D., Carson, R.P., Does, M.D. and Alexander, D.C., 2016.
Multi-compartment microscopic diffusion imaging. NeuroImage, 139, pp.346-359.
```

```lang-none 
Fieremans, E., Jensen, J. H., & Helpern, J. A. (2011). White matter
characterization with diffusional kurtosis imaging. Neuroimage, 58(1), 177-188.
```

```lang-none 
Sepehrband, F., Cabeen, R.P., Choupan, J., Barisano, G., Law, M., Toga, A.W.
and Alzheimer's Disease Neuroimaging Initiative, 2019. Perivascular space fluid
contributes to diffusion tensor imaging changes in white matter. NeuroImage,
197, pp.243-254.
```

```lang-none 
Mori, S., Oishi, K., Jiang, H., Jiang, L., Li, X., Akhter, K., Hua, K., Faria,
A.V., Mahmood, A., Woods, R. and Toga, A.W., 2008. Stereotaxic white matter
atlas based on diffusion tensor imaging in an ICBM template. Neuroimage, 40(2),
pp.570-582.
```

```lang-none 
Smith, S.M., Jenkinson, M., Johansen-Berg, H., Rueckert, D., Nichols, T.E.,
Mackay, C.E., Watkins, K.E., Ciccarelli, O., Cader, M.Z., Matthews, P.M. and
Behrens, T.E., 2006. Tract-based spatial statistics: voxelwise analysis of
multi-subject diffusion data. Neuroimage, 31(4), pp.1487-1505.
```

```lang-none 
Pauli, W.M., Nili, A.N. and Tyszka, J.M., 2018. A high-resolution probabilistic
in vivo atlas of human subcortical brain nuclei. Scientific data, 5, p.180063.
```

```lang-none 
Tyszka, J.M. and Pauli, W.M., 2016. In vivo delineation of subdivisions of the
human amygdaloid complex in a high‐resolution group template. Human brain
mapping, 37(11), pp.3979-3998.
```

```lang-none 
Tang, Y., Sun, W., Toga, A.W., Ringman, J.M. and Shi, Y., 2018. A probabilistic
atlas of human brainstem pathways based on connectome imaging data. Neuroimage,
169, pp.227-239.
```

```lang-none 
Desikan, R.S., Ségonne, F., Fischl, B., Quinn, B.T., Dickerson, B.C., Blacker,
D., Buckner, R.L., Dale, A.M., Maguire, R.P., Hyman, B.T. and Albert, M.S.,
2006. An automated labeling system for subdividing the human cerebral cortex on
MRI scans into gyral based regions of interest. Neuroimage, 31(3), pp.968-980.
```

```lang-none 
Fischl, B., 2012. FreeSurfer. Neuroimage, 62(2), pp.774-781.
```

```lang-none 
Avants, B.B., Tustison, N. and Song, G., 2009. Advanced normalization tools
(ANTS). Insight j, 2(365), pp.1-35.
```

## Closing

This concludes the tutorial for the QIT diffusion workflow.  This was written to provide only a brief overview, so you may be left wondering about how some things work.  If so, please feel free to reach out with any questions or thoughts via [cabeen@gmail.com](mailto:cabeen@gmail.com).

