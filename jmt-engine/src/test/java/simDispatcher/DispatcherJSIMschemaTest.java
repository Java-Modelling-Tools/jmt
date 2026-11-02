package simDispatcher;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import QueueNet.NetFixtures;

class DispatcherJSIMschemaTest {

	@TempDir
	Path dir;

	@Test
	void freshDispatcherIsIdle() {
		DispatcherJSIMschema d = new DispatcherJSIMschema(dir.resolve("m.xml").toString());
		assertFalse(d.isStarted());
		assertFalse(d.isFinished());
		assertFalse(d.isPaused());
		assertEquals(0.0, d.checkSimProgress());
		assertNull(d.getTempMeasures());
		assertNull(d.getSimulation());
		assertFalse(d.killSimulation());
		assertFalse(d.abortAllMeasures());
		d.refreshTempMeasures(); // not started: no-op
		assertNull(d.getTempMeasures());
		d.abortMeasureAtRefresh(0);
		d.setSimulationSeed(5);
		d.automaticSimulationSeed();
		d.setSimulationMaxDuration(100);
		d.setTerminalSimulation(true);
		d.setParametricStep(2);
	}

	@Test
	void forceFinishedUnblocksPollers() {
		DispatcherJSIMschema d = new DispatcherJSIMschema(new File(dir.toFile(), "m.xml"));
		d.forceFinished();
		assertTrue(d.isFinished());
		assertEquals(0.0, d.checkSimProgress()); // never started: progress stays 0
	}

	@Test
	void hardcodedDeveloperSmokeRoutinesFailGracefullyWithoutTheirFiles() {
		// they point at D:\\JMTtest paths that do not exist here; each reports an error and returns
		DispatcherJSIMschema.testSimulationTime();
		DispatcherJSIMschema.testSolution();
	}

	/** Puts the dispatcher in the "started" state over a hand-wired network, without running a simulation. */
	private static DispatcherJSIMschema startedOver(QueueNet.QueueNetwork net) throws Exception {
		DispatcherJSIMschema d = new DispatcherJSIMschema("unused.xml");
		java.lang.reflect.Field f = DispatcherJSIMschema.class.getDeclaredField("net");
		f.setAccessible(true);
		f.set(d, net);
		f = DispatcherJSIMschema.class.getDeclaredField("simStarted");
		f.setAccessible(true);
		f.setBoolean(d, true);
		return d;
	}

	@Test
	void pauseRefreshAndAbortOverAStartedNetwork() throws Exception {
		DispatcherJSIMschema d = startedOver(new NetFixtures.Mini(0).network);
		assertTrue(d.isStarted());
		assertTrue(d.pauseSim());
		assertTrue(d.isPaused());
		assertTrue(d.restartSim());
		assertFalse(d.isPaused());
		d.refreshTempMeasures();
		assertEquals(1, d.getTempMeasures().length);
		d.refreshTempMeasures(); // second time only refreshes the values
		d.printTempMeasures();
		d.abortMeasureAtRefresh(0);
		d.refreshTempMeasures();
		assertTrue(d.getTempMeasures()[0].isFinished());
		d.abortMeasure(0); // already aborted: must simply not fail
		assertTrue(d.abortAllMeasures());
		assertTrue(d.killSimulation());
		assertTrue(d.checkSimProgress() >= 0.0);
	}

	@Test
	void missingModelIsNotSolved() throws Exception {
		DispatcherJSIMschema d = new DispatcherJSIMschema(dir.resolve("missing.xml").toString());
		assertFalse(d.solveModel());
		assertFalse(d.solveHandlingExceptions());
		assertFalse(d.isStarted());
	}

	@Test
	void unreadableModelIsHandled() throws Exception {
		File bad = dir.resolve("bad.xml").toFile();
		assertTrue(bad.createNewFile());
		DispatcherJSIMschema d = new DispatcherJSIMschema(bad);
		assertFalse(d.solveHandlingExceptions());
	}

	@Test
	@Tag("simulation")
	void solvesAModelUntilTheTimeLimitAborts() throws Exception {
		File model = NetFixtures.model(dir, "closed_1class_2stat");
		DispatcherJSIMschema d = new DispatcherJSIMschema(model);
		d.setSimulationSeed(3);
		d.setSimulationMaxDuration(300);
		assertTrue(d.solveModel());
		assertTrue(d.isStarted());
		assertTrue(d.isFinished());
		assertEquals(1.0, d.checkSimProgress());
		assertNotNull(d.getSimulation());
		d.refreshTempMeasures();
		assertNotNull(d.getOutputFile());
		d.abortMeasureAtRefresh(0);
	}
}
