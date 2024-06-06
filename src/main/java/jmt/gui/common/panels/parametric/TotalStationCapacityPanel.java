package jmt.gui.common.panels.parametric;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
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
import jmt.gui.common.definitions.parametric.TotalStationCapacityParametricAnalysis;

/**
 * <p>Title: TotalStationCapacityPanel</p>
 * <p>Description: this is the panel for he parametric analysis where
 * the total capacity inside a station is modified.</p>
 *
 * @author Xinyu Gao
 *         Date: 1-7-2023
 */

public class TotalStationCapacityPanel extends ParameterOptionPanel {
    private static final long serialVersionUID = 1L;
    private JLabel fromLabel;
    private JSpinner from;
    private JLabel toLabel;
    private JSpinner to;
    private JLabel stepsLabel;
    private JSpinner steps;
    private JLabel stationChooserLabel;
    private JComboBox stationChooser;
    private JScrollPane scroll;
    private JTextArea description;
    private JScrollPane descrPane;
    private TitledBorder descriptionTitle;
    private ParametricAnalysisChecker checker;

    private TotalStationCapacityParametricAnalysis TSCPA;

    public TotalStationCapacityPanel(TotalStationCapacityParametricAnalysis tscpa, ClassDefinition classDef, StationDefinition stationDef, SimulationDefinition simDef) {
        super();
        TSCPA = tscpa;
        super.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        super.setDividerSize(3);

        DESCRIPTION = "Repeat the simulation with different total capacity of a station.\n\n"
                + "The 'To' value represents the final total station capacity.\n\n"
                + "The 'From' value represents the initial total station capacity.\n\n";

        cd = classDef;
        sd = stationDef;
        simd = simDef;
        checker = new ParametricAnalysisChecker(cd, sd, simd);

        initialize();
    }

    public void initialize() {
        JPanel edit = new JPanel(new GridLayout(5, 1, 0, 5));
        ParametricAnalysisChecker checker = new ParametricAnalysisChecker(cd, sd, simd);
        Vector<Object> availableS = checker.checkForTotalStationCapacityParametricAnalysisAvailableStations();
        String[] stationNames = new String[availableS.size()];
        for (int i = 0; i < availableS.size(); i++) {
            stationNames[i] = sd.getStationName(availableS.get(i));
        }

        stationChooser = new JComboBox(stationNames);
        stationChooser.setToolTipText("Choose the station whose total capacity will be changed");

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
        if (availableS.contains(TSCPA.getReferenceStation())) {
            stationChooser.setSelectedItem(sd.getStationName(TSCPA.getReferenceStation()));
        }
        //else select another one
        else {
            TSCPA.setReferenceStation(availableS.get(0));
            stationChooser.setSelectedIndex(0);
        }

        fromLabel = new JLabel("From:");
        from = new JSpinner(new SpinnerNumberModel(TSCPA.getInitialValue(), 1, Integer.MAX_VALUE, 1));
        from.setEnabled(true);
        from.setToolTipText("Sets the initial total capacity in the station");
        toLabel = new JLabel("To:");
        to = new JSpinner(new SpinnerNumberModel(TSCPA.getFinalValue(), 1, Integer.MAX_VALUE, 1));
        to.setToolTipText("Sets the final total capacity in the station");
        stepsLabel = new JLabel("Steps: ");
        steps = new JSpinner(new SpinnerNumberModel((int)(TSCPA.getFinalValue() - TSCPA.getInitialValue() + 1), 1, ParametricAnalysis.MAX_STEP_NUMBER, 1));
        TSCPA.setNumberOfSteps((int)(TSCPA.getFinalValue() - TSCPA.getInitialValue() + 1));
        steps.setToolTipText("Sets the number of steps to be performed");

        description.setText(DESCRIPTION);

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
        JPanel editPanel = new JPanel();
        editPanel.add(editLables);
        editPanel.add(edit);
        editPanel.setBorder(new EmptyBorder(10, 20, 0, 20));
        JPanel cont = new JPanel(new BorderLayout());
        cont.add(editPanel, BorderLayout.CENTER);
        scroll = new JScrollPane(cont);
        title = new TitledBorder("Station total capacity variation");
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
                    TSCPA.setInitialValue(((Integer) fValue).intValue());
                } else if (fValue instanceof Double) {
                    TSCPA.setInitialValue(((Double) fValue).intValue());
                }
                from.setValue(new Double(TSCPA.getInitialValue()));

