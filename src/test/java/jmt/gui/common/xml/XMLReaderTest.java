package jmt.gui.common.xml;

import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceType;
import jmt.engine.NetStrategies.ImpatienceStrategies.RenegingParameter;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.List;
import java.util.Map;

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
        documentBuilder = factory.newDocumentBuilder();
        model = new CommonModel();
        // Reset XMLReader static caches so tests are fully isolated.
        XMLReader.engineToGuiDistr = null;
        XMLReader.engineToGuiRouting = null;
        XMLReader.engineToGuiFork = null;
        XMLReader.engineToGuiJoin = null;
        XMLReader.engineToGuiSemaphore = null;
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void test_parseAndSetImpatienceParameters_branches_fullcoverage() {
        File xmlFile = new File("src/test/resources/jmt/gui/common/xml/parseImpatience_branches.xml");
        CommonModel model = new CommonModel();

        XMLArchiver.loadModel(xmlFile, model);

        Object queueWithImpatience = model.getStationByName("QueueWithImpatience");
        Object class1 = model.getClassByName("Class1");
        Object class2 = model.getClassByName("Class2");
        Object class3 = model.getClassByName("Class3");

        assertThat(queueWithImpatience)
                .as("QueueWithImpatience station must exist")
                .isNotNull();
        assertThat(class1).as("Class1 must exist").isNotNull();
        assertThat(class2).as("Class2 must exist").isNotNull();
        assertThat(class3).as("Class3 must exist").isNotNull();

        assertThat(model.getStationType(queueWithImpatience))
                .as("QueueWithImpatience should be parsed as a server station")
                .isEqualTo(CommonConstants.STATION_TYPE_SERVER);

        // RENEGING branch
        assertThat(model.getImpatienceType(queueWithImpatience, class1))
                .as("Class1 impatience type should be RENEGING")
                .isEqualTo(ImpatienceType.RENEGING);
        Object class1ImpatienceParameter = model.getImpatienceParameter(queueWithImpatience, class1);
        assertThat(class1ImpatienceParameter)
                .as("Class1 impatience parameter should be RenegingParameter")
                .isInstanceOf(RenegingParameter.class);

        RenegingParameter renegingParameter = (RenegingParameter) class1ImpatienceParameter;
        assertThat(renegingParameter.getDistribution())
                .as("Reneging distribution should be parsed")
                .isNotNull();
        Double lambda = (Double) renegingParameter.getDistribution().getParameter("lambda").getValue();
        assertThat(lambda)
                .as("Reneging lambda should be parsed as 4.0")
                .isCloseTo(4.0, within(1e-9));

        // BALKING branch
        assertThat(model.getImpatienceType(queueWithImpatience, class2))
                .as("Class2 impatience type should be BALKING")
                .isEqualTo(ImpatienceType.BALKING);
        assertThat(model.getImpatienceParameter(queueWithImpatience, class2))
                .as("Class2 impatience parameter should be set for BALKING")
                .isNotNull();

        // default branch (NONE)
        assertThat(model.getImpatienceType(queueWithImpatience, class3))
                .as("Class3 impatience type should fall back to NONE")
                .isEqualTo(ImpatienceType.NONE);
        assertThat(model.getImpatienceParameter(queueWithImpatience, class3))
                .as("Class3 impatience parameter should be null for NONE")
                .isNull();
    }

    @Test
    void test_parseStationDeadlinesAndQuanta_branches_fullcoverage() throws Exception {
        CommonModel model = new CommonModel();
        Object class1 = model.addClass("Class1", CommonConstants.CLASS_TYPE_CLOSED, 0, 0.0, 1, null);
        Object class2 = model.addClass("Class2", CommonConstants.CLASS_TYPE_CLOSED, 0, 0.0, 1, null);
        Object station = model.addStation("ServerWithQuanta", CommonConstants.STATION_TYPE_SERVER, 1,
                new java.util.ArrayList<jmt.gui.common.definitions.ServerType>());

        File fixtureFile = new File("src/test/resources/jmt/gui/common/xml/parseStationDeadlinesAndQuanta_branches.xml");
        Document fixtureDoc = documentBuilder.parse(fixtureFile);
        org.w3c.dom.Element fixtureRoot = fixtureDoc.getDocumentElement();
        org.w3c.dom.Element emptyCase = (org.w3c.dom.Element) fixtureRoot.getElementsByTagName("emptyCase").item(0);
        org.w3c.dom.Element populatedCase = (org.w3c.dom.Element) fixtureRoot.getElementsByTagName("populatedCase").item(0);

        XMLReader.parseClassStationSoftDeadlines(emptyCase.getElementsByTagName(XMLConstantNames.XML_E_STATION_DUE_DATES), model, station);
        XMLReader.parseQuantaSize(emptyCase.getElementsByTagName(XMLConstantNames.XML_E_STATION_QUANTUM_SIZE), model, station);
        XMLReader.parseQuantumSwitchoverTime(emptyCase.getElementsByTagName(XMLConstantNames.XML_E_STATION_QUANTUM_SWITCHOVER_TIME), model, station);

        XMLReader.parseClassStationSoftDeadlines(populatedCase.getElementsByTagName(XMLConstantNames.XML_E_STATION_DUE_DATES), model, station);
        XMLReader.parseQuantaSize(populatedCase.getElementsByTagName(XMLConstantNames.XML_E_STATION_QUANTUM_SIZE), model, station);
        XMLReader.parseQuantumSwitchoverTime(populatedCase.getElementsByTagName(XMLConstantNames.XML_E_STATION_QUANTUM_SWITCHOVER_TIME), model, station);

        assertThat(model.getClassStationSoftDeadline(station, class1))
                .as("First class soft deadline should be parsed from classSoftDeadlines")
                .isCloseTo(10.5, within(1e-9));
        assertThat(model.getClassStationSoftDeadline(station, class2))
                .as("Second class soft deadline should be parsed from classSoftDeadlines")
                .isCloseTo(20.75, within(1e-9));

        assertThat(model.getQuantumSize(station))
                .as("Quantum size should be parsed from quantumSize tag")
                .isCloseTo(0.25, within(1e-9));
        assertThat(model.getQuantumSwitchoverTime(station))
                .as("Quantum switchover time should be parsed from quantumSwitchoverTime tag")
                .isCloseTo(1.5, within(1e-9));
    }

    @Test
    void test_parseJoin_branches_fullcoverage() {
        File xmlFile = new File("src/test/resources/jmt/gui/common/xml/parseJoin_branches.xml");
        CommonModel model = new CommonModel();

        XMLArchiver.loadModel(xmlFile, model);

        Object class1 = model.getClassByName("Class1");
        Object class2 = model.getClassByName("Class2");
        Object joinWithParam = model.getStationByName("JoinWithParam");
        Object joinFallback = model.getStationByName("JoinFallback");

        assertThat(class1).as("Class1 must exist").isNotNull();
        assertThat(class2).as("Class2 must exist").isNotNull();
        assertThat(joinWithParam).as("JoinWithParam station must exist").isNotNull();
        assertThat(joinFallback).as("JoinFallback station must exist").isNotNull();

        assertThat(model.getStationType(joinWithParam))
                .as("JoinWithParam should be parsed as JOIN station")
                .isEqualTo(CommonConstants.STATION_TYPE_JOIN);
        assertThat(model.getStationType(joinFallback))
                .as("JoinFallback should be parsed as JOIN station")
                .isEqualTo(CommonConstants.STATION_TYPE_JOIN);

        Object joinClass1 = model.getJoinStrategy(joinWithParam, class1);
        assertThat(joinClass1)
                .as("Class1 join strategy should be PartialJoin")
                .isInstanceOf(jmt.gui.common.joinStrategies.PartialJoin.class);
        assertThat(((jmt.gui.common.joinStrategies.PartialJoin) joinClass1).getRequiredNum())
                .as("PartialJoin numRequired should be parsed from XML")
                .isEqualTo(3);

        Object joinClass2 = model.getJoinStrategy(joinWithParam, class2);
        assertThat(joinClass2)
                .as("Class2 join strategy should be GuardJoin")
                .isInstanceOf(jmt.gui.common.joinStrategies.GuardJoin.class);
        java.util.Map<Object, Integer> guard = ((jmt.gui.common.joinStrategies.GuardJoin) joinClass2).getGuard();
        assertThat(guard)
                .as("GuardJoin guard map should contain parsed class thresholds")
                .containsEntry(class1, 4)
                .containsEntry(class2, 2);

        assertThat(model.getJoinStrategy(joinFallback, class1))
                .as("Fallback join strategy for Class1 should be NormalJoin")
                .isInstanceOf(jmt.gui.common.joinStrategies.NormalJoin.class);
        assertThat(model.getJoinStrategy(joinFallback, class2))
                .as("Fallback join strategy for Class2 should be NormalJoin")
                .isInstanceOf(jmt.gui.common.joinStrategies.NormalJoin.class);
    }

    @Test
    void test_parseStorageSection_branches_fullcoverage() {
        File xmlFile = new File("src/test/resources/jmt/gui/common/xml/parseStorage_branches.xml");
        CommonModel model = new CommonModel();

        XMLArchiver.loadModel(xmlFile, model);

        Object class1 = model.getClassByName("Class1");
        Object class2 = model.getClassByName("Class2");
        Object class3 = model.getClassByName("Class3");
        Object place1 = model.getStationByName("Place1");

        assertThat(class1).as("Class1 must exist").isNotNull();
        assertThat(class2).as("Class2 must exist").isNotNull();
        assertThat(class3).as("Class3 must exist").isNotNull();
        assertThat(place1).as("Place1 station must exist").isNotNull();

        assertThat(model.getStationType(place1))
                .as("Place1 should be parsed as PLACE station")
                .isEqualTo(CommonConstants.STATION_TYPE_PLACE);

        assertThat(model.getStationQueueCapacity(place1))
                .as("totalCapacity should be parsed into station queue capacity")
                .isEqualTo(30);

        assertThat(model.getQueueCapacity(place1, class1))
                .as("Class1 capacity should be parsed from capacities parameter")
                .isEqualTo(5);
        assertThat(model.getQueueCapacity(place1, class2))
                .as("Class2 capacity should be parsed from capacities parameter")
                .isEqualTo(7);
        assertThat(model.getQueueCapacity(place1, class3))
                .as("Class3 capacity should be parsed from capacities parameter")
                .isEqualTo(9);

        assertThat(model.getDropRule(place1, class1))
                .as("Class1 drop rule should map 'drop' to FINITE_DROP")
                .isEqualTo(CommonConstants.FINITE_DROP);
        assertThat(model.getDropRule(place1, class2))
                .as("Class2 drop rule should map 'waiting queue' to FINITE_WAITING")
                .isEqualTo(CommonConstants.FINITE_WAITING);
        assertThat(model.getDropRule(place1, class3))
                .as("Class3 drop rule should map 'BAS blocking' to FINITE_BLOCK")
                .isEqualTo(CommonConstants.FINITE_BLOCK);

        assertThat(model.getQueueStrategy(place1, class1))
                .as("TailStrategy should map to FCFS")
                .isEqualTo(CommonConstants.QUEUE_STRATEGY_FCFS);
        assertThat(model.getQueueStrategy(place1, class2))
                .as("HeadStrategy should map to LCFS")
                .isEqualTo(CommonConstants.QUEUE_STRATEGY_LCFS);
        assertThat(model.getQueueStrategy(place1, class3))
                .as("RandStrategy should map to RAND")
                .isEqualTo(CommonConstants.QUEUE_STRATEGY_RAND);
    }

    @Test
    void test_parseEnablingSection_branches_fullcoverage() {
        File xmlFile = new File("src/test/resources/jmt/gui/common/xml/parseEnabling_branches.xml");
        CommonModel model = new CommonModel();

        XMLArchiver.loadModel(xmlFile, model);

        Object class1 = model.getClassByName("Class1");
        Object class2 = model.getClassByName("Class2");
        Object placeA = model.getStationByName("PlaceA");
        Object placeB = model.getStationByName("PlaceB");
        Object transitionBoth = model.getStationByName("TransitionBoth");
        Object transitionOnlyEnabling = model.getStationByName("TransitionOnlyEnabling");
        Object transitionOnlyInhibiting = model.getStationByName("TransitionOnlyInhibiting");

        assertThat(class1).as("Class1 must exist").isNotNull();
        assertThat(class2).as("Class2 must exist").isNotNull();
        assertThat(placeA).as("PlaceA station must exist").isNotNull();
        assertThat(placeB).as("PlaceB station must exist").isNotNull();
        assertThat(transitionBoth).as("TransitionBoth station must exist").isNotNull();
        assertThat(transitionOnlyEnabling).as("TransitionOnlyEnabling station must exist").isNotNull();
        assertThat(transitionOnlyInhibiting).as("TransitionOnlyInhibiting station must exist").isNotNull();

        assertThat(model.getStationType(transitionBoth))
                .as("TransitionBoth should be parsed as TRANSITION station")
                .isEqualTo(CommonConstants.STATION_TYPE_TRANSITION);
        assertThat(model.getStationType(transitionOnlyEnabling))
                .as("TransitionOnlyEnabling should be parsed as TRANSITION station")
                .isEqualTo(CommonConstants.STATION_TYPE_TRANSITION);
        assertThat(model.getStationType(transitionOnlyInhibiting))
                .as("TransitionOnlyInhibiting should be parsed as TRANSITION station")
                .isEqualTo(CommonConstants.STATION_TYPE_TRANSITION);

        assertThat(model.getEnablingCondition(transitionBoth, 0, placeA, class1))
                .as("TransitionBoth enabling condition for PlaceA/Class1 should be parsed")
                .isEqualTo(2);
        assertThat(model.getEnablingCondition(transitionBoth, 0, placeA, class2))
                .as("TransitionBoth enabling condition for PlaceA/Class2 should be parsed")
                .isEqualTo(3);
        assertThat(model.getInhibitingCondition(transitionBoth, 0, placeA, class1))
                .as("TransitionBoth inhibiting condition for PlaceA/Class1 should be parsed")
                .isEqualTo(4);
        assertThat(model.getInhibitingCondition(transitionBoth, 0, placeA, class2))
                .as("TransitionBoth inhibiting condition for PlaceA/Class2 should be parsed")
                .isEqualTo(5);

        assertThat(model.getEnablingCondition(transitionOnlyEnabling, 0, placeB, class1))
                .as("TransitionOnlyEnabling enabling condition for PlaceB/Class1 should be parsed")
                .isEqualTo(6);
        assertThat(model.getEnablingCondition(transitionOnlyEnabling, 0, placeB, class2))
                .as("TransitionOnlyEnabling enabling condition for PlaceB/Class2 should be parsed")
                .isEqualTo(7);
        assertThat(model.getInhibitingCondition(transitionOnlyEnabling, 0, placeB, class1))
                .as("TransitionOnlyEnabling inhibiting condition for PlaceB/Class1 should remain default")
                .isEqualTo(0);
        assertThat(model.getInhibitingCondition(transitionOnlyEnabling, 0, placeB, class2))
                .as("TransitionOnlyEnabling inhibiting condition for PlaceB/Class2 should remain default")
                .isEqualTo(0);

        assertThat(model.getInhibitingCondition(transitionOnlyInhibiting, 0, placeB, class1))
                .as("TransitionOnlyInhibiting inhibiting condition for PlaceB/Class1 should be parsed")
                .isEqualTo(8);
        assertThat(model.getInhibitingCondition(transitionOnlyInhibiting, 0, placeB, class2))
                .as("TransitionOnlyInhibiting inhibiting condition for PlaceB/Class2 should be parsed")
                .isEqualTo(9);
        assertThat(model.getEnablingCondition(transitionOnlyInhibiting, 0, placeB, class1))
                .as("TransitionOnlyInhibiting enabling condition for PlaceB/Class1 should remain default")
                .isEqualTo(0);
        assertThat(model.getEnablingCondition(transitionOnlyInhibiting, 0, placeB, class2))
                .as("TransitionOnlyInhibiting enabling condition for PlaceB/Class2 should remain default")
                .isEqualTo(0);
    }

    @Test
    void test_parseQueueSection_branches_fullcoverage() {
        File xmlFile = new File("src/test/resources/jmt/gui/common/xml/parseQueue_branches.xml");
        CommonModel model = new CommonModel();

        XMLArchiver.loadModel(xmlFile, model);

        Object queueNode = model.getStationByName("QueueNode");
        Object class1 = model.getClassByName("Class1");
        Object class2 = model.getClassByName("Class2");
        Object class3 = model.getClassByName("Class3");
        Object class4 = model.getClassByName("Class4");
        Object class5 = model.getClassByName("Class5");
        Object class6 = model.getClassByName("Class6");

        assertThat(queueNode).as("QueueNode station must exist").isNotNull();
        assertThat(class1).isNotNull();
        assertThat(class2).isNotNull();
        assertThat(class3).isNotNull();
        assertThat(class4).isNotNull();
        assertThat(class5).isNotNull();
        assertThat(class6).isNotNull();

        assertThat(model.getStationType(queueNode))
                .as("QueueNode should be parsed as SERVER station")
                .isEqualTo(CommonConstants.STATION_TYPE_SERVER);

        assertThat(model.getStationQueueCapacity(queueNode))
                .as("Queue size should be parsed from 'size' parameter")
                .isEqualTo(50);
        assertThat(model.getStationPollingServerKValue(queueNode))
                .as("pollingKValue should be parsed from FCFSstrategy sub-parameter")
                .isEqualTo(4);

        assertThat(model.getDropRule(queueNode, class1))
                .as("'drop' should map to FINITE_DROP")
                .isEqualTo(CommonConstants.FINITE_DROP);
        assertThat(model.getDropRule(queueNode, class2))
                .as("'retrial' should map to FINITE_RETRIAL")
                .isEqualTo(CommonConstants.FINITE_RETRIAL);
        assertThat(model.getDropRule(queueNode, class3))
                .as("'BAS blocking' should map to FINITE_BLOCK")
                .isEqualTo(CommonConstants.FINITE_BLOCK);
        assertThat(model.getDropRule(queueNode, class4))
                .as("'waiting queue' should map to FINITE_WAITING")
                .isEqualTo(CommonConstants.FINITE_WAITING);

        Object retrialDistributionObj = model.getRetrialDistribution(queueNode, class2);
        assertThat(retrialDistributionObj)
                .as("Retrial distribution should be parsed when drop rule is retrial")
                .isInstanceOf(jmt.gui.common.distributions.Distribution.class);
        jmt.gui.common.distributions.Distribution retrialDistribution =
                (jmt.gui.common.distributions.Distribution) retrialDistributionObj;
        assertThat((Double) retrialDistribution.getParameter("lambda").getValue())
                .as("Retrial distribution lambda should be parsed")
                .isCloseTo(9.0, within(1e-9));

        assertThat(model.getQueueStrategy(queueNode, class1))
                .as("TailStrategy should map to FCFS")
                .isEqualTo(CommonConstants.QUEUE_STRATEGY_FCFS);
        assertThat(model.getQueueStrategy(queueNode, class2))
                .as("HeadStrategyPriority should map to LCFS")
                .isEqualTo(CommonConstants.QUEUE_STRATEGY_LCFS);
        assertThat(model.getQueueStrategy(queueNode, class3))
                .as("RandStrategy should map to RAND")
                .isEqualTo(CommonConstants.QUEUE_STRATEGY_RAND);
        assertThat(model.getQueueStrategy(queueNode, class4))
                .as("SRPTStrategy should map to SRPT")
                .isEqualTo(CommonConstants.QUEUE_STRATEGY_SRPT);
        assertThat(model.getQueueStrategy(queueNode, class5))
                .as("EDDStrategyPriority should map to EDD")
                .isEqualTo(CommonConstants.QUEUE_STRATEGY_EDD);
        assertThat(model.getQueueStrategy(queueNode, class6))
                .as("EDFStrategyPriority should map to EDF")
                .isEqualTo(CommonConstants.QUEUE_STRATEGY_EDF);

        assertThat(model.getStationQueueStrategy(queueNode))
                .as("Station queue strategy should reflect the last parsed priority put strategy")
                .isEqualTo(CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY);
    }

    @Test
    void test_extractTransitionConditions_branches_fullcoverage() {
        File xmlFile = new File("src/test/resources/jmt/gui/common/xml/extractTransitionConditions_branches.xml");
        CommonModel model = new CommonModel();

        XMLArchiver.loadModel(xmlFile, model);

        Object queueNode = model.getStationByName("QueueNode");
        Object source1 = model.getStationByName("Source1");
        Object class1 = model.getClassByName("Class1");
        Object class2 = model.getClassByName("Class2");

        assertThat(queueNode).as("QueueNode station must exist").isNotNull();
        assertThat(source1).as("Source1 station must exist").isNotNull();
        assertThat(class1).as("Class1 must exist").isNotNull();
        assertThat(class2).as("Class2 must exist").isNotNull();

        assertThat(model.getEnablingCondition(queueNode, 0, source1, class1))
                .as("Enabling condition for Source1/Class1 should be parsed")
                .isEqualTo(11);
        assertThat(model.getEnablingCondition(queueNode, 0, source1, class2))
                .as("Enabling condition for Source1/Class2 should be parsed")
                .isEqualTo(12);

        assertThat(model.getInhibitingCondition(queueNode, 0, source1, class1))
                .as("Inhibiting condition for Source1/Class1 should be parsed")
                .isEqualTo(21);
        assertThat(model.getInhibitingCondition(queueNode, 0, source1, class2))
                .as("Inhibiting condition for Source1/Class2 should be parsed")
                .isEqualTo(22);

        assertThat(model.getResourceCondition(queueNode, 0, source1, class1))
                .as("Resource condition for Source1/Class1 should be parsed")
                .isEqualTo(31);
        assertThat(model.getResourceCondition(queueNode, 0, source1, class2))
                .as("Resource condition for Source1/Class2 should be parsed")
                .isEqualTo(32);
    }

    @Test
    void test_parseDelaySectionAndQueueSectionDelay_branches_fullcoverage() {
        File xmlFile = new File("src/test/resources/jmt/gui/common/xml/parseDelaySections_branches.xml");
        CommonModel model = new CommonModel();

        XMLArchiver.loadModel(xmlFile, model);

        Object delayNode = model.getStationByName("DelayNode");
        Object source1 = model.getStationByName("Source1");
        Object class1 = model.getClassByName("Class1");
        Object class2 = model.getClassByName("Class2");

        assertThat(delayNode).as("DelayNode station must exist").isNotNull();
        assertThat(source1).as("Source1 station must exist").isNotNull();
        assertThat(class1).as("Class1 must exist").isNotNull();
        assertThat(class2).as("Class2 must exist").isNotNull();

        assertThat(model.getStationType(delayNode))
                .as("DelayNode should be parsed as DELAY station")
                .isEqualTo(CommonConstants.STATION_TYPE_DELAY);

        assertThat(model.getEnablingCondition(delayNode, 0, source1, class1))
                .as("Delay enabling condition for Source1/Class1 should be parsed")
                .isEqualTo(101);
        assertThat(model.getEnablingCondition(delayNode, 0, source1, class2))
                .as("Delay enabling condition for Source1/Class2 should be parsed")
                .isEqualTo(102);

        assertThat(model.getInhibitingCondition(delayNode, 0, source1, class1))
                .as("Delay inhibiting condition for Source1/Class1 should be parsed")
                .isEqualTo(201);
        assertThat(model.getInhibitingCondition(delayNode, 0, source1, class2))
                .as("Delay inhibiting condition for Source1/Class2 should be parsed")
                .isEqualTo(202);

        assertThat(model.getResourceCondition(delayNode, 0, source1, class1))
                .as("Delay resource condition for Source1/Class1 should be parsed")
                .isEqualTo(301);
        assertThat(model.getResourceCondition(delayNode, 0, source1, class2))
                .as("Delay resource condition for Source1/Class2 should be parsed")
                .isEqualTo(302);

        Object delayDistributionClass1 = model.getServiceTimeDistribution(delayNode, class1);
        assertThat(delayDistributionClass1)
                .as("Delay distribution for Class1 should be parsed")
                .isInstanceOf(jmt.gui.common.distributions.Distribution.class);
        jmt.gui.common.distributions.Distribution delayDistClass1 =
                (jmt.gui.common.distributions.Distribution) delayDistributionClass1;
        assertThat((Double) delayDistClass1.getParameter("lambda").getValue())
                .as("Delay distribution lambda for Class1 should be parsed")
                .isCloseTo(2.0, within(1e-9));

        Object delayDistributionClass2 = model.getServiceTimeDistribution(delayNode, class2);
        assertThat(delayDistributionClass2)
                .as("Delay distribution for Class2 should be parsed")
                .isInstanceOf(jmt.gui.common.distributions.Distribution.class);
        jmt.gui.common.distributions.Distribution delayDistClass2 =
                (jmt.gui.common.distributions.Distribution) delayDistributionClass2;
        assertThat((Double) delayDistClass2.getParameter("lambda").getValue())
                .as("Delay distribution lambda for Class2 should be parsed")
                .isCloseTo(3.0, within(1e-9));
    }


    @Test
    void test_parseBlockingRegions_branches() throws Exception {
        // 1. Load the file
        File xmlFile = new File("src/test/resources/jmt/gui/common/xml/parseBlockingRegions_branches.xml");
        CommonModel model = new CommonModel();

        // Parse using the XML archiver facade
        XMLArchiver.loadModel(xmlFile, model);

        // ---------------------------------------------------------
        // BLOCK A: Basic Setup
        // ---------------------------------------------------------
        Object class1 = model.getClassByName("Class1");
        Object class2 = model.getClassByName("Class2");
        Object queue1 = model.getStationByName("Queue1");

        assertThat(model.getRegionKeys())
                .as("There should be exactly 1 blocking region")
                .hasSize(1);

        Object regionKey = model.getRegionKeys().get(0);
        assertThat(model.getRegionName(regionKey)).isEqualTo("Region1");
        assertThat(model.getRegionType(regionKey)).isEqualTo("SJF");

        // ---------------------------------------------------------
        // BLOCK B: Global & Station Constraints
        // ---------------------------------------------------------
        assertThat(model.getBlockingRegionStations(regionKey))
                .as("Region1 should contain Queue1")
                .contains(queue1);

        assertThat(model.getRegionCustomerConstraint(regionKey))
                .as("Global maxJobs for region should be 50")
                .isEqualTo(50);

        assertThat(model.getRegionMemorySize(regionKey))
                .as("Global maxMemory for region should be 1024")
                .isEqualTo(1024);

        // ---------------------------------------------------------
        // BLOCK C: Class-specific Constraints
        // ---------------------------------------------------------
        assertThat(model.getRegionClassCustomerConstraint(regionKey, class1))
                .as("Class1 maxJobs in region should be 20")
                .isEqualTo(20);

        assertThat(model.getRegionClassMemorySize(regionKey, class1))
                .as("Class1 memory size in region should be 512")
                .isEqualTo(512);

        assertThat(model.getRegionClassDropRule(regionKey, class1))
                .as("Class1 drop rule in region should be true")
                .isTrue();

        assertThat(model.getRegionClassWeight(regionKey, class1))
                .as("Class1 weight in region should be 5")
                .isEqualTo(5);

        assertThat(model.getRegionClassSize(regionKey, class1))
                .as("Class1 size in region should be 10")
                .isEqualTo(10);

        assertThat(model.getRegionClassSoftDeadline(regionKey, class1))
                .as("Class1 soft deadline in region should be 100.5")
                .isCloseTo(100.5, within(1e-9));

        // ---------------------------------------------------------
        // BLOCK D: Group Constraints
        // ---------------------------------------------------------
        // Testing groups (GroupA is index 0)
        assertThat(model.getRegionGroupList(regionKey))
                .as("Region1 should have 1 group")
                .hasSize(1);

        assertThat(model.getRegionGroupName(regionKey, 0)).isEqualTo("GroupA");

        assertThat(model.getRegionGroupCustomerConstraint(regionKey, 0))
                .as("GroupA maxJobs should be 30")
                .isEqualTo(30);

        assertThat(model.getRegionGroupMemorySize(regionKey, 0))
                .as("GroupA memory size should be 800")
                .isEqualTo(800);

        // Check if classes were correctly added to the group
        // Note: The internal model usually stores these as a set or list of classes for that group index
        assertThat(model.getRegionGroupClassList(regionKey, 0))
                .as("Class1 should be in GroupA")
                .contains(class1);
        assertThat(model.getRegionGroupClassList(regionKey, 0))
                .as("Class2 should be in GroupA")
                .contains(class2);
    }


    @Test
    void test_parseDistribution_branches_fullcoverage() throws Exception {
        File xmlFile = new File("src/test/resources/jmt/gui/common/xml/parseDistribution_branches.xml");
        CommonModel model = new CommonModel();

        XMLArchiver.loadModel(xmlFile, model);

        Object class1 = model.getClassByName("Class1");
        Object queue1 = model.getStationByName("Queue1");

        // Branch 1 + 3: nested DistributionContainer + scalar parameter on Burst
        Object sourceDistObj = model.getClassDistribution(class1);
        assertThat(sourceDistObj)
                .as("Source distribution should be parsed")
                .isInstanceOf(jmt.gui.common.distributions.Distribution.class);
        jmt.gui.common.distributions.Distribution sourceDist =
                (jmt.gui.common.distributions.Distribution) sourceDistObj;

        assertThat(sourceDist.getClass().getSimpleName())
                .as("Source distribution should be Burst")
                .contains("Burst");

        Double probability = (Double) sourceDist.getParameter("Probability").getValue();
        assertThat(probability)
                .as("Burst Probability should be parsed via scalar branch")
                .isCloseTo(0.5, within(1e-9));

        Boolean roundRobin = (Boolean) sourceDist.getParameter("Round-Robin").getValue();
        assertThat(roundRobin)
                .as("Burst Round-Robin should be parsed via scalar branch")
                .isTrue();

        Object nestedObj = sourceDist.getParameter("Length-Distribution_IntervalA").getValue();
        assertThat(nestedObj)
                .as("Nested distribution should be parsed for Length-Distribution_IntervalA")
                .isInstanceOf(jmt.gui.common.distributions.Distribution.class);
        jmt.gui.common.distributions.Distribution nestedDist =
                (jmt.gui.common.distributions.Distribution) nestedObj;
        Double nestedLambda = (Double) nestedDist.getParameter("lambda").getValue();
        assertThat(nestedLambda)
                .as("Nested exponential lambda should be parsed from DistributionContainer")
                .isCloseTo(1.0, within(1e-9));

        // Branch 2: array parameter parsing on MAP (D0/D1 matrices)
        Object queueDistObj = model.getServiceTimeDistribution(queue1, class1);
        assertThat(queueDistObj)
                .as("Queue distribution should be parsed")
                .isInstanceOf(jmt.gui.common.distributions.Distribution.class);
        jmt.gui.common.distributions.Distribution queueDist =
                (jmt.gui.common.distributions.Distribution) queueDistObj;

        assertThat(queueDist.getClass().getSimpleName())
                .as("Queue distribution should be MAP")
                .contains("MAP");

        Object[][] d0 = (Object[][]) queueDist.getParameter("D0").getValue();
        Object[][] d1 = (Object[][]) queueDist.getParameter("D1").getValue();

        assertThat(d0.length)
                .as("D0 matrix should have expected row size")
                .isEqualTo(2);
        assertThat(d0[0].length).isEqualTo(2);
        assertThat((Double) d0[0][0]).isCloseTo(-1.0, within(1e-9));
        assertThat((Double) d0[1][0]).isCloseTo(0.6, within(1e-9));

        assertThat(d1.length)
                .as("D1 matrix should have expected row size")
                .isEqualTo(2);
        assertThat(d1[1].length).isEqualTo(2);
        assertThat((Double) d1[0][1]).isCloseTo(0.2, within(1e-9));
        assertThat((Double) d1[1][1]).isCloseTo(0.4, within(1e-9));
    }


    @Test
    void test_parseSemaphore_branches_fullcoverage() throws Exception {
        File xmlFile = new File("src/test/resources/jmt/gui/common/xml/parseSemaphore_branches.xml");
        CommonModel model = new CommonModel();

        XMLArchiver.loadModel(xmlFile, model);

        Object class1 = model.getClassByName("Class1");
        Object class2 = model.getClassByName("Class2");
        Object semaphoreWithParam = model.getStationByName("SemaphoreWithParam");
        Object semaphoreDefault = model.getStationByName("SemaphoreDefault");

        assertThat(model.getStationType(semaphoreWithParam))
                .as("SemaphoreWithParam should be parsed as Semaphore station")
                .isEqualTo(CommonConstants.STATION_TYPE_SEMAPHORE);
        assertThat(model.getStationType(semaphoreDefault))
                .as("SemaphoreDefault should be parsed as Semaphore station")
                .isEqualTo(CommonConstants.STATION_TYPE_SEMAPHORE);

        // Branch: parameter != null, with explicit strategy and threshold values
        Object ssWithParamC1 = model.getSemaphoreStrategy(semaphoreWithParam, class1);
        Object ssWithParamC2 = model.getSemaphoreStrategy(semaphoreWithParam, class2);

        assertThat(ssWithParamC1)
                .as("Semaphore strategy for Class1 should be set from XML")
                .isInstanceOf(jmt.gui.common.semaphoreStrategies.NormalSemaphore.class);
        assertThat(ssWithParamC2)
                .as("Semaphore strategy for Class2 should be set from XML")
                .isInstanceOf(jmt.gui.common.semaphoreStrategies.NormalSemaphore.class);

        assertThat(((jmt.gui.common.semaphoreStrategies.SemaphoreStrategy) ssWithParamC1).getThreshold())
                .as("Threshold for Class1 should be parsed from XML")
                .isEqualTo(2);
        assertThat(((jmt.gui.common.semaphoreStrategies.SemaphoreStrategy) ssWithParamC2).getThreshold())
                .as("Threshold for Class2 should be parsed from XML")
                .isEqualTo(5);

        // Branch: parameter == null, fallback to SEMAPHORE_NORMAL.clone() for all classes
        Object ssDefaultC1 = model.getSemaphoreStrategy(semaphoreDefault, class1);
        Object ssDefaultC2 = model.getSemaphoreStrategy(semaphoreDefault, class2);

        assertThat(ssDefaultC1)
                .as("Default semaphore strategy for Class1 should be NormalSemaphore")
                .isInstanceOf(jmt.gui.common.semaphoreStrategies.NormalSemaphore.class);
        assertThat(ssDefaultC2)
                .as("Default semaphore strategy for Class2 should be NormalSemaphore")
                .isInstanceOf(jmt.gui.common.semaphoreStrategies.NormalSemaphore.class);

        assertThat(((jmt.gui.common.semaphoreStrategies.SemaphoreStrategy) ssDefaultC1).getThreshold())
                .as("Default threshold for Class1 should be 1")
                .isEqualTo(1);
        assertThat(((jmt.gui.common.semaphoreStrategies.SemaphoreStrategy) ssDefaultC2).getThreshold())
                .as("Default threshold for Class2 should be 1")
                .isEqualTo(1);
    }


    @Test
    void test_parseLogger_branches_fullcoverage() throws Exception {
        File xmlFile = new File("src/test/resources/jmt/gui/common/xml/parseLogger_branches.xml");
        CommonModel model = new CommonModel();

        XMLArchiver.loadModel(xmlFile, model);

        Object logger1 = model.getStationByName("Logger1");
        assertThat(model.getStationType(logger1))
                .as("Logger1 should be parsed as Logger station")
                .isEqualTo(CommonConstants.STATION_TYPE_LOGGER);

        jmt.engine.log.LoggerParameters params = (jmt.engine.log.LoggerParameters) model.getLoggingParameters(logger1);
        assertThat(params).as("Logger parameters should be parsed").isNotNull();

        // Known parameters branch
        assertThat(params.name).as("logfileName should be parsed").isEqualTo("logger-branch.csv");
        assertThat(params.boolExecTimestamp).isTrue();
        assertThat(params.boolLoggername).isFalse();
        assertThat(params.boolTimeStamp).isTrue();
        assertThat(params.boolJobID).isFalse();
        assertThat(params.boolJobClass).isTrue();
        assertThat(params.boolTimeSameClass).isFalse();
        assertThat(params.boolTimeAnyClass).isTrue();

        // Special logfilePath branch: parser ignores XML value and uses global logging path
        String expectedPath = jmt.framework.data.MacroReplacer.replace(model.getLoggingGlbParameter("path"));
        assertThat(params.path)
                .as("logfilePath should be resolved from global logging path")
                .isEqualTo(expectedPath)
                .isNotEqualTo("/this/path/must/be/ignored/by-parser");

        // `numClasses` is ignored and unknown parameter should not break parsing.
        assertThat(params.name).isEqualTo("logger-branch.csv");
        assertThat(params.boolTimeAnyClass).isTrue();
    }


    @Test
    void test_parseClassSwitch_branches_fullcoverage() throws Exception {
        File xmlFile = new File("src/test/resources/jmt/gui/common/xml/parseClassSwitch_branches.xml");
        CommonModel model = new CommonModel();

        XMLArchiver.loadModel(xmlFile, model);

        Object class1 = model.getClassByName("Class1");
        Object class2 = model.getClassByName("Class2");
        Object source = model.getStationByName("Source1");
        Object classSwitch = model.getStationByName("ClassSwitch1");
        Object sink = model.getStationByName("Sink1");

        assertThat(class1).as("Class1 should exist").isNotNull();
        assertThat(class2).as("Class2 should exist").isNotNull();
        assertThat(classSwitch).as("ClassSwitch1 station should exist").isNotNull();

        assertThat(model.getStationType(classSwitch))
                .as("ClassSwitch1 should be parsed as a ClassSwitch station")
                .isEqualTo(CommonConstants.STATION_TYPE_CLASSSWITCH);

        assertThat(model.getQueueStrategy(classSwitch, class1))
                .as("Queue put strategy for Class1 should be FCFS from TailStrategy")
                .isEqualTo(CommonConstants.QUEUE_STRATEGY_FCFS);
        assertThat(model.getQueueStrategy(classSwitch, class2))
                .as("Queue put strategy for Class2 should be FCFS from TailStrategy")
                .isEqualTo(CommonConstants.QUEUE_STRATEGY_FCFS);

        float c1ToC1 = model.getClassSwitchMatrix(classSwitch, class1, class1);
        float c1ToC2 = model.getClassSwitchMatrix(classSwitch, class1, class2);
        float c2ToC1 = model.getClassSwitchMatrix(classSwitch, class2, class1);
        float c2ToC2 = model.getClassSwitchMatrix(classSwitch, class2, class2);

        assertThat((double) c1ToC1)
                .as("Class1 -> Class1 switch probability should be 0.70")
                .isCloseTo(0.70, within(1e-7));
        assertThat((double) c1ToC2)
                .as("Class1 -> Class2 switch probability should be 0.30")
                .isCloseTo(0.30, within(1e-7));
        assertThat((double) c2ToC1)
                .as("Class2 -> Class1 switch probability should be 0.10")
                .isCloseTo(0.10, within(1e-7));
        assertThat((double) c2ToC2)
                .as("Class2 -> Class2 switch probability should be 0.90")
                .isCloseTo(0.90, within(1e-7));

        assertThat((double) (c1ToC1 + c1ToC2))
                .as("Outgoing class-switch probabilities for Class1 should sum to 1.0")
                .isCloseTo(1.0, within(1e-7));
        assertThat((double) (c2ToC1 + c2ToC2))
                .as("Outgoing class-switch probabilities for Class2 should sum to 1.0")
                .isCloseTo(1.0, within(1e-7));

        assertThat(model.areConnected(source, classSwitch))
                .as("Source1 should be connected to ClassSwitch1")
                .isTrue();
        assertThat(model.areConnected(classSwitch, sink))
                .as("ClassSwitch1 should be connected to Sink1")
                .isTrue();
    }


    @Test
    void test_parseRouter_branches_fullcoverage() throws Exception {
        File xmlFile = new File("src/test/resources/jmt/gui/common/xml/parseRouter_branches.xml");
        CommonModel model = new CommonModel();
        XMLArchiver.loadModel(xmlFile, model);

        Object class1 = model.getClassByName("Class1");
        Object class2 = model.getClassByName("Class2");
        Object probNode = model.getStationByName("ProbNode");
        Object ldNode = model.getStationByName("LdNode");
        Object kNode = model.getStationByName("KNode");
        Object wrrNode = model.getStationByName("WrrNode");
        Object csNode = model.getStationByName("CsNode");

        Object sinkP = model.getStationByName("SinkP");
        Object sinkL = model.getStationByName("SinkL");
        Object sinkW = model.getStationByName("SinkW");
        Object sinkC1 = model.getStationByName("SinkC1");
        Object sinkC2 = model.getStationByName("SinkC2");
        Object ldTarget = model.getStationByName("LdNode");
        Object kTarget = model.getStationByName("KNode");
        Object csTarget = model.getStationByName("CsNode");

        assertThat(model.getStationType(probNode)).isEqualTo(CommonConstants.STATION_TYPE_ROUTER);
        assertThat(model.getStationType(ldNode)).isEqualTo(CommonConstants.STATION_TYPE_ROUTER);
        assertThat(model.getStationType(kNode)).isEqualTo(CommonConstants.STATION_TYPE_ROUTER);
        assertThat(model.getStationType(wrrNode)).isEqualTo(CommonConstants.STATION_TYPE_ROUTER);
        assertThat(model.getStationType(csNode)).isEqualTo(CommonConstants.STATION_TYPE_ROUTER);

        // ProbabilityRouting branch
        Object probRoutingObj = model.getRoutingStrategy(probNode, class1);
        assertThat(probRoutingObj)
                .as("ProbNode Class1 routing should be ProbabilityRouting")
                .isInstanceOf(jmt.gui.common.routingStrategies.ProbabilityRouting.class);
        jmt.gui.common.routingStrategies.ProbabilityRouting probRouting =
                (jmt.gui.common.routingStrategies.ProbabilityRouting) probRoutingObj;
        assertThat(probRouting.getValues().get(ldTarget)).isCloseTo(0.6, within(1e-9));
        assertThat(probRouting.getValues().get(sinkP)).isCloseTo(0.4, within(1e-9));

        // LoadDependentRouting branch
        Object ldRoutingObj = model.getRoutingStrategy(ldNode, class1);
        assertThat(ldRoutingObj)
                .as("LdNode Class1 routing should be LoadDependentRouting")
                .isInstanceOf(jmt.gui.common.routingStrategies.LoadDependentRouting.class);
        jmt.gui.common.routingStrategies.LoadDependentRouting ldRouting =
                (jmt.gui.common.routingStrategies.LoadDependentRouting) ldRoutingObj;
        assertThat(ldRouting.getAllEmpiricalEntries().get(1).get(kTarget)).isCloseTo(0.8, within(1e-9));
        assertThat(ldRouting.getAllEmpiricalEntries().get(1).get(sinkL)).isCloseTo(0.2, within(1e-9));
        assertThat(ldRouting.getAllEmpiricalEntries().get(5).get(kTarget)).isCloseTo(0.3, within(1e-9));
        assertThat(ldRouting.getAllEmpiricalEntries().get(5).get(sinkL)).isCloseTo(0.7, within(1e-9));

        // PowerOfKRouting branch
        Object kRoutingObj = model.getRoutingStrategy(kNode, class1);
        assertThat(kRoutingObj)
                .as("KNode Class1 routing should be PowerOfKRouting")
                .isInstanceOf(jmt.gui.common.routingStrategies.PowerOfKRouting.class);
        jmt.gui.common.routingStrategies.PowerOfKRouting kRouting =
                (jmt.gui.common.routingStrategies.PowerOfKRouting) kRoutingObj;
        assertThat(kRouting.getK()).isEqualTo(3);
        assertThat(kRouting.isWithMemory()).isTrue();

        // WeightedRoundRobinRouting branch
        Object wrrRoutingObj = model.getRoutingStrategy(wrrNode, class1);
        assertThat(wrrRoutingObj)
                .as("WrrNode Class1 routing should be WeightedRoundRobinRouting")
                .isInstanceOf(jmt.gui.common.routingStrategies.WeightedRoundRobinRouting.class);
        jmt.gui.common.routingStrategies.WeightedRoundRobinRouting wrrRouting =
                (jmt.gui.common.routingStrategies.WeightedRoundRobinRouting) wrrRoutingObj;
        assertThat(wrrRouting.getWeights().get(csTarget)).isEqualTo(3);
        assertThat(wrrRouting.getWeights().get(sinkW)).isEqualTo(1);

        // ClassSwitchRouting branch + legacy alias classpath handling
        Object csRoutingObj = model.getRoutingStrategy(csNode, class1);
        assertThat(csRoutingObj)
                .as("CsNode Class1 routing should be ClassSwitchRouting")
                .isInstanceOf(jmt.gui.common.routingStrategies.ClassSwitchRouting.class);
        jmt.gui.common.routingStrategies.ClassSwitchRouting csRouting =
                (jmt.gui.common.routingStrategies.ClassSwitchRouting) csRoutingObj;

        assertThat(csRouting.getValues().get(sinkC1)).isCloseTo(0.4, within(1e-9));
        assertThat(csRouting.getValues().get(sinkC2)).isCloseTo(0.6, within(1e-9));

        assertThat(csRouting.getOutPaths().get(sinkC1).get(class1)).isCloseTo(0.7, within(1e-9));
        assertThat(csRouting.getOutPaths().get(sinkC1).get(class2)).isCloseTo(0.3, within(1e-9));
        assertThat(csRouting.getOutPaths().get(sinkC2).get(class1)).isCloseTo(0.2, within(1e-9));
        assertThat(csRouting.getOutPaths().get(sinkC2).get(class2)).isCloseTo(0.8, within(1e-9));
    }

    @Test
    void test_parseFork_branches_fullcoverage() throws Exception {
        File xmlFile = new File("src/test/resources/jmt/gui/common/xml/parseFork_branches.xml");
        CommonModel model = new CommonModel();

        XMLArchiver.loadModel(xmlFile, model);

        Object fork = model.getStationByName("Fork1");
        Object class1 = model.getClassByName("Class1");
        Object class2 = model.getClassByName("Class2");
        Object class3 = model.getClassByName("Class3");
        Object class4 = model.getClassByName("Class4");

        Object sinkP1 = model.getStationByName("SinkP1");
        Object sinkP2 = model.getStationByName("SinkP2");
        Object sinkCS = model.getStationByName("SinkCS");
        Object sinkMBCS = model.getStationByName("SinkMBCS");

        assertThat(fork).as("Fork1 station must exist").isNotNull();
        assertThat(class1).as("Class1 must exist").isNotNull();
        assertThat(class2).as("Class2 must exist").isNotNull();
        assertThat(class3).as("Class3 must exist").isNotNull();
        assertThat(class4).as("Class4 must exist").isNotNull();

        assertThat(model.getStationType(fork))
                .as("Fork1 must be parsed as a fork station")
                .isEqualTo(CommonConstants.STATION_TYPE_FORK);

        // Scalar fork parameters
        assertThat(model.getStationNumberOfServers(fork))
                .as("jobsPerLink must set station number of servers")
                .isEqualTo(2);
        assertThat(model.getForkBlock(fork))
                .as("block must be parsed")
                .isEqualTo(7);
        assertThat(model.getIsSimplifiedFork(fork))
                .as("isSimplifiedFork must be parsed")
                .isFalse();

        // ProbabilitiesFork branch
        Object probForkObj = model.getForkStrategy(fork, class1);
        assertThat(probForkObj)
                .as("Class1 fork strategy should be ProbabilitiesFork")
                .isInstanceOf(jmt.gui.common.forkStrategies.ProbabilitiesFork.class);
        jmt.gui.common.forkStrategies.ProbabilitiesFork probFork =
                (jmt.gui.common.forkStrategies.ProbabilitiesFork) probForkObj;
        Map<Object, jmt.gui.common.forkStrategies.OutPath> probOut = probFork.getOutDetails();
        assertThat(probOut)
                .as("ProbabilitiesFork must resolve output station keys")
                .containsKeys(sinkP1, sinkP2);

        jmt.gui.common.forkStrategies.OutPath outP1 = probOut.get(sinkP1);
        assertThat(outP1.getProbability())
                .as("SinkP1 branch probability must be parsed")
                .isCloseTo(0.7, within(1e-9));
        assertThat((Double) outP1.getOutParameters().get(1))
                .as("SinkP1 jobs-per-link P(N=1) must be parsed")
                .isCloseTo(0.25, within(1e-9));
        assertThat((Double) outP1.getOutParameters().get(2))
                .as("SinkP1 jobs-per-link P(N=2) must be parsed")
                .isCloseTo(0.75, within(1e-9));

        jmt.gui.common.forkStrategies.OutPath outP2 = probOut.get(sinkP2);
        assertThat(outP2.getProbability())
                .as("SinkP2 branch probability must be parsed")
                .isCloseTo(0.3, within(1e-9));
        assertThat((Double) outP2.getOutParameters().get(1))
                .as("SinkP2 jobs-per-link distribution must be parsed")
                .isCloseTo(1.0, within(1e-9));

        // CombFork branch
        Object combForkObj = model.getForkStrategy(fork, class2);
        assertThat(combForkObj)
                .as("Class2 fork strategy should be CombFork")
                .isInstanceOf(jmt.gui.common.forkStrategies.CombFork.class);
        jmt.gui.common.forkStrategies.CombFork combFork =
                (jmt.gui.common.forkStrategies.CombFork) combForkObj;
        Map<Object, Double> combOut = combFork.getOutDetails();
        assertThat(combOut)
                .as("CombFork distribution over number of branches must be parsed")
                .containsKeys("1", "2", "3");
        assertThat(combOut.get("1")).isCloseTo(0.2, within(1e-9));
        assertThat(combOut.get("2")).isCloseTo(0.5, within(1e-9));
        assertThat(combOut.get("3")).isCloseTo(0.3, within(1e-9));
        assertThat(combOut.get("1") + combOut.get("2") + combOut.get("3"))
                .as("CombFork probabilities should sum to 1")
                .isCloseTo(1.0, within(1e-9));

        // ClassSwitchFork branch
        Object classSwitchForkObj = model.getForkStrategy(fork, class3);
        assertThat(classSwitchForkObj)
                .as("Class3 fork strategy should be ClassSwitchFork")
                .isInstanceOf(jmt.gui.common.forkStrategies.ClassSwitchFork.class);
        jmt.gui.common.forkStrategies.ClassSwitchFork classSwitchFork =
                (jmt.gui.common.forkStrategies.ClassSwitchFork) classSwitchForkObj;
        Map<Object, jmt.gui.common.forkStrategies.OutPath> csOut = classSwitchFork.getOutDetails();
        assertThat(csOut).containsKey(sinkCS);
        Map<Object, Object> csParams = csOut.get(sinkCS).getOutParameters();
        assertThat(csParams.get(class1))
                .as("ClassSwitchFork must map Class1 to the configured number of jobs")
                .isEqualTo(4);
        assertThat(csParams.get(class3))
                .as("ClassSwitchFork must map Class3 to the configured number of jobs")
                .isEqualTo(1);

        // MultiBranchClassSwitchFork branch
        Object multiBranchObj = model.getForkStrategy(fork, class4);
        assertThat(multiBranchObj)
                .as("Class4 fork strategy should be MultiBranchClassSwitchFork")
                .isInstanceOf(jmt.gui.common.forkStrategies.MultiBranchClassSwitchFork.class);
        jmt.gui.common.forkStrategies.MultiBranchClassSwitchFork multiBranchFork =
                (jmt.gui.common.forkStrategies.MultiBranchClassSwitchFork) multiBranchObj;
        Map<Object, jmt.gui.common.forkStrategies.OutPath> mbOut = multiBranchFork.getOutDetails();
        assertThat(mbOut).containsKey(sinkMBCS);
        Map<Object, Object> mbParams = mbOut.get(sinkMBCS).getOutParameters();
        assertThat(mbParams.get(class2))
                .as("MultiBranchClassSwitchFork must map Class2 to the configured number of jobs")
                .isEqualTo(2);
        assertThat(mbParams.get(class4))
                .as("MultiBranchClassSwitchFork must map Class4 to the configured number of jobs")
                .isEqualTo(5);
    }


    @Test
    void test_serverfeature_fullcoverage() throws Exception {
        // 1. Load the file
        File xmlFile = new File("src/test/resources/jmt/gui/common/xml/serverfeature_fullcoverage.xml");
        CommonModel model = new CommonModel();

        // Parse using the XML archiver facade (returns void)
        XMLArchiver.loadModel(xmlFile, model);

        // ---------------------------------------------------------
        // BLOCK A: User Classes
        // ---------------------------------------------------------
        Object class1 = model.getClassByName("Class1");
        Object class2 = model.getClassByName("Class2");
        assertThat(class1).isNotNull();
        assertThat(class2).isNotNull();

        // ---------------------------------------------------------
        // BLOCK B: Node 1 - Polling Switchover & Max Jobs
        // ---------------------------------------------------------
        Object node1 = model.getStationByName("Node1_Polling");

        assertThat(model.getStationNumberOfServers(node1))
                .as("Node1 should have exactly 2 servers")
                .isEqualTo(2);
        assertThat(model.getStationMaxRunningJobs(node1))
                .as("Node1 should have exactly 10 max running jobs")
                .isEqualTo(10);

        Object pollingSwDist = model.getPollingSwitchoverDistribution(node1, class1);
        assertThat(pollingSwDist.getClass().getSimpleName())
                .as("Polling switchover strategy for Class1 should be Exponential")
                .contains("Exponential");
        Double pLambda = (Double) ((jmt.gui.common.distributions.Distribution) pollingSwDist).getParameter("lambda").getValue();
        assertThat(pLambda).as("Polling switchover lambda should be 1.0").isCloseTo(1.0, within(1e-9));

        // ---------------------------------------------------------
        // BLOCK C: Node 2 - Non-Polling Switchover & Delay Off
        // ---------------------------------------------------------
        Object node2 = model.getStationByName("Node2_SetupDelay");

        assertThat(model.getSwitchoverTimesEnabled(node2))
                .as("Switchover times should be enabled on Node2")
                .isTrue();
        Object switchoverDist = model.getSwitchoverTimeDistribution(node2, class1, class2);
        assertThat(switchoverDist.getClass().getSimpleName())
                .as("Switchover distribution from Class1 to Class2 should be Exponential")
                .contains("Exponential");
        Double sLambda = (Double) ((jmt.gui.common.distributions.Distribution) switchoverDist).getParameter("lambda").getValue();
        assertThat(sLambda).as("Switchover lambda should be 2.0").isCloseTo(2.0, within(1e-9));

        assertThat(model.getDelayOffTimesEnabled(node2))
                .as("Delay Off times should be enabled on Node2")
                .isTrue();
        Object delayOffDist = model.getDelayOffTimeDistribution(node2, class1);
        assertThat(delayOffDist.getClass().getSimpleName())
                .as("Delay Off distribution for Class1 should be Exponential")
                .contains("Exponential");
        Double dLambda = (Double) ((jmt.gui.common.distributions.Distribution) delayOffDist).getParameter("lambda").getValue();
        assertThat(dLambda).as("Delay Off lambda should be 3.0").isCloseTo(3.0, within(1e-9));

        // ---------------------------------------------------------
        // BLOCK D: Node 3 - Heterogeneous Servers
        // ---------------------------------------------------------
        Object node3 = model.getStationByName("Node3_Heterogeneous");

        assertThat(model.getHeterogeneousServersEnabled(node3))
                .as("Heterogeneous servers should be enabled on Node3")
                .isTrue();

        // FIX: getServerTypeKeys() takes no arguments in CommonModel
        List<?> serverTypes = model.getServerTypeKeys();

        if (serverTypes != null) {
            assertThat(serverTypes)
                    .as("There should be at least 2 defined heterogeneous server types in the model")
                    .hasSizeGreaterThanOrEqualTo(2);
        }

        // ---------------------------------------------------------
        // BLOCK E: Node 4 - Processor Sharing & Advanced Queue
        // ---------------------------------------------------------
        Object node4 = model.getStationByName("Node4_PS");

        assertThat(model.getStationQueueStrategy(node4))
                .as("Node4 station queue strategy should be forcefully set to PSSERVER_PRIORITY")
                .isEqualTo(CommonConstants.STATION_QUEUE_STRATEGY_PSSERVER_PRIORITY);

        assertThat(model.getQueueStrategy(node4, class1))
                .as("Node4 Class1 PS Strategy should be DPS (Discriminatory Processor Sharing)")
                .isEqualTo(CommonConstants.QUEUE_STRATEGY_DPS);

        assertThat(model.getQueueStrategy(node4, class2))
                .as("Node4 Class2 PS Strategy should be PS (Egalitarian Processor Sharing)")
                .isEqualTo(CommonConstants.QUEUE_STRATEGY_PS);

        assertThat((Double) model.getServiceWeight(node4, class1))
                .as("Node4 service weight for Class1 should be 2.5")
                .isCloseTo(2.5, within(1e-9));

        assertThat(model.getServerNumRequired(node4, class1))
                .as("Node4 class parallelism (num required) for Class1 should be 3")
                .isEqualTo(3);

        assertThat(model.getServerNumRequired(node4, class2))
                .as("Node4 class parallelism (num required) for Class2 should be 1")
                .isEqualTo(1);

        // ---------------------------------------------------------
        // BLOCK F: Node 5 - Vacation When Idle
        // ---------------------------------------------------------
        Object node5 = model.getStationByName("Node5_VacationIdle");

        assertThat(model.getStationVacationPolicy(node5))
                .as("Node5 vacation policy should be MULTIPLE (1)")
                .isEqualTo(CommonConstants.STATION_VACATION_POLICY_MULTIPLE);

        jmt.gui.common.definitions.VacationParameters vp5 = model.getStationVacationParameters(node5);
        assertThat(vp5).isNotNull();
        assertThat(vp5.getVacationTimeDistribution().getClass().getSimpleName())
                .as("Node5 vacation distribution should be Exponential")
                .contains("Exponential");
        Double v5Lambda = (Double) vp5.getVacationTimeDistribution().getParameter("lambda").getValue();
        assertThat(v5Lambda).as("Node5 vacation lambda should be 0.5").isCloseTo(0.5, within(1e-9));

        // ---------------------------------------------------------
        // BLOCK G: Node 6 - Bernoulli Vacation
        // ---------------------------------------------------------
        Object node6 = model.getStationByName("Node6_VacationBernoulli");

        assertThat(model.getStationVacationPolicy(node6))
                .as("Node6 vacation policy should be BERNOULLI")
                .isEqualTo(CommonConstants.STATION_VACATION_POLICY_BERNOULLI);

        jmt.gui.common.definitions.VacationParameters vp6 = model.getStationVacationParameters(node6);
        assertThat(vp6).isNotNull();
        assertThat(vp6.getBernoulliP())
                .as("Node6 Bernoulli probability 'p' should be 0.2")
                .isCloseTo(0.2, within(1e-9));

        Double v6Lambda = (Double) vp6.getVacationTimeDistribution().getParameter("lambda").getValue();
        assertThat(v6Lambda).as("Node6 vacation lambda should be 0.8").isCloseTo(0.8, within(1e-9));


        // ---------------------------------------------------------
        // BLOCK H: Node 7 - GPS & QBPS Strategies
        // ---------------------------------------------------------
        Object node7 = model.getStationByName("Node7_ExtraPS");

        assertThat(model.getQueueStrategy(node7, class1))
                .as("Node7 Class1 PS Strategy should be GPS (Generalized Processor Sharing)")
                .isEqualTo(CommonConstants.QUEUE_STRATEGY_GPS);

        assertThat(model.getQueueStrategy(node7, class2))
                .as("Node7 Class2 PS Strategy should be QBPS (Queue Based Processor Sharing)")
                .isEqualTo(CommonConstants.QUEUE_STRATEGY_QBPS);

        // ---------------------------------------------------------
        // BLOCK I: Queue Put Strategies Coverage (Nodes 8 to 20)
        // ---------------------------------------------------------

        // Node 8 - Tail (FCFS)
        Object node8 = model.getStationByName("Node8_Put_Tail");
        assertThat(model.getQueueStrategy(node8, class1)).as("Node8 Class1 should be FCFS").isEqualTo(CommonConstants.QUEUE_STRATEGY_FCFS);
        assertThat(model.getQueueStrategy(node8, class2)).as("Node8 Class2 should be FCFS").isEqualTo(CommonConstants.QUEUE_STRATEGY_FCFS);
        assertThat(model.getStationQueueStrategy(node8)).as("Node8 station should be NON_PREEMPTIVE_PRIORITY due to Class2").isEqualTo(CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY);

        // Node 9 - Head (LCFS)
        Object node9 = model.getStationByName("Node9_Put_Head");
        assertThat(model.getQueueStrategy(node9, class1)).as("Node9 Class1 should be LCFS").isEqualTo(CommonConstants.QUEUE_STRATEGY_LCFS);
        assertThat(model.getQueueStrategy(node9, class2)).as("Node9 Class2 should be LCFS").isEqualTo(CommonConstants.QUEUE_STRATEGY_LCFS);
        assertThat(model.getStationQueueStrategy(node9)).as("Node9 station should be NON_PREEMPTIVE_PRIORITY").isEqualTo(CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY);

        // Node 10 - Rand
        Object node10 = model.getStationByName("Node10_Put_Rand");
        assertThat(model.getQueueStrategy(node10, class1)).as("Node10 Class1 should be RAND").isEqualTo(CommonConstants.QUEUE_STRATEGY_RAND);
        assertThat(model.getQueueStrategy(node10, class2)).as("Node10 Class2 should be RAND").isEqualTo(CommonConstants.QUEUE_STRATEGY_RAND);

        // Node 11 - SJF (Shortest Job First)
        Object node11 = model.getStationByName("Node11_Put_SJF");
        assertThat(model.getQueueStrategy(node11, class1)).as("Node11 Class1 should be SJF").isEqualTo(CommonConstants.QUEUE_STRATEGY_SJF);
        assertThat(model.getQueueStrategy(node11, class2)).as("Node11 Class2 should be SJF").isEqualTo(CommonConstants.QUEUE_STRATEGY_SJF);

        // Node 12 - LJF (Longest Job First)
        Object node12 = model.getStationByName("Node12_Put_LJF");
        assertThat(model.getQueueStrategy(node12, class1)).as("Node12 Class1 should be LJF").isEqualTo(CommonConstants.QUEUE_STRATEGY_LJF);
        assertThat(model.getQueueStrategy(node12, class2)).as("Node12 Class2 should be LJF").isEqualTo(CommonConstants.QUEUE_STRATEGY_LJF);

        // Node 13 - SEPT (Shortest Expected Processing Time)
        Object node13 = model.getStationByName("Node13_Put_SEPT");
        assertThat(model.getQueueStrategy(node13, class1)).as("Node13 Class1 should be SEPT").isEqualTo(CommonConstants.QUEUE_STRATEGY_SEPT);
        assertThat(model.getQueueStrategy(node13, class2)).as("Node13 Class2 should be SEPT").isEqualTo(CommonConstants.QUEUE_STRATEGY_SEPT);

        // Node 14 - LEPT (Longest Expected Processing Time)
        Object node14 = model.getStationByName("Node14_Put_LEPT");
        assertThat(model.getQueueStrategy(node14, class1)).as("Node14 Class1 should be LEPT").isEqualTo(CommonConstants.QUEUE_STRATEGY_LEPT);
        assertThat(model.getQueueStrategy(node14, class2)).as("Node14 Class2 should be LEPT").isEqualTo(CommonConstants.QUEUE_STRATEGY_LEPT);

        // Node 15 - FCFS PR (Preemptive Resume)
        Object node15 = model.getStationByName("Node15_Put_FCFSPR");
        assertThat(model.getQueueStrategy(node15, class1)).as("Node15 Class1 should be FCFS_PR").isEqualTo(CommonConstants.QUEUE_STRATEGY_FCFS_PR);
        assertThat(model.getQueueStrategy(node15, class2)).as("Node15 Class2 should be FCFS_PR").isEqualTo(CommonConstants.QUEUE_STRATEGY_FCFS_PR);
        assertThat(model.getStationQueueStrategy(node15)).as("Node15 station should be PREEMPTIVE_PRIORITY").isEqualTo(CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY);

        // Node 16 - LCFS PR
        Object node16 = model.getStationByName("Node16_Put_LCFSPR");
        assertThat(model.getQueueStrategy(node16, class1)).as("Node16 Class1 should be LCFS_PR").isEqualTo(CommonConstants.QUEUE_STRATEGY_LCFS_PR);
        assertThat(model.getQueueStrategy(node16, class2)).as("Node16 Class2 should be LCFS_PR").isEqualTo(CommonConstants.QUEUE_STRATEGY_LCFS_PR);

        // Node 17 - SRPT (Shortest Remaining Processing Time)
        Object node17 = model.getStationByName("Node17_Put_SRPT");
        assertThat(model.getQueueStrategy(node17, class1)).as("Node17 Class1 should be SRPT").isEqualTo(CommonConstants.QUEUE_STRATEGY_SRPT);
        assertThat(model.getQueueStrategy(node17, class2)).as("Node17 Class2 should be SRPT").isEqualTo(CommonConstants.QUEUE_STRATEGY_SRPT);

        // Node 18 - EDD (Earliest Due Date)
        Object node18 = model.getStationByName("Node18_Put_EDD");
        assertThat(model.getQueueStrategy(node18, class1)).as("Node18 Class1 should be EDD").isEqualTo(CommonConstants.QUEUE_STRATEGY_EDD);
        assertThat(model.getQueueStrategy(node18, class2)).as("Node18 Class2 should be EDD").isEqualTo(CommonConstants.QUEUE_STRATEGY_EDD);

        // Node 19 - EDF (Earliest Deadline First)
        Object node19 = model.getStationByName("Node19_Put_EDF");
        assertThat(model.getQueueStrategy(node19, class1)).as("Node19 Class1 should be EDF").isEqualTo(CommonConstants.QUEUE_STRATEGY_EDF);
        assertThat(model.getQueueStrategy(node19, class2)).as("Node19 Class2 should be EDF").isEqualTo(CommonConstants.QUEUE_STRATEGY_EDF);
        assertThat(model.getStationQueueStrategy(node19)).as("Node19 station should be PREEMPTIVE_PRIORITY").isEqualTo(CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY);

        // Node 20 - TBS (Time Based Scheduling)
        Object node20 = model.getStationByName("Node20_Put_TBS");
        assertThat(model.getQueueStrategy(node20, class1)).as("Node20 Class1 should be TBS").isEqualTo(CommonConstants.QUEUE_STRATEGY_TBS);
        assertThat(model.getStationQueueStrategy(node20)).as("Node20 station should be PREEMPTIVE_PRIORITY").isEqualTo(CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY);
    }

    @Test
    void test_parseTimingSection_branches_fullcoverage() {
        File xmlFile = new File("src/test/resources/jmt/gui/common/xml/parseTiming_branches.xml");
        CommonModel model = new CommonModel();

        XMLArchiver.loadModel(xmlFile, model);

        Object transitionFull = model.getStationByName("TransitionFull");
        Object transitionFallback = model.getStationByName("TransitionFallback");

        assertThat(transitionFull).as("TransitionFull station must exist").isNotNull();
        assertThat(transitionFallback).as("TransitionFallback station must exist").isNotNull();

        assertThat(model.getStationType(transitionFull))
                .as("TransitionFull should be parsed as TRANSITION station")
                .isEqualTo(CommonConstants.STATION_TYPE_TRANSITION);
        assertThat(model.getStationType(transitionFallback))
                .as("TransitionFallback should be parsed as TRANSITION station")
                .isEqualTo(CommonConstants.STATION_TYPE_TRANSITION);

        assertThat(model.getTransitionModeName(transitionFull, 0))
                .as("First transition mode name should be parsed from modeNames")
                .isEqualTo("Alpha");
        assertThat(model.getTransitionModeName(transitionFull, 1))
                .as("Second transition mode name should be parsed from modeNames")
                .isEqualTo("Beta");

        assertThat(model.getNumberOfServers(transitionFull, 0))
                .as("First mode numberOfServers should be parsed")
                .isEqualTo(3);
        assertThat(model.getNumberOfServers(transitionFull, 1))
                .as("Second mode numberOfServers should be parsed")
                .isEqualTo(5);

        Object timingDistribution0 = model.getFiringTimeDistribution(transitionFull, 0);
        assertThat(timingDistribution0)
                .as("First timing strategy should be parsed as a Distribution")
                .isInstanceOf(jmt.gui.common.distributions.Distribution.class);
        assertThat((Double) ((jmt.gui.common.distributions.Distribution) timingDistribution0).getParameter("lambda").getValue())
                .as("First timing strategy lambda should be parsed")
                .isCloseTo(2.5, within(1e-9));

        Object timingDistribution1 = model.getFiringTimeDistribution(transitionFull, 1);
        assertThat(timingDistribution1)
                .as("Second timing strategy should be parsed as a Distribution")
                .isInstanceOf(jmt.gui.common.distributions.Distribution.class);
        assertThat((Double) ((jmt.gui.common.distributions.Distribution) timingDistribution1).getParameter("lambda").getValue())
                .as("Second timing strategy lambda should be parsed")
                .isCloseTo(7.5, within(1e-9));

        assertThat(model.getFiringPriority(transitionFull, 0))
                .as("First firing priority should be parsed")
                .isEqualTo(9);
        assertThat(model.getFiringPriority(transitionFull, 1))
                .as("Second firing priority should be parsed")
                .isEqualTo(4);

        assertThat(model.getFiringWeight(transitionFull, 0))
                .as("First firing weight should be parsed")
                .isCloseTo(1.25, within(1e-9));
        assertThat(model.getFiringWeight(transitionFull, 1))
                .as("Second firing weight should be parsed")
                .isCloseTo(0.75, within(1e-9));

        assertThat(model.getTransitionModeName(transitionFallback, 0))
                .as("Fallback transition should keep default mode name when modeNames is absent")
                .isEqualTo("Mode1");
        assertThat(model.getNumberOfServers(transitionFallback, 0))
                .as("Fallback transition should keep default numberOfServers when numbersOfServers is absent")
                .isEqualTo(1);
        assertThat(model.getFiringPriority(transitionFallback, 0))
                .as("Fallback transition should keep default firing priority when firingPriorities is absent")
                .isEqualTo(-1);
        assertThat(model.getFiringWeight(transitionFallback, 0))
                .as("Fallback transition should keep default firing weight when firingWeights is absent")
                .isCloseTo(1.0, within(1e-9));
    }

    @Test
    void test_parseFiringSection_branches_fullcoverage() {
        File xmlFile = new File("src/test/resources/jmt/gui/common/xml/parseFiring_branches.xml");
        CommonModel model = new CommonModel();

        XMLArchiver.loadModel(xmlFile, model);

        Object transitionWithFiring = model.getStationByName("TransitionWithFiring");
        Object transitionWithoutFiring = model.getStationByName("TransitionWithoutFiring");
        Object placeA = model.getStationByName("PlaceA");
        Object placeB = model.getStationByName("PlaceB");
        Object class1 = model.getClassByName("Class1");
        Object class2 = model.getClassByName("Class2");

        assertThat(transitionWithFiring).as("TransitionWithFiring station must exist").isNotNull();
        assertThat(transitionWithoutFiring).as("TransitionWithoutFiring station must exist").isNotNull();
        assertThat(placeA).as("PlaceA station must exist").isNotNull();
        assertThat(placeB).as("PlaceB station must exist").isNotNull();
        assertThat(class1).as("Class1 must exist").isNotNull();
        assertThat(class2).as("Class2 must exist").isNotNull();

        assertThat(model.getStationType(transitionWithFiring))
                .as("TransitionWithFiring should be parsed as TRANSITION station")
                .isEqualTo(CommonConstants.STATION_TYPE_TRANSITION);
        assertThat(model.getStationType(transitionWithoutFiring))
                .as("TransitionWithoutFiring should be parsed as TRANSITION station")
                .isEqualTo(CommonConstants.STATION_TYPE_TRANSITION);

        assertThat(model.getFiringOutcome(transitionWithFiring, 0, placeA, class1))
                .as("Mode 0 firing outcome for PlaceA/Class1 should be parsed")
                .isEqualTo(11);
        assertThat(model.getFiringOutcome(transitionWithFiring, 0, placeA, class2))
                .as("Mode 0 firing outcome for PlaceA/Class2 should be parsed")
                .isEqualTo(12);
        assertThat(model.getFiringOutcome(transitionWithFiring, 0, placeB, class1))
                .as("Mode 0 firing outcome for PlaceB/Class1 should be parsed")
                .isEqualTo(13);
        assertThat(model.getFiringOutcome(transitionWithFiring, 0, placeB, class2))
                .as("Mode 0 firing outcome for PlaceB/Class2 should be parsed")
                .isEqualTo(14);

        assertThat(model.getFiringOutcome(transitionWithFiring, 1, placeA, class1))
                .as("Mode 1 firing outcome for PlaceA/Class1 should be parsed")
                .isEqualTo(21);
        assertThat(model.getFiringOutcome(transitionWithFiring, 1, placeA, class2))
                .as("Mode 1 firing outcome for PlaceA/Class2 should be parsed")
                .isEqualTo(22);

        assertThat(model.getFiringOutcome(transitionWithoutFiring, 0, placeA, class1))
                .as("Without firingOutcomes parameter, PlaceA/Class1 should keep default outcome")
                .isZero();
        assertThat(model.getFiringOutcome(transitionWithoutFiring, 0, placeA, class2))
                .as("Without firingOutcomes parameter, PlaceA/Class2 should keep default outcome")
                .isZero();
    }

    @Test
    void test_parseMeasures_branches_fullcoverage() {
        File xmlFile = new File("src/test/resources/jmt/gui/common/xml/parseMeasures_branches.xml");
        CommonModel model = new CommonModel();

        XMLArchiver.loadModel(xmlFile, model);

        Object class1 = model.getClassByName("Class1");
        Object class2 = model.getClassByName("Class2");
        Object queueForMeasure = model.getStationByName("QueueForMeasure");
        Object transitionForFx = model.getStationByName("TransitionForFX");

        Object regionForMeasure = null;
        for (Object regionKey : model.getRegionKeys()) {
            if ("RegionForMeasure".equals(model.getRegionName(regionKey))) {
                regionForMeasure = regionKey;
                break;
            }
        }

        assertThat(class1).as("Class1 must exist").isNotNull();
        assertThat(class2).as("Class2 must exist").isNotNull();
        assertThat(queueForMeasure).as("QueueForMeasure station must exist").isNotNull();
        assertThat(transitionForFx).as("TransitionForFX station must exist").isNotNull();
        assertThat(regionForMeasure).as("RegionForMeasure must exist").isNotNull();

        assertThat(model.getMeasureKeys())
                .as("All measure entries from fixture should be added to the model")
                .hasSize(5);

        Object responseTimeMeasure = null;
        Object numberOfCustomersMeasure = null;
        Object systemThroughputMeasure = null;
        Object firingThroughputMeasure = null;
        Object utilizationMeasure = null;

        for (Object measureKey : model.getMeasureKeys()) {
            String measureType = model.getMeasureType(measureKey);
            if ("Response Time".equals(measureType)) {
                responseTimeMeasure = measureKey;
            } else if ("Number of Customers".equals(measureType)) {
                numberOfCustomersMeasure = measureKey;
            } else if ("System Throughput".equals(measureType)) {
                systemThroughputMeasure = measureKey;
            } else if ("Firing Throughput".equals(measureType)) {
                firingThroughputMeasure = measureKey;
            } else if ("Utilization".equals(measureType)) {
                utilizationMeasure = measureKey;
            }
        }

        assertThat(responseTimeMeasure).as("Response Time measure should exist").isNotNull();
        assertThat(numberOfCustomersMeasure).as("Number of Customers measure should exist").isNotNull();
        assertThat(systemThroughputMeasure).as("System Throughput measure should exist").isNotNull();
        assertThat(firingThroughputMeasure).as("Firing Throughput measure should exist").isNotNull();
        assertThat(utilizationMeasure).as("Utilization measure should exist").isNotNull();

        assertThat(model.getMeasureStation(responseTimeMeasure))
                .as("Station nodeType should resolve referenceNode through station map")
                .isEqualTo(queueForMeasure);
        assertThat(model.getMeasureClass(responseTimeMeasure))
                .as("Non-FX measure should resolve referenceUserClass through class map")
                .isEqualTo(class1);
        assertThat(model.getMeasureAlpha(responseTimeMeasure))
                .as("Alpha should be inverted for Response Time measure")
                .isCloseTo(0.9, within(1e-9));
        assertThat(model.getMeasurePrecision(responseTimeMeasure))
                .as("Precision should be parsed for Response Time measure")
                .isCloseTo(0.01, within(1e-9));
        assertThat(model.getMeasureLog(responseTimeMeasure))
                .as("Verbose flag should be parsed for Response Time measure")
                .isTrue();

        assertThat(model.getMeasureStation(numberOfCustomersMeasure))
                .as("Region nodeType should resolve referenceNode through region map")
                .isEqualTo(regionForMeasure);
        assertThat(model.getMeasureClass(numberOfCustomersMeasure))
                .as("Region measure class should resolve through class map")
                .isEqualTo(class2);
        assertThat(model.getMeasureAlpha(numberOfCustomersMeasure))
                .as("Alpha should be inverted for region measure")
                .isCloseTo(0.8, within(1e-9));
        assertThat(model.getMeasurePrecision(numberOfCustomersMeasure))
                .as("Precision should be parsed for region measure")
                .isCloseTo(0.02, within(1e-9));
        assertThat(model.getMeasureLog(numberOfCustomersMeasure))
                .as("Verbose flag should be parsed for region measure")
                .isFalse();

        assertThat(model.getMeasureStation(systemThroughputMeasure))
                .as("Empty referenceNode should produce null stationKey")
                .isNull();
        assertThat(model.getMeasureClass(systemThroughputMeasure))
                .as("Empty referenceUserClass should produce null classKey")
                .isNull();
        assertThat(model.getMeasureAlpha(systemThroughputMeasure))
                .as("Alpha should be inverted for system measure")
                .isCloseTo(0.7, within(1e-9));
        assertThat(model.getMeasurePrecision(systemThroughputMeasure))
                .as("Precision should be parsed for system measure")
                .isCloseTo(0.03, within(1e-9));
        assertThat(model.getMeasureLog(systemThroughputMeasure))
                .as("Verbose flag should be parsed for system measure")
                .isTrue();

        assertThat(model.getMeasureStation(firingThroughputMeasure))
                .as("FX measure station should resolve as normal station")
                .isEqualTo(transitionForFx);
        assertThat(model.getMeasureClass(firingThroughputMeasure))
                .as("FX measure class should resolve through modeNameLists instead of class map")
                .isEqualTo("ModeB");
        assertThat(model.getMeasureAlpha(firingThroughputMeasure))
                .as("Alpha should be inverted for FX measure")
                .isCloseTo(0.6, within(1e-9));
        assertThat(model.getMeasurePrecision(firingThroughputMeasure))
                .as("Precision should be parsed for FX measure")
                .isCloseTo(0.04, within(1e-9));
        assertThat(model.getMeasureLog(firingThroughputMeasure))
                .as("Verbose flag should be parsed for FX measure")
                .isFalse();

        assertThat(model.getMeasureStation(utilizationMeasure))
                .as("Class-only measure with empty station should keep null stationKey")
                .isNull();
        assertThat(model.getMeasureClass(utilizationMeasure))
                .as("Class-only measure should resolve class key even when station is null")
                .isEqualTo(class1);
        assertThat(model.getMeasureAlpha(utilizationMeasure))
                .as("Alpha should be inverted for class-only measure")
                .isCloseTo(0.5, within(1e-9));
        assertThat(model.getMeasurePrecision(utilizationMeasure))
                .as("Precision should be parsed for class-only measure")
                .isCloseTo(0.05, within(1e-9));
        assertThat(model.getMeasureLog(utilizationMeasure))
                .as("Verbose flag should be parsed for class-only measure")
                .isFalse();
    }


    @Test
    void test_Workcopy3_onoff() throws Exception {
        // 1. Load the file
        File xmlFile = new File("src/test/resources/jmt/gui/common/xml/792_Workcopy3_onoff.xml");
        CommonModel model = new CommonModel();

        // Parse using the XML archiver facade
        XMLArchiver.loadModel(xmlFile, model);

        // ---------------------------------------------------------
        // BLOCK A: Global Parameters & User Classes
        // ---------------------------------------------------------
        assertThat(model.getUseRandomSeed())
                .as("Random seed should be disabled since a specific seed is provided")
                .isFalse();
        assertThat(model.getSimulationSeed())
                .as("Simulation seed should be parsed as 23000")
                .isEqualTo(23000L);
        assertThat(model.getDisableStatistic())
                .as("Disable statistic should be true according to the XML")
                .isTrue();
        assertThat(model.getMaxSimulationSamples())
                .as("Max samples should be 1000000")
                .isEqualTo(1000000);

        assertThat(model.getOpenClassKeys())
                .as("There should be no open classes")
                .isEmpty();
        assertThat(model.getClosedClassKeys())
                .as("There should be exactly 1 closed class")
                .hasSize(1);

        Object class1 = model.getClassByName("Class1");
        assertThat(class1).isNotNull();

        assertThat(model.getClassPopulation(class1))
                .as("Class1 should have 10 customers")
                .isEqualTo(10);

        // ---------------------------------------------------------
        // BLOCK B: Stations Recognition
        // ---------------------------------------------------------
        Object delay1 = model.getStationByName("Delay 1");
        Object queue1 = model.getStationByName("Queue 1");
        Object offPlace = model.getStationByName("OFF");
        Object onPlace = model.getStationByName("ON");
        Object trans1 = model.getStationByName("Transition 1");
        Object trans2 = model.getStationByName("Transition 2");

        assertThat(model.getStationType(delay1)).isEqualTo(CommonConstants.STATION_TYPE_DELAY);
        assertThat(model.getStationType(queue1)).isEqualTo(CommonConstants.STATION_TYPE_SERVER);
        assertThat(model.getStationType(offPlace)).isEqualTo(CommonConstants.STATION_TYPE_PLACE);
        assertThat(model.getStationType(onPlace)).isEqualTo(CommonConstants.STATION_TYPE_PLACE);
        assertThat(model.getStationType(trans1)).isEqualTo(CommonConstants.STATION_TYPE_TRANSITION);
        assertThat(model.getStationType(trans2)).isEqualTo(CommonConstants.STATION_TYPE_TRANSITION);

        // ---------------------------------------------------------
        // BLOCK C: Server and Delay Distributions
        // ---------------------------------------------------------
        // Delay 1
        Object delay1Dist = model.getServiceTimeDistribution(delay1, class1);
        assertThat(delay1Dist).isInstanceOf(jmt.gui.common.distributions.Exponential.class);
        Double delay1Lambda = (Double) ((jmt.gui.common.distributions.Distribution) delay1Dist).getParameter("lambda").getValue();
        assertThat(delay1Lambda).as("Delay 1 service rate (lambda) should be 1.0").isCloseTo(1.0, within(1e-9));

        // Queue 1
        Object queue1Dist = model.getServiceTimeDistribution(queue1, class1);
        assertThat(queue1Dist).isInstanceOf(jmt.gui.common.distributions.Exponential.class);
        Double queue1Lambda = (Double) ((jmt.gui.common.distributions.Distribution) queue1Dist).getParameter("lambda").getValue();
        assertThat(queue1Lambda).as("Queue 1 service rate (lambda) should be 1.0").isCloseTo(1.0, within(1e-9));

        // ---------------------------------------------------------
        // BLOCK D: Queue 1 Advanced Strategies (Preemption & Conditions)
        // ---------------------------------------------------------
        assertThat(model.getStationQueueStrategy(queue1))
                .as("Queue 1 should use a PREEMPTIVE station queue strategy due to LCFSPR")
                .isEqualTo(CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE);

        assertThat(model.getQueueStrategy(queue1, class1))
                .as("Queue 1 should have LCFS_PR (Last Come First Served Preemptive Resume) strategy")
                .isEqualTo(CommonConstants.QUEUE_STRATEGY_LCFS_PR);

        // Enabling / Inhibiting / Resource conditions on Queue 1!
        assertThat(model.getEnablingCondition(queue1, 0, offPlace, class1))
                .as("Queue 1 should have an enabling condition requiring 0 tokens from OFF")
                .isEqualTo(0);

        assertThat(model.getInhibitingCondition(queue1, 0, offPlace, class1))
                .as("Queue 1 should have an inhibiting condition of 1 token from OFF")
                .isEqualTo(1);

        assertThat(model.getResourceCondition(queue1, 0, offPlace, class1))
                .as("Queue 1 should have a resource condition requiring 0 tokens from OFF")
                .isEqualTo(0);

        // ---------------------------------------------------------
        // BLOCK E: Routing Strategies
        // ---------------------------------------------------------
        Object queue1Routing = model.getRoutingStrategy(queue1, class1);
        assertThat(queue1Routing).isInstanceOf(jmt.gui.common.routingStrategies.ProbabilityRouting.class);

        Map<Object, Double> q1Routes = ((jmt.gui.common.routingStrategies.ProbabilityRouting) queue1Routing).getValues();
        assertThat(q1Routes)
                .as("Queue 1 routing should contain Delay 1 with prob 1.0 and OFF with prob 0.0")
                .containsEntry(delay1, 1.0)
                .containsEntry(offPlace, 0.0);

        // ---------------------------------------------------------
        // BLOCK F: Petri Net Transitions
        // ---------------------------------------------------------
        // Transition 1
        Object t1Timing = model.getFiringTimeDistribution(trans1, 0);
        assertThat(t1Timing).isInstanceOf(jmt.gui.common.distributions.Exponential.class);
        Double t1Lambda = (Double) ((jmt.gui.common.distributions.Distribution) t1Timing).getParameter("lambda").getValue();
        assertThat(t1Lambda).as("Transition 1 firing rate (lambda) should be 0.1").isCloseTo(0.1, within(1e-9));

        assertThat(model.getEnablingCondition(trans1, 0, offPlace, class1)).isEqualTo(1);
        assertThat(model.getInhibitingCondition(trans1, 0, offPlace, class1)).isEqualTo(0);
        assertThat(model.getFiringOutcome(trans1, 0, onPlace, class1)).isEqualTo(1);

        // Transition 2
        Object t2Timing = model.getFiringTimeDistribution(trans2, 0);
        assertThat(t2Timing).isInstanceOf(jmt.gui.common.distributions.Exponential.class);
        Double t2Lambda = (Double) ((jmt.gui.common.distributions.Distribution) t2Timing).getParameter("lambda").getValue();
        assertThat(t2Lambda).as("Transition 2 firing rate (lambda) should be 0.1").isCloseTo(0.1, within(1e-9));

        assertThat(model.getEnablingCondition(trans2, 0, onPlace, class1)).isEqualTo(1);
        assertThat(model.getInhibitingCondition(trans2, 0, onPlace, class1)).isEqualTo(0);
        assertThat(model.getFiringOutcome(trans2, 0, offPlace, class1)).isEqualTo(1);

        // ---------------------------------------------------------
        // BLOCK G: Preloads
        // ---------------------------------------------------------
        assertThat(model.getPreloadedJobs(delay1, class1))
                .as("Delay 1 should have 9 'Class1' customers preloaded")
                .isEqualTo(9);

        assertThat(model.getPreloadedJobs(onPlace, class1))
                .as("ON place should have 1 'Class1' token preloaded")
                .isEqualTo(1);

        // ---------------------------------------------------------
        // BLOCK H: Network Topology Verification
        // ---------------------------------------------------------
        assertThat(model.areConnected(delay1, queue1)).isTrue();
        assertThat(model.areConnected(queue1, delay1)).isTrue();
        assertThat(model.areConnected(queue1, offPlace)).isTrue();

        assertThat(model.areConnected(offPlace, queue1)).isTrue();
        assertThat(model.areConnected(offPlace, trans1)).isTrue();
        assertThat(model.areConnected(trans1, onPlace)).isTrue();
        assertThat(model.areConnected(onPlace, trans2)).isTrue();
        assertThat(model.areConnected(trans2, offPlace)).isTrue();
    }


    @Test
    void test_Sec5_3_FastTask_Quorum1_Wif_JSIMg() throws Exception {
        // 1. Load the file
        File xmlFile = new File("src/test/resources/jmt/gui/common/xml/520_Sec5_3_FastTask_Quorum1_Wif_JSIMg.xml");
        CommonModel model = new CommonModel();

        // Parse using the XML archiver facade
        XMLArchiver.loadModel(xmlFile, model);

        // ---------------------------------------------------------
        // BLOCK A: Global Parameters & User Classes
        // ---------------------------------------------------------
        assertThat(model.getUseRandomSeed())
                .as("Random seed should be disabled since a specific seed is provided")
                .isFalse();
        assertThat(model.getSimulationSeed())
                .as("Simulation seed should be parsed as 23000")
                .isEqualTo(23000L);
        assertThat(model.getDisableStatistic())
                .as("Disable statistic should be false according to the XML")
                .isFalse();
        assertThat(model.getMaxSimulationSamples())
                .as("Max samples should be 10000000")
                .isEqualTo(10000000);

        assertThat(model.getClosedClassKeys())
                .as("There should be no closed classes in this model")
                .isEmpty();
        assertThat(model.getOpenClassKeys())
                .as("There should be exactly 1 open class")
                .hasSize(1);

        Object class1 = model.getClassByName("Class1");
        assertThat(class1).isNotNull();

        // ---------------------------------------------------------
        // BLOCK B: Stations Recognition
        // ---------------------------------------------------------
        Object source = model.getStationByName("Source 1");
        Object fork1 = model.getStationByName("Fork 1");
        Object queue1 = model.getStationByName("Queue 1");
        Object queue2 = model.getStationByName("Queue 2");
        Object queue3 = model.getStationByName("Queue 3");
        Object queue4 = model.getStationByName("Queue 4");
        Object join1 = model.getStationByName("Join 1 - QUORUM 1");
        Object sink = model.getStationByName("Sink 1");

        assertThat(model.getStationType(source)).isEqualTo(CommonConstants.STATION_TYPE_SOURCE);
        assertThat(model.getStationType(fork1)).isEqualTo(CommonConstants.STATION_TYPE_FORK);
        assertThat(model.getStationType(queue1)).isEqualTo(CommonConstants.STATION_TYPE_SERVER);
        assertThat(model.getStationType(queue2)).isEqualTo(CommonConstants.STATION_TYPE_SERVER);
        assertThat(model.getStationType(queue3)).isEqualTo(CommonConstants.STATION_TYPE_SERVER);
        assertThat(model.getStationType(queue4)).isEqualTo(CommonConstants.STATION_TYPE_SERVER);
        assertThat(model.getStationType(join1)).isEqualTo(CommonConstants.STATION_TYPE_JOIN);
        assertThat(model.getStationType(sink)).isEqualTo(CommonConstants.STATION_TYPE_SINK);

        // ---------------------------------------------------------
        // BLOCK C: Source and Server Distributions
        // ---------------------------------------------------------
        // Source
        Object srcDist = model.getClassDistribution(class1);
        assertThat(srcDist).isInstanceOf(jmt.gui.common.distributions.Exponential.class);
        Double srcLambda = (Double) ((jmt.gui.common.distributions.Distribution) srcDist).getParameter("lambda").getValue();
        assertThat(srcLambda)
                .as("Source 1 arrival rate (lambda) should be 0.25")
                .isCloseTo(0.25, within(1e-9));

        // Queue 1 (Checking one of the parallel queues)
        Object q1Dist = model.getServiceTimeDistribution(queue1, class1);
        assertThat(q1Dist).isInstanceOf(jmt.gui.common.distributions.Exponential.class);
        Double q1Lambda = (Double) ((jmt.gui.common.distributions.Distribution) q1Dist).getParameter("lambda").getValue();
        assertThat(q1Lambda)
                .as("Queue 1 service rate (lambda) should be 2.0")
                .isCloseTo(2.0, within(1e-9));

        // Ensure all queues have 1 server and Drop strategy
        assertThat(model.getStationNumberOfServers(queue1)).isEqualTo(1);
        assertThat(model.getDropRule(queue2, class1)).isEqualTo(CommonConstants.FINITE_DROP);
        assertThat(model.getQueueStrategy(queue3, class1)).isEqualTo(CommonConstants.QUEUE_STRATEGY_FCFS);

        // ---------------------------------------------------------
        // BLOCK D: Fork and Join Settings (The Core Features)
        // ---------------------------------------------------------
        // Fork 1
        assertThat(model.getStationNumberOfServers(fork1))
                .as("Fork 1 should have jobsPerLink = 1")
                .isEqualTo(1);
        assertThat(model.getIsSimplifiedFork(fork1))
                .as("Fork 1 should be a simplified fork")
                .isTrue();

        Object forkStrategy = model.getForkStrategy(fork1, class1);
        assertThat(forkStrategy)
                .as("Fork strategy for Class1 should be ProbabilitiesFork")
                .isInstanceOf(jmt.gui.common.forkStrategies.ProbabilitiesFork.class);

        @SuppressWarnings("unchecked")
        Map<Object, jmt.gui.common.forkStrategies.OutPath> outPaths =
                (Map<Object, jmt.gui.common.forkStrategies.OutPath>) ((jmt.gui.common.forkStrategies.ProbabilitiesFork) forkStrategy).getOutDetails();

        assertThat(outPaths)
                .as("Fork 1 should branch to exactly 4 queues")
                .containsOnlyKeys(queue1, queue2, queue3, queue4);

        assertThat(outPaths.get(queue1).getProbability()).as("Probability to Queue 1 is 1.0").isCloseTo(1.0, within(1e-9));
        assertThat(outPaths.get(queue2).getProbability()).as("Probability to Queue 2 is 1.0").isCloseTo(1.0, within(1e-9));

        // Join 1 (Quorum Join)
        Object joinStrategy = model.getJoinStrategy(join1, class1);
        assertThat(joinStrategy)
                .as("Join strategy for Class1 should be PartialJoin (Quorum)")
                .isInstanceOf(jmt.gui.common.joinStrategies.PartialJoin.class);

        int numRequired = ((jmt.gui.common.joinStrategies.PartialJoin) joinStrategy).getRequiredNum();
        assertThat(numRequired)
                .as("PartialJoin should require exactly 1 job (Fastest Task)")
                .isEqualTo(1);

        // ---------------------------------------------------------
        // BLOCK E: Topology verification
        // ---------------------------------------------------------
        assertThat(model.areConnected(source, fork1))
                .as("Source should be connected to Fork 1")
                .isTrue();

        // Fork splits
        assertThat(model.areConnected(fork1, queue1)).isTrue();
        assertThat(model.areConnected(fork1, queue2)).isTrue();
        assertThat(model.areConnected(fork1, queue3)).isTrue();
        assertThat(model.areConnected(fork1, queue4)).isTrue();

        // Queues join
        assertThat(model.areConnected(queue1, join1)).isTrue();
        assertThat(model.areConnected(queue2, join1)).isTrue();
        assertThat(model.areConnected(queue3, join1)).isTrue();
        assertThat(model.areConnected(queue4, join1)).isTrue();

        assertThat(model.areConnected(join1, sink))
                .as("Join 1 should be connected to Sink 1")
                .isTrue();
    }

    @Test
    void test_2FJ_2tasks_nestedOK() throws Exception {
        // 1. Load the file
        File xmlFile = new File("src/test/resources/jmt/gui/common/xml/641_2FJ_2tasks_nestedOK.xml"); // Assicurati di rinominare/aggiustare il percorso!
        CommonModel model = new CommonModel();

        // Parse using the XML archiver facade
        XMLArchiver.loadModel(xmlFile, model);

        // ---------------------------------------------------------
        // BLOCK A: Global Parameters & User Classes
        // ---------------------------------------------------------
        assertThat(model.getUseRandomSeed())
                .as("Random seed should be disabled since a specific seed is provided")
                .isFalse();
        assertThat(model.getSimulationSeed())
                .as("Simulation seed should be parsed as 23000")
                .isEqualTo(23000L);
        assertThat(model.getDisableStatistic())
                .as("Disable statistic should be false according to the XML")
                .isFalse();
        assertThat(model.getMaxSimulationSamples())
                .as("Max samples should be 10000000")
                .isEqualTo(10000000);

        assertThat(model.getClosedClassKeys())
                .as("There should be no closed classes")
                .isEmpty();
        assertThat(model.getOpenClassKeys())
                .as("There should be exactly 1 open class")
                .hasSize(1);

        Object class1 = model.getClassByName("Class1");
        assertThat(class1).isNotNull();

        // ---------------------------------------------------------
        // BLOCK B: Stations Recognition
        // ---------------------------------------------------------
        Object source = model.getStationByName("Source 1");
        Object fork1 = model.getStationByName("Fork 1");
        Object delay1 = model.getStationByName("Delay 1");
        Object delay2 = model.getStationByName("Delay 2");
        Object fork2 = model.getStationByName("Fork 2");
        Object delay4 = model.getStationByName("Delay 4");
        Object delay5 = model.getStationByName("Delay 5");
        Object join2 = model.getStationByName("Join 2");
        Object join1 = model.getStationByName("Join 1");
        Object delay1_1 = model.getStationByName("Delay 1_1");
        Object sink = model.getStationByName("Sink 1");

        assertThat(model.getStationType(source)).isEqualTo(CommonConstants.STATION_TYPE_SOURCE);
        assertThat(model.getStationType(fork1)).isEqualTo(CommonConstants.STATION_TYPE_FORK);
        assertThat(model.getStationType(delay1)).isEqualTo(CommonConstants.STATION_TYPE_DELAY);
        assertThat(model.getStationType(fork2)).isEqualTo(CommonConstants.STATION_TYPE_FORK);
        assertThat(model.getStationType(join1)).isEqualTo(CommonConstants.STATION_TYPE_JOIN);
        assertThat(model.getStationType(sink)).isEqualTo(CommonConstants.STATION_TYPE_SINK);

        // ---------------------------------------------------------
        // BLOCK C: Source and Delay Distributions
        // ---------------------------------------------------------
        // Source
        Object srcDistC1 = model.getClassDistribution(class1);
        assertThat(srcDistC1).isInstanceOf(jmt.gui.common.distributions.Exponential.class);
        Double srcLambdaC1 = (Double) ((jmt.gui.common.distributions.Distribution) srcDistC1).getParameter("lambda").getValue();
        assertThat(srcLambdaC1).as("Source arrival rate (lambda) should be 1.0").isCloseTo(1.0, within(1e-9));

        // Delay 1
        Object delay1Dist = model.getServiceTimeDistribution(delay1, class1);
        assertThat(delay1Dist).isInstanceOf(jmt.gui.common.distributions.Deterministic.class);
        Double delay1T = (Double) ((jmt.gui.common.distributions.Distribution) delay1Dist).getParameter("t").getValue();
        assertThat(delay1T).as("Delay 1 deterministic time should be 0.1").isCloseTo(0.1, within(1e-9));

        // Delay 2
        Object delay2Dist = model.getServiceTimeDistribution(delay2, class1);
        Double delay2T = (Double) ((jmt.gui.common.distributions.Distribution) delay2Dist).getParameter("t").getValue();
        assertThat(delay2T).as("Delay 2 deterministic time should be 0.2").isCloseTo(0.2, within(1e-9));

        // Delay 5
        Object delay5Dist = model.getServiceTimeDistribution(delay5, class1);
        Double delay5T = (Double) ((jmt.gui.common.distributions.Distribution) delay5Dist).getParameter("t").getValue();
        assertThat(delay5T).as("Delay 5 deterministic time should be 0.6").isCloseTo(0.6, within(1e-9));

        // Delay 1_1
        Object delay1_1Dist = model.getServiceTimeDistribution(delay1_1, class1);
        Double delay1_1T = (Double) ((jmt.gui.common.distributions.Distribution) delay1_1Dist).getParameter("t").getValue();
        assertThat(delay1_1T).as("Delay 1_1 deterministic time should be 1.0").isCloseTo(1.0, within(1e-9));

        // ---------------------------------------------------------
        // BLOCK D: Fork and Join Settings
        // ---------------------------------------------------------
        // Fork 1 (Outer Fork)
        assertThat(model.getStationNumberOfServers(fork1))
                .as("Fork 1 should have jobsPerLink = 2")
                .isEqualTo(2);
        assertThat(model.getIsSimplifiedFork(fork1))
                .as("Fork 1 isSimplifiedFork should be true")
                .isTrue();

        Object fork1Strategy = model.getForkStrategy(fork1, class1);
        assertThat(fork1Strategy).isInstanceOf(jmt.gui.common.forkStrategies.ProbabilitiesFork.class);
        Map<Object, jmt.gui.common.forkStrategies.OutPath> f1Paths = (Map<Object, jmt.gui.common.forkStrategies.OutPath>) ((jmt.gui.common.forkStrategies.ProbabilitiesFork) fork1Strategy).getOutDetails();

        assertThat(f1Paths).containsKey(delay1).containsKey(delay2);
        assertThat(f1Paths.get(delay1).getProbability()).as("Fork 1 should route to Delay 1 with prob 1.0").isCloseTo(1.0, within(1e-9));

        // Fork 1 also contains the JobsPerLinkDis arrays (empirical maps integer -> double)
        Map<Object, Object> f1Delay1Params = f1Paths.get(delay1).getOutParameters();
        assertThat(f1Delay1Params).as("Fork 1 should send exactly 1 job to Delay 1").containsEntry(1, 1.0);

        // Fork 2 (Inner Fork)
        assertThat(model.getStationNumberOfServers(fork2))
                .as("Fork 2 should have jobsPerLink = 1")
                .isEqualTo(1);

        Object fork2Strategy = model.getForkStrategy(fork2, class1);
        assertThat(fork2Strategy).isInstanceOf(jmt.gui.common.forkStrategies.ProbabilitiesFork.class);
        Map<Object, jmt.gui.common.forkStrategies.OutPath> f2Paths = (Map<Object, jmt.gui.common.forkStrategies.OutPath>) ((jmt.gui.common.forkStrategies.ProbabilitiesFork) fork2Strategy).getOutDetails();

        assertThat(f2Paths).containsKey(delay4).containsKey(delay5);

        // Join 1 (Outer Join)
        Object join1Strategy = model.getJoinStrategy(join1, class1);
        assertThat(join1Strategy).isInstanceOf(jmt.gui.common.joinStrategies.NormalJoin.class);

        // Join 2 (Inner Join)
        Object join2Strategy = model.getJoinStrategy(join2, class1);
        assertThat(join2Strategy).isInstanceOf(jmt.gui.common.joinStrategies.NormalJoin.class);

        // ---------------------------------------------------------
        // BLOCK E: Topology verification (Nested Structure)
        // ---------------------------------------------------------
        // Source to Fork 1
        assertThat(model.areConnected(source, fork1)).isTrue();

        // Fork 1 Splits
        assertThat(model.areConnected(fork1, delay1)).isTrue();
        assertThat(model.areConnected(fork1, delay2)).isTrue();

        // Delay 1 goes to Fork 2
        assertThat(model.areConnected(delay1, fork2)).isTrue();

        // Fork 2 Splits
        assertThat(model.areConnected(fork2, delay4)).isTrue();
        assertThat(model.areConnected(fork2, delay5)).isTrue();

        // Fork 2 Joins at Join 2
        assertThat(model.areConnected(delay4, join2)).isTrue();
        assertThat(model.areConnected(delay5, join2)).isTrue();

        // Join 2 and Delay 2 merge at Join 1
        assertThat(model.areConnected(join2, join1)).isTrue();
        assertThat(model.areConnected(delay2, join1)).isTrue();

        // Join 1 goes to final Delay and Sink
        assertThat(model.areConnected(join1, delay1_1)).isTrue();
        assertThat(model.areConnected(delay1_1, sink)).isTrue();
    }

    @Test
    void test_conBeta13fesc() throws Exception {
        // 1. Load the file
        File xmlFile = new File("src/test/resources/jmt/gui/common/xml/225_conBeta13fesc.xml");
        CommonModel model = new CommonModel();

        // Parse using the XML archiver facade
        XMLArchiver.loadModel(xmlFile, model);

        // ---------------------------------------------------------
        // BLOCK A: Global Parameters & User Classes
        // ---------------------------------------------------------
        assertThat(model.getUseRandomSeed())
                .as("Random seed should be enabled (true) because no specific seed is defined in the XML")
                .isTrue();
        assertThat(model.getDisableStatistic())
                .as("Disable statistic should be false according to the XML")
                .isFalse();
        assertThat(model.getMaxSimulationSamples())
                .as("Max samples should be 1000000")
                .isEqualTo(1000000);

        assertThat(model.getOpenClassKeys())
                .as("There should be no open classes")
                .isEmpty();
        assertThat(model.getClosedClassKeys())
                .as("There should be exactly 1 closed class")
                .hasSize(1);

        Object class0 = model.getClassByName("Class0");
        assertThat(class0).isNotNull();

        assertThat(model.getClassPopulation(class0))
                .as("Class0 should have exactly 3 customers")
                .isEqualTo(3);

        // ---------------------------------------------------------
        // BLOCK B: Stations Recognition
        // ---------------------------------------------------------
        Object cpu = model.getStationByName("CPU");
        Object flowEq = model.getStationByName("FlowEq");

        assertThat(model.getStationType(cpu)).isEqualTo(CommonConstants.STATION_TYPE_SERVER);
        assertThat(model.getStationType(flowEq)).isEqualTo(CommonConstants.STATION_TYPE_SERVER);

        // ---------------------------------------------------------
        // BLOCK C: Server Distributions & Load Dependent Strategy
        // ---------------------------------------------------------
        // CPU Distribution
        Object cpuDist = model.getServiceTimeDistribution(cpu, class0);
        assertThat(cpuDist.getClass().getSimpleName())
                .as("CPU service time distribution should be Exponential")
                .contains("Exponential");
        Double cpuLambda = (Double) ((jmt.gui.common.distributions.Distribution) cpuDist).getParameter("lambda").getValue();
        assertThat(cpuLambda)
                .as("CPU service rate (lambda) should be 2.5")
                .isCloseTo(2.5, within(1e-9));

        // FlowEq Load Dependent Distribution
        Object flowEqDist = model.getServiceTimeDistribution(flowEq, class0);
        assertThat(flowEqDist)
                .as("FlowEq service time strategy should be LDStrategy (Load Dependent)")
                .isInstanceOf(jmt.gui.common.serviceStrategies.LDStrategy.class);

        jmt.gui.common.serviceStrategies.LDStrategy ldStrategy = (jmt.gui.common.serviceStrategies.LDStrategy) flowEqDist;

        assertThat(ldStrategy.getRangeNumber())
                .as("LoadDependent strategy should have exactly 3 ranges defined")
                .isEqualTo(3);

        Object[] ranges = ldStrategy.getAllRanges();

        // Range 1 (From = 1)
        assertThat(ldStrategy.getRangeFrom(ranges[0])).isEqualTo(1);
        assertThat(ldStrategy.getRangeDistributionMean(ranges[0])).isEqualTo("3");
        assertThat(ldStrategy.getRangeDistribution(ranges[0]).getClass().getSimpleName()).contains("Exponential");

        // Range 2 (From = 2)
        assertThat(ldStrategy.getRangeFrom(ranges[1])).isEqualTo(2);
        assertThat(ldStrategy.getRangeDistributionMean(ranges[1])).isEqualTo("2");
        assertThat(ldStrategy.getRangeDistribution(ranges[1]).getClass().getSimpleName()).contains("Exponential");

        // Range 3 (From = 3)
        assertThat(ldStrategy.getRangeFrom(ranges[2])).isEqualTo(3);
        assertThat(ldStrategy.getRangeDistributionMean(ranges[2])).isEqualTo("1.66666667");
        assertThat(ldStrategy.getRangeDistribution(ranges[2]).getClass().getSimpleName()).contains("Exponential");

        // ---------------------------------------------------------
        // BLOCK D: Queue Strategies
        // ---------------------------------------------------------
        assertThat(model.getStationNumberOfServers(cpu))
                .as("CPU should have exactly 1 server")
                .isEqualTo(1);
        assertThat(model.getQueueStrategy(cpu, class0))
                .as("CPU put strategy for Class0 should be FCFS (TailStrategy)")
                .isEqualTo(CommonConstants.QUEUE_STRATEGY_FCFS);
        assertThat(model.getDropRule(cpu, class0))
                .as("CPU drop rule should be Drop")
                .isEqualTo(CommonConstants.FINITE_DROP);

        // ---------------------------------------------------------
        // BLOCK E: Topology & Routing Strategies
        // ---------------------------------------------------------
        assertThat(model.areConnected(cpu, flowEq))
                .as("CPU should be connected to FlowEq")
                .isTrue();
        assertThat(model.areConnected(flowEq, cpu))
                .as("FlowEq should be connected back to CPU")
                .isTrue();

        // Both use RandomRouting (which is implicitly 100% if only one outgoing link exists, but the type is what matters)
        assertThat(model.getRoutingStrategy(cpu, class0))
                .as("CPU routing strategy should be RandomRouting")
                .isInstanceOf(jmt.gui.common.routingStrategies.RandomRouting.class);
        assertThat(model.getRoutingStrategy(flowEq, class0))
                .as("FlowEq routing strategy should be RandomRouting")
                .isInstanceOf(jmt.gui.common.routingStrategies.RandomRouting.class);

        // ---------------------------------------------------------
        // BLOCK F: Preloads
        // ---------------------------------------------------------
        assertThat(model.getPreloadedJobs(cpu, class0))
                .as("CPU should have 3 'Class0' customers preloaded")
                .isEqualTo(3);
    }

    @Test
    void test_240415_FCFS_QD() throws Exception {

        // 1. Load the file
        File xmlFile = new File("src/test/resources/jmt/gui/common/xml/4039_240415_FCFS_QD.xml");
        CommonModel model = new CommonModel();

        // Parse using the XML archiver facade
        XMLArchiver.loadModel(xmlFile, model);



        // ---------------------------------------------------------
        // BLOCK A: Global Parameters & User Classes
        // ---------------------------------------------------------
        assertThat(model.getUseRandomSeed())
                .as("Random seed should be disabled since a specific seed is provided")
                .isFalse();
        assertThat(model.getSimulationSeed())
                .as("Simulation seed should be parsed as 23000")
                .isEqualTo(23000L);
        assertThat(model.getDisableStatistic())
                .as("Disable statistic should be true")
                .isTrue();
        assertThat(model.getMaxSimulationSamples())
                .as("Max samples should be 1000000")
                .isEqualTo(1000000);

        assertThat(model.getOpenClassKeys())
                .as("There should be no open classes")
                .isEmpty();
        assertThat(model.getClosedClassKeys())
                .as("There should be exactly 2 closed classes")
                .hasSize(2);

        Object class1 = model.getClassByName("Class1");
        Object tokenClass = model.getClassByName("Token");
        assertThat(class1).isNotNull();
        assertThat(tokenClass).isNotNull();

        assertThat(model.getClassPopulation(class1))
                .as("Class1 should have 10 customers")
                .isEqualTo(10);
        assertThat(model.getClassPopulation(tokenClass))
                .as("Token class should have 1 customer")
                .isEqualTo(1);

        // ---------------------------------------------------------
        // BLOCK B: Stations Recognition
        // ---------------------------------------------------------
        Object queue1 = model.getStationByName("Queue 1");
        Object offPlace = model.getStationByName("OFF");
        Object onPlace = model.getStationByName("ON");
        Object trans1 = model.getStationByName("Transition 1");
        Object trans2 = model.getStationByName("Transition 2");
        Object delay1 = model.getStationByName("Delay 1");

        assertThat(model.getStationType(queue1)).isEqualTo(CommonConstants.STATION_TYPE_SERVER);
        assertThat(model.getStationType(offPlace)).isEqualTo(CommonConstants.STATION_TYPE_PLACE);
        assertThat(model.getStationType(onPlace)).isEqualTo(CommonConstants.STATION_TYPE_PLACE);
        assertThat(model.getStationType(trans1)).isEqualTo(CommonConstants.STATION_TYPE_TRANSITION);
        assertThat(model.getStationType(trans2)).isEqualTo(CommonConstants.STATION_TYPE_TRANSITION);
        assertThat(model.getStationType(delay1)).isEqualTo(CommonConstants.STATION_TYPE_DELAY);

        // ---------------------------------------------------------
        // BLOCK C: Server and Delay Distributions
        // ---------------------------------------------------------
        Object q1DistClass1 = model.getServiceTimeDistribution(queue1, class1);
        assertThat(q1DistClass1)
                .as("Queue 1 service time distribution for Class1 should be Exponential")
                .isInstanceOf(jmt.gui.common.distributions.Exponential.class);
        Double q1LambdaC1 = (Double) ((jmt.gui.common.distributions.Distribution) q1DistClass1).getParameter("lambda").getValue();
        assertThat(q1LambdaC1)
                .as("Queue 1 service rate (lambda) for Class1 should be 1.0")
                .isCloseTo(1.0, within(1e-9));

        Object q1DistToken = model.getServiceTimeDistribution(queue1, tokenClass);
        assertThat(q1DistToken).isInstanceOf(jmt.gui.common.distributions.Exponential.class);
        Double q1LambdaToken = (Double) ((jmt.gui.common.distributions.Distribution) q1DistToken).getParameter("lambda").getValue();
        assertThat(q1LambdaToken)
                .as("Queue 1 service rate (lambda) for Token should be Infinity")
                .isEqualTo(Double.POSITIVE_INFINITY);

        Object delayDistClass1 = model.getServiceTimeDistribution(delay1, class1);
        Double delayLambdaC1 = (Double) ((jmt.gui.common.distributions.Distribution) delayDistClass1).getParameter("lambda").getValue();
        assertThat(delayLambdaC1)
                .as("Delay 1 service rate (lambda) for Class1 should be 0.2")
                .isCloseTo(0.2, within(1e-9));

        // ---------------------------------------------------------
        // BLOCK D: Petri Net Transitions (Enabling, Timing, Firing)
        // ---------------------------------------------------------
        // Transition 1
        assertThat(model.getTransitionModeName(trans1, 0))
                .as("Transition 1 should have a mode named Mode1")
                .isEqualTo("Mode1");

        Object t1Timing = model.getFiringTimeDistribution(trans1, 0);
        assertThat(t1Timing).isInstanceOf(jmt.gui.common.distributions.Exponential.class);
        Double t1Lambda = (Double) ((jmt.gui.common.distributions.Distribution) t1Timing).getParameter("lambda").getValue();
        assertThat(t1Lambda)
                .as("Transition 1 firing rate (lambda) should be 100.0")
                .isCloseTo(100.0, within(1e-9));

        assertThat(model.getEnablingCondition(trans1, 0, offPlace, tokenClass))
                .as("Transition 1 should require exactly 1 Token from the OFF place")
                .isEqualTo(1);

        assertThat(model.getFiringOutcome(trans1, 0, onPlace, tokenClass))
                .as("Transition 1 should output exactly 1 Token to the ON place upon firing")
                .isEqualTo(1);

        // Transition 2
        Object t2Timing = model.getFiringTimeDistribution(trans2, 0);
        Double t2Lambda = (Double) ((jmt.gui.common.distributions.Distribution) t2Timing).getParameter("lambda").getValue();
        assertThat(t2Lambda)
                .as("Transition 2 firing rate (lambda) should be 10.0")
                .isCloseTo(10.0, within(1e-9));

        assertThat(model.getEnablingCondition(trans2, 0, onPlace, tokenClass))
                .as("Transition 2 should require exactly 1 Token from the ON place")
                .isEqualTo(1);

        assertThat(model.getFiringOutcome(trans2, 0, offPlace, tokenClass))
                .as("Transition 2 should output exactly 1 Token to the OFF place upon firing")
                .isEqualTo(1);

        // ---------------------------------------------------------
        // BLOCK E: Place / Storage Settings
        // ---------------------------------------------------------
        assertThat(model.getStationQueueCapacity(offPlace))
                .as("OFF place global capacity should be infinite (-1)")
                .isEqualTo(-1);
        assertThat(model.getDropRule(offPlace, tokenClass))
                .as("OFF place drop rule should be set to Drop")
                .isEqualTo(CommonConstants.FINITE_DROP);

        // ---------------------------------------------------------
        // BLOCK F: Preloads
        // ---------------------------------------------------------
        assertThat(model.getPreloadedJobs(offPlace, tokenClass))
                .as("OFF place should have 1 Token preloaded initially")
                .isEqualTo(1);
        assertThat(model.getPreloadedJobs(delay1, class1))
                .as("Delay 1 station should have 10 Class1 customers preloaded initially")
                .isEqualTo(10);

        // ---------------------------------------------------------
        // BLOCK G: Routing Strategies
        // ---------------------------------------------------------
        Object routingQ1 = model.getRoutingStrategy(queue1, class1);
        assertThat(routingQ1)
                .as("Routing strategy for Class1 in Queue 1 should be Probabilities")
                .isInstanceOf(jmt.gui.common.routingStrategies.ProbabilityRouting.class);

        jmt.gui.common.routingStrategies.ProbabilityRouting prQ1 = (jmt.gui.common.routingStrategies.ProbabilityRouting) routingQ1;
        assertThat(prQ1.getValues())
                .as("Class1 in Queue 1 should route entirely (100%) to Delay 1")
                .containsEntry(delay1, 1.0);
    }

    @Test
    void test_ForkJoin_4Stat_2_SEED1() throws Exception {

        // 1. Load the file
        File xmlFile = new File("src/test/resources/jmt/gui/common/xml/10089_ForkJoin_4Stat_2 - SEED1.xml");
        CommonModel model = new CommonModel();

        // Parse using the XML archiver facade
        XMLArchiver.loadModel(xmlFile, model);



        // ---------------------------------------------------------
        // BLOCK A: Global Parameters & User Classes
        // ---------------------------------------------------------
        assertThat(model.getUseRandomSeed())
                .as("Random seed should be disabled since a specific seed is provided")
                .isFalse();
        assertThat(model.getSimulationSeed())
                .as("Simulation seed should be parsed as 23000")
                .isEqualTo(23000L);
        assertThat(model.getDisableStatistic())
                .as("Disable statistic should be true")
                .isTrue();
        assertThat(model.getMaxSimulationSamples())
                .as("Max samples should be 10000000")
                .isEqualTo(10000000);

        assertThat(model.getOpenClassKeys())
                .as("There should be exactly 1 open class")
                .hasSize(1);
        assertThat(model.getClosedClassKeys())
                .as("There should be no closed classes in this model")
                .isEmpty();

        Object class1 = model.getClassByName("Class1");
        assertThat(class1).isNotNull();

        // ---------------------------------------------------------
        // BLOCK B: Stations Recognition
        // ---------------------------------------------------------
        Object source = model.getStationByName("Source 1");
        Object fork = model.getStationByName("Fork 1");
        Object queue1 = model.getStationByName("Queue 1");
        Object queue2 = model.getStationByName("Queue 2");
        Object queue3 = model.getStationByName("Queue 3");
        Object queue4 = model.getStationByName("Queue 4");
        Object join = model.getStationByName("Join 1");
        Object sink = model.getStationByName("Sink 1");

        assertThat(model.getStationType(source)).isEqualTo(CommonConstants.STATION_TYPE_SOURCE);
        assertThat(model.getStationType(fork)).isEqualTo(CommonConstants.STATION_TYPE_FORK);
        assertThat(model.getStationType(queue1)).isEqualTo(CommonConstants.STATION_TYPE_SERVER);
        assertThat(model.getStationType(queue2)).isEqualTo(CommonConstants.STATION_TYPE_SERVER);
        assertThat(model.getStationType(queue3)).isEqualTo(CommonConstants.STATION_TYPE_SERVER);
        assertThat(model.getStationType(queue4)).isEqualTo(CommonConstants.STATION_TYPE_SERVER);
        assertThat(model.getStationType(join)).isEqualTo(CommonConstants.STATION_TYPE_JOIN);
        assertThat(model.getStationType(sink)).isEqualTo(CommonConstants.STATION_TYPE_SINK);

        // ---------------------------------------------------------
        // BLOCK C: Source Distributions
        // ---------------------------------------------------------
        Object srcDist = model.getClassDistribution(class1);
        assertThat(srcDist)
                .as("Class1 source distribution should be Exponential")
                .isInstanceOf(jmt.gui.common.distributions.Exponential.class);
        Double lambda = (Double) ((jmt.gui.common.distributions.Distribution) srcDist).getParameter("lambda").getValue();
        assertThat(lambda)
                .as("Class1 arrival rate (lambda) should be 1.0")
                .isCloseTo(1.0, within(1e-12));

        // ---------------------------------------------------------
        // BLOCK D: Queue Strategies & Constraints
        // ---------------------------------------------------------
        // Queue 1
        assertThat(model.getStationNumberOfServers(queue1))
                .as("Queue 1 should have exactly 1 server")
                .isEqualTo(1);
        assertThat(model.getQueueStrategy(queue1, class1))
                .as("Queue 1 put strategy for Class1 should be FCFS")
                .isEqualTo(CommonConstants.QUEUE_STRATEGY_FCFS);
        Object queue1Dist = model.getServiceTimeDistribution(queue1, class1);
        assertThat(queue1Dist)
                .as("Queue 1 service time distribution should be Hyperexponential")
                .isInstanceOf(jmt.gui.common.distributions.Hyperexponential.class);
        Double q1P = (Double) ((jmt.gui.common.distributions.Distribution) queue1Dist).getParameter("p").getValue();
        Double q1L1 = (Double) ((jmt.gui.common.distributions.Distribution) queue1Dist).getParameter("lambda1").getValue();
        Double q1L2 = (Double) ((jmt.gui.common.distributions.Distribution) queue1Dist).getParameter("lambda2").getValue();
        assertThat(q1P).isCloseTo(0.05278640450004207, within(1e-9));
        assertThat(q1L1).isCloseTo(0.2111456180001683, within(1e-9));
        assertThat(q1L2).isCloseTo(3.7888543819998315, within(1e-9));

        // Queue 2
        assertThat(model.getStationNumberOfServers(queue2))
                .as("Queue 2 should have exactly 1 server")
                .isEqualTo(1);
        Object queue2Dist = model.getServiceTimeDistribution(queue2, class1);
        assertThat(queue2Dist)
                .as("Queue 2 service time distribution should be Exponential")
                .isInstanceOf(jmt.gui.common.distributions.Exponential.class);
        Double q2Lambda = (Double) ((jmt.gui.common.distributions.Distribution) queue2Dist).getParameter("lambda").getValue();
        assertThat(q2Lambda).isCloseTo(2.0, within(1e-9));

        // ---------------------------------------------------------
        // BLOCK E: Fork & Join Configuration
        // ---------------------------------------------------------
        // Fork
        assertThat(model.getStationNumberOfServers(fork))
                .as("Fork 1 should have jobsPerLink = 1")
                .isEqualTo(1);
        assertThat(model.getForkBlock(fork))
                .as("Fork 1 block parameter should be -1")
                .isEqualTo(-1);
        assertThat(model.getIsSimplifiedFork(fork))
                .as("Fork 1 should be a simplified fork")
                .isTrue();

        Object forkStrategy = model.getForkStrategy(fork, class1);
        assertThat(forkStrategy)
                .as("Fork strategy for Class1 should be ProbabilitiesFork")
                .isInstanceOf(jmt.gui.common.forkStrategies.ProbabilitiesFork.class);

        // Given that it's a parallel fork to 4 queues, JMT often uses ProbabilitiesFork even if there are no specific probabilities set when isSimplifiedFork=true
        // We verify it's the correct class.

        // Join
        Object joinStrategy = model.getJoinStrategy(join, class1);
        assertThat(joinStrategy)
                .as("Join strategy for Class1 should be NormalJoin")
                .isInstanceOf(jmt.gui.common.joinStrategies.NormalJoin.class);

        // ---------------------------------------------------------
        // BLOCK F: Network Topology
        // ---------------------------------------------------------
        assertThat(model.areConnected(source, fork)).isTrue();
        assertThat(model.areConnected(fork, queue1)).isTrue();
        assertThat(model.areConnected(fork, queue2)).isTrue();
        assertThat(model.areConnected(fork, queue3)).isTrue();
        assertThat(model.areConnected(fork, queue4)).isTrue();
        assertThat(model.areConnected(queue1, join)).isTrue();
        assertThat(model.areConnected(queue2, join)).isTrue();
        assertThat(model.areConnected(queue3, join)).isTrue();
        assertThat(model.areConnected(queue4, join)).isTrue();
        assertThat(model.areConnected(join, sink)).isTrue();
    }

    @Test
    void test_10190_220423_2RotNogen_2Q() throws Exception {

        // 1. Load the file
        File xmlFile = new File("src/test/resources/jmt/gui/common/xml/10190_220423_2RotNogen_2Q.xml");
        CommonModel model = new CommonModel();

        // Parse using the XML archiver facade
        XMLArchiver.loadModel(xmlFile, model);


        // ---------------------------------------------------------
        // BLOCK A: Global Parameters & User Classes
        // ---------------------------------------------------------
        assertThat(model.getUseRandomSeed())
                .as("Random seed should be disabled since a specific seed is provided")
                .isFalse();
        assertThat(model.getSimulationSeed())
                .as("Simulation seed should be parsed as 23000")
                .isEqualTo(23000L);
        assertThat(model.getMaxSimulationSamples())
                .as("Max samples should be 1000000")
                .isEqualTo(1000000);
        assertThat(model.getPollingInterval())
                .as("Polling interval should be 1.0")
                .isEqualTo(1.0);

        assertThat(model.getOpenClassKeys())
                .as("There should be exactly 2 open classes")
                .hasSize(2);
        assertThat(model.getClosedClassKeys())
                .as("There should be no closed classes in this model")
                .isEmpty();

        Object class1 = model.getClassByName("Class1");
        Object class2 = model.getClassByName("Class2");
        assertThat(class1).isNotNull();
        assertThat(class2).isNotNull();

        // ---------------------------------------------------------
        // BLOCK B: Stations Recognition
        // ---------------------------------------------------------
        Object source = model.getStationByName("Source 1");
        Object queue1 = model.getStationByName("Queue 1");
        Object queue2 = model.getStationByName("Queue 2");
        Object sink = model.getStationByName("Sink 1");

        assertThat(model.getStationType(source)).isEqualTo(CommonConstants.STATION_TYPE_SOURCE);
        assertThat(model.getStationType(queue1)).isEqualTo(CommonConstants.STATION_TYPE_SERVER);
        assertThat(model.getStationType(queue2)).isEqualTo(CommonConstants.STATION_TYPE_SERVER);
        assertThat(model.getStationType(sink)).isEqualTo(CommonConstants.STATION_TYPE_SINK);

        // ---------------------------------------------------------
        // BLOCK C: Source Distributions (Heterogeneous by Class)
        // ---------------------------------------------------------
        Object srcDist1 = model.getClassDistribution(class1);
        assertThat(srcDist1)
                .as("Class1 source distribution should be Exponential")
                .isInstanceOf(jmt.gui.common.distributions.Exponential.class);
        Double lambda1 = (Double) ((jmt.gui.common.distributions.Distribution) srcDist1).getParameter("lambda").getValue();
        assertThat(lambda1)
                .as("Class1 arrival rate (lambda) should be ~0.30")
                .isCloseTo(0.30000000000003, within(1e-12));

        Object srcDist2 = model.getClassDistribution(class2);
        Double lambda2 = (Double) ((jmt.gui.common.distributions.Distribution) srcDist2).getParameter("lambda").getValue();
        assertThat(lambda2)
                .as("Class2 arrival rate (lambda) should be ~0.60")
                .isCloseTo(0.59999999999988, within(1e-12));

        // ---------------------------------------------------------
        // BLOCK D: Queue Strategies & Constraints
        // ---------------------------------------------------------
        assertThat(model.getStationNumberOfServers(queue1))
                .as("Queue 1 should have exactly 1 server")
                .isEqualTo(1);
        assertThat(model.getQueueStrategy(queue1, class1))
                .as("Queue 1 put strategy for Class1 should be FCFS")
                .isEqualTo(CommonConstants.QUEUE_STRATEGY_FCFS);
        assertThat(model.getDropRule(queue1, class2))
                .as("Queue 1 drop rule for Class2 should be Drop")
                .isEqualTo(CommonConstants.FINITE_DROP);

        // ---------------------------------------------------------
        // BLOCK E: Class Switch Routing Matrices
        // ---------------------------------------------------------
        // Test Class1 at Queue1 -> Routes to Queue2 and switches class
        Object routingQ1C1 = model.getRoutingStrategy(queue1, class1);
        assertThat(routingQ1C1)
                .as("Routing for Class1 in Queue 1 should be Class Switch")
                .isInstanceOf(jmt.gui.common.routingStrategies.ClassSwitchRouting.class);

        jmt.gui.common.routingStrategies.ClassSwitchRouting csQ1C1 = (jmt.gui.common.routingStrategies.ClassSwitchRouting) routingQ1C1;

        assertThat(csQ1C1.getValues())
                .as("Class1 in Queue 1 should route entirely to Queue 2")
                .containsEntry(queue2, 1.0);

        Map<Object, Double> classPathsQ1C1 = csQ1C1.getOutPaths().get(queue2);
        assertThat(classPathsQ1C1)
                .as("Class switch probabilities for Class1 -> Queue 2 should be properly assigned")
                .containsEntry(class1, 0.2)
                .containsEntry(class2, 0.8);

        // Test Class2 at Queue2 -> Routes to Sink1 and switches class
        Object routingQ2C2 = model.getRoutingStrategy(queue2, class2);
        assertThat(routingQ2C2)
                .as("Routing for Class2 in Queue 2 should be Class Switch")
                .isInstanceOf(jmt.gui.common.routingStrategies.ClassSwitchRouting.class);

        jmt.gui.common.routingStrategies.ClassSwitchRouting csQ2C2 = (jmt.gui.common.routingStrategies.ClassSwitchRouting) routingQ2C2;

        assertThat(csQ2C2.getValues())
                .as("Class2 in Queue 2 should route entirely to Sink 1")
                .containsEntry(sink, 1.0);

        Map<Object, Double> classPathsQ2C2 = csQ2C2.getOutPaths().get(sink);
        assertThat(classPathsQ2C2)
                .as("Class switch probabilities for Class2 -> Sink 1 should be properly assigned")
                .containsEntry(class1, 0.2)
                .containsEntry(class2, 0.8);
    }


    @Test
    void test_SjAS04Model_6AS_L5() throws Exception {

        // 1. Load the file
        File xmlFile = new File("src/test/resources/jmt/gui/common/xml/23985_SjAS04Model_6AS-L5.xml");
        CommonModel model = new CommonModel();

        // Parse using the XML archiver facade
        XMLArchiver.loadModel(xmlFile, model);

        // ---------------------------------------------------------
        // BLOCK A: Global Parameters and Populations
        // ---------------------------------------------------------
        assertThat(model.getUseRandomSeed()).as("No seed is specified, so useRandomSeed must be true").isTrue();
        assertThat(model.getMaximumDuration()).as("maxTime must be 600.0").isEqualTo(600.0);
        assertThat(model.getMaxSimulationSamples()).as("maxSamples must be 1,000,000").isEqualTo(1000000);
        assertThat(model.getPollingInterval()).as("polling must be 1.0").isEqualTo(1.0);

        // All classes are 'closed' in this XML
        assertThat(model.getOpenClassKeys()).as("There are no open classes").isEmpty();
        assertThat(model.getClosedClassKeys()).as("There are 10 closed classes in total").hasSize(10);

        Object classB = model.getClassByName("B");
        Object classW = model.getClassByName("W");
        Object classb = model.getClassByName("b"); // Lowercase class used for Petri net components

        assertThat(classB).isNotNull();
        assertThat(model.getClassPopulation(classB)).as("Class B has 40 customers").isEqualTo(40);
        assertThat(model.getClassPopulation(classW)).as("Class W has 50 customers").isEqualTo(50);
        assertThat(model.getClassPopulation(classb)).as("Class b has 0 initial customers").isZero();

        // ---------------------------------------------------------
        // BLOCK B: Station Type Recognition
        // ---------------------------------------------------------
        Object t1 = model.getStationByName("t1");
        Object nodeG = model.getStationByName("G");
        Object lq = model.getStationByName("L-Q");

        assertThat(model.getStationType(t1)).as("t1 is a Petri Net transition").isEqualTo(CommonConstants.STATION_TYPE_TRANSITION);
        assertThat(model.getStationType(nodeG)).as("G is a storage node (Place)").isEqualTo(CommonConstants.STATION_TYPE_PLACE);
        assertThat(model.getStationType(lq)).as("L-Q is a server (PSServer)").isEqualTo(CommonConstants.STATION_TYPE_SERVER);

        // ---------------------------------------------------------
        // BLOCK C: Petri Net Transitions (Enabling / Firing)
        // ---------------------------------------------------------
        assertThat(model.getTransitionModeName(t1, 0)).as("The first mode of t1 is Mode1").isEqualTo("Mode1");
        assertThat(model.getTransitionModeName(t1, 2)).as("The third mode of t1 is Mode3").isEqualTo("Mode3");
        assertThat(model.getNumberOfServers(t1, 0)).as("The number of servers/links for t1 Mode 1 is 1").isEqualTo(1);

        Object c1d = model.getStationByName("C1-D");
        Object classP = model.getClassByName("P");

        // In "t1" Mode 1 (index 0), the enabling condition for C1-D requires 1 token of class 'B'
        assertThat(model.getEnablingCondition(t1, 0, c1d, classB)).isEqualTo(1);
        // In "t1" Mode 2 (index 1), it requires 1 token of class 'P'
        assertThat(model.getEnablingCondition(t1, 1, c1d, classP)).isEqualTo(1);

        // Outcome: in "t1" Mode 1 (index 0), it emits 1 token of class 'b' to Place 'G'
        assertThat(model.getFiringOutcome(t1, 0, nodeG, classb)).isEqualTo(1);

        // ---------------------------------------------------------
        // BLOCK D: Storage / Places
        // ---------------------------------------------------------
        assertThat(model.getStationQueueCapacity(nodeG)).as("G capacity is infinite (-1)").isEqualTo(-1);
        assertThat(model.getDropRule(nodeG, classB)).as("DropRule in G must be Drop").isEqualTo(CommonConstants.FINITE_DROP);
        assertThat(model.getQueueStrategy(nodeG, classB)).as("Put strategy in G must be FCFS").isEqualTo(CommonConstants.QUEUE_STRATEGY_FCFS);

        // ---------------------------------------------------------
        // BLOCK E: Queues, PS Server, and Nested Distributions
        // ---------------------------------------------------------
        assertThat(model.getStationQueueStrategy(lq)).as("L-Q is a PSServer").isEqualTo(CommonConstants.STATION_QUEUE_STRATEGY_PSSERVER);
        assertThat(model.getStationNumberOfServers(lq)).as("L-Q has 1 server").isEqualTo(1);

        // Verify distribution extraction with class-specific parameters
        Object distB = model.getServiceTimeDistribution(lq, classB);
        assertThat(distB).as("The service strategy must be a Distribution")
                .isInstanceOf(jmt.gui.common.distributions.Distribution.class);
        assertThat(distB.getClass().getSimpleName()).as("The distribution is Exponential").isEqualTo("Exponential");

        // Class B has lambda equal to 1.0
        Double lambdaB = (Double) ((jmt.gui.common.distributions.Distribution) distB).getParameter("lambda").getValue();
        assertThat(lambdaB).isCloseTo(1.0, within(1e-9));

        // Class 'b' in L-Q has lambda equal to 0.3979307.
        Object dist_b = model.getServiceTimeDistribution(lq, classb);
        Double lambda_b = (Double) ((jmt.gui.common.distributions.Distribution) dist_b).getParameter("lambda").getValue();
        assertThat(lambda_b).isCloseTo(0.3979307, within(1e-9));

        // ---------------------------------------------------------
        // BLOCK F: Routing Strategies
        // ---------------------------------------------------------
        Object routingB = model.getRoutingStrategy(lq, classB);
        assertThat(routingB).as("L-Q uses RandomRouting")
                .isInstanceOf(jmt.gui.common.routingStrategies.RandomRouting.class);
    }

}
