package jmt.gui.common.xml;

import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceType;
import jmt.engine.NetStrategies.ImpatienceStrategies.RenegingParameter;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.forkStrategies.CombFork;
import jmt.gui.common.forkStrategies.OutPath;
import jmt.gui.common.forkStrategies.ProbabilitiesFork;
import jmt.gui.common.joinStrategies.GuardJoin;
import jmt.gui.common.joinStrategies.NormalJoin;
import jmt.gui.common.joinStrategies.PartialJoin;
import jmt.gui.common.routingStrategies.*;
import jmt.gui.common.semaphoreStrategies.NormalSemaphore;
import jmt.gui.common.serviceStrategies.LDStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Integration test suite for {@link XMLReader#parseXML(org.w3c.dom.Element, CommonModel)}.
 * <p>
 * Purpose: verify that the public contract of parseXML is preserved across refactoring.
 * Every test targets one specific behaviour of the method; no test is "omnibus".
 * Each test is fully self-contained via an inline XML string.
 */
class XMLReaderTest {

    // -------------------------------------------------------------------------
    // Infrastructure
    // -------------------------------------------------------------------------

    private DocumentBuilder documentBuilder;
    private CommonModel model;

    @BeforeEach
    void setUp() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setValidating(false);
        try {
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {
            // Best-effort: keep parser creation resilient across JDK/XML implementations.
        }
        documentBuilder = factory.newDocumentBuilder();
        model = new CommonModel();
    }


    /**
     * Parse an inline XML string and return its root Element ready for XMLReader.
     */
    private org.w3c.dom.Element rootOf(String xml) throws Exception {
        Document doc = documentBuilder.parse(new InputSource(new StringReader(xml)));
        return doc.getDocumentElement();
    }

    // =========================================================================
    // 1. ROOT ATTRIBUTES
    // =========================================================================

    @Test
    void root_parsesAllSimAttributes() throws Exception {
        String tmpDir = System.getProperty("java.io.tmpdir");

        org.w3c.dom.Element root = rootOf(
                "<sim seed=\"42\"" +
                        "     maxTime=\"100.5\"" +
                        "     maxSimulated=\"50.0\"" +
                        "     polling=\"1.5\"" +
                        "     maxSamples=\"5000\"" +
                        "     disableStatisticStop=\"true\"" +
                        "     maxEvents=\"9999\"" +
                        "     logDelimiter=\"|\"" +
                        "     logDecimalSeparator=\",\"" +
                        "     logReplaceMode=\"1\"" +
                        "     logPath=\"" + tmpDir + "\"/>"
        );
        XMLReader.parseXML(root, model);

        assertThat(model.getUseRandomSeed()).isFalse();
        assertThat(model.getSimulationSeed()).isEqualTo(42L);
        assertThat(model.getMaximumDuration()).isEqualTo(100.5);
        assertThat(model.getMaxSimulatedTime()).isEqualTo(50.0);
        assertThat(model.getPollingInterval()).isEqualTo(1.5);
        assertThat(model.getMaxSimulationSamples()).isEqualTo(5000);
        assertThat(model.getDisableStatistic()).isTrue();
        assertThat(model.getMaxSimulationEvents()).isEqualTo(9999);
        assertThat(model.getLoggingGlbParameter("delim")).isEqualTo("|");
        assertThat(model.getLoggingGlbParameter("decimalSeparator")).isEqualTo(",");
        assertThat(model.getLoggingGlbParameter("autoAppend")).isEqualTo("1");
        assertThat(model.getLoggingGlbParameter("path")).isNotNull().isNotEmpty();
    }


    /**
     * Controlla che i valori di deafult vengano impostati
     *
     */
    @Test
    void root_withoutOptionalAttributes_setsDefaults() throws Exception {
        XMLReader.parseXML(rootOf("<sim/>"), model);

        assertThat(model.getUseRandomSeed()).isTrue();
        assertThat(model.getMaximumDuration()).isEqualTo(-1.0);
        assertThat(model.getMaxSimulationEvents()).isEqualTo(-1);
    }

    // =========================================================================
    // 2. CLASSES
    // =========================================================================

    @Test
    void classes_openClass_isAddedWithCorrectAttributes() throws Exception {
        org.w3c.dom.Element root = rootOf(
                "<sim>" +
                        "  <userClass name=\"OpenC\" type=\"open\" priority=\"2\" softDeadline=\"5.5\"/>" +
                        "</sim>");
        XMLReader.parseXML(root, model);

        Vector<Object> open = model.getOpenClassKeys();
        assertThat(open).hasSize(1);
        Object k = open.get(0);
        assertThat(model.getClassName(k)).isEqualTo("OpenC");
        assertThat(model.getClassType(k)).isEqualTo(CommonConstants.CLASS_TYPE_OPEN);
        assertThat(model.getClassPriority(k)).isEqualTo(2);
        assertThat(model.getClassSoftDeadline(k)).isEqualTo(5.5);
    }

    @Test
    void classes_closedClass_isAddedWithCorrectAttributes() throws Exception {
        org.w3c.dom.Element root = rootOf(
                "<sim>" +
                        "  <userClass name=\"ClosedC\" type=\"closed\" customers=\"30\" priority=\"3\"/>" +
                        "</sim>");
        XMLReader.parseXML(root, model);

        Vector<Object> closed = model.getClosedClassKeys();
        assertThat(closed).hasSize(1);
        Object k = closed.get(0);
        assertThat(model.getClassName(k)).isEqualTo("ClosedC");
        assertThat(model.getClassType(k)).isEqualTo(CommonConstants.CLASS_TYPE_CLOSED);
        assertThat(model.getClassPopulation(k)).isEqualTo(30);
        assertThat(model.getClassPriority(k)).isEqualTo(3);
    }

    @Test
    void classes_openClass_referenceSourceIsResolvedAfterStationsAreLoaded() throws Exception {
        org.w3c.dom.Element root = rootOf(
                "<sim>" +
                        "  <userClass name=\"OC\" type=\"open\" referenceSource=\"MySrc\"/>" +
                        "  <node name=\"MySrc\">" +
                        "    <section className=\"RandomSource\">" +
                        "      <parameter name=\"ServiceStrategy\">" +
                        "        <refClass>OC</refClass>" +
                        "        <subParameter classPath=\"jmt.engine.NetStrategies.ServiceStrategies.ZeroStrategy\"/>" +
                        "      </parameter>" +
                        "    </section>" +
                        "    <section className=\"ServiceTunnel\"/>" +
                        "    <section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>" +
                        "  </node>" +
                        "</sim>");
        XMLReader.parseXML(root, model);

        Object classKey = model.getClassByName("OC");
        Object stationKey = model.getStationByName("MySrc");
        assertThat(classKey).isNotNull();
        assertThat(stationKey).isNotNull();
        assertThat(model.getClassRefStation(classKey))
                .as("Reference source must resolve to the actual station key")
                .isEqualTo(stationKey);
    }

    @Test
    void classes_multipleClasses_allAddedInOrder() throws Exception {
        org.w3c.dom.Element root = rootOf(
                "<sim>" +
                        "  <userClass name=\"A\" type=\"open\"/>" +
                        "  <userClass name=\"B\" type=\"closed\" customers=\"10\"/>" +
                        "  <userClass name=\"C\" type=\"open\"/>" +
                        "</sim>");
        XMLReader.parseXML(root, model);

        assertThat(model.getOpenClassKeys()).hasSize(2);
        assertThat(model.getClosedClassKeys()).hasSize(1);
        assertThat(model.getClassByName("A")).isNotNull();
        assertThat(model.getClassByName("B")).isNotNull();
        assertThat(model.getClassByName("C")).isNotNull();
    }

    // =========================================================================
    // 3. STATION TYPES — correct type recognition
    // =========================================================================
    @ParameterizedTest(name = "{0} is recognized as station type {1}")
    @MethodSource("stationTypeProvider")
    void station_isRecognizedAndAdded(String stationName, String expectedType, String xml) throws Exception {
        XMLReader.parseXML(rootOf(xml), model);
        assertThat(model.getStationType(model.getStationByName(stationName)))
                .isEqualTo(expectedType);
    }

    private static Stream<Arguments> stationTypeProvider() {
        return Stream.of(
                Arguments.of("Src", CommonConstants.STATION_TYPE_SOURCE,
                        minimalSource("Src", "C1")),

                Arguments.of("MySink", CommonConstants.STATION_TYPE_SINK,
                        "<sim>" +
                                "  <userClass name=\"C1\" type=\"closed\"/>" +
                                "  <node name=\"MySink\"><section className=\"JobSink\"/></node>" +
                                "</sim>"),

                Arguments.of("R", CommonConstants.STATION_TYPE_ROUTER,
                        "<sim>" +
                                "  <userClass name=\"C1\" type=\"closed\"/>" +
                                "  <node name=\"R\">" +
                                "    <section className=\"Queue\"/>" +
                                "    <section className=\"ServiceTunnel\"/>" +
                                "    <section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>" +
                                "  </node>" +
                                "</sim>"),

                Arguments.of("D", CommonConstants.STATION_TYPE_DELAY,
                        minimalDelay("D", "C1")),

                Arguments.of("S", CommonConstants.STATION_TYPE_SERVER,
                        minimalServer("S", "C1", 1)),

                Arguments.of("F", CommonConstants.STATION_TYPE_FORK,
                        "<sim>" +
                                "  <userClass name=\"C1\" type=\"closed\"/>" +
                                "  <node name=\"F\">" +
                                "    <section className=\"Queue\"/>" +
                                "    <section className=\"ServiceTunnel\"/>" +
                                "    <section className=\"Fork\">" +
                                "      <parameter name=\"jobsPerLink\"><value>1</value></parameter>" +
                                "      <parameter name=\"block\"><value>-1</value></parameter>" +
                                "      <parameter name=\"isSimplifiedFork\"><value>true</value></parameter>" +
                                "    </section>" +
                                "  </node>" +
                                "</sim>"),

                Arguments.of("J", CommonConstants.STATION_TYPE_JOIN,
                        minimalJoin("J", "C1")),

                Arguments.of("L", CommonConstants.STATION_TYPE_LOGGER,
                        "<sim>" +
                                "  <userClass name=\"C1\" type=\"closed\"/>" +
                                "  <node name=\"L\">" +
                                "    <section className=\"Queue\"><parameter name=\"size\"><value>-1</value></parameter></section>" +
                                "    <section className=\"LogTunnel\">" +
                                "      <parameter name=\"logfileName\"><value>out.csv</value></parameter>" +
                                "    </section>" +
                                "    <section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>" +
                                "  </node>" +
                                "</sim>"),

                Arguments.of("CS", CommonConstants.STATION_TYPE_CLASSSWITCH,
                        "<sim>" +
                                "  <userClass name=\"C1\" type=\"closed\"/>" +
                                "  <userClass name=\"C2\" type=\"closed\"/>" +
                                "  <node name=\"CS\">" +
                                "    <section className=\"Queue\"/>" +
                                "    <section className=\"ClassSwitch\">" +
                                "      <parameter name=\"matrix\">" +
                                "        <refClass>C1</refClass>" +
                                "        <subParameter><refClass>C2</refClass><subParameter><value>1.0</value></subParameter></subParameter>" +
                                "      </parameter>" +
                                "    </section>" +
                                "    <section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>" +
                                "  </node>" +
                                "</sim>"),

                Arguments.of("Sem", CommonConstants.STATION_TYPE_SEMAPHORE,
                        minimalSemaphore("Sem", "C1", 3)),

                Arguments.of("P", CommonConstants.STATION_TYPE_PLACE,
                        minimalPlace("P", "C1")),

                Arguments.of("T", CommonConstants.STATION_TYPE_TRANSITION,
                        minimalTransition("T")),

                Arguments.of("SC", CommonConstants.STATION_TYPE_SCALER,
                        "<sim>" +
                                "  <userClass name=\"C1\" type=\"closed\"/>" +
                                "  <node name=\"SC\">" +
                                "    <section className=\"Join\">" +
                                "      <parameter name=\"JoinStrategy\">" +
                                "        <refClass>C1</refClass>" +
                                "        <subParameter classPath=\"jmt.engine.NetStrategies.JoinStrategies.NormalJoin\"/>" +
                                "      </parameter>" +
                                "    </section>" +
                                "    <section className=\"ServiceTunnel\"/>" +
                                "    <section className=\"Fork\">" +
                                "      <parameter name=\"jobsPerLink\"><value>1</value></parameter>" +
                                "      <parameter name=\"block\"><value>-1</value></parameter>" +
                                "      <parameter name=\"isSimplifiedFork\"><value>true</value></parameter>" +
                                "    </section>" +
                                "  </node>" +
                                "</sim>")
        );
    }

    // =========================================================================
    // 4. QUEUE SECTION — queue strategies and drop rules
    // =========================================================================

    @ParameterizedTest(name = "{0} → queueStrategy={1}, stationStrategy={2}")
    @MethodSource("putStrategyMappingProvider")
    void queue_putStrategy_mapsCorrectly(
            String strategyName, String expectedQueueStrategy, String expectedStationStrategy) throws Exception {

        String xml = strategyName.equals("TailStrategy") || strategyName.equals("TailStrategyPriority")
                || strategyName.equals("FCFSPRStrategy")
                ? serverWithPutStrategy(strategyName, "C1")
                : serverWithNamedPutStrategy(strategyName, "C1");

        XMLReader.parseXML(rootOf(xml), model);
        Object sk = model.getStationByName("S");
        Object ck = model.getClassByName("C1");

        if (expectedQueueStrategy != null) {
            assertThat(model.getQueueStrategy(sk, ck)).isEqualTo(expectedQueueStrategy);
        }
        assertThat(model.getStationQueueStrategy(sk)).isEqualTo(expectedStationStrategy);
    }

    private static Stream<Arguments> putStrategyMappingProvider() {
        return Stream.of(
                Arguments.of("TailStrategy", CommonConstants.QUEUE_STRATEGY_FCFS, CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE),
                Arguments.of("TailStrategyPriority", null, CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY),
                Arguments.of("HeadStrategy", CommonConstants.QUEUE_STRATEGY_LCFS, CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE),
                Arguments.of("RandStrategy", CommonConstants.QUEUE_STRATEGY_RAND, CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE),
                Arguments.of("FCFSPRStrategy", CommonConstants.QUEUE_STRATEGY_FCFS_PR, CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE)
        );
    }

    @Test
    void queue_dropRuleIsFiniteDrop_whenDropStrategyIsDrop() throws Exception {
        XMLReader.parseXML(rootOf(minimalServer("S", "C1", 1)), model);
        Object sk = model.getStationByName("S");
        Object ck = model.getClassByName("C1");
        assertThat(model.getDropRule(sk, ck)).isEqualTo(CommonConstants.FINITE_DROP);
    }

    @Test
    void queue_capacityIsReadFromSizeParameter() throws Exception {
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <node name=\"S\">" +
                        "    <section className=\"Queue\">" +
                        "      <parameter name=\"size\"><value>20</value></parameter>" +
                        "      <parameter name=\"dropStrategies\">" +
                        "        <refClass>C1</refClass><subParameter><value>drop</value></subParameter>" +
                        "      </parameter>" +
                        "    </section>" +
                        "    <section className=\"Server\">" +
                        "      <parameter name=\"maxJobs\"><value>1</value></parameter>" +
                        "      <parameter name=\"maxRunning\"><value>-1</value></parameter>" +
                        "    </section>" +
                        "    <section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>" +
                        "  </node>" +
                        "</sim>"), model);
        assertThat(model.getStationQueueCapacity(model.getStationByName("S"))).isEqualTo(20);
    }

    // =========================================================================
    // 5. SERVER SECTION — number of servers, PS, polling
    // =========================================================================

    @Test
    void server_numberOfServersIsReadFromMaxJobs() throws Exception {
        XMLReader.parseXML(rootOf(minimalServer("S", "C1", 4)), model);
        assertThat(model.getStationNumberOfServers(model.getStationByName("S"))).isEqualTo(4);
    }

    @Test
    void server_psServer_setsPS_QueueStrategy() throws Exception {
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <node name=\"S\">" +
                        "    <section className=\"Queue\"/>" +
                        "    <section className=\"PSServer\">" +
                        "      <parameter name=\"maxJobs\"><value>1</value></parameter>" +
                        "      <parameter name=\"maxRunning\"><value>-1</value></parameter>" +
                        "    </section>" +
                        "    <section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>" +
                        "  </node>" +
                        "</sim>"), model);

        Object sk = model.getStationByName("S");
        assertThat(model.getStationQueueStrategy(sk)).isEqualTo(CommonConstants.STATION_QUEUE_STRATEGY_PSSERVER);
        assertThat(model.getQueueStrategy(sk, model.getClassByName("C1"))).isEqualTo(CommonConstants.QUEUE_STRATEGY_PS);
    }

    // =========================================================================
    // 6. ROUTING STRATEGIES
    // =========================================================================

    @Test
    void routing_probabilityRouting_stationProbabilitiesAreResolvedCorrectly() throws Exception {
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <node name=\"Dest\">" + minimalDelaySections() + "</node>" +
                        "  <node name=\"Router\">" +
                        "    <section className=\"Queue\"/>" +
                        "    <section className=\"ServiceTunnel\"/>" +
                        "    <section className=\"Router\">" +
                        "      <parameter name=\"RoutingStrategy\">" +
                        "        <refClass>C1</refClass>" +
                        "        <subParameter classPath=\"jmt.engine.NetStrategies.RoutingStrategies.EmpiricalStrategy\">" +
                        "          <subParameter name=\"EmpiricalEntryArray\">" +
                        "            <subParameter>" +
                        "              <subParameter name=\"stationName\"><value>Dest</value></subParameter>" +
                        "              <subParameter name=\"probability\"><value>0.6</value></subParameter>" +
                        "            </subParameter>" +
                        "          </subParameter>" +
                        "        </subParameter>" +
                        "      </parameter>" +
                        "    </section>" +
                        "  </node>" +
                        "</sim>"), model);

        Object routerKey = model.getStationByName("Router");
        Object destKey = model.getStationByName("Dest");
        Object classKey = model.getClassByName("C1");

        Object rs = model.getRoutingStrategy(routerKey, classKey);
        assertThat(rs).isInstanceOf(ProbabilityRouting.class);
        Map<Object, Double> values = ((ProbabilityRouting) rs).getValues();
        assertThat(values).containsEntry(destKey, 0.6);
    }

    @Test
    void routing_probabilityRouting_multipleStations_accumulatesDeferredEntriesAcrossParse() throws Exception {
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <node name=\"DestA\">" + minimalDelaySections() + "</node>" +
                        "  <node name=\"DestB\">" + minimalDelaySections() + "</node>" +
                        "  <node name=\"RouterA\">" +
                        "    <section className=\"Queue\"/>" +
                        "    <section className=\"ServiceTunnel\"/>" +
                        "    <section className=\"Router\">" +
                        "      <parameter name=\"RoutingStrategy\">" +
                        "        <refClass>C1</refClass>" +
                        "        <subParameter classPath=\"jmt.engine.NetStrategies.RoutingStrategies.EmpiricalStrategy\">" +
                        "          <subParameter name=\"EmpiricalEntryArray\">" +
                        "            <subParameter>" +
                        "              <subParameter name=\"stationName\"><value>DestA</value></subParameter>" +
                        "              <subParameter name=\"probability\"><value>1.0</value></subParameter>" +
                        "            </subParameter>" +
                        "          </subParameter>" +
                        "        </subParameter>" +
                        "      </parameter>" +
                        "    </section>" +
                        "  </node>" +
                        "  <node name=\"RouterB\">" +
                        "    <section className=\"Queue\"/>" +
                        "    <section className=\"ServiceTunnel\"/>" +
                        "    <section className=\"Router\">" +
                        "      <parameter name=\"RoutingStrategy\">" +
                        "        <refClass>C1</refClass>" +
                        "        <subParameter classPath=\"jmt.engine.NetStrategies.RoutingStrategies.EmpiricalStrategy\">" +
                        "          <subParameter name=\"EmpiricalEntryArray\">" +
                        "            <subParameter>" +
                        "              <subParameter name=\"stationName\"><value>DestB</value></subParameter>" +
                        "              <subParameter name=\"probability\"><value>1.0</value></subParameter>" +
                        "            </subParameter>" +
                        "          </subParameter>" +
                        "        </subParameter>" +
                        "      </parameter>" +
                        "    </section>" +
                        "  </node>" +
                        "</sim>"), model);

        Object classKey = model.getClassByName("C1");
        Object routerA = model.getStationByName("RouterA");
        Object routerB = model.getStationByName("RouterB");
        Object destA = model.getStationByName("DestA");
        Object destB = model.getStationByName("DestB");

        ProbabilityRouting rsA = (ProbabilityRouting) model.getRoutingStrategy(routerA, classKey);
        ProbabilityRouting rsB = (ProbabilityRouting) model.getRoutingStrategy(routerB, classKey);
        assertThat(rsA.getValues()).containsEntry(destA, 1.0);
        assertThat(rsB.getValues()).containsEntry(destB, 1.0);
    }

    @Test
    void routing_weightedRoundRobin_weightsAreResolvedCorrectly() throws Exception {
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <node name=\"Dest\">" + minimalDelaySections() + "</node>" +
                        "  <node name=\"WRR\">" +
                        "    <section className=\"Queue\"/>" +
                        "    <section className=\"ServiceTunnel\"/>" +
                        "    <section className=\"Router\">" +
                        "      <parameter name=\"RoutingStrategy\">" +
                        "        <refClass>C1</refClass>" +
                        "        <subParameter classPath=\"jmt.engine.NetStrategies.RoutingStrategies.WeightedRoundRobinStrategy\">" +
                        "          <subParameter name=\"WeightedRoundRobinEntryArray\">" +
                        "            <subParameter>" +
                        "              <subParameter name=\"stationName\"><value>Dest</value></subParameter>" +
                        "              <subParameter name=\"weight\"><value>5</value></subParameter>" +
                        "            </subParameter>" +
                        "          </subParameter>" +
                        "        </subParameter>" +
                        "      </parameter>" +
                        "    </section>" +
                        "  </node>" +
                        "</sim>"), model);

        Object wrrKey = model.getStationByName("WRR");
        Object destKey = model.getStationByName("Dest");
        Object classKey = model.getClassByName("C1");

        Object rs = model.getRoutingStrategy(wrrKey, classKey);
        assertThat(rs).isInstanceOf(WeightedRoundRobinRouting.class);
        Map<Object, Integer> weights = ((WeightedRoundRobinRouting) rs).getWeights();
        assertThat(weights).containsEntry(destKey, 5);
    }

    @Test
    void routing_loadDependent_entriesAreResolvedToStationKeys() throws Exception {
        String ldrEnginePath = new LoadDependentRouting().getClassPath();
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <node name=\"Dest\">" + minimalDelaySections() + "</node>" +
                        "  <node name=\"S\">" +
                        "    <section className=\"Queue\"/>" +
                        "    <section className=\"Server\">" +
                        "      <parameter name=\"maxJobs\"><value>1</value></parameter>" +
                        "      <parameter name=\"maxRunning\"><value>-1</value></parameter>" +
                        "    </section>" +
                        "    <section className=\"Router\">" +
                        "      <parameter name=\"RoutingStrategy\">" +
                        "        <refClass>C1</refClass>" +
                        "        <subParameter classPath=\"" + ldrEnginePath + "\" name=\"C1\">" +
                        "          <subParameter name=\"LDParameterArray\">" +
                        "            <subParameter>" +
                        "              <subParameter name=\"from\"><value>0</value></subParameter>" +
                        "              <subParameter name=\"EmpiricalEntryArray\">" +
                        "                <subParameter name=\"EmpiricalEntry\">" +
                        "                  <subParameter name=\"stationName\"><value>Dest</value></subParameter>" +
                        "                  <subParameter name=\"probability\"><value>1.0</value></subParameter>" +
                        "                </subParameter>" +
                        "              </subParameter>" +
                        "            </subParameter>" +
                        "          </subParameter>" +
                        "        </subParameter>" +
                        "      </parameter>" +
                        "    </section>" +
                        "  </node>" +
                        "</sim>"), model);

        Object sk = model.getStationByName("S");
        Object ck = model.getClassByName("C1");
        Object destKey = model.getStationByName("Dest");

        Object rs = model.getRoutingStrategy(sk, ck);
        assertThat(rs).isInstanceOf(LoadDependentRouting.class);

        LoadDependentRouting ldr = (LoadDependentRouting) rs;
        // from=0 → entry verso Dest con probabilità 1.0
        Map<Object, Double> entries = ldr.getEmpiricalEntriesForFrom(0);
        assertThat(entries).isNotNull().containsEntry(destKey, 1.0);
    }

    @Test
    void routing_classSwitchRouting_branchAndClassProbabilitiesAreResolvedToStationAndClassKeys() throws Exception {
        String csrEnginePath = new ClassSwitchRouting().getClassPath();
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <userClass name=\"C2\" type=\"closed\"/>" +
                        "  <node name=\"Dest\">" + minimalDelaySections() + "</node>" +
                        "  <node name=\"S\">" +
                        "    <section className=\"Queue\"/>" +
                        "    <section className=\"Server\">" +
                        "      <parameter name=\"maxJobs\"><value>1</value></parameter>" +
                        "      <parameter name=\"maxRunning\"><value>-1</value></parameter>" +
                        "    </section>" +
                        "    <section className=\"Router\">" +
                        "      <parameter name=\"RoutingStrategy\">" +
                        "        <refClass>C1</refClass>" +
                        "        <subParameter classPath=\"" + csrEnginePath + "\" name=\"C1\">" +
                        "          <subParameter name=\"CSParameterArray\">" +
                        "            <subParameter name=\"CSParameter\">" +
                        "              <subParameter name=\"stationName\"><value>Dest</value></subParameter>" +
                        "              <subParameter name=\"probability\"><value>1.0</value></subParameter>" +
                        "              <subParameter name=\"CSClassArray\">" +
                        "                <subParameter name=\"CSClass\">" +
                        "                  <subParameter name=\"className\"><value>C2</value></subParameter>" +
                        "                  <subParameter name=\"probability\"><value>1.0</value></subParameter>" +
                        "                </subParameter>" +
                        "              </subParameter>" +
                        "            </subParameter>" +
                        "          </subParameter>" +
                        "        </subParameter>" +
                        "      </parameter>" +
                        "    </section>" +
                        "  </node>" +
                        "</sim>"), model);

        Object sk = model.getStationByName("S");
        Object ck = model.getClassByName("C1");
        Object destKey = model.getStationByName("Dest");
        Object c2Key = model.getClassByName("C2");

        Object rs = model.getRoutingStrategy(sk, ck);
        assertThat(rs).isInstanceOf(ClassSwitchRouting.class);
        ClassSwitchRouting csr = (ClassSwitchRouting) rs;

        assertThat(csr.getValues())
                .containsEntry(destKey, 1.0);

        assertThat(csr.getOutPaths())
                .containsKey(destKey);
        assertThat(csr.getOutPaths().get(destKey))
                .containsEntry(c2Key, 1.0);
    }

    // =========================================================================
    // 7. FORK STATION
    // =========================================================================

    @Test
    void fork_parameters_areReadCorrectly() throws Exception {
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <node name=\"F\">" +
                        "    <section className=\"Queue\"/>" +
                        "    <section className=\"ServiceTunnel\"/>" +
                        "    <section className=\"Fork\">" +
                        "      <parameter name=\"jobsPerLink\"><value>3</value></parameter>" +
                        "      <parameter name=\"block\"><value>8</value></parameter>" +
                        "      <parameter name=\"isSimplifiedFork\"><value>true</value></parameter>" +
                        "    </section>" +
                        "  </node>" +
                        "</sim>"), model);

        Object fk = model.getStationByName("F");
        assertThat(model.getStationNumberOfServers(fk)).isEqualTo(3);
        assertThat(model.getForkBlock(fk)).isEqualTo(8);
        assertThat(model.getIsSimplifiedFork(fk)).isTrue();
    }

    @Test
    void fork_probabilitiesFork_outPathsAreResolvedToStationKeys() throws Exception {
        XMLReader.parseXML(rootOf(minimalForkWithDest("F", "Dest", "C1", 1)), model);

        Object forkKey = model.getStationByName("F");
        Object destKey = model.getStationByName("Dest");
        Object classKey = model.getClassByName("C1");

        Object fs = model.getForkStrategy(forkKey, classKey);
        assertThat(fs).isNotNull().isInstanceOf(ProbabilitiesFork.class);

        @SuppressWarnings("unchecked")
        Map<Object, OutPath> outPaths = (Map<Object, OutPath>) ((ProbabilitiesFork) fs).getOutDetails();
        assertThat(outPaths).containsKey(destKey);
        assertThat(outPaths.get(destKey).getProbability()).isEqualTo(1.0, within(1e-9));
    }

    // =========================================================================
    // 8. JOIN STATION
    // =========================================================================
    @Test
    void join_normalJoin_strategyIsSetForAllClasses() throws Exception {
        XMLReader.parseXML(rootOf(minimalJoin("J", "C1")), model);
        Object js = model.getJoinStrategy(model.getStationByName("J"), model.getClassByName("C1"));
        assertThat(js).isNotNull().isInstanceOf(NormalJoin.class);
    }

    @Test
    void join_partialJoin_numRequiredIsSet() throws Exception {
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <node name=\"J\">" +
                        "    <section className=\"Join\">" +
                        "      <parameter name=\"JoinStrategy\">" +
                        "        <refClass>C1</refClass>" +
                        "        <subParameter classPath=\"jmt.engine.NetStrategies.JoinStrategies.PartialJoin\">" +
                        "          <subParameter name=\"numRequired\"><value>3</value></subParameter>" +
                        "        </subParameter>" +
                        "      </parameter>" +
                        "    </section>" +
                        "    <section className=\"ServiceTunnel\"/>" +
                        "    <section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>" +
                        "  </node>" +
                        "</sim>"), model);

        Object js = model.getJoinStrategy(model.getStationByName("J"), model.getClassByName("C1"));
        assertThat(js).isInstanceOf(PartialJoin.class);
        assertThat(((PartialJoin) js).getRequiredNum()).isEqualTo(3);
    }

    @Test
    void join_guardJoin_guardMixIsSet() throws Exception {
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <userClass name=\"C2\" type=\"closed\"/>" +
                        "  <node name=\"J\">" +
                        "    <section className=\"Join\">" +
                        "      <parameter name=\"JoinStrategy\">" +
                        "        <refClass>C1</refClass>" +
                        "        <subParameter classPath=\"jmt.engine.NetStrategies.JoinStrategies.GuardJoin\">" +
                        "          <subParameter name=\"Classes\">" +
                        "            <subParameter><value>C1</value></subParameter>" +
                        "            <subParameter><value>C2</value></subParameter>" +
                        "          </subParameter>" +
                        "          <subParameter name=\"Numbers\">" +
                        "            <subParameter><value>2</value></subParameter>" +
                        "            <subParameter><value>1</value></subParameter>" +
                        "          </subParameter>" +
                        "        </subParameter>" +
                        "      </parameter>" +
                        "    </section>" +
                        "    <section className=\"ServiceTunnel\"/>" +
                        "    <section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>" +
                        "  </node>" +
                        "</sim>"), model);

        Object js = model.getJoinStrategy(model.getStationByName("J"), model.getClassByName("C1"));
        assertThat(js).isInstanceOf(GuardJoin.class);
        Map<Object, Integer> guard = ((GuardJoin) js).getGuard();
        assertThat(guard).isNotEmpty()
                .containsEntry(model.getClassByName("C1"), 2)
                .containsEntry(model.getClassByName("C2"), 1);
    }

    // =========================================================================
    // 9. SEMAPHORE STATION
    // =========================================================================

    @Test
    void semaphore_normalSemaphore_thresholdIsSet() throws Exception {
        XMLReader.parseXML(rootOf(minimalSemaphore("Sem", "C1", 7)), model);
        Object ss = model.getSemaphoreStrategy(model.getStationByName("Sem"), model.getClassByName("C1"));
        assertThat(ss).isNotNull().isInstanceOf(NormalSemaphore.class);
        assertThat(((NormalSemaphore) ss).getThreshold()).isEqualTo(7);
    }

    // =========================================================================
    // 10. LOGGER STATION
    // =========================================================================

    @Test
    void logger_fileNameParameter_isReadCorrectly() throws Exception {
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <node name=\"L\">" +
                        "    <section className=\"Queue\"><parameter name=\"size\"><value>-1</value></parameter></section>" +
                        "    <section className=\"LogTunnel\">" +
                        "      <parameter name=\"logfileName\"><value>mylog.csv</value></parameter>" +
                        "      <parameter name=\"logLoggerName\"><value>true</value></parameter>" +
                        "      <parameter name=\"logTimeStamp\"><value>false</value></parameter>" +
                        "      <parameter name=\"logJobID\"><value>true</value></parameter>" +
                        "      <parameter name=\"logJobClass\"><value>false</value></parameter>" +
                        "      <parameter name=\"logTimeSameClass\"><value>true</value></parameter>" +
                        "      <parameter name=\"logTimeAnyClass\"><value>false</value></parameter>" +
                        "    </section>" +
                        "    <section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>" +
                        "  </node>" +
                        "</sim>"), model);

        jmt.engine.log.LoggerParameters lp = model.getLoggingParameters(model.getStationByName("L"));
        assertThat(lp).isNotNull();
        assertThat(lp.name).isEqualTo("mylog.csv");
        assertThat(lp.boolLoggername).isTrue();
        assertThat(lp.boolTimeStamp).isFalse();
        assertThat(lp.boolJobID).isTrue();
        assertThat(lp.boolJobClass).isFalse();
        assertThat(lp.boolTimeSameClass).isTrue();
        assertThat(lp.boolTimeAnyClass).isFalse();
    }

    // =========================================================================
    // 11. CLASS SWITCH STATION
    // =========================================================================

    @Test
    void classSwitch_matrix_probabilityIsStoredCorrectly() throws Exception {
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <userClass name=\"C2\" type=\"closed\"/>" +
                        "  <node name=\"CS\">" +
                        "    <section className=\"Queue\"/>" +
                        "    <section className=\"ClassSwitch\">" +
                        "      <parameter name=\"matrix\">" +
                        "        <refClass>C1</refClass>" +
                        "        <subParameter>" +
                        "          <refClass>C2</refClass>" +
                        "          <subParameter><value>0.8</value></subParameter>" +
                        "        </subParameter>" +
                        "      </parameter>" +
                        "    </section>" +
                        "    <section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>" +
                        "  </node>" +
                        "</sim>"), model);

        assertThat(model.getClassSwitchMatrix(
                model.getStationByName("CS"), model.getClassByName("C1"), model.getClassByName("C2")))
                .isEqualTo(0.8f, within(0.001f));
    }

    @Test
    void place_parametersAreReadCorrectly() throws Exception {
        XMLReader.parseXML(rootOf(minimalPlace("P", "C1")), model);
        Object pk = model.getStationByName("P");
        Object ck = model.getClassByName("C1");

        assertThat(model.getStationQueueCapacity(pk)).isEqualTo(10);
        assertThat(model.getQueueCapacity(pk, ck)).isEqualTo(5);
        assertThat(model.getDropRule(pk, ck)).isEqualTo(CommonConstants.FINITE_DROP);
        assertThat(model.getQueueStrategy(pk, ck)).isEqualTo(CommonConstants.QUEUE_STRATEGY_FCFS);
    }

    @Test
    void transition_parametersAreReadCorrectly() throws Exception {
        XMLReader.parseXML(rootOf(minimalTransition("T")), model);
        Object tk = model.getStationByName("T");

        assertThat(model.getTransitionModeName(tk, 0)).isEqualTo("FastMode");
        assertThat(model.getNumberOfServers(tk, 0)).isEqualTo(2);
        assertThat(model.getFiringPriority(tk, 0)).isEqualTo(5);
        assertThat(model.getFiringWeight(tk, 0)).isEqualTo(3.0);
    }
