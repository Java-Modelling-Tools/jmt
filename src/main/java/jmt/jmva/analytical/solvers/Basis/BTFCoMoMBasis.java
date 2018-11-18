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

package jmt.jmva.analytical.solvers.Basis;

import jmt.jmva.analytical.solvers.Basis.Comparators.SortVectorByhThenRmnz;
import jmt.jmva.analytical.solvers.DataStructures.PopulationChangeVector;
import jmt.jmva.analytical.solvers.DataStructures.QNModel;
import jmt.jmva.analytical.solvers.Exceptions.InternalErrorException;
import jmt.jmva.analytical.solvers.Utilities.MiscFunctions;

/**
 *A concrete implementation of the CoMoMBasis that orders the basis in the way required for the BTF linear systems
 * @author Jack Bradshaw
 */
public class BTFCoMoMBasis extends CoMoMBasis {

	public BTFCoMoMBasis(QNModel m) {
		super(m);
		setComparator(new SortVectorByhThenRmnz());
	}

	@Override
	public int indexOf(PopulationChangeVector n, int m) throws InternalErrorException {
		//Find the position of the n vector in the ordering
		int population_position = order.indexOf(n);
		int queue_added = m;

		if (population_position == -1) throw new InternalErrorException("Invalid PopulationChangeVector:" + n);

		//No queues added, constant is in Lambda_Y
		if (queue_added == 0) {
			return MiscFunctions.binomialCoefficient(M + R - 1, M)*M + population_position;
		} 

		//Queue added, constant is in Lambda_X
		return population_position * M + queue_added - 1;
	}

}
