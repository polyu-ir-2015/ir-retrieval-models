package hk.edu.polyu.ir.groupc.searchengine;

import hk.edu.polyu.ir.groupc.searchengine.frontend.MainApplication;
import hk.edu.polyu.ir.groupc.searchengine.frontend.MainController;
import hk.edu.polyu.ir.groupc.searchengine.model.retrievalmodel.*;

import java.util.ArrayList;

/**
 * Created by beenotung on 11/23/15.
 */
public class GuiTest {
    public static void main(String [] args){
        ArrayList<RetrievalModel> models = MainController.MODELS();
        /* clear origin models, e.g. simple model */
        models.clear();
        /* register models */
        models.add(new BooleanModel());
        models.add(new VectorSpaceModel());
        models.add(new SetBasedVectorSpaceModel());
        models.add(new ExtendedBooleanModel());

        MainApplication.main(args);
    }
}
