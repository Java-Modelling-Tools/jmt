package simEngine;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import QueueNet.SimConstants;

class EngineUtilsTest {

	@Test
	void everyMeasureNameDecodesAndEncodesBack() throws Exception {
		Set<Integer> seen = new HashSet<Integer>();
		for (Field f : SimConstants.class.getFields()) {
			if (Modifier.isStatic(f.getModifiers()) && f.getName().startsWith("MEASURE_") && f.getType() == String.class) {
				String name = (String) f.get(null);
				int type = EngineUtils.decodeMeasureType(name);
				assertEquals(name, EngineUtils.encodeMeasureType(type), f.getName());
				assertTrue(seen.add(type), "two names decode to " + type);
			}
		}
		assertEquals(37, seen.size());
	}

	@Test
	void unknownValuesFallBackToQueueLength() {
		assertEquals(SimConstants.QUEUE_LENGTH, EngineUtils.decodeMeasureType("nonsense"));
		assertEquals(SimConstants.MEASURE_QL, EngineUtils.encodeMeasureType(-5));
	}

	@Test
	void inverseMeasuresAreTheRateLikeOnes() {
		int[] inverse = { SimConstants.ARRIVAL_RATE, SimConstants.THROUGHPUT, SimConstants.DROP_RATE, SimConstants.BALKING_RATE,
				SimConstants.RENEGING_RATE, SimConstants.RETRIAL_ATTEMPTS_RATE, SimConstants.SYSTEM_THROUGHPUT,
				SimConstants.SYSTEM_DROP_RATE, SimConstants.SYSTEM_BALKING_RATE, SimConstants.SYSTEM_RENEGING_RATE,
				SimConstants.SYSTEM_RETRIAL_ATTEMPTS_RATE, SimConstants.SYSTEM_POWER, SimConstants.THROUGHPUT_PER_SINK,
				SimConstants.FIRING_THROUGHPUT };
		for (int t : inverse) {
			assertTrue(EngineUtils.isInverseMeasure(t), "" + t);
		}
		assertFalse(EngineUtils.isInverseMeasure(SimConstants.QUEUE_LENGTH));
		assertFalse(EngineUtils.isInverseMeasure(SimConstants.UTILIZATION));
		assertFalse(EngineUtils.isInverseMeasure(SimConstants.RESPONSE_TIME));
	}
}
