package jmt.engine.NetStrategies.ImpatienceStrategies;

// import com.google.common.base.Preconditions;
import java.util.Random;
import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceMeasurement.BooleanValueImpatienceMeasurement;
import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceMeasurement.ImpatienceMeasurement;
import jmt.engine.NetStrategies.ServiceStrategies.LDParameter;
import jmt.engine.NetStrategies.ServiceStrategies.LoadDependentStrategy;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomAdaptor;

public class Balking implements Impatience {
  private ImpatienceType impatienceType;
  private LoadDependentStrategy loadDependentStrategy;
  private boolean priorityActivated;
  private final Random randomGenerator;

  public Balking(LoadDependentStrategy loadDependentStrategy, Boolean priorityActivated){
    this(loadDependentStrategy, priorityActivated, System.nanoTime());
  }

  // For purposes of unit testing. Might be removed some time in the future
  public Balking(LoadDependentStrategy loadDependentStrategy, Boolean priorityActivated, long seed) {
    this.loadDependentStrategy = loadDependentStrategy;
    this.priorityActivated = priorityActivated;
    this.randomGenerator = new RandomAdaptor(new MersenneTwister(seed));

    if (loadDependentStrategy != null) {
      impatienceType = ImpatienceType.BALKING;
    }
  }

  public boolean isPriorityActivated() {
    return priorityActivated;
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
    if (!(impatienceObject instanceof BooleanValueImpatienceMeasurement)) {
      throw new IllegalArgumentException("Supplied argument for generateImpatience() method in "
          + "Balking must be of type BooleanValueImpatienceMeasurement.");
    }

    BooleanValueImpatienceMeasurement measurement = (BooleanValueImpatienceMeasurement) impatienceObject;
    int queueLength = measurement.getQueueLength();
    double balkingProbability;
    LDParameter[] parameters = loadDependentStrategy.getParameters();

    // If queueLength = 0, it not possible at all for the job to balk. This is just a safeguard.
    if (queueLength <= 0) {
      measurement.setBooleanValue(false);
      return;
    }

    // Find the range to which the queueLength belongs and extract the probability from that range
    balkingProbability = extractProbabilityFromCorrectLDParameter(queueLength, parameters);

    // Finally, decide if the job will balk
    if (randomGenerator.nextDouble() <= balkingProbability) {
      measurement.setBooleanValue(true);
    } else {
      measurement.setBooleanValue(false);
    }
  }

  private Double extractProbabilityFromCorrectLDParameter(int queueLength, LDParameter[] parameters) {
    double probabilityOfParamWithQueueLengthInItsRange = 0.0;

    for (LDParameter parameter : parameters) {
      int fromValue = parameter.getMinJobs();

      if (fromValue > queueLength) {
        break;
      }

      probabilityOfParamWithQueueLengthInItsRange = Double.parseDouble(parameter.getFunction());
    }

    return probabilityOfParamWithQueueLengthInItsRange;
  }
}
