package hk.edu.polyu.ir.groupc.searchengine.model.query;

import hk.edu.polyu.ir.groupc.searchengine.model.datasource.SearchResult;
import hk.edu.polyu.ir.groupc.searchengine.model.datasource.SearchResultFactory;

import java.util.ArrayList;

/**
 * Created by beenotung on 11/12/15.
 */
public class DummyModel extends RetrievalModel {
    @Override
    public SearchResult search(Query query) {
        ArrayList<RetrievalDocument> retrievalDocuments = new ArrayList<>();
        int fileId = 12;
        double score = 100;
        retrievalDocuments.add(new RetrievalDocument(fileId, score));
        fileId = 23;
        score = 80;
        retrievalDocuments.add(new RetrievalDocument(fileId, score));
        return SearchResultFactory.create(retrievalDocuments);
    }
}
