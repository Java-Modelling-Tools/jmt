package jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Solver;

import cern.colt.matrix.*;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix3D;
import cern.colt.matrix.linalg.Algebra;
import cern.jet.math.Functions;
import jmt.jmva.analytical.solvers.Exceptions.InternalErrorException;
import jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.NumericalIntegration.Function;
import jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.NumericalIntegration.SafeFunction;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;

import static jmt.engine.math.GammaFun.lnGamma;
import static jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Utils.checkMatrixNonnegative;
import static jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Utils.check_inputs;
import static jmt.jmva.analytical.solvers.Utilities.MiscFunctions.exp;

public class LogisticFunctionMS extends SafeFunction {

	protected int N;
	protected int R;
	protected int K;
	protected double epsilon;
	protected DoubleMatrix2D demands;
	protected DoubleMatrix2D sigma;
	protected DoubleMatrix1D servers;
	protected DoubleMatrix1D pop;
	protected Algebra A;
	DoubleFactory2D DF2;
	DoubleFactory1D DF1;
	protected MathContext MC;
	protected ArrayList<state> States;

	/**
	 * Implements the safeEvaluate abstract method. Computes the logistic additive function
	 * at the point given by point
	 * @param point Array representing point to be evaluated
	 * @return BigDecimal equal to the function value at that point
	 */
	@Override
	protected BigDecimal safeEvaluate(double[] point) throws InternalErrorException {
		DoubleMatrix1D x = new DenseDoubleMatrix1D(point);
		DoubleMatrix1D u = logistic_transform(x);
		DoubleMatrix1D logu = u.copy();
		logu.assign(Functions.log);
		double logJac = logu.zSum();

		BigDecimal I = BigDecimal.ZERO;

		ListIterator<state> S_it = States.listIterator();
		while (S_it.hasNext()) {
			state V = S_it.next();
			BigDecimal alpha = V.coeff;

			BigDecimal J = BigDecimal.ZERO;

			ArrayList<state> Ts = V.nested_states;
			ListIterator<state> Ts_it = Ts.listIterator();
			while (Ts_it.hasNext()) {
				state T = Ts_it.next();
				BigDecimal C = T.coeff;

				double t0 = T.s.get(0);
				DoubleMatrix1D t = T.s.viewPart(1,K).copy();

				DoubleMatrix1D y = u.copy();
				y.assign(Functions.mult(t0));
				y.assign(t, Functions.plus);
				DoubleMatrix1D z = A.mult(A.transpose(sigma), y);
				z.assign(Functions.log);
				z.assign(pop, Functions.mult);

				double logI = z.zSum();

				BigDecimal K = exp(logI, MC);

				K = K.multiply(C, MC);
				J = J.add(K, MC);
			}

			J = J.multiply(alpha, MC);
			I = I.add(J, MC);
		}
		I = I.multiply(exp(logJac, MC), MC);

		return I;
	}

	@Override
	protected void checkInDomain(double[] point) throws InternalErrorException {
		return;
	}

	@Override
	public Function copy() throws InternalErrorException {
		return new LogisticFunctionMS(this);
	}

