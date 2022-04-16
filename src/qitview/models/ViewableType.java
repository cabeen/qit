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

package qitview.models;

import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.List;

import qit.base.Dataset;
import qit.base.JsonDataset;
import qit.base.Logging;
import qit.base.structs.Pair;
import qit.base.utils.PathUtils;
import qitview.main.Viewer;
import qitview.views.AffineView;
import qitview.views.CurvesView;
import qitview.views.DatasetView;
import qitview.views.DeformationView;
import qitview.views.GradientsView;
import qitview.views.MaskView;
import qitview.views.MeshView;
import qitview.views.NeuronView;
import qitview.views.SolidsView;
import qitview.views.TableView;
import qitview.views.VectsView;
import qitview.views.VolumeView;

public enum ViewableType
{
    Solids("Solids", SolidsView.class, qit.data.datasets.Solids.class),
    Vects("Vects", VectsView.class, qit.data.datasets.Vects.class),
    Table("Table", TableView.class, qit.data.datasets.Table.class),
    Gradients("Gradients", GradientsView.class, qit.data.utils.mri.structs.Gradients.class),
    Neuron("Neuron", NeuronView.class, qit.data.datasets.Neuron.class),
    Curves("Curves", CurvesView.class, qit.data.datasets.Curves.class),
    Mesh("Mesh", MeshView.class, qit.data.datasets.Mesh.class),
    Affine("Affine", AffineView.class, qit.data.datasets.Affine.class),
    Deformation("Deform", DeformationView.class, qit.data.datasets.Deformation.class),
    Mask("Mask", MaskView.class, qit.data.datasets.Mask.class),
    Volume("Volume", VolumeView.class, qit.data.datasets.Volume.class),
    Dataset("Dataset", DatasetView.class, JsonDataset.class);

    private final String text;
    private final Class<? extends Viewable<?>> view;
    private final Class<? extends Dataset> data;

    public String getText()
    {
        return this.text;
    }

    public Class<? extends Viewable<?>> getViewType()
    {
        return this.view;
    }

    public Class<? extends Dataset> getDataType()
    {
        return this.data;
    }

    ViewableType(String n, Class<? extends Viewable<?>> t, Class<? extends Dataset> d)
    {
        this.text = n;
        this.view = t;
        this.data = d;
    }

    public static boolean hasName(String text)
    {
        for (ViewableType dv : ViewableType.values())
        {
            if (dv.getText().toLowerCase().equals(text.toLowerCase()))
            {
                return true;
            }
        }

        return false;
    }

    public static ViewableType getFromName(String text)
    {
        for (ViewableType dv : ViewableType.values())
        {
            if (dv.getText().toLowerCase().equals(text.toLowerCase()))
            {
                return dv;
            }
        }

        throw new RuntimeException("invalid text: " + text);
    }

    public static boolean hasViewType(Class<?> d)
    {
        for (ViewableType dv : ViewableType.values())
        {
            if (dv.view.equals(d))
            {
                return true;
            }
        }

        return false;
    }

    public static ViewableType getFromViewType(Class<?> d)
    {
        for (ViewableType dv : ViewableType.values())
        {
            if (d.equals(dv.view))
            {
                return dv;
            }
        }

        throw new RuntimeException("could not find type: " + d.getName());
    }

    public static ViewableType getFromView(Viewable<?> v)
    {
        for (ViewableType dv : ViewableType.values())
        {
            if (dv.view.isInstance(v))
            {
                return dv;
            }
        }

        throw new RuntimeException("could not find type");
    }

    public static boolean hasDataType(Class<?> d)
    {
        for (ViewableType dv : ViewableType.values())
        {
            if (dv.data.equals(d))
            {
                return true;
            }
        }

        return false;
    }

    public static ViewableType getFromDataType(Class<?> d)
    {
        for (ViewableType dv : ViewableType.values())
        {
            if (d.equals(dv.data))
            {
                return dv;
            }
        }

        throw new RuntimeException("could not find type: " + d.getName());
    }

    public static ViewableType getFromData(Dataset d)
    {
        for (ViewableType dv : ViewableType.values())
        {
            if (dv.data.isInstance(d))
            {
                return dv;
            }
        }

        throw new RuntimeException("could not find type");
    }

