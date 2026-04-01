/**
 * Generic parser interface for XML entities. Implementations convert a DOM {@link org.w3c.dom.Element}
 * into a typed DTO.
 *
 * @author Borgna Daniele
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml;

import org.w3c.dom.Element;

public interface XmlEntityParser<T> {
    T parse(Element element);
}

