# How to install QIT

This page provides instructions for installing QIT.  This has been tested on
Mac, Windows, and Debian and Ubuntu GNU/Linux, but your mileage may vary, based
on the specific version of the OS.  You can find instructions and download
links below.  You can also find an archive of previous versions of QIT from
[https://github.com/cabeen/qit/releases](https://github.com/cabeen/qit/releases).

## Installation for Windows

You can download the latest version of QIT for Windows from:

[![Download](images/download-icon.png){: style="height:25px;width:25px"} https://github.com/cabeen/qit/releases/download/latest/qit-build-win-latest.zip](https://github.com/cabeen/qit/releases/download/latest/qit-build-win-latest.zip)

Once the archive has been expanded, you can move the QIT directory to your preferred installation location.  The program files are located in the `bin` subdirectory.  

You can open the viewer by double-clicking `qitview.bat`, and you can access the command line interface by running `qit.bat` from the command prompt.  Note: these scripts are only used for Windows.

## Installation for Mac

You can download the latest version of QIT for Mac from:

[![Download](images/download-icon.png){: style="height:25px;width:25px"} https://github.com/cabeen/qit/releases/download/latest/qit-build-mac-latest.zip](https://github.com/cabeen/qit/releases/download/latest/qit-build-mac-latest.zip)

Once the archive has been expanded, you can move the QIT directory to your
preferred installation location.  The program files are located in the `bin`
subdirectory.  

You can open the viewer by double-clicking `qitview`, and you can access the command line interface by running `qit` from the command prompt.

If you have restrictions on what apps can be run, e.g. the default on macOS Catalina, you may get a message about app signing.  You can fix this issue by opening the  System Preferences > Security & Privacy and clicking the button to allow it.  You can read more about this [https://support.apple.com/en-us/HT202491 here].  You can also try running the script `bin/qitfixmac`, which fixes quarantine attributes that can cause problems in macOS Big Sur.

Note: if for some reason you do not have Python installed, you could also start qitview by running the script `qitview.sh`

## Installation for Linux

You can download the latest version of QIT for Linux here:

[![Download](images/download-icon.png){: style="height:25px;width:25px"} https://github.com/cabeen/qit/releases/download/latest/qit-build-linux-latest.zip](https://github.com/cabeen/qit/releases/download/latest/qit-build-linux-latest.zip)

Once the archive has been expanded, you can move the QIT directory to your preferred installation location.  The program files are located in the `bin` subdirectory.  

You can open the viewer by double-clicking `qitview`, and you can access the command line interface by running `qit` from the command prompt.

Note: if for some reason you do not have Python installed, you could also start qitview by running the script `qitview.sh`

## Advanced Dependencies

The following instructions are '''optional''', but if you would like to integrate QIT with other neuroimaging software packages or use QIT automated workflows, please read on.

Some modules in QIT are integrated with 3rd party software pacakages.  The documentation for those modules should indicate whether it has this kind of dependency.  Here is a list of all the software that you might need and their homepages:

* [DTI-TK](http://dti-tk.sourceforge.net/pmwiki/pmwiki.php), developed with version 2.3.3
* [FSL](https://fsl.fmrib.ox.ac.uk/fsl/fslwiki/), developed with version 5.0
* [Freesurfer](https://surfer.nmr.mgh.harvard.edu/fswiki), developed with version 5.0
* [MRtrix](http://www.mrtrix.org), developed with version 3.0
* [dcm2nii](https://www.nitrc.org/projects/dcm2nii)
* [MATLAB](https://www.mathworks.com/products/matlab.html)
* [Advances Normalization Tools (ANTs)](https://github.com/ANTsX/ANTs)

For QIT to use these packages, the associated programs have to be on your
system path.  That means that you can run them on the command line without
specifying the full path.  For example, you should be able to execute
Freesurfer's `mri_convert` from any working directory.  Each package should
have instructions for adding its programs to the path, so please check their
documentation pages.  After that, you should be good to go!
