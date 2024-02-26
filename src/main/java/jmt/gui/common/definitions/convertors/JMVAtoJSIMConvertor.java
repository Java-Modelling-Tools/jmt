package jmt.gui.common.definitions.convertors;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.Defaults;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.definitions.ServerType;
import jmt.gui.common.definitions.SimulationDefinition;
import jmt.gui.common.distributions.Exponential;
import jmt.gui.common.routingStrategies.ProbabilityRouting;
import jmt.gui.common.routingStrategies.RandomRouting;
import jmt.gui.common.serviceStrategies.LDStrategy;
import jmt.jmva.analytical.ExactConstants;
import jmt.jmva.analytical.ExactModel;

import java.util.ArrayList;
import java.util.List;

public class JMVAtoJSIMConvertor {

    /**
     * Converts a JMVA model to JSIM. Conversion is performed by equalling service times in every
     * LI station (adjusting visits) and creating FCFS queues with exponential service time distribution.
     * <br>
     * Visits are converted with routing probability, to maintain correctness of computed response time values
     * a "virtual" node called  RefStation is added as a reference station for closed classes and
     * routed when (in terms of mean values) a single visit is performed in the system. (as visits values are scaled to
     * compute probability distribution).
     * <br>
     * Note that a single router node, called "Router" is used to route jobs through the
     * entire network, simplifying its topology. Stations (LI, LD and Delays) are connected in a parallel form with
     * the Router.
     * @param input JMVA model (read only access)
     * @param output target JSIM or JMODEL model. This is expected to be empty
     * @return a List with all found warnings during conversion (in String format).
     */
    public static List<String> convert(ExactModel input, CommonModel output) {
        // Changes default values, then restores default back at the end of method
        String defaultRouting = Defaults.get("stationRoutingStrategy");
        String defaultStationQueue = Defaults.get("stationStationQueueStrategy");
        String defaultQueue = Defaults.get("stationQueueStrategy");
        Defaults.set("stationRoutingStrategy", RandomRouting.class.getName());
        Defaults.set("stationStationQueueStrategy", CommonConstants.STATION_QUEUE_STRATEGY_PSSERVER);
        Defaults.set("stationQueueStrategy", CommonConstants.QUEUE_STRATEGY_PS);

        ArrayList<String> res = new ArrayList<String>();
        // Keys for unique items
        Object sourceKey = null, sinkKey = null, routerKey, refRouterKey = null;
        // Sums visit for each class
        double[] visitSum = new double[input.getClasses()];
        // Visits matrix (row: stations, column: classes)
        double[][] visits = input.getVisits();

        // Convert classes
        Object[] classKeys = new Object[input.getClasses()];
        for (int i = 0; i < input.getClasses(); i++) {
            String name = input.getClassNames()[i];
            int type = input.getClassTypes()[i];
            // This holds customers if class is closed or arrival rate if open
            double data = input.getClassData()[i];
            int priority = input.getClassPriorities()[i];
            Object key;

            visitSum[i] = 1; // Sums visit for each station. This is initialized to one as we
            // count visit to reference station
            for (int j = 0; j < input.getStations(); j++) {
                visitSum[i] += visits[j][i];
            }

	    Double softDeadline = -1.0;
            if (type == ExactConstants.CLASS_CLOSED) {
                // Closed class
                key = output.addClass(name, CommonConstants.CLASS_TYPE_CLOSED, priority, softDeadline, new Integer((int) data), null);
            } else {
                // Open class
                Exponential exp = new Exponential();
                exp.setMean(1 / data);
                key = output.addClass(name, CommonConstants.CLASS_TYPE_OPEN, priority, softDeadline, null, exp);
            }
            classKeys[i] = key;
        }

        routerKey = output.addStation("Router", CommonConstants.STATION_TYPE_ROUTER, 1, new ArrayList<ServerType>());

        // Creates source, sink and router (if needed)
        if (input.isClosed() || input.isMixed()) {
            refRouterKey = output.addStation("RefStation", CommonConstants.STATION_TYPE_ROUTER, 1, new ArrayList<ServerType>());
            // Makes connection between refRouter and router
            output.setConnected(refRouterKey, routerKey, true);
            output.setConnected(routerKey, refRouterKey, true);
            // Gives warning on refStation
            res.add("A special node, called RefStation, is added to compute the system response times and throughputs of closed classes. "
                    + "Its presence is fundamental to compute the number of visits at each station for closed classes.");
        }

        if (input.isOpen() || input.isMixed()) {
            sourceKey = output.addStation("Source", CommonConstants.STATION_TYPE_SOURCE, 1, new ArrayList<ServerType>());
            sinkKey = output.addStation("Sink", CommonConstants.STATION_TYPE_SINK, 1, new ArrayList<ServerType>());
            //Makes connections between source, sink and router
            output.setConnected(sourceKey, routerKey, true);
            output.setConnected(routerKey, sinkKey, true);
        }

        // Convert stations
        Object[] stationKeys = new Object[input.getStations()];
        for (int i = 0; i < input.getStations(); i++) {
            String name = input.getStationNames()[i];
            int type = input.getStationTypes()[i];
            int servers = input.getStationServers()[i];
            double[][] serviceTimes = input.getServiceTimes()[i];
            Object key = null;
            switch (type) {
            case ExactConstants.STATION_DELAY:
                // Delay
                key = addDelayStation(output, classKeys, name, serviceTimes);
                break;
            case ExactConstants.STATION_LI:
                // Load independent
                key = addLIStation(output, classKeys, name, serviceTimes, servers);
                break;
            case ExactConstants.STATION_LD:
                // Load dependent - this is single class only, but here
                key = addLDStation(output, classKeys, name, serviceTimes, servers);
                break;
            case ExactConstants.STATION_PRS:
                // FCFS Preemptive Resume
                key = addPRSStation(output, classKeys, name, serviceTimes);
                break;
            case ExactConstants.STATION_HOL:
                // FCFS Non-preemptive Head-of-Line
                key = addHOLStation(output, classKeys, name, serviceTimes);
                break;
            }
            stationKeys[i] = key;

            // Make connections with router
            output.setConnected(routerKey, key, true);
            output.setConnected(key, routerKey, true);
        }

        // Sets routing for router
        for (int i = 0; i < classKeys.length; i++) {
            ProbabilityRouting pr = new ProbabilityRouting();
            output.setRoutingStrategy(routerKey, classKeys[i], pr);
            for (int j = 0; j < stationKeys.length; j++) {
                pr.getValues().put(stationKeys[j], new Double(visits[j][i] / visitSum[i]));
            }

            // Sets refRouter as reference station for closed class, sets its routing and avoid put jobs into sink
            if (output.getClassType(classKeys[i]) == CommonConstants.CLASS_TYPE_CLOSED) {
                output.setClassRefStation(classKeys[i], refRouterKey);
                pr.getValues().put(refRouterKey, new Double(1 / visitSum[i]));
                if (sinkKey != null) {
                    pr.getValues().put(sinkKey, new Double(0.0));
                }
            }
            // Sets source as reference station for open class and sets sink routing, avoid routing to refRouter
            else {
                output.setClassRefStation(classKeys[i], sourceKey);
                pr.getValues().put(sinkKey, new Double(1 / visitSum[i]));
                if (refRouterKey != null) {
                    pr.getValues().put(refRouterKey, new Double(0.0));
                }
            }
        }

        // Create measures
        createMeasures(output, classKeys, stationKeys);

        // Restores default values
        Defaults.set("stationRoutingStrategy", defaultRouting);
        Defaults.set("stationStationQueueStrategy", defaultStationQueue);
        Defaults.set("stationQueueStrategy", defaultQueue);

        // Manage preloading
        output.manageJobs();

        // Return warnings
        return res;
    }

