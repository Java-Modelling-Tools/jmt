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
package jmt.gui.common.panels.parametric;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.SwingUtilities;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.basic.BasicMenuItemUI;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.GuiInterface;
import jmt.gui.common.definitions.SimulationDefinition;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.common.definitions.parametric.ArrivalRateParametricAnalysis;
import jmt.gui.common.definitions.parametric.NumberOfCustomerParametricAnalysis;
import jmt.gui.common.definitions.parametric.ParametricAnalysisChecker;
import jmt.gui.common.definitions.parametric.ParametricAnalysisDefinition;
import jmt.gui.common.definitions.parametric.ParametricAnalysisModelFactory;
import jmt.gui.common.definitions.parametric.PopulationMixParametricAnalysis;
import jmt.gui.common.definitions.parametric.SeedParametricAnalysis;
import jmt.gui.common.definitions.parametric.ServiceTimesParametricAnalysis;
import jmt.gui.common.definitions.parametric.NumberOfServersParametricAnalysis;
import jmt.gui.common.definitions.parametric.TotalStationCapacityParametricAnalysis;
import jmt.gui.common.definitions.parametric.RoutingProbabilitiesParametricAnalysis;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

/**
 * <p>Title: ParametricAnalysisPanel</p>
 * <p>Description: with this panel user can select the type of parametric analysis
 * . This panel contains a <code>ParameterOptionPanel</code> that changes each time
 * user selects a different parametric analysis type.</p>
 *
 * @author Francesco D'Aquino
 *         Date: 7-mar-2006
 *         Time: 13.12.42
 *
 *  Modified by: Xinyu Gao
 */
public class ParametricAnalysisPanel extends WizardPanel implements CommonConstants {

	private static final long serialVersionUID = 1L;
	Color DEFAULT_TITLE_COLOR = new TitledBorder("").getTitleColor();
	private String[] parameters = { "                   " };
	private ParameterOptionPanel parameterOptionPanel;
	private JCheckBox enabler;
	private JPanel upperPanel;
	private JPanel chooserPanel;
	private JPanel enablerPanel;
	private JComboBox<String> chooser;
	private TitledBorder tb;
	private JPopupMenu popupMenu;
	private JButton btnChoose;
	private JButton arrowButton;
	private JPanel combinedButtonPanel;
	private String selectedItem = "";

	ClassDefinition cd;
	StationDefinition sd;
	SimulationDefinition simd;
	GuiInterface gui;

	public ParametricAnalysisPanel(ClassDefinition cd, StationDefinition sd, SimulationDefinition simd, GuiInterface gui) {
		this.cd = cd;
		this.sd = sd;
		this.simd = simd;
		this.gui = gui;
		initGui();
		setListeners();
	}

