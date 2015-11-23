package hk.edu.polyu.ir.groupc.searchengine;

import comm.exception.RichFileNotFoundException;
import hk.edu.polyu.ir.groupc.searchengine.model.result.SearchResultFactory;
import hk.edu.polyu.ir.groupc.searchengine.model.retrievalmodel.ExtendedBooleanModel;
import hk.edu.polyu.ir.groupc.searchengine.model.retrievalmodel.RetrievalModel;
import hk.edu.polyu.ir.groupc.searchengine.model.retrievalmodel.VectorSpaceModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Created by beenotung on 11/12/15.
 */
public class Test {

    public static final String FILE_PATH = "res/file.txt";
    public static final String TERM_INDEX_PATH = "res/term_index.txt";
    public static final String POST_PATH = "res/post1.txt";
    public static final String STOP_PATH = "res/estop.lst";
    public static final String JUDGEROBUST = "res/judgerobust";
    public static final String QUERY_T = "res/queryT";
    public static final String QUERY_TDN = "res/queryTDN";
    private static final String RESULT_FILE = "res/result.txt";

    public static void main(String[] args) throws RichFileNotFoundException {
        System.out.println("start");

        Launcher launcher = new Launcher() {
            {
                filePath(FILE_PATH);
                termIndexPath(TERM_INDEX_PATH);
                postPath(POST_PATH);
                stopPath(STOP_PATH);
                judgeRobustPath(JUDGEROBUST);
                queryPath(QUERY_TDN);
            }
        };

        SearchResultFactory.setRunId("GROUP-C");

        // Run each model and generate a set of result text files.
        ArrayList<ModelSetting> allModelSettings = getAllModels();
        for (ModelSetting modelSetting : allModelSettings) {
            System.out.println("=========================================================");
            launcher.start(modelSetting.model, modelSetting.getResultFilePath(), modelSetting.numberOfRetrieval);
            System.out.println("=========================================================");
        }

        // For each generated text file, input it into the trec_eval_cmd.exe evaluation program and extract some output values.
        Runtime runtime = Runtime.getRuntime();
        String currentPath = System.getProperty("user.dir");
        for (ModelSetting allModelSetting : allModelSettings) {
            String evalProgramPath = currentPath + "\\res\\trec_eval_cmd.exe";
            String judgePath = currentPath + "\\res\\judgerobust";
            String resultPath = currentPath + "\\" + allModelSetting.getResultFilePath();

            ArrayList<String> command = new ArrayList<>();
            command.add(evalProgramPath);
            command.add("-a"); // Show detailed evaluation report
            command.add(judgePath);
            command.add(resultPath);
            try {
                ProcessBuilder builder = new ProcessBuilder(command);
                builder.redirectErrorStream(true);
                builder.directory(new File(System.getenv("temp")));

                Process process = builder.start();
                BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                PrintWriter writer = new PrintWriter(allModelSetting.getEvalFilePath());
                while ((line = input.readLine()) != null) {
                    writer.println(line);
                }
                process.waitFor();
                writer.flush();
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("end");
    }

    private static ArrayList<ModelSetting> getAllModels() {
        ArrayList<ModelSetting> allModels = new ArrayList<>();
        VectorSpaceModel model;
        ModelSetting modelSetting;

        ////////////////////////////////////////////////
        // Change your testing model's settings here. //
        ////////////////////////////////////////////////
        model = new VectorSpaceModel();
        model.setNormalizationType(VectorSpaceModel.NormalizationType.NONE);
        modelSetting = new ModelSetting();
        modelSetting.model = model;
        modelSetting.customDescription = "NONE";
        modelSetting.numberOfRetrieval = 100;
        allModels.add(modelSetting);

        model = new VectorSpaceModel();
        model.setNormalizationType(VectorSpaceModel.NormalizationType.COSINE);
        modelSetting = new ModelSetting();
        modelSetting.model = model;
        modelSetting.customDescription = "COSINE";
        modelSetting.numberOfRetrieval = 100;
        allModels.add(modelSetting);

        model = new VectorSpaceModel();
        model.setNormalizationType(VectorSpaceModel.NormalizationType.PIVOT);
        modelSetting = new ModelSetting();
        modelSetting.model = model;
        modelSetting.customDescription = "PIVOT";
        modelSetting.numberOfRetrieval = 100;
        allModels.add(modelSetting);

        model = new VectorSpaceModel();
        model.setNormalizationType(VectorSpaceModel.NormalizationType.BM25);
        modelSetting = new ModelSetting();
        modelSetting.model = model;
        modelSetting.customDescription = "BM25";
        modelSetting.numberOfRetrieval = 100;
        allModels.add(modelSetting);

        ExtendedBooleanModel extendedModel;
        extendedModel = new ExtendedBooleanModel();
        extendedModel.setOperationType(ExtendedBooleanModel.OperationType.AND);
        modelSetting = new ModelSetting();
        modelSetting.model = extendedModel;
        modelSetting.customDescription = "";
        modelSetting.numberOfRetrieval = 100;
        allModels.add(modelSetting);
        ////////////////////////////////////////////////
        // Change your testing model's settings here. //
        ////////////////////////////////////////////////

        return allModels;
    }

    private static class ModelSetting {
        public RetrievalModel model;
        public String customDescription;
        public int numberOfRetrieval;

        public String getResultFilePath() {
            return "res\\result\\result-" + model.getClass().getSimpleName() + "-" + customDescription + "-" + numberOfRetrieval + ".txt";
        }

        public String getEvalFilePath() {
            return "res\\result\\eval-" + model.getClass().getSimpleName() + "-" + customDescription + "-" + numberOfRetrieval + ".txt";
        }
    }
}