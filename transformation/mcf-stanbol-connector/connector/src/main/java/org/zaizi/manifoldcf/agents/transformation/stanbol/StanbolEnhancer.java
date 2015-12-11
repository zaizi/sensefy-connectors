package org.zaizi.manifoldcf.agents.transformation.stanbol;

/**
 * (C) Copyright 2015 Zaizi Limited (http://www.zaizi.com).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 3.0 which accompanies this distribution, and is available at 
 * http://www.gnu.org/licenses/lgpl-3.0.en.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 **/

import org.apache.commons.io.IOUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.manifoldcf.core.interfaces.*;
import org.apache.manifoldcf.agents.interfaces.*;
import org.apache.manifoldcf.agents.system.Logging;
import org.apache.stanbol.client.Enhancer;
import org.apache.stanbol.client.StanbolClientFactory;
import org.apache.stanbol.client.enhancer.impl.EnhancerParameters;
import org.apache.stanbol.client.enhancer.model.EnhancementStructure;
import org.apache.stanbol.client.enhancer.model.EntityAnnotation;
import org.apache.stanbol.client.enhancer.model.TextAnnotation;
import org.apache.stanbol.client.entityhub.model.Entity;
import org.apache.stanbol.client.services.exception.StanbolServiceException;

import java.io.*;
import java.net.URI;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openrdf.model.impl.URIImpl;

