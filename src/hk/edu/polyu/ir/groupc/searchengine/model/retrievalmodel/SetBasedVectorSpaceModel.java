package hk.edu.polyu.ir.groupc.searchengine.model.retrievalmodel;

import comm.lang.ScalaSupport;
import hk.edu.polyu.ir.groupc.searchengine.Debug;
import hk.edu.polyu.ir.groupc.searchengine.model.query.ExpandedTerm;
import hk.edu.polyu.ir.groupc.searchengine.model.query.InvertedIndexAdapter;
import hk.edu.polyu.ir.groupc.searchengine.model.query.Query;
import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.mutable.ArrayBuffer;

import java.util.*;
import java.util.function.Consumer;


/**
 * <pre>
 * Created by nEbuLa on 15/11/2015.
 *
 * Set Based Model
 *
 * Description:     This model is built based on a published paper (see references below). This model
 *                  combines set theory and vector space model ranking. The fundamental
 *                  idea is to use mutual dependencies among index terms to improve results. Term dependencies
 *                  are captured through term-sets, which are sets of correlated terms.
 *
 *                  The algorithm of this model is to find all frequent term-sets (similar to finding
 *                  association rules in data mining) based on the query terms. Term-sets refer to a set
 *                  of query terms that appeared in documents. These frequent term-sets
 *                  are then treated as if a single term and calculate values such as TF and IDF, and
 *                  use ranking functions in vector space model.
 *
 *                  For a simple example, consider the query contains terms A, B, C, D, E. After getting
 *                  values from inverted index, we found out that A, B, D, E, AB, AD, BD and ABD are frequent
 *                  term-sets. We then calculate their TF and IDF values, and apply vector space model.
 *
 * References:      http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.116.7973&rep=rep1&type=pdf
 *                  http://grupoweb.upf.es/WRG/mir2ed/pdf/slides_chap03.pdf (see Set-based model section)
 * </pre>
 */
public class SetBasedVectorSpaceModel extends VectorSpaceModel {

    private double mTermSetRelativeMinSupport;
    private int mProximityDistance;
    private int mMaximumAssociationLevel;

    public SetBasedVectorSpaceModel() {
        // The proximity distance means how far can a term be apart from another term to be considered as a term set.
        // Setting 10 means term A and term B must only have 9 other terms in order to be a term set.
        // Adjusting this value to a high distance may result in higher computation time.
        this.mProximityDistance = 46;

        // For all term-sets, the frequency of the term-set must exceed or equal to the threshold in
        // order to be considered as frequent. Adjusting this value will greatly impact the retrieval result.
        // The unit is measured in number of documents of the term (i.e document frequency) divided by total num. of documents.
        // Set it between 0 to 1.
        this.mTermSetRelativeMinSupport = 0.014;

        // To prevent heavy computation, you can limit the program when to stop deriving next term-set level here.
        // You can set to Integer.MAX_VALUE for generating all possible association levels.
        this.mMaximumAssociationLevel = Integer.MAX_VALUE;
    }