// =========================================================================
// 13. SERVICE STRATEGIES
// =========================================================================

    @Test
    void serviceStrategy_exponentialDistribution_instanceOfDistributionIsSet() throws Exception {
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <node name=\"S\">" +
                        "    <section className=\"Queue\"><parameter name=\"size\"><value>-1</value></parameter></section>" +
                        "    <section className=\"Server\">" +
                        "      <parameter name=\"maxJobs\"><value>1</value></parameter>" +
                        "      <parameter name=\"maxRunning\"><value>-1</value></parameter>" +
                        "      <parameter classPath=\"jmt.engine.NetStrategies.ServiceStrategy\">" +
                        "        <refClass>C1</refClass>" +
                        "        <subParameter classPath=\"jmt.engine.random.DistributionContainer\" name=\"DistributionContainer\">" +
                        "          <subParameter classPath=\"jmt.engine.random.Exponential\" name=\"Exponential\"/>" +
                        "          <subParameter classPath=\"jmt.engine.random.ExponentialPar\" name=\"distrPar\"/>" +
                        "        </subParameter>" +
                        "      </parameter>" +
                        "    </section>" +
                        "    <section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>" +
                        "  </node>" +
                        "</sim>"), model);

        Object dist = model.getServiceTimeDistribution(model.getStationByName("S"), model.getClassByName("C1"));
        assertThat(dist).isNotNull();
        assertThat(dist.getClass().getName()).contains("Exponential");
    }

    @Test
    void serviceStrategy_ldStrategy_rangeCountIsCorrect() throws Exception {
        String ldPath = LDStrategy.getEngineClassPath();
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <node name=\"S\">" +
                        "    <section className=\"Queue\"><parameter name=\"size\"><value>-1</value></parameter></section>" +
                        "    <section className=\"Server\">" +
                        "      <parameter name=\"maxJobs\"><value>1</value></parameter>" +
                        "      <parameter name=\"maxRunning\"><value>-1</value></parameter>" +
                        "      <parameter classPath=\"jmt.engine.NetStrategies.ServiceStrategy\">" +
                        "        <refClass>C1</refClass>" +
                        "        <subParameter classPath=\"" + ldPath + "\">" +
                        "          <subParameter name=\"LDParameterArray\">" +
                        "            <subParameter>" +
                        "              <subParameter name=\"from\"><value>1</value></subParameter>" +
                        "              <subParameter classPath=\"jmt.engine.random.Exponential\" name=\"Exponential\"/>" +
                        "              <subParameter classPath=\"jmt.engine.random.ExponentialPar\" name=\"distrPar\"/>" +
                        "              <subParameter name=\"mean\"><value>2.0</value></subParameter>" +
                        "            </subParameter>" +
                        "            <subParameter>" +
                        "              <subParameter name=\"from\"><value>5</value></subParameter>" +
                        "              <subParameter classPath=\"jmt.engine.random.Exponential\" name=\"Exponential\"/>" +
                        "              <subParameter classPath=\"jmt.engine.random.ExponentialPar\" name=\"distrPar\"/>" +
                        "              <subParameter name=\"mean\"><value>1.0</value></subParameter>" +
                        "            </subParameter>" +
                        "          </subParameter>" +
                        "        </subParameter>" +
                        "      </parameter>" +
                        "    </section>" +
                        "    <section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>" +
                        "  </node>" +
                        "</sim>"), model);

        Object dist = model.getServiceTimeDistribution(model.getStationByName("S"), model.getClassByName("C1"));
        assertThat(dist).isNotNull().isInstanceOf(LDStrategy.class);
        assertThat(((LDStrategy) dist).getRangeNumber()).as("Should have 2 ranges").isEqualTo(2);
    }

    @Test
    void serviceStrategy_delaySection_distributionIsSetPerClass() throws Exception {
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <node name=\"D\">" +
                        "    <section className=\"Queue\"/>" +
                        "    <section className=\"Delay\">" +
                        "      <parameter classPath=\"jmt.engine.NetStrategies.ServiceStrategy\">" +
                        "        <refClass>C1</refClass>" +
                        "        <subParameter classPath=\"jmt.engine.random.DistributionContainer\" name=\"DistributionContainer\">" +
                        "          <subParameter classPath=\"jmt.engine.random.Exponential\" name=\"Exponential\"/>" +
                        "          <subParameter classPath=\"jmt.engine.random.ExponentialPar\" name=\"distrPar\"/>" +
                        "        </subParameter>" +
                        "      </parameter>" +
                        "    </section>" +
                        "    <section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>" +
                        "  </node>" +
                        "</sim>"), model);

        Object dist = model.getServiceTimeDistribution(model.getStationByName("D"), model.getClassByName("C1"));
        assertThat(dist).isNotNull();
        assertThat(dist.getClass().getName()).contains("Exponential");
    }

