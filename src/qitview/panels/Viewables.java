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
 * setout the express permission of the Author. The permission of
 * the Author is not required if the said reproduction, modification,
 * transmission or transference is done setout financial return, the
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
 * Software or any derivative of it for research set the final aim of
 * developing software products for sale or license to a third party or
 * (3) use of the Software or any derivative of it for research set the
 * final aim of developing non-software products for sale or license to a
 * third party, or (4) use of the Software to provide any service to an
 * external organisation for which payment is received.
 *
 ******************************************************************************/


package qitview.panels;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import qit.base.Dataset;
import qit.base.Global;
import qit.base.Logging;
import qit.base.structs.Named;
import qit.base.structs.ObservableInstance;
import qit.base.structs.Pair;
import qit.base.utils.JavaUtils;
import qit.base.utils.PathUtils;
import qit.data.datasets.Affine;
import qit.data.datasets.Curves;
import qit.data.datasets.Mask;
import qit.data.datasets.Mesh;
import qit.data.datasets.Solids;
import qit.data.datasets.Table;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.utils.mri.structs.Gradients;
import qit.math.structs.Box;
import qitview.main.*;
import qitview.models.Viewable;
import qitview.models.ViewableAction;
import qitview.models.ViewableActions;
import qitview.models.ViewableType;
import qitview.views.AffineView;
import qitview.views.CurvesView;
import qitview.views.GradientsView;
import qitview.views.MaskView;
import qitview.views.MeshView;
import qitview.views.SolidsView;
import qitview.views.TableView;
import qitview.views.VectsView;
import qitview.views.VolumeView;
import qitview.widgets.BasicButton;
import qitview.widgets.BasicComboBox;
import qitview.widgets.BasicTable;
import qitview.widgets.FileLoader;
import qitview.widgets.FileSaver;
import qitview.widgets.RowTableModel;
import qitview.widgets.SwingUtils;

@SuppressWarnings("unchecked")
public class Viewables implements Iterable<Viewable<?>>
{
    private final static Integer CB_WIDTH = 20;

    // make these wide to make things legible
    public final static Named<Viewable<?>> NONE = new Named<>("None              ", null);
    public final static Named<Viewable<?>> NEW = new Named<>("New...            ", null);
    public final static Named<Viewable<?>> NEW_NAMED = new Named<>("New named...      ", null);

    private RenderTableModel model;
    private BasicTable table;
    private List<ComboRecord> combos = Lists.newArrayList();
    private String sceneDirname = null;

    private ObservableInstance observableData = new ObservableInstance();
    private ObservableInstance observableSelection = new ObservableInstance();

