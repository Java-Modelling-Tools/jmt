package jmt.gui.common.panels.parametric;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
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
import javax.swing.border.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;


import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.SimulationDefinition;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.common.definitions.parametric.ParametricAnalysis;
import jmt.gui.common.definitions.parametric.ParametricAnalysisChecker;
import jmt.gui.common.definitions.parametric.RoutingProbabilitiesParametricAnalysis;

/**
 * <p>Title: RoutingProbabilitiesPanel</p>
 * <p>Description: this is the panel for he parametric analysis where
 * the routing probabilities of a class inside a station is modified.</p>
 *
 * @author Xinyu Gao
 *         Date: 11-Aug-2023
 */
public class RoutingProbabilitiesPanel extends ParameterOptionPanel {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private JRadioButton allClass;
    private JRadioButton singleClass;
    private JLabel fromLabel;
    private JSpinner from;
    private JLabel toLabel;
    private JSpinner to;
    private JLabel stepsLabel;
    private JSpinner steps;
    private JLabel classChooserLabel;
    private JComboBox classChooser;
    private JLabel sourceStationChooserLabel;
    private JComboBox sourceStationChooser;
    private JLabel destinationStationChooserLabel;
    private JComboBox destinationStationChooser;
    private JScrollPane scroll;
    private JTextArea description;
    private JTextArea description_rp;
    private JScrollPane descrPane;
    private TitledBorder descriptionTitle;
    private ParametricAnalysisChecker checker;
    private String DESCRIPTION_SINGLE;
    private String DESCRIPTION_ROUTING_PROBABILITY;

    private RoutingProbabilitiesParametricAnalysis RPPA;

    private Vector<Object> availableDestinationS;
    private String[] destinationStationNames;
    private JButton showTableButton;
    private JTable table;
    private JScrollPane tablePane;
    private JFrame tableFrame;

    public RoutingProbabilitiesPanel(RoutingProbabilitiesParametricAnalysis rppa, ClassDefinition classDef, StationDefinition stationDef, SimulationDefinition simDef) {
        super();
        RPPA = rppa;
        super.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        super.setDividerSize(3);
        DESCRIPTION = "Repeat the simulation with different routing probabilities of a station for" + " the jobs of all the classes.\n\n"
                + "The 'To' value represents the final routing probability.\n\n"
                + "The 'all class' option"
                + " will not be enabled for those source stations where the following condition is true:\n"
                + " - there is only one class available from source station to destination station of which the routing strategy is probability routing.\n";

        DESCRIPTION_SINGLE = "Repeat the simulation changing the routing probabilities of a station for" + " one class only.\n\n"
                + "The 'To' value represents the final value of routing probabilities.";
        DESCRIPTION_ROUTING_PROBABILITY = "The table below provides an intuitive sense of how the routing probability of each Queue changes dynamically.\n\n"
                + "By changing the values of different options, the table will change automatically.";

        cd = classDef;
        sd = stationDef;
        simd = simDef;
        checker = new ParametricAnalysisChecker(cd, sd, simd);
        initialize();
    }

