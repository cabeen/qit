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

package qit.data.utils.mri.structs;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import qit.base.Dataset;
import qit.base.Global;
import qit.base.Logging;
import qit.base.cli.CliUtils;
import qit.base.structs.Pair;
import qit.base.utils.JsonUtils;
import qit.base.utils.PathUtils;
import qit.data.datasets.Affine;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.data.source.VectsSource;
import qit.data.utils.VectsUtils;
import qit.math.source.VectFunctionSource;
import qit.math.structs.VectFunction;
import qit.math.structs.Quaternion;
import qit.math.utils.MathUtils;

public class Gradients implements Dataset
{
    public static final double GYRO = 2.675987E8; // this assumes the magnet strength

    public static final double BVAL_DEFAULT = 0;
    public static final double GMAG_DEFAULT = 0;
    public static final double BIGDEL_DEFAULT = 0;
    public static final double LILDEL_DEFAULT = 0;
    public static final double TE_DEFAULT = 0;

    public static final int BVECX_IDX = 0;
    public static final int BVECY_IDX = 1;
    public static final int BVECZ_IDX = 2;
    public static final int BVAL_IDX = 3;
    public static final int GMAG_IDX = 4;
    public static final int BIGDEL_IDX = 5;
    public static final int LILDEL_IDX = 6;
    public static final int TE_IDX = 7;
    public static final int DIM = 8;

    private transient List<Integer> zero = Lists.newArrayList();
    private transient List<Integer> dirs = Lists.newArrayList();
    private transient boolean complete = false;

    Vects entries = new Vects();

    @SuppressWarnings("unused")
    private Gradients()
    {
    }

    public Gradients(Vects vs)
    {
        int dim = vs.getDim();
        boolean valid = dim == DIM || dim == 4 || dim == DIM - 1;
        Global.assume(valid, "invalid gradients dimension: " + dim);

        if (dim == DIM)
        {
            this.entries = vs.copy();
            this.complete = true;
        }
        else if (dim == DIM - 1)
        {
            for (Vect v : vs)
            {
                Vect entry = VectSource.createND(DIM);
                entry.set(BVECX_IDX, v.get(0));
                entry.set(BVECY_IDX, v.get(1));
                entry.set(BVECZ_IDX, v.get(2));
                entry.set(BVAL_IDX, BVAL_DEFAULT); // @TODO: compute this!
                entry.set(GMAG_IDX, v.get(3));
                entry.set(BIGDEL_IDX, v.get(4));
                entry.set(LILDEL_IDX, v.get(5));
                entry.set(TE_IDX, v.get(6));
                this.entries.add(entry);
            }

            this.complete = true;
        }
        else
        {
            for (Vect v : vs)
            {
                Vect entry = VectSource.createND(DIM);
                entry.set(BVECX_IDX, v.get(0));
                entry.set(BVECY_IDX, v.get(1));
                entry.set(BVECZ_IDX, v.get(2));
                entry.set(BVAL_IDX, v.get(3));
                entry.set(GMAG_IDX, GMAG_DEFAULT);
                entry.set(BIGDEL_IDX, BIGDEL_DEFAULT);
                entry.set(LILDEL_IDX, LILDEL_DEFAULT);
                entry.set(TE_IDX, TE_DEFAULT);
                this.entries.add(entry);
            }

            this.complete = false;
        }

        this.init();
    }

    public Gradients(Vects bvecs, Vects bvals)
    {
        if (bvals.getDim() != 1)
        {
            Logging.info("transposing bvals");
            bvals = bvals.transpose();
        }

        if (bvecs.getDim() == bvals.size())
        {
            Logging.info("transposing bvecs");
            bvecs = bvecs.transpose();
        }

        Logging.info("using b-vales with length " + bvals.size());
        Logging.info("using b-vecs with length %d and dimension %d", bvecs.size(), bvecs.getDim());

        Global.assume(bvecs.size() == bvals.size(), String.format("invalid gradients"));

        for (int i = 0; i < bvecs.size(); i++)
        {
            Vect bvec = bvecs.get(i);
            Vect bval = bvals.get(i);

            Vect entry = VectSource.createND(DIM);
            entry.set(BVECX_IDX, bvec.get(0));
            entry.set(BVECY_IDX, bvec.get(1));
            entry.set(BVECZ_IDX, bvec.get(2));
            entry.set(BVAL_IDX, bval.get(0));
            entry.set(BIGDEL_IDX, BIGDEL_DEFAULT);
            entry.set(LILDEL_IDX, LILDEL_DEFAULT);
            entry.set(TE_IDX, TE_DEFAULT);
            this.entries.add(entry);
        }

        this.complete = false;

        this.init();
    }

