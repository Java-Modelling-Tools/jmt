package common.distributions.discrete;

import common.JMTImageLoader;
import common.distributions.Distribution;

import javax.swing.*;

/**
 * Shifted geometric distribution: number of trials before the first success, shifted by one.
 */
/**
 * Shifted geometric distribution: number of trials before the first success, shifted by one.
 */
/**
 * Shifted geometric distribution: number of trials before the first success, shifted by one.
 */
/**
 * Shifted geometric distribution: number of trials before the first success, shifted by one.
 */
public class ShiftedGeometric extends Distribution {
    /**
     * Constructs a new Shifted Geometric Distribution
     */
    public ShiftedGeometric() {
        super(
                "Shifted Geometric",
                "random.discrete.ShiftedGeometric",
                "random.discrete.ShiftedGeometricPar",
                "Shifted Geometric distribution"
        );
        hasMean = true;
        hasC = false;
        isNestable = true;
    }

    @Override
    public String toString() {
        return "Geo(" + formatNumber(((Double) parameters[0].getValue()).doubleValue()) + ")";
    }

    /**
     * Used to set parameters of this distribution
     *
     * @return distribution parameters
     */
    @Override
    protected Parameter[] setParameters() {
        // Creates parameter array
        Parameter[] parameters = new Parameter[1];

        // Sets parameter p
        parameters[0] = new Parameter("p", "p", Double.class, new Double(0.5));
        // Checks value of p must be between 0 and 1
        parameters[0].setValueChecker(value -> {
            Double d = (Double) value;
            return d.doubleValue() > 0.0 && d.doubleValue() <= 1.0;
        });

        return parameters;
    }
    @Override
    protected ImageIcon setImage() {
        return JMTImageLoader.loadImage("ShiftedGeometric");
    }

    @Override
    public void setMean(double value) {
        // p = 1 / mean
        if (getParameter(0).setValue(new Double(1 / value))) {
            mean = value;
        }
    }

    @Override
    public void updateCM() {
        // mean = 1 / p
        mean = 1 / ((Double) parameters[0].getValue()).doubleValue();
    }
}