// =========================================================================
// 14. CONNECTIONS
// =========================================================================

    @Test
    void connections_directedConnection_isRegisteredInModelButNotReverse() throws Exception {
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"open\"/>" +
                        "  <node name=\"A\">" + minimalSourceSections("C1") + "</node>" +
                        "  <node name=\"B\">" + minimalDelaySections() + "</node>" +
                        "  <connection source=\"A\" target=\"B\"/>" +
                        "</sim>"), model);

        Object a = model.getStationByName("A");
        Object b = model.getStationByName("B");
        assertThat(model.areConnected(a, b)).isTrue();
        assertThat(model.areConnected(b, a)).isFalse();
    }

    @Test
    void connections_multipleConnections_allRegistered() throws Exception {
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"open\"/>" +
                        "  <node name=\"A\">" + minimalSourceSections("C1") + "</node>" +
                        "  <node name=\"B\">" + minimalDelaySections() + "</node>" +
                        "  <node name=\"C\">" + minimalDelaySections() + "</node>" +
                        "  <connection source=\"A\" target=\"B\"/>" +
                        "  <connection source=\"B\" target=\"C\"/>" +
                        "</sim>"), model);

        Object a = model.getStationByName("A");
        Object b = model.getStationByName("B");
        Object c = model.getStationByName("C");
        assertThat(model.areConnected(a, b)).isTrue();
        assertThat(model.areConnected(b, c)).isTrue();
        assertThat(model.areConnected(a, c)).isFalse();
    }

