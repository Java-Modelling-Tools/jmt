/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package jmt.jmva.analytical.solvers.dispatchers;

import jmt.common.exception.InputDataException;
import jmt.common.exception.SolverException;
import jmt.common.exception.UnsupportedModelException;
import jmt.framework.data.ArrayUtils;
import jmt.framework.xml.XMLUtils;
import jmt.jmva.analytical.ExactConstants;
import jmt.jmva.analytical.ExactModel;
import jmt.jmva.analytical.ModelFESCApproximator;
import jmt.jmva.analytical.solvers.*;
import jmt.jmva.analytical.solvers.dataStructures.BigRational;
import jmt.jmva.analytical.solvers.dispatchers.wrappers.ClosedMultiToSingleSolverWrapper;
import jmt.jmva.analytical.solvers.dispatchers.wrappers.IterativeClosedMultiToSingleSolverWrapper;
import jmt.jmva.analytical.solvers.multiSolverAlgorithms.*;
import jmt.jmva.analytical.solvers.singleSolverAlgorithms.SolverSingleClosedMVA;
import jmt.jmva.analytical.solvers.singleSolverAlgorithms.SolverSingleOpen;
import jmt.jmva.analytical.solvers.utilities.ConstantConvertors;
import org.xml.sax.SAXException;

import java.io.File;

/**
 * Server side of the solver interface.<br>
 * This object takes a model, instantiates the correct solver, and solves
 * it.<br>
 * Should probably be rewritten using a different data structure to hold model
 * information
 *
 * @author unknown, Bertoli Marco (what-if analysis)
 */
public class SolverDispatcher {

    private static final boolean PRINTMODEL = false;

    private boolean stopped = false;

    private final XMLUtils xmlUtils;
    /** Used to notify when a computation ends */
    private SolverListener listener;

    private final WhatIfAnalysisDispatcher whatIfAnalysisDispatcher;

    private ExactModel model;

    public SolverDispatcher() {
        xmlUtils = new XMLUtils();
        whatIfAnalysisDispatcher = new WhatIfAnalysisDispatcher(this);
    }

    /**
     * Solves the model in file.
     * @throws InputDataException if some input data are malformed
     * @throws SolverException    if something goes wrong during solution
     */
    public void solve(File file) throws InputDataException, SolverException {
        model = new ExactModel();
        try {
            if (!model.loadDocument(xmlUtils.loadXML(file))) {
                DispatcherUtil.fail("Error loading model from tempfile", null);
            }
        } catch (SAXException e) {
            DispatcherUtil.fail("XML parse error in tempfile", e);
        } catch (Exception e) {
            DispatcherUtil.fail("Error loading model from tempfile", e);
        }

        if (PRINTMODEL) {
            System.out.println(model);
        }

        solve(model);

        if (PRINTMODEL) {
            System.out.println(model);
        }

        try {
            if (!xmlUtils.saveXML(model.createDocument(), file)) {
                DispatcherUtil.fail("Error saving solved model to tempfile", null);
            }
        } catch (SAXException e) {
            DispatcherUtil.fail("XML parse error in tempfile", e);
        } catch (Exception e) {
            DispatcherUtil.fail("Error saving solved model to tempfile", e);
        }

    }

    /**
     * Stops What-if analysis and invalidates results
     */
    public void stop() {
        stopped = true;
    }

    public boolean hasStopped() {
        return stopped;
    }

    /**
     * Solves input model. This method will look for what-if analysis data and
     * perform a what-if analysis if requested.<br>
     * Author: Bertoli Marco
     * @param model model to be solved
     * @throws InputDataException if some input data are malformed
     * @throws SolverException    if something goes wrong during solution
     */
    public void solve(ExactModel model) throws InputDataException, SolverException {
        stopped = false;
        model.resetResults();
        // Solves normal models
        if (!model.isWhatIf()) {
            finalDispatch(model, 0);
        }
        // Now solves what-if models
        else {
            // Arrival rates what-if analysis
            if (model.getWhatIfType().equalsIgnoreCase(ExactConstants.WHAT_IF_ARRIVAL)) {
                whatIfAnalysisDispatcher.whatIfArrival(model);
            }
            // Customers number what-if analysis
            else if (model.getWhatIfType().equalsIgnoreCase(ExactConstants.WHAT_IF_CUSTOMERS)) {
                whatIfAnalysisDispatcher.whatIfCustomers(model);
            }
            // Service demands what-if analysis
            else if (model.getWhatIfType().equalsIgnoreCase(ExactConstants.WHAT_IF_DEMANDS)) {
                whatIfAnalysisDispatcher.whatIfDemands(model);
            }
            // Population mix what-if analysis
            else if (model.getWhatIfType().equalsIgnoreCase(ExactConstants.WHAT_IF_MIX)) {
                whatIfAnalysisDispatcher.whatIfMix(model);
            }
        }
    }

