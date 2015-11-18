package hk.edu.polyu.ir.groupc.searchengine;

import hk.edu.polyu.ir.groupc.searchengine.model.datasource.SearchResultFactory;
import hk.edu.polyu.ir.groupc.searchengine.model.query.RetrievalModel;
import hk.edu.polyu.ir.groupc.searchengine.model.retrievalmodel.VectorSpaceModel;

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

    public static void main(String[] args) {
        System.out.println("start");
        Launcher launcher = new Launcher() {
            @Override
            public String FILE_PATH() {
                return FILE_PATH;
            }

            @Override
            public String TERM_INDEX_PATH() {
                return TERM_INDEX_PATH;
            }

            @Override
            public String POST_PATH() {
                return POST_PATH;
            }

            @Override
            public String STOP_PATH() {
                return STOP_PATH;
            }

            @Override
            public String JUDGEROBUST() {
                return JUDGEROBUST;
            }

            @Override
            public String QUERY() {
                return QUERY_T;
            }
        };

        RetrievalModel model = new VectorSpaceModel();
        SearchResultFactory.setRunId("GROUP-C-" + model.getClass().getSimpleName());

        launcher.start(model, RESULT_FILE, 100);
        System.out.println("end");
    }
}