    @Override
    public HashMap<Integer, Double> getRankedDocumentsWithoutSort(Query pQuery) {
        // retrievedDocuments will have a structure <Document ID, ranking score>
        HashMap<Integer, Double> retrievedDocuments = new HashMap<>();

        // Get the median document vector length for further computation.
        double medianDocumentVectorLength = InvertedIndexAdapter.getInstance().getMedianDocumentVectorLength();

        // Get all frequent query term-sets based on the input query.
        // The structure is <Term-set level, A set of query term-sets in that level>
        ArrayList<AssociationLevel> allFrequentAssociationLevels = this.generateAllAssocLevelWithFrequentTermSets(
                pQuery,
                this.mProximityDistance,
                this.mTermSetRelativeMinSupport,
                this.mMaximumAssociationLevel
        );

        // For each term-sets association level
        for (AssociationLevel currentAssocLevel : allFrequentAssociationLevels) {
            // For each term-sets in the current association level, we calculate its accumulated score for each document.
            for (QueryTermSet frequentQueryTermSet : currentAssocLevel.mAllFrequentQueryTermSets) {
                double queryTermSetWeight;
                double queryTermSetIDF;
                HashMap<Integer, Integer> documentsContainTermSet;

                try {
                    queryTermSetWeight = frequentQueryTermSet.getAveragedWeight();
                    queryTermSetIDF = frequentQueryTermSet.getInvertedDocumentFrequency();
                    documentsContainTermSet = frequentQueryTermSet.getDocumentToTermSetFrequenciesMap();

                    for (HashMap.Entry<Integer, Integer> document : documentsContainTermSet.entrySet()) {
                        int documentID = document.getKey();
                        int documentTermSetFrequency = document.getValue();
                        double documentVectorLength = InvertedIndexAdapter.getInstance().getDocumentVectorLength(documentID);

                        if (!retrievedDocuments.containsKey(documentID)) {
                            // Document is newly retrieved, initialize its document ranking to 0.
                            retrievedDocuments.put(documentID, 0.0);
                        }

                        this.accumulateDocumentScore(
                                retrievedDocuments,
                                documentID,
                                queryTermSetWeight,
                                queryTermSetIDF,
                                documentTermSetFrequency,
                                documentVectorLength,
                                medianDocumentVectorLength,
                                this.mPivotBParameter.value(),
                                this.mBM25KParameter.value(),
                                this.mNormalizationType
                        );
                    }  // End document foreach
                } catch (Exception error) {
                    Debug.loge("Unknown Error!",error);
                }
            }  // End query term foreach
        }  // End association level foreach

        return retrievedDocuments;
    }


    /*
     *
     *  Association levels with frequent term-sets generation
     *
     */
    protected ArrayList<AssociationLevel> generateAllAssocLevelWithFrequentTermSets(Query pQuery, int pProximityDistance,
                                                                                    double pTermSetRelativeMinSupport, int pMaxAssocLevel) {
        // This list stores all association level, which stores query term-sets.
        ArrayList<AssociationLevel> allFrequentAssocLevels = new ArrayList<>();

        // We consider user input query terms are important and must be included in the query as query term-sets.
        AssociationLevel firstAssocLevel = this.deriveFirstLevelWithFrequentTermSets(
                pQuery,
                pProximityDistance,
                pTermSetRelativeMinSupport
        );
        allFrequentAssocLevels.add(firstAssocLevel);

        // Then we iterate through different levels of association rules, starting from level 2, to find
        // frequent query term sets.
        for (int currentAssocLevel = 2; currentAssocLevel <= pMaxAssocLevel; currentAssocLevel++) {
            // Minus 2 because we need to get the previous level and the ArrayList index starts from zero.
            AssociationLevel previousLevel = allFrequentAssocLevels.get(currentAssocLevel - 2);
            AssociationLevel currentLevel = this.deriveNextLevelWithFrequentTermSets(
                    previousLevel,
                    pProximityDistance,
                    pTermSetRelativeMinSupport
            );

            if (currentLevel.getNumberOfFrequentTermSets() <= 0) {
                // No frequent term-sets are found. Stop iterating to the next level.
                break;
            }

            allFrequentAssocLevels.add(currentLevel);
        }

        return allFrequentAssocLevels;
    }