    public Viewables()
    {
        this.model = new RenderTableModel();
        this.table = new BasicTable(this.model);

        this.table.setFocusable(false);
        this.table.setCellSelectionEnabled(true);
        this.table.setPreferredScrollableViewportSize(new Dimension(300, 70));
        this.table.setFillsViewportHeight(true);
        this.table.getColumnModel().getColumn(0).setMinWidth(CB_WIDTH);
        this.table.getColumnModel().getColumn(0).setMaxWidth(CB_WIDTH);
        this.table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        this.table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.table.getSelectionModel().addListSelectionListener(event ->
        {
            List<Integer> selected = Viewables.this.getSelectionIndex();
            Collections.sort(selected);

            if (selected.size() > 0)
            {
                int idx = selected.get(0);
                Viewable entry = Viewables.this.model.getRow(idx);
                Viewer.getInstance().gui.setSelection(entry);

                Logging.info(entry.getName() + " is primary selection");
            }
        });

        {
            final JPopupMenu menu = new JPopupMenu();
            final JMenu process = new JMenu("Process Data");

            Runnable rebuildMenu = () ->
            {
                menu.removeAll();
                process.removeAll();

                BiConsumer<String, Runnable> addItem = (text, action) ->
                {
                    JMenuItem item = new JMenuItem(text);
                    item.addActionListener(e -> action.run());
                    menu.add(item);
                };

                addItem.accept("Get Info...", () ->
                {
                    Viewables.this.getFirstSelectionIndex().ifPresent((idx) ->
                    {
                        String name = this.getName(idx);
                        Viewable view = this.getViewable(idx);

                        BasicButton close = new BasicButton("Close");

                        JPanel buttonPanel = new JPanel();
                        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
                        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
                        buttonPanel.add(javax.swing.Box.createHorizontalGlue());
                        buttonPanel.add(close);

                        JPanel edit = new JPanel();
                        edit.setLayout(new BorderLayout());
                        edit.add(view.getInfoPanel(), BorderLayout.CENTER);
                        edit.add(buttonPanel, BorderLayout.SOUTH);

                        final JDialog dialog = new JDialog(Viewer.getInstance().gui.getFrame(), "Info: " + name);
                        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                        dialog.add(edit, BorderLayout.NORTH);
                        dialog.setSize(300, 300);
                        dialog.pack();
                        dialog.setVisible(true);

                        close.addActionListener(e13 -> dialog.dispose());
                    });
                });
                getFirstSelectionViewable().ifPresent(viewable ->
                {
                    if (ViewableType.getFromView(viewable).equals(ViewableType.Table))
                    {
                        addItem.accept("Show Table Editor...", () -> ((TableView) viewable).showEditor());
                    }
                });
                menu.addSeparator();
                menu.add(process);
                BiConsumer<String, Runnable> addItemProcess = (text, action) ->
                {
                    JMenuItem item = new JMenuItem(text);
                    item.addActionListener(e -> action.run());
                    process.add(item);
                };

                getFirstSelectionViewable().ifPresent(viewable ->
                {
                    switch (ViewableType.getFromView(viewable))
                    {
                        case Volume:
                            addItemProcess.accept("Replace Volume with Mask", () -> Viewer.getInstance().control.volumeToMask());
                            break;
                        case Mask:
                            addItemProcess.accept("Replace Mask with Volume", () -> Viewer.getInstance().control.maskToVolume());
                            break;
                    }

                    for (ViewableAction action : ViewableActions.getList(viewable))
                    {
                        if (action.replacable)
                        {
                            addItemProcess.accept(action.name + " and Export", () -> action.getAction(false).accept(viewable));
                            addItemProcess.accept(action.name + " and Replace", () -> action.getAction(true).accept(viewable));
                        }
                        else
                        {
                            addItemProcess.accept(action.name, () -> action.getAction(false).accept(viewable));
                        }
                    }
                });
                getFirstSelectionViewable().ifPresent(viewable ->
                {
                    if (ViewableType.getFromView(viewable).equals(ViewableType.Volume))
                    {
                        addItem.accept("Harmonize Colormaps", () -> this.harmonizeSelectedScalarColormaps());
                    }
                });
                menu.addSeparator();
                addItem.accept("Create New Sphere", () -> Viewer.getInstance().control.createSphere());
                addItem.accept("Create New Box", () -> Viewer.getInstance().control.createBox());
                addItem.accept("Create New Mask", () -> Viewer.getInstance().control.createMask());
                menu.addSeparator();
                addItem.accept("Rename Selected Data", () ->
                {
                    for (int idx : Viewables.this.getSelectionIndex())
                    {
                        String name = Viewables.this.getName(idx);
                        String nname = SwingUtils.getString(Viewer.getInstance().gui.getFrame(), "Choose a new name", name);
                        Viewables.this.setName(idx, nname);
                    }
                });
                addItem.accept("Trim Selected Names", () -> this.trimSelectedNames());
                menu.addSeparator();
                addItem.accept("Move to top", () -> Viewables.this.moveTop());
                addItem.accept("Move to bottom", () -> Viewables.this.moveBottom());
                menu.addSeparator();
                addItem.accept("Zoom in on selected data", () -> Viewer.getInstance().gui.canvas.render3D.zoomDetail());
                addItem.accept("Zoom out for overview", () -> Viewer.getInstance().gui.canvas.render3D.zoomOverview());
                menu.addSeparator();
                addItem.accept("Show Selected Data", () -> Viewer.getInstance().control.setVisibility(true));
                addItem.accept("Hide Selected Data", () -> Viewer.getInstance().control.setVisibility(false));
                menu.addSeparator();
                addItem.accept("Load More Data...", () -> new FileLoader());
                menu.addSeparator();
                addItem.accept("Save Selected Data As...", () -> new FileSaver().withPrompt().run());
                addItem.accept("Save Selected Data...", () -> new FileSaver().run());
                menu.addSeparator();
                addItem.accept("Delete Selected Data", () -> SwingUtils.confirm("Are you sure you want to delete the selected items?", () -> Viewables.this.removeSelection()));
                addItem.accept("Delete All Data", () -> SwingUtils.confirm("Are you sure you want to delete everything?", () -> Viewables.this.removeAll()));
            };

            menu.addPopupMenuListener(new PopupMenuListener()
            {
                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent e)
                {
                    rebuildMenu.run();
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
                {
                }

                @Override
                public void popupMenuCanceled(PopupMenuEvent e)
                {
                }
            });
            this.table.setComponentPopupMenu(menu);
        }

        this.changedData();
    }

    public void changedSelection()
    {
        this.observableSelection.changed();
    }

    public void addObserverSelection(Observer o)
    {
        this.observableSelection.addObserver(o);
    }

    public void changedData()
    {
        this.observableData.changed();
    }

    public void addObserverData(Observer o)
    {
        this.observableData.addObserver(o);
    }

    public String getUniqueName(String base)
    {
        String name = base;

        if (name == null || name.length() == 0)
        {
            name = "Unnamed";
        }

        if (this.has(name))
        {
            int idx = 1;
            while (this.has(name + "-" + idx))
            {
                idx += 1;
            }
            name += "-" + idx;
        }

        return name;
    }

    public String getUniqueName(ViewableType type)
    {
        String base = "New-" + type.getText().replace('-', '.');

        int idx = 0;
        outer:
        while (true)
        {
            String name = idx == 0 ? base : base + "-" + idx;
            for (int vidx = 0; vidx < this.model.getRowCount(); vidx++)
            {
                Viewable entry = this.model.getRow(vidx);
                if (entry.getName().equals(name))
                {
                    idx += 1;
                    continue outer;
                }
            }

            return name;
        }
    }

    public void removeAll()
    {
        this.clearSelection();
        for (int vidx = 0; vidx < this.model.getRowCount(); vidx++)
        {
            this.model.getRow(vidx).clearData();
        }
        this.model.removeRowAll();

        Viewer.getInstance().gui.canvas.render3D.box = null;

        this.updateCombos();

        Runtime.getRuntime().gc();
        Runtime.getRuntime().gc();

        Viewer.getInstance().control.setStatusMessage("removed all data objects");

        this.changedData();
    }

    /**********
     * ACCESS *
     **********/

