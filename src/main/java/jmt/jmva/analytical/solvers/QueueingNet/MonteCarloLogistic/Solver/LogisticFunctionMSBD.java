package jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Solver;

import cern.colt.matrix.*;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.jet.math.Functions;
import jmt.jmva.analytical.solvers.Exceptions.InternalErrorException;
import jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.NumericalIntegration.Function;
import jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Utils;
import org.apache.commons.math3.analysis.DifferentiableMultivariateFunction;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.optimization.general.ConjugateGradientFormula;
import org.apache.commons.math3.optimization.general.NonLinearConjugateGradientOptimizer;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;

import static jmt.engine.math.GammaFun.lnGamma;
import static jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Utils.checkMatrixNonnegative;
import static jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Utils.check_inputs;
import static jmt.jmva.analytical.solvers.Utilities.MiscFunctions.exp;
import static jmt.jmva.analytical.solvers.Utilities.MiscFunctions.log;

/**
 * Implementation of the Multiserver Logistic Function, as given in Ong 2018:
 * Java Algorithms for Computer Performance Analysis. Uses Method 2, with the
 * Sum and Difference operators inside
 */
public class LogisticFunctionMSBD extends LogisticFunctionBase {

	// Model parameters
	protected int N;
	protected int R;
	protected int K;
	protected double epsilon;
	protected DoubleMatrix2D demands;
	protected DoubleMatrix2D sigma;
	protected DoubleMatrix1D servers;
	protected DoubleMatrix1D pop;
	// Misc
	protected Algebra A;
	DoubleFactory2D DF2;
	DoubleFactory1D DF1;
	protected MathContext MC;
	// Cached list of states in the summation and difference operators
	protected ArrayList<state> States;

	/**
	 * Implements the safeEvaluate abstract method. Computes the logistic additive function
	 * at the point given by point, for multiserver networks e^(k(x))
	 * @param point Array representing point to be evaluated
	 * @return BigDecimal equal to the function value at that point
	 */
	@Override
	protected BigDecimal safeEvaluate(double[] point) throws InternalErrorException {
		DoubleMatrix1D x = new DenseDoubleMatrix1D(point);
		BigDecimal[] U = logistic_transform(x);

		BigDecimal I = BigDecimal.ZERO;

		ListIterator<state> S_it = States.listIterator();

		while (S_it.hasNext()) { // iterate over the summation terms, V
			state V = S_it.next();
			BigDecimal alpha = V.coeff;

			BigDecimal J = BigDecimal.ZERO;

			ArrayList<state> Ts = V.nested_states;
			ListIterator<state> Ts_it = Ts.listIterator();

			while (Ts_it.hasNext()) { // iterate over the difference terms, T
				state T = Ts_it.next();
				BigDecimal C = T.coeff;

				double t0 = T.s.get(0);
				DoubleMatrix1D t = T.s.viewPart(1,K).copy();

				BigDecimal H = calc_logisticF_inside(U, t0, t); // Compute h(u, t0, t)
				H = H.multiply(C, MC); // Multiply binomial coefficients in difference operators
				J = J.add(H, MC);
			}

			J = J.multiply(alpha, MC); // alpha coefficient in the summation
			I = I.add(J, MC);
		}

		return I;
	}

	@Override
	protected void checkInDomain(double[] point) throws InternalErrorException {
		return;
	}

	@Override
	public Function copy() throws InternalErrorException {
		return new LogisticFunctionMSBD(this);
	}

	/*================ Public utility functions =================*/

