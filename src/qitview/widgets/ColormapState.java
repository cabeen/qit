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


package qitview.widgets;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Observable;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import qit.base.JsonDataset;
import qit.base.Logging;
import qit.base.structs.ObservableInstance;
import qit.base.utils.JsonUtils;
import qit.base.utils.PathUtils;
import qit.data.source.VectSource;
import qit.math.source.VectFunctionSource;
import qit.math.utils.colormaps.ColormapDiscrete;
import qit.math.utils.colormaps.ColormapScalar;
import qit.math.utils.colormaps.ColormapSolid;
import qit.math.utils.colormaps.ColormapSource;
import qit.math.utils.colormaps.ColormapVector;
import qitview.main.Constants;
import qitview.main.Viewer;

public class ColormapState
{
    public static final String SOLID = "solid";
    public static final String DISCRETE = "discrete";
    public static final String SCALAR = "scalar";
    public static final String VECTOR = "vector";

    private DefaultListModel<ColormapSolid> modelSolid = new DefaultListModel<>();
    private DefaultListModel<ColormapDiscrete> modelDiscrete = new DefaultListModel<>();
    private DefaultListModel<ColormapScalar> modelScalar = new DefaultListModel<>();
    private DefaultListModel<ColormapVector> modelVector = new DefaultListModel<>();

    private BasicComboBox<ColormapSolid> comboSolid = new BasicComboBox<>(new SharedComboModel(this.modelSolid));
    private BasicComboBox<ColormapDiscrete> comboDiscrete = new BasicComboBox<>(new SharedComboModel(this.modelDiscrete));
    private BasicComboBox<ColormapScalar> comboScalar = new BasicComboBox<>(new SharedComboModel(this.modelScalar));

    private ColorWidget chooserSolid = new ColorWidget();
    private LookupWidget chooserDiscrete = new LookupWidget();
    private UnitMapWidget chooserScalarTransfer = new UnitMapWidget();
    private BasicComboBox<String> chooserScalarName = new BasicComboBox<>();

    private boolean immediate = false;
    private ObservableInstance observe = new ObservableInstance();

    private JDialog dialogSolid = null;
    private JDialog dialogDiscrete = null;
    private JDialog dialogScalar = null;

    public ColormapState()
    {
        this.modelSolid.addElement(new ColormapSolid().withName("white"));

        for (String name : ColormapSource.getSolidNames())
        {
            this.modelSolid.addElement(ColormapSource.getSolid(name));
        }

        for (String name : ColormapSource.getDiscreteNames())
        {
            this.modelDiscrete.addElement(ColormapSource.getDiscrete(name));
        }

        for (int i = 1; i <= 20; i++)
        {
            ColormapScalar cm = new ColormapScalar().withName("scalar" + i);
            cm.withColoring(ColormapSource.GRAYSCALE);
            this.modelScalar.addElement(cm);
        }

        this.modelVector.addElement(new ColormapVector().withName("rgb").withFunction(VectFunctionSource.rgb()));
        this.modelVector.addElement(new ColormapVector().withName("rgb255").withFunction(VectFunctionSource.rgb255()));
        this.modelVector.addElement(new ColormapVector().withName("rgba").withFunction(VectFunctionSource.rgba()));
        this.modelVector.addElement(new ColormapVector().withName("rgba255").withFunction(VectFunctionSource.rgba()));
        this.modelVector.addElement(new ColormapVector().withName("rgbdouble").withFunction(VectFunctionSource.rgbdouble()));
        this.modelVector.addElement(new ColormapVector().withName("rgbquad").withFunction(VectFunctionSource.rgbquad()));
        this.modelVector.addElement(new ColormapVector().withName("rgbsqrt").withFunction(VectFunctionSource.rgbsqrt()));
        this.modelVector.addElement(new ColormapVector().withName("rgbsq").withFunction(VectFunctionSource.rgbsq()));
        this.modelVector.addElement(new ColormapVector().withName("rgbnorm").withFunction(VectFunctionSource.rgbnorm()));

        for (String name : ColormapSource.getScalarNames())
        {
            this.chooserScalarName.addItem(name);
        }

        this.comboSolid.setSelectedIndex(0);
        this.comboDiscrete.setSelectedIndex(0);
        this.comboScalar.setSelectedIndex(0);

        this.connect();
        this.update();
    }

