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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JSimLoggerTest {

	@AfterEach
	void reset() {
		JSimLogger.logCalenderOff();
		JSimLogger.setDelimiter(",");
		JSimLogger.setDecimalSeparator(".");
	}

	@Test
	void decimalSeparator_isConfigurable() {
		JSimLogger.setDecimalSeparator(",");
		assertEquals(",", JSimLogger.getDecimalSeparator());
	}

	@Test
	void calender_flagDelegatesToStateManager() {
		JSimLogger l = JSimLogger.getLogger();
		assertFalse(l.isLogCalenderOn());
		JSimLogger.logCalenderOn();
		assertTrue(l.isLogCalenderOn());
		JSimLogger.logCalenderOff();
		assertFalse(l.isLogCalenderOn());
	}

	@Test
	void getLogger_variantsProduceUsableLoggers() {
		JSimLogger[] all = { JSimLogger.getRootLogger(), JSimLogger.getLogger(), JSimLogger.getLogger((Object) null),
				JSimLogger.getLogger(new Object()), JSimLogger.getLogger(String.class), JSimLogger.getLogger("custom.name") };
		for (JSimLogger l : all) {
			assertNotNull(l);
			l.debug("d");
			l.info("i");
			l.warn("w");
			l.error("e");
			l.error(new RuntimeException("t"));
			l.error("m", new RuntimeException("t"));
			l.fatal("f");
			l.isDebugEnabled();
			l.isInfoEnabled();
		}
	}

	@Test
	void serialization_restoresNamedAndRootLoggers() throws Exception {
		for (JSimLogger l : new JSimLogger[] { JSimLogger.getLogger("ser.name"), JSimLogger.getRootLogger() }) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
				out.writeObject(l);
			}
			try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
				JSimLogger back = (JSimLogger) in.readObject();
				back.info("works after deserialization");
			}
		}
	}

	@Test
	void debugCsv_headerThenCustomRowsUseDelimiter(@TempDir File tmp) throws Exception {
		JSimLogger.setDelimiter(";");
		JSimLogger.resetDebugLoggerStorePath(tmp.getPath());
		JSimLogger.logCustomDebug("D", "s", "ev", "r", "1.0");
		File f = new File(tmp, "JSIM_debug.csv");
		List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
		// header was written with the delimiter in force at reset time
		assertEquals("dispatchOrReceive;sender;eventname;receiver;timestamp", lines.get(0));
		assertEquals("D;s;ev;r;1.0", lines.get(1));
		// resetting again deletes the old file and rewrites only the header
		JSimLogger.resetDebugLoggerStorePath(tmp.getPath());
		assertEquals(1, Files.readAllLines(f.toPath(), StandardCharsets.UTF_8).size());
	}
}
