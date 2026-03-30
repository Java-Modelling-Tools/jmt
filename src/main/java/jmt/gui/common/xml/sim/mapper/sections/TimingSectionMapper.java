/**
 * This class is responsible for mapping the Timing section of a node.
 * It configures the timing strategies (firing times), priorities, and server counts for Petri Net transitions.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.sim.mapper.sections;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.sim.dto.NodeDescriptor;
import jmt.gui.common.xml.sim.dto.ParameterDescriptor;
import jmt.gui.common.xml.sim.dto.SectionDescriptor;
import jmt.gui.common.xml.sim.mapper.MappingContext;
import jmt.gui.common.xml.sim.serializer.WritingContext;

import java.util.ArrayList;
import java.util.List;

public class TimingSectionMapper extends AbstractSectionMapper {

    // --- READING PHASE (XML -> DTO -> Model) ---

    @Override
    public void mapDtoToCommonModel(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        mapTransitionModes(sec, model, key, ctx);
        mapNumberOfServers(sec, model, key);
        mapTimingStrategies(sec, model, key, ctx);
        mapFiringPriorities(sec, model, key);
        mapFiringWeights(sec, model, key);
    }

    private void mapTransitionModes(SectionDescriptor sec, CommonModel model, Object key, MappingContext ctx) {
        ParameterDescriptor modes = getParam(sec, XMLConstantNames.PARAM_MODE_NAMES);
        if (modes != null) {
            List<ParameterDescriptor> subModes = getSubParams(modes);
            if (!subModes.isEmpty()) {
                model.deleteTransitionMode(key, 0);
                List<String> nl = new ArrayList<>();
                for (ParameterDescriptor m : subModes) {
                    String n = val(m);
                    if (n != null) {
                        model.addTransitionMode(key, n);
                        nl.add(n);
                    }
                }
                ctx.modeNameLists.put(key, nl);
            }
        }
    }

    private void mapNumberOfServers(SectionDescriptor sec, CommonModel model, Object key) {
        ParameterDescriptor nos = getParam(sec, XMLConstantNames.PARAM_NUMBERS_OF_SERVERS);
        if (nos != null) {
            List<ParameterDescriptor> sp = getSubParams(nos);
            for (int i = 0; i < sp.size(); i++) {
                Integer num = valI(getAt(sp, i));
                if (num != null && i < model.getTransitionModeListSize(key)) {
                    model.setNumberOfServers(key, i, num);
                }
            }
        }
    }

    private void mapTimingStrategies(SectionDescriptor sec, CommonModel model, Object key, MappingContext ctx) {
        ParameterDescriptor ts = getParam(sec, XMLConstantNames.PARAM_TIMING_STRATEGIES);
        if (ts != null) {
            List<ParameterDescriptor> sp = getSubParams(ts);
            for (int i = 0; i < sp.size(); i++) {
                Object strategy = parseServiceStrategy(getAt(sp, i), ctx);
                if (strategy != null && i < model.getTransitionModeListSize(key)) {
                    model.setFiringTimeDistribution(key, i, strategy);
                }
            }
        }
    }

    private void mapFiringPriorities(SectionDescriptor sec, CommonModel model, Object key) {
        ParameterDescriptor fp = getParam(sec, XMLConstantNames.PARAM_FIRING_PRIORITIES);
        if (fp != null) {
            List<ParameterDescriptor> sp = getSubParams(fp);
            for (int i = 0; i < sp.size(); i++) {
                Integer prio = valI(getAt(sp, i));
                if (prio != null && i < model.getTransitionModeListSize(key)) {
                    model.setFiringPriority(key, i, prio);
                }
            }
        }
    }

    private void mapFiringWeights(SectionDescriptor sec, CommonModel model, Object key) {
        ParameterDescriptor fw = getParam(sec, XMLConstantNames.PARAM_FIRING_WEIGHTS);
        if (fw != null) {
            List<ParameterDescriptor> sp = getSubParams(fw);
            for (int i = 0; i < sp.size(); i++) {
                Double weight = valD(getAt(sp, i));
                if (weight != null && i < model.getTransitionModeListSize(key)) {
                    model.setFiringWeight(key, i, weight);
                }
            }
        }
    }

    // --- WRITING PHASE (Model -> DTO -> XML) ---

    @Override
    public void mapCommonModelToDto(SectionDescriptor sec, CommonModel model,
                                    Object key, NodeDescriptor node,
                                    String stationType, WritingContext context) {
        // modeNames
        ParameterDescriptor modeNames = new ParameterDescriptor();
        modeNames.setName(XMLConstantNames.PARAM_MODE_NAMES);
        modeNames.setClassPath(String.class.getName());
        for (int i = 0; i < model.getTransitionModeListSize(key); i++) {
            ParameterDescriptor mn = new ParameterDescriptor();
            mn.setName("modeName");
            mn.setClassPath(String.class.getName());
            mn.setValue(model.getTransitionModeName(key, i));
            modeNames.addSubParameter(mn);
        }
        sec.addParameter(modeNames);

        // numbersOfServers
        ParameterDescriptor numbersOfServers = new ParameterDescriptor();
        numbersOfServers.setName(XMLConstantNames.PARAM_NUMBERS_OF_SERVERS);
        numbersOfServers.setClassPath(Integer.class.getName());
        for (int i = 0; i < model.getTransitionModeListSize(key); i++) {
            ParameterDescriptor nos = new ParameterDescriptor();
            nos.setName("numberOfServers");
            nos.setClassPath(Integer.class.getName());
            nos.setValue(model.getNumberOfServers(key, i).toString());
            numbersOfServers.addSubParameter(nos);
        }
        sec.addParameter(numbersOfServers);

        // timingStrategies
        ParameterDescriptor timingStrategies = new ParameterDescriptor();
        timingStrategies.setName(XMLConstantNames.PARAM_TIMING_STRATEGIES);
        timingStrategies.setClassPath("jmt.engine.NetStrategies.ServiceStrategy");
        for (int i = 0; i < model.getTransitionModeListSize(key); i++) {
            Object strategy = model.getFiringTimeDistribution(key, i);
            ParameterDescriptor ts = getDistributionParameter(
                    strategy, model, null, "timingStrategy",
                    "jmt.engine.NetStrategies.ServiceStrategies.ServiceTimeStrategy", context);
            timingStrategies.addSubParameter(ts);
        }
        sec.addParameter(timingStrategies);

        // firingPriorities
        ParameterDescriptor firingPriorities = new ParameterDescriptor();
        firingPriorities.setName(XMLConstantNames.PARAM_FIRING_PRIORITIES);
        firingPriorities.setClassPath(Integer.class.getName());
        for (int i = 0; i < model.getTransitionModeListSize(key); i++) {
            ParameterDescriptor fp = new ParameterDescriptor();
            fp.setName("firingPriority");
            fp.setClassPath(Integer.class.getName());
            fp.setValue(model.getFiringPriority(key, i).toString());
            firingPriorities.addSubParameter(fp);
        }
        sec.addParameter(firingPriorities);

        // firingWeights
        ParameterDescriptor firingWeights = new ParameterDescriptor();
        firingWeights.setName(XMLConstantNames.PARAM_FIRING_WEIGHTS);
        firingWeights.setClassPath(Double.class.getName());
        for (int i = 0; i < model.getTransitionModeListSize(key); i++) {
            ParameterDescriptor fw = new ParameterDescriptor();
            fw.setName("firingWeight");
            fw.setClassPath(Double.class.getName());
            fw.setValue(model.getFiringWeight(key, i).toString());
            firingWeights.addSubParameter(fw);
        }
        sec.addParameter(firingWeights);
    }
}