    public void setComboSolid(ColormapSolid v)
    {
        this.comboSolid.setSelectedItem(v);
    }

    public void setComboDiscrete(ColormapDiscrete v)
    {
        this.comboDiscrete.setSelectedItem(v);
    }

    public void setComboScalar(ColormapScalar v)
    {
        this.comboScalar.setSelectedItem(v);
    }

    public BasicComboBox<ColormapSolid> getComboSolid()
    {
        BasicComboBox<ColormapSolid> out = new BasicComboBox<>(new SharedComboModel(this.modelSolid));
        out.setToolTipText("specify a solid colormap for rendering");
        return out;
    }

    public BasicComboBox<ColormapDiscrete> getComboDiscrete()
    {
        BasicComboBox<ColormapDiscrete> out = new BasicComboBox<>(new SharedComboModel(this.modelDiscrete));
        out.setToolTipText("specify a discrete colormap for rendering");
        return out;
    }

    public BasicComboBox<ColormapScalar> getComboScalar()
    {
        BasicComboBox<ColormapScalar> out = new BasicComboBox<>(new SharedComboModel(this.modelScalar));
        out.setToolTipText("specify a scalar colormap for rendering");
        return out;
    }

    public BasicComboBox<ColormapVector> getComboVector()
    {
        BasicComboBox<ColormapVector> out = new BasicComboBox<>(new SharedComboModel(this.modelVector));
        out.setToolTipText("specify a vector colormap");
        return out;
    }

    public Observable getDiscreteObservable()
    {
        return this.observe;
    }

    public void update()
    {
        this.copySolidDataToView();
        this.copyDiscreteDataToView();
        this.copyScalarDataToView();

        this.observe.changed();
    }

    public Observable getObservable()
    {
        return this.observe;
    }

    private void connect()
    {
        this.chooserSolid.getObservable().addObserver((o, arg) ->
        {
            if (ColormapState.this.immediate)
            {
                ColormapState.this.copySolidViewToData();
            }
        });

        this.chooserDiscrete.getObservable().addObserver((o, arg) ->
        {
            if (ColormapState.this.immediate)
            {
                ColormapState.this.copyDiscreteViewToData();
            }
        });

        this.chooserScalarTransfer.addObserver((o, arg) ->
        {
            if (ColormapState.this.immediate)
            {
                ColormapState.this.copyScalarViewToData();
            }
        });

        this.comboSolid.addActionListener(e -> ColormapState.this.copySolidDataToView());

        this.comboDiscrete.addActionListener(e -> ColormapState.this.copyDiscreteDataToView());

        this.comboScalar.addActionListener(e -> ColormapState.this.copyScalarDataToView());

        this.chooserScalarName.addActionListener(e ->
        {
            if (ColormapState.this.immediate)
            {
                ColormapState.this.copyScalarViewToData();
            }

            String name = (String) chooserScalarName.getSelectedItem();
            ColormapState.this.chooserScalarTransfer.withColoring(ColormapSource.getScalarFunction(name));
        });
    }

    private void copySolidDataToView()
    {
        ColormapSolid cm = (ColormapSolid) ColormapState.this.comboSolid.getSelectedItem();
        if (cm != null)
        {
            Color color = ColormapSource.color(cm.getColor());
            ColormapState.this.chooserSolid.setColor(color);
        }
    }

    private void copySolidViewToData()
    {
        ColormapSolid cm = (ColormapSolid) ColormapState.this.comboSolid.getSelectedItem();
        if (cm != null)
        {
            Color color = ColormapState.this.chooserSolid.getColor();
            cm.withColor(ColormapSource.vect(color));
            this.observe.changed();
        }
    }

