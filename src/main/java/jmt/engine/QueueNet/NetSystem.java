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

package jmt.engine.QueueNet;

import java.util.ListIterator;

import jmt.common.exception.NetException;
import jmt.engine.dataAnalysis.Measure;
import jmt.engine.simEngine.SimSystem;

/**
 * This class controls the simulation running. It should be used to :
 * <ul>
 * <li>start the simulation;</li>
 * <li>stop the simulation;</li>
 * <li>abort the simulation;</li>
 * <li>get simulation elapsed time;</li>
 * <li>get total simulation time;</li>
 * <li>get real elapsed time.</li>
 * </ul>
 * Note that all methods are static and the class <b>should not be</b>
 * instanced.
 * @author Francesco Radaelli, Federico Granata, Stefano Omini
 * Modified by Bertoli Marco: fixed bug that avoided simulation termination with closed classes. 22-09-2005
 */
public class NetSystem {

	private static NetController netController;

	private static long startTime;

	private static NetworkList networkList;

	private NetSystem() {
	}

	/**
	 * Initializes the simulation system
	 *
	 */
	public static void initialize() {
		netController = new NetController();
		SimSystem.initialize();
		networkList = new NetworkList();
	}

	/** Terminates NetSystem and closes the log. You should recall the initialize method, rebuild
	 * all the network nodes and recall the <i>start</i> method in order to
	 * launch a new simulation.
	 */
	public static void terminate() {
		abort();
	}

	/** Gets the current simulation time.
	 *	@return Current simulation time.
	 */
	public static double getTime() {
		return SimSystem.getClock();
	}

	/** Gets the current "real" elapsed time.
	 *	@return Current simulation time.
	 */
	public static double getElapsedTime() {
		return (System.currentTimeMillis() - startTime) / 1000.0;
	}

	/** Starts the NetSystem Engine and executes the simulation.
	 * This method should be called when <b>all</b> NetNode of <b>all</b>
	 * QueueNetworks are ready to start.
	 * @throws NetException
	 * @throws InterruptedException
	 */
	public static void start() throws NetException, InterruptedException {
		NetNode Node;
		ListIterator<QueueNetwork> nets = networkList.listIterator();
		ListIterator<NetNode> nodes;
		QueueNetwork Network;
		startTime = System.currentTimeMillis();

		while (nets.hasNext()) {
			Network = nets.next();
			if (Network.getState() == QueueNetwork.STATE_READY) {
				nodes = Network.getNodes().listIterator();
				while (nodes.hasNext()) {
					Node = nodes.next();
					Node.send(NetEvent.EVENT_START, null, 0.0, NodeSection.NO_ADDRESS, NodeSection.INPUT, Node);
					Node.send(NetEvent.EVENT_START, null, 0.0, NodeSection.NO_ADDRESS, NodeSection.SERVICE, Node);
					Node.send(NetEvent.EVENT_START, null, 0.0, NodeSection.NO_ADDRESS, NodeSection.OUTPUT, Node);
				}
				Network.setState(QueueNetwork.STATE_RUNNING);
			}
		}

		netController.start();
		netController.run();
	}

	public static boolean pause() {
		//if (netController != null && netController.isRunning()) {
		if (netController != null) {
			netController.block();
			return true;
		}
		return false;
	}

	public static boolean restartFromPause() {
		//if (netController != null && netController.isRunning()) {
		if (netController != null) {
			netController.unblock();
			return true;
		}
		return false;
	}

	/** Stops the NetSystem Engine and terminates the simulation (stops all the
	 * controlled QueueNetworks).
	 */
	public static void stop() {
		ListIterator<QueueNetwork> nets = networkList.listIterator();
		QueueNetwork network;
		while (nets.hasNext()) {
			network = nets.next();
			stop(network);
		}
	}

	/** Stops the NetSystem Engine and terminates the simulation.
	 * @param Network Reference to the network to be stopped.
	 */
	public static void stop(QueueNetwork Network) {
		NetNode node;
		if (Network.getState() == QueueNetwork.STATE_RUNNING) {
			/* Informs ALL nodes of stop event and not only Reference nodes */
			ListIterator<NetNode> nodes = Network.getNodes().listIterator();
			while (nodes.hasNext()) {
				node = nodes.next();
				node.send(NetEvent.EVENT_STOP, null, 0.0, NodeSection.NO_ADDRESS, NodeSection.NO_ADDRESS, node);
			}
			Network.setState(QueueNetwork.STATE_STOPPED);
		}
	}

	/** Aborts the NetSystem Engine and terminates the simulation (aborts all
	 * the controlled QueueNetworks).
	 */
	public static void abort() {
		ListIterator<QueueNetwork> nets = networkList.listIterator();
		QueueNetwork network;
		while (nets.hasNext()) {
			network = nets.next();
			abort(network);
		}
		SimSystem.abort();
	}

	/** Aborts the NetSystem Engine and terminates the simulation.
	 * @param network Reference to the network to be stopped.
	 */
	public static void abort(QueueNetwork network) {
		NetNode node;
		if (network.getState() == QueueNetwork.STATE_RUNNING) {
			ListIterator<NetNode> nodes = network.getReferenceNodes().listIterator();
			nodes = network.getReferenceNodes().listIterator();
			while (nodes.hasNext()) {
				node = nodes.next();
				node.send(NetEvent.EVENT_ABORT, null, 0.0, NodeSection.NO_ADDRESS, NodeSection.NO_ADDRESS, node);
			}
			network.setState(QueueNetwork.STATE_ABORTED);
		}
	}

