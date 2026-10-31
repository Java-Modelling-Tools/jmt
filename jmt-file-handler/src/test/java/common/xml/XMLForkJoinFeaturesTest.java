package common.xml;

import static common.xml.AdvancedFeatureSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Vector;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import common.CommonConstants;
import common.definitions.CommonModel;
import common.distributions.Exponential;
import common.forkStrategies.ClassSwitchFork;
import common.forkStrategies.CombFork;
import common.forkStrategies.ForkStrategy;
import common.forkStrategies.MultiBranchClassSwitchFork;
import common.forkStrategies.OutPath;
import common.forkStrategies.ProbabilitiesFork;
import common.joinStrategies.GuardJoin;
import common.joinStrategies.JoinStrategy;
import common.joinStrategies.NormalJoin;
import common.joinStrategies.PartialJoin;

/**
 * Fork / join / scaler variants no bundled example uses, built through the CommonModel API on top of an inline
 * {@code <sim>} model: every model is written, schema validated, checked in the DOM, reloaded and written again.
 */
class XMLForkJoinFeaturesTest {

	/** Src -> S -> F -> {A, B} -> J -> Out with two open classes C and D. */
	private static CommonModel forkJoin() {
		CommonModel m = base();
		Object d = m.addClass("D", CommonConstants.CLASS_TYPE_OPEN, Integer.valueOf(0), Double.valueOf(0), null, new Exponential());
		m.setClassRefStation(d, st(m, "Src"));
		m.setConnected(st(m, "S"), st(m, "Out"), false);
		add(m, "F", CommonConstants.STATION_TYPE_FORK);
		add(m, "A", SERVER);
		add(m, "B", SERVER);
		add(m, "J", CommonConstants.STATION_TYPE_JOIN);
		connect(m, "S", "F");
		connect(m, "F", "A");
		connect(m, "F", "B");
		connect(m, "A", "J");
		connect(m, "B", "J");
		connect(m, "J", "Out");
		return m;
	}

	/**
	 * Selects the fork kind for class C and returns the strategy object stored in the model. CommonModel keeps the
	 * current object when it is "equal" (same kind), so callers must mutate the returned object, not the prototype.
	 */
	private static ForkStrategy installFork(CommonModel m, ForkStrategy proto) {
		m.setForkStrategy(st(m, "F"), cl(m, "C"), proto.clone());
		ForkStrategy fs = forkOf(m, "C");
		Vector<Object> classes = m.getClassKeys();
		if (fs.getOutDetails() == null || fs.getOutDetails().isEmpty()) {
			fs.addStation(st(m, "A"), cl(m, "C"), classes);
			fs.addStation(st(m, "B"), cl(m, "C"), classes);
		}
		return fs;
	}

	private static ForkStrategy forkOf(CommonModel m, String cls) {
		return (ForkStrategy) m.getForkStrategy(st(m, "F"), cl(m, cls));
	}

	@Test
	void defaultForkJoinModelRoundTrips() throws Exception {
		CommonModel back = roundTrip(forkJoin());
		assertEquals(CommonConstants.STATION_TYPE_FORK, back.getStationType(st(back, "F")));
		assertEquals(CommonConstants.STATION_TYPE_JOIN, back.getStationType(st(back, "J")));
	}

	@Test
	void randomSubsetForkKeepsBranchCountProbabilities() throws Exception {
		CommonModel m = forkJoin();
		CombFork fs = (CombFork) installFork(m, CommonConstants.FORK_COMBINATION);
		fs.getOutDetails().put("1", Double.valueOf(0.25));
		fs.getOutDetails().put("2", Double.valueOf(0.75));

		Element e = section(doc(m), "F", "Fork");
		assertEquals("NetStrategies.ForkStrategies.CombFork", param(e, "Random Subset").getAttribute("classPath"));
		assertEquals("0.25", value(e, "probability"));

		CommonModel back = roundTrip(m);
		ForkStrategy got = forkOf(back, "C");
		assertInstanceOf(CombFork.class, got);
		assertEquals(0.25, ((Map<Object, Double>) got.getOutDetails()).get("1"), 1e-12);
		assertEquals(0.75, ((Map<Object, Double>) got.getOutDetails()).get("2"), 1e-12);
	}

	@Test
	void branchProbabilityForkKeepsOutPathsAndJobsPerLink() throws Exception {
		CommonModel m = forkJoin();
		ProbabilitiesFork fs = (ProbabilitiesFork) installFork(m, CommonConstants.FORK_PROBABILITIES);
		OutPath toA = fs.getOutDetails().get(st(m, "A"));
		toA.setProbability(Double.valueOf(0.4));
		toA.getOutParameters().clear();
		toA.putEntry(1, 0.5);
		toA.putEntry(2, 0.5);

		Document d = doc(m);
		assertNotNull(param(section(d, "F", "Fork"), "JobsPerLinkDis"));
		CommonModel back = roundTrip(m);
		ProbabilitiesFork got = (ProbabilitiesFork) forkOf(back, "C");
		assertEquals(2, got.getOutDetails().size());
		boolean found = false;
		for (OutPath p : got.getOutDetails().values()) {
			if (Math.abs(p.getProbability() - 0.4) < 1e-12) {
				found = true;
				assertEquals(0.5, ((Map<Object, Double>) (Map) p.getOutParameters()).get(2), 1e-12);
			}
		}
		assertTrue(found, "the 0.4 branch survives the round trip: " + got.getOutDetails().values().iterator().next().getProbability());
	}

