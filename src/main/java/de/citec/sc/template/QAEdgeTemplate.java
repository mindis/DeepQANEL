/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.template;

import de.citec.sc.corpus.AnnotatedDocument;
import de.citec.sc.utils.DBpediaEndpoint;
import de.citec.sc.variable.HiddenVariable;
import de.citec.sc.variable.SlotVariable;

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
public class QAEdgeTemplate extends AbstractTemplate<AnnotatedDocument, State, StateFactorScope<State>> {

    private Set<String> validPOSTags;
    private Set<String> frequentWordsToExclude;
    private Map<Integer, String> specialSemanticTypes;

    public QAEdgeTemplate(Set<String> validPOSTags, Set<String> frequentWordsToExclude, Map<Integer, String> s) {
        this.validPOSTags = validPOSTags;
        this.specialSemanticTypes = s;
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
            String token = state.getDocument().getParse().getToken(tokenID);
            String pos = state.getDocument().getParse().getPOSTag(tokenID);
            Integer dudeID = state.getHiddenVariables().get(tokenID).getDudeId();

            String dudeName = "EMPTY";
            if (specialSemanticTypes.containsKey(dudeID)) {
                dudeName = specialSemanticTypes.get(dudeID);

                SlotVariable slotVar = state.getSlotVariables().get(tokenID);

                HiddenVariable headVar = state.getHiddenVariables().get(slotVar.getParentTokenID());

                String headURI = headVar.getCandidate().getUri();
                
                String headPOS = state.getDocument().getParse().getPOSTag(headVar.getTokenId());

                String range = DBpediaEndpoint.getRange(headURI);
                String domain = DBpediaEndpoint.getDomain(headURI);

                featureVector.addToValue("QA EDGE - FEATURE: " + " token: " + token + "   head-dudeID: " + headVar.getDudeId() + "  Slot : " + slotVar.getSlotNumber() + " domain : " + domain, 1.0);
                featureVector.addToValue("QA EDGE - FEATURE: " + " token: " + token + "   head-dudeID: " + headVar.getDudeId() + "  Slot : " + slotVar.getSlotNumber() + " range:  " + range, 1.0);

                String depRelation = state.getDocument().getParse().getDependencyRelation(tokenID);
                
                featureVector.addToValue("QA LEXICAL DEP FEATURE: CHILD_TOKEN: " + token + " SEM-TYPE: " + dudeName + " DEP-REL: " + depRelation, 1.0);
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
