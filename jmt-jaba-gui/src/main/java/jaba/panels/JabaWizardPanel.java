package jaba.panels;

import jaba.JabaModel;

/**
 * Marker interface for JABA wizard panels that accept a JabaModel.
 * Allows JabaController to update all panels polymorphically without
 * requiring WizardPanel (in jmt-gui) to depend on JabaModel.
 */
public interface JabaWizardPanel {
	void setData(JabaModel data);
}
