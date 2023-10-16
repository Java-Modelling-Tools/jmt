package jmt.gui.common.definitions.parametric;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.*;
import javax.swing.JOptionPane;

import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.SimulationDefinition;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.routingStrategies.ProbabilityRouting;
import jmt.gui.common.routingStrategies.RoutingStrategy;

import javax.swing.*;
import javax.swing.JOptionPane;


/**
 * <p>Title: RoutingProbabilitiesParametricAnalysis</p>
 * <p>Description: this class is used to describe a parametric analysis where the
 * varied parameter is the value of routing probabilities of a class inside a station. It
 * adds the <code >classKey</code>, <code >stationKey</code>, and <code >destinationStationKey</code> fields, used
 * to keep the key of the Job-Class and the key of the station whose routing
 * probabilities will be varied, and a boolean value <code >singleClass</code> used to
 * choose the type of routing probability growth (single or all class).</p>
 *
 * @author Xinyu gao
 *         Date: 11-Aug-2023
 */
public class RoutingProbabilitiesParametricAnalysis extends ParametricAnalysisDefinition {
    private final double FROM_ALL = 0.2;
    private final double TO_ALL = 0.6;
    private final double INCREMENT_SINGLE = 4;
    private final int STEPS = 10; //must be < than ParametricAnalysis.MAX_NUMBER_OF_STEPS
    private final boolean SINGLE_CLASS = false;

    private boolean singleClass;
    private Object classKey;
    private Object stationKey;
    private Object destinationStationKey;
    private Vector<Object> availableClasses;
    private List<List<Object>> dataList = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(RoutingProbabilitiesParametricAnalysis.class.getName());

    private Object values;

    public RoutingProbabilitiesParametricAnalysis(ClassDefinition cd, StationDefinition sd, SimulationDefinition simd) {
        type = PA_TYPE_ROUTING_PROBABILITY;
        classDef = cd;
        stationDef = sd;
        simDef = simd;
        numberOfSteps = STEPS;
        ParametricAnalysisChecker checker = new ParametricAnalysisChecker(cd, sd, simd);
        stationKey = checker.checkForRoutingProbabilitiesParametricAnalysisAvailableSourceStations().get(0);
        Vector<Object> currentAvailableDestStations = stationDef.getForwardConnections(stationKey);
        destinationStationKey = currentAvailableDestStations.get(0);
        Vector<Object> available = checker.checkForRoutingProbabilitiesParametricSimulationAvailableClasses(stationKey);
        if (cd.getClassKeys().size() == 1 || available.size() < cd.getClassKeys().size()) {
            singleClass = true;
            classKey = available.get(0);
            double mean = 0.1;
            initialValue = mean;
            if (mean * INCREMENT_SINGLE <= 1.0) {
                finalValue = mean * INCREMENT_SINGLE;
            } else {
                finalValue = 1.0;
            }
        } else {
            singleClass = SINGLE_CLASS;
            initialValue = FROM_ALL;
            finalValue = TO_ALL;
        }
    }

    /**
     * Returns true if only the routign probability of one class will be increased
     * @return true if only the routign probability of one class will be increased
     */
    public boolean isSingleClass() {
        return singleClass;
    }

    /**
     * Sets the type of population increase. If <code> isSingleClass</code>
     * param is true only the routign probability of one class will be increased
     * @param isSingleClass
     */
    public void setSingleClass(boolean isSingleClass) {
        if (isSingleClass != singleClass) {
            simDef.setSaveChanged();
        }
        singleClass = isSingleClass;
    }

    /**
     * Sets the default initial Value
     */
    public void setDefaultInitialValue() {
        if (singleClass) {
            double mean = 0.1;
            initialValue = mean;
        }
        else {
            initialValue = FROM_ALL;
        }
    }

    /**
     * Sets default final value
     */
    public void setDefaultFinalValue() {
        if (singleClass) {
            double mean = 0.1;
            initialValue = mean;
            finalValue = initialValue * INCREMENT_SINGLE;
        }
        else {
            finalValue = TO_ALL;
        }
    }

