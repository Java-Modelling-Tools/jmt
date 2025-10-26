package jmt.gui.common.distributions.discrete;

import jmt.gui.common.JMTImageLoader;
import jmt.gui.common.distributions.Distribution;

import javax.swing.*;

public class Zipf extends Distribution {
    /**
     * Constructs a new Zipf Distribution
     */
    public Zipf() {
        super(
                "Zipf",
                "jmt.engine.random.discrete.Zipf",
                "jmt.engine.random.discrete.ZipfPar",
                "Zipf distribution"
        );
        hasMean = false;
        hasC = false;
        isNestable = true;
    }

    @Override
    protected Parameter[] setParameters() {
        Parameter[] parameters = new Parameter[2];

        // Sets parameter alpha
        parameters[0] = new Parameter("s", "s", Double.class, new Double(2.0));
        // Checks value of alpha must be greater than 0
        parameters[0].setValueChecker(value -> {
            Double d = (Double) value;
            return d.doubleValue() > 0.0;
        });

        // Sets parameter sampleSize
        parameters[1] = new Parameter("sampleSize", "Sample size", Integer.class, new Integer(1000));
        // Checks value of sampleSize must be greater than 0
        parameters[1].setValueChecker(value -> {
            Integer i = (Integer) value;
            return i.intValue() > 0;
        });

        return parameters;
    }

    @Override
    protected ImageIcon setImage() {
        return JMTImageLoader.loadImage("Zipf");
    }

    @Override
    public String toString() {
        return "Zipf(" + formatNumber(((Double) parameters[0].getValue()).doubleValue()) + ")";
    }
}
