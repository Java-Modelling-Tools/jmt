package jmt.engine.NetStrategies.PSStrategies;

import jmt.engine.NetStrategies.PSStrategy;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.JobClassList;
import jmt.engine.QueueNet.JobInfo;
import jmt.engine.QueueNet.JobInfoList;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class DPSStrategyPriority extends PSStrategy {

    public double slice(JobInfoList list, JobClassList classes, double[] weights, boolean[] saturated, JobClass jobClass) {
        Boolean[] compatibilities = new Boolean[classes.size()];
        Arrays.fill(compatibilities, true);
        double[] splits = new double[classes.size()];
        Arrays.fill(splits, 1);
        return slice(list, classes, weights, saturated, jobClass, compatibilities, splits);
    }

    public double slice(JobInfoList list, JobClassList classes, double[] weights, boolean[] saturated, JobClass jobClass, Boolean[] compatibilities, double[] splits) {
        if (list.size(jobClass) <= 0 || !compatibilities[jobClass.getId()]) {
            return 0.0;
        }
        int highestPriority = Integer.MIN_VALUE;
        List<JobInfo> listInfo = list.getInternalJobInfoList();
        Iterator<JobInfo> it = listInfo.iterator();
        while (it.hasNext()) {
            int currentPriority = it.next().getJob().getJobClass().getPriority();
            if (currentPriority > highestPriority) {
                highestPriority =  currentPriority;
            }
        }
        if (jobClass.getPriority() < highestPriority) {
            return 0.0;
        }

        double total = 0.0;
        for (int i = 0; i < classes.size(); i++) {
            if (list.size(jobClass) > 0 && !saturated[i] && splits[i] > 0 && classes.get(i).getPriority() == highestPriority) {
                total += weights[i] * list.size(classes.get(i)) * splits[i];
            }
        }
        return weights[jobClass.getId()] / total;
    }

}