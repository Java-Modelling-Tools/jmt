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

            // 1. Search for the source node: first among the stations, then among the regions
            Object sourceKey = context.stations.get(sourceName);
            if (sourceKey == null) {
                sourceKey = context.regions.get(sourceName);
            }

            // 2. Search for the destination node: first among the stations, then among the regions
            Object targetKey = context.stations.get(targetName);
            if (targetKey == null) {
                targetKey = context.regions.get(targetName);
            }

            // 3. If both nodes exist in the model, activate the connection via setConnected
            if (sourceKey != null && targetKey != null) {
                model.setConnected(sourceKey, targetKey, true);
            }
        }
    }
}