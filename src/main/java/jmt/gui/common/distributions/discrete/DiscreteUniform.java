package jmt.gui.common.distributions.discrete;

import jmt.gui.common.JMTImageLoader;
import jmt.gui.common.distributions.Distribution;

import javax.swing.*;

public class DiscreteUniform extends Distribution {
    /**
     * Constructs a new Discrete Uniform Distribution
     */
    public DiscreteUniform() {
        super(
                "Discrete Uniform",
                "jmt.engine.random.discrete.Uniform",
                "jmt.engine.random.discrete.UniformPar",
                "Discrete Uniform distribution"
        );
        hasMean = true;
        hasC = true;
        isNestable = true;
        setValueChecker(new ValueChecker() {
            public boolean checkValue(Object value) {
                if (((Integer) parameters[1].getValue()).intValue() <= ((Integer) parameters[0].getValue()).intValue()) {
                    precondition = "Value of 'max' must be greater than value of 'min'.";
                    return false;
                }
                return true;
            }
        });
    }

    @Override
    protected Parameter[] setParameters() {
        // Creates parameter array
        Parameter[] parameters = new Parameter[2];

        // Sets parameter min
        parameters[0] = new Parameter("min", "min", Integer.class, new Integer(1));
        // Checks value of min must be greater than or equal to 0
        parameters[0].setValueChecker(value -> {
            Integer i = (Integer) value;
            return i.intValue() >= 0;
        });
        // Sets parameter max
        parameters[1] = new Parameter("max", "max", Integer.class, new Integer(10));
        // Checks value of max must be greater than or equal to 0
        parameters[1].setValueChecker(value -> {
            Integer i = (Integer) value;
            return i.intValue() >= 0;
        });

        return parameters;
    }

    @Override
    protected ImageIcon setImage() {
        return JMTImageLoader.loadImage("DiscreteUniform");
    }

    @Override
    public String toString() {
        return "U(" + formatNumber(((Integer) parameters[0].getValue()).intValue()) + "," + formatNumber(((Integer) parameters[1].getValue()).intValue()) + ")";
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
        // Backups old parameters to restore them upon a false result
        Object oldmin = getParameter("min").getValue();
        Object oldmax = getParameter("max").getValue();
        int min = (int) Math.round(mean - mean * c * Math.sqrt(3));
        int max = (int) Math.round(mean + mean * c * Math.sqrt(3));
        if (getParameter("min").setValue(new Integer(min)) && getParameter("max").setValue(new Integer(max))) {
            updateCM();
        } else {
            getParameter("min").setValue(oldmin);
            getParameter("max").setValue(oldmax);
        }
    }

    @Override
    public void updateCM() {
        int min = ((Integer) getParameter("min").getValue()).intValue();
        int max = ((Integer) getParameter("max").getValue()).intValue();
        mean = ((double) (min + max)) / 2;
        c = (double) (max - min) / (max + min) / Math.sqrt(3);
    }
}
