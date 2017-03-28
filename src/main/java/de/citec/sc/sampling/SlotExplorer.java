/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.sampling;

import de.citec.sc.query.Candidate;
import de.citec.sc.query.Instance;
import de.citec.sc.variable.HiddenVariable;
import de.citec.sc.variable.State;

import java.util.ArrayList;
import java.util.HashSet;
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

            String dudeName = semanticTypes.get(headVar.getDudeId());

            if (dudeName == null) {
                continue;
            }
            //assign extra dude for dependent nodes if the head dude is class or property
            if (!(dudeName.equals("Property") || dudeName.equals("RestrictionClass") || dudeName.equals("Class"))) {
                continue;
            }

            List<Integer> childNodes = currentState.getDocument().getParse().getDependentEdges(indexOfNode);
            List<Integer> siblings = currentState.getDocument().getParse().getSiblings(indexOfNode);

            Set<Integer> depNodes = new HashSet<>();

            //loop over each dependent node and get the dependent nodes of those
            //add the child node itself
            for (Integer childNode : childNodes) {
                List<Integer> childOfChildNodes = currentState.getDocument().getParse().getDependentEdges(childNode);

                depNodes.addAll(childOfChildNodes);
                depNodes.add(childNode);
            }
            for (Integer sibling : siblings) {
                List<Integer> childOfSiblingNodes = currentState.getDocument().getParse().getDependentEdges(sibling);

                depNodes.addAll(childOfSiblingNodes);
                depNodes.add(sibling);
            }

            //get the head node
            Integer headNode = currentState.getDocument().getParse().getParentNode(indexOfNode);

            List<Integer> siblingsOfHeadNode = currentState.getDocument().getParse().getSiblings(headNode);

            //get a list of siblings of the head node, and add the head node itself.
            Set<Integer> headNodes = new HashSet<>();
            headNodes.addAll(siblingsOfHeadNode);
            headNodes.add(headNode);

            boolean hasValidSpecialNode = false;
            for (Integer depNodeIndex : depNodes) {

                //greedy exploring, skip nodes with assigned URI
                if (!currentState.getHiddenVariables().get(depNodeIndex).getCandidate().getUri().equals("EMPTY_STRING")) {
                    continue;
                }

                String depNode = currentState.getDocument().getParse().getNodes().get(depNodeIndex);

                //assign special semantic types to certain words  such as : who, which, where, when ...
                if (wordsWithSpecialSemanticTypes.contains(depNode.toLowerCase())) {

                    //if the the node has been found, no need to explore longer the head nodes
                    hasValidSpecialNode = true;

                    for (Integer indexOfDepDude : specialSemanticTypes.keySet()) {

                        List<Integer> usedSlots = currentState.getUsedSlots(indexOfNode);

                        if (usedSlots.contains(1) && usedSlots.contains(2)) {
                            State s = new State(currentState);

                            Candidate emptyInstance = new Candidate(new Instance("EMPTY_STRING", 0), 0, 0, 0);

                            s.addHiddenVariable(depNodeIndex, indexOfDepDude, emptyInstance);

                            if (!s.equals(currentState) && !newStates.contains(s)) {
                                newStates.add(s);
                            }
                        } else if (usedSlots.contains(1) && !usedSlots.contains(2)) {
                            State s = new State(currentState);

                            Candidate emptyInstance = new Candidate(new Instance("EMPTY_STRING", 0), 0, 0, 0);

                            s.addHiddenVariable(depNodeIndex, indexOfDepDude, emptyInstance);

                            //add as slot 2 since 1 taken by another node
                            s.addSlotVariable(depNodeIndex, indexOfNode, 2);

                            if (!s.equals(currentState) && !newStates.contains(s)) {
                                newStates.add(s);
                            }
                        } else if (!usedSlots.contains(1) && usedSlots.contains(2)) {
                            State s = new State(currentState);

                            Candidate emptyInstance = new Candidate(new Instance("EMPTY_STRING", 0), 0, 0, 0);

                            s.addHiddenVariable(depNodeIndex, indexOfDepDude, emptyInstance);

                            //add as slot 2 since 1 taken by another node
                            s.addSlotVariable(depNodeIndex, indexOfNode, 1);

                            if (!s.equals(currentState) && !newStates.contains(s)) {
                                newStates.add(s);
                            }
                        } else {
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

            if (hasValidSpecialNode) {
                continue;
            }
            for (Integer hNode : headNodes) {

                if (hNode == -1) {
                    continue;
                }

                //greedy exploring, skip nodes with assigned URI
                if (!currentState.getHiddenVariables().get(hNode).getCandidate().getUri().equals("EMPTY_STRING")) {
                    continue;
                }

                String hNodeToken = currentState.getDocument().getParse().getNodes().get(hNode);

                //assign special semantic types to certain words  such as : who, which, where, when ...
                if (wordsWithSpecialSemanticTypes.contains(hNodeToken.toLowerCase())) {

                    hasValidSpecialNode = true;
                    for (Integer indexOfDepDude : specialSemanticTypes.keySet()) {

                        List<Integer> usedSlots = currentState.getUsedSlots(indexOfNode);

                        if (usedSlots.contains(1) && usedSlots.contains(2)) {
                            State s = new State(currentState);

                            Candidate emptyInstance = new Candidate(new Instance("EMPTY_STRING", 0), 0, 0, 0);

                            s.addHiddenVariable(hNode, indexOfDepDude, emptyInstance);

                            if (!s.equals(currentState) && !newStates.contains(s)) {
                                newStates.add(s);
                            }
                        } else if (usedSlots.contains(1) && !usedSlots.contains(2)) {
                            State s = new State(currentState);

                            Candidate emptyInstance = new Candidate(new Instance("EMPTY_STRING", 0), 0, 0, 0);

                            s.addHiddenVariable(hNode, indexOfDepDude, emptyInstance);

                            //add as slot 2 since 1 taken by another node
                            s.addSlotVariable(indexOfNode, hNode, 2);

                            if (!s.equals(currentState) && !newStates.contains(s)) {
                                newStates.add(s);
                            }
                        } else if (!usedSlots.contains(1) && usedSlots.contains(2)) {
                            State s = new State(currentState);

                            Candidate emptyInstance = new Candidate(new Instance("EMPTY_STRING", 0), 0, 0, 0);

                            s.addHiddenVariable(hNode, indexOfDepDude, emptyInstance);

                            //add as slot 2 since 1 taken by another node
                            s.addSlotVariable(indexOfNode, hNode, 1);

                            if (!s.equals(currentState) && !newStates.contains(s)) {
                                newStates.add(s);
                            }
                        } else {
                            State s = new State(currentState);

                            Candidate emptyInstance = new Candidate(new Instance("EMPTY_STRING", 0), 0, 0, 0);

                            s.addHiddenVariable(hNode, indexOfDepDude, emptyInstance);

                            //add as slot 2 since 1 taken by another node
                            s.addSlotVariable(indexOfNode, hNode, 1);

                            if (!s.equals(currentState) && !newStates.contains(s)) {
                                newStates.add(s);
                            }
                        }
                    }
                }
            }

            if (!hasValidSpecialNode) {
                //desperate times require desperate measures

                for (int indexOfNode1 : currentState.getDocument().getParse().getNodes().keySet()) {
                    String someToken = currentState.getDocument().getParse().getNodes().get(indexOfNode1);

                    if (wordsWithSpecialSemanticTypes.contains(someToken.toLowerCase())) {

                        //if the the node has been found, no need to explore longer the head nodes
                        hasValidSpecialNode = true;

                        for (Integer indexOfDepDude : specialSemanticTypes.keySet()) {

                            List<Integer> usedSlots = currentState.getUsedSlots(indexOfNode);

                            if (usedSlots.contains(1) && usedSlots.contains(2)) {
                                State s = new State(currentState);

                                Candidate emptyInstance = new Candidate(new Instance("EMPTY_STRING", 0), 0, 0, 0);

                                s.addHiddenVariable(indexOfNode1, indexOfDepDude, emptyInstance);

                                if (!s.equals(currentState) && !newStates.contains(s)) {
                                    newStates.add(s);
                                }
                            } else if (usedSlots.contains(1) && !usedSlots.contains(2)) {
                                State s = new State(currentState);

                                Candidate emptyInstance = new Candidate(new Instance("EMPTY_STRING", 0), 0, 0, 0);

                                s.addHiddenVariable(indexOfNode1, indexOfDepDude, emptyInstance);

                                //add as slot 2 since 1 taken by another node
                                s.addSlotVariable(indexOfNode1, indexOfNode, 2);

                                if (!s.equals(currentState) && !newStates.contains(s)) {
                                    newStates.add(s);
                                }
                            } else if (!usedSlots.contains(1) && usedSlots.contains(2)) {
                                State s = new State(currentState);

                                Candidate emptyInstance = new Candidate(new Instance("EMPTY_STRING", 0), 0, 0, 0);

                                s.addHiddenVariable(indexOfNode1, indexOfDepDude, emptyInstance);

                                //add as slot 2 since 1 taken by another node
                                s.addSlotVariable(indexOfNode1, indexOfNode, 1);

                                if (!s.equals(currentState) && !newStates.contains(s)) {
                                    newStates.add(s);
                                }
                            } else {
                                State s = new State(currentState);

                                Candidate emptyInstance = new Candidate(new Instance("EMPTY_STRING", 0), 0, 0, 0);

                                s.addHiddenVariable(indexOfNode1, indexOfDepDude, emptyInstance);

                                //add as slot 2 since 1 taken by another node
                                s.addSlotVariable(indexOfNode1, indexOfNode, 1);

                                if (!s.equals(currentState) && !newStates.contains(s)) {
                                    newStates.add(s);
                                }
                            }
                        }
                    }
                }
            }
        }

        return newStates;
    }
    
    public List getNextStates2(State currentState) {
        List<State> newStates = new ArrayList<>();

        for (int indexOfNode : currentState.getDocument().getParse().getNodes().keySet()) {
            String node = currentState.getDocument().getParse().getNodes().get(indexOfNode);

            String pos = currentState.getDocument().getParse().getPOSTag(indexOfNode);

            boolean hasValidParent = false;

            if (wordsWithSpecialSemanticTypes.contains(node.toLowerCase())) {

                Integer parentNode = currentState.getDocument().getParse().getParentNode(indexOfNode);

                List<Integer> siblingsOfParentNode = currentState.getDocument().getParse().getSiblings(parentNode, validPOSTags, frequentWordsToExclude);

                List<Integer> headNodes = new ArrayList<>();
                headNodes.add(parentNode);
                headNodes.addAll(siblingsOfParentNode);

                for (Integer headNode : headNodes) {
                    
                    if(hasValidParent){
                        break;
                    }
                    
                    HiddenVariable headVar = currentState.getHiddenVariables().get(headNode);

                    String dudeName = semanticTypes.get(headVar.getDudeId());

                    if (dudeName != null) {
                        //assign extra dude for dependent nodes if the head dude is class or property
                        if ((dudeName.equals("Property") || dudeName.equals("RestrictionClass") || dudeName.equals("Class"))) {

                            for (Integer indexOfDepDude : specialSemanticTypes.keySet()) {
                                List<Integer> usedSlots = currentState.getUsedSlots(headNode);

                                if (usedSlots.contains(1) && usedSlots.contains(2)) {
                                    State s = new State(currentState);

                                    Candidate emptyInstance = new Candidate(new Instance("EMPTY_STRING", 0), 0, 0, 0);

                                    s.addHiddenVariable(indexOfNode, indexOfDepDude, emptyInstance);

                                    if (!s.equals(currentState) && !newStates.contains(s)) {
                                        newStates.add(s);
                                    }
                                } else if (usedSlots.contains(1) && !usedSlots.contains(2)) {
                                    State s = new State(currentState);

                                    Candidate emptyInstance = new Candidate(new Instance("EMPTY_STRING", 0), 0, 0, 0);

                                    s.addHiddenVariable(indexOfNode, indexOfDepDude, emptyInstance);

                                    //add as slot 2 since 1 taken by another node
                                    s.addSlotVariable(indexOfNode, indexOfNode, 2);

                                    if (!s.equals(currentState) && !newStates.contains(s)) {
                                        newStates.add(s);
                                        
                                        hasValidParent = true;
                                    }
                                } else if (!usedSlots.contains(1) && usedSlots.contains(2)) {
                                    State s = new State(currentState);

                                    Candidate emptyInstance = new Candidate(new Instance("EMPTY_STRING", 0), 0, 0, 0);

                                    s.addHiddenVariable(indexOfNode, indexOfDepDude, emptyInstance);

                                    //add as slot 2 since 1 taken by another node
                                    s.addSlotVariable(indexOfNode, indexOfNode, 1);

                                    if (!s.equals(currentState) && !newStates.contains(s)) {
                                        newStates.add(s);
                                        
                                        hasValidParent = true;
                                    }
                                } else {
                                    State s = new State(currentState);

                                    Candidate emptyInstance = new Candidate(new Instance("EMPTY_STRING", 0), 0, 0, 0);

                                    s.addHiddenVariable(indexOfNode, indexOfDepDude, emptyInstance);

                                    //add as slot 2 since 1 taken by another node
                                    s.addSlotVariable(indexOfNode, indexOfNode, 1);

                                    if (!s.equals(currentState) && !newStates.contains(s)) {
                                        newStates.add(s);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return newStates;
    }
}
