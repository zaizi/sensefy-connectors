/*******************************************************************************
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
 *******************************************************************************/
package org.zaizi.manifoldcf.agents.output.solrwrapper.utility;

import org.apache.commons.io.IOUtils;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.crawler.system.Logging;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * THis class contains utility methods to parse JSON, convert them to Repository Documents, create utility maps
 * @Author: Alessandro Benedetti
 * Date: 04/07/2014
 */
public class JSONRepositoryDocumentSerializer
{
    public static final String OCCURRENCES_FIELD = "occurrences";

    public static final String DOC_IDS_FIELD = "doc_ids";

    public static final String DOCUMENT_ID_FIELD = "id";

    public static final String ENTITIES_FIELD = "entities";

    public static final String ENTITY_TYPES_FIELD = "entity_types";

    /**
     * Create a Repository Document from the  JSON Object string.
     * This RepoDocument is really simple :
     * Id + binary content ( JSON)
     * @param id
     * @param atomicRemovalJSON
     * @return
     * @throws ManifoldCFException
     */
    public static RepositoryDocument createRepoDocFromJSON( String id, String atomicRemovalJSON )
        throws ManifoldCFException
    {
        RepositoryDocument removalRepoDoc=new RepositoryDocument();
        removalRepoDoc.addField( DOCUMENT_ID_FIELD,new String[]{id} );
        InputStream inputStream = IOUtils.toInputStream( atomicRemovalJSON );
        removalRepoDoc.setBinary(inputStream,atomicRemovalJSON.getBytes().length);
        return  removalRepoDoc;
    }

    /**
     * Returns a JSONArray containing the removal update
     * @param documentURI
     * @return
     * @throws JSONException
     */
    public static JSONArray createAtomicRemovalJSON(String entityId, String documentURI,boolean removeDocURI)
        throws JSONException
    {

        JSONObject removalJSON=new JSONObject(  );

        JSONObject inc=new JSONObject();
        inc.put( "inc","-1" );

        if(removeDocURI){
        JSONObject remove= new JSONObject();
        remove.put( "remove",documentURI );
        removalJSON.put( DOC_IDS_FIELD,remove ); }

        removalJSON.put( DOCUMENT_ID_FIELD,entityId);
        removalJSON.put( OCCURRENCES_FIELD,inc );


        JSONArray removalJSONArray=new JSONArray(  );
        removalJSONArray.put( removalJSON );
        return removalJSONArray; // To change body of created methods use File | Settings | File Templates.
    }

    /**
     * Returns a Semantic Map with a List of Repository Document per Index.
     * The documents are extracted from metadata of a RepositoryDocument .
     * Inside each metadata field we have a String[] of JSON Arrays containing each one one Document
     * @param document
     * @return
     * @throws java.io.IOException
     * @throws ManifoldCFException
     */
    public static Map<IndexNames, List<RepositoryDocument>> parseJSONChildStructures(String parentURI,RepositoryDocument document)
        throws  ManifoldCFException
    {
        Map<IndexNames, List<RepositoryDocument>> index2RepoDocs=new HashMap<IndexNames, List<RepositoryDocument>>();

        List<RepositoryDocument> primaryDocumentList;
        List<RepositoryDocument> entitiesList;
        List<RepositoryDocument> entityTypesList;

        try
        {
            // Parse Entities
            String[] entitiesJSON = document.getFieldAsStrings( ENTITIES_FIELD );
            entitiesList=getRepoDocListFromJSONArrayString( document, entitiesJSON,ENTITIES_FIELD );
            index2RepoDocs.put( IndexNames.ENTITY_INDEX,entitiesList);

            // parse EntityTypes
            String[] entityTypesJSON = document.getFieldAsStrings( ENTITY_TYPES_FIELD );
            entityTypesList=getRepoDocListFromJSONArrayString( document, entityTypesJSON,ENTITY_TYPES_FIELD );
            index2RepoDocs.put( IndexNames.ENTITY_TYPE_INDEX,entityTypesList);
        }
        catch ( IOException e )
        {
            Logging.connectors.error( "Error in the encoded JSON children of the current Repository Document :"+parentURI,e );
        }

        //set the primaryDocument
        primaryDocumentList=new ArrayList<RepositoryDocument>();
        primaryDocumentList.add(document);
        index2RepoDocs.put( IndexNames.PRIMARY_INDEX,primaryDocumentList);

        return index2RepoDocs; // To change body of created methods use File | Settings | File Templates.
    }


    /**
     * Parse a String array of JSON representation for Objects to a List of Repository Documents
     * @param document
     * @param entitiesJSON
     * @return
     * @throws ManifoldCFException
     */
    private static List<RepositoryDocument> getRepoDocListFromJSONArrayString( RepositoryDocument document, String[] entitiesJSON,String repoField )
        throws ManifoldCFException
    {
        List<RepositoryDocument> resultRepoDocList;
        resultRepoDocList=new ArrayList<RepositoryDocument>();
        if(entitiesJSON!=null)
        for(String JSONEntityString:entitiesJSON){
            try
            {
                JSONArray JSONArray=new JSONArray(JSONEntityString);
                JSONObject JSONEntity=JSONArray.getJSONObject( 0 );
                String currentId=JSONEntity.get( DOCUMENT_ID_FIELD ).toString();
                RepositoryDocument repoDocFromJSON =JSONRepositoryDocumentSerializer.createRepoDocFromJSON( currentId, JSONEntityString );
                resultRepoDocList.add( repoDocFromJSON );
            }
            catch ( JSONException e )
            {
                Logging.connectors.error("Error processing Entities JSON Child structure: "+JSONEntityString,e );
            }
        }
        String[] nullArray=null;
        document.addField( repoField,nullArray);  // remove the field from the primary document
        return resultRepoDocList;
    }

}