    private static Object addDelayStation(CommonModel output, Object[] classKeys, String name, double[][] serviceTimes) {
        Object key = output.addStation(name, CommonConstants.STATION_TYPE_DELAY, 1, new ArrayList<ServerType>());
        // Sets distribution for each class
        for (int j = 0; j < classKeys.length; j++) {
            Exponential exp = new Exponential();
            exp.setMean(serviceTimes[j][0]);
            output.setServiceTimeDistribution(key, classKeys[j], exp);
        }
        return key;
    }

    private static Object addLIStation(CommonModel output, Object[] classKeys, String name, double[][] serviceTimes, int servers) {
        Object key = output.addStation(name, CommonConstants.STATION_TYPE_SERVER, 1, new ArrayList<ServerType>());
        output.setStationNumberOfServers(key, new Integer(servers));
        // Sets distribution for each class
        for (int j = 0; j < classKeys.length; j++) {
            Exponential exp = new Exponential();
            exp.setMean(serviceTimes[j][0]);
            output.setServiceTimeDistribution(key, classKeys[j], exp);
        }
        return key;
    }

    private static Object addLDStation(CommonModel output, Object[] classKeys, String name, double[][] serviceTimes, int servers) {
        // we support multiclass too (future extensions).

        Object key = output.addStation(name, CommonConstants.STATION_TYPE_SERVER, 1, new ArrayList<ServerType>());
        output.setStationNumberOfServers(key, new Integer(servers));
        // Sets distribution for each class
        for (int j = 0; j < classKeys.length; j++) {
            LDStrategy lds = new LDStrategy();
            Object rangeKey = lds.getAllRanges()[0];
            for (int range = 0; range < serviceTimes[j].length; range++) {
                // First range is already available
                if (range > 0) {
                    rangeKey = lds.addRange();
                }
                Exponential exp = new Exponential();
                exp.setMean(serviceTimes[j][range]);
                lds.setRangeDistribution(rangeKey, exp);
                lds.setRangeDistributionMean(rangeKey, Double.toString(serviceTimes[j][range]));
            }
            output.setServiceTimeDistribution(key, classKeys[j], lds);
        }
        return key;
    }


