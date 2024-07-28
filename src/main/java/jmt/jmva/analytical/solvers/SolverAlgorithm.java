/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package jmt.jmva.analytical.solvers;

import java.util.*;

/**
 * Enum class for all implemented algorithm solvers
 *
 * @author Abhimanyu Chugh
 */
public enum SolverAlgorithm {
    //enum format:
	// algorithmName, closed, open, exact, iterative, loadDependent, priority
    
    // EXACT
    EXACT("MVA", true, true, true, false, true, false),
    // TODO: Update below algorithms to use new constructor instead of the old constructor as a wrapper
    RECAL("RECAL", true, false, true, false, false, false),
    //MOM("MoM", true, true, false),
    COMOM("CoMoM", true, false, true, false, false, false),
    //RGF("RGF", true, true, false), /* Looks buggy */
    TREE_MVA("Tree MVA", true, false, true, false, false, false),
    //TREE_CONV("Tree Convolution", true, true, false), /* Looks buggy */
    //MIXED("Mixed MVA", true, true, true, false, false, false),
    //OPEN("Open MVA", false, true, true, false, false, false),

    // APPROXIMATE
    CHOW("Chow", true, false, false, true, false, false),
    BARD_SCHWEITZER("Bard-Schweitzer", true, false, false, true, false, false),
    AQL("AQL", true, false, false, true, false, false),
    LINEARIZER("Linearizer", true, false, false, true, false, false),
    DESOUZA_MUNTZ_LINEARIZER("De Souza-Muntz Linearizer", true, false, false, true, false, false),
    //MONTE_CARLO("Monte Carlo", true, false, false), /* Looks buggy */
    MONTE_CARLO_LOGISTIC("Logistic Sampling", true, false, false, true, false, false),

    // Priority
    // PrioMVA's - can be LD in same way that MixedMVA can be, not implemented though
    PRIO_MVA_BKT("Bryant-Krzesinski-Teunissen", true, true, false, false, false, true),
    PRIO_MVA_CL("Chandy-Lakshmi", true, true, false, false, false, true),
    //PRIO_MVA_T("Teunissen", true, true, false, false, false, true),
    // If mixed MVA implements LD then SCA can be LD, shouldn't really be used with open
    SCA("Shadow Server", true, true, false, true, false, true);
    //PRIO_MVA_CL_Z("PRIOMVA - CL:Z", true, true, false, false, false, true),
    //PRIO_MVA_CL_Y("PRIOMVA - CL:Y", true, true, false, false, false, true),
    //PRIO_MVA_NLI_LI("PRIOMVA - NLI:LI", true, true, false, false, false, true),
    //PRIO_MVA_NLI_Z("PRIOMVA - NLI:Z", true, true, false, false, false, true),
    //PRIO_MVA_NLI_Y("PRIOMVA - NLI:Y", true, true, false, false, false, true),
    // NC is No Change
    //PRIO_MVA_RNLI_NC("PRIOMVA - RNLI", true, true, false, false, false, true),
    //PRIO_MVA_RNLI_LI("PRIOMVA - RNLI:LI", true, true, false, false, false, true),
    //PRIO_MVA_RNLI_Z("PRIOMVA - RNLI:Z", true, true, false, false, false, true),
    //PRIO_MVA_RNLI_Y("PRIOMVA - RNLI:Y", true, true, false, false, false, true);

    // string representation of the algorithm
    private final String algorithmName;
    // If the algorithm can handle closed classes
    private final boolean closed;
    // If the algorithm can handle open classes
    private final boolean open;
    private final boolean exact;
    private final boolean loadDependent;
    private final boolean priority;
    // Some algorithms such as PRIO_MVA aren't exact but aren't iterative
    // Means the algorithm will need either max num. samples or a tolerance
    private final boolean iterative;

    private static final String[] NAMES;
    private static final SolverAlgorithm[] CLOSED_VALUES;
    private static final SolverAlgorithm[] OPEN_VALUES;
    private static final SolverAlgorithm[] MIXED_VALUES;
    private static final Map<String, SolverAlgorithm> REVERSE_MAP;

    static {
        HashMap<String, SolverAlgorithm> revMap = new HashMap<String, SolverAlgorithm>();
        NAMES = new String[SolverAlgorithm.values().length];
        ArrayList<SolverAlgorithm> closedValues = new ArrayList<SolverAlgorithm>();
        ArrayList<SolverAlgorithm> openValues = new ArrayList<>();
        ArrayList<SolverAlgorithm> mixedValues = new ArrayList<>();
        for (int i = 0; i < NAMES.length; i++) {
            SolverAlgorithm algo = SolverAlgorithm.values()[i];
            NAMES[i] = SolverAlgorithm.values()[i].toString();
            if (algo.supportsClosedClasses()) {
                closedValues.add(algo);
                if (algo.supportsOpenClasses()) {
                    mixedValues.add(algo);
                }
            }
            if (algo.supportsOpenClasses()) {
                openValues.add(algo);
            }
            revMap.put(algo.toString(), algo);
        }
        CLOSED_VALUES = closedValues.toArray(new SolverAlgorithm[closedValues.size()]);
        OPEN_VALUES = openValues.toArray(new SolverAlgorithm[openValues.size()]);
        MIXED_VALUES = mixedValues.toArray(new SolverAlgorithm[mixedValues.size()]);
        REVERSE_MAP = Collections.unmodifiableMap(revMap);
    }

    private SolverAlgorithm(String algorithmName, boolean closed, boolean open, boolean exact, boolean iterative, boolean loadDependent, boolean priority) {
        this.algorithmName = algorithmName;
        this.closed = closed;
        this.open = open;
        this.exact = exact;
        this.iterative = iterative;
        this.loadDependent = loadDependent;
        this.priority = priority;
    }

    // Allows for backwards compatibility, please don't use this constructor and use the more up to date one above
    // If swapping to the new constructor, be careful as the order of the arguments changes as new ones are added
    private SolverAlgorithm(String algorithmName, boolean closed, boolean exact, boolean loadDependent) {
        this(algorithmName, closed, false, exact, true, loadDependent, false);
    }


    @Override
    public String toString() {
        return algorithmName;
    }

    /**
     * @return true if this algorithm supports closed classes
     */
    public boolean supportsClosedClasses() {
        return closed;
    }

    /**
     * @return true if this algorithm supports open classes
     */
    public boolean supportsOpenClasses() {
        return open;
    }

    /**
     * @return true if this algorithm is exact
     */
    public boolean isExact() {
        return exact;
    }

    /**
     * @return true if the algorithm supports load dependent, false otherwise.
     */
    public boolean supportsLoadDependent() {
        return loadDependent;
    }

    /**
     * @return true if the algorithm supports priorities, false otherwise.
     */
    public boolean supportsPriorities() {
        return priority;
    }

    /**
     * @return true if the algorithm is iterative, false otherwise.
     */
    public boolean isIterative() {
        return iterative;
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
     * Returns an array of all closed SolverAlgorithm values
     */
    public static SolverAlgorithm[] closedValues() {
        return CLOSED_VALUES;
    }

    /**
     * Returns an array of all open SolverAlgorithm values
     */
    public static SolverAlgorithm[] openValues() {
        return OPEN_VALUES;
    }

    /**
     * Returns an array of all mixed SolverAlgorithm values
     */
    public static SolverAlgorithm[] mixedValues() {
        return MIXED_VALUES;
    }

}
