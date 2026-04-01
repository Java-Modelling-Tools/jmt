/**
 * This class is responsible for mapping the Logger section of a node.
 * It configures the logging parameters, such as the output file and specific logging flags.
 *
 * @author Borgna Daniele
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml.sim.mapper.sections;

import jmt.engine.log.LoggerParameters;
import jmt.framework.data.MacroReplacer;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.sim.dto.NodeDescriptor;
import jmt.gui.common.xml.sim.dto.ParameterDescriptor;
import jmt.gui.common.xml.sim.dto.SectionDescriptor;
import jmt.gui.common.xml.sim.mapper.MappingContext;
import jmt.gui.common.xml.sim.serializer.WritingContext;

public class LoggerSectionMapper extends AbstractSectionMapper {

    // --- READING PHASE (XML -> DTO -> Model) ---

    @Override
    public void mapDtoToCommonModel(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, MappingContext ctx) {
        if (sec.getParameters() == null) return;

        LoggerParameters logParams = new LoggerParameters();
        for (ParameterDescriptor p : sec.getParameters()) {
            mapLoggerParameter(p, logParams, model);
        }
        model.setLoggingParameters(key, logParams);
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

    // --- WRITING PHASE (Model -> DTO -> XML) ---

    @Override
    public void mapCommonModelToDto(SectionDescriptor sec, CommonModel model, Object key, NodeDescriptor node, String stationType, WritingContext context) {
        LoggerParameters logParams = model.getLoggingParameters(key);
        if (logParams == null) return;

        addSimpleParameter(sec, XMLConstantNames.XML_LOG_FILENAME, XML_STRING_CLASS, logParams.name);

        // temporary fix as in XMLWriter
        logParams.path = MacroReplacer.replace(model.getLoggingGlbParameter("path"));
        addSimpleParameter(sec, XMLConstantNames.XML_LOG_FILEPATH, XML_STRING_CLASS, logParams.path);

        addSimpleParameter(sec, XMLConstantNames.XML_LOG_B_EXECTIMESTAMP, XML_BOOLEAN_CLASS, logParams.boolExecTimestamp.toString());
        addSimpleParameter(sec, XMLConstantNames.XML_LOG_B_LOGGERNAME, XML_BOOLEAN_CLASS, logParams.boolLoggername.toString());
        addSimpleParameter(sec, XMLConstantNames.XML_LOG_B_TIMESTAMP, XML_BOOLEAN_CLASS, logParams.boolTimeStamp.toString());
        addSimpleParameter(sec, XMLConstantNames.XML_LOG_B_JOBID, XML_BOOLEAN_CLASS, logParams.boolJobID.toString());
        addSimpleParameter(sec, XMLConstantNames.XML_LOG_B_JOBCLASS, XML_BOOLEAN_CLASS, logParams.boolJobClass.toString());
        addSimpleParameter(sec, XMLConstantNames.XML_LOG_B_TIMESAMECLS, XML_BOOLEAN_CLASS, logParams.boolTimeSameClass.toString());
        addSimpleParameter(sec, XMLConstantNames.XML_LOG_B_TIMEANYCLS, XML_BOOLEAN_CLASS, logParams.boolTimeAnyClass.toString());

        addSimpleParameter(sec, "numClasses", XML_INTEGER_CLASS, String.valueOf(model.getClassKeys().size()));
    }

    private void addSimpleParameter(SectionDescriptor sec, String name, String classPath, String value) {
        ParameterDescriptor p = new ParameterDescriptor();
        p.setName(name);
        p.setClassPath(classPath);
        p.setValue(value);
        sec.addParameter(p);
    }
}