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

package jmt.gui.jsimgraph.template;

import jmt.gui.common.Defaults;

/**
 * @author S Jiang
 * 
 */
public interface TemplateConstants {

	public static final String TEMPLATE_FOLDER = Defaults.getWorkingPath().getAbsolutePath() + "/templates/";

	public static final String F_NAME_INDEX = "index.xml";
	public static final String F_NAME_SITES = "sites.xml";

	public static final String SITE_FILE = TEMPLATE_FOLDER + F_NAME_SITES;

	public static final String D_TYPE_INDEX = "index";
	public static final String D_TYPE_TEMPLATE = "template";

	public static final int CONN_TIMEOUT = 10000;
	public static final int READ_TIMEOUT = 10000;

	public static final int CONN_SHORT_TIMEOUT = 3000;
	public static final int READ_SHORT_TIMEOUT = 3000;

	public static final String MF_NAME = "Name";
	public static final String MF_FILE_NAME = "File-name";
	public static final String MF_AUTHOR = "Author";
	public static final String MF_VERSION = "Version";
	public static final String MF_DATE = "Date";
	public static final String MF_SHORT_DESCRIPTION = "Short-description";
	public static final String MF_TOOLTIP = "Tool-tip";
	public static final String MF_UPDATE_ADDRESS = "Update-address";
	public static final String MF_DESCRIPTION_ADDRESS = "Description-address";
	public static final String MF_MD5 = "Md5";
	public static final String MF_DEFAULT = "Default";

	public static final String DEFAULT_SITE = "http://jmt.sourceforge.net/jtemplates";

	public static final String[] CONN_TEST_ADDRESSES = {"http://jmt.sourceforge.net", "https://www.google.co.uk"};

	public static final int CELL_HEIGHT = 30;

}