	/*================ Public utility functions =================*/
	public double[][] evaluateHessStat(double[] stat_point) throws InternalErrorException {
		DoubleMatrix1D x_stat = new DenseDoubleMatrix1D(stat_point);
		DoubleMatrix1D u_stat = new DenseDoubleMatrix1D(K);
		u_stat.viewPart(0,K-1).assign(x_stat);
		u_stat.set(K-1, 0);
		u_stat.assign(Functions.exp);
		double Z = u_stat.zSum();
		u_stat.assign(Functions.div(Z));

		BigDecimal[][] Hess = new BigDecimal[K-1][K-1];
		for (int i=0; i<K-1; i++)
			Arrays.fill(Hess[i], BigDecimal.ZERO);

		ListIterator<state> S_it = States.listIterator();
		while (S_it.hasNext()) {
			state V = S_it.next();
			BigDecimal alpha = V.coeff;

			ArrayList<state> Ts = V.nested_states;
			ListIterator<state> Ts_it = Ts.listIterator();
			while (Ts_it.hasNext()) {
				state T = Ts_it.next();
				BigDecimal C = T.coeff;

				double t0 = T.s.get(0);
				DoubleMatrix1D t = T.s.viewPart(1, K).copy();

				DoubleMatrix2D hi = calc_stat_hess_log_inside(u_stat, t0, t);

				BigDecimal Fi = calc_logisticF_inside(u_stat, t0, t);
				DoubleMatrix1D ji = calc_jac_log_inside(u_stat, t0, t);
				DoubleMatrix2D jijiT = new DenseDoubleMatrix2D(K-1,K-1);
				A.multOuter(ji,ji, jijiT);
				hi.assign(jijiT, Functions.plus);

				BigDecimal[][] Hi = toBigDecArray(hi);
				for (int i=0; i<K-1; i++) {
					for (int j=0; j<K-1; j++) {
						Hi[i][j] = Hi[i][j].multiply(Fi, MC);
						Hi[i][j] = Hi[i][j].multiply(alpha, MC);
						Hi[i][j] = Hi[i][j].multiply(C, MC);
						Hess[i][j] = Hess[i][j].add(Hi[i][j], MC);
					}
				}

			}
		}

		BigDecimal F = evaluate(stat_point);
		double[][] hess_double = new double[K-1][K-1];
		for (int i=0; i<K-1; i++) {
			for (int j=0; j<K-1; j++) {
				Hess[i][j] = Hess[i][j].divide(F, MC);
				hess_double[i][j] = Hess[i][j].doubleValue();
			}
		}

		return hess_double;
	}

	public double[] evaluateJac(double[] point) throws InternalErrorException {
		DoubleMatrix1D x = new DenseDoubleMatrix1D(point);
		DoubleMatrix1D u = logistic_transform(x);

		BigDecimal[] Jac = new BigDecimal[K-1];
		Arrays.fill(Jac, BigDecimal.ZERO);

		ListIterator<state> S_it = States.listIterator();
		while (S_it.hasNext()) {
			state V = S_it.next();
			BigDecimal alpha = V.coeff;

			ArrayList<state> Ts = V.nested_states;
			ListIterator<state> Ts_it = Ts.listIterator();
			while (Ts_it.hasNext()) {
				state T = Ts_it.next();
				BigDecimal C = T.coeff;

				double t0 = T.s.get(0);
				DoubleMatrix1D t = T.s.viewPart(1,K).copy();

				DoubleMatrix1D hi = calc_jac_log_inside(u, t0, t);
				BigDecimal Fi = calc_logisticF_inside(u, t0, t);
				BigDecimal[] Hi = toBigDecArray(hi);
				for (int i=0; i<Hi.length; i++) {
					Hi[i] = Hi[i].multiply(Fi, MC);
					Hi[i] = Hi[i].multiply(alpha, MC);
					Hi[i] = Hi[i].multiply(C, MC);
					Jac[i] = Jac[i].add(Hi[i], MC);
				}
			}
		}

		BigDecimal F = evaluate(point);
		double[] jac_double = new double[Jac.length];
		for (int i=0; i<Jac.length; i++) {
			Jac[i] = Jac[i].divide(F, MC);
			jac_double[i] = Jac[i].doubleValue();
		}

		return jac_double;

	}

	/*================ Constructors =================*/
	public LogisticFunctionMS(int ndim, int[] population, double[][] demands, double[] servers, double epsilon) throws InternalErrorException {
		this(ndim, population, demands, servers, epsilon, MathContext.DECIMAL128);
	}

	public LogisticFunctionMS(int ndim, DoubleMatrix1D population, DoubleMatrix2D demands, DoubleMatrix1D servers, double epsilon) throws InternalErrorException {
		this(ndim, population, demands, servers, epsilon, MathContext.DECIMAL128);
	}

	public LogisticFunctionMS(int ndim, DoubleMatrix1D population, DoubleMatrix2D demands, DoubleMatrix1D servers, double epsilon, MathContext MC) throws InternalErrorException {
		super(ndim);
		this.initialize(ndim, population, demands, servers, epsilon, MC);
	}

