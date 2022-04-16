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

package qitview.models;

import javax.swing.ButtonModel;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import qit.data.datasets.Matrix;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qitview.main.Viewer;

public class Slicer implements ChangeListener
{
    private transient SpinnerNumberModel modelSpinnerI;
    private transient SpinnerNumberModel modelSpinnerJ;
    private transient SpinnerNumberModel modelSpinnerK;

    private transient ButtonModel modelButtonI;
    private transient ButtonModel modelButtonJ;
    private transient ButtonModel modelButtonK;

    private Sampling reference;

    private boolean showI;
    private boolean showJ;
    private boolean showK;

    private int idxI = 0;
    private int idxJ = 0;
    private int idxK = 0;

    private int numI;
    private int numJ;
    private int numK;

    private Slicer()
    {
    }

    public Slicer(Sampling sampling)
    {
        this();
        this.set(sampling);
    }

    public boolean contains(Sample sample)
    {
        return this.idxI == sample.getI() || this.idxJ == sample.getJ() || this.idxK == sample.getK();
    }

    public void set(Sampling sampling)
    {
        this.reference = sampling;

        this.idxI = 0;
        this.idxJ = 0;
        this.idxK = 0;

        this.showI = true;
        this.showJ = true;
        this.showK = true;

        this.numI = sampling.numI();
        this.numJ = sampling.numJ();
        this.numK = sampling.numK();
    }

    public boolean compatible(Sampling sampling)
    {
        // use approximate tests here since some images may have different orientations
        // in those cases the rotation may be approximately equivalent but not strictly equal

        return this.reference.compatible(sampling);
    }

    public SpinnerNumberModel modelSpinnerI()
    {
        if (this.modelSpinnerI == null)
        {
            this.modelSpinnerI = new SpinnerNumberModel(0, 0, this.numI, 1);
        }

        return this.modelSpinnerI;
    }

    public SpinnerNumberModel modelSpinnerJ()
    {
        if (this.modelSpinnerJ == null)
        {
            this.modelSpinnerJ = new SpinnerNumberModel(0, 0, this.numJ, 1);
        }

        return this.modelSpinnerJ;
    }

    public SpinnerNumberModel modelSpinnerK()
    {
        if (this.modelSpinnerK == null)
        {
            this.modelSpinnerK = new SpinnerNumberModel(0, 0, this.numK, 1);
        }

        return this.modelSpinnerK;
    }
    
    public ButtonModel modelButtonI()
    {
        if (this.modelButtonI == null)
        {
            this.modelButtonI = new JToggleButton.ToggleButtonModel();
            this.modelButtonI.setSelected(this.showK);
        }

        return this.modelButtonI;
    }

    public ButtonModel modelButtonJ()
    {
        if (this.modelButtonJ == null)
        {
            this.modelButtonJ = new JToggleButton.ToggleButtonModel();
            this.modelButtonJ.setSelected(this.showJ);
        }

        return this.modelButtonJ;
    }

    public ButtonModel modelButtonK()
    {
        if (this.modelButtonK == null)
        {
            this.modelButtonK = new JToggleButton.ToggleButtonModel();
            this.modelButtonK.setSelected(this.showI);
        }

        return this.modelButtonK;
    }

    public boolean showK()
    {
        return this.showK;
    }
    
    public boolean showJ()
    {
        return this.showJ;
    }
    
    public boolean showI()
    {
        return this.showI;
    }

    public Sample sample()
    {
        return new Sample(this.idxI, this.idxJ, this.idxK);
    }
    
    public Integer idxI()
    {
        return this.idxI;
    }

    public Integer idxJ()
    {
        return this.idxJ;
    }

    public Integer idxK()
    {
        return this.idxK;
    }

