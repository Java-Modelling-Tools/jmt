package jmt.engine.NetStrategies;

import jmt.common.AutoCheck;

public abstract class SemaphoreStrategy implements AutoCheck{
	public abstract int getSemaphoreThres();
	
	public boolean check() {
		return true;
	}
}
