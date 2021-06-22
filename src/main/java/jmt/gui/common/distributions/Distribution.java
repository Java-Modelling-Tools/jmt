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

package jmt.gui.common.distributions;

import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;

import javax.swing.ImageIcon;

import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceParameter;
import jmt.gui.common.serviceStrategies.ServiceStrategy;

/**
 * <p>Title: Distribution</p>
 * <p>Description: This abstract class provides a generic pattern used to specify a
 * distribution.</p>
 * 
 * @author Bertoli Marco
 *         Date: 25-giu-2005
 *         Time: 14.47.12
 */
public abstract class Distribution implements ServiceStrategy {

	protected static Distribution[] all = null; // Used to store all distributions
	protected static Distribution[] allWithMean = null; // Used to store all distributions with Mean value adjustable
	protected static Distribution[] nestableDistributions = null; // Used to store nestable distributions
	protected static DecimalFormat df = new DecimalFormat();

	static {
		df.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.ENGLISH));
	}

	protected String classpath;
	protected String name;
	protected String parameterClasspath;
	protected Parameter[] parameters;
	protected String description;
	protected ImageIcon image;
	protected ValueChecker checker;
	protected String precondition;
	protected boolean hasMean; // Used to provide input parameters with the tuple (mean, C)
	protected boolean hasC;
	protected double mean;
	protected double c;
	protected boolean isNestable;

	/**
	 * Constructs a new Distribution object. Initialize all internal objects calling abstract
	 * methods setParameterNames(), setParameterClasses(), setParameterValues().
	 * @param name Name of the distribution
	 * @param classpath engine's classpath for this distribution
	 * @param parameterClasspath engine's classpath for this distribution's parameters
	 * @param description description of this distribution
	 */
	public Distribution(String name, String classpath, String parameterClasspath, String description) {
		this.classpath = classpath;
		this.name = name;
		this.parameterClasspath = parameterClasspath;
		this.description = description;
		this.parameters = setParameters();
		this.image = setImage();
		updateCM();
	}

	/**
	 * Gets engine's classpath for this distribution
	 * @return classpath
	 */
	public String getClassPath() {
		return classpath;
	}

	/**
	 * Gets engine's classpath for this distribution's parameters (This is needed as each
	 * distribution in the engine has a Distribution object and a Parameter object which
	 * are distinct classes)
	 * @return parameter's classpath
	 */
	public String getParameterClassPath() {
		return parameterClasspath;
	}

	/**
	 * Gets this distribution's name
	 * @return distribution's name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets this distribution description
	 * @return distribution's description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns number of parameters for this distribution
	 * @return number of parameters
	 */
	public int getNumberOfParameters() {
		return parameters.length;
	}

	/**
	 * Gets a parameter data structure of this distribution, given its index
	 * @param num index of requested parameter
	 * @return requested parameter
	 * @throws java.lang.ArrayIndexOutOfBoundsException if num is not inside bounds
	 */
	public Parameter getParameter(int num) {
		return parameters[num];
	}

	/**
	 * Gets a parameter data structure of this distribution, given its name
	 * @param name name of parameter to be retrieved
	 * @return requested parameter or null if a parameter with specified name does not exist
	 */
	public Parameter getParameter(String name) {
		for (Parameter parameter : parameters) {
			if (parameter.getName().equals(name)) {
				return parameter;
			}
		}
		return null;
	}

	/**
	 * Gets explicative image of this distribution used, together with description, to help the
	 * user to understand meaning of parameters.
	 * @return explicative image
	 */
	public ImageIcon getImage() {
		return image;
	}

	/**
	 * Checks if all parameters' values are correct for this distribution type
	 * @return true if no check has to be performed or parameters are correct, false otherwise.
	 */
	public boolean checkValue() {
		if (checker != null) {
			return checker.checkValue(this);
		} else {
			return true;
		}
	}

	/**
	 * Returns precondition that parameters' values must satisfy for this distribution to be valid
	 * Every Distribution that provides a checker must provide a valid precondition message too.
	 * @return Message describing distribution's preconditions
	 */
	public String getPrecondition() {
		return precondition;
	}

	/**
	 * Returns a deep copy of this Distribution
	 * @return a clone of this distribution
	 */
	@Override
	public Distribution clone() {
		// Gets subtype of this class to instantiate new object through reflection
		Class<? extends Distribution> instance = this.getClass();
		Distribution tmp = null;
		try {
			tmp = instance.newInstance();
			// Newly created instance will have default parameters. Now will clone parameters
			// of this and sets them as parameters of new instance.
			Parameter[] parameters = new Parameter[this.parameters.length];
			for (int i = 0; i < this.parameters.length; i++) {
				parameters[i] = (Parameter) this.parameters[i].clone();
			}
			tmp.parameters = parameters;
			tmp.mean = mean;
			tmp.c = c;
		} catch (IllegalAccessException e) {
			System.err.println("Error: Cannot clone Distribution object: cannot access to correct class");
			e.printStackTrace();
		} catch (InstantiationException e) {
			System.err.println("Error: Cannot clone Distribution object: instantiation problem during reflection");
			e.printStackTrace();
		}
		return tmp;
	}

	/**
	 * Returns if this distribution can be initialized providing its mean value
	 * @return true iff this distribution can be initialized using its mean value
	 */
	public boolean hasMean() {
		return hasMean;
	}

	/**
	 * Returns if this distribution can be initialized providing variation coefficient C
	 * @return true iff this distribution can be initialized using variation coefficient C
	 */
	public boolean hasC() {
		return hasC;
	}

	/**
	 * Returns distribution mean only if <code>hasMean()</code> is true
	 * @return distribution Mean
	 */
	public double getMean() {
		return mean;
	}

	/**
	 * Returns distribution variation coefficient C only if <code>hasC()</code> is true
	 * @return variation coefficient C
	 */
	public double getC() {
		return c;
	}

	/**
	 * Sets a value checker for this entire distribution (checks intra-parameters preconditions)
	 * @param checker checker to be set to this distribution
	 */
	protected void setValueChecker(ValueChecker checker) {
		this.checker = checker;
	}

	/**
	 * Returns whether a distribution can be used as nested distribution inside another distribution
	 * @return true if it can be nested. false otherwise
	 */
	public boolean isNestable() {
		return isNestable;
	}

	/**
	 * Helper method used to format given number into string according to default rules.
	 * @param d double to be converted
	 * @return string representation of given number
	 */
	public String formatNumber(double d) {
		if (d == 0.0 || (Math.abs(d) >= 1e-3 && Math.abs(d) < 1e3)) {
			df.applyPattern("#.###");
			return df.format(d);
		} else {
			df.applyPattern("0.00E00");
			return df.format(d);
		}
	}

	/**
	 * Returns an array with an instance of every allowed Distribution. Uses internal
	 * caching to search for distributions only the first time that this method is called.
	 * @return an array with an instance of every allowed Distribution
	 */
	public static Distribution[] findAll() {
		if (all != null) {
			return all;
		}
		String path = "jmt.gui.common.distributions.";
		Field[] fields = jmt.gui.common.CommonConstants.class.getFields();
		ArrayList<Distribution> tmp = new ArrayList<Distribution>();
		try {
			for (Field field : fields) {
				if (field.getName().startsWith("DISTRIBUTION_")) {
					tmp.add((Distribution) Class.forName(path + (String) field.get(null)).newInstance());
				}
			}
		} catch (IllegalAccessException ex) {
			System.err.println("A security manager has blocked reflection");
			ex.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.err.println("Reflection Error: cannot access to distribution's implementation");
			e.printStackTrace();
		} catch (InstantiationException e) {
			System.err.println("Reflection Error: Cannot instantiate a distribution");
			e.printStackTrace();
		}
		all = new Distribution[tmp.size()];
		for (int i = 0; i < tmp.size(); i++) {
			all[i] = tmp.get(i);
		}
		return all;
	}

	/**
	 * Returns an array with an instance of every Distribution that allows mean setting. Uses internal
	 * caching to search for distributions only the first time that this method is called.
	 * @return an array with an instance of every Distribution in which mean value can be specified
	 */
	public static Distribution[] findAllWithMean() {
		if (allWithMean != null) {
			return allWithMean;
		}
		Distribution[] all = findAll();
		Distribution[] tmp = new Distribution[all.length];
		int n = 0;
		for (Distribution element : all) {
			if (element.hasMean()) {
				tmp[n++] = element;
			}
		}
		// Now removes empty elements into tmp array
		allWithMean = new Distribution[n];
		for (int i = 0; i < n; i++) {
			allWithMean[i] = tmp[i];
		}
		return allWithMean;
	}

	/**
	 * Returns an array with an instance of every nestable Distribution. Uses internal
	 * caching to search for distributions only the first time that this method is called.
	 * @return an array with an instance of every nestable Distribution
	 */
	public static Distribution[] findNestableDistributions() {
		if (nestableDistributions != null) {
			return nestableDistributions;
		}
		Distribution[] all = findAll();
		Distribution[] tmp = new Distribution[all.length];
		int n = 0;
		for (Distribution element : all) {
			if (element.isNestable()) {
				tmp[n++] = element;
			}
		}
		// Now removes empty elements into tmp array
		nestableDistributions = new Distribution[n];
		for (int i = 0; i < n; i++) {
			nestableDistributions[i] = tmp[i];
		}
		return nestableDistributions;
	}

	/**
	 * Checks if the entries of an probability vector sum up to one.
	 * @param v vector to be checked
	 * @return true if the vector satisfies the condition, false otherwise
	 */
	public boolean checkProbabilityVector(Object[][] v) {
		double sum = 0.0;
		for (int i = 0; i < v[0].length; i++) {
			Double d = (Double) v[0][i];
			sum += d.doubleValue();
		}
		if (Math.abs(sum - 1.0) > 1e-6) {
			return false;
		}
		return true;
	}

	/**
	 * Normalizes the entries of a probability vector to sum up to one.
	 * @param v vector to be normalized
	 */
	public void normalizeProbabilityVector(Object[][] v) {
		double sum = 0.0;
		for (int i = 0; i < v[0].length; i++) {
			Double d = (Double) v[0][i];
			sum += d.doubleValue();
		}
		for (int i = 0; i < v[0].length; i++) {
			Double d = (Double) v[0][i];
			if (sum == 0.0) {
				v[0][i] = Double.valueOf(1.0 / v[0].length);
			} else {
				v[0][i] = Double.valueOf(d.doubleValue() / sum);
			}
		}
	}

	/**
	 * Checks if the diagonal of a transition rate matrix only contains negative entries
	 * and if the entries in each row of the matrix sum up to a non-positive value.
	 * @param m matrix to be checked
	 * @return true if the matrix satisfies the condition, false otherwise
	 */
	public boolean checkTransitionRateMatrix(Object[][] m) {
		for (int i = 0; i < m.length; i++) {
			double sum = 0.0;
			for (int j = 0; j < m[i].length; j++) {
				Double d = (Double) m[i][j];
				if (i == j && d.doubleValue() >= 0.0) {
					return false;
				}
				sum += d.doubleValue();
			}
			if (sum > 1e-6) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks if the entries in each row of an infinitesimal generator matrix sum up to
	 * zero.
	 * @param m0 hidden matrix to be checked
	 * @param m1 observable matrix to be checked
	 * @return true if the matrix satisfies the condition, false otherwise
	 */
	public boolean checkInfinitesimalGeneratorMatrix(Object[][] m0, Object[][] m1) {
		for (int i = 0; i < m0.length; i++) {
			double sum = 0.0;
			for (int j = 0; j < m0[i].length; j++) {
				Double d0 = (Double) m0[i][j];
				Double d1 = (Double) m1[i][j];
				sum += (d0.doubleValue() + d1.doubleValue());
			}
			if (Math.abs(sum) > 1e-6) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Tells if this distribution is equivalent to another one.
	 * @param o other distribution
	 * @return true if equals, false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Distribution)) {
			return false;
		}
		Distribution d = (Distribution) o;
		// Check if class is the same
		if (!getClass().equals(d.getClass())) {
			return false;
		}
		// Check if all parameters are equals
		for (int i = 0; i < getNumberOfParameters(); i++) {
			if (getParameter(i).getValue() == null ? d.getParameter(i).getValue() != null
					: !getParameter(i).getValue().equals(d.getParameter(i).getValue())) {
				return false;
			}
		}
		return true;
	}

	// ----- Abastract Methods that must be implemented by all distributions ------------------------------

	/**
	 * Used to set parameters of this distribution.
	 * @return distribution parameters
	 */
	protected abstract Parameter[] setParameters();

	/**
	 * Sets explicative image of this distribution used, together with description, to help the
	 * user to understand meaning of parameters.
	 * @return explicative image
	 */
	protected abstract ImageIcon setImage();

	/**
	 * Returns this distribution's short description
	 * @return distribution's short description
	 */
	@Override
	public abstract String toString();

	// ----- Methods that must be implemented ONLY if hasMean or hasC are true ----------------------------

	/**
	 * Sets the mean for this distribution
	 * @param value mean value
	 */
	public void setMean(double value) {
		mean = value;
	}

	/**
	 * Sets the variation coefficient C for this distribution
	 * @param value variation coefficient C value
	 */
	public void setC(double value) {
		c = value;
	}

	/**
	 * This method has to be called whenever a parameter changes and <code>hasMean</code> or
	 * <code>hasC</code> is true
	 */
	public void updateCM() {
	}

	// ----- Inner class used to store parameters ---------------------------------------------------------

	/**
	 * This class describes a parameter. It can be instantiated only by Distribution and its subclassses
	 * but will be accessed with public methods
	 */
	public class Parameter implements Cloneable {

		protected Class<?> valueClass;
		protected Object value;
		protected String name;
		protected String description;
		protected ValueChecker checker;
		protected boolean directParameter;

		/**
		 * Constructs a new Parameter object. If a custom check on input values is needed, a call
		 * to <code>setValueChecker(ValueChecker checker)</code> method must be performed.
		 * @param name name of this parameter
		 * @param description brief description of this parameter usage
		 * @param valueClass Class type of value of this parameter
		 * @param defaultValue initial value for this parameter
		 * <code>getParameterClasspath()</code> for more details.
		 */
		public Parameter(String name, String description, Class<?> valueClass, Object defaultValue) {
			this(name, description, valueClass, defaultValue, false);
		}

		/**
		 * 
		 * Constructs a new Parameter object. If a custom check on input values is needed, a call
		 * to <code>setValueChecker(ValueChecker checker)</code> method must be performed.
		 * @param name name of this parameter
		 * @param description brief description of this parameter usage
		 * @param valueClass Class type of value of this parameter
		 * @param defaultValue initial value for this parameter
		 * @param directParameter indicates if this parameter will be added to the distribution of the parameter object when translating it to XML
		 * <code>getParameterClasspath()</code> for more details.
		 */
		public Parameter(String name, String description, Class<?> valueClass, Object defaultValue, boolean directParameter) {
			this.name = name;
			this.description = description;
			this.valueClass = valueClass;
			this.value = defaultValue;
			checker = null;
			this.directParameter = directParameter;
		}

		/**
		 * Sets a ValueChecker to check if a parameter's value is in the correct
		 * range. If no checks are required do not call this method
		 * 
		 * @param checker
		 *            Instance of ValueChecker Interface
		 */
		public void setValueChecker(ValueChecker checker) {
			this.checker = checker;
		}

		/**
		 * Gets a ValueChecker to check if a parameter's value is in the correct
		 * range. If no checks are required do not call this method
		 * 
		 * @return checker
		 *             Instance of ValueChecker Interface
		 */
		public ValueChecker getValueChecker() {
			return checker;
		}

		/**
		 * Sets value for this parameter. Returns if parameter could be assigned correctly
		 * @param value value to be assigned to this parameter
		 * @return If value.class is different from valueClass, or parameter value is not
		 * valid, returns false, otherwise true
		 */
		public boolean setValue(Object value) {
			// if instance is not correct returns false
			if (valueClass.isInstance(value)) {
				// If checker exists and its check is not satisfied returns false
				if (checker != null && !checker.checkValue(value)) {
					return false;
				}
				// Otherwise sets value and returns true
				this.value = value;
				return true;
			}
			return false;
		}

		/**
		 * Sets value for this parameter. Returns if parameter could be assigned correctly
		 * @param value value to be assigned to this parameter
		 * @return If value.class is different from valueClass, or parameter value is not
		 * valid, returns false, otherwise true
		 */
		public boolean setValue(Object[][] value) {
			// if instance is not correct returns false
			if (valueClass.isInstance(value[0][0])) {
				// If checker exists and its check is not satisfied returns false
				if (checker != null && !checker.checkValue(value)) {
					return false;
				}
				// Otherwise sets value and returns true
				this.value = value;
				return true;
			}
			return false;
		}

		/**
		 * Sets value for this parameter. If parameter class is String sets it, otherwise
		 * try to parse string if parameter is of a known numeric type. Returns if parameter
		 * could be assigned correctly.
		 * @param value to be assigned to this parameter or to be parsed
		 * @return If value.class is different from valueClass, or parameter value is not
		 * parsable or valid, returns false, otherwise true
		 */
		public boolean setValue(String value) {
			// If parameter is of the correct type, sets it and return
			if (setValue((Object) value)) {
				return true;
			}
			// Otherwise if it is of a known numeric type, try to decode it
			Object objval = parseValue(value);
			if (objval == null) {
				return false;
			}
			return setValue(objval);
		}

		/**
		 * Parses string if parameter is of a known numeric type.
		 * @param value value to be parsed
		 * @return If value.class is different from valueClass, or parameter value is not
		 * parsable, returns null, otherwise number
		 */
		public Object parseValue(String value) {
			Object objval = null;
			try {
				if (valueClass.equals(Integer.class)) {
					objval = Integer.decode(value);
				} else if (valueClass.equals(Long.class)) {
					objval = Long.decode(value);
				} else if (valueClass.equals(Short.class)) {
					objval = Short.decode(value);
				} else if (valueClass.equals(Byte.class)) {
					objval = Byte.decode(value);
				} else if (valueClass.equals(Float.class)) {
					objval = Float.valueOf(value);
				} else if (valueClass.equals(Double.class)) {
					objval = Double.valueOf(value);
				} else if (valueClass.equals(Boolean.class)) {
					objval = Boolean.valueOf(value);
				}
			} catch (NumberFormatException e) {
			}
			return objval;
		}

		/**
		 * Gets this parameter's value
		 * @return this parameter's value
		 */
		public Object getValue() {
			return value;
		}

		/**
		 * Gets this parameter value's class, ie the class of which parameter value is instance of.
		 * @return parameter value's class
		 */
		public Class<?> getValueClass() {
			return valueClass;
		}

		/**
		 * Gets a brief description of this parameter, used on distributionpanel
		 * @return parameter's description
		 */
		public String getDescription() {
			return description;
		}

		/**
		 * Gets the name of this parameter
		 * @return this parameter's name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Returns if this parameter has to be added directly to the distribution when translating it into XML
		 * @return true if the parameter has to be added to the distribution. false otherwise
		 */
		public boolean isDirectParameter() {
			return directParameter;
		}

		/**
		 * Returns a shallow copy of this Parameter. Note that as nothing can be inferred about
		 * all values clonability, so parameters are only referenced and not cloned. This should
		 * not be a problem as every value should be an immutable type.
		 * @return a copy of this parameter
		 */
		@Override
		public Object clone() {
			Parameter tmp = new Parameter(this.name, this.description, this.valueClass, this.value, this.directParameter);
			if (this.value instanceof Object[][]) {
				Object[][] om = (Object[][]) this.value;
				Object[][] cm = new Object[om.length][];
				for (int i = 0; i < om.length; i++) {
					cm[i] = new Object[om[i].length];
					for (int j = 0; j < om[i].length; j++) {
						cm[i][j] = om[i][j];
					}
				}
				tmp.value = cm;
			}
			tmp.setValueChecker(this.checker);
			return tmp;
		}

	}

	/**
	 * This interface is used to specify a custom check method on parameter's value (for example
	 * value's range).
	 */
	public interface ValueChecker {

		/**
		 * Checks if value of this object is correct. Note that when this method is called,
		 * parameter's value is granted to be instance of the correct class.
		 * @param value value to be checked
		 * @return true iff value is correct for this distribution parameter
		 */
		public boolean checkValue(Object value);

	}

}
