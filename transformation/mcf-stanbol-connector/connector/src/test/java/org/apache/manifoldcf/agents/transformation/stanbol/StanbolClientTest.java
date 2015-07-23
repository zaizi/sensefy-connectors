package org.apache.manifoldcf.agents.transformation.stanbol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.manifoldcf.agents.system.Logging;
import org.apache.stanbol.client.StanbolClient;
import org.apache.stanbol.client.enhancer.model.EnhancementResult;
import org.apache.stanbol.client.enhancer.model.EntityAnnotation;
import org.apache.stanbol.client.enhancer.model.TextAnnotation;
import org.apache.stanbol.client.entityhub.model.Entity;
import org.apache.stanbol.client.exception.StanbolClientException;
import org.apache.stanbol.client.impl.StanbolClientImpl;
import org.apache.stanbol.client.services.exception.StanbolServiceException;

import junit.framework.TestCase;

public class StanbolClientTest extends TestCase
{
    private static final String STANBOL_ENDPOINT = "http://localhost:8080/";
    private static final String ENHANCER_PATH = "enhancer";

    public void testStanbolEnhancement() throws StanbolServiceException, StanbolClientException
    {
        final StanbolClient client = new StanbolClientImpl(STANBOL_ENDPOINT);

        EnhancementResult enhancements = client.enhancer().enhance(null, "Paris is the capital of France");
        // assertNotNull(enhancements);
        // assertFalse(enhancements.getEnhancements().size() == 0);
        // assertTrue(enhancements.getEntityAnnotations().size() == 6);
        // assertEquals(enhancements.getEntityAnnotations().iterator().next().getSite(), "dbpedia");
        //
        // List<String> labels = new ArrayList<String>();
        // for (EntityAnnotation ea : enhancements.getEntityAnnotations())
        // {
        // labels.add(ea.getEntityLabel());
        // }
        //
        // assertTrue(labels.contains("Paris"));
        // assertTrue(labels.contains("France"));
        // enhancements.disambiguate();

        for (TextAnnotation ta : enhancements.getTextAnnotations())
        {
            System.out.println("Selected context : " + ta.getSelectionContext());
            System.out.println("Selected text : " + ta.getSelectedText());

            for (EntityAnnotation ea : enhancements.getEntityAnnotations(ta))
            {
                System.out.println("==================================================================");
                System.out.println("Entity confidence : " + ea.getConfidence() + " Entity label : "
                        + ea.getEntityLabel() + "\n ref : " + ea.getEntityReference());
                // ea.getEntityReference();
                Entity entity = client.entityhub().lookup(ea.getEntityReference(), true);
                List<String> labels = entity.getLabels();
                List<String> entityTypes = entity.getTypes();
                System.out.println("entity uri: " + entity.getUri());
//                for (String label : labels)
//                {
//                    System.out.println("label: " + label);
//                }
//                for (String type : entityTypes)
//                {
//                    System.out.println("entity type: " + type);
//                }
               
                for(String prop : entity.getProperties()){
                   System.out.println("property : " + prop);
                }

                System.out.println("==================================================================");
            }
        }
    }
}
