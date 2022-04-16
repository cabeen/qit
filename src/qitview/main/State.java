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
import com.google.common.collect.Queues;
import qit.base.structs.Pair;
import qit.base.structs.Triple;
import qitview.models.Slicer;
import qitview.models.Viewable;
import qitview.models.ViewableType;
import qitview.panels.Modules;
import qitview.panels.Processes;
import qitview.panels.QueryVoxel;
import qitview.panels.Viewables;
import qitview.render.RenderAnnotation;
import qitview.views.MaskView;
import qitview.views.VolumeView;
import qitview.widgets.ColormapState;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

public class State
{
    public transient boolean started = false;
    public transient boolean running = true;

    public transient String osname = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
    public transient String jvm = System.getProperty("java.version");

    public transient Queue<Pair<String, Integer>> qshots = Queues.newConcurrentLinkedQueue();
    public transient Queue<Viewable> qviewables = Queues.newConcurrentLinkedQueue();
    public transient Queue<Triple<ViewableType, List<String>, Boolean>> qload = Queues.newConcurrentLinkedQueue();
    public transient Queue<Pair<String, Runnable>> qrun = Queues.newConcurrentLinkedQueue();

    public transient Map<Integer, Long> keys = Maps.newHashMap();
    public transient List<Slicer> slicers = Lists.newArrayList();;

    public transient double scaling = 1.0;

    public transient Controller control = new Controller();
    public transient Settings settings = new Settings();
    public transient Viewables data = new Viewables();
    public transient Processes processes = new Processes();
    public transient Modules modules = new Modules();
    public transient QueryVoxel query = new QueryVoxel();
    public transient ColormapState colormaps = null;
    public transient RenderAnnotation annotation = null;
    public transient Interface gui = null;

    public transient VolumeView referenceVolume = null;
    public transient VolumeView referenceOverlay = null;
    public transient MaskView referenceMask = null;

    public State()
    {
        this.gui = new Interface(this);
    }
}