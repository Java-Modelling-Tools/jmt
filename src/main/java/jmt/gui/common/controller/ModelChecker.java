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

//Flag

package jmt.gui.common.controller;

import java.util.*;

import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceType;
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
import jmt.gui.common.routingStrategies.RoutingStrategy;
import jmt.gui.common.serviceStrategies.LDStrategy;

//TODO: investigate delete or not
import jmt.gui.common.routingStrategies.LoadDependentRouting;
import jmt.gui.common.routingStrategies.ProbabilityRouting;

/**
 * <p>
 * Title: ModelChecker</p>
 * <p>
 * Description: Checks the model correctness</p>
 *
 * @author Francesco D'Aquino Date: 6-ott-2005 Time: 16.12.50
 *
 * @author Bertoli Marco (new errors)
 */
public class ModelChecker implements CommonConstants {

	private ClassDefinition class_def;
	private StationDefinition station_def;
	private SimulationDefinition simulation_def;
	private BlockingRegionDefinition blocking_def;

	//sets the behavior of the checker
	private boolean isToJMVAChecker;

	//Variable that contains information about errors
	private boolean[] errors;
	//Variable that contains information about warnings
	private boolean[] warnings;

	//Vector containing the keys of sources without an associated open class
	private Vector<Object> sourcesWithoutOpenClass;
	//Vector containing the keys of classes without a reference station
	private Vector<Object> classesWithoutReferenceStation;
	//Vector containing the keys of stations without an essential link
	private Vector<Object> stationsWithoutEssentialLink;
	//HashMap containing the vector of keys of stations that may route a closed
	//class to sinks. The key of the HashMap is the key of the closed class
	//considered.
	private HashMap<Object, Vector<Object>> closedClassesWithRoutingError;
	//Vector containing the keys of measures defined more than once
	private Vector<Object> redundantMeasures;
	//Vector containing the keys of stations using different queue strategies
	//for classes within the same priority group
	private Vector<Object> stationsWithInconsistentQueueStrategies;
	//Vector containing the keys of servers using SJF, LJF, SEPT or LEPT along
	//with the load dependent service strategy
	private Vector<Object> serversWithUnpredictableService;
	//Vector containing the keys of blocking regions without a station
	private Vector<Object> emptyBlockingRegions;
	//Vector containing the keys of blocking regions in which the total preload
	//exceeds the capacity constraints
	private Vector<Object> blockingRegionsWithCapacityOverload;
	//Vector containing the keys of blocking regions in which the total preload
	//exceeds the memory constraints
	private Vector<Object> blockingRegionsWithMemoryOverload;
	//Vector containing the keys of class switches with a matrix in which some
	//rows sum to less than one
	private Vector<Object> classSwitchesWithInvalidMatrix;
	//Vector containing the keys of class switches followed by a queue with the
	//BAS strategy
	private Vector<Object> classSwitchesFollowedByBasStrategy;
	//HashMap containing the vector of keys of classes for which this station
	//uses a Guard strategy with zero tasks. The key of the HashMap is the key
	//of the station considered.
	private HashMap<Object, Vector<Object>> stationsWithZeroGuardStrategy;
	//Vector containing the keys of semaphores not located between fork/join
	private Vector<Object> semaphoresNotBetweenForkJoin;
	//Vector containing the keys of scalers not located between fork/join
	private Vector<Object> scalersNotBetweenForkJoin;
	//Vector containing the keys of transitions with an infinite enabling
	//degree for a mode
	private Vector<Object> transitionsWithInfiniteEnablingDegree;

	//Vector containing the keys of servers with different queue strategies
	private Vector<Object> bcmpServersWithDifferentQueueStrategies;
	//Vector containing the keys of FCFS servers with different service
	//strategies
	//Used only in JMVA conversion
	private Vector<Object> bcmpFcfsServersWithDifferentServiceStrategies;
	//Vector containing the keys of FCFS servers with a service time
	//distribution that is not exponential
	//Used only in JMVA conversion
	private Vector<Object> bcmpFcfsServersWithNonExponentialDistribution;
	//Vector containing the keys of FCFS servers with different mean service
	//times
	//Used only in JMVA conversion
	private Vector<Object> bcmpFcfsServersWithDifferentServiceTimes;
	//Vector containing the keys of delays with a service time distribution
	//for which the Laplace transform is not rational
	//Used only in JMVA conversion
	private Vector<Object> bcmpDelaysWithNonRationalDistribution;
	//Vector containing the keys of stations with a routing strategy dependent
	//on the state of the model
	//Used only in JMVA conversion
	private Vector<Object> bcmpStationsWithStateDependentRoutingStrategy;
	//Vector containing the keys of servers with a scheduling policy other than
	//PS, FCFS or RAND
	//Used only in JMVA conversion
	private Vector<Object> bcmpServersWithAsymmetricSchedulingPolicy;

	//Vector containing the keys of stations without an optional link
	private Vector<Object> stationsWithoutOptionalLink;
	//Vector containing the keys of closed classes with zero population
	private Vector<Object> closedClassesWithZeroPopulation;
	//Vector containing the keys of class switches located between fork/join
	private Vector<Object> classSwitchesBetweenForkJoin;
	//Vector containing the keys of stations using priority scheduling when all
	//the classes have the same priority
	private Vector<Object> stationsWithPriorityScheduling;
	//Vector containing the keys of transitions located between fork/join
	private Vector<Object> transitionsBetweenForkJoin;
	//Vector containing the keys of transitions with a constant enabling degree
	//for a mode
	private Vector<Object> transitionsWithConstantEnablingDegree;
	//Vector containing the keys of transitions with invalid input condition
	//for a mode
	private Vector<Object> transitionsWithInvalidInputCondition;
	//Vector containing the keys of transitions without firing outcome for a
	//mode
	private Vector<Object> transitionsWithoutFiringOutcome;

	//TODO rose: check whether needed or not
	//Vector containing the keys of transitions with invalid firing outcome for
	//a mode
	private Vector<Object> transitionsWithInvalidOutcome;
	// Vector containing the keys of stations with impatience selected and which are connected to a fork
	private Vector<Object> stationsWithImpatienceAndConnectedToFork;

	//constant used to define an error
	public static final int ERROR_PROBLEM = 0;
	//constant used to define a warning
	public static final int WARNING_PROBLEM = 1;

	//it occurs when no classes have been defined.
	public static final int NO_CLASS_ERROR = 0;
	//it occurs when no stations have been defined.
	public static final int NO_STATION_ERROR = 1;
	//it occurs when no measures have been defined.
	public static final int NO_MEASURE_ERROR = 2;
	//it occurs when there is an open class but no sources or transitions have
	//been defined.
	public static final int OPEN_CLASS_BUT_NO_SOURCE_ERROR = 3;
	//it occurs when there is an open class but no sinks or transitions have
	//been defined.
	public static final int OPEN_CLASS_BUT_NO_SINK_ERROR = 4;
	//it occurs when a source has no associated open classes.
	public static final int SOURCE_WITHOUT_OPEN_CLASS_ERROR = 5;
	//it occurs when there is a sink but no open classes have been defined.
	public static final int SINK_BUT_NO_OPEN_CLASS_ERROR = 6;
	//it occurs when a class has no reference station.
	public static final int NO_REFERENCE_STATION_ERROR = 7;
	//it occurs when a station has no essential links.
	public static final int NO_ESSENTIAL_LINK_ERROR = 8;
	//it occurs when a measure has one or more 'null' field.
	public static final int INVALID_MEASURE_ERROR = 9;
	//it occurs when the same measure is defined more than once.
	public static final int DUPLICATE_MEASURE_ERROR = 10;
	//it occurs when a station uses different queue strategies for classes
	//within the same priority group.
	public static final int INCONSISTENT_QUEUE_STRATEGY_ERROR = 11;
	//it occurs when a server uses SJF, LJF, SEPT or LEPT along with the load
	//dependent service strategy.
	public static final int UNPREDICTABLE_SERVICE_ERROR = 12;
	//it occurs when there is a join but no forks.
	public static final int JOIN_WITHOUT_FORK_ERROR = 13;
	//it occurs when tasks split from a job may be routed to different joins.
	public static final int FORK_JOIN_ROUTING_ERROR = 14;
	//it occurs when a blocking region has no stations.
	public static final int EMPTY_BLOCKING_REGION_ERROR = 15;
	//it occurs when the total preload in a blocking region exceeds the
	//capacity constraints.
	public static final int BLOCKING_REGION_CAPACITY_OVERLOAD_ERROR = 16;
	//it occurs when the total preload in a blocking region exceeds the memory
	//constraints.
	public static final int BLOCKING_REGION_MEMORY_OVERLOAD_ERROR = 17;
	//it occurs when a class switch has a matrix in which some rows sum to less
	//than one.
	public static final int CLASS_SWITCH_INVALID_MATRIX_ERROR = 18;
	//it occurs when a class switch is in the model and closed classes have
	//different reference stations.
	public static final int CLASS_SWITCH_REFERENCE_STATION_ERROR = 19;
	//it occurs when a station uses a Guard strategy with zero tasks for a
	//class.
	public static final int ZERO_GUARD_STRATEGY_ERROR = 20;
	//it occurs when a semaphore is not located between fork/join.
	public static final int SEMAPHORE_NOT_BETWEEN_FORK_JOIN_ERROR = 21;
	//it occurs when a scaler is not located between fork/join.
	public static final int SCALER_NOT_BETWEEN_FORK_JOIN_ERROR = 22;
	//it occurs when a transition has an infinite enabling degree for a mode.
	public static final int TRANSITION_INFINITE_ENABLING_DEGREE_ERROR = 23;

	//TODO rose: added test
	//it occurs when there is an impatience strategy selected at a station connected to a fork.
	public static final int IMPATIENCE_ENABLED_IN_STATION_BETWEEN_FORK_JOIN_ERROR = 24;
	//it occurs when a server has different queue strategies.
	// (only when trying to convert to JMVA)
	public static final int BCMP_DIFFERENT_QUEUE_STRATEGIES_WARNING = 0;
	//it occurs when an FCFS server has different service strategies.
	// (only when trying to convert to JMVA)
	public static final int BCMP_FCFS_DIFFERENT_SERVICE_STRATEGIES_WARNING = 1;
	//it occurs when an FCFS server has a service time distribution that is not
	//exponential.
	// (only when trying to convert to JMVA)
	public static final int BCMP_FCFS_NON_EXPONENTIAL_DISTRIBUTION_WARNING = 2;
	//it occurs when an FCFS server has different mean service times.
	// (only when trying to convert to JMVA)
	public static final int BCMP_FCFS_DIFFERENT_SERVICE_TIMES_WARNING = 3;
	//it occurs when a PS server has a service time distribution for which the
	//Laplace rational is not rational.
	// (only when trying to convert to JMVA)
	public static final int BCMP_PS_NON_RATIONAL_DISTRIBUTION_WARNING = 4;
	//it occurs when an LCFS-PR server has a service time distribution for
	//which the Laplace transform is not rational.
	// (only when trying to convert to JMVA)
	public static final int BCMP_LCFS_PR_NON_RATIONAL_DISTRIBUTION_WARNING = 5;
	//it occurs when a delay has a service time distribution for which the
	//Laplace transform is not rational.
	// (only when trying to convert to JMVA)
	public static final int BCMP_DELAY_NON_RATIONAL_DISTRIBUTION_WARNING = 6;
	//it occurs when a station has a routing strategy dependent on the state of
	//the model.
	// (only when trying to convert to JMVA)
	public static final int BCMP_STATE_DEPENDENT_ROUTING_STRATEGY_WARNING = 7;
	//it occurs when a server has a scheduling policy other than PS, FCFS or
	//RAND.
	// (only when trying to convert to JMVA)
	public static final int BCMP_ASYMMETRIC_SCHEDULING_POLICY_WARNING = 8;

