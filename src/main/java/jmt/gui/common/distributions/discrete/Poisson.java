package jmt.gui.common.distributions.discrete;

import jmt.gui.common.JMTImageLoader;
import jmt.gui.common.distributions.Distribution;

import javax.swing.*;

public class Poisson extends Distribution {
    /**
     * Constructs a new Poisson Distribution
     */
    public Poisson() {
        super(
                "Poisson",
                "jmt.engine.random.discrete.Poisson",
                "jmt.engine.random.discrete.PoissonPar",
                "Poisson distribution"
        );
        hasMean = true;
        hasC = false;
        isNestable = true;
    }

    @Override
    protected Parameter[] setParameters() {
        // Creates parameter array
        Parameter[] parameters = new Parameter[1];

        // Sets parameter lambda
        parameters[0] = new Parameter("lambda", "\u03BB", Double.class, new Double(1.0));
        // Checks value of lambda must be greater than 0
        parameters[0].setValueChecker(value -> {
            Double d = (Double) value;
            return d.doubleValue() > 0.0;
        });

        return parameters;
    }

    @Override
    protected ImageIcon setImage() {
        return JMTImageLoader.loadImage("Poisson");
    }

    @Override
    public String toString() {
        return "Poi(" + formatNumber(((Double) parameters[0].getValue()).doubleValue()) + ")";
    }

    @Override
    public void setMean(double value) {
        // lambda = mean
        if (getParameter(0).setValue(new Double(value))) {
            mean = value;
        }
    }

    @Override
    public void updateCM() {
        // mean = lambda
        mean = ((Double) parameters[0].getValue()).doubleValue();
    }
}
