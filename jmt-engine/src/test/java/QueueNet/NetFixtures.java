/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */


package QueueNet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import simEngine.SimLoader;
import simEngine.Simulation;

/**
 * Runs a complete, real simulation from a small sim-model resource (models/*.xml), single threaded and
 * deterministic, so that whole networks (QueueNetwork, NetNode, node sections, job info lists) can be
 * exercised without mocks. Builds on the same package-level access as {@link StrategyFixtures}.
 */
public final class NetFixtures {

	/** Simulated-time cap that aborts all measures, so a run always terminates. */
	private static final double MAX_TIME = 3000.0;

	private NetFixtures() {
	}

	/** A finished (or event-bounded) run. */
	public static final class Run {
		public final Simulation sim;
		public final QueueNetwork network;
		public final File output;

		Run(Simulation sim, File output) {
			this.sim = sim;
			this.network = sim.getNetwork();
			this.output = output;
		}

		public NetNode node(String name) {
			return network.getNode(name);
		}
	}

	/** Copies the named model resource into dir and returns the copy. */
	public static File model(Path dir, String name) throws IOException {
		Path target = dir.resolve(name + ".xml");
		try (InputStream in = NetFixtures.class.getResourceAsStream("/models/" + name + ".xml")) {
			if (in == null) {
				throw new IOException("missing model " + name);
			}
			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
		}
		return target.toFile();
	}

	/** Writes a variant of a model resource: each (from, to) pair replaces the first occurrence of from. */
	public static void writeVariant(Path dir, String base, String variant, String... fromTo) throws IOException {
		String xml;
		try (InputStream in = NetFixtures.class.getResourceAsStream("/models/" + base + ".xml")) {
			xml = new String(in.readAllBytes(), "ISO-8859-1");
		}
		for (int i = 0; i < fromTo.length; i += 2) {
			if (!xml.contains(fromTo[i])) {
				throw new IOException("variant pattern not found: " + fromTo[i]);
			}
			xml = xml.replaceFirst(java.util.regex.Pattern.quote(fromTo[i]), java.util.regex.Matcher.quoteReplacement(fromTo[i + 1]));
		}
		Files.write(dir.resolve(variant + ".xml"), xml.getBytes("ISO-8859-1"));
	}

	/** Runs a model already present in dir (see writeVariant). */
	public static Run runInDir(Path dir, String name, long seed) throws Exception {
		File f = dir.resolve(name + ".xml").toFile();
		SimLoader loader = new SimLoader(f.getAbsolutePath());
		Simulation sim = loader.getSim();
		sim.setXmlSimModelDefPath(f.getAbsolutePath());
		sim.setRandomEngineSeed(seed);
		sim.initialize();
		sim.getNetSystem().setMaxSimulatedTime(MAX_TIME);
		sim.run();
		return new Run(sim, sim.getOutputFile());
	}

	/** Loads, initializes and runs the model; maxEvents bounds the run (negative = unbounded). */
	public static Run run(Path dir, String name, long seed, int maxEvents) throws Exception {
		File f = model(dir, name);
		SimLoader loader = new SimLoader(f.getAbsolutePath());
		Simulation sim = loader.getSim();
		sim.setXmlSimModelDefPath(f.getAbsolutePath());
		sim.setRandomEngineSeed(seed);
		sim.initialize();
		if (maxEvents > 0) {
			sim.getNetSystem().setMaxProcessedEvents(maxEvents);
			sim.getNetSystem().setMaxSimulatedTime(MAX_TIME);
		}
		sim.run();
		return new Run(sim, sim.getOutputFile());
	}

	/**
	 * Hand-wired mini network for single-section unit tests: stations are built from explicit sections,
	 * jobs are injected from a feeder node, and the event loop is stepped directly (no Simulation, no
	 * measures, single thread, deterministic).
	 */
	public static final class Mini {
		public final NetSystem system;
		public final QueueNetwork network;
		public final JobClass[] classes;
		public final NetNode feeder;
		private final GlobalJobInfoList global;
		private final java.util.List<NetNode> stations = new java.util.ArrayList<NetNode>();
		private boolean ready;

		public Mini(int... priorities) throws jmt.common.exception.NetException {
			this(priorities, openTypes(priorities.length));
		}

		private static int[] openTypes(int n) {
			int[] t = new int[n];
			java.util.Arrays.fill(t, JobClass.OPEN_CLASS);
			return t;
		}

