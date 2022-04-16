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

import Jama.EigenvalueDecomposition;
import Jama.SingularValueDecomposition;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import qit.base.Global;
import qit.base.Logging;
import qit.base.structs.Integers;
import qit.base.structs.Pair;
import qit.base.utils.PathUtils;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.utils.VectUtils;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.data.utils.vects.stats.VectsOnlineStats;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class VolumeStackUtils
{
    public static List<Integer> detect(String pattern)
    {
        return detect(pattern, null, null, null);
    }

    public static List<Integer> detect(String pattern, Integer kstart)
    {
        return detect(pattern, kstart, null, null);
    }

    public static List<Integer> detect(String pattern, Integer kstart, Integer kend)
    {
        return detect(pattern, kstart, kend, null);
    }

    public static List<Integer> detect(String pattern, Integer kstart, Integer kend, Integer kstep)
    {
        if (kstep == null)
        {
            kstep = 1;
        }

        int kmin = kstart == null ? 0 : kstart;
        int klimit = kmin + 1000000;
        while (kmin < klimit)
        {
            if (PathUtils.exists(String.format(pattern, kmin)))
            {
                break;
            }

            kmin += kstep;
        }

        if (klimit == kmin)
        {
            Logging.error("failed to find slices matching the pattern: " + pattern);
        }

        List<Integer> out = Lists.newArrayList();

        if (kend == null)
        {
            kend = klimit;
        }

        int myk = kmin;
        while (myk < kend)
        {
            if (PathUtils.exists(String.format(pattern, myk)))
            {
                out.add(myk);
                myk += kstep;
            }
            else
            {
                break;
            }
        }

        Logging.infosub("detected number of slices: %d", out.size());

        return out;
    }
}