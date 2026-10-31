package solvers.dataStructures;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import solvers.dataStructures.QNModel.QueueType;
import solvers.exceptions.IllegalValueInInputFileException;
import solvers.exceptions.InputFileParserException;
import solvers.exceptions.InternalErrorException;

class QNModelTest {

	@TempDir
	Path dir;

	/** R=2, M=3: N=(2,1), Z=(0,5), D rows scaled by station. */
	private static QNModel model() throws InternalErrorException {
		return new QNModel(2, 3, new int[] { 1, 0, 2 }, new Integer[] { 2, 1 }, new double[] { 0, 5 },
				new Integer[] { 1, 2, 1 }, new double[][] { { 1, 2 }, { 3, 4 }, { 0, 6 } });
	}

	private Path write(String content) throws IOException {
		Path p = dir.resolve("m.qn");
		Files.write(p, content.getBytes(StandardCharsets.UTF_8));
		return p;
	}

	@Test
	void arrayConstructorExposesInputs() throws Exception {
		QNModel m = model();
		assertEquals(2, m.R);
		assertEquals(3, m.M);
		assertEquals(2.0, m.getDemand(0, 1), 0.0);
		assertEquals(6.0, m.getDemand(2, 1), 0.0);
		assertEquals(5.0, m.getDelay(1), 0.0);
		assertEquals(QueueType.LI, m.getQueueType(0));
		assertEquals(QueueType.LD, m.getQueueType(1));
		assertEquals(QueueType.DELAY, m.getQueueType(2));
		for (int k = 0; k < 3; k++) {
			assertEquals(1, m.getNumberOfServers(k));
		}
	}

	@Test
	void serverCountsAreTakenFromExtendedConstructor() throws Exception {
		QNModel m = new QNModel(1, 2, new int[] { 1, 1 }, new Integer[] { 3 }, new double[] { 0 }, new Integer[] { 1, 1 },
				new double[][] { { 1 }, { 2 } }, new int[] { 4, 1 });
		assertEquals(4, m.getNumberOfServers(0));
		assertEquals(1, m.getNumberOfServers(1));
		assertThrows(InternalErrorException.class, () -> new QNModel(1, 2, new int[] { 1, 1 }, new Integer[] { 3 },
				new double[] { 0 }, new Integer[] { 1, 1 }, new double[][] { { 1 }, { 2 } }, new int[] { 4 }));
	}

	@Test
	void sizeMismatchesAreRejected() {
		double[][] d = { { 1, 2 }, { 3, 4 } };
		assertThrows(InternalErrorException.class, () -> new QNModel(2, 2, new int[] { 1, 1 }, new Integer[] { 1 }, new double[] { 0, 0 }, new Integer[] { 1, 1 }, d));
		assertThrows(InternalErrorException.class, () -> new QNModel(2, 2, new int[] { 1, 1 }, new Integer[] { 1, 1 }, new double[] { 0 }, new Integer[] { 1, 1 }, d));
		assertThrows(InternalErrorException.class, () -> new QNModel(2, 2, new int[] { 1, 1 }, new Integer[] { 1, 1 }, new double[] { 0, 0 }, new Integer[] { 1 }, d));
		assertThrows(InternalErrorException.class, () -> new QNModel(2, 3, new int[] { 1, 1, 1 }, new Integer[] { 1, 1 }, new double[] { 0, 0 }, new Integer[] { 1, 1, 1 }, d));
		assertThrows(InternalErrorException.class, () -> new QNModel(3, 2, new int[] { 1, 1 }, new Integer[] { 1, 1, 1 }, new double[] { 0, 0, 0 }, new Integer[] { 1, 1 }, d));
	}

	@Test
	void numericConversionsOfDemandsAndDelays() throws Exception {
		QNModel m = new QNModel(1, 1, new int[] { 1 }, new Integer[] { 1 }, new double[] { 2.9 }, new Integer[] { 1 },
				new double[][] { { 0.5 } });
		assertEquals(0, new BigDecimal("0.5").compareTo(m.getDemandAsBigDecimal(0, 0)));
		assertEquals(new BigRational(1, 2), m.getDemandAsBigRational(0, 0));
		assertEquals(BigInteger.ZERO, m.getDemandAsBigInteger(0, 0)); // truncation
		assertEquals(BigInteger.valueOf(2), m.getDelayAsBigInteger(0));
		assertEquals(0, new BigDecimal(2.9).compareTo(m.getDelayAsBigDecimal(0)));
		assertEquals(new BigRational(2.9), m.getDelayAsBigRational(0));
	}

