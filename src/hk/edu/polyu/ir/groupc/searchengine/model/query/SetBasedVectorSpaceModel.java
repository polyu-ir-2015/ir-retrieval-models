package hk.edu.polyu.ir.groupc.searchengine.model.query;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by nEbuLa on 15/11/2015.
 *
 * Set Based Model
 *
 * Description:     This model is built based on a published paper (see references below). This model
 *                  combines set theory and vector space model ranking. The fundamental
 *                  idea is to use mutual dependencies among index terms to improve results. Term dependencies
 *                  are captured through term-sets, which are sets of correlated terms.
 *
 *                  The algorithm of this model is to find all frequent term-sets (similar to finding
 *                  association rules in data mining) based on the query terms. Term-sets refer to a set
 *                  of query terms that appeared in documents. These frequent term-sets
 *                  are then treated as if a single term and calculate values such as TF and IDF, and
 *                  use ranking functions in vector space model.
 *
 *                  For a simple example, consider the query contains terms A, B, C, D, E. After getting
 *                  values from inverted index, we found out that A, B, D, E, AB, AD, BD and ABD are frequent
 *                  term-sets. We then calculate their TF and IDF values, and apply vector space model.
 *
 * References:      http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.116.7973&rep=rep1&type=pdf
 *                  http://grupoweb.upf.es/WRG/mir2ed/pdf/slides_chap03.pdf (see Set-based model section)
 */
public class SetBasedVectorSpaceModel extends VectorSpaceModel {

    @Override
    public HashMap<Integer, Double> getRankedDocumentsWithoutSort(Query pQuery) {
        // retrievedDocuments will have a structure <Document ID, ranking score>
        HashMap<Integer, Double> retrievedDocuments = new HashMap<>();

        // Get all frequent query term-sets based on the input query.
        ArrayList<QueryTermSet> allQueryTermSets = this.getAllQueryTermSets(pQuery);

        // For each term-sets, we calculate its accumulated score for each document.
        for(QueryTermSet theQueryTermSet: allQueryTermSets) {
            double queryTermSetWeight = theQueryTermSet.getWeight();
            double queryTermSetIDF = theQueryTermSet.getInvertedDocumentFrequency();
            HashMap<Integer, ArrayList<Integer>> documentsContainTermSet = theQueryTermSet.getDocumentsContainTermSet();

            for (HashMap.Entry<Integer, ArrayList<Integer>> document : documentsContainTermSet.entrySet()) {
                int documentID = document.getKey();
                int documentTermSetFrequency = document.getValue().size();
                double documentVectorLength = this.mInvertedIndexAdapter.getDocumentVectorLength(documentID);

                if (!retrievedDocuments.containsKey(documentID)) {
                    // Document is newly retrieved, initialize its document ranking to 0.
                    retrievedDocuments.put(documentID, 0.0);
                }
            }
        }
    }

    protected ArrayList<QueryTermSet> getAllQueryTermSets(Query pQuery) {

    }

    protected class QueryTermSet {
        double mWeight;
        double mInvertedDocumentFrequency;
        HashMap<Integer, ArrayList<Integer>> mDocumentsContainTermSet;

        public double getWeight() {
            return mWeight;
        }

        public double getInvertedDocumentFrequency() {
            return mInvertedDocumentFrequency;
        }

        public HashMap<Integer, ArrayList<Integer>> getDocumentsContainTermSet() {
            return mDocumentsContainTermSet;
        }
    }

}
