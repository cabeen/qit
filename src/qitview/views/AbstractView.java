/*******************************************************************************
 * Copyright (c) 2010-2016, Ryan Cabeen
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 * must display the following acknowledgement:
 * This product includes software developed by the Ryan Cabeen.
 * 4. Neither the name of the Ryan Cabeen nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY RYAN CABEEN ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL RYAN CABEEN BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package qitview.views;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jogamp.opengl.GL2;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import qit.base.Dataset;
import qit.base.Logging;
import qit.base.structs.ObservableInstance;
import qit.math.structs.Box;
import qitview.models.Viewable;
import qitview.models.WorldMouse;
import qitview.widgets.CollapsablePanel;
import qitview.widgets.ControlPanel;
import qitview.widgets.VerticalLayout;

/**
 * this should be subclassed for each view
 */
public abstract class AbstractView<E extends Dataset> implements Viewable<E>
{
    public transient ObservableInstance observable = new ObservableInstance();

    private boolean visible = false;
    private String name;
    private String filename;

    public transient Box bounds = null;
    protected transient E data = null;

    protected transient JPanel renderPanel = new JPanel();
    protected transient JPanel editPanel = new JPanel();
    protected transient JPanel infoPanel = new JPanel();

    public AbstractView()
    {
        Consumer<JPanel> initPanel = p ->
        {
            p.setLayout(new VerticalLayout(2, VerticalLayout.LEFT));
            p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        };

        initPanel.accept(this.infoPanel);
        initPanel.accept(this.renderPanel);
        initPanel.accept(this.editPanel);
    }

    protected Map<String, ControlPanel> makeEditControls()
    {
        return Maps.newLinkedHashMap();
    }

    protected Map<String, ControlPanel> makeRenderControls()
    {
        return Maps.newLinkedHashMap();
    }

    protected abstract ControlPanel makeInfoControls();

    public void display(GL2 gl)
    {
        // default display does nothing
    }

    public void input()
    {
        // default input does nothing
    }

    public List<String> modes()
    {
        return Lists.newArrayList();
    }

    public void handle(WorldMouse mouse, String mode)
    {
        // default handling does nothing
    }

    public Double dist(WorldMouse mouse)
    {
        // default selection does nothing
        return null;
    }

    public void dispose(GL2 gl)
    {
        // dispose display lists and anything else that is tied to a GL context
    }

    public AbstractView<E> setData(E d)
    {
        // subclass should extend and add any necessary data modification for
        // viewing and update the bounds variable to reflect the subclass datatype

        if (d == null)
        {
            this.bounds = null;
        }

        this.data = d;
        this.touchData();

        return this;
    }

    public AbstractView<E> setDataset(Dataset d)
    {
        return this.setData((E) d);
    }

    public final void touchData()
    {
        this.observable.changed();
    }


    // methods won't need anything new in subclasses

    protected void initPanel()
    {
        BiConsumer<JPanel, Map<String, ControlPanel>> buildPanel = (panel, controls) ->
        {
            panel.removeAll();

            for (String c : controls.keySet())
            {
                CollapsablePanel cpanel = new CollapsablePanel(c, controls.get(c));
                cpanel.setSelection(true);

                panel.add(cpanel);
            }
        };

        this.infoPanel.add(this.makeInfoControls());
        buildPanel.accept(this.renderPanel, this.makeRenderControls());
        buildPanel.accept(this.editPanel, this.makeEditControls());
    }

    public boolean hasData()
    {
        return this.data != null;
    }

    public boolean ifHasData(Consumer<E> run)
    {
        if (this.hasData())
        {
            run.accept(this.getData());
            return true;
        }

        return false;
    }

    public boolean ifHasData(Runnable run)
    {
        if (this.hasData())
        {
            run.run();
            return true;
        }

        return false;
    }

    public Box getBounds()
    {
        return this.bounds;
    }

    public boolean hasBounds()
    {
        return this.bounds != null;
    }

    public E getDataDirect()
    {
        // this method is to make life easier in other classes
        return this.data;
    }

    public void clearData()
    {
        this.data = null;
        this.bounds = null;
        this.touchData();
    }

    public JPanel getRenderPanel()
    {
        return this.renderPanel;
    }

    public JPanel getEditPanel()
    {
        return this.editPanel;
    }

    public JPanel getInfoPanel()
    {
        return this.infoPanel;
    }

    public String toFilename(String name)
    {
        // check the filename extension
        boolean valid = false;
        for (String ext : this.data.getExtensions())
        {
            if (name.endsWith(ext))
            {
                Logging.info("detected extension: " + ext);
                valid = true;
                break;
            }
        }

        String fn = name;

        // add the default if there isn't one
        if (!valid)
        {
            String ext = this.data.getExtensions().get(0);
            Logging.info("adding default extension: " + ext);
            fn += "." + ext;
        }

        return fn;
    }

    public String toString()
    {
        return this.getClass().getSimpleName();
    }

    public Observable getObservable()
    {
        return this.observable;
    }

    public boolean getVisible()
    {
        return this.visible;
    }

    public String getName()
    {
        return this.name;
    }

    public String getFilename()
    {
        return this.filename;
    }

    public Viewable setVisible(boolean v)
    {
        this.visible = v;
        return this;
    }

    public Viewable setFilename(String v)
    {
        this.filename = v;
        return this;
    }

    public Viewable setName(String v)
    {
        this.name = v;
        return this;
    }
}