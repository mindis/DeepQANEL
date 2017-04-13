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
import java.util.HashMap;
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

        Map<String, Double> depFeatures = getDependencyFeatures(state);
        Map<String, Double> siblingFeatures = getSiblingFeatures(state);
        
        
        

        for (String k : depFeatures.keySet()) {
            featureVector.addToValue(k, depFeatures.get(k));
        }

        for (String k : siblingFeatures.keySet()) {
            featureVector.addToValue(k, siblingFeatures.get(k));
        }


    }

    /**
     * returns features that involve edge exploration.
     */
    private Map<String, Double> getDependencyFeatures(State state) {
        Map<String, Double> features = new HashMap<>();

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

                    double depSimilarityScore = getSimilarityScore(depToken, depURI);
                    double depDBpediaScore = state.getHiddenVariables().get(depNodeID).getCandidate().getDbpediaScore();

                    double headSimilarityScore = getSimilarityScore(headToken, headURI);
                    double headMatollScore = state.getHiddenVariables().get(tokenID).getCandidate().getMatollScore();

                    double score = (Math.max(depSimilarityScore, depDBpediaScore)) * 0.7 + 0.3 * (Math.max(headMatollScore, headSimilarityScore));

                    if (depDudeName.equals("Individual")) {
                        double individualScore = depDBpediaScore * 0.3 + depSimilarityScore * 0.7;

//                        featureVector.addToValue("NEL EDGE - DEP FEATURE: Individual" + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " score = ", individualScore);
                    }

                    if (headURI.contains("ontology")) {

                        features.put("NEL EDGE - DEP FEATURE: ONTOLOGY Namespace " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " score = ", score);

                        //mayor of Tel-Aviv, headquarters of MI6
                        // NN(s) IN NNP
                        for (String pattern : mergedIntervalPOSTAGs) {
                            features.put("NEL EDGE - DEP FEATURE: ONTOLOGY Namespace " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " Pattern = " + pattern, 1.0);
                        }
                    } else {

                        features.put("NEL EDGE - DEP FEATURE: RDF Namespace" + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " score = ", score);

                        for (String pattern : mergedIntervalPOSTAGs) {
                            features.put("NEL EDGE - DEP FEATURE: RDF Namespace " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " Pattern = " + pattern, 1.0);
                        }
                    }
                }
            }
        }

        return features;
    }

    private Map<String, Double> getSiblingFeatures(State state) {
        Map<String, Double> features = new HashMap<>();

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

            List<Integer> siblings = state.getDocument().getParse().getSiblings(tokenID, validPOSTags, frequentWordsToExclude);

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

                    double depSimilarityScore = getSimilarityScore(depToken, depURI);
                    double depDBpediaScore = state.getHiddenVariables().get(depNodeID).getCandidate().getDbpediaScore();
                    double depMatollScore = state.getHiddenVariables().get(depNodeID).getCandidate().getMatollScore();

                    double headSimilarityScore = getSimilarityScore(headToken, headURI);
                    double headMatollScore = state.getHiddenVariables().get(tokenID).getCandidate().getMatollScore();
                    double headDBpediaScore = state.getHiddenVariables().get(tokenID).getCandidate().getDbpediaScore();

                    double score = (Math.max(depSimilarityScore, Math.max(depMatollScore, depDBpediaScore))) * 0.7 + 0.3 * (Math.max(Math.max(headDBpediaScore, headMatollScore), headSimilarityScore));

                    if (depDudeName.equals("Individual")) {
                        double individualScore = depDBpediaScore * 0.3 + depSimilarityScore * 0.7;

//                        featureVector.addToValue("NEL EDGE - SIBLING FEATURE: Individual" + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: to sibling node: " + depRelation + " score = ", individualScore);
                    }

                    if (dudeName.equals("Individual")) {
                        double individualScore = headDBpediaScore * 0.3 + headSimilarityScore * 0.7;

//                        featureVector.addToValue("NEL EDGE - SIBLING FEATURE: Individual" + "   sibling: " + dudeName + ":" + headPOS + " dep-relation: to dependent node: " + depRelation + " score = ", individualScore);
                    }

                    if (headURI.contains("ontology")) {

                        features.put("NEL EDGE - SIBLING FEATURE: ONTOLOGY Namespace " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " score = ", score);

                        for (String pattern : mergedIntervalPOSTAGs) {
                            features.put("NEL EDGE - SIBLING FEATURE: ONTOLOGY Namespace " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " Pattern = " + pattern, 1.0);
                        }
                    } else {

                        features.put("NEL EDGE - SIBLING FEATURE: " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " score = ", score);

                        for (String pattern : mergedIntervalPOSTAGs) {
                            features.put("NEL EDGE - SIBLING FEATURE: RDF Namespace " + " head: " + dudeName + ":" + headPOS + "   dep: " + depDudeName + ":" + depPOS + " dep-relation: " + depRelation + " Pattern = " + pattern, 1.0);
                        }
                    }
                }
            }
        }

        return features;
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

        temp = temp.replaceAll("\\s+", " ");
        uri = temp.trim().toLowerCase();

        //compute levenstein edit distance similarity and normalize
        final double weightedEditSimilarity = StringSimilarityMeasures.score(uri, node);

        return weightedEditSimilarity;
    }

}
