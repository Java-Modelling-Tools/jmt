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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;

/** 
 * This class is needed to understand the direction where the edge is pointing based on the starting point and the finish point.
 * It is also useful for painting the circle of the Job on top of the edge
 */
enum Direction{
	UP{
		@Override
		public int direction() {
			return -1;
		}
	},
	DOWN{
		@Override
		public int direction() {
			return 1;
		}
	},
	LEFT{
		@Override
		public int direction() {
			return -1;
		}
	},
	RIGHT{
		@Override
		public int direction() {
			return 1;
		}
	};
	
	//method for understanding the direction 1 or -1
	public abstract int direction();
}

/**
 * Class for an edge in the animation. 
 * Edges can only be straight from a start point to a finish point, so be careful when creating them, they must have either start.x = finish.x or start.y = finish.y
 * When a job arrives to the finish point then it is routed to the next element associated to this edge.
 *
 * @author Lorenzo Torri
 * Date: 26-mar-2024
 * Time: 13.40
 */
public class Edge extends JComponent implements JobContainer{
	private JPanel parent;
	private AnimationClass animation;
	
	//--variables of the edge
	private boolean centered;
	private boolean isArrow;
	private Point[] points;
	private double speed = 2; //by default the speed of the jobs over the edge is = 2, but you can change this value (in Routing the central edge has speed << than other edges)
	
	private List<Job> jobList;
	private JobContainer nextContainer = null;
	
	//for the probabilistic routing
	private boolean paintPercentage = false;
	private double percentage = 0.00;

	//for the nextEvent simulation
	private boolean nextEvent = false;
	private int velocityFactor = 1;

	//for the highlight
	private int counterHighLight;
	private Timer timer;
	private Color jobColor;
	private boolean highlight = false;
	
	/**
	 * Constructor
	 * @param animation, AnimationClass which the edge is part of
	 * @param container, JPanel that contains this edge
	 * @param centered, if the edge is y centered with respect to the JPanel or not
	 * @param isArrow, if the edge is an arrow and not a simple line
	 * @param points, points of the edge
	 * @param speed, the speed of the jobs running over this edge
	 * @param next, JobContainer next to this edge
	 */
	public Edge(AnimationClass anim, JPanel container, boolean centered, boolean isArrow, Point[] points, double speed, JobContainer next) {
		this.animation = anim;
		this.parent = container;
		this.centered = centered;
		this.isArrow = isArrow;
		this.points = points;
		this.nextContainer = next;
		this.speed = speed;
		
		jobList = new ArrayList<>();
	}
	
	public void paint(Graphics g) {
		super.paint(g);
		
		Graphics2D g2d = (Graphics2D) g;
			
		//create a connection for each couple of adjacent points
		for(int i = 0; i < points.length - 1; i++) {
			Point start = points[i];
			Point finish = points[i+1];
			
			if(centered) {
				int heightPanel = parent.getHeight();	
				start.y = parent.getY()+(heightPanel)/2;
				finish.y = start.y;
			}
			
			Color chosenColor = Color.BLACK;
			if(highlight) {
				chosenColor = jobColor;
				g2d.setStroke(new BasicStroke(4));
				g2d.setColor(chosenColor);
				g2d.drawLine(start.x, start.y, finish.x, finish.y);	
				g2d.setStroke(new BasicStroke(1));
			}
			else {
				g.setColor(chosenColor);
				g.drawLine(start.x, start.y, finish.x, finish.y);	
			}	
		}	
		
		//only if the routing policy with probability is chosen
		if(paintPercentage) {
			g.setFont(new Font("Arial", Font.PLAIN, 10));
			DecimalFormat df = new DecimalFormat("#.##");
			String value = "prob: "+ df.format(percentage);
			Direction direction = getDirection(points[0], points[1]);
			if(direction == Direction.UP || direction == Direction.DOWN) {
				g.drawString(value, points[0].x - 50,  points[0].y + direction.direction() * 20);
			}
			else {
				g.drawString(value, points[0].x,  points[0].y + direction.direction() * 20);
			}
		}

		//only if the edge is an arrow (for now the arrow is only for a direction = right)
		if(isArrow) {
			int[] xPoints = {points[points.length-1].x+5, points[points.length-1].x, points[points.length-1].x}; // x coordinates
		    int[] yPoints = {points[points.length-1].y, points[points.length-1].y-5, points[points.length-1].y+5};   // y coordinates

	        g2d.fillPolygon(xPoints, yPoints, 3); //draw the triangle
		}
	}
	
