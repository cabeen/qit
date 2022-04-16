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

import java.text.DecimalFormat;
import qit.data.datasets.Affine;
import qitview.widgets.BasicLabel;
import qitview.widgets.ControlPanel;

public class AffineView extends AbstractView<Affine>
{
    private transient Affine pdata = null;

    public AffineView()
    {
        super();
    }

    protected ControlPanel makeInfoControls()
    {
        ControlPanel infoPanel = new ControlPanel();
        infoPanel.addControl("Type: ", new BasicLabel(this.toString()));
        infoPanel.addControl(" ", new BasicLabel());

        final DecimalFormat df = new DecimalFormat("0.00##");

        final BasicLabel det3 = new BasicLabel("");
        final BasicLabel det4 = new BasicLabel("");
        infoPanel.addControl("det3: ", det3);
        infoPanel.addControl("det4: ", det4);
        this.observable.addObserver((o, arg) ->
        {
            if (AffineView.this.hasData())
            {
                det3.setText(df.format(AffineView.this.data.mat4().sub(0, 3, 0, 3).det()));
                det4.setText(df.format(AffineView.this.data.mat4().det()));
            }
            else
            {
                det3.setText("NA");
                det4.setText("NA");
            }
        });


        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 3; j++)
            {
                final BasicLabel a = new BasicLabel("");
                infoPanel.addControl("a" + i + "" + j + ": ", a);
                final int ii = i;
                final int jj = i;
                this.observable.addObserver((o, arg) ->
                {
                    if (AffineView.this.hasData())
                    {
                        a.setText(df.format(AffineView.this.data.mat4().get(ii, jj)));
                    }
                    else
                    {
                        a.setText("NA");
                    }
                });
            }
        }

        for (int i = 0; i < 3; i++)
        {
            final BasicLabel b = new BasicLabel("");
            infoPanel.addControl("b" + i + ": ", b);
            final int ii = i;
            this.observable.addObserver((o, arg) ->
            {
                if (AffineView.this.hasData())
                {
                    b.setText(df.format(AffineView.this.data.mat4().get(ii, 3)));
                }
                else
                {
                    b.setText("NA");
                }
            });
        }

        return infoPanel;
    }

    @Override
    public synchronized AffineView setData(Affine d)
    {
        this.bounds = null;
        super.setData(d);

        return this;
    }

    @Override
    public synchronized Affine getData()
    {
        return this.data;
    }
}
