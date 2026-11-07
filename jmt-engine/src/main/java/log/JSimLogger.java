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

package log;

import java.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p><b>Name:</b> JSimLogger</p>
 * <p><b>Description:</b>
 * A wrapper around the SLF4J API (backend: log4j 2, configured by log4j2.xml) to provide logging capabilities.
 * </p>
 * <p><b>Date:</b> 11/dic/2009
 * <b>Time:</b> 20.17.54</p>
 * @author Bertoli Marco [marco.bertoli@neptuny.com]
 * @version 1.0
 */
public class JSimLogger implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final String STD_LOGGER = "jmt.engine";
	private static String delimiter = ",";
	private static String decimalSeparator = ".";
	private static String debugLogPath = "JSIM_debug.csv";

	private transient Logger logger;

	public static void resetDebugLoggerStorePath(String newPath) {
		String correctedPath = newPath.replace("\\", System.getProperty("file.separator"));
		debugLogPath = correctedPath + System.getProperty("file.separator") + "JSIM_debug.csv";
		File oldEventTraceFile = new File(debugLogPath);
		if (oldEventTraceFile.exists()) {
			oldEventTraceFile.delete();
		}
		writeCSVHeaderIfNotExists(debugLogPath);
	}

	private static void writeCSVHeaderIfNotExists(String filePath) {
		File file = new File(filePath);
		if (!file.exists()) {
			try (FileWriter writer = new FileWriter(filePath, true)) {
				writer.append("dispatchOrReceive").append(delimiter)
						.append("sender").append(delimiter)
						.append("eventname").append(delimiter)
						.append("receiver").append(delimiter)
						.append("timestamp").append("\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void setDelimiter(String newDelimiter) {
		delimiter = newDelimiter;
	}

	public static void setDecimalSeparator(String newDecimalSeparator) {
		decimalSeparator = newDecimalSeparator;
	}

	public static String getDecimalSeparator() {
		return decimalSeparator;
	}

	public static void logCustomDebug(String sendOrReceive, String sender, String eventName, String receiver, String timeStamp) {
		try (FileWriter writer = new FileWriter(debugLogPath, true)) {
			writer.append(sendOrReceive).append(delimiter)
					.append(sender).append(delimiter)
					.append(eventName).append(delimiter)
					.append(receiver).append(delimiter)
					.append(timeStamp).append("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private JSimLogger() {
		logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
	}

	private JSimLogger(String loggerName) {
		logger = LoggerFactory.getLogger(loggerName);
	}

	public static JSimLogger getRootLogger() {
		JSimLogger mylogger = new JSimLogger();
		return mylogger;
	}

	public static JSimLogger getLogger() {
		return getLogger(JSimLogger.STD_LOGGER);
	}

	public static JSimLogger getLogger(Object caller) {
		JSimLogger mylogger = null;
		if (caller != null) {
			mylogger = JSimLogger.getLogger(caller.getClass());
		} else {
			mylogger = JSimLogger.getLogger();
		}
		return mylogger;
	}

	public static JSimLogger getLogger(Class<?> callerClass) {
		JSimLogger mylogger = new JSimLogger(callerClass.getName());
		return mylogger;
	}

	public static JSimLogger getLogger(String loggerName) {
		JSimLogger mylogger = new JSimLogger(loggerName);
		return mylogger;
	}

	// printing methods:
	public void debug(Object message) {
		logger.debug(String.valueOf(message));
	}

	public void info(Object message) {
		logger.info(String.valueOf(message));
	}

	public void warn(Object message) {
		logger.warn(String.valueOf(message));
	}

	public void error(Object message) {
		logger.error(String.valueOf(message));
	}

	public void error(Throwable th) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		th.printStackTrace(pw);
		String buffer = sw.toString();
		logger.error(buffer);
	}

	public static void logCalenderOn() {
		LoggerStateManager.getInstance().setLogCalenderOn(true);
	}

	public static void logCalenderOff() {
		LoggerStateManager.getInstance().setLogCalenderOn(false);
	}

	public boolean isLogCalenderOn() {
		return LoggerStateManager.getInstance().isLogCalenderOn();
	}

	public void error(String message, Throwable th) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println(message);
		th.printStackTrace(pw);
		String buffer = sw.toString();
		logger.error(buffer);
	}

	public void fatal(Object message) {
		logger.error(String.valueOf(message));
	}

	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	// --- Following methods handles recreation of transient logger object after serialization ------------------
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		out.writeObject(logger.getName());
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		String name = (String) in.readObject();
		if (name == null) {
			logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		} else {
			logger = LoggerFactory.getLogger(name);
		}
	}
	// ----------------------------------------------------------------------------------------------------------

}