    public Gradients(Vects bvecs, double bval)
    {
        for (int i = 0; i < bvecs.size(); i++)
        {
            Vect bvec = bvecs.get(i);

            Vect entry = VectSource.createND(DIM);
            entry.set(BVECX_IDX, bvec.get(0));
            entry.set(BVECY_IDX, bvec.get(1));
            entry.set(BVECZ_IDX, bvec.get(2));
            entry.set(BVAL_IDX, bval);
            entry.set(BIGDEL_IDX, BIGDEL_DEFAULT);
            entry.set(LILDEL_IDX, LILDEL_DEFAULT);
            entry.set(TE_IDX, TE_DEFAULT);
            this.entries.add(entry);
        }

        this.init();
    }

    private Gradients init()
    {
        Global.assume(this.entries.size() != 0, "empty gradient bvecs");
        Global.assume(this.entries.getDim() == DIM, "empty gradient bvecs");

        for (int i = 0; i < this.size(); i++)
        {
            Vect entry = this.entries.get(i);
            entry.set(0, entry.sub(0, 3).normalize());
        }

        double maxBvalue = 0;
        for (int i = 0; i < this.size(); i++)
        {
            maxBvalue = Math.max(maxBvalue, this.entries.get(i).get(BVAL_IDX));
        }

        // Sometimes there is a small non-zero baseline b-value
        // let's assume less than one percent of the max
        double thresh = 0.01 * maxBvalue;
        for (int i = 0; i < this.size(); i++)
        {
            double bval = this.entries.get(i).get(BVAL_IDX);
            if (MathUtils.zero(bval) || MathUtils.zero(this.entries.get(i).sub(0, 3).norm()) || bval < thresh)
            {
                this.zero.add(i);
            }
            else
            {
                this.dirs.add(i);
            }
        }

        return this;
    }

    public double zero(Vect signal)
    {
        return this.zeros(signal).mean();
    }

    public Vect zeros(Vect signal)
    {
        return signal.sub(this.zero);
    }

    public Vect dnorm(Vect signal)
    {
        double norm = 1.0 / this.zero(signal);

        Vect out = VectSource.createND(this.dirs.size());
        for (int i = 0; i < this.dirs.size(); i++)
        {
            out.set(i, norm * signal.get(this.dirs.get(i)));
        }

        return out;
    }

    public Vect norm(Vect signal)
    {
        return signal.divSafe(this.zero(signal));
    }

    public Vect adc(Vect signal)
    {
        return this.norm(signal).log().times(this.getBvals().flatten()).times(-1);
    }

    public double getDval(int idx)
    {
        return this.getBval(this.dirs.get(idx));
    }

    public Vect getDvec(int idx)
    {
        return this.getBvec(this.dirs.get(idx));
    }

    public Vect getBvec(int idx)
    {
        double bvx = this.entries.get(idx).get(BVECX_IDX);
        double bvy = this.entries.get(idx).get(BVECY_IDX);
        double bvz = this.entries.get(idx).get(BVECZ_IDX);

        return VectSource.create3D(bvx, bvy, bvz);
    }

    public double getBval(int idx)
    {
        return this.entries.get(idx).get(BVAL_IDX);
    }

    public double getGMag(int idx)
    {
        return this.entries.get(idx).get(GMAG_IDX);
    }

    public double getBigDelta(int idx)
    {
        return this.entries.get(idx).get(BIGDEL_IDX);
    }

