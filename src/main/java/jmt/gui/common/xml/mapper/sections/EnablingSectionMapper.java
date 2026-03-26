package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;

public class EnablingSectionMapper extends AbstractSectionMapper {

    @Override
    public void mapDtoToCommonModel(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        extractTransitionConditions(key, getParam(sec, XMLConstantNames.PARAM_ENABLING_CONDITIONS), getParam(sec, XMLConstantNames.PARAM_INHIBITING_CONDITIONS), null, ctx);
    }
}