package jwat.input;

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
