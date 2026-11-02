package jsimgraph.controller.actions;

import jsimgraph.controller.Mediator;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Provides the Action Cancel Edge Drawing functionality of the actions.
 */
/**
 * Provides the Action Cancel Edge Drawing functionality of the actions.
 */
/**
 * Provides the Action Cancel Edge Drawing functionality of the actions.
 */
/**
 * Provides the Action Cancel Edge Drawing functionality of the actions.
 */
public class ActionCancelEdgeDrawing extends AbstractJmodelAction {

    private static final long serialVersionUID = 1L;

    public ActionCancelEdgeDrawing(Mediator mediator) {
        super("Interrupt drawing of curved arc", "CancelBezierDrawing", mediator);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
        setEnabled(false);
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(ActionEvent e) {
//        System.out.println("ActionCancelEdgeDrawing -> actionPerformed");
        mediator.cancelEdgeDrawing();
    }

}
