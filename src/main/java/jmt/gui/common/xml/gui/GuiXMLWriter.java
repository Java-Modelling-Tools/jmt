/**
 * <p>Title: Gui XML Writer</p>
 * <p>Description: Refactored replacement for the legacy {@code GuiXMLWriter}.
 * Writes JModel GUI-specific information to XML using the two-phase
 * Map → Serialize pipeline.</p>
 *
 * <p>Phase 1 — {@link GuiDescriptorMapper}: {@link CommonModel} → {@link GuiDescriptor}</p>
 * <p>Phase 2 — {@link GuiSerializer}: {@link GuiDescriptor} → DOM {@link Document}</p>
 *
 * @author Borgna Daniele (Refactoring)
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.gui;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.GuiXMLConstants;
import jmt.gui.common.xml.gui.dto.GuiDescriptor;
import jmt.gui.common.xml.gui.mapper.GuiDescriptorMapper;
import jmt.gui.common.xml.gui.serializer.GuiSerializer;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.OutputStream;

public class GuiXMLWriter implements GuiXMLConstants {

    private GuiXMLWriter() { /* utility class */ }

    // -----------------------------------------------------------------------
    // Public write methods (mirror the legacy API)
    // -----------------------------------------------------------------------

    public static void writeXML(String fileName, CommonModel model) {
        writeToResult(new StreamResult(new File(fileName)), model);
    }

    public static void writeXML(File xmlFile, CommonModel model) {
        writeToResult(new StreamResult(xmlFile), model);
    }

    public static void writeXML(OutputStream out, CommonModel model) {
        writeToResult(new StreamResult(out), model);
    }

    /**
     * Builds the DOM {@link Document} from the given {@link CommonModel}.
     * Called by {@link jmt.gui.common.xml.XMLArchiver} when assembling the archive root.
     */
    public static Document getDocument(CommonModel model) {
        // Phase 1: map model → DTO
        GuiDescriptorMapper mapper = new GuiDescriptorMapper();
        GuiDescriptor dto = mapper.mapModelToDto(model);

        // Phase 2: serialize DTO → DOM Document
        GuiSerializer serializer = new GuiSerializer();
        return serializer.serialize(dto);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private static void writeToResult(Result result, CommonModel model) {
        Document doc = getDocument(model);
        if (doc == null) return;
        try {
            Transformer transformer = createSecureTransformer();
            transformer.transform(new DOMSource(doc), result);
        } catch (TransformerFactoryConfigurationError | TransformerException e) {
            e.printStackTrace();
        }
    }

    private static Transformer createSecureTransformer() throws TransformerConfigurationException {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        return transformer;
    }
}