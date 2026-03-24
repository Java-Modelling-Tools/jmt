package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;
import jmt.gui.common.xml.mapper.XMLMapperConstants;

import java.util.List;

public class FiringSectionMapper extends AbstractSectionMapper implements XMLMapperConstants {

    @Override
    public void map(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        ParameterDescriptor fo = getParam(sec, PARAM_FIRING_OUTCOMES);
        if (fo != null) {
            List<ParameterDescriptor> outcomes = getSubParams(fo);
            for (int i = 0; i < outcomes.size(); i++) {
                ParameterDescriptor firstOutcomeSub = getFirst(getSubParams(getAt(outcomes, i)));
                if (firstOutcomeSub != null) {
                    List<ParameterDescriptor> vectors = getSubParams(firstOutcomeSub);
                    for (ParameterDescriptor vector : vectors) {
                        List<ParameterDescriptor> els = getSubParams(vector);
                        if (!els.isEmpty()) {
                            String sName = val(getAt(els, 0));
                            final int index = i;
                            getRefClassMap(getAt(els, 1)).forEach((className, entry) -> {
                                Integer vVal = valI(entry);
                                if (sName != null && vVal != null) {
                                    ctx.firingOutcomeMap.put(new Object[]{key, index, sName, ctx.classes.get(className)}, vVal);
                                }
                            });
                        }
                    }
                }
            }
        }
    }
}