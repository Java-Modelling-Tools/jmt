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

package jmt.jmch.animation;


import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.text.DecimalFormat;
import java.util.Comparator;

import javax.swing.JComponent;
import javax.swing.JPanel;

import jmt.jmch.Constants;
import jmt.jmch.animation.customQueue.CustomCollection;
import jmt.jmch.animation.customQueue.FIFOQueue;
import jmt.jmch.animation.customQueue.LIFOQueue;
import jmt.jmch.animation.customQueue.PRIOQueue;
import jmt.jmch.simulation.Simulation;

/**
 * Class for a station in the animation
 *
 * @author Lorenzo Torri
 * Date: 11-mar-2024
 * Time: 16.29
 */
public class Station extends JComponent implements JobContainer, GraphicComponent{
	private JPanel parent; //panel in which the Station is drawn
	private AnimationClass animation;
	
	//general information about the station
	private Point pos;
	private boolean xcentered;
	private boolean ycentered;
	private int height = 60;
	private int length = 120;
	private Simulation simulation;
	private int nServers;
	private boolean paintQueueSize = false; //this parameter is used to know if it is needed to paint the size of the queue above the station (useful for polices like JSQ
	
	//this is the queue of Jobs, see CustomCollection to understand why this and not a general Collection
	private CustomCollection<Job> jobQueue; 
	private int arrivingJobs = 0; //jobs on the edge from the router to the station
	private int currentSizeQueue = 0;
	private JobContainer next;
	
	//-- subcomponents of the station, the BoxStation, and the Circle
	private BoxStation[] boxes;
	private final int sizeQueue = 5;	
	private CircleStation[] circles;

	//for the Step simulation
	private boolean nextEvent = false;
	
	/**
	 * Constructor
	 * @param animation, AnimationClass which the edge is part of
	 * @param parent, JPanel that contains this station
	 * @param xcentered, if the station is x centered with respect to the parent
	 * @param ycentered, if the station is y centered with respect to the parent
	 * @param pos, position of the station, if it is x-y centered then the respective coordinates are not considered
	 * @param next, Jobcontainer connected to this station
	 * @param sim, information about the simulation
	 * @param servers, number of servers
	 */
	public Station(AnimationClass anim, JPanel parent, boolean xcentered, boolean ycentered, Point pos, JobContainer next, Simulation sim, int servers) {
		this.animation = anim;
		this.parent = parent;
		this.pos = pos; 
		this.xcentered = xcentered;
		this.ycentered = ycentered;
		this.simulation = sim;
		this.nServers = servers;
		this.next = next;		
		
		boxes = new BoxStation[sizeQueue];
		for(int i = 0; i < sizeQueue; i++) {
        	boxes[i] = new BoxStation(this, i);
        }
		
		float position = 0.0f;
		if(nServers == 2) {
			position = -height/2;
		}
		circles = new CircleStation[nServers];
		for(int i = 0; i < nServers; i++) {
			circles[i] = new CircleStation(this, position);
			position *= -1;
		}
				
		typeOfQueue(simulation);
	}
	
