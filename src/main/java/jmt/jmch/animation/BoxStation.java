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
import java.awt.Graphics;
import java.awt.Point;

import javax.swing.JComponent;

/**
 * Class for a single space inside the queue of the station
 *
 * @author Lorenzo Torri
 * Date: 25-mar-2024
 * Time: 15.03
 */
public class BoxStation extends JComponent implements JobContainer{	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//information about the station
	private Station station;
	private int index; //this is the index of this box inside the array of boxes of the Station
	private Point sPos; 
	private int sLength;
	private int sHeight;
	private int queueSize;
	
	//information about the current Job inside the BoxStation
	private Job currentJob = null;
	private Color color;
	private double duration;
	private double mapDuration;

	//information about the current Job for processor sharing
	private long entranceTime;
	private long pausedTime = 0;
	private double velocityFactor = 1;
	private double processorSpeed = 1;
	
	private boolean isWorking = false; //to know if there is a job in this BoxStation or not to print the circle above
	private int sizeCircle = 10; //circle above the box if there is a job 
	
	/**
	 * Constructor
	 * @param st, station of this boxstation
	 * @param i, index in the list of boxstations
	 */
	public BoxStation(Station st, int i) {
		this.station = st;
		this.index = i;
		sPos = st.getPosition();
		sLength = st.getLength();
		sHeight = st.getHeight();
		queueSize = st.getsizeQueue();
		duration = 0;
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		
		int size = sLength/queueSize;
		
		g.setColor(color); //set the color of the job
		int result = (int) Math.round(mapDuration == 1.0 ? sHeight - 1: mapDuration*sHeight);
		g.fillRect(sPos.x + (queueSize-index-1)*size + 1, sPos.y + (sHeight-result), size - 1, result); //+1 - 1 values are used to avoid clipping with other figures
		
		//paint the circle above
		if(isWorking) {
			g.setColor(color);
			g.fillOval(sPos.x + (queueSize-index-1)*size + (sLength/queueSize)/2 - sizeCircle/2, sPos.y - sizeCircle - 10, sizeCircle, sizeCircle); 
		}
	}

	@Override
	public void refresh() { //in general refresh does nothing, but if processor sharing is active, then the job is processed even if is in the queue
		if(station.isProcessorSharing() && isWorking) {
			long jobDuration = (long) (currentJob.getDuration() * Math.pow(10, 3));
			//System.out.println((System.currentTimeMillis() - entranceTime - pausedTime)*velocityFactor);
			double newDuration = (jobDuration - (System.currentTimeMillis() - entranceTime - pausedTime)* (velocityFactor * processorSpeed)) / (Math.pow(10,3));
			currentJob.setDuration(newDuration);
			entranceTime = System.currentTimeMillis();
			pausedTime = 0;
			
			duration = currentJob.getDuration();
			//System.out.println("job "+currentJob.toString()+ " duration:"+duration);
			
			if(duration < 0) {
				isWorking = false;
				pausedTime = 0;
				
				station.removeJob(currentJob);
				station.routeJob(currentJob);
			}
		}
		
	}
	
	@Override
	public void addJob(JobContainer prec, Job newJob) {
		if(!newJob.equals(currentJob)) { //need to update only if the newJob is different than the one that the box already has
			entranceTime = System.currentTimeMillis();
		}
		currentJob = newJob;		
		color = newJob.getColor();
		duration = newJob.getDuration();
		mapDuration = newJob.getMapDuration();
		isWorking = true;
	}
	
	/**
	 * Paint the box white, since we do not have any job to display
	 */
	public void clear() {
		duration = 0;
		mapDuration = 0;
		isWorking = false;
	}

	/**
	 * To know for how long the animator has stopped
	 * @param value the time of pausing
	 */
	public void setPauseTime(long value) {
		if(isWorking) {
			pausedTime += value;
		}	
	}

	public void setProcessorSpeed(double velocity) {
		processorSpeed = velocity;
	}
	
	public void setVelocityFactor(int value) {
		velocityFactor = value;
	}
	
	public void setPosition(Point pos) {
		sPos = pos;
	}

}