    private void copyDiscreteDataToView()
    {
        ColormapDiscrete cm = (ColormapDiscrete) ColormapState.this.comboDiscrete.getSelectedItem();
        if (cm != null)
        {
            ColormapState.this.chooserDiscrete.clear();
            for (Integer label : cm.getLabels())
            {
                Color color = ColormapSource.color(cm.getColor(label));
                ColormapState.this.chooserDiscrete.with(color, color.getAlpha() / 255.0, label);
            }
            ColormapState.this.chooserDiscrete.changed();
        }
    }

    private void copyDiscreteViewToData()
    {
        ColormapDiscrete cm = (ColormapDiscrete) ColormapState.this.comboDiscrete.getSelectedItem();
        if (cm != null)
        {
            cm.clear();
            for (LookupWidget.TableRow row : ColormapState.this.chooserDiscrete.getRows())
            {
                cm.withRelation(row.label, ColormapSource.vect(row.color, row.alpha));
            }
            this.observe.changed();
        }
    }

    private void copyScalarDataToView()
    {
        ColormapScalar cm = (ColormapScalar) ColormapState.this.comboScalar.getSelectedItem();
        if (cm != null)
        {
            ColormapState.this.chooserScalarName.setSelectedItem(cm.getColoring());
            ColormapState.this.chooserScalarTransfer.withMin(cm.getMin());
            ColormapState.this.chooserScalarTransfer.withMax(cm.getMax());
            ColormapState.this.chooserScalarTransfer.withTransfer(cm.getPoints());
            ColormapState.this.chooserScalarTransfer.withColoring(cm.getColoringFunction());
        }
    }

    private void copyScalarViewToData()
    {
        ColormapScalar cm = (ColormapScalar) ColormapState.this.comboScalar.getSelectedItem();
        if (cm != null)
        {
            cm.withColoring((String) chooserScalarName.getSelectedItem());
            cm.withMin(ColormapState.this.chooserScalarTransfer.getMin());
            cm.withMax(ColormapState.this.chooserScalarTransfer.getMax());
            cm.withTransfer(ColormapState.this.chooserScalarTransfer.getTransfer());
            this.observe.changed();
        }
    }

    public void showSolid()
    {
        // use lazy initialization, since this requires the interface to be ready
        if (this.dialogSolid == null)
        {
            this.makeSolidView();
        }

        this.dialogSolid.setVisible(true);
    }

    public void showDiscrete()
    {
        // use lazy initialization, since this requires the interface to be ready
        if (this.dialogDiscrete == null)
        {
            this.makeDiscreteView();
        }

        this.dialogDiscrete.setVisible(true);
    }


    public void showScalar()
    {
        // use lazy initialization, since this requires the interface to be ready
        if (this.dialogScalar == null)
        {
            this.makeScalarView();
        }

        this.dialogScalar.setVisible(true);
    }

    public void hideSolid()
    {
        // use lazy initialization, since this requires the interface to be ready
        if (this.dialogSolid == null)
        {
            this.makeSolidView();
        }

        this.dialogSolid.setVisible(false);
    }

    public void hideDiscrete()
    {
        // use lazy initialization, since this requires the interface to be ready
        if (this.dialogDiscrete == null)
        {
            this.makeDiscreteView();
        }

        this.dialogDiscrete.setVisible(false);
    }


    public void hideScalar()
    {
        // use lazy initialization, since this requires the interface to be ready
        if (this.dialogScalar == null)
        {
            this.makeScalarView();
        }

        this.dialogScalar.setVisible(false);
    }

