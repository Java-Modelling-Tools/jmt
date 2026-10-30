package jsimgraph.controller.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import jsimgraph.controller.Mediator;
import log.SimEventTracer;

/**
 * Action to activate or deactivate the Visual Debug Tracer mode.
 * Simulation events will be recorded into the traces/ directory.
 * Only users who run from a packaged JMT JAR archive are allowed to activate it.
 */
public class VisualDebugTracer extends AbstractJmodelAction {

	private static final long serialVersionUID = 1L;

	public VisualDebugTracer(Mediator mediator) {
		super("Visual Debug Tracer Mode", mediator);
		putValue(SHORT_DESCRIPTION, "Activate visual debug event tracing (logs in traces/ folder)");
		putValue(MNEMONIC_KEY, KeyEvent.VK_V);
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.ALT_MASK | InputEvent.SHIFT_DOWN_MASK));
		setSelectable(true);
		setSelected(SimEventTracer.isEnabled());
		setEnabled(false);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (!SimEventTracer.isRunningFromJar() && !SimEventTracer.isVmOptionActive()) {
			setSelected(false);
			JOptionPane.showMessageDialog(mediator.getMainWindow(),
					"Visual Debug Tracer mode can only be activated when running from a packaged JMT JAR archive.",
					"Visual Debug Tracer - JAR Required",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		boolean activate = !SimEventTracer.isEnabled();
		if (activate) {
			boolean ok = mediator.toggleVisualDebugTracer(true);
			if (ok) {
				setSelected(true);
				JOptionPane.showMessageDialog(mediator.getMainWindow(),
						"Visual Debug Tracer mode activated.\nSimulation traces will be recorded in the 'traces/' folder.",
						"Visual Debug Tracer",
						JOptionPane.INFORMATION_MESSAGE);
			} else {
				setSelected(false);
			}
		} else {
			mediator.toggleVisualDebugTracer(false);
			setSelected(false);
			JOptionPane.showMessageDialog(mediator.getMainWindow(),
					"Visual Debug Tracer mode deactivated.",
					"Visual Debug Tracer",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}
}
