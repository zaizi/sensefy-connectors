package org.apache.manifoldcf.agents.transformation.stanbol;

/* $Id: TikaExtractor.java 1612814 2014-07-23 11:50:59Z kwright $ */

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
import org.apache.manifoldcf.agents.transformation.stanbol.util.EntityComparator;

import java.io.*;
import java.util.*;

import org.apache.stanbol.client.StanbolClient;
import org.apache.stanbol.client.enhancer.model.EnhancementResult;
import org.apache.stanbol.client.enhancer.model.EntityAnnotation;
import org.apache.stanbol.client.enhancer.model.TextAnnotation;
import org.apache.stanbol.client.entityhub.model.Entity;
import org.apache.stanbol.client.impl.StanbolClientImpl;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

/**
 * This connector works as a transformation connector, but does nothing other than logging.
 * 
 */
public class StanbolEnhancer extends org.apache.manifoldcf.agents.transformation.BaseTransformationConnector
{
    public static final String _rcsid = "@(#)$Id: StanbolEnhancer.java 2015-07-17 djayakody $";

    private static final String EDIT_SPECIFICATION_JS = "editSpecification.js";
    private static final String EDIT_SPECIFICATION_FIELDMAPPING_HTML = "editSpecification_FieldMapping.html";
    private static final String EDIT_SPECIFICATION_EXCEPTIONS_HTML = "editSpecification_Exceptions.html";
    private static final String VIEW_SPECIFICATION_HTML = "viewSpecification.html";
    private static final String STANBOL_ENDPOINT = "http://localhost:8080/";

    // private static final String FIELD_URIS = "entities_uris";

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
    static final String FIELD_URIS = "smlt_entities";

    public static final String TYPE_FIELD = "type";

    public static final String HIERARCHY_FIELD = "hierarchy";

    public static final String ATTRIBUTES_FIELD = "attributes";

    protected static final String ACTIVITY_ENHANCE = "enhance";

    public static final Collection<String> TYPES_BLACK_LIST = ImmutableSet.of(
            "http://rdf.freebase.com/ns/common.topic", "http://rdf.freebase.com/ns/base.ontologies.ontology_instance",
            "http://rdf.freebase.com/ns/user");

    protected static final String[] activitiesList = new String[] { ACTIVITY_ENHANCE };

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
        Logging.agents.info("Stanbol enhancer started..");
        long startTime = System.currentTimeMillis();
        String resultCode = "OK";
        String description = null;
        // sample content request
        String sample = "Testing Bruce Lee in America. Barak Obama is the president of USA.";

        // Extracting Content.
        long length = document.getBinaryLength();
        byte[] copy = IOUtils.toByteArray(document.getBinaryStream());
        String content = new String(copy);
        System.out.println("Starting to enhance content : " + content);

        Set<String> uris = Sets.newHashSet();
        SetMultimap<String, String> properties = HashMultimap.create();
        Collection<String> entityTypes = Sets.newHashSet();
        List<String> entitiesJsons = Lists.newArrayList();
        List<String> entitiesTypesJSONs = Lists.newArrayList();

        EnhancementResult enhancerResult = null;
        StanbolClient stanbolClient = null;
        
        
        //Create a copy of Repository Document       
        RepositoryDocument docCopy = document.duplicate();
        docCopy.setBinary(new ByteArrayInputStream(copy), length);
        //Add Entities as properties to doc
        docCopy.addField("hello", "world");

