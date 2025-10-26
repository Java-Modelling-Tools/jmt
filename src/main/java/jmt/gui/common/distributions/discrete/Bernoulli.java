package jmt.gui.common.distributions.discrete;

import jmt.gui.common.distributions.Distribution;

import jmt.gui.common.JMTImageLoader;
import javax.swing.ImageIcon;

public class Bernoulli extends Distribution {
    /**
     * Constructs a new Bernoulli Distribution
     */
    public Bernoulli() {
        super(
                "Bernoulli",
                "jmt.engine.random.discrete.Bernoulli",
                "jmt.engine.random.discrete.BernoulliPar",
                "Bernoulli distribution"
        );
        hasMean = true;
        hasC = false;
        isNestable = true;
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
            return d.doubleValue() >= 0.0 && d.doubleValue() <= 1.0;
        });

        return parameters;
    }

    @Override
    protected ImageIcon setImage() {
        return JMTImageLoader.loadImage("Bernoulli");
    }

    @Override
    public String toString() {
        return "Bern(" + formatNumber(((Double) parameters[0].getValue()).doubleValue()) + ")";
    }

    @Override
    public void setMean(double value) {
        // mean = p
        if (getParameter(0).setValue(new Double(value))) {
            mean = value;
        }
    }

    @Override
    public void updateCM() {
        // mean = p
        mean = ((Double) parameters[0].getValue()).doubleValue();
    }
}
