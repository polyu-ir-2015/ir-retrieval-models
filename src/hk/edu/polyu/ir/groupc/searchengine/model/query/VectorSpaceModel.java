package hk.edu.polyu.ir.groupc.searchengine.model.query;

import hk.edu.polyu.ir.groupc.searchengine.model.datasource.SearchResult;
import hk.edu.polyu.ir.groupc.searchengine.model.datasource.SearchResultFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class VectorSpaceModel extends RetrievalModel {

    private VectorSpaceModelNormalizationTypes normalizationTypes;
    private VectorSpaceModelDataAdapter irDataAdapter;
    private Double pivotBParameter;
    private Double bm25KParameter;

    public VectorSpaceModel() {
        // This adapter is used to link with the module developed by Benno.
        this.irDataAdapter = new VectorSpaceModelDataAdapter();

        this.normalizationTypes = VectorSpaceModelNormalizationTypes.NONE;
        this.pivotBParameter = 0.75;
        this.bm25KParameter = 1.5;
    }

    @Override
    public SearchResult search(Query pQuery) {
        // expandedQueryMap will have a structure <Document ID, ranking score>
        HashMap<Integer, Double> retrievedDocuments = new HashMap<>();

        // expandedQueryMap will have a structure <Query term string, query term weighting>
        HashMap<String, Double> expandedQueryMap = pQuery.getExpandedQuery();

        // Get the average document vector length for future computation.
        Double averageDocumentVectorLength = this.irDataAdapter.getAverageDocumentVectorLength();

        // Find all related documents and compute their score.
        for (HashMap.Entry<String, Double> queryItem : expandedQueryMap.entrySet()) {
            String queryTermString = queryItem.getKey();
            Double queryTermWeight = queryItem.getValue();
            Double queryTermIDF = this.irDataAdapter.getInvertedDocumentFrequency(queryTermString);
            HashMap<Integer, ArrayList<Integer>> documentsContainTerm = this.irDataAdapter.getDocumentsContainTerm(queryTermString);

            for (HashMap.Entry<Integer, ArrayList<Integer>> document : documentsContainTerm.entrySet()) {
                Integer documentID = document.getKey();
                Integer documentTermFrequency = document.getValue().size();
                Double documentVectorLength = this.irDataAdapter.getDocumentVectorLength(documentID);

                if( ! retrievedDocuments.containsKey(documentID)) {
                    // Document is newly retrieved, initialize its document ranking to 0.
                    retrievedDocuments.put(documentID, 0.0);
                }

                // New term is found, related documents should have additional scores in ranking.
                Double retrievedDocumentScore = retrievedDocuments.get(documentID);
                switch(this.normalizationTypes) {
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
                        retrievedDocumentScore += this.getRankingByBM25(queryTermWeight,
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

        ArrayList<RetrievalDocument> sortedRetrievedDocumentList = new ArrayList<>();

        // Convert the HaspMap into array list.
        for (HashMap.Entry<Integer, Double> retrievedSingleDoc : retrievedDocuments.entrySet()) {
            Integer documentID = retrievedSingleDoc.getKey();
            Double documentScore = retrievedSingleDoc.getValue();

            sortedRetrievedDocumentList.add(
                    new RetrievalDocument(documentID, documentScore)
            );
        }

        // Sort the array list by the retrieved documents' ranking.
        Collections.sort(sortedRetrievedDocumentList, new Comparator<RetrievalDocument>() {
            @Override
            public int compare(RetrievalDocument pDocument1, RetrievalDocument pDocument2) {
                // Sort by descending order using ranking score.
                return Double.compare(pDocument2.similarityScore, pDocument1.similarityScore);
            }
        });

        return SearchResultFactory.create(sortedRetrievedDocumentList);
    }


    /*
     *
     *   Scoring and normalization functions
     *
     */
    private Double getRankingWithoutNormalization(Double pQueryTermWeight, Double pQueryTermIDF,
                                                  Integer pDocumentTermFrequency) {
        return pQueryTermWeight * pDocumentTermFrequency * pQueryTermIDF;
    }

    private Double getRankingByCosineSimilarity(Double pQueryTermWeight, Double pQueryTermIDF,
                                                Integer pDocumentTermFrequency, Double pDocumentVectorLength) {
        return (pQueryTermWeight * pDocumentTermFrequency * pQueryTermIDF) / pDocumentVectorLength;
    }

    // Equation see https://d396qusza40orc.cloudfront.net/textretrieval/lecture_notes/wk1/1.9%20TR-Doc_Length_Normalization.pdf
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

    // Equation see https://d396qusza40orc.cloudfront.net/textretrieval/lecture_notes/wk1/1.9%20TR-Doc_Length_Normalization.pdf
    private Double getRankingByBM25(Double pQueryTermWeight, Double pQueryTermIDF, Integer pDocumentTermFrequency,
                                    Double pDocumentVectorLength, Double pAverageDocumentVectorLength,
                                    Double pPivotBParameter, Double pBM25KParameter) {
        return pQueryTermWeight *
                    (
                            (
                                    (pBM25KParameter + 1) * pDocumentTermFrequency
                            )
                                    /
                            (
                                    pDocumentTermFrequency +
                                            pBM25KParameter * (
                                                1 - pPivotBParameter + pPivotBParameter *
                                                        (pDocumentVectorLength / pAverageDocumentVectorLength)
                                            )
                            )
                    ) * pQueryTermIDF;
    }


    /*
     *
     *   Setter methods
     *
     */
    public void setNormalizationTypes(VectorSpaceModelNormalizationTypes pType) {
        this.normalizationTypes = pType;
    }

    public void setPivotBParameter(Double pValue) {
        this.pivotBParameter = pValue;
    }

    public void setBm25KParameter(Double pValue) {
        this.bm25KParameter = pValue;
    }


    /*
     *
     *   Getter methods
     *
     */
    public VectorSpaceModelNormalizationTypes getNormalizationTypes() {
        return this.normalizationTypes;
    }

    public Double getPivotBParameter() {
        return this.pivotBParameter;
    }

    public Double getBm25KParameter() {
        return this.bm25KParameter;
    }
}