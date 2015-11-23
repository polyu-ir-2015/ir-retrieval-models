package hk.edu.polyu.ir.groupc.searchengine;

import hk.edu.polyu.ir.groupc.searchengine.frontend.MainApplication;
import hk.edu.polyu.ir.groupc.searchengine.frontend.MainController;
import hk.edu.polyu.ir.groupc.searchengine.model.retrievalmodel.ExtendedBooleanModel;
import hk.edu.polyu.ir.groupc.searchengine.model.retrievalmodel.RetrievalModel;
import hk.edu.polyu.ir.groupc.searchengine.model.retrievalmodel.SetBasedVectorSpaceModel;
import hk.edu.polyu.ir.groupc.searchengine.model.retrievalmodel.VectorSpaceModel;

import java.util.ArrayList;

/**
 * Created by beenotung on 11/23/15.
 */
public class GuiTest {
    public static void main(String [] args){
        ArrayList<RetrievalModel> models = MainController.MODELS();
        /* clear origin models, e.g. simple model */
//        models.clear();
        /* register models */
        models.add(new VectorSpaceModel());
        models.add(new SetBasedVectorSpaceModel());
        models.add(new ExtendedBooleanModel());

        MainApplication.main(args);
    }
}
