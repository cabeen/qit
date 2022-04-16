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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import qit.base.Logging;
import qit.base.Module;
import qit.base.utils.ModuleUtils;
import qitview.main.Constants;
import qitview.main.Viewer;
import qitview.models.ModulePanel;
import qitview.widgets.BasicButton;
import qitview.widgets.SuggestionPanel;
import qitview.widgets.SwingUtils;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class Modules
{
    private JFrame window = null;
    private JTabbedPane tabs = new JTabbedPane();
    private List<ModulePanel> modules = Lists.newArrayList();

    public Modules()
    {
        SwingUtilities.invokeLater(() ->
        {
            final Map<String, Class<? extends Module>> unlistedModules = ModuleUtils.unlistedClasses();
            final Map<String, Class<? extends Module>> listedModules = ModuleUtils.listedClasses();
            final SuggestionPanel suggest = new SuggestionPanel("Search for a module to activate below (press Return to activate):", ModuleUtils.names());

            {
                Runnable activate = () ->
                {
                    try
                    {
                        String key = suggest.getText();

                        if (key.trim().length() == 0)
                        {
                            Logging.info("no module was specified");
                            return;
                        }

                        for (String listedModule : listedModules.keySet())
                        {
                            if (listedModule.toLowerCase().equals(key.toLowerCase()))
                            {
                                Logging.info("loading module: " + key);
                                Modules.this.add(listedModules.get(listedModule).newInstance());
                                return;
                            }
                        }

                        for (String unlistedModule : unlistedModules.keySet())
                        {
                            if (unlistedModule.toLowerCase().equals(key.toLowerCase()))
                            {
                                Modules.this.add(unlistedModules.get(unlistedModule).newInstance());
                                return;
                            }
                        }

                        Logging.info("module not found: " + key);
                    }
                    catch (InstantiationException | IllegalAccessException exp)
                    {
                        exp.printStackTrace();
                    }
                };
                suggest.addActionListener(e -> activate.run());

                JButton button = new JButton("Activate Module");
                button.addActionListener(e -> activate.run());
                suggest.add(button, BorderLayout.EAST);
            }

            JPanel buttons = new JPanel();
            {
                BasicButton about = new BasicButton("About");
                about.setToolTipText("show some information about the module");
                about.addActionListener(e ->
                {
                    if (Modules.this.modules.size() > 0)
                    {
                        Modules.this.selected().about();
                    }
                });

                BasicButton save = new BasicButton("Save");
                save.setToolTipText("save the current parameters to json");
                save.addActionListener(e ->
                {
                    if (Modules.this.modules.size() > 0)
                    {
                        Modules.this.selected().save();
                    }
                });

                BasicButton load = new BasicButton("Load");
                load.setToolTipText("load module parameters from json");
                load.addActionListener(e ->
                {
                    try
                    {
                        String fn = Viewer.getInstance().gui.chooseLoadFiles("Choose a module parameter set to load...").get(0);

                        if (fn != null)
                        {
                            Module module = ModuleUtils.read(fn);
                            Logging.info("loaded module: " + fn);
                            Modules.this.add(module);
                        }
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                        Viewer.getInstance().control.setStatusError("failed to load module: " + ex.getMessage());
                    }
                });

                BasicButton remove = new BasicButton("Remove");
                remove.setToolTipText("Remove the given module from the list of active modules");
                remove.addActionListener(e -> Modules.this.remove());

                BasicButton apply = new BasicButton("Apply");
                apply.setToolTipText("run the shown module");
                apply.addActionListener(e ->
                {
                    if (Modules.this.modules.size() > 0)
                    {
                        Modules.this.selected().pressed();
                    }
                });

                {
                    buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
                    buttons.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                    buttons.add(about);
                    buttons.add(load);
                    buttons.add(save);
                    buttons.add(remove);
                    buttons.add(Box.createHorizontalGlue());
                    buttons.add(apply);
                }
            }

            // searchPanel.setPreferredSize(new Dimension(350, 20));
            Modules.this.tabs.setPreferredSize(new Dimension(500, 300));
            Modules.this.empty();

            JPanel tabPanel = new JPanel();
            tabPanel.setBorder(BorderFactory.createTitledBorder("Active Modules"));
            tabPanel.setLayout(new BorderLayout());
            tabPanel.add(Modules.this.tabs, BorderLayout.CENTER);
            tabPanel.add(buttons, BorderLayout.SOUTH);

            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            panel.add(suggest, BorderLayout.NORTH);
            panel.add(tabPanel, BorderLayout.CENTER);

            JFrame frame = Viewer.getInstance().gui.getFrame();
            Modules.this.window = new JFrame(Constants.SETTINGS_MODULES);
            Modules.this.window.add(panel);
            Modules.this.window.setMinimumSize(new Dimension(300, 200));
            Modules.this.window.pack();
            Modules.this.window.setLocationRelativeTo(frame);
            Modules.this.window.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
            Modules.this.window.setResizable(true);
            Modules.this.window.setVisible(false);

            SwingUtils.addEscapeListener(Modules.this.window);
        });
    }

    public void add(Module m)
    {
        SwingUtilities.invokeLater(() ->
        {
            Modules.this.show();

            if (Modules.this.modules.size() == 0)
            {
                Modules.this.tabs.removeAll();
            }

            ModulePanel module = new ModulePanel(Modules.this.window, m);
            Modules.this.modules.add(module);

            JScrollPane scroll = new JScrollPane(module);
            Modules.this.tabs.addTab(m.getClass().getSimpleName(), scroll);

            int idx = Modules.this.tabs.getTabCount() - 1;
            Modules.this.tabs.setSelectedIndex(idx);
            Modules.this.window.pack();
        });
    }

    public void show()
    {
        this.setVisible(true);
    }

    public void setVisible(boolean v)
    {
        SwingUtilities.invokeLater(() ->
        {
            if (Modules.this.window != null)
            {
                Modules.this.window.setVisible(v);
            }
        });
    }

    private void empty()
    {
        this.tabs.removeAll();
        this.modules.clear();

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.add(new JLabel("No active modules have been selected"));

        this.tabs.addTab("None", panel);
    }

    public void remove()
    {
        if (Modules.this.modules.size() > 0)
        {
            int idx = Modules.this.tabs.getSelectedIndex();

            Modules.this.modules.remove(idx);
            Modules.this.tabs.removeTabAt(idx);

            if (Modules.this.tabs.getTabCount() == 0)
            {
                this.empty();
            }
        }
    }

    public ModulePanel selected()
    {
        int idx = Modules.this.tabs.getSelectedIndex();
        return Modules.this.modules.get(idx);
    }
}
