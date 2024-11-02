package jmt.jmch.wizard.actionsWizard;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import jmt.jmch.wizard.panels.JMCHWizardPanel;


/**
 * @author Lorenzo Torri
 * Date: 20-sept-2024
 * Time: 15.47
 */
public class IncreaseVelocity extends AbstractMCHAction {

    private static final long serialVersionUID = 1L;

    /**
     * Defines an <code>Action</code> object with a default
     * description string and default icon.
     */
    public IncreaseVelocity(JMCHWizardPanel wizardpanel) {
        super("Increase Velocity Animation", "SimTripleFront", wizardpanel);
        putValue(SHORT_DESCRIPTION, "Increase velocity");
        //putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
        //putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_MASK));
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(ActionEvent e) {
        panel.increaseVelocity();
    }

}
