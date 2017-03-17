/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.sampling;

import de.citec.sc.query.Candidate;
import de.citec.sc.query.Instance;
import de.citec.sc.query.ManualLexicon;

import de.citec.sc.query.Search;
import de.citec.sc.utils.DBpediaEndpoint;
import de.citec.sc.utils.Stopwords;
import de.citec.sc.variable.HiddenVariable;
import de.citec.sc.variable.State;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import sampling.Explorer;

/**
 *
 * @author sherzod
 */
public class SlotExplorer implements Explorer<State> {

    private Map<Integer, String> semanticTypes;
    private Map<Integer, String> specialSemanticTypes;
    private Set<String> validPOSTags;
    private Set<String> frequentWordsToExclude;
    private Set<String> wordsWithSpecialSemanticTypes;

    public SlotExplorer(Map<Integer, String> s, Map<Integer, String> sp, Set<String> validPOSTags, Set<String> wordToExclude, Set<String> wS) {
        this.specialSemanticTypes = sp;
        this.semanticTypes = s;
        this.validPOSTags = validPOSTags;
        this.frequentWordsToExclude = wordToExclude;
        this.wordsWithSpecialSemanticTypes = wS;
    }

    @Override
    public List getNextStates(State currentState) {
        List<State> newStates = new ArrayList<>();

        for (int indexOfNode : currentState.getDocument().getParse().getNodes().keySet()) {
            String node = currentState.getDocument().getParse().getNodes().get(indexOfNode);

            String pos = currentState.getDocument().getParse().getPOSTag(indexOfNode);

            if (!validPOSTags.contains(pos)) {
                continue;
            }
            if (frequentWordsToExclude.contains(node.toLowerCase())) {
                continue;
            }

            HiddenVariable headVar = currentState.getHiddenVariables().get(indexOfNode);

            String headDUDEName = semanticTypes.get(headVar.getDudeId());

            //assign extra dude for dependent nodes if the head dude is class or property
            if (!headDUDEName.equals("Property")) {
                continue;
            }

            List<Integer> depNodes = currentState.getDocument().getParse().getDependentEdges(indexOfNode);
            if (depNodes.isEmpty()) {
                int headOfHeadNodeIndex = currentState.getDocument().getParse().getParentNode(indexOfNode);
                String headOfHeadToken = currentState.getDocument().getParse().getToken(headOfHeadNodeIndex);

                if (frequentWordsToExclude.contains(headOfHeadToken)) {
                    depNodes = currentState.getDocument().getParse().getSiblings(indexOfNode);
                }
            }

            for (Integer depNodeIndex : depNodes) {

                //greedy exploring, skip nodes with assigned URI
                if (!currentState.getHiddenVariables().get(depNodeIndex).getCandidate().getUri().equals("EMPTY_STRING")) {
                    continue;
                }

                String depNode = currentState.getDocument().getParse().getNodes().get(depNodeIndex);

                //assign special semantic types to certain words  such as : who, which, where, when ...
                if (wordsWithSpecialSemanticTypes.contains(depNode.toLowerCase())) {

                    for (Integer indexOfDepDude : specialSemanticTypes.keySet()) {

                        List<Integer> usedSlots = currentState.getUsedSlots(indexOfNode);

                        if (usedSlots.contains(1)) {
                            State s = new State(currentState);

                            Candidate emptyInstance = new Candidate(new Instance("EMPTY_STRING", 0), 0, 0, 0);

                            s.addHiddenVariable(depNodeIndex, indexOfDepDude, emptyInstance);

                            //add as slot 2 since 1 taken by another node
                            s.addSlotVariable(depNodeIndex, indexOfNode, 2);

                            if (!s.equals(currentState) && !newStates.contains(s)) {
                                newStates.add(s);
                            }
                        }
                        
                        if (usedSlots.contains(2)) {
                            State s = new State(currentState);

                            Candidate emptyInstance = new Candidate(new Instance("EMPTY_STRING", 0), 0, 0, 0);

                            s.addHiddenVariable(depNodeIndex, indexOfDepDude, emptyInstance);

                            //add as slot 2 since 1 taken by another node
                            s.addSlotVariable(depNodeIndex, indexOfNode, 1);

                            if (!s.equals(currentState) && !newStates.contains(s)) {
                                newStates.add(s);
                            }
                        }
                    }
                }
            }
        }

        return newStates;
    }
}
