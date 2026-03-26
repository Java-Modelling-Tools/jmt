package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.joinStrategies.GuardJoin;
import jmt.gui.common.joinStrategies.JoinStrategy;
import jmt.gui.common.joinStrategies.PartialJoin;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JoinSectionMapper extends AbstractSectionMapper {

    @Override
    public void mapDtoToCommonModel(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        ParameterDescriptor p = getAt(sec.getParameters(), 0);

        if (p != null) {
            getRefClassMap(p).forEach((className, jP) -> mapJoinStrategy(className, jP, key, model, ctx));
        } else {
            ctx.classes.values().forEach(o -> model.setJoinStrategy(key, o, CommonConstants.JOIN_NORMAL.clone()));
        }
    }

    private void mapJoinStrategy(String className, ParameterDescriptor jP, Object key, CommonModel model, MappingContext ctx) {
        Object classKey = ctx.classes.get(className);

        ctx.getEngineToGuiJoin().values().stream()
                .filter(js -> jP.getClassPath().equals(js.getClassPath()))
                .findFirst()
                .ifPresent(js -> model.setJoinStrategy(key, classKey, js.clone()));

        JoinStrategy js = (JoinStrategy) model.getJoinStrategy(key, classKey);

        if (js instanceof PartialJoin) {
            mapPartialJoin((PartialJoin) js, jP);
        } else if (js instanceof GuardJoin) {
            mapGuardJoin((GuardJoin) js, jP, model);
        }
    }

    private void mapPartialJoin(PartialJoin js, ParameterDescriptor jP) {
        getSubParams(jP).stream()
                .filter(s -> XMLConstantNames.PARAM_NUM_REQUIRED.equals(s.getName()))
                .findFirst()
                .ifPresent(s -> {
                    Integer val = valI(s);
                    if (val != null) js.setRequiredNum(val);
                });
    }

    private void mapGuardJoin(GuardJoin js, ParameterDescriptor jP, CommonModel model) {
        List<String> cs = new ArrayList<>();
        List<Integer> ns = new ArrayList<>();

        for (ParameterDescriptor par : getSubParams(jP)) {
            if (XMLConstantNames.PARAM_CLASSES.equals(par.getName())) {
                getSubParams(par).forEach(c -> {
                    String val = val(c);
                    if (val != null) cs.add(val);
                });
            } else if (XMLConstantNames.PARAM_NUMBERS.equals(par.getName())) {
                getSubParams(par).forEach(n -> {
                    Integer val = valI(n);
                    if (val != null) ns.add(val);
                });
            }
        }

        Map<Object, Integer> mix = new HashMap<>();
        for (int i = 0; i < cs.size() && i < ns.size(); i++) {
            mix.put(model.getClassByName(cs.get(i)), ns.get(i));
        }
        js.setGuard(mix);
    }
}