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

package qit.data.models;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Model;
import qit.data.datasets.Matrix;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.MatrixSource;
import qit.data.source.MeshSource;
import qit.data.source.VectSource;
import qit.data.utils.mri.CaminoUtils;
import qit.data.utils.mri.structs.Gradients;
import qit.math.structs.Face;
import qit.math.structs.VectFunction;
import qit.math.structs.Triangle;
import qit.math.structs.Vertex;
import qit.math.utils.MathUtils;

public class Noddi extends Model<Noddi>
{
    public final static String NAME = "noddi";

    public static final String ECVF = "fecvf"; // extracellular volume fraction
    public static final String ODI = "odi"; // orientation dispersion index
    public static final String TORT = "tort"; // tortuosity (tau)
    public static final String EAD = "ead"; // extracelluar axial diffusivity (based on tortuosity model)
    public static final String ERD = "erd";  // extracellular radial diffusivity (based on tortuosity model)
    public static final String DIRX = "fibredirs_xvec"; // x principal axis of fibers
    public static final String DIRY = "fibredirs_yvec"; // y principal axis of fibers
    public static final String DIRZ = "fibredirs_zvec"; // z principal axis of fibers

    public static final String BASELINE = "baseline"; // baseline signal
    public static final String FICVF = "ficvf"; // intracellular volume fraction (neurite density)
    public static final String FISO = "fiso"; // isotropic volume fraction
    public static final String KAPPA = "kappa"; // kappa (Watson distribution dispersion parameter)
    public static final String DIR = "dir"; // principal axis of fibers
    public static final String IRFRAC = "irfrac"; // the dot compartment for ex vivo
    public static final int DIM = 8; // parameterized by the above variables (hint: 3 for dir)

    public final static String[] FEATURES = {BASELINE, FICVF, ECVF, FISO, ODI, DIR, KAPPA, TORT, EAD, ERD, IRFRAC};

    public final static int DEFAULT_SUBDIV = 2; // experiments show that this should be two or greater
    public final static int TORTUOSITY_RESOLUTION = 100; // resolution for numerical integral of tortuosity
    public final static int TORTUOSITY_SAMPLES = 1000; // samples for lookup table for tortuosity model
    private static double[] TORTUOSITY_LOOKUP = null; // lookup table to look up tortuosity

    public final static double INVIVO_PARALLEL = 1.7e-3; // default parallel diffusivity of sticks
    public final static double INVIVO_ISOTROPIC = 3.0e-3; // default isotropic diffusivity
    public final static double EXVIVO_PARALLEL = 0.6-3; // default parallel diffusivity of sticks
    public final static double EXVIVO_ISOTROPIC = 1.0e-3; // default isotropic diffusivity

    public static double PARALLEL = INVIVO_PARALLEL;
    public static double ISOTROPIC = INVIVO_ISOTROPIC;

    public double base;
    public double ficvf;
    public double fiso;
    public double kappa;
    public double irfrac;
    public Vect dir;

    public static boolean matches(String name)
    {
        if (name == null)
        {
            return false;
        }

        String lname = name.toLowerCase();
        return lname.endsWith(".ndi") || lname.endsWith(".noddi");
    }

    public Noddi()
    {
        this.clear();
    }

    public Noddi(Noddi model)
    {
        this();
        this.set(model);
    }

    public Noddi(Vect encoding)
    {
        this();
        this.setEncoding(encoding);
    }

    public int getDegreesOfFreedom()
    {
        // two for volume fractions
        // one for dispersion
        // two for orientation
        // one for dot

        return 6;
    }

    public void clear()
    {
        this.ficvf = 0;
        this.fiso = 1;
        this.kappa = 1;
        this.irfrac = 0;
        this.dir = VectSource.create3D(1, 0, 0);
    }

    public double baseline()
    {
        return this.base;
    }

    public double getBaseline()
    {
        return this.base;
    }

    public double getFICVF()
    {
        return this.ficvf;
    }

    public double getFECVF()
    {
        return 1.0 - this.ficvf;
    }

    public double getFISO()
    {
        return this.fiso;
    }

    public double getODI()
    {
        return kappaToDispersionIndex(this.kappa);
    }

    public double getKappa()
    {
        return this.kappa;
    }

    public double getIRFRAC()
    {
        return this.irfrac;
    }

    public double getTORT()
    {
        return dispersionIndexToTortuosityLookup(this.getODI());
    }

    public double getEAD()
    {
        return tortuosityToExtracellularAxialDiffusivity(this.getTORT(), this.getFICVF());
    }

    public double getERD()
    {
        return tortuosityToExtracellularRadialDiffusivity(this.getTORT(), this.getFICVF());
    }

