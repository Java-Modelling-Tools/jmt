package jmt.gui.common.definitions.convertors;

import Jama.Matrix;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.routingStrategies.ProbabilityRouting;
import jmt.gui.common.routingStrategies.RandomRouting;
import jmt.gui.common.routingStrategies.RoutingStrategy;
import jmt.gui.common.serviceStrategies.DisabledStrategy;
import jmt.gui.common.serviceStrategies.LDStrategy;
import jmt.gui.common.serviceStrategies.ZeroStrategy;
import jmt.jmva.analytical.ExactConstants;
import jmt.jmva.analytical.ExactModel;

import java.util.*;

public class JSIMtoJMVAConvertor {

    /**
     * Converts a JSIM model to JMVA. Visits are computed from topology. To work well this
     * method requires:
     * <ul>
     * <li> Reference station to be set for each class
     * <li> Every station must have at least one incoming connection and one outgoing connection
     * <li> Routing strategy have to be <code>ProbabilityRouting</code> or <code>RandomRouting</code>
     * </ul>
     * @param input JSIM model (read only)
     * @param output empty JMVA model (write)
     * @return a vector that enumerates all conversion warnings and how they have been fixed
     */
    public static List<String> convert(CommonModel input, ExactModel output) {
        // Manage probabilities
        input.manageProbabilities();

        // Used to store warnings
        ArrayList<String> res = new ArrayList<String>();
        int classNum, stationNum;
        // Used to iterate on lists
        Iterator<Object> it;

        // Number of classes
        classNum = input.getClassKeys().size();
        Vector<Object> classKeys = input.getClassKeys();

        // Find number of convertible stations
        it = input.getStationKeys().iterator();
        stationNum = 0;
        Vector<Object> stationKeys = new Vector<Object>();
        while (it.hasNext()) {
            Object key = it.next();
            String stationType = input.getStationType(key);
            if (stationType.equals(CommonConstants.STATION_TYPE_DELAY) || stationType.equals(CommonConstants.STATION_TYPE_SERVER)) {
                stationNum++;
                stationKeys.add(key);
            }
        }

        // Resizes output data structure
        output.resize(stationNum, classNum);

        // Exports class data
        exportClassData(input, output, classNum, classKeys, res);

        // Exports station data
        exportStationData(input, output, stationNum, classNum, stationKeys, classKeys, res);

        // Now calculates visits starting from routing.
        double[][] visits = new double[stationNum][classNum];
        double[] vis = null; // array used to store results of mldivide
        for (int cl = 0; cl < classNum; cl++) {
            // This is not equivalent to stationKeys as routers are considered
            Vector<Object> stations = input.getStationKeysNoSourceSink();
            if (output.getClassTypes()[cl] == ExactConstants.CLASS_OPEN) {
                // Open class, must calculate routing from source
                Object refStat = input.getClassRefStation(classKeys.get(cl));
                double[] p0;
                if (refStat == null || refStat.equals(CommonConstants.STATION_TYPE_FORK) || refStat.equals(CommonConstants.STATION_TYPE_CLASSSWITCH)
                        || refStat.equals(CommonConstants.STATION_TYPE_SCALER) || refStat.equals(CommonConstants.STATION_TYPE_TRANSITION)) {
                    // Reference station for this class was not valid
                    Vector<Object> sources = input.getStationKeysSource();
                    if (sources.size() > 0) {
                        refStat = sources.get(0);
                        res.add("Reference station for " + output.getClassNames()[cl] + " is not valid. "
                                + input.getStationName(refStat) + " is chosen instead.");
                    } else {
                        res.add("Reference station for " + output.getClassNames()[cl] + " is not valid. "
                                + output.getStationNames()[0] + " is chosen instead.");
                    }
                }

                if (refStat != null) {
                    p0 = getRoutingProbability(refStat, classKeys.get(cl), input, stations, res);
                } else {
                    p0 = new double[stations.size()];
                    p0[0] = 1; // Assumes that all jobs enter first station
                }
                try {
                    Matrix b = new Matrix(p0, 1);
                    Matrix P = new Matrix(buildProbabilityMatrix(stations, input, classKeys.get(cl), res));
                    // V = (P-eye(3))' \ (-b') where \ is "mldivide"
                    Matrix V = P.minus(Matrix.identity(stations.size(), stations.size())).solveTranspose(b.uminus());
                    vis = V.getColumnPackedCopy();
                } catch (Exception e) {
                    // Matrix is singular
                    res.add("Cannot compute the number of visits for " + output.getClassNames()[cl]
                            + " as the topology of the model is badly specified.");
                }
            } else {
                // Closed class, system is undefined, so sets visits to reference station to
                // 1 and builds a smaller P matrix
                Object refStat = input.getClassRefStation(classKeys.get(cl));
                if (refStat == null) {
                    refStat = stations.get(0);
                    res.add("Reference station for " + output.getClassNames()[cl] + " is not valid. "
                            + input.getStationName(refStat) + " is chosen instead.");
                }

                // Sets visits to reference station (if allowed) to 1
                if (stationKeys.contains(refStat)) {
                    visits[stationKeys.lastIndexOf(refStat)][cl] = 1;
                }
                // Removes reference station from stations vector and computes p0
                stations.remove(refStat);
                double[] p0 = getRoutingProbability(refStat, classKeys.get(cl), input, stations, res);

                try {
                    Matrix b = new Matrix(p0, 1);
                    Matrix P = new Matrix(buildProbabilityMatrix(stations, input, classKeys.get(cl), res));
                    // V = (P-eye(3))' \ (-b') where \ is "mldivide"
                    Matrix V = P.minus(Matrix.identity(stations.size(), stations.size())).solveTranspose(b.uminus());
                    vis = V.getColumnPackedCopy();
                } catch (Exception e) {
                    // Matrix is singular
                    res.add("Cannot compute the number of visits for " + output.getClassNames()[cl]
                            + " as the topology of the model is badly specified.");
                }
            }

            // Puts computed values into visits matrix. Rounds at 1e-10 for machine precision issues
            if (vis != null) {
                for (int i = 0; i < vis.length; i++) {
                    // Skips not allowed components (Routers, Terminals, Fork, Join....)
                    if (stationKeys.contains(stations.get(i))) {
                        visits[stationKeys.lastIndexOf(stations.get(i))][cl] = 1e-10 * Math.round(vis[i] * 1e10);
                    }
                }
            }
        }
        output.setVisits(visits);
        return res;
    }

