package linetesting;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FileUtil {

	public static boolean isFileWithNameInsideFolder(File folder, String fileName) {
		if (!folder.exists() || !folder.isDirectory()) {
			return false;
		}

		File[] entries = folder.listFiles();
		if (entries == null)
			return false;

		for (File entry : entries) {
			if (entry.isDirectory()) {
				// Recursively search in subfolder
				if (isFileWithNameInsideFolder(entry, fileName)) {
					return true;
				}
			} else if (entry.isFile() && entry.getName().equals(fileName)) {
				return true;
			}
		}

		return false;
	}

	public static File getFileInFolder(File folder, String fileName) {
		if (!folder.exists() || !folder.isDirectory()) {
			throw new IllegalArgumentException("Invalid folder: " + folder.getAbsolutePath());
		}

		File[] files = folder.listFiles();
		if (files == null)
			return null;

		for (File file : files) {
			if (file.isFile() && file.getName().equals(fileName)) {
				return file; // found
			}
		}

		return null; // not found
	}

	public static String getParentFolderName(File file) {
		File parent = file.getParentFile();
		return (parent != null) ? parent.getName() : null;
	}

	public static File[] listAllFilesAndFoldersInFolder(String folderName) throws RuntimeException {
		File folder = new File(folderName);

		if (!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("Folder not found: " + folder.getAbsolutePath());
		}

		return folder.listFiles(); // includes both files and folders
	}

	public static File createFileInTestResources(List<String> folderHierarchy, String fileName) throws IOException {
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
