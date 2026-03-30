/**
 * Bidirectional mapper between a {@link ResultsDescriptor} DTO and the live
 * {@link jmt.gui.common.definitions.MeasureDefinition} hierarchy
 * ({@link jmt.gui.common.definitions.StoredResultsModel} /
 * {@link jmt.gui.common.definitions.PAResultsModel}).
 *
 * <p><b>Reading phase</b> ({@link #mapDtoToModel}): iterates the measure list in the
 * descriptor and calls the appropriate {@code addMeasure} / {@code addSample} overload
 * on the target model, replicating exactly the logic that was inlined inside
 * {@code XMLResultsReader}.
 *
 * <p><b>Writing phase</b> ({@link #mapModelToDto}): queries the
 * {@link jmt.gui.common.definitions.MeasureDefinition} API to reconstruct a
 * {@link ResultsDescriptor} ready for serialization.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.results.mapper;

import jmt.engine.simEngine.EngineUtils;
import jmt.framework.gui.graph.MeasureValue;
import jmt.gui.common.definitions.MeasureDefinition;
import jmt.gui.common.definitions.PAResultsModel;
import jmt.gui.common.definitions.StoredResultsModel;
import jmt.gui.common.xml.results.dto.ResultMeasureDescriptor;
import jmt.gui.common.xml.results.dto.ResultsDescriptor;
import jmt.gui.common.xml.results.dto.SampleDescriptor;

import java.util.List;

public class ResultsDescriptorMapper {

    // -----------------------------------------------------------------------
    // Reading phase  (DTO → Model)
    // -----------------------------------------------------------------------

    /**
     * Populates the given {@link MeasureDefinition} model from the parsed
     * {@link ResultsDescriptor} DTO.
     *
     * @param dto   the DTO produced by {@link jmt.gui.common.xml.results.parsers.ResultsParser}
     * @param model the target model; must be a {@link StoredResultsModel} or a
     *              {@link PAResultsModel}
     */
    public void mapDtoToModel(ResultsDescriptor dto, MeasureDefinition model) {
        if (model instanceof PAResultsModel) {
            mapDtoToParametricModel(dto, (PAResultsModel) model);
        } else if (model instanceof StoredResultsModel) {
            mapDtoToStoredModel(dto, (StoredResultsModel) model);
        }
    }

    private void mapDtoToParametricModel(ResultsDescriptor dto, PAResultsModel model) {
        model.setLogDecimalSeparator(dto.getLogDecimalSeparator());
        model.setLogCsvDelimiter(dto.getLogCsvDelimiter());

        for (ResultMeasureDescriptor m : dto.getMeasures()) {
            model.addMeasure(
                    m.getName(),
                    m.getStationName(),
                    m.getClassName(),
                    m.getAlpha(),
                    m.getPrecision(),
                    m.getType(),
                    m.getNodeType());

            for (SampleDescriptor s : m.getSamples()) {
                model.addSample(m.getName(), s.getLowerBound(), s.getMean(), s.getUpperBound(), s.isValid());
            }
        }
    }

    private void mapDtoToStoredModel(ResultsDescriptor dto, StoredResultsModel model) {
        model.setPollingInterval(dto.getPollingInterval());
        model.setElapsedTime(dto.getElapsedTime());
        model.setLogDecimalSeparator(dto.getLogDecimalSeparator());
        model.setLogCsvDelimiter(dto.getLogCsvDelimiter());

        for (ResultMeasureDescriptor m : dto.getMeasures()) {
            model.addMeasure(
                    m.getName(),
                    m.getStationName(),
                    m.getClassName(),
                    m.getAlpha(),
                    m.getPrecision(),
                    m.getAnalyzedSamples(),
                    m.getDiscardedSamples(),
                    m.getState(),
                    m.getType(),
                    m.getNodeType(),
                    m.getLogFileName());

            for (SampleDescriptor s : m.getSamples()) {
                model.addMeasureSample(
                        m.getName(),
                        s.getLastIntervalAvgValue(),
                        s.getSimulationTime(),
                        s.getMean(),
                        s.getUpperBound(),
                        s.getLowerBound());
            }
        }
    }

    // -----------------------------------------------------------------------
    // Writing phase  (Model → DTO)
    // -----------------------------------------------------------------------

    /**
     * Builds a {@link ResultsDescriptor} DTO from the given {@link MeasureDefinition}.
     * The resulting DTO is in GUI format ({@code Results.xsd}), regardless of whether
     * the source model is normal or parametric.
     *
     * @param model the fully-populated results model
     * @return a populated {@link ResultsDescriptor} ready for serialization
     */
    public ResultsDescriptor mapModelToDto(MeasureDefinition model) {
        ResultsDescriptor dto = new ResultsDescriptor();
        dto.setPollingInterval(model.getPollingInterval());
        dto.setElapsedTime(model.getElapsedTime());
        dto.setLogDecimalSeparator(model.getLogDecimalSeparator());
        dto.setLogCsvDelimiter(model.getLogCsvDelimiter());

        boolean parametric = model instanceof PAResultsModel;
        dto.setParametric(parametric);

        for (int i = 0; i < model.getMeasureNumber(); i++) {
            dto.addMeasure(parametric
                    ? buildParametricMeasureDto(model, i)
                    : buildNormalMeasureDto(model, i));
        }
        return dto;
    }

    private ResultMeasureDescriptor buildNormalMeasureDto(MeasureDefinition model, int i) {
        ResultMeasureDescriptor m = new ResultMeasureDescriptor();
        m.setParametric(false);
        m.setName(model.getName(i));
        m.setType(Integer.toString(EngineUtils.decodeMeasureType(model.getMeasureType(i))));
        m.setClassName(model.getClassName(i));
        m.setStationName(model.getStationName(i));
        m.setNodeType(model.getNodeType(i));
        m.setAlpha(model.getAlpha(i));
        m.setPrecision(model.getPrecision(i));
        m.setAnalyzedSamples(model.getAnalyzedSamples(i));
        m.setDiscardedSamples(model.getDiscardedSamples(i));
        m.setState(model.getMeasureState(i));
        m.setLogFileName(model.getLogFileName(i));

        List<MeasureValue> values = model.getValues(i);

        if (values != null && !values.isEmpty()) {
            MeasureValue lastMeasure = values.get(values.size() - 1);

            if (lastMeasure != null) {
                m.setFinalValue(lastMeasure.getMeanValue());
            }
        }

        if (values != null) {
            for (MeasureValue v : values) {
                SampleDescriptor s = new SampleDescriptor();
                s.setMean(v.getMeanValue());
                s.setLastIntervalAvgValue(v.getLastIntervalAvgValue());
                s.setSimulationTime(v.getSimTime());
                double lb = v.getLowerBound();
                double ub = v.getUpperBound();
                if (lb > 0 && !Double.isInfinite(lb)) s.setLowerBound(lb);
                if (ub > 0 && !Double.isInfinite(ub)) s.setUpperBound(ub);
                m.addSample(s);
            }
        }
        return m;
    }

    private ResultMeasureDescriptor buildParametricMeasureDto(MeasureDefinition model, int i) {
        ResultMeasureDescriptor m = new ResultMeasureDescriptor();
        m.setParametric(true);
        m.setName(model.getName(i));
        m.setType(Integer.toString(EngineUtils.decodeMeasureType(model.getMeasureType(i))));
        m.setClassName(model.getClassName(i));
        m.setStationName(model.getStationName(i));
        m.setNodeType(model.getNodeType(i));
        m.setAlpha(model.getAlpha(i));
        m.setPrecision(model.getPrecision(i));
        m.setAnalyzedSamples(model.getAnalyzedSamples(i));
        m.setLogFileName(model.getLogFileName(i));

        List<MeasureValue> values = model.getValues(i);
        if (values != null) {
            for (MeasureValue v : values) {
                PAResultsModel.MeasureValueImpl pv = (PAResultsModel.MeasureValueImpl) v;
                SampleDescriptor s = new SampleDescriptor();
                s.setMean(pv.getMeanValue());
                s.setUpperBound(pv.getUpperBound());
                s.setLowerBound(pv.getLowerBound());
                s.setValid(pv.isValid());
                m.addSample(s);
            }
        }
        return m;
    }
}