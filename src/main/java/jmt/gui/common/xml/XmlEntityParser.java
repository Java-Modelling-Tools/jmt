package jmt.gui.common.xml;

import org.w3c.dom.Element;

/**
 * <p>Title: XML Entity Parser</p>
 * <p>Description: interface for entity parsers</p>
 *
 * @author Borgna Daniele
 *         Date: 25-mar-2026
 *
 */
public interface XmlEntityParser<T> {
    T parse(Element element);
}

