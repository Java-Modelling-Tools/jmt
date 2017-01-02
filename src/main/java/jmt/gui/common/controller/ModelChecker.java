/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */
package jmt.gui.common.controller;

import java.util.*;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.BlockingRegionDefinition;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.SimulationDefinition;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.common.definitions.parametric.ParametricAnalysisChecker;
import jmt.gui.common.definitions.parametric.ParametricAnalysisDefinition;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.joinStrategies.GuardJoin;
import jmt.gui.common.routingStrategies.LoadDependentRouting;
import jmt.gui.common.routingStrategies.ProbabilityRouting;
import jmt.gui.common.routingStrategies.RandomRouting;
import jmt.gui.common.routingStrategies.RoutingStrategy;
import jmt.gui.common.serviceStrategies.LDStrategy;

/**
 * <p>
 * Title: ModelChecker</p>
 * <p>
 * Description: Checks the model correctness</p>
 *
 * @author Francesco D'Aquino Date: 6-ott-2005 Time: 16.12.50
 *
 * @author Bertoli Marco (new errors)
 *
 * Modified by Ashanka (May 2010): Patch: Multi-Sink Perf. Index Description:
 * Added new Performance index for the capturing the 1. global response time
 * (ResponseTime per Sink) 2. global throughput (Throughput per Sink) each sink
 * per class. Hence new validations are required to check the Performance
 * Indices of response per sink and throughput per sink follow the model
 * validations. 1. Response Time per Sink and Throughput per Sink should have a
 * sink in the model. 2. Response Time per Sink and Throughput per Sink should
 * not be selected with a closed class as for a closed model till now in JMT no
 * jobs are routed to the sink and apparently sink is allowed to choose only
 * when a open class is present.
 *
 * Modified by Ashanka (June 2010): Added Sink Probability for Closed class
 * update to 0.0 warning message.
 */
public class ModelChecker implements CommonConstants {

	private ClassDefinition class_def;
	private StationDefinition station_def;
	private SimulationDefinition simulation_def;
	private BlockingRegionDefinition blocking_def;

	//sets the behavior of the checker
	private boolean isToJMVAChecker;

	//Variable that contains information about problems
	private boolean[] errors;
	//Variable that contains information about warnings
	private boolean[] warnings;

	//Vector containing the keys of the classes without a reference station
	private Vector<Object> classesWithoutRefStation;
	//Vector containing only the keys of the open classes without a reference station
	private Vector<Object> openClassesWithoutRefStation;
	//Vector containing the keys of the sources without associated open classes
	private Vector<Object> sourceWithoutOpenClass;
	//Vector containing the keys of stations with link problems
	private Vector<Object> stationsWithLinkErrors;
	//HashMap containing the vector of keys of the stations where
	//routing problems for this close class occur. The key of the
	//HashMap is the key of the closed class considered
	private HashMap<Object, Vector<Object>> routingErrors;
	//HashMap containing the vector of keys of the stations reachable
	//by a close class. The key of the HashMap is the key of the
	//closed class considered
	private HashMap<Object, Vector<Object>> allForwardStationsAreSinkErrors;
	//Vector containing the keys of measures defined more than once
	private Vector<Object> redundantMeasure;
	//Vector containing the inconsistent performance indices
	private Vector<Object> inconsistentMeasures;
	//Vector containing the keys of servers with per class different
	//queue strategies
	private Vector<Object> BCMPserversWithDifferentQueueStrategy;
	//Vector containing the keys of servers with FCFS queueing strategy
	//but mixed types of service
	private Vector<Object> BCMPserversWithDifferentServiceTypes;
	//Vector containing the keys of servers with FCFS queue strategy
	//but a non exponential distribution
	private Vector<Object> BCMPserversFCFSWithoutExponential;
	//Vector containing the keys of servers with FCFS, the same
	//type of service for each class, but with different mean service
	//times
	private Vector<Object> BCMPFcfsServersWithDifferentServiceTimes;
	//Vector containing the keys of delays with at least a service time
	//distribution with a non rational Laplace transform
	private Vector<Object> BCMPdelaysWithNonRationalServiceDistribution;
	//Vector containing the keys of the stations where a non BCMP scheduling policy is used.
	//Used only in JMVA conversion
	private Vector<Object> BCMPnonStateIndependentRoutingStations;
	//Vector containing the keys of the stations where a non random routing is used.
	//Used only in JMVA conversion
	private Vector<Object> BCMPserverWithNonSymmetricSchedulingPolicy;
	//Vector containing the keys of the stations (no sources or sinks) not backward linked.
	private Vector<Object> stationWithoutBackwardLinks;
	//Vector containing the keys of empty blocking regions
	private Vector<Object> emptyBlockingRegions;
	//Vector containing the keys of cs with wrong matrix(sum by rows less than 1)
	private Vector<Object> csWithWrongMatrix;
	//Vector containing the keys of cs followed by queue with drop bas strategy
	private Vector<Object> csFollowedByBas;
	//Vector containing the keys of cs between fork/join
	private Vector<Object> csBetweenForkJoin;
	//Vector containing the classes of the system throughput
	private Vector<Object> csSystemThroughputClass;
	//Vector containing the closed classes of zero population
	private Vector<Object> closedZeroPopulation;

	private Map<Object, HashMap<Object, Object>> invalidLoadDependentRouting;
	//HashMap containing the vector of keys of the classes for
	//which Guard strategies has zero tasks at this join. The
	//key of the HashMap is the key of the join considered
	private HashMap<Object, Vector<Object>> guardStrategyZeroTasks;
	//Vector containing the keys of semaphores not between fork/join
	private Vector<Object> semaphoreNotBetweenForkJoin;
	//Vector containing the keys of scalers not between fork/join
	private Vector<Object> scalerNotBetweenForkJoin;
	//Vector containing the keys of transitions between fork/join
	private Vector<Object> transitionBetweenForkJoin;
	//Vector containing the keys of transitions without forward links
	private Vector<Object> transitionWithoutForwardLinks;
	//Vector containing the keys of transitions with invalid conditions for some modes
	private Vector<Object> transitionWithInvalidConditions;
	//Vector containing the keys of transitions with invalid outcomes for some modes
	private Vector<Object> transitionWithInvalidOutcomes;

	//constant used to define an error
	public static final int ERROR_PROBLEM = 0;
	//constant used to define a warning
	public static final int WARNING_PROBLEM = 1;

	//it occurs when no classes have been defined
	public static final int NO_CLASS_ERROR = 0;
	//it occurs when no stations have been defined
	public static final int NO_STATION_ERROR = 1;
	//it occurs when no measures have been defined
	public static final int NO_MEASURE_ERROR = 2;
	//it occurs when there is a source without associated open classes
	public static final int SOURCE_WITHOUT_OPEN_CLASS_ERROR = 3;
	//it occurs when there is a sink but no open classes have been defined
	public static final int SINK_BUT_NO_OPEN_CLASS_ERROR = 4;
	//it occurs when there is an open class but no sources have been defined
	public static final int OPEN_CLASS_BUT_NO_SOURCE_ERROR = 5;
	//it occurs when there is an open class but no sinks have been defined
	public static final int OPEN_CLASS_BUT_NO_SINK_ERROR = 6;
	//it occurs when a class has not a reference station
	public static final int REFERENCE_STATION_ERROR = 7;
	//it occurs when a station is not properly linked
	public static final int STATION_LINK_ERROR = 8;
	//it occurs when a close class is routed with probability 1 into a sink
	public static final int ROUTING_ERROR = 9;
	//it occurs when a close class is routed into a station whose forward stations are all sinks
	public static final int ALL_FORWARD_STATION_ARE_SINK_ERROR = 10;
	//it occurs if the same measure is defined more than once
	public static final int DUPLICATE_MEASURE_ERROR = 11;
	//it occurs if one or performance index has one or more null field
	public static final int INCONSISTENT_MEASURE_ERROR = 12;
	//it occurs when there is at least one join but not a fork
	public static final int JOIN_WITHOUT_FORK_ERROR = 13;
	//All jobs split by a fork must be routed to the same sink, or to the same join
	public static final int FORK_JOIN_ROUTING_ERROR = 14;
	//Checks if there are empty blocking regions.
	public static final int EMPTY_BLOCKING_REGION = 15;
	//Checks if preloading in blocking regions exceeds the constraints.
	public static final int PRELOADING_IN_BLOCKING_REGION = 16;
	//Checks if XperSink and RperSink is selected without sinks in the model.
	public static final int SINK_PERF_IND_WITH_NO_SINK_ERROR = 17;
	//Checks if XperSink and RperSink is selected for closed classes.
	public static final int SINK_PERF_WITH_CLOSED_CLASS_ERROR = 18;
	//Checks if exist some rows of a cs matrix that sum to a number less than one.
	public static final int CS_MATRIX_HAS_ROWS_LESS_THAN_ONE = 19;
	//Checks if exist some cs followed by a queue with bas drop strategy.
	public static final int CS_FOLLOWED_BY_A_BAS = 20;

	public static final int LOAD_DEPENDENT_ROUTING_INVALID = 21;
	//Checks the different reference station of closed class if there exists class switch.
	public static final int CS_REFERENCE_STATION = 22;
	//Checks existence of the system throughput per class with class switch.
	public static final int CS_SYSTEM_THROUGHPUT_CLASS = 23;
	//Checks if all the closed classes population is zero with class switch.
	public static final int CS_ALL_CLOSED_ZERO_POPULATION = 24;
	//Checks if there are any joins that have Guard strategies with zero tasks.
	public static final int GUARD_STRATEGY_ZERO_TASKS = 25;
	//Checks if there are any semaphores not between a fork and a join.
	public static final int SEMAPHORE_NOT_BETWEEN_FORK_JOIN = 26;
	//Checks if there are any scalers not between a fork and a join.
	public static final int SCALER_NOT_BETWEEN_FORK_JOIN = 27;
	//Checks if any transitions have invalid conditions for some modes.
	public static final int TRANSITION_WITH_INVALID_CONDITIONS = 28;

	//it occurs when more than one sink have been defined
	public static final int MORE_THAN_ONE_SINK_WARNING = 0;
	//it occurs when stations (no sources or sinks) are not backward linked
	public static final int NO_BACKWARD_LINK_WARNING = 1;
	//it occurs if there is a fork but not a join
	public static final int FORK_WITHOUT_JOIN_WARNING = 2;
	//Thrown if the parametric analysis model has become inconsistent
	//with the model definition. This warning advises user to recheck the
	//parametric model definition, since it had been modified to make it
	//consistent back again.
	public static final int PARAMETRIC_ANALYSIS_MODEL_MODIFIED_WARNING = 3;
	//Thrown if a no PA are available, but a PA model was previously defined
	public static final int PARAMETRIC_ANALYSIS_NO_MORE_AVAILABLE_WARNING = 4;
	//Thrown if the station has different per class queueing
	//strategy. (only when trying to convert to JMVA)
	public static final int BCMP_DIFFERENT_QUEUEING_STRATEGIES_WARNING = 5;
	//Thrown if the service policy inside a station with FCFS
	//queueing strategy, differs from class to class. In fact,
	//according to the BCMP theorem hypothesis, the service times
	//in an FCFS station must be all load independent or independent,
	//mixtures are not allowed
	public static final int BCMP_FCFS_DIFFERENT_SERVICE_TYPES_WARNING = 6;
	//Thrown if the station has a FCFS queueing strategy, but
	//the service time distribution is not exponential
	public static final int BCMP_FCFS_EXPONENTIAL_WARNING = 7;
	//Thrown if the station has a FCFS queueing strategy, but
	//the per class mean values of service time differ
	// (only when trying to convert to JMVA)
	public static final int BCMP_FCFS_DIFFERENT_SERVICE_TIMES_WARNING = 8;
	//Thrown if the station has a Processor Sharing service strategy
	//but it is not possible to define a Laplace rational transform
	//for the service time distribution
	// (only when trying to convert to JMVA)
	public static final int BCMP_PROCESSOR_SHARING_WARNING = 9;
	//Thrown if the station has a delay behavior but it is not
	//possible to define a Laplace rational transform for a
	//service time distribution
	// (only when trying to convert to JMVA)
	public static final int BCMP_DELAY_WARNING = 10;
	//Thrown if the station has a LCFS - Preemptive Resume
	//queueing strategy but it is not possible to define a
	//Laplace rational transform for the service time distribution
	// (only when trying to convert to JMVA)
	public static final int BCMP_LCFS_PREEMPTIVE_WARNING = 11;
	//Thrown if the routing strategy is state dependent
	// (only when trying to convert to JMVA)
	public static final int BCMP_NON_STATE_INDEPENDENT_ROUTING_WARNING = 12;
	//Thrown if the scheduling policies other than PS, FCFS, or RAND
	//are used
	// (only when trying to convert to JMVA)
	public static final int BCMP_NON_SYMMETRIC_SCHEDULING = 13;