    public boolean stepI(int n)
    {
        int ni = this.idxI + n;
        if (ni >= 0 && ni < this.numI)
        {
            this.idxI = ni;
            this.modelSpinnerI().setValue(this.idxI);
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean stepJ(int n)
    {
        int nj = this.idxI + n;
        if (nj >= 0 && nj < this.numJ)
        {
            this.idxJ = nj;
            this.modelSpinnerJ().setValue(this.idxJ);
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean stepK(int n)
    {
        int nk = this.idxK + n;
        if (nk >= 0 && nk < this.numK)
        {
            this.idxK = nk;
            this.modelSpinnerK().setValue(this.idxK);
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean set(Sample sample)
    {
        return this.set(sample.getI(), sample.getJ(), sample.getK());
    }

    public boolean set(int i, int j, int k)
    {
        return this.set(i, j, k, this.showI, this.showJ, this.showK);
    }

    public boolean setI(int v)
    {
        return this.set(v, this.idxJ, this.idxK, this.showI, this.showJ, this.showK);
    }

    public boolean setJ(int v)
    {
        return this.set(this.idxI, v, this.idxK, this.showI, this.showJ, this.showK);
    }

    public boolean setK(int v)
    {
        return this.set(this.idxI, this.idxJ, v, this.showI, this.showJ, this.showK);
    }

    public boolean setShowK(boolean v)
    {
        return this.set(this.idxI, this.idxJ, this.idxI, this.showI, this.showJ, v);
    }

    public boolean setShowJ(boolean v)
    {
        return this.set(this.idxI, this.idxJ, this.idxI, this.showK, v, this.showK);
    }

    public boolean setShowI(boolean v)
    {
        return this.set(this.idxI, this.idxJ, this.idxK, v, this.showJ, this.showK);
    }

    public boolean toggleShow(VolumeSlicePlane plane)
    {
        switch (plane)
        {
            case I:
                return this.toggleShowI();
            case J:
                return this.toggleShowJ();
            case K:
                return this.toggleShowK();
            default:
                throw new RuntimeException("invalid slice plane: " + plane.toString());
        }
    }

    public boolean toggleShowI()
    {
        return this.set(this.idxI, this.idxJ, this.idxK, this.showI ^ true, this.showJ, this.showK);
    }

    public boolean toggleShowJ()
    {
        return this.set(this.idxI, this.idxJ, this.idxK, this.showI, this.showJ ^ true, this.showK);
    }

    public boolean toggleShowK()
    {
        return this.set(this.idxI, this.idxJ, this.idxK, this.showI, this.showJ, this.showK ^ true);
    }
    
    private boolean set(int nidxI, int nidxJ, int nidxK, boolean nshowI, boolean nshowJ, boolean nshowK)
    {
        boolean updateI = false;
        boolean updateJ = false;
        boolean updateK = false;

        if (nidxI != this.idxI && nidxI >= 0 && nidxI < this.numI)
        {
            this.idxI = nidxI;
            updateI = true;
        }
        if (nidxJ != this.idxJ && nidxJ >= 0 && nidxJ < this.numJ)
        {
            this.idxJ = nidxJ;
            updateJ = true;
        }
        if (nidxK != this.idxK && nidxK >= 0 && nidxK < this.numK)
        {
            this.idxK = nidxK;
            updateK = true;
        }

        if (updateI)
        {
            this.modelSpinnerI().setValue(this.idxI);
            this.modelButtonI().setSelected(this.showI);
        }

        if (updateJ)
        {
            this.modelSpinnerJ().setValue(this.idxJ);
            this.modelButtonJ().setSelected(this.showJ);
        }

        if (updateK)
        {
            this.modelSpinnerK().setValue(this.idxK);
            this.modelButtonK().setSelected(this.showK);
        }

        boolean any = updateI || updateJ || updateK;
        if (any)
        {
            for (Viewable<? extends Object> r : Viewer.getInstance().data.getAll())
            {
                if (r.hasData() && (r instanceof Sliceable))
                {
                    Sliceable rs = (Sliceable) r;
                    Slicer s = rs.getSlicer();
                    if (s != null && s.equals(this))
                    {
                        if (updateI)
                        {
                            rs.updateI();
                        }
                        if (updateJ)
                        {
                            rs.updateJ();
                        }
                        if (updateK)
                        {
                            rs.updateK();
                        }
                    }
                }
            }
        }

        // changes in the "show" box should not be sent to linked data views
        // they should check for this every rendering loop
        if (nshowI != this.showI)
        {
            this.showI = nshowI;
            this.modelButtonI.setSelected(this.showI);
        }
        if (nshowJ != this.showJ)
        {
            this.showJ = nshowJ;
            this.modelButtonJ.setSelected(this.showJ);
        }
        if (nshowK != this.showK)
        {
            this.showK = nshowK;
            this.modelButtonK.setSelected(this.showK);
        }

        return any;
    }

    public void stateChanged(ChangeEvent e)
    {
        int nSliceI = this.modelSpinnerI().getNumber().intValue();
        int nSliceJ = this.modelSpinnerJ().getNumber().intValue();
        int nSliceK = this.modelSpinnerK().getNumber().intValue();

        boolean nShowI = this.modelButtonI.isSelected();
        boolean nShowJ = this.modelButtonJ.isSelected();
        boolean nShowK = this.modelButtonK.isSelected();
        
        this.set(nSliceI, nSliceJ, nSliceK, nShowI, nShowJ, nShowK);
    }

    public int hashCode()
    {
        int hc = 0;
        hc += this.numI;
        hc += this.numJ;
        hc += this.numK;

        hc += this.numI * this.numJ;
        hc += this.numI * this.numK;
        hc += this.numJ * this.numK;
        hc += this.numI * this.numJ * this.numK;

        return hc;
    }

    public boolean equals(Object obj)
    {
        if (obj instanceof Slicer)
        {
            Slicer cast = (Slicer) obj;
            return cast.numI == this.numI && cast.numJ == this.numJ && cast.numK == this.numK;
        }
        else
        {
            return false;
        }
    }
}