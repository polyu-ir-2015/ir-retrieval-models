package hk.edu.polyu.ir.groupc.searchengine.model.query;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by nEbuLa on 14/11/2015.
 *
 * Description:     This class is used to abstract the implementation of inverted index done
 *                  by Benno. Retrieval models should construct an adapter of this and use
 *                  it the get data and useful information from the inverted index data
 *                  structure.
 */
public class IndexAdapter {

    public Double getAverageDocumentVectorLength() {
        throw new NotImplementedException();
    }

    public Double getDocumentVectorLength(Integer pDocumentID) {
        throw new NotImplementedException();
    }

    public Double getInvertedDocumentFrequency(String pTerm) {
        throw new NotImplementedException();
    }

    public HashMap<Integer, ArrayList<Integer>> getDocumentsContainTerm(String pTerm) {
        throw new NotImplementedException();
    }

    public Integer getMaximumTermFrequencyInDocument(Integer pDocumentID) {
        throw new NotImplementedException();
    }

    public Double getMaximumInvertedDocumentFrequency() {
        throw new NotImplementedException();
    }

}