    private static void exportClassData(CommonModel input, ExactModel output, int classNum, Vector<Object> classKeys, List<String> warnings) {
        int[] classTypes = new int[classNum];
        String[] classNames = new String[classNum];
        double[] classData = new double[classNum];
        int[] classPriorities = new int[classNum];

        for (int i = 0; i < classNum; i++) {
            Object key = classKeys.get(i);
            classNames[i] = input.getClassName(key);
            classPriorities[i] = input.getClassPriority(key);
            if (input.getClassType(key) == CommonConstants.CLASS_TYPE_CLOSED) {
                // Closed class parameters
                classTypes[i] = ExactConstants.CLASS_CLOSED;
                classData[i] = input.getClassPopulation(key).doubleValue();
            } else {
                // Open class parameters
                classTypes[i] = ExactConstants.CLASS_OPEN;
                Distribution d = (Distribution) input.getClassDistribution(key);
                if (d != null && d.hasMean()) {
                    classData[i] = 1.0 / d.getMean();
                } else {
                    classData[i] = 1.0;
                    warnings.add("Interarrival time distribution for " + input.getClassName(key) + " does not have a valid mean value. "
                            + "Arrival rate is set to default value 1.");
                }
            }
        }
        // Sets extracted values to output
        output.setClassNames(classNames);
        output.setClassTypes(classTypes);
        output.setClassData(classData);
        output.setClassPriorities(classPriorities);
        classNames = null;
        classTypes = null;
        classData = null;
        classPriorities = null;
    }

