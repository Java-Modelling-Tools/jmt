package jmt.jmva.analytical.solvers.QueuingNet.amva.test;


import jmt.jmva.analytical.solvers.QueuingNet.amva.AQL;
import jmt.jmva.analytical.solvers.QueuingNet.amva.Linearizer;
import jmt.jmva.analytical.solvers.QueuingNet.amva.Solver;

public class TestAMVA {

	public static void main(String[] args) {

		testMultiClosedLI();
		System.out.println("\n------------------------FOO 1----------------------------\n");
		testMultiClosedLI_visits_not1();
		System.out.println("\n------------------------FOO 2----------------------------\n");
		testMultiClosedLI_big();
		System.out.println("\n------------------------FOO 3----------------------------\n");
		testLinearizerExample();
		return;

	}
	
	public static void testMultiClosedLI() {

		System.out.println("Closed system with 2 classes");
		long start = System.currentTimeMillis();

		String[] name = { "Station1", "Station2" };
		int[] type = { Solver.LI, Solver.LI, };
		double[][][] servt = new double[2][3][1];

		int[] pop = new int[3];
		pop[0] = 10;
		pop[1] = 10;
		pop[2] = 10;

		//station 1
		servt[0][0][0] = 10;
		servt[0][1][0] = 5;
		servt[0][2][0] = 1;

		//station 2
		servt[1][0][0] = 5;
		servt[1][1][0] = 9;
		servt[1][2][0] = 1;

		double[][] visits = new double[2][3];

		//station 1
		visits[0][0] = 1;
		visits[0][1] = 1;
		visits[0][2] = 1;

		//station 2
		visits[1][0] = 1;
		visits[1][1] = 1;
		visits[1][2] = 1;

//		SolverMultiClosedMVA mClosedMVA = new SolverMultiClosedMVA(3, 2);
//		if (mClosedMVA.input(name, type, servt, visits, pop)) {
//			mClosedMVA.solve();
//			System.out.println(mClosedMVA);
//			System.out.println();
//			System.out.println("End Multi Closed LI - MVA");
//			long stop = System.currentTimeMillis();
//			System.out.print("Elapsed time (s): " + ((stop - start)/(double)1000));
//			System.out.println();
//
//		} else {
//			System.out.println("Wrong input!!");
//		}

		start = System.currentTimeMillis();
		AQL mClosedAQL = new AQL(3, 2, pop);
		if (mClosedAQL.input(name, type, servt, visits)) {
			mClosedAQL.solve();
			System.out.println(mClosedAQL);
			System.out.println();
			System.out.println("End Multi Closed LI - AQL");
			long stop = System.currentTimeMillis();
			System.out.print("Elapsed time (s): " + ((stop - start)/(double)1000));
			System.out.println();

		} else {
			System.out.println("Wrong input!!");
		}

		start = System.currentTimeMillis();
		Linearizer mClosedLinearizer = new Linearizer(3, 2, pop, false);
		if (mClosedLinearizer.input(name, type, servt, visits)) {
			mClosedLinearizer.solve();
			System.out.println(mClosedLinearizer);
			System.out.println();
			System.out.println("End Multi Closed LI - Linearizer");
			long stop = System.currentTimeMillis();
			System.out.print("Elapsed time (s): " + ((stop - start)/(double)1000));
			System.out.println();

		} else {
			System.out.println("Wrong input!!");
		}
		
		start = System.currentTimeMillis();
		Linearizer mClosedLinearizerDS = new Linearizer(3, 2, pop, true);
		if (mClosedLinearizerDS.input(name, type, servt, visits)) {
			mClosedLinearizerDS.solve();
			System.out.println(mClosedLinearizerDS);
			System.out.println();
			System.out.println("End Multi Closed LI - Linearizer (DeSouza-Muntz)");
			long stop = System.currentTimeMillis();
			System.out.print("Elapsed time (s): " + ((stop - start)/(double)1000));
			System.out.println();

		} else {
			System.out.println("Wrong input!!");
		}
	}

