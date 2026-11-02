package solvers.dispatchers;

import java.io.File;

import analytical.ExactModel;
import jmt.framework.xml.XMLUtils;
import solvers.SolverAlgorithm;
import solvers.multiSolverAlgorithms.MultiFixtures;

/** Small models and file helpers for the dispatcher and command-line tests. */
public final class DispatchFixtures {

	static final int DELAY = MultiFixtures.DELAY;
	static final int LI = MultiFixtures.LI;
	static final int CLOSED = MultiFixtures.CLOSED;
	static final int OPEN = MultiFixtures.OPEN;

	private DispatchFixtures() {
	}

	/** Single closed class, queue D=2 then delay Z=3, population n. */
	public static ExactModel repairman(int n) {
		return MultiFixtures.closed(new int[] { LI, DELAY }, new double[] { n }, new double[][] { { 2 }, { 3 } });
	}

	/** Single closed class, two queues D=1 and D=2, population n. */
	static ExactModel twoQueues(int n) {
		return MultiFixtures.closed(new int[] { LI, LI }, new double[] { n }, new double[][] { { 1 }, { 2 } });
	}

	/** Two closed classes on a queue (D=1,2) and a delay (Z=3,1). */
	static ExactModel twoClasses(int n1, int n2) {
		return MultiFixtures.closed(new int[] { LI, DELAY }, new double[] { n1, n2 }, new double[][] { { 1, 2 }, { 3, 1 } });
	}

	/** Measure [station][class] of the first (or only) iteration. */
	static double x(ExactModel m, SolverAlgorithm a, int st, int cl, int it) {
		return m.getThroughput(a)[st][cl][it];
	}

	static double q(ExactModel m, SolverAlgorithm a, int st, int cl, int it) {
		return m.getQueueLen(a)[st][cl][it];
	}

	static double r(ExactModel m, SolverAlgorithm a, int st, int cl, int it) {
		return m.getResidTimes(a)[st][cl][it];
	}

	static double u(ExactModel m, SolverAlgorithm a, int st, int cl, int it) {
		return m.getUtilization(a)[st][cl][it];
	}

	public static File write(ExactModel m, File f) throws Exception {
		if (!new XMLUtils().saveXML(m.createDocument(), f)) {
			throw new IllegalStateException("cannot write " + f);
		}
		return f;
	}

	public static ExactModel read(File f) throws Exception {
		ExactModel m = new ExactModel();
		if (!m.loadDocument(new XMLUtils().loadXML(f))) {
			throw new IllegalStateException("cannot load " + f);
		}
		return m;
	}
}
