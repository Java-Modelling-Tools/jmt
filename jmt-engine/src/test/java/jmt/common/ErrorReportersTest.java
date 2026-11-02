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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ErrorReportersTest {

	@AfterEach
	void restore() {
		ErrorReporters.set(null);
	}

	@Test
	void get_defaultsToLoggingReporter() {
		assertTrue(ErrorReporters.get() instanceof LoggingErrorReporter);
	}

	@Test
	void set_replacesReporter_andNullRestoresDefault() {
		RecordingErrorReporter r = new RecordingErrorReporter();
		ErrorReporters.set(r);
		assertSame(r, ErrorReporters.get());
		ErrorReporters.set(null);
		assertTrue(ErrorReporters.get() instanceof LoggingErrorReporter);
	}

	@Test
	void loggingReporter_doesNotThrowInHeadlessJvm() {
		ErrorReporter r = new LoggingErrorReporter();
		assertDoesNotThrow(() -> r.warning("t", null));
		assertDoesNotThrow(() -> r.error("t", null));
	}
}