// =========================================================================
// 15. MEASURES
// =========================================================================

    @Test
    void measures_standardAttributes_areStoredCorrectly() throws Exception {
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <node name=\"S\">" + minimalDelaySections() + "</node>" +
                        "  <measure referenceNode=\"S\" referenceUserClass=\"C1\" type=\"Throughput\"" +
                        "           alpha=\"0.05\" precision=\"0.07\" verbose=\"true\" nodeType=\"station\"/>" +
                        "</sim>"), model);

        Object mk = model.getMeasureKeys().get(0);
        assertThat(model.getMeasureAlpha(mk)).isEqualTo(0.95);
        assertThat(model.getMeasurePrecision(mk)).isEqualTo(0.07);
        assertThat(model.getMeasureStation(mk)).isEqualTo(model.getStationByName("S"));
        assertThat(model.getMeasureClass(mk)).isEqualTo(model.getClassByName("C1"));
        assertThat(model.getMeasureLog(mk)).isTrue();
    }

    @Test
    void measures_globalMeasure_stationKeyIsNull() throws Exception {
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <measure referenceUserClass=\"C1\" type=\"Throughput\"" +
                        "           alpha=\"0.05\" precision=\"0.03\" verbose=\"false\" nodeType=\"station\"/>" +
                        "</sim>"), model);

        assertThat(model.getMeasureStation(model.getMeasureKeys().get(0))).isNull();
    }

    @Test
    void measures_measureWithNoClass_classKeyIsNull() throws Exception {
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <node name=\"S\">" + minimalDelaySections() + "</node>" +
                        "  <measure referenceNode=\"S\" type=\"Throughput\"" +
                        "           alpha=\"0.05\" precision=\"0.03\" verbose=\"false\" nodeType=\"station\"/>" +
                        "</sim>"), model);

        assertThat(model.getMeasureClass(model.getMeasureKeys().get(0))).isNull();
    }

// =========================================================================
// 16. PRELOADING
// =========================================================================

    @Test
    void preload_jobCount_isSetCorrectlyOnStationAndClass() throws Exception {
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <node name=\"S\">" + minimalDelaySections() + "</node>" +
                        "  <preload>" +
                        "    <stationPopulations stationName=\"S\">" +
                        "      <classPopulation refClass=\"C1\" population=\"12\"/>" +
                        "    </stationPopulations>" +
                        "  </preload>" +
                        "</sim>"), model);

        assertThat(model.getPreloadedJobs(model.getStationByName("S"), model.getClassByName("C1")))
                .isEqualTo(12);
    }

    @Test
    void preload_multipleStationsAndClasses_allJobCounts_areSetIndependently() throws Exception {
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <userClass name=\"C2\" type=\"closed\"/>" +
                        "  <node name=\"S1\">" + minimalDelaySections() + "</node>" +
                        "  <node name=\"S2\">" + minimalDelaySections() + "</node>" +
                        "  <preload>" +
                        "    <stationPopulations stationName=\"S1\">" +
                        "      <classPopulation refClass=\"C1\" population=\"5\"/>" +
                        "      <classPopulation refClass=\"C2\" population=\"3\"/>" +
                        "    </stationPopulations>" +
                        "    <stationPopulations stationName=\"S2\">" +
                        "      <classPopulation refClass=\"C1\" population=\"7\"/>" +
                        "    </stationPopulations>" +
                        "  </preload>" +
                        "</sim>"), model);

        Object s1 = model.getStationByName("S1"), s2 = model.getStationByName("S2");
        Object c1 = model.getClassByName("C1"), c2 = model.getClassByName("C2");
        assertThat(model.getPreloadedJobs(s1, c1)).isEqualTo(5);
        assertThat(model.getPreloadedJobs(s1, c2)).isEqualTo(3);
        assertThat(model.getPreloadedJobs(s2, c1)).isEqualTo(7);
    }

// =========================================================================
// 17. BLOCKING REGIONS
// =========================================================================

    @Test
    void blockingRegion_parametersAreReadCorrectly() throws Exception {
        XMLReader.parseXML(rootOf(minimalBlockingRegion("BR", "S", "C1")), model);

        Object rk = model.getFCRegionKeys().get(0);
        Object sk = model.getStationByName("S");
        Object ck = model.getClassByName("C1");

        assertThat(model.getFCRegionKeys()).hasSize(1);
        assertThat(model.getRegionName(rk)).isEqualTo("BR");
        assertThat(model.getBlockingRegionStations(rk)).contains(sk);
        assertThat(model.getStationBlockingRegion(sk)).isEqualTo(rk);
        assertThat(model.getRegionCustomerConstraint(rk)).isEqualTo(10);
        assertThat(model.getRegionClassCustomerConstraint(rk, ck)).isEqualTo(4);
        assertThat(model.getRegionGroupCustomerConstraint(rk, 0)).isEqualTo(3);
        assertThat(model.getRegionGroupClassList(rk, 0)).contains(ck);
    }

// =========================================================================
// 18. SOURCE
// =========================================================================

    @Test
    void source_openClassDistribution_isAssignedFromSourceSection() throws Exception {
        XMLReader.parseXML(rootOf(minimalSource("Src", "OC")), model);
        assertThat(model.getClassDistribution(model.getClassByName("OC")))
                .as("Open class must have a distribution set by the source")
                .isNotNull();
    }

