package hk.edu.polyu.ir.groupc.searchengine.model.query;

import comm.lang.ScalaSupport;
import hk.edu.polyu.ir.groupc.searchengine.model.datasource.SearchResult;
import hk.edu.polyu.ir.groupc.searchengine.model.datasource.SearchResultFactory;
import scala.Tuple2;
import scala.collection.mutable.ArrayBuffer;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Created by beenotung on 11/12/15.
 */
public class SimpleModel extends RetrievalModel {
    @Override
    public SearchResult search(Query query, int numOfRetrievalDocument) {
        ConcurrentLinkedQueue<RetrievalDocument> retrievalDocuments = new ConcurrentLinkedQueue<>();
        for (ExpandedTerm expandedTerm : query.expandedTerms()) {
            ScalaSupport.foreachParMap(expandedTerm.term().filePositionMap(), new Consumer<Tuple2<Object, ArrayBuffer<Object>>>() {
                @Override
                public void accept(Tuple2<Object, ArrayBuffer<Object>> e) {
                    retrievalDocuments.add(new RetrievalDocument((int) e._1(), e._2().size() * expandedTerm.weight()));
                }
            });
        }
        RetrievalDocument[] docs = new RetrievalDocument[Math.min(numOfRetrievalDocument, retrievalDocuments.size())];
        int i = 0;
        while (i < docs.length)
            docs[i++] = retrievalDocuments.poll();
        Arrays.sort(docs);
        return SearchResultFactory.create(query, Arrays.asList(docs));
    }
}
