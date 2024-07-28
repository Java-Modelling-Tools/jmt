package jmt.jmva.analytical.solvers.dispatchers;

import jmt.common.exception.InputDataException;
import jmt.common.exception.UnsupportedModelException;
import jmt.jmva.analytical.ExactModel;
import jmt.jmva.analytical.solvers.SolverAlgorithm;

public class ModelSolverCompatibilityChecker {

    public static void checkCompatibility(ExactModel model, double[] classData, SolverAlgorithm algorithmType) throws UnsupportedModelException, InputDataException {
        checkModelAlgorithmCompatability(model, algorithmType);
        checkClassPopulationsAndArrivalRates(model, classData);
    }

    public static void checkClassPopulationsAndArrivalRates(ExactModel model, double[] classData) throws InputDataException {
        // First of all controls that every class has population or rate greater than 0.
        // Otherwise throws a InputDataException
        for (double element : classData) {
            if (element <= 0) {
                // error: population or rate not greater than 0.0
                // prepare message according to model type (mixed, open or closed)
                if (model.isMixed()) {
                    // mixed model -> populations or rates
                    throw new InputDataException("Populations and arrival rates must be greater than zero");
                } else if (model.isOpen()) {
                    // open model -> rates
                    throw new InputDataException("Arrival rates must be greater than zero");
                } else {
                    // closed model -> populations
                    throw new InputDataException("Populations must be greater than zero");
                }
            }
        }
    }


    public static void checkModelAlgorithmCompatability(ExactModel model, SolverAlgorithm algorithmType) throws UnsupportedModelException {
        if (!algorithmType.supportsLoadDependent()) {
            checkLDModel(model);
        }
        if (!algorithmType.supportsPriorities()) {
            checkPriorityModel(model);
        }
        // !Closed = Open or Mixed model
        if (!model.isClosed() && !algorithmType.supportsOpenClasses()) {
           throwCantHandleClassType("open");
        }
        // !Open = Closed or Mixed model
        if (!model.isOpen() && !algorithmType.supportsClosedClasses()) {
            throwCantHandleClassType("closed");
        }
    }

    private static void checkLDModel(ExactModel model) throws UnsupportedModelException {
        if (model.isLd()) {
            throwCantHandleStation("load-dependent");
        }
    }

    private static void checkPriorityModel(ExactModel model) throws UnsupportedModelException {
        if (model.isPriority()) {
            throwCantHandleStation("priority");
        }
    }

    private static void throwCantHandleClassType(String classType) throws UnsupportedModelException {
        throw new UnsupportedModelException(String.format(
                "The selected solver cannot handle %s classes, please choose another.", classType));
    }

    private static void throwCantHandleStation(String stationType) throws UnsupportedModelException {
        throw new UnsupportedModelException(String.format(
                "The selected solver cannot handle %s stations, please choose another.", stationType));
    }
}
