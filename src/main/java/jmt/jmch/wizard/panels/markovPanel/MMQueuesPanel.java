/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */
package jmt.jmch.wizard.panels.markovPanel;

import java.util.ArrayList;

import javax.swing.AbstractButton;

import jmt.framework.gui.components.JMTToolBar;
import jmt.framework.gui.help.HoverHelp;
import jmt.gui.common.JMTImageLoader;
import jmt.jmch.Constants;
import jmt.jmch.wizard.actionsWizard.AbstractMCHAction;
import jmt.jmch.wizard.actionsWizard.Help;
import jmt.jmch.wizard.actionsWizard.PauseSimulation;
import jmt.jmch.wizard.actionsWizard.StartSimulation;
import jmt.jmch.wizard.actionsWizard.StopSimulation;
import jmt.jmch.wizard.panels.JMCHWizardPanel;
import jmt.jmch.wizard.MainWizard;

/**
 * Panel of the main Wizard for the Markov chains.
 *
 * @author Lorenzo Torri
 * Date: 15-apr-2024
 * Time: 14.29
 */
public class MMQueuesPanel extends JMCHWizardPanel{
    /*
     * This class does not have all the features of the panel as in the AnimationPanel, because I had to re-use the MMQueues.java file.
     * For example there is no JMenu in here, since it was already defined in MMQueues
     */

    private String selectedMethod;
    private MMQueues mq;

    //---- abstract actions for the toolbar
    private AbstractMCHAction start;
    private AbstractMCHAction pause;
    private AbstractMCHAction stop;
    private AbstractMCHAction openHelp;

    private HoverHelp help;

    public MMQueuesPanel(MainWizard main, String selectedMethod){
        this.parent = main;
        this.selectedMethod = selectedMethod;
        
        help = main.getHoverHelp();

        start = new StartSimulation(this);
        pause = new PauseSimulation(this);
        stop = new StopSimulation(this);
        openHelp = new Help(this,"JMCH");

        pause.setEnabled(false);
        stop.setEnabled(false);

        createToolbar();
        
        mq = new MMQueues(main, this, selectedMethod);
        this.add(mq);
    }

    @Override
    public String getName() {
        String name;
        if (selectedMethod == "mm1") {
			name = "M/M/1";
		} else if (selectedMethod == "mm1k") {
			name = "M/M/1/k";
		} else if (selectedMethod == "mmn Finite Capacity Station") {
			name = "M/M/n";
		} else{
			name = "M/M/n/k";
		}
        return name;
    }

    /**
	 * Update the toolbar for the Scheduling Window
	 */
    protected void createToolbar() {
        JMTToolBar toolbar = new JMTToolBar(JMTImageLoader.getImageLoader());	

        //first add all the icons with their actions
        AbstractMCHAction[] actions = new AbstractMCHAction[] {start, pause, stop, null, openHelp}; // Builds an array with all actions	
        toolbar.populateToolbar(actions);
        ArrayList<AbstractButton> buttons = new ArrayList<AbstractButton>(); //create a list of AbstractButtons for the helpLabel
		buttons.addAll(toolbar.populateToolbar(actions));

        //add help for each Action/JComboBox with helpLabel
		for (int i = 0; i < buttons.size(); i++) {
			AbstractButton button = buttons.get(i);
			help.addHelp(button, Constants.HELP_BUTTONS_MARKOV[i]);
		}
		  
		parent.setToolBar(toolbar);
	}

    @Override
    public boolean canGoBack() {
		return true;
	}

    @Override
    public void startAnimation() {
        start.setEnabled(false);
        pause.setEnabled(true);
        stop.setEnabled(true);
        mq.playBActionPerformed();
    }

    @Override
    public void pauseAnimation() {
        start.setEnabled(false);
        pause.setEnabled(false);
        stop.setEnabled(true);
        mq.pauseBActionPerformed();
    }

    @Override
    public void stopAnimation() {
        start.setEnabled(true);
        pause.setEnabled(false);
        stop.setEnabled(false);
        mq.stopBActionPerformed();
    }

    @Override
    public void setLastPanel(){
        parent.setLastPanel(Constants.PANEL_MARKOV);
    }

    @Override
    public void lostFocus() { 
        setLastPanel();
    }

}
