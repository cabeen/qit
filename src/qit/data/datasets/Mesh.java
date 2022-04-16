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

package qit.data.datasets;

import com.google.common.collect.Lists;
import qit.base.Dataset;
import qit.base.Global;
import qit.base.Logging;
import qit.base.cli.CliUtils;
import qit.data.formats.mesh.*;
import qit.math.structs.Face;
import qit.math.structs.HalfEdgePoly;
import qit.math.structs.Vertex;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** a manifold triangle mesh with named per-vertex attributes */
public class Mesh implements Dataset
{
    public final static String COORD = "coord";
    public final static String NORMAL = "normal";
    public final static String COLOR = "color";
    public final static String OPACITY = "opacity";
    public final static String TEXTURE = "uv";
    public final static String TEMP = "temporary";

    public final static String LABEL = "label";
    public final static String MASK = "mask";
    public final static String INDEX = "index";
    public final static String PRIOR = "prior";
    public final static String SEGMENT = "segment";
    public final static String WHOLE = "whole";
    public final static String SELECTION = "selection";
    public final static String VALUE = "value";
    public final static String DISTANCE = "distance";
    public final static String SPHERE = "sphere";
    public final static String SMOOTH = "smooth";
    public final static String AREA = "area";
    public final static String AREA_INNER = "area_inner";
    public final static String AREA_OUTER = "area_outer";
    public final static String VOLUME = "volume";

    public final static String MIN_PRIN_CURV = "min_curv";
    public final static String MAX_PRIN_CURV = "max_curv";
    public final static String MIN_PRIN_DIR = "min_dir";
    public final static String MAX_PRIN_DIR = "max_dir";
    public final static String MEAN_CURV = "mean_curv";
    public final static String GAUSS_CURV = "gauss_curv";
    public final static String SHAPE_INDEX = "shape_index";
    public final static String CURVEDNESS = "curvedness";
    public final static String CURVED_INDEX = "curved_index";
    public final static String FEVAL = "feval";

    public AttrMap<Vertex> vattr;
    public HalfEdgePoly graph;

    public Mesh()
    {
        this(new AttrMap<>(), new HalfEdgePoly());
    }

    public Mesh(Mesh mesh)
    {
        this(mesh.vattr, mesh.graph);
    }

    public Mesh(AttrMap<Vertex> attr, HalfEdgePoly graph)
    {
        this.vattr = attr;
        this.graph = graph;
    }

    public Mesh copy()
    {
        return new Mesh(this.vattr.copy(), this.graph.copy());
    }

    public void add(Mesh mesh)
    {
        Map<Vertex, Vertex> lut = new HashMap<>();

        for (Vertex v : mesh.graph.verts())
        {
            Vertex nv = this.graph.addVertex();
            lut.put(v, nv);
            this.vattr.add(nv);
            for (String name : this.vattr.attrs())
            {
                if (mesh.vattr.has(name))
                {
                    this.vattr.set(nv, name, mesh.vattr.get(v, name));
                }
            }
        }

        for (Face f : mesh.graph.faces())
        {
            Vertex va = lut.get(f.getA());
            Vertex vb = lut.get(f.getB());
            Vertex vc = lut.get(f.getC());

            this.graph.add(new Face(va, vb, vc));
        }
    }

    public static Mesh read(String fn) throws IOException
    {
        CliUtils.validate(fn);
        InputStream is = new FileInputStream(fn);

        for (String f : new String[]{"pial", "white", "sphere", "inflated", "smoothwm", "sphere.reg"})
        {
            if (fn.endsWith(f))
            {
                Mesh mesh = FreesurferMeshCoder.read(is);
                is.close();
                return mesh;
            }
        }

        if (fn.endsWith("off"))
        {
            return OffMeshCoder.read(is);
        }
        else if (fn.endsWith("vtk.gz"))
        {
            return VtkMeshCoder.read(new GZIPInputStream(is));
        }
        else if (fn.endsWith("vtk"))
        {
            return VtkMeshCoder.read(is);
        }
        else if (fn.endsWith("obj.gz"))
        {
            return MincObjMeshCoder.read(new GZIPInputStream(is));
        }
        else if (fn.endsWith("obj"))
        {
            return MincObjMeshCoder.read(is);
        }
        else if (fn.endsWith("stl"))
        {
            return StlMeshCoder.read(is);
        }
        else if (fn.endsWith("ply"))
        {
            return PlyMeshCoder.read(is);
        }
        else
        {
            Logging.info("unrecognized mesh format, assuming VTK");
            return VtkMeshCoder.read(is);
        }
    }

    public void write(String fn) throws IOException
    {
        OutputStream os = new FileOutputStream(fn);

        if (fn.endsWith("off"))
        {
            OffMeshCoder.write(this, os);
        }
        else if (fn.endsWith("stl"))
        {
            StlMeshCoder.write(this, os);
        }
        else if (fn.endsWith("vrml") || fn.endsWith("wrl"))
        {
            VrmlMeshCoder.write(this, os);
        }
        else if (fn.endsWith("ply"))
        {
            PlyMeshCoder.write(this, os);
        }
        else if (fn.endsWith("csv") && fn.contains("particle"))
        {
            ParticleMeshCoder.write(this, os);
        }
        else if (fn.endsWith("vtk.gz"))
        {
            VtkMeshCoder.write(this, new GZIPOutputStream(os));
        }
        else if (fn.endsWith("obj"))
        {
            if (Global.MINC || fn.contains("minc"))
            {
                MincObjMeshCoder.write(this, os);
            }
            else
            {
                LightwaveObjMeshCoder.write(this, os);
            }
        }
        else
        {
            VtkMeshCoder.write(this, os);
        }
    }

    public List<String> getExtensions()
    {
        List<String> out = Lists.newArrayList();
        out.add("vtk.gz");
        out.add("vtk");
        out.add("off");
        out.add("stl");
        out.add("obj");
        out.add("ply");
        out.add("txt");
        return out;
    }

    public String getDefaultExtension()
    {
        return "vtk";
    }
}
