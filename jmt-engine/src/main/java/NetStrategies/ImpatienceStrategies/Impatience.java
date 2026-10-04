package NetStrategies.ImpatienceStrategies;

import NetStrategies.ImpatienceStrategies.ImpatienceMeasurement.ImpatienceMeasurement;

public interface Impatience {
  ImpatienceType impatienceType();
  boolean isImpatienceType (ImpatienceType type);
  void generateImpatience(ImpatienceMeasurement impatienceObject);
}
