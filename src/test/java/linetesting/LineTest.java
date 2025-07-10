package linetesting;

import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;

public class LineTest {

	@Test
	void testLine() {

		String testFolderName = "tpb80d3b67_44ae_4d6c_8a83_fa3b14eb9397";

		assertTrue(TestUtil.testJSIMGraph(testFolderName));

	}

}