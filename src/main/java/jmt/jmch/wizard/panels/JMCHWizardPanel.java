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

/**
 * This interface is for all the methods associated to the actions.
 * Each WizardPanel has to implement those methods. If one panel does not support an action, then method can remain empty.
 *
 * @author Lorenzo Torri
 * Date: 29-mar-2024
 * Time: 15.43
 */
public interface JMCHWizardPanel{
    /** Opens the panel for help */
    public void openHelp();

    /** Starts the animation */
    public void startAnimation();

    /** Pauses the animation */
    public void pauseAnimation();

    /** Reloads the animation */
    public void reloadAnimation();

    /** Updates the animation to the next step */
    public void nextStepAnimation();

    /** Goes back to the main menu */
    public void exit();

    /** 
     * Stops the simulation, 
     * used in Markov Chain, but also in the Simulation Models in reverse mode (it's the simulation that decides when the animation has to be stopped and all the UI changes must be done) 
     */
    public void stopAnimation();
}
