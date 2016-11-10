package jmt.engine.jwat.input;

public interface EventStatus {

	public static final int ABORT_EVENT = 0;
	public static final int DONE_EVENT = 1;

	public int getType();
}
