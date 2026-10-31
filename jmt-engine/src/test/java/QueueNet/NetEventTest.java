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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class NetEventTest {
	@Test
	void everyNamedEventMapsToItsConstantName() throws Exception {
		int checked = 0;
		for (Field f : NetEvent.class.getFields()) {
			if (Modifier.isStatic(f.getModifiers()) && f.getName().startsWith("EVENT_") && !f.getName().equals("EVENT_MASK")
					&& f.getType() == int.class) {
				String name = NetEvent.getEventName(f.getInt(null));
				if (!name.equals("UNKNOWN_EVENT")) {
					assertEquals(f.getName(), name);
					checked++;
				}
			}
		}
		assertTrue(checked >= 28, "checked " + checked);
	}

	@Test
	void unnamedEventsReportUnknown() {
		assertEquals("UNKNOWN_EVENT", NetEvent.getEventName(0x7777));
	}
}
