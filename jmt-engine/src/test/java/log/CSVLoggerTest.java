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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CSVLoggerTest {

	private static final String[] COLS = { "a", "b", "c" };

	private static String read(File f) throws Exception {
		return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).replace("\r\n", "\n");
	}

	private static Map<String, Object> map(Object... kv) {
		Map<String, Object> m = new HashMap<String, Object>();
		for (int i = 0; i < kv.length; i += 2) {
			m.put((String) kv[i], kv[i + 1]);
		}
		return m;
	}

	@Test
	void log_writesHeaderThenRowsWithFormattedNumbers(@TempDir File tmp) throws Exception {
		File f = new File(tmp, "l.csv");
		CSVLogger l = new CSVLogger(f, COLS, false, ";", ",");
		l.log(map("a", 1.5, "b", "txt", "c", 2), map());
		l.log(map("a", 3, "b", "y", "c", 0.25), map());
		JSimLoggerFactory.remove(l);
		assertEquals("a;b;c\n1,5;txt;2\n3;y;0,25", read(f));
		assertEquals(f, l.getFile());
	}

	@Test
	void log_emptyValuesFallBackToDefaultsOrBlank(@TempDir File tmp) throws Exception {
		File f = new File(tmp, "l.csv");
		CSVLogger l = new CSVLogger(f, COLS, false, ",", ".");
		l.log(map("a", "", "b", null), map("a", "dflt", "c", 7));
		JSimLoggerFactory.remove(l);
		assertEquals("a,b,c\ndflt,,7", read(f));
	}

	@Test
	void append_skipsHeaderWhenFileAlreadyHasContent(@TempDir File tmp) throws Exception {
		File f = new File(tmp, "l.csv");
		Files.write(f.toPath(), "old".getBytes(StandardCharsets.UTF_8));
		CSVLogger l = new CSVLogger(f, COLS, true, ",", ".");
		l.log(map("a", 1, "b", 2, "c", 3), map());
		JSimLoggerFactory.remove(l);
		assertEquals("old\n1,2,3", read(f));
	}

	@Test
	void noAppend_overwritesExistingFileWithHeader(@TempDir File tmp) throws Exception {
		File f = new File(tmp, "l.csv");
		Files.write(f.toPath(), "old".getBytes(StandardCharsets.UTF_8));
		CSVLogger l = new CSVLogger(f, COLS, false, ",", ".");
		l.log(map("a", 1, "b", 2, "c", 3), map());
		JSimLoggerFactory.remove(l);
		assertEquals("a,b,c\n1,2,3", read(f));
	}
}