	/**
	 * Method to paint the percentage near the edge. 
	 * This method is useful for Routing Probabilistic policy
	 * @param p
	 */
	public void paintPercentage(double p) {
		paintPercentage = true;
		percentage = p;
	}
	
	/** Method to understand the direction of the segment defined by two points */
	public Direction getDirection(Point start, Point finish) {
		if(start.x == finish.x) { //two vertical points
			if(start.y > finish.y) {
				return Direction.UP;
			}
			else {
				return Direction.DOWN;
			}
		}
		else { //two horizontal points
			if(start.x > finish.x) {
				return Direction.LEFT;
			}
			else {
				return Direction.RIGHT;
			}
		}
	}

	@Override
	public void refresh() {
		//check for each job if it is still in the edge, if not route it to the next element, otherwise update its position
		for(int i = 0; i < jobList.size(); i++) {
			Job j = jobList.get(i);
			if(checkFinish(j, j.getFinalPos())) {
				if(j.getFinalPos() == points.length - 1) { //job travelled along all the edges 
					routeJob(i);
				}
				else {
					j.setStartingPosition(points[j.getFinalPos()].x, points[j.getFinalPos()].y, getDirection(points[j.getFinalPos()], points[j.getFinalPos()+1]), j.getFinalPos() + 1); 
				}			
			}
			else {
				moveJob(j);
			}			
		}		
	}
	
	/**
	 * Method to update the current position of a job based on the direction of the edge
	 * @param j Job that has to update its position inside the edge
	 */
	private void moveJob(Job j) {
		Point current = j.getPosition();
		switch(j.getDirection()) {
			case UP: 
				j.updatePosition(current.x, current.y - speed);
				break;
			case DOWN:
				j.updatePosition(current.x, current.y + speed);
				break;
			case RIGHT:
				j.updatePosition(current.x + speed, current.y);
				break;
			case LEFT:	
				j.updatePosition(current.x - speed, current.y);
				break;
		}
	}
	
	/**
	 * Check whether the Job has completed its path along the edge.
	 * The check is performed based on the direction of the edge and on the position of the job
	 * @param j Job to check
	 * @param finalPos index of Points (the arrival point for the job moving in the segment)
	 * @return boolean, true if the job has reached the finish point, false otherwise
	 */
	private boolean checkFinish(Job j, int finalPos) {
		Point current = j.getPosition();
		Point finish = points[finalPos];
		//add to current half of the circleSize, in order to consider the center of the circle as the position of the job, and not the start drawing point
		int circleSize = j.getCircleSize();
		switch(j.getDirection()) {
			case LEFT:
				if(current.x + circleSize/2 <= finish.x) {
					return true;
				}
				break;
			case RIGHT:
				if(current.x + circleSize/2 >= finish.x) {
					return true;
				}
				break;
			case UP:
				if(current.y + circleSize/2 <= finish.y) {
					return true;
				}
				break;
			case DOWN:
				if(current.y + circleSize/2 >= finish.y) {
					return true;
				}
				break;
		}
		return false;
	}
	
	@Override
	public void addJob(JobContainer prec, Job newJob) {
		newJob.setStartingPosition(points[0].x, points[0].y, getDirection(points[0], points[1]), 1); //when a new Job is added, then its position is the starting point of the edge
		jobList.add(newJob);

		if(nextEvent) { //do not stop for the next event if a job is passing from an edge to another
			animation.pause();
			animation.resetNextEvent();
		}
	}

	/**
	 * Route a job to the next JobContainer since it has arrived to the final point of the edge
	 * @param i, index of the job inside the JobList of the edge
	 */
	public void routeJob(int i) {
		Job job = jobList.remove(i);
		if(nextContainer != null) {
			if(nextContainer instanceof Edge) {
				job.setOnEdge();
			}
			else {
				job.unsetOnEdge();
			}
			nextContainer.addJob(this, job);	
		}
		else {
			job.unsetOnEdge();			
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

	/* Methods for the Next Step */
	public void setVelocityFactor(int value) {
    	speed *= value;
    	velocityFactor = value;
    }

	public void resetVelocityFactor() {
    	speed /= velocityFactor;
    	velocityFactor = 1;
    }

	/* Methods for highlight */
	public void highlightON(Color jobColor) {
		counterHighLight = 0;
		if(timer != null) {
			timer.stop();
		}
		timer = new Timer(300, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                highlight();
			}
        });
		
		this.jobColor = jobColor;
		timer.start();
	}
	
	
	public void highlight() {
		if(counterHighLight >= 3) {
			timer.stop();
			counterHighLight = 0;
			highlight = false;
		}
		else {
			highlight = !highlight;
			counterHighLight++;
		}
		
	}
}
