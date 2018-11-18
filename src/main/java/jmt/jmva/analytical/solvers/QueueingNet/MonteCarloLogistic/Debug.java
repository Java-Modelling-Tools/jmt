package jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;

import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import jmt.jmva.analytical.solvers.Exceptions.InternalErrorException;
import jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Solver.LogisticFunctionMS;
import jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Solver.LogisticFunctionMSBD;
import org.apache.commons.math3.analysis.*;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.optimization.direct.BOBYQAOptimizer;
import org.apache.commons.math3.optimization.direct.PowellOptimizer;
import org.apache.commons.math3.optimization.general.ConjugateGradientFormula;
import org.apache.commons.math3.optimization.general.NonLinearConjugateGradientOptimizer;

import static jmt.jmva.analytical.solvers.Utilities.MiscFunctions.log;

public class Debug {

	public static void main(String[] args) throws Exception {

		/*============= Test use of direct optimizer ===============*/
		class QuadraticFunction2D implements MultivariateFunction {
			private double a,b,c;
			public QuadraticFunction2D(double a, double b, double c) {
				this.a = a; this.b = b; this.c = c;
			}

			@Override
			public double value(double[] doubles) {
				double x = doubles[0];
				double y = doubles[1];
				return a*(Math.pow(x,2)) + b*x + c + a*(Math.pow(y,2)) + b*y + c;
			}
		}

		BOBYQAOptimizer Opt = new BOBYQAOptimizer(4);
		PointValuePair optimum = Opt.optimize(1000, new QuadraticFunction2D(1,1,1), GoalType.MINIMIZE, new double[] { -5.0, 5.0 });
		System.out.print("optimum: ");
		System.out.println(Arrays.toString(optimum.getPoint()));

		/*============= Test functions ===============*/
		double[][] dem_mat = new double[][] {{.8},{.6}};
		double[] s = new double[] {2,2};
		int[] pop = new int[] {3};
		LogisticFunctionMS LFMS = new LogisticFunctionMS(1, pop, dem_mat, s, 0);
		LogisticFunctionMSBD LFMSBD = new LogisticFunctionMSBD(1, pop, dem_mat, s, 0);
		System.out.print("Optimum value: ");
		System.out.println(LFMS.evaluate(new double[] {.30036055}));
		System.out.print("Optimum value BD: ");
		System.out.println(LFMSBD.evaluate(new double[] {.30036055}));
		System.out.print("Jacobian: ");
		System.out.println(Arrays.toString(LFMS.evaluateJac(new double[] {.30036055})));
		System.out.print("Jacobian: ");
		System.out.println(Arrays.toString(LFMSBD.evaluateJac(new double[] {.30036055})));
		System.out.print("Hessian: ");
		System.out.println(new DenseDoubleMatrix2D(LFMS.evaluateHessStat(new double[] {.30036055})));
		System.out.print("Hessian: ");
		System.out.println(new DenseDoubleMatrix2D(LFMSBD.evaluateHessStat(new double[] {.30036055})));

		pop = new int[] {11};
		LFMS = new LogisticFunctionMS(1, pop, dem_mat, s, 0);
		LFMSBD = new LogisticFunctionMSBD(1, pop, dem_mat, s, 0);
		System.out.print("Optimum value: ");
		System.out.println(LFMS.evaluate(new double[] {1.10520391}));
		System.out.print("Optimum value BD: ");
		System.out.println(LFMSBD.evaluate(new double[] {1.10520391}));
		System.out.print("Jacobian: ");
		System.out.println(Arrays.toString(LFMS.evaluateJac(new double[] {1.10520391})));
		System.out.print("Jacobian: ");
		System.out.println(Arrays.toString(LFMSBD.evaluateJac(new double[] {1.10520391})));
		System.out.print("Hessian: ");
		System.out.println(new DenseDoubleMatrix2D(LFMS.evaluateHessStat(new double[] {1.10520391})));
		System.out.print("Hessian: ");
		System.out.println(new DenseDoubleMatrix2D(LFMSBD.evaluateHessStat(new double[] {1.10520391})));

		dem_mat = new double[][] {{3.8},{.4},{.9}};
		s = new double[] {2,2,4};
		pop = new int[] {3};
		LFMS = new LogisticFunctionMS(2, pop, dem_mat, s, 0);
		LFMSBD = new LogisticFunctionMSBD(2, pop, dem_mat, s, 0);
		System.out.print("Optimum value: ");
		System.out.println(LFMS.evaluate(new double[] {1.02029616, -0.00949956}));
		System.out.print("Optimum value BD: ");
		System.out.println(LFMSBD.evaluate(new double[] {1.02029616, -0.00949956}));
		System.out.print("Jacobian: ");
		System.out.println(Arrays.toString(LFMS.evaluateJac(new double[] {1.02029616, -0.00949956})));
		System.out.print("Jacobian: ");
		System.out.println(Arrays.toString(LFMSBD.evaluateJac(new double[] {1.02029616, -0.00949956})));
		System.out.print("Hessian: ");
		System.out.println(new DenseDoubleMatrix2D(LFMS.evaluateHessStat(new double[] {1.02029616, -0.00949956})));
		System.out.print("Hessian: ");
		System.out.println(new DenseDoubleMatrix2D(LFMSBD.evaluateHessStat(new double[] {1.02029616, -0.00949956})));

		pop = new int[] {11};
		LFMS = new LogisticFunctionMS(2, pop, dem_mat, s, 0);
		LFMSBD = new LogisticFunctionMSBD(2, pop, dem_mat, s, 0);
		System.out.print("Optimum value: ");
		System.out.println(LFMS.evaluate(new double[] {2.27028574, -0.01329452}));
		System.out.print("Optimum value BD: ");
		System.out.println(LFMSBD.evaluate(new double[] {2.27028574, -0.01329452}));
		System.out.print("Jacobian: ");
		System.out.println(Arrays.toString(LFMS.evaluateJac(new double[] {2.27028574, -0.01329452})));
		System.out.print("Jacobian: ");
		System.out.println(Arrays.toString(LFMSBD.evaluateJac(new double[] {2.27028574, -0.01329452})));
		System.out.print("Hessian: ");
		System.out.println(new DenseDoubleMatrix2D(LFMS.evaluateHessStat(new double[] {2.27028574, -0.01329452})));
		System.out.print("Hessian: ");
		System.out.println(new DenseDoubleMatrix2D(LFMSBD.evaluateHessStat(new double[] {2.27028574, -0.01329452})));

		/*============= Test use of direct optimizer on LFMS ===============*/
		dem_mat = new double[][] {{.8},{.6}};
		s = new double[] {2,2};
		pop = new int[] {3};

		class LFMS1D implements MultivariateFunction {
			private LogisticFunctionMS LFMS;
			public LFMS1D(int ndim, int[] population, double[][] demands, double[] servers, double epsilon) throws InternalErrorException {
				LFMS = new LogisticFunctionMS(ndim, population, demands, servers, epsilon);
			}

			@Override
			public double value(double[] doubles) {
				double[] x = new double[doubles.length-1];
				for (int i=0; i<doubles.length-1; i++)
					x[i] = doubles[i];

				try {
					BigDecimal F = LFMS.evaluate(x);
					F = log(F, MathContext.DECIMAL128);
					return -F.doubleValue() + Math.pow(doubles[doubles.length-1],2);
				} catch (Exception e) {
					return 0.;
				}
			}
		}

		PowellOptimizer POpt = new PowellOptimizer(1e-05, 1e-05);
		optimum = POpt.optimize(1000, new LFMS1D(1,pop, dem_mat, s, 0), GoalType.MINIMIZE, new double[] { -5.0, 0.0 });
		System.out.print("optimum: ");
		System.out.println(Arrays.toString(optimum.getPoint()));

		dem_mat = new double[][] {{3.8},{.4},{.9}};
		s = new double[] {2,2,4};
		pop = new int[] {3};

		LFMS = new LogisticFunctionMS(2, pop, dem_mat, s, 0);
		System.out.println(LFMS.evaluate(new double[] {1.020336, -0.00955834}));

		POpt = new PowellOptimizer(1e-05, 1e-05);
		optimum = POpt.optimize(1000, new LFMS1D(2,pop, dem_mat, s, 0), GoalType.MINIMIZE, new double[] { -5.0, 0.0, 0.0 });
		System.out.print("optimum: ");
		System.out.println(Arrays.toString(optimum.getPoint()));

		/*=============== Test use of grad_desc optimizer on LFMS ===============*/

		class LFMS1D_grad implements MultivariateVectorFunction {
			private LogisticFunctionMSBD LFMS;
			public LFMS1D_grad(int ndim, int[] population, double[][] demands, double[] servers, double epsilon) throws InternalErrorException {
				LFMS = new LogisticFunctionMSBD(ndim, population, demands, servers, epsilon, new MathContext(512));
			}

			@Override
			public double[] value(double[] doubles) {
				double[] x = new double[doubles.length];
				for (int i=0; i<doubles.length; i++)
					x[i] = doubles[i];
				try {
					double[] J = LFMS.evaluateJac(x);
					for (int i=0; i<J.length; i++)
						J[i] *= -1;
					System.out.println(new DenseDoubleMatrix1D(J));
					return J;
				} catch (Exception e) {
					double[] J = new double[x.length];
					Arrays.fill(J, 0.);
					return J;
				}
			}
		}

		class LFMS1D_partgrad implements MultivariateFunction {
			private LogisticFunctionMS LFMS;
			private int k;
			public LFMS1D_partgrad(int ndim, int[] population, double[][] demands, double[] servers, double epsilon, int k) throws InternalErrorException {
				LFMS = new LogisticFunctionMS(ndim, population, demands, servers, epsilon);
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
				} catch (Exception e) {
					double[] J = new double[x.length];
					Arrays.fill(J, 0.);
					return J[k];
				}
			}
		}