	public LogisticFunctionMS(int ndim, int[] population, double[][] demands, double[] servers, double epsilon, MathContext MC) throws InternalErrorException {
		super(ndim);

		/*convert array into matrix objects*/
		DoubleMatrix1D pop_temp;
		DoubleMatrix2D demands_temp;
		DoubleMatrix1D servers_temp;

		try {
			pop_temp = new DenseDoubleMatrix1D(population.length);
			for (int i=0; i<population.length; i++)
				pop_temp.set(i, (double)population[i]);
			demands_temp = new DenseDoubleMatrix2D(demands);
			servers_temp = new DenseDoubleMatrix1D(servers);
		} catch (IllegalArgumentException e) {
			throw new InternalErrorException("Demands needs to be a rectangular 2D array!");
		}

		this.initialize(ndim, pop_temp, demands_temp, servers_temp, epsilon, MC);
	}

	/*Copy constructor*/
	public LogisticFunctionMS(LogisticFunctionMS LF) throws InternalErrorException {
		this(LF.ndim, LF.pop, LF.demands, LF.servers, LF.epsilon, LF.MC);
	}

	private void initialize(int ndim, DoubleMatrix1D population, DoubleMatrix2D demands, DoubleMatrix1D servers, double epsilon, MathContext MC) throws InternalErrorException {
		this.A = new Algebra();
		DF2 = DoubleFactory2D.dense;
		DF1 = DoubleFactory1D.dense;
		this.MC = MC;

		this.pop = population.copy();
		this.demands = demands.copy();
		this.servers = servers.copy();
		this.epsilon = epsilon;

		if ((ndim != this.demands.rows()-1) || (ndim != this.servers.size()-1))
			throw new InternalErrorException("Number of integrated dimensions must be one less than number of queues!");

		check_inputs(this.pop, this.demands, this.epsilon);

		this.N = (int)this.pop.zSum();
		this.K = this.demands.rows();
		this.R = this.demands.columns();

		this.sigma = new DenseDoubleMatrix2D(K,R);
		for (int k=0; k<K; k++) {
			sigma.viewRow(k).assign(demands.viewRow(k));
			sigma.viewRow(k).assign(Functions.div(servers.get(k)));
		}

		checkMatrixNonnegative(servers);

		/* initialize and cache the states for the summation and differences
		 * and cache the coefficients too */
		States = new ArrayList<>();

		// set up the final and initial states for the sum
		int[] v = new int[K]; Arrays.fill(v, 0);

		int P = (int)pop.zSum();

		int[] s_1 = new int[K];
		for (int i=0; i<K; i++)
			s_1[i] = (int)servers.get(i)-1;

		while (v.length>0) {
			int V = 0;
			for (int i : v)
				V += i;

			if (P-V < 0) {
				v = next_state(v, s_1);
				continue;
			}

			double beta = 0;
			for (int i=0; i<K; i++) {
				beta += v[i] * Math.log(servers.get(i));
				beta -= lnGamma(v[i]+1);
				beta += Math.log(1-(double)v[i]/servers.get(i));
			}

			beta += lnGamma(P+K-V) - lnGamma(P-V+1);
			BigDecimal alpha = exp( beta, MC);

			States.add(new state(new DenseDoubleMatrix1D(ints2doubles(v)) ,alpha, true));

			// set up the final and initial states for the differences
			int[] Tfinal = new int[K+1];
			Tfinal[0] = P-V;
			for (int i=0; i<K; i++) {
				Tfinal[i+1] = v[i];
			}
			int[] T = new int[K+1];
			Arrays.fill(T,0);

			while (T.length>0) {
				double C = Math.pow(-1, sum_int(Tfinal) - sum_int(T));
				for (int i=0; i<K+1; i++)
					C *= binomial(Tfinal[i], T[i]);

				if (sum_int(T)==0) {
					T = next_state(T, Tfinal);
					continue;
				}

				States.get(States.size()-1).add_nested_state(
						new state(new DenseDoubleMatrix1D(ints2doubles(T)), new BigDecimal(C), false));
				T = next_state(T, Tfinal);
			}

			v = next_state(v, s_1);
		}
	}

	/*================ Non-static helper classes/ methods =================*/
	private class state {

