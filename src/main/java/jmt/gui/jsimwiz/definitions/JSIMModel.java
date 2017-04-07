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

package jmt.gui.jsimwiz.definitions;

import jmt.gui.common.definitions.CommonModel;

/**
 * Created by IntelliJ IDEA.
 * User: orsotronIII
 * Date: 25-lug-2005
 * Time: 14.31.58
 * This is a variant of CommonModel that automatically add source and sink stations to model
 * as the first open class (if defined) is added to the model. If last open class is
 * deleted, source and sink stations are deleted as well. This class is designed for a
 * single-source and -sink model. Reference source for all open classes is this only source.
 * Modified by Bertoli Marco 4-oct-2005 / Fixed bugs while loading from file
 */
public class JSIMModel extends CommonModel {

	//keys for source and sink
	Object src;
	Object snk;

	//intercept each call for addition of a new class
	@Override
	public Object addClass(String name, int type, Integer priority, Integer population, Object distribution) {
		Object key = super.addClass(name, type, priority, population, distribution);
		if (type == CLASS_TYPE_OPEN) {
			if (getOpenClassKeys().size() == 1) {
				addSourceAndSink();
			}
			setClassRefStation(key, src);
		}
		return key;
	}

	//Intercept each call to the deletion of a class
	@Override
	public void deleteClass(Object key) {
		int type = getClassType(key);
		super.deleteClass(key);
		if (type == CLASS_TYPE_OPEN) {
			if (getOpenClassKeys().size() == 0) {
				deleteSourceAndSink();
			}
		}
	}

	//intercept each call for change of class type
	@Override
	public void setClassType(Object key, int type) {
		if (getClassType(key) == type) {
			return;
		}
		super.setClassType(key, type);
		if (type == CLASS_TYPE_OPEN) {
			if (getOpenClassKeys().size() == 1) {
				addSourceAndSink();
			}
			setClassRefStation(key, src);
		} else {
			if (getOpenClassKeys().size() == 0) {
				deleteSourceAndSink();
			}
		}
	}

	//intercept each call for addition of a new station
	@Override
	public Object addStation(String name, String type) {
		if (type.equals(STATION_TYPE_SOURCE)) {
			return src;
		} else if (type.equals(STATION_TYPE_SINK)) {
			return snk;
		} else {
			return super.addStation(name, type);
		}
	}

	//intercept each call for deletion of a station
	@Override
	public void deleteStation(Object key) {
		String type = getStationType(key);
		if (type.equals(STATION_TYPE_SOURCE) || type.equals(STATION_TYPE_SINK)) {
			return;
		}
		super.deleteStation(key);
	}

	//intercept each calls for change of station type
	@Override
	public void setStationType(Object key, String type) {
		String oldType = getStationType(key);
		if (oldType.equals(STATION_TYPE_SOURCE) || oldType.equals(STATION_TYPE_SINK)
				|| type.equals(STATION_TYPE_SOURCE) || type.equals(STATION_TYPE_SINK)) {
			return;
		}
		super.setStationType(key, type);
	}

	//addition of source and sink
	private void addSourceAndSink() {
		if (src == null) {
			src = super.addStation("Source", STATION_TYPE_SOURCE);
		}
		if (snk == null) {
			snk = super.addStation("Sink", STATION_TYPE_SINK);
		}
		stationsKeyset.remove(src);
		stationsKeyset.remove(snk);
		stationsKeyset.add(0, src);
		stationsKeyset.add(1, snk);
	}

	//deletion of source and sink
	private void deleteSourceAndSink() {
		if (src != null) {
			super.deleteStation(src);
			src = null;
		}
		if (snk != null) {
			super.deleteStation(snk);
			snk = null;
		}
	}
}
