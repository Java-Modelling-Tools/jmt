package jmt.engine.NetStrategies.PSStrategies;

import jmt.engine.NetStrategies.PSStrategy;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.JobClassList;
import jmt.engine.QueueNet.JobInfoList;
import jmt.engine.QueueNet.PSJobInfoList;

import java.util.Arrays;

public class QBPSStrategy extends PSStrategy {
    public double slice(JobInfoList list, JobClassList classes, double[] weights, boolean[] saturated, JobClass jobClass) {
        Boolean[] compatibilities = new Boolean[classes.size()];
        Arrays.fill(compatibilities, true);
        double[] splits = new double[classes.size()];
        Arrays.fill(splits, 1);
        return slice(list, classes, weights, saturated, jobClass, compatibilities, splits); }

    public double slice(JobInfoList list, JobClassList classes, double[] weights, boolean[] saturated, JobClass jobClass, Boolean[] compatibilities, double[] splits) {
        if (list.size() <= 0 || !compatibilities[jobClass.getId()]) {
            return 0.0;
        }
        double compatibleJobs = 0;
        for (int i = 0; i < classes.size(); i++) {
            if (compatibilities[i] && !saturated[i]) {
                compatibleJobs += list.size(classes.get(i)) * splits[i];
            }
        }
        return 1 / compatibleJobs;
    }
}
