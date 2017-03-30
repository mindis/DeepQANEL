/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.template;

import de.citec.sc.corpus.AnnotatedDocument;
import de.citec.sc.utils.DBpediaEndpoint;
import de.citec.sc.variable.HiddenVariable;

import de.citec.sc.variable.State;
import factors.Factor;
import factors.FactorScope;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import learning.Vector;
import net.ricecode.similarity.SimilarityStrategy;
import net.ricecode.similarity.StringSimilarityMeasures;
import templates.AbstractTemplate;

/**
 *
 * @author sherzod
 */
public class NELEdgeTemplate extends AbstractTemplate<AnnotatedDocument, State, StateFactorScope<State>> {

    private Set<String> validPOSTags;
    private Set<String> frequentWordsToExclude;
    private Map<Integer, String> semanticTypes;

    public NELEdgeTemplate(Set<String> validPOSTags, Set<String> frequentWordsToExclude, Map<Integer, String> s) {
        this.validPOSTags = validPOSTags;
        this.semanticTypes = s;
        this.frequentWordsToExclude = frequentWordsToExclude;
    }

    @Override
    public List<StateFactorScope<State>> generateFactorScopes(State state) {
        List<StateFactorScope<State>> factors = new ArrayList<>();

        for (Integer key : state.getDocument().getParse().getNodes().keySet()) {

            HiddenVariable a = state.getHiddenVariables().get(key);

            factors.add(new StateFactorScope<>(this, state));
        }

        return factors;
    }

