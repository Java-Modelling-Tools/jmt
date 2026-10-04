package jaba;

import jmt.framework.gui.help.HoverHelp;

/**
 * Interface for the JABA wizard controller, accessible from jmt-jaba-gui panels
 * without a compile-time dependency on jaba.JabaController (which lives in jmt-jsimg-gui).
 */
public interface JabaController {
	JabaModel getData();
	HoverHelp getHelp();
	void switchFromSTVtoSD();
	void switchFromSDtoSTV();
	void solve();
}