    /**
     * Gets the class key of the job class whose routign probability will be
     * increased. If the simulation is not single class, the <code> null </code>
     * value will be returned
     * @return the key of the class whose routign probability will be increased if the
     *         parametric analysis is single class, <code> null </code> otherwise.
     */
    @Override
    public Object getReferenceClass() {
        if (singleClass) {
            return classKey;
        } else {
            return null;
        }
    }

    /**
     * Get the reference class name
     *
     * @return the name of the class
     */
    @Override
    public String getReferenceClassName() {
        return classDef.getClassName(classKey);
    }

    /**
     * Gets a TreeMap containing for each property its value. The supported properties are
     * defined as constants inside this class.
     * @return a TreeMap containing the value for each property
     */
    @Override
    public Map<String, String> getProperties() {
        TreeMap<String, String> properties = new TreeMap<String, String>();
        properties.put(TYPE_PROPERTY, getType());
        properties.put(FROM_PROPERTY, Double.toString(initialValue));
        properties.put(TO_PROPERTY, Double.toString(finalValue));
        properties.put(STEPS_PROPERTY, Integer.toString(numberOfSteps));
        properties.put(IS_SINGLE_CLASS_PROPERTY, Boolean.toString(singleClass));
        properties.put(REFERENCE_STATION_PROPERTY, stationDef.getStationName(stationKey));
        properties.put(REFERENCE_DEST_STATION_PROPERTY, stationDef.getStationName(destinationStationKey));
        if (singleClass) {
            properties.put(REFERENCE_CLASS_PROPERTY, classDef.getClassName(classKey));
        }
        return properties;
    }

    /**
     * Sets the value for the specified property. The supported properties are: <br>
     * - FROM_PROPERTY  <br>
     * - TO_PROPERTY  <br>
     * - STEPS_PROPERTY <br>
     * - IS_SINGLE_CLASS_PROPERTY <br>
     * - REFERENCE_STATION_PROPERTY <br>
     * - REFERENCE_CLASS_PROPERTY <br>
     * - REFERENCE_DEST_STATION_PROPERTY
     * @param propertyName the name of the property to be set
     * @param value the value to be set
     */
    @Override
    public void setProperty(String propertyName, String value) {
        if (propertyName.equals(TO_PROPERTY)) {
            finalValue = Double.parseDouble(value);
        } else if (propertyName.equals(FROM_PROPERTY)) {
            initialValue = Double.parseDouble(value);
        } else if (propertyName.equals(STEPS_PROPERTY)) {
            numberOfSteps = Integer.parseInt(value);
            if (numberOfSteps > MAX_STEP_NUMBER) {
                numberOfSteps = MAX_STEP_NUMBER;
            }
        } else if (propertyName.equals(IS_SINGLE_CLASS_PROPERTY)) {
            singleClass = Boolean.valueOf(value).booleanValue();
        } else if (propertyName.equals(REFERENCE_STATION_PROPERTY)) {
            stationKey = stationDef.getStationByName(value);
        } else if (propertyName.equals(REFERENCE_DEST_STATION_PROPERTY)) {
            destinationStationKey = stationDef.getStationByName(value);
        } else if (propertyName.equals(REFERENCE_CLASS_PROPERTY)) {
            classKey = classDef.getClassByName(value);
        }
    }

    /**
     * Sets the class whose routing probability will be increased. If <code> singleClass </code>
     * value is not true nothing will be done
     * @param classKey the key of the class whose routing probability will be
     *        increased
     */
    public void setReferenceClass(Object classKey) {
        if (singleClass) {
            if (this.classKey != classKey) {
                simDef.setSaveChanged();
            }
            this.classKey = classKey;
        }
    }

    /**
     * Gets the station key whose routing probability will be varied
     * @return the key of the station whose service times will be varied
     */
    public Object getReferenceStation() { return stationKey; }

    /**
     * Gets name of the source station whose routing probability will be varied
     * @return the key of the source station whose routing probability will be varied
     */
    public String getReferenceStationName() {
        return stationDef.getStationName(stationKey);
    }

