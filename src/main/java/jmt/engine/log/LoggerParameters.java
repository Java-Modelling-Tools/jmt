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

package jmt.engine.log;

import jmt.framework.data.MacroReplacer;

public class LoggerParameters {

	public volatile boolean isEnabled; // the variable is left public for fast external read access - do not write to it.
	public String name;
	public String path;
	public Boolean boolExecTimestamp;
	public Boolean boolLoggername;
	public Boolean boolTimeStamp;
	public Boolean boolJobID;
	public Boolean boolJobClass;
	public Boolean boolTimeSameClass;
	public Boolean boolTimeAnyClass;

	/* Constants for name passing */
	public static final int LOGGER_AR_ASK = 0;
	public static final int LOGGER_AR_REPLACE = 1;
	public static final int LOGGER_AR_APPEND = 2;
	public static final String GLOBALLOGNAME = "global.csv";

	/**
	 * Creates a logger with default parameters.
	 */
	public LoggerParameters() {
		name = new String(GLOBALLOGNAME);
		path = MacroReplacer.replace(MacroReplacer.MACRO_WORKDIR);
		boolExecTimestamp = new Boolean(false);
		boolLoggername = new Boolean(true);
		boolTimeStamp = new Boolean(true);
		boolJobID = new Boolean(true);
		boolJobClass = new Boolean(false);
		boolTimeSameClass = new Boolean(false);
		boolTimeAnyClass = new Boolean(false);
		enable();
	}

	/**
	 * Creates a logger with most parameters specified as a string.
	 * Used by jmt.engine.NodeSections.LogTunnel class.
	 */
	public LoggerParameters(String FN, String FP, Boolean ET, Boolean LN, Boolean TS, Boolean JID, Boolean JC, Boolean TSC, Boolean TAC) {
		name = FN;
		path = FP;
		boolExecTimestamp = ET;
		boolLoggername = LN;
		boolTimeStamp = TS;
		boolJobID = JID;
		boolJobClass = JC;
		boolTimeSameClass = TSC;
		boolTimeAnyClass = TAC;
		enable();
	}

	/**
	 * Returns a deep copy of this object.
	 * @return A deep copy of this object.
	 */
	@Override
	public Object clone() {
		LoggerParameters lp = new LoggerParameters(name, path, boolExecTimestamp, boolLoggername, boolTimeStamp, boolJobID, boolJobClass, boolTimeSameClass, boolTimeAnyClass);
		lp.isEnabled = isEnabled;
		return lp;
	}

	/**
	 * @returns String representation of all variables of LoggerParameters instance.
	 */
	@Override
	public final String toString() {
		String s = "";
		s += isEnabled() ? "1" : "0";
		s += "|";
		s += boolExecTimestamp.booleanValue() ? "1" : "0";
		s += boolLoggername.booleanValue() ? "1" : "0";
		s += boolTimeStamp.booleanValue() ? "1" : "0";
		s += boolJobID.booleanValue() ? "1" : "0";
		s += boolJobClass.booleanValue() ? "1" : "0";
		s += boolTimeSameClass.booleanValue() ? "1" : "0";
		s += boolTimeAnyClass.booleanValue() ? "1" : "0";
		s += "|";
		s += name;
		s += "|";

		return s;
	}

	/**
	 * Convenience: Calculates if the logger should log to a file
	 * @return Returns <em>true</em> if (1) the logger is enabled, and (2) if there is something to log
	 */
	public boolean isEnabled() {
		try {
			if (!isEnabled) {
				return isEnabled;
			}

			if (!boolExecTimestamp.booleanValue() && !boolLoggername.booleanValue() && !boolTimeStamp.booleanValue()
					&& !boolJobID.booleanValue() && !boolJobClass.booleanValue() && !boolTimeSameClass.booleanValue()
					&& !boolTimeAnyClass.booleanValue()) {
				this.disable();
				return isEnabled;
			}
		} catch (NullPointerException npe) {
			this.disable();
			npe.printStackTrace();
			return isEnabled;
		}

		// The above proves there is no disabling reason, so enable.
		return isEnabled;
	}

	public boolean enable() {
		isEnabled = true;
		return isEnabled();
	}

	public void disable() {
		isEnabled = false;
	}

	public boolean isGlobal() {
		if (name.equalsIgnoreCase(GLOBALLOGNAME)) {
			return true;
		} else {
			return false;
		}
	}

}
