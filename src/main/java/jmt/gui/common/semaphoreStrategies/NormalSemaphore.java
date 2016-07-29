/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jmt.gui.common.semaphoreStrategies;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Vitor S. Lopes
 */
public class NormalSemaphore extends SemaphoreStrategy{
        
	private int semaphoreThres = 1;
        	
	public NormalSemaphore(){
		description = "The semaphore stops a percentage of forked tasks in each class until they all reach the semaphore. Then it lets them pass through altogether. Tasks originated from the same job that arrive later are not blocked.";		
	}
	
	@Override
	public int getSemaphoreThres() {
		return this.semaphoreThres;
	}
        
	@Override
	public String getName() {
		return "Standard Semaphore";
	}
        
	@Override
	public void setSemaphoreThres(int i) {
		this.semaphoreThres = i;
	}
        
	@Override
	public SemaphoreStrategy clone() {
		NormalSemaphore s = new NormalSemaphore();
		//j.semaphore = new HashMap(semaphore);
		s.semaphoreThres = this.semaphoreThres;
		return s;
	}
        
	@Override
	public String getClassPath() {
		return "jmt.engine.NetStrategies.SemaphoreStrategies.NormalSemaphore";
	}
        
	@Override
	public boolean isModelStateDependent() {
		return false;
	}
        
}