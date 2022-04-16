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

import com.google.common.collect.Lists;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

public class SuggestionPanel extends JPanel
{
    private static final long serialVersionUID = 1L;
    private final JTextField field;
    private final BasicComboBox<String> combo = new BasicComboBox<>();
    private final Vector<String> items = new Vector<>();

    public SuggestionPanel(String title, Iterable<String> vitems)
    {
        super(new BorderLayout());
        this.combo.setEditable(true);
        this.field = (JTextField) combo.getEditor().getEditorComponent();
        this.field.requestFocus();
        this.field.addKeyListener(new KeyAdapter()
        {
            public void keyTyped(KeyEvent e)
            {
                EventQueue.invokeLater(() ->
                {
                    String text = SuggestionPanel.this.field.getText();
                    if (text.length() == 0)
                    {
                        SuggestionPanel.this.combo.hidePopup();
                        setModel(new DefaultComboBoxModel<>(SuggestionPanel.this.items), "");
                    }
                    else
                    {
                        DefaultComboBoxModel<String> m = getSuggestedModel(SuggestionPanel.this.items, text);
                        if (m.getSize() == 0 || SuggestionPanel.this.hide_flag)
                        {
                            SuggestionPanel.this.combo.hidePopup();
                            SuggestionPanel.this.hide_flag = false;
                        }
                        else
                        {
                            setModel(m, text);
                            SuggestionPanel.this.combo.showPopup();
                        }
                    }
                });
            }

            public void keyPressed(KeyEvent e)
            {
                String text = SuggestionPanel.this.field.getText();
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_ENTER)
                {
                    SuggestionPanel.this.hide_flag = true;
                }
                else if (code == KeyEvent.VK_ESCAPE)
                {
                    SuggestionPanel.this.hide_flag = true;
                }
                else if (code == KeyEvent.VK_RIGHT)
                {
                    for (int i = 0; i < SuggestionPanel.this.items.size(); i++)
                    {
                        String str = SuggestionPanel.this.items.elementAt(i);
                        if (matches(str, text))
                        {
                            SuggestionPanel.this.combo.setSelectedIndex(-1);
                            SuggestionPanel.this.field.setText(str);
                            return;
                        }
                    }
                }
            }
        });

        for (String item : vitems)
        {
            this.items.addElement(item);
        }
        this.setModel(new DefaultComboBoxModel<>(items), "");

        // JPanel p = new JPanel(new BorderLayout());
        // p.setBorder(BorderFactory.createTitledBorder(title));
        // p.add(combo, BorderLayout.NORTH);
        // this.add(p);
        // this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        this.setBorder(BorderFactory.createTitledBorder(title));
        this.add(combo, BorderLayout.CENTER);
    }

    private boolean hide_flag = false;

    private void setModel(DefaultComboBoxModel<String> mdl, String str)
    {
        combo.setModel(mdl);
        combo.setSelectedIndex(-1);
        field.setText(str);
    }

    private static boolean matches(String a, String b)
    {
        return a.toLowerCase().contains(b.toLowerCase());
    }

    private static DefaultComboBoxModel<String> getSuggestedModel(java.util.List<String> list, String text)
    {
        DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
        for (String s : list)
        {
            if (matches(s, text))
            {
                m.addElement(s);
            }
        }
        return m;
    }

    public void addActionListener(ActionListener a)
    {
        this.field.addActionListener(a);
    }

    public String getText()
    {
        return this.field.getText();
    }
}