	public static void testMultiClosedLI_visits_not1() {

		System.out.println("Closed system with 2 classes - visits > 1");
		long start = System.currentTimeMillis();

		String[] name = { "Station1", "Station2" };
		int[] type = { Solver.LI, Solver.LI, };
		double[][][] servt = new double[2][2][1];

		int[] pop = new int[2];
		pop[0] = 100;
		pop[1] = 50;

		//station 1
		servt[0][0][0] = 10;
		servt[0][1][0] = 5;

		//station 2
		servt[1][0][0] = 5;
		servt[1][1][0] = 9;

		double[][] visits = new double[2][2];

		//station 1
		visits[0][0] = 3;
		visits[0][1] = 4;

		//station 2
		visits[1][0] = 2;
		visits[1][1] = 5;

//		SolverMultiClosedMVA mClosedMVA = new SolverMultiClosedMVA(2, 2);
//		if (mClosedMVA.input(name, type, servt, visits, pop)) {
//			mClosedMVA.solve();
//			System.out.println(mClosedMVA);
//			System.out.println();
//			System.out.println("End Multi Closed LI - MVA");
//			long stop = System.currentTimeMillis();
//			System.out.print("Elapsed time (s): " + ((stop - start)/(double)1000));
//			System.out.println();
//
//		} else {
//			System.out.println("Wrong input!!");
//		}

		start = System.currentTimeMillis();
		AQL mClosedAQL = new AQL(2, 2, pop);
		if (mClosedAQL.input(name, type, servt, visits)) {
			mClosedAQL.solve();
			System.out.println(mClosedAQL);
			System.out.println();
			System.out.println("End Multi Closed LI - AQL");
			long stop = System.currentTimeMillis();
			System.out.print("Elapsed time (s): " + ((stop - start)/(double)1000));
			System.out.println();

		} else {
			System.out.println("Wrong input!!");
		}

		start = System.currentTimeMillis();
		Linearizer mClosedLinearizer = new Linearizer(2, 2, pop, false);
		if (mClosedLinearizer.input(name, type, servt, visits)) {
			mClosedLinearizer.solve();
			System.out.println(mClosedLinearizer);
			System.out.println();
			System.out.println("End Multi Closed LI - Linearizer");
			long stop = System.currentTimeMillis();
			System.out.print("Elapsed time (s): " + ((stop - start)/(double)1000));
			System.out.println();

		} else {
			System.out.println("Wrong input!!");
		}

		start = System.currentTimeMillis();
		Linearizer mClosedLinearizerDS = new Linearizer(2, 2, pop, true);
		if (mClosedLinearizerDS.input(name, type, servt, visits)) {
			mClosedLinearizerDS.solve();
			System.out.println(mClosedLinearizerDS);
			System.out.println();
			System.out.println("End Multi Closed LI - Linearizer (DeSouza-Muntz)");
			long stop = System.currentTimeMillis();
			System.out.print("Elapsed time (s): " + ((stop - start)/(double)1000));
			System.out.println();

		} else {
			System.out.println("Wrong input!!");
		}
	}

	public static void testMultiClosedLI_big() {

		System.out.println("Closed system with 4 classes");
		long start = System.currentTimeMillis();

		String[] name = { "Station1", "Station2" };
		int[] type = { Solver.LI, Solver.LI, };
		double[][][] servt = new double[2][4][1];

		int[] pop = new int[4];
		pop[0] = 100;
		pop[1] = 100;
		pop[2] = 100;
		pop[3] = 100;

		//station 1
		servt[0][0][0] = 10;
		servt[0][1][0] = 5;
		servt[0][2][0] = 10;
		servt[0][3][0] = 5;

		//station 2
		servt[1][0][0] = 5;
		servt[1][1][0] = 9;
		servt[1][2][0] = 4;
		servt[1][3][0] = 5;

		double[][] visits = new double[2][4];

		//station 1
		visits[0][0] = 1;
		visits[0][1] = 1;
		visits[0][2] = 1;
		visits[0][3] = 1;

		//station 2
		visits[1][0] = 1;
		visits[1][1] = 1;
		visits[1][2] = 1;
		visits[1][3] = 1;

//		SolverMultiClosedMVA mClosedMVA = new SolverMultiClosedMVA(4, 2);
//		if (mClosedMVA.input(name, type, servt, visits, pop)) {
//			mClosedMVA.solve();
//			System.out.println(mClosedMVA);
//			System.out.println();
//			System.out.println("End Multi Closed LI - MVA");
//			long stop = System.currentTimeMillis();
//			System.out.print("Elapsed time (s): " + ((stop - start)/(double)1000));
//			System.out.println();
//
//		} else {
//			System.out.println("Wrong input!!");
//		}

		start = System.currentTimeMillis();
		AQL mClosedAQL = new AQL(4, 2, pop);
		if (mClosedAQL.input(name, type, servt, visits)) {
			mClosedAQL.solve();
			System.out.println(mClosedAQL);
			System.out.println();
			System.out.println("End Multi Closed LI - AQL");
			long stop = System.currentTimeMillis();
			System.out.print("Elapsed time (s): " + ((stop - start)/(double)1000));
			System.out.println();

		} else {
			System.out.println("Wrong input!!");
		}
		
		start = System.currentTimeMillis();
		Linearizer mClosedLinearizer = new Linearizer(4, 2, pop, false);
		if (mClosedLinearizer.input(name, type, servt, visits)) {
			mClosedLinearizer.solve();
			System.out.println(mClosedLinearizer);
			System.out.println();
			System.out.println("End Multi Closed LI - Linearizer");
			long stop = System.currentTimeMillis();
			System.out.print("Elapsed time (s): " + ((stop - start)/(double)1000));
			System.out.println();

		} else {
			System.out.println("Wrong input!!");
		}

		start = System.currentTimeMillis();
		Linearizer mClosedLinearizerDS = new Linearizer(4, 2, pop, true);
		if (mClosedLinearizerDS.input(name, type, servt, visits)) {
			mClosedLinearizerDS.solve();
			System.out.println(mClosedLinearizerDS);
			System.out.println();
			System.out.println("End Multi Closed LI - Linearizer (DeSouza-Muntz)");
			long stop = System.currentTimeMillis();
			System.out.print("Elapsed time (s): " + ((stop - start)/(double)1000));
			System.out.println();

		} else {
			System.out.println("Wrong input!!");
		}
}

