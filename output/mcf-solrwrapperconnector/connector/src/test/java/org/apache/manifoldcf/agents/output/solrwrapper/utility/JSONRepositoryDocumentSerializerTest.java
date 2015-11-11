/**
 * Sensefy
 *
 * Copyright (c) Zaizi Limited, All rights reserved.
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 **/
package org.apache.manifoldcf.agents.output.solrwrapper.utility;

import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.json.JSONArray;
import org.json.JSONException;
import org.zaizi.manifoldcf.agents.output.solrwrapper.utility.IndexNames;
import org.zaizi.manifoldcf.agents.output.solrwrapper.utility.JSONRepositoryDocumentSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: Alessandro Benedetti Date: 04/07/2014
 */
public class JSONRepositoryDocumentSerializerTest extends TestCase
{
    public void testCreateRemovalRepoDoc() throws IOException, ManifoldCFException
    {
        String expectedJSON = "[{\"id\":\"entityId1\",\"occurrences\":{\"inc\":\"-1\"},\"doc_ids\":{\"remove\":\"uri1\"}}]";

        String id = "entityId1";
        RepositoryDocument removalRepoDoc = JSONRepositoryDocumentSerializer.createRepoDocFromJSON(id, expectedJSON);
        InputStream binaryStream = removalRepoDoc.getBinaryStream();
        String currentStreamAsString = IOUtils.toString(binaryStream);
        assertEquals(expectedJSON, currentStreamAsString);
        assertEquals(removalRepoDoc.getFieldAsStrings("id")[0], id);

    }

    public void testCreateAtomicRemovalJSON() throws JSONException
    {
        String documentURI = "uri1";

        JSONArray atomicRemovalJSON = JSONRepositoryDocumentSerializer.createAtomicRemovalJSON("entityId1", documentURI,true);
        String expectedJSON = "[{\"id\":\"entityId1\",\"occurrences\":{\"inc\":\"-1\"},\"doc_ids\":{\"remove\":\"uri1\"}}]";
        ;
        assertEquals(expectedJSON, atomicRemovalJSON.toString());

    }

    public void testJSONParsing() throws ManifoldCFException
    {
        String parentURI = "parentURI";
        RepositoryDocument currentDocument = this.buildInputRepoDoc( parentURI );

        Map<IndexNames, List<RepositoryDocument>> expectedJSONChildStructures = this.buildExpectedMap(currentDocument,
                parentURI);

        Map<IndexNames, List<RepositoryDocument>> parsedJSONChildStructures =JSONRepositoryDocumentSerializer.parseJSONChildStructures(parentURI,currentDocument  );
        assertEquals(expectedJSONChildStructures.keySet().size(),parsedJSONChildStructures.keySet().size());
        assertEquals(3,parsedJSONChildStructures.keySet().size());
        assertEquals(expectedJSONChildStructures.get( IndexNames.ENTITY_TYPE_INDEX ).size(),parsedJSONChildStructures.get(
            IndexNames.ENTITY_TYPE_INDEX ).size());
        assertEquals(30,parsedJSONChildStructures.get( IndexNames.ENTITY_TYPE_INDEX ).size());
        assertEquals(expectedJSONChildStructures.get( IndexNames.ENTITY_INDEX ).size(),parsedJSONChildStructures.get( IndexNames.ENTITY_INDEX ).size());
        assertEquals(50,parsedJSONChildStructures.get( IndexNames.ENTITY_INDEX ).size());


    }



    private Map<IndexNames, List<RepositoryDocument>> buildExpectedMap(RepositoryDocument document, String parentURI)
            throws ManifoldCFException
    {
        Map<IndexNames, List<RepositoryDocument>> resultingMap = new HashMap<IndexNames, List<RepositoryDocument>>();
        List<RepositoryDocument> primaryDocumentList;
        List<RepositoryDocument> entitiesList;
        List<RepositoryDocument> entityTypesList;
        // Parse Entities
        entitiesList = this.generateExpectedEntitiesList();
        resultingMap.put(IndexNames.ENTITY_INDEX, entitiesList);

        // parse EntityTypes
        entityTypesList = this.generateExpectedEntityTypesList();
        resultingMap.put(IndexNames.ENTITY_TYPE_INDEX, entityTypesList);

        // set the primaryDocument
        primaryDocumentList = new ArrayList<RepositoryDocument>();
        primaryDocumentList.add(document);
        resultingMap.put(IndexNames.PRIMARY_INDEX, primaryDocumentList);

        return resultingMap;
    }




    private List<RepositoryDocument> generateExpectedEntityTypesList() throws ManifoldCFException
    {
        List<RepositoryDocument> entitiesList = new ArrayList<RepositoryDocument>();
        for (int i = 0; i < 30; i++)
        {
            entitiesList.add(this.generateExpectedRepoDoc( "EntityTypeId" + String.valueOf( i ) ));
        }

        return entitiesList; // To change body of created methods use File | Settings | File Templates.
    }

    private List<RepositoryDocument> generateExpectedEntitiesList() throws ManifoldCFException
    {
        List<RepositoryDocument> entitiesList = new ArrayList<RepositoryDocument>();
        for (int i = 0; i < 50; i++)
        {
            entitiesList.add(this.generateExpectedRepoDoc( "EntityId" + String.valueOf( i ) ));
        }

        return entitiesList; // To change body of created methods use File | Settings | File Templates.
    }

    private RepositoryDocument generateExpectedRepoDoc( String id ) throws ManifoldCFException
    {
        RepositoryDocument resultingDoc = new RepositoryDocument();
        resultingDoc.addField("id", id);
        return resultingDoc; // To change body of created methods use File | Settings | File Templates.
    }

    private RepositoryDocument buildInputRepoDoc( String parentURI ) throws ManifoldCFException
    {
        RepositoryDocument repoDoc = new RepositoryDocument();
        repoDoc.addField("id", parentURI);
        String[] entitiesArray = this.generateEntitiesArrayInput( parentURI );
        String[] entitiyTypesArray = this.generateEntityTypesArrayInput();
        repoDoc.addField("entities", entitiesArray);
        repoDoc.addField("entity_types", entitiyTypesArray);

        return repoDoc; // To change body of created methods use File | Settings | File Templates.
    }

    private String[] generateEntityTypesArrayInput()
    {
        String[] entitiTypesArray = new String[30];
        for (int i = 0; i < entitiTypesArray.length; i++)
        {
            entitiTypesArray[i] = this.generateEntityTypeJSON(String.valueOf(i));
        }
        return entitiTypesArray;
    }

    private String generateEntityJSON(String id, String parentURI)
    {
        String entityId = "EntityId" + id;

        String expectedJSON = "[{\"id\":\"" + entityId + "\",\"occurrences\":{\"inc\":\"1\"},\"doc_ids\":{\"add\":\""
                + parentURI + "\"}}]";

        return expectedJSON;

    }

    private String generateEntityTypeJSON(String id)
    {
        String entityId = "EntityTypeId" + id;

        String expectedJSON = "[{\"id\":\"" + entityId + "\",\"occurrences\":{\"inc\":\"1\"}}]";

        return expectedJSON;

    }

    private String[] generateEntitiesArrayInput( String parentURI )
    {
        String[] entitiesArray = new String[50];
        for (int i = 0; i < entitiesArray.length; i++)
        {
            entitiesArray[i] = this.generateEntityJSON(String.valueOf(i), parentURI);
        }
        return entitiesArray;
    }

}
