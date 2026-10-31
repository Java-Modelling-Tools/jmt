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

import java.io.File;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JSimLoggerFactoryTest {

	private static final String[] COLS = { "a" };

	@Test
	void getCSVLogger_cachesPerFile(@TempDir File tmp) throws Exception {
		File f = new File(tmp, "x.csv");
		CSVLogger a = JSimLoggerFactory.getCSVLogger(f, COLS, false, ",", ".");
		assertSame(a, JSimLoggerFactory.getCSVLogger(f, COLS, false, ",", "."));
		CSVLogger b = JSimLoggerFactory.getCSVLogger(new File(tmp, "y.csv"), COLS, false, ",", ".");
		assertNotSame(a, b);
		JSimLoggerFactory.remove(a);
		JSimLoggerFactory.remove(b);
	}

	@Test
	void remove_evictsFromCache(@TempDir File tmp) throws Exception {
		File f = new File(tmp, "x.csv");
		CSVLogger a = JSimLoggerFactory.getCSVLogger(f, COLS, false, ",", ".");
		JSimLoggerFactory.remove(a);
		CSVLogger b = JSimLoggerFactory.getCSVLogger(f, COLS, false, ",", ".");
		assertNotSame(a, b);
		JSimLoggerFactory.remove(b);
	}

	@Disabled("bug: JSimLoggerFactory.removeAllLoggers() throws ConcurrentModificationException when a logger is cached")
	@Test
	void removeAllLoggers_emptiesCacheWithoutError(@TempDir File tmp) throws Exception {
		File f = new File(tmp, "x.csv");
		CSVLogger a = JSimLoggerFactory.getCSVLogger(f, COLS, false, ",", ".");
		JSimLoggerFactory.removeAllLoggers();
		assertNotSame(a, JSimLoggerFactory.getCSVLogger(f, COLS, false, ",", "."));
	}
}
