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

package jmt.gui.common.definitions.parametric;

import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.SimulationDefinition;
import jmt.gui.common.definitions.StationDefinition;

/**
 * <p>Title: PopulationMixParametricAnalysis</p>
 * <p>Description: this class is used to describe a parametric analysis where
 * the varied parameter is the population mix between two closed classes.It
 * adds the <code >classKey</code> field used to keep the key of
 * the Job-Class whose service time will be varied. This type of parametric
 * simulation is available only when there are exactly two closed classes.</p>
 *
 * @author Francesco D'Aquino
 *         Date: 14-dic-2005
 *         Time: 11.51.51
 */
public class PopulationMixParametricAnalysis extends ParametricAnalysisDefinition {
	private final double FROM = 1;
	private final double TO = 0;

	private Object classKey;
	private Object otherClassKey;
	private int popValue;
	private ValuesTable values;

	public PopulationMixParametricAnalysis(ClassDefinition cd, StationDefinition sd, SimulationDefinition simd) {
		type = PA_TYPE_POPULATION_MIX;
		classDef = cd;
		stationDef = sd;
		simDef = simd;
		classKey = cd.getClosedClassKeys().get(0);
		otherClassKey = cd.getClosedClassKeys().get(1);
		setDefaultInitialValue();
		setDefaultFinalValue();
		popValue = cd.getTotalClosedClassPopulation();
		numberOfSteps = searchForAvailableSteps();
		if (numberOfSteps > MAX_STEP_NUMBER) {
			numberOfSteps = MAX_STEP_NUMBER;
		}
	}

	/**
	 * Gets the class key of the job class whose number of jobs will be
	 * increased.
	 * @return the key of the class whose number of jobs will be increased
	 */
	@Override
	public Object getReferenceClass() {
		return classKey;
	}

	/**
	 * Sets the class whose number of jobs will be increased.
	 * @param classKey the key of the class whose number of job will be
	 *        increased
	 */
	public void setReferenceClass(Object classKey) {
		if (this.classKey != classKey) {
			simDef.setSaveChanged();
		}
		this.classKey = classKey;
	}

	/**
	 * Sets default initial value
	 */
	public void setDefaultInitialValue() {
		double pop = classDef.getClassPopulation(classKey).doubleValue();
		double totalPop = classDef.getTotalClosedClassPopulation();
		initialValue = 0;
		if (totalPop > 0) {
			initialValue = pop / totalPop;
		}
	}

	/**
	 * Sets default final value
	 */
	public void setDefaultFinalValue() {
		finalValue = TO;
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
	 * returns the set of values that the varying parameter will assume
	 * @return a structure containing the set of values that the varying parameter will assume
	 */
	public Object getValuesSet() {
		return values;
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
			int refPop = (int) values.getValue(classKey, step);
			int otherClassPop = classDef.getTotalClosedClassPopulation() - refPop;
			classDef.setClassPopulation(classKey, new Integer(refPop));
			classDef.setClassPopulation(otherClassKey, new Integer(otherClassPop));
			simDef.manageJobs();
		}
	}

	/**
	 * Gets the maximum number of steps compatible with the model definition and the type of parametric analysis.
	 *
	 * @return the maximum number of steps
	 */
	@Override
	public int searchForAvailableSteps() {
		float diff = Math.abs((float) initialValue - (float) finalValue);
		int steps = ((int) (classDef.getTotalClosedClassPopulation() * diff)) + 1;
		return (steps < 2) ? 2 : steps;
	}

	/**
	 * Finds the set of possible values of the population on which the
	 * simulation may be iterated on.
	 *
	 */
	@Override
	public void createValuesSet() {
		Vector<Object> closeClasses = classDef.getClosedClassKeys();
		otherClassKey = (closeClasses.get(0) != classKey) ? closeClasses.get(0) : closeClasses.get(1);
		int totalPop = classDef.getTotalClosedClassPopulation();
		int initialClassPop = (int) Math.round(totalPop * initialValue);
		int initialOtherClassPop = totalPop - initialClassPop;
		values = new ValuesTable(classDef, closeClasses, numberOfSteps);
		double p = 0;
		if (totalPop > 0) {
			p = (finalValue - initialValue) / (numberOfSteps - 1) * totalPop;
		}
		double sum = 0;
		for (int i = 0; i < numberOfSteps; i++) {
			double increment = (int) Math.round(sum);
			double value = initialClassPop + increment;
			double otherValue = initialOtherClassPop - increment;
			values.setValue(classKey, value);
			values.setValue(otherClassKey, otherValue);
			sum += p;
		}
		originalValues = new Vector(2);
		int thisClassPop = classDef.getClassPopulation(classKey).intValue();
		int otherClassPop = totalPop - thisClassPop;
		((Vector<Integer>) originalValues).add(new Integer(thisClassPop));
		((Vector<Integer>) originalValues).add(new Integer(otherClassPop));
	}

