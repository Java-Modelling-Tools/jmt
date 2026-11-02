/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */
package random;

import jmt.common.exception.IncorrectDistributionParameterException;
import random.engine.MersenneTwister;

/** Seeded sampling helpers shared by the distribution tests. */
final class SamplingSupport {

	static final int N = 100000;

	private SamplingSupport() {
	}

	static double[] draw(Distribution d, Parameter p, long seed, int n) throws IncorrectDistributionParameterException {
		d.setRandomEngine(new MersenneTwister(seed));
		double[] out = new double[n];
		for (int i = 0; i < n; i++) {
			out[i] = d.nextRand(p);
		}
		return out;
	}

	static double mean(double[] x) {
		double s = 0;
		for (double v : x) {
			s += v;
		}
		return s / x.length;
	}
}