    public void initialize() {
        JPanel radioButtonsPanel = new JPanel(new GridLayout(2, 1));
        allClass = new JRadioButton("Change routing probabilities of all classes");
        allClass.setToolTipText("Change routing probabilities of all classes");
        singleClass = new JRadioButton("Change routing probability of one class");
        singleClass.setToolTipText("Change only the routing probability of one class");
        ButtonGroup group = new ButtonGroup();
        group.add(allClass);
        group.add(singleClass);
        allClass.setSelected(true);
        radioButtonsPanel.add(allClass);
        radioButtonsPanel.add(singleClass);
        radioButtonsPanel.setBorder(new EmptyBorder(5, 20, 0, 20));
        JPanel edit = new JPanel(new GridLayout(6, 1, 0, 6));
        ParametricAnalysisChecker checker = new ParametricAnalysisChecker(cd, sd, simd);
        Vector<Object> availableSourceS = checker.checkForRoutingProbabilitiesParametricAnalysisAvailableSourceStations();
        String[] sourceStationNames = new String[availableSourceS.size()];
        for (int i = 0; i < availableSourceS.size(); i++) {
            sourceStationNames[i] = sd.getStationName(availableSourceS.get(i));
        }

        classChooserLabel = new JLabel("Class:");
        classChooser = new JComboBox();
        classChooser.setToolTipText("Choose the class whose routing probability inside the selected station will change");
        sourceStationChooser = new JComboBox(sourceStationNames);  // set available source stations to sourceStationChooser
        sourceStationChooser.setToolTipText("Choose the source station whose routing probabilities will be changed");
        Object firstStationKey = availableSourceS.get(0);
        Vector<Object> firstAvaiDestStationsKeys = sd.getForwardConnections(firstStationKey);
        String[] destStationNames = new String[firstAvaiDestStationsKeys.size()];
        for (int i = 0; i < firstAvaiDestStationsKeys.size(); i++) {
            destStationNames[i] = sd.getStationName(firstAvaiDestStationsKeys.get(i));
        }
        destinationStationChooser = new JComboBox(destStationNames);
        destinationStationChooser.setToolTipText("Choose the destination station whose routing probabilities will be changed");
        description = new JTextArea();
        description.setOpaque(false);
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);

        description_rp = new JTextArea();
        description_rp.setOpaque(false);
        description_rp.setEditable(false);
        description_rp.setLineWrap(true);
        description_rp.setWrapStyleWord(true);
        description_rp.setText(DESCRIPTION_ROUTING_PROBABILITY);

        Object[] columns = RPPA.getDestStationNames();
        Object[][] data = RPPA.getTableData();
        Object[][] initialData = RPPA.getRoutingProbabilityTable();
        DefaultTableModel model = new DefaultTableModel(initialData, columns);
        table = new JTable(model);
        tablePane = new JScrollPane(table);

        JPanel combinedPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        combinedPanel.add(description, gbc);

        gbc.gridy = 1;
        gbc.weighty = 0.2;
        combinedPanel.add(description_rp, gbc);

        gbc.gridy = 2;
        gbc.weighty = 0.3;
        combinedPanel.add(tablePane, gbc);

        descrPane = new JScrollPane(combinedPanel);
        descriptionTitle = new TitledBorder(new EtchedBorder(), "Description");
        descrPane.setBorder(descriptionTitle);
        descrPane.setMinimumSize(new Dimension((int)(80 * CommonConstants.widthScaling), (int)(0 * CommonConstants.heightScaling)));

