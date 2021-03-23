# QIT Module Library

QIT has many modules available on the command line interface through `qit` and
in the 3D viewer through `qitview`.  This page below provides an index of the
publicly available modules.
<hr/>
## AffineCompose 

### Description:

  Compose two affine transforms 

### Required Input Arguments:

```lang-none

  --inner <Affine>      input inner   
```

```lang-none

  --outer <Affine>      input outer   
```

### Required Output Arguments:

```lang-none

  --output <Affine>      output affine   
```

### Author:

  Ryan Cabeen 

<hr/>
## AffineConvert 

### Description:

  Convert an affine transform between file formats 

### Required Input Arguments:

```lang-none

  --input <Affine>      input affine   
```

### Required Output Arguments:

```lang-none

  --output <Affine>      output affine   
```

### Author:

  Ryan Cabeen 

<hr/>
## AffineCreate 

### Description:

  Create an affine transform based on user specified parameters 

### Optional Parameter Arguments:

```lang-none

  --transX <double>      translation in x (Default: 0.0)   
```

```lang-none

  --transY <double>      translation in y (Default: 0.0)   
```

```lang-none

  --transZ <double>      translation in z (Default: 0.0)   
```

```lang-none

  --rotX <double>      rotation axis in x (Default: 0.0)   
```

```lang-none

  --rotY <double>      rotation axis in y (Default: 0.0)   
```

```lang-none

  --rotZ <double>      rotation axis in z (Default: 0.0)   
```

```lang-none

  --rotA <double>      rotation angle (Default: 0.0)   
```

```lang-none

  --scaleX <double>      scaleCamera in x (Default: 1.0)   
```

```lang-none

  --scaleY <double>      scaleCamera in y (Default: 1.0)   
```

```lang-none

  --scaleZ <double>      scaleCamera in z (Default: 1.0)   
```

```lang-none

  --skewX <double>      skew in x (Default: 0.0)   
```

```lang-none

  --skewY <double>      skew in y (Default: 0.0)   
```

```lang-none

  --skewZ <double>      skew in z (Default: 0.0)   
```

### Required Output Arguments:

```lang-none

  --output <Affine>      output affine   
```

### Author:

  Ryan Cabeen 

<hr/>
## AffineInvert 

### Description:

  Invert an affine transform 

### Required Input Arguments:

```lang-none

  --input <Affine>      input affine   
```

### Required Output Arguments:

```lang-none

  --output <Affine>      output affine   
```

### Author:

  Ryan Cabeen 

<hr/>
## AffinePrintInfo 

### Description:

  Print basic information about an affine transform 

### Required Input Arguments:

```lang-none

  --input <Affine>      the input affine   
```

### Author:

  Ryan Cabeen 

<hr/>
## AffineReduce 

### Description:

  Reduce an affine transform to a simpler affine transform in one of several 
  possible ways 

### Required Input Arguments:

```lang-none

  --input <Affine>      an affine transform   
```

### Optional Parameter Arguments:

```lang-none

  --translation       include the translation part   
```

```lang-none

  --linear       include the linear part   
```

```lang-none

  --orthogonalize       orthogonalize the linear part   
```

### Required Output Arguments:

```lang-none

  --output <Affine>      output affine   
```

### Author:

  Ryan Cabeen 

<hr/>
    0 [qit] starting analysis
    0 [qit] parsing arguments
<hr/>
## CurvesAttributeMap 

### Description:

  Compute an attribute map, that is, a volume that represents the most likely 
  value of a given curves attribute for each voxel. 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Optional Input Arguments:

```lang-none

  --refvolume <Volume>      input reference volume (exclusive with refmask)   
```

```lang-none

  --refmask <Mask>      input reference mask (exclusive with refvolume)   
```

### Optional Parameter Arguments:

```lang-none

  --attr <String>      the name of attribute to extract from the curves (Default: attr)   
```

```lang-none

  --threads <int>      the number of threads (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output attribute map   
```

### Optional Output Arguments:

```lang-none

  --outputDensity <Volume>      output density map   
```

```lang-none

  --outputMask <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesAttributes 

### Description:

  Manipulate curves vertex attributes.  Operations support comma-delimited 
  lists 

### Required Input Arguments:

```lang-none

  --input <Curves>      the input curves   
```

### Optional Parameter Arguments:

```lang-none

  --copy <String>      copy attribute (x=y syntax)   
```

```lang-none

  --rename <String>      rename attribute (x=y syntax)   
```

```lang-none

  --remove <String>      remove attribute (- for all)   
```

```lang-none

  --retain <String>      retain the given attributes and remove others)   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      the output curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesBox 

### Description:

  Compute a bounding box from curves 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Required Output Arguments:

```lang-none

  --output <Solids>      output box   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesCat 

### Description:

  Concatenate multiple curves datasets to a single larger dataset 

### Required Input Arguments:

```lang-none

  --a <Curves>      input curves a   
```

### Optional Input Arguments:

```lang-none

  --b <Curves>      input curves b   
```

```lang-none

  --c <Curves>      input curves c   
```

```lang-none

  --d <Curves>      input curves d   
```

```lang-none

  --e <Curves>      input curves e   
```

```lang-none

  --f <Curves>      input curves f   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      output curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesCatBatch 

### Description:

  Concatenate a batch of curves files 

### Required Input Arguments:

```lang-none

  --input <Curves>      specify any number of input curves files   
```

### Optional Parameter Arguments:

```lang-none

  --reduce <Number>      reduce the number of curves in each input by the given fraction   
```

```lang-none

  --label       label the curves with a unique identifier   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      specify the output curves file   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesCatPattern 

### Description:

  Concatenate curves using a filename pattern 

### Required Parameter Arguments:

```lang-none

  --pattern <String>      a pattern to read curves filenames (should contains %s for substitution)   
```

```lang-none

  --names <String>      a list of identifiers (will be substituted in the input pattern)   
```

### Optional Parameter Arguments:

```lang-none

  --along <String>      optionally use the given attribute to account for along bundle     parameterization   
```

```lang-none

  --bundleName <String>      use the given bundle attribute/field name (Default: bundle_name)   
```

```lang-none

  --alongName <String>      use the given along attribute/field name (Default: along_name)   
```

```lang-none

  --bundleIndex <String>      use the given bundle attribute/field index (Default: bundle_index)   
```

```lang-none

  --alongIndex <String>      use the given along attribute/field index (Default: along_index)   
```

### Required Output Arguments:

```lang-none

  --curves <Curves>      the output combined curves   
```

```lang-none

  --table <Table>      the table to store labels   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesClusterFeature 

### Description:

  Cluster curves using simple curve features (length, endpoints, position, 
  shape) using K-means 

### Required Input Arguments:

```lang-none

  --input <Curves>      the input curves   
```

### Optional Parameter Arguments:

```lang-none

  --length       include the length   
```

```lang-none

  --endpoints       include tensor product of endpoints   
```

```lang-none

  --gaussian       include the Gaussian shape   
```

```lang-none

  --num <Integer>      the number of clusters (Default: 2)   
```

```lang-none

  --thresh <Double>      the threshold size for clusters (enables DP-means)   
```

```lang-none

  --iters <Integer>      the maxima number of iterations (Default: 100)   
```

```lang-none

  --restarts <Integer>      the number of restarts   
```

```lang-none

  --relabel       relabel to reflect cluster size   
```

```lang-none

  --largest       keep only the largest cluster   
```

### Advanced Parameter Arguments:

```lang-none

  --iters <Integer>      the maxima number of iterations (Default: 100)   
```

```lang-none

  --restarts <Integer>      the number of restarts   
```

```lang-none

  --relabel       relabel to reflect cluster size   
```

```lang-none

  --largest       keep only the largest cluster   
```

### Optional Output Arguments:

```lang-none

  --output <Curves>      the output curves   
```

```lang-none

  --protos <Curves>      the prototypical curves for each cluster   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesClusterGraph 

### Description:

  Curve bundle segmentation with graph-based thresholding 

### Required Input Arguments:

```lang-none

  --input <Curves>      the input curves   
```

### Optional Parameter Arguments:

```lang-none

  --dist <String>      the name of the inter-curve distance (Default: meanhaus)   
```

```lang-none

  --thresh <double>      the threshold for grouping (Default: 1.0)   
```

```lang-none

  --largest       retain the largest group   
```

```lang-none

  --relabel       relabel to reflect cluster size   
```

### Advanced Parameter Arguments:

```lang-none

  --largest       retain the largest group   
```

```lang-none

  --relabel       relabel to reflect cluster size   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      the output curves   
```

### Author:

  Ryan Cabeen 

### Citation:

  Felzenszwalb, P. F., & Huttenlocher, D. P. (2004). Efficient graph-based 
  image segmentation. International journal of computer vision, 59(2), 167-181. 

<hr/>
## CurvesClusterHierarchical 

### Description:

  Cluster curves with hierarchical clustering. 

### Required Input Arguments:

```lang-none

  --input <Curves>      the input curves   
```

### Optional Parameter Arguments:

```lang-none

  --dist <String>      the name of the inter-curve distance (Default: meanhaus)   
```

```lang-none

  --num <Integer>      the number of clusters   
```

```lang-none

  --thresh <Double>      the threshold for grouping   
```

```lang-none

  --density <Double>      resample the curves to speed up computation   
```

```lang-none

  --epsilon <Double>      simplify the curves to speed up computation   
```

```lang-none

  --relabel       relabel to reflect cluster size   
```

### Advanced Parameter Arguments:

```lang-none

  --density <Double>      resample the curves to speed up computation   
```

```lang-none

  --epsilon <Double>      simplify the curves to speed up computation   
```

```lang-none

  --relabel       relabel to reflect cluster size   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      the output curves   
```

### Author:

  Ryan Cabeen 

### Citation:

  Zhang, S., Correia, S., & Laidlaw, D. H. (2008). Identifying white-matter 
  fiber bundles in DTI data using an automated proximity-based fiber-clustering 
  method. IEEE transactions on visualization and computer graphics, 14(5), 
  1044-1053. 

<hr/>
## CurvesClusterQuickBundle 

### Description:

  Cluster curves with the quicksbundles algorithm 

### Required Input Arguments:

```lang-none

  --input <Curves>      the input curves   
```

### Optional Parameter Arguments:

```lang-none

  --subset <Integer>      take a subset of curves before clustering (not applicable if you provide     fewer)   
```

```lang-none

  --samples <Integer>      the number of sample vertices (Default: 5)   
```

```lang-none

  --thresh <Double>      the separation threshold (Default: 1000.0)   
```

```lang-none

  --relabel       relabel to reflect cluster size   
```

### Advanced Parameter Arguments:

```lang-none

  --relabel       relabel to reflect cluster size   
```

### Optional Output Arguments:

```lang-none

  --output <Curves>      the output curves   
```

```lang-none

  --centers <Curves>      the output centers (the centroid curves for each cluster)   
```

### Author:

  Ryan Cabeen 

### Citation:

  Garyfallidis, E., Brett, M., Correia, M. M., Williams, G. B., & Nimmo-Smith, 
  I. (2012). Quickbundles, a method for tractography simplification. Frontiers 
  in neuroscience, 6, 175. 

<hr/>
## CurvesClusterSCPT 

### Description:

  Cluster curves with a sparse closest point transform. 

### Required Input Arguments:

```lang-none

  --input <Curves>      the input curves   
```

### Optional Parameter Arguments:

```lang-none

  --subset <Integer>      take a subset of curves before clustering (not applicable if you provide     fewer)   
```

```lang-none

  --thresh <Double>      the threshold size for clusters (Default: 22.0)   
```

```lang-none

  --largest       retain only the largest cluster   
```

```lang-none

  --fraction <Double>      retain only clusters that are a given proportion of the total (zero to one)   
```

```lang-none

  --preeps <Double>      preprocess by simplifying the curves (Default: 1.0)   
```

```lang-none

  --lmsub <Integer>      the number of curves for landmarking (Default: 5000)   
```

```lang-none

  --lmeps <Double>      the simplifification threshold for landmarking (Default: 1.0)   
```

```lang-none

  --lmthresh <Double>      the landmarking threshold (Default: 30.0)   
```

```lang-none

  --iters <Integer>      the maxima number of iterations (Default: 100)   
```

```lang-none

  --restarts <Integer>      the number of restarts   
```

```lang-none

  --num <Integer>      the number of clusters (Default: 2)   
```

### Advanced Parameter Arguments:

```lang-none

  --preeps <Double>      preprocess by simplifying the curves (Default: 1.0)   
```

```lang-none

  --lmsub <Integer>      the number of curves for landmarking (Default: 5000)   
```

```lang-none

  --lmeps <Double>      the simplifification threshold for landmarking (Default: 1.0)   
```

```lang-none

  --lmthresh <Double>      the landmarking threshold (Default: 30.0)   
```

```lang-none

  --restarts <Integer>      the number of restarts   
```

```lang-none

  --num <Integer>      the number of clusters (Default: 2)   
```

### Optional Output Arguments:

```lang-none

  --output <Curves>      the output curves   
```

```lang-none

  --landmarks <Vects>      the computed landmarks   
```

```lang-none

  --protos <Curves>      the prototypical curves for each cluster   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesClusterSpectral 

### Description:

  Cluster curves with spectral clustering. 

### Required Input Arguments:

```lang-none

  --input <Curves>      the input curves   
```

### Optional Parameter Arguments:

```lang-none

  --dist <String>      the name of the inter-curve distance (Default: meanhaus)   
```

```lang-none

  --num <Integer>      the number of clusters   
```

```lang-none

  --density <Double>      resample the curves to speed up computation   
```

```lang-none

  --epsilon <Double>      simplify the curves to speed up computation   
```

```lang-none

  --relabel       relabel to reflect cluster size   
```

### Advanced Parameter Arguments:

```lang-none

  --density <Double>      resample the curves to speed up computation   
```

```lang-none

  --epsilon <Double>      simplify the curves to speed up computation   
```

```lang-none

  --relabel       relabel to reflect cluster size   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      the output curves   
```

### Author:

  Ryan Cabeen 

### Citation:

  Cluster curves with spectral clustering.  O'Donnell, L. J., & Westin, C. F. 
  (2007). Automatic tractography segmentation using a high-dimensional white 
  matter atlas. IEEE transactions on medical imaging, 26(11), 1562-1575. 

<hr/>
## CurvesCompare 

### Description:

  Compare a pair of curves objects quantitatively 

### Required Input Arguments:

```lang-none

  --left <Curves>      input curves   
```

```lang-none

  --right <Curves>      the other curves   
```

### Optional Parameter Arguments:

```lang-none

  --delta <double>      the volume resolution (Default: 1.0)   
```

```lang-none

  --thresh <double>      the density threshold (Default: 0.5)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesConnectRegions 

### Description:

  Extract the connecting segments between the given regions 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

```lang-none

  --regions <Mask>      input regions (should have two distinct labels)   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      output curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesConvert 

### Description:

  Convert curves between file formats 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      output curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesCreateHelix 

### Description:

  Create a curves dataset consisting of a helix bundle 

### Optional Parameter Arguments:

```lang-none

  --radius <double>      the radius of the helix (Default: 10.0)   
```

```lang-none

  --length <double>      the length of one loop of the helix (Default: 5.0)   
```

```lang-none

  --loops <int>      the number of loops in the helix (Default: 2)   
```

```lang-none

  --steps <int>      the number of steps sampled along each loop of the helix (Default: 100)   
```

```lang-none

  --sigma <double>      the width of the helix bundle (Default: 1.0)   
```

```lang-none

  --samples <int>      the number of samples in the helix bundle (Default: 1)   
```

```lang-none

  --startx <double>      the starting position in x (Default: 0.0)   
```

```lang-none

  --starty <double>      the starting position in y (Default: 0.0)   
```

```lang-none

  --startz <double>      the starting position in z (Default: 0.0)   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      output curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesCrop 

### Description:

  Crop curves to retain only portions inside the selection 

### Required Input Arguments:

```lang-none

  --input <Curves>      the input curves   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

```lang-none

  --solids <Solids>      some solids   
```

### Optional Parameter Arguments:

```lang-none

  --invert       invert the selection   
```

```lang-none

  --and       require that curves are inside all solids (logical AND)   
```

```lang-none

  --attr <String>      specify an attribute for cropping   
```

```lang-none

  --above <Double>      require that the given attribute at each vertex is above this value   
```

```lang-none

  --below <Double>      require that the given attribute at each vertex is below this value   
```

```lang-none

  --equals <Double>      require that the given attribute approximately equals this value   
```

```lang-none

  --delta <Double>      the threshold distance for approximate cropping (Default: 0.001)   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      the output curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesCull 

### Description:

  Cull redundant curves.  This can be done using SCPT (scpt) or pairwise 
  distance-based clustering (haus, cham, end, or cutoff).  DistanceExtrinsic 
  based clustering is much slower but technically more accurate. 

### Required Input Arguments:

```lang-none

  --input <Curves>      the input curves   
```

### Optional Parameter Arguments:

```lang-none

  --dist <String>      the name of the inter-curve distance (scpt, haus, cham, end, or cutoff).      Pairwise distances can be symmeterized by adding mean, min, or max to the     name (except scpt). (Default: scpt)   
```

```lang-none

  --thresh <Double>      the threshold for removal in mm (but the exact meaning of this depends on     the distance metric, so be careful) (Default: 1.5)   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      the output curves   
```

### Author:

  Ryan Cabeen 

### Citation:

  Zhang, S., Correia, S., & Laidlaw, D. H. (2008). Identifying white-matter 
  fiber bundles in DTI data using an automated proximity-based fiber-clustering 
  method. IEEE transactions on visualization and computer graphics, 14(5), 
  1044-1053. 

<hr/>
## CurvesDensity 

### Description:

  Compute a volumetric density of curves.  This works by find the voxels that 
  intersect the curves and accumulating how many curves intersected each voxel. 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Optional Input Arguments:

```lang-none

  --reference <Volume>      input reference volume (exclusive with refmask)   
```

### Optional Parameter Arguments:

```lang-none

  --type <CurvesDensityType>      specify the type of normalization (Options: Count, CountNorm, Color,     ColorNorm) (Default: Count)   
```

```lang-none

  --delta <double>      when no reference sampling is present, create a volume with this voxel size     (Default: 1.0)   
```

### Advanced Parameter Arguments:

```lang-none

  --delta <double>      when no reference sampling is present, create a volume with this voxel size     (Default: 1.0)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output density volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesEndpointMask 

### Description:

  Extract curve endpoints and create a mask 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Optional Input Arguments:

```lang-none

  --reference <Volume>      input reference volume   
```

### Optional Parameter Arguments:

```lang-none

  --head <int>      curve head label (Default: 1)   
```

```lang-none

  --tail <int>      curve tail label (Default: 2)   
```

```lang-none

  --num <int>      use the given number of vertices from the endpoints (Default: 1)   
```

```lang-none

  --dilate <int>      dilate the endpoint mask (Default: 0)   
```

```lang-none

  --mode       apply a mode filter   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesEndpoints 

### Description:

  Extract curve endpoints and return them as vects 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Optional Parameter Arguments:

```lang-none

  --head       curve head vertices   
```

```lang-none

  --tail       curve tail vertices   
```

### Required Output Arguments:

```lang-none

  --output <Vects>      output vects   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesExtract 

### Description:

  Extract only curves that match a given attribute value 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Optional Parameter Arguments:

```lang-none

  --attr <String>      the attribute name of the label used to select (Default: label)   
```

```lang-none

  --which <String>      which labels to extract, e.g. 1,2,3:5 (Default: )   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      output curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesFeatures 

### Description:

  Compute features of curves and add them as vertex attributes 

### Required Input Arguments:

```lang-none

  --input <Curves>      the input curves   
```

### Optional Parameter Arguments:

```lang-none

  --tangent       compute tangents   
```

```lang-none

  --color       compute vertex colors   
```

```lang-none

  --arclength       compute per-vertex arclength   
```

```lang-none

  --index       compute per-vertex index   
```

```lang-none

  --fraction       compute per-vertex fraction along curve   
```

```lang-none

  --count       compute per-curve vertex count   
```

```lang-none

  --length       compute per-curve length   
```

```lang-none

  --frame       compute the per-vertex frenet frame   
```

```lang-none

  --curvature       compute the per-vertex curvatures   
```

```lang-none

  --density       compute the per-vertex density   
```

```lang-none

  --stats       compute the per-curve statistics of vertex curvature and density   
```

```lang-none

  --all       compute all possible features   
```

```lang-none

  --voxel <double>      specify a voxel size used for computing density (Default: 1.0)   
```

### Advanced Parameter Arguments:

```lang-none

  --all       compute all possible features   
```

```lang-none

  --voxel <double>      specify a voxel size used for computing density (Default: 1.0)   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      the output curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesFilterKernel 

### Description:

  Filter curves with kernel regression.  This uses a non-parametric statistical 
  approach to smooth the curves 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Optional Parameter Arguments:

```lang-none

  --density <Double>      resample with a given vertex density (mm/vertex), otherwise the original     arclength sampling is used   
```

```lang-none

  --sigma <Double>      use the given spatial bandwidth (Default: 4.0)   
```

```lang-none

  --order <int>      specify the order of the local approximating polynomial (Default: 0)   
```

```lang-none

  --thresh <Double>      the threshold for excluding data from local regression (Default: 0.05)   
```

```lang-none

  --attrs <String>      the attributes to smooth (comma separated list) (Default: coord)   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      output curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesFilterLoess 

### Description:

  Filter curves with lowess smoothing 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Optional Parameter Arguments:

```lang-none

  --density <Double>      resample with a given vertex density (mm/vertex), otherwise the original     arclength sampling is used   
```

```lang-none

  --num <int>      use the given neighborhood size for local estimation (Default: 5)   
```

```lang-none

  --order <Integer>      use a given polynomial order for local estimation (Default: 2)   
```

```lang-none

  --endpoints       smooth endpoints   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      output curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesFilterPolynomial 

### Description:

  Filter cures with polynomial splines 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Optional Parameter Arguments:

```lang-none

  --density <Double>      resample with a given vertex density (mm/vertex), otherwise the original     arclength sampling is used   
```

```lang-none

  --lambda <Double>      use regularization Tikhonov regularization (specify the negative log, so     and input of 2.3 would give a regularization weight of 0.1)   
```

```lang-none

  --order <Integer>      use a given polynomial order (Default: 10)   
```

```lang-none

  --residual       save the residuals   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      output curves   
```

### Optional Output Arguments:

```lang-none

  --outputResiduals <Vects>      output residual error between the polynomial and the curve   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesFilterPredicate 

### Description:

  Filter curves with kernel regression.  This uses a non-parametric statistical 
  approach to smooth the curves 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Optional Parameter Arguments:

```lang-none

  --min <int>      The minimum number of vertices of the retained segments (Default: 2)   
```

```lang-none

  --nodot       Replace any dots in the attribute name with an underscore, e.g. pval.age     would become pval_age   
```

```lang-none

  --predicate <String>      the predicate for determining if a vertex should be kept (Default: pval <     0.05)   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      output curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesFilterTOM 

### Description:

  Filter curves based on their agreement with a tract orientation map 

### Required Input Arguments:

```lang-none

  --input <Curves>      the input curves   
```

### Optional Input Arguments:

```lang-none

  --reference <Volume>      input reference volume   
```

### Optional Parameter Arguments:

```lang-none

  --iters <int>      the number of iterations (Default: 1)   
```

```lang-none

  --beta <double>      a gain factor for computing likelihoods (Default: 1.0)   
```

```lang-none

  --thresh <double>      a threshold for outlier rejection (Default: 0.05)   
```

```lang-none

  --orient       orient the bundle prior to mapping   
```

```lang-none

  --vector       compute a vector orientation (you may want to enable the orient flag)   
```

```lang-none

  --norm <VolumeEnhanceContrastType>      specify a method for normalizing orientation magnitudes (Options:     Histogram, Ramp, RampGauss, Mean, None) (Default: Histogram)   
```

```lang-none

  --smooth       apply fiber smoothing after mapping   
```

```lang-none

  --sigma <Double>      apply smoothing with the given amount (bandwidth in mm) (default is largest     voxel dimension)   
```

```lang-none

  --support <int>      the smoothing filter radius in voxels (Default: 3)   
```

### Advanced Parameter Arguments:

```lang-none

  --sigma <Double>      apply smoothing with the given amount (bandwidth in mm) (default is largest     voxel dimension)   
```

```lang-none

  --support <int>      the smoothing filter radius in voxels (Default: 3)   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      the output inlier curves   
```

### Optional Output Arguments:

```lang-none

  --tom <Volume>      input reference volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesFilterTTF 

### Description:

  Filter curves to enhance their topographic regularity using the group graph 
  spectral distance 

### Required Input Arguments:

```lang-none

  --input <Curves>      the input curves   
```

### Optional Parameter Arguments:

```lang-none

  --K <Integer>      size of neighborhood (Default: 20)   
```

```lang-none

  --sigma <Double>      parameter for the exponential in the proximity measure (Default: 0.01)   
```

```lang-none

  --density <double>      the resolution for downsampling curves (Default: 5.0)   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      the output curves   
```

### Author:

  Junyan Wang, Ryan Cabeen 

### Citation:

  Wang, J., Aydogan, D. B., Varma, R., Toga, A. W., & Shi, Y. (2018). Modeling 
  topographic regularity in structural brain connectivity with application to 
  tractogram filtering. NeuroImage. 

<hr/>
## CurvesLabelMap 

### Description:

  Compute a label map, that is, a mask that represents the most likely label of 
  a given curves label for each voxel. 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Optional Input Arguments:

```lang-none

  --refvolume <Volume>      input reference volume (exclusive with refmask)   
```

```lang-none

  --refmask <Mask>      input reference mask (exclusive with refvolume)   
```

### Optional Parameter Arguments:

```lang-none

  --attr <String>      the name of attribute to extract from the curves (Default: label)   
```

```lang-none

  --largest       filter out largest components   
```

```lang-none

  --dilate <int>      dilate the mask a given number of times (Default: 0)   
```

```lang-none

  --threads <int>      the number of threads (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output label map   
```

### Optional Output Arguments:

```lang-none

  --outputProb <Volume>      output probability map   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesLandmarks 

### Description:

  Generate landmarks from curves using simplification and vertex clustering 

### Required Input Arguments:

```lang-none

  --input <Curves>      the input curves   
```

### Optional Parameter Arguments:

```lang-none

  --subsamp <Integer>      the number of curves to subsample (Default: 1000)   
```

```lang-none

  --eps <Double>      the spatial threshold for simplificaion (Default: 2.0)   
```

```lang-none

  --radius <Double>      the diameter of clusters for vertex grouping   
```

```lang-none

  --num <Integer>      the number of clusters to start with (Default: 2)   
```

### Required Output Arguments:

```lang-none

  --output <Vects>      the output landmarks   
```

### Author:

  Ryan Cabeen 

### Citation:

  (in preparation) 

<hr/>
## CurvesLengths 

### Description:

  Compute the lengths of the given curves 

### Required Input Arguments:

```lang-none

  --input <Curves>      the input curves   
```

### Required Output Arguments:

```lang-none

  --output <Vects>      the output landmarks   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesMask 

### Description:

  Compute a volumetric mask of curves.  Any voxel that intersects the curves 
  will be included 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Optional Input Arguments:

```lang-none

  --refvolume <Volume>      input reference volume (exclusive with refmask)   
```

```lang-none

  --refmask <Mask>      input reference mask (exclusive with refvolume)   
```

### Optional Parameter Arguments:

```lang-none

  --thresh <double>      the threshold (Default: 0.5)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesMaskMap 

### Description:

  Measure how many curves overlap a given mask parcellation 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

```lang-none

  --regions <Mask>      input regions   
```

```lang-none

  --lookup <Table>      use a lookup for region names   
```

### Optional Parameter Arguments:

```lang-none

  --nameField <String>      specify the lookup table name field (Default: name)   
```

```lang-none

  --indexField <String>      specify the lookup table index field (Default: index)   
```

```lang-none

  --mode <CurvesMaskMapMode>      specify the way that curves are counted (Options: Count, Binary, Fraction)     (Default: Binary)   
```

```lang-none

  --endpoints       select only curves with endpoints in the mask   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesMaskSelect 

### Description:

  Select curves using volumetric masks 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Optional Input Arguments:

```lang-none

  --deform <Deformation>      a deformation between curves and the masks   
```

```lang-none

  --include <Mask>      use an include mask (AND for multiple labels)   
```

```lang-none

  --exclude <Mask>      use an exclude mask (AND for multiple labels)   
```

```lang-none

  --contain <Mask>      use a containment mask   
```

### Optional Parameter Arguments:

```lang-none

  --binarize       binarize the include mask   
```

```lang-none

  --invert       invert the exclude mask   
```

```lang-none

  --skip       skip masks without regions or an insufficient number of regions without     error   
```

```lang-none

  --thresh <Double>      specify a containment threshold (Default: 0.8)   
```

```lang-none

  --endpoints       select based on only curve endpoints   
```

```lang-none

  --connect       select curves with endpoints that connect different labels   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      the output curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesMaskSelectBatch 

### Description:

  select curves from masks in batch mode 

### Required Input Arguments:

```lang-none

  --input <File>      specify an input curves   
```

### Optional Input Arguments:

```lang-none

  --deform <File>      specify a deformation between curves and volumes   
```

### Optional Parameter Arguments:

```lang-none

  --names <Spec>      specify bundle identifiers   
```

```lang-none

  --thresh <Value>      specify threshold for containment   
```

```lang-none

  --endpoints       specify that only endpoints should be tested   
```

```lang-none

  --invert       specify masks should be inverted   
```

```lang-none

  --binarize       specify masks should be binarized   
```

```lang-none

  --include <FilePattern>      specify an filename pattern for an include mask   
```

```lang-none

  --exclude <FilePattern>      specify an filename pattern for an exclude mask   
```

```lang-none

  --contain <FilePattern>      specify an filename pattern for an contain mask   
```

### Required Output Arguments:

```lang-none

  --output <FilePattern>      specify an output directory   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesMath 

### Description:

  Evaluate a mathematical expression at each vertex of curves. 

### Required Input Arguments:

```lang-none

  --input <Curves>      the input curves   
```

### Optional Parameter Arguments:

```lang-none

  --expression <String>      the expression to evaluate (Default: x > 0.5)   
```

```lang-none

  --result <String>      the attribute name for the result (Default: result)   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      output curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesMeasure 

### Description:

  Measure statistics of curves and store the results in a table 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Optional Parameter Arguments:

```lang-none

  --delta <double>      the volume resolution (Default: 1.0)   
```

```lang-none

  --thresh <double>      the density threshold (Default: 0.5)   
```

```lang-none

  --advanced       add advanced measures (outlier measures).  warning: these take polynomial     time with the number of curves   
```

```lang-none

  --endcor       add the endpoint correlation measure   
```

```lang-none

  --neighbors <int>      specify a number of neighbors for topographic measures (Default: 16)   
```

```lang-none

  --samples <int>      specify a number of samples for endpoint correlations estimation (Default:     5000)   
```

### Advanced Parameter Arguments:

```lang-none

  --advanced       add advanced measures (outlier measures).  warning: these take polynomial     time with the number of curves   
```

```lang-none

  --endcor       add the endpoint correlation measure   
```

```lang-none

  --neighbors <int>      specify a number of neighbors for topographic measures (Default: 16)   
```

```lang-none

  --samples <int>      specify a number of samples for endpoint correlations estimation (Default:     5000)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesMeasureAlongBatch 

### Description:

  Compute measures of a set of curves in batch mode. 

### Required Input Arguments:

```lang-none

  --input <FilePattern>      specify an input bundle filename pattern   
```

### Optional Input Arguments:

```lang-none

  --volume <String=Volume> [...]      specify volumes to sample   
```

```lang-none

  --deform <File>      specify a deformation between curves and volumes   
```

### Required Parameter Arguments:

```lang-none

  --names <Spec>      specify bundle identifiers (e.g. a file that lists the bundle names)   
```

### Optional Parameter Arguments:

```lang-none

  --attrs <String> [<String>] [...]      only include the specified attributes   
```

```lang-none

  --label <String>      compute statistics with parameterization attribute (discrete valued)     (Default: label)   
```

```lang-none

  --voxel       use voxel-based measurement (default is vertex-based)   
```

```lang-none

  --interp <String>      specify an interpolation method (Default: Trilinear)   
```

```lang-none

  --iters <Int>      smooth the sampled data with the given number of laplacian iterations     (Default: 3)   
```

```lang-none

  --lambda <Double>      smooth the sampled data with the given laplacian weighting (Default: 0.3)   
```

```lang-none

  --threads <Integer>      specify a number of threads (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Directory>      specify an output directory   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesMeasureBatch 

### Description:

  Compute measures of a set of curves in batch mode. 

### Required Input Arguments:

```lang-none

  --input <FilePattern>      specify an input bundle filename pattern   
```

### Optional Input Arguments:

```lang-none

  --volume <String=Volume> [...]      specify volumes to sample   
```

```lang-none

  --deform <File>      specify a deformation between curves and volumes   
```

```lang-none

  --mask <Mask>      specify a mask for including voxels   
```

### Required Parameter Arguments:

```lang-none

  --names <Spec>      specify bundle identifiers (e.g. a file that lists the bundle names)   
```

### Optional Parameter Arguments:

```lang-none

  --attrs <String> [<String>] [...]      only include the specified attributes   
```

```lang-none

  --thresh <Double>      specify a density threshold for volumetry (Default: 1.0)   
```

```lang-none

  --delta <Double>      specify a density threshold for volumetry (Default: 1.0)   
```

```lang-none

  --neighbors <Integer>      specify a number of neighbors for advanced metrics (Default: 1)   
```

```lang-none

  --samples <Integer>      specify a number of samples for the endpoint correlation (Default: 5000)   
```

```lang-none

  --voxel       use voxel-based measurement (default is vertex-based)   
```

```lang-none

  --advanced       include advanced metrics   
```

```lang-none

  --endcor       include the endpoint correlation metric   
```

```lang-none

  --interp <String>      specify an interpolation method (Default: Trilinear)   
```

```lang-none

  --threads <Integer>      specify a number of threads (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Directory>      specify an output directory   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesMidpointMask 

### Description:

  Extract curve endpoints and return them as vects 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Optional Input Arguments:

```lang-none

  --reference <Volume>      input reference volume   
```

### Optional Parameter Arguments:

```lang-none

  --label <int>      the mask label (Default: 1)   
```

```lang-none

  --num <int>      use the given number of vertices from the midpoint (Default: 1)   
```

```lang-none

  --dilate <int>      dilate the endpoint mask (Default: 0)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesNearest 

### Description:

  Extract the nearest curve to a collection 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

```lang-none

  --ref <Curves>      reference curves   
```

### Optional Input Arguments:

```lang-none

  --deform <Deformation>      a deformation for the input curve   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      output curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesOrient 

### Description:

  Orient curves to best match endpoints, i.e. flip them to make the starts and 
  ends as close as possible 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Optional Parameter Arguments:

```lang-none

  --iters <int>      a maxima number of iterations (Default: 10)   
```

```lang-none

  --axis       orient to the nearest spatial axis   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      output curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesOrientationMap 

### Description:

  Compute an orientation map.  This will find the most likely direction in each 
  voxel 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Optional Input Arguments:

```lang-none

  --refvolume <Volume>      input reference volume (exclusive with refmask)   
```

```lang-none

  --refmask <Mask>      input reference mask (exclusive with refvolume)   
```

### Optional Parameter Arguments:

```lang-none

  --orient       orient the bundle prior to mapping   
```

```lang-none

  --vector       compute a vector orientation (you may want to enable the orient flag)   
```

```lang-none

  --norm <VolumeEnhanceContrastType>      specify a method for normalizing orientation magnitudes (Options:     Histogram, Ramp, RampGauss, Mean, None) (Default: Histogram)   
```

```lang-none

  --smooth       apply fiber smoothing after mapping   
```

```lang-none

  --sigma <Double>      apply smoothing with the given amount (bandwidth in mm) (a negative value     will use the largest voxel dimension) (Default: -1.0)   
```

```lang-none

  --support <int>      the smoothing filter radius in voxels (Default: 3)   
```

```lang-none

  --fibers       return a fibers volume   
```

### Advanced Parameter Arguments:

```lang-none

  --sigma <Double>      apply smoothing with the given amount (bandwidth in mm) (a negative value     will use the largest voxel dimension) (Default: -1.0)   
```

```lang-none

  --support <int>      the smoothing filter radius in voxels (Default: 3)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesOrientationMapBatch 

### Description:

  Concatenate a batch of curves files 

### Required Input Arguments:

```lang-none

  --input <Curves>      specify any number of input curves files   
```

```lang-none

  --ref <Volume>      a reference volume   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      specify the output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesOutlierGaussian 

### Description:

  Filter outliers using a mvGaussian probabilistic model with a Chi2 
  distribution on the Mahalanobis distance 

### Required Input Arguments:

```lang-none

  --input <Curves>      the input curves   
```

```lang-none

  --reference <Curves>      input reference curve   
```

### Optional Parameter Arguments:

```lang-none

  --outlierCount <Integer>      the number of points used for outlier rejection (Default: 10)   
```

```lang-none

  --outlierThresh <Double>      the probability threshold for outlier rejection (Default: 0.95)   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      the output curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesOutlierLength 

### Description:

  Detect outliers among curves based on their length.  In practice an absolute 
  z-threshold of 2.0 should work well. 

### Required Input Arguments:

```lang-none

  --input <Curves>      the input curves   
```

### Optional Parameter Arguments:

```lang-none

  --zthresh <Double>      specify an absolute threshold z-score   
```

```lang-none

  --zlow <Double>      specify a low threshold z-score   
```

```lang-none

  --zhigh <Double>      specify a high threshold z-score   
```

### Optional Output Arguments:

```lang-none

  --inlier <Curves>      the output inlier curves   
```

```lang-none

  --outlier <Curves>      the output outlier curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesOutlierSCPT 

### Description:

  Detect outliers among curves with a sparse closest point transform. 

### Required Input Arguments:

```lang-none

  --input <Curves>      the input curves   
```

### Optional Parameter Arguments:

```lang-none

  --prior <Double>      the prior bundle size (mm) (Default: 0.001)   
```

```lang-none

  --mix <Double>      the prior mixing weight (use zero for no prior) (Default: 0.0)   
```

```lang-none

  --type <String>      covariance matrix type (full, diagonal, spherical) (Default: diagonal)   
```

```lang-none

  --thresh <Double>      the threshold probability (Default: 0.99)   
```

### Advanced Parameter Arguments:

```lang-none

  --prior <Double>      the prior bundle size (mm) (Default: 0.001)   
```

```lang-none

  --mix <Double>      the prior mixing weight (use zero for no prior) (Default: 0.0)   
```

### Optional Output Arguments:

```lang-none

  --inlier <Curves>      the output inlier curves   
```

```lang-none

  --outlier <Curves>      the output outlier curves   
```

```lang-none

  --landmarks <Vects>      the computed landmarks   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesPrintInfo 

### Description:

  Print basic information about curves 

### Required Input Arguments:

```lang-none

  --input <Curves>      the input curves   
```

### Optional Parameter Arguments:

```lang-none

  --stats       print statistics   
```

```lang-none

  --length       print statistics of curve lengths   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesPrototype 

### Description:

  Extract a prototypical curve that has maximum track density 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Optional Parameter Arguments:

```lang-none

  --delta <double>      the volume resolution (Default: 1.0)   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      output curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesReduce 

### Description:

  Reduce the number of curves by random subsampling 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Optional Parameter Arguments:

```lang-none

  --count <Integer>      a maxima number of curves   
```

```lang-none

  --fraction <Double>      remove a given fraction of the curves   
```

```lang-none

  --min <Integer>      retain at least this many curves   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      output selected curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesRelabel 

### Description:

  Relabel curves from biggest to smallest cluster 

### Required Input Arguments:

```lang-none

  --input <Curves>      the input curves   
```

### Required Parameter Arguments:

```lang-none

  --threshold <Double>      retain only clusters above a given proportion of the total   
```

### Optional Parameter Arguments:

```lang-none

  --largest       keep only the largest label   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      the output curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesResample 

### Description:

  Resample the position of vertices along curves so that they have uniform 
  spacing 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Optional Parameter Arguments:

```lang-none

  --num <Integer>      resample with a constant number of vertices per curve (0 specifices that     the max should be used)   
```

```lang-none

  --orient       reorient curves   
```

```lang-none

  --density <Double>      resample with a given vertex density (mm/vertex)   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      output curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesSample 

### Description:

  Sample a volume at the vertices of curves 

### Required Input Arguments:

```lang-none

  --input <Curves>      the input curves   
```

```lang-none

  --volume <Volume>      the volume   
```

### Optional Parameter Arguments:

```lang-none

  --interp <InterpolationType>      interpolation method (Options: Nearest, Trilinear, Tricubic, Gaussian,     GaussianLocalLinear, GaussianLocalQuadratic) (Default: Trilinear)   
```

```lang-none

  --attr <String>      attribute name (Default: sampled)   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      the output curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesSegmentAlong 

### Description:

  Segment a bundle by matching vertices along its length based on a prototype 
  curve 

### Required Input Arguments:

```lang-none

  --input <Curves>      the input curves   
```

### Optional Input Arguments:

```lang-none

  --proto <Curves>      input prototype curve (if not supplied, one will be computed)   
```

```lang-none

  --deform <Deformation>      a deformation to transform the proto curves to the coordinates of the input     curves   
```

### Optional Parameter Arguments:

```lang-none

  --method <CurvesSegmentAlongType>      the method for segmenting along the bundle (Options: Arclength, Distance,     Hybrid) (Default: Hybrid)   
```

```lang-none

  --samples <Integer>      the number of points used for along tract mapping (if not supplied, the     number of points in the proto will be used)   
```

```lang-none

  --density <Double>      the density of points used for along tract mapping (if not supplied, the     number of points in the proto will be used)   
```

```lang-none

  --label <String>      an attribute name for the label used to indicate the position along the     bundle (Default: label)   
```

```lang-none

  --outlier       remove outliers   
```

```lang-none

  --outlierCount <Integer>      the number of points used for outlier rejection (Default: 10)   
```

```lang-none

  --outlierThresh <Double>      the probability threshold for outlier rejection (Default: 0.99)   
```

```lang-none

  --outlierAttrs <String>      use the following attributes for outlier detection (if they exist)     (Default: FA,MD,frac,diff,S0,base)   
```

```lang-none

  --delta <Double>      the volume resolution for distance segmentation (Default: 1.0)   
```

```lang-none

  --power <Double>      the power for the mixing function (must be a positive even number, e.g. 2,     4, 6) (Default: 4.0)   
```

### Advanced Parameter Arguments:

```lang-none

  --delta <Double>      the volume resolution for distance segmentation (Default: 1.0)   
```

```lang-none

  --power <Double>      the power for the mixing function (must be a positive even number, e.g. 2,     4, 6) (Default: 4.0)   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      the output curves   
```

### Optional Output Arguments:

```lang-none

  --outputDist <Volume>      output distance volume (not provided by arclength segmentation)   
```

```lang-none

  --outputLabel <Volume>      output label volume (not provided by arclength segmentation   
```

```lang-none

  --outputProto <Curves>      output proto curve (useful in case it was computed automatically)   
```

```lang-none

  --outputCore <Curves>      output core curve (the nearest curve to the prototype)   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesSelect 

### Description:

  Select a which of curves using a number of possible criteria 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Optional Input Arguments:

```lang-none

  --deform <Deformation>      a deformation between curves and the masks   
```

```lang-none

  --mask <Mask>      a mask   
```

```lang-none

  --solids <Solids>      some solids   
```

```lang-none

  --vects <Vects>      some vects   
```

### Optional Parameter Arguments:

```lang-none

  --radius <double>      vects radius (Default: 5.0)   
```

```lang-none

  --or       use OR (instead of AND) to combine selections   
```

```lang-none

  --invert       invert the selection after combining   
```

```lang-none

  --exclude       exclude the selected curves   
```

```lang-none

  --endpoints       select based on only curve endpoints   
```

```lang-none

  --expression <String>      select based on a boolean-valued expression using any of: length, size,     min_attr, max_attr, mean_attr, or sum_attr   
```

```lang-none

  --minlen <Double>      a minimum length   
```

```lang-none

  --maxlen <Double>      a maximum length   
```

```lang-none

  --longest       select the longest curve   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      output selected curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesSetAttributeLookupTable 

### Description:

  Set vertex attributes of curves based on a table.  The curves should have a 
  discrete-valued attribute that is used to match vertices to entries in the 
  table. 

### Required Input Arguments:

```lang-none

  --curves <Curves>      input curves   
```

```lang-none

  --table <Table>      input table   
```

### Optional Input Arguments:

```lang-none

  --lookup <Table>      a lookup table to relate names to indices   
```

### Optional Parameter Arguments:

```lang-none

  --mergeTable <String>      a table field name to merge on (Default: name)   
```

```lang-none

  --mergeLookup <String>      a lookup field name to merge on (Default: along_name)   
```

```lang-none

  --index <String>      the lookup table index field name (Default: along_index)   
```

```lang-none

  --value <String>      a table field to get (Default: value)   
```

```lang-none

  --cindex <String>      the curves index field name (defaults to lookup table index field name)   
```

```lang-none

  --cvalue <String>      a curves field to set (defaults to table field name)   
```

```lang-none

  --background <double>      a background value (Default: 0.0)   
```

```lang-none

  --missing <Double>      an missing value   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      output curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesSetAttributeVects 

### Description:

  Set vertex attributes of curves based on a table.  The curves should have a 
  discrete-valued attribute that is used to match vertices to entries in the 
  table. 

### Required Input Arguments:

```lang-none

  --curves <Curves>      input curves   
```

```lang-none

  --vects <Vects>      input vectors   
```

### Optional Parameter Arguments:

```lang-none

  --name <String>      an attribute field name (Default: attr)   
```

```lang-none

  --quiet       don't complain if the vects and curves don't match (and try to add as much     data to the curves as possible)   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      output curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesSimplify 

### Description:

  Simplify curves with the Ramer-Douglas-Peucker algorithm 

### Required Input Arguments:

```lang-none

  --input <Curves>      the input curves   
```

### Optional Parameter Arguments:

```lang-none

  --epsilon <double>      the distance threshold (Default: 2.0)   
```

```lang-none

  --radial       use radial distance   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      the output simplified curves   
```

### Author:

  Ryan Cabeen 

### Citation:

  Heckbert, Paul S.; Garland, Michael (1997). Survey of polygonal 
  simplification algorithms 

<hr/>
## CurvesSmooth 

### Description:

  Smooth out irregularities in the input curves using laplacian (or Taubin) 
  smoothing.  This works best if curve vertices are equally spaced along each 
  curve. 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Optional Parameter Arguments:

```lang-none

  --attr <String>      the attribute to smooth (Default: coord)   
```

```lang-none

  --iters <int>      number of iterations (Default: 1)   
```

```lang-none

  --lambda <double>      lambda smoothing parameter (Default: 0.3)   
```

```lang-none

  --mu <Double>      mu smoothing parameter (for Taubin smoothing)   
```

### Advanced Parameter Arguments:

```lang-none

  --mu <Double>      mu smoothing parameter (for Taubin smoothing)   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      output curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesSplit 

### Description:

  Split a curves file into multiple parts 

### Required Input Arguments:

```lang-none

  --input <File>      specify an input curves file   
```

### Optional Input Arguments:

```lang-none

  --table <File>      specify a table for looking up names   
```

### Optional Parameter Arguments:

```lang-none

  --attr <String>      use a specific label attribute name (Default: label)   
```

```lang-none

  --names <String(s)>      specify a which of names to extract   
```

### Required Output Arguments:

```lang-none

  --output <Pattern>      specify the output filename pattern with %s   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesSubdivide 

### Description:

  Subdivide the curves by inserting vertices in each edge 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Optional Parameter Arguments:

```lang-none

  --num <Integer>      subdivide the curves a given number of times  (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      output curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesTable 

### Description:

  Concatenate curves files 

### Required Input Arguments:

```lang-none

  --input <File(s)>      specify an input curves   
```

### Optional Parameter Arguments:

```lang-none

  --names <String(s)>      use pattern-based input with the given names   
```

```lang-none

  --label       add a label attribute   
```

```lang-none

  --attr <String>      use a specific label attribute name (Default: label)   
```

### Required Output Arguments:

```lang-none

  --output <File>      specify the output   
```

### Optional Output Arguments:

```lang-none

  --output-table <File>      specify the output table name   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesThickness 

### Description:

  Compute the thickness of a bundle 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Optional Input Arguments:

```lang-none

  --core <Curves>      input core   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      output core with thickness   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesTransform 

### Description:

  Apply a spatial transformation to curves 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Optional Input Arguments:

```lang-none

  --affine <Affine>      apply an affine xfm   
```

```lang-none

  --invaffine <Affine>      apply an inverse affine xfm   
```

```lang-none

  --deform <Deformation>      apply a deformation xfm   
```

```lang-none

  --pose <Volume>      apply a transform to match the pose of a volume   
```

```lang-none

  --invpose <Volume>      apply a transform to match the inverse pose of a volume   
```

### Optional Parameter Arguments:

```lang-none

  --reverse       reverse the order, i.e. compose the affine(deform(x)), whereas the default     is deform(affine(x))   
```

```lang-none

  --tx <Double>      translate the curves in the x dimension   
```

```lang-none

  --ty <Double>      translate the curves in the y dimension   
```

```lang-none

  --tz <Double>      translate the curves in the z dimension   
```

```lang-none

  --sx <Double>      scale the curves in the x dimension   
```

```lang-none

  --sy <Double>      scale the curves in the y dimension   
```

```lang-none

  --sz <Double>      scale the curves in the z dimension   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      output curves   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesTrkPrintHeader 

### Description:

  Print basic information about TrackVis curves 

### Required Parameter Arguments:

```lang-none

  --input <String>      the input curves filename   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesTubes 

### Description:

  Create a mesh representing 3D tubes based on the input curves 

### Required Input Arguments:

```lang-none

  --input <Curves>      the input curves   
```

### Optional Input Arguments:

```lang-none

  --affine <Affine>      apply an affine xfm   
```

```lang-none

  --invaffine <Affine>      apply an inverse affine xfm   
```

```lang-none

  --deform <Deformation>      apply a deformation xfm   
```

### Optional Parameter Arguments:

```lang-none

  --color       use per-vertex orientation coloring   
```

```lang-none

  --wash <Double>      the color wash (Default: 0.2)   
```

```lang-none

  --dthick <Double>      the default tube thickness (Default: 0.15)   
```

```lang-none

  --fthick <Double>      the tube thickness factor (scales the thickness attribute, it it is     present) (Default: 1.0)   
```

```lang-none

  --thick <String>      use the given thickness attribute (Default: thickness)   
```

```lang-none

  --resolution <int>      the tube resolution (Default: 5)   
```

```lang-none

  --noColor       skip color attributes   
```

```lang-none

  --noThick       skip thickness attributes   
```

```lang-none

  --smooth       create smooth tube caps (the default will use a separate disk for tube     caps)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      the output tubes   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesVertices 

### Description:

  Extract all curve vertices 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Required Output Arguments:

```lang-none

  --output <Vects>      output vects   
```

### Author:

  Ryan Cabeen 

<hr/>
## CurvesVoxelize 

### Description:

  Compute a mask by voxelizing curves.  This works by find the voxels that 
  intersect the curves and marking them as one. 

### Required Input Arguments:

```lang-none

  --input <Curves>      input curves   
```

### Optional Input Arguments:

```lang-none

  --refvolume <Volume>      input reference volume (exclusive with refmask)   
```

```lang-none

  --refmask <Mask>      input reference mask (exclusive with refvolume)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
<hr/>
<hr/>
<hr/>
## GradientsCat 

### Description:

  Concatenate two sets of gradients into one 

### Required Input Arguments:

```lang-none

  --input <Gradients>      input gradients   
```

```lang-none

  --cat <Gradients>      gradients to concatenate   
```

### Optional Input Arguments:

```lang-none

  --catB <Gradients>      gradients to concatenate   
```

```lang-none

  --catC <Gradients>      gradients to concatenate   
```

```lang-none

  --catD <Gradients>      gradients to concatenate   
```

### Required Output Arguments:

```lang-none

  --output <Gradients>      output concatenated gradients   
```

### Author:

  Ryan Cabeen 

<hr/>
## GradientsConvert 

### Description:

  Convert gradients between file formats 

### Required Input Arguments:

```lang-none

  --input <Gradients>      the input gradients   
```

### Required Output Arguments:

```lang-none

  --output <Gradients>      the output gradients   
```

### Author:

  Ryan Cabeen 

<hr/>
## GradientsCreate 

### Description:

  Create a gradients file from separate bvecs and bvals 

### Required Input Arguments:

```lang-none

  --bvecs <Vects>      the input bvecs   
```

```lang-none

  --bvals <Vects>      the input bvals   
```

### Required Output Arguments:

```lang-none

  --output <Gradients>      the output gradients   
```

### Author:

  Ryan Cabeen 

<hr/>
## GradientsMatch 

### Description:

  Correct for errors in the orientation of diffusion gradients using the fiber 
  coherence index 

### Required Input Arguments:

```lang-none

  --input <Gradients>      the input gradients   
```

```lang-none

  --dwi <Volume>      the input diffusion-weighted MR volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      the input mask   
```

### Optional Parameter Arguments:

```lang-none

  --nospat       skip spatial scaling (note: this is not included in the paper by Schilling     et al)   
```

### Required Output Arguments:

```lang-none

  --output <Gradients>      the output matched gradients   
```

### Author:

  Ryan Cabeen 

### Citation:

  Schilling, Kurt G., et al. "A fiber coherence index for quality control of 
  B-table orientation in diffusion MRI scans." Magnetic resonance imaging 
  (2019). 

<hr/>
## GradientsReduce 

### Description:

  Reduce a set of gradients to a which based on user specification 

### Required Input Arguments:

```lang-none

  --input <Gradients>      the input gradients   
```

### Optional Parameter Arguments:

```lang-none

  --which <String>      include only specific gradients (comma separated zero-based indices)   
```

```lang-none

  --exclude <String>      exclude specific gradients (comma separated zero-based indices)   
```

```lang-none

  --shells <String>      include only specific shells   
```

### Required Output Arguments:

```lang-none

  --output <Gradients>      the output gradients   
```

### Author:

  Ryan Cabeen 

<hr/>
## GradientsTransform 

### Description:

  Transform gradient directions (note: the order of operations is transpose, 
  flip, swap, perm, affine) 

### Required Input Arguments:

```lang-none

  --input <Gradients>      input gradients   
```

### Optional Input Arguments:

```lang-none

  --affine <Affine>      apply an affine transform to gradient directions (and normalize afterwards)   
```

### Optional Parameter Arguments:

```lang-none

  --round       round the gradients magnitudes   
```

```lang-none

  --rounder <int>      specify how coarsely to round the gradient magnitudes (Default: 100)   
```

```lang-none

  --subset <String>      select a subset of gradients   
```

```lang-none

  --flip <String>      flip a coodinate (x, y, or z)   
```

```lang-none

  --swap <String>      swap a pair of coordinates (xy, xz, or yz)   
```

```lang-none

  --perm <String>      permute by coordinate index, e.g. 1,0,2   
```

### Required Output Arguments:

```lang-none

  --output <Gradients>      output transformed gradients   
```

### Author:

  Ryan Cabeen 

<hr/>
## ImageTile 

### Description:

  combine image tiles into a single big image 

### Required Input Arguments:

```lang-none

  --pattern <Pattern>      specify an input pattern (containing two %d patterns for the rows and     columns)   
```

```lang-none

  --maxidx <Number>      specify the maximum index (Default: 100)   
```

### Required Output Arguments:

```lang-none

  --output <File>      specify the output   
```

### Author:

  Ryan Cabeen 

<hr/>
## MapCat 

### Description:

  concatenate map files into a table 

### Required Parameter Arguments:

```lang-none

  --pattern <Pattern>      specify an input pattern (substitution like ${name})   
```

```lang-none

  --vars name=value(s)      specify a list of identifiers   
```

### Optional Parameter Arguments:

```lang-none

  --tuples       expand multiple vars to tuples (default: cartesian product)   
```

```lang-none

  --rows       expand each input file to a row   
```

```lang-none

  --input-name <String>      specify the input map name field (Default: name)   
```

```lang-none

  --input-value <String>      specify the input map value field (Default: value)   
```

```lang-none

  --output-name <String>      specify the output name field (Default: name)   
```

```lang-none

  --output-value <String>      specify the output value field (Default: value)   
```

```lang-none

  --na <String>      specify value for missing entries (Default: NA)   
```

```lang-none

  --skip       skip missing files   
```

```lang-none

  --include name(s)      specify which names to include   
```

```lang-none

  --exclude name(s)      specify which names to exclude   
```

### Required Output Arguments:

```lang-none

  --output <File>      specify the output   
```

### Author:

  Ryan Cabeen 

<hr/>
## MapCatBatch 

### Description:

  Concatenate a batch of curves files 

### Required Input Arguments:

```lang-none

  --input <Map>      specify any number of input map files   
```

### Required Output Arguments:

```lang-none

  --output <Map>      specify the output map file   
```

### Author:

  Ryan Cabeen 

<hr/>
## MapCatNarrow 

### Description:

  concatenate map files into columns of a table (more efficient than MapCat) 

### Required Parameter Arguments:

```lang-none

  --pattern <Pattern>      specify an input pattern (substitution like ${name})   
```

```lang-none

  --vars name=value(s)      specify a list of identifiers   
```

### Optional Parameter Arguments:

```lang-none

  --tuples       expand multiple vars to tuples (default: cartesian product)   
```

```lang-none

  --input-name <String>      specify the input map name field (Default: name)   
```

```lang-none

  --input-value <String>      specify the input map value field (Default: value)   
```

```lang-none

  --output-name <String>      specify the output map name field (Default: name)   
```

```lang-none

  --output-value <String>      specify the output map value field (Default: value)   
```

```lang-none

  --na <String>      specify value for missing entries (Default: NA)   
```

```lang-none

  --skip       skip missing files   
```

```lang-none

  --include name(s)      specify which names to include   
```

```lang-none

  --exclude name(s)      specify which names to exclude   
```

### Required Output Arguments:

```lang-none

  --output <File>      specify the output   
```

### Author:

  Ryan Cabeen 

<hr/>
## MapCatWide 

### Description:

  concatenate map files into rows of a table (more efficient than MapCat) 

### Required Parameter Arguments:

```lang-none

  --pattern <Pattern>      specify an input pattern (substitution like ${name})   
```

```lang-none

  --vars name=value(s)      specify a list of identifiers   
```

### Optional Parameter Arguments:

```lang-none

  --tuples       expand multiple vars to tuples (default: cartesian product)   
```

```lang-none

  --input-name <String>      specify the input map name field (Default: name)   
```

```lang-none

  --input-value <String>      specify the input map value field (Default: value)   
```

```lang-none

  --na <String>      specify value for missing entries (Default: NA)   
```

```lang-none

  --skip       skip missing files   
```

```lang-none

  --include name(s)      specify which names to include   
```

```lang-none

  --exclude name(s)      specify which names to exclude   
```

### Required Output Arguments:

```lang-none

  --output <File>      specify the output   
```

### Author:

  Ryan Cabeen 

<hr/>
## MapEddy 

### Description:

  Summarize the motion parameters from FSL EDDY 

### Required Input Arguments:

```lang-none

  --input <Vects>      the input eddy motion file, e.g. 'eddy_movement_rms'   
```

### Optional Parameter Arguments:

```lang-none

  --prefix <String>      a prefix to add before the metric name (Default: )   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output map   
```

### Author:

  Ryan Cabeen 

<hr/>
## MapFilter 

### Description:

  filter map files into a single map 

### Required Input Arguments:

```lang-none

  --pattern <Pattern>      specify an input pattern (containing a single %)   
```

```lang-none

  --vars value(s)      specify a list of identifiers (space separated)   
```

### Optional Parameter Arguments:

```lang-none

  --name <String>      specify the entry to select in each map (Default: name)   
```

```lang-none

  --skip       skip missing or erroneous files   
```

### Required Output Arguments:

```lang-none

  --output <File>      specify the output   
```

### Author:

  Ryan Cabeen 

<hr/>
## MapLaterality 

### Description:

  Compute the lateralization index of a given map.  This assumes you have a two 
  column table (name,value) that contains left and right measurements (see 
  module parameters to specify the left/right identifiers) 

### Required Input Arguments:

```lang-none

  --input <Table>      input table (should use the naming convention like the module parameters)   
```

### Optional Parameter Arguments:

```lang-none

  --name <String>      the field holding the name of the measurement (Default: name)   
```

```lang-none

  --value <String>      the field holding the value of the measurement (Default: value)   
```

```lang-none

  --left <String>      the identifier for left values (Default: lh_)   
```

```lang-none

  --right <String>      the identifier for right values (Default: rh_)   
```

```lang-none

  --indlat <String>      the identifier for the lateralization index (Default: indlat_)   
```

```lang-none

  --abslat <String>      the identifier for the absolute lateralization index (Default: abslat_)   
```

```lang-none

  --unilat <String>      the identifier for the unilateral averaging (Default: unilat_)   
```

```lang-none

  --minlat <String>      the identifier for the left right minimum (Default: minlat_)   
```

```lang-none

  --maxlat <String>      the identifier for the left right maximum (Default: maxlat_)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskAdapt 

### Description:

  Clean a mask by performing morphological opening followed by closing 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

```lang-none

  --volume <Volume>      input volume   
```

### Optional Parameter Arguments:

```lang-none

  --diameter <int>      the distance in voxels for adaptation (Default: 1)   
```

```lang-none

  --thresh <double>      the statistical threshold (Default: 1.0)   
```

```lang-none

  --robust       use robust statistics   
```

```lang-none

  --robustHuber       use a Huber estimator robust statistics   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

```lang-none

  --outputProb <Volume>      output prob   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskBinarize 

### Description:

  Binarize a mask to convert all labels to zero or one. 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskBoundary 

### Description:

  Close a mask using morphological operations 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --element <String>      specify an element (Default: cross)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskBox 

### Description:

  Compute the bounding box of a mask 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --foreground       compute box around foreground   
```

```lang-none

  --buffer <Double>      a buffer in mm   
```

### Required Output Arguments:

```lang-none

  --output <Solids>      output bounding box   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskBoxCreate 

### Description:

  Create a mask from a box 

### Required Input Arguments:

```lang-none

  --input <Solids>      the input box   
```

### Optional Parameter Arguments:

```lang-none

  --delta <double>      voxel spacing (Default: 1.0)   
```

```lang-none

  --round       round the starting point   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskCat 

### Description:

  Concatenate mask files 

### Required Input Arguments:

```lang-none

  --input <File(s)>      specify an input masks   
```

### Required Output Arguments:

```lang-none

  --output <File>      specify the output   
```

### Author:

  Ryan Cabeen 

<hr/>
Module: MaskCentroids
Field: nearest
Message: cannot be datatype 'Vect'. (hint: change it to String, Boolean, Integer, Double, or an Enum)


Module validation revealed 1 invalid fields.  See report results above.

<hr/>
## MaskClean 

### Description:

  Clean a mask by performing morphological opening followed by closing 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --num <int>      the number of iterations (Default: 1)   
```

```lang-none

  --largest       select the largest component as an intermediate step   
```

```lang-none

  --element <String>      specify an element: cross, cube, or sphere. you can also specify an     optional size, e.g. cross(3) (Default: cross)   
```

```lang-none

  --outside       treat voxels outside mask as background   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskClose 

### Description:

  Close a mask morphologically.  This dilates and then erodes the mask 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --num <int>      the number of times to erode and dilate the mask (Default: 1)   
```

```lang-none

  --largest       select the largest component as an intermediate step between erosion and     dilation   
```

```lang-none

  --mode       apply a mode filter as an intermediate step between erosion and dilation   
```

```lang-none

  --element <String>      specify an element: cross, cube, or sphere. you can also specify an     optional size, e.g. cross(3) (Default: cross)   
```

```lang-none

  --outside       treat voxels outside mask as background   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskComponents 

### Description:

  Compute connected components of a mask.  The output will be sorted by the 
  number of voxels per component, e.g. the largest component will have label 1, 
  and the second largest will have label 2, etc. 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --full       use a full 27-voxel neighborhood (default is 6-voxel)   
```

```lang-none

  --which <String>      include only specific component labels, e.g. "1,2" would select the two     largest components   
```

```lang-none

  --minvoxels <Integer>      filter out components with fewer then the given number of voxels   
```

```lang-none

  --minvolume <Double>      filter out components that are smaller in volume than the given threshold   
```

```lang-none

  --keep       keep the input labels (only relevant to filtering options)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskConvert 

### Description:

  Convert a mask between file formats 

### Required Input Arguments:

```lang-none

  --input <Mask>      input   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskCreate 

### Description:

  Create a mask based on user specified parameters 

### Optional Parameter Arguments:

```lang-none

  --deltax <double>      voxel spacing in x (Default: 1.0)   
```

```lang-none

  --deltay <double>      voxel spacing in y (Default: 1.0)   
```

```lang-none

  --deltaz <double>      voxel spacing in z (Default: 1.0)   
```

```lang-none

  --numx <int>      number of voxels in x (Default: 128)   
```

```lang-none

  --numy <int>      number of voxels in y (Default: 128)   
```

```lang-none

  --numz <int>      number of voxels in z (Default: 1)   
```

```lang-none

  --startx <double>      starting position in x (Default: 0.0)   
```

```lang-none

  --starty <double>      starting position in y (Default: 0.0)   
```

```lang-none

  --startz <double>      starting position in z (Default: 0.0)   
```

```lang-none

  --label <int>      constant label (Default: 0)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskCreateOverlay 

### Description:

  Create a mask from an overlay file 

### Required Input Arguments:

```lang-none

  --reference <Mask>      input reference mask   
```

### Required Parameter Arguments:

```lang-none

  --overlay <String>      the overlay filename   
```

### Optional Parameter Arguments:

```lang-none

  --label <int>      the label to use (Default: 1)   
```

```lang-none

  --preserve       preserve the labels from the reference mask   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskCrop 

### Description:

  Crop a mask down to a smaller mask (only one criteria per run) 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

```lang-none

  --solids <Solids>      some soids   
```

### Optional Parameter Arguments:

```lang-none

  --range <String>      a range specification, e.g. start:end,start:end,start:end   
```

```lang-none

  --invert       invert the selection   
```

```lang-none

  --pad <int>      a padding size in voxels (Default: 0)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      the output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskDeform 

### Description:

  Dilate a mask morphologically. 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --effect <double>      deformation effect size (Default: 1.0)   
```

```lang-none

  --extent <double>      deformation spatial extent (Default: 1.0)   
```

```lang-none

  --iters <int>      number of velocity field integrations (Default: 8)   
```

```lang-none

  --interp <InterpolationType>      the velocity field interpolation method (Options: Nearest, Trilinear,     Tricubic, Gaussian, GaussianLocalLinear, GaussianLocalQuadratic) (Default:     Nearest)   
```

### Optional Output Arguments:

```lang-none

  --velocity <Volume>      output forward deformation   
```

```lang-none

  --forward <Deformation>      output forward deformation   
```

```lang-none

  --backward <Deformation>      output backward deformation   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskDice 

### Description:

  Binarize a mask to convert all labels to zero or one. 

### Required Input Arguments:

```lang-none

  --left <Mask>      input left mask   
```

```lang-none

  --right <Mask>      input right mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskDiceBatch 

### Description:

  Compute the dice coefficient between a set of segmentations.  The input can 
  be specified two ways.  In the first way, you can specify a single 
  multi-label mask for each of the left and right flags (optionally specifying 
  the name of each label using a lookup table).  In the second way, you provide 
  a pattern (containing %s) to a set of mask files and then specify a list of 
  names for substitution into that pattern. 

### Required Input Arguments:

```lang-none

  --left <FilePattern>      specify the first set of segmentations   
```

```lang-none

  --right <FilePattern>      specify the second set of segmentations   
```

### Optional Parameter Arguments:

```lang-none

  --names <Spec>      specify bundle identifiers (e.g. a file that lists the bundle names)   
```

```lang-none

  --lookup <Spec>      specify region labels (should be a csv table with index and name columns)   
```

### Required Output Arguments:

```lang-none

  --output <File>      specify an output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskDilate 

### Description:

  Dilate a mask morphologically. 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --num <int>      the number of times to dilate the mask (Default: 1)   
```

```lang-none

  --element <String>      specify an element: cross, cube, or sphere. you can also specify an     optional size, e.g. cross(3) (Default: cross)   
```

```lang-none

  --outside       treat voxels outside mask as background   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskDistanceTransform 

### Description:

  Compute a distance transform using pff's fast algorithm 

### Required Input Arguments:

```lang-none

  --input <Mask>      the input mask   
```

### Optional Parameter Arguments:

```lang-none

  --signed       compute a signed transform   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output distance transform   
```

### Author:

  Ryan Cabeen 

### Citation:

  Felzenszwalb, P., & Huttenlocher, D. (2004). DistanceExtrinsic transforms of 
  sampled functions. Cornell University. 

<hr/>
## MaskEdge 

### Description:

  Detect the edges of a input mask 

### Required Input Arguments:

```lang-none

  --input <Mask>      input input   
```

### Optional Parameter Arguments:

```lang-none

  --binary       binarize the edges   
```

```lang-none

  --full       use a full neighborhood   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output input   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskErode 

### Description:

  Erode a mask morphologically 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --num <int>      the number of iterations (Default: 1)   
```

```lang-none

  --outside       treat voxels outside mask as background   
```

```lang-none

  --verbose       print messages   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskExtract 

### Description:

  Extract specific labels from a mask 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Input Arguments:

```lang-none

  --lookup <Table>      input lookup table (must store the index and name of each label)   
```

### Optional Parameter Arguments:

```lang-none

  --label <String>      the label(s) to extract (comma delimited, e.g. 1,2,3 or     lh-temporal,rh-temporal if a lookup is used) (Default: 1)   
```

```lang-none

  --mode <MaskExtractMode>      the mode for extracting labels (Options: Binary, Preserve, Distinct)     (Default: Binary)   
```

```lang-none

  --name <String>      the name field in the lookup table (Default: name)   
```

```lang-none

  --index <String>      the index field in the lookup table (Default: index)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskFill 

### Description:

  Flood fill the interior of the mask regions 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskFilter 

### Description:

  Filter a mask in a variety of possible ways 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a region to limit the filtering   
```

```lang-none

  --ref <Volume>      input reference volume   
```

### Optional Parameter Arguments:

```lang-none

  --mode       apply a mode filter   
```

```lang-none

  --largest       extract the largest region   
```

```lang-none

  --largestn <Integer>      extract the largest N region   
```

```lang-none

  --minvox <Integer>      the minima region voxel count   
```

```lang-none

  --minvol <Double>      the minima region volume   
```

```lang-none

  --highest       extract the region with the highest average average reference value   
```

```lang-none

  --lowest       extract the region with the lowest average average reference value   
```

```lang-none

  --most       extract the region with the most total reference signal   
```

```lang-none

  --binarize       binarize the mask at the end   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskFilterMedian 

### Description:

  Perform median filtering of a mask. 

### Required Input Arguments:

```lang-none

  --input <Mask>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --window <int>      the window size in voxels (Default: 1)   
```

```lang-none

  --slice <String>      restrict the filtering to a specific slice (i, j, or k)   
```

```lang-none

  --num <Integer>      the number of times to filter (Default: 1)   
```

```lang-none

  --threads <Integer>      the number of threads in the pool (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskFilterMode 

### Description:

  Perform mode filtering a mask.  Each voxel will be replaced by the most 
  frequent label in the surrounding neighborhood, so this is like performing 
  non-linear smoothing a mask 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask to restrict filtering   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskForce 

### Description:

  Compute a force field of a mask 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Input Arguments:

```lang-none

  --lookup <Table>      input lookup table (must store the index and name of each label)   
```

### Optional Parameter Arguments:

```lang-none

  --range <Double>      the distance for the force field (Default: 2.0)   
```

```lang-none

  --outside       only include forces outside   
```

```lang-none

  --flip       flip the orientation of the forces   
```

```lang-none

  --smooth       apply fiber smoothing after projection   
```

```lang-none

  --sigma <Double>      apply smoothing with the given amount (bandwidth in mm) (default is largest     voxel dimension)   
```

```lang-none

  --support <int>      the smoothing filter radius in voxels (Default: 3)   
```

```lang-none

  --which <String>      only process certain parcellation labels (by default all will be processed)   
```

```lang-none

  --name <String>      the name field in the lookup table (Default: name)   
```

```lang-none

  --index <String>      the index field in the lookup table (Default: index)   
```

```lang-none

  --threads <int>      the number of threads (Default: 1)   
```

### Advanced Parameter Arguments:

```lang-none

  --range <Double>      the distance for the force field (Default: 2.0)   
```

```lang-none

  --sigma <Double>      apply smoothing with the given amount (bandwidth in mm) (default is largest     voxel dimension)   
```

```lang-none

  --support <int>      the smoothing filter radius in voxels (Default: 3)   
```

```lang-none

  --name <String>      the name field in the lookup table (Default: name)   
```

```lang-none

  --index <String>      the index field in the lookup table (Default: index)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output vectors   
```

### Optional Output Arguments:

```lang-none

  --labels <Mask>      the output mask of labels   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskFuse 

### Description:

  fuse volumes 

### Required Input Arguments:

```lang-none

  --input <Mask(s)>      the input volumes   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      specify a mask   
```

### Optional Parameter Arguments:

```lang-none

  --pattern <String(s)>      specify a list of names that will be substituted with input %s   
```

### Optional Output Arguments:

```lang-none

  --output-label <Mask>      specify the output maxima likelihood label   
```

```lang-none

  --output-prob <Volume>      specify the output label probability   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskGreater 

### Description:

  Extract the larger components of a mesh 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --minimum <double>      the minima volume (Default: 25.0)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskHull 

### Description:

  Compute the convex hull of a mask 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

### Citation:

  Barber, C. B., Dobkin, D. P., & Huhdanpaa, H. (1996). The quickhull algorithm 
  for convex hulls. ACM Transactions on Mathematical Software (TOMS), 22(4), 
  469-483. 

<hr/>
## MaskIntersection 

### Description:

  Compute the logical AND of two masks 

### Required Input Arguments:

```lang-none

  --left <Mask>      input left mask   
```

```lang-none

  --right <Mask>      input right mask   
```

### Optional Parameter Arguments:

```lang-none

  --mode <MaskIntersectionMode>      specify what label should be returned (Options: Left, Right, Max, Min, Sum,     Product, One) (Default: Left)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskInvert 

### Description:

  Invert the labels of a mask 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      only invert in this region   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskLargest 

### Description:

  Extract the largest component of a mask 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskLateralize 

### Description:

  Compute the logical AND of two masks 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

```lang-none

  --left <Mask>      a mask marking off the left hemisphere (the remainder is assumed to be the     right)   
```

### Optional Parameter Arguments:

```lang-none

  --leftPrefix <String>      the left prefix (Default: Left_)   
```

```lang-none

  --leftPostfix <String>      the left postfix (Default: )   
```

```lang-none

  --rightPrefix <String>      the right prefix (Default: Right_)   
```

```lang-none

  --rightPostfix <String>      the right postfix (Default: )   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskLesser 

### Description:

  Extract the smaller components of a mesh 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --maximum <double>      the maxima volume (Default: 25.0)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskList 

### Description:

  Create a list of regions from a mask 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --zero       include zero   
```

```lang-none

  --base <String>      specify a basename (Default: region)   
```

```lang-none

  --name <String>      specify the lookup name field (Default: name)   
```

```lang-none

  --index <String>      specify the output index name (Default: index)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskMRFEM 

### Description:

  Refine a segmentation using a Expectation Maximization Markov Random Field 
  framework 

### Required Input Arguments:

```lang-none

  --input <Mask>      input region   
```

```lang-none

  --volume <Volume>      input volume   
```

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --distance <Double>      the distance in voxels for adaptation (Default: 1.0)   
```

```lang-none

  --mrfCross       use a 6-neighborhood (instead of a full 27-neighborhood with diagonals)   
```

```lang-none

  --mrfGamma <Double>      use the following spatial regularization weight (Default: 1.0)   
```

```lang-none

  --mrfCrfGain <Double>      use the gain used for the conditional random field (zero will disable it)     (Default: 1.0)   
```

```lang-none

  --mrfIcmIters <Integer>      use the following number of MRF optimization iterations (Default: 5)   
```

```lang-none

  --mrfEmIters <Integer>      use the following number of expectation maximization iteration (Default: 5)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output input   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskMarchingCubes 

### Description:

  Extract surfaces from a mask.  If there are multiple labels in the volume, 
  distinct meshes will be produced for each label. 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Input Arguments:

```lang-none

  --table <Table>      a table listing a subset of indices   
```

### Optional Parameter Arguments:

```lang-none

  --std <Double>      perform Gaussian smoothing before surface extraction   
```

```lang-none

  --support <Integer>      use a given support in voxels for smoothing (Default: 3)   
```

```lang-none

  --level <Double>      use a given isolevel for smoothed surface extraction (Default: 0.5)   
```

```lang-none

  --which <String>      a string specifying a subset of labels, e.g. 1,2,4:6   
```

```lang-none

  --tindex <String>      the index field name to use in the table (Default: index)   
```

```lang-none

  --mindex <String>      the mesh attribute for the index value (default is "label") (Default:     label)   
```

```lang-none

  --meanarea <Double>      simplify the mesh to have the given mean triangle area   
```

### Advanced Parameter Arguments:

```lang-none

  --tindex <String>      the index field name to use in the table (Default: index)   
```

```lang-none

  --mindex <String>      the mesh attribute for the index value (default is "label") (Default:     label)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      output mesh   
```

### Author:

  Ryan Cabeen 

### Citation:

  Lorensen, W. E., & Cline, H. E. (1987, August). Marching cubes: A high 
  resolution 3D surface construction algorithm. In ACM siggraph computer 
  graphics (Vol. 21, No. 4, pp. 163-169). ACM. 

<hr/>
## MaskMarchingSquares 

### Description:

  Extract contours from slices of a mask 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --dim <String>      volume channel to contour (x, y, or z) (Default: z)   
```

```lang-none

  --start <int>      starting slice index (Default: 0)   
```

```lang-none

  --step <int>      number of slices between contours (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      output curves   
```

### Author:

  Ryan Cabeen 

### Citation:

  Maple, C. (2003, July). Geometric design and space planning using the 
  marching squares and marching cube algorithms. In Geometric Modeling and 
  Graphics, 2003. Proceedings. 2003 International Conference on (pp. 90-95). 
  IEEE. 

<hr/>
## MaskMeasure 

### Description:

  Measure properties of a mask 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Input Arguments:

```lang-none

  --region <Mask>      restrict the analysis to a given region of interest   
```

```lang-none

  --lookup <Table>      use a lookup for region names   
```

### Optional Parameter Arguments:

```lang-none

  --nameField <String>      specify the output name field (Default: name)   
```

```lang-none

  --valueField <String>      specify the output value field name (Default: value)   
```

```lang-none

  --lutNameField <String>      specify the lookup name field (Default: name)   
```

```lang-none

  --lutIndexField <String>      specify the lut index field name (Default: index)   
```

```lang-none

  --volumes       report only the region volumes   
```

```lang-none

  --fraction       compute the fraction of each region   
```

```lang-none

  --cluster       compute cluster statistics of each region   
```

```lang-none

  --position       compute the position of each region   
```

```lang-none

  --components       run connected components labeling before anything else   
```

```lang-none

  --binarize       binarize the mask before anything else (overrides the components flag)   
```

```lang-none

  --comps       include component counts in the report   
```

```lang-none

  --counts       include voxel counts in the report   
```

```lang-none

  --minvolume <Double>      the minimum component volume (mm^3) to be included   
```

```lang-none

  --minvoxels <Integer>      the minimum component voxel count to be included   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskMeasureBatch 

### Description:

  Compute measures of a set of curves in batch mode. 

### Required Input Arguments:

```lang-none

  --input <FilePattern>      specify an input mask filename pattern   
```

### Required Parameter Arguments:

```lang-none

  --names <Spec>      specify the case identifiers (e.g. a file that lists the subject ids)   
```

### Optional Parameter Arguments:

```lang-none

  --name <String>      specify an interpolation method (Default: name)   
```

```lang-none

  --value <String>      specify an interpolation method (Default: value)   
```

```lang-none

  --threads <Integer>      specify a number of threads (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      specify an output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskMeasureClusters 

### Description:

  Measure individual cluster sizes of a mask 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Input Arguments:

```lang-none

  --region <Mask>      restrict the analysis to a given region of interest   
```

```lang-none

  --lookup <Table>      use a lookup for region names   
```

### Optional Parameter Arguments:

```lang-none

  --nameField <String>      specify the output name field (Default: name)   
```

```lang-none

  --clusterField <String>      specify the output cluster field (Default: cluster)   
```

```lang-none

  --voxelsField <String>      specify the output voxel count field name (Default: voxels)   
```

```lang-none

  --lutNameField <String>      specify the lookup name field (Default: name)   
```

```lang-none

  --lutIndexField <String>      specify the lut index field name (Default: index)   
```

```lang-none

  --minvolume <Double>      the minimum component volume (mm^3) to be included   
```

```lang-none

  --minvoxels <Integer>      the minimum component voxel count to be included   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskNodeLink 

### Description:

  Create a node link representation of mask connectivity in a mesh 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

```lang-none

  --lookup <Table>      input table   
```

```lang-none

  --attributes <Table>      input attributes   
```

### Optional Parameter Arguments:

```lang-none

  --index <String>      index (Default: index)   
```

```lang-none

  --name <String>      name (Default: name)   
```

```lang-none

  --group <String>      group (Default: group)   
```

```lang-none

  --value <String>      value (Default: value)   
```

```lang-none

  --radius <double>      radius (Default: 10.0)   
```

```lang-none

  --subdiv <int>      subdiv (Default: 2)   
```

### Optional Output Arguments:

```lang-none

  --links <Curves>      output links   
```

```lang-none

  --nodes <Mesh>      output nodes   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskOpen 

### Description:

  Open a mask using morphological operations.  This first erodes the mask and 
  then dilates it. 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --num <int>      the number of times to erode and dilate the mask (Default: 1)   
```

```lang-none

  --largest       select the largest component as an intermediate step between erosion and     dilation   
```

```lang-none

  --mode       apply a mode filter as an intermediate step between erosion and dilation   
```

```lang-none

  --element <String>      specify an element: cross, cube, or sphere. you can also specify an     optional size, e.g. cross(3) (Default: cross)   
```

```lang-none

  --outside       treat voxels outside mask as background   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskOrigin 

### Description:

  Set the origin of a mask 

### Required Input Arguments:

```lang-none

  --input <Mask>      the input Mask   
```

### Optional Parameter Arguments:

```lang-none

  --x <double>      the origin in x (Default: 0.0)   
```

```lang-none

  --y <double>      the origin in y (Default: 0.0)   
```

```lang-none

  --z <double>      the origin in z (Default: 0.0)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      the output Mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskPad 

### Description:

  Pad a mask by a given number of voxels 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --pad <int>      the amount of padding in voxels (Default: 10)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskPhantomNoisy 

### Description:

  Generate a noisy mask 

### Optional Parameter Arguments:

```lang-none

  --width <int>      image width (Default: 100)   
```

```lang-none

  --height <int>      image height (Default: 100)   
```

```lang-none

  --slices <int>      image slices (Default: 1)   
```

```lang-none

  --labels <int>      the number of labels to use (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskPrintInfo 

### Description:

  Print basic information about a mask 

### Required Input Arguments:

```lang-none

  --input <Mask>      the input mask   
```

### Optional Parameter Arguments:

```lang-none

  --stats       print statistics   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskProbFuse 

### Description:

  Fuse probability maps into a multi-label mask.  Each input volume should be a 
  probability map for an individual region.  The labels will match the order of 
  the inputs 

### Required Input Arguments:

```lang-none

  --input <Volume(s)>      the input probability maps   
```

```lang-none

  --thresh <double>      specify the minimum probability threshold (Default: 0.25)   
```

### Optional Output Arguments:

```lang-none

  --output-label <Mask>      specify the output maxima likelihood label   
```

```lang-none

  --output-prob <Volume>      specify the output label probability   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskPrototype 

### Description:

  Create a mask from example data 

### Optional Input Arguments:

```lang-none

  --mask <Mask>      an example mask   
```

```lang-none

  --volume <Volume>      an example volume   
```

### Optional Parameter Arguments:

```lang-none

  --label <int>      the default label (Default: 0)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskRegionMerge 

### Description:

  Merge small regions using an adjacency graph 

### Required Input Arguments:

```lang-none

  --input <Mask>      input region input   
```

### Optional Parameter Arguments:

```lang-none

  --threshold <double>      the input threshold (Default: 10.0)   
```

```lang-none

  --full       use a full 27-voxel neighborhood (default is 6-voxel)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output input   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskRegionsExtract 

### Description:

  extract mesh models from a mask for each region 

### Required Input Arguments:

```lang-none

  --regions <Mask>      specify input region of interest(s)   
```

```lang-none

  --lookup <Table>      a table listing the names of regions   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      specify a mask for including voxels   
```

### Optional Parameter Arguments:

```lang-none

  --name <String>      specify a lookup table field for region names (Default: name)   
```

```lang-none

  --index <String>      specify a lookup table field for region index (label) (Default: index)   
```

```lang-none

  --include name(s)      specify which names to include   
```

```lang-none

  --exclude name(s)      specify which names to exclude   
```

```lang-none

  --support <Integer>      specify a filter support size (Default: 3)   
```

```lang-none

  --std <Double>      specify a smoothing bandwidth   
```

```lang-none

  --level <Double>      specify a isolevel (Default: 0.5)   
```

### Optional Output Arguments:

```lang-none

  --output-mesh <Pattern>      specify an output pattern (contains %s) for mesh output   
```

```lang-none

  --output-mask <Pattern>      specify an output pattern (contains %s) for mask output   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskRegionsMeasure 

### Description:

  measure statistics of volume data in a set of regions of interest 

### Required Input Arguments:

```lang-none

  --regions <Mask>      specify input region of interest(s)   
```

```lang-none

  --lookup <Table>      a table listing the names of regions   
```

### Optional Input Arguments:

```lang-none

  --weight <Volume>      specify a volumetric weighting for computing statistics   
```

```lang-none

  --mask <Mask>      specify a mask for including voxels   
```

### Optional Parameter Arguments:

```lang-none

  --basic       report only basic statistics   
```

```lang-none

  --volume <String=Volume> [...]      specify volumes to measure   
```

```lang-none

  --name <String>      specify a lookup table field for region names (Default: name)   
```

```lang-none

  --index <String>      specify a lookup table field for region index (label) (Default: index)   
```

```lang-none

  --na <String>      specify a name for missing values (Default: NA)   
```

### Required Output Arguments:

```lang-none

  --output <Directory>      specify an output directory   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskRelabel 

### Description:

  Relabel a mask by replacing voxel labels with new values 

### Required Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

```lang-none

  --mapping <Table>      input mapping (includes fields named 'from' and 'to')   
```

### Optional Input Arguments:

```lang-none

  --lookup <Table>      input lookup   
```

### Optional Parameter Arguments:

```lang-none

  --preserve       preserve the input labels if possible   
```

```lang-none

  --names       use names for remapping (instead of index labels)   
```

```lang-none

  --from <String>      specify a from field to us in mapping (Default: from)   
```

```lang-none

  --to <String>      specify a to field to us in mapping (Default: to)   
```

```lang-none

  --name <String>      the name field (Default: name)   
```

```lang-none

  --index <String>      the index field (Default: index)   
```

### Required Output Arguments:

```lang-none

  --outputMask <Mask>      output mask   
```

### Optional Output Arguments:

```lang-none

  --outputLookup <Table>      output lookup   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskReorder 

### Description:

  Change the ordering of voxels in a mask.  You can flip and shift the voxels.  
  Which shifting outside the field a view, the data is wrapped around.  
  Shifting is applied before flipping 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --flipi       flip in i   
```

```lang-none

  --flipj       flip in j   
```

```lang-none

  --flipk       flip in k   
```

```lang-none

  --shifti <int>      shift in i (Default: 0)   
```

```lang-none

  --shiftj <int>      shift in j (Default: 0)   
```

```lang-none

  --shiftk <int>      shift in k (Default: 0)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskResample 

### Description:

  Resample a mask with a different voxel size 

### Required Input Arguments:

```lang-none

  --input <Mask>      the input volume   
```

### Optional Parameter Arguments:

```lang-none

  --dx <double>      the voxel size in x (Default: 1.0)   
```

```lang-none

  --dy <double>      the voxel size in y (Default: 1.0)   
```

```lang-none

  --dz <double>      the voxel size in z (Default: 1.0)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      the output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskRestoreMRF 

### Description:

  Restore a mask using a markov random field with loopy belief propagation 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask of the area to restore   
```

### Optional Parameter Arguments:

```lang-none

  --cost <double>      the cost for changing a voxel label (Default: 0.75)   
```

```lang-none

  --data <double>      the weight for the data term (Default: 1.0)   
```

```lang-none

  --smooth <double>      the weight for the smoothness term (Default: 1.0)   
```

```lang-none

  --iters <int>      the number of belief propagation iterations (Default: 50)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

### Citation:

  Felzenszwalb, P. F., & Huttenlocher, D. P. (2006). Efficient belief 
  propagation for early vision. International journal of computer vision, 
  70(1), 41-54. 

<hr/>
## MaskRings 

### Description:

  Compute equidistant rings around a region 

### Required Input Arguments:

```lang-none

  --input <Mask>      input region   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --levels <String>      the levels for segmentation (comma-separated in mm) (Default: 3,5,7,9)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskSampleVects 

### Description:

  Sample vects from a mask 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --count <int>      the number of samples per voxel (Default: 1)   
```

```lang-none

  --limit <int>      the maximum number of points to produce (Default: 10000)   
```

### Required Output Arguments:

```lang-none

  --output <Vects>      output vects   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskSet 

### Description:

  Set the values of a mask 

### Required Input Arguments:

```lang-none

  --input <Mask>      the input mask   
```

### Optional Input Arguments:

```lang-none

  --solids <Solids>      some solids   
```

```lang-none

  --vects <Vects>      some vects   
```

```lang-none

  --mask <Mask>      the mask   
```

### Optional Parameter Arguments:

```lang-none

  --clear       clear the input labels before setting anything   
```

```lang-none

  --range <String>      a range, e.g. start:end,start:end,start:end   
```

```lang-none

  --label <int>      the label to set (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      the output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskSetEyes 

### Description:

  Binarize a mask to convert all labels to zero or one. 

### Required Input Arguments:

```lang-none

  --input <Mask>      input reference mask   
```

```lang-none

  --eyes <Vects>      input eye positions   
```

### Optional Parameter Arguments:

```lang-none

  --start <double>      the starting level (Default: 10.0)   
```

```lang-none

  --end <double>      the end level (Default: 55.0)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskSetTable 

### Description:

  Create a table listing regions of a mask 

### Required Input Arguments:

```lang-none

  --reference <Mask>      input reference   
```

```lang-none

  --table <Table>      input table   
```

### Optional Input Arguments:

```lang-none

  --lookup <Table>      a lookup table to relate names to indices   
```

### Optional Parameter Arguments:

```lang-none

  --merge <String>      a field name to merge on (Default: name)   
```

```lang-none

  --index <String>      the index field name (Default: index)   
```

```lang-none

  --value <String>      a field to set (Default: value)   
```

```lang-none

  --background <double>      a background value (Default: 0.0)   
```

```lang-none

  --missing <Double>      an missing value   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskShell 

### Description:

  Compute the shell of a mask (the voxels at the boundary of the mesh) 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --num <int>      dilate this many times (Default: 1)   
```

```lang-none

  --mode <ShellMode>      specify a mode for computing the shell (Options: Inner, Outer, Multi)     (Default: Inner)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskSkeleton 

### Description:

  Skeletonize a mask using medial axis thinning.  Based on Hanno Homan's 
  implementation of Lee et al. at http://hdl.handle.net/1926/1292 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

### Citation:

  Lee, Ta-Chih, Rangasami L. Kashyap, and Chong-Nam Chu. Building skeleton 
  models via 3-D medial surface axis thinning algorithms. CVGIP: Graphical 
  Models and Image Processing 56.6 (1994): 462-478. 

<hr/>
## MaskSort 

### Description:

  Sort the labels of a mask by their size 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskSplitMidline 

### Description:

  Split mask regions at the midline to produce left and right regions 

### Required Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Input Arguments:

```lang-none

  --lookup <Table>      input lookup   
```

### Optional Parameter Arguments:

```lang-none

  --pattern <String>      a pattern for renaming entries (Default: %{hemi}_%{name})   
```

```lang-none

  --midline <Integer>      specify a midline index (otherwise the middle is used)   
```

```lang-none

  --start <Integer>      specify an offset value (0 or 1) (Default: 1)   
```

```lang-none

  --left <String>      specify an identifier for the left hemisphere (Default: left)   
```

```lang-none

  --right <String>      specify an identifier for the right hemisphere (Default: right)   
```

```lang-none

  --name <String>      the name field in the lookup table (Default: name)   
```

```lang-none

  --index <String>      the index field in the lookup table (Default: index)   
```

### Required Output Arguments:

```lang-none

  --outputMask <Mask>      output mask   
```

### Optional Output Arguments:

```lang-none

  --outputLookup <Table>      output lookup   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskStandardize 

### Description:

  Standardize the pose of a mask 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

```lang-none

  --xfm <Affine>      output affine   
```

```lang-none

  --invxfm <Affine>      output inverse affine   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskTable 

### Description:

  Create a table listing regions of a mask 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --nameField <String>      the region name field (Default: name)   
```

```lang-none

  --indexField <String>      the region label field (Default: index)   
```

```lang-none

  --namePattern <String>      a pattern for naming regions (Default: region%d)   
```

```lang-none

  --xField <String>      the region centroid x field (Default: x)   
```

```lang-none

  --yField <String>      the region centroid y field (Default: y)   
```

```lang-none

  --zField <String>      the region centroid z field (Default: z)   
```

```lang-none

  --name       include a name field   
```

```lang-none

  --centroid       include a centroid fields   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskThick 

### Description:

  Filter the mask to include only subregions with at least the given thickness 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --num <int>      the minimum thickness in voxels (Default: 1)   
```

```lang-none

  --element <String>      specify an element: cross, cube, or sphere. you can also specify an     optional size, e.g. cross(3) (Default: cross)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskTransform 

### Description:

  Transform a mask 

### Required Input Arguments:

```lang-none

  --input <Mask>      input volume   
```

```lang-none

  --reference <Volume>      input reference volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask (defined in the reference space)   
```

```lang-none

  --inputMask <Mask>      use an input mask (defined in the input space)   
```

```lang-none

  --affine <Affine>      apply an affine xfm   
```

```lang-none

  --invaffine <Affine>      apply an inverse affine xfm   
```

```lang-none

  --deform <Deformation>      apply a deformation xfm   
```

### Optional Parameter Arguments:

```lang-none

  --reverse       reverse the order, i.e. compose the affine(deform(x)), whereas the default     is deform(affine(x))   
```

```lang-none

  --background <Integer>      a label for filling background voxels   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskTransformBatch 

### Description:

  Apply a spatial transformation to many masks 

### Required Input Arguments:

```lang-none

  --input <FilePattern>      specify a pattern for mask inputs (contains %s)   
```

### Required Parameter Arguments:

```lang-none

  --names <Spec>      specify identifiers (used for substitution of %s in the input and output)   
```

### Optional Parameter Arguments:

```lang-none

  --reference <Filename>      specify a reference volume   
```

```lang-none

  --affine <Filename>      specify an affine transform   
```

```lang-none

  --deform <Filename>      specify a deformation   
```

```lang-none

  --invaffine <Filename>      specify an inverse affine transform   
```

```lang-none

  --reverse       apply the reverse transform   
```

```lang-none

  --threads <Integer>      use the given number of threads (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <FilePattern>      specify a pattern for mask outputs (contains %s)   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskUnion 

### Description:

  Combine two masks into one.  By default, the left mask labels will be 
  preserved and background voxels will be OR-ed with the right mask.  You can 
  optionally relabel the result to assign new labels to distinct pairs of 
  left/right labels 

### Required Input Arguments:

```lang-none

  --left <Mask>      input left mask   
```

```lang-none

  --right <Mask>      input right mask   
```

### Optional Parameter Arguments:

```lang-none

  --relabel       combine the masks using distinct values   
```

```lang-none

  --max       use the maximum label when masks overlap (default is to use the left)   
```

```lang-none

  --distinct       use a label of one for the left mask and a label of two for the right   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskVects 

### Description:

  Extract a vector for each foreground voxel 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Required Output Arguments:

```lang-none

  --output <Vects>      output vects   
```

### Author:

  Ryan Cabeen 

<hr/>
## MaskZoom 

### Description:

  Zoom a mask 

### Required Input Arguments:

```lang-none

  --input <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --factor <Double>      an isotropic scaling factor (Default: 2.0)   
```

```lang-none

  --isotropic <Double>      an isotropic scaling factor   
```

```lang-none

  --fi <Double>      a scaling factor in i   
```

```lang-none

  --fj <Double>      a scaling factor in j   
```

```lang-none

  --fk <Double>      a scaling factor in k   
```

```lang-none

  --mode       filter the mask to smooth it   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## MatrixConvert 

### Description:

  Convert a matrix between file formats 

### Required Input Arguments:

```lang-none

  --input <Matrix>      input matrix   
```

### Required Output Arguments:

```lang-none

  --output <Matrix>      output matrix   
```

### Author:

  Ryan Cabeen 

<hr/>
## MatrixCreate 

### Description:

  Create a matrix 

### Required Parameter Arguments:

```lang-none

  --identity <Integer>      create an identity matrix with the given channel   
```

### Required Output Arguments:

```lang-none

  --output <Matrix>      output matrix   
```

### Author:

  Ryan Cabeen 

<hr/>
## MatrixInvert 

### Description:

  Invert a matrix 

### Required Input Arguments:

```lang-none

  --input <Matrix>      input matrix   
```

### Required Output Arguments:

```lang-none

  --output <Matrix>      output matrix   
```

### Author:

  Ryan Cabeen 

<hr/>
<hr/>
## MatrixOrthogonalize 

### Description:

  Orthogonalize a matrix 

### Required Input Arguments:

```lang-none

  --input <Matrix>      input matrix   
```

### Required Output Arguments:

```lang-none

  --output <Matrix>      output matrix   
```

### Author:

  Ryan Cabeen 

<hr/>
## MatrixPrintInfo 

### Description:

  Print basic information about a matrix 

### Required Input Arguments:

```lang-none

  --input <Matrix>      the input matrix   
```

### Author:

  Ryan Cabeen 

<hr/>
## MatrixTranspose 

### Description:

  Transpose a matrix 

### Required Input Arguments:

```lang-none

  --input <Matrix>      input matrix   
```

### Required Output Arguments:

```lang-none

  --output <Matrix>      output matrix   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshAttributes 

### Description:

  Manipulate mesh vertex attributes.  Operations support comma-delimited lists 

### Required Input Arguments:

```lang-none

  --input <Mesh>      the input Mesh   
```

### Optional Parameter Arguments:

```lang-none

  --copy <String>      copy attribute (x=y syntax)   
```

```lang-none

  --rename <String>      rename attribute (x=y syntax)   
```

```lang-none

  --remove <String>      remove attribute (- for all)   
```

```lang-none

  --retain <String>      retain the given attributes and remove others)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      the output Mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshBox 

### Description:

  Get the bounding box of a mesh 

### Required Input Arguments:

```lang-none

  --input <Mesh>      input mesh   
```

### Required Output Arguments:

```lang-none

  --output <Solids>      output box   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshCat 

### Description:

  Concatentate two meshes 

### Required Input Arguments:

```lang-none

  --left <Mesh>      input left   
```

```lang-none

  --right <Mesh>      input right   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshCatBatch 

### Description:

  Concatenate mesh files 

### Required Input Arguments:

```lang-none

  --input <File(s)>      specify an input meshes   
```

### Optional Parameter Arguments:

```lang-none

  --names <String(s)>      use pattern-based input with the given names   
```

```lang-none

  --label       add a label attribute   
```

```lang-none

  --attr <String>      use a specific label attribute name (Default: label)   
```

### Required Output Arguments:

```lang-none

  --output <File>      specify the output   
```

### Optional Output Arguments:

```lang-none

  --output-table <File>      specify the output table name   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshComponents 

### Description:

  Compute connected components of a mesh 

### Required Input Arguments:

```lang-none

  --input <Mesh>      the input mesh   
```

### Optional Parameter Arguments:

```lang-none

  --largest       retain only the largest component   
```

```lang-none

  --attr <String>      the name of the component attribute (Default: index)   
```

```lang-none

  --select <String>      retain components selected with the given attribute   
```

```lang-none

  --area <Double>      retain components above a given surface area   
```

```lang-none

  --invert       invert the selection   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      the output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshConvert 

### Description:

  Convert a mesh between file formats 

### Required Input Arguments:

```lang-none

  --input <Mesh>      input mesh   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshCreateBox 

### Description:

  Create a box 

### Required Output Arguments:

```lang-none

  --output <Mesh>      output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshCreateCylinder 

### Description:

  Create a cylinder mesh 

### Optional Parameter Arguments:

```lang-none

  --startx <double>      input start x coordinate (Default: 0.0)   
```

```lang-none

  --starty <double>      input start y coordinate (Default: 0.0)   
```

```lang-none

  --startz <double>      input start z coordinate (Default: 0.0)   
```

```lang-none

  --endx <double>      input end x coordinate (Default: 1.0)   
```

```lang-none

  --endy <double>      input end y coordinate (Default: 10.0)   
```

```lang-none

  --endz <double>      input end z coordinate (Default: 10.0)   
```

```lang-none

  --radius <double>      input radius (Default: 5.0)   
```

```lang-none

  --resolution <int>      input resolution (Default: 5)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshCreateHemisphere 

### Description:

  Create a hemisphere mesh 

### Optional Parameter Arguments:

```lang-none

  --sudiv <int>      the number of subdivisions (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshCreateIcosahedron 

### Description:

  Create a icosahedron 

### Required Output Arguments:

```lang-none

  --output <Mesh>      output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshCreateOctahedron 

### Description:

  Create an octahedron 

### Required Output Arguments:

```lang-none

  --output <Mesh>      output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshCreateSphere 

### Description:

  Create a sphere mesh 

### Optional Parameter Arguments:

```lang-none

  --x <double>      input x coordinate (Default: 0.0)   
```

```lang-none

  --y <double>      input y coordinate (Default: 0.0)   
```

```lang-none

  --z <double>      input z coordinate (Default: 0.0)   
```

```lang-none

  --radius <double>      input radius (Default: 5.0)   
```

```lang-none

  --subdiv <int>      input subdivisions (Default: 2)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshCreateTetrahedron 

### Description:

  Create a tetrahedron 

### Required Output Arguments:

```lang-none

  --output <Mesh>      output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshCrop 

### Description:

  Crop a mesh 

### Required Input Arguments:

```lang-none

  --input <Mesh>      the input mesh   
```

### Optional Input Arguments:

```lang-none

  --solids <Solids>      remove vertices inside the given solids   
```

### Optional Parameter Arguments:

```lang-none

  --selection <String>      remove vertices that are selected with a non-zero attribute   
```

```lang-none

  --invert       invert the selection   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      the output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshCropSelection 

### Description:

  Crop a mesh from a selection 

### Required Input Arguments:

```lang-none

  --input <Mesh>      the input mesh   
```

### Optional Parameter Arguments:

```lang-none

  --invert       invert the selection   
```

```lang-none

  --attr <String>      the selection attribute (Default: selection)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      the output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshExtract 

### Description:

  Extract a subset of a mesh that matches the given attribute values 

### Required Input Arguments:

```lang-none

  --input <Mesh>      input mesh   
```

### Optional Parameter Arguments:

```lang-none

  --attr <String>      the attribute name of the label used to select (Default: label)   
```

```lang-none

  --include <String>      which labels to include, e.g. 1,2,3:5   
```

```lang-none

  --exclude <String>      which labels to exclude, e.g. 1,2,3:5   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshFeatures 

### Description:

  Compute geometric features of a mesh at each vertex.  A locally quadratic 
  approximation at each vertex is used to find curvature features 

### Required Input Arguments:

```lang-none

  --input <Mesh>      the input mesh   
```

### Optional Parameter Arguments:

```lang-none

  --attr <String>      the mesh attribute (Default: coord)   
```

```lang-none

  --rings <int>      the number of vertex rings to use for estimation (Default: 2)   
```

```lang-none

  --smooth <int>      the number of pre-smoothing iterations (Default: 0)   
```

```lang-none

  --refine       refine the vertex positions to match the quadratic estimate (a loess type     filter)   
```

```lang-none

  --step <double>      the gradient step size for refinement (Default: 0.001)   
```

```lang-none

  --thresh <double>      the error threshold for refinement (Default: 0.01)   
```

```lang-none

  --iters <int>      the maximum number of refinement iterations (Default: 1000)   
```

```lang-none

  --scaling <double>      the scaling factor for boundary estimation (Default: 2.0)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      the output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshFlipNormals 

### Description:

  Flip the normals of a mesh 

### Required Input Arguments:

```lang-none

  --input <Mesh>      the input mesh   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      the output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshForceCortex 

### Description:

  Compute the force field of a cortical mesh 

### Required Input Arguments:

```lang-none

  --input <Mesh>      input mesh   
```

### Optional Input Arguments:

```lang-none

  --refvolume <Volume>      input reference volume (exclusive with refmask)   
```

```lang-none

  --refmask <Mask>      input reference mask (exclusive with refvolume)   
```

```lang-none

  --lookup <Table>      input lookup table (must store the index and name of each label)   
```

### Optional Parameter Arguments:

```lang-none

  --pial <String>      pial attribute name (Default: pial)   
```

```lang-none

  --white <String>      white attribute name (Default: white)   
```

```lang-none

  --parc <String>      parcellation attribute name (Default: aparc)   
```

```lang-none

  --which <String>      only process certain parcellation labels (by default all will be processed)   
```

```lang-none

  --name <String>      the name field in the lookup table (Default: name)   
```

```lang-none

  --index <String>      the index field in the lookup table (Default: index)   
```

```lang-none

  --mindist <double>      the minimum white-pial to be included (Default: 1.0)   
```

```lang-none

  --samples <int>      the number of points to sample (Default: 15)   
```

```lang-none

  --inner <double>      the inner buffer size (Default: 2.0)   
```

```lang-none

  --outer <double>      the outer buffer size (Default: 2.0)   
```

```lang-none

  --nofill       skip the filling step   
```

```lang-none

  --smooth       apply fiber smoothing after projection   
```

```lang-none

  --sigma <Double>      apply smoothing with the given amount (bandwidth in mm) (default is largest     voxel dimension)   
```

```lang-none

  --support <int>      the smoothing filter radius in voxels (Default: 3)   
```

```lang-none

  --threads <int>      the number of threads (Default: 1)   
```

### Advanced Parameter Arguments:

```lang-none

  --name <String>      the name field in the lookup table (Default: name)   
```

```lang-none

  --index <String>      the index field in the lookup table (Default: index)   
```

```lang-none

  --sigma <Double>      apply smoothing with the given amount (bandwidth in mm) (default is largest     voxel dimension)   
```

```lang-none

  --support <int>      the smoothing filter radius in voxels (Default: 3)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output vectors   
```

### Optional Output Arguments:

```lang-none

  --labels <Mask>      the output labels   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshFuse 

### Description:

  fuse meshes 

### Required Input Arguments:

```lang-none

  --input <Meshes>      the input meshes   
```

### Optional Parameter Arguments:

```lang-none

  --pattern <String(s)>      specify a list of names that will be substituted with input %s   
```

### Optional Output Arguments:

```lang-none

  --vector <String>      specify vector attributes (comma-separated)   
```

```lang-none

  --discrete <String>      specify discrete attributes (comma-separated)   
```

```lang-none

  --output <Volume>      specify the output label probability   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshGetVects 

### Description:

  Extract a mesh vertex attribute as vectors 

### Required Input Arguments:

```lang-none

  --mesh <Mesh>      input mesh   
```

### Optional Parameter Arguments:

```lang-none

  --name <String>      the destination attribute name (Default: attr)   
```

### Required Output Arguments:

```lang-none

  --output <Vects>      output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshGrow 

### Description:

  Grow a mesh to fill a volume 

### Required Input Arguments:

```lang-none

  --input <Mesh>      the input mesh   
```

```lang-none

  --reference <Volume>      the reference volume   
```

### Optional Parameter Arguments:

```lang-none

  --source <String>      the source vertex attribute (default is the vertex position) (Default:     coord)   
```

```lang-none

  --dest <String>      the destination vertex attribute (the result will be saved here) (Default:     shell)   
```

```lang-none

  --dist <String>      the distance to the best match (the result will be saved here) (Default:     dist)   
```

```lang-none

  --threshold <double>      the threshold (Default: 0.5)   
```

```lang-none

  --distance <double>      the maximums distance to search (Default: 20.0)   
```

```lang-none

  --step <double>      the maximums distance to search (Default: 0.25)   
```

```lang-none

  --invert       detect and increase past the threshold (the default is to detect the drop)   
```

```lang-none

  --outside       search inside the mesh (the default is to search outside)   
```

```lang-none

  --interp <InterpolationType>      image interpolation method (Options: Nearest, Trilinear, Tricubic,     Gaussian, GaussianLocalLinear, GaussianLocalQuadratic) (Default: Trilinear)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      the output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshHoleFill 

### Description:

  Fill holes in a mesh.  This can be applied to either the entire mesh or a 
  selection 

### Required Input Arguments:

```lang-none

  --input <Mesh>      the input mesh   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      the output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshHull 

### Description:

  Compute the convex hull of a mesh 

### Required Input Arguments:

```lang-none

  --input <Mesh>      input mesh   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      output hull mesh   
```

### Author:

  Ryan Cabeen 

### Citation:

  Barber, C. B., Dobkin, D. P., & Huhdanpaa, H. (1996). The quickhull algorithm 
  for convex hulls. ACM Transactions on Mathematical Software (TOMS), 22(4), 
  469-483. 

<hr/>
## MeshMapSphere 

### Description:

  Convert a mesh between file formats 

### Required Input Arguments:

```lang-none

  --input <Mesh>      input mesh   
```

### Optional Parameter Arguments:

```lang-none

  --preiters <int>      the world space Laplacian smoothing iterations (Default: 100)   
```

```lang-none

  --iters <int>      the Laplacian smoothing iterations (Default: 100)   
```

```lang-none

  --lambmin <double>      the minimum Laplacian smoothing rate (Default: 0.1)   
```

```lang-none

  --lambmax <double>      the maximum Laplacian smoothing rate (Default: 0.5)   
```

```lang-none

  --gain <double>      the curvature gain (Default: 5.0)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshMath 

### Description:

  Evaluate an expression at each vertex of a mesh 

### Required Input Arguments:

```lang-none

  --input <Mesh>      the input mesh   
```

### Optional Parameter Arguments:

```lang-none

  --expression <String>      the expression to evaluate (Default: x > 0.5)   
```

```lang-none

  --result <String>      the attribute name for the result (Default: result)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshMeasure 

### Description:

  Measure global properties of a mesh 

### Required Input Arguments:

```lang-none

  --input <Mesh>      input mesh   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshMeasureRegion 

### Description:

  Measure regional attributes properties of a mesh 

### Required Input Arguments:

```lang-none

  --input <Mesh>      input mesh   
```

### Optional Input Arguments:

```lang-none

  --lookup <Table>      input lookup table   
```

```lang-none

  --labels <Vects>      add the following per-vertex labeling to use for computing region     statistics   
```

### Required Parameter Arguments:

```lang-none

  --attr <String>      compute statistics of the following attributes (a comma-delimited list can     be used)   
```

```lang-none

  --output <String>      output directory   
```

### Optional Parameter Arguments:

```lang-none

  --label <String>      use the given vertex attribute to identify regions and compute statistics     (Default: label)   
```

```lang-none

  --lutname <String>      the lookup name field (Default: name)   
```

```lang-none

  --lutindex <String>      the lookup index field (Default: index)   
```

```lang-none

  --outname <String>      the output name field (Default: name)   
```

```lang-none

  --outvalue <String>      the output value field (Default: value)   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshNormals 

### Description:

  Compute vertex normals of a mesh 

### Required Input Arguments:

```lang-none

  --input <Mesh>      the input mesh   
```

### Optional Parameter Arguments:

```lang-none

  --attr <String>      the mesh attribute (Default: coord)   
```

```lang-none

  --smooth <int>      the number of smoothing iterations (Default: 0)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      the output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshPrintInfo 

### Description:

  Print basic information about a mesh 

### Required Input Arguments:

```lang-none

  --input <Mesh>      the input mesh   
```

### Optional Parameter Arguments:

```lang-none

  --stats       print statistics   
```

```lang-none

  --area       print statistics of triangle areas   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshRegionBoundary 

### Description:

  Extract the boundaries of mesh regions 

### Required Input Arguments:

```lang-none

  --input <Mesh>      the input Mesh   
```

### Optional Parameter Arguments:

```lang-none

  --coords <String>      a comma-separated list of coordinate attributes to lift (Default: coord)   
```

```lang-none

  --label <String>      the label attribute name (Default: coord)   
```

```lang-none

  --lift <double>      apply the given amount of lift off the mesh (Default: 0.0)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      the output Mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshSample 

### Description:

  Sample a volume at input vertices 

### Required Input Arguments:

```lang-none

  --input <Mesh>      input input   
```

```lang-none

  --volume <Volume>      input volume   
```

### Optional Parameter Arguments:

```lang-none

  --interp <InterpolationType>      interpolation method (Options: Nearest, Trilinear, Tricubic, Gaussian,     GaussianLocalLinear, GaussianLocalQuadratic) (Default: Trilinear)   
```

```lang-none

  --coord <String>      coord attribute name (used for sampling volume) (Default: coord)   
```

```lang-none

  --attr <String>      output attribute name (where the sampled image data will be stored)     (Default: sampled)   
```

```lang-none

  --window <Double>      compute a statistical summary of image intensities sampled along points     within a certain window of each vertex (units: mm), sampled along the     surface normal direction   
```

```lang-none

  --sample <MeshSampleDirection>      compute the statistical summary in a given direction (default is both     inside and outside the mesh) (Options: Both, Inside, Outside) (Default:     Both)   
```

```lang-none

  --stat <MeshSampleStatistic>      use the given statistic for summarizing the vertex window (Options: Mean,     Min, Max) (Default: Mean)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      the output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshSelect 

### Description:

  Select vertices of a mesh 

### Required Input Arguments:

```lang-none

  --input <Mesh>      the input mesh   
```

### Optional Input Arguments:

```lang-none

  --solids <Solids>      some solids   
```

### Optional Parameter Arguments:

```lang-none

  --boundary       select the boundary   
```

```lang-none

  --invert       invert the selection   
```

```lang-none

  --attr <String>      the selection attribute (Default: selection)   
```

```lang-none

  --value <double>      the selection value (Default: 1.0)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      the output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshSelectionClose 

### Description:

  Close a mesh vertex selection 

### Required Input Arguments:

```lang-none

  --input <Mesh>      input mesh   
```

### Optional Parameter Arguments:

```lang-none

  --largest       select the largest component as an intermediate step   
```

```lang-none

  --num <int>      the number of iterations (Default: 1)   
```

```lang-none

  --attr <String>      the selection attribute (Default: selection)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshSelectionComponents 

### Description:

  Compute connected components of a mesh selection 

### Required Input Arguments:

```lang-none

  --input <Mesh>      the input mesh   
```

### Optional Parameter Arguments:

```lang-none

  --largest       retain only the largest component   
```

```lang-none

  --attr <String>      the name of the selection attribute (Default: selection)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      the output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshSelectionDilate 

### Description:

  Dilate a mesh selection 

### Required Input Arguments:

```lang-none

  --input <Mesh>      input mesh   
```

### Optional Parameter Arguments:

```lang-none

  --num <int>      the number of iterations (Default: 1)   
```

```lang-none

  --attr <String>      the selection attribute (Default: selection)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshSelectionErode 

### Description:

  Erode a mesh selection 

### Required Input Arguments:

```lang-none

  --input <Mesh>      input mesh   
```

### Optional Parameter Arguments:

```lang-none

  --num <int>      the number of iterations (Default: 1)   
```

```lang-none

  --attr <String>      the selection attribute (Default: selection)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshSelectionOpen 

### Description:

  Open a mesh selection 

### Required Input Arguments:

```lang-none

  --input <Mesh>      input mesh   
```

### Optional Parameter Arguments:

```lang-none

  --largest       select the largest component as an intermediate step   
```

```lang-none

  --num <int>      the number of iterations (Default: 1)   
```

```lang-none

  --attr <String>      the selection attribute (Default: selection)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshSetTable 

### Description:

  Create a table listing regions of a mask 

### Required Input Arguments:

```lang-none

  --mesh <Mesh>      input mesh   
```

```lang-none

  --table <Table>      input table   
```

### Optional Input Arguments:

```lang-none

  --lookup <Table>      a lookup table to relate names to indices   
```

### Optional Parameter Arguments:

```lang-none

  --merge <String>      a field name to merge on (Default: name)   
```

```lang-none

  --index <String>      the table index field name (Default: index)   
```

```lang-none

  --value <String>      a table field to get (can be comma delimited) (Default: value)   
```

```lang-none

  --mindex <String>      the mesh index field name (defaults to table index field name)   
```

```lang-none

  --mvalue <String>      the mesh value field name (defaults to table value field name)   
```

```lang-none

  --background <double>      a background value (Default: 0.0)   
```

```lang-none

  --missing <Double>      an missing value   
```

```lang-none

  --remove       remove triangles with vertices that are not included in the input table   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshSetVects 

### Description:

  Add vects as an attribute on a mesh (number of vects must match the number of 
  vertices) 

### Required Input Arguments:

```lang-none

  --mesh <Mesh>      input mesh   
```

```lang-none

  --vects <Vects>      input vects   
```

### Optional Parameter Arguments:

```lang-none

  --name <String>      the destination attribute name (Default: attr)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshSimplify 

### Description:

  Simplify a input by removing short edges 

### Required Input Arguments:

```lang-none

  --input <Mesh>      the input mesh   
```

### Optional Parameter Arguments:

```lang-none

  --maxiter <Integer>      maxima number of iterations (Default: 10000)   
```

```lang-none

  --maxvert <Integer>      maxima number of vertices   
```

```lang-none

  --maxedge <Integer>      maxima number of edges   
```

```lang-none

  --maxface <Integer>      maxima number of faces   
```

```lang-none

  --meanarea <Double>      maxima average face surface area   
```

```lang-none

  --nobound       do not remove boundary vertices   
```

```lang-none

  --chatty       print messages   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      the output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshSmooth 

### Description:

  Smooth a mesh 

### Required Input Arguments:

```lang-none

  --input <Mesh>      the input   
```

### Optional Parameter Arguments:

```lang-none

  --attr <String>      the attribute to smooth (Default: coord)   
```

```lang-none

  --num <int>      a number of iterations (Default: 5)   
```

```lang-none

  --lambda <Double>      the lambda laplacian smoothing parameter (Default: 0.3)   
```

```lang-none

  --mu <Double>      the optional mu parameter for Taubin's method   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      the output mesh   
```

### Author:

  Ryan Cabeen 

### Citation:

  Taubin, G. (1995, September). A signal processing approach to fair surface 
  design. In Proceedings of the 22nd annual conference on Computer graphics and 
  interactive techniques (pp. 351-358). ACM. 

<hr/>
## MeshSubdivide 

### Description:

  Subdivide a mesh.  Each triangle is split into four with new vertices that 
  split each edge in two. 

### Required Input Arguments:

```lang-none

  --input <Mesh>      the input mesh   
```

### Optional Parameter Arguments:

```lang-none

  --num <int>      a number of subdivisions (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      the output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshTable 

### Description:

  Create a table from mesh vertex attributes 

### Required Input Arguments:

```lang-none

  --input <Mesh>      input input   
```

### Optional Parameter Arguments:

```lang-none

  --vertices <String>      which vertices retain (comma separated)   
```

```lang-none

  --which <String>      which attributes to retain (comma separated) (Default: coord)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshTransform 

### Description:

  Transform a mesh 

### Required Input Arguments:

```lang-none

  --input <Mesh>      input mesh   
```

### Optional Input Arguments:

```lang-none

  --affine <Affine>      apply an affine xfm   
```

```lang-none

  --invaffine <Affine>      apply an inverse affine xfm   
```

```lang-none

  --deform <Deformation>      apply a deformation xfm   
```

```lang-none

  --pose <Volume>      apply a transform to match the pose of a volume   
```

```lang-none

  --invpose <Volume>      apply a transform to match the inverse pose of a volume   
```

### Optional Parameter Arguments:

```lang-none

  --reverse       reverse the order, i.e. compose the affine(deform(x)), whereas the default     is deform(affine(x))   
```

```lang-none

  --attrs <String>      a comma-separated list of attributes to transform (Default: coord)   
```

```lang-none

  --tx <Double>      translate the mesh in by the given about in the x dimension   
```

```lang-none

  --ty <Double>      translate the mesh in by the given about in the y dimension   
```

```lang-none

  --tz <Double>      translate the mesh in by the given about in the z dimension   
```

```lang-none

  --sx <Double>      scale the mesh in by the given about in the x dimension   
```

```lang-none

  --sy <Double>      scale the mesh in by the given about in the y dimension   
```

```lang-none

  --sz <Double>      scale the mesh in by the given about in the z dimension   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshVertexMath 

### Description:

  Evaluate an expression at each vertex of a mesh 

### Required Input Arguments:

```lang-none

  --input <Mesh>      the input mesh   
```

### Optional Parameter Arguments:

```lang-none

  --expression <String>      the expression to evaluate (Default: x > 0.5)   
```

```lang-none

  --result <String>      the attribute for the result (Default: result)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      the output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshVertices 

### Description:

  Extract the vertices of a mesh 

### Required Input Arguments:

```lang-none

  --input <Mesh>      input mesh   
```

### Optional Parameter Arguments:

```lang-none

  --attribute <String>      attribute to extract (Default: coord)   
```

```lang-none

  --query <String>      an attribute to query (Default: label)   
```

```lang-none

  --value <Integer>      an integer value to query   
```

```lang-none

  --nonzero       select only vertices that have a non-zero query attribute   
```

### Required Output Arguments:

```lang-none

  --output <Vects>      output vects   
```

### Author:

  Ryan Cabeen 

<hr/>
## MeshVoxelize 

### Description:

  Voxelize a mesh to a mask (should be watertight if the mesh is also) 

### Required Input Arguments:

```lang-none

  --input <Mesh>      input input   
```

```lang-none

  --reference <Mask>      reference mask   
```

### Optional Parameter Arguments:

```lang-none

  --label <Integer>      a label to draw (Default: 1)   
```

```lang-none

  --attr <String>      a attribute to draw (instead of the constant label)   
```

```lang-none

  --coord <String>      the coordinate attribute to use (not applicable when specifying inner and     outer shells) (Default: coord)   
```

```lang-none

  --innerAttr <String>      label vertices in a shell between an inner and outer spatial attribute,     e.g. like white and pial cortical surfaces   
```

```lang-none

  --outerAttr <String>      label vertices in a shell between an inner and outer spatial attribute,     e.g. like white and pial cortical surfaces   
```

```lang-none

  --innerBuffer <Double>      label vertices in a shell between this attribute and the primary one, e.g.     like white and pial cortical surfaces   
```

```lang-none

  --outerBuffer <Double>      label vertices in a shell between this attribute and the primary one, e.g.     like white and pial cortical surfaces   
```

```lang-none

  --fill       fill the inside of the mask   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      the output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## NeuronCat 

### Description:

  Concatenate neuron files 

### Required Input Arguments:

```lang-none

  --input <Neuron(s)>      specify an input neuron files   
```

### Required Output Arguments:

```lang-none

  --output <Neuron>      specify the neuron file   
```

### Author:

  Ryan Cabeen 

<hr/>
## NeuronCatPair 

### Description:

  Combine two neurons into a single object 

### Required Input Arguments:

```lang-none

  --left <Neuron>      the input left neuron   
```

```lang-none

  --right <Neuron>      the input right neuron   
```

### Required Output Arguments:

```lang-none

  --output <Neuron>      output neuron   
```

### Author:

  Ryan Cabeen 

<hr/>
## NeuronCrossing 

### Description:

  Detect the crossing points between two neurons 

### Required Input Arguments:

```lang-none

  --left <Neuron>      the input left neuron   
```

```lang-none

  --right <Neuron>      the input right neuron   
```

### Optional Parameter Arguments:

```lang-none

  --thresh <Double>      the minimum distance to be considered a crossing (by default it will be the     average inter-node distance)   
```

```lang-none

  --mode <NeuronCrossingMode>      specify a statistic for computing the threshold (not relevant if you     provide a specific value) (Options: Min, Max, Mean) (Default: Mean)   
```

### Required Output Arguments:

```lang-none

  --output <Vects>      output vects   
```

### Author:

  Ryan Cabeen 

<hr/>
## NeuronExport 

### Description:

  Export a neuron file to vects and curves 

### Required Input Arguments:

```lang-none

  --input <Neuron>      the input filename to the SWC neuron file   
```

### Optional Output Arguments:

```lang-none

  --outputRoots <Vects>      output roots   
```

```lang-none

  --outputForks <Vects>      output forks   
```

```lang-none

  --outputLeaves <Vects>      output leaves   
```

```lang-none

  --outputForest <Curves>      output forest   
```

### Author:

  Ryan Cabeen 

<hr/>
## NeuronFilter 

### Description:

  Filter a neuron 

### Required Input Arguments:

```lang-none

  --input <Neuron>      the input neuron   
```

### Optional Parameter Arguments:

```lang-none

  --which <String>      select only the neurons with the given root labels (comma-separated list)   
```

```lang-none

  --laplacianIters <Integer>      apply laplacian smoothing the given number of times   
```

```lang-none

  --laplacianLambda <Double>      the smoothing weight for laplacian smoothing (only relevant if smoothing is     enabled) (Default: 0.25)   
```

```lang-none

  --lowessNum <Integer>      apply lowess smoothing with the given neighborhood size, e.g. 5   
```

```lang-none

  --lowessOrder <Integer>      apply lowess smoothing with the given local polynomial order (Default: 2)   
```

```lang-none

  --simplify <Double>      simplify the neuron segments with the Douglas-Peucker algorithm   
```

```lang-none

  --cut <Double>      cut away the portion of the neuron past the given Euclidean distance away     from the root   
```

```lang-none

  --relabel       relabel the nodes to be sequential   
```

```lang-none

  --sort       perform a topological sorting of nodes   
```

```lang-none

  --debase       separate the basal dendrite (detected by the soma branch with the farthest     reach)   
```

### Advanced Parameter Arguments:

```lang-none

  --relabel       relabel the nodes to be sequential   
```

```lang-none

  --sort       perform a topological sorting of nodes   
```

```lang-none

  --debase       separate the basal dendrite (detected by the soma branch with the farthest     reach)   
```

### Required Output Arguments:

```lang-none

  --output <Neuron>      output neuron   
```

### Author:

  Ryan Cabeen 

<hr/>
## NeuronMeasure 

### Description:

  Measure statistics of a neuron file 

### Required Input Arguments:

```lang-none

  --input <Neuron>      the input neuron   
```

### Optional Parameter Arguments:

```lang-none

  --single       include statistics for single neurons   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## NeuronMesh 

### Description:

  Create a mesh from a neuron file 

### Required Input Arguments:

```lang-none

  --input <Neuron>      the input filename to the SWC neuron file   
```

### Optional Parameter Arguments:

```lang-none

  --noRootMesh       exclude sphere meshes for roots   
```

```lang-none

  --noForkMesh       exclude sphere meshes for forks   
```

```lang-none

  --noLeafMesh       exclude sphere meshes for leaves   
```

```lang-none

  --noTrunkMesh       exclude tube meshes for trunks   
```

```lang-none

  --noSphereSculpt       use constant radius for spheres (the default will modulate the thickness by     the neuron radius)   
```

```lang-none

  --noTrunkSculpt       use constant thickness tubes (the default will modulate the thickness by     the neuron radius)   
```

```lang-none

  --trunkColorMode <TrunkColorMode>      the coloring mode for trunks (Options: Solid, DEC, Segment, Tree) (Default:     Solid)   
```

```lang-none

  --rootScale <double>      the size of root spheres (Default: 1.0)   
```

```lang-none

  --forkScale <double>      the size of fork spheres (Default: 1.0)   
```

```lang-none

  --leafScale <double>      the size of leaf spheres (Default: 1.0)   
```

```lang-none

  --trunkScale <double>      the size of trunk tubes (Default: 1.0)   
```

```lang-none

  --rootColor <SolidColor>      the color of root spheres (Options: White, Cyan, Yellow, Blue, Magenta,     Orange, Green, Pink, GRAY, DarkGray, LightGray) (Default: Orange)   
```

```lang-none

  --forkColor <SolidColor>      the color of fork spheres (Options: White, Cyan, Yellow, Blue, Magenta,     Orange, Green, Pink, GRAY, DarkGray, LightGray) (Default: Cyan)   
```

```lang-none

  --leafColor <SolidColor>      the color of leaf spheres (Options: White, Cyan, Yellow, Blue, Magenta,     Orange, Green, Pink, GRAY, DarkGray, LightGray) (Default: Yellow)   
```

```lang-none

  --trunkColor <SolidColor>      the color of trunk tubes (if the coloring mode is solid) (Options: White,     Cyan, Yellow, Blue, Magenta, Orange, Green, Pink, GRAY, DarkGray,     LightGray) (Default: White)   
```

```lang-none

  --noColor       remove all colors (and invalidates any other color options)   
```

```lang-none

  --sphereResolution <int>      the resolution of the spheres i.e. the number of subdivisions, so be     careful with values above 3 (Default: 0)   
```

```lang-none

  --tubeResolution <int>      the resolution of the tubes, i.e. the number of radial spokes (Default: 5)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## NeuronPrintInfo 

### Description:

  Measure statistics of a neuron file 

### Required Input Arguments:

```lang-none

  --input <Neuron>      the input neuron   
```

### Author:

  Ryan Cabeen 

<hr/>
## NeuronSplit 

### Description:

  Split a SWC file to produce a single file per neuron 

### Required Input Arguments:

```lang-none

  --input <Neuron>      the input neuron   
```

### Required Parameter Arguments:

```lang-none

  --output <String>      output filename (if you include %d, it will be substituted with the root     label, otherwise it will be renamed automatically)   
```

### Optional Parameter Arguments:

```lang-none

  --root       use the root neuron label for naming the output file (i.e. the number     substituted for %d)   
```

### Author:

  Ryan Cabeen 

<hr/>
## NeuronStitch 

### Description:

  Perform stitching to build neuron segments into a complete neuron.  The 
  neuron will be split into pieces and rebuilt. 

### Required Input Arguments:

```lang-none

  --input <Neuron>      the input neuron   
```

### Optional Input Arguments:

```lang-none

  --somas <Solids>      a solids object for manually defining somas for stitching, e.g. each box     and sphere will define a region for building somas (only used when the soma     mode is Manual)   
```

### Optional Parameter Arguments:

```lang-none

  --mode <NeuronStitchSomaMode>      choose a mode for selecting the soma(s).  Manual selection is for     user-defined soma (from a solids object passed to the somas option).      RadiusMax is for choosing the soma from the node with the largest radius.      Cluster is for detecting the soma by clustering endpoints and finding the     largest cluster (depends on the cluster option for defining the cluster     spatial extent) (Options: Manual, RadiusMax, Cluster) (Default: RadiusMax)   
```

```lang-none

  --threshold <Double>      a maximum distance for stitching together segments.  If two segments are     farther than this distance, they will not be stitched (default is to stitch     everything   
```

```lang-none

  --cluster <Double>      a specific radius for cluster-based soma selection (only used for the     Cluster soma detection mode)   
```

### Required Output Arguments:

```lang-none

  --output <Neuron>      output neuron   
```

### Author:

  Ryan Cabeen 

<hr/>
## NeuronTransform 

### Description:

  Apply a spatial transformation to a neuron 

### Required Input Arguments:

```lang-none

  --input <Neuron>      the neuron   
```

### Optional Input Arguments:

```lang-none

  --affine <Affine>      apply an affine xfm   
```

```lang-none

  --invaffine <Affine>      apply an inverse affine xfm   
```

### Optional Parameter Arguments:

```lang-none

  --xshift <double>      translate the neuron position in x by the given amount (Default: 0.0)   
```

```lang-none

  --yshift <double>      translate the neuron position in y by the given amount (Default: 0.0)   
```

```lang-none

  --zshift <double>      translate the neuron position in z by the given amount (Default: 0.0)   
```

```lang-none

  --xscale <double>      scale the neuron position in x by the given amount (Default: 1.0)   
```

```lang-none

  --yscale <double>      scale the neuron position in y by the given amount (Default: 1.0)   
```

```lang-none

  --zscale <double>      scale the neuron position in z by the given amount (Default: 1.0)   
```

```lang-none

  --rshift <double>      add the given amount to the radius (Default: 0.0)   
```

```lang-none

  --rscale <double>      scale the neuron radius by the given amount (Default: 1.0)   
```

```lang-none

  --rset <Double>      set the radius to a constant value   
```

```lang-none

  --jitter <Double>      jitter the position of the nodes by a random amount   
```

```lang-none

  --jitterEnds       jitter only leaves and roots   
```

```lang-none

  --swap <String>      swap a pair of coordinates (xy, xz, or yz)   
```

### Required Output Arguments:

```lang-none

  --output <Neuron>      output neuron   
```

### Author:

  Ryan Cabeen 

<hr/>
## SolidsBox 

### Description:

  Compute the bounding box of solids 

### Required Input Arguments:

```lang-none

  --input <Solids>      input solids   
```

### Required Output Arguments:

```lang-none

  --output <Solids>      output bounding box   
```

### Author:

  Ryan Cabeen 

<hr/>
## SolidsCat 

### Description:

  Concatenate solids 

### Required Input Arguments:

```lang-none

  --left <Solids>      input solids   
```

```lang-none

  --right <Solids>      input solids   
```

### Required Output Arguments:

```lang-none

  --output <Solids>      output solids   
```

### Author:

  Ryan Cabeen 

<hr/>
## SolidsConvert 

### Description:

  Convert solids between file formats 

### Required Input Arguments:

```lang-none

  --input <Solids>      input solids   
```

### Required Output Arguments:

```lang-none

  --output <Solids>      output sphere   
```

### Author:

  Ryan Cabeen 

<hr/>
## SolidsCreateBox 

### Description:

  Create a soilds object containing a box 

### Optional Parameter Arguments:

```lang-none

  --xmin <double>      a box minima value in x (Default: 0.0)   
```

```lang-none

  --ymin <double>      a box minima in y (Default: 0.0)   
```

```lang-none

  --zmin <double>      a box minima in z (Default: 0.0)   
```

```lang-none

  --xmax <double>      a box maxima value in x (Default: 1.0)   
```

```lang-none

  --ymax <double>      a box maxima in y (Default: 1.0)   
```

```lang-none

  --zmax <double>      a box maxima in z (Default: 1.0)   
```

### Required Output Arguments:

```lang-none

  --output <Solids>      output solids   
```

### Author:

  Ryan Cabeen 

<hr/>
## SolidsCreateEmpty 

### Description:

  Create an empty soilds object 

### Required Output Arguments:

```lang-none

  --output <Solids>      output solids   
```

### Author:

  Ryan Cabeen 

<hr/>
## SolidsCreatePlane 

### Description:

  Create a soilds object containing a plane 

### Optional Parameter Arguments:

```lang-none

  --xpoint <double>      the x coordinate of a point on the plane (Default: 0.0)   
```

```lang-none

  --ypoint <double>      the y coordinate of a point on the plane (Default: 0.0)   
```

```lang-none

  --zpoint <double>      the z coordinate of a point on the plane (Default: 0.0)   
```

```lang-none

  --xnormal <double>      the x coordinate of the plane normal (Default: 0.0)   
```

```lang-none

  --ynormal <double>      the y coordinate of the plane normal (Default: 0.0)   
```

```lang-none

  --znormal <double>      the z coordinate of the plane normal (Default: 0.0)   
```

### Required Output Arguments:

```lang-none

  --output <Solids>      output solids   
```

### Author:

  Ryan Cabeen 

<hr/>
## SolidsCreateSphere 

### Description:

  Create a soilds object containing a sphere 

### Optional Parameter Arguments:

```lang-none

  --x <double>      a sphere x center (Default: 0.0)   
```

```lang-none

  --y <double>      a sphere y center (Default: 0.0)   
```

```lang-none

  --z <double>      a sphere z center (Default: 0.0)   
```

```lang-none

  --radius <double>      a sphere radius (Default: 10.0)   
```

### Required Output Arguments:

```lang-none

  --output <Solids>      output solids   
```

### Author:

  Ryan Cabeen 

<hr/>
## SolidsMask 

### Description:

  Create a mask from solids 

### Required Input Arguments:

```lang-none

  --input <Solids>      input solids   
```

### Optional Parameter Arguments:

```lang-none

  --dx <double>      sample spacing in x (Default: 1.0)   
```

```lang-none

  --dy <double>      sample spacing in x (Default: 1.0)   
```

```lang-none

  --dz <double>      sample spacing in x (Default: 1.0)   
```

```lang-none

  --label <int>      label (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## SolidsMesh 

### Description:

  Create a mask from solids 

### Required Input Arguments:

```lang-none

  --input <Solids>      input solids   
```

### Optional Parameter Arguments:

```lang-none

  --delta <double>      the voxel resolution for discretization (Default: 1.0)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## SolidsPrintInfo 

### Description:

  Print basic information about a solids 

### Required Input Arguments:

```lang-none

  --input <Solids>      the input solids   
```

### Author:

  Ryan Cabeen 

<hr/>
## SolidsSample 

### Description:

  Sample vects inside solids 

### Required Input Arguments:

```lang-none

  --input <Solids>      input solids   
```

### Optional Parameter Arguments:

```lang-none

  --num <int>      the number of points to sample (Default: 100)   
```

### Required Output Arguments:

```lang-none

  --output <Vects>      output sampled vects   
```

### Author:

  Ryan Cabeen 

<hr/>
## SolidsToWaypoints 

### Description:

  Create a waypoint mask from two solids 

### Required Input Arguments:

```lang-none

  --a <Solids>      first input set of solids   
```

```lang-none

  --b <Solids>      second input set of solids   
```

```lang-none

  --ref <Mask>      input reference mask   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output waypoint mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## SurfaceMeasure 

### Description:

  Compute measures of brain surfaces, including surface area, hull surface 
  area, gyrification, and lateralization, i.e. (left - right) / (left + right). 

### Required Input Arguments:

```lang-none

  --input <dir>      specify an input surface directory   
```

### Required Output Arguments:

```lang-none

  --output <fn>      specify an output measure filename   
```

### Author:

  Ryan Cabeen 

<hr/>
## TableCat 

### Description:

  Concatenate the rows of two tables.  By default, it will only include fields 
  that are common to both input tables. 

### Optional Input Arguments:

```lang-none

  --x <Table>      input first table   
```

```lang-none

  --y <Table>      input second table   
```

### Optional Parameter Arguments:

```lang-none

  --outer       include all possible fields   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## TableConvert 

### Description:

  Convert a table between file formats 

### Required Input Arguments:

```lang-none

  --input <Table>      input table   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## TableDistanceMatrix 

### Description:

  Create a distance matrix from pairs of distances 

### Required Input Arguments:

```lang-none

  --input <Table>      input table (should have field for the left and right identifiers)   
```

### Optional Parameter Arguments:

```lang-none

  --left <String>      the identifier for left values (Default: left)   
```

```lang-none

  --right <String>      the identifier for right values (Default: right)   
```

```lang-none

  --value <String>      the identifier for right values (Default: value)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## TableFilter 

### Description:

  Filter a table, a few options are available 

### Required Input Arguments:

```lang-none

  --input <Table>      input table   
```

```lang-none

  --volumes <Table>      add a field for the mean value based on a table of volumes for each field     (this is an unusual need)   
```

### Optional Parameter Arguments:

```lang-none

  --meanField <String>      the name of the field for saving the mean value (Default: mean)   
```

```lang-none

  --mergeField <String>      the name of the field for merging tables (Default: subject)   
```

```lang-none

  --missing <String>      a token for missing values (Default: NA)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## TableHemiMean 

### Description:

  Compute the left-right average of a lateralized dataset 

### Required Input Arguments:

```lang-none

  --input <Table>      input table (should use the naming convention like the module parameters)   
```

### Optional Parameter Arguments:

```lang-none

  --left <String>      the identifier for left values (Default: lh_)   
```

```lang-none

  --right <String>      the identifier for right values (Default: rh_)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## TableLaterality 

### Description:

  Compute the lateralization index of a given map.  This assumes you have a two 
  column table (name,value) that contains left and right measurements (see 
  module parameters to specify the left/right identifiers) 

### Required Input Arguments:

```lang-none

  --input <Table>      input table (should use the naming convention like the module parameters)   
```

### Optional Parameter Arguments:

```lang-none

  --left <String>      the identifier for left values (Default: lh_)   
```

```lang-none

  --right <String>      the identifier for right values (Default: rh_)   
```

```lang-none

  --indlat <String>      the identifier for the lateralization index (Default: indlat_)   
```

```lang-none

  --abslat <String>      the identifier for the absolute lateralization index (Default: abslat_)   
```

```lang-none

  --unilat <String>      the identifier for the unilateral averaging (Default: unilat_)   
```

```lang-none

  --minlat <String>      the identifier for the left right minimum (Default: minlat_)   
```

```lang-none

  --maxlat <String>      the identifier for the left right maximum (Default: maxlat_)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## TableLateralityIndex 

### Description:

  Compute the lateralization index of a given map.  This assumes you have a two 
  column table (name,value) that contains left and right measurements (see 
  module parameters to specify the left/right identifiers) 

### Required Input Arguments:

```lang-none

  --input <Table>      input table (should use the naming convention like the module parameters)   
```

### Optional Parameter Arguments:

```lang-none

  --name <String>      the field holding the name of the measurement (Default: name)   
```

```lang-none

  --value <String>      the field holding the value of the measurement (Default: value)   
```

```lang-none

  --left <String>      the identifier for left values (Default: lh_)   
```

```lang-none

  --right <String>      the identifier for right values (Default: rh_)   
```

```lang-none

  --lat <String>      the identifier for the lateralization index (Default: lat_)   
```

```lang-none

  --abslat <String>      the identifier for the absolute lateralization index (Default: abslat_)   
```

```lang-none

  --mean <String>      the identifier for the left-right average (Default: mean_)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## TableMapMath 

### Description:

  Evaluate an expression with a map (a table listing name/value pairs) 

### Required Input Arguments:

```lang-none

  --input <Table>      the input table map   
```

### Optional Parameter Arguments:

```lang-none

  --expression <String>      the expression to evaluate (Default: x > 0.5)   
```

```lang-none

  --name <String>      the field name for the name (Default: name)   
```

```lang-none

  --value <String>      the field name for the value (Default: value)   
```

```lang-none

  --result <String>      the name of the result (Default: result)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## TableMath 

### Description:

  Evaluate an expression for each row of a table 

### Required Input Arguments:

```lang-none

  --input <Table>      the input table   
```

### Optional Parameter Arguments:

```lang-none

  --expression <String>      the expression to evaluate (Default: x > 0.5)   
```

```lang-none

  --result <String>      the field name for the result (Default: result)   
```

```lang-none

  --na <String>      the string used for invalid values (Default: NA)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## TableMerge 

### Description:

  Merge two tables based the value of a shared field 

### Required Input Arguments:

```lang-none

  --left <Table>      input left table   
```

```lang-none

  --right <Table>      input right table   
```

### Optional Parameter Arguments:

```lang-none

  --field <String>      the common field to merge on (Default: name)   
```

```lang-none

  --leftField <String>      use the given left field (instead of the shared merge field)   
```

```lang-none

  --rightField <String>      use the given right field (instead of the shared merge field)   
```

```lang-none

  --leftPrefix <String>      add a prefix to the fields from the left table (Default: )   
```

```lang-none

  --rightPrefix <String>      add a prefix to the fields from the right table (Default: )   
```

```lang-none

  --leftPostfix <String>      add a postfix to the fields from the left table (Default: )   
```

```lang-none

  --rightPostfix <String>      add a postfix to the fields from the right table (Default: )   
```

### Advanced Parameter Arguments:

```lang-none

  --leftField <String>      use the given left field (instead of the shared merge field)   
```

```lang-none

  --rightField <String>      use the given right field (instead of the shared merge field)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## TableNarrow 

### Description:

  Narrow a table to reduce the number of fields 

### Required Input Arguments:

```lang-none

  --input <Table>      input table   
```

### Optional Parameter Arguments:

```lang-none

  --name <String>      the new field for each field name (Default: name)   
```

```lang-none

  --value <String>      the new field for each field value (Default: value)   
```

```lang-none

  --keep <String>      fields to keep (comma separated)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## TableOutliers 

### Description:

  Compute z-scores to detect outliers in a table 

### Required Input Arguments:

```lang-none

  --input <Table>      input table   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## TablePrintData 

### Description:

  Print data from a table 

### Required Input Arguments:

```lang-none

  --input <Table>      the input table   
```

### Optional Parameter Arguments:

```lang-none

  --where <String>      an predicate for selecting records (using existing field names)   
```

```lang-none

  --name <String>      the name variable for use in the select predicate (Default: name)   
```

```lang-none

  --sort <String>      sort by fields (e.g. field2,#field3,^#field1).  '#' indicates the value is     numeric, and '^' indicates the sorting should be reversed   
```

```lang-none

  --retain <String>      retain only specific fields (comma delimited)   
```

```lang-none

  --remove <String>      remove specific fields (comma delimited)   
```

```lang-none

  --tab       use tabs   
```

```lang-none

  --header       print the header   
```

```lang-none

  --na <String>      use the given string for missing values (Default: NA)   
```

### Author:

  Ryan Cabeen 

<hr/>
## TablePrintInfo 

### Description:

  Print basic information about a table 

### Required Input Arguments:

```lang-none

  --input <Table>      the input table   
```

### Optional Parameter Arguments:

```lang-none

  --na <String>      specify the token for identifying missing values (Default: NA)   
```

```lang-none

  --field <String>      only print values from the given field (specify either the field index or     name)   
```

```lang-none

  --pattern <String>      print a pattern for each line, e.g. '${fieldA} and ${fieldB}   
```

```lang-none

  --indexed       print the row index   
```

### Author:

  Ryan Cabeen 

<hr/>
## TableReliability 

### Description:

  Compute reproducibility statistics from repeated measures 

### Required Input Arguments:

```lang-none

  --input <Table>      input table   
```

### Optional Parameter Arguments:

```lang-none

  --value <String>      the field to summarize (Default: value)   
```

```lang-none

  --id <String>      fields for identifying repeated measures (comma separated)   
```

```lang-none

  --group <String>      fields to group records by (comma separated)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## TableScoreZ 

### Description:

  Compute summary statistics of a field of a table 

### Required Input Arguments:

```lang-none

  --input <Table>      input table   
```

### Optional Parameter Arguments:

```lang-none

  --value <String>      the field to summarize (Default: value)   
```

```lang-none

  --absolute       report the absolute value of the z-score   
```

```lang-none

  --group <String>      fields to group records by (comma separated)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## TableSelect 

### Description:

  Select columns from a table 

### Required Input Arguments:

```lang-none

  --input <Table>      input table   
```

### Optional Parameter Arguments:

```lang-none

  --where <String>      an predicate for selecting records (using existing field names)   
```

```lang-none

  --sort <String>      sort by fields (e.g. field2,#field3,^#field1).  '#' indicates the value is     numeric, and '^' indicates the sorting should be reversed   
```

```lang-none

  --unique <String>      select a unique set from the given fields (comma delimited)   
```

```lang-none

  --retain <String>      include only specific fields (comma delimited regular expressions)   
```

```lang-none

  --remove <String>      exclude specific fields (comma delimited regular expressions)   
```

```lang-none

  --rename <String>      an renaming of fields (e.g. newfield=oldfield)   
```

```lang-none

  --dempty       delete fields with an empty name   
```

```lang-none

  --cat <String>      concatenate existing fields into a new one, e.g.     newfield=%{fieldA}_%{fieldB}   
```

```lang-none

  --constant <String>      add a constant field (e.g. newfield=value,newfield2=value2)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## TableSplitAlong 

### Description:

  Split the name field from along tract analysis 

### Required Input Arguments:

```lang-none

  --input <Table>      input table   
```

### Optional Parameter Arguments:

```lang-none

  --name <String>      the field to split (Default: name)   
```

```lang-none

  --bundle <String>      the name of the new bundle field (Default: bundle)   
```

```lang-none

  --index <String>      the name of the new index field (Default: index)   
```

```lang-none

  --delimiter <String>      the delimiter (Default: _)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## TableStats 

### Description:

  Compute summary statistics of a field of a table 

### Required Input Arguments:

```lang-none

  --input <Table>      input table   
```

### Optional Parameter Arguments:

```lang-none

  --value <String>      the field to summarize (Default: value)   
```

```lang-none

  --group <String>      fields to group records by (comma separated)   
```

```lang-none

  --pattern <String>      a pattern for saving statistics (contains %s) (Default: %s)   
```

```lang-none

  --which <String>      which statistics to include (mean, min, max, sum, var, std, num, cv)     (Default: mean,std)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## TableSubjectMatch 

### Description:

  Match subjects into pairs.  You should provide a field that stores a binary 
  variable on which to split.  The smaller group will be paired with subjects 
  from the bigger group, and optionally, the pairing can be optimized to 
  maximize similarity between paired individuals based on a number of scalar 
  and discrete variables 

### Required Input Arguments:

```lang-none

  --input <Table>      input table   
```

### Optional Parameter Arguments:

```lang-none

  --subject <String>      the field specifying the subject identifier (Default: subject)   
```

```lang-none

  --split <String>      the field for splitting the group (should be binary) (Default: subject)   
```

```lang-none

  --na <String>      a token for specifying missing data (Default: NA)   
```

```lang-none

  --distance <String>      the field name for distance in the output (Default: distance)   
```

```lang-none

  --scalar <String>      scalar fields to include when computing subject distances (comma separated     list) (Default: )   
```

```lang-none

  --discrete <String>      discrete fields to include when computing subject distances (comma     separated list) (Default: )   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## TableSynth 

### Description:

  Synthesize data based on a reference dataset 

### Required Input Arguments:

```lang-none

  --reference <Table>      input reference table   
```

```lang-none

  --sample <Table>      input sample table   
```

### Optional Parameter Arguments:

```lang-none

  --group <String>      an optional list of variables (comma-separated) of variables for grouping   
```

```lang-none

  --scalar <String>      specify the scalar-valued fields (floating point)   
```

```lang-none

  --discrete <String>      specify the discrete-valued fields (integers)   
```

```lang-none

  --factor <String>      specify the categorical-valued fields (factors)   
```

```lang-none

  --missing       synthesize missing data   
```

```lang-none

  --na <String>      specify a missing value token (Default: NA)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output synthesized table   
```

### Author:

  Ryan Cabeen 

<hr/>
## TableVolume 

### Description:

  Map tabular data to a volume 

### Required Input Arguments:

```lang-none

  --input <Table>      input table   
```

```lang-none

  --reference <Mask>      a reference mask   
```

### Optional Parameter Arguments:

```lang-none

  --vector       read vector values   
```

```lang-none

  --voxel <String>      index (Default: index)   
```

```lang-none

  --value <String>      value (Default: value)   
```

```lang-none

  --na <double>      a value to substitute for NAs (Default: 0.0)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## TableWiden 

### Description:

  Widen a table to expand a single field to many 

### Required Input Arguments:

```lang-none

  --input <Table>      input table   
```

### Optional Parameter Arguments:

```lang-none

  --name <String>      the field name to expand (Default: name)   
```

```lang-none

  --value <String>      the field value to expand (Default: value)   
```

```lang-none

  --pattern <String>      pattern for joining names (Default: %s.%s)   
```

```lang-none

  --na <String>      the value used for missing entries (Default: NA)   
```

```lang-none

  --include <String>      include fields   
```

```lang-none

  --exclude <String>      exclude fields   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## TablesCat 

### Description:

  concatentate rows of a collection of tables 

### Required Input Arguments:

```lang-none

  --input <Table(s)>      the input tables   
```

### Optional Parameter Arguments:

```lang-none

  --outer       include all possible fields   
```

### Required Output Arguments:

```lang-none

  --output <Table>      specify the output tables   
```

### Author:

  Ryan Cabeen 

<hr/>
## TablesMerge 

### Description:

  merge the fields of a collection of tables 

### Required Input Arguments:

```lang-none

  --input <Table(s)>      the input tables   
```

### Optional Parameter Arguments:

```lang-none

  --field <String>      the common field to merge on (Default: name)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      specify the output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## VectsAlignAxis 

### Description:

  Compute an affine transform to align a set of points to the z-axis 

### Required Input Arguments:

```lang-none

  --input <Vects>      input vects (should be roughly linear   
```

### Required Output Arguments:

```lang-none

  --output <Affine>      output affine transform   
```

### Author:

  Ryan Cabeen 

<hr/>
## VectsBox 

### Description:

  Get the bounding box of vects 

### Required Input Arguments:

```lang-none

  --input <Vects>      input vects   
```

### Required Output Arguments:

```lang-none

  --output <Solids>      output bounding box   
```

### Author:

  Ryan Cabeen 

<hr/>
## VectsCat 

### Description:

  Concatenate two sets of vectors into one 

### Required Input Arguments:

```lang-none

  --input <Vects>      input vects   
```

```lang-none

  --cat <Vects>      vects to concatenate   
```

### Optional Parameter Arguments:

```lang-none

  --dims       concatenate dimensions (instead of vects)   
```

### Required Output Arguments:

```lang-none

  --output <Vects>      output concatenated vects   
```

### Author:

  Ryan Cabeen 

<hr/>
## VectsClusterKMeans 

### Description:

  Cluster vects using k-means or DP-means clustering 

### Required Input Arguments:

```lang-none

  --input <Vects>      the input vects   
```

### Optional Parameter Arguments:

```lang-none

  --k <Integer>      none (Default: 3)   
```

```lang-none

  --lambda <Double>      none   
```

```lang-none

  --maxiter <Integer>      none (Default: 300)   
```

### Optional Output Arguments:

```lang-none

  --centers <Vects>      the output cluster centeres   
```

```lang-none

  --output <Vects>      the output clustered vects (last coordinate stores label)   
```

### Author:

  Ryan Cabeen 

<hr/>
## VectsConvert 

### Description:

  Convert vects between file formats 

### Required Input Arguments:

```lang-none

  --input <Vects>      input vects   
```

### Required Output Arguments:

```lang-none

  --output <Vects>      output vects   
```

### Author:

  Ryan Cabeen 

<hr/>
## VectsCreate 

### Description:

  Create vects 

### Required Output Arguments:

```lang-none

  --output <Vects>      output vects   
```

### Author:

  Ryan Cabeen 

<hr/>
## VectsCreateSphere 

### Description:

  Create vects that lie on a sphere by subdividing an icosahedron 

### Optional Parameter Arguments:

```lang-none

  --subdiv <int>      the number of subdivisions of the sphere (Default: 3)   
```

```lang-none

  --smooth <Integer>      the number of smoothing iterations   
```

```lang-none

  --points <Integer>      reduce the point count   
```

### Required Output Arguments:

```lang-none

  --output <Vects>      output vects   
```

### Author:

  Ryan Cabeen 

<hr/>
## VectsCreateSphereLookup 

### Description:

  Create a set of uniformly distributed spherical points from a lookup table 

### Required Output Arguments:

```lang-none

  --output <Vects>      output vects   
```

### Author:

  Ryan Cabeen 

<hr/>
## VectsDistances 

### Description:

  Compute the pairwise distance matrix of a set of vects 

### Required Input Arguments:

```lang-none

  --input <Vects>      input vects   
```

### Required Output Arguments:

```lang-none

  --output <Matrix>      output distance matrix   
```

### Author:

  Ryan Cabeen 

<hr/>
## VectsFuse 

### Description:

  fuse vects 

### Required Input Arguments:

```lang-none

  --input <Vect(s)>      the input vectss   
```

### Optional Parameter Arguments:

```lang-none

  --pattern <String(s)>      specify a list of names that will be substituted with input %s   
```

```lang-none

  --bg <Double>      specify an additional background value for softmax or sumnorm   
```

```lang-none

  --gain <Double>      specify a gain for softmax   
```

```lang-none

  --offset <Double>      specify a offset for softmax   
```

```lang-none

  --skip       skip missing files   
```

### Optional Output Arguments:

```lang-none

  --output-min <Vect>      specify the output min vects   
```

```lang-none

  --output-max <Vect>      specify the output max vects   
```

```lang-none

  --output-sum <Vect>      specify the output sum vects   
```

```lang-none

  --output-mean <Vect>      specify the output mean vects   
```

```lang-none

  --output-median <Vect>      specify the output median vects   
```

```lang-none

  --output-var <Vect>      specify the output var vects   
```

```lang-none

  --output-std <Vect>      specify the output std vects   
```

```lang-none

  --output-cv <Vect>      specify the output cv vects   
```

### Author:

  Ryan Cabeen 

<hr/>
## VectsHistogram 

### Description:

  Compute a histogram of a vects 

### Required Input Arguments:

```lang-none

  --input <Vects>      the input vects   
```

### Optional Parameter Arguments:

```lang-none

  --channel <int>      the channel to use (Default: 0)   
```

```lang-none

  --bins <int>      the number of bins (Default: 100)   
```

```lang-none

  --lower <Bound>      the method for computing the histogram lower bound (Options: Extrema,     Quartile, HalfStd, OneStd, TwoStd, ThreeStd, User) (Default: Extrema)   
```

```lang-none

  --upper <Bound>      the method for computing the histogram upper bound (Options: Extrema,     Quartile, HalfStd, OneStd, TwoStd, ThreeStd, User) (Default: Extrema)   
```

```lang-none

  --mylower <Double>      specify a specific lower bound for user defined mode   
```

```lang-none

  --myupper <Double>      specify a specific upper bound for user defined mode   
```

```lang-none

  --normalize       normalize the counts by the total count   
```

```lang-none

  --cdf       compute the cdf   
```

```lang-none

  --smooth <Double>      apply Gaussian smoothing (see hdata)   
```

```lang-none

  --hdata       interpret the smoothing parameter relative to the data intensities (default     is bins)   
```

```lang-none

  --print       print a the histogram to standard output   
```

### Author:

  Ryan Cabeen 

<hr/>
## VectsHull 

### Description:

  Compute the convex hull of vects 

### Required Input Arguments:

```lang-none

  --input <Vects>      input vects   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      output convex hull   
```

### Author:

  Ryan Cabeen 

### Citation:

  Barber, C. B., Dobkin, D. P., & Huhdanpaa, H. (1996). The quickhull algorithm 
  for convex hulls. ACM Transactions on Mathematical Software (TOMS), 22(4), 
  469-483. 

<hr/>
## VectsJitter 

### Description:

  Jitter vects with isotropic Gaussian random displacements 

### Required Input Arguments:

```lang-none

  --input <Vects>      input vects   
```

### Optional Parameter Arguments:

```lang-none

  --multiplier <int>      the number of multiples to include (Default: 1)   
```

```lang-none

  --std <double>      the number of samples to take (Default: 1.0)   
```

### Required Output Arguments:

```lang-none

  --output <Vects>      output subsampled vects   
```

### Author:

  Ryan Cabeen 

<hr/>
## VectsMath 

### Description:

  Evaluate an expression at each vector 

### Required Input Arguments:

```lang-none

  --input <Vects>      the input vects   
```

### Optional Parameter Arguments:

```lang-none

  --expression <String>      the expression to evaluate (Default: mean(v))   
```

### Required Output Arguments:

```lang-none

  --output <Vects>      output vects   
```

### Author:

  Ryan Cabeen 

<hr/>
## VectsMeasureRegion 

### Description:

  Measure regional of a set of labeled vectors 

### Required Input Arguments:

```lang-none

  --input <Vects>      input vects   
```

### Optional Input Arguments:

```lang-none

  --lookup <Table>      input lookup table   
```

```lang-none

  --labels <Vects>      add the following per-vertex labeling to use for computing region     statistics   
```

### Required Parameter Arguments:

```lang-none

  --output <String>      output directory   
```

### Optional Parameter Arguments:

```lang-none

  --name <String>      the name of the input data (Default: name)   
```

```lang-none

  --lutname <String>      the lookup name field (Default: name)   
```

```lang-none

  --lutindex <String>      the lookup index field (Default: index)   
```

```lang-none

  --outname <String>      the output name field (Default: name)   
```

```lang-none

  --outvalue <String>      the output value field (Default: value)   
```

### Author:

  Ryan Cabeen 

<hr/>
## VectsModeMeanShift 

### Description:

  Compute the positions of modes of vectors using the mean shift algorithm 

### Required Input Arguments:

```lang-none

  --input <Vects>      input vects   
```

### Optional Parameter Arguments:

```lang-none

  --bandwidth <Double>      the spatial bandwidth (Default: 1.0)   
```

```lang-none

  --minshift <Double>      the error threshold for convergence (Default: 1.0E-6)   
```

```lang-none

  --maxiter <Integer>      the maximum number of iterations (Default: 10000)   
```

### Required Output Arguments:

```lang-none

  --masses <Vects>      output masses   
```

```lang-none

  --modes <Vects>      output modes   
```

### Author:

  Ryan Cabeen 

### Citation:

  Comaniciu, D., & Meer, P. (2002). Mean shift: A robust approach toward 
  feature space analysis. IEEE Transactions on pattern analysis and machine 
  intelligence, 24(5), 603-619. 

<hr/>
## VectsPCA 

### Description:

  Perform principal component analysis on vects 

### Required Input Arguments:

```lang-none

  --input <Vects>      the input vects   
```

### Optional Parameter Arguments:

```lang-none

  --top <Integer>      none   
```

### Optional Output Arguments:

```lang-none

  --mean <Vects>      the output mean vect   
```

```lang-none

  --comps <Vects>      the output principal components   
```

```lang-none

  --vals <Vects>      the output principal values   
```

```lang-none

  --output <Vects>      the output transformed vects   
```

### Author:

  Ryan Cabeen 

<hr/>
## VectsPrintInfo 

### Description:

  Print basic information about vectors 

### Required Input Arguments:

```lang-none

  --input <Vects>      the input vects   
```

### Optional Parameter Arguments:

```lang-none

  --stats       print statistics   
```

```lang-none

  --norm       print statistics of vect norms   
```

### Author:

  Ryan Cabeen 

<hr/>
## VectsReduce 

### Description:

  Reduce the number of vectors in either a random or systematic way 

### Required Input Arguments:

```lang-none

  --input <Vects>      input vects   
```

### Optional Parameter Arguments:

```lang-none

  --num <Integer>      the maxima number of samples   
```

```lang-none

  --fraction <Double>      the fraction of vects to remove (zero to one)   
```

```lang-none

  --which <String>      the list of indices to select   
```

```lang-none

  --exclude <String>      the list of indices to exclude   
```

### Required Output Arguments:

```lang-none

  --output <Vects>      output subsampled vects   
```

### Author:

  Ryan Cabeen 

<hr/>
## VectsRegisterLinear 

### Description:

  Estimate a linear transform to register a given pair of vects.  You can 
  choose one of several methods to specify the degrees of freedom of the 
  transform and how the transform parameters are estimated 

### Required Input Arguments:

```lang-none

  --source <Vects>      input source vects   
```

```lang-none

  --dest <Vects>      input dest vects (should match source)   
```

### Optional Input Arguments:

```lang-none

  --weights <Vects>      input weights (only available for some methods)   
```

### Optional Parameter Arguments:

```lang-none

  --method <VectsRegisterLinearMethod>      the registration method (each has different degrees of freedom and     estimation techniques) (Options: AffineLeastSquares, RigidDualQuaternion)     (Default: AffineLeastSquares)   
```

### Required Output Arguments:

```lang-none

  --output <Affine>      output affine transform mapping source to dest   
```

### Optional Output Arguments:

```lang-none

  --transformed <Vects>      output transformed source vects (redundant, but useful for validation)   
```

### Author:

  Ryan Cabeen, Yonggang Shi 

<hr/>
## VectsRegisterRigidDualQuaternion 

### Description:

  Estimate a rigid transform using the dual quaterionion method.  This assumes 
  point-to-point correspondence between vects. 

### Required Input Arguments:

```lang-none

  --source <Vects>      input source vects   
```

```lang-none

  --dest <Vects>      input dest vects (should match source)   
```

### Optional Input Arguments:

```lang-none

  --weights <Vects>      input weights   
```

### Required Output Arguments:

```lang-none

  --output <Affine>      output affine transform mapping source to dest   
```

### Optional Output Arguments:

```lang-none

  --transformed <Vects>      output transformed source vects (redundant, but useful for validation)   
```

### Author:

  Ryan Cabeen, Yonggang Shi 

### Citation:

  Walker, Michael W., Lejun Shao, and Richard A. Volz. "Estimating 3-D location 
  parameters using dual number quaternions." CVGIP: image understanding 54.3 
  (1991): 358-367. 

<hr/>
## VectsSample 

### Description:

  Sample a volume at a set of points 

### Required Input Arguments:

```lang-none

  --input <Vects>      input 3D coordinates   
```

```lang-none

  --volume <Volume>      input volume   
```

### Optional Parameter Arguments:

```lang-none

  --interp <InterpolationType>      interpolation method (Options: Nearest, Trilinear, Tricubic, Gaussian,     GaussianLocalLinear, GaussianLocalQuadratic) (Default: Trilinear)   
```

### Required Output Arguments:

```lang-none

  --output <Vects>      the output samples   
```

### Author:

  Ryan Cabeen 

<hr/>
## VectsSelect 

### Description:

  Select a which of vects 

### Required Input Arguments:

```lang-none

  --input <Vects>      input vects   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

```lang-none

  --solids <Solids>      input solids   
```

### Optional Parameter Arguments:

```lang-none

  --or       use OR (instead of AND) to combine selections   
```

```lang-none

  --invert       invert the selection after combining   
```

### Required Output Arguments:

```lang-none

  --output <Vects>      output selected vects   
```

### Author:

  Ryan Cabeen 

<hr/>
## VectsSpheres 

### Description:

  Create spheres from vects 

### Required Input Arguments:

```lang-none

  --input <Vects>      input vects   
```

### Optional Parameter Arguments:

```lang-none

  --radius <double>      the sphere radius (Default: 1.0)   
```

### Required Output Arguments:

```lang-none

  --output <Solids>      output solid spheres   
```

### Author:

  Ryan Cabeen 

<hr/>
## VectsTransform 

### Description:

  Transform vects.  (note: the order of operations is transpose, flip, swap, 
  perm, affine) 

### Required Input Arguments:

```lang-none

  --input <Vects>      input vects   
```

### Optional Input Arguments:

```lang-none

  --affine <Affine>      apply an affine xfm   
```

```lang-none

  --invaffine <Affine>      apply an inverse affine xfm   
```

```lang-none

  --deform <Deformation>      apply a deformation xfm   
```

### Optional Parameter Arguments:

```lang-none

  --reverse       reverse the order, i.e. compose the affine(deform(x)), whereas the default     is deform(affine(x))   
```

```lang-none

  --rows       force rows > cols   
```

```lang-none

  --cols       force cols > rows   
```

```lang-none

  --transpose       transpose   
```

```lang-none

  --subset <String>      which the coordinates   
```

```lang-none

  --flip <String>      flip a coodinate (x, y, or z)   
```

```lang-none

  --swap <String>      swap a pair of coordinates (xy, xz, or yz)   
```

```lang-none

  --perm <String>      permute by coordinate index, e.g. 1,0,2   
```

```lang-none

  --negate       negative the vectors   
```

```lang-none

  --normalize       normalize   
```

### Required Output Arguments:

```lang-none

  --output <Vects>      output transformed vects   
```

### Author:

  Ryan Cabeen 

<hr/>
<hr/>
## VolumeAxialFuse 

### Description:

  fuse volumes 

### Required Input Arguments:

```lang-none

  --input <Volume(s)>      the input volumes   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      specify a mask   
```

### Optional Parameter Arguments:

```lang-none

  --spherical       use input from spherical coordinates   
```

### Optional Output Arguments:

```lang-none

  --output-mean <Volume>      specify the output mean volume   
```

```lang-none

  --output-lambda <Volume>      specify the output lambda volume   
```

```lang-none

  --output-coherence <Volume>      specify the output coherence volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeBiHistogram 

### Description:

  Compute a histogram of a volume 

### Required Input Arguments:

```lang-none

  --x <Volume>      the volume for the x-dimension   
```

```lang-none

  --y <Volume>      the volume for the y-dimension   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      an input mask   
```

### Optional Parameter Arguments:

```lang-none

  --xbins <int>      the number of bins in x (Default: 100)   
```

```lang-none

  --ybins <int>      the number of bins in y (Default: 100)   
```

```lang-none

  --smooth <Double>      apply smoothing by the given amount   
```

```lang-none

  --exclude       exclude counts outside the histogram range   
```

```lang-none

  --xmin <Double>      the minimum in x   
```

```lang-none

  --ymin <Double>      the minimum in y   
```

```lang-none

  --xmax <Double>      the maximum in x   
```

```lang-none

  --ymax <Double>      the maximum in y   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Optional Output Arguments:

```lang-none

  --breaks <Volume>      output breaks   
```

```lang-none

  --mapping <Mask>      output mapping   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeBiTensorFit 

### Description:

  Fit a bi-tensor volume to a diffusion-weighted MRI. 

### Required Input Arguments:

```lang-none

  --input <Volume>      input diffusion-weighted MR volume   
```

```lang-none

  --gradients <Gradients>      the gradients   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      the mask   
```

### Optional Parameter Arguments:

```lang-none

  --method <BiTensorFitType>      specify an estimation method (Options: DTI, DTIFWE, Isotropic,     FixedIsotropic, BothIsotropic, AlignedZeppelin, Zeppelin, Anisotropic)     (Default: Isotropic)   
```

```lang-none

  --cost <CostType>      specify a cost function for non-linear fitting (Options: SE, MSE, RMSE,     NRMSE, CHISQ, RLL) (Default: SE)   
```

```lang-none

  --shells <String>      specify a subset of gradient shells to include (comma-separated list of     b-values)   
```

```lang-none

  --which <String>      specify a subset of gradients to include (comma-separated list of indices     starting from zero)   
```

```lang-none

  --exclude <String>      specify a subset of gradients to exclude (comma-separated list of indices     starting from zero)   
```

```lang-none

  --threads <int>      the number of threads to use (Default: 1)   
```

### Advanced Parameter Arguments:

```lang-none

  --shells <String>      specify a subset of gradient shells to include (comma-separated list of     b-values)   
```

```lang-none

  --which <String>      specify a subset of gradients to include (comma-separated list of indices     starting from zero)   
```

```lang-none

  --exclude <String>      specify a subset of gradients to exclude (comma-separated list of indices     starting from zero)   
```

```lang-none

  --threads <int>      the number of threads to use (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output bitensor volume (name output like *.bti and an directory of volumes     will be created)   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeBiTensorReduce 

### Description:

  Reduce a bitensor volume to a single tensor volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      input bitensor volume   
```

### Optional Parameter Arguments:

```lang-none

  --fluid       return the fluid compartment (default is the tissue compartment)   
```

```lang-none

  --threads <int>      the number of threads to use (Default: 1)   
```

### Advanced Parameter Arguments:

```lang-none

  --threads <int>      the number of threads to use (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output tensor volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeBiTensorTransform 

### Description:

  Spatially transform a bi-tensor volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input tensor volume   
```

```lang-none

  --reference <Volume>      input reference volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

```lang-none

  --inputMask <Mask>      use an input mask (defined in the input space)   
```

```lang-none

  --affine <Affine>      apply an affine xfm   
```

```lang-none

  --invaffine <Affine>      apply an inverse affine xfm   
```

```lang-none

  --deform <Deformation>      apply a deformation xfm   
```

### Optional Parameter Arguments:

```lang-none

  --reverse       reverse the order, i.e. compose the affine(deform(x)), whereas the default     is deform(affine(x))   
```

```lang-none

  --reorient <ReorientationType>      specify a reorient method (fs or jac) (Options: FiniteStrain, Jacobian)     (Default: Jacobian)   
```

```lang-none

  --interp <KernelInterpolationType>      the interpolation type (Options: Nearest, Trilinear, Gaussian) (Default:     Trilinear)   
```

```lang-none

  --support <Integer>      the filter radius in voxels (Default: 3)   
```

```lang-none

  --hpos <Double>      the positional bandwidth in mm (Default: 1.0)   
```

```lang-none

  --log       use log estimation   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output transformed tensor volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeBlockRead 

### Description:

  compose blocks into a volume 

### Required Input Arguments:

```lang-none

  --input <Pattern>      the input pattern (e.g. volume%d.nii.gz   
```

```lang-none

  --ref <Volume>      the reference volume   
```

### Optional Parameter Arguments:

```lang-none

  --i <Integer>      the i block size   
```

```lang-none

  --j <Integer>      the j block size   
```

```lang-none

  --k <Integer>      the k block size (Default: 1)   
```

```lang-none

  --skip       skip files if they are missing   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      specify the output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeBlockWrite 

### Description:

  decompose a volume into blocks 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input volume   
```

### Optional Parameter Arguments:

```lang-none

  --i <Integer>      the i block size   
```

```lang-none

  --j <Integer>      the j block size   
```

```lang-none

  --k <Integer>      the k block size (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Pattern>      specify the output pattern (e.g. output%d.nii.gz)   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeBox 

### Description:

  Compute the bounding box of a volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Required Output Arguments:

```lang-none

  --output <Solids>      output bounding box   
```

### Author:

  Ryan Cabeen 

<hr/>
<hr/>
## VolumeCat 

### Description:

  Concatenate two volumes into one 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

```lang-none

  --cat <Volume>      volume to concatenate   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output concatenated volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeCenter 

### Description:

  Voxel sizes and position of a volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      input Volume   
```

### Required Parameter Arguments:

```lang-none

  --factor <Double>      input voxel dimension scaling factor   
```

### Optional Output Arguments:

```lang-none

  --output <Volume>      output Volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeConform 

### Description:

  Process a volume to conform to a given set of constraints 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input volume   
```

### Optional Parameter Arguments:

```lang-none

  --maxdi <double>      the voxel size in i (Default: 1.0)   
```

```lang-none

  --maxdj <double>      the voxel size in j (Default: 1.0)   
```

```lang-none

  --maxdk <double>      the voxel size in k (Default: 1.0)   
```

```lang-none

  --interp <InterpolationType>      image interpolation method (Options: Nearest, Trilinear, Tricubic,     Gaussian, GaussianLocalLinear, GaussianLocalQuadratic) (Default: Trilinear)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeConvert 

### Description:

  Convert a volume to a different format (only useful on command line) 

### Required Input Arguments:

```lang-none

  --input <Volume>      input   
```

### Optional Parameter Arguments:

```lang-none

  --proto       only copy the structure, not the intensities   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeCrop 

### Description:

  Crop a volume down to a smaller volume (only one criteria per run) 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

```lang-none

  --solids <Solids>      some soids   
```

### Optional Parameter Arguments:

```lang-none

  --range <String>      a range specification, e.g. start:end,start:end,start:end   
```

```lang-none

  --thresh <Double>      crop out the background with a given threshold   
```

```lang-none

  --invert       invert the selection   
```

```lang-none

  --even       ensure the volume has even dimensions   
```

```lang-none

  --pad <int>      a padding size in voxels (Default: 0)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeDeformationCompose 

### Description:

  Compose a deformation field with an affine transform 

### Required Input Arguments:

```lang-none

  --affine <Affine>      an affine transform   
```

### Optional Input Arguments:

```lang-none

  --deform <Volume>      an deform deformation field   
```

```lang-none

  --reference <Volume>      a reference image   
```

### Optional Parameter Arguments:

```lang-none

  --invert       invert the affine transform   
```

```lang-none

  --reverse       reverse the order of the transforms   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output   
```

### Author:

  Ryan Cabeen 

<hr/>
<hr/>
## VolumeDiceBatch 

### Description:

  Compute the weighted dice coefficient between a set of density maps.  
  Citation: Cousineau et al. NeuroImage: Clinical 2017 

### Required Input Arguments:

```lang-none

  --left <FilePattern>      specify the first set of segmentations   
```

```lang-none

  --right <FilePattern>      specify the second set of segmentations   
```

### Required Parameter Arguments:

```lang-none

  --names <Spec>      specify bundle identifiers (e.g. a file that lists the bundle names)   
```

### Required Output Arguments:

```lang-none

  --output <File>      specify an output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeDifferenceMap 

### Description:

  Summarize the difference between two image volumes 

### Required Input Arguments:

```lang-none

  --left <Volume>      input left volume   
```

```lang-none

  --right <Volume>      input right volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --prefix <String>      a prefix for the output names (Default: )   
```

### Required Output Arguments:

```lang-none

  --output <Table>      the table   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeDownsample 

### Description:

  Downsample a volume.  Unlike VolumeZoom, this includes a prefiltering step 
  with a triangle filter to reduce alias artifact 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input volume   
```

### Optional Parameter Arguments:

```lang-none

  --factor <double>      an downsampling scaling factor (Default: 2.0)   
```

```lang-none

  --threads <Integer>      the number of threads in the pool (Default: 3)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeDwiAdc 

### Description:

  Compute the apparent mri coefficient (ADC) of the mri signal.  The default 
  output is the mean ADC, but per-gradient outputs can also be computed 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input diffusion-weighted MR volume   
```

```lang-none

  --gradients <Gradients>      the input gradients   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      the input mask   
```

### Optional Parameter Arguments:

```lang-none

  --component       compute the ADC for each gradient, otherwise the average will be computed   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output ADC volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeDwiAverage 

### Description:

  Average subvolumes from diffusion MRI.  This is often used to improve the SNR 
  when the scan includes repeats 

### Required Input Arguments:

```lang-none

  --dwi <Volume>      the input diffusion-weighted MR volume   
```

```lang-none

  --gradients <Gradients>      the input gradients (must match input DWI)   
```

### Optional Parameter Arguments:

```lang-none

  --average <String>      a file or comma-separated list of integers specifying how the data should     be grouped, e.g. 1,1,1,2,2,2,3,3,3,...   
```

### Required Output Arguments:

```lang-none

  --outputDwi <Volume>      the output dwi volume   
```

### Optional Output Arguments:

```lang-none

  --outputGradients <Gradients>      the output gradients   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeDwiBaseline 

### Description:

  Extract baseline signal statistics from a diffusion-weighted MR volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input diffusion-weighted MR volume   
```

```lang-none

  --gradients <Gradients>      the input gradients   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      the input mask   
```

### Optional Output Arguments:

```lang-none

  --mean <Volume>      the output mean baseline signal   
```

```lang-none

  --median <Volume>      the output median baseline signal   
```

```lang-none

  --var <Volume>      the output variance of baseline signal (only meaningful if there are     multiple baselines)   
```

```lang-none

  --std <Volume>      the output standard deviation of baseline signal (only meaningful if there     are multiple baselines)   
```

```lang-none

  --cat <Volume>      the output complete set of baseline signals   
```

```lang-none

  --drift <Volume>      the output signal drift estimates (slope, stderr, tstat, pval, sig)     measured by percentage change   
```

### Author:

  Ryan Cabeen 

### Citation:

  Vos, Sjoerd B., et al. "The importance of correcting for signal drift in 
  diffusion MRI." Magnetic resonance in medicine 77.1 (2017): 285-299. 

<hr/>
<hr/>
## VolumeDwiCorrectDrift 

### Description:

  Correct for signal drift in a diffusion-weighted MR volume using a polynomial 
  model 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input diffusion-weighted MR volume   
```

```lang-none

  --gradients <Gradients>      the input gradients   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      the input mask   
```

### Optional Parameter Arguments:

```lang-none

  --global       use global drift estimation   
```

```lang-none

  --local       use local drift estimation   
```

```lang-none

  --iso       use isotropic drift estimation   
```

```lang-none

  --blend       use blended drift estimation   
```

```lang-none

  --order <int>      the order of the polynomial model (Default: 2)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output corrected dwi   
```

### Author:

  Ryan Cabeen 

### Citation:

  Vos, Sjoerd B., et al. "The importance of correcting for signal drift in 
  diffusion MRI." Magnetic resonance in medicine 77.1 (2017): 285-299. 

<hr/>
## VolumeDwiEstimateNoise 

### Description:

  Estimate the noise parameter from the baselines of a DWI 

### Required Input Arguments:

```lang-none

  --input <Volume>      input dwi   
```

```lang-none

  --gradients <Gradients>      input gradients   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output error volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeDwiFeature 

### Description:

  Compute features of the the diffusion weighted signal.  These features are 
  statistical summaries of the signal within and across shells (depending on 
  the method), mostly avoiding any modeling assumptions 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input diffusion-weighted MR volume   
```

```lang-none

  --gradients <Gradients>      the input gradients   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      the input mask   
```

### Optional Parameter Arguments:

```lang-none

  --feature <VolumeDwiFeatureType>      the method for computing anisotropy (Options: Attenuation, AnisoCoeffVar,     AnisoStd, AnisoMu, SphericalMean, SphericalMeanNorm, SphericalMeanADC,     SphericalStd, NoiseStd, NoiseCoeffVar) (Default: AnisoCoeffVar)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output anisotropy volume   
```

### Author:

  Ryan Cabeen 

<hr/>
<hr/>
## VolumeDwiNormalize 

### Description:

  normalize a mri weighted image volume by the baseline signal 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input diffusion-weighted MR volume   
```

```lang-none

  --gradients <Gradients>      the input gradients   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      the input mask   
```

### Optional Parameter Arguments:

```lang-none

  --grouping <String>      a grouping of scans with similar TE/TR (otherwise all scans are assumed to     be part of one group)   
```

```lang-none

  --unit       normalize the signal to the unit interval (divide by average baseline by     voxel)   
```

```lang-none

  --mean <Double>      normalize the signal to have a given mean value across the entire volume   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output baseline volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeDwiReduce 

### Description:

  Reduce the number of mri samples in a mri weighted image 

### Required Input Arguments:

```lang-none

  --dwi <Volume>      the input diffusion-weighted MR volume   
```

```lang-none

  --gradients <Gradients>      the input gradients (must match input DWI)   
```

### Optional Parameter Arguments:

```lang-none

  --shells <String>      include only specific shells   
```

```lang-none

  --which <String>      include only specific gradients (comma separated zero-based indices)   
```

```lang-none

  --exclude <String>      exclude specific gradients (comma separated zero-based indices)   
```

```lang-none

  --bscale <Double>      scale the baseline signal by the given amount (this is very rarely     necessary)   
```

### Required Output Arguments:

```lang-none

  --outputDwi <Volume>      the output dwi volume   
```

### Optional Output Arguments:

```lang-none

  --outputGradients <Gradients>      the output gradients   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeDwiResampleGP 

### Description:

  Resample a diffusion-weighted MR volume to have a different set of gradients 
  (arbitrary b-vectors and b-values) 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input diffusion-weighted MR volume   
```

```lang-none

  --gradients <Gradients>      the input gradients (must match input DWI)   
```

```lang-none

  --reference <Gradients>      the gradients used for resampling   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --lambda <Double>      the lambda parameter (controls smoothness) (Default: 0.1)   
```

```lang-none

  --alpha <Double>      the alpha parameter (controls gradient direction scaling) (Default: 1.0)   
```

```lang-none

  --beta <Double>      the beta parameter (controls gradient strength scaling) (Default: 1.0)   
```

```lang-none

  --threads <Integer>      the number of threads to use (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output resampled diffusion-weighted MR volume (will match reference     gradients)   
```

### Author:

  Ryan Cabeen 

### Citation:

  Andersson, J. L., & Sotiropoulos, S. N. (2015). Non-parametric representation 
  and prediction of single-and multi-shell diffusion-weighted MRI data using 
  Gaussian processes. NeuroImage, 122, 166-176. 

<hr/>
## VolumeDwiResampleOutlierGP 

### Description:

  Detect and replace outliers of a diffusion-weighted MR volume using Gaussian 
  Process regression.  This first detects outliers using a very smooth GP model 
  and then replaces only those outliers using a more rigid GP model 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input diffusion-weighted MR volume   
```

```lang-none

  --gradients <Gradients>      the input gradients (must match input DWI)   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --outlier <Double>      the threshold for outlier detection. this is the fractional change in     single value from the GP prediction to qualify as an outlier (Default:     0.35)   
```

```lang-none

  --include <Double>      the minimum fraction of gradient directions to use for outlier replacement.      if more than this fraction are considered inliers, then interpolation will     be used (Default: 0.75)   
```

```lang-none

  --lambdaDetect <Double>      the lambda parameter for detecting outliers (controls smoothness) (Default:     1.0)   
```

```lang-none

  --lambdaPredict <Double>      the lambda parameter for replacing outliers (controls smoothness) (Default:     1.0)   
```

```lang-none

  --resample       resample all signal values with reduced GP (default will only resample     outliers)   
```

```lang-none

  --alpha <Double>      the alpha parameter (controls gradient direction scaling) (Default: 1.0)   
```

```lang-none

  --beta <Double>      the beta parameter (controls gradient strength scaling) (Default: 1.0)   
```

```lang-none

  --threads <Integer>      the number of threads to use (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output resampled diffusion-weighted MR volume (will match reference     gradients)   
```

### Author:

  Ryan Cabeen 

### Citation:

  Andersson, J. L., & Sotiropoulos, S. N. (2015). Non-parametric representation 
  and prediction of single-and multi-shell diffusion-weighted MRI data using 
  Gaussian processes. NeuroImage, 122, 166-176. 

<hr/>
## VolumeDwiResampleSpherical 

### Description:

  Resample a diffusion-weighted MR volume to have a different set of gradients 
  (single shell only) 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input diffusion-weighted MR volume   
```

```lang-none

  --source <Gradients>      the (original) source gradients   
```

```lang-none

  --dest <Vects>      the destination gradient directions   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --filter <String>      specify a filtering method (linear, local, or global) (Default: linear)   
```

```lang-none

  --kappa <Double>      the lambda parameter (Default: 5.0)   
```

```lang-none

  --lambda <Double>      the lambda parameter (Default: 0.1)   
```

```lang-none

  --sigma <Double>      the sigma parameter (Default: 20.0)   
```

```lang-none

  --adaptive       use adaptive resampling   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output resampled diffusion-weighted MR volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeDwiSphericalMean 

### Description:

  Compute the spherical mean for each shell. 

### Required Input Arguments:

```lang-none

  --dwi <Volume>      the dwi diffusion-weighted MR volume   
```

```lang-none

  --gradients <Gradients>      the (original) source gradients   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Required Output Arguments:

```lang-none

  --outputDwi <Volume>      the output resampled diffusion-weighted MR volume   
```

```lang-none

  --outputBvals <Vects>      the output b-values   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeDwiSubsample 

### Description:

  Subsample a mri weighted volume by removing channels to maximize the 
  separation of gradient directions.  This is currently optimized for single 
  shell data 

### Required Input Arguments:

```lang-none

  --indwi <Volume>      the input diffusion-weighted MR volume   
```

```lang-none

  --ingrad <Gradients>      the input gradients   
```

### Optional Parameter Arguments:

```lang-none

  --nums <int>      the number of baseline channels to keep (Default: 1)   
```

```lang-none

  --numd <int>      the number of diffusion-weighted channels to keep (Default: 12)   
```

### Required Output Arguments:

```lang-none

  --outdwi <Volume>      the output diffusion-weighted MR volume   
```

```lang-none

  --outgrad <Gradients>      the output gradients   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeEnhanceContrast 

### Description:

  enhance the intensity contrast of a given volume.  The output will have a 
  range of zero to one. 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --type <VolumeEnhanceContrastType>      the type of contrast enhancement (Options: Histogram, Ramp, RampGauss,     Mean, None) (Default: Histogram)   
```

```lang-none

  --squeeze <VolumeEnhanceContrastSqueeze>      the type of squeezing (Options: Square, Root, Sine, SquineLow, SquineHigh,     None) (Default: None)   
```

```lang-none

  --bins <int>      the number of histogram bins (Default: 1024)   
```

```lang-none

  --gauss <double>      the Gaussian bandwidth (Default: 3.0)   
```

```lang-none

  --scale <double>      the intensity scaling (Default: 1.0)   
```

```lang-none

  --smooth <Double>      smooth the histogram by adding the given proportion of the total (reduces     effect of outliers)   
```

```lang-none

  --nobg       remove background voxels   
```

```lang-none

  --invert       invert the intensities on a unit interval   
```

```lang-none

  --thresh <double>      specify a threshold for background removal (Default: 1.0E-6)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output enhanced image   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeEstimateLookup 

### Description:

  Compute a histogram of a volume 

### Required Input Arguments:

```lang-none

  --from <Volume>      the volume for the x-dimension   
```

```lang-none

  --to <Volume>      the volume for the y-dimension   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      an input mask   
```

### Optional Parameter Arguments:

```lang-none

  --bins <int>      the number of bins in x (Default: 100)   
```

```lang-none

  --xmin <Double>      the minimum in x   
```

```lang-none

  --xmax <Double>      the maximum in x   
```

```lang-none

  --inputField <String>      the name field name for the input value (Default: input)   
```

```lang-none

  --outputField <String>      the name field name for the output value (Default: output)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Optional Output Arguments:

```lang-none

  --breaks <Volume>      output breaks   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeExpDecayError 

### Description:

  Synthesize a volume from exp decay parameters 

### Required Input Arguments:

```lang-none

  --input <Volume>      input exp decay sample volume   
```

```lang-none

  --varying <Vects>      the varying parameters   
```

```lang-none

  --model <Volume>      the exponential decay model parameter volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --method <String>      the type of error metric (me, nme, sse, nsse, mse, nmse, rmse, nrmse)     (Default: nrmse)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output error   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeExpDecayFit 

### Description:

  Fit an exponential decay model to volumetric data: y = alpha * exp(-beta * x) 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input dwi   
```

### Optional Input Arguments:

```lang-none

  --varying <Vects>      the varying myparameters values used for fitting, if not provided the start     and step myparameters are used instead   
```

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --method <String>      the method for fitting (lls, wlls, nlls) (Default: wlls)   
```

```lang-none

  --start <Double>      the starting echo time (not used if you provide a varying input) (Default:     1.0)   
```

```lang-none

  --step <Double>      the spacing between echo times (not used if you provide a varying input)     (Default: 1.0)   
```

```lang-none

  --which <String>      specify a subset of data to include (comma-separated list of indices     starting from zero)   
```

```lang-none

  --exclude <String>      specify a subset of data to exclude (comma-separated list of indices     starting from zero)   
```

```lang-none

  --minAlpha <Double>      the minimum alpha (Default: 0.0)   
```

```lang-none

  --minBeta <Double>      the minimum beta (Default: 0.0)   
```

```lang-none

  --thresh <Double>      apply a threshold to the input image, e.g. remove zero values   
```

```lang-none

  --shrinkage       apply shrinkage regularization for low SNR regions   
```

```lang-none

  --shrinkSmooth <Integer>      the amount of smoothing to apply (Default: 5)   
```

```lang-none

  --shrinkSnrMax <Double>      the maximum shrinkage snr (Default: 7.0)   
```

```lang-none

  --shrinkSnrMin <Double>      the minimum shrinkage snr (Default: 5.0)   
```

```lang-none

  --threads <Integer>      the mynumber of threads in the pool (Default: 1)   
```

### Advanced Parameter Arguments:

```lang-none

  --which <String>      specify a subset of data to include (comma-separated list of indices     starting from zero)   
```

```lang-none

  --exclude <String>      specify a subset of data to exclude (comma-separated list of indices     starting from zero)   
```

### Optional Output Arguments:

```lang-none

  --output <Volume>      the output exp decay model volume   
```

```lang-none

  --outputAlpha <Volume>      the output exp decay alpha myparameter   
```

```lang-none

  --outputBeta <Volume>      the output exp decay beta myparameter   
```

```lang-none

  --outputError <Volume>      the output exp decay error   
```

```lang-none

  --outputResiduals <Volume>      the output exp decay residuals   
```

```lang-none

  --outputSnr <Volume>      the output exp decay SNR map   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeExpDecaySynth 

### Description:

  Synthesize a volume from exp decay parameters 

### Required Input Arguments:

```lang-none

  --input <Volume>      input exp decay volume   
```

```lang-none

  --varying <Vects>      the varying parameters   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      predicted output   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeExpRecoveryError 

### Description:

  Synthesize a volume from exp recovery parameters 

### Required Input Arguments:

```lang-none

  --input <Volume>      input exp recovery sample volume   
```

```lang-none

  --varying <Vects>      the varying parameters   
```

```lang-none

  --model <Volume>      the exponential recovery model parameter volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --method <String>      the type of error metric (me, nme, sse, nsse, mse, nmse, rmse, nrmse)     (Default: nrmse)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output error   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeExpRecoveryFit 

### Description:

  Fit an exponential recovery model to volumetric data: y = alpha * exp(-beta * 
  x) 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input dwi   
```

```lang-none

  --varying <Vects>      the varying parameters values used for fitting   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --threads <Integer>      the number of threads in the pool (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output exp recovery model volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeExpRecoverySynth 

### Description:

  Synthesize a volume from exp recovery parameters 

### Required Input Arguments:

```lang-none

  --input <Volume>      input exp recovery volume   
```

```lang-none

  --varying <Vects>      the varying parameters   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      predicted output   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeExtrema 

### Description:

  Compute the local extrema of a volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --channel <int>      the volume channel (Default: 0)   
```

```lang-none

  --minima       find the minima   
```

```lang-none

  --maxima       find the maxima   
```

```lang-none

  --element <String>      specify an element (Default: cross)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeFibersCat 

### Description:

  concatenate fibers volumes 

### Required Input Arguments:

```lang-none

  --input <FibersVolume(s)>      the input fibers volumes   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      specify a mask   
```

### Required Output Arguments:

```lang-none

  --output <FibersVolume>      specify the output fibers volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeFibersError 

### Description:

  Compute error metrics between a ground truth and test fibers volume 

### Required Input Arguments:

```lang-none

  --truth <Volume>      the truth fibers volume   
```

```lang-none

  --test <Volume>      the test fibers volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --thresh <double>      a threshold for compartment existence (Default: 0.05)   
```

### Optional Output Arguments:

```lang-none

  --missing <Volume>      output measuring number of missing compartment error   
```

```lang-none

  --extra <Volume>      output measuring number of extra compartment error   
```

```lang-none

  --linehaus <Volume>      output measuring the hausdorff angular error   
```

```lang-none

  --linetotal <Volume>      output measuring the total angular error   
```

```lang-none

  --frachaus <Volume>      output measuring the hausdorff fraction error   
```

```lang-none

  --fractotal <Volume>      output measuring the total fraction error   
```

```lang-none

  --fraciso <Volume>      output measuring the isotropic fraction error   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeFibersFilter 

### Description:

  Add orientation and volume fraction noise to a fibers volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      input fibers volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --base <Double>      the fraction gain (Default: 1.0)   
```

```lang-none

  --frac <Double>      the fraction gain (Default: 2.0)   
```

```lang-none

  --angle <Double>      the angle gain (Default: 1.0)   
```

```lang-none

  --align <Double>      the align gain (Default: 2.0)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output noisy fibers volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeFibersFit 

### Description:

  Fit a fibers volume using a multi-stage procedure 

### Required Input Arguments:

```lang-none

  --input <Volume>      input diffusion-weighted MR volume   
```

```lang-none

  --gradients <Gradients>      the gradients   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      the mask   
```

### Optional Parameter Arguments:

```lang-none

  --round <Integer>      specify rounding factor for the gradients   
```

```lang-none

  --shells <String>      specify a subset of gradient shells to include (comma-separated list of     b-values)   
```

```lang-none

  --which <String>      specify a subset of gradients to include (comma-separated list of indices     starting from zero)   
```

```lang-none

  --exclude <String>      specify a subset of gradients to exclude (comma-separated list of indices     starting from zero)   
```

```lang-none

  --threads <Integer>      the number of threads in the pool (Default: 1)   
```

```lang-none

  --columns       use fine-grained multi-threading   
```

```lang-none

  --minfrac <double>      the minimum volume fraction (Default: 0.01)   
```

```lang-none

  --comps <int>      the maximum number of fiber compartments to extract (Default: 3)   
```

```lang-none

  --points <Integer>      the number of points used for peak extraction (Default: 256)   
```

```lang-none

  --cluster <double>      the angle for peak clustering (Default: 30.0)   
```

```lang-none

  --peak <PeakMode>      the statistic for aggregating peaks (Options: Sum, Mean, Max) (Default:     Sum)   
```

```lang-none

  --dmax <double>      the maximum diffusivity for MCSMT (Default: 0.003)   
```

```lang-none

  --maxiters <int>      the maximum number of iterations for MCSMT (Default: 1500)   
```

```lang-none

  --dperp <Double>      use the givel fraction of axial diffusivity when fitting fiber volume     fractions   
```

```lang-none

  --dfix <Double>      use the given fixed axial diffusivity when fitting fiber volume fractions     (Default: 0.00125)   
```

```lang-none

  --tort       use a tortuosity model (otherwise the extra-cellular diffusivity is matched     to the intra)   
```

```lang-none

  --restarts <Integer>      use the given fixed axial diffusivity when fitting fiber volume fractions     (Default: 1)   
```

```lang-none

  --nosmt       skip SMT estimation (for multishell data only)   
```

```lang-none

  --modeSingle <Mode>      specify an parameter estimation mode for single shell analysis (Options:     Fracs, FracsFixedSum, FracsDiff, FracSum, FracSumDiff, Diff,     FracSumDiffMultistage, FracsDiffMultistage) (Default:     FracSumDiffMultistage)   
```

```lang-none

  --modeMulti <Mode>      specify an parameter estimation mode for single shell analysis (Options:     Fracs, FracsFixedSum, FracsDiff, FracSum, FracSumDiff, Diff,     FracSumDiffMultistage, FracsDiffMultistage) (Default: FracsFixedSum)   
```

```lang-none

  --diffinit <DiffInit>      specify a method for initializing diffusivity (Options: Tensor, Fixed,     Best) (Default: Best)   
```

```lang-none

  --method <Method>      specify an orientation estimation method (Options: CSD, RLD) (Default: RLD)   
```

```lang-none

  --rldIters <int>      the number of iterations for RLD (Default: 500)   
```

```lang-none

  --rldAlpha <double>      the alpha parameter for RLD (Default: 0.0017)   
```

```lang-none

  --rldBeta <double>      the beta parameter for RLD (Default: 0.0)   
```

```lang-none

  --csdOrder <Integer>      specify the maximum spherical harmonic order for CSD (Default: 8)   
```

```lang-none

  --csdAxial <double>      the axial fiber response for CSD (Default: 0.0017)   
```

```lang-none

  --csdRadial <double>      the radial fiber response for CSD (Default: 1.2E-4)   
```

```lang-none

  --csdTau <double>      the tau parameter for CSD (Default: 0.1)   
```

```lang-none

  --csdLambda <double>      the lambda parameter for CSD (Default: 0.2)   
```

```lang-none

  --csdIters <int>      the number of iterations for CSD (Default: 50)   
```

### Advanced Parameter Arguments:

```lang-none

  --shells <String>      specify a subset of gradient shells to include (comma-separated list of     b-values)   
```

```lang-none

  --which <String>      specify a subset of gradients to include (comma-separated list of indices     starting from zero)   
```

```lang-none

  --exclude <String>      specify a subset of gradients to exclude (comma-separated list of indices     starting from zero)   
```

```lang-none

  --dperp <Double>      use the givel fraction of axial diffusivity when fitting fiber volume     fractions   
```

```lang-none

  --dfix <Double>      use the given fixed axial diffusivity when fitting fiber volume fractions     (Default: 0.00125)   
```

```lang-none

  --tort       use a tortuosity model (otherwise the extra-cellular diffusivity is matched     to the intra)   
```

```lang-none

  --restarts <Integer>      use the given fixed axial diffusivity when fitting fiber volume fractions     (Default: 1)   
```

```lang-none

  --nosmt       skip SMT estimation (for multishell data only)   
```

```lang-none

  --modeSingle <Mode>      specify an parameter estimation mode for single shell analysis (Options:     Fracs, FracsFixedSum, FracsDiff, FracSum, FracSumDiff, Diff,     FracSumDiffMultistage, FracsDiffMultistage) (Default:     FracSumDiffMultistage)   
```

```lang-none

  --modeMulti <Mode>      specify an parameter estimation mode for single shell analysis (Options:     Fracs, FracsFixedSum, FracsDiff, FracSum, FracSumDiff, Diff,     FracSumDiffMultistage, FracsDiffMultistage) (Default: FracsFixedSum)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output fibers volume   
```

### Author:

  Ryan Cabeen 

<hr/>
<hr/>
<hr/>
## VolumeFibersFuse 

### Description:

  fuse a collection of fibers volumes 

### Required Input Arguments:

```lang-none

  --input <Volume(s)>      specify the input fibers volumes   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      specify a mask   
```

### Optional Parameter Arguments:

```lang-none

  --pattern <String(s)>      specify a list of names that will be substituted with input %s   
```

```lang-none

  --estimator <String>      specify an estimator type   
```

```lang-none

  --selection <String>      specify a selection type   
```

```lang-none

  --hpos <Double>      specify spatial bandwidth   
```

```lang-none

  --support <Integer>      specify a support size   
```

```lang-none

  --lambda <Double>      specify a lambda parameter   
```

```lang-none

  --maxcomps <Integer>      specify a maxima number of components   
```

```lang-none

  --restarts <Integer>      specify a number of restarts   
```

```lang-none

  --minfrac <Double>      specify a minima volume fraction   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      specify the output fibers volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeFibersNoise 

### Description:

  Add orientation and volume fraction noise to a fibers volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      input fibers volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --line <Double>      the sigma of the orientation noise   
```

```lang-none

  --frac <Double>      the sigma of the volume fraction noise   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output noisy fibers volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeFibersPhantomCrossing 

### Description:

  Create a benchmark fibers phantom 

### Optional Parameter Arguments:

```lang-none

  --size <int>      the size of the phantom (Default: 50)   
```

```lang-none

  --fracPrimary <double>      volume fraction of the primary bundle (Default: 0.4)   
```

```lang-none

  --fracSecondary <double>      volume fraction of the secondary bundle (Default: 0.4)   
```

```lang-none

  --radiusPrimary <double>      the relative radius of the primary bundle (0 to 1) (Default: 0.4)   
```

```lang-none

  --radiusSecondary <double>      the relative radius of the secondary bundle (0 to 1) (Default: 0.2)   
```

```lang-none

  --angle <double>      the angle between the primary and secondary bundles (Default: 45.0)   
```

```lang-none

  --noiseAngle <double>      the amount of noise added to the compartment fiber orientations (Default:     5.0)   
```

```lang-none

  --noiseFraction <double>      the amount of noise added to the compartment volume fraction (Default:     0.05)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the synthesized output fibers   
```

### Optional Output Arguments:

```lang-none

  --outputBundle <Mask>      the output bundle mask   
```

```lang-none

  --outputEnd <Mask>      the output end mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeFibersPick 

### Description:

  Project fibers onto a reference volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input fibers to pick   
```

```lang-none

  --reference <Volume>      the reference vectors   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask of the input image   
```

```lang-none

  --deform <Deformation>      a deformation from the input to the reference   
```

### Optional Parameter Arguments:

```lang-none

  --thresh <double>      a threshold for picking (Default: 0.05)   
```

```lang-none

  --threads <int>      the number of threads (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output fibers   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeFibersProject 

### Description:

  Project fibers onto a reference volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input fibers to project   
```

```lang-none

  --reference <Volume>      the reference fibers (may be deformed from another space)   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask of the input image   
```

```lang-none

  --deform <Deformation>      a deformation from the input to the reference   
```

### Optional Parameter Arguments:

```lang-none

  --thresh <double>      a threshold for compartment existence (Default: 0.01)   
```

```lang-none

  --keep       keep the reference fiber orientations   
```

```lang-none

  --angle <Double>      the maximum angle for matching   
```

```lang-none

  --presmooth <Double>      apply fiber smoothing before projection with the given bandwidth   
```

```lang-none

  --soft       use soft projection with angular smoothing   
```

```lang-none

  --soften <Double>      use the given soft smoothing angular bandwidth (zero to one, where greater     is more smoothing) (Default: 0.1)   
```

```lang-none

  --deviation       save the angular deviation to the fiber statistic field of the output   
```

```lang-none

  --smooth       apply fiber smoothing after projection   
```

```lang-none

  --hpos <Double>      apply smoothing with the given amount (bandwidth in mm) (default is largest     voxel dimension)   
```

```lang-none

  --mix <Double>      smoothly mix reference and the input voxel fiber orientations (0 is all     input, 1 is all reference)   
```

```lang-none

  --support <int>      the smoothing filter radius in voxels (Default: 3)   
```

```lang-none

  --threads <int>      the number of threads (Default: 1)   
```

### Advanced Parameter Arguments:

```lang-none

  --soft       use soft projection with angular smoothing   
```

```lang-none

  --soften <Double>      use the given soft smoothing angular bandwidth (zero to one, where greater     is more smoothing) (Default: 0.1)   
```

```lang-none

  --deviation       save the angular deviation to the fiber statistic field of the output   
```

```lang-none

  --hpos <Double>      apply smoothing with the given amount (bandwidth in mm) (default is largest     voxel dimension)   
```

```lang-none

  --mix <Double>      smoothly mix reference and the input voxel fiber orientations (0 is all     input, 1 is all reference)   
```

```lang-none

  --support <int>      the smoothing filter radius in voxels (Default: 3)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output fibers   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeFibersPrune 

### Description:

  Prune outlier fibers 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input fibers   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask of the input image   
```

### Optional Parameter Arguments:

```lang-none

  --normalize       globally normalize the volume fractions to be in a unit range   
```

```lang-none

  --min <Double>      a minimum volume fraction   
```

```lang-none

  --thresh <Double>      a threshold angle for pruning (in degrees)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output fibers   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeFibersSmooth 

### Description:

  Smooth a fibers volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input fibers volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --hpos <Double>      the positional bandwidth in mm (negative value will use the voxel size,     zero will skip smoothing) (Default: 1.0)   
```

```lang-none

  --support <Integer>      the filter radius in voxels (Default: 3)   
```

```lang-none

  --comps <int>      a maxima number of fiber compartments (Default: 3)   
```

```lang-none

  --threads <Integer>      the number of threads in the pool (Default: 1)   
```

```lang-none

  --hsig <Double>      the baseline signal adaptive bandwidth   
```

```lang-none

  --hsigrel       treat hsig as a fraction of the mean baseline signal (inside the mask)   
```

```lang-none

  --hfrac <Double>      the fraction adaptive bandwidth   
```

```lang-none

  --hdiff <Double>      the diffusivity adaptive bandwidth   
```

```lang-none

  --hdir <Double>      the fiber adaptive bandwidth   
```

```lang-none

  --min <double>      a minima volume fraction (Default: 0.01)   
```

```lang-none

  --lambda <double>      a data adaptive threshold (Default: 0.99)   
```

```lang-none

  --restarts <int>      a number of restarts (Default: 2)   
```

```lang-none

  --pass       pass through the data without smoothing   
```

### Advanced Parameter Arguments:

```lang-none

  --hsig <Double>      the baseline signal adaptive bandwidth   
```

```lang-none

  --hsigrel       treat hsig as a fraction of the mean baseline signal (inside the mask)   
```

```lang-none

  --hfrac <Double>      the fraction adaptive bandwidth   
```

```lang-none

  --hdiff <Double>      the diffusivity adaptive bandwidth   
```

```lang-none

  --hdir <Double>      the fiber adaptive bandwidth   
```

```lang-none

  --min <double>      a minima volume fraction (Default: 0.01)   
```

```lang-none

  --lambda <double>      a data adaptive threshold (Default: 0.99)   
```

```lang-none

  --restarts <int>      a number of restarts (Default: 2)   
```

```lang-none

  --pass       pass through the data without smoothing   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output fibers volume   
```

### Author:

  Ryan Cabeen 

### Citation:

  Cabeen, R. P., Bastin, M. E., & Laidlaw, D. H. (2016). Kernel regression 
  estimation of fiber orientation mixtures in mri MRI. NeuroImage, 127, 
  158-172. 

<hr/>
## VolumeFibersTransform 

### Description:

  Spatially transform a fibers volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input fibers volume   
```

```lang-none

  --reference <Volume>      input reference volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

```lang-none

  --inputMask <Mask>      use an input mask (defined in the input space)   
```

```lang-none

  --affine <Affine>      apply an affine xfm   
```

```lang-none

  --invaffine <Affine>      apply an inverse affine xfm   
```

```lang-none

  --deform <Deformation>      apply a deformation xfm   
```

### Optional Parameter Arguments:

```lang-none

  --reverse       reverse the order, i.e. compose the affine(deform(x)), whereas the default     is deform(affine(x))   
```

```lang-none

  --reorientation <ReorientationType>      specify a reorient method (Options: FiniteStrain, Jacobian) (Default:     Jacobian)   
```

```lang-none

  --estimation <EstimationType>      the estimation type (Options: Match, Rank) (Default: Match)   
```

```lang-none

  --selection <SelectionType>      the selection type (Options: Max, Fixed, Linear, Adaptive) (Default:     Adaptive)   
```

```lang-none

  --interp <KernelInterpolationType>      the interpolation type (Options: Nearest, Trilinear, Gaussian) (Default:     Trilinear)   
```

```lang-none

  --comps <int>      a maxima number of fiber compartments (Default: 3)   
```

```lang-none

  --support <Integer>      the filter radius in voxels (Default: 3)   
```

```lang-none

  --hpos <Double>      the positional bandwidth in mm (Default: 1.0)   
```

```lang-none

  --min <double>      a minima volume fraction (Default: 0.01)   
```

```lang-none

  --lambda <double>      a data adaptive threshold (Default: 0.99)   
```

```lang-none

  --threads <int>      the number of threads (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output transformed volume   
```

### Author:

  Ryan Cabeen 

### Citation:

  Cabeen, R. P., Bastin, M. E., & Laidlaw, D. H. (2016). Kernel regression 
  estimation of fiber orientation mixtures in mri MRI. NeuroImage, 127, 
  158-172. 

<hr/>
## VolumeFibersZoom 

### Description:

  Zoom a fibers volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input fibers volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --factor <double>      a zooming factor (Default: 2.0)   
```

```lang-none

  --isotropic <Double>      zoom to a given isotropic voxel size (not by a factor)   
```

```lang-none

  --estimation <EstimationType>      the estimation type (Options: Match, Rank) (Default: Match)   
```

```lang-none

  --selection <SelectionType>      the selection type (Options: Max, Fixed, Linear, Adaptive) (Default:     Linear)   
```

```lang-none

  --interp <KernelInterpolationType>      the interpolation type (Options: Nearest, Trilinear, Gaussian) (Default:     Trilinear)   
```

```lang-none

  --comps <int>      a maxima number of fiber compartments (Default: 3)   
```

```lang-none

  --support <Integer>      the filter radius in voxels (Default: 3)   
```

```lang-none

  --hpos <Double>      the positional bandwidth in mm (Default: 1.0)   
```

```lang-none

  --min <double>      a minima volume fraction (Default: 0.01)   
```

```lang-none

  --lambda <double>      a data adaptive threshold (Default: 0.99)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output fibers volume   
```

### Author:

  Ryan Cabeen 

### Citation:

  Cabeen, R. P., Bastin, M. E., & Laidlaw, D. H. (2016). Kernel regression 
  estimation of fiber orientation mixtures in mri MRI. NeuroImage, 127, 
  158-172. 

<hr/>
## VolumeFilterBilateral 

### Description:

  Filter a volume with a bilateral filter 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --channel <Integer>      volume channel (default applies to all)   
```

```lang-none

  --hpos <Double>      positional bandwidth (Default: 1.0)   
```

```lang-none

  --hval <Double>      data adaptive bandwidth   
```

```lang-none

  --support <int>      filter radius in voxels (filter will be 2 * support + 1 in each dimension)     (Default: 3)   
```

```lang-none

  --iterations <int>      number of repeats (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Author:

  Ryan Cabeen 

### Citation:

  Elad, M. (2002). On the origin of the bilateral filter and ways to improve 
  it. IEEE Transactions on image processing, 11(10), 1141-1151. 

<hr/>
## VolumeFilterDoG 

### Description:

  Filter a volume using a Gaussian kernel 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --support <int>      filter support (filter will be 2 * support + 1 voxels in each dimension)     (Default: 3)   
```

```lang-none

  --low <double>      the low smoothing level (Default: 1.0)   
```

```lang-none

  --high <double>      the low smoothing level (Default: 2.0)   
```

```lang-none

  --num <int>      number of applications (Default: 1)   
```

```lang-none

  --threads <int>      number of threads (Default: 1)   
```

```lang-none

  --preserve       preserve un-masked values through the filter   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeFilterFrangi 

### Description:

  Filter a volume using a Frangi filter.  This extracts tubular structures 
  (scale level is the sigma parameter of a Gaussian in mm) 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --support <int>      filter support (filter will be 2 * support + 1 voxels in each dimension)     (Default: 3)   
```

```lang-none

  --num <int>      number of smoothing iterations (Default: 1)   
```

```lang-none

  --low <double>      the low scale level (Default: 1.0)   
```

```lang-none

  --high <double>      the high scale level (Default: 5.0)   
```

```lang-none

  --scales <int>      the number of scale samples (Default: 5)   
```

```lang-none

  --alpha <double>      the alpha Frangi parameter (Default: 0.5)   
```

```lang-none

  --beta <double>      the beta Frangi parameter (Default: 0.5)   
```

```lang-none

  --gamma <double>      the gamma Frangi parameter for 3D structures (Default: 300.0)   
```

```lang-none

  --gammaPlanar <double>      the gamma Frangi parameter for 2D structures (Default: 15.0)   
```

```lang-none

  --threads <int>      number of threads (Default: 1)   
```

```lang-none

  --dark       use detect dark tubes instead of bright tubes   
```

```lang-none

  --sobel       use a sobel operator (default is central finite difference)   
```

```lang-none

  --full       return the full scale space (default is maximum)   
```

```lang-none

  --pass       pass un-masked values through the filter   
```

### Advanced Parameter Arguments:

```lang-none

  --num <int>      number of smoothing iterations (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output filter response   
```

### Optional Output Arguments:

```lang-none

  --outputScale <Volume>      output scale of the tubular structure   
```

### Author:

  Ryan Cabeen 

### Citation:

  Frangi, Alejandro F., et al. "Multiscale vessel enhancement filtering." 
  International Conference on Medical Image Computing and Computer-Assisted 
  Intervention. Springer, Berlin, Heidelberg, 1998. 

<hr/>
## VolumeFilterGaussian 

### Description:

  Filter a volume using a Gaussian kernel 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --support <int>      filter support (filter will be 2 * support + 1 voxels in each dimension)     (Default: 3)   
```

```lang-none

  --sigma <Double>      the smoothing level (Default: 1.0)   
```

```lang-none

  --num <int>      number of applications (Default: 1)   
```

```lang-none

  --threads <int>      number of threads (Default: 1)   
```

```lang-none

  --full       use a full convolution filter (otherwise separate filters are used)   
```

```lang-none

  --pass       pass un-masked values through the filter   
```

```lang-none

  --channel <Integer>      the volume channel (default applies to all)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output filtered volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeFilterGradient 

### Description:

  Filter a volume to compute its Hessian 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --threads <int>      number of threads (Default: 1)   
```

```lang-none

  --sobel       use a sobel operator (default is central finite difference)   
```

```lang-none

  --mag       return the gradient magnitude   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeFilterHessian 

### Description:

  Filter a volume to compute its Hessian 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --threads <int>      number of threads (Default: 1)   
```

```lang-none

  --sobel       use a sobel operator (default is central finite difference)   
```

```lang-none

  --mode <VolumeFilterHessianMode>      return the given output mode (Options: Matrix, Eigen, Determ, Norm, Westin,     Spherical, Linear, Planar, Ridge, Blob, DarkBlob, LightBlob) (Default:     Matrix)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeFilterLaplacian 

### Description:

  Filter a volume using a Laplacian kernel 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --support <int>      filter support (filter will be 2 * support + 1 voxels in each dimension)     (Default: 3)   
```

```lang-none

  --sigma <Double>      the smoothing level (Default: 1.0)   
```

```lang-none

  --amp <Double>      the  (Default: 1.0)   
```

```lang-none

  --threads <int>      number of threads (Default: 1)   
```

```lang-none

  --preserve       pass un-masked values through the filter   
```

```lang-none

  --channel <Integer>      the volume channel (default applies to all)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output filtered volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeFilterMeanShift 

### Description:

  Process a volume with mean shift analysis for either anisotropic filtering or 
  segmentation 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --hpos <double>      the spatial bandwidth in mm (Default: 8.0)   
```

```lang-none

  --hval <double>      the adaptive bandwidth in units of intensity (Default: 64.0)   
```

```lang-none

  --iters <int>      the maxima number of iterations (Default: 5000)   
```

```lang-none

  --minshift <double>      the threshold for stopping gradient updates (Default: 0.001)   
```

```lang-none

  --minsize <double>      the minima cluster size (Default: 50.0)   
```

### Optional Output Arguments:

```lang-none

  --filtered <Volume>      the output filtered image   
```

```lang-none

  --segmented <Mask>      the output segmentation   
```

### Author:

  Ryan Cabeen 

### Citation:

  Comaniciu, D., & Meer, P. (2002). Mean shift: A robust approach toward 
  feature space analysis. IEEE Transactions on pattern analysis and machine 
  intelligence, 24(5), 603-619. 

<hr/>
## VolumeFilterMedian 

### Description:

  Filter a volume using a median filter 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --window <int>      the window size in voxels (Default: 1)   
```

```lang-none

  --channel <Integer>      the volume channel (default applies to all)   
```

```lang-none

  --slice <String>      restrict the filtering to a specific slice (i, j, or k)   
```

```lang-none

  --num <Integer>      the number of times to filter (Default: 1)   
```

```lang-none

  --threads <Integer>      the number of threads in the pool (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeFilterNLM 

### Description:

  Apply a non-local means filter to a volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

```lang-none

  --hrelMask <Mask>      a mask for computing the mean image intensity for relative bandwidth (see     hrel)   
```

### Optional Parameter Arguments:

```lang-none

  --channel <Integer>      the volume channel (default applies to all)   
```

```lang-none

  --mode <FilterMode>      indicate whether the filtering should be volumetric (default) or restricted     to a specified slice (Options: Volumetric, SliceI, SliceJ, SliceK)     (Default: Volumetric)   
```

```lang-none

  --patch <int>      the patch size (Default: 1)   
```

```lang-none

  --search <int>      the search window size (Default: 2)   
```

```lang-none

  --stats <int>      the statistics window size (for computing mean and variance in adaptive     filtering) (Default: 2)   
```

```lang-none

  --h <Double>      use a fixed noise level (skips adaptive noise esimation)   
```

```lang-none

  --epsilon <double>      the minimum meaningful value for mean and vars (Default: 1.0E-5)   
```

```lang-none

  --meanThresh <double>      the mean intensity threshold (Default: 0.95)   
```

```lang-none

  --varThresh <double>      the variance intensity threshold (Default: 0.5)   
```

```lang-none

  --factor <double>      a factor for scaling adaptive sensitivity (larger values remove more noise)     (Default: 1.0)   
```

```lang-none

  --hrel       use the h bandwidth as a fraction of the mean image intensity   
```

```lang-none

  --hrelwhich <String>      specify which channels to use for hrel (Default: 0)   
```

```lang-none

  --rician       whether a Rician noise model should be used   
```

```lang-none

  --threads <Integer>      the number of threads in the pool (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Optional Output Arguments:

```lang-none

  --outputNoise <Volume>      output noise map   
```

### Author:

  Ryan Cabeen 

### Citation:

  Manjon, Jose V., et al. Adaptive non-local means denoising of MR images with 
  spatially varying noise levels. Journal of Magnetic Resonance Imaging 31.1 
  (2010): 192-203. 

<hr/>
## VolumeFilterNeuron 

### Description:

  Filter a volume to isolate neuronal structures 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Parameter Arguments:

```lang-none

  --median <Integer>      the number of median filter passes (Default: 1)   
```

```lang-none

  --dilate <Integer>      the number of dilation passes (Default: 1)   
```

```lang-none

  --min <Integer>      the minimum number of voxels in a neuron connected component (Default: 300)   
```

```lang-none

  --thresh <Double>      the threshold for segmenting cell bodies (Default: 0.01)   
```

```lang-none

  --frangi       apply the Frangi filter   
```

```lang-none

  --frangiThresh <double>      the threshold for Frangi segmentation (Default: 0.01)   
```

```lang-none

  --frangiScales <int>      the number of scales used in the Frangi filter (Default: 5)   
```

```lang-none

  --frangiLow <int>      the lowest scale used in the Frangi filter (Default: 1)   
```

```lang-none

  --frangiHigh <Integer>      the highest scale used in the Frangi filter (Default: 10)   
```

```lang-none

  --threads <int>      number of threads (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Optional Output Arguments:

```lang-none

  --outputFrangi <Volume>      output frangi   
```

```lang-none

  --outputMask <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeFilterPCA 

### Description:

  Denoise a volume using random matrix theory.  Noise is estimated using a 
  universal Marchenko Pastur distribution and removed via principal component 
  analysis 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --type <VolumeFilterPCAType>      the type of PCA filtering to use (Options: MP, CenterMP) (Default: MP)   
```

```lang-none

  --window <int>      the window size (Default: 5)   
```

```lang-none

  --threads <Integer>      the number of threads in the pool (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output denoised volume   
```

### Optional Output Arguments:

```lang-none

  --noise <Volume>      output noise map   
```

```lang-none

  --comps <Volume>      output pca component count   
```

### Author:

  Ryan Cabeen 

### Citation:

  Veraart, J., Novikov, D. S., Christiaens, D., Ades-Aron, B., Sijbers, J., & 
  Fieremans, E. (2016). Denoising of diffusion MRI using random matrix theory. 
  NeuroImage, 142, 394-406. 

<hr/>
## VolumeFilterPDE 

### Description:

  Filter a volume using a PDE representing anisotropic mri 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --channel <String>      the volume channel(s) (default applies to all)   
```

```lang-none

  --lambda <Double>      smoothing parameter (in mm) (Default: 0.16666666666666666)   
```

```lang-none

  --k <Double>      anisotropic flux parameter (relative to image intensities) (Default: 0.01)   
```

```lang-none

  --steps <Integer>      the number of timesteps to apply (Default: 5)   
```

```lang-none

  --anisotropic       use anisotropic smoothing   
```

```lang-none

  --quadratic       use a quadratic flux (instead of exponential)   
```

```lang-none

  --recurse       use the output as input   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Author:

  Ryan Cabeen 

### Citation:

  Perona, P., & Malik, J. (1990). Scale-space and edge detection using 
  anisotropic mri. IEEE Transactions on pattern analysis and machine 
  intelligence, 12(7), 629-639. 

<hr/>
## VolumeFilterSigmoid 

### Description:

  Filter a volume with a sigmoid to perform a soft threshold 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --thresh <double>      the position of the sigmoid (Default: 0.5)   
```

```lang-none

  --slope <double>      the slope of the sigmoid (Default: 25.0)   
```

```lang-none

  --low <double>      low output (Default: 0.0)   
```

```lang-none

  --high <double>      high output (Default: 1.0)   
```

```lang-none

  --invert       invert the sigmoid (lower input values will be high)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeFilterSigmoidRange 

### Description:

  Filter a volume with the produce of two sigmoids, which act as a soft range 
  threshold 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --below <double>      the below sigmoid threshold (Default: 0.5)   
```

```lang-none

  --above <double>      the above sigmoid threshold (Default: 0.5)   
```

```lang-none

  --slope <double>      the slope of the sigmoid (Default: 25.0)   
```

```lang-none

  --low <double>      low output (Default: 0.0)   
```

```lang-none

  --high <double>      high output (Default: 1.0)   
```

```lang-none

  --invert       invert the sigmoid   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeFilterTriangle 

### Description:

  Filter a volume using a Triangle kernel 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --support <int>      filter support (filter will be 2 * support + 1 in each dimension) (Default:     3)   
```

```lang-none

  --num <int>      number of applications (Default: 1)   
```

```lang-none

  --preserve       preserve un-masked values through the filter   
```

```lang-none

  --channel <Integer>      the volume channel (default applies to all)   
```

```lang-none

  --threads <Integer>      the number of threads (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeFilterZeroCrossing 

### Description:

  Filter a volume using a Gaussian kernel 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --large       use a bigger 27-voxel neighborhood   
```

```lang-none

  --mode <Mode>      the crossing selection mode (Options: Below, Above, Both) (Default: Both)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output zero crossings   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeFloodFill 

### Description:

  Flip the voxel values of volume along coordinate axes 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume of values for filling   
```

```lang-none

  --source <Mask>      a mask indicating the source, i.e. which voxels of the input are used for     filling   
```

### Optional Input Arguments:

```lang-none

  --dest <Mask>      a mask used for restricting the voxel flooded (must be convex, but this is     not checked)   
```

### Optional Parameter Arguments:

```lang-none

  --fix <int>      maximum number of fixing passes (Default: 0)   
```

### Required Output Arguments:

```lang-none

  --outputFlood <Volume>      output volume of flood values   
```

### Optional Output Arguments:

```lang-none

  --outputDist <Volume>      output distance field   
```

### Author:

  Ryan Cabeen 

<hr/>
<hr/>
## VolumeFuse 

### Description:

  fuse volumes 

### Required Input Arguments:

```lang-none

  --input <Volume(s)>      the input volumes   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      specify a mask   
```

### Optional Parameter Arguments:

```lang-none

  --pattern <String(s)>      specify a list of names that will be substituted with input %s   
```

```lang-none

  --bg <Double>      specify an additional background value for softmax or sumnorm   
```

```lang-none

  --gain <Double>      specify a gain for softmax   
```

```lang-none

  --offset <Double>      specify a offset for softmax   
```

```lang-none

  --skip       skip missing files   
```

```lang-none

  --limit       do not load more than this number of images   
```

```lang-none

  --robust       use robust statistics   
```

```lang-none

  --exmulti       exclude extra multi-channel volumes (load only the first one)   
```

```lang-none

  --norm       use the vector norm (or absolute value)   
```

### Optional Output Arguments:

```lang-none

  --output-cat <Volume>      specify the output concatenated volume   
```

```lang-none

  --output-min <Volume>      specify the output min volume   
```

```lang-none

  --output-max <Volume>      specify the output max volume   
```

```lang-none

  --output-sum <Volume>      specify the output sum volume   
```

```lang-none

  --output-mean <Volume>      specify the output mean volume   
```

```lang-none

  --output-median <Volume>      specify the output median volume   
```

```lang-none

  --output-var <Volume>      specify the output var volume   
```

```lang-none

  --output-std <Volume>      specify the output std volume   
```

```lang-none

  --output-cv <Volume>      specify the output cv volume   
```

```lang-none

  --output-softmax <Volume>      specify the output softmax   
```

```lang-none

  --output-sumnorm <Volume>      specify the output sumnorm   
```

```lang-none

  --output-first <Volume>      specify the output first volume   
```

```lang-none

  --output-last <Volume>      specify the output last volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeGradient 

### Description:

  Compute the gradient of an image 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --channel <Integer>      the volume channel (default applies to all) (Default: 0)   
```

```lang-none

  --type <String>      type of finite difference approximation (forward, backward, central)     (Default: central)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeGradientMagnitude 

### Description:

  Compute the gradient magnitude of an image 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --channel <Integer>      the volume channel (default applies to all) (Default: 0)   
```

```lang-none

  --type <String>      type of finite difference approximation (forward, backward, central)     (Default: central)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeHarmonize 

### Description:

  Harmonize a volume with a reference volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input volume   
```

### Optional Input Arguments:

```lang-none

  --inputMask <Mask>      an input mask to restrict both statistics and harmonized voxels   
```

```lang-none

  --inputStatMask <Mask>      an input mask to only restrict computing statistics   
```

```lang-none

  --reference <Volume>      a reference volume   
```

```lang-none

  --referenceMask <Mask>      a reference mask   
```

### Optional Parameter Arguments:

```lang-none

  --bins <int>      the number of bins (Default: 256)   
```

```lang-none

  --lower <Bound>      the method for computing the histogram lower bound (Options: Extrema,     Quartile, HalfStd, OneStd, TwoStd, ThreeStd, User) (Default: ThreeStd)   
```

```lang-none

  --upper <Bound>      the method for computing the histogram upper bound (Options: Extrema,     Quartile, HalfStd, OneStd, TwoStd, ThreeStd, User) (Default: ThreeStd)   
```

```lang-none

  --mylower <Double>      specify a specific lower bound for user defined mode   
```

```lang-none

  --myupper <Double>      specify a specific upper bound for user defined mode   
```

```lang-none

  --smooth <Double>      the amount of smoothing (relative to bins) (Default: 10.0)   
```

```lang-none

  --lowerThresh <Double>      a lower threshold for excluding background   
```

```lang-none

  --upperThresh <Double>      an upper threshold for excluding background   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeHistogram 

### Description:

  Compute a histogram of a volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      an input mask   
```

### Optional Parameter Arguments:

```lang-none

  --bins <int>      the number of bins (Default: 256)   
```

```lang-none

  --lower <Bound>      the method for computing the histogram lower bound (Options: Extrema,     Quartile, HalfStd, OneStd, TwoStd, ThreeStd, User) (Default: Extrema)   
```

```lang-none

  --upper <Bound>      the method for computing the histogram upper bound (Options: Extrema,     Quartile, HalfStd, OneStd, TwoStd, ThreeStd, User) (Default: Extrema)   
```

```lang-none

  --mylower <Double>      specify a specific lower bound for user defined mode   
```

```lang-none

  --myupper <Double>      specify a specific upper bound for user defined mode   
```

```lang-none

  --lowerThresh <Double>      apply a lower threshold before computing the histogram   
```

```lang-none

  --upperThresh <Double>      apply an upper threshold before computing the histogram   
```

```lang-none

  --normalize       normalize the counts by the total count   
```

```lang-none

  --cdf       compute the cdf   
```

```lang-none

  --smooth <Double>      apply Gaussian smoothing (see hdata)   
```

```lang-none

  --hdata       interpret the smoothing parameter relative to the data intensities (default     is bins)   
```

```lang-none

  --print       print a the histogram to standard output   
```

```lang-none

  --value <String>      the value column name (Default: value)   
```

### Optional Output Arguments:

```lang-none

  --output <Table>      an output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeImpute 

### Description:

  Impute voxels by computing a moving average 

### Required Input Arguments:

```lang-none

  --input <Volume>      input Volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

```lang-none

  --impute <Mask>      input imputation mask   
```

### Optional Parameter Arguments:

```lang-none

  --support <int>      the radius for imputation (Default: 5)   
```

```lang-none

  --knn <int>      the k-nearest neighbors for imputation (Default: 6)   
```

```lang-none

  --missing <Double>      a value for missing cases (when imputation region is larger than the     support)   
```

```lang-none

  --outlier <Double>      remove extreme values (specify a k value for Tukey's fence, e.g. 1.5 for     outliers or 3 for far out)   
```

### Optional Output Arguments:

```lang-none

  --output <Volume>      output Volume   
```

```lang-none

  --outputOutlier <Mask>      output outlier mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeIsotropic 

### Description:

  Isotropically scaleCamera a volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input volume   
```

### Optional Parameter Arguments:

```lang-none

  --size <double>      the output voxel size (Default: 1.0)   
```

```lang-none

  --interp <InterpolationType>      an interpolation type (Options: Nearest, Trilinear, Tricubic, Gaussian,     GaussianLocalLinear, GaussianLocalQuadratic) (Default: Tricubic)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeJacobian 

### Description:

  Compute the jacobian of a deformation volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

```lang-none

  --affine <Affine>      optional affine component to remove   
```

### Optional Parameter Arguments:

```lang-none

  --deformation       treat the input as a deformation (i.e. displacement)   
```

```lang-none

  --eigenvalues       return the eigenvalues (exclusive with determinant flag)   
```

```lang-none

  --determinant       return the determinant (exclusive with eigenvalues flag)   
```

```lang-none

  --logarithm       return the logarithm (works with other options)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeKurtosisFit 

### Description:

  Fit a kurtosis volume.  Warning: this does not yet estimate the kurtosis 
  indices, e.g. FA, MK, etc. 

### Required Input Arguments:

```lang-none

  --input <Volume>      input diffusion-weighted MR volume   
```

```lang-none

  --gradients <Gradients>      the gradients   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      the mask   
```

### Optional Parameter Arguments:

```lang-none

  --method <KurtosisFitType>      specify an estimation method, (FW enables free water elimination) (Options:     LLS, WLLS, FWLLS, FWWLLS) (Default: LLS)   
```

```lang-none

  --shells <String>      specify a subset of gradient shells to include (comma-separated list of     b-values)   
```

```lang-none

  --which <String>      specify a subset of gradients to include (comma-separated list of indices     starting from zero)   
```

```lang-none

  --exclude <String>      specify a subset of gradients to exclude (comma-separated list of indices     starting from zero)   
```

```lang-none

  --threads <int>      the number of threads to use (Default: 1)   
```

### Advanced Parameter Arguments:

```lang-none

  --shells <String>      specify a subset of gradient shells to include (comma-separated list of     b-values)   
```

```lang-none

  --which <String>      specify a subset of gradients to include (comma-separated list of indices     starting from zero)   
```

```lang-none

  --exclude <String>      specify a subset of gradients to exclude (comma-separated list of indices     starting from zero)   
```

```lang-none

  --threads <int>      the number of threads to use (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output kurtosis volume   
```

### Author:

  Ryan Cabeen 

### Citation:

  Veraart, J., Sijbers, J., Sunaert, S., Leemans, A., Jeurissen, B.: Weighted 
  linear least squares estimation of mri mri parameters: strengths, 
  limitations, and pitfalls. NeuroImage 81 (2013) 335-346 

<hr/>
<hr/>
## VolumeKurtosisODF 

### Description:

  Sample an orientation distribution function (ODF) from a kurtosis volume. 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input kurtosis volume   
```

```lang-none

  --dirs <Vects>      the sphere directions   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --alpha <Double>      the alpha power value (Default: 10.0)   
```

```lang-none

  --detail <Integer>      the level of detail for spherical ODF sampling (Default: 4)   
```

```lang-none

  --threads <Integer>      the number of threads in the pool (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output odf volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeKurtosisPeaks 

### Description:

  Extract the peaks from a kurtosis volume.  This finds the average direction 
  of local maxima clustered by hierarchical clustering.  The output fibers will 
  encode the peak ODF value in place of volume fraction. 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input kurtosis volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --comps <Integer>      the maximum number of comps (Default: 3)   
```

```lang-none

  --alpha <Double>      the alpha power value (Default: 10.0)   
```

```lang-none

  --thresh <Double>      the minimum peak threshold (Default: 0.0)   
```

```lang-none

  --detail <Integer>      the level of detail for spherical ODF sampling (Default: 4)   
```

```lang-none

  --cluster <Double>      the minimum angle in degrees for hierarchical clustering of local maxima     (Default: 5.0)   
```

```lang-none

  --threads <Integer>      the number of threads in the pool (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output fibers volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeMagnitude 

### Description:

  Compute the magnitude of an image 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeMarchingCubes 

### Description:

  Extract a mesh from a volume level set.  This can be used for extracting 
  boundary surfaces of distance fields, probability maps, parametric maps, etc. 
   The background level should be changed depending on the context though to 
  make sure the mesh orientation is correct. 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Parameter Arguments:

```lang-none

  --level <double>      level set to extract (Default: 0.5)   
```

```lang-none

  --background <double>      background value for outside sampling range (should be zero if the     background is 'black' and a large number if the input is similar to a     distance field) (Default: 0.0)   
```

```lang-none

  --nobound       exclude triangles on the boundary of the volume   
```

```lang-none

  --largest       retain only the largest component   
```

```lang-none

  --multi       extract a mesh for every channel   
```

```lang-none

  --label <String>      a label attribute name for multi-channel extraction (Default: label)   
```

```lang-none

  --median <Integer>      apply a median filter a given number of times before surface extraction   
```

```lang-none

  --std <Double>      apply g Gaussian smoothing before surface extraction   
```

```lang-none

  --support <Integer>      use a given support in voxels for voxel smoothing (Default: 3)   
```

```lang-none

  --meanarea <Double>      simplify the mesh to have the given mean triangle area   
```

```lang-none

  --noboundmesh       do not simplify vertices on the mesh boundary   
```

```lang-none

  --smooth <Double>      smooth the mesh   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      output mesh   
```

### Author:

  Ryan Cabeen 

### Citation:

  Lorensen, W. E., & Cline, H. E. (1987, August). Marching cubes: A high 
  resolution 3D surface construction algorithm. In ACM SIGGRAPH Computer 
  Graphics (Vol. 21, No. 4, pp. 163-169) 

<hr/>
## VolumeMarchingSquares 

### Description:

  Extract isocontours from slices of a volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Parameter Arguments:

```lang-none

  --level <double>      level set to extract (Default: 0.0)   
```

```lang-none

  --background <double>      background value for outside sampling range (Default:     1.7976931348623157E308)   
```

```lang-none

  --dim <String>      volume channel to contour (x, y, or z) (Default: z)   
```

```lang-none

  --start <int>      starting slice index (Default: 0)   
```

```lang-none

  --step <int>      number of slices between contours (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      output contours   
```

### Author:

  Ryan Cabeen 

### Citation:

  Maple, C. (2003, July). Geometric design and space planning using the 
  marching squares and marching cube algorithms. In Geometric Modeling and 
  Graphics, 2003. Proceedings. 2003 International Conference on (pp. 90-95). 
  IEEE. 

<hr/>
## VolumeMask 

### Description:

  Mask a volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

```lang-none

  --mask <Mask>      a mask   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeMaxProb 

### Description:

  Find the maxima probability labels 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask to indicate background   
```

### Optional Parameter Arguments:

```lang-none

  --bglabel <int>      the background label (Default: 0)   
```

```lang-none

  --bgvalue <double>      the background value (Default: 1.0)   
```

### Optional Output Arguments:

```lang-none

  --labels <Mask>      output mask of the extremal index   
```

```lang-none

  --values <Volume>      output volume of the extremal values   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeMcsmtFit 

### Description:

  Estimate multi-compartment microscopic diffusion parameters 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input diffusion-weighted MR volume   
```

```lang-none

  --gradients <Gradients>      the input gradients   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      the input mask   
```

### Optional Parameter Arguments:

```lang-none

  --threads <int>      the input number of threads (Default: 1)   
```

```lang-none

  --maxiters <int>      the maximum number of iterations (Default: 1500)   
```

```lang-none

  --rhobeg <double>      the starting simplex size (Default: 0.1)   
```

```lang-none

  --rhoend <double>      the ending simplex size (Default: 0.001)   
```

```lang-none

  --dint <double>      the initial guess for parallel diffusivity (Default: 0.0017)   
```

```lang-none

  --frac <double>      the initial guess for intra-neurite volume fraction (Default: 0.5)   
```

```lang-none

  --fscale <double>      the scaling for volume fraction optimization (Default: 10.0)   
```

```lang-none

  --dscale <double>      the scaling for diffusivity optimization (Default: 1000.0)   
```

```lang-none

  --dmax <double>      the maximum diffusivity (Default: 0.003)   
```

```lang-none

  --dot       include a dot compartment   
```

### Advanced Parameter Arguments:

```lang-none

  --maxiters <int>      the maximum number of iterations (Default: 1500)   
```

```lang-none

  --rhobeg <double>      the starting simplex size (Default: 0.1)   
```

```lang-none

  --rhoend <double>      the ending simplex size (Default: 0.001)   
```

```lang-none

  --dint <double>      the initial guess for parallel diffusivity (Default: 0.0017)   
```

```lang-none

  --frac <double>      the initial guess for intra-neurite volume fraction (Default: 0.5)   
```

```lang-none

  --fscale <double>      the scaling for volume fraction optimization (Default: 10.0)   
```

```lang-none

  --dscale <double>      the scaling for diffusivity optimization (Default: 1000.0)   
```

```lang-none

  --dmax <double>      the maximum diffusivity (Default: 0.003)   
```

```lang-none

  --dot       include a dot compartment   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output parameter map   
```

### Author:

  Ryan Cabeen 

### Citation:

  Kaden, E., Kelm, N. D., Carson, R. P., Does, M. D., & Alexander, D. C. 
  (2016). Multi-compartment microscopic diffusion imaging. NeuroImage, 139, 
  346-359. 

<hr/>
## VolumeMcsmtTransform 

### Description:

  Transform an mcsmt volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input mcsmt volume   
```

```lang-none

  --reference <Volume>      input reference volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

```lang-none

  --inputMask <Mask>      use an input mask (defined in the input space)   
```

```lang-none

  --affine <Affine>      apply an affine xfm   
```

```lang-none

  --invaffine <Affine>      apply an inverse affine xfm   
```

```lang-none

  --deform <Deformation>      apply a deformation xfm   
```

### Optional Parameter Arguments:

```lang-none

  --reverse       reverse the order, i.e. compose the affine(deform(x)), whereas the default     is deform(affine(x))   
```

```lang-none

  --reorient <ReorientationType>      specify a reorient method (Options: FiniteStrain, Jacobian) (Default:     Jacobian)   
```

```lang-none

  --interp <KernelInterpolationType>      the interpolation type (Options: Nearest, Trilinear, Gaussian) (Default:     Trilinear)   
```

```lang-none

  --support <Integer>      the filter radius in voxels (Default: 3)   
```

```lang-none

  --hpos <Double>      the positional bandwidth in mm (Default: 1.0)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output transformed mcsmt volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeMeasure 

### Description:

  Collect statistical sumaries of a volume using a mask.  This supports single 
  label masks and more advanced options for multi-channel masks (e.g. loading 
  names associated with each label) 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --weight <Volume>      a weighting   
```

```lang-none

  --mask <Mask>      a mask   
```

```lang-none

  --lookup <Table>      use a lookup for region names   
```

### Optional Parameter Arguments:

```lang-none

  --background       use the background   
```

```lang-none

  --multiple       use multiple regions   
```

```lang-none

  --channel <int>      specify a volume channel (Default: 0)   
```

```lang-none

  --base <String>      specify a region basename (Default: region)   
```

```lang-none

  --index <String>      specify the lookup index field (Default: index)   
```

```lang-none

  --name <String>      specify the lookup name field (Default: name)   
```

```lang-none

  --region <String>      specify the output region field name (Default: region)   
```

```lang-none

  --stat <String>      specify the output statistic field name (Default: stat)   
```

```lang-none

  --value <String>      specify the output value field name (Default: value)   
```

```lang-none

  --include <String>      specify which statistics should be included (e.g. mean)   
```

```lang-none

  --union       combine the region and stat fields into one   
```

```lang-none

  --join <String>      the character for joining region and stat names (depends on union flag)     (Default: _)   
```

```lang-none

  --nostat       exclude the statistics field   
```

```lang-none

  --widen       widen the table on the statistics field   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeMeasureBatch 

### Description:

  Compute measures of a set of volumes in batch mode. 

### Required Input Arguments:

```lang-none

  --input <FilePattern>      specify an input filename pattern (masks or density volumes)   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      specify a mask for including voxels   
```

### Required Parameter Arguments:

```lang-none

  --names <Spec>      specify bundle identifiers   
```

### Optional Parameter Arguments:

```lang-none

  --density       measure density from input mask   
```

```lang-none

  --thresh <Double>      specify a density threshold for volumetry (Default: 0.5)   
```

```lang-none

  --volume <String=Volume> [...]      specify volumes to sample   
```

### Required Output Arguments:

```lang-none

  --output <Directory>      specify an output directory   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeModelError 

### Description:

  Compute the root mean square error between a model and dwi volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      input model volume   
```

```lang-none

  --dwi <Volume>      input dwi   
```

```lang-none

  --gradients <Gradients>      input gradients   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --model <String>      a model name (default will try to detect it)   
```

```lang-none

  --type <ModelErrorType>      specify an error type (default is root mean square error) (Options: MSE,     RMSE, NRMSE, BIC, AIC, AICc) (Default: RMSE)   
```

```lang-none

  --dof <Integer>      specify an specific degrees of freedom (useful for models that were     constrained when fitting)   
```

```lang-none

  --dparNoddi <Double>      specify a NODDI parallel diffusivity   
```

```lang-none

  --disoNoddi <Double>      specify a NODDI isotropic diffusivity   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output error volume   
```

### Author:

  Ryan Cabeen 

### Citation:

  Burnham, K. P., & Anderson, D. R. (2004). Multimodel inference: understanding 
  AIC and BIC in model selection. Sociological methods & research, 33(2), 
  261-304. 

<hr/>
## VolumeModelFeature 

### Description:

  Extract a feature from a model volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      input model volume   
```

### Optional Parameter Arguments:

```lang-none

  --model <String>      a model name (default will try to detect it)   
```

```lang-none

  --feature <String>      a feature name   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output feature volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeModelReorient 

### Description:

  Reorient the fiber orientations of a model volume.  If both a flip and swap 
  are specified, the flip is performed first. This only works for tensor, 
  noddi, and fibers volumes 

### Required Input Arguments:

```lang-none

  --input <Volume>      input model volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --model <String>      a model name (default will try to detect it)   
```

```lang-none

  --flip <String>      flip a coodinate (x, y, z, or none)   
```

```lang-none

  --swap <String>      swap a pair of coordinates (xy, xz, yz, or none)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output error volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeModelSample 

### Description:

  Sample model parameters based on a variety of possible data objects 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input model volume   
```

### Optional Input Arguments:

```lang-none

  --vects <Vects>      vects storing the position where models should be sampled   
```

```lang-none

  --solids <Solids>      solids storing the position where models should be sampled   
```

```lang-none

  --mask <Mask>      mask storing the position where models should be sampled   
```

```lang-none

  --curves <Curves>      curves storing the position where models should be sampled   
```

```lang-none

  --mesh <Mesh>      mesh storing the position where models should be sampled   
```

### Optional Parameter Arguments:

```lang-none

  --model <ModelType>      a model type (if you select None, the code will try to detect it) (Options:     Vect, Tensor, BiTensor, Fibers, Spharm, Kurtosis, Noddi, ExpDecay, Mcsmt)     (Default: Vect)   
```

```lang-none

  --jitter <Double>      jitter the samples by a given amount   
```

```lang-none

  --cull <Double>      cull the samples at a given threshold ditance   
```

```lang-none

  --multiplier <Integer>      a multiplier of the number of samples taken (sometimes only makes sense     when jittering) (Default: 1)   
```

```lang-none

  --limit <Integer>      a maximum number of samples (the excess is randomly selected)   
```

```lang-none

  --param       samples should store only model parameters (otherwise the position is     prepended)   
```

```lang-none

  --interp <KernelInterpolationType>      the interpolation type (Options: Nearest, Trilinear, Gaussian) (Default:     Trilinear)   
```

```lang-none

  --support <Integer>      the interpolation filter radius in voxels (Default: 5)   
```

```lang-none

  --hpos <Double>      the positional interpolation bandwidth in mm (Default: 1.5)   
```

```lang-none

  --comps <int>      a maxima number of compartments (xfib only) (Default: 3)   
```

```lang-none

  --log       use log-euclidean estimation (dti only)   
```

```lang-none

  --threads <int>      the number of threads (Default: 4)   
```

### Advanced Parameter Arguments:

```lang-none

  --interp <KernelInterpolationType>      the interpolation type (Options: Nearest, Trilinear, Gaussian) (Default:     Trilinear)   
```

```lang-none

  --support <Integer>      the interpolation filter radius in voxels (Default: 5)   
```

```lang-none

  --hpos <Double>      the positional interpolation bandwidth in mm (Default: 1.5)   
```

```lang-none

  --comps <int>      a maxima number of compartments (xfib only) (Default: 3)   
```

```lang-none

  --log       use log-euclidean estimation (dti only)   
```

```lang-none

  --threads <int>      the number of threads (Default: 4)   
```

### Required Output Arguments:

```lang-none

  --output <Vects>      the output sampled vects   
```

### Author:

  Ryan Cabeen 

### Citation:

  Cabeen, R. P., Bastin, M. E., & Laidlaw, D. H. (2016). Kernel regression 
  estimation of fiber orientation mixtures in mri MRI. NeuroImage, 127, 
  158-172. 

<hr/>
## VolumeModelSynth 

### Description:

  Synthesize a dMRI from a fibers volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      input model volume   
```

```lang-none

  --gradients <Gradients>      the gradient scheme   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --model <String>      a model name (default will try to detect it)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output synthesized diffusion-weighted MR volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeModelTrackStreamline 

### Description:

  Perform deterministic multi-fiber streamline tractography from a model 
  volume.  This supports tensor, fibers, spharm, and noddi volumes. 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input model volume   
```

### Optional Input Arguments:

```lang-none

  --seedVects <Vects>      seed from vects   
```

```lang-none

  --seedMask <Mask>      a seed mask (one seed per voxel is initiated from this)   
```

```lang-none

  --seedSolids <Solids>      seed from solids (one seed per object is initiated from this)   
```

```lang-none

  --includeMask <Mask>      an include mask (curves are only included if they touch this, i.e. AND)   
```

```lang-none

  --includeSolids <Solids>      an include solids (curves are only included if they touch this, i.e. AND)   
```

```lang-none

  --includeAddMask <Mask>      an additional include mask (curves are only included if they touch this,     i.e. AND)   
```

```lang-none

  --includeAddSolids <Solids>      an additional include solids (curves are only included if they touch this,     i.e. AND)   
```

```lang-none

  --excludeMask <Mask>      an exclude mask (curves are removed if they touch this mask, i.e. NOT)   
```

```lang-none

  --excludeSolids <Solids>      an exclude solids object (curves are removed if they touch any solid, ie.e     NOT)   
```

```lang-none

  --trapMask <Mask>      a trap mask (tracking terminates when leaving this mask)   
```

```lang-none

  --stopMask <Mask>      a stop mask (tracking terminates when reaching this mask)   
```

```lang-none

  --stopSolids <Solids>      a stop solids object  (tracking terminates when reaching any solid)   
```

```lang-none

  --endMask <Mask>      an endpoint mask (curves are only retained if they end inside this)   
```

```lang-none

  --endSolids <Solids>      an endpoint solids object (curves are only retained if they end inside     this)   
```

```lang-none

  --trackMask <Mask>      a tracking mask (tracking is stopped if a curve exits this mask)   
```

```lang-none

  --trackSolids <Solids>      a tracking solids (tracking is stopped if a curve exits solids)   
```

```lang-none

  --containMask <Mask>      a containment mask (a minimum fraction of arclength must be inside this     mask)   
```

```lang-none

  --connectMask <Mask>      a connection mask (tracks will be constrained to connect distinct labels)   
```

```lang-none

  --odfPoints <Vects>      specify the spherical points for processing the odf (the number of vectors     should match the dimensionality of the input volume)   
```

```lang-none

  --force <Volume>      use a force field   
```

```lang-none

  --hybridPeaks <Volume>      use the given peaks (otherwise they will be computed from the input)   
```

```lang-none

  --hybridConnectMask <Mask>      use the given connect mask during the initial tracking phase (but not the     secondary one)   
```

```lang-none

  --hybridTrapMask <Mask>      use the given trap mask during the initial tracking phase (but not the     secondary one)   
```

```lang-none

  --hybridExcludeMask <Mask>      use the given exclude mask during the initial tracking phase (but not the     secondary one)   
```

```lang-none

  --hybridStopMask <Mask>      use the given stop mask during the initial tracking phase (but not the     secondary one)   
```

```lang-none

  --hybridTrackMask <Mask>      use the given track mask during the initial tracking phase (but not the     secondary one)   
```

```lang-none

  --hybridCurves <Curves>      use the given curves (and skip the first stage of hybrid tracking)   
```

### Optional Parameter Arguments:

```lang-none

  --samplesFactor <Double>      increase or decrease the total number of samples by a given factor     (regardless of whether using vects, mask, or solids) (Default: 1.0)   
```

```lang-none

  --samplesMask <Integer>      the number of samples per voxel when using mask seeding (Default: 1)   
```

```lang-none

  --samplesSolids <Integer>      the number of samples per object when using solid seeding (Default: 5000)   
```

```lang-none

  --min <double>      a minimum cutoff value for tracking (depend on the voxel model, e.g. FA for     DTI, frac for Fibers, etc.) (Default: 0.075)   
```

```lang-none

  --angle <Double>      the angle stopping criteria (maximum change in orientation per step)     (Default: 45.0)   
```

```lang-none

  --disperse <Double>      use a fixed amount of orientation dispersion during tracking (e.g. 0.1)     (Default: 0.0)   
```

```lang-none

  --step <Double>      the step size for tracking (Default: 1.0)   
```

```lang-none

  --minlen <Double>      the minimum streamline length (Default: 0.0)   
```

```lang-none

  --maxlen <Double>      the maximum streamline length (Default: 1000000.0)   
```

```lang-none

  --maxseeds <Integer>      a maximum number of seeds (Default: 2000000)   
```

```lang-none

  --maxtracks <Integer>      a maximum number of tracks (Default: 2000000)   
```

```lang-none

  --interp <KernelInterpolationType>      the interpolation type (Options: Nearest, Trilinear, Gaussian) (Default:     Nearest)   
```

```lang-none

  --mono       use monodirectional seeding (default is bidirectional), e.g. for seeding     from a subcortical structure   
```

```lang-none

  --vector       use vector field integration (the default is to use axes, which have not     preferred forward/back direction)   
```

```lang-none

  --prob       use probabilistic sampling and selection   
```

```lang-none

  --probMax       follow the maximum probability direction (only used when the prob flag is     enabled)   
```

```lang-none

  --probPower <Double>      raise the sample probability to the given power (to disable, leave at one)     (Default: 1.0)   
```

```lang-none

  --probAngle <Double>      the prior gain for angle changes (beta * change / threshold) (t disable,     set to zero) (Default: 5.0)   
```

```lang-none

  --probPoints <Integer>      the number of ODF points to sample for probabilistic sampling (or you can     provide specific odfPoints) (Default: 300)   
```

```lang-none

  --endConnect       find connections in the end mask   
```

```lang-none

  --hybrid       use the hybrid approach   
```

```lang-none

  --hybridSamplesFactor <Double>      specify a seed multiplication factor for the first stage of hybrid tracking     (Default: 1.0)   
```

```lang-none

  --hybridMin <Double>      specify an minimum for the first stage of hybrid tracking   
```

```lang-none

  --hybridAngle <Double>      specify an angle for the first stage of hybrid tracking   
```

```lang-none

  --hybridDisperse <Double>      specify an amount of dispersion to in the first stage of hybrid tracking     (Default: 0.1)   
```

```lang-none

  --hybridPresmooth <Double>      specify an pre-smoothing bandwidth (before projection)   
```

```lang-none

  --hybridProjAngle <Double>      specify an angle for hybrid projection (Default: 45.0)   
```

```lang-none

  --hybridProjNorm <Double>      specify a minimum norm for hybrid projection (Default: 0.01)   
```

```lang-none

  --hybridProjFrac <Double>      specify a minimum compartment fraction for hybrid projection (Default:     0.025)   
```

```lang-none

  --hybridProjFsum <Double>      specify a minimum total fraction for hybrid projection (Default: 0.05)   
```

```lang-none

  --hybridPostsmooth <Double>      specify an post-smoothing bandwidth (after projection)   
```

```lang-none

  --hybridInterp <KernelInterpolationType>      the interpolation type for the first stage of hybrid tracking (Options:     Nearest, Trilinear, Gaussian) (Default: Nearest)   
```

```lang-none

  --gforce <Double>      the prior gain forces (larger values have smoother effects and zero is hard     selection) (Default: 0.0)   
```

```lang-none

  --model <String>      a model name (default will try to detect it)   
```

```lang-none

  --threads <int>      the number of threads (Default: 3)   
```

```lang-none

  --max <Double>      a maximum value for tracking (FA for dti, frac for xfib/spharm, ficvf for     noddi)   
```

```lang-none

  --mixing <Double>      the mixing weight for orientation updates (0 to 1) (Default: 1.0)   
```

```lang-none

  --arclen <Double>      an arclength containment threshold (Default: 0.8)   
```

```lang-none

  --reach <Double>      only allow tracking to travel a given reach distance from the seed   
```

```lang-none

  --rk       use fourth-order Runge-Kutta integration (default is Euler)   
```

```lang-none

  --binarize       binarize masks (ignore multiple labels in include and other masks)   
```

```lang-none

  --quiet       print progress messages   
```

### Advanced Parameter Arguments:

```lang-none

  --vector       use vector field integration (the default is to use axes, which have not     preferred forward/back direction)   
```

```lang-none

  --hybrid       use the hybrid approach   
```

```lang-none

  --hybridSamplesFactor <Double>      specify a seed multiplication factor for the first stage of hybrid tracking     (Default: 1.0)   
```

```lang-none

  --hybridMin <Double>      specify an minimum for the first stage of hybrid tracking   
```

```lang-none

  --hybridAngle <Double>      specify an angle for the first stage of hybrid tracking   
```

```lang-none

  --hybridDisperse <Double>      specify an amount of dispersion to in the first stage of hybrid tracking     (Default: 0.1)   
```

```lang-none

  --hybridPresmooth <Double>      specify an pre-smoothing bandwidth (before projection)   
```

```lang-none

  --hybridProjAngle <Double>      specify an angle for hybrid projection (Default: 45.0)   
```

```lang-none

  --hybridProjNorm <Double>      specify a minimum norm for hybrid projection (Default: 0.01)   
```

```lang-none

  --hybridProjFrac <Double>      specify a minimum compartment fraction for hybrid projection (Default:     0.025)   
```

```lang-none

  --hybridProjFsum <Double>      specify a minimum total fraction for hybrid projection (Default: 0.05)   
```

```lang-none

  --hybridPostsmooth <Double>      specify an post-smoothing bandwidth (after projection)   
```

```lang-none

  --hybridInterp <KernelInterpolationType>      the interpolation type for the first stage of hybrid tracking (Options:     Nearest, Trilinear, Gaussian) (Default: Nearest)   
```

```lang-none

  --gforce <Double>      the prior gain forces (larger values have smoother effects and zero is hard     selection) (Default: 0.0)   
```

```lang-none

  --model <String>      a model name (default will try to detect it)   
```

```lang-none

  --threads <int>      the number of threads (Default: 3)   
```

```lang-none

  --max <Double>      a maximum value for tracking (FA for dti, frac for xfib/spharm, ficvf for     noddi)   
```

```lang-none

  --mixing <Double>      the mixing weight for orientation updates (0 to 1) (Default: 1.0)   
```

```lang-none

  --arclen <Double>      an arclength containment threshold (Default: 0.8)   
```

```lang-none

  --reach <Double>      only allow tracking to travel a given reach distance from the seed   
```

```lang-none

  --rk       use fourth-order Runge-Kutta integration (default is Euler)   
```

```lang-none

  --binarize       binarize masks (ignore multiple labels in include and other masks)   
```

### Required Output Arguments:

```lang-none

  --output <Curves>      the output tractography curves   
```

### Author:

  Ryan Cabeen 

### Citation:

  Cabeen, R. P., Bastin, M. E., & Laidlaw, D. H. (2016). Kernel regression 
  estimation of fiber orientation mixtures in diffusion MRI. NeuroImage, 127, 
  158-172. 

<hr/>
## VolumeMosaic 

### Description:

  Create a mosaic from an image volume.  This will take a 3D volume and create 
  a single slice that shows the whole dataset.  This is mainly useful for 
  quality assessment. 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Required Parameter Arguments:

```lang-none

  --output <String>      output filename to save mosaic   
```

### Optional Parameter Arguments:

```lang-none

  --axis <VolumeMosaicAxis>      choose an axis for slicing (Options: i, j, k) (Default: i)   
```

```lang-none

  --crop <String>      a string for cropping the volume, e.g. :,:,10:2:17 would show slices 10,     12, 14, and 16 etc.   
```

```lang-none

  --rgb       treat the input as RGB   
```

```lang-none

  --enhance       enhance the contrast   
```

```lang-none

  --min <Double>      use a linear colormap with the given fixed minimum   
```

```lang-none

  --max <Double>      use a linear colormap with the given fixed maximum   
```

```lang-none

  --multichannel       save out multichannel slices (the output filename must include '%d' if you     use this option)   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeNiftiCopyHeader 

### Description:

  Copy the image data of the input image into a reference volume's nifti header 

### Required Parameter Arguments:

```lang-none

  --input <String>      the filename of the input volume (the source of data)   
```

```lang-none

  --ref <String>      the filename of input reference volume (for providing the header)   
```

```lang-none

  --output <String>      the filename of the output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeNiftiPrintHeader 

### Description:

  Print the header of the given nifti file (this is faster than other modules 
  because it doesn't read the image data) 

### Required Parameter Arguments:

```lang-none

  --input <String>      the filename of the input volume   
```

### Optional Parameter Arguments:

```lang-none

  --table       write the summary as a table   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeNiftiRead 

### Description:

  Load a nifti in a non-standard way, e.g. setting certain header fields before 
  loading the image data 

### Required Parameter Arguments:

```lang-none

  --input <String>      the filename of the input nifti volume   
```

### Optional Parameter Arguments:

```lang-none

  --scl_slope <Double>      set the scl_slope field to a certain value   
```

```lang-none

  --scl_inter <Double>      set the scl_inter field to a certain value   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the filename of the output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeNiftiVoxelToWorld 

### Description:

  Extract the affine transform between voxel and world coordinates (note: this 
  is designed for nifti volumes that have some arbitrary voxel ordering) 

### Required Input Arguments:

```lang-none

  --input <Volume>      input Volume   
```

### Optional Parameter Arguments:

```lang-none

  --noscale       skip the scaling   
```

```lang-none

  --notrans       skip the translation   
```

```lang-none

  --norot       skip the rotation   
```

### Required Output Arguments:

```lang-none

  --output <Affine>      output affine   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeNoddiAbtin 

### Description:

  Extract ABTIN (ABsolute TIssue density from NODDI) parameters from a noddi 
  volume.  Port from: https://github.com/sepehrband/ABTIN/blob/master/ABTIN.m 

### Required Input Arguments:

```lang-none

  --input <Volume>      input noddi volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      the mask   
```

### Optional Parameter Arguments:

```lang-none

  --alpha <double>      the alpha parameter (see theory section of paper) (Default: 25.0)   
```

```lang-none

  --gratio <double>      the alpha parameter (see theory section of paper) (Default: 0.7)   
```

### Required Output Arguments:

```lang-none

  --outputMylDen <Volume>      output myelin density volume   
```

```lang-none

  --outputCSFDen <Volume>      output CSF density volume   
```

```lang-none

  --outputFibDen <Volume>      output fiber density volume   
```

```lang-none

  --outputCelDen <Volume>      output cellular density volume   
```

### Author:

  Ryan Cabeen 

### Citation:

  Sepehrband, F., Clark, K. A., Ullmann, J. F.P., Kurniawan, N. D., Leanage, 
  G., Reutens, D. C. and Yang, Z. (2015), Brain tissue compartment density 
  estimated using diffusion-weighted MRI yields tissue parameters consistent 
  with histology. Hum. Brain Mapp.. doi: 10.1002/hbm.22872 Link: 
  http://onlinelibrary.wiley.com/doi/10.1002/hbm.22872/abstract 

<hr/>
## VolumeNoddiBlendExperiment 

### Description:

  run a noddi blending experiment 

### Required Input Arguments:

```lang-none

  --gradients <Gradients>      specify gradientss   
```

### Optional Parameter Arguments:

```lang-none

  --size <Integer>      specify a size (Default: 21)   
```

```lang-none

  --abase <Double>      specify a noddi parameter (Default: 1)   
```

```lang-none

  --bbase <Double>      specify a noddi parameter (Default: 1)   
```

```lang-none

  --aicvf <Double>      specify a noddi parameter (Default: 0.70)   
```

```lang-none

  --bicvf <Double>      specify a noddi parameter (Default: 0.70)   
```

```lang-none

  --aisovf <Double>      specify a noddi parameter (Default: 0.01)   
```

```lang-none

  --bisovf <Double>      specify a noddi parameter (Default: 0.01)   
```

```lang-none

  --aod <Double>      specify a noddi parameter (Default: 0.2)   
```

```lang-none

  --bod <Double>      specify a noddi parameter (Default: 0.2)   
```

```lang-none

  --angle <Double>      specify an angle between models in degrees (Default: 90)   
```

### Required Output Arguments:

```lang-none

  --output <Directory>      specify an output directory   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeNoddiCortex 

### Description:

  Estimate cortical gray matter noddi parameters 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

```lang-none

  --mesh <Mesh>      input mesh   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --interp <KernelInterpolationType>      the interpolation type (Options: Nearest, Trilinear, Gaussian) (Default:     Trilinear)   
```

```lang-none

  --estimation <String>      specify an estimation method (Default: Component)   
```

```lang-none

  --support <Integer>      the filter radius in voxels (Default: 3)   
```

```lang-none

  --hpos <Double>      the positional bandwidth in mm (Default: 1.0)   
```

```lang-none

  --pial <String>      pial attribute name (Default: pial)   
```

```lang-none

  --white <String>      white attribute name (Default: white)   
```

```lang-none

  --samples <int>      the number of points to sample (Default: 15)   
```

```lang-none

  --inner <double>      the inner search buffer size (Default: 0.5)   
```

```lang-none

  --outer <double>      the outer search buffer size (Default: 0.5)   
```

```lang-none

  --maxiso <Double>      the maximum isotropic volume fraction (Default: 0.5)   
```

```lang-none

  --zscore <Double>      the maximum isotropic volume fraction (Default: 3.0)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      the output mesh   
```

### Optional Output Arguments:

```lang-none

  --weights <Volume>      the output weights   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeNoddiFit 

### Description:

  Fit a noddi volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      input diffusion-weighted MR volume   
```

```lang-none

  --gradients <Gradients>      the gradients   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      the mask   
```

### Optional Parameter Arguments:

```lang-none

  --method <VolumeNoddiFitMethod>      the method for fitting (Options: SmartStart, FullSMT, SMT, FastSMT, NLLS,     Grid) (Default: FullSMT)   
```

```lang-none

  --dpar <Double>      the parallel diffusivity (change for ex vivo) (Default: 0.0017)   
```

```lang-none

  --diso <Double>      the isotropic diffusivity (change for ex vivo) (Default: 0.003)   
```

```lang-none

  --dot       include a dot compartment (set for ex vivo)   
```

```lang-none

  --shells <String>      specify a subset of gradient shells to include (comma-separated list of     b-values)   
```

```lang-none

  --which <String>      specify a subset of gradients to include (comma-separated list of indices     starting from zero)   
```

```lang-none

  --exclude <String>      specify a subset of gradients to exclude (comma-separated list of indices     starting from zero)   
```

```lang-none

  --threads <Integer>      the number of threads in the pool (Default: 1)   
```

```lang-none

  --columns       use fine-grained multi-threading   
```

```lang-none

  --maxiter <Integer>      specify a maximum number of iterations for non-linear optimization     (Default: 100)   
```

```lang-none

  --rhobeg <double>      specify the starting rho parameter for non-linear optimization (Default:     0.1)   
```

```lang-none

  --rhoend <double>      specify the ending rho parameter for non-linear optimization (Default:     1.0E-4)   
```

```lang-none

  --prior <Double>      specify the prior for regularizing voxels with mostly free water, a value     between zero and one that indicates substantial free water, e.g. 0.95)   
```

### Advanced Parameter Arguments:

```lang-none

  --shells <String>      specify a subset of gradient shells to include (comma-separated list of     b-values)   
```

```lang-none

  --which <String>      specify a subset of gradients to include (comma-separated list of indices     starting from zero)   
```

```lang-none

  --exclude <String>      specify a subset of gradients to exclude (comma-separated list of indices     starting from zero)   
```

```lang-none

  --maxiter <Integer>      specify a maximum number of iterations for non-linear optimization     (Default: 100)   
```

```lang-none

  --rhobeg <double>      specify the starting rho parameter for non-linear optimization (Default:     0.1)   
```

```lang-none

  --rhoend <double>      specify the ending rho parameter for non-linear optimization (Default:     1.0E-4)   
```

```lang-none

  --prior <Double>      specify the prior for regularizing voxels with mostly free water, a value     between zero and one that indicates substantial free water, e.g. 0.95)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output noddi volume (name output like *.noddi and an directory of volumes     will be created)   
```

### Author:

  Ryan Cabeen 

<hr/>
<hr/>
<hr/>
## VolumeNoddiFuse 

### Description:

  fuse a collection of noddi volumes 

### Required Input Arguments:

```lang-none

  --input <Volume(s)>      specify the input noddi volumes   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      specify a mask   
```

### Optional Parameter Arguments:

```lang-none

  --pattern <String(s)>      specify a list of names that will be substituted with input %s   
```

```lang-none

  --estimation <String>      specify a type of estimation (Default: Component)   
```

```lang-none

  --fractions       use volume fractions in estimation   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      specify the output noddi volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeNoddiSmooth 

### Description:

  Smooth a noddi volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input Noddi volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --support <Integer>      the filter radius in voxels (Default: 3)   
```

```lang-none

  --hpos <Double>      the positional bandwidth in mm (Default: 1.0)   
```

```lang-none

  --hdir <Double>      the directionally adaptive bandwidth (only used with adaptive flag)   
```

```lang-none

  --hsig <Double>      the baseline signal adaptive bandwidth   
```

```lang-none

  --estimation <String>      specify an estimation method (Default: Scatter)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output noddi volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeNoddiTransform 

### Description:

  Transform a noddi volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input noddi volume   
```

```lang-none

  --reference <Volume>      input reference volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask (defined in the reference space)   
```

```lang-none

  --inputMask <Mask>      use an input mask (defined in the input space)   
```

```lang-none

  --affine <Affine>      apply an affine xfm   
```

```lang-none

  --invaffine <Affine>      apply an inverse affine xfm   
```

```lang-none

  --deform <Deformation>      apply a deformation xfm   
```

### Optional Parameter Arguments:

```lang-none

  --reverse       reverse the order, i.e. compose the affine(deform(x)), whereas the default     is deform(affine(x))   
```

```lang-none

  --reorient <ReorientationType>      specify a reorient method (Options: FiniteStrain, Jacobian) (Default:     Jacobian)   
```

```lang-none

  --interp <KernelInterpolationType>      the interpolation type (Options: Nearest, Trilinear, Gaussian) (Default:     Trilinear)   
```

```lang-none

  --support <Integer>      the filter radius in voxels (Default: 3)   
```

```lang-none

  --hpos <Double>      the positional bandwidth in mm (Default: 1.0)   
```

```lang-none

  --estimation <String>      specify an estimation method (Default: Component)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output transformed noddi volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeNoddiZoom 

### Description:

  Zoom a noddi volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input noddi volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --factor <double>      a zooming factor (Default: 2.0)   
```

```lang-none

  --interp <KernelInterpolationType>      the interpolation type (Options: Nearest, Trilinear, Gaussian) (Default:     Trilinear)   
```

```lang-none

  --support <Integer>      the filter radius in voxels (Default: 3)   
```

```lang-none

  --hpos <Double>      the positional kernel bandwidth in mm (Default: 2.0)   
```

```lang-none

  --hval <Double>      the data kernel bandwidth   
```

```lang-none

  --hsig <Double>      the signal kernel bandwidth   
```

```lang-none

  --estimation <String>      specify an estimation method (Default: Scatter)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output noddi volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeNoise 

### Description:

  Add synthetic noise to a volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --sigma <double>      noise level (Default: 1.0)   
```

```lang-none

  --rician       use rician distributed noise   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeNormalize 

### Description:

  Normalize the values of a volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --type <VolumeNormalizeType>      type of normalization (Options: Unit, UnitMax, UnitSum, UnitMean, Mean,     UnitMaxFraction, UnitMeanFraction, Gaussian) (Default: UnitMax)   
```

```lang-none

  --fraction <double>      a fraction for normalization (only applies to some types of normalization)     (Default: 0.5)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeOdfFeature 

### Description:

  Estimate spherical harmonics representing the ODF.  SUpported features 
  include: generalized fractional anisotropy (GFA) and normalized entropy (NE) 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input ODF volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --name <String>      the name of the feature (GFA, NE) (Default: GFA)   
```

```lang-none

  --threads <Integer>      the number of threads in the pool (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output spharm volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeOdfFit 

### Description:

  Fit an orientation distribution function (ODF) using spherical deconvolution 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input dwi   
```

```lang-none

  --gradients <Gradients>      the gradients   
```

### Optional Input Arguments:

```lang-none

  --points <Vects>      the input odf directions (if you don't provide this, you they will be     generated and returned in outpoints)   
```

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --shells <String>      specify a subset of gradient shells to include (comma-separated list of     b-values)   
```

```lang-none

  --which <String>      specify a subset of gradients to include (comma-separated list of indices     starting from zero)   
```

```lang-none

  --exclude <String>      specify a subset of gradients to exclude (comma-separated list of indices     starting from zero)   
```

```lang-none

  --num <int>      the number of points to use if you need to generate spherical points     (Default: 300)   
```

```lang-none

  --iters <int>      the number of iterations for deconvolution (Default: 500)   
```

```lang-none

  --alpha <double>      the kernel diffusivity for deconvolution (Default: 0.0017)   
```

```lang-none

  --beta <double>      the kernel radial diffusivity for deconvolution (Default: 0.0)   
```

```lang-none

  --threads <Integer>      the number of threads in the pool (Default: 1)   
```

### Advanced Parameter Arguments:

```lang-none

  --shells <String>      specify a subset of gradient shells to include (comma-separated list of     b-values)   
```

```lang-none

  --which <String>      specify a subset of gradients to include (comma-separated list of indices     starting from zero)   
```

```lang-none

  --exclude <String>      specify a subset of gradients to exclude (comma-separated list of indices     starting from zero)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output ODF volume   
```

### Optional Output Arguments:

```lang-none

  --outpoints <Vects>      the output odf directions (only relevant if you didn't provide a specific     set)   
```

### Author:

  Ryan Cabeen 

### Citation:

  Dell'Acqua, F., Scifo, P., Rizzo, G., Catani, M., Simmons, A., Scotti, G., & 
  Fazio, F. (2010). A modified damped Richardson-Lucy algorithm to reduce 
  isotropic background effects in spherical deconvolution. Neuroimage, 49(2), 
  1446-1458. 

<hr/>
## VolumeOdfPeaks 

### Description:

  Extract the peaks from an odf volume.  This finds the average direction of 
  local maxima clustered by hierarchical clustering.  The output fibers will 
  encode the peak ODF value in place of volume fraction. 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input ODF volume   
```

```lang-none

  --points <Vects>      the odf directions   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --comps <Integer>      the maximum number of comps (Default: 3)   
```

```lang-none

  --gfa       scaleCamera peak values by generalized fractional anisotropy after     extraction   
```

```lang-none

  --thresh <Double>      the minimum peak threshold (Default: 0.01)   
```

```lang-none

  --mode <PeakMode>      the mode for extracting the peak fraction from the lobe (Options: Sum,     Mean, Max) (Default: Mean)   
```

```lang-none

  --cluster <Double>      the minimum angle in degrees for hierarchical clustering of local maxima     (Default: 25.0)   
```

```lang-none

  --match       match the ODF sum   
```

```lang-none

  --threads <Integer>      the number of threads in the pool (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output fibers volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeOdfResample 

### Description:

  Resample an ODF using spherical harmonics.  Using a lower maximum order will 
  lead to smoother resamplings 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input ODF volume   
```

```lang-none

  --dirs <Vects>      the input odf directions   
```

```lang-none

  --resample <Vects>      the directions to resample   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --order <Integer>      the maximum spherical harmonic order used for resampling (Default: 8)   
```

```lang-none

  --threads <Integer>      the number of threads in the pool (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output ODF volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeOdfSpharm 

### Description:

  Estimate spherical harmonics representing the ODF 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input ODF volume   
```

```lang-none

  --dirs <Vects>      the odf directions   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --order <Integer>      the maximum spherical harmonic order (Default: 8)   
```

```lang-none

  --threads <Integer>      the number of threads in the pool (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output spharm volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeOrigin 

### Description:

  set the origin of a volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input volume   
```

### Optional Parameter Arguments:

```lang-none

  --x <double>      the origin in x (Default: 0.0)   
```

```lang-none

  --y <double>      the origin in y (Default: 0.0)   
```

```lang-none

  --z <double>      the origin in z (Default: 0.0)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumePad 

### Description:

  Pad a volume by a given number of voxels 

### Required Input Arguments:

```lang-none

  --input <Volume>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --pad <int>      the amount of padding in voxels (Default: 10)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeParseEchoes 

### Description:

  fuse volumes 

### Required Input Arguments:

```lang-none

  --input <Volume(s)>      the input volumes   
```

### Optional Output Arguments:

```lang-none

  --output-volume <Volume>      specify the output volume   
```

```lang-none

  --output-echoes <Text>      specify the output echo times   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeParticles 

### Description:

  Create particles representing a volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Required Parameter Arguments:

```lang-none

  --output <String>      output filename (should end in csv)   
```

### Optional Parameter Arguments:

```lang-none

  --samples <int>      at most, produce this many samples per voxel (Default: 4)   
```

```lang-none

  --adaptive       sample particles adaptively, that is, most particles with higher image     intensities   
```

```lang-none

  --thresh <double>      specify a threshold for background removal, e.g. 0.01 (based on input image     intensities) (Default: 0.0)   
```

```lang-none

  --type <VolumeEnhanceContrastType>      use the given type of contrast enhancement to normalize the image (Options:     Histogram, Ramp, RampGauss, Mean, None) (Default: Ramp)   
```

```lang-none

  --interp <InterpolationType>      use the given image interpolation method (Options: Nearest, Trilinear,     Tricubic, Gaussian, GaussianLocalLinear, GaussianLocalQuadratic) (Default:     Nearest)   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumePerturbDeform 

### Description:

  Perturb a volume morphometry for data augmentation 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --deform <Mask>      apply a deform in the given region   
```

### Optional Parameter Arguments:

```lang-none

  --flip <Double>      specify the probability of a flip perturbation (zero to one)   
```

```lang-none

  --angle <Double>      specify the sampling standard deviation of the angular perturbation     (degrees)   
```

```lang-none

  --scale <Double>      specify the sampling standard deviation of the total scaling perturbation     (added to a scaling factor of one)   
```

```lang-none

  --aniso <Double>      specify the sampling standard deviation of the anisotropic scaling     perturbation (added to an anisotropic scaling of one)   
```

```lang-none

  --shear <Double>      specify the sampling standard deviation of the anisotropic shear     perturbation   
```

```lang-none

  --shift <Double>      specify the sampling standard deviation of the translational shift     perturbation   
```

```lang-none

  --deformEffect <Double>      specify the amount of deform (Default: 3.0)   
```

```lang-none

  --deformExtent <Double>      specify the extent of deform (Default: 10.0)   
```

```lang-none

  --deformIters <Integer>      specify the number of deformation integration iterations (Default: 10)   
```

```lang-none

  --deformRandomize <Double>      randomize the amount of deform with the given sample standard deviation     (Default: 0.0)   
```

### Optional Output Arguments:

```lang-none

  --outputVolume <Volume>      output volume   
```

```lang-none

  --outputDeform <Deformation>      output deformation field   
```

```lang-none

  --outputAffine <Affine>      output affine transform   
```

```lang-none

  --outputTable <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumePerturbSignal 

### Description:

  Perturb the volume signal for data augmentation 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --blend <Volume>      input blending volume   
```

### Optional Parameter Arguments:

```lang-none

  --blendMean <Double>      specify the blending factor sampling mean value (Default: 0.0)   
```

```lang-none

  --blendStd <Double>      specify the averaging factor sampling standard deviation (Default: 0.0)   
```

```lang-none

  --noise <Double>      specify the sampling standard deviation of the intensity noise perturbation     (Default: 0.0)   
```

```lang-none

  --biasMean <Double>      specify the sampling mean of the bias field perturbation (Default: 0.0)   
```

```lang-none

  --biasStd <Double>      specify the sampling standard deviation of the bias field perturbation     (Default: 0.0)   
```

```lang-none

  --contrastMean <Double>      specify the sampling mean of the contrast perturbation (zero is no change)     (Default: 0.0)   
```

```lang-none

  --contrastStd <Double>      specify the sampling standard deviation of the contrast perturbation (zero     is no change) (Default: 0.0)   
```

### Optional Output Arguments:

```lang-none

  --outputVolume <Volume>      output volume   
```

```lang-none

  --outputTable <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumePhantomBox 

### Description:

  Create a simple box phantom 

### Optional Parameter Arguments:

```lang-none

  --width <int>      image width (Default: 100)   
```

```lang-none

  --height <int>      image height (Default: 100)   
```

```lang-none

  --slices <int>      image slices (Default: 1)   
```

```lang-none

  --size <double>      box relative dimensions (Default: 0.5)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumePoseCopy 

### Description:

  Set the pose (orientation and origin) of a volume to match a reference volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input volume   
```

```lang-none

  --ref <Volume>      the reference volume (the pose will be copied from here)   
```

### Optional Parameter Arguments:

```lang-none

  --delta       copy the voxel spacing as well   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumePoseGet 

### Description:

  Get the pose (orientation and origin) of a volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input volume   
```

### Required Output Arguments:

```lang-none

  --output <Affine>      the output affine xfm   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumePoseSet 

### Description:

  Set the pose (orientation and origin) of a volume.  By default, the existing 
  origin and orientation will be removed 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input volume   
```

```lang-none

  --pose <Affine>      the affine pose (any shear will be removed)   
```

### Optional Parameter Arguments:

```lang-none

  --premult       compose the existing pose with the given one by premultiplying   
```

```lang-none

  --postmult       compose the existing pose with the given one by post multiplying   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumePrintInfo 

### Description:

  Print basic information about a volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input volume   
```

### Optional Parameter Arguments:

```lang-none

  --stats       print statistics   
```

```lang-none

  --nifti       print complete nifti header (only relevant if the input is nifti)   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeProjection 

### Description:

  Compute a projection of an image volume along one axis, for example a minimum 
  intensity projection 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --axis <VolumeProjectionAxis>      the axis for the projection (Options: i, j, k) (Default: k)   
```

```lang-none

  --type <VolumeProjectionType>      the type of statistic for the projection (Options: Min, Max, Mean, Sum,     Var, CV, Median, IQR) (Default: Mean)   
```

```lang-none

  --thin <Integer>      use thin projection, whereby groups of the given number of slices are     aggregated   
```

```lang-none

  --index <int>      when collapsing the volume to a single slice, use this image index for the     position (Default: 0)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output image slice   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumePrototype 

### Description:

  Crop a volume down to a smaller volume (only one criteria per run) 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Required Parameter Arguments:

```lang-none

  --dim <Integer>      the output volume dimension   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeReduce 

### Description:

  Reduce a multi-channel volume to a volume with fewer channels 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Parameter Arguments:

```lang-none

  --which <String>      the list of indices to select (comma separated zero-based indices)   
```

```lang-none

  --exclude <String>      exclude specific indices (comma separated zero-based indices)   
```

```lang-none

  --method <VolumeReduceType>      specify the reduction technique (Subset depends on which or exclude flags)     (Options: Subset, Mean, Min, Max, Sum, Var, Std, Gray, RGB) (Default: Mean)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeRegionMinima 

### Description:

  Mask a volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

```lang-none

  --regions <Mask>      a mask encoding regions   
```

```lang-none

  --lookup <Table>      a table encoding region names   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask restricting which voxels are checked   
```

### Optional Parameter Arguments:

```lang-none

  --minimum       retain the minimum value   
```

```lang-none

  --sample       retain the sample voxel indices   
```

```lang-none

  --index <String>      specify the lookup index field (Default: index)   
```

```lang-none

  --name <String>      specify the lookup name field (Default: name)   
```

```lang-none

  --min <String>      specify the output field name for the minimum value (Default: min)   
```

```lang-none

  --i <String>      specify the output field name for the sample i index (Default: i)   
```

```lang-none

  --j <String>      specify the output field name for the sample j index (Default: j)   
```

```lang-none

  --k <String>      specify the output field name for the sample k index (Default: k)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
<hr/>
<hr/>
## VolumeRegisterLinear 

### Description:

  Estimate an affine transform between two volumes 

### Required Input Arguments:

```lang-none

  --input <Volume>      input moving volume, which is registered to the reference volume   
```

```lang-none

  --ref <Volume>      the reference volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask used to exclude a portion of the input volume   
```

```lang-none

  --refmask <Mask>      a mask used to exclude a portion of the reference volume   
```

### Optional Parameter Arguments:

```lang-none

  --init <VolumeRegisterLinearInit>      the type of initialization (Options: World, Center, CenterSize) (Default:     Center)   
```

### Required Output Arguments:

```lang-none

  --output <Affine>      output transform   
```

### Author:

  Ryan Cabeen 

<hr/>
<hr/>
## VolumeRemoveNaN 

### Description:

  Remove NaN values 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --replace <Double>      the value to replace (Default: 0.0)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeRender 

### Description:

  Render a volume slice based on a colormap 

### Optional Input Arguments:

```lang-none

  --background <Volume>      input background   
```

```lang-none

  --foreground <Volume>      input foreground colored using a scalar colormap   
```

```lang-none

  --labels <Mask>      input labels colored using a discrete colormap   
```

```lang-none

  --fgmask <Mask>      input foreground mask   
```

```lang-none

  --bgmask <Mask>      input background mask   
```

### Optional Parameter Arguments:

```lang-none

  --range <String>      input slice range, e.g. :,:,80 (Default: all)   
```

```lang-none

  --bgmap <String>      background colormap (Default: grayscale)   
```

```lang-none

  --bglow <String>      background lower bound (supports statistics like min, max, etc) (Default:     0)   
```

```lang-none

  --bghigh <String>      background upper bound (supports statistics like min, max, etc) (Default:     1)   
```

```lang-none

  --fgmap <String>      scalar colormap for coloring the volume (Default: grayscale)   
```

```lang-none

  --fglow <String>      scalar lower bound (supports statistics like min, max, etc) (Default: 0)   
```

```lang-none

  --fghigh <String>      scalar upper bound (supports statistics like min, max, etc) (Default: 1)   
```

```lang-none

  --fgrlow <String>      scalar lower bound for range (0 to 1) (Default: 0)   
```

```lang-none

  --fgrhigh <String>      scalar upper bound for range (0 to 1) (Default: 1)   
```

```lang-none

  --discrete <String>      discrete colormap for coloring the label volume (Default: White)   
```

```lang-none

  --invert       invert colormap   
```

```lang-none

  --wash <double>      wash out colors (Default: 0.0)   
```

```lang-none

  --alpha <double>      blending of background with foreground (Default: 1.0)   
```

```lang-none

  --label <String>      a label for the colormap (Default: attribute)   
```

### Optional Output Arguments:

```lang-none

  --output <Volume>      output RGB volume rendering   
```

```lang-none

  --colormap <Volume>      output RGB colormap rendering   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeReorder 

### Description:

  Change the ordering of voxels in a volume.  You can flip and shift the 
  voxels.  Which shifting outside the field a view, the data is wrapped around. 
   Shifting is applied before flipping 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Parameter Arguments:

```lang-none

  --flipi       flip in i   
```

```lang-none

  --flipj       flip in j   
```

```lang-none

  --flipk       flip in k   
```

```lang-none

  --shifti <int>      shift in i (Default: 0)   
```

```lang-none

  --shiftj <int>      shift in j (Default: 0)   
```

```lang-none

  --shiftk <int>      shift in k (Default: 0)   
```

```lang-none

  --swapij       swap ij   
```

```lang-none

  --swapik       swap ik   
```

```lang-none

  --swapjk       swap jk   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeResample 

### Description:

  Resample a volume with a different voxel size 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input volume   
```

### Optional Parameter Arguments:

```lang-none

  --dx <double>      the voxel size in x (Default: 1.0)   
```

```lang-none

  --dy <double>      the voxel size in y (Default: 1.0)   
```

```lang-none

  --dz <double>      the voxel size in z (Default: 1.0)   
```

```lang-none

  --interp <InterpolationType>      image interpolation method (Options: Nearest, Trilinear, Tricubic,     Gaussian, GaussianLocalLinear, GaussianLocalQuadratic) (Default: Trilinear)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeResliceACPC 

### Description:

  Reslice a volume using ACPC alignment (anterior and posterior commissure).  
  You must provide a vects object with three points for the AC, PC, and another 
  midline position (order must match) 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

```lang-none

  --acpcmid <Vects>      a vects containing three points: the anterior commisure, the posterior     commissure, and any other midline point that is superior to the AC-PC axis   
```

### Optional Input Arguments:

```lang-none

  --bounds <Vects>      points that bound the region of interest (by default the whole volume will     be used)   
```

### Optional Parameter Arguments:

```lang-none

  --delta <Double>      the voxel spacing (by default it detects it from the input)   
```

```lang-none

  --interp <InterpolationType>      image interpolation method (Options: Nearest, Trilinear, Tricubic,     Gaussian, GaussianLocalLinear, GaussianLocalQuadratic) (Default: Trilinear)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeResliceAxis 

### Description:

  Reslice a volume along a given axis (specified by a vects) 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --vects <Vects>      input vects (should roughly lie on a straight line)   
```

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --delta <Double>      the voxel spacing (by default it detects it from the input)   
```

```lang-none

  --mode <VolumeResliceMode>      specify the target slice (Options: I, J, K) (Default: K)   
```

```lang-none

  --interp <InterpolationType>      image interpolation method (Options: Nearest, Trilinear, Tricubic,     Gaussian, GaussianLocalLinear, GaussianLocalQuadratic) (Default: Trilinear)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeReslicePlane 

### Description:

  Reslice a volume by a given plane (contained in a solids object) 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

```lang-none

  --plane <Solids>      input solids (should contain a plane)   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --delta <Double>      the voxel spacing (by default it detects it from the input)   
```

```lang-none

  --mode <VolumeResliceMode>      specify the target slice (Options: I, J, K) (Default: K)   
```

```lang-none

  --interp <InterpolationType>      image interpolation method (Options: Nearest, Trilinear, Tricubic,     Gaussian, GaussianLocalLinear, GaussianLocalQuadratic) (Default: Trilinear)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeSampleCortex 

### Description:

  Estimate cortical gray matter parameters 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

```lang-none

  --mesh <Mesh>      input mesh   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

```lang-none

  --gm <Volume>      input gray matter probability map   
```

### Optional Parameter Arguments:

```lang-none

  --name <String>      the name of the volumetric data (only used for naming mesh attributes)     (Default: data)   
```

```lang-none

  --pial <String>      pial attribute name (Default: pial)   
```

```lang-none

  --middle <String>      the middle attribute name (Default: middle)   
```

```lang-none

  --white <String>      white attribute name (Default: white)   
```

```lang-none

  --samples <int>      the number of points to sample (Default: 15)   
```

```lang-none

  --inner <double>      the inner search buffer size (Default: 0.0)   
```

```lang-none

  --outer <double>      the outer search buffer size (Default: 0.0)   
```

```lang-none

  --linear       use linear weights, e.g. 1 - abs(x)   
```

```lang-none

  --gain <double>      use the given weight gain, e.g. exp(-gain*x^2)) (Default: 12.0)   
```

```lang-none

  --zscore <Double>      the z-score for filtering (Default: 3.0)   
```

```lang-none

  --mest       use Tukey's biweight m-estimator for robust statistics   
```

### Optional Output Arguments:

```lang-none

  --output <Mesh>      the output mesh   
```

```lang-none

  --weights <Volume>      the output weights   
```

```lang-none

  --num <Vects>      the output num vects   
```

```lang-none

  --median <Vects>      the output mean vects   
```

```lang-none

  --mean <Vects>      the output mean vects   
```

```lang-none

  --std <Vects>      the output std vects   
```

```lang-none

  --min <Vects>      the output min vects   
```

```lang-none

  --max <Vects>      the output max vects   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeSampleLine 

### Description:

  Sample a volume along a polyline.  The results are stored in table along with 
  the world coordinates and position along the segment 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

```lang-none

  --vects <Vects>      input vects (two points in a vects object)   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --num <int>      the number of samples (Default: 5)   
```

```lang-none

  --interp <InterpolationType>      image interpolation method (Options: Nearest, Trilinear, Tricubic,     Gaussian, GaussianLocalLinear, GaussianLocalQuadratic) (Default: Trilinear)   
```

```lang-none

  --value <String>      the name of the volume value field (Default: value)   
```

```lang-none

  --dims <String>      a which of dimensions to use   
```

```lang-none

  --vector       include vector values   
```

```lang-none

  --vx <String>      the field for the x coordinate (Default: x)   
```

```lang-none

  --vy <String>      the field for the y coordinate (Default: y)   
```

```lang-none

  --vz <String>      the field for the z coordinate (Default: z)   
```

```lang-none

  --pos <String>      the position along the line segment in mm (Default: posCamera)   
```

```lang-none

  --idx <String>      the index along the line segment (Default: idx)   
```

### Advanced Parameter Arguments:

```lang-none

  --dims <String>      a which of dimensions to use   
```

```lang-none

  --vx <String>      the field for the x coordinate (Default: x)   
```

```lang-none

  --vy <String>      the field for the y coordinate (Default: y)   
```

```lang-none

  --vz <String>      the field for the z coordinate (Default: z)   
```

```lang-none

  --pos <String>      the position along the line segment in mm (Default: posCamera)   
```

```lang-none

  --idx <String>      the index along the line segment (Default: idx)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeSegmentCluster 

### Description:

  Segment a volume by clustering intensities (with k-means, dp-means, or 
  Gaussian mixtures) 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --iters <int>      a maximum number of iterations (Default: 10)   
```

```lang-none

  --num <int>      the number classes (inital guess in the case of dp-means) (Default: 3)   
```

```lang-none

  --restarts <int>      the number of restarts (Default: 1)   
```

```lang-none

  --lambda <Double>      a threshold for detecting the number of clusters in the DP-means algorithm   
```

```lang-none

  --bayesian       use a Bayesian probabilistic approach (MRF Gaussian mixture expectation     maximization framework)   
```

```lang-none

  --emIters <Integer>      use the following number of expectation maximization iteration (Default: 5)   
```

```lang-none

  --emCov <String>      specify a covariance type (spherical, diagonal, full) (Default: spherical)   
```

```lang-none

  --emReg <Double>      a regularization parameter added to the covariance (Default: 0.0)   
```

```lang-none

  --mrfIters <Integer>      use the following number of MRF optimization iterations (Default: 5)   
```

```lang-none

  --mrfCross       use a 6-neighborhood (instead of a full 27-neighborhood with diagonals)   
```

```lang-none

  --mrfGamma <Double>      use the following spatial regularization weight (Default: 1.0)   
```

```lang-none

  --mrfCrfGain <Double>      use the gain used for the conditional random field (zero will disable it)     (Default: 1.0)   
```

### Required Output Arguments:

```lang-none

  --labels <Mask>      the output label map   
```

```lang-none

  --membership <Volume>      the output membership map   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeSegmentForeground 

### Description:

  Segment the foreground of a volume using a statistical approach 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Parameter Arguments:

```lang-none

  --median <Integer>      the number of times to median prefilter (Default: 1)   
```

```lang-none

  --thresh <Double>      apply a threshold   
```

```lang-none

  --fill       fill the foreground mask to remove islands   
```

```lang-none

  --largest       extract the largest island   
```

```lang-none

  --mrf       apply markov random field spatial regularization   
```

```lang-none

  --mrfCross       use a 6-neighborhood (instead of a full 27-neighborhood with diagonals)   
```

```lang-none

  --mrfGamma <Double>      use the following spatial regularization weight (Default: 1.0)   
```

```lang-none

  --mrfCrfGain <Double>      use the gain used for the conditional random field (zero will disable it)     (Default: 1.0)   
```

```lang-none

  --mrfIcmIters <Integer>      use the following number of MRF optimization iterations (Default: 5)   
```

```lang-none

  --mrfEmIters <Integer>      use the following number of expectation maximization iteration (Default: 5)   
```

### Optional Output Arguments:

```lang-none

  --output <Mask>      the output mask   
```

```lang-none

  --report <Table>      the output report   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeSegmentForegroundOtsu 

### Description:

  Segment the foreground of a volume using the Otsu method 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --median <int>      apply a median filter this many times before segmenting (Default: 3)   
```

```lang-none

  --open <int>      open the mask with mask morphology (Default: 2)   
```

```lang-none

  --islands       keep islands in the segmentation (otherwise only the largest component is     kept)   
```

```lang-none

  --holes       keep holes in the segmentation (otherwise they will be filled)   
```

```lang-none

  --dilate <int>      dilate the segmentation at the very end (Default: 0)   
```

```lang-none

  --threads <int>      use this many threads (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      the output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeSegmentGraph 

### Description:

  Segment a volume using graph based segmentation 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input tensor volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --min <Integer>      a minima region size (Default: 10)   
```

```lang-none

  --threshold <Double>      a threshold for grouping (Default: 1.0)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      the output segmentation mask   
```

### Author:

  Ryan Cabeen 

### Citation:

  Felzenszwalb, P. F., & Huttenlocher, D. P. (2004). Efficient graph-based 
  image segmentation. International journal of computer vision, 59(2), 167-181. 
  Chicago 

<hr/>
## VolumeSegmentLocalExtrema 

### Description:

  Segment a volume based on local extrema 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --minima       find minima instead of maxima   
```

```lang-none

  --full       use a full 27-voxel neighborhood (default is 6-voxel)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output region labels   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeSegmentRegionGrowing 

### Description:

  Segment a volume using a statistical region-growing technique 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input volume   
```

```lang-none

  --seed <Mask>      the input seed mask   
```

### Optional Input Arguments:

```lang-none

  --include <Mask>      an option mask for including only specific voxels in the segmentation   
```

```lang-none

  --exclude <Mask>      an option for excluding voxels from the segmentation   
```

### Optional Parameter Arguments:

```lang-none

  --nbThresh <int>      the neighbor count threshold (Default: 0)   
```

```lang-none

  --dataThresh <Double>      the intensity stopping threshold (Default: 3.0)   
```

```lang-none

  --gradThresh <Double>      the gradient stopping threshold (Default: 3.0)   
```

```lang-none

  --maxiter <int>      the maxima number of iterations (Default: 10000)   
```

```lang-none

  --maxsize <double>      the maxima region volume (Default: 1.7976931348623157E308)   
```

```lang-none

  --full       use a full 27-voxel neighborhood (default is 6-voxel)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      the output segmentation   
```

### Author:

  Ryan Cabeen 

### Citation:

  Adams, R., & Bischof, L. (1994). Seeded region growing. IEEE Transactions on 
  pattern analysis and machine intelligence, 16(6), 641-647. 

<hr/>
## VolumeSegmentSuperKMeans 

### Description:

  Segment supervoxels from a volume using k-means.  Voxels are clustered based 
  on position and intensity 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --iters <int>      a maxima number of iterations (Default: 10)   
```

```lang-none

  --num <int>      the number regions (Default: 100)   
```

```lang-none

  --scale <double>      the scaleCamera for combining intensities with voxel positions (Default:     1.0)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      none   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeSegmentSuperSLIC 

### Description:

  Segment a volume to obtain SLIC supervoxels 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --iters <int>      a maximum number of iterations (Default: 500)   
```

```lang-none

  --error <double>      the convergence criteria (Default: 0.001)   
```

```lang-none

  --size <double>      the average size (along each dimension) for the supervoxels (Default: 20.0)   
```

```lang-none

  --scale <double>      the scaleCamera for intensities (Default: 1.0)   
```

```lang-none

  --merge <double>      a threshold region volume for merging (Default: 10.0)   
```

```lang-none

  --group <Double>      a threshold gradient magnitude for grouping   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      none   
```

### Author:

  Ryan Cabeen 

### Citation:

  Radhakrishna Achanta, Appu Shaji, Kevin Smith, Aurelien Lucchi, Pascal Fua, 
  and Sabine Susstrunk, SLIC Superpixels, EPFL Technical Report 149300, June 
  2010. 

<hr/>
## VolumeSetSampling 

### Description:

  Set the origin of a volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input volume   
```

### Optional Parameter Arguments:

```lang-none

  --starti <Double>      the origin in i   
```

```lang-none

  --startj <Double>      the origin in j   
```

```lang-none

  --startk <Double>      the origin in k   
```

```lang-none

  --deltai <Double>      the voxel spacing in i   
```

```lang-none

  --deltaj <Double>      the voxel spacing in j   
```

```lang-none

  --deltak <Double>      the voxel spacing in k   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeSetTable 

### Description:

  Create a table listing regions of a mask 

### Required Input Arguments:

```lang-none

  --reference <Volume>      input volume   
```

```lang-none

  --table <Table>      input table   
```

### Optional Parameter Arguments:

```lang-none

  --dim <int>      the channel (Default: 0)   
```

```lang-none

  --index <String>      the index field name (Default: index)   
```

```lang-none

  --value <String>      a field to set (Default: value)   
```

```lang-none

  --background <double>      a background value (Default: 0.0)   
```

```lang-none

  --missing <Double>      a missing value   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeSkeletonize 

### Description:

  Extract a skeleton from the local maxima and ridges of a volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --threshold <Double>      exclude voxels with an input intensity below this value   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output skeleton   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeSlice 

### Description:

  Threshold a volume to make a mask 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Parameter Arguments:

```lang-none

  --slice <int>      a slice number (Default: 0)   
```

```lang-none

  --dim <String>      a slice channel (x, y, or z) (Default: z)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeSliceMesh 

### Description:

  Create a mesh from image slices 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --attribute <String>      specify a name for the attribute (Default: data)   
```

```lang-none

  --range <String>      specify a range of slices to extract (or a specific slice) (Default:     start:25:end)   
```

```lang-none

  --axis <VolumeSliceMeshAxis>      the slice axis (Options: i, j, k) (Default: k)   
```

```lang-none

  --nobg       remove background voxels   
```

```lang-none

  --thresh <double>      specify a threshold for background removal (Default: 1.0E-6)   
```

```lang-none

  --color       add vertex colors   
```

```lang-none

  --normalize       use min/max normalization   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      output mesh   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeSmtFit 

### Description:

  Estimate microscopic diffusion parameters 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input diffusion-weighted MR volume   
```

```lang-none

  --gradients <Gradients>      the input gradients   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      the input mask   
```

### Optional Parameter Arguments:

```lang-none

  --threads <int>      the input number of threads (Default: 1)   
```

```lang-none

  --maxiter <int>      the maximum number of iterations (Default: 5000)   
```

```lang-none

  --rhobeg <double>      the starting simplex size (Default: 0.1)   
```

```lang-none

  --rhoend <double>      the ending simplex size (Default: 1.0E-6)   
```

```lang-none

  --dperp <double>      the initial guess for perpendicular diffusivity (Default: 5.0E-4)   
```

```lang-none

  --dpar <double>      the initial guess for parallel diffusivity (Default: 0.0017)   
```

```lang-none

  --dmax <double>      the maximum diffusivity (Default: 0.003)   
```

### Advanced Parameter Arguments:

```lang-none

  --maxiter <int>      the maximum number of iterations (Default: 5000)   
```

```lang-none

  --rhobeg <double>      the starting simplex size (Default: 0.1)   
```

```lang-none

  --rhoend <double>      the ending simplex size (Default: 1.0E-6)   
```

```lang-none

  --dperp <double>      the initial guess for perpendicular diffusivity (Default: 5.0E-4)   
```

```lang-none

  --dpar <double>      the initial guess for parallel diffusivity (Default: 0.0017)   
```

```lang-none

  --dmax <double>      the maximum diffusivity (Default: 0.003)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output parameter map   
```

### Author:

  Ryan Cabeen 

### Citation:

  Kaden, E., Kruggel, F., & Alexander, D. C. (2016). Quantitative mapping of 
  the per-axon diffusion coefficients in brain white matter. Magnetic resonance 
  in medicine, 75(4), 1752-1763. 

<hr/>
## VolumeSort 

### Description:

  Sort two volumes based on their mean intensity 

### Required Input Arguments:

```lang-none

  --a <Volume>      input volume a   
```

```lang-none

  --b <Volume>      input volume b   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Required Output Arguments:

```lang-none

  --outputLow <Volume>      output low volume   
```

```lang-none

  --outputHigh <Volume>      output high volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeSortChannels 

### Description:

  Sort the channels of a multi-channel volumes based on their mean intensity 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask   
```

### Optional Parameter Arguments:

```lang-none

  --reverse       reverse the order (output will be decreasing)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output   
```

### Author:

  Ryan Cabeen 

<hr/>
<hr/>
## VolumeSpharmODF 

### Description:

  Sample an orientation distribution function (ODF) from a spharm volume. 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input spharm volume   
```

### Optional Input Arguments:

```lang-none

  --points <Vects>      the sphere points for estimation   
```

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --num <Integer>      the number of points to use if you need to generate spherical points     (Default: 300)   
```

```lang-none

  --threads <Integer>      the number of threads in the pool (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output odf volume   
```

### Optional Output Arguments:

```lang-none

  --outpoints <Vects>      the output odf directions (only relevant if you didn't provide a specific     set)   
```

### Author:

  Ryan Cabeen, Dogu Baran Aydogan 

<hr/>
## VolumeSpharmPeaks 

### Description:

  Extract the peaks from a spherical harmonic volume.  This finds the average 
  direction of local maxima clustered by hierarchical clustering.  The output 
  fibers will encode the peak q-value in place of volume fraction. 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input spharm volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --comps <Integer>      the maximum number of comps (Default: 4)   
```

```lang-none

  --thresh <Double>      the minimum peak threshold (Default: 0.1)   
```

```lang-none

  --detail <Integer>      the level of detail for spherical harmonic sampling (Default: 5)   
```

```lang-none

  --mode <PeakMode>      the mode for extracting the peak fraction from the lobe (Options: Sum,     Mean, Max) (Default: Mean)   
```

```lang-none

  --cluster <Double>      the minimum angle in degrees for hierarchical clustering of local maxima     (Default: 25.0)   
```

```lang-none

  --match       match the ODF sum   
```

```lang-none

  --threads <Integer>      the number of threads in the pool (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output fibers volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeSplit 

### Description:

  Split a multi-channel volume into multiple seperate volumes 

### Required Input Arguments:

```lang-none

  --input <File>      specify an input multi-channel volume   
```

### Optional Parameter Arguments:

```lang-none

  --which <Integers(s)>      specify a subset of channels to extract   
```

### Required Output Arguments:

```lang-none

  --output <Pattern>      specify the output filename pattern with %d (will correspond to the channel     number)   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeStackParticles 

### Description:

  Extract particles from an image stack.  This is meant to process datasets 
  that are too big to be fully loaded into memory.  This will process one slice 
  at a time and save the results to disk in the process. 

### Optional Parameter Arguments:

```lang-none

  --input <String>      input filename pattern (e.g. %04d will be replaced with 0000, 0001, etc. or     e.g. %d will be replaced with 0, 1, etc.) (Default: slice%04d.tif)   
```

```lang-none

  --isize <double>      the voxel size in the i direction (Default: 1.0)   
```

```lang-none

  --jsize <double>      the voxel size in the j direction (Default: 1.0)   
```

```lang-none

  --ksize <double>      the voxel size in the k direction (Default: 1.0)   
```

```lang-none

  --istart <int>      start at the given pixel in i when reading the volume (Default: 0)   
```

```lang-none

  --jstart <int>      start at the given pixel in j when reading the volume (Default: 0)   
```

```lang-none

  --kstart <int>      start at the given pixel in k when reading the volume (Default: 0)   
```

```lang-none

  --istep <int>      step the given number of pixels in i when reading the volume (Default: 1)   
```

```lang-none

  --jstep <int>      step the given number of pixels in j when reading the volume (Default: 1)   
```

```lang-none

  --kstep <int>      step the given number of pixels in k when reading the volume (Default: 1)   
```

```lang-none

  --samples <Double>      the number of particle samples per unit intensity (e.g. if the image     intensity is one, it will produce this many particles) (Default: 8.0)   
```

```lang-none

  --min <double>      specify a minimum window intensity (all voxels below this intensity are     ignored) (Default: 0.25)   
```

```lang-none

  --max <double>      specify a maximum window intensity (all voxels above this intensity are     sampled uniformly) (Default: 0.75)   
```

```lang-none

  --empty       keep empty particle slices   
```

```lang-none

  --dstart <int>      start at the given index when loading slices (Default: 0)   
```

```lang-none

  --dend <Integer>      end at the given index when loading slices   
```

```lang-none

  --dstep <int>      step the given amount when loading slices (Default: 1)   
```

```lang-none

  --interp <InterpolationType>      use the given image interpolation method (Options: Nearest, Trilinear,     Tricubic, Gaussian, GaussianLocalLinear, GaussianLocalQuadratic) (Default:     Nearest)   
```

```lang-none

  --output <String>      output filename of particles (should end in csv) (Default: output.csv)   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeStackProject 

### Description:

  Compute the maximum or minimum intensity projection of an image stack 

### Optional Parameter Arguments:

```lang-none

  --input <String>      input filename pattern (e.g. %04d will be replaced with 0000, 0001, etc. or     e.g. %d will be replaced with 0, 1, etc.) (Default: input/slice%04d.png)   
```

```lang-none

  --min       use a minimum intensity projection (default is maximum intensity     projection)   
```

```lang-none

  --mode <ProjectMode>      project slabs of a given stack fraction (only relevant to SlabFracZ)     (Options: FullZ, SlabZ, SlabFracZ, FullX, FullY) (Default: FullZ)   
```

```lang-none

  --statistic <StatisticMode>      the statistic to use for projection (Options: Max, Min, Mean, Sum)     (Default: Max)   
```

```lang-none

  --slabCount <Integer>      project slabs of a given thickness (only relevant to SlabZ) (Default: 100)   
```

```lang-none

  --slabStep <Integer>      use a given step amount (only relevant to the SlabZ) (Default: 100)   
```

```lang-none

  --slabFraction <Double>      project slabs of a given stack fraction (only relevant to SlabFracZ)     (Default: 0.1)   
```

```lang-none

  --xstart <int>      start at the given index projecting pixels in x (Default: 0)   
```

```lang-none

  --xend <Integer>      end at the given index projecting pixels in x   
```

```lang-none

  --ystart <int>      start at the given index projecting pixels in y (Default: 0)   
```

```lang-none

  --yend <Integer>      end at the given index projecting pixels in y   
```

```lang-none

  --dstart <int>      start at the given index when loading slices (Default: 0)   
```

```lang-none

  --dend <Integer>      end at the given index when loading slices   
```

```lang-none

  --dstep <int>      step the given amount when loading slices (Default: 1)   
```

```lang-none

  --output <String>      the output stack (use %d if you specify a slab) (Default: output.png)   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeStackRead 

### Description:

  Read a volume from an image stack 

### Optional Parameter Arguments:

```lang-none

  --input <String>      the input filename input for reading each image (must contain %d or some a     similar formatting charater for substituting the index) (Default: %03d.tif)   
```

```lang-none

  --isize <double>      the voxel size in the i direction (Default: 1.0)   
```

```lang-none

  --jsize <double>      the voxel size in the j direction (Default: 1.0)   
```

```lang-none

  --ksize <double>      the voxel size in the k direction (Default: 1.0)   
```

```lang-none

  --istart <int>      start at the given pixel in i when reading the volume (Default: 0)   
```

```lang-none

  --jstart <int>      start at the given pixel in j when reading the volume (Default: 0)   
```

```lang-none

  --kstart <int>      start at the given pixel in k when reading the volume (Default: 0)   
```

```lang-none

  --istep <int>      step the given number of pixels in i when reading the volume (Default: 1)   
```

```lang-none

  --jstep <int>      step the given number of pixels in j when reading the volume (Default: 1)   
```

```lang-none

  --kstep <int>      step the given number of pixels in k when reading the volume (Default: 1)   
```

```lang-none

  --dstart <int>      start at the given index when loading slices (Default: 0)   
```

```lang-none

  --dend <Integer>      end at the given index when loading slices   
```

```lang-none

  --dstep <int>      step the given amount when loading slices (Default: 1)   
```

```lang-none

  --smooth <Double>      apply Gaussian smoothing to each slice with the given bandwidth in pixels     (before downsampling)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeStackReadBlocks 

### Description:

  Extract particles from an image stack.  This is meant to process datasets 
  that are too big to be fully loaded into memory.  This will process one slice 
  at a time and save the results to disk in the process. 

### Optional Parameter Arguments:

```lang-none

  --input <String>      input block filename pattern (e.g. %04d will be replaced with 0000, 0001,     etc. or e.g. %d will be replaced with 0, 1, etc.) (Default:     /your/path/input/block%04d.nii.gz)   
```

```lang-none

  --ref <String>      reference image stack filename pattern (e.g. %04d will be replaced with     0000, 0001, etc. or e.g. %d will be replaced with 0, 1, etc.) (Default:     /your/path/ref/slice%04d.png)   
```

```lang-none

  --isize <int>      the block size in the i direction (Default: 256)   
```

```lang-none

  --jsize <int>      the block size in the j direction (Default: 256)   
```

```lang-none

  --ksize <int>      the voxel size in the k direction (Default: 64)   
```

```lang-none

  --overlap <int>      the amount of overlap in blocks (Default: 8)   
```

```lang-none

  --scale <double>      the scaling applied to the image intensities (Default: 1.0)   
```

```lang-none

  --dstart <int>      start at the given index when loading slices (Default: 0)   
```

```lang-none

  --dend <Integer>      end at the given index when loading slices   
```

```lang-none

  --dstep <int>      step the given amount when loading slices (Default: 1)   
```

```lang-none

  --output <String>      output image stack filename pattern (e.g. %04d will be replaced with 0000,     0001, etc. or e.g. %d will be replaced with 0, 1, etc.) (Default:     /your/path/output/slice%04d.png)   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeStackWrite 

### Description:

  Save the volume to an image stack 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Required Parameter Arguments:

```lang-none

  --output <String>      output directory to the stack   
```

### Optional Parameter Arguments:

```lang-none

  --axis <VolumeSaveStackAxis>      choose an axis for slicing (Options: i, j, k) (Default: k)   
```

```lang-none

  --range <String>      specify a range, e.g. '10:50,5:25,start:2:end'   
```

```lang-none

  --enhance       enhance the contrast   
```

```lang-none

  --pattern <String>      the output pattern for saving each image (Default: slice%04d.png)   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeStackWriteBlocks 

### Description:

  Extract particles from an image stack.  This is meant to process datasets 
  that are too big to be fully loaded into memory.  This will process one slice 
  at a time and save the results to disk in the process. 

### Optional Parameter Arguments:

```lang-none

  --input <String>      input image stack filename pattern (e.g. %04d will be replaced with 0000,     0001, etc. or e.g. %d will be replaced with 0, 1, etc.) (Default:     /your/path/slice%04d.tif)   
```

```lang-none

  --isize <int>      the block size in the i direction (Default: 256)   
```

```lang-none

  --jsize <int>      the block size in the j direction (Default: 256)   
```

```lang-none

  --ksize <int>      the voxel size in the k direction (Default: 64)   
```

```lang-none

  --overlap <int>      the amount of overlap in blocks (Default: 8)   
```

```lang-none

  --idelta <double>      the voxel size in the i direction (Default: 1.0)   
```

```lang-none

  --jdelta <double>      the voxel size in the j direction (Default: 1.0)   
```

```lang-none

  --kdelta <double>      the voxel delta in the k direction (Default: 1.0)   
```

```lang-none

  --dstart <int>      start at the given index when loading slices (Default: 0)   
```

```lang-none

  --dend <Integer>      end at the given index when loading slices   
```

```lang-none

  --dstep <int>      step the given amount when loading slices (Default: 1)   
```

```lang-none

  --output <String>      output block filename pattern (e.g. %04d will be replaced with 0000, 0001,     etc. or e.g. %d will be replaced with 0, 1, etc.) (Default:     /your/path/output%04d.nii.gz)   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeStandardize 

### Description:

  Standardize the orientation of a volume (no rotation and zero origin).  The 
  original pose can be saved to xfm. 

### Required Input Arguments:

```lang-none

  --input <Volume>      input Volume   
```

### Optional Output Arguments:

```lang-none

  --output <Volume>      output Volume   
```

```lang-none

  --xfm <Affine>      output affine   
```

```lang-none

  --invxfm <Affine>      output inverse affine   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeStatsPaired 

### Description:

  Compute pairwise statistics from volumetric data 

### Required Input Arguments:

```lang-none

  --matching <Table>      a table where each row stores a pair of matching subjects   
```

### Required Parameter Arguments:

```lang-none

  --pattern <String>      input volume filename pattern (should contain %s for the subject     identifier)   
```

### Optional Parameter Arguments:

```lang-none

  --left <String>      the left group in the pair (Default: left)   
```

```lang-none

  --right <String>      the right group in the pair (Default: right)   
```

### Optional Output Arguments:

```lang-none

  --outputLeftMean <Volume>      output left mean volume   
```

```lang-none

  --outputLeftStd <Volume>      output left standard deviation volume   
```

```lang-none

  --outputRightMean <Volume>      output right mean volume   
```

```lang-none

  --outputRightStd <Volume>      output right standard deviation volume   
```

```lang-none

  --outputDiff <Volume>      output difference in means   
```

```lang-none

  --outputCohenD <Volume>      output Cohen's-d effect size   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeStoreVoxelPosition 

### Description:

  Create a volume storing the 3D world coordinates of each voxel 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input volume   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeSubset 

### Description:

  Which the intensities of a volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Required Parameter Arguments:

```lang-none

  --which <String>      the list of indices to select   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeSurfacePlot 

### Description:

  Plot data from a planar image 

### Required Input Arguments:

```lang-none

  --input <Volume>      input   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --dimension <int>      the channel (Default: 0)   
```

```lang-none

  --sx <double>      the scaleCamera in x (Default: 1.0)   
```

```lang-none

  --sy <double>      the scaleCamera in y (Default: 1.0)   
```

```lang-none

  --sz <double>      the scaleCamera in z (Default: 1.0)   
```

### Required Output Arguments:

```lang-none

  --output <Mesh>      output   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeTable 

### Description:

  Record voxel values in a table 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

```lang-none

  --lookup <Table>      a lookup table for matching names to mask labels   
```

### Optional Parameter Arguments:

```lang-none

  --value <String>      value (Default: value)   
```

```lang-none

  --multiple       use multiple mask labels   
```

```lang-none

  --lookupNameField <String>      specify the lookup name field (Default: name)   
```

```lang-none

  --lookupIndexField <String>      specify the output voxel field name (Default: voxel)   
```

```lang-none

  --range <String>      a mask specification   
```

```lang-none

  --dims <String>      a which of dimensions to use   
```

```lang-none

  --vector       include vector values   
```

```lang-none

  --vi <String>      the field for the voxel i coordinate   
```

```lang-none

  --vj <String>      the field for the voxel j coordinate   
```

```lang-none

  --vk <String>      the field for the voxel k coordinate   
```

```lang-none

  --voxel <String>      the field for the voxel voxel (Default: voxel)   
```

### Advanced Parameter Arguments:

```lang-none

  --lookupNameField <String>      specify the lookup name field (Default: name)   
```

```lang-none

  --lookupIndexField <String>      specify the output voxel field name (Default: voxel)   
```

```lang-none

  --range <String>      a mask specification   
```

```lang-none

  --dims <String>      a which of dimensions to use   
```

```lang-none

  --vector       include vector values   
```

```lang-none

  --vi <String>      the field for the voxel i coordinate   
```

```lang-none

  --vj <String>      the field for the voxel j coordinate   
```

```lang-none

  --vk <String>      the field for the voxel k coordinate   
```

```lang-none

  --voxel <String>      the field for the voxel voxel (Default: voxel)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      output table   
```

### Author:

  Ryan Cabeen 

<hr/>
<hr/>
## VolumeTensorFilter 

### Description:

  Filter a tensor volume in a variety of ways, e.g. changing units, clamping 
  diffusivities, masking out regions 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input tensor volume   
```

### Optional Parameter Arguments:

```lang-none

  --scale <Double>      the scaleCamera to multiply diffusivities (typically needed to changed the     physical units)   
```

```lang-none

  --clamp <Double>      clamp the diffusivities (values below the threshold will be set to it)   
```

```lang-none

  --expression <String>      zero out voxels that do not satisfy the given arithmetic expression, e.g.     FA > 0.15 && MD < 0.001 && MD > 0.0001   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output tensor volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeTensorFit 

### Description:

  Fit a tensor volume to a diffusion-weighted MRI. 

### Required Input Arguments:

```lang-none

  --input <Volume>      input diffusion-weighted MR volume   
```

```lang-none

  --gradients <Gradients>      the gradients   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      the mask   
```

### Optional Parameter Arguments:

```lang-none

  --method <TensorFitType>      specify an estimation method (Options: LLS, WLLS, NLLS, FWLLS, FWWLLS,     FWNLLS, RESTORE) (Default: LLS)   
```

```lang-none

  --cost <CostType>      specify a cost function for non-linear fitting (Options: SE, MSE, RMSE,     NRMSE, CHISQ, RLL) (Default: SE)   
```

```lang-none

  --single       use the lowest single shell for tensor estimation (if used, this will skip     the shells, which, and exclude flags)   
```

```lang-none

  --bestb       use the best combination of b-values (first try b<=1250, and otherwise use     the lowest single shell)   
```

```lang-none

  --rounder <Integer>      specify the multiple for rounding b-values, e.g. if the rounder in 100,     then 1233 is treated as 1200 (Default: 100)   
```

```lang-none

  --shells <String>      specify a subset of gradient shells to include (comma-separated list of     b-values)   
```

```lang-none

  --which <String>      specify a subset of gradients to include (comma-separated list of indices     starting from zero)   
```

```lang-none

  --exclude <String>      specify a subset of gradients to exclude (comma-separated list of indices     starting from zero)   
```

```lang-none

  --baseline       estimate the baseline value separately from tensor parameter estimation     (only for LLS)   
```

```lang-none

  --threads <int>      the number of threads to use (Default: 1)   
```

```lang-none

  --clamp <Double>      clamp the diffusivity to be greater or equal to the given value (Default:     0.0)   
```

### Advanced Parameter Arguments:

```lang-none

  --cost <CostType>      specify a cost function for non-linear fitting (Options: SE, MSE, RMSE,     NRMSE, CHISQ, RLL) (Default: SE)   
```

```lang-none

  --single       use the lowest single shell for tensor estimation (if used, this will skip     the shells, which, and exclude flags)   
```

```lang-none

  --bestb       use the best combination of b-values (first try b<=1250, and otherwise use     the lowest single shell)   
```

```lang-none

  --rounder <Integer>      specify the multiple for rounding b-values, e.g. if the rounder in 100,     then 1233 is treated as 1200 (Default: 100)   
```

```lang-none

  --shells <String>      specify a subset of gradient shells to include (comma-separated list of     b-values)   
```

```lang-none

  --which <String>      specify a subset of gradients to include (comma-separated list of indices     starting from zero)   
```

```lang-none

  --exclude <String>      specify a subset of gradients to exclude (comma-separated list of indices     starting from zero)   
```

```lang-none

  --baseline       estimate the baseline value separately from tensor parameter estimation     (only for LLS)   
```

```lang-none

  --threads <int>      the number of threads to use (Default: 1)   
```

```lang-none

  --clamp <Double>      clamp the diffusivity to be greater or equal to the given value (Default:     0.0)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output tensor volume (name output like *.dti and an directory of volumes     will be created)   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeTensorFuse 

### Description:

  fuse a collection of tensor volumes 

### Required Input Arguments:

```lang-none

  --input <Volume(s)>      specify the input tensor volumes   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      specify a mask   
```

### Optional Parameter Arguments:

```lang-none

  --pattern <String(s)>      specify a list of names that will be substituted with input %s   
```

```lang-none

  --log       use log-euclidean estimation   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      specify the output tensor volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeTensorNoise 

### Description:

  Add noise to a tensor volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      input tensor volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --sigma <Double>      the sigma of the noise   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output tensor volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeTensorOdf 

### Description:

  Sample an orientation distribution function (ODF) from a tensor volume. 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input tensor volume   
```

```lang-none

  --dirs <Vects>      the sphere directions   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --alpha <Double>      the alpha power value (Default: 1.0)   
```

```lang-none

  --threads <Integer>      the number of threads in the pool (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output odf volume   
```

### Author:

  Ryan Cabeen 

<hr/>
<hr/>
<hr/>
## VolumeTensorSegmentGraph 

### Description:

  Tensor volume graph-based segmentation based on principal direction 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input tensor volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --line       use a line metric   
```

```lang-none

  --log       use a log Euclidean metric   
```

```lang-none

  --threshold <Double>      a threshold for grouping (Default: 1.0)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      the output segmentation mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeTensorSegmentSuper 

### Description:

  Segment supervoxels from a tensor volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input tensor volume   
```

```lang-none

  --mask <Mask>      a mask   
```

### Optional Input Arguments:

```lang-none

  --weights <Volume>      a weight volume   
```

### Optional Parameter Arguments:

```lang-none

  --size <Integer>      a threshold size (any region smaller than this will be removed)   
```

```lang-none

  --iters <Integer>      the number of iterations (Default: 10)   
```

```lang-none

  --restarts <Integer>      the number of restarts   
```

```lang-none

  --num <Integer>      the number of clusters (the initial number if dp-means is used) (Default:     100)   
```

```lang-none

  --alpha <Double>      the alpha parameter for spatial extent (Default: 1.0)   
```

```lang-none

  --beta <Double>      the beta parameter for angular extent (Default: 15.0)   
```

```lang-none

  --lambda <Double>      the lambda parameter for region size (for dp-means clustering)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      the output segmentation mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeTensorSegmentTissue 

### Description:

  Use a free-water eliminated tensor volume to segment gray and white matter 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input tensor volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --fw <Double>      a maximum free water (Default: 0.5)   
```

```lang-none

  --md <Double>      a minimum mean diffusivity (Default: 2.0E-4)   
```

```lang-none

  --num <Integer>      a number of iterations for mask opening (Default: 2)   
```

### Required Output Arguments:

```lang-none

  --labels <Mask>      the gray matter segmentation mask   
```

```lang-none

  --gray <Volume>      the gray matter density map   
```

```lang-none

  --white <Volume>      the white matter density map   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeTensorSmooth 

### Description:

  Smooth a tensor volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input tensor volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --support <Integer>      the filter radius in voxels (Default: 3)   
```

```lang-none

  --hpos <Double>      the positional bandwidth in mm (Default: 1.0)   
```

```lang-none

  --hdir <Double>      the tensor adaptive bandwidth   
```

```lang-none

  --hsig <Double>      the baseline signal adaptive bandwidth   
```

```lang-none

  --log       use log-euclidean processing   
```

```lang-none

  --frac <Double>      exclude voxels below a give FA (Default: 0.0)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output tensor volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeTensorStandardize 

### Description:

  Standardize the pose of a tensor volume.  This will remove the rotation and 
  tranlation from the image grid and rotate the tensors accordingly 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input tensor volume   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output transformed tensor volume   
```

### Optional Output Arguments:

```lang-none

  --xfm <Affine>      output affine xfm   
```

```lang-none

  --invxfm <Affine>      output inverse affine xfm   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeTensorThreshold 

### Description:

  Create a mask from a tensor volume using a complex expression 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input tensor volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      mask   
```

### Optional Parameter Arguments:

```lang-none

  --expression <String>      the expression to evaluate (Default: FA > 0.15 && MD < 0.001 && MD >     0.0001)   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeTensorTransform 

### Description:

  Spatially transform a tensor volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input tensor volume   
```

```lang-none

  --reference <Volume>      input reference volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      input mask (defined in the reference space)   
```

```lang-none

  --inputMask <Mask>      use an input mask (defined in the input space)   
```

```lang-none

  --affine <Affine>      apply an affine xfm   
```

```lang-none

  --invaffine <Affine>      apply an inverse affine xfm   
```

```lang-none

  --deform <Deformation>      apply a deformation xfm   
```

### Optional Parameter Arguments:

```lang-none

  --reverse       reverse the order, i.e. compose the affine(deform(x)), whereas the default     is deform(affine(x))   
```

```lang-none

  --reorient <ReorientationType>      specify a reorient method (fs or jac) (Options: FiniteStrain, Jacobian)     (Default: Jacobian)   
```

```lang-none

  --interp <KernelInterpolationType>      the interpolation type (Options: Nearest, Trilinear, Gaussian) (Default:     Trilinear)   
```

```lang-none

  --support <Integer>      the filter radius in voxels (Default: 3)   
```

```lang-none

  --hpos <Double>      the positional bandwidth in mm (Default: 1.0)   
```

```lang-none

  --log       use log estimation   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output transformed tensor volume   
```

### Author:

  Ryan Cabeen 

<hr/>
<hr/>
## VolumeTensorZoom 

### Description:

  Zoom a tensor volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input tensor volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      a mask   
```

### Optional Parameter Arguments:

```lang-none

  --factor <double>      a zooming factor (Default: 2.0)   
```

```lang-none

  --interp <KernelInterpolationType>      the interpolation type (Options: Nearest, Trilinear, Gaussian) (Default:     Trilinear)   
```

```lang-none

  --log       use log euclidean estimation   
```

```lang-none

  --support <Integer>      the filter radius in voxels (Default: 3)   
```

```lang-none

  --hpos <Double>      the positional bandwidth in mm (Default: 2.0)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output tensor volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeThreshold 

### Description:

  Threshold a volume to make a mask 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      mask   
```

### Optional Parameter Arguments:

```lang-none

  --threshold <Double>      threshold value (Default: 0.5)   
```

```lang-none

  --magnitude       use the magnitude for multi-channel volumes   
```

```lang-none

  --invert       invert the threshold   
```

```lang-none

  --prenorm       normalize the intensities to unit mean before thresholding   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeThresholdHysteresis 

### Description:

  Apply a hysteresis threshold 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      mask   
```

### Optional Parameter Arguments:

```lang-none

  --low <Double>      this low threshold value (Default: 0.5)   
```

```lang-none

  --high <Double>      the high threshold value (Default: 0.75)   
```

```lang-none

  --magnitude       use the magnitude for multi-channel volumes   
```

```lang-none

  --invert       invert the threshold   
```

```lang-none

  --prenorm       normalize the intensities to unit mean before thresholding   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeThresholdOtsu 

### Description:

  Threshold a volume using Otsu's method 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      mask   
```

### Optional Parameter Arguments:

```lang-none

  --bins <int>      the number of bins (Default: 512)   
```

```lang-none

  --invert       invert the segmentation   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

### Citation:

  Nobuyuki Otsu (1979). "A threshold selection method from gray-level 
  histograms". IEEE Trans. Sys., Man., Cyber. 9 (1): 62-66. 

<hr/>
## VolumeThresholdRamp 

### Description:

  Threshold a volume using a ramp defined by low and high thresholds.  Values 
  below the low threshold will be zero, values above will be one, and ones in 
  between will increase gradually. 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      mask   
```

### Optional Parameter Arguments:

```lang-none

  --low <double>      low threshold value (Default: 0.25)   
```

```lang-none

  --high <double>      low threshold value (Default: 0.25)   
```

```lang-none

  --magnitude       use the magnitude for multi-channel volumes   
```

```lang-none

  --invert       invert the threshold   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output threshold map   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeThresholdRange 

### Description:

  Threshold a volume to make a mask 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      mask   
```

### Optional Parameter Arguments:

```lang-none

  --lower <Double>      threshold value (Default: 0.25)   
```

```lang-none

  --upper <Double>      threshold value (Default: 0.75)   
```

```lang-none

  --magnitude       use the magnitude for multi-channel volumes   
```

```lang-none

  --invert       invert the threshold   
```

```lang-none

  --prenorm       normalize the intensities to unit mean before thresholding   
```

### Required Output Arguments:

```lang-none

  --output <Mask>      output mask   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeTiffPrintInfo 

### Description:

  Print info about the organization of a TIFF file 

### Required Parameter Arguments:

```lang-none

  --input <String>      the filename of the input volume   
```

### Optional Parameter Arguments:

```lang-none

  --which <String>      print info only about a specific sub-page   
```

```lang-none

  --table       write the summary as a table   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeTiffSplit 

### Description:

  Print info about the organization of a TIFF file 

### Required Parameter Arguments:

```lang-none

  --input <String>      the filename of the input tiff file   
```

```lang-none

  --output <String>      the filename pattern of the output tiffs (should contain %d)   
```

### Optional Parameter Arguments:

```lang-none

  --which <String>      extract only a specific sub-page (comma-separated list)   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeTile 

### Description:

  Tile a collection of volumes in a single volume 

### Optional Parameter Arguments:

```lang-none

  --pattern <String>      the input pattern (should contain ${x} and ${y}) (Default:     volume.${x}.${y}.nii.gz)   
```

```lang-none

  --xids <String>      the row identifiers to be substituted (Default: a,b,c)   
```

```lang-none

  --yids <String>      the column identifiers to be substituted (Default: 1,2,3,4)   
```

```lang-none

  --xbuffer <int>      a buffer size between tiles (Default: 0)   
```

```lang-none

  --ybuffer <int>      a buffer size between tiles (Default: 0)   
```

```lang-none

  --orientation <String>      slice orientation for storing a stack (x, y, or z) (Default: z)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeTransfer 

### Description:

  Apply a transfer function to a volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

```lang-none

  --transfer <Table>      input transfer function (table with from and to fields)   
```

### Optional Parameter Arguments:

```lang-none

  --from <String>      the from field name (Default: from)   
```

```lang-none

  --to <String>      the to field name (Default: to)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeTransform 

### Description:

  Transform a volume 

### Required Input Arguments:

```lang-none

  --input <Volume>      input volume   
```

```lang-none

  --reference <Volume>      input reference volume   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      use a mask (defined in the reference space)   
```

```lang-none

  --inputMask <Mask>      use an input mask (defined in the input space)   
```

```lang-none

  --affine <Affine>      apply an affine xfm   
```

```lang-none

  --invaffine <Affine>      apply an inverse affine xfm   
```

```lang-none

  --deform <Deformation>      apply a deformation xfm   
```

### Optional Parameter Arguments:

```lang-none

  --interp <InterpolationType>      image interpolation method (Options: Nearest, Trilinear, Tricubic,     Gaussian, GaussianLocalLinear, GaussianLocalQuadratic) (Default: Trilinear)   
```

```lang-none

  --reverse       reverse the order, i.e. compose the affine(deform(x)), whereas the default     is deform(affine(x))   
```

```lang-none

  --reorient       reorient vector image data   
```

```lang-none

  --reoriention <ReorientationType>      specify a reorient method (fs or jac) (Options: FiniteStrain, Jacobian)     (Default: Jacobian)   
```

```lang-none

  --threads <Integer>      the number of threads in the pool (Default: 1)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeVoxelMathScalar 

### Description:

  Evaluate an expression at each voxel of volume data 

### Required Input Arguments:

```lang-none

  --a <Volume>      the input volume stored as a variable named 'a'   
```

### Optional Input Arguments:

```lang-none

  --b <Volume>      the input volume stored as a variable named 'b'   
```

```lang-none

  --c <Volume>      the input volume stored as a variable named 'c'   
```

```lang-none

  --d <Volume>      the input volume stored as a variable named 'd'   
```

```lang-none

  --mask <Mask>      mask   
```

### Optional Parameter Arguments:

```lang-none

  --expression <String>      the expression to evaluate (Default: a > 0.5)   
```

```lang-none

  --undefined <double>      use the specificed value for undefined output, e.g. NaN or Infinity     (Default: 0.0)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeVoxelMathVect 

### Description:

  Evaluate an expression at each voxel of volume data 

### Required Input Arguments:

```lang-none

  --a <Volume>      the input volume stored as a variable named 'a'   
```

### Optional Input Arguments:

```lang-none

  --b <Volume>      the input volume stored as a variable named 'b'   
```

```lang-none

  --c <Volume>      the input volume stored as a variable named 'c'   
```

```lang-none

  --d <Volume>      the input volume stored as a variable named 'd'   
```

```lang-none

  --mask <Mask>      mask   
```

### Optional Parameter Arguments:

```lang-none

  --world       add a variable for world coordinates of the given voxel   
```

```lang-none

  --expression <String>      the expression to evaluate (Default: mean(a))   
```

```lang-none

  --undefined <double>      use the specified value for undefined output, e.g. NaN or Infinity     (Default: 0.0)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumeZoom 

### Description:

  Zoom a volume.  Note: be careful using this for downsampling, as it does not 
  apply an anti-aliasing prefilter. 

### Required Input Arguments:

```lang-none

  --input <Volume>      the input volume   
```

### Optional Parameter Arguments:

```lang-none

  --factor <Double>      an isotropic scaling factor   
```

```lang-none

  --fi <Double>      a scaling factor in i   
```

```lang-none

  --fj <Double>      a scaling factor in j   
```

```lang-none

  --fk <Double>      a scaling factor in k   
```

```lang-none

  --interp <InterpolationType>      an interpolation type (Options: Nearest, Trilinear, Tricubic, Gaussian,     GaussianLocalLinear, GaussianLocalQuadratic) (Default: Trilinear)   
```

```lang-none

  --threads <Integer>      the number of threads in the pool (Default: 3)   
```

### Required Output Arguments:

```lang-none

  --output <Volume>      the output volume   
```

### Author:

  Ryan Cabeen 

<hr/>
## VolumesTable 

### Description:

  create a table from one or multiple volumes 

### Required Input Arguments:

```lang-none

  --input <String=Volume(s)>      specify the input volumes (field=file field2=file2 ...)   
```

### Optional Input Arguments:

```lang-none

  --mask <Mask>      specify a mask   
```

```lang-none

  --lookup <Table>      use a lookup to map mask labels to names   
```

### Optional Parameter Arguments:

```lang-none

  --pattern <String(s)>      specify a list of names that will be substituted with input %s   
```

```lang-none

  --multiple       use multiple mask labels   
```

```lang-none

  --lookupNameField <String>      specify a name field in the lookup table (Default: name)   
```

```lang-none

  --lookupIndexField <String>      specify an voxel field in the lookup table (Default: index)   
```

```lang-none

  --vi <String>      specify a voxel i voxel name   
```

```lang-none

  --vj <String>      specify a voxel j voxel name   
```

```lang-none

  --vk <String>      specify a voxel k voxel name   
```

```lang-none

  --voxel <String>      specify a voxel voxel field name (Default: voxel)   
```

### Required Output Arguments:

```lang-none

  --output <Table>      specify the output table   
```

### Author:

  Ryan Cabeen 

