package common.xml;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import common.definitions.CommonModel;

/** Shared helpers to load example models from the repo's examples/ directory and re-serialize them. */
public final class ModelFixtures {

	private ModelFixtures() {
	}

	/** Repo examples/ dir, found by walking up from the working dir until examples/jsim exists. */
	public static File examplesDir() {
		File d = new File(System.getProperty("user.dir")).getAbsoluteFile();
		while (d != null) {
			File ex = new File(d, "examples");
			if (new File(ex, "jsim").isDirectory()) {
				return ex;
			}
			d = d.getParentFile();
		}
		throw new IllegalStateException("examples/jsim not found above " + System.getProperty("user.dir"));
	}

	/** Loads a .jsimg file; throws if the reader reports failure. */
	public static CommonModel load(File jsimg) {
		CommonModel model = new CommonModel();
		if (!XMLReader.loadModel(jsimg, model)) {
			throw new IllegalStateException("XMLReader.loadModel returned false for " + jsimg);
		}
		return model;
	}

	public static String writeToString(CommonModel model) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XMLWriter.writeXML(out, model);
		return new String(out.toByteArray(), StandardCharsets.UTF_8);
	}

	/** Sorted files under examples/jsim (the tracked examples) whose name matches the glob (e.g. "*.jsimg"). */
	public static List<File> listExamples(String glob) {
		Path root = new File(examplesDir(), "jsim").toPath(); // only tracked examples
		java.nio.file.PathMatcher m = root.getFileSystem().getPathMatcher("glob:" + glob);
		try (Stream<Path> s = Files.walk(root)) {
			List<File> r = new ArrayList<File>();
			s.filter(Files::isRegularFile).filter(p -> m.matches(p.getFileName())).forEach(p -> r.add(p.toFile()));
			Collections.sort(r);
			return r;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