	/**
	 * Create the CustomCollection instance, based on the simulation information
	 * @param sim 
	 */
	@SuppressWarnings("unchecked")
	protected void typeOfQueue(Simulation sim) {
		switch(sim.getName()) {
			case "FCFS":
				jobQueue = new FIFOQueue<Job>();
				break;
			case "LCFS":
				jobQueue = new LIFOQueue<Job>();
				break;
			case "SJF":
				jobQueue = new PRIOQueue<Job>(new Comparator<Job>() {
					@Override
					public int compare(Job o1, Job o2) {
						return Double.compare(o1.getDuration(), o2.getDuration());
					}
					
				});
				break;
			case "LJF":
				jobQueue = new PRIOQueue<Job>(new Comparator<Job>() {
					@Override
					public int compare(Job o1, Job o2) {
						return Double.compare(o2.getDuration(), o1.getDuration());
					}
					
				});
				break;
			default: //default since the sim.getName could be also something like RR, so in this case we opt for a station with FIFO policy 
				jobQueue = new FIFOQueue<Job>();
				break;
		}
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
 
		//----paint all sub-components below the station
        for(int i = 0; i < boxes.length; i++) {
			boxes[i].paint(g);
		}   
        for(int i = 0; i < nServers; i++) {
        	circles[i].paint(g);
        }
		
		//get the correct position for centering the station in the panel
		if(xcentered) {
			int widthPanel = parent.getWidth();	
			pos.x = parent.getX()+(widthPanel - length - height)/2;;		
		}
		if(ycentered) {
			int heightPanel = parent.getHeight();
			pos.y = parent.getY()+(heightPanel- height)/2;
		}
		
		for(BoxStation box: boxes) {
			box.setPosition(pos);
		}
		for(CircleStation circle: circles){
			circle.setPosition(pos);
		}
		
		g.setColor(Color.BLACK);
        g.drawRect(pos.x, pos.y, length, height); //create the box of the station centered in the panel
        
        //for creating the lines between in the station
        int size = length/sizeQueue;
        for(int i = 1; i < sizeQueue; i++) {
        	g.drawLine(pos.x + i*size, pos.y, pos.x + i*size, pos.y + height);
        }
        
        g.setColor(Color.BLACK);
        if(nServers == 1) {
        	g.drawOval(pos.x + length + 1, pos.y, height, height); //create the circle of the station
        }
        else {
        	g.drawOval(pos.x + length + 1, pos.y - height/2, height, height);
        	g.drawOval(pos.x + length + 1, pos.y + height/2, height, height);
        }
        
       
        if(paintQueueSize) {
        	g.setColor(Color.BLACK);
        	g.setFont(new Font("Arial", Font.PLAIN, 10));
        	g.drawString("Queue Size: "+String.valueOf(jobQueue.size() + arrivingJobs), pos.x,  pos.y-10);
        }

		if(simulation.getName() == Constants.PS){ //if processor sharing, then print the processorSpeed
			g.setColor(Color.BLACK);
        	g.setFont(new Font("Arial", Font.PLAIN, 10));
        	double processorSpeed = jobQueue.size() > 0 ? (double)nServers/(jobQueue.size() + nServers): 1;
        	DecimalFormat df = new DecimalFormat("#.##");
			int percentage = (int) (processorSpeed * 100);
        	g.drawString("Processor Speed: "+df.format(processorSpeed) + "  ("+ percentage +"%)", pos.x,  pos.y-25);
		}
	}
	
	@Override
	public void refresh() {		
		boolean somethingHasChanged = false;
		somethingHasChanged = currentSizeQueue != jobQueue.size(); //in case a new job is added to the station

		if(jobQueue.size() >= 1) { //some jobs are waiting to enter inside the circles
			for(int i = 0; i < nServers; i++) {
				if(!circles[i].isWorking()) { //if the circle is not working than the head of the queue can go inside the circle
					somethingHasChanged = true;
					circles[i].addJob(this, (Job) jobQueue.first());
					jobQueue.removeHead();
					
					if(jobQueue.size() == 0) {
						break;
					}
				}				
			}
		}

		double processorSpeed = jobQueue.size() > 0 ? (double)nServers/(jobQueue.size() + nServers): 1; //this is needed only for processorSharing
		
		for(int i = 0; i < nServers; i++) {
			if(simulation.getName() == Constants.PS) {
				circles[i].setProcessorSpeed(processorSpeed);
			}
			circles[i].refresh();
		}
				
		//for all the jobs in the queue, we need to update the correspondent boxStation
		int index = 0;
		for(Job j: jobQueue) {
			boxes[index].addJob(this, j);
			index++;
		}
		
		//if other boxStations are free, then clear them
		for(int i = index; i < sizeQueue; i++) {
			boxes[i].clear();
		}

		if(simulation.getName() == Constants.PS){
			for(BoxStation b: boxes) {
				b.setProcessorSpeed(processorSpeed);
				b.refresh();
			}
		}

		currentSizeQueue = jobQueue.size();

		if(somethingHasChanged && nextEvent){ //stop the simulation if the jobs inside the station moved
			animation.pause();
			animation.resetNextEvent();
		}
	}
	