	public static final int SINK_PROBABILITY_UPDATE_WARNING = 14;
	//Checks if the closed classes population is zero.
	public static final int CLOSED_ZERO_POPULATION = 15;
	//Checks if exist some cs between a fork and a join.
	public static final int CS_BETWEEN_FORK_JOIN = 16;
	//Checks if exist some cs and fork/join section.
	public static final int CS_WITH_FORK_JOIN = 17;
	//Checks if priority scheduling is used but all the classes have the same priorities.
	public static final int SCHEDULING_SAME_PRIORITIES = 18;
	//Checks if there are any transitions between a fork and a join.
	public static final int TRANSITION_BETWEEN_FORK_JOIN = 19;
	//Checks if there are any transitions without forward links.
	public static final int TRANSITION_WITHOUT_FORWARD_LINKS = 20;
	//Checks if any transitions have invalid outcomes for some modes.
	public static final int TRANSITION_WITH_INVALID_OUTCOMES = 21;

	private static final int NUMBER_OF_ERROR_TYPES = 29;
	private static final int NUMBER_OF_NORMAL_ERROR_TYPES = 29;
	private static final int NUMBER_OF_WARNING_TYPES = 22;
	private static final int NUMBER_OF_NORMAL_WARNING_TYPES = 5;

	/**
	 * Creates a new modelchecker
	 *
	 * @param class_def reference to class definition data structure
	 * @param station_def reference to station definition data structure
	 * @param simulation_def reference to simulation definition data structure
	 * @param bd reference to blocking region definition data structure
	 * @param isToJMVA true if model must be converted to jmva, false otherwise
	 */
	public ModelChecker(ClassDefinition class_def, StationDefinition station_def, SimulationDefinition simulation_def, BlockingRegionDefinition bd,
			boolean isToJMVA) {
		this.class_def = class_def;
		this.station_def = station_def;
		this.simulation_def = simulation_def;
		this.blocking_def = bd;
		errors = new boolean[NUMBER_OF_ERROR_TYPES];
		warnings = new boolean[NUMBER_OF_WARNING_TYPES];
		classesWithoutRefStation = new Vector<Object>(0, 1);
		openClassesWithoutRefStation = new Vector<Object>(0, 1);
		sourceWithoutOpenClass = new Vector<Object>(0, 1);
		stationsWithLinkErrors = new Vector<Object>(0, 1);
		routingErrors = new HashMap<Object, Vector<Object>>(0, 1);
		allForwardStationsAreSinkErrors = new HashMap<Object, Vector<Object>>(0, 1);
		stationWithoutBackwardLinks = new Vector<Object>(0, 1);
		redundantMeasure = new Vector<Object>(0, 1);
		inconsistentMeasures = new Vector<Object>(0, 1);
		BCMPserversWithDifferentQueueStrategy = new Vector<Object>(0, 1);
		BCMPserversWithDifferentServiceTypes = new Vector<Object>(0, 1);
		BCMPserversFCFSWithoutExponential = new Vector<Object>(0, 1);
		BCMPFcfsServersWithDifferentServiceTimes = new Vector<Object>(0, 1);
		BCMPdelaysWithNonRationalServiceDistribution = new Vector<Object>(0, 1);
		BCMPnonStateIndependentRoutingStations = new Vector<Object>(0, 1);
		BCMPserverWithNonSymmetricSchedulingPolicy = new Vector<Object>(0, 1);
		csWithWrongMatrix = new Vector<Object>(0, 1);
		csFollowedByBas = new Vector<Object>(0, 1);
		csBetweenForkJoin = new Vector<Object>(0, 1);
		csSystemThroughputClass = new Vector<Object>(0, 1);
		closedZeroPopulation = new Vector<Object>(0, 1);
		emptyBlockingRegions = new Vector<Object>(0, 1);
		invalidLoadDependentRouting = new HashMap<Object, HashMap<Object, Object>>(0);
		guardStrategyZeroTasks = new HashMap<Object, Vector<Object>>(0, 1);
		semaphoreNotBetweenForkJoin = new Vector<Object>(0, 1);
		scalerNotBetweenForkJoin = new Vector<Object>(0, 1);
		transitionBetweenForkJoin = new Vector<Object>(0, 1);
		transitionWithoutForwardLinks = new Vector<Object>(0, 1);
		transitionWithInvalidConditions = new Vector<Object>(0, 1);
		transitionWithInvalidOutcomes = new Vector<Object>(0, 1);
		isToJMVAChecker = isToJMVA;
		checkModel();
	}

	//resets all the variables of the model checker
	private void reset() {
		openClassesWithoutRefStation.removeAllElements();
		classesWithoutRefStation.removeAllElements();
		sourceWithoutOpenClass.removeAllElements();
		stationsWithLinkErrors.removeAllElements();
		routingErrors.clear();
		allForwardStationsAreSinkErrors.clear();
		stationWithoutBackwardLinks.removeAllElements();
		redundantMeasure.removeAllElements();
		inconsistentMeasures.clear();
		BCMPserversWithDifferentQueueStrategy.removeAllElements();
		BCMPserversWithDifferentServiceTypes.removeAllElements();
		BCMPserversFCFSWithoutExponential.removeAllElements();
		BCMPFcfsServersWithDifferentServiceTimes.removeAllElements();
		BCMPdelaysWithNonRationalServiceDistribution.removeAllElements();
		BCMPnonStateIndependentRoutingStations.removeAllElements();
		BCMPserverWithNonSymmetricSchedulingPolicy.removeAllElements();
		csWithWrongMatrix.removeAllElements();
		csFollowedByBas.removeAllElements();
		csBetweenForkJoin.removeAllElements();
		csSystemThroughputClass.removeAllElements();
		closedZeroPopulation.removeAllElements();
		emptyBlockingRegions.removeAllElements();
		invalidLoadDependentRouting.clear();
		guardStrategyZeroTasks.clear();
		semaphoreNotBetweenForkJoin.removeAllElements();
		scalerNotBetweenForkJoin.removeAllElements();
		transitionBetweenForkJoin.removeAllElements();
		transitionWithoutForwardLinks.removeAllElements();
		transitionWithInvalidConditions.removeAllElements();
		transitionWithInvalidOutcomes.removeAllElements();
		for (int i = 0; i < NUMBER_OF_ERROR_TYPES; i++) {
			errors[i] = false;
		}
		for (int i = 0; i < NUMBER_OF_WARNING_TYPES; i++) {
			warnings[i] = false;
		}
	}

	//checks for problems of the model
	public void checkModel() {
		reset();

		if (isToJMVAChecker) {
			checkForNoClassError();
			checkForNoStationError();
			checkForRefStationError();
			checkForOpenClassButNoSourceError();
			checkForBCMPDifferentQueueingStrategyWarning();
			checkForBCMPFcfsDifferentServiceTypesWarning();
			checkForBCMPFcfsNonExponentialWarning();
			checkForBCMPFcfsDifferentServiceTimesWarning();
			checkForBCMPProcessorSharingWarning();
			checkForBCMPDelayWarning();
			checkForBCMPLcfsPreemptiveWarning();
			checkForBCMPNonStateIndependentRoutingWarning();
			checkForBCMPNonSymmetricSchedulingPolicies();
		} else {
			station_def.manageProbabilities();
			checkForNoClassError();
			checkForNoStationError();
			checkForNoMeasureError();
			checkForRefStationError();
			checkForSourceWithoutOpenClassError();
			checkForSinkButNoOpenClassError();
			checkForOpenClassButNoSourceError();
			checkForOpenClassButNoSinkError();
			checkForStationLinkError();
			checkForRoutingError();
			checkForAllForwardStationsAreSinkError();
			checkForMeasureError();
			checkForInconsistentMeasureError();
			checkForLoadDependentRoutingError();
			//checkForMoreThanOneSinkWarning();
			checkForNoBackwardLinkWarning();
			checkForParametricAnalysisModelModifiedWarning();
			checkForParametricAnalysisNoMoreAvailableWarning();
			checkForJoinWithoutForkErrors();
			checkForForkWithoutJoinWarnings();
			checkForEmptyBlockingRegions();
			checkForPreloadingInBlockingRegions();
			checkForSinkPerfIndicesWithNoSink();
			checkForSinkPerfIndicesWithClosedClass();
			checkForSinkProbabilityUpdateWarning();
			checkForCsMatrixProbabilities();
			checkForCsFollowedByBAS();
			checkForCsBetweenForkJoin();
			checkForCsReferenceStation();
			checkForCsSystemThroughputClass();
			checkForZeroPopulation();
			checkForCsWithForkJoin();
			checkForGuardStrategyZeroTasks();
			checkForSchedulingSamePriorities();
			checkForSemaphoreNotBetweenForkJoin();
			checkForScalerNotBetweenForkJoin();
			checkForTransitionBetweenForkJoin();
			checkForTransitionWithoutForwardLink();
			checkForTransitionWithInvalidConditions();
			checkForTransitionWithInvalidOutcomes();
		}
	}

	public StationDefinition getStationModel() {
		return station_def;
	}

	public ClassDefinition getClassModel() {
		return class_def;
	}

	public BlockingRegionDefinition getBlockingModel() {
		return blocking_def;
	}

	public boolean isToJMVA() {
		return isToJMVAChecker;
	}

	public boolean isEverythingOkNormal() {
		boolean ok = false;
		if (isErrorFreeNormal() && isWarningFreeNormal()) {
			ok = true;
		}
		return ok;
	}

	public boolean isEverythingOkToJMVA() {
		boolean ok = false;
		if (isErrorFreeToJMVA() && isWarningFreeToJMVA()) {
			ok = true;
		}
		return ok;
	}

	public boolean isErrorFreeNormal() {
		boolean ok = true;
		for (int i = 0; i < NUMBER_OF_NORMAL_ERROR_TYPES; i++) {
			if (errors[i]) {
				ok = false;
			}
		}
		return ok;
	}

	public boolean isWarningFreeNormal() {
		boolean ok = true;
		for (int i = 0; i < NUMBER_OF_NORMAL_WARNING_TYPES; i++) {
			if (warnings[i]) {
				ok = false;
			}
		}
		//Added warnings here separately otherwise we had to redo the numbering
		if (warnings[SINK_PROBABILITY_UPDATE_WARNING] || warnings[CLOSED_ZERO_POPULATION] || warnings[CS_BETWEEN_FORK_JOIN]
				|| warnings[CS_WITH_FORK_JOIN] || warnings[SCHEDULING_SAME_PRIORITIES] || warnings[TRANSITION_BETWEEN_FORK_JOIN]
				|| warnings[TRANSITION_WITHOUT_FORWARD_LINKS] || warnings[TRANSITION_WITH_INVALID_OUTCOMES]) {
			ok = false;
		}
		return ok;
	}

	public boolean isErrorFreeToJMVA() {
		boolean ok = true;
		if (errors[NO_CLASS_ERROR] || errors[NO_STATION_ERROR] || errors[OPEN_CLASS_BUT_NO_SOURCE_ERROR] || errors[REFERENCE_STATION_ERROR]) {
			ok = false;
		}
		return ok;
	}

	public boolean isWarningFreeToJMVA() {
		boolean ok = true;
		if (warnings[BCMP_DIFFERENT_QUEUEING_STRATEGIES_WARNING] || warnings[BCMP_FCFS_DIFFERENT_SERVICE_TYPES_WARNING]
				|| warnings[BCMP_FCFS_EXPONENTIAL_WARNING] || warnings[BCMP_FCFS_DIFFERENT_SERVICE_TIMES_WARNING]
				|| warnings[BCMP_PROCESSOR_SHARING_WARNING] || warnings[BCMP_DELAY_WARNING] || warnings[BCMP_LCFS_PREEMPTIVE_WARNING]
				|| warnings[BCMP_NON_STATE_INDEPENDENT_ROUTING_WARNING] || warnings[BCMP_NON_SYMMETRIC_SCHEDULING]) {
			ok = false;
		}
		return ok;
	}

	/**
	 * Returns true if no classes have been defined
	 *
	 * @return false, if there is at least one class
	 */
	public boolean isThereNoClassError() {
		return errors[NO_CLASS_ERROR];
	}

	/**
	 * Returns true if no stations have been defined
	 *
	 * @return false, if there is at least one station
	 */
	public boolean isThereNoStationError() {
		return errors[NO_STATION_ERROR];
	}

	/**
	 * Returns true if no measures have been defined
	 *
	 * @return true if the problem occurs
	 */
	public boolean isThereNoMeasureError() {
		return errors[NO_MEASURE_ERROR];
	}

	/**
	 * Returns true if a class has no reference station
	 *
	 * @return false, if each class has a reference station
	 */
	public boolean isThereClassesWithoutRefStationError() {
		return errors[REFERENCE_STATION_ERROR];
	}

