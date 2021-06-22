package jmt.engine.NetStrategies.ImpatienceStrategies;

import jmt.common.exception.IncorrectDistributionParameterException;
import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceMeasurement.DoubleValueImpatienceMeasurement;
import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceMeasurement.ImpatienceMeasurement;
import jmt.engine.QueueNet.NetSystem;
import jmt.engine.random.Distribution;
import jmt.engine.random.Exponential;
import jmt.engine.random.ExponentialPar;
import jmt.engine.random.Parameter;
import jmt.engine.random.engine.RandomEngine;
import org.apache.commons.lang.mutable.MutableDouble;

public class Retrial implements Impatience {

  public Distribution getDistribution() {
    return distribution;
  }

  private Distribution distribution;
  private Parameter parameter;
  private ImpatienceType impatienceType;

  public Double retrialRate;

  private Exponential exponential;
  private RandomEngine randEngine;


  public Retrial(NetSystem netSystem, Double retrialRate) {
    retrialRate = retrialRate;
    try {
      parameter = new ExponentialPar(retrialRate);
    } catch (IncorrectDistributionParameterException e) {
      e.printStackTrace();
    }
    exponential = new Exponential();
    randEngine = netSystem.getEngine();
    exponential.setRandomEngine(randEngine);
    this.impatienceType = ImpatienceType.RETRIAL;
    this.distribution = exponential;
  }

  /**
   * Returns the type of impatience
   */
  @Override
  public ImpatienceType impatienceType() {
    return impatienceType;
  }

  @Override
  public boolean isImpatienceType(ImpatienceType type) {
    return impatienceType == type;
  }

  @Override
  public void generateImpatience(ImpatienceMeasurement impatienceObject) {
    if (!(impatienceObject instanceof DoubleValueImpatienceMeasurement)) {
      throw new IllegalArgumentException("Supplied argument for generateImpatience() method in "
          + "Retrial must be of type DoubleValueImpatienceMeasurement.");
    }
    Double retrialDelay = 0.0;

    try {
      retrialDelay = distribution.nextRand(parameter);
      if (retrialDelay < 0.0) {
        retrialDelay = 0.0;
      }
    } catch (IncorrectDistributionParameterException e) {
      e.printStackTrace();
    }
//
//    MutableDouble impatienceObjectAsDouble = (DoubleValueImpatienceMeasurement) impatienceObject;
//    impatienceObjectAsDouble.setValue(retrialDelay);
  }

  public double generateRandomDelay() {
    double randomDelay = new DoubleValueImpatienceMeasurement().doubleValue();

    try {
      randomDelay = exponential.nextRand(this.parameter);

    } catch (IncorrectDistributionParameterException e) {
      e.printStackTrace();
    }
    return randomDelay;
  }
}
