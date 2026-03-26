package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;

public class ClassSwitchSectionMapper extends AbstractSectionMapper {

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

    @Override
    public void mapCommonModelToDto(SectionDescriptor sec, CommonModel model, Object stationKey, NodeDescriptor node, String stationType, MappingContext ctx) {
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
                cellParam.setValue(Float.toString(model.getClassSwitchMatrix(stationKey, classInKey, classOutKey)));
                cellParam.setArray(true);
                rowParam.addSubParameter(cellParam);
            }
            matrixParam.addSubParameter(rowParam);
        }
        sec.addParameter(matrixParam);
    }
}