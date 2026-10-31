package solvers.utilities;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import analytical.ExactConstants;
import solvers.Solver;
import solvers.SolverMulti;

class ConstantConvertorsTest {

	@Test
	void classTypesAreRemappedBetweenNumberingSchemes() {
		// ExactConstants: CLOSED=0, OPEN=1; SolverMulti: OPEN=0, CLOSED=1 (the schemes are swapped)
		int[] res = ConstantConvertors.mapClassTypes(new int[] { ExactConstants.CLASS_CLOSED, ExactConstants.CLASS_OPEN, 7 });
		assertArrayEquals(new int[] { SolverMulti.CLOSED_CLASS, SolverMulti.OPEN_CLASS, -1 }, res);
		assertEquals(1, res[0]);
		assertEquals(0, res[1]);
		assertEquals(0, ConstantConvertors.mapClassTypes(new int[0]).length);
	}

	@Test
	void stationTypesMultiClass() {
		int[] in = { ExactConstants.STATION_DELAY, ExactConstants.STATION_LI, ExactConstants.STATION_LD, ExactConstants.STATION_PRS,
				ExactConstants.STATION_HOL, 99 };
		assertArrayEquals(new int[] { SolverMulti.DELAY, SolverMulti.LI, SolverMulti.LD, SolverMulti.PRS, SolverMulti.HOL, -1 },
				ConstantConvertors.mapStationTypes(in, true));
	}

	@Test
	void stationTypesSingleClass() {
		int[] in = { ExactConstants.STATION_DELAY, ExactConstants.STATION_LI, ExactConstants.STATION_LD, ExactConstants.STATION_PRS,
				ExactConstants.STATION_HOL, -3 };
		assertArrayEquals(new int[] { Solver.DELAY, Solver.LI, Solver.LD, Solver.PRS, Solver.HOL, -1 },
				ConstantConvertors.mapStationTypes(in, false));
	}
}
