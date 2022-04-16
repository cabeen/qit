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

import java.util.Observable;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import qit.math.structs.VectFunction;
import qitview.main.Viewer;

public class UnitMapDialog extends Observable
{
    String name;
    UnitMapControl control;

    public UnitMapDialog(final String name, double min, double max)
    {
        this.name = name;
        this.control = new UnitMapControl(min, max);
        this.control.addObserver((a, b) ->
        {
            this.setChanged();
            this.notifyObservers(name);
        });
    }

    public void set(double min, double max)
    {
        this.control.set(min, max);
    }

    public VectFunction toFunction()
    {
        return this.control.toFuction();
    }

    public void show()
    {
        JDialog dialog = new JDialog(Viewer.getInstance().gui.getFrame());
        dialog.setTitle("Unitmap for " + name);
        dialog.add(this.control.getPanel());
        dialog.pack();
        dialog.setResizable(false);
        dialog.setVisible(true);
    }

    public class UnitMapControl extends Observable
    {
        final UnitMapWidget widget = new UnitMapWidget();
        private JPanel panel = new JPanel();
        private VectFunction normalize = null;

        public UnitMapControl(double min, double max)
        {
            this();
            this.set(min, max);
        }

        public UnitMapControl()
        {
            this.panel.setLayout(new BoxLayout(this.panel, BoxLayout.PAGE_AXIS));
            this.panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

            {
                this.widget.addObserver((a, b) ->
                {
                    this.normalize = UnitMapControl.this.widget.toFunction();
                });
                this.normalize = this.widget.toFunction();
                this.panel.add(this.widget.getPanel());
            }
            {
                BasicButton elem = new BasicButton("Apply");
                elem.addActionListener(v ->
                {
                    this.change();
                });

                JPanel subpanel = new JPanel();
                subpanel.setLayout(new BoxLayout(subpanel, BoxLayout.LINE_AXIS));
                subpanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
                subpanel.add(Box.createHorizontalGlue());
                subpanel.add(elem);

                this.panel.add(subpanel);
            }
        }

        public void set(double min, double max)
        {
            this.widget.set(min, max);
            this.change();
        }

        public void change()
        {
            this.setChanged();
            this.notifyObservers();
        }

        public JPanel getPanel()
        {
            return this.panel;
        }

        public VectFunction toFuction()
        {
            return this.normalize;
        }
    }
}
