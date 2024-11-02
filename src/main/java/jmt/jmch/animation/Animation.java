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

/**
 * Interface that must be implemented by the class that represent the Animation
 *
 * @author Lorenzo Torri
 * Date: 22-mar-2024
 * Time: 10.30
 */
public interface Animation {
	/**
	 * Method for updating some values inside the class.
	* It is crucial since when we call the repaint() method and start the chain of calls of the paint() methods of each JComponent, those paint() method use some local variables of the clasess.
	* If we change those variables with this method, then we can obtain some changes also in the graphics
	*/
	public void refresh();

	/**
	 * Paint is a method for all JComponents. It is called when the class is istanciated, and called again when the repaint() method is invoked inside the class.
	* Each paint of an animation recall then all the paint() of the subcomponents of an Animation.
	* Repaint() must be called only once inside the Animation class, never on the subcomponents
	* @param g
	*/
	public void paint(Graphics g);
	
	/* The next three methods are related to the methods of the Animator associated to the Animation */
	/** Start the animation */
	public void start();
	
	/** Pause the animation */
	public void pause();
	
	/** Reload the animation if it was paused */
	public void reload();

	/** Stop the animation. It is used when a maximum number of jobs is reached and nothing can be done */
	public void stop();

	/**
	 * Next event in the animation.
	 * The simulation time is increased (whenever System.currentMillis() is used, multiply it for a factor > 1), and the simulation pauses when a new event occurs
	 */
	public void next();

	/** Set the velocity Factor of the animation */
	public void setVelocityFactor(int velocity);
		
	/**
	* This method is important for udpdating all those components inside the Animation that are affected by a pause of the Animator.
	* One example is the CircleStation, in which the amount of progression is computed as the difference between the current time and the time of entrance of the job in the circle.
	* But if the animator was paused, then this amount does not have to be computed in the difference of before, so it is crucial to pass to the Circle this value and remove it from the difference
	* @param pause the amount of time during which the animator was paused
	*/
	public void updatePause(long pause);

	public Source getSource();
}
 
