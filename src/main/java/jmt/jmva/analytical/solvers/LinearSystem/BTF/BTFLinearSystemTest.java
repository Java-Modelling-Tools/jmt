package jmt.jmva.analytical.solvers.LinearSystem.BTF;

import static org.junit.Assert.*;

import javax.naming.OperationNotSupportedException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import jmt.jmva.analytical.solvers.Basis.BTFCoMoMBasis;
import jmt.jmva.analytical.solvers.Basis.CoMoMBasis;
import jmt.jmva.analytical.solvers.DataStructures.BigRational;
import jmt.jmva.analytical.solvers.DataStructures.MultiplicitiesVector;
import jmt.jmva.analytical.solvers.DataStructures.PopulationChangeVector;
import jmt.jmva.analytical.solvers.DataStructures.PopulationVector;
import jmt.jmva.analytical.solvers.DataStructures.QNModel;
import jmt.jmva.analytical.solvers.Exceptions.BTFMatrixErrorException;
import jmt.jmva.analytical.solvers.Exceptions.InconsistentLinearSystemException;
import jmt.jmva.analytical.solvers.Exceptions.InputFileParserException;
import jmt.jmva.analytical.solvers.Exceptions.InternalErrorException;
import jmt.jmva.analytical.solvers.QueuingNet.ConvolutionSolver;
import jmt.jmva.analytical.solvers.Utilities.MiscFunctions;

/**
 * @author Jack Bradshaw
 */
public class BTFLinearSystemTest {

	private QNModel qnm;
	private CoMoMBasis basis;
	BTFLinearSystem system;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		qnm = new QNModel("models\\test_model_4");
	
		basis =  new BTFCoMoMBasis(qnm);
		//system without in place LUP decomposition
		system = new BTFLinearSystem(qnm, basis, false);
		
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void Muliplytest() throws BTFMatrixErrorException, InputFileParserException, InternalErrorException, OperationNotSupportedException, InconsistentLinearSystemException {
		
		ConvolutionSolver convolution = new ConvolutionSolver(qnm);
		
		BigRational G ;
		
		for(int cur_class = 1; cur_class <= qnm.R; cur_class++) {
			PopulationVector N = qnm.N.copy();
			for(int i = cur_class - 1; i < qnm.R; i++) {
				N.set(i,0);
			}
			
			System.out.print("N: " + N + "\n");			
			
			system.initialiseForClass(N, cur_class);
			N.restore();
			system.update(1);
			System.out.print("N: " + N + "\n");		
						
			//Compute values in previous basis
			basis.startBasisComputation();
			for(int i = 0; i < basis.getOrder().size(); i++) {
				PopulationChangeVector n = basis.getOrder().get(i);
				for(int k = 0; k <= qnm.M; k++) {
					
					MultiplicitiesVector m = qnm.getMultiplicitiesVector();	
					m.plusOne(k);
					
					//compute RHS value
					G = convolution.compute(m, N.changePopulation(n));
					basis.setValue(G.copy(), basis.indexOf(n, k));
				}
			}
			
			//Compute values in new basis:
			basis.startBasisComputation();
			for(int i = 0; i < basis.getOrder().size(); i++) {
				PopulationChangeVector n = basis.getOrder().get(i);
				for(int k = 0; k <= qnm.M; k++) {
					
					MultiplicitiesVector m = qnm.getMultiplicitiesVector();	
					m.plusOne(k);
					
					//compute LHS value
					N.plusOne(cur_class);
					
					G = convolution.compute(m, N.changePopulation(n));
					N.restore();
					basis.setValue(G.copy(), basis.indexOf(n, k));
				}
			}
			
			//intialise vector to store results
			BigRational[] lhs_result = new BigRational[basis.getSize()];
			BigRational[] rhs_result = new BigRational[basis.getSize()];
			for(int i = 0; i < lhs_result.length; i++) {
				lhs_result[i] = BigRational.ZERO;
				rhs_result[i] = BigRational.ZERO;
			}
			
			//printing lhs and rhs:
			System.out.print(String.format("%40s", "lhs: "));
			System.out.print(String.format("%40s", "rhs: "));
			System.out.print("\n");
			for(int i = 0; i < basis.getSize(); i++) {
				System.out.print(String.format("%40s", basis.getNewValue(i)));
				System.out.print(String.format("%40s", basis.getOldValue(i)));
				System.out.print("\n");
			}
			
			system.multiply(lhs_result, rhs_result);	
			
			
			for(int i = 0; i < MiscFunctions.binomialCoefficient(qnm.M + qnm.R - 1 , qnm.M); i++)  { //loop over all possible population changes n
		   		PopulationChangeVector n  = basis.getPopulationChangeVector(i).copy(); // To improve bug safety
		   		if(n.sumTail(cur_class-1) > 0) {  //potential negative population
		   			for(int k = 0; k <= qnm.M; k++) {
		   		
		   				int row = basis.indexOf(n,k);
		   				
		   				if(n.sumTail(cur_class) == 0) {  //carry
		   					
		   					n.minusOne(cur_class); 
		   					rhs_result[row] = basis.getOldValue(basis.indexOf(n, k));
		   				
		   					n.restore();
		   				}    				
		   			}
		   		}
			}
			
			//printing lhs and rhs results:
			System.out.print(String.format("%40s", "Multiplied lhs: "));
			System.out.print(String.format("%40s", "Multiplied rhs: "));
			System.out.print("\n");
			for(int i = 0; i < basis.getSize(); i++) {
				System.out.print(String.format("%40s", lhs_result[i]));
				System.out.print(String.format("%40s", rhs_result[i]));
				System.out.print("\n");
				assertTrue(lhs_result[i].equals(rhs_result[i]));
			}
		}
	}
	
