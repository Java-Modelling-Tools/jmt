package jmt.jmva.analytical.solvers.dispatchers;

import jmt.common.exception.InputDataException;
import jmt.common.exception.SolverException;
import jmt.framework.data.ArrayUtils;
import jmt.jmva.analytical.ExactConstants;
import jmt.jmva.analytical.ExactModel;

public class WhatIfAnalysisDispatcher {

    private final SolverDispatcher solverDispatcher;

    public WhatIfAnalysisDispatcher(SolverDispatcher solverDispatcher) {
        this.solverDispatcher = solverDispatcher;
    }

    // --- What-if Analysis methods --- Bertoli Marco -------------------------------------
    /**
     * Performs a what-if analysis by changing arrival rates.
     * @param model input model
     */
    public void whatIfArrival(ExactModel model) throws InputDataException, SolverException {
        // Sanity checks on input model.
        if (model.getWhatIfClass() >= 0 && model.getClassTypes()[model.getWhatIfClass()] != ExactConstants.CLASS_OPEN) {
            throw new InputDataException("Cannot change arrival rate of a closed class.");
        }
        if (model.isClosed()) {
            throw new InputDataException("Cannot change arrival rates in a closed model.");
        }
        // Values for what-if
        double[] values = model.getWhatIfValues();

        // Backup class data
        double[] initials = model.getClassData().clone();

        // Iterates for what-if executions
        for (int i = 0; i < model.getWhatIfValues().length && !solverDispatcher.hasStopped(); i++) {
            double[] current = initials.clone();
            // If this is one class only
            if (model.getWhatIfClass() >= 0) {
                current[model.getWhatIfClass()] = values[i];
            }
            // If this is all open classes
            else {
                for (int j = 0; j < current.length; j++) {
                    if (model.getClassTypes()[j] == ExactConstants.CLASS_OPEN) {
                        current[j] = initials[j] * values[i];
                    }
                }
            }
            model.setClassData(current);

            // Checks if stopped
            if (solverDispatcher.hasStopped()) {
                break;
            }

            // Now solves current model - we cannot interrupt this as it is not designed to
            // be done.
            solverDispatcher.finalDispatch(model, i);
        }
        // Resets initial model
        model.setClassData(initials);
        // Results are ok if the process was not stopped.
        model.setResultsOK(!solverDispatcher.hasStopped());
    }

    /**
     * Performs a what-if analysis by changing the number of customers.
     * @param model input model
     */
    public void whatIfCustomers(ExactModel model) throws InputDataException, SolverException {
        // Sanity checks on input model.
        if (model.getWhatIfClass() >= 0 && model.getClassTypes()[model.getWhatIfClass()] != ExactConstants.CLASS_CLOSED) {
            throw new InputDataException("Cannot change the number of customers of an open class.");
        }
        if (model.isOpen()) {
            throw new InputDataException("Cannot change the number of customers in an open model.");
        }
        if (model.isLd()) {
            throw new InputDataException("Cannot change the number of customers in a load-dependent model.");
        }

        // Values for what-if
        double[] values = model.getWhatIfValues();

        // Backup class data
        double[] initials = model.getClassData().clone();

        // Iterates for what-if executions
        int i;
        for (i = 0; i < model.getWhatIfValues().length && !solverDispatcher.hasStopped(); i++) {
            double[] current = initials.clone();
            // If this is one class only
            if (model.getWhatIfClass() >= 0) {
                current[model.getWhatIfClass()] = values[i];
                // Check for not integer values
                if (Math.abs(current[model.getWhatIfClass()] - Math.rint(current[model.getWhatIfClass()])) > 1e-8) {
                    throw new InputDataException("A fractional population value was assigned to class "
                            + model.getClassNames()[model.getWhatIfClass()] + " during step " + i);
                }
                // Rounds number to avoid truncation problems
                current[model.getWhatIfClass()] = Math.round(current[model.getWhatIfClass()]);

            }
            // If this is all closed classes
            else {
                for (int j = 0; j < current.length; j++) {
                    if (model.getClassTypes()[j] == ExactConstants.CLASS_CLOSED) {
                        current[j] = initials[j] * values[i];
                        // Check for not integer values
                        if (Math.abs(current[j] - Math.rint(current[j])) > 1e-8) {
                            throw new InputDataException("A fractional population value was assigned to class " + model.getClassNames()[j]
                                    + " during step " + i);
                        }
                        // Rounds number to avoid truncation problems
                        current[j] = Math.round(current[j]);
                    }
                }
            }
            model.setClassData(current);

            // Checks if stopped
            if (solverDispatcher.hasStopped()) {
                break;
            }

            // Now solves current model - we cannot interrupt this as it is not designed to
            // be done.
            solverDispatcher.finalDispatch(model, i);
        }
        // Resets initial model
        model.setClassData(initials);
        // Results are ok if the process was not stopped.
        model.setResultsOK(!solverDispatcher.hasStopped());
    }

