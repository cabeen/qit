# What voxel models are supported by QIT?

QIT has special features for working with model-based imaging data.  Most imaging data represents colors or grayscale intensities at each pixel, but scientific imaging data can also represent physical measurements.  Magnetic resonance (MR) imaging can also be used to depict a spatial distribution of such physical measurements.  There are many types of physical processes that can be characterized using MR, for example relaxation rates, diffusivities, compartment proportions, etc.  QIT is designed to support many of the commonly encountered models, particularly those obtained from diffusion MR imaging.  In this section, we will discuss the way models are supported by QIT and how you can use them.

## What is a `Model`?

A `Model` object represents a collection of parameters of some physical model, typically stored in an image voxel.  We use this type of object-oriented approach because most useful models are multivariate and have some constraints on what model parameters are valid.  QIT is designed to make it possible to treat these special imaging datasets like any other data, but it allows allows more advanced visualization techniques, like creating 3D renderings of model imaging data using glyphs.

There are several  characteristics shared by all models:

- A `Model` can be encoded (or parameterized) by a `Vect`
- A `Model` has any number of named `Vect` valued features
- A `Model` has a way to compute the distance between it any other model of the same type

This representation is flexible and can be used for storing model data in `Volume` datasets, or any other datatype that uses `Vect` object.   This simplifies things greatly, as all of their existing file formats and `Module` objects are available for use with model data.  It also allows model data to be treated in special ways, for example, in specially designed image processing algorithms or in glyph-based visualization.

However, even though a `Model` can be converted to a `Vect`, it is important to remember that they often cannot be treated like vectors algebraically.  For example, you may be tempted to add together, scale, or compute the magnitude of `Vect`s derived from a `Model`; however, if the model parameters store a vector representing a 3D direction as a point on a sphere, then adding those values may leave you with model parameters that don't make any sense, i.e. no longer lie on a sphere.

## What types of `Model` are available?

Below is a detailed list of the models supported by QIT.  A general description of each model is provided and any peculiarities are noted as well.

This list is a work in progress and will eventually be expanded to provide more detail regarding the parameters, features, and motivation behind each model.

## Tensor

The `Tensor` model is the basis for diffusion tensor imaging (DTI). DTI depicts the decay of the diffusion signal using a 3x3 positive definite matrix, which provides a way to depict an ellipsoid-shaped pattern of diffusion.  The shape of the tensor can be used to characterize the tissue being modeled, and this is usually done by extracting a variety of features.  The primary orientation of the tensor is typically visualized to depict the dominant orientation of axonal fibers within the vowel.  Fractional anisotropic is a feature that conveys the degree to which the tensor is anisotropic, or cigar-shaped.  Mean diffusivity describes the overal shape of the tensor (regardless of its anisotropic).  The tensor diffusivity can also be depicted in specific directions, for example, axial diffusivity describes the diffusivity in the direction of the primary orientation, and radial diffusivity describes the diffusivity in the plane orthogonal to the primary orientation.

## Fibers

The `Fibers` model is used for representing the multi-compartment ball and sticks model.  This model was developed to address shortcomings of the tensor model, namely that it cannot accurately depict voxels with multiple distinct components.  For example, a voxel may contain some gray matter or cerebrospinal fluid in addition to white matter fibers, or it may combine white matter fibers from distinct fascicles that cross.  Multi-compartment models address this issue by depicting the diffusion signal with a linear combination of tissue compartments.  The ball and sticks model s a simple but powerful special case of this approach, in which the signal is decomposed into compartments for isotopic diffusion and some arbitrary number of fibers populations.  The diffusivities of the compartments are assumed to be identical and each fiber compartment is formulated as an infinitely thin cylinder.  This enables the model to distinguish between crossing fibers, as well as more complex mixtures with gray matter and CSF.  Each compartment is assigned a volume fraction, which indicates the proportion of the signal that it explains.  Two common features to extract are the total fiber volume fraction (the sum of the individual fiber compartment volume fractions), the isotopic volume fraction, and the diffusivity.

## Spharm

The `Spharm` model represents an arbitrary spherical function describing a fiber orientation distribution...

## Noddi

The `Noddi` model is a biophysical model representing multiple compartments for intra- and extra-axonal water and fiber orientation dispersion...

## Kurtosis

The `Kurtosis` model is the basis for a higher-order extension of the diffusion tensor model...

## ExpDecay

The `ExpDecay` model is a general model for representing processes that exhibit a pattern of exponential decay in the MR signal over time.

