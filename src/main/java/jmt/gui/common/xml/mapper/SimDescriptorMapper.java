package jmt.gui.common.xml.mapper;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.SimDescriptor;

import java.util.ArrayList;
import java.util.List;

public class SimDescriptorMapper {

    private final SimDescriptor dto;
    private final MappingContext context;
    private final List<ModelElementMapper> pipeline;

    public SimDescriptorMapper(SimDescriptor dto, CommonModel model) {
        this.dto = dto;
        this.context = new MappingContext(model);
        this.pipeline = new ArrayList<>();


        // We assemble the pipeline STRICTLY respecting the order of OLDXMLReader
        pipeline.add(new GlobalSettingsMapper()); // setup path, maxTime, seed...
        pipeline.add(new ClassesMapper());        // parseClasses
        pipeline.add(new StationsMapper());       // parseStations (inside here we will use the section architecture we discussed before!)
        pipeline.add(new ConnectionsMapper());    // parseConnections
        pipeline.add(new BlockingRegionsMapper());// parseBlockingRegions
        pipeline.add(new MeasuresMapper());       // parseMeasures
        pipeline.add(new PreloadMapper());        // parsePreloading
        pipeline.add(new PostProcessingMapper()); // Resolves refStations, empiricalRouting, fork, conditions (the last block of parseXML)
    }

    public CommonModel map() {
        // We execute the pipeline: each mapper enriches the context and the CommonModel
        for (ModelElementMapper mapper : pipeline) {
            mapper.map(dto, context);
        }

        return context.getModel();
    }
}