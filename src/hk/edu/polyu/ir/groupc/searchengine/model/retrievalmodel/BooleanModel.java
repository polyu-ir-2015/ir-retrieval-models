package hk.edu.polyu.ir.groupc.searchengine.model.retrievalmodel;

import comm.lang.ScalaSupport;
import hk.edu.polyu.ir.groupc.searchengine.model.datasource.SearchResult;
import hk.edu.polyu.ir.groupc.searchengine.model.datasource.SearchResultFactory;
import hk.edu.polyu.ir.groupc.searchengine.model.datasource.TermEntity;
import hk.edu.polyu.ir.groupc.searchengine.model.query.ExpandedTerm;
import hk.edu.polyu.ir.groupc.searchengine.model.query.Query;
import hk.edu.polyu.ir.groupc.searchengine.model.query.RetrievalDocument;
import hk.edu.polyu.ir.groupc.searchengine.model.query.RetrievalModel;
import scala.Option;
import scala.Tuple2;
import scala.collection.mutable.ArrayBuffer;
import scala.collection.mutable.HashMap;

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

    private void addToList(HashMap<Object, ArrayBuffer<Object>> map, List<RetrievalDocument> list, double score) {
        ScalaSupport.foreachMap(map, new Consumer<Tuple2<Object, ArrayBuffer<Object>>>() {
            @Override
            public void accept(Tuple2<Object, ArrayBuffer<Object>> pair) {
                list.add(new RetrievalDocument((Integer) pair._1(), score));
            }
        });
    }

    public SearchResult search_ext(Query query, int numResult) {
        List<RetrievalDocument> result_list = new ArrayList<>();

        List<RetrievalDocument> and_list = new ArrayList<>();
        List<RetrievalDocument> or_list = new ArrayList<>();
        List<RetrievalDocument> not_list = new ArrayList<>();
        for (ExpandedTerm expandedTerm : query.expandedTerms()) {
            if (expandedTerm.weight() > 0) {
                addToList(expandedTerm.term().filePositionMap(), and_list, 1);
            } else if (expandedTerm.weight() < 0) {
                addToList(expandedTerm.term().filePositionMap(), not_list, 1);
            } else {
                addToList(expandedTerm.term().filePositionMap(), or_list, 1);
            }
        }
        //TODO merge list

        return SearchResultFactory.create(query, result_list);
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
