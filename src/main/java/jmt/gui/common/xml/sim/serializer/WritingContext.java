package jmt.gui.common.xml.sim.serializer;

import jmt.gui.common.definitions.CommonModel;

public class WritingContext {
    public final CommonModel model;
    // Mappa inversa stationKey → name già coperta da model.getStationName()
    // Se servono lookup aggiuntive (es. per Petri net backward connections)
    // si aggiungono qui, non nel mapper

    public WritingContext(CommonModel model) {
        this.model = model;
    }
}