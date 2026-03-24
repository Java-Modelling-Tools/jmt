package jmt.gui.common.xml.mapper;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.MeasureDescriptor;
import jmt.gui.common.xml.dto.SimDescriptor;

public class MeasuresMapper implements ModelElementMapper {

    @Override
    public void map(SimDescriptor dto, MappingContext context) {
        if (dto.getMeasures() == null || dto.getMeasures().isEmpty()) return;

        CommonModel model = context.getModel();

        for (MeasureDescriptor m : dto.getMeasures()) {
            Object stationKey = null;
            if (m.getReferenceNode() != null && !m.getReferenceNode().isEmpty()) {
                stationKey = context.stations.get(m.getReferenceNode());
                if (stationKey == null) stationKey = context.regions.get(m.getReferenceNode());
            }

            Object classKey = null;
            if (m.getReferenceUserClass() != null && !m.getReferenceUserClass().isEmpty()) {
                classKey = context.classes.get(m.getReferenceUserClass());
            }

            Double alpha = 0.05;
            if (m.getAlpha() != null && !m.getAlpha().isEmpty()) {
                // FIX: L'OLDXMLReader fa "1 - alpha" incondizionatamente su tutto
                alpha = 1.0 - Double.parseDouble(m.getAlpha());
            }

            Double precision = 0.03;
            if (m.getPrecision() != null && !m.getPrecision().isEmpty()) {
                precision = Double.parseDouble(m.getPrecision());
            }

            boolean verbose = Boolean.parseBoolean(m.getVerbose());
            model.addMeasure(m.getType(), stationKey, classKey, alpha, precision, verbose);
        }
    }
}