    protected AssociationLevel deriveFirstLevelWithFrequentTermSets(Query pQuery, int pProximityDistance,
                                                                    double pTermSetRelativeMinSupport) {
        AssociationLevel firstAssocLevel = new AssociationLevel();
        firstAssocLevel.mLevelNumber = 1;

        // expendedQueryTerms will have a structure <Query term string, query term weight>
        ExpandedTerm[] expendedQueryTerms = pQuery.expandedTerms();

        // All query terms in the query are candidate term-set.
        LinkedHashSet<QueryTermSet> candidateTermSets = new LinkedHashSet<>();
        for (ExpandedTerm expendedQueryTerm : expendedQueryTerms) {
            QueryTermSet newQueryTermSet = new QueryTermSet();
            newQueryTermSet.addQueryTerm(expendedQueryTerm);
            candidateTermSets.add(newQueryTermSet);
        }

        // For each query term, calculate the term frequency, document frequency and other
        // values based on the proximity distance input.
        this.setProximityDistanceForEachTermSet(candidateTermSets, pProximityDistance);
        this.updateValuesForEachTermSet(candidateTermSets);

        // For each query term, we need to verify if it is frequent or not
        // If not, kick it out from the candidate set.
        // Note: We treat all user inputs are important, so we do not check for min. support
        // for first level to let all terms to be included in the calculation.
        // candidateTermSets = this.filterCandidateSetsBySupport(candidateTermSets, pTermSetAbsoluteSupport);

        firstAssocLevel.mAllFrequentQueryTermSets = candidateTermSets;
        return firstAssocLevel;
    }

    protected AssociationLevel deriveNextLevelWithFrequentTermSets(AssociationLevel pPreviousLevelTermSets,
                                                                   int pProximityDistance, double pTermSetRelativeMinSupport) {
        AssociationLevel nextAssocLevel = new AssociationLevel();
        nextAssocLevel.mLevelNumber = pPreviousLevelTermSets.mLevelNumber + 1;

        // This is used to hold all derived term-sets for later computation, but these
        // term-sets are not necessarily frequent, thus called as candidate.
        // The candidate term-sets have already been pruned.
        LinkedHashSet<QueryTermSet> candidateTermSets = this.deriveNextLevelCandidateTermSets(pPreviousLevelTermSets);

        if (candidateTermSets.size() <= 0) {
            return nextAssocLevel;  // Not able to derive any items and return empty association level.
        }

        // For each query term, calculate the term frequency, document frequency and other
        // values based on the proximity distance input.
        this.setProximityDistanceForEachTermSet(candidateTermSets, pProximityDistance);
        this.updateValuesForEachTermSet(candidateTermSets);

        // For each candidate query term, we verify if it is frequent or not.
        // If not, kick it out from the candidate set.
        candidateTermSets = this.filterCandidateSetsBySupport(candidateTermSets, pTermSetRelativeMinSupport);

        nextAssocLevel.mAllFrequentQueryTermSets = candidateTermSets;
        return nextAssocLevel;
    }


    /*
     *
     *  Candidate term-sets generation
     *
     */
    protected LinkedHashSet<QueryTermSet> deriveNextLevelCandidateTermSets(AssociationLevel pPreviousLevelTermSets) {
        LinkedHashSet<QueryTermSet> candidateTermSets = new LinkedHashSet<>();

        for (QueryTermSet frequentTermSet1 : pPreviousLevelTermSets.mAllFrequentQueryTermSets) {
            for (QueryTermSet frequentTermSet2 : pPreviousLevelTermSets.mAllFrequentQueryTermSets) {
                QueryTermSet unionTermSet = this.unionQueryTermSets(frequentTermSet1, frequentTermSet2);

                // We are looking for term-sets in one level, and all term-sets size in that level
                // should be the same as the level number. i.e. in level 3, all term-sets should only contain 3 terms.
                if (unionTermSet.size() != pPreviousLevelTermSets.mLevelNumber + 1) {
                    continue; // Too many terms and do not consider at this level
                }

                // If the newly derived term-set has already been added to the candidate frequent term-sets,
                // then do not add it again.
                if (this.hasTermSetsAlreadyContain(candidateTermSets, unionTermSet)) {
                    continue;
                }

                // Pruning step, based on the definition that any set will not be frequent if
                // some subsets of it is not frequent, we check if its all immediate subset are
                // in the previous association level, if one of the term-set is missing, then it will not
                // be a candidate set.
                if (this.someImmediateSubsetsAreNotFrequent(pPreviousLevelTermSets.mAllFrequentQueryTermSets, unionTermSet)) {
                    continue;
                }

                // Add to the association level, we call them candidate term-sets.
                candidateTermSets.add(unionTermSet);
            }
        }

        return candidateTermSets;
    }


