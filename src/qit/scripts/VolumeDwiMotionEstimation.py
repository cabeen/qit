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

"""estimate the motion in a multi-channel volume"""

from common import *

def main():
    usage = "qit %s [opts]" % basename(args[0].split(".")[0])
    parser = OptionParser(usage=usage, description=__doc__)

    parser.add_option("--input", metavar="<fn>", \
        help="specify the multi-channel input volume")
    parser.add_option("--cost", metavar="<str>", default="mutualinfo", \
        help="specify the cost function (default is mutualinfo)")
    parser.add_option("--dof", metavar="<int>", default="6", \
        help="specify the degrees of freedom (default is rigid)")
    parser.add_option("--output", metavar="<dn>", \
        help="specify the output directory name")

    (opts, pos) = parser.parse_args()

    if len(pos) != 0 or len(args) == 1:
        parser.print_help()
        return

    Logging.info("started")

    if not opts.input or not opts.output:
        Logging.error("no input specified")

    tmp_dir = PathUtils.tmpDir(opts.output)
    Logging.info("using temporary directory %s" % tmp_dir)

    Logging.info("reading input volume")
    volume = Volume.read(opts.input)
    num = volume.getDim()

    PathUtils.mkdirs(join(tmp_dir, "split"))

    Logging.info("splitting channels")
    for idx in range(num):
        fn = join(tmp_dir, "split", "channel.%d.nii.gz" % idx)
        VolumeUtils.subvolume(volume, idx).write(fn)

    table = Table()  
    table.withField("index")
    table.withField("angle")
    table.withField("tranm")
    table.withField("tranx")
    table.withField("trany")
    table.withField("tranz")

    angles = VectOnlineStats()
    tranms = VectOnlineStats()
    tranxs = VectOnlineStats()
    tranys = VectOnlineStats()
    tranzs = VectOnlineStats()

    for idx in range(1,num):
        invol = join(tmp_dir, "split", "channel.%d.nii.gz" % idx)
        refvol = join(tmp_dir, "split", "channel.%d.nii.gz" % (idx-1))
        fslxfm = join(tmp_dir, "split", "channel.%d.fsl" % idx)
        worldxfm = join(tmp_dir, "split", "channel.%d.xfm" % idx)

        Logging.info("registering channel %d" % idx)
        flirt(invol, refvol, opts.cost, opts.dof, fslxfm)
        mat = fsl2world(invol, refvol, fslxfm)
        mat.write(worldxfm)
       
        trans = mat.getColumn(3)
        rot = MatrixUtils.axisangle(mat.sub(0, 2, 0, 2))

        angle = abs(rot.angle)
        tranm = trans.norm()
        tranx = abs(trans.get(0))
        trany = abs(trans.get(1))
        tranz = abs(trans.get(2))

        row = Record()
        row.with("index", idx)
        row.with("angle", angle)
        row.with("tranm", tranm)
        row.with("tranx", tranx)
        row.with("trany", trany)
        row.with("tranz", tranz)
        table.addRecord(idx, row)

        angles.update(angle)
        tranms.update(tranm)
        tranxs.update(tranx)
        tranys.update(trany)
        tranzs.update(tranz)

    table.write(join(tmp_dir, "all.csv"))

    meant = Table()  
    meant.withField("name")
    meant.withField("value")
    meant.addRecord(Record().with("name", "angle").with("value", angles.mean))
    meant.addRecord(Record().with("name", "tranm").with("value", tranms.mean))
    meant.addRecord(Record().with("name", "tranx").with("value", tranxs.mean))
    meant.addRecord(Record().with("name", "trany").with("value", tranys.mean))
    meant.addRecord(Record().with("name", "tranz").with("value", tranzs.mean))
    meant.write(join(tmp_dir, "mean.csv"))

    maxt = Table()  
    maxt.addField("name")
    maxt.addField("value")
    maxt.addRecord(Record().with("name", "angle").with("value", angles.max))
    maxt.addRecord(Record().with("name", "tranm").with("value", tranms.max))
    maxt.addRecord(Record().with("name", "tranx").with("value", tranxs.max))
    maxt.addRecord(Record().with("name", "trany").with("value", tranys.max))
    maxt.addRecord(Record().with("name", "tranz").with("value", tranzs.max))
    maxt.write(join(tmp_dir, "max.csv"))

    sumt = Table()  
    sumt.addField("name")
    sumt.addField("value")
    sumt.addRecord(Record().with("name", "angle").with("value", angles.sum))
    sumt.addRecord(Record().with("name", "tranm").with("value", tranms.sum))
    sumt.addRecord(Record().with("name", "tranx").with("value", tranxs.sum))
    sumt.addRecord(Record().with("name", "trany").with("value", tranys.sum))
    sumt.addRecord(Record().with("name", "tranz").with("value", tranzs.sum))
    sumt.write(join(tmp_dir, "sum.csv"))

    Logging.info("cleaning up")
    PathUtils.delete(join(tmp_dir, "split"))
    PathUtils.move(tmp_dir, opts.output, False)

    Logging.info("finished")

def flirt(invol, refvol, cost, dof, outxfm):
    cmd = ["flirt", "-cost", cost, "-in", invol, "-usesqform"]
    cmd += ["-dof", dof, "-ref", refvol, "-omat", outxfm]
    cmd += ["-searchrx", "-180", "180"]
    cmd += ["-searchry", "-180", "180"]
    cmd += ["-searchrz", "-179", "180"]
    if call(cmd, shell=False):
        Logging.error("failed to run flirt")

def fsl2world(invol, refvol, fslxfm):
    coords_fn = join(Global.getRoot(), "share", "fsl", "coords.txt")
    cmd = ["img2imgcoord", coords_fn, "-mm"]
    cmd += ["-src", invol, "-dest", refvol, "-xfm", fslxfm]
    pipe = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    lines = pipe.stdout.readlines()

    p0 = map(float, lines[1].strip().split())
    p1 = map(float, lines[2].strip().split())
    p2 = map(float, lines[3].strip().split())
    p3 = map(float, lines[4].strip().split())

    m03 = p0[0]
    m13 = p0[1]
    m23 = p0[2]
    m00 = p1[0] - p0[0]
    m10 = p1[1] - p0[1]
    m20 = p1[2] - p0[2]
    m01 = p2[0] - p0[0]
    m11 = p2[1] - p0[1]
    m21 = p2[2] - p0[2]
    m02 = p3[0] - p0[0]
    m12 = p3[1] - p0[1]
    m22 = p3[2] - p0[2]

    row0 = [m00, m01, m02, m03] 
    row1 = [m10, m11, m12, m13] 
    row2 = [m20, m21, m22, m23] 

    return Matrix([row0, row1, row2])

if __name__ == "__main__":
    main()
