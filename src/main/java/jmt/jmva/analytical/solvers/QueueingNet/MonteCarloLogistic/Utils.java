package jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import jmt.jmva.analytical.solvers.SolverMulti;
import jmt.jmva.analytical.solvers.DataStructures.QNModel;
import jmt.jmva.analytical.solvers.Exceptions.InternalErrorException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;

public class Utils {

	private static Algebra A = new Algebra();

	/*============= Utility functions for pre-processing models ============*/
	public static DoubleMatrix2D sanitize_demands(DoubleMatrix2D demands) {
		ArrayList<Integer> nonzero_rows = new ArrayList<Integer>();
		//remove rows
		for (int i=0; i<demands.rows(); i++) {
			DoubleMatrix1D row = demands.viewRow(i).copy();
			if (row.zSum()==0)
				continue;
			nonzero_rows.add(i);
		}

		/* need to leave at least one row for demand matrix, otherwise integration domain dimensionality is -1 */
		if (nonzero_rows.size()==0)
			nonzero_rows.add(0);

		DoubleMatrix2D demands_s = new DenseDoubleMatrix2D(nonzero_rows.size(), demands.columns());
		int r;
		for (int i=0; i<demands_s.rows(); i++) {
			for (int j=0; j<demands_s.columns(); j++) {
				r = nonzero_rows.get(i);
				demands_s.set(i, j, demands.get(r,j));
			}
		}
		return demands_s;
	}

	public static void check_inputs(DoubleMatrix1D population, DoubleMatrix2D demands, double epsilon) throws InternalErrorException {
		if (population.size()!=demands.columns())
			throw new InternalErrorException("Number of classes in population has to be the same as demand matrix columns!");
		if (population.size()==0)
			throw new InternalErrorException("Cannot have empty model!");
		if (!checkMatrixNonnegative(population))
			throw new InternalErrorException("Class populations cannot be negative!");
		if (!checkMatrixNonnegative(demands))
			throw new InternalErrorException("Server demands cannot be negative!");
		if (epsilon<0)
			throw new InternalErrorException("Epsilon value cannot be negative!!");
	}

	public static void check_inputs(DoubleMatrix1D population, DoubleMatrix2D demands, DoubleMatrix1D delays, double epsilon) throws InternalErrorException {
		check_inputs(population, demands, epsilon);
		if (population.size()!=delays.size())
			throw new InternalErrorException("Number of delays must be equal to number of classes!");
		if (!checkMatrixNonnegative(delays))
			throw new InternalErrorException("Class delays cannot be negative!");
	}

	public static void check_inputs(DoubleMatrix1D population, DoubleMatrix2D demands, DoubleMatrix1D serv, DoubleMatrix1D delays, double epsilon) throws InternalErrorException {
		check_inputs(population, demands, delays, epsilon);
		if (serv.size()!=demands.rows())
			throw new InternalErrorException("Number of servers wrongly specified!");
		if (!checkMatrixNonzero(serv))
			throw new InternalErrorException("Number of servers must be larger than 1!");
	}

	public static boolean isInvalidModel(DoubleMatrix2D demands, DoubleMatrix1D delays) {
		for (int i=0; i<demands.columns(); i++) {
			DoubleMatrix1D col = demands.viewColumn(i).copy();
			if ((col.zSum() + delays.get(i))==0) { //if delays and demands are zero
				return true;
			}
		}
		return false;
	}

	public static boolean checkMatrixNonnegative(DoubleMatrix1D V) {
		for (int i=0; i<V.size(); i++)
			if (V.get(i) < 0)
				return false;
		return true;
	}

	public static boolean checkMatrixNonnegative(DoubleMatrix2D V) {
		for (int i=0; i<V.rows(); i++)
			for (int j=0; j<V.columns(); j++)
				if (V.get(i,j) < 0)
					return false;
		return true;
	}

	public static boolean checkMatrixNonzero(DoubleMatrix1D V) {
		for (int i=0; i<V.size(); i++)
			if (V.get(i) < 1)
				return false;
		return true;
	}

	public static double[][][] getDemandsFromQNM(QNModel qnm) {
		double[][][] demands = new double[qnm.M][qnm.R][0];
		for (int k=0; k<qnm.M; k++) {
			for (int r=0; r<qnm.R; r++) {
				int Nr = qnm.getPopulationVector().get(r);
				int c = qnm.getNumberOfServers(k);
				demands[k][r] = new double[Nr];
				for (int n=0; n<Nr; n++)
					demands[k][r][n] = qnm.getDemand(k,r)/Math.min(c,n+1);
			}
		}
		return demands;
	}

