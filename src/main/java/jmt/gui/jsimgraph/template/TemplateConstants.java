package jmt.gui.jsimgraph.template;

import jmt.gui.common.Defaults;

public interface TemplateConstants {

	public static final String TEMPLATE_FOLDER = Defaults.getWorkingPath()
			.getAbsolutePath() + "/templates/";

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
	
	public static final String[] CONN_TEST_ADDRESSES = 
		{"http://jmt.sourceforge.net", "https://www.google.co.uk"};
	
	public static final int CELL_HEIGHT = 30;

}
