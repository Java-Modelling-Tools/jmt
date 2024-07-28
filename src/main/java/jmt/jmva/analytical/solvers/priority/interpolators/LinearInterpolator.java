<<<<<<< HEAD
package jmt.jmva.analytical.solvers.priority.interpolators;

public class LinearInterpolator implements Interpolator {

    @Override
    public double interpolate(double x1, double y1, double x2, double y2, double mainX) {
        double linear = y1 * (x2 - mainX) + y2 * (mainX - x1);
        return linear / (x2 - x1);
    }
}
=======
package jmt.jmva.analytical.solvers.priority.interpolators;

public class LinearInterpolator implements Interpolator {

    @Override
    public double interpolate(double x1, double y1, double x2, double y2, double mainX) {
        double linear = y1 * (x2 - mainX) + y2 * (mainX - x1);
        return linear / (x2 - x1);
    }
}
>>>>>>> b0a37c45 (Initial commit at SVN r1153.)
