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
import jmt.engine.QueueNet.NodeSection;

/**
 * This service strategy always raises an exception
 * @author Giuliano Casale
 */
public class DisabledServiceTimeStrategy extends ServiceStrategy {

	boolean showDisabledWarning;
	
	/** Creates a new instance of ServiceTime.*/
	public DisabledServiceTimeStrategy() {
		showDisabledWarning = false;
	}

	/**
	 * The method always throws an exception.
	 * @param callingSection The node section which is calling this method.
	 * @return NaN
	 * @throws NetException, this is always thrown.
	 */
	@Override
	public double wait(NodeSection callingSection) throws NetException {
		showDisabledWarning();
//		throw new NetException("A job requested service at a station where its class is disabled.");
		return 0.0;
	}

	/**
	 * The method always throws an exception.
	 * @param callingSection The node section which is calling this method.
	 * @return NaN
	 * @throws NetException, this is always thrown.
	 */
	@Override
	public double expect(NodeSection callingSection) throws NetException {
//		throw new NetException("A job requested service at a station where its class is disabled.");
		return 0.0;
	}

	/**
	 * Shows the fork job number warning.
	 */
	private void showDisabledWarning() {
		if (!showDisabledWarning) {
/*			if (getOwnerNode().getQueueNet().isTerminalSimulation()) {
				System.out.println("JMT - Warning: " + getOwnerNode().getName()
						+ " failed to maintain the equilibrium between input and output tasks.");
			} else {
			*/
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Thread.sleep(800);
						} catch (InterruptedException e) {
						}
						JOptionPane.showMessageDialog(null, "A job requested service at a station where its class is disabled.", "JMT - Warning", JOptionPane.WARNING_MESSAGE);
					}
				}).start();
//			}
			showDisabledWarning = true;
		}
	}

}