    public void finalDispatch(ExactModel model, int iteration) throws InputDataException, SolverException {
        /* disable all change-checking */
        model.discardChanges();
        model.setChanged();

        ModelFESCApproximator fesc = new ModelFESCApproximator(model, iteration);
        try {
            if (model.isWhatIf() && model.isWhatifAlgorithms()) {
                SolverAlgorithm origAlgType = model.getAlgorithmType();
                double origAlgTolerance = model.getTolerance();
                int origAlgMaxSamples = model.getMaxSamples();
                for (SolverAlgorithm algo : model.getWhatifAlgorithms()) {
                    model.setAlgorithmType(algo);
                    model.setTolerance(model.getWhatifAlgorithmTolerance(algo));
                    model.setMaxSamples(model.getWhatifAlgorithmMaxSamples(algo));
                    if (model.isMultiClass()) {
                        SolverMultiDispatcher.solveMulti(fesc.getModelToBeSolved(), iteration);
                    } else {
                        solveSingle(fesc.getModelToBeSolved(), iteration);
                    }
                }
                model.setAlgorithmType(origAlgType);
                model.setTolerance(origAlgTolerance);
                model.setMaxSamples(origAlgMaxSamples);
            } else {
                if (model.isOpen() || model.isMultiClass()) {
                    SolverMultiDispatcher.solveMulti(fesc.getModelToBeSolved(), iteration);
                } else {
                    solveSingle(fesc.getModelToBeSolved(), iteration);
                }
            }
            // set boolean to notify results have been computed
            model.setResultsBooleans(true);

            // Notify termination of current model solution
            if (listener != null) {
                listener.computationTerminated(iteration);
            }
        } catch (InputDataException e) {
            throw e;
        } catch (SolverException e) {
            throw e;
        } catch (Exception e) {
            DispatcherUtil.fail("Unhandled exception", e);
        }
        fesc.processModelAfterSolution();
    }

