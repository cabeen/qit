/*******************************************************************************
 *
 * Quantitative Imaging Toolkit (QIT) (c) 2012-2022 Ryan Cabeen
 * All rights reserved.
 *
 * The Software remains the property of Ryan Cabeen ("the Author").
 *
 * The Software is distributed "AS IS" under this Licence solely for
 * non-commercial use in the hope that it will be useful, but in order
 * that the Author as a charitable foundation protects its assets for
 * the benefit of its educational and research purposes, the Author
 * makes clear that no condition is made or to be implied, nor is any
 * warranty given or to be implied, as to the accuracy of the Software,
 * or that it will be suitable for any particular purpose or for use
 * under any specific conditions. Furthermore, the Author disclaims
 * all responsibility for the use which is made of the Software. It
 * further disclaims any liability for the outcomes arising from using
 * the Software.
 *
 * The Licensee agrees to indemnify the Author and hold the
 * Author harmless from and against any and all claims, damages and
 * liabilities asserted by third parties (including claims for
 * negligence) which arise directly or indirectly from the use of the
 * Software or the sale of any products based on the Software.
 *
 * No part of the Software may be reproduced, modified, transmitted or
 * transferred in any form or by any means, electronic or mechanical,
 * without the express permission of the Author. The permission of
 * the Author is not required if the said reproduction, modification,
 * transmission or transference is done without financial return, the
 * conditions of this Licence are imposed upon the receiver of the
 * product, and all original and amended source code is included in any
 * transmitted product. You may be held legally responsible for any
 * copyright infringement that is caused or encouraged by your failure to
 * abide by these terms and conditions.
 *
 * You are not permitted under this Licence to use this Software
 * commercially. Use for which any financial return is received shall be
 * defined as commercial use, and includes (1) integration of all or part
 * of the source code or the Software into a product for sale or license
 * by or on behalf of Licensee to third parties or (2) use of the
 * Software or any derivative of it for research with the final aim of
 * developing software products for sale or license to a third party or
 * (3) use of the Software or any derivative of it for research with the
 * final aim of developing non-software products for sale or license to a
 * third party, or (4) use of the Software to provide any service to an
 * external organisation for which payment is received.
 *
 ******************************************************************************/

package qit.data.formats.mri;

import qit.base.ModelType;
import qit.base.utils.PathUtils;
import qit.data.datasets.Sample;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.models.BiTensor;
import qit.data.models.Mcsmt;
import qit.data.modules.mri.model.VolumeModelFeature;
import qit.data.source.VolumeSource;

import java.io.File;
import java.io.IOException;

public class VolumeMcsmtCoder
{
    public static boolean matches(String fn)
    {
        return Mcsmt.matches(PathUtils.basename(fn));
    }

    public static Volume read(String path) throws IOException
    {
        File file = new File(path).getAbsoluteFile();
        String basename = null;
        if (file.isDirectory())
        {
            // read a directory
            basename = new File(path).getAbsolutePath();
        }
        else
        {
            // read from a basename
            String dirname = file.getParent();
            String name = file.getName();
            String base = name.split("[_...nii.gz]?")[0];
            basename = new File(dirname, base).getAbsolutePath();
        }

        String basefn = PathUtils.join(basename, "mcsmt_base.nii.gz");
        Volume base = null;
        if (new File(basefn).exists())
        {
            base = Volume.read(basefn);
        }

        Volume frac = Volume.read(PathUtils.join(basename, "mcsmt_frac.nii.gz"));
        Volume diff = Volume.read(PathUtils.join(basename, "mcsmt_diff.nii.gz"));
        Volume dot = null;

        String dotfn = PathUtils.join(basename, "mcsmt_dot.nii.gz");
        if (PathUtils.exists(dotfn))
        {
            dot = Volume.read(dotfn);
        }

        Volume out = VolumeSource.create(frac.getSampling(), new Mcsmt().getEncodingSize());

        for (Sample sample : out.getSampling())
        {
            Mcsmt model = new Mcsmt(out.get(sample));

            if (base != null)
            {
                model.setBase(base.get(sample, 0));
            }

            model.setFrac(frac.get(sample, 0));
            model.setDiff(diff.get(sample, 0));

            if (dot != null)
            {
                model.setDot(dot.get(sample, 0));
            }

            out.set(sample, model.getEncoding());
        }

        out.setModel(ModelType.Mcsmt);

        System.gc();
        System.gc();

        return out;
    }

    public static void write(Volume volume, String dirname) throws IOException
    {
        File file = new File(dirname).getAbsoluteFile();
        if (!file.exists())
        {
            file.mkdir();
        }
        String basename = new File(dirname).getAbsolutePath();

        for (String id : new String[]{Mcsmt.BASE, Mcsmt.FRAC, Mcsmt.DIFF, Mcsmt.DOT})
        {
            String fn = PathUtils.join(basename, "mcsmt_" + id + ".nii.gz");
            Volume elem = VolumeSource.create(volume.getSampling(), 1);

            for (Sample sample : elem.getSampling())
            {
                Mcsmt model = new Mcsmt(volume.get(sample));
                Vect value = model.feature(id);
                elem.set(sample, value);
            }

            elem.write(fn);
        }
    }
}
