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

package NetStrategies.ServiceStrategies;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jmt.common.exception.NetException;
import NetStrategies.ServiceStrategy;
import QueueNet.NodeSection;
import QueueNet.StrategyFixtures;
import QueueNet.StrategyFixtures.ScriptedService;
import random.DeterministicDistr;
import random.DeterministicDistrPar;
import random.Distribution;
import random.Exponential;
import random.ExponentialPar;
import random.Parameter;

class ServiceStrategiesTest {

	private StrategyFixtures f;

	@BeforeEach
	void setUp() throws Exception {
		f = new StrategyFixtures(StrategyFixtures.classes(0), new ServiceStrategy[] { new ScriptedService(1) });
	}

	@Test
	void zeroServiceTimeIsAlwaysZero() throws Exception {
		ZeroServiceTimeStrategy s = new ZeroServiceTimeStrategy();
		assertEquals(0.0, s.wait(f.server, f.classes[0]), 0.0);
		assertEquals(0.0, s.expect(f.server, f.classes[0]), 0.0);
		assertTrue(s.check());
	}

	@Test
	void disabledServiceTimeIsZeroAndWarnsOnlyOnce() throws Exception {
		// terminal mode warns on stdout, avoiding the delayed GUI-warning thread
		f.network.setTerminalSimulation(true);
		DisabledServiceTimeStrategy s = new DisabledServiceTimeStrategy();
		assertEquals(0.0, s.wait(f.server, f.classes[0]), 0.0);
		assertEquals(0.0, s.wait(f.server, f.classes[0]), 0.0);
		assertEquals(0.0, s.expect(f.server, f.classes[0]), 0.0);
	}

	@Test
	void serviceTimeStrategyDrawsFromItsDistribution() throws Exception {
		ServiceTimeStrategy s = new ServiceTimeStrategy(new DeterministicDistr(), new DeterministicDistrPar(2.5));
		assertEquals(2.5, s.wait(f.server, f.classes[0]), 0.0);
		assertEquals(2.5, s.expect(f.server, f.classes[0]), 0.0);
		assertEquals(2.5, s.getRand(), 0.0);
		assertNotNull(s.getDistribution());
		assertNotNull(s.getParameter());
	}

	@Test
	void serviceTimeStrategySettersReplaceDistributionAndParameter() throws Exception {
		ServiceTimeStrategy s = new ServiceTimeStrategy();
		Distribution d = new Exponential();
		Parameter p = new ExponentialPar(0.5);
		assertTrue(s.setDistribution(d));
		assertTrue(s.setParameter(p));
		assertSame(d, s.getDistribution());
		assertSame(p, s.getParameter());
		assertEquals(2.0, s.expect(f.server, f.classes[0]), 1e-12);
	}

	@Test
	void serviceTimeStrategyWrapsInvalidParameters() throws Exception {
		ExponentialPar p = new ExponentialPar(1.0);
		Exponential exp = new Exponential();
		exp.setRandomEngine(f.system.getEngine());
		ServiceTimeStrategy s = new ServiceTimeStrategy(exp, p);
		// an unset (invalid) parameter makes the distribution refuse to sample
		ServiceTimeStrategy bad = new ServiceTimeStrategy(exp, new ExponentialPar());
		NetException e1 = assertThrows(NetException.class, () -> bad.wait(f.server, f.classes[0]));
		assertTrue(e1.getMessage().contains("ServiceTimeStrategy"));
		assertThrows(NetException.class, () -> bad.expect(f.server, f.classes[0]));
		assertEquals(0.0, bad.getRand(), 0.0);
		assertTrue(s.wait(f.server, f.classes[0]) >= 0.0);
	}

	private LDParameter ld(int from, String fn) throws Exception {
		return new LDParameter(from, new DeterministicDistr(), new DeterministicDistrPar(1.0), fn);
	}

	@Test
	void ldParameterExposesItsFieldsAndOrdersByThreshold() throws Exception {
		LDParameter a = ld(0, "N+1");
		LDParameter b = ld(3, "10");
		assertEquals(0, a.getMinJobs());
		assertEquals("n+1", a.getFunction());
		assertNotNull(a.getParser());
		assertNotNull(a.getDistribution());
		assertNotNull(a.getDistrParameter());
		assertEquals(-3, a.compareTo(b));
		assertEquals(0, b.compareTo(Integer.valueOf(3)));
		assertThrows(ClassCastException.class, () -> a.compareTo("x"));
		LDParameter plain = new LDParameter(1, new DeterministicDistr(), new DeterministicDistrPar(1.0));
		assertNull(plain.getFunction());
		assertNull(plain.getParser());
		assertDoesNotThrow(() -> a.setRandomEngine(f.system.getEngine()));
	}

	@Test
	void loadDependentStrategyEvaluatesFunctionAtCurrentLoad() throws Exception {
		LoadDependentStrategy s = new LoadDependentStrategy(new LDParameter[] { ld(3, "100"), ld(0, "n+1") });
		assertEquals(2, s.getParameters().length);
		assertEquals(0, s.getParameters()[0].getMinJobs());
		assertEquals(0.0, s.expect(f.server, f.classes[0]), 0.0);
		assertEquals(1.0, s.wait(f.server, f.classes[0]), 1e-12);
		f.load(f.node, NodeSection.INPUT, 1);
		f.load(f.node, NodeSection.SERVICE, 1);
		assertEquals(3.0, s.wait(f.server, f.classes[0]), 1e-12);
		// cached evaluation gives the same value
		assertEquals(3.0, s.wait(f.server, f.classes[0]), 1e-12);
		f.load(f.node, NodeSection.INPUT, 2);
		assertEquals(100.0, s.wait(f.server, f.classes[0]), 1e-12);
	}

	@Test
	void loadDependentStrategyFallsBackToFirstRangeBelowAllThresholds() throws Exception {
		LoadDependentStrategy s = new LoadDependentStrategy(new LDParameter[] { ld(2, "7") });
		assertEquals(7.0, s.wait(f.server, f.classes[0]), 1e-12);
	}

	@Test
	void loadDependentStrategyWrapsBadFunctionsInNetException() throws Exception {
		LoadDependentStrategy s = new LoadDependentStrategy(new LDParameter[] { ld(0, "n+") });
		NetException e = assertThrows(NetException.class, () -> s.wait(f.server, f.classes[0]));
		assertTrue(e.getMessage().startsWith("Error in LoadDependentStrategy"));
	}
}
