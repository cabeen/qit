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
import qit.base.structs.Pair;
import qit.data.datasets.Matrix;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.modules.mesh.MeshSimplify;
import qit.data.modules.mesh.MeshSmooth;
import qit.data.source.MeshSource;
import qit.data.source.VectSource;
import qit.base.Model;
import qit.data.source.VectsSource;
import qit.math.structs.Vertex;
import qit.math.source.VectFunctionSource;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.Map;

public class Spharm extends Model<Spharm>
{
    public final static String NAME = "spharm";
    public final static String SIZE = "size";
    public final static String SUM = "sum";
    public final static String MIN = "min";
    public final static String MAX = "max";
    public final static String MEAN = "mean";
    public final static String[] FEATURES = {SIZE, SUM, MIN, MAX, MEAN};

    public static final String ATTR_LINE = "line";
    public static final String ATTR_AMP = "amp";

    public static int[] ORDERS = {2, 4, 6, 8, 10, 12, 14, 16};
    public static int[] DETAIL_FACES = {100, 500, 1000, 2000, 3000, 4000, 5000};
    public static int DETAIL_MAX = DETAIL_FACES.length;
    public static int DETAIL_DEFAULT = 2;
    private static Map<Pair<Integer, Integer>, Pair<Matrix, Mesh>> CACHE = Maps.newHashMap();

    public static void initCache()
    {
        if (CACHE.size() == 0)
        {
            Logging.info("started making spharm cache");

            Mesh base = MeshSource.sphere(5);

            for (int detail = DETAIL_MAX; detail > 0; detail--)
            {
                Logging.info("... at detail: " + detail);

                int faces = DETAIL_FACES[detail - 1];

                MeshSimplify simplify = new MeshSimplify();
                simplify.input = base;
                simplify.maxface = faces;
                Mesh mesh = simplify.run().output;

                MeshSmooth smooth = new MeshSmooth();
                smooth.input = mesh;
                smooth.num = 5;
                mesh = smooth.run().output;

                base = mesh;

                mesh.vattr.add(ATTR_LINE, VectSource.create3D());
                mesh.vattr.add(ATTR_AMP, VectSource.create1D());

                Vects coords = new Vects();
                for (Vertex vert : mesh.graph.verts())
                {
                    Vect coord = mesh.vattr.get(vert, Mesh.COORD).normalize();
                    Vect color = VectFunctionSource.rgb().apply(coord);

                    mesh.graph.add(vert);
                    mesh.vattr.set(vert, ATTR_AMP, VectSource.create1D());
                    mesh.vattr.set(vert, ATTR_LINE, coord);
                    mesh.vattr.set(vert, Mesh.COORD, coord);
                    mesh.vattr.set(vert, Mesh.COLOR, color);
                    mesh.vattr.set(vert, Mesh.NORMAL, coord);

                    coords.add(coord);
                }

                for (Integer order : ORDERS)
                {
                    Matrix matrix = Spharm.bmatrix(order, coords);

                    CACHE.put(Pair.of(detail, order), Pair.of(matrix, mesh));
                }
            }

            Logging.info("finished making spharm cache");
        }
    }

    public static Mesh getMesh(int detail, int order)
    {
        Global.assume(detail > 0 && detail <= DETAIL_MAX, "invalid detail: " + detail);
        Global.assume(order > 0 && order <= 16, "invalid order: " + order);
        initCache();

        return CACHE.get(Pair.of(detail, order)).b;
    }

    public static Matrix getMatrix(int detail, int order)
    {
        Global.assume(detail > 0 && detail <= DETAIL_MAX, "invalid detail: " + detail);
        Global.assume(order > 0 && order <= 16, "invalid order: " + order);
        initCache();

        return CACHE.get(Pair.of(detail, order)).a;
    }

    public static Mesh getMesh(int order)
    {
        return getMesh(DETAIL_DEFAULT, order);
    }

    public static Matrix getMatrix(int order)
    {
        return getMatrix(DETAIL_DEFAULT, order);
    }

    public static boolean valid(int size)
    {
        switch (size)
        {
            case 1:
                return true;
            case 6:
                return true;
            case 15:
                return true;
            case 28:
                return true;
            case 45:
                return true;
            case 66:
                return true;
            case 91:
                return true;
            case 120:
                return true;
            case 153:
                return true;
        }
        return false;

    }

