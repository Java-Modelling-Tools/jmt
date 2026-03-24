package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.definitions.VacationParameters;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;
import jmt.gui.common.xml.mapper.XMLMapperConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class ServerSectionMapper extends AbstractSectionMapper implements XMLMapperConstants {

    @Override
    public void map(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        debug("Parsing ServerSection per la stazione: %s", node.getName());
        String secName = sec.getClassName();

        if (XMLConstantNames.CLASSNAME_PSSERVER.equals(secName)) {
            model.setStationQueueStrategy(key, CommonConstants.STATION_QUEUE_STRATEGY_PSSERVER);
            ctx.classes.values().forEach(c -> model.setQueueStrategy(key, c, CommonConstants.QUEUE_STRATEGY_PS));
        } else if (XMLConstantNames.CLASSNAME_LKPSERVER.equals(secName)) {
            model.setStationQueueStrategy(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING);
            model.setStationPollingServerType(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING_LIMITED);
        } else if (XMLConstantNames.CLASSNAME_GATEDPSERVER.equals(secName)) {
            model.setStationQueueStrategy(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING);
            model.setStationPollingServerType(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING_GATED);
        } else if (XMLConstantNames.CLASSNAME_EXHAUSTIVEPSERVER.equals(secName)) {
            model.setStationQueueStrategy(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING);
            model.setStationPollingServerType(key, CommonConstants.STATION_QUEUE_STRATEGY_POLLING_EXHAUSTIVE);
        }

        List<String> serverNames = new ArrayList<>();
        List<Integer> serversPerType = new ArrayList<>();
        List<List<Boolean>> compatibilities = new ArrayList<>();
        List<Object> heterogeneous = new ArrayList<>();

        // Costruiamo la lista appiattendo advancedFeatures come faceva l'Oracolo
        List<ParameterDescriptor> params = sec.getParameters() != null ? new ArrayList<>(sec.getParameters()) : new ArrayList<>();
        ParameterDescriptor adv = getParam(sec, CommonConstants.SERVER_PARAMETER_ADVANCED_FEATURES);
        if (adv != null) params.addAll(getSubParams(adv));

        for (ParameterDescriptor p : params) {
            String name = p.getName();
            String cp = p.getClassPath();

            if (PARAM_MAX_JOBS.equals(name)) {
                Integer v = valI(p);
                debug("  -> Trovato maxJobs: %d", v);
                if (v != null) model.setStationNumberOfServers(key, v);

            } else if ("maxRunning".equals(name)) {
                // FIX ORACLE: Aggiunto il branch mancante per maxRunning!
                Integer v = valI(p);
                debug("  -> Trovato maxRunning: %d", v);
                if (v != null) model.setStationMaxRunningJobs(key, v);

            } else if (ENGINE_SERVICE_STRATEGY.equals(cp)) {
                Map<String, ParameterDescriptor> dists = getRefClassMap(p);
                if (dists.isEmpty()) {
                    getSubParams(p).forEach(sP -> heterogeneous.add(parseServiceStrategy(sP, ctx)));
                } else {
                    dists.forEach((c, sP) -> model.setServiceTimeDistribution(key, ctx.classes.get(c), parseServiceStrategy(sP, ctx)));
                }
            } else if (CommonConstants.SERVER_FEATURE_SERVER_NAMES.equals(name)) {
                getSubParams(p).forEach(sP -> {
                    String sName = val(sP);
                    if (sName != null) serverNames.add(sName);
                });
            } else if (CommonConstants.SERVER_FEATURE_SERVERS_PER_SERVER_TYPE.equals(name)) {
                getSubParams(p).forEach(sP -> {
                    Integer sv = valI(sP);
                    if (sv != null) serversPerType.add(sv);
                });
            } else if (CommonConstants.SERVER_FEATURE_SERVER_COMPATIBILITIES.equals(name)) {
                getSubParams(p).stream().filter(sP -> PARAM_SERVER_TYPES_COMPATIBILITIES.equals(sP.getName())).forEach(sP -> {
                    List<Boolean> comp = new ArrayList<>();
                    getSubParams(sP).forEach(sSP -> {
                        String bVal = val(sSP);
                        comp.add(bVal != null ? Boolean.parseBoolean(bVal) : true);
                    });
                    compatibilities.add(comp);
                });
            } else if (CommonConstants.SERVER_FEATURE_SWITCHOVER_STRATEGIES.equals(name) || "switchoverTimes".equals(name) || "SwitchoverStrategy".equals(name) || "switchoverStrategy".equals(name)) {
                if (model.getStationQueueStrategy(key) == CommonConstants.STATION_QUEUE_STRATEGY_POLLING) {
                    getRefClassMap(p).forEach((c, sP) -> model.setPollingSwitchoverDistribution(key, ctx.classes.get(c), parseServiceStrategy(sP, ctx)));
                } else {
                    model.setSwitchoverTimesEnabled(key, Boolean.TRUE);
                    getRefClassMap(p).forEach((fromC, sP) -> getRefClassMap(sP).forEach((toC, sSP) ->
                            model.setSwitchoverTimeDistribution(key, ctx.classes.get(fromC), ctx.classes.get(toC), parseServiceStrategy(sSP, ctx))));
                }
            } else if (CommonConstants.SERVER_FEATURE_DELAY_OFF_STRATEGIES.equals(name) || "delayOffTimes".equals(name) || "delayOffTime".equals(name)) {
                model.setDelayOffTimesEnabled(key, Boolean.TRUE);
                getRefClassMap(p).forEach((c, sP) -> getRefClassMap(sP).forEach((innerC, sSP) ->
                        model.setDelayOffTimeDistribution(key, ctx.classes.get(innerC), parseServiceStrategy(sSP, ctx))));
            } else if (CommonConstants.SERVER_FEATURE_SETUP_STRATEGIES.equals(name)) {
                getRefClassMap(p).forEach((c, sP) -> getRefClassMap(sP).forEach((innerC, sSP) ->
                        model.setSetupTimeDistribution(key, ctx.classes.get(innerC), parseServiceStrategy(sSP, ctx))));
            } else if (ENGINE_PS_STRATEGY.equals(cp)) {
                getRefClassMap(p).forEach((c, sP) -> {
                    String strat = sP.getClassPath().substring(sP.getClassPath().lastIndexOf(".") + 1);
                    if (strat.contains(STRAT_EPS)) model.setQueueStrategy(key, ctx.classes.get(c), CommonConstants.QUEUE_STRATEGY_PS);
                    else if (strat.contains(STRAT_GPS)) model.setQueueStrategy(key, ctx.classes.get(c), CommonConstants.QUEUE_STRATEGY_GPS);
                    else if (strat.contains(STRAT_DPS)) model.setQueueStrategy(key, ctx.classes.get(c), CommonConstants.QUEUE_STRATEGY_DPS);
                    else if (strat.contains(STRAT_QBPS)) model.setQueueStrategy(key, ctx.classes.get(c), CommonConstants.QUEUE_STRATEGY_QBPS);
                });
            } else if (PARAM_SERVICE_WEIGHTS.equals(name)) {
                getRefClassMap(p).forEach((c, sP) -> model.setServiceWeight(key, ctx.classes.get(c), valD(sP)));
            } else if (PARAM_PS_PRIORITY_USED.equals(name)) {
                String bVal = val(p);
                if (bVal != null && Boolean.parseBoolean(bVal)) {
                    model.setStationQueueStrategy(key, CommonConstants.STATION_QUEUE_STRATEGY_PSSERVER_PRIORITY);
                }
            } else if (CommonConstants.SERVER_FEATURE_CLASS_PARALLELISM.equals(name) || "serversNumRequired".equals(name) || "numRequired".equals(name)) {
                getRefClassMap(p).forEach((c, sP) -> model.setServerNumRequired(key, ctx.classes.get(c), valI(sP)));
            } else if (CommonConstants.SERVER_FEATURE_VACATION_STRATEGY.equals(name)) {
                model.setStationVacationPolicy(key, CommonConstants.STATION_VACATION_POLICY_NONE);
                VacationParameters vp = model.getStationVacationParameters(key);
                List<ParameterDescriptor> sps = getSubParams(p);

                if (cp != null && cp.endsWith(STRAT_VACATION_IDLE)) {
                    String v = val(getAt(sps, 0));
                    if (VAL_ZERO.equals(v)) model.setStationVacationPolicy(key, CommonConstants.STATION_VACATION_POLICY_SINGLE);
                    else if (VAL_ONE.equals(v)) model.setStationVacationPolicy(key, CommonConstants.STATION_VACATION_POLICY_MULTIPLE);
                    vp.setVacationTimeDistribution(parseDistribution(getAt(sps, 1), getAt(sps, 2), ctx));
                } else if (cp != null && cp.endsWith(STRAT_VACATION_BERNOULLI)) {
                    model.setStationVacationPolicy(key, CommonConstants.STATION_VACATION_POLICY_BERNOULLI);
                    Double dVal = valD(getAt(sps, 0));
                    if (dVal != null) vp.setBernoulliP(dVal);
                    vp.setVacationTimeDistribution(parseDistribution(getAt(sps, 1), getAt(sps, 2), ctx));
                }
            }
        }

        int numClasses = model.getClassKeys().size();
        int numServersToCreate = Math.max(1, serverNames.size());

        for (int i = 0; i < numServersToCreate; i++) {
            Integer sCount = (i < serversPerType.size()) ? serversPerType.get(i) : null;
            int n = (sCount != null) ? sCount : 1;

            List<Boolean> comp = (i < compatibilities.size()) ? new ArrayList<>(compatibilities.get(i)) : new ArrayList<>();

            while (comp.size() < numClasses) {
                comp.add(Boolean.TRUE);
            }

            String sName = (i < serverNames.size() && serverNames.get(i) != null) ? serverNames.get(i) : "Server " + (i + 1);

            model.addServerType(key, sName, n, comp, true);
        }

        if (!heterogeneous.isEmpty()) {
            model.setHeterogeneousServersEnabled(key, true);
            Vector<Object> cKeys = model.getClassKeys();
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