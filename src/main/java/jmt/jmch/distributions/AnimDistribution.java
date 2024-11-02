/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package jmt.jmch.distributions;

import jmt.common.exception.IncorrectDistributionParameterException;
import jmt.engine.random.DeterministicDistr;
import jmt.engine.random.DeterministicDistrPar;
import jmt.engine.random.Exponential;
import jmt.engine.random.ExponentialPar;
import jmt.engine.random.HyperExp;
import jmt.engine.random.HyperExpPar;
import jmt.engine.random.Parameter;
import jmt.engine.random.Uniform;
import jmt.engine.random.UniformPar;
import jmt.engine.random.engine.MersenneTwister;
import jmt.engine.random.engine.RandomEngine;

/**
 * Abstract class for all the distributions that the tool offers for the service time and the inter arrival time.
 * 
 * @author Lorenzo Torri
 * Date: 29-mar-2024
 * Time: 13.40
 * 
 */
public abstract class AnimDistribution {
    protected double mean;
    protected double lambda;
    protected double[] percentiles = new double[2];
    protected RandomEngine engine = new MersenneTwister(23000);

    private static final String[] distributions = {ExponentialD.NAME, DeterminsiticD.NAME, UniformD.NAME, HyperExponentialD.NAME};

    public static String[] getDistributions(){
        return distributions;
    }

    /** creates a random engine for the nextrand() of each distribution */
    protected RandomEngine createEngine(){
        return engine;
    }

    /**
     * Reset the engine for the distributions
     * This is needed because we would like that if we start a new animation, the inter arrival times, the service times... are not completely random but pseudo random, and they follow always the same pattern
     */
    public void resetEngine(){
        engine = new MersenneTwister(23000);
    }

    /** 
     * Generates a new random value following the distribution of the enum value
     * 
     * @return new random value
     * @throws IncorrectDistributionParameterException this exception will never occur if you chose the parameters of the XPar in the body of the function correclty.
     * For example this Exception will happen if you chose a lambda < 0 for the Exponential
     */
    public abstract double nextRand() throws IncorrectDistributionParameterException;

    /** Converts the enum to a string (it is used for rendering the enum values inside a JComboBox) */
    public abstract String toString();

    /**
     * Set the mean of the distribution passing lambda or mhu (those distributions are only used for inter arrival and service)
     * @param value
     */
    public abstract void setMean(double value);

    /**
     * Calculate the percentiles 0.10 and 0.90 for each distribution
     */
    protected abstract void setPercentiles();

    /**
     * Map a value x on the interval [percentile 0.10, percentile 0.90]
     * @return a value between 0 and 1
     */
    public double mapValue(double value){
        double map = (value - percentiles[0]) / (percentiles[1] - percentiles[0]);
        map = Math.max(0.0, map);
        map = Math.min(1.0, map);
        return map;
    }
}


//------------------------ all the distributions available ----------------------------
class ExponentialD extends AnimDistribution{
    protected static final String NAME = "Exponential";

    public double nextRand() throws IncorrectDistributionParameterException {
        RandomEngine r = createEngine();
        Parameter par = new ExponentialPar(lambda); //if lambda < 0 then Exception
        Exponential distribution = new Exponential();
        distribution.setRandomEngine(r);
        return distribution.nextRand(par);
    }

    @Override
    public String toString() {
        return NAME;
    }

    @Override
    public void setMean(double value) {
        lambda = value;
        mean = 1/value;
        setPercentiles();
    }

    @Override
    protected void setPercentiles() {
        percentiles[0] = -Math.log(1 - 0.10) / lambda;
        percentiles[1] = -Math.log(1 - 0.90) / lambda;
    }
}

class DeterminsiticD extends AnimDistribution{
    protected static final String NAME = "Deterministic";

    @Override
    public double nextRand() throws IncorrectDistributionParameterException {
        RandomEngine r = createEngine();
        Parameter par = new DeterministicDistrPar(mean); //if t < 0 then Exception
        DeterministicDistr distribution = new DeterministicDistr();
        distribution.setRandomEngine(r);
        return distribution.nextRand(par); //next rand for a deterministic is the value t
    }

    @Override
    public String toString() {
        return NAME;
    }

    @Override
    public void setMean(double value) {
        lambda = value;
        mean = 1/value;
        setPercentiles();
    }

    @Override
    protected void setPercentiles() {
        percentiles[0] = mean;
        percentiles[1] = mean;
    } 
}

class UniformD extends AnimDistribution{
    protected static final String NAME = "Uniform";

    @Override
    public double nextRand() throws IncorrectDistributionParameterException {
        RandomEngine r = createEngine();
        Parameter par = new UniformPar(mean-1, mean+1); //if max > min or the mean is < 0 Exception
        Uniform distribution = new Uniform();
        distribution.setRandomEngine(r);
        return distribution.nextRand(par); 
    }

    @Override
    public String toString() {
        return "Uniform";
    }

    @Override
    public void setMean(double value) {
        lambda = value;
        mean = 1/value;
        setPercentiles();
    }

    @Override
    protected void setPercentiles() {
        double a = mean - 1;
        double b = mean + 1;
        percentiles[0] = a + 0.10 * (b-a);
        percentiles[1] = a + 0.90 * (b-a);
    } 
}

class HyperExponentialD extends AnimDistribution{
    protected static final String NAME = "Hyper-Exponential";

    @Override
    public double nextRand() throws IncorrectDistributionParameterException {
        RandomEngine r = createEngine();
        Parameter par = new HyperExpPar(0.5, lambda-0.1, lambda+0.1); //if p is not 0<p<1 or l1 < 0 or l2 < 0
        HyperExp distribution = new HyperExp();
        distribution.setRandomEngine(r);
        return distribution.nextRand(par);
    }

    @Override
    public String toString() {
        return "Hyper-Exponential";
    }

    @Override
    public void setMean(double value) {
        lambda = value;
        mean = 1/value;
        setPercentiles();
    }

    private double getPercentile(double perc){
        double cumulative = 0.0;
        cumulative += 0.5 * (-Math.log(1 - perc) / (lambda - 0.1));
        cumulative += 0.5 * (-Math.log(1 - perc) / (lambda + 0.1));
        return cumulative;
    } 

    @Override
    protected void setPercentiles() {
        double[] result = new double[2];
        result[0] = getPercentile(0.10);
        result[1] = getPercentile(0.90);
    }
}