		private DoubleMatrix1D s;
		private BigDecimal coeff;
		private ArrayList<state> nested_states;

		state(DoubleMatrix1D s, BigDecimal coeff, boolean nest) {
			this.s = s;
			this.coeff = coeff;
			if (nest)
				nested_states = new ArrayList<state>();
		}

		void add_nested_state(state s) {
			nested_states.add(s);
		}

	}

	private DoubleMatrix1D calc_jac_log_inside(DoubleMatrix1D u, double t0, DoubleMatrix1D t) {
		DoubleMatrix1D zeta = u.copy();
		zeta.assign(Functions.mult(t0));
		zeta.assign(t, Functions.plus);
		zeta = A.mult(A.transpose(sigma), zeta);
		zeta.assign(pop, Functions.div);
		zeta.assign(Functions.inv);

		DoubleMatrix1D w = A.mult(sigma, zeta);
		w.assign(u, Functions.mult);
		w.assign(Functions.mult(t0));

		DoubleMatrix1D M = u.copy();
		M = A.mult(A.transpose(sigma), M);
		double m = A.mult(zeta, M);
		m *= t0;
		M = u.copy();
		M.assign(Functions.mult(m));

		DoubleMatrix1D P = u.copy();
		P.assign(Functions.mult(K));

		DoubleMatrix1D Jac = new DenseDoubleMatrix1D(K);
		Jac.assign(1);
		Jac.assign(w, Functions.plus);
		Jac.assign(P, Functions.minus);
		Jac.assign(M, Functions.minus);

		return Jac.viewPart(0,K-1);
	}

	private DoubleMatrix2D calc_stat_hess_log_inside(DoubleMatrix1D u_stat, double t0, DoubleMatrix1D t) {
		DoubleMatrix1D zeta = u_stat.copy();
		zeta.assign(Functions.mult(t0));
		zeta.assign(t, Functions.plus);
		zeta = A.mult(A.transpose(sigma), zeta);
		zeta.assign(pop, Functions.div);
		zeta.assign(Functions.inv);
		DoubleMatrix1D zeta2 = zeta.copy();
		zeta2.assign(Functions.pow(2));

		DoubleMatrix2D Hess = new DenseDoubleMatrix2D(K,K);
		for (int i=0; i<K; i++) {
			for (int j=0; j<K; j++) {
				DoubleMatrix1D g = zeta2.copy();
				g.assign(Functions.neg);
				g.assign(pop, Functions.div);
				g.assign(Functions.mult(Math.pow(t0,2)));
				g.assign(sigma.viewRow(j), Functions.mult);
				g.assign(sigma.viewRow(i), Functions.mult);

				Hess.set(i,j,g.zSum());
				if (i==j)
					Hess.set(i,j, Hess.get(i,j)-Math.pow(u_stat.get(j),-2));
			}
		}

		DoubleMatrix2D Jux = jac_ux(u_stat);
		Hess = A.mult(Hess, Jux);
		Hess = A.mult(A.transpose(Jux), Hess);

		//contribution from 1st derivative (not necessarily zero)
		DoubleMatrix1D Jac = A.mult(sigma, zeta);
		Jac.assign(Functions.mult(t0));
		DoubleMatrix1D u_stat_inv = u_stat.copy();
		u_stat_inv.assign(Functions.inv);
		Jac.assign(u_stat_inv, Functions.plus);

		DoubleMatrix3D Juxx = jac_uxx(u_stat);

		DoubleMatrix2D J = new DenseDoubleMatrix2D(K-1, K-1);
		for (int i=0; i<K-1; i++)
			for (int j=0; j<K-1; j++) {
				DoubleMatrix2D SlicesRows = Juxx.viewColumn(j);
				DoubleMatrix1D Slices = SlicesRows.viewColumn(j);
				double h = A.mult(Jac, Slices);
				J.set(i,j,h);
			}

		Hess.assign(J, Functions.plus);

		return Hess;
	}

