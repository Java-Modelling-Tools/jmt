package simEngine;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jmt.common.exception.LoadException;

/** Loads and initializes (but does not run) the bundled models; exercises the XML loader and Simulation setup. */
@Tag("simulation")
class SimLoaderTest {

	@TempDir
	Path dir;

	private static final String[] MODELS = { "closed_1class_2stat", "closed_2class_2stat_switch", "open_1class_3stat_fork",
			"open_1class_1stat_mm1k_fcr", "open_3class_3stat_loadbalNEW", "region", "terminal", "polling", "preemptive",
			"ps-priority", "cpn", "simple_gspn", "producer_consumer", "readers_writers", "philosophers", "logger",
			"open_1class_3stat_advfork", "2FJ_2tasks_nestedOK", "burst-mmpp-01", "gamma-01", "pareto-01", "case3_bas_sd",
			"classswitch", "delay-closed", "indices-01", "open_2class_3stat_fcrNEW", "ispass03",
			"HO5-hadoop", "Sec6_4_Obj1_UnavCV15_S60_", "SEED1test_3Job2Cl2CS2FJ_2NEWparameters", "fcfs-01", "ps-01",
			"open_1class_1stat_mg1ps", "open_1class_1stat_mg1fcfs", "convers_test1LDcrescNEW", "conBeta13fescNEW",
			"PhTypeconERROREsuiRdT20_6", "FCR_bugPerfInd1FCR", "LPS_C3_K2_OK", "NOTgoodRANDOM_FCFS_New", "Sec2_4_OptPower_JSIMg" };

	private Simulation load(String name) throws Exception {
		File f = QueueNet.NetFixtures.model(dir, name);
		SimLoader loader = new SimLoader(f.getAbsolutePath());
		Simulation sim = loader.getSim();
		sim.setXmlSimModelDefPath(f.getAbsolutePath());
		sim.setRandomEngineSeed(1L);
		sim.initialize();
		return sim;
	}

	@Test
	void everyListedModelLoadsAndInitializes() throws Exception {
		StringBuilder failures = new StringBuilder();
		for (String name : MODELS) {
			try {
				Simulation sim = load(name);
				assertNotNull(sim.getNetwork(), name);
				assertTrue(sim.getClasses().length > 0, name);
				assertTrue(sim.getNetSystem() != null, name);
			} catch (Exception | Error e) {
				failures.append(name).append(": ").append(e).append('\n');
			}
		}
		assertEquals("", failures.toString());
	}

	@Test
	void loadedModelExposesStructure() throws Exception {
		Simulation sim = load("closed_2class_2stat_switch");
		assertEquals(2, sim.getClasses().length);
		assertNotNull(sim.getNetwork().getNode("Queue 1"));
		assertNotNull(sim.getXmlSimModelDefPath());
		assertFalse(sim.hasFinished());
		assertSame(sim.getSimSystem(), sim.getNetSystem().getSimSystem());
		assertNotNull(sim.getSimParameters());
		assertNotNull(sim.getEngine());
	}

	@Test
	void regionsAreLoaded() throws Exception {
		Simulation sim = load("region");
		assertTrue(sim.getAllRegions().length > 0);
	}

	@Test
	void missingFileIsAnIoError() {
		assertThrows(IOException.class, () -> new SimLoader(dir.resolve("nope.xml").toString()));
	}

	@Test
	void malformedAndInvalidDocumentsAreRejected() throws Exception {
		Path bad = dir.resolve("bad.xml");
		Files.write(bad, "<not-xml".getBytes(StandardCharsets.UTF_8));
		// oddity: SAX errors are only printed, the loader is created but holds no simulation
		assertNull(new SimLoader(bad.toString()).getSim());
		Path invalid = dir.resolve("invalid.xml");
		Files.write(invalid, "<?xml version=\"1.0\"?><archive name=\"x\"><sim/></archive>".getBytes(StandardCharsets.UTF_8));
		LoadException e = assertThrows(LoadException.class, () -> new SimLoader(invalid.toString()));
		assertEquals("Problems loading", e.getMessage());
	}

	@Test
	void classLookupFailuresAreLoadExceptions() throws Exception {
		QueueNet.NetFixtures.writeVariant(dir, "closed_1class_2stat", "bogusclass", "className=\"Queue\"", "className=\"NoSuchQueue\"");
		LoadException e = assertThrows(LoadException.class, () -> new SimLoader(dir.resolve("bogusclass.xml").toString()));
		assertTrue(e.getMessage().contains("Not found"), e.getMessage());
	}

	@Test
	void xmlParserFindsOnlyDirectChildren() throws Exception {
		javax.xml.parsers.DocumentBuilder b = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
		org.w3c.dom.Document d = b.parse(new java.io.ByteArrayInputStream("<r><a/><x><a/></x><a/></r>".getBytes(StandardCharsets.UTF_8)));
		org.w3c.dom.NodeList l = SimLoader.XMLParser.getElementsByTagName(d.getDocumentElement(), "a");
		assertEquals(2, l.getLength());
		assertThrows(IndexOutOfBoundsException.class, () -> new SimLoader.XMLParser.JmtNodeList().item(0));
	}
}
