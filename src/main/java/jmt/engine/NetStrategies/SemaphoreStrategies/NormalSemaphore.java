/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Vitor S. Lopes
 */


package jmt.engine.NetStrategies.SemaphoreStrategies;

import jmt.engine.NetStrategies.SemaphoreStrategy;


public class NormalSemaphore extends SemaphoreStrategy{
    
private int semaphoreThres;
	
	public NormalSemaphore(Integer semaphoreThreshold){
		this.semaphoreThres = semaphoreThreshold;
	}
	@Override
	public int getSemaphoreThres() {
		return semaphoreThres; 
	}
    
}