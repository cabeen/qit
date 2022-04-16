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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.base.structs.Integers;
import qit.base.structs.Pair;
import qit.data.datasets.Mask;
import qit.data.datasets.Record;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.utils.TableUtils;
import qit.math.structs.VectFunction;
import qit.math.utils.colormaps.ColormapScalar;
import qit.math.utils.colormaps.ColormapSource;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ModuleDescription("Composite statistical results onto precomputed 3D renderings.  This requires " +
  "a pair of images showing a 3D rendered scene.  One image should have pixels colored " +
  "according to the region of interest without any shading, and the other should " +
  "have the 3D shading without any coloring.  The program takes CSV tables and " +
  "then colors the shaded surface by matching the pixel coloring to records in the tables.")
@ModuleAuthor("Ryan Cabeen")
@ModuleUnlisted
public class VolumeCompositeRender implements Module
{
    @ModuleInput
    @ModuleDescription("input coloring volume")
    public Volume coloring;

    @ModuleInput
    @ModuleDescription("input shading volume")
    public Volume shading;

    @ModuleInput
    @ModuleDescription("input lookup table (relation of names and colors)")
    public Table lut;

    @ModuleInput
    @ModuleDescription("input data table (relation of names and parameters)")
    public Table data;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input data table lookup (relation of names and index values)")
    public Table datalut;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("use a discrete colormap")
    public String discrete = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("use a scalar colormap")
    public String scalar = null;

    @ModuleParameter
    @ModuleDescription("reverse the scalar colormap")
    public boolean reverse = false;

    @ModuleParameter
    @ModuleDescription("the high value for scalar color mapping")
    public double high = 1.0;

    @ModuleParameter
    @ModuleDescription("the low value for scalar color mapping (set to negative high value if using a diverging colormap)")
    public double low = 0.0;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("red field name")
    public String red = "r";

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("green field name")
    public String green = "g";

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("blue field name")
    public String blue = "b";

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("matching field name")
    public String match = "name";

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("value field name")
    public String value = "value";

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the name field")
    public String name = "name";

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the index field")
    public String index = "index";

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output label mask")
    public Mask indices;

    @ModuleOutput
    @ModuleDescription("output volume")
    public Volume output;

