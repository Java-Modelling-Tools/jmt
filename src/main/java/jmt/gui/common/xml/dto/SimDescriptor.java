package jmt.gui.common.xml.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SimDescriptor {
    private Map<String, String> globalAttributes = new LinkedHashMap<>();
    private List<UserClassDescriptor> userClasses = new ArrayList<>();
    private List<NodeDescriptor> nodes = new ArrayList<>();
    private List<ConnectionDescriptor> connections = new ArrayList<>();
    private List<MeasureDescriptor> measures = new ArrayList<>();
    private List<BlockingRegionDescriptor> blockingRegions = new ArrayList<>();
    private List<NodeDescriptor> preloads = new ArrayList<>();

    public Map<String, String> getGlobalAttributes() {
        return globalAttributes;
    }

    public void addGlobalAttribute(String key, String value) {
        this.globalAttributes.put(key, value);
    }

    public List<UserClassDescriptor> getUserClasses() {
        return userClasses;
    }

    public void addUserClass(UserClassDescriptor userClass) {
        if (userClass != null) this.userClasses.add(userClass);
    }

    public List<NodeDescriptor> getNodes() {
        return nodes;
    }

    public void addNode(NodeDescriptor node) {
        if (node != null) this.nodes.add(node);
    }

    public List<ConnectionDescriptor> getConnections() {
        return connections;
    }

    public void addConnection(ConnectionDescriptor connection) {
        if (connection != null) this.connections.add(connection);
    }

    public List<MeasureDescriptor> getMeasures() {
        return measures;
    }

    public void addMeasure(MeasureDescriptor measure) {
        if (measure != null) this.measures.add(measure);
    }

    public List<BlockingRegionDescriptor> getBlockingRegions() {
        return blockingRegions;
    }

    public void addBlockingRegion(BlockingRegionDescriptor region) {
        if (region != null) this.blockingRegions.add(region);
    }

    public List<NodeDescriptor> getPreloads() {
        return preloads;
    }

    public void addPreload(NodeDescriptor preload) {
        if (preload != null) this.preloads.add(preload);
    }
}