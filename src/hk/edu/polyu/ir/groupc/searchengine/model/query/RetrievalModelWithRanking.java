package hk.edu.polyu.ir.groupc.searchengine.model.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
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

    protected ArrayList<RetrievalDocument> convertToRetrievalDocumentArrayList(HashMap<Integer, Double> pInputHashMap) {
        ArrayList<RetrievalDocument> theArrayList = new ArrayList<>();

        for (HashMap.Entry<Integer, Double> singleDocument : pInputHashMap.entrySet()) {
            Integer documentID = singleDocument.getKey();
            Double documentScore = singleDocument.getValue();

            theArrayList.add(new RetrievalDocument(documentID, documentScore));
        }

        return theArrayList;
    }

    protected void sortRetrievalDocumentArrayListByDescRanking(ArrayList<RetrievalDocument> pTheArrayList) {
        Collections.sort(pTheArrayList, new Comparator<RetrievalDocument>() {
            @Override
            public int compare(RetrievalDocument pDocument1, RetrievalDocument pDocument2) {
                // Sort by descending order using ranking score.
                return Double.compare(pDocument2.similarityScore, pDocument1.similarityScore);
            }
        });
    }

}
