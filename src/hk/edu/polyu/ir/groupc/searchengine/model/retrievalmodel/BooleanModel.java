package hk.edu.polyu.ir.groupc.searchengine.model.retrievalmodel;

import comm.lang.ScalaSupport;
import hk.edu.polyu.ir.groupc.searchengine.model.query.ExpandedTerm;
import hk.edu.polyu.ir.groupc.searchengine.model.query.Query;
import hk.edu.polyu.ir.groupc.searchengine.model.result.RetrievalDocument;
import scala.Tuple2;
import scala.collection.mutable.ArrayBuffer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by beenotung on 11/12/15.
 */
public class BooleanModel extends RetrievalModel {
    private static final String MODE_OR = "OR";
    private static final String MODE_AND = "AND";
    static final String MODE_DEFAULT = MODE_AND;
    private static final List<String> MODES;
    private static final List<Parameter<? extends Number>> Parameters;
    private static final int NOT = -1;
    private static final int AND = 1;
    private static final int OR = 0;


    static {
        MODES = new LinkedList<>();
        MODES.add(MODE_AND);
        MODES.add(MODE_OR);
        Parameters = new LinkedList<>();
    }

    String mode = MODE_DEFAULT;

    @Override
    public List<String> getModes() {
        return MODES;
    }

    @Override
    public String getDefaultMode() {
        return MODE_DEFAULT;
    }

    @Override
    public String getMode() {
        return mode;
    }

    @Override
    public void setMode(String newMode) {
        mode = newMode;
    }

    @Override
    public List<Parameter<? extends Number>> getParameters() {
        return Parameters;
    }

    public List<RetrievalDocument> search(Query query) {
        List<RetrievalDocument> list = new ArrayList<>();
        ArrayList<Integer> int_list = new ArrayList<Integer>();
        for (ExpandedTerm termEntity : query.expandedTerms()) {
            int weight = termEntity.weight() > 0 ? AND : ((termEntity.weight() < 0) ? NOT : OR);
            /* distinct mode from AND or OR */
            if (weight != NOT) {
                if (mode.equals(MODE_AND))
                    weight = AND;
                else
                    weight = OR;
            }
            switch (weight) {
                case NOT:
                    ScalaSupport.foreachMap(termEntity.term().filePositionMap(), new Consumer<Tuple2<Object, ArrayBuffer<Object>>>() {
                        @Override
                        public void accept(Tuple2<Object, ArrayBuffer<Object>> pair) {
                            if (int_list.contains((Integer) pair._1()))
                                int_list.remove((Integer) pair._1());
                        }
                    });
                    break;
                case OR:
                    ScalaSupport.foreachMap(termEntity.term().filePositionMap(), new Consumer<Tuple2<Object, ArrayBuffer<Object>>>() {
                        @Override
                        public void accept(Tuple2<Object, ArrayBuffer<Object>> pair) {
                            if (!int_list.contains((Integer) pair._1()))
                                int_list.add((Integer) pair._1());
                        }
                    });
                    break;
                case AND:
                    ScalaSupport.foreachMap(termEntity.term().filePositionMap(), new Consumer<Tuple2<Object, ArrayBuffer<Object>>>() {
                        @Override
                        public void accept(Tuple2<Object, ArrayBuffer<Object>> pair) {
                            if (!int_list.contains((Integer) pair._1()))
                                int_list.remove((Integer) pair._1());
                            // list.add(new RetrievalDocument((Integer) pair._1(), 1));
                        }
                    });
                    break;
            }

        }
        for (int docID : int_list) {
            list.add(new RetrievalDocument(docID, 1));
        }
        return list;
    }
}