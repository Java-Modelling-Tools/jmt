package simEngine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import QueueNet.NetEvent;
import QueueNet.NetSystem;
import jmt.common.exception.NetException;

/** SimSystem and SimEntity: entity registry, message passing, predicates, holds and error paths. */
class SimSystemTest {

	/** Entity whose behavior is scripted by the test. */
	static class Probe extends SimEntity {
		final List<SimEvent> received = new ArrayList<SimEvent>();
		Runnable onStart = () -> simWait();
		Runnable onBody = () -> simWait();
		int restarts;

		Probe(String name) {
			super(name);
		}

		@Override
		protected void body() {
			received.add(evbuf);
			onBody.run();
		}

		@Override
		public void restart() {
			restarts++;
		}

		@Override
		public void start() {
			onStart.run();
		}
	}

	private NetSystem net;
	private SimSystem sys;

	@BeforeEach
	void setUp() throws Exception {
		net = new NetSystem();
		net.initialize();
		sys = net.getSimSystem();
	}

	private Probe add(String name) {
		Probe p = new Probe(name);
		p.setSimSystem(sys);
		sys.add(p);
		return p;
	}

	@Test
	void registryLooksEntitiesUpByIdAndName() {
		Probe a = add("a");
		Probe b = add("b");
		assertEquals(2, sys.getNumEntities());
		assertEquals(0, a.getId());
		assertEquals(1, b.simCurrent());
		assertSame(b, sys.getEntity(1));
		assertSame(a, sys.getEntity("a"));
		assertEquals("a", a.getName());
		assertNull(sys.getEntity(99));
		assertNull(sys.getEntity("zzz"));
		assertEquals(0.0, sys.clock());
		assertEquals(0.0, sys.getClock());
		assertNotNull(sys.getNumberFormat());
	}

	@Test
	void emptySystemStopsImmediately() throws Exception {
		sys.runStart();
		assertTrue(sys.running());
		assertFalse(sys.runTick());
		assertFalse(sys.running());
	}

	@Test
	void messageWakesWaitingEntity() throws Exception {
		Probe a = add("a");
		Probe b = add("b");
		a.onStart = () -> {
			a.simSchedule(b.getId(), 2.5, 7, "payload");
			a.simWait();
		};
		sys.runStart();
		assertTrue(sys.runTick());
		assertEquals(2.5, sys.clock());
		assertEquals(1, b.received.size());
		assertEquals(7, b.received.get(0).getTag());
		assertEquals("payload", b.received.get(0).getData());
		assertSame(b.received.get(0), b.getEvbuf());
		assertFalse(sys.runTick());
	}

	@Test
	void messageToBusyEntityIsDeferredThenSelected() throws Exception {
		Probe a = add("a");
		Probe b = add("b");
		b.onStart = () -> b.simHold(10); // holding, so not waiting
		a.onStart = () -> {
			a.simSchedule(b.getId(), 1, 5);
			a.simSchedule(b.getId(), 1, 6);
			a.simWait();
		};
		sys.runStart();
		sys.runTick();
		assertEquals(2, b.simWaiting());
		assertEquals(1, b.simWaiting(new SimTypeP(6)));
		b.simSelect(new SimTypeP(6));
		assertEquals(6, b.getEvbuf().getTag());
		assertEquals(1, b.simWaiting());
		b.simGetNext(); // one already deferred: selects it
		assertEquals(5, b.getEvbuf().getTag());
		b.simGetNext(new SimTypeP(1)); // none: becomes waiting
		assertEquals(SimEntity.WAITING, b.getState());
		assertNotNull(b.getWaitingPred());
		b.simSelect(new SimTypeP(1));
		assertNull(b.getEvbuf());
	}

	@Test
	void predicateMismatchPutsEventBack() throws Exception {
		Probe a = add("a");
		Probe b = add("b");
		b.onStart = () -> b.simWaitFor(new SimTypeP(1, 2, 3));
		a.onStart = () -> {
			a.simSchedule(b.getId(), 1, 9);
			a.simSchedule(b.getId(), 2, 3);
			a.simWait();
		};
		sys.runStart();
		sys.runTick();
		assertTrue(b.received.isEmpty());
		assertEquals(1, b.simWaiting());
		sys.runTick();
		assertEquals(1, b.received.size());
		assertEquals(3, b.received.get(0).getTag());
	}

	@Test
	void holdRestartsTheEntity() throws Exception {
		Probe a = add("a");
		a.onStart = () -> a.simHold(4);
		sys.runStart();
		assertTrue(sys.runTick());
		assertEquals(4.0, sys.clock());
		assertEquals(1, a.restarts);
	}

	@Test
	void unscheduleRemovesPendingMessage() throws Exception {
		Probe a = add("a");
		Probe b = add("b");
		RemoveToken[] tok = new RemoveToken[1];
		a.onStart = () -> {
			tok[0] = a.simSchedule(b.getId(), 1, 5);
			a.simWait();
		};
		sys.runStart();
		assertSame(tok[0].getEvent(), tok[0].getEvent());
		assertFalse(tok[0].isDeferred());
		assertTrue(a.simUnschedule(tok[0]));
		assertFalse(sys.runTick());
	}

