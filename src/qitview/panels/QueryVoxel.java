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


package qitview.panels;

import com.jogamp.opengl.GL2;
import qit.base.Model;
import qit.base.ModelType;
import qit.base.structs.ObservableInstance;
import qit.base.structs.Pair;
import qit.data.datasets.*;
import qit.data.source.VectSource;
import qit.data.utils.mri.ModelUtils;
import qitview.main.Constants;
import qitview.main.Viewer;
import qitview.models.Slicer;
import qitview.models.Viewable;
import qitview.models.VolumeSlicePlane;
import qitview.views.MaskView;
import qitview.views.VolumeView;
import qitview.widgets.*;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unchecked")
public class QueryVoxel extends JPanel
{
    public final static DecimalFormat DATA_FORMAT = new DecimalFormat("#0.00000");
    public final static DecimalFormat COORD_FORMAT = new DecimalFormat("#0.00");
    public final static int MAX_IDX = 10000;

    public transient ObservableInstance observable = new ObservableInstance();

    private boolean showBox = true;
    private boolean showGuide = false;
    private VoxelQueryTableModel model;
    private BasicTable table;

    public transient Vect query = VectSource.create3D(0, 0, 0);
    public transient Sampling reference = null;

    public QueryVoxel()
    {
        this.model = new VoxelQueryTableModel();
        this.table = new BasicTable(this.model);

        this.table.setFocusable(false);
        this.table.setCellSelectionEnabled(true);
        this.table.setRowSelectionAllowed(true);
        this.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        this.table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.table.setDefaultRenderer(Color.class, new ColorCellRenderer(false));

        ControlPanel controls = new ControlPanel();
        {
            final JCheckBox showBoxCheck = new JCheckBox("Show box");
            showBoxCheck.setSelected(this.showBox);
            showBoxCheck.addActionListener((a) ->
            {
                this.showBox = showBoxCheck.isSelected();
            });
            controls.add(showBoxCheck);

            final JCheckBox showGuideCheck = new JCheckBox("Show guides");
            showGuideCheck.setSelected(this.showGuide);
            showGuideCheck.addActionListener((a) ->
            {
                this.showGuide = showGuideCheck.isSelected();
            });
            controls.add(showGuideCheck);

            final BasicSpinner spinI = new BasicSpinner(new SpinnerNumberModel(0, -MAX_IDX, MAX_IDX, 1));
            final BasicSpinner spinJ = new BasicSpinner(new SpinnerNumberModel(0, -MAX_IDX, MAX_IDX, 1));
            final BasicSpinner spinK = new BasicSpinner(new SpinnerNumberModel(0, -MAX_IDX, MAX_IDX, 1));

            final BasicTextField worldX = new BasicTextField("0");
            final BasicTextField worldY = new BasicTextField("0");
            final BasicTextField worldZ = new BasicTextField("0");

            for (BasicSpinner s : new BasicSpinner[]{spinI, spinJ, spinK})
            {
                ((BasicSpinner.DefaultEditor) s.getEditor()).getTextField().setColumns(3);
            }

            for (BasicSpinner s : new BasicSpinner[]{spinI, spinJ, spinK})
            {
                s.addChangeListener(e ->
                {
                    if (QueryVoxel.this.reference != null)
                    {
                        int i = (Integer) spinI.getValue();
                        int j = (Integer) spinJ.getValue();
                        int k = (Integer) spinK.getValue();

                        QueryVoxel.this.query.set(QueryVoxel.this.reference.world(i, j, k));

                        worldX.setText(COORD_FORMAT.format(QueryVoxel.this.query.getX()));
                        worldY.setText(COORD_FORMAT.format(QueryVoxel.this.query.getY()));
                        worldZ.setText(COORD_FORMAT.format(QueryVoxel.this.query.getZ()));
                    }
                    else
                    {
                        QueryVoxel.this.query.setAll(0);
                    }

                    QueryVoxel.this.changed();
                });
            }

            for (BasicTextField f : new BasicTextField[]{worldX, worldY, worldZ})
            {
                f.addActionListener(e ->
                {
                    try
                    {
                        double x1 = Double.parseDouble(worldX.getText());
                        double y1 = Double.parseDouble(worldY.getText());
                        double z = Double.parseDouble(worldZ.getText());

                        QueryVoxel.this.query.set(0, x1);
                        QueryVoxel.this.query.set(1, y1);
                        QueryVoxel.this.query.set(2, z);

                        Vect query1 = QueryVoxel.this.query;
                        Sample nearest = QueryVoxel.this.reference.nearest(query1);

                        spinI.setValue(nearest.getI());
                        spinJ.setValue(nearest.getJ());
                        spinK.setValue(nearest.getK());

                        QueryVoxel.this.changed();
                    }
                    catch (RuntimeException re)
                    {
                        Viewer.getInstance().gui.setStatusMessage("warning: failed to parse world coordinates");
                    }
                });
            }

            QueryVoxel.this.observable.addObserver((o, arg) ->
            {
                if (QueryVoxel.this.reference != null)
                {
                    Vect query12 = QueryVoxel.this.query;
                    Sample nearest = QueryVoxel.this.reference.nearest(query12);

                    spinI.setValue(nearest.getI());
                    spinJ.setValue(nearest.getJ());
                    spinK.setValue(nearest.getK());
                }
                else
                {
                    spinI.setValue(0);
                    spinJ.setValue(0);
                    spinK.setValue(0);
                }

                worldX.setText(COORD_FORMAT.format(QueryVoxel.this.query.getX()));
                worldY.setText(COORD_FORMAT.format(QueryVoxel.this.query.getY()));
                worldZ.setText(COORD_FORMAT.format(QueryVoxel.this.query.getZ()));
            });

            {
                JPanel gridPanel = new JPanel();
                gridPanel.setLayout(new GridLayout(1, 4));
                gridPanel.add(new BasicLabel("Voxel"));
                gridPanel.add(spinI);
                gridPanel.add(spinJ);
                gridPanel.add(spinK);
                controls.addControl(gridPanel);
            }
            {
                JPanel gridPanel = new JPanel();
                gridPanel.setLayout(new GridLayout(1, 4));
                gridPanel.add(new BasicLabel("World"));
                gridPanel.add(worldX);
                gridPanel.add(worldY);
                gridPanel.add(worldZ);
                controls.addControl(gridPanel);
            }
        }

        JScrollPane scroll = BasicTable.createStripedJScrollPane(QueryVoxel.this.table);

        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        this.add(controls, BorderLayout.NORTH);
        this.add(scroll, BorderLayout.CENTER);
        this.setPreferredSize(new Dimension(Constants.USER_WIDTH - 30, 0));
    }

