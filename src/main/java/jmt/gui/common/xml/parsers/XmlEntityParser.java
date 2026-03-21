package jmt.gui.common.xml.parsers;

import org.w3c.dom.Element;

public interface XmlEntityParser<T> {
    T parse(Element element);
}

