package jwat.workloadAnalysis.tables;

import java.awt.Color;

import javax.swing.JTable;

import common.CommonConstants;

/**
 * Table showing the j wat bivariate stats.
 */
/**
 * Table showing the j wat bivariate stats.
 */
/**
 * Table showing the j wat bivariate stats.
 */
/**
 * Table showing the j wat bivariate stats.
 */
public class JWatBivariateStatsTable extends JTable implements CommonConstants {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public JWatBivariateStatsTable() {
		setSelectionBackground(new Color(83, 126, 126));
		setSelectionForeground(Color.BLACK);
	}

	// Sets a table model for visualization and editing of data
	public void setModel(JWatBivariateStatsTableModel tabMod) {
		super.setModel(tabMod);
		setRowHeight(ROW_HEIGHT);
		this.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
	}
}
