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

import qit.data.datasets.Affine;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** a coder for the trackvis file format
 * http://trackvis.org/docs/?subsect=fileformat */
public class DtkHeader
{
    public char[] id_string = new char[6];
    public int[] dim = new int[3];
    public float[] voxel_size = new float[3];
    public float[] origin = new float[3];
    public int n_scalars;
    public char[][] scalar_name = new char[10][20];
    public int n_properties;
    public char[][] property_name = new char[10][20];
    public float[][] vox_to_ras = new float[4][4];
    public char[] reserved = new char[444];
    public char[] voxel_order = new char[4];
    public char[] pad2 = new char[4];
    public float[] image_orientation_patient = new float[6];
    public char[] pad1 = new char[2];
    public char invert_x;
    public char invert_y;
    public char invert_z;
    // (mistakenly?) called invert_x in the range
    public char swap_xy;
    public char swap_yz;
    public char swap_zx;

    public int n_count;
    public int version;
    public int hdr_size;

    public DtkHeader()
    {
        this.id_string[0] = 'T';
        this.id_string[1] = 'R';
        this.id_string[2] = 'A';
        this.id_string[3] = 'C';
        this.id_string[4] = 'K';
        this.id_string[5] = 0;

        this.dim[0] = 128;
        this.dim[1] = 128;
        this.dim[2] = 70;

        this.voxel_size[0] = 2.0f;
        this.voxel_size[1] = 2.0f;
        this.voxel_size[2] = 2.0f;

        this.vox_to_ras[0][0] = 1.0f;
        this.vox_to_ras[1][1] = 1.0f;
        this.vox_to_ras[2][2] = 1.0f;
        this.vox_to_ras[3][3] = 1.0f;

        this.voxel_order[0] = 'R';
        this.voxel_order[1] = 'A';
        this.voxel_order[2] = 'S';

        this.image_orientation_patient[0] = 1.0f;
        this.image_orientation_patient[5] = 1.0f;

        this.version = 2;
        this.hdr_size = 1000;
    }

    public String toString()
    {
        StringBuffer b = new StringBuffer();

        b.append("id_string: ");
        b.append(String.valueOf(this.id_string));
        b.append("\n");

        b.append("dim:");
        for (int element : this.dim)
        {
            b.append(" ");
            b.append(element);
        }
        b.append("\n");

        b.append("voxel_size:");
        for (float element : this.voxel_size)
        {
            b.append(" ");
            b.append(element);
        }
        b.append("\n");

        b.append("origin:");
        for (float element : this.origin)
        {
            b.append(" ");
            b.append(element);
        }
        b.append("\n");

        b.append("n_scalars:");
        b.append(this.n_scalars);
        b.append("\n");

        for (int i = 0; i < this.n_scalars; i++)
        {
            b.append("scalar_name[");
            b.append(i);
            b.append("]:");
            for (int j = 0; j < this.scalar_name[i].length; j++)
            {
                b.append(" ");
                b.append(this.scalar_name[i][j]);
            }
            b.append("\n");
        }

        b.append("n_properties:");
        b.append(this.n_properties);
        b.append("\n");

        for (int i = 0; i < this.n_properties; i++)
        {
            b.append("property_name[");
            b.append(i);
            b.append("]:");
            for (int j = 0; j < this.property_name[i].length; j++)
            {
                b.append(" ");
                b.append(this.property_name[i][j]);
            }
            b.append("\n");
        }

        b.append("vox_to_ras:");
        for (float[] vox_to_ra : this.vox_to_ras)
        {
            for (int j = 0; j < vox_to_ra.length; j++)
            {
                b.append(" ");
                b.append(vox_to_ra[j]);
            }
        }
        b.append("\n");

        b.append("reserved:");
        b.append(String.valueOf(this.reserved));
        b.append("\n");

        b.append("voxel_order:");
        b.append(String.valueOf(this.voxel_order));
        b.append("\n");

        b.append("pad2:");
        b.append(String.valueOf(this.pad2));
        b.append("\n");

        b.append("image_orientation_patient:");
        for (float element : this.image_orientation_patient)
        {
            b.append(" ");
            b.append(element);
        }
        b.append("\n");

        b.append("pad1:");
        b.append(String.valueOf(this.pad1));
        b.append("\n");

        b.append("invert_x:");
        b.append(this.invert_x);
        b.append("\n");

        b.append("invert_y:");
        b.append(this.invert_y);
        b.append("\n");

        b.append("invert_z:");
        b.append(this.invert_z);
        b.append("\n");

        b.append("swap_xy:");
        b.append(this.swap_xy);
        b.append("\n");

        b.append("swap_yz:");
        b.append(this.swap_yz);
        b.append("\n");

        b.append("swap_zx:");
        b.append(this.swap_zx);
        b.append("\n");

        b.append("n_count:");
        b.append(this.n_count);
        b.append("\n");

        b.append("version:");
        b.append(this.version);
        b.append("\n");

        b.append("hdr_size:");
        b.append(this.hdr_size);
        b.append("\n");

        return b.toString();
    }
    
