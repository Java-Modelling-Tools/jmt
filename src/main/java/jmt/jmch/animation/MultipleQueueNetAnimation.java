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
import java.util.Arrays;
import java.util.List;

import javax.swing.JPanel;

import jmt.jmch.simulation.Simulation;
import jmt.jmch.distributions.AnimDistribution;
import jmt.jmch.wizard.panels.AnimationPanel;

/**
 * Class with all the JComponents for the Animation of a Routing Policy
 *
 * @author Lorenzo Torri
 * Date: 02-apr-2024
 * Time: 09.10
 */
public class MultipleQueueNetAnimation extends AnimationClass{
	private AnimationPanel animPanel;
	private JPanel parent;

	//-- characteristics of the main panel
	private int panelWidth = 0;
	private int panelHeight = 0;
	
	//-- all the classes contained in the Animation, like stations, edges...
	private Source source;
	private Router router;
	private Sink sink;
	private List<Station> stationList;
	private List<Edge> edgeList;
	private double[] probabilities = {0.00,0.00,0.00};
	
	
	//-- all the characteristics of the Animation
	private int nServers = 1; //by default = 1
	private AnimDistribution interArrival;
	private AnimDistribution service;
	
	/** Constructor */
	public MultipleQueueNetAnimation(AnimationPanel animPanel, JPanel container, Simulation sim) {
		this.parent = container;
		this.animPanel = animPanel;
		simulation = sim;	
		initGUI(container);
	}
	
	public void initGUI(JPanel container) {	
		anim = new Animator(30, this);	
		edgeList = new ArrayList<>();
		stationList = new ArrayList<>();
		sink = new Sink(container, true, new Point(800,0), this);
		jobList = new ArrayList<>(); //do not move this line in the super class
		
		//final horizontal edge
		edgeList.add(new Edge(this,container, true, true, new Point[] {new Point(550,0), new Point(580,0)}, sink));
		
		//3 edges from stations to final edge
		edgeList.add(new Edge(this,container, false, false, new Point[]{ new Point(500,75+30), new Point(550,75+30), new Point(550,235)}, edgeList.get(0)));
		edgeList.add(new Edge(this, container, true, false, new Point[] {new Point(500,0), new Point(550,0)}, edgeList.get(0)));
		edgeList.add(new Edge(this, container, false, false, new Point[] { new Point(500,340+30), new Point(550,340+30), new Point(550,235)}, edgeList.get(0)));
		
		//three stations
		stationList.add(new Station(this, container, false, false, new Point(300,75), edgeList.get(1), simulation, nServers));
		stationList.add(new Station(this, container, false, true, new Point(300,0), edgeList.get(2), simulation, nServers));
		stationList.add(new Station(this, container, false, false, new Point(300, 340), edgeList.get(3), simulation, nServers));
		
		//3 edges from router to stations
		edgeList.add(new Edge(this, container, false, true, new Point[] {new Point(180,235 - 30), new Point(180, 75+30), new Point(280,75+30)}, stationList.get(0)));
		edgeList.add(new Edge(this, container, true, true, new Point[] {new Point(210,0), new Point(280,0)}, stationList.get(1)));
		edgeList.add(new Edge(this, container, false, true, new Point[] { new Point(180,235 + 30), new Point(180, 340+30), new Point(280,340+30)}, stationList.get(2)));
				
		//create the two Lists for the router
		int l = edgeList.size();
		List<Edge> eList = new ArrayList<>(Arrays.asList(edgeList.get(l-3), edgeList.get(l-2), edgeList.get(l-1)));
		router = new Router(container, false, true, new Point(160,0), eList, stationList, simulation, probabilities);
		
		edgeList.add(new Edge(this, container, true, true, new Point[] {new Point(80,0), new Point(150,0)}, router));
		
		source = new Source(this, container, true, new Point(10,0), edgeList.get(edgeList.size()-1), interArrival, service);

		panelHeight = 0;
		panelWidth = 0;
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		
		source.paint(g);
		sink.paint(g);

		int currentPanelHeight = parent.getHeight();
		int currentPanelWidth = parent.getWidth();

		if(currentPanelHeight != panelHeight || currentPanelWidth != panelWidth){ //the window has changed size, or it's the first time the paint method is called
			//this part is used to support dynamic changes of the size of the window (and also for different screen resolutions)
			int quarterPanel = parent.getHeight() / 4;
			stationList.get(0).setYPos(parent.getY()+ quarterPanel - stationList.get(0).getHeight()/2 - 20);
			stationList.get(2).setYPos(parent.getY()+ quarterPanel*3 - stationList.get(0).getHeight()/2 + 20);
			
			for(Station st: stationList){
				st.setXPos(parent.getWidth() * 19/50);
				st.paint(g);
			}

			router.setXPos(parent.getWidth() * 11/50);
			router.paint(g);
		
			updateEdgesPosition();
			updateVelocityEdges();

			panelHeight = currentPanelHeight;
			panelWidth = currentPanelWidth;
		}
		else{
			for(Station st: stationList){
				st.paint(g);
			}
			router.paint(g);
		}	
		
		for(Edge e: edgeList) {
			e.paint(g);
		}
		for(Job j: jobList) {
			j.paint(g);
		}
	}

	/** Update the velocity of the three edges from the router to the stations */
	private void updateVelocityEdges(){
		int lFirstEdge = edgeList.get(4).getTotalLength();
		int lSecondEdge = edgeList.get(5).getTotalLength();
		double sFirstEdge = edgeList.get(4).getSpeed();

		double time = lFirstEdge / sFirstEdge; //time must be equal for all the edges

		double sSecondEdge = lSecondEdge / time;
		edgeList.get(5).setSpeed(sSecondEdge);
	}

