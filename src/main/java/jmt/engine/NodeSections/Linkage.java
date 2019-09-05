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

package jmt.engine.NodeSections;

import jmt.common.exception.NetException;
import jmt.engine.QueueNet.BlockingRegion;
import jmt.engine.QueueNet.Job;
import jmt.engine.QueueNet.NetEvent;
import jmt.engine.QueueNet.NetMessage;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.NodeList;
import jmt.engine.QueueNet.NodeSection;

/**
 * <p>Title: Linkage</p>
 * <p>Description: This class implements the linkage section.</p>
 *
 * @author Lulai Zhu
 * Date: 15-07-2016
 * Time: 21.00.00
 */
public class Linkage extends OutputSection {

	private NodeList outputNodes;
	private NetNode requestSource;

	//--------------------BLOCKING REGION PROPERTIES--------------------//
	//true if the border router behaviour is turned on
	private boolean borderRouterON;
	//the blocking region that the owner node of this section belongs to
	private BlockingRegion myRegion;
	//the input station of the blocking region
	private NetNode regionInputStation;
	//------------------------------------------------------------------//

	/**
	 * Creates a new instance of the linkage section.
	 */
	public Linkage() {
		borderRouterTurnOFF();
	}

	/**
	 * Tells if the border router behaviour is turned on.
	 */
	public boolean isBorderRouterON() {
		return borderRouterON;
	}

	/**
	 * Turns off the border router behaviour.
	 */
	public void borderRouterTurnOFF() {
		borderRouterON = false;
		myRegion = null;
		regionInputStation = null;
	}

	/**
	 * Turns on the border router behaviour.
	 */
	public void borderRouterTurnON(BlockingRegion region) {
		borderRouterON = true;
		myRegion = region;
		regionInputStation = myRegion.getInputStation();
	}

	@Override
	protected void nodeLinked(NetNode node) throws NetException {
		outputNodes = node.getOutputNodes();
		requestSource = null;
	}

	@Override
	protected int process(NetMessage message) throws NetException {
		Object data = message.getData();
		NetNode source = message.getSource();

		switch (message.getEvent()) {

		case NetEvent.EVENT_START:
			break;

		case NetEvent.EVENT_JOB:
		{
			Job job = (Job) data;
			send(job, 0.0, requestSource);
			if (borderRouterON && !myRegion.belongsToRegion(requestSource)) {
				myRegion.decreaseOccupation(job.getJobClass());
				send(NetEvent.EVENT_JOB_OUT_OF_REGION, job, 0.0, NodeSection.INPUT, regionInputStation);
			}
			break;
		}

		case NetEvent.EVENT_ACK:
			return MSG_NOT_PROCESSED;

		case NetEvent.EVENT_JOB_CHANGE:
		{
			for (int i = 0; i < outputNodes.size(); i++) {
				send(NetEvent.EVENT_JOB_CHANGE, data, 0.0, NodeSection.INPUT, outputNodes.get(i));
			}
			break;
		}

		case NetEvent.EVENT_JOB_REQUEST:
		{
			if (requestSource != null) {
				return MSG_NOT_PROCESSED;
			}
			requestSource = source;
			sendBackward(NetEvent.EVENT_JOB_REQUEST, data, 0.0);
			break;
		}

		case NetEvent.EVENT_JOB_FINISH:
		{
			send(NetEvent.EVENT_JOB_FINISH, data, 0.0, NodeSection.INPUT, requestSource);
			requestSource = null;
			break;
		}

		case NetEvent.EVENT_STOP:
			break;

		default:
			return MSG_NOT_PROCESSED;
		}

		return MSG_PROCESSED;
	}

}