    /*
     *
     *  Helper methods
     *
     */
    protected QueryTermSet unionQueryTermSets(QueryTermSet pTermSet1, QueryTermSet pTermSet2) {
        QueryTermSet unionTermSet = new QueryTermSet();
        unionTermSet.mAllTerms = (LinkedHashSet<ExpandedTerm>) pTermSet1.mAllTerms.clone();
        unionTermSet.mAllTerms.addAll(pTermSet2.mAllTerms);
        return unionTermSet;
    }

    protected boolean hasTermSetsAlreadyContain(LinkedHashSet<QueryTermSet> pCandidateTermSets, QueryTermSet pTermSet) {
        for (QueryTermSet currentCandidateTermSet : pCandidateTermSets) {
            if (currentCandidateTermSet.mAllTerms.containsAll(pTermSet.mAllTerms)) {
                return true;
            }
        }
        return false;
    }

    protected ArrayList<QueryTermSet> deriveAllImmediateSubsets(QueryTermSet pTermSetBeingDerived) {
        ArrayList<QueryTermSet> derivedImmediateSubsets = new ArrayList<>();

        for (ExpandedTerm term1 : pTermSetBeingDerived.mAllTerms) {
            QueryTermSet subsetTermSet = new QueryTermSet();

            for (ExpandedTerm term2 : pTermSetBeingDerived.mAllTerms) {
                if (term1.term().termStem().equals(term2.term().termStem())) {
                    continue;
                }
                subsetTermSet.mAllTerms.add(term2);
            }

            derivedImmediateSubsets.add(subsetTermSet);
        }

        return derivedImmediateSubsets;
    }

    protected LinkedHashSet<QueryTermSet> filterCandidateSetsBySupport(LinkedHashSet<QueryTermSet> pCandidateSet,
                                                                       double pRelativeMinSupportThreshold) {
        LinkedHashSet<QueryTermSet> resultSet = new LinkedHashSet<>();

        for (QueryTermSet currentCandidateTermSet : pCandidateSet) {
            double documentRelativeFrequency;
            int totalNumOfDocuments = InvertedIndexAdapter.getInstance().getNumberOfDocument();
            try {
                // Multiply 1.0 to cast the type to double before division.
                documentRelativeFrequency = (currentCandidateTermSet.getDocumentFrequency() * 1.0) / (totalNumOfDocuments * 1.0);

                if (documentRelativeFrequency >= pRelativeMinSupportThreshold) {
                    resultSet.add(currentCandidateTermSet);
                }
            } catch (Exception error) {
                Debug.loge("Unknown Error!",error);
            }
        }

        return resultSet;
    }

    protected void setProximityDistanceForEachTermSet(LinkedHashSet<QueryTermSet> pCandidateTermSets, int pProximityDistance) {
        for (QueryTermSet currentCandidateTermSet : pCandidateTermSets) {
            currentCandidateTermSet.setProximityDistanceThreshold(pProximityDistance);
        }
    }

    protected void updateValuesForEachTermSet(LinkedHashSet<QueryTermSet> pCandidateTermSets) {
        for (QueryTermSet currentCandidateTermSet : pCandidateTermSets) {
            currentCandidateTermSet.updateValues();
        }
    }

    protected boolean someImmediateSubsetsAreNotFrequent(LinkedHashSet<QueryTermSet> pPreviousLevelFrequentTermSets,
                                                         QueryTermSet pCheckingTermSet) {
        for (QueryTermSet immediateSubset : this.deriveAllImmediateSubsets(pCheckingTermSet)) {
            if (!this.hasTermSetsAlreadyContain(pPreviousLevelFrequentTermSets, immediateSubset)) {
                return true;
            }
        }
        return false;
    }