    public void importColormap(String fn)
    {
        try
        {
            ColormapScalar cmap = ColormapScalar.read(fn);
            this.addColormap(cmap);
            Viewer.getInstance().control.setStatusMessage("imported colormap: " + fn);
        }
        catch (Exception e)
        {
            try
            {
                ColormapScalar[] cmaps = JsonDataset.read(ColormapScalar[].class, fn);
                for (ColormapScalar cmap : cmaps)
                {
                    this.addColormap(cmap);
                }
                Viewer.getInstance().control.setStatusMessage(String.format("imported %d colormaps from ", cmaps.length, fn));
            }
            catch (Exception ee)
            {
                Viewer.getInstance().control.setStatusMessage("warning: failed to import colormap!");
            }
        }
    }

    public void addColormap(ColormapScalar cmap)
    {
        String name = cmap.getName();

        Set<String> taken = Sets.newHashSet();
        for (int i = 0; i < ColormapState.this.comboScalar.getModel().getSize(); i++)
        {
            taken.add(ColormapState.this.comboScalar.getModel().getElementAt(i).getName());
        }

        if (taken.contains(name))
        {
            name = name + "-imported";

            String base = name;
            int idx = 1;
            while (taken.contains(name))
            {
                name = base + "-" + idx;
                idx += 1;
            }

            cmap.withName(name);
        }

        ColormapState.this.comboScalar.addItem(cmap);
        ColormapState.this.comboScalar.setSelectedItem(cmap);
        ColormapState.this.comboScalar.updateUI();
    }

    private void makeSolidView()
    {
        JFrame frame = Viewer.getInstance().gui.getFrame();

        ControlPanel controls = new ControlPanel();
        controls.addControl("Name:", "specify the solid colormap to edit", this.comboSolid);
        controls.addControl(this.chooserSolid.getPanel());

        JPanel wpanel = new JPanel();
        wpanel.setLayout(new BoxLayout(wpanel, BoxLayout.PAGE_AXIS));
        wpanel.add(controls);
        {

            JPanel subpanel = new JPanel();
            subpanel.setLayout(new BoxLayout(subpanel, BoxLayout.LINE_AXIS));
            subpanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

            {
                final JCheckBox elem = new JCheckBox("Immediate Mode");
                elem.setToolTipText("WHen checked, the colormap will be immediately rendered (the default is to wait for you to apply it, since immediate mode is slow for large datasets)");
                elem.setSelected(this.immediate);
                elem.addActionListener(e -> ColormapState.this.immediate = elem.isSelected());
                subpanel.add(elem);
            }
            subpanel.add(Box.createHorizontalGlue());
            {
                BasicButton elem = new BasicButton("Apply");
                elem.setToolTipText("Apply the colormap changes to the scene");
                elem.addActionListener(e -> ColormapState.this.copySolidViewToData());
                subpanel.add(elem);
            }
            {
                BasicButton elem = new BasicButton("Close");
                elem.setToolTipText("Apply the colormap changes to the scene and close this window");
                elem.addActionListener(e ->
                {
                    ColormapState.this.copySolidViewToData();
                    ColormapState.this.hideSolid();
                });
                subpanel.add(elem);
            }

            wpanel.add(subpanel);
        }

        this.dialogSolid = new JDialog(frame, Constants.SETTINGS_COLORMAPS_SOLID);
        this.dialogSolid.add(wpanel);
        this.dialogSolid.pack();
        this.dialogSolid.setResizable(false);
        this.dialogSolid.setLocationRelativeTo(frame);
        this.dialogSolid.setVisible(false);
        this.dialogSolid.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        SwingUtils.addEscapeListener(this.dialogSolid);
    }

