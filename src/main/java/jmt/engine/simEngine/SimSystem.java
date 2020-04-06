/**
 * The simjava package is a support library for discrete
 * event simulations.
 */

package jmt.engine.simEngine;

import java.util.Hashtable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jmt.common.exception.NetException;
import jmt.engine.QueueNet.NetEvent;
import jmt.engine.QueueNet.NetSystem;

/**
 * This is the system class which manages the simulation. All
 * the members of this class are static, so there is no need to
 * create an instance of this class.
 */
public class SimSystem {

	private static final boolean DEBUG = false;

	// Private data members
	private List<SimEntity> entities; // The current entity list

	private EventQueue future; // The future event queue

	private EventQueue deferred; // The deferred event queue

	private EventQueue timing; // The timing event queue

	private double clock; // Holds the current global simulation time
	private boolean running; // Tells whether the run() member been called yet
	private NumberFormat nf;

	// cache for the entity IDs
	private Hashtable<Integer,SimEntity> entityIdHash = new Hashtable<Integer,SimEntity>();
	// cache for the entity Names
	private Hashtable<String,SimEntity> entityNameHash = new Hashtable<String,SimEntity>();

	private NetSystem netSystem;
	//
	// Public library interface
	//

	// Initializes system
	/** Initializes the system, this function works as a
	 * constructor, and should be called at the start of any simulation
	 * program. It comes in several flavours depending on what context
	 * the simulation is running.<P>
	 * This is the simplest, and should be used for standalone simulations
	 * It sets up trace output to a file in the current directory called
	 * `tracefile'
	 */
	public void initialize() {
		entities = new ArrayList<SimEntity>();

		// future = new ListEventQueue();
		// future = new CircularEventQueue();
		// future = new SuperEventQueue();
		future = new HybridEventQueue();

		deferred = new ListEventQueue();

		timing = new TimingEventQueue();
		((TimingEventQueue) timing).setEngine(netSystem.getEngine());

		clock = 0.0;
		running = false;

		// Set the default number format
		nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(4);
		nf.setMinimumFractionDigits(2);
	}

	/** Returns the number format used for generating
	 * times in trace lines
	 */
	public NumberFormat getNumberFormat() {
		return nf;
	}

	// The two standard predicates
	/** A standard predicate that matches any event. */
	static final public SimAnyP SIM_ANY = new SimAnyP();
	/** A standard predicate that does not match any events. */
	static final public SimNoneP SIM_NONE = new SimNoneP();

	// Public access methods

	/** Get the current simulation time.
	 * @return The simulation time
	 */
	public double clock() {
		return clock;
	}

	/** A different name for <tt>SimSystem.clock()</tt>.
	 * @return The current simulation time
	 */
	public double getClock() {
		return clock;
	}

	/** Gets the current number of entities in the simulation
	 * @return A count of entities
	 */
	public int getNumEntities() {
		return entities.size();
	}

	/** Finds an entity by its id number.
	 * @param id The entity's unique id number
	 * @return A reference to the entity, or null if it could not be found
	 */
	final public SimEntity getEntity(int id) {
		SimEntity found = entityIdHash.get(id);
		if (found == null) {
			System.out.println("SimSystem: could not find entity " + id);
		}
		return found;
	}

	/** Finds an entity by its name.
	 * @param name The entity's name
	 * @return A reference to the entity, or null if it could not be found
	 */
	public SimEntity getEntity(String name) {
		SimEntity found = entityNameHash.get(name);
		if (found == null) {
			System.out.println("SimSystem: could not find entity " + name);
		}
		return found;
	}

	/** Adds a new entity to the simulation.
	 * This is now done automatically in the SimEntity constructor,
	 * so there is no need to call this explicitly.
	 * @param e A reference to the new entity
	 */
	public void add(SimEntity e) {
		SimEvent evt;
		if (running()) {
			/* Post an event to make this entity */
			evt = new SimEvent(SimEvent.CREATE, clock, e.getId(), 0, 0, e);
			future.add(evt);
		} else {
			e.setId(entities.size());
			entities.add(e);
			entityIdHash.put(e.getId(), e);
			entityNameHash.put(e.getName(), e);
		}
	}

