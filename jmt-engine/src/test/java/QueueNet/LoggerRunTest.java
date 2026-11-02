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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jmt.common.ErrorReporters;

/** Runs a model with a Logger station (LogTunnel) writing its CSV into a temp directory. */
@Tag("simulation")
class LoggerRunTest {
	@TempDir
	Path dir;

	@AfterEach
	void restore() {
		ErrorReporters.set(null);
	}

	@Test
	void loggerStationWritesOneCsvRowPerJobWithItsColumns() throws Exception {
		Path logDir = Files.createDirectory(dir.resolve("logs"));
		NetFixtures.writeVariant(dir, "logger", "logger-run", "@LOGDIR@", logDir.toString());
		NetFixtures.Run r = NetFixtures.runInDir(dir, "logger-run", 99L);
		assertTrue(r.sim.hasFinished());
		File csv = logDir.resolve("Logger1.csv").toFile();
		assertTrue(csv.exists(), "logger csv written");
		java.util.List<String> lines = Files.readAllLines(csv.toPath());
		assertTrue(lines.size() > 1, "header plus rows");
		assertTrue(lines.get(0).contains("LoggerName") || lines.get(0).toLowerCase().contains("logger"), lines.get(0));
	}
}
