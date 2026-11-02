package analytical;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jmt.common.exception.SolverException;
import jmt.framework.xml.XMLUtils;
import solvers.multiSolverAlgorithms.MultiFixtures;

/**
 * Moment is not reached from production code (only its own main). Oracle: two queues D=1,2, N=2.
 * States (n1,n2) = (2,0),(1,1),(0,2) with weights 1,2,4: E[Q1]=4/7, E[Q1^2]=6/7, Var=26/49.
 */
class MomentTest {

	private static File write(ExactModel m, File dir) throws Exception {
		File f = new File(dir, "m.xml");
		assertTrue(new XMLUtils().saveXML(m.createDocument(), f));
		return f;
	}

	private static void checkMoments(Moment mo, double tol) throws Exception {
		double b1 = mo.getBinomialMoment(new int[] { 1, 0 });
		double b2 = mo.getBinomialMoment(new int[] { 2, 0 });
		assertEquals(11.0 / 7, b1, tol, "E[Q]+1");
		assertEquals(16.0 / 7, b2, tol, "(E[Q]+1)(E[Q of replicated model]+1)");
		double p1 = mo.getFirstPowerMoment(b1);
		double p2 = mo.getSecondPowerMoment(p1, b2);
		assertEquals(4.0 / 7, p1, tol);
		assertEquals(6.0 / 7, p2, tol);
		double var = mo.getVariance(p1, p2);
		assertEquals(26.0 / 49, var, tol);
		assertEquals(Math.sqrt(26.0 / 49), mo.getStandardDeviation(var), tol);
	}

	@Test
	void singleClassMomentsMatchTheExactDistribution(@TempDir File dir) throws Exception {
		Moment mo = new Moment();
		assertTrue(mo.loadModel(write(MultiFixtures.closed(new int[] { MultiFixtures.LI, MultiFixtures.LI }, new double[] { 2 },
				new double[][] { { 1 }, { 2 } }), dir)));
		checkMoments(mo, 1e-9);
	}

	@Test
	void identicalClassesGiveTheSameAggregateMomentsAsOneClass(@TempDir File dir) throws Exception {
		// two classes with equal demands and one customer each behave like one class of two customers (multiclass MVA path)
		Moment mo = new Moment();
		mo.loadModel(write(MultiFixtures.closed(new int[] { MultiFixtures.LI, MultiFixtures.LI }, new double[] { 1, 1 },
				new double[][] { { 1, 1 }, { 2, 2 } }), dir));
		checkMoments(mo, 1e-9);
	}

	@Test
	void powerMomentFormulas() {
		Moment mo = new Moment();
		assertEquals(2.5, mo.getFirstPowerMoment(3.5), 0);
		assertEquals(2 * 5.0 - 3 * 1.5 - 2, mo.getSecondPowerMoment(1.5, 5.0), 1e-12);
		assertEquals(6 * 2.0 - 6 * 3.0 - 11 * 1.0 - 6, mo.getThirdPowerMoment(1.0, 3.0, 2.0), 1e-12);
		assertEquals(0.75, mo.getVariance(1.5, 3.0), 1e-12);
		assertEquals(3.0, mo.getStandardDeviation(9.0), 0);
	}

	@Test
	void unreadableModelIsASolverException(@TempDir File dir) throws Exception {
		File bad = new File(dir, "bad.xml");
		Files.write(bad.toPath(), "nope".getBytes("UTF-8"));
		assertThrows(SolverException.class, () -> new Moment().loadModel(bad));
		assertThrows(SolverException.class, () -> new Moment().loadModel(new File(dir, "missing.xml")));
	}

	@Test
	void mainPrintsMeanAndVariancePerStation(@TempDir File dir) throws Exception {
		File f = write(MultiFixtures.closed(new int[] { MultiFixtures.LI, MultiFixtures.LI }, new double[] { 2 },
				new double[][] { { 1 }, { 2 } }), dir);
		PrintStream old = System.out;
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		System.setOut(new PrintStream(buf, true, "UTF-8"));
		try {
			Moment.main(new String[] {});
			Moment.main(new String[] { f.getPath() });
		} finally {
			System.setOut(old);
		}
		String out = buf.toString("UTF-8");
		assertTrue(out.contains("Please pass an input model file"), out);
		java.util.regex.Matcher mean = java.util.regex.Pattern.compile("Mean queue length at [^:]*: (\\S+)").matcher(out);
		java.util.regex.Matcher var = java.util.regex.Pattern.compile("Variance of queue length at [^:]*: (\\S+)").matcher(out);
		assertTrue(mean.find() && var.find(), out);
		assertEquals(4.0 / 7, Double.parseDouble(mean.group(1)), 1e-9);
		assertEquals(26.0 / 49, Double.parseDouble(var.group(1)), 1e-9);
		assertTrue(mean.find() && var.find(), "second station printed: " + out);
		assertEquals(10.0 / 7, Double.parseDouble(mean.group(1)), 1e-9);
	}
}