	/**
	 * Stationary point calculation for multi-server networks - requires
	 * CG descent. Requires the implementation of a gradient MultivariateVectorFunction
	 * and a partial gradient MultivariateFunction to be given into the optimizer class.
	 * These will then be used in the nonlinear CG algorithm to calculate the direction
	 * of descent
	 * @throws InternalErrorException
	 */
	public void calculate_stationary_point() throws InternalErrorException {
		/**
		 * Implementation of gradient vector function, contains a replica of this Logistic Function object
		 */
		class LFMS1D_grad implements MultivariateVectorFunction {

			private LogisticFunctionMSBD LFMS;

			public LFMS1D_grad(int ndim, DoubleMatrix1D population, DoubleMatrix2D demands, DoubleMatrix1D servers, double epsilon) throws InternalErrorException {
				LFMS = new LogisticFunctionMSBD(ndim, population, demands, servers, epsilon);
			}

			/**
			 * Calculated the gradient of the associated differentiable function as a given point doubles
			 * @param doubles
			 * @return
			 */
			@Override
			public double[] value(double[] doubles) {
				double[] x = new double[doubles.length];
				for (int i=0; i<doubles.length; i++)
					x[i] = doubles[i];

				try {
					double[] J = LFMS.evaluateJac(x);
					for (int i=0; i<J.length; i++)
						J[i] *= -1;
					return J;
				} catch (ArithmeticException | NumberFormatException e) {
					System.err.println("Warning: Bad value (possibly negative) encountered when evaluating multiserver " +
							"logistic function! Try setting a higher precsion! Continuing ... ");
					double[] J = new double[x.length];
					Arrays.fill(J, 0.);
					return J;
				} catch (Exception e) {
					System.err.println("Warning: Unknown error encountered when evaluating multiserver logistic function! Continuing ... ");
					double[] J = new double[x.length];
					Arrays.fill(J, 0.);
					return J;
				}
			}
		}

		/**
		 * Implementation of partial gradient vector function, contains a replica of this Logistic Function object
		 */
		class LFMS1D_partgrad implements MultivariateFunction {
			private LogisticFunctionMSBD LFMS;
			private int k;
			public LFMS1D_partgrad(int ndim, DoubleMatrix1D population, DoubleMatrix2D demands, DoubleMatrix1D servers, double epsilon, int k) throws InternalErrorException {
				LFMS = new LogisticFunctionMSBD(ndim, population, demands, servers, epsilon);
				this.k = k;
			}

			@Override
			public double value(double[] doubles) {
				double[] x = new double[doubles.length];
				for (int i=0; i<doubles.length; i++)
					x[i] = doubles[i];

				try {
					double[] J = LFMS.evaluateJac(x);
					return -J[k];
				} catch (ArithmeticException | NumberFormatException e) {
					System.err.println("Warning: Bad value (possibly negative) encountered when evaluating multiserver " +
							"logistic function! Try setting a higher precsion! Continuing ... ");
					return 0.;
				} catch (Exception e) {
					System.err.println("Warning: Unknown error encountered when evaluating multiserver logistic function! Continuing ... ");
					return 0.;
				}
			}
		}

		/**
		 * Implementation of a DifferentiableMultivariateFunction as required by the CG optimizer,
		 * contains a replica of this Logistic Function object
		 */
		class LFMS1D_diff implements DifferentiableMultivariateFunction {

			private LogisticFunctionMSBD LFMS;
			private int ndim;
			private DoubleMatrix1D pop;
			private DoubleMatrix2D demands;
			private DoubleMatrix1D servers;
			private double epsilon;

			public LFMS1D_diff(int ndim, DoubleMatrix1D population, DoubleMatrix2D demands, DoubleMatrix1D servers, double epsilon) throws InternalErrorException {
				this.ndim = ndim;
				this.pop = population;
				this.demands = demands;
				this.servers = servers;
				this.epsilon = epsilon;
				LFMS = new LogisticFunctionMSBD(ndim, population, demands, servers, epsilon);
			}

			/**
			 * Calculates the value at a given point
			 * @param doubles
			 * @return
			 */
			@Override
			public double value(double[] doubles) {
				double[] x = new double[doubles.length];
				for (int i=0; i<doubles.length; i++)
					x[i] = doubles[i];

				try {
					BigDecimal F = LFMS.evaluate(x);
					F = log(F, MathContext.DECIMAL128);
					return -F.doubleValue();
				} catch (ArithmeticException | NumberFormatException e) {
					System.err.println("Warning: Bad value (possibly negative) encountered when evaluating multiserver " +
							"logistic function! Try setting a higher precsion! Continuing ... ");
					return 0.;
				} catch (Exception e) {
					System.err.println("Warning: Unknown error encountered when evaluating multiserver logistic function! Continuing ... ");
					return 0.;
				}
			}

			/**
			 * Returns the gradient MVF instance
			 * @return
			 */
			@Override
			public MultivariateVectorFunction gradient() {
				try {
					return new LFMS1D_grad(ndim, pop, demands, servers, epsilon);
				} catch (Exception e) {
					return null;
				}
			}

			/**
			 * Returns the partial gradient function instance
			 * @param i
			 * @return
			 */
			@Override
			public MultivariateFunction partialDerivative(int i) {
				try {
					return new LFMS1D_partgrad(ndim, pop, demands, servers, epsilon, i);
				} catch (Exception e) {
					return null;
				}
			}
		}

		// Single station case - trivial
		if (K==1) {
			u_stat = new DenseDoubleMatrix1D(new double[]{1.});
			x_stat = new DenseDoubleMatrix1D(new double[]{});
			return;
		}

		// Initialize optimizer
		NonLinearConjugateGradientOptimizer optimizer =
				new NonLinearConjugateGradientOptimizer(ConjugateGradientFormula.POLAK_RIBIERE);

		double[] start = new double[K-1];
		Arrays.fill(start,0);

		// Attempt optimization
		DenseDoubleMatrix1D x_opt;
		try {
			PointValuePair optimum = optimizer.optimize(1000, new LFMS1D_diff(K - 1, pop, demands, servers, epsilon), GoalType.MINIMIZE, start);
			x_opt = new DenseDoubleMatrix1D(optimum.getPoint());
		} catch (TooManyEvaluationsException e) { //failed
			throw new InternalErrorException(" Gradient Descent Optimizer Failed for Multiserver Logistic Sampling Algorithm! Try using more preceision!");
		}

		x_stat = x_opt.copy();
		u_stat = new DenseDoubleMatrix1D(K);
		u_stat.viewPart(0,K-1).assign(x_opt);
		u_stat.set(K-1, 0);
		u_stat.assign(Functions.exp);
		double Z = u_stat.zSum();
		u_stat.assign(Functions.div(Z));
	}