    private void makeDiscreteView()
    {
        JFrame frame = Viewer.getInstance().gui.getFrame();

        JPanel wholePanel = new JPanel();
        wholePanel.setLayout(new VerticalLayout(5, VerticalLayout.LEFT));
        wholePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        {

            JPanel panel = new JPanel();
            panel.setLayout(new VerticalLayout(5, VerticalLayout.LEFT));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            {
                JPanel subpanel = new JPanel();
                subpanel.setLayout(new BoxLayout(subpanel, BoxLayout.LINE_AXIS));
                subpanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

                BasicLabel label = new BasicLabel("Name:");
                label.setToolTipText("specify the discrete colormap to edit");
                subpanel.add(label);
                subpanel.add(this.comboDiscrete);

                panel.add(subpanel);
            }

            panel.add(this.chooserDiscrete.getPanel());

            {
                JPanel subpanel = new JPanel();
                subpanel.setLayout(new BoxLayout(subpanel, BoxLayout.LINE_AXIS));

                subpanel.add(Box.createHorizontalGlue());
                {
                    BasicButton elem = new BasicButton("New");
                    elem.setToolTipText("Create a new colormap");
                    elem.addActionListener(new ActionListener()
                    {
                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            String name = SwingUtils.getString(Viewer.getInstance().gui.getFrame(), "Enter a name:", "my.discrete.colormap");
                            ColormapDiscrete cmap = new ColormapDiscrete();
                            cmap.withName(name);
                            cmap.withRelation(0, VectSource.create4D(0, 0, 0, 1));
                            cmap.withRelation(1, VectSource.create4D(1, 1, 1, 1));

                            ColormapState.this.comboDiscrete.addItem(cmap);
                            ColormapState.this.comboDiscrete.setSelectedItem(cmap);
                            ColormapState.this.comboDiscrete.updateUI();

                            Viewer.getInstance().control.setStatusMessage("created new colormap: " + name);
                        }
                    });

                    subpanel.add(elem);
                }
                subpanel.add(Box.createHorizontalGlue());
                {
                    BasicButton elem = new BasicButton("Rename");
                    elem.setToolTipText("Rename the colormap");
                    elem.addActionListener(new ActionListener()
                    {
                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            ColormapDiscrete cmap = (ColormapDiscrete) ColormapState.this.comboDiscrete.getSelectedItem();
                            String prev = cmap.getName();
                            String next = SwingUtils.getString(Viewer.getInstance().gui.getFrame(), "Enter a name:", prev);
                            Viewer.getInstance().control.setStatusMessage("renaming scalar colormap " + prev + " to " + next);
                            cmap.withName(next);
                            ColormapState.this.comboDiscrete.updateUI();
                        }
                    });

                    subpanel.add(elem);
                }
                subpanel.add(Box.createHorizontalGlue());
                {
                    BasicButton elem = new BasicButton("Copy");
                    elem.setToolTipText("Duplicate the given colormap");
                    elem.addActionListener(new ActionListener()
                    {
                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            ColormapDiscrete cmap = (ColormapDiscrete) ColormapState.this.comboDiscrete.getSelectedItem();
                            String guess = cmap.getName() + "-copy";
                            String name = SwingUtils.getString(Viewer.getInstance().gui.getFrame(), "Enter a new name:", guess);

                            ColormapDiscrete ncmap = cmap.copy().withName(name);

                            ColormapState.this.comboDiscrete.addItem(ncmap);
                            ColormapState.this.comboDiscrete.setSelectedItem(ncmap);
                            ColormapState.this.comboDiscrete.updateUI();

                            Viewer.getInstance().control.setStatusMessage("created new colormap: " + name);
                        }
                    });

                    subpanel.add(elem);
                }
                subpanel.add(Box.createHorizontalGlue());
                {
                    BasicButton elem = new BasicButton("Export");
                    elem.setToolTipText("Export a colormap to file (json format)");
                    elem.addActionListener(new ActionListener()
                    {
                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            try
                            {
                                ColormapState.this.copyScalarViewToData();

                                ColormapDiscrete cmap = (ColormapDiscrete) ColormapState.this.comboDiscrete.getSelectedItem();
                                String fn = Viewer.getInstance().gui.chooseSaveFile("Choose a colormap filename for export...", cmap.getName() + ".json");
                                cmap.write(fn);
                                Viewer.getInstance().control.setStatusMessage("saved colormap to: " + fn);
                            }
                            catch (Exception e1)
                            {
                                Viewer.getInstance().control.setStatusMessage("warning: failed to export colormap!");
                            }
                        }
                    });