	/**
	 * Returns true if there is at least a source without associated open
	 * class
	 *
	 * @return true if the problem occurs
	 */
	public boolean isThereSourceWithoutOpenClassError() {
		return errors[SOURCE_WITHOUT_OPEN_CLASS_ERROR];
	}

	/**
	 * Returns true if there is at least a sink but no open classes have been
	 * defined
	 *
	 * @return true if the problem occurs
	 */
	public boolean isThereSinkButNoOpenClassError() {
		return errors[SINK_BUT_NO_OPEN_CLASS_ERROR];
	}

	/**
	 * Returns true if there is at least one open class but no sources have been
	 * defined
	 *
	 * @return true if the problem occurs
	 */
	public boolean isThereOpenClassButNoSourceError() {
		return errors[OPEN_CLASS_BUT_NO_SOURCE_ERROR];
	}

	/**
	 * Returns true if there is at least one open class but no sinks have been
	 * defined
	 *
	 * @return true if the problem occurs
	 */
	public boolean isThereOpenClassButNoSinkError() {
		return errors[OPEN_CLASS_BUT_NO_SINK_ERROR];
	}

	/**
	 * Returns true if there is at least one station link error
	 *
	 * @return true if the problem occurs
	 */
	public boolean isThereStationLinkError() {
		return errors[STATION_LINK_ERROR];
	}

	/**
	 * Returns true if there is at least one routing error (a closed class
	 * routed into a sink)
	 *
	 * @return true if at least one routing problem was found
	 */
	public boolean isThereRoutingError() {
		return errors[ROUTING_ERROR];
	}

	/**
	 * Returns true if there is at least one closed class routed into a station
	 * whose forward stations are all sinks
	 *
	 * @return true if at least one routing problem was found
	 */
	public boolean isThereAllForwardStationsAreSinkErrors() {
		return errors[ALL_FORWARD_STATION_ARE_SINK_ERROR];
	}

	/**
	 * Returns true if the same measure is defined more than one time
	 *
	 * @return true if the same measure is defined more than one time
	 */
	public boolean isThereMeasureError() {
		return errors[DUPLICATE_MEASURE_ERROR];
	}

	/**
	 * Returns true if there is at least one measure with at least one 'null'
	 * field
	 *
	 * @return true if the same measure is defined more than one time
	 */
	public boolean isThereInconsistentMeasureError() {
		return errors[INCONSISTENT_MEASURE_ERROR];
	}

	/**
	 * Returns true if there is at least one join but not a fork, false
	 * otherwise.
	 *
	 * @return true if there is at least one join but not a fork, false
	 * otherwise.
	 */
	public boolean isTherejoinWithoutForkError() {
		return errors[JOIN_WITHOUT_FORK_ERROR];
	}

	/**
	 * Returns true if there are empty blocking regions.
	 *
	 * @return true if there are empty blocking regions.
	 */
	public boolean isThereEmptyBlockingRegionError() {
		return errors[EMPTY_BLOCKING_REGION];
	}

	/**
	 * Returns true if preloading in blocking regions exceeds the constraints.
	 *
	 * @return true if preloading in blocking regions exceeds the constraints.
	 */
	public boolean isTherePreloadingInBlockingRegionError() {
		return errors[PRELOADING_IN_BLOCKING_REGION];
	}

	public boolean isThereSinkPerfIndicesWithNoSinkError() {
		return errors[SINK_PERF_IND_WITH_NO_SINK_ERROR];
	}

	public boolean isSinkPerfIndicesWithClosedClassError() {
		return errors[SINK_PERF_WITH_CLOSED_CLASS_ERROR];
	}

	/**
	 * Returns true if exist some class switch components with a invalid matrix
	 * (e.g. some rows do not sum to a value greater or equals to one.), false
	 * otherwise.
	 */
	public boolean isThereCsMatrixRowSumError() {
		return errors[CS_MATRIX_HAS_ROWS_LESS_THAN_ONE];
	}

	/**
	 * Returns true if exist some class switch components followed by a queue
	 * implementing the BAS strategy, false otherwise.
	 */
	public boolean isThereCsFollowedByBasError() {
		return errors[CS_FOLLOWED_BY_A_BAS];
	}

	public boolean isThereLoadDependentRoutingError() {
		return errors[LOAD_DEPENDENT_ROUTING_INVALID];
	}

	/**
	 * Returns true if exist some class switch components between a fork and a
	 * join, false otherwise.
	 *
	 * @return true if exist some class switch components between a fork and a
	 * join, false otherwise.
	 */
	public boolean isThereCsBetweenForkJoin() {
		return warnings[CS_BETWEEN_FORK_JOIN];
	}

	/**
	 * Returns true if exist at least one class switch and different reference
	 * station for the closed classes, false otherwise.
	 *
	 * @return true if exist at least one class switch and different reference
	 * station for the closed classes, false otherwise.
	 */
	public boolean isThereCsReferenceStation() {
		return errors[CS_REFERENCE_STATION];
	}

	/**
	 * Returns true if exists a system throughput per class performance index
	 * when there exists at least one class switch in the model, false otherwise.
	 *
	 * @return true if exists a system throughput per class performance index
	 * when there exists at least one class switch in the model, false otherwise.
	 */
	public boolean isThereCsSystemThroughputClass() {
		return errors[CS_SYSTEM_THROUGHPUT_CLASS];
	}

	/**
	 * Returns true if there exists at least one closed class with zero
	 * population, false otherwise.
	 *
	 * @return true if there exists at least one closed class with zero
	 * population, false otherwise.
	 */
	public boolean isThereClosedClassZeroPopulation() {
		return warnings[CLOSED_ZERO_POPULATION];
	}

	/**
	 * Returns true if all the closed classes has zero population with class
	 * switch, false otherwise.
	 *
	 * @return true if all the closed classes has zero population with class
	 * switch, false otherwise.
	 */
	public boolean isThereCsAllClosedZeroPopulation() {
		return errors[CS_ALL_CLOSED_ZERO_POPULATION];
	}

	/**
	 * Returns true if exist some class switch components and fork/join sections,
	 * false otherwise.
	 *
	 * @return true if exist some class switch components and fork/join sections,
	 * false otherwise.
	 */
	public boolean isThereCsWithForkJoin() {
		return warnings[CS_WITH_FORK_JOIN];
	}

	/**
	 * Returns true if there are any joins that have Guard strategies with zero
	 * tasks, false otherwise.
	 *
	 * @return true if there are any joins that have Guard strategies with zero
	 * tasks, false otherwise.
	 */
	public boolean isThereGuardStrategyZeroTasks() {
		return errors[GUARD_STRATEGY_ZERO_TASKS];
	}

	/**
	 * Returns true if there are any semaphores not between a fork and a join,
	 * false otherwise.
	 *
	 * @return true if there are any semaphores not between a fork and a join,
	 * false otherwise.
	 */
	public boolean isThereSemaphoreNotBetweenForkJoin() {
		return errors[SEMAPHORE_NOT_BETWEEN_FORK_JOIN];
	}

	/**
	 * Returns true if there are any scalers not between a fork and a join,
	 * false otherwise.
	 *
	 * @return true if there are any scalers not between a fork and a join,
	 * false otherwise.
	 */
	public boolean isThereScalerNotBetweenForkJoin() {
		return errors[SCALER_NOT_BETWEEN_FORK_JOIN];
	}

	/**
	 * Returns true if any transitions have invalid conditions for some modes,
	 * false otherwise.
	 *
	 * @return true if any transitions have invalid conditions for some modes,
	 * false otherwise.
	 */
	public boolean isThereTransitionWithInvalidConditions() {
		return errors[TRANSITION_WITH_INVALID_CONDITIONS];
	}

	/**
	 * Returns true if more than one Sink have been defined.
	 *
	 * @return true if more than one Sink have been defined.
	 */
	public boolean isThereMoreThanOneSinkWarning() {
		return warnings[MORE_THAN_ONE_SINK_WARNING];
	}

	/**
	 * Returns true if there are stations (no sources or sinks) not backward
	 * linked.
	 *
	 * @return true if there are stations (no sources or sinks) not backward
	 * linked.
	 */
	public boolean isThereNoBackwardLinkWarning() {
		return warnings[NO_BACKWARD_LINK_WARNING];
	}

	/**
	 * Returns true if there is at least one fork but not a join, false
	 * otherwise.
	 *
	 * @return true if there is at least one fork but not a join, false
	 * otherwise.
	 */
	public boolean isThereForkWithoutJoinWarnings() {
		return warnings[FORK_WITHOUT_JOIN_WARNING];
	}

	/**
	 * Returns true if the parametric analysis model had become inconsistent
	 * consequently it was modified.
	 *
	 * @return true if the parametric analysis model had become inconsistent
	 * consequently it was modified.
	 */
	public boolean isThereParametricAnalysisModelModifiedWarning() {
		return warnings[PARAMETRIC_ANALYSIS_MODEL_MODIFIED_WARNING];
	}

	/**
	 * Returns true if no PA are available but a PA model was previously defined.
	 *
	 * @return true if no PA are available but a PA model was previously defined.
	 */
	public boolean isThereParametricAnalysisNoMoreAvailableWarning() {
		return warnings[PARAMETRIC_ANALYSIS_NO_MORE_AVAILABLE_WARNING];
	}

	/**
	 * Returns true if there are servers with per class different queueing
	 * strategy.
	 *
	 * @return true if there are servers with per class different queueing
	 * strategy.
	 */
	public boolean isThereBCMPDifferentQueueingStrategyWarning() {
		return warnings[BCMP_DIFFERENT_QUEUEING_STRATEGIES_WARNING];
	}

	/**
	 * Returns true if there are FCFS servers with a mixed service type.
	 *
	 * @return true if there are FCFS servers with a mixed service type.
	 */
	public boolean isThereBCMPDifferentServiceTypeWarning() {
		return warnings[BCMP_FCFS_DIFFERENT_SERVICE_TYPES_WARNING];
	}

	/**
	 * Returns true if there are FCFS servers with homogeneous but non exponential
	 * service type.
	 *
	 * @return true if there are FCFS servers with homogeneous but non exponential
	 * service type.
	 */
	public boolean isThereBCMPFcfsNonExponentialWarning() {
		return warnings[BCMP_FCFS_EXPONENTIAL_WARNING];
	}

	/**
	 * Returns true if there are FCFS servers with homogeneous exponential service
	 * type with different per class service times mean values. If the service
	 * type is Load Dependent it checks that for each class the load dependent
	 * strategies are the same, i.e.:
	 * <br>1) have the same number of ranges</br>
	 * <br>2) have the same distribution in each range (Exponential)</br>
	 * <br>3) have the same mean value in each range</br>
	 * <br>4) have the same 'from' and 'to' values in each range</br>
	 *
	 * @return true if there are FCFS servers with homogeneous exponential service
	 * type with different per class service times mean values.
	 */
	public boolean isThereBCMPFcfsDifferentServiceTimesWarning() {
		return warnings[BCMP_FCFS_DIFFERENT_SERVICE_TIMES_WARNING];
	}

	/**
	 * Returns true if there are stations with Processor Sharing service strategy
	 * with a service time distribution whose Laplace transform is not rational.
	 *
	 * @return true if there are stations with Processor Sharing service strategy
	 * with a service time distribution whose Laplace transform is not rational.
	 */
	public boolean isThereBCMPProcessorSharingWarning() {
		return warnings[BCMP_PROCESSOR_SHARING_WARNING];
	}

	/**
	 * Returns true if there are delays with a service time distribution whose
	 * Laplace transform is not rational.
	 *
	 * @return true if there are delays with a service time distribution whose
	 * Laplace transform is not rational.
	 */
	public boolean isThereBCMPDelayWarning() {
		return warnings[BCMP_DELAY_WARNING];
	}

	/**
	 * Returns true if there are LCFS-PR stations with service time distribution
	 * whose Laplace transform is not rational.
	 *
	 * @return true if there are LCFS-PR stations with service time distribution
	 * whose Laplace transform is not rational.
	 */
	public boolean isThereBCMPLcfsPreemptiveWarning() {
		return warnings[BCMP_LCFS_PREEMPTIVE_WARNING];
	}

	/**
	 * Returns true if a non random routing is used.
	 *
	 * @return true if a non random routing is used.
	 */
	public boolean isThereBCMPNonStateIndependentRoutingWarning() {
		return warnings[BCMP_NON_STATE_INDEPENDENT_ROUTING_WARNING];
	}

	/**
	 * Returns true if the scheduling policies other than PS, FCFS, or RAND are
	 * used.
	 *
	 * @return true if the scheduling policies other than PS, FCFS, or RAND are
	 * used.
	 */
	public boolean isThereBCMPNonSymmetricSchedulingPolicies() {
		return warnings[BCMP_NON_SYMMETRIC_SCHEDULING];
	}

