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


package qitview.views;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.jogamp.opengl.GL2;

import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SpinnerNumberModel;

import org.apache.commons.io.FilenameUtils;
import qit.base.Logging;
import qit.base.utils.PathUtils;
import qit.data.datasets.Mask;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.modules.curves.MeshExtract;
import qit.data.modules.mesh.*;
import qit.data.source.VectSource;
import qit.data.utils.MeshUtils;
import qit.data.utils.mesh.MeshFunction;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.math.source.VectFunctionSource;
import qit.math.structs.Box;
import qit.math.structs.Face;
import qit.math.structs.Line;
import qit.math.structs.VectFunction;
import qit.math.structs.Vertex;
import qit.math.utils.MathUtils;
import qit.math.utils.colormaps.ColormapDiscrete;
import qit.math.utils.colormaps.ColormapScalar;
import qit.math.utils.colormaps.ColormapSolid;
import qit.math.utils.colormaps.ColormapVector;
import qitview.main.Constants;
import qitview.main.Interface;
import qitview.main.Viewer;
import qitview.models.ViewableAction;
import qitview.models.ViewableActions;
import qitview.models.WorldMouse;
import qitview.panels.Viewables;
import qitview.render.RenderGeometry;
import qitview.widgets.BasicButton;
import qitview.widgets.BasicComboBox;
import qitview.widgets.BasicFormattedTextField;
import qitview.widgets.BasicLabel;
import qitview.widgets.BasicSpinner;
import qitview.widgets.ColormapState;
import qitview.widgets.ControlPanel;
import qitview.widgets.SwingUtils;

public class MeshView extends AbstractView<Mesh>
{
    private static final int MIN_WIDTH = 0;
    private static final int MAX_WIDTH = 25;
    private static final int STEP_WIDTH = 1;

    private enum Interaction
    {
        Query, Draw, Erase, Isolate, Hide, None
    }

    private static final String ATTR_DRAW = ".draw";
    private static final String ATTR_NONE = "none";

    private transient ReentrantLock lock = new ReentrantLock();

    private transient Integer list = null;
    private transient boolean update = false;

    private transient RenderGeometry render;
    private transient Set<Vertex> selected = Sets.newHashSet();

    private transient Set<String> toInclude = Sets.newLinkedHashSet();
    private transient boolean sort = false;

    private transient BasicComboBox<String> comboCoordinate = new BasicComboBox<>();
    private transient BasicComboBox<String> comboAttribute = new BasicComboBox<>();
    private transient BasicComboBox<String> comboColorType = new BasicComboBox<>();
    private transient BasicComboBox<ColormapSolid> comboColorSolid;
    private transient BasicComboBox<ColormapDiscrete> comboColorDiscrete;
    private transient BasicComboBox<ColormapScalar> comboColorScalar;
    private transient BasicComboBox<ColormapVector> comboColorVector;
    private transient BasicButton autoMinMax;
    private transient BasicButton editColorSolid;
    private transient BasicButton editColorDiscrete;
    private transient BasicButton editColorScalar;
    private transient BasicComboBox<ColormapDiscrete> comboColorDiscreteLabel;
    private transient BasicButton editColorDiscreteLabel;

    private float wash = 0.0f;
    private double stencilRadius = 100;
    private boolean stencilConnected = true;

    private String labelsName = Mesh.LABEL;
    private String labelsInclude = "";
    private String labelsExclude = "";

    private int selectDraw = 1;
    private boolean selectRender = true;
    private double selectLift = 0.1;
    private int selectWidthVertex = 10;
    private int selectWidthLine = 0;

    public MeshView()
    {
        super();
        this.render = new RenderGeometry(() -> this.update = true);
        this.observable.addObserver((a, b) -> this.update = true);
        super.initPanel();
    }

    public MeshView setData(Mesh d)
    {
        if (d != null)
        {
            this.lock.lock();

            try
            {
                this.bounds = MeshUtils.bounds(d, this.getCoord());
                Viewer.getInstance().gui.canvas.render3D.boxUpdate();

                if (!d.vattr.has(Mesh.NORMAL))
                {
                    Viewer.getInstance().control.setStatusMessage("recomputing normals");
                    MeshUtils.computeNormals(d);
                }

                if (!d.vattr.has(Mesh.COLOR))
                {
                    d.vattr.add(Mesh.COLOR, new Vect(4));
                    MeshUtils.setAll(d, Mesh.COLOR, VectSource.create4D(1, 1, 1, 1));
                }
            }
            finally
            {
                this.lock.unlock();
            }
        }

        super.setData(d);

        return this;
    }

    @Override
    public Mesh getData()
    {
        return this.data;
    }