	/** Adds a new network to the NetSystem.
	 * @param Network Reference to the network to be added.*/
	public static void addNetwork(QueueNetwork Network) {
		networkList.add(Network);
	}

	/** Gets networks list.
	 * @return Reference to a network list.*/
	public static NetworkList getNetworkList() {
		return networkList;
	}

	/** Checks if the NetSystem Engine is running.
	 * @return True if NetSystem Engine is running.
	 */
	public static boolean isRunning() {
		return netController.isRunning();
	}

	/** Gets total simulation time.
	 * @return Simulation time.
	 */
	public static double getSimulationTime() {
		return netController.getSimulationTime();
	}

	/** Gets a node from its id. Searches the id between all the networks
	 *
	 * @param id the id of the node
	 * @return searched node
	 */
	public static final NetNode getNode(int Id) {
		return (NetNode) SimSystem.getEntity(Id);
	}

	/** Gets a node from its name. Searches the name between all the networks
	 *
	 * @param name the name of the node
	 * @return searched node
	 */
	public static NetNode getNode(String name) {
		return (NetNode) SimSystem.getEntity(name);
	}

	static void checkMeasures() {
		ListIterator<QueueNetwork> networks = networkList.listIterator();
		QueueNetwork network;
		ListIterator<Measure> measures;
		int count, num;
		Measure measure;
		while (networks.hasNext()) {
			network = networks.next();
			num = network.getMeasures().size();
			if (num > 0) {
				measures = network.getMeasures().listIterator();
				count = 0;
				while (measures.hasNext()) {
					measure = measures.next();
					if (measure.hasFinished()) {
						count++;
					}
				}

				switch (network.getBehaviour()) {
				case QueueNetwork.BEHAVIOUR_ABORT:
					abort();
					break;
				case QueueNetwork.BEHAVIOUR_STOP:
					stop();
					break;
				case QueueNetwork.BEHAVIOUR_OBTAIN_ALL_MEASURES_THEN_ABORT:
					if (count == num) {
						abort(network);
					}
					break;
				case QueueNetwork.BEHAVIOUR_OBTAIN_ALL_MEASURES_THEN_STOP:
					if (count == num) {
						stop(network);
					}
					break;
				}
			}
		}
	}

	/**
	 * Checks simulation progress, showing a percentage of completed works
	 * <br>Author: Bertoli Marco
	 * @param network network to be checked for progress
	 * @return estimated simulation progress
	 * @throws NetException if network is null
	 */
	public static double checkProgress(QueueNetwork network) throws NetException {
		if (network == null) {
			throw new NetException("Cannot measure progress of a network which does not exist.");
		}

		// We estimate on the slowest not completed measure
		ListIterator<Measure> measures;
		double slowest = 1;
		Measure measure;
		measures = network.getMeasures().listIterator();
		while (measures.hasNext()) {
			measure = measures.next();
			if (!measure.hasFinished()) {
				// find slowest measure
				if (measure.getSamplesAnalyzedPercentage() < slowest) {
					slowest = measure.getSamplesAnalyzedPercentage();
				}
			}
		}
		return slowest;
	}

	public static double getTempMeasures(QueueNetwork network) throws NetException {
		if (network == null) {
			throw new NetException("Cannot get measures of a network which does not exist.");
		}

		ListIterator<Measure> measures;
		int count = 0;
		Measure measure;
		int num = network.getMeasures().size();
		if (num > 0) {
			measures = network.getMeasures().listIterator();
			while (measures.hasNext()) {
				measure = measures.next();
				//count finished measures
				if (measure.hasFinished()) {
					count++;
				}
			}
		}
		return (double) count / (double) num;
	}

	static void stopNoSamplesMeasures() {
		ListIterator<QueueNetwork> networks = networkList.listIterator();
		QueueNetwork network;
		ListIterator<Measure> measures;
		int num;
		Measure measure;
		while (networks.hasNext()) {
			network = networks.next();
			num = network.getMeasures().size();
			if (num > 0) {
				measures = network.getMeasures().listIterator();
				while (measures.hasNext()) {
					measure = measures.next();
					if (!measure.hasFinished()) {
						measure.testDeadMeasure();
					}
				}
			}
		}
	}

	/**
	 * Sets the maximum simulated time for the simulation in the netController
	 * @param maxSimulatedTime the maximum simulated time for the simulation
	 */
	public static void setMaxSimulatedTime(double maxSimulatedTime) {
		if (netController != null) {
			netController.setMaxSimulatedTime(maxSimulatedTime);
		}
	}

	/**
	 * Sets the maximum processed events for the simulation in the netController
	 * @param maxProcessedEvents the maximum processed events for the simulation
	 */
	public static void setMaxProcessedEvents(int maxProcessedEvents) {
		if (netController != null) {
			netController.setMaxProcessedEvents(maxProcessedEvents);
		}
	}

}
