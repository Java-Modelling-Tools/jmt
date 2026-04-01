package jmt.gui.common.xml.sim.mapper;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.sim.dto.MeasureDescriptor;
import jmt.gui.common.xml.sim.dto.SimDescriptor;
import jmt.gui.common.xml.sim.serializer.WritingContext;

import java.util.List;

/**
 * Maps performance measure descriptors between the {@link SimDescriptor} DTO and
 * the {@link CommonModel}, in both parsing and serialization directions.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
public class MeasuresMapper implements ModelElementMapper {


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

        Double alpha = 1.0 - getDouble(m.getAlpha(), 0.95);
        Double precision = getDouble(m.getPrecision(), 0.03);
        boolean verbose = Boolean.parseBoolean(m.getVerbose());

        Object key = model.addMeasure(m.getType(), stationKey, classKey, alpha, precision, verbose);

        if (stationKey != null && isPresent(m.getServerType())) {
            model.setMeasureServerTypeKey(model.getServerTypeKey(stationKey, m.getServerType()), key);
        }
    }

    private Object resolveStationKey(MeasureDescriptor m, MappingContext context) {
        String refNode = m.getReferenceNode();
        if (isPresent(refNode)) {
            if (NODETYPE_REGION.equals(m.getNodeType())) return context.regions.get(refNode);
            else return context.stations.get(refNode);
        }
        return null;
    }

    private Object resolveClassKey(MeasureDescriptor m, Object stationKey, MappingContext context) {
        String className = m.getReferenceUserClass();
        String type = m.getType();
        if (!isPresent(className)) return null;

        if (isFiringMeasure(type)) return resolveFiringClassKey(className, stationKey, context);
        else return context.classes.get(className);
    }

    private boolean isFiringMeasure(String type) {
        return type != null && (type.equals(MEASURE_TYPE_FIRING_THROUGHPUT) || type.equals(MEASURE_TYPE_FIRE_FREQUENCY) || type.endsWith(MEASURE_TYPE_FX));
    }

    private Object resolveFiringClassKey(String className, Object stationKey, MappingContext context) {
        if (stationKey != null && context.modeNameLists != null) {
            List<String> modeNameList = context.modeNameLists.get(stationKey);
            if (modeNameList != null && modeNameList.contains(className)) {
                return modeNameList.get(modeNameList.indexOf(className));
            }
        }
        return className;
    }

    @Override
    public void mapCommonModelToDto(SimDescriptor dto, WritingContext context) {
        CommonModel model = context.model;
        List<Object> measureKeys = model.getMeasureKeys();
        if (measureKeys == null || measureKeys.isEmpty()) return;

        for (Object measureKey : measureKeys) {
            MeasureDescriptor measureDto = new MeasureDescriptor();
            mapModelMeasureToDto(model, measureKey, measureDto);
            dto.addMeasure(measureDto);
        }
    }

    private void mapModelMeasureToDto(CommonModel model, Object measureKey, MeasureDescriptor measureDto) {
        String measureType = model.getMeasureType(measureKey);
        measureDto.setType(measureType);

        Object stationKey = model.getMeasureStation(measureKey);
        String refNodeName = resolveAndSetNodeAttributes(model, stationKey, measureDto);
        String refClassName = resolveReferenceClassName(model, measureKey);

        measureDto.setReferenceNode(refNodeName);
        measureDto.setReferenceUserClass(refClassName);
        measureDto.setName(buildMeasureName(refNodeName, refClassName, measureType));

        setNumericAndBooleanAttributes(model, measureKey, measureDto);

        String serverType = resolveServerType(model, measureKey, stationKey);
        if (serverType != null) {
            measureDto.setServerType(serverType);
        }
    }

    private String resolveAndSetNodeAttributes(CommonModel model, Object stationKey, MeasureDescriptor measureDto) {
        measureDto.setNodeType(NODETYPE_STATION);
        if (stationKey == null) return "";

        String stationName = model.getStationName(stationKey);
        if (stationName != null) {
            return stationName;
        }

        String regionName = model.getRegionName(stationKey);
        if (regionName != null) {
            measureDto.setNodeType(NODETYPE_REGION);
            return regionName;
        }

        return "";
    }

    private String resolveReferenceClassName(CommonModel model, Object measureKey) {
        Object classKey = model.getMeasureClass(measureKey);
        if (classKey == null) return "";

        if (classKey instanceof String) {
            return (String) classKey;
        }

        String className = model.getClassName(classKey);
        return className != null ? className : "";
    }

    private String buildMeasureName(String refNodeName, String refClassName, String measureType) {
        String measureName = "";
        if (!refNodeName.isEmpty()) {
            measureName += refNodeName + XML_MEASURE_NAME_SEPARATOR;
        }
        if (!refClassName.isEmpty()) {
            measureName += refClassName + XML_MEASURE_NAME_SEPARATOR;
        }
        return measureName + measureType;
    }

    private void setNumericAndBooleanAttributes(CommonModel model, Object measureKey, MeasureDescriptor measureDto) {
        Double alpha = model.getMeasureAlpha(measureKey);
        if (alpha != null) {
            double xmlAlpha = 1.0 - alpha;
            measureDto.setAlpha(String.valueOf(xmlAlpha));
        }

        Double precision = model.getMeasurePrecision(measureKey);
        if (precision != null) {
            measureDto.setPrecision(precision.toString());
        }

        boolean verbose = model.getMeasureLog(measureKey);
        measureDto.setVerbose(String.valueOf(verbose));
    }

    private String resolveServerType(CommonModel model, Object measureKey, Object stationKey) {
        Object serverTypeKey = model.getMeasureServerTypeKey(measureKey);
        if (serverTypeKey == null || stationKey == null) return null;

        List<String> serverTypeNames = model.getServerTypeNames(stationKey);
        if (serverTypeNames == null) return null;

        for (String stName : serverTypeNames) {
            if (serverTypeKey.equals(model.getServerTypeKey(stationKey, stName))) {
                return stName;
            }
        }
        return null;
    }
}