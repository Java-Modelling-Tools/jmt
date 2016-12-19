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

package jmt.gui.jsimgraph.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.MatteBorder;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.JMTImageLoader;
import jmt.gui.common.controller.ModelChecker;
import jmt.gui.common.definitions.GuiInterface;

/**
 * <p>Title:</p>
 * <p>Description:</p>
 *
 * @author Francesco D'Aquino
 *         Date: 13-ott-2005
 *         Time: 14.36.31
 *         
 * Modified by Ashanka (May 2010): 
 * Patch: Multi-Sink Perf. Index 
 * Description: Added new Performance index for the capturing the 
 * 				1. global response time (ResponseTime per Sink)
 *              2. global throughput (Throughput per Sink)
 *              each sink per class.
 * Hence new validations are required to check the Performance Indices of
 * response per sink and throughput per sink follow the model validations.
 * 1. Response Time per Sink and Throughput per Sink should have a sink in
 * the model : added new function : isThereSinkPerfIndicesError
 * 2. Response Time per Sink and Throughput per Sink should not be selected
 * with a closed class because for a closed model as of now in JMT no jobs 
 * are routed to the it. So sink should be chosen only when a open class 
 * is present : isSinkPerfIndicesWithClosedClassError.
 * 
 * Modified by Ashanka (June 2010):
 * Updated the Manage Probabilities. Closed Classes routed to Sink with probability <> 0.0 should show warning to Users.
 * 
 * Modified by Mattia Cazzoli
 * Long messages are now visualized on multilple lines.
 * 
 */
public class JModelProblemsWindow extends JDialog {

	private static final long serialVersionUID = 1L;

	private static final int DESCRIPTION_HTML_WIDTH = 662;

	GuiInterface gi;

	private boolean canBeRun;
	private boolean operationCanceled;

	boolean isToJMVAConversion;

	JLabel title;
	JList problemsList;
	ModelChecker mc;
	DefaultListModel<ProblemElement> problems;

	GridBagLayout gblayout;
	GridBagConstraints gbconstants;

	JButton continueButton;
	JButton cancelButton;

	JButton typeButton;
	JButton descriptionButton;

