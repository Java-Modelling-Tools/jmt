package jmt.jmva.analytical.solvers.queueingNet;

import jmt.jmva.analytical.solvers.basis.BTFCoMoMBasis;
import jmt.jmva.analytical.solvers.dataStructures.QNModel;
import jmt.jmva.analytical.solvers.exceptions.BTFMatrixErrorException;
import jmt.jmva.analytical.solvers.exceptions.InconsistentLinearSystemException;
import jmt.jmva.analytical.solvers.exceptions.InternalErrorException;
import jmt.jmva.analytical.solvers.linearSystem.btf.BTFLinearSystem;

/**
 * A concrete implementation of the CoMoMSolver class that choose to use the BTF Linear System.
 * @author Jack Bradshaw
 */
public class CoMoMBTFSolver extends CoMoMSolver {

	public CoMoMBTFSolver(QNModel qnm) throws InternalErrorException, BTFMatrixErrorException, InconsistentLinearSystemException {
		super(qnm);
		basis =  new BTFCoMoMBasis(qnm);
		system = new BTFLinearSystem(qnm, basis);
	}

	/**
	 * Prints a short welcome message that says which solver is being used.
	 */
	@Override
	public void printWelcome() {
		System.out.println("Using BTF CoMoM");
	}

}
