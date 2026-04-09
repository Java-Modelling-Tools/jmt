/**
 * This class maps the Delay section of a node.
 * It reads service strategies (and legacy transition conditions if present) and writes only service strategies.
 *
 * @author Borgna Daniele
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml.sim.mapper.sections;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.sim.dto.NodeDescriptor;
import jmt.gui.common.xml.sim.dto.ParameterDescriptor;
import jmt.gui.common.xml.sim.dto.SectionDescriptor;
import jmt.gui.common.xml.sim.mapper.MappingContext;
import jmt.gui.common.xml.sim.serializer.WritingContext;

public class DelaySectionMapper extends AbstractSectionMapper {

    // --- READING PHASE (XML -> DTO -> Model) ---

    @Override
    public void mapDtoToCommonModel(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        ParameterDescriptor serviceStrategyParam = getParam(sec, "ServiceStrategy");
        if (serviceStrategyParam != null) {
            getRefClassMap(serviceStrategyParam).forEach((className, subParam) -> {
                Object classKey = ctx.classes.get(className);
                if (classKey != null) {
                    Object parsedDistribution = parseServiceStrategy(subParam, ctx);
                    model.setServiceTimeDistribution(key, classKey, parsedDistribution);
                }
            });
        }

        // Legacy XMLReader: transition conditions for Delay/Server are parsed from Queue section, not Delay section.
        // See XMLReader.parseQueueSectionDelay() vs XMLReader.parseDelaySection()
    }

    // --- WRITING PHASE (Model -> DTO -> XML) ---

    @Override
    public void mapCommonModelToDto(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, WritingContext context) {
        addServiceStrategyParameter(sec, model, key, context);
        // Transition conditions for Server/Delay are serialized in Queue section.
        // Keeping Delay section limited to ServiceStrategy preserves engine constructor compatibility.
    }
}

