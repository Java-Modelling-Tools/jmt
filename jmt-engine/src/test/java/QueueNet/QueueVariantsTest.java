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

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jmt.common.ErrorReporters;

/** Overloaded M/M/1 variants (arrival rate 6, service rate 2) with different queue disciplines. */
@Tag("simulation")
class QueueVariantsTest {
	private static final String TAIL = "QueuePutStrategies.TailStrategy\" name=\"TailStrategy\"";
	@TempDir
	Path dir;

	@AfterEach
	void restore() {
		ErrorReporters.set(null);
	}

	private NetFixtures.Run runVariant(String name, String... fromTo) throws Exception {
		String[] all = new String[fromTo.length + 2];
		all[0] = "<value>1.0</value>";
		all[1] = "<value>6.0</value>";
		System.arraycopy(fromTo, 0, all, 2, fromTo.length);
		NetFixtures.writeVariant(dir, "fcfs-01", name, all);
		NetFixtures.Run r = NetFixtures.runInDir(dir, name, 777L);
		assertTrue(r.sim.hasFinished());
		assertNotNull(r.output);
		return r;
	}

	@ParameterizedTest
	@ValueSource(strings = { "drop", "BAS blocking", "waiting queue" })
	void finiteCapacityQueueRunsWithEachFullQueuePolicy(String policy) throws Exception {
		NetFixtures.Run r = runVariant("cap-" + policy.hashCode(), "<value>-1</value>", "<value>3</value>", "<value>drop</value>",
				"<value>" + policy + "</value>");
		NetNode q = r.node("Queue 1");
		assertTrue(q.getJobInfoList().size() <= 3 || policy.equals("waiting queue") || policy.equals("BAS blocking"));
	}

	@Test
	void lcfsGetStrategyRuns() throws Exception {
		runVariant("lcfs", "QueueGetStrategies.FCFSstrategy\" name=\"FCFSstrategy\"", "QueueGetStrategies.LCFSstrategy\" name=\"LCFSstrategy\"");
	}

	@ParameterizedTest
	@ValueSource(strings = { "Head", "SJF", "LJF", "Rand" })
	void putStrategyRuns(String strategy) throws Exception {
		runVariant("put" + strategy, "QueuePutStrategies.TailStrategy\" name=\"TailStrategy\"",
				"QueuePutStrategies." + strategy + "Strategy\" name=\"" + strategy + "Strategy\"");
	}
}
