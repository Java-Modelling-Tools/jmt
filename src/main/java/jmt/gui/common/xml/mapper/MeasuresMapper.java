package jmt.gui.common.xml.mapper;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.MeasureDescriptor;
import jmt.gui.common.xml.dto.SimDescriptor;

import java.util.List;

public class MeasuresMapper implements ModelElementMapper {

    @Override
    public void map(SimDescriptor dto, MappingContext context) {
        if (dto.getMeasures() == null || dto.getMeasures().isEmpty()) return;
        CommonModel model = context.getModel();

        for (MeasureDescriptor m : dto.getMeasures()) {
            Object stationKey = null;
            String nodeType = m.getNodeType();
            String refNode = m.getReferenceNode();

            // Identify the reference station or region
            if (refNode != null && !refNode.isEmpty()) {
                if ("region".equals(nodeType)) {
                    stationKey = context.regions.get(refNode);
                } else {
                    stationKey = context.stations.get(refNode);
                }
            }

            Object classKey = null;
            String className = m.getReferenceUserClass();
            String type = m.getType();

            if (className != null && !className.isEmpty()) {
                // FIX ORACLE: If it is an FX measure (Firing Throughput), the "class" is actually a Transition Mode.
                if (type != null && (type.equals("Firing Throughput") || type.equals("Fire Frequency") || type.endsWith("FX"))) {
                    if (stationKey != null && context.modeNameLists != null) {
                        List<String> modeNameList = context.modeNameLists.get(stationKey);
                        if (modeNameList != null && modeNameList.contains(className)) {
                            // Like the Oracle: it gets it from the modeNameLists
                            classKey = modeNameList.get(modeNameList.indexOf(className));
                        } else {
                            classKey = className; // Safety fallback
                        }
                    } else {
                        classKey = className;
                    }
                } else {
                    classKey = context.classes.get(className);
                }
            }

            // Inverts alpha (e.g.: 0.10 -> 0.90) as in OLDXMLReader
            Double alpha = 0.05;
            if (m.getAlpha() != null && !m.getAlpha().isEmpty()) {
                alpha = 1.0 - Double.parseDouble(m.getAlpha());
            }

            Double precision = 0.03;
            if (m.getPrecision() != null && !m.getPrecision().isEmpty()) {
                precision = Double.parseDouble(m.getPrecision());
            }

            boolean verbose = Boolean.parseBoolean(m.getVerbose());

            // Adds the measure to the model
            model.addMeasure(type, stationKey, classKey, alpha, precision, verbose);
        }
    }
}