    public double getLittleDelta(int idx)
    {
        return this.entries.get(idx).get(LILDEL_IDX);
    }

    public double getEchoTime(int idx)
    {
        return this.entries.get(idx).get(TE_IDX);
    }

    public Vects getBvals()
    {
        return this.entries.sub(BVAL_IDX, BVAL_IDX + 1);
    }

    public Vects getBvecs()
    {
        Vects out = new Vects();
        for (int i = 0; i < this.size(); i++)
        {
            out.add(this.getBvec(i));
        }

        return out;
    }

    public Vects getDvecs()
    {
        Vects out = new Vects();
        for (int idx : this.dirs)
        {
            out.add(this.getBvec(idx));
        }

        return out;
    }

    public List<Integer> getBaselineIdx()
    {
        return Lists.newArrayList(this.zero);
    }

    public List<Integer> getDvecIdx()
    {
        return Lists.newArrayList(this.dirs);
    }

    public List<Integer> getShellsIdx(boolean baseline, String shells)
    {
        List<Integer> which = Lists.newArrayList();

        List<Integer> pshells = CliUtils.parseWhich(shells);

        for (int i = 0; i < this.size(); i++)
        {
            int bval = (int) Math.round(this.getBval(i));

            if ((baseline && bval == 0) || pshells.contains(bval))
            {
                which.add(i);
            }
        }

        return which;
    }

    public boolean multishell()
    {
        return this.getShells(false).size() > 1;
    }

    public List<Integer> getShellsIdx(int shell)
    {
        List<Integer> which = Lists.newArrayList();

        for (int i = 0; i < this.size(); i++)
        {
            int bval = (int) Math.round(this.getBval(i));

            if (bval == shell)
            {
                which.add(i);
            }
        }

        return which;
    }

    public List<Integer> getShellsDIdx(int shell)
    {
        List<Integer> which = Lists.newArrayList();

        for (int i = 0; i < this.getNumDvecs(); i++)
        {
            int bval = (int) Math.round(this.getDval(i));

            if (bval == shell)
            {
                which.add(i);
            }
        }

        return which;
    }

    public List<Integer> getShells(boolean baseline)
    {
        List<Integer> shells = Lists.newArrayList();

        for (int i = 0; i < this.size(); i++)
        {
            int bval = (int) Math.round(this.getBval(i));

            if (!baseline && bval == 0)
            {
                continue;
            }

            if (shells.contains(bval))
            {
                continue;
            }

            shells.add(bval);
        }

        Collections.sort(shells);

        return shells;
    }

    public int getNumDvecs()
    {
        return this.dirs.size();
    }

    public int getNumBaselines()
    {
        return this.zero.size();
    }

    public Vects vects()
    {
        return this.entries.copy();
    }

    public int size()
    {
        return this.entries.size();
    }

    public Gradients rotate(Quaternion quat)
    {
        Gradients out = this.copy();

        Affine affine = new Affine(quat.matrix(), VectSource.create3D());
        for (int i = 0; i < this.size(); i++)
        {
            Vect bvec = this.getBvec(i);
            Vect rot = affine.apply(bvec);
            out.entries.get(i).set(BVECX_IDX, rot.getX());
            out.entries.get(i).set(BVECY_IDX, rot.getY());
            out.entries.get(i).set(BVECZ_IDX, rot.getZ());
        }

        return out;
    }

    public Gradients transform(Affine xfm)
    {
        Gradients out = this.copy();

        for (int i = 0; i < this.size(); i++)
        {
            Vect bvec = this.getBvec(i);
            Vect rot = xfm.apply(bvec);
            out.entries.get(i).set(BVECX_IDX, rot.getX());
            out.entries.get(i).set(BVECY_IDX, rot.getY());
            out.entries.get(i).set(BVECZ_IDX, rot.getZ());
        }

        return out;
    }

    public Gradients transform(Matrix xfm)
    {
        Gradients out = this.copy();

        for (int i = 0; i < this.size(); i++)
        {
            Vect bvec = this.getBvec(i);
            Vect rot = xfm.times(bvec);
            out.entries.get(i).set(BVECX_IDX, rot.getX());
            out.entries.get(i).set(BVECY_IDX, rot.getY());
            out.entries.get(i).set(BVECZ_IDX, rot.getZ());
        }

        return out;
    }

