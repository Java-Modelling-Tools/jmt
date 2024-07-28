package jmt.jmva.analytical.solvers.queueingNet;

import javax.naming.OperationNotSupportedException;

import jmt.jmva.analytical.solvers.basis.CoMoMBasis;
import jmt.jmva.analytical.solvers.dataStructures.BigRational;
import jmt.jmva.analytical.solvers.dataStructures.PopulationVector;
import jmt.jmva.analytical.solvers.dataStructures.QNModel;
import jmt.jmva.analytical.solvers.exceptions.BTFMatrixErrorException;
import jmt.jmva.analytical.solvers.exceptions.InconsistentLinearSystemException;
import jmt.jmva.analytical.solvers.exceptions.InternalErrorException;
import jmt.jmva.analytical.solvers.linearSystem.LinearSystem;

/**
 * This class provides the high-level implementation of the CoMoM algorithm, it is programmed 
 * to the interface provided by the LinearSystem class. Sub-classes must choose the linear system they 
 * are to use.
 * 
 * @author Jack Bradshaw *
 */
public class CoMoMSolver extends QNSolver {

	private  int M,R;

	private PopulationVector current_N;
	private PopulationVector target_N;

	protected LinearSystem system;

	protected CoMoMBasis basis;

	public CoMoMSolver(QNModel qnm) throws InternalErrorException {
		super(qnm);
		M = qnm.M;
		R = qnm.R;
		target_N = qnm.N;
	}

	@Override
	public void computeNormalisingConstant() throws InternalErrorException, OperationNotSupportedException, InconsistentLinearSystemException, BTFMatrixErrorException {
		current_N = new PopulationVector(0,R);

		for (int _class = 1; _class <= R; _class++) {
			current_N.plusOne(_class);
			system.initialiseForClass(current_N, _class);

			solveForClass(_class);
		}

		//Store the computed normalising constant
		BigRational G = basis.getNormalisingConstant();
		qnm.setNormalisingConstant(G);
	}

	private  void solveForClass(int _class) throws InternalErrorException, OperationNotSupportedException, InconsistentLinearSystemException, BTFMatrixErrorException {
		for (int class_population = current_N.get(_class - 1);
				class_population <= target_N.get(_class - 1);
				class_population++) {
			system.update(class_population);
			system.solve();

			if (class_population < target_N.get(_class - 1)) {
				current_N.plusOne(_class);
			}
		}
	}

	@Override
	public void computePerformanceMeasures() throws InternalErrorException {
		system.computePerformanceMeasures();
	}

}
