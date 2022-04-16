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


package qit.data.modules.neuron;

import qit.base.Module;
import qit.base.annot.*;
import qit.base.structs.TriConsumer;
import qit.base.structs.Triple;
import qit.data.datasets.*;
import qit.data.modules.curves.CurvesTubes;
import qit.data.source.MeshSource;
import qit.data.source.VectSource;
import qit.data.utils.CurvesUtils;
import qit.math.structs.Vertex;

import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Function;

@ModuleDescription("Create a mesh from a neuron file")
@ModuleAuthor("Ryan Cabeen")
public class NeuronMesh implements Module
{
    private enum TrunkColorMode
    {
        Solid, DEC, Segment, Tree
    }

    private enum SolidColor
    {
        White(Color.WHITE),
        Cyan(Color.CYAN),
        Yellow(Color.YELLOW),
        Blue(Color.BLUE),
        Magenta(Color.MAGENTA),
        Orange(Color.ORANGE),
        Green(Color.GREEN),
        Pink(Color.PINK),
        GRAY(Color.GRAY),
        DarkGray(Color.DARK_GRAY),
        LightGray(Color.LIGHT_GRAY);

        Color color;

        SolidColor(Color c)
        {
            this.color = c;
        }
    }

    @ModuleInput
    @ModuleDescription("the input filename to the SWC neuron file")
    public Neuron input;

    @ModuleParameter
    @ModuleDescription("exclude sphere meshes for roots")
    public boolean noRootMesh;

    @ModuleParameter
    @ModuleDescription("exclude sphere meshes for forks")
    public boolean noForkMesh;

    @ModuleParameter
    @ModuleDescription("exclude sphere meshes for leaves")
    public boolean noLeafMesh;

    @ModuleParameter
    @ModuleDescription("exclude tube meshes for trunks")
    public boolean noTrunkMesh;

    @ModuleParameter
    @ModuleDescription("use constant radius for spheres (the default will modulate the thickness by the neuron radius)")
    public boolean noSphereSculpt;

    @ModuleParameter
    @ModuleDescription("use constant thickness tubes (the default will modulate the thickness by the neuron radius)")
    public boolean noTrunkSculpt;

    @ModuleParameter
    @ModuleDescription("the coloring mode for trunks")
    public TrunkColorMode trunkColorMode = TrunkColorMode.Solid;

    @ModuleParameter
    @ModuleDescription("the size of root spheres")
    public double rootScale = 1.0;

    @ModuleParameter
    @ModuleDescription("the size of fork spheres")
    public double forkScale = 1.0;

    @ModuleParameter
    @ModuleDescription("the size of leaf spheres")
    public double leafScale = 1.0;

    @ModuleParameter
    @ModuleDescription("the size of trunk tubes")
    public double trunkScale = 1.0;

    @ModuleParameter
    @ModuleDescription("the color of root spheres")
    public SolidColor rootColor = SolidColor.Orange;

    @ModuleParameter
    @ModuleDescription("the color of fork spheres")
    public SolidColor forkColor = SolidColor.Cyan;

    @ModuleParameter
    @ModuleDescription("the color of leaf spheres")
    public SolidColor leafColor = SolidColor.Yellow;

    @ModuleParameter
    @ModuleDescription("the color of trunk tubes (if the coloring mode is solid)")
    public SolidColor trunkColor = SolidColor.White;

    @ModuleParameter
    @ModuleDescription("remove all colors (and invalidates any other color options)")
    public boolean noColor;

    @ModuleParameter
    @ModuleDescription("the resolution of the spheres i.e. the number of subdivisions, so be careful with values above 3")
    public int sphereResolution = 0;

    @ModuleParameter
    @ModuleDescription("the resolution of the tubes, i.e. the number of radial spokes")
    public int tubeResolution = 5;

    @ModuleOutput
    @ModuleDescription("output mesh")
    public Mesh output;

