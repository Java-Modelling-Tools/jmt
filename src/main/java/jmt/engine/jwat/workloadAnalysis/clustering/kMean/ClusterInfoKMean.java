package jmt.engine.jwat.workloadAnalysis.clustering.kMean;

public class ClusterInfoKMean {

	public ClusterInfoKMean(int numVars) {
		statClust = new SCluStat[numVars];
		for (int i = 0; i < numVars; i++) {
			statClust[i] = new SCluStat();
		}
		percVar = new double[numVars];
	}

	public class SCluStat {

public int iNotZr; //Number of Observations other than Zero
public double dMean; //Average
public double dStdEr; //StandardError
public double dStdDv; // Standard Deviation
public double dVarnc; //Variance
public double dKurto; // Kurtosis
public double dSkewn; //Skewness
public double dRange; //Range (Max-Min)
public double dMinObs; //Minimum value
public double dMaxObs; //Maximum value

//Temp variables
public double dSum; //Sum of the values for each variable v[1]
public double dSQuad; //Sum of squares v[2]
public double dSThird; //Sum of third powers v[3]
public double dSFourth; //Sum of the fourth powers v[4]
	}

public double[] percVar;// percentage of each variable out of the total variable
	public SCluStat[] statClust;
	public int numObs;

}
