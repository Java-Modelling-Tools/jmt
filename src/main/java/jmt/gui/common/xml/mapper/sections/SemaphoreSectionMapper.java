/**
 * This class is responsible for mapping the Semaphore section of a node.
 * It configures the semaphore strategies and thresholds for the user classes.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.semaphoreStrategies.NormalSemaphore;
import jmt.gui.common.semaphoreStrategies.SemaphoreStrategy;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;
import jmt.gui.common.xml.serializer.WritingContext;

public class SemaphoreSectionMapper extends AbstractSectionMapper {

    private static final String STRATEGIES_BASE = "jmt.engine.NetStrategies.";

    // --- READING PHASE (XML -> DTO -> Model) ---

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

    // --- WRITING PHASE (Model -> DTO -> XML) ---

    @Override
    public void mapCommonModelToDto(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, WritingContext context) {
        ParameterDescriptor semaphoreStrategy = new ParameterDescriptor();
        semaphoreStrategy.setName("SemaphoreStrategy");
        semaphoreStrategy.setClassPath(STRATEGIES_BASE + "SemaphoreStrategy");

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