    /**
     * Sets the source station whose routing probability will be varied
     * @param stationKey the source station whose routing probability will be varied
     */
    public void setReferenceStation(Object stationKey) {
        this.stationKey = stationKey;
    }

    /**
     * Gets the destination station key whose routing probability will be varied
     * @return the key of the destination station whose service times will be varied
     */
    public Object getDestinationStationKey() {
        return destinationStationKey;
    }

    /**
     * Sets the destination station whose routing probability will be varied
     * @param stationKey the destination station whose routing probability will be varied
     */
    public void setDestinationStationKey(Object key) {
        this.destinationStationKey = key;
    }

    /**
     * Gets name of the destination station whose routing probability will be varied
     * @return the key of the destination station whose routing probability will be varied
     */
    public String getDestinationStationName() {
        return stationDef.getStationName(destinationStationKey);
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
     * Gets the decimals retained to four decimal places
     * @return the decimals retained to four decimal places
     */
    public static double roundDecimals(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    /**
     * Redistributes the routing probabilities from the source station to stations other than the destination station
     *
     */
    public void normalizeRoutingProbabilities(Object stationKey, Object classKey, Map<Object, Double> values, Object changedStationKey, double newProbability) {
        Vector<Object> outputs = stationDef.getForwardConnections(stationKey);

        double probToChangedStation = newProbability;
        double totalProb = 1.0 - probToChangedStation;
        double oldTotalProb = 0.0;

        for (Object key : outputs) {
            if (!key.equals(changedStationKey)) {
                oldTotalProb += values.get(key);
            }
        }

        if (oldTotalProb == 0 && values.get(changedStationKey) == 1.0) {
            // This warning may be useful but keeps popping up so it is disabled
            //JOptionPane.showMessageDialog(null, "Current station and class choice cannot be used to implement routing probability parametric analysis.", "Warning", JOptionPane.WARNING_MESSAGE);
            logger.log(Level.WARNING, "Current station and class choice cannot be used to implement routing probability parametric analysis.");
        } else {
            for (Object key : outputs) {
                if (!key.equals(changedStationKey)) {
                    double oldProb = values.get(key);
                    double newProb = totalProb * oldProb / oldTotalProb;
                    values.put(key, roundDecimals(newProb));
                } else {
                    values.put(key, roundDecimals(probToChangedStation));
                }
            }
        }
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
            if (singleClass) {
                Double refST = (Double) ((Vector) values).get(step);
                if (stationDef.getRoutingStrategy(stationKey, classKey) instanceof ProbabilityRouting) {
                    ProbabilityRouting pr = (ProbabilityRouting) stationDef.getRoutingStrategy(stationKey, classKey);
                    Map<Object, Double> routingValues = pr.getValues();
                    normalizeRoutingProbabilities(stationKey, classKey, routingValues, destinationStationKey, refST);

                    List<Object> row = new ArrayList<>();
                    row.add("Step = " + (step + 1));

                    Collection<Double> values = routingValues.values();
                    row.addAll(values);
                    dataList.add(row);
                }
            } else {
                for (int i = 0; i < availableClasses.size(); i++) {
                    Object thisClass = availableClasses.get(i);
                    Double refST = (Double) ((Vector) values).get(step);
                    ProbabilityRouting pr = (ProbabilityRouting) stationDef.getRoutingStrategy(stationKey, thisClass);
                    Map<Object, Double> routingValues = pr.getValues();
                    normalizeRoutingProbabilities(stationKey, thisClass, routingValues, destinationStationKey, refST);

                    List<Object> row = new ArrayList<>();
                    row.add("Step = " + (step + 1));

                    Collection<Double> values = routingValues.values();
                    row.addAll(values);
                    dataList.add(row);
                }
            }
        }
    }

    /**
     * Gets the names of destination stations
     * @return the names of destination stations
     */
    public Object[] getDestStationNames() {
        Vector<Object> destStations = stationDef.getForwardConnections(stationKey);
        String[] destStationNames = new String[destStations.size() + 1];
        destStationNames[0] = " ";
        for (int i = 1; i < destStationNames.length; i++) {
            destStationNames[i] = stationDef.getStationName(destStations.get(i-1));
        }
        return destStationNames;
    }

    public Object[][] getTableData() {
        Object[][] data = new Object[dataList.size()][];
        for (int i = 0; i < dataList.size(); i++) {
            List<Object> currentRow = dataList.get(i);
            data[i] = currentRow.toArray(new Object[0]);
        }
        return data;
    }

    /**
     * Gets the routing probability table
     * @return the table that contains different routing probabilities
     */
    public Object[][] getRoutingProbabilityTable() {
        List<List<Object>> probabilityList = new ArrayList<>();
        Object values_rp;
        Object originalValues_rp;

        if (singleClass) {
            double sum = 0;
            double increment = (finalValue - initialValue) / ((numberOfSteps - 1));
            values_rp = new Vector(numberOfSteps);
            for (int i = 0; i < numberOfSteps; i++) {
                double value = initialValue + sum;
                ((Vector<Double>) values_rp).add(new Double(value));
                sum += increment; //note that the increment may be < 0
            }
            originalValues_rp = new Double(initialValue);
            for (int i = 0; i < numberOfSteps; i++) {
                Double refST = (Double) ((Vector) values_rp).get(i);
                if (stationDef.getRoutingStrategy(stationKey, classKey) instanceof ProbabilityRouting) {
                    ProbabilityRouting pr = (ProbabilityRouting) stationDef.getRoutingStrategy(stationKey, classKey);
                    Map<Object, Double> routingValues = pr.getValues();
                    normalizeRoutingProbabilities(stationKey, classKey, routingValues, destinationStationKey, refST);

                    List<Object> row = new ArrayList<>();
                    row.add("Step = " + (i + 1));

                    Collection<Double> values = routingValues.values();
                    row.addAll(values);
                    probabilityList.add(row);
                }
            }
        } else {
            double sum = 0;
            double increment = (finalValue - initialValue) / ((numberOfSteps - 1));
            values_rp = new Vector(numberOfSteps);
            for (int i = 0; i < numberOfSteps; i++) {
                double value = initialValue + sum;
                ((Vector<Double>) values_rp).add(new Double(value));
                sum += increment; //note that the increment may be < 0
            }

            Vector<Object> allClasses_rp = classDef.getClassKeys();
            Vector<Object> availableClasses_rp = new Vector<Object>(0, 1);
            for (int i = 0; i < allClasses_rp.size(); i++) {
                Object thisClass = allClasses_rp.get(i);
                if (stationDef.getRoutingStrategy(stationKey, thisClass) instanceof ProbabilityRouting) {
                    ProbabilityRouting pr = (ProbabilityRouting) stationDef.getRoutingStrategy(stationKey, thisClass);
                    Map<Object, Double> routingValues = pr.getValues();
                    if(!routingValues.isEmpty()) {
                        availableClasses_rp.add(thisClass);
                    }
                }
            }

            originalValues_rp = new Vector(availableClasses_rp.size());
            for (int i = 0; i < availableClasses_rp.size(); i++) {
                Object thisClass = availableClasses_rp.get(i);
                ProbabilityRouting pr = (ProbabilityRouting) stationDef.getRoutingStrategy(stationKey, thisClass);
                Map<Object, Double> routingValues = pr.getValues();
                double thisRoutingProbability = routingValues.get(destinationStationKey);
                ((Vector<Double>) originalValues_rp).add(new Double(thisRoutingProbability));
            }

            for (int m = 0; m < numberOfSteps; m++) {
                for (int i = 0; i < availableClasses_rp.size(); i++) {
                    Object thisClass = availableClasses_rp.get(i);
                    Double refST = (Double) ((Vector) values_rp).get(m);
                    ProbabilityRouting pr = (ProbabilityRouting) stationDef.getRoutingStrategy(stationKey, thisClass);
                    Map<Object, Double> routingValues = pr.getValues();
                    normalizeRoutingProbabilities(stationKey, thisClass, routingValues, destinationStationKey, refST);

                    List<Object> row = new ArrayList<>();
                    row.add("Step = " + (m + 1));

                    Collection<Double> values = routingValues.values();
                    row.addAll(values);
                    probabilityList.add(row);
                }
            }
        }
        Object[][] routingProbabilityTable = new Object[probabilityList.size()][];
        for (int i = 0; i < probabilityList.size(); i++) {
            List<Object> currentRow = probabilityList.get(i);
            routingProbabilityTable[i] = currentRow.toArray(new Object[0]);
        }
        return routingProbabilityTable;
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
        double increment = (finalValue - initialValue) / ((numberOfSteps - 1));
        values = new Vector(numberOfSteps);
        for (int i = 0; i < numberOfSteps; i++) {
            double value = initialValue + sum;
            ((Vector<Double>) values).add(new Double(value));
            sum += increment; //note that the increment may be < 0
        }

        if (singleClass) {
            originalValues = new Double(initialValue);
        } else {
            //find the set of available classes
            Vector<Object> allClasses = classDef.getClassKeys();
            availableClasses = new Vector<Object>(0, 1);
            for (int i = 0; i < allClasses.size(); i++) {
                Object thisClass = allClasses.get(i);
                if (stationDef.getRoutingStrategy(stationKey, thisClass) instanceof ProbabilityRouting) {
                    ProbabilityRouting pr = (ProbabilityRouting) stationDef.getRoutingStrategy(stationKey, thisClass);
                    Map<Object, Double> routingValues = pr.getValues();
                    if(!routingValues.isEmpty()) {
                        availableClasses.add(thisClass);
                    }
                }
            }
            originalValues = new Vector(availableClasses.size());
            for (int i = 0; i < availableClasses.size(); i++) {
                Object thisClass = availableClasses.get(i);
                ProbabilityRouting pr = (ProbabilityRouting) stationDef.getRoutingStrategy(stationKey, thisClass);
                Map<Object, Double> routingValues = pr.getValues();
                double thisRoutingProbability = routingValues.get(destinationStationKey);
                ((Vector<Double>) originalValues).add(new Double(thisRoutingProbability));
            }
        }
    }

    /**
     * Restore the original values of routing probabilities
     */
    @Override
    public void restoreOriginalValues() {
        if (originalValues != null) {
            if (singleClass) {
                ProbabilityRouting pr = (ProbabilityRouting) stationDef.getRoutingStrategy(stationKey, classKey);
                Map<Object, Double> routingValues = pr.getValues();
                Double mean = (Double) originalValues;
                routingValues.put(destinationStationKey, mean);
            } else {
                Vector values = (Vector) originalValues;
                for (int i = 0; i < availableClasses.size(); i++) {
                    Object thisClass = availableClasses.get(i);
                    ProbabilityRouting pr = (ProbabilityRouting) stationDef.getRoutingStrategy(stationKey, thisClass);
                    Map<Object, Double> routingValues = pr.getValues();
                    Double thisValue = (Double) values.get(i);
                    routingValues.put(destinationStationKey, thisValue);
                }
            }
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
        Vector<Object> classes = classDef.getClassKeys();
        ParametricAnalysisChecker checker = new ParametricAnalysisChecker(classDef, stationDef, simDef);
        Vector<Object> availableStations = checker.checkForRoutingProbabilitiesParametricAnalysisAvailableSourceStations();
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
            Vector<Object> availableClasses = checker.checkForRoutingProbabilitiesParametricSimulationAvailableClasses(stationKey);
            //if is single class...
            if (isSingleClass()) {
                // ... and the selected closed class is no more available
                if (!availableClasses.contains(classKey)) {
                    code = 1;
                    if (autocorrect) {
                        classKey = availableClasses.get(0); //change the reference class
                        setDefaultInitialValue();
                        setDefaultFinalValue();
                    }
                }
            }
            //all class case...
            else {
                if (classes.size() == 1 || availableClasses.size() < classes.size()) {
                    code = 1;
                    if (autocorrect) {
                        singleClass = true;
                        classKey = availableClasses.get(0);
                        setDefaultInitialValue();
                        setDefaultFinalValue();
                    }
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
}
