package jmt.gui.common.panels.parametric;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.SimulationDefinition;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.common.definitions.parametric.ParametricAnalysis;
import jmt.gui.common.definitions.parametric.ParametricAnalysisChecker;
import jmt.gui.common.definitions.parametric.NumberOfServersParametricAnalysis;

/**
 * <p>Title: NumberOfServersPanel</p>
 * <p>Description: this is the panel for he parametric analysis where
 * the number of servers inside a station is modified.</p>
 *
 * @author Xinyu Gao
 *         Date: 20-6-2023
 */

public class NumberOfServersPanel extends ParameterOptionPanel {
    private static final long serialVersionUID = 1L;
    private JLabel fromLabel;
    private JSpinner from;
    private JLabel toLabel;
    private JSpinner to;
    private JLabel stepsLabel;
    private JSpinner steps;
    private JLabel stationChooserLabel;
    private JComboBox stationChooser;
    private JLabel serverTypeChooserLabel;
    private JComboBox serverTypeChooser;
    private JScrollPane scroll;
    private JTextArea description;
    private JScrollPane descrPane;
    private TitledBorder descriptionTitle;
    private ParametricAnalysisChecker checker;
    private String DESCRIPTION_SERVER_TYPES;

    private NumberOfServersParametricAnalysis NSPA;

    public NumberOfServersPanel(NumberOfServersParametricAnalysis nspa, ClassDefinition classDef, StationDefinition stationDef, SimulationDefinition simDef) {
        super();
        NSPA = nspa;
        super.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        super.setDividerSize(3);

        DESCRIPTION = "Repeat the simulation with different number of servers in a station.\n\n"
                + "The 'From' value represents the initial number of servers in a station.\n\n"
                + "The 'To' value represents the final number of servers in a station.\n\n";

        DESCRIPTION_SERVER_TYPES = "Repeat the simulation with different number of servers for a server type in a station.\n\n"
            + "The 'From' value represents the initial number of servers for the server type in the station.\n\n"
            + "The 'To' value represents the final number of servers for the server type in the station.\n\n";

        cd = classDef;
        sd = stationDef;
        simd = simDef;
        checker = new ParametricAnalysisChecker(cd, sd, simd);

        initialize();
    }

