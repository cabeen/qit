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


package qit.data.modules.mask;

import com.google.common.collect.Lists;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;

import java.util.List;

@ModuleDescription("Skeletonize a mask using medial axis thinning.  Based on Hanno Homan's implementation of Lee et al. at http://hdl.handle.net/1926/1292")
@ModuleCitation("Lee, Ta-Chih, Rangasami L. Kashyap, and Chong-Nam Chu. Building skeleton models via 3-D medial surface axis thinning algorithms. CVGIP: Graphical Models and Image Processing 56.6 (1994): 462-478.")
@ModuleAuthor("Ryan Cabeen")
public class MaskSkeleton implements Module
{
    private static final int[] LUT = new int[256];

    static
    {
        LUT[1] = 1;
        LUT[3] = -1;
        LUT[5] = -1;
        LUT[7] = 1;
        LUT[9] = -3;
        LUT[11] = -1;
        LUT[13] = -1;
        LUT[15] = 1;
        LUT[17] = -1;
        LUT[19] = 1;
        LUT[21] = 1;
        LUT[23] = -1;
        LUT[25] = 3;
        LUT[27] = 1;
        LUT[29] = 1;
        LUT[31] = -1;
        LUT[33] = -3;
        LUT[35] = -1;
        LUT[37] = 3;
        LUT[39] = 1;
        LUT[41] = 1;
        LUT[43] = -1;
        LUT[45] = 3;
        LUT[47] = 1;
        LUT[49] = -1;
        LUT[51] = 1;

        LUT[53] = 1;
        LUT[55] = -1;
        LUT[57] = 3;
        LUT[59] = 1;
        LUT[61] = 1;
        LUT[63] = -1;
        LUT[65] = -3;
        LUT[67] = 3;
        LUT[69] = -1;
        LUT[71] = 1;
        LUT[73] = 1;
        LUT[75] = 3;
        LUT[77] = -1;
        LUT[79] = 1;
        LUT[81] = -1;
        LUT[83] = 1;
        LUT[85] = 1;
        LUT[87] = -1;
        LUT[89] = 3;
        LUT[91] = 1;
        LUT[93] = 1;
        LUT[95] = -1;
        LUT[97] = 1;
        LUT[99] = 3;
        LUT[101] = 3;
        LUT[103] = 1;

        LUT[105] = 5;
        LUT[107] = 3;
        LUT[109] = 3;
        LUT[111] = 1;
        LUT[113] = -1;
        LUT[115] = 1;
        LUT[117] = 1;
        LUT[119] = -1;
        LUT[121] = 3;
        LUT[123] = 1;
        LUT[125] = 1;
        LUT[127] = -1;
        LUT[129] = -7;
        LUT[131] = -1;
        LUT[133] = -1;
        LUT[135] = 1;
        LUT[137] = -3;
        LUT[139] = -1;
        LUT[141] = -1;
        LUT[143] = 1;
        LUT[145] = -1;
        LUT[147] = 1;
        LUT[149] = 1;
        LUT[151] = -1;
        LUT[153] = 3;
        LUT[155] = 1;

        LUT[157] = 1;
        LUT[159] = -1;
        LUT[161] = -3;
        LUT[163] = -1;
        LUT[165] = 3;
        LUT[167] = 1;
        LUT[169] = 1;
        LUT[171] = -1;
        LUT[173] = 3;
        LUT[175] = 1;
        LUT[177] = -1;
        LUT[179] = 1;
        LUT[181] = 1;
        LUT[183] = -1;
        LUT[185] = 3;
        LUT[187] = 1;
        LUT[189] = 1;
        LUT[191] = -1;
        LUT[193] = -3;
        LUT[195] = 3;
        LUT[197] = -1;
        LUT[199] = 1;
        LUT[201] = 1;
        LUT[203] = 3;
        LUT[205] = -1;
        LUT[207] = 1;

        LUT[209] = -1;
        LUT[211] = 1;
        LUT[213] = 1;
        LUT[215] = -1;
        LUT[217] = 3;
        LUT[219] = 1;
        LUT[221] = 1;
        LUT[223] = -1;
        LUT[225] = 1;
        LUT[227] = 3;
        LUT[229] = 3;
        LUT[231] = 1;
        LUT[233] = 5;
        LUT[235] = 3;
        LUT[237] = 3;
        LUT[239] = 1;
        LUT[241] = -1;
        LUT[243] = 1;
        LUT[245] = 1;
        LUT[247] = -1;
        LUT[249] = 3;
        LUT[251] = 1;
        LUT[253] = 1;
        LUT[255] = -1;
    }

