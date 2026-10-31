package simEngine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import NetStrategies.TransitionUtilities.TimingPacket;
import random.engine.MersenneTwister;

/** Unit tests of the future-event queues: ordering, ties, removal, iteration, growth. */
class EventQueuesTest {

	static SimEvent ev(double time, int tag) {
		return new SimEvent(0, time, 0, 0, tag, null);
	}

	static Stream<Supplier<EventQueue>> ordered() {
		return Stream.of(ListEventQueue::new, () -> new CircularEventQueue(1), HybridEventQueue::new);
	}

	@ParameterizedTest
	@MethodSource("ordered")
	void popsInTimeOrderWithFifoTies(Supplier<EventQueue> f) {
		EventQueue q = f.get();
		double[] times = { 5, 1, 3, 3, 3, 0.5, 9, 3 };
		for (int i = 0; i < times.length; i++) {
			assertTrue(q.add(ev(times[i], i)));
		}
		assertEquals(times.length, q.size());
		assertEquals(0.5, q.peek().eventTime());
		int[] expectedTags = { 5, 1, 2, 3, 4, 7, 0, 6 };
		for (int tag : expectedTags) {
			assertEquals(tag, q.pop().getTag());
		}
		assertEquals(0, q.size());
	}

	@ParameterizedTest
	@MethodSource("ordered")
	void interleavedAddAndPopMatchesSortedReference(Supplier<EventQueue> f) {
		EventQueue q = f.get();
		Random r = new Random(7);
		List<SimEvent> ref = new ArrayList<SimEvent>();
		double now = 0;
		for (int i = 0; i < 400; i++) {
			if (r.nextInt(3) > 0 || ref.isEmpty()) {
				SimEvent e = ev(now + r.nextInt(6), i);
				q.add(e);
				int pos = 0;
				while (pos < ref.size() && ref.get(pos).eventTime() <= e.eventTime()) {
					pos++;
				}
				ref.add(pos, e);
			} else {
				SimEvent expected = ref.remove(0);
				SimEvent got = q.pop();
				assertSame(expected, got);
				now = got.eventTime();
			}
			assertEquals(ref.size(), q.size());
		}
	}

	@ParameterizedTest
	@MethodSource("ordered")
	void clearEmpties(Supplier<EventQueue> f) {
		EventQueue q = f.get();
		q.add(ev(1, 1));
		q.add(ev(2, 2));
		q.clear();
		assertEquals(0, q.size());
		q.add(ev(3, 3));
		assertEquals(3, q.pop().getTag());
	}

	@ParameterizedTest
	@MethodSource("ordered")
	void iteratesAllEvents(Supplier<EventQueue> f) {
		EventQueue q = f.get();
		for (int i = 0; i < 6; i++) {
			q.add(ev(i % 3, i));
		}
		int n = 0;
		for (SimEvent e : q) {
			assertNotNull(e);
			n++;
		}
		assertEquals(6, n);
	}

	// ListEventQueue

	@Test
	void listQueueSpecifics() {
		ListEventQueue q = new ListEventQueue(4);
		SimEvent a = ev(1, 1), b = ev(2, 2), c = ev(3, 3);
		q.add(c);
		q.add(a);
		q.add(b);
		assertSame(a, q.top());
		assertSame(a, q.peek());
		assertTrue(q.remove(b));
		assertFalse(q.remove(b));
		assertSame(a, q.pop());
		assertSame(c, q.pop());
		assertThrows(IndexOutOfBoundsException.class, q::pop);
	}

	// HybridEventQueue

	@Test
	void hybridEmptyReturnsNull() {
		HybridEventQueue q = new HybridEventQueue();
		assertNull(q.peek());
		assertNull(q.pop());
		assertFalse(q.iterator().hasNext());
	}

	@Test
	void hybridPastEventBecomesCurrent() {
		HybridEventQueue q = new HybridEventQueue();
		q.add(ev(5, 1));
		q.add(ev(7, 2));
		assertEquals(5, q.pop().eventTime());
		q.add(ev(2, 3)); // earlier than current time: robust path
		assertEquals(3, q.pop().getTag());
		assertEquals(7, q.pop().eventTime());
	}

	@Test
	void hybridRemoveFromCurrentAndFuture() {
		HybridEventQueue q = new HybridEventQueue();
		SimEvent now1 = ev(0, 1), now2 = ev(0, 2), later = ev(4, 3);
		q.add(now1);
		q.add(now2);
		q.add(later);
		assertTrue(q.remove(now1));
		assertTrue(q.remove(later));
		assertFalse(q.remove(later));
		assertEquals(1, q.size());
		assertSame(now2, q.pop());
	}

	@Test
	void hybridIteratorRemovesFromBothParts() {
		HybridEventQueue q = new HybridEventQueue();
		q.add(ev(0, 1));
		q.add(ev(3, 2));
		Iterator<SimEvent> it = q.iterator();
		while (it.hasNext()) {
			it.next();
			it.remove();
		}
		assertEquals(0, q.size());
	}

	// TimingEventQueue

	static SimEvent timed(double time, double delay, int prio, double weight, int tag) {
		return new SimEvent(0, time, 0, 0, tag, new TimingPacket(0, delay, prio, weight));
	}

	@Test
	void timingOrdersByTimeThenDelayThenPriority() {
		TimingEventQueue q = new TimingEventQueue();
		q.setEngine(new MersenneTwister(42));
		q.add(timed(2, 0, 0, 1, 1));
		q.add(timed(1, 5, 0, 1, 2));
		q.add(timed(1, 3, 0, 1, 3));
		q.add(timed(1, 3, 9, 1, 4)); // higher priority wins the tie at equal delay
		assertEquals(4, q.size());
		assertEquals(4, q.peek().getTag());
		assertEquals(4, q.pop().getTag());
		assertEquals(3, q.pop().getTag());
		assertEquals(2, q.pop().getTag());
		assertEquals(1, q.pop().getTag());
		assertNull(q.pop());
		assertNull(q.peek());
	}

