package analytical;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jmt.framework.xml.XMLUtils;
import solvers.SolverAlgorithm;
import solvers.dispatchers.SolverDispatcher;
import solvers.multiSolverAlgorithms.MultiFixtures;

class ExactModelTest {

	private static final int LI = MultiFixtures.LI, DELAY = MultiFixtures.DELAY, CLOSED = MultiFixtures.CLOSED, OPEN = MultiFixtures.OPEN;

	private static ExactModel closed2() {
		return MultiFixtures.closed(new int[] { LI, DELAY }, new double[] { 3, 2 }, new double[][] { { 1, 2 }, { 3, 1 } });
	}

	@Test
	void modelTypeFlagsFollowTheClassTypes() {
		ExactModel c = closed2();
		assertTrue(c.isClosed() && !c.isOpen() && !c.isMixed());
		assertTrue(c.isMultiClass());
		ExactModel o = MultiFixtures.model(new int[] { LI }, new int[] { OPEN }, new double[] { 0.1 }, new double[][] { { 1 } });
		assertTrue(o.isOpen() && !o.isClosed() && !o.isMixed());
		assertFalse(o.isMultiClass());
		ExactModel mx = MultiFixtures.model(new int[] { LI }, new int[] { CLOSED, OPEN }, new double[] { 1, 0.1 }, new double[][] { { 1, 1 } });
		assertTrue(mx.isMixed() && !mx.isOpen() && !mx.isClosed());
		assertFalse(c.isLd());
		assertFalse(c.isPriority());
		assertFalse(c.isMultipleServers());
	}

	@Test
	void settersRejectArraysOfTheWrongSize() {
		ExactModel m = closed2();
		assertThrows(IllegalArgumentException.class, () -> m.setStationServers(new int[] { 1 }));
		assertThrows(IllegalArgumentException.class, () -> m.setStationNames(new String[] { "a" }));
		assertThrows(IllegalArgumentException.class, () -> m.setClassNames(new String[] { "a", "b", "c" }));
		assertTrue(m.setStationServers(new int[] { 3, 1 }));
		assertFalse(m.setStationServers(new int[] { 3, 1 }), "same data: no change");
		assertTrue(m.isMultipleServers());
	}

	@Test
	void delayStationWithManyServersIsNotMultiServer() {
		ExactModel m = closed2();
		m.setStationServers(new int[] { 1, 5 });
		assertFalse(m.isMultipleServers());
	}

	@Test
	void copyConstructorIsDeep() throws Exception {
		ExactModel m = closed2();
		new SolverDispatcher().solve(m);
		ExactModel c = new ExactModel(m);
		assertEquals(m.getStations(), c.getStations());
		assertTrue(c.hasResults());
		SolverAlgorithm a = SolverAlgorithm.EXACT;
		assertArrayEquals(m.getThroughput(a)[0][0], c.getThroughput(a)[0][0], 0);
		c.getServiceTimes()[0][0][0] = 99;
		c.getThroughput(a)[0][0][0] = -1;
		assertEquals(1, m.getServiceTimes()[0][0][0], 0);
		assertTrue(m.getThroughput(a)[0][0][0] > 0);
	}

	@Test
	void changingTheModelInvalidatesResults() throws Exception {
		ExactModel m = closed2();
		new SolverDispatcher().solve(m);
		assertTrue(m.hasResults());
		m.setClassData(new double[] { 4, 2 });
		assertFalse(m.areResultsOK());
		m.resetResults();
		assertFalse(m.hasResults());
	}

	@Test
	void xmlRoundTripKeepsModelAndResults(@TempDir File dir) throws Exception {
		ExactModel m = closed2();
		m.setClassNames(new String[] { "A", "B" });
		m.setStationNames(new String[] { "cpu", "think" });
		m.setDescription("two classes");
		new SolverDispatcher().solve(m);
		File f = new File(dir, "m.xml");
		assertTrue(new XMLUtils().saveXML(m.createDocument(), f));
		ExactModel b = new ExactModel();
		assertTrue(b.loadDocument(new XMLUtils().loadXML(f)));
		assertArrayEquals(new String[] { "A", "B" }, b.getClassNames());
		assertArrayEquals(new String[] { "cpu", "think" }, b.getStationNames());
		assertArrayEquals(new double[] { 3, 2 }, b.getClassData(), 0);
		assertArrayEquals(m.getStationTypes(), b.getStationTypes());
		assertEquals(3, b.getServiceTimes()[1][0][0], 0);
		SolverAlgorithm a = SolverAlgorithm.EXACT;
		assertTrue(b.hasResults());
		for (int s = 0; s < 2; s++) {
			for (int c = 0; c < 2; c++) {
				assertEquals(m.getQueueLen(a)[s][c][0], b.getQueueLen(a)[s][c][0], 1e-9);
				assertEquals(m.getUtilization(a)[s][c][0], b.getUtilization(a)[s][c][0], 1e-9);
			}
		}
	}

