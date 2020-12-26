package jmt.gui.jsimgraph.controller.actions;

import jmt.gui.jsimgraph.controller.BezierEdgeModificationState;
import jmt.gui.jsimgraph.controller.Mediator;

import java.awt.event.ActionEvent;

public class ActionBezierEdgeAddControlPoint extends AbstractJmodelAction {


    private static final long serialVersionUID = 1L;

    /**
     * Defines an <code>Action</code> object with a default
     * description string and default icon.
     */
    public ActionBezierEdgeAddControlPoint(Mediator mediator) {
        super("AddPoint", "BezierAddPoint", mediator);
        putValue(SHORT_DESCRIPTION, "Add a new control point");
        setGroup(1);
        setSelectable(true);
        //System.out.println(SELECTABLE);
        setEnabled(true);
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(ActionEvent e) {
        if (mediator.getMouseListener().getCurrentState() instanceof BezierEdgeModificationState){
            ((BezierEdgeModificationState) mediator.getMouseListener().getCurrentState()).setState("ADD_POINT");
        }
    }
}
