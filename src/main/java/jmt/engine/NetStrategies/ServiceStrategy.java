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

package jmt.engine.NetStrategies;

import jmt.common.AutoCheck;
import jmt.common.exception.NetException;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.NodeSection;

/**
 * Use this class to implement a specific service strategy. A service
 * strategy is a rule which calculates the service time for a job.
 * @author Francesco Radaelli
 */
public abstract class ServiceStrategy implements AutoCheck {

	/**
	 * This method should be overridden to implement a specific strategy.
	 * @param nodeSection the node section which applies this strategy.
	 * @param jobClass the job class which follows this strategy.
	 * @return the value of service time.
	 * @throws NetException
	 */
	public abstract double wait(final NodeSection nodeSection, final JobClass jobClass) throws NetException;

	/**
	 * This method should be overridden to implement a specific strategy.
	 * @param nodeSection the node section which applies this strategy.
	 * @param jobClass the job class which follows this strategy.
	 * @return the value of service mean.
	 * @throws NetException
	 */
	public abstract double expect(final NodeSection nodeSection, final JobClass jobClass) throws NetException;

	public boolean check() {
		return true;
	}

}
