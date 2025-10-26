package jmt.gui.common.distributions.discrete;

import jmt.gui.common.JMTImageLoader;
import jmt.gui.common.distributions.Distribution;

import javax.swing.*;

public class Binomial extends Distribution {
    /**
     * Constructs a new Binomial Distribution
     */
    public Binomial() {
        super(
                "Binomial",
                "jmt.engine.random.discrete.Binomial",
                "jmt.engine.random.discrete.BinomialPar",
                "Binomial distribution"
        );
        hasMean = true;
        hasC = true;
        isNestable = true;
    }

    /**
     * Used to set parameters of this distribution
     *
     * @return distribution parameters
     */
    @Override
    protected Parameter[] setParameters() {
        Parameter[] parameters = new Parameter[2];

        // Sets parameter n
        parameters[0] = new Parameter("n", "n", Integer.class, new Integer(1));
        // Checks value of n must be greater than 0
        parameters[0].setValueChecker(value -> {
            Integer i = (Integer) value;
            return i.intValue() > 0;
        });
        // Sets parameter p
        parameters[1] = new Parameter("p", "p", Double.class, new Double(0.5));
        // Checks value of p must be between 0 and 1
        parameters[1].setValueChecker(value -> {
            Double d = (Double) value;
            return d.doubleValue() >= 0.0 && d.doubleValue() <= 1.0;
        });

        return parameters;
    }

    @Override
    protected ImageIcon setImage() {
        return JMTImageLoader.loadImage("Binomial");
    }

    @Override
    public String toString() {
        return "B(" + formatNumber(((Integer) parameters[0].getValue()).intValue()) + "," + formatNumber(((Double) parameters[1].getValue()).doubleValue()) + ")";
    }

    @Override
    public void setMean(double value) {
        setCM(value, c);
    }

    @Override
    public void setC(double value) {
        setCM(mean, value);
    }

    protected void setCM(double mean, double c) {
        // p = 1 - mean * c^2 && n = mean / p
        Object oldp = getParameter("p").getValue();
        Object oldn = getParameter("n").getValue();
        if (getParameter("p").setValue(new Double(1 - mean * c * c))
                && getParameter("n").setValue(new Integer(
                (int) Math.round(mean / ((Double) getParameter("p").getValue()).doubleValue())))) {
            updateCM();
        } else {
            getParameter("p").setValue(oldp);
            getParameter("n").setValue(oldn);
        }
    }

    @Override
    public void updateCM() {
        // mean = n*p
        mean = ((Integer) parameters[0].getValue()).intValue() * ((Double) parameters[1].getValue()).doubleValue();
        // c = sqrt((1-p)/mean)
        c = Math.sqrt((1 - ((Double) parameters[1].getValue()).doubleValue()) / mean);
    }
}
