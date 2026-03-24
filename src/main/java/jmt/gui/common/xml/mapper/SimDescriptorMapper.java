package jmt.gui.common.xml.mapper;

import jmt.gui.common.xml.dto.SimDescriptor;
import jmt.gui.common.definitions.CommonModel;
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

        // Assembliamo la pipeline rispettando RIGOROSAMENTE l'ordine di OLDXMLReader
        pipeline.add(new GlobalSettingsMapper()); // setup path, maxTime, seed...
        pipeline.add(new ClassesMapper());        // parseClasses
        pipeline.add(new StationsMapper());       // parseStations (qui dentro useremo l'architettura a sezioni che abbiamo discusso prima!)
        pipeline.add(new ConnectionsMapper());    // parseConnections
        pipeline.add(new BlockingRegionsMapper());// parseBlockingRegions
        pipeline.add(new MeasuresMapper());       // parseMeasures
        pipeline.add(new PreloadMapper());        // parsePreloading
        pipeline.add(new PostProcessingMapper()); // Risolve refStations, empiricalRouting, fork, condizioni (l'ultimo blocco di parseXML)
    }

    public CommonModel map() {
        // Eseguiamo la pipeline: ogni mapper arricchisce il context e il CommonModel
        for (ModelElementMapper mapper : pipeline) {
            mapper.map(dto, context);
        }

        return context.getModel();
    }
}