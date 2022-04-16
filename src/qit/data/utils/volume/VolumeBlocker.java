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

package qit.data.utils.volume;

import qit.base.Global;
import qit.base.structs.Integers;
import qit.base.utils.PathUtils;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VolumeSource;
import qit.math.structs.Quaternion;
import qit.math.utils.MathUtils;

import java.io.IOException;

public class VolumeBlocker
{
    private Sampling sampling = null;
    private Integer blockSizeI = null;
    private Integer blockSizeJ = null;
    private Integer blockSizeK = null;

    private Integer blockCountI = null;
    private Integer blockCountJ = null;
    private Integer blockCountK = null;

    public VolumeBlocker()
    {
    }

    public VolumeBlocker withSampling(Sampling v)
    {
        this.sampling = v;

        return this;
    }

    public VolumeBlocker withBlockSizeI(Integer v)
    {
        this.blockSizeI = v;

        return this;
    }

    public VolumeBlocker withBlockSizeJ(Integer v)
    {
        this.blockSizeJ = v;

        return this;
    }

    public VolumeBlocker withBlockSizeK(Integer v)
    {
        this.blockSizeK = v;

        return this;
    }

    public int getNumBlocks()
    {
        this.init();
        return this.blockCountI * this.blockCountJ * this.blockCountK;
    }

    private VolumeBlocker init()
    {
        Global.assume(this.sampling != null, "no sampling found");

        int ni = this.sampling.numI();
        int nj = this.sampling.numJ();
        int nk = this.sampling.numK();

        if (this.blockSizeI == null || this.blockSizeI == 0)
        {
            this.blockSizeI = ni;
        }

        if (this.blockSizeJ == null || this.blockSizeJ == 0)
        {
            this.blockSizeJ = nj;
        }

        if (this.blockSizeK == null || this.blockSizeK == 0)
        {
            this.blockSizeK = nk;
        }

        this.blockCountI = (int) Math.ceil(ni / (double) this.blockSizeI);
        this.blockCountJ = (int) Math.ceil(nj / (double) this.blockSizeJ);
        this.blockCountK = (int) Math.ceil(nk / (double) this.blockSizeK);

        return this;
    }

    public Volume read(String pattern) throws IOException
    {
        String protoFn = String.format(pattern, 0);
        Volume protoVol = Volume.read(protoFn);
        Volume volume = VolumeSource.create(this.sampling, protoVol.getDim());
        int blocks = this.getNumBlocks();
        this.setBlock(volume, 0, protoVol);
        for (int i = 1; i < blocks; i++)
        {
            String blockFn = String.format(pattern, i);
            Volume blockVol = Volume.read(blockFn);
            this.setBlock(volume, i, blockVol);
        }

        return volume;
    }

    public void write(Volume volume, String pattern) throws IOException
    {
        int blocks = this.getNumBlocks();
        for (int i = 0; i < blocks; i++)
        {
            Volume blockVol = this.getBlock(volume, i);
            String blockFn = String.format(pattern, i);
            PathUtils.mkpar(blockFn);
            blockVol.write(blockFn);
        }
    }

    public void setBlock(Volume volume, int idx, Volume block)
    {
        this.init();

        int i = idx % this.blockCountI;
        int tmp = (idx - i) / this.blockCountI;
        int j = tmp % this.blockCountJ;
        int k = (tmp - j) / this.blockCountJ;

        int si = this.blockSizeI * i;
        int sj = this.blockSizeJ * j;
        int sk = this.blockSizeK * k;

        Sampling sampling = volume.getSampling();
        for (Sample blockSample : block.getSampling())
        {
            Sample volumeSample = new Sample(blockSample.getI() + si, blockSample.getJ() + sj, blockSample.getK() + sk);
            if (sampling.contains(volumeSample))
            {
                volume.set(volumeSample, block.get(blockSample));
            }
        }
    }

    public Volume getBlock(Volume volume, int idx)
    {
        this.init();

        Sampling volumeSampling = volume.getSampling();

        int i = idx % blockCountI;
        int tmp = (idx - i) / blockCountI;
        int j = tmp % blockCountJ;
        int k = (tmp - j) / blockCountJ;

        int si = this.blockSizeI * i;
        int sj = this.blockSizeJ * j;
        int sk = this.blockSizeK * k;

        Vect delta = volumeSampling.delta();
        Vect start = volumeSampling.world(i, j, k);
        Quaternion quat = volumeSampling.quat();
        Integers num = new Integers(this.blockSizeI, this.blockSizeJ, this.blockSizeK);

        Sampling blockSampling = new Sampling(start, delta, quat, num);
        Volume block = VolumeSource.create(blockSampling, volume.getDim());
        block.setModel(volume.getModel());

        for (Sample blockSample : blockSampling)
        {
            Sample volumeSample = new Sample(blockSample.getI() + si, blockSample.getJ() + sj, blockSample.getK() + sk);
            if (volumeSampling.contains(volumeSample))
            {
                block.set(blockSample, volume.get(volumeSample));
            }
        }

        return block;
    }
}
