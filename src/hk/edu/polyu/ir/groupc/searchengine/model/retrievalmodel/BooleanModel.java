package hk.edu.polyu.ir.groupc.searchengine.model.retrievalmodel;

import hk.edu.polyu.ir.groupc.searchengine.model.query.Query;
import hk.edu.polyu.ir.groupc.searchengine.model.query.RetrievalModel;
import hk.edu.polyu.ir.groupc.searchengine.model.result.RetrievalDocument;

import java.util.List;

/**
 * Created by beenotung on 11/12/15.
 */
public class BooleanModel extends RetrievalModel {

    //TODO to merge
    @Override
    public List<String> getModes() {
        return null;
    }

    @Override
    public String getDefaultMode() {
        return null;
    }

    @Override
    public String getMode() {
        return null;
    }

    @Override
    public void setMode(String newMode) {

    }

    @Override
    public List<Parameter> getParameters() {
        return null;
    }

    @Override
    public List<RetrievalDocument> search(Query query) {
        return null;
    }
}
