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
  
    public BooleanModel() {
       
    }

    public List<RetrievalDocument> search(Query query) {
        List<RetrievalDocument> list = new ArrayList<>();
        ArrayList<Integer> int_list = new ArrayList<Integer>();
        for (ExpandedTerm termEntity : query.expandedTerms()) {
			int weight = termEntity.weight()>0?1:((termEntity.weight()<0)?-1:0);
			switch (weight){
				case -1: 
						ScalaSupport.foreachMap(termEntity.term().filePositionMap(), new Consumer<Tuple2<Object, ArrayBuffer<Object>>>() {
							@Override
							public void accept(Tuple2<Object, ArrayBuffer<Object>> pair) {
							   if (int_list.contains((Integer) pair._1()))
								   int_list.remove((Integer) pair._1());
							}
						});
				break; 
				case 0:
						ScalaSupport.foreachMap(termEntity.term().filePositionMap(), new Consumer<Tuple2<Object, ArrayBuffer<Object>>>() {
							@Override
							public void accept(Tuple2<Object, ArrayBuffer<Object>> pair) {
								if (!int_list.contains((Integer) pair._1()))
									int_list.add((Integer) pair._1());
							}
						});
				break;
				case 1:
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
        for (int docID : int_list){
        	list.add(new RetrievalDocument(docID, 1));
        }
        return list;
    }
}