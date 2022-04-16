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

package qit.math.source;

import qit.data.datasets.Vect;
import qit.math.structs.BinaryVectFunction;
import qit.math.utils.MathUtils;

public class BinaryVectFunctionSource
{
    public static BinaryVectFunction mux(final int dim)
    {
        return new BinaryVectFunction()
        {
            public void apply(Vect left, Vect right, Vect output)
            {
                for (int i = 0; i < dim; i++)
                {
                    output.set(i, left.get(i));
                    output.set(dim + i, right.get(i));
                }
            }
        }.init(dim, dim, 2 * dim);
    }

    public static BinaryVectFunction gt()
    {
        return new BinaryVectFunction()
        {
            public void apply(Vect left, Vect right, Vect output)
            {
                output.set(0, left.get(0) > right.get(0) ? 1 : 0);
            }
        }.init(1, 1, 1);
    }

    public static BinaryVectFunction lt()
    {
        return new BinaryVectFunction()
        {
            public void apply(Vect left, Vect right, Vect output)
            {
                output.set(0, left.get(0) < right.get(0) ? 1 : 0);
            }
        }.init(1, 1, 1);
    }

    public static BinaryVectFunction eq()
    {
        return new BinaryVectFunction()
        {
            public void apply(Vect left, Vect right, Vect output)
            {
                output.set(0, MathUtils.eq(left.get(0), right.get(0)) ? 1 : 0);
            }
        }.init(1, 1, 1);
    }

    public static BinaryVectFunction dist2(int dim)
    {
        return new BinaryVectFunction()
        {
            public void apply(Vect left, Vect right, Vect output)
            {
                output.set(0, left.dist2(right));
            }
        }.init(dim, dim, 1);
    }

    public static BinaryVectFunction divide(final boolean zero)
    {
        return new BinaryVectFunction()
        {
            public void apply(Vect left, Vect right, Vect output)
            {
                double leftv = left.get(0);
                double rightv = right.get(0);
                if (MathUtils.zero(rightv))
                {
                    if (zero)
                    {
                        output.set(0, 0);
                    }
                    else
                    {
                        throw new RuntimeException("zero denominator");
                    }
                }
                else
                {
                    output.set(0, leftv / rightv);
                }
            }
        }.init(1, 1, 1);
    }

    public static BinaryVectFunction divide()
    {
        return divide(true);
    }

    public static BinaryVectFunction dist(int dim)
    {
        return new BinaryVectFunction()
        {
            public void apply(Vect left, Vect right, Vect output)
            {
                output.set(0, left.dist(right));
            }
        }.init(dim, dim, 1);
    }

    public static BinaryVectFunction dot(int dim)
    {
        return new BinaryVectFunction()
        {
            public void apply(Vect left, Vect right, Vect output)
            {
                output.set(0, left.dot(right));
            }
        }.init(dim, dim, 1);
    }

    public static BinaryVectFunction dot()
    {
        return dot(3);
    }

    public static BinaryVectFunction max()
    {
        return new BinaryVectFunction()
        {
            public void apply(Vect left, Vect right, Vect output)
            {
                output.set(0, Math.max(left.get(0), right.get(0)));
            }
        }.init(1, 1, 1);
    }

    public static BinaryVectFunction min()
    {
        return new BinaryVectFunction()
        {
            public void apply(Vect left, Vect right, Vect output)
            {
                output.set(0, Math.min(left.get(0), right.get(0)));
            }
        }.init(1, 1, 1);
    }

    public static BinaryVectFunction sum(final double alpha, final double beta, int dim)
    {
        return new BinaryVectFunction()
        {
            public void apply(Vect left, Vect right, Vect output)
            {
                int dimLeft = left.size();
                int dimRight = right.size();
                int dimOut = output.size();

                for (int i = 0; i < dimOut; i++)
                {
                    double leftValue = dimLeft == 1 ? left.get(0) : left.get(i);
                    double rightValue = dimRight == 1 ? right.get(0) : right.get(i);

                    output.set(i, alpha * leftValue + beta * rightValue);
                }
            }
        }.init(dim, dim, dim);
    }

    public static BinaryVectFunction product(final int dim)
    {
        return new BinaryVectFunction()
        {
            public void apply(Vect left, Vect right, Vect output)
            {
                int dimLeft = left.size();
                int dimRight = right.size();

                for (int i = 0; i < dim; i++)
                {
                    double leftValue = dimLeft == 1 ? left.get(0) : left.get(i);
                    double rightValue = dimRight == 1 ? right.get(0) : right.get(i);

                    output.set(i, leftValue * rightValue);
                }
            }
        }.init(dim, dim, dim);
    }

    public static BinaryVectFunction quot(int dim)
    {
        return new BinaryVectFunction()
        {
            public void apply(Vect left, Vect right, Vect output)
            {
                int dimLeft = left.size();
                int dimRight = right.size();
                int dimOut = output.size();

                for (int i = 0; i < dimOut; i++)
                {
                    double leftValue = dimLeft == 1 ? left.get(0) : left.get(i);
                    double rightValue = dimRight == 1 ? right.get(0) : right.get(i);

                    if (MathUtils.zero(rightValue))
                    {
                        output.set(i, 0);
                    }
                    else
                    {
                        output.set(i, leftValue / rightValue);
                    }
                }
            }
        }.init(dim, dim, dim);
    }

    public static BinaryVectFunction lincomb()
    {
        return sum(1, 1, 1);
    }

    public static BinaryVectFunction lincomb(final double alpha, final double beta)
    {
        return sum(alpha, beta, 1);
    }

    public static BinaryVectFunction prod()
    {
        return product(1);
    }

    public static BinaryVectFunction quot()
    {
        return quot(1);
    }

}
