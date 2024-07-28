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

package jmt.gui.common.panels;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.ServerType;
import jmt.gui.common.definitions.SimulationDefinition;
import jmt.gui.common.definitions.StationDefinition;

public class MetricConfigurationsPanel extends WizardPanel implements CommonConstants {

    private static final long serialVersionUID = 1L;

    //Interfaces for model data exchange
    protected SimulationDefinition simData;

    protected StationDefinition stationData;

    protected Object parentStationKey;

    protected Object measureKey;

    private JComboBox serverTypesCombos;

    private String [] serverTypesNames;


    public MetricConfigurationsPanel(SimulationDefinition simulation, StationDefinition stations, Object measureKey) {
        setData(simulation, stations, measureKey);
        initComponents();
    }


    private void initComponents() {
        JPanel serverTypesPanel = new JPanel();
        serverTypesPanel.setBorder(new TitledBorder(new EtchedBorder(), "Server Types for Metric"));
        serverTypesPanel.add(Box.createRigidArea(new Dimension((int)(5 * CommonConstants.widthScaling), (int)(0 * CommonConstants.heightScaling))));
        serverTypesPanel.add(new JLabel("Metric defined for:"));
        serverTypesPanel.add(serverTypesCombos);

        this.setLayout(new BorderLayout());
        this.add(serverTypesPanel, BorderLayout.CENTER);
    }


    /**
     * Updates data contained in this panel's components
     */
    public void setData(SimulationDefinition simulation, StationDefinition stations, Object mKey) {
        simData = simulation;
        stationData = stations;
        measureKey = mKey;
        parentStationKey = simData.getMeasureStation(measureKey);
        serverTypesCombos = new JComboBox();
        List<String> serverTypesNamesList = new ArrayList<>();
        serverTypesNamesList.add("--- All Server Types ---");
        serverTypesNamesList.addAll(stationData.getServerTypeNames(parentStationKey));
        serverTypesNames = serverTypesNamesList.toArray(new String[0]);
        serverTypesCombos.setModel(new DefaultComboBoxModel(serverTypesNames));

        addDataManagers();
        updateServerPreferences();
        refreshComponents();
    }


    private void updateServerPreferences() {
        Object key = simData.getMeasureStation(measureKey);
        String measureStationName = stationData.getStationName(key);
        Object measureServerTypeKey = simData.getMeasureServerTypeKey(measureKey);
        if(measureServerTypeKey != null){
            measureStationName = stationData.getServerType(measureServerTypeKey).getName();
        }
        int index = 0;
        for (int i = 0; i < serverTypesNames.length; i++) {
            if (serverTypesNames[i].equals(measureStationName)) {
                index = i;
                break;
            }
        }
        serverTypesCombos.setSelectedIndex(index);
    }

    private void addDataManagers() {
        serverTypesCombos.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String serverTypeName = (String) serverTypesCombos.getSelectedItem();
                Object serverTypeKey = stationData.getServerTypeKey(parentStationKey,serverTypeName);
                simData.setMeasureServerTypeKey(serverTypeKey, measureKey);
            }
        });

    }
    /**
     * called by the Wizard when the panel becomes active
     */
    @Override
    public void gotFocus() {
        refreshComponents();
    }

    @Override
    public void repaint() {
        refreshComponents();
        super.repaint();
    }

    private void refreshComponents() {
    }


    @Override
    public String getName() {
        return "Advanced Metric Configurations";
    }
}