	//it occurs when a station has no optional links.
	public static final int NO_OPTIONAL_LINK_WARNING = 9;
	//it occurs when there is a fork but no joins.
	public static final int FORK_WITHOUT_JOIN_WARNING = 10;
	//it occurs when the parametric analysis model becomes inconsistent with
	//the simulation model.
	public static final int PARAMETRIC_ANALYSIS_MODEL_MODIFIED_WARNING = 11;
	//it occurs when a parametric analysis model was defined but no parametric
	//analysis is available.
	public static final int PARAMETRIC_ANALYSIS_NOT_AVAILABLE_WARNING = 12;
	//it occurs when the population of a closed class is zero.
	public static final int ZERO_POPULATION_WARNING = 13;
	//it occurs when the total population of closed classes is zero.
	public static final int ZERO_TOTAL_POPULATION_WARNING = 14;
	//it occurs when a class switch is located between fork/join.
	public static final int CLASS_SWITCH_BETWEEN_FORK_JOIN_WARNING = 15;
	//it occurs when a station uses priority scheduling but all the classes
	//have the same priority.
	public static final int SCHEDULING_SAME_PRIORITY_WARNING = 16;
	//it occurs when a transition is located between fork/join.
	public static final int TRANSITION_BETWEEN_FORK_JOIN_WARNING = 17;
	//it occurs when a transition has a constant enabling degree for a mode.
	public static final int TRANSITION_CONSTANT_ENABLING_DEGREE_WARNING = 18;
	//it occurs when a transition has invalid input condition for a mode.
	public static final int TRANSITION_INVALID_INPUT_CONDITION_WARNING = 19;
	//it occurs when a transition has no firing outcome for a mode.
	public static final int TRANSITION_NO_FIRING_OUTCOME_WARNING = 20;

	private static final int NUMBER_OF_ERROR_TYPES = 25;
	private static final int NUMBER_OF_WARNING_TYPES = 21;

	/**
	 * Creates a new modelchecker
	 *
	 * @param class_def reference to class definition data structure
	 * @param station_def reference to station definition data structure
	 * @param simulation_def reference to simulation definition data structure
	 * @param brd reference to blocking region definition data structure
	 * @param isToJMVA true if model must be converted to jmva, false otherwise
	 */
	public ModelChecker(ClassDefinition class_def, StationDefinition station_def, SimulationDefinition simulation_def, BlockingRegionDefinition brd,
			boolean isToJMVA) {
		this.class_def = class_def;
		this.station_def = station_def;
		this.simulation_def = simulation_def;
		this.blocking_def = brd;
		errors = new boolean[NUMBER_OF_ERROR_TYPES];
		warnings = new boolean[NUMBER_OF_WARNING_TYPES];
		sourcesWithoutOpenClass = new Vector<Object>(0, 1);
		classesWithoutReferenceStation = new Vector<Object>(0, 1);
		stationsWithoutEssentialLink = new Vector<Object>(0, 1);
		redundantMeasures = new Vector<Object>(0, 1);
		stationsWithInconsistentQueueStrategies = new Vector<Object>(0, 1);
		serversWithUnpredictableService = new Vector<Object>(0, 1);
		emptyBlockingRegions = new Vector<Object>(0, 1);
		blockingRegionsWithCapacityOverload = new Vector<Object>(0, 1);
		blockingRegionsWithMemoryOverload = new Vector<Object>(0, 1);
		classSwitchesWithInvalidMatrix = new Vector<Object>(0, 1);
		stationsWithZeroGuardStrategy = new HashMap<Object, Vector<Object>>(0, 1);
		semaphoresNotBetweenForkJoin = new Vector<Object>(0, 1);
		scalersNotBetweenForkJoin = new Vector<Object>(0, 1);
		transitionsWithInfiniteEnablingDegree = new Vector<Object>(0, 1);
		bcmpServersWithDifferentQueueStrategies = new Vector<Object>(0, 1);
		bcmpFcfsServersWithDifferentServiceStrategies = new Vector<Object>(0, 1);
		bcmpFcfsServersWithNonExponentialDistribution = new Vector<Object>(0, 1);
		bcmpFcfsServersWithDifferentServiceTimes = new Vector<Object>(0, 1);
		bcmpDelaysWithNonRationalDistribution = new Vector<Object>(0, 1);
		bcmpStationsWithStateDependentRoutingStrategy = new Vector<Object>(0, 1);
		bcmpServersWithAsymmetricSchedulingPolicy = new Vector<Object>(0, 1);
		stationsWithoutOptionalLink = new Vector<Object>(0, 1);
		closedClassesWithZeroPopulation = new Vector<Object>(0, 1);
		classSwitchesBetweenForkJoin = new Vector<Object>(0, 1);
		stationsWithPriorityScheduling = new Vector<Object>(0, 1);
		transitionsBetweenForkJoin = new Vector<Object>(0, 1);
		transitionsWithConstantEnablingDegree = new Vector<Object>(0, 1);
		transitionsWithInvalidInputCondition = new Vector<Object>(0, 1);
		transitionsWithoutFiringOutcome = new Vector<Object>(0, 1);
		stationsWithImpatienceAndConnectedToFork = new Vector<>(0, 1);
		isToJMVAChecker = isToJMVA;
		checkModel();
	}

