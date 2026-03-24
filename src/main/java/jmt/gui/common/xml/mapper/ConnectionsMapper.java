package jmt.gui.common.xml.mapper;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.ConnectionDescriptor;
import jmt.gui.common.xml.dto.SimDescriptor;

public class ConnectionsMapper implements ModelElementMapper {

    @Override
    public void map(SimDescriptor dto, MappingContext context) {
        if (dto.getConnections() == null || dto.getConnections().isEmpty()) {
            return;
        }

        CommonModel model = context.getModel();

        for (ConnectionDescriptor conn : dto.getConnections()) {
            String sourceName = conn.getSource();
            String targetName = conn.getTarget();

            // 1. Cerca il nodo sorgente: prima tra le stazioni, poi tra le regioni
            Object sourceKey = context.stations.get(sourceName);
            if (sourceKey == null) {
                sourceKey = context.regions.get(sourceName);
            }

            // 2. Cerca il nodo di destinazione: prima tra le stazioni, poi tra le regioni
            Object targetKey = context.stations.get(targetName);
            if (targetKey == null) {
                targetKey = context.regions.get(targetName);
            }

            // 3. Se entrambi i nodi esistono nel modello, attiva la connessione tramite setConnected
            if (sourceKey != null && targetKey != null) {
                model.setConnected(sourceKey, targetKey, true);
            }
        }
    }
}