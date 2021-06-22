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
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
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
 * Modified by Mattia Cazzoli
 * Long messages are now visualized on multiple lines.
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
				problems.clear();
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
									|| (pSubType == ModelChecker.OPEN_CLASS_BUT_NO_SOURCE_ERROR)
									|| (pSubType == ModelChecker.OPEN_CLASS_BUT_NO_SINK_ERROR)
									|| (pSubType == ModelChecker.NO_ESSENTIAL_LINK_ERROR)
									|| (pSubType == ModelChecker.JOIN_WITHOUT_FORK_ERROR)
									|| (pSubType == ModelChecker.SEMAPHORE_NOT_BETWEEN_FORK_JOIN_ERROR)
									|| (pSubType == ModelChecker.SCALER_NOT_BETWEEN_FORK_JOIN_ERROR)))
							|| ((pType == ModelChecker.WARNING_PROBLEM)
									&& ((pSubType == ModelChecker.NO_OPTIONAL_LINK_WARNING)
											|| (pSubType == ModelChecker.FORK_WITHOUT_JOIN_WARNING)
											|| (pSubType == ModelChecker.CLASS_SWITCH_BETWEEN_FORK_JOIN_WARNING)
											|| (pSubType == ModelChecker.TRANSITION_BETWEEN_FORK_JOIN_WARNING)))) {
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
		this.centerWindow();
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
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Open class defined but no sources.</div></html>", null, null));
			}
			if (mc.isThereNoReferenceStationError()) {
				Vector<Object> temp = mc.getClassesWithoutReferenceStation();
				for (int i = 0; i < temp.size(); i++) {
					Object classKey = temp.get(i);
					String className = mc.getClassModel().getClassName(classKey);
					problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.NO_REFERENCE_STATION_ERROR,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">No reference station defined for "
									+ className + ".</div></html>", null, classKey));
				}
			}
			if (mc.isThereBcmpDifferentQueueStrategiesWarning()) {
				Vector<Object> temp = mc.getBcmpServersWithDifferentQueueStrategies();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.BCMP_DIFFERENT_QUEUE_STRATEGIES_WARNING,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Mixed queue strategies found at "
									+ stationName + ".</div></html>", stationKey, null));
				}
			}
			if (mc.isThereBcmpDifferentServiceStrategiesWarning()) {
				Vector<Object> temp = mc.getBcmpFcfsServersWithDifferentServiceStrategies();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.BCMP_FCFS_DIFFERENT_SERVICE_STRATEGIES_WARNING,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Mixed service strategies found at "
									+ stationName + ".</div></html>", stationKey, null));
				}
			}
			if (mc.isThereBcmpFcfsNonExponentialDistributionWarning()) {
				Vector<Object> temp = mc.getBcmpFcfsServersWithNonExponentialDistribution();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.BCMP_FCFS_NON_EXPONENTIAL_DISTRIBUTION_WARNING,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Non exponential distribution found at "
									+ stationName + ".</div></html>", stationKey, null));
				}
			}
			if (mc.isThereBcmpFcfsDifferentServiceTimesWarning()) {
				Vector<Object> temp = mc.getBcmpFcfsServersWithDifferentServiceTimes();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.BCMP_FCFS_DIFFERENT_SERVICE_TIMES_WARNING,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Mixed mean service times found at "
									+ stationName + ".</div></html>", stationKey, null));
				}
			}
			//TODO: BCMP_PS_NON_RATIONAL_DISTRIBUTION_WARNING
			//TODO: BCMP_LCFS_PR_NON_RATIONAL_DISTRIBUTION_WARNING
			if (mc.isThereBcmpDelayNonRationalDistributionWarning()) {
				Vector<Object> temp = mc.getBcmpDelaysWithNonRationalDistribution();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.BCMP_DELAY_NON_RATIONAL_DISTRIBUTION_WARNING,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Non rational distribution found at "
									+ stationName + ".</div></html>", stationKey, null));
				}
			}
			if (mc.isThereBcmpStateDependentRoutingStrategyWarning()) {
				Vector<Object> temp = mc.getBcmpStationsWithStateDependentRoutingStrategy();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.BCMP_STATE_DEPENDENT_ROUTING_STRATEGY_WARNING,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">State dependent routing strategy found at "
									+ stationName + ".</div></html>", stationKey, null));
				}
			}
			if (mc.isThereBcmpAsymmetricSchedulingPolicyWarning()) {
				Vector<Object> temp = mc.getBcmpServersWithAsymmetricSchedulingPolicy();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.BCMP_ASYMMETRIC_SCHEDULING_POLICY_WARNING,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Unsupported scheduling policy found at "
									+ stationName + ".</div></html>", stationKey, null));
				}
			}
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
			if (mc.isThereOpenClassButNoSourceError()) {
				problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.OPEN_CLASS_BUT_NO_SOURCE_ERROR,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Open class defined but no sources or transitions.</div></html>", null, null));
			}
			if (mc.isThereOpenClassButNoSinkError()) {
				problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.OPEN_CLASS_BUT_NO_SINK_ERROR,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Open class defined but no sinks or transitions.</div></html>", null, null));
			}
			if (mc.isThereSourceWithoutOpenClassError()) {
				Vector<Object> temp = mc.getSourcesWithoutOpenClass();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.SOURCE_WITHOUT_OPEN_CLASS_ERROR,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
							+ " is found without an associated open class.</div></html>", stationKey, null));
				}
			}
			if (mc.isThereSinkButNoOpenClassError()) {
				problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.SINK_BUT_NO_OPEN_CLASS_ERROR,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Sink defined but no open classes.", null, null));
			}
			if (mc.isThereNoReferenceStationError()) {
				Vector<Object> temp = mc.getClassesWithoutReferenceStation();
				for (int i = 0; i < temp.size(); i++) {
					Object classKey = temp.get(i);
					String className = mc.getClassModel().getClassName(classKey);
					problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.NO_REFERENCE_STATION_ERROR,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">No reference station defined for "
									+ className + ".</div></html>", null, classKey));
				}
			}
			if (mc.isThereNoEssentialLinkError()) {
				Vector<Object> temp = mc.getStationsWithoutEssentialLink();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String staitonType = mc.getStationModel().getStationType(stationKey);
					String stationName = mc.getStationModel().getStationName(stationKey);
					String description = null;
					if (staitonType.equals(CommonConstants.STATION_TYPE_SINK)) {
						description = "<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
								+ " is not backward linked.</div></html>";
					} else if (staitonType.equals(CommonConstants.STATION_TYPE_TRANSITION)) {
							description = "<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
									+ " is not forward or backward linked.</div></html>";
					} else {
						description = "<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
								+ " is not forward linked.</div></html>";
					}
					problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.NO_ESSENTIAL_LINK_ERROR,
							description, stationKey, null));
				}
			}
			if (mc.isThereInvalidMeasureError()) {
				problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.INVALID_MEASURE_ERROR,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Undefined station for performance index.</div></html>", null, null));
			}
			if (mc.isThereRedundantMeasureError()) {
				problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.DUPLICATE_MEASURE_ERROR,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Performance index defined more than once.</div></html>", null, null));
			}
			if (mc.isThereInconsistentQueueStrategyError()) {
				Vector<Object> temp = mc.getStationsWithInconsistentQueueStrategies();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.INCONSISTENT_QUEUE_STRATEGY_ERROR,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
							+ " uses different queue strategies for classes within the same priority group.</div></html>", stationKey, null));
				}
			}
			if (mc.isThereUnpredictableServiceError()) {
				Vector<Object> temp = mc.getServersWithUnpredictableService();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.UNPREDICTABLE_SERVICE_ERROR,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
							+ " uses SJF, LJF, SEPT or LEPT along with the load dependent service strategy.</div></html>", stationKey, null));
				}
			}
			if (mc.isThereJoinWithoutForkError()) {
				problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.JOIN_WITHOUT_FORK_ERROR,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Join found but no forks.</div></html>", null, null));
			}
			//TODO: FORK_JOIN_ROUTING_ERROR
			if (mc.isThereBlockingRegionCapacityOverloadError()) {
				Vector<Object> temp = mc.getBlockingRegionsWithCapacityOverload();
				for (int i = 0; i < temp.size(); i++) {
					Object regionKey = temp.get(i);
					String regionName = mc.getBlockingModel().getRegionName(regionKey);
					problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.BLOCKING_REGION_CAPACITY_OVERLOAD_ERROR,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">The total preload in " + regionName
							+ " exceeds the global, class or group capacity constraints (weights have been considered).</div></html>", null, null));
				}
			}
			if (mc.isThereBlockingRegionMemoryOverloadError()) {
				Vector<Object> temp = mc.getBlockingRegionsWithMemoryOverload();
				for (int i = 0; i < temp.size(); i++) {
					Object regionKey = temp.get(i);
					String regionName = mc.getBlockingModel().getRegionName(regionKey);
					problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.BLOCKING_REGION_MEMORY_OVERLOAD_ERROR,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">The total preload in " + regionName
							+ " exceeds the global, class or group memory constraints (sizes have been considered).</div></html>", null, null));
				}
			}
			if (mc.isThereClassSwitchInvalidMatrixError()) {
				Vector<Object> temp = mc.getClassSwitchesWithInvalidMatrix();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.CLASS_SWITCH_INVALID_MATRIX_ERROR,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
							+ " has a matrix in which some rows sum to less than one.</div></html>", stationKey, null));
				}
			}
			if (mc.isThereClassSwitchReferenceStationError()) {
				problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.CLASS_SWITCH_REFERENCE_STATION_ERROR,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Since a class switch is in the model, "
								+ "all the closed classes must have the same reference station.</div></html>", null, null));
			}
			if (mc.isThereZeroGuardStrategyError()) {
				HashMap<Object, Vector<Object>> temp = mc.getStationsWithZeroGuardStrategy();
				Vector<Object> stationKeys = mc.getStationModel().getStationKeys();
				for (int i = 0; i < stationKeys.size(); i++) {
					Object stationKey = stationKeys.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					Vector<Object> classKeys = temp.get(stationKey);
					if (classKeys != null) {
						for (int j = 0; j < classKeys.size(); j++) {
							Object classKey = classKeys.get(j);
							String className = mc.getClassModel().getClassName(classKey);
							problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.ZERO_GUARD_STRATEGY_ERROR,
									"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
									+ " uses a Guard strategy with zero tasks for " + className + ".</div></html>", stationKey, classKey));
						}
					}
				}
			}
			if (mc.isThereSemaphoreNotBetweenForkJoinError()) {
				Vector<Object> temp = mc.getSemaphoresNotBetweenForkJoin();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.SEMAPHORE_NOT_BETWEEN_FORK_JOIN_ERROR,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
							+ " is not located between fork/join.</div></html>", stationKey, null));
				}
			}
			if (mc.isThereScalerNotBetweenForkJoinError()) {
				Vector<Object> temp = mc.getScalersNotBetweenForkJoin();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.SCALER_NOT_BETWEEN_FORK_JOIN_ERROR,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
							+ " is not located between fork/join.</div></html>", stationKey, null));
				}
			}
			if (mc.isThereTransitionInfiniteEnablingDegreeError()) {
				Vector<Object> temp = mc.getTransitionsWithInfiniteEnablingDegree();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM, ModelChecker.TRANSITION_INFINITE_ENABLING_DEGREE_ERROR,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
							+ " has an infinite enabling degree for a mode.</div></html>", stationKey, null));
				}
			}
			//TODO rose: Check whether still works
			if (mc.isThereImpatienceInStationBetweenForkJoinError()) {
				Vector<Object> stationsWithError = mc.getStationsWithImpatienceAndConnectedToFork();
				for (int i = 0; i < stationsWithError.size(); i++) {
					Object stationKey = stationsWithError.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.ERROR_PROBLEM,
									ModelChecker.IMPATIENCE_ENABLED_IN_STATION_BETWEEN_FORK_JOIN_ERROR,
									"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH
													+ "px;\">Impatience cannot be selected for stations connected to a fork. "
													+ stationName + " must have its impatience selected as None.</div></html>",
									stationKey, null));
				}
			}
			if (mc.isThereNoOptionalLinkWarning()) {
				Vector<Object> temp = mc.getStationsWithoutOptionalLink();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.NO_OPTIONAL_LINK_WARNING,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
							+ " is not backward linked.</div></html>", stationKey, null));
				}
			}
			if (mc.isThereForkWithoutJoinWarning()) {
				problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.FORK_WITHOUT_JOIN_WARNING,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">Fork found but no joins.</div></html>", null, null));
			}
			if (mc.isThereParametricAnalysisModelModifiedWarning()) {
				problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.PARAMETRIC_ANALYSIS_MODEL_MODIFIED_WARNING,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">What-if analysis model modified.</div></html>", null, null));
			}
			if (mc.isThereParametricAnalysisNotAvailableWarning()) {
				problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.PARAMETRIC_ANALYSIS_NOT_AVAILABLE_WARNING,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">What-if analysis not available.</div></html>", null, null));
			}
			if (mc.isThereZeroPopulationWarning()) {
				for (Object classKey:mc.getClosedClassesWithZeroPopulation()) {
					String className = mc.getClassModel().getClassName(classKey);
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.ZERO_POPULATION_WARNING,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">The population of " + className
							+ " is zero.</div></html>", null, classKey));
				}
			}
			if (mc.isThereZeroTotalPopulationWarning()) {
				problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.ZERO_TOTAL_POPULATION_WARNING,
						"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">The total population of closed classes is zero.</div></html>", null, null));
			}
			if (mc.isThereClassSwitchBetweenForkJoinWarning()) {
				Vector<Object> temp = mc.getClassSwitchesBetweenForkJoin();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.CLASS_SWITCH_BETWEEN_FORK_JOIN_WARNING,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
							+ " seems to be located between fork/join.</div></html>", stationKey, null));
				}
			}
			if (mc.isThereSchedulingSamePriorityWarning()) {
				Vector<Object> temp = mc.getStationsWithPriorityScheduling();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.SCHEDULING_SAME_PRIORITY_WARNING,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
							+ " uses priority scheduling but all the classes have the same priority.</div></html>", stationKey, null));
				}
			}
			if (mc.isThereTransitionBetweenForkJoinWarning()) {
				Vector<Object> temp = mc.getTransitionsBetweenForkJoin();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.TRANSITION_BETWEEN_FORK_JOIN_WARNING,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
							+ " seems to be located between fork/join.</div></html>", stationKey, null));
				}
			}
			if (mc.isThereTransitionConstantEnablingDegreeWarning()) {
				Vector<Object> temp = mc.getTransitionsWithConstantEnablingDegree();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.TRANSITION_CONSTANT_ENABLING_DEGREE_WARNING,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
							+ " has a constant enabling degree for a mode.</div></html>", stationKey, null));
				}
			}
			if (mc.isThereTransitionInvalidInputConditionWarning()) {
				Vector<Object> temp = mc.getTransitionsWithInvalidInputCondition();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.TRANSITION_INVALID_INPUT_CONDITION_WARNING,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
							+ " has invalid input condition for a mode.</div></html>", stationKey, null));
				}
			}
			if (mc.isThereTransitionNoFiringOutcomeWarning()) {
				Vector<Object> temp = mc.getTransitionsWithoutFiringOutcome();
				for (int i = 0; i < temp.size(); i++) {
					Object stationKey = temp.get(i);
					String stationName = mc.getStationModel().getStationName(stationKey);
					problems.addElement(new ProblemElement(ModelChecker.WARNING_PROBLEM, ModelChecker.TRANSITION_NO_FIRING_OUTCOME_WARNING,
							"<html><div style=\"width:" + DESCRIPTION_HTML_WIDTH + "px;\">" + stationName
							+ " has no firing outcome for a mode.</div></html>", stationKey, null));
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

	public void centerWindow() {
		Toolkit tk = Toolkit.getDefaultToolkit();
		//gets dimensions of the screen to center window.
		int xOffset = ((int) tk.getScreenSize().getWidth() - getWidth()) / 2;
		int yOffset = ((int) tk.getScreenSize().getHeight() - getHeight()) / 2;

		setBounds(xOffset, yOffset, getWidth(), getHeight());
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
