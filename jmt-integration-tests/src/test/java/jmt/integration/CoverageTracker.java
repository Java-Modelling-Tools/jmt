package jmt.integration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;

public class CoverageTracker {

	// Helper method to extract specific line numbers
	private static String[] getLineDetails(IClassCoverage cc) {
		StringBuilder coveredLines = new StringBuilder();
		StringBuilder missedLines = new StringBuilder();

		// Iterate over all lines in the class file
		for (int i = cc.getFirstLine(); i <= cc.getLastLine(); i++) {
			ILine line = cc.getLine(i);

			// Ignore empty lines (comments, whitespace, etc.)
			if (line.getStatus() == ICounter.EMPTY) {
				continue;
			}

			// Check coverage status
			if (line.getStatus() == ICounter.NOT_COVERED) {
				if (missedLines.length() > 0)
					missedLines.append(" ");
				missedLines.append(i);
			} else {
				// PARTLY_COVERED or FULLY_COVERED are treated as executed
				if (coveredLines.length() > 0)
					coveredLines.append(" ");
				coveredLines.append(i);
			}
		}

		return new String[] { coveredLines.toString(), missedLines.toString() };
	}

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
			// "dump" with true resets the counters
			agent.getClass().getMethod("dump", boolean.class).invoke(agent, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void calculateAndAppend(String testId, File[] classesDirs, String csvFile, String[] excludedPackages) {
		Object agent = getAgent();
		if (agent == null) {
			System.err.println("!!! ERROR: JaCoCo Agent is NOT attached. Run with -javaagent VM argument.");
			return;
		}
		System.out.println("Agent found. Proceeding with analysis...");

		try {
			// Get execution data without resetting (false), we handle reset manually in
			// testLine
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
			// One directory per analysed module — the reactor no longer has a single
			// target/classes holding every compiled class.
			for (File classesDir : classesDirs) {
				analyzer.analyzeAll(classesDir);
			}

			IBundleCoverage bundle = builder.getBundle("JMT");

			// Open CSV in append mode
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
						continue;
					}

					for (IClassCoverage cls : pkg.getClasses()) {

						// Get specific line numbers
						String[] lineDetails = getLineDetails(cls);
						String specificCovered = lineDetails[0];
						String specificMissed = lineDetails[1];

						// printf to include the new columns at the end
						// Note: We wrap the line lists in quotes "%s" because they contain spaces
						writer.printf("%s,%s,%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,\"%s\",\"%s\"%n", testId, "JMT",
								pkg.getName(), cls.getName(), cls.getInstructionCounter().getMissedCount(),
								cls.getInstructionCounter().getCoveredCount(), cls.getBranchCounter().getMissedCount(),
								cls.getBranchCounter().getCoveredCount(), cls.getLineCounter().getMissedCount(),
								cls.getLineCounter().getCoveredCount(), cls.getComplexityCounter().getMissedCount(),
								cls.getComplexityCounter().getCoveredCount(), cls.getMethodCounter().getMissedCount(),
								cls.getMethodCounter().getCoveredCount(), specificCovered, // New Column 1
								specificMissed // New Column 2
						);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}