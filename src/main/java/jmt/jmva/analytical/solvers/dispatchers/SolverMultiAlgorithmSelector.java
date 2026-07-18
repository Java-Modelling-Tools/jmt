package jmt.jmva.analytical.solvers.dispatchers;

import jmt.common.exception.SolverException;
import jmt.framework.data.ArrayUtils;
import jmt.jmva.analytical.ExactModel;
import jmt.jmva.analytical.solvers.*;
import jmt.jmva.analytical.solvers.exceptions.InternalErrorException;
import jmt.jmva.analytical.solvers.multiSolverAlgorithms.*;
import jmt.jmva.analytical.solvers.utilities.ConstantConvertors;
import jmt.jmva.analytical.solvers.priority.PrioMVASolverFactory;

public class SolverMultiAlgorithmSelector {

    /**
     * All local variables are input values extracted from the model
     */
    private final int classes;
    private final int stations;

    private final String[] stationNames;
    private final int[] classTypes;
    private final int[] stationTypes;
    private final double[] classData;
    private final int[] classPriorities;
    private final double[][][] serviceTimes;
    private final double[][] visits;

    private final int[] stationServers;

    private final int nThreads;
    private final double tolerance;
    private final int maxSamples;

    private final ExactModel model;

    public SolverMultiAlgorithmSelector(ExactModel model) {
        this.classes = model.getClasses();
        this.stations = model.getStations();

        this.stationNames = model.getStationNames();
        this.classTypes = ConstantConvertors.mapClassTypes(model.getClassTypes());
        this.stationTypes = ConstantConvertors.mapStationTypes(model.getStationTypes(), true);
        this.classData = model.getClassData();
        this.classPriorities = model.getClassPriorities();
        this.serviceTimes = model.getServiceTimes();
        this.visits = model.getVisits();

        this.stationServers = model.getStationServers();

        this.nThreads = 1;
        this.tolerance = model.getTolerance();
        this.maxSamples = model.getMaxSamples();

        this.model = model;
    }

