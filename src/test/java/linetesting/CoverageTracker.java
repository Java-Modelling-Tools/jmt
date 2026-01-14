package linetesting;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;

public class CoverageTracker {

	private static Object getAgent() {
		try {
			Class<?> clazz = Class.forName("org.jacoco.agent.rt.RT");
			return clazz.getMethod("getAgent").invoke(null);
		} catch (Exception e) {
			return null;
		}
	}

	public static void reset() {
		Object agent = getAgent();
		if (agent == null)
			return;
		try {
			agent.getClass().getMethod("dump", boolean.class).invoke(agent, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void calculateAndAppend(String testId, File classesDir, String csvFile, String[] excludedPackages) {
		Object agent = getAgent();
		if (agent == null)
			return;

		try {
			byte[] executionData = (byte[]) agent.getClass().getMethod("getExecutionData", boolean.class).invoke(agent,
					false);

			ExecutionDataStore execStore = new ExecutionDataStore();
			SessionInfoStore sessionInfos = new SessionInfoStore();
			ExecutionDataReader reader = new ExecutionDataReader(new ByteArrayInputStream(executionData));
			reader.setExecutionDataVisitor(execStore);
			reader.setSessionInfoVisitor(sessionInfos);
			reader.read();

			CoverageBuilder builder = new CoverageBuilder();
			Analyzer analyzer = new Analyzer(execStore, builder);
			analyzer.analyzeAll(classesDir);

			IBundleCoverage bundle = builder.getBundle("JMT");

			try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile, true))) {

				for (IPackageCoverage pkg : bundle.getPackages()) {

					String pkgName = pkg.getName();

					boolean isExcluded = false;
					if (excludedPackages != null) {
						for (String exclude : excludedPackages) {
							if (pkgName.startsWith(exclude)) {
								isExcluded = true;
								break;
							}
						}
					}

					if (isExcluded) {
						continue; // Skip this package
					}

					for (IClassCoverage cls : pkg.getClasses()) {

						writer.printf("%s,%s,%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d%n", testId, "JMT", pkg.getName(),
								cls.getName(), cls.getInstructionCounter().getMissedCount(),
								cls.getInstructionCounter().getCoveredCount(), cls.getBranchCounter().getMissedCount(),
								cls.getBranchCounter().getCoveredCount(), cls.getLineCounter().getMissedCount(),
								cls.getLineCounter().getCoveredCount(), cls.getComplexityCounter().getMissedCount(),
								cls.getComplexityCounter().getCoveredCount(), cls.getMethodCounter().getMissedCount(),
								cls.getMethodCounter().getCoveredCount());
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}