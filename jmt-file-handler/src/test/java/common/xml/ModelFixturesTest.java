package common.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;

import common.definitions.CommonModel;

class ModelFixturesTest {

	@Test
	void loadsOneExampleHeadless() {
		File f = new File(new File(ModelFixtures.examplesDir(), "jsim"), "qn_models/closed_1class_2stat.jsimg");
		CommonModel m = new CommonModel();
		assertTrue(XMLReader.loadModel(f, m));
		assertEquals(2, m.getStationKeys().size()); // Delay 1 + Queue 1
		assertEquals(2, m.getClassKeys().size());
		assertFalse(ModelFixtures.writeToString(m).isEmpty());
	}

	@Test
	void listExamplesIsSortedAndFindsJsimg() {
		List<File> l = ModelFixtures.listExamples("*.jsimg");
		assertFalse(l.isEmpty());
		for (int i = 1; i < l.size(); i++) {
			assertTrue(l.get(i - 1).compareTo(l.get(i)) <= 0);
		}
	}
}