	public static void testLinearizerExample() {

		System.out.println("Closed system with 2 classes - Linearizer example from research paper");
		long start = System.currentTimeMillis();

		String[] name = { "Station1", "Station2" };
		int[] type = { Solver.DELAY, Solver.LI, };
		double[][][] servt = new double[2][2][1];

		int[] pop = new int[2];
		pop[0] = 8;
		pop[1] = 1;

		//station 1
		servt[0][0][0] = 8;
		servt[0][1][0] = 0;

		//station 2
		servt[1][0][0] = 1;
		servt[1][1][0] = 1;

		double[][] visits = new double[2][2];

		//station 1
		visits[0][0] = 1;
		visits[0][1] = 0;

		//station 2
		visits[1][0] = 1;
		visits[1][1] = 1;

//		SolverMultiClosedMVA mClosedMVA = new SolverMultiClosedMVA(2, 2);
//		if (mClosedMVA.input(name, type, servt, visits, pop)) {
//			mClosedMVA.solve();
//			System.out.println(mClosedMVA);
//			System.out.println();
//			System.out.println("End Multi Closed LI - MVA");
//			long stop = System.currentTimeMillis();
//			System.out.print("Elapsed time (s): " + ((stop - start)/(double)1000));
//			System.out.println();
//
//		} else {
//			System.out.println("Wrong input!!");
//		}

		start = System.currentTimeMillis();
		AQL mClosedAQL = new AQL(2, 2, pop);
		if (mClosedAQL.input(name, type, servt, visits)) {
			mClosedAQL.solve();
			System.out.println(mClosedAQL);
			System.out.println();
			System.out.println("End Multi Closed LI - AQL");
			long stop = System.currentTimeMillis();
			System.out.print("Elapsed time (s): " + ((stop - start)/(double)1000));
			System.out.println();

		} else {
			System.out.println("Wrong input!!");
		}

		start = System.currentTimeMillis();
		Linearizer mClosedLinearizer = new Linearizer(2, 2, pop, false);
		if (mClosedLinearizer.input(name, type, servt, visits)) {
			mClosedLinearizer.solve();
			System.out.println(mClosedLinearizer);
			System.out.println();
			System.out.println("End Multi Closed LI - Linearizer");
			long stop = System.currentTimeMillis();
			System.out.print("Elapsed time (s): " + ((stop - start)/(double)1000));
			System.out.println();

		} else {
			System.out.println("Wrong input!!");
		}

		start = System.currentTimeMillis();
		Linearizer mClosedLinearizerDS = new Linearizer(2, 2, pop, true);
		if (mClosedLinearizerDS.input(name, type, servt, visits)) {
			mClosedLinearizerDS.solve();
			System.out.println(mClosedLinearizerDS);
			System.out.println();
			System.out.println("End Multi Closed LI - Linearizer (DeSouza-Muntz)");
			long stop = System.currentTimeMillis();
			System.out.print("Elapsed time (s): " + ((stop - start)/(double)1000));
			System.out.println();

		} else {
			System.out.println("Wrong input!!");
		}
	}

}