    private static Object addPRSStation(CommonModel output, Object[] classKeys, String name, double[][] serviceTimes) {
        Object key = output.addStation(name, CommonConstants.STATION_TYPE_SERVER, 1, new ArrayList<ServerType>());
        output.setStationQueueStrategy(key, CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE);
        // Sets distribution for each class
        for (int j = 0; j < classKeys.length; j++) {
            Exponential exp = new Exponential();
            exp.setMean(serviceTimes[j][0]);
            output.setServiceTimeDistribution(key, classKeys[j], exp);
            output.setQueueStrategy(key, classKeys[j], CommonConstants.QUEUE_STRATEGY_FCFS_PR);
        }
        return key;
    }


    private static Object addHOLStation(CommonModel output, Object[] classKeys, String name, double[][] serviceTimes) {
        // Non-preemptive Priority Head-of-line (FCFS)
        Object key = output.addStation(name, CommonConstants.STATION_TYPE_SERVER, 1, new ArrayList<ServerType>());
        output.setStationQueueStrategy(key, CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY);
        // Sets distribution for each class
        for (int j = 0; j < classKeys.length; j++) {
            Exponential exp = new Exponential();
            exp.setMean(serviceTimes[j][0]);
            output.setServiceTimeDistribution(key, classKeys[j], exp);
            output.setQueueStrategy(key, classKeys[j], CommonConstants.QUEUE_STRATEGY_FCFS);
        }
        return key;
    }



    private static void createMeasures(CommonModel output, Object[] classKeys, Object[] stationKeys) {
        for (Object classKey : classKeys) {
            for (Object stationKey : stationKeys) {
                // Queue length
                output.addMeasure(SimulationDefinition.MEASURE_QL, stationKey, classKey);
                // Residence Time
                output.addMeasure(SimulationDefinition.MEASURE_RD, stationKey, classKey);
                // Utilization
                output.addMeasure(SimulationDefinition.MEASURE_U, stationKey, classKey);
                // Throughput
                output.addMeasure(SimulationDefinition.MEASURE_X, stationKey, classKey);
            }
        }
    }
}