	@Test
	void xmlRoundTripKeepsWhatIfAndMultiServer(@TempDir File dir) throws Exception {
		ExactModel m = closed2();
		m.setStationServers(new int[] { 2, 1 });
		m.setWhatIfType(ExactConstants.WHAT_IF_DEMANDS);
		m.setWhatIfClass(0);
		m.setWhatIfStation(0);
		m.setWhatIfValues(new double[] { 1, 2, 3 });
		File f = new File(dir, "w.xml");
		assertTrue(new XMLUtils().saveXML(m.createDocument(), f));
		ExactModel b = new ExactModel();
		assertTrue(b.loadDocument(new XMLUtils().loadXML(f)));
		assertTrue(b.isWhatIf());
		assertEquals(ExactConstants.WHAT_IF_DEMANDS, b.getWhatIfType());
		assertEquals(0, b.getWhatIfClass());
		assertEquals(0, b.getWhatIfStation());
		assertArrayEquals(new double[] { 1, 2, 3 }, b.getWhatIfValues(), 1e-12);
		assertEquals(2, b.getStationServers()[0]);
		b.removeWhatIf();
		assertFalse(b.isWhatIf());
	}

	// ---- generateWhatIfValues ------------------------------------------------------------------------------------

	@Test
	void arrivalAndDemandValuesAreEvenlySpacedAndNeverZeroRate() {
		ExactModel m = closed2();
		assertArrayEquals(new double[] { 1, 2, 3 }, m.generateWhatIfValues(ExactConstants.WHAT_IF_DEMANDS, 1, 3, 3, 0, 0), 1e-12);
		assertArrayEquals(new double[] { 3, 2, 1 }, m.generateWhatIfValues(ExactConstants.WHAT_IF_DEMANDS, 3, 1, 3, 0, 0), 1e-12, "inverted range reversed");
		double[] a = m.generateWhatIfValues(ExactConstants.WHAT_IF_ARRIVAL, 0, 1, 2, 0, 0);
		assertEquals(1e-5, a[0], 0, "zero arrival rate avoided");
		assertEquals(1, a[1], 1e-12);
		assertArrayEquals(new double[] { 2 }, m.generateWhatIfValues(ExactConstants.WHAT_IF_DEMANDS, 2, 2, 5, 0, 0), 0);
		assertNull(m.generateWhatIfValues("unknown", 1, 2, 3, 0, 0));
	}

	@Test
	void customerValuesAreIntegersWithoutDuplicates() {
		ExactModel m = closed2();
		assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, m.generateWhatIfValues(ExactConstants.WHAT_IF_CUSTOMERS, 1, 5, 5, 0, -1), 0);
		// 3 steps over 1..2 round to 1,2,2 (1.5 rounds to even 2): duplicates dropped
		double[] v = m.generateWhatIfValues(ExactConstants.WHAT_IF_CUSTOMERS, 1, 2, 3, 0, -1);
		for (int i = 1; i < v.length; i++) {
			assertTrue(v[i] > v[i - 1]);
			assertEquals(Math.rint(v[i]), v[i], 0);
		}
	}

	@Test
	void mixValuesAreMultiplesOfOneOverTotalPopulation() {
		ExactModel m = MultiFixtures.closed(new int[] { LI }, new double[] { 2, 2 }, new double[][] { { 1, 1 } });
		// total 4: fractions must be k/4 with 1<=k<=3
		double[] v = m.generateWhatIfValues(ExactConstants.WHAT_IF_MIX, 0, 1, 10, 0, -1);
		assertArrayEquals(new double[] { 0.25, 0.5, 0.75 }, v, 1e-12);
	}

	@Disabled("bug: generateWhatIfValues(Number of Customers, all classes) indexes classTypes[c] with the closed-class counter instead of classTypes[i], so a mixed model whose first class is open collects no closed populations: hcf falls back to 1 and the step is 1 instead of 1/hcf. Reachable from the JMVA wizard (what-if on customers, all classes) for mixed models")
	@Test
	void customerFractionsOnMixedModelUseTheClosedPopulationsHcf() {
		ExactModel m = MultiFixtures.model(new int[] { LI }, new int[] { OPEN, CLOSED, CLOSED }, new double[] { 0.1, 4, 6 },
				new double[][] { { 1, 1, 1 } });
		// closed populations 4 and 6 -> hcf 2 -> smallest fraction 1/2, so scaling factors 0.5, 1, ...
		double[] v = m.generateWhatIfValues(ExactConstants.WHAT_IF_CUSTOMERS, 0.5, 1.5, 3, -1, -1);
		assertEquals(0.5, v[0], 1e-12);
		assertEquals(1.0, v[1], 1e-12);
		assertEquals(1.5, v[2], 1e-12);
	}
}
