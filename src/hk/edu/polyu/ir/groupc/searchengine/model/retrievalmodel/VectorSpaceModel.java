package hk.edu.polyu.ir.groupc.searchengine.model.retrievalmodel;

import hk.edu.polyu.ir.groupc.searchengine.Debug;
import hk.edu.polyu.ir.groupc.searchengine.model.query.ExpandedTerm;
import hk.edu.polyu.ir.groupc.searchengine.model.query.InvertedIndexAdapter;
import hk.edu.polyu.ir.groupc.searchengine.model.query.Query;
import hk.edu.polyu.ir.groupc.searchengine.model.query.RetrievalModelWithRanking;
import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.mutable.ArrayBuffer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by nEbuLa on 14/11/2015.
 * <p>
 * Vector space model
 * <p>
 * Description:     This model make implements the vector space model idea taught in COMP433
 * classes. Besides the basics, additional weighting and normalization calculation
 * methods are implemented, which includes pivot document length normalization
 * and BM25.
 * <p>
 * References:      https://en.wikipedia.org/wiki/Vector_space_model
 * https://d396qusza40orc.cloudfront.net/textretrieval/lecture_notes/wk1/1.8%20TR-TF_Transformation.pdf
 * https://d396qusza40orc.cloudfront.net/textretrieval/lecture_notes/wk1/1.9%20TR-Doc_Length_Normalization.pdf
 */
public class VectorSpaceModel extends RetrievalModelWithRanking {

    protected final Parameter<Double> mPivotBParameter;
    protected final Parameter<Double> mBM25KParameter;
    private final List<String> MODES;
    private final List<Parameter> Parameters;
    protected NormalizationType mNormalizationType;

    {
        MODES = new LinkedList<>();
        for (NormalizationType normalizationType : NormalizationType.values()) {
            MODES.add(normalizationType.toString());
        }
        Parameters = new LinkedList<>();
        mPivotBParameter = new Parameter<Double>("Pivot B", 0.01, 10.0, 0.75);
        mBM25KParameter = new Parameter<Double>("BM25K", 0.01, 10.0, 1.5);
        Parameters.add(mPivotBParameter);
        Parameters.add(mBM25KParameter);
    }

    public VectorSpaceModel() {
        // By default, the program uses inner product only to calculate the rank.
        this.mNormalizationType = NormalizationType.NONE;

        // The b parameter is usually chosen (in absence of an advanced optimization) to be 0.75.
        this.mPivotBParameter.value = 0.75;

        // The k parameter is usually chosen (in absence of an advanced optimization) to be 1.5.
        this.mBM25KParameter.value = 1.5;
    }
//    protected double mPivotBParameter;
//    protected double mBM25KParameter;

    @Override
    public List<String> getModes() {
        return MODES;
    }

    @Override
    public String getDefaultMode() {
        return NormalizationType.NONE.toString();
    }

    @Override
    public String getMode() {
        return mNormalizationType.toString();
    }

    @Override
    public void setMode(String s) {
        boolean found = false;
        for (NormalizationType normalizationType : NormalizationType.values()) {
            if (normalizationType.toString().equals(s)) {
                mNormalizationType = normalizationType;
                found = true;
                break;
            }
        }
        if (!found) Debug.loge("failed to set mode on " + getClass().getSimpleName());
    }

    @Override
    public List<Parameter> getParameters() {
        return Parameters;
    }

    @Override
    public HashMap<Integer, Double> getRankedDocumentsWithoutSort(Query pQuery) {
        // retrievedDocuments will have a structure <Document ID, ranking score>
        HashMap<Integer, Double> retrievedDocuments = new HashMap<>();

        // Get the average document vector length for further computation.
        double averageDocumentVectorLength = InvertedIndexAdapter.getInstance().getAverageDocumentVectorLength();

        ExpandedTerm[] expendedQueryTerms = pQuery.expandedTerms();

        // Find all related documents and compute their scores.
        for (ExpandedTerm expendedQueryTerm : expendedQueryTerms) {
            double queryTermWeight = expendedQueryTerm.weight();
            double queryTermIDF = InvertedIndexAdapter.getInstance().getInvertedDocumentFrequency(expendedQueryTerm.term());

            Iterator<Tuple2<Object, ArrayBuffer<Object>>> documentsIterator = expendedQueryTerm.term().filePositionMap().iterator();
            while (documentsIterator.hasNext()) {
                Tuple2<Object, ArrayBuffer<Object>> document = documentsIterator.next();
                int documentID = (int) document._1();
                int documentTermFrequency = document._2().length();
                double documentVectorLength = InvertedIndexAdapter.getInstance().getDocumentVectorLength(documentID);

                if (!retrievedDocuments.containsKey(documentID)) {
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
                        this.mPivotBParameter.value,
                        this.mBM25KParameter.value,
                        this.mNormalizationType
                );
            }  // End document while
        }  // End query term foreach

        return retrievedDocuments;
    }  // End getRankedDocumentsWithoutSort()

    /*
     *
     *   Scoring helper functions
     *
     */
    protected void accumulateDocumentScore(HashMap<Integer, Double> pRetrievalDocuments, int pDocumentID,
                                           double pQueryTermWeight, double pQueryTermIDF, int pDocumentTermFrequency,
                                           double pDocumentVectorLength, double pAverageDocumentVectorLength,
                                           double pPivotBParameter, double pBM25KParameter,
                                           NormalizationType pNormalizationType) {
        double retrievedDocumentScore = pRetrievalDocuments.get(pDocumentID);

        switch (pNormalizationType) {
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
     *
     *   Term weighting, normalization and document scoring functions
     *
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
                                1.0 + Math.log(1.0 + pDocumentTermFrequency)
                        )
                                /
                                (
                                        1.0 - pPivotBParameter + pPivotBParameter *
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

    public void setBm25KParameter(double pValue) {
        this.mBM25KParameter.value = pValue;
    }

    /*
     *
     *   Getter methods
     *
     */
    public NormalizationType getNormalizationType() {
        return this.mNormalizationType;
    }

    /*
     *
     *   Setter methods
     *
     */
    public void setNormalizationType(NormalizationType pType) {
        this.mNormalizationType = pType;
    }

    public double getPivotBParameter() {
        return this.mPivotBParameter.value;
    }

    public void setPivotBParameter(double pValue) {
        this.mPivotBParameter.value = pValue;
    }

    public double getBM25KParameter() {
        return this.mBM25KParameter.value;
    }

    public enum NormalizationType {
        NONE {
            @Override
            public String toString() {
                return "No Normalization";
            }
        }, COSINE, PIVOT, BM25
    }

}