    protected Map<String, ControlPanel> makeEditControls()
    {
        Map<String, ControlPanel> controls = Maps.newLinkedHashMap();

        {
            ControlPanel panel = new ControlPanel();
            {
                final JCheckBox elem = new JCheckBox();
                elem.setToolTipText("render labels for the specified attribute");
                elem.setSelected(this.selectRender);
                elem.addItemListener(e ->
                {
                    this.selectRender = e.getStateChange() == ItemEvent.SELECTED;
                    this.update = true;
                });
                panel.addControl("Show", elem);
            }
            {
                final BasicSpinner spin = new BasicSpinner(new SpinnerNumberModel(this.selectDraw, 0, 10000, 1));
                spin.setToolTipText("the vertex attribute value to use when drawing on the mesh");
                spin.addChangeListener(e ->
                {
                    this.selectDraw = Integer.valueOf(spin.getValue().toString());
                    this.update = true;
                });
                panel.addControl("Drawing Label", spin);
            }
            {
                final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
                elem.setToolTipText("specify the size of the spherical selection tool on the surface of the mesh");
                elem.setValue(new Double(this.stencilRadius));
                elem.addPropertyChangeListener("value", e ->
                {
                    double nprecision = ((Number) elem.getValue()).doubleValue();
                    if (nprecision != this.stencilRadius)
                    {
                        this.stencilRadius = nprecision;
                        this.update = true;
                        Logging.info("updated precision to " + this.stencilRadius);
                    }
                });
                panel.addControl("Radius", elem);
            }
            {
                final JCheckBox elem = new JCheckBox();
                elem.setToolTipText("force the vertex selection to be topologically connected (useful for selection ridges that are tighly packed)");
                elem.setSelected(this.stencilConnected);
                elem.addItemListener(e ->
                {
                    this.stencilConnected = e.getStateChange() == ItemEvent.SELECTED;
                    this.update = true;
                });
                panel.addControl("Connected", elem);
            }
            {
                SpinnerNumberModel model = new SpinnerNumberModel(this.selectWidthVertex, MIN_WIDTH, MAX_WIDTH, STEP_WIDTH);
                final BasicSpinner elem = new BasicSpinner(model);
                elem.setToolTipText("specify the point width of the label vertex rendering");
                elem.addChangeListener(e ->
                {
                    int value = Integer.valueOf(elem.getValue().toString());
                    if (value != this.selectWidthVertex)
                    {
                        this.selectWidthVertex = value;
                        this.update = true;
                        Logging.info("updated mesh labels vertices width to " + this.selectWidthVertex);
                    }
                });
                panel.addControl("Vertices width", elem);
            }
            {
                SpinnerNumberModel model = new SpinnerNumberModel(this.selectWidthLine, MIN_WIDTH, MAX_WIDTH, STEP_WIDTH);
                final BasicSpinner elem = new BasicSpinner(model);
                elem.setToolTipText("specify the point line of the label vertex rendering");
                elem.addChangeListener(e ->
                {
                    int value = Integer.valueOf(elem.getValue().toString());
                    if (value != this.selectWidthLine)
                    {
                        this.selectWidthLine = value;
                        this.update = true;
                        Logging.info("updated mesh labels vertices line to " + this.selectWidthLine);
                    }
                });
                panel.addControl("Vertices line", elem);
            }
            {
                this.editColorDiscreteLabel = new BasicButton("Edit Colormap");
                {
                    this.editColorDiscreteLabel.addActionListener(e ->
                    {
                        ColormapState cmap = Viewer.getInstance().colormaps;
                        cmap.showDiscrete();
                        cmap.setComboDiscrete((ColormapDiscrete) this.comboColorDiscreteLabel.getSelectedItem());
                    });
                }

                final ColormapState cms = Viewer.getInstance().colormaps;
                this.comboColorDiscreteLabel = cms.getComboDiscrete();
                this.comboColorDiscreteLabel.setSelectedIndex(0);
            }

            panel.addControl(this.comboColorDiscreteLabel);
            panel.addControl(this.editColorDiscreteLabel);

            Consumer<Runnable> runif = process ->
            {
                if (this.hasData())
                {
                    process.run();
                }
            };

            final BiConsumer<String, Mesh> replace = (name, curves) ->
            {
                this.setData(curves);
                this.updateColoring();
            };

            {
                BasicButton elem = new BasicButton("Clear");
                elem.setToolTipText("clear the vertex attribute for drawing");
                elem.addActionListener(e ->
                {
                    this.labelsInclude = "";
                    this.labelsExclude = "";
                    if (this.hasData() && this.data.vattr.has(ATTR_DRAW))
                    {
                        this.data.vattr.setAll(ATTR_DRAW, VectSource.create1D(0));
                    }
                    this.observable.changed();
                    this.update = true;
                });
                panel.addControl(elem);
            }
            {
                final BasicButton button = new BasicButton("Dilate Selection");
                button.setToolTipText("dilate the selected vertices");
                button.addActionListener(e -> runif.accept(Viewables.processorOld("mesh.draw.dilate", replace, () ->
                {
                    return new MeshAttrDilate()
                    {{
                        this.input = MeshView.this.getData();
                        this.attrin = MeshView.ATTR_DRAW;
                    }}.run().output;
                })));

                panel.addControl(button);
            }
            {
                final BasicButton button = new BasicButton("Erode Selection");
                button.setToolTipText("erode the selected vertices");
                button.addActionListener(e -> runif.accept(Viewables.processorOld("mesh.draw.erode", replace, () ->
                {
                    return new MeshAttrErode()
                    {{
                        this.input = MeshView.this.getData();
                        this.attrin = MeshView.ATTR_DRAW;
                    }}.run().output;
                })));

                panel.addControl(button);
            }
            {
                final BasicButton button = new BasicButton("Export Selection");
                button.setToolTipText("export the selected vertices to a new mesh");
                button.addActionListener(e -> runif.accept(Viewables.processorOld("mesh.draw.export", Viewables.consumeMesh(), () ->
                {
                    return new MeshCrop()
                    {{
                        this.input = MeshView.this.getData();
                        this.selection = MeshView.ATTR_DRAW;
                        this.invert = false;
                    }}.run().output;
                })));

                panel.addControl(button);
            }
            {
                final BasicButton button = new BasicButton("Crop Selection");
                button.setToolTipText("remove all but the selected vertices");
                button.addActionListener(e -> runif.accept(Viewables.processorOld("mesh.draw.crop", replace, () ->
                {
                    return new MeshCrop()
                    {{
                        this.input = MeshView.this.getData();
                        this.selection = MeshView.ATTR_DRAW;
                        this.invert = false;
                    }}.run().output;
                })));

                panel.addControl(button);
            }
            {
                final BasicButton button = new BasicButton("Delete Selection");
                button.setToolTipText("delete the selected vertices");
                button.addActionListener(e -> runif.accept(Viewables.processorOld("mesh.draw.delete", replace, () ->
                {
                    return new MeshCrop()
                    {{
                        this.input = MeshView.this.getData();
                        this.selection = MeshView.ATTR_DRAW;
                        this.invert = true;
                    }}.run().output;
                })));

                panel.addControl(button);
            }
            {
                final BasicButton button = new BasicButton("Export Components");
                button.setToolTipText("export the selected mesh components");
                button.addActionListener(e -> runif.accept(Viewables.processorOld("mesh.draw.export.comps", Viewables.consumeMesh(), () ->
                {
                    return new MeshComponents()
                    {{
                        this.input = MeshView.this.getData();
                        this.select = MeshView.ATTR_DRAW;
                    }}.run().output;
                })));

                panel.addControl(button);
            }
            {
                final BasicButton button = new BasicButton("Crop Components");
                button.setToolTipText("crop the selected mesh components");
                button.addActionListener(e -> runif.accept(Viewables.processorOld("mesh.draw.export.comps", replace, () ->
                {
                    return new MeshComponents()
                    {{
                        this.input = MeshView.this.getData();
                        this.select = MeshView.ATTR_DRAW;
                    }}.run().output;
                })));

                panel.addControl(button);
            }
            {
                final BasicButton button = new BasicButton("Delete Components");
                button.setToolTipText("delete the selected mesh components");
                button.addActionListener(e -> runif.accept(Viewables.processorOld("mesh.draw.export.comps", replace, () ->
                {
                    return new MeshComponents()
                    {{
                        this.input = MeshView.this.getData();
                        this.select = MeshView.ATTR_DRAW;
                        this.invert = true;
                    }}.run().output;
                })));

                panel.addControl(button);
            }
            {
                JButton elem = new JButton("Import from File");
                elem.setToolTipText("load a drawing buffer from a file");
                elem.addActionListener(e ->
                {
                    try
                    {
                        String fn = Viewer.getInstance().gui.chooseLoadFiles("Choose an attribute text file").get(0);
                        Logging.info("found attribute file: " + fn);
                        Vects attr = Vects.read(fn);

                        String guess = FilenameUtils.removeExtension(PathUtils.basename(fn));

                        int asize = attr.size();
                        int msize = this.hasData() ? MeshView.this.getData().vattr.size() : 0;
                        if (this.hasData() && asize == msize)
                        {
                            runif.accept(Viewables.processorOld("attribute", replace, () ->
                            {
                                return new MeshSetVects()
                                {{
                                    this.mesh = MeshView.this.getData();
                                    this.name = MeshView.ATTR_DRAW;
                                    this.vects = attr;
                                }}.run().output;
                            }));
                        }
                        else
                        {
                            SwingUtils.showMessage(String.format("Error: the number of attribute values (%d) does not match the number of mesh vertices (%d)", asize, msize));
                        }
                    }
                    catch (IOException e1)
                    {
                        e1.printStackTrace();
                        SwingUtils.showMessage("Error: failed to load attribute file (please see t");
                    }
                });
                panel.addControl(elem);
            }
            {
                JButton elem = new JButton("Export to File");
                elem.setToolTipText("save a drawing buffer to a file");
                elem.addActionListener(e ->
                {
                    String fn = Viewer.getInstance().gui.chooseSaveFile("Save drawing buffer", "Choose an output filename");

                    if (this.hasData())
                    {
                        runif.accept(Viewables.processorOld("save", replace, () ->
                        {
                            Vects vects = new MeshGetVects()
                            {{
                                this.mesh = MeshView.this.getData();
                                this.name = MeshView.ATTR_DRAW;
                            }}.run().output;

                            try
                            {
                                vects.write(fn);
                            }
                            catch (IOException ee)
                            {
                                SwingUtils.showMessage("Error: failed to save drawing buffer to file " + fn);
                            }

                            return MeshView.this.getData();
                        }));
                    }
                    else
                    {
                        SwingUtils.showMessage(String.format("Error: no mesh was found"));
                    }
                });
                panel.addControl(elem);
            }
            {
                final BasicButton button = new BasicButton("Import from Attribute");
                button.setToolTipText("copy and attribute to the selection buffer");
                button.addActionListener(e -> runif.accept(Viewables.processorOld("import drawing buffer", replace, () ->
                {
                    String name = SwingUtils.getStringSafe("Specify an attribute name", "attr");
                    return new MeshAttributes()
                    {{
                        this.input = MeshView.this.getData();
                        this.copy = String.format("%s=%s", MeshView.ATTR_DRAW, name);
                    }}.run().output;
                })));

                panel.addControl(button);
            }
            {
                final BasicButton button = new BasicButton("Export to Attribute");
                button.setToolTipText("copy the selection buffer to an attribute");
                button.addActionListener(e -> runif.accept(Viewables.processorOld("export drawing buffer", replace, () ->
                {
                    String name = SwingUtils.getStringSafe("Specify an attribute name", "attr");
                    return new MeshAttributes()
                    {{
                        this.input = MeshView.this.getData();
                        this.copy = String.format("%s=%s", name, MeshView.ATTR_DRAW);
                    }}.run().output;
                })));

                panel.addControl(button);
            }
            {
                final BasicButton button = new BasicButton("Export Vertices");
                button.setToolTipText("export the selection as vects");
                button.addActionListener(e -> runif.accept(Viewables.processorOld("mesh.select.vects", Viewables.consumeVects(), () ->
                {
                    return new MeshVertices()
                    {{
                        this.input = MeshView.this.getData();
                        this.query = MeshView.ATTR_DRAW;
                        this.nonzero = true;
                    }}.run().output;
                })));

                panel.addControl(button);
            }
            {
                final BasicButton button = new BasicButton("Export Mask");
                button.setToolTipText("export the selection as a mask (uses reference volume sampling)");
                button.addActionListener(e ->
                {
                    Interface inter = Viewer.getInstance().gui;
                    if (this.hasData() && inter.hasReferenceVolume())
                    {
                        Viewables.processorOld("mesh.select.mask", Viewables.consumeMask(), () ->
                        {
                            Mask ref = new Mask(inter.getReferenceVolume().getData().getSampling());

                            return new MeshVoxelize()
                            {{
                                this.input = MeshView.this.getData();
                                this.reference = ref;
                                this.attr = MeshView.ATTR_DRAW;
                            }}.run().output;
                        }).run();
                    }
                    else
                    {
                        SwingUtils.showMessage("Error: failed to run process");
                    }
                });

                panel.addControl(button);
            }

            controls.put("Drawing", panel);
        }

        {
            ControlPanel panel = new ControlPanel();

            {
                final BasicFormattedTextField elem = new BasicFormattedTextField();
                elem.setValue(this.labelsName);
                elem.setToolTipText("Specify the attribute (the vertex label used when drawing on the mesh)");
                elem.addPropertyChangeListener("value", e ->
                {
                    String nname = (String) elem.getValue();
                    if (nname != this.labelsName)
                    {
                        this.labelsName = nname;
                        this.render.setName(this.labelsName);
                        this.update = true;
                        Logging.info("updated name to " + this.labelsName);
                    }
                });
                panel.addControl("Select Attr", elem);
            }
            {
                final BasicFormattedTextField elem = new BasicFormattedTextField();
                elem.setValue(this.labelsInclude);
                elem.setToolTipText("Specify which labels to specifically include in the rendering, e.g. 0,2:10");
                elem.addPropertyChangeListener("value", e ->
                {
                    String nwhich = (String) elem.getValue();
                    if (nwhich != this.labelsInclude)
                    {
                        this.labelsInclude = nwhich;
                        this.render.setInclude(this.labelsInclude);
                        this.update = true;
                        Logging.info("updated which to labelsInclude:" + this.labelsInclude);
                    }
                });
                panel.addControl("Include Label", elem);
            }
            {
                final BasicFormattedTextField elem = new BasicFormattedTextField();
                elem.setValue(this.labelsExclude);
                elem.setToolTipText("Specify which labels to exclude from rendering, e.g. 0,2:10");
                elem.addPropertyChangeListener("value", e ->
                {
                    String nwhich = (String) elem.getValue();
                    if (nwhich != this.labelsExclude)
                    {
                        this.labelsExclude = nwhich;
                        this.render.setExclude(this.labelsExclude);
                        this.update = true;
                        Logging.info("updated which to labelsExclude: " + this.labelsExclude);
                    }
                });
                panel.addControl("Exclude Label", elem);
            }
            {
                final BasicButton button = new BasicButton("Retain Shown");
                button.setToolTipText("discard parts of the mesh that are not currently selected");
                button.addActionListener(e ->
                {
                    if (this.hasData())
                    {
                        Logging.info("retaining selection");
                        MeshExtract extract = new MeshExtract();
                        extract.input = this.data;
                        extract.include = this.labelsInclude;
                        extract.exclude = this.labelsExclude;
                        Mesh subset = extract.run().output;

                        this.setData(subset);
                    }
                });

                panel.addControl(button);
            }
            {
                final BasicButton button = new BasicButton("Export Shown");
                button.setToolTipText("create a new mesh data object from the current selection");
                button.addActionListener(e ->
                {
                    Logging.info("exporting selection");
                    MeshExtract extract = new MeshExtract();
                    extract.input = this.data;
                    extract.include = this.labelsInclude;
                    extract.exclude = this.labelsExclude;
                    Mesh subset = extract.run().output;

                    String name = SwingUtils.getString(Viewer.getInstance().gui.getFrame(), "Specify a name", "mesh.selection");
                    MeshView viewable = new MeshView();
                    viewable.setData(subset);
                    viewable.setName(name);
                    Viewer.getInstance().qviewables.offer(viewable);
                });

                panel.addControl(button);
            }

            controls.put("Selection", panel);
        }

        {
            ControlPanel panel = new ControlPanel();

            String modeExport = "Export";
            String modeReplace = "Replace";

            BasicComboBox modeComboBox = new BasicComboBox();
            modeComboBox.addItem(modeExport);
            modeComboBox.addItem(modeReplace);

            panel.addControl("Mode", modeComboBox);

            final Supplier<Boolean> replace = () -> modeComboBox.getSelectedItem().equals(modeReplace);

            for (ViewableAction action : ViewableActions.Mesh)
            {
                final BasicButton elem = new BasicButton(action.getName());
                elem.setToolTipText(action.getDescription());
                elem.addActionListener(e -> action.getAction(replace.get()).accept(this));
                panel.addControl(elem);
            }

            controls.put("Processing", panel);
        }

        return controls;
    }

