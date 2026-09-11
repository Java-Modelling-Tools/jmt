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
package jmt.jmch.wizard;

import java.awt.BorderLayout;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToolBar;


import jmt.framework.gui.help.HoverHelp;
import jmt.framework.gui.wizard.Wizard;

/**
 * SuperClass of the MainWizard. It contains some methods of Wizard that are Overrided to fit the needs of this type of Wizard
 *
 * @author Lorenzo Torri
 * Date: 31-mar-2024
 * Time: 16.22
 */
public class JMCHWizard extends Wizard{

    private static final long serialVersionUID = 1L;
	private HoverHelp help;
	private JLabel helpLabel;
	private JButton[] btnList;

	protected JMenuBar menuBar;
    protected JToolBar toolBar;


    /**
     * Method of Wizard of creating the Button Panel.
     * Ovveride it to add also the helpLabel.
     */
	@Override
	protected JComponent makeButtons() {
		help = new HoverHelp();
		helpLabel = help.getHelpLabel();

		helpLabel.setBorder(BorderFactory.createEtchedBorder());

		//ACTION_FINISH.putValue(Action.NAME, "Solve");
		ACTION_CANCEL.putValue(Action.NAME, "Exit");

		JPanel buttons = new JPanel();
		btnList = new JButton[3];

		/* Added first pane of all */

		//JButton button_finish = new JButton(ACTION_FINISH);
		//help.addHelp(button_finish, "Validates choices and solve");
		JButton button_cancel = new JButton(ACTION_CANCEL);
		help.addHelp(button_cancel, "Exits the wizard discarding all changes");
		JButton button_next = new JButton(ACTION_NEXT);
		help.addHelp(button_next, "Moves on to the next step");
		JButton button_previous = new JButton(ACTION_PREV);
		help.addHelp(button_previous, "Goes back to the previous step");
		buttons.add(button_previous);
		btnList[0] = button_previous;
		buttons.add(button_next);
		btnList[1] = button_next;
		//buttons.add(button_finish);
		//btnList[2] = button_finish;
		buttons.add(button_cancel);
		btnList[2] = button_cancel;
		
		JPanel labelbox = new JPanel();
		labelbox.setLayout(new BorderLayout());
		labelbox.add(Box.createVerticalStrut(30), BorderLayout.WEST);
		labelbox.add(helpLabel, BorderLayout.CENTER);

		Box buttonBox = Box.createVerticalBox();
		buttonBox.add(buttons);
		buttonBox.add(labelbox);
		return buttonBox;
	}

	/**
	 * Set a button working or not based on the value or enabled
	 * @param button the string that represents the button
	 * @param enabled true or false
	 */
    public void setEnableButton(String button, boolean enabled) {
		for (JButton element : btnList) {
			if (element.getText().equals(button)) {
				element.setEnabled(enabled);
				break;
			}
		}
    }

	/** Return the HoverHelp of this Wizard */
    public HoverHelp getHoverHelp(){
        return help;
    }
}
