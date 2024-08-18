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
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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
import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.JMTImageLoader;
import jmt.gui.common.controller.DispatcherThread;
import jmt.gui.common.definitions.GuiInterface;
import jmt.gui.common.definitions.MeasureDefinition;
import jmt.gui.common.definitions.ResultsModel;
import jmt.gui.common.xml.XMLWriter;
import jmt.jmch.Constants;
import jmt.jmch.Solver;
import jmt.jmch.wizard.actionsWizard.About;
import jmt.jmch.wizard.actionsWizard.AbstractMCHAction;
import jmt.jmch.wizard.actionsWizard.Exit;
import jmt.jmch.wizard.actionsWizard.Help;
import jmt.jmch.wizard.actionsWizard.NextStepSimulation;
import jmt.jmch.wizard.actionsWizard.PauseSimulation;
import jmt.jmch.wizard.actionsWizard.ReloadSimulation;
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
public class AnimationPanel extends WizardPanel implements JMCHWizardPanel, GuiInterface{
    private static final String PANEL_NAME = "Simulation";

    //------------ components of the panel -----------------------
    private MainWizard parent;
    private JPanel mainPanel;
    private JLabel descrLabel;
    private JComboBox<String> algorithmJComboBox = null;
    private JComboBox<String> interAComboBox;
    private JComboBox<String> serviceComboBox;
    private JSpinner serversSpinner;

    private JLabel avgArrivalRateLabel;
    private JSlider avgArrivalRateSlider;
    private JLabel avgServiceTimeLabel;
    private JSlider avgServiceTimeSlider;
    private JLabel trafficIntensityLabel;

    private JSpinner prob1 = null; //those two spinners are instanciated only if Probabilisitic routing is selected
    private JSpinner prob2 = null;
    private JButton createButton;
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
    private final String sliderArrival = "Avg. Arrival Rate (\u03BB): %.2f cust./s";
    private final String sliderService = "Avg. Service Rate (\u03BC): %.2f s" + "\u207B" + "\u00B9";
    private final String sliderTraffic = "Traffic Intensity (\u03C1): ";
    private double lambda = 0.5;
    private double mhu = 0.5;
    private final DecimalFormat df = new DecimalFormat("#.##");

    //-------------all the Actions of this panel------------------
    private AbstractMCHAction exit;
    private AbstractMCHAction start;
    private AbstractMCHAction pause;
    private AbstractMCHAction reload;
    private AbstractMCHAction nextStep;
    private AbstractMCHAction openHelp;
    private AbstractMCHAction about;

    //--------------properties of the animation------------------
    private Simulation simulation;
    private AnimationClass animation;
    private List<String> algorithms; //array of all possible algorithms to select

    //------------- engine simulation --------------------------
    private Solver solver;
    private DispatcherThread dispatcher;


    //Action associated to the button Create
    protected AbstractAction CREATE = new AbstractAction("Create") {
		private static final long serialVersionUID = 1L;
		{
			putValue(Action.SHORT_DESCRIPTION, "Create a new policy with the parameters chosen");
		}

		public void actionPerformed(ActionEvent e) {
            try {
                updateAnimationPanel(); 
            } catch (Exception x) {
                handleException(x);
            }
			
		}
	};