    protected ControlPanel makeInfoControls()
    {
        final BasicLabel labelVertices = new BasicLabel("");
        final BasicLabel labelEdges = new BasicLabel("");
        final BasicLabel labelFaces = new BasicLabel("");
        final BasicLabel labelBoundary = new BasicLabel("");
        final BasicLabel labelGenus = new BasicLabel("");
        final BasicLabel labelArea = new BasicLabel("");
        final BasicLabel labelMinX = new BasicLabel("");
        final BasicLabel labelMaxX = new BasicLabel("");
        final BasicLabel labelMinY = new BasicLabel("");
        final BasicLabel labelMaxY = new BasicLabel("");
        final BasicLabel labelMinZ = new BasicLabel("");
        final BasicLabel labelMaxZ = new BasicLabel("");

        ControlPanel infoPanel = new ControlPanel();
        infoPanel.addControl("Vertices: ", labelVertices);
        infoPanel.addControl("Edges: ", labelEdges);
        infoPanel.addControl("Faces: ", labelFaces);
        infoPanel.addControl("Genus: ", labelGenus);
        infoPanel.addControl("Area: ", labelArea);
        infoPanel.addControl("Boundary: ", labelBoundary);
        infoPanel.addControl("Min X: ", labelMinX);
        infoPanel.addControl("Max X: ", labelMaxX);
        infoPanel.addControl("Min Y: ", labelMinY);
        infoPanel.addControl("Max Y: ", labelMaxY);
        infoPanel.addControl("Min Z: ", labelMinZ);
        infoPanel.addControl("Max Z: ", labelMaxZ);

        Runnable updateInfo = () ->
        {
            if (this.hasData() && this.hasBounds())
            {
                Mesh data = this.data;
                Box box = this.getBounds();
                DecimalFormat df = new DecimalFormat("0.00##");
                labelVertices.setText(df.format(data.graph.numVertex()));
                labelEdges.setText(df.format(data.graph.numEdge()));
                labelFaces.setText(df.format(data.graph.numFace()));
                labelGenus.setText(df.format(MeshUtils.genus(data)));
                labelArea.setText(df.format(MeshUtils.area(data)));
                labelBoundary.setText(df.format(data.graph.bound().size()));
                labelMinX.setText(df.format(box.getMin().getX()));
                labelMaxX.setText(df.format(box.getMax().getX()));
                labelMinY.setText(df.format(box.getMin().getY()));
                labelMaxY.setText(df.format(box.getMax().getY()));
                labelMinZ.setText(df.format(box.getMin().getZ()));
                labelMaxZ.setText(df.format(box.getMax().getZ()));
            }
            else
            {
                labelVertices.setText("NA");
                labelEdges.setText("NA");
                labelFaces.setText("NA");
                labelGenus.setText("NA");
                labelArea.setText("NA");
                labelBoundary.setText("NA");
                labelMinX.setText("NA");
                labelMaxX.setText("NA");
                labelMinY.setText("NA");
                labelMaxY.setText("NA");
                labelMinZ.setText("NA");
                labelMaxZ.setText("NA");
            }
        };

        this.observable.addObserver((a, b) -> updateInfo.run());

        {
            BasicButton elem = new BasicButton("Update Info");
            elem.setToolTipText("Update the info to reflect the most current data");
            elem.addActionListener(e -> updateInfo.run());

            infoPanel.addControl(elem);
        }

        return infoPanel;
    }

