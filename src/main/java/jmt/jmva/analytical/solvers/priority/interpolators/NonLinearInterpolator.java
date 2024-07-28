<<<<<<< HEAD
package jmt.jmva.analytical.solvers.priority.interpolators;

public class NonLinearInterpolator implements Interpolator {

    @Override
    public double interpolate(double x1, double y1, double x2, double y2, double mainX) {
        double lhs = Math.pow(y1, (x2 - mainX) / (x2 - x1));
        double rhs = Math.pow(y2, (mainX - x1) / (x2 - x1));
        return lhs * rhs;
    }
}
=======
package jmt.jmva.analytical.solvers.priority.interpolators;

public class NonLinearInterpolator implements Interpolator {

    @Override
    public double interpolate(double x1, double y1, double x2, double y2, double mainX) {
        double lhs = Math.pow(y1, (x2 - mainX) / (x2 - x1));
        double rhs = Math.pow(y2, (mainX - x1) / (x2 - x1));
        return lhs * rhs;
    }
}
>>>>>>> b0a37c45 (Initial commit at SVN r1153.)
