package hk.edu.polyu.ir.groupc.searchengine.model.query;

import hk.edu.polyu.ir.groupc.searchengine.model.datasource.SearchResult;
import hk.edu.polyu.ir.groupc.searchengine.model.datasource.SearchResultFactory;
import scala.util.Random;

import java.util.ArrayList;

/**
 * Created by beenotung on 11/12/15.
 */
public class DummyModel extends RetrievalModel {
    @Override
    public SearchResult search(Query query) {
        ArrayList<RetrievalDocument> retrievalDocuments = new ArrayList<>();
        Random random=new Random();
        retrievalDocuments.add(new RetrievalDocument(random.nextInt(10)+1,random.nextDouble()));
        return SearchResultFactory.create(query, retrievalDocuments);
    }
}