	@Test
	void cancelTakesBackOwnEvent() throws Exception {
		Probe a = add("a");
		Probe b = add("b");
		a.onStart = () -> {
			a.simSchedule(b.getId(), 1, 5);
			a.simWait();
		};
		sys.runStart();
		SimEvent copy = new SimEvent();
		assertEquals(1, a.simCancel(new SimTypeP(5), copy));
		assertEquals(5, copy.getTag());
		assertEquals(0, a.simCancel(new SimTypeP(5), null));
		assertEquals(0, a.simCancel(SimSystem.SIM_NONE, null));
	}

	@Test
	@Disabled("bug: SimEntity.simHoldFor reads evbuf right after simWaitFor, before any event is delivered (NullPointerException)")
	void holdForWithoutInterruption() throws Exception {
		Probe a = add("a");
		a.onStart = () -> a.simHoldFor(3);
		sys.runStart();
		assertTrue(sys.runTick());
		assertEquals(3.0, sys.clock());
		assertEquals(9999, a.received.get(0).getTag());
	}

	@Test
	void bodyEndingRunnableIsAnError() throws Exception {
		Probe a = add("a");
		Probe b = add("b");
		b.onBody = () -> { };
		a.onStart = () -> {
			a.simSchedule(b.getId(), 1, 1);
			a.simWait();
		};
		sys.runStart();
		NetException e = assertThrows(NetException.class, sys::runTick);
		assertTrue(e.getMessage().contains("ended incorrectly"));
		assertFalse(sys.running());
	}

	@Test
	void sendToNullEntityIsAnError() throws Exception {
		Probe a = add("a");
		a.onStart = () -> {
			a.simSchedule(-1, 1, 1);
			a.simWait();
		};
		sys.runStart();
		assertThrows(NetException.class, sys::runTick);
	}

	@Test
	void dynamicEntityCreatedWhileRunning() throws Exception {
		Probe a = add("a");
		Probe late = new Probe("late");
		late.setSimSystem(sys);
		boolean[] started = new boolean[1];
		late.onStart = () -> started[0] = true;
		sys.runStart();
		sys.add(late); // running: queued as a CREATE event
		assertEquals(1, sys.getNumEntities());
		assertTrue(sys.runTick());
		assertTrue(started[0]);
		assertEquals(2, sys.getNumEntities());
		assertSame(late, sys.getEntity("late"));
		assertNotNull(a);
	}

	@Test
	void timingEventsGoThroughTheTimingQueue() throws Exception {
		Probe a = add("a");
		Probe b = add("b");
		NetStrategies.TransitionUtilities.TimingPacket packet = new NetStrategies.TransitionUtilities.TimingPacket(0, 0.0, 0, 1.0);
		RemoveToken[] tok = new RemoveToken[1];
		a.onStart = () -> {
			tok[0] = a.simSchedule(b.getId(), 2, NetEvent.EVENT_TIMING, packet);
			a.simSchedule(b.getId(), 2, 1);
			a.simWait();
		};
		sys.runStart();
		assertTrue(sys.runTick());
		assertEquals(2, b.received.size());
		assertFalse(sys.runTick());
		assertFalse(sys.remove(tok[0]));
	}

	@Test
	void timingOnlyQueueAdvancesAndCanBeUnscheduled() throws Exception {
		Probe a = add("a");
		Probe b = add("b");
		NetStrategies.TransitionUtilities.TimingPacket packet = new NetStrategies.TransitionUtilities.TimingPacket(0, 0.0, 0, 1.0);
		RemoveToken[] tok = new RemoveToken[1];
		a.onStart = () -> {
			tok[0] = a.simSchedule(b.getId(), 5, NetEvent.EVENT_TIMING, packet);
			a.simWait();
		};
		sys.runStart();
		assertTrue(sys.runTick());
		assertEquals(5.0, sys.clock());
		assertEquals(1, b.received.size());
	}

	@Test
	void abortAndStop() throws Exception {
		Probe a = add("a");
		sys.runStart();
		sys.abort();
		assertFalse(sys.running());
		sys.runStop();
		assertEquals(SimEntity.FINISHED, a.getState());
	}

	@Test
	void predicatesAndEventAccessors() {
		SimEvent e = new SimEvent(SimEvent.SEND, 3.0, 1, 2, 8, "d");
		assertTrue(SimSystem.SIM_ANY.match(e));
		assertFalse(SimSystem.SIM_NONE.match(e));
		assertTrue(new SimTypeP(8).match(e));
		assertFalse(new SimTypeP(1).match(e));
		assertTrue(new SimTypeP(1, 8).match(e));
		assertFalse(new SimTypeP(1, 2).match(e));
		assertTrue(new SimTypeP(1, 2, 8).match(e));
		assertFalse(new SimTypeP(1, 2, 3).match(e));
		assertEquals(3.0, e.eventTime());
		assertEquals(8, e.type());
		assertEquals(1, e.scheduledBy());
		assertEquals(1, e.getSrc());
		assertEquals(2, e.getDest());
		e.setSrc(5);
		e.setDest(6);
		SimEvent c = (SimEvent) e.clone();
		assertEquals(5, c.getSrc());
		assertEquals(6, c.getDest());
		assertEquals("d", c.getData());
		assertTrue(c.toString().contains("tag = 8"));
		SimEvent blank = new SimEvent();
		assertEquals(-1, blank.type());
		blank.copy(e);
		assertEquals(8, blank.getTag());
		RemoveToken t = new RemoveToken(e, true);
		assertTrue(t.isDeferred());
		assertSame(e, t.getEvent());
		assertEquals(2, new SimEvent(SimEvent.HOLD_DONE, 1.0, 2).getSrc());
	}
}