    public static DtkHeader read(DataInput dis) throws IOException
    {
        DtkHeader h = new DtkHeader();
        for (int i = 0; i < h.id_string.length; i++)
        {
            h.id_string[i] = (char) dis.readUnsignedByte();
        }

        for (int i = 0; i < h.dim.length; i++)
        {
            h.dim[i] = dis.readShort();
        }

        for (int i = 0; i < h.voxel_size.length; i++)
        {
            h.voxel_size[i] = dis.readFloat();
        }

        for (int i = 0; i < h.origin.length; i++)
        {
            h.origin[i] = dis.readFloat();
        }

        h.n_scalars = dis.readShort();

        for (int i = 0; i < h.scalar_name.length; i++)
        {
            for (int j = 0; j < h.scalar_name[i].length; j++)
            {
                h.scalar_name[i][j] = (char) dis.readUnsignedByte();
            }
        }

        h.n_properties = dis.readShort();

        for (int i = 0; i < h.property_name.length; i++)
        {
            for (int j = 0; j < h.property_name[i].length; j++)
            {
                h.property_name[i][j] = (char) dis.readUnsignedByte();
            }
        }

        for (int i = 0; i < h.vox_to_ras.length; i++)
        {
            for (int j = 0; j < h.vox_to_ras[i].length; j++)
            {
                h.vox_to_ras[i][j] = dis.readFloat();
            }
        }

        for (int i = 0; i < h.reserved.length; i++)
        {
            h.reserved[i] = (char) dis.readUnsignedByte();
        }

        for (int i = 0; i < h.voxel_order.length; i++)
        {
            h.voxel_order[i] = (char) dis.readUnsignedByte();
        }

        for (int i = 0; i < h.pad2.length; i++)
        {
            h.pad2[i] = (char) dis.readUnsignedByte();
        }

        for (int i = 0; i < h.image_orientation_patient.length; i++)
        {
            h.image_orientation_patient[i] = dis.readFloat();
        }

        for (int i = 0; i < h.pad1.length; i++)
        {
            h.pad1[i] = (char) dis.readUnsignedByte();
        }

        // mistakenly called invert_x in the range
        h.invert_x = (char) dis.readUnsignedByte();
        h.invert_y = (char) dis.readUnsignedByte();
        h.invert_z = (char) dis.readUnsignedByte();
        h.swap_xy = (char) dis.readUnsignedByte();
        h.swap_yz = (char) dis.readUnsignedByte();
        h.swap_zx = (char) dis.readUnsignedByte();

        h.n_count = dis.readInt();
        h.version = dis.readInt();
        h.hdr_size = dis.readInt();

        if (h.hdr_size != 1000)
        {
            throw new RuntimeException("Failed to read DTK file.  Header size doesn't match");
        }

        return h;
    }

    public static void write(DataOutput dos, DtkHeader h) throws IOException
    {
        for (char c : h.id_string)
        {
            dos.writeByte(c);
        }

        for (int dim : h.dim)
        {
            dos.writeShort(dim);
        }

        for (float voxel_size : h.voxel_size)
        {
            dos.writeFloat(voxel_size);
        }

        for (float origin : h.origin)
        {
            dos.writeFloat(origin);
        }

        dos.writeShort(h.n_scalars);

        for (char[] name : h.scalar_name)
        {
            for (char c : name)
            {
                dos.writeByte(c);
            }
        }

        dos.writeShort(h.n_properties);

        for (char[] name : h.property_name)
        {
            for (char c : name)
            {
                dos.writeByte(c);
            }
        }

        for (float[] row : h.vox_to_ras)
        {
            for (float v : row)
            {
                dos.writeFloat(v);
            }
        }

        for (char c : h.reserved)
        {
            dos.writeByte(c);
        }

        for (char c : h.voxel_order)
        {
            dos.writeByte(c);
        }

        for (char c : h.pad2)
        {
            dos.writeByte(c);
        }

        for (float v : h.image_orientation_patient)
        {
            dos.writeFloat(v);
        }

        for (char c : h.pad1)
        {
            dos.writeByte(c);
        }

        dos.writeByte(h.invert_x);
        dos.writeByte(h.invert_y);
        dos.writeByte(h.invert_z);

        dos.writeByte(h.swap_xy);
        dos.writeByte(h.swap_yz);
        dos.writeByte(h.swap_zx);

        dos.writeInt(h.n_count);
        dos.writeInt(h.version);
        dos.writeInt(h.hdr_size);
    }

    public Affine xfm()
    {
        float[][] array = this.vox_to_ras;
        Matrix rot = new Matrix(3, 3);
        Vect trans = new Vect(3);

        rot.set(0, 0, array[0][0]);
        rot.set(1, 0, array[0][1]);
        rot.set(2, 0, array[0][2]);
        rot.set(0, 1, array[1][0]);
        rot.set(1, 1, array[1][1]);
        rot.set(2, 1, array[1][2]);
        rot.set(0, 2, array[2][0]);
        rot.set(1, 2, array[2][1]);
        rot.set(2, 2, array[2][2]);

        trans.set(0, array[0][3]);
        trans.set(1, array[1][3]);
        trans.set(2, array[2][3]);

        return new Affine(rot, trans);
    }
}
