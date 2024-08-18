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
package jmt.jmch.wizard.panels.resultsPanel;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.TableCellRenderer;


import jmt.framework.data.ArrayUtils;
import jmt.framework.gui.table.editors.ButtonCellEditor;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.JMTImageLoader;
import jmt.gui.table.DisabledCellRenderer;
import jmt.gui.table.ExactTable;
import jmt.gui.table.ExactTableModel;
import jmt.gui.table.ListOp;
import jmt.jmch.Constants;
import jmt.jmch.wizard.MainWizard;

/**
 * Panel for showing the results of the Scheduling simulations
 *
 * @author Lorenzo Torri
 * Date: 29-apr-2024
 * Time: 20.10
 */
public class ResultsPanelScheduling extends ResultsPanel{
    //------------ variables of the TABLE
    private ResultTable table;
    private int nResults = 0; //n of Simulations, which is also the number of rows of the table
    

    private List<ListOp> classOps; //for keeping track of row deletions and insertions
	private boolean deleting = false;

    // Column numbers
	private final static int COL_ALGO = 0;
	private final static int COL_DISTR_ARRIVAL = 1;
	private final static int COL_LAMBDA = 2;
	private final static int COL_DISTR_SERVICE = 3;
    private final static int COL_NSERVERS = 4;
	private final static int COL_SERVICE = 5;
    private final static int COL_R = 6;
    private final static int COL_QUEUETIME = 7; 
    private final static int COL_NQUEUE = 8;
    private final static int COL_THROUGHPUT = 9;
	private final static int COL_DELETE_BUTTON = 10;

    //for each column, an array of values
    private String[] algorithms = new String[0];
    private String[] arrivalDistibutions = new String[0];
    private double[] lambdas = new double[0];
    private String[] serviceDistributions = new String[0];
    private int[] serversNumber = new int[0];
    private double[] services = new double[0];
    private double[] responseTimes = new double[0];
    private double[] queueTimes = new double[0];
    private double[] thoughputs = new double[0];
    private double[] nCustomers = new double[0];

    /** This Action is only for displaying the X in each row */
    private AbstractAction deleteOneResult = new AbstractAction("") {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Delete This Simulation");
			putValue(Action.SMALL_ICON, JMTImageLoader.loadImage("Delete"));
		}

