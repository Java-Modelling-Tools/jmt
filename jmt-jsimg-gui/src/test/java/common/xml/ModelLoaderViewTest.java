package common.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import analytical.ExactModel;
import common.definitions.CommonModel;
import jaba.JabaModel;
import jmt.common.GlobalSettings;
import jmt.framework.xml.XMLUtils;

/** ModelLoader decision paths (cancel, choose, confirmations) driven through a scripted view; no dialog is shown. */
class ModelLoaderViewTest {

	@TempDir
	File tmp;

	/** One loader flavour: how to build it, a model to load into / save from, and a loadable file. */
	private final class Flavour {
		final ModelLoader.JmtFileFilter open, save;
		final String ext;
		final Supplier<Object> model;
		final Function<String, File> validFile;

		Flavour(ModelLoader.JmtFileFilter open, ModelLoader.JmtFileFilter save, String ext, Supplier<Object> model, Function<String, File> validFile) {
			this.open = open;
			this.save = save;
			this.ext = ext;
			this.model = model;
			this.validFile = validFile;
		}

		ModelLoader loader(ModelFileView v) {
			return new ModelLoader(open, save, v);
		}
	}

	private File simFile(String name) {
		File f = new File(tmp, name);
		XMLWriter.writeXML(f, ArchiveFixtures.simpleModel());
		return f;
	}

	private File archiveFile(String name) {
		File f = new File(tmp, name);
		new ModelLoader(ModelLoader.JSIM, ModelLoader.JSIM_SAVE, new HeadlessModelFileView()).saveModel(ArchiveFixtures.simpleModel(), null, f);
		return f;
	}