    public int size()
    {
        return this.model.getRowCount();
    }

    public Viewable<?> get(String name)
    {
        for (int vidx = 0; vidx < this.model.getRowCount(); vidx++)
        {
            Viewable v = this.model.getRow(vidx);
            if (v.getName().equals(name))
            {
                return v;
            }
        }

        return null;
    }

    public int index(String v)
    {
        for (int vidx = 0; vidx < this.model.getRowCount(); vidx++)
        {
            Viewable entry = this.model.getRow(vidx);
            if (entry.getName().equals(v))
            {
                return vidx;
            }
        }

        throw new RuntimeException("name not found: " + v);
    }

    public int index(Viewable<?> v)
    {
        for (int vidx = 0; vidx < this.model.getRowCount(); vidx++)
        {
            if (this.model.getRow(vidx) == v)
            {
                return vidx;
            }
        }

        throw new RuntimeException("viewable not found: " + v.toString());
    }

    public boolean has(String name)
    {
        for (int vidx = 0; vidx < this.model.getRowCount(); vidx++)
        {
            if (this.model.getRow(vidx).getName().equals(name))
            {
                return true;
            }
        }
        return false;
    }

    public boolean has(Viewable<?> viewable)
    {
        for (int vidx = 0; vidx < this.model.getRowCount(); vidx++)
        {
            if (this.model.getRow(vidx) == viewable)
            {
                return true;
            }
        }
        return false;
    }

    public boolean getVisibility(int idx)
    {
        return this.model.getRow(idx).getVisible();
    }

    public Named<Viewable<?>> get(int idx)
    {
        return Named.of(this.getName(idx), this.getViewable(idx));
    }

    public String getName(int idx)
    {
        return this.model.getRow(idx).getName();
    }

    public Viewable<?> getViewable(int idx)
    {
        return this.model.getRow(idx);
    }

    public void setVisibility(int idx, boolean v)
    {
        this.model.setValueAt(v, idx, 0);
    }

    public List<Viewable<?>> getAll()
    {
        List<Viewable<?>> out = Lists.newArrayList();
        for (int vidx = 0; vidx < this.model.getRowCount(); vidx++)
        {
            out.add(this.model.getRow(vidx));
        }
        return out;
    }

    public List<Viewable<?>> getVisible()
    {
        List<Viewable<?>> out = Lists.newArrayList();
        for (int vidx = 0; vidx < this.model.getRowCount(); vidx++)
        {
            Viewable entry = this.model.getRow(vidx);

            if (entry.getVisible())
            {
                out.add(entry);
            }
        }
        return out;
    }

    public void flipVisibility(int idx)
    {
        boolean nv = this.model.getRow(idx).getVisible() ^ true;
        this.setVisibility(idx, nv);
    }

    public void updateCombos(Class<?> clas)
    {
        for (ComboRecord record : new ArrayList<ComboRecord>(this.combos))
        {
            // only update combos set the matching type
            if (clas == null || record.type.getViewType().isAssignableFrom(clas))
            {
                Named<Viewable<?>> selected = (Named<Viewable<?>>) record.combo.getSelectedItem();
                record.combo.setModel(this.getComboModel(record.type, record.input, record.none));

                if (selected == NEW || selected == NEW_NAMED || selected == NONE || this.has(selected.getValue()))
                {
                    record.combo.setSelectedItem(selected);
                }
                else
                {
                    List<Integer> sidx = this.getSelectionIndex();
                    Collections.reverse(sidx);

                    for (int i : sidx)
                    {
                        Viewable<?> v = this.model.getRow(i);
                        if (!(record.input && !v.hasData()) && record.type.getViewType().isAssignableFrom(v.getClass()))
                        {
                            record.combo.setSelectedItem(v);
                            break;
                        }
                    }
                }
            }
        }
    }

    public void updateCombos(String older, String newer)
    {
        for (ComboRecord record : new ArrayList<>(this.combos))
        {
            record.combo.setModel(this.getComboModel(record.type, record.input, record.none));

            Named<Viewable<?>> selected = (Named<Viewable<?>>) record.combo.getSelectedItem();
            if (selected == NEW || selected == NEW_NAMED || selected == NONE || this.has(selected.getValue()))
            {
                record.combo.setSelectedItem(selected);
            }
            else if (selected.getName().equals(older))
            {
                int sidx = this.index(newer);

                Viewable<?> v = this.model.getRow(sidx);
                if (!(record.input && !v.hasData()) && record.type.getViewType().isAssignableFrom(v.getClass()))
                {
                    record.combo.setSelectedItem(v);
                    break;
                }
            }
        }
    }

    public void updateCombos()
    {
        this.updateCombos(null);
    }

    public void printInfo(int idx)
    {
        if (idx < 0 || idx >= this.model.getRowCount())
        {
            Viewer.getInstance().control.setStatusMessage("warning: invalid index: " + idx);
        }
        else
        {
            Viewable record = this.model.getRow(idx);
            Logging.info("Information for data object");
            Logging.info("    Index: " + idx);
            Logging.info("    Selected: " + this.getSelectionIndex().contains(idx));
            Logging.info("    Visibility: " + record.getVisible());
            Logging.info("    Name: " + record.getName());
            Logging.info("    Filename: " + record.getFilename());
            Logging.info("    Type: " + record.getClass().toString());
        }
    }

    public void printInfoAll()
    {
        for (int i = 0; i < this.model.getRowCount(); i++)
        {
            printInfo(i);
        }
    }

