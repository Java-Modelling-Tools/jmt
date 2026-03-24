package jmt.gui.common.xml.mapper;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.forkStrategies.ForkStrategy;
import jmt.gui.common.forkStrategies.OutPath;
import jmt.gui.common.joinStrategies.JoinStrategy;
import jmt.gui.common.routingStrategies.RoutingStrategy;
import jmt.gui.common.semaphoreStrategies.SemaphoreStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MappingContext {

    public MappingContext(CommonModel model) {
        this.model = model;
    }

    // The model that will be populated during the pipeline
    private final CommonModel model;

    // --- Main caches for keys and names ---
    public final Map<String, Object> classes = new TreeMap<>();
    public final Map<String, Object> stations = new TreeMap<>();
    public final Map<String, Object> regions = new TreeMap<>();

    // --- References and transition lists ---
    public final Map<Object, String> refStations = new HashMap<>();
    public final Map<Object, List<String>> modeNameLists = new HashMap<>();

    // --- Maps for Post-Processing: Routing and Fork ---
    public final Map<Object[], Map<String, Double>> empiricalRouting = new HashMap<>();
    public final Map<Object[], Map<String, Double>> empiricalLDRouting = new HashMap<>();
    public final Map<Object[], Map<String, Integer>> wrrRouting = new HashMap<>();
    public final Map<Object[], Map<String, Double>> csBranchRouting = new HashMap<>();
    public final Map<Object[], Map<String, Map<String, Double>>> csClassRouting = new HashMap<>();
    public final Map<Object[], Map<String, OutPath>> empiricalFork = new HashMap<>();
    public final Map<Object[], Map<String, Double>> combFork = new HashMap<>();

    // --- Maps for Post-Processing: Conditions ---
    public final Map<Object[], Integer> enablingConditionMap = new HashMap<>();
    public final Map<Object[], Integer> inhibitingConditionMap = new HashMap<>();
    public final Map<Object[], Integer> resourceConditionMap = new HashMap<>();
    public final Map<Object[], Integer> firingOutcomeMap = new HashMap<>();

    // --- Lookup caches for GUI vs Engine strategies ---
    // Lazily initialized as in OLDXMLReader to save memory if not used
    public Map<String, Distribution> engineToGuiDistr = null;
    public Map<String, RoutingStrategy> engineToGuiRouting = null;
    public Map<String, ForkStrategy> engineToGuiFork = null;
    public Map<String, JoinStrategy> engineToGuiJoin = null;
    public Map<String, SemaphoreStrategy> engineToGuiSemaphore = null;

    /**
     * Returns the CommonModel instance that the various Mappers must enrich.
     */
    public CommonModel getModel() {
        return model;
    }
}