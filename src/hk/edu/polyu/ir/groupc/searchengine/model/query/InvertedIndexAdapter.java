package hk.edu.polyu.ir.groupc.searchengine.model.query;

import hk.edu.polyu.ir.groupc.searchengine.model.datasource.TermEntity;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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

    private InvertedIndexAdapter() {}

    public static InvertedIndexAdapter getInstance() {
        if(InvertedIndexAdapter.instance == null) {
            InvertedIndexAdapter.instance = new InvertedIndexAdapter();
        }
        return InvertedIndexAdapter.instance;
    }

    public double getAverageDocumentVectorLength() {
        throw new NotImplementedException();
    }

    public double getDocumentVectorLength(int pDocumentID) {
        throw new NotImplementedException();
    }

    public double getInvertedDocumentFrequency(TermEntity pTermEntity) {
        throw new NotImplementedException();
    }

    public int getMaximumTermFrequencyInDocument(int pDocumentID) {
        throw new NotImplementedException();
    }

    public double getMaximumInvertedDocumentFrequency() {
        throw new NotImplementedException();
    }

    public int getNumberOfDocument() {
        throw new NotImplementedException();
    }

}
