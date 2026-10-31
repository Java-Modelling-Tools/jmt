package solvers.utilities;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TimerTest {

	@Test
	void prettyIntervalFormatsByMagnitude() {
		assertEquals("0 msec", new Timer().getPrettyInterval());
		assertEquals("999 msec", new Timer(999).getPrettyInterval());
		assertEquals("1.0 sec", new Timer(1000).getPrettyInterval());
		assertEquals("1.5 sec", new Timer(1500).getPrettyInterval());
		assertEquals("59.999 sec", new Timer(59999).getPrettyInterval());
		assertEquals("1 min 0.0 sec", new Timer(60000).getPrettyInterval());
		assertEquals("2 min 3.5 sec", new Timer(123500).getPrettyInterval());
	}

	@Test
	void intervalAccumulatesAndIsNeverNegative() {
		Timer t = new Timer(40);
		assertEquals(40, t.getInterval());
		t.start();
		t.pause();
		long first = t.getInterval();
		assertTrue(first >= 40);
		t.start();
		t.pause();
		assertTrue(t.getInterval() >= first);
	}
}
