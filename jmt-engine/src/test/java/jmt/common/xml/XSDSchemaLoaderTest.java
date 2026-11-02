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
package jmt.common.xml;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

class XSDSchemaLoaderTest {

	@Test
	void loadSchema_knownSchemaIsUrl() {
		String url = XSDSchemaLoader.loadSchema(XSDSchemaLoader.JMVA_MODEL_DEFINITION);
		assertNotNull(url);
		assertTrue(url.endsWith("JMTmodel.xsd"));
	}

	@Test
	void loadSchema_unknownSchemaIsNull() {
		assertNull(XSDSchemaLoader.loadSchema("missing.xsd"));
	}

	@Test
	void loadSchemaAsStream_knownReadableUnknownNull() throws Exception {
		try (InputStream in = XSDSchemaLoader.loadSchemaAsStream(XSDSchemaLoader.JSIM_MODEL_DEFINITION)) {
			assertNotNull(in);
			assertTrue(in.read() != -1);
		}
		assertNull(XSDSchemaLoader.loadSchemaAsStream("missing.xsd"));
	}
}
