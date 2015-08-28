package org.apache.manifoldcf.agents.transformation.stanbol;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.io.IOUtils;
import org.apache.manifoldcf.core.interfaces.*;
import org.apache.manifoldcf.agents.interfaces.*;
import org.apache.manifoldcf.agents.system.Logging;
import org.apache.stanbol.client.Enhancer;
import org.apache.stanbol.client.EntityHub;
import org.apache.stanbol.client.StanbolClientFactory;
import org.apache.stanbol.client.enhancer.impl.EnhancerParameters;
import org.apache.stanbol.client.enhancer.model.EnhancementStructure;
import org.apache.stanbol.client.enhancer.model.EntityAnnotation;
import org.apache.stanbol.client.enhancer.model.TextAnnotation;
import org.apache.stanbol.client.entityhub.model.Entity;
import org.apache.stanbol.client.services.exception.StanbolServiceException;

import java.io.*;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Stanbol Enhancer transformation connector 
 * @author Dileepa Jayakody <djayakody@zaizi.com>
 */
public class StanbolEnhancer extends org.apache.manifoldcf.agents.transformation.BaseTransformationConnector
{
    public static final String _rcsid = "@(#)$Id: StanbolEnhancer.java 2015-07-17 djayakody $";

    private static final String EDIT_SPECIFICATION_JS = "editSpecification.js";
    private static final String EDIT_SPECIFICATION_FIELDMAPPING_HTML = "editSpecification_FieldMapping.html";
    private static final String VIEW_SPECIFICATION_HTML = "viewSpecification.html";

    private static final String STANBOL_ENDPOINT = "http://localhost:8080/";
    private static final String STANBOL_ENHANCEMENT_CHAIN = "default";
    /**
     * DOCUMENTS' FIELDS
     */
    static final String ENTITY_INDEX_ID_FIELD = "id"; // the id field for the Entity Index
    static final String DOC_IDS2POS_FIELD = "doc_ids2pos";
    static final String ENTITIES_FIELD = "entities";
    static final String ENTITIES_TYPES_FIELD = "entity_types";
    static final String PARENT_URI_FIELD = "doc_ids";
    static final String OCCURRENCES_FIELD = "occurrences";
    static final String SMLT_ENTITY_TYPES_FIELD = "smlt_entity_types";
    static final String SMLT_ENTITY_URI_FIELD = "smlt_entities";

    static final String PERSON_TYPE_FIELD = "has_person";
    static final String ORGANIZATION_TYPE_FIELD = "has_organization";
    static final String PLACE_TYPE_FIELD = "has_place";

    public static final String TYPE_FIELD = "type";
    public static final String LABEL_FIELD = "label";

    public static final String DESCRIPTION_FIELD = "description";
    public static final String THUMBNAIL_FIELD = "thumbnail";
    public static final String HIERARCHY_FIELD = "hierarchy";
    public static final String ATTRIBUTES_FIELD = "attributes";
    protected static final String ACTIVITY_ENHANCE = "enhance";

    public static final String FOAF_NAMESPACE = "http://xmlns.com/foaf/0.1/";
    public static final String DBPEDIA_NAMESPACE = "http://dbpedia.org/ontology/";
    public static final String SCHEMA_NAMESPACE = "http://schema.org/";

    // attributes we require from the entity
    public static final String DEPICTION_ENTITY_ATTRIBUTE = "depiction";

    // main types of entity
    public static final String PERSON_ENTITY_ATTRIBUTE_VALUE = "Person";
    public static final String ORGANIZATION_ENTITY_ATTRIBUTE_VALUE = "Organization";
    public static final String PLACE_ENTITY_ATTRIBUTE_VALUE = "Place";

    protected static final String[] activitiesList = new String[] { ACTIVITY_ENHANCE };

    private StanbolClientFactory stanbolFactory = null;

    /**
     * Return a list of activities that this connector generates. The connector does NOT need to be connected before
     * this method is called.
     * 
     * @return the set of activities.
     */
    @Override
    public String[] getActivitiesList()
    {
        return activitiesList;
    }