    public void setQuery(Vect v)
    {
        this.query = v;
        this.observable.changed();
        this.changed();
    }

    public void setQuery(Sampling r, Vect v)
    {
        this.reference = r;
        this.query = v;
        this.observable.changed();
        this.changed();
    }

    public void clearQuery()
    {
        this.reference = null;
        this.query.setAll(0);
        this.observable.changed();
        this.changed();
    }

    public void changed()
    {
        if (this.model.getRowCount() > 0)
        {
            this.model.removeRowAll();
        }

        if (this.reference != null)
        {
            Sample nearest = QueryVoxel.this.reference.nearest(this.query);

            if (!this.reference.contains(nearest))
            {
                nearest = this.reference.nearestInside(this.query);
                this.query = this.reference.world(nearest);
            }

            Viewables viewables = Viewer.getInstance().data;
            for (int idx = 0; idx < viewables.size(); idx++)
            {
                String name = viewables.getName(idx);
                Viewable<?> viewable = viewables.getViewable(idx);

                if (viewable instanceof VolumeView && viewable.hasData())
                {
                    VolumeView viewOther = ((VolumeView) viewable);
                    Volume volumeOther = viewOther.getData();
                    Sampling samplingOther = volumeOther.getSampling();
                    Sample nearestOther = samplingOther.nearest(this.query);

                    if (samplingOther.contains(nearestOther))
                    {
                        Vect val = volumeOther.get(nearestOther);

                        if (!volumeOther.getModel().equals(ModelType.Vect))
                        {
                            Model dmodel = ModelUtils.proto(volumeOther.getModel(), volumeOther.getDim());
                            dmodel.setEncoding(val);
                            List<String> features = dmodel.features();
                            for (String feature : features)
                            {
                                Vect sval = dmodel.feature(feature);
                                String sname = String.format("%s[%s]", name, feature);
                                if (sval.size() == 1)
                                {
                                    this.model.addRow(Pair.of(sname, DATA_FORMAT.format(sval.get(0))));
                                }
                            }
                        }

                        if (val.size() == 1)
                        {
                            this.model.addRow(Pair.of(name, DATA_FORMAT.format(val.get(0))));
                        }
                        else
                        {
                            // Older approach:
                            // int c = viewOther.getChannel();
                            // String sname = String.format("%s[%d]", name, c);
                            // this.model.addRow(Pair.of(sname, DATA_FORMAT.format(val.get(c))));

                            for (int i = 0; i < val.size(); i++)
                            {
                                String sname = String.format("%s[%d]", name, i);
                                this.model.addRow(Pair.of(sname, DATA_FORMAT.format(val.get(i))));
                            }
                        }
                    }
                }

                if (viewable instanceof MaskView && viewable.hasData())
                {
                    Mask maskOther = ((MaskView) viewable).getData();
                    Sampling samplingOther = maskOther.getSampling();
                    Sample nearestOther = samplingOther.nearest(this.query);

                    if (samplingOther.contains(nearestOther))
                    {
                        int val = maskOther.get(nearest);
                        this.model.addRow(Pair.of(name, Integer.toString(val)));

                        if (maskOther.hasName(val))
                        {
                            this.model.addRow(Pair.of(name, maskOther.getName(val)));
                        }
                    }
                }
            }
        }
    }

