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

package qitview.main;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.iharder.dnd.FileDrop;
import org.reflections.Reflections;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleDescription;
import qit.base.structs.Named;
import qit.base.structs.Pair;
import qit.base.utils.ModuleUtils;
import qit.base.utils.PathUtils;
import qitview.models.Viewable;
import qitview.models.ViewableType;
import qitview.models.VolumeSlicePlane;
import qitview.panels.Modules;
import qitview.panels.Processes;
import qitview.panels.QueryVoxel;
import qitview.panels.Viewables;
import qitview.views.MaskView;
import qitview.views.SliceableView;
import qitview.views.VolumeView;
import qitview.widgets.BasicButton;
import qitview.widgets.BasicComboBox;
import qitview.widgets.BasicLabel;
import qitview.widgets.BasicSpinner;
import qitview.widgets.BasicTable;
import qitview.widgets.BasicTextField;
import qitview.widgets.ControlPanel;
import qitview.widgets.FileLoader;
import qitview.widgets.SuggestionPanel;
import qitview.widgets.SwingUtils;
import qitview.widgets.VerticalLayout;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.*;
import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Interface
{
    private transient JFrame frame = new JFrame(Constants.TITLE);
    private transient JPanel info = new JPanel(new CardLayout());
    private transient JPanel render = new JPanel(new CardLayout());
    private transient JPanel edit = new JPanel(new CardLayout());
    private transient JTabbedPane tabs = new JTabbedPane();
    private transient BasicLabel status = new BasicLabel(" ");
    private transient BasicComboBox<String> mode = new BasicComboBox<>();

    private transient JFileChooser chooserLoad = null;
    private transient JFileChooser chooserSave = null;

    public transient Canvas canvas = null;
    public transient State state = null;

    private Runnable updateSliders = () ->
    {
    };

    protected boolean ready = false;

    Interface(State state)
    {
        this.state = state;

        SwingUtilities.invokeLater(() ->
        {
            Logging.info("creating canvas");
            this.canvas = new Canvas(state);

            Logging.info("creating interface");
            JMenuBar menu = this.initMenu();

            this.initView();

            this.clearSelection();
            this.info.setBorder(BorderFactory.createEmptyBorder());
            this.render.setBorder(BorderFactory.createEmptyBorder());
            this.edit.setBorder(BorderFactory.createEmptyBorder());

            ControlPanel modePanel = new ControlPanel();

            // @TODO: look into how the canvas should be added
            Container c3D = new Container();
            c3D.setLayout(new BorderLayout());
            c3D.add(this.canvas);

            JPanel viewPanel = new JPanel(new BorderLayout());
            viewPanel.setPreferredSize(new Dimension(Constants.WIDTH, Constants.HEIGHT));
            viewPanel.add(c3D);

            {
                final BasicComboBox<Canvas.Layout> elem = new BasicComboBox<>();
                for (Canvas.Layout layout : Canvas.Layout.values())
                {
                    elem.addItem(layout);
                }
                elem.setPrototypeDisplayValue(this.canvas.layout);
                elem.setToolTipText("set the layout of the data viewer");
                elem.addActionListener(e -> this.canvas.layout = (Canvas.Layout) elem.getSelectedItem());

                modePanel.addControl("Viewer Layout:", elem);

                this.state.control.setAction(Constants.VIEW_LAYOUT_3D, () -> this.canvas.layout = Canvas.Layout.View3D);
                this.state.control.setAction(Constants.VIEW_LAYOUT_I, () -> this.canvas.layout = Canvas.Layout.SliceI);
                this.state.control.setAction(Constants.VIEW_LAYOUT_J, () -> this.canvas.layout = Canvas.Layout.SliceJ);
                this.state.control.setAction(Constants.VIEW_LAYOUT_K, () -> this.canvas.layout = Canvas.Layout.SliceK);
                this.state.control.setAction(Constants.VIEW_LAYOUT_I3D, () -> this.canvas.layout = Canvas.Layout.SliceI3D);
                this.state.control.setAction(Constants.VIEW_LAYOUT_J3D, () -> this.canvas.layout = Canvas.Layout.SliceJ3D);
                this.state.control.setAction(Constants.VIEW_LAYOUT_K3D, () -> this.canvas.layout = Canvas.Layout.SliceK3D);
                this.state.control.setAction(Constants.VIEW_LAYOUT_1BY3, () -> this.canvas.layout = Canvas.Layout.OneByThree);
                this.state.control.setAction(Constants.VIEW_LAYOUT_2BY2, () -> this.canvas.layout = Canvas.Layout.TwoByTwo);
            }
            {
                final BasicComboBox<Named<Viewable<?>>> elem = this.state.data.getComboBox(ViewableType.Volume, true, true);
                elem.setPrototypeDisplayValue(Viewables.NONE);
                elem.setToolTipText("set the volume shown in the 2D slice views");
                Supplier<Named<Viewable<?>>> elemget = () -> elem.getItemAt(elem.getSelectedIndex());
                elem.addActionListener(e ->
                {
                    Named<Viewable<?>> entry = elemget.get();
                    VolumeView volume = (VolumeView) entry.getValue();

                    this.state.gui.setStatusMessage("set reference volume: " + entry.getName());
                    this.state.referenceVolume = volume;
                    this.updateSliders.run();
                });
                elem.setToolTipText("Specify the 2D slice reference volume");
                modePanel.addControl("2D Reference:", elem);

                this.state.data.addObserverData((a, b) ->
                {
                    // if the reference points to missing data, fix this
                    if (!this.state.data.has(elemget.get().getValue()))
                    {
                        elem.setSelectedItem(Viewables.NONE);
                    }

                    // if no reference volume is set, then detect when one is loaded and use it
                    if (elemget.get().equals(Viewables.NONE))
                    {
                        for (int i = 0; i < this.state.data.size(); i++)
                        {
                            Named<Viewable<?>> entry = this.state.data.get(i);
                            if (entry.getValue() instanceof VolumeView)
                            {
                                elem.setSelectedItem(entry);
                                break;
                            }
                        }
                    }
                });
            }
            {
                final BasicComboBox<Named<Viewable<?>>> elem = this.state.data.getComboBox(ViewableType.Volume, true, true);
                elem.setPrototypeDisplayValue(Viewables.NONE);
                elem.setToolTipText("set the overlay shown in the 2D slice views");
                Supplier<Named<Viewable<?>>> elemget = () -> elem.getItemAt(elem.getSelectedIndex());
                elem.addActionListener(e ->
                {
                    Named<Viewable<?>> entry = elemget.get();
                    VolumeView overlay = (VolumeView) entry.getValue();

                    this.state.gui.setStatusMessage("set 2D overlay: " + entry.getName());
                    this.state.referenceOverlay = overlay;
                });
                elem.setToolTipText("Specify the 2D overlay volume");
                modePanel.addControl("2D Overlay:", elem);

                this.state.data.addObserverData((a, b) ->
                {
                    // if the reference points to missing data, fix this
                    if (!this.state.data.has(elemget.get().getValue()))
                    {
                        elem.setSelectedItem(Viewables.NONE);
                    }
                });
            }
            {
                final BasicComboBox<Named<Viewable<?>>> elem = this.state.data.getComboBox(ViewableType.Mask, true, true);
                elem.setPrototypeDisplayValue(Viewables.NONE);
                elem.setToolTipText("set the mask shown in the 2D slice views");
                Supplier<Named<Viewable<?>>> elemget = () -> elem.getItemAt(elem.getSelectedIndex());
                elem.addActionListener(e ->
                {
                    Named<Viewable<?>> entry = elemget.get();
                    MaskView mask = (MaskView) entry.getValue();

                    this.state.gui.setStatusMessage("set reference mask: " + entry.getName());
                    this.state.referenceMask = mask;
                });
                elem.setToolTipText("Specify the 2D slice reference mask");
                modePanel.addControl("2D Mask:", elem);

                this.state.data.addObserverData((a, b) ->
                {
                    // if the reference points to missing data, fix this
                    if (!this.state.data.has(elemget.get().getValue()))
                    {
                        elem.setSelectedItem(Viewables.NONE);
                    }

                    // if no reference volume is set, then detect when one is loaded and use it
                    if (elemget.get().equals(Viewables.NONE))
                    {
                        for (int i = 0; i < this.state.data.size(); i++)
                        {
                            Named<Viewable<?>> entry = this.state.data.get(i);
                            if (entry.getValue() instanceof MaskView)
                            {
                                elem.setSelectedItem(entry);
                                break;
                            }
                        }
                    }
                });
            }

            {
                this.mode.removeAllItems();
                this.mode.addItem(Constants.INTERACTION_ROTATE);
                this.mode.addItem(Constants.INTERACTION_PAN);
                this.mode.addItem(Constants.INTERACTION_ZOOM);
                this.mode.setSelectedIndex(0);

                this.mode.addActionListener(e ->
                {
                    String nmode = (String) this.mode.getSelectedItem();
                    this.canvas.render3D.mode = nmode;
                    this.setStatusMessage("changed interaction mode to " + nmode);
                });

                modePanel.addControl("3D Interaction:", this.mode);
            }
//            {
//                JSlider sliceI = new JSlider(JSlider.HORIZONTAL, 0, 1000, 0);
//                sliceI.setForeground(Color.RED);
//                sliceI.addChangeListener(e ->
//                {
//                    if (this.hasReferenceVolume())
//                    {
//                        this.getReferenceVolume().setSliceI(sliceI.getValue());
//                    }
//                });
//
//                JSlider sliceJ = new JSlider(JSlider.HORIZONTAL, 0, 1000, 0);
//                sliceJ.setForeground(Color.GREEN);
//                sliceJ.addChangeListener(e ->
//                {
//                    if (this.hasReferenceVolume())
//                    {
//                        this.getReferenceVolume().setSliceJ(sliceJ.getValue());
//                    }
//                });
//
//                JSlider sliceK = new JSlider(JSlider.HORIZONTAL, 0, 1000, 0);
//                sliceK.setForeground(Color.BLUE);
//                sliceK.addChangeListener(e ->
//                {
//                    if (this.hasReferenceVolume())
//                    {
//                        this.getReferenceVolume().setSliceK(sliceK.getValue());
//                    }
//                });
//
//                this.updateSliders = () ->
//                {
//                    if (this.hasReferenceVolume())
//                    {
//                        VolumeView volume = this.getReferenceVolume();
//                        sliceI.setValue(volume.getSlicer().idxI());
//                        sliceI.setMaximum(volume.getData().getSampling().numI());
//                        sliceJ.setValue(volume.getSlicer().idxJ());
//                        sliceJ.setMaximum(volume.getData().getSampling().numJ());
//                        sliceK.setValue(volume.getSlicer().idxK());
//                        sliceK.setMaximum(volume.getData().getSampling().numK());
//                    }
//                };
//
//                modePanel.addControl(sliceI);
//                modePanel.addControl(sliceJ);
//                modePanel.addControl(sliceK);
//            }
            {
                Runnable reset2D = () ->
                {
                    this.canvas.renderI.resetView();
                    this.canvas.renderJ.resetView();
                    this.canvas.renderK.resetView();
                };

                final BasicButton elem2D = new BasicButton("Reset 2D View");
                elem2D.setToolTipText("reset2D the position and zoom of the 2D slices to their defaults");
                elem2D.addActionListener((e) -> reset2D.run());
                this.state.control.setAction(Constants.VIEW_RESET_VIEW_2D, reset2D);

                Runnable reset3D = () ->
                {
                    this.canvas.render3D.resetPose();
                };

                final BasicButton elem3D = new BasicButton("Reset 3D View");
                elem3D.setToolTipText("reset3D the position and zoom of the 3D view :w" +
                        "to their defaults");
                elem3D.addActionListener((e) -> reset3D.run());

                this.state.control.setAction(Constants.VIEW_POSE_RESET, reset3D);

                modePanel.addControl(elem2D, elem3D);
            }
            {
                BasicButton chooseBG = new BasicButton(Constants.SETTINGS_BG_SET);
                chooseBG.addActionListener(e ->
                {
                    Color color = SwingUtils.chooseColor(this.frame, this.state.control.getBackgroundColor());
                    this.state.control.setBackgroundColor(color);
                    this.state.control.setStatusMessage("updated background color");
                });

                BasicButton resetBG = new BasicButton(Constants.SETTINGS_BG_BLACK);
                resetBG.addActionListener(e ->
                {
                    this.state.control.setBackgroundColor(Color.BLACK);
                    this.state.control.setStatusMessage("set background color to black");
                });

                modePanel.addControl(chooseBG, resetBG);
            }
            {
                int max = 4;
                int sub = 1000;
                JSlider window = new JSlider(JSlider.HORIZONTAL, 1, max * sub, sub / 2);
                window.addChangeListener(e ->
                {
                    this.state.settings.referenceWindow = window.getValue() / (double) sub;
                });
                window.setToolTipText("specify the thickness of the slice for including geometry in a 2D viewing panel");

                modePanel.addControl("3D Slab Size", window);
            }
            {
                int breaks = 300;
                JSlider window = new JSlider(JSlider.HORIZONTAL, 1, breaks, (int) (breaks * this.state.settings.halve));
                window.addChangeListener(e ->
                {
                    this.state.settings.split = window.getValue() / (double) breaks;
                });
                window.setToolTipText("specify the proportion of the window used to show 2D slices in the 1x3 layout");

                modePanel.addControl("Split in 1x3", window);
            }
            {
                int breaks = 300;
                JSlider window = new JSlider(JSlider.HORIZONTAL, 1, breaks, (int) (breaks * this.state.settings.halve));
                window.addChangeListener(e ->
                {
                    this.state.settings.halve = window.getValue() / (double) breaks;
                });
                window.setToolTipText("specify the proportion of the window used to show 2D slices in the split layout");

                modePanel.addControl("Split in Two Panels", window);
            }
            {
                JCheckBox elem = new JCheckBox();
                elem.setSelected(this.canvas.renderI.getVolumeVisible());
                elem.addItemListener(e ->
                {
                    boolean v = e.getStateChange() == ItemEvent.SELECTED;
                    this.canvas.renderI.setVolumeVisible(v);
                    this.canvas.renderJ.setVolumeVisible(v);
                    this.canvas.renderK.setVolumeVisible(v);
                    this.state.control.setStatusMessage("updated 2D volume visibility " + v);
                });
                modePanel.addControl("Show 2D Volume", elem);

                this.state.control.setAction(Constants.VIEW_TOGGLE_MASK_2D, () ->
                {
                    boolean v = !this.canvas.renderI.getVolumeVisible();
                    this.canvas.renderI.setVolumeVisible(v);
                    this.canvas.renderJ.setVolumeVisible(v);
                    this.canvas.renderK.setVolumeVisible(v);
                    elem.setSelected(v);
                    this.state.control.setStatusMessage("updated 2D volume visibility " + v);
                });
            }
            {
                JCheckBox elem = new JCheckBox();
                elem.setSelected(this.canvas.renderI.getGeometryVisible());
                elem.addItemListener(e ->
                {
                    boolean v = e.getStateChange() == ItemEvent.SELECTED;
                    this.canvas.renderI.setGeometryVisible(v);
                    this.canvas.renderJ.setGeometryVisible(v);
                    this.canvas.renderK.setGeometryVisible(v);
                    this.state.control.setStatusMessage("updated 2D geometry visibility " + v);
                });
                modePanel.addControl("Show 2D Geometry", elem);

                this.state.control.setAction(Constants.VIEW_TOGGLE_MASK_2D, () ->
                {
                    boolean v = !this.canvas.renderI.getGeometryVisible();
                    this.canvas.renderI.setGeometryVisible(v);
                    this.canvas.renderJ.setGeometryVisible(v);
                    this.canvas.renderK.setGeometryVisible(v);
                    elem.setSelected(v);
                    this.state.control.setStatusMessage("updated 2D geometry visibility " + v);
                });
            }
            {
                JCheckBox elem = new JCheckBox();
                elem.setSelected(this.canvas.renderI.getMaskVisible());
                elem.addItemListener(e ->
                {
                    boolean v = e.getStateChange() == ItemEvent.SELECTED;
                    this.canvas.renderI.setMaskVisible(v);
                    this.canvas.renderJ.setMaskVisible(v);
                    this.canvas.renderK.setMaskVisible(v);
                    this.state.control.setStatusMessage("updated 2D mask visibility " + v);
                });
                modePanel.addControl("Show 2D Mask", elem);

                this.state.control.setAction(Constants.VIEW_TOGGLE_MASK_2D, () ->
                {
                    boolean v = !this.canvas.renderI.getMaskVisible();
                    this.canvas.renderI.setMaskVisible(v);
                    this.canvas.renderJ.setMaskVisible(v);
                    this.canvas.renderK.setMaskVisible(v);
                    elem.setSelected(v);
                    this.state.control.setStatusMessage("updated 2D mask visibility " + v);
                });
            }
            {
                JCheckBox elem = new JCheckBox();
                elem.setSelected(this.canvas.renderI.getCrossVisible());
                elem.addItemListener(e ->
                {
                    boolean v = e.getStateChange() == ItemEvent.SELECTED;
                    this.canvas.renderI.setCrossVisible(v);
                    this.canvas.renderJ.setCrossVisible(v);
                    this.canvas.renderK.setCrossVisible(v);
                    this.state.control.setStatusMessage("updated 2D cross visibility " + v);
                });
                modePanel.addControl("Show 2D Cross", elem);

                this.state.control.setAction(Constants.VIEW_TOGGLE_CROSS_2D, () ->
                {
                    boolean v = !this.canvas.renderI.getCrossVisible();
                    this.canvas.renderI.setCrossVisible(v);
                    this.canvas.renderJ.setCrossVisible(v);
                    this.canvas.renderK.setCrossVisible(v);
                    elem.setSelected(v);
                    this.state.control.setStatusMessage("updated 2D cross visibility " + v);
                });
            }
            {
                JCheckBox elem = new JCheckBox();
                elem.setSelected(this.canvas.renderI.getOverlayVisible());
                elem.addItemListener(e ->
                {
                    boolean v = e.getStateChange() == ItemEvent.SELECTED;
                    this.canvas.renderI.setOverlayVisible(v);
                    this.canvas.renderJ.setOverlayVisible(v);
                    this.canvas.renderK.setOverlayVisible(v);
                    this.state.control.setStatusMessage("updated 2D overlay visibility " + v);
                });
                modePanel.addControl("Show 2D Overlay", elem);

                this.state.control.setAction(Constants.VIEW_TOGGLE_OVERLAY_2D, () ->
                {
                    boolean v = !this.canvas.renderI.getOverlayVisible();
                    this.canvas.renderI.setOverlayVisible(v);
                    this.canvas.renderJ.setOverlayVisible(v);
                    this.canvas.renderK.setOverlayVisible(v);
                    elem.setSelected(v);
                    this.state.control.setStatusMessage("updated 2D overlay visibility " + v);
                });
            }

            this.tabs.addTab(Constants.TAB_GLOBAL, new JScrollPane(modePanel));
            this.tabs.addTab(Constants.TAB_INFO, new JScrollPane(this.info));
            this.tabs.addTab(Constants.TAB_VIEW, new JScrollPane(this.render));
            this.tabs.addTab(Constants.TAB_EDIT, new JScrollPane(this.edit));
            this.tabs.addTab(Constants.TAB_QUERY, this.state.query);

            JScrollPane data = BasicTable.createStripedJScrollPane(this.state.data.getTable());

            JTabbedPane dataTab = new JTabbedPane();
            dataTab.add("Datasets", data);

            JSplitPane user = new JSplitPane(JSplitPane.VERTICAL_SPLIT, dataTab, this.tabs);
            user.setDividerLocation(Constants.USER_HEIGHT);
            user.setBorder(null);
            user.setOneTouchExpandable(false);
            user.setMinimumSize(new Dimension(1, 1));
            user.setPreferredSize(new Dimension(Constants.USER_WIDTH, Constants.HEIGHT));

            JSplitPane center = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, viewPanel, user);
            center.setDividerLocation(Constants.WIDTH);
            center.setBorder(null);
            center.setOneTouchExpandable(false);

            JPanel statusPanel = new JPanel();
            statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
            statusPanel.add(this.status);
            this.status.setHorizontalAlignment(SwingConstants.LEFT);

            JPanel content = new JPanel();
            content.setLayout(new BorderLayout());
            content.add(center, BorderLayout.CENTER);
            content.add(statusPanel, BorderLayout.SOUTH);

            Logging.info("creating window");
            this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            this.frame.setJMenuBar(menu);
            this.frame.setContentPane(content);
            this.frame.pack();
            this.frame.setVisible(true);
            this.frame.addWindowListener(new WindowAdapter()
            {
                public void windowClosing(WindowEvent e)
                {
                    // Run this on another thread than the AWT event queue to
                    // make sure the call to Animator.stop() completes before
                    // exiting
                    new Thread(() ->
                    {
                        Interface.this.canvas.animator.stop();
                        System.exit(0);
                    }).start();
                }
            });

            new FileDrop(this.frame, files ->
            {
                List<String> fns = Lists.newArrayList();
                for (File file : files)
                {
                    String fn = file.getAbsolutePath();
                    if (PathUtils.exists(fn))
                    {
                        fns.add(fn);
                    }
                    else
                    {
                        Logging.info("skipping: " + fn);
                    }
                }

                new FileLoader(fns);
            });

            this.ready = true;
        });

//        new Thread(() ->
//        {
//            boolean slow = false;
//
//            while (true)
//            {
//                try
//                {
//                    Thread.sleep(1000);
//                }
//                catch (InterruptedException e)
//                {
//                    e.printStackTrace();
//                }
//
//                if (!Viewer.ready() || !this.state.gui.ready)
//                {
//                    continue;
//                }
//
//                long current = System.currentTimeMillis();
//                long elapsed = current - this.state.settings.touched;
//                elapsed = Math.min(elapsed, current - this.canvas.renderI.touched);
//                elapsed = Math.min(elapsed, current - this.canvas.renderJ.touched);
//                elapsed = Math.min(elapsed, current - this.canvas.renderK.touched);
//
//                if (elapsed > Constants.FPS_TIMEOUT && !slow)
//                {
//                    Logging.info("using background fps");
//                    this.state.settings.animate(Constants.FPS_SLOW);
//                    this.canvas.renderI.animate(Constants.FPS_SLOW);
//                    this.canvas.renderJ.animate(Constants.FPS_SLOW);
//                    this.canvas.renderK.animate(Constants.FPS_SLOW);
//
//                    slow = true;
//                }
//                else if (elapsed < Constants.FPS_TIMEOUT && slow)
//                {
//                    Logging.info("using background fps");
//                    this.state.settings.animate(Constants.FPS_FAST);
//                    this.canvas.renderI.animate(Constants.FPS_FAST);
//                    this.canvas.renderJ.animate(Constants.FPS_FAST);
//                    this.canvas.renderK.animate(Constants.FPS_FAST);
//
//                    slow = false;
//                }
//            }
//        }).start();

        this.setStatusMessage("Welcome to qitview!");
    }

    /********************************
     * PUBLIC INTERFACE
     ********************************/

    public void setStatusMessage(String status)
    {
        this.status.setText(status);
        this.status.setForeground(Color.BLACK);
    }

    public void setStatusError(String v)
    {
        this.status.setText(v);
        this.status.setForeground(Color.RED);
    }

    public JFrame getFrame()
    {
        return this.frame;
    }

    public void close()
    {
        Logging.info("closing interface");
        this.state.running = false;
    }

    public String chooseSaveFile(String title, String fn)
    {
        Logging.info("choosing file to save");

        this.chooserSave.setDialogTitle(title);
        this.chooserSave.setSelectedFile(new File(fn));

        if (this.chooserSave.showSaveDialog(this.frame) != JFileChooser.APPROVE_OPTION)
        {
            return null;
        }

        return this.chooserSave.getSelectedFile().getAbsolutePath();
    }

    public java.util.List<String> chooseLoadFiles(String title)
    {
        return chooseLoadFiles(title, this.frame);
    }

    public java.util.List<String> chooseLoadFiles(String title, Component parent)
    {
        this.state.control.setStatusMessage("prompting for file to load");

        this.chooserLoad.setDialogTitle(title);
        if (this.chooserLoad.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION)
        {
            Logging.info("warning: load cancelled by user");
            return null;
        }

        java.util.List<String> out = Lists.newArrayList();
        for (File file : this.chooserLoad.getSelectedFiles())
        {
            out.add(file.getAbsolutePath());
        }

        return out;
    }

    public void clearSelection()
    {
        Consumer<JPanel> clearPanel = p ->
        {
            p.removeAll();
            JPanel nosel = new JPanel();
            p.add(nosel, Constants.NOSEL);
        };

        clearPanel.accept(this.info);
        clearPanel.accept(this.render);
        clearPanel.accept(this.edit);
    }

    public void resetMode()
    {
        this.mode.setSelectedIndex(0);
        String nmode = (String) this.mode.getSelectedItem();
        this.canvas.render3D.mode = nmode;
        this.setStatusMessage("changed interaction mode to " + nmode);
    }

    public void showProcesses(boolean persist)
    {
        if (persist)
        {
            this.state.processes.request();
        }
        else
        {
            this.state.processes.show();
        }
    }

    public Modules getModules()
    {
        return this.state.modules;
    }

    public QueryVoxel getQuery()
    {
        return this.state.query;
    }

    public void showQuery()
    {
        this.tabs.setSelectedComponent(this.state.query);
    }

    public boolean shownQuery()
    {
        return this.tabs.getSelectedComponent().equals(this.state.query) && this.state.query.isVisible();
    }

    public boolean hasReferenceVolume()
    {
        return this.state.referenceVolume != null && this.state.referenceVolume.hasData();
    }

    public boolean hasReferenceOverlay()
    {
        return this.state.referenceOverlay != null && this.state.referenceOverlay.hasData();
    }

    public boolean hasReferenceMask()
    {
        return this.state.referenceMask != null && this.state.referenceMask.hasData();
    }

    public VolumeView getReferenceVolume()
    {
        return this.state.referenceVolume;
    }

    public VolumeView getReferenceOverlay()
    {
        return this.state.referenceOverlay;
    }

    public MaskView getReferenceMask()
    {
        return this.state.referenceMask;
    }

    public void changeSlice(int delta)
    {
        if (delta != 0)
        {
            Viewables data = this.state.data;

            if (this.canvas.isVisible3D())
            {
                data.getFirstSelectionIndex().ifPresent((idx) ->
                {
                    Viewable<?> viewable = data.getViewable(idx);
                    if (viewable instanceof SliceableView)
                    {
                        ((SliceableView) viewable).changeSlice(delta);
                    }
                });
            }
            else
            {
                if (this.canvas.isVisible2D(VolumeSlicePlane.I))
                {
                    if (this.canvas.renderI.hasVolume())
                    {
                        this.canvas.renderI.getVolume().changeSlice(delta, VolumeSlicePlane.I);
                    }
                    else if (this.canvas.renderI.hasMask())
                    {
                        this.canvas.renderI.getMask().changeSlice(delta, VolumeSlicePlane.I);
                    }
                }
                else if (this.canvas.isVisible2D(VolumeSlicePlane.J))
                {
                    if (this.canvas.renderJ.hasVolume())
                    {
                        this.canvas.renderJ.getVolume().changeSlice(delta, VolumeSlicePlane.J);
                    }
                    else if (this.canvas.renderJ.hasMask())
                    {
                        this.canvas.renderJ.getMask().changeSlice(delta, VolumeSlicePlane.J);
                    }
                }
                else if (this.canvas.isVisible2D(VolumeSlicePlane.K))
                {
                    if (this.canvas.renderK.hasVolume())
                    {
                        this.canvas.renderK.getVolume().changeSlice(delta, VolumeSlicePlane.K);
                    }
                    else if (this.canvas.renderK.hasMask())
                    {
                        this.canvas.renderK.getMask().changeSlice(delta, VolumeSlicePlane.K);
                    }
                }
            }
        }
    }

    public void addProcess(String message, Runnable run)
    {
        SwingUtilities.invokeLater(() -> this.state.processes.setVisible(true));
        this.state.processes.add(message, run);
    }

    public void setSelection(Viewable entry)
    {
        {
            this.edit.removeAll();
            this.edit.add(entry.getEditPanel(), entry.getName());
            this.edit.repaint();
        }
        {
            this.render.removeAll();
            this.render.add(entry.getRenderPanel(), entry.getName());
            this.render.repaint();
        }
        {
            this.info.removeAll();
            this.info.add(entry.getInfoPanel(), entry.getName());
            this.info.repaint();
        }

        String prevMode = this.canvas.render3D.mode;
        this.mode.removeAllItems();
        this.mode.addItem(Constants.INTERACTION_ROTATE);
        this.mode.addItem(Constants.INTERACTION_PAN);
        this.mode.addItem(Constants.INTERACTION_ZOOM);

        for (Object m : entry.modes())
        {
            this.mode.addItem((String) m);
        }

        if (((DefaultComboBoxModel) this.mode.getModel()).getIndexOf(prevMode) == -1)
        {
            this.mode.setSelectedIndex(0);
        }
        else
        {
            this.mode.setSelectedItem(prevMode);
        }
    }

    /********************************
     * PRIVATE CONSTRUCTION METHODS
     ********************************/

    private void addMenuItem(JMenu menu, String cmd, String tip)
    {
        JMenuItem item = new JMenuItem(cmd);
        item.setActionCommand(cmd);
        item.addActionListener(this.state.control);
        item.setToolTipText(tip);
        menu.add(item);
    }

    private void addMenuItem(JMenu menu, String cmd, int key, int mod, String tip)
    {
        JMenuItem item = new JMenuItem(cmd);
        item.setActionCommand(cmd);
        item.addActionListener(this.state.control);
        item.setToolTipText(tip);
        item.setAccelerator(KeyStroke.getKeyStroke(key, mod));
        menu.add(item);
    }

    private void addMenuItem(JMenu menu, String cmd, String tip, ActionListener listener)
    {
        JMenuItem item = new JMenuItem(cmd);
        item.setActionCommand(cmd);
        item.addActionListener(listener);
        item.setToolTipText(tip);
        menu.add(item);
    }

    private JMenuBar initMenu()
    {
        int menuCode = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        int shiftCode = InputEvent.SHIFT_DOWN_MASK;

        JMenuBar menubar = new JMenuBar();
        {
            JMenu menu = new JMenu("File");
            menu.getPopupMenu().setLightWeightPopupEnabled(false);

            this.addMenuItem(menu, Constants.FILE_MENU_LOAD_FILES, 'O', menuCode, "load a group of files");
            {
                JMenu submenu = new JMenu("Load Files by Type...");
                submenu.getPopupMenu().setLightWeightPopupEnabled(false);
                submenu.setToolTipText("load files with a specific file type");

                for (ViewableType d : ViewableType.values())
                {
                    String id = d.getText();
                    JMenuItem item = new JMenuItem(id);
                    item.setActionCommand(Constants.LOAD_PREFIX + id);
                    item.addActionListener(this.state.control);
                    submenu.add(item);
                }
                menu.add(submenu);
            }
            menu.addSeparator();
            this.addMenuItem(menu, Constants.FILE_MENU_SAVE_SEL_FILES, 'S', menuCode, "save the data selected in the workspace panel (will ask for a new filename or use the last known filename)");
            this.addMenuItem(menu, Constants.FILE_MENU_SAVE_SEL_FILES_AS, 'S', menuCode | shiftCode, "save the data selected in the workspace panel (will ask for new filenames for all of them)");
            this.addMenuItem(menu, Constants.FILE_MENU_SAVE_ALL_FILES, "save all data (will ask for a new filename or use the last known filename)");
            this.addMenuItem(menu, Constants.FILE_MENU_SAVE_ALL_FILES_AS, "save all data (will ask for new filenames for all of them)");
            menu.addSeparator();
            {
                JMenu submenu = new JMenu("Scene...");
                this.addMenuItem(submenu, Constants.FILE_MENU_LOAD_SCENE, "load a scene (select a directory from previously saving a scene)");
                this.addMenuItem(submenu, Constants.FILE_MENU_SAVE_SEL_SCENE, "save the data selected in the workspace to a scene directory (will either ask for a new scene directory or use the last known directory)");
                this.addMenuItem(submenu, Constants.FILE_MENU_SAVE_SEL_SCENE_AS, "save the data selected in the workspace to a scene directory (will either ask for a new scene directory)");
                this.addMenuItem(submenu, Constants.FILE_MENU_SAVE_ALL_SCENE, "save all data to a scene directory (will either ask for a new scene directory or use the last known directory)");
                this.addMenuItem(submenu, Constants.FILE_MENU_SAVE_ALL_SCENE_AS, "save all data to a scene directory (will either ask for a new scene directory)");
                menu.add(submenu);
            }
            {
                JMenu submenu = new JMenu("Global Settings...");
                this.addMenuItem(submenu, Constants.FILE_MENU_LOAD_GLOBAL, "load global settings (select a JSON file from previously saving)");
                this.addMenuItem(submenu, Constants.FILE_MENU_SAVE_GLOBAL, "save the global settings to a file, e.g. rendering options and viewing orientation");
                this.addMenuItem(submenu, Constants.FILE_MENU_SAVE_DEFAULT_GLOBAL, "save the global settings to the system defaults, e.g. rendering options and viewing orientation");
                this.addMenuItem(submenu, Constants.FILE_MENU_CLEAR_DEFAULT_GLOBAL, "clear the system defaults for the global settings to a file, e.g. rendering options and viewing orientation");
                menu.add(submenu);
            }
            {
                JMenu submenu = new JMenu("Screenshot...");
                this.addMenuItem(submenu, Constants.FILE_MENU_SHOT_1X, '1', menuCode | shiftCode, "take a screenshot at the current screen resolution (useful for quality control)");
                this.addMenuItem(submenu, Constants.FILE_MENU_SHOT_2X, '2', menuCode | shiftCode, "take a screenshot at double the current resolution (useful for antialiasing)");
                this.addMenuItem(submenu, Constants.FILE_MENU_SHOT_3X, '3', menuCode | shiftCode, "take a screenshot at triple the current resolution (useful for figures)");
                this.addMenuItem(submenu, Constants.FILE_MENU_SHOT_NX, "take a screenshot at some user defined zoom of the current resolution (useful for posters)");
                menu.add(submenu);
            }
            menu.addSeparator();
            this.addMenuItem(menu, Constants.FILE_MENU_QUIT, 'Q', menuCode, "quit the viewer (warning: any unsaved data will be deleted)");

            menubar.add(menu);
        }
        {
            JMenu menu = new JMenu("Workspace");
            menu.getPopupMenu().setLightWeightPopupEnabled(false);

            this.addMenuItem(menu, Constants.VIEW_LAYOUT_3D, '1', menuCode, "Show a single 3D viewing window");
            this.addMenuItem(menu, Constants.VIEW_LAYOUT_I, '2', menuCode, "Show a single 2D viewing window of the I slice");
            this.addMenuItem(menu, Constants.VIEW_LAYOUT_J, '3', menuCode, "Show a single 2D viewing window of the J slice");
            this.addMenuItem(menu, Constants.VIEW_LAYOUT_K, '4', menuCode, "Show a single 2D viewing window of the K slice");
            this.addMenuItem(menu, Constants.VIEW_LAYOUT_I3D, "Show a combined 2D/3D view of the I slice");
            this.addMenuItem(menu, Constants.VIEW_LAYOUT_J3D, "Show a combined 2D/3D view of the J slice");
            this.addMenuItem(menu, Constants.VIEW_LAYOUT_K3D, "Show a combined 2D/3D view of the K slice");
            this.addMenuItem(menu, Constants.VIEW_LAYOUT_1BY3, '5', menuCode, "Show a combined 3D and 2D viewing window with a large 3D view");
            this.addMenuItem(menu, Constants.VIEW_LAYOUT_2BY2, '6', menuCode, "Show a combined 3D and 2D viewing window in a grid layout");
            menu.addSeparator();
            this.addMenuItem(menu, Constants.VIEW_TOGGLE, 'T', menuCode, "toggle the visibility of the selected data objects (useful for looking for differences between data objects)");
            this.addMenuItem(menu, Constants.VIEW_LIST_NEXT, 'N', menuCode, "hide the current selection and show the next in the list (useful for sequentially viewing data objects)");
            this.addMenuItem(menu, Constants.VIEW_LIST_PREV, 'P', menuCode, "hide the current selection and show the previous in the list (useful for sequentially viewing data objects)");
            this.addMenuItem(menu, Constants.VIEW_SHOW_ONLY, "show only the selected data by hiding all others (useful for isolating a single data object in a complex scene)");
            this.addMenuItem(menu, Constants.VIEW_SHOW_ALL, "show all data (useful for quickly seeing what is loaded)");
            menu.addSeparator();
            this.addMenuItem(menu, Constants.VIEW_MOVE_UP, '[', menuCode, "move the selected data object up in the list (useful for organization and rendering with transparency)");
            this.addMenuItem(menu, Constants.VIEW_MOVE_DOWN, ']', menuCode, "move the selected data object down in the list (useful for organization and rendering with transparency)");
            this.addMenuItem(menu, Constants.VIEW_MOVE_TOP, '[', menuCode | shiftCode, "move the selected data object to the top of the list (useful for quickly fixing order-dependent transparency issues)");
            this.addMenuItem(menu, Constants.VIEW_MOVE_BOTTOM, ']', menuCode | shiftCode, "move the selected data object to the bototm of the list (useful for quickly fixing order-dependent transparency issues)");

            menubar.add(menu);
        }
        {
            JMenu menu = new JMenu("View");
            menu.getPopupMenu().setLightWeightPopupEnabled(false);

            this.addMenuItem(menu, Constants.VIEW_POSE_RESET, 'V', menuCode | shiftCode, "reset the viewing orientation and zoom");
            this.addMenuItem(menu, Constants.VIEW_POSE_ANGLES, '4', menuCode | shiftCode, "set the viewing orientation to specific angles (useful for loading your favorite view)");
            this.addMenuItem(menu, Constants.VIEW_POSE_LEFT, '5', menuCode | shiftCode, "move the camera to the left of the dataset (useful for quickly switching between views)");
            this.addMenuItem(menu, Constants.VIEW_POSE_RIGHT, '6', menuCode | shiftCode, "move the camera to the right of the dataset (useful for quickly switching between views)");
            this.addMenuItem(menu, Constants.VIEW_POSE_TOP, '7', menuCode | shiftCode, "move the camera to the top of the dataset (useful for quickly switching between views)");
            this.addMenuItem(menu, Constants.VIEW_POSE_BOTTOM, '8', menuCode | shiftCode, "move the camera to the bottom of the dataset (useful for quickly switching between views)");
            this.addMenuItem(menu, Constants.VIEW_POSE_FRONT, '9', menuCode | shiftCode, "move the camera to the front of the dataset (useful for quickly switching between views)");
            this.addMenuItem(menu, Constants.VIEW_POSE_BACK, '0', menuCode | shiftCode, "move the camera to the back of the dataset (useful for quickly switching between views)");
            menu.addSeparator();
            this.addMenuItem(menu, Constants.VIEW_ZOOM_DETAIL, '=', menuCode, "zoom in to the selected dataset (useful for rotating around a specific part of the scene)");
            this.addMenuItem(menu, Constants.VIEW_ZOOM_OVERVIEW, '-', menuCode, "zoom out to view all data (useful for getting a view of the whole scene)");
            menu.addSeparator();
            this.addMenuItem(menu, Constants.VIEW_SLICE_I, 'Z', menuCode, "Toggle the visibility of slice I of the selected volume (useful for quickly hiding the slice)");
            this.addMenuItem(menu, Constants.VIEW_SLICE_J, 'X', menuCode, "Toggle the visibility of slice J of the selected volume (useful for quickly hiding the slice)");
            this.addMenuItem(menu, Constants.VIEW_SLICE_K, 'C', menuCode, "Toggle the visibility of slice K of the selected volume (useful for quickly hiding the slice)");
            menu.addSeparator();
            this.addMenuItem(menu, Constants.VIEW_TOGGLE_MASK_2D, 'Z', menuCode | shiftCode, "Toggle the visibility of mask in the 2D slice viewer");
            this.addMenuItem(menu, Constants.VIEW_TOGGLE_CROSS_2D, 'X', menuCode | shiftCode, "Toggle the visibility of cross in the 2D slice viewer");
            this.addMenuItem(menu, Constants.VIEW_TOGGLE_OVERLAY_2D, 'C', menuCode | shiftCode, "Toggle the visibility of cross in the 2D slice viewer");
            menu.addSeparator();
            this.addMenuItem(menu, Constants.VIEW_RESET_VIEW_2D, 'V', menuCode | shiftCode, "Reset the 2D slice view and position to the defaults");

            menubar.add(menu);
        }
        {
            JMenu menu = new JMenu("Render");
            menu.getPopupMenu().setLightWeightPopupEnabled(false);

            this.addMenuItem(menu, Constants.DATA_AUTO_MIN_MAX, 'R', menuCode, "Assign the min and max intensities of the selected volume to the scalar colormap (the default range is zero to one)");
            this.addMenuItem(menu, Constants.VIEW_CHAN_NEXT, 'N', menuCode | shiftCode, "Increment the rendered channel of the selected volume");
            this.addMenuItem(menu, Constants.VIEW_CHAN_PREV, 'P', menuCode | shiftCode, "Decrement the rendered channel of the selected volume");
            menu.addSeparator();
            {
                JMenuItem item = new JMenuItem(Constants.SETTINGS_COLORMAPS_SOLID);
                item.addActionListener(e ->
                {
                    this.state.control.setStatusMessage("showing solid colormap controls");
                    this.state.colormaps.showSolid();
                });

                item.setAccelerator(KeyStroke.getKeyStroke('J', menuCode));
                item.setToolTipText("open a window for editing and exploring the solid colormaps");

                menu.add(item);
            }
            {
                JMenuItem item = new JMenuItem(Constants.SETTINGS_COLORMAPS_DISCRETE);
                item.addActionListener(e ->
                {
                    this.state.control.setStatusMessage("showing discrete colormap controls");
                    this.state.colormaps.showDiscrete();
                });

                item.setAccelerator(KeyStroke.getKeyStroke('K', menuCode));
                item.setToolTipText("open a window for editing and exploring the discrete colormaps");

                menu.add(item);
            }
            {
                JMenuItem item = new JMenuItem(Constants.SETTINGS_COLORMAPS_SCALAR);
                item.addActionListener(e ->
                {
                    this.state.control.setStatusMessage("showing scalar colormap controls");
                    this.state.colormaps.showScalar();
                });

                item.setAccelerator(KeyStroke.getKeyStroke('L', menuCode));
                item.setToolTipText("open a window for editing and exploring the scalar colormaps");

                menu.add(item);
            }
            menu.addSeparator();
            {
                Consumer<Pair<String, Function<JDialog,JPanel>>> panelItem = (p) ->
                {
                    JDialog dialog = new JDialog(this.frame, p.a);
                    JPanel panel = new JPanel();
                    panel.setLayout(new VerticalLayout(5, VerticalLayout.LEFT));
                    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                    panel.add(p.b.apply(dialog));

                    dialog.add(new JScrollPane(panel));
                    dialog.pack();
                    dialog.setLocationRelativeTo(frame);
                    dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
                    dialog.setResizable(true);
                    dialog.setVisible(false);

                    SwingUtils.addEscapeListener(dialog);
                    JMenuItem item = new JMenuItem(p.a);
                    item.setToolTipText("open a window for changing the advanced settings");
                    item.addActionListener(e ->
                    {
                        this.state.control.setStatusMessage("showing settings");
                        dialog.pack();
                        dialog.setLocationRelativeTo(this.frame);
                        dialog.setVisible(true);
                    });

                    menu.add(item);
                };

                panelItem.accept(Pair.of(Constants.SETTINGS_RENDERING, (d) -> this.initRenderingPanel(d)));
                panelItem.accept(Pair.of(Constants.SETTINGS_ANNOTATION, (d) -> this.state.annotation.getPanel()));
                panelItem.accept(Pair.of(Constants.SETTINGS_REFERENCE, (d) -> this.initReferencePanel(d)));
                panelItem.accept(Pair.of(Constants.SETTINGS_INTERACTION, (d) -> this.initInteractionPanel()));
                panelItem.accept(Pair.of(Constants.SETTINGS_DATA, (d) -> this.initDataPanel()));

                menubar.add(menu);
            }

            menubar.add(menu);
        }

        {
            // disable info messages
            Reflections reflections = new Reflections("qit");
            final Map<String, Class<? extends Module>> unlistedModules = ModuleUtils.unlistedClasses();
            final Map<String, Class<? extends Module>> listedModules = ModuleUtils.listedClasses();

            Map<String, JMenu> subs = Maps.newLinkedHashMap();
            subs.put("Solids", new JMenu("Solids..."));
            subs.put("Vects", new JMenu("Vects..."));
            subs.put("Table", new JMenu("Table..."));
            subs.put("Matrix", new JMenu("Matrix..."));
            subs.put("Affine", new JMenu("Affine..."));
            subs.put("Mesh", new JMenu("Mesh..."));
            subs.put("Curves", new JMenu("Curves..."));
            subs.put("Volume", new JMenu("Volume..."));
            subs.put("Mask", new JMenu("Mask..."));
            subs.put("Neuron", new JMenu("Neuron..."));

            Map<String, JMenu> dsubs = Maps.newLinkedHashMap();
            dsubs.put("Dwi", new JMenu("Dwi..."));
            dsubs.put("Gradients", new JMenu("Gradients..."));
            dsubs.put("Model", new JMenu("Model..."));
            dsubs.put("Tensor", new JMenu("Tensor..."));
            dsubs.put("Fibers", new JMenu("Fibers..."));
            dsubs.put("Spharm", new JMenu("Spharm..."));
            dsubs.put("Odf", new JMenu("Odf..."));
            dsubs.put("Kurtosis", new JMenu("Kurtosis..."));
            dsubs.put("Noddi", new JMenu("Noddi..."));
            dsubs.put("ExpDecay", new JMenu("ExpDecay..."));
            dsubs.put("ExpRecovery", new JMenu("ExpRecovery..."));

            JMenu mri = new JMenu("MRI...");
            JMenu other = new JMenu("Other...");

            for (String key : subs.keySet())
            {
                subs.get(key).getPopupMenu().setLightWeightPopupEnabled(false);
            }
            mri.getPopupMenu().setLightWeightPopupEnabled(false);
            other.getPopupMenu().setLightWeightPopupEnabled(false);

            final List<String> keys = Lists.newArrayList(listedModules.keySet());
            Collections.sort(keys);

            for (final String key : keys)
            {
                final Class<? extends Module> clas = listedModules.get(key);
                JMenuItem item = new JMenuItem(key);
                ModuleDescription desc = clas.getAnnotation(ModuleDescription.class);
                if (desc != null)
                {
                    item.setToolTipText(desc.value());
                }
                item.addActionListener(e ->
                {
                    try
                    {
                        Logging.info("loading module: " + key);
                        this.state.modules.add(clas.newInstance());
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                        Logging.error("failed to load option: " + ex.getMessage());
                    }

                });

                String first = key.split("(?<=.)(?=\\p{Lu})")[0];

                boolean found = false;
                for (String dsub : dsubs.keySet())
                {
                    if (key.startsWith(dsub) || key.startsWith("Volume" + dsub))
                    {
                        dsubs.get(dsub).add(item);
                        found = true;
                        break;
                    }
                }

                if (!found)
                {
                    if (subs.containsKey(first))
                    {
                        subs.get(first).add(item);
                    }
                    else
                    {
                        other.add(item);
                    }
                }
            }

            JMenu menu = new JMenu("Data");
            menu.getPopupMenu().setLightWeightPopupEnabled(false);

            this.addMenuItem(menu, Constants.SETTINGS_PROCESSES, 'W', menuCode, "open the process manager");
            menu.addSeparator();
            this.addMenuItem(menu, Constants.SETTINGS_MODULES, 'M', menuCode, "open the module manager");

            {
                JMenuItem search = new JMenuItem("Search Modules...");
                search.setToolTipText("search for a module by pattern matching the module name");
                search.setAccelerator(KeyStroke.getKeyStroke('F', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

                search.addActionListener(e ->
                {
                    try
                    {
                        SuggestionPanel suggest = new SuggestionPanel("Enter a search term:", keys);
                        int code = JOptionPane.showConfirmDialog(this.state.gui.getFrame(), suggest, "Search by name...", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

                        if (code == JOptionPane.OK_OPTION)
                        {
                            String key = suggest.getText();
                            Logging.info("loading module: " + key);

                            boolean found = false;
                            {
                                // first look for exact matches
                                for (String listedModule : listedModules.keySet())
                                {
                                    if (listedModule.equals(key))
                                    {
                                        final Class<? extends Module> clas = listedModules.get(listedModule);
                                        this.state.modules.add(clas.newInstance());
                                        found = true;
                                        break;
                                    }
                                }
                            }

                            if (!found)
                            {
                                for (String unlistedModule : unlistedModules.keySet())
                                {
                                    if (unlistedModule.contains(key))
                                    {
                                        final Class<? extends Module> clas = unlistedModules.get(unlistedModule);
                                        this.state.modules.add(clas.newInstance());
                                        found = true;
                                        break;
                                    }
                                }
                            }

                            if (!found)
                            {
                                Logging.info("module not found: " + key);
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                        Logging.error("failed to load module: " + ex.getMessage());
                    }

                });
                menu.add(search);
            }
            {
                JMenu browse = new JMenu("Browse Modules...");
                browse.getPopupMenu().setLightWeightPopupEnabled(false);
                for (String sub : subs.keySet())
                {
                    browse.add(subs.get(sub));
                }
                browse.add(mri);
                for (String dsub : dsubs.keySet())
                {
                    mri.add(dsubs.get(dsub));
                }
                browse.add(other);

                menu.add(browse);
            }

            menu.addSeparator();
            {
                JMenu submenu = new JMenu("Create...");
                this.addMenuItem(submenu, Constants.DATA_CREATE_MASK, ',', menuCode, "create a mask based on a selected volume (the mask will have the same voxel dimensions as the selected data)");
                this.addMenuItem(submenu, Constants.DATA_CREATE_SPHERE, '.', menuCode, "create a sphere solid based on a selected data object (the sphere will enclose the selected data)");
                this.addMenuItem(submenu, Constants.DATA_CREATE_BOX, '/', menuCode, "create a box solid based on a selected data object (the box will enclose the selected data)");
                this.addMenuItem(submenu, Constants.DATA_CREATE_VECTS, ';', menuCode, "create an empty vects object (useful for manually placing landmarks)");
                menu.add(submenu);
            }
            {
                JMenu submenu = new JMenu("Convert...");
                this.addMenuItem(submenu, Constants.DATA_VOLUME_TO_MASK, '\\', menuCode, "Convert the selected volumes to masks (useful for when you accidentally load a mask as a volume)");
                this.addMenuItem(submenu, Constants.DATA_MASK_TO_VOLUME, '\\', menuCode | shiftCode, "Convert the selected masks to volumes (useful for when you accidentally load a volume as a mask)");
                menu.add(submenu);
            }
            {
                JMenu submenu = new JMenu("Delete...");
                this.addMenuItem(submenu, Constants.DATA_DELETE_SELECTION, 'D', menuCode, "Delete the selected data objects (useful for tidying up)");
                this.addMenuItem(submenu, Constants.DATA_DELETE_ALL, 'D', menuCode | shiftCode, "Delete all data objects (useful for getting a clean slate)");
                submenu.addSeparator();
                menu.add(submenu);
            }
            {
                JMenu submenu = new JMenu("Advanced...");
                this.addMenuItem(submenu, Constants.DATA_DELETE_FILENAMES, "Delete the filenames associated with the selected data objects (advanced feature with potential data loss)");
                this.addMenuItem(submenu, Constants.DATA_DELETE_VALUE, "Delete the data associated with the selected data objects (advanced feature with potential data loss)");
                this.addMenuItem(submenu, Constants.DATA_DELETE_INTERACTION, "Delete the record of interactions with the keyboard and mouse (useful for debugging)");
                this.addMenuItem(submenu, Constants.DATA_SORT_FACES, 'Y', menuCode, "Sort the faces of all meshes (greatly improves quality of rendering non-convex meshes with transparency)");
                menu.add(submenu);
            }
            if (Global.getExpert())
            {
                menu.addSeparator();
                this.addMenuItem(menu, Constants.SETTINGS_REPL, "open the Python command line (a read-eval-print loop for interactive data analysis)");
                {
                    JMenuItem export = new JMenuItem("Export module library to LONI Pipeline....");
                    export.setToolTipText("create a library of pipeline modules in a specified directory");
                    export.addActionListener(e ->
                    {
                        try
                        {
                            String dn = this.state.gui.chooseSaveFile("Choose a directory for the library...", "my.library");
                            ModuleUtils.writePipelines(dn);
                            this.state.control.setStatusMessage("created library of remote pipeline modules: " + dn);
                        }
                        catch (Exception ex)
                        {
                            ex.printStackTrace();
                            this.state.control.setStatusError("failed to library module: " + ex.getMessage());
                        }
                    });
                    menu.add(export);
                }
            }

            menubar.add(menu);
        }

        {
            JMenu menu = new JMenu("Help");
            menu.getPopupMenu().setLightWeightPopupEnabled(false);

            {
                this.addMenuItem(menu, Constants.HELP_ABOUT, "tell me about this package", e ->
                {
                    this.state.control.setStatusMessage("showing about page");
                    SwingUtils.showMessage(Constants.ABOUT_MSG);
                });
            }
            {
                this.addMenuItem(menu, Constants.HELP_CITE, "tell me how to cite this software in publications", e ->
                {
                    this.state.control.setStatusMessage("showing citation information");
                    SwingUtils.showMessageScroll(Global.getCitation());
                });
            }
            {
                this.addMenuItem(menu, Constants.HELP_BUILD, "tell me about the build version", e ->
                {
                    this.state.control.setStatusMessage("showing build information page");
                    SwingUtils.showMessageScroll(Global.getBuildInfo());
                });
            }
            {
                this.addMenuItem(menu, Constants.HELP_LICENSE, "show me the software license", e ->
                {
                    this.state.control.setStatusMessage("showing software license");
                    SwingUtils.showMessageScroll(Global.getLicense());
                });
            }
            {
                this.addMenuItem(menu, Constants.HELP_MANUAL, "open the QIT user manual/wiki in an external web browser (requires network connection)", e ->
                {
                    this.state.control.setStatusMessage("showing user manual");
                    try
                    {
                        if (Desktop.isDesktopSupported())
                        {
                            Desktop.getDesktop().browse(new URI(Constants.MANUAL_URI));
                        }
                    }
                    catch (Exception ex)
                    {
                        SwingUtils.showMessage("A user manual can be found at: " + Constants.MANUAL_URI);
                    }
                });
            }
            this.addMenuItem(menu, Constants.SETTINGS_DIAGNOSTICS, '\'', menuCode, "Print QIT diagnostics to the console and messaging window");

            menubar.add(menu);
        }

        return menubar;
    }

    private void initView()
    {
        try
        {
            String syslaf = UIManager.getSystemLookAndFeelClassName();
            String cplaf = UIManager.getCrossPlatformLookAndFeelClassName();
            String laf = this.state.osname.contains("linux") ? cplaf : syslaf;
            Logging.info("using look and feel: " + laf);
            UIManager.setLookAndFeel(laf);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            this.state.control.setStatusError("warning: failed to set look and feel");
        }

        this.chooserLoad = new JFileChooser();
        this.chooserLoad.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        this.chooserLoad.setCurrentDirectory(new File(System.getProperty("user.dir")));
        this.chooserLoad.setMultiSelectionEnabled(true);

        this.chooserSave = new JFileChooser();
        this.chooserSave.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        this.chooserSave.setCurrentDirectory(new File(System.getProperty("user.dir")));

        this.canvas.setPreferredSize(new Dimension(Constants.WIDTH, Constants.HEIGHT));
        this.canvas.setMinimumSize(new Dimension(1, 1));

        this.canvas.addKeyListener(this.state.control);

        this.frame.addWindowFocusListener(new WindowAdapter()
        {
            public void windowGainedFocus(WindowEvent e)
            {
                Interface.this.focus();
            }
        });

        this.frame.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                Interface.this.close();
            }
        });

        this.focus();
    }

    private void focus()
    {
        Interface.this.canvas.requestFocusInWindow();
    }

    private JPanel initReferencePanel(JDialog parent)
    {
        ControlPanel controls = new ControlPanel();
        {
            JCheckBox elem = new JCheckBox();
            elem.addItemListener(e ->
            {
                this.state.settings.frameVisible = e.getStateChange() == ItemEvent.SELECTED;
                this.state.control.setStatusMessage("updated coordinate frame visibility: " + this.state.settings.frameVisible);
            });
            controls.addControl("Show Coordinate Frame", elem);
        }
        {
            JCheckBox elem = new JCheckBox();
            elem.addItemListener(e ->
            {
                this.state.settings.boxVisible = e.getStateChange() == ItemEvent.SELECTED;
                this.state.control.setStatusMessage("updated bounding box visibility: " + this.state.settings.boxVisible);
            });
            controls.addControl("Show Bounding Box", elem);
        }
        {
            JCheckBox elem = new JCheckBox();
            elem.setSelected(this.state.settings.anatomical);
            elem.addItemListener(e ->
            {
                this.state.settings.anatomical = e.getStateChange() == ItemEvent.SELECTED;
                this.state.control.setStatusMessage("updated anatomical: " + this.state.settings.anatomical);
            });
            controls.addControl("Show Anatomical Labels", elem);
        }
        {
            final BasicSpinner elem = new BasicSpinner(new SpinnerNumberModel(this.state.settings.lineWidth, 1, 15, 1));
            elem.addChangeListener(e ->
            {
                this.state.settings.lineWidth = Integer.valueOf(elem.getValue().toString());
                this.state.control.setStatusMessage("updated bounding box width: " + this.state.settings.lineWidth);
            });
            controls.addControl("Bound Box Line Width", elem);
        }
        {
            BasicButton elem = new BasicButton("Set Bounding Box Color");
            elem.addActionListener(e ->
            {
                Color color = SwingUtils.chooseColor(parent, this.state.control.getBoxColor());
                this.state.control.setBoxColor(color);
                this.state.control.setStatusMessage("updated bounding box color");
            });
            controls.addControl(elem);
        }
        {
            JCheckBox elem = new JCheckBox();
            elem.addItemListener(e ->
            {
                this.state.settings.scaleVisible = e.getStateChange() == ItemEvent.SELECTED;
                this.state.control.setStatusMessage("updated scaleCamera visibility: " + this.state.settings.scaleVisible);
            });
            controls.addControl("Show Scale", elem);
        }
        {
            JCheckBox elem = new JCheckBox();
            elem.addItemListener(e ->
            {
                this.state.settings.scaleBoxVisible = e.getStateChange() == ItemEvent.SELECTED;
                this.state.control.setStatusMessage("updated scaleCamera box visibility: " + this.state.settings.scaleBoxVisible);
            });
            controls.addControl("Show Scale Box", elem);
        }
        {
            final BasicSpinner elem = new BasicSpinner(new SpinnerNumberModel(this.state.settings.scaleWidth, 1, 15, 1));
            elem.addChangeListener(e ->
            {
                this.state.settings.scaleWidth = Integer.valueOf(elem.getValue().toString());
                this.state.control.setStatusMessage("updated scaleCamera width: " + this.state.settings.scaleWidth);
            });
            controls.addControl("Scale Line Width", elem);
        }
        {
            final BasicSpinner elem = new BasicSpinner(new SpinnerNumberModel(this.state.settings.scaleValue, 0, 50, 1));
            elem.addChangeListener(e ->
            {
                this.state.settings.scaleValue = Float.valueOf(elem.getValue().toString());
                this.state.control.setStatusMessage("updated scaleCamera value: " + this.state.settings.scaleValue);
            });
            controls.addControl("Scale Spacing", elem);
        }
        {
            BasicButton elem = new BasicButton("Set Scale Color");
            elem.addActionListener(e ->
            {
                Color color = SwingUtils.chooseColor(parent, this.state.control.getScaleColor());
                this.state.control.setScaleColor(color);
                this.state.control.setStatusMessage("updated scaleCamera color");
            });
            controls.addControl(elem);
        }

        JPanel panel = new JPanel();
        panel.add(controls);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        return panel;
    }

    private JPanel initInteractionPanel()
    {
        ControlPanel controls = new ControlPanel();
        {
            final BasicSpinner elem = new BasicSpinner(new SpinnerNumberModel(this.state.settings.xrotAuto, -50, 50, 0.001f));
            ((BasicSpinner.DefaultEditor) elem.getEditor()).getTextField().setColumns(6);
            elem.addChangeListener(e ->
            {
                this.state.settings.xrotAuto = Double.valueOf(elem.getValue().toString());
                this.state.control.setStatusMessage("updated x cameraRot auto: " + this.state.settings.xrotAuto);
            });
            controls.addControl("X Rotation Auto", elem);
        }
        {
            final BasicSpinner elem = new BasicSpinner(new SpinnerNumberModel(this.state.settings.yrotAuto, -50, 50, 0.001f));
            elem.addChangeListener(e ->
            {
                this.state.settings.yrotAuto = Double.valueOf(elem.getValue().toString());
                this.state.control.setStatusMessage("updated y cameraRot auto: " + this.state.settings.yrotAuto);
            });
            controls.addControl("Y Rotation Auto", elem);
        }
        {
            final BasicSpinner elem = new BasicSpinner(new SpinnerNumberModel(this.state.settings.xposMouse, -50, 50, 0.0001));
            elem.addChangeListener(e ->
            {
                this.state.settings.xposMouse = Double.valueOf(elem.getValue().toString());
                this.state.control.setStatusMessage("updated x trans mouse: " + this.state.settings.xposMouse);
            });
            controls.addControl("X Translation Mouse", elem);
        }
        {
            final BasicSpinner elem = new BasicSpinner(new SpinnerNumberModel(this.state.settings.yposMouse, -50, 50, 0.0001));
            elem.addChangeListener(e ->
            {
                this.state.settings.yposMouse = Double.valueOf(elem.getValue().toString());
                this.state.control.setStatusMessage("updated y trans mouse: " + this.state.settings.yposMouse);
            });
            controls.addControl("Y Translation Mouse", elem);
        }
        {
            final BasicSpinner elem = new BasicSpinner(new SpinnerNumberModel(this.state.settings.zposMouse, -50, 50, 0.0001));
            elem.addChangeListener(e ->
            {
                this.state.settings.zposMouse = Double.valueOf(elem.getValue().toString());
                this.state.control.setStatusMessage("updated z trans mouse: " + this.state.settings.zposMouse);
            });
            controls.addControl("Z Translation Mouse", elem);
        }
        {
            final BasicSpinner elem = new BasicSpinner(new SpinnerNumberModel(this.state.settings.scaleMouse, -50, 50, 0.00075f));
            elem.addChangeListener(e ->
            {
                this.state.settings.scaleMouse = Double.valueOf(elem.getValue().toString());
                this.state.control.setStatusMessage("updated scaleCamera mouse: " + this.state.settings.scaleMouse);
            });
            controls.addControl("Scale Mouse", elem);
        }
        {
            final BasicSpinner elem = new BasicSpinner(new SpinnerNumberModel(this.state.settings.xrotMouse, -50, 50, 0.001f));
            elem.addChangeListener(e ->
            {
                this.state.settings.xrotMouse = Double.valueOf(elem.getValue().toString());
                this.state.control.setStatusMessage("updated x cameraRot mouse: " + this.state.settings.xrotMouse);
            });
            controls.addControl("X Rotation Mouse", elem);
        }
        {
            final BasicSpinner elem = new BasicSpinner(new SpinnerNumberModel(this.state.settings.yrotMouse, -50, 50, 0.001f));
            elem.addChangeListener(e ->
            {
                this.state.settings.yrotMouse = Double.valueOf(elem.getValue().toString());
                this.state.control.setStatusMessage("updated y cameraRot mouse: " + this.state.settings.yrotMouse);
            });
            controls.addControl("Y Rotation Mouse", elem);
        }
        {
            BasicButton elem = new BasicButton("Reset");
            elem.addActionListener(e ->
            {
                this.state.control.setStatusMessage("reseting interaction parameters");
                this.state.control.resetInteraction();
            });
            controls.addControl(elem);
        }

        JPanel panel = new JPanel();
        panel.add(controls);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        return panel;
    }

    private JPanel initRenderingPanel(JDialog parent)
    {
        ControlPanel controls = new ControlPanel();
        {
            JCheckBox elem = new JCheckBox();
            elem.setToolTipText("use orthographic rendering to remove perspective from rendering");
            elem.addItemListener(e ->
            {
                this.state.settings.orthoView = e.getStateChange() == ItemEvent.SELECTED;
                this.state.control.setStatusMessage("updated ortho view: " + this.state.settings.orthoView);
            });
            controls.addControl("Orthographic", elem);
        }
        {
            JCheckBox elem = new JCheckBox();
            elem.setToolTipText("show the frames per second");
            elem.addItemListener(e ->
            {
                this.canvas.showFPS = e.getStateChange() == ItemEvent.SELECTED;
                this.state.control.setStatusMessage("updated showFPS to: " + this.canvas.showFPS);
            });
            controls.addControl("Show FPS", elem);
        }
        {
            JCheckBox elem = new JCheckBox();
            elem.setToolTipText("enable automatic sorting of faces whenever the mouse button is released (this can be expensive for large meshes)");
            elem.addItemListener(e ->
            {
                this.state.settings.autoSort = e.getStateChange() == ItemEvent.SELECTED;
                this.state.control.setStatusMessage("updated auto sort faces to: " + this.state.settings.autoSort);
            });
            controls.addControl("Auto Sort Faces", elem);
        }
        {
            final BasicSpinner elem = new BasicSpinner(new SpinnerNumberModel(this.state.scaling, 0.25, 4.0, 0.25));
            elem.addChangeListener(e ->
            {
                double value = Double.valueOf(elem.getValue().toString());
                this.state.scaling = value;
                this.state.control.setStatusMessage("updated scaling: " + value);
            });
            controls.addControl("Scaling Factor", elem);
        }
        {
            final BasicSpinner elem = new BasicSpinner(new SpinnerNumberModel(this.state.settings.fov, 5, 90, 1));
            elem.addChangeListener(e ->
            {
                float value = Float.valueOf(elem.getValue().toString());
                this.state.settings.fov = value;
                this.state.control.setStatusMessage("updated fov lighting: " + value);
            });
            controls.addControl("Field of View (deg)", elem);
        }
        {
            final BasicSpinner elem = new BasicSpinner(new SpinnerNumberModel(this.state.settings.diffuseLight, 0, 1000, 0.01));
            elem.addChangeListener(e ->
            {
                float value = Float.valueOf(elem.getValue().toString());
                this.state.settings.diffuseLight = value;
                this.state.control.setStatusMessage("updated diffuse lighting: " + value);
            });
            controls.addControl("Diffuse Lighting", elem);
        }
        {
            final BasicSpinner elem = new BasicSpinner(new SpinnerNumberModel(this.state.settings.ambientLight, 0, 1, 0.01));

            elem.addChangeListener(e ->
            {
                float value = Float.valueOf(elem.getValue().toString());
                this.state.settings.ambientLight = value;
                this.state.control.setStatusMessage("updated ambient lighting: " + value);
            });
            controls.addControl("Ambient Lighting", elem);
        }
        {
            final BasicSpinner elem = new BasicSpinner(new SpinnerNumberModel(this.state.settings.specularLight, 0, 1, 0.01));
            elem.addChangeListener(e ->
            {
                float value = Float.valueOf(elem.getValue().toString());
                this.state.settings.specularLight = value;
                this.state.control.setStatusMessage("updated specular lighting: " + value);
            });
            controls.addControl("Specular Lighting", elem);
        }
        {
            JCheckBox elem = new JCheckBox();
            elem.setToolTipText("create screenshots with a transparent background");
            elem.addItemListener(e ->
            {
                this.state.settings.transparent = e.getStateChange() == ItemEvent.SELECTED;
                this.state.control.setStatusMessage("updated transparent to: " + this.state.settings.transparent);
            });
            controls.addControl("Transparent", elem);
        }
        {
            BasicButton elem = new BasicButton(Constants.SETTINGS_BG_SET);
            elem.addActionListener(e ->
            {
                Color color = SwingUtils.chooseColor(parent, this.state.control.getBackgroundColor());
                this.state.control.setBackgroundColor(color);
                this.state.control.setStatusMessage("updated background color");
            });
            controls.addControl(elem);
        }
        {
            BasicButton elem = new BasicButton(Constants.SETTINGS_BG_RESET);
            elem.addActionListener(e ->
            {
                this.state.control.setBackgroundColor(Constants.BG_COLOR_DEFAULT);
                this.state.control.setStatusMessage("reset background color");
            });
            controls.addControl(elem);
        }

        JPanel panel = new JPanel();
        panel.add(controls);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        return panel;
    }


    private JPanel initDataPanel()
    {
        ControlPanel controls = new ControlPanel();
        {
            JCheckBox elem = new JCheckBox();
            elem.setSelected(this.state.settings.clobber);
            elem.setToolTipText("checking this will ignore warnings about overwriting existing files");
            elem.addItemListener(e ->
            {
                this.state.settings.clobber = e.getStateChange() == ItemEvent.SELECTED;
                this.state.control.setStatusMessage("updated clobber: " + this.state.settings.clobber);
            });
            controls.addControl("Clobber", elem);
        }
        {
            JCheckBox elem = new JCheckBox();
            elem.setSelected(this.state.settings.backup);
            elem.setToolTipText("checking this will backup existing files before overwritting");
            elem.addItemListener(e ->
            {
                this.state.settings.backup = e.getStateChange() == ItemEvent.SELECTED;
                this.state.control.setStatusMessage("updated backup: " + this.state.settings.backup);
            });
            controls.addControl("Backup", elem);
        }
        {
            BasicTextField elem = new BasicTextField(Global.PIPELINE_HOST);
            elem.setToolTipText("Specify the hostname when exporting pipeline modules");
            elem.addPropertyChangeListener("value", e ->
            {
                Global.PIPELINE_HOST = elem.getText();
            });
            controls.addControl("Pipeline hostname", elem);
        }
        {
            BasicTextField elem = new BasicTextField(Global.PIPELINE_PATH);
            elem.setToolTipText("Specify the path to QIT used when exporting pipeline modules");
            elem.addPropertyChangeListener("value", e ->
            {
                Global.PIPELINE_PATH = elem.getText();
            });
            controls.addControl("Pipeline path", elem);
        }

        JPanel panel = new JPanel();
        panel.add(controls);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        return panel;
    }
}