		public void actionPerformed(ActionEvent e) {
		}
	};

    /** This action is for deleting more rows in one time */
    private AbstractAction deleteResults = new AbstractAction("Delete selected classes") {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		{
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, false));
			putValue(Action.SHORT_DESCRIPTION, "Deletes selected classes from the system");
		}

		public void actionPerformed(ActionEvent e) {
			deleteSelectedRows();
		}
	};

    /**
	 * Method called by the MainWizard to update the Result Panel
	 * @param algorithm queue algorithm of the animation
	 * @param arrivalDistr arrival time distribution
	 * @param lambda the inter arrival time
	 * @param serviceDistr service time distribution
     * @param nServers number of servers in the station
	 * @param service service time
	 * @param responseTime response time
	 * @param queueTime queue time
	 * @param thoughput thoughput
	 * @param nCustomers customer numbers
	 */
    public void addResult(String algorithm, String arrivalDistr, double lambda, String serviceDistr, int nServers, double service, double responseTime, double queueTime, double thoughput, double nCustomer){
        setNumberOfResults(nResults+1, algorithm, arrivalDistr, lambda, serviceDistr, nServers, service, responseTime, queueTime, thoughput, nCustomer);
    }

    private void addRow() {
		//setNumberOfResults(nResults + 1);
	}

    /** Change the size of data structures updating also the table */
    private void setNumberOfResults(int number, String algorithm, String arrivalDistr, double lambda, String serviceDistr, int nServers, double service, double responseTime, double queueTime, double thoughput, double nCustomer) {
		table.stopEditing();
		nResults = number;

        //resize the arrays
		algorithms = ArrayUtils.resize(algorithms, nResults, null);
        arrivalDistibutions = ArrayUtils.resize(arrivalDistibutions, nResults, null);
        lambdas = ArrayUtils.resize(lambdas, nResults, 0.0);
        serviceDistributions = ArrayUtils.resize(serviceDistributions, nResults, null);
        serversNumber = ArrayUtils.resize(serversNumber, nResults, 0);
        services = ArrayUtils.resize(services, nResults, 0.0);
        responseTimes = ArrayUtils.resize(responseTimes, nResults, 0.0);
        queueTimes = ArrayUtils.resize(queueTimes, nResults, 0.0);
        thoughputs = ArrayUtils.resize(thoughputs, nResults, 0);
        nCustomers = ArrayUtils.resize(nCustomers, nResults, 0);

        //add new values
        algorithms[nResults-1] = algorithm;		
        arrivalDistibutions[nResults-1] = arrivalDistr;
        lambdas[nResults-1] = lambda;
        serviceDistributions[nResults-1] = serviceDistr;
        serversNumber[nResults-1] = nServers;
        services[nResults-1] =  service;
        responseTimes[nResults-1] = responseTime;
        queueTimes[nResults-1] = queueTime;
        thoughputs[nResults-1] = thoughput;
        nCustomers[nResults-1] = nCustomer;
       
		updateTable();
	}

    /** Update the changes on the table */
    private void updateTable(){
        table.updateStructure();
		if (!deleting) {
			classOps.add(ListOp.createResizeOp(nResults));
		}

		table.updateDeleteCommand();
    }


    public ResultsPanelScheduling(MainWizard main){
        super(main);
        classOps = new ArrayList<ListOp>();

        initGUI();
    }

    public void initGUI(){
        this.setLayout(new BorderLayout());

        table = new ResultTable();

        //----upper part
        Box introductionBox = Box.createHorizontalBox();
        JLabel label = new JLabel(Constants.INTRODUCTION_RESULTS);
        introductionBox.add(label);
 
        //----center part
        Box resultBox = Box.createVerticalBox();
        resultBox.add(Box.createVerticalStrut(5));
		resultBox.add(introductionBox);
		resultBox.add(Box.createVerticalStrut(10));
        JScrollPane tablePane = new JScrollPane(table);
		tablePane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		tablePane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        resultBox.add(tablePane);
		resultBox.add(Box.createVerticalStrut(1));

        Box totalBox = Box.createHorizontalBox(); //this box is for adding also the horizontal padding
		totalBox.add(Box.createHorizontalStrut(20));
		totalBox.add(resultBox);
		totalBox.add(Box.createHorizontalStrut(20));
        this.add(totalBox, BorderLayout.CENTER);

        /*----lower part
        Box numberBox = Box.createVerticalBox();
        numberBox.add(new JButton(addResult));
        this.add(numberBox, BorderLayout.SOUTH); */
        
		
        addRow();
    }

    /** Adds the possibility of deleting multiple rows by selecting them with 2 clicks of the mouse + Shift */
    private void deleteSelectedRows() {
		int[] selectedRows = table.getSelectedRows();
		int nrows = selectedRows.length;
		int left = table.getRowCount() - nrows;
		if (left < 0) { // 1 if you want that at least one row is in the table
			table.removeRowSelectionInterval(selectedRows[nrows - 1], selectedRows[nrows - 1]);
			deleteSelectedRows();
			return;
		}
		deleteResults(selectedRows);
	}

    /** 
     * Remove selected Rows from arrays 
     * @param idx, array of indices of the rows selected to be deleted
     */
	private void deleteResults(int[] idx) {
		deleting = true;
		Arrays.sort(idx);
		for (int i = idx.length - 1; i >= 0; i--) {
			deleteResult(idx[i]);
		}
		updateTable();
		deleting = false;
	}

    /** 
     * Delete a single Result 
     * @param i, the index of the row to be deleted
     */
	private void deleteResult(int i) {
		nResults--;

        algorithms = ArrayUtils.delete(algorithms, i);
		arrivalDistibutions = ArrayUtils.delete(arrivalDistibutions, i);
		lambdas = ArrayUtils.delete(lambdas, i);
		serviceDistributions = ArrayUtils.delete(serviceDistributions, i);
        serversNumber = ArrayUtils.delete(serversNumber, i);
        services = ArrayUtils.delete(services, i);
        responseTimes = ArrayUtils.delete(responseTimes, i);
        queueTimes = ArrayUtils.delete(queueTimes, i);
        thoughputs = ArrayUtils.delete(thoughputs, i);
        nCustomers = ArrayUtils.delete(nCustomers, i);

		classOps.add(ListOp.createDeleteOp(i));
	}

    /* 
    private void updateSizes() {
		setNumberOfResults(nResults);
	} */

    public int getRows(){
        return nResults;
    }

    //END of ResultsPanel logic

    /*
        ------------------------------------------------------------------
        ResultTable needs access to the data structures of the ResultPanel,
        so having it as an inner class is *much* more practical
        ------------------------------------------------------------------
    */

    /*
     * The big table
     */
    private class ResultTable extends ExactTable {

        TableCellRenderer disabledCellRenderer;
        JButton deleteButton;
        ButtonCellEditor deleteButtonCellRenderer;

        public ResultTable() {
            super(new ResultsTableModel());
            setName("ResultTable");
			disabledCellRenderer = new DisabledCellRenderer();
            
			deleteButton = new JButton(deleteOneResult);
			deleteButtonCellRenderer = new ButtonCellEditor(deleteButton);
			enableDeletes();
			rowHeader.setRowHeight(CommonConstants.ROW_HEIGHT);
			setRowHeight(CommonConstants.ROW_HEIGHT);

            setDefaultRenderer(DisabledCellRenderer.class, disabledCellRenderer);

            setRowSelectionAllowed(true);
			setColumnSelectionAllowed(false);

            installKeyboardAction(getInputMap(), getActionMap(), deleteResults); //add the action of deleting a row 

            //add popup menu when right clicking
			mouseHandler = new ExactTable.MouseHandler(makeMouseMenu());
			mouseHandler.install();

            help.addHelp(this,
					"Click or drag to select classes. Right-click for a list of available operations");
			help.addHelp(moreRowsLabel, "There are more classes: scroll down to see them");
			help.addHelp(selectAllButton, "Click to select all classes");
            help.addHelp(tableHeader, "Parameters of the simulation");
			tableHeader.setToolTipText(null);
			rowHeader.setToolTipText(null);
			help.addHelp(rowHeader, "Number of simulations");
        }  

        @Override
		public TableCellRenderer getCellRenderer(int row, int column) {
            //different renderers based on the column
            if (column == COL_DELETE_BUTTON) {
				return deleteButtonCellRenderer;
			} else {
				return disabledCellRenderer;
			}
		}

        /*enables deleting operations with last column's button*/
		private void enableDeletes() {
			deleteOneResult.setEnabled(nResults > 0); // 1 if you want that at least one row is in the table
			
			this.addMouseListener(new MouseAdapter() { //detection of the rows selected
				@Override
				public void mouseClicked(MouseEvent e) {
					if ((columnAtPoint(e.getPoint()) == getColumnCount() - 1) && getRowCount() > 0) { // 1 if you want that at least one row is in the table
						setRowSelectionInterval(rowAtPoint(e.getPoint()), rowAtPoint(e.getPoint()));
						deleteSelectedRows();
					}
				}
			});
			getColumnModel().getColumn(getColumnCount() - 1).setMinWidth(20);
			getColumnModel().getColumn(getColumnCount() - 1).setMaxWidth(20);
		}

        @Override
		protected JPopupMenu makeMouseMenu() { //this is for popping the menu when right clicking on selected rows
			JPopupMenu menu = new JPopupMenu();
			menu.add(deleteResults);
			return menu;
		}

        /** Called by ResultPanel to update the Delete Buttons */
        protected void updateDeleteCommand() {
			deleteOneResult.setEnabled(nResults > 0); // 1 if you want that at least one row is in the table
			getColumnModel().getColumn(getColumnCount() - 1).setMinWidth(20);
			getColumnModel().getColumn(getColumnCount() - 1).setMaxWidth(20);
		} 

        @Override
		protected void updateActions() {
			boolean isEnabled = nResults > 0 && getSelectedRowCount() > 0; // 1 if you want that at least one row is in the table
			deleteResults.setEnabled(isEnabled);
			deleteOneResult.setEnabled(nResults > 0); // 1 if you want that at least one row is in the table
		}
    }

    /** The model for the table */
    private class ResultsTableModel extends ExactTableModel{
        private final int nColumns = 11;

        //------------------------------index, algorithm, distribution arrival, --------lambda, distribution service, ----nServers, service, response time, queue times, ncustomers, thoughput, delete
        private Object[] prototypes = { "100", "------", "------------------", "", "------------------", "", "", "", "", "", "", "", "" };

		@Override
		public Object getPrototype(int columnIndex) {
			return prototypes[columnIndex + 1];
		}

        @Override
        public String getColumnName(int index) {
            switch (index) {
                case COL_ALGO:
                    return "Scheduling Algo.";
                case COL_DISTR_ARRIVAL:
                    return "Arrival Distr.";
                case COL_LAMBDA:
                    return "\u03BB";
                case COL_DISTR_SERVICE:
                    return "Service Distr.";
                case COL_NSERVERS:
                    return "N.Servers";
                case COL_SERVICE:
                    return "S";
                case COL_R:
                    return "R";
                case COL_QUEUETIME:
                    return "Q";
                case COL_NQUEUE:
                    return "N";
                case COL_THROUGHPUT:
                    return "X";                 
                default:
                    return null;
            }
        }

        @Override
        public int getRowCount() {
            return nResults;
        }

        @Override
        public int getColumnCount() {
            return nColumns;
        }

        @Override
        protected Object getValueAtImpl(int rowIndex, int columnIndex) {
            switch (columnIndex) { //select the column, then the value in the row
                case COL_ALGO:
                    return algorithms[rowIndex];
                case COL_DISTR_ARRIVAL:
                    return arrivalDistibutions[rowIndex];
                case COL_LAMBDA:
                    return lambdas[rowIndex];
                case COL_DISTR_SERVICE:
                    return serviceDistributions[rowIndex];
                case COL_NSERVERS:
                    return serversNumber[rowIndex];
                case COL_SERVICE:
                    return services[rowIndex];
                case COL_R:
                    return responseTimes[rowIndex];
                case COL_QUEUETIME:
                    return queueTimes[rowIndex];             
                case COL_NQUEUE:
                    return nCustomers[rowIndex];  
                case COL_THROUGHPUT:
                    return thoughputs[rowIndex];       
                default:
                    return null;
            }
        }

        @Override
        protected Object getRowName(int rowIndex) {
            return rowIndex+1;
        }

    }
}


