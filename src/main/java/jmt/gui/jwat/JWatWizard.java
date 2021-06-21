package jmt.gui.jwat;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import jmt.framework.gui.help.HoverHelp;
import jmt.framework.gui.wizard.Wizard;

public class JWatWizard extends Wizard {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private HoverHelp help;
	private JLabel helpLabel;
	private JButton[] btnList;
	protected JToolBar toolBar;
	protected JMenuBar mMenuBar;

	public JWatWizard() {
	}

	public HoverHelp getHelp() {
		return help;
	}

	/**
	 * @return the button panel
	 */
	@Override
	protected JComponent makeButtons() {
		help = new HoverHelp();
		helpLabel = help.getHelpLabel();

		helpLabel.setBorder(BorderFactory.createEtchedBorder());

		ACTION_FINISH.putValue(Action.NAME, "Solve");
		ACTION_CANCEL.putValue(Action.NAME, "Exit");

		JPanel buttons = new JPanel();
		btnList = new JButton[5];

		/* Added first pane of all */

		JButton button_finish = new JButton(ACTION_FINISH);
		help.addHelp(button_finish, "Validates choices and start selected clustering");
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
		buttons.add(button_finish);
		btnList[2] = button_finish;
		buttons.add(button_cancel);
		btnList[3] = button_cancel;
		
		JPanel labelbox = new JPanel();
		labelbox.setLayout(new BorderLayout());
		labelbox.add(Box.createVerticalStrut(30), BorderLayout.WEST);
		labelbox.add(helpLabel, BorderLayout.CENTER);

		Box buttonBox = Box.createVerticalBox();
		buttonBox.add(buttons);
		buttonBox.add(labelbox);
		return buttonBox;
	}

	public void setEnableButton(String button, boolean enabled) {
		for (JButton element : btnList) {
			if (element.getText().equals(button)) {
				element.setEnabled(enabled);
				break;
			}
		}
		if (button.equals("Solve")) {
			for (int i = 0; i < toolBar.getComponentCount(); i++) {
				Component component = toolBar.getComponent(i);
				if (component instanceof JButton) {
					JButton item = (JButton) component;
					String description = (String) item.getAction().getValue(Action.SHORT_DESCRIPTION);
					if (description.equals("Clusterize")) {
						item.setEnabled(enabled);
						break;
					}
				}
			}
			for (int i = 0; i < mMenuBar.getMenuCount(); i++) {
				JMenu menu = mMenuBar.getMenu(i);
				for (int j = 0; j < menu.getMenuComponentCount(); j++) {
					Component component = menu.getMenuComponent(j);
					if (component instanceof JMenuItem) {
						JMenuItem item = (JMenuItem) component;
						String description = (String) item.getAction().getValue(Action.SHORT_DESCRIPTION);
						if (description.equals("Clusterize")) {
							item.setEnabled(enabled);
							break;
						}
					}
				}
			}
		}
	}

	public void setActionButton(String button, Action action) {
		for (JButton element : btnList) {
			if (element.getText().equals(button)) {
				element.setAction(action);
				element.setText(button);
				break;
			}
		}
	}

	public void setActionToolBar(String toolItem, Action action) {
		for (int i = 0; i < toolBar.getComponentCount(); i++) {
			Component component = toolBar.getComponent(i);
			if (component instanceof JButton) {
				JButton item = (JButton) component;
				String description = (String) item.getAction().getValue(Action.SHORT_DESCRIPTION);
				if (description.equals(toolItem)) {
					item.setAction(action);
					break;
				}
			}
		}
	}

	public void setActionMenuBar(String menuItem, Action action) {
		for (int i = 0; i < mMenuBar.getMenuCount(); i++) {
			JMenu menu = mMenuBar.getMenu(i);
			for (int j = 0; j < menu.getMenuComponentCount(); j++) {
				Component component = menu.getMenuComponent(j);
				if (component instanceof JMenuItem) {
					JMenuItem item = (JMenuItem) component;
					String description = (String) item.getAction().getValue(Action.SHORT_DESCRIPTION);
					if (description.equals(menuItem)) {
						item.setAction(action);
						item.setText(menuItem);
						break;
					}
				}
			}
		}
	}

	public void showNextPanel() {
		this.showNext();
	}
}