    @ModuleInput
    @ModuleDescription("input mask")
    public Mask input;

    @ModuleOutput
    @ModuleDescription("output mask")
    public Mask output;

    @Override
    public MaskSkeleton run()
    {
        Mask in = this.input.copy();
        Mask out = in.copy();

        thin(out);

        this.output = out;
        return this;
    }

    public void thin(Mask mask)
    {
        Logging.info("started thinning mask");

        Sampling sampling = mask.getSampling();

        int iterations = 0;
        int unchangedBorders = 0;
        List<Sample> simple = Lists.newArrayList();

        while (unchangedBorders < 6)
        {
            Logging.info("... started thinning iteration");

            unchangedBorders = 0;
            iterations++;
            for (int currentBorder = 1; currentBorder <= 6; currentBorder++)
            {
                boolean noChange = true;

                for (Sample sample : sampling)
                {
                    if (mask.get(sample) != 1)
                    {
                        continue;
                    }

                    int i = sample.getI();
                    int j = sample.getJ();
                    int k = sample.getK();

                    boolean isBorderPoint = false;

                    if (currentBorder == 1 && getLabelSafe(mask, i, j - 1, k) == 0)
                    {
                        isBorderPoint = true;
                    }

                    if (currentBorder == 2 && getLabelSafe(mask, i, j + 1, k) == 0)
                    {
                        isBorderPoint = true;
                    }

                    if (currentBorder == 3 && getLabelSafe(mask, i + 1, j, k) == 0)
                    {
                        isBorderPoint = true;
                    }

                    if (currentBorder == 4 && getLabelSafe(mask, i - 1, j, k) == 0)
                    {
                        isBorderPoint = true;
                    }

                    if (currentBorder == 5 && getLabelSafe(mask, i, j, k + 1) == 0)
                    {
                        isBorderPoint = true;
                    }

                    if (currentBorder == 6 && getLabelSafe(mask, i, j, k - 1) == 0)
                    {
                        isBorderPoint = true;
                    }

                    if (!isBorderPoint)
                    {
                        continue;
                    }

                    if (isEndPoint(mask, i, j, k))
                    {
                        continue;
                    }

                    final int[] neighborhood = getNeighborhood(mask, i, j, k);

                    if (!isEulerInvariant(neighborhood))
                    {
                        continue;
                    }

                    if (!isSimplePoint(neighborhood))
                    {
                        continue;
                    }

                    simple.add(sample);
                }

                for (Sample s : simple)
                {
                    if (isSimplePoint(getNeighborhood(mask, s.getI(), s.getJ(), s.getK())))
                    {
                        mask.set(s, 0);
                        noChange = false;
                    }
                }

                if (noChange)
                {
                    unchangedBorders++;
                }

                simple.clear();
            }
        }

        Logging.info("... iteration count: " + iterations);

        Logging.info("finished thinning mask");
    }

    boolean isEndPoint(Mask mask, int x, int y, int z)
    {
        int numberOfNeighbors = -1;
        int[] neighbor = getNeighborhood(mask, x, y, z);
        for (int i = 0; i < 27; i++)
        {
            if (neighbor[i] == 1)
            {
                numberOfNeighbors++;
            }
        }

        return numberOfNeighbors == 1;
    }