// =========================================================================
// 19. SOFT DEADLINES
// =========================================================================

    @Test
    void softDeadlines_perStationClass_areSetCorrectly() throws Exception {
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <node name=\"S\">" +
                        "    <section className=\"Queue\"/>" +
                        "    <section className=\"Delay\"><parameter name=\"ServiceStrategy\"/></section>" +
                        "    <section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>" +
                        "    <classSoftDeadlines><value>9.9</value></classSoftDeadlines>" +
                        "  </node>" +
                        "</sim>"), model);

        assertThat(model.getClassStationSoftDeadline(model.getStationByName("S"), model.getClassByName("C1")))
                .isEqualTo(9.9, within(1e-9));
    }

    // =========================================================================
    // Helpers — minimal XML fragments
    // =========================================================================

    /**
     * Minimal complete <sim> XML with an open-class Source station.
     */
    private static String minimalSource(String name, String className) {
        return "<sim>" +
                "  <userClass name=\"" + className + "\" type=\"open\" referenceSource=\"" + name + "\"/>" +
                "  <node name=\"" + name + "\">" +
                "    <section className=\"RandomSource\">" +
                "      <parameter name=\"ServiceStrategy\">" +
                "        <refClass>" + className + "</refClass>" +
                "        <subParameter classPath=\"jmt.engine.NetStrategies.ServiceStrategies.ZeroStrategy\"/>" +
                "      </parameter>" +
                "    </section>" +
                "    <section className=\"ServiceTunnel\"/>" +
                "    <section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>" +
                "  </node>" +
                "</sim>";
    }

    /**
     * Just the inner <section> elements for a Source (for embedding in larger XML).
     */
    private String minimalSourceSections(String className) {
        return "<section className=\"RandomSource\">" +
                "  <parameter name=\"ServiceStrategy\">" +
                "    <refClass>" + className + "</refClass>" +
                "    <subParameter classPath=\"jmt.engine.NetStrategies.ServiceStrategies.ZeroStrategy\"/>" +
                "  </parameter>" +
                "</section>" +
                "<section className=\"ServiceTunnel\"/>" +
                "<section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>";
    }

    /**
     * Minimal complete <sim> XML with a Delay station.
     */
    private static String minimalDelay(String name, String className) {
        return "<sim>" +
                "  <userClass name=\"" + className + "\" type=\"closed\"/>" +
                "  <node name=\"" + name + "\">" + minimalDelaySections() + "</node>" +
                "</sim>";
    }

    /**
     * Just the inner <section> elements for a Delay (for embedding in larger XML).
     */
    private static String minimalDelaySections() {
        return "<section className=\"Queue\"/>" +
                "<section className=\"Delay\"><parameter name=\"ServiceStrategy\"/></section>" +
                "<section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>";
    }

    /**
     * Minimal complete <sim> XML with a Server station with {@code servers} servers.
     */
    private static String minimalServer(String name, String className, int servers) {
        return "<sim>" +
                "  <userClass name=\"" + className + "\" type=\"closed\"/>" +
                "  <node name=\"" + name + "\">" +
                "    <section className=\"Queue\">" +
                "      <parameter name=\"size\"><value>-1</value></parameter>" +
                "      <parameter name=\"dropStrategies\">" +
                "        <refClass>" + className + "</refClass>" +
                "        <subParameter><value>drop</value></subParameter>" +
                "      </parameter>" +
                "      <parameter classPath=\"jmt.engine.NetStrategies.QueuePutStrategy\">" +
                "        <refClass>" + className + "</refClass>" +
                "        <subParameter classPath=\"jmt.engine.NetStrategies.QueuePutStrategies.TailStrategy\"/>" +
                "      </parameter>" +
                "    </section>" +
                "    <section className=\"Server\">" +
                "      <parameter name=\"maxJobs\"><value>" + servers + "</value></parameter>" +
                "      <parameter name=\"maxRunning\"><value>-1</value></parameter>" +
                "    </section>" +
                "    <section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>" +
                "  </node>" +
                "</sim>";
    }

    /**
     * Minimal complete <sim> XML with a Server using a specific queue put strategy class name,
     * used to test all strategy-to-constant mappings.
     */
    private String serverWithPutStrategy(String putStrategyName, String className) {
        return "<sim>" +
                "  <userClass name=\"" + className + "\" type=\"closed\"/>" +
                "  <node name=\"S\">" +
                "    <section className=\"Queue\">" +
                "      <parameter name=\"size\"><value>-1</value></parameter>" +
                "      <parameter classPath=\"jmt.engine.NetStrategies.QueuePutStrategy\">" +
                "        <refClass>" + className + "</refClass>" +
                "        <subParameter classPath=\"jmt.engine.NetStrategies.QueuePutStrategies." + putStrategyName + "\"/>" +
                "      </parameter>" +
                "    </section>" +
                "    <section className=\"Server\">" +
                "      <parameter name=\"maxJobs\"><value>1</value></parameter>" +
                "      <parameter name=\"maxRunning\"><value>-1</value></parameter>" +
                "    </section>" +
                "    <section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>" +
                "  </node>" +
                "</sim>";
    }

    /**
     * Minimal complete <sim> XML with a Join station (NormalJoin).
     */
    private static String minimalJoin(String name, String className) {
        return "<sim>" +
                "  <userClass name=\"" + className + "\" type=\"closed\"/>" +
                "  <node name=\"" + name + "\">" +
                "    <section className=\"Join\">" +
                "      <parameter name=\"JoinStrategy\">" +
                "        <refClass>" + className + "</refClass>" +
                "        <subParameter classPath=\"jmt.engine.NetStrategies.JoinStrategies.NormalJoin\"/>" +
                "      </parameter>" +
                "    </section>" +
                "    <section className=\"ServiceTunnel\"/>" +
                "    <section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>" +
                "  </node>" +
                "</sim>";
    }

    /**
     * Minimal complete <sim> XML with a Semaphore station with a given threshold.
     */
    private static String minimalSemaphore(String name, String className, int threshold) {
        return "<sim>" +
                "  <userClass name=\"" + className + "\" type=\"closed\"/>" +
                "  <node name=\"" + name + "\">" +
                "    <section className=\"Semaphore\">" +
                "      <parameter name=\"SemaphoreStrategy\">" +
                "        <refClass>" + className + "</refClass>" +
                "        <subParameter classPath=\"jmt.engine.NetStrategies.SemaphoreStrategies.NormalSemaphore\">" +
                "          <subParameter name=\"threshold\"><value>" + threshold + "</value></subParameter>" +
                "        </subParameter>" +
                "      </parameter>" +
                "    </section>" +
                "    <section className=\"ServiceTunnel\"/>" +
                "    <section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>" +
                "  </node>" +
                "</sim>";
    }

    /**
     * Minimal complete <sim> XML with a Place station.
     */
    private static String minimalPlace(String name, String className) {
        return "<sim>" +
                "  <userClass name=\"" + className + "\" type=\"closed\"/>" +
                "  <node name=\"" + name + "\">" +
                "    <section className=\"Storage\">" +
                "      <parameter name=\"totalCapacity\"><value>10</value></parameter>" +
                "      <parameter name=\"capacities\">" +
                "        <refClass>" + className + "</refClass><subParameter><value>5</value></subParameter>" +
                "      </parameter>" +
                "      <parameter name=\"dropRules\">" +
                "        <refClass>" + className + "</refClass><subParameter><value>drop</value></subParameter>" +
                "      </parameter>" +
                "      <parameter name=\"putStrategies\">" +
                "        <refClass>" + className + "</refClass>" +
                "        <subParameter classPath=\"jmt.engine.NetStrategies.QueuePutStrategies.TailStrategy\"/>" +
                "      </parameter>" +
                "    </section>" +
                "    <section className=\"ServiceTunnel\"/>" +
                "    <section className=\"Linkage\"/>" +
                "  </node>" +
                "</sim>";
    }

    /**
     * Minimal complete <sim> XML with a Transition station (single mode named FastMode).
     */
    private static String minimalTransition(String name) {
        return "<sim>" +
                "  <userClass name=\"C1\" type=\"closed\"/>" +
                "  <node name=\"" + name + "\">" +
                "    <section className=\"Enabling\"/>" +
                "    <section className=\"Timing\">" +
                "      <parameter name=\"modeNames\"><subParameter><value>FastMode</value></subParameter></parameter>" +
                "      <parameter name=\"numbersOfServers\"><subParameter><value>2</value></subParameter></parameter>" +
                "      <parameter name=\"firingPriorities\"><subParameter><value>5</value></subParameter></parameter>" +
                "      <parameter name=\"firingWeights\"><subParameter><value>3.0</value></subParameter></parameter>" +
                "    </section>" +
                "    <section className=\"Firing\"/>" +
                "  </node>" +
                "</sim>";
    }

    /**
     * Minimal complete <sim> XML with a Fork (ProbabilitiesFork) sending all traffic
     * to a Delay station named {@code destName}, with {@code jobsPerLink} jobs per link.
     */
    private String minimalForkWithDest(String forkName, String destName,
                                       String className, int jobsPerLink) {
        return "<sim>" +
                "  <userClass name=\"" + className + "\" type=\"closed\"/>" +
                "  <node name=\"" + destName + "\">" + minimalDelaySections() + "</node>" +
                "  <node name=\"" + forkName + "\">" +
                "    <section className=\"Queue\"/>" +
                "    <section className=\"ServiceTunnel\"/>" +
                "    <section className=\"Fork\">" +
                "      <parameter name=\"jobsPerLink\"><value>" + jobsPerLink + "</value></parameter>" +
                "      <parameter name=\"block\"><value>-1</value></parameter>" +
                "      <parameter name=\"isSimplifiedFork\"><value>true</value></parameter>" +
                "      <parameter name=\"ForkStrategy\">" +
                "        <refClass>" + className + "</refClass>" +
                "        <subParameter classPath=\"jmt.engine.NetStrategies.ForkStrategies.ProbabilitiesFork\">" +
                "          <subParameter name=\"EntryArray\">" +
                "            <subParameter>" +
                "              <subParameter>" +
                "                <subParameter><value>" + destName + "</value></subParameter>" +
                "                <subParameter><value>1.0</value></subParameter>" +
                "              </subParameter>" +
                "              <subParameter>" +
                "                <subParameter>" +
                "                  <subParameter><value>1</value></subParameter>" +
                "                  <subParameter><value>1.0</value></subParameter>" +
                "                </subParameter>" +
                "              </subParameter>" +
                "            </subParameter>" +
                "          </subParameter>" +
                "        </subParameter>" +
                "      </parameter>" +
                "    </section>" +
                "  </node>" +
                "</sim>";
    }

    /**
     * Minimal complete <sim> XML with one blocking region containing one station.
     */
    private String minimalBlockingRegion(String regionName, String stationName, String className) {
        return "<sim>" +
                "  <userClass name=\"" + className + "\" type=\"closed\"/>" +
                "  <node name=\"" + stationName + "\">" + minimalDelaySections() + "</node>" +
                "  <blockingRegion name=\"" + regionName + "\" type=\"default\">" +
                "    <regionNode nodeName=\"" + stationName + "\"/>" +
                "    <globalConstraint maxJobs=\"10\"/>" +
                "    <globalMemoryConstraint maxMemory=\"-1\"/>" +
                "    <classConstraint jobClass=\"" + className + "\" maxJobsPerClass=\"4\"/>" +
                "    <classMemoryConstraint jobClass=\"" + className + "\" maxMemoryPerClass=\"-1\"/>" +
                "    <dropRules jobClass=\"" + className + "\" dropThisClass=\"false\"/>" +
                "    <classWeight jobClass=\"" + className + "\" weight=\"1\"/>" +
                "    <classSize jobClass=\"" + className + "\" size=\"1\"/>" +
                "    <groupConstraint jobGroup=\"G1\" maxJobsPerGroup=\"3\"/>" +
                "    <groupMemoryConstraint jobGroup=\"G1\" maxMemoryPerGroup=\"-1\"/>" +
                "    <groupClassList>" +
                "      <groupClass jobGroup=\"G1\" jobClass=\"" + className + "\"/>" +
                "    </groupClassList>" +
                "  </blockingRegion>" +
                "</sim>";
    }


    // =========================================================================
    // Section 20 — Queue strategies (missing)
    // =========================================================================

    @ParameterizedTest(name = "{0} non attiva il polling quando usato come name")
    @MethodSource("queueGetStrategyNameProvider")
    void queue_queueGetStrategyInName_keepsNonPreemptiveScheduling(String getStrategyClassPath) throws Exception {
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <node name=\"S\">" +
                        "    <section className=\"Queue\">" +
                        "      <parameter name=\"" + getStrategyClassPath + "\"/>" +
                        "    </section>" +
                        "    <section className=\"Server\">" +
                        "      <parameter name=\"maxJobs\"><value>1</value></parameter>" +
                        "      <parameter name=\"maxRunning\"><value>-1</value></parameter>" +
                        "    </section>" +
                        "    <section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>" +
                        "  </node>" +
                        "</sim>"), model);

        Object sk = model.getStationByName("S");
        assertThat(model.getStationQueueStrategy(sk))
                .isEqualTo(CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE);
    }

    private static Stream<Arguments> queueGetStrategyNameProvider() {
        return Stream.of(
                Arguments.of("jmt.engine.NetStrategies.QueueGetStrategies.LimitedPollingGetStrategy"),
                Arguments.of("jmt.engine.NetStrategies.QueueGetStrategies.GatedPollingGetStrategy"),
                Arguments.of("jmt.engine.NetStrategies.QueueGetStrategies.ExhaustivePollingGetStrategy")
        );
    }

    @ParameterizedTest(name = "{0} → queueStrategy={1}, stationStrategy={2}")
    @MethodSource("namedPutStrategyMappingProvider")
    void queue_namedPutStrategy_mapsCorrectly(
            String strategyName, String expectedQueueStrategy, String expectedStationStrategy) throws Exception {
        XMLReader.parseXML(rootOf(serverWithNamedPutStrategy(strategyName, "C1")), model);
        Object sk = model.getStationByName("S");
        Object ck = model.getClassByName("C1");
        assertThat(model.getQueueStrategy(sk, ck)).isEqualTo(expectedQueueStrategy);
        assertThat(model.getStationQueueStrategy(sk)).isEqualTo(expectedStationStrategy);
    }

    private static Stream<Arguments> namedPutStrategyMappingProvider() {
        return Stream.of(
                Arguments.of("HeadStrategy", CommonConstants.QUEUE_STRATEGY_LCFS, CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE),
                Arguments.of("HeadStrategyPriority", CommonConstants.QUEUE_STRATEGY_LCFS, CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY),
                Arguments.of("RandStrategy", CommonConstants.QUEUE_STRATEGY_RAND, CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE),
                Arguments.of("RandStrategyPriority", CommonConstants.QUEUE_STRATEGY_RAND, CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY),
                Arguments.of("SJFStrategy", CommonConstants.QUEUE_STRATEGY_SJF, CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE),
                Arguments.of("SJFStrategyPriority", CommonConstants.QUEUE_STRATEGY_SJF, CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY),
                Arguments.of("LJFStrategy", CommonConstants.QUEUE_STRATEGY_LJF, CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE),
                Arguments.of("LJFStrategyPriority", CommonConstants.QUEUE_STRATEGY_LJF, CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY),
                Arguments.of("SEPTStrategy", CommonConstants.QUEUE_STRATEGY_SEPT, CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE),
                Arguments.of("SEPTStrategyPriority", CommonConstants.QUEUE_STRATEGY_SEPT, CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY),
                Arguments.of("LEPTStrategy", CommonConstants.QUEUE_STRATEGY_LEPT, CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE),
                Arguments.of("LEPTStrategyPriority", CommonConstants.QUEUE_STRATEGY_LEPT, CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY),
                Arguments.of("FCFSPRStrategyPriority", CommonConstants.QUEUE_STRATEGY_FCFS_PR, CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY),
                Arguments.of("LCFSPRStrategy", CommonConstants.QUEUE_STRATEGY_LCFS_PR, CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE),
                Arguments.of("LCFSPRStrategyPriority", CommonConstants.QUEUE_STRATEGY_LCFS_PR, CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY),
                Arguments.of("SRPTStrategy", CommonConstants.QUEUE_STRATEGY_SRPT, CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE),
                Arguments.of("SRPTStrategyPriority", CommonConstants.QUEUE_STRATEGY_SRPT, CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY),
                Arguments.of("EDDStrategy", CommonConstants.QUEUE_STRATEGY_EDD, CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE),
                Arguments.of("EDDStrategyPriority", CommonConstants.QUEUE_STRATEGY_EDD, CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY),
                Arguments.of("EDFStrategy", CommonConstants.QUEUE_STRATEGY_EDF, CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE),
                Arguments.of("EDFStrategyPriority", CommonConstants.QUEUE_STRATEGY_EDF, CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY),
                Arguments.of("TBSStrategyPriority", CommonConstants.QUEUE_STRATEGY_TBS, CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY)
        );
    }

    @ParameterizedTest(name = "Place {0} → queueStrategy={1}")
    @MethodSource("placePutStrategyMappingProvider")
    void queue_place_namedPutStrategy_mapsCorrectly(
            String strategyName, String expectedQueueStrategy) throws Exception {
        XMLReader.parseXML(rootOf(placeWithNamedPutStrategy(strategyName, "C1")), model);
        assertThat(model.getQueueStrategy(model.getStationByName("P"), model.getClassByName("C1")))
                .isEqualTo(expectedQueueStrategy);
    }

    private static Stream<Arguments> placePutStrategyMappingProvider() {
        return Stream.of(
                Arguments.of("HeadStrategy", CommonConstants.QUEUE_STRATEGY_LCFS),
                Arguments.of("RandStrategy", CommonConstants.QUEUE_STRATEGY_RAND)
        );
    }