	private File copyExample(String rel, String name) {
		try {
			File f = new File(tmp, name);
			Files.copy(new File(ArchiveFixtures.examplesDir(), rel).toPath(), f.toPath());
			return f;
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private Flavour[] flavours() {
		return new Flavour[] {
				new Flavour(ModelLoader.JSIM, ModelLoader.JSIM_SAVE, ".jsimw", () -> ArchiveFixtures.simpleModel(), this::archiveFile),
				new Flavour(ModelLoader.JMODEL, ModelLoader.JMODEL_SAVE, ".jsimg", () -> ArchiveFixtures.simpleModel(), this::archiveFile),
				new Flavour(ModelLoader.JMVA, ModelLoader.JMVA_SAVE, ".jmva", () -> {
					ExactModel m = new ExactModel();
					try {
						assertTrue(m.loadDocument(new XMLUtils().loadXML(new File(ArchiveFixtures.examplesDir(), "jmva/1cl_1stat_mm1.jmva"))));
					} catch (Exception e) {
						throw new AssertionError(e);
					}
					return m;
				}, n -> copyExample("jmva/1cl_1stat_mm1.jmva", n)),
				new Flavour(ModelLoader.JABA, ModelLoader.JABA_SAVE, ".jaba", () -> {
					JabaModel m = new JabaModel();
					assertEquals(ModelLoader.SUCCESS, new ModelLoader(ModelLoader.JABA, ModelLoader.JABA_SAVE, new HeadlessModelFileView())
							.loadModel(m, null, new File(ArchiveFixtures.examplesDir(), "jaba/2cl_3stat_1commonsat.jaba")));
					return m;
				}, n -> copyExample("jaba/2cl_3stat_1commonsat.jaba", n)) };
	}

	/** The load target is a fresh model of the flavour's own type. */
	private Object target(Flavour f) {
		Object m = f.model.get();
		return m instanceof CommonModel ? new CommonModel() : m instanceof ExactModel ? new ExactModel() : new JabaModel();
	}

	// --- load ------------------------------------------------------------------------------------------

	@Test
	void loadWithoutFileCancelledByViewKeepsSelection() {
		for (Flavour f : flavours()) {
			RecordingModelFileView v = new RecordingModelFileView();
			ModelLoader ml = f.loader(v);
			java.awt.Component parent = new java.awt.Container();
			assertEquals(ModelLoader.CANCELLED, ml.loadModel(target(f), null, null), f.ext);
			assertEquals(Collections.singletonList("chooseFileToOpen"), v.calls, f.ext);
			assertNull(ml.getSelectedFile(), f.ext);
			// a previous selection survives a later cancel
			File ok = f.validFile.apply("a" + f.ext);
			v.openAnswer = ok;
			assertEquals(ModelLoader.SUCCESS, ml.loadModel(target(f), parent, null), f.ext);
			assertSame(parent, v.parents.get(1), f.ext);
			v.openAnswer = null;
			assertEquals(ModelLoader.CANCELLED, ml.loadModel(target(f), null, null), f.ext);
			assertEquals(ok, ml.getSelectedFile(), f.ext);
		}
	}

	@Test
	void loadWithoutFileUsesTheFileChosenByTheView() {
		for (Flavour f : flavours()) {
			RecordingModelFileView v = new RecordingModelFileView();
			v.openAnswer = f.validFile.apply("chosen" + f.ext);
			ModelLoader ml = f.loader(v);
			assertEquals(ModelLoader.SUCCESS, ml.loadModel(target(f), null, null), f.ext);
			assertEquals(v.openAnswer, ml.getSelectedFile(), f.ext);
			assertEquals(Collections.singletonList("chooseFileToOpen"), v.calls, f.ext);
		}
	}

	@Test
	void loadWithExplicitFileTellsTheViewAndDoesNotAsk() {
		for (Flavour f : flavours()) {
			RecordingModelFileView v = new RecordingModelFileView();
			File file = f.validFile.apply("given" + f.ext);
			ModelLoader ml = f.loader(v);
			assertEquals(ModelLoader.SUCCESS, ml.loadModel(target(f), null, file), f.ext);
			assertEquals(Collections.singletonList("setSelectedFile"), v.calls, f.ext);
			assertEquals(file, v.selected, f.ext);
			assertEquals(file, ml.getSelectedFile(), f.ext);
		}
	}

	// --- save ------------------------------------------------------------------------------------------

	@Test
	void saveWithoutFileCancelledByViewWritesNothing() {
		for (Flavour f : flavours()) {
			RecordingModelFileView v = new RecordingModelFileView();
			ModelLoader ml = f.loader(v);
			assertEquals(ModelLoader.CANCELLED, ml.saveModel(f.model.get(), null, null), f.ext);
			assertEquals(Collections.singletonList("chooseFileToSave"), v.calls, f.ext);
			assertNull(ml.getSelectedFile(), f.ext);
		}
	}

	@Test
	void saveWithoutFileWritesTheFileChosenByTheView() {
		for (Flavour f : flavours()) {
			RecordingModelFileView v = new RecordingModelFileView();
			v.saveAnswer = new File(tmp, "out" + f.ext);
			ModelLoader ml = f.loader(v);
			assertEquals(ModelLoader.SUCCESS, ml.saveModel(f.model.get(), null, null), f.ext);
			assertTrue(v.saveAnswer.length() > 0, f.ext);
			assertEquals(v.saveAnswer, ml.getSelectedFile(), f.ext);
			assertEquals(Collections.singletonList("chooseFileToSave"), v.calls, f.ext);
		}
	}

	@Test
	void saveWithExplicitValidExtensionAsksNothingAndKeepsSelection() {
		for (Flavour f : flavours()) {
			RecordingModelFileView v = new RecordingModelFileView();
			File lower = new File(tmp, "v" + f.ext);
			ModelLoader ml = f.loader(v);
			assertEquals(ModelLoader.SUCCESS, ml.saveModel(f.model.get(), null, lower), f.ext);
			assertTrue(v.calls.isEmpty(), f.ext);
			assertTrue(lower.length() > 0, f.ext);
			// Save with an explicit file does not change the selection (only save-as does)
			assertNull(ml.getSelectedFile(), f.ext);
		}
	}

	@Test
	void saveWithInvalidExtensionAsksAndNoWritesNothing() {
		for (Flavour f : flavours()) {
			RecordingModelFileView v = new RecordingModelFileView();
			v.extensionAnswer = false;
			File out = new File(tmp, "bad_" + f.ext.substring(1) + ".txt");
			ModelLoader ml = f.loader(v);
			assertEquals(ModelLoader.CANCELLED, ml.saveModel(f.model.get(), null, out), f.ext);
			assertEquals(Collections.singletonList("confirmSaveWithoutValidExtension"), v.calls, f.ext);
			assertEquals(out, v.confirmedFile, f.ext);
			assertFalse(out.exists(), f.ext);
		}
	}

	@Test
	void saveWithInvalidExtensionAcceptedWrites() {
		for (Flavour f : flavours()) {
			RecordingModelFileView v = new RecordingModelFileView();
			v.extensionAnswer = true;
			File out = new File(tmp, "ok_" + f.ext.substring(1) + ".txt");
			ModelLoader ml = f.loader(v);
			assertEquals(ModelLoader.SUCCESS, ml.saveModel(f.model.get(), null, out), f.ext);
			assertEquals(Collections.singletonList("confirmSaveWithoutValidExtension"), v.calls, f.ext);
			assertTrue(out.length() > 0, f.ext);
		}
	}

	// --- newer-version confirmation (archives, JSIM/JMODEL/JMVA loaders) --------------------------------

	private File archiveWithVersion(String name, String version) throws Exception {
		File f = archiveFile(name);
		String xml = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
		String patched = xml.replaceFirst("jmtVersion=\"[^\"]*\"", "jmtVersion=\"" + version + "\"");
		if (!patched.contains("jmtVersion=\"" + version + "\"")) {
			patched = patched.replaceFirst("<archive", "<archive jmtVersion=\"" + version + "\"");
		}
		Files.write(f.toPath(), patched.getBytes(StandardCharsets.UTF_8));
		return f;
	}

	private Flavour[] archiveReaders() {
		Flavour[] all = flavours();
		return new Flavour[] { all[0], all[1], all[2] };
	}

	@Test
	void olderOrSameVersionArchiveIsNotAskedAbout() throws Exception {
		String running = GlobalSettings.getSetting("jmt.version");
		for (String version : Arrays.asList("0.0.1", running)) {
			File file = archiveWithVersion("old.jsimg", version);
			for (Flavour f : archiveReaders()) {
				RecordingModelFileView v = new RecordingModelFileView();
				assertLoaded(f.loader(v).loadModel(target(f), null, file), f.ext + " " + version);
				assertFalse(v.calls.contains("confirmOpenNewerVersion"), f.ext + " " + version);
			}
		}
	}

	@Test
	void newerArchiveConfirmedByViewIsLoaded() throws Exception {
		File file = archiveWithVersion("new.jsimg", "99.0.0");
		for (Flavour f : archiveReaders()) {
			RecordingModelFileView v = new RecordingModelFileView();
			v.newerAnswer = true;
			assertLoaded(f.loader(v).loadModel(target(f), null, file), f.ext);
			assertEquals(Arrays.asList("setSelectedFile", "confirmOpenNewerVersion"), v.calls, f.ext);
			assertEquals(file, v.confirmedFile, f.ext);
			assertEquals("99.0.0", v.savedVersion, f.ext);
			assertEquals(GlobalSettings.getSetting("jmt.version"), v.runningVersion, f.ext);
		}
	}

	@Test
	void newerArchiveRefusedByViewIsCancelledAndModelUntouched() throws Exception {
		File file = archiveWithVersion("new.jsimg", "99.0.0");
		for (Flavour f : archiveReaders()) {
			RecordingModelFileView v = new RecordingModelFileView();
			v.newerAnswer = false;
			Object model = target(f);
			String before = snapshot(model);
			assertEquals(ModelLoader.CANCELLED, f.loader(v).loadModel(model, null, file), f.ext);
			assertEquals(Arrays.asList("setSelectedFile", "confirmOpenNewerVersion"), v.calls, f.ext);
			assertEquals(before, snapshot(model), f.ext);
		}
	}

	/** JSIM-to-JMVA conversion reports warnings; both mean the model was loaded. */
	private static void assertLoaded(int status, String msg) {
		assertTrue(status == ModelLoader.SUCCESS || status == ModelLoader.WARNING, msg + " status " + status);
	}

	private static String snapshot(Object m) {
		if (m instanceof CommonModel) {
			return ArchiveFixtures.saveToString((CommonModel) m);
		}
		return ArchiveFixtures.toString(((ExactModel) m).createDocument());
	}
}