    public int[] getNeighborhood(Mask mask, int x, int y, int z)
    {
        int[] neighborhood = new int[27];

        neighborhood[0] = getLabelSafe(mask, x - 1, y - 1, z - 1);
        neighborhood[1] = getLabelSafe(mask, x, y - 1, z - 1);
        neighborhood[2] = getLabelSafe(mask, x + 1, y - 1, z - 1);

        neighborhood[3] = getLabelSafe(mask, x - 1, y, z - 1);
        neighborhood[4] = getLabelSafe(mask, x, y, z - 1);
        neighborhood[5] = getLabelSafe(mask, x + 1, y, z - 1);

        neighborhood[6] = getLabelSafe(mask, x - 1, y + 1, z - 1);
        neighborhood[7] = getLabelSafe(mask, x, y + 1, z - 1);
        neighborhood[8] = getLabelSafe(mask, x + 1, y + 1, z - 1);

        neighborhood[9] = getLabelSafe(mask, x - 1, y - 1, z);
        neighborhood[10] = getLabelSafe(mask, x, y - 1, z);
        neighborhood[11] = getLabelSafe(mask, x + 1, y - 1, z);

        neighborhood[12] = getLabelSafe(mask, x - 1, y, z);
        neighborhood[13] = getLabelSafe(mask, x, y, z);
        neighborhood[14] = getLabelSafe(mask, x + 1, y, z);

        neighborhood[15] = getLabelSafe(mask, x - 1, y + 1, z);
        neighborhood[16] = getLabelSafe(mask, x, y + 1, z);
        neighborhood[17] = getLabelSafe(mask, x + 1, y + 1, z);

        neighborhood[18] = getLabelSafe(mask, x - 1, y - 1, z + 1);
        neighborhood[19] = getLabelSafe(mask, x, y - 1, z + 1);
        neighborhood[20] = getLabelSafe(mask, x + 1, y - 1, z + 1);

        neighborhood[21] = getLabelSafe(mask, x - 1, y, z + 1);
        neighborhood[22] = getLabelSafe(mask, x, y, z + 1);
        neighborhood[23] = getLabelSafe(mask, x + 1, y, z + 1);

        neighborhood[24] = getLabelSafe(mask, x - 1, y + 1, z + 1);
        neighborhood[25] = getLabelSafe(mask, x, y + 1, z + 1);
        neighborhood[26] = getLabelSafe(mask, x + 1, y + 1, z + 1);

        return neighborhood;
    }

    private int getLabelSafe(Mask image, int x, int y, int z)
    {
        if (image.valid(x, y, z))
        {
            return image.get(x, y, z);
        }
        else
        {
            return 0;
        }
    }

    boolean isEulerInvariant(int[] neighbors)
    {
        // Calculate Euler characteristic for each octant and sum up
        int eulerChar = 0;
        char n;

        // Octant SWU
        n = indexOctantSWU(neighbors);
        eulerChar += LUT[n];

        // Octant SEU
        n = indexOctantSEU(neighbors);
        eulerChar += LUT[n];

        // Octant NWU
        n = indexOctantNWU(neighbors);
        eulerChar += LUT[n];

        // Octant NEU
        n = indexOctantNEU(neighbors);
        eulerChar += LUT[n];

        // Octant SWB
        n = indexOctantSWB(neighbors);
        eulerChar += LUT[n];

        // Octant SEB
        n = indexOctantSEB(neighbors);
        eulerChar += LUT[n];

        // Octant NWB
        n = indexOctantNWB(neighbors);
        eulerChar += LUT[n];

        // Octant NEB
        n = indexOctantNEB(neighbors);
        eulerChar += LUT[n];

        return eulerChar == 0;
    }


    boolean isSurfacePoint(int[] neighbors)
    {
        // this isn't working yet, but checking for this condition
        // should give us a medial surface instead of a medial axis

        char[] octs = new char[8];

        octs[0] = indexOctantSWU(neighbors);
        octs[1] = indexOctantSEU(neighbors);
        octs[2] = indexOctantNWU(neighbors);
        octs[3] = indexOctantNEU(neighbors);
        octs[4] = indexOctantSWB(neighbors);
        octs[5] = indexOctantSEB(neighbors);
        octs[6] = indexOctantNWB(neighbors);
        octs[7] = indexOctantNEB(neighbors);

        for (char oct : octs)
        {
            if (Integer.bitCount(oct) < 3 || oct == 240 || oct == 165 || oct == 170 || oct == 204)
            {
                continue;
            }

            return false;
        }

        return true;
    }

