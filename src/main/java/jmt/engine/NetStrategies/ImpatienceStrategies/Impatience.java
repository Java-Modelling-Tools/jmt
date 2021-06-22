package jmt.engine.NetStrategies.ImpatienceStrategies;

import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceMeasurement.ImpatienceMeasurement;

public interface Impatience {
  ImpatienceType impatienceType();
  boolean isImpatienceType (ImpatienceType type);
  void generateImpatience(ImpatienceMeasurement impatienceObject);
}
