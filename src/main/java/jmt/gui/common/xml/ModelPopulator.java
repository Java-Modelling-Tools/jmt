package jmt.gui.common.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jmt.framework.data.MacroReplacer;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.Defaults;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.definitions.ServerType;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.dto.SimDescriptor;
import jmt.gui.common.xml.dto.UserClassDescriptor;

public final class ModelPopulator implements CommonConstants, XMLConstantNames {

    private ModelPopulator() {
        // Utility class.
    }

    public static void populate(SimDescriptor descriptor, CommonModel model) {
        if (descriptor == null || model == null) {
            return;
        }

        Map<String, Object> classKeys = new HashMap<String, Object>();
        Map<String, Object> stationKeys = new HashMap<String, Object>();
        Map<String, Object> regionKeys = new HashMap<String, Object>();
        Map<Object, String> refStationsPending = new HashMap<Object, String>();

        applyGlobalParameters(descriptor, model);
        createBaseClasses(descriptor, model, classKeys, refStationsPending);
        createBaseStations(descriptor, model, stationKeys);

        // Pass 3 and reference resolution will be implemented in the next refactoring step.
        if (regionKeys.isEmpty()) {
            // No-op to keep the declared local map intentionally in scope for upcoming passes.
        }
    }

    private static void applyGlobalParameters(SimDescriptor descriptor, CommonModel model) {
        Map<String, String> attrs = descriptor.getGlobalAttributes();
        if (attrs == null) {
            return;
        }

        Long seed = parseLong(firstNonBlank(attrs.get(XML_A_ROOT_SEED), attrs.get("seed")));
        if (seed != null) {
            model.setUseRandomSeed(Boolean.FALSE);
            model.setSimulationSeed(seed);
        } else {
            model.setUseRandomSeed(Boolean.TRUE);
        }

        Double maxDuration = parseDouble(firstNonBlank(attrs.get(XML_A_ROOT_DURATION), attrs.get("duration")));
        model.setMaximumDuration(maxDuration != null ? maxDuration : Double.valueOf(-1));

        Double maxSimulated = parseDouble(firstNonBlank(attrs.get(XML_A_ROOT_SIMULATED), attrs.get("maxSimulated")));
        model.setMaxSimulatedTime(maxSimulated != null ? maxSimulated : Double.valueOf(-1));

        Double polling = parseDouble(firstNonBlank(attrs.get(XML_A_ROOT_POLLING), attrs.get("polling")));
        if (polling != null) {
            model.setPollingInterval(polling);
        }

        Integer maxSamples = parseInteger(firstNonBlank(attrs.get(XML_A_ROOT_MAXSAMPLES), attrs.get("maxSamples")));
        if (maxSamples != null) {
            model.setMaxSimulationSamples(maxSamples);
        }

        Boolean disableStatistic = parseBoolean(firstNonBlank(
                attrs.get(XML_A_ROOT_DISABLESTATISTIC),
                attrs.get("disableStatistic"),
                attrs.get("disableStatisticStop")));
        if (disableStatistic != null) {
            model.setDisableStatistic(disableStatistic);
        }

        Integer maxEvents = parseInteger(firstNonBlank(attrs.get(XML_A_ROOT_MAXEVENTS), attrs.get("maxEvents")));
        model.setMaxSimulationEvents(maxEvents != null ? maxEvents : Integer.valueOf(-1));

        String logPath = firstNonBlank(attrs.get(XML_A_ROOT_LOGPATH), attrs.get("logPath"));
        if (hasText(logPath)) {
            File dir = new File(logPath);
            if (dir.isDirectory()) {
                model.setLoggingGlbParameter("path", dir.getAbsolutePath());
            } else {
                model.setLoggingGlbParameter("path", MacroReplacer.replace(MacroReplacer.MACRO_WORKDIR));
            }
        } else {
            model.setLoggingGlbParameter("path", MacroReplacer.replace(MacroReplacer.MACRO_WORKDIR));
        }

        String logReplaceMode = firstNonBlank(attrs.get(XML_A_ROOT_LOGREPLACE), attrs.get("logReplaceMode"));
        model.setLoggingGlbParameter("autoAppend", hasText(logReplaceMode)
                ? logReplaceMode
                : Defaults.get("loggerAutoAppend"));

        String logDelimiter = firstNonBlank(attrs.get(XML_A_ROOT_LOGDELIM), attrs.get("logDelimiter"));
        model.setLoggingGlbParameter("delim", hasText(logDelimiter)
                ? logDelimiter
                : Defaults.get("loggerDelimiter"));

        String logDecimalSeparator = firstNonBlank(
                attrs.get(XML_A_ROOT_LOGDECIMALSEPARATOR),
                attrs.get("logDecimalSeparator"));
        model.setLoggingGlbParameter("decimalSeparator", hasText(logDecimalSeparator)
                ? logDecimalSeparator
                : Defaults.get("loggerDecimalSeparator"));
    }

