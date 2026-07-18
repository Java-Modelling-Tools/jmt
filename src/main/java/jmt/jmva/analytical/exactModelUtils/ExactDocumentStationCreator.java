package jmt.jmva.analytical.exactModelUtils;

import jmt.framework.data.ArrayUtils;
import jmt.jmva.analytical.ExactConstants;
import jmt.jmva.analytical.ExactModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import static jmt.jmva.analytical.exactModelUtils.ExactDocumentConstants.*;
import static jmt.jmva.analytical.exactModelUtils.ExactDocumentConstants.DOC_STATION_CUSTOMER_CLASS;

public class ExactDocumentStationCreator {

    public static void addStations(ExactModel model, Document root, Element parametersElement) {
        int stations = model.getStations();
        Element stationsElement = root.createElement("stations");
        parametersElement.appendChild(stationsElement);
        stationsElement.setAttribute(DOC_STATION_NUMBER, Integer.toString(stations));
        for (int i = 0; i < stations; i++) {
            stationsElement.appendChild(makeStationElement(model, root, i));
        }
    }

    private static Element makeStationElement(ExactModel model, Document root, int stationNum) {
        Element station_element;
        int[] stationTypes = model.getStationTypes();
        switch (stationTypes[stationNum]) {

        case ExactConstants.STATION_LI:
            station_element = root.createElement(DOC_STATION_TYPE_LI);
            addServiceTimesAndVisitsNotLDWithNumServers(model, root, stationNum, station_element);

            break;
        case ExactConstants.STATION_DELAY:
            station_element = root.createElement(DOC_STATION_TYPE_DELAY);
            addServiceTimesAndVisitsNotLD(model, root, stationNum, station_element);

            break;
        case ExactConstants.STATION_LD:
            station_element = root.createElement(DOC_STATION_TYPE_LD);
            addServiceTimesAndVisitsLD(model, root, station_element, stationNum);

            break;
        case ExactConstants.STATION_PRS:
            station_element = root.createElement(DOC_STATION_TYPE_PRS);
            addServiceTimesAndVisitsNotLDWithNumServers(model, root, stationNum, station_element);

            break;
        case ExactConstants.STATION_HOL:
            station_element = root.createElement(DOC_STATION_TYPE_HOL);
            addServiceTimesAndVisitsNotLDWithNumServers(model, root, stationNum, station_element);

            break;
        default:
            station_element = null;
        }//end switch
        return station_element;
    }

    private static void addServiceTimesAndVisitsLD(ExactModel model, Document root, Element station_element, int stationNum) {
        int classes = model.getClasses();
        String[] stationNames = model.getStationNames();
        String[] classNames = model.getClassNames();
        int[] stationServers = model.getStationServers();
        double[][][] serviceTimes = model.getServiceTimes();
        double[][] visits = model.getVisits();

        station_element.setAttribute(DOC_STATION_NAME, stationNames[stationNum]);
        station_element.setAttribute(DOC_STATION_SERVERS, String.valueOf(stationServers[stationNum]));

        /* create the section for service times */
        Node servicetimes_element = station_element.appendChild(root.createElement(DOC_STATION_SERVICE_TIMES));
        station_element.appendChild(servicetimes_element);

        /* create the section for visits */
        Node visits_element = station_element.appendChild(root.createElement(DOC_STATION_VISITS));
        station_element.appendChild(visits_element);

        /* for each customer class */
        for (int j = 0; j < classes; j++) {
            String class_name = classNames[j];
            /* set service times, one for each population (values are CSV formatted) */
            Element st_element = root.createElement(DOC_STATION_SERVICE_TIMES);
            st_element.setAttribute(DOC_STATION_CUSTOMER_CLASS, class_name);

            String serv_t = ArrayUtils.toCSV(serviceTimes[stationNum][j]);

            st_element.appendChild(root.createTextNode(serv_t));

            servicetimes_element.appendChild(st_element);
            /* set visit */
            Element visit_element = root.createElement(DOC_STATION_VISIT);
            visit_element.setAttribute(DOC_STATION_CUSTOMER_CLASS, class_name);
            visit_element.appendChild(root.createTextNode(Double.toString(visits[stationNum][j])));
            visits_element.appendChild(visit_element);
        }
    }

    private static void addServiceTimesAndVisitsNotLDWithNumServers(ExactModel model, Document root, int stationNum,
                                                                    Element station_element) {
        int[] stationServers = model.getStationServers();
        station_element.setAttribute(DOC_STATION_SERVERS, String.valueOf(stationServers[stationNum]));
        addServiceTimesAndVisitsNotLD(model, root, stationNum, station_element);
    }

    /**
     * Sets one value for service times and visits for each class
     * not one value per amount of customers
     */
    private static void addServiceTimesAndVisitsNotLD(ExactModel model, Document root, int stationNum,
                                                      Element station_element) {
        String[] stationNames = model.getStationNames();
        station_element.setAttribute(DOC_STATION_NAME, stationNames[stationNum]);

        /* create the section for service times */
        Node servicetimes_node = station_element.appendChild(root.createElement(DOC_STATION_SERVICE_TIMES));
        station_element.appendChild(servicetimes_node);

        /* create the section for visits */
        Node visits_node = station_element.appendChild(root.createElement(DOC_STATION_VISITS));
        station_element.appendChild(visits_node);

        /* for each customer class */
        setServiceTimesAndVisitsForEachClass(model, root, stationNum, servicetimes_node, visits_node);
    }

    private static void setServiceTimesAndVisitsForEachClass(ExactModel model, Document root, int stationNum,
                                                             Node servicetimes_node, Node visits_node) {
        int classes = model.getClasses();
        String[] classNames = model.getClassNames();
        double[][][] serviceTimes = model.getServiceTimes();
        double[][] visits = model.getVisits();

        /* for each customer class */
        for (int j = 0; j < classes; j++) {
            String class_name = classNames[j];
            /* set service time */
            Element st_element = root.createElement(DOC_STATION_SERVICE_TIME);
            st_element.setAttribute(DOC_STATION_CUSTOMER_CLASS, class_name);
            st_element.appendChild(root.createTextNode(Double.toString(serviceTimes[stationNum][j][0])));
            servicetimes_node.appendChild(st_element);
            /* set visit */
            Element visit_element = root.createElement(DOC_STATION_VISIT);
            visit_element.setAttribute(DOC_STATION_CUSTOMER_CLASS, class_name);
            visit_element.appendChild(root.createTextNode(Double.toString(visits[stationNum][j])));
            visits_node.appendChild(visit_element);
        }
    }
}
