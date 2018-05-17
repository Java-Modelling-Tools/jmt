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

package jmt.gui.exact.table;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.plaf.UIResource;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import jmt.gui.common.CommonConstants;

import jmt.gui.common.JMTImageLoader;

/**

 * @author alyf (Andrea Conti)
 * Date: 15-set-2003
 * Time: 20.39.53

 */

/**
 * a table with some improvements:
 * <li>has a click-aware header
 * <li>has a click-aware row header
 * <li>"more rows" and "more columns" warning icons
 * <li>clipboard transfer of cell values (support depends on the underlying data model) - CTRL+C, CTRL+X, CTRL+V
 * <li>can clear selected cells - DELETE
 */
public class ExactTable extends JTable {

	private static final long serialVersionUID = 1L;

	protected RowHeader rowHeader;
	protected int rowHeaderWidth = 80;
	protected JButton selectAllButton;
	protected JLabel moreColumnsLabel;

	protected JLabel moreRowsLabel;
	protected boolean displaysScrollLabels = true;
	protected boolean selectAllEnabled = true;
	protected boolean batchEditingEnabled = false;
	protected TableSelectionListener selectionListener;
	protected MouseHandler mouseHandler;
	protected boolean mouseMenuEnabled = true;

	protected Clipboard clipboard;

	protected String selectAllTooltip;//"Click to select the whole table";
	protected String moreColumnsTooltip;//"Scroll right to see more columns...";
	protected String moreRowsTooltip;//"Scroll down to see more rows...";

	protected AbstractAction selectAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

		{
			if (getRowSelectionAllowed() || getColumnSelectionAllowed()) {
				putValue(Action.SHORT_DESCRIPTION, selectAllTooltip);
				putValue(Action.NAME, "*");
			} else {
				putValue(Action.SHORT_DESCRIPTION, null);
				putValue(Action.NAME, " ");
			}
		}