	/** Adds a new entity to the simulation, when the simulation is running.
	 * Note this is an internal method and should not be called from
	 * user simulations. Use <tt>add()</tt> instead to add entities
	 * on the fly.
	 * @param e A reference to the new entity
	 */
	synchronized void addEntityDynamically(SimEntity e) {
		e.setId(entities.size());
		entities.add(e);
		entityIdHash.put(e.getId(), e);
		entityNameHash.put(e.getName(), e);
		e.start();
	}

	/**
	 * Starts the simulation running, by calling the start() method of each entity.
	 * Of course this should be called after all the entities have been setup and added,
	 * and their ports linked.
	 */
	public void runStart() {
		SimEntity ent;
		running = true;
		// Start all the entities' threads
		if (DEBUG) {
			System.out.println("SimSystem: Starting entities");
		}
		for (Iterator<SimEntity> it = entities.iterator(); it.hasNext();) {
			ent = it.next();
			ent.start();
		}
	}

	/**
	 * Runs one tick of the simulation: the system looks for events in the future queue.
	 * @return <tt>false</tt> if there are no more future events to be processed
	 */
	public boolean runTick() throws NetException {
		double now = 0.0;
		if (future.size() > 0 && timing.size() > 0) {
			now = Math.min(future.peek().eventTime(), timing.peek().eventTime());
		} else if (future.size() > 0) {
			now = future.peek().eventTime();
		} else if (timing.size() > 0) {
			now = timing.peek().eventTime();
		} else {
			running = false;
			return false;
		}

		SimEvent event = null;
		while (future.size() > 0 && future.peek().eventTime() == now) {
			event = future.pop();
			processEvent(event);
		}
		while (timing.size() > 0 && timing.peek().eventTime() == now) {
			future.add(timing.pop());
			while (future.size() > 0 && future.peek().eventTime() == now) {
				event = future.pop();
				processEvent(event);
			}
		}
		return true;
	}

	/** Stops the simulation, by calling the poison() method of each SimEntity.
	 */
	public void runStop() {
		SimEntity ent;
		// Attempt to kill all the entity threads
		for (Iterator<SimEntity> it = entities.iterator(); it.hasNext();) {
			ent = it.next();
			ent.poison();
		}
		if (DEBUG) {
			System.out.println("Exiting SimSystem.run()");
		}
	}

	//
	// Package level methods
	//

	boolean running() {
		return running;
	}

	// Entity helper methods

	synchronized RemoveToken hold(int src, double delay) {
		SimEvent e = new SimEvent(SimEvent.HOLD_DONE, clock + delay, src);
		future.add(e);
		return new RemoveToken(e);
	}

	synchronized RemoveToken send(int src, int dest, double delay, int tag, Object data) {
		SimEvent e = new SimEvent(SimEvent.SEND, clock + delay, src, dest, tag, data);
		if ((tag & NetEvent.EVENT_MASK) == NetEvent.EVENT_TIMING) {
			timing.add(e);
		} else {
			future.add(e);
		}
		return new RemoveToken(e);
	}

	/**
	 * Given a remove token, this method will remove a future or deferred simulation event
	 * @param token the remove token
	 * @return true if the event was found and removed, false otherwise.
	 */
	synchronized boolean remove(RemoveToken token) {
		SimEvent e = token.getEvent();
		if (token.isDeferred()) {
			return deferred.remove(e);
		} else {
			if ((e.getTag() & NetEvent.EVENT_MASK) == NetEvent.EVENT_TIMING) {
				return timing.remove(e);
			} else {
				return future.remove(e);
			}
		}
	}

	synchronized void wait(int src) {

	}

	synchronized int waiting(int d, SimPredicate p) {
		SimEvent ev = null;
		int w = 0;
		for (Object element : deferred) {
			ev = (SimEvent) element;
			if (ev.getDest() == d) {
				if (p.match(ev)) {
					w++;
				}
			}
		}
		return w;
	}

