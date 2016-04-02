/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jmt.gui.common.joinStrategies;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author mattia
 */
public class GuardJoin extends JoinStrategy {
        
        private Map<Object, Integer> guard;
	
	public GuardJoin(){
		description = "Fires the join upon arrival of a subset of forked tasks in each class.";
                guard = new HashMap<>();
	}
	
	@Override
	public int getRequiredNum() {
		int sum = 0;
		for(Integer i: guard.values())
                        sum += i;
                return sum;
	}
        
	@Override
	public String getName() {
		return "Guard";
	}
        
	@Override
	public void setRequiredNum(int i) {
		
	}
        
        public Map<Object, Integer> getGuard() {
                return guard;
        }
        
        public void setGuard(Map<Object, Integer> mix) {
                this.guard = mix;
        }
        
	@Override
	public JoinStrategy clone() {
		GuardJoin j = new GuardJoin();
		j.guard = new HashMap(guard);
		return j;
	}
        
	@Override
	public String getClassPath() {
		return "jmt.engine.NetStrategies.JoinStrategies.GuardJoin";
	}
        
	@Override
	public boolean isModelStateDependent() {
		return false;
	}
        
}
