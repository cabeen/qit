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

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/** This class exists for cases when you want combo boxes to share their data
 *  but not their selection (the standard model includes both selection and the list).
 *
 *  source: http://www.coderanch.com/t/615857/GUI/java/static-list-multiple-JComboBoxes
 *  changes: modified to use generics
 */
class SharedComboModel<E> extends DefaultComboBoxModel<E>
{
    private DefaultListModel<E> listModel;
    private final ListDataListener listener = new ListDataListener()
    {
        public void intervalAdded(ListDataEvent e)
        {
            fireIntervalAdded(this, e.getIndex0(), e.getIndex1());
        }

        public void intervalRemoved(ListDataEvent e)
        {
            int index0 = e.getIndex0();
            int index1 = e.getIndex1();
            for (int index = index0; index <= index1; index++)
            {
                if (listModel.get(index) == getSelectedItem())
                {
                    setSelectedItem(getSize() == 1 ? null : getElementAt(index + 1));
                }
            }
            fireIntervalRemoved(this, index0, index1);
        }

        public void contentsChanged(ListDataEvent e)
        {
            fireContentsChanged(this, e.getIndex0(), e.getIndex1());
        }
    };

    public SharedComboModel(DefaultListModel listModel)
    {
        this.setListModel(listModel);
    }

    public DefaultListModel getListModel()
    {
        return listModel;
    }

    public void setListModel(DefaultListModel listModel)
    {
        if (this.listModel != null)
        {
            this.listModel.removeListDataListener(listener);
        }
        listModel.addListDataListener(listener);
        this.listModel = listModel;
    }

    @Override
    public int getSize()
    {
        return listModel.getSize();
    }

    @Override
    public E getElementAt(int index)
    {
        return listModel.getElementAt(index);
    }

    @Override
    public void addElement(E anObject)
    {
        listModel.addElement(anObject);
    }

    @Override
    public void addListDataListener(ListDataListener l)
    {
        listModel.addListDataListener(l);
    }

    @Override
    public void removeListDataListener(ListDataListener l)
    {
        listModel.removeListDataListener(l);
    }
}