// =========================================================================
// 21. IMPATIENCE
// =========================================================================

    @Test
    void impatience_reneging_typeAndParameterAreSet() throws Exception {
        String renegingInner =
                "<subParameter classPath=\"jmt.engine.random.Exponential\" name=\"Exponential\"/>" +
                        "<subParameter classPath=\"jmt.engine.random.ExponentialPar\" name=\"distrPar\">" +
                        "  <subParameter name=\"lambda\"><value>1.0</value></subParameter>" +
                        "</subParameter>";
        XMLReader.parseXML(rootOf(serverWithImpatience("C1", "Reneging", renegingInner)), model);

        Object sk = model.getStationByName("S");
        Object ck = model.getClassByName("C1");
        assertThat(model.getImpatienceType(sk, ck)).isEqualTo(ImpatienceType.RENEGING);
        assertThat(model.getImpatienceParameter(sk, ck)).isInstanceOf(RenegingParameter.class);
    }

    @Test
    void impatience_none_typeIsNone() throws Exception {
        XMLReader.parseXML(rootOf(serverWithImpatience("C1", "None", "")), model);
        assertThat(model.getImpatienceType(model.getStationByName("S"), model.getClassByName("C1")))
                .isEqualTo(ImpatienceType.NONE);
    }

// =========================================================================
// 22. ROUTING ADVANCED
// =========================================================================

    @Test
    void routing_powerOfK_parametersAreReadCorrectly() throws Exception {
        XMLReader.parseXML(rootOf(serverWithPowerOfKRouting("C1", "Sink", 3, true)), model);

        PowerOfKRouting rs = (PowerOfKRouting) model.getRoutingStrategy(
                model.getStationByName("S"), model.getClassByName("C1"));
        assertThat(rs).isNotNull();
        assertThat(rs.getK()).isEqualTo(3);
        assertThat(rs.isWithMemory()).isTrue();
    }

// =========================================================================
// 23. PS SERVER
// =========================================================================

    @Test
    void server_psServer_setsQueueStrategies() throws Exception {
        XMLReader.parseXML(rootOf(psServerXml("C1")), model);

        Object sk = model.getStationByName("S");
        assertThat(model.getStationQueueStrategy(sk)).isEqualTo(CommonConstants.STATION_QUEUE_STRATEGY_PSSERVER);
        assertThat(model.getQueueStrategy(sk, model.getClassByName("C1"))).isEqualTo(CommonConstants.QUEUE_STRATEGY_PS);
    }

    @ParameterizedTest(name = "{0} → queueStrategy={1}")
    @MethodSource("psStrategyMappingProvider")
    void server_psStrategy_mapsCorrectly(String psStrategyClassPath, String expectedQueueStrategy) throws Exception {
        XMLReader.parseXML(rootOf(psServerWithPsStrategy("C1", psStrategyClassPath)), model);
        assertThat(model.getQueueStrategy(model.getStationByName("S"), model.getClassByName("C1")))
                .isEqualTo(expectedQueueStrategy);
    }

    private static Stream<Arguments> psStrategyMappingProvider() {
        return Stream.of(
                Arguments.of("jmt.engine.NetStrategies.PSStrategies.EPSStrategy", CommonConstants.QUEUE_STRATEGY_PS),
                Arguments.of("jmt.engine.NetStrategies.PSStrategies.GPSStrategy", CommonConstants.QUEUE_STRATEGY_GPS),
                Arguments.of("jmt.engine.NetStrategies.PSStrategies.DPSStrategy", CommonConstants.QUEUE_STRATEGY_DPS),
                Arguments.of("jmt.engine.NetStrategies.PSStrategies.QBPSStrategy", CommonConstants.QUEUE_STRATEGY_QBPS)
        );
    }

    @Test
    void server_serviceWeights_weightIsSet() throws Exception {
        XMLReader.parseXML(rootOf(psServerWithServiceWeights("C1", 2.5)), model);
        assertThat((Double) model.getServiceWeight(model.getStationByName("S"), model.getClassByName("C1")))
                .isEqualTo(2.5, within(1e-9));
    }

    @Test
    void server_psPriorityUsed_setsQueueStrategyPriority() throws Exception {
        XMLReader.parseXML(rootOf(psServerWithPriorityUsed("C1", true)), model);
        assertThat(model.getStationQueueStrategy(model.getStationByName("S")))
                .isEqualTo(CommonConstants.STATION_QUEUE_STRATEGY_PSSERVER_PRIORITY);
    }
// =========================================================================
// 24. COMBFORK
// =========================================================================

    @Test
    void fork_combFork_strategyIsSet() throws Exception {

        String stringXML = forkWithCombFork("Fork", "Dest", "C1", 2);

        Element xml = rootOf(stringXML);


        XMLReader.parseXML(xml, model);

        Object fs = model.getForkStrategy(model.getStationByName("Fork"), model.getClassByName("C1"));
        assertThat(fs)
                .as("XMLReader doveva leggere la struttura comb fork e impostare la strategia sul modello")
                .isInstanceOf(CombFork.class);
    }

// =========================================================================
// 25. POLLING SERVER
// =========================================================================

    @ParameterizedTest(name = "{0} → pollingType={1}")
    @MethodSource("pollingServerTypeProvider")
    void server_pollingServer_setsCorrectPollingType(
            String serverClassName, String expectedPollingType) throws Exception {
        XMLReader.parseXML(rootOf(pollingServerXml("C1", serverClassName)), model);

        Object sk = model.getStationByName("S");
        assertThat(model.getStationQueueStrategy(sk)).isEqualTo(CommonConstants.STATION_QUEUE_STRATEGY_POLLING);
        assertThat(model.getStationPollingServerType(sk)).isEqualTo(expectedPollingType);
    }

    private static Stream<Arguments> pollingServerTypeProvider() {
        return Stream.of(
                Arguments.of("LimitedPollingServer", CommonConstants.STATION_QUEUE_STRATEGY_POLLING_LIMITED),
                Arguments.of("GatedPollingServer", CommonConstants.STATION_QUEUE_STRATEGY_POLLING_GATED),
                Arguments.of("ExhaustivePollingServer", CommonConstants.STATION_QUEUE_STRATEGY_POLLING_EXHAUSTIVE)
        );
    }

// =========================================================================
// 26. EMPTY SERVICE STRATEGY
// =========================================================================

    @Test
    void parseServiceStrategy_emptyDistribution_returnsNull() throws Exception {
        XMLReader.parseXML(rootOf(serverWithEmptyServiceStrategy("C1")), model);
        assertThat(model.getServiceTimeDistribution(
                model.getStationByName("S"), model.getClassByName("C1"))).isNull();
    }

// =========================================================================
// 27. FILE LOADING
// =========================================================================

    @Test
    void loadModel_fromPhysicalFile_executesWithoutCrashing() throws Exception {
        java.io.File temp = java.io.File.createTempFile("jmt_test", ".jsim");
        temp.deleteOnExit();
        java.nio.file.Files.write(temp.toPath(), (
                "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"no\"?>\n" +
                        "<sim name=\"TestModel\" maxTime=\"10.0\">\n" +
                        "  <userClass name=\"C1\" type=\"closed\" priority=\"0\" referenceSource=\"S\"/>\n" +
                        "  <node name=\"S\"><section className=\"JobSink\"/></node>\n" +
                        "</sim>").getBytes());

        CommonModel tempModel = new CommonModel();
        XMLReader.loadModel(temp, tempModel);
        assertThat(tempModel).isNotNull();
    }

