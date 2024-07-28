<<<<<<< HEAD
package jmt.engine.jwat.input;

import jmt.engine.jwat.MatrixObservations;

public interface ProgressStatusListener {

	public void abortEvent();

	public void finishedEvent(MatrixObservations m, int MaxToRead, int Readed);
}
=======
package jmt.engine.jwat.input;

import jmt.engine.jwat.MatrixObservations;

public interface ProgressStatusListener {

	public void abortEvent();

	public void finishedEvent(MatrixObservations m, int MaxToRead, int Readed);
}
>>>>>>> b0a37c45 (Initial commit at SVN r1153.)
