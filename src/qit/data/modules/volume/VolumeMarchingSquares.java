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
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;

import java.util.List;
import java.util.Map;
import java.util.Set;

@ModuleDescription("Extract isocontours from slices of a volume")
@ModuleAuthor("Ryan Cabeen")
@ModuleCitation("Maple, C. (2003, July). Geometric design and space planning using the marching squares and marching cube algorithms. In Geometric Modeling and Graphics, 2003. Proceedings. 2003 International Conference on (pp. 90-95). IEEE.")
public class VolumeMarchingSquares implements Module
{
    private enum Dimension
    {
        X(0), Y(1), Z(2), D(3);

        private int idx;

        public int getIdx()
        {
            return this.idx;
        }

        private Dimension(int idx)
        {
            this.idx = idx;
        }
    }


    @ModuleInput
    @ModuleDescription("input volume")
    private Volume input;
    
    @ModuleParameter
    @ModuleDescription("level set to extract")
    private double level = 0;
    
    @ModuleParameter
    @ModuleDescription("background value for outside sampling range")
    private double background = Double.MAX_VALUE;

    @ModuleParameter
    @ModuleDescription("volume channel to contour (x, y, or z)")
    private String dim = "z";

    @ModuleParameter
    @ModuleDescription("starting slice index")
    private int start = 0;
    
    @ModuleParameter
    @ModuleDescription("number of slices between contours")
    private int step = 1;

    @ModuleOutput
    @ModuleDescription("output contours")
    private Curves output;

    public VolumeMarchingSquares withInput(Volume input)
    {
        this.input = input;
        this.output = null;

        return this;
    }

    public VolumeMarchingSquares withLevel(double v)
    {
        this.level = v;
        this.output = null;

        return this;
    }

    public VolumeMarchingSquares withBackground(double v)
    {
        this.background = v;
        this.output = null;

        return this;
    }

    public VolumeMarchingSquares withStart(int v)
    {
        this.start = v;
        this.output = null;

        return this;
    }

    public VolumeMarchingSquares withStep(int v)
    {
        this.step = v;
        this.output = null;

        return this;
    }
    
    public VolumeMarchingSquares withDim(String d)
    {
        this.dim = d;
        this.output = null;

        return this;
    }

    public VolumeMarchingSquares run()
    {
        Curves contours = new Curves();

        int axis = Dimension.valueOf(this.dim.toUpperCase()).getIdx();
        
        Sampling sampling = this.input.getSampling();
        int idxI = axis;
        int idxJ = (idxI + 1) % 3;
        int idxK = (idxI + 2) % 3;

        int numI = sampling.num(idxI);
        int numJ = sampling.num(idxJ);
        int numK = sampling.num(idxK);

        int[] idxA = new int[3];
        int[] idxB = new int[3];
        int[] idxC = new int[3];
        int[] idxD = new int[3];

        List<Integer> is = Lists.newArrayList();
        is.add(this.start);
        if (this.step > 0)
        {
            for (int i = this.start; i < numI; i += this.step)
            {
                is.add(i);
            }
            for (int i = this.start; i >= 0; i -= this.step)
            {
                is.add(i);
            }
        }

        for (Integer i : is)
        {
            idxA[idxI] = i;
            idxB[idxI] = i;
            idxC[idxI] = i;
            idxD[idxI] = i;

            Marcher marcher = new Marcher(sampling);

            for (int j = -1; j <= numJ; j++)
            {
                idxA[idxJ] = j;
                idxB[idxJ] = j;
                idxC[idxJ] = j + 1;
                idxD[idxJ] = j + 1;

                for (int k = -1; k <= numK; k++)
                {
                    idxA[idxK] = k;
                    idxB[idxK] = k + 1;
                    idxC[idxK] = k + 1;
                    idxD[idxK] = k;

                    Sample sampA = new Sample(idxA);
                    Sample sampB = new Sample(idxB);
                    Sample sampC = new Sample(idxC);
                    Sample sampD = new Sample(idxD);

                    double valA = this.background;
                    double valB = this.background;
                    double valC = this.background;
                    double valD = this.background;

                    if (sampling.contains(sampA))
                    {
                        valA = this.input.get(sampA, 0);
                    }

                    if (sampling.contains(sampB))
                    {
                        valB = this.input.get(sampB, 0);
                    }

                    if (sampling.contains(sampC))
                    {
                        valC = this.input.get(sampC, 0);
                    }

                    if (sampling.contains(sampD))
                    {
                        valD = this.input.get(sampD, 0);
                    }

                    boolean belowA = valA < this.level;
                    boolean belowB = valB < this.level;
                    boolean belowC = valC < this.level;
                    boolean belowD = valD < this.level;

                    MarcherVertex pairAB = new MarcherVertex(sampA, sampB);
                    MarcherVertex pairBC = new MarcherVertex(sampB, sampC);
                    MarcherVertex pairCD = new MarcherVertex(sampC, sampD);
                    MarcherVertex pairDA = new MarcherVertex(sampD, sampA);

                    // assume:
                    // f(x) = m * x + b
                    // f(0) = valLeft
                    // f(1) = valRight
                    // b = valLeft
                    // m = valRight - valLeft
                    // zero = -b / m = -valLeft / (valRight - valLeft)

                    if (belowA != belowB)
                    {
                        marcher.add(pairAB, (this.level - valA) / (valB - valA));
                    }

                    if (belowB != belowC)
                    {
                        marcher.add(pairBC, (this.level - valB) / (valC - valB));
                    }

                    if (belowC != belowD)
                    {
                        marcher.add(pairCD, (this.level - valC) / (valD - valC));
                    }

                    if (belowD != belowA)
                    {
                        marcher.add(pairDA, (this.level - valD) / (valA - valD));
                    }

                    if (belowA && belowB && belowC && belowD || !belowA && !belowB && !belowC && !belowD)
                    {
                        // no change
                        continue;
                    }
                    else if (!belowA && belowB && !belowC && belowD || belowA && !belowB && belowC && !belowD)
                    {
                        // two edges needed

                        // try to avoid ambiguity with a center value
                        double vCent = (valA + valB + valC + valD) / 4.0;
                        boolean bCent = vCent < this.level;

                        if (bCent == belowA)
                        {
                            marcher.add(pairAB, pairDA);
                            marcher.add(pairBC, pairCD);
                        }
                        else
                        {
                            marcher.add(pairAB, pairBC);
                            marcher.add(pairCD, pairDA);
                        }
                    }
                    else if (!belowA && !belowB && !belowC && belowD || belowA && belowB && belowC && !belowD)
                    {
                        marcher.add(pairCD, pairDA);
                    }
                    else if (!belowA && !belowB && belowC && !belowD || belowA && belowB && !belowC && belowD)
                    {
                        marcher.add(pairBC, pairCD);
                    }
                    else if (!belowA && belowB && !belowC && !belowD || belowA && !belowB && belowC && belowD)
                    {
                        marcher.add(pairAB, pairBC);
                    }
                    else if (belowA && !belowB && !belowC && !belowD || !belowA && belowB && belowC && belowD)
                    {
                        marcher.add(pairDA, pairAB);
                    }
                    else if (!belowA && !belowB && belowC && belowD || belowA && belowB && !belowC && !belowD)
                    {
                        marcher.add(pairBC, pairDA);
                    }
                    else if (!belowA && belowB && belowC && !belowD || belowA && !belowB && !belowC && belowD)
                    {
                        marcher.add(pairAB, pairCD);
                    }
                    else
                    {
                        Logging.error("bug: forgot a case!");
                    }
                }
            }
            contours.add(marcher.build());
        }

        this.output = contours;

        return this;
    }

