/**
 * This class is responsible for mapping the global settings from the {@link SimDescriptor} to the {@link CommonModel}.
 * This includes simulation parameters like seed, duration, and logging configurations.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
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
        Map<String, String> globals = dto.getGlobalAttributes();

        mapSimulationAndSeed(model, globals);
        mapTimeLimits(model, globals);
        mapIntervalsAndSamples(model, globals);
        mapStatisticsAndEvents(model, globals);
        mapLoggerParameters(model, globals);
    }

    private void mapSimulationAndSeed(CommonModel model, Map<String, String> globals) {
        String seed = globals.get(XMLConstantNames.XML_A_ROOT_SEED);
        if (isPresent(seed)) {
            model.setUseRandomSeed(Boolean.FALSE);
            model.setSimulationSeed(Long.valueOf(seed));
        } else {
            model.setUseRandomSeed(Boolean.TRUE);
        }
    }

    private void mapTimeLimits(CommonModel model, Map<String, String> globals) {
        model.setMaximumDuration(getDouble(globals.get(XMLConstantNames.XML_A_ROOT_DURATION), -1.0));
        model.setMaxSimulatedTime(getDouble(globals.get(XMLConstantNames.XML_A_ROOT_SIMULATED), -1.0));
    }

    private void mapIntervalsAndSamples(CommonModel model, Map<String, String> globals) {
        String polling = globals.get(XMLConstantNames.XML_A_ROOT_POLLING);
        if (isPresent(polling)) {
            model.setPollingInterval(Double.valueOf(polling));
        }

        String maxSamples = globals.get(XMLConstantNames.XML_A_ROOT_MAXSAMPLES);
        if (isPresent(maxSamples)) {
            model.setMaxSimulationSamples(Integer.valueOf(maxSamples));
        }
    }

    private void mapStatisticsAndEvents(CommonModel model, Map<String, String> globals) {
        String disableStatistic = globals.get(XMLConstantNames.XML_A_ROOT_DISABLESTATISTIC);
        if (isPresent(disableStatistic)) {
            model.setDisableStatistic(Boolean.valueOf(disableStatistic));
        }

        model.setMaxSimulationEvents(getInt(globals.get(XMLConstantNames.XML_A_ROOT_MAXEVENTS), -1));
    }

    private void mapLoggerParameters(CommonModel model, Map<String, String> globals) {
        String logPath = globals.get(XMLConstantNames.XML_A_ROOT_LOGPATH);
        if (isPresent(logPath) && new File(logPath).isDirectory()) {
            model.setLoggingGlbParameter("path", new File(logPath).getAbsolutePath());
        } else {
            model.setLoggingGlbParameter("path", MacroReplacer.replace(MacroReplacer.MACRO_WORKDIR));
        }

        model.setLoggingGlbParameter("autoAppend", globals.getOrDefault(XMLConstantNames.XML_A_ROOT_LOGREPLACE, Defaults.get("loggerAutoAppend")));
        model.setLoggingGlbParameter("delim", globals.getOrDefault(XMLConstantNames.XML_A_ROOT_LOGDELIM, Defaults.get("loggerDelimiter")));
        model.setLoggingGlbParameter("decimalSeparator", globals.getOrDefault(XMLConstantNames.XML_A_ROOT_LOGDECIMALSEPARATOR, Defaults.get("loggerDecimalSeparator")));
    }
}