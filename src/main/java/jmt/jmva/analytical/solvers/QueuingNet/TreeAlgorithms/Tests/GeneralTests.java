package jmt.jmva.analytical.solvers.QueuingNet.TreeAlgorithms.Tests;

import org.junit.Test;

import jmt.jmva.analytical.solvers.DataStructures.QNModel;
import jmt.jmva.analytical.solvers.QueuingNet.TreeAlgorithms.Tests.TestUtils;
import jmt.jmva.analytical.solvers.Utilities.RandomNetworkGenerator;

/** Contains tests which can be run for any pair of solvers.
 * @author Ben Homer, 2014.
 */
public class GeneralTests {

	/** The test configuration to run each test with. */
	private TestConfig t;
	
	/**
	 * Constructor
	 * @param t The test configuration to run each test with.
	 */
	public GeneralTests(TestConfig t) {
		this.t = t;
	}
	
	/** Runs all tests using the local test configuration. */
	@Test
	public void runAll() {
		TestResultConsistencyForLamLienExampleNetwork();
		TestResultConsistencyForTucciExampleNetwork();
		TestResultConsistencyForTucciLeftExampleNetwork();
		TestResultConsistencyForTucciRightExampleNetwork();
		TestResultConsistencyForTempFile();
		TestResultConsistencyForTempFileTC();
		
		TestResultConsistency_1Station_2Classes_pop1_1_demandZero();
		TestResultConsistency_1Station_2Classes_pop1_1_demandsDifferent();
		TestResultConsistency_2IdenticalStations_2Classes_pop1_1_demandsIdentical();
		TestResultConsistency_2IdenticalStations_2Classes_pop1_1_demandsUnique();
		TestResultConsistency_4IdenticalStations_2Classes_pop1_1_demandsIdentical();
		TestResultConsistency_4IdenticalStations_2Classes_pop5_11_demandsIdentical();
		TestResultConsistency_4Stations_2Classes_pop5_11_uniqueDemands();
		TestResultConsistency_4Stations_3Classes_pop1_1_1_demandsIdentical();
		TestResultConsistency_4Stations_3Classes_pop1_2_3_demandsUnique();
		TestResultConsistency_8Stations_4Classes_pop1_2_3_4_demandsUnique();
		TestResultConsistency_2IdenticalStations_2Classes_pop1_1_demandsIncZeros();
		TestResultConsistency_2IdenticalStations_2Classes_pop2_5_demandsIncZeros();
		TestResultConsistency_4IdenticalStations_2Classes_pop1_1_demandsIncZeros();
		TestResultConsistency_8Stations_4Classes_pop1_2_3_4_demandsUnique2();
		TestResultConsistencyForRandomModel();
		TestRandomModelWithStationsNotPowerOf2();
	}
	
	public void Test2_2_150() {
		TestUtils.TestResultConsistencyForModelFile(
				"C:/Users/BenLaptop/workspace/project/JCoMoM/models/model2-2-150.qn", t);
	}
	
	public void Test2_4_150() {
		TestUtils.TestResultConsistencyForModelFile(
				"C:/Users/BenLaptop/workspace/project/JCoMoM/models/model2-4-150.qn", t);
	}
	
	public void Test4_4_300() {
		TestUtils.TestResultConsistencyForModelFile(
				"C:/Users/BenLaptop/workspace/project/JCoMoM/models/model4-4-300.qn", t);
	}
	
	public void TestResultConsistencyForLamLienExampleNetwork() {
		TestUtils.TestResultConsistencyForModelFile(
				"C:/Users/BenLaptop/workspace/project/JCoMoM/models/lamlien_ex1Small.qn", t);
	}
	
	public void TestResultConsistencyForTucciExampleNetwork() {
		TestUtils.TestResultConsistencyForModelFile(
				"C:/Users/BenLaptop/workspace/project/JCoMoM/models/tucci_ex1.qn", t);
	}
	
