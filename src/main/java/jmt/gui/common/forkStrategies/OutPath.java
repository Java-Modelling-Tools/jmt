package jmt.gui.common.forkStrategies;

import java.util.HashMap;

public class OutPath {
	
	private HashMap<Object, Object> outParameters = new HashMap<Object, Object>();
	private Object outName;
	private Double prob;
	
	public OutPath(){
	}
	
	public void putEntry(Object o, Object d){
		this.outParameters.put(o, d);
	}
	
	public HashMap<Object, Object> getOutParameters() {
		return outParameters;
	}
	public void setOutParameters(HashMap<Object, Object> outProbabilities) {
		this.outParameters = outProbabilities;
	}
	
	public Object getOutName() {
		return outName;
	}
	public void setOutName(Object outName) {
		this.outName = outName;
	}
	
	public Double getProb() {
		return prob;
	}
	public void setProb(Double prob) {
		this.prob = prob;
	}

}