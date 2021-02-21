![alt-text](https://github.com/cabeen/qit/blob/main/splash.jpg?raw=true)

# The Quantitative Imaging Toolkit

The repository for the Quantitative Imaging Toolkit (QIT). QIT is a software 
package for visualization, exploration, and analysis of neuroimaging datasets. 
QIT supports a variety of magnetic resonance imaging (MRI) with advanced 
capabilities for mapping brain microstructure and connectivity using diffusion 
MRI, and it also has an expanding base of microscopy tools.   You can learn 
more at the [QIT website](http://cabeen.io/qitwiki).  Presently, QIT is only 
available as compiled binaries, which you can download through
[github releases](https://github.com/cabeen/qit/releases).

## What is QIT?

QIT is a software package of computational tools for the modeling, analysis,
and visualization of scientific imaging data.  It was specifically developed
for tractography and microstructure analysis of diffusion magnetic resonance
imaging datasets, but it has capabilities that are generally useful for other
imaging modalities as well.  It supports many different data types, including
multi-channel volumetric datasets, multi-label masks, curves, triangle meshes,
geometric primitives, tabular data, and spatial transformations.  QIT provides
an application called `qitview` for interactive 3D rendering and data analysis,
as well as, a suite of command line tools available through a program named
`qit` that provides a way to do batch processing and scripting.  In addition,
QIT also provides ways to integrate of these tools into grid computing
environments and scientific workflows.

## How can I use QIT?

If QIT was used in part of your research, please cite the following abstract in associated publications:

```
Cabeen, R. P., Laidlaw, D. H., and Toga, A. W. (2018). Quantitative Imaging
Toolkit: Software for Interactive 3D Visualization, Data Exploration, and
Computational Analysis of Neuroimaging Datasets. Proceedings of the
International Society for Magnetic Resonance in Medicine (ISMRM), 2854.
```

You can also find the license agreement for QIT use in the `LICENSE` file.

## How is QIT developed?

QIT was originally designed and developed by [Ryan Cabeen](http://cabeen.io)
for his Ph.D under advisement of [David H.
Laidlaw](http://cs.brown.edu/people/dhl/) in the [Department of Computer
Science at Brown University](http://cs.brown.edu) starting in 2012.  Currently,
QIT is actively being developed under advisement of Dr. Arthur Toga at the USC
[http://resource.loni.usc.edu/ Laboratory of Neuro Imaging Resource].

QIT was developed using [IntelliJ IDEA](https://www.jetbrains.com/idea/) with a
combination of Java (version 11 and above) and Python (version 2.7 and above).
The package also uses the excellent open source resources listed below.  In the
QIT application directory, you can also find copies of their license files, as
well as the separate jars for each library that are linked by QIT.

* [Apache Commons CLI 1.2](http://commons.apache.org/cli), released under the Apache v2 license
* [Apache Commons IO 2.1](http://commons.apache.org/io/), released under the Apache v2 license
* [Apache Commons Lang 3.0.1](http://commons.apache.org/lang/), released under the Apache v2 license
* [Gson](https://github.com/google/gson), released under the Apache v2 license
* [log4j 1.2.9](http://logging.apache.org/log4j/1.2) released under the Apache v2 license
* [Guava 11.0.2](http://code.google.com/p/guava-libraries), released under the Apache v2 license
* [Jama 1.0.2](http://math.nist.gov/javanumerics/jama), released into the public domain
* [opencsv 2.3](http://opencsv.sourceforge.net/), released under the Apache v2 license
* [reflections-0.9.8](https://code.google.com/p/reflections/), released under the WTFPL license
* [Jython 2.5.0](http://www.jython.org/), released under the Jython license
* [jythonconsole-0.0.7](https://code.google.com/p/jythonconsole/), released under the LGPL v3 license
* [Camino](http://cmic.cs.ucl.ac.uk/camino/), released under the Artistic 2.0 license
* [matrix-toolkits-java](https://github.com/fommil/matrix-toolkits-java), released under the LGPL v3 license
* [smile](https://github.com/haifengl/smile), released under the Apache 2.0 license
* [jogl](https://jogamp.org), released under the BSD license
* [JPOP: Java parallel optimization package](https://www5.cs.fau.de/research/software/java-parallel-optimization-package/), released under the LGPL license
* [object-explorer](https://github.com/DimitrisAndreou/memory-measurer), released under Apache v2 license
* [Logo](https://www.flickr.com/photos/dierkschaefer/2961565820), released under the Creative Commons license

The license documentation associated with each of these can be found in the
software package under `doc/licensing`.

# Contact

Comments, criticism, and concerns are appreciated and can be directed to [Ryan Cabeen](cabeen@gmail.com).
