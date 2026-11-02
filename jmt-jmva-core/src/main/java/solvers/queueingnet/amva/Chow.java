package solvers.queueingnet.amva;

/**
 * This class implements the Chow approximate MVA algorithm [Cho83] for solving
 * multiclass closed models. The Chow approximation estimates the queue length
 * seen by each class at every station from the queue lengths of the other
 * classes, as described in the AMVA section of the JMVA chapter of the JMT
 * user manual.
 *
 * @author Abhimanyu Chugh
 */
public class Chow extends AMVA {

	public Chow(int classes, int stations, int[] classPopulation) {
		super(classes, stations, classPopulation);
	}

	@Override
	protected double[][] getQueueLensWithOneLessCustomer() {
		double[][] scQueueLens = new double[stations][classes];
		for (int k = 0; k < stations; k++) {
			for (int c = 0; c < classes; c++) {
				// assume the queue lengths with one less customer in system
				// are the same as queue lengths with full customer population
				double currQueueLen = 0;
				for (int r = 0; r < classes; r++) {
					currQueueLen += queueLen[k][r];
				}
				scQueueLens[k][c] = currQueueLen;
			}
		}
		return scQueueLens;
	}

}
