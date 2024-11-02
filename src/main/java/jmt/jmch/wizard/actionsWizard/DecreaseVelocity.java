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
public class DecreaseVelocity extends AbstractMCHAction {

    private static final long serialVersionUID = 1L;

    /**
     * Defines an <code>Action</code> object with a default
     * description string and default icon.
     */
    public DecreaseVelocity(JMCHWizardPanel wizardpanel) {
        super("Decrease Velocity Animation", "SimTripleBack", wizardpanel);
        putValue(SHORT_DESCRIPTION, "Decrease velocity");
        //putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
        //putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_MASK));
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(ActionEvent e) {
        panel.decreaseVelocity();
    }

}