	public void TestResultConsistencyForTucciLeftExampleNetwork() {
		TestUtils.TestResultConsistencyForModelFile(
				"C:/Users/BenLaptop/workspace/project/JCoMoM/models/tucci_ex1LeftS.qn", t);
	}
	
	public void TestResultConsistencyForTucciRightExampleNetwork() {
		TestUtils.TestResultConsistencyForModelFile(
				"C:/Users/BenLaptop/workspace/project/JCoMoM/models/tucci_ex1RightS.qn",  t);
	}
	
	public void TestResultConsistencyForTempFile() {
		TestUtils.TestResultConsistencyForModelFile("C:/Users/BenLaptop/workspace/project/JCoMoM/models/tmva1.qn", t);
	}
	
	public void TestResultConsistencyForTempFileTC() {
		TestUtils.TestResultConsistencyForModelFile("C:/Users/BenLaptop/workspace/project/JCoMoM/models/tmva1.qn", t);
	}
	
	// ** Incremental Tests start here **/ 
	
	public void TestResultConsistency_1Station_2Classes_pop1_1_demandZero() {
		int R = 2; // no. classes
	    int M = 1; // no. stations
	    int[] ty = {1}; // queue types
	    Integer[] N = {1, 1}; // class populations
	    double[] Z = {0, 0}; // delays
	    Integer[] mul = {1}; // multiplicities
	    double[][] D = {{0, 0}}; // demands
	    
		QNModel model;
		try {
			model = new QNModel(R, M, ty, N, Z, mul, D);
			TestUtils.TestResultsConsistencyForModel(model, t);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void TestResultConsistency_1Station_2Classes_pop1_1_demandsDifferent() {
		int R = 2; // no. classes
	    int M = 1; // no. stations
	    int[] ty = {1}; // queue types
	    Integer[] N = {1, 1}; // class populations
	    double[] Z = {0, 0}; // delays
	    Integer[] mul = {1}; // multiplicities
	    double[][] D = {{5, 8}}; // demands
	    
		QNModel model;
		try {
			model = new QNModel(R, M, ty, N, Z, mul, D);
			TestUtils.TestResultsConsistencyForModel(model, t);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void TestResultConsistency_2IdenticalStations_2Classes_pop1_1_demandsIdentical() {
		int R = 2; // no. classes
	    int M = 2; // no. stations
	    int[] ty = {1, 1}; // queue types
	    Integer[] N = {1, 1}; // class populations
	    double[] Z = {0, 0}; // delays
	    Integer[] mul = {1, 1}; // multiplicities
	    double[][] D = {{1, 1}, {1, 1}}; // demands
	    
		QNModel model;
		try {
			model = new QNModel(R, M, ty, N, Z, mul, D);
			TestUtils.TestResultsConsistencyForModel(model, t);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void TestResultConsistency_2IdenticalStations_2Classes_pop1_1_demandsUnique() {
		int R = 2; // no. classes
	    int M = 2; // no. stations
	    int[] ty = {1, 1}; // queue types
	    Integer[] N = {1, 1}; // class populations
	    double[] Z = {0, 0}; // delays
	    Integer[] mul = {1, 1}; // multiplicities
	    double[][] D = {{1, 2}, {3, 4}}; // demands
	    
		QNModel model;
		try {
			model = new QNModel(R, M, ty, N, Z, mul, D);
			TestUtils.TestResultsConsistencyForModel(model, t);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void TestResultConsistency_4IdenticalStations_2Classes_pop1_1_demandsIdentical() {
		int R = 2; // no. classes
	    int M = 4; // no. stations
	    int[] ty = {1, 1, 1, 1}; // queue types
	    Integer[] N = {1, 1}; // class populations
	    double[] Z = {0, 0}; // delays
	    Integer[] mul = {1, 1, 1, 1}; // multiplicities
	    double[][] D = {{1, 1}, {1, 1}, {1, 1}, {1, 1}}; // demands
	    
		QNModel model;
		try {
			model = new QNModel(R, M, ty, N, Z, mul, D);
			TestUtils.TestResultsConsistencyForModel(model, t);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void TestResultConsistency_4IdenticalStations_2Classes_pop5_11_demandsIdentical() {
	    int R = 2; // no. classes
	    int M = 4; // no. stations
	    int[] ty = {1, 1, 1, 1}; // queue types
	    Integer[] N = {5, 11}; // class populations
	    double[] Z = {0, 0}; // delays
	    Integer[] mul = {1, 1, 1, 1}; // multiplicities
	    double[][] D = {{1, 1}, {1, 1}, {1, 1}, {1, 1}}; // demands
	    
		QNModel model;
		try {
			model = new QNModel(R, M, ty, N, Z, mul, D);
			TestUtils.TestResultsConsistencyForModel(model, t);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void TestResultConsistency_4Stations_2Classes_pop5_11_uniqueDemands() {
	    int R = 2; // no. classes
	    int M = 4; // no. stations
	    int[] ty = {1, 1, 1, 1}; // queue types
	    Integer[] N = {5, 11}; // class populations
	    double[] Z = {0, 0}; // delays
	    Integer[] mul = {1, 1, 1, 1}; // multiplicities
	    double[][] D = {{1, 2}, {3, 4}, {5, 6}, {7, 8}}; // demands
	    
		QNModel model;
		try {
			model = new QNModel(R, M, ty, N, Z, mul, D);
			TestUtils.TestResultsConsistencyForModel(model, t);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void TestResultConsistency_4Stations_3Classes_pop1_1_1_demandsIdentical() {
	    int R = 3; // no. classes
	    int M = 4; // no. stations
	    int[] ty = {1, 1, 1, 1}; // queue types
	    Integer[] N = {1, 1, 1}; // class populations
	    double[] Z = {0, 0, 0}; // delays
	    Integer[] mul = {1, 1, 1, 1}; // multiplicities
	    double[][] D = {{1, 1, 1}, {1, 1, 1}, {1, 1, 1}, {1, 1, 1}}; // demands
	    
		QNModel model;
		try {
			model = new QNModel(R, M, ty, N, Z, mul, D);
			TestUtils.TestResultsConsistencyForModel(model, t);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void TestResultConsistency_4Stations_3Classes_pop1_2_3_demandsUnique() {
	    int R = 3; // no. classes
	    int M = 4; // no. stations
	    int[] ty = {1, 1, 1, 1}; // queue types
	    Integer[] N = {1, 2, 3}; // class populations
	    double[] Z = {0, 0, 0}; // delays
	    Integer[] mul = {1, 1, 1, 1}; // multiplicities
	    double[][] D = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}, {10, 11, 12}}; // demands
	    
		QNModel model;
		try {
			model = new QNModel(R, M, ty, N, Z, mul, D);
			TestUtils.TestResultsConsistencyForModel(model, t);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void TestResultConsistency_8Stations_4Classes_pop1_2_3_4_demandsUnique() {
	    int R = 4; // no. classes
	    int M = 8; // no. stations
	    int[] ty = {1, 1, 1, 1, 1, 1, 1, 1}; // queue types
	    Integer[] N = {1, 2, 3, 4}; // class populations
	    double[] Z = {0, 0, 0, 0}; // delays
	    Integer[] mul = {1, 1, 1, 1, 1, 1, 1, 1}; // multiplicities
	    double[][] D = {{1, 2, 3, 4}, {5, 6, 7, 8}, {9, 10, 11, 12}, {13, 14, 15, 16}, {1, 2, 3, 4}, {5, 6, 7, 8}, {9, 10, 11, 12}, {13, 14, 15, 16}}; // demands
	    
		QNModel model;
		try {
			model = new QNModel(R, M, ty, N, Z, mul, D);
			TestUtils.TestResultsConsistencyForModel(model, t);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void TestResultConsistency_2IdenticalStations_2Classes_pop1_1_demandsIncZeros() {
		int R = 2; // no. classes
	    int M = 2; // no. stations
	    int[] ty = {1, 1}; // queue types
	    Integer[] N = {1, 1}; // class populations
	    double[] Z = {0, 0}; // delays
	    Integer[] mul = {1, 1}; // multiplicities
	    double[][] D = {{0, 1}, {1, 0}}; // demands
	    
		QNModel model;
		try {
			model = new QNModel(R, M, ty, N, Z, mul, D);
			TestUtils.TestResultsConsistencyForModel(model, t);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void TestResultConsistency_2IdenticalStations_2Classes_pop2_5_demandsIncZeros() {
		int R = 2; // no. classes
	    int M = 2; // no. stations
	    int[] ty = {1, 1}; // queue types
	    Integer[] N = {2, 2}; // class populations
	    double[] Z = {0, 0}; // delays
	    Integer[] mul = {1, 1}; // multiplicities
	    double[][] D = {{0, 1}, {1, 1}}; // demands
	    
		QNModel model;
		try {
			model = new QNModel(R, M, ty, N, Z, mul, D);
			TestUtils.TestResultsConsistencyForModel(model, t);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void TestResultConsistency_4IdenticalStations_2Classes_pop1_1_demandsIncZeros() {
		int R = 2; // no. classes
	    int M = 4; // no. stations
	    int[] ty = {1, 1, 1, 1}; // queue types
	    Integer[] N = {1, 1}; // class populations
	    double[] Z = {0, 0}; // delays
	    Integer[] mul = {1, 1, 1, 1}; // multiplicities
	    double[][] D = {{0, 0}, {0, 1}, {0, 1}, {1, 0}}; // demands
	    
		QNModel model;
		try {
			model = new QNModel(R, M, ty, N, Z, mul, D);
			TestUtils.TestResultsConsistencyForModel(model, t);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void TestResultConsistency_8Stations_4Classes_pop1_2_3_4_demandsUnique2() {
	    int R = 4; // no. classes
	    int M = 8; // no. stations
	    int[] ty = {1, 1, 1, 1, 1, 1, 1, 1}; // queue types
	    Integer[] N = {1, 2, 3, 4}; // class populations
	    double[] Z = {0, 0, 0, 0}; // delays
	    Integer[] mul = {1, 1, 1, 1, 1, 1, 1, 1}; // multiplicities
	    double[][] D = {{0, 1, 0, 0}, {5, 6, 7, 8}, {9, 10, 0, 12}, {13, 0, 15, 16}, {1, 2, 3, 0}, {0, 0, 1, 0}, {9, 10, 11, 12}, {13, 0, 15, 16}}; // demands
	    
		QNModel model;
		try {
			model = new QNModel(R, M, ty, N, Z, mul, D);
			TestUtils.TestResultsConsistencyForModel(model, t);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void TestResultConsistency_delays_random() {
		for (int runs = 0; runs < 10; runs++) {
			RandomNetworkGenerator rng = new RandomNetworkGenerator(true, 2);
			QNModel qnm = rng.generateSparseNetwork(8, 4, 0.5, false, true);
			TestUtils.TestResultsConsistencyForModel(qnm, t);
		}
	}
	
	public void TestResultConsistencyForRandomModel() {
		//for (int runs = 0; runs < 10; runs++) {
			RandomNetworkGenerator rng = new RandomNetworkGenerator(true, 2);
						QNModel qnm = rng.generateSparseNetwork(8, 4, 0.5, false, false);
						TestUtils.TestResultsConsistencyForModel(qnm, t);
		//}
	}	
	
	public void TestRandomModelWithStationsNotPowerOf2() {
		RandomNetworkGenerator rng = new RandomNetworkGenerator(true, 3);
		QNModel qnm = rng.generateSparseNetwork(5, 4, 0.5, false, false);
		TestUtils.TestResultsConsistencyForModel(qnm, t);
		qnm = rng.generateSparseNetwork(12, 4, 0.5, false, false);
		TestUtils.TestResultsConsistencyForModel(qnm, t);
	}
}
