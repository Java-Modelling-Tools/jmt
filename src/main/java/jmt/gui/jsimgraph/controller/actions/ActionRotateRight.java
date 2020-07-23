package jmt.gui.jsimgraph.controller.actions;

import jmt.gui.jsimgraph.controller.Mediator;

import java.awt.event.ActionEvent;

/**

 * @author Emma Bortone
 * Date: 7-April-2020
 * Time: 14.48.49

 */

public class ActionRotateRight extends AbstractJmodelAction {

    private static final long serialVersionUID = 1L;

    /**
     * Defines an <code>Action</code> object with a default
     * description string and default icon.
     */
    public ActionRotateRight(Mediator mediator) {
        super("RotateRight", "RotateRight", mediator);
        putValue(SHORT_DESCRIPTION, "Rotate right the selected stations");

    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(ActionEvent e) { mediator.rotateRight(null);
        
        }
}
