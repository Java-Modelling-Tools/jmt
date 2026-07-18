package jmt.jmva.analytical.exactModelUtils;

import jmt.framework.data.ArrayUtils;
import jmt.jmva.analytical.ExactConstants;
import jmt.jmva.analytical.ExactModel;
import jmt.jmva.analytical.solvers.SolverAlgorithm;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Map;

import static jmt.jmva.analytical.exactModelUtils.ExactDocumentConstants.*;

public class ExactDocumentCreator {

    /**
     * Creates a DOM representation of this object
     *
     * @return a DOM representation of this object
     */
    public static Document createDocument(ExactModel model) {
        Document root;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            root = dbf.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException pce) {
            throw new RuntimeException(pce);
        }

        /* model */
        Element modelElement = createModelElement(root);

        /* description */
        addDescription(model, root, modelElement);

        /* parameters */
        Element parametersElement = root.createElement("parameters");
        modelElement.appendChild(parametersElement);
        /* classes */
        addClasses(model, root, parametersElement);
        /* stations */
        ExactDocumentStationCreator.addStations(model, root, parametersElement);
        /* Reference Stations */
        addReferenceStation(model, root, parametersElement);

        /* algorithm parameters */
        Element algParamsElement = root.createElement("algParams");
        modelElement.appendChild(algParamsElement);
        /* algorithm combo box */
        addAlgorithmComboBox(model, root, algParamsElement);
        /* compare algorithms box */
        addWhatIfAlgorithms(model, root, algParamsElement);
        /* What-if Analysis - Bertoli Marco */
        addWhatIfAnalysis(model, root, modelElement);

        if (model.hasResults() && model.areResultsOK()) {
            appendSolutionElement(model, root, modelElement);
        }

