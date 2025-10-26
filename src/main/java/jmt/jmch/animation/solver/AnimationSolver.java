package jmt.jmch.animation.solver;

import jmt.jmch.animation.AnimationClass;
import jmt.jmch.animation.Job;

/**
 * This is the superclass for solving the model using the Animation.
 * Since the timer of the animation is effected by some errors (like fps...), this is a workouround to calculate the indices of the simulation.
 * The indices are calculated based just on the arrival time and the service time of each job.
 * The animation is used to generate the events (and also to schedule correclty the jobs, to route them to the correct station...) and then based on these events the solver calculates the indices.
 * 
 * For single queue and router the logic is quite the same, when a job arrives, the solver checks if it is dropped or not.
 * When a job exits the queue, the solver calculates the response time based on the time at which the previous job ended its service.
 * 
 * For PS the situation is a little bit more complex. The solver cannot calculate the response time just based on the arrival time, the service time of this job and the completion time of the previous job.
 * It needs also to store (or retrieve the list of the jobs from the station), and when a job enters the queue, add to each job in the queue the velocityOfProcessor * (entrance - last entrance),
 * by doing that in the end we know quite correclty how much time every job passed in the queue.
 * 
 * Of course for both the situation, the solver is still a little bit not totally synchronized with the animation. This because in the solver we are considering that arrival time = time at which it enters in the queue,
 * while in the animation, the time needed to run along the first edge is not 0!
 * 
 * @author Lorenzo Torri
 * Date: 13-may-2025
 * Time: 20.01
 */
public abstract class AnimationSolver {
    protected AnimationClass animation;
    protected int nQueues;
    protected double T;

    // -- variables for the indices
    private double X;
    private double R;
    private double Q;
    private double N;  
    private double W;

    // -- variables for the virtual time
    protected int generatedJobs;
    protected int completedJobs;
    protected double globalArrivalTime;

    public AnimationSolver(AnimationClass animation, int nQueues){
        this.animation = animation;
        this.nQueues = nQueues;
        init();
    }

    /**
     * Init variables, but it's also a way to reset all of them when the stop button is pressed
     */
    public void init(){
        this.globalArrivalTime = 0;
        this.completedJobs = 0;

        this.X = 0.0;
        this.R = 0.0;
        this.Q = 0.0;
        this.N = 0.0;
        this.W = 0.0;
    }

    //-- THE LOGIC OF THESE METHODS IS IN THE SUBCLASSES

    public abstract double jobEntrance(double randomArrival);

    public abstract void jobExit(Job job, int index);

    
    /**
     * Update the measures when a job is processed
     * @param responseTime
     */
    protected void updateMeasures(double responseTime, double serviceTime){
        completedJobs++; 

        X = (double) completedJobs / T;
        R = (double) (R * (completedJobs - 1) + (responseTime)) / (completedJobs);
        N = X * R;
        W = (double) (W * (completedJobs - 1) + (responseTime - serviceTime)) / (completedJobs); 
        Q = W;  

        //System.out.printf("X = %.4f, R = %.4f, N = %.4f, Q = %.4f\n",X,R,N,Q);
    }

    public double[] getMeasures(){
        double[] measures = {R, Q, X, N};
        return measures;
    }
}
