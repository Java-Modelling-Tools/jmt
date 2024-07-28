package jmt.jmva.analytical.solvers.priority.interpolators;

public interface Interpolator {

    /**
     * Returns an interpolated value for mainY given (x1, y1), (x2, y2)
     * When used for calculating \rho' in PrioMVA,
     * x1 = floor(mainX)
     * y1 = rho(n - x1)
     * x2 = ceil(mainX)
     * y2 = rho(n - x2)
     * As x1 < x2, y1 >= y2 as the utilization increases as n does
     * */
    double interpolate(double x1, double y1, double x2, double y2, double mainX);
}