	synchronized void select(int src, SimPredicate p) {
		SimEvent ev = null;
		boolean found = false;

		// retrieve + remove event with dest == src
		for (Iterator<SimEvent> it = deferred.iterator(); it.hasNext() && !found;) {
			ev = it.next();
			if (ev.getDest() == src) {
				if (p.match(ev)) {
					deferred.remove(ev);
					found = true;
				}
			}
		}

		if (found) {
			entities.get(src).setEvbuf((SimEvent) ev.clone());
		} else {
			entities.get(src).setEvbuf(null);
		}
	}

	synchronized void cancel(int src, SimPredicate p) {
		SimEvent ev = null;
		boolean found = false;

		// retrieves + remove event with dest == src
		for (Iterator<SimEvent> it = future.iterator(); it.hasNext() && !found;) {
			ev = it.next();
			if (ev.getSrc() == src) {
				if (p.match(ev)) {
					it.remove();
					found = true;
				}
			}
		}

		if (found) {
			entities.get(src).setEvbuf((SimEvent) ev.clone());
		} else {
			entities.get(src).setEvbuf(null);
		}
	}

	RemoveToken putback(SimEvent ev) {
		deferred.add(ev);
		return new RemoveToken(ev, true);
	}

	//
	// Private internal methods
	//

	private void processEvent(SimEvent e) throws NetException {
		int dest, src;
		SimEntity destEnt;

		// Update the system's clock
		if (e.eventTime() < clock) {
			throw new NetException("SimSystem: Error - past event detected!\n" + "Time: " + clock + ", event time: "
					+ e.eventTime() + ", event type: " + e.getType() + future);
		}
		clock = e.eventTime();

		// Ok now process it
		switch (e.getType()) {

		case (SimEvent.SEND):
			// Checks for matching wait
			dest = e.getDest();
			if (dest < 0) {
				throw new NetException("SimSystem: Error - attempt to send to a null entity");
			} else {
				destEnt = entities.get(dest);
				if (destEnt.getState() == SimEntity.WAITING) {
					SimPredicate p = destEnt.getWaitingPred();
	
					if (p == null) {
						//the entity was waiting for a generic predicate
						destEnt.setEvbuf((SimEvent) e.clone());
						destEnt.setState(SimEntity.RUNNABLE);
						try {
							destEnt.execute();
						} catch (NetException e1) {
							abort();
							throw e1;
						}
					} else {
						//the entity was waiting for events with a specified predicate
						//this event matches with such predicate??
						if (destEnt.getWaitingPred().match(e)) {
							p = null;
							destEnt.setEvbuf((SimEvent) e.clone());
							destEnt.setState(SimEntity.RUNNABLE);
							try {
								destEnt.execute();
							} catch (NetException e1) {
								abort();
								throw e1;
							}
						} else {
							//the event does not match with the predicate, so it is put in the deferred queue
							destEnt.simPutback(e);
						}
					}
				} else {
					//if the entity is not WAITING the event is put in the deferred queue
					deferred.add(e);
				}
			}
			break;

		case (SimEvent.ENULL):
			throw new NetException("SimSystem: Error - event has a null type");

		case (SimEvent.CREATE):
			SimEntity newe = (SimEntity) e.getData();
			addEntityDynamically(newe);
			break;

		case (SimEvent.HOLD_DONE):
			src = e.getSrc();
			if (src < 0) {
				throw new NetException("SimSystem: Error - NULL entity holding");
			} else {
				entities.get(src).setState(SimEntity.RUNNABLE);
				entities.get(src).restart();
			}
			break;
		}
	}

	/**
	 * Aborts the simulation!
	 *
	 */
	public void abort() {
		running = false;
		if (DEBUG) {
			System.out.println("Simulation Aborted");
		}
	}

	public void setNetSystem(NetSystem netSystem) {
		this.netSystem = netSystem;
	}

}
