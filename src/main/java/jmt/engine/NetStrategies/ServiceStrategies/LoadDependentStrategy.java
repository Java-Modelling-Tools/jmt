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

package jmt.engine.NetStrategies.ServiceStrategies;

import java.util.Arrays;

import jmt.common.exception.ExpressionParseException;
import jmt.common.exception.IncorrectDistributionParameterException;
import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.ServiceStrategy;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.NodeSection;
import jmt.engine.math.DirectCircularList;
import jmt.engine.math.parser.Parser;
import jmt.engine.random.Distribution;
import jmt.engine.random.Parameter;

/**
 * <p>Title: Load Dependent Service Time Strategy</p>
 * <p>Description: This is a load dependent implementation of <code>ServiceStrategy</code>,
 * useful to model macro-portion of network into a single station and to model local
 * area networks.</p>
 * 
 * @author Bertoli Marco
 *         Date: 6-ott-2005
 *         Time: 16.37.36
 */
public class LoadDependentStrategy extends ServiceStrategy {

	private static final String VAR = "n";
	private static final int CACHESIZE = 1024;

	private LDParameter[] parameters;
	// Used to cache mean values. This structure has O(1) access time.
	DirectCircularList<MeanCache> cache;

	/**
	 * Creates a new Load Dependent Service Time Strategy
	 * @param parameters an array with all parameters in LDParameter format
	 */
	public LoadDependentStrategy(LDParameter[] parameters) {
		Arrays.sort(parameters);
		this.parameters = parameters;
		cache = new DirectCircularList<MeanCache>(CACHESIZE);
	}

	/**
	 * Calculates the service time using the specified parameters
	 * @param nodeSection the node section which applies this strategy
	 * @param jobClass the job class which follows this strategy
	 * @return the value of service time
	 * @throws NetException
	 */
	@Override
	public double wait(final NodeSection nodeSection, final JobClass jobClass) throws NetException {
		// Gets number of jobs in the station as the sum of job in queue and job under service
		try {
			// Number of jobs into service section
			int jobs = nodeSection.getOwnerNode().getSection(NodeSection.INPUT).getIntSectionProperty(NodeSection.PROPERTY_ID_RESIDENT_JOBS)
					+ nodeSection.getOwnerNode().getSection(NodeSection.SERVICE).getIntSectionProperty(NodeSection.PROPERTY_ID_RESIDENT_JOBS);

			// Search in cache for corresponding item
			MeanCache item = cache.get(jobs);
			if (item == null) {
				// Item is not in cache, so retrieves right LDParameter and perform parsing of function
				int index = Arrays.binarySearch(parameters, new Integer(jobs));
				if (index < 0) {
					index = -index - 2;
				}

				// Polling server switchover time could happen with 0 jobs in the lists
				if (index < 0) {
					index = 0;
				}

				LDParameter parameter = parameters[index];
				item = new MeanCache(parameter, jobs);
				cache.set(jobs, item);
			}
			// At this point item is retrieved from cache or parsed.
			if (item.valid) {
				// Note: this is needed as parameter is shared among all items of the same LDParameter
				item.parameter.setMean(item.mean);
			}

			return item.distribution.nextRand(item.parameter);
		} catch (NetException e) {
			throw new NetException("Error in LoadDependentStrategy: " + e.getMessage());
		} catch (ExpressionParseException e) {
			throw new NetException("Error in LoadDependentStrategy: " + e.getMessage());
		} catch (IncorrectDistributionParameterException e) {
			throw new NetException("Error in LoadDependentStrategy: " + e.getMessage());
		}
	}

	/**
	 * Calculates the service mean using the specified parameters
	 * @param nodeSection the node section which applies this strategy
	 * @param jobClass the job class which follows this strategy
	 * @return the value of service mean
	 * @throws NetException
	 */
	@Override
	public double expect(final NodeSection nodeSection, final JobClass jobClass) throws NetException {
		return 0.0;
	}

	/**
	 * Used mainly for the Balking class.
	 * @return parameters variable
	 */
	public LDParameter[] getParameters() {
		return parameters;
	}

	/**
	 * Inner class used to cache mean values and distributions to avoid parsing a function
	 * at each call of this strategy
	 */
	private class MeanCache {
		/** distribution used to evaluate service times */
		public Distribution distribution;
		/** parameter of distribution used to evaluate service times */
		public Parameter parameter;
		/** mean value */
		public double mean;
		/** tells if mean field is valid */
		public boolean valid;

		/**
		 * Creates a new MeanCache object by parsing mean value in given LDParameter
		 * @param ldp LDParameter of right range
		 * @param n current queue length value
		 * @throws ExpressionParseException if function cannot be parsed correctly
		 */
		public MeanCache(LDParameter ldp, int n) throws ExpressionParseException {
			distribution = ldp.getDistribution();
			parameter = ldp.getDistrParameter();
			if (ldp.getFunction() != null) {
				try {
					Parser p = ldp.getParser();
					p.setVariable(VAR, n);
					mean = p.getValue();
					valid = true;
				} catch (RuntimeException e) {
					throw new ExpressionParseException(e.getMessage());
				}
			}
		}
	}

}