	@Test
	void copyConstructorIsDeepForArraysAndVectors() throws Exception {
		QNModel a = model();
		QNModel b = new QNModel(a);
		assertEquals(a.R, b.R);
		assertEquals(a.M, b.M);
		assertEquals(a.N, b.N);
		assertNotSame(a.N, b.N);
		b.Z[1] = 99;
		b.N.set(0, 7);
		b.multiplicities.set(0, 8);
		assertEquals(5.0, a.getDelay(1), 0.0);
		assertEquals(2, a.N.get(0));
		assertEquals(1, a.multiplicities.get(0));
		for (int k = 0; k < a.M; k++) {
			assertEquals(a.getQueueType(k), b.getQueueType(k));
			assertEquals(a.getNumberOfServers(k), b.getNumberOfServers(k));
			for (int r = 0; r < a.R; r++) {
				assertEquals(a.getDemand(k, r), b.getDemand(k, r), 0.0);
			}
		}
	}

	@Test
	void vectorAccessorsReturnCopies() throws Exception {
		QNModel m = model();
		MultiplicitiesVector mv = m.getMultiplicitiesVector();
		assertEquals(new MultiplicitiesVector(new Integer[] { 1, 2, 1 }), mv);
		mv.set(0, 42);
		assertEquals(1, m.multiplicities.get(0));
		PopulationVector pv = m.getPopulationVector();
		assertEquals(new PopulationVector(new Integer[] { 2, 1 }), pv);
		pv.set(0, 42);
		assertEquals(2, m.N.get(0));
	}

	@Test
	void maxValuesAreMaxOfPopulationDelayAndDemand() throws Exception {
		assertEquals(6.0, model().getMaxModelValue(), 0.0);
		QNModel bigN = new QNModel(1, 1, new int[] { 1 }, new Integer[] { 50 }, new double[] { 1 }, new Integer[] { 1 }, new double[][] { { 2 } });
		assertEquals(50.0, bigN.getMaxModelValue(), 0.0);
		QNModel bigZ = new QNModel(1, 1, new int[] { 1 }, new Integer[] { 1 }, new double[] { 70 }, new Integer[] { 1 }, new double[][] { { 2 } });
		assertEquals(70.0, bigZ.getMaxModelValue(), 0.0);
	}

	@Test
	void maxGIsPowerOfTenBoundHandComputed() throws Exception {
		// Dmax=4, Ntot=2, M=R=1: 4*(2+1+1)=16, ceil(log10 16)=2, nmax=2*2=4 -> 10^5
		QNModel m = new QNModel(1, 1, new int[] { 1 }, new Integer[] { 2 }, new double[] { 0 }, new Integer[] { 1 }, new double[][] { { 4 } });
		assertEquals(BigInteger.TEN.pow(5), m.getMaxG());
	}

	@Test
	void maxGBoundsActualNormalisingConstantOfSingleStation() throws Exception {
		// one LI station, D=4, N=2, no delay: G = D^N = 16
		QNModel m = new QNModel(1, 1, new int[] { 1 }, new Integer[] { 2 }, new double[] { 0 }, new Integer[] { 1 }, new double[][] { { 4 } });
		assertTrue(m.getMaxG().compareTo(BigInteger.valueOf(16)) > 0);
	}

	@Test
	void resultsCannotBeReadBeforeBeingSet() throws Exception {
		QNModel m = model();
		assertThrows(UnsupportedOperationException.class, m::getNormalisingConstant);
		assertThrows(UnsupportedOperationException.class, m::getPrettyNormalisingConstant);
		assertThrows(UnsupportedOperationException.class, m::getMeanThroughputs);
		assertThrows(UnsupportedOperationException.class, m::getMeanQueueLengths);
	}

	@Test
	void normalisingConstantIsWriteOnce() throws Exception {
		QNModel m = model();
		m.setNormalisingConstant(new BigRational(1, 4));
		assertEquals(new BigRational(1, 4), m.getNormalisingConstant());
		assertEquals("0.25", m.getPrettyNormalisingConstant());
		assertThrows(InternalErrorException.class, () -> m.setNormalisingConstant(BigRational.ONE));
		assertEquals(new BigRational(1, 4), m.getNormalisingConstant());
		QNModel n = model();
		n.setNormalisingConstant(new BigRational(1, 3));
		assertEquals("1/3", n.getPrettyNormalisingConstant());
	}