    /**
     * Get an output version string, given an output specification. The output version string is used to uniquely
     * describe the pertinent details of the output specification and the configuration, to allow the Connector
     * Framework to determine whether a document will need to be output again. Note that the contents of the document
     * cannot be considered by this method, and that a different version string (defined in IRepositoryConnector) is
     * used to describe the version of the actual document.
     * 
     * This method presumes that the connector object has been configured, and it is thus able to communicate with the
     * output data store should that be necessary.
     * 
     * @param os is the current output specification for the job that is doing the crawling.
     * @return a string, of unlimited length, which uniquely describes output configuration and specification in such a
     *         way that if two such strings are equal, the document will not need to be sent again to the output data
     *         store.
     */
    @Override
    public VersionContext getPipelineDescription(Specification os) throws ManifoldCFException, ServiceInterruption
    {
        SpecPacker sp = new SpecPacker(os);
        return new VersionContext(sp.toPackedString(), params, os);
    }

    // We intercept checks pertaining to the document format and send modified checks further down

    /**
     * Detect if a mime type is acceptable or not. This method is used to determine whether it makes sense to fetch a
     * document in the first place.
     * 
     * @param pipelineDescription is the document's pipeline version string, for this connection.
     * @param mimeType is the mime type of the document.
     * @param checkActivity is an object including the activities that can be performed by this method.
     * @return true if the mime type can be accepted by this connector.
     */
    public boolean checkMimeTypeIndexable(VersionContext pipelineDescription, String mimeType,
            IOutputCheckActivity checkActivity) throws ManifoldCFException, ServiceInterruption
    {
        // We should see what Tika will transform
        // MHL
        // Do a downstream check
        return checkActivity.checkMimeTypeIndexable("text/plain;charset=utf-8");
    }

    /**
     * Pre-determine whether a document (passed here as a File object) is acceptable or not. This method is used to
     * determine whether a document needs to be actually transferred. This hook is provided mainly to support search
     * engines that only handle a small set of accepted file types.
     * 
     * @param pipelineDescription is the document's pipeline version string, for this connection.
     * @param localFile is the local file to check.
     * @param checkActivity is an object including the activities that can be done by this method.
     * @return true if the file is acceptable, false if not.
     */
    @Override
    public boolean checkDocumentIndexable(VersionContext pipelineDescription, File localFile,
            IOutputCheckActivity checkActivity) throws ManifoldCFException, ServiceInterruption
    {
        // Document contents are not germane anymore, unless it looks like Tika won't accept them.
        // Not sure how to check that...
        return true;
    }

    /**
     * Pre-determine whether a document's length is acceptable. This method is used to determine whether to fetch a
     * document in the first place.
     * 
     * @param pipelineDescription is the document's pipeline version string, for this connection.
     * @param length is the length of the document.
     * @param checkActivity is an object including the activities that can be done by this method.
     * @return true if the file is acceptable, false if not.
     */
    @Override
    public boolean checkLengthIndexable(VersionContext pipelineDescription, long length,
            IOutputCheckActivity checkActivity) throws ManifoldCFException, ServiceInterruption
    {
        // Always true
        return true;
    }

