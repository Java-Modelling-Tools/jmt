package jmt.gui.jsimgraph.controller.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import jmt.gui.jsimgraph.controller.Mediator;

/**
 *A class representing the "rotate" action.
 * @author Giuseppe De Cicco & Fabio Granara
 * Date: 22-08-2006
 * 
 */
public class ActionRotate extends AbstractJmodelAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Defines an <code>Action</code> object with a default
	 * description string and default icon.
	 */
	public ActionRotate(Mediator mediator) {
		super("Rotate", "Rotate", mediator);
		putValue(SHORT_DESCRIPTION, "Rotate selected stations");
		putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
		setEnabled(false);
	}

	/**
	 * Invoked when an action occurs.
	 */
	public void actionPerformed(ActionEvent e) {
		mediator.rotateComponent(null);
	}
}
