/**
 * This class is responsible for mapping the Firing section of a node.
 * It handles the parsing and writing of the Petri Net firing outcomes.
 *
 * @author Borgna Daniele
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml.sim.mapper.sections;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.sim.dto.NodeDescriptor;
import jmt.gui.common.xml.sim.dto.ParameterDescriptor;
import jmt.gui.common.xml.sim.dto.SectionDescriptor;
import jmt.gui.common.xml.sim.mapper.MappingContext;
import jmt.gui.common.xml.sim.serializer.WritingContext;

import java.util.List;

public class FiringSectionMapper extends AbstractSectionMapper {

    // --- READING PHASE (XML -> DTO -> Model) ---

    @Override
    public void mapDtoToCommonModel(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        ParameterDescriptor fo = getParam(sec, XMLConstantNames.PARAM_FIRING_OUTCOMES);
        if (fo != null) {
            List<ParameterDescriptor> outcomes = getSubParams(fo);
            for (int i = 0; i < outcomes.size(); i++) {
                mapOutcome(key, i, outcomes.get(i), ctx);
            }
        }
    }

    private void mapOutcome(Object key, int outcomeIndex, ParameterDescriptor outcomeParam, MappingContext ctx) {
        ParameterDescriptor firstOutcomeSub = getFirst(getSubParams(outcomeParam));
        if (firstOutcomeSub == null) return;

        List<ParameterDescriptor> vectors = getSubParams(firstOutcomeSub);
        for (ParameterDescriptor vector : vectors) {
            mapVector(key, outcomeIndex, vector, ctx);
        }
    }

    private void mapVector(Object key, int outcomeIndex, ParameterDescriptor vector, MappingContext ctx) {
        List<ParameterDescriptor> els = getSubParams(vector);
        if (els.isEmpty()) return;

        String sName = val(getAt(els, 0));
        getRefClassMap(getAt(els, 1)).forEach((className, entry) -> {
            Integer vVal = valI(entry);
            if (sName != null && vVal != null) {
                ctx.firingOutcomeMap.put(new Object[]{key, outcomeIndex, sName, ctx.classes.get(className)}, vVal);
            }
        });
    }

    // --- WRITING PHASE (Model -> DTO -> XML) ---
    @Override
    public void mapCommonModelToDto(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, WritingContext context) {
        ParameterDescriptor firingOutcomes = createConditionMatrix("firing", "Outcome", key, model, (s, m, i, to, cl) -> model.getFiringOutcome(s, i, to, cl), false);
        if (firingOutcomes != null) {
            sec.addParameter(firingOutcomes);
        }
    }
}