    public SolverMulti selectClosedSolver(SolverAlgorithm algorithmType) throws SolverException, InternalErrorException {
        int[] classPop = ArrayUtils.toInt(classData);

        if (SolverAlgorithm.EXACT.equals(algorithmType)) {
            SolverMultiClosedMVA closedsolver = new SolverMultiClosedMVA(classes, stations);
            if (!closedsolver.input(stationNames, stationTypes, serviceTimes, visits, classPop)) {
                DispatcherUtil.fail("Error initializing MVAMultiSolver", null);
            }
            return closedsolver;
        }
        if (SolverAlgorithm.RECAL.equals(algorithmType)) {
            SolverMultiClosedRECAL closedSolver = new SolverMultiClosedRECAL(classes, stations, classPop);
            if (!closedSolver.input(stationTypes, serviceTimes, visits, nThreads)) {
                DispatcherUtil.fail("Error initializing RECALMultiSolver", null);
            }
            return closedSolver;
        }
        if (SolverAlgorithm.COMOM.equals(algorithmType)) {
            SolverMultiClosedCoMoM closedSolver = new SolverMultiClosedCoMoM(classes, stations, classPop);
            if (!closedSolver.input(stationTypes, serviceTimes, visits, nThreads)) {
                DispatcherUtil.fail("Error initializing CoMoMMultiSolver", null);
            }
            return closedSolver;
        }
        if (SolverAlgorithm.TREE_MVA.equals(algorithmType)) {
            SolverMultiClosedTreeMVA closedSolver = new SolverMultiClosedTreeMVA(classes, stations, classPop);
            if (!closedSolver.input(stationTypes, serviceTimes, visits)) {
                DispatcherUtil.fail("Error initializing TreeMVAMultiSolver", null);
            }
            return closedSolver;
        }
        if (SolverAlgorithm.MONTE_CARLO_LOGISTIC.equals(algorithmType)) {
            SolverMultiClosedMonteCarloLogistic closedSolver = new SolverMultiClosedMonteCarloLogistic(classes, stations, classPop,
                    maxSamples, SolverMultiClosedMonteCarloLogistic.DEFAULT_PRECISION);
            if (!closedSolver.input(stationTypes, serviceTimes, visits)) {
                DispatcherUtil.fail("Error initializing MonteCarloLogisticMultiSolver", null);
            }
            return closedSolver;
        }
        // These are all of the buggy algorithms
        /* if (SolverAlgorithm.MOM.equals(algorithmType)) {
						SolverMultiClosedMoM closedSolver = new SolverMultiClosedMoM(classes, stations, classPop);
						if (!closedSolver.input(stationTypes, serviceTimes, visits, nThreads)) {
								DispatcherUtil.fail("Error initializing MoMMultiSolver", null);
						}
						solver = closedSolver;
					}*/
        /* if (SolverAlgorithm.RGF.equals(algorithmType)) {
						SolverMultiClosedRGF closedSolver = new SolverMultiClosedRGF(classes, stations, classPop);
						if (!closedSolver.input(stationTypes, serviceTimes, visits, nThreads)) {
							DispatcherUtil.fail("Error initializing RGFMultiSolver", null);
						}
						solver = closedSolver;
					}*/
        /* if (SolverAlgorithm.TREE_CONV.equals(algorithmType)) {
						SolverMultiClosedTreeConvolution closedSolver = new SolverMultiClosedTreeConvolution(classes, stations, classPop);
						if (!closedSolver.input(stationTypes, serviceTimes, visits)) {
							DispatcherUtil.fail("Error initializing TreeConvolutionMultiSolver", null);
						}
						solver = closedSolver;
					} else if (SolverAlgorithm.MONTE_CARLO.equals(algorithmType)) {
						SolverMultiClosedMonteCarlo closedSolver = new SolverMultiClosedMonteCarlo(classes, stations, classPop);
						if (!closedSolver.input(stationTypes, serviceTimes, visits)) {
							DispatcherUtil.fail("Error initializing MonteCarloMultiSolver", null);
						}
						closedSolver.setTolerance(tolerance);
						closedSolver.setMaxSamples(maxSamples);
						solver = closedSolver;
					}*/

        // Mixed Solvers can solve closed models so need to try them
        SolverMulti mixedSolver = selectMixedSolver(algorithmType);
        if (mixedSolver != null) {
            return mixedSolver;
        }
        return selectClosedAMVASolver(algorithmType);
    }

    public SolverMulti selectClosedAMVASolver(SolverAlgorithm algorithmType, int[] classPop) throws SolverException {
        SolverMultiClosedAMVA closedSolver;
        if (SolverAlgorithm.CHOW.equals(algorithmType)) {
            closedSolver = new SolverMultiClosedChow(classes, stations, classPop);
        } else if (SolverAlgorithm.BARD_SCHWEITZER.equals(algorithmType)) {
            closedSolver = new SolverMultiClosedBardSchweitzer(classes, stations, classPop);
        } else if (SolverAlgorithm.AQL.equals(algorithmType)) {
            closedSolver = new SolverMultiClosedAQL(classes, stations, classPop);
        } else if (SolverAlgorithm.DESOUZA_MUNTZ_LINEARIZER.equals(algorithmType)) {
            closedSolver = new SolverMultiClosedLinearizer(classes, stations, classPop,
                    true);
        } else if (SolverAlgorithm.LINEARIZER.equals(algorithmType)) {
            closedSolver = new SolverMultiClosedLinearizer(classes, stations, classPop,
                    false);
        } else {
           return null;
        }
        closedSolver.setTolerance(tolerance);

        if (!closedSolver.input(stationNames, stationTypes, serviceTimes, visits)) {
            String algName = algorithmType.toString().replace(" ", "").replace("-", "");
            DispatcherUtil.fail("Error initializing " + algName + "MultiSolver", null);
        }
        return closedSolver;
    }