	/**
	 * Stationary point Hessian calculations for multi-server networks
	 * @throws InternalErrorException
	 */
	public void calculate_hessian()throws InternalErrorException {
		DoubleMatrix1D x_opt = this.u_stat.viewPart(0,this.K-1).copy();
		x_opt.assign(Functions.div(this.u_stat.get(this.K-1)));
		x_opt.assign(Functions.log);

		hess_stat = new DenseDoubleMatrix2D(evaluateHessStat(x_opt.toArray()));
		hess_stat.assign(Functions.mult(-1));
	}

	/**
	 * Evaluate the Hessian of k(x) at the stationary point, stat_point.
	 * Will be correct iff stat-point is the correct stationary point
	 * @param stat_point stationry point to provide
	 * @return double[][] containing the Hessian of log(k(x))
	 * @throws InternalErrorException
	 */
	public double[][] evaluateHessStat(double[] stat_point) throws InternalErrorException {
		BigDecimal[] Ustat = logistic_transform(new DenseDoubleMatrix1D(stat_point));

		BigDecimal[][] Hess = new BigDecimal[K-1][K-1];
		for (int i=0; i<K-1; i++)
			Arrays.fill(Hess[i], BigDecimal.ZERO);

		ListIterator<state> S_it = States.listIterator();
		while (S_it.hasNext()) { // iterate over the summation terms, V
			state V = S_it.next();
			BigDecimal alpha = V.coeff;

			ArrayList<state> Ts = V.nested_states;
			ListIterator<state> Ts_it = Ts.listIterator();
			while (Ts_it.hasNext()) { // iterate over the difference terms, T
				state T = Ts_it.next();
				BigDecimal C = T.coeff;

				double t0 = T.s.get(0);
				DoubleMatrix1D t = T.s.viewPart(1, K).copy();

				BigDecimal Fi = calc_logisticF_inside(Ustat, t0, t); // Compute h(u, t0, t)
				BigDecimal[] Ji = calc_jac_log_inside(Ustat, t0, t); // Compute d/dx (h(u, t0, t))
				BigDecimal[][] Hi = calc_stat_hess_log_inside(Ustat, t0, t); // Compute d^2/dx^2 (h(u, t0, t))

				for (int i=0; i<K-1; i++) {
					for (int j=0; j<K-1; j++) {
						BigDecimal G = Ji[i];
						G = G.multiply(Ji[j], MC);
						Hi[i][j] = Hi[i][j].add(G, MC);

						Hi[i][j] = Hi[i][j].multiply(Fi, MC);
						Hi[i][j] = Hi[i][j].multiply(alpha, MC);
						Hi[i][j] = Hi[i][j].multiply(C, MC);
						Hess[i][j] = Hess[i][j].add(Hi[i][j], MC);
					} // endfor i
				} // endfor j
			}
		}

		BigDecimal F = evaluate(stat_point); // compute e^k(x)
		double[][] hess_double = new double[K-1][K-1];
		for (int i=0; i<K-1; i++) {
			for (int j=0; j<K-1; j++) {
				Hess[i][j] = Hess[i][j].divide(F, MC);
				hess_double[i][j] = Hess[i][j].doubleValue();
			}
		}

		return hess_double;
	}

