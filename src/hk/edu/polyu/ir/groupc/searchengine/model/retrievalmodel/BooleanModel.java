package hk.edu.polyu.ir.groupc.searchengine.model.retrievalmodel;

import hk.edu.polyu.ir.groupc.searchengine.model.datasource.SearchResult;
import hk.edu.polyu.ir.groupc.searchengine.model.query.Query;
import hk.edu.polyu.ir.groupc.searchengine.model.query.RetrievalModel;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Created by beenotung on 11/12/15.
 */
public class BooleanModel extends RetrievalModel {
    @Override
    public SearchResult search(Query query, int numResult) {
        return null;
    }
}