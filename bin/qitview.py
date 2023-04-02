#!/usr/bin/env python
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

"""  qitview: visualization tool for the quatitative imaging toolkit"""

from subprocess import call
from subprocess import Popen 
from subprocess import PIPE
from subprocess import STDOUT
from os.path import join
from os.path import basename
from os.path import dirname
from os.path import exists
from os.path import abspath
from os.path import pardir
from os.path import expanduser
from os import walk
from os import devnull
from sys import argv
from sys import exit
from sys import stdout
from sys import stderr
from sys import platform
 
def main(mem="6G"):

    # check if we can allocate the memory
    #if call(["java", "-Xmx%s" % mem], stdout=open(devnull,'w'), stderr=STDOUT):
    #  # reduce to a 32-bit friendly allocation
    #  mem = "1500M"

    code = run(mem)

    # if that fails, there's a more serious problem
    if code and platform.startswith("win32"):
      # on windows we should keep the window open
      raw_input('An error occurred, please send the above error to the developers.  Press enter to exit')

    exit(code)

def run(mem):
    args = argv[1:]

    if len(args) == 1 and ("-help" in args or "--help" in args):
        print("Usage: qit [jvm_args] [script [script_args]]")
        print("")
        print(__doc__)
        print("")
        print("Options:")
        print("  --help             print this usage page")
        print("  --verbose          enable verbose messaging")
        print("  --debug            enable debugging messaging")
        print("  --scaling <factor> specify a HiDPI scaling factor for linux")
        print("  --volume <fns>     load volume files")
        print("  --mask <fns>       load mask files")
        print("  --mesh <fns>       load mesh files")
        print("  --curves <fns>     load curves files")
        print("  --vects <fns>      load vects files")
        print("  --rseed <int>      specify a specific random seed (for reproducibity)")
        print("  --command          [advanced] print the command used for starting the JVM (but it will not be run)")
        print("  --arch <name>      [advanced] specify a JOGL architecture")
        print("  -D<value>          [advanced] set a JVM system property")
        print("  -X<value>          [advanced] set a JVM non-standard option")
        print("")
        return
    else: 
        jvm = []
        jargs = []     
        for arg in argv[1:]: 
            if arg.startswith("-X") or arg.startswith("-D"):
                # handle a JVM argument
                jvm.append(arg)
            else:
                # handle a script-mode argument
                jargs.append(arg)
        
        # add default memory options if not specified
        if not any([arg.startswith("-Xms") for arg in jvm]):
            jvm.append("-Xms256M")
        if not any([arg.startswith("-Xmx") for arg in jvm]):
            jvm.append("-Xmx%s" % mem)

        base = abspath(join(dirname(argv[0]), pardir))
   
        lib = abspath(join(dirname(argv[0]), pardir, "lib"))
        qitjar = join(lib, "jars", "qit.jar")

        #plugins = abspath(join(expanduser("~"), ".qit", "plugins"))
        #if exists(plugins):
        #  for root, dirs, fns in walk(plugins):
        #    jars.extend(join(root, fn) for fn in fns if fn.endswith("jar"))

        cpsep = ":" # the classpath separator is platform dependent
        if platform.startswith("win32") or platform.startswith("cygwin"):
            cpsep = ";"

        command = False

        if "--command" in jargs:
          del jargs[jargs.index("--command")]
          command = True

        scaling = None

        if "--scaling" in jargs:
          idx = jargs.index("--scaling")
          del jargs[idx]
          scaling = jargs[idx]
          del jargs[idx]

        arch = None

        # check for user-defined architecture
        if "--arch" in argv:
            arch = argv[argv.index("--arch")+1]
       
        # try to detect the arch with the platform variable
        if arch == None:
            if platform.startswith("linux"):
                arch = "linux-amd64" # assume 64-bit
            elif platform == "darwin":
                arch = "macosx-universal"
            elif platform.startswith("win32") or platform.startswith("cygwin"):
                arch = "windows-amd64" # assume 64-bit
            else:
                stderr.write("could not load GL for platform '%s'" % platform) 
                return

        # set up mac-specific properties to allow icons and nice UI integration
        props = []
        if arch == "macosx-universal" and not "--nomac" in argv:
            props.append("-Dsun.awt.noerasebackground=true")
            props.append("-Dsun.java2d.opengl=True")
            props.append("-Dsun.java2d.noddraw=True")
            props.append("-Dapple.laf.useScreenMenuBar=true")
            props.append("-Dcom.apple.mrj.application.apple.menu.about.name=qitview")
            props.append("-Xdock:name=qitview")
            props.append("-Xdock:icon=%s/share/gfx/icon.png" % base)
            # props.append("-Djogamp.gluegen.UseTempJarCache=false")

        if platform.startswith("cygwin"):
          qitjar = Popen(["cygpath", "-pw", qitjar], stdout=PIPE).communicate()[0].strip()

        if scaling:
            props.append("-Dsun.java2d.uiScale=%s" % scaling)

        javacmd = "java"

        javamac = abspath(join(dirname(argv[0]), "runtime", "Contents", "Home", "bin", "java"))
        if exists(javamac):
          javacmd = javamac
          # if which("xattr") is not None:
          #   # help the java binary to run without issue
          #   call(["xattr", "-d", "com.apple.quarantine", javamac])

        javawin = abspath(join(dirname(argv[0]), "runtime", "win", "bin", "java.exe"))
        if exists(javawin):
          javacmd = javawin

        javalin = abspath(join(dirname(argv[0]), "runtime", "bin", "java"))
        if exists(javalin):
          javacmd = javalin

        run = ["qitview.main.Viewer", "--verbose", "--debug"]
        cmd = [javacmd] + jvm + ["-cp", qitjar] + props + run + jargs

        if command:
          print(" ".join(cmd))
        else:
          return exit(call(cmd))

if __name__ == "__main__":
    main()

################################################################################
# End of file
################################################################################
