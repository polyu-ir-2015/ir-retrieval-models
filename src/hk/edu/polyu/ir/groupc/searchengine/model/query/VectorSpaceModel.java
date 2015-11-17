package hk.edu.polyu.ir.groupc.searchengine.model.query;

import java.util.ArrayList;
import java.util.HashMap;

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

    public enum NormalizationType {
        NONE, COSINE, PIVOT, BM25
    }

    protected NormalizationType mNormalizationType;
    protected InvertedIndexAdapter mInvertedIndexAdapter;
    protected double mPivotBParameter;
    protected double mBM25KParameter;


    public VectorSpaceModel() {
        // This adapter is used to link with the module developed by Benno.
        this.mInvertedIndexAdapter = new InvertedIndexAdapter();

        // By default, the program uses inner product only to calculate the rank.
        this.mNormalizationType = NormalizationType.NONE;

        // The b parameter is usually chosen (in absence of an advanced optimization) to be 0.75.
        this.mPivotBParameter = 0.75;

        // The k parameter is usually chosen (in absence of an advanced optimization) to be 1.5.
        this.mBM25KParameter = 1.5;
    }

    @Override
    public HashMap<Integer, Double> getRankedDocumentsWithoutSort(Query pQuery) {
        // retrievedDocuments will have a structure <Document ID, ranking score>
        HashMap<Integer, Double> retrievedDocuments = new HashMap<>();

        // Get the average document vector length for further computation.
        double averageDocumentVectorLength = this.mInvertedIndexAdapter.getAverageDocumentVectorLength();

        // expendedQueryTerms will have a structure <Query term string, query term weight>
        HashMap<String, Double> expendedQueryTerms = pQuery.getExpandedQueryTermsWithWeight();

        // Find all related documents and compute their scores.
        for (HashMap.Entry<String, Double> queryItem : expendedQueryTerms.entrySet()) {
            String queryTermString = queryItem.getKey();
            double queryTermWeight = queryItem.getValue();
            double queryTermIDF = this.mInvertedIndexAdapter.getInvertedDocumentFrequency(queryTermString);
            HashMap<Integer, ArrayList<Integer>> documentsContainTerm = this.mInvertedIndexAdapter
                    .getDocumentsContainTerm(queryTermString);

            for (HashMap.Entry<Integer, ArrayList<Integer>> document : documentsContainTerm.entrySet()) {
                int documentID = document.getKey();
                int documentTermFrequency = document.getValue().size();
                double documentVectorLength = this.mInvertedIndexAdapter.getDocumentVectorLength(documentID);

                if( ! retrievedDocuments.containsKey(documentID)) {
                    // Document is newly retrieved, initialize its document ranking to 0.
                    retrievedDocuments.put(documentID, 0.0);
                }

                // New term is found, related documents should have additional scores in ranking.
                this.accumulateDocumentScore(
                        retrievedDocuments,
                        documentID,
                        queryTermWeight,
                        queryTermIDF,
                        documentTermFrequency,
                        documentVectorLength,
                        averageDocumentVectorLength,
                        this.mPivotBParameter,
                        this.mBM25KParameter
                );
            }  // End document foreach
        }  // End query term foreach

        return retrievedDocuments;
    }  // End getRankedDocumentsWithoutSort()


    /*
     *   Scoring helper functions
     */
    protected void accumulateDocumentScore(HashMap<Integer, Double> pRetrievalDocuments, int pDocumentID,
                                           double pQueryTermWeight, double pQueryTermIDF, int pDocumentTermFrequency,
                                           double pDocumentVectorLength, double pAverageDocumentVectorLength,
                                           double pPivotBParameter, double pBM25KParameter) {
        double retrievedDocumentScore = pRetrievalDocuments.get(pDocumentID);

        switch(this.mNormalizationType) {
            case NONE:
                retrievedDocumentScore += this.getRankingWithoutNormalization(
                        pQueryTermWeight,
                        pQueryTermIDF,
                        pDocumentTermFrequency);
                break;
            case COSINE:
                retrievedDocumentScore += this.getRankingByCosineSimilarity(
                        pQueryTermWeight,
                        pQueryTermIDF,
                        pDocumentTermFrequency,
                        pDocumentVectorLength);
                break;
            case PIVOT:
                retrievedDocumentScore += this.getRankingByPivotNormalization(
                        pQueryTermWeight,
                        pQueryTermIDF,
                        pDocumentTermFrequency,
                        pDocumentVectorLength,
                        pAverageDocumentVectorLength,
                        pPivotBParameter);
                break;
            case BM25:
                retrievedDocumentScore += this.getRankingByBM25(
                        pQueryTermWeight,
                        pQueryTermIDF,
                        pDocumentTermFrequency,
                        pDocumentVectorLength,
                        pAverageDocumentVectorLength,
                        pPivotBParameter,
                        pBM25KParameter);
                break;
        }

        // Update the ranking score saved.
        pRetrievalDocuments.put(pDocumentID, retrievedDocumentScore);
    }


    /*
     *   Term weighting, normalization and document scoring functions
     */
    protected double getRankingWithoutNormalization(double pQueryTermWeight, double pQueryTermIDF,
                                                  int pDocumentTermFrequency) {
        return pQueryTermWeight * pDocumentTermFrequency * pQueryTermIDF;
    }

    protected double getRankingByCosineSimilarity(double pQueryTermWeight, double pQueryTermIDF,
                                                int pDocumentTermFrequency, double pDocumentVectorLength) {
        return (pQueryTermWeight * pDocumentTermFrequency * pQueryTermIDF) / pDocumentVectorLength;
    }

    protected double getRankingByPivotNormalization(double pQueryTermWeight, double pQueryTermIDF,
                                                  int pDocumentTermFrequency, double pDocumentVectorLength,
                                                  double pAverageDocumentVectorLength, double pPivotBParameter) {
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

    protected double getRankingByBM25(double pQueryTermWeight, double pQueryTermIDF, int pDocumentTermFrequency,
                                    double pDocumentVectorLength, double pAverageDocumentVectorLength,
                                    double pPivotBParameter, double pBM25KParameter) {
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
    public void setNormalizationType(NormalizationType pType) {
        this.mNormalizationType = pType;
    }

    public void setPivotBParameter(double pValue) {
        this.mPivotBParameter = pValue;
    }

    public void setBm25KParameter(double pValue) {
        this.mBM25KParameter = pValue;
    }


    /*
     *   Getter methods
     */
    public NormalizationType getNormalizationType() {
        return this.mNormalizationType;
    }

    public double getPivotBParameter() {
        return this.mPivotBParameter;
    }

    public double getBM25KParameter() {
        return this.mBM25KParameter;
    }

}