    @Override
    public VolumeCompositeRender run()
    {
        Sampling sampling = this.shading.getSampling();
        Volume out = this.shading.proto(3);
        Mask outIndices = new Mask(this.shading.getSampling());

        Logging.info("computing dictionaries");
        Map<Integer, Double> mapKeyToValue = Maps.newHashMap();
        Map<String, Integer> mapNameToKey = Maps.newHashMap();

        for (Integer key : this.lut.keys())
        {
            String name = this.lut.get(key, this.match);
            mapNameToKey.put(name, key);
        }

        Map<String,Integer> datalutMap = null;

        if (this.datalut != null)
        {
            datalutMap = TableUtils.createStringIntegerLookup(this.datalut, this.name, this.index);
        }

        for (Integer key : this.data.keys())
        {
            String name = this.data.get(key, this.match);
            if (mapNameToKey.containsKey(name))
            {
                int keyLUT = mapNameToKey.get(name);

                if (this.datalut != null)
                {
                    String token = this.data.get(key, this.value);
                    Global.assume(datalutMap.containsKey(token), "token not found: " + token);
                    double value = datalutMap.get(token);
                    mapKeyToValue.put(keyLUT, value);
                }
                else
                {
                    double value = Double.valueOf(this.data.get(key, this.value));
                    mapKeyToValue.put(keyLUT, value);
                }
            }
        }

        Logging.info("computing label map");
        Mask labels = new Mask(sampling);
        Map<Integer, Integer> mapLabelToKey = Maps.newHashMap();
        Set<Integer> blended = Sets.newHashSet();
        for (Sample sample : sampling)
        {
            Vect color = this.coloring.get(sample);
            int r = (int) (255 * color.getX());
            int g = (int) (255 * color.getY());
            int b = (int) (255 * color.getZ());

            if (r == 0 && g == 0 && b == 0)
            {
                continue;
            }
            else
            {
                int rgb = new Color(r, g, b).getRGB();

                labels.set(sample, rgb);

                // exclude pixels that are blended by detecting uniform patches
                boolean valid = true;
                for (Sample nsample : sampling.iterateNeighborhood(sample, new Integers(1, 1, 1)))
                {
                    if (this.coloring.valid(nsample))
                    {
                        valid &= pack(this.coloring.get(nsample)) == rgb;
                    }
                }

                if (valid && !mapLabelToKey.containsKey(rgb))
                {
                    mapLabelToKey.put(rgb, null);
                }

                if (!valid)
                {
                    blended.add(rgb);
                }
            }
        }

        Logging.info("computing label key dictionary");
        Map<Integer, Integer> mapLabelToIndex = Maps.newHashMap();
        for (int color : mapLabelToKey.keySet())
        {
            float[] hsv = new float[3];
            int mindist = 255;
            int ncolor = 0;

            Integer minkey = null;
            Integer minidx = null;
            {
                for (Integer key : this.lut.keys())
                {
                    Record record = this.lut.getRecord(key);
                    int idx = Integer.valueOf(record.get(this.index));

                    int tr = Integer.valueOf(record.get(this.red));
                    int tg = Integer.valueOf(record.get(this.green));
                    int tb = Integer.valueOf(record.get(this.blue));

                    Color c = new Color(color);
                    int sr = c.getRed();
                    int sg = c.getGreen();
                    int sb = c.getBlue();

                    int dist = 0;
                    dist = Math.max(dist, Math.abs(sr - tr));
                    dist = Math.max(dist, Math.abs(sg - tg));
                    dist = Math.max(dist, Math.abs(sb - tb));

                    if (dist < mindist)
                    {
                        mindist = dist;
                        minkey = key;
                        minidx = idx;
                        ncolor = new Color(tr, tg, tb).getRGB();
                    }
                }
            }
            mapLabelToKey.put(color, minkey);
            mapLabelToIndex.put(color, minidx);
        }

        Logging.info("coloring volume");
        VectFunction colormap = null;
        if (this.discrete != null)
        {
            colormap = ColormapSource.getDiscrete(this.discrete).getFunction();
        }
        else if (this.scalar != null)
        {
            ColormapScalar cmap = ColormapSource.getScalar(this.scalar).withMin(this.low).withMax(this.high);

            if (this.reverse)
            {
                List<Pair<Double, Double>> pairs = Lists.newArrayList();
                pairs.add(Pair.of(0.0, 1.0));
                pairs.add(Pair.of(1.0, 0.0));
                cmap.withTransfer(pairs);
            }

            colormap = cmap.getFunction();
        }
        else
        {
            Logging.error("either a discrete or scalar colormap must be specified");
        }

        for (Sample sample : sampling)
        {
            Vect shadeRGB = this.shading.get(sample);
            Vect shadeHSV = hsv(shadeRGB);

            double hue = shadeHSV.getX();
            double sat = shadeHSV.getY();
            double val = shadeHSV.getZ();

            int label = labels.get(sample);

            if (mapLabelToKey.containsKey(label))
            {
                int key = mapLabelToKey.get(label);
                if (mapKeyToValue.containsKey(key))
                {
                    double dataValue = mapKeyToValue.get(key);
                    Vect dataRGB = colormap.apply(VectSource.create1D(dataValue));
                    Vect dataHSV = hsv(dataRGB);

                    hue = dataHSV.getX();
                    sat = dataHSV.getY();
                }

                outIndices.set(sample, mapLabelToIndex.get(label));
            }
            else if (blended.contains(label))
            {
                hue = 0;
                sat = 0;
                val = 0;
            }

            out.set(sample, rgb(VectSource.create3D(hue, sat, val)));
        }

        this.indices = outIndices;
        this.output = out;

        return this;
    }

    private static int pack(Vect rgb)
    {
        int r = (int) (255 * rgb.getX());
        int g = (int) (255 * rgb.getY());
        int b = (int) (255 * rgb.getZ());

        int packed = new Color(r, g, b).getRGB();

        return packed;
    }

    private static Vect hsv(Vect rgb)
    {
        int cr = (int) (255 * rgb.getX());
        int cg = (int) (255 * rgb.getY());
        int cb = (int) (255 * rgb.getZ());

        float[] hsb = new float[3];
        Color.RGBtoHSB(cr, cg, cb, hsb);

        return VectSource.create(hsb);
    }

    private static Vect rgb(Vect hsv)
    {
        Color ncolor = new Color(Color.HSBtoRGB((float) hsv.getX(), (float) hsv.getY(), (float) hsv.getZ()));

        double nr = ((double) ncolor.getRed()) / 255.0;
        double ng = ((double) ncolor.getGreen()) / 255.0;
        double nb = ((double) ncolor.getBlue()) / 255.0;

        return VectSource.create3D(nr, ng, nb);
    }
}