    public static int nearestOrder(int order)
    {
        for (Integer n : ORDERS)
        {
            if (order <= n)
            {
                return n;
            }
        }

        return 16;
    }

    public static int sizeToOrder(int size)
    {
        switch (size)
        {
            case 1: // 0 + (0 * 2 + 1) = 1
                return 0;
            case 6: // 1 + (2 * 2 + 1) = 6
                return 2;
            case 15: // 6 + (4 * 2 + 1) = 15
                return 4;
            case 28: // 15 + (6 * 2 + 1) = 28
                return 6;
            case 45: // 28 + (8 * 2 + 1) = 45
                return 8;
            case 66: //
                return 10;
            case 91: //
                return 12;
            case 120: //
                return 14;
            case 153: //
                return 16;
            default:
                throw new RuntimeException("unsupported spharm size");

        }
    }

    public static int orderToSize(int order)
    {
        switch (order)
        {
            case 0:
                return 1;
            case 2:
                return 6;
            case 4:
                return 15;
            case 6:
                return 28;
            case 8:
                return 45;
             case 10:
                 return 66;
             case 12:
                 return 91;
             case 14:
                 return 120;
            case 16:
                return 153;
            default:
                throw new RuntimeException("unsupported spharm order");
        }
    }

    public static List<Pair<Integer, Integer>> indices(int order)
    {
        // returns a list of (order, degree) for each coefficient

        Global.assume(order > 0, "invalid order: " + order);

        List<Pair<Integer, Integer>> out = Lists.newArrayList();

        int num = 1 + (order / 2);
        for (int i = 0; i < num; i++)
        {
            int n = 2 * i;

            for (int m = -n; m <= n; m++)
            {
                out.add(Pair.of(m, n));
            }
        }

        return out;
    }

