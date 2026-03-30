/**
 * This class is responsible for mapping the Enabling section of a node.
 * It handles the parsing and writing of the Petri Net transition conditions (enabling/inhibiting).
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.sim.mapper.sections;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.sim.dto.NodeDescriptor;
import jmt.gui.common.xml.sim.dto.SectionDescriptor;
import jmt.gui.common.xml.sim.mapper.MappingContext;
import jmt.gui.common.xml.sim.serializer.WritingContext;

public class EnablingSectionMapper extends AbstractSectionMapper {

    // --- READING PHASE (XML -> DTO -> Model) ---

    @Override
    public void mapDtoToCommonModel(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        extractTransitionConditions(key, getParam(sec, XMLConstantNames.PARAM_ENABLING_CONDITIONS), getParam(sec, XMLConstantNames.PARAM_INHIBITING_CONDITIONS), null, ctx);
    }

    // --- WRITING PHASE (Model -> DTO -> XML) ---

    @Override
    public void mapCommonModelToDto(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, WritingContext context) {
        mapTransitionConditions(key, sec, model,  false);
    }
}