/**
 * Stanbol Enhancer transformation connector
 * 
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
    static final String ENTITY_URI_FIELD = "entity_uris";

    protected static final String ACTIVITY_ENHANCE = "enhance";
    public static final double DEFAULT_DISAMBIGUATION_SCORE = 0.7;

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
        Logging.agents.info("Starting to enhance document content in Stanbol connector ");

        String resultCode = "OK";
        String description = null;

        SpecPacker sp = new SpecPacker(pipelineDescription.getSpecification());
        // stanbol server url
        String stanbolServer = sp.getStanbolServer();
        String chain = sp.getStanbolChain();

        // Stanbol entity dereference fields
        Map<String, String> derefFields = sp.getDereferenceFields();
        String ldPath = sp.getLDPath();

        stanbolFactory = new StanbolClientFactory(stanbolServer);

        // Extracting Content.
        long length = document.getBinaryLength();
        byte[] copy = IOUtils.toByteArray(document.getBinaryStream());
        String content = new String(copy);

        Set<String> uris = new HashSet<String>();
        Map<String, Set<String>> entityPropertyMap = new HashMap<String, Set<String>>();
        Enhancer enhancerClient = null;
        EnhancementStructure eRes = null;
        // Create a copy of Repository Document
        RepositoryDocument docCopy = document.duplicate();
        docCopy.setBinary(new ByteArrayInputStream(copy), length);

        try
        {
            enhancerClient = stanbolFactory.createEnhancerClient();
            EnhancerParameters parameters = null;
            // ldpath program is given priority if it's set
            if (ldPath != null)
            {
                parameters = EnhancerParameters.builder().setChain(chain).setContent(content).setLDpathProgram(ldPath).build();
            }
            else if (!derefFields.isEmpty())
            {
                parameters = EnhancerParameters.builder().setChain(chain).setContent(content).setDereferencingFields(
                        derefFields.keySet()).build();
            }
            else
            {
                parameters = EnhancerParameters.builder().setChain(chain).setContent(content).build();
            }
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
            return DOCUMENTSTATUS_REJECTED;
        }
        else
        {
            // processing the text annotations extracted by Stanbol
            for (TextAnnotation ta : eRes.getTextAnnotations())
            {
                Logging.agents.debug("Processing text annotation for content : " + ta.getUri());
                // need to disambiguate the entity-annotations returned
                for (EntityAnnotation ea : eRes.getEntityAnnotations(ta))
                {
                    double confidence = ea.getConfidence();
                    Logging.agents.debug("Processing entity annotation for content : " + ea.getUri() + " confidence : "
                            + confidence);
                    if (confidence < DEFAULT_DISAMBIGUATION_SCORE)
                    {
                        Logging.agents.debug("Confidence for the entity annotation is below the threshold, hence not processing the entity annotation");
                    }
                    else
                    {
                        Entity entity = null;
                        uris.add(ea.getEntityReference());
                        // dereference the entity
                        entity = ea.getDereferencedEntity();
                        if (entity != null)
                        {
                            Collection<String> properties = entity.getProperties();
                            for (String property : properties)
                            {
                                String targetFieldName = derefFields.get(property);
                                Set<String> propValues = entityPropertyMap.get(targetFieldName);

                                if (propValues == null)
                                {
                                    propValues = new HashSet<String>();
                                }
                                Collection<String> entityPropValues = entity.getPropertyValues(property);
                                propValues.addAll(entityPropValues);
                                entityPropertyMap.put(targetFieldName, propValues);
                            }
                        }
                    }
                }
            }

            // Enrichment complete!
            docCopy.addField(ENTITY_URI_FIELD, uris.toArray(new String[uris.size()]));
            // adding all entity properties and values into the document
            for (String property : entityPropertyMap.keySet())
            {
                Set<String> propertyValues = entityPropertyMap.get(property);
                docCopy.addField(property, propertyValues.toArray(new String[propertyValues.size()]));
            }
        }
        // Send new document downstream
        int rval = activities.sendDocument(documentURI, docCopy);
        resultCode = (rval == DOCUMENTSTATUS_ACCEPTED) ? "ACCEPTED" : "REJECTED";
        return rval;

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

        // added for attrib mappings
        String x;

        x = variableContext.getParameter(seqPrefix + "fieldmapping_count");
        if (x != null && x.length() > 0)
        {
            // About to gather the fieldmapping nodes, so get rid of the old ones.
            int i = 0;
            while (i < os.getChildCount())
            {
                SpecificationNode node = os.getChild(i);
                if (node.getType().equals(StanbolConfig.NODE_FIELDMAP)
                        || node.getType().equals(StanbolConfig.NODE_KEEPMETADATA))
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
                    {
                        target = source;
                    }

                    SpecificationNode node = new SpecificationNode(StanbolConfig.NODE_FIELDMAP);
                    node.setAttribute(StanbolConfig.ATTRIBUTE_SOURCE, source);
                    node.setAttribute(StanbolConfig.ATTRIBUTE_TARGET, target);
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
                {
                    target = source;
                }
                SpecificationNode node = new SpecificationNode(StanbolConfig.NODE_FIELDMAP);
                node.setAttribute(StanbolConfig.ATTRIBUTE_SOURCE, source);
                node.setAttribute(StanbolConfig.ATTRIBUTE_TARGET, target);
                os.addChild(os.getChildCount(), node);
            }
        }

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

        String stanbolLdpath = variableContext.getParameter(seqPrefix + "stanbol_ldpath");
        if (stanbolLdpath != null && !stanbolLdpath.equals(""))
        {
            SpecificationNode ldPathNode = new SpecificationNode(StanbolConfig.STANBOL_LDPATH);
            ldPathNode.setAttribute(StanbolConfig.ATTRIBUTE_VALUE, stanbolLdpath);
            os.addChild(os.getChildCount(), ldPathNode);
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
        Messages.outputResourceWithVelocity(out, locale, VIEW_SPECIFICATION_HTML, paramMap);

    }

    protected static void fillInFieldMappingSpecificationMap(Map<String, Object> paramMap, Specification os)
    {
        // Prep for field mappings
        List<Map<String, String>> fieldMappings = new ArrayList<Map<String, String>>();
        // adding default Stanbol parameters to the the map
        String server = STANBOL_ENDPOINT;
        String chain = STANBOL_ENHANCEMENT_CHAIN;
        String ldpath = "";

        for (int i = 0; i < os.getChildCount(); i++)
        {
            SpecificationNode sn = os.getChild(i);
            if (sn.getType().equals(StanbolConfig.NODE_FIELDMAP))
            {
                String source = sn.getAttributeValue(StanbolConfig.ATTRIBUTE_SOURCE);
                String target = sn.getAttributeValue(StanbolConfig.ATTRIBUTE_TARGET);
                String targetDisplay;
                if (target == null)
                {
                    // if destination field name is not given, the destination field name is the source field name by
                    // default
                    target = source;
                }
                targetDisplay = target;

                Map<String, String> fieldMapping = new HashMap<String, String>();
                fieldMapping.put("SOURCE", source);
                fieldMapping.put("TARGET", target);
                fieldMapping.put("TARGETDISPLAY", targetDisplay);
                fieldMappings.add(fieldMapping);
            }
            else if (sn.getType().equals(StanbolConfig.STANBOL_SERVER_VALUE))
            {
                server = sn.getAttributeValue(StanbolConfig.ATTRIBUTE_VALUE);
            }
            else if (sn.getType().equals(StanbolConfig.STANBOL_CHAIN_VALUE))
            {
                chain = sn.getAttributeValue(StanbolConfig.ATTRIBUTE_VALUE);

            }
            else if (sn.getType().equals(StanbolConfig.STANBOL_LDPATH))
            {
                ldpath = sn.getAttributeValue(StanbolConfig.ATTRIBUTE_VALUE);
            }
        }

        paramMap.put("STANBOL_SERVER", server);
        paramMap.put("STANBOL_CHAIN", chain);
        paramMap.put("STANBOL_LDPATH", ldpath);
        paramMap.put("FIELDMAPPINGS", fieldMappings);
    }

    protected static class SpecPacker
    {
        private final String stanbolServer;
        private final String stanbolChain;
        private final Map<String, String> dereferenceFields = new HashMap<String, String>();
        private final String ldpath;

        public SpecPacker(Specification os)
        {

            String serverURL = STANBOL_ENDPOINT;
            String stanbolChain = STANBOL_ENHANCEMENT_CHAIN;
            String ldpath = null;

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
                else if (sn.getType().equals(StanbolConfig.STANBOL_LDPATH))
                {
                    ldpath = sn.getAttributeValue(StanbolConfig.ATTRIBUTE_VALUE);

                }
                // adding deref fields
                else if (sn.getType().equals(StanbolConfig.NODE_FIELDMAP))
                {
                    String source = sn.getAttributeValue(StanbolConfig.ATTRIBUTE_SOURCE);
                    String target = sn.getAttributeValue(StanbolConfig.ATTRIBUTE_TARGET);
                    if (target == null)
                    {
                        // if destination field name is not given, the destination field name is the source field name
                        // by default
                        target = source;
                    }
                    dereferenceFields.put(source, target);

                }

            }
            this.stanbolServer = serverURL;
            this.stanbolChain = stanbolChain;
            this.ldpath = ldpath;

        }

        public String toPackedString()
        {
            StringBuilder sb = new StringBuilder();

            int i;
            // Mappings
            final String[] sortArray = new String[dereferenceFields.size()];
            i = 0;
            for (String field : dereferenceFields.keySet())
            {
                sortArray[i++] = field;
            }
            java.util.Arrays.sort(sortArray);

            List<String> packedMappings = new ArrayList<String>();
            String[] fixedList = new String[2];
            for (String source : sortArray)
            {
                String target = dereferenceFields.get(source);
                StringBuilder localBuffer = new StringBuilder();
                fixedList[0] = source;
                fixedList[1] = target;
                packFixedList(localBuffer, fixedList, ':');
                packedMappings.add(localBuffer.toString());
            }
            packList(sb, packedMappings, '+');

            if (ldpath != null)
            {
                sb.append('+');
                sb.append(ldpath);

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

        public Map<String, String> getDereferenceFields()
        {
            return dereferenceFields;
        }

        public String getLDPath()
        {
            return ldpath;
        }

    }

}