	/** Update the position of the vertices of the edges based on the size of the window */
	private void updateEdgesPosition(){
		//update the vertices of the edges dinamically
		int padding = 10;
		edgeList.get(0).setXFinish(sink.getXPos() - padding);
		int stationFinishX = (stationList.get(0).getXPos() + stationList.get(0).getTotalLenght());
		int halfStationSink = (sink.getXPos() - stationFinishX)/2 + stationFinishX;
		edgeList.get(0).setXStart(halfStationSink); //half the distance between the finish of the station and the begin of the sink

		edgeList.get(1).setXStart(stationFinishX + padding);
		edgeList.get(1).setXPoint(halfStationSink, 1);
		edgeList.get(1).setXFinish(halfStationSink); 
		edgeList.get(1).setYStart(stationList.get(0).getYPos() + stationList.get(0).getHeight()/2);
		edgeList.get(1).setYPoint(stationList.get(0).getYPos() + stationList.get(0).getHeight()/2, 1);
		edgeList.get(1).setYFinish(parent.getY() + parent.getHeight()/2);

		edgeList.get(2).setXStart(stationList.get(1).getXPos() + stationList.get(1).getTotalLenght() + padding);
		edgeList.get(2).setXFinish(halfStationSink);

		edgeList.get(3).setXStart(stationFinishX + padding);
		edgeList.get(3).setXPoint(halfStationSink, 1);
		edgeList.get(3).setXFinish(halfStationSink); 
		edgeList.get(3).setYStart(stationList.get(2).getYPos() + stationList.get(2).getHeight()/2);
		edgeList.get(3).setYPoint(stationList.get(2).getYPos() + stationList.get(2).getHeight()/2, 1);
		edgeList.get(3).setYFinish(parent.getY() + parent.getHeight()/2);

		edgeList.get(4).setXStart(router.getXPos() + router.getTotalLenght()/2);
		edgeList.get(4).setXPoint(router.getXPos() + router.getTotalLenght()/2, 1);
		edgeList.get(4).setXFinish(stationList.get(0).getXPos() - padding); 
		edgeList.get(4).setYStart(router.getYPos() - padding);
		edgeList.get(4).setYPoint(stationList.get(0).getYPos() + stationList.get(0).getHeight()/2, 1);
		edgeList.get(4).setYFinish(stationList.get(0).getYPos() + stationList.get(0).getHeight()/2);

		edgeList.get(5).setXStart(router.getXPos() + router.getTotalLenght() + padding);
		edgeList.get(5).setXFinish(stationList.get(1).getXPos() - padding);

		edgeList.get(6).setXStart(router.getXPos() + router.getTotalLenght()/2);
		edgeList.get(6).setXPoint(router.getXPos() + router.getTotalLenght()/2, 1);
		edgeList.get(6).setXFinish(stationList.get(0).getXPos() - padding); 
		edgeList.get(6).setYStart(router.getYPos() + router.getTotalLenght() + padding);
		edgeList.get(6).setYPoint(stationList.get(2).getYPos() + stationList.get(2).getHeight()/2, 1);
		edgeList.get(6).setYFinish(stationList.get(2).getYPos() + stationList.get(2).getHeight()/2);

		edgeList.get(7).setXStart(source.getXPos() + source.getTotalLenght() + padding);
		edgeList.get(7).setXFinish(router.getXPos() - padding);
	}

	@Override
	public void refresh() {
		source.refresh();
		router.refresh();
		
		for(Station st: stationList) {
			st.refresh();
		}
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
	public void updatePause(long pause) {
		//update all those elements that work with System.currentMillis() like CircleStation
		for(Station st: stationList) {
			st.updatePause(pause);
		}
		source.updatePause(pause);
	}

	
	@Override
	public void stop(){
		anim.terminate();
		animPanel.stopAnimation();
	}

	@Override
	public void setVelocityFactor(int factor){
		source.setVelocityFactor(factor);
		for(Station st: stationList){
			st.setVelocityFactor(factor);
		}
		for(Job j: jobList) {
			j.setVelocityFactor(factor);
		}
		for(Edge e: edgeList){
			e.setVelocityFactor(factor);
		}
	}

	@Override
	public void next() {
		//first update the velocity of each component that works with time
		setVelocityFactor(5);
		
		anim.start();
		
		for(Edge e: edgeList) {
			e.nextEvent();
		}
		for(Station st: stationList){
			st.nextEvent();
		}	
	}

	public void resetNextEvent() {
		//reset all the velocity factors
		setVelocityFactor(1);
		
		for(Edge e: edgeList) {
			e.resetNextEvent();
		}
		for(Station st: stationList){
			st.resetNextEvent();
		}

		//reset the buttons of the animationPanel
		animPanel.resetNextStepAnimation();
	} 

	@Override
	public void updateMultiple(Simulation sim, AnimDistribution service, AnimDistribution interA){
		simulation = sim;
		interArrival = interA;
		this.service = service;

		source.updateDistribution(service, interA);	
	}

	@Override
	public void updateMultiple(Simulation sim, double[] percentages, AnimDistribution service, AnimDistribution interA){
		updateMultiple(sim, service, interA);
		probabilities[0] = percentages[0];
		probabilities[1] = percentages[1];
		probabilities[2] = 1.00 - percentages[0] - percentages[1];
		router.changeProbabilities(probabilities);
	}

	@Override
	public Source getSource() {
		return source;
	}
}