        try
        {
            stanbolClient = new StanbolClientImpl(STANBOL_ENDPOINT);
            Logging.agents.info("Going to enhance sample : " + sample);
            // enhancing content
            enhancerResult = stanbolClient.enhancer().enhance(null, sample);
            enhancerResult.disambiguate();

            for (TextAnnotation ta : enhancerResult.getTextAnnotations())
            {
                //Logging.agents.info("Selected context : " + ta.getSelectionContext());
                Logging.agents.info("Selected text : " + ta.getSelectedText());

                for (EntityAnnotation ea : enhancerResult.getEntityAnnotations(ta))
                {
                    JSONArray nextEntityJSONArray = new JSONArray();
                    JSONObject nextEntityJSON = new JSONObject();
                    nextEntityJSONArray.put(nextEntityJSON);
                    boolean jsonFail = false;

                    uris.add(ea.getEntityReference());
                    // Parent Document + Ocurrence Counter
                    try
                    {
                        JSONObject occur = new JSONObject();
                        occur.put("inc", 1);
                        nextEntityJSON.put(OCCURRENCES_FIELD, occur);

                        JSONObject parentURI = new JSONObject();
                        parentURI.put("add", documentURI);
                        nextEntityJSON.put(PARENT_URI_FIELD, parentURI);
                        nextEntityJSON.put(ENTITY_INDEX_ID_FIELD, ea.getEntityReference());

                    }
                    catch (JSONException e)
                    {
                        Logging.agents.error("Error creating Entity Document with URI: " + ea, e);
                        jsonFail = true;
                    }

                    Logging.agents.info("Entity confidence : " + ea.getConfidence() + "Entity label : "
                            + ea.getEntityLabel() + " ref : " + ea.getEntityReference());

                    Entity entity = stanbolClient.entityhub().lookup(ea.getEntityReference(), true);
                    List<String> labels = entity.getLabels();
                    Logging.agents.info("entity uri: " + entity.getUri());

                    for (String prop : entity.getProperties())
                    {
                        List<String> propValues = entity.getPropertyValue(prop, false);
                        properties.putAll(prop, propValues);
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

                        // Positions
                        if (!jsonFail)
                        {
                            try
                            {
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
                            }
                            catch (JSONException e)
                            {
                                Logging.agents.error(
                                        "Error creating Entity Document with URI: " + ea.getEntityReference(), e);
                                jsonFail = true;
                            }
                        }

                        if (!jsonFail)
                        {
                            entitiesJsons.add(nextEntityJSONArray.toString()); // we need the array
                        }

                    }

                }

            }

        }
        catch (Exception e)
        {
            Logging.agents.error("Error occurred while performing Stanbol enhancement");
            resultCode = "STANBOL_ENHANCEMENT_FAIL";
            description = e.getMessage();
            Logging.agents.error("Error enhancing the document  : " + documentURI, e);

        }
        finally
        {
            
            activities.recordActivity(new Long(startTime), ACTIVITY_ENHANCE, length, documentURI, resultCode,
                    description);
            
        }

        
        if (enhancerResult == null)
        {
            return DOCUMENTSTATUS_REJECTED; // TODO Make This Configurable
        }
        

        // Enrichment complete!
        // Add Entities JSONs
        //docCopy.addField(ENTITIES_FIELD, entitiesJsons.toArray(new String[entitiesJsons.size()]));

        // // Add Semantic Metadata
        // docCopy.addField(FIELD_URIS, uris.toArray(new String[uris.size()]));
        // for(String property:properties.keySet()){
        // Set<String> values = properties.get(property);
        // docCopy.addField(property,
        // values.toArray(new String[values.size()]));
        // }

        // docCopy.addField(SMLT_ENTITY_TYPES_FIELD,
        // entityTypes.toArray(new String[entityTypes.size()]));
        //
        // Add Entities's types JSONs
        // docCopy.addField(ENTITIES_TYPES_FIELD,
        // entitiesTypesJSONs.toArray(new String[entitiesTypesJSONs.size()]));