    public void initialize() {
        JPanel edit = new JPanel(new GridLayout(5, 1, 0, 5));
        ParametricAnalysisChecker checker = new ParametricAnalysisChecker(cd, sd, simd);
        Vector<Object> availableS = checker.checkForNumberOfServersParametricAnalysisAvailableStations();
        String[] stationNames = new String[availableS.size()];
        for (int i = 0; i < availableS.size(); i++) {
            stationNames[i] = sd.getStationName(availableS.get(i));
        }

        stationChooser = new JComboBox(stationNames);
        stationChooser.setToolTipText("Choose the station whose number of servers will be changed");

        description = new JTextArea();
        description.setOpaque(false);
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        descrPane = new JScrollPane(description);
        descriptionTitle = new TitledBorder(new EtchedBorder(), "Description");

        descrPane.setBorder(descriptionTitle);
        descrPane.setMinimumSize(new Dimension((int)(80 * CommonConstants.widthScaling), (int)(0 * CommonConstants.heightScaling)));

        //if the reference station of the NSPA is still available
        if (availableS.contains(NSPA.getReferenceStation())) {
            stationChooser.setSelectedItem(sd.getStationName(NSPA.getReferenceStation()));
        }
        //else select another one
        else {
            NSPA.setReferenceStation(availableS.get(0));
            stationChooser.setSelectedIndex(0);
        }

        fromLabel = new JLabel("From:");
        from = new JSpinner(new SpinnerNumberModel(NSPA.getInitialValue(), 1, Integer.MAX_VALUE, 1));
        from.setEnabled(true);
        from.setToolTipText("Sets the initial number of servers in the station");
        toLabel = new JLabel("To:");
        to = new JSpinner(new SpinnerNumberModel(NSPA.getFinalValue(), 1, Integer.MAX_VALUE, 1));
        to.setToolTipText("Sets the final number of servers in the station");
        stepsLabel = new JLabel("Steps: ");
        steps = new JSpinner(new SpinnerNumberModel((int)(NSPA.getFinalValue() - NSPA.getInitialValue() + 1), 1, ParametricAnalysis.MAX_STEP_NUMBER, 1));
        NSPA.setNumberOfSteps((int)(NSPA.getFinalValue() - NSPA.getInitialValue() + 1));
        steps.setToolTipText("Sets the number of steps to be performed");

        if (NSPA.isSingleServerType()) {
            description.setText(DESCRIPTION);
        } else {
            description.setText(DESCRIPTION_SERVER_TYPES);
        }

        from.setBackground(Color.WHITE);
        stationChooserLabel = new JLabel("Station:");
        edit.add(fromLabel);
        edit.add(from);
        edit.add(toLabel);
        edit.add(to);
        edit.add(stepsLabel);
        edit.add(steps);
        edit.add(stationChooser);
        edit.setPreferredSize(new Dimension((int)(130 * CommonConstants.widthScaling), (int)(128 * CommonConstants.heightScaling)));
        JPanel editLables = new JPanel(new GridLayout(5, 1, 0, 5));
        editLables.add(fromLabel);
        editLables.add(toLabel);
        editLables.add(stepsLabel);
        editLables.add(stationChooserLabel);
        editLables.setPreferredSize(new Dimension((int)(100 * CommonConstants.widthScaling), (int)(128 * CommonConstants.heightScaling)));

        if (!NSPA.isSingleServerType()) {
            serverTypeChooser = new JComboBox(NSPA.getServerTypeNames());
            if (NSPA.serverTypeInStation()) {
                serverTypeChooser.setSelectedItem(sd.getServerType(NSPA.getServerTypeKey()).getName());
            } else {
                NSPA.setServerType((String) serverTypeChooser.getSelectedItem());
            }
            serverTypeChooserLabel = new JLabel("Server Type:");
            edit.add(serverTypeChooser);
            editLables.add(serverTypeChooserLabel);
        }

        JPanel editPanel = new JPanel();
        editPanel.add(editLables);
        editPanel.add(edit);
        editPanel.setBorder(new EmptyBorder(10, 20, 0, 20));
        JPanel cont = new JPanel(new BorderLayout());
        cont.add(editPanel, BorderLayout.CENTER);
        scroll = new JScrollPane(cont);
        title = new TitledBorder("Station number of servers variation");
        scroll.setBorder(title);
        scroll.setMinimumSize(new Dimension((int)(360 * CommonConstants.widthScaling), (int)(0 * CommonConstants.heightScaling)));
        setLeftComponent(scroll);
        setRightComponent(descrPane);
        this.setBorder(new EmptyBorder(5, 0, 5, 0));
        setListeners();
    }

    @Override
    public void setEnabled(boolean enabled) {
        fromLabel.setEnabled(enabled);
        from.setEnabled(enabled);
        toLabel.setEnabled(enabled);
        to.setEnabled(enabled);
        stepsLabel.setEnabled(enabled);
        steps.setEnabled(enabled);
        description.setEnabled(enabled);
        stationChooserLabel.setEnabled(enabled);
        stationChooser.setEnabled(enabled);
        if (!enabled) {
            scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            descrPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        } else {
            scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            descrPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        }
        if (!enabled) {
            title.setTitleColor(Color.LIGHT_GRAY);
            descriptionTitle.setTitleColor(Color.LIGHT_GRAY);
        } else {
            title.setTitleColor(DEFAULT_TITLE_COLOR);
            descriptionTitle.setTitleColor(DEFAULT_TITLE_COLOR);
        }
    }

