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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/** This class implements a list of job classes. Note that only classes of QueueNet
 * package can add or remove objects to/from the list.
 * @author Francesco Radaelli
 */
public class JobClassList {

	private List<JobClass> jobClasses;
	private Map<String, JobClass> jobClassMap;

	/** Creates a new instance of JobClassList class.
	 */
	JobClassList() {
		jobClasses = new ArrayList<JobClass>();
		jobClassMap = new HashMap<String, JobClass>();
	}

	/** Adds a new job class to the list.
	 * @param jobClass Reference to the job class.
	 */
	void add(JobClass jobClass) {
		jobClasses.add(jobClass);
		jobClassMap.put(jobClass.getName(), jobClass);
	}

	/** Removes a job class from the list.
	 * @param jobClass Reference to the job class.
	 */
	void remove(JobClass jobClass) {
		jobClasses.remove(jobClass);
		jobClassMap.remove(jobClass.getName());
	}

	/** Gets the first job class in the list.
	 * @return Reference to the first job class.
	 */
	public JobClass getFirst() {
		return jobClasses.get(0);
	}

	/** Gets the last job class in the list.
	 * @return Reference to the last job class.
	 */
	public JobClass getLast() {
		return jobClasses.get(jobClasses.size() - 1);
	}

	/** Gets i-th job class in the list.
	 * @param index Index of the job class.
	 * @return Reference to the i-th job class.
	 */
	public JobClass get(int index) {
		return jobClasses.get(index);
	}

	/** Gets the job class with a specific name in the list.
	 * @param name Name of the job class.
	 * @return Reference to the job class.
	 */
	public JobClass get(String name) {
		return jobClassMap.get(name);
	}

	/** Gets the size of the list.
	 * @return Size of the list.
	 */
	public int size() {
		return jobClasses.size();
	}

	/** Gets an iterator of the list.
	 * @return Iterator of the list.
	 */
	public ListIterator<JobClass> listIterator() {
		return jobClasses.listIterator();
	}

	/** Converts the list into an array of job classes.
	 * @return Array of job classes in the list.
	 */
	public JobClass[] toArray() {
		return jobClasses.toArray(new JobClass[jobClasses.size()]);
	}

	/** Gets the index of a job class.
	 * @param jobClass Reference to the job class.
	 * @return Index of the job class.
	 */
	public int indexOf(JobClass jobClass) {
		return jobClasses.indexOf(jobClass);
	}

	/** Used to know if a job class is in the list.
	 * @param jobClass Reference to the job class.
	 * @return true if the job class is in the list.
	 */
	public boolean contains(JobClass jobClass) {
		return jobClasses.contains(jobClass);
	}

}
