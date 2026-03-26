/**
 * This class is responsible for mapping {@link ConnectionDescriptor} objects from the DTO to the {@link CommonModel}.
 * It iterates through the connections defined in the {@link SimDescriptor} and establishes the links between nodes in the model.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.mapper;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.ConnectionDescriptor;
import jmt.gui.common.xml.dto.SimDescriptor;

public class ConnectionsMapper implements ModelElementMapper {

    @Override
    public void mapDtoToCommonModel(SimDescriptor dto, MappingContext context) {
        if (dto.getConnections() == null || dto.getConnections().isEmpty()) {
            return;
        }

        for (ConnectionDescriptor conn : dto.getConnections()) {
            mapConnection(conn, context);
        }
    }

    @Override
    public void mapCommonModelToDto(SimDescriptor dto, MappingContext context) {
        // TODO: implement logic to map common model to dto
    }

    private void mapConnection(ConnectionDescriptor conn, MappingContext context) {
        CommonModel model = context.getModel();
        String sourceName = conn.getSource();
        String targetName = conn.getTarget();

        // 1. Search for the source node: first among the stations, then among the regions
        Object sourceKey = resolveStationOrRegion(sourceName, context);

        // 2. Search for the destination node: first among the stations, then among the regions
        Object targetKey = resolveStationOrRegion(targetName, context);

        // 3. If both nodes exist in the model, activate the connection via setConnected
        if (sourceKey != null && targetKey != null) {
            model.setConnected(sourceKey, targetKey, true);
        }
    }
}