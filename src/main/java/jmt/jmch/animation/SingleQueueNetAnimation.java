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
import java.util.HashSet;
import java.util.List;

import javax.swing.JPanel;

import jmt.jmch.distributions.DistributionJMCH;
import jmt.jmch.simulation.Simulation;
import jmt.jmch.wizard.panels.AnimationPanel;

/**
 * Class with all the JComponents for the Animation of a Scheduling Policy
 *
 * @author Lorenzo Torri
 * Date: 25-mar-2024
 * Time: 10.46
 */
public class SingleQueueNetAnimation extends AnimationClass{		
	//--all the characteristics of the Animation
	private int nServers = 1;
	private DistributionJMCH interArrival; //by default the two distributions are DETERMINSTIC
	private DistributionJMCH service;

	//-- components that are possibly removed
	private Edge edge;
	
	
	/** Constructor*/
	public SingleQueueNetAnimation(AnimationPanel animPanel, JPanel container, Simulation sim) {		
		super(animPanel, container, sim, 1);
		
		initGUI(container);
	}
	
	@Override
	public void initGUI(JPanel container) {
		//Define the elements of the Animation from the last to the first, since each element must have a reference to the next one
		stationList = new ArrayList<>();
		edgeList = new ArrayList<>();
		jobSet = new HashSet<>();
		
		sink = new Sink(container, true, new Point(0,0), this);
		edgeList.add(new Edge(this, container, true, true, new Point[] {new Point(450,0), new Point(sink.getPositionX() - 10 ,0)}, sink));
		stationList.add(new Station(this, container, true, true, new Point(0,0), edgeList.get(0), simulation, nServers, 0));
		edge = new Edge(this, container, true, true, new Point[] {new Point(80,0), new Point(230,0)}, stationList.get(0));
		edgeList.add(edge);
		source = new Source(this, container, true, new Point(0,0), edgeList.get(edgeList.size()-1), interArrival, service);	
	}
	
	/**
	 * This is the most important method for repainting.
	 * This one is the only one paint that needs to be called when repaint is invoked.
	 * Do not call repaint in other classes related to Animation, otherwise it will not work, since in paint, there is a call to the paint methods of all the classes related to QNA
	 */
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		
		sink.paint(g);
		source.paint(g);
		for(Station st: stationList){
			st.paint(g);
		}
		
		int padding = 10;
		//modify the position of the start and finish points of the edges based on the position of sink, source and station
		edgeList.get(0).setXFinish(sink.getXPos() - padding);
		edgeList.get(0).setXStart(stationList.get(0).getXPos() + stationList.get(0).getTotalLenght() + padding);
		
		if(edgeList.size() > 1){
			edgeList.get(1).setXFinish(stationList.get(0).getXPos() - padding);
			edgeList.get(1).setXStart(source.getXPos() + source.getTotalLenght() + padding);
		}
		
		for(Edge e: edgeList) {
			e.paint(g);
		}
		for(Job j: jobSet) {
			j.paint(g);
		}
	}

	@Override
	public void updateSingle(Simulation sim, int nservers, DistributionJMCH service, DistributionJMCH interA, int maxJobs, boolean isGraphicalAnimation){
		simulation = sim;
		interArrival = interA;
		this.service = service;
		this.nServers = nservers;

		stationList.get(0).typeOfQueue(simulation);
		stationList.get(0).updateNServers(nservers);
		source.updateDistribution(service, interA);	
		source.updateMaxJobs(maxJobs);

		if(isGraphicalAnimation){
			if(edgeList.size() == 1){ //otherwise each time we update the single station with isGraphicalAnimation = true, a new edge is added
				edgeList.add(edge);
				source.updateNextContainer(edge);
			} 	
		}
		else{
			edgeList.remove(1);
			source.updateNextContainer(stationList.get(0));
		}
	}	
}
