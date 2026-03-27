/**
 * This class is responsible for mapping the global settings from the {@link SimDescriptor} to the {@link CommonModel}
 * and vice-versa. This includes simulation parameters like seed, duration, and logging configurations.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.mapper;

import jmt.framework.data.MacroReplacer;
import jmt.gui.common.Defaults;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.SimDescriptor;
import jmt.gui.common.xml.serializer.WritingContext;

import java.io.File;
import java.util.Map;

public class GlobalSettingsMapper implements ModelElementMapper {

    // --- READING PHASE (XML -> DTO -> Model) ---

    @Override
    public void mapDtoToCommonModel(SimDescriptor dto, MappingContext context) {
        CommonModel model = context.getModel();
        Map<String, String> globals = dto.getGlobalAttributes();

        mapSimulationAndSeed(model, globals);
        mapTimeLimits(model, globals);
        mapIntervalsAndSamples(model, globals);
        mapStatisticsAndEvents(model, globals);
        mapLoggerParameters(model, globals);
    }

    private void mapSimulationAndSeed(CommonModel model, Map<String, String> globals) {
        String seed = globals.get(XML_A_ROOT_SEED);
        if (isPresent(seed)) {
            model.setUseRandomSeed(Boolean.FALSE);
            model.setSimulationSeed(Long.valueOf(seed));
        } else {
            model.setUseRandomSeed(Boolean.TRUE);
        }
    }

    private void mapTimeLimits(CommonModel model, Map<String, String> globals) {
        model.setMaximumDuration(getDouble(globals.get(XML_A_ROOT_DURATION), -1.0));
        model.setMaxSimulatedTime(getDouble(globals.get(XML_A_ROOT_SIMULATED), -1.0));
    }

    private void mapIntervalsAndSamples(CommonModel model, Map<String, String> globals) {
        String polling = globals.get(XML_A_ROOT_POLLING);
        if (isPresent(polling)) {
            model.setPollingInterval(Double.valueOf(polling));
        }

        String maxSamples = globals.get(XML_A_ROOT_MAXSAMPLES);
        if (isPresent(maxSamples)) {
            model.setMaxSimulationSamples(Integer.valueOf(maxSamples));
        }
    }

    private void mapStatisticsAndEvents(CommonModel model, Map<String, String> globals) {
        String disableStatistic = globals.get(XML_A_ROOT_DISABLESTATISTIC);
        if (isPresent(disableStatistic)) {
            model.setDisableStatistic(Boolean.valueOf(disableStatistic));
        }

        model.setMaxSimulationEvents(getInt(globals.get(XML_A_ROOT_MAXEVENTS), -1));
    }

    private void mapLoggerParameters(CommonModel model, Map<String, String> globals) {
        String logPath = globals.get(XML_A_ROOT_LOGPATH);
        if (isPresent(logPath) && new File(logPath).isDirectory()) {
            model.setLoggingGlbParameter(LOGGER_PARAM_PATH, new File(logPath).getAbsolutePath());
        } else {
            model.setLoggingGlbParameter(LOGGER_PARAM_PATH, MacroReplacer.replace(MacroReplacer.MACRO_WORKDIR));
        }

        model.setLoggingGlbParameter(LOGGER_PARAM_AUTO_APPEND, globals.getOrDefault(XML_A_ROOT_LOGREPLACE, Defaults.get("loggerAutoAppend")));
        model.setLoggingGlbParameter(LOGGER_PARAM_DELIM, globals.getOrDefault(XML_A_ROOT_LOGDELIM, Defaults.get("loggerDelimiter")));
        model.setLoggingGlbParameter(LOGGER_PARAM_DECIMAL_SEPARATOR, globals.getOrDefault(XML_A_ROOT_LOGDECIMALSEPARATOR, Defaults.get("loggerDecimalSeparator")));
    }


    // --- WRITING PHASE (Model -> DTO -> XML) ---

    @Override
    public void mapCommonModelToDto(SimDescriptor dto, WritingContext context) {
        CommonModel model = context.model; // Accessing the model from the writing context

        mapModelSimulationAndSeedToDto(model, dto);
        mapModelTimeLimitsToDto(model, dto);
        mapModelIntervalsAndSamplesToDto(model, dto);
        mapModelStatisticsAndEventsToDto(model, dto);
        mapModelLoggerParametersToDto(model, dto);
    }

    private void mapModelSimulationAndSeedToDto(CommonModel model, SimDescriptor dto) {
        Boolean useRandomSeed = model.getUseRandomSeed();
        // If random seed is not used and a seed is present, we write it to the DTO
        if (useRandomSeed != null && !useRandomSeed) {
            Long seed = model.getSimulationSeed();
            if (seed != null) {
                dto.addGlobalAttribute(XML_A_ROOT_SEED, seed.toString());
            }
        }
    }

    private void mapModelTimeLimitsToDto(CommonModel model, SimDescriptor dto) {
        Double maxDuration = model.getMaximumDuration();
        if (maxDuration != null) {
            dto.addGlobalAttribute(XML_A_ROOT_DURATION, maxDuration.toString());
        }

        Double maxSimulatedTime = model.getMaxSimulatedTime();
        if (maxSimulatedTime != null) {
            dto.addGlobalAttribute(XML_A_ROOT_SIMULATED, maxSimulatedTime.toString());
        }
    }

    private void mapModelIntervalsAndSamplesToDto(CommonModel model, SimDescriptor dto) {
        Double pollingInterval = model.getPollingInterval();
        if (pollingInterval != null) {
            dto.addGlobalAttribute(XML_A_ROOT_POLLING, pollingInterval.toString());
        }

        Integer maxSamples = model.getMaxSimulationSamples();
        if (maxSamples != null) {
            dto.addGlobalAttribute(XML_A_ROOT_MAXSAMPLES, maxSamples.toString());
        }
    }

    private void mapModelStatisticsAndEventsToDto(CommonModel model, SimDescriptor dto) {
        Boolean disableStatistic = model.getDisableStatistic();
        if (disableStatistic != null) {
            dto.addGlobalAttribute(XML_A_ROOT_DISABLESTATISTIC, disableStatistic.toString());
        }

        Integer maxEvents = model.getMaxSimulationEvents();
        if (maxEvents != null) {
            dto.addGlobalAttribute(XML_A_ROOT_MAXEVENTS, maxEvents.toString());
        }
    }

    private void mapModelLoggerParametersToDto(CommonModel model, SimDescriptor dto) {
        String logPath = model.getLoggingGlbParameter(LOGGER_PARAM_PATH);
        if (logPath != null) {
            dto.addGlobalAttribute(XML_A_ROOT_LOGPATH, logPath);
        }

        String autoAppend = model.getLoggingGlbParameter(LOGGER_PARAM_AUTO_APPEND);
        if (autoAppend != null) {
            dto.addGlobalAttribute(XML_A_ROOT_LOGREPLACE, autoAppend);
        }

        String delim = model.getLoggingGlbParameter(LOGGER_PARAM_DELIM);
        if (delim != null) {
            dto.addGlobalAttribute(XML_A_ROOT_LOGDELIM, delim);
        }

        String decimalSeparator = model.getLoggingGlbParameter(LOGGER_PARAM_DECIMAL_SEPARATOR);
        if (decimalSeparator != null) {
            dto.addGlobalAttribute(XML_A_ROOT_LOGDECIMALSEPARATOR, decimalSeparator);
        }
    }
}