	/**
	 * Returns true if priority scheduling is used but all the classes have the same
	 * priorities.
	 *
	 * @return true if priority scheduling is used but all the classes have the same
	 * priorities.
	 */
	public boolean isThereSchedulingSamePriorities() {
		return warnings[SCHEDULING_SAME_PRIORITIES];
	}

	/**
	 * Returns true if there are any transitions between a fork and a join,
	 * false otherwise.
	 *
	 * @return true if there are any transitions between a fork and a join,
	 * false otherwise.
	 */
	public boolean isThereTransitionBetweenForkJoin() {
		return warnings[TRANSITION_BETWEEN_FORK_JOIN];
	}

	/**
	 * Returns true if there are any transitions without forward links,
	 * false otherwise.
	 *
	 * @return true if there are any transitions without forward links,
	 * false otherwise.
	 */
	public boolean isThereTransitionWithoutForwardLinks() {
		return warnings[TRANSITION_WITHOUT_FORWARD_LINKS];
	}

	/**
	 * Returns true if any transitions have invalid outcomes for some modes,
	 * false otherwise.
	 *
	 * @return true if any transitions have invalid outcomes for some modes,
	 * false otherwise.
	 */
	public boolean isThereTransitionWithInvalidOutcomes() {
		return warnings[TRANSITION_WITH_INVALID_OUTCOMES];
	}

	/**
	 * Returns a Vector<Object> containing the keys of the classes without a
	 * reference station.
	 *
	 * @return a Vector<Object> containing the keys of the classes without a
	 * reference station.
	 */
	public Vector<Object> getKeysOfClassesWithoutRefStation() {
		return classesWithoutRefStation;
	}

	/**
	 * Returns a Vector<Object> containing the keys of the sources without
	 * associated open classes.
	 *
	 * @return a Vector<Object> containing the keys of the sources without
	 * associated open classes.
	 */
	public Vector<Object> getKeysOfSourceWithoutOpenClass() {
		return sourceWithoutOpenClass;
	}

	/**
	 * Returns a Vector<Object> containing the keys of the stations with link
	 * problems.
	 *
	 * @return a Vector<Object> containing the keys of the stations with link
	 * problems.
	 */
	public Vector<Object> getKeysOfStationsWithLinkProblems() {
		return stationsWithLinkErrors;
	}

	/**
	 * Returns a Vector<Object> containing the keys of empty blocking regions.
	 *
	 * @return a Vector<Object> containing the keys of empty blocking regions.
	 */
	public Vector<Object> getKeysOfEmptyBlockingRegions() {
		return emptyBlockingRegions;
	}

	/**
	 * Returns a HashMap where the key is the key of a close class. For each
	 * close class with a routing problem it contains a Vector<Object> with the
	 * keys of the stations where the routing problems occur.
	 *
	 * @return a HashMap where the key is the key of a close class.
	 */
	public HashMap<Object, Vector<Object>> getKeysOfRoutingProblems() {
		return routingErrors;
	}

	/**
	 * Returns a HashMap where the key is the key of a close class. For each
	 * close class that may be routed into a station whose forward stations are
	 * all sinks it contains a Vector<Object> with the keys of the stations where
	 * the problems occur.
	 *
	 * @return a HashMap where the key is the key of a close class.
	 */
	public HashMap<Object, Vector<Object>> getKeysOfAllForwardStationsAreSinkErrors() {
		return allForwardStationsAreSinkErrors;
	}

	/**
	 * Returns a Vector<Object> containing the keys of stations (no sources or
	 * sinks) not backward linked.
	 *
	 * @return a Vector<Object> containing the keys of stations (no sources or
	 * sinks) not backward linked.
	 */
	public Vector<Object> getKeysOfStationWithoutBackwardLinks() {
		return stationWithoutBackwardLinks;
	}

	/**
	 * Returns a Vector<Object> containing the keys of servers with per class
	 * different queue strategies.
	 *
	 * @return a Vector<Object> containing the keys of servers with per class
	 * different queue strategies.
	 */
	public Vector<Object> getBCMPserversWithDifferentQueueStrategy() {
		return BCMPserversWithDifferentQueueStrategy;
	}

	/**
	 * Returns a Vector<Object> containing the keys of servers with FCFS queueing
	 * strategy but mixed types of service.
	 *
	 * @return a Vector<Object> containing the keys of servers with FCFS queueing
	 * strategy but mixed types of service.
	 */
	public Vector<Object> getBCMPserversWithDifferentServiceTypes() {
		return BCMPserversWithDifferentServiceTypes;
	}

	/**
	 * Returns a Vector<Object> containing the keys of servers with FCFS queueing
	 * strategy but a non exponential distribution.
	 *
	 * @return a Vector<Object> containing the keys of servers with FCFS queueing
	 * strategy but a non exponential distribution.
	 */
	public Vector<Object> getBCMPserversFCFSWithoutExponential() {
		return BCMPserversFCFSWithoutExponential;
	}

	/**
	 * Returns a Vector<Object> containing the keys of servers with FCFS queueing
	 * strategy, exponential distribution but different per class service times.
	 *
	 * @return a Vector<Object> containing the keys of servers with FCFS queueing
	 * strategy, exponential distribution but different per class service times.
	 */
	public Vector<Object> getBCMPFcfsServersWithDifferentServiceTimes() {
		return BCMPFcfsServersWithDifferentServiceTimes;
	}

	/**
	 * Returns a Vector<Object> containing the keys of delays with at least a
	 * service time distribution with a non rational Laplace transform.
	 *
	 * @return a Vector<Object> containing the keys of delays with at least a
	 * service time distribution with a non rational Laplace transform.
	 */
	public Vector<Object> getBCMPdelaysWithNonRationalServiceDistribution() {
		return BCMPdelaysWithNonRationalServiceDistribution;
	}

	/**
	 * Returns a Vector<Object> containing the keys of servers with per class
	 * different queue strategies.
	 *
	 * @return a Vector<Object> containing the keys of servers with per class
	 * different queue strategies.
	 */
	public Vector<Object> getBCMPserversWithNonSymmetricScheduling() {
		return BCMPserverWithNonSymmetricSchedulingPolicy;
	}

	/**
	 * Returns a Vector<Object> containing the keys of css with a matrix in which
	 * some rows sum to a value less than one.
	 *
	 * @return a Vector<Object> containing the keys of css with a matrix in which
	 * some rows sum to a value less than one.
	 */
	public Vector<Object> getCsWithWrongMatrix() {
		return csWithWrongMatrix;
	}

	/**
	 * Returns a Vector<Object> containing the keys of css followed by a queue
	 * implementing a drop bas strategy.
	 *
	 * @return a Vector<Object> containing the keys of css followed by a queue
	 * implementing a drop bas strategy.
	 */
	public Vector<Object> getCsFolloweByBas() {
		return csFollowedByBas;
	}

	/**
	 * Returns a Vector<Object> containing the keys of css between fork/join.
	 *
	 * @return a Vector<Object> containing the keys of css between fork/join.
	 */
	public Vector<Object> getCsBetweenForkJoin() {
		return csBetweenForkJoin;
	}

	/**
	 * Returns a Vector<Object> containing classes that are defined system
	 * throughput when class switch exists in model.
	 *
	 * @return a Vector<Object> containing classes that are defined system
	 * throughput when class switch exists in model.
	 */
	public Vector<Object> getCsSystemThroughputClass() {
		return csSystemThroughputClass;
	}

	/**
	 * Returns a Vector<Object> containing closed classes that has zero
	 * populations.
	 *
	 * @return a Vector<Object> containing closed classes that has zero
	 * populations.
	 */
	public Vector<Object> getClosedZeroPopulation() {
		return closedZeroPopulation;
	}

	public Map<Object, HashMap<Object, Object>> getInvalidLoadDependentRoutingStations() {
		return invalidLoadDependentRouting;
	}

	/**
	 * Returns a HashMap where the key is the key of a join. For each join that
	 * has Guard strategies with zero tasks it contains a Vector<Object> with the
	 * keys of the classes for which the problem occurs.
	 *
	 * @return a HashMap where the key is the key of a join.
	 */
	public HashMap<Object, Vector<Object>> getKeysOfAllGuardStrategyZeroTask() {
		return guardStrategyZeroTasks;
	}

	/**
	 * Returns a Vector<Object> containing the keys of semaphores not between
	 * fork/join.
	 *
	 * @return a Vector<Object> containing the keys of semaphores not between
	 * fork/join.
	 */
	public Vector<Object> getSemaphoreNotBetweenForkJoin() {
		return semaphoreNotBetweenForkJoin;
	}

	/**
	 * Returns a Vector<Object> containing the keys of scalers not between
	 * fork/join.
	 *
	 * @return a Vector<Object> containing the keys of scalers not between
	 * fork/join.
	 */
	public Vector<Object> getScalerNotBetweenForkJoin() {
		return scalerNotBetweenForkJoin;
	}

	/**
	 * Returns a Vector<Object> containing the keys of transitions between
	 * fork/join.
	 *
	 * @return a Vector<Object> containing the keys of transitions between
	 * fork/join.
	 */
	public Vector<Object> getTransitionBetweenForkJoin() {
		return transitionBetweenForkJoin;
	}

	/**
	 * Returns a Vector<Object> containing the keys of transitions without forward
	 * links.
	 *
	 * @return a Vector<Object> containing the keys of transitions without forward
	 * links.
	 */
	public Vector<Object> getTransitionWithoutForwardLinks() {
		return transitionWithoutForwardLinks;
	}

	/**
	 * Returns a Vector<Object> containing the keys of transitions have invalid
	 * conditions for some modes.
	 *
	 * @return a Vector<Object> containing the keys of transitions have invalid
	 * conditions for some modes.
	 */
	public Vector<Object> getTransitionWithInvalidConditions() {
		return transitionWithInvalidConditions;
	}

	/**
	 * Returns a Vector<Object> containing the keys of transitions have invalid
	 * outcomes for some modes.
	 *
	 * @return a Vector<Object> containing the keys of transitions have invalid
	 * outcomes for some modes.
	 */
	public Vector<Object> getTransitionWithInvalidOutcomes() {
		return transitionWithInvalidOutcomes;
	}

	/**
	 * Checks that there is at least one class defined. If a problem is found
	 * it raises to "true" the corresponding position inside the problems array.
	 */
	private void checkForNoClassError() {
		if (class_def.getClassKeys().isEmpty()) {
			errors[NO_CLASS_ERROR] = true;
		}
	}

	/**
	 * Checks that there is at least one station defined. If a problem is found
	 * it raises to "true" the corresponding position inside the problems array.
	 */
	private void checkForNoStationError() {
		if (station_def.getStationKeysNoSourceSink().isEmpty()) {
			errors[NO_STATION_ERROR] = true;
		}
	}

	/**
	 * Checks that there is at least one measure defined. If a problem is found
	 * it raises to "true" the corresponding position inside the problems array.
	 */
	private void checkForNoMeasureError() {
		if (simulation_def.getMeasureKeys().isEmpty()) {
			errors[NO_MEASURE_ERROR] = true;
		}
	}

	/**
	 * Checks that every class has a reference station. If a problem is found it
	 * raises to "true" the corresponding position inside the problems array.
	 */
	private void checkForRefStationError() {
		Vector<Object> classes = class_def.getClassKeys();
		for (int i = 0; i < classes.size(); i++) {
			Object thisClass = classes.get(i);
			Object thisRefStation = class_def.getClassRefStation(thisClass);
			if (thisRefStation == null) {
				classesWithoutRefStation.add(thisClass);
				errors[REFERENCE_STATION_ERROR] = true;
			}
		}
	}

	/**
	 * Checks if there is at least a source without associated open classes
	 */
	private void checkForSourceWithoutOpenClassError() {
		Vector<Object> sources = station_def.getStationKeysSource();
		for (int i = 0; i < sources.size(); i++) {
			Object thisSource = sources.get(i);
			boolean hasAssoicatedOpenClass = false;
			Vector<Object> openClasses = class_def.getOpenClassKeys();
			for (int j = 0; j < openClasses.size(); j++) {
				Object thisOpenClass = openClasses.get(j);
				if (thisSource == class_def.getClassRefStation(thisOpenClass)) {
					hasAssoicatedOpenClass = true;
					break;
				}
			}
			if (!hasAssoicatedOpenClass) {
				sourceWithoutOpenClass.add(thisSource);
				errors[SOURCE_WITHOUT_OPEN_CLASS_ERROR] = true;
			}
		}
	}

