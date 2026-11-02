package jmt.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AsyncWarningTest {

	private long savedDelay;

	@BeforeEach
	void zeroDelay() {
		savedDelay = AsyncWarning.delayMillis;
		AsyncWarning.delayMillis = 0;
	}

	@AfterEach
	void restore() {
		AsyncWarning.delayMillis = savedDelay;
		ErrorReporters.set(null);
	}

	@Test
	void warningReachesTheReporter() throws InterruptedException {
		RecordingErrorReporter rec = new RecordingErrorReporter();
		ErrorReporters.set(rec);
		AsyncWarning.warn("t", "m").join();
		assertEquals(1, rec.calls.size());
		assertEquals("warning|t|m", rec.calls.get(0));
	}

	@Test
	void reporterIsCapturedWhenTheWarningIsRaised() throws InterruptedException {
		RecordingErrorReporter rec = new RecordingErrorReporter();
		ErrorReporters.set(rec);
		AsyncWarning.delayMillis = 200;
		Thread t = AsyncWarning.warn("t", "m");
		ErrorReporters.set(null); // swapped back inside the delay window
		t.join();
		assertEquals(1, rec.calls.size());
	}

	@Test
	void threadIsDaemon() throws InterruptedException {
		AsyncWarning.delayMillis = 100;
		Thread t = AsyncWarning.warn("t", "m");
		assertTrue(t.isDaemon());
		t.join();
	}
}
