package hk.edu.polyu.ir.groupc.searchengine.model.query;

import hk.edu.polyu.ir.groupc.searchengine.model.datasource.SearchResult;
import hk.edu.polyu.ir.groupc.searchengine.model.datasource.SearchResultFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 *
 * Created by nEbuLa on 14/11/2015.
 *
 * Description:     This class provides method for converting a hash map data structure
 *                  to an array list which contains RetrievalDocument objects. It also
 *                  provides sorting method to sort the array list by ranking in descending
 *                  order.
 *
 *                  It is suggested that any retrieval model that outputs ranking should
 *                  inherit this class.
 *
 */
abstract public class RetrievalModelWithRanking extends RetrievalModel {

    // Models should implement this method and return a Hash map, which keys are document IDs,
    // and values are ranking score decimal number.
    abstract protected HashMap<Integer, Double> getRankedDocumentsWithoutSort(Query pQuery);

    @Override
    public SearchResult search(Query pQuery, int pNumOfRetrievalDocument) {
        HashMap<Integer, Double> rankedDocuments = this.getRankedDocumentsWithoutSort(pQuery);

        // Help to sort the ranked documents and return an array list of RetrievalDocument objects
        // rather than a hash map.
        ArrayList<RetrievalDocument> theArrayList = new ArrayList<>();

        for (HashMap.Entry<Integer, Double> singleDocument : rankedDocuments.entrySet()) {
            Integer documentID = singleDocument.getKey();
            Double documentScore = singleDocument.getValue();
            theArrayList.add(new RetrievalDocument(documentID, documentScore));
        }

        Collections.sort(theArrayList, new Comparator<RetrievalDocument>() {
            @Override
            public int compare(RetrievalDocument pDocument1, RetrievalDocument pDocument2) {
                // Sort by descending order using ranking score.
                return Double.compare(pDocument2.similarityScore, pDocument1.similarityScore);
            }
        });

        // TODO: need to remove extra items based on the number of pNumOfRetrievalDocument required.

        return SearchResultFactory.create(pQuery, theArrayList);
    }

}