                if (TSCPA.getFinalValue() > TSCPA.getInitialValue()) {
                    if ((Integer) steps.getValue() >= (TSCPA.getFinalValue() - TSCPA.getInitialValue() + 1)) {
                        TSCPA.setNumberOfSteps((int) (TSCPA.getFinalValue() - TSCPA.getInitialValue() + 1));
                    } else {
                        TSCPA.setNumberOfSteps((Integer) steps.getValue());
                    }
                } else if (TSCPA.getInitialValue() > TSCPA.getFinalValue()) {
                    if ((Integer) steps.getValue() >= (TSCPA.getInitialValue() - TSCPA.getFinalValue() + 1)) {
                        TSCPA.setNumberOfSteps((int) (TSCPA.getInitialValue() - TSCPA.getFinalValue() + 1));
                    } else {
                        TSCPA.setNumberOfSteps((Integer) steps.getValue());
                    }
                    // convert "From" and "To"
                    from.setValue(new Double(TSCPA.getInitialValue()));
                    to.setValue(new Double(TSCPA.getFinalValue()));
                    Object sub = from.getValue();
                    from.setValue(new Double(TSCPA.getFinalValue()));
                    if (to.getValue() instanceof Integer) {
                        to.setValue((Integer) sub);
                    } else if (to.getValue() instanceof Double) {
                        to.setValue((Double) sub);
                    }
                } else {
                    if (TSCPA.getNumberOfSteps() >= 2) {
                        TSCPA.setNumberOfSteps(1);
                        steps.setValue(new Integer(TSCPA.getNumberOfSteps()));
                    }
                }
                steps.setValue(TSCPA.getNumberOfSteps());
            }
        });
        to.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                Object tValue = to.getValue();
                if (tValue instanceof Integer) {
                    TSCPA.setFinalValue(((Integer) tValue).intValue());
                } else if (tValue instanceof Double) {
                    TSCPA.setFinalValue(((Double) tValue).intValue());
                }
                to.setValue(new Double(TSCPA.getFinalValue()));

                if (TSCPA.getFinalValue() > TSCPA.getInitialValue()) {
                    if ((Integer) steps.getValue() >= (TSCPA.getFinalValue() - TSCPA.getInitialValue() + 1)) {
                        TSCPA.setNumberOfSteps((int) (TSCPA.getFinalValue() - TSCPA.getInitialValue() + 1));
                    } else {
                        TSCPA.setNumberOfSteps((Integer) steps.getValue());
                    }
                } else if (TSCPA.getInitialValue() > TSCPA.getFinalValue()) {
                    if ((Integer) steps.getValue() >= (TSCPA.getInitialValue() - TSCPA.getFinalValue() + 1)) {
                        TSCPA.setNumberOfSteps((int) (TSCPA.getInitialValue() - TSCPA.getFinalValue() + 1));
                    } else {
                        TSCPA.setNumberOfSteps((Integer) steps.getValue());
                    }
                    // convert "From" and "To"
                    from.setValue(new Double(TSCPA.getInitialValue()));
                    to.setValue(new Double(TSCPA.getFinalValue()));
                    Object sub = from.getValue();
                    from.setValue(new Double(TSCPA.getFinalValue()));
                    if (to.getValue() instanceof Integer) {
                        to.setValue((Integer) sub);
                    } else if (to.getValue() instanceof Double) {
                        to.setValue((Double) sub);
                    }
                } else {
                    if (TSCPA.getNumberOfSteps() >= 2) {
                        TSCPA.setNumberOfSteps(1);
                        steps.setValue(new Integer(TSCPA.getNumberOfSteps()));
                    }
                }
                steps.setValue(TSCPA.getNumberOfSteps());
            }
        });
        steps.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (steps.getValue() instanceof Integer) {
                    Integer sValue = (Integer) steps.getValue();
                    if (TSCPA.getFinalValue() > TSCPA.getInitialValue()) {
                        if (sValue >= (TSCPA.getFinalValue() - TSCPA.getInitialValue() + 1)) {
                            TSCPA.setNumberOfSteps((int) (TSCPA.getFinalValue() - TSCPA.getInitialValue() + 1));
                        } else {
                            TSCPA.setNumberOfSteps((Integer) steps.getValue());
                        }
                    } else if (TSCPA.getInitialValue() > TSCPA.getFinalValue()) {
                        if (sValue >= (TSCPA.getInitialValue() - TSCPA.getFinalValue() + 1)) {
                            TSCPA.setNumberOfSteps((int) (TSCPA.getInitialValue() - TSCPA.getFinalValue() + 1));
                        } else {
                            TSCPA.setNumberOfSteps((Integer) steps.getValue());
                        }
                        // convert "From" and "To"
                        from.setValue(new Double(TSCPA.getInitialValue()));
                        to.setValue(new Double(TSCPA.getFinalValue()));
                        Object sub = from.getValue();
                        from.setValue(new Double(TSCPA.getFinalValue()));
                        if (to.getValue() instanceof Integer) {
                            to.setValue((Integer) sub);
                        } else if (to.getValue() instanceof Double) {
                            to.setValue((Double) sub);
                        }
                    } else {
                        if (TSCPA.getNumberOfSteps() >= 2) {
                            TSCPA.setNumberOfSteps(1);
                        }
                    }
                    steps.setValue(TSCPA.getNumberOfSteps());
                }
            }
        });
        stationChooser.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.DESELECTED) {
                    return;
                }
                String stationName = (String) stationChooser.getSelectedItem();
                TSCPA.setReferenceStation(getStationKey(stationName));

                TSCPA.setDefaultInitialValue();
                TSCPA.setDefaultFinalValue();
                from.setModel(new SpinnerNumberModel(TSCPA.getInitialValue(), 1, Double.MAX_VALUE, 1));
                from.setValue(new Double(TSCPA.getInitialValue()));
                to.setModel(new SpinnerNumberModel(TSCPA.getFinalValue(), 1, Double.MAX_VALUE, 1));
                to.setValue(new Double(TSCPA.getFinalValue()));

                if (TSCPA.getFinalValue() > TSCPA.getInitialValue()) {
                    if ((Integer) steps.getValue() >= (TSCPA.getFinalValue() - TSCPA.getInitialValue() + 1)) {
                        TSCPA.setNumberOfSteps((int) (TSCPA.getFinalValue() - TSCPA.getInitialValue() + 1));
                    } else {
                        TSCPA.setNumberOfSteps((Integer) steps.getValue());
                    }
                } else if (TSCPA.getInitialValue() > TSCPA.getFinalValue()) {
                    if ((Integer) steps.getValue() >= (TSCPA.getInitialValue() - TSCPA.getFinalValue() + 1)) {
                        TSCPA.setNumberOfSteps((int) (TSCPA.getInitialValue() - TSCPA.getFinalValue() + 1));
                    } else {
                        TSCPA.setNumberOfSteps((Integer) steps.getValue());
                    }
                    // convert "From" and "To"
                    from.setValue(new Double(TSCPA.getInitialValue()));
                    to.setValue(new Double(TSCPA.getFinalValue()));
                    Object sub = from.getValue();
                    from.setValue(new Double(TSCPA.getFinalValue()));
                    if (to.getValue() instanceof Integer) {
                        to.setValue((Integer) sub);
                    } else if (to.getValue() instanceof Double) {
                        to.setValue((Double) sub);
                    }
                } else {
                    if (TSCPA.getNumberOfSteps() >= 2) {
                        TSCPA.setNumberOfSteps(1);
                        steps.setValue(new Integer(TSCPA.getNumberOfSteps()));
                    }
                }
                steps.setValue(TSCPA.getNumberOfSteps());
            }
        });
    }

}