    /**
     * Add (or replace) a document in the output data store using the connector. This method presumes that the connector
     * object has been configured, and it is thus able to communicate with the output data store should that be
     * necessary. The OutputSpecification is *not* provided to this method, because the goal is consistency, and if
     * output is done it must be consistent with the output description, since that was what was partly used to
     * determine if output should be taking place. So it may be necessary for this method to decode an output
     * description string in order to determine what should be done.
     * 
     * @param documentURI is the URI of the document. The URI is presumed to be the unique identifier which the output
     *            data store will use to process and serve the document. This URI is constructed by the repository
     *            connector which fetches the document, and is thus universal across all output connectors.
     * @param outputDescription is the description string that was constructed for this document by the
     *            getOutputDescription() method.
     * @param document is the document data to be processed (handed to the output data store).
     * @param authorityNameString is the name of the authority responsible for authorizing any access tokens passed in
     *            with the repository document. May be null.
     * @param activities is the handle to an object that the implementer of a pipeline connector may use to perform
     *            operations, such as logging processing activity, or sending a modified document to the next stage in
     *            the pipeline.
     * @return the document status (accepted or permanently rejected).
     * @throws IOException only if there's a stream error reading the document data.
     */
    @Override
    public int addOrReplaceDocumentWithException(String documentURI, VersionContext pipelineDescription,
            RepositoryDocument document, String authorityNameString, IOutputAddActivity activities)
            throws ManifoldCFException, ServiceInterruption, IOException
    {

        long startTime = System.currentTimeMillis();
        Logging.agents.debug("Starting to enhance document content in Stanbol connector ");

        String resultCode = "OK";
        String description = null;

        SpecPacker sp = new SpecPacker(pipelineDescription.getSpecification());
        // stanbol server url
        String stanbolServer = sp.getStanbolServer();
        stanbolFactory = new StanbolClientFactory(stanbolServer);

        // Extracting Content.
        long length = document.getBinaryLength();
        byte[] copy = IOUtils.toByteArray(document.getBinaryStream());
        String content = new String(copy);

        Set<String> uris = new HashSet<String>();
        // entityType uris
        Collection<String> entityTypes = new HashSet<String>();

        Collection<String> entitiesJsons = new ArrayList<String>();
        Collection<String> entitiesTypesJSONs = new ArrayList<String>();

        Enhancer enhancerClient = null;
        EntityHub entityhubClient = null;
        EnhancementStructure eRes = null;
        // Create a copy of Repository Document
        RepositoryDocument docCopy = document.duplicate();
        docCopy.setBinary(new ByteArrayInputStream(copy), length);

        try
        {
            String chain = sp.getStanbolChain();
            enhancerClient = stanbolFactory.createEnhancerClient();
            entityhubClient = stanbolFactory.createEntityHubClient();

            EnhancerParameters parameters = EnhancerParameters.builder().setChain(chain).setContent(content).build();
            eRes = enhancerClient.enhance(parameters);

        }
        catch (StanbolServiceException e)
        {
            Logging.agents.error("Error occurred while performing Stanbol enhancement for document : " + documentURI, e);
            resultCode = "STANBOL_ENHANCEMENT_FAIL";
            description = e.getMessage();
        }
        finally
        {
            activities.recordActivity(new Long(startTime), ACTIVITY_ENHANCE, length, documentURI, resultCode,
                    description);

        }
        if (eRes == null)
        {
            // if no enhancement result is received, cannot enhance document, hence reject it
            return DOCUMENTSTATUS_REJECTED; // TODO Make This Configurable
        }
        else
        {
            boolean hasPersonTypeEntities = false;
            boolean hasOrganizationTypeEntities = false;
            boolean hasPlaceTypeEntities = false;
            
            for (TextAnnotation ta : eRes.getTextAnnotations())
            {
                //need to disambiguate the eas returned
                for (EntityAnnotation ea : eRes.getEntityAnnotations(ta))
                {
                    JSONArray nextEntityJSONArray = new JSONArray();
                    JSONObject nextEntityJSON = new JSONObject();
                    nextEntityJSONArray.put(nextEntityJSON);
                    boolean jsonFail = false;

                    uris.add(ea.getEntityReference());
                    // Parent Document + Occurrence Counter
                    try
                    {
                        JSONObject occur = new JSONObject();
                        occur.put("inc", 1);
                        nextEntityJSON.put(OCCURRENCES_FIELD, occur);

                        // to refer to the primary document in the entity object under doc_ids
                        JSONObject parentURI = new JSONObject();
                        parentURI.put("add", documentURI);
                        nextEntityJSON.put(PARENT_URI_FIELD, parentURI);
                        nextEntityJSON.put(ENTITY_INDEX_ID_FIELD, ea.getEntityReference());

                        JSONArray positions = new JSONArray();
                        JSONObject posEntry = new JSONObject();
                        posEntry.put("start", ta.getStart());
                        posEntry.put("end", ta.getEnd());
                        positions.put(posEntry);

                        JSONObject ids2pos = new JSONObject();
                        ids2pos.put(documentURI, positions);
                        JSONObject addObject = new JSONObject();
                        addObject.put("add", ids2pos.toString());
                        nextEntityJSON.put(DOC_IDS2POS_FIELD, addObject);

                        // manually add some default properties for entities by dileepa
                        nextEntityJSON.put("label", ea.getEntityLabel());

                    }
                    catch (JSONException e)
                    {
                        Logging.agents.error("Error creating Entity Document with URI: " + ea, e);
                        jsonFail = true;
                    }

                    Logging.agents.debug("Extracted Stanbol entity label : " + ea.getEntityLabel() + " ref uri : "
                            + ea.getEntityReference());

                    Entity entity = null;
                    // entity types
                    Collection<String> types = null;
                    // adding only the entity-type label;not the whole URI
                    Collection<String> typeLabels = null;

                    try
                    {
                        // calling the entityhub to retrieve the entity object for the entityReference
                        entity = entityhubClient.lookup(ea.getEntityReference(), true);
                        if(entity != null)
                        {
                            // entity types
                            types = entity.getTypes();
                            // adding only the entity-type label;not the whole URI
                            typeLabels = new HashSet<String>();
                            if(types != null){
                                for (String typeURI : types)
                                {
                                    if (!entityTypes.contains(typeURI))
                                    {
                                        entityTypes.add(typeURI);
                                        JSONArray nextEntityTypeJSONArray = new JSONArray();
                                        JSONObject nextEntityTypeJSON = new JSONObject();
                                        nextEntityTypeJSONArray.put(nextEntityTypeJSON);
                                        try
                                        {
                                            nextEntityTypeJSON.put(ENTITY_INDEX_ID_FIELD, typeURI);
                                            JSONObject occur = new JSONObject();
                                            occur.put("inc", 1);
                                            nextEntityTypeJSON.put(OCCURRENCES_FIELD, occur);

                                            // need to put the label of the type;taken as a suggestion in search ui
                                            String typeLabel = this.getTypeLiteral(typeURI);
                                            typeLabels.add(typeLabel);
                                            
                                            if(typeLabel.equalsIgnoreCase(PERSON_ENTITY_ATTRIBUTE_VALUE))
                                            {
                                                hasPersonTypeEntities = true;
                                                
                                            } else if(typeLabel.equalsIgnoreCase(ORGANIZATION_ENTITY_ATTRIBUTE_VALUE))
                                            {
                                                hasOrganizationTypeEntities = true;
                                                
                                            } else if(typeLabel.equalsIgnoreCase(PLACE_ENTITY_ATTRIBUTE_VALUE))
                                            {
                                                hasPlaceTypeEntities = true;
                                            }

                                            nextEntityTypeJSON.put(TYPE_FIELD, typeLabel);

                                            // Hierarchy : no hierarchy implementation in Stanbol
                                            // Collection<String> hierarchy =
                                            // helper.getHierarchy().get(typeURI);
                                            // nextEntityTypeJSON.put( HIERARCHY_FIELD, hierarchy);

                                            Collection<String> properties = entity.getProperties();
                                            nextEntityTypeJSON.put(ATTRIBUTES_FIELD, properties);

                                            entitiesTypesJSONs.add(nextEntityTypeJSONArray.toString());

                                        }
                                        catch (JSONException e)
                                        {
                                            Logging.agents.error("Error creating Entity Type Document with URI: " + typeURI, e);
                                            continue; // Continue with next Entity Type
                                        }

                                    }
                                }
                            }
                           
                            // adding the type to the entity object
                            nextEntityJSON.put(TYPE_FIELD, typeLabels);

                            // entity description
                            Collection<String> comments = entity.getComments();
                            nextEntityJSON.put(DESCRIPTION_FIELD, comments);

                            // entity depiction
                            Collection<String> depictions = entity.getPropertyValues(FOAF_NAMESPACE,
                                    DEPICTION_ENTITY_ATTRIBUTE, false);
                            nextEntityJSON.put(THUMBNAIL_FIELD, depictions);

                            // putting all the other entity properties to entity json, added to entity solr doc as dynamic
                            // attributes
                            for (String prop : entity.getProperties())
                            {
                                // List<String> propValues = entity.getPropertyValue(prop, false);
                                Collection<String> propValues = entity.getPropertyValues(prop);
                                // properties.putAll(prop, propValues);
                                if (!jsonFail)
                                {
                                    try
                                    {
                                        nextEntityJSON.put(prop, propValues);
                                    }
                                    catch (JSONException e)
                                    {
                                        Logging.agents.error(
                                                "Error creating Entity Document with URI: " + ea.getEntityReference(), e);
                                        jsonFail = true;
                                    }
                                }
                            }
                        }

                    }
                    catch (StanbolServiceException e)
                    {
                        Logging.agents.error(
                                "Error enhancing the document by retrieving the entity from Stanbol entityhub : "
                                        + documentURI, e);
                    }
                    catch (JSONException e)
                    {
                        Logging.agents.error(
                                "Error creating Entity Document with URI: " + ea.getEntityReference(), e);
                        jsonFail = true;
                    }

                    if (!jsonFail)
                    {
                        String entityJsonArray = nextEntityJSONArray.toString();
                        Logging.agents.debug("New entity object json : " + entityJsonArray);
                        entitiesJsons.add(entityJsonArray); // we need the array
                    }

                }

            }

            // Enrichment complete!
            // Add Entities JSONs
            docCopy.addField(ENTITIES_FIELD, entitiesJsons.toArray(new String[entitiesJsons.size()]));
            // Add Entities's types JSONs
            docCopy.addField(ENTITIES_TYPES_FIELD, entitiesTypesJSONs.toArray(new String[entitiesTypesJSONs.size()]));

            // // Add Semantic Metadata
            // these are flat fields/ no hierarchy
            docCopy.addField(SMLT_ENTITY_URI_FIELD, uris.toArray(new String[uris.size()]));
            docCopy.addField(SMLT_ENTITY_TYPES_FIELD, entityTypes.toArray(new String[entityTypes.size()]));
            
            //tagging the primary document whether it has person, org, place type entities
            docCopy.addField(PERSON_TYPE_FIELD, Boolean.toString(hasPersonTypeEntities));
            docCopy.addField(ORGANIZATION_TYPE_FIELD, Boolean.toString(hasOrganizationTypeEntities));
            docCopy.addField(PLACE_TYPE_FIELD, Boolean.toString(hasPlaceTypeEntities));
        }
        
        // Send new document downstream
        int rval = activities.sendDocument(documentURI, docCopy);
        resultCode = (rval == DOCUMENTSTATUS_ACCEPTED) ? "ACCEPTED" : "REJECTED";
        return rval;

    }