	/**
	 * Checks if there is at least a sink but no open classes have been defined
	 */
	private void checkForSinkButNoOpenClassError() {
		Vector<Object> sinks = station_def.getStationKeysSink();
		Vector<Object> openClasses = class_def.getOpenClassKeys();
		if (!sinks.isEmpty() && openClasses.isEmpty()) {
			errors[SINK_BUT_NO_OPEN_CLASS_ERROR] = true;
		}
	}

	/**
	 * Checks if there is at least an open class but no sources have been defined
	 */
	private void checkForOpenClassButNoSourceError() {
		Vector<Object> openClasses = class_def.getOpenClassKeys();
		Vector<Object> sources = station_def.getStationKeysSource();
		if (!openClasses.isEmpty() && sources.isEmpty()) {
			errors[OPEN_CLASS_BUT_NO_SOURCE_ERROR] = true;
		}
	}

	/**
	 * Checks if there is at least an open class but no sinks have been defined
	 */
	private void checkForOpenClassButNoSinkError() {
		Vector<Object> openClasses = class_def.getOpenClassKeys();
		Vector<Object> sinks = station_def.getStationKeysSink();
		if (!openClasses.isEmpty() && sinks.isEmpty()) {
			errors[OPEN_CLASS_BUT_NO_SINK_ERROR] = true;
		}
	}

	/**
	 * Checks: 1) Each Source is forward linked to something. 2) Each Server is
	 * forward linked to something. 3) Each Delay is forward linked to
	 * something. 4) Each Sink is backward linked to something. If a problem is
	 * found it raises to "true" the corresponding position inside the problems
	 * array.
	 */
	private void checkForStationLinkError() {
		//get the vector of the keys of the stations
		Vector<Object> stations = station_def.getStationKeys();
		for (int i = 0; i < stations.size(); i++) {
			//get the key of the station at i
			Object thisStation = stations.get(i);
			//get the type of the station
			String thisStationType = station_def.getStationType(thisStation);
			if (thisStationType.equals(STATION_TYPE_PLACE) || thisStationType.equals(STATION_TYPE_TRANSITION)) {
				continue;
			} else if (thisStationType.equals(STATION_TYPE_SOURCE)) {
				//get the vector of forward links
				Vector<Object> forwardStations = station_def.getForwardConnections(thisStation);
				//if the vector is empty the Source is not forward connected
				if (forwardStations.isEmpty()) {
					stationsWithLinkErrors.add(thisStation);
					errors[STATION_LINK_ERROR] = true;
				}
			} else if (thisStationType.equals(STATION_TYPE_SINK)) {
				//get the vector of backward links
				Vector<Object> backwardStations = station_def.getBackwardConnections(thisStation);
				//if the vector is empty the Sink is not backward connected
				if (backwardStations.isEmpty()) {
					stationsWithLinkErrors.add(thisStation);
					errors[STATION_LINK_ERROR] = true;
				}
			} else {
				//get the vector of forward links
				Vector<Object> forwardStations = station_def.getForwardConnections(thisStation);
				//if the vector is empty the Server is not forward connected
				if (forwardStations.isEmpty()) {
					stationsWithLinkErrors.add(thisStation);
					errors[STATION_LINK_ERROR] = true;
				}
			}
		}
	}

	/**
	 * Checks if there are empty blocking regions.
	 */
	private void checkForEmptyBlockingRegions() {
		Vector<Object> regionKeys = blocking_def.getRegionKeys();
		for (Object regionKey : regionKeys) {
			if (blocking_def.getBlockingRegionStations(regionKey).isEmpty()) {
				emptyBlockingRegions.add(regionKey);
				errors[EMPTY_BLOCKING_REGION] = true;
			}
		}
	}

	/**
	 * Checks if preloading in blocking regions exceeds the constraints.
	 */
	private void checkForPreloadingInBlockingRegions() {
		Vector<Object> regionKeys = blocking_def.getRegionKeys();
		Vector<Object> classKeys = class_def.getClassKeys();
		for (Object regionKey : regionKeys) {
			int totalPreloaded = 0;
			int[] totalPreloadedClass = new int[classKeys.size()];
			Arrays.fill(totalPreloadedClass, 0);

			Set<Object> stationKeys = blocking_def.getBlockingRegionStations(regionKey);
			for (Object stationKey : stationKeys) {
				for (int i = 0; i < classKeys.size(); i++) {
					totalPreloaded += simulation_def.getPreloadedJobs(stationKey, classKeys.get(i)).intValue();
					totalPreloadedClass[i] += simulation_def.getPreloadedJobs(stationKey, classKeys.get(i)).intValue();
				}
			}

			int globalConstraint = blocking_def.getRegionCustomerConstraint(regionKey).intValue();
			if (globalConstraint > 0 && totalPreloaded > globalConstraint) {
				errors[PRELOADING_IN_BLOCKING_REGION] = true;
			}

			for (int i = 0; i < classKeys.size(); i++) {
				int classConstraint = blocking_def.getRegionClassCustomerConstraint(regionKey, classKeys.get(i)).intValue();
				if (classConstraint > 0 && totalPreloadedClass[i] > classConstraint) {
					errors[PRELOADING_IN_BLOCKING_REGION] = true;
				}
			}
		}
	}

	/**
	 * Checks that no closed classes may be routed into a Sink. If a problem is
	 * found it raises to "true" the corresponding position inside the errors
	 * array.
	 */
	@SuppressWarnings("unchecked")
	private void checkForRoutingError() {
		//vector collecting all the station keys where routing problems occur
		// for this close class
		Vector<Object> problemsPerClass = new Vector<Object>(0, 1);
		Vector<Object> closedClassKeys = class_def.getClosedClassKeys();
		Vector<Object> startingPoints = new Vector<Object>(0, 1);
		//get the vector of the possible starting points
		Vector<Object> noSourceSink = station_def.getStationKeysNoSourceSink();
		for (int i = 0; i < closedClassKeys.size(); i++) {
			//remove all elements from the problemsPerClass vector
			problemsPerClass.removeAllElements();
			//create the vector containing the already visited station keys
			Vector<Object> alreadyVisited = new Vector<Object>(0, 1);
			//get the class at i
			Object thisClassKey = closedClassKeys.get(i);
			//checks for each possible starting point if there are preloaded jobs for the class i.
			//In that case put it into the startingPoints vector
			for (int j = 0; j < noSourceSink.size(); j++) {
				Object thisElementKey = noSourceSink.get(j);
				int preloadedJobs = simulation_def.getPreloadedJobs(thisElementKey, thisClassKey).intValue();
				if (preloadedJobs > 0) {
					startingPoints.add(thisElementKey);
				}
			}
			//start the explore algorithm for each of the station containing some preloaded jobs for class i
			for (int j = 0; j < startingPoints.size(); j++) {
				//get the element at j
				Object thisStartingPoint = startingPoints.get(j);
				//if this possible starting point has not been visited yet
				if (!alreadyVisited.contains(thisStartingPoint)) {
					alreadyVisited.add(thisStartingPoint);
					Vector<Object> problemsForThisStartingPoint = exploreForRoutingProblems(thisClassKey,
							thisStartingPoint, alreadyVisited);
					for (int k = 0; k < problemsForThisStartingPoint.size(); k++) {
						problemsPerClass.add(problemsForThisStartingPoint.get(k));
					}
				}
			}
			if (!problemsPerClass.isEmpty()) {
				routingErrors.put(thisClassKey, (Vector<Object>) problemsPerClass.clone());
				errors[ROUTING_ERROR] = true;
			}
		}
	}

	/**
	 * Checks that no closed classes may be routed into a station whose forward
	 * station(s) are all sinks. If a problem is found it raises to "true" the
	 * corresponding position inside the errors array.
	 */
	@SuppressWarnings("unchecked")
	private void checkForAllForwardStationsAreSinkError() {
		//vector collecting all the station keys where errors occur
		// for this close class
		Vector<Object> problemsPerClass = new Vector<Object>(0, 1);
		Vector<Object> closedClassKeys = class_def.getClosedClassKeys();
		Vector<Object> startingPoints = new Vector<Object>(0, 1);
		//get the vector of the possible starting points
		Vector<Object> noSourceSink = station_def.getStationKeysNoSourceSink();
		for (int i = 0; i < closedClassKeys.size(); i++) {
			//remove all elements from the problemsPerClass vector
			problemsPerClass.removeAllElements();
			//create the vector containing the already visited station keys
			Vector<Object> alreadyVisited = new Vector<Object>(0, 1);
			//get the class at i
			Object thisClassKey = closedClassKeys.get(i);
			//checks for each possible starting point if there are preloaded jobs for the class i.
			//In that case put it into the startingPoints vector
			for (int j = 0; j < noSourceSink.size(); j++) {
				Object thisElementKey = noSourceSink.get(j);
				int preloadedJobs = simulation_def.getPreloadedJobs(thisElementKey, thisClassKey).intValue();
				if (preloadedJobs > 0) {
					startingPoints.add(thisElementKey);
				}
			}
			//start the explore algorithm for each of the station containing some preloaded jobs for class i
			for (int j = 0; j < startingPoints.size(); j++) {
				//get the element at j
				Object thisStartingPoint = startingPoints.get(j);
				//if this possible starting point has not been visited yet
				if (!alreadyVisited.contains(thisStartingPoint)) {
					alreadyVisited.add(thisStartingPoint);
					Vector<Object> problemsForThisStartingPoint = exploreForAllForwardStationAreSink(thisClassKey,
							thisStartingPoint, alreadyVisited);
					for (int k = 0; k < problemsForThisStartingPoint.size(); k++) {
						problemsPerClass.add(problemsForThisStartingPoint.get(k));
					}
				}
			}
			if (!problemsPerClass.isEmpty()) {
				allForwardStationsAreSinkErrors.put(thisClassKey, (Vector<Object>) problemsPerClass.clone());
				errors[ALL_FORWARD_STATION_ARE_SINK_ERROR] = true;
			}
		}
	}

	/**
	 * Checks that each measure is defined only one time.
	 */
	private void checkForMeasureError() {
		Vector<Object> measures = simulation_def.getMeasureKeys();
		Vector<String> measuresAlreadyChecked = new Vector<String>(0, 1);
		for (int i = 0; i < measures.size(); i++) {
			Object thisMeasure = measures.get(i);
			String thisMeasureType = simulation_def.getMeasureType(thisMeasure);
			String thisMeasureClass;
			if (simulation_def.getMeasureClass(thisMeasure) == null) {
				thisMeasureClass = "ALL";
			} else {
				thisMeasureClass = (thisMeasureType.equals(SimulationDefinition.MEASURE_FX))
						? (String) simulation_def.getMeasureClass(thisMeasure)
						: class_def.getClassName(simulation_def.getMeasureClass(thisMeasure));
			}
			String thisMeasureStation;
			if (simulation_def.getMeasureStation(thisMeasure) == null) {
				thisMeasureStation = "ALL";
			} else {
				thisMeasureStation = station_def.getStationName(simulation_def.getMeasureStation(thisMeasure));
			}
			String thisMeasureDescription = thisMeasureType + thisMeasureClass + thisMeasureStation;
			if (!measuresAlreadyChecked.contains(thisMeasureDescription)) {
				measuresAlreadyChecked.add(thisMeasureDescription);
			} else {
				redundantMeasure.add(thisMeasure);
				errors[DUPLICATE_MEASURE_ERROR] = true;
			}
		}
	}

	/**
	 * Checks that each performance index is consistent, i.e. it has no 'null'
	 * field in reference station.
	 */
	private void checkForInconsistentMeasureError() {
		Vector<Object> measures = simulation_def.getMeasureKeys();
		for (int i = 0; i < measures.size(); i++) {
			Object thisMeasure = measures.get(i);
			String thisMeasureType = simulation_def.getMeasureType(thisMeasure);
			Object thisMeasureStationKey = simulation_def.getMeasureStation(thisMeasure);
			if ((thisMeasureType == null) || (thisMeasureStationKey == null && !simulation_def.isGlobalMeasure(thisMeasure))) {
				inconsistentMeasures.add(thisMeasure);
				errors[INCONSISTENT_MEASURE_ERROR] = true;
			}
		}
	}

	private void checkForLoadDependentRoutingError() {
		for (Object stationKey : station_def.getStationKeys()) {
			for (Object classKey : class_def.getClassKeys()) {
				RoutingStrategy strategy = (RoutingStrategy) station_def.getRoutingStrategy(stationKey, classKey);
				if (strategy instanceof LoadDependentRouting) {
					List<String> errorMsgs = ((LoadDependentRouting) strategy).validate();
					if (errorMsgs != null && errorMsgs.size() > 0) {
						HashMap<Object, Object> innerVal = new HashMap<Object, Object>();
						innerVal.put(stationKey, classKey);
						invalidLoadDependentRouting.put(strategy, innerVal);
						errors[LOAD_DEPENDENT_ROUTING_INVALID] = true;
					}
				}
			}
		}
	}