    public Gradients copy(boolean[] subset)
    {
        Global.assume(subset.length == this.size(), "invalid subset length");

        Gradients out = new Gradients();
        for (int i = 0; i < subset.length; i++)
        {
            if (subset[i])
            {
                out.entries.add(this.entries.get(i));
            }
        }
        out.complete = this.complete;
        out.init();

        return out;
    }

    public void flipX()
    {
        for (Vect entry : this.entries)
        {
            entry.set(BVECX_IDX, -entry.get(BVECX_IDX));
        }
    }

    public void flipY()
    {
        for (Vect entry : this.entries)
        {
            entry.set(BVECY_IDX, -entry.get(BVECY_IDX));
        }
    }

    public void flipZ()
    {
        for (Vect entry : this.entries)
        {
            entry.set(BVECZ_IDX, -entry.get(BVECZ_IDX));
        }
    }

    public Gradients copy()
    {
        Gradients out = new Gradients();
        out.entries = this.entries.copy();
        out.complete = this.complete;
        out.init();

        return out;
    }

    public List<String> getExtensions()
    {
        return Lists.newArrayList(new String[]{"txt", "json", "scheme"});
    }

    public void write(String fn) throws IOException
    {
        PathUtils.mkpar(fn);

        if (fn.endsWith(JsonUtils.EXT))
        {
            FileUtils.write(new File(fn), JsonUtils.encode(this), false);
        }
        else if (fn.contains("vec"))
        {
            String bvecs_fn = fn;
            String bvals_fn = fn.replaceFirst("vec", "val");
            this.write(bvecs_fn, bvals_fn);
        }
        else if (fn.contains("vec"))
        {
            String bvecs_fn = fn;
            String bvals_fn = fn.replaceFirst("vec", "val");
            this.write(bvecs_fn, bvals_fn);
        }
        else if (fn.endsWith("scheme") || fn.endsWith("scheme1"))
        {
            PrintWriter pw = new PrintWriter(fn);

            String version = fn.endsWith("1") ? "1" : this.complete ? "STEJSKALTANNER" : "BVECTOR";
            pw.println("VERSION: " + version);

            for (Vect entry : this.entries)
            {
                if (this.complete)
                {
                    String[] tokens = new String[DIM - 1];
                    tokens[0] = String.valueOf(entry.get(BVECX_IDX));
                    tokens[1] = String.valueOf(entry.get(BVECY_IDX));
                    tokens[2] = String.valueOf(entry.get(BVECZ_IDX));
                    tokens[3] = String.valueOf(entry.get(GMAG_IDX));
                    tokens[4] = String.valueOf(entry.get(BIGDEL_IDX));
                    tokens[5] = String.valueOf(entry.get(LILDEL_IDX));
                    tokens[6] = String.valueOf(entry.get(TE_IDX));
                    pw.println(StringUtils.join(tokens, " "));
                }
                else
                {
                    String[] tokens = new String[4];
                    tokens[0] = String.valueOf(entry.get(BVECX_IDX));
                    tokens[1] = String.valueOf(entry.get(BVECY_IDX));
                    tokens[2] = String.valueOf(entry.get(BVECZ_IDX));
                    tokens[3] = String.valueOf(entry.get(BVAL_IDX));
                    pw.println(StringUtils.join(tokens, " "));
                }
            }
            pw.close();
        }
        else
        {
            this.entries.write(fn);
        }
    }

    public void write(String bvecs_fn, String bvals_fn) throws IOException
    {
        this.getBvecs().write(bvecs_fn);
        this.getBvals().write(bvals_fn);
    }

