package jmt.gui.common.definitions.parametric;

import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.SimulationDefinition;
import jmt.gui.common.definitions.StationDefinition;

/**
 * <p>Title: NumberOfServersParametricAnalysis</p>
 * <p>Description: this class is used to describe a parametric analysis where the
 * varied parameter is the number of servers inside a station. It adds the <code >stationKey</code> field,
 * used to keep the key of the station whose number of servers will be varied. </p>
 *
 * @author Xinyu Gao
 *         Date: 20-June-2023
 */

public class NumberOfServersParametricAnalysis extends ParametricAnalysisDefinition {
    private final int FROM_ALL = 100;
    private final int TO_ALL = 120;
    private final int INCREMENT_SINGLE = 4;
    private final int STEPS = 10;  //must be < than ParametricAnalysis.MAX_NUMBER_OF_STEPS

    private Object stationKey;
    private Object serverTypeKey;
    private Object values;

    public NumberOfServersParametricAnalysis(ClassDefinition cd, StationDefinition sd, SimulationDefinition simd) {
        type = PA_TYPE_NUMBER_OF_SERVERS;
        classDef = cd;
        stationDef = sd;
        simDef = simd;
        ParametricAnalysisChecker checker = new ParametricAnalysisChecker(cd, sd, simd);
        stationKey = checker.checkForNumberOfServersParametricAnalysisAvailableStations().get(0);
        double mean = stationDef.getStationNumberOfServers(stationKey).doubleValue();
        if (!isSingleServerType()) {
            serverTypeKey = stationDef.getServerTypes(stationKey).get(0).getServerKey();
            mean = stationDef.getServerType(serverTypeKey).getNumOfServers();
        }
        initialValue = mean;
        finalValue = mean * INCREMENT_SINGLE;
    }

    /**
     * Sets the default initial Value
     */
    public void setDefaultInitialValue() {
        double mean = stationDef.getStationNumberOfServers(stationKey).doubleValue();
        if (!isSingleServerType()) {
            if (serverTypeKey == null) {
                serverTypeKey = stationDef.getServerTypes(stationKey).get(0).getServerKey();
            }
            mean = stationDef.getServerType(serverTypeKey).getNumOfServers();
        }
        initialValue = mean;
    }

    /**
     * Sets default final value
     */
    public void setDefaultFinalValue() {
        double mean = stationDef.getStationNumberOfServers(stationKey).doubleValue();
        if (!isSingleServerType()) {
            if (serverTypeKey == null) {
                serverTypeKey = stationDef.getServerTypes(stationKey).get(0).getServerKey();
            }
            mean = stationDef.getServerType(serverTypeKey).getNumOfServers();
        }
        finalValue = mean * INCREMENT_SINGLE;
    }

    /**
     * Gets the class key of the job class whose number of servers will be
     * increased. If the simulation is not single class, the <code> null </code>
     * value will be returned
     * @return the key of the class whose number of servers will be increased if the
     *         parametric analysis is single class, <code> null </code> otherwise.
     */
    @Override
    public Object getReferenceClass() {
        return null;
    }

    /**
     * Get the reference class name
     *
     * @return the name of the class
     */
    @Override
    public String getReferenceClassName() {
        return null;
    }

    /**
     * Gets a TreeMap containing for each property its value. The supported properties are
     * defined as constants inside this class.
     * @return a TreeMap containing the value for each property
     */
    @Override
    public Map<String, String> getProperties() {
        TreeMap<String, String> properties = new TreeMap<String, String>();
        properties.put(FROM_PROPERTY, Double.toString(initialValue));
        properties.put(TO_PROPERTY, Double.toString(finalValue));
        properties.put(STEPS_PROPERTY, Integer.toString(numberOfSteps));
        properties.put(REFERENCE_STATION_PROPERTY, stationDef.getStationName(stationKey));
        if (serverTypeKey != null) {
            properties.put(SERVER_TYPE_PROPERTY, stationDef.getServerType(serverTypeKey).getName());
        }
        return properties;
    }

    /**
     * Sets the value for the specified property. The supported properties are: <br>
     * - FROM_PROPERTY <br>
     * - TO_PROPERTY  <br>
     * - STEPS_PROPERTY <br>
     * - REFERENCE_STATION_PROPERTY <br>
     * @param propertyName the name of the property to be set
     * @param value the value to be set
     */
    @Override
    public void setProperty(String propertyName, String value) {
        if(propertyName.equals(FROM_PROPERTY)) {
            initialValue = Double.parseDouble(value);
        } else if (propertyName.equals(TO_PROPERTY)) {
            finalValue = Double.parseDouble(value);
        } else if (propertyName.equals(STEPS_PROPERTY)) {
            numberOfSteps = Integer.parseInt(value);
            if (numberOfSteps > MAX_STEP_NUMBER) {
                numberOfSteps = MAX_STEP_NUMBER;
            }
        } else if (propertyName.equals(REFERENCE_STATION_PROPERTY)) {
            stationKey = stationDef.getStationByName(value);
        } else if (propertyName.equals(SERVER_TYPE_PROPERTY)) {
            serverTypeKey = stationDef.getServerTypeKey(stationKey, value);
        }
    }

