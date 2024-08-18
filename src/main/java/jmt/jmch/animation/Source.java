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
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.geom.Arc2D;
import java.util.Random;

import javax.swing.JComponent;
import javax.swing.JPanel;

import jmt.common.exception.IncorrectDistributionParameterException;
import jmt.gui.common.JMTImageLoader;
import jmt.jmch.distributions.AnimDistribution;

/**
 * This class is responsible for creating the jobs and routing them to the first edge of the animation
 *
 * @author Lorenzo Torri
 * Date: 31-mar-2024
 * Time: 11.30
 */
public class Source extends JComponent implements JobContainer{
	private JPanel parent;
	private Point pos;
	private JobContainer next;
	private Job routeJob;
	
	private long start; //solo per debug, sarà da eliminare
	
	private Image sourceImg;
	private boolean centered = false;
	private int size = 50;
	
	private AnimationClass anim;
	private AnimDistribution interArrival;
	private AnimDistribution service;
	private double nextRandomValue = 0;

	private long pauseTime = 0; //keep track for how long the animation was paused
	private long passedTime = 0;
	private int velocityFactor = 1; //to increase the velocity of the simulation

	private int sizeCircle = 20;
	private float progression = 0.0f;

	private Color jobColor;
	
	
	/**
     * Constructor
     * @param anim, AnimationClass associated to this component. It is needed in order to remove the job arrived to the sink also from the list of jobs of the AnimationClass
	 * @param container, JPanel that contains this sink
     * @param centered, if the component is centered with respect to the Jpanel
     * @param pos, position of the component. If it is centered then this pos is not important, it can be anything
     * @param next, next JContainer linked to the source
	 * @param interArrival, distribution of the interarrival time
     */
	public Source(AnimationClass anim, JPanel container,boolean centered, Point pos, JobContainer next, AnimDistribution interArrival, AnimDistribution service) {
		this.parent = container;
		this.pos = pos;
		this.next = next;	
		this.centered = centered;
		this.anim = anim;
		this.interArrival = interArrival;
		this.service = service;
		
		jobColor = getRandomColor();
		sourceImg = JMTImageLoader.loadImageAwt("Source");
	}
	
	public void paint(Graphics g) {
		super.paint(g);
		
		if(centered) {
			//int widthPanel = parent.getWidth();
			int heightPanel = parent.getHeight();	
			pos.y = parent.getY()+(heightPanel- size)/2 ;
			pos.x = 20;
		}
		
		g.drawImage(sourceImg, pos.x, pos.y, size, size, null);

		//TODO: remove those two lines, only for debug
		//g.setFont(new Font("Arial", Font.BOLD, 13));
		//g.drawString(String.valueOf(nextRandomValue), pos.x, pos.y-20);

		//pie arrivals
		g.drawOval(pos.x + size/2 - sizeCircle/2, pos.y+size+10, sizeCircle, sizeCircle);
		g.setColor(jobColor);
		Graphics2D g2d = (Graphics2D) g.create();
		Arc2D arc = new Arc2D.Double(pos.x + size/2 - sizeCircle/2, pos.y+size+10, sizeCircle, sizeCircle, 90, 360*progression, Arc2D.PIE); //paint the circle
	    g2d.fill(arc); 
	}

	@Override
	public void refresh() {
		passedTime += (System.currentTimeMillis() - pauseTime - start)*velocityFactor;
		start = System.currentTimeMillis();
		pauseTime = 0;
		
		long nextValue = (long)nextRandomValue*1000;
		long diff = nextValue - passedTime;
		progression = (float) diff/nextValue;
		
		if(progression < 0) {
			progression = 0;
			pauseTime = 0;
			passedTime = 0;
			start = System.currentTimeMillis();

			routeJob = new Job(service, jobColor);
			jobColor = getRandomColor(); //get a new color for the next job
			routeJob(0);

			try {
				nextRandomValue = interArrival.nextRand();
			} catch (IncorrectDistributionParameterException e) {
				//this will never happen, since the parameters of the distribution of the interArrival are OK
				nextRandomValue = 64.0;
			}
			anim.addNewJob(routeJob);
		}		
	}
	
	/**
	 * Method used by the AnimationClass to set the starting timer of the animation and to set the new randomValue
	 * @param time start time of the animation
	 */
	public void setStart(long time){
		start = time;

		if(!anim.getAnimator().isPaused()) { //if it is paused, and we restart the animation, we don't have to create another arrival time
			try {
				nextRandomValue = interArrival.nextRand();
			} catch (IncorrectDistributionParameterException e) {
				//this will never happen, since the parameters of the distribution of the interArrival are OK
				nextRandomValue = 64.0;
			}
		}
	}

	/** Create a random color associated to each job */
    private Color getRandomColor() {
        Random rand = new Random();
        int r = rand.nextInt(256);
        int g = rand.nextInt(256);
        int b = rand.nextInt(256);
        return new Color(r, g, b); 
    }

	
	public void routeJob(int i) {
		if(next instanceof Edge) {
			routeJob.setOnEdge();
		}
		else {
			routeJob.unsetOnEdge();
		}
		next.addJob(this, routeJob);
	}

	@Override
	public void addJob(JobContainer prec, Job newJob) {
		
	}

	public void updatePause(long pause) {
		pauseTime += pause;
	}

	/**
	 * Update the Service time and InterArrival time distribution. Called from the AnimationClass
	 * @param service new distribution for service time
	 * @param interA new distribution for inter arrival time
	 */
	public void updateDistribution(AnimDistribution service, AnimDistribution interA){
		this.service = service;
		interArrival = interA;
	}

	public void setVelocityFactor(int value) {
		velocityFactor = value;
	}	
}