    @Override
    public void computeFactor(Factor<StateFactorScope<State>> factor) {
        State state = factor.getFactorScope().getState();

        Vector featureVector = factor.getFeatureVector();

        //add dependency feature between tokens
        for (Integer tokenID : state.getDocument().getParse().getNodes().keySet()) {
            String headToken = state.getDocument().getParse().getToken(tokenID);
            String headPOS = state.getDocument().getParse().getPOSTag(tokenID);
            String headURI = state.getHiddenVariables().get(tokenID).getCandidate().getUri();
            Integer dudeID = state.getHiddenVariables().get(tokenID).getDudeId();
            String dudeName = "EMPTY";
            if (dudeID != -1) {
                dudeName = semanticTypes.get(dudeID);
            }

            if (headURI.equals("EMPTY_STRING")) {
                continue;
            }

            List<Integer> dependentNodes = state.getDocument().getParse().getDependentEdges(tokenID, validPOSTags, frequentWordsToExclude);
            List<Integer> siblings = state.getDocument().getParse().getSiblings(tokenID, validPOSTags, frequentWordsToExclude);

            if (!dependentNodes.isEmpty()) {

                for (Integer depNodeID : dependentNodes) {
                    String depToken = state.getDocument().getParse().getToken(depNodeID);
                    String depURI = state.getHiddenVariables().get(depNodeID).getCandidate().getUri();
                    String depPOS = state.getDocument().getParse().getPOSTag(depNodeID);
                    Integer depDudeID = state.getHiddenVariables().get(depNodeID).getDudeId();
                    String depRelation = state.getDocument().getParse().getDependencyRelation(depNodeID);
                    String depDudeName = "EMPTY";
                    if (depDudeID != -1) {
                        depDudeName = semanticTypes.get(depDudeID);
                    }

                    if (depURI.equals("EMPTY_STRING")) {
                        continue;
                    }

                    Set<String> mergedIntervalPOSTAGs = state.getDocument().getParse().getIntervalPOSTagsMerged(tokenID, depNodeID);

                    //handle specific case
                    //mayor of Tel-Aviv, headquarters of MI6
                    // NN(s) IN NNP
                    for (String pattern : mergedIntervalPOSTAGs) {
//                        featureVector.addToValue("NEL EDGE - DEP FEATURE: " + pattern + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " ", 1.0);
                    }
                    
                    double depSimilarityScore = getSimilarityScore(depToken, depURI);
                    double depDBpediaScore = state.getHiddenVariables().get(depNodeID).getCandidate().getDbpediaScore();
                    
                    double headSimilarityScore = getSimilarityScore(headToken, headURI);
                    double headMatollScore = state.getHiddenVariables().get(tokenID).getCandidate().getMatollScore();
                    
                    double score = (Math.max(depSimilarityScore, depDBpediaScore)) * 0.7 + 0.3 * (Math.max(headMatollScore, headSimilarityScore));
                    
                    
                    
                    if(headURI.contains("ontology")){
//                        score += 0.1;
//                        featureVector.addToValue("NEL EDGE - DEP FEATURE: ONTOLOGY Namespace " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " dep-sim-score = ", depSimilarityScore);
//                        featureVector.addToValue("NEL EDGE - DEP FEATURE: ONTOLOGY Namespace " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " dep-dbpedia-score = ", depDBpediaScore);
//                        featureVector.addToValue("NEL EDGE - DEP FEATURE: ONTOLOGY Namespace " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " head-sim-score = ", headSimilarityScore);
//                        featureVector.addToValue("NEL EDGE - DEP FEATURE: ONTOLOGY Namespace " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " head-matoll-score = ", headMatollScore);
                        
                        
                        featureVector.addToValue("NEL EDGE - DEP FEATURE: ONTOLOGY Namespace " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " score = ", score);
                    }
                    else{
//                        featureVector.addToValue("NEL EDGE - DEP FEATURE: " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " dep-sim-score = ", depSimilarityScore);
//                        featureVector.addToValue("NEL EDGE - DEP FEATURE: " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " dep-dbpedia-score = ", depDBpediaScore);
//                        featureVector.addToValue("NEL EDGE - DEP FEATURE: " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " head-sim-score = ", headSimilarityScore);
//                        featureVector.addToValue("NEL EDGE - DEP FEATURE: " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " head-matoll-score = ", headMatollScore);
                        
                        
                        featureVector.addToValue("NEL EDGE - DEP FEATURE: " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " score = ", score);
                     }
                    
                    
                    
                    
//                    if(depSimilarityScore >= 0.8 && headSimilarityScore >= 0.8){
//                        featureVector.addToValue("NEL EDGE - DEP FEATURE: " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " dep-sim >= 0.8 && head-sim >= 0.8", 1.0);
//                    }
//                    if(depSimilarityScore >= 0.8 && headMatollScore >= 0.3){
//                        featureVector.addToValue("NEL EDGE - DEP FEATURE: " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " dep-sim >= 0.8 && head-matoll >= 0.3", 1.0);
//                    }
//                    if(depSimilarityScore >= 0.8 && headMatollScore >= 0.5){
//                        featureVector.addToValue("NEL EDGE - DEP FEATURE: " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " dep-sim >= 0.8 && head-matoll >= 0.5", 1.0);
//                    }
//                    if(depSimilarityScore >= 0.8 && headMatollScore >= 0.8){
//                        featureVector.addToValue("NEL EDGE - DEP FEATURE: " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " dep-sim >= 0.8 && head-matoll >= 0.8", 1.0);
//                    }
//                    if(headURI.endsWith("er")){
//                        featureVector.addToValue("NEL EDGE - DEP FEATURE: " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " head ends with -er", 1.0);
//                    }
//                    
                    

                }
            }
            if (!siblings.isEmpty()) {
                for (Integer depNodeID : siblings) {
                    String depToken = state.getDocument().getParse().getToken(depNodeID);
                    String depURI = state.getHiddenVariables().get(depNodeID).getCandidate().getUri();
                    String depPOS = state.getDocument().getParse().getPOSTag(depNodeID);
                    Integer depDudeID = state.getHiddenVariables().get(depNodeID).getDudeId();
                    String depRelation = state.getDocument().getParse().getSiblingDependencyRelation(depNodeID, tokenID);
                    
                    String depDudeName = "EMPTY";
                    if (depDudeID != -1) {
                        depDudeName = semanticTypes.get(depDudeID);
                    }

                    if (depURI.equals("EMPTY_STRING")) {
                        continue;
                    }

                    Set<String> mergedIntervalPOSTAGs = state.getDocument().getParse().getIntervalPOSTagsMerged(tokenID, depNodeID);

                    //handle specific case
                    //mayor of Tel-Aviv, headquarters of MI6
                    // NN(s) IN NNP
                    for (String pattern : mergedIntervalPOSTAGs) {
//                        featureVector.addToValue("NEL EDGE - SIBLING FEATURE: " + pattern + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " ", 1.0);
                    }

                    
                    
                    
                    double depSimilarityScore = getSimilarityScore(depToken, depURI);
                    double depDBpediaScore = state.getHiddenVariables().get(depNodeID).getCandidate().getDbpediaScore();
                    double headSimilarityScore = getSimilarityScore(headToken, headURI);
                    double headMatollScore = state.getHiddenVariables().get(tokenID).getCandidate().getMatollScore();
                    
                    
                    double score = (Math.max(depSimilarityScore, depDBpediaScore)) * 0.7 + 0.3 * (Math.max(headMatollScore, headSimilarityScore));
                    
                    if(headURI.contains("ontology")){
//                        score += 0.1;
//                        featureVector.addToValue("NEL EDGE - SIBLING FEATURE: ONTOLOGY Namespace " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " dep-sim-score = ", depSimilarityScore);
//                        featureVector.addToValue("NEL EDGE - SIBLING FEATURE: ONTOLOGY Namespace " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " dep-dbpedia-score = ", depDBpediaScore);
//                        featureVector.addToValue("NEL EDGE - SIBLING FEATURE: ONTOLOGY Namespace " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " head-sim-score = ", headSimilarityScore);
//                        featureVector.addToValue("NEL EDGE - SIBLING FEATURE: ONTOLOGY Namespace " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " head-matoll-score = ", headMatollScore);
                        
                        featureVector.addToValue("NEL EDGE - SIBLING FEATURE: ONTOLOGY Namespace " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " score = ", score);
                    }
                    else{
//                        featureVector.addToValue("NEL EDGE - SIBLING FEATURE: " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " dep-sim-score = ", depSimilarityScore);
//                        featureVector.addToValue("NEL EDGE - SIBLING FEATURE: " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " dep-dbpedia-score = ", depDBpediaScore);
//                        featureVector.addToValue("NEL EDGE - SIBLING FEATURE: " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " head-sim-score = ", headSimilarityScore);
//                        featureVector.addToValue("NEL EDGE - SIBLING FEATURE: " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " head-matoll-score = ", headMatollScore);
                        
                        featureVector.addToValue("NEL EDGE - SIBLING FEATURE: " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " score = ", score);
                     }
                }
            }
        }
    }
    
    /**
     * levenstein sim
     */
    private double getSimilarityScore(String node, String uri) {

        uri = uri.replace("http://dbpedia.org/resource/", "");
        uri = uri.replace("http://dbpedia.org/property/", "");
        uri = uri.replace("http://dbpedia.org/ontology/", "");
        uri = uri.replace("http://www.w3.org/1999/02/22-rdf-syntax-ns#type###", "");

        uri = uri.replaceAll("@en", "");
        uri = uri.replaceAll("\"", "");
        uri = uri.replaceAll("_", " ");

        //replace capital letters with space
        //to tokenize compount classes e.g. ProgrammingLanguage => Programming Language
        String temp = "";
        for (int i = 0; i < uri.length(); i++) {
            String c = uri.charAt(i) + "";
            if (c.equals(c.toUpperCase())) {
                temp += " ";
            }
            temp += c;
        }

        uri = temp.trim().toLowerCase();

        //compute levenstein edit distance similarity and normalize
        final double weightedEditSimilarity = StringSimilarityMeasures.score(uri, node);

        return weightedEditSimilarity;
    }

}
