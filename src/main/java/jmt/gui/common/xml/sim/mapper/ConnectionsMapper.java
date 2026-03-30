/**
 * This class is responsible for mapping {@link ConnectionDescriptor} objects from the DTO to the {@link CommonModel}
 * and vice-versa. It iterates through the connections defined in the {@link SimDescriptor} and establishes the links
 * between nodes in the model, or retrieves them from the model to build the DTO.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
package jmt.gui.common.xml.sim.mapper;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.sim.dto.ConnectionDescriptor;
import jmt.gui.common.xml.sim.dto.SimDescriptor;
import jmt.gui.common.xml.sim.serializer.WritingContext;

import java.util.ArrayList;
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

        // 1. Search for the source node: first among the stations, then among the regions
        Object sourceKey = resolveStationOrRegion(sourceName, context);

        // 2. Search for the destination node: first among the stations, then among the regions
        Object targetKey = resolveStationOrRegion(targetName, context);

        // 3. If both nodes exist in the model, activate the connection via setConnected
        if (sourceKey != null && targetKey != null) {
            model.setConnected(sourceKey, targetKey, true);
        }
    }

    // --- WRITING PHASE (Model -> DTO -> XML) ---

    @Override
    public void mapCommonModelToDto(SimDescriptor dto, WritingContext context) {
        CommonModel model = context.model;

        // We need to check connections from all possible sources: stations and blocking regions
        List<Object> allPossibleSources = new ArrayList<>();
        if (model.getStationKeys() != null) {
            allPossibleSources.addAll(model.getStationKeys());
        }
        if (model.getRegionKeys() != null) {
            allPossibleSources.addAll(model.getRegionKeys());
        }

        if (allPossibleSources.isEmpty()) {
            return;
        }

        for (Object sourceKey : allPossibleSources) {
            mapModelConnectionsToDto(model, sourceKey, dto);
        }
    }

    private void mapModelConnectionsToDto(CommonModel model, Object sourceKey, SimDescriptor dto) {
        // Retrieve all forward connections for the current source node
        List<Object> targetKeys = model.getForwardConnections(sourceKey);

        if (targetKeys == null || targetKeys.isEmpty()) {
            return;
        }

        String sourceName = getNodeName(model, sourceKey);
        if (sourceName == null) {
            return; // Fallback for safety, should not happen in a valid model
        }

        for (Object targetKey : targetKeys) {
            String targetName = getNodeName(model, targetKey);
            if (targetName == null) {
                continue; // Fallback for safety
            }

            ConnectionDescriptor connDto = new ConnectionDescriptor();
            connDto.setSource(sourceName);
            connDto.setTarget(targetName);

            dto.addConnection(connDto);
        }
    }
}