    /*
     *
     *  Getter methods
     *
     */
    public double getTermSetRelativeMinSupport() {
        return this.mTermSetRelativeMinSupport;
    }

    /*
     *
     *  Setter methods
     *
     */
    public void setTermSetRelativeMinSupport(double pValue) {
        this.mTermSetRelativeMinSupport = pValue;
    }

    public int getProximityDistance() {
        return this.mProximityDistance;
    }

    public void setProximityDistance(int pValue) {
        this.mProximityDistance = pValue;
    }

    public int getMaximumAssociationLevel() {
        return this.mMaximumAssociationLevel;
    }

    public void setMaximumAssociationLevel(int pValue) {
        this.mMaximumAssociationLevel = pValue;
    }


    /*
     *
     *  AssociationLevel inner class declaration
     *
     */
    class AssociationLevel {

        protected int mLevelNumber;
        protected LinkedHashSet<QueryTermSet> mAllFrequentQueryTermSets;

        public AssociationLevel() {
            this.mLevelNumber = 0;
            this.mAllFrequentQueryTermSets = new LinkedHashSet<>();
        }

        public int getNumberOfFrequentTermSets() {
            return this.mAllFrequentQueryTermSets.size();
        }

    }  // End inner class AssociationLevel


    /*
     *
     *  QueryTermSet inner class declarations
     *
     */
    class QueryTermSet {

        protected boolean mIsValueUpdate;
        protected int mProximityDistanceThreshold;
        protected double mAveragedWeight;
        protected int mDocumentFrequency;
        protected double mInvertedDocumentFrequency;
        protected HashMap<Integer, Integer> mDocumentToTermSetFrequenciesMap;
        protected LinkedHashSet<ExpandedTerm> mAllTerms;

        protected String NOT_UP_TO_DATE_ERROR_MSG = "Not able to retrieve query term set's value because it is not up-to-date.";

        public QueryTermSet() {
            this.mIsValueUpdate = true;
            this.mAveragedWeight = 0.0;
            this.mDocumentFrequency = 0;
            this.mInvertedDocumentFrequency = 0.0;
            this.mDocumentToTermSetFrequenciesMap = new HashMap<>();
            this.mAllTerms = new LinkedHashSet<>();
        }


        /*
         *
         *  Value update methods
         *
         */
        public void updateValues() {
            this.mAveragedWeight = this.computeAveragedWeight(this.mAllTerms);

            this.mDocumentToTermSetFrequenciesMap = this.computeDocumentTermSetFrequencies(
                    this.mAllTerms,
                    this.mProximityDistanceThreshold
            );

            this.mDocumentFrequency = this.computeDocumentFrequency(this.mDocumentToTermSetFrequenciesMap);

            this.mInvertedDocumentFrequency = this.computeInvertedDocumentFrequency(
                    this.mDocumentFrequency,
                    InvertedIndexAdapter.getInstance().getNumberOfDocument()
            );

            // After updating, the object getter methods can be used.
            this.mIsValueUpdate = true;
        }

        protected double computeAveragedWeight(LinkedHashSet<ExpandedTerm> pAllTerms) {
            double averagedWeight = 0.0;
            for (ExpandedTerm queryTerm : pAllTerms) {
                averagedWeight += queryTerm.weight();
            }
            averagedWeight /= pAllTerms.size();
            return averagedWeight;
        }

