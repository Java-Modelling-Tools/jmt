package jmt.gui.jsimgraph.controller.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import jmt.gui.jsimgraph.controller.Mediator;

import javax.swing.*;

public class DebugSolver extends AbstractJmodelAction {
    private static final long serialVersionUID = 1L;
    public DebugSolver(Mediator mediator) {
        super("Debug Simulation", "DebugSolver", mediator);
        putValue(SHORT_DESCRIPTION, "Configure and start simulation with debug options");
        putValue(MNEMONIC_KEY, KeyEvent.VK_D);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.ALT_MASK));
        setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        mediator.editDebugParameter();
    }
}