    public Vect getDir()
    {
        return this.dir;
    }

    public void setBaseline(double v)
    {
        this.base = v;
    }

    public void setFICVF(double v)
    {
        this.ficvf = v;
    }

    public void setFISO(double v)
    {
        this.fiso = v;
    }

    public void setODI(double v)
    {
        this.kappa = dispersionIndexToKappa(v);
    }

    public void setKappa(double v)
    {
        this.kappa = v;
    }

    public void setIRFRAC(double v)
    {
        this.irfrac = v;
    }

    public void setDir(Vect v)
    {
        this.dir = v;
    }

    public Matrix getScatter()
    {
        return kappaToScatter(this.getDir(), this.getKappa(), false);
    }

    public Matrix getScatter(boolean log)
    {
        return kappaToScatter(this.getDir(), this.getKappa(), log);
    }

    public double dist(Noddi model)
    {
        // this has not been tested, but it seems like a reasonable approach
        double dfiso = this.fiso - model.fiso;
        double dficvf = this.ficvf - model.ficvf;
        double dmat = this.getScatter().minus(model.getScatter()).normF();

        double out = 0;
        out += dfiso * dfiso;
        out += dficvf * dficvf;
        out += dmat * dmat;
        out = Math.sqrt(out);

        return out;
    }

    @Override
    public Noddi set(Noddi model)
    {
        this.clear();
        this.base = model.base;
        this.ficvf = model.ficvf;
        this.fiso = model.fiso;
        this.kappa = model.kappa;
        this.irfrac = model.irfrac;
        this.dir = model.dir.copy();
        return this;
    }

    @Override
    public Noddi copy()
    {
        return new Noddi(this);
    }

    @Override
    public Noddi proto()
    {
        return new Noddi();
    }

    @Override
    public int getEncodingSize()
    {
        return DIM;
    }

    @Override
    public Noddi setEncoding(Vect encoding)
    {
        this.base = encoding.get(0);
        this.ficvf = encoding.get(1);
        this.fiso = encoding.get(2);
        this.kappa = encoding.get(3);
        this.dir = encoding.sub(4, 7).normalize();
        this.irfrac = encoding.get(7);

        return this;
    }

    @Override
    public void getEncoding(Vect encoding)
    {
        Global.assume(encoding.size() == this.getEncodingSize(), "invalid encoding");

        encoding.set(0, this.base);
        encoding.set(1, this.ficvf);
        encoding.set(2, this.fiso);
        encoding.set(3, this.kappa);
        encoding.set(4, this.dir.get(0));
        encoding.set(5, this.dir.get(1));
        encoding.set(6, this.dir.get(2));
        encoding.set(7, this.irfrac);
    }

    @Override
    public String toString()
    {
        String out = "{ficvf: " + this.ficvf + ", fiso: " + this.fiso + ", od: " + this.getODI() + ", dir: [";
        out += this.dir.get(0) + ", " + this.dir.get(1) + ", " + this.dir.get(2) +  ", " + this.irfrac + "]}";

        return out;
    }

    @Override
    public List<String> features()
    {
        List<String> out = Lists.newArrayList();
        for (String feature : FEATURES)
        {
            out.add(feature);
        }

        return out;
    }

    @Override
    public Vect feature(String name)
    {
        if (BASELINE.equals(name))
        {
            return VectSource.create1D(this.getBaseline());
        }
        if (FICVF.equals(name))
        {
            return VectSource.create1D(this.getFICVF());
        }
        if (ECVF.equals(name))
        {
            return VectSource.create1D(this.getFECVF());
        }
        if (FISO.equals(name))
        {
            return VectSource.create1D(this.getFISO());
        }
        if (ODI.equals(name))
        {
            return VectSource.create1D(this.getODI());
        }
        if (DIR.equals(name))
        {
            return this.getDir();
        }
        if (KAPPA.equals(name))
        {
            return VectSource.create1D(this.getKappa());
        }
        if (TORT.equals(name))
        {
            return VectSource.create1D(this.getTORT());
        }
        if (EAD.equals(name))
        {
            return VectSource.create1D(this.getEAD());
        }
        if (ERD.equals(name))
        {
            return VectSource.create1D(this.getERD());
        }
        if (DIRX.equals(name))
        {
            return VectSource.create1D(this.getDir().getX());
        }
        if (DIRY.equals(name))
        {
            return VectSource.create1D(this.getDir().getY());
        }
        if (DIRZ.equals(name))
        {
            return VectSource.create1D(this.getDir().getZ());
        }
        if (IRFRAC.equals(name))
        {
            return VectSource.create1D(this.getIRFRAC());
        }

        throw new RuntimeException("invalid index: " + name);
    }

