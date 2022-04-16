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

"""convert freesurfer data to nifti and vtk formats with LPI<->RAS coords"""

from common import *

HEMIS = ["lh", "rh"]
LABELS = ["aparc", "aparc.a2009s"]
SURFS = ["pial", "white", "inflated", "smoothwm"]
ATTRS = ["thickness", "volume", "sulc", "curv", "area"]

ROOT_DIR = Global.getRoot()
FS_PARC_DIR = join(ROOT_DIR, "share", "fs", "parcs")
QIT_PARC_DIR = join(ROOT_DIR, "share", "qit", "parcs")

AS_NAME = "aseg"
DK_NAME = "aparc+aseg"
WM_NAME = "wmparc"

AS_TABLE = join(FS_PARC_DIR, "%s.csv" % AS_NAME)
DK_TABLE = join(FS_PARC_DIR, "%s.csv" % DK_NAME)
WM_TABLE = join(FS_PARC_DIR, "%s.csv" % WM_NAME)
SEGS = {DK_NAME: DK_TABLE, WM_NAME: WM_TABLE, AS_NAME: AS_TABLE}

RIBBON_TABLE = join(FS_PARC_DIR, "ribbon.csv")

SF_VOLS =  ["hippoAmygLabels-T1-T2.v20"]
SF_VOLS += ["hippoAmygLabels-T1.v20"]
SF_VOLS += ["hippoAmygLabels-T2.v20"]

SF_TABLE = join(QIT_PARC_DIR, "subfields.csv")
SF_NAMES = join(QIT_PARC_DIR, "subfields.txt")

WBBM_SRC = DK_NAME
CCWM_SRC = DK_NAME
SCGM_SRC = DK_NAME
DKGM_SRC = DK_NAME
DKWM_SRC = WM_NAME
DKBM_SRC = WM_NAME
LBGM_SRC = DK_NAME
LBWM_SRC = WM_NAME
LBBM_SRC = WM_NAME

WBBM_MAP = join(FS_PARC_DIR, "aparc+aseg.wbbm.map.csv")
CCWM_MAP = join(FS_PARC_DIR, "aparc+aseg.ccwm.map.csv")
SCGM_MAP = join(FS_PARC_DIR, "aparc+aseg.scgm.map.csv")
DKGM_MAP = join(FS_PARC_DIR, "wmparc.dkgm.map.csv")
DKWM_MAP = join(FS_PARC_DIR, "wmparc.dkwm.map.csv")
DKBM_MAP = join(FS_PARC_DIR, "wmparc.dkbm.map.csv")
LBGM_MAP = join(FS_PARC_DIR, "wmparc.lbgm.map.csv")
LBWM_MAP = join(FS_PARC_DIR, "wmparc.lbwm.map.csv")
LBBM_MAP = join(FS_PARC_DIR, "wmparc.lbbm.map.csv")

WBBM_LUT = join(QIT_PARC_DIR, "wbbm.csv")
CCWM_LUT = join(QIT_PARC_DIR, "ccwm.csv")
SCGM_LUT = join(QIT_PARC_DIR, "scgm.csv")
DKGM_LUT = join(QIT_PARC_DIR, "dkgm.csv")
DKWM_LUT = join(QIT_PARC_DIR, "dkwm.csv")
DKBM_LUT = join(QIT_PARC_DIR, "dkbm.csv")
LBGM_LUT = join(QIT_PARC_DIR, "lbgm.csv")
LBWM_LUT = join(QIT_PARC_DIR, "lbwm.csv")
LBBM_LUT = join(QIT_PARC_DIR, "lbbm.csv")

WBBM_NAMES = join(QIT_PARC_DIR, "wbbm.txt")
CCWM_NAMES = join(QIT_PARC_DIR, "ccwm.txt")
SCGM_NAMES = join(QIT_PARC_DIR, "scgm.txt")
DKGM_NAMES = join(QIT_PARC_DIR, "dkgm.txt")
DKWM_NAMES = join(QIT_PARC_DIR, "dkwm.txt")
DKBM_NAMES = join(QIT_PARC_DIR, "dkbm.txt")
LBGM_NAMES = join(QIT_PARC_DIR, "lbgm.txt")
LBWM_NAMES = join(QIT_PARC_DIR, "lbwm.txt")
LBBM_NAMES = join(QIT_PARC_DIR, "lbbm.txt")

