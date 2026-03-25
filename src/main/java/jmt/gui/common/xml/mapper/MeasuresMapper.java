package jmt.gui.common.xml.mapper;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.MeasureDescriptor;
import jmt.gui.common.xml.dto.SimDescriptor;

import java.util.List;

public class MeasuresMapper implements ModelElementMapper {

    @Override
    public void map(SimDescriptor dto, MappingContext context) {
        if (dto.getMeasures() == null || dto.getMeasures().isEmpty()) return;

        for (MeasureDescriptor m : dto.getMeasures()) {
            mapMeasure(m, context);
        }
    }

    private void mapMeasure(MeasureDescriptor m, MappingContext context) {
        CommonModel model = context.getModel();
        Object stationKey = resolveStationKey(m, context);
        Object classKey = resolveClassKey(m, stationKey, context);

        Double alpha = 1.0 - getDouble(m.getAlpha(), 0.95); // Default alpha in XML is 0.05, so 1-0.05 = 0.95
        Double precision = getDouble(m.getPrecision(), 0.03);
        boolean verbose = Boolean.parseBoolean(m.getVerbose());

        model.addMeasure(m.getType(), stationKey, classKey, alpha, precision, verbose);
    }

    private Object resolveStationKey(MeasureDescriptor m, MappingContext context) {
        String refNode = m.getReferenceNode();
        if (isPresent(refNode)) {
            if ("region".equals(m.getNodeType())) {
                return context.regions.get(refNode);
            } else {
                return context.stations.get(refNode);
            }
        }
        return null;
    }

    private Object resolveClassKey(MeasureDescriptor m, Object stationKey, MappingContext context) {
        String className = m.getReferenceUserClass();
        String type = m.getType();

        if (!isPresent(className)) {
            return null;
        }

        if (isFiringMeasure(type)) {
            return resolveFiringClassKey(className, stationKey, context);
        } else {
            return context.classes.get(className);
        }
    }

    private boolean isFiringMeasure(String type) {
        return type != null && (type.equals("Firing Throughput") || type.equals("Fire Frequency") || type.endsWith("FX"));
    }

    private Object resolveFiringClassKey(String className, Object stationKey, MappingContext context) {
        if (stationKey != null && context.modeNameLists != null) {
            List<String> modeNameList = context.modeNameLists.get(stationKey);
            if (modeNameList != null && modeNameList.contains(className)) {
                return modeNameList.get(modeNameList.indexOf(className));
            }
        }
        return className; // Safety fallback
    }
}