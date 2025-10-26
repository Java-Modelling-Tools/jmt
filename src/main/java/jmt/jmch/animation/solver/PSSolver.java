package jmt.jmch.animation.solver;

import jmt.jmch.animation.AnimationClass;
import jmt.jmch.animation.Job;

/**
 * Solver for PS animations
 * 
 * @author Lorenzo Torri
 * Date: 13-may-2025
 * Time: 20.13 
 */
public class PSSolver extends AnimationSolver{
    private double lastEntrance;

    public PSSolver(AnimationClass animation, int nQueues){
        super(animation, nQueues);
        lastEntrance = 0;
    }

    @Override
    public void init(){
        this.lastEntrance = 0;
        super.init();
    }

    //when a job enters in the queue, update all the cumulativeService of each job in the station (server or queue) based on the last arrival time (exacly for randomArrival time...)
    @Override
    public double jobEntrance(double randomArrival) {  
        double nextGlobalArrivalTime = lastEntrance + randomArrival;
        double interval = nextGlobalArrivalTime - globalArrivalTime;  //it could happen that in the meanwhile, between the arrival of two jobs, one or more have finished
        globalArrivalTime = nextGlobalArrivalTime;
        jobEvent(interval);  
        lastEntrance = globalArrivalTime;
        return globalArrivalTime;
    }

    private void jobEvent(double interval){
        //System.out.println("This event happened possibly at "+ globalArrivalTime + " , because interarrival is " + interval); 
        animation.getSingleStation().updateCumulativeService(interval);
    }

    //when a job exits the queue is like a jobEntrance(), so update every job cumulatedService and then just retrieve the cumulated Service, it is its responseTime!
    @Override
    public void jobExit(Job job, int index) {
        //when we update the cumulatedService time of the jobs in the station is deltaS = deltaT * v, now we now deltaS = duration - cumulated -> deltaT = deltaS / v
        double deltaS = job.getServiceTime() - job.getCumulatedService();
        double deltaT = deltaS / animation.getSingleStation().getProcessorSharingSpeed();
        //System.out.println("Exiting job "+job.id + " which service time was: " + job.getServiceTime() + ", and the cumulated is " + job.getCumulatedService());
        
        
        if(deltaT > 0){ //should be always > 0
            job.updateCumulatedService(animation.getSingleStation().getProcessorSharingSpeed(), deltaT);
            globalArrivalTime += deltaT;
            jobEvent(deltaT);
        }
        
        double responseTime = job.getCumulatedResponseTime();
        T = globalArrivalTime;
        if(responseTime == 0 || responseTime >= job.getServiceTime()){
            //System.out.println("-------------------------- JOB CORRETTO," +  job.getServiceTime() + " " + job.getCumulatedResponseTime());
            updateMeasures(responseTime == 0 ? job.getServiceTime() : responseTime, job.getServiceTime());
        }
        else{
            //System.out.println("-------------------------- JOB NON CORRETTO, SERVICE TIME > RESPONSE TIME " +  job.getServiceTime() + " " + job.getCumulatedResponseTime());
        }
        
    }

}
