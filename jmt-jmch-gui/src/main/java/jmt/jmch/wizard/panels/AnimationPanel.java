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
package jmt.jmch.wizard.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;

import random.engine.MersenneTwister;
import random.engine.RandomEngine;
import jmt.framework.gui.components.JMTMenuBar;
import jmt.framework.gui.components.JMTToolBar;
import jmt.framework.gui.help.HoverHelp;
import jmt.framework.gui.listeners.MenuAction;
import common.JMTImageLoader;
import common.controller.SimulationRunner;
import common.controller.SimulationRunnerFactory;
import common.definitions.GuiInterface;
import common.definitions.MeasureDefinition;
import common.definitions.ResultsModel;
import common.distributions.Exponential;
import common.editors.DistributionsEditor;
import common.xml.XMLWriter;
// JModelProblemsWindow import removed (unused)
import jmt.jmch.Constants;
import jmt.jmch.Solver;
import jmt.jmch.wizard.actionsWizard.About;
import jmt.jmch.wizard.actionsWizard.AbstractMCHAction;
import jmt.jmch.wizard.actionsWizard.DecreaseVelocity;
import jmt.jmch.wizard.actionsWizard.Exit;
import jmt.jmch.wizard.actionsWizard.Help;
import jmt.jmch.wizard.actionsWizard.IncreaseVelocity;
import jmt.jmch.wizard.actionsWizard.NextStepSimulation;
import jmt.jmch.wizard.actionsWizard.PauseSimulation;
import jmt.jmch.wizard.actionsWizard.RestartSimulation;
import jmt.jmch.wizard.actionsWizard.StartSimulation;
import jmt.jmch.animation.AnimationClass;
import jmt.jmch.animation.MultipleQueueNetAnimation;
import jmt.jmch.animation.SingleQueueNetAnimation;
import jmt.jmch.simulation.NonPreemptiveSimulation;
import jmt.jmch.simulation.Simulation;
import jmt.jmch.simulation.SimulationFactory;
import jmt.jmch.simulation.SimulationType;
import jmt.jmch.wizard.MainWizard;
import jmt.jmch.distributions.DistributionJMCH;


/**
 * Panel for JTeach models.
 * It is the same class for all types of Queueing Network.
 *
 * @author Lorenzo Torri
 * Date: 30-mar-2024
 * Time: 14.40
 */
public class AnimationPanel extends JMCHWizardPanel implements GuiInterface{
    private static final String PANEL_NAME = "Simulation";

    //------------ components of the panel -----------------------
    private JPanel mainPanel;
    private JPanel leftPanel;
    private JLabel descrLabel;
    private JComboBox<String> algorithmJComboBox = null;
    private JButton interAEdit;
    private JButton serviceEdit;
    private JSpinner serversSpinner;
    private JLabel interADistrLabel;
    private JLabel serviceDistrLabel;
    private JCheckBox simulationTypeCheckBox;

    private List<JComponent> editableComponents;

    private JLabel trafficIntensityLabel;

    private JSpinner prob1 = null; //those two spinners are instanciated only if Probabilisitic routing is selected
    private JSpinner prob2 = null;
    private JSpinner prob3 = null;
    private JPanel animationPanel;
    private HoverHelp help;
    
    //------------ variables for parameters JPanel ---------------
    private JPanel parametersPanel;
    private final int spaceBetweenPanels = 3;
    private JSpinner maxSamples;
    private JSpinner seedSpinner;

    //--- variables for Utilization label
    private final String sliderTraffic = "<html>Avg. Utilization <sub>per server</sub>: %.2f </html>";
    private final String sliderTrafficProb = "<html>Max. Utilization: %.2f </html>";
    private final String sliderTrafficSaturation = "<html>Avg. Utilization<sub>server</sub>: Saturation (%.2f) </html>";
    private final String sliderTrafficSaturationProb = "<html>Max. Utilization : Saturation (%.2f) </html>";
    private int nQueues = 1;
    private double U = 1.0;

    //-------------all the Actions of this panel------------------
    private AbstractMCHAction exit;
    private AbstractMCHAction start;
    private AbstractMCHAction pause;
    private AbstractMCHAction reload;
    private AbstractMCHAction nextStep;
    private AbstractMCHAction openHelp;
    private AbstractMCHAction about;
    private AbstractMCHAction increaseVelocity;
    private AbstractMCHAction decreaseVelocity;

    //------------ properties simulation velocity ---------------
    private int[] velocity = {1, 3, 5, 8};
    private int indexVelocity = 1;

    //--------------properties of the animation------------------
    private Simulation simulation;
    private AnimationClass animation;
    private List<String> algorithms; //array of all possible algorithms to select

