package jmt.gui.common.xml;

import jmt.framework.gui.graph.MeasureValue;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.definitions.MeasureDefinition;
import jmt.gui.common.definitions.PAResultsModel;
import jmt.gui.common.definitions.StoredResultsModel;
import jmt.gui.common.xml.results.XMLResultsReader;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.Vector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Integration test suite for {@link XMLResultsReader#parseXML(Element, MeasureDefinition)}.
 *
 * <h3>Architecture under test</h3>
 * The two-phase pipeline: {@link jmt.gui.common.xml.parsers.ResultsParser} (DOM → DTO)
 * followed by {@link jmt.gui.common.xml.mapper.ResultsDescriptorMapper} (DTO → Model).
 *
 * <h3>Prerequisites — replacing the legacy class</h3>
 * The project contains a {@code @Deprecated} class with the same fully-qualified name
 * ({@code jmt.gui.common.xml.XMLResultsReader}) that {@link XMLArchiver} still references.
 * For these tests to exercise the new pipeline, the legacy class body must be replaced
 * with the new implementation (the file delivered as part of the refactoring output).
 * Until that replacement is in place, all tests in this suite will run against the legacy
 * code and may fail or produce unexpected results.
 *
 * <h3>Test strategy</h3>
 * Each test loads an {@code .xml} archive with {@link XMLArchiver#loadModel} to build a
 * valid {@link CommonModel}, then extracts the {@code <results>} element directly from the
 * same {@link Document} and feeds it to {@link XMLResultsReader#parseXML(Element, MeasureDefinition)}.
 * This exercises the full reading pipeline without introducing a secondary DOM parse,
 * and tests only the results layer in isolation from the simulation-model layer.
 *
 * <h3>Type representation</h3>
 * {@link MeasureDefinition#getMeasureType(int)} returns the <em>textual</em> suffix derived
 * from the measure name (e.g. {@code "Response Time"}, {@code "System Response Time"}).
 * The numeric {@code type} attribute in {@code Results.xsd} (e.g. {@code "2"}, {@code "8"})
 * is a write-only serialization detail produced by
 * {@link jmt.engine.simEngine.EngineUtils#decodeMeasureType} and is intentionally ignored
 * during parsing.
 */
class XMLResultsReaderTest {

    // -----------------------------------------------------------------------
    // Infrastructure
    // -----------------------------------------------------------------------

    /**
     * Parses the archive XML from disk, loads the simulation model via the full
     * {@link XMLArchiver} pipeline, and returns both the DOM {@link Document} and the
     * populated {@link CommonModel} as a two-element array.
     */
    private Object[] loadArchive(File xmlFile) throws Exception {
        // Load the CommonModel via the new pipeline (XMLReader + GuiXMLReader)
        CommonModel commonModel = new CommonModel();
        XMLArchiver.loadModel(xmlFile, commonModel);

        // Also parse the raw Document so we can extract specific sub-elements
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        return new Object[]{doc, commonModel};
    }

    /**
     * Extracts the {@code <results>} root element from the given {@link Document}.
     * Throws if not found, making test failures explicit.
     */
    private Element getResultsElement(Document doc) {
        NodeList list = doc.getElementsByTagName(XMLResultsConstants.XML_DOCUMENT_ROOT);
        if (list.getLength() > 0) {
            return (Element) list.item(0);
        }
        // Fall back to engine-format root
        list = doc.getElementsByTagName(XMLResultsConstants.XML_DOCUMENT_O_ROOT);
        if (list.getLength() > 0) {
            return (Element) list.item(0);
        }
        throw new IllegalArgumentException(
                "No <results> or <solutions> element found in document");
    }

    /**
     * Finds the index of a measure by its exact name. Returns {@code -1} when absent.
     */
    private int findMeasureIndex(MeasureDefinition model, String measureName) {
        for (int i = 0; i < model.getMeasureNumber(); i++) {
            if (measureName.equals(model.getName(i))) {
                return i;
            }
        }
        return -1;
    }

    // -----------------------------------------------------------------------
    // Test 1 — Parametric analysis results (Results.xsd, PA format)
    // File: Sec5_3_FastTask_Quorum1_Wif_JSIMg.xml
    // -----------------------------------------------------------------------

    /**
     * Verifies that a {@code <results>} block from a What-If (parametric) simulation is
     * correctly parsed into a {@link PAResultsModel}.
     *
     * <p>Covers:
     * <ul>
     *   <li>Global metadata: {@code logDecimalSeparator}, {@code logDelimiter}</li>
     *   <li>Measure metadata: station, class, alpha, precision, type code, nodeType</li>
     *   <li>Parametric sample values: mean, lower, upper bounds</li>
     *   <li>Parametric-specific {@code validity} flag via {@link PAResultsModel.MeasureValueImpl}</li>
     * </ul>
     */
    @Test
    void test_parseXML_PAResultsModel_fullCoverage() throws Exception {
        File xmlFile = new File(
                "src/test/resources/jmt/gui/common/xml/520_Sec5_3_FastTask_Quorum1_Wif_JSIMg.xml");

        Object[] loaded     = loadArchive(xmlFile);
        Document doc        = (Document) loaded[0];
        CommonModel simModel = (CommonModel) loaded[1];

        // A PAResultsModel requires a backing CommonModel for its parametric metadata
        PAResultsModel resultsModel = new PAResultsModel(simModel, true);

        XMLResultsReader.parseXML(getResultsElement(doc), resultsModel);

        // --- Global metadata ---
        // XML: logDecimalSeparator="," logDelimiter=";"
        assertThat(resultsModel.getLogDecimalSeparator())
                .as("logDecimalSeparator must be populated from the <results> root attribute")
                .isEqualTo(",");

        assertThat(resultsModel.getLogCsvDelimiter())
                .as("logDelimiter must be populated from the <results> root attribute")
                .isEqualTo(";");

        // --- Measure existence ---
        // XML: name="Queue 4_All classes_Response Time"
        int idx = findMeasureIndex(resultsModel, "Queue 4_All classes_Response Time");
        assertThat(idx)
                .as("Measure 'Queue 4_All classes_Response Time' must be registered in the model")
                .isGreaterThanOrEqualTo(0);

        // --- Measure metadata ---
        // XML: referenceStation="Queue 4"
        assertThat(resultsModel.getStationName(idx))
                .as("referenceStation attribute must be mapped to stationName")
                .isEqualTo("Queue 4");

        // XML: referenceClass="All classes"
        assertThat(resultsModel.getClassName(idx))
                .as("referenceClass attribute must be mapped to className")
                .isEqualTo("All classes");

        // XML: alpha="0.99" — stored directly (no complement for GUI format)
        assertThat(resultsModel.getAlpha(idx))
                .as("alpha must be stored as-is from the XML attribute (no complement for GUI format)")
                .isCloseTo(0.99, within(1e-9));

        // XML: precision="0.03"
        assertThat(resultsModel.getPrecision(idx))
                .as("precision must be mapped exactly from the XML attribute")
                .isCloseTo(0.03, within(1e-9));

        // XML: type="2" (numeric write-only code in Results.xsd).
        // MeasureDefinition.getMeasureType() always returns the textual suffix derived from
        // the measure name (legacy contract preserved by the new parser). For this measure
        // the name suffix after the last '_' is "Response Time".
        assertThat(resultsModel.getMeasureType(idx))
                .as("getMeasureType must return the name-suffix string 'Response Time', not the numeric XML attr")
                .isEqualTo("Response Time");

        // XML: nodeType="station"
        assertThat(resultsModel.getNodeType(idx))
                .as("nodeType attribute must be mapped")
                .isEqualTo("station");

        // --- Parametric samples (XML: 7 samples declared via analyzedSamples="7") ---
        Vector<MeasureValue> values = resultsModel.getValues(idx);
        assertThat(values)
                .as("All parametric sample points must have been parsed (7 expected)")
                .hasSize(7);

        // First sample:  lowerBound="0.5604330957282753"
        //                meanValue="0.5709857162306148"
        //                upperBound="0.5815383367329543"
        //                validity="true"
        MeasureValue first = values.get(0);
        assertThat(first.getLowerBound())
                .as("First sample lowerBound must match XML exactly")
                .isCloseTo(0.5604330957282753, within(1e-12));
        assertThat(first.getMeanValue())
                .as("First sample meanValue must match XML exactly")
                .isCloseTo(0.5709857162306148, within(1e-12));
        assertThat(first.getUpperBound())
                .as("First sample upperBound must match XML exactly")
                .isCloseTo(0.5815383367329543, within(1e-12));

        // Cast to the concrete PA implementation to check the validity flag
        PAResultsModel.MeasureValueImpl paSample = (PAResultsModel.MeasureValueImpl) first;
        assertThat(paSample.isValid())
                .as("validity='true' must be mapped to the isValid flag on MeasureValueImpl")
                .isTrue();
    }

    // -----------------------------------------------------------------------
    // Test 2 — Standard (non-parametric) simulation results
    // File: 225_conBeta13fesc.xml
    // -----------------------------------------------------------------------

    /**
     * Verifies that a standard simulation {@code <results>} block is correctly parsed
     * into a {@link StoredResultsModel}.
     *
     * <p>Covers:
     * <ul>
     *   <li>Global metadata: {@code pollingInterval}</li>
     *   <li>Measure metadata: state, type code, nodeType</li>
     *   <li>Normal sample values: mean, lower, upper bounds</li>
     * </ul>
     *
     * <p>Note: this file has no {@code logDecimalSeparator} / {@code logDelimiter}
     * attributes on the {@code <results>} element; the parser must apply the defaults
     * ({@code "."} and {@code ";"} respectively).
     */
    @Test
    void test_parseXML_StoredResultsModel_standardSim() throws Exception {
        File xmlFile = new File(
                "src/test/resources/jmt/gui/common/xml/225_conBeta13fesc.xml");

        Object[] loaded = loadArchive(xmlFile);
        Document doc    = (Document) loaded[0];

        StoredResultsModel resultsModel = new StoredResultsModel();
        XMLResultsReader.parseXML(getResultsElement(doc), resultsModel);

        // --- Global metadata ---
        // XML: pollingInterval="1.0"
        assertThat(resultsModel.getPollingInterval())
                .as("pollingInterval must be mapped from the <results> root attribute")
                .isCloseTo(1.0, within(1e-9));

        // XML has no logDecimalSeparator / logDelimiter → defaults apply
        assertThat(resultsModel.getLogDecimalSeparator())
                .as("Missing logDecimalSeparator must fall back to the default '.'")
                .isEqualTo(".");
        assertThat(resultsModel.getLogCsvDelimiter())
                .as("Missing logDelimiter must fall back to the default ';'")
                .isEqualTo(";");

        // --- Measure existence ---
        // XML: name="System Response Time"
        int idx = findMeasureIndex(resultsModel, "System Response Time");
        assertThat(idx)
                .as("Measure 'System Response Time' must be registered in the model")
                .isGreaterThanOrEqualTo(0);

        // --- Measure metadata ---
        // XML: nodeType="station"
        assertThat(resultsModel.getNodeType(idx))
                .as("nodeType must be mapped from the XML attribute")
                .isEqualTo("station");

        // XML: state="1" (MeasureDefinition.MEASURE_SUCCESS)
        assertThat(resultsModel.getMeasureState(idx))
                .as("state='1' must be mapped to MEASURE_SUCCESS")
                .isEqualTo(MeasureDefinition.MEASURE_SUCCESS);

        // XML: type="8" (numeric write-only code in Results.xsd).
        // MeasureDefinition.getMeasureType() returns the textual suffix from the measure name.
        // For "System Response Time" the suffix after the last '_' is "System Response Time"
        // (no underscore in the type fragment itself).
        assertThat(resultsModel.getMeasureType(idx))
                .as("getMeasureType must return 'System Response Time' (name suffix), not the numeric XML attr")
                .isEqualTo("System Response Time");

        // XML: alpha="0.99"
        assertThat(resultsModel.getAlpha(idx))
                .as("alpha must be stored as-is (no complement for GUI format)")
                .isCloseTo(0.99, within(1e-9));

        // XML: precision="0.01"
        assertThat(resultsModel.getPrecision(idx))
                .as("precision must be mapped exactly")
                .isCloseTo(0.01, within(1e-9));

        // --- Samples ---
        // First sample:  meanValue="5.106280776867828"
        //                lowerBound="4.393999594412545"
        //                upperBound="5.818561959323111"
        Vector<MeasureValue> values = resultsModel.getValues(idx);
        assertThat(values)
                .as("At least one sample must have been parsed")
                .isNotEmpty();

        MeasureValue first = values.get(0);
        assertThat(first.getMeanValue())
                .as("First sample meanValue must match XML exactly")
                .isCloseTo(5.106280776867828, within(1e-12));
        assertThat(first.getLowerBound())
                .as("First sample lowerBound must match XML exactly")
                .isCloseTo(4.393999594412545, within(1e-12));
        assertThat(first.getUpperBound())
                .as("First sample upperBound must match XML exactly")
                .isCloseTo(5.818561959323111, within(1e-12));
    }

    // -----------------------------------------------------------------------
    // Test 3 — discardedSamples, lastIntervalAvgValue, simulationTime
    // File: 792_Workcopy3_onoff.xml
    // -----------------------------------------------------------------------

    /**
     * Verifies the mapping of transient-period metadata ({@code discardedSamples}) and
     * the extended per-sample fields ({@code lastIntervalAvgValue}, {@code simulationTime}).
     * Also verifies that missing {@code lowerBound}/{@code upperBound} attributes default
     * to {@code 0.0} rather than throwing.
     */
    @Test
    void test_parseXML_StoredResultsModel_discardedAndIntervalFields() throws Exception {
        File xmlFile = new File(
                "src/test/resources/jmt/gui/common/xml/792_Workcopy3_onoff.xml");

        Object[] loaded = loadArchive(xmlFile);
        Document doc    = (Document) loaded[0];

        StoredResultsModel resultsModel = new StoredResultsModel();
        XMLResultsReader.parseXML(getResultsElement(doc), resultsModel);

        // XML: name="Queue 1_Utilization"
        int idx = findMeasureIndex(resultsModel, "Queue 1_Utilization");
        assertThat(idx)
                .as("Measure 'Queue 1_Utilization' must be registered in the model")
                .isGreaterThanOrEqualTo(0);

        // --- Measure metadata ---
        // XML: analyzedSamples="1000000"
        assertThat(resultsModel.getAnalyzedSamples(idx))
                .as("analyzedSamples must be mapped exactly from the XML attribute")
                .isEqualTo(1_000_000);

        // XML: discardedSamples="84180"
        assertThat(resultsModel.getDiscardedSamples(idx))
                .as("discardedSamples (transient period) must be mapped exactly")
                .isEqualTo(84_180);

        // --- Extended sample fields ---
        // First sample (index 0):
        //   meanValue="1.0"
        //   lastIntervalAvgValue="0.9999660299030937"
        //   simulationTime="29484.638885393677"
        //   No lowerBound / upperBound attributes → must default to 0.0
        Vector<MeasureValue> values = resultsModel.getValues(idx);
        assertThat(values)
                .as("At least one sample must have been parsed")
                .isNotEmpty();

        MeasureValue first = values.get(0);
        assertThat(first.getMeanValue())
                .as("meanValue must match XML exactly")
                .isCloseTo(1.0, within(1e-12));
        assertThat(first.getLastIntervalAvgValue())
                .as("lastIntervalAvgValue must be mapped from the XML attribute")
                .isCloseTo(0.9999660299030937, within(1e-12));
        assertThat(first.getSimTime())
                .as("simulationTime must be mapped from the XML attribute")
                .isCloseTo(29484.638885393677, within(1e-9));

        // First sample has no lowerBound / upperBound in the XML
        assertThat(first.getLowerBound())
                .as("Absent lowerBound attribute must default to 0.0")
                .isZero();
        assertThat(first.getUpperBound())
                .as("Absent upperBound attribute must default to 0.0")
                .isZero();
    }

    // -----------------------------------------------------------------------
    // Test 4 — Custom log delimiter / decimal separator
    // File: 4039_240415_FCFS_QD.xml
    // -----------------------------------------------------------------------

    /**
     * Verifies that non-default {@code logDecimalSeparator} and {@code logDelimiter}
     * attributes on the {@code <results>} root element are correctly mapped.
     *
     * <p>This file uses {@code logDecimalSeparator="."} and {@code logDelimiter=","},
     * which differs from the defaults ({@code "."} / {@code ";"}) and from the files
     * used in other tests.
     */
    @Test
    void test_parseXML_StoredResultsModel_customLogFormatAttributes() throws Exception {
        File xmlFile = new File(
                "src/test/resources/jmt/gui/common/xml/4039_240415_FCFS_QD.xml");

        Object[] loaded = loadArchive(xmlFile);
        Document doc    = (Document) loaded[0];

        StoredResultsModel resultsModel = new StoredResultsModel();
        XMLResultsReader.parseXML(getResultsElement(doc), resultsModel);

        // XML: logDecimalSeparator="."  logDelimiter=","
        assertThat(resultsModel.getLogDecimalSeparator())
                .as("logDecimalSeparator='.' must be stored as-is")
                .isEqualTo(".");
        assertThat(resultsModel.getLogCsvDelimiter())
                .as("logDelimiter=',' must be stored as-is")
                .isEqualTo(",");

        // Sanity: at least one measure was parsed (verifies the pipeline ran)
        assertThat(resultsModel.getMeasureNumber())
                .as("At least one measure must have been parsed from this archive")
                .isPositive();
    }

    // -----------------------------------------------------------------------
    // Test 5 — Unrecognized root element is a no-op
    // -----------------------------------------------------------------------

    /**
     * Verifies that calling {@link XMLResultsReader#parseXML} with a {@link Document}
     * whose root element is neither {@code <results>} nor {@code <solutions>} leaves
     * the model unchanged (zero measures, default metadata).
     *
     * <p>This exercises the guard branches inside the parser that skip processing for
     * unknown document types without throwing.
     */
    @Test
    void test_parseXML_unrecognizedRootElement_gracefulNoOp() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        doc.appendChild(doc.createElement("model")); // neither <results> nor <solutions>

        StoredResultsModel model = new StoredResultsModel();
        XMLResultsReader.parseXML(doc, model);

        assertThat(model.getMeasureNumber())
                .as("Model must remain empty when the root element is not a results document")
                .isZero();
    }

    // -----------------------------------------------------------------------
    // Test 6 — Fork-Join topology with Fork Join Response Time measure type
    // File: 10089_ForkJoin_4Stat_2 - SEED1.xml
    // -----------------------------------------------------------------------

    /**
     * Verifies parsing of a Fork-Join simulation that contains both per-class measures and
     * the {@code Fork Join Response Time} measure type (numeric type code 29).
     *
     * <p>Covers:
     * <ul>
     *   <li>Nine measures parsed correctly (system + per-station + fork-join)</li>
     *   <li>{@code Fork Join Response Time} measure type resolved from measure name</li>
     *   <li>Per-class station metadata: referenceClass and referenceStation</li>
     *   <li>Samples without {@code lowerBound}/{@code upperBound} default to {@code 0.0}</li>
     *   <li>{@code discardedSamples} and {@code analyzedSamples} for a high-volume run</li>
     * </ul>
     */
    @Test
    void test_parseXML_StoredResultsModel_forkJoinTopology() throws Exception {
        File xmlFile = new File(
                "src/test/resources/jmt/gui/common/xml/10089_ForkJoin_4Stat_2 - SEED1.xml");

        Object[] loaded = loadArchive(xmlFile);
        Document doc    = (Document) loaded[0];

        StoredResultsModel resultsModel = new StoredResultsModel();
        XMLResultsReader.parseXML(getResultsElement(doc), resultsModel);

        // --- Global metadata ---
        // XML: logDecimalSeparator="," logDelimiter=";" pollingInterval="1.0"
        assertThat(resultsModel.getLogDecimalSeparator())
                .as("logDecimalSeparator must be ','")
                .isEqualTo(",");
        assertThat(resultsModel.getLogCsvDelimiter())
                .as("logDelimiter must be ';'")
                .isEqualTo(";");
        assertThat(resultsModel.getPollingInterval())
                .as("pollingInterval must be 1.0")
                .isCloseTo(1.0, within(1e-9));

        // --- Measure count ---
        // XML: 9 <measure> elements in the <results> block
        assertThat(resultsModel.getMeasureNumber())
                .as("Exactly 9 measures must be parsed from this archive")
                .isEqualTo(9);

        // --- Fork Join Response Time measure ---
        // XML: name="Fork 1_Class1_Fork Join Response Time" referenceStation="Fork 1"
        //      referenceClass="Class1" alpha="0.99" precision="0.03" state="1"
        int forkIdx = findMeasureIndex(resultsModel, "Fork 1_Class1_Fork Join Response Time");
        assertThat(forkIdx)
                .as("Measure 'Fork 1_Class1_Fork Join Response Time' must be registered")
                .isGreaterThanOrEqualTo(0);

        assertThat(resultsModel.getMeasureType(forkIdx))
                .as("Measure type must be derived from the name suffix 'Fork Join Response Time'")
                .isEqualTo("Fork Join Response Time");
        assertThat(resultsModel.getStationName(forkIdx))
                .as("referenceStation must be 'Fork 1'")
                .isEqualTo("Fork 1");
        assertThat(resultsModel.getClassName(forkIdx))
                .as("referenceClass must be 'Class1'")
                .isEqualTo("Class1");
        assertThat(resultsModel.getMeasureState(forkIdx))
                .as("state='1' must map to MEASURE_SUCCESS")
                .isEqualTo(MeasureDefinition.MEASURE_SUCCESS);
        assertThat(resultsModel.getNodeType(forkIdx))
                .as("nodeType must be 'station'")
                .isEqualTo("station");

        // --- System Number of Customers measure: analyzedSamples and discardedSamples ---
        // XML: name="Class1_System Number of Customers"
        //      analyzedSamples="10000001" discardedSamples="4200"
        int sysIdx = findMeasureIndex(resultsModel, "Class1_System Number of Customers");
        assertThat(sysIdx)
                .as("Measure 'Class1_System Number of Customers' must be registered")
                .isGreaterThanOrEqualTo(0);
        assertThat(resultsModel.getAnalyzedSamples(sysIdx))
                .as("analyzedSamples must be 10000001")
                .isEqualTo(10_000_001);
        assertThat(resultsModel.getDiscardedSamples(sysIdx))
                .as("discardedSamples must be 4200")
                .isEqualTo(4_200);

        // --- First sample of Class1_System Number of Customers ---
        // XML: meanValue="3.444952652663055"  (no lowerBound/upperBound)
        Vector<MeasureValue> sysValues = resultsModel.getValues(sysIdx);
        assertThat(sysValues).as("At least one sample must have been parsed").isNotEmpty();
        MeasureValue firstSys = sysValues.get(0);
        assertThat(firstSys.getMeanValue())
                .as("First sample meanValue must match XML exactly")
                .isCloseTo(3.444952652663055, within(1e-12));
        assertThat(firstSys.getLowerBound())
                .as("Absent lowerBound must default to 0.0")
                .isZero();
        assertThat(firstSys.getUpperBound())
                .as("Absent upperBound must default to 0.0")
                .isZero();
    }

    // -----------------------------------------------------------------------
    // Test 7 — elapsedTime, comma delimiter, MEASURE_FAILED state, empty nodeType
    // File: 10190_220423_2RotNogen_2Q.xml
    // -----------------------------------------------------------------------

    /**
     * Verifies parsing of a two-queue, two-class model where some measures did not
     * converge ({@code state="2"}), the log uses a comma delimiter, and the
     * {@code elapsedTime} attribute is present on the {@code <results>} element.
     *
     * <p>Covers:
     * <ul>
     *   <li>{@code elapsedTime} attribute mapped to {@link StoredResultsModel#getElapsedTime()}</li>
     *   <li>{@code logDelimiter=","} (differs from default {@code ";"})</li>
     *   <li>{@code state="2"} mapped to {@link MeasureDefinition#MEASURE_FAILED}</li>
     *   <li>System-level measures with empty {@code nodeType} and empty station/class</li>
     *   <li>Per-class system response time measures (Class1, Class2)</li>
     *   <li>Seven measures total</li>
     * </ul>
     */
    @Test
    void test_parseXML_StoredResultsModel_elapsedTimeAndMeasureFailed() throws Exception {
        File xmlFile = new File(
                "src/test/resources/jmt/gui/common/xml/10190_220423_2RotNogen_2Q.xml");

        Object[] loaded = loadArchive(xmlFile);
        Document doc    = (Document) loaded[0];

        StoredResultsModel resultsModel = new StoredResultsModel();
        XMLResultsReader.parseXML(getResultsElement(doc), resultsModel);

        // --- Global metadata ---
        // XML: elapsedTime="1949127" logDecimalSeparator="." logDelimiter="," pollingInterval="1.0"
        assertThat(resultsModel.getElapsedTime())
                .as("elapsedTime must be mapped from the <results> root attribute")
                .isEqualTo(1_949_127L);
        assertThat(resultsModel.getLogDecimalSeparator())
                .as("logDecimalSeparator must be '.'")
                .isEqualTo(".");
        assertThat(resultsModel.getLogCsvDelimiter())
                .as("logDelimiter must be ',' (non-default)")
                .isEqualTo(",");
        assertThat(resultsModel.getPollingInterval())
                .as("pollingInterval must be 1.0")
                .isCloseTo(1.0, within(1e-9));

        // --- Measure count ---
        // XML: 7 <measure> elements in the <results> block
        assertThat(resultsModel.getMeasureNumber())
                .as("Exactly 7 measures must be parsed from this archive")
                .isEqualTo(7);

        // --- Successful measure: Queue 1_Utilization ---
        // XML: state="1" referenceStation="Queue 1" referenceClass=""
        int utilIdx = findMeasureIndex(resultsModel, "Queue 1_Utilization");
        assertThat(utilIdx)
                .as("Measure 'Queue 1_Utilization' must be registered")
                .isGreaterThanOrEqualTo(0);
        assertThat(resultsModel.getMeasureState(utilIdx))
                .as("state='1' must map to MEASURE_SUCCESS")
                .isEqualTo(MeasureDefinition.MEASURE_SUCCESS);
        assertThat(resultsModel.getStationName(utilIdx))
                .as("referenceStation must be 'Queue 1'")
                .isEqualTo("Queue 1");
        assertThat(resultsModel.getClassName(utilIdx))
                .as("referenceClass must be empty string for all-class measure")
                .isEqualTo("");

        // First sample meanValue
        // XML: meanValue="0.8873052109573796"
        Vector<MeasureValue> utilValues = resultsModel.getValues(utilIdx);
        assertThat(utilValues).as("At least one sample must have been parsed").isNotEmpty();
        assertThat(utilValues.get(0).getMeanValue())
                .as("First sample meanValue must match XML exactly")
                .isCloseTo(0.8873052109573796, within(1e-12));

        // --- Failed measure: Queue 1_Response Time ---
        // XML: state="2" → MEASURE_FAILED
        int rtIdx = findMeasureIndex(resultsModel, "Queue 1_Response Time");
        assertThat(rtIdx)
                .as("Measure 'Queue 1_Response Time' must be registered")
                .isGreaterThanOrEqualTo(0);
        assertThat(resultsModel.getMeasureState(rtIdx))
                .as("state='2' must map to MEASURE_FAILED")
                .isEqualTo(MeasureDefinition.MEASURE_FAILED);

        // --- System-level measure: empty nodeType and empty station/class ---
        // XML: name="System Response Time" nodeType="" referenceClass="" referenceStation=""
        int sysRtIdx = findMeasureIndex(resultsModel, "System Response Time");
        assertThat(sysRtIdx)
                .as("Measure 'System Response Time' must be registered")
                .isGreaterThanOrEqualTo(0);
        assertThat(resultsModel.getNodeType(sysRtIdx))
                .as("nodeType must be empty string for system-level measure")
                .isEqualTo("");
        assertThat(resultsModel.getMeasureState(sysRtIdx))
                .as("state='2' must map to MEASURE_FAILED for system response time")
                .isEqualTo(MeasureDefinition.MEASURE_FAILED);

        // --- Per-class system response time measures exist ---
        assertThat(findMeasureIndex(resultsModel, "Class1_System Response Time"))
                .as("Measure 'Class1_System Response Time' must be registered")
                .isGreaterThanOrEqualTo(0);
        assertThat(findMeasureIndex(resultsModel, "Class2_System Response Time"))
                .as("Measure 'Class2_System Response Time' must be registered")
                .isGreaterThanOrEqualTo(0);
    }

    // -----------------------------------------------------------------------
    // Test 8 — Nested Fork-Join with zero discarded samples
    // File: 641_2FJ_2tasks_nestedOK.xml
    // -----------------------------------------------------------------------

    /**
     * Verifies parsing of a nested Fork-Join model with two fork-join chains and
     * multiple number-of-customers measures.
     *
     * <p>Covers:
     * <ul>
     *   <li>Two {@code Fork Join Response Time} measures (Fork 1 and Fork 2)</li>
     *   <li>{@code discardedSamples="0"} correctly stored as zero</li>
     *   <li>System-level measures with {@code nodeType="station"} and empty referenceStation</li>
     *   <li>Eleven measures total</li>
     *   <li>Second sample lowerBound/upperBound present and correct</li>
     * </ul>
     */
    @Test
    void test_parseXML_StoredResultsModel_nestedForkJoinZeroDiscarded() throws Exception {
        File xmlFile = new File(
                "src/test/resources/jmt/gui/common/xml/641_2FJ_2tasks_nestedOK.xml");

        Object[] loaded = loadArchive(xmlFile);
        Document doc    = (Document) loaded[0];

        StoredResultsModel resultsModel = new StoredResultsModel();
        XMLResultsReader.parseXML(getResultsElement(doc), resultsModel);

        // --- Global metadata ---
        // XML: logDecimalSeparator="," logDelimiter=";" pollingInterval="1.0"
        assertThat(resultsModel.getLogDecimalSeparator())
                .as("logDecimalSeparator must be ','")
                .isEqualTo(",");
        assertThat(resultsModel.getLogCsvDelimiter())
                .as("logDelimiter must be ';'")
                .isEqualTo(";");

        // --- Measure count ---
        // XML: 11 <measure> elements in the <results> block
        assertThat(resultsModel.getMeasureNumber())
                .as("Exactly 11 measures must be parsed from this archive")
                .isEqualTo(11);

        // --- Fork 1_Fork Join Response Time ---
        // XML: referenceStation="Fork 1" discardedSamples="0" analyzedSamples="81920" state="1"
        int fork1Idx = findMeasureIndex(resultsModel, "Fork 1_Fork Join Response Time");
        assertThat(fork1Idx)
                .as("Measure 'Fork 1_Fork Join Response Time' must be registered")
                .isGreaterThanOrEqualTo(0);
        assertThat(resultsModel.getMeasureType(fork1Idx))
                .as("Measure type must be 'Fork Join Response Time'")
                .isEqualTo("Fork Join Response Time");
        assertThat(resultsModel.getDiscardedSamples(fork1Idx))
                .as("discardedSamples='0' must be stored as zero")
                .isEqualTo(0);
        assertThat(resultsModel.getAnalyzedSamples(fork1Idx))
                .as("analyzedSamples must be 81920")
                .isEqualTo(81_920);
        assertThat(resultsModel.getMeasureState(fork1Idx))
                .as("state='1' must map to MEASURE_SUCCESS")
                .isEqualTo(MeasureDefinition.MEASURE_SUCCESS);
        assertThat(resultsModel.getStationName(fork1Idx))
                .as("referenceStation must be 'Fork 1'")
                .isEqualTo("Fork 1");

        // Second sample: lowerBound and upperBound present
        // XML: lowerBound="0.634133029082304" meanValue="0.6999999999998747"
        //      upperBound="0.7658669709174455"
        Vector<MeasureValue> fork1Values = resultsModel.getValues(fork1Idx);
        assertThat(fork1Values).as("At least two samples must have been parsed").hasSizeGreaterThan(1);
        MeasureValue secondSample = fork1Values.get(1);
        assertThat(secondSample.getMeanValue())
                .as("Second sample meanValue must match XML exactly")
                .isCloseTo(0.6999999999998747, within(1e-12));
        assertThat(secondSample.getLowerBound())
                .as("Second sample lowerBound must match XML exactly")
                .isCloseTo(0.634133029082304, within(1e-12));
        assertThat(secondSample.getUpperBound())
                .as("Second sample upperBound must match XML exactly")
                .isCloseTo(0.7658669709174455, within(1e-12));

        // --- Fork 2_Fork Join Response Time also registered ---
        assertThat(findMeasureIndex(resultsModel, "Fork 2_Fork Join Response Time"))
                .as("Measure 'Fork 2_Fork Join Response Time' must be registered")
                .isGreaterThanOrEqualTo(0);

        // --- System Response Time: nodeType="station" with empty referenceStation ---
        // XML: nodeType="station" referenceStation="" referenceClass=""
        int sysRtIdx = findMeasureIndex(resultsModel, "System Response Time");
        assertThat(sysRtIdx)
                .as("Measure 'System Response Time' must be registered")
                .isGreaterThanOrEqualTo(0);
        assertThat(resultsModel.getNodeType(sysRtIdx))
                .as("nodeType must be 'station' even for system-level measure in this file")
                .isEqualTo("station");
        assertThat(resultsModel.getStationName(sysRtIdx))
                .as("referenceStation must be empty string for system-level measure")
                .isEqualTo("");
    }
}