    public static Gradients read(String fn) throws IOException
    {
        if (fn.endsWith(JsonUtils.EXT))
        {
            return JsonUtils.decode(Gradients.class, Files.toString(new File(fn), Charsets.UTF_8)).init();
        }
        else if (fn.endsWith("scheme") || fn.endsWith("scheme1"))
        {
            Vects out = new Vects();

            BufferedReader br = new BufferedReader(new FileReader(fn));
            String line = null;

            while ((line = br.readLine()) != null)
            {
                // remove hashed comments
                String[] comment = line.split("#");

                if (comment.length == 0)
                {
                    continue;
                }

                String[] version = comment[0].trim().split("VERSION");

                if (version.length == 0)
                {
                    continue;
                }

                String body = version[0].trim();

                if (body.length() == 0)
                {
                    continue;
                }

                // split on a variety of possible delimiters
                String[] tokens = body.split("(\\s|,|;)+");

                if (tokens.length == 0)
                {
                    continue;
                }

                if (out.size() > 0 && tokens.length != out.getDim())
                {
                    br.close();
                    throw new RuntimeException("inconsistent dimensions");
                }

                Vect vect = new Vect(tokens.length);
                for (int i = 0; i < tokens.length; i++)
                {
                    String value = tokens[i].trim().replace("\r", "");
                    vect.set(i, Double.valueOf(value));
                }
                out.add(vect);
            }

            br.close();

            return new Gradients(out);
        }
        else if (fn.contains("bvec") || fn.contains("bval"))
        {
            String bvecs_fn = fn.contains("bvec") ? fn : fn.replaceFirst("bval", "bvec");
            String bvals_fn = fn.contains("bval") ? fn : fn.replaceFirst("bvec", "bval");

            Global.assume(PathUtils.exists(bvecs_fn), "bvecs do not exist: " + bvecs_fn);
            Global.assume(PathUtils.exists(bvals_fn), "bvals do not exist:" + bvals_fn);

            return read(bvecs_fn, bvals_fn);
        }
        else
        {
            return new Gradients(Vects.read(fn));
        }
    }

    public static Gradients read(String vecs, String vals) throws IOException
    {
        return read(null, vecs, vals);
    }

    public static Gradients read(Integer dim, String vecs, String vals) throws IOException
    {
        Vects bvecs = null;
        Vects bvals = null;

        if (!PathUtils.exists(vals))
        {
            Vects data = Vects.read(vecs);

            bvals = VectsUtils.index(data, 3);
            bvecs = VectsUtils.index(data, new int[]{0, 1, 2});
        }
        else
        {
            bvecs = Vects.read(vecs);
            bvals = Vects.read(vals);
        }

        if (dim != null)
        {
            VectsUtils.packFrontTo(bvecs, dim, VectSource.create3D(0, 0, 0));
            VectsUtils.packFrontTo(bvals, dim, VectSource.create1D(0));
        }

        for (Vect bvec : bvecs)
        {
            bvec.normalizeEquals();
        }

        return new Gradients(bvecs, bvals);
    }

    public Pair<Gradients, VectFunction> subset(String shells, String which, String exclude)
    {
        if (shells == null && which == null && exclude == null)
        {
            return Pair.of(this.copy(), VectFunctionSource.identity(this.size()));
        }

        int size = this.size();
        boolean[] subset = new boolean[this.size()];

        for (int i = 0; i < size; i++)
        {
            subset[i] = true;
        }

        if (which != null)
        {
            List<Integer> whichidx = CliUtils.parseWhich(which);

            for (int i = 0; i < size; i++)
            {
                if (subset[i] && !whichidx.contains(i))
                {
                    subset[i] = false;
                }
            }
        }

        if (exclude != null)
        {
            for (int i : CliUtils.parseWhich(exclude))
            {
                subset[i] = false;
            }
        }

        if (shells != null)
        {
            List<Integer> pshells = CliUtils.parseWhich(shells);

            for (int i = 0; i < size; i++)
            {
                if (subset[i] && !pshells.contains((int) Math.round(this.getBval(i))))
                {
                    subset[i] = false;
                }
            }
        }

        Gradients out = this.copy(subset);

        String msg = "using a subset of bvalues:";
        for (Integer shell : out.getShells(true))
        {
            msg += String.format(" %d", shell);
        }
        Logging.info(msg);


        return Pair.of(out, VectFunctionSource.subset(subset));
    }
}