		class LFMS1D_diff implements DifferentiableMultivariateFunction {
			private LogisticFunctionMSBD LFMS;
			private int ndim;
			private int[] pop;
			private double[][] demands;
			private double[] servers;

			public LFMS1D_diff(int ndim, int[] population, double[][] demands, double[] servers, double epsilon) throws InternalErrorException {
				this.ndim = ndim;
				this.pop = population;
				this.demands = demands;
				this.servers = servers;
				LFMS = new LogisticFunctionMSBD(ndim, population, demands, servers, epsilon, new MathContext(512));
			}

			@Override
			public double value(double[] doubles) {
				double[] x = new double[doubles.length];
				for (int i=0; i<doubles.length; i++)
					x[i] = doubles[i];

				BigDecimal G=BigDecimal.ZERO;
				try {
					BigDecimal F = LFMS.evaluate(x);
					G = F;
					F = log(F, MathContext.DECIMAL128);
					return -F.doubleValue();
				} catch (Exception e) {
					System.out.println(G);
					e.printStackTrace();
					return 0.;
				}
			}

			@Override
			public MultivariateVectorFunction gradient() {
				try {
					return new LFMS1D_grad(ndim, pop, demands, servers, 0);
				} catch (Exception e) {
					return null;
				}
			}

			@Override
			public MultivariateFunction partialDerivative(int i) {
				try {
					return new LFMS1D_partgrad(ndim, pop, demands, servers, 0, i);
				} catch (Exception e) {
					return null;
				}
			}
		}

