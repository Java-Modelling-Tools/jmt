package jaba.panels;

import java.awt.Component;

import javax.swing.JTabbedPane;

import jaba.JabaModel;
import jmt.framework.gui.wizard.WizardPanel;

/**
 * Marker interface for JABA wizard panels that accept a JabaModel.
 * Allows JabaController to update all panels polymorphically without
 * requiring WizardPanel (in jmt-gui) to depend on JabaModel.
 */
public interface JabaWizardPanel {
	void setData(JabaModel data);

	/**
	 * Gives a panel the current data and redraws it. Result panels (graphs, tables) only show
	 * new results after this, so JabaController must call it on every panel after a solve.
	 */
	static void refresh(WizardPanel panel, JabaModel data) {
		if (panel instanceof JabaWizardPanel) {
			((JabaWizardPanel) panel).setData(data);
		}
		panel.redraw();
	}

	/** Disables the tabs that only support 2 classes when the model has 3 classes, enables them otherwise. */
	static void enableTwoClassOnlyTabs(JTabbedPane tabs, int classes, Component... twoClassOnly) {
		// by panel, not by position: tab components make getComponentCount() differ from getTabCount()
		for (Component panel : twoClassOnly) {
			int index = tabs.indexOfComponent(panel);
			if (index >= 0) {
				tabs.setEnabledAt(index, classes != 3);
			}
		}
	}
}
