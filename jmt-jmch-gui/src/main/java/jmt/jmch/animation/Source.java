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

import javax.swing.JComponent;
import javax.swing.JPanel;

import jmt.common.exception.IncorrectDistributionParameterException;
import random.Uniform;
import random.engine.MersenneTwister;
import random.engine.RandomEngine;
import random.UniformPar;
import random.Parameter;
import common.JMTImageLoader;
import jmt.jmch.distributions.DistributionJMCH;

/**
 * This class is responsible for creating the jobs and routing them to the first edge of the animation
 *
 * @author Lorenzo Torri
 * Date: 31-mar-2024
 * Time: 11.30
 */
public class Source extends JComponent implements JobContainer, GraphicComponent{
	private JPanel parent;
	private Point pos;
	private JobContainer next;
	private Job newJob;
	
	private long start; //solo per debug, sarà da eliminare
	
	private Image sourceImg;
	private boolean centered = false;
	private int size = 50;
	
	private AnimationClass anim;
	private DistributionJMCH interArrival;
	private DistributionJMCH service;
	private double interArrivalTime = 0;

	private long pauseTime = 0; //keep track for how long the animation was paused
	private long passedTime = 0;
	private int velocityFactor = 1; //to increase the velocity of the simulation

	private int sizeCircle = 20;
	private float progression = 0.0f;

	private int maxJobs = 10;
	private int generatedJobs = 0;
	private Image doneImg;
	private Color jobColor;
	private UniformD uniform;
	
	/**
     * Constructor
     * @param anim, AnimationClass associated to this component. It is needed in order to remove the job arrived to the sink also from the list of jobs of the AnimationClass
	 * @param container, JPanel that contains this sink
     * @param centered, if the component is centered with respect to the Jpanel
     * @param pos, position of the component. If it is centered then this pos is not important, it can be anything
     * @param next, next JContainer linked to the source
	 * @param interArrival, distribution of the interarrival time
     */
	public Source(AnimationClass anim, JPanel container,boolean centered, Point pos, JobContainer next, DistributionJMCH interArrival, DistributionJMCH service) {
		this.parent = container;
		this.pos = pos;
		this.next = next;	
		this.centered = centered;
		this.anim = anim;
		this.interArrival = interArrival;
		this.service = service;
		
		uniform = new UniformD();
		jobColor = getRandomColor();
		sourceImg = JMTImageLoader.loadImageAwt("Source");	
		doneImg = JMTImageLoader.loadImageAwt("Measure_ok");
		generatedJobs = 0;
	}

	public void paint(Graphics g) {
		super.paint(g);

		if(maxJobs <= 0){
			g.drawImage(doneImg, pos.x + size + 10, pos.y - 10, size/2, size/2, null);
		}
		
		if(centered) {
			//int widthPanel = parent.getWidth();
			int heightPanel = parent.getHeight();	
			pos.y = parent.getY()+(heightPanel- size)/2 ;
			pos.x = 20;
		}
		
		g.drawImage(sourceImg, pos.x, pos.y, size, size, null);

		//g.setFont(new Font("Arial", Font.BOLD, 13));
		//g.drawString(String.valueOf(interArrivalTime), pos.x, pos.y-20);

		//pie arrivals
		g.drawOval(pos.x + size/2 - sizeCircle/2, pos.y+size+10, sizeCircle, sizeCircle);
		g.setColor(jobColor);
		Graphics2D g2d = (Graphics2D) g.create();
		Arc2D arc = new Arc2D.Double(pos.x + size/2 - sizeCircle/2, pos.y+size+10, sizeCircle, sizeCircle, 90, 360*progression, Arc2D.PIE); //paint the circle
	    g2d.fill(arc); 

		//generated jobs
		g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.drawString("Generated Jobs: "+String.valueOf(generatedJobs), pos.x,  pos.y - size/2);
	}

	@Override
	public void refresh() {
		this.velocityFactor = anim.getVelocityFactor();
		
		if(maxJobs > 0){
			passedTime += (System.currentTimeMillis() - pauseTime - start)*velocityFactor;
			start = System.currentTimeMillis();
			pauseTime = 0;
			
			long nextValue = Math.round(interArrivalTime * 1000);
			long diff = nextValue - passedTime;
			progression = (float) diff/nextValue;
			
			if(progression < 0) {
				maxJobs--;
				generatedJobs++;

				progression = 0;
				pauseTime = 0;
				passedTime = 0;
				start = System.currentTimeMillis();

				double duration = 0.0;
				try {
					duration = service.nextRand();
				} catch (IncorrectDistributionParameterException e) {
					//this will never happen, since the parameters of the distribution of the service are OK
					duration = 64.0;
				}

				newJob = new Job(generatedJobs, anim, service, interArrivalTime, duration, jobColor);
				jobColor = getRandomColor(); //get a new color for the next job
				routeJob(0);

				try {
					interArrivalTime = interArrival.nextRand();
				} catch (IncorrectDistributionParameterException e) {
					//this will never happen, since the parameters of the distribution of the interArrival are OK
					interArrivalTime = 64.0;
				}
		
				anim.addNewJob(newJob);
			}
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
				interArrivalTime = interArrival.nextRand();
			} catch (IncorrectDistributionParameterException e) {
				//this will never happen, since the parameters of the distribution of the interArrival are OK
				interArrivalTime = 64.0;
			}
		}
	}

	/** Create a random color associated to each job */
    private Color getRandomColor() {
        int r = 0, g = 0, b = 0;
		try {
			r = (int) Math.round(uniform.nextRand());
			g = (int) Math.round(uniform.nextRand());
			b = (int) Math.round(uniform.nextRand());
		} catch (IncorrectDistributionParameterException e) {
			e.printStackTrace();
		}
  
        return new Color(r, g, b); 
    }

	
	public void routeJob(int i) {
		if(next instanceof Edge) {
			newJob.setOnEdge();
		}
		else {
			newJob.unsetOnEdge();
		}
		next.addJob(this, newJob);
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
	public void updateDistribution(DistributionJMCH service, DistributionJMCH interA){
		this.service = service;
		interArrival = interA;
	}

	public void updateMaxJobs(int maxJobs){
		this.maxJobs = maxJobs;
	}

	public void updateNextContainer(JobContainer next){
		this.next = next;
	}
	
	public int getMaxJobs(){
		return maxJobs;
	}

	public int getGeneratedJobs(){
		return generatedJobs;
	}
	
	//----- Graphic component Methods
	@Override
	public int getXPos() {
		return pos.x;
	}

	@Override
	public int getYPos() {
		return pos.y;
	}

	@Override
	public int getTotalLenght() {
		return size;
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

/**
 * This class is used for obtaining random colors. It is separated from the AnimDistributions, because it must have a different engine
 */
class UniformD{
    protected double mean;
    protected double lambda;
    protected double[] percentiles = new double[2];
    protected RandomEngine engine;

	public UniformD(){
		engine = new MersenneTwister(23000);
		setMean(127.5);
	}


    public double nextRand() throws IncorrectDistributionParameterException {
        Parameter par = new UniformPar(0, 255); //if max > min or the mean is < 0 Exception
        Uniform distribution = new Uniform();
        distribution.setRandomEngine(engine);
        return distribution.nextRand(par); 
    }


    public void setMean(double value) {
        lambda = value;
        mean = 1/value;
        setPercentiles();
    }


    protected void setPercentiles() {
        double a = 0;
        double b = 255;
        percentiles[0] = a + 0.10 * (b-a);
        percentiles[1] = a + 0.90 * (b-a);
    } 
}
