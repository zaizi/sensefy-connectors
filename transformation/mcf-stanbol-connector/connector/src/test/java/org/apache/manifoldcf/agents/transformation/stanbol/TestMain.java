package org.apache.manifoldcf.agents.transformation.stanbol;

import java.util.List;

import org.apache.stanbol.*;
import org.apache.stanbol.client.Enhancer;
import org.apache.stanbol.client.StanbolClientFactory;
import org.apache.stanbol.client.enhancer.impl.EnhancerParameters;
import org.apache.stanbol.client.enhancer.model.EnhancementStructure;
import org.apache.stanbol.client.enhancer.model.EntityAnnotation;
import org.apache.stanbol.client.enhancer.model.TextAnnotation;
import org.apache.stanbol.client.exception.StanbolClientException;
import org.apache.stanbol.client.services.exception.StanbolServiceException;

public class TestMain
{
    private static final String STANBOL_ENDPOINT = "http://localhost:8080/";
    private static final String ENHANCEMENT_ENGINE = "dbpedia-fst-linking";
    final static String parisId = "http://dbpedia.org/resource/Paris";
    final static String ldPathProgram = "@prefix find:<http://stanbol.apache.org/ontology/entityhub/find/>;"
            + " find:labels = rdfs:label :: xsd:string; find:comment = rdfs:comment[@en] :: xsd:string; "
            + "find:categories = dc:subject :: xsd:anyURI; find:mainType = rdf:type :: xsd:anyURI;";

    
//    public static void stanbol05() throws StanbolClientException, StanbolServiceException
//    {
//        StanbolClient client = new StanbolClientImpl(STANBOL_ENDPOINT);
//
//        EnhancementResult enhancements = client.enhancer().enhance(null, "Paris is the capital of France",
//                ENHANCEMENT_ENGINE);
//        enhancements.disambiguate();
//
//        LDPathProgram program = new LDPathProgram(ldPathProgram);
//
//        for (TextAnnotation ta : enhancements.getTextAnnotations())
//        {
//            System.out.println("==================================================================");
//
//            System.out.println("Selected context : " + ta.getSelectionContext());
//            System.out.println("Selected text : " + ta.getSelectedText());
//
//            for (EntityAnnotation ea : enhancements.getEntityAnnotations(ta))
//            {
//                System.out.println("site : " + ea.getSite() + " confidence : " + ea.getConfidence()
//                        + "\n entityAnnotation label : " + ea.getEntityLabel() + "\n entityAnnotation uri : "
//                        + ea.getUri() + "\n entityAnnotation ref : " + ea.getEntityReference());
//
//                // ea.getEntityReference();
//                Entity entity = client.entityhub().lookup(ea.getEntityReference(), true);
//                List<String> labels = entity.getLabels();
//
//                List<String> entityTypes = entity.getTypes();
//                System.out.println("entity uri: " + entity.getUri());
//                // for (String label : labels)
//                // {
//                // System.out.println("label: " + label);
//                // }
//                for (String type : entityTypes)
//                {
//                    System.out.println("entity type: " + type);
//                }
//
//                for (String prop : entity.getProperties())
//                {
//                    List<String> propValues = entity.getPropertyValue(prop, false);
//                    for (String val : propValues)
//                    {
//                        System.out.println("prop : " + prop + " value :" + val);
//                    }
//                }
//                // //
//                // for(String entityProperty : entity.getEntityPropertiesMap().keySet()){
//                // List<String> propValues = entity.getEntityPropertiesMap().get(entityProperty);
//                // while(propValues.iterator().hasNext()){
//                // String val = propValues.iterator().next();
//                // System.out.println("prop :" + entityProperty + " value :" + val);
//                // }
//                // }
//
//                // System.out.println("extracting properties using ldpath");
//                // program.addNamespace("find", "http://stanbol.apache.org/ontology/entityhub/find/");
//                // program.addFieldDefinition("find:categories", "dc:subject :: xsd:anyURI;");
//                // if(ea.getSite().equals("dbpedia"))
//                {
//                    // Model model = client.entityhub().ldpath(ea.getEntityReference(), program);
//                    // System.out.println("label prop value : "
//                    // + model.getProperty("http://stanbol.apache.org/ontology/entityhub/find/labels"));
//                    //
//                    // Property labelsProperty =
//                    // model.getProperty("http://stanbol.apache.org/ontology/entityhub/find/labels");
//
//                    // NodeIterator nodeIterator = model.listObjectsOfProperty(labelsProperty);
//                    // while(nodeIterator.hasNext()){
//                    // RDFNode node = nodeIterator.next();
//                    // System.out.println("label value from model : " + node.asLiteral().getString());
//                    //
//                    // }
//
//                    // NodeIterator iterator = model.listObjectsOfProperty(model.getResource(ea.getEntityReference()),
//                    // labelsProperty);
//                    // while(iterator.hasNext()){
//                    // String label = iterator.next().asLiteral().toString();
//                    // System.out.println("label : " + label);
//                    // }
//
//                    //
//                    // String category = model.listObjectsOfProperty(model.getResource(ea.getEntityReference()),
//                    // labelsProperty).next().asLiteral().toString();
//                    // System.out.println("label : " + category);
//
//                }
//
//                System.out.println("==================================================================");
//
//            }
//        }
//
//    }

    public static void main(String[] args) throws StanbolClientException, StanbolServiceException
    {
        final StanbolClientFactory factory = new StanbolClientFactory(STANBOL_ENDPOINT);
        final Enhancer client = factory.createEnhancerClient();
        EnhancerParameters parameters = EnhancerParameters.
                    builder().
                    buildDefault("Paris is the capital of France");
    
        EnhancementStructure eRes = client.enhance(parameters);

        for(TextAnnotation ta: eRes.getTextAnnotations()){
            System.out.println("********************************************");
            System.out.println("Selection Context: " + ta.getSelectionContext());
            System.out.println("Selected Text: " + ta.getSelectedText());
            System.out.println("Engine: " + ta.getCreator());
            System.out.println("Candidates: ");
            for(EntityAnnotation ea:eRes.getEntityAnnotations(ta))
                  System.out.println("\t" + ea.getEntityLabel() + " - " + ea.getEntityReference());
        }
        
    }
}
