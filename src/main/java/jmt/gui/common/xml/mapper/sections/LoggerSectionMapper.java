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
    public void map(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        if (sec.getParameters() == null) return;

        LoggerParameters logParams = new LoggerParameters();

        for (ParameterDescriptor p : sec.getParameters()) {
            String name = p.getName();
            String v = val(p);

            if (XMLConstantNames.XML_LOG_FILENAME.equals(name) && v != null) logParams.name = v;
            else if (XMLConstantNames.XML_LOG_FILEPATH.equals(name)) logParams.path = MacroReplacer.replace(model.getLoggingGlbParameter("path"));
            else if (XMLConstantNames.XML_LOG_B_EXECTIMESTAMP.equals(name) && v != null) logParams.boolExecTimestamp = Boolean.valueOf(v);
            else if (XMLConstantNames.XML_LOG_B_LOGGERNAME.equals(name) && v != null) logParams.boolLoggername = Boolean.valueOf(v);
            else if (XMLConstantNames.XML_LOG_B_TIMESTAMP.equals(name) && v != null) logParams.boolTimeStamp = Boolean.valueOf(v);
            else if (XMLConstantNames.XML_LOG_B_JOBID.equals(name) && v != null) logParams.boolJobID = Boolean.valueOf(v);
            else if (XMLConstantNames.XML_LOG_B_JOBCLASS.equals(name) && v != null) logParams.boolJobClass = Boolean.valueOf(v);
            else if (XMLConstantNames.XML_LOG_B_TIMESAMECLS.equals(name) && v != null) logParams.boolTimeSameClass = Boolean.valueOf(v);
            else if (XMLConstantNames.XML_LOG_B_TIMEANYCLS.equals(name) && v != null) logParams.boolTimeAnyClass = Boolean.valueOf(v);
        }
        model.setLoggingParameters(key, logParams);
    }
}