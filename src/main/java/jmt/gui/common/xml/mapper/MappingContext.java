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

    // Il modello che verrà popolato durante la pipeline
    private final CommonModel model;

    // --- Cache principali per chiavi e nomi ---
    public final Map<String, Object> classes = new TreeMap<>();
    public final Map<String, Object> stations = new TreeMap<>();
    public final Map<String, Object> regions = new TreeMap<>();

    // --- Riferimenti e liste di transizioni ---
    public final Map<Object, String> refStations = new HashMap<>();
    public final Map<Object, List<String>> modeNameLists = new HashMap<>();

    // --- Mappe per il Post-Processing: Routing e Fork ---
    public final Map<Object[], Map<String, Double>> empiricalRouting = new HashMap<>();
    public final Map<Object[], Map<String, Double>> empiricalLDRouting = new HashMap<>();
    public final Map<Object[], Map<String, Integer>> wrrRouting = new HashMap<>();
    public final Map<Object[], Map<String, Double>> csBranchRouting = new HashMap<>();
    public final Map<Object[], Map<String, Map<String, Double>>> csClassRouting = new HashMap<>();
    public final Map<Object[], Map<String, OutPath>> empiricalFork = new HashMap<>();
    public final Map<Object[], Map<String, Double>> combFork = new HashMap<>();

    // --- Mappe per il Post-Processing: Condizioni ---
    public final Map<Object[], Integer> enablingConditionMap = new HashMap<>();
    public final Map<Object[], Integer> inhibitingConditionMap = new HashMap<>();
    public final Map<Object[], Integer> resourceConditionMap = new HashMap<>();
    public final Map<Object[], Integer> firingOutcomeMap = new HashMap<>();

    // --- Cache di lookup per le strategie GUI vs Engine ---
    // Inizializzate lazy come in OLDXMLReader per risparmiare memoria se non usate
    public Map<String, Distribution> engineToGuiDistr = null;
    public Map<String, RoutingStrategy> engineToGuiRouting = null;
    public Map<String, ForkStrategy> engineToGuiFork = null;
    public Map<String, JoinStrategy> engineToGuiJoin = null;
    public Map<String, SemaphoreStrategy> engineToGuiSemaphore = null;

    /**
     * Ritorna l'istanza del CommonModel che i vari Mapper devono arricchire.
     */
    public CommonModel getModel() {
        return model;
    }
}