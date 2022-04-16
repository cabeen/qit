/*******************************************************************************
 * Copyright (c) 2010-2016, Ryan Cabeen
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 * must display the following acknowledgement:
 * This product includes software developed by the Ryan Cabeen.
 * 4. Neither the name of the Ryan Cabeen nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY RYAN CABEEN ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL RYAN CABEEN BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package qit.data.modules.volume;

import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.cli.CliUtils;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Volume;
import qit.math.utils.MathUtils;

import java.util.List;

@ModuleDescription("Filter a volume using a PDE representing anisotropic mri")
@ModuleAuthor("Ryan Cabeen")
@ModuleCitation("Perona, P., & Malik, J. (1990). Scale-space and edge detection using anisotropic mri. IEEE Transactions on pattern analysis and machine intelligence, 12(7), 629-639.")
public class VolumeFilterPDE implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the volume channel(s) (default applies to all)")
    public String channel;

    @ModuleParameter
    @ModuleDescription("smoothing parameter (in mm)")
    public Double lambda = 1.0 / 6.0;

    @ModuleParameter
    @ModuleDescription("anisotropic flux parameter (relative to image intensities)")
    public Double k = 0.01;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the number of timesteps to apply")
    public Integer steps = 5;

    @ModuleParameter
    @ModuleDescription("use anisotropic smoothing")
    public boolean anisotropic = false;

    @ModuleParameter
    @ModuleDescription("use a quadratic flux (instead of exponential)")
    public boolean quadratic = false;

    @ModuleParameter
    @ModuleDescription("use the output as input")
    public boolean recurse = false;

    @ModuleOutput
    @ModuleDescription("output volume")
    public Volume output = null;

    public VolumeFilterPDE run()
    {
        Global.assume(MathUtils.nonzero(this.k), "k must be nonzero");

        Volume invol = (this.recurse && this.output != null) ? this.output : this.input;

        Sampling sampling = invol.getSampling();
        Volume a = invol.copy();
        Volume b = invol.copy();

        int nx = sampling.numI();
        int ny = sampling.numJ();
        int nz = sampling.numK();

        double dx = sampling.deltaI();
        double dy = sampling.deltaJ();
        double dz = sampling.deltaK();

        dx = MathUtils.zero(dx) ? 1.0 : dx;
        dy = MathUtils.zero(dy) ? 1.0 : dy;
        dz = MathUtils.zero(dz) ? 1.0 : dz;

        double invk = 1.0 / this.k;

        List<Integer> channels = CliUtils.parseIndexList(this.channel, invol.getDim());

        Logging.info("started pde filtering");
        for (int t = 0; t < this.steps; t++)
        {
            Logging.info(String.format("... on time step %d / %d", t + 1, this.steps));

            for (Integer d : channels)
            {
                if (channels.size() > 1)
                {
                    Logging.info(String.format("...... on channel %d", d));
                }

                for (Sample s : sampling)
                {
                    if (invol.valid(s, this.mask))
                    {
                        int i = s.getI();
                        int j = s.getJ();
                        int k = s.getK();
                        double vc = a.get(i, j, k, d);

                        int ip = (i == 0) ? i : i - 1;
                        int in = (i == nx - 1) ? i : i + 1;
                        int jp = (j == 0) ? j : j - 1;
                        int jn = (j == ny - 1) ? j : j + 1;
                        int kp = (k == 0) ? k : k - 1;
                        int kn = (k == nz - 1) ? k : k + 1;

                        double vip = a.get(ip, j, k, d);
                        double vin = a.get(in, j, k, d);
                        double vjp = a.get(i, jp, k, d);
                        double vjn = a.get(i, jn, k, d);
                        double vkp = a.get(i, j, kp, d);
                        double vkn = a.get(i, j, kn, d);

                        double gip = (vip - vc) / dx;
                        double gin = (vin - vc) / dx;
                        double gjp = (vjp - vc) / dy;
                        double gjn = (vjn - vc) / dy;
                        double gkp = (vkp - vc) / dz;
                        double gkn = (vkn - vc) / dz;

                        if (this.anisotropic)
                        {
                            if (this.quadratic)
                            {
                                gip *= 1.0 / (1.0 + (invk * gip * gip));
                                gin *= 1.0 / (1.0 + (invk * gin * gin));
                                gjp *= 1.0 / (1.0 + (invk * gjp * gjp));
                                gjn *= 1.0 / (1.0 + (invk * gjn * gjn));
                                gkp *= 1.0 / (1.0 + (invk * gkp * gkp));
                                gkn *= 1.0 / (1.0 + (invk * gkn * gkn));
                            }
                            else
                            {
                                gip *= Math.exp(-invk * gip * gip);
                                gin *= Math.exp(-invk * gin * gin);
                                gjp *= Math.exp(-invk * gjp * gjp);
                                gjn *= Math.exp(-invk * gjn * gjn);
                                gkp *= Math.exp(-invk * gkp * gkp);
                                gkn *= Math.exp(-invk * gkn * gkn);
                            }
                        }

                        double v = vc + this.lambda * (gip + gin + gjp + gjn + gkp + gkn);

                        b.set(i, j, k, d, v);
                    }
                }
            }

            Volume tmp = a;
            a = b;
            b = tmp;
        }
        Logging.info("finished pde filtering");

        this.output = a;

        return this;
    }
}
