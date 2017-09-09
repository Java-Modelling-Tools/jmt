/*
 * Created on Oct 31, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

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

//import jmt.gui.common.distributions.Distribution.Parameter;
//import jmt.gui.common.distributions.Distribution.ValueChecker;
import javax.swing.ImageIcon;

import jmt.gui.common.JMTImageLoader;

/**
* <p>Title: Cox-2 Distribution</p>
* <p>Description: Cox-2 distribution data structure</p>
* 
* @author Casale Giuliano
* @date   September 9, 2017
*/

public class Cox2 extends Distribution {
	/**
	 * Construct a new Cox2 distribution
	 */
	public Cox2() {
		super("Coxian", // name in GUI
				"jmt.engine.random.Cox2Distr", "jmt.engine.random.Cox2Par", "Cox2");
		hasMean = true;
		hasC = true;
	}

	/**
	 * Used to set parameters of this distribution.
	 * @return distribution parameters
	 */

	@Override
	protected Parameter[] setParameters() {
		// Creates parameter array
		Parameter[] parameters = new Parameter[3]; //Cox-2 is specified by two rates and one probability

		/* arrival rate of jobs if Coxian is in state 0 */
		parameters[0] = new Parameter("lambda0", "\u03BB0", //unicode character for lambda0
				Double.class, new Double(4.0));
		// Checks value of k must be greater then 0
		parameters[0].setValueChecker(new ValueChecker() {
			public boolean checkValue(Object value) {
				Double d = (Double) value;
				if (d.doubleValue() >= 0) {
					return true;
				} else {
					return false;
				}
			}
		});

		/* arrival rate of jobs if Coxian is in state 1 */
		parameters[1] = new Parameter("lambda1", "\u03BB1", //unicode character for lambda1
				Double.class, new Double(0.5));
		// Checks value of k must be greater then 0
		parameters[1].setValueChecker(new ValueChecker() {
			public boolean checkValue(Object value) {
				Double d = (Double) value;
				if (d.doubleValue() >= 0) {
					return true;
				} else {
					return false;
				}
			}
		});

		/* rate of jump from state 0 to state 1 */
		parameters[2] = new Parameter("phi0", "p0", //unicode character for sigma0 
				Double.class, new Double(0.875));
		// Checks value of alpha must greater or equal then 2
		parameters[2].setValueChecker(new ValueChecker() {
			public boolean checkValue(Object value) {
				Double d = (Double) value;
				if (d.doubleValue() >= 0.0) {
					return true;
				} else {
					return false;
				}
			}
		});

		return parameters;
	}

	/**
	 * Set illustrating figure in distribution panel
	 * @return illustrating figure
	 */
	@Override
	protected ImageIcon setImage() {
		return JMTImageLoader.loadImage("Cox2");
	}

	/**
	 * Return distribution short description
	 * @return distribution short description
	 */
	@Override
	public String toString() {
		return "Cox2((" + FormatNumber(((Double) parameters[0].getValue()).doubleValue()) + ","
				+ FormatNumber(((Double) parameters[2].getValue()).doubleValue()) + "),("
				+ FormatNumber(((Double) parameters[1].getValue()).doubleValue()) + ",1.0))";
	}

	/**
	 * Sets the mean for this distribution
	 * @param value mean value
	 */
	@Override
	public void setMean(double value) {
		setCM(value, c);
	}

	/**
	 * Sets the variation coefficient C for this distribution
	 * @param value variation coefficient C value
	 */
	@Override
	public void setC(double value) {
		setCM(mean, value);
	}

	/**
	 * Sets Mean and C values
	 * @param mean mean value
	 * @param c c value
	 */
	protected void setCM(double mean, double c) {
		double l1, l2, p;
		if (c>=0.5 && c<1) {
			p = 0.0;
			l1 = 2/mean/(1+Math.sqrt(1+2*(c*c-1)));
			l2 = 2/mean/(1-Math.sqrt(1+2*(c*c-1)));
		}
		else if (c==1.0){
			p = 1.0;
			l1 = 1/mean;
			l2 = 1/mean;			
		}
		else {
		    l1 = 2/mean;
		    l2 = l1/(2*c*c);
		    p = 1 - l2/l1;
		}
	
		l1 = (double)Math.round(l1 * 100000d) / 100000d;
		l2 = (double)Math.round(l2 * 100000d) / 100000d;
		p = (double)Math.round(p * 100000d) / 100000d;
		
		if (p >= 0.0 && p <= 1.0 && l1 > 0 && l2 > 0 && mean > 0 && c>=0.5) {
			getParameter(0).setValue(new Double(l1));
			getParameter(1).setValue(new Double(l2));
			getParameter(2).setValue(new Double(p));
			c = (double)Math.round(c * 100000d) / 100000d;
			mean = (double)Math.round(mean * 100000d) / 100000d;
			this.c = c;
			this.mean = mean;
		}
	}

	/**
	 * This method is called whenever a parameter changes and <code>hasC</code> or
	 * <code>hasMean</code> are true
	 */
	@Override
	public void updateCM() {
		double phi0, lambda0, lambda1, var;
		lambda0 = ((Double) getParameter(0).getValue()).doubleValue();
		lambda1 = ((Double) getParameter(1).getValue()).doubleValue();
		phi0 = ((Double) getParameter(2).getValue()).doubleValue();
		mean = 1/lambda0 + (1-phi0)/lambda1;
		var = ((2*lambda1*(lambda0 - lambda0*phi0))/(lambda0 + lambda1 - lambda0*phi0) + (2*lambda0*lambda1*phi0)/(lambda0 + lambda1 - lambda0*phi0))/(lambda0*lambda0*((lambda1*(lambda0 - lambda0*phi0))/(lambda0 + lambda1 - lambda0*phi0) + (lambda0*lambda1*phi0)/(lambda0 + lambda1 - lambda0*phi0))) - (1/lambda0 - (phi0 - 1)/lambda1)*(1/lambda0 - (phi0 - 1)/lambda1) - (((phi0 - 1)/(lambda1*lambda1) + (phi0 - 1)/(lambda0*lambda1))*((2*lambda1*(lambda0 - lambda0*phi0))/(lambda0 + lambda1 - lambda0*phi0) + (2*lambda0*lambda1*phi0)/(lambda0 + lambda1 - lambda0*phi0)))/((lambda1*(lambda0 - lambda0*phi0))/(lambda0 + lambda1 - lambda0*phi0) + (lambda0*lambda1*phi0)/(lambda0 + lambda1 - lambda0*phi0));
		c = Math.sqrt(var) / mean;
		
		c = (double)Math.round(c * 100000d) / 100000d;
		mean = (double)Math.round(mean * 100000d) / 100000d;
	}

}
