package jmt.common;

public class Class_ {

	private String name;
	private boolean closed;
	private double data;
	
	/**
	 * Constructs a class with default values
	 */
	public Class_() {
		name = "Class1";
		closed = true;
		data = 1;
	}
	
	public Class_(String name, boolean closed, double data) {
		this.name = name;
		this.closed = closed;
		this.data = data;
	}
	
	public String getName() {
		return name;
	}

	public boolean isClosed() {
		return closed;
	}
	
	public boolean isOpen() {
		return !closed;
	}

	public double getData() {
		return data;
	}
}
