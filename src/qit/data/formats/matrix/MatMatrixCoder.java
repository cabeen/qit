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

package qit.data.formats.matrix;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import qit.base.Logging;
import qit.base.structs.LEDataInputStream;
import qit.data.datasets.Affine;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;

import java.io.*;
import java.util.List;
import java.util.function.Consumer;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;

/*
 * An affine transformation produced by ANTs
 *
 * https://github.com/ANTsX/ANTs/wiki/ITK-affine-transform-conversion
 *
 * AffineTransform_double_3_3<binary 3x3 matrix><binary 3-vector>fixed
 *
 */
public class MatMatrixCoder
{
    public static Matrix read(String fn) throws IOException
    {
        Matrix out = new Matrix(4, 4);

        LEDataInputStream dis = new LEDataInputStream(new FileInputStream(fn));

        try
        {
            // this assumes the header is fixed
            // this part include AffineTransform_double_3_3
            for (int i = 0; i < 47; i++)
            {
                dis.readByte();
            }

            // read the matrix elements and convert to RAS
            Matrix C = new Matrix(4, 4);

            // linear part
            C.set(0, 0, dis.readDouble());
            C.set(0, 1, dis.readDouble());
            C.set(0, 2, dis.readDouble());
            C.set(1, 0, dis.readDouble());
            C.set(1, 1, dis.readDouble());
            C.set(1, 2, dis.readDouble());
            C.set(2, 0, dis.readDouble());
            C.set(2, 1, dis.readDouble());
            C.set(2, 2, dis.readDouble());

            // translation part
            C.set(0, 3, dis.readDouble());
            C.set(1, 3, dis.readDouble());
            C.set(2, 3, dis.readDouble());
            C.set(3, 3, 1.0);

            // this assumes the header is fixed
            // this part include fixed
            for (int i = 0; i < 26; i++)
            {
                dis.readByte();
            }

            // center part
            Vect center = VectSource.create3D();
            center.set(0, dis.readDouble());
            center.set(1, dis.readDouble());
            center.set(2, dis.readDouble());

            // the ITK matrix format doesn't store the matrix directly
            // we have to compute it from an additional center of mass vector
            // and include some reflections to match the expected coordinate systems

            Matrix A = MatrixSource.scale4(-1,-1, 1);
            Matrix B = MatrixSource.trans4(center);
            Matrix D = MatrixSource.trans4(center.times(-1));
            Matrix E = MatrixSource.scale4(-1,-1, 1);

            out = A.times(B).times(C).times(D).times(E);
        }
        catch (Exception e)
        {
            dis.close();
        }

        return out;
    }
}