    private static void createBaseClasses(
            SimDescriptor descriptor,
            CommonModel model,
            Map<String, Object> classKeys,
            Map<Object, String> refStationsPending) {
        List<UserClassDescriptor> classes = descriptor.getUserClasses();
        if (classes == null) {
            return;
        }

        for (UserClassDescriptor userClass : classes) {
            if (userClass == null) {
                continue;
            }

            String name = userClass.getName();
            int type = "open".equalsIgnoreCase(userClass.getType())
                    ? CLASS_TYPE_OPEN
                    : CLASS_TYPE_CLOSED;

            Integer priority = parseInteger(userClass.getPriority());
            if (priority == null) {
                priority = Defaults.getAsInteger("classPriority");
            }

            Double softDeadline = parseDouble(userClass.getSoftDeadline());
            if (softDeadline == null) {
                softDeadline = Defaults.getAsDouble("classSoftDeadline");
            }

            Integer customers = parseInteger(userClass.getCustomers());
            if (customers == null) {
                customers = Defaults.getAsInteger("classPopulation");
            }

            Object key = model.addClass(name, type, priority, softDeadline, customers, null);
            classKeys.put(name, key);
            refStationsPending.put(key, userClass.getReferenceSource());
        }
    }

    private static void createBaseStations(
            SimDescriptor descriptor,
            CommonModel model,
            Map<String, Object> stationKeys) {
        List<NodeDescriptor> nodes = descriptor.getNodes();
        if (nodes == null) {
            return;
        }

        for (NodeDescriptor node : nodes) {
            if (node == null) {
                continue;
            }

            String stationType = determineStationType(node);
            Object key = model.addStation(node.getName(), stationType, 1, new ArrayList<ServerType>());
            stationKeys.put(node.getName(), key);
        }
    }

    private static String determineStationType(NodeDescriptor node) {
        String first = getSectionClassName(node, 0);
        String second = getSectionClassName(node, 1);
        String third = getSectionClassName(node, 2);

        if (CLASSNAME_SOURCE.equals(first)
                && CLASSNAME_TUNNEL.equals(second)
                && CLASSNAME_ROUTER.equals(third)) {
            return STATION_TYPE_SOURCE;
        } else if (CLASSNAME_SINK.equals(first)) {
            return STATION_TYPE_SINK;
        } else if (CLASSNAME_TERMINAL.equals(first)
                && CLASSNAME_TUNNEL.equals(second)
                && CLASSNAME_ROUTER.equals(third)) {
            return STATION_TYPE_TERMINAL;
        } else if (CLASSNAME_QUEUE.equals(first)
                && CLASSNAME_TUNNEL.equals(second)
                && CLASSNAME_ROUTER.equals(third)) {
            return STATION_TYPE_ROUTER;
        } else if (CLASSNAME_QUEUE.equals(first)
                && CLASSNAME_DELAY.equals(second)
                && CLASSNAME_ROUTER.equals(third)) {
            return STATION_TYPE_DELAY;
        } else if (CLASSNAME_QUEUE.equals(first)
                && (CLASSNAME_SERVER.equals(second)
                || CLASSNAME_PREEMPTIVESERVER.equals(second)
                || CLASSNAME_PSSERVER.equals(second)
                || CLASSNAME_PSSERVER_PRIORITY.equals(second)
                || CLASSNAME_EXHAUSTIVEPSERVER.equals(second)
                || CLASSNAME_LKPSERVER.equals(second)
                || CLASSNAME_GATEDPSERVER.equals(second))
                && CLASSNAME_ROUTER.equals(third)) {
            return STATION_TYPE_SERVER;
        } else if (CLASSNAME_QUEUE.equals(first)
                && CLASSNAME_TUNNEL.equals(second)
                && CLASSNAME_FORK.equals(third)) {
            return STATION_TYPE_FORK;
        } else if (CLASSNAME_JOIN.equals(first)
                && CLASSNAME_TUNNEL.equals(second)
                && CLASSNAME_ROUTER.equals(third)) {
            return STATION_TYPE_JOIN;
        } else if (CLASSNAME_QUEUE.equals(first)
                && CLASSNAME_LOGGER.equals(second)
                && CLASSNAME_ROUTER.equals(third)) {
            return STATION_TYPE_LOGGER;
        } else if (CLASSNAME_QUEUE.equals(first)
                && CLASSNAME_CLASSSWITCH.equals(second)
                && CLASSNAME_ROUTER.equals(third)) {
            return STATION_TYPE_CLASSSWITCH;
        } else if (CLASSNAME_SEMAPHORE.equals(first)
                && CLASSNAME_TUNNEL.equals(second)
                && CLASSNAME_ROUTER.equals(third)) {
            return STATION_TYPE_SEMAPHORE;
        } else if (CLASSNAME_JOIN.equals(first)
                && CLASSNAME_TUNNEL.equals(second)
                && CLASSNAME_FORK.equals(third)) {
            return STATION_TYPE_SCALER;
        } else if (CLASSNAME_STORAGE.equals(first)
                && CLASSNAME_TUNNEL.equals(second)
                && CLASSNAME_LINKAGE.equals(third)) {
            return STATION_TYPE_PLACE;
        } else if (CLASSNAME_ENABLING.equals(first)
                && CLASSNAME_TIMING.equals(second)
                && CLASSNAME_FIRING.equals(third)) {
            return STATION_TYPE_TRANSITION;
        }

        throw new IllegalArgumentException("Unable to determine station type for node: " + node.getName());
    }

    private static String getSectionClassName(NodeDescriptor node, int index) {
        List<SectionDescriptor> sections = node.getSections();
        if (sections == null || index < 0 || index >= sections.size()) {
            return null;
        }
        SectionDescriptor section = sections.get(index);
        return section != null ? section.getClassName() : null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }

    private static Long parseLong(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Integer parseInteger(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Double parseDouble(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return Double.valueOf(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Boolean parseBoolean(String value) {
        if (!hasText(value)) {
            return null;
        }
        return Boolean.valueOf(value.trim());
    }
}

