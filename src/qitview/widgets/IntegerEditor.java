package qitview.widgets;

import javax.swing.AbstractAction;
import javax.swing.DefaultCellEditor;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.text.ParseException;

public class IntegerEditor extends DefaultCellEditor
{
    BasicFormattedTextField ftf;
    NumberFormat integerFormat;
    private Integer minimum, maximum;
    private boolean DEBUG = false;

    public IntegerEditor(int min, int max)
    {
        super(new BasicFormattedTextField());
        this.ftf = (BasicFormattedTextField) getComponent();
        this.minimum = new Integer(min);
        this.maximum = new Integer(max);

        //Set up the editor for the integer cells.
        this.integerFormat = NumberFormat.getIntegerInstance();
        NumberFormatter intFormatter = new NumberFormatter(this.integerFormat);
        intFormatter.setFormat(this.integerFormat);
        intFormatter.setMinimum(this.minimum);
        intFormatter.setMaximum(this.maximum);

        this.ftf.setFormatterFactory(new DefaultFormatterFactory(intFormatter));
        this.ftf.setValue(this.minimum);
        this.ftf.setHorizontalAlignment(BasicTextField.TRAILING);
        this.ftf.setFocusLostBehavior(BasicFormattedTextField.PERSIST);

        //React when the user presses Enter while the editor is
        //active.  (Tab is handled as specified by
        //BasicFormattedTextField's focusLostBehavior property.)
        this.ftf.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "check");
        this.ftf.getActionMap().put("check", new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!IntegerEditor.this.ftf.isEditValid())
                { //The text is invalid.
                    if (userSaysRevert())
                    { //reverted
                        IntegerEditor.this.ftf.postActionEvent(); //inform the editor
                    }
                }
                else
                {
                    try
                    {              //The text is valid,
                        IntegerEditor.this.ftf.commitEdit();     //so use it.
                        IntegerEditor.this.ftf.postActionEvent(); //stop editing
                    }
                    catch (ParseException exc)
                    {
                    }
                }
            }
        });
    }

    //Override to invoke setValue on the formatted text field.
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
    {
        BasicFormattedTextField myftf = (BasicFormattedTextField) super.getTableCellEditorComponent(table, value, isSelected, row, column);
        myftf.setValue(value);
        return myftf;
    }

    //Override to ensure that the value remains an Integer.
    public Object getCellEditorValue()
    {
        BasicFormattedTextField ftf = (BasicFormattedTextField) getComponent();
        Object o = ftf.getValue();
        if (o instanceof Integer)
        {
            return o;
        }
        else if (o instanceof Number)
        {
            return new Integer(((Number) o).intValue());
        }
        else
        {
            if (DEBUG)
            {
                System.out.println("getCellEditorValue: o isn't a Number");
            }
            try
            {
                return integerFormat.parseObject(o.toString());
            }
            catch (ParseException exc)
            {
                System.err.println("getCellEditorValue: can't parse o: " + o);
                return null;
            }
        }
    }

    //Override to check whether the edit is valid,
    //setting the value if it is and complaining if
    //it isn't.  If it's OK for the editor to go
    //away, we need to invoke the superclass's version
    //of this method so that everything gets cleaned up.
    public boolean stopCellEditing()
    {
        BasicFormattedTextField myftf = (BasicFormattedTextField) getComponent();
        if (myftf.isEditValid())
        {
            try
            {
                myftf.commitEdit();
            }
            catch (ParseException exc)
            {
            }

        }
        else
        { //text is invalid
            if (!userSaysRevert())
            { //user wants to edit
                return false; //don't let the editor go away
            }
        }
        return super.stopCellEditing();
    }

    /**
     * Lets the user know that the text they entered is
     * bad. Returns true if the user elects to revert to
     * the last good value.  Otherwise, returns false,
     * indicating that the user wants to continue editing.
     */
    protected boolean userSaysRevert()
    {
        Toolkit.getDefaultToolkit().beep();
        this.ftf.selectAll();
        Object[] options = {"Edit", "Revert"};
        int answer = JOptionPane.showOptionDialog(SwingUtilities.getWindowAncestor(ftf), "The value must be an integer between " + minimum + " and " + maximum + ".\n" + "You can either continue editing " + "or revert to the last valid value.", "Invalid Text Entered", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[1]);

        if (answer == 1)
        { //Revert!
            this.ftf.setValue(this.ftf.getValue());
            return true;
        }
        return false;
    }
}