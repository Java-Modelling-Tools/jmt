package jmt.gui.common.forkStrategies;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.text.html.HTMLDocument.Iterator;

import jmt.gui.common.routingStrategies.ProbabilityRouting;

public class ProbabilitiesFork extends ForkStrategy {
        
	private HashMap<Object, OutPath> outPaths = new HashMap<Object, OutPath>();
	
	public ProbabilitiesFork() {
		description = "Each output link has a probability p of generating tasks. When a output link is selected, the number of tasks to generate is described by a custom distribution (one for each output link).";
	}
	
	@Override
	public String getName() {
		return "Branch Probabilities";
	}

	@Override
	public Map<Object, OutPath> getOutDetails() {
		return outPaths;
	}
	
	@Override
	public ForkStrategy clone() {
		ProbabilitiesFork pf = new ProbabilitiesFork();
		pf.outPaths = new HashMap<Object, OutPath>(outPaths);
		return pf;
	}

	@Override
	public String getClassPath() {
		return "jmt.engine.NetStrategies.ForkStrategies.ProbabilitiesFork";
	}

	@Override
	public boolean isModelStateDependent() {
		return false;
	}

        @Override
        public void removeStation(Object stationKey) {
                outPaths.remove(stationKey);
        }

        @Override
        public void addStation(String stationName, Object stationKey, Object classKey, Vector<Object> classKeys) {
                OutPath tempPath = new OutPath();
                tempPath.putEntry(1, 1.0);
                tempPath.setOutName(stationName);
                tempPath.setProb(1.0);
                outPaths.put(stationKey, tempPath);
        }

}
