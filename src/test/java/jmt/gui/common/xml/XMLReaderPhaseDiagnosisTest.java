package jmt.gui.common.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceType;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.forkStrategies.OutPath;
import jmt.gui.common.forkStrategies.ProbabilitiesFork;
import jmt.gui.common.routingStrategies.ProbabilityRouting;
import jmt.gui.common.xml.dto.BlockingRegionDescriptor;
import jmt.gui.common.xml.dto.ConnectionDescriptor;
import jmt.gui.common.xml.dto.MeasureDescriptor;
import jmt.gui.common.xml.dto.NodeDescriptor;
import jmt.gui.common.xml.dto.ParameterDescriptor;
import jmt.gui.common.xml.dto.SectionDescriptor;
import jmt.gui.common.xml.dto.SimDescriptor;
import jmt.gui.common.xml.dto.UserClassDescriptor;
import jmt.gui.common.xml.parsers.SimParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class XMLReaderPhaseDiagnosisTest {

    private enum Phase {
        PARSING,
        POPULATION,
        MIXED
    }

    private interface ParsingProbe {
        boolean isBroken(ParseContext context);
    }

    private interface PopulationProbe {
        boolean isBroken(PopulationContext context);
    }

    private static class ParseContext {
        private final Element simRoot;
        private final SimDescriptor descriptor;

        private ParseContext(Element simRoot, SimDescriptor descriptor) {
            this.simRoot = simRoot;
            this.descriptor = descriptor;
        }
    }

    private static class PopulationContext {
        private final SimDescriptor descriptor;
        private final CommonModel model;
        private final Throwable error;

        private PopulationContext(SimDescriptor descriptor, CommonModel model, Throwable error) {
            this.descriptor = descriptor;
            this.model = model;
            this.error = error;
        }
    }

    private static class DiagnosisCase {
        private final String failName;
        private final String xmlPath;
        private final Phase expected;
        private final ParsingProbe parsingProbe;
        private final PopulationProbe populationProbe;

        private DiagnosisCase(String failName, String xmlPath, Phase expected, ParsingProbe parsingProbe, PopulationProbe populationProbe) {
            this.failName = failName;
            this.xmlPath = xmlPath;
            this.expected = expected;
            this.parsingProbe = parsingProbe;
            this.populationProbe = populationProbe;
        }
    }

    @Test
    void classify_current_xmlreader_failures_by_phase() {
        List<DiagnosisCase> cases = Arrays.asList(
                new DiagnosisCase(
                        "test_parseBlockingRegions_branches",
                        "src/test/resources/jmt/gui/common/xml/parseBlockingRegions_branches.xml",
                        Phase.PARSING,
                        context -> {
                            if (context.descriptor.getBlockingRegions().isEmpty()) return true;
                            BlockingRegionDescriptor region = context.descriptor.getBlockingRegions().get(0);
                            return region.getRegionNodes().isEmpty();
                        },
                        context -> false
                ),
                new DiagnosisCase(
                        "test_parseStationDeadlinesAndQuanta_branches_fullcoverage",
                        "src/test/resources/jmt/gui/common/xml/parseStationDeadlinesAndQuanta_branches.xml",
                        Phase.PARSING,
                        context -> context.simRoot.getElementsByTagName("classSoftDeadlines").getLength() > 0
                                || context.simRoot.getElementsByTagName("quantumSize").getLength() > 0
                                || context.simRoot.getElementsByTagName("quantumSwitchoverTime").getLength() > 0,
                        context -> false
                ),
                new DiagnosisCase(
                        "test_parseMeasures_branches_fullcoverage",
                        "src/test/resources/jmt/gui/common/xml/parseMeasures_branches.xml",
                        Phase.MIXED,
                        context -> {
                            for (BlockingRegionDescriptor region : context.descriptor.getBlockingRegions()) {
                                if ("RegionForMeasure".equals(region.getName())) {
                                    return region.getRegionNodes().isEmpty();
                                }
                            }
                            return true;
                        },
                        context -> isMeasureRegionOrderBugPresent()
                ),
                new DiagnosisCase(
                        "test_SjAS04Model_6AS_L5",
                        "src/test/resources/jmt/gui/common/xml/23985_SjAS04Model_6AS-L5.xml",
                        Phase.POPULATION,
                        context -> false,
                        context -> context.error instanceof IndexOutOfBoundsException
                ),
                new DiagnosisCase(
                        "test_2FJ_2tasks_nestedOK",
                        "src/test/resources/jmt/gui/common/xml/641_2FJ_2tasks_nestedOK.xml",
                        Phase.POPULATION,
                        context -> !descriptorContainsValue(context.descriptor, "1.0"),
                        context -> !isClassDistributionLambda(context.model, "Class1", 1.0)
                ),
                new DiagnosisCase(
                        "test_ForkJoin_4Stat_2_SEED1",
                        "src/test/resources/jmt/gui/common/xml/10089_ForkJoin_4Stat_2 - SEED1.xml",
                        Phase.POPULATION,
                        context -> !descriptorContainsValue(context.descriptor, "1.0"),
                        context -> !isClassDistributionLambda(context.model, "Class1", 1.0)
                ),
                new DiagnosisCase(
                        "test_Sec5_3_FastTask_Quorum1_Wif_JSIMg",
                        "src/test/resources/jmt/gui/common/xml/520_Sec5_3_FastTask_Quorum1_Wif_JSIMg.xml",
                        Phase.POPULATION,
                        context -> !descriptorContainsValue(context.descriptor, "0.25"),
                        context -> !isClassDistributionLambda(context.model, "Class1", 0.25)
                ),
                new DiagnosisCase(
                        "test_10190_220423_2RotNogen_2Q",
                        "src/test/resources/jmt/gui/common/xml/10190_220423_2RotNogen_2Q.xml",
                        Phase.POPULATION,
                        context -> !descriptorContainsValue(context.descriptor, "0.30000000000003"),
                        context -> !isClassDistributionLambda(context.model, "Class1", 0.30000000000003)
                ),
                new DiagnosisCase(
                        "test_parseAndSetImpatienceParameters_branches_fullcoverage",
                        "src/test/resources/jmt/gui/common/xml/parseImpatience_branches.xml",
                        Phase.POPULATION,
                        context -> !descriptorContainsValue(context.descriptor, "4.0"),
                        context -> {
                            Object station = context.model.getStationByName("QueueWithImpatience");
                            Object clazz = context.model.getClassByName("Class1");
                            return station == null || clazz == null || context.model.getImpatienceType(station, clazz) != ImpatienceType.RENEGING;
                        }
                ),
                new DiagnosisCase(
                        "test_parseQueueSection_branches_fullcoverage",
                        "src/test/resources/jmt/gui/common/xml/parseQueue_branches.xml",
                        Phase.POPULATION,
                        context -> !descriptorContainsValue(context.descriptor, "9.0"),
                        context -> {
                            Object station = context.model.getStationByName("QueueNode");
                            Object clazz = context.model.getClassByName("Class2");
                            if (station == null || clazz == null) return true;
                            Object distObj = context.model.getRetrialDistribution(station, clazz);
                            if (!(distObj instanceof Distribution)) return true;
                            Double lambda = (Double) ((Distribution) distObj).getParameter("lambda").getValue();
                            return Math.abs(lambda - 9.0) > 1.0e-9;
                        }
                ),
                new DiagnosisCase(
                        "test_parseDelaySectionAndQueueSectionDelay_branches_fullcoverage",
                        "src/test/resources/jmt/gui/common/xml/parseDelaySections_branches.xml",
                        Phase.POPULATION,
                        context -> !descriptorContainsValue(context.descriptor, "2.0"),
                        context -> {
                            Object station = context.model.getStationByName("DelayNode");
                            Object clazz = context.model.getClassByName("Class1");
                            if (station == null || clazz == null) return true;
                            return !isStationClassLambda(context.model, station, clazz, 2.0);
                        }
                ),
                new DiagnosisCase(
                        "test_parseDistribution_branches_fullcoverage",
                        "src/test/resources/jmt/gui/common/xml/parseDistribution_branches.xml",
                        Phase.POPULATION,
                        context -> !descriptorContainsValue(context.descriptor, "0.5"),
                        context -> {
                            Object classKey = context.model.getClassByName("Class1");
                            if (classKey == null) return true;
                            Object distObj = context.model.getClassDistribution(classKey);
                            return distObj == null || !distObj.getClass().getSimpleName().contains("Burst");
                        }
                ),
                new DiagnosisCase(
                        "test_parseSemaphore_branches_fullcoverage",
                        "src/test/resources/jmt/gui/common/xml/parseSemaphore_branches.xml",
                        Phase.POPULATION,
                        context -> !descriptorContainsValue(context.descriptor, "threshold"),
                        context -> {
                            Object station = context.model.getStationByName("SemaphoreWithParam");
                            Object classKey = context.model.getClassByName("Class1");
                            if (station == null || classKey == null) return true;
                            Object strategy = context.model.getSemaphoreStrategy(station, classKey);
                            if (!(strategy instanceof jmt.gui.common.semaphoreStrategies.SemaphoreStrategy)) return true;
                            return ((jmt.gui.common.semaphoreStrategies.SemaphoreStrategy) strategy).getThreshold() != 2;
                        }
                ),
                new DiagnosisCase(
                        "test_parseRouter_branches_fullcoverage",
                        "src/test/resources/jmt/gui/common/xml/parseRouter_branches.xml",
                        Phase.POPULATION,
                        context -> !descriptorContainsValue(context.descriptor, "0.6"),
                        context -> {
                            Object station = context.model.getStationByName("ProbNode");
                            Object classKey = context.model.getClassByName("Class1");
                            Object target = context.model.getStationByName("LdNode");
                            if (station == null || classKey == null || target == null) return true;
                            Object routingObj = context.model.getRoutingStrategy(station, classKey);
                            if (!(routingObj instanceof ProbabilityRouting)) return true;
                            Double probability = ((ProbabilityRouting) routingObj).getValues().get(target);
                            return probability == null || Math.abs(probability - 0.6) > 1.0e-9;
                        }
                ),
                new DiagnosisCase(
                        "test_parseFork_branches_fullcoverage",
                        "src/test/resources/jmt/gui/common/xml/parseFork_branches.xml",
                        Phase.POPULATION,
                        context -> !descriptorContainsValue(context.descriptor, "0.7"),
                        context -> {
                            Object station = context.model.getStationByName("Fork1");
                            Object classKey = context.model.getClassByName("Class1");
                            Object sink = context.model.getStationByName("SinkP1");
                            if (station == null || classKey == null || sink == null) return true;
                            Object forkObj = context.model.getForkStrategy(station, classKey);
                            if (!(forkObj instanceof ProbabilitiesFork)) return true;
                            OutPath outPath = ((ProbabilitiesFork) forkObj).getOutDetails().get(sink);
                            return outPath == null || Math.abs(outPath.getProbability() - 0.7) > 1.0e-9;
                        }
                ),
                new DiagnosisCase(
                        "test_serverfeature_fullcoverage",
                        "src/test/resources/jmt/gui/common/xml/serverfeature_fullcoverage.xml",
                        Phase.POPULATION,
                        context -> !descriptorContainsValue(context.descriptor, "1.0"),
                        context -> {
                            Object station = context.model.getStationByName("Node1_Polling");
                            Object classKey = context.model.getClassByName("Class1");
                            if (station == null || classKey == null) return true;
                            Object distObj = context.model.getPollingSwitchoverDistribution(station, classKey);
                            return distObj == null || !distObj.getClass().getSimpleName().contains("Exponential");
                        }
                ),
                new DiagnosisCase(
                        "test_Workcopy3_onoff",
                        "src/test/resources/jmt/gui/common/xml/792_Workcopy3_onoff.xml",
                        Phase.POPULATION,
                        context -> !descriptorContainsValue(context.descriptor, "1.0"),
                        context -> {
                            Object station = context.model.getStationByName("Queue 1");
                            Object classKey = context.model.getClassByName("Class1");
                            Object target = context.model.getStationByName("Delay 1");
                            if (station == null || classKey == null || target == null) return true;
                            Object routingObj = context.model.getRoutingStrategy(station, classKey);
                            if (!(routingObj instanceof ProbabilityRouting)) return true;
                            Double probability = ((ProbabilityRouting) routingObj).getValues().get(target);
                            return probability == null || Math.abs(probability - 1.0) > 1.0e-9;
                        }
                ),
                new DiagnosisCase(
                        "test_conBeta13fesc",
                        "src/test/resources/jmt/gui/common/xml/225_conBeta13fesc.xml",
                        Phase.POPULATION,
                        context -> !descriptorContainsValue(context.descriptor, "2.5"),
                        context -> {
                            Object station = context.model.getStationByName("CPU");
                            Object classKey = context.model.getClassByName("Class0");
                            if (station == null || classKey == null) return true;
                            return !isStationClassLambda(context.model, station, classKey, 2.5);
                        }
                ),
                new DiagnosisCase(
                        "test_parseTimingSection_branches_fullcoverage",
                        "src/test/resources/jmt/gui/common/xml/parseTiming_branches.xml",
                        Phase.POPULATION,
                        context -> !descriptorContainsValue(context.descriptor, "2.5"),
                        context -> {
                            Object station = context.model.getStationByName("TransitionFull");
                            if (station == null) return true;
                            Object distribution = context.model.getFiringTimeDistribution(station, 0);
                            if (!(distribution instanceof Distribution)) return true;
                            Double lambda = (Double) ((Distribution) distribution).getParameter("lambda").getValue();
                            return Math.abs(lambda - 2.5) > 1.0e-9;
                        }
                ),
                new DiagnosisCase(
                        "test_240415_FCFS_QD",
                        "src/test/resources/jmt/gui/common/xml/4039_240415_FCFS_QD.xml",
                        Phase.POPULATION,
                        context -> !descriptorContainsValue(context.descriptor, "Infinity"),
                        context -> {
                            Object station = context.model.getStationByName("Queue 1");
                            Object classKey = context.model.getClassByName("Token");
                            if (station == null || classKey == null) return true;
                            Object distObj = context.model.getServiceTimeDistribution(station, classKey);
                            if (!(distObj instanceof Distribution)) return true;
                            Double lambda = (Double) ((Distribution) distObj).getParameter("lambda").getValue();
                            return !Double.isInfinite(lambda);
                        }
                )
        );

        List<Executable> checks = new ArrayList<Executable>();
        for (DiagnosisCase c : cases) {
            checks.add(() -> {
                ParseContext parseContext = parseXml(c.xmlPath);
                PopulationContext populationContext = populate(parseContext.descriptor);

                boolean parsingBroken = c.parsingProbe.isBroken(parseContext);
                boolean populationBroken = c.populationProbe.isBroken(populationContext);
                Phase actual = classify(parsingBroken, populationBroken);

                assertThat(actual)
                        .as(c.failName + " -> parsingBroken=" + parsingBroken + ", populationBroken=" + populationBroken)
                        .isEqualTo(c.expected);
            });
        }

        assertAll("Current XMLReader failures must be diagnosed by phase", checks);
    }

    private static Phase classify(boolean parsingBroken, boolean populationBroken) {
        if (parsingBroken && populationBroken) return Phase.MIXED;
        if (parsingBroken) return Phase.PARSING;
        return Phase.POPULATION;
    }

    private static ParseContext parseXml(String xmlPath) {
        Document doc = XMLReader.loadXML(new File(xmlPath).getAbsolutePath());
        assertThat(doc).as("Document should be loaded for " + xmlPath).isNotNull();

        Element root = doc.getDocumentElement();
        Element simRoot;
        if (root != null && "archive".equals(root.getTagName())) simRoot = XMLArchiver.getSimFromArchiveDocument(doc);
        else simRoot = root;

        assertThat(simRoot).as("Sim root should be present for " + xmlPath).isNotNull();

        SimDescriptor descriptor = new SimParser().parse(simRoot);
        return new ParseContext(simRoot, descriptor);
    }

    private static PopulationContext populate(SimDescriptor descriptor) {
        CommonModel model = new CommonModel();
        Throwable error = null;
        try {
            ModelPopulator.populate(descriptor, model);
        } catch (Throwable t) {
            error = t;
        }
        return new PopulationContext(descriptor, model, error);
    }

    private static boolean descriptorContainsValue(SimDescriptor descriptor, String expectedValue) {
        if (expectedValue == null) return false;
        for (NodeDescriptor node : descriptor.getNodes()) {
            for (SectionDescriptor section : node.getSections()) {
                for (ParameterDescriptor parameter : section.getParameters()) {
                    if (parameterTreeContains(parameter, expectedValue)) return true;
                }
            }
        }
        return false;
    }

    private static boolean parameterTreeContains(ParameterDescriptor parameter, String expectedValue) {
        if (parameter == null) return false;
        if (expectedValue.equals(parameter.getValue()) || expectedValue.equals(parameter.getName())) return true;
        for (ParameterDescriptor sub : parameter.getSubParameters()) {
            if (parameterTreeContains(sub, expectedValue)) return true;
        }
        return false;
    }

    private static boolean isClassDistributionLambda(CommonModel model, String className, double expected) {
        Object classKey = model.getClassByName(className);
        if (classKey == null) return false;
        Object distObj = model.getClassDistribution(classKey);
        if (!(distObj instanceof Distribution)) return false;
        Double lambda = (Double) ((Distribution) distObj).getParameter("lambda").getValue();
        return Math.abs(lambda - expected) <= 1.0e-9;
    }

    private static boolean isStationClassLambda(CommonModel model, Object station, Object classKey, double expected) {
        Object distObj = model.getServiceTimeDistribution(station, classKey);
        if (!(distObj instanceof Distribution)) return false;
        Double lambda = (Double) ((Distribution) distObj).getParameter("lambda").getValue();
        return Math.abs(lambda - expected) <= 1.0e-9;
    }

    private static boolean isMeasureRegionOrderBugPresent() {
        SimDescriptor descriptor = new SimDescriptor();

        UserClassDescriptor userClass = new UserClassDescriptor();
        userClass.setName("C1");
        userClass.setType("open");
        userClass.setReferenceSource("Q1");
        descriptor.addUserClass(userClass);

        NodeDescriptor node = new NodeDescriptor();
        node.setName("Q1");
        SectionDescriptor queue = new SectionDescriptor();
        queue.setClassName("Queue");
        SectionDescriptor server = new SectionDescriptor();
        server.setClassName("Server");
        SectionDescriptor router = new SectionDescriptor();
        router.setClassName("Router");
        node.addSection(queue);
        node.addSection(server);
        node.addSection(router);
        descriptor.addNode(node);

        ConnectionDescriptor connection = new ConnectionDescriptor();
        connection.setSource("Q1");
        connection.setTarget("Q1");
        descriptor.addConnection(connection);

        BlockingRegionDescriptor region = new BlockingRegionDescriptor();
        region.setName("R1");
        region.setType("SJF");
        region.addRegionNode("Q1");
        descriptor.addBlockingRegion(region);

        MeasureDescriptor measure = new MeasureDescriptor();
        measure.setType("Number of Customers");
        measure.setNodeType("region");
        measure.setReferenceNode("R1");
        measure.setReferenceUserClass("C1");
        measure.setAlpha("0.1");
        measure.setPrecision("0.01");
        measure.setVerbose("false");
        descriptor.addMeasure(measure);

        PopulationContext population = populate(descriptor);
        if (population.error != null) return true;
        if (population.model.getMeasureKeys().isEmpty()) return true;

        Object measureKey = population.model.getMeasureKeys().get(0);
        return population.model.getMeasureStation(measureKey) == null;
    }
}