	/*============= Utility functions for performance index calculations ============*/
	public static double[][] augmentDemandsAtServer(int K, int R, double[][] demands, int s) {
		double[][] demands_plus = new double[K+1][R];
		for (int k=0; k<K; k++) {
			for (int r=0; r<R; r++) {
				demands_plus[k][r] = demands[k][r];
			}
		}
		for (int r=0; r<R; r++) {
			demands_plus[K][r] = demands[s][r];
		}
		return demands_plus;
	}

	public static int[] augmentSeverCountAtServer(int K, int R, int[] serv, int s) {
		int[] serv_plus = new int[K+1];
		for (int k=0; k<K; k++) {
			serv_plus[k] = serv[k];
		}
		serv_plus[K] = serv[s];
		return serv_plus;
	}

	public static double[][] removeServer(int K, int R, double[][] demands, int s) {
		double[][] demands_minus = new double[K-1][R];
		int i=0;
		for (int k=0; k<K; k++) {
			if (k==s)
				continue;
			for (int r=0; r<R; r++) {
				demands_minus[i][r] = demands[k][r];
			}
			i++;
		}
		return demands_minus;
	}

	public static int[] removeSeverCountAtServer(int K, int R, int[] serv, int s) {
		int[] serv_minus = new int[K-1];
		int i=0;
		for (int k=0; k<K; k++) {
			if (k==s)
				continue;
			serv_minus[i] = serv[k];
			i++;
		}
		return serv_minus;
	}

	public static int count_num_servs(double[] s, int[] pop) throws InternalErrorException {
		int m = (int)Math.round(s[0]/s[s.length-1]); // calculate m/i
		for (int i=0; i<s.length; i++) { // check all demands
			double si = s[i];
			double si_correct = s[0]/Math.min(i+1,m);
			if (!(Math.abs((si-si_correct)/si_correct) < 1e-03))
				throw new InternalErrorException("Invalid multi-server model!");
		}
		return m;
	}

	public static QNModel arrays2qnm(int[] t, double[][][] s, double[][] v, int[] pop) throws InternalErrorException {
		int K = s.length;
		int R = s[0].length;

		double[][] demands = new double[K][R];
		int[] num_stations = new int[K];

		int N = 0;
		for (int r=0; r<R; r++)
			N += pop[r];

		for (int k=0; k<K; k++) {
			if (t[k]==SolverMulti.LD) {
				int num_station = count_num_servs(s[k][0], pop);
				for (int r = 1; r < R; r++) {
					int num_station_r = count_num_servs(s[k][r], pop);
					if (num_station_r!=num_station)
						throw new InternalErrorException("Invalid multi-server model!");
				}
				num_stations[k] = num_station;
			} else if (t[k]==SolverMulti.LI) {
				num_stations[k] = 1;
			} else if (t[k]==SolverMulti.DELAY) {
				num_stations[k] = N;
			}

			for (int r=0; r<R; r++) {
				demands[k][r] = v[k][r]*s[k][r][0];
			}
		}

		Integer[] Mult = new Integer[K];
		Arrays.fill(Mult,1);
		double[] Z = new double[R];
		Arrays.fill(Z,0);
		Integer[] popInt = new Integer[R];
		for (int r=0; r<R; r++)
			popInt[r] = pop[r];

		return new QNModel(R, K, t, popInt, Z, Mult, demands, num_stations);
	}

	public static int[] pprod(int[] N) {
		int[] state = new int[N.length];
		Arrays.fill(state, 0);
		return state;
	}

	public static boolean pprod(int[] n, int[] N) {
		int L = N.length;

		int sum = 0;
		for(int i=0; i<L; i++)
			if(n[i]==N[i])
				sum++;
		if (sum==L)
			return false;

		int s = L-1;
		while (s>-1 && n[s]==N[s]) {
			n[s] = 0;
			s--;
		}
		if(s==-1)
			return false;
		n[s]++;
		return true;
	}

	/*===================== static tensor operator functions ===================== */
	// See report (Ong 2018) for definitions
	public static double beta(DoubleMatrix1D u, int i) {
		double b = u.get(i);
		double c = 1- u.viewPart(0,i).zSum();
		b = b/c;
		return b;
	}

	public static double alpha(int i, int j) {
		if (i>j)
			return 0;
		return 1;
	}

	/**
	 * Well-known kronecker delta operator
	 * @param i
	 * @param j
	 * @return
	 */
	public static double delta(int i, int j) {
		if (i==j)
			return 1;
		return 0;
	}