    /**
     * process the type uri and return the literal value
     * 
     * @param typeURI
     * @return
     */
    private String getTypeLiteral(String typeURI)
    {
        int startIndex = typeURI.lastIndexOf("/") + 1;
        String typeLabel = typeURI.substring(startIndex);
        return typeLabel;
    }

    /**
     * Obtain the name of the form check javascript method to call.
     * 
     * @param connectionSequenceNumber is the unique number of this connection within the job.
     * @return the name of the form check javascript method.
     */
    @Override
    public String getFormCheckJavascriptMethodName(int connectionSequenceNumber)
    {
        return "s" + connectionSequenceNumber + "_checkSpecification";
    }

    /**
     * Obtain the name of the form presave check javascript method to call.
     * 
     * @param connectionSequenceNumber is the unique number of this connection within the job.
     * @return the name of the form presave check javascript method.
     */
    @Override
    public String getFormPresaveCheckJavascriptMethodName(int connectionSequenceNumber)
    {
        return "s" + connectionSequenceNumber + "_checkSpecificationForSave";
    }

    /**
     * Output the specification header section. This method is called in the head section of a job page which has
     * selected a pipeline connection of the current type. Its purpose is to add the required tabs to the list, and to
     * output any javascript methods that might be needed by the job editing HTML.
     * 
     * @param out is the output to which any HTML should be sent.
     * @param locale is the preferred local of the output.
     * @param os is the current pipeline specification for this connection.
     * @param connectionSequenceNumber is the unique number of this connection within the job.
     * @param tabsArray is an array of tab names. Add to this array any tab names that are specific to the connector.
     */
    @Override
    public void outputSpecificationHeader(IHTTPOutput out, Locale locale, Specification os,
            int connectionSequenceNumber, List<String> tabsArray) throws ManifoldCFException, IOException
    {
        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("SEQNUM", Integer.toString(connectionSequenceNumber));

        tabsArray.add(Messages.getString(locale, "StanbolEnhancer.FieldMappingTabName"));
        fillInFieldMappingSpecificationMap(paramMap, os);
        Messages.outputResourceWithVelocity(out, locale, EDIT_SPECIFICATION_JS, paramMap);
    }

