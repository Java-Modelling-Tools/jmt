package jmt.gui.common.xml.mapper.sections;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;
import jmt.gui.common.xml.mapper.XMLMapperConstants;

public class ClassSwitchSectionMapper extends AbstractSectionMapper implements XMLMapperConstants {

    @Override
    public void map(SectionDescriptor sec, CommonModel model, Object stationKey, NodeDescriptor node, String stationType, MappingContext ctx) {
        debug("Parsing ClassSwitchSection per la stazione: %s", node.getName());

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
}