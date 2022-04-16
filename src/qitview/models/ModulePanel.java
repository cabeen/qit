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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import qit.base.Dataset;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleExpert;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.structs.Named;
import qit.base.structs.Pair;
import qit.base.structs.Triple;
import qit.base.utils.ModuleUtils;
import qitview.panels.Viewables;
import qitview.main.Viewer;
import qitview.widgets.BasicComboBox;
import qitview.widgets.BasicTextField;
import qitview.widgets.CollapsablePanel;
import qitview.widgets.ControlPanel;
import qitview.widgets.SwingUtils;
import qitview.widgets.VerticalLayout;

public class ModulePanel extends JPanel
{
    public static int TEXT_WIDTH = 20;

    // this variable specifies the size of the combobox when not selected.  we need this because names can be very long
    private static final Named<Viewable<?>> PROTO = new Named<>("                                                          ", null);

    private Component parent;
    private Module module;
    private Map<String, BasicComboBox<Named<Viewable<?>>>> viewEntries = Maps.newLinkedHashMap();
    private Map<String, JCheckBox> boolEntries = Maps.newLinkedHashMap();
    private Map<String, BasicTextField> otherEntries = Maps.newLinkedHashMap();
    private Map<String, BasicComboBox<String>> enumEntries = Maps.newLinkedHashMap();

