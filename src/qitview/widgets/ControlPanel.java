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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JPanel;

public class ControlPanel extends JPanel
{
    private static final long serialVersionUID = -6479677386188502806L;

    private int singleFill = GridBagConstraints.HORIZONTAL;
    private int singleAnchor = GridBagConstraints.CENTER;
    private int leftFill = GridBagConstraints.HORIZONTAL;
    private int leftAnchor = GridBagConstraints.EAST;
    private int rightFill = GridBagConstraints.HORIZONTAL;
    private int rightAnchor = GridBagConstraints.WEST;

    private int count = 0;

    public ControlPanel()
    {
        super(new GridBagLayout());
    }
    
    public ControlPanel withSingleFill(int fill)
    {
        this.singleFill = fill;
        return this;
    }

    public ControlPanel withSingleAnchor(int anchor)
    {
        this.singleAnchor = anchor;
        return this;
    }

    public ControlPanel withLeftFill(int fill)
    {
        this.leftFill = fill;
        return this;
    }

    public ControlPanel withLeftAnchor(int anchor)
    {
        this.leftAnchor = anchor;
        return this;
    }

    public ControlPanel withRightFill(int fill)
    {
        this.rightFill = fill;
        return this;
    }

    public ControlPanel withRightAnchor(int anchor)
    {
        this.rightAnchor = anchor;
        return this;
    }

    public void addControl(String name, Component comp)
    {
        addControl(name, "", comp);
    }

    public void addControl(String name, String tip, Component comp)
    {
        BasicLabel label = new BasicLabel(name);
        label.setToolTipText(tip);

        addControl(label, comp);
    }

    public void addControl(Component comp)
    {
        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0;
        cc.gridwidth = 2;
        cc.fill = this.singleFill;
        cc.anchor = this.singleAnchor;
        this.add(comp, cc);
        this.count += 1;
    }

    public void addControl(Component left, Component right)
    {
        {
            GridBagConstraints cc = new GridBagConstraints();
            cc.gridx = 0;
            cc.fill = this.leftFill;
            cc.anchor = this.leftAnchor;
            this.add(left, cc);
        }
        {
            GridBagConstraints cc = new GridBagConstraints();
            cc.gridx = 1;
            cc.fill = this.leftFill;
            cc.anchor = this.rightAnchor;
            this.add(right, cc);
        }

        this.count += 1;
    }

    public int getNumControls()
    {
        return this.count;
    }
}