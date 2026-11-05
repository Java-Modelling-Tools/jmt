package NetStrategies.ImpatienceStrategies;

import NetStrategies.ImpatienceStrategies.ImpatienceMeasurement.ImpatienceMeasurement;

/**
 * Base class of the JSIM impatience strategies, which model customers leaving a queue before being served.
 */
/**
 * Base class of the JSIM impatience strategies, which model customers leaving a queue before being served.
 */
/**
 * Base class of the JSIM impatience strategies, which model customers leaving a queue before being served.
 */
/**
 * Base class of the JSIM impatience strategies, which model customers leaving a queue before being served.
 */
public interface Impatience {
  ImpatienceType impatienceType();
  boolean isImpatienceType (ImpatienceType type);
  void generateImpatience(ImpatienceMeasurement impatienceObject);
}
