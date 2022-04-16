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

package qit.data.modules.volume;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.text.StrSubstitutor;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Integers;
import qit.base.utils.PathUtils;
import qit.data.formats.volume.VolumeStackCoder;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Volume;
import qit.data.source.SamplingSource;
import qit.data.source.VolumeSource;

import java.io.IOException;
import java.util.Map;

@ModuleDescription("Tile a collection of volumes in a single volume")
@ModuleAuthor("Ryan Cabeen")
public class VolumeTile implements Module
{
    @ModuleParameter
    @ModuleDescription("the input pattern (should contain ${x} and ${y})")
    public String pattern = "volume.${x}.${y}.nii.gz";

    @ModuleParameter
    @ModuleDescription("the row identifiers to be substituted")
    public String xids = "a,b,c";

    @ModuleParameter
    @ModuleDescription("the column identifiers to be substituted")
    public String yids = "1,2,3,4";

    @ModuleParameter
    @ModuleDescription("a buffer size between tiles")
    public int xbuffer = 0;

    @ModuleParameter
    @ModuleDescription("a buffer size between tiles")
    public int ybuffer = 0;

    @ModuleParameter
    @ModuleDescription("slice orientation for storing a stack (x, y, or z)")
    public String orientation = "z";

    @ModuleOutput
    @ModuleDescription("the output volume")
    public Volume output;

    @Override
    public Module run()
    {
        String spattern = this.pattern.replace('%', '$');
        String[] xs = this.xids.split(",");
        String[] ys = this.yids.split(",");

        Logging.info("detected rows: " + xs.length);
        Logging.info("detected columns: " + ys.length);

        int dim = 0;
        Sampling reference = null;
        Volume[][] volumes = new Volume[xs.length][ys.length];
        for (int i = 0; i < xs.length; i++)
        {
            for (int j = 0; j < ys.length; j++)
            {
                Map<String,String> env = Maps.newHashMap();
                env.put("x", xs[i]);
                env.put("y", ys[j]);
                String fn = new StrSubstitutor(env).replace(spattern);

                if (PathUtils.exists(fn))
                {
                    Logging.info("reading: " + fn);
                    try
                    {
                        Volume volume = Volume.read(fn);
                        if (reference == null)
                        {
                            reference = volume.getSampling();
                            dim = volume.getDim();
                        }

                        if (reference.num().equals(volume.getSampling().num()) && dim == volume.getDim())
                        {
                            volumes[i][j] = volume;
                        }
                        else
                        {
                            Logging.info("skipping invalid sampling: " + fn);
                        }
                    }
                    catch (IOException e)
                    {
                        Logging.info("skipping missing file: " + fn);
                    }
                }
                else
                {
                    Logging.info("skipping missing file: " + fn);
                }
            }
        }

        Logging.info("rendering tiles");
        Volume out = null;
        if (Sets.newHashSet(new String[]{"z", "k", "axial", "transverse"}).contains(this.orientation.toLowerCase()))
        {
            int numX = xs.length * reference.numI() + this.xbuffer * (xs.length - 1);
            int numY = ys.length * reference.numJ() + this.ybuffer * (ys.length - 1);
            int numZ = reference.numK();
            Integers num = new Integers(numX, numY, numZ);
            out = VolumeSource.create(SamplingSource.create(num, reference.delta()), dim);

            for (int i = 0; i < xs.length; i++)
            {
                for (int j = 0; j < ys.length; j++)
                {
                    Volume volume = volumes[i][j];
                    if (volume != null)
                    {
                        Logging.info(String.format("...rendering tile: %s %s", i, j));
                        for (Sample sample : reference)
                        {
                            int si = sample.getI() + (reference.numI() + this.xbuffer) * i;
                            int sj = sample.getJ() + (reference.numJ() + this.ybuffer) * j;
                            int sk = sample.getK();

                            out.set(si, sj, sk, volume.get(sample));
                        }
                    }
                }
            }
        }
        else if (Sets.newHashSet(new String[]{"y", "j", "sagittal", "median"}).contains(this.orientation.toLowerCase()))
        {
            int numX = xs.length * reference.numI() + this.xbuffer * (xs.length - 1);
            int numY = reference.numJ();
            int numZ = ys.length * reference.numK() + this.ybuffer * (ys.length - 1);
            Integers num = new Integers(numX, numY, numZ);
            out = VolumeSource.create(SamplingSource.create(num, reference.delta()), dim);

            for (int i = 0; i < xs.length; i++)
            {
                for (int j = 0; j < ys.length; j++)
                {
                    Volume volume = volumes[i][j];
                    if (volume != null)
                    {
                        Logging.info(String.format("...rendering tile: %s %s", i, j));
                        for (Sample sample : reference)
                        {
                            int si = sample.getI() + (reference.numI() + this.xbuffer) * i;
                            int sj = sample.getJ();
                            int sk = sample.getK() + (reference.numK() + this.ybuffer) * j;

                            out.set(si, sj, sk, volume.get(sample));
                        }
                    }
                }
            }
        }
        else if (Sets.newHashSet(new String[]{"x", "i", "frontal", "coronal"}).contains(this.orientation.toLowerCase()))
        {
            int numX = reference.numI();
            int numY = xs.length * reference.numJ() + this.xbuffer * (xs.length - 1);
            int numZ = ys.length * reference.numK() + this.ybuffer * (ys.length - 1);
            Integers num = new Integers(numX, numY, numZ);
            out = VolumeSource.create(SamplingSource.create(num, reference.delta()), dim);

            for (int i = 0; i < xs.length; i++)
            {
                for (int j = 0; j < ys.length; j++)
                {
                    Volume volume = volumes[i][j];
                    if (volume != null)
                    {
                        Logging.info(String.format("...rendering tile: %s %s", i, j));
                        for (Sample sample : reference)
                        {
                            int si = sample.getI();
                            int sj = sample.getJ() + (reference.numJ() + this.xbuffer) * i;
                            int sk = sample.getK() + (reference.numK() + this.ybuffer) * j;

                            out.set(si, sj, sk, volume.get(sample));
                        }
                    }
                }
            }
        }
        else
        {
            Logging.error("invalid orientation: " + this.orientation);
        }

        Logging.info("setting stack orientation");
        VolumeStackCoder.ORIENTATION = this.orientation;

        this.output = out;
        return this;
    }
}
