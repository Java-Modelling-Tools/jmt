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

import javax.swing.JOptionPane;

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.ServiceStrategy;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.NodeSection;

/**
 * This service strategy always raises a warning.
 * @author Giuliano Casale
 */
public class DisabledServiceTimeStrategy extends ServiceStrategy {

	boolean isDisabledWarningShown;
	
	/** Creates a new instance of ServiceTime.*/
	public DisabledServiceTimeStrategy() {
		isDisabledWarningShown = false;
	}

	/**
	 * Returns the service time, which is always 0.0.
	 * @param nodeSection the node section which applies this strategy.
	 * @param jobClass the job class which follows this strategy.
	 * @return the value of service time.
	 * @throws NetException
	 */
	@Override
	public double wait(final NodeSection nodeSection, final JobClass jobClass) throws NetException {
		if (!isDisabledWarningShown) {
			if (nodeSection.getOwnerNode().getQueueNet().isTerminalSimulation()) {
				System.out.println("JSIMengine - Warning: A job of class \""
						+ jobClass.getName() + "\" requires service at station \""
						+ nodeSection.getOwnerNode().getName() + "\", which however is disabled.");
			} else {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Thread.sleep(800);
						} catch (InterruptedException e) {
						}
						JOptionPane.showMessageDialog(null, "A job of class \""
								+ jobClass.getName() + "\" requires service at station \""
								+ nodeSection.getOwnerNode().getName() + "\", which however is disabled.",
								"JSIMengine - Warning", JOptionPane.WARNING_MESSAGE);
					}
				}).start();
			}
			isDisabledWarningShown = true;
		}
		return 0.0;
	}

	/**
	 * Returns the service mean, which is always 0.0.
	 * @param nodeSection the node section which applies this strategy.
	 * @param jobClass the job class which follows this strategy.
	 * @return the value of service mean.
	 * @throws NetException
	 */
	@Override
	public double expect(final NodeSection nodeSection, final JobClass jobClass) throws NetException {
		return 0.0;
	}

}
