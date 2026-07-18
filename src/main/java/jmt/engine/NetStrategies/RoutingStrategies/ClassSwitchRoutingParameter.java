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

package jmt.engine.NetStrategies.RoutingStrategies;

import jmt.common.exception.IncorrectDistributionParameterException;
import jmt.common.exception.LoadException;
import jmt.engine.random.Empirical;
import jmt.engine.random.EmpiricalEntry;
import jmt.engine.random.EmpiricalPar;
import jmt.engine.random.engine.RandomEngine;

public class ClassSwitchRoutingParameter {

	// store which destination node the class points to and probability of going to the node
	private String destNode;
	private Double destProbability;

	// store possible classes a job can change to
	private String[] toClasses;

	// the distribution parameters
	private Empirical distribution;
	private EmpiricalPar param;

	public ClassSwitchRoutingParameter(String destNode, Double destProbability, EmpiricalEntry entries[]) throws LoadException, IncorrectDistributionParameterException {
		this.destNode = destNode;
		this.destProbability = destProbability;

		// Build the individual outPaths map for the dest station node
		toClasses = new String[entries.length];
		double[] probabilities = new double[entries.length];
		for (int i = 0; i < entries.length; i++) {
			EmpiricalEntry entry = entries[i];
			if (entry.getValue() instanceof String) {
				//sets the class name to switch to
				toClasses[i] = (String) entry.getValue();
				//sets the corresponding routing probability
				probabilities[i] = entry.getProbability();
			} else {
				throw new LoadException("Name of the class is not a String");
			}

			probabilities[i] = entries[i].getProbability();
		}

		distribution = new Empirical();
		param = new EmpiricalPar(probabilities);
	}

	public String getDestNode() {
		return destNode;
	}

	public Double getDestProbability() {
		return destProbability;
	}

	public String getToClass() {
		try {
			//the empirical distribution returns the position of the chosen output node
			int nodePos = (int) Math.round(distribution.nextRand(param));
			if (nodePos >= 0) {
				return toClasses[nodePos];
			}
		} catch (IncorrectDistributionParameterException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void setRandomEngine(RandomEngine randomEngine) {
		distribution.setRandomEngine(randomEngine);
	}

}