    private static void exportStationData(CommonModel input,
                                          ExactModel output,
                                          int stationNum,
                                          int classNum,
                                          Vector<Object> stationKeys,
                                          Vector<Object> classKeys,
                                          List<String> warnings) {
        double[][][] serviceTimes = new double[stationNum][classNum][];
        int[] stationTypes = new int[stationNum];
        String[] stationNames = new String[stationNum];
        int[] stationServers = new int[stationNum];
        for (int st = 0; st < stationNum; st++) {
            Object key = stationKeys.get(st);
            stationNames[st] = input.getStationName(key);
            Integer serverNum = input.getStationNumberOfServers(key);
            if (serverNum != null && serverNum.intValue() > 0) {
                stationServers[st] = serverNum.intValue();
            } else {
                stationServers[st] = 1;
            }
            if (input.getStationType(key).equals(CommonConstants.STATION_TYPE_DELAY)) {
                stationTypes[st] = ExactConstants.STATION_DELAY;
            } else if (input.getStationType(key).equals(CommonConstants.STATION_TYPE_SERVER)) {
                // A queue of some sort
                if (input.getStationQueueStrategy(key).equals(CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE)) {
                    // FCFS Preemptive Resume
                    // In JSIM, the only service strategy is LCFS but JMVA only has FCFS for preemptive scheduling
                    stationTypes[st] = ExactConstants.STATION_PRS;
                } else if (input.getStationQueueStrategy(key).equals(CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY)) {
                    // FCFS Non-preemptive Head-of-Line
                    // Currently this is the only non-preemptive strategy
                    stationTypes[st] = ExactConstants.STATION_HOL;
                } else {
                    // JSIM implements more serving strategies than JMVA
                    stationTypes[st] = ExactConstants.STATION_LI;
                }
            } else {
                // TODO: Should non-serving stations automatically be converted into servers?
                // If so, should the user be informed about the change
                stationTypes[st] = ExactConstants.STATION_LI;
            }

            // Sets service time for each class
            for (int cl = 0; cl < classNum; cl++) {
                Object serv = input.getServiceTimeDistribution(key, classKeys.get(cl));
                if (serv instanceof Distribution) {
                    Distribution d = (Distribution) serv;
                    serviceTimes[st][cl] = new double[1]; // This is not load dependent
                    if (d.hasMean()) {
                        serviceTimes[st][cl][0] = d.getMean();
                    } else {
                        serviceTimes[st][cl][0] = 1;
                        warnings.add("Service time distribution for " + output.getClassNames()[cl] + " at " + output.getStationNames()[st]
                                + " does not have a valid mean value. Service time is set to default value 1.");
                    }
                } else if (serv instanceof ZeroStrategy) {
                    serviceTimes[st][cl] = new double[1];
                    serviceTimes[st][cl][0] = 0;
                } else if (serv instanceof DisabledStrategy) {
                    serviceTimes[st][cl] = new double[1];
                    serviceTimes[st][cl][0] = 0;
                } else if (serv instanceof LDStrategy) {
                    LDStrategy lds = (LDStrategy) serv;
                    if (output.isClosed() && output.isSingleClass()) {
                        int pop = input.getClassPopulation(classKeys.get(cl)).intValue();
                        serviceTimes[st][cl] = new double[pop]; // This is load dependent
                        stationTypes[st] = ExactConstants.STATION_LD;
                        for (int i = 0; i < pop; i++) {
                            serviceTimes[st][cl][i] = lds.getMeanValue(i);
                        }
                    } else {
                        serviceTimes[st][cl] = new double[1]; // This is not load dependent
                        serviceTimes[st][cl][0] = lds.getMeanValue(1);
                        warnings.add("LD stations are supported only if the model has a single closed class. "
                                + stationNames[st] + " is converted to a LI station.");
                    }
                }
            }
        }
        // Sets extracted values to output
        output.setStationNames(stationNames);
        output.setStationServers(stationServers);
        output.setStationTypes(stationTypes);
        output.setServiceTimes(serviceTimes);
    }

