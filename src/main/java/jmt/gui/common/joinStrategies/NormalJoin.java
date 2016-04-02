package jmt.gui.common.joinStrategies;

public class NormalJoin extends JoinStrategy {
	
	private int requiredNum = -1;
        
        public NormalJoin() {
                description = "Fires when all the forked tasks are arrrived at the join.";
        }
        
	@Override
	public String getName() {
		return "Standard Join";
	}

	@Override
	public int getRequiredNum() {
		return this.requiredNum;
	}

	@Override
	public void setRequiredNum(int i) {
		this.requiredNum = i;
	}

	@Override
	public JoinStrategy clone() {
		NormalJoin nj = new NormalJoin();
		nj.requiredNum = this.requiredNum;
		return nj;
	}

	@Override
	public String getClassPath() {
		return "jmt.engine.NetStrategies.JoinStrategies.NormalJoin";
	}

	@Override
	public boolean isModelStateDependent() {
		return false;
	}

}
