package jmt.gui.common.xml.dto;

import java.util.ArrayList;
import java.util.List;

public class NodeDescriptor {

    private String name;
    private List<SectionDescriptor> sections = new ArrayList<SectionDescriptor>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SectionDescriptor> getSections() {
        return sections;
    }

    public void setSections(List<SectionDescriptor> sections) {
        this.sections = sections != null ? sections : new ArrayList<SectionDescriptor>();
    }

    public void addSection(SectionDescriptor section) {
        if (section != null) {
            sections.add(section);
        }
    }
}

