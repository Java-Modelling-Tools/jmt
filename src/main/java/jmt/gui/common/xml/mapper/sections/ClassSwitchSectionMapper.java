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
    public void mapCommonModelToDto(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        // TODO: implement logic to map common model to dto
    }
}