	/**
	 * Explores the model, searching for routing problems for the specified
	 * close class
	 *
	 * @param classKey the key of the class to be analyzed
	 * @param startingStationKey the key of the station where we start analyzing
	 * the routing
	 * @param alreadyVisited vector containing the keys of the already visited
	 * stations
	 * @return the Vector<Object> containing the keys of the stations where the
	 * routing problem occurs
	 */
	private Vector<Object> exploreForRoutingProblems(Object classKey, Object startingStationKey, Vector<Object> alreadyVisited) {
		//Vector<Object> containing the keys to be returned
		Vector<Object> toBeReturned = new Vector<Object>(0, 1);
		//Vector<Object> containing the keys of the forward stations
		Vector<Object> forwardStations = station_def.getForwardConnections(startingStationKey);
		for (int i = 0; i < forwardStations.size(); i++) {
			// ... get the forward station at i ...
			Object thisForwardStation = forwardStations.get(i);
			//... if it has not been explored yet ...
			if (!alreadyVisited.contains(thisForwardStation)) {
				//... add the element to the alreadyVisited vector ...
				alreadyVisited.add(thisForwardStation);
				// ... get the routing strategy for the class in the starting station ...
				RoutingStrategy strategy = (RoutingStrategy) station_def.getRoutingStrategy(startingStationKey, classKey);
				// ... if thisForwardStation is a Sink ...
				if (station_def.getStationType(thisForwardStation).equals(STATION_TYPE_SINK)) {
					// ... if the routing strategy is the Probability Routing ...
					if (strategy instanceof ProbabilityRouting) {
						Map<Object, Double> probabilities = strategy.getValues();
						// ... get the routing probability toward thisForwardStation ...
						double p = probabilities.get(thisForwardStation).doubleValue();
						// ... if p = 1 there is an error, put startingStationKey into the
						// toBeReturned vector ...
						if (p == 1) {
							toBeReturned.add(startingStationKey);
						}
					}
				} // ... else thisForwardStation is not a Sink ...
				else {
					Vector<Object> temp;
					// ... if the routing policy is ProbabilityRouting ...
					if (strategy instanceof ProbabilityRouting) {
						Map<Object, Double> probabilities = strategy.getValues();
						// ... get the routing probability toward thisForwardStation ...
						double p = probabilities.get(thisForwardStation).doubleValue();
						// ... if p != 0 start exploring from thisForwardStation and collect
						// the returned vector into temp ...
						if (p != 0) {
							temp = exploreForRoutingProblems(classKey, thisForwardStation, alreadyVisited);
						} else {
							temp = null;
						}
					} else {
						temp = exploreForRoutingProblems(classKey, thisForwardStation, alreadyVisited);
					}
					// ... if temp have been used ...
					if (temp != null) {
						// ... put its elements into the toBeReturned vector
						for (int j = 0; j < temp.size(); j++) {
							toBeReturned.add(temp.get(j));
						}
					}
				}
			}
		}
		// return the vector containing the keys of the stations where problems occur
		return toBeReturned;
	}

	/**
	 * Explores the model, searching for routing problems for the specified
	 * close class
	 *
	 * @param classKey the key of the class to be analyzed
	 * @param startingStationKey the key of the station where we start analyzing
	 * the routing
	 * @param alreadyVisited vector containing the keys of the already visited
	 * stations
	 * @return the Vector<Object> containing the keys of the stations where the
	 * error occurs
	 */
	private Vector<Object> exploreForAllForwardStationAreSink(Object classKey, Object startingStationKey, Vector<Object> alreadyVisited) {
		boolean allSink = true;
		//Vector<Object> containing the keys to be returned
		Vector<Object> toBeReturned = new Vector<Object>(0, 1);
		//Vector<Object> containing the keys of the forward stations
		Vector<Object> forwardStations = station_def.getForwardConnections(startingStationKey);
		//Check if forwardStations are all sinks
		if (forwardStations.size() == 0) {
			allSink = false;
		} else {
			for (int i = 0; i < forwardStations.size(); i++) {
				Object thisStation = forwardStations.get(i);
				if (!station_def.getStationType(thisStation).equals(STATION_TYPE_SINK)) {
					allSink = false;
				}
			}
		}
		//if forward station are all sinks ...
		if (allSink) {
			//... add this startingStationKey to toBeReturned vector
			toBeReturned.add(startingStationKey);
		} else {
			for (int i = 0; i < forwardStations.size(); i++) {
				// ... get the forward station at i ...
				Object thisForwardStation = forwardStations.get(i);
				//... if it has not been explored yet ...
				if (!alreadyVisited.contains(thisForwardStation)) {
					Vector<Object> temp = null;
					//... add the element to the alreadyVisited vector ...
					alreadyVisited.add(thisForwardStation);
					// ... if thisForwardStation is not a Sink ...
					if (!station_def.getStationType(thisForwardStation).equals(STATION_TYPE_SINK)) {
						// ... get the routing strategy for the class in the starting station ...
						RoutingStrategy strategy = (RoutingStrategy) station_def.getRoutingStrategy(startingStationKey, classKey);
						if (strategy instanceof ProbabilityRouting) {
							Map<Object, Double> probabilities = strategy.getValues();
							// ... get the routing probability toward thisForwardStation ...
							double p = probabilities.get(thisForwardStation).doubleValue();
							// ... if p != 0 start exploring from thisForwardStation and collect
							// the returned vector into temp ...
							if (p != 0) {
								//... start exploring from thisForwardStation
								temp = exploreForAllForwardStationAreSink(classKey, thisForwardStation, alreadyVisited);
							}
						} else if (strategy instanceof LoadDependentRouting) {
							String stationName = station_def.getStationName(thisForwardStation);
							LoadDependentRouting loadDependentRouting = (LoadDependentRouting) strategy;
							Map<Integer, LoadDependentRouting.EmpiricalEntry[]> empiricalEntries = loadDependentRouting.getEmpiricalEntries();
							Iterator<LoadDependentRouting.EmpiricalEntry[]> rows = empiricalEntries.values().iterator();
							Collection<LoadDependentRouting.EmpiricalEntry> allEntries = new ArrayList<LoadDependentRouting.EmpiricalEntry>(5);
							while (rows.hasNext()) {
								LoadDependentRouting.EmpiricalEntry[] entries = rows.next();
								allEntries.addAll(Arrays.asList(entries));
							}
							for (Iterator<LoadDependentRouting.EmpiricalEntry> itEntries = allEntries.iterator(); itEntries.hasNext();) {
								LoadDependentRouting.EmpiricalEntry entry = itEntries.next();
								if (entry.getStationName().equals(stationName)) {
									double p = entry.getProbability();
									if (p != 0) {
										//... start exploring from thisForwardStation
										temp = exploreForAllForwardStationAreSink(classKey, thisForwardStation, alreadyVisited);
									}
								}
							}
						} else {
							temp = exploreForAllForwardStationAreSink(classKey, thisForwardStation, alreadyVisited);
						}
					}
					//if temp != null insert its elements into toBeReturned vector
					if (temp != null) {
						for (int j = 0; j < temp.size(); j++) {
							toBeReturned.add(temp.get(j));
						}
					}
				}
			}
		}
		return toBeReturned;
	}

	/**
	 * Checks if there are more than one sink. If a problem is found it raises
	 * to "true" the corresponding position inside the warnings array.
	 */
	private void checkForMoreThanOneSinkWarning() {
		Vector<Object> sinks = station_def.getStationKeysSink();
		if (sinks.size() > 1) {
			warnings[MORE_THAN_ONE_SINK_WARNING] = true;
		}
	}

	/**
	 * Checks if there are stations (no sources or sinks) not backward linked
	 */
	private void checkForNoBackwardLinkWarning() {
		Vector<Object> stations = station_def.getStationKeysNoSourceSink();
		for (int i = 0; i < stations.size(); i++) {
			Object thisStation = stations.get(i);
			Vector<Object> backwardStations = station_def.getBackwardConnections(thisStation);
			if (backwardStations.isEmpty()) {
				stationWithoutBackwardLinks.add(thisStation);
				warnings[NO_BACKWARD_LINK_WARNING] = true;
			}
		}
	}

	private void checkForJoinWithoutForkErrors() {
		Vector<Object> forks = station_def.getStationKeysFork();
		Vector<Object> joins = station_def.getStationKeysJoin();
		if (forks.isEmpty() && !joins.isEmpty()) {
			errors[JOIN_WITHOUT_FORK_ERROR] = true;
		}
	}

	private void checkForForkWithoutJoinWarnings() {
		Vector<Object> forks = station_def.getStationKeysFork();
		Vector<Object> joins = station_def.getStationKeysJoin();
		if (!forks.isEmpty() && joins.isEmpty()) {
			warnings[FORK_WITHOUT_JOIN_WARNING] = true;
		}
	}

	/**
	 * Checks the consistency of the parametric analysis model (if one was
	 * defined). If it is inconsistent with simulation model definition it will
	 * be automatically changed when possible.
	 */
	private void checkForParametricAnalysisModelModifiedWarning() {
		if (simulation_def.isParametricAnalysisEnabled()) {
			ParametricAnalysisDefinition pad = simulation_def.getParametricAnalysisModel();
			int code = pad.checkCorrectness(false);
			if (code == 1) {
				warnings[PARAMETRIC_ANALYSIS_MODEL_MODIFIED_WARNING] = true;
			} else if (code == 2) {
				ParametricAnalysisChecker pac = new ParametricAnalysisChecker(class_def, station_def, simulation_def);
				if (pac.canBeEnabled()) {
					warnings[PARAMETRIC_ANALYSIS_MODEL_MODIFIED_WARNING] = true;
				}
			}
		}
	}

	/**
	 * Checks if PA is no more available, but a PA model was previously defined.
	 */
	private void checkForParametricAnalysisNoMoreAvailableWarning() {
		ParametricAnalysisChecker pac = new ParametricAnalysisChecker(class_def, station_def, simulation_def);
		if (simulation_def.isParametricAnalysisEnabled() && !pac.canBeEnabled()) {
			warnings[PARAMETRIC_ANALYSIS_NO_MORE_AVAILABLE_WARNING] = true;
		}
	}

	private void checkForBCMPDifferentQueueingStrategyWarning() {
		Vector<Object> servers = station_def.getStationKeysServer();
		Vector<Object> classes = class_def.getClassKeys();
		for (int i = 0; i < servers.size(); i++) {
			Object thisServer = servers.get(i);
			String stationStrategy = station_def.getStationQueueStrategy(thisServer);
			if (stationStrategy.equals(CommonConstants.QUEUE_STRATEGY_STATION_PS)
					|| stationStrategy.equals(CommonConstants.QUEUE_STRATEGY_STATION_SRPT)) {
				continue;
			}
			for (int j = 1; j < classes.size(); j++) {
				String lastStrategy = station_def.getQueueStrategy(thisServer, classes.get(j - 1));
				String thisStrategy = station_def.getQueueStrategy(thisServer, classes.get(j));
				if (!thisStrategy.equals(lastStrategy)) {
					BCMPserversWithDifferentQueueStrategy.add(thisServer);
					warnings[BCMP_DIFFERENT_QUEUEING_STRATEGIES_WARNING] = true;
					break;
				}
			}
		}
	}

	private void checkForBCMPFcfsDifferentServiceTypesWarning() {
		Vector<Object> servers = station_def.getStationKeysServer();
		for (int i = 0; i < servers.size(); i++) {
			Object thisServer = servers.get(i);
			if (isAllFCFSQueueingStrategy(thisServer) && !isTypeUniformServiceStrategy(thisServer)) {
				BCMPserversWithDifferentServiceTypes.add(thisServer);
				warnings[BCMP_FCFS_DIFFERENT_SERVICE_TYPES_WARNING] = true;
			}
		}
	}

	private void checkForBCMPFcfsNonExponentialWarning() {
		Vector<Object> servers = station_def.getStationKeysServer();
		Vector<Object> classes = class_def.getClassKeys();
		for (int i = 0; i < servers.size(); i++) {
			Object thisServer = servers.get(i);
			String stationStrategy = station_def.getStationQueueStrategy(thisServer);
			if (stationStrategy.equals(CommonConstants.QUEUE_STRATEGY_STATION_PS)
					|| stationStrategy.equals(CommonConstants.QUEUE_STRATEGY_STATION_SRPT)) {
				continue;
			}
			if (isAllFCFSQueueingStrategy(thisServer) && isTypeUniformServiceStrategy(thisServer)) {
				boolean ok = true;
				for (int j = 0; j < classes.size(); j++) {
					Object thisClass = classes.get(j);
					Object service = station_def.getServiceTimeDistribution(thisServer, thisClass);
					if (service instanceof Distribution) {
						Distribution d = (Distribution) service;
						if (!d.getName().equals(CommonConstants.DISTRIBUTION_EXPONENTIAL)) {
							BCMPserversFCFSWithoutExponential.add(thisServer);
							warnings[BCMP_FCFS_EXPONENTIAL_WARNING] = true;
							break;
						}
					} else if (service instanceof LDStrategy) {
						LDStrategy ld = (LDStrategy) service;
						Object[] ranges = ld.getAllRanges();
						for (Object range : ranges) {
							Distribution d = ld.getRangeDistribution(range);
							if (!d.getName().equals(CommonConstants.DISTRIBUTION_EXPONENTIAL)) {
								BCMPserversFCFSWithoutExponential.add(thisServer);
								warnings[BCMP_FCFS_EXPONENTIAL_WARNING] = true;
								ok = false;
								break;
							}
						}
						if (!ok) {
							break;
						}
					}
				}
			}
		}
	}

