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
package log;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LoggerParametersTest {

	private static LoggerParameters custom(boolean flag) {
		Boolean b = Boolean.valueOf(flag);
		return new LoggerParameters("f.csv", "/p", b, b, b, b, b, b, b);
	}

	@Test
	void defaultConstructor_isEnabledGlobalLog() {
		LoggerParameters p = new LoggerParameters();
		assertTrue(p.isEnabled());
		assertTrue(p.isGlobal());
		assertEquals(LoggerParameters.GLOBALLOGNAME, p.name);
		assertEquals("1|0111000|global.csv|", p.toString());
	}

	@Test
	void isGlobal_ignoresCase() {
		assertTrue(new LoggerParameters("GLOBAL.CSV", "/p", true, true, true, true, true, true, true).isGlobal());
		assertFalse(custom(true).isGlobal());
	}

	@Test
	void allFlagsFalse_disablesOnEnable() {
		LoggerParameters p = custom(false);
		assertFalse(p.isEnabled());
		assertFalse(p.enable());
	}

	@Test
	void disable_thenEnable_restores() {
		LoggerParameters p = custom(true);
		p.disable();
		assertFalse(p.isEnabled());
		assertTrue(p.enable());
	}

	@Test
	void nullFlag_disablesInsteadOfThrowing() {
		LoggerParameters p = new LoggerParameters("f", "/p", null, true, true, true, true, true, true);
		assertFalse(p.isEnabled());
	}

	@Test
	void toString_encodesFlagsAndName() {
		assertEquals("1|1111111|f.csv|", custom(true).toString());
		LoggerParameters p = custom(true);
		p.disable();
		assertEquals("0|1111111|f.csv|", p.toString());
	}

	@Test
	void clone_copiesFieldsAndEnabledState() {
		LoggerParameters p = custom(true);
		p.disable();
		LoggerParameters c = (LoggerParameters) p.clone();
		assertNotSame(p, c);
		assertEquals(p.toString(), c.toString());
		assertEquals("/p", c.path);
		assertFalse(c.isEnabled());
	}
}
