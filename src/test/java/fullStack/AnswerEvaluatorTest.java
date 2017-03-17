/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fullStack;

import de.citec.sc.evaluator.AnswerEvaluator;
import de.citec.sc.evaluator.QueryEvaluator;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author sherzod
 */
public class AnswerEvaluatorTest {
    
    @Test
    public void test(){
        String q1 = "SELECT DISTINCT ?uri WHERE { <http://dbpedia.org/resource/Family_Guy> <http://dbpedia.org/ontology/creator> ?uri . }";
        String q2 = "SELECT DISTINCT ?uri WHERE { <http://dbpedia.org/resource/Family_Guy> <http://dbpedia.org/ontology/developer> ?uri . }";
        
        double score1 = AnswerEvaluator.evaluate(q1, q2);
        double score2 = QueryEvaluator.evaluate(q1, q2);
        
        System.out.println(score1);
        System.out.println(score2);
        
        Assert.assertEquals(true, score1 > 0.5);
        Assert.assertEquals(true, score2 > 0.5);
        
    }
}