	/**
	 * Evaluate the Jacobian of k(x) at the given point, point
	 * Does not have to be the stat point. For use in gradient descent for
	 * finding the stationary point
	 * @param point Given point to evaluate Jacobian
	 * @return double[] representing Jacobian
	 * @throws InternalErrorException
	 */
	public double[] evaluateJac(double[] point) throws InternalErrorException {
		DoubleMatrix1D x = new DenseDoubleMatrix1D(point);
		BigDecimal[] U = logistic_transform(x);

		BigDecimal[] Jac = new BigDecimal[K-1];
		Arrays.fill(Jac, BigDecimal.ZERO);

		ListIterator<state> S_it = States.listIterator();
		while (S_it.hasNext()) { // iterate over the summation terms, V
			state V = S_it.next();
			BigDecimal alpha = V.coeff;

			ArrayList<state> Ts = V.nested_states;
			ListIterator<state> Ts_it = Ts.listIterator();
			while (Ts_it.hasNext()) { // iterate over the difference terms, T
				state T = Ts_it.next();
				BigDecimal C = T.coeff;

				double t0 = T.s.get(0);
				DoubleMatrix1D t = T.s.viewPart(1,K).copy();

				BigDecimal[] Hi = calc_jac_log_inside(U, t0, t); // calculate d/dx (h(u, to, t))
				BigDecimal Fi = calc_logisticF_inside(U, t0, t); // calculate e^(-h(u, t0, t))
				for (int i=0; i<Hi.length; i++) { // multiply by binomial and alpha coefficients
					Hi[i] = Hi[i].multiply(Fi, MC);
					Hi[i] = Hi[i].multiply(alpha, MC);
					Hi[i] = Hi[i].multiply(C, MC);
					Jac[i] = Jac[i].add(Hi[i], MC);
				}
			}
		}

		BigDecimal F = evaluate(point); // calculate e^(k(x))
		double[] jac_double = new double[Jac.length];
		for (int i=0; i<Jac.length; i++) {
			Jac[i] = Jac[i].divide(F, MC);
			jac_double[i] = Jac[i].doubleValue();
		}

		return jac_double;
	}

