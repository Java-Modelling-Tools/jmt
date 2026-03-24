package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;
import jmt.gui.common.xml.mapper.XMLMapperConstants;

public class EnablingSectionMapper extends AbstractSectionMapper implements XMLMapperConstants {

    @Override
    public void map(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        debug("Parsing EnablingSection per la stazione: %s", node.getName());
        extractTransitionConditions(key, getParam(sec, PARAM_ENABLING_CONDITIONS), getParam(sec, PARAM_INHIBITING_CONDITIONS), null, ctx);
    }
}