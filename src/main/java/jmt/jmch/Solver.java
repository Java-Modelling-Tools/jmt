package jmt.jmch;

import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.Defaults;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.definitions.ServerType;
import jmt.gui.common.definitions.SimulationDefinition;
import jmt.gui.common.distributions.Deterministic;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.distributions.Exponential;
import jmt.gui.common.distributions.Hyperexponential;
import jmt.gui.common.distributions.Uniform;
import jmt.gui.common.routingStrategies.ProbabilityRouting;
import jmt.jmch.simulation.Simulation;
import jmt.jmch.simulation.SimulationType;

/**
 * This class transforms the information of the Animation to match the structure of the CommonModel class.
 * CommonModel is used to obtain the results of the simulation.
 * 
 * @author Lorenzo Torri
 * Date: 06-mag-2024
 * Time: 17.28
 */
public class Solver implements CommonConstants{
    private CommonModel model;
    private Simulation simulation;
    private boolean isSingleQueue = true;

    private int classNameIndex = 1;
    private int serverNameIndex = 1;
    private int servers = 1;

    //------------------- keys of the model --------------------------
    private Object classKey;
    private Object sourceKey;
    private Object sinkKey;
    private Object routerKey;

    //----------- all changable fields of the simulation -------------
    private static Distribution[] serviceDistributions = { 
        new Exponential(),
        new Deterministic(),
        new Uniform(),
        new Hyperexponential()
    };

    private static Distribution[] interArrivalDistributions = { 
        new Exponential(),
        new Deterministic(),
        new Uniform(),
        new Hyperexponential()
    };


    //types of measures selectable
	/*protected static final String[] measureTypes = new String[] {
			"------ Select an index  ------",
			SimulationDefinition.MEASURE_QL,
			SimulationDefinition.MEASURE_QT,
			SimulationDefinition.MEASURE_RP,
			SimulationDefinition.MEASURE_RD,
			SimulationDefinition.MEASURE_AR,
			SimulationDefinition.MEASURE_X,
			SimulationDefinition.MEASURE_U,
			SimulationDefinition.MEASURE_T,
			SimulationDefinition.MEASURE_E,
			SimulationDefinition.MEASURE_L,
			"------ Advanced indexes ------",
			SimulationDefinition.MEASURE_EU,
			SimulationDefinition.MEASURE_DR,
			SimulationDefinition.MEASURE_BR,
			SimulationDefinition.MEASURE_RN,
			SimulationDefinition.MEASURE_RT,
			SimulationDefinition.MEASURE_OS,
			SimulationDefinition.MEASURE_OT,
			SimulationDefinition.MEASURE_P,
			SimulationDefinition.MEASURE_RP_PER_SINK,
			SimulationDefinition.MEASURE_X_PER_SINK,
			SimulationDefinition.MEASURE_FCR_TW,
			SimulationDefinition.MEASURE_FCR_MO,
			SimulationDefinition.MEASURE_FJ_CN,
			SimulationDefinition.MEASURE_FJ_RP,
			SimulationDefinition.MEASURE_FX,
			SimulationDefinition.MEASURE_NS
	};*/

    /**
     * @param sim Simulation data Structure
     * @param lambda parameter for the InterArrival Time
     * @param mhu parameter for the Service Time
     * @param indexInter index of the array of Distributions for InterArrival Time
     * @param indexService index of the array of Distributions for Service Time
     * @param nservers number of servers in the simulation
     * @param prob array of probabilities (for Routing Probabilities)
     * @param maxSamples max samples for the animation
     */
    public Solver(Simulation sim, double lambda, double mhu, int indexInter, int indexService, int nservers, double[] prob, int maxSamples){
        model = new CommonModel();
        simulation = sim;
        servers = nservers;
        isSingleQueue = simulation.getType() != SimulationType.ROUTING;
        setDistributionsParameters(serviceDistributions, mhu);
        setDistributionsParameters(interArrivalDistributions, lambda);
        addClass(indexInter);
        model.setMaxSimulationSamples(maxSamples);
        
        addQueue(indexService, nservers);
        if(!isSingleQueue){
            addQueue(indexService, nservers);
            addQueue(indexService, nservers);
            addRouter();
        }

        addSource();
        addSink();
        
        setConnections();  
        
        addMeasure(); 
        
        if(!isSingleQueue){ //this operation needs to be performed after setting the connections
            setRouterStrategy(prob);
        } 
    }

    /** Set the parameters for all the distributions. Always keep the mean = 1/mhu, change the cv accordingly */
    public void setDistributionsParameters(Distribution[] distributions, double mhu){
        double mean = 1/mhu;
        distributions[0].setMean(1/mhu);

        distributions[1].setMean(1/mhu);

        double min = mean - 1;
        double max = mean + 1;
        double ds = (max-min)/Math.sqrt(12);
        distributions[2].setMean(mean);
        distributions[2].setC(ds/mean);

        double p = 0.5; 
        double l1 = 1;
        double l2 = 1/(mean*2 - 1); //l1 = 1, l2 can be retrieved with this formula to keep the mean = 1/mhu
        double var = 2*p/(Math.pow(l1, 2)) + 2*p/(Math.pow(l2, 2)) - Math.pow(mean, 2);
        distributions[3].setMean(mean);
        distributions[3].setC(Math.sqrt(var)/mean);
    }

    /** Add a class to the model (similar to the task you perform in JSIM when you click on the class button) */
    private void addClass(int indexInter){
        classKey = model.addClass(Defaults.get("className") + (++classNameIndex),
            0,
            Defaults.getAsInteger("classPriority"),
            Defaults.getAsDouble("classSoftDeadline"),
            Defaults.getAsInteger("classPopulation"), 
            interArrivalDistributions[indexInter]); 
    }

