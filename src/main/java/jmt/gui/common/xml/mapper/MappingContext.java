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
    private Map<String, Distribution> engineToGuiDistr = null;
    private Map<String, RoutingStrategy> engineToGuiRouting = null;
    private Map<String, ForkStrategy> engineToGuiFork = null;
    private Map<String, JoinStrategy> engineToGuiJoin = null;
    private Map<String, SemaphoreStrategy> engineToGuiSemaphore = null;

    /**
     * Returns the CommonModel instance that the various Mappers must enrich.
     */
    public CommonModel getModel() {
        return model;
    }

    public Map<String, Distribution> getEngineToGuiDistr() {
        if (engineToGuiDistr == null) {
            engineToGuiDistr = new TreeMap<>();
            for (Distribution d : Distribution.findAll()) {
                engineToGuiDistr.put(d.getClassPath(), d);
            }
            for (Distribution d : Distribution.findAllDiscrete()) {
                engineToGuiDistr.put(d.getClassPath(), d);
            }
        }
        return engineToGuiDistr;
    }

    public Map<String, RoutingStrategy> getEngineToGuiRouting() {
        if (engineToGuiRouting == null) {
            engineToGuiRouting = new TreeMap<>();
            for (RoutingStrategy rs : RoutingStrategy.findAll()) {
                engineToGuiRouting.put(rs.getClass().getName(), rs);
            }
        }
        return engineToGuiRouting;
    }

    public Map<String, ForkStrategy> getEngineToGuiFork() {
        if (engineToGuiFork == null) {
            engineToGuiFork = new TreeMap<>();
            for (ForkStrategy fs : ForkStrategy.findAll()) {
                engineToGuiFork.put(fs.getClass().getName(), fs);
            }
        }
        return engineToGuiFork;
    }

    public Map<String, JoinStrategy> getEngineToGuiJoin() {
        if (engineToGuiJoin == null) {
            engineToGuiJoin = new TreeMap<>();
            for (JoinStrategy js : JoinStrategy.findAll()) {
                engineToGuiJoin.put(js.getClass().getName(), js);
            }
        }
        return engineToGuiJoin;
    }

    public Map<String, SemaphoreStrategy> getEngineToGuiSemaphore() {
        if (engineToGuiSemaphore == null) {
            engineToGuiSemaphore = new TreeMap<>();
            for (SemaphoreStrategy ss : SemaphoreStrategy.findAll()) {
                engineToGuiSemaphore.put(ss.getClass().getName(), ss);
            }
        }
        return engineToGuiSemaphore;
    }
}