    @Override
    public Noddi getThis()
    {
        return this;
    }

    public static double kappaToTortuosity(double kappa)
    {
        // this should not be called to populate the lookup table, not during optimization
        // tortuosity model
        double num = 0;
        double denom = 0;

        double d = 1.0 / (2.0 * TORTUOSITY_RESOLUTION);
        for (int i = 0; i <= TORTUOSITY_RESOLUTION; i++)
        {
            boolean end = i == 0 || i == TORTUOSITY_RESOLUTION;
            double factor = d * (end ? 1 : 2);

            double u = (double) i / (double) TORTUOSITY_RESOLUTION;
            double u2 = u * u;
            double exp = Math.exp(kappa * u2);
            double term = factor * exp;

            num += u2 * term;
            denom += term;
        }

        double t = MathUtils.zero(denom) ? 1.0 : num / denom;

        return Double.isNaN(t) ? 1.0 : t;
    }

    public static double tortuosityToExtracellularAxialDiffusivity(double tort, double ficvf)
    {
        return PARALLEL * (1.0 - ficvf * (1 - tort));
    }

    public static double tortuosityToExtracellularRadialDiffusivity(double tort, double ficvf)
    {
        return PARALLEL * (1.0 - ficvf * (1 + tort) * 0.5);
    }

    public static double dispersionIndexToKappa(double odi)
    {
        double s = MathUtils.sign(odi);
        double k = s * 1.0 / Math.tan(s * Math.PI * odi / 2.0);
        if (Double.isInfinite(k))
        {
            k = Double.MAX_VALUE;
        }

        return k;
    }

    public static double kappaToDispersionIndex(double kappa)
    {
        return (2.0 / Math.PI) * Math.atan2(1.0, Math.abs(kappa));
    }

    public static double dispersionIndexToTortuosityLookup(double odi)
    {
        if (TORTUOSITY_LOOKUP == null)
        {
            // Compute a cache of (ODI, tortuosity) on a regular grid
            TORTUOSITY_LOOKUP = new double[TORTUOSITY_SAMPLES];
            for (int i = 0; i < TORTUOSITY_SAMPLES; i++)
            {
                double ods = i / (double) (TORTUOSITY_SAMPLES - 1);
                TORTUOSITY_LOOKUP[i] = kappaToTortuosity(dispersionIndexToKappa(ods));
            }
        }

        // use linear interpolation of cached valued
        double fidx = odi * (double) (TORTUOSITY_SAMPLES - 1);
        int idx = (int) Math.min(Math.max(0, Math.floor(fidx)), TORTUOSITY_SAMPLES);
        double frac = Math.min(Math.max(0, fidx - idx), 1);
        double low = TORTUOSITY_LOOKUP[idx];
        double high = TORTUOSITY_LOOKUP[Math.min(TORTUOSITY_SAMPLES - 1, idx + 1)];
        double tort = low * frac + (1.0 - frac) * high;

        return tort;
    }

    public static Matrix kappaToScatter(Vect dir, double kappa, boolean log)
    {
        if (Double.isInfinite(kappa))
        {
            kappa = Double.MAX_VALUE;
        }

        double l1 = CaminoUtils.lambdaWatson(kappa);
        double l2 = (1.0 - l1) / 2.0;
        double l3 = l2;

        if (log)
        {
            l1 = Math.log(l1 + Global.DELTA);
            l2 = Math.log(l2 + Global.DELTA);
            l3 = l2;
        }

        Matrix D = MatrixSource.diag(VectSource.create3D(l1, l2, l3));

        Vect v1 = dir;
        Vect v2 = v1.perp();
        Vect v3 = v1.cross(v2);

        Matrix V = new Matrix(3, 3);
        V.setRow(0, v1);
        V.setRow(1, v2);
        V.setRow(2, v3);

        Matrix mat = V.transpose().times(D).times(V);

        return mat;
    }

    public static VectFunction synth(final Gradients gradients)
    {
        return synth(gradients, DEFAULT_SUBDIV);
    }

