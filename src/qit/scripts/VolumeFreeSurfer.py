#! /usr/bin/env qit
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

"""  process a volume with FreeSurfer """

from common import *

def main():
    usage="qit %s [opts]" % argv[0]
    parser = OptionParser(usage=usage, description=__doc__)

    parser.add_option("--input", metavar="<file>", \
        help="specify an input T1 image (required)")
    parser.add_option("--output", metavar="<dir>", \
        help="specify an output FreeSurfer directory (required)")

    (opts, pos) = parser.parse_args()

    Logging.info("started")

    Global.assume(opts.input is not None, "no input found")
    Global.assume(opts.output is not None, "no output found")

    tmp_dir = abspath("%s.tmp.%d" % (opts.output, int(time())))
    Logging.info("using temporary directory %s" % tmp_dir)

    sub_dir = abspath("%s/subjects" % tmp_dir)
    Logging.info("using subject directory %s" % sub_dir)

    if not exists(tmp_dir):
        makedirs(tmp_dir)

    if not exists(sub_dir):
        makedirs(sub_dir)

    sub_id = "subject"

    script_fn = join(tmp_dir, "run_fs.sh")

    sf = open(script_fn, "w")
    sf.write('#! /bin/bash\n')
    sf.write('recon-all \\\n')
    sf.write(' -i %s \\\n' % opts.input)
    sf.write(' -s %s \\\n' % sub_id)
    sf.write(' -sd %s \\\n' % sub_dir)
    sf.write(' -all \\\n')
    sf.close()

    Logging.info("running FreeSurfer")
    if call(["bash", script_fn], shell=False):
        print("FreeSurfer failed")
    else:
        if exists(opts.output):
            if opts.clobber:
                Logging.info("clobbering existing results")
                rmtree(opts.output)
            else:  
                bck_dir = "%s.bck.%d" % (opts.output, int(time()))
                Logging.info("backing up existing results to %s" % bck_dir)
                move(opts.output, bck_dir)

        move("%s/%s" % (sub_dir, sub_id), opts.output)
        rmtree(tmp_dir)

    Logging.info("finished")

if __name__ == "__main__":
    main()
