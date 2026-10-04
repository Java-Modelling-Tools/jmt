package jmva;

import jmt.framework.gui.help.HoverHelp;
import analytical.ExactModel;

/**
 * Interface for the JMVA wizard controller, accessible from JMVA panels
 * without a compile-time dependency on JMVAController (which lives in jmt-jsimg-gui).
 */
public interface JMVAController {
	ExactModel getData();
	HoverHelp getHelp();
	void switchFromSTVtoSD();
	void switchFromSDtoSTV();
	void updateAlgoPanel(Boolean isClosed, Boolean isOpen, Boolean isAlgowhatif, Boolean isLoadDependent, Boolean isPriority);
}
