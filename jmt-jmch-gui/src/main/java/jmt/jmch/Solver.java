package jmt.jmch;

import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;

import common.CommonConstants;
import common.Defaults;
import common.definitions.CommonModel;
import common.definitions.ServerType;
import common.definitions.SimulationDefinition;
import common.distributions.Distribution;
import common.routingStrategies.ProbabilityRouting;
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
    private int maxSamples;

    private int classNameIndex = 1;
    private int serverNameIndex = 1;
    private int servers = 1;

    private int nQueues = 1; //(for the probabilities, nQueues could be 1,2 or 3 based on the probabilities)

    private Distribution interADistr;
    private Distribution serviceDistr;

    //------------------- keys of the model --------------------------
    private Object classKey;
    private Object sourceKey;
    private Object sinkKey;
    private Object routerKey;


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
     * 
     * @param sim Simulation data Structure
     * @param interADistr Distribution for the inter arrival time
     * @param serviceDistr Distribution for the service time
     * @param nservers number of servers in the simulation
     * @param prob array of probabilities (for Routing Probabilities)
     * @param maxSamples max samples for the animation
     * @param seed the seed for the random generator used in the simulation
     */
    public Solver(Simulation sim, Distribution interADistr, Distribution serviceDistr, int nservers, double[] prob, int maxSamples, long seed){
        model = new CommonModel();
        this.simulation = sim;
        this.interADistr = interADistr;
        this.serviceDistr = serviceDistr;
        this.servers = nservers;
        this.maxSamples = maxSamples;
        
        isSingleQueue = simulation.getType() != SimulationType.ROUTING;

        addClass();
        //model.setMaxSimulationSamples(maxSamples); da problemi con alcuni modelli
        model.setMaxSimulationEvents(maxSamples);
        
        addQueue();
        if(!isSingleQueue){
            addQueue();
            addQueue();
            addRouter();
        }

        addSource();
        addSink();
        
        setConnections();  
        
        addMeasure(); 
        
        if(!isSingleQueue){ //this operation needs to be performed after setting the connections
            setRouterStrategy(prob);
            checkNumberOfQueues(prob);
        } 

        //same configuration = same result (remove randomness)
        model.setUseRandomSeed(false);
        model.setSimulationSeed(seed);
    }


    /** Add a class to the model (similar to the task you perform in JSIM when you click on the class button) */
    private void addClass(){
        classKey = model.addClass(Defaults.get("className") + (++classNameIndex),
            0,//Defaults.getAsInteger("classType").intValue(), 
            Defaults.getAsInteger("classPriority"),
            Defaults.getAsDouble("classSoftDeadline"),
            Defaults.getAsInteger("classPopulation"), 
            interADistr); 
    }

    /** Add a source station to the model (like adding a Source cell to the JGraph of JSIM) */
    private void addSource(){
        sourceKey = model.addStation("Source", STATION_TYPE_SOURCE, 1, new ArrayList<ServerType>());
        //model.setRoutingStrategy(sourceKey, classKey, Defaults.getAsNewInstance("stationRoutingStrategy")); //random routing by default
        model.setClassRefStation(classKey, sourceKey); 
    }
    

    /** Add a queue station to the model (like adding a Station cell to the JGraph in JSIM) */
    private void addQueue(){
        Object serverKey = model.addStation("Queue "+(serverNameIndex), STATION_TYPE_SERVER, 1, new ArrayList<ServerType>());
        serverNameIndex++;

        model.setStationQueueCapacity(serverKey, 5+servers);
        model.setDropRule(serverKey, classKey, "Drop");
        setStrategy(serverKey);
        //model.setServiceWeight(serverKey, classKey, Defaults.getAsDouble("serviceWeight"));
        //model.updateBalkingParameter(serverKey, classKey, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE);
       
        model.setStationNumberOfServers(serverKey, servers);
        model.updateNumOfServers(serverKey, servers);

        model.setServiceTimeDistribution(serverKey, classKey, serviceDistr);
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
        if(isSingleQueue){
            model.addMeasure(SimulationDefinition.MEASURE_AR, model.getStationKeys().get(0), classKey); //0
            model.addMeasure(SimulationDefinition.MEASURE_RP, model.getStationKeys().get(0), classKey); //1
        }
        else{
            model.addMeasure(SimulationDefinition.MEASURE_AR, routerKey, classKey);
            model.addMeasure(SimulationDefinition.MEASURE_S_RP, "", classKey); //1
        }
        
       
        model.addMeasure(SimulationDefinition.MEASURE_QT, model.getStationKeys().get(0), classKey); //2

        if(isSingleQueue){
            model.addMeasure(SimulationDefinition.MEASURE_X, model.getStationKeys().get(0), classKey); //3
            model.addMeasure(SimulationDefinition.MEASURE_QL, model.getStationKeys().get(0), classKey);  //4 
        }
        else{ //in this case we look at system parameters, so stationKey = null
            model.addMeasure(SimulationDefinition.MEASURE_S_X, "", classKey); //3
            model.addMeasure(SimulationDefinition.MEASURE_S_CN, "", classKey);  //4 
        }
                    
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

    /* Check the number of queues reachable */
    private void checkNumberOfQueues(double[] prob){
        if(prob == null){
            if(isSingleQueue){
                nQueues = 1;
            }
            else{
                nQueues = 3;
            }    
            return;
        }
        nQueues = 0;
        if(prob[0] > 0){
            nQueues++;
        }
        if(prob[1] > 0){
            nQueues++;
        }
        if(prob[2] > 0){
            nQueues++;
        }
    }

    public CommonModel getModel(){
        return model;
    }

    //--------------- methods to get some parameters of the simulation --------------------
    public String getStrategy(){
        return simulation.getName();
    }

    public String getInterArrivalDistribution(){
        return interADistr.toString();
    }

    public double getInterArrivalTimeMean(){
        return (1/(double) interADistr.getMean());
    }

    public String getServiceDistribution(){
        return serviceDistr.toString();
    }

    public double getServiceTimeMean(){
        return serviceDistr.getMean();
    }

    public int getNumberServers(){  
        return servers;
    }   

    public int getNumberQueues(){
        return nQueues;
    }

    public int getMaxSamples(){
        return maxSamples;
    }
}
