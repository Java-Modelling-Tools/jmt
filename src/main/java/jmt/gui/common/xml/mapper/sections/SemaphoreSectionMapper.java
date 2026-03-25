package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.semaphoreStrategies.NormalSemaphore;
import jmt.gui.common.semaphoreStrategies.SemaphoreStrategy;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;

public class SemaphoreSectionMapper extends AbstractSectionMapper {

    @Override
    public void map(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
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
}