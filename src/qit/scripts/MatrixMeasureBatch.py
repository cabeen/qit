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

"""measure properties of network matrices in batch mode"""

from common import *

def main():
    usage="qit MatrixMeasureBatch [opts]"
    parser = OptionParser(usage=usage, description=__doc__)

    parser.add_option("--matrix", metavar="<pattern>", \
        help="the filename pattern (%s is substituted)")
    parser.add_option("--nodes", metavar="<list>", \
        help="a list of node names")
    parser.add_option("--names", metavar="<list>", \
        help="a list of names to process in batch")
    parser.add_option("--output", metavar="<dir>", \
        help="the output directory")

    (opts, pos) = parser.parse_args()

    if len(pos) != 0 or len(args) == 1:
        parser.print_help()
        return

    if not opts.matrix:
        Logging.error("no input specified")

    if not opts.nodes:
        Logging.error("no nodes specified")

    if not opts.names:
        Logging.error("no names specified")

    if not opts.output:
        Logging.error("no output specified")

    Logging.info("started")

    tmp_dir = "%s.tmp.%d" % (opts.output, int(time())) 
    if not exists(tmp_dir):
        makedirs(tmp_dir)
    Logging.info("using temporary directory %s" % tmp_dir)

    if exists(opts.names):
        names = [line.strip() for line in open(opts.names)]
    else:
        names = opts.names.split(",")

    for name in names:
      Logging.info("processing: %s" % name)

      matrix_fn = opts.matrix % name
      nodes_fn = opts.nodes
      out_dn = join(tmp_dir, name)

      cmd = ["qbctmeas", matrix_fn, nodes_fn, out_dn]
      if call(cmd, shell=False):
          Logging.error("failed to run: " + " ".join(cmd))

    Logging.info("finalizing")
    move(tmp_dir, opts.output)

    Logging.info("finished")

if __name__ == "__main__":
    main()
