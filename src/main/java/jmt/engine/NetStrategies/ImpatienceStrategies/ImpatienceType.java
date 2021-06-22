package jmt.engine.NetStrategies.ImpatienceStrategies;

public enum ImpatienceType {
  NONE("None"),
  IMPATIENCE("Impatience"),
  RENEGING("Reneging"),
  RETRIAL("Retrial"),
  BALKING("Balking");

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
    return ImpatienceType.valueOf(impatienceString);
  }
}
