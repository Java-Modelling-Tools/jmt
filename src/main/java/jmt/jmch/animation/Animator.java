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


public class Animator extends Thread{
	private final Object mutex = new Object();

	//tells whether this animation is running.
	private boolean isWorking = false;
	private boolean isPaused = false;
	private long startPause;

	//number of millisecs between two frames of the animation
	private long sleepTime;

	/**The {@link jmt.gui.common.animation.Animation} implementing class that has to be
	 * updated to show the animation.
	 */
	protected Animation animation = null;

	/**Creates a new instance of Animator.
	 * Frame rate is set by default at 30 frames per seconds.
	 * @param a: Animation to be updated by this animator.{@see animation}
	 */
	public Animator(Animation a) {
		this(30, a);
	}

	/**Creates a new instance of Animator with explicit definition of framerate.
	 * @param fps: framerate for this animation.
	 * @param a: Animation to be updated by this animator.{@see animation}
	 */
	public Animator(double fps, Animation a) {
		super();
		animation = a;
		sleepTime = (long) (1000 / fps);
	}

	/**Starts the handled animation, otherwise if the Animator is paused it restarts the animation*/
	@Override
	public void start() {
		if(!isPaused) {
			super.start();
			isWorking = true;
		}
		else {
			restart();
		}
	}
	
	/** Pause the animation, save also the time at which the animation was paused */
	public void pause() {
		if(!isPaused){
			isPaused = true;
			startPause = System.currentTimeMillis();
		}	
	}
	
	/** Restart the animation, update also the components of animation to let them know the animation was paused for a certain period of time */
	public void restart() {
		isPaused = false;
		animation.updatePause(System.currentTimeMillis()-startPause);
	}

	/**Performs update of the handled animation as far as terminate() method has not been called.*/
	@Override
	public void run() {
		synchronized (mutex) {
			while (isWorking) {
					try {
						mutex.wait(sleepTime);
					} catch (InterruptedException e) {
						this.terminate();
					}
					
					if (isWorking && !isPaused) {
						animation.refresh();
					}
			}
		}
	}

	/**Terminates current animation*/
	public void terminate() {
		isWorking = false;
		synchronized (mutex) {
			mutex.notifyAll();
		}
	}

	/** To know if the animator is paused */
	public boolean isPaused() {
		return isPaused;
	}
}
