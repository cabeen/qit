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

"""  qit: quantitative imaging toolkit. This is the command line interface for
a variety of computational tools that supports three modes of operation:
built-in modules, user-defined scripts (Jython), or an interactive Jython REPL
interpreter.  To get started, you can see a list of available modules by
running 'qit --list', choosing a module, and running 'qit ModuleName --help' to
learn more. The advanced features include batch processing, logging, and
standard JVM options. Batch variables are expected to be used with either
${var} or %{var}.  If you have a memory error when processing large datasets,
you should allocate more space to the JVM by including a flag like '-Xmx8G',
where 8G indicates 8 gigabytes of memory is made available.  For more
information, please check out the documentation at http://cabeen.io/qitwiki"""

from subprocess import call
from subprocess import Popen 
from subprocess import PIPE
from os.path import join
from os.path import basename
from os.path import dirname
from os.path import exists
from os.path import abspath
from os.path import pardir
from os.path import expanduser
from os import environ
from os import walk
from os import getcwd
from sys import argv
from sys import exit
from sys import stdout
from sys import stderr
from sys import platform
 
def main():
    args = argv[1:]

    if len(args) == 0 or (len(args) == 1 and ("-help" in args or "--help" in args)):
        print("Usage: qit [jvm_args] [[module or script name] [optional flags]]")
        print("")
        print(__doc__)
        print("")
        print("Options:")
        print("  --help                   print this usage page")
        print("  --list                   list the available built-in modules")
        print("  --verbose                enable verbose messaging")
        print("  --debug                  enable debugging messaging")
        print("  --rseed <int>            specify a random seed")
        print("  --dtype <type>           specify a default datatype (double or float)")
        print("  --double                 set the datatype to double")
        print("  --preserve               preserve the datatype when possible")
        print("  --interactive            start the Jython REPL interpreter")
        print("  --load                   load module parameters from json")
        print("  --save                   save module parameters to json")
        print("  --validate               run a validation of module definitions")
        print("  --pipeline <name> <fn>   export a module to the LONI pipeline")
        print("  --pipelines <dir>        export all modules to the LONI pipeline")
        print("  --dump                   dump output data when an error occurs")
        print("  --batch                  execute a script in batch mode")
        print("  --batch-var <name=spec>  specify a batch mode variable")
        print("  --batch-table <fn>       specify batch mode variable(s)")
        print("  --batch-product          enable variable cartesian product")
        print("  --nailgun                use nailgun for fast startup")
        print("  --nailgun-start          start the nailgun server")
        print("  --nailgun-stop           stop the nailgun server")
        print("  --class=<name>           run a specific main Java class")
        print("  --command                print the command used for starting the JVM (but it will not be run)")
        print("  -D<value>                set a JVM system property")
        print("  -X<value>                set a JVM non-standard option")
        print("")
        return
    else: 
        jvm = []
        jargs = []     
        ng = False
        ng_start = False 
        ng_stop = False 
        main = "qit.main.QitMain"

        for arg in argv[1:]: 
            if arg.startswith("-X") or arg.startswith("-D"):
                # handle a JVM argument
                jvm.append(arg)
            elif arg == "--nailgun":
                ng = True
            elif arg == "--nailgun-start":
                ng_start = True
            elif arg == "--nailgun-stop":
                ng_stop = True
            elif arg.startswith("--class="):
                main = arg.split("--class=")[1]
            else:
                # handle a script-mode argument
                jargs.append(arg)

        command = False

        if "--command" in jargs:
          del jargs[jargs.index("--command")]
          command = True

        # use nailgun if specified in environment
        if "NAILGUN" in environ and environ["NAILGUN"] == "1":
            ng = True

        # keep MacOS icons from popping up in script mode
        if "--interactive" not in jargs:
            jvm.append("-Djava.awt.headless=true")
        
        # add default memory options if not specified
        if not any([arg.startswith("-Xms") for arg in jvm]):
            jvm.append("-Xms256M")
        if not any([arg.startswith("-Xmx") for arg in jvm]):
            jvm.append("-Xmx4G")

        lib = abspath(join(dirname(argv[0]), pardir, "lib"))
        qitjar = join(lib, "jars", "qit.jar")

        #plugins = abspath(join(expanduser("~"), ".qit", "plugins"))
        #if exists(plugins):
        #  for root, dirs, fns in walk(plugins):
        #    jars.extend(join(root, fn) for fn in fns if fn.endswith("jar")) 

        # cygwin needs the paths to be fixed
        if platform.startswith("cygwin"):
          qitjar = Popen(["cygpath", "-pw", qitjar], stdout=PIPE).communicate()[0].strip()

        cpsep = ":" # the classpath separator is platform dependent
        if platform.startswith("win32") or platform.startswith("cygwin"):
            cpsep = ";"

        javacmd = "java"

        javamac = abspath(join(dirname(argv[0]), "runtime", "Contents", "Home", "bin", "java"))
        if exists(javamac):
          javacmd = javamac
          # if which("xattr") is not None:
          #   # help the java binary to run without issue
          #   call(["xattr", "-d", "com.apple.quarantine", javamac])

        javawin = abspath(join(dirname(argv[0]), "runtime", "bin", "java.exe"))
        if exists(javawin):
          javacmd = javawin

        javalin = abspath(join(dirname(argv[0]), "runtime", "bin", "java"))
        if exists(javalin):
          javacmd = javalin

        if ng_start:   
            cmd = [javacmd] + jvm + ["-classpath", qitjar]
            cmd = cmd + ["com.martiansoftware.nailgun.NGServer"]
            Popen(cmd)
        elif ng_stop:
            cmd = ["ng", "ng-stop"]
            exit(call(cmd))
        elif ng:
            jargs += ["--cwd", abspath(getcwd())]
            cmd = ["ng", main] + jargs
            exit(call(cmd))
        else:
            cmd = [javacmd] + jvm + ["-cp", qitjar, main] + jargs

            if command:
              print(" ".join(cmd))
            else:
              exit(call(cmd))

if __name__ == "__main__":
    main()

################################################################################
# End of file
################################################################################
