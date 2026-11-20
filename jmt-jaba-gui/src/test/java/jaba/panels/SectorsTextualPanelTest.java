package jaba.panels;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.lang.reflect.Proxy;

import javax.swing.JTextPane;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jaba.JabaController;
import jaba.JabaModel;
import jaba.link.SolverDispatcher;

/** The "Saturation Sectors - Text" tab lists the sectors of every solved example, 2 and 3 classes (issue #61). */
class SectorsTextualPanelTest {

	private static JTextPane textPane(Container c) {
		for (Component k : c.getComponents()) {
			if (k instanceof JTextPane) {
				return (JTextPane) k;
			}
			if (k instanceof Container) {
				JTextPane t = textPane((Container) k);
				if (t != null) {
					return t;
				}
			}
		}
		return null;
	}

	@ParameterizedTest
	@ValueSource(strings = { "2cl_3stat_1commonsat", "2cl_3stat_2commonsat", "3cl_3stat_1commonsat" })
	void textTabListsTheSectorsAfterASolve(String example) throws Exception {
		System.setProperty("java.awt.headless", "true");
		JabaModel solved = new JabaModel();
		File f = new File("../examples/jaba/" + example + ".jaba");
		assertTrue(solved.loadDocument(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f)));
		solved.setResults(new SolverDispatcher().solve(solved));
		JabaController controller = (JabaController) Proxy.newProxyInstance(JabaController.class.getClassLoader(),
				new Class<?>[] { JabaController.class }, (p, m, a) -> m.getName().equals("getData") ? solved : null);

		SectorsTextualPanel panel = new SectorsTextualPanel(controller);
		panel.gotFocus();

		JTextPane text = textPane(panel);
		assertNotNull(text);
		String shown = text.getText();
		assertFalse(shown.contains("will be here displayed"), example + ": placeholder shown instead of the sectors");
		assertTrue(shown.contains("Bottleneck Regions for"), example + ": " + shown);
		assertTrue(shown.contains("Class2:"), example + ": " + shown);
		if (example.startsWith("3cl")) {
			assertTrue(shown.contains("Class3:"), example + ": " + shown);
		}
	}
}
