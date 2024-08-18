package jmt.jmch.animation;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Arc2D;

import javax.swing.JComponent;

/**
 * Class for the server in the station
 *
 * @author Lorenzo Torri
 * Date: 25-mar-2024
 * Time: 15.35
 */
public class CircleStation extends JComponent implements JobContainer{	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//information about the station
	private Station station;
	private Point sPos; 
	private int sHeight;
	private int sLength;
	private float position;
	
	//information about the job currently processed it is all in arrays
	private Color color = Color.RED;
	private float progression; //value from 0 to 1 to know how much we have completed the job duration
	private Job currentJob;
	private long entranceTime;
	private boolean isWorking; //to know if it is processing a job or not
	private long passedTime;
	
	private long pauseTime; //this variable is important if we are processing a job and then the animator is paused, we need to know how much time the job remained paused and remove this value from the passedTime
	//then set this value = 0 when the job gets out from this class
	private int velocityFactor = 0; //to increase the velocity of the simulation
	private double processorSpeed = 1; //only for processorsharing
	
	/**
	 * Constructor
	 * @param st, Station that has this circle
	 * @param position, set it = 0.0f if it is a single server station. Otherwise set it equal to the correct position for multiple servers
	 */
	public CircleStation(Station st, float position) {
		station = st;
		sPos = st.getPosition();
		sHeight = st.getHeight();
		sLength = st.getLength();
		isWorking = false;
		this.progression = 0.0f;
		this.position = position;
		pauseTime = 0;
	}
	
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		
		g.setColor(color);
        Graphics2D g2d = (Graphics2D) g.create();
        //int radius = sHeight/2;
        
        Arc2D arc = new Arc2D.Double((sPos.x + sLength+1), sPos.y + position, sHeight, sHeight, 90, 360*progression, Arc2D.PIE); //paint the circle
    	g2d.fill(arc);    
	}
	
	@Override
	public void refresh() {
		if(isWorking) {
			long duration = (long) (currentJob.getDuration() * Math.pow(10, 3));
			passedTime += (System.currentTimeMillis() - entranceTime - pauseTime)* (velocityFactor + processorSpeed); //both the factor for the step and the one of processor sharing
			entranceTime = System.currentTimeMillis();
			pauseTime = 0;
			
			long diff =  duration - passedTime;
			progression = (float) diff/duration;
			
			if(progression < 0) {
				isWorking = false;
				progression = 0; //to avoid printing another cycle of the circle
				pauseTime = 0;
				passedTime = 0;
				
				station.routeJob(currentJob);
			}
		}	
	}
	
	@Override
	public void addJob(JobContainer prec, Job newJob) {
		currentJob = newJob;
		entranceTime = System.currentTimeMillis();
		isWorking = true;
		color = newJob.getColor();
	}
	
	/**
	 * To know if the station is currently processing a job or not
	 * @return true if it is processing a job, false otherwise
	 */
	public boolean isWorking() {
		return isWorking;
	}
	
	/**
	 * To know for how long the animator has stopped
	 * @param value the time of pausing
	 */
	public void setPauseTime(long value) {
		if(isWorking) { //only if the circle is working it is important to set the pauseTime, otherwise not.
			pauseTime += value;
		}	
	}
	
	public void setPosition(Point pos) {
		sPos = pos;
	}

	public void setVelocityFactor(int value) {
		velocityFactor = value;
	}

	public void setProcessorSpeed(double velocity) {
		processorSpeed = velocity;
	}

	
}