    /**
     * Output the specification body section. This method is called in the body section of a job page which has selected
     * a pipeline connection of the current type. Its purpose is to present the required form elements for editing. The
     * coder can presume that the HTML that is output from this configuration will be within appropriate <html>, <body>,
     * and <form> tags. The name of the form is "editjob".
     * 
     * @param out is the output to which any HTML should be sent.
     * @param locale is the preferred local of the output.
     * @param os is the current pipeline specification for this job.
     * @param connectionSequenceNumber is the unique number of this connection within the job.
     * @param actualSequenceNumber is the connection within the job that has currently been selected.
     * @param tabName is the current tab name.
     */
    @Override
    public void outputSpecificationBody(IHTTPOutput out, Locale locale, Specification os, int connectionSequenceNumber,
            int actualSequenceNumber, String tabName) throws ManifoldCFException, IOException
    {
        Map<String, Object> paramMap = new HashMap<String, Object>();

        // Set the tab name
        paramMap.put("TABNAME", tabName);
        paramMap.put("SEQNUM", Integer.toString(connectionSequenceNumber));
        paramMap.put("SELECTEDNUM", Integer.toString(actualSequenceNumber));

        // Fill in the field mapping tab data
        fillInFieldMappingSpecificationMap(paramMap, os);

        Messages.outputResourceWithVelocity(out, locale, EDIT_SPECIFICATION_FIELDMAPPING_HTML, paramMap);
    }

