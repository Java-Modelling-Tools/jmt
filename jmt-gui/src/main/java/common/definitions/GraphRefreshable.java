package common.definitions;

/**
 * Interface for models that can refresh their graph representation
 * when station data changes. Implemented by JSimGraphModel in jmt-jsimg-gui.
 * Allows common panels (EnablingSectionPanel, TimingSectionPanel) to trigger
 * graph refreshes without a compile-time dependency on jsimgraph.
 */
public interface GraphRefreshable {
	String getStationIcon(Object stationKey);
	void setStationIcon(Object stationKey, String icon);
	void refreshGraph();
	int getConnectionEnd(Object sourceKey, Object destKey);
	void setConnectionEnd(Object sourceKey, Object destKey, int end);
}