    /*************
     * SELECTION *
     *************/

    public void add(Viewable entry)
    {
        this.add(entry, false);
    }

    public void add(Viewable entry, boolean select)
    {
        entry.setName(getUniqueName(entry.getName()));

        this.model.addRow(entry);

        if (entry.hasBounds())
        {
            Box box = entry.getBounds();
            Render3D renderer = Viewer.getInstance().gui.canvas.render3D;
            renderer.box = renderer.box == null ? box : renderer.box.union(box);
        }

        if (this.getSelectionIndex().size() == 0 || select)
        {
            this.setSelection(this.size() - 1);
            this.table.updateUI();
            this.table.repaint();
        }

        this.updateCombos(entry.getClass());
        this.changedData();
    }

    public void remove(int idx)
    {
        this.model.removeRowRange(idx, idx);
        this.clearSelection();
        Viewer.getInstance().gui.clearSelection();

        this.updateCombos();
        this.changedData();
    }

    public void remove(List<Integer> selection)
    {
        Collections.sort(selection);
        Collections.reverse(selection);
        for (Integer idx : selection)
        {
            String name = this.getName(idx);
            this.model.removeRowRange(idx, idx);
            Viewer.getInstance().control.setStatusMessage("deleted: " + name);
        }
        this.clearSelection();
        Viewer.getInstance().gui.clearSelection();

        this.updateCombos();
        this.changedData();
    }

    public void removeSelection()
    {
        remove(this.getSelectionIndex());
        this.changedSelection();
    }

    public void harmonizeSelectedScalarColormaps()
    {
        this.getFirstSelectionViewable().ifPresent((reference) ->
        {
            if (reference instanceof VolumeView)
            {
                for (Viewable viewable : this.getSelectionViewable())
                {
                    if (viewable instanceof VolumeView)
                    {
                        ((VolumeView) viewable).setScalarColormap(((VolumeView) reference).getScalarColormapName());
                    }
                }
                Viewer.getInstance().control.setStatusMessage("matched scalar colormaps");
            }
        });
    }

    public void trimSelectedNames()
    {
        this.getFirstSelectionViewable().ifPresent((reference) ->
        {
            List<Integer> selection = this.getSelectionIndex();
            if (selection.size() < 2)
            {
                return;
            }

            String prefix = "";
            String postfix = "";
            String name = reference.getName();

            outer: for (int i = 0; i < name.length(); i++)
            {
                char c = name.charAt(i);
                for (int j : selection)
                {
                    String n = this.getName(j);
                    if (i >= n.length() || n.charAt(i) != c)
                    {
                        break outer;
                    }
                }
                prefix += c;
            }

            outer: for (int i = 0; i < name.length(); i++)
            {
                char c = name.charAt(name.length() - 1 - i);
                for (int j : selection)
                {
                    String n = this.getName(j);
                    int k = n.length() - 1 - i;
                    if (k < 0 || n.charAt(k) != c)
                    {
                        break outer;
                    }
                }
                postfix = c + postfix;
            }

            for (int i : selection)
            {
                String n = this.getName(i);
                n = n.replace(prefix, "");
                n = n.replace(postfix, "");
                this.setName(i, n);
            }
        });
    }

    public void clearSelection()
    {
        Viewer.getInstance().gui.clearSelection();
        this.table.getSelectionModel().clearSelection();
        this.table.clearSelection();

        this.changedSelection();
    }

    public List<Integer> getAllIndex()
    {
        List<Integer> out = Lists.newArrayList();
        for (int i = 0; i < this.size(); i++)
        {
            out.add(i);
        }
        return out;
    }

    public List<Integer> getSelectionIndex()
    {
        List<Integer> out = Lists.newArrayList();
        for (int idx : this.table.getSelectedRows())
        {
            out.add(this.table.convertRowIndexToModel(idx));
        }
        return out;
    }

    public List<Viewable> getSelectionViewable()
    {
        List<Viewable> out = Lists.newArrayList();
        for (Integer selected : getSelectionIndex())
        {
            out.add(this.getViewable(selected));
        }
        return out;
    }

    public Optional<Viewable> getFirstSelectionViewable()
    {
        List<Integer> selection = this.getSelectionIndex();
        if (selection.size() == 0)
        {
            return Optional.empty();
        }
        else
        {
            return Optional.of(this.getViewable(selection.get(0)));
        }
    }

    public Optional<Integer> getFirstSelectionIndex()
    {
        List<Integer> selection = this.getSelectionIndex();
        if (selection.size() == 0)
        {
            return Optional.empty();
        }
        else
        {
            return Optional.of(selection.get(0));
        }
    }

    public boolean hasSelection()
    {
        return this.getSelectionIndex().size() > 0;
    }

    public void setSelection(int a)
    {
        this.table.getSelectionModel().setSelectionInterval(a, a);
        this.changedSelection();
    }

    public void addSelection(int a)
    {
        this.table.getSelectionModel().addSelectionInterval(a, a);
        this.changedSelection();
    }

    public void setSelection(String name)
    {
        for (int i = 0; i < this.model.getRowCount(); i++)
        {
            if (this.model.getRow(i).getName().equals(name))
            {
                this.setSelection(i);
                return;
            }
        }

        Viewer.getInstance().control.setStatusMessage("data item not found: " + name);
    }