PARCS = {}
PARCS["wbbm"] = (WBBM_SRC, WBBM_MAP, WBBM_LUT, WBBM_NAMES)
PARCS["ccwm"] = (CCWM_SRC, CCWM_MAP, CCWM_LUT, CCWM_NAMES)
PARCS["scgm"] = (SCGM_SRC, SCGM_MAP, SCGM_LUT, SCGM_NAMES)
PARCS["dkgm"] = (DKGM_SRC, DKGM_MAP, DKGM_LUT, DKGM_NAMES)
PARCS["dkwm"] = (DKWM_SRC, DKWM_MAP, DKWM_LUT, DKWM_NAMES)
PARCS["dkbm"] = (DKBM_SRC, DKBM_MAP, DKBM_LUT, DKBM_NAMES)
PARCS["lbgm"] = (LBGM_SRC, LBGM_MAP, LBGM_LUT, LBGM_NAMES)
PARCS["lbwm"] = (LBWM_SRC, LBWM_MAP, LBWM_LUT, LBWM_NAMES)
PARCS["lbbm"] = (LBBM_SRC, LBBM_MAP, LBBM_LUT, LBBM_NAMES)

def qitcall(args):
    Logging.info("running command: %s" % " ".join(args))
    if call(args, shell=False):
        Logging.error("failed to run command")

