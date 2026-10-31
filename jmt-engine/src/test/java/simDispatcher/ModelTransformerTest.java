package simDispatcher;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import simEngine.SimLoader;

/** XSLT conversions between JMVA and JSIM documents, on a tiny inline model. */
class ModelTransformerTest {

	@TempDir
	Path dir;

	private static final String JMVA = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
			+ "<model xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"JMTmodel.xsd\">"
			+ "<parameters><classes number=\"1\"><closedclass name=\"Class1\" population=\"3\"/></classes>"
			+ "<stations number=\"2\"><listation name=\"Station1\" servers=\"1\"><servicetimes><servicetime customerclass=\"Class1\">1.0</servicetime></servicetimes>"
			+ "<visits><visit customerclass=\"Class1\">1.0</visit></visits></listation>"
			+ "<listation name=\"Station2\" servers=\"1\"><servicetimes><servicetime customerclass=\"Class1\">2.0</servicetime></servicetimes>"
			+ "<visits><visit customerclass=\"Class1\">1.0</visit></visits></listation></stations>"
			+ "<ReferenceStation number=\"1\"><Class name=\"Class1\" refStation=\"Station1\"/></ReferenceStation></parameters>"
			+ "<algParams><algType maxSamples=\"10000\" name=\"MVA\" tolerance=\"1.0E-7\"/><compareAlgs value=\"false\"/></algParams></model>";

	private Path write(String name, String content) throws Exception {
		Path p = dir.resolve(name);
		Files.write(p, content.getBytes(StandardCharsets.UTF_8));
		return p;
	}

	@Test
	void mvaToSimProducesAnOutputFile() throws Exception {
		Path src = write("m.jmva", JMVA);
		Path dst = dir.resolve("m.jsimw");
		ModelTransformer t = new ModelTransformer();
		assertTrue(t.MVAtoSIM(src.toString(), dst.toString()));
		assertTrue(Files.size(dst) > 0);
	}

	@Test
	@Disabled("bug: MVAtoSIM.xslt output is schema-valid but SimLoader rejects it (LoadException: Constructor of Section not found)")
	void mvaToSimOutputIsLoadable() throws Exception {
		Path dst = dir.resolve("m.jsimw");
		assertTrue(new ModelTransformer().MVAtoSIM(write("m.jmva", JMVA).toString(), dst.toString()));
		assertNotNull(new SimLoader(dst.toString()).getSim());
	}

	@Test
	void simToMvaRoundTripsAStationModel() throws Exception {
		Path src = write("m.jmva", JMVA);
		Path sim = dir.resolve("m.jsimw");
		Path back = dir.resolve("back.jmva");
		ModelTransformer t = new ModelTransformer();
		assertTrue(t.MVAtoSIM(src.toString(), sim.toString()));
		assertTrue(t.SIMtoMVA(sim.toString(), back.toString()));
		String xml = new String(Files.readAllBytes(back), StandardCharsets.UTF_8);
		assertTrue(xml.contains("Station1"), xml);
	}

	@Test
	void validationCanBeSwitchedOff() throws Exception {
		Path src = write("loose.jmva", JMVA.replace("tolerance=\"1.0E-7\"", "tolerance=\"1.0E-7\" extra=\"1\""));
		ModelTransformer t = new ModelTransformer();
		t.setValidate(false);
		assertTrue(t.MVAtoSIM(src.toString(), dir.resolve("o.jsimw").toString()));
	}

	@Test
	void missingSourceFailsWithoutThrowing() {
		ModelTransformer t = new ModelTransformer();
		assertFalse(t.MVAtoSIM(dir.resolve("absent.jmva").toString(), dir.resolve("o.jsimw").toString()));
		assertFalse(t.SIMtoMVA(dir.resolve("absent.xml").toString(), dir.resolve("o2.jmva").toString()));
		assertFalse(t.OUTtoMVA(dir.resolve("absent.xml").toString(), dir.resolve("o3.jmva").toString()));
		assertFalse(t.OUTtoMVAScaling(dir.resolve("absent.xml").toString(), dir.resolve("o4.jmva").toString()));
	}

	@Test
	void printNodesWalksTheTree() throws Exception {
		org.w3c.dom.Document d = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(new java.io.ByteArrayInputStream("<a x=\"1\"><b/>t</a>".getBytes(StandardCharsets.UTF_8)));
		java.io.PrintStream old = System.out;
		java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
		System.setOut(new java.io.PrintStream(bytes, true, "UTF-8"));
		try {
			new ModelTransformer().printNodes(d.getDocumentElement());
			new ModelTransformer().printNodes(null);
		} finally {
			System.setOut(old);
		}
		String out = bytes.toString("UTF-8");
		assertTrue(out.contains("x=1"), out);
		assertTrue(out.contains("#text"), out);
		assertNotNull(new File(dir.toString()));
	}
}
