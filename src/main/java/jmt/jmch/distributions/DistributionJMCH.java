package jmt.jmch.distributions;

import java.util.Random;

import jmt.common.exception.IncorrectDistributionParameterException;
import jmt.engine.random.*;
import jmt.engine.random.engine.MersenneTwister;
import jmt.engine.random.engine.RandomEngine;


/**
 * Manager of the service time and inter arrival time distributions for JMCH.
 * It is encharge of converting the distribution result from the editor to a engine.random distribution.
 * 
 * @author Lorenzo Torri
 * Date: 14-jan-2025
 * Time: 22.13
 * 
 */
public class DistributionJMCH {
    protected DistributionContainer distributionCont; //this one is for the graphical part
    protected jmt.gui.common.distributions.Distribution distribution; //this one is for the solver
    protected double percentiles[];
    
    public DistributionJMCH () { 
        percentiles = new double[2];
    }

    /**
     * Convert the distribution from the gui.common version to the engine.random version
     * @param distribution the result of the editor
     * @throws IncorrectDistributionParameterException
     */
    public void convertDistribution(jmt.gui.common.distributions.Distribution d){
        try{
            //per le formule consiglio è vedersi cosa fa il metodo  updateCM() e trovare le formule inverse
            if(d instanceof jmt.gui.common.distributions.Exponential){
                distribution = (jmt.gui.common.distributions.Exponential) d;
                
                distributionCont = new DistributionContainer(new Exponential(), new ExponentialPar());
                distributionCont.getParameter().setMean(d.getMean());

                percentiles[0] = -Math.log(1 - 0.10) / (1/(double) d.getMean());
                percentiles[1] = -Math.log(1 - 0.90) / (1/(double) d.getMean());
            }
            else if(d instanceof jmt.gui.common.distributions.Deterministic){
                distribution = (jmt.gui.common.distributions.Deterministic) d;

                distributionCont = new DistributionContainer(new DeterministicDistr(), new DeterministicDistrPar(d.getMean()));

                percentiles[0] = 0;
                percentiles[1] = 2*d.getMean();
            }
            else if(d instanceof jmt.gui.common.distributions.Uniform){
                distribution = (jmt.gui.common.distributions.Uniform) d;

                double mean = d.getMean();
                double c = d.getC();
                double min = mean * (1 - c * Math.sqrt(3));
                double max = mean * (1 + c * Math.sqrt(3));

                distributionCont = new DistributionContainer(new Uniform(), new UniformPar(min, max));

                percentiles[0] = min + 0.10 * (max-min);
                percentiles[1] = min + 0.90 * (max-min);
            }
            else if(d instanceof jmt.gui.common.distributions.Hyperexponential){
                distribution = (jmt.gui.common.distributions.Hyperexponential) distribution;

                double c = d.getC();
                double mean = d.getMean();
                double p = 0.5 * (1 - Math.sqrt((c * c - 1) / (c * c + 1)));

                distributionCont = new DistributionContainer(new HyperExp(), new HyperExpPar(p, 2 * p / mean, 2 * (1 - p) / mean));
                
                //calculating the percentiles of an hyperexponential is quite difficult, since it is just for graphical taks, the option is to calculate them as if the distribution is an exponential
                percentiles[0] = -Math.log(1 - 0.10) / (1/(double) d.getMean());
                percentiles[1] = -Math.log(1 - 0.90) / (1/(double) d.getMean());
            }
        }
        catch(Exception e){
            
        }

        //resetEngine();
    }

    /**
     * Get the next random value for this distribution
     * @return random value
     * @throws IncorrectDistributionParameterException
     */
    public double nextRand() throws IncorrectDistributionParameterException{
        double rand = distributionCont.getDistribution().nextRand(distributionCont.getParameter());
        return rand;
    }

    /**
     * Map a value x on the interval [percentile 0.10, percentile 0.90]
     * @param value 
     * @return a value between 0 and 1
     */
    public double mapValue(double value){
        double map = (value - percentiles[0]) / (percentiles[1] - percentiles[0]);
        map = Math.max(0.0, map);
        map = Math.min(1.0, map);
        return map;
    }

    /**
     * Add new randomEngine
     */
    public void setEngine(RandomEngine randomEngine){
        ((Distribution) distributionCont.getDistribution()).setRandomEngine(randomEngine);
    }

    
    public jmt.gui.common.distributions.Distribution getDistribution(){
        return distribution;
    }
    
}
