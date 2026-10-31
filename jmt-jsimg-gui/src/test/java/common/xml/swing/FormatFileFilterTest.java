package common.xml.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import common.xml.FileFormat;

class FormatFileFilterTest {

	private final FileFormat format = new FileFormat(".jsimg;.jmodel", "Model");
	private final FormatFileFilter filter = new FormatFileFilter(format);

	@Test
	void acceptsDirectory(@TempDir File dir) {
		assertTrue(filter.accept(dir));
	}

	@Test
	void acceptsAndRejectsFiles(@TempDir File dir) {
		assertTrue(filter.accept(new File(dir, "m.JSIMG")));
		assertFalse(filter.accept(new File(dir, "m.xml")));
	}

	@Test
	void descriptionDelegated() {
		assertEquals(format.getDescription(), filter.getDescription());
	}
}
