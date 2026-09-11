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
import java.util.HashSet;
import java.util.List;

import javax.swing.JPanel;

import jmt.jmch.distributions.DistributionJMCH;
import jmt.jmch.simulation.Simulation;
import jmt.jmch.wizard.panels.AnimationPanel;

/**
 * Class with all the JComponents for the Animation of a Routing Policy
 *
 * @author Lorenzo Torri
 * Date: 02-apr-2024
 * Time: 09.10
 */
public class MultipleQueueNetAnimation extends AnimationClass{
	//-- characteristics of the main panel
	private int panelWidth = 0;
	private int panelHeight = 0;
	
	//-- all the classes contained in the Animation, like stations, edges...
	private Router router;
	private double[] probabilities = {0.00,0.00,0.00};
	
	//-- all the characteristics of the Animation
	private int nServers = 1; //by default = 1
	private DistributionJMCH interArrival;
	private DistributionJMCH service;

	//-- components that are possibly removed (three edges from router to stations, and the edge from source to router)
	private List<Edge> routerEdges;
	private List<JobContainer> stationsContainer;
	private List<JobContainer> edgesContainer;
	private Edge e4;
	
	/** Constructor */
	public MultipleQueueNetAnimation(AnimationPanel animPanel, JPanel container, Simulation sim) {
		super(animPanel, container, sim, 3);
			
		initGUI(container);
	}
	
	@Override
	public void initGUI(JPanel container) {		
		edgeList = new ArrayList<>();
		stationList = new ArrayList<>();
		sink = new Sink(container, true, new Point(800,0), this);
		jobSet = new HashSet<>();
		
		//final horizontal edge
		edgeList.add(new Edge(this,container, true, true, new Point[] {new Point(550,0), new Point(580,0)}, sink));
		
		//3 edges from stations to final edge
		edgeList.add(new Edge(this,container, false, false, new Point[]{ new Point(500,75+30), new Point(550,75+30), new Point(550,235)}, edgeList.get(0)));
		edgeList.add(new Edge(this, container, true, false, new Point[] {new Point(500,0), new Point(550,0)}, edgeList.get(0)));
		edgeList.add(new Edge(this, container, false, false, new Point[] { new Point(500,340+30), new Point(550,340+30), new Point(550,235)}, edgeList.get(0)));
		
		//three stations
		stationList.add(new Station(this, container, false, false, new Point(300,75), edgeList.get(1), simulation, nServers, 0));
		stationList.add(new Station(this, container, false, true, new Point(300,0), edgeList.get(2), simulation, nServers, 1));
		stationList.add(new Station(this, container, false, false, new Point(300, 340), edgeList.get(3), simulation, nServers, 2));

		stationsContainer =  new ArrayList<>(Arrays.asList(stationList.get(0), stationList.get(1), stationList.get(2)));
		
		//3 edges from router to stations
		edgeList.add(new Edge(this, container, false, true, new Point[] {new Point(180,235 - 30), new Point(180, 75+30), new Point(280,75+30)}, stationList.get(0), 3));
		edgeList.add(new Edge(this, container, true, true, new Point[] {new Point(210,0), new Point(280,0)}, stationList.get(1), 3));
		edgeList.add(new Edge(this, container, false, true, new Point[] { new Point(180,235 + 30), new Point(180, 340+30), new Point(280,340+30)}, stationList.get(2), 3));
				
		//create the two Lists for the router
		int l = edgeList.size();
		routerEdges = new ArrayList<>(Arrays.asList(edgeList.get(l-3), edgeList.get(l-2), edgeList.get(l-1)));
		edgesContainer = new ArrayList<>(Arrays.asList(edgeList.get(l-3), edgeList.get(l-2), edgeList.get(l-1)));
		router = new Router(container, false, true, new Point(160,0), edgesContainer, stationList, simulation, probabilities);
		
		e4 = new Edge(this, container, true, true, new Point[] {new Point(80,0), new Point(150,0)}, router);
		edgeList.add(e4);
		
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
		for(Job j: jobSet) {
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

		if(edgeList.size() > 4){
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
	}

	@Override
	public void refresh() {
		super.refresh();
		router.refresh(); //for multiple queue there is also the router
	}

		
	@Override
	public void updateMultiple(Simulation sim, DistributionJMCH service, DistributionJMCH interA, int maxJobs, boolean isGraphicalAnimation){
		simulation = sim;
		interArrival = interA;
		this.service = service;

		source.updateDistribution(service, interA);	
		source.updateMaxJobs(maxJobs);

		if(isGraphicalAnimation){
			if(edgeList.size() < 7){
				edgeList.add(routerEdges.get(0));
				edgeList.add(routerEdges.get(1));
				edgeList.add(routerEdges.get(2));
				edgeList.add(e4);

				source.updateNextContainer(e4);
				router.updateNextContainers(edgesContainer);
			}
		}
		else{
			edgeList.remove(7);
			edgeList.remove(6);
			edgeList.remove(5);
			edgeList.remove(4);
			
			source.updateNextContainer(router);
			router.updateNextContainers(stationsContainer);
		}
	}

	@Override
	public void updateMultiple(Simulation sim, double[] percentages, DistributionJMCH service, DistributionJMCH interA, int maxJobs, boolean isGraphicalAnimation){
		updateMultiple(sim, service, interA, maxJobs, isGraphicalAnimation);
		probabilities[0] = percentages[0];
		probabilities[1] = percentages[1];
		probabilities[2] = 1.00 - percentages[0] - percentages[1];
		router.changeProbabilities(probabilities);
	}
}
