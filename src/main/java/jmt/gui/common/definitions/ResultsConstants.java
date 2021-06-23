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

package jmt.gui.common.definitions;

/**
 * <p>Title: Results Constants</p>
 * <p>Description: All sort of constant text displayed into results window. This is provided here
 * for easy maintainability.</p>
 *
 * @author Bertoli Marco
 *         Date: 26-set-2005
 *         Time: 11.06.50
 *
 * Modified by Ashanka (Nov 09):
 * Desc: Added the description of the Drop Rate
 *
 * Modified by Ashanka (May 2010): 
 * Patch: Multi-Sink Perf. Index 
 * Description: Added new Performance index for capturing 
 * 				1. global response time (ResponseTime per Sink)
 *              2. global throughput (Throughput per Sink)
 *              each sink per class.
 */
public interface ResultsConstants {

	public static final int BORDERSIZE = 5;
	public static final String IN_PROGRESS_IMAGE = "Measure_running";
	public static final String IN_PROGRESS_TEXT = "Simulator is still computing this measure";
	public static final String SUCCESS_IMAGE = "Measure_ok";
	public static final String SUCCESS_TEXT = "This measure was computed with the requested confidence interval (red lines) and maximum relative error";
	public static final String FAILED_IMAGE = "Measure_fail";
	public static final String FAILED_TEXT = "Simulator failed to compute this measure with the requested confidence interval and maximum relative error";
	public static final String NO_SAMPLES_IMAGE = "Measure_nosamples";
	public static final String NO_SAMPLES_TEXT = "Simulator cannot compute this measure as no samples were received";
	public static final String ALL_CLASSES = "-- All --";
	public static final String ALL_MODES = "-- All --";
	public static final String ALL_STATIONS = "Network";
	public static final String FOR_GREEN_GRAPH = "Double click on this graph to open it in a new windows.\nRight-click to save it.\nClick on green bars to see the simulation time, "
			+ "the sample average (blue), and the sample values (green).\n";
	/**HTML formats for panels descriptions*/
	static final String HTML_START = "<html><body align=\"left\">";
	static final String HTML_END = "</body></html>";
	static final String HTML_FONT_TITLE = "<font size=\"4\"><b>";
	static final String HTML_FONT_NORM = "<font size=\"3\">";
	static final String HTML_FONT_TIT_END = "</b></font><br>";
	static final String HTML_FONT_NOR_END = "</font>";

	// Tabbed panels description
	public static final String DESCRIPTION_QUEUE_LENGTH = HTML_START + HTML_FONT_TITLE + "Number of Customers" + HTML_FONT_TIT_END + HTML_FONT_NORM
			+ "Average number of customers for each selected class at each selected station." + HTML_FONT_NOR_END + HTML_END;

	public static final String DESCRIPTION_QUEUE_TIME = HTML_START + HTML_FONT_TITLE + "Queue Time" + HTML_FONT_TIT_END + HTML_FONT_NORM
			+ "Average queue time for each selected class at each selected station." + HTML_FONT_NOR_END + HTML_END;

	public static final String DESCRIPTION_RESPONSE_TIME = HTML_START + HTML_FONT_TITLE + "Response Time" + HTML_FONT_TIT_END + HTML_FONT_NORM
			+ "Average response time for each selected class at each selected station. In a Fork/Join section this index refers to tasks and not to customers." + HTML_FONT_NOR_END + HTML_END;

	public static final String DESCRIPTION_RESIDENCE_TIME = HTML_START + HTML_FONT_TITLE + "Residence Time" + HTML_FONT_TIT_END + HTML_FONT_NORM
			+ "Average residence time for each selected class at each selected station. (Residence Time = Number of Visits * Response Time).  In a Fork/Join section this index refers to tasks and not to customers."
			+ HTML_FONT_NOR_END + HTML_END;