    protected Map<String, ControlPanel> makeRenderControls()
    {
        Map<String, ControlPanel> controls = Maps.newLinkedHashMap();

        {
            ControlPanel panel = new ControlPanel();
            {
                Viewer.getInstance().colormaps.getObservable().addObserver((a, b) -> this.updateColoring());
                final ActionListener listener = e -> this.updateColoring();

                this.autoMinMax = new BasicButton("Auto Min/Max");
                this.autoMinMax.addActionListener(e -> this.autoMinMax());

                this.editColorSolid = new BasicButton("Edit Solid Colormap");
                {
                    this.editColorSolid.addActionListener(e ->
                    {
                        ColormapState cmap = Viewer.getInstance().colormaps;
                        cmap.showSolid();
                        cmap.setComboSolid((ColormapSolid) MeshView.this.comboColorSolid.getSelectedItem());
                    });
                }

                this.editColorDiscrete = new BasicButton("Edit Discrete Colormap");
                {
                    this.editColorDiscrete.addActionListener(e ->
                    {
                        ColormapState cmap = Viewer.getInstance().colormaps;
                        cmap.showDiscrete();
                        cmap.setComboDiscrete((ColormapDiscrete) MeshView.this.comboColorDiscrete.getSelectedItem());
                    });
                }

                this.editColorScalar = new BasicButton("Edit Scalar Colormap");
                {
                    this.editColorScalar.addActionListener(e ->
                    {
                        ColormapState cmap = Viewer.getInstance().colormaps;
                        cmap.showScalar();
                        cmap.setComboScalar((ColormapScalar) MeshView.this.comboColorScalar.getSelectedItem());
                    });
                }

                this.observable.addObserver((a, b) ->
                {
                    BasicComboBox<String> combo = MeshView.this.comboAttribute;
                    combo.setPrototypeDisplayValue(ATTR_NONE);
                    Object selected = combo.getSelectedItem();
                    combo.removeActionListener(listener);
                    combo.removeAllItems();
                    combo.addItem(ATTR_NONE);
                    for (String attr : MeshView.this.data.vattr.attrs())
                    {
                        if (!Mesh.COLOR.equals(attr) && !Mesh.OPACITY.equals(attr))
                        {
                            combo.addItem(attr);
                        }
                    }
                    combo.setSelectedItem(selected == null ? ATTR_NONE : selected);
                    combo.addActionListener(listener);
                });

                this.observable.addObserver((a, b) ->
                {
                    BasicComboBox<String> combo = MeshView.this.comboCoordinate;
                    combo.setPrototypeDisplayValue(Mesh.COORD);
                    Object selected = combo.getSelectedItem();
                    combo.removeActionListener(listener);
                    combo.removeAllItems();
                    combo.addItem(Mesh.COORD);
                    for (String attr : MeshView.this.data.vattr.attrs())
                    {
                        if (!Mesh.COORD.equals(attr) && MeshView.this.data.vattr.dim(attr) == 3)
                        {
                            combo.addItem(attr);
                        }
                    }
                    combo.setSelectedItem(selected == null ? Mesh.COORD : selected);
                    combo.addActionListener(listener);
                    combo.addActionListener(e ->
                    {
                        // this is required to recompute normals
                        if (this.hasData())
                        {
                            Mesh mesh = this.getData();
                            String coord = this.getCoord();

                            Viewer.getInstance().control.setStatusMessage("updating mesh with " + coord);
                            this.bounds = MeshUtils.bounds(mesh, coord);
                            Viewer.getInstance().gui.canvas.render3D.boxUpdate();
                            MeshUtils.computeNormals(mesh, coord);
                            this.render.setCoord(coord);
                        }
                    });
                });

                this.comboColorType.addItem(ColormapState.SOLID);
                this.comboColorType.addItem(ColormapState.DISCRETE);
                this.comboColorType.addItem(ColormapState.SCALAR);
                this.comboColorType.addItem(ColormapState.VECTOR);

                this.comboColorType.setToolTipText("specify the type of colormap to use for the vertex coloring");
                this.comboAttribute.setToolTipText("specify which attribute to use for vertex coloring");

                final ColormapState cms = Viewer.getInstance().colormaps;
                this.comboColorSolid = cms.getComboSolid();
                this.comboColorDiscrete = cms.getComboDiscrete();
                this.comboColorScalar = cms.getComboScalar();
                this.comboColorVector = cms.getComboVector();

                this.comboAttribute.addActionListener(listener);
                this.comboColorType.addActionListener(listener);
                this.comboColorSolid.addActionListener(listener);
                this.comboColorDiscrete.addActionListener(listener);
                this.comboColorScalar.addActionListener(listener);
                this.comboColorVector.addActionListener(listener);

                this.comboColorSolid.setVisible(true);
                this.comboColorDiscrete.setVisible(false);
                this.comboColorScalar.setVisible(false);
                this.comboColorVector.setVisible(false);

                this.comboCoordinate.setSelectedItem(Mesh.COORD);
                this.comboAttribute.setSelectedItem(ATTR_NONE);
                this.comboColorType.setSelectedItem(ColormapState.SOLID);
                this.comboColorSolid.setSelectedIndex(0);
                this.comboColorDiscrete.setSelectedIndex(0);
                this.comboColorScalar.setSelectedIndex(0);
                this.comboColorVector.setSelectedIndex(0);

                this.updateColoring();

                this.comboColorType.addActionListener(e ->
                {
                    String selection = (String) this.comboColorType.getSelectedItem();

                    this.comboColorSolid.setVisible(false);
                    this.comboColorDiscrete.setVisible(false);
                    this.comboColorScalar.setVisible(false);
                    this.comboColorVector.setVisible(false);

                    switch (selection)
                    {
                        case ColormapState.SOLID:
                            this.comboColorSolid.setVisible(true);
                            this.editColorSolid.setVisible(true);
                            this.editColorDiscrete.setVisible(false);
                            this.editColorScalar.setVisible(false);
                            this.autoMinMax.setVisible(false);
                            break;
                        case ColormapState.DISCRETE:
                            this.comboColorDiscrete.setVisible(true);
                            this.editColorSolid.setVisible(false);
                            this.editColorDiscrete.setVisible(true);
                            this.editColorScalar.setVisible(false);
                            this.autoMinMax.setVisible(false);
                            break;
                        case ColormapState.SCALAR:
                            this.comboColorScalar.setVisible(true);
                            this.editColorSolid.setVisible(false);
                            this.editColorDiscrete.setVisible(false);
                            this.editColorScalar.setVisible(true);
                            this.autoMinMax.setVisible(true);
                            break;
                        case ColormapState.VECTOR:
                            this.comboColorVector.setVisible(true);
                            this.autoMinMax.setVisible(false);
                            this.editColorSolid.setVisible(false);
                            this.editColorDiscrete.setVisible(false);
                            this.editColorScalar.setVisible(false);
                            break;
                    }

                    Logging.info("updating colormap after changing type");
                    this.updateColoring();
                });

                final JPanel combos = new JPanel();
                combos.add(this.comboColorSolid);
                combos.add(this.comboColorDiscrete);
                combos.add(this.comboColorScalar);
                combos.add(this.comboColorVector);

                panel.addControl("Coord Attribute", this.comboCoordinate);
                panel.addControl("Color Attribute", this.comboAttribute);
                panel.addControl("Color Type", this.comboColorType);
                panel.addControl("Color Map", combos);
                panel.addControl(this.editColorSolid);
                panel.addControl(this.editColorDiscrete);
                panel.addControl(this.editColorScalar);
                panel.addControl(this.autoMinMax);
            }
            {
                final BasicButton button = new BasicButton("Sort Faces");
                button.setToolTipText("sort triangles to produce high quality transparency");
                button.addActionListener(e ->
                {
                    if (this.hasData())
                    {
                        this.sortFaces();
                    }
                });

                panel.addControl(button);
            }
            {
                final BasicFormattedTextField wfield = new BasicFormattedTextField(NumberFormat.getNumberInstance());
                wfield.setValue(new Double(this.wash));
                wfield.addPropertyChangeListener("value", e ->
                {
                    float nwash = ((Number) wfield.getValue()).floatValue();
                    if (nwash != this.wash)
                    {
                        this.wash = nwash;
                        this.update = true;
                        Logging.info("updated wash to " + this.wash);
                    }
                });
                panel.addControl("Wash", wfield);
            }

            controls.put("Mesh", panel);
        }

        controls.put("Render", this.render.getPanel());

        return controls;
    }