		NonLinearConjugateGradientOptimizer optimizer =
				new NonLinearConjugateGradientOptimizer(ConjugateGradientFormula.POLAK_RIBIERE);

		dem_mat = new double[][] {{.8},{.6}};
		s = new double[] {2,2};
		pop = new int[] {3};

		optimum = optimizer.optimize(1000, new LFMS1D_diff(1,pop, dem_mat, s, 0), GoalType.MINIMIZE, new double[] { -1.0 });
		System.out.print("optimum: ");
		System.out.println(Arrays.toString(optimum.getPoint()));
		LFMS = new LogisticFunctionMS(1, pop, dem_mat, s, 0);
		LFMSBD = new LogisticFunctionMSBD(1, pop, dem_mat, s, 0);
		System.out.print("Hessian: ");
		System.out.println(new DenseDoubleMatrix2D(LFMS.evaluateHessStat(optimum.getPoint())));
		System.out.print("Hessian: ");
		System.out.println(new DenseDoubleMatrix2D(LFMSBD.evaluateHessStat(optimum.getPoint())));

		pop = new int[] {6};

		optimum = optimizer.optimize(1000, new LFMS1D_diff(1,pop, dem_mat, s, 0), GoalType.MINIMIZE, new double[] { -1.0 });
		System.out.print("optimum: ");
		System.out.println(Arrays.toString(optimum.getPoint()));
		LFMS = new LogisticFunctionMS(1, pop, dem_mat, s, 0);
		LFMSBD = new LogisticFunctionMSBD(1, pop, dem_mat, s, 0);
		System.out.print("Hessian: ");
		System.out.println(new DenseDoubleMatrix2D(LFMS.evaluateHessStat(optimum.getPoint())));
		System.out.print("Hessian: ");
		System.out.println(new DenseDoubleMatrix2D(LFMSBD.evaluateHessStat(optimum.getPoint())));

