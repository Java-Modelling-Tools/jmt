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
package jmt.jmch.wizard.panels;

import jmt.framework.gui.wizard.WizardPanel;
import jmt.jmch.wizard.MainWizard;

/**
 * Abstract class to group all the new methods used in the panels of the wizard
 *
 * @author Lorenzo Torri
 * Date: 29-mar-2024
 * Time: 15.43
 */
public abstract class JMCHWizardPanel extends WizardPanel{
    protected MainWizard parent;

    /** Opens the panel for help */
    public void openHelp(){ }

    /** Starts the animation */
    public void startAnimation(){ }

    /** Pauses the animation */
    public void pauseAnimation(){ }

    /** Reloads the animation */
    public void reloadAnimation(){ }

    /** Updates the animation to the next step */
    public void nextStepAnimation(){ }

    /** Double the animation's velocity */
    public void increaseVelocity() { }

    /** Halve the animation's velocity */
    public void decreaseVelocity() { }

    /** Go back to the main menu */
    public void exit(){ 
        parent.close();
    }

    /** Open the about page */
    public void about(){
        parent.about();
    }

    /** 
     * Stops the simulation, 
     * used in Markov Chain, but also in the Simulation Models in reverse mode (it's the simulation that decides when the animation has to be stopped and all the UI changes must be done) 
     */
    public void stopAnimation(){ }

    public void setLastPanel(){ }

    @Override
    public String getName() {
        return null;
    }
}
