package hk.edu.polyu.ir.groupc.searchengine.model.query;

import hk.edu.polyu.ir.groupc.searchengine.model.datasource.SearchResult;
import hk.edu.polyu.ir.groupc.searchengine.model.datasource.SearchResultFactory;

import java.util.ArrayList;
import java.util.HashMap;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Created by nEbuLa on 14/11/2015.
 *
 * Vector space model
 *
 * Description:     This model make implements the vector space model idea taught in COMP433
 *                  classes. Besides the basics, additional weighting and normalization calculation
 *                  methods are implemented, which includes pivot document length normalization
 *                  and BM25.
 *
 * References:      https://en.wikipedia.org/wiki/Vector_space_model
 *                  https://d396qusza40orc.cloudfront.net/textretrieval/lecture_notes/wk1/1.8%20TR-TF_Transformation.pdf
 *                  https://d396qusza40orc.cloudfront.net/textretrieval/lecture_notes/wk1/1.9%20TR-Doc_Length_Normalization.pdf
 */
public class VectorSpaceModel extends RetrievalModelWithRanking {

    private VectorSpaceModelNormalizationType normalizationType;
    private IndexAdapter indexAdapter;
    private Double pivotBParameter;
    private Double bm25KParameter;

    public VectorSpaceModel() {
        // This adapter is used to link with the module developed by Benno.
        this.indexAdapter = new IndexAdapter();

        this.normalizationType = VectorSpaceModelNormalizationType.NONE;
        this.pivotBParameter = 0.75;
        this.bm25KParameter = 1.5;
    }

    @Override
    public SearchResult search(Query pQuery) {
        // retrievedDocuments will have a structure <Document ID, ranking score>
        HashMap<Integer, Double> retrievedDocuments = new HashMap<>();

        // Get the average document vector length for further computation.
        Double averageDocumentVectorLength = this.indexAdapter.getAverageDocumentVectorLength();

        HashMap<String, Double> expendedQueryTerms = pQuery.getExpandedQueryTermsWithWeight();

        // STEP 1:
        // Find all related documents and compute their scores.
        for (HashMap.Entry<String, Double> queryItem : expendedQueryTerms.entrySet()) {
            String queryTermString = queryItem.getKey();
            Double queryTermWeight = queryItem.getValue();
            Double queryTermIDF = this.indexAdapter.getInvertedDocumentFrequency(queryTermString);
            HashMap<Integer, ArrayList<Integer>> documentsContainTerm = this.indexAdapter.getDocumentsContainTerm(queryTermString);

            for (HashMap.Entry<Integer, ArrayList<Integer>> document : documentsContainTerm.entrySet()) {
                Integer documentID = document.getKey();
                Integer documentTermFrequency = document.getValue().size();
                Double documentVectorLength = this.indexAdapter.getDocumentVectorLength(documentID);

                if( ! retrievedDocuments.containsKey(documentID)) {
                    // Document is newly retrieved, initialize its document ranking to 0.
                    retrievedDocuments.put(documentID, 0.0);
                }

                // New term is found, related documents should have additional scores in ranking.
                Double retrievedDocumentScore = retrievedDocuments.get(documentID);
                switch(this.normalizationType) {
                    case NONE:
                        retrievedDocumentScore += this.getRankingWithoutNormalization(
                                queryTermWeight,
                                queryTermIDF,
                                documentTermFrequency);
                        break;
                    case COSINE:
                        retrievedDocumentScore += this.getRankingByCosineSimilarity(
                                queryTermWeight,
                                queryTermIDF,
                                documentTermFrequency,
                                documentVectorLength);
                        break;
                    case PIVOT:
                        retrievedDocumentScore += this.getRankingByPivotNormalization(
                                queryTermWeight,
                                queryTermIDF,
                                documentTermFrequency,
                                documentVectorLength,
                                averageDocumentVectorLength,
                                this.pivotBParameter);
                        break;
                    case BM25:
                        retrievedDocumentScore += this.getRankingByBM25(
                                queryTermWeight,
                                queryTermIDF,
                                documentTermFrequency,
                                documentVectorLength,
                                averageDocumentVectorLength,
                                this.pivotBParameter,
                                this.bm25KParameter);
                        break;
                }

                // Update the ranking score saved.
                retrievedDocuments.put(documentID, retrievedDocumentScore);

            }  // End document foreach
        }  // End query term foreach

        // STEP 2:
        // Convert the hash map into array list and sort it by rank in descending order.
        ArrayList<RetrievalDocument> sortedRetrievedDocumentList = this.convertToRetrievalDocumentArrayList(retrievedDocuments);
        this.sortRetrievalDocumentArrayListByDescRanking(sortedRetrievedDocumentList);

        return SearchResultFactory.create(sortedRetrievedDocumentList);
    }


    /*
     *   Term weighting, normalization and document scoring functions
     */
    private Double getRankingWithoutNormalization(Double pQueryTermWeight, Double pQueryTermIDF,
                                                  Integer pDocumentTermFrequency) {
        return pQueryTermWeight * pDocumentTermFrequency * pQueryTermIDF;
    }

    private Double getRankingByCosineSimilarity(Double pQueryTermWeight, Double pQueryTermIDF,
                                                Integer pDocumentTermFrequency, Double pDocumentVectorLength) {
        return (pQueryTermWeight * pDocumentTermFrequency * pQueryTermIDF) / pDocumentVectorLength;
    }

    private Double getRankingByPivotNormalization(Double pQueryTermWeight, Double pQueryTermIDF,
                                                  Integer pDocumentTermFrequency, Double pDocumentVectorLength,
                                                  Double pAverageDocumentVectorLength, Double pPivotBParameter) {
        return pQueryTermWeight *
                    (
                            Math.log(
                                    1 + Math.log(1 + pDocumentTermFrequency)
                            )
                                    /
                            (
                                    1 - pPivotBParameter + pPivotBParameter *
                                            (pDocumentVectorLength / pAverageDocumentVectorLength)
                            )
                    ) * pQueryTermIDF;
    }

    private Double getRankingByBM25(Double pQueryTermWeight, Double pQueryTermIDF, Integer pDocumentTermFrequency,
                                    Double pDocumentVectorLength, Double pAverageDocumentVectorLength,
                                    Double pPivotBParameter, Double pBM25KParameter) {
        return pQueryTermWeight *
                    (
                            (
                                    (pBM25KParameter + 1.0) * pDocumentTermFrequency
                            )
                                    /
                            (
                                    pDocumentTermFrequency +
                                            pBM25KParameter * (
                                                1.0 - pPivotBParameter + pPivotBParameter *
                                                        (pDocumentVectorLength / pAverageDocumentVectorLength)
                                            )
                            )
                    ) * pQueryTermIDF;
    }


    /*
     *   Setter methods
     */
    public void setNormalizationType(VectorSpaceModelNormalizationType pType) {
        this.normalizationType = pType;
    }

    public void setPivotBParameter(Double pValue) {
        this.pivotBParameter = pValue;
    }

    public void setBm25KParameter(Double pValue) {
        this.bm25KParameter = pValue;
    }


    /*
     *   Getter methods
     */
    public VectorSpaceModelNormalizationType getNormalizationType() {
        return this.normalizationType;
    }

    public Double getPivotBParameter() {
        return this.pivotBParameter;
    }

    public Double getBM25KParameter() {
        return this.bm25KParameter;
    }

}