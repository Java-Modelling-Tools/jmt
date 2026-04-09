/**
 * This class is responsible for mapping {@link ConnectionDescriptor} objects from the DTO to the {@link CommonModel}
 * and vice-versa. It iterates through the connections defined in the {@link SimDescriptor} and establishes the links
 * between nodes in the model, or retrieves them from the model to build the DTO.
 *
 * @author Borgna Daniele
 *         Date: 25-mar-2026
 */
package jmt.gui.common.xml.sim.mapper;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.sim.dto.ConnectionDescriptor;
import jmt.gui.common.xml.sim.dto.SimDescriptor;
import jmt.gui.common.xml.sim.serializer.WritingContext;

import java.util.List;


public class ConnectionsMapper implements ModelElementMapper {

    // --- READING PHASE (XML -> DTO -> Model) ---

    @Override
    public void mapDtoToCommonModel(SimDescriptor dto, MappingContext context) {
        if (dto.getConnections() == null || dto.getConnections().isEmpty()) {
            return;
        }

        for (ConnectionDescriptor conn : dto.getConnections()) {
            mapConnection(conn, context);
        }
    }

    private void mapConnection(ConnectionDescriptor conn, MappingContext context) {
        CommonModel model = context.getModel();
        String sourceName = conn.getSource();
        String targetName = conn.getTarget();

        // Legacy-compat: connections are resolved only among stations.
        Object sourceKey = context.stations.get(sourceName);
        Object targetKey = context.stations.get(targetName);

        if (sourceKey != null && targetKey != null) {
            model.setConnected(sourceKey, targetKey, true);
        }
    }

    // --- WRITING PHASE (Model -> DTO -> XML) ---

    @Override
    public void mapCommonModelToDto(SimDescriptor dto, WritingContext context) {
        CommonModel model = context.model;
        List<Object> stationKeys = model.getStationKeys();

        if (stationKeys == null || stationKeys.isEmpty()) {
            return;
        }

        // Legacy-compat: nested loop over all stations and areConnected check.
        for (Object sourceKey : stationKeys) {
            String sourceName = model.getStationName(sourceKey);
            if (sourceName == null) {
                continue;
            }
            for (Object targetKey : stationKeys) {
                if (!model.areConnected(sourceKey, targetKey)) {
                    continue;
                }
                String targetName = model.getStationName(targetKey);
                if (targetName == null) {
                    continue;
                }

                ConnectionDescriptor connDto = new ConnectionDescriptor();
                connDto.setSource(sourceName);
                connDto.setTarget(targetName);
                dto.addConnection(connDto);
            }
        }
    }
}