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
import qit.data.datasets.Volume;
import qit.data.source.VolumeSource;
import qit.data.models.Tensor;
import qit.data.modules.mri.model.VolumeModelFeature;

import java.io.File;
import java.io.IOException;

public class VolumeTensorCoder
{
    public static boolean matches(String fn)
    {
        return Tensor.matches(PathUtils.basename(fn));
    }

    public static boolean IDX = true;

    public static Volume read(String path) throws IOException
    {
        File file = new File(path).getAbsoluteFile();
        String basename = null;
        if (file.isDirectory())
        {
            // read a directory
            basename = new File(path, "dti").getAbsolutePath();
        }
        else
        {
            // read from a basename
            String dirname = file.getParent();
            String name = file.getName();
            String base = name.split("[_...nii.gz]?")[0];
            basename = new File(dirname, base).getAbsolutePath();
        }

        String baseline = String.format("%s_S0.nii.gz", basename);
        Volume S0 = null;
        if (PathUtils.exists(baseline))
        {
            S0 = Volume.read(String.format("%s_S0.nii.gz", basename));
        }

        Volume V1 = Volume.read(String.format("%s_V1.nii.gz", basename));
        Volume V2 = Volume.read(String.format("%s_V2.nii.gz", basename));
        Volume V3 = Volume.read(String.format("%s_V3.nii.gz", basename));
        Volume L1 = Volume.read(String.format("%s_L1.nii.gz", basename));
        Volume L2 = Volume.read(String.format("%s_L2.nii.gz", basename));
        Volume L3 = Volume.read(String.format("%s_L3.nii.gz", basename));

        Volume FW = null;
        String fwfn = String.format("%s_FW.nii.gz", basename);
        if (PathUtils.exists(fwfn))
        {
            FW = Volume.read(fwfn);
        }

        Volume out = VolumeSource.create(V1.getSampling(), Tensor.DT_DIM);

        for (Sample sample : out.getSampling())
        {
            Tensor tensor = new Tensor(out.get(sample));

            if (S0 != null)
            {
                tensor.setBaseline(S0.get(sample, 0));
            }

            tensor.setVec(0, V1.get(sample));
            tensor.setVec(1, V2.get(sample));
            tensor.setVec(2, V3.get(sample));

            tensor.setVal(0, L1.get(sample, 0));
            tensor.setVal(1, L2.get(sample, 0));
            tensor.setVal(2, L3.get(sample, 0));

            if (FW != null)
            {
                tensor.setFreeWater(FW.get(sample, 0));
            }

            out.set(sample, tensor.getEncoding());
        }

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
        String basename = new File(dirname, "dti").getAbsolutePath();

        for (String id : new String[]{"V", "L"})
        {
            for (int idx = 0; idx < 3; idx++)
            {
                String fn = String.format("%s_%s%d.nii.gz", basename, id, idx + 1);
                int dim = id.equals("V") ? 3 : 1;
                Volume elem = VolumeSource.create(volume.getSampling(), dim);

                for (Sample sample : elem.getSampling())
                {
                    Tensor tensor = new Tensor(volume.get(sample));
                    if (id.equals("V"))
                    {
                        elem.set(sample, tensor.getVec(idx));
                    }
                    else
                    {
                        elem.set(sample, 0, tensor.getVal(idx));
                    }
                }

                elem.write(fn);
            }
        }

        for (String name : Tensor.FEATURES)
        {
            if (name.equals(Tensor.FEATURES_PD))
            {
                continue;
            }

            VolumeModelFeature feature = new VolumeModelFeature();
            feature.input = volume;
            feature.model = ModelType.Tensor.toString();
            feature.feature = name;
            Volume feat = feature.run().output;

            String fn = String.format("%s_%s.nii.gz", basename, name);
            feat.write(fn);
        }
    }
}
