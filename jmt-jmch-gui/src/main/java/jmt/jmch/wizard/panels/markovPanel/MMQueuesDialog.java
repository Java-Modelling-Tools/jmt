/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */
package jmt.jmch.wizard.panels.markovPanel;

import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Dialog for the MMQueues
 * 
 * @author Lorenzo Torri
 * Date: 15-apr-2024
 * Time: 18.15
 */
public class MMQueuesDialog extends JDialog implements PropertyChangeListener {
    private int typedValue = 0;

    // maximum number of server (c) 
	private int maximumServer = 30;

    //--------------components of the JDialog-----------------
    private JOptionPane optionPane;
    private  JTextField textField;
    private String btnString1 = "Enter";

    public MMQueuesDialog(Frame aFrame){
        super(aFrame, true);

        initGUI();
    }

    public int getValidatedValue() {
		return typedValue;
	}

    /** 
     * Initialize the GUI of the Dialog 
     */
    private void initGUI(){
        setTitle("Select the Number of Servers");
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(1,2));

        JLabel numberLabel = new JLabel("Number of servers c:");
        mainPanel.add(numberLabel);

        textField = new JTextField(10);
		textField.setEnabled(true);
		textField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				char c = e.getKeyChar();
				if (!((c >= '0') && (c <= '9') || (c == KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE))) {
					getToolkit().beep();
					e.consume();
				}
			}
		});
        mainPanel.add(textField);
        
        Object[] array = {mainPanel};
        Object[] options = {btnString1};

        optionPane = new JOptionPane(array, JOptionPane.DEFAULT_OPTION, JOptionPane.OK_OPTION, null, options, options[0]);
        setContentPane(optionPane);
  
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); //avoid closing the window

        // Register an event handler that reacts to option pane state changes.
        optionPane.addPropertyChangeListener(this);
    }

    /** 
     * React to changes inside the JDialog
     */
    public void propertyChange(PropertyChangeEvent e) {
        if (isVisible() && (e.getSource() == optionPane)) {
            Object value = optionPane.getValue();

            //Reset the JOptionPane's value.
			//If you do not do this, then if the user
			//presses the same button next time, no
			//property change event will be fired.
			optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);

            if (btnString1.equals(value)) {
                try {
                    typedValue = Integer.parseInt(textField.getText());
                } catch (NumberFormatException nfe) {
                    typedValue = 0;
                }

                if ((typedValue > maximumServer) || (typedValue < 1)) {
                    //text was invalid
                    textField.selectAll();
                    JOptionPane.showMessageDialog(MMQueuesDialog.this, "Sorry, '" + typedValue + "' " + "is not a valid response.\n"
                            + "Please enter a number between 2 and " + maximumServer + ".", "Please try again", JOptionPane.ERROR_MESSAGE);
                    typedValue = 0;
                } else {
                    clearAndHide();
                }        
            }           
        }
    }

    /** 
     * Clear the dialog and close it
     */
    public void clearAndHide() {
        textField.setText(null);
        setVisible(false);
    }
}