	/**
	 * Restore the original values of population
	 */
	@Override
	public void restoreOriginalValues() {
		Vector vals = (Vector) originalValues;
		int thisClassPop = ((Integer) vals.get(0)).intValue();
		int otherClassPop = ((Integer) vals.get(1)).intValue();
		classDef.setClassPopulation(classKey, new Integer(thisClassPop));
		classDef.setClassPopulation(otherClassKey, new Integer(otherClassPop));
		simDef.manageJobs();
	}

	/**
	 * Checks if the PA model is still coherent with simulation model definition. If
	 * the <code>autocorrect</code> variable is set to true, if the PA model is no more
	 * valid but it can be corrected it will be changed.
	 *
	 * @param autocorrect if true the PA model will be autocorrected 
	 *
	 * @return 0 - If the PA model is still valid <br>
	 *          1 - If the PA model is no more valid, but it will be corrected <br>
	 *          2 - If the PA model can be no more used
	 */
	@Override
	public int checkCorrectness(boolean autocorrect) {
		int code = 0;
		Vector<Object> closeClasses = classDef.getClosedClassKeys();
		int totalPop = classDef.getTotalClosedClassPopulation();
		if (closeClasses.size() != 2 || totalPop < 1) {
			code = 2; //This PA model can be no more used
		} else {
			//if one of the two classes was changed..
			if (!closeClasses.contains(classKey) || !closeClasses.contains(otherClassKey)) {
				code = 1;
				if (autocorrect) {
					classKey = closeClasses.get(0);
					otherClassKey = closeClasses.get(1);
					setDefaultInitialValue();
					setDefaultFinalValue();
					numberOfSteps = searchForAvailableSteps();
					if (numberOfSteps > MAX_STEP_NUMBER) {
						numberOfSteps = MAX_STEP_NUMBER;
					}
				}
			} else {
				//else, if the total number of jobs has changed re - calculate the number of steps
				if (popValue != totalPop) {
					code = 1;
					if (autocorrect) {
						setDefaultInitialValue();
						setDefaultFinalValue();
						numberOfSteps = searchForAvailableSteps();
						if (numberOfSteps > MAX_STEP_NUMBER) {
							numberOfSteps = MAX_STEP_NUMBER;
						}
						popValue = totalPop;
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
		Vector<Number> assumedValues = new Vector<Number>(numberOfSteps);
		for (int i = 0; i < numberOfSteps; i++) {
			double tempThisClassPop = values.getValue(classKey, i);
			double tempOtherClassPop = values.getValue(otherClassKey, i);
			double sum = tempThisClassPop + tempOtherClassPop;
			double val = 0;
			if (sum > 0) {
				val = tempThisClassPop / sum;
			}
			assumedValues.add(new Double(val));
		}
		return assumedValues;
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
		properties.put(REFERENCE_CLASS_PROPERTY, classDef.getClassName(classKey));
		return properties;
	}

	/**
	 * Sets the value for the specified property
	 *
	 * @param propertyName the name of the property to be set. The supported properties are: <br>
	 * - FROM_PROPERTY  <br>
	 * - TO_PROPERTY  <br>
	 * - STEPS_PROPERTY <br>
	 * - REFERENCE_CLASS_PROPERTY
	 * @param value the value to be set
	 */
	@Override
	public void setProperty(String propertyName, String value) {
		if (propertyName.equals(FROM_PROPERTY)) {
			initialValue = Double.parseDouble(value);
		} else if (propertyName.equals(TO_PROPERTY)) {
			finalValue = Double.parseDouble(value);
		} else if (propertyName.equals(STEPS_PROPERTY)) {
			numberOfSteps = Integer.parseInt(value);
			if (numberOfSteps > MAX_STEP_NUMBER) {
				numberOfSteps = MAX_STEP_NUMBER;
			}
		} else if (propertyName.equals(REFERENCE_CLASS_PROPERTY)) {
			classKey = classDef.getClassByName(value);
			Vector<Object> closedClasses = classDef.getClosedClassKeys();
			otherClassKey = (closedClasses.get(0) != classKey) ? closedClasses.get(0) : closedClasses.get(1);
		}
	}
}
