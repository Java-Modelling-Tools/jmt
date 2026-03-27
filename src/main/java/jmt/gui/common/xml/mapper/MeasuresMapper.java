/**
 * This class is responsible for mapping {@link MeasureDescriptor} objects from the DTO to the {@link CommonModel}
 * and vice-versa. It iterates through the measures defined in the {@link SimDescriptor} and adds them to the model,
 * or extracts them from the model to build the DTO.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.mapper;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.MeasureDescriptor;
import jmt.gui.common.xml.dto.SimDescriptor;
import jmt.gui.common.xml.serializer.WritingContext;

import java.util.List;
import java.util.Vector;

public class MeasuresMapper implements ModelElementMapper {

    // --- READING PHASE (XML -> DTO -> Model) ---

    @Override
    public void mapDtoToCommonModel(SimDescriptor dto, MappingContext context) {
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

        Object key = model.addMeasure(m.getType(), stationKey, classKey, alpha, precision, verbose);

        // Add server type to model if it exists
        if (stationKey != null && isPresent(m.getServerType())) {
            model.setMeasureServerTypeKey(model.getServerTypeKey(stationKey, m.getServerType()), key);
        }
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

    // --- WRITING PHASE (Model -> DTO -> XML) ---

    @Override
    public void mapCommonModelToDto(SimDescriptor dto, WritingContext context) {
        CommonModel model = context.model;
        Vector<Object> measureKeys = model.getMeasureKeys();

        if (measureKeys == null || measureKeys.isEmpty()) {
            return;
        }

        for (Object measureKey : measureKeys) {
            MeasureDescriptor measureDto = new MeasureDescriptor();
            mapModelMeasureToDto(model, measureKey, measureDto);
            dto.addMeasure(measureDto);
        }
    }

    private void mapModelMeasureToDto(CommonModel model, Object measureKey, MeasureDescriptor measureDto) {
        // Measure Type
        measureDto.setType(model.getMeasureType(measureKey));

        // Node Reference and Node Type
        Object stationKey = model.getMeasureStation(measureKey);
        if (stationKey != null) {
            String stationName = model.getStationName(stationKey);
            if (stationName != null) {
                measureDto.setReferenceNode(stationName);
                measureDto.setNodeType("station"); // default logic
            } else {
                String regionName = model.getRegionName(stationKey);
                if (regionName != null) {
                    measureDto.setReferenceNode(regionName);
                    measureDto.setNodeType("region");
                }
            }
        }

        // Class Reference (or Firing Mode name)
        Object classKey = model.getMeasureClass(measureKey);
        if (classKey != null) {
            if (classKey instanceof String) {
                // If it's a string, it represents a transition mode name for firing measures
                measureDto.setReferenceUserClass((String) classKey);
            } else {
                // Otherwise, it's a standard class key
                String className = model.getClassName(classKey);
                if (className != null) {
                    measureDto.setReferenceUserClass(className);
                }
            }
        }

        // Alpha (Confidence Interval)
        Double alpha = model.getMeasureAlpha(measureKey);
        if (alpha != null) {
            // Reverse the calculation from the reading phase
            double xmlAlpha = 1.0 - alpha;
            measureDto.setAlpha(String.valueOf(xmlAlpha));
        }

        // Precision
        Double precision = model.getMeasurePrecision(measureKey);
        if (precision != null) {
            measureDto.setPrecision(precision.toString());
        }

        // Verbose (Log)
        boolean verbose = model.getMeasureLog(measureKey);
        measureDto.setVerbose(String.valueOf(verbose));

        // Server Type
        Object serverTypeKey = model.getMeasureServerTypeKey(measureKey);
        if (serverTypeKey != null && stationKey != null) {
            // We find the matching server type name using the model's exposed string lists
            List<String> serverTypeNames = model.getServerTypeNames(stationKey);
            if (serverTypeNames != null) {
                for (String stName : serverTypeNames) {
                    if (serverTypeKey.equals(model.getServerTypeKey(stationKey, stName))) {
                        measureDto.setServerType(stName);
                        break;
                    }
                }
            }
        }
    }
}