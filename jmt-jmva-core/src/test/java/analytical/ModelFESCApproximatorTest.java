package analytical;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import solvers.SolverAlgorithm;
import solvers.dispatchers.SolverDispatcher;
import solvers.multiSolverAlgorithms.MultiFixtures;

class ModelFESCApproximatorTest {

	private static final SolverAlgorithm EXACT = SolverAlgorithm.EXACT;

	private static ExactModel multiServer(int n, int servers) {
		ExactModel m = MultiFixtures.closed(new int[] { MultiFixtures.LI, MultiFixtures.DELAY }, new double[] { n },
				new double[][] { { 2 }, { 3 } });
		m.setStationServers(new int[] { servers, 1 });
		return m;
	}

	@Test
	void modelWithoutMultipleServersIsSolvedAsIs() {
		ExactModel m = multiServer(2, 1);
		ModelFESCApproximator a = new ModelFESCApproximator(m, 0);
		assertSame(m, a.getModelToBeSolved());
		assertSame(m, a.processModelAfterSolution());
	}

	@Test
	void openModelWithMultipleServersIsNotApproximated() {
		ExactModel m = MultiFixtures.model(new int[] { MultiFixtures.LI }, new int[] { MultiFixtures.OPEN }, new double[] { 0.1 }, new double[][] { { 1 } });
		m.setStationServers(new int[] { 2 });
		assertSame(m, new ModelFESCApproximator(m, 0).getModelToBeSolved());
	}

	@Test
	void multiServerStationBecomesStationPlusDelayWithSplitDemand() {
		ExactModel m = multiServer(2, 4);
		ExactModel out = new ModelFESCApproximator(m, 0).getModelToBeSolved();
		assertNotSame(m, out);
		assertEquals(3, out.getStations());
		assertEquals(0.5, out.getServiceTimes()[0][0][0], 1e-12, "D/n");
		assertEquals(1.5, out.getServiceTimes()[2][0][0], 1e-12, "(n-1)/n D on the added delay");
		assertEquals(ExactConstants.STATION_DELAY, out.getStationTypes()[2]);
		assertEquals(1, out.getStationServers()[0]);
		assertEquals(2, m.getStations(), "input model untouched");
		assertEquals(4, m.getStationServers()[0]);
	}

	@Test
	void singleCustomerNeverQueuesSoTheApproximationIsExact() throws Exception {
		// N=1: R=D=2 whatever the number of servers, X=1/(2+3), busy servers U=X*D
		ExactModel m = multiServer(1, 2);
		new SolverDispatcher().solve(m);
		assertEquals(2, m.getStations(), "results are mapped back to the original stations");
		assertEquals(0.2, m.getThroughput(EXACT)[0][0][0], 1e-12);
		assertEquals(2.0, m.getResidTimes(EXACT)[0][0][0], 1e-12);
		assertEquals(0.4, m.getQueueLen(EXACT)[0][0][0], 1e-12);
		assertEquals(0.4, m.getUtilization(EXACT)[0][0][0], 1e-12);
		assertTrue(m.areResultsOK() || m.hasResults());
	}

	@Test
	void twoCustomersOnTwoServersAreWithinFivePercentOfTheExactNoQueueingValue() throws Exception {
		// exact: two servers, two customers never wait -> X=2/(2+3)=0.4, R=2. FESC: queue D=1 + delay 1+3
		// gives X=2/(1.2+4)=0.3846 (4% low) and R=1.2+1=2.2
		ExactModel m = multiServer(2, 2);
		new SolverDispatcher().solve(m);
		double x = m.getThroughput(EXACT)[0][0][0];
		assertEquals(2 / 5.2, x, 1e-12, "the FESC value itself");
		assertEquals(0.4, x, 0.05 * 0.4, "approximation error bounded");
		assertEquals(2.2, m.getResidTimes(EXACT)[0][0][0], 1e-12);
		// customers are conserved after merging station and delay back
		assertEquals(2.0, m.getQueueLen(EXACT)[0][0][0] + m.getQueueLen(EXACT)[1][0][0], 1e-12);
	}

	@Test
	void whatIfIterationsAreMappedToTheirOwnSlot() throws Exception {
		ExactModel m = multiServer(1, 2);
		m.setWhatIfType(ExactConstants.WHAT_IF_CUSTOMERS);
		m.setWhatIfClass(0);
		m.setWhatIfValues(new double[] { 1, 2 });
		new SolverDispatcher().solve(m);
		assertEquals(0.2, m.getThroughput(EXACT)[0][0][0], 1e-12);
		assertEquals(2 / 5.2, m.getThroughput(EXACT)[0][0][1], 1e-12);
	}
}
