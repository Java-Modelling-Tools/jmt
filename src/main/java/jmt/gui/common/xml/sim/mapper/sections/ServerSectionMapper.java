/**
 * This class is responsible for mapping the Server section of a node.
 * It is highly complex as it handles different server types (PS, Polling, Preemptive, Non-Preemptive),
 * vacation strategies, switchover times, setup times, and heterogeneous server definitions.
 *
 * @author Borgna Daniele
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml.sim.mapper.sections;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.definitions.ServerType;
import jmt.gui.common.definitions.VacationParameters;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.sim.dto.NodeDescriptor;
import jmt.gui.common.xml.sim.dto.ParameterDescriptor;
import jmt.gui.common.xml.sim.dto.SectionDescriptor;
import jmt.gui.common.xml.sim.mapper.MappingContext;
import jmt.gui.common.xml.sim.serializer.WritingContext;

import java.util.*;

public class ServerSectionMapper extends AbstractSectionMapper {

    // --- Engine classpaths (mirrored from XMLWriter constants) ---


    // -----------------------------------------------------------------------
    // READING PHASE (mapDtoToCommonModel)
    // -----------------------------------------------------------------------

    @Override
    public void mapDtoToCommonModel(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        mapQueueStrategyByClassName(sec.getClassName(), model, key, ctx);

        ServerMappingState state = new ServerMappingState();
        List<ParameterDescriptor> params = getFlattenedParameters(sec);

        for (ParameterDescriptor p : params) {
            mapServerParameter(p, key, model, ctx, state);
        }

        createServers(key, model, state.serverNames, state.serversPerType, state.compatibilities);
        mapHeterogeneousServers(key, model, state.heterogeneous);
    }

    // -----------------------------------------------------------------------
    // WRITING PHASE (mapCommonModelToDto)
    // -----------------------------------------------------------------------

    @Override
    public void mapCommonModelToDto(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, WritingContext context) {
        String queueStrategy = model.getStationQueueStrategy(key);

        // --- className of the section (dynamic, based on queue strategy) ---
        sec.setClassName(resolveServerClassName(queueStrategy, model, key));

        List<Object> classes = model.getClassKeys();

        // maxJobs (number of servers)
        sec.addParameter(makeParam(XMLConstantNames.XML_A_GLOBALCONSTRAINT_MAXJOBS,
                Integer.class.getName(), model.getStationNumberOfServers(key).toString()));

        // maxRunning (PS only)
        if (isPsStrategy(queueStrategy)) {
            sec.addParameter(makeParam("maxRunning",
                    Integer.class.getName(), model.getStationMaxRunningJobs(key).toString()));
        }

        // numberOfVisits (always 1 per class, matches legacy writer)
        sec.addParameter(makePerClassContainer("numberOfVisits", Integer.class.getName(),
                classes, c -> makeRefParam("numberOfVisits", Integer.class.getName(),
                        model.getClassName(c), VAL_ONE)));

        // ServiceStrategy
        sec.addParameter(buildServiceStrategy(model, key, classes, context));

        // PS-specific params (PSPriorityUsed + PSStrategy + serviceWeights)
        if (CommonConstants.STATION_QUEUE_STRATEGY_PSSERVER.equals(queueStrategy)) {
            addPsParams(sec, model, key, classes, false);
        } else if (CommonConstants.STATION_QUEUE_STRATEGY_PSSERVER_PRIORITY.equals(queueStrategy)) {
            addPsParams(sec, model, key, classes, true);
        }

        // advancedFeatures map parameter
        sec.addParameter(buildAdvancedFeatures(model, key, classes, queueStrategy, context));
    }

    // -----------------------------------------------------------------------
    // Section className resolution
    // -----------------------------------------------------------------------

    private String resolveServerClassName(String queueStrategy, CommonModel model, Object key) {
        switch (queueStrategy) {
            case CommonConstants.STATION_QUEUE_STRATEGY_PSSERVER:
            case CommonConstants.STATION_QUEUE_STRATEGY_PSSERVER_PRIORITY:
                return XMLConstantNames.CLASSNAME_PSSERVER;
            case CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE:
            case CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY:
                return XMLConstantNames.CLASSNAME_PREEMPTIVESERVER;
            case CommonConstants.STATION_QUEUE_STRATEGY_POLLING:
                return Maps.POLLING_TYPE_MAPPING.get(model.getStationPollingServerType(key));
            default:
                return XMLConstantNames.CLASSNAME_SERVER;
        }
    }

    private boolean isPsStrategy(String queueStrategy) {
        return CommonConstants.STATION_QUEUE_STRATEGY_PSSERVER.equals(queueStrategy)
                || CommonConstants.STATION_QUEUE_STRATEGY_PSSERVER_PRIORITY.equals(queueStrategy);
    }

    // -----------------------------------------------------------------------
    // ServiceStrategy
    // -----------------------------------------------------------------------

    private ParameterDescriptor buildServiceStrategy(CommonModel model, Object key, List<Object> classes, WritingContext ctx) {
        ParameterDescriptor container = makeContainer(XMLConstantNames.PARAM_SERVICE_STRATEGY, CP_SERVICE_STRAT);
        container.setArray(true);

        Boolean heterogeneous = model.getHeterogeneousServersEnabled(key);
        if (Boolean.TRUE.equals(heterogeneous)) {
            // Heterogeneous: one entry per (serverType × class), no refClass
            List<ServerType> serverTypes = model.getServerTypes(key);
            for (ServerType st : serverTypes) {
                for (Object classKey : classes) {
                    Object dist = model.getServiceTimeDistribution(key, classKey, st.getServerKey());
                    container.addSubParameter(getDistributionParameter(dist, model, null, PARAM_SERVICE_TIME_STRATEGY,
                            CP_SERVICE + PARAM_SERVICE_TIME_STRATEGY, ctx));
                }
            }
        } else {
            // Homogeneous: one entry per class with refClass
            for (Object classKey : classes) {
                Object dist = model.getServiceTimeDistribution(key, classKey);
                container.addSubParameter(getDistributionParameter(dist, model, classKey, PARAM_SERVICE_TIME_STRATEGY,
                        CP_SERVICE + PARAM_SERVICE_TIME_STRATEGY, ctx));
            }
        }
        return container;
    }

    // -----------------------------------------------------------------------
    // PS parameters
    // -----------------------------------------------------------------------

    private void addPsParams(SectionDescriptor sec, CommonModel model, Object key, List<Object> classes, boolean priority) {
        // PSPriorityUsed (emitted before PSStrategy, matching legacy writer order)
        sec.addParameter(makeParam(XMLConstantNames.PARAM_PS_PRIORITY_USED, Boolean.class.getName(), String.valueOf(priority)));

        // PSStrategy
        Map<String, String> psMap = priority ? Maps.PRIORITY_PS_STRATEGIES_MAPPING : Maps.PS_STRATEGIES_MAPPING;
        ParameterDescriptor psStrategies = makeContainer("PSStrategy", STRATEGIES_BASE + "PSStrategy");
        psStrategies.setArray(true);
        for (Object classKey : classes) {
            String stratName = psMap.get(model.getQueueStrategy(key, classKey));
            ParameterDescriptor ps = makeRefParam(stratName, CP_PS + stratName,
                    model.getClassName(classKey), null);
            psStrategies.addSubParameter(ps);
        }
        sec.addParameter(psStrategies);

        // serviceWeights
        ParameterDescriptor serviceWeights = makeContainer(XMLConstantNames.PARAM_SERVICE_WEIGHTS, Double.class.getName());
        serviceWeights.setArray(true);
        for (Object classKey : classes) {
            serviceWeights.addSubParameter(makeRefParam("serviceWeight", Double.class.getName(),
                    model.getClassName(classKey), model.getServiceWeight(key, classKey).toString()));
        }
        sec.addParameter(serviceWeights);
    }

    // -----------------------------------------------------------------------
    // advancedFeatures map
    // -----------------------------------------------------------------------

    private ParameterDescriptor buildAdvancedFeatures(CommonModel model, Object key,
                                                      List<Object> classes, String queueStrategy, WritingContext ctx) {
        ParameterDescriptor adv = new ParameterDescriptor();
        adv.setName(CommonConstants.SERVER_PARAMETER_ADVANCED_FEATURES);
        adv.setClassPath(HashMap.class.getName());
        adv.setMap(true);

        // Preemptive strategies
        if (CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE.equals(queueStrategy)
                || CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY.equals(queueStrategy)) {
            adv.addSubParameter(buildPreemptiveStrategies(model, key, classes, queueStrategy));
        }

        // Non-preemptive: switchover (setup) times
        if ((CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE.equals(queueStrategy)
                || CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY.equals(queueStrategy))
                && Boolean.TRUE.equals(model.getSwitchoverTimesEnabled(key))) {

            adv.addSubParameter(buildSwitchoverStrategies(model, key, classes, ctx));
        }

        // DelayOff + Setup times
        if (Boolean.TRUE.equals(model.getDelayOffTimesEnabled(key))) {
            adv.addSubParameter(buildDelayOffStrategies(model, key, classes, ctx));
            adv.addSubParameter(buildSetupStrategies(model, key, classes, ctx));
        }

        // Polling switchover
        if (CommonConstants.STATION_QUEUE_STRATEGY_POLLING.equals(queueStrategy)) {
            adv.addSubParameter(buildPollingSwitchoverStrategies(model, key, classes, ctx));
        }

        // classParallelism (always present)
        adv.addSubParameter(buildClassParallelism(model, key, classes));

        // Server types (names, counts, compatibilities)
        buildServerTypes(adv, model, key, classes);

        // Vacation strategy
        adv.addSubParameter(buildVacationStrategy(model, key, ctx));

        return adv;
    }

    // -----------------------------------------------------------------------
    // Advanced features sub-builders
    // -----------------------------------------------------------------------

    private ParameterDescriptor buildPreemptiveStrategies(CommonModel model, Object key,
                                                          List<Object> classes, String queueStrategy) {
        boolean priority = CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY.equals(queueStrategy)
                || CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY.equals(queueStrategy);
        Map<String, String> putMap = priority ? Maps.PRIORITY_QUEUE_PUT_MAPPING : Maps.QUEUE_PUT_MAPPING;

        ParameterDescriptor container = makeContainer(CommonConstants.SERVER_FEATURE_PREEMPTIVE_STRATEGIES, CP_QUEUE_PUT_STRAT);
        container.setArray(true);
        for (Object classKey : classes) {
            String stratName = putMap.get(model.getQueueStrategy(key, classKey));
            container.addSubParameter(makeRefParam(stratName, CP_QUEUE_PUT + stratName,
                    model.getClassName(classKey), null));
        }
        return container;
    }

    /**
     * Switchover (from-class × to-class) for non-preemptive server.
     */
    private ParameterDescriptor buildSwitchoverStrategies(CommonModel model, Object key,
                                                          List<Object> classes, WritingContext ctx) {
        ParameterDescriptor container = makeContainer(CommonConstants.SERVER_FEATURE_SWITCHOVER_STRATEGIES,
                Object.class.getName());
        container.setArray(true);
        for (Object fromClass : classes) {
            // One inner "SwitchoverStrategy" per fromClass, refClass = fromClass name
            ParameterDescriptor inner = makeContainer("SwitchoverStrategy", CP_SERVICE_STRAT);
            inner.setRefClass(model.getClassName(fromClass));
            inner.setArray(true);
            for (Object toClass : classes) {
                Object dist = model.getSwitchoverTimeDistribution(key, fromClass, toClass);
                inner.addSubParameter(getDistributionParameter(dist, model, toClass, PARAM_SERVICE_TIME_STRATEGY,
                        CP_SERVICE + PARAM_SERVICE_TIME_STRATEGY, ctx));
            }
            container.addSubParameter(inner);
        }
        return container;
    }

    /**
     * DelayOff strategies: one entry per class with refClass.
     */
    private ParameterDescriptor buildDelayOffStrategies(CommonModel model, Object key,
                                                        List<Object> classes, WritingContext ctx) {
        ParameterDescriptor container = makeContainer(CommonConstants.SERVER_FEATURE_DELAY_OFF_STRATEGIES,
                Object.class.getName());
        container.setArray(true);
        for (Object classKey : classes) {
            Object dist = model.getDelayOffTimeDistribution(key, classKey);
            ParameterDescriptor entry = getDistributionParameter(dist, model, classKey, "delayOffTime",
                    CP_SERVICE + PARAM_SERVICE_TIME_STRATEGY, ctx);
            container.addSubParameter(entry);
        }
        return container;
    }

    /**
     * Setup strategies: one entry per class with refClass.
     */
    private ParameterDescriptor buildSetupStrategies(CommonModel model, Object key,
                                                     List<Object> classes, WritingContext ctx) {
        ParameterDescriptor container = makeContainer(CommonConstants.SERVER_FEATURE_SETUP_STRATEGIES,
                Object.class.getName());
        container.setArray(true);
        for (Object classKey : classes) {
            Object dist = model.getSetupTimeDistribution(key, classKey);
            ParameterDescriptor entry = getDistributionParameter(dist, model, classKey, "setUpTime",
                    CP_SERVICE + PARAM_SERVICE_TIME_STRATEGY, ctx);
            container.addSubParameter(entry);
        }
        return container;
    }

    /**
     * Polling switchover: one distribution per class.
     */
    private ParameterDescriptor buildPollingSwitchoverStrategies(CommonModel model, Object key,
                                                                 List<Object> classes, WritingContext ctx) {
        ParameterDescriptor container = makeContainer("SwitchoverStrategy", CP_SERVICE_STRAT);
        for (Object classKey : classes) {
            Object dist = model.getPollingSwitchoverDistribution(key, classKey);
            container.addSubParameter(getDistributionParameter(dist, model, classKey, PARAM_SERVICE_TIME_STRATEGY,
                    CP_SERVICE + PARAM_SERVICE_TIME_STRATEGY, ctx));
        }
        return container;
    }

    private ParameterDescriptor buildClassParallelism(CommonModel model, Object key, List<Object> classes) {
        ParameterDescriptor container = makeContainer(CommonConstants.SERVER_FEATURE_CLASS_PARALLELISM,
                Integer.class.getName());
        container.setArray(true);
        for (Object classKey : classes) {
            Integer num = model.getServerNumRequired(key, classKey);
            if (num == null) num = 1;
            container.addSubParameter(makeRefParam("serverParallelism", Integer.class.getName(),
                    model.getClassName(classKey), num.toString()));
        }
        return container;
    }

    /**
     * Server type names, counts, and per-class compatibility arrays.
     */
    private void buildServerTypes(ParameterDescriptor adv, CommonModel model, Object key, List<Object> classes) {
        int numTypes = model.getNumberOfDifferentServerTypes(key);
        int count = Math.max(1, numTypes);
        String stationName = model.getStationName(key);

        ParameterDescriptor serverNames = makeContainer(CommonConstants.SERVER_FEATURE_SERVER_NAMES, String.class.getName());
        ParameterDescriptor serversPerType = makeContainer(CommonConstants.SERVER_FEATURE_SERVERS_PER_SERVER_TYPE, Integer.class.getName());
        ParameterDescriptor serverCompatibilities = makeContainer(CommonConstants.SERVER_FEATURE_SERVER_COMPATIBILITIES, Object.class.getName());
        serverNames.setArray(true);
        serversPerType.setArray(true);
        serverCompatibilities.setArray(true);

        for (int i = 0; i < count; i++) {
            String name;
            int num;
            List<Boolean> compat = new ArrayList<>();

            if (numTypes > 0) {
                ServerType st = model.getInfoForServerType(key, i);
                name = stationName + " - " + st.getName();
                num = st.getNumOfServers();
                for (Object classKey : classes) {
                    compat.add(st.isCompatible(classKey));
                }
            } else {
                name = stationName + " - Server Type 1";
                num = 1;
                for (Object ignored : classes) compat.add(Boolean.TRUE);
            }

            serverNames.addSubParameter(makeParam("serverTypesNames", String.class.getName(), name));
            serversPerType.addSubParameter(makeParam("serverTypesNumOfServers", Integer.class.getName(), String.valueOf(num)));

            ParameterDescriptor compatParam = makeContainer("serverTypesCompatibilities", Boolean.class.getName());
            compatParam.setArray(true);
            for (Boolean b : compat) {
                compatParam.addSubParameter(makeParam("compatibilities", Boolean.class.getName(), String.valueOf(b)));
            }
            serverCompatibilities.addSubParameter(compatParam);
        }

        adv.addSubParameter(serverNames);
        adv.addSubParameter(serversPerType);
        adv.addSubParameter(serverCompatibilities);

        // Scheduling policy
        adv.addSubParameter(makeParam(CommonConstants.SERVER_FEATURE_SCHEDULING_POLICY,
                String.class.getName(), model.getStationSchedulingPolicy(key)));
    }

    /**
     * Vacation strategy parameter, matching VacationStrategyWriter logic.
     */
    private ParameterDescriptor buildVacationStrategy(CommonModel model, Object key, WritingContext ctx) {
        String vacationPolicy = model.getStationVacationPolicy(key);
        VacationParameters vp = model.getStationVacationParameters(key);

        String className;
        ParameterDescriptor container;

        switch (vacationPolicy) {
            case CommonConstants.STATION_VACATION_POLICY_SINGLE:
                className = "VacationWhenIdleStrategy";
                container = makeContainer(CommonConstants.SERVER_FEATURE_VACATION_STRATEGY,
                        CP_VACATION + className);
                container.addSubParameter(makeParam("type", Integer.class.getName(), "0"));
                addVacationDistribution(container, vp, ctx);
                break;
            case CommonConstants.STATION_VACATION_POLICY_MULTIPLE:
                className = "VacationWhenIdleStrategy";
                container = makeContainer(CommonConstants.SERVER_FEATURE_VACATION_STRATEGY,
                        CP_VACATION + className);
                container.addSubParameter(makeParam("type", Integer.class.getName(), "1"));
                addVacationDistribution(container, vp, ctx);
                break;
            case CommonConstants.STATION_VACATION_POLICY_BERNOULLI:
                className = "BernoulliVacationStrategy";
                container = makeContainer(CommonConstants.SERVER_FEATURE_VACATION_STRATEGY,
                        CP_VACATION + className);
                container.addSubParameter(makeParam("p", Double.class.getName(),
                        String.valueOf(vp.getBernoulliP())));
                addVacationDistribution(container, vp, ctx);
                break;
            default: // NONE
                className = "NoVacationStrategy";
                container = makeContainer(CommonConstants.SERVER_FEATURE_VACATION_STRATEGY,
                        CP_VACATION + className);
                break;
        }
        return container;
    }

    /**
     * Appends the distribution and distrPar sub-parameters of a vacation distribution.
     */
    private void addVacationDistribution(ParameterDescriptor container, VacationParameters vp, WritingContext ctx) {
        ParameterDescriptor distParam = getDistributionParameter(
                vp.getVacationTimeDistribution(), null, null,
                "distr", "jmt.engine.random.Distribution", ctx);
        // getDistributionParameter returns a wrapper; its sub-params are [distDesc, distrParDesc]
        for (ParameterDescriptor sub : distParam.getSubParameters()) {
            container.addSubParameter(sub);
        }
    }

    // -----------------------------------------------------------------------
    // Factory helpers (supplement AbstractSectionMapper)
    // -----------------------------------------------------------------------

    /**
     * Creates a container param whose sub-parameters are built per-class.
     */
    @FunctionalInterface
    private interface PerClassParamFactory {
        ParameterDescriptor build(Object classKey);
    }

    private ParameterDescriptor makePerClassContainer(String name, String classPath,
                                                      List<Object> classes,
                                                      PerClassParamFactory factory) {
        ParameterDescriptor container = makeContainer(name, classPath);
        for (Object classKey : classes) {
            container.addSubParameter(factory.build(classKey));
        }
        return container;
    }

    // -----------------------------------------------------------------------
    // mapDtoToCommonModel helpers (unchanged from original)
    // -----------------------------------------------------------------------

    private static class ServerMappingState {
        final List<String> serverNames = new ArrayList<>();
        final List<Integer> serversPerType = new ArrayList<>();
        final List<List<Boolean>> compatibilities = new ArrayList<>();
        final List<Object> heterogeneous = new ArrayList<>();
    }

    private void mapQueueStrategyByClassName(String secName, CommonModel model, Object key, MappingContext ctx) {
        if (XMLConstantNames.CLASSNAME_PSSERVER.equals(secName)) {
            model.setStationQueueStrategy(key, CommonConstants.STATION_QUEUE_STRATEGY_PSSERVER);
            ctx.classes.values().forEach(c -> model.setQueueStrategy(key, c, CommonConstants.QUEUE_STRATEGY_PS));
        } else if (isPollingServer(secName)) {
            model.setStationQueueStrategy(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING);
            if (XMLConstantNames.CLASSNAME_LKPSERVER.equals(secName))
                model.setStationPollingServerType(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING_LIMITED);
            else if (XMLConstantNames.CLASSNAME_GATEDPSERVER.equals(secName))
                model.setStationPollingServerType(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING_GATED);
            else if (XMLConstantNames.CLASSNAME_EXHAUSTIVEPSERVER.equals(secName))
                model.setStationPollingServerType(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING_EXHAUSTIVE);
        }
    }

    private boolean isPollingServer(String secName) {
        return XMLConstantNames.CLASSNAME_LKPSERVER.equals(secName)
                || XMLConstantNames.CLASSNAME_GATEDPSERVER.equals(secName)
                || XMLConstantNames.CLASSNAME_EXHAUSTIVEPSERVER.equals(secName);
    }

    private List<ParameterDescriptor> getFlattenedParameters(SectionDescriptor sec) {
        List<ParameterDescriptor> params = sec.getParameters() != null
                ? new ArrayList<>(sec.getParameters()) : new ArrayList<>();
        ParameterDescriptor adv = getParam(sec, CommonConstants.SERVER_PARAMETER_ADVANCED_FEATURES);
        if (adv != null) params.addAll(getSubParams(adv));
        return params;
    }

    private void mapServerParameter(ParameterDescriptor p, Object key, CommonModel model,
                                    MappingContext ctx, ServerMappingState state) {
        String name = p.getName();
        String cp = p.getClassPath();

        if (XMLConstantNames.XML_A_GLOBALCONSTRAINT_MAXJOBS.equals(name)) {
            mapMaxJobs(p, key, model);
        } else if ("maxRunning".equals(name)) {
            mapMaxRunning(p, key, model);
        } else if (XMLConstantNames.ENGINE_SERVICE_STRATEGY.equals(cp)) {
            mapServiceStrategy(p, key, model, ctx, state.heterogeneous);
        } else if (CommonConstants.SERVER_FEATURE_SERVER_NAMES.equals(name)) {
            mapServerNames(p, state.serverNames);
        } else if (CommonConstants.SERVER_FEATURE_SERVERS_PER_SERVER_TYPE.equals(name)) {
            mapServersPerType(p, state.serversPerType);
        } else if (CommonConstants.SERVER_FEATURE_SERVER_COMPATIBILITIES.equals(name)) {
            mapCompatibilities(p, state.compatibilities);
        } else if (isSwitchoverParam(name)) {
            mapSwitchoverStrategies(p, key, model, ctx);
        } else if (isDelayOffParam(name)) {
            mapDelayOffStrategies(p, key, model, ctx);
        } else if (CommonConstants.SERVER_FEATURE_SETUP_STRATEGIES.equals(name)) {
            mapSetupStrategies(p, key, model, ctx);
        } else if (XMLConstantNames.ENGINE_PS_STRATEGY.equals(cp)) {
            mapPsStrategy(p, key, model, ctx);
        } else if (XMLConstantNames.PARAM_SERVICE_WEIGHTS.equals(name)) {
            mapServiceWeights(p, key, model, ctx);
        } else if (XMLConstantNames.PARAM_PS_PRIORITY_USED.equals(name)) {
            mapPsPriority(p, key, model);
        } else if (isParallelismParam(name)) {
            mapClassParallelism(p, key, model, ctx);
        } else if (CommonConstants.SERVER_FEATURE_VACATION_STRATEGY.equals(name)) {
            mapVacationStrategy(p, cp, key, model, ctx);
        } else if (CommonConstants.SERVER_FEATURE_SCHEDULING_POLICY.equals(name)) {
            String policy = val(p);
            if (policy != null) model.setStationSchedulingPolicy(key, policy);
        }
    }

    private boolean isSwitchoverParam(String name) {
        return CommonConstants.SERVER_FEATURE_SWITCHOVER_STRATEGIES.equals(name)
                || "switchoverTimes".equals(name) || "switchoverStrategy".equals(name);
    }

    private boolean isDelayOffParam(String name) {
        return CommonConstants.SERVER_FEATURE_DELAY_OFF_STRATEGIES.equals(name) || "delayOffTimes".equals(name);
    }

    private boolean isParallelismParam(String name) {
        return CommonConstants.SERVER_FEATURE_CLASS_PARALLELISM.equals(name)
                || "serversNumRequired".equals(name) || "numRequired".equals(name);
    }

    private void mapMaxJobs(ParameterDescriptor p, Object key, CommonModel model) {
        Integer v = valI(p);
        if (v != null) model.setStationNumberOfServers(key, v);
    }

    private void mapMaxRunning(ParameterDescriptor p, Object key, CommonModel model) {
        Integer v = valI(p);
        if (v != null) model.setStationMaxRunningJobs(key, v);
    }

    private void mapServerNames(ParameterDescriptor p, List<String> serverNames) {
        getSubParams(p).forEach(sP -> {
            // Legacy format stores the value directly on each serverTypesNames subParameter.
            String s = val(sP);
            if (s == null) {
                List<ParameterDescriptor> children = getSubParams(sP);
                if (!children.isEmpty()) {
                    s = val(getAt(children, 0));
                }
            }
            if (s != null) {
                serverNames.add(s);
            }
        });
    }

    private void mapServersPerType(ParameterDescriptor p, List<Integer> serversPerType) {
        getSubParams(p).forEach(sP -> {
            // Legacy format stores the value directly on each serverTypesNumOfServers subParameter.
            Integer sv = valI(sP);
            if (sv == null) {
                List<ParameterDescriptor> children = getSubParams(sP);
                if (!children.isEmpty()) {
                    sv = valI(getAt(children, 0));
                }
            }
            if (sv != null) {
                serversPerType.add(sv);
            }
        });
    }

    private void mapServiceWeights(ParameterDescriptor p, Object key, CommonModel model, MappingContext ctx) {
        getRefClassMap(p).forEach((c, sP) -> model.setServiceWeight(key, ctx.classes.get(c), valD(sP)));
    }

    private void mapPsPriority(ParameterDescriptor p, Object key, CommonModel model) {
        if (Boolean.parseBoolean(val(p)))
            model.setStationQueueStrategy(key, CommonConstants.STATION_QUEUE_STRATEGY_PSSERVER_PRIORITY);
    }

    private void mapClassParallelism(ParameterDescriptor p, Object key, CommonModel model, MappingContext ctx) {
        getRefClassMap(p).forEach((c, sP) -> {
            // Legacy format stores the numeric value directly on serverParallelism.
            Integer num = valI(sP);
            if (num == null) {
                List<ParameterDescriptor> children = getSubParams(sP);
                if (!children.isEmpty()) {
                    num = valI(getAt(children, 0));
                }
            }
            if (num != null) {
                model.setServerNumRequired(key, ctx.classes.get(c), num);
            }
        });
    }

    private void mapServiceStrategy(ParameterDescriptor p, Object key, CommonModel model,
                                    MappingContext ctx, List<Object> heterogeneous) {
        Map<String, ParameterDescriptor> dists = getRefClassMap(p);
        if (dists.isEmpty()) {
            getSubParams(p).forEach(sP -> heterogeneous.add(parseServiceStrategy(sP, ctx)));
        } else {
            dists.forEach((c, sP) -> model.setServiceTimeDistribution(key, ctx.classes.get(c), parseServiceStrategy(sP, ctx)));
        }
    }

    private void mapCompatibilities(ParameterDescriptor p, List<List<Boolean>> compatibilities) {
        getSubParams(p).stream()
                .filter(sP -> XMLConstantNames.PARAM_SERVER_TYPES_COMPATIBILITIES.equals(sP.getName()))
                .forEach(sP -> {
                    List<Boolean> comp = new ArrayList<>();
                    getSubParams(sP).forEach(sSP -> {
                        String bVal = val(sSP);
                        comp.add(bVal == null || Boolean.parseBoolean(bVal));
                    });
                    compatibilities.add(comp);
                });
    }

    private void mapSwitchoverStrategies(ParameterDescriptor p, Object key, CommonModel model, MappingContext ctx) {
        if (Objects.equals(model.getStationQueueStrategy(key), CommonConstants.STATION_QUEUE_STRATEGY_POLLING)) {
            getRefClassMap(p).forEach((c, sP) -> model.setPollingSwitchoverDistribution(key, ctx.classes.get(c), parseServiceStrategy(sP, ctx)));
        } else {
            model.setSwitchoverTimesEnabled(key, Boolean.TRUE);
            getRefClassMap(p).forEach((fromC, sP) -> getRefClassMap(sP).forEach((toC, sSP) ->
                    model.setSwitchoverTimeDistribution(key, ctx.classes.get(fromC), ctx.classes.get(toC), parseServiceStrategy(sSP, ctx))));
        }
    }

    private void mapDelayOffStrategies(ParameterDescriptor p, Object key, CommonModel model, MappingContext ctx) {
        model.setDelayOffTimesEnabled(key, Boolean.TRUE);
        getRefClassMap(p).forEach((c, sP) -> getRefClassMap(sP).forEach((innerC, sSP) ->
                model.setDelayOffTimeDistribution(key, ctx.classes.get(innerC), parseServiceStrategy(sSP, ctx))));
    }

    private void mapSetupStrategies(ParameterDescriptor p, Object key, CommonModel model, MappingContext ctx) {
        getRefClassMap(p).forEach((c, sP) -> getRefClassMap(sP).forEach((innerC, sSP) ->
                model.setSetupTimeDistribution(key, ctx.classes.get(innerC), parseServiceStrategy(sSP, ctx))));
    }

    private void mapPsStrategy(ParameterDescriptor p, Object key, CommonModel model, MappingContext ctx) {
        getRefClassMap(p).forEach((c, sP) -> {
            String strat = sP.getClassPath().substring(sP.getClassPath().lastIndexOf(".") + 1);
            if (strat.contains(XMLConstantNames.STRAT_EPS))
                model.setQueueStrategy(key, ctx.classes.get(c), CommonConstants.QUEUE_STRATEGY_PS);
            else if (strat.contains(XMLConstantNames.STRAT_GPS))
                model.setQueueStrategy(key, ctx.classes.get(c), CommonConstants.QUEUE_STRATEGY_GPS);
            else if (strat.contains(XMLConstantNames.STRAT_DPS))
                model.setQueueStrategy(key, ctx.classes.get(c), CommonConstants.QUEUE_STRATEGY_DPS);
            else if (strat.contains(XMLConstantNames.STRAT_QBPS))
                model.setQueueStrategy(key, ctx.classes.get(c), CommonConstants.QUEUE_STRATEGY_QBPS);
        });
    }

    private void mapVacationStrategy(ParameterDescriptor p, String cp, Object key, CommonModel model, MappingContext ctx) {
        model.setStationVacationPolicy(key, CommonConstants.STATION_VACATION_POLICY_NONE);
        VacationParameters vp = model.getStationVacationParameters(key);
        List<ParameterDescriptor> sps = getSubParams(p);

        if (cp != null && cp.endsWith(XMLConstantNames.STRAT_VACATION_IDLE)) {
            String v = val(getAt(sps, 0));
            if (XMLConstantNames.VAL_ZERO.equals(v))
                model.setStationVacationPolicy(key, CommonConstants.STATION_VACATION_POLICY_SINGLE);
            else if (XMLConstantNames.VAL_ONE.equals(v))
                model.setStationVacationPolicy(key, CommonConstants.STATION_VACATION_POLICY_MULTIPLE);
            vp.setVacationTimeDistribution(parseDistribution(getAt(sps, 1), getAt(sps, 2), ctx));
        } else if (cp != null && cp.endsWith(XMLConstantNames.STRAT_VACATION_BERNOULLI)) {
            model.setStationVacationPolicy(key, CommonConstants.STATION_VACATION_POLICY_BERNOULLI);
            Double dVal = valD(getAt(sps, 0));
            if (dVal != null) vp.setBernoulliP(dVal);
            vp.setVacationTimeDistribution(parseDistribution(getAt(sps, 1), getAt(sps, 2), ctx));
        }
    }

    private void createServers(Object key, CommonModel model, List<String> serverNames,
                               List<Integer> serversPerType, List<List<Boolean>> compatibilities) {
        int numClasses = model.getClassKeys().size();
        int numServersToCreate = Math.max(1, serverNames.size());
        for (int i = 0; i < numServersToCreate; i++) {
            Integer sCount = (i < serversPerType.size()) ? serversPerType.get(i) : null;
            int n = (sCount != null) ? sCount : 1;
            List<Boolean> comp = (i < compatibilities.size()) ? new ArrayList<>(compatibilities.get(i)) : new ArrayList<>();
            while (comp.size() < numClasses) comp.add(Boolean.TRUE);
            String sName = (i < serverNames.size() && serverNames.get(i) != null)
                    ? serverNames.get(i) : "Server Type " + (i + 1);
            model.addServerType(key, sName, n, comp, true);
        }
    }

    private void mapHeterogeneousServers(Object key, CommonModel model, List<Object> heterogeneous) {
        if (!heterogeneous.isEmpty()) {
            model.setHeterogeneousServersEnabled(key, true);
            List<Object> cKeys = model.getClassKeys();
            int hIdx = 0;
            for (ServerType st : model.getServerTypes(key)) {
                for (Object ck : cKeys) {
                    if (hIdx < heterogeneous.size()) {
                        model.setServiceTimeDistribution(key, ck, st.getServerKey(), heterogeneous.get(hIdx++));
                    }
                }
            }
        }
    }
}