    public static Vect bvect(int order, Vect dir)
    {
        try
        {
            int maxOrder = order;
            int numOrders = 1 + (maxOrder / 2);

            int[] orders = new int[numOrders];
            for (int i = 0; i < numOrders; i++)
            {
                orders[i] = 2 * i;
            }

            Global.assume(maxOrder > 0, "invalid order: " + order);

            int numCoeff = MathUtils.sum(orders) * 2 + numOrders;
            Vect B = new Vect(numCoeff);

            dir = dir.normalize();
            double phi = Math.atan2(dir.getY(), dir.getX());
            double z = dir.getZ();

            Vect allSinAbsMPhi = new Vect(maxOrder);
            Vect allCosAbsMPhi = new Vect(maxOrder);

            for (int j = 0; j < maxOrder; j++)
            {
                allSinAbsMPhi.set(j, Math.sin(phi * (j + 1)));
                allCosAbsMPhi.set(j, Math.cos(phi * (j + 1)));
            }

            int idx = 0;
            for (int k = 0; k < numOrders; k++)
            {
                int l = orders[k];

                Vect prePlm = new Vect(l + 1);
                for (int j = 0; j < l + 1; j++)
                {
                    if (j >= 0 && Math.abs(z) <= 1.0D)
                    {
                        prePlm.set(j, plgndr(l, j, z));
                    }
                }

                Vect one = VectSource.create1D(1);
                double NlmA = (l + l + 1) / 4.0 / Math.PI;
                Vect NlmB = l == 0 ? one : one.cat(VectSource.seq(l + 1, 1, l + l)).cumprod();
                Vect NlmC = l == 0 ? one : one.cat(VectSource.seq(l, -1, 1)).cumprod();

                Vect Nlm = NlmB.recip().times(NlmC.recip()).times(NlmA).sqrt();

                Vect preScalar = Nlm.times(one.cat(VectSource.createND(l, Math.sqrt(2))));

                int numShells = 2 * (l + 1) - 1;
                Vect Plm = new Vect(numShells);
                Vect Scalar = new Vect(numShells);
                Vect SinOrCos = new Vect(numShells);

                Scalar.set(l, preScalar.get(0));
                Plm.set(l, prePlm.get(0));
                SinOrCos.set(l, 1);
                for (int i = 1; i <= l; i++)
                {
                    Scalar.set(l - i, preScalar.get(i));
                    Scalar.set(l + i, preScalar.get(i));
                    Plm.set(l - i, prePlm.get(i));
                    Plm.set(l + i, prePlm.get(i));
                    SinOrCos.set(l - i, allSinAbsMPhi.get(i - 1));
                    SinOrCos.set(l + i, allCosAbsMPhi.get(i - 1));
                }

                for (int j = 0; j < numShells; j++)
                {
                    B.set(idx, Scalar.get(j) * Plm.get(j) * SinOrCos.get(j));

                    idx += 1;
                }
            }

            return B;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public static Matrix bmatrix(int order, Vects dirs)
    {
        Matrix B = new Matrix(dirs.size(), Spharm.orderToSize(order));
        for (int i = 0; i < dirs.size(); i++)
        {
            B.setRow(i, bvect(order, dirs.get(i)));
        }
        return B;
    }

    private static double plgndr(int l, int m, double x)
    {
        // P^{m}_{l}(x)
        double fact = 0.0;
        double pll = 0.0;
        double pmm = 0.0;
        double pmmp1 = 0.0;
        double somx2 = 0.0;
        int i = 0;
        int ll = 0;

        if (m < 0 || m > l || Math.abs(x) > 1.0)
        {
            throw new RuntimeException("invalid arguments");
        }
        pmm = 1.0;
        if (m > 0)
        {
            somx2 = Math.sqrt((1.0 - x) * (1.0 + x));
            fact = 1.0;
            for (i = 1; i <= m; i++)
            {
                pmm *= -fact * somx2;
                fact += 2.0;
            }
        }
        if (l == m)
        {
            return pmm;
        } else
        {
            pmmp1 = x * (2 * m + 1) * pmm;
            if (l == (m + 1))
            {
                return pmmp1;
            } else
            {
                for (ll = m + 2; ll <= l; ll++)
                {
                    pll = (x * (2 * ll - 1) * pmmp1 - (ll + m - 1) * pmm) / (ll - m);
                    pmm = pmmp1;
                    pmmp1 = pll;
                }
                return pll;
            }
        }
    }

    public static boolean matches(String name)
    {
        if (name == null)
        {
            return false;
        }

        String lname = name.toLowerCase();
        return lname.contains(NAME) || lname.contains("fod") || lname.contains("FOD");
    }


    private Vect params;

    public Spharm(int size)
    {
        this.params = VectSource.createND(size);
    }

    public Spharm(Vect params)
    {
        this.params = params.copy();
    }

    public double baseline()
    {
        // get the lowest order coefficient
        // this is not the same as other models!
        return this.params.get(0);
    }

    public Spharm set(Spharm t)
    {
        this.params = t.params.copy();

        return this;
    }

    public Spharm copy()
    {
        Spharm out = proto();
        out.set(this);
        return out;
    }

    public Spharm proto()
    {
        return new Spharm(this.params.copy());
    }

    public int getEncodingSize()
    {
        return this.params.size();
    }

    public Spharm setEncoding(Vect encoding)
    {
        this.params.set(encoding);

        return this;
    }

    public void getEncoding(Vect encoding)
    {
        encoding.set(this.params);
    }

    public double dist(Spharm t)
    {
        return this.params.dist(t.params);
    }

    public Vect feature(String name)
    {
        if (SIZE.equals(name))
        {
            return VectSource.create1D(this.params.get(0));
        } else if (SUM.equals(name))
        {
            return VectSource.create1D(this.params.sum());
        } else if (MIN.equals(name))
        {
            return VectSource.create1D(this.params.min());
        } else if (MAX.equals(name))
        {
            return VectSource.create1D(this.params.max());
        } else if (MEAN.equals(name))
        {
            return VectSource.create1D(this.params.mean());
        } else
        {
            throw new RuntimeException("invalid feature: " + name);
        }
    }

    public List<String> features()
    {
        return Lists.newArrayList(FEATURES);
    }

    public Spharm getThis()
    {
        return this;
    }

    public double sample(Vect dir)
    {
        int order = Spharm.sizeToOrder(this.getEncodingSize());
        Vect amps = Spharm.bmatrix(order, VectsSource.create(dir)).times(this.params);
        return amps.get(0);
    }
}