/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fullStack;

import de.citec.sc.evaluator.AnswerEvaluator;
import de.citec.sc.evaluator.QueryEvaluator;
import de.citec.sc.learning.NELObjectiveFunction;
import de.citec.sc.learning.QAObjectiveFunction;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author sherzod
 */
public class ObjectiveFunctionTest {
    
    @Test
    public void test(){
        
        NELObjectiveFunction function1 = new NELObjectiveFunction();
        QAObjectiveFunction function2 = new QAObjectiveFunction();
        
        String q1 = "SELECT DISTINCT ?uri WHERE { <http://dbpedia.org/resource/Family_Guy> <http://dbpedia.org/ontology/creator> ?uri . }";
        String q2 = "SELECT DISTINCT ?uri WHERE { <http://dbpedia.org/resource/Family_Guy> <http://dbpedia.org/ontology/developer> ?uri . }";
        
        double score1 = function1.computeValue(q1, q2);
        double score2 = function2.computeValue(q1, q2);
        
        System.out.println("Sim score NEL: "+score1);
        System.out.println("Sim score QA: "+score2);
        
        Assert.assertEquals(true, score1 >= 0.5);
        Assert.assertEquals(true, score2 > 0.5);
        
    }
}
