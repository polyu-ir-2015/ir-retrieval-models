package hk.edu.polyu.ir.groupc.searchengine.model.query;

import hk.edu.polyu.ir.groupc.searchengine.model.datasource.SearchResult;
import hk.edu.polyu.ir.groupc.searchengine.model.datasource.SearchResultFactory;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by nEbuLa on 14/11/2015.
 *
 * Extended Boolean Model
 *
 * Description:     This model make uses of the advantages of vector space model (which documents can be ranked)
 *                  and the boolean model (which allows boolean operations). The combined implementation
 *                  of these two models results in the extended boolean model.
 *
 * References:      https://en.wikipedia.org/wiki/Extended_Boolean_model
 */
public class ExtendedBooleanModel extends RetrievalModelWithRanking {

    private ExtendedBooleanModelOperationType operationType;
    private IndexAdapter indexAdapter;
    private Double modelPNormParameter;

    public ExtendedBooleanModel() {
        this.indexAdapter = new IndexAdapter();

        // By default, we treat spaces between query terms as OR boolean operations.
        this.operationType = ExtendedBooleanModelOperationType.OR;
        this.modelPNormParameter = 2.0;
    }

    @Override
    public SearchResult search(Query pQuery) {
        // termWeightsPerDocument will have a structure <Document ID, List of term weights in that document>
        HashMap<Integer, ArrayList<Double>> termWeightsPerDocument = new HashMap<>();

        // Get the average document vector length for further computation.
        Double maximumIDFInCollection = this.indexAdapter.getMaximumInvertedDocumentFrequency();

        ArrayList<String> rawQueryTerms = pQuery.getRawQueryTerms();
        Integer numberOfQueryTerms = rawQueryTerms.size();

        // STEP 1:
        // Compute the normalized term weight per document.
        for (String queryTermString: rawQueryTerms) {
            Double queryTermIDF = this.indexAdapter.getInvertedDocumentFrequency(queryTermString);
            HashMap<Integer, ArrayList<Integer>> documentsContainTerm = this.indexAdapter.getDocumentsContainTerm(queryTermString);

            for (HashMap.Entry<Integer, ArrayList<Integer>> document : documentsContainTerm.entrySet()) {
                Integer documentID = document.getKey();
                Integer documentTermFrequency = document.getValue().size();
                Integer maximumTFInDocument = this.indexAdapter.getMaximumTermFrequencyInDocument(documentID);

                if( ! termWeightsPerDocument.containsKey(documentID)) {
                    termWeightsPerDocument.put(documentID, new ArrayList<>());
                }

                // Save all the weights inside each document for further computation.
                termWeightsPerDocument.get(documentID).add(
                        this.getSquaredAndNormalizedTermWeight(
                                documentTermFrequency, maximumTFInDocument,
                                queryTermIDF, maximumIDFInCollection)
                );

            }  // End document foreach
        }  // End query term foreach

        // retrievedDocuments will have a structure <Document ID, ranking score>
        HashMap<Integer, Double> retrievedDocuments = new HashMap<>();

        // STEP 2:
        // For each document, compute the similarity scores by using the extended boolean model's formula.
        for (HashMap.Entry<Integer, ArrayList<Double>> document : termWeightsPerDocument.entrySet()) {
            Integer documentID = document.getKey();
            ArrayList<Double> allWeights = document.getValue();

            Double rankingScore = this.getDocumentRankingScore(
                    this.operationType,
                    this.modelPNormParameter,
                    allWeights, numberOfQueryTerms);
            retrievedDocuments.put(documentID, rankingScore);
        }

        // STEP 3:
        // Convert the hash map into array list and sort it by rank in descending order.
        ArrayList<RetrievalDocument> sortedRetrievedDocumentList;
        sortedRetrievedDocumentList = this.convertToRetrievalDocumentArrayList(retrievedDocuments);
        this.sortRetrievalDocumentArrayListByDescRanking(sortedRetrievedDocumentList);

        return SearchResultFactory.create(sortedRetrievedDocumentList);
    }


    /*
     *   Term weighting, normalization and document scoring functions
     */
    private Double getSquaredAndNormalizedTermWeight(Integer pDocumentTermFrequency, Integer pMaximumTFInDocument,
                                                     Double pQueryTermIDF, Double pMaximumIDFInCollection) {
        // The term weight is normalized to 0 to 1.
        return (pDocumentTermFrequency / pMaximumTFInDocument) * (pQueryTermIDF / pMaximumIDFInCollection);
    }

    private Double getDocumentRankingScore(ExtendedBooleanModelOperationType pOperationType, Double pModelPNormParameter,
                                           ArrayList<Double> pAllWeights, Integer pNumberOfQueryTerms) {
        Double documentRankingScore = 0.0;

        for(Double weight: pAllWeights) {
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
        if(pOperationType == ExtendedBooleanModelOperationType.AND) {
            documentRankingScore = 1.0 - documentRankingScore;
        }

        return documentRankingScore;
    }


    /*
     *   Setter methods
     */
    public void setOperationType(ExtendedBooleanModelOperationType pOperatorType) {
        this.operationType = pOperatorType;
    }

    public void setModelPNormParameter(Double pModelPNormParameter) {
        this.modelPNormParameter = pModelPNormParameter;
    }


    /*
     *   Getter methods
     */
    public ExtendedBooleanModelOperationType getOperationType() {
        return this.operationType;
    }

    public Double getModelPNormParameter() {
        return this.modelPNormParameter;
    }

}