	public JModelProblemsWindow(Frame owner, ModelChecker checker, GuiInterface gi) {
		super(owner, true);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		problems = new DefaultListModel<ProblemElement>();
		mc = checker;
		isToJMVAConversion = mc.isToJMVA();
		this.gi = gi;
		canBeRun = false;
		operationCanceled = true;
		GridBagLayout gblayout = new GridBagLayout();
		GridBagConstraints gbconstants = new GridBagConstraints();
		getContentPane().setLayout(gblayout);

		if (isToJMVAConversion) {
			this.setTitle("Problems while trying to convert to JMVA");
		} else {
			this.setTitle("Simulation diagnostic");
		}
		setBounds(200, 200, 200, 200);
		setMinimumSize(new Dimension(1000, 100));
		title = new JLabel(CommonConstants.HTML_START + CommonConstants.HTML_FONT_TITLE + "Problems found" + CommonConstants.HTML_FONT_TIT_END
				+ CommonConstants.HTML_FONT_NORM + "Click on an element to solve the problem" + CommonConstants.HTML_FONT_NOR_END
				+ CommonConstants.HTML_END);

		problemsList = new JList();
		initializeList();
		problemsList.setModel(problems);
		problemsList.setCellRenderer(new ProblemElementRenderer());
		problemsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		problemsList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		problemsList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				setVisible(false);
				ProblemElement temp = (ProblemElement) problemsList.getSelectedValue();
				int pType = temp.getProblemType();
				int pSubType = temp.getProblemSubType();
				getRelatedPanel(pType, pSubType, temp.getRelatedStationKey(), temp.getRelatedClassKey());
				problems.removeAllElements();
				mc.checkModel();
				initializeList();
				if (isToJMVAConversion) {
					if (mc.isErrorFreeToJMVA()) {
						continueButton.setEnabled(true);
					}
					if (pType == ModelChecker.ERROR_PROBLEM
							&& (pSubType == ModelChecker.NO_STATION_ERROR
									|| pSubType == ModelChecker.OPEN_CLASS_BUT_NO_SOURCE_ERROR)) {
						setVisible(false);
					} else {
						if (!mc.isEverythingOkToJMVA()) {
							setVisible(true);
						}
					}
				} else {
					if (mc.isErrorFreeNormal()) {
						continueButton.setEnabled(true);
					}
					if (((pType == ModelChecker.ERROR_PROBLEM)
							&& ((pSubType == ModelChecker.NO_STATION_ERROR)
									|| (pSubType == ModelChecker.ALL_FORWARD_STATION_ARE_SINK_ERROR)
									|| (pSubType == ModelChecker.OPEN_CLASS_BUT_NO_SOURCE_ERROR)
									|| (pSubType == ModelChecker.OPEN_CLASS_BUT_NO_SINK_ERROR)
									|| (pSubType == ModelChecker.STATION_LINK_ERROR)
									|| (pSubType == ModelChecker.JOIN_WITHOUT_FORK_ERROR)
									|| (pSubType == ModelChecker.CS_FOLLOWED_BY_A_BAS)
									|| (pSubType == ModelChecker.SEMAPHORE_NOT_BETWEEN_FORK_JOIN)
									|| (pSubType == ModelChecker.SCALER_NOT_BETWEEN_FORK_JOIN)))
							|| ((pType == ModelChecker.WARNING_PROBLEM)
									&& ((pSubType == ModelChecker.MORE_THAN_ONE_SINK_WARNING)
											|| (pSubType == ModelChecker.NO_BACKWARD_LINK_WARNING)
											|| (pSubType == ModelChecker.FORK_WITHOUT_JOIN_WARNING)
											|| (pSubType == ModelChecker.CS_BETWEEN_FORK_JOIN)
											|| (pSubType == ModelChecker.CS_WITH_FORK_JOIN)
											|| (pSubType == ModelChecker.TRANSITION_BETWEEN_FORK_JOIN)
											|| (pSubType == ModelChecker.TRANSITION_WITHOUT_FORWARD_LINKS)))) {
						setVisible(false);
					} else {
						if (!mc.isEverythingOkNormal()) {
							setVisible(true);
						}
					}
				}
			}
		});

		JPanel containerPanel = new JPanel();
		containerPanel.setLayout(new BorderLayout());
		JPanel blankPanel = new JPanel();
		blankPanel.setBackground(Color.WHITE);
		containerPanel.add(problemsList, BorderLayout.NORTH);
		containerPanel.add(blankPanel, BorderLayout.CENTER);

		JScrollPane jsp = new JScrollPane(containerPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		jsp.setPreferredSize(new Dimension(310, 230));
		gbconstants.insets.top = 10;
		gbconstants.anchor = GridBagConstraints.LINE_START;
		gbconstants.insets.left = 15;
		this.addComponent(title, gblayout, gbconstants, 0, 0, 2, 1);
		gbconstants.anchor = GridBagConstraints.CENTER;
		gbconstants.insets.top = 20;
		gbconstants.insets.left = -38;
		typeButton = new JButton("Type");
		typeButton.setPreferredSize(new Dimension(100, 20));
		typeButton.setFocusable(false);
		this.addComponent(typeButton, gblayout, gbconstants, 1, 0, 1, 1);
		descriptionButton = new JButton("Description");
		descriptionButton.setPreferredSize(new Dimension(882, 20));
		descriptionButton.setFocusable(false);
		gbconstants.insets.left = -68;
		this.addComponent(descriptionButton, gblayout, gbconstants, 1, 1, 1, 1);
		gbconstants.fill = GridBagConstraints.BOTH;
		gbconstants.insets.top = 0;
		gbconstants.weightx = 1;
		gbconstants.weighty = 1;
		gbconstants.insets.right = 10;
		gbconstants.insets.left = 10;
		jsp.setFocusable(false);
		this.addComponent(jsp, gblayout, gbconstants, 2, 0, 2, 1);
		ButtonEventHandler beh = new ButtonEventHandler();
		continueButton = new JButton("Continue");
		continueButton.setPreferredSize(new Dimension(80, 25));
		continueButton.addActionListener(beh);
		cancelButton = new JButton("Cancel");
		cancelButton.setPreferredSize(new Dimension(80, 25));
		cancelButton.setSelected(true);
		cancelButton.addActionListener(beh);
		if (isToJMVAConversion) {
			if (!mc.isErrorFreeToJMVA()) {
				continueButton.setEnabled(false);
			}
		} else {
			if (!mc.isErrorFreeNormal()) {
				continueButton.setEnabled(false);
			}
		}
		gbconstants.fill = GridBagConstraints.NONE;
		gbconstants.insets.left = 50;
		this.addComponent(continueButton, gblayout, gbconstants, 3, 0, 1, 1);
		gbconstants.insets.right = -45;
		this.addComponent(cancelButton, gblayout, gbconstants, 3, 1, 1, 1);
		this.setSize(CommonConstants.MAX_GUI_WIDTH_JSIM_PROBLEMS, CommonConstants.MAX_GUI_HEIGHT_JSIM_PROBLEMS);
		this.setLocation(300, 190);
		this.setModal(true);
		this.setResizable(false);
	}

	/**
	 * create the ProblemElements and insert them into the problems vector
	 */
	private void initializeList() {
		if (isToJMVAConversion) {
			if (mc.isThereNoClassError()) {
				problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.NO_CLASS_ERROR,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">No classes defined.</div></html>", null, null));
			}
			if (mc.isThereNoStationError()) {
				problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.NO_STATION_ERROR,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">No stations defined.</div></html>", null, null));
			}
			if (mc.isThereOpenClassButNoSourceError()) {
				problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.OPEN_CLASS_BUT_NO_SOURCE_ERROR,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Open class found but no source defined.</div></html>", null, null));
			}
			if (mc.isThereClassesWithoutRefStationError()) {
				Vector<Object> temp = mc.getKeysOfClassesWithoutRefStation();
				for (int i = 0; i < temp.size(); i++) {
					Object classKey = temp.get(i);
					String className = mc.getClassModel().getClassName(classKey);
					problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.REFERENCE_STATION_ERROR,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">No reference station defined for "
									+ className + ".</div></html>", null, classKey));
				}
			}
			if (mc.isThereBCMPDifferentQueueingStrategyWarning()) {
				Vector<Object> temp = mc.getBCMPserversWithDifferentQueueStrategy();
				for (int i = 0; i < temp.size(); i++) {
					String thisStation = mc.getStationModel().getStationName(temp.get(i));
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.BCMP_DIFFERENT_QUEUEING_STRATEGIES_WARNING,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Different per class queueing strategy found at "
									+ thisStation + ".</div></html>", temp.get(i), null));
				}
			}
			if (mc.isThereBCMPDifferentServiceTypeWarning()) {
				Vector<Object> temp = mc.getBCMPserversWithDifferentServiceTypes();
				for (int i = 0; i < temp.size(); i++) {
					String thisStation = mc.getStationModel().getStationName(temp.get(i));
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.BCMP_FCFS_DIFFERENT_SERVICE_TYPES_WARNING,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Non uniform service strategy inside FCFS station "
									+ thisStation + ".</div></html>", temp.get(i), null));
				}
			}
			if (mc.isThereBCMPFcfsNonExponentialWarning()) {
				Vector<Object> temp = mc.getBCMPserversFCFSWithoutExponential();
				for (int i = 0; i < temp.size(); i++) {
					String thisStation = mc.getStationModel().getStationName(temp.get(i));
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.BCMP_FCFS_EXPONENTIAL_WARNING,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Non exponential service time inside FCFS station "
									+ thisStation + ".</div></html>", temp.get(i), null));
				}
			}
			if (mc.isThereBCMPFcfsDifferentServiceTimesWarning()) {
				Vector<Object> temp = mc.getBCMPFcfsServersWithDifferentServiceTimes();
				for (int i = 0; i < temp.size(); i++) {
					String thisStation = mc.getStationModel().getStationName(temp.get(i));
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.BCMP_FCFS_DIFFERENT_SERVICE_TIMES_WARNING,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Different service times inside FCFS station "
									+ thisStation + ".</div></html>", temp.get(i), null));
				}
			}
			if (mc.isThereBCMPDelayWarning()) {
				Vector<Object> temp = mc.getBCMPdelaysWithNonRationalServiceDistribution();
				for (int i = 0; i < temp.size(); i++) {
					String thisStation = mc.getStationModel().getStationName(temp.get(i));
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.BCMP_DELAY_WARNING,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + thisStation
							+ " with non valid service time distribution.</div></html>", temp.get(i), null));
				}
			}
			if (mc.isThereBCMPNonStateIndependentRoutingWarning()) {
				problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.BCMP_NON_STATE_INDEPENDENT_ROUTING_WARNING,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">A non state independent routing strategy was found.</div></html>", null, null));
			}
			if (mc.isThereBCMPNonSymmetricSchedulingPolicies()) {
				Vector<Object> temp = mc.getBCMPserversWithNonSymmetricScheduling();
				for (int i = 0; i < temp.size(); i++) {
					String thisStation = mc.getStationModel().getStationName(temp.get(i));
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.BCMP_NON_SYMMETRIC_SCHEDULING,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Unsupported scheduling policy found at "
									+ thisStation + ".</div></html>", temp.get(i), null));
				}
			}
			//TODO: BCMP_PROCESSOR_SHARING_WARNING
			//TODO: BCMP_LCFS_PREEMPTIVE_WARNING
		} else {
			if (mc.isThereNoClassError()) {
				problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.NO_CLASS_ERROR,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">No classes defined.</div></html>", null, null));
			}
			if (mc.isThereNoStationError()) {
				problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.NO_STATION_ERROR,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">No stations defined.</div></html>", null, null));
			}
			if (mc.isThereNoMeasureError()) {
				problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.NO_MEASURE_ERROR,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">No performance indices defined.</div></html>", null, null));
			}
			if (mc.isThereStationLinkError()) {
				Vector<Object> temp = mc.getKeysOfStationsWithLinkProblems();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					String description = null;
					if (mc.getStationModel().getStationType(stationKey).equals(CommonConstants.STATION_TYPE_SINK)) {
						description = ("<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
								+ " has no incoming links! No routing is possible.</div></html>");
					} else {
						description = ("<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
								+ " has no outgoing links! No routing is possible.</div></html>");
					}
					problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.STATION_LINK_ERROR,
							description, stationKey, null));
				}
			}
			if (mc.isThereAllForwardStationsAreSinkErrors()) {
				HashMap<Object, Vector<Object>> temp = mc.getKeysOfAllForwardStationsAreSinkErrors();
				Vector<Object> classKeys = mc.getClassModel().getClassKeys();
				for (int i = 0; i < classKeys.size(); i++) {
					Object classKey = classKeys.get(i);
					String className = mc.getClassModel().getClassName(classKey);
					Vector<Object> stationWithAllForwardStationsAreSinkErrors = temp.get(classKey);
					if (stationWithAllForwardStationsAreSinkErrors != null) {
						for (int j = 0; j < stationWithAllForwardStationsAreSinkErrors.size(); j++) {
							Object stationKey = stationWithAllForwardStationsAreSinkErrors.get(j);
							String stationName = mc.getStationModel().getStationName(stationKey);
							problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.ALL_FORWARD_STATION_ARE_SINK_ERROR,
									"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Close class " + className + " routed to station "
											+ stationName + " linked only to sink.</div></html>", stationKey, classKey));
						}
					}
				}
			}
			if (mc.isThereRoutingError()) {
				HashMap<Object, Vector<Object>> temp = mc.getKeysOfRoutingProblems();
				Vector<Object> classKeys = mc.getClassModel().getClassKeys();
				for (int i = 0; i < classKeys.size(); i++) {
					Object classKey = classKeys.get(i);
					String className = mc.getClassModel().getClassName(classKey);
					Vector<Object> stationWithRoutingProblems = temp.get(classKey);
					if (stationWithRoutingProblems != null) {
						for (int j = 0; j < stationWithRoutingProblems.size(); j++) {
							Object stationKey = stationWithRoutingProblems.get(j);
							String stationName = mc.getStationModel().getStationName(stationKey);
							problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.ROUTING_ERROR,
									"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Closed class " + className
									+ " at station " + stationName + " is routed to sink with p=1.</div></html>", stationKey, classKey));
						}
					}
				}
			}
			if (mc.isThereClassesWithoutRefStationError()) {
				Vector<Object> temp = mc.getKeysOfClassesWithoutRefStation();
				for (int i = 0; i < temp.size(); i++) {
					Object classKey = temp.get(i);
					String className = mc.getClassModel().getClassName(classKey);
					problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.REFERENCE_STATION_ERROR,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">No reference station defined for "
									+ className + ".</div></html>", null, classKey));
				}
			}
			if (mc.isThereSourceWithoutOpenClassError()) {
				Vector<Object> temp = mc.getKeysOfSourceWithoutOpenClass();
				for (int i = 0; i < temp.size(); i++) {
					Object sourceKey = temp.get(i);
					String sourceName = mc.getStationModel().getStationName(sourceKey);
					problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.SOURCE_WITHOUT_OPEN_CLASS_ERROR,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + sourceName
							+ " were found without associated open classes.</div></html>", sourceKey, null));
				}
			}
			if (mc.isThereSinkButNoOpenClassError()) {
				problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.SINK_BUT_NO_OPEN_CLASS_ERROR,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Sinks were found but no open classes have been defined.", null, null));
			}
			if (mc.isThereOpenClassButNoSourceError()) {
				problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.OPEN_CLASS_BUT_NO_SOURCE_ERROR,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Open classes were found but no sources have been defined.</div></html>", null, null));
			}
			if (mc.isThereOpenClassButNoSinkError()) {
				problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.OPEN_CLASS_BUT_NO_SINK_ERROR,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Open classes were found but no sinks have been defined.</div></html>", null, null));
			}
			if (mc.isThereSinkPerfIndicesWithNoSinkError()) {
				problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.SINK_PERF_IND_WITH_NO_SINK_ERROR,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Response Time per Sink and Throughput per Sink should not be used "
								+ "if there are no sinks defined in the model.</div></html>", null, null));
			}
			if (mc.isThereInconsistentMeasureError()) {
				problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.INCONSISTENT_MEASURE_ERROR,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Undefined station in performance index.</div></html>", null, null));
			}
			if (mc.isThereMeasureError()) {
				problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.DUPLICATE_MEASURE_ERROR,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">A performance index is defined more than once.</div></html>", null, null));
			}
			if (mc.isTherejoinWithoutForkError()) {
				problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.JOIN_WITHOUT_FORK_ERROR,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Join found but no fork.</div></html>", null, null));
			}
			if (mc.isThereEmptyBlockingRegionError()) {
				Vector<Object> regionKeys = mc.getKeysOfEmptyBlockingRegions();
				for (int i = 0; i < regionKeys.size(); i++) {
					String name = mc.getBlockingModel().getRegionName(regionKeys.get(i));
					problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.EMPTY_BLOCKING_REGION,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Finite Capacity Region " + name
							+ " is empty.</div></html>", regionKeys.get(i), null));
				}
			}
			if (mc.isTherePreloadingInBlockingRegionError()) {
				problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.PRELOADING_IN_BLOCKING_REGION,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Preloading of stations in finite capacity regions "
								+"exceeds global or class constraints (weights have not been considered).</div></html>", null, null));
			}
			if (mc.isThereMoreThanOneSinkWarning()) {
				problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM,ModelChecker.MORE_THAN_ONE_SINK_WARNING,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">More than one sink defined, "
								+ "measures may not be accurate.</div></html>", null, null));
			}
			if (mc.isThereNoBackwardLinkWarning()) {
				Vector<Object> temp = mc.getKeysOfStationWithoutBackwardLinks();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.NO_BACKWARD_LINK_WARNING,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
							+ " is not backward linked.</div></html>", stationKey, null));
				}
			}
			if (mc.isThereForkWithoutJoinWarnings()) {
				problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.FORK_WITHOUT_JOIN_WARNING,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Fork found but no join.</div></html>", null, null));
			}
			if (mc.isThereParametricAnalysisModelModifiedWarning()) {
				problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.PARAMETRIC_ANALYSIS_MODEL_MODIFIED_WARNING,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">What-if analysis model modified.</div></html>", null, null));
			}
			if (mc.isThereParametricAnalysisNoMoreAvailableWarning()) {
				problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.PARAMETRIC_ANALYSIS_NO_MORE_AVAILABLE_WARNING,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">What-if analysis not available.</div></html>", null, null));
			}
			if (mc.isSinkPerfIndicesWithClosedClassError()) {
				problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.SINK_PERF_WITH_CLOSED_CLASS_ERROR,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Response Time per Sink and Throughput per Sink "
								+ "should not be used for closed class.</div></html>", null, null));
			}
			if (mc.isThereSinkProbabilityUpdateWarning()) {
				problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.SINK_PROBABILITY_UPDATE_WARNING,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Sink Probability of the Closed Class(s) "
								+ mc.getClassModel().getSinkProbabilityUpdateClasses() + " of Station(s) "
								+ mc.getStationModel().getSinkProbabilityUpdateStations() + " has been updated to 0.0.</div></html>", null, null));
				//Reset after displaying the offending Station and Class names.
				mc.getClassModel().resetSinkProbabilityUpdateClasses();
				mc.getStationModel().resetSinkProbabilityUpdateStations();
			}
			if (mc.isThereCsMatrixRowSumError()) {
				Vector<Object> temp = mc.getCsWithWrongMatrix();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.CS_MATRIX_HAS_ROWS_LESS_THAN_ONE,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">In " + stationName
							+ " switch, rows must sum to a value equal or greater than one.</div></html>", stationKey, null));
				}				
			}
			if (mc.isThereCsFollowedByBasError()) {
				Vector<Object> temp = mc.getCsFolloweByBas();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.CS_FOLLOWED_BY_A_BAS,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
							+ " is followed by a queue with BAS strategy scheduling.</div></html>", stationKey, null));
				}				
			}
			if (mc.isThereLoadDependentRoutingError()) {
				Map<Object, HashMap<Object, Object>> temp = mc.getInvalidLoadDependentRoutingStations();
				for (Iterator<Object> it = temp.keySet().iterator(); it.hasNext();) {
					Object key = it.next();
					HashMap<Object, Object> val = (HashMap<Object, Object>)temp.get(key);
					Map.Entry<Object, Object> m = val.entrySet().iterator().next();
					Object stationKey = m.getKey();
					Object classKey = m.getValue();
					String className = mc.getClassModel().getClassName(classKey);
					String sourceName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.LOAD_DEPENDENT_ROUTING_INVALID,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Station: " + sourceName + " for class: "
									+ className	+ " with invalid LoadDependentRouting.</div></html>", stationKey, classKey));
				}
			}
			if (mc.isThereCsBetweenForkJoin()) {
				Vector<Object> temp = mc.getCsBetweenForkJoin();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.CS_BETWEEN_FORK_JOIN,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
							+ " seems to be located between fork/join.</div></html>", stationKey, null));
				}
			}
			if (mc.isThereCsReferenceStation()) {
				problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.CS_REFERENCE_STATION,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Since a class switch is in the model, "
								+ "all the closed classes must have the same reference station.</div></html>", null, null));
			}
			if (mc.isThereCsSystemThroughputClass()) {
				problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.CS_SYSTEM_THROUGHPUT_CLASS,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Since a class switch is in the model, "
								+ "the system throughput per class cannot be computed.</div></html>", null, null));
			}
			if (mc.isThereClosedClassZeroPopulation()) {
				for (Object classKey:mc.getClosedZeroPopulation()) {
					String className = mc.getClassModel().getClassName(classKey);
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.CLOSED_ZERO_POPULATION,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Population set to zero: "
									+ className + "</div></html>", null, classKey));
				}
			}
			if (mc.isThereCsAllClosedZeroPopulation()) {
				problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.CS_ALL_CLOSED_ZERO_POPULATION,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Since a class switch is in the model, "
						+ "at least one closed class must have non-zero population.</div></html>", null, null));
			}
			if (mc.isThereCsWithForkJoin()) {
				problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.CS_WITH_FORK_JOIN,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Class switches and Fork/Join sections are in the same model.</div></html>", null, null));
			}
			if (mc.isThereGuardStrategyZeroTasks()) {
				HashMap<Object, Vector<Object>> temp = mc.getKeysOfAllGuardStrategyZeroTask();
				Vector<Object> stationKeys = mc.getStationModel().getStationKeys();
				for (int i = 0; i < stationKeys.size(); i++) {
					Object stationKey = stationKeys.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					Vector<Object> classes = temp.get(stationKey);
					if (classes != null) {
						for (int j = 0; j < classes.size(); j++) {
							Object classKey = classes.get(j);
							String className = mc.getClassModel().getClassName(classKey);
							problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.GUARD_STRATEGY_ZERO_TASKS,
									"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Station " + stationName
									+ " has the Guard strategy for class " + className + " with zero tasks.</div></html>", stationKey, classKey));
						}
					}
				}
			}
			if (mc.isThereSchedulingSamePriorities()) {
				problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.SCHEDULING_SAME_PRIORITIES,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Priority scheduling is used but all the classes have the same priorities. "
								+ "The simulation may run slower than usual.</div></html>", null, null));
			}
			if (mc.isThereSemaphoreNotBetweenForkJoin()) {
				Vector<Object> temp = mc.getSemaphoreNotBetweenForkJoin();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.SEMAPHORE_NOT_BETWEEN_FORK_JOIN,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
							+ " is not located between fork/join. This topology is not allowed.</div></html>", stationKey, null));
				}
			}
			if (mc.isThereScalerNotBetweenForkJoin()) {
				Vector<Object> temp = mc.getScalerNotBetweenForkJoin();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.SCALER_NOT_BETWEEN_FORK_JOIN,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
							+ " is not located between fork/join. This topology is not allowed.</div></html>", stationKey, null));
				}
			}
			if (mc.isThereTransitionBetweenForkJoin()) {
				Vector<Object> temp = mc.getTransitionBetweenForkJoin();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.TRANSITION_BETWEEN_FORK_JOIN,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
							+ " seems to be located between fork/join.</div></html>", stationKey, null));
				}
			}
			if (mc.isThereTransitionWithoutForwardLinks()) {
				Vector<Object> temp = mc.getTransitionWithoutForwardLinks();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.TRANSITION_WITHOUT_FORWARD_LINKS,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
							+ " is not forward linked.</div></html>", stationKey, null));
				}
			}
			if (mc.isThereTransitionWithInvalidConditions()) {
				Vector<Object> temp = mc.getTransitionWithInvalidConditions();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.TRANSITION_WITH_INVALID_CONDITIONS,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
							+ " has invalid enabling or inhibiting conditions for some modes.</div></html>", stationKey, null));
				}
			}
			if (mc.isThereTransitionWithInvalidOutcomes()) {
				Vector<Object> temp = mc.getTransitionWithInvalidOutcomes();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.TRANSITION_WITH_INVALID_OUTCOMES,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
							+ " has invalid firing outcomes for some modes.</div></html>", stationKey, null));
				}
			}
		}
	}

	/**
	 * Enable or not to run simulation/conversion to JMVA
	 * @return true if the simulation is runnable and user wants to run simulation
	 */
	public boolean continued() {
		if (isToJMVAConversion) {
			if ((!operationCanceled) && (mc.isErrorFreeToJMVA())) {
				canBeRun = true;
			}
		} else {
			if ((!operationCanceled) && (mc.isErrorFreeNormal())) {
				canBeRun = true;
			}
		}
		return canBeRun;
	}

	private void addComponent(Component component, GridBagLayout gbl, GridBagConstraints gbc, int row, int column, int width, int heigth) {
		Container c = this.getContentPane();

		gbc.gridx = column;
		gbc.gridy = row;

		gbc.gridwidth = width;
		gbc.gridheight = heigth;

		gbl.setConstraints(component, gbc);
		c.add(component);
	}

	private void getRelatedPanel(int problemType, int problemSubType, Object relatedStation, Object relatedClass) {
		gi.showRelatedPanel(problemType, problemSubType, relatedStation, relatedClass);
	}

	private class ProblemElementRenderer implements ListCellRenderer {

		private String[] iconNames = new String[] { "Error", "Warning" };
		private Icon[] icons = new Icon[iconNames.length];
		private int[] problemTypes = { ModelChecker.ERROR_PROBLEM, ModelChecker.WARNING_PROBLEM };

		public ProblemElementRenderer() {
			for (int i = 0; i < iconNames.length; i++) {
				icons[i] = JMTImageLoader.loadImage(iconNames[i], new Dimension(16, 16));
			}
		}

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			JLabel label = null;
			JPanel pane = new JPanel();
			pane.setLayout(new BoxLayout(pane, BoxLayout.X_AXIS));
			for (int i = 0; i < problemTypes.length; i++) {
				if (problemTypes[i] == ((ProblemElement) value).getProblemType()) {
					String errorDescription = ((ProblemElement) value).getDescription();
					label = new JLabel(errorDescription, icons[i], SwingConstants.LEFT);
				}
			}
			String labelName = iconNames[((ProblemElement) value).getProblemType()];
			if (((ProblemElement) value).getProblemType() == ModelChecker.ERROR_PROBLEM) {
				labelName = "<html><b>" + labelName + "</b></html>";
			} else {
				labelName = "<html><i>" + labelName + "</i></html>";
			}
			label = new JLabel(labelName, icons[((ProblemElement) value).getProblemType()], SwingConstants.CENTER);
			label.setMinimumSize(new Dimension(111, 20));
			label.setMaximumSize(new Dimension(111, 1000));
			label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			label.setOpaque(true);
			label.setBackground(isSelected ? list.getSelectionBackground() : Color.WHITE);
			label.setForeground(isSelected ? list.getSelectionForeground() : Color.BLACK);
			label.setFont(isSelected ? label.getFont().deriveFont(Font.BOLD) : label.getFont().deriveFont(Font.ROMAN_BASELINE));

			JLabel description = null;
			String errorDescription = ((ProblemElement) value).getDescription();
			description = new JLabel(errorDescription, SwingConstants.LEFT);
			description.setMinimumSize(new Dimension(871, 20));
			description.setMaximumSize(new Dimension(871, 1000));
			description.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			description.setOpaque(true);
			description.setBackground(isSelected ? list.getSelectionBackground() : Color.WHITE);
			description.setForeground(isSelected ? list.getSelectionForeground() : Color.BLACK);
			description.setFont(isSelected ? description.getFont().deriveFont(Font.BOLD) : description.getFont().deriveFont(Font.ROMAN_BASELINE));

			pane.add(label);
			pane.add(description);

			pane.setBorder(new MatteBorder(0, 0, 1, 0, Color.GRAY));

			return pane;
		}

	}

	private class ProblemElement {

		int type;
		int subType;
		String description;
		Object relatedStationKey;
		Object relatedClassKey;

		public ProblemElement(int type, int subType, String description, Object relatedStationKey, Object relatedClassKey) {
			this.type = type;
			this.subType = subType;
			this.description = description;
			this.relatedStationKey = relatedStationKey;
			this.relatedClassKey = relatedClassKey;
		}

		public int getProblemType() {
			return type;
		}

		public int getProblemSubType() {
			return subType;
		}

		public String getDescription() {
			return description;
		}

		public Object getRelatedStationKey() {
			return relatedStationKey;
		}

		public Object getRelatedClassKey() {
			return relatedClassKey;
		}

	}

	private class ButtonEventHandler implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == cancelButton) {
				operationCanceled = true;
				dispose();
			} else if (e.getSource() == continueButton) {
				operationCanceled = false;
				dispose();
			}
		}

	}

}