	public void initGui() {
		combinedButtonPanel = new JPanel(new GridBagLayout());

		GridBagConstraints gbc2 = new GridBagConstraints();
		gbc2.weightx = 0.0;
		gbc2.weighty = 0;
		gbc2.gridx = 0;
		gbc2.gridy = 0;
		gbc2.anchor = GridBagConstraints.WEST;


		popupMenu = new JPopupMenu() {};
		btnChoose = new JButton("Choose analysis");
		btnChoose.setPreferredSize(new Dimension((int)(173 * CommonConstants.widthScaling), (int)(22 * CommonConstants.heightScaling)));
		arrowButton = new JButton("▼");

		enabler = new JCheckBox("Enable What-If analysis");
		enabler.setToolTipText("Enable or disable What-If analysis");
		enablerPanel = new JPanel(new BorderLayout());
		enablerPanel.add(enabler, BorderLayout.WEST);
		upperPanel = new JPanel(new BorderLayout());
		chooserPanel = new JPanel();
		tb = new TitledBorder("Parameter selection for the control of repeated executions");
		chooserPanel.setBorder(tb);

		JLabel description = new JLabel(PARAMETRIC_ANALYSIS_DESCRIPTION);
		JLabel Warning = new JLabel (HTML_START + HTML_FONT_TITLE + "WARNING: "+ HTML_FONT_TIT_END + HTML_FONT_NORM
				+ "Enabling What-If analysis will disable all statistical outputs." + HTML_FONT_NOR_END + HTML_END);
		JPanel northPanel = new JPanel(new BorderLayout());
		northPanel.setBorder(new EmptyBorder(0, 0, 15, 0));
		northPanel.add(description, "Center");
		northPanel.add(Warning, "South");
		northPanel.add(enablerPanel, "East");
		upperPanel.add(northPanel, BorderLayout.NORTH);
		upperPanel.add(chooserPanel, BorderLayout.SOUTH);
		this.setLayout(new BorderLayout());
		this.setBorder(new EmptyBorder(20, 20, 20, 20));
		ParametricAnalysisChecker pac = new ParametricAnalysisChecker(cd, sd, simd);
		if (!pac.canBeEnabled()) {
			enabler.setEnabled(false);
			parameterOptionPanel = createPanel(null);
			simd.setParametricAnalysisModel(null);
			simd.setParametricAnalysisEnabled(false);
		} else {
			enabler.setEnabled(true);
			enabler.setSelected(simd.isParametricAnalysisEnabled());
			ParametricAnalysisDefinition pad = simd.getParametricAnalysisModel();
			parameters = pac.getRunnableParametricAnalysis();

			for (String parameter : parameters) {
				final String currentParameter = parameter;
				JMenuItem menuItem = new JMenuItem(currentParameter);

				btnChoose.setHorizontalAlignment(SwingConstants.LEFT);
				btnChoose.setMargin(new Insets(0, 24, 0, 0));
				if ("Routing probability".equals(currentParameter)) {
					menuItem.setEnabled(true);
				}
				menuItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						selectedItem = ((JMenuItem) e.getSource()).getText();
						btnChoose.setText(selectedItem);
						String param = ((JMenuItem) e.getSource()).getText();

						if (parameterOptionPanel != null) {
							remove(parameterOptionPanel);
							ParametricAnalysisDefinition temp = ParametricAnalysisModelFactory.createParametricAnalysisModel(param, cd, sd, simd);
							simd.setParametricAnalysisModel(temp);
							simd.setSaveChanged();
							parameterOptionPanel = createPanel(temp);
							add(parameterOptionPanel, BorderLayout.CENTER);
							doLayout();
							parameterOptionPanel.validate();
						}
					}
				});
				popupMenu.add(menuItem);
			}

			int lastItemIndex = popupMenu.getComponentCount() - 1;
			if (!Arrays.asList(parameters).contains("Routing probability")) {
				JMenuItem menuItemRoutingProbability = new JMenuItem("Routing probability");
				menuItemRoutingProbability.setEnabled(false);
				popupMenu.insert(menuItemRoutingProbability, lastItemIndex);
			}

			btnChoose.setToolTipText("Choose the what-if analysis to be performed");
			btnChoose.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					popupMenu.show(combinedButtonPanel, 0, btnChoose.getHeight());
				}
			});

			arrowButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					popupMenu.show(btnChoose, 0, btnChoose.getHeight());
				}
			});

			btnChoose.addPropertyChangeListener("enabled", new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					boolean isEnabled = (Boolean) evt.getNewValue();
					arrowButton.setEnabled(isEnabled);
				}
			});

			combinedButtonPanel.add(btnChoose, gbc2);
			combinedButtonPanel.add(arrowButton);

			chooserPanel.add(combinedButtonPanel, BorderLayout.NORTH);

			String temp = parameters[0];
			if (pad == null) {
				pad = ParametricAnalysisModelFactory.createParametricAnalysisModel(temp, cd, sd, simd);
				simd.setParametricAnalysisModel(pad);
			} else {
				int code = pad.checkCorrectness(true);
				if (code != 2) {
					String initialType = pad.getType();
					btnChoose.setText(initialType);
				} else {
					pad = ParametricAnalysisModelFactory.createParametricAnalysisModel(temp, cd, sd, simd);
					simd.setParametricAnalysisModel(pad);
				}
			}
			parameterOptionPanel = createPanel(pad);
		}
		parameterOptionPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
		this.add(upperPanel, BorderLayout.NORTH);
		this.add(parameterOptionPanel, BorderLayout.CENTER);
		this.setEnabled(enabler.isSelected());
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (!enabled) {
			btnChoose.setEnabled(false);
			enablerPanel.setEnabled(false);
			chooserPanel.setEnabled(false);
			upperPanel.setEnabled(false);
			parameterOptionPanel.setEnabled(false);
			tb.setTitleColor(Color.LIGHT_GRAY);
			parameterOptionPanel.repaint();
		} else {
			btnChoose.setEnabled(true);
			enablerPanel.setEnabled(true);
			chooserPanel.setEnabled(true);
			upperPanel.setEnabled(true);
			parameterOptionPanel.setEnabled(true);
			tb.setTitleColor(DEFAULT_TITLE_COLOR);
			parameterOptionPanel.repaint();
		}
	}

	/**
	 * Sets the listeners to enabler and chooser
	 */
	private void setListeners() {
		enabler.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					setEnabled(true);
					simd.setParametricAnalysisEnabled(true);
				} else {
					setEnabled(false);
					simd.setParametricAnalysisEnabled(false);
				}
			}
		});
	}

	/**
	 * Creates the chosen parameter option panel
	 * @param pad the instance of <code>ParameterAnalysisDefinition</code> model.
	 * @return  the <code>ParameterOptionPanel</code> corresponding to the <code>ParameterAnalysisDefinition</code>
	 * passed as parameter
	 */
	protected ParameterOptionPanel createPanel(ParametricAnalysisDefinition pad) {
		ParameterOptionPanel pop;
		if (pad == null) {
			pop = new EmptyPanel();
		} else {
			if (pad instanceof NumberOfCustomerParametricAnalysis) {
				pop = new NumberOfCustomersPanel((NumberOfCustomerParametricAnalysis) pad, cd, sd, simd, gui);
			} else if (pad instanceof PopulationMixParametricAnalysis) {
				pop = new PopulationMixPanel((PopulationMixParametricAnalysis) pad, cd, sd, simd);
			} else if (pad instanceof ServiceTimesParametricAnalysis) {
				pop = new ServiceTimesPanel((ServiceTimesParametricAnalysis) pad, cd, sd, simd);
			} else if (pad instanceof ArrivalRateParametricAnalysis) {
				pop = new ArrivalRatesPanel((ArrivalRateParametricAnalysis) pad, cd, sd, simd);
			} else if (pad instanceof NumberOfServersParametricAnalysis) {
				pop = new NumberOfServersPanel((NumberOfServersParametricAnalysis) pad, cd, sd, simd);
			} else if (pad instanceof TotalStationCapacityParametricAnalysis) {
				pop = new TotalStationCapacityPanel((TotalStationCapacityParametricAnalysis) pad, cd, sd, simd);
			} else if (pad instanceof RoutingProbabilitiesParametricAnalysis) {
				pop = new RoutingProbabilitiesPanel((RoutingProbabilitiesParametricAnalysis) pad, cd, sd, simd);
			} else if (pad instanceof SeedParametricAnalysis) {
				pop = new SeedPanel((SeedParametricAnalysis) pad, cd, sd, simd);
			} else {
				pop = null;
			}
		}
		return pop;
	}

	/**
	 * @return the panel's name
	 */
	@Override
	public String getName() {
		return "What-if Analysis";
	}

	public void setData(ClassDefinition cd, StationDefinition sd, SimulationDefinition simd) {
		this.cd = cd;
		this.sd = sd;
		this.simd = simd;
		this.removeAll();
		this.initGui();
		this.setListeners();
		this.doLayout();
		this.validate();
		this.repaint();
	}

	/**
	 * Called in JSIM when the What-if analysis panel is selected
	 */
	@Override
	public void gotFocus() {
		this.removeAll();
		initGui();
		setListeners();
	}

}