                    subpanel.add(elem);
                }
                subpanel.add(Box.createHorizontalGlue());
                {
                    BasicButton elem = new BasicButton("Import");
                    elem.setToolTipText("Import a colormap file (json format)");
                    elem.addActionListener(new ActionListener()
                    {
                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            try
                            {
                                String fn = Viewer.getInstance().gui.chooseLoadFiles("Choose a colormap filename to import...").get(0);
                                ColormapDiscrete cmap = ColormapDiscrete.read(fn);
                                Viewer.getInstance().control.setStatusMessage(String.format("loaded discrete colormap from %s with %d entries", fn, cmap.size()));

                                for (int i = 0; i < ColormapState.this.comboDiscrete.getItemCount(); i++)
                                {
                                    ColormapDiscrete item = ColormapState.this.comboDiscrete.getItemAt(i);
                                    if (item.getName().equals(cmap.getName()))
                                    {
                                        cmap.withName(cmap.getName() + "-imported");
                                    }
                                }

                                ColormapState.this.comboDiscrete.addItem(cmap);
                                ColormapState.this.comboDiscrete.setSelectedItem(cmap);
                                ColormapState.this.comboDiscrete.updateUI();
                            }
                            catch (Exception e1)
                            {
                                e1.printStackTrace();
                                Viewer.getInstance().control.setStatusMessage("warning: failed to import colormap!");
                            }
                        }
                    });

                    subpanel.add(elem);
                }
                subpanel.add(Box.createHorizontalGlue());

                panel.add(subpanel);
            }

            wholePanel.add(panel);
        }

        {
            JPanel subpanel = new JPanel();
            subpanel.setLayout(new BoxLayout(subpanel, BoxLayout.LINE_AXIS));

            {
                final JCheckBox elem = new JCheckBox("Immediate Mode");
                elem.setToolTipText("WHen checked, the colormap will be immediately rendered (the default is to wait for you to apply it, since immediate mode is slow for large datasets)");
                elem.setSelected(this.immediate);
                elem.addActionListener(new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        ColormapState.this.immediate = elem.isSelected();
                    }
                });
                subpanel.add(elem);
            }
            subpanel.add(Box.createHorizontalGlue());
            {
                BasicButton elem = new BasicButton("Apply");
                elem.setToolTipText("Apply the colormap changes to the scene");
                elem.addActionListener(new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        ColormapState.this.copyDiscreteViewToData();
                    }
                });
                subpanel.add(elem);
            }
            subpanel.add(Box.createRigidArea(new Dimension(10, 0)));
            {
                BasicButton elem = new BasicButton("Close");
                elem.setToolTipText("Apply the colormap changes to the scene and close this window");
                elem.addActionListener(new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        ColormapState.this.copyDiscreteViewToData();
                        ColormapState.this.hideDiscrete();
                    }
                });
                subpanel.add(elem);
            }

            wholePanel.add(subpanel);
        }

        this.dialogDiscrete = new JDialog(frame, Constants.SETTINGS_COLORMAPS_DISCRETE);
        this.dialogDiscrete.add(wholePanel);
        this.dialogDiscrete.pack();
        this.dialogDiscrete.setResizable(false);
        this.dialogDiscrete.setLocationRelativeTo(frame);
        this.dialogDiscrete.setVisible(false);
        this.dialogDiscrete.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        SwingUtils.addEscapeListener(this.dialogDiscrete);
    }

    private void makeScalarView()
    {
        JFrame frame = Viewer.getInstance().gui.getFrame();

        ControlPanel controls = new ControlPanel();
        controls.addControl("Name:", "specify the scalar colormap to edit", this.comboScalar);
        controls.addControl("Coloring:", "specify the coloring scheme", this.chooserScalarName);
        {
            JPanel subpanel = new JPanel();
            subpanel.setLayout(new BoxLayout(subpanel, BoxLayout.LINE_AXIS));

            subpanel.add(Box.createHorizontalGlue());
            {
                BasicButton elem = new BasicButton("New");
                elem.setToolTipText("Create a new colormap");
                elem.addActionListener(e ->
                {
                    String name = SwingUtils.getString(Viewer.getInstance().gui.getFrame(), "Enter a name:", "my.scalar.colormap");
                    ColormapScalar cmap = new ColormapScalar();
                    cmap.withName(name);

                    ColormapState.this.comboScalar.addItem(cmap);
                    ColormapState.this.comboScalar.setSelectedItem(cmap);
                    ColormapState.this.comboScalar.updateUI();

                    Viewer.getInstance().control.setStatusMessage("created new colormap: " + name);
                });

                subpanel.add(elem);
            }
            subpanel.add(Box.createHorizontalGlue());
            {
                BasicButton elem = new BasicButton("Rename");
                elem.setToolTipText("Rename the colormap");
                elem.addActionListener(e ->
                {
                    ColormapScalar cmap = (ColormapScalar) ColormapState.this.comboScalar.getSelectedItem();
                    String prev = cmap.getName();
                    String next = SwingUtils.getString(Viewer.getInstance().gui.getFrame(), "Enter a name:", prev);
                    Viewer.getInstance().control.setStatusMessage("renaming scalar colormap " + prev + " to " + next);
                    cmap.withName(next);
                    ColormapState.this.comboScalar.updateUI();
                });

                subpanel.add(elem);
            }
            subpanel.add(Box.createHorizontalGlue());
            {
                BasicButton elem = new BasicButton("Export Colorbar Image");
                elem.setToolTipText("Save an image depicting the colorbar");
                elem.addActionListener(e ->
                {
                    try
                    {
                        ColormapState.this.copyScalarViewToData();

                        ColormapScalar cmap = (ColormapScalar) ColormapState.this.comboScalar.getSelectedItem();
                        String fn = Viewer.getInstance().gui.chooseSaveFile("Choose a colorbar output filename...", cmap.getName() + ".png");
                        BufferedImage bi = cmap.colorbar();
                        ImageIO.write(bi, "PNG", new File(fn));
                        Viewer.getInstance().control.setStatusMessage("saved colorbar to: " + fn);
                    }
                    catch (Exception e1)
                    {
                        Viewer.getInstance().control.setStatusMessage("warning: failed to save colorbar!");
                    }
                });

                subpanel.add(elem);
            }
            subpanel.add(Box.createHorizontalGlue());

            controls.addControl(subpanel);
        }

        {
            JPanel subpanel = new JPanel();
            subpanel.setLayout(new BoxLayout(subpanel, BoxLayout.LINE_AXIS));

            subpanel.add(Box.createHorizontalGlue());
            {
                BasicButton elem = new BasicButton("Export All");
                elem.setToolTipText("Export all colormaps to file (json format)");
                elem.addActionListener(e ->
                {
                    try
                    {
                        ColormapState.this.copyScalarViewToData();

                        ColormapScalar[] cmaps = new ColormapScalar[ColormapState.this.comboScalar.getItemCount()];
                        for (int i = 0; i < cmaps.length; i++)
                        {
                            cmaps[i] = ColormapState.this.comboScalar.getItemAt(i);
                        }

                        String fn = Viewer.getInstance().gui.chooseSaveFile("Choose a colormap filename for export...", "mycolormaps.json");
                        FileUtils.write(new File(fn), JsonUtils.encode(cmaps), false);
                        Viewer.getInstance().control.setStatusMessage("exported colormaps to: " + fn);
                    }
                    catch (Exception e1)
                    {
                        Viewer.getInstance().control.setStatusMessage("warning: failed to export colormap!");
                    }
                });

                subpanel.add(elem);
            }
            subpanel.add(Box.createHorizontalGlue());
            {
                BasicButton elem = new BasicButton("Export Current");
                elem.setToolTipText("Export a colormap to file (json format)");
                elem.addActionListener(e ->
                {
                    try
                    {
                        ColormapState.this.copyScalarViewToData();

                        ColormapScalar cmap = (ColormapScalar) ColormapState.this.comboScalar.getSelectedItem();

                        String name = cmap.getName();
                        String guess = name.startsWith("scalar") ? "mycolormap.json" : name + ".json";
                        String fn = Viewer.getInstance().gui.chooseSaveFile("Choose a colormap filename for export...", guess);

                        if (name.startsWith("scalar"))
                        {
                            String next = PathUtils.basename(fn).split(".json")[0];
                            Viewer.getInstance().control.setStatusMessage("renaming scalar colormap " + cmap.getName() + " to " + next);
                            cmap.withName(next);
                            ColormapState.this.comboScalar.updateUI();
                        }

                        cmap.write(fn);
                        Viewer.getInstance().control.setStatusMessage("exported colormap to: " + fn);
                    }
                    catch (Exception e1)
                    {
                        Viewer.getInstance().control.setStatusMessage("warning: failed to export colormap!");
                    }
                });

                subpanel.add(elem);
            }
            subpanel.add(Box.createHorizontalGlue());
            {
                BasicButton elem = new BasicButton("Import");
                elem.setToolTipText("Import a colormap file (json format)");
                elem.addActionListener(e ->
                {
                    List<String> fns = Viewer.getInstance().gui.chooseLoadFiles("Choose a colormap filename to import...");
                    for (String fn : fns)
                    {
                        ColormapState.this.importColormap(fn);
                    }
                });

                subpanel.add(elem);
            }
            subpanel.add(Box.createHorizontalGlue());

            controls.addControl(subpanel);
        }
        controls.addControl(this.chooserScalarTransfer.getPanel());

        JPanel panel = new JPanel();
        panel.add(controls);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel wpanel = new JPanel();
        wpanel.setLayout(new BoxLayout(wpanel, BoxLayout.PAGE_AXIS));
        wpanel.add(panel);
        {

            JPanel subpanel = new JPanel();
            subpanel.setLayout(new BoxLayout(subpanel, BoxLayout.LINE_AXIS));
            subpanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

            {
                final JCheckBox elem = new JCheckBox("Immediate Mode");
                elem.setToolTipText("WHen checked, the colormap will be immediately rendered (the default is to wait for you to apply it, since immediate mode is slow for large datasets)");
                elem.setSelected(this.immediate);
                elem.addActionListener(e -> ColormapState.this.immediate = elem.isSelected());
                subpanel.add(elem);
            }
            subpanel.add(Box.createHorizontalGlue());
            {
                BasicButton elem = new BasicButton("Apply");
                elem.setToolTipText("Apply the colormap changes to the scene");
                elem.addActionListener(e -> ColormapState.this.copyScalarViewToData());
                subpanel.add(elem);
            }
            {
                BasicButton elem = new BasicButton("Close");
                elem.setToolTipText("Apply the colormap changes to the scene and close this window");
                elem.addActionListener(e ->
                {
                    ColormapState.this.copyScalarViewToData();
                    ColormapState.this.hideScalar();
                });
                subpanel.add(elem);
            }

            wpanel.add(subpanel);
        }

        this.dialogScalar = new JDialog(frame, Constants.SETTINGS_COLORMAPS_SCALAR);
        this.dialogScalar.add(wpanel);
        this.dialogScalar.pack();
        this.dialogScalar.setResizable(false);
        this.dialogScalar.setLocationRelativeTo(frame);
        this.dialogScalar.setVisible(false);
        this.dialogScalar.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        SwingUtils.addEscapeListener(this.dialogScalar);
    }
}