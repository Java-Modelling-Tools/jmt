/**
 * This class orchestrates the entire mapping process between the {@link SimDescriptor} DTO
 * and the {@link CommonModel} in both directions.
 *
 * @author Borgna Daniele
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml.sim.mapper;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.sim.dto.SimDescriptor;
import jmt.gui.common.xml.sim.serializer.WritingContext;

import java.util.ArrayList;
import java.util.List;

public class SimDescriptorMapper {

    private final SimDescriptor dto;
    private MappingContext mappingContext;
    private WritingContext writingContext;
    private final List<ModelElementMapper> pipeline;

    /**
     * Constructor for the READING phase (Parsing: XML -> DTO -> Model).
     * Takes as input the DTO already populated by the XML parser.
     * * @param dto   The populated SimDescriptor DTO.
     * @param model The CommonModel to be enriched.
     */
    public SimDescriptorMapper(SimDescriptor dto, CommonModel model) {
        this.dto = dto;
        this.mappingContext = new MappingContext(model);
        this.pipeline = buildPipeline();
    }

    /**
     * Constructor for the WRITING phase (Writing: Model -> DTO -> XML).
     * Initializes an empty SimDescriptor that will be populated by the mappers.
     * * @param model The fully populated CommonModel.
     */
    public SimDescriptorMapper(CommonModel model) {
        this.dto = new SimDescriptor();
        this.writingContext = new WritingContext(model);
        this.pipeline = buildPipeline();
    }

    private List<ModelElementMapper> buildPipeline() {
        List<ModelElementMapper> p = new ArrayList<>();
        // Assembling the pipeline strictly respecting the logical order
        p.add(new GlobalSettingsMapper()); // setup path, maxTime, seed...
        p.add(new ClassesMapper());        // parseClasses
        p.add(new StationsMapper());       // parseStations
        p.add(new ConnectionsMapper());    // parseConnections
        p.add(new BlockingRegionsMapper());// parseBlockingRegions
        p.add(new MeasuresMapper());       // parseMeasures
        p.add(new PreloadMapper());        // parsePreloading
        p.add(new PostProcessingMapper()); // Resolves refStations, empiricalRouting, fork, conditions
        return p;
    }

    /**
     * Executes the pipeline in READING mode: populates the CommonModel starting from the DTO.
     * * @return The populated CommonModel.
     * @throws IllegalStateException if the mapper was initialized for writing.
     */
    public CommonModel mapToModel() {
        if (mappingContext == null) {
            throw new IllegalStateException("Mapper was initialized for writing, cannot map to model.");
        }
        for (ModelElementMapper mapper : pipeline) {
            mapper.mapDtoToCommonModel(dto, mappingContext);
        }
        return mappingContext.getModel();
    }

    /**
     * Executes the pipeline in WRITING mode: populates the SimDescriptor starting from the CommonModel.
     * * @return The populated SimDescriptor DTO ready for XML serialization.
     * @throws IllegalStateException if the mapper was initialized for reading.
     */
    public SimDescriptor mapToDto() {
        if (writingContext == null) {
            throw new IllegalStateException("Mapper was initialized for reading, cannot map to DTO.");
        }
        for (ModelElementMapper mapper : pipeline) {
            mapper.mapCommonModelToDto(dto, writingContext);
        }
        return dto;
    }
}