	public static final String DESCRIPTION_UTILIZATION = HTML_START + HTML_FONT_TITLE + "Utilization" + HTML_FONT_TIT_END + HTML_FONT_NORM
			+ "Average utilization for each selected class at each selected station. For multi-server queueing stations this is the "
			+ "average utilization of each server. The utilization of a delay station is the average number of customers in the station (may be greater than 1)."
			+ HTML_FONT_NOR_END + HTML_END;

	public static final String DESCRIPTION_THROUGHPUT = HTML_START + HTML_FONT_TITLE + "Throughput" + HTML_FONT_TIT_END + HTML_FONT_NORM
			+ "Average throughput for each selected class at each selected station." + HTML_FONT_NOR_END + HTML_END;

	public static final String DESCRIPTION_DROP_RATE = HTML_START + HTML_FONT_TITLE + "Drop Rate" + HTML_FONT_TIT_END + HTML_FONT_NORM
			+ "Average drop rate for each selected class at each selected station." + HTML_FONT_NOR_END + HTML_END;

	public static final String DESCRIPTION_RENEGING_RATE = HTML_START + HTML_FONT_TITLE + "Reneging Rate" + HTML_FONT_TIT_END + HTML_FONT_NORM
			+ "Average reneging rate for each selected class at each selected station." + HTML_FONT_NOR_END + HTML_END;

	public static final String DESCRIPTION_BALKING_RATE = HTML_START + HTML_FONT_TITLE + "Balking Rate" + HTML_FONT_TIT_END + HTML_FONT_NORM
			+ "Average balking rate for each selected class at each selected station." + HTML_FONT_NOR_END + HTML_END;

	public static final String DESCRIPTION_RETRIAL_ATTEMPTS_RATE = HTML_START + HTML_FONT_TITLE + "Retrial Rate" + HTML_FONT_TIT_END + HTML_FONT_NORM
			+ "Average retrial attempts rate for each selected class at each selected station." + HTML_FONT_NOR_END + HTML_END;

	public static final String DESCRIPTION_RETRIAL_ORBIT_TIME = HTML_START + HTML_FONT_TITLE + "Retrial Orbit Residence Time" + HTML_FONT_TIT_END + HTML_FONT_NORM
					+ "Total average orbit time for each selected class until admitted at the selected station. (Orbit Residence Time = Number of Orbits * Orbit Time). " + HTML_FONT_NOR_END + HTML_END;

	public static final String DESCRIPTION_RETRIAL_ORBIT_SIZE = HTML_START + HTML_FONT_TITLE + "Retrial Orbit Size" + HTML_FONT_TIT_END + HTML_FONT_NORM
					+ "Average number of customers at the retrial orbit of a selected class at each selected station." + HTML_FONT_NOR_END + HTML_END;

	//public static final String DESCRIPTION_BUSY_SERVER_NUMBER = HTML_START + HTML_FONT_TITLE + "Number of Busy Servers" + HTML_FONT_TIT_END + HTML_FONT_NORM
	//				+ "Average number of number of busy servers at each selected station." + HTML_FONT_NOR_END + HTML_END;

	public static final String DESCRIPTION_SYSTEM_CUSTOMER_NUMBER = HTML_START + HTML_FONT_TITLE + "System Number of Customers" + HTML_FONT_TIT_END + HTML_FONT_NORM
			+ "Average number of customers in the entire system for each selected class." + HTML_FONT_NOR_END + HTML_END;

	public static final String DESCRIPTION_SYSTEM_RESPONSE_TIME = HTML_START + HTML_FONT_TITLE + "System Response Time" + HTML_FONT_TIT_END + HTML_FONT_NORM
			+ "Average response time of the entire system for each selected class." + HTML_FONT_NOR_END + HTML_END;

	public static final String DESCRIPTION_SYSTEM_THROUGHPUT = HTML_START + HTML_FONT_TITLE + "System Throughput" + HTML_FONT_TIT_END + HTML_FONT_NORM
			+ "Average throughput of the entire system for each selected class." + HTML_FONT_NOR_END + HTML_END;

	public static final String DESCRIPTION_SYSTEM_DROP_RATE = HTML_START + HTML_FONT_TITLE + "System Drop Rate" + HTML_FONT_TIT_END + HTML_FONT_NORM
			+ "Average drop rate of the entire system for each selected class." + HTML_FONT_NOR_END + HTML_END;

