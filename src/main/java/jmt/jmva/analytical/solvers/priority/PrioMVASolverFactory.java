package jmt.jmva.analytical.solvers.priority;

import jmt.common.exception.SolverException;
import jmt.jmva.analytical.ExactModel;
import jmt.jmva.analytical.solvers.dispatchers.DispatcherUtil;
import jmt.jmva.analytical.solvers.multiSolverAlgorithms.SolverMultiMixedPrioMVA;
import jmt.jmva.analytical.solvers.priority.prioMVACalculators.implementations.utilisations.*;
import jmt.jmva.analytical.solvers.utilities.ConstantConvertors;

/**
 * A class to initialise the different variations of PrioMVA
 */
public class PrioMVASolverFactory {

    public static SolverMultiMixedPrioMVA initialiseNewPriorMVA(SolverMultiMixedPrioMVA pmvaSolver, ExactModel model) throws SolverException {
        int[] classTypes = ConstantConvertors.mapClassTypes(model.getClassTypes());
        int[] stationTypes = ConstantConvertors.mapStationTypes(model.getStationTypes(), true);
        if (!pmvaSolver.input(model.getStationNames(), stationTypes, model.getServiceTimes(), model.getVisits(),
                model.getClassData(), classTypes, model.getClassPriorities())) {
            DispatcherUtil.fail("Error initializing PriorityMVAMultiSolver", null);
        }
        return pmvaSolver;
    }

    public static SolverMultiMixedPrioMVA getBKTPrioMVA(ExactModel model) throws SolverException {
        return initialiseNewPriorMVA(
                new SolverMultiMixedPrioMVA(model.getClasses(), model.getStations(), new BKTPrioMVAUtilCalculator()),
                model);
    }

    public static SolverMultiMixedPrioMVA getTPrioMVA(ExactModel model) throws SolverException {
        return initialiseNewPriorMVA(
                new SolverMultiMixedPrioMVA(model.getClasses(), model.getStations(), new TPrioMVAUtilCalculator()),
                model);
    }

    public static SolverMultiMixedPrioMVA getCLPrioMVA(ExactModel model) throws SolverException {
        return initialiseNewPriorMVA(
                new SolverMultiMixedPrioMVA(model.getClasses(), model.getStations(), new CLPrioMVAUtilCalculator()),
                model);
    }

    public static SolverMultiMixedPrioMVA getCL_ZPrioMVA(ExactModel model) throws SolverException {
        return initialiseNewPriorMVA(
                new SolverMultiMixedPrioMVA(model.getClasses(), model.getStations(), new CL_ZPrioMVAUtilCalculator()),
                model);
    }

    public static SolverMultiMixedPrioMVA getCL_YPrioMVA(ExactModel model) throws SolverException {
        return initialiseNewPriorMVA(
                new SolverMultiMixedPrioMVA(model.getClasses(), model.getStations(), new CL_YPrioMVAUtilCalculator()),
                model);
    }

    public static SolverMultiMixedPrioMVA getNLI_LIPrioMVA(ExactModel model) throws SolverException {
        return initialiseNewPriorMVA(
                new SolverMultiMixedPrioMVA(model.getClasses(), model.getStations(), new NLI_LIPrioMVAUtilCalculator()),
                model);
    }

    public static SolverMultiMixedPrioMVA getNLI_ZPrioMVA(ExactModel model) throws SolverException {
        return initialiseNewPriorMVA(
                new SolverMultiMixedPrioMVA(model.getClasses(), model.getStations(), new NLI_ZPrioMVAUtilCalculator()),
                model);
    }

    public static SolverMultiMixedPrioMVA getNLI_YPrioMVA(ExactModel model) throws SolverException {
        return initialiseNewPriorMVA(
                new SolverMultiMixedPrioMVA(model.getClasses(), model.getStations(), new NLI_YPrioMVAUtilCalculator()),
                model);
    }

    public static SolverMultiMixedPrioMVA getRNLI_NCPrioMVA(ExactModel model) throws SolverException {
        return initialiseNewPriorMVA(
                new SolverMultiMixedPrioMVA(model.getClasses(), model.getStations(), new RNLI_NCPrioMVAUtilCalculator()),
                model);
    }

    public static SolverMultiMixedPrioMVA getRNLI_LIPrioMVA(ExactModel model) throws SolverException {
        return initialiseNewPriorMVA(
                new SolverMultiMixedPrioMVA(model.getClasses(), model.getStations(), new RNLI_LIPrioMVAUtilCalculator()),
                model);
    }

    public static SolverMultiMixedPrioMVA getRNLI_ZPrioMVA(ExactModel model) throws SolverException {
        return initialiseNewPriorMVA(
                new SolverMultiMixedPrioMVA(model.getClasses(), model.getStations(), new RNLI_ZPrioMVAUtilCalculator()),
                model);
    }

    public static SolverMultiMixedPrioMVA getRNLI_YPrioMVA(ExactModel model) throws SolverException {
        return initialiseNewPriorMVA(
                new SolverMultiMixedPrioMVA(model.getClasses(), model.getStations(), new RNLI_YPrioMVAUtilCalculator()),
                model);
    }
}
