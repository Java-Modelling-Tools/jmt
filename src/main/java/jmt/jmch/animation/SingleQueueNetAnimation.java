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

import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import jmt.jmch.simulation.Simulation;
import jmt.jmch.distributions.AnimDistribution;
import jmt.jmch.wizard.panels.AnimationPanel;

/**
 * Class with all the JComponents for the Animation of a Scheduling Policy
 *
 * @author Lorenzo Torri
 * Date: 25-mar-2024
 * Time: 10.46
 */
public class SingleQueueNetAnimation extends AnimationClass{
	private AnimationPanel animPanel;
	private JPanel parent;
	
	//-- all the classes contained in the Animation, like stations, edges...
	private Source source;
	private Sink sink;
	private Station station;
	private List<Edge> edgeList;
	
	//--all the characteristics of the Animation
	private int nServers = 1;
	private AnimDistribution interArrival; //by default the two distributions are DETERMINSTIC
	private AnimDistribution service;
	
	
	/** Constructor*/
	public SingleQueueNetAnimation(AnimationPanel animPanel, JPanel container, Simulation sim) {		
		super();
		this.animPanel = animPanel;
		this.parent = container;
		simulation = sim;
		initGUI(container);
	}
	
	public void initGUI(JPanel container) {
		//Define the elements of the Animation from the last to the first, since each element must have a reference to the next one
		edgeList = new ArrayList<>();
		anim = new Animator(30, this);
		jobList = new ArrayList<>(); //do not move in the super class, since each time I have to reload the simulation, this method is called
		
		sink = new Sink(container, true, new Point(0,0), this);
		edgeList.add(new Edge(this, container, true, true, new Point[] {new Point(450,0), new Point(590,0)}, 2, sink));
		station = new Station(this, container, true, true, new Point(0,0), edgeList.get(0), simulation, nServers);
		edgeList.add(new Edge(this, container, true, true, new Point[] {new Point(80,0), new Point(230,0)}, 2, station));
		source = new Source(this, container, true, new Point(0,0), edgeList.get(edgeList.size()-1), interArrival, service);
	}
	
	/**
	 * This is the most important method for repainting.
	 * This one is the only one paint that needs to be called when repaint is invoked.
	 * Do not call repaint in other classes related to QNAnimation, otherwise it will not work, since in paint, there is a call to the paint methods of all the classes related to QNA
	 */
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		
		source.paint(g);
		station.paint(g);
		sink.paint(g);
		
		for(Edge e: edgeList) {
			e.paint(g);
		}
		for(Job j: jobList) {
			j.paint(g);
		}
	}
	
	@Override
	public void refresh() {		
		source.refresh();
		station.refresh();
		
		for(Edge e: edgeList) {
			e.refresh();
		}

		repaint();
	}
	
	@Override
	public void start() {
		if(!anim.isPaused()) { //if it's the first time the animation is started, otherwise it is a simple restart after a pause
			source.setStart(System.currentTimeMillis());			
		}
		anim.start();
	}
	
	@Override
	public void pause() {
		anim.pause();
	}
	
	@Override
	public void reload() {
		initGUI(parent);
		repaint();
	}

	@Override
	public void stop(){
		anim.terminate();
		animPanel.stopAnimation();
	}

	@Override
	public void updatePause(long pause) {
		//update all those elements that work with System.currentMillis()
		station.updatePause(pause);
		source.updatePause(pause);
	}

	@Override
	public void next() {
		//first update the velocity of each component that works with time
		source.setVelocityFactor(5);
		station.setVelocityFactor(5);
		for(Job j: jobList) {
			j.setVelocityFactor(5);
		}
		for(Edge e: edgeList){
			e.setVelocityFactor(5);
		}
		
		anim.start();
		
		for(Edge e: edgeList) {
			e.nextEvent();
		}
		station.nextEvent();
	}

	public void resetNextEvent() {
		//reset all the velocity factors
		source.setVelocityFactor(1);
		station.setVelocityFactor(0); //in stations is = 0, since there is also the processor speed
		for(Job j: jobList) {
			j.resetVelocityFactor();
		}
		for(Edge e: edgeList){
			e.resetVelocityFactor();
		}
		
		for(Edge e: edgeList) {
			e.resetNextEvent();
		}
		station.resetNextEvent();

		//reset the buttons of the animationPanel
		animPanel.resetNextStepAnimation();
	}

	@Override
	public void updateSingle(Simulation sim, int nservers, AnimDistribution service, AnimDistribution interA){
		simulation = sim;
		interArrival = interA;
		this.service = service;
		this.nServers = nservers;

		station.typeOfQueue(simulation);
		station.updateNServers(nservers);
		source.updateDistribution(service, interA);	
	}

}
