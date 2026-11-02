package QueueNet;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jmt.common.ErrorReporters;

/** Whole-network runs (fixed seed, single threaded, capped simulated time) exercising QueueNet and NodeSections end to end. */
@Tag("simulation")
class NetworkRunTest {
	private static final long SEED = 4242L;
	private static final int MAX_EVENTS = 400000;

	@TempDir
	Path dir;

	@AfterEach
	void restore() {
		ErrorReporters.set(null);
	}

	@ParameterizedTest
	@ValueSource(strings = {"1Fork2JoinOK", "2Cl_1server_K1", "2FJ_2tasks_nestedOK", "3cl_5stat_loadbalNEW", "ERR_160106_5servAllDetNEW_NEW", "ExpExp1_Pree_1cl", "FCR_bugPerfInd1FCR", "GREGerror", "HO5-hadoop", "LDerror", "LPS_C3_K2_OK", "NOTgoodRANDOM_FCFS_New", "PhTypeconERROREsuiRdT20_6", "SEED1test_3Job2Cl2CS2FJ_2NEWparameters", "Sec2_4_OptPower_JSIMg", "Sec5_3_FastTask_Quorum1_Wif_JSIMg", "Sec6_4_Obj1_UnavCV15_S60_", "burst-general-01", "burst-mmpp-01", "case3_bas_sd", "classswitch", "closed_1class_2stat", "closed_2class_2stat", "closed_2class_2stat_switch", "closed_2class_3stat_switch", "conBeta13fescNEW", "convers_test1LDcrescNEW", "cpn", "delay-closed", "fcfs-01", "gamma-01", "indices-01", "ispass03", "ispass03-02", "open_1class_1stat_mg1fcfs", "open_1class_1stat_mg1fcfs_2", "open_1class_1stat_mg1ps", "open_1class_1stat_mm1k", "open_1class_1stat_mm1k_fcr", "open_1class_3stat_advfork", "open_1class_3stat_fork", "open_2class_3stat_fcr", "open_2class_3stat_fcrNEW", "open_2class_3stat_fork_whatif", "open_3class_3stat_loadbal", "open_3class_3stat_loadbalNEW", "open_3class_4stat", "pareto-01", "pepsy-bcmp2", "philosophers", "philosophers_2", "preemptive", "terminal", "polling", "producer_consumer", "producer_consumer_2", "ps-01", "ps-priority", "readers_writers", "region", "simple_cpn", "simple_gspn"})
	void modelRunsToCompletionAndWritesResults(String model) throws Exception {
		NetFixtures.Run r = NetFixtures.run(dir, model, SEED, MAX_EVENTS);
		assertTrue(r.sim.hasFinished(), model + " finished");
		assertNotNull(r.output, model + " wrote its results");
		assertTrue(r.output.length() > 0);
		assertTrue(r.network.getNodes().size() >= 2);
	}

	@Test
	void sameSeedGivesSameArrivalCounts() throws Exception {
		Path d1 = java.nio.file.Files.createDirectory(dir.resolve("a"));
		Path d2 = java.nio.file.Files.createDirectory(dir.resolve("b"));
		NetFixtures.Run r1 = NetFixtures.run(d1, "fcfs-01", SEED, MAX_EVENTS);
		NetFixtures.Run r2 = NetFixtures.run(d2, "fcfs-01", SEED, MAX_EVENTS);
		int in1 = r1.node("Queue 1").getJobInfoList().getJobsIn();
		assertTrue(in1 > 0);
		assertEquals(in1, r2.node("Queue 1").getJobInfoList().getJobsIn());
	}
}