// =========================================================================
// 28. ADVANCED SERVICE & DROP STRATEGIES
// =========================================================================

    @Test
    void serviceStrategy_zeroAndDisabled_areParsedCorrectly() throws Exception {
        String zeroPath = jmt.gui.common.serviceStrategies.ZeroStrategy.getEngineClassPath();
        String disabledPath = jmt.gui.common.serviceStrategies.DisabledStrategy.getEngineClassPath();

        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <userClass name=\"C2\" type=\"closed\"/>" +
                        "  <node name=\"S\">" +
                        "    <section className=\"Queue\"><parameter name=\"size\"><value>-1</value></parameter></section>" +
                        "    <section className=\"Server\">" +
                        "      <parameter name=\"maxJobs\"><value>1</value></parameter>" +
                        "      <parameter name=\"maxRunning\"><value>-1</value></parameter>" +
                        "      <parameter classPath=\"jmt.engine.NetStrategies.ServiceStrategy\">" +
                        "        <refClass>C1</refClass><subParameter classPath=\"" + zeroPath + "\"/>" +
                        "        <refClass>C2</refClass><subParameter classPath=\"" + disabledPath + "\"/>" +
                        "      </parameter>" +
                        "    </section>" +
                        "    <section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>" +
                        "  </node>" +
                        "</sim>"), model);

        Object sk = model.getStationByName("S");
        assertThat(model.getServiceTimeDistribution(sk, model.getClassByName("C1")))
                .isInstanceOf(jmt.gui.common.serviceStrategies.ZeroStrategy.class);
        assertThat(model.getServiceTimeDistribution(sk, model.getClassByName("C2")))
                .isInstanceOf(jmt.gui.common.serviceStrategies.DisabledStrategy.class);
    }

    @Test
    void queue_dropRule_retrial_setsRetrialDistribution() throws Exception {
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <node name=\"S\">" +
                        "    <section className=\"Queue\">" +
                        "      <parameter name=\"size\"><value>-1</value></parameter>" +
                        "      <parameter name=\"dropStrategies\">" +
                        "        <refClass>C1</refClass><subParameter><value>retrial</value></subParameter>" +
                        "      </parameter>" +
                        "      <parameter name=\"retrialDistributions\">" +
                        "        <refClass>C1</refClass>" +
                        "        <subParameter classPath=\"jmt.engine.random.DistributionContainer\">" +
                        "          <subParameter classPath=\"jmt.engine.random.Exponential\" name=\"Exponential\"/>" +
                        "          <subParameter classPath=\"jmt.engine.random.ExponentialPar\" name=\"distrPar\"/>" +
                        "        </subParameter>" +
                        "      </parameter>" +
                        "    </section>" +
                        "    <section className=\"Server\">" +
                        "      <parameter name=\"maxJobs\"><value>1</value></parameter>" +
                        "      <parameter name=\"maxRunning\"><value>-1</value></parameter>" +
                        "    </section>" +
                        "    <section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>" +
                        "  </node>" +
                        "</sim>"), model);

        Object sk = model.getStationByName("S");
        Object ck = model.getClassByName("C1");
        assertThat(model.getDropRule(sk, ck)).isEqualTo(CommonConstants.FINITE_RETRIAL);
        assertThat(model.getRetrialDistribution(sk, ck)).isNotNull();
    }

    @Test
    void impatience_balking_isParsedCorrectly() throws Exception {
        String ldEnginePath = LDStrategy.getEngineClassPath();
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <node name=\"S\">" +
                        "    <section className=\"Queue\">" +
                        "      <parameter name=\"size\"><value>-1</value></parameter>" +
                        "      <parameter name=\"Impatience\" array=\"true\">" +
                        "        <refClass>C1</refClass>" +
                        "        <subParameter classPath=\"\" name=\"balking\">" +
                        "          <subParameter/>" +
                        "          <subParameter classPath=\"" + ldEnginePath + "\">" +
                        "            <subParameter name=\"LDParameterArray\">" +
                        "              <subParameter>" +
                        "                <subParameter name=\"from\"><value>1</value></subParameter>" +
                        "                <subParameter classPath=\"jmt.engine.random.Exponential\" name=\"Exponential\"/>" +
                        "                <subParameter classPath=\"jmt.engine.random.ExponentialPar\" name=\"distrPar\"/>" +
                        "                <subParameter name=\"mean\"><value>2.5</value></subParameter>" +
                        "              </subParameter>" +
                        "            </subParameter>" +
                        "          </subParameter>" +
                        "          <subParameter/>" +
                        "          <subParameter name=\"priorityActivated\"><value>true</value></subParameter>" +
                        "        </subParameter>" +
                        "      </parameter>" +
                        "    </section>" +
                        "    <section className=\"Server\">" +
                        "      <parameter name=\"maxJobs\"><value>1</value></parameter>" +
                        "      <parameter name=\"maxRunning\"><value>-1</value></parameter>" +
                        "    </section>" +
                        "    <section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>" +
                        "  </node>" +
                        "</sim>"), model);

        Object sk = model.getStationByName("S");
        Object ck = model.getClassByName("C1");
        assertThat(model.getImpatienceType(sk, ck)).isEqualTo(ImpatienceType.BALKING);
        assertThat(model.getImpatienceParameter(sk, ck))
                .isInstanceOf(jmt.engine.NetStrategies.ImpatienceStrategies.BalkingParameter.class);
    }// =========================================================================
// 29. CLASS SWITCH FORK
// =========================================================================

    @Test
    void fork_classSwitchFork_isParsed() throws Exception {
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <userClass name=\"C2\" type=\"closed\"/>" +
                        "  <node name=\"Dest\">" +
                        "    <section className=\"Queue\"/>" +
                        "    <section className=\"Delay\"><parameter name=\"ServiceStrategy\"/></section>" +
                        "    <section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>" +
                        "  </node>" +
                        "  <node name=\"F\">" +
                        "    <section className=\"Queue\"/>" +
                        "    <section className=\"ServiceTunnel\"/>" +
                        "    <section className=\"Fork\">" +
                        "      <parameter name=\"jobsPerLink\"><value>1</value></parameter>" +
                        "      <parameter name=\"block\"><value>-1</value></parameter>" +
                        "      <parameter name=\"isSimplifiedFork\"><value>true</value></parameter>" +
                        "      <parameter name=\"ForkStrategy\">" +
                        "        <refClass>C1</refClass>" +
                        "        <subParameter classPath=\"jmt.engine.NetStrategies.ForkStrategies.ClassSwitchFork\">" +
                        "          <subParameter name=\"EntryArray\">" +
                        "            <subParameter>" +
                        "              <subParameter><value>Dest</value></subParameter>" +
                        "              <subParameter><subParameter><value>C2</value></subParameter></subParameter>" +
                        "              <subParameter><subParameter><value>2</value></subParameter></subParameter>" +
                        "            </subParameter>" +
                        "          </subParameter>" +
                        "        </subParameter>" +
                        "      </parameter>" +
                        "    </section>" +
                        "  </node>" +
                        "</sim>"), model);

        Object fs = model.getForkStrategy(model.getStationByName("F"), model.getClassByName("C1"));
        assertThat(fs)
                .as("Il ClassSwitchFork deve essere interpretato e assegnato alla classe")
                .isNotNull();
    }

