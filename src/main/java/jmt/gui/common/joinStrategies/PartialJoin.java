package jmt.gui.common.joinStrategies;

import jmt.gui.common.joinStrategies.JoinStrategy;

public class PartialJoin extends JoinStrategy {
        
	private int requiredNum = 1;
	
	public PartialJoin(){
		description = "Fires the join upon arrival of a subset of forked tasks. Exceeding tasks will be ignored at this station.";
	}
	
	@Override
	public int getRequiredNum() {
		return this.requiredNum;
	}
	@Override
	public String getName() {
		return "Quorum";
	}
	@Override
	public void setRequiredNum(int i) {
		this.requiredNum = i;
	}
	@Override
	public JoinStrategy clone() {
		PartialJoin ps = new PartialJoin();
		ps.requiredNum = this.requiredNum;
		return ps;
	}
	@Override
	public String getClassPath() {
		return "jmt.engine.NetStrategies.JoinStrategies.PartialJoin";
	}
	@Override
	public boolean isModelStateDependent() {
		return false;
	}

}
