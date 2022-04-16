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

package qit.math.utils.colormaps;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import qit.base.Global;
import qit.base.JsonDataset;
import qit.base.Logging;
import qit.base.utils.PathUtils;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ColormapDiscrete extends JsonDataset
{
    private static final String INDEX_FIELD = "index";
    private static final String RED_FIELD = "r";
    private static final String GREEN_FIELD = "g";
    private static final String BLUE_FIELD = "b";
    private static final String ALPHA_FIELD = "a";
    private static final String DEFAULT = "NA";

    private transient Map<Integer, ColorRelation> lookup = Maps.newLinkedHashMap();
    private transient VectFunction function;
    private transient ColorRelation missing = new ColorRelation();

    private String name = "default";
    private List<ColorRelation> relations = null;
    private boolean periodic = true;

    public ColormapDiscrete()
    {
        this.update();
    }

    public ColormapDiscrete withName(String v)
    {
        this.name = v;
        return this;
    }

    public ColormapDiscrete clear()
    {
        this.relations.clear();
        return this.update();
    }

    public ColormapDiscrete withRelation(int label, Vect color)
    {
        this.relations.add(new ColorRelation().withLabel(label).withColor(color));
        return this.update();
    }

    public ColormapDiscrete withPeriodic(boolean v)
    {
        this.periodic = v;
        return this.update();
    }

    public Set<Integer> getLabels()
    {
        return lookup.keySet();
    }

    public Vect getColor(int label)
    {
        return this.lookup.containsKey(label) ? this.lookup.get(label).color : this.missing.color;
    }

    public String getName()
    {
        return this.name;
    }

    public boolean getPeriodic()
    {
        return this.periodic;
    }

    public VectFunction getFunction()
    {
        return this.function;
    }

    public int size()
    {
        return this.relations.size();
    }

    private ColormapDiscrete update()
    {
        if (this.relations == null)
        {
            this.relations = Lists.newArrayList();
        }

        this.lookup.clear();
        for (ColorRelation r : this.relations)
        {
            this.lookup.put(r.label, r);
        }

        this.function = new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                int count = ColormapDiscrete.this.relations.size();
                int label = MathUtils.round(input.get(0));
                boolean has = ColormapDiscrete.this.lookup.containsKey(label);

                if (!has && ColormapDiscrete.this.periodic && count > 0 && label != 0)
                {
                    label = (label % count) + 1;
                }

                if (has)
                {
                    output.set(ColormapDiscrete.this.lookup.get(label).color);
                }
                else if (count > 0 && label > 0)
                {
                    output.set(ColormapDiscrete.this.relations.get(count - 1).color);
                }
                else
                {
                    output.setAll(0);
                }
            }
        }.init(1, 4);

        return this;
    }

    private static class ColorRelation
    {
        Integer label = 0;
        Vect color = VectSource.create4D(1, 1, 1, 1);

        private ColorRelation withLabel(int v)
        {
            this.label = v;
            return this;
        }

        private ColorRelation withColor(Vect v)
        {
            if (v.size() == 3)
            {
                this.color.set(0, v.get(0));
                this.color.set(1, v.get(1));
                this.color.set(2, v.get(2));
            }
            else if (v.size() == 4)
            {
                this.color.set(v);
            }
            else
            {
                Logging.error("invalid color: " + v.toString());
            }

            this.color = v;
            return this;
        }

        public ColorRelation copy()
        {
            return new ColorRelation().withLabel(this.label).withColor(this.color.copy());
        }
    }

    public String toString()
    {
        return this.name;
    }

    public static ColormapDiscrete create(Table table)
    {
        Global.assume(table.hasField(INDEX_FIELD), "missing index field");

        boolean hasRed = table.hasField(RED_FIELD);
        boolean hasGreen = table.hasField(GREEN_FIELD);
        boolean hasBlue = table.hasField(BLUE_FIELD);
        boolean hasColor = hasRed && hasGreen && hasBlue;
        boolean hasAlpha = table.hasField(ALPHA_FIELD);

        ColormapDiscrete out = new ColormapDiscrete();
        List<Vect> colors = ColormapSource.random(table.getNumRecords());

        int count = 0;
        for (Integer key : table.getKeys())
        {
            Record row = table.getRecord(key);
            Integer label = Integer.valueOf(row.get(INDEX_FIELD));

            if (hasColor)
            {
                Integer red = Integer.valueOf(row.get(RED_FIELD));
                Integer green = Integer.valueOf(row.get(GREEN_FIELD));
                Integer blue = Integer.valueOf(row.get(BLUE_FIELD));

                double r = red / 255.0f;
                double g = green / 255.0f;
                double b = blue / 255.0f;

                Vect color = VectSource.create4D(r, g, b, 1.0);

                if (hasAlpha)
                {
                    Integer alpha = Integer.valueOf(row.get(ALPHA_FIELD));
                    double a = alpha / 255.0f;
                    color.set(3, a);
                }

                out.withRelation(label, color);
            }
            else
            {
                out.withRelation(label, colors.get(count));
                count += 1;
            }
        }

        return out;
    }

    public ColormapDiscrete set(ColormapDiscrete c)
    {
        this.clear();

        this.name = c.name;
        this.periodic = c.periodic;

        this.relations.clear();
        for (ColorRelation relation : c.relations)
        {
            this.relations.add(relation.copy());
        }

        return this.update();
    }

    @Override
    public ColormapDiscrete copy()
    {
        return new ColormapDiscrete().set(this);
    }

    public static ColormapDiscrete read(String fn) throws IOException
    {
        if (fn.endsWith(".json"))
        {
            Logging.info("loading json colormap: " + fn);
            return JsonDataset.read(ColormapDiscrete.class, fn).update();
        }
        else if (fn.endsWith(".csv"))
        {
            Logging.info("loading csv colormap: " + fn);
            ColormapDiscrete cmap = ColormapDiscrete.create(Table.read(fn));
            cmap.withName(PathUtils.basename(fn).split(".csv")[0]);
            return cmap;
        }
        else
        {
            Logging.error("failed to detect file format: " + fn);
            return null;
        }
    }
}