    /**
     * Performs a what-if analysis by changing service demands of a given station.
     * @param model input model
     */
    public void whatIfDemands(ExactModel model) throws InputDataException, SolverException {
        // Sanity checks on input model.
        if (model.getWhatIfStation() < 0 || model.getWhatIfStation() >= model.getStations()) {
            throw new InputDataException("Station for what-if analysis not specified.");
        }
        if (model.getStationTypes()[model.getWhatIfStation()] == ExactConstants.STATION_LD) {
            throw new InputDataException("Service Demands what-if analysis not supported on Load Dependent stations.");
        }

        // Values for what-if
        double[] values = model.getWhatIfValues();

        // Backup service times data (note: we multiply only service times as it is the
        // same of multiply service demands)
        double[][][] initials = ArrayUtils.copy3(model.getServiceTimes());

        // Saves what-if class and station indices
        int cl = model.getWhatIfClass();
        int st = model.getWhatIfStation();

        // Iterates for what-if executions
        int i;
        for (i = 0; i < values.length && !solverDispatcher.hasStopped(); i++) {
            double[][][] current = ArrayUtils.copy3(initials);
            // If this is one class only
            if (cl >= 0) {
                if (model.getVisits()[st][cl] > 0) {
                    current[st][cl][0] = values[i] / model.getVisits()[st][cl];
                } else {
                    current[st][cl][0] = 0.0;
                }
            }
            // If this is all classes
            else {
                for (int j = 0; j < model.getClasses(); j++) {
                    current[st][j][0] = initials[st][j][0] * values[i];
                }
            }
            model.setServiceTimes(current);

            // Checks if stopped
            if (solverDispatcher.hasStopped()) {
                break;
            }

            // Now solves current model - we cannot interrupt this as it is not designed to
            // be done.
            solverDispatcher.finalDispatch(model, i);
        }
        // Resets initial model
        model.setServiceTimes(initials);
        // Results are ok if the process was not stopped.
        model.setResultsOK(!solverDispatcher.hasStopped());
    }

    /**
     * Performs a what-if analysis by changing population mix.
     * @param model input model
     */
    public void whatIfMix(ExactModel model) throws InputDataException, SolverException {
        // First and second closed class for population mix what-if
        int class1, class2 = -1;
        class1 = model.getWhatIfClass();
        if (class1 < 0) {
            throw new InputDataException("Class not specified for population mix what-if analysis.");
        }
        // Find second class
        for (int i = 0; i < model.getClasses(); i++) {
            if (model.getClassTypes()[i] == ExactConstants.CLASS_CLOSED && i != class1) {
                if (class2 < 0) {
                    class2 = i;
                } else {
                    throw new InputDataException("Only models with two closed classes are supported. More than two classes detected.");
                }
            }
        }
        if (class2 < 0) {
            throw new InputDataException("Only models with two closed classes are supported. Only one classes detected.");
        }

        // Values for what-if
        double[] values = model.getWhatIfValues();

        // Backup class data
        double[] initials = model.getClassData().clone();

        // Value for total number of customer
        double N = initials[class1] + initials[class2];

        // Iterates for what-if executions
        int i;
        for (i = 0; i < model.getWhatIfValues().length && !solverDispatcher.hasStopped(); i++) {
            double[] current = initials.clone();
            current[class1] = values[i] * N;
            current[class2] = (1 - values[i]) * N;
            // Check for not integer values
            if (Math.abs(current[class1] - Math.rint(current[class1])) > 1e-8) {
                throw new InputDataException("A fractional population value was assigned to class " + model.getClassNames()[class1]
                        + " during step " + i);
            } else if (Math.abs(current[class2] - Math.rint(current[class2])) > 1e-8) {
                throw new InputDataException("A fractional population value was assigned to class " + model.getClassNames()[class2]
                        + " during step " + i);
            }
            // Rounds number to avoid truncation problems
            current[class1] = Math.round(current[class1]);
            current[class2] = Math.round(current[class2]);

            model.setClassData(current);

            // Checks if stopped
            if (solverDispatcher.hasStopped()) {
                break;
            }

            // Now solves current model - we cannot interrupt this as it is not designed to
            // be done.
            solverDispatcher.finalDispatch(model, i);
        }
        // Resets initial model
        model.setClassData(initials);
        // Results are ok if the process was not stopped.
        model.setResultsOK(!solverDispatcher.hasStopped());
    }

}
