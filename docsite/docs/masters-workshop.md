# Masters Program Workshop for QIT

This page provides a guide for a QIT workshop held as part of the [USC
Master's of Science in Neuroimaging program](http://niin.usc.edu USC
Master's of Science in Neuroimaging program).  The goal of the workshop is
to provide an opportunity to work directly with neuroimaging data and to
gain hands-on experience with various diffusion MRI techniques.  The
workshop doesn't require any prior programming experience, and it is
designed to work on most laptops.  That said, an interested reader can also
use this as a starting point for more advanced scripting of image analysis
tasks or integrating QIT in compute-heavy workflows with other datasets
using the LONI Pipeline.

## Setup

If you have not already, you can start by downloading the latest version from
the [Installation](install.md) page.  You may also benefit from checking
out the **Concepts** pages linked on the left side panel, which will help you
become familiar with the design and capabilities of QIT.  Next, you should
download the sample dataset, decompress the archive, and keep it somewhere
accessible:

[![Download](images/download-icon.png){: style="height:25px;width:25px"} Download qit-workshop-average.zip](https://github.com/cabeen/qit/releases/download/data-2021-03-21/qit-workshop-average.zip)

## Dataset

The data included in this workshop was created by averaging a collection of
MRIs from typical adults, which were acquired as part of the
[Human Connectome Project](http://www.humanconnectomeproject.org).  The images
and data were moderately downsampled from the full resolution to make the
workshop run smoothly, but most anatomical features present in the original
data can be found in this data as well.  The archive contains the following
groups of data:

-  `models.dti`: the diffusion tensor image volume (a Volume dataset)
-  `models.xfib`: the multi-fiber image volume (a Volume dataset)
-  `models.noddi`: the NODDI image volume (a Volume dataset)
-  `regions`: various atlas image labels (a set of Mask datasets)
-  `tracts`: various tractography reconstructions (a set of Curves datasets)
-  `meshes`: various surface models (a set of Mesh datasets)
-  `masks`: brain and white matter masks (Mask datasets)

`Volume` and `Mask` datasets are typically stored in the nifti format with the `nii.gz` extension.  `Mask` files typically have a `csv` stored alongside them that indicates the name of each label.  Diffusion models are stored as multiple volumes inside a directory, and while you can load the individual files, to do tractography or render glyphs you need to load the directory itself, e.g. `models.xfib`.  `Mesh` and `Curves` data are ''both'' stored as VTK files with the `vtk.gz` extension.  `qitview` should detect which is which based on the filename, but you should make sure the file loader correctly identifies the dataset type (the type can be changed using the combo box to the left of the filename).  You can read more about the various dataset types [[Datasets|here]].

## Agenda

In this section, we provide instructions and video guides (no audio) for using QIT in various diffusion MRI tasks.  You will only need the data provided in the above mentioned archive, and after the first step, the others are mostly self-contained.  In total, the tutorial should take from 30-45 minutes to complete, but please feel free to take your time experimenting and to ask questions along the way!  

## Start Up and Loading Data

The first step will be to start `qitview` and load some data.  You should
start by double clicking `qitview` (on macOS) or `qitview.py` (if you are on
Windows).  You should see a terminal window open that prints status messages
followed by a graphical interface with several panels.  You can load data by
dragging data into the `qitview` window (or by navigating through the
**File>Load Files** menu item).  You should load the file named
`models.dti/dti_S0.nii.gz` and try exploring the dataset.  ''Tip: you can
quickly change slices by hold **Alt** while clicking and dragging on an
image slice.''

<video width="720" controls>
  <source src="../videos/Niin.workshop.load.mp4" type="video/mp4">
Your browser does not support the video tag.
</video>
<br> 

## Visualizing Surface Anatomy

Next, we'll examine some 3D models of anatomy represented by surfaces.  You
should load the data in the `meshes` directory into the viewer, along with
an image volume for reference.  The surfaces show the boundaries of various
anatomical structures.  The larger structure is a white matter surface, and
the smaller structures are subcortical nuclei. ''Tip: try to change the
coloring of the subcortical meshes to the label attribute like in the
video.''

<video width="720" controls>
  <source src="../videos/Niin.workshop.mesh.mp4" type="video/mp4">
Your browser does not support the video tag.
</video>
<br> 

## Visualizing Diffusion Glyphs

Next, we'll examine 3D rendering that depict diffusion modeling.  You should
load `models.dti` and `models.xfib` and follow the steps shown in the video
below to create diffusion glyphs.  ''Tip:  watch for the step with a combo
box incrementing from 0 to 1.  This nudges the glyphs off the image slice so
they are completely visible.''

<video width="720" controls>
  <source src="../videos/Niin.workshop.glyphs.mp4" type="video/mp4">
Your browser does not support the video tag.
</video>
<br> 

## Visualizing Track Density

Next, we'll go over how to visualize fiber bundles in two ways.  You should
load a fiber bundle model, e.g. `tracts/arcuate.vtk.gz` and a reference
volume.  You can see the bundle is represented by curves.  The video shows
how to convert it to a density map that can be viewed on top of the
reference volume.  ''Tip: try changing the colormap and opacity of the
density map.''

<video width="720" controls>
  <source src="../videos/Niin.workshop.density.mp4" type="video/mp4">
Your browser does not support the video tag.
</video>
<br> 

## Comparing Diffusion Models

Next we'll create new tractography reconstructions and compare two different
diffusion models.  You should load two diffusion models, `models.dti` and
`models.xfib`, along with a corpus callosum region mask
`regions/fsa.ccwm/rois.nii.gz`.  The video shows how you can open the
tractography module and create tracks for each of the diffusion models, and
then observe the differences in the depicted anatomy.  ''Tip: the toggle
keyboard shortcut (Command-T on macOS or Control-T otherwise) will let you
switch on and off the visibility of the highlighted datasets.  You can also
try changing the tractography parameters, e.g. increasing the samples or
changing angle threshold.''

<video width="720" controls>
  <source src="../videos/Niin.workshop.cc.mp4" type="video/mp4">
Your browser does not support the video tag.
</video>
<br> 

## Extracting Tracks Manually

Next, we'll show how you can extract tracks using a manually drawn seed
mask.  You should open a diffusion model volume and a reference dataset.
`models.dti/dti_RGB.nii.gz` is an efficient way to visualize white matter
anatomy.  You can also load atlas labels, e.g.
`regions/jhu.regions/rois.nii.gz`.  You should create a new mask for seeding
by right clicking on the model data and selecting the option shown in the
video.  You can then draw on the mask: first, select the mask in the **Data
Panel**, then click the **Edit**  tab in the **Control Panel** to see the
options, then drag the mouse over the volume to draw the mask while holding
down **Alt+Control**.  Try drawing a region of interest on the corticospinal
tract in the brainstem.  ''Tip: you can erase a mask by dragging the mouse
while holding down **Alt+Control+Shift**.''

<video width="720" controls>
  <source src="../videos/Niin.workshop.cst.mp4" type="video/mp4">
Your browser does not support the video tag.
</video>
<br> 

## Filtering Tracks Interactively

Finally, we'll try interactively filtering tracks.  You should load a
reference volume and one of the fiber bundles, e.g. `tracts/arcuate.vtk.gz`.
You should create a sphere object as indicated in the video.  You can then
modify the sphere by first selecting it in the the **Data Panel** and then
clicking and dragging the mouse while holding down either **Alt** (to move
the sphere) or **Alt+Shift** (to resize the sphere).   Then, you can filter
the fiber bundle using the sphere by selecting the curves in the **Data
Panel** and opening the **Edit** tab in the **Control Panel**.   You should
select the sphere as an ''include'' criteria as shown in the video.  Now
only the subset of curves that go through the sphere will be shown.  You can
similarly set the sphere to ''exclude'' curves, as shown in the video.
''Tip: the filtering only changes what is shown on the screen.  If you want
to save the filtered curves, you must either **Export** them or **Retain**
them using the buttons in the **Edit** tab.''

<video width="720" controls>
  <source src="../videos/Niin.workshop.filter.mp4" type="video/mp4">
Your browser does not support the video tag.
</video>
<br> 

## Fin
 
That's it, congratulations on finishing the workshop!  Hopefully, you're now more familiar with a few common diffusion MRI tasks, and you have an idea of how to use `qitview` for other tasks you may encounter in the future.  Many of the tasks you completed in `qitview` can be scripted or run on the command line using the `qit` program.  To learn more, please feel free to explore the rest of the documentation on this site, experiment the features in `qitview`, or [mailto:rcabeen@loni.usc.edu email me] with any questions!

