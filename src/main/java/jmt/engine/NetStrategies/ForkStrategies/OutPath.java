package jmt.engine.NetStrategies.ForkStrategies;

import jmt.common.exception.IncorrectDistributionParameterException;
import jmt.engine.random.EmpiricalEntry;

public class OutPath {
	private String name;
	
	private EmpiricalEntry probabilityOfChosen;
	
	private EmpiricalEntry[] JobsPerLinkDis;
	
	public OutPath(EmpiricalEntry probabilityOfChosen, EmpiricalEntry[] jobsPerLinkDis) 
			throws IncorrectDistributionParameterException{
		this.name = (String)(probabilityOfChosen.getValue());
		this.setProbabilityOfChosen(probabilityOfChosen);
		this.setJobsPerLinkDis(jobsPerLinkDis);
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public EmpiricalEntry getProbabilityOfChosen() {
		return probabilityOfChosen;
	}

	public void setProbabilityOfChosen(EmpiricalEntry probabilityOfChosen) 
			throws IncorrectDistributionParameterException {
		if (probabilityOfChosen.getProbability() <= 1 
				&& probabilityOfChosen.getProbability() >=0) {
			this.probabilityOfChosen = probabilityOfChosen;
		} else {
			throw new IncorrectDistributionParameterException(
					"the sum of probabilities should between 0~1");
		}
	}

	public EmpiricalEntry[] getJobsPerLinkDis() {
		return JobsPerLinkDis;
	}

	public void setJobsPerLinkDis(EmpiricalEntry[] jobsPerLinkDis) 
			throws IncorrectDistributionParameterException {
		if (checkProb(jobsPerLinkDis)) {
			JobsPerLinkDis = jobsPerLinkDis;
		} else {
			throw new IncorrectDistributionParameterException(
					"the sum of probabilities should be 1");
		}
	}
	
	public boolean checkProb(EmpiricalEntry[] ee) {
		boolean flag = false;
		double sum = 0;
		for (int i = 0; i < ee.length; i++) {
			sum += ee[i].getProbability();
		}
		if (sum == 1) {
			flag = true;
		}
		return flag;
	}

}
