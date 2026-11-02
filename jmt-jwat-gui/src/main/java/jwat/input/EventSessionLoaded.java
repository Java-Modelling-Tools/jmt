package jwat.input;

import jwat.JwatSession;

/**
 * Event signalling that a JWAT session has been loaded.
 */
/**
 * Event signalling that a JWAT session has been loaded.
 */
/**
 * Event signalling that a JWAT session has been loaded.
 */
/**
 * Event signalling that a JWAT session has been loaded.
 */
public class EventSessionLoaded implements EventStatus {

	private JwatSession loadedSession;
	protected String msg;

	public EventSessionLoaded(JwatSession session) {
		loadedSession = session;
	}

	public int getType() {
		return DONE_EVENT;
	}

	public JwatSession getSession() {
		return loadedSession;
	}

}
