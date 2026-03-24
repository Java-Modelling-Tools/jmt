package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.joinStrategies.GuardJoin;
import jmt.gui.common.joinStrategies.JoinStrategy;
import jmt.gui.common.joinStrategies.PartialJoin;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;
import jmt.gui.common.xml.mapper.XMLMapperConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JoinSectionMapper extends AbstractSectionMapper implements XMLMapperConstants {

    @Override
    public void map(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        ParameterDescriptor p = sec.getParameters() != null && !sec.getParameters().isEmpty() ? sec.getParameters().get(0) : null;

        if (p != null) {
            if (ctx.engineToGuiJoin == null) {
                initGuiJoinCache(ctx);
            }

            getRefClassMap(p).forEach((className, jP) -> {
                Object classKey = ctx.classes.get(className);

                ctx.engineToGuiJoin.values().stream()
                        .filter(js -> jP.getClassPath().equals(js.getClassPath()))
                        .findFirst()
                        .ifPresent(js -> model.setJoinStrategy(key, classKey, js.clone()));

                JoinStrategy js = (JoinStrategy) model.getJoinStrategy(key, classKey);

                if (js instanceof PartialJoin) {
                    getSubParams(jP).stream()
                            .filter(s -> PARAM_NUM_REQUIRED.equals(s.getName()))
                            .findFirst()
                            .ifPresent(s -> {
                                Integer val = valI(s);
                                if (val != null) ((PartialJoin) js).setRequiredNum(val);
                            });

                } else if (js instanceof GuardJoin) {
                    Map<Object, Integer> mix = new HashMap<>();
                    List<String> cs = new ArrayList<>();
                    List<Integer> ns = new ArrayList<>();

                    for (ParameterDescriptor par : getSubParams(jP)) {
                        if (PARAM_CLASSES.equals(par.getName())) {
                            for (ParameterDescriptor c : getSubParams(par)) {
                                String val = val(c);
                                if (val != null) cs.add(val);
                            }
                        } else if (PARAM_NUMBERS.equals(par.getName())) {
                            for (ParameterDescriptor n : getSubParams(par)) {
                                Integer val = valI(n);
                                if (val != null) ns.add(val);
                            }
                        }
                    }

                    for (int i = 0; i < cs.size() && i < ns.size(); i++) {
                        mix.put(model.getClassByName(cs.get(i)), ns.get(i));
                    }
                    ((GuardJoin) js).setGuard(mix);
                }
            });
        } else {
            ctx.classes.values().forEach(o -> model.setJoinStrategy(key, o, CommonConstants.JOIN_NORMAL.clone()));
        }
    }
}