        protected HashMap<Integer, Integer> computeDocumentTermSetFrequencies(LinkedHashSet<ExpandedTerm> pAllTerms,
                                                                              int pProximityDistanceThreshold) {
            HashMap<Integer, Integer> documentToTermFrequencyMap = new HashMap<>();

            // Find documents that has all query terms appear.
            HashSet<Integer> hasAllTermsDocumentIDs = this.getAllDocumentIDsThatAllTermsAppeared(pAllTerms);

            for (Integer currentProcessingDocumentID : hasAllTermsDocumentIDs) {
                // Initialize the term frequency count to zero.
                documentToTermFrequencyMap.put(currentProcessingDocumentID, 0);

                ExpandedTerm firstTerm = this.getTermSetFirstElement(pAllTerms);
                ArrayBuffer<Object> firstTermPositions = firstTerm.term().filePositionMap().get(currentProcessingDocumentID).get();

                Iterator<Object> firstTermPosIterator = firstTermPositions.iterator();

                firstTermPosIteration:
                while (firstTermPosIterator.hasNext()) {
                    int currentFirstTermPosition = (int) firstTermPosIterator.next();
                    int remainingProximityDistance = pProximityDistanceThreshold;

                    // For each other terms, we check if there is any term that is within the proximity distance.
                    // We iterate the terms in reverse order to check if the last one falls into the proximity distance
                    // first, if the last few terms does not fall, there then the first term position checking can
                    // be skipped to the next one.
                    LinkedList<ExpandedTerm> reversedAllTerms = new LinkedList<>(pAllTerms);
                    java.util.Iterator<ExpandedTerm> reversedAllTermsIterator = reversedAllTerms.descendingIterator();
                    while (reversedAllTermsIterator.hasNext()) {
                        ExpandedTerm comparingTerm = reversedAllTermsIterator.next();
                        if (comparingTerm == firstTerm) {
                            // Do not compare itself,
                            // break because the list is iterated in reverse order, so comparingTerm == firstTerm
                            // comparingTerm must be the last term.
                            break;
                        }

                        ArrayBuffer<Object> comparingTermPositions = comparingTerm.term().filePositionMap()
                                .get(currentProcessingDocumentID).get();

                        Iterator<Object> comparingTermPosIterator = comparingTermPositions.iterator();

                        while (comparingTermPosIterator.hasNext()) {
                            // We assume all positions are sorted in ascending order.
                            int currentComparingTermPosition = (int) comparingTermPosIterator.next();
                            int twoTermsProximityDistance = currentComparingTermPosition - currentFirstTermPosition;

                            // If the comparing term is located before the current first term position,
                            // then it is not necessary to consider it and just move the comparing term pointer forward.
                            if (twoTermsProximityDistance <= 0) {
                                continue;
                            }

                            if (twoTermsProximityDistance > remainingProximityDistance) {
                                // If the comparing term is too far away from the current first term position we
                                // are comparing, there is no need to consider the current first term position anymore,
                                // because any further comparing terms will have a farther distance and thus
                                // not possible to be matched.
                                continue firstTermPosIteration;
                            } else {
                                // Otherwise, we found one term falls into the proximity range, continue to search
                                // for other terms.
                                remainingProximityDistance = twoTermsProximityDistance;
                                break;
                            }
                        }
                    }

                    // When reached here, that means a term set is found.
                    documentToTermFrequencyMap.put(
                            currentProcessingDocumentID,
                            documentToTermFrequencyMap.get(currentProcessingDocumentID) + 1
                    );
                }
            }

            return documentToTermFrequencyMap;
        }  // End computeDocumentTermSetFrequencies()

        protected int computeDocumentFrequency(HashMap<Integer, Integer> pDocumentToTermSetFrequenciesMap) {
            return pDocumentToTermSetFrequenciesMap.size();
        }

        protected double computeInvertedDocumentFrequency(int pDocumentFrequency, int pTotalNumberOfDocuments) {
            // Multiply 1.0 to cast the number to double, int divided by int will be an int, which
            // is not desirable.
            return Math.log(
                    (pTotalNumberOfDocuments * 1.0) / (pDocumentFrequency * 1.0 + 1.0)
            );
        }


