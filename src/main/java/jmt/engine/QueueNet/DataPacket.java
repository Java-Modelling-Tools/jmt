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

/**
 * <p>Title: Data Packet</p>
 * <p>Description: This interface defines the data packet.</p>
 *
 * @author Lulai Zhu
 * Date: 15-07-2016
 * Time: 21.00.00
 */
public interface DataPacket {

	/** PACKET SIZE: Enabling packet.*/
	public static final int PACKET_SIZE_ENABLING = 2;
	/** PACKET SIZE: Timing packet.*/
	public static final int PACKET_SIZE_TIMING = 5;
	/** PACKET SIZE: Firing packet.*/
	public static final int PACKET_SIZE_FIRING = 4;

	/** FIELD ID: Mode index field.*/
	public static final int FIELD_MODE_INDEX = 0;
	/** FIELD ID: Enabling degree field.*/
	public static final int FIELD_ENABLING_DEGREE = 1;
	/** FIELD ID: Firing delay field.*/
	public static final int FIELD_FIRING_DELAY = 1;
	/** FIELD ID: Total lifetime field.*/
	public static final int FIELD_TOTAL_LIFETIME = 1;
	/** FIELD ID: Firing priority field.*/
	public static final int FIELD_FIRING_PRIORITY = 2;
	/** FIELD ID: Fork job info field.*/
	public static final int FIELD_FORK_JOB_INFO = 2;
	/** FIELD ID: Firing weight field.*/
	public static final int FIELD_FIRING_WEIGHT = 3;
	/** FIELD ID: Fork job num field.*/
	public static final int FIELD_FORK_JOB_NUM = 3;
	/** FIELD ID: Timing token field.*/
	public static final int FIELD_TIMING_TOKEN = 4;

}
