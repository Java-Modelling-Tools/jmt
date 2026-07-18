package jmt.engine.jwat.workloadAnalysis.clustering.fuzzyKMean;

public class ClusterInfoFuzzy {

	public ClusterInfoFuzzy(int numVars) {
		statClust = new SFCluStat[numVars];
		for (int i = 0; i < numVars; i++) {
			statClust[i] = new SFCluStat();
		}
		percVar = new double[numVars];
	}

	public class SFCluStat {

public int iNotZr; //Non-zero observations
public double dMean; //Average
public double dStdEr; //StandardError
public double dStdDv; // Standard Deviation
public double dVarnc; // Variance
public double dKurto; // Kurtosis
public double dSkewn; //Skewness
public double dRange; //Range (Max-Min)
public double dMinObs; //Minimum value
public double dMaxObs; //Maximum value

public double dSum; //Sum of the values for each variable v[1]
public double dSQuad; //Sum of squares v[2]
public double dSThird; //Sum of third powers v[3]
public double dSFourth; //Sum of the fourth powers v[4]
public double dPerc5; //Used for percentiles v[5]
public double dPerc6; //Used for percentiles v[6]
public double dPerc7; //Used for percentiles v[7]
	}

public String clus_log; // log of Cluster values
public double[] percVar;// percentage of each variable out of the total variable
	public SFCluStat[] statClust;
	public int numObs;
}