    //------------- engine simulation --------------------------
    private Solver solver;

    //2 engines one for JMCH and JSIM, and 4 distributions in total
    private RandomEngine engineJMCH;
    private RandomEngine engineJSIM;
    private DistributionJMCH serviceDJMCH;
    private DistributionJMCH arrivalDJMCH;
    private DistributionJMCH serviceDJSIM;
    private DistributionJMCH arrivalDJSIM;
    private SimulationRunner dispatcher;
        

    /* Abstract action for the Edit Distribution Button */
    protected AbstractAction editArrivalDistrAction = new AbstractAction("Edit") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Edits distribution parameters");
		}

		public void actionPerformed(ActionEvent e) {
            DistributionsEditor editor = DistributionsEditor.getInstance(AnimationPanel.this.getParent(), arrivalDJMCH.getDistribution());
            editor.setTitle("Editing Inter Arrival Time distribution...");
            editor.setJMCH();
            editor.show();
            interADistrLabel.setText(editor.getResult().toString());
            arrivalDJMCH.convertDistribution(editor.getResult());
            arrivalDJSIM.convertDistribution(editor.getResult());
            editor.unsetJMCH();
            updateFields();
		}
	};

    protected AbstractAction editServiceDistrAction = new AbstractAction("Edit") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Edits distribution parameters");
		}

		public void actionPerformed(ActionEvent e) {
            DistributionsEditor editor = DistributionsEditor.getInstance(AnimationPanel.this.getParent(), serviceDJMCH.getDistribution());
            editor.setTitle("Editing Service Time distribution...");
            editor.setJMCH();
            editor.show();
            serviceDistrLabel.setText(editor.getResult().toString());
            serviceDJMCH.convertDistribution(editor.getResult());
            serviceDJSIM.convertDistribution(editor.getResult());
            editor.unsetJMCH();
            updateFields();
		}
	};

    private AnimationPanel(MainWizard main){
        this.parent = main;
        help = parent.getHoverHelp();
        editableComponents = new ArrayList<>();

        algorithms = NonPreemptiveSimulation.getAlgorithms();

        serversSpinner = new JSpinner();
        serversSpinner.setValue(1); //by default, since in routing policts the server spinner is not present, but its value is used in the formulas of the U

        //define all the AbstractTeachAction
        exit = new Exit(this);
        start = new StartSimulation(this);
        pause = new PauseSimulation(this);
        reload = new RestartSimulation(this);
        nextStep = new NextStepSimulation(this);
        openHelp = new Help(this);
        about = new About(this);
        increaseVelocity = new IncreaseVelocity(this);
        decreaseVelocity = new DecreaseVelocity(this);

        start.setEnabled(true); 
        nextStep.setEnabled(true);
        pause.setEnabled(false);
        reload.setEnabled(false);   
        decreaseVelocity.setEnabled(false); 
        increaseVelocity.setEnabled(false);

        engineJMCH = new MersenneTwister(23000);
        engineJSIM = new MersenneTwister(23000);

        serviceDJMCH = new DistributionJMCH();
        serviceDJMCH.convertDistribution(new Exponential());
        arrivalDJMCH = new DistributionJMCH();
        arrivalDJMCH.convertDistribution(new Exponential());
        serviceDJSIM = new DistributionJMCH();
        serviceDJSIM.convertDistribution(new Exponential());
        arrivalDJSIM = new DistributionJMCH(); 
        arrivalDJSIM.convertDistribution(new Exponential());   
    }

    public AnimationPanel(MainWizard main, Simulation sim){
        this(main);
        this.simulation = sim;  
        U = simulation.getType() == SimulationType.ROUTING && simulation.getName() == Constants.PROBABILISTIC ? 0.5 : 1.0; 
        initGUI();  
    }

    private void initGUI(){
        Box introductionBox = Box.createHorizontalBox();
        JLabel introductionLabel = new JLabel(Constants.INTRODUCTION_SIMULATION);
        introductionBox.add(introductionLabel);

        this.setLayout(new BorderLayout());
        mainPanel = new JPanel();

        String title = ""; //for NON_PREEMPTIVE the algorithm is not already chosen, in all other cases yes
        if(simulation.getType() != SimulationType.NON_PREEMPTIVE){
            title = " - " + simulation.getName();
        }
        else{
            title = " Scheduling";
        }
        mainPanel.setBorder(new TitledBorder(new EtchedBorder(), simulation.getType().toString() + title));
        
        mainPanel.setLayout(new BorderLayout());

        //divide the main panels in three columns
        leftPanel = new JPanel();
        leftPanel.setMaximumSize(new Dimension(275, mainPanel.getHeight()));
        leftPanel.setPreferredSize(new Dimension(275, mainPanel.getHeight()));
        mainPanel.add(leftPanel, BorderLayout.WEST);
       
        JPanel rightPanel = new JPanel();
        mainPanel.add(rightPanel, BorderLayout.CENTER);

        JPanel paddingPanel = new JPanel(); //this one is used only to create some padding on the left part
        paddingPanel.setMaximumSize(new Dimension(10, mainPanel.getHeight()));
        paddingPanel.setPreferredSize(new Dimension(10, mainPanel.getHeight()));
        mainPanel.add(paddingPanel, BorderLayout.EAST);
        

        //------------------LEFT PART
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10,5,10,5));
        leftPanel.setLayout(new BorderLayout());
        leftPanel.add(Box.createVerticalStrut(5), BorderLayout.NORTH);

        //description of the policy
        JPanel descrPanel = new JPanel(new BorderLayout());
        //descrPanel.setPreferredSize(new Dimension(leftPanel.getWidth(), 120));
        descrLabel = new JLabel();
        descrLabel.setText("<html><body><p style='text-align:justify;'><font size=\"3\">"+simulation.getDescription()+"</p></body></html>");
        descrPanel.add(descrLabel, BorderLayout.NORTH);
        descrPanel.add(Box.createVerticalStrut(10), BorderLayout.CENTER);
        leftPanel.add(descrPanel, BorderLayout.NORTH);

        //paramters panel
        createParameters(leftPanel);
        JScrollPane scrollPane = new JScrollPane(parametersPanel);
        scrollPane.setBorder(new TitledBorder(new EtchedBorder(), "Parameters"));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        parametersPanel.setPreferredSize(new Dimension(leftPanel.getWidth(), parametersPanel.getPreferredSize().height));
        leftPanel.add(scrollPane, BorderLayout.CENTER);

        //------------------RIGHT PART
        //rightPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0)); //handle padding correctly, since it seems to move all the objects of the animation in one direction
        rightPanel.setLayout(new BorderLayout());

        animationPanel = new JPanel(new BorderLayout());
        help.addHelp(animationPanel, Constants.HELP_ANIMATION);
        //based on the type of Policy passed in the constructor, create a new Animation
        if(simulation.getType() == SimulationType.ROUTING){
            animation = new MultipleQueueNetAnimation(this, animationPanel, simulation); 
            nQueues = 3;
        }
        else{
            animation = new SingleQueueNetAnimation(this, animationPanel, simulation);
            nQueues = 1;
        }
        animationPanel.add(animation, BorderLayout.CENTER);
        animationPanel.setBackground(Color.WHITE);

        rightPanel.add(Box.createVerticalStrut(10), BorderLayout.NORTH);
        rightPanel.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
        rightPanel.add(animationPanel, BorderLayout.CENTER);
        rightPanel.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);

        Box mainBox = Box.createVerticalBox();
        mainBox.add(Box.createVerticalStrut(20));
		mainBox.add(introductionBox);
		mainBox.add(Box.createVerticalStrut(10));
        mainBox.add(mainPanel);
        mainBox.add(Box.createVerticalStrut(1));

        Box totalBox = Box.createHorizontalBox(); //this box is for adding also the horizontal padding
		totalBox.add(Box.createHorizontalStrut(20));
		totalBox.add(mainBox);
		totalBox.add(Box.createHorizontalStrut(20));
        this.add(totalBox, BorderLayout.CENTER);

        this.add(totalBox, BorderLayout.CENTER);
        createMenu();
        createToolbar();
    }

    /**
     * Update the JPanel of the paramters in the left panel of this window
     */
    private void createParameters(JPanel container){
        parametersPanel = new JPanel();
        parametersPanel.setLayout(new BoxLayout(parametersPanel, BoxLayout.Y_AXIS));
        int heightPanels = 25;

        EmptyBorder paddingBorder = new EmptyBorder(0, 10, 0, 10); //padding right and left for all the panels inside the JPanel (top and bottom = 0 otherwise it does not show other panels)
        
        //algorithm Panel (this one is displayed only for Scheduling Policies)
        if(simulation.getType() == SimulationType.NON_PREEMPTIVE || simulation.getType() == SimulationType.PREEMPTIVE){
            JPanel algorithmPanel = createPanel(paddingBorder, false, spaceBetweenPanels, Constants.TOOLTIPS_PARAMETERS_PANEL[0], Constants.HELP_PARAMETERS_PANELS[0], heightPanels);
            algorithmPanel.setLayout(new GridLayout(1,2));
            algorithmPanel.add(new JLabel("Policy :"));

            if(simulation.getType() == SimulationType.NON_PREEMPTIVE){
                String[] options = new String[algorithms.size()];
                for (int i = 0; i < options.length; i++) {
                    options[i] = algorithms.get(i);
                }
                algorithmJComboBox = new JComboBox<String>(options);
                algorithmJComboBox.addActionListener(new ActionListener() { //to change the description & titles based on the type of policy selected
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        simulation = SimulationFactory.createSimulation(simulation.getType(), String.valueOf(algorithmJComboBox.getSelectedItem()));
                        mainPanel.setBorder(new TitledBorder(new EtchedBorder(), simulation.getType().toString() + " Scheduling - " + simulation.getName()));
                        descrLabel.setText("<html><body><p style='text-align:justify;'><font size=\"3\">"+simulation.getDescription()+"</p></body></html>");
                    }              
                });
                editableComponents.add(algorithmJComboBox);
                algorithmJComboBox.setSelectedItem(simulation.getName()); //set as selected policy the one chosen in the MainPanel when the button was pressed
                algorithmPanel.add(algorithmJComboBox);
            }
            
            parametersPanel.add(algorithmPanel);
        }

        //N servers panel (this one is displayed only for Scheduling Policies or Srocessor Sharing)
        if(simulation.getType() == SimulationType.NON_PREEMPTIVE || simulation.getType() == SimulationType.PREEMPTIVE || simulation.getType() == SimulationType.PROCESSOR_SHARING){
            JPanel nserversPanel = createPanel(paddingBorder, true, spaceBetweenPanels, Constants.TOOLTIPS_PARAMETERS_PANEL[1], Constants.HELP_PARAMETERS_PANELS[1], heightPanels);
            nserversPanel.setLayout(new GridLayout(1,2));
            nserversPanel.add(new JLabel("N.servers:"));
            SpinnerNumberModel model = new SpinnerNumberModel(1,1,2,1);
            serversSpinner = new JSpinner(model);
            serversSpinner.addChangeListener((ChangeEvent e) -> {
                updateFields();
            });
            editableComponents.add(serversSpinner);
            nserversPanel.add(serversSpinner);
        }

        //probability panel (displayed only for probabilistic routing)
        if(simulation.getType() == SimulationType.ROUTING && simulation.getName() == Constants.PROBABILISTIC){
            JPanel p1Panel = createPanel(paddingBorder, true, spaceBetweenPanels, Constants.TOOLTIPS_PARAMETERS_PANEL[2], Constants.HELP_PARAMETERS_PANELS[2], heightPanels);           
            p1Panel.setLayout(new GridLayout(1,2));
            p1Panel.add(new JLabel("Probability P1:"));      
            SpinnerNumberModel model1 = new SpinnerNumberModel(0.5,0,1,0.01);
            prob1 = new JSpinner(model1);
            p1Panel.add(prob1);

            JPanel p2Panel = createPanel(paddingBorder, true, spaceBetweenPanels, Constants.TOOLTIPS_PARAMETERS_PANEL[2], Constants.HELP_PARAMETERS_PANELS[2], heightPanels);  
            p2Panel.setLayout(new GridLayout(1,2));
            p2Panel.add(new JLabel("Probability P2:"));
            SpinnerNumberModel model2 = new SpinnerNumberModel(0.5,0,1,0.01);
            prob2 = new JSpinner(model2);
            p2Panel.add(prob2);

            JPanel p3Panel = createPanel(paddingBorder, true, spaceBetweenPanels, Constants.TOOLTIPS_PARAMETERS_PANEL[2], Constants.HELP_PARAMETERS_PANELS[2], heightPanels);  
            p3Panel.setLayout(new GridLayout(1,2));
            p3Panel.add(new JLabel("Probability P3:"));
            prob3 = new JSpinner();
            prob3.setEnabled(false);
            prob3.setValue(Integer.valueOf(0));
            p3Panel.add(prob3);

            editableComponents.add(prob1);
            editableComponents.add(prob2);

            prob1.addChangeListener((ChangeEvent e) -> {
                updateFields();
            });
            prob2.addChangeListener((ChangeEvent e) -> {
                updateFields();
            });
        }

        //Inter arrival time panel
        JPanel interAPanel = createPanel(paddingBorder, true, spaceBetweenPanels, Constants.TOOLTIPS_PARAMETERS_PANEL[3], Constants.HELP_PARAMETERS_PANELS[3], heightPanels);
        interAPanel.setLayout(new GridLayout(1,2));
        interAPanel.add(new JLabel("Inter Arrival Time:"));
        interADistrLabel = new JLabel(arrivalDJMCH.getDistribution().toString(), SwingConstants.CENTER);
        interAPanel.add(interADistrLabel);

        //Inter arrival distribution panel
        JPanel interADistrPanel = createPanel(paddingBorder, true, spaceBetweenPanels, Constants.TOOLTIPS_PARAMETERS_PANEL[3], Constants.HELP_PARAMETERS_PANELS[3], heightPanels);
        interADistrPanel.setLayout(new GridLayout(1,3));
        interAEdit = new JButton(editArrivalDistrAction);
        editableComponents.add(interAEdit);
        interADistrPanel.add(new JLabel()); 
        interADistrPanel.add(interAEdit); 
        interADistrPanel.add(new JLabel()); 

        //Service Time panel
        JPanel serviceTPanel = createPanel(paddingBorder, true, spaceBetweenPanels, Constants.TOOLTIPS_PARAMETERS_PANEL[4], Constants.HELP_PARAMETERS_PANELS[4], heightPanels);
        serviceTPanel.setLayout(new GridLayout(1,2));
        serviceTPanel.add(new JLabel("Service Time:"));
        serviceDistrLabel = new JLabel(serviceDJMCH.getDistribution().toString(), SwingConstants.CENTER);
        serviceTPanel.add(serviceDistrLabel);

        //Service Time distribution panel
        JPanel serviceTDistrPanel = createPanel(paddingBorder, true, spaceBetweenPanels, Constants.TOOLTIPS_PARAMETERS_PANEL[4], Constants.HELP_PARAMETERS_PANELS[4], heightPanels);
        serviceTDistrPanel.setLayout(new GridLayout(1,3));
        serviceEdit = new JButton(editServiceDistrAction);
        editableComponents.add(serviceEdit);
        serviceTDistrPanel.add(new JLabel()); 
        serviceTDistrPanel.add(serviceEdit);
        serviceTDistrPanel.add(new JLabel()); 

        //Traffic Intensity Panel
        JPanel trafficIntensityPanel= createPanel(paddingBorder, true, spaceBetweenPanels+5, Constants.TOOLTIPS_PARAMETERS_PANEL[5], Constants.HELP_PARAMETERS_PANELS[5], heightPanels);
        trafficIntensityPanel.setLayout(new GridLayout(1,2));
        //lambda = lambdaS.getValue()*multiplierSlider; 
        nQueues = simulation.getType() == SimulationType.ROUTING ? 3 : 1;
        trafficIntensityLabel = new JLabel();
        setUtilizationLabel(sliderTrafficProb, sliderTraffic);  
        trafficIntensityPanel.add(trafficIntensityLabel);
        
        //Simulation Duration
        JPanel simulationDuration = createPanel(paddingBorder, true, spaceBetweenPanels, Constants.TOOLTIPS_PARAMETERS_PANEL[6], Constants.HELP_PARAMETERS_PANELS[6], heightPanels);
        simulationDuration.setLayout(new GridLayout(1,2));
        simulationDuration.add(new JLabel("Max n. of jobs:"));
        //maxSamples = new JSpinner(new SpinnerNumberModel(1000000, 1, 10000000, 1));   
        maxSamples = new JSpinner(new SpinnerNumberModel(20, 1, 10000000, 1)); 
        editableComponents.add(maxSamples);
        simulationDuration.add(maxSamples);

        //Seed Panel
        JPanel seedPanel = createPanel(paddingBorder, true, spaceBetweenPanels, Constants.TOOLTIPS_PARAMETERS_PANEL[7], Constants.HELP_PARAMETERS_PANELS[7], heightPanels);
        seedPanel.setLayout(new GridLayout(1,2));
        seedPanel.add(new JLabel("Seed:"));
        seedSpinner = new JSpinner(new SpinnerNumberModel(23000, 1, Long.MAX_VALUE, 1));   
        editableComponents.add(seedSpinner);
        seedPanel.add(seedSpinner);

        //Simulation Type Panel
        /*JPanel simulationTypePanel = createPanel(paddingBorder, true, spaceBetweenPanels, Constants.TOOLTIPS_PARAMETERS_PANEL[8], Constants.HELP_PARAMETERS_PANELS[8], heightPanels);
        simulationTypePanel.setLayout(new GridLayout(1,2));
        simulationTypePanel.add(new JLabel("Graphical Sim.:"));
        simulationTypeCheckBox = new JCheckBox();
        simulationTypeCheckBox.setSelected(true);
        simulationTypePanel.add(simulationTypeCheckBox);
        editableComponents.add(simulationTypeCheckBox);*/
    }

    /**
     * Create a new JPanel inside the ParamtersPanel
     * @param paddingBorder border left and right for all the panels
     * @param padding boolean if it is needed to add a padding over the panel (the first panel does not need the padding)
     * @param space how much padding with the upper panel
     * @param toolTipText the string to display as a tooltip
     * @param helpText the string to display at the bottom of the Wizard
     * @param setMaxSpace max height for each panel
     * @return a new Panel
     */
    private JPanel createPanel(EmptyBorder paddingBorder, boolean padding, int space, String toolTipText, String helpText, int setMaxSpace) {
        if(padding){
            parametersPanel.add(Box.createVerticalStrut(space));
        }
        JPanel p = new JPanel();  
        p.setBorder(paddingBorder);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, setMaxSpace));
        p.setToolTipText(toolTipText);
        parametersPanel.add(p);
        help.addHelp(p, helpText);
        return p;
	}

    /**
     * Sets the editable Components
     * @param editable true or false if components are editable or not
     */
    private void setEditableComponents(boolean editable){
        for(JComponent comp: editableComponents){
            comp.setEnabled(editable);
        }
    }

    private void setUtilizationLabel(String prob, String noProb){
        if(simulation.getType() == SimulationType.ROUTING && simulation.getName() == Constants.PROBABILISTIC){       
            trafficIntensityLabel.setText(String.format(prob, U));
        }
        else{
            trafficIntensityLabel.setText(String.format(noProb, U));
        }
    }

    /**
     * Update the utilization per server label
     */
    private void updateFields(){
        
        boolean canStart = true;
        if(simulation.getType() == SimulationType.ROUTING && simulation.getName() == Constants.PROBABILISTIC){  
            U = (1/(double) arrivalDJMCH.getDistribution().getMean()) * serviceDJMCH.getDistribution().getMean() * getMaxProbability();
            double value1 = (double) prob1.getValue();
            double value2 = (double) prob2.getValue();
            prob3.setValue(Double.valueOf(1 - value1 - value2));
            canStart = (value1 + value2) <= 1;
        }
        else{
            U = (1/(double)arrivalDJMCH.getDistribution().getMean()) * serviceDJMCH.getDistribution().getMean() / ((int)(serversSpinner.getValue()) * nQueues);
        } 

        if (U > 0 && U <= 1) { 
            setUtilizationLabel(sliderTrafficProb, sliderTraffic);
            trafficIntensityLabel.setForeground(Color.BLACK);
            start.setEnabled(canStart);
            nextStep.setEnabled(canStart);
        } else {
            setUtilizationLabel(sliderTrafficSaturationProb, sliderTrafficSaturation);
            trafficIntensityLabel.setForeground(Color.RED);
            start.setEnabled(false);
            nextStep.setEnabled(false);
        }   
    }

    private double getMaxProbability(){
        double p1 = (double) prob1.getValue();
        double p2 = (double) prob2.getValue();
        double p3 = 1 - p2 - p1;
        return Math.max(p3, Math.max(p2, p1));
    }

    /**
	 * Update the menuBar for the Scheduling Window
	 */
	protected void createMenu() {
		JMTMenuBar menu = new JMTMenuBar(JMTImageLoader.getImageLoader());

        //File window
        MenuAction action = new MenuAction("File", new AbstractMCHAction[] { null, exit});
		menu.addMenu(action);

        //Solve window
		action = new MenuAction("Solve", new AbstractMCHAction[] {start, pause, reload, nextStep, null, decreaseVelocity, increaseVelocity});
		menu.addMenu(action);

        //Help window
        action = new MenuAction("Help", new AbstractMCHAction[] {openHelp, null, about});
		menu.addMenu(action);

		parent.setMenuBar(menu);
	}

    /**
	 * Update the toolbar for the Scheduling Window
	 */
    protected void createToolbar() {
        JMTToolBar toolbar = new JMTToolBar(JMTImageLoader.getImageLoader());	

        //first add all the icons with their actions
        AbstractMCHAction[] actions = new AbstractMCHAction[] {start, pause, reload, nextStep, null, decreaseVelocity, increaseVelocity, openHelp}; // Builds an array with all actions	
        toolbar.populateToolbar(actions);
        ArrayList<AbstractButton> buttons = new ArrayList<AbstractButton>(); //create a list of AbstractButtons for the helpLabel
		buttons.addAll(toolbar.populateToolbar(actions));
        toolbar.setFloatable(false);

        //add help for each Action/JComboBox with helpLabel
		for (int i = 0; i < buttons.size(); i++) {
			AbstractButton button = buttons.get(i);
			help.addHelp(button, Constants.HELP_BUTTONS_ANIMATIONS[i]);
		}
		  
		parent.setToolBar(toolbar);
	}

    @Override
    public boolean canGoBack() {
		return true;
	}

    /**
     * Update this panel
     */
    private void updateAnimationPanel(){
        if(simulation.getType() == SimulationType.NON_PREEMPTIVE || simulation.getType() == SimulationType.PREEMPTIVE || simulation.getType() == SimulationType.PROCESSOR_SHARING){            
            animation.updateSingle(simulation, (int)serversSpinner.getValue(), serviceDJMCH, arrivalDJMCH, (int) maxSamples.getValue(), true);//simulationTypeCheckBox.isSelected());
        }
        else{ //in case of routing only the animation must be updated
            if(simulation.getType() == SimulationType.ROUTING && simulation.getName() == Constants.PROBABILISTIC){
                animation.updateMultiple(simulation, new double[]{(double) prob1.getValue(), (double) prob2.getValue()}, serviceDJMCH, arrivalDJMCH, (int) maxSamples.getValue(), true);//simulationTypeCheckBox.isSelected());
            }
            else{
                animation.updateMultiple(simulation, serviceDJMCH, arrivalDJMCH, (int) maxSamples.getValue(), true);//simulationTypeCheckBox.isSelected());
            }
        }

        parent.setIconLoading();     
        getSimulationResults();
    }

    private void resetEngines(){
        Integer seedValue = ((Number) seedSpinner.getValue()).intValue();
        engineJMCH = new MersenneTwister(seedValue);
        engineJSIM = new MersenneTwister(seedValue);

        serviceDJMCH.setEngine(engineJMCH);
        arrivalDJMCH.setEngine(engineJMCH);

        serviceDJSIM.setEngine(engineJSIM);
        arrivalDJSIM.setEngine(engineJSIM);
    }

    

    /**
     * Start the simulation with the JSIM engine.
     * Moreover after setting up the solver, it creates a new entry for the result table for the ANIM type of result 
     */
    private void getSimulationResults(){
        //-- SOLVER SETUP
        int servers = 1;
        double[] prob = null;
        if(simulation.getType() != SimulationType.ROUTING){
            servers = (int) serversSpinner.getValue();
        }
        if(simulation.getType() == SimulationType.ROUTING && simulation.getName() == Constants.PROBABILISTIC){
            prob = new double[3];
            prob[0] = (double) prob1.getValue();
            prob[1] = (double) prob2.getValue();
            prob[2] = 1.0 - prob[0] - prob[1];
        }

        Long seedValue = ((Number) seedSpinner.getValue()).longValue();
        solver = new Solver(simulation, arrivalDJSIM.getDistribution(), serviceDJSIM.getDistribution(), servers, prob, 100000, seedValue);

        //-- ENTRY FOR ANIM RESULT
        parent.routeResults(
            Constants.RESULT_ANIM,
            0,
            solver.getStrategy(), 
            solver.getInterArrivalDistribution(), 
            solver.getInterArrivalTimeMean(),
            solver.getServiceDistribution(), 
            solver.getNumberServers(),
            solver.getServiceTimeMean(), 
            solver.getNumberQueues(),
            0.0,
            0.0, 
            0.0, 
            0.0); //create a new entry with 0.0 values. They will be updated real time during the simulation

        //-- START JSIM SIMULATION
        File temp = null;
        try {
            temp = File.createTempFile("~JModelSimulation", ".xml");
            temp.deleteOnExit();       
            XMLWriter.writeXML(temp, solver.getModel());
            String logCSVDelimiter = solver.getModel().getLoggingGlbParameter("delim");
            String logDecimalSeparator = solver.getModel().getLoggingGlbParameter("decimalSeparator");
            solver.getModel().setSimulationResults(new ResultsModel(solver.getModel().getPollingInterval().doubleValue(), logCSVDelimiter, logDecimalSeparator));
            dispatcher = SimulationRunnerFactory.Registry.create(this, solver.getModel());
            dispatcher.setDaemon(true);
            dispatcher.startSimulation(temp);
        } catch (IOException e) {
            handleException(e);
        }
    }

    @Override
    public String getName() {
        return PANEL_NAME;
    }

    public SimulationType getSimulationType(){
        return simulation.getType();
    }

    //----------------- toolbar buttons actions
    @Override
    public void startAnimation() {
        setUpAnimation();

        animation.setVelocityFactor(velocity[indexVelocity]);
        animation.start();

        enableButtons(false, true, false, false, indexVelocity > 0, indexVelocity < (velocity.length - 1));
    }

    public void setUpAnimation(){
        if(!animation.isPaused()){
            resetEngines(); //update the seed of the engines
            updateAnimationPanel(); 
            setEditableComponents(false);
        }
    }

    @Override
    public void pauseAnimation() {
        animation.pause();

        //when reaching the end of the simulation, we do not allow anymore the nextStep (the problem is that the sink is not enabled for the next step, so if you finish the animation with a next step the sys ends in a loop)
        enableButtons(true, false, true, animation.getSource().getMaxJobs() > 0, false, false);
    }

    @Override
    public void reloadAnimation() {
        setEditableComponents(true);
        animation.reload();

        enableButtons(true, false, false, animation.getSource().getMaxJobs() > 0,false, false);
    }

    @Override
    public void nextStepAnimation(){
        setUpAnimation();

        animation.next();
        //disable all the buttons, they will be enabled by the AnimationClass when the step simulation is completed
        enableButtons(false, false, false, false, false, false);
    }

    /** Set all the buttons correclty when the Step simulation is complete (called by AnimationClass) */
    public void resetNextStepAnimation(){
        enableButtons(true, false, true, animation.getSource().getMaxJobs() > 0, false, false);
    }

    @Override
    public void increaseVelocity(){
        indexVelocity++;
        decreaseVelocity.setEnabled(true);
        if(indexVelocity == velocity.length - 1){
            increaseVelocity.setEnabled(false);     
        }

        animation.setVelocityFactor(velocity[indexVelocity]);
    }

    @Override
    public void decreaseVelocity(){
        indexVelocity--;
        increaseVelocity.setEnabled(true);
        if(indexVelocity == 0){
            decreaseVelocity.setEnabled(false);     
        }

        animation.setVelocityFactor(velocity[indexVelocity]);
    }

    @Override
    public void stopAnimation() {
        enableButtons(false, false, true, false, false, false);
    }

    @Override
    public void setLastPanel(){
        parent.setLastPanel(Constants.PANEL_ANIMATION);
    }

    @Override
    public void lostFocus() { 
        setLastPanel();
    }

    /**
     * Enable or disable the buttons 
     * @param str start button
     * @param ps pause button
     * @param rl reload button
     * @param nxtSt nextStep button
     * @param decVel decreseVelocity button
     * @param incVel increaseVelocity button
     */
    public void enableButtons(boolean str, boolean ps, boolean rl, boolean nxtSt, boolean decVel, boolean incVel){
        start.setEnabled(str);
        pause.setEnabled(ps);
        reload.setEnabled(rl);
        nextStep.setEnabled(nxtSt);
        decreaseVelocity.setEnabled(decVel); 
        increaseVelocity.setEnabled(incVel);
    }

    //-----------------------------------------------------------------------
    //-------------------- all GUI Interface methods ------------------------
    //-----------------------------------------------------------------------
    @Override
    public void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void handleException(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
		showErrorMessage(sw.toString());
    }

    @Override
    public void changeSimActionsState(boolean start, boolean pause, boolean stop) {
        
    }

    @Override
    public void setResultsWindow(JFrame rsw) {

    }

    @Override
    public void showResultsWindow() {
    }

    @Override
    public boolean isAnimationDisplayable() {
        return true;
    }

    @Override
    public void showRelatedPanel(int problemType, int problemSubType, Object relatedStation, Object relatedCLass) {
    }

    @Override
    public void setAnimationHolder(Thread thread) {

    }

    @Override
    public String getFileName() {
        return null;
    }

    /**
     * Called by dispatcher when the simulation with JSIM Engine is finished.
     *  I opted for this solution since the progressionListener is not very well synchronized with the available data, it happened most of the times that the progression was 100% but there was no available data
     */
    @Override
    public void simulationFinished() {   
        MeasureDefinition results = solver.getModel().getSimulationResults();
        parent.setIconFinish();
        dispatcher.stopSimulation();


        try{
            parent.routeResults(
                Constants.RESULT_JSIM,
                solver.getMaxSamples(),
                solver.getStrategy(), 
                solver.getInterArrivalDistribution(), 
                solver.getInterArrivalTimeMean(),
                solver.getServiceDistribution(), 
                solver.getNumberServers(),
                solver.getServiceTimeMean(), 
                solver.getNumberQueues(),
                getLastMeasure(results, 1),
                getLastMeasure(results, 2), 
                getLastMeasure(results, 3), 
                getLastMeasure(results, 4));  
        }
        catch(Exception e){
            //TODO: capire come mai ogni tanto le ultime misure sono null: 
           //showErrorMessage("Something went wrong in the JSIM engine");
        }

                
    }

    public double getLastMeasure(MeasureDefinition md, int index){
        return md.getValues(index).lastElement().getMeanValue();
    }

    public void updateAnimResults(int genereatedJobs, double[] measures){
        parent.updateAnimResults(genereatedJobs, measures);
    }

    public void terminateThreads(){
        animation.stop();
        if(dispatcher != null && dispatcher.isAlive()){
            //dispatcher.interrupt();
        }
    }

    @Override
    public void exit(){
        //terminate all the threads
        terminateThreads();
        super.exit();
    }
}