// =========================================================================
// 30. PETRI NET — ENABLING CONDITIONS
// =========================================================================

    @Test
    void transition_enablingConditions_populatesConditionMap() throws Exception {
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <node name=\"Place1\">" +
                        "    <section className=\"Storage\"/>" +
                        "    <section className=\"ServiceTunnel\"/>" +
                        "    <section className=\"Linkage\"/>" +
                        "  </node>" +
                        "  <node name=\"T\">" +
                        "    <section className=\"Enabling\">" +
                        "      <parameter name=\"enablingConditions\">" +
                        "        <subParameter>" +
                        "          <subParameter>" +
                        "            <subParameter>" +
                        "              <subParameter name=\"stationName\"><value>Place1</value></subParameter>" +
                        "              <subParameter name=\"entries\">" +
                        "                <refClass>C1</refClass><subParameter><value>2</value></subParameter>" +
                        "              </subParameter>" +
                        "            </subParameter>" +
                        "          </subParameter>" +
                        "        </subParameter>" +
                        "      </parameter>" +
                        "    </section>" +
                        "    <section className=\"Timing\">" +
                        "      <parameter name=\"modeNames\"><subParameter><value>Mode1</value></subParameter></parameter>" +
                        "    </section>" +
                        "    <section className=\"Firing\"/>" +
                        "  </node>" +
                        "</sim>"), model);

        Object transitionKey = model.getStationByName("T");
        Object placeKey = model.getStationByName("Place1");
        Object classKey = model.getClassByName("C1");
        assertThat(model.getEnablingCondition(transitionKey, 0, placeKey, classKey))
                .as("Le enabling conditions devono essere state propagate nel modello")
                .isEqualTo(2);
    }

    @Test
    void queue_extractTransitionConditions_populatesModelForAllConditionTypes() throws Exception {
        XMLReader.parseXML(rootOf(
                "<sim>" +
                        "  <userClass name=\"C1\" type=\"closed\"/>" +
                        "  <node name=\"Place1\">" +
                        "    <section className=\"Storage\"/>" +
                        "    <section className=\"ServiceTunnel\"/>" +
                        "    <section className=\"Linkage\"/>" +
                        "  </node>" +
                        "  <node name=\"S\">" +
                        "    <section className=\"Queue\">" +
                        "      <parameter name=\"enablingConditions\">" +
                        "        <subParameter>" +
                        "          <subParameter>" +
                        "            <subParameter>" +
                        "              <subParameter name=\"stationName\"><value>Place1</value></subParameter>" +
                        "              <subParameter name=\"entries\">" +
                        "                <refClass>C1</refClass><subParameter><value>2</value></subParameter>" +
                        "              </subParameter>" +
                        "            </subParameter>" +
                        "          </subParameter>" +
                        "        </subParameter>" +
                        "      </parameter>" +
                        "      <parameter name=\"inhibitingConditions\">" +
                        "        <subParameter>" +
                        "          <subParameter>" +
                        "            <subParameter>" +
                        "              <subParameter name=\"stationName\"><value>Place1</value></subParameter>" +
                        "              <subParameter name=\"entries\">" +
                        "                <refClass>C1</refClass><subParameter><value>3</value></subParameter>" +
                        "              </subParameter>" +
                        "            </subParameter>" +
                        "          </subParameter>" +
                        "        </subParameter>" +
                        "      </parameter>" +
                        "      <parameter name=\"resourceConditions\">" +
                        "        <subParameter>" +
                        "          <subParameter>" +
                        "            <subParameter>" +
                        "              <subParameter name=\"stationName\"><value>Place1</value></subParameter>" +
                        "              <subParameter name=\"entries\">" +
                        "                <refClass>C1</refClass><subParameter><value>4</value></subParameter>" +
                        "              </subParameter>" +
                        "            </subParameter>" +
                        "          </subParameter>" +
                        "        </subParameter>" +
                        "      </parameter>" +
                        "    </section>" +
                        "    <section className=\"Server\">" +
                        "      <parameter name=\"maxJobs\"><value>1</value></parameter>" +
                        "      <parameter name=\"maxRunning\"><value>-1</value></parameter>" +
                        "    </section>" +
                        "    <section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>" +
                        "  </node>" +
                        "</sim>"), model);

        Object stationKey = model.getStationByName("S");
        Object placeKey = model.getStationByName("Place1");
        Object classKey = model.getClassByName("C1");

        assertThat(model.getEnablingCondition(stationKey, 0, placeKey, classKey)).isEqualTo(2);
        assertThat(model.getInhibitingCondition(stationKey, 0, placeKey, classKey)).isEqualTo(3);
        assertThat(model.getResourceCondition(stationKey, 0, placeKey, classKey)).isEqualTo(4);
    }


    // =========================================================================
    // XML builder helpers (sections 20-26)
    // =========================================================================

    /**
     * Helper: the two-child ServiceTimeStrategy subParameter block for a class.
     */
    private static String expServiceStrategy(String className) {
        return "<subParameter name=\"" + className + "\"" +
                "              classPath=\"jmt.engine.NetStrategies.ServiceStrategies.ServiceTimeStrategy\">" +
                "  <subParameter classPath=\"jmt.engine.random.Exponential\" name=\"Exponential\"/>" +
                "  <subParameter classPath=\"jmt.engine.random.ExponentialPar\" name=\"distrPar\">" +
                "    <subParameter name=\"lambda\"><value>1.0</value></subParameter>" +
                "  </subParameter>" +
                "</subParameter>";
    }

    /**
     * Helper: Queue section with TailStrategy for one class.
     */
    private static String tailQueueSection(String className) {
        return "<section className=\"Queue\">" +
                "  <parameter name=\"size\"><value>-1</value></parameter>" +
                "  <parameter classPath=\"jmt.engine.NetStrategies.QueuePutStrategy\">" +
                "    <refClass>" + className + "</refClass>" +
                "    <subParameter classPath=\"jmt.engine.NetStrategies.QueuePutStrategies.TailStrategy\"/>" +
                "  </parameter>" +
                "</section>";
    }

    /**
     * Helper: Queue section with a named put strategy for one class.
     */
    private String namedPutQueueSection(String strategyName, String className) {
        return "<section className=\"Queue\">" +
                "  <parameter name=\"size\"><value>-1</value></parameter>" +
                "  <parameter classPath=\"jmt.engine.NetStrategies.QueuePutStrategy\">" +
                "    <refClass>" + className + "</refClass>" +
                "    <subParameter classPath=\"jmt.engine.NetStrategies.QueuePutStrategies." + strategyName + "\"/>" +
                "  </parameter>" +
                "</section>";
    }

    /**
     * Helper: Server section with standard Exponential service for one class.
     */
    private String expServerSection(String className) {
        return "<section className=\"Server\">" +
                "  <parameter name=\"maxJobs\"><value>1</value></parameter>" +
                "  <parameter name=\"maxRunning\"><value>-1</value></parameter>" +
                "  <parameter classPath=\"jmt.engine.NetStrategies.ServiceStrategy\">" +
                "    <refClass>" + className + "</refClass>" +
                "    " + expServiceStrategy(className) +
                "  </parameter>" +
                "</section>";
    }

    /**
     * Helper: PSServer section with standard Exponential service for one class.
     */
    private String expPSServerSection(String className) {
        return "<section className=\"PSServer\">" +
                "  <parameter name=\"maxJobs\"><value>1</value></parameter>" +
                "  <parameter name=\"maxRunning\"><value>-1</value></parameter>" +
                "  <parameter classPath=\"jmt.engine.NetStrategies.ServiceStrategy\">" +
                "    <refClass>" + className + "</refClass>" +
                "    " + expServiceStrategy(className) +
                "  </parameter>" +
                "</section>";
    }

    /**
     * Helper: minimal Router section.
     */
    private static String minimalRouterSection() {
        return "<section className=\"Router\"><parameter name=\"RoutingStrategy\"/></section>";
    }

    /**
     * Server station with a named put strategy — for queue strategy mapping tests.
     */
    private String serverWithNamedPutStrategy(String strategyName, String className) {
        return "<sim>" +
                "  <userClass name=\"" + className + "\" type=\"closed\"/>" +
                "  <node name=\"S\">" +
                namedPutQueueSection(strategyName, className) +
                expServerSection(className) +
                minimalRouterSection() +
                "  </node>" +
                "</sim>";
    }

    /**
     * Place (Storage+Tunnel+Linkage) with a named put strategy for one class.
     */
    private String placeWithNamedPutStrategy(String strategyName, String className) {
        return "<sim>" +
                "  <userClass name=\"" + className + "\" type=\"closed\"/>" +
                "  <node name=\"P\">" +
                "    <section className=\"Storage\">" +
                "      <parameter name=\"putStrategies\"" +
                "                 classPath=\"jmt.engine.NetStrategies.QueuePutStrategy\"" +
                "                 array=\"true\">" +
                "        <refClass>" + className + "</refClass>" +
                "        <subParameter classPath=\"jmt.engine.NetStrategies.QueuePutStrategies." + strategyName + "\"/>" +
                "      </parameter>" +
                "    </section>" +
                "    <section className=\"ServiceTunnel\"/>" +
                "    <section className=\"Linkage\"><parameter name=\"RoutingStrategy\"/></section>" +
                "  </node>" +
                "</sim>";
    }

    /**
     * Server station with an Impatience block for the given class.
     *
     * @param impatienceName the impatience type name (e.g. "Reneging", "None")
     * @param innerXml       XML children inside the impatience subParameter
     */
    private String serverWithImpatience(String className, String impatienceName, String innerXml) {
        return "<sim>" +
                "  <userClass name=\"" + className + "\" type=\"closed\"/>" +
                "  <node name=\"S\">" +
                "    <section className=\"Queue\">" +
                "      <parameter name=\"size\"><value>-1</value></parameter>" +
                "      <parameter classPath=\"jmt.engine.NetStrategies.QueuePutStrategy\">" +
                "        <refClass>" + className + "</refClass>" +
                "        <subParameter classPath=\"jmt.engine.NetStrategies.QueuePutStrategies.TailStrategy\"/>" +
                "      </parameter>" +
                "      " +
                "      <parameter name=\"Impatience\" array=\"true\">" +
                "        <refClass>" + className + "</refClass>" +
                "        <subParameter classPath=\"\" name=\"" + impatienceName + "\">" +
                "          " + innerXml +
                "        </subParameter>" +
                "      </parameter>" +
                "    </section>" +
                expServerSection(className) +
                minimalRouterSection() +
                "  </node>" +
                "</sim>";
    }

    /**
     * Server with PowerOfKRouting for the given class.
     */
    private String serverWithPowerOfKRouting(String className, String destName, int k, boolean withMemory) {
        // FIX: Iniettiamo la stringa esatta dall'oggetto GUI per evitare il ClassCastException
        String enginePath = new PowerOfKRouting().getClassPath();
        return "<sim>" +
                "  <userClass name=\"" + className + "\" type=\"closed\"/>" +
                "  <node name=\"S\">" +
                tailQueueSection(className) +
                expServerSection(className) +
                "    <section className=\"Router\">" +
                "      <parameter name=\"RoutingStrategy\">" +
                "        <refClass>" + className + "</refClass>" +
                "        <subParameter classPath=\"" + enginePath + "\"" +
                "                      name=\"" + className + "\">" +
                "          <subParameter name=\"k\"><value>" + k + "</value></subParameter>" +
                "          <subParameter name=\"withMemory\"><value>" + withMemory + "</value></subParameter>" +
                "        </subParameter>" +
                "      </parameter>" +
                "    </section>" +
                "  </node>" +
                "  " +
                "  <node name=\"" + destName + "\">" + minimalDelaySections() + "</node>" +
                "</sim>";
    }

    /**
     * Minimal PS server XML.
     */
    private String psServerXml(String className) {
        return "<sim>" +
                "  <userClass name=\"" + className + "\" type=\"closed\"/>" +
                "  <node name=\"S\">" +
                tailQueueSection(className) +
                expPSServerSection(className) +
                minimalRouterSection() +
                "  </node>" +
                "</sim>";
    }

    /**
     * PS server with a specific PS strategy for the given class.
     */
    private static String psServerWithPsStrategy(String className, String psStrategyClassPath) {
        return "<sim>" +
                "  <userClass name=\"" + className + "\" type=\"closed\"/>" +
                "  <node name=\"S\">" +
                tailQueueSection(className) +
                "    <section className=\"PSServer\">" +
                "      <parameter name=\"maxJobs\"><value>1</value></parameter>" +
                "      <parameter name=\"maxRunning\"><value>-1</value></parameter>" +
                "      <parameter classPath=\"jmt.engine.NetStrategies.ServiceStrategy\">" +
                "        <refClass>" + className + "</refClass>" +
                "        " + expServiceStrategy(className) +
                "      </parameter>" +
                "      <parameter name=\"psStrategy\"" +
                "                 classPath=\"jmt.engine.NetStrategies.PSStrategy\"" +
                "                 array=\"true\">" +
                "        <refClass>" + className + "</refClass>" +
                "        <subParameter classPath=\"" + psStrategyClassPath + "\"/>" +
                "      </parameter>" +
                "    </section>" +
                minimalRouterSection() +
                "  </node>" +
                "</sim>";
    }

    /**
     * PS server with serviceWeights for the given class and weight.
     */
    private String psServerWithServiceWeights(String className, double weight) {
        return "<sim>" +
                "  <userClass name=\"" + className + "\" type=\"closed\"/>" +
                "  <node name=\"S\">" +
                tailQueueSection(className) +
                "    <section className=\"PSServer\">" +
                "      <parameter name=\"maxJobs\"><value>1</value></parameter>" +
                "      <parameter name=\"maxRunning\"><value>-1</value></parameter>" +
                "      <parameter classPath=\"jmt.engine.NetStrategies.ServiceStrategy\">" +
                "        <refClass>" + className + "</refClass>" +
                "        " + expServiceStrategy(className) +
                "      </parameter>" +
                "      <parameter name=\"serviceWeights\" classPath=\"\" array=\"true\">" +
                "        <refClass>" + className + "</refClass>" +
                "        <subParameter classPath=\"\">" +
                "          <value>" + weight + "</value>" +
                "        </subParameter>" +
                "      </parameter>" +
                "    </section>" +
                minimalRouterSection() +
                "  </node>" +
                "</sim>";
    }

    /**
     * PS server with PSPriorityUsed parameter.
     */
    private String psServerWithPriorityUsed(String className, boolean priorityUsed) {
        return "<sim>" +
                "  <userClass name=\"" + className + "\" type=\"closed\"/>" +
                "  <node name=\"S\">" +
                tailQueueSection(className) +
                "    <section className=\"PSServer\">" +
                "      <parameter name=\"maxJobs\"><value>1</value></parameter>" +
                "      <parameter name=\"maxRunning\"><value>-1</value></parameter>" +
                "      <parameter classPath=\"jmt.engine.NetStrategies.ServiceStrategy\">" +
                "        <refClass>" + className + "</refClass>" +
                "        " + expServiceStrategy(className) +
                "      </parameter>" +
                "      <parameter name=\"PSPriorityUsed\" classPath=\"\">" +
                "        <value>" + priorityUsed + "</value>" +
                "      </parameter>" +
                "    </section>" +
                minimalRouterSection() +
                "  </node>" +
                "</sim>";
    }

    /**
     * Polling server XML with the correct polling section classname.
     */
    private String pollingServerXml(String className, String pollingClassName) {
        return "<sim>" +
                "  <userClass name=\"" + className + "\" type=\"closed\"/>" +
                "  <node name=\"S\">" +
                tailQueueSection(className) +
                "    <section className=\"" + pollingClassName + "\">" +
                "      <parameter name=\"maxJobs\"><value>1</value></parameter>" +
                "      <parameter name=\"maxRunning\"><value>-1</value></parameter>" +
                "      <parameter classPath=\"jmt.engine.NetStrategies.ServiceStrategy\">" +
                "        <refClass>" + className + "</refClass>" +
                "        " + expServiceStrategy(className) +
                "      </parameter>" +
                "    </section>" +
                minimalRouterSection() +
                "  </node>" +
                "</sim>";
    }

    /**
     * Fork station XML with CombFork strategy.
     */
    private String forkWithCombFork(String forkName, String destName, String className, int jobsPerLink) {
        // Usiamo la stringa hardcoded dell'engine per evitare ClassCast o mancate inizializzazioni
        String enginePath = "jmt.engine.NetStrategies.ForkStrategies.CombFork";
        return "<sim>" +
                "  <userClass name=\"" + className + "\" type=\"closed\"/>" +
                "  <node name=\"" + destName + "\">" + minimalDelaySections() + "</node>" +
                "  <node name=\"" + forkName + "\">" +
                "    <section className=\"Queue\">" +
                "      <parameter name=\"size\"><value>-1</value></parameter>" +
                "    </section>" +
                "    <section className=\"ServiceTunnel\"/>" +
                "    <section className=\"Fork\">" +
                "      <parameter name=\"jobsPerLink\"><value>" + jobsPerLink + "</value></parameter>" +
                "      <parameter name=\"block\"><value>-1</value></parameter>" +
                "      <parameter name=\"isSimplifiedFork\"><value>true</value></parameter>" +
                // Rimosso array="true" e classPath dal parent per matchare l'if interno di XMLReader
                "      <parameter name=\"ForkStrategy\">" +
                "        <refClass>" + className + "</refClass>" +
                "        <subParameter classPath=\"" + enginePath + "\">" +
                "          <subParameter name=\"EntryArray\">" +
                "            <subParameter>" +
                "              <subParameter><value>2</value></subParameter>" +
                "              <subParameter><value>1.0</value></subParameter>" +
                "            </subParameter>" +
                "          </subParameter>" +
                "        </subParameter>" +
                "      </parameter>" +
                "    </section>" +
                "  </node>" +
                "</sim>";
    }

    /**
     * Server with a ServiceStrategy that has no subParameters → parseServiceStrategy returns null.
     */
    private String serverWithEmptyServiceStrategy(String className) {
        return "<sim>" +
                "  <userClass name=\"" + className + "\" type=\"closed\"/>" +
                "  <node name=\"S\">" +
                tailQueueSection(className) +
                "    <section className=\"Server\">" +
                "      <parameter name=\"maxJobs\"><value>1</value></parameter>" +
                "      <parameter name=\"maxRunning\"><value>-1</value></parameter>" +
                "      <parameter classPath=\"jmt.engine.NetStrategies.ServiceStrategy\">" +
                "        <refClass>" + className + "</refClass>" +
                "        <subParameter name=\"" + className + "\"" +
                "                      classPath=\"jmt.engine.NetStrategies.ServiceStrategies.ServiceTimeStrategy\">" +
                "        </subParameter>" +
                "      </parameter>" +
                "    </section>" +
                "    <section className=\"Router\">" +
                "      <parameter name=\"RoutingStrategy\">" +
                "        <refClass>" + className + "</refClass>" +
                "        <subParameter classPath=\"jmt.engine.NetStrategies.RoutingStrategies.RandomRouting\"" +
                "                      name=\"" + className + "\"/>" +
                "      </parameter>" +
                "    </section>" +
                "  </node>" +
                "</sim>";
    }
}