    public Curves getOutput()
    {
        if (this.output == null)
        {
            this.run();
        }

        return this.output;
    }

    private static class MarcherVertex
    {
        Sample a;
        Sample b;

        private MarcherVertex(Sample a, Sample b)
        {
            this.a = a;
            this.b = b;
        }

        public int hashCode()
        {
            return this.a.hashCode() + this.b.hashCode();
        }

        public boolean equals(Object obj)
        {
            if (!(obj instanceof MarcherVertex))
            {
                return false;
            }

            if (obj == this)
            {
                return true;
            }

            MarcherVertex e = (MarcherVertex) obj;
            if (e.a.equals(this.a) && e.b.equals(this.b))
            {
                return true;
            }

            return e.a.equals(this.b) && e.b.equals(this.a);
        }
    }

    private static class MarcherTopology
    {
        Double zero = null;
        Set<MarcherVertex> verts = Sets.newHashSet();

        private void add(MarcherVertex vert)
        {
            this.verts.add(vert);
        }

        private MarcherVertex getOther(MarcherVertex vert)
        {
            for (MarcherVertex v : this.verts)
            {
                if (!v.equals(vert))
                {
                    return v;
                }
            }

            throw new RuntimeException("invalid topo");
        }

        private void validate()
        {
            Global.assume(this.zero != null, "no zero in topo");
            Global.assume(this.verts.size() == 2, "wrong count in topo");
        }
    }

    private static class Marcher
    {
        Sampling sampling;
        Map<MarcherVertex, MarcherTopology> model = Maps.newHashMap();

        private Marcher(Sampling sampling)
        {
            this.sampling = sampling;
        }

        void add(MarcherVertex pair, double zero)
        {
            if (!this.model.containsKey(pair))
            {
                this.model.put(pair, new MarcherTopology());
            }

            this.model.get(pair).zero = zero;
        }

        void add(MarcherVertex a, MarcherVertex b)
        {
            if (!this.model.containsKey(a))
            {
                this.model.put(a, new MarcherTopology());
            }

            this.model.get(a).add(b);

            if (!this.model.containsKey(b))
            {
                this.model.put(b, new MarcherTopology());
            }

            this.model.get(b).add(a);
        }

        Curves build()
        {
            Curves curves = new Curves();

            Set<MarcherVertex> remaining = Sets.newHashSet(this.model.keySet());

            for (MarcherVertex v : remaining)
            {
                this.model.get(v).validate();
            }

            while (!remaining.isEmpty())
            {
                Logging.info("building loop");

                Vects points = new Vects();

                MarcherVertex start = null;
                for (MarcherVertex pair : remaining)
                {
                    start = pair;
                    break;
                }
                remaining.remove(start);

                MarcherVertex prev = null;
                MarcherVertex curr = start;
                do
                {
                    // make the static checker happy, but we know better!
                    Global.assume(curr != null, "invalid topology");
                    assert (curr != null);

                    MarcherTopology topo = this.model.get(curr);

                    double zero = topo.zero;
                    Vect posA = this.sampling.world(curr.a);
                    Vect posB = this.sampling.world(curr.b);
                    Vect BdelA = posB.minus(posA);
                    Vect pos = posA.plus(zero, BdelA);

                    points.add(pos);

                    MarcherVertex next = topo.getOther(prev);
                    remaining.remove(next);

                    prev = curr;
                    curr = next;
                }
                while (!curr.equals(start));
                points.add(points.get(0));

                Curve curve = curves.add(points.size());
                for (int i = 0; i < points.size(); i++)
                {
                    curve.set(i, points.get(i));
                }
            }

            return curves;
        }
    }
}
