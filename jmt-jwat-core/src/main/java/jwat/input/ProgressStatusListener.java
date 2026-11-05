package jwat.input;

import jwat.MatrixObservations;

/**
 * Listener notified of the progress of a time consuming JWAT operation.
 */
/**
 * Listener notified of the progress of a time consuming JWAT operation.
 */
/**
 * Listener notified of the progress of a time consuming JWAT operation.
 */
/**
 * Listener notified of the progress of a time consuming JWAT operation.
 */
public interface ProgressStatusListener {

	public void abortEvent();

	public void finishedEvent(MatrixObservations m, int MaxToRead, int Readed);
}
