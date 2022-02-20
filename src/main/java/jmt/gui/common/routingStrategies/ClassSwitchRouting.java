package jmt.gui.common.routingStrategies;

import java.util.HashMap;
import java.util.Map;

public class ClassSwitchRouting extends RoutingStrategy {
    public Map<Object, Map<Object, Double>> outPaths = new HashMap<Object, Map<Object, Double>>();

    public ClassSwitchRouting() {
        description = "The class of jobs is switched based on the possibility of each job class defined by the user, and the possibility can be changed after selecting the corresponding destinations.";
    }

    @Override
    public String getName() {
        return "Class Switch";
    }

    @Override
    public Map<Object, Double> getValues() {
        return null;
    }

    @Override
    public ClassSwitchRouting clone() {
        ClassSwitchRouting csr = new ClassSwitchRouting();
        csr.outPaths = cloneOutPath();
        return csr;
    }

    public Map<Object, Map<Object, Double>> getOutPaths() {
        return outPaths;
    }

    public void setOutPaths(Object outPaths) {
        this.outPaths = (Map<Object, Map<Object, Double>>)outPaths;
    }

    private Map<Object, Map<Object, Double>> cloneOutPath() {
        Map<Object, Map<Object, Double>> op = new HashMap<Object, Map<Object, Double>>();
        for (Object key : outPaths.keySet()) {
            op.put(key, outPaths.get(key));
        }
        return op;
    }

    @Override
    public String getClassPath() {
        return "jmt.engine.NetStrategies.RoutingStrategies" +
                ".ClassSwitchStrategy";
    }

    @Override
    public boolean isModelStateDependent() {
        return false;
    }

    @Override
    public void addStation(Object stationKey) {
        return;
    }

    public Map<Object, Map<Object, Double>> getOutDetails() {
        return outPaths;
    }

    @Override
    public void removeStation(Object stationKey) {
        return;
    }
}
