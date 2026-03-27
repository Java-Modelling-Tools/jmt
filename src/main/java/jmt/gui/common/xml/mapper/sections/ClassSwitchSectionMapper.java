/**
 * This class is responsible for mapping the ClassSwitch section of a node.
 * It handles the parsing and writing of the class switch matrix, which defines
 * the probabilities of a job changing its class upon passing through the station.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;
import jmt.gui.common.xml.serializer.WritingContext;

public class ClassSwitchSectionMapper extends AbstractSectionMapper {

    // --- READING PHASE (XML -> DTO -> Model) ---

    @Override
    public void mapDtoToCommonModel(SectionDescriptor sec, CommonModel model, Object stationKey, NodeDescriptor node, String stationType, MappingContext ctx) {
        ParameterDescriptor matrixParam = getFirst(sec.getParameters());
        if (matrixParam == null) return;

        getRefClassMap(matrixParam).forEach((classIn, row) -> {
            Object classInKey = model.getClassByName(classIn);

            getRefClassMap(row).forEach((classOut, subP) -> {
                Object classOutKey = model.getClassByName(classOut);
                String valStr = val(subP);
                if (valStr != null) {
                    model.setClassSwitchMatrix(stationKey, classInKey, classOutKey, Float.parseFloat(valStr));
                }
            });
        });
    }

    // --- WRITING PHASE (Model -> DTO -> XML) ---

    @Override
    public void mapCommonModelToDto(SectionDescriptor sec, CommonModel model, Object stationKey, NodeDescriptor node, String stationType, WritingContext context) {
        ParameterDescriptor matrixParam = new ParameterDescriptor();
        matrixParam.setName("matrix");
        matrixParam.setClassPath("java.lang.Object");

        for (Object classInKey : model.getClassKeys()) {
            ParameterDescriptor rowParam = new ParameterDescriptor();
            rowParam.setName("row");
            rowParam.setClassPath("java.lang.Float");
            rowParam.setRefClass(model.getClassName(classInKey));
            rowParam.setArray(true);

            for (Object classOutKey : model.getClassKeys()) {
                ParameterDescriptor cellParam = new ParameterDescriptor();
                cellParam.setName("cell");
                cellParam.setClassPath("java.lang.Float");
                cellParam.setRefClass(model.getClassName(classOutKey));

                // Retrieve the probability of switching from classIn to classOut
                float switchProbability = model.getClassSwitchMatrix(stationKey, classInKey, classOutKey);
                cellParam.setValue(Float.toString(switchProbability));
                cellParam.setArray(true);

                rowParam.addSubParameter(cellParam);
            }
            matrixParam.addSubParameter(rowParam);
        }
        sec.addParameter(matrixParam);
    }
}