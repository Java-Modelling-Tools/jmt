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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MD5ChecksumTest {

	private static File write(File dir, String name, String content) throws Exception {
		File f = new File(dir, name);
		Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
		return f;
	}

	@Test
	void getMD5Checksum_matchesKnownDigests(@TempDir File tmp) throws Exception {
		assertEquals("900150983cd24fb0d6963f7d28e17f72", MD5Checksum.getMD5Checksum(write(tmp, "a", "abc").getPath()));
		assertEquals("d41d8cd98f00b204e9800998ecf8427e", MD5Checksum.getMD5Checksum(write(tmp, "e", "").getPath()));
	}

	@Test
	void createChecksum_returns16BytesForFileLargerThanBuffer(@TempDir File tmp) throws Exception {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 3000; i++) {
			sb.append('x');
		}
		assertEquals(16, MD5Checksum.createChecksum(write(tmp, "big", sb.toString()).getPath()).length);
	}

	@Test
	void getMD5List_keepsFileOrder(@TempDir File tmp) throws Exception {
		File a = write(tmp, "a", "abc");
		File e = write(tmp, "e", "");
		List<String> l = MD5Checksum.getMD5List(new File[] { a, e });
		assertEquals(Arrays.asList("900150983cd24fb0d6963f7d28e17f72", "d41d8cd98f00b204e9800998ecf8427e"), l);
	}

	@Test
	void createChecksum_missingFileThrows(@TempDir File tmp) {
		assertThrows(java.io.FileNotFoundException.class, () -> MD5Checksum.createChecksum(new File(tmp, "nope").getPath()));
	}
}
