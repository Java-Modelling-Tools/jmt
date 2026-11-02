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

package NodeSections;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import NetStrategies.ServiceStrategy;
import QueueNet.StrategyFixtures;
import jmt.common.ErrorReporters;
import jmt.common.RecordingErrorReporter;
import jmt.common.exception.NetException;

class ServerTest {
	private final RecordingErrorReporter reporter = new RecordingErrorReporter();

	@AfterEach
	void restore() {
		ErrorReporters.set(null);
	}

	private static ServiceStrategy[] services() {
		return new ServiceStrategy[] { new StrategyFixtures.ScriptedService(1) };
	}

	@Test
	void requiringMoreServersThanExistReportsParameterError() {
		ErrorReporters.set(reporter);
		NetException e = assertThrows(NetException.class, () -> new Server(2, new Integer[] { 1 }, services(),
				new Integer[] { 3 }, new String[] { "t" }, new Integer[] { 2 }, new Object[] { new Boolean[] { true } }, "ALIS"));
		assertEquals("Remember: Servers required can't be more than the total number of servers", e.getMessage());
		assertEquals(1, reporter.calls.size());
		assertEquals("error|Parameter Error|Servers Required for a class are more than the total number of servers", reporter.calls.get(0));
	}

	@Test
	void validServerTypeConfigurationIsAccepted() throws Exception {
		Server s = new Server(2, new Integer[] { 1 }, services(),
				new Integer[] { 2 }, new String[] { "t" }, new Integer[] { 2 }, new Object[] { new Boolean[] { true } }, "ALIS");
		assertEquals(1, s.getServerTypes().size());
		assertTrue(reporter.calls.isEmpty());
	}
}
