package hk.edu.polyu.ir.groupc.searchengine.model.query;

import hk.edu.polyu.ir.groupc.searchengine.model.Index;
import hk.edu.polyu.ir.groupc.searchengine.model.datasource.TermEntity;

/**
 *
 * Created by nEbuLa on 14/11/2015.
 *
 * Description:     This class is used to abstract the implementation of inverted index done
 *                  by Benno. Retrieval models should construct an adapter of this and use
 *                  it the get data and useful information from the inverted index data
 *                  structure.
 *
 */
public class InvertedIndexAdapter {

    private static InvertedIndexAdapter instance;

    private InvertedIndexAdapter() {
    }

    public static InvertedIndexAdapter getInstance() {
        if (InvertedIndexAdapter.instance == null) {
            InvertedIndexAdapter.instance = new InvertedIndexAdapter();
        }
        return InvertedIndexAdapter.instance;
    }

    public double getAverageDocumentVectorLength() {
        return Index.averageDocumentLength();
    }

    public double getMedianDocumentVectorLength() {
        return Index.medianDocumentLength();
    }

    public double getDocumentVectorLength(int pDocumentID) {
        return Index.getDocumentLength(pDocumentID);
    }

    public double getInvertedDocumentFrequency(TermEntity pTermEntity) {
        return Index.getIDF(pTermEntity);
    }

    public int getMaximumTermFrequencyInDocument(int pDocumentID) {
        return Index.maxTermFrequency(pDocumentID);
    }

    public double getMaximumInvertedDocumentFrequency() {
        return Index.maxIDF();
    }

    public int getNumberOfDocument() {
        return Index.getDocumentCount();
    }

}
