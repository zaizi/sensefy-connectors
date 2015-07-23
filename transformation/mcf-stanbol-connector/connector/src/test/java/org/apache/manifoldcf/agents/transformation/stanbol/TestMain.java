package org.apache.manifoldcf.agents.transformation.stanbol;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.apache.manifoldcf.agents.system.Logging;
import org.apache.stanbol.client.StanbolClient;
import org.apache.stanbol.client.enhancer.model.EnhancementResult;
import org.apache.stanbol.client.enhancer.model.EntityAnnotation;
import org.apache.stanbol.client.enhancer.model.TextAnnotation;
import org.apache.stanbol.client.entityhub.model.Entity;
import org.apache.stanbol.client.entityhub.model.LDPathProgram;
import org.apache.stanbol.client.exception.StanbolClientException;
import org.apache.stanbol.client.impl.StanbolClientImpl;
import org.apache.stanbol.client.services.exception.StanbolServiceException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

public class TestMain
{
    private static final String STANBOL_ENDPOINT = "http://localhost:8080/";
    private static final String ENHANCEMENT_ENGINE = "dbpedia-fst-linking";
    final static String parisId = "http://dbpedia.org/resource/Paris";
    final static String ldPathProgram = "@prefix find:<http://stanbol.apache.org/ontology/entityhub/find/>;"
            + " find:labels = rdfs:label :: xsd:string; find:comment = rdfs:comment[@en] :: xsd:string; "
            + "find:categories = dc:subject :: xsd:anyURI; find:mainType = rdf:type :: xsd:anyURI;";

    public static void main(String[] args) throws StanbolClientException, StanbolServiceException
    {
        StanbolClient client = new StanbolClientImpl(STANBOL_ENDPOINT);
        
        EnhancementResult enhancements = client.enhancer().enhance(null,
                "Paris is the capital of France", ENHANCEMENT_ENGINE);
        enhancements.disambiguate();
        
        LDPathProgram program = new LDPathProgram(ldPathProgram);

        for (TextAnnotation ta : enhancements.getTextAnnotations())
        {
            System.out.println("==================================================================");

            System.out.println("Selected context : " + ta.getSelectionContext());
            System.out.println("Selected text : " + ta.getSelectedText());

            for (EntityAnnotation ea : enhancements.getEntityAnnotations(ta))
            {
                System.out.println("site : " + ea.getSite() + " confidence : " + ea.getConfidence()
                        + "\n entityAnnotation label : " + ea.getEntityLabel() + "\n entityAnnotation uri : "
                        + ea.getUri() + "\n entityAnnotation ref : " + ea.getEntityReference());
             
                // ea.getEntityReference();
                Entity entity = client.entityhub().lookup(ea.getEntityReference(), true);
                List<String> labels = entity.getLabels();
                
                List<String> entityTypes = entity.getTypes();
                System.out.println("entity uri: " + entity.getUri());
//                 for (String label : labels)
//                 {
//                 System.out.println("label: " + label);
//                 }
                 for (String type : entityTypes)
                 {
                     System.out.println("entity type: " + type);
                 }
                
                for (String prop : entity.getProperties())
                {
                    List<String> propValues = entity.getPropertyValue(prop, false);
                    for(String val : propValues){
                       // System.out.println("prop : " +  prop + " value :" + val);
                    }
                }
////               
//                 for(String entityProperty : entity.getEntityPropertiesMap().keySet()){
//                     List<String> propValues = entity.getEntityPropertiesMap().get(entityProperty);
//                     while(propValues.iterator().hasNext()){
//                         String val = propValues.iterator().next();
//                         System.out.println("prop :" + entityProperty + " value :" + val);
//                     }
//                 }

                //System.out.println("extracting properties using ldpath");
                // program.addNamespace("find", "http://stanbol.apache.org/ontology/entityhub/find/");
                // program.addFieldDefinition("find:categories", "dc:subject :: xsd:anyURI;");
                //if(ea.getSite().equals("dbpedia"))
                {
//                    Model model = client.entityhub().ldpath(ea.getEntityReference(), program);
//                    System.out.println("label prop value : "
//                            + model.getProperty("http://stanbol.apache.org/ontology/entityhub/find/labels"));
//                    
//                    Property labelsProperty = model.getProperty("http://stanbol.apache.org/ontology/entityhub/find/labels"); 

//                    NodeIterator nodeIterator = model.listObjectsOfProperty(labelsProperty);
//                    while(nodeIterator.hasNext()){
//                        RDFNode node = nodeIterator.next();
//                        System.out.println("label value from model : " + node.asLiteral().getString());
//                       
//                    }
                    
//                    NodeIterator iterator = model.listObjectsOfProperty(model.getResource(ea.getEntityReference()),
//                            labelsProperty);
//                    while(iterator.hasNext()){
//                        String label = iterator.next().asLiteral().toString();
//                        System.out.println("label : " + label);
//                    }
                    
                    
//                    
//                     String category = model.listObjectsOfProperty(model.getResource(ea.getEntityReference()),
//                     labelsProperty).next().asLiteral().toString();
//                     System.out.println("label : " + category);
                    
                }

                System.out.println("==================================================================");

            }
        }
    }
}
