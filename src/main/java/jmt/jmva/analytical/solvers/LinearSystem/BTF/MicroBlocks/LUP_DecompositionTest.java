package jmt.jmva.analytical.solvers.LinearSystem.BTF.MicroBlocks;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jmt.jmva.analytical.solvers.Basis.BTFCoMoMBasis;
import jmt.jmva.analytical.solvers.Basis.CoMoMBasis;
import jmt.jmva.analytical.solvers.DataStructures.BigRational;
import jmt.jmva.analytical.solvers.DataStructures.QNModel;
import jmt.jmva.analytical.solvers.Exceptions.InconsistentLinearSystemException;
import jmt.jmva.analytical.solvers.Exceptions.InputFileParserException;
import jmt.jmva.analytical.solvers.LinearSystem.BTF.Position;
import jmt.jmva.analytical.solvers.Utilities.MiscFunctions;

/**
 * @author Jack Bradshaw
 */
public class LUP_DecompositionTest {

	//private BigRational A;
	private CoMoMBasis basis;
	private QNModel qnm;
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws InconsistentLinearSystemException, InputFileParserException {
		
		qnm = new QNModel("models\\test_model_4_comp_larger.txt");
		//qnm = new QNModel("models\\test_model_3.txt");
		basis =  new BTFCoMoMBasis(qnm);
			
		BigRational[][] A = 
			{
				{new BigRational(2), new BigRational(0), new BigRational(2), new BigRational("6/10")},
				{new BigRational(3), new BigRational(3), new BigRational(4), new BigRational(-2)},
				{new BigRational(5), new BigRational(5), new BigRational(4), new BigRational(2)},
				{new BigRational(-1), new BigRational(-2), new BigRational("34/10"), new BigRational(-1)}				
			};
		Position zero_zero = new Position(0,0);
		
		LUP_Decomposition LUP = new LUP_Decomposition(basis, zero_zero, A, false);
		
		BigRational[] b = {new BigRational(1), new BigRational(2), new BigRational(3), new BigRational(4)};
		
		MiscFunctions.printMatrix(b);
		
		BigRational[] x = LUP.test_solve(b);
		
		MiscFunctions.printMatrix(x);
		
		MiscFunctions.printMatrix(A);
		
		BigRational[] Ax = MiscFunctions.matrixVectorMultiplyJ(A, x);
		
		MiscFunctions.printMatrix(Ax);
	}

}
