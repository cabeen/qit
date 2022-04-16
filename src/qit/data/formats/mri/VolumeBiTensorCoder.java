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
import qit.data.models.BiTensor;
import qit.data.modules.mri.model.VolumeModelFeature;
import qit.data.source.VolumeSource;

import java.io.File;
import java.io.IOException;

public class VolumeBiTensorCoder
{
    public static boolean matches(String fn)
    {
        return BiTensor.matches(PathUtils.basename(fn));
    }

    public static Volume read(String path) throws IOException
    {
        File file = new File(path).getAbsoluteFile();
        String basename = null;
        if (file.isDirectory())
        {
            // read a directory
            basename = new File(path, "bitensor").getAbsolutePath();
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
        if (new File(baseline).exists())
        {
            S0 = Volume.read(String.format("%s_S0.nii.gz", basename));
        }

        Volume frac = Volume.read(String.format("%s_frac.nii.gz", basename));

        Volume tV1 = Volume.read(String.format("%s_tV1.nii.gz", basename));
        Volume tV2 = Volume.read(String.format("%s_tV2.nii.gz", basename));
        Volume tV3 = Volume.read(String.format("%s_tV3.nii.gz", basename));
        Volume tL1 = Volume.read(String.format("%s_tL1.nii.gz", basename));
        Volume tL2 = Volume.read(String.format("%s_tL2.nii.gz", basename));
        Volume tL3 = Volume.read(String.format("%s_tL3.nii.gz", basename));

        Volume fV1 = Volume.read(String.format("%s_fV1.nii.gz", basename));
        Volume fV2 = Volume.read(String.format("%s_fV2.nii.gz", basename));
        Volume fV3 = Volume.read(String.format("%s_fV3.nii.gz", basename));
        Volume fL1 = Volume.read(String.format("%s_fL1.nii.gz", basename));
        Volume fL2 = Volume.read(String.format("%s_fL2.nii.gz", basename));
        Volume fL3 = Volume.read(String.format("%s_fL3.nii.gz", basename));

        Volume out = VolumeSource.create(tV1.getSampling(), BiTensor.BDT_DIM);

        for (Sample sample : out.getSampling())
        {
            BiTensor tensor = new BiTensor(out.get(sample));

            if (S0 != null)
            {
                tensor.setBaseline(S0.get(sample, 0));
            }

            tensor.setFraction(frac.get(sample, 0));

            tensor.setTissueVec(0, tV1.get(sample));
            tensor.setTissueVec(1, tV2.get(sample));
            tensor.setTissueVec(2, tV3.get(sample));

            tensor.setTissueVal(0, tL1.get(sample, 0));
            tensor.setTissueVal(1, tL2.get(sample, 0));
            tensor.setTissueVal(2, tL3.get(sample, 0));

            tensor.setFluidVec(0, fV1.get(sample));
            tensor.setFluidVec(1, fV2.get(sample));
            tensor.setFluidVec(2, fV3.get(sample));

            tensor.setFluidVal(0, fL1.get(sample, 0));
            tensor.setFluidVal(1, fL2.get(sample, 0));
            tensor.setFluidVal(2, fL3.get(sample, 0));

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
        String basename = new File(dirname, "bitensor").getAbsolutePath();

        for (String id : new String[]{"tV", "tL", "fV", "fL"})
        {
            for (int idx = 0; idx < 3; idx++)
            {
                String fn = String.format("%s_%s%d.nii.gz", basename, id, idx + 1);
                int dim = id.endsWith("V") ? 3 : 1;
                Volume elem = VolumeSource.create(volume.getSampling(), dim);

                for (Sample sample : elem.getSampling())
                {
                    BiTensor tensor = new BiTensor(volume.get(sample));
                    if (id.equals("tV"))
                    {
                        elem.set(sample, tensor.getTissueVec(idx));
                    }
                    else if (id.equals("tL"))
                    {
                        elem.set(sample, 0, tensor.getTissueVal(idx));
                    }
                    else if (id.equals("fV"))
                    {
                        elem.set(sample, tensor.getFluidVec(idx));
                    }
                    else if (id.equals("fL"))
                    {
                        elem.set(sample, 0, tensor.getFluidVal(idx));
                    }
                }

                elem.write(fn);
            }
        }

        for (String name : BiTensor.FEATURES)
        {
            if (name.equals(BiTensor.FEATURES_TPD) || name.equals(BiTensor.FEATURES_FPD))
            {
                continue;
            }

            VolumeModelFeature feature = new VolumeModelFeature();
            feature.input = volume;
            feature.model = ModelType.BiTensor.toString();
            feature.feature = name;
            Volume feat = feature.run().output;

            String fn = String.format("%s_%s.nii.gz", basename, name);
            feat.write(fn);
        }
    }
}