    public SolverMulti selectOpenSolver(SolverAlgorithm algorithmType) throws SolverException {
        if (SolverAlgorithm.EXACT.equals(algorithmType)) {
            SolverMulti solver = new SolverMultiOpen(classes, stations, classData, stationServers);
            if (!solver.input(stationNames, stationTypes, serviceTimes, visits)) {
                DispatcherUtil.fail("Error initializing SolverMultiOpen", null);
            }
            return solver;
        }

        return selectMixedSolver(algorithmType);
    }

    public SolverMulti selectMixedSolver(SolverAlgorithm algorithmType) throws SolverException {
        if (SolverAlgorithm.EXACT.equals(algorithmType)) {
            SolverMultiMixed mixedSolver = new SolverMultiMixed(classes, stations);
            if (!mixedSolver.input(stationNames, stationTypes, serviceTimes, visits, classData, classTypes)) {
                DispatcherUtil.fail("Error initializing SolverMultiMixed", null);
            }
            return mixedSolver;
        }

        if (SolverAlgorithm.SCA.equals(algorithmType)) {
            SolverMultiMixedRWA mixedROA = new SolverMultiMixedRWA(classes, stations);
            if (!mixedROA.input(stationNames, stationTypes, serviceTimes, visits, classData, classTypes, classPriorities)) {
                DispatcherUtil.fail("Error initializing SCA", null);
            }
            mixedROA.setTolerance(tolerance);
            return mixedROA;
        }

        if (SolverAlgorithm.PRIO_MVA_CL.equals(algorithmType)) {
            return PrioMVASolverFactory.getCLPrioMVA(model);
        }
        if (SolverAlgorithm.PRIO_MVA_BKT.equals(algorithmType)) {
            return PrioMVASolverFactory.getBKTPrioMVA(model);
        }
        //if (SolverAlgorithm.PRIO_MVA_T.equals(algorithmType)) {
        //    return PrioMVASolverFactory.getTPrioMVA(model);
        //}
		/*
        if (SolverAlgorithm.PRIO_MVA_CL_Z.equals(algorithmType)) {
            return PrioMVASolverFactory.getCL_ZPrioMVA(model);
        }
        if (SolverAlgorithm.PRIO_MVA_CL_Y.equals(algorithmType)) {
            return PrioMVASolverFactory.getCL_YPrioMVA(model);
        }
        if (SolverAlgorithm.PRIO_MVA_NLI_LI.equals(algorithmType)) {
            return PrioMVASolverFactory.getNLI_LIPrioMVA(model);
        }
        if (SolverAlgorithm.PRIO_MVA_NLI_Z.equals(algorithmType)) {
            return PrioMVASolverFactory.getNLI_ZPrioMVA(model);
        }
        if (SolverAlgorithm.PRIO_MVA_NLI_Y.equals(algorithmType)) {
            return PrioMVASolverFactory.getNLI_YPrioMVA(model);
        }
        if (SolverAlgorithm.PRIO_MVA_RNLI_NC.equals(algorithmType)) {
            return PrioMVASolverFactory.getRNLI_NCPrioMVA(model);
        }
        if (SolverAlgorithm.PRIO_MVA_RNLI_LI.equals(algorithmType)) {
            return PrioMVASolverFactory.getRNLI_LIPrioMVA(model);
        }
        if (SolverAlgorithm.PRIO_MVA_RNLI_Z.equals(algorithmType)) {
            return PrioMVASolverFactory.getRNLI_ZPrioMVA(model);
        }
        if (SolverAlgorithm.PRIO_MVA_RNLI_Y.equals(algorithmType)) {
            return PrioMVASolverFactory.getRNLI_YPrioMVA(model);
        }
		*/
        return null;
    }

    public SolverMulti selectClosedAMVASolver(SolverAlgorithm algorithmType) throws SolverException {
        return selectClosedAMVASolver(algorithmType, ArrayUtils.toInt(classData));
    }

}