	//resets all the variables of the model checker
	private void reset() {
		sourcesWithoutOpenClass.clear();
		classesWithoutReferenceStation.clear();
		stationsWithoutEssentialLink.clear();
		redundantMeasures.clear();
		stationsWithInconsistentQueueStrategies.clear();
		serversWithUnpredictableService.clear();
		emptyBlockingRegions.clear();
		blockingRegionsWithCapacityOverload.clear();
		blockingRegionsWithMemoryOverload.clear();
		classSwitchesWithInvalidMatrix.clear();
		stationsWithZeroGuardStrategy.clear();
		semaphoresNotBetweenForkJoin.clear();
		scalersNotBetweenForkJoin.clear();
		transitionsWithInfiniteEnablingDegree.clear();
		bcmpServersWithDifferentQueueStrategies.clear();
		bcmpFcfsServersWithDifferentServiceStrategies.clear();
		bcmpFcfsServersWithNonExponentialDistribution.clear();
		bcmpFcfsServersWithDifferentServiceTimes.clear();
		bcmpDelaysWithNonRationalDistribution.clear();
		bcmpStationsWithStateDependentRoutingStrategy.clear();
		bcmpServersWithAsymmetricSchedulingPolicy.clear();
		stationsWithoutOptionalLink.clear();
		closedClassesWithZeroPopulation.clear();
		classSwitchesBetweenForkJoin.clear();
		stationsWithPriorityScheduling.clear();
		transitionsBetweenForkJoin.clear();
		transitionsWithConstantEnablingDegree.clear();
		transitionsWithInvalidInputCondition.clear();
		transitionsWithoutFiringOutcome.clear();
		stationsWithImpatienceAndConnectedToFork.clear();
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
			checkForOpenClassButNoSourceError();
			checkForNoReferenceStationError();
			checkForBcmpDifferentQueueStrategiesWarning();
			checkForBcmpFcfsDifferentServiceStrategiesWarning();
			checkForBcmpFcfsNonExponentialDistributionWarning();
			checkForBcmpFcfsDifferentServiceTimesWarning();
			checkForBcmpPsNonRationalDistributionWarning();
			checkForBcmpLcfsPrNonRationalDistributionWarning();
			checkForBcmpDelayNonRationalDistributionWarning();
			checkForBcmpStateDependentRoutingStrategyWarning();
			checkForBcmpAsymmetricSchedulingPolicyWarning();
		} else {
			station_def.manageProbabilities();
			checkForNoClassError();
			checkForNoStationError();
			checkForNoMeasureError();
			checkForOpenClassButNoSourceError();
			checkForOpenClassButNoSinkError();
			checkForSourceWithoutOpenClassError();
			checkForSinkButNoOpenClassError();
			checkForNoReferenceStationError();
			checkForNoEssentialLinkError();
			checkForInvalidMeasureError();
			checkForRedundantMeasureError();
			checkForInconsistentQueueStrategyError();
			checkForUnpredictableServiceError();
			checkForJoinWithoutForkError();
			checkForForkJoinRoutingError();
			checkForEmptyBlockingRegionError();
			checkForBlockingRegionCapacityOverloadError();
			checkForBlockingRegionMemoryOverloadError();
			checkForClassSwitchInvalidMatrixError();
			checkForClassSwitchReferenceStationError();
			checkForZeroGuardStrategyError();
			checkForSemaphoreNotBetweenForkJoinError();
			checkForScalerNotBetweenForkJoinError();
			checkForTransitionInfiniteEnablingDegreeError();
			checkForNoOptionalLinkWarning();
			checkForForkWithoutJoinWarning();
			checkForParametricAnalysisModelModifiedWarning();
			checkForParametricAnalysisNotAvailableWarning();
			checkForZeroPopulationWarning();
			checkForZeroTotalPopulationWarning();
			checkForClassSwitchBetweenForkJoinWarning();
			checkForSchedulingSamePriorityWarning();
			checkForTransitionBetweenForkJoinWarning();
			checkForTransitionConstantEnablingDegreeWarning();
			checkForTransitionInvalidInputConditionWarning();
			checkForTransitionNoFiringOutcomeWarning();
			checkForImpatienceEnabledInStationBetweenForkJoinError();

		}
	}

	public ClassDefinition getClassModel() {
		return class_def;
	}

	public StationDefinition getStationModel() {
		return station_def;
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
		for (int i = 0; i < NUMBER_OF_ERROR_TYPES; i++) {
			if (errors[i]) {
				ok = false;
			}
		}
		return ok;
	}

	public boolean isWarningFreeNormal() {
		boolean ok = true;
		for (int i = 9; i < NUMBER_OF_WARNING_TYPES; i++) {
			if (warnings[i]) {
				ok = false;
			}
		}
		return ok;
	}

	public boolean isErrorFreeToJMVA() {
		boolean ok = true;
		if (errors[NO_CLASS_ERROR] || errors[NO_STATION_ERROR] || errors[OPEN_CLASS_BUT_NO_SOURCE_ERROR]
				|| errors[NO_REFERENCE_STATION_ERROR]) {
			ok = false;
		}
		return ok;
	}

	public boolean isWarningFreeToJMVA() {
		boolean ok = true;
		if (warnings[BCMP_DIFFERENT_QUEUE_STRATEGIES_WARNING] || warnings[BCMP_FCFS_DIFFERENT_SERVICE_STRATEGIES_WARNING]
				|| warnings[BCMP_FCFS_NON_EXPONENTIAL_DISTRIBUTION_WARNING] || warnings[BCMP_FCFS_DIFFERENT_SERVICE_TIMES_WARNING]
				|| warnings[BCMP_PS_NON_RATIONAL_DISTRIBUTION_WARNING] || warnings[BCMP_LCFS_PR_NON_RATIONAL_DISTRIBUTION_WARNING]
				|| warnings[BCMP_DELAY_NON_RATIONAL_DISTRIBUTION_WARNING] || warnings[BCMP_STATE_DEPENDENT_ROUTING_STRATEGY_WARNING]
				|| warnings[BCMP_ASYMMETRIC_SCHEDULING_POLICY_WARNING]) {
			ok = false;
		}
		return ok;
	}

	/**
	 * Returns true if no classes have been defined.
	 *
	 * @return true if no classes have been defined.
	 */
	public boolean isThereNoClassError() {
		return errors[NO_CLASS_ERROR];
	}

	/**
	 * Returns true if no stations have been defined.
	 *
	 * @return true if no stations have been defined.
	 */
	public boolean isThereNoStationError() {
		return errors[NO_STATION_ERROR];
	}

	/**
	 * Returns true if no measures have been defined.
	 *
	 * @return true if no measures have been defined.
	 */
	public boolean isThereNoMeasureError() {
		return errors[NO_MEASURE_ERROR];
	}

	/**
	 * Returns true if there is an open class but no sources or transitions
	 * have been defined.
	 *
	 * @return true if there is an open class but no sources or transitions
	 * have been defined.
	 */
	public boolean isThereOpenClassButNoSourceError() {
		return errors[OPEN_CLASS_BUT_NO_SOURCE_ERROR];
	}

	/**
	 * Returns true if there is an open class but no sinks or transitions have
	 * been defined.
	 *
	 * @return true if there is an open class but no sinks or transitions have
	 * been defined.
	 */
	public boolean isThereOpenClassButNoSinkError() {
		return errors[OPEN_CLASS_BUT_NO_SINK_ERROR];
	}

	/**
	 * Returns true if a source has no associated open classes.
	 *
	 * @return true if a source has no associated open classes.
	 */
	public boolean isThereSourceWithoutOpenClassError() {
		return errors[SOURCE_WITHOUT_OPEN_CLASS_ERROR];
	}

	/**
	 * Returns true if there is a sink but no open classes have been defined.
	 *
	 * @return true if there is a sink but no open classes have been defined.
	 */
	public boolean isThereSinkButNoOpenClassError() {
		return errors[SINK_BUT_NO_OPEN_CLASS_ERROR];
	}

	/**
	 * Returns true if a class has no reference station.
	 *
	 * @return true if a class has no reference station.
	 */
	public boolean isThereNoReferenceStationError() {
		return errors[NO_REFERENCE_STATION_ERROR];
	}

	/**
	 * Returns true if a station has no essential links.
	 *
	 * @return true if a station has no essential links.
	 */
	public boolean isThereNoEssentialLinkError() {
		return errors[NO_ESSENTIAL_LINK_ERROR];
	}

	/**
	 * Returns true if a measure has one or more 'null' field.
	 *
	 * @return true if a measure has one or more 'null' field.
	 */
	public boolean isThereInvalidMeasureError() {
		return errors[INVALID_MEASURE_ERROR];
	}

	/**
	 * Returns true if the same measure is defined more than once.
	 *
	 * @return true if the same measure is defined more than once.
	 */
	public boolean isThereRedundantMeasureError() {
		return errors[DUPLICATE_MEASURE_ERROR];
	}

	/**
	 * Returns true if a station uses different queue strategies for classes
	 * within the same priority group.
	 *
	 * @return true if a station uses different queue strategies for classes
	 * within the same priority group.
	 */
	public boolean isThereInconsistentQueueStrategyError() {
		return errors[INCONSISTENT_QUEUE_STRATEGY_ERROR];
	}

	/**
	 * Returns true if a server uses SJF, LJF, SEPT or LEPT along with the load
	 * dependent service strategy.
	 *
	 * @return true if a server uses SJF, LJF, SEPT or LEPT along with the load
	 * dependent service strategy.
	 */
	public boolean isThereUnpredictableServiceError() {
		return errors[UNPREDICTABLE_SERVICE_ERROR];
	}

	/**
	 * Returns true if there is a join but no forks.
	 *
	 * @return true if there is a join but no forks.
	 */
	public boolean isThereJoinWithoutForkError() {
		return errors[JOIN_WITHOUT_FORK_ERROR];
	}

	/**
	 * Returns true if tasks split from a job may be routed to different joins.
	 *
	 * @return true if tasks split from a job may be routed to different joins.
	 */
	public boolean isThereForkJoinRoutingError() {
		return errors[FORK_JOIN_ROUTING_ERROR];
	}

	/**
	 * Returns true if a blocking region has no stations.
	 *
	 * @return true if a blocking region has no stations.
	 */
	public boolean isThereEmptyBlockingRegionError() {
		return errors[EMPTY_BLOCKING_REGION_ERROR];
	}

	/**
	 * Returns true if the total preload in a blocking region exceeds the
	 * capacity constraints.
	 *
	 * @return true if the total preload in a blocking region exceeds the
	 * capacity constraints.
	 */
	public boolean isThereBlockingRegionCapacityOverloadError() {
		return errors[BLOCKING_REGION_CAPACITY_OVERLOAD_ERROR];
	}

	/**
	 * Returns true if the total preload in a blocking region exceeds the
	 * memory constraints.
	 *
	 * @return true if the total preload in a blocking region exceeds the
	 * memory constraints.
	 */
	public boolean isThereBlockingRegionMemoryOverloadError() {
		return errors[BLOCKING_REGION_MEMORY_OVERLOAD_ERROR];
	}

	/**
	 * Returns true if a class switch has a matrix in which some rows sum to
	 * less than one.
	 *
	 * @return true if a class switch has a matrix in which some rows sum to
	 * less than one.
	 */
	public boolean isThereClassSwitchInvalidMatrixError() {
		return errors[CLASS_SWITCH_INVALID_MATRIX_ERROR];
	}

	/**
	 * Returns true if a class switch is in the model and closed classes have
	 * different reference stations.
	 *
	 * @return true if a class switch is in the model and closed classes have
	 * different reference stations.
	 */
	public boolean isThereClassSwitchReferenceStationError() {
		return errors[CLASS_SWITCH_REFERENCE_STATION_ERROR];
	}

	/**
	 * Returns true if a station uses a Guard strategy with zero tasks for a
	 * class.
	 *
	 * @return true if a station uses a Guard strategy with zero tasks for a
	 * class.
	 */
	public boolean isThereZeroGuardStrategyError() {
		return errors[ZERO_GUARD_STRATEGY_ERROR];
	}

	/**
	 * Returns true if a semaphore is not located between fork/join.
	 *
	 * @return true if a semaphore is not located between fork/join.
	 */
	public boolean isThereSemaphoreNotBetweenForkJoinError() {
		return errors[SEMAPHORE_NOT_BETWEEN_FORK_JOIN_ERROR];
	}

	/**
	 * Returns true if a scaler is not located between fork/join.
	 *
	 * @return true if a scaler is not located between fork/join.
	 */
	public boolean isThereScalerNotBetweenForkJoinError() {
		return errors[SCALER_NOT_BETWEEN_FORK_JOIN_ERROR];
	}

	/**
	 * Returns true if a transition has an infinite enabling degree for a mode.
	 *
	 * @return true if a transition has an infinite enabling degree for a mode.
	 */
	public boolean isThereTransitionInfiniteEnablingDegreeError() {
		return errors[TRANSITION_INFINITE_ENABLING_DEGREE_ERROR];
	}


	//TODO emergent: add error
	/**
	 * @return true if a station connected to a fork has an impatience strategy enabled.
	 */
	public boolean isThereImpatienceInStationBetweenForkJoinError() {
		return errors[IMPATIENCE_ENABLED_IN_STATION_BETWEEN_FORK_JOIN_ERROR];
	}


	/**
	 * Returns true if a server has different queue strategies.
	 *
	 * @return true if a server has different queue strategies.
	 */
	public boolean isThereBcmpDifferentQueueStrategiesWarning() {
		return warnings[BCMP_DIFFERENT_QUEUE_STRATEGIES_WARNING];
	}

	/**
	 * Returns true if an FCFS server has different service strategies.
	 *
	 * @return true if an FCFS server has different service strategies.
	 */
	public boolean isThereBcmpDifferentServiceStrategiesWarning() {
		return warnings[BCMP_FCFS_DIFFERENT_SERVICE_STRATEGIES_WARNING];
	}

	/**
	 * Returns true if an FCFS server has a service time distribution that is
	 * not exponential.
	 *
	 * @return true if an FCFS server has a service time distribution that is
	 * not exponential.
	 */
	public boolean isThereBcmpFcfsNonExponentialDistributionWarning() {
		return warnings[BCMP_FCFS_NON_EXPONENTIAL_DISTRIBUTION_WARNING];
	}

	/**
	 * Returns true if an FCFS server has different mean service times.
	 *
	 * @return true if an FCFS server has different mean service times.
	 */
	public boolean isThereBcmpFcfsDifferentServiceTimesWarning() {
		return warnings[BCMP_FCFS_DIFFERENT_SERVICE_TIMES_WARNING];
	}

	/**
	 * Returns true if a PS server has a service time distribution for which
	 * the Laplace transform is not rational.
	 *
	 * @return true if a PS server has a service time distribution for which
	 * the Laplace transform is not rational.
	 */
	public boolean isThereBcmpPsNonRationalDistributionWarning() {
		return warnings[BCMP_PS_NON_RATIONAL_DISTRIBUTION_WARNING];
	}

	/**
	 * Returns true if an LCFS-PR server has a service time distribution for
	 * which the Laplace transform is not rational.
	 *
	 * @return true if an LCFS-PR server has a service time distribution for
	 * which the Laplace transform is not rational.
	 */
	public boolean isThereBcmpLcfsPrNonRationalDistributionWarning() {
		return warnings[BCMP_LCFS_PR_NON_RATIONAL_DISTRIBUTION_WARNING];
	}

	/**
	 * Returns true if a delay has a service time distribution for which the
	 * Laplace transform is not rational.
	 *
	 * @return true if a delay has a service time distribution for which the
	 * Laplace transform is not rational.
	 */
	public boolean isThereBcmpDelayNonRationalDistributionWarning() {
		return warnings[BCMP_DELAY_NON_RATIONAL_DISTRIBUTION_WARNING];
	}

	/**
	 * Returns true if a station has a routing strategy dependent on the state
	 * of the model.
	 *
	 * @return true if a station has a routing strategy dependent on the state
	 * of the model.
	 */
	public boolean isThereBcmpStateDependentRoutingStrategyWarning() {
		return warnings[BCMP_STATE_DEPENDENT_ROUTING_STRATEGY_WARNING];
	}

	/**
	 * Returns true if a server has a scheduling policy other than PS, FCFS or
	 * RAND.
	 *
	 * @return true if a server has a scheduling policy other than PS, FCFS or
	 * RAND.
	 */
	public boolean isThereBcmpAsymmetricSchedulingPolicyWarning() {
		return warnings[BCMP_ASYMMETRIC_SCHEDULING_POLICY_WARNING];
	}

	/**
	 * Returns true if a station has no optional links.
	 *
	 * @return true if a station has no optional links.
	 */
	public boolean isThereNoOptionalLinkWarning() {
		return warnings[NO_OPTIONAL_LINK_WARNING];
	}

	/**
	 * Returns true if there is a fork but no joins.
	 *
	 * @return true if there is a fork but no joins.
	 */
	public boolean isThereForkWithoutJoinWarning() {
		return warnings[FORK_WITHOUT_JOIN_WARNING];
	}

	/**
	 * Returns true if the parametric analysis model becomes inconsistent with
	 * the simulation model.
	 *
	 * @return true if the parametric analysis model becomes inconsistent with
	 * the simulation model.
	 */
	public boolean isThereParametricAnalysisModelModifiedWarning() {
		return warnings[PARAMETRIC_ANALYSIS_MODEL_MODIFIED_WARNING];
	}

	/**
	 * Returns true if a parametric analysis model was defined but no
	 * parametric analysis is available.
	 *
	 * @return true if a parametric analysis model was defined but no
	 * parametric analysis is available.
	 */
	public boolean isThereParametricAnalysisNotAvailableWarning() {
		return warnings[PARAMETRIC_ANALYSIS_NOT_AVAILABLE_WARNING];
	}

	/**
	 * Returns true if the population of a closed class is zero.
	 *
	 * @return true if the population of a closed class is zero.
	 */
	public boolean isThereZeroPopulationWarning() {
		return warnings[ZERO_POPULATION_WARNING];
	}

	/**
	 * Returns true if the total population of closed classes is zero.
	 *
	 * @return true if the total population of closed classes is zero.
	 */
	public boolean isThereZeroTotalPopulationWarning() {
		return errors[ZERO_TOTAL_POPULATION_WARNING];
	}

	/**
	 * Returns true if a class switch is located between fork/join.
	 *
	 * @return true if a class switch is located between fork/join.
	 */
	public boolean isThereClassSwitchBetweenForkJoinWarning() {
		return warnings[CLASS_SWITCH_BETWEEN_FORK_JOIN_WARNING];
	}

	/**
	 * Returns true if a station uses priority scheduling but all the classes
	 * have the same priority.
	 *
	 * @return true if a station uses priority scheduling but all the classes
	 * have the same priority.
	 */
	public boolean isThereSchedulingSamePriorityWarning() {
		return warnings[SCHEDULING_SAME_PRIORITY_WARNING];
	}

	/**
	 * Returns true if a transition is located between fork/join.
	 *
	 * @return true if a transition is located between fork/join.
	 */
	public boolean isThereTransitionBetweenForkJoinWarning() {
		return warnings[TRANSITION_BETWEEN_FORK_JOIN_WARNING];
	}

	/**
	 * Returns true if a transition has a constant enabling degree for a mode.
	 *
	 * @return true if a transition has a constant enabling degree for a mode.
	 */
	public boolean isThereTransitionConstantEnablingDegreeWarning() {
		return warnings[TRANSITION_CONSTANT_ENABLING_DEGREE_WARNING];
	}

	/**
	 * Returns true if a transition has invalid input condition for a mode.
	 *
	 * @return true if a transition has invalid input condition for a mode.
	 */
	public boolean isThereTransitionInvalidInputConditionWarning() {
		return warnings[TRANSITION_INVALID_INPUT_CONDITION_WARNING];
	}

	/**
	 * Returns true if a transition has no firing outcome for a mode.
	 *
	 * @return true if a transition has no firing outcome for a mode.
	 */
	public boolean isThereTransitionNoFiringOutcomeWarning() {
		return warnings[TRANSITION_NO_FIRING_OUTCOME_WARNING];
	}

	/**
	 * Returns a Vector<Object> containing the keys of sources without an
	 * associated open class.
	 *
	 * @return a Vector<Object> containing the keys of sources without an
	 * associated open class.
	 */
	public Vector<Object> getSourcesWithoutOpenClass() {
		return sourcesWithoutOpenClass;
	}

	/**
	 * Returns a Vector<Object> containing the keys of classes without a
	 * reference station.
	 *
	 * @return a Vector<Object> containing the keys of classes without a
	 * reference station.
	 */
	public Vector<Object> getClassesWithoutReferenceStation() {
		return classesWithoutReferenceStation;
	}

	/**
	 * Returns a Vector<Object> containing the keys of stations without an
	 * essential link.
	 *
	 * @return a Vector<Object> containing the keys of stations without an
	 * essential link.
	 */
	public Vector<Object> getStationsWithoutEssentialLink() {
		return stationsWithoutEssentialLink;
	}

	/**
	 * Returns a Vector<Object> containing the keys of stations using different
	 * queue strategies for classes within the same priority group.
	 *
	 * @return a Vector<Object> containing the keys of stations using different
	 * queue strategies for classes within the same priority group.
	 */
	public Vector<Object> getStationsWithInconsistentQueueStrategies() {
		return stationsWithInconsistentQueueStrategies;
	}

	/**
	 * Returns a Vector<Object> containing the keys of servers using SJF, LJF,
	 * SEPT or LEPT along with the load dependent service strategy.
	 *
	 * @return a Vector<Object> containing the keys of servers using SJF, LJF,
	 * SEPT or LEPT along with the load dependent service strategy.
	 */
	public Vector<Object> getServersWithUnpredictableService() {
		return serversWithUnpredictableService;
	}

	/**
	 * Returns a Vector<Object> containing the keys of blocking regions in
	 * which the total preload exceeds the capacity constraints.
	 *
	 * @return a Vector<Object> containing the keys of blocking regions in
	 * which the total preload exceeds the capacity constraints.
	 */
	public Vector<Object> getBlockingRegionsWithCapacityOverload() {
		return blockingRegionsWithCapacityOverload;
	}

	/**
	 * Returns a Vector<Object> containing the keys of blocking regions in
	 * which the total preload exceeds the memory constraints.
	 *
	 * @return a Vector<Object> containing the keys of blocking regions in
	 * which the total preload exceeds the memory constraints.
	 */
	public Vector<Object> getBlockingRegionsWithMemoryOverload() {
		return blockingRegionsWithMemoryOverload;
	}

	/**
	 * Returns a Vector<Object> containing the keys of class switches with a
	 * matrix in which some rows sum to less than one.
	 *
	 * @return a Vector<Object> containing the keys of class switches with a
	 * matrix in which some rows sum to less than one.
	 */
	public Vector<Object> getClassSwitchesWithInvalidMatrix() {
		return classSwitchesWithInvalidMatrix;
	}

	/**
	 * Returns a HashMap where the key is the key of a station. For each
	 * station that uses a Guard strategy with zero tasks for a class, it
	 * contains a Vector<Object> with the keys of classes for which the problem
	 * occurs.
	 *
	 * @return a HashMap where the key is the key of a station.
	 */
	public HashMap<Object, Vector<Object>> getStationsWithZeroGuardStrategy() {
		return stationsWithZeroGuardStrategy;
	}

	/**
	 * Returns a Vector<Object> containing the keys of semaphores not located
	 * between fork/join.
	 *
	 * @return a Vector<Object> containing the keys of semaphores not located
	 * between fork/join.
	 */
	public Vector<Object> getSemaphoresNotBetweenForkJoin() {
		return semaphoresNotBetweenForkJoin;
	}

	/**
	 * Returns a Vector<Object> containing the keys of scalers not located
	 * between fork/join.
	 *
	 * @return a Vector<Object> containing the keys of scalers not located
	 * between fork/join.
	 */
	public Vector<Object> getScalersNotBetweenForkJoin() {
		return scalersNotBetweenForkJoin;
	}

	/**
	 * Returns a Vector<Object> containing the keys of transitions with an
	 * infinite enabling degree for a mode.
	 *
	 * @return a Vector<Object> containing the keys of transitions with an
	 * infinite enabling degree for a mode.
	 */
	public Vector<Object> getTransitionsWithInfiniteEnablingDegree() {
		return transitionsWithInfiniteEnablingDegree;
	}

	/**
	 * Returns a Vector<Object> containing the keys of servers with different
	 * queue strategies.
	 *
	 * @return a Vector<Object> containing the keys of servers with different
	 * queue strategies.
	 */
	public Vector<Object> getBcmpServersWithDifferentQueueStrategies() {
		return bcmpServersWithDifferentQueueStrategies;
	}

	/**
	 * Returns a Vector<Object> containing the keys of FCFS servers with
	 * different service strategies.
	 *
	 * @return a Vector<Object> containing the keys of FCFS servers with
	 * different service strategies.
	 */
	public Vector<Object> getBcmpFcfsServersWithDifferentServiceStrategies() {
		return bcmpFcfsServersWithDifferentServiceStrategies;
	}

	/**
	 * Returns a Vector<Object> containing the keys of FCFS servers with a
	 * service time distribution that is not exponential.
	 *
	 * @return a Vector<Object> containing the keys of FCFS servers with a
	 * service time distribution that is not exponential.
	 */
	public Vector<Object> getBcmpFcfsServersWithNonExponentialDistribution() {
		return bcmpFcfsServersWithNonExponentialDistribution;
	}

	/**
	 * Returns a Vector<Object> containing the keys of FCFS servers with
	 * different mean service times.
	 *
	 * @return a Vector<Object> containing the keys of FCFS servers with
	 * different mean service times.
	 */
	public Vector<Object> getBcmpFcfsServersWithDifferentServiceTimes() {
		return bcmpFcfsServersWithDifferentServiceTimes;
	}

	/**
	 * Returns a Vector<Object> containing the keys of delays with a service
	 * time distribution for which the Laplace transform is not rational.
	 *
	 * @return a Vector<Object> containing the keys of delays with a service
	 * time distribution for which the Laplace transform is not rational.
	 */
	public Vector<Object> getBcmpDelaysWithNonRationalDistribution() {
		return bcmpDelaysWithNonRationalDistribution;
	}

	/**
	 * Returns a Vector<Object> containing the keys of stations with a routing
	 * strategy dependent on the state of the model.
	 *
	 * @return a Vector<Object> containing the keys of stations with a routing
	 * strategy dependent on the state of the model.
	 */
	public Vector<Object> getBcmpStationsWithStateDependentRoutingStrategy() {
		return bcmpStationsWithStateDependentRoutingStrategy;
	}

	/**
	 * Returns a Vector<Object> containing the keys of servers with a
	 * scheduling policy other than PS, FCFS or RAND.
	 *
	 * @return a Vector<Object> containing the keys of servers with a
	 * scheduling policy other than PS, FCFS or RAND.
	 */
	public Vector<Object> getBcmpServersWithAsymmetricSchedulingPolicy() {
		return bcmpServersWithAsymmetricSchedulingPolicy;
	}

	/**
	 * Returns a Vector<Object> containing the keys of stations without an
	 * optional link.
	 *
	 * @return a Vector<Object> containing the keys of stations without an
	 * optional link.
	 */
	public Vector<Object> getStationsWithoutOptionalLink() {
		return stationsWithoutOptionalLink;
	}

	/**
	 * Returns a Vector<Object> containing the keys of closed classes with zero
	 * population.
	 *
	 * @return a Vector<Object> containing the keys of closed classes with zero
	 * population.
	 */
	public Vector<Object> getClosedClassesWithZeroPopulation() {
		return closedClassesWithZeroPopulation;
	}

	/**
	 * Returns a Vector<Object> containing the keys of class switches between
	 * fork/join.
	 *
	 * @return a Vector<Object> containing the keys of class switches between
	 * fork/join.
	 */
	public Vector<Object> getClassSwitchesBetweenForkJoin() {
		return classSwitchesBetweenForkJoin;
	}

	/**
	 * Returns a Vector<Object> containing the keys of stations using priority
	 * scheduling when all the classes have the same priority.
	 *
	 * @return a Vector<Object> containing the keys of stations using priority
	 * scheduling when all the classes have the same priority.
	 */
	public Vector<Object> getStationsWithPriorityScheduling() {
		return stationsWithPriorityScheduling;
	}

	/**
	 * Returns a Vector<Object> containing the keys of transitions between
	 * fork/join.
	 *
	 * @return a Vector<Object> containing the keys of transitions between
	 * fork/join.
	 */
	public Vector<Object> getTransitionsBetweenForkJoin() {
		return transitionsBetweenForkJoin;
	}

	/**
	 * Returns a Vector<Object> containing the keys of transitions with a
	 * constant enabling degree for a mode.
	 *
	 * @return a Vector<Object> containing the keys of transitions with a
	 * constant enabling degree for a mode.
	 */
	public Vector<Object> getTransitionsWithConstantEnablingDegree() {
		return transitionsWithConstantEnablingDegree;
	}

	/**
	 * Returns a Vector<Object> containing the keys of transitions with invalid
	 * input condition for a mode.
	 *
	 * @return a Vector<Object> containing the keys of transitions with invalid
	 * input condition for a mode.
	 */
	public Vector<Object> getTransitionsWithInvalidInputCondition() {
		return transitionsWithInvalidInputCondition;
	}

	/**
	 * Returns a Vector<Object> containing the keys of transitions without
	 * firing outcome for a mode.
	 *
	 * @return a Vector<Object> containing the keys of transitions without
	 * firing outcome for a mode.
	 */
	public Vector<Object> getTransitionsWithoutFiringOutcome() {
		return transitionsWithoutFiringOutcome;
	}

	//TODO rose: do we need this
	public Vector<Object> getStationsWithImpatienceAndConnectedToFork() {
		return stationsWithImpatienceAndConnectedToFork;
	}

	/**
	 * Checks if no classes have been defined.
	 */
	private void checkForNoClassError() {
		if (class_def.getClassKeys().isEmpty()) {
			errors[NO_CLASS_ERROR] = true;
		}
	}

	/**
	 * Checks if no stations have been defined.
	 */
	private void checkForNoStationError() {
		if (station_def.getStationKeysNoSourceSink().isEmpty()) {
			errors[NO_STATION_ERROR] = true;
		}
	}

	/**
	 * Checks if no measures have been defined.
	 */
	private void checkForNoMeasureError() {
		if (simulation_def.getMeasureKeys().isEmpty()) {
			errors[NO_MEASURE_ERROR] = true;
		}
	}

	/**
	 * Checks if there is an open class but no sources or transitions have been
	 * defined.
	 */
	private void checkForOpenClassButNoSourceError() {
		Vector<Object> openClasses = class_def.getOpenClassKeys();
		Vector<Object> sources = station_def.getStationKeysSource();
		Vector<Object> transitions = station_def.getStationKeysTransition();
		if (!openClasses.isEmpty() && (sources.isEmpty() && transitions.isEmpty())) {
			errors[OPEN_CLASS_BUT_NO_SOURCE_ERROR] = true;
		}
	}

	/**
	 * Checks if there is an open class but no sinks or transitions have been
	 * defined.
	 */
	private void checkForOpenClassButNoSinkError() {
		Vector<Object> openClasses = class_def.getOpenClassKeys();
		Vector<Object> sinks = station_def.getStationKeysSink();
		Vector<Object> transitions = station_def.getStationKeysTransition();
		if (!openClasses.isEmpty() && (sinks.isEmpty() && transitions.isEmpty())) {
			errors[OPEN_CLASS_BUT_NO_SINK_ERROR] = true;
		}
	}

	/**
	 * Checks if a source has no associated open classes.
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
				sourcesWithoutOpenClass.add(thisSource);
				errors[SOURCE_WITHOUT_OPEN_CLASS_ERROR] = true;
			}
		}
	}

	/**
	 * Checks if there is a sink but no open classes have been defined.
	 */
	private void checkForSinkButNoOpenClassError() {
		Vector<Object> sinks = station_def.getStationKeysSink();
		Vector<Object> openClasses = class_def.getOpenClassKeys();
		if (!sinks.isEmpty() && openClasses.isEmpty()) {
			errors[SINK_BUT_NO_OPEN_CLASS_ERROR] = true;
		}
	}

	/**
	 * Checks if a class has no reference station.
	 */
	private void checkForNoReferenceStationError() {
		Vector<Object> classes = class_def.getClassKeys();
		for (int i = 0; i < classes.size(); i++) {
			Object thisClass = classes.get(i);
			Object thisRefStation = class_def.getClassRefStation(thisClass);
			if (thisRefStation == null) {
				classesWithoutReferenceStation.add(thisClass);
				errors[NO_REFERENCE_STATION_ERROR] = true;
			}
		}
	}

	/**
	 * Checks if a station has no essential links.
	 */
	private void checkForNoEssentialLinkError() {
		Vector<Object> stations = station_def.getStationKeys();
		for (int i = 0; i < stations.size(); i++) {
			Object thisStation = stations.get(i);
			String thisStationType = station_def.getStationType(thisStation);
			if (thisStationType.equals(STATION_TYPE_SINK)) {
				Vector<Object> backwardStations = station_def.getBackwardConnections(thisStation);
				if (backwardStations.isEmpty()) {
					stationsWithoutEssentialLink.add(thisStation);
					errors[NO_ESSENTIAL_LINK_ERROR] = true;
				}
			} else if (thisStationType.equals(STATION_TYPE_TRANSITION)) {
				Vector<Object> backwardStations = station_def.getBackwardConnections(thisStation);
				Vector<Object> forwardStations = station_def.getForwardConnections(thisStation);
				if (backwardStations.isEmpty() && forwardStations.isEmpty()) {
					stationsWithoutEssentialLink.add(thisStation);
					errors[NO_ESSENTIAL_LINK_ERROR] = true;
				}
			} else {
				Vector<Object> forwardStations = station_def.getForwardConnections(thisStation);
				if (forwardStations.isEmpty()) {
					stationsWithoutEssentialLink.add(thisStation);
					errors[NO_ESSENTIAL_LINK_ERROR] = true;
				}
			}
		}
	}

	/**
	 * Checks if a measure has one or more 'null' field.
	 */
	private void checkForInvalidMeasureError() {
		Vector<Object> measures = simulation_def.getMeasureKeys();
		for (int i = 0; i < measures.size(); i++) {
			Object thisMeasure = measures.get(i);
			String thisMeasureType = simulation_def.getMeasureType(thisMeasure);
			Object thisMeasureStation = simulation_def.getMeasureStation(thisMeasure);
			if (thisMeasureType == null
					|| (!simulation_def.isGlobalMeasure(thisMeasureType) && thisMeasureStation == null)) {
				errors[INVALID_MEASURE_ERROR] = true;
			}
		}
	}

	/**
	 * Checks if the same measure is defined more than once.
	 */
	private void checkForRedundantMeasureError() {
		Vector<Object> measures = simulation_def.getMeasureKeys();
		Vector<String> measuresAlreadyChecked = new Vector<String>(0, 1);
		for (int i = 0; i < measures.size(); i++) {
			Object thisMeasure = measures.get(i);
			String thisMeasureType = simulation_def.getMeasureType(thisMeasure);
			Object thisMeasureClass = simulation_def.getMeasureClass(thisMeasure);
			String thisMeasureClassName = null;
			if (thisMeasureClass == null) {
				thisMeasureClassName = "ALL";
			} else {
				if (thisMeasureType.equals(SimulationDefinition.MEASURE_FX)) {
					thisMeasureClassName = (String) thisMeasureClass;
				} else {
					thisMeasureClassName = class_def.getClassName(thisMeasureClass);
				}
			}
			Object thisMeasureStation = simulation_def.getMeasureStation(thisMeasure);
			String thisMeasureStationName = null;
			if (thisMeasureStation == null) {
				if (simulation_def.isGlobalMeasure(thisMeasureType)) {
					thisMeasureStationName = "ALL";
				} else {
					thisMeasureStationName = "";
				}
			} else {
				if (blocking_def.getRegionKeys().contains(thisMeasureStation)) {
					thisMeasureStationName = blocking_def.getRegionName(thisMeasureStation);
				} else {
					thisMeasureStationName = station_def.getStationName(thisMeasureStation);
				}
			}
			String thisMeasureDescription = thisMeasureType + "_" + thisMeasureClassName + "_" + thisMeasureStationName;
			if (!measuresAlreadyChecked.contains(thisMeasureDescription)) {
				measuresAlreadyChecked.add(thisMeasureDescription);
			} else {
				redundantMeasures.add(thisMeasure);
				errors[DUPLICATE_MEASURE_ERROR] = true;
			}
		}
	}

	/**
	 * Deletes all redundant measures.
	 */
	public void deleteRedundantMeasures() {
		for (int i = 0; i < redundantMeasures.size(); i++) {
			Object thisRedundantMeasure = redundantMeasures.get(i);
			simulation_def.removeMeasure(thisRedundantMeasure);
		}
	}

	/**
	 * Checks if a station uses different queue strategies for classes within
	 * the same priority group.
	 */
	private void checkForInconsistentQueueStrategyError() {
		Vector<Object> stations = station_def.getStationKeys();
		Vector<Object> classes = class_def.getClassKeys();
		HashMap<Integer, String> strategies = new HashMap<Integer, String>();
		for (int i = 0; i < stations.size(); i++) {
			Object thisServer = stations.get(i);
			String stationStrategy = station_def.getStationQueueStrategy(thisServer);
			if (stationStrategy == null) {
				continue;
			}
			for (int j = 0; j < classes.size(); j++) {
				Object thisClass = classes.get(j);
				Integer priority = stationStrategy.equals(STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY)
						? class_def.getClassPriority(thisClass) : Integer.valueOf(0);
				String strategy = station_def.getQueueStrategy(thisServer, thisClass);
				if (!strategies.containsKey(priority)) {
					strategies.put(priority, strategy);
				} else if (!strategies.get(priority).equals(strategy)) {
					stationsWithInconsistentQueueStrategies.add(thisServer);
					errors[INCONSISTENT_QUEUE_STRATEGY_ERROR] = true;
					break;
				}
			}
			strategies.clear();
		}
	}

	/**
	 * Checks if a server uses SJF, LJF, SEPT or LEPT along with the load
	 * dependent service strategy.
	 */
	private void checkForUnpredictableServiceError() {
		Vector<Object> servers = station_def.getStationKeysServer();
		Vector<Object> classes = class_def.getClassKeys();
		for (int i = 0; i < servers.size(); i++) {
			Object thisServer = servers.get(i);
			for (int j = 1; j < classes.size(); j++) {
				Object thisClass = classes.get(j);
				String queueStrategy = station_def.getQueueStrategy(thisServer, thisClass);
				Object serviceStrategy = station_def.getServiceTimeDistribution(thisServer, thisClass);
				if ((queueStrategy.equals(QUEUE_STRATEGY_SJF) || queueStrategy.equals(QUEUE_STRATEGY_LJF)
						|| queueStrategy.equals(QUEUE_STRATEGY_SEPT) || queueStrategy.equals(QUEUE_STRATEGY_LEPT))
						&& serviceStrategy instanceof LDStrategy) {
					serversWithUnpredictableService.add(thisServer);
					errors[UNPREDICTABLE_SERVICE_ERROR] = true;
					break;
				}
			}
		}
	}

	/**
	 * Checks if there is a join but no forks.
	 */
	private void checkForJoinWithoutForkError() {
		Vector<Object> forks = station_def.getStationKeysFork();
		Vector<Object> joins = station_def.getStationKeysJoin();
		if (forks.isEmpty() && !joins.isEmpty()) {
			errors[JOIN_WITHOUT_FORK_ERROR] = true;
		}
	}

	/**
	 * Checks if tasks split from a job may be routed to different joins.
	 */
	private void checkForForkJoinRoutingError() {
		//TODO: implementation
	}

	/**
	 * Checks if a blocking region has no stations.
	 */
	private void checkForEmptyBlockingRegionError() {
		Vector<Object> regionKeys = blocking_def.getRegionKeys();
		for (Object regionKey : regionKeys) {
			if (blocking_def.getBlockingRegionStations(regionKey).isEmpty()) {
				emptyBlockingRegions.add(regionKey);
				errors[EMPTY_BLOCKING_REGION_ERROR] = true;
			}
		}
	}

	/**
	 * Deletes all empty blocking regions.
	 */
	public void deleteEmptyBlockingRegions() {
		for (int i = 0; i < emptyBlockingRegions.size(); i++) {
			blocking_def.deleteBlockingRegion(emptyBlockingRegions.get(i));
		}
	}

	/**
	 * Checks if the total preload in a blocking region exceeds the capacity
	 * constraints.
	 */
	private void checkForBlockingRegionCapacityOverloadError() {
		Vector<Object> regionKeys = blocking_def.getRegionKeys();
		Vector<Object> classKeys = class_def.getClassKeys();
		OUTER_LOOP:
		for (Object regionKey : regionKeys) {
			int globalPreload = 0;
			int[] classPreload = new int[classKeys.size()];
			Set<Object> stationKeys = blocking_def.getBlockingRegionStations(regionKey);
			for (Object stationKey : stationKeys) {
				for (int i = 0; i < classKeys.size(); i++) {
					int preload = simulation_def.getPreloadedJobs(stationKey, classKeys.get(i)).intValue();
					int weight = blocking_def.getRegionClassWeight(regionKey, classKeys.get(i)).intValue();
					globalPreload += preload * weight;
					classPreload[i] += preload * weight;
				}
			}

			int globalConstraint = blocking_def.getRegionCustomerConstraint(regionKey).intValue();
			if (globalConstraint > 0 && globalPreload > globalConstraint) {
				blockingRegionsWithCapacityOverload.add(regionKey);
				errors[BLOCKING_REGION_CAPACITY_OVERLOAD_ERROR] = true;
				continue;
			}

			for (int i = 0; i < classKeys.size(); i++) {
				int classConstraint = blocking_def.getRegionClassCustomerConstraint(regionKey, classKeys.get(i)).intValue();
				if (classConstraint > 0 && classPreload[i] > classConstraint) {
					blockingRegionsWithCapacityOverload.add(regionKey);
					errors[BLOCKING_REGION_CAPACITY_OVERLOAD_ERROR] = true;
					continue OUTER_LOOP;
				}
			}

			for (int i = 0; i < blocking_def.getRegionGroupList(regionKey).size(); i++) {
				int groupConstraint = blocking_def.getRegionGroupCustomerConstraint(regionKey, i).intValue();
				int groupPreload = 0;
				for (int j = 0; j < classKeys.size(); j++) {
					if (blocking_def.getRegionGroupClassList(regionKey, i).contains(classKeys.get(j))) {
						groupPreload += classPreload[j];
					}
				}
				if (groupConstraint > 0 && groupPreload > groupConstraint) {
					blockingRegionsWithCapacityOverload.add(regionKey);
					errors[BLOCKING_REGION_CAPACITY_OVERLOAD_ERROR] = true;
					continue OUTER_LOOP;
				}
			}
		}
	}

	/**
	 * Checks if the total preload in a blocking region exceeds the memory
	 * constraints.
	 */
	private void checkForBlockingRegionMemoryOverloadError() {
		Vector<Object> regionKeys = blocking_def.getRegionKeys();
		Vector<Object> classKeys = class_def.getClassKeys();
		OUTER_LOOP:
		for (Object regionKey : regionKeys) {
			int globalPreload = 0;
			int[] classPreload = new int[classKeys.size()];
			Set<Object> stationKeys = blocking_def.getBlockingRegionStations(regionKey);
			for (Object stationKey : stationKeys) {
				for (int i = 0; i < classKeys.size(); i++) {
					int preload = simulation_def.getPreloadedJobs(stationKey, classKeys.get(i)).intValue();
					int size = blocking_def.getRegionClassSize(regionKey, classKeys.get(i)).intValue();
					globalPreload += preload * size;
					classPreload[i] += preload * size;
				}
			}

			int globalConstraint = blocking_def.getRegionMemorySize(regionKey).intValue();
			if (globalConstraint > 0 && globalPreload > globalConstraint) {
				blockingRegionsWithMemoryOverload.add(regionKey);
				errors[BLOCKING_REGION_MEMORY_OVERLOAD_ERROR] = true;
				continue;
			}

			for (int i = 0; i < classKeys.size(); i++) {
				int classConstraint = blocking_def.getRegionClassMemorySize(regionKey, classKeys.get(i)).intValue();
				if (classConstraint > 0 && classPreload[i] > classConstraint) {
					blockingRegionsWithMemoryOverload.add(regionKey);
					errors[BLOCKING_REGION_MEMORY_OVERLOAD_ERROR] = true;
					continue OUTER_LOOP;
				}
			}

			for (int i = 0; i < blocking_def.getRegionGroupList(regionKey).size(); i++) {
				int groupConstraint = blocking_def.getRegionGroupMemorySize(regionKey, i).intValue();
				int groupPreload = 0;
				for (int j = 0; j < classKeys.size(); j++) {
					if (blocking_def.getRegionGroupClassList(regionKey, i).contains(classKeys.get(j))) {
						groupPreload += classPreload[j];
					}
				}
				if (groupConstraint > 0 && groupPreload > groupConstraint) {
					blockingRegionsWithMemoryOverload.add(regionKey);
					errors[BLOCKING_REGION_MEMORY_OVERLOAD_ERROR] = true;
					continue OUTER_LOOP;
				}
			}
		}
	}

	/**
	 * Checks if a class switch has a matrix in which some rows sum to less
	 * than one.
	 */
	private void checkForClassSwitchInvalidMatrixError() {
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
					classSwitchesWithInvalidMatrix.add(thisClassSwitch);
					errors[CLASS_SWITCH_INVALID_MATRIX_ERROR] = true;
					break;
				}
			}
		}
	}

	/**
	 * Checks if a class switch is in the model and closed classes have
	 * different reference stations.
	 */
	private void checkForClassSwitchReferenceStationError() {
		Vector<Object> classSwitches = station_def.getStationKeysClassSwitch();
		if (classSwitches.isEmpty()) {
			return;
		}
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
				errors[CLASS_SWITCH_REFERENCE_STATION_ERROR] = true;
				break;
			}
		}
	}

	/**
	 * Checks if a station uses a Guard strategy with zero tasks for a class.
	 */
	private void checkForZeroGuardStrategyError() {
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
				stationsWithZeroGuardStrategy.put(stationKey, classes);
				errors[ZERO_GUARD_STRATEGY_ERROR] = true;
			}
		}
	}

	/**
	 * Checks if a semaphore is not located between fork/join.
	 */
	private void checkForSemaphoreNotBetweenForkJoinError() {
		Vector<Object> semaphores = station_def.getStationKeysSemaphore();
		Vector<Object> forks = station_def.getStationKeysFork();
		semaphoresNotBetweenForkJoin.addAll(semaphores);
		for (int i = 0; i < forks.size(); i++) {
			Object thisFork = forks.get(i);
			Vector<Object> result = searchForStationBetweenForkJoin(STATION_TYPE_SEMAPHORE, thisFork);
			semaphoresNotBetweenForkJoin.removeAll(result);
		}
		if (!semaphoresNotBetweenForkJoin.isEmpty()) {
			errors[SEMAPHORE_NOT_BETWEEN_FORK_JOIN_ERROR] = true;
		}
	}

	/**
	 * Checks if a scaler is not located between fork/join.
	 */
	private void checkForScalerNotBetweenForkJoinError() {
		Vector<Object> scalers = station_def.getStationKeysScaler();
		Vector<Object> forks = station_def.getStationKeysFork();
		scalersNotBetweenForkJoin.addAll(scalers);
		for (int i = 0; i < forks.size(); i++) {
			Object thisFork = forks.get(i);
			Vector<Object> result = searchForStationBetweenForkJoin(STATION_TYPE_SCALER, thisFork);
			scalersNotBetweenForkJoin.removeAll(result);
		}
		if (!scalersNotBetweenForkJoin.isEmpty()) {
			errors[SCALER_NOT_BETWEEN_FORK_JOIN_ERROR] = true;
		}
	}

	/**
	 * Checks if a transition has an infinite enabling degree for a mode.
	 */
	private void checkForTransitionInfiniteEnablingDegreeError() {
		Vector<Object> classKeys = class_def.getClassKeys();
		if (classKeys.isEmpty()) {
			return;
		}
		Vector<Object> transitions = station_def.getStationKeysTransition();
		for (int i = 0; i < transitions.size(); i++) {
			Object thisTransition = transitions.get(i);
			Vector<Object> stationInKeys = station_def.getBackwardConnections(thisTransition);
			int size = station_def.getTransitionModeListSize(thisTransition);
			if (stationInKeys.isEmpty()) {
				for (int j = 0; j < size; j++) {
					int numberOfServers = station_def.getNumberOfServers(thisTransition, j).intValue();
					if (numberOfServers < 1) {
						transitionsWithInfiniteEnablingDegree.add(thisTransition);
						errors[TRANSITION_INFINITE_ENABLING_DEGREE_ERROR] = true;
						break;
					}
				}
			} else {
				for (int j = 0; j < size; j++) {
					boolean hasSpecifiedValues = false;
					for (Object stationInKey : stationInKeys) {
						for (Object classKey : classKeys) {
							int enablingValue = station_def.getEnablingCondition(thisTransition, j,
									stationInKey, classKey).intValue();
							int inhibitingValue = station_def.getInhibitingCondition(thisTransition, j,
									stationInKey, classKey).intValue();
							if (enablingValue > 0 || inhibitingValue > 0) {
								hasSpecifiedValues = true;
							}
						}
					}
					int numberOfServers = station_def.getNumberOfServers(thisTransition, j).intValue();
					if (!hasSpecifiedValues && numberOfServers < 1) {
						transitionsWithInfiniteEnablingDegree.add(thisTransition);
						errors[TRANSITION_INFINITE_ENABLING_DEGREE_ERROR] = true;
						break;
					}
				}
			}
		}
	}


	/** Checks if there is an impatience strategy selected at a station connected to a fork. */
	private void checkForImpatienceEnabledInStationBetweenForkJoinError() {
		Vector<Object> stations = station_def.getStationKeys();
		Vector<Object> classes = class_def.getClassKeys();
		Vector<Object> forks = station_def.getStationKeysFork();
		Vector<Object> stationsWithImpatience = new Vector<>();

		// Get all the stations that have impatience
		for (Object currentStation : stations) {
			if (!station_def.getStationType(currentStation).equals(STATION_TYPE_SERVER)) {
				continue;
			}

			for (Object currentClass : classes) {
				if (station_def.getImpatienceType(currentStation, currentClass) != ImpatienceType.NONE) {
					stationsWithImpatience.add(currentStation);
					break;
				}
			}
		}

		// Return if no stations have impatience set
		if (stationsWithImpatience.isEmpty()) {
			return;
		}

		// Check if stations with impatience are connected to a fork
		for (Object fork : forks) {
			Vector<Object> stationsBetweenForkJoin = searchForStationBetweenForkJoin(STATION_TYPE_SERVER,
							fork);

			if (stationsBetweenForkJoin == null) {
				continue;
			}

			for (Object currentStation : stationsBetweenForkJoin) {
				if (stationsWithImpatience.contains(currentStation)) {
					stationsWithImpatienceAndConnectedToFork.add(currentStation);
					errors[IMPATIENCE_ENABLED_IN_STATION_BETWEEN_FORK_JOIN_ERROR] = true;
					return;
				}
			}
		}
	}

	/**
	 * Checks if a server has different queue strategies.
	 */
	private void checkForBcmpDifferentQueueStrategiesWarning() {
		Vector<Object> servers = station_def.getStationKeysServer();
		Vector<Object> classes = class_def.getClassKeys();
		for (int i = 0; i < servers.size(); i++) {
			Object thisServer = servers.get(i);
			for (int j = 1; j < classes.size(); j++) {
				String lastStrategy = station_def.getQueueStrategy(thisServer, classes.get(j - 1));
				String thisStrategy = station_def.getQueueStrategy(thisServer, classes.get(j));
				if (!thisStrategy.equals(lastStrategy)) {
					bcmpServersWithDifferentQueueStrategies.add(thisServer);
					warnings[BCMP_DIFFERENT_QUEUE_STRATEGIES_WARNING] = true;
					break;
				}
			}
		}
	}

	/**
	 * Checks if an FCFS server has different service strategies.
	 */
	private void checkForBcmpFcfsDifferentServiceStrategiesWarning() {
		Vector<Object> servers = station_def.getStationKeysServer();
		for (int i = 0; i < servers.size(); i++) {
			Object thisServer = servers.get(i);
			if (isAllFCFSQueueStrategy(thisServer) && !isAllSameServiceStrategy(thisServer)) {
				bcmpFcfsServersWithDifferentServiceStrategies.add(thisServer);
				warnings[BCMP_FCFS_DIFFERENT_SERVICE_STRATEGIES_WARNING] = true;
			}
		}
	}

	/**
	 * Checks if an FCFS server has a service time distribution that is not
	 * exponential.
	 */
	private void checkForBcmpFcfsNonExponentialDistributionWarning() {
		Vector<Object> servers = station_def.getStationKeysServer();
		Vector<Object> classes = class_def.getClassKeys();
		for (int i = 0; i < servers.size(); i++) {
			Object thisServer = servers.get(i);
			if (isAllFCFSQueueStrategy(thisServer) && isAllSameServiceStrategy(thisServer)) {
				OUTER_LOOP:
				for (int j = 0; j < classes.size(); j++) {
					Object thisClass = classes.get(j);
					Object service = station_def.getServiceTimeDistribution(thisServer, thisClass);
					if (service instanceof Distribution) {
						Distribution d = (Distribution) service;
						if (!d.getName().equals(CommonConstants.DISTRIBUTION_EXPONENTIAL)) {
							bcmpFcfsServersWithNonExponentialDistribution.add(thisServer);
							warnings[BCMP_FCFS_NON_EXPONENTIAL_DISTRIBUTION_WARNING] = true;
							break;
						}
					} else if (service instanceof LDStrategy) {
						LDStrategy ld = (LDStrategy) service;
						Object[] ranges = ld.getAllRanges();
						for (Object range : ranges) {
							Distribution d = ld.getRangeDistribution(range);
							if (!d.getName().equals(CommonConstants.DISTRIBUTION_EXPONENTIAL)) {
								bcmpFcfsServersWithNonExponentialDistribution.add(thisServer);
								warnings[BCMP_FCFS_NON_EXPONENTIAL_DISTRIBUTION_WARNING] = true;
								break OUTER_LOOP;
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Checks if an FCFS server has different mean service times.
	 */
	private void checkForBcmpFcfsDifferentServiceTimesWarning() {
		Vector<Object> servers = station_def.getStationKeysServer();
		Vector<Object> classes = class_def.getClassKeys();
		if (!servers.isEmpty() && !classes.isEmpty()) {
			for (int i = 0; i < servers.size(); i++) {
				Object thisServer = servers.get(i);
				if (isAllFCFSQueueStrategy(thisServer) && isAllSameServiceStrategy(thisServer)
						&& !bcmpFcfsServersWithNonExponentialDistribution.contains(thisServer)) {
					Object service = station_def.getServiceTimeDistribution(thisServer, classes.get(0));
					if (service instanceof Distribution) {
						double mean = ((Distribution) service).getMean();
						for (int j = 0; j < classes.size(); j++) {
							Object thisClass = classes.get(j);
							Distribution d = (Distribution) station_def.getServiceTimeDistribution(thisServer, thisClass);
							double thisMean = d.getMean();
							if (thisMean != mean) {
								bcmpFcfsServersWithDifferentServiceTimes.add(thisServer);
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
								bcmpFcfsServersWithDifferentServiceTimes.add(thisServer);
								warnings[BCMP_FCFS_DIFFERENT_SERVICE_TIMES_WARNING] = true;
								break;
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Checks if a PS server has a service time distribution for which the
	 * Laplace transform is not rational.
	 */
	private void checkForBcmpPsNonRationalDistributionWarning() {
		//TODO: implementation
	}

	/**
	 * Checks if an LCFS-PR server has a service time distribution for which
	 * the Laplace transform is not rational.
	 */
	private void checkForBcmpLcfsPrNonRationalDistributionWarning() {
		//TODO: implementation
	}

	/**
	 * Checks if a delay has a service time distribution for which the Laplace
	 * transform is not rational.
	 */
	private void checkForBcmpDelayNonRationalDistributionWarning() {
		Vector<Object> delays = station_def.getStationKeysDelay();
		Vector<Object> classes = class_def.getClassKeys();
		for (int i = 0; i < delays.size(); i++) {
			Object thisDelay = delays.get(i);
			OUTER_LOOP:
			for (int j = 0; j < classes.size(); j++) {
				Object thisClass = classes.get(j);
				Object service = station_def.getServiceTimeDistribution(thisDelay, thisClass);
				if (service instanceof Distribution) {
					Distribution d = (Distribution) service;
					if (d.getName().equals(CommonConstants.DISTRIBUTION_PARETO)) {
						bcmpDelaysWithNonRationalDistribution.add(thisDelay);
						warnings[BCMP_DELAY_NON_RATIONAL_DISTRIBUTION_WARNING] = true;
						break;
					}
				} else if (service instanceof LDStrategy) {
					LDStrategy ld = (LDStrategy) service;
					Object[] ranges = ld.getAllRanges();
					for (Object range : ranges) {
						if (ld.getRangeDistribution(range).getName().equals(CommonConstants.DISTRIBUTION_PARETO)) {
							bcmpDelaysWithNonRationalDistribution.add(thisDelay);
							warnings[BCMP_DELAY_NON_RATIONAL_DISTRIBUTION_WARNING] = true;
							break OUTER_LOOP;
						}
					}
				}
			}
		}
	}

	/**
	 * Checks if a station has a routing strategy dependent on the state of the
	 * model.
	 */
	private void checkForBcmpStateDependentRoutingStrategyWarning() {
		Vector<Object> stations = station_def.getStationKeysNoSourceSink();
		Vector<Object> classes = class_def.getClassKeys();
		for (int i = 0; i < stations.size(); i++) {
			Object thisStation = stations.get(i);
			for (int j = 0; j < classes.size(); j++) {
				Object thisClass = classes.get(j);
				RoutingStrategy strategy = (RoutingStrategy) station_def.getRoutingStrategy(thisStation, thisClass);
				if (strategy != null) {
					if (strategy.isModelStateDependent()) {
						bcmpStationsWithStateDependentRoutingStrategy.add(thisStation);
						warnings[BCMP_STATE_DEPENDENT_ROUTING_STRATEGY_WARNING] = true;
						break;
					}
				}
			}
		}
	}

	/**
	 * Checks if a server has a scheduling policy other than PS, FCFS or RAND.
	 */
	private void checkForBcmpAsymmetricSchedulingPolicyWarning() {
		Vector<Object> servers = station_def.getStationKeysServer();
		Vector<Object> classes = class_def.getClassKeys();
		for (int i = 0; i < servers.size(); i++) {
			Object thisServer = servers.get(i);
			for (int j = 0; j < classes.size(); j++) {
				Object thisClass = classes.get(j);
				String strategy = station_def.getQueueStrategy(thisServer, thisClass);
				if (strategy.equals(CommonConstants.QUEUE_STRATEGY_PS)
						|| strategy.equals(CommonConstants.QUEUE_STRATEGY_FCFS)
						|| strategy.equals(CommonConstants.QUEUE_STRATEGY_RAND)) {
					continue;
				}
				bcmpServersWithAsymmetricSchedulingPolicy.add(thisServer);
				warnings[BCMP_ASYMMETRIC_SCHEDULING_POLICY_WARNING] = true;
				break;
			}
		}
	}

	private boolean isAllFCFSQueueStrategy(Object serverKey) {
		Vector<Object> classes = class_def.getClassKeys();
		for (int j = 0; j < classes.size(); j++) {
			String strategy = station_def.getQueueStrategy(serverKey, classes.get(j));
			if (!strategy.equals(CommonConstants.QUEUE_STRATEGY_FCFS)) {
				return false;
			}
		}
		return true;
	}

	private boolean isAllSameServiceStrategy(Object stationKey) {
		Vector<Object> classes = class_def.getClassKeys();
		if (!classes.isEmpty()) {
			boolean distrFound = false;
			boolean ldFound = false;
			for (int j = 0; j < classes.size(); j++) {
				Object thisClass = classes.get(j);
				Object service = station_def.getServiceTimeDistribution(stationKey, thisClass);
				if (service instanceof Distribution) {
					distrFound = true;
				} else if (service instanceof LDStrategy) {
					ldFound = true;
				}
			}
			if (distrFound && ldFound) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks if a station has no optional links.
	 */
	private void checkForNoOptionalLinkWarning() {
		Vector<Object> stations = station_def.getStationKeysNoSourceSink();
		for (int i = 0; i < stations.size(); i++) {
			Object thisStation = stations.get(i);
			String thisStationType = station_def.getStationType(thisStation);
			if (thisStationType.equals(STATION_TYPE_TRANSITION)) {
				continue;
			}
			Vector<Object> backwardStations = station_def.getBackwardConnections(thisStation);
			if (backwardStations.isEmpty()) {
				stationsWithoutOptionalLink.add(thisStation);
				warnings[NO_OPTIONAL_LINK_WARNING] = true;
			}
		}
	}

	/**
	 * Checks if there is a fork but no joins.
	 */
	private void checkForForkWithoutJoinWarning() {
		Vector<Object> forks = station_def.getStationKeysFork();
		Vector<Object> joins = station_def.getStationKeysJoin();
		if (!forks.isEmpty() && joins.isEmpty()) {
			warnings[FORK_WITHOUT_JOIN_WARNING] = true;
		}
	}

	/**
	 * Checks if the parametric analysis model becomes inconsistent with the
	 * simulation model.
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
	 * Checks if a parametric analysis model was defined but no parametric
	 * analysis is available.
	 */
	private void checkForParametricAnalysisNotAvailableWarning() {
		ParametricAnalysisChecker pac = new ParametricAnalysisChecker(class_def, station_def, simulation_def);
		if (simulation_def.isParametricAnalysisEnabled() && !pac.canBeEnabled()) {
			warnings[PARAMETRIC_ANALYSIS_NOT_AVAILABLE_WARNING] = true;
		}
	}

	/**
	 * Checks if the population of a closed class is zero.
	 */
	private void checkForZeroPopulationWarning() {
		Vector<Object> classSwitches = station_def.getStationKeysClassSwitch();
		Vector<Object> tranitions = station_def.getStationKeysTransition();
		if (!classSwitches.isEmpty() || !tranitions.isEmpty()) {
			return;
		}
		if (class_def.getTotalClosedClassPopulation() == 0) {
			return;
		}
		for (Object closedClassKey : class_def.getClosedClassKeys()) {
			if (class_def.getClassPopulation(closedClassKey).intValue() == 0) {
				closedClassesWithZeroPopulation.add(closedClassKey);
				warnings[ZERO_POPULATION_WARNING] = true;
			}
		}
	}

	/**
	 * Checks if the total population of closed classes is zero.
	 */
	private void checkForZeroTotalPopulationWarning() {
		if (!class_def.getClosedClassKeys().isEmpty() && class_def.getTotalClosedClassPopulation() == 0) {
			errors[ZERO_TOTAL_POPULATION_WARNING] = true;
		}
	}

	/**
	 * Checks if a class switch is located between fork/join.
	 */
	private void checkForClassSwitchBetweenForkJoinWarning() {
		Vector<Object> forks = station_def.getStationKeysFork();
		for (int i = 0; i < forks.size(); i++) {
			Object thisFork = forks.get(i);
			Vector<Object> result = searchForStationBetweenForkJoin(STATION_TYPE_CLASSSWITCH, thisFork);
			classSwitchesBetweenForkJoin.removeAll(result);
			classSwitchesBetweenForkJoin.addAll(result);
		}
		if (!classSwitchesBetweenForkJoin.isEmpty()) {
			warnings[CLASS_SWITCH_BETWEEN_FORK_JOIN_WARNING] = true;
		}
	}

	/**
	 * Checks if a station uses priority scheduling but all the classes have
	 * the same priority.
	 */
	private void checkForSchedulingSamePriorityWarning() {
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

		Vector<Object> stations = station_def.getStationKeys();
		for (int i = 0; i < stations.size(); i++) {
			Object thisStation = stations.get(i);
			String stationStrategy = station_def.getStationQueueStrategy(thisStation);
			if (stationStrategy != null && stationStrategy.equals(CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY)) {
				stationsWithPriorityScheduling.add(thisStation);
				warnings[SCHEDULING_SAME_PRIORITY_WARNING] = true;
			}
		}
	}

	/**
	 * Checks if a transition is located between fork/join.
	 */
	private void checkForTransitionBetweenForkJoinWarning() {
		Vector<Object> forks = station_def.getStationKeysFork();
		for (int i = 0; i < forks.size(); i++) {
			Object thisFork = forks.get(i);
			Vector<Object> result = searchForStationBetweenForkJoin(STATION_TYPE_TRANSITION, thisFork);
			transitionsBetweenForkJoin.removeAll(result);
			transitionsBetweenForkJoin.addAll(result);
		}
		if (!transitionsBetweenForkJoin.isEmpty()) {
			warnings[TRANSITION_BETWEEN_FORK_JOIN_WARNING] = true;
		}
	}

	/**
	 * Checks if a transition has a constant enabling degree for a mode.
	 */
	private void checkForTransitionConstantEnablingDegreeWarning() {
		Vector<Object> classKeys = class_def.getClassKeys();
		if (classKeys.isEmpty()) {
			return;
		}
		Vector<Object> transitions = station_def.getStationKeysTransition();
		for (int i = 0; i < transitions.size(); i++) {
			Object thisTransition = transitions.get(i);
			Vector<Object> stationInKeys = station_def.getBackwardConnections(thisTransition);
			int size = station_def.getTransitionModeListSize(thisTransition);
			if (stationInKeys.isEmpty()) {
				for (int j = 0; j < size; j++) {
					int numberOfServers = station_def.getNumberOfServers(thisTransition, j).intValue();
					if (numberOfServers >= 1) {
						transitionsWithConstantEnablingDegree.add(thisTransition);
						warnings[TRANSITION_CONSTANT_ENABLING_DEGREE_WARNING] = true;
						break;
					}
				}
			} else {
				for (int j = 0; j < size; j++) {
					boolean hasSpecifiedValues = false;
					for (Object stationInKey : stationInKeys) {
						for (Object classKey : classKeys) {
							int enablingValue = station_def.getEnablingCondition(thisTransition, j,
									stationInKey, classKey).intValue();
							int inhibitingValue = station_def.getInhibitingCondition(thisTransition, j,
									stationInKey, classKey).intValue();
							if (enablingValue > 0 || inhibitingValue > 0) {
								hasSpecifiedValues = true;
							}
						}
					}
					int numberOfServers = station_def.getNumberOfServers(thisTransition, j).intValue();
					if (!hasSpecifiedValues && numberOfServers >= 1) {
						transitionsWithConstantEnablingDegree.add(thisTransition);
						warnings[TRANSITION_CONSTANT_ENABLING_DEGREE_WARNING] = true;
						break;
					}
				}
			}
		}
	}

	/**
	 * Checks if a transition has invalid input condition for a mode.
	 */
	private void checkForTransitionInvalidInputConditionWarning() {
		Vector<Object> classKeys = class_def.getClassKeys();
		if (classKeys.isEmpty()) {
			return;
		}
		Vector<Object> transitions = station_def.getStationKeysTransition();
		for (int i = 0; i < transitions.size(); i++) {
			Object thisTransition = transitions.get(i);
			Vector<Object> stationInKeys = station_def.getBackwardConnections(thisTransition);
			if (stationInKeys.isEmpty()) {
				continue;
			}
			int size = station_def.getTransitionModeListSize(thisTransition);
			for (int j = 0; j < size; j++) {
				boolean hasInvalidValues = false;
				for (Object stationInKey : stationInKeys) {
					for (Object classKey : classKeys) {
						int enablingValue = station_def.getEnablingCondition(thisTransition, j,
								stationInKey, classKey).intValue();
						int inhibitingValue = station_def.getInhibitingCondition(thisTransition, j,
								stationInKey, classKey).intValue();
						if (inhibitingValue > 0 && enablingValue >= inhibitingValue) {
							hasInvalidValues = true;
						}
					}
				}
				if (hasInvalidValues) {
					transitionsWithInvalidInputCondition.add(thisTransition);
					warnings[TRANSITION_INVALID_INPUT_CONDITION_WARNING] = true;
					break;
				}
			}
		}
	}

	/**
	 * Checks if a transition has no firing outcome for a mode.
	 */
	private void checkForTransitionNoFiringOutcomeWarning() {
		Vector<Object> classKeys = class_def.getClassKeys();
		if (classKeys.isEmpty()) {
			return;
		}
		Vector<Object> transitions = station_def.getStationKeysTransition();
		for (int i = 0; i < transitions.size(); i++) {
			Object thisTransition = transitions.get(i);
			Vector<Object> stationOutKeys = station_def.getForwardConnections(thisTransition);
			if (stationOutKeys.isEmpty()) {
				transitionsWithoutFiringOutcome.add(thisTransition);
				warnings[TRANSITION_NO_FIRING_OUTCOME_WARNING] = true;
			} else {
				int size = station_def.getTransitionModeListSize(thisTransition);
				for (int j = 0; j < size; j++) {
					boolean hasSpecifiedValues = false;
					for (Object stationOutKey : stationOutKeys) {
						for (Object classKey : classKeys) {
							int firingValue = station_def.getFiringOutcome(thisTransition, j,
									stationOutKey, classKey).intValue();
							if (firingValue > 0) {
								hasSpecifiedValues = true;
							}
						}
					}
					if (!hasSpecifiedValues) {
						transitionsWithoutFiringOutcome.add(thisTransition);
						warnings[TRANSITION_NO_FIRING_OUTCOME_WARNING] = true;
						break;
					}
				}
			}
		}
	}

	/**
	 * Searches for all the stations of a given type between fork/join.
	 * @param type the type of station to be searched for.
	 * @param start the fork where the search starts.
	 * @return all the stations of the given type found between fork/join, or
	 * null if start is not a fork.
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

}
