/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package jmt.engine.QueueNet;

/**
 * This class implements the description of a job class.
 * @author Francesco Radaelli, Stefano Omini
 */
public class JobClass {

	//job class name
	private String name;
	//job class id
	private int id;

	/* Closed class type */
	public static final int CLOSED_CLASS = 0;
	/* Open class type */
	public static final int OPEN_CLASS = 1;

	//class type (open or closed)
	private int type;
	//class priority
	private int priority;

	private String referenceNodeName;

	private double softDeadline;

	private boolean hasCachePair;
	private boolean isCacheHit;

	private JobClass cachePairClass;
	private String cachePairClassName;

	//----------------------CONSTRUCTORS---------------------------///

	/** Creates a new instance of JobClass
	 * @param Name Symbolic name of the job class.
	 */
	public JobClass(String Name) {
		this.name = Name;
		this.priority = 0;
		this.referenceNodeName = null;
	}

	/** Creates a new instance of JobClass
	 * @param Name Symbolic name of the job class.
	 * @param priority Priority of this job class. Must be greater than 0.
	 * @param type Class type. See constants.
	 * @param refNode the reference node of this JobClass
	 */
	public JobClass(String Name, int priority, int type, String refNode) {
		this.name = Name;
		this.type = type;
		this.referenceNodeName = refNode;
		if (priority < 0) {
			this.priority = 0;
		} else {
			this.priority = priority;
		}
	}

	//----------------------SETTER AND GETTER---------------------------///

	/**Sets the Id of this class
	 * @param id
	 */
	void setId(int id) {
		this.id = id;
	}

	/** Gets the id of this class.
	 * @return Value of property Id.
	 */
	public int getId() {
		return id;
	}

	/** Gets the name of this class.
	 * @return Value of property Name.
	 */
	public String getName() {
		return name;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getReferenceNodeName() {
		return referenceNodeName;
	}

	public void setReferenceNodeName(String referenceNodeName) {
		this.referenceNodeName = referenceNodeName;
	}


	public void setCacheReferece(String cachePairClassName, boolean isCacheHit){
		this.hasCachePair = true;
		this.isCacheHit = isCacheHit;
		this.cachePairClassName = cachePairClassName;
	}

	public boolean isHasCachePair() {
		return hasCachePair;
	}

	public boolean isCacheHit() {
		return isCacheHit;
	}

	public String getCachePairClassName() {
		return cachePairClassName;
	}
	public JobClass getCachePairClass() {
		return cachePairClass;
	}

	public void setCachePairClass(JobClass jobClass) {
		this.cachePairClass = jobClass;
	}

	public void setSoftDeadline(double softDeadline) {
		this.softDeadline = softDeadline;
	}

	public double getSoftDeadline() {
		return this.softDeadline;
	}
}