	@Test
	public void Solvetest() throws BTFMatrixErrorException, InputFileParserException, InternalErrorException, OperationNotSupportedException, InconsistentLinearSystemException {
		
		
		ConvolutionSolver convolution = new ConvolutionSolver(qnm);
		
		BigRational G ;
		
		for(int cur_class = 1; cur_class <= qnm.R; cur_class++) {
			PopulationVector N = qnm.N.copy();
			for(int i = cur_class - 1; i < qnm.R; i++) {
				N.set(i,0);
			}
			
			System.out.print("N: " + N + "\n");			
			
			system.initialiseForClass(N, cur_class);
			N.restore();
			system.update(1);
			System.out.print("N: " + N + "\n");		
			
			//Compute values in previous basis
			basis.startBasisComputation();
			for(int i = 0; i < basis.getOrder().size(); i++) {
				PopulationChangeVector n = basis.getOrder().get(i);
				for(int k = 0; k <= qnm.M; k++) {
					
					MultiplicitiesVector m = qnm.getMultiplicitiesVector();	
					m.plusOne(k);
					
					//compute RHS value
					G = convolution.compute(m, N.changePopulation(n));
					basis.setValue(G.copy(), basis.indexOf(n, k));
				}
			}
			
			//Compute expected values
			BigRational[] result = new BigRational[basis.getSize()];
			for(int i = 0; i < basis.getOrder().size(); i++) {
				PopulationChangeVector n = basis.getOrder().get(i);
				for(int k = 0; k <= qnm.M; k++) {
					
					MultiplicitiesVector m = qnm.getMultiplicitiesVector();	
					m.plusOne(k);
					
					//compute LHS value
					N.plusOne(cur_class);
					
					G = convolution.compute(m, N.changePopulation(n));
					N.restore();
					result[basis.indexOf(n, k)] = G.copy();
				}
			}
			
			//Solve the system, the results will accessable via basis.getNewValue();			
			system.solve();				
			
			
			//printing lhs and rhs results:
			System.out.print(String.format("%40s", "expect: "));
			System.out.print(String.format("%40s", "actual: "));
			System.out.print("\n");
			for(int i = 0; i < basis.getSize(); i++) {
				System.out.print(String.format("%40s", result[i]));
				System.out.print(String.format("%40s", basis.getNewValue(i)));
				System.out.print("\n");
				assertTrue(result[i].equals(basis.getNewValue(i)));
			}
		}
	}
}
