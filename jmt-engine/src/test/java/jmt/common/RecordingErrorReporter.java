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

package jmt.common;

import java.util.ArrayList;
import java.util.List;

/** Test helper: records every call as "level|title|message". */
public class RecordingErrorReporter implements ErrorReporter {

	public final List<String> calls = new ArrayList<String>();

	@Override
	public void warning(String title, String message) {
		calls.add("warning|" + title + "|" + message);
	}

	@Override
	public void error(String title, String message) {
		calls.add("error|" + title + "|" + message);
	}
}