def main():
    usage = "qit FreesurferImport [opts]"
    parser = OptionParser(usage=usage, description=__doc__)
    parser.add_option("--input", metavar="<dir>", \
        help="specify the input directory")
    parser.add_option("--output", metavar="<dir>", \
        help="specify the output directory")
    parser.add_option("--brain", action="store_true", \
        help="import the assorted anatomical images")
    parser.add_option("--segmentations", action="store_true", \
        help="import volumetric segmentations")
    parser.add_option("--subfields", action="store_true", \
        help="import volumetric subfield segmentations")
    parser.add_option("--surfaces", action="store_true", \
        help="import surfaces")
    parser.add_option("--resample", metavar="<mesh>", \
        help="resample surface data to match the given reference mesh")

    (opts, pos) = parser.parse_args()

    if len(pos) != 0 or len(args) == 1:
        parser.print_help()
        return

    if not opts.input:
        Logging.error("no input specified")

    if not opts.output:
        Logging.error("no output specified")

    Logging.info("started")
    subj_dir = opts.input
    tmp_dir = "%s.tmp.%d" % (opts.output, int(time()))

    Logging.info("using subject directory: %s" % subj_dir)

    if not exists(subj_dir):
        Logging.error("invalid subject directory")

    if not exists(tmp_dir):
        makedirs(tmp_dir)

    Logging.info("using temporary directory %s" % tmp_dir)

    def make_vol(name, xfm=False, dname=None, write=True, box=None):
        Logging.info("converting %s" % name)
        in_fn = join(subj_dir, "mri", "%s.mgz" % name)

        if dname:
            pdir = join(tmp_dir, dname)
            if not exists(pdir):
                makedirs(pdir)
            out_fn = join(pdir, "%s.nii.gz" % name)
        else:
            out_fn = join(tmp_dir, "%s.nii.gz" % name)

        if exists(in_fn):
            qitcall(["mri_convert", in_fn, out_fn])

            Logging.info("reading volume")
            volume = Volume.read(out_fn)
            affine = volume.getSampling().affine()

            samp = volume.getSampling()
            ti = 0.5 * samp.numI() * samp.deltaI() - 1
            tj = 0.5 * samp.numJ() * samp.deltaJ() # why is this different?!
            tk = 0.5 * samp.numK() * samp.deltaK() - 1
            shift = VectFunctionSource.add(1.0, VectSource.create3D(ti, tj, tk))

            if box is not None:
              volume = VolumeCrop.apply(volume, box)
              volume.write(out_fn)

            return volume, affine, shift
        else:
            Logging.info("...skipping %s" % in_fn)

    mask, maskaffine, maskshift = make_vol("brainmask")
    mask = VolumeThreshold.apply(mask, 0.5)
    box = MaskBox.applyForeground(mask, 20)
    mask = MaskCrop.apply(mask, box)
    mask.write(join(tmp_dir, "brainmask.nii.gz"))

    make_vol("T1", box=box)
    ref, affine, shift = make_vol("brain", xfm=True, write=opts.brain, box=box)

    if opts.brain:
      make_vol("orig", box=box)
      make_vol("ribbon", box=box)
      qitcall(["cp", RIBBON_TABLE, join(tmp_dir, "ribbon.csv")])

    if opts.segmentations:
        Logging.info("importing segmentations")

        for seg in SEGS:
            make_vol(seg, dname="segs", box=box)
            qitcall(["cp", SEGS[seg], join(tmp_dir, "segs", "%s.csv" % seg)])

        for name in PARCS.keys():
            pdir = join(tmp_dir, name)
            makedirs(pdir)

            source, remap, table, names = PARCS[name]

            Logging.info("copying %s lut" % name)
            qitcall(["cp", table, join(pdir, "rois.csv")])

            Logging.info("copying %s names" % name)
            qitcall(["cp", names, join(pdir, "rois.txt")])

            source_fn = join(tmp_dir, "segs", "%s.nii.gz" % source)
            if exists(source_fn):
                Logging.info("remapping %s labels" % name)
                lut = TableUtils.createIntegerLookup(Table.read(remap))
                func = VectFunctionSource.map(False, lut)
                vol = Volume.read(source_fn)
                remapped = VolumeFunction(func).withInput(vol).run()
                remapped.write(join(pdir, "rois.nii.gz"))

    if opts.subfields:
        Logging.info("importing subfields")
        for hemi in HEMIS:

            mysf = None
            for sf in SF_VOLS:
                sf_name = "%s.%s" % (hemi, sf)
                sf_fn = join(subj_dir, "mri", "%s.mgz" % sf_name)
                if exists(sf_fn):
                    mysf = sf
                    break

            name = "%s.%s" % (hemi, mysf)
            in_fn = join(subj_dir, "mri", "%s.mgz" % name)

            Logging.info("using subfields: %s" % mysf)

            if exists(in_fn):
                pdir = join(tmp_dir, "segs")
                out_fn = join(pdir, "%s.nii.gz" % name)

                if not exists(pdir):
                    makedirs(pdir)

                Logging.info("converting %s" % name)

                qitcall(["mri_convert", in_fn, out_fn])

                Logging.info("writing subfields table")
                qitcall(["cp", SF_TABLE, join(pdir, "%s.csv" % name)])

                pdir = join(tmp_dir, "%s.subfields" % hemi)
                makedirs(pdir)

                Logging.info("writing subfields mask")
                v.write(join(pdir, "rois.nii.gz"))

                Logging.info("writing subfields table")
                qitcall(["cp", SF_TABLE, join(pdir, "rois.csv")])
                qitcall(["cp", SF_NAMES, join(pdir, "rois.txt")])

    if opts.surfaces:
      def writeMesh(base, hemi, mesh):
          mesh.write(join(tmp_dir, "%s.%s.vtk.gz" % (hemi, base)))
      def writeLabel(base, hemi, name, label):
          label.write(join(tmp_dir, "%s.%s.label.%s.txt.gz" % (hemi, base, name)))
      def writeAttr(base, hemi, name, attr):
          attr.write(join(tmp_dir, "%s.%s.attr.%s.txt.gz" % (hemi, base, name)))

      Logging.info("importing surfaces")
      for hemi in HEMIS: 
          Logging.info("reading %s surfaces" % hemi)

          sphere_fn = join(subj_dir, "surf", "%s.sphere" % hemi)
          sphere = Mesh.read(sphere_fn)
          scale = VectFunctionSource.scale(0.01, 3)
          MeshFunction(scale).withMesh(sphere).run()
          cat = sphere.copy()
          MeshUtils.copy(cat, "coord", "sphere")

          reg_sphere_fn = join(subj_dir, "surf", "%s.sphere.reg" % hemi)
          if exists(reg_sphere_fn):
              Logging.info("...adding spherical registration")
              reg_sphere = Mesh.read(reg_sphere_fn)
              scale = VectFunctionSource.scale(0.01, 3)
              MeshFunction(scale).withMesh(reg_sphere).run()
              MeshUtils.copy(reg_sphere, "coord", cat, "sphere.reg")

          labels = {}
          for label in LABELS:
              fn = join(subj_dir, "label", "%s.%s.annot" % (hemi, label))
              if exists(fn):
                  Logging.info("...reading label %s" % label)
                  vals = Vects.read(fn)
                  labels[label] = vals
                  MeshUtils.set(cat, label, vals)
                  writeLabel("native", hemi, label, vals)
              else:
                  Logging.info("...skipping label %s" % label)

          attrs = {}
          for attr in ATTRS:
              fn = join(subj_dir, "surf", "%s.%s" % (hemi, attr))
              if exists(fn):
                  Logging.info("...reading attr %s" % attr)
                  vals = Vects.read(fn)
                  attrs[attr] = vals
                  MeshUtils.set(cat, attr, vals)
                  writeAttr("native", hemi, attr, vals)
              else:
                  Logging.info("...skipping attr %s" % attr)

          for surf in SURFS:
              Logging.info("...processing %s.%s" % (hemi, surf))
              fn = join(subj_dir, "surf", "%s.%s" % (hemi, surf))
              if exists(fn):
                  mesh = Mesh.read(fn)
                  MeshFunction(shift).withMesh(mesh).run()
                  MeshUtils.copy(mesh, "coord", cat, surf)

          Logging.info("...computing middle surface")
          mather = MeshAttrMath()
          mather.input = cat 
          mather.expression = "0.5 * (white + pial)"
          mather.result = "middle" 
          cat = mather.run().output

          Logging.info("...transforming mesh coordinates")
          xfmer = MeshTransform()
          xfmer.input = cat
          xfmer.affine = affine
          xfmer.attrs = "coord,pial,white,smoothwm,middle,inflated"
          xfmer.inplace = True
          xfmer.run()

          Logging.info("...writing output")
          MeshUtils.addIndex(cat)
          MeshUtils.copy(cat, "inflated", "coord")
          writeMesh("native", hemi, cat)

          if opts.resample:
              Logging.info("...resampling surfaces")
              reg = Mesh.read(opts.resample)

              sampler = MeshSampleSphere()
              sampler.withInput(cat)
              sampler.withInputSphere("sphere.reg")
              sampler.withOutput(reg)
              sampler.withOutputSphere("coord")
              sampler.addSkip("coord")
              sampler.addSkip("sphere")
              for label in LABELS:
                  sampler.addLabel(label)
              sampler.run()

              # the area and volume are dependent on the 
              # triangulation, so we need to update these
              features = MeshFeaturesCortex()
              features.input = reg
              features.inplace = True
              features.run()

              MeshUtils.copy(reg, "coord", "sphere")
              MeshUtils.copy(reg, "inflated", "coord")
              writeMesh("reg", hemi, reg)

              for label in LABELS:
                  try:
                      vects = MeshGetVects.apply(reg, label)
                      writeLabel("reg", hemi, label, vects)
                  except:
                      Logging.info("skipping " + label)

              for attr in ATTRS:
                  try:
                      vects = MeshGetVects.apply(reg, attr)
                      writeAttr("reg", hemi, attr, vects) 
                  except:
                      Logging.info("skipping " + attr)
                  

    Logging.info("finalizing")
    if exists(opts.output):
      move(opts.output, "%s.bck.%d" % (opts.output, int(time())))

    move(tmp_dir, opts.output)

    Logging.info("finished")

if __name__ == "__main__":
    main()
