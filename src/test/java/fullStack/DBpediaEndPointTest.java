/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fullStack;

import de.citec.sc.utils.DBpediaEndpoint;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author sherzod
 */
public class DBpediaEndPointTest {

    @Test
    public void test() {
        String range = DBpediaEndpoint.getRange("http://dbpedia.org/ontology/birthPlace");
        String domain = DBpediaEndpoint.getDomain("http://dbpedia.org/ontology/birthPlace");

        System.out.println("Range: " + range);
        System.out.println("Domain: " + domain);
        Assert.assertEquals(true, range.equals("http://dbpedia.org/ontology/Place"));
        Assert.assertEquals(true, domain.equals("http://dbpedia.org/ontology/Person"));

    }
}