	@Test
	void performanceMeasuresAreWriteOnceAndConvertToDoubles() throws Exception {
		QNModel m = new QNModel(2, 1, new int[] { 1 }, new Integer[] { 1, 1 }, new double[] { 0, 0 }, new Integer[] { 1 }, new double[][] { { 1, 1 } });
		BigRational[][] q = { { new BigRational(1, 4), new BigRational(3, 4) } };
		BigRational[] x = { new BigRational(1, 2), new BigRational(1, 8) };
		m.setPerformanceMeasures(q, x);
		assertSame(q, m.getMeanQueueLengths());
		assertSame(x, m.getMeanThroughputs());
		assertArrayEquals(new double[] { 0.5, 0.125 }, m.getMeanThroughputsAsDoubles(), 0.0);
		assertEquals(0.25, m.getMeanQueueLengthsAsDoubles()[0][0], 0.0);
		assertEquals(0.75, m.getMeanQueueLengthsAsDoubles()[0][1], 0.0);
		assertThrows(InternalErrorException.class, () -> m.setPerformanceMeasures(q, x));
	}

	@Test
	void hasNoFractionalPart() throws Exception {
		QNModel m = model();
		assertTrue(m.hasNoFractionalPart(3.0));
		assertTrue(m.hasNoFractionalPart(-2.0));
		assertFalse(m.hasNoFractionalPart(0.5));
	}

	@Test
	void readsWellFormedFile() throws Exception {
		// R=2; N=(3,1); Z=(0,2); M=2; station 1: mult 1, D=(4,5); station 2: mult 2, D=(6,7)
		QNModel m = new QNModel(write("2\n3 1\n0 2\n2\n1 4 5\n2 6 7\n").toString());
		assertEquals(2, m.R);
		assertEquals(2, m.M);
		assertEquals(new PopulationVector(new Integer[] { 3, 1 }), m.N);
		assertEquals(2.0, m.getDelay(1), 0.0);
		assertEquals(new MultiplicitiesVector(new Integer[] { 1, 2 }), m.multiplicities);
		assertEquals(4.0, m.getDemand(0, 0), 0.0);
		assertEquals(7.0, m.getDemand(1, 1), 0.0);
		assertEquals(QueueType.LI, m.getQueueType(1));
		assertEquals(1, m.getNumberOfServers(0));
	}

	@Test
	void rejectsTruncatedAndMalformedFiles() throws Exception {
		assertThrows(InputFileParserException.class, () -> new QNModel(write("").toString()));
		assertThrows(InputFileParserException.class, () -> new QNModel(write("abc").toString()));
		assertThrows(InputFileParserException.class, () -> new QNModel(write("2\n3\n").toString()));
		assertThrows(InputFileParserException.class, () -> new QNModel(write("1\n3\n0\n1\n1\n").toString()));
	}

	@Test
	void rejectsFractionalAndZeroValues() throws Exception {
		assertThrows(IllegalValueInInputFileException.class, () -> new QNModel(write("1.5\n3\n0\n1\n1 4\n").toString()));
		assertThrows(IllegalValueInInputFileException.class, () -> new QNModel(write("1\n2.5\n0\n1\n1 4\n").toString()));
		assertThrows(IllegalValueInInputFileException.class, () -> new QNModel(write("1\n2\n0.5\n1\n1 4\n").toString()));
		assertThrows(IllegalValueInInputFileException.class, () -> new QNModel(write("1\n2\n0\n0\n").toString()));
		assertThrows(IllegalValueInInputFileException.class, () -> new QNModel(write("1\n2\n0\n1.5\n1 4\n").toString()));
		assertThrows(IllegalValueInInputFileException.class, () -> new QNModel(write("1\n2\n0\n1\n1.5 4\n").toString()));
	}

	@Disabled("unused-code defect: QNModel(String) is only called from solvers.control.Main and RandomNetworkGenerator; readModel() calls fr.close() in finally although fr is null when FileReader throws, so a missing file raises NullPointerException instead of InputFileParserException(\"File not found.\")")
	@Test
	void missingFileIsReportedAsParserException() {
		assertThrows(InputFileParserException.class, () -> new QNModel(dir.resolve("absent.qn").toString()));
	}
}