    public ModulePanel(Window p, Module o)
    {
        this.parent = p;
        this.module = o;
        ModuleUtils.validate(this.module);

        try
        {
            ModulePanel.this.run();
        }
        catch (IllegalArgumentException e)
        {
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }

    private void run() throws IllegalArgumentException, IllegalAccessException
    {
        ControlPanel requiredInputControls = new ControlPanel();
        ControlPanel optionalInputControls = new ControlPanel();
        ControlPanel paramControls = new ControlPanel();
        ControlPanel paramAdvancedControls = new ControlPanel();
        ControlPanel requiredOutputControls = new ControlPanel();
        ControlPanel optionalOutputControls = new ControlPanel();

        final Class<? extends Module> type = this.module.getClass();
        final String moduleName = type.getSimpleName();

        for (Field field : ModuleUtils.fields(this.module))
        {
            field.setAccessible(true);

            if (!Global.getExpert() && field.getAnnotation(ModuleExpert.class) != null)
            {
                continue;
            }

            boolean optional = field.getAnnotation(ModuleOptional.class) != null;
            boolean input = field.getAnnotation(ModuleInput.class) != null;
            boolean output = field.getAnnotation(ModuleOutput.class) != null;

            Class<?> fieldType = field.getType();
            final String fieldName = field.getName();

            Object paramValue = field.get(ModulePanel.this.module);

            ModuleDescription annot = field.getAnnotation(ModuleDescription.class);
            String tip = annot != null ? annot.value() : "";

            ModuleCitation citation = field.getAnnotation(ModuleCitation.class);

            if (input || output)
            {
                if (!ViewableType.hasDataType(fieldType))
                {
                    Viewer.getInstance().control.setStatusMessage(String.format("Warning: model '%s' field '%s' could not be added to user interface", moduleName, fieldName));
                    continue;
                }

                ControlPanel controls = output ? (optional ? optionalOutputControls : requiredOutputControls) : (optional ? optionalInputControls : requiredInputControls);

                ViewableType viewableType = ViewableType.getFromDataType(fieldType);
                BasicComboBox<Named<Viewable<?>>> combo = Viewer.getInstance().data.getComboBox(viewableType, !output || input, optional);
                combo.setPrototypeDisplayValue(PROTO);

                ModulePanel.this.viewEntries.put(fieldName, combo);
                controls.addControl(fieldName, tip, combo);
            }
            else if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class))
            {
                final JCheckBox elem = new JCheckBox();
                elem.setSelected((boolean) paramValue);

                if (field.getAnnotation(ModuleAdvanced.class) == null)
                {
                    paramControls.addControl(fieldName + ":", tip, elem);
                }
                else
                {
                    paramAdvancedControls.addControl(fieldName + ":", tip, elem);
                }

                ModulePanel.this.boolEntries.put(fieldName, elem);
            }
            else if (fieldType.isEnum())
            {
                Object[] values = fieldType.getEnumConstants();

                String[] svalues = null;
                if (optional)
                {
                    svalues = new String[values.length + 1];

                    svalues[0] = Viewables.NONE.getName();
                    for (int i = 0; i < values.length; i++)
                    {
                        svalues[i + 1] = values[i].toString();
                    }
                }
                else
                {
                    svalues = new String[values.length];

                    for (int i = 0; i < values.length; i++)
                    {
                        svalues[i] = values[i].toString();
                    }
                }

                final BasicComboBox<String> elem = new BasicComboBox<>(svalues);

                if (optional)
                {
                    elem.setSelectedItem(Viewables.NONE.getName());
                }
                else
                {
                    elem.setSelectedItem(paramValue.toString());
                }

                if (field.getAnnotation(ModuleAdvanced.class) == null)
                {
                    paramControls.addControl(fieldName + ":", tip, elem);
                }
                else
                {
                    paramAdvancedControls.addControl(fieldName + ":", tip, elem);
                }
                ModulePanel.this.enumEntries.put(fieldName, elem);
            }
            else
            {
                String defaultValue = "";
                if (paramValue != null)
                {
                    defaultValue = String.valueOf(paramValue);
                }

                final BasicTextField elem = new BasicTextField(TEXT_WIDTH);
                elem.setText(defaultValue);
                if (field.getAnnotation(ModuleAdvanced.class) == null)
                {
                    paramControls.addControl(fieldName + ":", tip, elem);
                }
                else
                {
                    paramAdvancedControls.addControl(fieldName + ":", tip, elem);
                }
                ModulePanel.this.otherEntries.put(fieldName, elem);
            }
        }

        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        {
            JPanel panel = new JPanel();
            panel.setLayout(new VerticalLayout(5, VerticalLayout.LEFT));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            java.util.List<Triple<String, ControlPanel, Boolean>> groups = Lists.newArrayList();
            groups.add(Triple.of("Input", requiredInputControls, true));
            groups.add(Triple.of("Optional Input", optionalInputControls, false));
            groups.add(Triple.of("Parameters", paramControls, false));
            groups.add(Triple.of("Advanced Parameters", paramAdvancedControls, false));
            groups.add(Triple.of("Optional Output", optionalOutputControls, false));
            groups.add(Triple.of("Output", requiredOutputControls, true));

            for (Triple<String, ControlPanel, Boolean> group : groups)
            {
                if (group.b.getNumControls() > 0)
                {
                    CollapsablePanel collapser = new CollapsablePanel(group.a, group.b);
                    collapser.setSelection(group.c);
                    panel.add(collapser);
                }
            }

            this.add(panel);
        }
    }

    public void pressed()
    {
        final String moduleName = ModulePanel.this.module.getClass().getSimpleName();

        // the threading madness below is needed because we need to query and update the GUI on
        // the swing thread and run the module on another thread

        try
        {
            ModulePanel.this.naming().ifPresent(naming -> Viewer.getInstance().qrun.offer(Pair.of("Running " + moduleName, () ->
            {
                Viewer.getInstance().control.setStatusMessage("started " + moduleName);
                try
                {
                    SwingUtilities.invokeAndWait(() ->
                    {
                        try
                        {
                            ModulePanel.this.pushParamsToModule();
                            ModulePanel.this.pushInputToModule();
                        }
                        catch (IllegalAccessException e)
                        {
                            e.printStackTrace();
                        }
                    });

                    ModulePanel.this.module.run();

                    SwingUtilities.invokeAndWait(() ->
                    {
                        try
                        {
                            ModulePanel.this.pullDataFromModule(naming);
                        }
                        catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e)
                        {
                            e.printStackTrace();
                        }
                    });
                }
                catch (Exception e)
                {
                    try
                    {
                        SwingUtilities.invokeAndWait(() ->
                        {
                            SwingUtils.showMessage(ModulePanel.this.parent, "Error", moduleName + " failed to run.  Check the logs for details.");
                            Viewer.getInstance().control.setStatusMessage("warning: module failed.  " + e.getMessage());
                            e.printStackTrace();
                        });
                    }
                    catch (InterruptedException | InvocationTargetException exp)
                    {
                        exp.printStackTrace();
                    }
                }
            })));
        }
        catch (Exception e)
        {
            SwingUtils.showMessage(ModulePanel.this.parent, "Error", moduleName + " failed to run.  Check the logs for details.");
            Viewer.getInstance().control.setStatusMessage("warning: module failed.  " + e.getMessage());
            e.printStackTrace();

            return;
        }
    }

    @SuppressWarnings("unchecked")
    private void pushParamsToModule() throws IllegalArgumentException, IllegalAccessException
    {
        // load input data and create output containers
        for (Field field : ModuleUtils.parameters(this.module))
        {
            field.setAccessible(true);

            if (!Global.getExpert() && field.getAnnotation(ModuleExpert.class) != null)
            {
                continue;
            }

            boolean optional = field.getAnnotation(ModuleOptional.class) != null;

            Class<?> fieldType = field.getType();
            final String fieldName = field.getName();

            if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class))
            {
                boolean value = ModulePanel.this.boolEntries.get(fieldName).isSelected();
                field.set(ModulePanel.this.module, value);

                Logging.info(String.format("set %s to ", fieldName) + value);
            }
            else if (fieldType.isEnum())
            {
                String value = (String) ModulePanel.this.enumEntries.get(fieldName).getSelectedItem();

                if (value.equals(Viewables.NONE.getName()))
                {
                    value = null;
                }

                if (value != null)
                {
                    field.set(ModulePanel.this.module, Enum.valueOf((Class<Enum>) field.getType(), value));
                }
                else if (!optional)
                {
                    throw new RuntimeException("missing parameter data: " + fieldName);
                }
                else
                {
                    field.set(ModulePanel.this.module, null);
                }
            }
            else
            {
                String value = ModulePanel.this.otherEntries.get(fieldName).getText();

                boolean valid = value != null;
                valid &= value.length() > 0;
                valid &= !value.equals("");
                valid &= !value.toLowerCase().equals("null");

                if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class))
                {
                    field.set(ModulePanel.this.module, valid);
                }
                else if (valid)
                {
                    if (fieldType.equals(String.class))
                    {
                        field.set(ModulePanel.this.module, value);
                    }
                    else if (fieldType.equals(Integer.class) || fieldType.equals(int.class))
                    {
                        field.set(ModulePanel.this.module, Integer.valueOf(value));
                    }
                    else if (fieldType.equals(Double.class) || fieldType.equals(double.class))
                    {
                        field.set(ModulePanel.this.module, Double.valueOf(value));
                    }
                    else
                    {
                        throw new RuntimeException("invalid field type: " + fieldType.toString());
                    }
                }
                else if (!optional)
                {
                    throw new RuntimeException("missing parameter data: " + fieldName);
                }
                else
                {
                    field.set(ModulePanel.this.module, null);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void pushInputToModule() throws IllegalArgumentException, IllegalAccessException
    {
        // load input data and create output containers
        for (Field field : ModuleUtils.inputs(this.module))
        {
            field.setAccessible(true);

            if (!Global.getExpert() && field.getAnnotation(ModuleExpert.class) != null)
            {
                continue;
            }

            boolean optional = field.getAnnotation(ModuleOptional.class) != null;
            final String fieldName = field.getName();

            BasicComboBox<Named<Viewable<?>>> combo = ModulePanel.this.viewEntries.get(fieldName);
            combo.setPrototypeDisplayValue(PROTO);

            Viewable<? extends Dataset> viewable = ((Named<Viewable<? extends Dataset>>) combo.getSelectedItem()).getValue();

            if (viewable != null && viewable.hasData())
            {
                field.set(ModulePanel.this.module, viewable.getData());
            }
            else if (!optional)
            {
                throw new RuntimeException("missing parameter data: " + fieldName);
            }
            else
            {
                field.set(ModulePanel.this.module, null);
            }
        }
    }


    @SuppressWarnings("unchecked")
    private Optional<Map<String, String>> naming() throws IllegalArgumentException, IllegalAccessException
    {
        Map<String, String> out = Maps.newHashMap();

        // load input data and create output containers
        for (Field field : ModuleUtils.outputs(this.module))
        {
            field.setAccessible(true);

            if (!Global.getExpert() && field.getAnnotation(ModuleExpert.class) != null)
            {
                continue;
            }

            Class<?> fieldType = field.getType();
            final String fieldName = field.getName();

            BasicComboBox<Named<Viewable<?>>> combo = ModulePanel.this.viewEntries.get(fieldName);
            Named<Viewable<? extends Dataset>> selected = (Named<Viewable<? extends Dataset>>) combo.getSelectedItem();

            if (combo != null)
            {
                combo.setPrototypeDisplayValue(PROTO);
            }

            // add viewables if necessary
            if (!selected.getName().equals(Viewables.NONE.getName()))
            {
                if (selected.getValue() == null)
                {
                    ViewableType viewableType = ViewableType.getFromDataType(fieldType);
                    final Class<? extends Module> type = this.module.getClass();
                    final String moduleName = type.getSimpleName();

                    String base = String.format("Output-%s-%s", viewableType.getText(), moduleName).replace(" ", "-");
                    Optional<String> name = Optional.of(base);

                    if (selected.getName().equals(Viewables.NEW_NAMED.getName()))
                    {
                        String msg = String.format("Name for %s:", fieldName);
                        name = SwingUtils.getStringOptionalEventThread(msg, name.get());
                    }

                    Logging.info(name.toString());

                    if (name.isPresent())
                    {
                        out.put(fieldName, Viewer.getInstance().data.getUniqueName(name.get()));
                    }
                    else
                    {
                        Logging.info("processing cancelled");
                        return Optional.empty();
                    }
                }
                else
                {
                    out.put(fieldName, selected.getName());
                }
            }
        }

        return Optional.of(out);
    }

    @SuppressWarnings("unchecked")
    private void pullDataFromModule(Map<String, String> naming) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException, InstantiationException
    {
        // load input data and create output containers
        Set<Class<?>> changes = Sets.newHashSet();
        for (Field field : ModuleUtils.outputs(this.module))
        {
            field.setAccessible(true);

            if (!Global.getExpert() && field.getAnnotation(ModuleExpert.class) != null)
            {
                continue;
            }

            boolean optional = field.getAnnotation(ModuleOptional.class) != null;

            Class<?> paramType = field.getType();
            final String paramName = field.getName();
            Object paramValue = field.get(ModulePanel.this.module);

            if (ViewableType.hasDataType(paramType) && paramValue != null && naming.containsKey(paramName))
            {
                String name = naming.get(paramName);
                Viewables viewables = Viewer.getInstance().data;

                Viewable<?> data = null;
                if (viewables.has(name))
                {
                    int idx = viewables.index(name);
                    data = viewables.getViewable(idx);
                }
                else
                {
                    Class<?> fieldType = field.getType();
                    ViewableType viewableType = ViewableType.getFromDataType(fieldType);
                    data = viewableType.getViewType().newInstance();
                    data.setName(name);

                    Viewer.getInstance().control.add(data);
                }

                BasicComboBox<Named<Viewable<?>>> combo = ModulePanel.this.viewEntries.get(paramName);
                combo.setPrototypeDisplayValue(PROTO);
                combo.setSelectedItem(Named.of(name, data));

                // push the data back with some reflection black magic

                if (data != null)
                {
                    Class<? extends Dataset> t = ViewableType.getFromView(data).getDataType();
                    Method m = data.getClass().getMethod("setData", t);
                    m.invoke(data, field.get(ModulePanel.this.module));

                    changes.add(data.getClass());
                }
            }
            else if (!optional)
            {
                throw new RuntimeException("missing parameter data: " + paramName);
            }
        }

        // only update combos for output data types
        // do this outside the loop so each type is updated at most once
        Viewables data = Viewer.getInstance().data;
        for (Class<?> clas : changes)
        {
            data.updateCombos(clas);
        }
    }

    public void about()
    {
        final Class<? extends Module> type = this.module.getClass();
        final String moduleName = type.getSimpleName();

        StringBuilder msg = new StringBuilder();

        msg.append("Name:\n\n  ");
        msg.append(moduleName);
        msg.append("\n");

        ModuleDescription annot = type.getAnnotation(ModuleDescription.class);
        if (annot != null)
        {
            msg.append("\nDescription:\n\n");
            for (String line : SwingUtils.format(annot.value(), 80))
            {
                msg.append("  " + line + "\n");
            }
        }

        ModuleAuthor author = type.getAnnotation(ModuleAuthor.class);
        if (author != null)
        {
            msg.append("\nAuthor:\n\n");
            for (String line : SwingUtils.format(author.value(), 80))
            {
                msg.append("  " + line + "\n");
            }
        }

        ModuleCitation cite = type.getAnnotation(ModuleCitation.class);
        if (cite != null)
        {
            msg.append("\nCitation:\n\n");
            for (String line : SwingUtils.format(cite.value(), 80))
            {
                msg.append("  " + line + "\n");
            }
        }

        SwingUtils.showMessage(ModulePanel.this.parent, "About " + moduleName, msg.toString());
    }

    public void save()
    {
        String fn = Viewer.getInstance().gui.chooseSaveFile("Choose a filename for saving parameters...", "module.json");
        if (fn != null)
        {
            try
            {
                ModulePanel.this.pushParamsToModule();

                if (fn.endsWith("pipe"))
                {
                    ModuleUtils.writePipeline(this.module, fn);
                    Viewer.getInstance().control.setStatusMessage("exported module to pipeline: " + fn);
                }
                else
                {
                    ModuleUtils.write(this.module, fn);
                    Viewer.getInstance().control.setStatusMessage("exported module to json: " + fn);
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                Viewer.getInstance().control.setStatusMessage("warning: failed to save module parameters");
            }
        }
    }
}