package jsimwiz;

/**
 * Interface for the JSIM Wizard controller, usable from jsimwiz panels
 * without depending on JSIMWizMain (which lives in jmt-jsimg-gui).
 */
public interface JSIMWizController {
	void launchToJMVA();
	void launchSimulation();
}