    private void setListeners() {
        from.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                Object fValue = from.getValue();
                if (fValue instanceof Integer) {
                    NSPA.setInitialValue(((Integer) fValue).intValue());
                } else if (fValue instanceof Double) {
                    NSPA.setInitialValue(((Double) fValue).intValue());
                }
                from.setValue(new Double(NSPA.getInitialValue()));

                if (NSPA.getFinalValue() > NSPA.getInitialValue()) {
                    if ((Integer) steps.getValue() >= (NSPA.getFinalValue() - NSPA.getInitialValue() + 1)) {
                        NSPA.setNumberOfSteps((int) (NSPA.getFinalValue() - NSPA.getInitialValue() + 1));
                    } else {
                        NSPA.setNumberOfSteps((Integer) steps.getValue());
                    }
                } else if (NSPA.getInitialValue() > NSPA.getFinalValue()) {
                    if ((Integer) steps.getValue() >= (NSPA.getInitialValue() - NSPA.getFinalValue() + 1)) {
                        NSPA.setNumberOfSteps((int) (NSPA.getInitialValue() - NSPA.getFinalValue() + 1));
                    } else {
                        NSPA.setNumberOfSteps((Integer) steps.getValue());
                    }
                    // convert "From" and "To"
                    from.setValue(new Double(NSPA.getInitialValue()));
                    to.setValue(new Double(NSPA.getFinalValue()));
                    Object sub = from.getValue();
                    from.setValue(new Double(NSPA.getFinalValue()));
                    if (to.getValue() instanceof Integer) {
                        to.setValue((Integer) sub);
                    } else if (to.getValue() instanceof Double) {
                        to.setValue((Double) sub);
                    }
                } else {
                    if (NSPA.getNumberOfSteps() >= 2) {
                        NSPA.setNumberOfSteps(1);
                        steps.setValue(new Integer(NSPA.getNumberOfSteps()));
                    }
                }
                steps.setValue(NSPA.getNumberOfSteps());
            }
        });
        to.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                Object tValue = to.getValue();
                if (tValue instanceof Integer) {
                    NSPA.setFinalValue(((Integer) tValue).intValue());
                } else if (tValue instanceof Double) {
                    NSPA.setFinalValue(((Double) tValue).intValue());
                }
                to.setValue(new Double(NSPA.getFinalValue()));

                if (NSPA.getFinalValue() > NSPA.getInitialValue()) {
                    if ((Integer) steps.getValue() >= (NSPA.getFinalValue() - NSPA.getInitialValue() + 1)) {
                        NSPA.setNumberOfSteps((int) (NSPA.getFinalValue() - NSPA.getInitialValue() + 1));
                    } else {
                        NSPA.setNumberOfSteps((Integer) steps.getValue());
                    }
                } else if (NSPA.getInitialValue() > NSPA.getFinalValue()) {
                    if ((Integer) steps.getValue() >= (NSPA.getInitialValue() - NSPA.getFinalValue() + 1)) {
                        NSPA.setNumberOfSteps((int) (NSPA.getInitialValue() - NSPA.getFinalValue() + 1));
                    } else {
                        NSPA.setNumberOfSteps((Integer) steps.getValue());
                    }
                    // convert "From" and "To"
                    from.setValue(new Double(NSPA.getInitialValue()));
                    to.setValue(new Double(NSPA.getFinalValue()));
                    Object sub = from.getValue();
                    from.setValue(new Double(NSPA.getFinalValue()));
                    if (to.getValue() instanceof Integer) {
                        to.setValue((Integer) sub);
                    } else if (to.getValue() instanceof Double) {
                        to.setValue((Double) sub);
                    }
                } else {
                    if (NSPA.getNumberOfSteps() >= 2) {
                        NSPA.setNumberOfSteps(1);
                        steps.setValue(new Integer(NSPA.getNumberOfSteps()));
                    }
                }
                steps.setValue(NSPA.getNumberOfSteps());
            }
        });
        steps.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (steps.getValue() instanceof Integer) {
                    Integer sValue = (Integer) steps.getValue();
                    if (NSPA.getFinalValue() > NSPA.getInitialValue()) {
                        if (sValue >= (NSPA.getFinalValue() - NSPA.getInitialValue() + 1)) {
                            NSPA.setNumberOfSteps((int) (NSPA.getFinalValue() - NSPA.getInitialValue() + 1));
                        } else {
                            NSPA.setNumberOfSteps((Integer) steps.getValue());
                        }
                    } else if (NSPA.getInitialValue() > NSPA.getFinalValue()) {
                        if (sValue >= (NSPA.getInitialValue() - NSPA.getFinalValue() + 1)) {
                            NSPA.setNumberOfSteps((int) (NSPA.getInitialValue() - NSPA.getFinalValue() + 1));
                        } else {
                            NSPA.setNumberOfSteps((Integer) steps.getValue());
                        }
                        // convert "From" and "To"
                        from.setValue(new Double(NSPA.getInitialValue()));
                        to.setValue(new Double(NSPA.getFinalValue()));
                        Object sub = from.getValue();
                        from.setValue(new Double(NSPA.getFinalValue()));
                        if (to.getValue() instanceof Integer) {
                            to.setValue((Integer) sub);
                        } else if (to.getValue() instanceof Double) {
                            to.setValue((Double) sub);
                        }
                    } else {
                        if (NSPA.getNumberOfSteps() >= 2) {
                            NSPA.setNumberOfSteps(1);
                        }
                    }
                    steps.setValue(NSPA.getNumberOfSteps());
                }
            }
        });
        stationChooser.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.DESELECTED) {
                    return;
                }
                String stationName = (String) stationChooser.getSelectedItem();
                NSPA.setReferenceStation(getStationKey(stationName));

                if (serverTypeChooser != null) {
                    serverTypeChooser.setModel(new DefaultComboBoxModel(NSPA.getServerTypeNames()));
                    NSPA.setServerType((String) serverTypeChooser.getSelectedItem());
                }

                NSPA.setDefaultInitialValue();
                NSPA.setDefaultFinalValue();
                from.setModel(new SpinnerNumberModel(NSPA.getInitialValue(), 1, Double.MAX_VALUE, 1));
                from.setValue(new Double(NSPA.getInitialValue()));
                to.setModel(new SpinnerNumberModel(NSPA.getFinalValue(), 1, Double.MAX_VALUE, 1));
                to.setValue(new Double(NSPA.getFinalValue()));

                if (NSPA.getFinalValue() > NSPA.getInitialValue()) {
                    if ((Integer) steps.getValue() >= (NSPA.getFinalValue() - NSPA.getInitialValue() + 1)) {
                        NSPA.setNumberOfSteps((int) (NSPA.getFinalValue() - NSPA.getInitialValue() + 1));
                    } else {
                        NSPA.setNumberOfSteps((Integer) steps.getValue());
                    }
                } else if (NSPA.getInitialValue() > NSPA.getFinalValue()) {
                    if ((Integer) steps.getValue() >= (NSPA.getInitialValue() - NSPA.getFinalValue() + 1)) {
                        NSPA.setNumberOfSteps((int) (NSPA.getInitialValue() - NSPA.getFinalValue() + 1));
                    } else {
                        NSPA.setNumberOfSteps((Integer) steps.getValue());
                    }
                    // convert "From" and "To"
                    from.setValue(new Double(NSPA.getInitialValue()));
                    to.setValue(new Double(NSPA.getFinalValue()));
                    Object sub = from.getValue();
                    from.setValue(new Double(NSPA.getFinalValue()));
                    if (to.getValue() instanceof Integer) {
                        to.setValue((Integer) sub);
                    } else if (to.getValue() instanceof Double) {
                        to.setValue((Double) sub);
                    }
                } else {
                    if (NSPA.getNumberOfSteps() >= 2) {
                        NSPA.setNumberOfSteps(1);
                        steps.setValue(new Integer(NSPA.getNumberOfSteps()));
                    }
                }
                steps.setValue(NSPA.getNumberOfSteps());
            }
        });
        if (serverTypeChooser != null) {
            serverTypeChooser.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.DESELECTED) {
                        return;
                    }
                    NSPA.setServerType((String) serverTypeChooser.getSelectedItem());

                    NSPA.setDefaultInitialValue();
                    NSPA.setDefaultFinalValue();
                    from.setModel(new SpinnerNumberModel(NSPA.getInitialValue(), 1, Double.MAX_VALUE, 1));
                    from.setValue(new Double(NSPA.getInitialValue()));
                    to.setModel(new SpinnerNumberModel(NSPA.getFinalValue(), 1, Double.MAX_VALUE, 1));
                    to.setValue(new Double(NSPA.getFinalValue()));

                    if (NSPA.getFinalValue() > NSPA.getInitialValue()) {
                        if ((Integer) steps.getValue() >= (NSPA.getFinalValue() - NSPA.getInitialValue() + 1)) {
                            NSPA.setNumberOfSteps((int) (NSPA.getFinalValue() - NSPA.getInitialValue() + 1));
                        } else {
                            NSPA.setNumberOfSteps((Integer) steps.getValue());
                        }
                    } else if (NSPA.getInitialValue() > NSPA.getFinalValue()) {
                        if ((Integer) steps.getValue() >= (NSPA.getInitialValue() - NSPA.getFinalValue() + 1)) {
                            NSPA.setNumberOfSteps((int) (NSPA.getInitialValue() - NSPA.getFinalValue() + 1));
                        } else {
                            NSPA.setNumberOfSteps((Integer) steps.getValue());
                        }
                        // convert "From" and "To"
                        from.setValue(new Double(NSPA.getInitialValue()));
                        to.setValue(new Double(NSPA.getFinalValue()));
                        Object sub = from.getValue();
                        from.setValue(new Double(NSPA.getFinalValue()));
                        if (to.getValue() instanceof Integer) {
                            to.setValue((Integer) sub);
                        } else if (to.getValue() instanceof Double) {
                            to.setValue((Double) sub);
                        }
                    } else {
                        if (NSPA.getNumberOfSteps() >= 2) {
                            NSPA.setNumberOfSteps(1);
                            steps.setValue(new Integer(NSPA.getNumberOfSteps()));
                        }
                    }
                    steps.setValue(NSPA.getNumberOfSteps());
                }
            });
        }
    }

}
