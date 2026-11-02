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
package jmt.framework.xml;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXParseException;

class XMLUtilsTest {

	@Test
	void saveXML_thenLoadXML_roundTripsDocument(@TempDir File tmp) throws Exception {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element root = doc.createElement("root");
		root.setAttribute("k", "v");
		root.appendChild(doc.createElement("child"));
		doc.appendChild(root);
		File f = new File(tmp, "out.xml");
		XMLUtils u = new XMLUtils();
		assertTrue(u.saveXML(doc, f));
		assertTrue(f.length() > 0);
		Document back = u.loadXML(f);
		assertEquals("root", back.getDocumentElement().getNodeName());
		assertEquals("v", back.getDocumentElement().getAttribute("k"));
		assertEquals(1, back.getDocumentElement().getElementsByTagName("child").getLength());
	}

	@Test
	void loadXML_malformedInputThrowsParseException(@TempDir File tmp) throws Exception {
		File f = new File(tmp, "bad.xml");
		Files.write(f.toPath(), "<a><b></a>".getBytes(StandardCharsets.UTF_8));
		assertThrows(SAXParseException.class, () -> new XMLUtils().loadXML(f));
	}

	@Test
	void loadXML_missingFileThrowsIOException(@TempDir File tmp) {
		assertThrows(java.io.FileNotFoundException.class, () -> new XMLUtils().loadXML(new File(tmp, "none.xml")));
	}
}