	private void checkForBCMPFcfsDifferentServiceTimesWarning() {
		Vector<Object> servers = station_def.getStationKeysServer();
		Vector<Object> classes = class_def.getClassKeys();
		if (!servers.isEmpty() && !classes.isEmpty()) {
			for (int i = 0; i < servers.size(); i++) {
				Object thisServer = servers.get(i);
				String stationStrategy = station_def.getStationQueueStrategy(thisServer);
				if (stationStrategy.equals(CommonConstants.QUEUE_STRATEGY_STATION_PS)) {
					continue;
				}
				if (mustHaveSameExponentialServiceTimes(thisServer) && !BCMPserversFCFSWithoutExponential.contains(thisServer)) {
					Object service = station_def.getServiceTimeDistribution(thisServer, classes.get(0));
					if (service instanceof Distribution) {
						double mean = ((Distribution) service).getMean();
						for (int j = 0; j < classes.size(); j++) {
							Object thisClass = classes.get(j);
							Distribution d = (Distribution) station_def.getServiceTimeDistribution(thisServer, thisClass);
							double thisMean = d.getMean();
							if (thisMean != mean) {
								BCMPFcfsServersWithDifferentServiceTimes.add(thisServer);
								warnings[BCMP_FCFS_DIFFERENT_SERVICE_TIMES_WARNING] = true;
								break;
							}
						}
					} else if (service instanceof LDStrategy) {
						LDStrategy ld = (LDStrategy) service;
						for (int j = 0; j < classes.size(); j++) {
							Object thisClass = classes.get(j);
							LDStrategy thisLd = (LDStrategy) station_def.getServiceTimeDistribution(thisServer, thisClass);
							if (!ld.isEquivalent(thisLd)) {
								BCMPFcfsServersWithDifferentServiceTimes.add(thisServer);
								warnings[BCMP_FCFS_DIFFERENT_SERVICE_TIMES_WARNING] = true;
								break;
							}
						}
					}
				}
			}
		}
	}

	private void checkForBCMPProcessorSharingWarning() {
		//TODO:implementation
	}

	private void checkForBCMPDelayWarning() {
		Vector<Object> delays = station_def.getStationKeysDelay();
		Vector<Object> classes = class_def.getClassKeys();
		for (int i = 0; i < delays.size(); i++) {
			Object thisDelay = delays.get(i);
			for (int j = 0; j < classes.size(); j++) {
				Object thisClass = classes.get(j);
				Object service = station_def.getServiceTimeDistribution(thisDelay, thisClass);
				if (service instanceof Distribution) {
					Distribution d = (Distribution) service;
					if (d.getName().equals(CommonConstants.DISTRIBUTION_PARETO)) {
						BCMPdelaysWithNonRationalServiceDistribution.add(thisDelay);
						warnings[BCMP_DELAY_WARNING] = true;
						break;
					}
				} else if (service instanceof LDStrategy) {
					LDStrategy ld = (LDStrategy) service;
					Object[] ranges = ld.getAllRanges();
					for (Object range : ranges) {
						if (ld.getRangeDistribution(range).getName().equals(CommonConstants.DISTRIBUTION_PARETO)) {
							BCMPdelaysWithNonRationalServiceDistribution.add(thisDelay);
							warnings[BCMP_DELAY_WARNING] = true;
							break;
						}
					}
				}
			}
		}
	}

	private void checkForBCMPLcfsPreemptiveWarning() {
		//TODO:implementation
	}

	private void checkForBCMPNonStateIndependentRoutingWarning() {
		Vector<Object> stations = station_def.getStationKeysNoSourceSink();
		Vector<Object> classes = class_def.getClassKeys();
		for (int i = 0; i < stations.size(); i++) {
			Object thisStation = stations.get(i);
			for (int j = 0; j < classes.size(); j++) {
				Object thisClass = classes.get(j);
				RoutingStrategy thisRoutingStrategy = (RoutingStrategy) station_def.getRoutingStrategy(thisStation, thisClass);
				if (thisRoutingStrategy != null) {
					if (thisRoutingStrategy.isModelStateDependent()) {
						BCMPnonStateIndependentRoutingStations.add(thisStation);
						warnings[BCMP_NON_STATE_INDEPENDENT_ROUTING_WARNING] = true;
						break;
					}
				}
			}
		}
	}

	private void checkForBCMPNonSymmetricSchedulingPolicies() {
		Vector<Object> servers = station_def.getStationKeysServer();
		Vector<Object> classes = class_def.getClassKeys();
		for (int i = 0; i < servers.size(); i++) {
			Object thisServer = servers.get(i);
			String stationStrategy = station_def.getStationQueueStrategy(thisServer);
			if (stationStrategy.equals(CommonConstants.QUEUE_STRATEGY_STATION_PS)) {
				continue;
			}
			for (int j = 0; j < classes.size(); j++) {
				Object thisClass = classes.get(j);
				String strategy = station_def.getQueueStrategy(thisServer, thisClass);
				if (strategy.equals(CommonConstants.QUEUE_STRATEGY_FCFS)
						|| strategy.equals(CommonConstants.QUEUE_STRATEGY_RAND)) {
					continue;
				}
				BCMPserverWithNonSymmetricSchedulingPolicy.add(thisServer);
				warnings[BCMP_NON_SYMMETRIC_SCHEDULING] = true;
				break;
			}
		}
	}

	private boolean isTypeUniformServiceStrategy(Object stationKey) {
		Vector<Object> classes = class_def.getClassKeys();
		if (!classes.isEmpty()) {
			boolean ldFound = false;
			boolean distrFound = true;
			for (int j = 0; j < classes.size(); j++) {
				Object thisClass = classes.get(j);
				Object service = station_def.getServiceTimeDistribution(stationKey, thisClass);
				if (service instanceof LDStrategy) {
					ldFound = true;
				} else if (service instanceof Distribution) {
					distrFound = true;
				}
			}
			if (ldFound && distrFound) {
				return false;
			}
		}
		return true;
	}

	private boolean isAllFCFSQueueingStrategy(Object serverKey) {
		Vector<Object> classes = class_def.getClassKeys();
		for (int j = 0; j < classes.size(); j++) {
			String strategy = station_def.getQueueStrategy(serverKey, classes.get(j));
			if (!strategy.equals(CommonConstants.QUEUE_STRATEGY_FCFS)) {
				return false;
			}
		}
		return true;
	}

	private boolean mustHaveSameExponentialServiceTimes(Object station) {
		return isAllFCFSQueueingStrategy(station) && isTypeUniformServiceStrategy(station)
				&& station_def.getStationType(station).equals(STATION_TYPE_SERVER);
	}

	/**
	 * Deletes all measure already defined
	 */
	public void deleteRedundantMeasure() {
		for (int i = 0; i < redundantMeasure.size(); i++) {
			Object thisRedundantMeasure = redundantMeasure.get(i);
			simulation_def.removeMeasure(thisRedundantMeasure);
		}
	}

	/**
	 * Deletes all empty blocking regions
	 */
	public void deleteEmptyBlockingRegions() {
		for (int i = 0; i < emptyBlockingRegions.size(); i++) {
			blocking_def.deleteBlockingRegion(emptyBlockingRegions.get(i));
		}
	}

	/**
	 * Converts all state dependent routing strategies to Random Routing
	 * (only used in JMVA conversion)
	 */
	public void setAllStateDependentRoutingStrategyToRandomRouting() {
		Vector<Object> classes = class_def.getClassKeys();
		for (int i = 0; i < BCMPnonStateIndependentRoutingStations.size(); i++) {
			Object thisStation = BCMPnonStateIndependentRoutingStations.get(i);
			for (int j = 0; j < classes.size(); j++) {
				Object thisClass = classes.get(j);
				RoutingStrategy strategy = (RoutingStrategy) station_def.getRoutingStrategy(thisStation, thisClass);
				if (strategy.isModelStateDependent()) {
					station_def.setRoutingStrategy(thisStation, thisClass, new RandomRouting());
				}
			}
		}
	}

	private void checkForSinkPerfIndicesWithNoSink() {
		Vector<Object> sinks = station_def.getStationKeysSink();
		Vector<Object> measures = simulation_def.getMeasureKeys();
		for (int i = 0; i < measures.size(); i++) {
			Object thisMeasure = measures.get(i);
			String thisMeasureType = simulation_def.getMeasureType(thisMeasure);
			boolean sinkPerfIndex = (thisMeasureType.equals(SimulationDefinition.MEASURE_RP_PER_SINK)
					|| thisMeasureType.equals(SimulationDefinition.MEASURE_X_PER_SINK)) ? true : false;
			if (sinkPerfIndex && sinks.isEmpty()) {
				errors[SINK_PERF_IND_WITH_NO_SINK_ERROR] = true;
				break;
			}
		}
	}

	private void checkForSinkPerfIndicesWithClosedClass() {
		Vector<Object> closedClasses = class_def.getClosedClassKeys();
		Vector<Object> measures = simulation_def.getMeasureKeys();
		for (int i = 0; i < measures.size(); i++) {
			Object thisMeasure = measures.get(i);
			String thisMeasureType = simulation_def.getMeasureType(thisMeasure);
			Object thisMeasureClass = simulation_def.getMeasureClass(thisMeasure);
			boolean sinkPerfIndex = (thisMeasureType.equals(SimulationDefinition.MEASURE_RP_PER_SINK)
					|| thisMeasureType.equals(SimulationDefinition.MEASURE_X_PER_SINK)) ? true : false;
			if (sinkPerfIndex && closedClasses.contains(thisMeasureClass)) {
				errors[SINK_PERF_WITH_CLOSED_CLASS_ERROR] = true;
				break;
			}
		}
	}

	public boolean isThereSinkProbabilityUpdateWarning() {
		return warnings[SINK_PROBABILITY_UPDATE_WARNING];
	}

	private void checkForSinkProbabilityUpdateWarning() {
		if (station_def.isSinkProbabilityUpdated()) {
			station_def.setSinkProbabilityUpdatedVar(false);
			warnings[SINK_PROBABILITY_UPDATE_WARNING] = true;
		}
	}

	/**
	 * It checks if exist some class switch components between a fork and a
	 * join.
	 */
	private void checkForCsBetweenForkJoin() {
		Vector<Object> forks = station_def.getStationKeysFork();
		for (int i = 0; i < forks.size(); i++) {
			Object thisFork = forks.get(i);
			Vector<Object> result = searchForStationBetweenForkJoin(STATION_TYPE_CLASSSWITCH, thisFork);
			if (result != null) {
				result.removeAll(csBetweenForkJoin);
				csBetweenForkJoin.addAll(result);
			}
		}
		if (!csBetweenForkJoin.isEmpty()) {
			warnings[CS_BETWEEN_FORK_JOIN] = true;
		}
	}

	/**
	 * It checks the different reference station of closed class if there exists
	 * class switch.
	 */
	private void checkForCsReferenceStation() {
		if (csExists()) {
			Vector<Object> closedClasses = class_def.getClosedClassKeys();
			Object closedRefStation = null;
			for (int i = 0; i < closedClasses.size(); i++) {
				Object thisClosedClass = closedClasses.get(i);
				Object thisRefStation = class_def.getClassRefStation(thisClosedClass);
				if (thisRefStation == null) {
					continue;
				}
				if (closedRefStation == null) {
					closedRefStation = thisRefStation;
				} else if (thisRefStation != closedRefStation) {
					errors[CS_REFERENCE_STATION] = true;
				}
			}
		}
	}

	private void checkForCsSystemThroughputClass() {
		if (csExists()) {
			for (Object measure : simulation_def.getMeasureKeys()) {
				String thisMeasureType = simulation_def.getMeasureType(measure);
				Object thisMeasureClass = simulation_def.getMeasureClass(measure);
				if (thisMeasureType.equals(SimulationDefinition.MEASURE_S_X) && thisMeasureClass != null) {
					csSystemThroughputClass.add(thisMeasureClass);
					errors[CS_SYSTEM_THROUGHPUT_CLASS] = true;
				}
			}
		}
	}