    private class VoxelQueryTableModel extends RowTableModel<Pair<String, String>>
    {
        VoxelQueryTableModel()
        {
            super(Arrays.asList(new String[]{"Name", "Value"}));
            setRowClass(Pair.class);
            setColumnClass(0, String.class);
            setColumnClass(1, String.class);
        }

        @Override
        public boolean isCellEditable(int row, int column)
        {
            //all cells false
            return false;
        }

        @Override
        public Object getValueAt(int row, int column)
        {
            Pair<String, String> record = getRow(row);

            switch (column)
            {
                case 0:
                    return record.a;
                case 1:
                    return record.b;
                default:
                    return null;
            }
        }
    }

    public void render3D(GL2 gl)
    {
        this.render(gl, null);
    }

    public void render(GL2 gl, VolumeSlicePlane plane)
    {
        if (!Viewer.getInstance().gui.shownQuery())
        {
            return;
        }

        if (this.reference != null)
        {
            Sampling sampling = QueryVoxel.this.reference;

            Sample nearest = sampling.nearest(this.query);
            Vect scen = VectSource.create3D(nearest.getI(), nearest.getJ(), nearest.getK());

            Vect s000 = sampling.world(scen.plus(VectSource.create3D(-0.5, -0.5, -0.5)));
            Vect s001 = sampling.world(scen.plus(VectSource.create3D(-0.5, -0.5, 0.5)));
            Vect s010 = sampling.world(scen.plus(VectSource.create3D(-0.5, 0.5, -0.5)));
            Vect s011 = sampling.world(scen.plus(VectSource.create3D(-0.5, 0.5, 0.5)));
            Vect s100 = sampling.world(scen.plus(VectSource.create3D(0.5, -0.5, -0.5)));
            Vect s101 = sampling.world(scen.plus(VectSource.create3D(0.5, -0.5, 0.5)));
            Vect s110 = sampling.world(scen.plus(VectSource.create3D(0.5, 0.5, -0.5)));
            Vect s111 = sampling.world(scen.plus(VectSource.create3D(0.5, 0.5, 0.5)));

            Vect i0 = sampling.world(VectSource.create3D(0, nearest.getJ(), nearest.getK()));
            Vect i1 = sampling.world(VectSource.create3D(scen.getX() - 0.5, nearest.getJ(), nearest.getK()));
            Vect i2 = sampling.world(VectSource.create3D(scen.getX() + 0.5, nearest.getJ(), nearest.getK()));
            Vect i3 = sampling.world(VectSource.create3D(sampling.numI() - 1, nearest.getJ(), nearest.getK()));

            Vect j0 = sampling.world(VectSource.create3D(nearest.getI(), 0, nearest.getK()));
            Vect j1 = sampling.world(VectSource.create3D(nearest.getI(), scen.getY() - 0.5, nearest.getK()));
            Vect j2 = sampling.world(VectSource.create3D(nearest.getI(), scen.getY() + 0.5, nearest.getK()));
            Vect j3 = sampling.world(VectSource.create3D(nearest.getI(), sampling.numJ() - 1, nearest.getK()));

            Vect k0 = sampling.world(VectSource.create3D(nearest.getI(), nearest.getJ(), 0));
            Vect k1 = sampling.world(VectSource.create3D(nearest.getI(), nearest.getJ(), scen.getZ() - 0.5));
            Vect k2 = sampling.world(VectSource.create3D(nearest.getI(), nearest.getJ(), scen.getZ() + 0.5));
            Vect k3 = sampling.world(VectSource.create3D(nearest.getI(), nearest.getJ(), sampling.numK() - 1));

            gl.glDisable(GL2.GL_LIGHTING);
            gl.glColor3d(255, 0, 0);
            gl.glEnable(GL2.GL_POINT_SMOOTH);
            gl.glLineWidth(2);

            // draw the guides
            Slicer slicer = Viewer.getInstance().control.getSlicer(sampling);

            boolean drawI = true;
            boolean drawJ = true;
            boolean drawK = true;
            boolean drawV = true;

            boolean matchI = nearest.getI() == slicer.idxI();
            boolean matchJ = nearest.getJ() == slicer.idxJ();
            boolean matchK = nearest.getK() == slicer.idxK();

            if (plane != null)
            {
                switch (plane)
                {
                    case I:
                        drawI = false;
                        drawJ = matchI;
                        drawK = matchI;
                        drawV = matchI;
                        break;
                    case J:
                        drawI = matchJ;
                        drawJ = false;
                        drawK = matchJ;
                        drawV = matchJ;
                        break;
                    case K:
                        drawI = matchK;
                        drawJ = matchK;
                        drawK = false;
                        drawV = matchK;
                        break;
                }
            }

            if (this.showGuide)
            {
                if (drawI)
                {
                    gl.glBegin(GL2.GL_LINE_STRIP);
                    gl.glVertex3d(i0.getX(), i0.getY(), i0.getZ());
                    gl.glVertex3d(i1.getX(), i1.getY(), i1.getZ());
                    gl.glEnd();

                    gl.glBegin(GL2.GL_LINE_STRIP);
                    gl.glVertex3d(i2.getX(), i2.getY(), i2.getZ());
                    gl.glVertex3d(i3.getX(), i3.getY(), i3.getZ());
                    gl.glEnd();
                }

                if (drawJ)
                {
                    gl.glBegin(GL2.GL_LINE_STRIP);
                    gl.glVertex3d(j0.getX(), j0.getY(), j0.getZ());
                    gl.glVertex3d(j1.getX(), j1.getY(), j1.getZ());
                    gl.glEnd();

                    gl.glBegin(GL2.GL_LINE_STRIP);
                    gl.glVertex3d(j2.getX(), j2.getY(), j2.getZ());
                    gl.glVertex3d(j3.getX(), j3.getY(), j3.getZ());
                    gl.glEnd();
                }

                if (drawK)
                {
                    gl.glBegin(GL2.GL_LINE_STRIP);
                    gl.glVertex3d(k0.getX(), k0.getY(), k0.getZ());
                    gl.glVertex3d(k1.getX(), k1.getY(), k1.getZ());
                    gl.glEnd();

                    gl.glBegin(GL2.GL_LINE_STRIP);
                    gl.glVertex3d(k2.getX(), k2.getY(), k2.getZ());
                    gl.glVertex3d(k3.getX(), k3.getY(), k3.getZ());
                    gl.glEnd();
                }
            }

            if (this.showBox && drawV)
            {
                // draw the box
                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glVertex3d(s000.getX(), s000.getY(), s000.getZ());
                gl.glVertex3d(s001.getX(), s001.getY(), s001.getZ());
                gl.glVertex3d(s011.getX(), s011.getY(), s011.getZ());
                gl.glVertex3d(s010.getX(), s010.getY(), s010.getZ());
                gl.glVertex3d(s000.getX(), s000.getY(), s000.getZ());
                gl.glEnd();

                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glVertex3d(s100.getX(), s100.getY(), s100.getZ());
                gl.glVertex3d(s101.getX(), s101.getY(), s101.getZ());
                gl.glVertex3d(s111.getX(), s111.getY(), s111.getZ());
                gl.glVertex3d(s110.getX(), s110.getY(), s110.getZ());
                gl.glVertex3d(s100.getX(), s100.getY(), s100.getZ());
                gl.glEnd();

                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glVertex3d(s000.getX(), s000.getY(), s000.getZ());
                gl.glVertex3d(s100.getX(), s100.getY(), s100.getZ());
                gl.glEnd();

                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glVertex3d(s001.getX(), s001.getY(), s001.getZ());
                gl.glVertex3d(s101.getX(), s101.getY(), s101.getZ());
                gl.glEnd();

                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glVertex3d(s011.getX(), s011.getY(), s011.getZ());
                gl.glVertex3d(s111.getX(), s111.getY(), s111.getZ());
                gl.glEnd();

                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glVertex3d(s010.getX(), s010.getY(), s010.getZ());
                gl.glVertex3d(s110.getX(), s110.getY(), s110.getZ());
                gl.glEnd();
            }
        }
    }
}
