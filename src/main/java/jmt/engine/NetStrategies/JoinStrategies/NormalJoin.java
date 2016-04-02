package jmt.engine.NetStrategies.JoinStrategies;

import jmt.engine.NetStrategies.JoinStrategy;

public class NormalJoin extends JoinStrategy {
	
	private int requiredNum;
	
	public NormalJoin(Integer requiredNums){
		this.requiredNum = requiredNums;
	}
	
	@Override
	public int getRequiredNum() {
		return requiredNum;
	}

}