    public void moveUp()
    {
        List<Integer> selection = this.getSelectionIndex();

        if (selection.size() > 0)
        {
            int sidx = selection.get(0);

            if (sidx > 0)
            {
                this.model.moveRow(sidx, sidx, sidx - 1);
                this.setSelection(sidx - 1);
            }
        }
    }

    public void moveDown()
    {
        List<Integer> selection = this.getSelectionIndex();

        if (selection.size() > 0)
        {
            int sidx = selection.get(0);

            if (sidx < this.model.getRowCount() - 1)
            {
                this.model.moveRow(sidx, sidx, sidx + 1);
                this.setSelection(sidx + 1);
            }
        }
    }

    public void moveTop()
    {
        List<Integer> selection = this.getSelectionIndex();

        if (selection.size() > 0)
        {
            int sidx = selection.get(0);
            this.model.moveRow(sidx, sidx, 0);
            this.setSelection(0);
        }
    }

    public void moveBottom()
    {
        List<Integer> selection = this.getSelectionIndex();

        if (selection.size() > 0)
        {
            int sidx = selection.get(0);
            this.model.moveRow(sidx, sidx, this.size() - 1);
            this.setSelection(this.size() - 1);
        }
    }

    public void setName(int idx, String name)
    {
        String uname = this.getUniqueName(name);

        this.model.setValueAt(uname, idx, 1);
        this.changedData();
    }

    /****************
     * GUI Elements *
     ****************/

    private DefaultComboBoxModel<Named<Viewable<?>>> getComboModel(ViewableType type, boolean input, boolean none)
    {
        Global.assume(type != null, "invalid type");

        Class<? extends Viewable<?>> tclass = type.getViewType();
        DefaultComboBoxModel<Named<Viewable<?>>> out = new DefaultComboBoxModel<>();

        for (Viewable v : this.model)
        {
            if (!(input && !v.hasData()) && tclass.isAssignableFrom(v.getClass()))
            {
                out.addElement(new Named<Viewable<?>>(v.getName(), v));
            }
        }

        if (!input)
        {
            out.addElement(NEW);
            out.addElement(NEW_NAMED);
        }

        if (none || out.getSize() == 0)
        {
            out.addElement(NONE);
        }

        return out;
    }

    public BasicComboBox<Named<Viewable<?>>> getComboBox(ViewableType type, boolean input, boolean none)
    {
        DefaultComboBoxModel<Named<Viewable<?>>> model = this.getComboModel(type, input, none);
        BasicComboBox<Named<Viewable<?>>> combo = new BasicComboBox<>(model);

        if (none)
        {
            combo.setSelectedItem(NONE);
        }
        else if (input)
        {
            for (int i : this.getSelectionIndex())
            {
                Viewable entry = this.model.getRow(i);
                boolean matches = type.getViewType().isAssignableFrom(entry.getClass());
                if (entry.hasData() && matches)
                {
                    combo.setSelectedItem(Named.of(entry.getName(), entry));
                    break;
                }
            }
        }
        else
        {
            combo.setSelectedItem(NEW);
            combo.setSelectedItem(NEW_NAMED);
        }

        this.combos.add(new ComboRecord(combo, type, input, none));

        return combo;
    }

    public BasicTable getTable()
    {
        return this.table;
    }

    @Override
    public Iterator<Viewable<?>> iterator()
    {
        return new Iterator<Viewable<?>>()
        {
            private Iterator<Viewable> subiter = Viewables.this.model.iterator();

            @Override
            public boolean hasNext()
            {
                return subiter.hasNext();
            }

            @Override
            public Viewable<?> next()
            {
                Viewable entry = subiter.next();
                return entry;
            }

            @Override
            public void remove()
            {
                Logging.error("remove operation not available");
            }
        };
    }

    private class ComboRecord
    {
        BasicComboBox<Named<Viewable<?>>> combo;
        ViewableType type;
        boolean input;
        boolean none;

        public ComboRecord(BasicComboBox<Named<Viewable<?>>> c, ViewableType t, boolean i, boolean n)
        {
            this.combo = c;
            this.type = t;
            this.input = i;
            this.none = n;
        }
    }

    private class RenderTableModel extends RowTableModel<Viewable>
    {
        RenderTableModel()
        {
            super(Arrays.asList(new String[]{"", "Name",}));
            setRowClass(Viewable.class);
            setColumnClass(0, Boolean.class);
            setColumnClass(1, String.class);
        }

        @Override
        public Object getValueAt(int row, int column)
        {
            Viewable record = getRow(row);

            switch (column)
            {
                case 0:
                    return record.getVisible();
                case 1:
                    return record.getName();
                default:
                    return null;
            }
        }

        @Override
        public void setValueAt(Object value, int row, int column)
        {
            Viewable record = getRow(row);

            switch (column)
            {
                case 0:
                    record.setVisible((boolean) value);
                    break;
                case 1:
                    record.setName((String) value);
                    updateCombos();
                    break;
            }

            fireTableCellUpdated(row, column);
        }
    }

    /***************
     * PERSISTANCE *
     ***************/