    /**
     * This method will return a reachability vector for given station and given class
     * @param stationKey search's key for given station
     * @param classKey search's key for given class
     * @param model model data structure
     * @param stations vector with ordered station keys (the same order is used in output array)
     * @param warnings vector Vector to store warnings found during computation
     * @return an array with probability to reach each other station starting from given station
     */
    private static double[] getRoutingProbability(Object stationKey, Object classKey, CommonModel model, List<Object> stations, List<String> warnings) {
        double[] p = new double[stations.size()];
        RoutingStrategy strategy = (RoutingStrategy) model.getRoutingStrategy(stationKey, classKey);
        String type = model.getStationType(stationKey);
        if (type.equals(CommonConstants.STATION_TYPE_FORK)
                || type.equals(CommonConstants.STATION_TYPE_JOIN)
                || type.equals(CommonConstants.STATION_TYPE_LOGGER)
                || type.equals(CommonConstants.STATION_TYPE_CLASSSWITCH)
                || type.equals(CommonConstants.STATION_TYPE_SEMAPHORE)
                || type.equals(CommonConstants.STATION_TYPE_SCALER)
                || type.equals(CommonConstants.STATION_TYPE_PLACE)
                || type.equals(CommonConstants.STATION_TYPE_TRANSITION)) {
            warnings.add("Station type of " + model.getStationName(stationKey) + " is " + type
                    + ", which is considered as Router in JMVA.");
        }
        if (strategy instanceof ProbabilityRouting) {
            Map<Object, Double> routingMap = ((ProbabilityRouting) strategy).getValues();
            Iterator<Object> it = routingMap.keySet().iterator();
            while (it.hasNext()) {
                Object dest = it.next();
                if (stations.lastIndexOf(dest) >= 0) {
                    p[stations.lastIndexOf(dest)] = (routingMap.get(dest)).doubleValue();
                }
            }
        } else {
            if (type.equals(CommonConstants.STATION_TYPE_FORK)
                    || type.equals(CommonConstants.STATION_TYPE_SCALER)
                    || type.equals(CommonConstants.STATION_TYPE_PLACE)
                    || type.equals(CommonConstants.STATION_TYPE_TRANSITION)) {
                strategy = new RandomRouting();
            }
            if (!(strategy instanceof RandomRouting)) {
                warnings.add("Routing strategy for " + model.getClassName(classKey) + " at " + model.getStationName(stationKey) + " is " + strategy.getName()
                        + ", which is considered as Random in JMVA.");
            }

            Vector<Object> links = model.getForwardConnections(stationKey);
            int linksNum = links.size();
            // Now ignores sinks for closed classes
            if (model.getClassType(classKey) == CommonConstants.CLASS_TYPE_CLOSED) {
                for (int i = 0; i < links.size(); i++) {
                    if (model.getStationType(links.get(i)).equals(CommonConstants.STATION_TYPE_SINK)) {
                        linksNum--;
                    }
                }
            }

            double weight = 1.0 / linksNum;
            for (int i = 0; i < links.size(); i++) {
                if (stations.contains(links.get(i))) {
                    p[stations.lastIndexOf(links.get(i))] = weight;
                }
            }
        }

        return p;
    }

    /**
     * Builds a routing probability matrix for a given set of stations
     * @param stations stations to be considered
     * @param model data structure
     * @param classKey search's key for target class
     * @param warnings Vector where computation warnings must be put
     * @return computed routing probability matrix
     */
    private static double[][] buildProbabilityMatrix(List<Object> stations, CommonModel model, Object classKey, List<String> warnings) {
        double[][] matrix = new double[stations.size()][stations.size()];
        double[] tmp;
        for (int i = 0; i < stations.size(); i++) {
            tmp = getRoutingProbability(stations.get(i), classKey, model, stations, warnings);
            System.arraycopy(tmp, 0, matrix[i], 0, tmp.length);
        }
        return matrix;
    }
}
