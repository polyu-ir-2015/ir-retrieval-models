package hk.edu.polyu.ir.groupc.searchengine.model.retrievalmodel;

import comm.lang.ScalaSupport;
import hk.edu.polyu.ir.groupc.searchengine.model.datasource.SearchResult;
import hk.edu.polyu.ir.groupc.searchengine.model.datasource.SearchResultFactory;
import hk.edu.polyu.ir.groupc.searchengine.model.datasource.TermEntity;
import hk.edu.polyu.ir.groupc.searchengine.model.query.Query;
import hk.edu.polyu.ir.groupc.searchengine.model.query.RetrievalDocument;
import hk.edu.polyu.ir.groupc.searchengine.model.query.RetrievalModel;
import scala.Option;
import scala.Tuple2;
import scala.collection.mutable.ArrayBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by beenotung on 11/12/15.
 */
public class BooleanModel extends RetrievalModel {
    public static final int MODE_AND = 2;
    public static final int MODE_OR = 1;
    public static final int MODE_DEFAULT = MODE_OR;
    final int mode;

    public BooleanModel() {
        mode = MODE_DEFAULT;
    }

    public BooleanModel(int mode) {
        this.mode = mode;
    }

    public SearchResult search_and(Query query, int numResult) {
        //TODO implement and
        List<RetrievalDocument> list = new ArrayList<>();
        for (Option<TermEntity> termEntityOption : query.simpleTerms()) {
            if (termEntityOption.isDefined()) {
                TermEntity termEntity = termEntityOption.get();
                ScalaSupport.foreachMap(termEntity.filePositionMap(), new Consumer<Tuple2<Object, ArrayBuffer<Object>>>() {
                    @Override
                    public void accept(Tuple2<Object, ArrayBuffer<Object>> pair) {
                        list.add(new RetrievalDocument((Integer) pair._1(), 1));
                    }
                });
            }
        }
        return SearchResultFactory.create(query, list);
    }

    public SearchResult search_or(Query query, int numResult) {
        List<RetrievalDocument> list = new ArrayList<>();
        for (Option<TermEntity> termEntityOption : query.simpleTerms()) {
            if (termEntityOption.isDefined()) {
                TermEntity termEntity = termEntityOption.get();
                ScalaSupport.foreachMap(termEntity.filePositionMap(), new Consumer<Tuple2<Object, ArrayBuffer<Object>>>() {
                    @Override
                    public void accept(Tuple2<Object, ArrayBuffer<Object>> pair) {
                        list.add(new RetrievalDocument((Integer) pair._1(), 1));
                    }
                });
            }
        }
        return SearchResultFactory.create(query, list);
    }

    @Override
    public SearchResult search(Query query, int numResult) {
        switch (mode) {
            case MODE_AND:
                return search_or(query, numResult);
            case MODE_OR:
            default:
                return search_and(query, numResult);
        }
    }
}
