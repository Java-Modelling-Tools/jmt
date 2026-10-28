package jmt.integration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public class FileUtil {

	public static boolean isFileWithNameInsideFolder(File folder, String fileName) {
		if (folder == null || !folder.exists() || !folder.isDirectory()) {
			return false;
		}

		File[] entries = folder.listFiles();
		if (entries == null) {
			return false;
		}

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
		if (folder == null || !folder.exists() || !folder.isDirectory()) {
			throw new IllegalArgumentException("Invalid folder: " + (folder != null ? folder.getAbsolutePath() : "null"));
		}

		File[] files = folder.listFiles();
		if (files == null) {
			return null;
		}

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

	/**
	 * Lists all entries in a folder using Java NIO.
	 */
	public static File[] listAllFilesAndFoldersInFolder(Path folder) throws IOException {
		if (folder == null || !Files.isDirectory(folder)) {
			throw new IllegalArgumentException("Folder not found or not a directory: " + folder);
		}
		try (Stream<Path> stream = Files.list(folder)) {
			return stream.map(Path::toFile).sorted().toArray(File[]::new);
		}
	}

	public static File[] listAllFilesAndFoldersInFolder(String folderName) throws RuntimeException {
		try {
			return listAllFilesAndFoldersInFolder(Paths.get(folderName));
		} catch (Exception e) {
			throw new RuntimeException("Error listing folder: " + folderName, e);
		}
	}

	/**
	 * Helper to create an output file within a target base directory.
	 */
	public static File createOutputFile(Path baseDir, List<String> folderHierarchy, String fileName) throws IOException {
		Path targetPath = baseDir;
		for (String folder : folderHierarchy) {
			targetPath = targetPath.resolve(folder);
		}

		Files.createDirectories(targetPath);
		Path outputFile = targetPath.resolve(fileName);
		if (!Files.exists(outputFile)) {
			Files.createFile(outputFile);
		}
		return outputFile.toFile();
	}
}
