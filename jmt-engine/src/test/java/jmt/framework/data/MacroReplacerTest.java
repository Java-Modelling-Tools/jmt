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
package jmt.framework.data;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MacroReplacerTest {

	private String oldWorkDir;

	@BeforeEach
	void save() {
		oldWorkDir = System.getProperty("jmt.work.dir");
		System.setProperty("jmt.test.macro", "VAL$ue");
	}

	@AfterEach
	void restore() {
		System.clearProperty("jmt.test.macro");
		if (oldWorkDir == null) {
			System.clearProperty("jmt.work.dir");
		} else {
			System.setProperty("jmt.work.dir", oldWorkDir);
		}
	}

	@Test
	void replace_nullAndMacroFreeStringsAreUnchanged() {
		assertNull(MacroReplacer.replace(null));
		assertEquals("plain", MacroReplacer.replace("plain"));
	}

	@Test
	void replace_systemPropertyIsSubstitutedLiterally() {
		assertEquals("a-VAL$ue-b", MacroReplacer.replace("a-${jmt.test.macro}-b"));
		assertEquals("VAL$ue", MacroReplacer.replace(MacroReplacer.Type.SYSTEM, "${jmt.test.macro}"));
	}

	@Test
	void replace_unknownMacroIsLeftInPlace() {
		assertEquals("${no.such.prop.jmt}", MacroReplacer.replace("${no.such.prop.jmt}"));
	}

	@Test
	void replace_appTypeIgnoresSystemProperties() {
		assertEquals("${jmt.test.macro}", MacroReplacer.replace(MacroReplacer.Type.APP, "${jmt.test.macro}"));
	}

	@Test
	void replace_systemTypeIgnoresWorkdirMacroWhenNotSetAsProperty() {
		System.clearProperty("jmt.work.dir");
		assertEquals(MacroReplacer.MACRO_WORKDIR, MacroReplacer.replace(MacroReplacer.Type.SYSTEM, MacroReplacer.MACRO_WORKDIR));
	}

	@Test
	void replace_workdirMacroCreatesConfiguredDirectory(@TempDir File tmp) {
		File dir = new File(tmp, "work" + File.separator + "sub");
		System.setProperty("jmt.work.dir", dir.getPath());
		assertEquals(dir.getPath(), MacroReplacer.replace(MacroReplacer.MACRO_WORKDIR));
		assertTrue(dir.isDirectory());
	}

	@Test
	void replace_workdirOnlyExpandsUnderAppOrAllTypes(@TempDir File tmp) {
		System.setProperty("jmt.work.dir", tmp.getPath());
		assertEquals(tmp.getPath(), MacroReplacer.replace(MacroReplacer.Type.APP, "${jmt.work.dir}"));
		assertEquals(tmp.getPath(), MacroReplacer.replace(MacroReplacer.Type.ALL, "${jmt.work.dir}"));
	}
}