    /** Add a source station to the model (like adding a Source cell to the JGraph of JSIM) */
    private void addSource(){
        sourceKey = model.addStation("Source", STATION_TYPE_SOURCE, 1, new ArrayList<ServerType>());
        //model.setRoutingStrategy(sourceKey, classKey, Defaults.getAsNewInstance("stationRoutingStrategy")); //random routing by default
        model.setClassRefStation(classKey, sourceKey); 
    }
    

    /** Add a queue station to the model (like adding a Station cell to the JGraph in JSIM) */
    private void addQueue(int indexService, int nservers){
        Object serverKey = model.addStation("Queue "+(serverNameIndex), STATION_TYPE_SERVER, 1, new ArrayList<ServerType>());
        serverNameIndex++;

        model.setStationQueueCapacity(serverKey, 5);
        model.setDropRule(serverKey, classKey, "Drop");
        setStrategy(serverKey);
        //model.setServiceWeight(serverKey, classKey, Defaults.getAsDouble("serviceWeight"));
        //model.updateBalkingParameter(serverKey, classKey, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE);
       
        model.setStationNumberOfServers(serverKey, nservers);
        model.updateNumOfServers(serverKey, nservers);

        model.setServiceTimeDistribution(serverKey, classKey, serviceDistributions[indexService]);
    }

    /* To change the type of simulation and the algorithm */
    private void setStrategy(Object serverKey){
        String strategy = "";
        String algorithm = simulation.getName();

        switch(simulation.getType()){ //setting the correct string of strategy and algorithm from CommonConstants
            case NON_PREEMPTIVE:
                strategy = STATION_QUEUE_STRATEGY_NON_PREEMPTIVE;
                break;
            case PROCESSOR_SHARING:
                strategy = STATION_QUEUE_STRATEGY_PSSERVER;
                break; 
            case ROUTING:
                strategy = STATION_QUEUE_STRATEGY_NON_PREEMPTIVE;
                algorithm = QUEUE_STRATEGY_FCFS;
            default: 
                break;
        }

        model.setStationQueueStrategy(serverKey, strategy);
        model.setQueueStrategy(serverKey, classKey, algorithm);         
    }   

    /** Add a sink station to the model (like adding a Sink cell to teh JGraph in JSIM) */
    private void addSink(){
        sinkKey = model.addStation("Sink", STATION_TYPE_SINK, 1, new ArrayList<ServerType>());
    }

    private void addRouter(){
        routerKey = model.addStation("Router", STATION_TYPE_ROUTER, 1, new ArrayList<ServerType>());  
    }

    
    /** Connect the components of model (like adding edges to the JGraph in JSIM) */
    private void setConnections(){
        if(isSingleQueue){
            model.setConnected(sourceKey, model.getStationKeys().get(0), true);
            model.setConnected(model.getStationKeys().get(0), sinkKey, true);
        }
        else{
            model.setConnected(sourceKey, routerKey, true);
            model.setConnected(routerKey, model.getStationKeys().get(0), true);
            model.setConnected(routerKey, model.getStationKeys().get(1), true);
            model.setConnected(routerKey, model.getStationKeys().get(2), true);
            model.setConnected(model.getStationKeys().get(0), sinkKey, true);
            model.setConnected(model.getStationKeys().get(1), sinkKey, true);
            model.setConnected(model.getStationKeys().get(2), sinkKey, true);
        }
    }

    /** Add all the metrics to control (like setting the parameters in JSIM) */
    private void addMeasure(){
        model.addMeasure(SimulationDefinition.MEASURE_AR, model.getStationKeys().get(0), classKey); //0
        //service??
        model.addMeasure(SimulationDefinition.MEASURE_RP, model.getStationKeys().get(0), classKey); //1
        model.addMeasure(SimulationDefinition.MEASURE_QT, model.getStationKeys().get(0), classKey); //2
        model.addMeasure(SimulationDefinition.MEASURE_X, model.getStationKeys().get(0), classKey); //3
        model.addMeasure(SimulationDefinition.MEASURE_QL, model.getStationKeys().get(0), classKey);  //4             
    }

    /** Set the router strategy */
    private void setRouterStrategy(double[] probabilities){
        switch(simulation.getName()){
            case "RR":
                model.setRoutingStrategy(routerKey, classKey, ROUTING_ROUNDROBIN);
                break;
            case "JSQ":
                model.setRoutingStrategy(routerKey, classKey, ROUTING_SHORTESTQL);
                break;
            case "PROBABILITIES":
                ProbabilityRouting pr = new ProbabilityRouting();
                model.setRoutingStrategy(routerKey, classKey, pr);
                Map<Object, Double> values = pr.getValues();
                values.clear();
                
                Vector<Object> outputs = model.getForwardConnections(routerKey);

                for (int i = 0; i < outputs.size(); i++) {
                    values.put(outputs.get(i), Double.valueOf(probabilities[i]));
                } 
                break;                  
        }
        
    }

    public CommonModel getModel(){
        return model;
    }

    //--------------- methods to get some parameters of the simulation --------------------
    public String getQueueStrategy(){
        return model.getQueueStrategy(model.getStationKeys().get(0), classKey);
    }

    public String getInterArrivalDistribution(){
        return ((Distribution) model.getClassDistribution(classKey)).toString();
    }

    public String getServiceDistribution(){
        return ((Distribution) model.getServiceTimeDistribution(model.getStationKeys().get(0), classKey)).toString();
    }

    public double getServiceTimeMean(){
        return ((Distribution) model.getServiceTimeDistribution(model.getStationKeys().get(0), classKey)).getMean();
    }

    public int getNumberServers(){  
        return servers;
    }    
}