		pop = new int[] {10};

		optimum = optimizer.optimize(1000, new LFMS1D_diff(1,pop, dem_mat, s, 0), GoalType.MINIMIZE, new double[] { -1.0 });
		System.out.print("optimum: ");
		System.out.println(Arrays.toString(optimum.getPoint()));
		LFMS = new LogisticFunctionMS(1, pop, dem_mat, s, 0);
		LFMSBD = new LogisticFunctionMSBD(1, pop, dem_mat, s, 0);
		System.out.print("Hessian: ");
		System.out.println(new DenseDoubleMatrix2D(LFMS.evaluateHessStat(optimum.getPoint())));
		System.out.print("Hessian: ");
		System.out.println(new DenseDoubleMatrix2D(LFMSBD.evaluateHessStat(optimum.getPoint())));

		dem_mat = new double[][] {{3.8},{.4},{.9}};
		s = new double[] {2,2,4};
		pop = new int[] {3};

		optimum = optimizer.optimize(1000, new LFMS1D_diff(2,pop, dem_mat, s, 0), GoalType.MINIMIZE, new double[] { -1.0, 0.0 });
		System.out.print("optimum: ");
		System.out.println(Arrays.toString(optimum.getPoint()));
		LFMS = new LogisticFunctionMS(2, pop, dem_mat, s, 0);
		LFMSBD = new LogisticFunctionMSBD(2, pop, dem_mat, s, 0);
		System.out.print("Hessian: ");
		System.out.println(new DenseDoubleMatrix2D(LFMS.evaluateHessStat(optimum.getPoint())));
		System.out.print("Hessian: ");
		System.out.println(new DenseDoubleMatrix2D(LFMSBD.evaluateHessStat(optimum.getPoint())));

		pop = new int[] {5};

		optimum = optimizer.optimize(1000, new LFMS1D_diff(2,pop, dem_mat, s, 0), GoalType.MINIMIZE, new double[] { -1.0, 0.0 });
		System.out.print("optimum: ");
		System.out.println(Arrays.toString(optimum.getPoint()));
		LFMS = new LogisticFunctionMS(2, pop, dem_mat, s, 0);
		LFMSBD = new LogisticFunctionMSBD(2, pop, dem_mat, s, 0);
		System.out.print("Hessian: ");
		System.out.println(new DenseDoubleMatrix2D(LFMS.evaluateHessStat(optimum.getPoint())));
		System.out.print("Hessian: ");
		System.out.println(new DenseDoubleMatrix2D(LFMSBD.evaluateHessStat(optimum.getPoint())));

		pop = new int[] {12};

		optimum = optimizer.optimize(1000, new LFMS1D_diff(2,pop, dem_mat, s, 0), GoalType.MINIMIZE, new double[] { -1.0, 0.0 });
		System.out.print("optimum: ");
		System.out.println(Arrays.toString(optimum.getPoint()));
		LFMS = new LogisticFunctionMS(2, pop, dem_mat, s, 0);
		LFMSBD = new LogisticFunctionMSBD(2, pop, dem_mat, s, 0);
		System.out.print("Hessian: ");
		System.out.println(new DenseDoubleMatrix2D(LFMS.evaluateHessStat(optimum.getPoint())));
		System.out.print("Hessian: ");
		System.out.println(new DenseDoubleMatrix2D(LFMSBD.evaluateHessStat(optimum.getPoint())));

		dem_mat = new double[][] {{3.8, 3.8, 1.8},{2.6, 2.6, 1.8},{3.4,3.4, 1.8}};
		s = new double[] {2,2,4};
		pop = new int[] {10,10, 10};

		optimum = optimizer.optimize(1000, new LFMS1D_diff(2,pop, dem_mat, s, 0), GoalType.MINIMIZE, new double[] { -1.0, 0.0 });
		System.out.print("optimum grad : ");
		System.out.println(Arrays.toString(optimum.getPoint()));

		LFMS = new LogisticFunctionMS(2, pop, dem_mat, s, 0);
		LFMSBD = new LogisticFunctionMSBD(2, pop, dem_mat, s, 0);
		System.out.print("Hessian: ");
		System.out.println(new DenseDoubleMatrix2D(LFMS.evaluateHessStat(optimum.getPoint())));
		System.out.print("Hessian: ");
		System.out.println(new DenseDoubleMatrix2D(LFMSBD.evaluateHessStat(optimum.getPoint())));
	}

}