    private boolean isSimplePoint(int[] neighbors)
    {
        // copy neighbors for labeling
        int cube[] = new int[26];
        int i;
        for (i = 0; i < 13; i++)  // i =  0..12 -> cube[0..12]
        {
            cube[i] = neighbors[i];
        }
        // i != 13 : ignore center pixel when counting (see [Lee94])
        for (i = 14; i < 27; i++) // i = 14..26 -> cube[13..25]
        {
            cube[i - 1] = neighbors[i];
        }
        // set initial label
        int label = 2;
        // for all points in the neighborhood
        for (i = 0; i < 26; i++)
        {
            if (cube[i] == 1)     // voxel has not been labeled yet
            {
                // start recursion with any octant that contains the point i
                switch (i)
                {
                    case 0:
                    case 1:
                    case 3:
                    case 4:
                    case 9:
                    case 10:
                    case 12:
                        octreeLabeling(1, label, cube);
                        break;
                    case 2:
                    case 5:
                    case 11:
                    case 13:
                        octreeLabeling(2, label, cube);
                        break;
                    case 6:
                    case 7:
                    case 14:
                    case 15:
                        octreeLabeling(3, label, cube);
                        break;
                    case 8:
                    case 16:
                        octreeLabeling(4, label, cube);
                        break;
                    case 17:
                    case 18:
                    case 20:
                    case 21:
                        octreeLabeling(5, label, cube);
                        break;
                    case 19:
                    case 22:
                        octreeLabeling(6, label, cube);
                        break;
                    case 23:
                    case 24:
                        octreeLabeling(7, label, cube);
                        break;
                    case 25:
                        octreeLabeling(8, label, cube);
                        break;
                }
                label++;
                if (label - 2 >= 2)
                {
                    return false;
                }
            }
        }
        //return label-2; in [Lee94] if the number of connected components would be needed
        return true;
    }

    public char indexOctantNEB(int[] neighbors)
    {
        char n;
        n = 1;
        if (neighbors[2] == 1)
        {
            n |= 128;
        }
        if (neighbors[1] == 1)
        {
            n |= 64;
        }
        if (neighbors[11] == 1)
        {
            n |= 32;
        }
        if (neighbors[10] == 1)
        {
            n |= 16;
        }
        if (neighbors[5] == 1)
        {
            n |= 8;
        }
        if (neighbors[4] == 1)
        {
            n |= 4;
        }
        if (neighbors[14] == 1)
        {
            n |= 2;
        }
        return n;
    }

    public char indexOctantNWB(int[] neighbors)
    {
        char n;
        n = 1;
        if (neighbors[0] == 1)
        {
            n |= 128;
        }
        if (neighbors[9] == 1)
        {
            n |= 64;
        }
        if (neighbors[3] == 1)
        {
            n |= 32;
        }
        if (neighbors[12] == 1)
        {
            n |= 16;
        }
        if (neighbors[1] == 1)
        {
            n |= 8;
        }
        if (neighbors[10] == 1)
        {
            n |= 4;
        }
        if (neighbors[4] == 1)
        {
            n |= 2;
        }
        return n;
    }

    public char indexOctantSEB(int[] neighbors)
    {
        char n;
        n = 1;
        if (neighbors[8] == 1)
        {
            n |= 128;
        }
        if (neighbors[7] == 1)
        {
            n |= 64;
        }
        if (neighbors[17] == 1)
        {
            n |= 32;
        }
        if (neighbors[16] == 1)
        {
            n |= 16;
        }
        if (neighbors[5] == 1)
        {
            n |= 8;
        }
        if (neighbors[4] == 1)
        {
            n |= 4;
        }
        if (neighbors[14] == 1)
        {
            n |= 2;
        }
        return n;
    }

