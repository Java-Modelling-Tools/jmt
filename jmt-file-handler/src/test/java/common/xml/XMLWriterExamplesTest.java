package common.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import common.definitions.CommonModel;

/** XMLWriter on models loaded from the example {@code <sim>} sections: feature coverage, schema validity, round trip. */
class XMLWriterExamplesTest {

	private static CommonModel sim(String rel) {
		return SimXml.loadSim(new File(new File(ModelFixtures.examplesDir(), "jsim"), rel));
	}

	private static String tail(String xml) {
		return xml.substring(xml.indexOf("<sim")).replaceAll("logPath=\"[^\"]*\"", "");
	}

	private static int count(Document d, String section) {
		int n = 0;
		NodeList l = d.getElementsByTagName("section");
		for (int i = 0; i < l.getLength(); i++) {
			if (section.equals(((Element) l.item(i)).getAttribute("className"))) {
				n++;
			}
		}
		return n;
	}

	@ParameterizedTest
	@ValueSource(strings = { "qn_models/closed_1class_2stat.jsimg", "qn_models/closed_2class_2stat_switch.jsimg",
			"qn_models/closed_2class_3stat_switch.jsimg", "qn_models/open_1class_1stat_mm1k_fcr.jsimg",
			"qn_models/open_1class_3stat_advfork.jsimg", "qn_models/open_2class_3stat_fcr.jsimg",
			"qn_models/open_3class_3stat_loadbal.jsimg", "qn_models/open_3class_4stat.jsimg", "qn_models/open_1class_1stat_mg1ps.jsimg" })
	void exampleWritesValidXmlAndIsStableOnSecondWrite(String rel) throws Exception {
		String first = ModelFixtures.writeToString(sim(rel));
		assertEquals(new ArrayList<String>(), XMLWriterTest.validate(first), rel);
		String second = ModelFixtures.writeToString(SimXml.parseSim(first.substring(first.indexOf("<sim"))));
		assertEquals(tail(first), tail(second), rel);
	}

	@Test
	void forkJoinSectionsAreWritten() {
		Document d = XMLWriter.getDocument(sim("qn_models/open_1class_3stat_fork.jsimg"), "x");
		assertEquals(1, count(d, "Fork"));
		assertEquals(1, count(d, "Join"));
	}

	@Test
	void classSwitchSectionIsWritten() {
		Document d = XMLWriter.getDocument(sim("qn_models/closed_2class_2stat_switch.jsimg"), "x");
		assertEquals(1, count(d, "ClassSwitch"));
	}

	@Test
	void delayAndRouterSectionsAreWritten() {
		Document d = XMLWriter.getDocument(sim("qn_models/open_2class_3stat_fcr.jsimg"), "x");
		assertTrue(count(d, "Delay") >= 1);
		assertEquals(1, d.getElementsByTagName("blockingRegion").getLength(), "finite capacity region");
	}

	@Test
	void preloadIsWrittenForClosedModels() {
		Document d = XMLWriter.getDocument(sim("qn_models/closed_2class_3stat_switch.jsimg"), "x");
		assertEquals(1, d.getElementsByTagName("preload").getLength());
	}

	@Test
	void petriNetExampleWritesPlacesAndTransitions() throws Exception {
		File f = null;
		for (File c : ModelFixtures.listExamples("*.jsimg")) {
			if (c.getPath().contains("pn_models")) {
				f = c;
				break;
			}
		}
		assertNotNull(f, "no pn_models .jsimg found under examples/jsim");
		CommonModel m = SimXml.loadSim(f);
		String xml = ModelFixtures.writeToString(m);
		Document d = SimXml.parseDoc(xml);
		assertTrue(count(d, "Storage") >= 1, "places are Storage sections");
		assertTrue(count(d, "Enabling") >= 1 && count(d, "Firing") >= 1, "transitions");
		assertEquals(new ArrayList<String>(), XMLWriterTest.validate(xml));
	}

	@Test
	@Tag("slow")
	void everyQnAndPnExampleWritesValidXml() throws Exception {
		List<File> all = ModelFixtures.listExamples("*.jsimg");
		assertFalse(all.isEmpty(), "no examples found");
		List<String> bad = new ArrayList<String>();
		for (File f : all) {
			try {
				String xml = ModelFixtures.writeToString(SimXml.loadSim(f));
				List<String> errs = XMLWriterTest.validate(xml);
				if (!errs.isEmpty()) {
					bad.add(f.getName() + ": " + errs.get(0));
				}
			} catch (RuntimeException e) {
				bad.add(f.getName() + ": " + e);
			}
		}
		assertEquals(new ArrayList<String>(), bad);
	}
}
