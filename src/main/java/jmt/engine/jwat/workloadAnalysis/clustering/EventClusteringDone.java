<<<<<<< HEAD
package jmt.engine.jwat.workloadAnalysis.clustering;

import jmt.engine.jwat.input.EventStatus;

public class EventClusteringDone implements EventStatus {

	private Clustering c;

	public EventClusteringDone(Clustering c) {
		this.c = c;
	}

	public int getType() {
		return EventStatus.DONE_EVENT;
	}

	public Clustering getClustering() {
		return c;
	}

}
=======
package jmt.engine.jwat.workloadAnalysis.clustering;

import jmt.engine.jwat.input.EventStatus;

public class EventClusteringDone implements EventStatus {

	private Clustering c;

	public EventClusteringDone(Clustering c) {
		this.c = c;
	}

	public int getType() {
		return EventStatus.DONE_EVENT;
	}

	public Clustering getClustering() {
		return c;
	}

}
>>>>>>> b0a37c45 (Initial commit at SVN r1153.)