        //if the reference station of the RPPA is still available
        if (availableSourceS.contains(RPPA.getReferenceStation())) {
            sourceStationChooser.setSelectedItem(sd.getStationName(RPPA.getReferenceStation()));
        }
        //else select another one
        else {
            RPPA.setReferenceStation(availableSourceS.get(0));
            sourceStationChooser.setSelectedIndex(0);
        }
        if (RPPA.isSingleClass()) {
            fromLabel = new JLabel("From:");
            from = new JSpinner(new SpinnerNumberModel(RPPA.getInitialValue(), 0, 1.0, 0.001));
            from.setToolTipText("Sets the initial routing probability");
            from.setEnabled(true);
            toLabel = new JLabel("To:");
            to = new JSpinner(new SpinnerNumberModel(RPPA.getFinalValue(), 0, 1.0, 0.01));
            to.setToolTipText("Sets the final routing probability");
            stepsLabel = new JLabel("Steps: ");
            steps = new JSpinner(new SpinnerNumberModel(RPPA.getNumberOfSteps(), 2, ParametricAnalysis.MAX_STEP_NUMBER, 1));
            steps.setToolTipText("Sets the number of steps to be performed");
            Vector<Object> validC = checker.checkForRoutingProbabilitiesParametricSimulationAvailableClasses(RPPA.getReferenceStation());
            String[] classeNames = new String[validC.size()];
            for (int i = 0; i < validC.size(); i++) {
                classeNames[i] = cd.getClassName(validC.get(i));
            }
            classChooser.removeAllItems();
            for (String classeName : classeNames) {
                classChooser.addItem(classeName);
            }
            classChooser.setEnabled(true);
            classChooser.setSelectedItem(cd.getClassName(RPPA.getReferenceClass()));
            singleClass.setSelected(true);
            if (cd.getClosedClassKeys().size() == 1 || validC.size() < cd.getClassKeys().size()) {
                allClass.setEnabled(false);
            }
            allClass.setSelected(false);
            description.setText(DESCRIPTION_SINGLE);
        } else {
            fromLabel = new JLabel("From:");
            from = new JSpinner(new SpinnerNumberModel(RPPA.getInitialValue(), 0, 1.0, 0.01));
            from.setEnabled(true);
            from.setToolTipText("Sets the initial routing probability");
            toLabel = new JLabel("To:");
            to = new JSpinner(new SpinnerNumberModel(RPPA.getFinalValue(), 0, 1.0, 0.01));
            to.setToolTipText("Sets the final routing probability");
            stepsLabel = new JLabel("Steps: ");
            steps = new JSpinner(new SpinnerNumberModel(RPPA.getNumberOfSteps(), 2, ParametricAnalysis.MAX_STEP_NUMBER, 1));
            steps.setToolTipText("Sets the number of steps to be performed");
            classChooser.setEnabled(false);
            singleClass.setSelected(false);
            allClass.setSelected(true);
            description.setText(DESCRIPTION);
            classChooser.addItem("All classes");
        }
        from.setBackground(Color.WHITE);
        sourceStationChooserLabel = new JLabel("Source Station: ");
        destinationStationChooserLabel = new JLabel("Destination Station: ");
        edit.add(fromLabel);
        edit.add(from);
        edit.add(toLabel);
        edit.add(to);
        edit.add(stepsLabel);
        edit.add(steps);
        edit.add(classChooserLabel);
        edit.add(sourceStationChooser);
        edit.add(destinationStationChooser);
        edit.add(classChooser);
        edit.setPreferredSize(new Dimension((int)(130 * CommonConstants.widthScaling), (int)(160 * CommonConstants.heightScaling)));
        JPanel editLables = new JPanel(new GridLayout(6, 1, 0, 6));
        editLables.add(fromLabel);
        editLables.add(toLabel);
        editLables.add(stepsLabel);
        editLables.add(sourceStationChooserLabel);
        editLables.add(destinationStationChooserLabel);
        editLables.add(classChooserLabel);
        editLables.setPreferredSize(new Dimension((int)(130 * CommonConstants.widthScaling), (int)(160 * CommonConstants.heightScaling)));
        JPanel editPanel = new JPanel();
        editPanel.add(editLables);
        editPanel.add(edit);
        editPanel.setBorder(new EmptyBorder(10, 20, 0, 20));
        JPanel cont = new JPanel(new BorderLayout());
        cont.add(radioButtonsPanel, BorderLayout.NORTH);
        cont.add(editPanel, BorderLayout.CENTER);
        scroll = new JScrollPane(cont);
        title = new TitledBorder("Type of routing probability growth");
        scroll.setBorder(title);
        scroll.setMinimumSize(new Dimension((int)(360 * CommonConstants.widthScaling), (int)(0 * CommonConstants.heightScaling)));
        setLeftComponent(scroll);
        setRightComponent(descrPane);
        this.setBorder(new EmptyBorder(5, 0, 5, 0));
        setListeners();
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled) {
            Vector<Object> validC = checker.checkForRoutingProbabilitiesParametricSimulationAvailableClasses(RPPA.getReferenceStation());
            if (cd.getClassKeys().size() == 1 || validC.size() < cd.getClassKeys().size()) {
                allClass.setEnabled(false);
            } else {
                allClass.setEnabled(true);
            }
        } else {
            allClass.setEnabled(false);
        }
        singleClass.setEnabled(enabled);
        fromLabel.setEnabled(enabled);
        from.setEnabled(enabled);
        toLabel.setEnabled(enabled);
        to.setEnabled(enabled);
        stepsLabel.setEnabled(enabled);
        steps.setEnabled(enabled);
        description.setEnabled(enabled);
        classChooserLabel.setEnabled(enabled);
        if (!enabled) {
            classChooser.setEnabled(enabled);
        } else if (singleClass.isSelected()) {
            classChooser.setEnabled(enabled);
        }
        sourceStationChooserLabel.setEnabled(enabled);
        sourceStationChooser.setEnabled(enabled);
        destinationStationChooserLabel.setEnabled(enabled);
        destinationStationChooser.setEnabled(enabled);
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
                if (fValue instanceof Double) {
                    RPPA.setInitialValue(((Double) fValue).doubleValue());
                } else if (fValue instanceof Integer) {
                    RPPA.setInitialValue(((Integer) fValue).doubleValue());
                }
                from.setValue(new Double(RPPA.getInitialValue()));

                Object[] columns = RPPA.getDestStationNames();
                Object[][] updatedData = RPPA.getRoutingProbabilityTable();
                DefaultTableModel updatedModel = new DefaultTableModel(updatedData, columns);
                table.setModel(updatedModel);
            }
        });
        to.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                Object tValue = to.getValue();
                if (tValue instanceof Double) {
                    RPPA.setFinalValue(((Double) tValue).doubleValue());
                } else if (tValue instanceof Integer) {
                    RPPA.setFinalValue(((Integer) tValue).doubleValue());
                }
                to.setValue(new Double(RPPA.getFinalValue()));

                Object[] columns = RPPA.getDestStationNames();
                Object[][] updatedData = RPPA.getRoutingProbabilityTable();
                DefaultTableModel updatedModel = new DefaultTableModel(updatedData, columns);
                table.setModel(updatedModel);
            }
        });
        steps.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (steps.getValue() instanceof Integer) {
                    Integer sValue = (Integer) steps.getValue();
                    RPPA.setNumberOfSteps(sValue.intValue());
                }
                steps.setValue(new Integer(RPPA.getNumberOfSteps()));

                Object[] columns = RPPA.getDestStationNames();
                Object[][] updatedData = RPPA.getRoutingProbabilityTable();
                DefaultTableModel updatedModel = new DefaultTableModel(updatedData, columns);
                table.setModel(updatedModel);
            }
        });
        allClass.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.DESELECTED) {
                    return;
                }
                RPPA.setSingleClass(false);
                fromLabel.setText("From:");
                toLabel.setText("To: ");
                RPPA.setDefaultInitialValue();
                RPPA.setDefaultFinalValue();
                from.setModel(new SpinnerNumberModel(RPPA.getInitialValue(), 0, 1.0, 0.01));
                from.setValue(new Double(RPPA.getInitialValue()));
                from.setToolTipText("Sets the initial value of routing probability");
                to.setModel(new SpinnerNumberModel(RPPA.getFinalValue(), 0, 1.0, 0.01));
                to.setValue(new Double(RPPA.getFinalValue()));
                to.setToolTipText("Sets the final value of routing probability");
                classChooser.removeAllItems();
                classChooser.addItem("All classes");
                classChooser.setEnabled(false);
                description.setText(DESCRIPTION);

                Object[] columns = RPPA.getDestStationNames();
                Object[][] updatedData = RPPA.getRoutingProbabilityTable();
                DefaultTableModel updatedModel = new DefaultTableModel(updatedData, columns);
                table.setModel(updatedModel);
            }
        });
        singleClass.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.DESELECTED) {
                    return;
                }
                RPPA.setSingleClass(true);
                Vector<Object> validC = checker.checkForRoutingProbabilitiesParametricSimulationAvailableClasses(RPPA.getReferenceStation());
                String[] classNames = new String[validC.size()];
                for (int i = 0; i < validC.size(); i++) {
                    classNames[i] = cd.getClassName(validC.get(i));
                }
                classChooser.setEnabled(true);
                ItemListener listener = classChooser.getItemListeners()[0];
                classChooser.removeItemListener(listener);
                classChooser.removeAllItems();
                for (String className : classNames) {
                    classChooser.addItem(className);
                }
                classChooser.addItemListener(listener);
                //if no classes where previously associated, associate
                //the first one
                if (!validC.contains(RPPA.getReferenceClass())) {
                    RPPA.setReferenceClass(validC.get(0));
                }
                classChooser.setSelectedItem(cd.getClassName(RPPA.getReferenceClass()));
                fromLabel.setText("From:");
                toLabel.setText("To:");
                RPPA.setDefaultInitialValue();
                RPPA.setDefaultFinalValue();
                from.setModel(new SpinnerNumberModel(RPPA.getInitialValue(), 0, 1.0, 0.001));
                from.setValue(new Double(RPPA.getInitialValue()));
                from.setToolTipText("Sets the initial value of routing probability");
                to.setModel(new SpinnerNumberModel(RPPA.getFinalValue(), 0, 1.0, 0.01));
                to.setValue(new Double(RPPA.getFinalValue()));
                to.setToolTipText("Sets the final value of routing probability");
                description.setText(DESCRIPTION_SINGLE);

                Object[] columns = RPPA.getDestStationNames();
                Object[][] updatedData = RPPA.getRoutingProbabilityTable();
                DefaultTableModel updatedModel = new DefaultTableModel(updatedData, columns);
                table.setModel(updatedModel);
            }
        });
        classChooser.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.DESELECTED) {
                    return;
                }
                String className = (String) classChooser.getSelectedItem();
                Object classKey = null;
                Vector<Object> classes = cd.getClassKeys();
                //for loop used to get the key of the selected class
                for (int i = 0; i < classes.size(); i++) {
                    if (cd.getClassName(classes.get(i)).equals(className)) {
                        classKey = classes.get(i);
                        break;
                    }
                }
                RPPA.setReferenceClass(classKey);
                RPPA.setDefaultInitialValue();
                RPPA.setDefaultFinalValue();
                from.setValue(new Double(RPPA.getInitialValue()));
                to.setValue(new Double(RPPA.getFinalValue()));

                Object[] columns = RPPA.getDestStationNames();
                Object[][] updatedData = RPPA.getRoutingProbabilityTable();
                DefaultTableModel updatedModel = new DefaultTableModel(updatedData, columns);
                table.setModel(updatedModel);
            }
        });
        sourceStationChooser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String stationName = (String) sourceStationChooser.getSelectedItem();
                RPPA.setReferenceStation(getStationKey(stationName));  // 把当前选择的source station当作reference station
                availableDestinationS = sd.getForwardConnections(RPPA.getReferenceStation());
                RPPA.setDestinationStationKey(availableDestinationS.get(0));
                destinationStationNames = new String[availableDestinationS.size()];
                for (int i = 0; i < availableDestinationS.size(); i++) {
                    destinationStationNames[i] = sd.getStationName(availableDestinationS.get(i));
                }
                destinationStationChooser.removeAllItems();
                for (String name : destinationStationNames) {
                    destinationStationChooser.addItem(name);
                }
                Vector<Object> validC = checker.checkForRoutingProbabilitiesParametricSimulationAvailableClasses(RPPA.getReferenceStation());
                if (RPPA.isSingleClass()) {
                    String[] classes = new String[validC.size()];
                    for (int i = 0; i < validC.size(); i++) {
                        classes[i] = cd.getClassName(validC.get(i));
                    }
                    //if the reference station of the RPPA is no more valid get the first available
                    if (!validC.contains(RPPA.getReferenceClass())) {
                        RPPA.setReferenceClass(validC.get(0));
                    }
                    RPPA.setDefaultInitialValue();
                    RPPA.setDefaultFinalValue();
                    from.setValue(new Double(RPPA.getInitialValue()));
                    to.setValue(new Double(RPPA.getFinalValue()));
                    ItemListener listener = classChooser.getItemListeners()[0];
                    classChooser.removeItemListener(listener);
                    classChooser.removeAllItems();
                    for (String classe : classes) {
                        classChooser.addItem(classe);
                    }
                    classChooser.setEnabled(true);
                    classChooser.setSelectedItem(cd.getClassName(RPPA.getReferenceClass()));
                    classChooser.addItemListener(listener);
                    singleClass.setSelected(true);
                    allClass.setSelected(false);
                    //if the number of available classes is less than the total number of classes the all class routing
                    //probability parametric analysis is no more available.
                    if (validC.size() < cd.getClassKeys().size()) {
                        allClass.setEnabled(false);
                    } else {
                        allClass.setEnabled(true);
                    }
                } else {
                    //if the number of available classes is less than the total number of classes the all class routing
                    //probability parametric analysis is no more available...
                    if (validC.size() < cd.getClassKeys().size()) {
                        RPPA.setSingleClass(true);
                        String[] classes = new String[validC.size()];
                        for (int i = 0; i < validC.size(); i++) {
                            classes[i] = cd.getClassName(validC.get(i));
                        }
                        //set the reference class to the RPPA...
                        RPPA.setReferenceClass(validC.get(0));
                        RPPA.setDefaultInitialValue();
                        RPPA.setDefaultFinalValue();
                        from.setModel(new SpinnerNumberModel(RPPA.getInitialValue(), 0, 1.0, 0.001));
                        from.setValue(new Double(RPPA.getInitialValue()));
                        to.setModel(new SpinnerNumberModel(RPPA.getFinalValue(), 0, 1.0, 0.01));
                        to.setValue(new Double(RPPA.getFinalValue()));
                        //... and set the RPPA to be single class
                        singleClass.setSelected(true);
                        allClass.setEnabled(false);
                        description.setText(DESCRIPTION_SINGLE);
                    } else {
                        allClass.setEnabled(true);
                        classChooser.setEnabled(false);
                        singleClass.setSelected(false);
                        allClass.setSelected(true);
                    }
                }

                Object[] columns = RPPA.getDestStationNames();
                Object[][] updatedData = RPPA.getRoutingProbabilityTable();
                DefaultTableModel updatedModel = new DefaultTableModel(updatedData, columns);
                table.setModel(updatedModel);
            }
        });
        destinationStationChooser.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.DESELECTED) {
                    return;
                }
                String stationName = (String) destinationStationChooser.getSelectedItem();
                RPPA.setDestinationStationKey(getStationKey(stationName));

                Object[] columns = RPPA.getDestStationNames();
                Object[][] updatedData = RPPA.getRoutingProbabilityTable();
                DefaultTableModel updatedModel = new DefaultTableModel(updatedData, columns);
                table.setModel(updatedModel);
            }
        });
        classChooser.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.DESELECTED) {
                    return;
                }
                String className = (String) classChooser.getSelectedItem();
                Object classKey = null;
                Vector<Object> classes = cd.getClassKeys();
                for (int i = 0; i < classes.size(); i++) {
                    if (cd.getClassName(classes.get(i)).equals(className)) {
                        classKey = classes.get(i);
                        break;
                    }
                }
                RPPA.setReferenceClass(classKey);
                RPPA.setDefaultInitialValue();
                RPPA.setDefaultFinalValue();
                from.setValue(new Double(RPPA.getInitialValue()));
                to.setValue(new Double(RPPA.getFinalValue()));

                Object[] columns = RPPA.getDestStationNames();
                Object[][] updatedData = RPPA.getRoutingProbabilityTable();
                DefaultTableModel updatedModel = new DefaultTableModel(updatedData, columns);
                table.setModel(updatedModel);
            }
        });
    }
}
