package jmt.jmva.dataCollection;

import jmt.gui.common.xml.ModelLoader;
import jmt.jmva.analytical.CommandLineSolver;
import jmt.jmva.analytical.ExactModel;
import jmt.jmva.analytical.solvers.SolverAlgorithm;
import jmt.jmva.gui.utilities.SolverAlgorithmCounter;

import java.io.File;
import java.util.Arrays;

import static jmt.jmva.dataCollection.DataCollectUtils.*;

public class SolveAndSaveModel {

    private static final boolean VERBOSE = true;
    private static final ModelLoader modelLoader = new ModelLoader(ModelLoader.JMVA, ModelLoader.JMVA_SAVE);

    private static final String MODEL1_FN = "/home/ben_pahnke/IndProj/individualproject/WriteUp/Code/Bondi/Solved/Model1/Model1_SCA.xml";
    private static final String MODEL2_FN = "/home/ben_pahnke/IndProj/individualproject/WriteUp/Code/Bondi/Solved/Model2/Model2_SCA.xml";
    private static final String MODEL3_FN = "/home/ben_pahnke/IndProj/individualproject/WriteUp/Code/Bondi/Solved/Model3/Model3_SCA.xml";

    private static final String MODELH1_FN = "/home/ben_pahnke/IndProj/individualproject/WriteUp/Code/HOL/Solved/ModelH1/ModelH1_SCA.xml";
    private static final String MODELH2_FN = "/home/ben_pahnke/IndProj/individualproject/WriteUp/Code/HOL/Solved/ModelH2/ModelH2_SCA.xml";
    private static final String MODELH3_FN = "/home/ben_pahnke/IndProj/individualproject/WriteUp/Code/HOL/Solved/ModelH3/ModelH3_SCA.xml";

    private static final String MODELHA_FN = "/home/ben_pahnke/IndProj/individualproject/WriteUp/Code/Let_HOL/Solved/ModelHA/ModelHA_SCA.xml";
    private static final String MODELHB_FN = "/home/ben_pahnke/IndProj/individualproject/WriteUp/Code/Let_HOL/Solved/ModelHB/ModelHB_SCA.xml";
    //private static final String MODELHC_FN = "/home/ben_pahnke/IndProj/individualproject/WriteUp/Code/HOL/Solved/ModelHC/ModelHC_SCA.xml";

    public static void main(String[] args) {
        solveModelAllAlgs(MODEL1_FN);
        solveModelAllAlgs(MODEL2_FN);
        solveModelAllAlgs(MODEL3_FN);

        solveModelAllAlgs(MODELH1_FN);
        solveModelAllAlgs(MODELH2_FN);
        solveModelAllAlgs(MODELH3_FN);

        solveModelAllAlgs(MODELHA_FN);
        solveModelAllAlgs(MODELHB_FN);
        //solveModelAllAlgs(MODELHC_FN);
    }

    public static void solveModelAllAlgs(String modelFilename) {
        if (VERBOSE) {
            System.out.println();
            System.out.println(modelFilename);
            System.out.println();
        }
        String fileContent = getFileContent(modelFilename);
        for (SolverAlgorithm alg : SolverAlgorithmCounter.getPriorityAlgorithms(
                Arrays.asList(SolverAlgorithm.closedValues()), false)) {
            String filename = changeAlgorithmInFile(modelFilename, alg, fileContent);
            CommandLineSolver solver = new CommandLineSolver();
            solver.solve(filename);
            filename = filename.substring(0, filename.length() - 3) + "jmva";
            File newFile = new File(filename);
            ExactModel model = solver.getSolverDispatcher().getModel();
            modelLoader.saveModel(model, null, newFile);
            if (VERBOSE) {
                System.out.println(alg.toString());
            }
        }
    }
}
