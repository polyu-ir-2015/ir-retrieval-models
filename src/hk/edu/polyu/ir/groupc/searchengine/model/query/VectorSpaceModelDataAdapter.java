package hk.edu.polyu.ir.groupc.searchengine.model.query;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.HashMap;

public class VectorSpaceModelDataAdapter {

    public Double getAverageDocumentVectorLength() {
        throw new NotImplementedException();
    }

    public Double getInvertedDocumentFrequency(String pTerm) {
        throw new NotImplementedException();
    }

    public HashMap<Integer, ArrayList<Integer>> getDocumentsContainTerm(String pTerm) {
        throw new NotImplementedException();
    }

    public Double getDocumentVectorLength(Integer pDocumentID) {
        throw new NotImplementedException();
    }

}