    //this is the change listener associated to the two spinner for the probabilities in routing prob
    private ChangeListener changeListener = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
            if(simulation.getType() == SimulationType.ROUTING && simulation.getName() == Constants.PROBABILISTIC && prob1 != null && prob2 != null){
                double value1 = (double) prob1.getValue();
                double value2 = (double) prob2.getValue();
                if (value1 + value2 <= 1) { 
                    createButton.setEnabled(true);
                } else {
                    createButton.setEnabled(false);
                }
            }           
        }
    };

    private AnimationPanel(MainWizard main){
        this.parent = main;
        help = parent.getHoverHelp();

        algorithms = NonPreemptiveSimulation.getAlgorithms();

        //define all the AbstractTeachAction
        exit = new Exit(this);
        start = new StartSimulation(this);
        pause = new PauseSimulation(this);
        reload = new ReloadSimulation(this);
        nextStep = new NextStepSimulation(this);
        openHelp = new Help(this,"JTCH");
        about = new About(this);

        start.setEnabled(false); //cannot start a simulation without setting the parameters
        nextStep.setEnabled(false);
        pause.setEnabled(false);
        reload.setEnabled(false);
    }

    public AnimationPanel(MainWizard main, Simulation sim){
        this(main);
        this.simulation = sim;   
        initGUI();
    }

    public void initGUI(){
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
        JPanel leftPanel = new JPanel();
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
        descrLabel = new JLabel();
        if(simulation.getType() == SimulationType.ROUTING || simulation.getType() == SimulationType.PROCESSOR_SHARING){ 
            descrLabel.setText("<html><body><p style='text-align:justify;'><font size=\"3\">"+simulation.getDescription()+"</p></body></html>");
        }
        else{ //only in the case of preemptive or non preemptive no description at the beginning
            descrLabel.setText(Constants.NO_DESCRIPTION);
        }
        descrPanel.add(descrLabel, BorderLayout.CENTER);
        leftPanel.add(descrPanel, BorderLayout.NORTH);

        //paramters panel
        createParameters(leftPanel);;
        leftPanel.add(parametersPanel, BorderLayout.SOUTH);

        //------------------RIGHT PART
        //rightPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0)); //handle padding correctly, since it seems to move all the objects of the animation in one direction
        rightPanel.setLayout(new BorderLayout());

        animationPanel = new JPanel(new BorderLayout());
        //based on the type of Policy passed in the constructor, create a new Animation
        if(simulation.getType() == SimulationType.ROUTING){
            animation = new MultipleQueueNetAnimation(this, animationPanel, simulation);     
        }
        else{
            animation = new SingleQueueNetAnimation(this, animationPanel, simulation);
        }
        animationPanel.add(animation, BorderLayout.CENTER);
        animationPanel.setBackground(Color.WHITE);

        rightPanel.add(Box.createVerticalStrut(10), BorderLayout.NORTH);
        rightPanel.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
        rightPanel.add(animationPanel, BorderLayout.CENTER);
        rightPanel.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);

        Box mainBox = Box.createVerticalBox();
        mainBox.add(Box.createVerticalStrut(5));
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
        parametersPanel.setBorder(new TitledBorder(new EtchedBorder(), "Parameters"));
        parametersPanel.setLayout(new BoxLayout(parametersPanel, BoxLayout.Y_AXIS));

        EmptyBorder paddingBorder = new EmptyBorder(0, 10, 0, 10); //padding right and left for all the panels inside the JPanel (top and bottom = 0 otherwise it does not show other panels)
        
        //algorithm Panel (this one is displayed only for Scheduling Policies)
        if(simulation.getType() == SimulationType.NON_PREEMPTIVE || simulation.getType() == SimulationType.PREEMPTIVE){
            JPanel algorithmPanel = createPanel(paddingBorder, false, spaceBetweenPanels, Constants.HELP_PARAMETERS_PANELS[0]);
            algorithmPanel.setLayout(new GridLayout(1,2));
            algorithmPanel.add(new JLabel("Algorithm :"));

            if(simulation.getType() == SimulationType.NON_PREEMPTIVE){
                String[] options = new String[algorithms.size()];
                for (int i = 0; i < options.length; i++) {
                    options[i] = algorithms.get(i);
                }
                algorithmJComboBox = new JComboBox<String>(options);
                algorithmJComboBox.setSelectedItem(simulation.getName()); //set as selected policy the one chosen in the MainPanel when the button was pressed
                algorithmPanel.add(algorithmJComboBox);
            }
            
            parametersPanel.add(algorithmPanel);
        }

        //N servers panel (this one is displayed only for Scheduling Policies or Srocessor Sharing)
        if(simulation.getType() == SimulationType.NON_PREEMPTIVE || simulation.getType() == SimulationType.PREEMPTIVE || simulation.getType() == SimulationType.PROCESSOR_SHARING){
            JPanel nserversPanel = createPanel(paddingBorder, true, spaceBetweenPanels, Constants.HELP_PARAMETERS_PANELS[1]);
            nserversPanel.setLayout(new GridLayout(1,2));
            nserversPanel.add(new JLabel("N.servers:"));
            SpinnerNumberModel model = new SpinnerNumberModel(1,1,2,1);
            serversSpinner = new JSpinner(model);
            nserversPanel.add(serversSpinner);
        }

        //probability panel (displayed only for probabilistic routing)
        if(simulation.getType() == SimulationType.ROUTING && simulation.getName() == Constants.PROBABILISTIC){
            JPanel probabilitiesPanel = createPanel(paddingBorder, false, spaceBetweenPanels, Constants.HELP_PROBABILITIES);
            //probabilitiesPanel.setBorder(new TitledBorder(new EtchedBorder(), "Probabilities"));
            probabilitiesPanel.setLayout(new BoxLayout(probabilitiesPanel, BoxLayout.Y_AXIS));

            JPanel p1Panel = new JPanel();
            probabilitiesPanel.add(p1Panel);            
            p1Panel.setLayout(new GridLayout(1,2));
            p1Panel.add(new JLabel("P1:"));
            SpinnerNumberModel model1 = new SpinnerNumberModel(0.5,0,1,0.01);
            prob1 = new JSpinner(model1);
            p1Panel.add(prob1);
            probabilitiesPanel.add(Box.createVerticalStrut(spaceBetweenPanels));

            JPanel p2Panel = new JPanel();
            probabilitiesPanel.add(p2Panel);
            p2Panel.setLayout(new GridLayout(1,2));
            p2Panel.add(new JLabel("P2:"));
            SpinnerNumberModel model2 = new SpinnerNumberModel(0.5,0,1,0.01);
            prob2 = new JSpinner(model2);
            p2Panel.add(prob2);

            prob1.addChangeListener(changeListener);
            prob2.addChangeListener(changeListener);
        }

        //Inter arrival time panel
        JPanel interAPanel = createPanel(paddingBorder, true, spaceBetweenPanels, Constants.HELP_PARAMETERS_PANELS[2]);
        interAPanel.setLayout(new GridLayout(1,2));
        interAPanel.add(new JLabel("Inter Arrival Time:"));
        interAComboBox = new JComboBox<String>(distributions);
        interAPanel.add(interAComboBox);
        parametersPanel.add(interAPanel);

        //Service Time panel
        JPanel serviceTPanel = createPanel(paddingBorder, true, spaceBetweenPanels, Constants.HELP_PARAMETERS_PANELS[3]);
        serviceTPanel.setLayout(new GridLayout(1,2));
        serviceTPanel.add(new JLabel("Service Time:"));
        serviceComboBox = new JComboBox<String>(distributions);
        serviceTPanel.add(serviceComboBox);
        parametersPanel.add(serviceTPanel);

        //Slider panel
        JPanel trafficIntensityPanel = createPanel(paddingBorder, true, spaceBetweenPanels, Constants.HELP_PARAMETERS_PANELS[4]);
        trafficIntensityPanel.setLayout(new GridLayout(5,1));
        avgArrivalRateLabel = new JLabel(String.format(sliderArrival, startValueSlider * multiplierSlider));
        trafficIntensityPanel.add(avgArrivalRateLabel);
        avgArrivalRateSlider = createSlider();
        trafficIntensityPanel.add(avgArrivalRateSlider);
        avgServiceTimeLabel = new JLabel(String.format(sliderService, startValueSlider * multiplierSlider));
        trafficIntensityPanel.add(avgServiceTimeLabel);
        avgServiceTimeSlider = createSlider();
        trafficIntensityPanel.add(avgServiceTimeSlider);
        trafficIntensityLabel = new JLabel(sliderTraffic + df.format(1));
        trafficIntensityPanel.add(trafficIntensityLabel);
        //paramTrafficLabel = new JLabel(String.format(sliderFS, lambda, mhu));
        //trafficIntensityPanel.add(paramTrafficLabel);


        parametersPanel.add(trafficIntensityPanel);
        
        //Simulation Duration
        JPanel simulationDuration = createPanel(paddingBorder, true, spaceBetweenPanels, Constants.HELP_PARAMETERS_PANELS[5]);
        simulationDuration.setLayout(new GridLayout(1,2));
        simulationDuration.add(new JLabel("Max n. of samples:"));
        maxSamples = new JSpinner(new SpinnerNumberModel(10000000, 100000, Integer.MAX_VALUE, 50000));   
        simulationDuration.add(maxSamples);
        parametersPanel.add(simulationDuration);

        //create button
        paddingBorder = new EmptyBorder(0,80,0,80);
        JPanel createPanel = createPanel(paddingBorder, true, spaceBetweenPanels*2, Constants.HELP_PARAMETERS_PANELS[6]);
        createPanel.setLayout(new BorderLayout());
        createButton = new JButton(CREATE);
        help.addHelp(createButton, Constants.HELP_PARAMETERS_PANELS[4]);
        createPanel.add(createButton, BorderLayout.CENTER);
        parametersPanel.add(Box.createVerticalStrut(spaceBetweenPanels));
    }

    /**
     * Create a new JPanel inside the ParamtersPanel
     * @param paddingBorder border left and right for all the panels
     * @param padding boolean if it is needed to add a padding over the panel (the first panel does not need the padding)
     * @param space how much padding with the upper panel
     * @param helpText the string to be displayed on the bottom of the JDialog
     * @return a new Panel
     */
    private JPanel createPanel(EmptyBorder paddingBorder, boolean padding, int space, String helpText) {
        if(padding){
            parametersPanel.add(Box.createVerticalStrut(space));
        }
        JPanel p = new JPanel();  
        p.setBorder(paddingBorder);
        parametersPanel.add(p);
        help.addHelp(p, helpText);
        return p;
	}

    /* Method for setting the slider for the traffic intensity */
    private JSlider createSlider(){
        JSlider slider = new JSlider();
        slider.setMaximum(100);
        slider.setMinimum(0);
        slider.setMajorTickSpacing(25);
        slider.setMinorTickSpacing(1);
        slider.setSnapToTicks(true);
        slider.setValue(startValueSlider);

        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("0.0"));
        labelTable.put(25, new JLabel("0.25"));
        labelTable.put(50, new JLabel("0.5"));
        labelTable.put(75, new JLabel("0.75"));
        labelTable.put(100, new JLabel("1.0"));
        slider.setLabelTable(labelTable);
        slider.setPaintLabels(true);

        slider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
                sliderStateChanged(evt);
            }
        });

        return slider;
    }

    /**
     * Called each time the sliders change their value.
     * One method for all the sliders in the parameters panel
     * @param evt the trigger event
     */
    public void sliderStateChanged(ChangeEvent evt){
        JSlider source = (JSlider) evt.getSource();
        lambda = avgArrivalRateSlider.getValue() * multiplierSlider;
        mhu = avgServiceTimeSlider.getValue() * multiplierSlider;

        if(source == avgArrivalRateSlider){         
            avgArrivalRateLabel.setText(String.format(sliderArrival, lambda));
        }
        else{            
            avgServiceTimeLabel.setText(String.format(sliderService, mhu));
        }

        double rho = lambda / mhu;
        trafficIntensityLabel.setText(sliderTraffic + df.format(rho));

        if (rho > 0 && rho <= 1) { 
            createButton.setEnabled(true);
        } else {
            createButton.setEnabled(false);
        }
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
		action = new MenuAction("Solve", new AbstractMCHAction[] {start, pause, reload, nextStep, null});
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
        AbstractMCHAction[] actions = new AbstractMCHAction[] {start, pause, reload, nextStep, null, openHelp}; // Builds an array with all actions	
        toolbar.populateToolbar(actions);
        ArrayList<AbstractButton> buttons = new ArrayList<AbstractButton>(); //create a list of AbstractButtons for the helpLabel
		buttons.addAll(toolbar.populateToolbar(actions));

        //add help for each Action/JComboBox with helpLabel
		for (int i = 0; i < buttons.size(); i++) {
			AbstractButton button = buttons.get(i);
			help.addHelp(button, Constants.HELP_BUTTONS_ANIMATIONS[i]);
		}
		  
		parent.setToolBar(toolbar);
	}

    @Override
    public boolean canGoBack() {
		if (JOptionPane.showConfirmDialog(this, "This operation resets all data. Are you sure you want to go back to start screen?", "Back operation", JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION) {
			return false;
		}		
		parent.resetScreen(); //remove this panel and go back to the intitial MainPanel
		return true;
	}

    /**
     * Method called by the Create button to update this panel
     */
    private void updateAnimationPanel(){
        animation.pause();
        reloadAnimation();

        AnimDistribution sd = DistributionFactory.createDistribution(String.valueOf(serviceComboBox.getSelectedItem()));
        sd.setMean(mhu);
        AnimDistribution ad = DistributionFactory.createDistribution(String.valueOf(interAComboBox.getSelectedItem()));
        ad.setMean(lambda);

        if(simulation.getType() == SimulationType.NON_PREEMPTIVE || simulation.getType() == SimulationType.PREEMPTIVE || simulation.getType() == SimulationType.PROCESSOR_SHARING){            
            if(simulation.getType() == SimulationType.NON_PREEMPTIVE){ //in case of non preemptive update the title and the description
                simulation = SimulationFactory.createSimulation(simulation.getType(), String.valueOf(algorithmJComboBox.getSelectedItem()));
                mainPanel.setBorder(new TitledBorder(new EtchedBorder(), simulation.getType().toString() + " Scheduling - " + simulation.getName()));
                descrLabel.setText("<html><body><p style='text-align:justify;'><font size=\"3\">"+simulation.getDescription()+"</p></body></html>");
            }
        
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

        getSimulationResults();
    }

    

    /** Called each time 'Create' is pressed. Start the simulation with the engine to get the results of the simulation in the Results Panel */
    public void getSimulationResults(){
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
        solver = new Solver(simulation, lambda, mhu, interAComboBox.getSelectedIndex(), serviceComboBox.getSelectedIndex(), servers, prob, (Integer) maxSamples.getValue());

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


    @Override
    public void openHelp() {
        //TODO:fai help
    }

    //----------------- toolbar buttons actions
    @Override
    public void startAnimation() {
        animation.start();
        start.setEnabled(false);
        pause.setEnabled(true);
        reload.setEnabled(false);   
        nextStep.setEnabled(false);   
    }

    @Override
    public void pauseAnimation() {
        animation.pause();
        start.setEnabled(true);
        pause.setEnabled(false);
        reload.setEnabled(true);
        nextStep.setEnabled(true);
    }

    @Override
    public void reloadAnimation() {
        animation.reload();
        start.setEnabled(true);
        pause.setEnabled(false);
        reload.setEnabled(false);
        nextStep.setEnabled(true);
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
    public void stopAnimation() {
        disableAllButtons();
    }

    @Override
    public void exit(){
        parent.close();
    }

    public void disableAllButtons(){
        start.setEnabled(false);
        pause.setEnabled(false);
        reload.setEnabled(false);
        nextStep.setEnabled(false);
    }

    public int showInfoMessage() {
        Object[] options = {"Yes", "No"};
        return JOptionPane.showOptionDialog(parent, Constants.PROMPT_SIMULATION_FINISHED[0], Constants.PROMPT_SIMULATION_FINISHED[1], JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, null);
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

        parent.routeResults(solver.getQueueStrategy(), 
            solver.getInterArrivalDistribution(), 
            getLastMeasure(results, 0),
            solver.getServiceDistribution(), 
            solver.getNumberServers(),
            solver.getServiceTimeMean(), 
            getLastMeasure(results, 1),
            getLastMeasure(results, 2), 
            getLastMeasure(results, 3), 
            getLastMeasure(results, 4));          
    }
}
