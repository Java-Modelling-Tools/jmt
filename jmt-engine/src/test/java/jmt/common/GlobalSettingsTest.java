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
package jmt.common;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GlobalSettingsTest {

	@Test
	void getSetting_nullOrEmptyNameReturnsNull() {
		assertNull(GlobalSettings.getSetting(null));
		assertNull(GlobalSettings.getSetting(""));
	}

	@Test
	void getSetting_systemPropertyWins() {
		System.setProperty("jmt.test.setting", "sys");
		try {
			assertEquals("sys", GlobalSettings.getSetting("jmt.test.setting"));
		} finally {
			System.clearProperty("jmt.test.setting");
		}
	}

	@Test
	void getSetting_unknownNameReturnsNull() {
		assertNull(GlobalSettings.getSetting("jmt.no.such.setting"));
	}

	@Test
	void getSetting_versionComesFromBundledConfig() {
		assertNotNull(GlobalSettings.getSetting(GlobalSettings.VERSION));
	}

	@Test
	void reload_keepsSettingsAvailable() {
		GlobalSettings.reload();
		assertNotNull(GlobalSettings.getSetting(GlobalSettings.VERSION));
	}
}
