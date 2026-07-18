package jmt.jmva.analytical.solvers.priority.interpolators;

public class ReflectedNonLinearInterpolator implements Interpolator {

    /**
     * (a, b), (c, d)
     * y = d + f(x - c)/(x - c - 1)
     * <p>
     * f is a scalar factor to ensure it passes through (a, b)
     * f = (a - c - 1) * (b - d) / (a - c)
     */

    @Override
    public double interpolate(double a, double b, double c, double d, double x) {
        double f = (a - c - 1) * (b - d) / (a - c);
        return d + f * (x - c) / (x - c - 1);
    }
}
