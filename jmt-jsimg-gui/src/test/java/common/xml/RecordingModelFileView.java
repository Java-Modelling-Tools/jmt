package common.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Test double: scripted answers; {@code calls} records call names only, {@code parents} the parent of each call that takes one. */
class RecordingModelFileView implements ModelFileView {

	File openAnswer;
	File saveAnswer;
	boolean newerAnswer = true;
	boolean extensionAnswer = true;

	final List<String> calls = new ArrayList<String>();
	final List<Object> parents = new ArrayList<Object>();
	File selected;
	String savedVersion;
	String runningVersion;
	File confirmedFile;

	@Override
	public void setSelectedFile(File file) {
		calls.add("setSelectedFile");
		selected = file;
	}

	@Override
	public File chooseFileToOpen(Object parent) {
		calls.add("chooseFileToOpen");
		parents.add(parent);
		return openAnswer;
	}

	@Override
	public File chooseFileToSave(Object parent) {
		calls.add("chooseFileToSave");
		parents.add(parent);
		return saveAnswer;
	}

	@Override
	public boolean confirmOpenNewerVersion(Object parent, File file, String savedVersion, String runningVersion) {
		calls.add("confirmOpenNewerVersion");
		parents.add(parent);
		this.confirmedFile = file;
		this.savedVersion = savedVersion;
		this.runningVersion = runningVersion;
		return newerAnswer;
	}

	@Override
	public boolean confirmSaveWithoutValidExtension(Object parent, File file) {
		calls.add("confirmSaveWithoutValidExtension");
		parents.add(parent);
		this.confirmedFile = file;
		return extensionAnswer;
	}
}
