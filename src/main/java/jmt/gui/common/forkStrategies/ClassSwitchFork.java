/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jmt.gui.common.forkStrategies;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author mattia
 */
public class ClassSwitchFork extends ForkStrategy {
                
	private HashMap<Object, OutPath> outPaths = new HashMap<Object, OutPath>();
        
        @Override
        public String getName() {
                description = "Generates tasks of multiple classes (the same number on each output link)";
                return "Class Switch";
        }

        @Override
        public Map<Object, OutPath> getOutDetails() {
                return outPaths;
        }

        @Override
        public ForkStrategy clone() {
                ClassSwitchFork csf = new ClassSwitchFork();
		csf.outPaths = new HashMap<Object, OutPath>(outPaths);
		return csf;
        }

        @Override
        public String getClassPath() {
                return "jmt.engine.NetStrategies.ForkStrategies.ClassSwitchFork";
        }

        @Override
        public boolean isModelStateDependent() {
                return false;
        }
        
        @Override
        public void removeStation(Object stationKey) {
                outPaths.remove(stationKey);
        }
   
        
}
