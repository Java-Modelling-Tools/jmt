package common.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import common.definitions.CommonModel;

/**
 * A model saved before server types existed has no server-type list. When it is loaded and written back, the default
 * server type must carry all the servers of the station, otherwise the engine simulates a single server.
 */
class XMLWriterServerTypesTest {

	/** Old-format model: CompServers has maxJobs=200 and Unavailable has maxJobs=1, neither has advancedFeatures. */
	private static final String OLD_FORMAT_MODEL = "/common/xml/old-format-200-servers.xml";

	@Test
	void defaultServerTypeCarriesAllServersOfTheStation() throws Exception {
		File file = new File(getClass().getResource(OLD_FORMAT_MODEL).toURI());
		CommonModel model = new CommonModel();
		XMLReader.parseXML(XMLReader.loadXML(file.getAbsolutePath()), model);

		Map<String, Integer> serversPerStation = serverTypeCountByStation(XMLWriter.getDocument(model, "model"));

		assertEquals(Integer.valueOf(200), serversPerStation.get("CompServers"));
		assertEquals(Integer.valueOf(1), serversPerStation.get("Unavailable"));
	}

	/** Maps each station name to the serverTypesNumOfServers value written for its Server section. */
	private static Map<String, Integer> serverTypeCountByStation(Document written) {
		Map<String, Integer> result = new HashMap<>();
		NodeList nodes = written.getElementsByTagName("node");
		for (int i = 0; i < nodes.getLength(); i++) {
			Element node = (Element) nodes.item(i);
			NodeList counts = node.getElementsByTagName("subParameter");
			for (int j = 0; j < counts.getLength(); j++) {
				Element count = (Element) counts.item(j);
				if ("serverTypesNumOfServers".equals(count.getAttribute("name"))) {
					String value = count.getElementsByTagName("value").item(0).getTextContent().trim();
					result.put(node.getAttribute("name"), Integer.valueOf(value));
				}
			}
		}
		return result;
	}
}