	@Test
	void classSwitchForkKeepsJobsPerClassAndBranch() throws Exception {
		CommonModel m = forkJoin();
		ClassSwitchFork fs = (ClassSwitchFork) installFork(m, CommonConstants.FORK_CLASS_SWITCH);
		fs.getOutDetails().get(st(m, "B")).getOutParameters().put(cl(m, "D"), 3);

		Element e = section(doc(m), "F", "Fork");
		assertEquals("NetStrategies.ForkStrategies.ClassSwitchFork", param(e, "Class Switch").getAttribute("classPath"));
		assertNotNull(param(e, "ClassJobNumArray"));

		CommonModel back = roundTrip(m);
		ClassSwitchFork got = (ClassSwitchFork) forkOf(back, "C");
		OutPath toB = got.getOutDetails().get(st(back, "B"));
		assertEquals(3, ((Map<Object, Integer>) (Map) toB.getOutParameters()).get(cl(back, "D")).intValue());
		assertEquals(1, ((Map<Object, Integer>) (Map) toB.getOutParameters()).get(cl(back, "C")).intValue());
	}

	@Test
	void multiBranchClassSwitchForkRoundTrips() throws Exception {
		CommonModel m = forkJoin();
		MultiBranchClassSwitchFork fs = (MultiBranchClassSwitchFork) installFork(m, CommonConstants.FORK_MULTI_BRANCH_CLASS_SWITCH);
		fs.getOutDetails().get(st(m, "A")).getOutParameters().put(cl(m, "D"), 2);
		CommonModel back = roundTrip(m);
		MultiBranchClassSwitchFork got = (MultiBranchClassSwitchFork) forkOf(back, "C");
		OutPath toA = got.getOutDetails().get(st(back, "A"));
		assertEquals(2, ((Map<Object, Integer>) (Map) toA.getOutParameters()).get(cl(back, "D")).intValue());
	}

	@Test
	void forkBlockAndSimplifiedForkAreWritten() throws Exception {
		CommonModel m = forkJoin();
		m.setForkBlock(st(m, "F"), Integer.valueOf(4));
		m.setIsSimplifiedFork(st(m, "F"), Boolean.TRUE);
		CommonModel back = roundTrip(m);
		assertEquals(Integer.valueOf(4), back.getForkBlock(st(back, "F")));
		assertEquals(Boolean.TRUE, back.getIsSimplifiedFork(st(back, "F")));
	}

	@Test
	void quorumJoinKeepsRequiredNumber() throws Exception {
		CommonModel m = forkJoin();
		PartialJoin js = (PartialJoin) CommonConstants.JOIN_PARTIAL.clone();
		js.setRequiredNum(2);
		m.setJoinStrategy(st(m, "J"), cl(m, "C"), js);

		Element e = section(doc(m), "J", "Join");
		assertEquals("2", value(e, "numRequired"));
		CommonModel back = roundTrip(m);
		JoinStrategy got = (JoinStrategy) back.getJoinStrategy(st(back, "J"), cl(back, "C"));
		assertInstanceOf(PartialJoin.class, got);
		assertEquals(2, got.getRequiredNum());
	}

	@Test
	void standardJoinIsWrittenWithRequiredNumber() throws Exception {
		CommonModel m = forkJoin();
		m.setJoinStrategy(st(m, "J"), cl(m, "C"), new NormalJoin());
		CommonModel back = roundTrip(m);
		assertInstanceOf(NormalJoin.class, back.getJoinStrategy(st(back, "J"), cl(back, "C")));
	}

	@Test
	void guardJoinKeepsRequiredJobsPerClass() throws Exception {
		CommonModel m = forkJoin();
		GuardJoin js = (GuardJoin) CommonConstants.JOIN_MIX.clone();
		Map<Object, Integer> guard = new java.util.HashMap<Object, Integer>();
		guard.put(cl(m, "C"), 1);
		guard.put(cl(m, "D"), 2);
		js.setGuard(guard);
		m.setJoinStrategy(st(m, "J"), cl(m, "C"), js);

		Element e = section(doc(m), "J", "Join");
		assertNotNull(param(e, "Classes"));
		assertNotNull(param(e, "Numbers"));
		CommonModel back = roundTrip(m);
		GuardJoin got = (GuardJoin) back.getJoinStrategy(st(back, "J"), cl(back, "C"));
		assertEquals(2, got.getGuard().get(cl(back, "D")).intValue());
		assertEquals(1, got.getGuard().get(cl(back, "C")).intValue());
	}
}
