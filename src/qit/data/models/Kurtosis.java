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


package qit.data.models;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Model;
import qit.base.structs.Integers;
import qit.base.utils.PathUtils;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.mri.structs.Gradients;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Kurtosis extends Model<Kurtosis>
{
    // port of https://github.com/NYU-DiffusionMRI/Diffusion-Kurtosis-Imaging

    /****************
     * STATIC STUFF *
     ****************/

    public final static String NAME = "dki";

    public final static double FREE_DIFF = 3.0e-3;

    // how parameters are stored as vectors
    public final static int DK_GXX = 0;
    public final static int DK_GXY = 1;
    public final static int DK_GXZ = 2;
    public final static int DK_GYY = 3;
    public final static int DK_GYZ = 4;
    public final static int DK_GZZ = 5;
    public final static int DK_GXXXX = 6;
    public final static int DK_GXXXY = 7;
    public final static int DK_GXXXZ = 8;
    public final static int DK_GXXYY = 9;
    public final static int DK_GXXYZ = 10;
    public final static int DK_GXXZZ = 11;
    public final static int DK_GXYYY = 12;
    public final static int DK_GXYYZ = 13;
    public final static int DK_GXYZZ = 14;
    public final static int DK_GXZZZ = 15;
    public final static int DK_GYYYY = 16;
    public final static int DK_GYYYZ = 17;
    public final static int DK_GYYZZ = 18;
    public final static int DK_GYZZZ = 19;
    public final static int DK_GZZZZ = 20;
    public final static int DK_DIM = 21;

    public final static int MODEL_DIM = DK_DIM + 13;

    public static int[][] LOW_IDX = {
            {0, 0, 0, 1, 1, 2},
            {0, 1, 2, 1, 2, 2}
    };

    public static int[] LOW_CNT = {
            1, 2, 2, 1, 2, 1};

    public static int[][] HIGH_IDX = new int[][]{
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2},
            {0, 0, 0, 0, 0, 0, 1, 1, 1, 2, 1, 1, 1, 2, 2},
            {0, 0, 0, 1, 1, 2, 1, 1, 2, 2, 1, 1, 2, 2, 2},
            {0, 1, 2, 1, 2, 2, 1, 2, 2, 2, 1, 2, 2, 2, 2}};

    public static int[] HIGH_CNT = new int[]{
            1, 4, 4, 6, 12, 6, 4, 12, 12, 4, 1, 4, 6, 4, 1};

    public final static String FEATURES_B0 = "B0";
    public final static String FEATURES_DT = "DT";
    public final static String FEATURES_FA = "FA";
    public final static String FEATURES_MD = "MD";
    public final static String FEATURES_RD = "RD";
    public final static String FEATURES_AD = "AD";
    public final static String FEATURES_FE = "FE";
    public final static String FEATURES_MK = "MK";
    public final static String FEATURES_AK = "AK";
    public final static String FEATURES_RK = "RK";
    public final static String FEATURES_AWF = "AWF";
    public final static String FEATURES_EAS = "EAS";
    public final static String FEATURES_IAS = "IAS";
    public final static String FEATURES_FW = "FW";

    public final static String[] FEATURES = {
            FEATURES_DT, FEATURES_B0, FEATURES_FW,
            FEATURES_FA, FEATURES_MD, FEATURES_RD, FEATURES_AD,
            FEATURES_FE, FEATURES_MK, FEATURES_RK, FEATURES_AK,
            FEATURES_AWF, FEATURES_EAS, FEATURES_IAS};

    private final static Matrix DIRS_256 = new Matrix(new double[][]{
            {0, 0, 1.0000},
            {0.5924, 0, 0.8056},
            {-0.7191, -0.1575, -0.6768},
            {-0.9151, -0.3479, 0.2040},
            {0.5535, 0.2437, 0.7964},
            {-0.0844, 0.9609, -0.2636},
            {0.9512, -0.3015, 0.0651},
            {-0.4225, 0.8984, 0.1202},
            {0.5916, -0.6396, 0.4909},
            {0.3172, 0.8818, -0.3489},
            {-0.1988, -0.6687, 0.7164},
            {-0.2735, 0.3047, -0.9123},
            {0.9714, -0.1171, 0.2066},
            {-0.5215, -0.4013, 0.7530},
            {-0.3978, -0.9131, -0.0897},
            {0.2680, 0.8196, 0.5063},
            {-0.6824, -0.6532, -0.3281},
            {0.4748, -0.7261, -0.4973},
            {0.4504, -0.4036, 0.7964},
            {-0.5551, -0.8034, -0.2153},
            {0.0455, -0.2169, 0.9751},
            {0.0483, 0.5845, 0.8099},
            {-0.1909, -0.1544, -0.9694},
            {0.8383, 0.5084, 0.1969},
            {-0.2464, 0.1148, 0.9623},
            {-0.7458, 0.6318, 0.2114},
            {-0.0080, -0.9831, -0.1828},
            {-0.2630, 0.5386, -0.8005},
            {-0.0507, 0.6425, -0.7646},
            {0.4476, -0.8877, 0.1081},
            {-0.5627, 0.7710, 0.2982},
            {-0.3790, 0.7774, -0.5020},
            {-0.6217, 0.4586, -0.6350},
            {-0.1506, 0.8688, -0.4718},
            {-0.4579, 0.2131, 0.8631},
            {-0.8349, -0.2124, 0.5077},
            {0.7682, -0.1732, -0.6163},
            {0.0997, -0.7168, -0.6901},
            {0.0386, -0.2146, -0.9759},
            {0.9312, 0.1655, -0.3249},
            {0.9151, 0.3053, 0.2634},
            {0.8081, 0.5289, -0.2593},
            {-0.3632, -0.9225, 0.1305},
            {0.2709, -0.3327, -0.9033},
            {-0.1942, -0.9790, -0.0623},
            {0.6302, -0.7641, 0.1377},
            {-0.6948, -0.3137, 0.6471},
            {-0.6596, -0.6452, 0.3854},
            {-0.9454, 0.2713, 0.1805},
            {-0.2586, -0.7957, 0.5477},
            {-0.3576, 0.6511, 0.6695},
            {-0.8490, -0.5275, 0.0328},
            {0.3830, 0.2499, -0.8893},
            {0.8804, -0.2392, -0.4095},
            {0.4321, -0.4475, -0.7829},
            {-0.5821, -0.1656, 0.7961},
            {0.3963, 0.6637, 0.6344},
            {-0.7222, -0.6855, -0.0929},
            {0.2130, -0.9650, -0.1527},
            {0.4737, 0.7367, -0.4825},
            {-0.9956, 0.0891, 0.0278},
            {-0.5178, 0.7899, -0.3287},
            {-0.8906, 0.1431, -0.4317},
            {0.2431, -0.9670, 0.0764},
            {-0.6812, -0.3807, -0.6254},
            {-0.1091, -0.5141, 0.8507},
            {-0.2206, 0.7274, -0.6498},
            {0.8359, 0.2674, 0.4794},
            {0.9873, 0.1103, 0.1147},
            {0.7471, 0.0659, -0.6615},
            {0.6119, -0.2508, 0.7502},
            {-0.6191, 0.0776, 0.7815},
            {0.7663, -0.4739, 0.4339},
            {-0.5699, 0.5369, 0.6220},
            {0.0232, -0.9989, 0.0401},
            {0.0671, -0.4207, -0.9047},
            {-0.2145, 0.5538, 0.8045},
            {0.8554, -0.4894, 0.1698},
            {-0.7912, -0.4194, 0.4450},
            {-0.2341, 0.0754, -0.9693},
            {-0.7725, 0.6346, -0.0216},
            {0.0228, 0.7946, -0.6067},
            {0.7461, -0.3966, -0.5348},
            {-0.4045, -0.0837, -0.9107},
            {-0.4364, 0.6084, -0.6629},
            {0.6177, -0.3175, -0.7195},
            {-0.4301, -0.0198, 0.9026},
            {-0.1489, -0.9706, 0.1892},
            {0.0879, 0.9070, -0.4117},
            {-0.7764, -0.4707, -0.4190},
            {0.9850, 0.1352, -0.1073},
            {-0.1581, -0.3154, 0.9357},
            {0.8938, -0.3246, 0.3096},
            {0.8358, -0.4464, -0.3197},
            {0.4943, 0.4679, 0.7327},
            {-0.3095, 0.9015, -0.3024},
            {-0.3363, -0.8942, -0.2956},
            {-0.1271, -0.9274, -0.3519},
            {0.3523, -0.8717, -0.3407},
            {0.7188, -0.6321, 0.2895},
            {-0.7447, 0.0924, -0.6610},
            {0.1622, 0.7186, 0.6762},
            {-0.9406, -0.0829, -0.3293},
            {-0.1229, 0.9204, 0.3712},
            {-0.8802, 0.4668, 0.0856},
            {-0.2062, -0.1035, 0.9730},
            {-0.4861, -0.7586, -0.4338},
            {-0.6138, 0.7851, 0.0827},
            {0.8476, 0.0504, 0.5282},
            {0.3236, 0.4698, -0.8213},
            {-0.7053, -0.6935, 0.1473},
            {0.1511, 0.3778, 0.9135},
            {0.6011, 0.5847, 0.5448},
            {0.3610, 0.3183, 0.8766},
            {0.9432, 0.3304, 0.0341},
            {0.2423, -0.8079, -0.5372},
            {0.4431, -0.1578, 0.8825},
            {0.6204, 0.5320, -0.5763},
            {-0.2806, -0.5376, -0.7952},
            {-0.5279, -0.8071, 0.2646},
            {-0.4214, -0.6159, 0.6656},
            {0.6759, -0.5995, -0.4288},
            {0.5670, 0.8232, -0.0295},
            {-0.0874, 0.4284, -0.8994},
            {0.8780, -0.0192, -0.4782},
            {0.0166, 0.8421, 0.5391},
            {-0.7741, 0.2931, -0.5610},
            {0.9636, -0.0579, -0.2611},
            {0, 0, -1.0000},
            {-0.5924, 0, -0.8056},
            {0.7191, 0.1575, 0.6768},
            {0.9151, 0.3479, -0.2040},
            {-0.5535, -0.2437, -0.7964},
            {0.0844, -0.9609, 0.2636},
            {-0.9512, 0.3015, -0.0651},
            {0.4225, -0.8984, -0.1202},
            {-0.5916, 0.6396, -0.4909},
            {-0.3172, -0.8818, 0.3489},
            {0.1988, 0.6687, -0.7164},
            {0.2735, -0.3047, 0.9123},
            {-0.9714, 0.1171, -0.2066},
            {0.5215, 0.4013, -0.7530},
            {0.3978, 0.9131, 0.0897},
            {-0.2680, -0.8196, -0.5063},
            {0.6824, 0.6532, 0.3281},
            {-0.4748, 0.7261, 0.4973},
            {-0.4504, 0.4036, -0.7964},
            {0.5551, 0.8034, 0.2153},
            {-0.0455, 0.2169, -0.9751},
            {-0.0483, -0.5845, -0.8099},
            {0.1909, 0.1544, 0.9694},
            {-0.8383, -0.5084, -0.1969},
            {0.2464, -0.1148, -0.9623},
            {0.7458, -0.6318, -0.2114},
            {0.0080, 0.9831, 0.1828},
            {0.2630, -0.5386, 0.8005},
            {0.0507, -0.6425, 0.7646},
            {-0.4476, 0.8877, -0.1081},
            {0.5627, -0.7710, -0.2982},
            {0.3790, -0.7774, 0.5020},
            {0.6217, -0.4586, 0.6350},
            {0.1506, -0.8688, 0.4718},
            {0.4579, -0.2131, -0.8631},
            {0.8349, 0.2124, -0.5077},
            {-0.7682, 0.1732, 0.6163},
            {-0.0997, 0.7168, 0.6901},
            {-0.0386, 0.2146, 0.9759},
            {-0.9312, -0.1655, 0.3249},
            {-0.9151, -0.3053, -0.2634},
            {-0.8081, -0.5289, 0.2593},
            {0.3632, 0.9225, -0.1305},
            {-0.2709, 0.3327, 0.9033},
            {0.1942, 0.9790, 0.0623},
            {-0.6302, 0.7641, -0.1377},
            {0.6948, 0.3137, -0.6471},
            {0.6596, 0.6452, -0.3854},
            {0.9454, -0.2713, -0.1805},
            {0.2586, 0.7957, -0.5477},
            {0.3576, -0.6511, -0.6695},
            {0.8490, 0.5275, -0.0328},
            {-0.3830, -0.2499, 0.8893},
            {-0.8804, 0.2392, 0.4095},
            {-0.4321, 0.4475, 0.7829},
            {0.5821, 0.1656, -0.7961},
            {-0.3963, -0.6637, -0.6344},
            {0.7222, 0.6855, 0.0929},
            {-0.2130, 0.9650, 0.1527},
            {-0.4737, -0.7367, 0.4825},
            {0.9956, -0.0891, -0.0278},
            {0.5178, -0.7899, 0.3287},
            {0.8906, -0.1431, 0.4317},
            {-0.2431, 0.9670, -0.0764},
            {0.6812, 0.3807, 0.6254},
            {0.1091, 0.5141, -0.8507},
            {0.2206, -0.7274, 0.6498},
            {-0.8359, -0.2674, -0.4794},
            {-0.9873, -0.1103, -0.1147},
            {-0.7471, -0.0659, 0.6615},
            {-0.6119, 0.2508, -0.7502},
            {0.6191, -0.0776, -0.7815},
            {-0.7663, 0.4739, -0.4339},
            {0.5699, -0.5369, -0.6220},
            {-0.0232, 0.9989, -0.0401},
            {-0.0671, 0.4207, 0.9047},
            {0.2145, -0.5538, -0.8045},
            {-0.8554, 0.4894, -0.1698},
            {0.7912, 0.4194, -0.4450},
            {0.2341, -0.0754, 0.9693},
            {0.7725, -0.6346, 0.0216},
            {-0.0228, -0.7946, 0.6067},
            {-0.7461, 0.3966, 0.5348},
            {0.4045, 0.0837, 0.9107},
            {0.4364, -0.6084, 0.6629},
            {-0.6177, 0.3175, 0.7195},
            {0.4301, 0.0198, -0.9026},
            {0.1489, 0.9706, -0.1892},
            {-0.0879, -0.9070, 0.4117},
            {0.7764, 0.4707, 0.4190},
            {-0.9850, -0.1352, 0.1073},
            {0.1581, 0.3154, -0.9357},
            {-0.8938, 0.3246, -0.3096},
            {-0.8358, 0.4464, 0.3197},
            {-0.4943, -0.4679, -0.7327},
            {0.3095, -0.9015, 0.3024},
            {0.3363, 0.8942, 0.2956},
            {0.1271, 0.9274, 0.3519},
            {-0.3523, 0.8717, 0.3407},
            {-0.7188, 0.6321, -0.2895},
            {0.7447, -0.0924, 0.6610},
            {-0.1622, -0.7186, -0.6762},
            {0.9406, 0.0829, 0.3293},
            {0.1229, -0.9204, -0.3712},
            {0.8802, -0.4668, -0.0856},
            {0.2062, 0.1035, -0.9730},
            {0.4861, 0.7586, 0.4338},
            {0.6138, -0.7851, -0.0827},
            {-0.8476, -0.0504, -0.5282},
            {-0.3236, -0.4698, 0.8213},
            {0.7053, 0.6935, -0.1473},
            {-0.1511, -0.3778, -0.9135},
            {-0.6011, -0.5847, -0.5448},
            {-0.3610, -0.3183, -0.8766},
            {-0.9432, -0.3304, -0.0341},
            {-0.2423, 0.8079, 0.5372},
            {-0.4431, 0.1578, -0.8825},
            {-0.6204, -0.5320, 0.5763},
            {0.2806, 0.5376, 0.7952},
            {0.5279, 0.8071, -0.2646},
            {0.4214, 0.6159, -0.6656},
            {-0.6759, 0.5995, 0.4288},
            {-0.5670, -0.8232, 0.0295},
            {0.0874, -0.4284, 0.8994},
            {-0.8780, 0.0192, 0.4782},
            {-0.0166, -0.8421, -0.5391},
            {0.7741, -0.2931, 0.5610},
            {-0.9636, 0.0579, 0.2611}});

    private static Matrix ADC_256 = null;
    private static Matrix AKC_256 = null;

    private static Matrix DIRS_10K = null;
    private static Matrix ADC_10K = null;
    private static Matrix AKC_10K = null;

    static
    {
        ADC_256 = projectLowOrder(DIRS_256);
        AKC_256 = projectHighOrder(DIRS_256);

        try
        {
            String cache = Global.getRoot();
            cache = PathUtils.join(cache, "share");
            cache = PathUtils.join(cache, "qit");
            cache = PathUtils.join(cache, "dirs10000.txt");

            DIRS_10K = Matrix.read(cache);

            ADC_10K = projectLowOrder(DIRS_10K);
            AKC_10K = projectHighOrder(DIRS_10K);

        }
        catch (IOException e)
        {
            Logging.info("warning: failed to read 10k dir cache");
        }
    }

    public static Matrix projectLowOrder(Matrix dirs)
    {
        Matrix a = dirs.permuteRows(LOW_IDX[0]).timesElem(dirs.permuteRows(LOW_IDX[1]));
        Matrix b = MatrixSource.diag(VectSource.create(LOW_CNT));

        return a.times(b);
    }

    public static Matrix projectHighOrder(Matrix dirs)
    {
        Matrix a = dirs.permuteRows(HIGH_IDX[0]);
        a.timesElemEquals(dirs.permuteRows(HIGH_IDX[1]));
        a.timesElemEquals(dirs.permuteRows(HIGH_IDX[2]));
        a.timesElemEquals(dirs.permuteRows(HIGH_IDX[3]));

        Matrix b = MatrixSource.diag(VectSource.create(HIGH_CNT));

        return a.times(b);
    }

    public static Matrix getLinearSystemMatrixLog(Gradients gradients)
    {
        // https://github.com/NYU-DiffusionMRI/Diffusion-Kurtosis-Imaging/blob/master/dki_fit.m
        final int dim = gradients.size();

        Matrix grads = new Matrix(dim, 4);
        for (int i = 0; i < dim; i++)
        {
            Vect g = gradients.getBvec(i);
            double b = gradients.getBval(i);

            double gx = g.getX();
            double gy = g.getY();
            double gz = g.getZ();

            grads.set(i, 0, gx);
            grads.set(i, 1, gy);
            grads.set(i, 2, gz);
            grads.set(i, 3, b);
        }

        // baseline part
        Matrix A = null;

        {
            int[] BIDX = {3, 3, 3, 3, 3, 3};

            Matrix gi = grads.permuteRows(LOW_IDX[0]);
            Matrix gj = grads.permuteRows(LOW_IDX[1]);
            Matrix gb = grads.permuteRows(BIDX);
            Matrix gijb = gb.timesElem(gi).timesElem(gj);
            Matrix dc = MatrixSource.diag(VectSource.create(LOW_CNT));
            Matrix br = gijb.times(dc).times(-1);

            A = br;
        }

        {
            Matrix bsq = MatrixSource.col(grads.getColumn(3).sq());
            Matrix bsqd6 = bsq.times(MatrixSource.constant(1, 15, 1.0 / 6.0));

            Matrix wid = bsqd6.copy();
            wid.timesElemEquals(grads.permuteRows(HIGH_IDX[0]));
            wid.timesElemEquals(grads.permuteRows(HIGH_IDX[1]));
            wid.timesElemEquals(grads.permuteRows(HIGH_IDX[2]));
            wid.timesElemEquals(grads.permuteRows(HIGH_IDX[3]));

            Matrix dwc = MatrixSource.diag(VectSource.create(HIGH_CNT));

            A = A.catRows(wid.times(dwc));
        }

        return A;
    }

    public static Matrix getLinearSystemSignalLog(Vect signal)
    {
        int dim = signal.size();
        final Matrix B = new Matrix(dim, 1);
        for (int i = 0; i < dim; i++)
        {
            double s = signal.get(i);
            if (s > 0)
            {
                B.set(i, 0, Math.log(s));
            }
            else
            {
                B.set(i, 0, 0);
            }
        }

        return B;
    }

    public static Vect getLinearSystemSolution(Vect soln)
    {
        Vect params = soln.copy();

        double dxx = params.get(0);
        double dyy = params.get(3);
        double dzz = params.get(5);
        double md = (dxx + dyy + dzz) / 3;
        double DapprSq = 1.0 / (md * md);

        for (int i = 6; i < 21; i++)
        {
            params.set(i, params.get(i) * DapprSq);
        }

        return params;
    }

    private static Vect carlsonRf(Vect x, Vect y, Vect z)
    {
        /**
         * Computes the Carlson's incomplete elliptic integral of the first kind defined as:
         *
         *   R_F = \frac{1}{2} \int_{0}^{\infty} \left [(t+x)(t+y)(t+z)  \right ]^{-\frac{1}{2}}dt
         *
         *   Carlson, B.C., 1994. Numerical computation of real or complex elliptic integrals. arXiv:math/9409227
         */

        double errtol = 3e-4;
        int dim = x.size();

        Vect An = x.plus(y).plus(z).times(1.0 / 3.0);

        Matrix Qmat = new Matrix(3, dim);
        Qmat.setRow(0, An.minus(x));
        Qmat.setRow(1, An.minus(y));
        Qmat.setRow(2, An.minus(z));
        Vect Qvec = MatrixUtils.maxCols(MatrixUtils.abs(Qmat));
        double Qfac = (3 * errtol) * (-1.0 / 6.0);
        Vect Q = Qvec.times(Qfac);

        for (int i = 0; i < dim; i++)
        {
            int n = 0;
            while ((Math.pow(4.0, -n) * Q.get(i) > Math.abs(An.get(i))))
            {
                double xroot = Math.sqrt(x.get(i));
                double yroot = Math.sqrt(y.get(i));
                double zroot = Math.sqrt(z.get(i));
                double lambda = xroot * (yroot + zroot) + yroot * zroot;

                n += 1;
                x.set(i, (x.get(i) + lambda) * 0.025);
                y.set(i, (x.get(i) + lambda) * 0.025);
                z.set(i, (x.get(i) + lambda) * 0.025);
                An.set(i, (An.get(i) + lambda) * 0.025);
            }
        }

        Vect X = x.div(An).times(-1.0).plus(1.0);
        Vect Y = y.div(An).times(-1.0).plus(1.0);
        Vect Z = X.times(-1).minus(Y);

        Vect E2 = X.times(Y).minus(Z.times(Z));
        Vect E3 = X.times(Y).times(Z);

        Vect RFa = VectSource.ones(dim);
        Vect RFb = E2.times(-1.0 / 10.0);
        Vect RFc = E3.times(1.0 / 14.0);
        Vect RFd = E3.pow(2).times(1.0 / 24.0);
        Vect RFe = E2.times(E3).times(-3.0 / 44.0);
        Vect RFf = An.pow(-0.5);

        Vect RF = (RFa.plus(RFb).plus(RFc).plus(RFd).plus(RFd).plus(RFe)).times(RFf);

        return RF;
    }

    private static Vect carlsonRd(Vect x, Vect y, Vect z)
    {
        /**
         * Computes the Carlson's incomplete elliptic integral of the second kind defined as:
         *
         *   R_D = \frac{3}{2} \int_{0}^{\infty} (t+x)^{-\frac{1}{2}} (t+y)^{-\frac{1}{2}}(t+z)  ^{-\frac{3}{2}}
         *
         *   Carlson, B.C., 1994. Numerical computation of real or complex elliptic integrals. arXiv:math/9409227
         */

        double errtol = 3e-4;
        int dim = x.size();

        Vect A0 = x.plus(y).plus(z.times(3.0)).times(1.0 / 5.0);
        Vect An = A0.copy();

        Matrix Qmat = new Matrix(3, dim);
        Qmat.setRow(0, An.minus(x));
        Qmat.setRow(1, An.minus(y));
        Qmat.setRow(2, An.minus(z));
        Vect Qvec = MatrixUtils.maxCols(MatrixUtils.abs(Qmat));
        double Qfac = (errtol / 4.0) * (-1.0 / 6.0);
        Vect Q = Qvec.times(Qfac);

        return null;
    }

    private static Matrix radialSampling(Vect dir, int num)
    {
        double dx = dir.getX();
        double dy = dir.getY();
        double dz = dir.getZ();

        double dt = 2.0 * Math.PI / (double) num;

        Vect v = VectSource.create3D(-dy, dx, 0);
        double s = v.norm();
        double c = dz;
        double f = (1.0 - c) / (s * s);

        Matrix V = new Matrix(3, 3);
        V.set(0, 0, 0);
        V.set(0, 1, -dz);
        V.set(0, 2, dy);
        V.set(1, 0, dz);
        V.set(1, 1, 0);
        V.set(1, 2, -dx);
        V.set(2, 0, -dy);
        V.set(2, 1, dx);
        V.set(2, 2, 0);
        V = V.transpose();

        Matrix R = MatrixSource.identity(3).plus(V).plus(V.times(V).times(f));

        Matrix out = new Matrix(num, 3);

        for (int i = 0; i < num; i++)
        {
            double theta = i * dt;
            Vect rdir = R.times(VectSource.create3D(Math.cos(theta), Math.sin(theta), 0));

            out.setRow(i, rdir);
        }

        return out;
    }

    public static boolean matches(String name)
    {
        if (name == null)
        {
            return false;
        }

        String lname = name.toLowerCase();
        return (lname.contains(".dki") || lname.contains(".kurtosis"));
    }

    /****************
     * MEMBER STUFF *
     ****************/

    // storing the dki indices is wasteful of space, but it will save us computation time
    public double b0;
    public Vect dt = VectSource.createND(DK_DIM);
    public double fa;
    public double md;
    public double ad;
    public double rd;
    public double fe;
    public double mk;
    public double ak;
    public double rk;
    public double awf;
    public double eas;
    public double ias;
    public double fw;

    public Kurtosis()
    {
    }

    public Kurtosis(Vect encoding)
    {
        this();
        this.setEncoding(encoding);
    }

    public static boolean valid(int size)
    {
        return size == MODEL_DIM;
    }

    public double baseline()
    {
        return this.b0;
    }

    public Matrix getTensorD()
    {
        double[][] out = new double[3][3];
        out[0][0] = this.dt.get(DK_GXX);
        out[1][1] = this.dt.get(DK_GYY);
        out[2][2] = this.dt.get(DK_GZZ);
        out[0][1] = this.dt.get(DK_GXY);
        out[1][0] = this.dt.get(DK_GXY);
        out[0][2] = this.dt.get(DK_GXZ);
        out[2][0] = this.dt.get(DK_GXZ);
        out[1][2] = this.dt.get(DK_GYZ);
        out[2][1] = this.dt.get(DK_GYZ);

        return new Matrix(out);
    }

    public void setTensorD(Matrix D)
    {
        this.dt.set(DK_GXX, D.get(0, 0));
        this.dt.set(DK_GYY, D.get(1, 1));
        this.dt.set(DK_GZZ, D.get(2, 2));
        this.dt.set(DK_GXY, D.get(0, 1));
        this.dt.set(DK_GXY, D.get(1, 0));
        this.dt.set(DK_GXZ, D.get(0, 2));
        this.dt.set(DK_GXZ, D.get(2, 0));
        this.dt.set(DK_GYZ, D.get(1, 2));
        this.dt.set(DK_GYZ, D.get(2, 1));
    }

    public double[][] getTensorU()
    {
        try
        {
            double md = (this.dt.get(DK_GXX) + this.dt.get(DK_GYY) + this.dt.get(DK_GZZ)) / 3.0;
            return this.getTensorD().inv().times(md).toArray();
        }
        catch (RuntimeException e)
        {
            return new double[3][3];
        }
    }

    public double getTensorOdf(double alpha, Vect n)
    {
        Matrix U = new Matrix(this.getTensorU());
        Vect Un = U.times(n);
        double nUn = n.dot(Un);
        double out = MathUtils.zero(nUn) ? 0.0 : 1.0 / nUn;
        out = MathUtils.eq(alpha, 1.0) ? out : Math.pow(out, 0.5 * (alpha + 1));

        return out;
    }

    public double[][] getTensorV(Vect n)
    {
        Matrix U = new Matrix(this.getTensorU());
        Vect Un = U.times(n);
        double Unx = Un.getX();
        double Uny = Un.getY();
        double Unz = Un.getZ();
        double nUn = n.dot(Un);
        double norm = MathUtils.zero(nUn) ? 0.0 : 1.0 / nUn;

        double[][] out = new double[3][3];
        out[0][0] = Unx * Unx * norm;
        out[1][1] = Uny * Uny * norm;
        out[2][2] = Unz * Unz * norm;
        out[0][1] = Unx * Uny * norm;
        out[1][0] = out[0][1];
        out[0][2] = Unx * Unz * norm;
        out[2][0] = out[0][2];
        out[1][2] = Uny * Unz * norm;
        out[2][1] = out[1][2];

        return out;
    }

    public double[][][][] getTensorW()
    {
        double[][][][] out = new double[3][3][3][3];

        Map<Integers,Double> lut = Maps.newHashMap();

        lut.put(new Integers(0, 0, 0, 0), this.dt.get(DK_GXXXX));
        lut.put(new Integers(0, 0, 0, 1), this.dt.get(DK_GXXXY));
        lut.put(new Integers(0, 0, 0, 2), this.dt.get(DK_GXXXZ));
        lut.put(new Integers(0, 0, 1, 1), this.dt.get(DK_GXXYY));
        lut.put(new Integers(0, 0, 1, 2), this.dt.get(DK_GXXYZ));
        lut.put(new Integers(0, 0, 2, 2), this.dt.get(DK_GXXZZ));
        lut.put(new Integers(0, 1, 1, 1), this.dt.get(DK_GXYYY));
        lut.put(new Integers(0, 1, 1, 2), this.dt.get(DK_GXYYZ));
        lut.put(new Integers(0, 1, 2, 2), this.dt.get(DK_GXYZZ));
        lut.put(new Integers(0, 2, 2, 2), this.dt.get(DK_GXZZZ));
        lut.put(new Integers(1, 1, 1, 1), this.dt.get(DK_GYYYY));
        lut.put(new Integers(1, 1, 1, 2), this.dt.get(DK_GYYYZ));
        lut.put(new Integers(1, 1, 2, 2), this.dt.get(DK_GYYZZ));
        lut.put(new Integers(1, 2, 2, 2), this.dt.get(DK_GYZZZ));
        lut.put(new Integers(2, 2, 2, 2), this.dt.get(DK_GZZZZ));

        for (int a = 0; a < 3; a++)
        {
            for (int b = 0; b < 3; b++)
            {
                for (int c = 0; c < 3; c++)
                {
                    for (int d = 0; d < 3; d++)
                    {
                        List<Integer> idx = Lists.newArrayList();
                        idx.add(a);
                        idx.add(b);
                        idx.add(c);
                        idx.add(d);
                        Collections.sort(idx);

                        Integers key = new Integers(idx.get(0), idx.get(1), idx.get(2), idx.get(3));
                        double value = lut.get(key);

                        out[a][b][c][d] = value;
                    }
                }
            }
        }

        return out;
    }

    public Vect odf(boolean gaussian, double alpha, Vects samples)
    {
        double[][] U = this.getTensorU();
        double[][][][] W = this.getTensorW();

        Vect out = VectSource.createND(samples.size());
        for (int i = 0; i < samples.size(); i++)
        {
            Vect sample = samples.get(i);

            double odf = this.getTensorOdf(alpha, sample);

            if (!gaussian)
            {
                double[][] V = this.getTensorV(sample);

                double factor = 0.0;
                for (int a = 0; a < 3; a++)
                {
                    for (int b = 0; b < 3; b++)
                    {
                        for (int c = 0; c < 3; c++)
                        {
                            for (int d = 0; d < 3; d++)
                            {
                                double wfactor = 0;
                                wfactor += 3.0 * U[a][b] * U[c][d];
                                wfactor += -6.0 * (alpha + 1) * U[a][b] * V[c][d];
                                wfactor += (alpha + 1) * (alpha + 3) * V[a][b] * V[c][d];
                                wfactor *= W[a][b][c][d];

                                factor += wfactor;
                            }
                        }
                    }
                }

                factor /= 24.0;
                factor += 1;

                odf *= factor;
            }

            out.set(i, odf);
        }

        return out;
    }

    public Kurtosis set(Kurtosis t)
    {
        this.dt = t.dt.copy();
        this.b0 = t.b0;
        this.fa = t.fa;
        this.md = t.md;
        this.ad = t.ad;
        this.rd = t.rd;
        this.fe = t.fe;
        this.mk = t.mk;
        this.ak = t.ak;
        this.rk = t.rk;
        this.awf = t.awf;
        this.eas = t.eas;
        this.ias = t.ias;
        this.fw = t.fw;

        return this;
    }

    public void setFreeWater(double e)
    {
        this.fw = e;
    }

    public double getFreeWater()
    {
        return this.fw;
    }

    public int getDegreesOfFreedom()
    {
        // 6 for low order tensor
        // 15 for high order tensor
        // 1 is for free water

        return 22;
    }

    public Kurtosis copy()
    {
        Kurtosis out = new Kurtosis();
        out.set(this);
        return out;
    }

    public Kurtosis proto()
    {
        return new Kurtosis();
    }

    public int getEncodingSize()
    {
        return MODEL_DIM;
    }

    public Kurtosis setEncoding(Vect encoding)
    {
        this.b0 = encoding.get(0);
        this.dt.set(encoding.sub(1, DK_DIM + 1));
        this.fa = encoding.get(22);
        this.md = encoding.get(23);
        this.ad = encoding.get(24);
        this.rd = encoding.get(25);
        this.fe = encoding.get(26);
        this.mk = encoding.get(27);
        this.ak = encoding.get(28);
        this.rk = encoding.get(29);
        this.awf = encoding.get(30);
        this.eas = encoding.get(31);
        this.ias = encoding.get(32);
        this.fw= encoding.get(33);

        return this;
    }

    public void getEncoding(Vect encoding)
    {
        encoding.set(0, this.b0);
        encoding.set(1, this.dt);
        encoding.set(22, this.fa);
        encoding.set(23, this.md);
        encoding.set(24, this.ad);
        encoding.set(25, this.rd);
        encoding.set(26, this.fe);
        encoding.set(27, this.mk);
        encoding.set(28, this.ak);
        encoding.set(29, this.rk);
        encoding.set(30, this.awf);
        encoding.set(31, this.eas);
        encoding.set(32, this.ias);
        encoding.set(33, this.fw);
    }

    public double dist(Kurtosis t)
    {
        return this.dt.dist(t.dt);
    }

    public void updateFeatures()
    {
        // unlike other models, the kurtosis features are expensve to compute, so we should cache them

        Tensor tensor = new Tensor(this.b0, this.getTensorD());

        this.fa = tensor.feature(Tensor.FEATURES_FA).get(0);
        this.md = tensor.feature(Tensor.FEATURES_MD).get(0);
        this.rd = tensor.feature(Tensor.FEATURES_RD).get(0);
        this.ad = tensor.feature(Tensor.FEATURES_AD).get(0);

        // @todo: update kurtosis and wmti features
    }

    public Vect feature(String name)
    {
        if (FEATURES_B0.equals(name))
        {
            return VectSource.create1D(this.b0);
        }
        else if (FEATURES_DT.equals(name))
        {
            return this.dt;
        }
        else if (FEATURES_FA.equals(name))
        {
            return VectSource.create1D(this.fa);
        }
        else if (FEATURES_MD.equals(name))
        {
            return VectSource.create1D(this.md);
        }
        else if (FEATURES_RD.equals(name))
        {
            return VectSource.create1D(this.rd);
        }
        else if (FEATURES_AD.equals(name))
        {
            return VectSource.create1D(this.ad);
        }
        else if (FEATURES_FE.equals(name))
        {
            return VectSource.create1D(this.fe);
        }
        else if (FEATURES_MK.equals(name))
        {
            return VectSource.create1D(this.mk);
        }
        else if (FEATURES_AK.equals(name))
        {
            return VectSource.create1D(this.ak);
        }
        else if (FEATURES_RK.equals(name))
        {
            return VectSource.create1D(rk);
        }
        else if (FEATURES_AWF.equals(name))
        {
            return VectSource.create1D(awf);
        }
        else if (FEATURES_EAS.equals(name))
        {
            return VectSource.create1D(this.eas);
        }
        else if (FEATURES_IAS.equals(name))
        {
            return VectSource.create1D(this.ias);
        }
        else if (FEATURES_FW.equals(name))
        {
            return VectSource.create1D(this.fw);
        }

        throw new RuntimeException("invalid feature: " + name);
    }

    private Vect akc(Matrix low, Matrix high)
    {
        double dxx = this.dt.get(DK_GXX);
        double dyy = this.dt.get(DK_GYY);
        double dzz = this.dt.get(DK_GZZ);
        double md = (dxx + dyy + dzz) / 3.0;

        Vect adc = low.times(this.dt.sub(1, 7));
        Vect akc = high.times(this.dt.sub(7, 22));

        for (int i = 0; i < akc.size(); i++)
        {
            double adcv = adc.get(i);
            double akcv = akc.get(i);

            akc.set(i, akcv * md * md / (adcv * adcv));
        }

        return akc;
    }

    private double ak()
    {
        Logging.error("not yet implemented");
        Vect e1 = null;

        Matrix dirs = new Matrix(2, 3);
        dirs.setRow(0, e1);
        dirs.setRow(1, e1.times(-1));

        return akc(projectLowOrder(dirs), projectHighOrder(dirs)).mean();
    }

    public List<String> features()
    {
        return Lists.newArrayList(FEATURES);
    }

    public Kurtosis getThis()
    {
        return this;
    }

    public static VectFunction synth(final Gradients gradients)
    {
        final Matrix A = Kurtosis.getLinearSystemMatrixLog(gradients);

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                Kurtosis model = new Kurtosis(input);

                Vect dtadj = model.dt.copy();
                double dxx = dtadj.get(0);
                double dyy = dtadj.get(3);
                double dzz = dtadj.get(5);
                double md = (dxx + dyy + dzz) / 3;
                double DapprSq = 1.0 / (md * md);

                Vect expiso = VectSource.createND(gradients.size());
                for (int i = 0; i < gradients.size(); i++)
                {
                    double bval = gradients.getBval(i);
                    expiso.set(i, Math.exp(-bval * FREE_DIFF));
                }

                for (int i = 6; i < 21; i++)
                {
                    dtadj.set(i, dtadj.get(i) / DapprSq);
                }

                double b0 = model.b0;
                double frac = input.get(Tensor.DT_FW);
                Vect exptissue = A.times(dtadj).exp();
                Vect signal = exptissue.times(1.0 - frac).plus(frac, expiso).times(b0);

                output.set(signal);
            }
        }.init(MODEL_DIM, gradients.size());
    }
}