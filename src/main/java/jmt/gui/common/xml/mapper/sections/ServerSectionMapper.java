package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.definitions.VacationParameters;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;

import java.util.*;

public class ServerSectionMapper extends AbstractSectionMapper {

    @Override
    public void map(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        mapQueueStrategyByClassName(sec.getClassName(), model, key, ctx);

        ServerMappingState state = new ServerMappingState();
        List<ParameterDescriptor> params = getFlattenedParameters(sec);

        for (ParameterDescriptor p : params) {
            mapServerParameter(p, key, model, ctx, state);
        }

        createServers(key, model, state.serverNames, state.serversPerType, state.compatibilities);
        mapHeterogeneousServers(key, model, state.heterogeneous);
    }

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
            if (XMLConstantNames.CLASSNAME_LKPSERVER.equals(secName)) model.setStationPollingServerType(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING_LIMITED);
            else if (XMLConstantNames.CLASSNAME_GATEDPSERVER.equals(secName)) model.setStationPollingServerType(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING_GATED);
            else if (XMLConstantNames.CLASSNAME_EXHAUSTIVEPSERVER.equals(secName)) model.setStationPollingServerType(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING_EXHAUSTIVE);
        }
    }

    private boolean isPollingServer(String secName) {
        return XMLConstantNames.CLASSNAME_LKPSERVER.equals(secName) || XMLConstantNames.CLASSNAME_GATEDPSERVER.equals(secName) || XMLConstantNames.CLASSNAME_EXHAUSTIVEPSERVER.equals(secName);
    }

    private List<ParameterDescriptor> getFlattenedParameters(SectionDescriptor sec) {
        List<ParameterDescriptor> params = sec.getParameters() != null ? new ArrayList<>(sec.getParameters()) : new ArrayList<>();
        ParameterDescriptor adv = getParam(sec, CommonConstants.SERVER_PARAMETER_ADVANCED_FEATURES);
        if (adv != null) params.addAll(getSubParams(adv));
        return params;
    }

    private void mapServerParameter(ParameterDescriptor p, Object key, CommonModel model, MappingContext ctx, ServerMappingState state) {
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
        }
    }

    private boolean isSwitchoverParam(String name) {
        return CommonConstants.SERVER_FEATURE_SWITCHOVER_STRATEGIES.equals(name) || "switchoverTimes".equals(name) || "switchoverStrategy".equals(name);
    }

    private boolean isDelayOffParam(String name) {
        return CommonConstants.SERVER_FEATURE_DELAY_OFF_STRATEGIES.equals(name) || "delayOffTimes".equals(name);
    }

    private boolean isParallelismParam(String name) {
        return CommonConstants.SERVER_FEATURE_CLASS_PARALLELISM.equals(name) || "serversNumRequired".equals(name) || "numRequired".equals(name);
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
            String sName = val(sP);
            if (sName != null) serverNames.add(sName);
        });
    }

    private void mapServersPerType(ParameterDescriptor p, List<Integer> serversPerType) {
        getSubParams(p).forEach(sP -> {
            Integer sv = valI(sP);
            if (sv != null) serversPerType.add(sv);
        });
    }

    private void mapServiceWeights(ParameterDescriptor p, Object key, CommonModel model, MappingContext ctx) {
        getRefClassMap(p).forEach((c, sP) -> model.setServiceWeight(key, ctx.classes.get(c), valD(sP)));
    }

    private void mapPsPriority(ParameterDescriptor p, Object key, CommonModel model) {
        if (Boolean.parseBoolean(val(p))) model.setStationQueueStrategy(key, CommonConstants.STATION_QUEUE_STRATEGY_PSSERVER_PRIORITY);
    }

    private void mapClassParallelism(ParameterDescriptor p, Object key, CommonModel model, MappingContext ctx) {
        getRefClassMap(p).forEach((c, sP) -> model.setServerNumRequired(key, ctx.classes.get(c), valI(sP)));
    }

    private void mapServiceStrategy(ParameterDescriptor p, Object key, CommonModel model, MappingContext ctx, List<Object> heterogeneous) {
        Map<String, ParameterDescriptor> dists = getRefClassMap(p);
        if (dists.isEmpty()) {
            getSubParams(p).forEach(sP -> heterogeneous.add(parseServiceStrategy(sP, ctx)));
        } else {
            dists.forEach((c, sP) -> model.setServiceTimeDistribution(key, ctx.classes.get(c), parseServiceStrategy(sP, ctx)));
        }
    }

    private void mapCompatibilities(ParameterDescriptor p, List<List<Boolean>> compatibilities) {
        getSubParams(p).stream().filter(sP -> XMLConstantNames.PARAM_SERVER_TYPES_COMPATIBILITIES.equals(sP.getName())).forEach(sP -> {
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
            if (strat.contains(XMLConstantNames.STRAT_EPS)) model.setQueueStrategy(key, ctx.classes.get(c), CommonConstants.QUEUE_STRATEGY_PS);
            else if (strat.contains(XMLConstantNames.STRAT_GPS)) model.setQueueStrategy(key, ctx.classes.get(c), CommonConstants.QUEUE_STRATEGY_GPS);
            else if (strat.contains(XMLConstantNames.STRAT_DPS)) model.setQueueStrategy(key, ctx.classes.get(c), CommonConstants.QUEUE_STRATEGY_DPS);
            else if (strat.contains(XMLConstantNames.STRAT_QBPS)) model.setQueueStrategy(key, ctx.classes.get(c), CommonConstants.QUEUE_STRATEGY_QBPS);
        });
    }

    private void mapVacationStrategy(ParameterDescriptor p, String cp, Object key, CommonModel model, MappingContext ctx) {
        model.setStationVacationPolicy(key, CommonConstants.STATION_VACATION_POLICY_NONE);
        VacationParameters vp = model.getStationVacationParameters(key);
        List<ParameterDescriptor> sps = getSubParams(p);

        if (cp != null && cp.endsWith(XMLConstantNames.STRAT_VACATION_IDLE)) {
            String v = val(getAt(sps, 0));
            if (XMLConstantNames.VAL_ZERO.equals(v)) model.setStationVacationPolicy(key, CommonConstants.STATION_VACATION_POLICY_SINGLE);
            else if (XMLConstantNames.VAL_ONE.equals(v)) model.setStationVacationPolicy(key, CommonConstants.STATION_VACATION_POLICY_MULTIPLE);
            vp.setVacationTimeDistribution(parseDistribution(getAt(sps, 1), getAt(sps, 2), ctx));
        } else if (cp != null && cp.endsWith(XMLConstantNames.STRAT_VACATION_BERNOULLI)) {
            model.setStationVacationPolicy(key, CommonConstants.STATION_VACATION_POLICY_BERNOULLI);
            Double dVal = valD(getAt(sps, 0));
            if (dVal != null) vp.setBernoulliP(dVal);
            vp.setVacationTimeDistribution(parseDistribution(getAt(sps, 1), getAt(sps, 2), ctx));
        }
    }

    private void createServers(Object key, CommonModel model, List<String> serverNames, List<Integer> serversPerType, List<List<Boolean>> compatibilities) {
        int numClasses = model.getClassKeys().size();
        int numServersToCreate = Math.max(1, serverNames.size());

        for (int i = 0; i < numServersToCreate; i++) {
            Integer sCount = (i < serversPerType.size()) ? serversPerType.get(i) : null;
            int n = (sCount != null) ? sCount : 1;
            List<Boolean> comp = (i < compatibilities.size()) ? new ArrayList<>(compatibilities.get(i)) : new ArrayList<>();
            while (comp.size() < numClasses) comp.add(Boolean.TRUE);
            String sName = (i < serverNames.size() && serverNames.get(i) != null) ? serverNames.get(i) : "Server " + (i + 1);
            model.addServerType(key, sName, n, comp, true);
        }
    }

    private void mapHeterogeneousServers(Object key, CommonModel model, List<Object> heterogeneous) {
        if (!heterogeneous.isEmpty()) {
            model.setHeterogeneousServersEnabled(key, true);
            List<Object> cKeys = model.getClassKeys();
            int hIdx = 0;
            List<jmt.gui.common.definitions.ServerType> localServers = model.getServerTypes(key);
            for (jmt.gui.common.definitions.ServerType st : localServers) {
                for (Object ck : cKeys) {
                    if (hIdx < heterogeneous.size()) {
                        model.setServiceTimeDistribution(key, ck, st.getServerKey(), heterogeneous.get(hIdx++));
                    }
                }
            }
        }
    }
}