    /**
     * Process a specification post. This method is called at the start of job's edit or view page, whenever there is a
     * possibility that form data for a connection has been posted. Its purpose is to gather form information and modify
     * the transformation specification accordingly. The name of the posted form is "editjob".
     * 
     * @param variableContext contains the post data, including binary file-upload information.
     * @param locale is the preferred local of the output.
     * @param os is the current pipeline specification for this job.
     * @param connectionSequenceNumber is the unique number of this connection within the job.
     * @return null if all is well, or a string error message if there is an error that should prevent saving of the job
     *         (and cause a redirection to an error page).
     */
    @Override
    public String processSpecificationPost(IPostParameters variableContext, Locale locale, Specification os,
            int connectionSequenceNumber) throws ManifoldCFException
    {
        String seqPrefix = "s" + connectionSequenceNumber + "_";

        String stanbolURLValue = variableContext.getParameter(seqPrefix + "stanbol_url");
        if (stanbolURLValue == null || stanbolURLValue.equalsIgnoreCase(""))
            stanbolURLValue = STANBOL_ENDPOINT;

        SpecificationNode serverNode = new SpecificationNode(StanbolConfig.STANBOL_SERVER_VALUE);
        serverNode.setAttribute(StanbolConfig.ATTRIBUTE_VALUE, stanbolURLValue);
        os.addChild(os.getChildCount(), serverNode);

        String stanbolChainValue = variableContext.getParameter(seqPrefix + "stanbol_chain");
        if (stanbolChainValue == null || stanbolChainValue.equalsIgnoreCase(""))
            stanbolChainValue = STANBOL_ENHANCEMENT_CHAIN;

        SpecificationNode chainNode = new SpecificationNode(StanbolConfig.STANBOL_CHAIN_VALUE);
        chainNode.setAttribute(StanbolConfig.ATTRIBUTE_VALUE, stanbolChainValue);
        os.addChild(os.getChildCount(), chainNode);

        return null;
    }

