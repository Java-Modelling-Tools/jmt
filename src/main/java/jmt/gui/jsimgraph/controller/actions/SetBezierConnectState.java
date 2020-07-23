package jmt.gui.jsimgraph.controller.actions;


import java.awt.event.ActionEvent;

import jmt.gui.jsimgraph.controller.Mediator;

public class SetBezierConnectState extends AbstractJmodelAction {

    private static final long serialVersionUID = 1L;

    /**
     * Defines an <code>Action</code> object with a default
     * description string and default icon.
     */
    public SetBezierConnectState(Mediator mediator) {
        super("Connect", "BezierLink", mediator);
        putValue(SHORT_DESCRIPTION, "Draw arc between two stations");
        setSelectable(true);
        setGroup(0);
        setEnabled(false);
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(ActionEvent e) { mediator.setBezierConnectState(); }

}