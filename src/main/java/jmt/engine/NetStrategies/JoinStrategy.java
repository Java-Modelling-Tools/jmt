package jmt.engine.NetStrategies;

import jmt.common.AutoCheck;

public abstract class JoinStrategy implements AutoCheck{
	public abstract int getRequiredNum();
	
	public boolean check() {
		return true;
	}
}
