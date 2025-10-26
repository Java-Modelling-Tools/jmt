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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JPanel;

import jmt.jmch.Constants;
import jmt.jmch.animation.solver.GeneralSolver;
import jmt.jmch.animation.solver.PSSolver;
import jmt.jmch.animation.solver.AnimationSolver;
import jmt.jmch.distributions.DistributionJMCH;
import jmt.jmch.simulation.Simulation;
import jmt.jmch.wizard.panels.AnimationPanel;

/**
 * This class is a super class for all the animations.
 *
 * @author Lorenzo Torri
 * Date: 22-mar-2024
 * Time: 10.30
 */
public class AnimationClass extends JComponent implements Animation{
	//each Animation must have a jobList of the Jobs inside the Animation. This because the rendering must be performed by the animation, so that it can paint the jobs over all the other components
	protected Set<Job> jobSet;
	protected Animator anim;
	protected AnimationPanel animPanel;
	protected JPanel parent;

	protected Simulation simulation;
	private AnimationSolver solverAnim;

	protected int velocityFactor;

	//common components of all Animations
	protected Source source;
	protected Sink sink;
	protected List<Edge> edgeList;
	protected List<Station> stationList;


	public AnimationClass(AnimationPanel animPanel, JPanel container, Simulation sim, int nQueues){
		restartAnimator();
		jobSet = new HashSet<>();
		this.simulation = sim;
		this.parent = container;
		this.animPanel = animPanel;

		if(simulation.getName() == Constants.PS){
			solverAnim = new PSSolver(this, nQueues);
		}
		else{
			solverAnim = new GeneralSolver(this, nQueues);
		}		
	}

	public void restartAnimator(){
		if(anim != null){
			anim.terminate();
		}
		anim = new Animator(30, this);
	}

	// ================== GRAPHICS METHODS ==================

	@Override
	public void initGUI(JPanel container) {}
	
	@Override
	public void refresh() {
		source.refresh();
		
		for(Station st: stationList) {
			st.refresh();
		}
		for(Edge e: edgeList) {
			e.refresh();
		}

		repaint();	
	}

	@Override
	public void paint(Graphics g) {}

	// ================== ANIMATION'S LIFE METHODS ==================

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
		restartAnimator();
		initGUI(parent);
		repaint();
		solverAnim.init();
	}

	@Override
	public void stop(){
		anim.terminate();
		animPanel.stopAnimation();
	}

	@Override
	public void next() {
		if(!anim.isPaused()) { //if it's the first time the animation is started, otherwise it is a simple restart after a pause
			source.setStart(System.currentTimeMillis());			
		}

		setVelocityFactor(5);
		
		anim.start();
		
		for(Edge e: edgeList) {
			e.nextEvent();
		}
		for(Station st: stationList){
			st.nextEvent();
		}
	}

	/** To set all the subcomponents with nextEvent = false (because we want, after an event, the possibility to continue the simulation in normal mode or with nextEvent way) */
	public void resetNextEvent() { 
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
	public void setVelocityFactor(int velocity) {
		this.velocityFactor = velocity;
	}

	public boolean isPaused(){
		return anim.isPaused();
	}

	@Override
	public void updatePause(long pause) {
		for(Station st: stationList) {
			st.updatePause(pause);
		}
		source.updatePause(pause);
	}

	// ================== UTILS METHODS ==================
	
	/**
	 * This method adds a new job inside the list of jobs of the Animation.
	 * It is usually called by the source of the Animation
	 * @param job to add in the list
	 */
	public void addNewJob(Job job) {
		jobSet.add(job);
	}
	
	/**
	 * This method removes a job inside the list of jobs of the Animation.
	 * It is usually called by the sink of the Animation
	 * @param job to remove from the list
	 */
	public void removeJob(Job job) {
		jobSet.remove(job);
	}

	/**
	 * Method for updating the SingleQueue 
	 * @param sim new info about the simulation
	 * @param nservers number of servers
	 * @param serviceTime distribution of the service time
	 * @param interArrivalTime distribution of the inter-arrival time
	 * @param maxJobs max number of generated jobs
	 * @param isGraphicalAnimation if the edge between the source and the station is there or not
	 */
	public void updateSingle(Simulation sim, int nservers, DistributionJMCH serviceTime, DistributionJMCH interArrivalTime, int maxJobs, boolean isGraphicalAnimation){ }

	/**
	 * Method for updating the MultipleQueue
	 * @param sim new info about the simulation
	 * @param serviceTime distribution of the service time
	 * @param interArrivalTime distribution of the inter-arrival time
	 * @param maxJobs max number of generated jobs
	 * @param isGraphicalAnimation if the edge between the source and the station is there or not
	 */
	public void updateMultiple(Simulation sim, DistributionJMCH serviceTime, DistributionJMCH interArrivalTime, int maxJobs, boolean isGraphicalAnimation){}

	/**
	 * Method for updating the MultipleQueue with routing Policy = PROBABILISTIC
	 * @param sim new info about the simulation
	 * @param percentages new array of percentages associated to the outgoing arcs from the router
	 * @param serviceTime distribution of the service time
	 * @param interArrivalTime distribution of the inter-arrival time
	 * @param maxJobs max number of generated jobs
	 * @param isGraphicalAnimation if the edge between the source and the station is there or not
	 */
	public void updateMultiple(Simulation sim, double[] percentages, DistributionJMCH serviceTime, DistributionJMCH interArrivalTime, int maxJobs, boolean isGraphicalAnimation){}


	public Animator getAnimator(){
		return anim;
	}

	public Simulation getSimulation(){
		return simulation;
	}

	public AnimationSolver getSolverAnimation(){
		return solverAnim;
	}

	public int getVelocityFactor(){
		return velocityFactor;
	}

	public int getListJobSize(){
		return jobSet.size();
	}

	public Source getSource() {
		return source;
	}

	public Station getSingleStation(){
		return stationList.get(0);
	}

	@Override
	public void updateAnimResults() {
		animPanel.updateAnimResults(source.getGeneratedJobs(), solverAnim.getMeasures());
	}
}
