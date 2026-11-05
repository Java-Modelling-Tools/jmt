package jmt.jmch.animation.solver;

import jmt.jmch.animation.AnimationClass;
import jmt.jmch.animation.Job;

/**
 * Solver for all the different animations (no PS)
 *
 * @author Lorenzo Torri
 * Date: 10-apr-2025
 * Time: 20.25
 */
public class GeneralSolver extends AnimationSolver{       
    private double[][] globalCompletionTime; //it's the time at which the i-th job finishes its execution. Array because in case of Routing we need 3 globalCompletitionTimes, each station could have 2 server

    public GeneralSolver(AnimationClass animation, int nQueues){
        super(animation, nQueues);
        
        init();
    }

    @Override
    public void init(){
        this.globalCompletionTime = new double[nQueues][2];
        super.init();
    }

    /**
     * Set the real arrival time of a job as Real arrival time previous job + new random value
     * @param randomArrival the new random value from the distribution
     * @return
     */
    @Override
    public double jobEntrance(double randomArrival){
        globalArrivalTime += randomArrival;
        return globalArrivalTime;
    }

    /**
     * Sets the completion time (T) based on the arrival of the job in the system and when the previous job finished.
     * @param job the job exiting the system
     * @param index the i-th station (0 if single queue)
     */
    @Override
    public void jobExit(Job job, int index){
        generatedJobs = job.id;
        double arrivalTime = job.getArrivalTime();
        double serviceTime = job.getServiceTime();
        double responseTime = 0.0;

        double startTime = Math.max(arrivalTime, globalCompletionTime[index][job.getIdServer()]);
        double endTime = startTime + serviceTime;
        globalCompletionTime[index][job.getIdServer()] = endTime;
        responseTime = endTime - arrivalTime;

        //System.out.printf("Job %d: interarrival = %.4f, service = %.4f, arrival = %.4f, exit = %.4f, response = %.4f, queue = %.4f\n",
            //job.id, job.getInterArrivalTime(), serviceTime, arrivalTime, endTime, responseTime, responseTime-serviceTime);
    

        T = globalCompletionTime[index][job.getIdServer()];
        updateMeasures(responseTime, serviceTime);
    }
    
}
