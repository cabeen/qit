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
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Mask;
import qit.data.datasets.Record;
import qit.data.datasets.Sample;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.modules.vects.VectsModeMeanShift;
import qit.data.utils.TableUtils;

import java.util.Map;

@ModuleUnlisted
@ModuleDescription("Compute the modes of a volume within each region using mean shift")
@ModuleAuthor("Ryan Cabeen")
public class VolumeRegionModesMeanShift implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    private Volume input;

    @ModuleParameter
    @ModuleDescription("a mask encoding regions")
    private Mask regions;

    @ModuleParameter
    @ModuleDescription("the mean shift spatial bandwidth")
    public Double bandwidth = 1.0;

    @ModuleParameter
    @ModuleDescription("the mean shift error threshold for convergence")
    public Double minshift = 1e-3;

    @ModuleParameter
    @ModuleDescription("the mean shift maximum iterations")
    public Integer maxiter = 1000;

    @ModuleParameter
    @ModuleDescription("report the highest single variate peak (default returns highest mass)")
    public boolean peak = false;

    @ModuleParameter
    @ModuleDescription("a table encoding region names")
    private Table lookup;

    @ModuleParameter
    @ModuleDescription("specify the lookup index field")
    private String index = "index";

    @ModuleParameter
    @ModuleDescription("specify the lookup name field")
    private String name = "name";

    @ModuleParameter
    @ModuleDescription("specify the output field name for the mode")
    private String mode = "mode";

    @ModuleParameter
    @ModuleDescription("specify the output field name for the mode")
    private String mass = "mass";

    @ModuleOutput
    @ModuleDescription("output table")
    private Table output;

    @Override
    public Module run()
    {
        Logging.info("started mode seeking");

        Map<Integer, String> lut = TableUtils.createIntegerStringLookup(this.lookup, this.index, this.name);

        Map<Integer,Vects> regions = Maps.newHashMap();
        for (Sample sample : this.input.getSampling())
        {
            int label = this.regions.get(sample);
            Vect value = this.input.get(sample);

            if (label > 0 && lut.containsKey(label))
            {
                if (!regions.containsKey(label))
                {
                    regions.put(label, new Vects());
                }

                regions.get(label).add(value);
            }
        }

        Table out = new Table();
        out.withField(this.name);
        out.withField(this.index);
        out.withField(this.mass);

        if (this.input.getDim() != 1)
        {
            for (int i = 0; i < this.input.getDim(); i++)
            {
                out.withField(String.format("%s%d", this.mode, i));
            }
        }
        else
        {
            out.withField(this.mode);
        }

        for (int label : lut.keySet())
        {
            Logging.info("... processing region: " +  lut.get(label));

            VectsModeMeanShift moder = new VectsModeMeanShift();
            moder.input = regions.get(label);
            moder.minshift = this.minshift;
            moder.maxiter = this.maxiter;
            moder.bandwidth = this.bandwidth;
            moder.run();

            if (moder.modes.size() == 0)
            {
                Logging.info("...... no modes found!");
                continue;
            }

            for (int i = 0; i < moder.modes.size(); i++)
            {
                Logging.info(String.format("...... mass: %g, mode: %s", moder.masses.get(i).get(0), moder.modes.get(i).toString()));
            }

            Double mass = moder.masses.get(0).get(0);;
            Vect mode = moder.modes.get(0);

            if (this.peak)
            {
                for (int i = 0; i < moder.modes.size(); i++)
                {
                    Vect check = moder.modes.get(i);
                    if (check.get(0) > mode.get(0))
                    {
                        mode = check;
                        mass = moder.masses.get(i).get(0);
                    }
                }
            }
            else
            {
                for (int i = 0; i < moder.modes.size(); i++)
                {
                    double check = moder.masses.get(i).get(0);
                    if (check > mass)
                    {
                        mode = moder.modes.get(0);
                        mass = check;
                    }
                }
            }

            Record record = new Record();
            record.with(this.name, lut.get(label));
            record.with(this.index, label);
            record.with(this.mass, String.valueOf(mass));

            if (this.input.getDim() != 1)
            {
                for (int i = 0; i < this.input.getDim(); i++)
                {
                    record.with(String.format("%s%d", this.mode, i), String.valueOf(mode.get(i)));
                }
            }
            else
            {
                record.with(this.mode, String.valueOf(mode.get(0)));
            }

            out.addRecord(record);
        }

        Logging.info("finished mode seeking");

        this.output = out;

        return this;
    }
}