	/*================ Constructors =================*/
	public LogisticFunctionMSBD(int ndim, int[] population, double[][] demands, double[] servers, double epsilon) throws InternalErrorException {
		this(ndim, population, demands, servers, epsilon, MathContext.DECIMAL128);
	}

	public LogisticFunctionMSBD(int ndim, DoubleMatrix1D population, DoubleMatrix2D demands, DoubleMatrix1D servers, double epsilon) throws InternalErrorException {
		this(ndim, population, demands, servers, epsilon, MathContext.DECIMAL128);
	}

	public LogisticFunctionMSBD(int ndim, DoubleMatrix1D population, DoubleMatrix2D demands, DoubleMatrix1D servers, double epsilon, MathContext MC) throws InternalErrorException {
		super(ndim);
		this.initialize(ndim, population, demands, servers, epsilon, MC);
	}

	public LogisticFunctionMSBD(int ndim, int[] population, double[][] demands, double[] servers, double epsilon, MathContext MC) throws InternalErrorException {
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
	public LogisticFunctionMSBD(LogisticFunctionMSBD LF) throws InternalErrorException {
		// easier to re-calculate the states, than risk wrongly copying it
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

	/**
	 * Private class to store the summation/difference terms, can have nested states,
	 * for multiple levels of nesting. Each state has it value stored as a DoubleMatrix,
	 * amd a coefficient to multiply its terms by. It can have an ArrayList of nested
	 * states, which can be traversed over too
	 */
	private class state {

		private DoubleMatrix1D s; // the state
		private BigDecimal coeff; // the coefficient
		private ArrayList<state> nested_states; // nested states associated with this state

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

	/**
	 * Calculates the function d/dx(-h(u, t0, t)) as specified in Ong, 2018
	 * @param U simplex point to evaluate
	 * @param t0 t0 term
	 * @param t t term
	 * @return d/dx(-h(u, t0, t)) in BigDecimal
	 */
	private BigDecimal[] calc_jac_log_inside(BigDecimal[] U, double t0, DoubleMatrix1D t) {
		BigDecimal T0 = new BigDecimal(t0);

		BigDecimal[] Zeta = new BigDecimal[R];
		for (int r=0; r<R; r++) {
			BigDecimal Zetar = BigDecimal.ZERO;
			for (int k=0; k<K; k++) {
				BigDecimal H = U[k];
				H = H.multiply(T0, MC);
				H = H.add(new BigDecimal(t.get(k)), MC);
				H = H.multiply(new BigDecimal(sigma.get(k,r)), MC);
				Zetar = Zetar.add(H, MC);
			}
			BigDecimal Nr = new BigDecimal(pop.get(r));
			Zetar = BigDecimal.ONE.divide(Zetar, MC);
			Zetar = Zetar.multiply(Nr, MC);
			Zeta[r] = Zetar;
		}

		BigDecimal[] Jac = new BigDecimal[K-1];
		for (int i=0; i<K-1; i++) {
			BigDecimal Ui = U[i];
			BigDecimal L = BigDecimal.ZERO;
			for (int r=0; r<R; r++) {
				BigDecimal Zetar = Zeta[r];
				BigDecimal H = BigDecimal.ZERO;
				for (int k=0; k<K; k++) {
					BigDecimal G = new BigDecimal(sigma.get(k,r), MC);
					G = G.multiply(U[k], MC);
					H = H.subtract(G, MC);
				}
				H = H.add(new BigDecimal(sigma.get(i,r)), MC);
				H = H.multiply(Zetar, MC);
				L = L.add(H, MC);
			}
			L = L.multiply(T0, MC);
			L = L.subtract(new BigDecimal(K), MC);
			L = L.multiply(Ui, MC);
			L = L.add(BigDecimal.ONE);
			Jac[i] = L;
		}

		return Jac;
	}

	/**
	 * Calculates the function d^2/dx^2 (-h(u, t0, t)) as specified in Ong, 2018
	 * @param Ustat Stationary point in simplex terms
	 * @param t0
	 * @param t
	 * @return Hessian at given point (not stationary w.r.t h(u, t0, t)
	 */
	private BigDecimal[][] calc_stat_hess_log_inside(BigDecimal[] Ustat, double t0, DoubleMatrix1D t) {
		BigDecimal T0 = new BigDecimal(t0);

		BigDecimal[] Zeta = new BigDecimal[R];
		BigDecimal[] Zeta2 = new BigDecimal[R];
		for (int r=0; r<R; r++) {
			BigDecimal Zetar = BigDecimal.ZERO;
			for (int k=0; k<K; k++) {
				BigDecimal H = Ustat[k];
				H = H.multiply(T0, MC);
				H = H.add(new BigDecimal(t.get(k)), MC);
				H = H.multiply(new BigDecimal(sigma.get(k,r)), MC);
				Zetar = Zetar.add(H, MC);
			}
			BigDecimal Nr = new BigDecimal(pop.get(r));
			Zetar = BigDecimal.ONE.divide(Zetar, MC);
			Zetar = Zetar.multiply(Nr, MC);
			Zeta[r] = Zetar;
			Zeta2[r] = Zetar.pow(2, MC);
		}

		BigDecimal[][] Hessu = new BigDecimal[K][K]; // d^2/du^2 (h(u, t0, t))
		for (int i=0; i<K; i++) {
			for (int j=0; j<K; j++) {
				BigDecimal G = BigDecimal.ZERO;
				for (int r=0; r<R; r++) {
					BigDecimal H = new BigDecimal(sigma.get(i,r));
					H = H.multiply(new BigDecimal(sigma.get(j,r)), MC);
					H = H.multiply(Zeta2[r], MC);
					H = H.divide(new BigDecimal(pop.get(r)), MC);
					G = G.subtract(H, MC);
				}
				G = G.multiply(T0.pow(2,MC),MC);

				if (i==j) {
					BigDecimal U = Ustat[i];
					U = U.multiply(Ustat[i], MC);
					U = U.pow(-1, MC);
					G = G.subtract(U, MC);
				}
				Hessu[i][j] = G;
			}
		}

		//contribution from 1st derivative (not necessarily zero)
		BigDecimal[] Jacu = new BigDecimal[K]; // d/du (h(u, t0, t))
		for (int i=0; i<K; i++) {
			BigDecimal G = BigDecimal.ZERO;
			for (int r=0; r<R; r++) {
				BigDecimal H = Zeta[r];
				H = H.multiply(new BigDecimal(sigma.get(i,r)), MC);
				G = G.add(H, MC);
			}
			G = G.multiply(T0, MC);
			BigDecimal U = Ustat[i];
			U = U.pow(-1, MC);
			G = G.add(U, MC);
			Jacu[i] = G;
		}

		BigDecimal[][] Jux = jac_ux(Ustat); // du/dx
		BigDecimal[][][] Juxx = jac_uxx(Ustat); // d^u/dx^2
		BigDecimal[][] Hessx = new BigDecimal[K-1][K-1];
		for (int i=0; i<K-1; i++) {
			for (int j=0; j<K-1; j++) {
				BigDecimal G = BigDecimal.ZERO;
				for (int p=0; p<K; p++) {
					for (int q=0; q<K; q++) {
						BigDecimal H = Hessu[p][q];
						H = H.multiply(Jux[p][j], MC);
						H = H.multiply(Jux[q][i], MC);
						G = G.add(H, MC);
					}
				}
				Hessx[i][j] = G;

				G = BigDecimal.ZERO;
				for (int p=0; p<K; p++) {
					BigDecimal H =  Jacu[p];
					H = H.multiply(Juxx[p][i][j], MC);
					G = G.add(H, MC);
				}
				Hessx[i][j] = Hessx[i][j].add(G);
			}
		}

		return Hessx;
	}

	/**
	 * Calculates the function e^(-h(u, t0, t)) as specified in Ong, 2018
	 * @param U Point in simplex terms
	 * @param t0
	 * @param t
	 * @return e^(-h(u, t0, t)) at given point
	 */
	private BigDecimal calc_logisticF_inside(BigDecimal[] U, double t0, DoubleMatrix1D t) {
		BigDecimal H = BigDecimal.ONE;
		for (int r=0; r<R; r++) {
			BigDecimal G = BigDecimal.ZERO;
			for (int k = 0; k<(K); k++) {
				BigDecimal L = new BigDecimal(t0);
				L = L.multiply(U[k], MC);
				L = L.add(new BigDecimal(t.get(k)), MC);
				L = L.multiply(new BigDecimal(sigma.get(k,r)),MC);
				G = G.add(L,MC);
			}
			G = G.pow((int) pop.get(r),MC);
			H = H.multiply(G,MC);
		}
		for (int k = 0; k<(K); k++) {
			H = H.multiply(U[k],MC);
		}
		return H;
	}

	/**
	 * Jacobian between simplex and x : du_i/dx_j, given u
	 * @param U point to evaluate on
	 * @return Jacobian
	 */
	private BigDecimal[][] jac_ux(BigDecimal[] U) {
		BigDecimal[][] Jac = new BigDecimal[K][K-1];
		for (int i=0; i<K; i++) {
			for (int j=0; j<K-1; j++) {
				Jac[i][j] = U[i];
				Jac[i][j] = Jac[i][j].negate();
				Jac[i][j] = Jac[i][j].multiply(U[j], MC);
				if (i==j)
					Jac[i][j] = Jac[i][j].add(U[i], MC);
			}
		}
		return Jac;
	}

	/**
	 * Hessian between simplex and x : d^2(u_i)/(d_xj d_xk)
	 * @param U point to evaluate on, in simplex terms
	 * @return Hessian
	 */
	private BigDecimal[][][] jac_uxx(BigDecimal[] U) {
		BigDecimal[][][] J = new BigDecimal[K][K-1][K-1];
		for (int p=0; p<K; p++)
			for (int i=0; i<K-1; i++)
				for (int j=0; j<K-1; j++) {
					BigDecimal H = new BigDecimal(Utils.delta(p,j)*Utils.delta(p,i));
					H = H.multiply(U[p], MC);
					J[p][i][j] = H;

					H = new BigDecimal(Utils.delta(p,j));
					H = H.multiply(U[p], MC);
					H = H.multiply(U[i], MC);
					J[p][i][j] = J[p][i][j].subtract(H, MC);

					H = new BigDecimal(Utils.delta(i,j));
					H = H.multiply(U[p], MC);
					H = H.multiply(U[j], MC);
					J[p][i][j] = J[p][i][j].subtract(H, MC);

					H = new BigDecimal(Utils.delta(p,i));
					H = H.multiply(U[p], MC);
					H = H.multiply(U[j], MC);
					J[p][i][j] = J[p][i][j].subtract(H, MC);

					H = U[p];
					H = H.multiply(U[i], MC);
					H = H.multiply(U[j], MC);
					H = H.multiply(new BigDecimal(2), MC);
					J[p][i][j] = J[p][i][j].add(H, MC);
				}
		return J;
	}

	/**
	 * Transforms from x into u
	 * @param x point in R^{K-1}
	 * @return u, simplex point
	 * @throws InternalErrorException
	 */
	private BigDecimal[] logistic_transform(DoubleMatrix1D x) throws InternalErrorException {
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
			U[i] = ui;
		}
		U[K-1] = BigDecimal.ONE.divide(Z, MC);
		return U;
	}


	/*================ Static helper functions =============== */
	private static int sum_int(int[] int_array) {
		int sum = 0;
		for (int i : int_array)
			sum += i;
		return sum;
	}

	/**
	 * Binomial term n choose k
	 * @param n
	 * @param k
	 * @return
	 */
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

}
