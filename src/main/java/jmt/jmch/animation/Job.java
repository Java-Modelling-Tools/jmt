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
import java.util.Random;

import javax.swing.JComponent;

import jmt.common.exception.IncorrectDistributionParameterException;
import jmt.jmch.distributions.AnimDistribution;

/**
 * Class for a job in the animation
 *
 * @author Lorenzo Torri
 * Date: 26-mar-2024
 * Time: 13.50
 */
public class Job extends JComponent{
	//--variables needed only for the movement along the edges (speed is a value of the edge, since jobs can have different speeds based on the edge on which they are)
	private Point pos;	
	private boolean onEdge = false; //this parameter is used to know if the current job is on a Edge or not
	private Direction direction; //use this to know the direction where the job is going
	private int finalPos; //to know the position of the next point the job is going to 
	private double fractX = 0.0;
	private double fractY = 0.0;
	
	private int circleSize = 15;
	private int boxWidth = 12;
	private int boxHeight = 30;
	
	private double duration;
	private double mapDuration;
	private Color color;
	private int priority;
	
	//those values are only for debug now
	public int maxValue = 10; //this value is for the conversion from the duration to a colored box
	public AnimDistribution service;
	
	private long entrance; //this value is the associated long value for the entrance of a job inside a JComponent
	private int velocityFactor = 1;
	
	public Job(AnimDistribution service, double duration, Color color) {	
		this.color = color;
		priority = new Random().nextInt(5)+1; //set priority always > 1 otherwise it does not work properly in the BoxStation
		this.service = service;
		this.duration = duration;
		mapDuration = service.mapValue(duration); //based on the type of distribution, map the duration to a value between 0 and 1
	}
	
	/** This paint needs to know if we are on an edge or not, because if we are not on an edge, then we do not need to paint anything */
	public void paint(Graphics g) {
		super.paint(g);
		
		if(onEdge) {
			g.setColor(color);
			g.fillOval(pos.x, pos.y, circleSize, circleSize);		
			
			//then draw also the box with the filled rectangle
			g.setColor(Color.BLACK);
			g.drawRect(pos.x + 15, pos.y - 30, boxWidth, boxHeight);

			//g.setFont(new Font("Arial", Font.BOLD, 13));
			//g.drawString(String.valueOf(mapDuration), pos.x - 15, pos.y-40);
			
			//to convert the duration to the size of the above box
			g.setColor(color);
			int result = (int) Math.round(mapDuration == 1.0 ? boxHeight-1: mapDuration * boxHeight); 
			g.fillRect(pos.x + 16, pos.y - 30 + (boxHeight-result), boxWidth-1, result);
		}	
	}
    
    public void setEntrance() {
    	entrance = System.currentTimeMillis()*velocityFactor;
    }
    
    public long getEntrance() {
    	return entrance;
    }
    
    public int getPriority() {
    	return priority;
    }
    
    public Color getColor() {
    	return color;
    }
    
    public double getDuration() {
    	return duration;
    }

	public double getMapDuration(){
		return mapDuration;
	}
    
    /**
     * Method to set the staring point position of a job. It is called by an edge that have this new Job on its route.
     * @param x x position of the Job
     * @param y y position of the Job
	 * @param d direction of the movement
	 * @param finalPos index of the array of edges of the next point 
     */
    public void setStartingPosition(int x, int y,  Direction d, int finalPos) {
    	pos = new Point(x - circleSize/2, y - circleSize/2); //remove circleSize/2 to adapt the position, since g.fillOval always draws considering the point as the left up corner of the square that contains the circle
		this.direction = d;
    	this.finalPos = finalPos;
	}
    
    /**
     * Method to update the position of a moving job.
     * It is different than the setStartingPoisition, since here we do not have to remove the circleSize/2 part
     * @param x x position of the Job
     * @param y y position of the Job
     */
    public void updatePosition(double x, double y) {
		/* All the methods for the graphical part like fillOval() use coordinate space with integer precision.
		 * Here instead we have them as double values (because the edge's speed has sometime a decimal part).
		 * The idea is to accumulate the decimal part and add +1 only when it is >= 1
		 */
		fractX += (x - Math.floor(x));
		fractY += (y - Math.floor(y));
		int intX = (int) Math.floor(x);
		int intY = (int) Math.floor(y);

		if(fractX >= 1){
			fractX = fractX - 1;
			intX += 1;
		}
		if(fractY >= 1){
			fractY = fractY -1;
			intY += 1;
		}

    	pos = new Point(intX, intY);
    }
    
    public Point getPosition() {
    	return pos;
    }
        
    public int getCircleSize() {
    	return circleSize;
    }
    
    /** Called once the Job is going outside of a station */
    public void setDuration() {
    	duration = 0;
		mapDuration = 0;
    }

	public Direction getDirection(){
		return direction;
	}

	public int getFinalPos(){
		return finalPos;
	}

	/** Called by the BoxStation in PS, to update the new duration of the job */
	public void setDuration(double value) {
    	duration = value;
		mapDuration = service.mapValue(duration);
    }
    
    public void setOnEdge() {
    	onEdge = true;
    }
    
    public void unsetOnEdge() {
    	onEdge = false;
    }

	/* Next step methods */
	public void setVelocityFactor(int value) {
    	velocityFactor = value;
    }
}
