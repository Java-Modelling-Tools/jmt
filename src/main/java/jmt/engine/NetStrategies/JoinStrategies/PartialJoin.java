package jmt.engine.NetStrategies.JoinStrategies;


import jmt.engine.NetStrategies.JoinStrategy;

public class PartialJoin extends JoinStrategy {
	private int requiredNum;
	
	public PartialJoin(Integer requiredNums) {
		this.requiredNum = requiredNums;
	}
	@Override
	public int getRequiredNum() {
		return requiredNum;
	}

}
