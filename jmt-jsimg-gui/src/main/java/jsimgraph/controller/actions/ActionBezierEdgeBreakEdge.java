package jsimgraph.controller.actions;

import jsimgraph.controller.BezierEdgeModificationState;
import jsimgraph.controller.Mediator;

import java.awt.event.ActionEvent;

/**
 * Provides the Action Bezier Edge Break Edge functionality of the actions.
 */
/**
 * Provides the Action Bezier Edge Break Edge functionality of the actions.
 */
/**
 * Provides the Action Bezier Edge Break Edge functionality of the actions.
 */
/**
 * Provides the Action Bezier Edge Break Edge functionality of the actions.
 */
public class ActionBezierEdgeBreakEdge extends AbstractJmodelAction {


    private static final long serialVersionUID = 1L;

    /**
     * Defines an <code>Action</code> object with a default
     * description string and default icon.
     */
    public ActionBezierEdgeBreakEdge(Mediator mediator) {
        super("BreakArc", "BezierBreakArc", mediator);
        putValue(SHORT_DESCRIPTION, "Break a bezier edge");
        setGroup(1);
        setSelectable(true);
        setEnabled(true);
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(ActionEvent e) {
        if (mediator.getMouseListener().getCurrentState() instanceof BezierEdgeModificationState){
            ((BezierEdgeModificationState) mediator.getMouseListener().getCurrentState()).setState("BREAK");
        }
    }
}