	@Test
	void timingTiedEventsPopByWeightedRandomChoice() {
		TimingEventQueue q = new TimingEventQueue();
		q.setEngine(new MersenneTwister(42));
		int heavy = 0;
		for (int round = 0; round < 200; round++) {
			q.add(timed(1, 0, 0, 1, 1));
			q.add(timed(1, 0, 0, 9, 2));
			SimEvent first = q.pop();
			if (first.getTag() == 2) {
				heavy++;
			}
			assertEquals(1, q.size());
			q.pop();
		}
		assertTrue(heavy > 140 && heavy < 200, "heavy chosen " + heavy);
	}

	@Test
	void timingRemoveAndClear() {
		TimingEventQueue q = new TimingEventQueue();
		SimEvent a = timed(1, 0, 0, 1, 1), b = timed(2, 0, 0, 1, 2), c = timed(1, 0, 0, 1, 3);
		assertFalse(q.remove(a));
		q.add(a);
		q.add(b);
		q.add(c);
		assertTrue(q.remove(b)); // future
		assertTrue(q.remove(c)); // current
		assertEquals(1, q.size());
		int n = 0;
		for (Iterator<SimEvent> it = q.iterator(); it.hasNext();) {
			it.next();
			n++;
		}
		assertEquals(1, n);
		q.add(timed(5, 0, 0, 1, 9));
		Iterator<SimEvent> it = q.iterator();
		it.next();
		it.next();
		it.remove(); // from future
		assertEquals(1, q.size());
		it = q.iterator();
		it.next();
		it.remove(); // from current
		assertEquals(0, q.size());
	}

	@Test
	@Disabled("bug: CircularList.clear() does not reset size, so TimingEventQueue.clear() keeps reporting the old size")
	void timingClearEmpties() {
		TimingEventQueue q = new TimingEventQueue();
		q.add(timed(1, 0, 0, 1, 1));
		q.clear();
		assertEquals(0, q.size());
	}

	// CircularEventQueue

	@Test
	void circularGrowsAndTracksState() {
		CircularEventQueue q = new CircularEventQueue();
		assertNull(q.pop());
		assertNull(q.peek());
		for (int i = 0; i < 40; i++) {
			q.add(ev(40 - i, i)); // always inserted at the front
		}
		assertEquals(40, q.size());
		assertEquals(1.0, q.peek().eventTime());
		assertTrue(q.toString().contains("size = 40"));
		for (int i = 0; i < 40; i++) {
			assertEquals(1.0 + i, q.pop().eventTime());
		}
		assertTrue(q.toString().contains("start = -1"));
		assertEquals(0, new CircularEventQueue(0).size());
	}

	@Test
	void circularPushAppendsAndInsertsInMiddle() {
		CircularEventQueue q = new CircularEventQueue(3);
		q.push(ev(1, 1));
		q.push(ev(2, 2));
		q.push(ev(4, 4));
		q.push(ev(5, 5));
		q.add(ev(3, 3));
		for (int i = 1; i <= 5; i++) {
			assertEquals(i, q.pop().getTag());
		}
	}

	@Test
	void circularMiddleInsertAfterPops() {
		CircularEventQueue q = new CircularEventQueue(4);
		for (int i = 0; i < 4; i++) {
			q.add(ev(i * 2, i));
		}
		q.pop();
		q.pop();
		q.add(ev(5, 10)); // middle of a queue whose start moved
		q.add(ev(7, 11));
		int[] expected = { 2, 10, 3, 11 };
		for (int tag : expected) {
			assertEquals(tag, q.pop().getTag());
		}
	}

	@Test
	void circularEnumerationAndIterator() {
		CircularEventQueue q = new CircularEventQueue();
		q.add(ev(1, 1));
		q.add(ev(2, 2));
		Enumeration<SimEvent> en = q.elements();
		assertTrue(en.hasMoreElements());
		assertEquals(1, en.nextElement().getTag());
		assertEquals(2, en.nextElement().getTag());
		assertFalse(en.hasMoreElements());
		assertThrows(java.util.NoSuchElementException.class, en::nextElement);
		Iterator<SimEvent> it = q.iterator();
		assertTrue(it.hasNext());
		assertEquals(1, it.next().getTag());
	}

	@Test
	void circularClear() {
		CircularEventQueue q = new CircularEventQueue();
		q.add(ev(1, 1));
		q.clear();
		assertEquals(0, q.size());
		assertNull(q.peek());
		CircularEventQueue.test(); // built-in smoke routine (prints)
	}

	@Test
	@Disabled("bug: CircularEventQueue.remove(SimEvent) removes the last slot, not the requested event")
	void circularRemoveTakesOutTheGivenEvent() {
		CircularEventQueue q = new CircularEventQueue();
		SimEvent a = ev(1, 1), b = ev(2, 2), c = ev(3, 3);
		q.add(a);
		q.add(b);
		q.add(c);
		assertTrue(q.remove(a));
		assertEquals(2, q.size());
		assertSame(b, q.pop());
		assertSame(c, q.pop());
	}

	@Test
	@Disabled("bug: CircularEventQueue.remove(SimEvent) returns true for an event that is not queued")
	void circularRemoveUnknownReturnsFalse() {
		CircularEventQueue q = new CircularEventQueue();
		q.add(ev(1, 1));
		assertFalse(q.remove(ev(1, 2)));
		assertEquals(1, q.size());
	}
}
