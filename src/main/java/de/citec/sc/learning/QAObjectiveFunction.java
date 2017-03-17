/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.learning;

import de.citec.sc.evaluator.AnswerEvaluator;
import de.citec.sc.evaluator.QueryEvaluator;
import de.citec.sc.qald.SPARQLParser;

import de.citec.sc.variable.State;

import java.io.Serializable;
import learning.ObjectiveFunction;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;

/**
 *
 * @author sherzod
 */
public class QAObjectiveFunction extends ObjectiveFunction<State, String> implements Serializable {

    public double computeValue(State deptState, String goldState) {

        return computeScore(deptState, goldState);
    }

    @Override
    protected double computeScore(State state, String goldState) {

        String constructedQuery = QueryConstructor.getSPARQLQuery(state);

        double score1 = AnswerEvaluator.evaluate(constructedQuery, goldState);
        double score2 = QueryEvaluator.evaluate(constructedQuery, goldState);

        double score = Math.max(score1, score2);

        return score;
    }
}
