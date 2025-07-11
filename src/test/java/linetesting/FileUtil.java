package linetesting;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtil {

	public static File createFileInTestResources(String[] folderHierarchy, String fileName) throws IOException {
		// Base path: src/test/resources
		Path basePath = Paths.get("src", "test", "resources");

		// Append all folders from the hierarchy
		for (String folder : folderHierarchy) {
			basePath = basePath.resolve(folder);
		}

		// Create directories if not exist
		File directory = basePath.toFile();
		if (!directory.exists() && !directory.mkdirs()) {
			throw new IOException("Failed to create directory: " + directory.getAbsolutePath());
		}

		// Create/overwrite file
		File outputFile = basePath.resolve(fileName).toFile();
		if (!outputFile.exists()) {
			if (!outputFile.createNewFile()) {
				throw new IOException("Failed to create file: " + outputFile.getAbsolutePath());
			}
		}

		return outputFile; // Return for reference or assertion
	}
}
