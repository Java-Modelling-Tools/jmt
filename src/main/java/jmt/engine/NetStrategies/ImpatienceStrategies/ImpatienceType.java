<<<<<<< HEAD
package jmt.engine.NetStrategies.ImpatienceStrategies;

public enum ImpatienceType {
  NONE("None"),
  BALKING("Balking"),
  RENEGING("Reneging");

  // The display name of that enum type in the UI
  private String displayName;

  ImpatienceType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public static ImpatienceType getType(String impatienceString) {
    impatienceString = impatienceString.toUpperCase();
    try {
    	return ImpatienceType.valueOf(impatienceString);
    } catch (IllegalArgumentException e) {
    	return ImpatienceType.NONE;
    }
  }
}
=======
package jmt.engine.NetStrategies.ImpatienceStrategies;

public enum ImpatienceType {
  NONE("None"),
  BALKING("Balking"),
  RENEGING("Reneging");

  // The display name of that enum type in the UI
  private String displayName;

  ImpatienceType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public static ImpatienceType getType(String impatienceString) {
    impatienceString = impatienceString.toUpperCase();
    try {
    	return ImpatienceType.valueOf(impatienceString);
    } catch (IllegalArgumentException e) {
    	return ImpatienceType.NONE;
    }
  }
}
>>>>>>> b0a37c45 (Initial commit at SVN r1153.)
