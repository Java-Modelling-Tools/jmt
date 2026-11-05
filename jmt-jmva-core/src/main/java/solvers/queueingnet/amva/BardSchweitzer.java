package solvers.queueingnet.amva;

/**
 * This class implements the Bard-Schweitzer approximate MVA algorithm [Sch79]
 * for solving multiclass closed models. The approximation removes the class
 * dependence of the queue lengths by substituting the queue length seen by a
 * class with the total queue length, as described in the AMVA section of the
 * JMVA chapter of the JMT user manual.
 *
 * @author Abhimanyu Chugh
 */
public class BardSchweitzer extends AMVA {

	public BardSchweitzer(int classes, int stations, int[] classPopulation) {
		super(classes, stations, classPopulation);
	}

	@Override
	protected double[][] getQueueLensWithOneLessCustomer() {
		double[][] scQueueLens = new double[stations][classes];
		for (int k = 0; k < stations; k++) {
			for (int c = 0; c < classes; c++) {
				double currQueueLen = 0;
				for (int r = 0; r < classes; r++) {
					// if this is the class with one less customer, normalise its queue length
					// otherwise assume its queue length is same as queue length with full customer
					// population
					if (c == r) {
						if (clsPopulation[r] != 0) {
							currQueueLen += queueLen[k][r] * (clsPopulation[r] - 1) / (double) clsPopulation[r];
						}
					} else {
						currQueueLen += queueLen[k][r];
					}
				}
				scQueueLens[k][c] = currQueueLen;
			}
		}
		return scQueueLens;
	}

}
