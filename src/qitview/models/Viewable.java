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

import com.jogamp.opengl.GL2;
import qit.base.Dataset;
import qit.math.structs.Box;

import javax.swing.JPanel;
import java.io.IOException;
import java.util.List;
import java.util.Observable;
import java.util.function.Consumer;

public interface Viewable<E extends Dataset>
{
    /**
     * @return a panel for controlling the how the data is rendererd
     */
    JPanel getRenderPanel();

    /**
     * @return a panel for editing the data
     */
    JPanel getEditPanel();

    /**
     * @return a panel showing information about the dataset
     */
    JPanel getInfoPanel();

    /**
     * @return the dataset stored in the viewable, this may be null
     */
    E getData();

    /**
     * This is a method for creating a filename appropriate for the given viewable type.
     * The idea is that this will add a sensible default extension or whatever else is needed.
     * This is part of the interface because it likely changes based on the type of data stored.
     *
     * @param name a base name (this can be anything really)
     * @return a filename based on the given basename
     */
    String toFilename(String name);

    /**
     * Set the dataset stored in this viewable
     * @param e the dataset to store (may be null)
     */
     Viewable<E> setData(E e);

    /**
     * Set the dataset stored in this viewable
     * @param e the dataset to store (may be null)
     */
    Viewable<E> setDataset(Dataset e);

    /**
     * Remove the data (sets it to null)
     */
    void clearData();

    /**
     * @return true if there is any data stored in the viewable
     */
    boolean hasData();

    /** Run this if there is data, and return whether it was run */
    boolean ifHasData(Consumer<E> run);

    /** Run this if there is data, and return whether it was run */
    boolean ifHasData(Runnable run);

    /**
     * @return true if the data has a well defined bounding box
     */
    boolean hasBounds();

    /**
     * @return the bounding box for the data (null if one is not well defined)
     */
    Box getBounds();

    /**
     * Dispose any references used by OpenGL to render the data.  This is needed when there are major changes to the GUI
     */
    void dispose(GL2 gl);

    /**
     * Render the data
     * @param gl the OpenGL context
     */
    void display(GL2 gl);

    /**
     * Compute the distance from the given mouse click to the data
     * @param mouse the mouse event
     * @return the distance
     */
    Double dist(WorldMouse mouse);

    /**
     * @return the interaction modes supported by this viewable
     */
    List<String> modes();

    /**
     * Handle an interactio with the data
     * @param mouse the mouse event
     * @param mode the type of interaction
     */
    void handle(WorldMouse mouse, String mode);

    boolean getVisible();

    String getName();

    String getFilename();

    Viewable setVisible(boolean v);

    Viewable setFilename(String v);

    Viewable setName(String v);
}
