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
import com.google.common.collect.Sets;
import objectexplorer.ObjectGraphMeasurer;
import qit.base.Dataset;
import qit.base.Global;
import qit.base.Interpreter;
import qit.base.Logging;
import qit.base.structs.Pair;
import qit.base.structs.Triple;
import qit.base.utils.PathUtils;
import qit.data.datasets.Mask;
import qit.data.datasets.Sampling;
import qit.data.datasets.Solids;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.source.MaskSource;
import qit.math.structs.Box;
import qit.math.structs.Sphere;
import qitview.models.ScreenMouse;
import qitview.models.Sliceable;
import qitview.models.Slicer;
import qitview.models.Viewable;
import qitview.models.ViewableType;
import qitview.models.VolumeSlicePlane;
import qitview.panels.Viewables;
import qitview.views.MaskView;
import qitview.views.MeshView;
import qitview.views.SolidsView;
import qitview.views.VectsView;
import qitview.views.VolumeView;
import qitview.widgets.FileLoader;
import qitview.widgets.FileSaver;
import qitview.widgets.SwingUtils;

import javax.swing.AbstractButton;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class Controller implements KeyListener, ActionListener
{
    private Map<String, Runnable> actions = Maps.newHashMap();
    private long actionLastTime = 0;
    private String actionLastName = "";

    public Controller()
    {
        this.actions.put(Constants.FILE_MENU_QUIT, () -> Viewer.getInstance().gui.close());
        this.actions.put(Constants.SETTINGS_DIAGNOSTICS, () -> this.printDiagnostics());
        this.actions.put(Constants.DATA_AUTO_MIN_MAX, () -> this.autoMinMax());
        this.actions.put(Constants.DATA_SORT_FACES, () -> this.sortFaces());
        this.actions.put(Constants.VIEW_SLICE_I, () -> this.toggleSlice(VolumeSlicePlane.I));
        this.actions.put(Constants.VIEW_SLICE_J, () -> this.toggleSlice(VolumeSlicePlane.J));
        this.actions.put(Constants.VIEW_SLICE_K, () -> this.toggleSlice(VolumeSlicePlane.K));
        this.actions.put(Constants.VIEW_CHAN_NEXT, () -> this.channelNext());
        this.actions.put(Constants.VIEW_CHAN_PREV, () -> this.channelPrev());
        this.actions.put(Constants.VIEW_TOGGLE, () -> this.toggleVisibility());
        this.actions.put(Constants.VIEW_SHOW_ONLY, () -> this.showOnlySelection());
        this.actions.put(Constants.VIEW_SHOW_ALL, () -> this.showAll());
        this.actions.put(Constants.VIEW_MOVE_UP, () -> Viewer.getInstance().data.moveUp());
        this.actions.put(Constants.VIEW_MOVE_DOWN, () -> Viewer.getInstance().data.moveDown());
        this.actions.put(Constants.VIEW_MOVE_TOP, () -> Viewer.getInstance().data.moveTop());
        this.actions.put(Constants.VIEW_MOVE_BOTTOM, () -> Viewer.getInstance().data.moveBottom());
        this.actions.put(Constants.VIEW_LIST_NEXT, () -> this.showNext());
        this.actions.put(Constants.VIEW_LIST_PREV, () -> this.showPrev());
        this.actions.put(Constants.VIEW_ZOOM_DETAIL, () -> Viewer.getInstance().gui.canvas.render3D.zoomDetail());
        this.actions.put(Constants.VIEW_ZOOM_OVERVIEW, () -> Viewer.getInstance().gui.canvas.render3D.zoomOverview());
        this.actions.put(Constants.DATA_CREATE_VECTS, () -> this.createVects());
        this.actions.put(Constants.DATA_CREATE_BOX, () -> this.createBox());
        this.actions.put(Constants.DATA_CREATE_SPHERE, () -> this.createSphere());
        this.actions.put(Constants.DATA_CREATE_MASK, () -> this.createMask());
        this.actions.put(Constants.DATA_MASK_TO_VOLUME, () -> this.maskToVolume());
        this.actions.put(Constants.DATA_VOLUME_TO_MASK, () -> this.volumeToMask());
        this.actions.put(Constants.VIEW_POSE_TOP, () -> Viewer.getInstance().gui.canvas.render3D.poseTop());
        this.actions.put(Constants.VIEW_POSE_FRONT, () -> Viewer.getInstance().gui.canvas.render3D.poseFront());
        this.actions.put(Constants.VIEW_POSE_LEFT, () -> Viewer.getInstance().gui.canvas.render3D.poseLeft());
        this.actions.put(Constants.VIEW_POSE_RIGHT, () -> Viewer.getInstance().gui.canvas.render3D.poseRight());
        this.actions.put(Constants.VIEW_POSE_BOTTOM, () -> Viewer.getInstance().gui.canvas.render3D.poseBottom());
        this.actions.put(Constants.VIEW_POSE_BACK, () -> Viewer.getInstance().gui.canvas.render3D.poseBack());
        this.actions.put(Constants.VIEW_POSE_ANGLES, () -> Viewer.getInstance().gui.canvas.render3D.poseAngles());
        this.actions.put(Constants.FILE_MENU_SHOT_1X, () -> this.takeShotPrompt(1));
        this.actions.put(Constants.FILE_MENU_SHOT_2X, () -> this.takeShotPrompt(2));
        this.actions.put(Constants.FILE_MENU_SHOT_3X, () -> this.takeShotPrompt(3));
        this.actions.put(Constants.FILE_MENU_SHOT_NX, () -> this.takeShotPrompt());
        this.actions.put(Constants.FILE_MENU_LOAD_SCENE, () -> Viewer.getInstance().data.openScene());
        this.actions.put(Constants.FILE_MENU_LOAD_GLOBAL, () -> Viewer.getInstance().data.openGlobalSettings());
        this.actions.put(Constants.FILE_MENU_SAVE_GLOBAL, () -> Viewer.getInstance().data.saveGlobalSettings());
        this.actions.put(Constants.FILE_MENU_SAVE_DEFAULT_GLOBAL, () -> Viewer.getInstance().data.saveGlobalSettingsDefault());
        this.actions.put(Constants.FILE_MENU_CLEAR_DEFAULT_GLOBAL, () -> Viewer.getInstance().data.clearGlobalSettingsDefault());
        this.actions.put(Constants.FILE_MENU_SAVE_SEL_FILES, () -> new FileSaver().run());
        this.actions.put(Constants.FILE_MENU_SAVE_SEL_FILES_AS, () -> new FileSaver().withPrompt().run());
        this.actions.put(Constants.FILE_MENU_SAVE_ALL_FILES, () -> new FileSaver().withAll().run());
        this.actions.put(Constants.FILE_MENU_SAVE_ALL_FILES_AS, () -> new FileSaver().withAll().withPrompt().run());
        this.actions.put(Constants.FILE_MENU_SAVE_SEL_SCENE, () -> new FileSaver().withScene().run());
        this.actions.put(Constants.FILE_MENU_SAVE_SEL_SCENE_AS, () -> new FileSaver().withScene().withPrompt().run());
        this.actions.put(Constants.FILE_MENU_SAVE_ALL_SCENE, () -> new FileSaver().withAll().withScene().run());
        this.actions.put(Constants.FILE_MENU_SAVE_ALL_SCENE_AS, () -> new FileSaver().withAll().withScene().withPrompt().run());
        this.actions.put(Constants.DATA_DELETE_FILENAMES, () -> this.deleteFilenames());
        this.actions.put(Constants.DATA_DELETE_VALUE, () -> this.deleteValues());
        this.actions.put(Constants.DATA_DELETE_INTERACTION, () -> this.keyClear());
        this.actions.put(Constants.DATA_DELETE_SELECTION, () -> Viewer.getInstance().data.removeSelection());
        this.actions.put(Constants.DATA_DELETE_ALL, () -> this.deleteAll());
        this.actions.put(Constants.SETTINGS_BG_SET, () -> this.setBackgroundColor(SwingUtils.chooseColor(Viewer.getInstance().gui.getFrame(), this.getBackgroundColor())));
        this.actions.put(Constants.SETTINGS_BG_RESET, () -> this.resetBackgroundColor());
        this.actions.put(Constants.SETTINGS_REPL, () -> Interpreter.interactive());
        this.actions.put(Constants.SETTINGS_PROCESSES, () -> Viewer.getInstance().gui.showProcesses(true));
        this.actions.put(Constants.SETTINGS_MODULES, () -> Viewer.getInstance().gui.getModules().show());
        this.actions.put(Constants.FILE_MENU_LOAD_FILES, () -> new FileLoader());
        this.actions.put(Constants.FILE_MENU_LOAD_SCENE, () -> Viewer.getInstance().data.openScene());
    }

    public synchronized void setAction(String name, Runnable action)
    {
        this.actions.put(name, action);
    }

    public synchronized void setStatusMessage(String v)
    {
        Viewer.getInstance().gui.setStatusMessage(v);
        Logging.info(v);
    }

    public synchronized void setStatusError(String v)
    {
        Viewer.getInstance().gui.setStatusError(v);
        Logging.info("warning: " + v);
    }

    public synchronized Slicer getSlicer(Sampling sampling)
    {
        Global.assume(sampling != null, "invalid sampling");

        for (Slicer slicer : Viewer.getInstance().slicers)
        {
            if (slicer.compatible(sampling))
            {
                return slicer;
            }
        }

        Slicer added = new Slicer(sampling);
        added.set(sampling.numI() / 2, sampling.numJ() / 2, sampling.numK() / 2);

        Viewer.getInstance().slicers.add(added);
        return added;
    }

    @Override
    public synchronized void keyTyped(KeyEvent e)
    {
        mouseModifierUpdate(e);
    }

    @Override
    public synchronized void keyPressed(KeyEvent e)
    {
        int code = e.getKeyCode();
        Map<Integer, Long> keys = Viewer.getInstance().keys;
        keys.put(code, System.currentTimeMillis());
        mouseModifierUpdate(e);

        if (code == KeyEvent.VK_ESCAPE)
        {
            keys.clear();
            Viewer.getInstance().gui.canvas.render3D.mouse.pick = false;
        }

        int delta = 0;
        if (code == KeyEvent.VK_UP || code == KeyEvent.VK_RIGHT)
        {
            delta = 1;
        }
        else if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_LEFT)
        {
            delta = -1;
        }

        if (e.isShiftDown())
        {
            delta *= 2;
        }

        if (e.isControlDown())
        {
            delta *= 5;
        }

        Viewer.getInstance().gui.changeSlice(delta);
    }

    private void mouseModifierUpdate(KeyEvent e)
    {
        Settings state = Viewer.getInstance().settings;
        ScreenMouse mouse = Viewer.getInstance().gui.canvas.render3D.mouse;

        mouse.control = e.isControlDown();
        mouse.shift = e.isShiftDown();
        mouse.pick = e.isAltDown();
        mouse.pick |= Viewer.getInstance().keys.containsKey(KeyEvent.VK_BACK_QUOTE);
        mouse.pick |= Viewer.getInstance().keys.containsKey(KeyEvent.VK_DEAD_TILDE);
        mouse.pick |= Viewer.getInstance().keys.containsKey(KeyEvent.VK_F1);
    }

    @Override
    public synchronized void keyReleased(KeyEvent e)
    {
        int code = e.getKeyCode();

        Map<Integer, Long> keys = Viewer.getInstance().keys;

        if (keys.containsKey(code))
        {
            keys.remove(code);
        }

        mouseModifierUpdate(e);

        if (code == KeyEvent.VK_ESCAPE)
        {
            Viewer.getInstance().gui.resetMode();
            keyClear();
        }
    }

    public synchronized void keyClear()
    {
        Viewer.getInstance().keys.clear();
        Viewer.getInstance().gui.canvas.render3D.mouse.pick = false;
    }

    public synchronized void stepI(int n)
    {
        Logging.info("stepping slice I");
        for (Slicer slicer : Viewer.getInstance().slicers)
        {
            slicer.stepI(n);
        }
    }

    public synchronized void stepJ(int n)
    {
        Logging.info("stepping slice J");
        for (Slicer slicer : Viewer.getInstance().slicers)
        {
            slicer.stepJ(n);
        }
    }

    public synchronized void stepK(int n)
    {
        Logging.info("stepping slice K");
        for (Slicer slicer : Viewer.getInstance().slicers)
        {
            slicer.stepK(n);
        }
    }

    public synchronized void channelNext()
    {
        Logging.info("switching to the next channel");
        List<Integer> sidx = Viewer.getInstance().data.getSelectionIndex();
        for (int idx : sidx)
        {
            Viewable<?> viewable = Viewer.getInstance().data.getViewable(idx);
            if (viewable instanceof VolumeView)
            {
                ((VolumeView) viewable).nextChannel();
            }
        }
    }

    public synchronized void channelPrev()
    {
        Logging.info("switching to the prev channel");
        List<Integer> sidx = Viewer.getInstance().data.getSelectionIndex();
        for (int idx : sidx)
        {
            Viewable<?> viewable = Viewer.getInstance().data.getViewable(idx);
            if (viewable instanceof VolumeView)
            {
                ((VolumeView) viewable).prevChannel();
            }
        }
    }

    public synchronized void toggleVisibility()
    {
        List<Integer> sidx = Viewer.getInstance().data.getSelectionIndex();
        for (int idx : sidx)
        {
            Viewer.getInstance().data.flipVisibility(idx);
        }
    }

    public synchronized void setVisibility(boolean v)
    {
        Logging.info("setting visibility");
        List<Integer> sidx = Viewer.getInstance().data.getSelectionIndex();
        for (int idx : sidx)
        {
            Viewer.getInstance().data.setVisibility(idx, v);
        }
    }

    public synchronized void showOnlySelection()
    {
        Logging.info("showing only selection");
        Viewables data = Viewer.getInstance().data;
        List<Integer> sidx = Viewer.getInstance().data.getSelectionIndex();

        for (int idx = 0; idx < data.size(); idx++)
        {
            data.setVisibility(idx, sidx.contains(idx));
        }
    }

    public synchronized void showAll()
    {
        Logging.info("showing all");
        Viewables data = Viewer.getInstance().data;

        for (int idx = 0; idx < data.size(); idx++)
        {
            data.setVisibility(idx, true);
        }
    }

    public synchronized void showNext()
    {
        Logging.info("showing next item");
        Viewables data = Viewer.getInstance().data;
        List<Integer> cidx = data.getSelectionIndex();

        if (cidx.size() > 0)
        {
            int idx = cidx.get(0);

            int num = Viewer.getInstance().data.size();
            int next = (idx + 1) % num;
            data.setVisibility(next, data.getVisibility(idx));
            data.setVisibility(idx, false);

            data.setSelection(next);
        }
    }

    public synchronized void showPrev()
    {
        Logging.info("showing next item");
        Viewables data = Viewer.getInstance().data;

        List<Integer> cidx = data.getSelectionIndex();

        if (cidx.size() > 0)
        {
            int idx = cidx.get(0);

            int num = Viewer.getInstance().data.size();
            int prev = (idx + num - 1) % num;
            data.setVisibility(prev, data.getVisibility(idx));
            data.setVisibility(idx, false);

            data.setSelection(prev);
        }
    }

    public synchronized void autoMinMax()
    {
        Viewables data = Viewer.getInstance().data;
        List<Integer> sidx = data.getSelectionIndex();

        if (sidx.size() > 0)
        {
            Viewable record = data.getViewable(sidx.get(0));

            if (record.hasData() && record instanceof VolumeView)
            {
                Logging.info("setting auto min max: " + record.getName());
                ((VolumeView) record).autoMinMax();
            }
        }
    }

    public synchronized void sortFaces()
    {
        Viewables data = Viewer.getInstance().data;

        Class<? extends Viewable<?>> vclass = ViewableType.Mesh.getViewType();

        for (int i = 0; i < data.size(); i++)
        {
            Viewable record = data.getViewable(i);

            if (record.hasData() && vclass.isAssignableFrom(record.getData().getClass()))
            {
                Logging.info("sorting faces of " + record.getName());
                ((MeshView) record.getData()).sortFaces();
            }
        }
    }

    public synchronized void toggleSlice(VolumeSlicePlane plane)
    {
        Viewables data = Viewer.getInstance().data;
        List<Integer> sidx = data.getSelectionIndex();

        if (sidx.size() > 0)
        {
            Viewable<?> record = data.getViewable(sidx.get(0));

            if (record.hasData() && record instanceof VolumeView)
            {
                Logging.info("toggling slice I: " + record.getName());
                ((VolumeView) record).getSlicer().toggleShow(plane);
            }

            if (record.hasData() && record instanceof MaskView)
            {
                Logging.info("toggling slice I: " + record.getName());
                ((MaskView) record).getSlicer().toggleShow(plane);
            }
        }
    }

    public synchronized void volumeToMask()
    {
        Viewables data = Viewer.getInstance().data;

        Class<? extends Viewable<?>> vclass = ViewableType.Volume.getViewType();

        List<Viewable> records = Lists.newArrayList();
        List<Integer> remove = Lists.newArrayList();

        for (int idx : data.getSelectionIndex())
        {
            Viewable record = data.getViewable(idx);
            if (record.hasData() && record instanceof VolumeView)
            {
                records.add(record);
                remove.add(idx);
            }
        }

        data.remove(remove);

        for (Viewable record : records)
        {
            Logging.info("converting volume to mask: " + record.getName());
            Volume volume = (Volume) record.getData();
            Mask mask = MaskSource.mask(volume);

            MaskView nview = new MaskView();
            nview.setFilename(record.getFilename());
            nview.setName(record.getName());
            nview.setVisible(record.getVisible());
            nview.setData(mask);

            data.add(nview);
        }

        data.updateCombos();
    }

    public synchronized void maskToVolume()
    {
        Viewables data = Viewer.getInstance().data;

        Class<? extends Viewable<?>> vclass = ViewableType.Mask.getViewType();

        List<Viewable> records = Lists.newArrayList();
        List<Integer> remove = Lists.newArrayList();

        for (int idx : data.getSelectionIndex())
        {
            Viewable record = data.getViewable(idx);
            if (record.hasData() && record instanceof MaskView)
            {
                records.add(record);
                remove.add(idx);
            }
        }

        data.remove(remove);

        for (Viewable record : records)
        {
            Logging.info("converting volume to mask: " + record.getName());

            Mask mask = (Mask) record.getData();
            Volume volume = mask.copyVolume();

            VolumeView nview = new VolumeView();
            nview.setFilename(record.getFilename());
            nview.setName(record.getName());
            nview.setVisible(record.getVisible());
            nview.setData(volume);

            data.add(nview);
        }

        data.updateCombos();
    }

    public synchronized void resetInteraction()
    {
        Logging.info("resetting interaction parameters");
        Settings state = Viewer.getInstance().settings;
        state.xposMouse = Constants.XPOS_FACTOR;
        state.yposMouse = Constants.YPOS_FACTOR;
        state.zposMouse = Constants.ZPOS_FACTOR;
        state.scaleMouse = Constants.SCALE_FACTOR;
        state.xrotMouse = Constants.XROT_FACTOR;
        state.yrotMouse = Constants.YROT_FACTOR;
    }

    public synchronized void createMask()
    {
        String name = SwingUtils.getString(Viewer.getInstance().gui.getFrame(), "Mask name:", "New-Mask");

        if (name == null)
        {
            this.setStatusMessage("mask creation aborted");
            return;
        }

        for (int idx : Viewer.getInstance().data.getSelectionIndex())
        {
            Viewable<?> r = Viewer.getInstance().data.getViewable(idx);
            if (r instanceof VolumeView)
            {
                VolumeView vr = (VolumeView) r;
                Mask mask = MaskSource.create(vr.getData().getSampling());

                MaskView data = new MaskView();
                data.setData(mask);
                data.setName(name);
                data.setVisible(true);

                this.add(data);
                Viewer.getInstance().data.setSelection(name);

                this.setStatusMessage("created prototype mask: " + name);
            }
            else if (r instanceof MaskView)
            {
                MaskView vr = (MaskView) r;
                Mask mask = MaskSource.create(vr.getData().getSampling());

                MaskView data = new MaskView();
                data.setData(mask);
                data.setName(name);
                data.setVisible(true);

                this.add(data);
                Viewer.getInstance().data.setSelection(name);

                this.setStatusMessage("created prototype mask: " + name);
            }
            else
            {
                SwingUtils.showMessage("Could not create mask, please select a volume and try again");
            }
        }
    }

    public synchronized void createSphere()
    {
        String name = SwingUtils.getString(Viewer.getInstance().gui.getFrame(), "Sphere name:", "New-Sphere");

        if (name == null)
        {
            this.setStatusMessage("sphere creation aborted");
            return;
        }

        Viewer.getInstance().data.getFirstSelectionIndex().ifPresent(idx ->
        {
            Viewable<?> r = Viewer.getInstance().data.getViewable(idx);
            Box box = r.getBounds();

            if (box != null)
            {
                SolidsView data = new SolidsView();
                Solids solids = new Solids();
                solids.addSphere(Sphere.fromBox(r.getBounds()));
                data.setData(solids);
                data.setName(name);
                data.setVisible(true);

                this.add(data);
                Viewer.getInstance().data.setSelection(name);

                this.setStatusMessage("created prototype sphere: " + name);
            }
        });
    }

    public synchronized void createBox()
    {
        String name = SwingUtils.getString(Viewer.getInstance().gui.getFrame(), "Box name:", "New-Box");

        if (name == null)
        {
            this.setStatusMessage("box creation aborted");
            return;
        }

        Viewer.getInstance().data.getFirstSelectionIndex().ifPresent(idx ->
        {
            Viewable<?> r = Viewer.getInstance().data.getViewable(idx);
            Box box = r.getBounds();

            if (box != null)
            {
                SolidsView data = new SolidsView();
                Solids solids = new Solids();
                solids.addBox(box);
                data.setData(solids);
                data.setName(name);
                data.setVisible(true);

                this.add(data);
                Viewer.getInstance().data.setSelection(name);

                this.setStatusMessage("creating prototype box: " + name);
            }
        });
    }

    public synchronized void createVects()
    {
        String name = SwingUtils.getString(Viewer.getInstance().gui.getFrame(), "Vects name:", "New-Vects");

        if (name == null)
        {
            this.setStatusMessage("box creation aborted");
            return;
        }

        VectsView data = new VectsView();
        data.ballShow = true;
        Vects vects = new Vects();
        data.setData(vects);
        data.setName(name);
        data.setVisible(true);

        this.add(data);
        Viewer.getInstance().data.setSelection(name);

        this.setStatusMessage("created vects: " + name);
    }

    public synchronized void showQuery()
    {
        Viewer.getInstance().gui.showQuery();
    }

    public synchronized void setQuery(Vect v)
    {
        Viewer.getInstance().gui.getQuery().setQuery(v);
    }

    public synchronized void setQuery(Sampling r, Vect v)
    {
        Viewer.getInstance().gui.getQuery().setQuery(r, v);
    }

    public synchronized void deleteFilenames()
    {
        Logging.info("deleting stored filenames");
        Viewables data = Viewer.getInstance().data;

        for (int idx : data.getSelectionIndex())
        {
            data.getViewable(idx).setFilename(null);
        }
    }

    public synchronized void deleteValues()
    {
        Logging.info("deleting stored data values");
        Viewables data = Viewer.getInstance().data;

        for (int idx : data.getSelectionIndex())
        {
            data.getViewable(idx).clearData();
        }
    }

    public synchronized void takeShot(String fn, int n)
    {
        Global.assume(n > 0, "the screenshot zoom must be positive");
        if (fn != null)
        {
            this.setStatusMessage("taking screenshot with zoom: " + n);
            Viewer.getInstance().qshots.offer(Pair.of(fn, n));
        }
        else
        {
            this.setStatusMessage("warning: failed to take screenshot");
        }
    }

    public synchronized void resetBackgroundColor()
    {
        Viewer.getInstance().settings.bgRed = Constants.BG_RED_DEFAULT;
        Viewer.getInstance().settings.bgGreen = Constants.BG_GREEN_DEFAULT;
        Viewer.getInstance().settings.bgBlue = Constants.BG_BLUE_DEFAULT;
    }

    public synchronized void setBackgroundColor(Color c)
    {
        Viewer.getInstance().settings.bgRed = c.getRed() / 255.0f;
        Viewer.getInstance().settings.bgGreen = c.getGreen() / 255.0f;
        Viewer.getInstance().settings.bgBlue = c.getBlue() / 255.0f;
    }

    public synchronized Color getBackgroundColor()
    {
        int red = Math.round(255 * Viewer.getInstance().settings.bgRed);
        int green = Math.round(255 * Viewer.getInstance().settings.bgGreen);
        int blue = Math.round(255 * Viewer.getInstance().settings.bgBlue);

        return new Color(red, green, blue);
    }

    public synchronized void resetBoxColor()
    {
        Viewer.getInstance().settings.boxRed = Constants.BOX_RED_DEFAULT;
        Viewer.getInstance().settings.boxGreen = Constants.BOX_GREEN_DEFAULT;
        Viewer.getInstance().settings.boxBlue = Constants.BOX_BLUE_DEFAULT;
    }

    public synchronized void setBoxColor(Color c)
    {
        Viewer.getInstance().settings.boxRed = c.getRed() / 255.0f;
        Viewer.getInstance().settings.boxGreen = c.getGreen() / 255.0f;
        Viewer.getInstance().settings.boxBlue = c.getBlue() / 255.0f;
    }

    public synchronized Color getBoxColor()
    {
        int red = Math.round(255 * Viewer.getInstance().settings.boxRed);
        int green = Math.round(255 * Viewer.getInstance().settings.boxGreen);
        int blue = Math.round(255 * Viewer.getInstance().settings.boxBlue);

        return new Color(red, green, blue);
    }

    public synchronized void resetScaleColor()
    {
        Viewer.getInstance().settings.scaleRed = Constants.SCALE_RED_DEFAULT;
        Viewer.getInstance().settings.scaleGreen = Constants.SCALE_GREEN_DEFAULT;
        Viewer.getInstance().settings.scaleBlue = Constants.SCALE_BLUE_DEFAULT;
    }

    public synchronized void setScaleColor(Color c)
    {
        Viewer.getInstance().settings.scaleRed = c.getRed() / 255.0f;
        Viewer.getInstance().settings.scaleGreen = c.getGreen() / 255.0f;
        Viewer.getInstance().settings.scaleBlue = c.getBlue() / 255.0f;
    }

    public synchronized Color getScaleColor()
    {
        int red = Math.round(255 * Viewer.getInstance().settings.scaleRed);
        int green = Math.round(255 * Viewer.getInstance().settings.scaleGreen);
        int blue = Math.round(255 * Viewer.getInstance().settings.scaleBlue);

        return new Color(red, green, blue);
    }

    public synchronized void takeShotPrompt(int n)
    {
        this.takeShot(Viewer.getInstance().gui.chooseSaveFile("Choose screenshot filename...", "screenshot.png"), n);
    }

    public synchronized void takeShotPrompt()
    {
        try
        {
            String value = SwingUtils.getString(Viewer.getInstance().gui.getFrame(), "What zoom level should be used for the screenshot?", "5");
            int level = Integer.valueOf(value);

            if (level < 6 || SwingUtils.getDecision("That's a huge image, are you sure you want to render this?"))
            {
                this.takeShotPrompt(level);
            }
            else
            {
                this.setStatusMessage("warning: failed to take screenshot at zoom: " + level);
            }
        }
        catch (RuntimeException e)
        {
            this.setStatusMessage("warning: failed to take screenshot");
        }
    }

    public synchronized void add(Viewable entry)
    {
        this.add(entry, false);
    }

    public synchronized void add(Viewable entry, boolean select)
    {
        if (entry == null)
        {
            this.setStatusMessage("invalid entry encountered");
            return;
        }

        Viewer.getInstance().data.add(entry, select);
        this.setStatusMessage("added: " + entry.getName());

        if (entry.hasData() && entry.getData() instanceof Sliceable)
        {
            Slicer slicer = ((Sliceable) entry.getData()).getSlicer();
            if (slicer != null)
            {
                Viewer.getInstance().slicers.add(slicer);
            }
            else
            {
                throw new RuntimeException("invalid slicer");
            }
        }
    }

    public synchronized void add(String name, Dataset data)
    {
        try
        {
            ViewableType type = ViewableType.getFromData(data);
            Viewable view = ViewableType.create(type);
            view.setVisible(true);
            view.setName(name);
            view.setData(data);
            this.add(view, true);
        }
        catch (Exception e)
        {
            Logging.info("warning: failed to create new data object!");
            e.printStackTrace();
        }
    }

    public synchronized void readQueue(ViewableType type, final List<String> fns, boolean fullpath)
    {
        boolean pass = Viewer.getInstance().qload.offer(Triple.of(type, fns, fullpath));

        if (!pass)
        {
            SwingUtils.safeMessage("Warning: failed to initiate file loading!  Please report this as a bug.");
        }
    }

    public synchronized static Map<String, String> names(List<String> fns, boolean fullpath)
    {
        if (fullpath)
        {
            String sep = Pattern.quote(System.getProperty("file.separator"));
            Set<String> proto = Sets.newHashSet();
            if (fns.size() > 1)
            {
                {
                    String[] tokens = fns.get(0).split(sep);
                    for (int i = 0; i < tokens.length - 1; i++)
                    {
                        proto.add(tokens[i]);
                    }
                }
                {
                    String[] tokens = fns.get(1).split(sep);
                    Set<String> tset = Sets.newHashSet();

                    for (String token : tokens)
                    {
                        tset.add(token);
                    }

                    for (String found : Lists.newArrayList(proto))
                    {
                        if (!tset.contains(found))
                        {
                            proto.remove(found);
                        }
                    }
                }
            }

            Map<String, String> names = Maps.newLinkedHashMap();
            if (fns.size() == 1)
            {
                String[] tokens = fns.get(0).split(sep);
                String base = tokens[tokens.length - 1];
                names.put(fns.get(0), PathUtils.noext(base));
            }
            else
            {
                for (String fn : fns)
                {
                    String[] tokens = fn.contains(sep) ? fn.split(sep) : fn.split("\"");

                    StringBuffer name = new StringBuffer();

                    // only include parts of the filename that are unique
                    for (int i = 0; i < tokens.length - 1; i++)
                    {
                        if (!proto.contains(tokens[i]))
                        {
                            name.append(tokens[i]);
                            name.append(".");
                        }
                    }

                    // always include the basename
                    name.append(tokens[tokens.length - 1]);

                    names.put(fn, PathUtils.noext(name.toString()));
                }
            }

            return names;
        }
        else
        {
            Map<String, String> names = Maps.newLinkedHashMap();
            for (String fn : fns)
            {
                names.put(fn, PathUtils.noext(PathUtils.basename(fn)));
            }
            return names;
        }
    }

    public synchronized void deleteAll()
    {
        if (SwingUtils.getDecision("This will remove all data in your workspace, are you sure?"))
        {
            Viewer.getInstance().data.removeAll();
        }
    }

    public synchronized void printDiagnostics()
    {
        System.gc();
        Runtime rt = Runtime.getRuntime();
        long usage = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;

        Viewables data = Viewer.getInstance().data;
        System.out.println("");
        System.out.println("DIAGNOSTICS:");
        System.out.println("");
        System.out.println("  Bounding box: " + Viewer.getInstance().gui.canvas.render3D.box);
        System.out.println("");
        System.out.println("  Number of data objects: " + data.size());
        System.out.println("  Total memory usage (MB): " + usage);

        for (int i = 0; i < data.size(); i++)
        {
            Viewable record = data.getViewable(i);

            System.out.println("");
            System.out.println("    Data Object " + i);
            System.out.println("      Visible: " + record.getVisible());
            System.out.println("      Name: " + record.getName());
            System.out.println("      Filename: " + record.getFilename());
            System.out.println("      Datatype: " + record.getClass());
            System.out.println("      Has data: " + record.hasData());
            if (record.hasData())
            {
                System.out.println("      Memory footprint: " + ObjectGraphMeasurer.measure(record.getData()));
            }
        }
        System.out.println("");
    }

    @Override
    public synchronized void actionPerformed(ActionEvent event)
    {
        final String action = event.getActionCommand();
        long now = System.currentTimeMillis();

        if (now < this.actionLastTime + Constants.KEY_WAIT)
        {
            return;
        }

        this.actionLastName = action;
        this.actionLastTime = System.currentTimeMillis();

        try
        {
            if (this.actions.containsKey(action))
            {
                this.actions.get(action).run();
            }
            else if (action.equals(Constants.FILE_MENU_CLOBBER))
            {
                AbstractButton aButton = (AbstractButton) event.getSource();
                boolean selected = aButton.getModel().isSelected();
                Viewer.getInstance().settings.clobber = selected;
            }
            else if (action.equals(Constants.FILE_MENU_BACKUPS))
            {
                AbstractButton aButton = (AbstractButton) event.getSource();
                boolean selected = aButton.getModel().isSelected();
                Viewer.getInstance().settings.backup = selected;
            }
            else if (ViewableType.hasName(action.replaceFirst(Constants.LOAD_PREFIX, "")))
            {
                final ViewableType type = ViewableType.getFromName(action.replaceFirst(Constants.LOAD_PREFIX, ""));
                final List<String> fns = Viewer.getInstance().gui.chooseLoadFiles("Open " + type.getText() + "...");
                new Thread(() -> readQueue(type, fns, Viewer.FULLPATH)).start();
            }
            else
            {
                this.setStatusError("unknown action: " + action);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            this.setStatusError("action failed: " + e.getMessage());
        }
    }

}