    public char indexOctantSWB(int[] neighbors)
    {
        char n;
        n = 1;
        if (neighbors[6] == 1)
        {
            n |= 128;
        }
        if (neighbors[15] == 1)
        {
            n |= 64;
        }
        if (neighbors[7] == 1)
        {
            n |= 32;
        }
        if (neighbors[16] == 1)
        {
            n |= 16;
        }
        if (neighbors[3] == 1)
        {
            n |= 8;
        }
        if (neighbors[12] == 1)
        {
            n |= 4;
        }
        if (neighbors[4] == 1)
        {
            n |= 2;
        }
        return n;
    }

    public char indexOctantNEU(int[] neighbors)
    {
        char n;
        n = 1;
        if (neighbors[20] == 1)
        {
            n |= 128;
        }
        if (neighbors[23] == 1)
        {
            n |= 64;
        }
        if (neighbors[19] == 1)
        {
            n |= 32;
        }
        if (neighbors[22] == 1)
        {
            n |= 16;
        }
        if (neighbors[11] == 1)
        {
            n |= 8;
        }
        if (neighbors[14] == 1)
        {
            n |= 4;
        }
        if (neighbors[10] == 1)
        {
            n |= 2;
        }
        return n;
    }

    public char indexOctantNWU(int[] neighbors)
    {
        char n;
        n = 1;
        if (neighbors[18] == 1)
        {
            n |= 128;
        }
        if (neighbors[21] == 1)
        {
            n |= 64;
        }
        if (neighbors[9] == 1)
        {
            n |= 32;
        }
        if (neighbors[12] == 1)
        {
            n |= 16;
        }
        if (neighbors[19] == 1)
        {
            n |= 8;
        }
        if (neighbors[22] == 1)
        {
            n |= 4;
        }
        if (neighbors[10] == 1)
        {
            n |= 2;
        }
        return n;
    }

    public char indexOctantSEU(int[] neighbors)
    {
        char n;
        n = 1;
        if (neighbors[26] == 1)
        {
            n |= 128;
        }
        if (neighbors[23] == 1)
        {
            n |= 64;
        }
        if (neighbors[17] == 1)
        {
            n |= 32;
        }
        if (neighbors[14] == 1)
        {
            n |= 16;
        }
        if (neighbors[25] == 1)
        {
            n |= 8;
        }
        if (neighbors[22] == 1)
        {
            n |= 4;
        }
        if (neighbors[16] == 1)
        {
            n |= 2;
        }
        return n;
    }

    public char indexOctantSWU(int[] neighbors)
    {
        char n;
        n = 1;
        if (neighbors[24] == 1)
        {
            n |= 128;
        }
        if (neighbors[25] == 1)
        {
            n |= 64;
        }
        if (neighbors[15] == 1)
        {
            n |= 32;
        }
        if (neighbors[16] == 1)
        {
            n |= 16;
        }
        if (neighbors[21] == 1)
        {
            n |= 8;
        }
        if (neighbors[22] == 1)
        {
            n |= 4;
        }
        if (neighbors[12] == 1)
        {
            n |= 2;
        }
        return n;
    }

    public static int countbits(char arg)
    {
        int counter = 0;
        for (int oneBit = 1; oneBit <= 0x8000; oneBit <<= 1)
        {
            if ((arg & oneBit) > 0)
            {
                counter++;
            }
        }
        return counter;
    }

