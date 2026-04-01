/**
 * <p>Title: Gui XML Reader</p>
 * <p>Description: Refactored replacement for the legacy {@code GuiXMLReader}.
 * Reads JModel GUI-specific information from the {@code JModelGUI.xsd} section of an
 * archive using the two-phase Parse → Map pipeline.</p>
 *
 * <p>Phase 1 — {@link jmt.gui.common.xml.parsers.GuiParser}: DOM → {@link GuiDescriptor}</p>
 * <p>Phase 2 — {@link GuiDescriptorMapper}: {@link GuiDescriptor} → {@link CommonModel}</p>
 *
 * <p>This class must be called <em>after</em> the simulation data has been loaded into
 * the {@link CommonModel}, as it resolves station and class names against keys that the
 * sim-layer mapper already created.</p>
 *
 * @author Borgna Daniele (Refactoring)
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml.gui;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.GuiXMLConstants;
import jmt.gui.common.xml.gui.dto.GuiDescriptor;
import jmt.gui.common.xml.gui.mapper.GuiDescriptorMapper;
import jmt.gui.common.xml.gui.parsers.GuiParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class GuiXMLReader implements GuiXMLConstants {

    private GuiXMLReader() { /* utility class */ }

    /**
     * Parses the GUI XML {@link Document} and applies its metadata to the given model.
     */
    public static void parseXML(Document xml, CommonModel model) {
        if (xml != null) parseXML(xml.getDocumentElement(), model);
    }

    /**
     * Parses the GUI XML root {@link Element} and applies its metadata to the given
     * model. The root element must carry the {@code jmodel} tag name.
     */
    public static void parseXML(Element root, CommonModel model) {
        if (root == null || model == null) return;
        if (!XML_DOCUMENT_ROOT.equals(root.getTagName())) return;

        // Phase 1: parse DOM → DTO
        GuiParser parser = new GuiParser();
        GuiDescriptor dto = parser.parse(root);

        // Phase 2: map DTO → model
        GuiDescriptorMapper mapper = new GuiDescriptorMapper();
        mapper.mapDtoToModel(dto, model);
    }
}