	/**
	 * Update all the subComponents of Station that are affected by a Pause in the Animator.
	 * @param pause value for which the animator was paused
	 */
	public void updatePause(long pause) {
		for(int i = 0; i < nServers; i++) {
			circles[i].setPauseTime(pause);
		}	
		for(int i = 0; i < boxes.length; i++) {
			boxes[i].setPauseTime(pause);
		}
	}

	/**
	 * Once the job is processed, then it must be routed to the next JobContainer. Can be called both by a CircleStation, but also by a BoxStation if processor sharing is active
	 * @param job the job to be routed
	 */
	public void routeJob(Job job) {
		job.setDuration(); //set the duration = 0 since the job is completely processed
		//then if the next element is an edge, the job must be painted as a green circle, otherwise do not paint it
		if(getNextContainer() instanceof Edge) {
			job.setOnEdge();
		}
		else {
			job.unsetOnEdge();
		}
		getNextContainer().addJob(this, job);
	}
	
	@Override
	public void addJob(JobContainer prec, Job newJob) {
		newJob.setEntrance(); //update the entrance in the queue
		arrivingJobs--;
		
		//try to add the job inside the queue, if the queue is full, then drop the job
		if(jobQueue.size() != sizeQueue) {
			jobQueue.addNew(newJob);
		}	

		if(nextEvent) { //refresh the component to stop the simulation, the stopping politic is then handled in the refresh method
			refresh();
		}
	}

	/** Remove the job from the collection, called by the BoxStation */
	public void removeJob(Job job) {
		jobQueue.removeObject(job);
	}

	public void addArrivingJob(){
		arrivingJobs++;
	}

	public Point getPosition() {
		return pos;
	}
	
	public int getHeight() {
		return height;
	}
	
	public int getLength() {
		return length;
	}
	
	public int getsizeQueue() {
		return sizeQueue;
	}
	
	public int getNumberWaitingJobs() {
		return jobQueue.size() + arrivingJobs;
	}
	
	public Simulation getSimulation() {
		return simulation;
	}
	
	public JobContainer getNextContainer() {
		return next;
	}
	
	public void paintQueueSize() {
		paintQueueSize = true;
	}

	public void updateNServers(int nservers){
		this.nServers = nservers;
		float position = 0.0f;
		if(nServers == 2) {
			position = -height/2;
		}
		circles = new CircleStation[nServers];
		for(int i = 0; i < nServers; i++) {
			circles[i] = new CircleStation(this, position);
			position *= -1;
		}
	}

	public void setVelocityFactor(int value) {
		for(CircleStation c: circles) {
			c.setVelocityFactor(value);
		}

		for(BoxStation b: boxes) {
			b.setVelocityFactor(value);
		}
	}

	/** Set the Step simulation on */
	public void nextEvent() {
		nextEvent = true;
	}

	/** Set the Step simulation off */
	public void resetNextEvent() {
		nextEvent = false;
	}

	public boolean isProcessorSharing() {
		return simulation.getName() == Constants.PS;
	}

	

	

	//------- Graphic Component Methods
	@Override
	public int getXPos() {
		return pos.x;
	}

	@Override
	public int getYPos(){
		return pos.y;
	}
	
	@Override
	public int getTotalLenght() {
		return length + length/2;
	}

	@Override
	public void setYPos(int yPos){
		pos.y = yPos;
	}
	@Override
	public void setXPos(int xPos){
		pos.x = xPos;
	}
}