        // Send new document downstream
        int rval = activities.sendDocument(documentURI, docCopy);
        resultCode = (rval == DOCUMENTSTATUS_ACCEPTED) ? "ACCEPTED" : "REJECTED";
        return rval;

    }

    /**
     * not yet used..a local mechanism to disambiguate entitieannotations
     * 
     * @param entityAnnotations
     * @return
     */
    private EntityAnnotation getBestEntityAnnotation(List<EntityAnnotation> entityAnnotations)
    {

        EntityAnnotation[] entityAnnotationArray = entityAnnotations.toArray(new EntityAnnotation[entityAnnotations.size()]);
        Arrays.sort(entityAnnotationArray, new EntityComparator());
        return entityAnnotationArray[0];
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

        tabsArray.add(Messages.getString(locale, "TikaExtractor.FieldMappingTabName"));
        tabsArray.add(Messages.getString(locale, "TikaExtractor.ExceptionsTabName"));

        // Fill in the specification header map, using data from all tabs.
        fillInFieldMappingSpecificationMap(paramMap, os);
        fillInExceptionsSpecificationMap(paramMap, os);

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
        fillInExceptionsSpecificationMap(paramMap, os);

        Messages.outputResourceWithVelocity(out, locale, EDIT_SPECIFICATION_FIELDMAPPING_HTML, paramMap);
        Messages.outputResourceWithVelocity(out, locale, EDIT_SPECIFICATION_EXCEPTIONS_HTML, paramMap);
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

        String x;

        x = variableContext.getParameter(seqPrefix + "fieldmapping_count");
        if (x != null && x.length() > 0)
        {
            // About to gather the fieldmapping nodes, so get rid of the old ones.
            int i = 0;
            while (i < os.getChildCount())
            {
                SpecificationNode node = os.getChild(i);
                if (node.getType().equals(TikaConfig.NODE_FIELDMAP)
                        || node.getType().equals(TikaConfig.NODE_KEEPMETADATA))
                    os.removeChild(i);
                else
                    i++;
            }
            int count = Integer.parseInt(x);
            i = 0;
            while (i < count)
            {
                String prefix = seqPrefix + "fieldmapping_";
                String suffix = "_" + Integer.toString(i);
                String op = variableContext.getParameter(prefix + "op" + suffix);
                if (op == null || !op.equals("Delete"))
                {
                    // Gather the fieldmap etc.
                    String source = variableContext.getParameter(prefix + "source" + suffix);
                    String target = variableContext.getParameter(prefix + "target" + suffix);
                    if (target == null)
                        target = "";
                    SpecificationNode node = new SpecificationNode(TikaConfig.NODE_FIELDMAP);
                    node.setAttribute(TikaConfig.ATTRIBUTE_SOURCE, source);
                    node.setAttribute(TikaConfig.ATTRIBUTE_TARGET, target);
                    os.addChild(os.getChildCount(), node);
                }
                i++;
            }

            String addop = variableContext.getParameter(seqPrefix + "fieldmapping_op");
            if (addop != null && addop.equals("Add"))
            {
                String source = variableContext.getParameter(seqPrefix + "fieldmapping_source");
                String target = variableContext.getParameter(seqPrefix + "fieldmapping_target");
                if (target == null)
                    target = "";
                SpecificationNode node = new SpecificationNode(TikaConfig.NODE_FIELDMAP);
                node.setAttribute(TikaConfig.ATTRIBUTE_SOURCE, source);
                node.setAttribute(TikaConfig.ATTRIBUTE_TARGET, target);
                os.addChild(os.getChildCount(), node);
            }

            // Gather the keep all metadata parameter to be the last one
            SpecificationNode node = new SpecificationNode(TikaConfig.NODE_KEEPMETADATA);
            String keepAll = variableContext.getParameter(seqPrefix + "keepallmetadata");
            if (keepAll != null)
            {
                node.setAttribute(TikaConfig.ATTRIBUTE_VALUE, keepAll);
            }
            else
            {
                node.setAttribute(TikaConfig.ATTRIBUTE_VALUE, "false");
            }
            // Add the new keepallmetadata config parameter
            os.addChild(os.getChildCount(), node);
        }

        if (variableContext.getParameter(seqPrefix + "ignoretikaexceptions_present") != null)
        {
            int i = 0;
            while (i < os.getChildCount())
            {
                SpecificationNode node = os.getChild(i);
                if (node.getType().equals(TikaConfig.NODE_IGNORETIKAEXCEPTION))
                    os.removeChild(i);
                else
                    i++;
            }

            String value = variableContext.getParameter(seqPrefix + "ignoretikaexceptions");
            if (value == null)
                value = "false";

            SpecificationNode node = new SpecificationNode(TikaConfig.NODE_IGNORETIKAEXCEPTION);
            node.setAttribute(TikaConfig.ATTRIBUTE_VALUE, value);
            os.addChild(os.getChildCount(), node);
        }

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
        fillInExceptionsSpecificationMap(paramMap, os);

        Messages.outputResourceWithVelocity(out, locale, VIEW_SPECIFICATION_HTML, paramMap);

    }

    protected static void fillInFieldMappingSpecificationMap(Map<String, Object> paramMap, Specification os)
    {
        // Prep for field mappings
        List<Map<String, String>> fieldMappings = new ArrayList<Map<String, String>>();
        String keepAllMetadataValue = "true";
        for (int i = 0; i < os.getChildCount(); i++)
        {
            SpecificationNode sn = os.getChild(i);
            if (sn.getType().equals(TikaConfig.NODE_FIELDMAP))
            {
                String source = sn.getAttributeValue(TikaConfig.ATTRIBUTE_SOURCE);
                String target = sn.getAttributeValue(TikaConfig.ATTRIBUTE_TARGET);
                String targetDisplay;
                if (target == null)
                {
                    target = "";
                    targetDisplay = "(remove)";
                }
                else
                    targetDisplay = target;
                Map<String, String> fieldMapping = new HashMap<String, String>();
                fieldMapping.put("SOURCE", source);
                fieldMapping.put("TARGET", target);
                fieldMapping.put("TARGETDISPLAY", targetDisplay);
                fieldMappings.add(fieldMapping);
            }
            else if (sn.getType().equals(TikaConfig.NODE_KEEPMETADATA))
            {
                keepAllMetadataValue = sn.getAttributeValue(TikaConfig.ATTRIBUTE_VALUE);
            }
        }
        paramMap.put("FIELDMAPPINGS", fieldMappings);
        paramMap.put("KEEPALLMETADATA", keepAllMetadataValue);
    }

    protected static void fillInExceptionsSpecificationMap(Map<String, Object> paramMap, Specification os)
    {
        String ignoreTikaExceptions = "true";
        for (int i = 0; i < os.getChildCount(); i++)
        {
            SpecificationNode sn = os.getChild(i);
            if (sn.getType().equals(TikaConfig.NODE_IGNORETIKAEXCEPTION))
            {
                ignoreTikaExceptions = sn.getAttributeValue(TikaConfig.ATTRIBUTE_VALUE);
            }
        }
        paramMap.put("IGNORETIKAEXCEPTIONS", ignoreTikaExceptions);
    }

   

    
      protected static class SpecPacker
    {

        private final Map<String, String> sourceTargets = new HashMap<String, String>();
        private final boolean keepAllMetadata;
        private final boolean ignoreTikaException;

        public SpecPacker(Specification os)
        {
            boolean keepAllMetadata = true;
            boolean ignoreTikaException = true;
            for (int i = 0; i < os.getChildCount(); i++)
            {
                SpecificationNode sn = os.getChild(i);

                if (sn.getType().equals(TikaConfig.NODE_KEEPMETADATA))
                {
                    String value = sn.getAttributeValue(TikaConfig.ATTRIBUTE_VALUE);
                    keepAllMetadata = Boolean.parseBoolean(value);
                }
                else if (sn.getType().equals(TikaConfig.NODE_FIELDMAP))
                {
                    String source = sn.getAttributeValue(TikaConfig.ATTRIBUTE_SOURCE);
                    String target = sn.getAttributeValue(TikaConfig.ATTRIBUTE_TARGET);

                    if (target == null)
                    {
                        target = "";
                    }
                    sourceTargets.put(source, target);
                }
                else if (sn.getType().equals(TikaConfig.NODE_IGNORETIKAEXCEPTION))
                {
                    String value = sn.getAttributeValue(TikaConfig.ATTRIBUTE_VALUE);
                    ignoreTikaException = Boolean.parseBoolean(value);
                }
            }
            this.keepAllMetadata = keepAllMetadata;
            this.ignoreTikaException = ignoreTikaException;
        }

        public SpecPacker(String packedString)
        {

            int index = 0;

            // Mappings
            final List<String> packedMappings = new ArrayList<String>();
            index = unpackList(packedMappings, packedString, index, '+');
            String[] fixedList = new String[2];
            for (String packedMapping : packedMappings)
            {
                unpackFixedList(fixedList, packedMapping, 0, ':');
                sourceTargets.put(fixedList[0], fixedList[1]);
            }

            // Keep all metadata
            if (packedString.length() > index)
                keepAllMetadata = (packedString.charAt(index++) == '+');
            else
                keepAllMetadata = true;

            // Ignore tika exception
            if (packedString.length() > index)
                ignoreTikaException = (packedString.charAt(index++) == '+');
            else
                ignoreTikaException = true;

        }

        public String toPackedString()
        {
            StringBuilder sb = new StringBuilder();
            int i;

            // Mappings
            final String[] sortArray = new String[sourceTargets.size()];
            i = 0;
            for (String source : sourceTargets.keySet())
            {
                sortArray[i++] = source;
            }
            java.util.Arrays.sort(sortArray);

            List<String> packedMappings = new ArrayList<String>();
            String[] fixedList = new String[2];
            for (String source : sortArray)
            {
                String target = sourceTargets.get(source);
                StringBuilder localBuffer = new StringBuilder();
                fixedList[0] = source;
                fixedList[1] = target;
                packFixedList(localBuffer, fixedList, ':');
                packedMappings.add(localBuffer.toString());
            }
            packList(sb, packedMappings, '+');

            // Keep all metadata
            if (keepAllMetadata)
                sb.append('+');
            else
                sb.append('-');

            if (ignoreTikaException)
                sb.append('+');
            else
                sb.append('-');

            return sb.toString();
        }

        public String getMapping(String source)
        {
            return sourceTargets.get(source);
        }

        public boolean keepAllMetadata()
        {
            return keepAllMetadata;
        }

        public boolean ignoreTikaException()
        {
            return ignoreTikaException;
        }
    }

}
