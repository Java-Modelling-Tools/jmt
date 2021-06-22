package jmt.engine.NetStrategies.ImpatienceStrategies;

import jmt.common.exception.IncorrectDistributionParameterException;
import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceMeasurement.DoubleValueImpatienceMeasurement;
import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceMeasurement.ImpatienceMeasurement;
import jmt.engine.random.Distribution;
import jmt.engine.random.Parameter;
import org.apache.commons.lang.mutable.MutableDouble;

public class Reneging implements Impatience {

  private Distribution distribution;
  private Parameter parameter;
  private ImpatienceType impatienceType;

  public Reneging(Distribution distribution, Parameter parameter) {
    this.distribution = distribution;
    this.parameter = parameter;

    if (distribution != null && parameter != null) {
      impatienceType = ImpatienceType.RENEGING;
    }
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
          + "Reneging must be of type DoubleValueImpatienceMeasurement.");
    }

    double renegingDelay = 0.0;

    try {
      renegingDelay = distribution.nextRand(parameter);
      if (renegingDelay < 0.0) {
        renegingDelay = 0.0;
      }
    } catch (IncorrectDistributionParameterException e) {
      e.printStackTrace();
    }

    MutableDouble impatienceObjectAsDouble = (DoubleValueImpatienceMeasurement) impatienceObject;
    impatienceObjectAsDouble.setValue(renegingDelay);
  }
}
