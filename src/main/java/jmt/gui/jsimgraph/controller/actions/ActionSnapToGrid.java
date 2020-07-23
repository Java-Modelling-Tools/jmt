package jmt.gui.jsimgraph.controller.actions;

import java.awt.event.ActionEvent;

import jmt.gui.jsimgraph.controller.Mediator;
/**

 * @author Emma Bortone
 * Date: 7-April-2020
 * Time: 14.48.49

 */

public class ActionSnapToGrid extends AbstractJmodelAction {

    private static final long serialVersionUID = 1L;

    /**
     * Defines an <code>Action</code> object with a default
     * description string and default icon.
     */
    public ActionSnapToGrid(Mediator mediator) {
        super("ShowGrid", "ShowGrid", mediator);
        putValue(SHORT_DESCRIPTION, "Snap to grid");

    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(ActionEvent e) { //
        mediator.toggleGrid();
        }
}