    @Override
    public NeuronMesh run()
    {
        Mesh mesh = new Mesh();
        mesh.vattr.add(Mesh.COORD, VectSource.create3D());
        mesh.vattr.add(Mesh.COLOR, VectSource.create3D());

        TriConsumer<Vect, Double, Color> addSphere = (vect, scale, color) ->
        {
            Mesh sphere = MeshSource.sphere(this.sphereResolution);
            sphere.vattr.add(Mesh.COLOR, VectSource.create3D());
            sphere.vattr.setAll(Mesh.COLOR, VectSource.create3(color));

            Vect center = vect.sub(0, 3);
            double radius = this.noSphereSculpt ? scale : scale * vect.get(3);

            for (Vertex v : sphere.vattr)
            {
                Vect p = sphere.vattr.get(v, Mesh.COORD);
                sphere.vattr.set(v, Mesh.COORD, p.times(radius).plus(center));
            }

            mesh.add(sphere);
        };

        if (!this.noRootMesh)
        {
            for (Vect root : this.input.toVectsRoots())
            {
                addSphere.accept(root, this.rootScale, this.rootColor.color);
            }
        }

        if (!this.noForkMesh)
        {
            for (Vect fork : this.input.toVectsForks())
            {
                addSphere.accept(fork, this.forkScale, this.forkColor.color);
            }
        }

        if (!this.noLeafMesh)
        {
            for (Vect leaf : this.input.toVectsLeaves())
            {
                addSphere.accept(leaf, this.leafScale, this.leafColor.color);
            }
        }

        if (!this.noTrunkMesh)
        {
            Function<Integer, Vect> colormap = (idx) ->
            {
                float hue = (float) (idx / (Math.PI * 2.0));
                float sat = 0.5f;
                float val = 1.0f;
                Color color = Color.getHSBColor(hue, sat, val);

                return VectSource.create3(color);
            };

            Curves trunks = this.input.toCurves();
            trunks.add(Curves.COLOR, VectSource.create3D());
            if (!trunks.has(Curves.TANGENT))
            {
                CurvesUtils.attrSetTangent(trunks);
            }

            switch (this.trunkColorMode)
            {
                case DEC:
                    for (Curves.Curve curve : trunks)
                    {
                        for (int i = 0; i < curve.size(); i++)
                        {
                            Vect t = curve.get(Curves.TANGENT, i);
                            curve.set(Curves.COLOR, i, t.abs());
                        }
                    }
                    break;

                case Segment:
                    for (Curves.Curve curve : trunks)
                    {
                        for (int i = 0; i < curve.size(); i++)
                        {
                            int idx = (int) curve.get(Neuron.ATTR_SEGMENT, i).get(0);
                            curve.set(Curves.COLOR, i, colormap.apply(idx));
                        }
                    }
                    break;

                case Tree:
                    for (Curves.Curve curve : trunks)
                    {
                        for (int i = 0; i < curve.size(); i++)
                        {
                            int idx = (int) curve.get(Neuron.ATTR_TREE, i).get(0);
                            curve.set(Curves.COLOR, i, colormap.apply(idx));
                        }
                    }
                    break;

                default:
                    Color color = this.trunkColor.color;
                    double r = color.getRed() / 255.0;
                    double g = color.getGreen() / 255.0;
                    double b = color.getBlue() / 255.0;
                    CurvesUtils.attrSetAll(trunks, Curves.COLOR, VectSource.create3D(r, g, b));
                    break;
            }

            CurvesTubes tuber = new CurvesTubes();
            tuber.dthick = this.trunkScale;
            tuber.smooth = false;
            tuber.thick = Neuron.ATTR_RADIUS;
            tuber.fthick = this.trunkScale;
            tuber.noThick = this.noTrunkSculpt;
            tuber.resolution = this.tubeResolution;

            for (Curves.Curve trunk : trunks)
            {
                Mesh tube = tuber.single(trunk);
                mesh.add(tube);
            }
        }

        if (this.noColor)
        {
            mesh.vattr.remove(Mesh.COLOR);
        }

        this.output = mesh;

        return this;
    }

    public static Mesh apply(Neuron data)
    {
        return new NeuronMesh()
        {{
            this.input = data;
        }}.run().output;
    }
}
