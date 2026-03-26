package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;

import java.util.ArrayList;
import java.util.List;

public class TimingSectionMapper extends AbstractSectionMapper {

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
}