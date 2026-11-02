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

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LoggerStateManagerTest {

	@AfterEach
	void reset() {
		LoggerStateManager.clearAllowedEvents();
		LoggerStateManager.getInstance().setLogCalenderOn(false);
	}

	@Test
	void instance_isSingleton() {
		assertSame(LoggerStateManager.getInstance(), LoggerStateManager.getInstance());
	}

	@Test
	void calenderFlag_canBeToggled() {
		LoggerStateManager m = LoggerStateManager.getInstance();
		assertFalse(m.isLogCalenderOn());
		m.setLogCalenderOn(true);
		assertTrue(m.isLogCalenderOn());
	}

	@Test
	void addAllowedEvent_ignoresNullAndDuplicates() {
		LoggerStateManager.addAllowedEvent(null);
		LoggerStateManager.addAllowedEvent("a");
		LoggerStateManager.addAllowedEvent("a");
		LoggerStateManager.addAllowedEvent("b");
		assertEquals(Arrays.asList("a", "b"), Arrays.asList(LoggerStateManager.getAllowedEvents()));
	}

	@Test
	void deleteAllowedEvent_removesOnlyThatEvent() {
		LoggerStateManager.addAllowedEvent("a");
		LoggerStateManager.addAllowedEvent("b");
		LoggerStateManager.addAllowedEvent("c");
		LoggerStateManager.deleteAllowedEvent("b");
		LoggerStateManager.deleteAllowedEvent("zzz");
		LoggerStateManager.deleteAllowedEvent(null);
		assertEquals(Arrays.asList("a", "c"), Arrays.asList(LoggerStateManager.getAllowedEvents()));
	}

	@Test
	void clearAllowedEvents_emptiesList() {
		LoggerStateManager.addAllowedEvent("a");
		LoggerStateManager.clearAllowedEvents();
		assertEquals(0, LoggerStateManager.getAllowedEvents().length);
	}
}
