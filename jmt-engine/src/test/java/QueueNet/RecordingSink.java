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

package QueueNet;

import java.util.ArrayList;
import java.util.List;

import NodeSections.InputSection;

/** Test sink: acknowledges and records the ids of arriving jobs, with the time of arrival. */
public class RecordingSink extends InputSection {
	public final List<Integer> ids = new ArrayList<Integer>();
	public final List<Integer> classIds = new ArrayList<Integer>();
	public final List<Double> times = new ArrayList<Double>();

	@Override
	protected int process(NetMessage message) throws jmt.common.exception.NetException {
		switch (message.getEvent()) {
			case NetEvent.EVENT_JOB:
				ids.add(message.getJob().getId());
				classIds.add(message.getJob().getJobClass().getId());
				times.add(getOwnerNode().getNetSystem().getTime());
				send(NetEvent.EVENT_ACK, message.getJob(), 0.0, message.getSourceSection(), message.getSource());
				return MSG_PROCESSED;
			case NetEvent.EVENT_START:
			case NetEvent.EVENT_STOP:
				return MSG_PROCESSED;
			default:
				return MSG_NOT_PROCESSED;
		}
	}
}
