package jmt.gui.common.xml.mapper;

import jmt.framework.data.MacroReplacer;
import jmt.gui.common.Defaults;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.XMLConstantNames;
import jmt.gui.common.xml.dto.SimDescriptor;

import java.io.File;
import java.util.Map;

public class GlobalSettingsMapper implements ModelElementMapper {

    @Override
    public void map(SimDescriptor dto, MappingContext context) {
        CommonModel model = context.getModel();

        // We retrieve the map of attributes extracted from the root <sim> node
        Map<String, String> globals = dto.getGlobalAttributes();

        // --- Simulation and Seed ---
        String seed = globals.get(XMLConstantNames.XML_A_ROOT_SEED);
        if (seed != null && !seed.isEmpty()) {
            model.setUseRandomSeed(Boolean.FALSE);
            model.setSimulationSeed(Long.valueOf(seed));
        } else {
            model.setUseRandomSeed(Boolean.TRUE);
        }

        // --- Time Limits ---
        String maxTime = globals.get(XMLConstantNames.XML_A_ROOT_DURATION);
        if (maxTime != null && !maxTime.isEmpty()) {
            model.setMaximumDuration(Double.valueOf(maxTime));
        } else {
            model.setMaximumDuration(Double.valueOf(-1));
        }

        String maxSimulated = globals.get(XMLConstantNames.XML_A_ROOT_SIMULATED);
        if (maxSimulated != null && !maxSimulated.isEmpty()) {
            model.setMaxSimulatedTime(Double.valueOf(maxSimulated));
        } else {
            model.setMaxSimulatedTime(Double.valueOf(-1));
        }

        // --- Intervals and Samples ---
        String polling = globals.get(XMLConstantNames.XML_A_ROOT_POLLING);
        if (polling != null && !polling.isEmpty()) {
            model.setPollingInterval(Double.valueOf(polling));
        }

        String maxSamples = globals.get(XMLConstantNames.XML_A_ROOT_MAXSAMPLES);
        if (maxSamples != null && !maxSamples.isEmpty()) {
            model.setMaxSimulationSamples(Integer.valueOf(maxSamples));
        }

        // --- Statistics and Events ---
        String disableStatistic = globals.get(XMLConstantNames.XML_A_ROOT_DISABLESTATISTIC);
        if (disableStatistic != null && !disableStatistic.isEmpty()) {
            model.setDisableStatistic(Boolean.valueOf(disableStatistic));
        }

        String maxEvents = globals.get(XMLConstantNames.XML_A_ROOT_MAXEVENTS);
        if (maxEvents != null && !maxEvents.isEmpty()) {
            model.setMaxSimulationEvents(Integer.valueOf(maxEvents));
        } else {
            model.setMaxSimulationEvents(Integer.valueOf(-1));
        }

        // --- Global Logger Parameters ---
        String logPath = globals.get(XMLConstantNames.XML_A_ROOT_LOGPATH);
        if (logPath != null && !logPath.isEmpty()) {
            File dir = new File(logPath);
            if (dir.isDirectory()) {
                model.setLoggingGlbParameter("path", dir.getAbsolutePath());
            } else {
                model.setLoggingGlbParameter("path", MacroReplacer.replace(MacroReplacer.MACRO_WORKDIR));
            }
        } else {
            model.setLoggingGlbParameter("path", MacroReplacer.replace(MacroReplacer.MACRO_WORKDIR));
        }

        String logReplaceMode = globals.get(XMLConstantNames.XML_A_ROOT_LOGREPLACE);
        if (logReplaceMode != null && !logReplaceMode.isEmpty()) {
            model.setLoggingGlbParameter("autoAppend", logReplaceMode);
        } else {
            model.setLoggingGlbParameter("autoAppend", Defaults.get("loggerAutoAppend"));
        }

        String logDelimiter = globals.get(XMLConstantNames.XML_A_ROOT_LOGDELIM);
        if (logDelimiter != null && !logDelimiter.isEmpty()) {
            model.setLoggingGlbParameter("delim", logDelimiter);
        } else {
            model.setLoggingGlbParameter("delim", Defaults.get("loggerDelimiter"));
        }

        String logDecimalSeparator = globals.get(XMLConstantNames.XML_A_ROOT_LOGDECIMALSEPARATOR);
        if (logDecimalSeparator != null && !logDecimalSeparator.isEmpty()) {
            model.setLoggingGlbParameter("decimalSeparator", logDecimalSeparator);
        } else {
            model.setLoggingGlbParameter("decimalSeparator", Defaults.get("loggerDecimalSeparator"));
        }
    }
}