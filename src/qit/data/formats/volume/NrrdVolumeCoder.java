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

package qit.data.formats.volume;

import com.google.common.collect.Lists;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/** a limited coder for the NRRD formate */
public class NrrdVolumeCoder
{
    public static boolean matches(String fn)
    {
        return fn.endsWith("nrrd");
    }

    public static Double bval = null;
    public static Vects bvecs = null;

    public static void write(Volume volume, OutputStream os) throws IOException
    {
        Sampling sampling = volume.getSampling();
        int nx = sampling.numI();
        int ny = sampling.numJ();
        int nz = sampling.numK();
        double dx = sampling.deltaI();
        double dy = sampling.deltaJ();
        double dz = sampling.deltaK();
        double sx = sampling.startI();
        double sy = sampling.startJ();
        double sz = sampling.startK();
        int dim = volume.getDim();

        List<String> header = Lists.newArrayList();
        header.add("NRRD0005");
        header.add("# Complete NRRD file format specification at:");
        header.add("# http://teem.sourceforge.net/nrrd/format.html");
        header.add("content: dwi");
        header.add("type: float");
        header.add("channel: 4");
        header.add("space: right-anterior-superior");
        header.add(String.format("sizes: %d %d %d %d", nx, ny, nz, dim));
        header.add(String.format("thicknesses: %g %g %g NaN", dx, dy, dz));
        header.add("space directions: (1,0,0) (0,1,0) (0,0,1) none");
        header.add("centerings: cell cell cell none");
        header.add("kinds: space space space list");
        header.add("endian: big");
        header.add("encoding: raw");
        header.add("space units: \"mm\" \"mm\" \"mm\"");
        header.add(String.format("space origin: (%g,%g,%g)", sx, sy, sz));
        header.add("byte skip: -1");
        header.add("measurement frame: (1,0,0) (0,1,0) (0,0,1)");

        if (bval != null)
        {
            // note: multiple b-values are encoded by changing
            //       the magnitude of  the bvecs
            header.add("modality:=DWMRI");
            header.add(String.format("DWMRI_b-value:=%g", bval));
            for (int i = 0; i < bvecs.size(); i++)
            {
                Vect bvec = bvecs.get(i);
                double bx = bvec.get(0);
                double by = bvec.get(1);
                double bz = bvec.get(2);
                header.add(String.format("DWMRI_gradient_%04d:= %g %g %g", i, bx, by, bz));
            }
        }

        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(os));
        for (String line : header)
        {
            out.writeBytes(line + "\n");
        }
        for (int i = 0; i < sampling.size(); i++)
        {
            for (int j = 0; j < dim; j++)
            {
                out.writeFloat((float) volume.get(i, j));
            }
        }
        out.close();
    }
}
