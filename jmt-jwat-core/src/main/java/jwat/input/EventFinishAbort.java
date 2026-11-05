package jwat.input;

/**
 * Event signalling that a JWAT operation has been aborted.
 */
/**
 * Event signalling that a JWAT operation has been aborted.
 */
/**
 * Event signalling that a JWAT operation has been aborted.
 */
/**
 * Event signalling that a JWAT operation has been aborted.
 */
public class EventFinishAbort implements EventStatus {

	private String msg;

	public EventFinishAbort(String msg) {
		this.msg = msg;
	}

	public int getType() {
		return ABORT_EVENT;
	}

	public String getMessage() {
		return msg;
	}

}
