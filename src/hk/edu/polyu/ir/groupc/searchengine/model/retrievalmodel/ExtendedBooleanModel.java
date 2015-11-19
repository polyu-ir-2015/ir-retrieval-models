package hk.edu.polyu.ir.groupc.searchengine.model.retrievalmodel;

import hk.edu.polyu.ir.groupc.searchengine.model.query.ExpandedTerm;
import hk.edu.polyu.ir.groupc.searchengine.model.query.InvertedIndexAdapter;
import hk.edu.polyu.ir.groupc.searchengine.model.query.Query;
import hk.edu.polyu.ir.groupc.searchengine.model.query.RetrievalModelWithRanking;

import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.mutable.ArrayBuffer;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * Created by nEbuLa on 14/11/2015.
 *
 * Extended Boolean Model
 *
 * Description:     This model make uses of the advantages of vector space model (which documents can be ranked)
 *                  and the boolean model (which allows boolean operations). The combined implementation
 *                  of these two models results in the extended boolean model.
 *
 * References:      https://en.wikipedia.org/wiki/Extended_Boolean_model
 *
 */
public class ExtendedBooleanModel extends RetrievalModelWithRanking {

    public enum OperationType {
        AND, OR
    }

    protected OperationType mOperationType;
    protected double mModelPNormParameter;

    public ExtendedBooleanModel() {
        // By default, we treat spaces between query terms as AND boolean operations.
        this.mOperationType = OperationType.AND;

        // Setting the parameter p to 2 is said to be good.
        this.mModelPNormParameter = 2.0;
    }

    @Override
    public HashMap<Integer, Double> getRankedDocumentsWithoutSort(Query pQuery) {
        // termWeightsPerDocument will have a structure <Document ID, List of term weights in that document>
        HashMap<Integer, ArrayList<Double>> termWeightsPerDocument = new HashMap<>();

        // Get the average document vector length for further computation.
        double maximumIDFInCollection = InvertedIndexAdapter.getInstance().getMaximumInvertedDocumentFrequency();

        ExpandedTerm[] expendedQueryTerms = pQuery.expandedTerms();

        // STEP 1:
        // Compute the normalized term weight per document.
        for (ExpandedTerm expendedQueryTerm : expendedQueryTerms) {
            double queryTermIDF = InvertedIndexAdapter.getInstance().getInvertedDocumentFrequency(expendedQueryTerm.term());

            Iterator<Tuple2<Object, ArrayBuffer<Object>>> documentsIterator = expendedQueryTerm.term().filePositionMap().iterator();
            while(documentsIterator.hasNext()) {
                Tuple2<Object, ArrayBuffer<Object>> document = documentsIterator.next();
                int documentID = (int) document._1;
                int documentTermFrequency = document._2.length();
                int maximumTFInDocument = InvertedIndexAdapter.getInstance().getMaximumTermFrequencyInDocument(documentID);

                if( ! termWeightsPerDocument.containsKey(documentID)) {
                    termWeightsPerDocument.put(documentID, new ArrayList<>());
                }

                // Save all the weights inside each document for further computation.
                termWeightsPerDocument.get(documentID).add(
                        this.getNormalizedTermWeight(
                                documentTermFrequency, maximumTFInDocument,
                                queryTermIDF, maximumIDFInCollection)
                );

            }  // End document foreach
        }  // End query term foreach

        // retrievedDocuments will have a structure <Document ID, ranking score>
        HashMap<Integer, Double> retrievedDocuments = new HashMap<>();

        // STEP 2:
        // For each document, compute the similarity scores by using the extended boolean model's formula.
        int numberOfQueryTerms = expendedQueryTerms.length;
        double rankingScore;

        for (HashMap.Entry<Integer, ArrayList<Double>> document : termWeightsPerDocument.entrySet()) {
            int documentID = document.getKey();
            ArrayList<Double> allWeights = document.getValue();

            rankingScore = this.getDocumentRankingScore(
                    this.mOperationType,
                    this.mModelPNormParameter,
                    allWeights,
                    numberOfQueryTerms);
            retrievedDocuments.put(documentID, rankingScore);
        }

        return retrievedDocuments;
    }  // End getRankedDocumentsWithoutSort()


    /*
     *
     *   Term weighting, normalization and document scoring functions
     *
     */
    protected double getNormalizedTermWeight(int pDocumentTermFrequency, int pMaximumTFInDocument,
                                           double pQueryTermIDF, double pMaximumIDFInCollection) {
        // The term weight is normalized to 0 to 1.
        return (pDocumentTermFrequency / pMaximumTFInDocument) * (pQueryTermIDF / pMaximumIDFInCollection);
    }

    protected double getDocumentRankingScore(OperationType pOperationType, double pModelPNormParameter,
                                           ArrayList<Double> pAllWeights, int pNumberOfQueryTerms) {
        double documentRankingScore = 0.0;

        for(double weight: pAllWeights) {
            switch(pOperationType) {
                case AND:
                    documentRankingScore += Math.pow(1.0 - weight, pModelPNormParameter);
                    break;
                case OR:
                    documentRankingScore += Math.pow(weight, pModelPNormParameter);
                    break;
            }
        }

        documentRankingScore /= pNumberOfQueryTerms;
        documentRankingScore = Math.pow(documentRankingScore, 1.0 / pModelPNormParameter);
        if(pOperationType == OperationType.AND) {
            documentRankingScore = 1.0 - documentRankingScore;
        }

        return documentRankingScore;
    }


    /*
     *
     *   Setter methods
     *
     */
    public void setOperationType(OperationType pOperatorType) {
        this.mOperationType = pOperatorType;
    }

    public void setModelPNormParameter(double pModelPNormParameter) {
        this.mModelPNormParameter = pModelPNormParameter;
    }


    /*
     *
     *   Getter methods
     *
     */
    public OperationType getOperationType() {
        return this.mOperationType;
    }

    public double getModelPNormParameter() {
        return this.mModelPNormParameter;
    }

}