	private void checkForZeroPopulation() {
		if (transitionExists()) {
			return;
		} else if (csExists()) {
			if (!class_def.getClosedClassKeys().isEmpty() && class_def.getTotalClosedClassPopulation() == 0) {
				errors[CS_ALL_CLOSED_ZERO_POPULATION] = true;
			}
		} else {
			for (Object closedClassKey : class_def.getClosedClassKeys()) {
				if (class_def.getClassPopulation(closedClassKey).intValue() == 0) {
					closedZeroPopulation.add(closedClassKey);
					warnings[CLOSED_ZERO_POPULATION] = true;
				}
			}
		}
	}

	private void checkForCsWithForkJoin() {
		if (csExists() && forkExists()) {
			warnings[CS_WITH_FORK_JOIN] = true;
		}
	}

	private boolean csExists() {
		return !station_def.getStationKeysClassSwitch().isEmpty();
	}

	private boolean forkExists() {
		return !station_def.getStationKeysFork().isEmpty();
	}

	private boolean transitionExists() {
		return !station_def.getStationKeysTransition().isEmpty();
	}

	private void checkForGuardStrategyZeroTasks() {
		for (Object stationKey : station_def.getStationKeys()) {
			Vector<Object> classes = new Vector<Object>(0, 1);
			for (Object classKey : class_def.getClassKeys()) {
				Object strategy = station_def.getJoinStrategy(stationKey, classKey);
				if (strategy instanceof GuardJoin) {
					int sum = 0;
					for (Integer i : ((GuardJoin) strategy).getGuard().values()) {
						sum += i;
					}
					if (sum <= 0) {
						classes.add(classKey);
					}
				}
			}
			if (!classes.isEmpty()) {
				guardStrategyZeroTasks.put(stationKey, classes);
				errors[GUARD_STRATEGY_ZERO_TASKS] = true;
			}
		}
	}

	/**
	 * It checks if exist some class switch components followed by a queue
	 * implementing the BAS strategy.
	 */
	private void checkForCsFollowedByBAS() {
		Vector<Object> classSwitches = station_def.getStationKeysClassSwitch();
		for (int i = 0; i < classSwitches.size(); i++) {
			Object thisClassSwitch = classSwitches.get(i);
			Vector<Object> forwardStations = station_def.getForwardConnections(thisClassSwitch);
			for (int j = 0; j < forwardStations.size(); j++) {
				Object thisForwardStation = forwardStations.get(j);
				Vector<Object> classes = class_def.getClassKeys();
				for (int k = 0; k < classes.size(); k++) {
					Object thisClass = classes.get(k);
					String dropRule = station_def.getDropRule(thisForwardStation, thisClass);
					if (dropRule != null && dropRule.equals(CommonConstants.FINITE_BLOCK)) {
						csFollowedByBas.add(thisClassSwitch);
						errors[CS_FOLLOWED_BY_A_BAS] = true;
					}
				}
			}
		}
	}

	/**
	 * It checks if exist some class switch components with a wrong matrix (e.g.
	 * some rows do not sum to a value greater or equals to one.)
	 */
	private void checkForCsMatrixProbabilities() {
		Vector<Object> classSwitches = station_def.getStationKeysClassSwitch();
		Vector<Object> classes = class_def.getClassKeys();
		for (int i = 0; i < classSwitches.size(); i++) {
			Object thisClassSwitch = classSwitches.get(i);
			for (int j = 0; j < classes.size(); j++) {
				Object classInKey = classes.get(j);
				float row = 0;
				for (int k = 0; k < classes.size(); k++) {
					Object classOutKey = classes.get(k);
					row += station_def.getClassSwitchMatrix(thisClassSwitch, classInKey, classOutKey);
				}
				if (row < 1) {
					csWithWrongMatrix.add(thisClassSwitch);
					errors[CS_MATRIX_HAS_ROWS_LESS_THAN_ONE] = true;
					break;
				}
			}
		}
	}

	/**
	 * It checks if priority scheduling is used but all the classes have the
	 * same priorities.
	 */
	private void checkForSchedulingSamePriorities() {
		Vector<Object> classes = class_def.getClassKeys();
		if (classes.size() <= 1) {
			return;
		}
		for (int i = 1; i < classes.size(); i++) {
			Object lastClass = classes.get(i - 1);
			Object thisClass = classes.get(i);
			Integer lastPriority = class_def.getClassPriority(lastClass);
			Integer thisPriority = class_def.getClassPriority(thisClass);
			if (!thisPriority.equals(lastPriority)) {
				return;
			}
		}

		Vector<Object> servers = station_def.getStationKeysServer();
		for (int i = 0; i < servers.size(); i++) {
			Object thisServer = servers.get(i);
			String stationStrategy = station_def.getStationQueueStrategy(thisServer);
			if (stationStrategy.equals(CommonConstants.QUEUE_STRATEGY_STATION_QUEUE_PRIORITY)) {
				warnings[SCHEDULING_SAME_PRIORITIES] = true;
				break;
			}
		}
	}

	/**
	 * Search for all the stations of a given type between a fork and a join.
	 * @param type: the type of the stations to be searched for.
	 * @param start: the fork at which the search begins.
	 * @return all the stations of the given type found between the fork and
	 * the join, or null if source is not a fork.
	 */
	private Vector<Object> searchForStationBetweenForkJoin(String type, Object start) {
		if (!station_def.getStationType(start).equals(STATION_TYPE_FORK)) {
			return null;
		}
		Vector<Object> found = new Vector<Object>(0, 1);
		Map<Object, Boolean> reachable = new HashMap<Object, Boolean>(0, 1);
		searchForStationBetweenForkJoinAux(type, start, start, 0, reachable, found);
		return found;
	}

	private void searchForStationBetweenForkJoinAux(String type, Object start, Object source,
			int nest, Map<Object, Boolean> reachable, Vector<Object> found) {
		Vector<Object> forwardStations = station_def.getForwardConnections(source);
		if (station_def.getStationType(source).equals(STATION_TYPE_FORK)) {
			nest++;
		}
		if (station_def.getStationType(source).equals(STATION_TYPE_JOIN)) {
			nest--;
		}
		reachable.put(source, false);
		for (Object next : forwardStations) {
			if (next.equals(start)) {
				continue;
			}
			if (station_def.getStationType(next).equals(STATION_TYPE_JOIN)) {
				reachable.put(source, true);
				if (nest == 1) {
					continue;
				}
			}
			if (!reachable.containsKey(next)) {
				searchForStationBetweenForkJoinAux(type, start, next, nest, reachable, found);
			}
			// The reachable state may be missed when the next station has not been finished.
			if (reachable.get(next)) {
				reachable.put(source, true);
			}
		}
		if (station_def.getStationType(source).equals(type) && reachable.get(source)) {
			found.add(source);
		}
		return;
	}

	/**
	 * It checks if there are any semaphores not between a fork and a join.
	 */
	private void checkForSemaphoreNotBetweenForkJoin() {
		Vector<Object> semaphores = station_def.getStationKeysSemaphore();
		Vector<Object> forks = station_def.getStationKeysFork();
		semaphoreNotBetweenForkJoin.addAll(semaphores);
		for (int i = 0; i < forks.size(); i++) {
			Object thisFork = forks.get(i);
			Vector<Object> result = searchForStationBetweenForkJoin(STATION_TYPE_SEMAPHORE, thisFork);
			if (result != null) {
				semaphoreNotBetweenForkJoin.removeAll(result);
			}
		}
		if (!semaphoreNotBetweenForkJoin.isEmpty()) {
			errors[SEMAPHORE_NOT_BETWEEN_FORK_JOIN] = true;
		}
	}

	/**
	 * It checks if there are any scalers not between a fork and a join.
	 */
	private void checkForScalerNotBetweenForkJoin() {
		Vector<Object> scalers = station_def.getStationKeysScaler();
		Vector<Object> forks = station_def.getStationKeysFork();
		scalerNotBetweenForkJoin.addAll(scalers);
		for (int i = 0; i < forks.size(); i++) {
			Object thisFork = forks.get(i);
			Vector<Object> result = searchForStationBetweenForkJoin(STATION_TYPE_SCALER, thisFork);
			if (result != null) {
				scalerNotBetweenForkJoin.removeAll(result);
			}
		}
		if (!scalerNotBetweenForkJoin.isEmpty()) {
			errors[SCALER_NOT_BETWEEN_FORK_JOIN] = true;
		}
	}

	/**
	 * It checks if there are any transitions between a fork and a join.
	 */
	private void checkForTransitionBetweenForkJoin() {
		Vector<Object> forks = station_def.getStationKeysFork();
		for (int i = 0; i < forks.size(); i++) {
			Object thisFork = forks.get(i);
			Vector<Object> result = searchForStationBetweenForkJoin(STATION_TYPE_TRANSITION, thisFork);
			if (result != null) {
				result.removeAll(transitionBetweenForkJoin);
				transitionBetweenForkJoin.addAll(result);
			}
		}
		if (!transitionBetweenForkJoin.isEmpty()) {
			warnings[TRANSITION_BETWEEN_FORK_JOIN] = true;
		}
	}

	/**
	 * It checks if there are any transitions without forward links.
	 */
	private void checkForTransitionWithoutForwardLink() {
		Vector<Object> transitions = station_def.getStationKeysTransition();
		for (int i = 0; i < transitions.size(); i++) {
			Object thisTransition = transitions.get(i);
			Vector<Object> forwardStations = station_def.getForwardConnections(thisTransition);
			if (forwardStations.isEmpty()) {
				transitionWithoutForwardLinks.add(thisTransition);
			}
		}
		if (!transitionWithoutForwardLinks.isEmpty()) {
			warnings[TRANSITION_WITHOUT_FORWARD_LINKS] = true;
		}
	}

	/**
	 * It checks if any transitions have invalid conditions for some modes.
	 */
	private void checkForTransitionWithInvalidConditions() {
		Vector<Object> transitions = station_def.getStationKeysTransition();
		for (int i = 0; i < transitions.size(); i++) {
			Object thisTransition = transitions.get(i);
			Vector<Object> classKeys = class_def.getClassKeys();
			Vector<Object> stationInKeys = station_def.getBackwardConnections(thisTransition);
			if (classKeys.isEmpty() || stationInKeys.isEmpty()) {
				continue;
			}

			int size = station_def.getTransitionModeListSize(thisTransition);
			int count = 0;
			for (int j = 0; j < size; j++) {
				boolean hasNonzeroValues = false;
				boolean hasInvalidValues = false;
				for (Object stationInKey : stationInKeys) {
					for (Object classKey : classKeys) {
						int enablingValue = station_def.getEnablingCondition(thisTransition, j,
								stationInKey, classKey).intValue();
						int inhibitingValue = station_def.getInhibitingCondition(thisTransition, j,
								stationInKey, classKey).intValue();
						if (enablingValue > 0) {
							hasNonzeroValues = true;
						}
						if (inhibitingValue > 0 && enablingValue >= inhibitingValue) {
							hasInvalidValues = true;
						}
					}
				}
				if (!hasNonzeroValues || hasInvalidValues) {
					count++;
				}
			}

			if (count > 0) {
				transitionWithInvalidConditions.add(thisTransition);
			}
		}
		if (!transitionWithInvalidConditions.isEmpty()) {
			errors[TRANSITION_WITH_INVALID_CONDITIONS] = true;
		}
	}

	/**
	 * It checks if any transitions have invalid outcomes for some modes.
	 */
	private void checkForTransitionWithInvalidOutcomes() {
		Vector<Object> transitions = station_def.getStationKeysTransition();
		for (int i = 0; i < transitions.size(); i++) {
			Object thisTransition = transitions.get(i);
			Vector<Object> classKeys = class_def.getClassKeys();
			Vector<Object> stationOutKeys = station_def.getForwardConnections(thisTransition);
			if (classKeys.isEmpty() || stationOutKeys.isEmpty()) {
				continue;
			}

			int size = station_def.getTransitionModeListSize(thisTransition);
			int count = 0;
			for (int j = 0; j < size; j++) {
				boolean hasNonzeroValues = false;
				for (Object stationOutKey : stationOutKeys) {
					for (Object classKey : classKeys) {
						int firingValue = station_def.getFiringOutcome(thisTransition, j,
								stationOutKey, classKey).intValue();
						if (firingValue > 0) {
							hasNonzeroValues = true;
						}
					}
				}
				if (!hasNonzeroValues) {
					count++;
				}
			}

			if (count > 0) {
				transitionWithInvalidOutcomes.add(thisTransition);
			}
		}
		if (!transitionWithInvalidOutcomes.isEmpty()) {
			warnings[TRANSITION_WITH_INVALID_OUTCOMES] = true;
		}
	}

}