    public void save(boolean all, boolean scene, boolean prompt, boolean backup, boolean clobber)
    {
        try
        {
            Viewer.getInstance().control.setStatusMessage("started saving data");
            List<Integer> which = all ? this.getAllIndex() : this.getSelectionIndex();

            if (scene)
            {
                Viewer.getInstance().control.setStatusMessage("...saving scene");
                if (prompt || this.sceneDirname == null)
                {
                    this.sceneDirname = Viewer.getInstance().gui.chooseSaveFile("Save scene directory...", "my.scene");

                    if (this.sceneDirname == null)
                    {
                        Logging.info("save cancelled by user");
                        return;
                    }
                }

                String dn = this.sceneDirname;

                File dnf = new File(dn);
                if (dnf.exists() && dnf.isFile())
                {
                    SwingUtils.showMessage("Could not save! A valid directory must be specified.");
                    return;
                }

                if (!dnf.exists() && dnf.mkdirs())
                {
                    Viewer.getInstance().control.setStatusMessage("created directory: " + dnf);
                }


                List<MetaRecord> mentries = Lists.newArrayList();
                for (int idx : which)
                {
                    Viewable entry = this.model.getRow(idx);
                    String fn = entry.toFilename(PathUtils.join(dn, entry.getName()));

                    if (backup && PathUtils.exists(fn))
                    {
                        PathUtils.backupFile(fn);
                    }

                    Viewer.getInstance().qrun.offer(Pair.of(String.format("Saving %s", entry.getName()), () ->
                    {
                        try
                        {
                            Viewer.getInstance().control.setStatusMessage(String.format("started writing %s to %s", entry.getName(), fn));
                            entry.setFilename(fn);
                            ViewableType.write(entry.getData(), fn);
                        }
                        catch (IOException e)
                        {
                            Logging.info(String.format("warning: failed to write %s to %s", entry.getName(), fn));
                            e.printStackTrace();
                            Viewer.getInstance().control.setStatusMessage(String.format("... finished writing %s to %s", entry.getName(), fn));
                        }
                    }));

                    String name = entry.getName();
                    String bn = PathUtils.basename(fn);

                    // TODO: write out entry view parameters

                    mentries.add(new MetaRecord(entry.getVisible(), name, bn, ViewableType.getFromView(entry)));
                }

                String fn = new File(dn, "entries.json").toString();
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(mentries);
                PrintStream stream = new PrintStream(fn);
                stream.println(json);
                stream.close();

                Viewer.getInstance().settings.save(PathUtils.join(dn, "settings.json"));
            }
            else
            {
                Viewer.getInstance().control.setStatusMessage("saving individual files");
                if (which.size() == 0)
                {
                    SwingUtils.showMessage("Warning: no output files were selected!");
                }
                for (int idx : which)
                {
                    Viewable viewable = this.model.getRow(idx);

                    String name = viewable.getName();

                    if (!viewable.hasData())
                    {
                        Logging.info("skipping due to missing data: name");
                        continue;
                    }

                    Dataset dataset = viewable.getData();
                    String type = dataset.getClass().getSimpleName();

                    if (prompt || viewable.getFilename() == null)
                    {
                        String title = String.format("Save %s...", type);
                        String fn = name;
                        List<String> exts = dataset.getExtensions();
                        boolean exted = false;
                        for (String ext : exts)
                        {
                            if (fn.endsWith(ext))
                            {
                                exted = true;
                                break;
                            }
                        }

                        if (!exted)
                        {
                            fn += "." + exts.get(0);
                        }

                        viewable.setFilename(Viewer.getInstance().gui.chooseSaveFile(title, fn));

                        if (viewable.getFilename() == null)
                        {
                            Logging.info("save cancelled by user");
                            return;
                        }
                    }

                    String fn = viewable.getFilename();

                    if (fn == null)
                    {
                        Viewer.getInstance().control.setStatusMessage("save aborted");
                        return;
                    }

                    if (backup && PathUtils.exists(fn))
                    {
                        PathUtils.backupFile(fn);
                    }

                    String msg = String.format("Warning: save path %s already exists!  Are you sure you want to overwrite it?", fn);
                    if (!clobber && PathUtils.exists(fn) && !SwingUtils.getDecision(msg))
                    {
                        return;
                    }

                    if (PathUtils.isDir(fn))
                    {
                        msg = String.format("Warning: the save path %s is a directory/folder!  This is unusual, are you absolutely sure you want to overwrite it?", fn);
                        if (!SwingUtils.getDecision(msg))
                        {
                            return;
                        }
                    }

                    Viewer.getInstance().qrun.offer(Pair.of(String.format("Saving %s", viewable.getName()), () ->
                    {
                        try
                        {
                            Viewer.getInstance().control.setStatusMessage(String.format("started writing %s to %s", name, fn));
                            ViewableType.write(viewable.getData(), fn);
                            Viewer.getInstance().control.setStatusMessage(String.format("finished writing %s to %s", name, fn));
                        }
                        catch (IOException e)
                        {
                            Logging.info(String.format("warning: failed to write %s to %s", viewable.getName(), fn));
                            e.printStackTrace();
                        }
                    }));
                }
            }
            Viewer.getInstance().control.setStatusMessage("finished saving data");
        }
        catch (IOException e)
        {
            Viewer.getInstance().control.setStatusMessage("error saving: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveGlobalSettings()
    {
        String fn = Viewer.getInstance().gui.chooseSaveFile("Save global settings...", "qitview_global.json");
        saveGlobalSettings(fn);
    }

    public void saveGlobalSettings(String fn)
    {
        try
        {
            Viewer.getInstance().settings.save(fn);
            Viewer.getInstance().gui.setStatusMessage("saved global settings to: " + fn);
        }
        catch (IOException e)
        {
            Logging.info(String.format("warning: failed to write global settings to %s", fn));
            e.printStackTrace();
        }
    }

    public void saveGlobalSettingsDefault()
    {
        String settingsDir = PathUtils.join(System.getProperty("user.home"), ".qit");
        String fn = PathUtils.join(settingsDir, Constants.GLOBAL_SETTINGS);

        try
        {
            Viewer.getInstance().settings.save(fn);
            Viewer.getInstance().gui.setStatusMessage("saved global settings to: " + fn);
        }
        catch (IOException e)
        {
            Logging.info(String.format("warning: failed to write global settings to %s", fn));
            e.printStackTrace();
        }
    }

    public void clearGlobalSettingsDefault()
    {
        String settingsDir = PathUtils.join(System.getProperty("user.home"), ".qit");
        String fn = PathUtils.join(settingsDir, Constants.GLOBAL_SETTINGS);

        if (!PathUtils.exists(fn))
        {
            SwingUtils.showMessage("Warning: no global settings were found to delete...");
        }
        else
        {

            if (SwingUtils.getDecision("This will clear your default global settings, are you sure?"))
            {
                try
                {
                    PathUtils.delete(fn);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public void openGlobalSettings()
    {
        String fn = Viewer.getInstance().gui.chooseLoadFiles("Save global settings...").get(0);
        openGlobalSettings(fn);
    }

    public void openGlobalSettings(String fn)
    {
        try
        {
            Viewer.getInstance().settings.load(fn);
            Viewer.getInstance().gui.setStatusMessage("loaded global settings from: " + fn);
        }
        catch (IOException e)
        {
            Logging.info(String.format("warning: failed to write global settings to %s", fn));
            e.printStackTrace();
        }
    }

    public void openScene()
    {
        List<String> dns = Viewer.getInstance().gui.chooseLoadFiles("Load scene directory...");
        openScene(dns);
    }

    public void openScene(List<String> dns)
    {
        Viewer.getInstance().qrun.offer(Pair.of("Loading scene", () ->
        {
            try
            {
                if (dns == null)
                {
                    Viewer.getInstance().control.setStatusMessage("aborted scene reading");
                    return;
                }

                for (String dn : dns)
                {
                    String fn = PathUtils.join(dn, "entries.json");
                    Global.assume(PathUtils.exists(fn), "entry list doesn't exist: " + fn);

                    Gson gson = new Gson();
                    String json = Files.toString(new File(fn), Charsets.UTF_8);
                    Type listType = new TypeToken<ArrayList<MetaRecord>>()
                    {
                    }.getType();
                    List<MetaRecord> mentries = gson.fromJson(json, listType);

                    for (MetaRecord mentry : mentries)
                    {
                        String efn = PathUtils.join(dn, mentry.filename);
                        if (!PathUtils.exists(efn))
                        {
                            Viewer.getInstance().control.setStatusMessage("skipping entry: " + efn);
                        }
                        else
                        {
                            Viewer.getInstance().control.setStatusMessage("reading entry: " + efn);
                            ViewableType type = mentry.type;
                            Viewable<? extends Dataset> data = ViewableType.create(type);
                            data.setDataset(ViewableType.read(type, efn));
                            data.setVisible(mentry.visible);
                            data.setFilename(efn);
                            data.setName(mentry.name);
                            Viewables.this.add(data);
                        }
                    }

                    Viewer.getInstance().settings.load(PathUtils.join(dn, "settings.json"));
                }

                Viewer.getInstance().gui.canvas.render3D.zoomOverview();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Viewer.getInstance().control.setStatusMessage("error opening: " + e.getMessage());
            }
        }));
    }

    private class MetaRecord
    {
        // an object used to encode meta-data about scene objects to json

        private boolean visible;
        private String name;
        private String filename;
        private ViewableType type;

        @SuppressWarnings("unused")
        private MetaRecord()
        {
        }

        public MetaRecord(boolean visible, String name, String filename, ViewableType type)
        {
            this.visible = visible;
            this.name = name;
            this.filename = filename;
            this.type = type;
        }

    }

    public static BiConsumer<String, Volume> consumeVolume()
    {
        return (name, volume) ->
        {
            String out = SwingUtils.getStringSafe(Viewer.getInstance().gui.getFrame(), "Volume name:", "output.volume." + name);
            if (out == null || out.length() == 0)
            {
                return;
            }

            SwingUtils.invokeAndWait(() ->
            {
                VolumeView data = new VolumeView();
                data.setData(volume);
                data.setName(out);
                data.setVisible(true);

                Viewer.getInstance().control.add(data, true);
            });
        };
    }

    public static BiConsumer<String, Mask> consumeMask()
    {
        return (name, mask) ->
        {
            String out = SwingUtils.getStringSafe(Viewer.getInstance().gui.getFrame(), "Mask name:", "output.mask." + name);
            if (out == null || out.length() == 0)
            {
                return;
            }

            SwingUtils.invokeAndWait(() ->
            {
                MaskView data = new MaskView();
                data.setData(mask);
                data.setName(out);
                data.setVisible(true);

                Viewer.getInstance().control.add(data, true);
            });
        };
    }

    public static BiConsumer<String, Curves> consumeCurves()
    {
        return (name, curves) ->
        {
            String out = SwingUtils.getStringSafe(Viewer.getInstance().gui.getFrame(), "Curves name:", "output.curves." + name);
            if (out == null || out.length() == 0)
            {
                return;
            }

            SwingUtils.invokeAndWait(() ->
            {
                CurvesView data = new CurvesView();
                data.setData(curves);
                data.setName(out);
                data.setVisible(true);

                Viewer.getInstance().control.add(data, true);
            });
        };
    }

    public static BiConsumer<String, Mesh> consumeMesh()
    {
        return (name, mesh) ->
        {
            String out = SwingUtils.getStringSafe(Viewer.getInstance().gui.getFrame(), "Mesh name:", "output.mesh." + name);
            if (out == null || out.length() == 0)
            {
                return;
            }

            SwingUtils.invokeAndWait(() ->
            {
                MeshView data = new MeshView();
                data.setData(mesh);
                data.setName(out);
                data.setVisible(true);

                Viewer.getInstance().control.add(data, true);
            });
        };
    }

    public static BiConsumer<String, Vects> consumeVects()
    {
        return (name, vects) ->
        {
            String out = SwingUtils.getStringSafe(Viewer.getInstance().gui.getFrame(), "Vects name:", "output.vects." + name);
            if (out == null || out.length() == 0)
            {
                return;
            }

            SwingUtils.invokeAndWait(() ->
            {
                VectsView data = new VectsView();
                data.setData(vects);
                data.setName(out);
                data.setVisible(true);

                Viewer.getInstance().control.add(data, true);
            });
        };
    }

    public static BiConsumer<String, Solids> consumeSolids()
    {
        return (name, solids) ->
        {
            String out = SwingUtils.getStringSafe(Viewer.getInstance().gui.getFrame(), "Solids name:", "output.solids." + name);
            if (out == null || out.length() == 0)
            {
                return;
            }

            SwingUtils.invokeAndWait(() ->
            {
                SolidsView data = new SolidsView();
                data.setData(solids);
                data.setName(out);
                data.setVisible(true);

                Viewer.getInstance().control.add(data, true);
            });
        };
    }

    public static BiConsumer<String, Affine> consumeAffine()
    {
        return (name, affine) ->
        {
            String out = SwingUtils.getStringSafe(Viewer.getInstance().gui.getFrame(), "Affine name:", "output.affine." + name);
            if (out == null || out.length() == 0)
            {
                return;
            }

            SwingUtils.invokeAndWait(() ->
            {
                AffineView data = new AffineView();
                data.setData(affine);
                data.setName(out);
                data.setVisible(true);

                Viewer.getInstance().control.add(data, true);
            });
        };
    }

    public static BiConsumer<String, Gradients> consumeGradients()
    {
        return (name, affine) ->
        {
            String out = SwingUtils.getStringSafe(Viewer.getInstance().gui.getFrame(), "Gradients name:", "output.affine." + name);
            if (out == null || out.length() == 0)
            {
                return;
            }

            SwingUtils.invokeAndWait(() ->
            {
                GradientsView data = new GradientsView();
                data.setData(affine);
                data.setName(out);
                data.setVisible(true);

                Viewer.getInstance().control.add(data, true);
            });
        };
    }

    public static BiConsumer<String, Table> consumeTable()
    {
        return (name, table) ->
        {
            String out = SwingUtils.getStringSafe(Viewer.getInstance().gui.getFrame(), "Table name:", "output.table." + name);
            if (out == null || out.length() == 0)
            {
                return;
            }

            SwingUtils.invokeAndWait(() ->
            {
                TableView data = new TableView();
                data.setData(table);
                data.setName(out);
                data.setVisible(true);

                Viewer.getInstance().control.add(data, true);
            });
        };
    }

    public static <E> Runnable processorOld(String name, BiConsumer<String, E> consume, Supplier<E> supply)
    {
        return () -> Viewer.getInstance().qrun.offer(Pair.of("Running " + name, () -> consume.accept(name, supply.get())));
    }

    public static BiConsumer<String, Supplier<Optional<Dataset>>> consumer(ViewableType type, Supplier<Optional<Viewable>> input)
    {
        return (name, source) ->
        {
            Optional<Viewable> destination = input.get();
            if (destination.isEmpty())
            {
                String which = type.getText();
                String prompt = String.format("Name for Output %s:", which);
                String initial = String.format("Output-%s-%s", which, name);
                BiConsumer<String, Dataset> storeit = (user, data) ->
                {
                    SwingUtils.invokeAndWait(() ->
                    {
                        try
                        {
                            Viewable view = type.getViewType().newInstance();
                            view.setVisible(true);
                            view.setName(user);
                            view.setData(data);
                            Viewer.getInstance().control.add(view, true);
                        }
                        catch (Exception e)
                        {
                            Logging.info("warning: failed to create new data object!");
                            e.printStackTrace();
                        }
                    });
                };

                SwingUtils.getStringOptional(prompt, initial).ifPresent((user) -> source.get().ifPresent((data) -> storeit.accept(user, data)));
            }
            else
            {
                source.get().ifPresent((data) -> SwingUtils.invokeAndWait(() -> destination.get().setData(data)));
            }
        };
    }

    public static void process(String name, BiConsumer<String, Supplier<Optional<Dataset>>> consumer, Supplier<Optional<Dataset>> supplier)
    {
        Viewer.getInstance().qrun.offer(Pair.of("Running " + name, () -> consumer.accept(name, supplier)));
    }

    public static Runnable processor(String name, BiConsumer<String, Supplier<Optional<Dataset>>> consumer, Supplier<Optional<Dataset>> supplier)
    {
        return () -> process(name, consumer, supplier);
    }
}
