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

import java.util.ArrayList;
import java.util.List;

import jmt.common.exception.NetException;
import NetStrategies.ServiceStrategy;
import NetStrategies.QueueGetStrategies.FCFSstrategy;
import NodeSections.Queue;
import NodeSections.Server;

/**
 * Small fixture that builds a real, minimal queue network (one station with a
 * Server service section) so that strategies can be exercised without mocks.
 * It lives in package QueueNet because job class registration is package-private.
 */
public final class StrategyFixtures {

	/** Service strategy returning scripted values, in order; the last one is repeated. */
	public static final class ScriptedService extends ServiceStrategy {
		private final double[] values;
		private int next;

		public ScriptedService(double... values) {
			this.values = values;
		}

		public double wait(NodeSection nodeSection, JobClass jobClass) {
			double v = values[Math.min(next, values.length - 1)];
			next++;
			return v;
		}

		public double expect(NodeSection nodeSection, JobClass jobClass) {
			return values[0];
		}
	}

	public final QueueNetwork network;
	public final NetSystem system;
	public final NetNode node;
	public final Server server;
	public final Queue input;
	public final JobClass[] classes;
	private final GlobalJobInfoList global;

	/** Builds a station whose service strategies are given per class (one entry per class). */
	public StrategyFixtures(JobClass[] classes, ServiceStrategy[] services) throws NetException {
		this.classes = classes;
		system = new NetSystem();
		system.initialize();
		system.getEngine().setNewSeed(12345L);
		network = new QueueNetwork("test");
		network.setNetSystem(system);
		for (JobClass c : classes) {
			network.addJobClass(c);
		}
		global = new GlobalJobInfoList(classes.length);
		node = new NetNode("station");
		input = new Queue(-1, drops(), new FCFSstrategy(), null);
		server = newServer(services);
		register(node, input, server);
	}

	private String[] drops() {
		String[] drops = new String[classes.length];
		java.util.Arrays.fill(drops, Queue.FINITE_DROP);
		return drops;
	}

	private Server newServer(ServiceStrategy[] services) throws NetException {
		Integer[] visits = new Integer[classes.length];
		java.util.Arrays.fill(visits, 1);
		return new Server(1, visits, services);
	}

	private void register(NetNode n, Queue in, Server out) throws NetException {
		if (in != null) {
			n.addSection(in);
		}
		if (out != null) {
			n.addSection(out);
		}
		network.addNode(n);
		system.getSimSystem().add(n);
		n.initializeSections();
	}

	/** Adds a Queue+Server station and connects it as an output of the main station. */
	public NetNode addOutput(String name) throws NetException {
		NetNode n = new NetNode(name);
		ServiceStrategy[] services = new ServiceStrategy[classes.length];
		java.util.Arrays.fill(services, new ScriptedService(1));
		register(n, new Queue(-1, drops(), new FCFSstrategy(), null), newServer(services));
		node.connect(n);
		return n;
	}

	/** Adds a sink (no service/output section) connected as an output of the main station. */
	public NetNode addSink(String name) throws NetException {
		NetNode n = new NetNode(name);
		register(n, null, null);
		node.connect(n);
		return n;
	}

	/** Makes the station look loaded with n resident jobs in its input section. */
	public void load(NetNode n, int count) throws NetException {
		load(n, NodeSection.INPUT, count);
	}

	/** Same as load, but into the given section (INPUT or SERVICE). */
	public void load(NetNode n, int section, int count) throws NetException {
		for (int i = 0; i < count; i++) {
			n.getSection(section).getJobInfoList().addLast(new JobInfo(job(0)));
		}
	}

	public static JobClass[] classes(int... priorities) {
		JobClass[] cs = new JobClass[priorities.length];
		for (int i = 0; i < cs.length; i++) {
			cs[i] = new JobClass("c" + i, priorities[i], JobClass.OPEN_CLASS, null);
		}
		return cs;
	}

	public Job job(int classIndex) {
		Job j = new Job(classes[classIndex], global);
		j.initialize(system);
		return j;
	}

	public JobInfoList queue() {
		JobInfoList q = new LinkedJobInfoList(network.getJobClasses());
		q.setNetSystem(system);
		return q;
	}

	/** Ids of the jobs in the queue, front to back. */
	public static List<Integer> ids(JobInfoList queue) {
		List<Integer> r = new ArrayList<Integer>();
		for (JobInfo ji : queue.getInternalJobInfoList()) {
			r.add(ji.getJob().getId());
		}
		return r;
	}
}
