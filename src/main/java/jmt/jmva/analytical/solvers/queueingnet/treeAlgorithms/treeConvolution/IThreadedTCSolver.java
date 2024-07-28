package jmt.jmva.analytical.solvers.queueingNet.treeAlgorithms.treeConvolution;

import jmt.jmva.analytical.solvers.dataStructures.BigRational;
import jmt.jmva.analytical.solvers.dataStructures.PopulationVector;
import jmt.jmva.analytical.solvers.queueingNet.treeAlgorithms.Node;
import jmt.jmva.analytical.solvers.utilities.Timer;

/**
 * The solver interface for tree convolution solvers.
 * @author Ben Homer, 2014.
 */
public interface IThreadedTCSolver {
	
	/** Timer for computing normalisation constant. */
	Timer normalisationConstantTimer = new Timer();
	
	/** Timer for computing throughputs. */
	Timer throughputTimer = new Timer();
	
	/** Timer for computing mean queue lengths. */
	Timer queueLengthTimer = new Timer();
	
	/** Compute G using postorder tree traversal. 
	 * @param node The current node to compute the normalisation constant G for.
	 * @param pv The population vector to be used during the computation.
	 * @returns The normalisation constant G at the given node, calculated using the
	 * specified population vector.
	 */
	BigRational computeG(Node node, PopulationVector pv);
	
	/** 
	 * Calculates per class throughputs using methods outlined in LamLien83 paper.
	 * @param p The full population vector.
	 * @return The per class throughputs as an array of BigRationals.
	 */
	BigRational[] calculateThroughputs(PopulationVector p);
	
	/** 
	 * Calculates per class and per station mean queue lengths using methods outlined in LamLien83 paper.
	 * @param p The full population vector.
	 * @return The per class per station mean queue lengths as a 2D array of BigRationals.
	 */
	BigRational[][] calculateMeanQueueLengths(PopulationVector p);
}
