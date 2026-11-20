package jaba.panels;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * With 3 classes only the 2-class-only tabs (Performance Indices, All-In-One) are disabled.
 * Wizard gives every tab a JLabel tab component, so the tabbed pane has one more child than tabs:
 * counting from getComponentCount() disabled "Saturation Sectors - Text" instead (issue #61).
 */
class JabaWizardTabsTest {

	private JTabbedPane tabs;
	private JPanel performance;
	private JPanel allInOne;
	private JPanel text;

	@BeforeAll
	static void headless() {
		System.setProperty("java.awt.headless", "true");
	}

	/** Same layout as JabaWizard: ..., Performance Indices, All-In-One, Saturation Sectors - Text. */
	private void buildTabs() {
		tabs = new JTabbedPane();
		performance = new JPanel();
		allInOne = new JPanel();
		text = new JPanel();
		JPanel[] panels = { new JPanel(), new JPanel(), performance, allInOne, text };
		for (int i = 0; i < panels.length; i++) {
			tabs.insertTab(null, null, panels[i], "", i);
			tabs.setTabComponentAt(i, new JLabel("tab " + i));
		}
	}

	private boolean enabled(JPanel p) {
		return tabs.isEnabledAt(tabs.indexOfComponent(p));
	}

	@Test
	void threeClassesDisableOnlyTheTwoClassTabs() {
		buildTabs();
		JabaWizardPanel.enableTwoClassOnlyTabs(tabs, 3, performance, allInOne);
		assertFalse(enabled(performance));
		assertFalse(enabled(allInOne));
		assertTrue(enabled(text), "Saturation Sectors - Text must stay enabled");
	}

	@Test
	void twoClassesEnableThemAgain() {
		buildTabs();
		JabaWizardPanel.enableTwoClassOnlyTabs(tabs, 3, performance, allInOne);
		JabaWizardPanel.enableTwoClassOnlyTabs(tabs, 2, performance, allInOne);
		assertTrue(enabled(performance));
		assertTrue(enabled(allInOne));
		assertTrue(enabled(text));
	}
}
