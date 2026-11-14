package jaba.panels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.lang.reflect.Proxy;

import javax.swing.JTextPane;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jaba.JabaController;
import jaba.JabaModel;
import jaba.graphs.JabaCanvas;
import jaba.link.SolverDispatcher;

/**
 * After a solve, JabaWizard pushes the new results into every result panel with
 * JabaWizardPanel.refresh. Without it the graph panels keep showing the "no results" text (issue #61).
 */
class JabaPanelsRefreshTest {
	private static JabaModel solved;
	private static JabaController controller;

	@BeforeAll
	static void solveExample() throws Exception {
		System.setProperty("java.awt.headless", "true");
		solved = new JabaModel();
		File f = new File("../examples/jaba/2cl_3stat_1commonsat.jaba");
		assertTrue(solved.loadDocument(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f)));
		solved.setResults(new SolverDispatcher().solve(solved));
		controller = (JabaController) Proxy.newProxyInstance(JabaController.class.getClassLoader(), new Class<?>[] { JabaController.class },
				(p, m, a) -> m.getName().equals("getData") ? solved : null);
	}

	private static int count(Container c, Class<?> type) {
		int n = 0;
		for (Component k : c.getComponents()) {
			if (type.isInstance(k)) {
				n++;
			}
			if (k instanceof Container) {
				n += count((Container) k, type);
			}
		}
		return n;
	}

	@Test
	void convexHullShowsTheGraphAfterRefresh() {
		ConvexHullPanel p = new ConvexHullPanel(controller);
		JabaWizardPanel.refresh(p, solved);
		assertEquals(1, count(p, JabaCanvas.class));
		assertEquals(0, count(p, JTextPane.class));
	}

	@Test
	void performanceIndicesShowsTheGraphAfterRefresh() {
		PerformanceIndicesPanel p = new PerformanceIndicesPanel(controller);
		JabaWizardPanel.refresh(p, solved);
		assertEquals(1, count(p, JabaCanvas.class));
	}

	@Test
	void sectorsGraphicShowsTheGraphAfterRefresh() {
		SectorsGraphicPanel p = new SectorsGraphicPanel();
		JabaWizardPanel.refresh(p, solved);
		assertEquals(1, count(p, JabaCanvas.class));
	}
}
