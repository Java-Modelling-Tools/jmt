/**
 * This class is responsible for mapping the Join section of a node.
 * It handles the parsing and writing of the join strategies (Normal, Partial, Guard).
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.sim.mapper.sections;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.joinStrategies.GuardJoin;
import jmt.gui.common.joinStrategies.JoinStrategy;
import jmt.gui.common.joinStrategies.NormalJoin;
import jmt.gui.common.joinStrategies.PartialJoin;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.sim.dto.NodeDescriptor;
import jmt.gui.common.xml.sim.dto.ParameterDescriptor;
import jmt.gui.common.xml.sim.dto.SectionDescriptor;
import jmt.gui.common.xml.sim.mapper.MappingContext;
import jmt.gui.common.xml.sim.serializer.WritingContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JoinSectionMapper extends AbstractSectionMapper {

    // --- READING PHASE (XML -> DTO -> Model) ---

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

    // --- WRITING PHASE (Model -> DTO -> XML) ---

    @Override
    public void mapCommonModelToDto(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, WritingContext context) {
        List<ParameterDescriptor> joinStrats = new ArrayList<>();

        for (Object classKey : model.getClassKeys()) {
            JoinStrategy js = (JoinStrategy) model.getJoinStrategy(key, classKey);
            if (js != null) {
                joinStrats.add(createJoinStrategyParameter(js, model, classKey));
            }
        }

        ParameterDescriptor globalJoin = new ParameterDescriptor();
        globalJoin.setName("JoinStrategy");
        globalJoin.setClassPath("jmt.engine.NetStrategies.JoinStrategy");
        globalJoin.setSubParameters(joinStrats);
        globalJoin.setArray(false);

        sec.getParameters().add(globalJoin);
    }

    private ParameterDescriptor createJoinStrategyParameter(JoinStrategy js, CommonModel model, Object classKey) {
        ParameterDescriptor p = new ParameterDescriptor();
        p.setName(js.getName());
        p.setClassPath(js.getClassPath());
        p.setRefClass(model.getClassName(classKey));
        p.setArray(false);

        List<ParameterDescriptor> subParams = new ArrayList<>();
        if (js instanceof PartialJoin || js instanceof NormalJoin) {
            ParameterDescriptor numRequired = new ParameterDescriptor();
            numRequired.setName(XMLConstantNames.PARAM_NUM_REQUIRED);
            numRequired.setClassPath(Integer.class.getName());
            numRequired.setValue(String.valueOf(js.getRequiredNum()));
            numRequired.setArray(true);
            subParams.add(numRequired);
        } else if (js instanceof GuardJoin) {
            GuardJoin gj = (GuardJoin) js;
            Map<Object, Integer> guard = gj.getGuard();

            ParameterDescriptor classesPar = new ParameterDescriptor();
            classesPar.setName(XMLConstantNames.PARAM_CLASSES);
            classesPar.setClassPath(String.class.getName());
            classesPar.setArray(true);
            List<ParameterDescriptor> classList = new ArrayList<>();

            ParameterDescriptor numbersPar = new ParameterDescriptor();
            numbersPar.setName(XMLConstantNames.PARAM_NUMBERS);
            numbersPar.setClassPath(String.class.getName());
            numbersPar.setArray(true);
            List<ParameterDescriptor> numberList = new ArrayList<>();

            for (Object c : model.getClassKeys()) {
                Integer required = guard.get(c);
                if (required != null) {
                    ParameterDescriptor classSub = new ParameterDescriptor();
                    classSub.setName("class");
                    classSub.setClassPath(String.class.getName());
                    classSub.setValue(model.getClassName(c));
                    classSub.setArray(true);
                    classList.add(classSub);

                    ParameterDescriptor numSub = new ParameterDescriptor();
                    numSub.setName("required");
                    numSub.setClassPath(String.class.getName());
                    numSub.setValue(required.toString());
                    numSub.setArray(true);
                    numberList.add(numSub);
                }
            }
            classesPar.setSubParameters(classList);
            numbersPar.setSubParameters(numberList);

            subParams.add(classesPar);
            subParams.add(numbersPar);
        }

        p.setSubParameters(subParams);
        return p;
    }
}