    private void octreeLabeling(int octant, int label, int[] cube)
    {
        // check if there are points in the octant with value 1
        if (octant == 1)
        {
            // set points in this octant to current label
            // and recursive labeling of adjacent octants
            if (cube[0] == 1)
            {
                cube[0] = label;
            }
            if (cube[1] == 1)
            {
                cube[1] = label;
                octreeLabeling(2, label, cube);
            }
            if (cube[3] == 1)
            {
                cube[3] = label;
                octreeLabeling(3, label, cube);
            }
            if (cube[4] == 1)
            {
                cube[4] = label;
                octreeLabeling(2, label, cube);
                octreeLabeling(3, label, cube);
                octreeLabeling(4, label, cube);
            }
            if (cube[9] == 1)
            {
                cube[9] = label;
                octreeLabeling(5, label, cube);
            }
            if (cube[10] == 1)
            {
                cube[10] = label;
                octreeLabeling(2, label, cube);
                octreeLabeling(5, label, cube);
                octreeLabeling(6, label, cube);
            }
            if (cube[12] == 1)
            {
                cube[12] = label;
                octreeLabeling(3, label, cube);
                octreeLabeling(5, label, cube);
                octreeLabeling(7, label, cube);
            }
        }
        if (octant == 2)
        {
            if (cube[1] == 1)
            {
                cube[1] = label;
                octreeLabeling(1, label, cube);
            }
            if (cube[4] == 1)
            {
                cube[4] = label;
                octreeLabeling(1, label, cube);
                octreeLabeling(3, label, cube);
                octreeLabeling(4, label, cube);
            }
            if (cube[10] == 1)
            {
                cube[10] = label;
                octreeLabeling(1, label, cube);
                octreeLabeling(5, label, cube);
                octreeLabeling(6, label, cube);
            }
            if (cube[2] == 1)
            {
                cube[2] = label;
            }
            if (cube[5] == 1)
            {
                cube[5] = label;
                octreeLabeling(4, label, cube);
            }
            if (cube[11] == 1)
            {
                cube[11] = label;
                octreeLabeling(6, label, cube);
            }
            if (cube[13] == 1)
            {
                cube[13] = label;
                octreeLabeling(4, label, cube);
                octreeLabeling(6, label, cube);
                octreeLabeling(8, label, cube);
            }
        }
        if (octant == 3)
        {
            if (cube[3] == 1)
            {
                cube[3] = label;
                octreeLabeling(1, label, cube);
            }
            if (cube[4] == 1)
            {
                cube[4] = label;
                octreeLabeling(1, label, cube);
                octreeLabeling(2, label, cube);
                octreeLabeling(4, label, cube);
            }
            if (cube[12] == 1)
            {
                cube[12] = label;
                octreeLabeling(1, label, cube);
                octreeLabeling(5, label, cube);
                octreeLabeling(7, label, cube);
            }
            if (cube[6] == 1)
            {
                cube[6] = label;
            }
            if (cube[7] == 1)
            {
                cube[7] = label;
                octreeLabeling(4, label, cube);
            }
            if (cube[14] == 1)
            {
                cube[14] = label;
                octreeLabeling(7, label, cube);
            }
            if (cube[15] == 1)
            {
                cube[15] = label;
                octreeLabeling(4, label, cube);
                octreeLabeling(7, label, cube);
                octreeLabeling(8, label, cube);
            }
        }
        if (octant == 4)
        {
            if (cube[4] == 1)
            {
                cube[4] = label;
                octreeLabeling(1, label, cube);
                octreeLabeling(2, label, cube);
                octreeLabeling(3, label, cube);
            }
            if (cube[5] == 1)
            {
                cube[5] = label;
                octreeLabeling(2, label, cube);
            }
            if (cube[13] == 1)
            {
                cube[13] = label;
                octreeLabeling(2, label, cube);
                octreeLabeling(6, label, cube);
                octreeLabeling(8, label, cube);
            }
            if (cube[7] == 1)
            {
                cube[7] = label;
                octreeLabeling(3, label, cube);
            }
            if (cube[15] == 1)
            {
                cube[15] = label;
                octreeLabeling(3, label, cube);
                octreeLabeling(7, label, cube);
                octreeLabeling(8, label, cube);
            }
            if (cube[8] == 1)
            {
                cube[8] = label;
            }
            if (cube[16] == 1)
            {
                cube[16] = label;
                octreeLabeling(8, label, cube);
            }
        }
        if (octant == 5)
        {
            if (cube[9] == 1)
            {
                cube[9] = label;
                octreeLabeling(1, label, cube);
            }
            if (cube[10] == 1)
            {
                cube[10] = label;
                octreeLabeling(1, label, cube);
                octreeLabeling(2, label, cube);
                octreeLabeling(6, label, cube);
            }
            if (cube[12] == 1)
            {
                cube[12] = label;
                octreeLabeling(1, label, cube);
                octreeLabeling(3, label, cube);
                octreeLabeling(7, label, cube);
            }
            if (cube[17] == 1)
            {
                cube[17] = label;
            }
            if (cube[18] == 1)
            {
                cube[18] = label;
                octreeLabeling(6, label, cube);
            }
            if (cube[20] == 1)
            {
                cube[20] = label;
                octreeLabeling(7, label, cube);
            }
            if (cube[21] == 1)
            {
                cube[21] = label;
                octreeLabeling(6, label, cube);
                octreeLabeling(7, label, cube);
                octreeLabeling(8, label, cube);
            }
        }
        if (octant == 6)
        {
            if (cube[10] == 1)
            {
                cube[10] = label;
                octreeLabeling(1, label, cube);
                octreeLabeling(2, label, cube);
                octreeLabeling(5, label, cube);
            }
            if (cube[11] == 1)
            {
                cube[11] = label;
                octreeLabeling(2, label, cube);
            }
            if (cube[13] == 1)
            {
                cube[13] = label;
                octreeLabeling(2, label, cube);
                octreeLabeling(4, label, cube);
                octreeLabeling(8, label, cube);
            }
            if (cube[18] == 1)
            {
                cube[18] = label;
                octreeLabeling(5, label, cube);
            }
            if (cube[21] == 1)
            {
                cube[21] = label;
                octreeLabeling(5, label, cube);
                octreeLabeling(7, label, cube);
                octreeLabeling(8, label, cube);
            }
            if (cube[19] == 1)
            {
                cube[19] = label;
            }
            if (cube[22] == 1)
            {
                cube[22] = label;
                octreeLabeling(8, label, cube);
            }
        }
        if (octant == 7)
        {
            if (cube[12] == 1)
            {
                cube[12] = label;
                octreeLabeling(1, label, cube);
                octreeLabeling(3, label, cube);
                octreeLabeling(5, label, cube);
            }
            if (cube[14] == 1)
            {
                cube[14] = label;
                octreeLabeling(3, label, cube);
            }
            if (cube[15] == 1)
            {
                cube[15] = label;
                octreeLabeling(3, label, cube);
                octreeLabeling(4, label, cube);
                octreeLabeling(8, label, cube);
            }
            if (cube[20] == 1)
            {
                cube[20] = label;
                octreeLabeling(5, label, cube);
            }
            if (cube[21] == 1)
            {
                cube[21] = label;
                octreeLabeling(5, label, cube);
                octreeLabeling(6, label, cube);
                octreeLabeling(8, label, cube);
            }
            if (cube[23] == 1)
            {
                cube[23] = label;
            }
            if (cube[24] == 1)
            {
                cube[24] = label;
                octreeLabeling(8, label, cube);
            }
        }
        if (octant == 8)
        {
            if (cube[13] == 1)
            {
                cube[13] = label;
                octreeLabeling(2, label, cube);
                octreeLabeling(4, label, cube);
                octreeLabeling(6, label, cube);
            }
            if (cube[15] == 1)
            {
                cube[15] = label;
                octreeLabeling(3, label, cube);
                octreeLabeling(4, label, cube);
                octreeLabeling(7, label, cube);
            }
            if (cube[16] == 1)
            {
                cube[16] = label;
                octreeLabeling(4, label, cube);
            }
            if (cube[21] == 1)
            {
                cube[21] = label;
                octreeLabeling(5, label, cube);
                octreeLabeling(6, label, cube);
                octreeLabeling(7, label, cube);
            }
            if (cube[22] == 1)
            {
                cube[22] = label;
                octreeLabeling(6, label, cube);
            }
            if (cube[24] == 1)
            {
                cube[24] = label;
                octreeLabeling(7, label, cube);
            }
            if (cube[25] == 1)
            {
                cube[25] = label;
            }
        }
    }

    public static Mask apply(Mask mask)
    {
        return new MaskSkeleton()
        {{
            this.input = mask;
        }}.run().output;
    }
}
