package common.xml;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FileFormatTest {

	private final FileFormat multi = new FileFormat(".jsimg;.jmodel;.pnml", "JSIMgraph data file");

	@Test
	void acceptsAnyCase() {
		assertTrue(multi.accepts("a.jsimg"));
		assertTrue(multi.accepts("A.JSIMG"));
		assertTrue(multi.accepts("b.JModel"));
	}

	@Test
	void rejectsOtherSuffix() {
		assertFalse(multi.accepts("a.xml"));
		assertFalse(multi.accepts("jsimg"));
	}

	@Test
	void multipleExtensions() {
		assertTrue(multi.accepts("x.pnml"));
	}

	@Test
	void description() {
		assertEquals("JSIMgraph data file (*.jsimg; *.jmodel; *.pnml)", multi.getDescription());
		assertEquals("Single (*.xml)", new FileFormat(".xml", "Single").getDescription());
	}

	@Test
	void extensions() {
		assertArrayEquals(new String[] { ".jsimg", ".jmodel", ".pnml" }, multi.getExtensions());
	}
}