		public void actionPerformed(ActionEvent e) {
			stopEditing();
			selectAll();
			requestFocus();
		}

	};

	/**
	 * updates the label state when the table is scrolled
	 */
	private ChangeListener scrollListener = new ChangeListener() {
		public void stateChanged(ChangeEvent e) {
			if (displaysScrollLabels) {
				updateScrollLabels();
			}
		}
	};

	public ExactTable(TableModel dm) {
		super(dm);

		/* apply default settings */
		setDefaultRenderer(Object.class, new ExactCellRenderer());
		setDefaultEditor(Object.class, new ExactCellEditor());

		/* try to resize the columns in a not-so-stupid way */
		sizeColumns();

		moreColumnsLabel = new JLabel();
		moreRowsLabel = new JLabel();
		selectAllButton = new JButton(selectAction);

		rowHeader = new RowHeader(dataModel);

		selectionModel.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		columnModel.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

		setColumnSelectionAllowed(true);
		setRowSelectionAllowed(true);
		setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

		installKeyboard();
		installMouse();

		selectionListener = new TableSelectionListener() {
			@Override
			protected void selectionChanged(JTable table, ListSelectionEvent e, int type) {
				updateActions();
			}
		};
		selectionListener.install(this);

		clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	}

	protected void installKeyboard() {
		InputMap im = getInputMap();
		ActionMap am = getActionMap();
		installKeyboardAction(im, am, CUT_ACTION);
		installKeyboardAction(im, am, COPY_ACTION);
		installKeyboardAction(im, am, PASTE_ACTION);
		installKeyboardAction(im, am, CLEAR_ACTION);
	}

	protected void installKeyboardAction(Action a) {
		installKeyboardAction(getInputMap(), getActionMap(), a);
	}

	protected void installKeyboardAction(InputMap im, ActionMap am, Action a) {
		Object name = a.getValue(Action.NAME);
		KeyStroke key = (KeyStroke) a.getValue(Action.ACCELERATOR_KEY);
		im.put(key, name);
		am.put(name, a);
	}

	protected void installMouse() {
		mouseHandler = new MouseHandler(makeMouseMenu());
		mouseHandler.install();
	}

	protected JPopupMenu makeMouseMenu() {
		JPopupMenu menu = new JPopupMenu();
		menu.add(CUT_ACTION);
		menu.add(COPY_ACTION);
		menu.add(PASTE_ACTION);
		menu.add(CLEAR_ACTION);
		return menu;
	}

	/**
	 * Overridden to make sure column sizes are maintained
	 */
	@Override
	public void tableChanged(TableModelEvent e) {
		super.tableChanged(e);
		if ((e == null || e.getFirstRow() == TableModelEvent.HEADER_ROW)) {
			sizeColumns();
		}
	}

	private void sizeColumns() {
		/* try and resize columns according to their header and (if available) prototype width*/
		/* taken from Sun's JTable tutorial, with some added features */
		int colnum = dataModel.getColumnCount();
		TableColumn col;
		Component comp;
		PrototypedTableModel ptm = null;
		boolean hasPrototypes = false;
		int autoWidth;

		if (dataModel instanceof PrototypedTableModel) {
			hasPrototypes = true;
			ptm = (PrototypedTableModel) dataModel;
		}

		if (tableHeader == null) {
			return; // hack: skip column sizing until we actually have a header
		}

		for (int i = 0; i < colnum; i++) {
			col = columnModel.getColumn(i);

			comp = tableHeader.getDefaultRenderer().getTableCellRendererComponent(null, col.getHeaderValue(), false, false, 0, i);
			autoWidth = comp.getPreferredSize().width;

			if (hasPrototypes) {
				//comp = getDefaultRenderer(dataModel.getColumnClass(i)).getTableCellRendererComponent(this, ptm.getPrototype(i), false, false, 0, i);
				comp = getDefaultRenderer(Object.class).getTableCellRendererComponent(this, ptm.getPrototype(i), false, false, 0, i);
				autoWidth = Math.max(autoWidth, comp.getPreferredSize().width);
			}

			col.setPreferredWidth(autoWidth);
		}
	}

	@Override
	public void setRowSelectionAllowed(boolean allowed) {
		super.setRowSelectionAllowed(allowed);
		allowed = getRowSelectionAllowed();
		if (rowHeader != null) {
			rowHeader.setAllowsClickRowSelection(allowed);
		}
		setSelectAllStatus(allowed || getColumnSelectionAllowed());
	}

	@Override
	public void setColumnSelectionAllowed(boolean allowed) {
		super.setColumnSelectionAllowed(allowed);
		allowed = getColumnSelectionAllowed();
		if (tableHeader instanceof ClickableTableHeader) {
			((ClickableTableHeader) tableHeader).setAllowsClickColumnSelection(allowed);
		}
		setSelectAllStatus(allowed || getRowSelectionAllowed());
	}

	public void setSelectAllStatus(boolean allowed) {
		if (allowed && !selectAllEnabled) {
			if (selectAction != null) {
				selectAction.setEnabled(true);
				selectAction.putValue(Action.NAME, "*");
				selectAction.putValue(Action.SHORT_DESCRIPTION, selectAllTooltip);
			}
			selectAllEnabled = true;
		} else if (!allowed && selectAllEnabled) {
			if (selectAction != null) {
				selectAction.setEnabled(false);
				selectAction.putValue(Action.NAME, " ");
				selectAction.putValue(Action.SHORT_DESCRIPTION, null);
			}
			selectAllEnabled = false;
		}
	}

	/**
	 * Overridden to return our nifty header
	 */
	@Override
	protected JTableHeader createDefaultTableHeader() {
		return new ClickableTableHeader(columnModel);
	}

	/**
	 * duplicated to avoid repeating the same checks multiple times
	 */
	@Override
	protected void configureEnclosingScrollPane() {
		Container p = getParent();
		if (p instanceof JViewport) {
			Container gp = p.getParent();
			if (gp instanceof JScrollPane) {
				JScrollPane scrollPane = (JScrollPane) gp;
				// Make certain we are the viewPort's view and not, for
				// example, the rowHeaderView of the scrollPane -
				// an implementor of fixed columns might do this.
				JViewport viewport = scrollPane.getViewport();
				if (viewport == null || viewport.getView() != this) {
					return;
				}
				scrollPane.setColumnHeaderView(getTableHeader());
				Border border = scrollPane.getBorder();
				if (border == null || border instanceof UIResource) {
					scrollPane.setBorder(UIManager.getBorder("Table.scrollPaneBorder"));
				}
				//rowHeader = new RowHeader(dataModel);
				rowHeader.setRowHeight(CommonConstants.ROW_HEIGHT);
				rowHeader.install(this, scrollPane);
				installLabels(scrollPane);
				installSelectAllButton(scrollPane);
				viewport.addChangeListener(scrollListener);
			}
		}
	}

	@Override
	protected void unconfigureEnclosingScrollPane() {
		Container p = getParent();
		if (p instanceof JViewport) {
			Container gp = p.getParent();
			if (gp instanceof JScrollPane) {
				JScrollPane scrollPane = (JScrollPane) gp;
				// Make certain we are the viewPort's view and not, for
				// example, the rowHeaderView of the scrollPane -
				// an implementor of fixed columns might do this.
				JViewport viewport = scrollPane.getViewport();
				if (viewport == null || viewport.getView() != this) {
					return;
				}
				scrollPane.setColumnHeaderView(null);
				scrollPane.setRowHeaderView(null);

				viewport.removeChangeListener(scrollListener);
				rowHeader.uninstall();
			}
		}
	}

	private void installSelectAllButton(JScrollPane scrollPane) {
		selectAllButton.setFocusable(false);
		selectAllButton.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
		scrollPane.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, selectAllButton);
	}

	private void installLabels(JScrollPane scrollPane) {
		moreColumnsLabel.setIcon(JMTImageLoader.loadImage("table_rightarrow"));
		moreColumnsLabel.setHorizontalAlignment(SwingConstants.CENTER);
		moreColumnsLabel.setToolTipText(moreColumnsTooltip);
		moreColumnsLabel.setVisible(false);

		moreRowsLabel.setIcon(JMTImageLoader.loadImage("table_downarrow"));
		moreRowsLabel.setHorizontalAlignment(SwingConstants.CENTER);
		moreRowsLabel.setToolTipText(moreRowsTooltip);
		moreRowsLabel.setVisible(false);

		scrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, moreColumnsLabel);
		scrollPane.setCorner(ScrollPaneConstants.LOWER_LEFT_CORNER, moreRowsLabel);

		if (displaysScrollLabels) {
			updateScrollLabels();
		}
	}

	/**
	 *  Make sure we're not editing a cell. NOTE: requires the editor to honor stopCellEditing() or
	 *  cancelCellEditing() calls.
	 */
	public void stopEditing() {
		if (cellEditor != null) {
			if (!cellEditor.stopCellEditing()) {
				cellEditor.cancelCellEditing();
			}
		}
	}

	/**
	 * selects a cell
	 */
	public void setSelectedCell(int row, int col) {
		setColumnSelectionInterval(col, col);
		setRowSelectionInterval(row, row);
	}

	/**
	 * Overridden to stop editing
	 */
	@Override
	public void selectAll() {
		stopEditing();
		super.selectAll();
	}

	/**
	 * Updates the state of the actions. called whenever the selection state of the table changes.
	 */
	protected void updateActions() {
		int cols = getSelectedColumnCount();
		int rows = getSelectedRowCount();
		boolean somethingSelected = (rows > 0 || cols > 0);
		CUT_ACTION.setEnabled(batchEditingEnabled && somethingSelected);
		COPY_ACTION.setEnabled(somethingSelected);
		PASTE_ACTION.setEnabled(batchEditingEnabled && somethingSelected && canPaste());
		CLEAR_ACTION.setEnabled(batchEditingEnabled && somethingSelected);
	}

	private void updateScrollLabels() {
		Rectangle vr = getVisibleRect();
		moreColumnsLabel.setVisible(vr.x + vr.width < getWidth());
		moreRowsLabel.setVisible(vr.y + vr.height < getHeight());
	}

	public void updateStructure() {
		tableChanged(new TableModelEvent(dataModel, TableModelEvent.HEADER_ROW));
		if (rowHeader != null) {
			rowHeader.updateStructure();
		}
	}

	public void update() {
		tableChanged(new TableModelEvent(dataModel));
		if (rowHeader != null) {
			rowHeader.update();
		}
	}

	public void updateRow(int row) {
		updateRows(row, row);
	}

	public void updateRows(int firstRow, int lastRow) {
		tableChanged(new TableModelEvent(dataModel, firstRow, lastRow));
		if (rowHeader != null) {
			rowHeader.updateRows(firstRow, lastRow);
		}
	}

	public int getRowHeaderWidth() {
		return rowHeaderWidth;
	}

	public void setRowHeaderWidth(int rowHeaderWidth) {
		this.rowHeaderWidth = rowHeaderWidth;
		if (rowHeader != null) {
			rowHeader.setWidth(rowHeaderWidth);
		}
	}

	public String getSelectAllTooltip() {
		return selectAllTooltip;
	}

	public void setSelectAllTooltip(String selectAllTooltip) {
		this.selectAllTooltip = selectAllTooltip;
		selectAction.putValue(Action.SHORT_DESCRIPTION, selectAllTooltip);
	}

	public String getMoreRowsTooltip() {
		return moreRowsTooltip;
	}

	public void setMoreRowsTooltip(String moreRowsTooltip) {
		this.moreRowsTooltip = moreRowsTooltip;
		if (moreRowsLabel != null) {
			moreRowsLabel.setToolTipText(moreRowsTooltip);
		}
	}

	public String getMoreColumnsTooltip() {
		return moreColumnsTooltip;
	}

	public void setMoreColumnsTooltip(String moreColumnsTooltip) {
		this.moreColumnsTooltip = moreColumnsTooltip;
		if (moreColumnsLabel != null) {
			moreColumnsLabel.setToolTipText(moreColumnsTooltip);
		}
	}

	public boolean displaysScrollLabels() {
		return displaysScrollLabels;
	}

	public void setDisplaysScrollLabels(boolean displaysScrollLabels) {
		this.displaysScrollLabels = displaysScrollLabels;
		if (!displaysScrollLabels) {
			moreColumnsLabel.setVisible(false);
			moreRowsLabel.setVisible(false);
		} else {
			updateScrollLabels();
		}
	}

	/**
	 * Try to keep the viewport aligned on column boundaries in the direction of interest
	 */
	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		if (orientation == SwingConstants.HORIZONTAL) {
			return 80;
		}
		return getRowHeight();
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		/* borrowed from JTable */
		if (orientation == SwingConstants.HORIZONTAL) {
			return 5 * getScrollableUnitIncrement(visibleRect, orientation, direction);
		}
		return super.getScrollableBlockIncrement(visibleRect, orientation, direction);
	}

	public boolean isBatchEditingEnabled() {
		return batchEditingEnabled;
	}

	public void setBatchEditingEnabled(boolean batchEditingEnabled) {
		this.batchEditingEnabled = batchEditingEnabled;
	}

	public boolean isMouseMenuEnabled() {
		return mouseMenuEnabled;
	}

	public void setMouseMenuEnabled(boolean mouseMenuEnabled) {
		this.mouseMenuEnabled = mouseMenuEnabled;
	}

	/* cut/copy/paste/clear stuff -------------------------------------*/

	public void copyCells() {
		int numrows = getSelectedRowCount();
		int numcols = getSelectedColumnCount();
		int[] rowsselected = getSelectedRows();
		int[] colsselected = getSelectedColumns();
		if (!((numrows - 1 == rowsselected[rowsselected.length - 1] - rowsselected[0] && numrows == rowsselected.length) && (numcols - 1 == colsselected[colsselected.length - 1]
				- colsselected[0] && numcols == colsselected.length))) {
			JOptionPane.showMessageDialog(null, "Invalid Copy Selection", "Invalid Copy Selection", JOptionPane.ERROR_MESSAGE);
			return;
		}

		StringBuffer sbf = new StringBuffer();
		for (int i = 0; i < numrows; i++) {
			for (int j = 0; j < numcols; j++) {
				sbf.append(getValueAt(rowsselected[i], colsselected[j]));
				if (j < numcols - 1) {
					sbf.append("\t");
				}
			}
			sbf.append("\n");
		}
		StringSelection ssel = new StringSelection(sbf.toString());
		clipboard.setContents(ssel, ssel);
	}

	public void clearCells() {
		int numrows = getSelectedRowCount();
		int numcols = getSelectedColumnCount();
		int[] rowsselected = getSelectedRows();
		int[] colsselected = getSelectedColumns();
		if (!((numrows - 1 == rowsselected[rowsselected.length - 1] - rowsselected[0] && numrows == rowsselected.length) && (numcols - 1 == colsselected[colsselected.length - 1]
				- colsselected[0] && numcols == colsselected.length))) {
			JOptionPane.showMessageDialog(null, "Invalid Clear Selection", "Invalid Clear Selection", JOptionPane.ERROR_MESSAGE);
			return;
		}

		boolean hasClear = false;
		ExactTableModel etm = null;
		if (dataModel instanceof ExactTableModel) {
			hasClear = true;
			etm = (ExactTableModel) dataModel;
		}
		for (int i = 0; i < numrows; i++) {
			for (int j = 0; j < numcols; j++) {
				if (hasClear) {
					etm.clear(rowsselected[i], colsselected[j]);
				} else {
					setValueAt(null, rowsselected[i], colsselected[j]);
				}
			}
		}
		updateRows(rowsselected[0], rowsselected[numrows - 1]);
	}

	public void pasteCells() {
		if (!canPaste()) {
			return;
		}
		int numrows = getSelectedRowCount();
		int numcols = getSelectedColumnCount();
		int[] rowsselected = getSelectedRows();
		int[] colsselected = getSelectedColumns();
		if (!((numrows - 1 == rowsselected[rowsselected.length - 1] - rowsselected[0] && numrows == rowsselected.length) && (numcols - 1 == colsselected[colsselected.length - 1]
				- colsselected[0] && numcols == colsselected.length))) {
			JOptionPane.showMessageDialog(null, "Invalid Paste Selection", "Invalid Paste Selection", JOptionPane.ERROR_MESSAGE);
			return;
		}

		try {
			String s = ((String) (clipboard.getContents(this).getTransferData(DataFlavor.stringFlavor))).trim();
			String[] rows = s.contains(";") ? s.split("\\s*;\\s*") : s.split("\\s*\\n\\s*");
			if (numrows == 1 && numcols == 1) {
				numrows = Math.min(rows.length, getRowCount() - rowsselected[0]);
				for (int i = 0; i < numrows; i++) {
					String row = rows[i];
					String[] cells = row.contains(",") ? row.split("\\s*,\\s*") : row.split("\\s+");
					numcols = Math.min(cells.length, getColumnCount() - colsselected[0]);
					for (int j = 0; j < numcols; j++) {
						setValueAt(cells[j], rowsselected[0] + i, colsselected[0] + j);
					}
				}
				setRowSelectionInterval(rowsselected[0], rowsselected[0] + numrows - 1);
				setColumnSelectionInterval(colsselected[0], colsselected[0] + numcols - 1);
			} else {
				for (int i = 0; i < numrows; i++) {
					String row = rows[i % rows.length];
					String[] cells = row.contains(",") ? row.split("\\s*,\\s*") : row.split("\\s+");
					for (int j = 0; j < numcols; j++) {
						setValueAt(cells[j % cells.length], rowsselected[0] + i, colsselected[0] + j);
					}
				}
			}
			updateRows(rowsselected[0], rowsselected[0] + numrows - 1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean canPaste() {
		return clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor);
	}

	public AbstractAction CUT_ACTION = new AbstractAction("Cut") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK, false));
			putValue(Action.SHORT_DESCRIPTION, "Copies selected cells to the system clipboard and clears them");
		}

		public void actionPerformed(ActionEvent e) {
			copyCells();
			clearCells();
		}

	};

	public AbstractAction COPY_ACTION = new AbstractAction("Copy") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK, false));
			putValue(Action.SHORT_DESCRIPTION, "Copies selected cells to the system clipboard");
		}

		public void actionPerformed(ActionEvent e) {
			copyCells();
		}

	};

	public AbstractAction PASTE_ACTION = new AbstractAction("Paste") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK, false));
			putValue(Action.SHORT_DESCRIPTION, "Pastes cells from the system clipboard, starting from the currently focused cell");
		}

		public void actionPerformed(ActionEvent e) {
			pasteCells();
		}

	};

	public AbstractAction CLEAR_ACTION = new AbstractAction("Clear") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, false));
			putValue(Action.SHORT_DESCRIPTION, "Clears selected cells");
		}

		public void actionPerformed(ActionEvent e) {
			clearCells();
		}

	};

	protected class MouseHandler extends MouseAdapter {

		private boolean isInstalled;
		private JPopupMenu menu;

		public MouseHandler(JPopupMenu menu) {
			this.menu = menu;
		}

		public void install() {
			if (!isInstalled) {
				addMouseListener(this);
				isInstalled = true;
			}
		}

		public void uninstall() {
			if (isInstalled) {
				removeMouseListener(this);
				isInstalled = false;
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			processME(e);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			processME(e);
		}

		protected void processME(MouseEvent e) {
			if (!mouseMenuEnabled) {
				return;
			}
			if (e.isPopupTrigger()) {
				updateActions();
				menu.show(e.getComponent(), e.getX(), e.getY());
			}
		}

		public JPopupMenu getMenu() {
			return menu;
		}

	}

}
