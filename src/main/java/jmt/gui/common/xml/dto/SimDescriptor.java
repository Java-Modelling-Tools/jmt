package jmt.gui.common.xml.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class SimDescriptor {

    private LinkedHashMap<String, String> globalAttributes = new LinkedHashMap<String, String>();
    private List<UserClassDescriptor> userClasses = new ArrayList<UserClassDescriptor>();
    private List<NodeDescriptor> nodes = new ArrayList<NodeDescriptor>();
    private List<ConnectionDescriptor> connections = new ArrayList<ConnectionDescriptor>();
    private List<MeasureDescriptor> measures = new ArrayList<MeasureDescriptor>();
    private List<BlockingRegionDescriptor> blockingRegions = new ArrayList<BlockingRegionDescriptor>();

    public LinkedHashMap<String, String> getGlobalAttributes() {
        return globalAttributes;
    }

    public void setGlobalAttributes(LinkedHashMap<String, String> globalAttributes) {
        this.globalAttributes = globalAttributes != null
                ? globalAttributes
                : new LinkedHashMap<String, String>();
    }

    public void addGlobalAttribute(String name, String value) {
        if (name != null) {
            globalAttributes.put(name, value);
        }
    }

    public List<UserClassDescriptor> getUserClasses() {
        return userClasses;
    }

    public void setUserClasses(List<UserClassDescriptor> userClasses) {
        this.userClasses = userClasses != null ? userClasses : new ArrayList<UserClassDescriptor>();
    }

    public void addUserClass(UserClassDescriptor userClassDescriptor) {
        if (userClassDescriptor != null) {
            userClasses.add(userClassDescriptor);
        }
    }

    public List<NodeDescriptor> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeDescriptor> nodes) {
        this.nodes = nodes != null ? nodes : new ArrayList<NodeDescriptor>();
    }

    public void addNode(NodeDescriptor nodeDescriptor) {
        if (nodeDescriptor != null) {
            nodes.add(nodeDescriptor);
        }
    }

    public List<ConnectionDescriptor> getConnections() {
        return connections;
    }

    public void setConnections(List<ConnectionDescriptor> connections) {
        this.connections = connections != null ? connections : new ArrayList<ConnectionDescriptor>();
    }

    public void addConnection(ConnectionDescriptor connectionDescriptor) {
        if (connectionDescriptor != null) {
            connections.add(connectionDescriptor);
        }
    }

    public List<MeasureDescriptor> getMeasures() {
        return measures;
    }

    public void setMeasures(List<MeasureDescriptor> measures) {
        this.measures = measures != null ? measures : new ArrayList<MeasureDescriptor>();
    }

    public void addMeasure(MeasureDescriptor measureDescriptor) {
        if (measureDescriptor != null) {
            measures.add(measureDescriptor);
        }
    }

    public List<BlockingRegionDescriptor> getBlockingRegions() {
        return blockingRegions;
    }

    public void setBlockingRegions(List<BlockingRegionDescriptor> blockingRegions) {
        this.blockingRegions = blockingRegions != null
                ? blockingRegions
                : new ArrayList<BlockingRegionDescriptor>();
    }

    public void addBlockingRegion(BlockingRegionDescriptor blockingRegionDescriptor) {
        if (blockingRegionDescriptor != null) {
            blockingRegions.add(blockingRegionDescriptor);
        }
    }
}
