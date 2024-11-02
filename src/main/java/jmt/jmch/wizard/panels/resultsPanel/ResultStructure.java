package jmt.jmch.wizard.panels.resultsPanel;

import java.util.Arrays;

import jmt.jmch.simulation.SimulationType;

/**
 * Custom class for saving the results of previous simulations
 *
 * @author Lorenzo Torri
 * Date: 19-Oct-2024
 * Time: 10.00
 */
public class ResultStructure{
	protected SimulationType type;
	
	/* data structures common to all the types of simulations */
	protected String[] algorithms;
    protected String[] arrivalDistibutions;
    protected double[] lambdas;
    protected String[] serviceDistributions;
    protected double[] responseTimes;
    protected double[] thoughputs;
    protected double[] nCustomers;

	/* data structures for Routing simulations */
	protected int[] queuesNumber;

	/* data strctures dor Scheduling simulations*/
	protected int[] nservers;
	protected double[] services;
	protected double[] queueTimes;

	
	public ResultStructure(){
		algorithms = new String[0];
		arrivalDistibutions = new String[0];
		lambdas = new double[0];
		serviceDistributions = new String[0];
		responseTimes = new double[0];
		thoughputs = new double[0];
		nCustomers = new double[0];

		queuesNumber = new int[0];

		nservers = new int[0];
		services = new double[0];
		queueTimes = new double[0];
	}

	/**
	 * Save the results of previous simulations in this data structure.
	 * If you need to save Routing simulations, than nservs, S, Q should be equal to arrays of size = 0.
	 * If you need to save Scheduling simulations, than queueN should be equal to an array of size = 0.
	 * @param simType the type of the simulation to save
	 * @param algo algorithms used
	 * @param arrivalD inter arrival distributions
	 * @param lamb lambdas
	 * @param serviceD service time distributions
	 * @param R response times
	 * @param X thoughputs
	 * @param N number of customers
	 * @param queueN number of queues (only for Routing)
	 * @param nservs number of servers (only for Scheduling)
	 * @param S service times (only for Scheduling)
	 * @param Q queue times (only for Scheduling)
	 */
	public void setAll(SimulationType simType, String[] algo, String[] arrivalD, double[] lamb, String[] serviceD, double[] R,  double[] X, double[] N, int[] queueN, int[] nservs, double[] S, double[] Q){
		type = simType;

		algorithms = Arrays.copyOf(algo, algo.length);
		arrivalDistibutions = Arrays.copyOf(arrivalD, arrivalD.length);
		lambdas = Arrays.copyOf(lamb, lamb.length);
		serviceDistributions = Arrays.copyOf(serviceD, serviceD.length);
		responseTimes = Arrays.copyOf(R, R.length);
		thoughputs = Arrays.copyOf(X, X.length);
		nCustomers = Arrays.copyOf(N, N.length);

		queuesNumber = Arrays.copyOf(queueN, queueN.length);

		nservers = Arrays.copyOf(nservs, nservs.length);
		services = Arrays.copyOf(S, S.length);
		queueTimes = Arrays.copyOf(Q, Q.length);
	}

    /* get methods */
	public SimulationType getPreviousSimulationType(){
		return type;
	}

    public String[] getAlgorithms() {
        return algorithms;
    }

    public String[] getArrivalDistibutions() {
        return arrivalDistibutions;
    }

    public double[] getLambdas() {
        return lambdas;
    }

    public int[] getNservers() {
        return nservers;
    }

    public double[] getQueueTimes() {
        return queueTimes;
    }

    public int[] getQueuesNumber() {
        return queuesNumber;
    }

    public double[] getResponseTimes() {
        return responseTimes;
    }

    public String[] getServiceDistributions() {
        return serviceDistributions;
    }

    public double[] getServices() {
        return services;
    }

    public double[] getThoughputs() {
        return thoughputs;
    }

    public SimulationType getType() {
        return type;
    }

    public double[] getnCustomers() {
        return nCustomers;
    }
}