        /*
         *
         *  Helper methods
         *
         */
        protected ExpandedTerm getTermSetFirstElement(LinkedHashSet<ExpandedTerm> pTheList) {
            if (pTheList.size() <= 0) {
                return null;
            }
            return pTheList.iterator().next();
        }

        protected HashSet<Integer> getAllDocumentIDsThatAllTermsAppeared(LinkedHashSet<ExpandedTerm> pAllTerms) {
            HashSet<Integer> hasAllTermsDocumentIDs = new HashSet<>();

            // Get all the document IDs for each term, and then do set intersection operation to
            // find which documents has all terms.
            ExpandedTerm firstTerm = this.getTermSetFirstElement(pAllTerms);
            if (firstTerm == null) {
                return hasAllTermsDocumentIDs;
            }

            ScalaSupport.foreachMap(firstTerm.term().filePositionMap(), new Consumer<Tuple2<Object, ArrayBuffer<Object>>>() {
                @Override
                public void accept(Tuple2<Object, ArrayBuffer<Object>> pDocumentContainFirstTerm) {
                    int documentIDContainFirstTerm = (int) pDocumentContainFirstTerm._1();
                    hasAllTermsDocumentIDs.add(documentIDContainFirstTerm);
                }
            });

            for (ExpandedTerm comparingTerm : pAllTerms) {
                if (comparingTerm == firstTerm) {
                    continue;  // Do not intersect itself.
                }

                HashSet<Integer> currentComparingDocumentIDs = new HashSet<>();
                ScalaSupport.foreachMap(comparingTerm.term().filePositionMap(), new Consumer<Tuple2<Object, ArrayBuffer<Object>>>() {
                    @Override
                    public void accept(Tuple2<Object, ArrayBuffer<Object>> pDocumentContainCurrentTerm) {
                        int documentIDContainCurrentTerm = (int) pDocumentContainCurrentTerm._1();
                        currentComparingDocumentIDs.add(documentIDContainCurrentTerm);
                    }
                });

                // Do an intersection operation to remove documents that does not appear all query term.
                hasAllTermsDocumentIDs.retainAll(currentComparingDocumentIDs);
            }

            return hasAllTermsDocumentIDs;
        }


        /*
         *
         *  Setter methods
         *
         */
        public void addQueryTerm(ExpandedTerm pTheQueryTerm) {
            this.mAllTerms.add(pTheQueryTerm);
            this.mIsValueUpdate = false;
        }

        public void setProximityDistanceThreshold(int pProximityDistanceThreshold) {
            this.mProximityDistanceThreshold = pProximityDistanceThreshold;
            this.mIsValueUpdate = false;
        }


        /*
         *
         *  Getter methods
         *
         */
        public double getAveragedWeight() throws Exception {
            if (!mIsValueUpdate) {
                throw new Exception(this.NOT_UP_TO_DATE_ERROR_MSG);
            }
            return this.mAveragedWeight;
        }

        public int getDocumentFrequency() throws Exception {
            if (!mIsValueUpdate) {
                throw new Exception(this.NOT_UP_TO_DATE_ERROR_MSG);
            }
            return this.mDocumentFrequency;
        }

        public double getInvertedDocumentFrequency() throws Exception {
            if (!mIsValueUpdate) {
                throw new Exception(this.NOT_UP_TO_DATE_ERROR_MSG);
            }
            return this.mInvertedDocumentFrequency;
        }

        public HashMap<Integer, Integer> getDocumentToTermSetFrequenciesMap() throws Exception {
            if (!mIsValueUpdate) {
                throw new Exception(this.NOT_UP_TO_DATE_ERROR_MSG);
            }
            return this.mDocumentToTermSetFrequenciesMap;
        }

        public int size() {
            return this.mAllTerms.size();
        }

    }  // End inner class QueryTermSet

}  // End SetBasedVectorSpaceModel