    public static VectFunction synth(final Gradients gradients, int subdiv)
    {
        final Synther synther = new Synther(gradients, subdiv);

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                Noddi model = new Noddi();
                model.setEncoding(input);
                output.set(synther.synth(model));
            }
        }.init(new Noddi().getEncodingSize(), gradients.size());

        // input: noddi parameters
        // output: mri signal
    }

    public static class Synther
    {
        int resolution;
        Vects samples;
        Vect areas;
        Matrix basis;
        Gradients gradients;

        public Synther(Gradients gradients, int subdiv)
        {
            // use a numerical approximation on the sphere
            // subdiv can be 0, 1, 2, ... (2 is great, 1 is okay)

            Mesh mesh = MeshSource.sphere(subdiv + 1);
            Map<Vertex, Double> area = Maps.newHashMap();
            List<Vertex> vertices = Lists.newArrayList();

            for (Vertex vert : mesh.graph.verts())
            {
                area.put(vert, Double.valueOf(0));
                vertices.add(vert);
            }

            for (Face face : mesh.graph.faces())
            {
                Vertex a = face.getA();
                Vertex b = face.getB();
                Vertex c = face.getC();

                Vect va = mesh.vattr.get(a, Mesh.COORD);
                Vect vb = mesh.vattr.get(b, Mesh.COORD);
                Vect vc = mesh.vattr.get(c, Mesh.COORD);

                Vect vab = va.plus(vb).times(1.0 / 2.0).normalize();
                Vect vac = va.plus(vc).times(1.0 / 2.0).normalize();
                Vect vbc = vb.plus(vc).times(1.0 / 2.0).normalize();
                Vect vabc = va.plus(vb).plus(vc).times(1.0 / 3.0).normalize();

                double wa = new Triangle(va, vab, vabc).area() + new Triangle(va, vac, vabc).area();
                double wb = new Triangle(vb, vab, vabc).area() + new Triangle(vb, vbc, vabc).area();
                double wc = new Triangle(vc, vac, vabc).area() + new Triangle(vc, vbc, vabc).area();

                area.put(a, area.get(a) + wa);
                area.put(b, area.get(b) + wb);
                area.put(c, area.get(c) + wc);
            }

            this.gradients = gradients;
            this.resolution = vertices.size();
            this.samples = new Vects();
            this.areas = VectSource.createND(this.resolution);
            this.basis = new Matrix(gradients.size(), this.resolution);

            for (int i = 0; i < this.resolution; i++)
            {
                this.samples.add(mesh.vattr.get(vertices.get(i), Mesh.COORD).normalize());
                double a = area.get(vertices.get(i));
                this.areas.set(i, a);
            }

            // this.areas.timesEquals(1.0 / this.areas.sum());

            for (int i = 0; i < gradients.size(); i++)
            {
                double b = gradients.getBval(i);
                Vect q = gradients.getBvec(i).normalize();

                for (int j = 0; j < this.resolution; j++)
                {
                    double qdotn = q.dot(this.samples.get(j));
                    double cn = Math.exp(-b * PARALLEL * qdotn * qdotn);
                    this.basis.set(i, j, cn);
                }
            }
        }

        public Matrix basis(Noddi model)
        {
            double base = model.getBaseline();
            double kappa = model.getKappa();
            double ead = model.getEAD();
            double erd = model.getERD();
            Vect mu = model.getDir();

            Matrix eicten = MatrixSource.outer(mu, mu).times(ead - erd).plus(MatrixSource.identity(3).times(erd));

            Vect probv = VectSource.createND(this.resolution);
            for (int j = 0; j < this.resolution; j++)
            {
                double mudotn = mu.dot(this.samples.get(j));
                double watson = Math.exp(kappa * mudotn * mudotn);
                watson = Double.isInfinite(watson) ? Math.exp(1e6 * mudotn * mudotn) : watson;
                double area = this.areas.get(j);
                double prob = area * watson;

                probv.set(j, prob);
            }
            probv.timesEquals(1.0 / probv.sum());

            Matrix output = new Matrix(this.gradients.size(), 3);
            for (int i = 0; i < this.gradients.size(); i++)
            {
                double b = this.gradients.getBval(i);
                Vect q = this.gradients.getBvec(i).normalize();

                if (MathUtils.zero(base))
                {
                    output.set(i, 0, 0);
                    output.set(i, 1, 0);
                    output.set(i, 2, 0);
                }
                else
                {
                    double aic = this.basis.getRow(i).dot(probv);
                    double aec = Math.exp(-b * eicten.times(q).dot(q));
                    double aiso = Math.exp(-b * Noddi.ISOTROPIC);

                    if (Double.isNaN(aic))
                    {
                        aic = 0;
                    }

                    output.set(i, 0, aic);
                    output.set(i, 1, aec);
                    output.set(i, 2, aiso);
                }
            }

            return output;
        }

        public Vect synth(Noddi model)
        {
            double base = model.getBaseline();
            double fiso = model.getFISO();
            double fisoi = 1.0 - fiso;
            double ficvf = model.getFICVF();
            double ficvfi = 1.0 - ficvf;

            double fic = fisoi * ficvf;
            double fec = fisoi * ficvfi;

            Vect fracs = VectSource.create3D(fic, fec, fiso);

            return this.basis(model).times(fracs).plus(model.getIRFRAC()).times(base);
        }
    }
}