	public static final String DESCRIPTION_SYSTEM_RENEGING_RATE = HTML_START + HTML_FONT_TITLE + "System Reneging Rate" + HTML_FONT_TIT_END + HTML_FONT_NORM
			+ "Average reneging rate of the entire system for each selected class." + HTML_FONT_NOR_END + HTML_END;

	public static final String DESCRIPTION_SYSTEM_BALKING_RATE = HTML_START + HTML_FONT_TITLE + "System Balking Rate" + HTML_FONT_TIT_END + HTML_FONT_NORM
			+ "Average balking rate of the entire system for each selected class." + HTML_FONT_NOR_END + HTML_END;

	public static final String DESCRIPTION_SYSTEM_POWER = HTML_START + HTML_FONT_TITLE + "System Power" + HTML_FONT_TIT_END + HTML_FONT_NORM
			+ "Average power of the entire system for each selected class." + HTML_FONT_NOR_END + HTML_END;

	public static final String DESCRIPTION_SYSTEM_RETRIAL_ATTEMPTS_RATE = HTML_START + HTML_FONT_TITLE + "System Retrial Attempts Rate" + HTML_FONT_TIT_END + HTML_FONT_NORM
			+ "Average retrial attempts rate of the entire system for each selected class." + HTML_FONT_NOR_END + HTML_END;

	public static final String DESCRIPTION_RESPONSE_TIME_PER_SINK = HTML_START + HTML_FONT_TITLE + "Response Time per Sink" + HTML_FONT_TIT_END + HTML_FONT_NORM
			+ "Average response time for each selected class at each selected Sink station." + HTML_FONT_NOR_END + HTML_END;

	public static final String DESCRIPTION_THROUGHPUT_PER_SINK = HTML_START + HTML_FONT_TITLE + "Throughput per Sink" + HTML_FONT_TIT_END + HTML_FONT_NORM
			+ "Average throughput for each selected class at each selected Sink station." + HTML_FONT_NOR_END + HTML_END;

	public static final String DESCRIPTION_FCR_TOTAL_WEIGHT = HTML_START + HTML_FONT_TITLE + "FCR Total Weight" + HTML_FONT_TIT_END + HTML_FONT_NORM
			+ "Average total weight for each selected finite capacity region." + HTML_FONT_NOR_END + HTML_END;

	public static final String DESCRIPTION_FCR_MEMORY_OCCUPATION = HTML_START + HTML_FONT_TITLE + "FCR Memory Occupation" + HTML_FONT_TIT_END + HTML_FONT_NORM
			+ "Average memory occupation for each selected finite capacity region." + HTML_FONT_NOR_END + HTML_END;

	public static final String DESCRIPTION_FJ_CUSTOMER_NUMBER = HTML_START + HTML_FONT_TITLE + "Fork Join Number of Customers" + HTML_FONT_TIT_END + HTML_FONT_NORM
			+ "Average number of customers for each selected class in each selected Fork/Join section." + HTML_FONT_NOR_END + HTML_END;

	public static final String DESCRIPTION_FJ_RESPONSE_TIME = HTML_START + HTML_FONT_TITLE + "Fork Join Response Time" + HTML_FONT_TIT_END + HTML_FONT_NORM
			+ "Average response time of a customer for each selected class in each selected Fork/Join section." + HTML_FONT_NOR_END + HTML_END;

	public static final String DESCRIPTION_FIRING_THROUGHPUT = HTML_START + HTML_FONT_TITLE + "Firing Throughput" + HTML_FONT_TIT_END + HTML_FONT_NORM
			+ "Average firing throughput for each selected mode at each selected transition station." + HTML_FONT_NOR_END + HTML_END;

	// Temp measure mean label
	public static final String TEMP_MEAN = HTML_START + HTML_FONT_NORM + "<b>Average value: </b>" + HTML_FONT_NOR_END + HTML_END;

}
