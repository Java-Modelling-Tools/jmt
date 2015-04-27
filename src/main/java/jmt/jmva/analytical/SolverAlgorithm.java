/**    
  * Copyright (C) 2012, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.

  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
  */

package jmt.jmva.analytical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Enum class for all implemented algorithm solvers
 * @author Abhimanyu Chugh, bounding algorithms added by Ben Homer
 */
public enum SolverAlgorithm {
	// Exact
	EXACT("MVA", true, Type.EXACT, true),
	RECAL("RECAL", true, Type.EXACT, false),
	MOM("MoM", true, Type.EXACT, false),
	COMOM("CoMoM", true, Type.EXACT, false),
	TREE_CONVOLUTION("Tree Convolution", true, Type.EXACT, false),
	TREE_MVA("Tree MVA", true, Type.EXACT, true),
	// Approximate
	CHOW("Chow", true, Type.APPROXIMATE, false),
	BARD_SCHWEITZER("Bard-Schweitzer", true, Type.APPROXIMATE, false),
	AQL("AQL", true, Type.APPROXIMATE, false),
	LINEARIZER("Linearizer", true, Type.APPROXIMATE, false),
	DESOUZA_MUNTZ_LINEARIZER("De Souza-Muntz Linearizer", true, Type.APPROXIMATE, false);
	// Bounding
	//ASYMPTOTIC_BOUNDS("Asymptotic Bounds", true, Type.BOUNDING, false),
	//BALANCED_JOB_BOUNDS("Balanced Job Bounds", true, Type.BOUNDING, false),
	//GEOMETRIC_BOUNDS("Geometric Bounds", true, Type.BOUNDING, false);
	
	private enum Type { EXACT, APPROXIMATE, BOUNDING };
	
	// string representation of the algorithm
	private String algorithmName;
	private boolean closed, loadDependent;
  	private Type type;
	
	private static final String[] NAMES;
	private static final String[] CLOSED_NAMES;
	private static final SolverAlgorithm[] CLOSED_VALUES;
	private static final Map<String, SolverAlgorithm> REVERSE_MAP;
	static {
		HashMap<String, SolverAlgorithm> revMap = new HashMap<String, SolverAlgorithm>();
		NAMES = new String[SolverAlgorithm.values().length];
		ArrayList<String> closedNames = new ArrayList<String>();
		ArrayList<SolverAlgorithm> closedValues = new ArrayList<SolverAlgorithm>();
		for (int i=0; i<NAMES.length;i++) {
			SolverAlgorithm algo = SolverAlgorithm.values()[i];
			NAMES[i] = SolverAlgorithm.values()[i].toString();
			if (algo.isClosed()) {
				closedNames.add(algo.toString());
				closedValues.add(algo);
			}
			revMap.put(algo.toString(), algo);
		}
		CLOSED_NAMES = closedNames.toArray(new String[closedNames.size()]);
		CLOSED_VALUES = closedValues.toArray(new SolverAlgorithm[closedValues.size()]);
		REVERSE_MAP = Collections.unmodifiableMap(revMap);
	}
	
	private SolverAlgorithm(String algorithmName, boolean closed, Type type, boolean loadDependent) {
		this.algorithmName = algorithmName;
		this.closed = closed;
		this.loadDependent = loadDependent;
    this.type = type;
	}
	
	@Override
	public String toString() {
		return algorithmName;
	}
	
	/**
	 * @return true if this algorithm is closed
	 */
	public boolean isClosed() {
		return closed;
	}
	
  /**
	 * @return true if this algorithm is exact
	 */
	public boolean isExact() {
		return type == Type.EXACT;
	}
	
	/**
	 * @return true if this algorithm is approximate
	 */
	public boolean isApproximate() {
		return type == Type.APPROXIMATE;
	}
	
	/**
	 * @return true if this is a tree algorithm
	 */
	public boolean isTreeAlg() {
		return this.equals(SolverAlgorithm.TREE_MVA) || this.equals(SolverAlgorithm.TREE_CONVOLUTION);
	}
	
	/**
	 * @return true if this algorithm is a bounding algorithm
	 */
	public boolean isBounding() {
		return type == Type.BOUNDING;
	}
	
	/**
	 * @return true if the algorithm supports load dependent, false otherwise.
	 */
	public boolean supportLoadDependent() {
		return loadDependent;
	}
	
	/**
	 * Find the enum value that corresponds to the given string
	 */
	public static SolverAlgorithm fromString(String algName) {
		return REVERSE_MAP.get(algName);
	}
	
	/**
	 * Returns an array of string representations of all potential SolverAlgorithm values
	 */
	public static String[] names() {
		return NAMES;
	}
	
	
	/**
	 * Returns an array of string representations of all closed SolverAlgorithm values
	 */
	public static String[] closedNames() {
		return CLOSED_NAMES;
	}
	
	/**
	 * Returns an array of all closed SolverAlgorithm values
	 */
	public static SolverAlgorithm[] closedValues() {
		return CLOSED_VALUES;
	}
	
	public static int noOfExactAlgs() {
		return 6;
	}
	
	public static int noOfApproximateAlgs() {
		return 5;
	}
	
	public static int noOfBoundingAlgs() {
		return 3;
	}
}
