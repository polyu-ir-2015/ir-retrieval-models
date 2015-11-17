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
public class InvertedIndexAdapter {

    public double getAverageDocumentVectorLength() {
        throw new NotImplementedException();
    }

    public double getDocumentVectorLength(int pDocumentID) {
        throw new NotImplementedException();
    }

    public double getInvertedDocumentFrequency(String pTerm) {
        throw new NotImplementedException();
    }

    public HashMap<Integer, ArrayList<Integer>> getDocumentsContainTerm(String pTerm) {
        throw new NotImplementedException();
    }

    public int getMaximumTermFrequencyInDocument(int pDocumentID) {
        throw new NotImplementedException();
    }

    public double getMaximumInvertedDocumentFrequency() {
        throw new NotImplementedException();
    }

}
