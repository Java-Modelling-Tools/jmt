package jmt.gui.common.xml.mapper.sections;

import jmt.engine.log.LoggerParameters;
import jmt.framework.data.MacroReplacer;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.mapper.MappingContext;

public class LoggerSectionMapper extends AbstractSectionMapper {

    @Override
    public void mapDtoToCommonModel(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        if (sec.getParameters() == null) return;

        LoggerParameters logParams = new LoggerParameters();
        for (ParameterDescriptor p : sec.getParameters()) {
            mapLoggerParameter(p, logParams, model);
        }
        model.setLoggingParameters(key, logParams);
    }

    @Override
    public void mapCommonModelToDto(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        // TODO: implement logic to map common model to dto
    }

    private void mapLoggerParameter(ParameterDescriptor p, LoggerParameters logParams, CommonModel model) {
        String name = p.getName();
        String v = val(p);

        if (XMLConstantNames.XML_LOG_FILENAME.equals(name)) {
            if (v != null) logParams.name = v;
        } else if (XMLConstantNames.XML_LOG_FILEPATH.equals(name)) {
            logParams.path = MacroReplacer.replace(model.getLoggingGlbParameter("path"));
        } else {
            mapBooleanLoggerParameter(name, v, logParams);
        }
    }

    private void mapBooleanLoggerParameter(String name, String v, LoggerParameters logParams) {
        if (v == null) return;
        boolean bVal = Boolean.parseBoolean(v);

        if (XMLConstantNames.XML_LOG_B_EXECTIMESTAMP.equals(name)) logParams.boolExecTimestamp = bVal;
        else if (XMLConstantNames.XML_LOG_B_LOGGERNAME.equals(name)) logParams.boolLoggername = bVal;
        else if (XMLConstantNames.XML_LOG_B_TIMESTAMP.equals(name)) logParams.boolTimeStamp = bVal;
        else if (XMLConstantNames.XML_LOG_B_JOBID.equals(name)) logParams.boolJobID = bVal;
        else if (XMLConstantNames.XML_LOG_B_JOBCLASS.equals(name)) logParams.boolJobClass = bVal;
        else if (XMLConstantNames.XML_LOG_B_TIMESAMECLS.equals(name)) logParams.boolTimeSameClass = bVal;
        else if (XMLConstantNames.XML_LOG_B_TIMEANYCLS.equals(name)) logParams.boolTimeAnyClass = bVal;
    }
}