	/*================ Logistic transforms =============== */
	public static DoubleMatrix1D logistic_transform_add(DoubleMatrix1D u) {
		int K = u.size();
		DoubleMatrix1D x = new DenseDoubleMatrix1D(K-1);
		for (int i=0; i<K-1; i++) {
			x.set(i, Math.log(u.get(i)/u.get(K-1)));
		}
		return x;
	}

	public static DoubleMatrix1D logistic_transform_add(DoubleMatrix1D u, double v) {
		int K = u.size();
		DoubleMatrix1D x = new DenseDoubleMatrix1D(K);
		DoubleMatrix1D w = logistic_transform_add(u);
		x.set(0, Math.log(v));
		x.viewPart(1,K-1).assign(w);
		return x;
	}

	public static DoubleMatrix1D logistic_transform_mult(DoubleMatrix1D u) {
		int K = u.size();
		DoubleMatrix1D x = new DenseDoubleMatrix1D(K-1);
		for (int i=0; i<K-1; i++) {
			double denom = 1 - u.viewPart(0,i+1).zSum();
			x.set(i, Math.log(u.get(i)/denom));
		}
		return x;
	}

	public static DoubleMatrix1D logistic_transform_mult(DoubleMatrix1D u, double v) {
		int K = u.size();
		DoubleMatrix1D x = new DenseDoubleMatrix1D(K);
		DoubleMatrix1D w = logistic_transform_mult(u);
		x.set(0, Math.log(v));
		x.viewPart(1,K-1).assign(w);
		return x;
	}

	public static DoubleMatrix2D compute_JacobianAdd2Mult(DoubleMatrix1D u) {
		int K = u.size();
		DoubleMatrix2D xu = new DenseDoubleMatrix2D(K-1,K);
		DoubleMatrix2D uy = new DenseDoubleMatrix2D(K,K-1);

		for (int i=0; i<K-1; i++)
			for (int k=0; k<K; k++)
				xu.set(i,k, delta(i,k)/u.get(i) - delta(k, K-1)/u.get(K-1));

		for (int k=0; k<K; k++)
			for (int j=0; j<K-1; j++)
				uy.set(k, j, delta(k,j)*u.get(k) - u.get(k)*beta(u,j)*alpha(j,k));

		return A.mult(xu, uy);
	}

	public static DoubleMatrix2D compute_JacobianAdd2Mult(DoubleMatrix1D u, double v) {
		int K = u.size();
		DoubleMatrix2D J = new DenseDoubleMatrix2D(K, K);
		J.assign(0);
		J.set(0,0,1);
		J.viewPart(1,1,K-1,K-1).assign(compute_JacobianAdd2Mult(u));
		return J;
	}

	/*============= Utility functions for experiments ============*/
	public static double getNCError(QNModel exp_qnm, QNModel ans_qnm) {
		BigDecimal DNC = exp_qnm.getNormalisingConstant().asBigDecimal();
		DNC = DNC.subtract(ans_qnm.getNormalisingConstant().asBigDecimal(), MathContext.DECIMAL128);
		if (exp_qnm.getNormalisingConstant().asBigDecimal().doubleValue() == 0)
			return 1e-05;
		DNC = DNC.divide(exp_qnm.getNormalisingConstant().asBigDecimal(), MathContext.DECIMAL128);
		return DNC.doubleValue();
	}

	public static double getThroughputError(QNModel qnm0, QNModel qnm1) {
		double S=0., e=0., E=0.;
		for (int r=0; r<qnm0.R; r++) {
			e += Math.abs(qnm0.getMeanThroughputsAsDoubles()[r] - qnm1.getMeanThroughputsAsDoubles()[r]);
			S += Math.abs(qnm0.getMeanThroughputsAsDoubles()[r]);
		}
		E += e/S;
		return E;
	}

	public static double getQueueLengthError(QNModel qnm0, QNModel qnm1) {
		double S=0., e=0., E=0.;
		for (int k=0; k<qnm0.M; k++) {
			for (int r=0; r<qnm0.R; r++) {
				if (qnm0.getMeanQueueLengthsAsDoubles()[k][r] == 0.)
					continue;
				e += Math.abs(qnm0.getMeanQueueLengthsAsDoubles()[k][r] - qnm1.getMeanQueueLengthsAsDoubles()[k][r]);
				S += Math.abs(qnm0.getMeanQueueLengthsAsDoubles()[k][r]);
			}
		}
		E += e/S;
		return E;
	}

}
