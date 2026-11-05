package jwat.input;

import java.io.IOException;
import java.util.zip.ZipFile;

import jwat.JwatSession;

import org.w3c.dom.NodeList;

/**
 * Loads the results of a JWAT analysis.
 */
/**
 * Loads the results of a JWAT analysis.
 */
/**
 * Loads the results of a JWAT analysis.
 */
/**
 * Loads the results of a JWAT analysis.
 */
public interface ResultLoader {

	public int loadResult(ZipFile zf, NodeList resultNodeList, JwatSession session) throws IOException;

}