    private void solveSingle(ExactModel model, int iteration) throws UnsupportedModelException, InputDataException, SolverException {
        // TODO: Convert this to use MultiSolver, the methods are really similar
        int stations = model.getStations();
        Solver solver = null;
        SolverAlgorithm algorithmType = model.getAlgorithmType();
        int algIterations = 0;

        // init
        String[] names = model.getStationNames();
        int[] types = ConstantConvertors.mapStationTypes(model.getStationTypes(), false);
        double[][] serviceTimes = ArrayUtils.extract13(model.getServiceTimes(), 0);
        // no supplemental copy here since extract13 already copies the first level of
        // the array
        adjustLD(serviceTimes, types);
        double[] visits = ArrayUtils.extract1(model.getVisits(), 0);

        double tolerance = model.getTolerance();
        int maxSamples = model.getMaxSamples();

        ModelSolverCompatibilityChecker.checkModelAlgorithmCompatability(model, algorithmType);

        try {
            if (model.isClosed()) {
                // single closed
                int pop = (int) model.getClassData()[0];
                // First of all controls that the closed class has population greater than 0.
                // Otherwise throws a InputDataException
                if (pop <= 0) {
                    // error: population is not greater than 0.0
                    throw new InputDataException("Population must be greater than zero");
                }

                boolean alreadyInitialised = false;
                if (SolverAlgorithm.EXACT.equals(algorithmType)) {
                    solver = new SolverSingleClosedMVA(pop, stations);
                } else if (SolverAlgorithm.MONTE_CARLO_LOGISTIC.equals(algorithmType)) {
                    solver = new SolverSingleClosedMonteCarloLogistic(pop, stations,
                            maxSamples, SolverMultiClosedMonteCarloLogistic.DEFAULT_PRECISION);
                } else if (SolverAlgorithm.RECAL.equals(algorithmType)) {
                    solver = new SolverSingleClosedRECAL(pop, stations);
                } else if (SolverAlgorithm.COMOM.equals(algorithmType)) {
                    // in single-class we run RECAL as COMOM initialization, since it is meant for
                    // models with few stations
                    solver = new SolverSingleClosedRECAL(pop, stations);
                } else if (algorithmType.isIterative()) {
                    solver = new IterativeClosedMultiToSingleSolverWrapper(algorithmType, model, tolerance);
                    alreadyInitialised = true;
                } else {
                    solver = new ClosedMultiToSingleSolverWrapper(algorithmType, model);
                    alreadyInitialised = true;
                }

                // alreadyInitialised short-circuts the if statement so input isn't called twice
                if (!alreadyInitialised && !solver.input(names, types, serviceTimes, visits)) {
                    String algName = algorithmType.toString().replace(" ", "").replace("-", "");
                    DispatcherUtil.fail("Error initializing " + algName + "SingleSolver", null);
                    // DispatcherUtil.fail("Error initializing MVASolver", null);
                }
            } else {
                algorithmType = SolverAlgorithm.EXACT;
                // TODO this is not used any more (multi solver is used instead)
                /* single open */
                double lambda = model.getClassData()[0];
                // First of all controls that the open class has rate greater than 0.
                // Otherwise throws a InputDataException
                if (lambda <= 0) {
                    // error: rate is not greater than 0.0
                    throw new InputDataException("Arrival rate must be greater than zero");
                }
                solver = new SolverSingleOpen(lambda, stations);
                if (!solver.input(names, types, serviceTimes, visits)) {
                    DispatcherUtil.fail("Error initializing OpenSolver", null);
                }
            }
        } catch (Exception e) {
            DispatcherUtil.fail("Error initializing SingleClass solver", e);
        }
        // controls processing capacity
        if (!solver.hasSufficientProcessingCapacity()) {
            throw new InputDataException("One or more resources are in saturation. Decrease arrival rates or service demands.");
        }

        /* solve */
        solver.solve();

        double logNC = Double.NaN;
        if (solver instanceof IterativeClosedMultiToSingleSolverWrapper) {
            algIterations = ((IterativeClosedMultiToSingleSolverWrapper) solver).getIterations();
        } else if (solver instanceof SolverSingleClosedRECAL) {
            BigRational G = ((SolverSingleClosedRECAL) solver).solver.qnm.getNormalisingConstant();
            logNC = G.log() - 5 * Math.log(10) * ((SolverSingleClosedRECAL) solver).solver.qnm.N.sum();
        } else if (solver instanceof SolverSingleClosedCoMoM) {
            BigRational G = ((SolverSingleClosedCoMoM) solver).solver.qnm.getNormalisingConstant();
            logNC = G.log() - 5 * Math.log(10) * ((SolverSingleClosedCoMoM) solver).solver.qnm.N.sum();
        } else if (solver instanceof SolverSingleClosedMonteCarloLogistic) {
            BigRational G = ((SolverSingleClosedMonteCarloLogistic) solver).solver.qnm.getNormalisingConstant();
            logNC = G.log();
        }

        /* solution */
        double[][] ql = ArrayUtils.makeFilled(stations, 1, -1);
        ArrayUtils.insert1(ql, solver.getQueueLen(), 0);

        double[][] tp = ArrayUtils.makeFilled(stations, 1, -1);
        ArrayUtils.insert1(tp, solver.getThroughput(), 0);

        double[][] rt = ArrayUtils.makeFilled(stations, 1, -1);
        ArrayUtils.insert1(rt, solver.getResTime(), 0);

        double[][] util = ArrayUtils.makeFilled(stations, 1, -1);
        ArrayUtils.insert1(util, solver.getUtilization(), 0);

        model.setResults(algorithmType, algIterations, ql, tp, rt, util, logNC, iteration);
    }

    /** HACK: adds an initial zero to all LD stations */
    private void adjustLD(double[][] st, int[] types) {
        for (int i = 0; i < st.length; i++) {
            if (types[i] == Solver.LD) {
                st[i] = ArrayUtils.prepend0(st[i]);
            }
        }
    }

    // ------------------------------------------------------------------------------------

    // ---- Callbacks ---------------------------------------------------------------------

    /**
     * Adds a solver listener to be notified when computation of an iteration
     * terminates. This is useful for notification of a progress window. Only one
     * listener is allowed.
     * @param listener listener to be added or null to remove previous one.
     */
    public void addSolverListener(SolverListener listener) {
        this.listener = listener;
    }

    /**
     * Listener used to notify when computation of a model is terminated.
     */
    public interface SolverListener {
        /**
         * This method is called each time the computation of a model is terminated.
         * @param num number of computed model (used for iterated solutions).
         */
        public void computationTerminated(int num);
    }
    // ------------------------------------------------------------------------------------


    public ExactModel getModel() {
        return model;
    }
}