    /**
     * View specification. This method is called in the body section of a job's view page. Its purpose is to present the
     * pipeline specification information to the user. The coder can presume that the HTML that is output from this
     * configuration will be within appropriate <html> and <body> tags.
     * 
     * @param out is the output to which any HTML should be sent.
     * @param locale is the preferred local of the output.
     * @param connectionSequenceNumber is the unique number of this connection within the job.
     * @param os is the current pipeline specification for this job.
     */
    @Override
    public void viewSpecification(IHTTPOutput out, Locale locale, Specification os, int connectionSequenceNumber)
            throws ManifoldCFException, IOException
    {
        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("SEQNUM", Integer.toString(connectionSequenceNumber));

        // Fill in the map with data from all tabs
        fillInFieldMappingSpecificationMap(paramMap, os);
        Messages.outputResourceWithVelocity(out, locale, VIEW_SPECIFICATION_HTML, paramMap);

    }

    protected static void fillInFieldMappingSpecificationMap(Map<String, Object> paramMap, Specification os)
    {
        for (int i = 0; i < os.getChildCount(); i++)
        {
            SpecificationNode sn = os.getChild(i);
            if (sn.getType().equals(StanbolConfig.STANBOL_SERVER_VALUE))
            {
                String server = sn.getAttributeValue(StanbolConfig.ATTRIBUTE_VALUE);
                paramMap.put("STANBOL_SERVER", server);

            }
            else if (sn.getType().equals(StanbolConfig.STANBOL_CHAIN_VALUE))
            {
                String chain = sn.getAttributeValue(StanbolConfig.ATTRIBUTE_VALUE);
                paramMap.put("STANBOL_CHAIN", chain);
            }
        }
    }

    protected static class SpecPacker
    {
        private final String stanbolServer;

        private final String stanbolChain;

        public SpecPacker(Specification os)
        {

            String serverURL = STANBOL_ENDPOINT;
            String stanbolChain = STANBOL_ENHANCEMENT_CHAIN;

            for (int i = 0; i < os.getChildCount(); i++)
            {
                SpecificationNode sn = os.getChild(i);

                if (sn.getType().equals(StanbolConfig.STANBOL_SERVER_VALUE))
                {
                    serverURL = sn.getAttributeValue(StanbolConfig.ATTRIBUTE_VALUE);

                }
                else if (sn.getType().equals(StanbolConfig.STANBOL_CHAIN_VALUE))
                {
                    stanbolChain = sn.getAttributeValue(StanbolConfig.ATTRIBUTE_VALUE);

                }

            }
            this.stanbolServer = serverURL;
            this.stanbolChain = stanbolChain;
        }

        public String toPackedString()
        {
            StringBuilder sb = new StringBuilder();
            if (stanbolServer != null)
            {
                sb.append('+');
                sb.append(stanbolServer);
            }
            else
            {
                sb.append('-');
            }
            if (stanbolServer != null)
            {
                sb.append('+');
                sb.append(stanbolServer);
            }
            else
            {
                sb.append('-');
            }

            return sb.toString();
        }

        public String getStanbolServer()
        {
            return stanbolServer;
        }

        public String getStanbolChain()
        {
            return stanbolChain;
        }

    }

}
