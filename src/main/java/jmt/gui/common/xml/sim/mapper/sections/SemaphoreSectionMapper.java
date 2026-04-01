package jmt.gui.common.xml.sim.mapper.sections;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.semaphoreStrategies.NormalSemaphore;
import jmt.gui.common.semaphoreStrategies.SemaphoreStrategy;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.sim.dto.NodeDescriptor;
import jmt.gui.common.xml.sim.dto.ParameterDescriptor;
import jmt.gui.common.xml.sim.dto.SectionDescriptor;
import jmt.gui.common.xml.sim.mapper.MappingContext;
import jmt.gui.common.xml.sim.serializer.WritingContext;

/**
 * Maps semaphore section descriptors between the DTO and the {@link CommonModel},
 * handling semaphore strategy types and per-class threshold configurations.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
public class SemaphoreSectionMapper extends AbstractSectionMapper {

    private static final String STRATEGIES_BASE = "jmt.engine.NetStrategies.";

    @Override
    public void mapDtoToCommonModel(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        ParameterDescriptor p = getFirst(sec.getParameters());

        if (p != null) {
            getRefClassMap(p).forEach((className, sP) -> {
                Object classKey = ctx.classes.get(className);
                String cp = sP.getClassPath();

                ctx.getEngineToGuiSemaphore().values().stream()
                        .filter(ss -> cp != null && cp.equals(ss.getClassPath()))
                        .findFirst()
                        .ifPresent(ss -> model.setSemaphoreStrategy(key, classKey, ss.clone()));

                SemaphoreStrategy ss = (SemaphoreStrategy) model.getSemaphoreStrategy(key, classKey);
                if (ss instanceof NormalSemaphore) {
                    ParameterDescriptor thresParam = getFirst(getSubParams(sP));
                    if (thresParam != null) {
                        Integer val = valI(thresParam);
                        if (val != null) ss.setThreshold(val);
                    }
                }
            });
        } else {
            ctx.classes.values().forEach(o -> model.setSemaphoreStrategy(key, o, CommonConstants.SEMAPHORE_NORMAL.clone()));
        }
    }

    @Override
    public void mapCommonModelToDto(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, WritingContext context) {
        boolean hasNonDefault = false;
        for (Object classKey : model.getClassKeys()) {
            SemaphoreStrategy ss = (SemaphoreStrategy) model.getSemaphoreStrategy(key, classKey);
            // Verifica se è stato modificato il parametro di default
            if (ss != null && (!(ss instanceof NormalSemaphore) || ss.getThreshold() != 1)) {
                hasNonDefault = true;
                break;
            }
        }

        if (!hasNonDefault) {
            return;
        }

        ParameterDescriptor semaphoreStrategy = new ParameterDescriptor();
        semaphoreStrategy.setName("SemaphoreStrategy");
        semaphoreStrategy.setClassPath(STRATEGIES_BASE + "SemaphoreStrategy");
        semaphoreStrategy.setArray(true); // FIX 2: Forza l'attributo array="true"

        for (Object classKey : model.getClassKeys()) {
            SemaphoreStrategy ss = (SemaphoreStrategy) model.getSemaphoreStrategy(key, classKey);

            ParameterDescriptor threshold = new ParameterDescriptor();
            threshold.setName(XMLConstantNames.PARAM_THRESHOLD);
            threshold.setClassPath(Integer.class.getName());
            threshold.setValue(String.valueOf(ss.getThreshold()));

            ParameterDescriptor classParam = new ParameterDescriptor();
            classParam.setName(ss.getName());
            classParam.setClassPath(ss.getClassPath());
            classParam.setRefClass(model.getClassName(classKey));
            classParam.addSubParameter(threshold);

            semaphoreStrategy.addSubParameter(classParam);
        }

        sec.addParameter(semaphoreStrategy);
    }
}