		/** types[i] is JobClass.OPEN_CLASS (0) or JobClass.CLOSED_CLASS. */
		public Mini(int[] priorities, int[] types) throws jmt.common.exception.NetException {
			system = new NetSystem();
			system.initialize();
			system.getEngine().setNewSeed(12345L);
			network = new QueueNetwork("mini");
			network.setNetSystem(system);
			classes = new JobClass[priorities.length];
			for (int i = 0; i < classes.length; i++) {
				classes[i] = new JobClass("c" + i, priorities[i], types[i], "feeder");
				network.addJobClass(classes[i]);
			}
			// registering any measure makes the network create its global job info list
			network.addMeasure(new dataAnalysis.Measure("m", 0.05, 0.05, 10000, false, null));
			global = network.getJobInfoList();
			feeder = station("feeder", null, null, new NodeSections.Router(new NetStrategies.RoutingStrategy[classes.length]));
		}

		public NetNode station(String name, NodeSection in, NodeSection svc, NodeSection out) throws jmt.common.exception.NetException {
			NetNode n = new NetNode(name);
			if (in != null) {
				n.addSection(in);
			}
			if (svc != null) {
				n.addSection(svc);
			}
			if (out != null) {
				n.addSection(out);
			}
			network.addNode(n);
			n.setSimSystem(system.getSimSystem());
			system.getSimSystem().add(n);
			stations.add(n);
			return n;
		}

		public void connect(NetNode from, NetNode to) {
			from.connect(to);
		}

		/** Sections are initialized after wiring; called automatically by run. */
		public void ready() throws jmt.common.exception.NetException {
			if (!ready) {
				ready = true;
				for (NetNode n : stations) {
					n.initializeSections();
				}
			}
		}

		public Job newJob(int classIndex) {
			Job j = new Job(classes[classIndex], global);
			j.initialize(system);
			return j;
		}

		/** Schedules the arrival of a new job of the class at the node input after delay. */
		public Job inject(NetNode target, int classIndex, double delay) {
			Job j = newJob(classIndex);
			try {
				ready();
			} catch (jmt.common.exception.NetException e) {
				throw new IllegalStateException(e);
			}
			feeder.send(NetEvent.EVENT_JOB, j, delay, NodeSection.OUTPUT, NodeSection.INPUT, target);
			return j;
		}

		/** Sends the start event to every section and steps the event loop until it is empty (or maxTicks). */
		public int run(int maxTicks) throws Exception {
			ready();
			for (NetNode n : stations) {
				n.send(NetEvent.EVENT_START, null, 0.0, NodeSection.NO_ADDRESS, NodeSection.INPUT, n);
				n.send(NetEvent.EVENT_START, null, 0.0, NodeSection.NO_ADDRESS, NodeSection.SERVICE, n);
				n.send(NetEvent.EVENT_START, null, 0.0, NodeSection.NO_ADDRESS, NodeSection.OUTPUT, n);
				n.simGetNext(simEngine.SimSystem.SIM_ANY);
			}
			int ticks = 0;
			while (ticks < maxTicks && system.getSimSystem().runTick()) {
				ticks++;
			}
			return ticks;
		}

		/** Registers a recording measure on the node's job info list (node-level analyzer). */
		public RecordingMeasure measure(NetNode node, int measureName, int classIndex) throws jmt.common.exception.NetException {
			ready();
			RecordingMeasure meas = new RecordingMeasure();
			network.addMeasure(meas);
			node.analyze(measureName, classes[classIndex], meas);
			return meas;
		}

		/** Same, for measures that are rates (the engine casts them to InverseMeasure). */
		public RecordingInverseMeasure rate(NetNode node, int measureName, int classIndex) throws jmt.common.exception.NetException {
			ready();
			RecordingInverseMeasure meas = new RecordingInverseMeasure();
			network.addMeasure(meas);
			node.analyze(measureName, classes[classIndex], meas);
			return meas;
		}

		/** Section-level analyzer (utilization, effective utilization, ...). */
		public RecordingMeasure sectionMeasure(NodeSection section, int measureName, int classIndex) throws jmt.common.exception.NetException {
			ready();
			RecordingMeasure meas = new RecordingMeasure();
			network.addMeasure(meas);
			section.analyze(measureName, classes[classIndex], meas);
			return meas;
		}

		/** Tells every node to stop (as NetSystem.stop does) and drains the resulting events. */
		public void stop() throws Exception {
			for (NetNode n : stations) {
				n.send(NetEvent.EVENT_STOP, null, 0.0, NodeSection.NO_ADDRESS, NodeSection.NO_ADDRESS, n);
			}
			int guard = 0;
			while (guard++ < 10000 && system.getSimSystem().runTick()) {
				// drain
			}
		}

		/** Runs through NetSystem.start (NetController loop) instead of the hand-stepped loop. */
		public void startSystem() throws Exception {
			ready();
			system.addNetwork(network);
			system.start();
		}

		public double clock() {
			return system.getSimSystem().clock();
		}
	}
}