	private BigDecimal calc_logisticF_inside(DoubleMatrix1D u, double t0, DoubleMatrix1D t) throws InternalErrorException {
		DoubleMatrix1D g = u.copy();
		g.assign(Functions.mult(t0));
		g.assign(t, Functions.plus);
		g = A.mult(A.transpose(sigma), g);
		g.assign(Functions.log);
		double h = A.mult(pop, g);

		DoubleMatrix1D logu = u.copy();
		logu.assign(Functions.log);
		h += logu.zSum();

		if (Double.isInfinite(h) || Double.isNaN(h))
			return BigDecimal.ZERO;

		return exp(h, MC);
	}

	private DoubleMatrix2D jac_ux(DoubleMatrix1D u) {
		DoubleMatrix2D u_diag = DF2.diagonal(u.copy());
		DoubleMatrix2D uTu = new DenseDoubleMatrix2D(K, K);
		A.multOuter(u,u, uTu);
		u_diag.assign(uTu, Functions.minus);
		return u_diag.viewPart(0,0, K, K-1);
	}

	private DoubleMatrix3D jac_uxx(DoubleMatrix1D u) {
		DoubleMatrix3D J = new DenseDoubleMatrix3D(K, K-1,K-1);
		for (int p=0; p<K; p++)
			for (int i=0; i<K-1; i++)
				for (int j=0; j<K-1; j++) {
					double h = kr_delta(p,j)*kr_delta(p,i)*u.get(p);
					h -= kr_delta(p,j)*u.get(p)*u.get(i);
					h -= kr_delta(i,j)*u.get(p)*u.get(j);
					h += 2*u.get(p)*u.get(i)*u.get(j);
					h -= kr_delta(p,i)*u.get(p)*u.get(j);
					J.set(p,i,j,h);
				}
		return J;
	}

	private int kr_delta(int i, int j) {
		if (i==j)
			return 1;
		return 0;
	}

	private DoubleMatrix1D logistic_transform(DoubleMatrix1D x) throws InternalErrorException {
		DoubleMatrix1D u = new DenseDoubleMatrix1D(K);
		BigDecimal[] U = new BigDecimal[K];
		BigDecimal Z = BigDecimal.ZERO;
		for (int i=0; i<(K-1); i++) {
			U[i] = exp(x.get(i), MC);
			Z = Z.add(U[i], MC);
		}
		Z = Z.add(BigDecimal.ONE, MC);
		for (int i=0; i<(K-1); i++) {
			BigDecimal ui = U[i];
			ui = ui.divide(Z, MC);
			u.set(i, ui.doubleValue());
		}
		u.set(K-1, BigDecimal.ONE.divide(Z, MC).doubleValue());
		return u;
	}


	/*================ Static helper functions =============== */
	private static int sum_int(int[] int_array) {
		int sum = 0;
		for (int i : int_array)
			sum += i;
		return sum;
	}

	private static long binomial(int n, int k) {
		if (k>n-k)
			k=n-k;

		long b=1;
		for (int i=1, m=n; i<=k; i++, m--)
			b=b*m/i;
		return b;
	}

	private static int[] next_state(int[] prev_state, int[] final_state) {
		int[] next = prev_state.clone();
		int D = final_state.length;
		int d = D-1;
		while ((d>-1) && next[d]==final_state[d]) {
			next[d] = 0;
			d -= 1;
		}

		if (d==-1) {
			return new int[0];
		}

		next[d] += 1;
		return next;
	}

	private static double[] ints2doubles(int[] ints) {
		double[] doubles = new double[ints.length];
		for (int i=0; i<ints.length; i++)
			doubles[i] = ints[i];
		return doubles;
	}

	private static BigDecimal[] toBigDecArray(DoubleMatrix1D a) {
		BigDecimal[] BDA = new BigDecimal[a.size()];
		for (int i=0; i<a.size(); i++)
			BDA[i] = new BigDecimal(a.get(i));
		return BDA;
	}

	private static BigDecimal[][] toBigDecArray(DoubleMatrix2D a) {
		BigDecimal[][] BDA = new BigDecimal[a.size()][a.size()];
		for (int i=0; i<a.rows(); i++)
			for (int j=0; j<a.columns(); j++)
				BDA[i][j] = new BigDecimal(a.get(i,j));
		return BDA;
	}

}