    /**
     * Gets the station key whose number of servers will be varied
     * @return the key of the station whose number of servers will be varied
     */
    public Object getReferenceStation() {
        return stationKey;
    }

    /**
     * Gets name of the station whose number of servers will be varied
     * @return the key of the station whose number of servers will be varied
     */
    public String getReferenceStationName() {
        return stationDef.getStationName(stationKey);
    }

    /**
     * Sets the station whose number of servers will be varied
     * @param stationKey the station whose number of servers will be varied
     */
    public void setReferenceStation(Object stationKey) {
        this.stationKey = stationKey;
    }

    /**
     * Gets the type of parametric analysis
     *
     * @return the type of parametric analysis
     */
    @Override
    public String getType() {
        return type;
    }

    /**
     * Changes the model preparing it for the next step
     *
     */
    @Override
    public void changeModel(int step) {
        if (step >= numberOfSteps) {
            return;
        }
        if (values != null) {
            Double refST = (Double) ((Vector) values).get(step);
            if (serverTypeKey == null) {
                serverTypeKey = stationDef.getServerTypes(stationKey).get(0).getServerKey();
            }
            int diff = refST.intValue() - stationDef.getServerType(serverTypeKey).getNumOfServers();
            stationDef.getServerType(serverTypeKey).setNumOfServers(refST.intValue());
            stationDef.setStationNumberOfServers(stationKey, stationDef.getStationNumberOfServers(stationKey) + diff);
        }
    }

    /**
     * Gets the maximum number of steps compatible with the model definition and the type of parametric analysis.
     *
     * @return the maximum number of steps
     */
    @Override
    public int searchForAvailableSteps() {
        return Integer.MAX_VALUE;
    }

    /**
     * Finds the set of possible values of the parameter on which the
     * simulation may be iterated on.
     *
     */
    @Override
    public void createValuesSet() {
        double sum = 0;
        double increment = (finalValue - initialValue) / (double) (numberOfSteps - 1);
        values = new Vector(numberOfSteps);
        for (int i = 0; i < numberOfSteps; i++) {
            double value = initialValue + sum;
            double roundedValue = (double) Math.round(value);
            if (!((Vector<Double>) values).contains(roundedValue)) {
                ((Vector<Double>) values).add(new Double(roundedValue));
            }
            sum += increment;  //note that the increment may be < 0
        }
        originalValues = new Double(initialValue);
    }

    /**
     * Restore the original values of number of servers
     */
    @Override
    public void restoreOriginalValues() {
        if (originalValues != null) {
            Double mean = (Double) originalValues;
            if (serverTypeKey == null) {
                serverTypeKey = stationDef.getServerTypes(stationKey).get(0).getServerKey();
            }
            int diff = mean.intValue() - stationDef.getServerType(serverTypeKey).getNumOfServers();
            stationDef.getServerType(serverTypeKey).setNumOfServers(mean.intValue());
            stationDef.setStationNumberOfServers(stationKey, stationDef.getStationNumberOfServers(stationKey) + diff);
        }
    }

    /**
     * Checks if the PA model is still coherent with simulation model definition. If
     * the <code>autocorrect</code> variable is set to true, if the PA model is no more
     * valid but it can be corrected it will be changed.
     *
     * @param autocorrect if true the PA model will be autocorrected
     *
     * @return 0 - If the PA model is still valid <br>
     *         1 - If the PA model is no more valid, but it will be corrected <br>
     *         2 - If the PA model can be no more used
     */
    @Override
    public int checkCorrectness(boolean autocorrect) {
        int code = 0;
        ParametricAnalysisChecker checker = new ParametricAnalysisChecker(classDef, stationDef, simDef);
        Vector<Object> availableStations = checker.checkForNumberOfServersParametricAnalysisAvailableStations();
        if (availableStations.isEmpty()) {
            code = 2; // -> This type of PA is not available
        } else {
            //if the reference station is no more available change reference station
            if (!availableStations.contains(stationKey)) {
                code = 1;
                if (autocorrect) {
                    stationKey = availableStations.get(0);
                    setDefaultInitialValue();
                    setDefaultFinalValue();
                }
            }
        }
        return code;
    }

    /**
     * Returns the values assumed by the varying parameter
     *
     * @return a Vector containing the values assumed by the varying parameter
     */
    @Override
    public Vector<Number> getParameterValues() {
        return (Vector<Number>) values;
    }

    public Object getServerTypeKey() {
        return serverTypeKey;
    }

    public Boolean isSingleServerType() {
        return stationDef.getServerTypeKeys().size() <= 1;
    }

    public String[] getServerTypeNames() {
        return stationDef.getServerTypeNames(stationKey).toArray(new String[0]);
    }

    public void setServerType(String serverTypeName) {
        this.serverTypeKey = stationDef.getServerTypeKey(stationKey, serverTypeName);
    }

    public Boolean serverTypeInStation() {
        return serverTypeKey != null && stationDef.getServerType(serverTypeKey).getStationKey() == stationKey;
    }
}