    public String getCoord()
    {
        String attr = (String) this.comboCoordinate.getSelectedItem();
        if (attr == null || !this.hasData() || !this.getData().vattr.has(attr))
        {
            return Mesh.COORD;
        }
        else
        {
            return attr;
        }
    }

    public void sortFaces()
    {
        Logging.info("sorting faces");
        this.sort = true;
        this.update = true;
    }

    public void dispose(GL2 gl)
    {
        if (this.list != null)
        {
            gl.glDeleteLists(this.list, 1);
            this.list = null;
            this.update = true;
        }
    }

    private void displayMesh(GL2 gl)
    {
        if (this.update && this.list != null)
        {
            gl.glDeleteLists(this.list, 1);
            this.list = null;
            this.update = false;
        }

        if (this.list == null)
        {
            int idx = gl.glGenLists(1);
            if (idx != 0)
            {
                this.list = idx;
                gl.glNewList(idx, GL2.GL_COMPILE);

                gl.glColor3f(1.0f, 1.0f, 1.0f);
                this.render.render(gl, this.data, this.sort);
                this.sort = false;

                gl.glEndList();
            }
        }

        if (this.list != null)
        {
            gl.glCallList(this.list);
        }

    }

    private void displayDraw(GL2 gl)
    {
        if (this.hasData() && this.selected.size() > 0)
        {
            try
            {
                // maybe this could be done with vertex coloring with textures instead...

                gl.glDisable(GL2.GL_LIGHTING);
                gl.glEnable(GL2.GL_POINT_SMOOTH);
                gl.glPointSize(this.selectWidthVertex);
                gl.glLineWidth(this.selectWidthLine);

                Vect color = VectSource.create3D(1, 0, 0);
                gl.glColor3d(color.getX(), color.getY(), color.getZ());

                for (Vertex vert : this.selected)
                {
                    Vect pos = this.data.vattr.get(vert, this.getCoord());
                    Vect norm = this.data.vattr.get(vert, Mesh.NORMAL);
                    Vect up = pos.plus(this.selectLift, norm);
                    Vect down = pos.plus(-this.selectLift, norm);

                    gl.glBegin(GL2.GL_POINTS);
                    gl.glColor3d(color.getX(), color.getY(), color.getZ());
                    gl.glVertex3d(up.getX(), up.getY(), up.getZ());
                    gl.glVertex3d(down.getX(), down.getY(), down.getZ());
                    gl.glEnd();

                    if (this.selectWidthLine > 0)
                    {
                        for (Face face : this.data.graph.faceRing(vert))
                        {
                            Vertex a = face.getA();
                            Vertex b = face.getB();
                            Vertex c = face.getC();
                            Vect pa = this.data.vattr.get(a, this.getCoord());
                            Vect pb = this.data.vattr.get(b, Mesh.COORD);
                            Vect pc = this.data.vattr.get(c, Mesh.COORD);
                            Vect na = this.data.vattr.get(a, Mesh.NORMAL);
                            Vect nb = this.data.vattr.get(b, Mesh.NORMAL);
                            Vect nc = this.data.vattr.get(c, Mesh.NORMAL);
                            boolean ia = this.selected.contains(a);
                            boolean ib = this.selected.contains(b);
                            boolean ic = this.selected.contains(c);

                            double lift = this.selectLift * 1.25;
                            for (double d : new double[]{lift, -lift})
                            {
                                Vect da = pa.plus(d, na);
                                Vect db = pb.plus(d, nb);
                                Vect dc = pc.plus(d, nc);

                                if (ia && ib)
                                {
                                    gl.glBegin(GL2.GL_LINE_STRIP);
                                    gl.glVertex3d(da.getX(), da.getY(), da.getZ());
                                    gl.glVertex3d(db.getX(), db.getY(), db.getZ());
                                    gl.glEnd();
                                }

                                if (ib && ic)
                                {
                                    gl.glBegin(GL2.GL_LINE_STRIP);
                                    gl.glVertex3d(db.getX(), db.getY(), db.getZ());
                                    gl.glVertex3d(dc.getX(), dc.getY(), dc.getZ());
                                    gl.glEnd();
                                }

                                if (ic && ia)
                                {
                                    gl.glBegin(GL2.GL_LINE_STRIP);
                                    gl.glVertex3d(dc.getX(), dc.getY(), dc.getZ());
                                    gl.glVertex3d(da.getX(), da.getY(), da.getZ());
                                    gl.glEnd();
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception e)
            {
                Logging.info("warning: failed to draw mesh selection");
            }
        }

        if (this.hasData() && this.selectRender && this.data.vattr.has(ATTR_DRAW))
        {
            try
            {
                // maybe this could be done with vertex coloring with textures instead...

                VectFunction coloring = ((ColormapDiscrete) this.comboColorDiscreteLabel.getSelectedItem()).getFunction();

                gl.glDisable(GL2.GL_LIGHTING);
                gl.glEnable(GL2.GL_POINT_SMOOTH);
                gl.glPointSize(this.selectWidthVertex);
                gl.glLineWidth(this.selectWidthLine);

                if (this.selectWidthVertex > 0)
                {
                    gl.glBegin(GL2.GL_POINTS);
                    for (Vertex vert : this.data.graph.verts())
                    {
                        int label = MathUtils.round(this.data.vattr.get(vert, ATTR_DRAW).get(0));

                        if (label != 0)
                        {
                            Vect color = coloring.apply(VectSource.create1D(label));
                            Vect pos = this.data.vattr.get(vert, this.getCoord());
                            Vect norm = this.data.vattr.get(vert, Mesh.NORMAL);
                            Vect up = pos.plus(this.selectLift, norm);
                            Vect down = pos.plus(-this.selectLift, norm);

                            gl.glColor3d(color.getX(), color.getY(), color.getZ());
                            gl.glVertex3d(up.getX(), up.getY(), up.getZ());
                            gl.glVertex3d(down.getX(), down.getY(), down.getZ());
                        }
                    }
                    gl.glEnd();
                }

                if (this.selectWidthLine > 0)
                {
                    for (Face face : this.data.graph.faces())
                    {
                        Vertex a = face.getA();
                        Vertex b = face.getB();
                        Vertex c = face.getC();
                        Vect pa = this.data.vattr.get(a, Mesh.COORD);
                        Vect pb = this.data.vattr.get(b, Mesh.COORD);
                        Vect pc = this.data.vattr.get(c, Mesh.COORD);
                        Vect na = this.data.vattr.get(a, Mesh.NORMAL);
                        Vect nb = this.data.vattr.get(b, Mesh.NORMAL);
                        Vect nc = this.data.vattr.get(c, Mesh.NORMAL);
                        int la = MathUtils.round(this.data.vattr.get(a, ATTR_DRAW).get(0));
                        int lb = MathUtils.round(this.data.vattr.get(b, ATTR_DRAW).get(0));
                        int lc = MathUtils.round(this.data.vattr.get(c, ATTR_DRAW).get(0));
                        Vect ca = coloring.apply(VectSource.create1D(la));
                        Vect cb = coloring.apply(VectSource.create1D(lb));
                        Vect cc = coloring.apply(VectSource.create1D(lc));
                        Vect pab = pa.times(0.5).plus(0.5, pb);
                        Vect pbc = pb.times(0.5).plus(0.5, pc);
                        Vect pca = pc.times(0.5).plus(0.5, pa);

                        for (double d : new double[]{this.selectLift, -this.selectLift})
                        {
                            Vect da = pa.plus(d, na);
                            Vect db = pb.plus(d, nb);
                            Vect dc = pc.plus(d, nc);
                            Vect dab = pab.plus(d, na);
                            Vect dbc = pbc.plus(d, nb);
                            Vect dca = pca.plus(d, nc);

                            if (la != 0 && lb != 0)
                            {
                                gl.glBegin(GL2.GL_LINE_STRIP);
                                gl.glColor3d(ca.getX(), ca.getY(), ca.getZ());
                                gl.glVertex3d(da.getX(), da.getY(), da.getZ());
                                gl.glVertex3d(dab.getX(), dab.getY(), dab.getZ());
                                gl.glEnd();

                                gl.glBegin(GL2.GL_LINE_STRIP);
                                gl.glColor3d(cb.getX(), cb.getY(), cb.getZ());
                                gl.glVertex3d(db.getX(), db.getY(), db.getZ());
                                gl.glVertex3d(dab.getX(), dab.getY(), dab.getZ());
                                gl.glEnd();
                            }

                            if (lb != 0 && lc != 0)
                            {
                                gl.glBegin(GL2.GL_LINE_STRIP);
                                gl.glColor3d(cb.getX(), cb.getY(), cb.getZ());
                                gl.glVertex3d(db.getX(), db.getY(), db.getZ());
                                gl.glVertex3d(dbc.getX(), dbc.getY(), dbc.getZ());
                                gl.glEnd();

                                gl.glBegin(GL2.GL_LINE_STRIP);
                                gl.glColor3d(cc.getX(), cc.getY(), cc.getZ());
                                gl.glVertex3d(dc.getX(), dc.getY(), dc.getZ());
                                gl.glVertex3d(dbc.getX(), dbc.getY(), dbc.getZ());
                                gl.glEnd();
                            }

                            if (lc != 0 && la != 0)
                            {
                                gl.glBegin(GL2.GL_LINE_STRIP);
                                gl.glColor3d(cc.getX(), cc.getY(), cc.getZ());
                                gl.glVertex3d(dc.getX(), dc.getY(), dc.getZ());
                                gl.glVertex3d(dca.getX(), dca.getY(), dca.getZ());
                                gl.glEnd();

                                gl.glBegin(GL2.GL_LINE_STRIP);
                                gl.glColor3d(ca.getX(), ca.getY(), ca.getZ());
                                gl.glVertex3d(da.getX(), da.getY(), da.getZ());
                                gl.glVertex3d(dca.getX(), dca.getY(), dca.getZ());
                                gl.glEnd();
                            }
                        }
                    }
                }
            }
            catch (Exception e)
            {
                Logging.info("warning: failed to draw mesh selection");
            }
        }
    }

    public void display(GL2 gl)
    {
        if (this.lock.tryLock())
        {
            try
            {
                this.displayMesh(gl);
                this.displayDraw(gl);
            }

            finally
            {
                this.lock.unlock();
            }
        }
    }

    private void autoMinMax()
    {
        this.lock.lock();

        try
        {
            if (MeshView.this.hasData())
            {
                Mesh mesh = this.getData();

                String attribute = (String) this.comboAttribute.getSelectedItem();
                String ctype = (String) this.comboColorType.getSelectedItem();

                if (ctype.equals(ColormapState.SCALAR) && mesh.vattr.has(attribute))
                {
                    double min = Double.MAX_VALUE;
                    double max = Double.MIN_VALUE;
                    for (Vertex vertex : mesh.vattr)
                    {
                        double value = mesh.vattr.get(vertex, attribute).get(0);
                        min = Math.min(min, value);
                        max = Math.max(max, value);
                    }

                    ColormapScalar cm = (ColormapScalar) this.comboColorScalar.getSelectedItem();
                    cm.withMin(min);
                    cm.withMax(max);
                    this.update = true;
                    Viewer.getInstance().colormaps.update();
                    this.updateColoring();
                }
            }
        }
        finally
        {
            this.lock.unlock();
        }
    }

    private void updateColoring()
    {
        if (this.data != null)
        {
            // look up coloring function
            String attribute = (String) this.comboAttribute.getSelectedItem();
            String ctype = (String) this.comboColorType.getSelectedItem();

            VectFunction coloring = null;
            switch (ctype)
            {
                case ColormapState.SOLID:
                    coloring = ((ColormapSolid) this.comboColorSolid.getSelectedItem()).getFunction();
                    break;
                case ColormapState.DISCRETE:
                    coloring = ((ColormapDiscrete) this.comboColorDiscrete.getSelectedItem()).getFunction();
                    break;
                case ColormapState.SCALAR:
                    coloring = ((ColormapScalar) this.comboColorScalar.getSelectedItem()).getFunction();
                    break;
                case ColormapState.VECTOR:
                    coloring = ((ColormapVector) this.comboColorVector.getSelectedItem()).getFunction();
                    break;
                default:
                    Logging.error("invalid colortype");
            }

            Logging.info("coloring mesh with attribute: " + attribute);

            boolean hasit = this.data.vattr.has(attribute);

            int dattr = hasit ? this.data.vattr.dim(attribute) : 1;
            int dcolor = coloring.getDimIn();
            if (attribute == null || dcolor != dattr)
            {
                coloring = VectFunctionSource.constant(dattr, VectSource.createND(dcolor, 1)).compose(coloring);
            }

            // apply wash
            if (MathUtils.nonzero(this.wash))
            {
                coloring = coloring.compose(VectFunctionSource.wash(this.wash));
            }

            // apply coloring
            new MeshFunction(coloring).withMesh(this.data).withSource(attribute).withDest(Mesh.COLOR).run();

            // signal a change
            this.update = true;
        }
    }

    public Double dist(WorldMouse mouse)
    {
        if (!this.hasData() || mouse.press == null)
        {
            return null;
        }

        double mindist = Double.MAX_VALUE;
        Line pressLine = Line.fromTwoPoints(mouse.press.point, mouse.press.hit);

        for (Vertex vert : this.data.vattr)
        {
            Vect pos = this.data.vattr.get(vert, this.getCoord());
            double distLine = pressLine.dist(pos);

            if (distLine < this.stencilRadius)
            {
                mindist = Math.min(mouse.press.point.dist(pos), mindist);
            }
        }

        return mindist;
    }

    private Interaction parse(WorldMouse mouse, String mode)
    {
        for (Interaction i : Interaction.values())
        {
            if (i.toString().equals(mode))
            {
                return i;
            }
        }

        if (Constants.INTERACTION_ROTATE.equals(mode))
        {
            if (mouse.shift && mouse.control)
            {
                return Interaction.Erase;
            }
            else if (!mouse.shift && mouse.control)
            {
                return Interaction.Draw;
            }
            else if (mouse.shift && !mouse.control)
            {
                return Interaction.Query;
            }
            else
            {
                return Interaction.None;
            }
        }

        return Interaction.None;
    }

    public List<String> modes()
    {
        List<String> out = Lists.newArrayList();
        out.add(Interaction.Query.toString());
        out.add(Interaction.Draw.toString());
        out.add(Interaction.Erase.toString());
        out.add(Interaction.Isolate.toString());
        out.add(Interaction.Hide.toString());
        return out;
    }

    public void handle(WorldMouse mouse, String mode)
    {
        if (this.lock.tryLock())
        {
            try
            {

                if (!this.hasData() || mouse.current == null || !mouse.pick)
                {
                    if (!mouse.pick && mouse.press == null && this.toInclude.size() > 0)
                    {
                        this.labelsInclude = this.toInclude.size() == 1 ? this.toInclude.iterator().next() : Joiner.on(",").join(this.toInclude);
                        this.observable.changed();
                        this.update = true;
                        this.toInclude.clear();
                    }

                    this.selected.clear();
                    return;
                }

                Interaction inter = parse(mouse, mode);

                Vect hit = mouse.current == null ? mouse.press.hit : mouse.current.hit;

                Map<Vertex, Map<String, Double>> found = Maps.newHashMap();
                Vertex closestVert = null;
                Double closestDist = null;

                for (Vertex vert : this.data.vattr)
                {
                    Vect p = this.data.vattr.get(vert, this.getCoord());
                    double d = p.dist2(hit);

                    if (d < this.stencilRadius)
                    {
                        found.put(vert, null);

                        if (closestDist == null || d < closestDist)
                        {
                            closestDist = d;
                            closestVert = vert;
                        }
                    }
                }

                if (this.stencilConnected)
                {
                    Stack<Vertex> search = new Stack();
                    search.add(closestVert);

                    Set<Vertex> connect = Sets.newHashSet();

                    try
                    {
                        while (search.size() > 0)
                        {
                            Vertex vert = search.pop();

                            for (Vertex ring : this.data.graph.vertRing(vert))
                            {
                                if (!connect.contains(ring) && found.containsKey(ring))
                                {
                                    search.add(ring);
                                }
                            }

                            connect.add(vert);
                        }

                        found.clear();
                        for (Vertex vert : connect)
                        {
                            found.put(vert, null);
                        }
                    }
                    catch (Exception e)
                    {
                        // skip it when it fails
                    }
                }

                for (Vertex vert : found.keySet())
                {
                    Map<String, Double> map = Maps.newHashMap();
                    for (String attr : this.data.vattr.attrs())
                    {
                        Vect v = this.data.vattr.get(vert, attr);
                        if (v.size() == 1)
                        {
                            map.put(attr, v.get(0));
                        }
                    }
                    found.put(vert, map);
                }

                this.selected.clear();
                this.selected.addAll(found.keySet());

                if (found.size() > 0 && inter == Interaction.Isolate)
                {
                    for (Vertex vert : found.keySet())
                    {
                        Map<String, Double> map = found.get(vert);

                        if (map.containsKey(ATTR_DRAW))
                        {
                            int label = (int) Math.round(map.get(ATTR_DRAW));
                            String slabel = String.valueOf(label);

                            this.toInclude.add(slabel);
                        }
                        else
                        {
                            Logging.info("warning: attribute not found on mesh: " + ATTR_DRAW);
                        }

                        break;
                    }
                }
                else if (found.size() > 0 && inter == Interaction.Hide)
                {
                    for (Vertex vert : found.keySet())
                    {
                        Map<String, Double> map = found.get(vert);

                        if (map.containsKey(ATTR_DRAW))
                        {
                            int label = (int) Math.round(map.get(ATTR_DRAW));
                            String slabel = String.valueOf(label);

                            List<String> tokens = Lists.newArrayList();

                            for (String token : this.labelsExclude.split(","))
                            {
                                if (token.length() > 0)
                                {
                                    tokens.add(token);
                                }
                            }

                            if (!tokens.contains(slabel))
                            {
                                tokens.add(slabel);
                            }

                            this.labelsExclude = tokens.size() == 1 ? tokens.get(0) : Joiner.on(",").join(tokens);
                            this.observable.changed();
                            this.update = true;
                        }
                        else
                        {
                            Logging.info("warning: attribute not found on mesh: " + ATTR_DRAW);
                        }

                        break;
                    }
                }
                else if (found.size() > 0 && inter == Interaction.Query)
                {
                    StringBuilder builder = new StringBuilder();
                    builder.append("\n");
                    builder.append("\n");

                    {
                        builder.append(String.format("Closest vertex (index %d):\n", closestVert.id()));
                        Map<String, Double> map = found.get(closestVert);
                        for (String name : map.keySet())
                        {
                            builder.append(String.format("  %s: %g\n", name, map.get(name)));
                        }
                        builder.append("\n");
                    }

                    Map<String, VectOnlineStats> stats = Maps.newHashMap();
                    for (Vertex vert : found.keySet())
                    {
                        Map<String, Double> map = found.get(vert);
                        for (String name : map.keySet())
                        {
                            if (!stats.containsKey(name))
                            {
                                stats.put(name, new VectOnlineStats());
                            }
                            stats.get(name).update(map.get(name));
                        }
                    }

                    {
                        builder.append("Selection Averages:\n");
                        builder.append(String.format("  num: %d\n", found.size()));
                        for (String name : stats.keySet())
                        {
                            builder.append(String.format("  %s: %g\n", name, stats.get(name).mean));
                        }
                    }

                    Viewer.getInstance().control.setStatusMessage(builder.toString());
                }
                else if (inter == Interaction.Draw || inter == Interaction.Erase)
                {
                    if (found.size() > 0 && this.hasData() && mouse.press != null)
                    {
                        int label = inter == Interaction.Erase ? 0 : this.selectDraw;

                        Mesh mesh = this.getData();
                        if (!mesh.vattr.has(ATTR_DRAW))
                        {
                            Logging.info("added mesh attribute: " + ATTR_DRAW);
                            mesh.vattr.add(ATTR_DRAW, VectSource.create1D());
                        }

                        for (Vertex vert : found.keySet())
                        {
                            mesh.vattr.set(vert, ATTR_DRAW, VectSource.create1D(label));
                        }
                    }
                }
            }
            finally
            {
                this.lock.unlock();
            }
        }
    }
}