    public static Dataset read(ViewableType type, String fn) throws IOException
    {
        switch (type)
        {
            case Solids:
                return qit.data.datasets.Solids.read(fn);
            case Vects:
                return qit.data.datasets.Vects.read(fn);
            case Table:
                return qit.data.datasets.Table.read(fn);
            case Gradients:
                return qit.data.utils.mri.structs.Gradients.read(fn);
            case Neuron:
                return qit.data.datasets.Neuron.read(fn);
            case Curves:
                return qit.data.datasets.Curves.read(fn);
            case Mesh:
                return qit.data.datasets.Mesh.read(fn);
            case Affine:
                return qit.data.datasets.Affine.read(fn);
            case Volume:
                return qit.data.datasets.Volume.read(fn);
            case Mask:
                return qit.data.datasets.Mask.read(fn);
            default:
                throw new RuntimeException("invalid type: " + type);
        }
    }

    public static void write(Dataset data, String fn) throws IOException
    {
        String bfn = PathUtils.backup(fn);

        try
        {
            data.write(fn);
        }
        catch (RuntimeException e)
        {
            PathUtils.delete(fn);
            PathUtils.move(bfn, fn, false);

            throw e;
        }

        if (PathUtils.exists(bfn))
        {
            if (Viewer.getInstance().settings.backup)
            {
                Logging.info(String.format("backed up %s", bfn));
            }
            else
            {
                PathUtils.delete(bfn);
            }
        }
    }

    public static Viewable<? extends Dataset> create(ViewableType type)
    {
        switch (type)
        {
            case Solids:
                return new SolidsView();
            case Vects:
                return new VectsView();
            case Table:
                return new TableView();
            case Gradients:
                return new GradientsView();
            case Neuron:
                return new NeuronView();
            case Curves:
                return new CurvesView();
            case Mesh:
                return new MeshView();
            case Affine:
                return new AffineView();
            case Volume:
                return new VolumeView();
            case Mask:
                return new MaskView();
            case Dataset:
                return new DatasetView();
            default:
                throw new RuntimeException("invalid type: " + type);
        }
    }

    public static ViewableType guess(String fn)
    {
        String s = PathUtils.basename(fn).toLowerCase();

        if (s.contains("xfm") && s.endsWith("txt"))
        {
            return ViewableType.Affine;
        }

        if (s.contains("xfm") && (s.endsWith("nii.gz") || s.endsWith("nii")))
        {
            return ViewableType.Deformation;
        }

        if (s.contains("mesh"))
        {
            return ViewableType.Mesh;
        }

        if (s.contains("curves"))
        {
            return ViewableType.Curves;
        }

        if (s.contains("mask"))
        {
            return ViewableType.Mask;
        }

        if (s.contains("swc"))
        {
            return ViewableType.Neuron;
        }

        if (s.contains("roi"))
        {
            return ViewableType.Mask;
        }

        if (s.contains("bv"))
        {
            return ViewableType.Gradients;
        }

        List<Pair<String, ViewableType>> endings = Lists.newArrayList();
        endings.add(Pair.of("json", ViewableType.Solids));
        endings.add(Pair.of("csv", ViewableType.Table));
        endings.add(Pair.of("dti", ViewableType.Volume));
        endings.add(Pair.of("dki", ViewableType.Volume));
        endings.add(Pair.of("noddi", ViewableType.Volume));
        endings.add(Pair.of("xfib", ViewableType.Volume));
        endings.add(Pair.of("nii.gz", ViewableType.Volume));
        endings.add(Pair.of("nii", ViewableType.Volume));
        endings.add(Pair.of("nii", ViewableType.Volume));
        endings.add(Pair.of("obj", ViewableType.Mesh));
        endings.add(Pair.of("off", ViewableType.Mesh));
        endings.add(Pair.of("trk", ViewableType.Curves));
        endings.add(Pair.of("tck", ViewableType.Curves));
        endings.add(Pair.of("dtk", ViewableType.Curves));
        endings.add(Pair.of("vtk", ViewableType.Curves));
        endings.add(Pair.of("vtk.gz", ViewableType.Curves));

        for (Pair<String, ViewableType> ending : endings)
        {
            if (s.endsWith(ending.a))
            {
                return ending.b;
            }
        }

        return ViewableType.Volume;
    }
}
