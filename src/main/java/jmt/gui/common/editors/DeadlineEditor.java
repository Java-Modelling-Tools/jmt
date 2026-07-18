package jmt.gui.common.editors;

import java.awt.BorderLayout;
import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.common.editors.ImagedComboBoxCellEditorFactory;
import jmt.gui.common.panels.WarningScrollTable;

public class DeadlineEditor extends WizardPanel implements CommonConstants {

  private static final long serialVersionUID = 1L;

  protected Object stationKey;
  protected StationDefinition data;
  protected ClassDefinition classData;

  private DeadlineTable softDeadlineTable;

  /** Used to display classes with icon */
  protected ImagedComboBoxCellEditorFactory classEditor;

  public DeadlineEditor(StationDefinition sd, ClassDefinition cd, Object stationKey) {
    classEditor = new ImagedComboBoxCellEditorFactory(cd);
    setData(sd, cd, stationKey);
  }

  public void setData(StationDefinition sd, ClassDefinition cd, Object stationKey) {
    this.data = sd;
    this.classData = cd;
    this.stationKey = stationKey;
    this.softDeadlineTable = new DeadlineTable();
    initComponents();
  }

  private void initComponents() {
    //building mainPanel
    this.setLayout(new BorderLayout(5, 5));
    this.setBorder(new EmptyBorder(5, 5, 5, 5));

    JPanel descriptionPanel = new JPanel();
    descriptionPanel.setBorder(new TitledBorder(new EtchedBorder(), "Description"));
    JLabel softDeadlinesText = new JLabel("Set soft deadlines to compute response time earliness, tardiness, or lateness. If missed, the job keeps running.");
		descriptionPanel.add(softDeadlinesText);
    //  deadline panel
    WarningScrollTable warningScrollTable = new WarningScrollTable(softDeadlineTable, WARNING_CLASS);
    warningScrollTable.setBorder(new TitledBorder(new EtchedBorder(), "Soft Deadlines"));
    this.add(descriptionPanel, BorderLayout.NORTH);
    this.add(warningScrollTable, BorderLayout.CENTER);
  }

  @Override
  public String getName() {
    return "Deadlines";
  }


  @Override
  public void lostFocus() {
    // Abort editing of table
    TableCellEditor editor = softDeadlineTable.getCellEditor();
    if (editor != null) {
      editor.stopCellEditing();
    }
  }

  @Override
  public void gotFocus() {
    classEditor.clearCache();
  }

  protected class DeadlineTable extends JTable {

    private final int[] columnSizes = new int[]{90, 150};

    public DeadlineTable() {
      setModel(new DeadlineTableModel());
      sizeColumns();
      setRowHeight(ROW_HEIGHT);
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      getTableHeader().setReorderingAllowed(false);
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
      if (column == 0) {
        return classEditor.getRenderer();
      } else {
        return super.getCellRenderer(row, column);
      }
    }

    private void sizeColumns() {
      for (int i = 0; i < columnSizes.length && i < getColumnCount(); i++) {
        this.getColumnModel().getColumn(i).setPreferredWidth(columnSizes[i]);
      }
    }
  }

  protected class DeadlineTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private final String[] columnNames = new String[] { "Class", "Soft Deadline" };
    private final Class<?>[] columnClasses = new Class[] { String.class, Double.class };

    @Override
    public int getRowCount() {
      return classData.getClassKeys().size();
    }

    @Override
    public int getColumnCount() {
      return columnNames.length;
    }

    @Override
    public String getColumnName(int columnIndex) {
      return columnNames[columnIndex];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      return columnClasses[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return columnIndex == 1;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      Object classKey = classData.getClassKeys().get(rowIndex);
      switch (columnIndex) {
        case (0):
          return classKey;
        case (1):
          return data.getClassStationSoftDeadline(stationKey, classKey);
        default:
          return null;
      }
    }

    /**Puts edited values to the underlying data structure for model implementation*/
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      Object classKey = classData.getClassKeys().get(rowIndex);
      if (columnIndex == 1) {
        data.setClassStationSoftDeadline(stationKey, classKey, Math.max(0, (double) aValue));
      }
    }
  }
}
