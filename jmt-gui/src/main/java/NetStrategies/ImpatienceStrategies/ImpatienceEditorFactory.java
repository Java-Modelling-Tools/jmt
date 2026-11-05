package NetStrategies.ImpatienceStrategies;

import java.awt.Container;
import common.distributions.Distribution;
import common.editors.BalkingStrategyEditor;
import common.editors.DistributionsEditor;
import common.serviceStrategies.LDStrategy;

/**
 * Factory creating the impatience editor objects.
 */
/**
 * Factory creating the impatience editor objects.
 */
/**
 * Factory creating the impatience editor objects.
 */
/**
 * Factory creating the impatience editor objects.
 */
public class ImpatienceEditorFactory {
  public static ImpatienceParameter displayEditorAndReturnParameter
      (ImpatienceType impatienceType, ImpatienceParameter impatienceParameter, Container parent, String className) {
    switch (impatienceType) {
      case RENEGING:
        Distribution distribution = ((RenegingParameter) impatienceParameter).getDistribution();
        DistributionsEditor renegingEditor = DistributionsEditor
            .getInstance(parent, distribution);
        // Sets editor window title
        renegingEditor.setTitle(
            "Editing " + className + " Reneging Time Distribution...");
        // Shows editor window
        renegingEditor.show();
        return new RenegingParameter(renegingEditor.getResult());

      case BALKING:
        LDStrategy ldStrategy = ((BalkingParameter) impatienceParameter).getLdStrategy();
        BalkingStrategyEditor balkingEditor = BalkingStrategyEditor
            .getInstance(parent, ldStrategy);
        // Sets editor window title
        balkingEditor.setTitle("Editing " + className + " Balking Probability...");
        // Shows editor window
        balkingEditor.show();
        return impatienceParameter;

      default:
        return null;
    }
  }
}
