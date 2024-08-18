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

package jmt.jmch.wizard.actionsWizard;

import jmt.framework.gui.listeners.AbstractJMTAction;
import jmt.gui.common.JMTImageLoader;
import jmt.jmch.wizard.panels.JMCHWizardPanel;

/**
 * Defines the Abstract action of this application, it is connected to the
 * WizardPanelMCH which has the responsibility to react to it.

 * @author Lorenzo Torri
 * Date: 29-mar-2024
 * Time: 15.30
 */
public abstract class AbstractMCHAction extends AbstractJMTAction {

    private static final long serialVersionUID = 1L;
    protected JMCHWizardPanel panel;

    /**
     * Defines an <code>Action</code> object with a default
     * description string and default icon.
     */
    public AbstractMCHAction(String name, JMCHWizardPanel panel) {
        this.setName(name);
        this.panel = panel;
    }

    /**
     * Defines an <code>Action</code> object with the specified
     * description string and a the specified icon.
     */
    public AbstractMCHAction(String name, String iconName, JMCHWizardPanel panel) {
        this(name, panel);
        this.setIcon(iconName, JMTImageLoader.getImageLoader());
    }
}
