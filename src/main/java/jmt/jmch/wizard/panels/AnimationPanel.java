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
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jmt.framework.gui.components.JMTMenuBar;
import jmt.framework.gui.components.JMTToolBar;
import jmt.framework.gui.help.HoverHelp;
import jmt.framework.gui.listeners.MenuAction;
import jmt.gui.common.JMTImageLoader;
import jmt.gui.common.controller.DispatcherThread;
import jmt.gui.common.definitions.GuiInterface;
import jmt.gui.common.definitions.MeasureDefinition;
import jmt.gui.common.definitions.ResultsModel;
import jmt.gui.common.xml.XMLWriter;
import jmt.jmarkov.utils.Formatter;
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
import jmt.jmch.distributions.AnimDistribution;
import jmt.jmch.distributions.DistributionFactory;


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
    private JComboBox<String> interAComboBox;
    private JComboBox<String> serviceComboBox;
    private JSpinner serversSpinner;

    private List<JComponent> editableComponents;

    private JLabel avgArrivalRateLabel;
    private JSlider lambdaS;
    private JLabel avgServiceTimeLabel;
    private JSlider sS;
    private JLabel trafficIntensityLabel;

    private JSpinner prob1 = null; //those two spinners are instanciated only if Probabilisitic routing is selected
    private JSpinner prob2 = null;
    private JSpinner prob3 = null;
    private JPanel animationPanel;
    private HoverHelp help;
    
    //------------ variables for parameters JPanel ---------------
    private JPanel parametersPanel;
    private final int spaceBetweenPanels = 3;
    private final String[] distributions = AnimDistribution.getDistributions(); 
    private JSpinner maxSamples;

    //--- variables for slider 
    private final double multiplierSlider = 0.01; 
    private final int startValueSlider = 50;
    private final String sliderArrival = "Arrival Rate (\u03BB): %.2f cust./s";
    private final String sliderService = "Avg. Service Time (S): %.2f s";
    private final String sliderTraffic = "<html>Avg. Utilization <sub>per server</sub>: %.2f </html>";
    private final String sliderTrafficProb = "<html>Max. Utilization: %.2f </html>";
    private final String sliderTrafficSaturation = "<html>Avg. Utilization<sub>server</sub>: Saturation (%.2f) </html>";
    private final String sliderTrafficSaturationProb = "<html>Max. Utilization : Saturation (%.2f) </html>";
    private final int LAMBDA_I = 50;
    private final int S_I = 95;
    private int nQueues = 1;
    private final DecimalFormat df = new DecimalFormat("#.##");
    private boolean lambdaSChange = true;
	private boolean sSChange = true;
    private double sMultiplier = 1; //service time slide bar multiplier
	private double lambdaMultiplier = 1; //lambda slide bar multiplier
	private int lambdaMultiplierChange = 0; //for the lambda slide bar
	private int sMultiplierChange = 1; //for the service slide bar
    private double lambda = LAMBDA_I * lambdaMultiplier;
    private double S = S_I * sMultiplier;

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
    private DispatcherThread dispatcher;

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
        openHelp = new Help(this,"JTCH");
        about = new About(this);
        increaseVelocity = new IncreaseVelocity(this);
        decreaseVelocity = new DecreaseVelocity(this);

        start.setEnabled(true); 
        nextStep.setEnabled(false);
        pause.setEnabled(false);
        reload.setEnabled(false);   
        decreaseVelocity.setEnabled(false); 
        increaseVelocity.setEnabled(false);
    }

    public AnimationPanel(MainWizard main, Simulation sim){
        this(main);
        this.simulation = sim;   
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
        interAComboBox = new JComboBox<String>(distributions);
        editableComponents.add(interAComboBox);
        interAPanel.add(interAComboBox);

        //Service Time panel
        JPanel serviceTPanel = createPanel(paddingBorder, true, spaceBetweenPanels, Constants.TOOLTIPS_PARAMETERS_PANEL[4], Constants.HELP_PARAMETERS_PANELS[4], heightPanels);
        serviceTPanel.setLayout(new GridLayout(1,2));
        serviceTPanel.add(new JLabel("Service Time:"));
        serviceComboBox = new JComboBox<String>(distributions);
        editableComponents.add(serviceComboBox);
        serviceTPanel.add(serviceComboBox);

        //Slider panels
        JPanel avgArrivalRatePanel = createPanel(paddingBorder, true, spaceBetweenPanels, Constants.TOOLTIPS_PARAMETERS_PANEL[5], Constants.HELP_PARAMETERS_PANELS[5], heightPanels*3);
        avgArrivalRatePanel.setLayout(new GridLayout(2,1));
        avgArrivalRateLabel = new JLabel(String.format(sliderArrival, startValueSlider * multiplierSlider));
        avgArrivalRatePanel.add(avgArrivalRateLabel);
        createLambdaSlider();
        editableComponents.add(lambdaS);
        avgArrivalRatePanel.add(lambdaS);

        JPanel avgServiceTimePanel= createPanel(paddingBorder, true, spaceBetweenPanels, Constants.TOOLTIPS_PARAMETERS_PANEL[6], Constants.HELP_PARAMETERS_PANELS[5], heightPanels*3);
        avgServiceTimePanel.setLayout(new GridLayout(2,1));
        avgServiceTimeLabel = new JLabel(String.format(sliderService, startValueSlider * multiplierSlider));
        avgServiceTimePanel.add(avgServiceTimeLabel);
        createSSlider();
        editableComponents.add(sS);
        avgServiceTimePanel.add(sS);

        JPanel trafficIntensityPanel= createPanel(paddingBorder, true, spaceBetweenPanels, Constants.TOOLTIPS_PARAMETERS_PANEL[7], Constants.HELP_PARAMETERS_PANELS[5], heightPanels);
        trafficIntensityPanel.setLayout(new GridLayout(1,1));
        lambda = lambdaS.getValue()*multiplierSlider;
        nQueues = simulation.getType() == SimulationType.ROUTING ? 3 : 1;
        trafficIntensityLabel = new JLabel();
        setUtilizationLabel(sliderTrafficProb, sliderTraffic);  
        trafficIntensityPanel.add(trafficIntensityLabel);
        
        //Simulation Duration
        JPanel simulationDuration = createPanel(paddingBorder, true, spaceBetweenPanels, Constants.TOOLTIPS_PARAMETERS_PANEL[8], Constants.HELP_PARAMETERS_PANELS[6], heightPanels);
        simulationDuration.setLayout(new GridLayout(1,2));
        simulationDuration.add(new JLabel("Max n. of samples:"));
        maxSamples = new JSpinner(new SpinnerNumberModel(1000000, 100000, 10000000, 50000));   
        editableComponents.add(maxSamples);
        simulationDuration.add(maxSamples);
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

    /* Method for setting the slider for the Lambda */
    private void createLambdaSlider(){
        lambdaS = new JSlider();
        
        lambdaMultiplier = 0.01;
        lambdaMultiplierChange = 0;
        lambdaS.setMaximum(100);
        lambdaS.setMinimum(0);
        lambdaS.setMajorTickSpacing(25);
        lambdaS.setMinorTickSpacing(1);
        lambdaS.setPaintLabels(true);
        lambdaS.setSnapToTicks(true);
        lambdaS.setValue(LAMBDA_I);
        lambda = LAMBDA_I * lambdaMultiplier;
        setLambdaSlider();
        lambdaS.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
                lambdaSStateChanged(evt);
                if (lambdaSChange) {
                    setLambdaMultiplier();
                }

            }
        });
        lambdaS.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {}

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                lambdaSChange = false;
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                setLambdaMultiplier();
				lambdaSChange = true;
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {}

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {}

        });
        lambdaS.repaint();
    }

    /* Method for setting the slider for the Lambda */
    private void createSSlider(){
        sS = new JSlider();

        sS.setMaximum(100);
        sS.setMinimum(0);
        sS.setMajorTickSpacing(25);
        sS.setMinorTickSpacing(1);
        sS.setPaintLabels(true);
        sMultiplier = 0.02;
        sMultiplierChange = 1;
        sS.setValue(S_I);
        S = S_I * sMultiplier;

        setSSlider();
        sS.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
                sSStateChanged(evt);
                if (sSChange) {
                    setSMultiplier();
                }
            }
        });
        sS.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {}

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                sSChange = false;
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                setSMultiplier();
				sSChange = true;
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {}

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {}
        });
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
            trafficIntensityLabel.setText(String.format(prob, S*lambda*getMaxProbability()));
        }
        else{
            trafficIntensityLabel.setText(String.format(sliderTraffic, S*lambda / ((int)(serversSpinner.getValue()) * nQueues)));
        }
    }

    //--------------------- Methods for updating dinamically the sliders (from MMQueues.java) ------------------------
    protected void lambdaSStateChanged(ChangeEvent evt) {
		if (lambdaS.getValue() == 0) {
			lambdaMultiplier = 0.01;
			lambdaMultiplierChange = 0;
			lambdaS.setValue(1);
		}
		lambda = lambdaMultiplier * lambdaS.getValue();
		avgArrivalRateLabel.setText(String.format(sliderArrival, lambda));
		setSSlider();
		updateFields();
	}

	protected void sSStateChanged(ChangeEvent evt) {
        if(sS.getValue() == 0){
            sMultiplier = 0.01;
            sMultiplierChange = 0;
            sS.setValue(1);
        }
        S = sMultiplier * sS.getValue();
        avgServiceTimeLabel.setText(String.format(sliderService, S));
		setSSlider();
		updateFields();
	}

    /* Update the utilization and all the labels related to U */
    private void updateFields(){
        double U;
        boolean canStart = true;
        if(simulation.getType() == SimulationType.ROUTING && simulation.getName() == Constants.PROBABILISTIC){  
            U = lambda * S * getMaxProbability();
            double value1 = (double) prob1.getValue();
            double value2 = (double) prob2.getValue();
            prob3.setValue(Double.valueOf(1 - value1 - value2));
            canStart = (value1 + value2) <= 1;
        }
        else{
            U = lambda * S / ((int)(serversSpinner.getValue()) * nQueues);
        } 

        if (U > 0 && U <= 1) { 
            setUtilizationLabel(sliderTrafficProb, sliderTraffic);
            trafficIntensityLabel.setForeground(Color.BLACK);
            start.setEnabled(canStart);
        } else {
            setUtilizationLabel(sliderTrafficSaturationProb, sliderTrafficSaturation);
            trafficIntensityLabel.setForeground(Color.RED);
            start.setEnabled(false);
        }   
    }

    public void setSSlider() {
		Dictionary<Integer, JLabel> d = sS.getLabelTable();

		for (int i = sS.getMinimum(); i <= sS.getMaximum(); i += sS.getMajorTickSpacing()) {
			d.put(new Integer(i), new JLabel("" + Formatter.formatNumber(i * sMultiplier, 2)));
		}
		sS.setLabelTable(d);
        avgServiceTimeLabel.setText(String.format(sliderService, sS.getValue() * sMultiplier));
        S = sS.getValue() * sMultiplier;
		sS.repaint();
	}

	public void setLambdaSlider() {
		Dictionary<Integer, JLabel> ld = lambdaS.getLabelTable();

		for (int i = lambdaS.getMinimum(); i <= lambdaS.getMaximum(); i += lambdaS.getMajorTickSpacing()) {
			ld.put(new Integer(i), new JLabel("" + Formatter.formatNumber(i * lambdaMultiplier, 2)));
		}

		lambdaS.setLabelTable(ld);
        avgArrivalRateLabel.setText(String.format(sliderArrival, lambdaS.getValue() * lambdaMultiplier));
        lambda = lambdaS.getValue() * lambdaMultiplier;
        lambdaS.repaint();
	}

    public void setLambdaMultiplier() {
		while (true) {
			if (lambdaS.getValue() > lambdaS.getMaximum() * 0.95) {
				if (lambdaMultiplierChange <= 4) {
					if (lambdaMultiplierChange % 2 == 0) {
						lambdaMultiplier *= 2;
						setLambdaSlider();
						lambdaS.setValue((lambdaS.getValue() + 1) / 2);
					} else {
						lambdaMultiplier *= 5;
						setLambdaSlider();
						lambdaS.setValue((lambdaS.getValue() + 1) / 5);
					}
					lambdaMultiplierChange++;
				} else {
					break;
				}
			} else if (lambdaS.getValue() < lambdaS.getMaximum() * 0.05) {
				if (lambdaMultiplierChange > 0) {
					if (lambdaMultiplierChange % 2 == 1) {
						lambdaMultiplier /= 2;
						setLambdaSlider();
						lambdaS.setValue(lambdaS.getValue() * 2);
					} else {
						lambdaMultiplier /= 5;
						setLambdaSlider();
						lambdaS.setValue(lambdaS.getValue() * 5);
					}
					lambdaMultiplierChange--;
				} else {
					break;
				}
			} else {
				break;
			}
		}
	}

	public void setSMultiplier() {
		while (true) {
			if (sS.getValue() > sS.getMaximum() * 0.95) {
				if (sMultiplierChange <= 4) {
					if (sMultiplierChange % 2 == 0) {
						sMultiplier *= 2;
						setSSlider();
						sS.setValue((sS.getValue() + 1) / 2);
					} else {
						sMultiplier *= 5;
						setSSlider();
						sS.setValue((sS.getValue() + 1) / 5);
					}
					sMultiplierChange++;
				} else {
					break;
				}
			} else if (sS.getValue() < sS.getMaximum() * 0.05) {
				if (sMultiplierChange > 0) {
					if (sMultiplierChange % 2 == 1) {
						sMultiplier /= 2;
						setSSlider();
						sS.setValue(sS.getValue() * 2);
					} else {
						sMultiplier /= 5;
						setSSlider();
						sS.setValue(sS.getValue() * 5);
					}
					sMultiplierChange--;
				} else {
					break;
				}
			} else {
				break;
			}
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
        animation.pause();
        reloadAnimation();

        AnimDistribution sd = DistributionFactory.createDistribution(String.valueOf(serviceComboBox.getSelectedItem()));
        sd.setMean(1/S);
        AnimDistribution ad = DistributionFactory.createDistribution(String.valueOf(interAComboBox.getSelectedItem()));
        ad.setMean(lambda);

        if(simulation.getType() == SimulationType.NON_PREEMPTIVE || simulation.getType() == SimulationType.PREEMPTIVE || simulation.getType() == SimulationType.PROCESSOR_SHARING){            
            animation.updateSingle(simulation, (int)serversSpinner.getValue(), sd, ad);
        }
        else{ //in case of routing only the animation must be updated
            if(simulation.getType() == SimulationType.ROUTING && simulation.getName() == Constants.PROBABILISTIC){
                animation.updateMultiple(simulation, new double[]{(double) prob1.getValue(), (double) prob2.getValue()}, sd, ad);
            }
            else{
                animation.updateMultiple(simulation, sd, ad);
            }
        }


        parent.setIconLoading();     
        getSimulationResults();
    }

    

    /** Called each time 'Create' is pressed. Start the simulation with the engine to get the results of the simulation in the Results Panel */
    private void getSimulationResults(){
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
        solver = new Solver(simulation, lambda, 1/S, interAComboBox.getSelectedIndex(), serviceComboBox.getSelectedIndex(), servers, prob, (Integer) maxSamples.getValue());

        File temp = null;
        try {
            temp = File.createTempFile("~JModelSimulation", ".xml");
            temp.deleteOnExit();       
            XMLWriter.writeXML(temp, solver.getModel());
            String logCSVDelimiter = solver.getModel().getLoggingGlbParameter("delim");
            String logDecimalSeparator = solver.getModel().getLoggingGlbParameter("decimalSeparator");
            solver.getModel().setSimulationResults(new ResultsModel(solver.getModel().getPollingInterval().doubleValue(), logCSVDelimiter, logDecimalSeparator));
            dispatcher = new DispatcherThread(this, solver.getModel());
            dispatcher.setDaemon(true);
            dispatcher.startSimulation(temp);
        } catch (IOException e) {
            handleException(e);
        }
    }

    public double getLastMeasure(MeasureDefinition md, int index){
        return md.getValues(index).lastElement().getMeanValue();
    }

    @Override
    public String getName() {
        return PANEL_NAME;
    }

    public SimulationType getSimulationType(){
        return simulation.getType();
    }


    @Override
    public void openHelp() {
        
    }

    //----------------- toolbar buttons actions
    @Override
    public void startAnimation() {
        animation.getSource().resetEngines(); //always reset the engine

        if(!nextStep.isEnabled()){
            updateAnimationPanel(); 
            setEditableComponents(false);
        }

        animation.setVelocityFactor(velocity[indexVelocity]);
        animation.start();
        start.setEnabled(false);
        pause.setEnabled(true);
        reload.setEnabled(false);   
        nextStep.setEnabled(false); 
        
        if(indexVelocity > 0){
            decreaseVelocity.setEnabled(true);
        }
        else{
            decreaseVelocity.setEnabled(false);
        }
        
        if(indexVelocity < (velocity.length - 1)){
            increaseVelocity.setEnabled(true);
        }
        else{
            increaseVelocity.setEnabled(false);
        }
    }

    @Override
    public void pauseAnimation() {
        animation.pause();
        start.setEnabled(true);
        pause.setEnabled(false);
        reload.setEnabled(true);
        nextStep.setEnabled(true);
        decreaseVelocity.setEnabled(false); 
        increaseVelocity.setEnabled(false);
    }

    @Override
    public void reloadAnimation() {
        setEditableComponents(true);
        animation.reload();
        start.setEnabled(true);
        pause.setEnabled(false);
        reload.setEnabled(false);
        nextStep.setEnabled(false);
        decreaseVelocity.setEnabled(false); 
        increaseVelocity.setEnabled(false);
    }

    @Override
    public void nextStepAnimation(){
        animation.next();
        //disable all the buttons, they will be enabled by the AnimationClass when the step simulation is completed
        disableAllButtons();
    }

    /** Set all the buttons correclty when the Step simulation is complete (called by AnimationClass) */
    public void resetNextStepAnimation(){
        start.setEnabled(true);
        pause.setEnabled(false);
        reload.setEnabled(true);
        nextStep.setEnabled(true);
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
        disableAllButtons();
    }

    @Override
    public void setLastPanel(){
        parent.setLastPanel(Constants.PANEL_ANIMATION);
    }

    @Override
    public void lostFocus() { 
        setLastPanel();
    }

    public void disableAllButtons(){
        start.setEnabled(false);
        pause.setEnabled(false);
        reload.setEnabled(false);
        nextStep.setEnabled(false);
        decreaseVelocity.setEnabled(false); 
        increaseVelocity.setEnabled(false);
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

    @Override
    //I opted for this solution since the progressionListener is not very well synchronized with the available data, it happened most of the times that the progression was 100% but there was no available data
    public void simulationFinished() { //called by dispatcher when the simulation is finished   
        MeasureDefinition results = solver.getModel().getSimulationResults();
        parent.setIconFinish();

        parent.routeResults(solver.getStrategy(), 
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
}