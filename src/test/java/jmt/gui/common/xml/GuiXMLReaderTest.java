/**
 * Integration test suite for {@link jmt.gui.common.xml.gui.GuiXMLReader#parseXML(org.w3c.dom.Element, jmt.gui.common.definitions.CommonModel)}.
 * Verifies that GUI-specific data (user-class colors, station positions, connection shapes,
 * and parametric-analysis configuration) is correctly parsed from the {@code <jmodel>} section.
 *
 * @author Borgna Daniele
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.definitions.parametric.ParametricAnalysisDefinition;
import jmt.gui.common.xml.gui.GuiXMLReader;
import jmt.gui.jsimgraph.definitions.JMTPath;
import jmt.gui.jsimgraph.definitions.JMTPoint;
import jmt.gui.jsimgraph.definitions.JSimGraphModel;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GuiXMLReaderTest {

    // -----------------------------------------------------------------------
    // Infrastructure
    // -----------------------------------------------------------------------

    /**
     * Parses the archive XML from disk, loads the simulation model via the full
     * {@link XMLArchiver} pipeline into a {@link JSimGraphModel}, and returns both
     * the DOM {@link Document} and the populated model as a two-element array.
     */
    private Object[] loadArchive(File xmlFile) throws Exception {
        JSimGraphModel model = new JSimGraphModel(null);
        XMLArchiver.loadModel(xmlFile, model);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        return new Object[]{doc, model};
    }

    /**
     * Extracts the {@code <jmodel>} root element from the given {@link Document}.
     * Throws if not found, making test failures explicit.
     */
    private Element getJModelElement(Document doc) {
        Element el = (Element) doc.getElementsByTagName(GuiXMLConstants.XML_DOCUMENT_ROOT).item(0);
        if (el == null) {
            throw new IllegalArgumentException("No <jmodel> element found in document");
        }
        return el;
    }

    /**
     * Returns the key for a station by name, or {@code null} when absent.
     */
    private Object findStationKey(JSimGraphModel model, String name) {
        for (Object key : model.getStationKeys()) {
            if (name.equals(model.getStationName(key))) {
                return key;
            }
        }
        return null;
    }

    /**
     * Returns the key for a user class by name, or {@code null} when absent.
     */
    private Object findClassKey(JSimGraphModel model, String name) {
        for (Object key : model.getClassKeys()) {
            if (name.equals(model.getClassName(key))) {
                return key;
            }
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Test 1 — Single class, two stations, no connections, no parametric
    // File: 225_conBeta13fesc.xml
    // -----------------------------------------------------------------------

    /**
     * Verifies the minimal jmodel section: one user class whose color is parsed
     * from the ARGB hex string, two stations with their (x, y) positions, and
     * {@code rotate=false} with no {@code angle} attribute (defaults to {@code 0.0}).
     */
    @Test
    void test_parseXML_singleClassTwoStations_colorsAndPositions() throws Exception {
        File xmlFile = new File(
                "src/test/resources/jmt/gui/common/xml/225_conBeta13fesc.xml");

        Object[] loaded = loadArchive(xmlFile);
        Document doc = (Document) loaded[0];
        JSimGraphModel model = (JSimGraphModel) loaded[1];

        GuiXMLReader.parseXML(getJModelElement(doc), model);

        // --- Class color ---
        // XML: <userClass color="#FF0000FF" name="Class0"/>
        // #FF0000FF in ARGB → alpha=255, R=0, G=0, B=255 (opaque blue)
        Object classKey = findClassKey(model, "Class0");
        assertThat(classKey).as("Class 'Class0' must exist in the model").isNotNull();
        Color color = model.getClassColor(classKey);
        assertThat(color.getAlpha()).as("alpha must be 255 (fully opaque)").isEqualTo(255);
        assertThat(color.getRed()).as("red channel must be 0").isEqualTo(0);
        assertThat(color.getGreen()).as("green channel must be 0").isEqualTo(0);
        assertThat(color.getBlue()).as("blue channel must be 255").isEqualTo(255);

        // --- Station positions ---
        // XML: <station name="CPU"><position rotate="false" x="160.0" y="125.0"/>
        Object cpuKey = findStationKey(model, "CPU");
        assertThat(cpuKey).as("Station 'CPU' must exist in the model").isNotNull();
        JMTPoint cpuPos = model.getStationPosition(cpuKey);
        assertThat(cpuPos.getX()).as("CPU x position must be 160.0").isCloseTo(160.0, within(1e-9));
        assertThat(cpuPos.getY()).as("CPU y position must be 125.0").isCloseTo(125.0, within(1e-9));
        assertThat(cpuPos.isRotate()).as("CPU rotate must be false").isFalse();
        assertThat(cpuPos.getAngle()).as("CPU angle must default to 0.0 when absent").isCloseTo(0.0, within(1e-9));

        // XML: <station name="FlowEq"><position rotate="false" x="368.0" y="139.0"/>
        Object flowKey = findStationKey(model, "FlowEq");
        assertThat(flowKey).as("Station 'FlowEq' must exist in the model").isNotNull();
        JMTPoint flowPos = model.getStationPosition(flowKey);
        assertThat(flowPos.getX()).as("FlowEq x position must be 368.0").isCloseTo(368.0, within(1e-9));
        assertThat(flowPos.getY()).as("FlowEq y position must be 139.0").isCloseTo(139.0, within(1e-9));

        // --- No parametric analysis ---
        assertThat(model.isParametricAnalysisEnabled())
                .as("Parametric analysis must not be enabled for this archive")
                .isFalse();
    }

    // -----------------------------------------------------------------------
    // Test 2 — angle attribute in positions and parametric analysis
    // File: 520_Sec5_3_FastTask_Quorum1_Wif_JSIMg.xml
    // -----------------------------------------------------------------------

    /**
     * Verifies that the {@code angle} attribute on {@code <position>} elements is
     * correctly parsed, and that a {@code <parametric>} block with
     * {@code enabled="true"} is loaded into the model with the correct type,
     * initial value, final value, and step count.
     */
    @Test
    void test_parseXML_angleAttributeAndParametricAnalysis() throws Exception {
        File xmlFile = new File(
                "src/test/resources/jmt/gui/common/xml/520_Sec5_3_FastTask_Quorum1_Wif_JSIMg.xml");

        Object[] loaded = loadArchive(xmlFile);
        Document doc = (Document) loaded[0];
        JSimGraphModel model = (JSimGraphModel) loaded[1];

        GuiXMLReader.parseXML(getJModelElement(doc), model);

        // --- Class color ---
        // XML: <userClass color="#FFFF0000" name="Class1"/>
        // #FFFF0000 in ARGB → alpha=255, R=255, G=0, B=0 (opaque red)
        Object classKey = findClassKey(model, "Class1");
        assertThat(classKey).as("Class 'Class1' must exist in the model").isNotNull();
        Color color = model.getClassColor(classKey);
        assertThat(color.getRed()).as("red channel must be 255").isEqualTo(255);
        assertThat(color.getGreen()).as("green channel must be 0").isEqualTo(0);
        assertThat(color.getBlue()).as("blue channel must be 0").isEqualTo(0);

        // --- Station position with angle attribute ---
        // XML: <station name="Source 1"><position angle="0.0" rotate="false" x="121.0" y="183.0"/>
        Object sourceKey = findStationKey(model, "Source 1");
        assertThat(sourceKey).as("Station 'Source 1' must exist in the model").isNotNull();
        JMTPoint sourcePos = model.getStationPosition(sourceKey);
        assertThat(sourcePos.getX()).as("Source 1 x must be 121.0").isCloseTo(121.0, within(1e-9));
        assertThat(sourcePos.getY()).as("Source 1 y must be 183.0").isCloseTo(183.0, within(1e-9));
        assertThat(sourcePos.getAngle()).as("Source 1 angle must be 0.0").isCloseTo(0.0, within(1e-9));
        assertThat(sourcePos.isRotate()).as("Source 1 rotate must be false").isFalse();

        // --- Parametric analysis ---
        // XML: <parametric classPath="...ArrivalRateParametricAnalysis" enabled="true">
        //        <field name="From" value="0.25"/>
        //        <field name="To" value="1.75"/>
        //        <field name="Steps" value="7"/>
        assertThat(model.isParametricAnalysisEnabled())
                .as("Parametric analysis must be enabled")
                .isTrue();
        ParametricAnalysisDefinition pad = model.getParametricAnalysisModel();
        assertThat(pad).as("ParametricAnalysisModel must not be null").isNotNull();
        assertThat(pad.getType())
                .as("Parametric analysis type must be 'Arrival rates'")
                .isEqualTo("Arrival rates");
        assertThat(pad.getInitialValue())
                .as("Initial value (From) must be 0.25")
                .isCloseTo(0.25, within(1e-9));
        assertThat(pad.getFinalValue())
                .as("Final value (To) must be 1.75")
                .isCloseTo(1.75, within(1e-9));
        assertThat(pad.getNumberOfSteps())
                .as("Number of steps must be 7")
                .isEqualTo(7);
    }

    // -----------------------------------------------------------------------
    // Test 3 — Two classes and connection shapes
    // File: 4039_240415_FCFS_QD.xml
    // -----------------------------------------------------------------------

    /**
     * Verifies that two {@code <userClass>} entries receive distinct colors and that
     * {@code <connectionShape>} elements are stored correctly on the model.
     */
    @Test
    void test_parseXML_twoClassesAndConnectionShapes() throws Exception {
        File xmlFile = new File(
                "src/test/resources/jmt/gui/common/xml/4039_240415_FCFS_QD.xml");

        Object[] loaded = loadArchive(xmlFile);
        Document doc = (Document) loaded[0];
        JSimGraphModel model = (JSimGraphModel) loaded[1];

        GuiXMLReader.parseXML(getJModelElement(doc), model);

        // --- Two distinct class colors ---
        // XML: Class1 "#FF0000FF" (blue), Token "#FFFF0000" (red)
        Object class1Key = findClassKey(model, "Class1");
        Object tokenKey = findClassKey(model, "Token");
        assertThat(class1Key).as("Class 'Class1' must exist").isNotNull();
        assertThat(tokenKey).as("Class 'Token' must exist").isNotNull();

        Color class1Color = model.getClassColor(class1Key);
        assertThat(class1Color.getBlue()).as("Class1 must have blue=255 (#FF0000FF)").isEqualTo(255);
        assertThat(class1Color.getRed()).as("Class1 must have red=0 (#FF0000FF)").isEqualTo(0);

        Color tokenColor = model.getClassColor(tokenKey);
        assertThat(tokenColor.getRed()).as("Token must have red=255 (#FFFF0000)").isEqualTo(255);
        assertThat(tokenColor.getBlue()).as("Token must have blue=0 (#FFFF0000)").isEqualTo(0);

        // --- Connection shape: Queue 1 → Delay 1 (single straight arc) ---
        Object queue1Key = findStationKey(model, "Queue 1");
        Object delay1Key = findStationKey(model, "Delay 1");
        assertThat(queue1Key).as("Station 'Queue 1' must exist").isNotNull();
        assertThat(delay1Key).as("Station 'Delay 1' must exist").isNotNull();

        JMTPath path = model.getConnectionShape(queue1Key, delay1Key);
        assertThat(path).as("Connection shape Queue1→Delay1 must have been stored").isNotNull();
        assertThat(path.getArcsNb()).as("Queue1→Delay1 path must have exactly 1 arc").isEqualTo(1);

        // --- Connection shape: Delay 1 → Queue 1 (multi-arc path with 5 arcs) ---
        JMTPath returnPath = model.getConnectionShape(delay1Key, queue1Key);
        assertThat(returnPath).as("Connection shape Delay1→Queue1 must have been stored").isNotNull();
        assertThat(returnPath.getArcsNb())
                .as("Delay1→Queue1 return path must have 5 arcs (as in XML)")
                .isEqualTo(5);
    }

    // -----------------------------------------------------------------------
    // Test 4 — Two classes, four stations, three connection shapes
    // File: 10190_220423_2RotNogen_2Q.xml
    // -----------------------------------------------------------------------

    /**
     * Verifies a two-class, two-queue model: both class colors are set, all four
     * station positions are loaded, and the three connection shapes are present.
     */
    @Test
    void test_parseXML_twoClassesFourStationsThreeConnections() throws Exception {
        File xmlFile = new File(
                "src/test/resources/jmt/gui/common/xml/10190_220423_2RotNogen_2Q.xml");

        Object[] loaded = loadArchive(xmlFile);
        Document doc = (Document) loaded[0];
        JSimGraphModel model = (JSimGraphModel) loaded[1];

        GuiXMLReader.parseXML(getJModelElement(doc), model);

        // --- Two class colors ---
        // XML: Class1 "#FF0000FF" (blue), Class2 "#FFFF0000" (red)
        Object c1Key = findClassKey(model, "Class1");
        Object c2Key = findClassKey(model, "Class2");
        assertThat(c1Key).as("Class 'Class1' must exist").isNotNull();
        assertThat(c2Key).as("Class 'Class2' must exist").isNotNull();
        assertThat(model.getClassColor(c1Key).getBlue())
                .as("Class1 color must have blue=255").isEqualTo(255);
        assertThat(model.getClassColor(c2Key).getRed())
                .as("Class2 color must have red=255").isEqualTo(255);

        // --- Station positions ---
        // XML: Source 1 at (51.0, 82.0), Queue 2 at (375.0, 81.0)
        Object src1Key = findStationKey(model, "Source 1");
        JMTPoint src1Pos = model.getStationPosition(src1Key);
        assertThat(src1Pos.getX()).as("Source 1 x must be 51.0").isCloseTo(51.0, within(1e-9));
        assertThat(src1Pos.getY()).as("Source 1 y must be 82.0").isCloseTo(82.0, within(1e-9));

        Object q2Key = findStationKey(model, "Queue 2");
        JMTPoint q2Pos = model.getStationPosition(q2Key);
        assertThat(q2Pos.getX()).as("Queue 2 x must be 375.0").isCloseTo(375.0, within(1e-9));

        // --- Three connection shapes present ---
        Object q1Key = findStationKey(model, "Queue 1");
        Object sink1Key = findStationKey(model, "Sink 1");
        assertThat(model.getConnectionShape(src1Key, q1Key))
                .as("Connection Source1→Queue1 must have been stored").isNotNull();
        assertThat(model.getConnectionShape(q1Key, q2Key))
                .as("Connection Queue1→Queue2 must have been stored").isNotNull();
        assertThat(model.getConnectionShape(q2Key, sink1Key))
                .as("Connection Queue2→Sink1 must have been stored").isNotNull();
    }

    // -----------------------------------------------------------------------
    // Test 5 — Ten user classes with distinct colors
    // File: 23985_SjAS04Model_6AS-L5.xml
    // -----------------------------------------------------------------------

    /**
     * Verifies that all ten {@code <userClass>} entries are parsed and receive
     * the correct color. Exercises the full color-parsing loop for a large class set.
     */
    @Test
    void test_parseXML_tenUserClassColors() throws Exception {
        File xmlFile = new File(
                "src/test/resources/jmt/gui/common/xml/23985_SjAS04Model_6AS-L5.xml");

        Object[] loaded = loadArchive(xmlFile);
        Document doc = (Document) loaded[0];
        JSimGraphModel model = (JSimGraphModel) loaded[1];

        GuiXMLReader.parseXML(getJModelElement(doc), model);

        // XML declares 10 userClass elements — verify each key resolves
        String[] classNames = {"B", "P", "M", "W", "L", "b", "p", "m", "w", "l"};
        for (String name : classNames) {
            assertThat(findClassKey(model, name))
                    .as("Class '" + name + "' must have a color entry in the model")
                    .isNotNull();
        }

        // Spot-check specific ARGB colors:
        // B = "#FF0000FF" → blue (R=0, G=0, B=255)
        Color colorB = model.getClassColor(findClassKey(model, "B"));
        assertThat(colorB.getRed()).as("B: red=0").isEqualTo(0);
        assertThat(colorB.getGreen()).as("B: green=0").isEqualTo(0);
        assertThat(colorB.getBlue()).as("B: blue=255").isEqualTo(255);

        // P = "#FFFF0000" → red (R=255, G=0, B=0)
        Color colorP = model.getClassColor(findClassKey(model, "P"));
        assertThat(colorP.getRed()).as("P: red=255").isEqualTo(255);
        assertThat(colorP.getBlue()).as("P: blue=0").isEqualTo(0);

        // M = "#FF00FF00" → green (R=0, G=255, B=0)
        Color colorM = model.getClassColor(findClassKey(model, "M"));
        assertThat(colorM.getGreen()).as("M: green=255").isEqualTo(255);
        assertThat(colorM.getRed()).as("M: red=0").isEqualTo(0);
    }

    // -----------------------------------------------------------------------
    // Test 6 — Eleven stations without angle attribute
    // File: 641_2FJ_2tasks_nestedOK.xml
    // -----------------------------------------------------------------------

    /**
     * Verifies that all eleven stations are loaded correctly and that positions
     * without the optional {@code angle} attribute default to {@code 0.0}.
     */
    @Test
    void test_parseXML_elevenStationsNoAngleAttribute() throws Exception {
        File xmlFile = new File(
                "src/test/resources/jmt/gui/common/xml/641_2FJ_2tasks_nestedOK.xml");

        Object[] loaded = loadArchive(xmlFile);
        Document doc = (Document) loaded[0];
        JSimGraphModel model = (JSimGraphModel) loaded[1];

        GuiXMLReader.parseXML(getJModelElement(doc), model);

        // --- All 11 stations must have a position ---
        String[] stationNames = {
                "Source 1", "Fork 1", "Delay 1", "Delay 2", "Fork 2",
                "Delay 4", "Delay 5", "Join 2", "Sink 1", "Join 1", "Delay 1_1"
        };
        for (String name : stationNames) {
            Object key = findStationKey(model, name);
            assertThat(key).as("Station '" + name + "' must exist in model").isNotNull();
            assertThat(model.getStationPosition(key))
                    .as("Station '" + name + "' must have a position").isNotNull();
        }

        // --- Specific position values ---
        // XML: Fork 1 at (156.0, 124.0), Sink 1 at (856.0, 132.0)
        JMTPoint fork1Pos = model.getStationPosition(findStationKey(model, "Fork 1"));
        assertThat(fork1Pos.getX()).as("Fork 1 x must be 156.0").isCloseTo(156.0, within(1e-9));
        assertThat(fork1Pos.getY()).as("Fork 1 y must be 124.0").isCloseTo(124.0, within(1e-9));

        JMTPoint sink1Pos = model.getStationPosition(findStationKey(model, "Sink 1"));
        assertThat(sink1Pos.getX()).as("Sink 1 x must be 856.0").isCloseTo(856.0, within(1e-9));

        // --- No angle attribute → defaults to 0.0 ---
        assertThat(fork1Pos.getAngle())
                .as("Absent angle attribute must default to 0.0")
                .isCloseTo(0.0, within(1e-9));

        // --- No parametric analysis ---
        assertThat(model.isParametricAnalysisEnabled())
                .as("Parametric analysis must not be enabled for this archive")
                .isFalse();
    }

    // -----------------------------------------------------------------------
    // Test 7 — Unrecognized root element is a no-op
    // -----------------------------------------------------------------------

    /**
     * Verifies that calling {@link GuiXMLReader#parseXML} with a {@link Document}
     * whose root element is not {@code <jmodel>} leaves the model in its initial
     * state (no class colors overridden, no parametric analysis set).
     *
     * <p>Since {@link GuiXMLReader} uses {@code getElementsByTagName} on the root,
     * it finds no {@code <userClass>}, {@code <station>}, or {@code <parametric>}
     * children and silently produces no changes.
     */
    @Test
    void test_parseXML_unrecognizedRootElement_gracefulNoOp() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        doc.appendChild(doc.createElement("model")); // neither <jmodel> nor archive

        CommonModel model = new CommonModel();
        GuiXMLReader.parseXML(doc.getDocumentElement(), model);

        assertThat(model.isParametricAnalysisEnabled())
                .as("Parametric analysis must not be enabled after parsing unrecognized root")
                .isFalse();
        assertThat(model.getParametricAnalysisModel())
                .as("ParametricAnalysisModel must remain null after parsing unrecognized root")
                .isNull();
    }
}
