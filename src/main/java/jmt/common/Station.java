package jmt.common;

import jmt.jmva.analytical.ExactConstants;

public class Station {
	
	private String name;
	private int type;
	private int servers;
	
	/**
	 * Constructs a station with default values
	 */
	public Station() {
		name = "Station1";
		type = ExactConstants.STATION_LI;
		servers = 1;
	}

	public String getName() {
		return name;
	}

	public int getType() {
		return type;
	}

	public int getServers() {
		return servers;
	}

}
