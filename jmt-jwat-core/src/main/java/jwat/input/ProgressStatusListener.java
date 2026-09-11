package jwat.input;

import jwat.MatrixObservations;

public interface ProgressStatusListener {

	public void abortEvent();

	public void finishedEvent(MatrixObservations m, int MaxToRead, int Readed);
}
