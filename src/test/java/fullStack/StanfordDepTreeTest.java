/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fullStack;

import de.citec.sc.parser.DependencyParse;
import de.citec.sc.parser.StanfordParser;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.PropertiesUtils;
import edu.stanford.nlp.util.StringUtils;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author sherzod
 */
public class StanfordDepTreeTest {

    public static void main(String[] args) {

        String text1 = "I come to home.";
        String text2 = "Ich komme zu Hause.";
        String text3 = "Yo vengo a casa.";

        DependencyParse parse = StanfordParser.parse(text1, StanfordParser.Language.EN);
        
        System.out.println(parse);
//        
        DependencyParse parse2 = StanfordParser.parse(text2, StanfordParser.Language.DE);
        
        System.out.println(parse2);
//        
//        
        DependencyParse parse3 = StanfordParser.parse(text3, StanfordParser.Language.ES);
        
        System.out.println(parse3);
        
//        Annotation document = new Annotation(text3);
////        Properties props = PropertiesUtils.asProperties("props", "src/main/resources/dep-parse-properties/german.props");
//        Properties props = StringUtils.argsToProperties(
//                new String[]{"-props", "src/main/resources/dep-parse-properties/spanish.props"});
//        StanfordCoreNLP corenlp = new StanfordCoreNLP(props);
//        corenlp.annotate(document);
//        
//        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
//
//        for (CoreMap sentence : sentences) {
//
//            SemanticGraph dependencies = sentence.get(SemanticGraphCoreAnnotations.EnhancedDependenciesAnnotation.class);
//            
//            System.out.println(dependencies);
//        }

    }
}