        return root;
    }

    private static Element createModelElement(Document root) {
        Element modelElement = root.createElement("model");

        modelElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        modelElement.setAttribute("xsi:noNamespaceSchemaLocation", "JMTmodel.xsd");

        root.appendChild(modelElement);
        return modelElement;
    }

    private static void addDescription(ExactModel model, Document root, Element modelElement) {
        String description = model.getDescription();
        if (!description.equals("")) {
            Element descriptionElement = root.createElement("description");
            descriptionElement.appendChild(root.createCDATASection(description));
            modelElement.appendChild(descriptionElement);
        }
    }

    private static void addClasses(ExactModel model, Document root, Element parametersElement) {
        int classes = model.getClasses();
        Element classes_element = root.createElement("classes");
        parametersElement.appendChild(classes_element);
        classes_element.setAttribute(DOC_CLASS_NUMBER, Integer.toString(classes));
        for (int i = 0; i < classes; i++) {
            classes_element.appendChild(makeClassElement(model, root, i));
        }
    }

    private static Element makeClassElement(ExactModel model, Document root, int classNum) {
        Element classElement;
        int[] classTypes = model.getClassTypes();
        String[] classNames = model.getClassNames();
        int[] classPriorities = model.getClassPriorities();
        double[] classData = model.getClassData();
        if (classTypes[classNum] == ExactConstants.CLASS_CLOSED) {
            classElement = root.createElement(DOC_CLASS_CLOSED);
            classElement.setAttribute(DOC_CLASS_POPULATION, Integer.toString((int) classData[classNum]));
        } else {
            classElement = root.createElement(DOC_CLASS_OPEN);
            classElement.setAttribute(DOC_CLASS_RATE, Double.toString(classData[classNum]));
        }
        classElement.setAttribute(DOC_CLASS_NAME, classNames[classNum]);
        classElement.setAttribute(DOC_CLASS_PRIORITY, Integer.toString(classPriorities[classNum]));
        return classElement;
    }

    private static void addAlgorithmComboBox(ExactModel model, Document root, Element algParamsElement) {
        Element algType_element = root.createElement("algType");
        algParamsElement.appendChild(algType_element);
        algType_element.setAttribute("name", model.getAlgorithmType().toString());
        algType_element.setAttribute("tolerance", Double.toString(model.getTolerance()));
        algType_element.setAttribute("maxSamples", Integer.toString(model.getMaxSamples()));
    }

    private static void addWhatIfAlgorithms(ExactModel model, Document root, Element algParamsElement) {
        Element compareAlgs_element = root.createElement("compareAlgs");
        compareAlgs_element.setAttribute("value", Boolean.toString(model.isWhatifAlgorithms()));
        algParamsElement.appendChild(compareAlgs_element);
        if (model.isWhatifAlgorithms()) {
            for (SolverAlgorithm algo : model.getWhatifAlgorithms()) {
                Element alg_element = root.createElement("whatIfAlg");
                compareAlgs_element.appendChild(alg_element);
                alg_element.setAttribute("name", algo.toString());
                alg_element.setAttribute("tolerance", Double.toString(model.getWhatifAlgorithmTolerance(algo)));
                alg_element.setAttribute("maxSamples", Integer.toString(model.getWhatifAlgorithmMaxSamples(algo)));
            }
        }
    }

    private static void addWhatIfAnalysis(ExactModel model, Document root, Element modelElement) {
        /* What-if Analysis - Bertoli Marco */
        if (model.isWhatIf()) {
            Element whatIf = root.createElement("whatIf");
            modelElement.appendChild(whatIf);
            whatIf.setAttribute("type", model.getWhatIfType());
            whatIf.setAttribute("values", ArrayUtils.toCSV(model.getWhatIfValues()));
            // Class name
            int whatIfClass = model.getWhatIfClass();
            String[] classNames = model.getClassNames();
            if (whatIfClass >= 0) {
                whatIf.setAttribute("className", classNames[whatIfClass]);
            }
            // Station name
            int whatIfStation = model.getWhatIfStation();
            String[] stationNames = model.getStationNames();
            if (whatIfStation >= 0) {
                whatIf.setAttribute("stationName", stationNames[whatIfStation]);
            }
        }
    }

    private static void addReferenceStation(ExactModel model, Document root, Element parametersElement) {
        int classes = model.getClasses();
        Element referenceStationElement = root.createElement("ReferenceStation");
        parametersElement.appendChild(referenceStationElement);
        referenceStationElement.setAttribute("number", Integer.toString(classes));
        for (int i = 0; i < classes; i++) {
            referenceStationElement.appendChild(makeReferenceStationElement(model, root, i));
        }
    }

    private static Element makeReferenceStationElement(ExactModel model, Document root, int classNum) {
        Element classElement;
        String[] myStationNames = model.getStationNames();
        myStationNames = ArrayUtils.resize(myStationNames, myStationNames.length + 1, null);
        myStationNames[myStationNames.length - 1] = "Arrival Process";
        classElement = root.createElement("Class");
        classElement.setAttribute("name", model.getClassNames()[classNum]);
        classElement.setAttribute("refStation", myStationNames[(int) model.getReferenceStation()[classNum]]);
        return classElement;
    }

    /**
     * Appends solution elements to model element
     * @param root root element of Document
     * @param parentElement model element where solutions have to be appended
     * <br>
     * Author: Bertoli Marco
     */
    private static void appendSolutionElement(ExactModel model, Document root, Element parentElement) {
        int classes = model.getClasses();
        int stations = model.getStations();
        double[] whatIfValues = model.getWhatIfValues();

        Map<SolverAlgorithm, double[][][]> queueLen = model.getQueueLen();
        Map<SolverAlgorithm, double[][][]> throughput = model.getThroughput();
        Map<SolverAlgorithm, double[][][]> resTimes = model.getResidTimes();
        Map<SolverAlgorithm, double[][][]> util = model.getUtilization();

        Map<SolverAlgorithm, Double> logNormConst = model.getLogNormConst();
        Map<SolverAlgorithm, int[]> algIterations = model.getAlgIterations();

        String[] classNames = model.getClassNames();
        String[] stationNames = model.getStationNames();

        for (int k = 0; k < model.getIterations(); k++) {
            Element result_element = root.createElement("solutions");
            result_element.setAttribute("ok", "true");
            if (!model.isWhatIf()) {
                result_element.setAttribute("solutionMethod", "analytical");
            } else {
                result_element.setAttribute("solutionMethod", "analytical whatif");
                result_element.setAttribute("iteration", Integer.toString(k));
                result_element.setAttribute("iterationValue", Double.toString(whatIfValues[k]));
            }
            result_element.setAttribute("algCount", Integer.toString(queueLen.size()));

            for (SolverAlgorithm alg : queueLen.keySet()) {
                Element algorithm_element = (Element) result_element.appendChild(root.createElement("algorithm"));
                algorithm_element.setAttribute("name", alg.toString());
                algorithm_element.setAttribute("iterations", Integer.toString(algIterations.get(alg)[k]));

                for (int i = 0; i < stations; i++) {
                    Element stationresults_element = (Element) algorithm_element.appendChild(root.createElement("stationresults"));
                    stationresults_element.setAttribute("station", stationNames[i]);
                    for (int j = 0; j < classes; j++) {
                        Element classesresults_element = (Element) stationresults_element.appendChild(root.createElement("classresults"));
                        classesresults_element.setAttribute("customerclass", classNames[j]);

                        Element Q_element = (Element) classesresults_element.appendChild(root.createElement("measure"));
                        Q_element.setAttribute("measureType", "Number of Customers");
                        Q_element.setAttribute("successful", "true");
                        Q_element.setAttribute("meanValue", Double.toString(queueLen.get(alg)[i][j][k]));

                        Element X_element = (Element) classesresults_element.appendChild(root.createElement("measure"));
                        X_element.setAttribute("measureType", "Throughput");
                        X_element.setAttribute("successful", "true");
                        X_element.setAttribute("meanValue", Double.toString(throughput.get(alg)[i][j][k]));

                        Element R_element = (Element) classesresults_element.appendChild(root.createElement("measure"));
                        R_element.setAttribute("measureType", "Residence time");
                        R_element.setAttribute("successful", "true");
                        R_element.setAttribute("meanValue", Double.toString(resTimes.get(alg)[i][j][k]));

                        Element U_element = (Element) classesresults_element.appendChild(root.createElement("measure"));
                        U_element.setAttribute("measureType", "Utilization");
                        U_element.setAttribute("successful", "true");
                        U_element.setAttribute("meanValue", Double.toString(util.get(alg)[i][j][k]));
                    }
                }

                Element nc_element = (Element) algorithm_element.appendChild(root.createElement("normconst"));
                nc_element.setAttribute("logValue", Double.toString(logNormConst.get(alg).doubleValue()));
            }

            parentElement.appendChild(result_element);
        }
    }

}
