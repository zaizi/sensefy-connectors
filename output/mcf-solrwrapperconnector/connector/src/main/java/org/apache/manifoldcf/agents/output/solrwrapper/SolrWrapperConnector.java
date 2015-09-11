/* $Id: NullConnector.java 988245 2010-08-23 18:39:35Z kwright $ */

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
package org.apache.manifoldcf.agents.output.solrwrapper;

import org.apache.commons.lang.StringUtils;
import org.apache.manifoldcf.agents.interfaces.IOutputAddActivity;
import org.apache.manifoldcf.agents.interfaces.IOutputConnection;
import org.apache.manifoldcf.agents.interfaces.IOutputConnectionManager;
import org.apache.manifoldcf.agents.interfaces.IOutputConnector;
import org.apache.manifoldcf.agents.interfaces.IOutputConnectorPool;
import org.apache.manifoldcf.agents.interfaces.IOutputNotifyActivity;
import org.apache.manifoldcf.agents.interfaces.IOutputRemoveActivity;
import org.apache.manifoldcf.agents.interfaces.OutputConnectionManagerFactory;
import org.apache.manifoldcf.agents.interfaces.OutputConnectorPoolFactory;
import org.apache.manifoldcf.agents.interfaces.OutputSpecification;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
import org.apache.manifoldcf.agents.output.BaseOutputConnector;
import org.apache.manifoldcf.agents.output.solrwrapper.activity.EmptyOutputAddActivity;
import org.apache.manifoldcf.agents.output.solrwrapper.utility.IndexNames;
import org.apache.manifoldcf.agents.output.solrwrapper.utility.JSONRepositoryDocumentSerializer;
import org.apache.manifoldcf.core.interfaces.ConfigParams;
import org.apache.manifoldcf.core.interfaces.IHTTPOutput;
import org.apache.manifoldcf.core.interfaces.IPostParameters;
import org.apache.manifoldcf.core.interfaces.IThreadContext;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.crawler.system.Logging;
import org.apache.manifoldcf.ui.beans.ThreadContext;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This is the SolrWrapper connector. This Indexer is able to recgnize different type of documents and send them to the
 * proper output connector.
 */
public class SolrWrapperConnector extends org.apache.manifoldcf.agents.output.BaseOutputConnector
{
    public static final String _rcsid = "@(#)$Id: SolrWrapperConnector.java 988245 2014-02-06 12:39:35Z aperez $";

    private final static String SOLRWRAPPER_TAB_PARAMETERS = "SolrWrapperConnector.Parameters";

    /**
     * Forward to the javascript to check the configuration parameters
     */
    private static final String EDIT_CONFIG_HEADER_FORWARD = "editConfiguration.js";

    /**
     * Forward to the HTML template to edit the configuration parameters
     */
    private static final String EDIT_CONFIG_FORWARD_PARAMETERS = "editConfiguration_Parameters.html";

    /**
     * Forward to the HTML template to view the configuration parameters
     */
    private static final String VIEW_CONFIG_FORWARD = "viewConfiguration.html";

    // Activities we log

    /**
     * Ingestion activity
     */
    public final static String INGEST_ACTIVITY = "document ingest";

    /**
     * Document removal activity
     */
    public final static String REMOVE_ACTIVITY = "document deletion";

    /**
     * Job notify activity
     */
    public final static String JOB_COMPLETE_ACTIVITY = "output notification";

    public static final String OCCURRENCES_FIELD = "occurrences";

    public static final String DOC_IDS_FIELD = "doc_ids";

    public static final String DOCUMENT_ID_FIELD = "id";

    public static final String SOLR_CONNECTOR_CLASS = "org.apache.manifoldcf.agents.output.solr.SolrConnector";

    public static final String PRIMARY_INDEX_SELECTION_FIELD = "primaryIndex";

    public static final String ENTITY_SELECTION_FIELD = "entityIndex";

    public static final String ENTITY_TYPE_SELECTION_FIELD = "entityTypeIndex";

    public static final String CONNECTORS_FIELD = "solrConnectors";

    public static final String PRIMARY_DOCUMENT_URI = "uri";

    public static final String SMLT_ENTITIES = "smlt_entities";

    private static final String SMLT_ENTITY_TYPES = "smlt_entity_types";

    /**
     * Constructor.
     */
    public SolrWrapperConnector()
    {
    }

    /**
     * Return the list of activities that this connector supports (i.e. writes into the log).
     * 
     * @return the list.
     */
    @Override
    public String[] getActivitiesList()
    {
        return new String[] { INGEST_ACTIVITY, REMOVE_ACTIVITY, JOB_COMPLETE_ACTIVITY };
    }

    /**
     * Connect.
     * 
     * @param configParameters is the set of configuration parameters, which in this case describe the target appliance,
     *            basic auth configuration, etc. (This formerly came out of the ini file.)
     */
    @Override
    public void connect(ConfigParams configParameters)
    {
        super.connect(configParameters);
    }

    /**
     * Close the connection. Call this before discarding the connection.
     */
    @Override
    public void disconnect() throws ManifoldCFException
    {
        super.disconnect();
    }

    /**
     * Set up a session
     */
    protected void getSession() throws ManifoldCFException, ServiceInterruption
    {
    }

    /**
     * Test the connection. Returns a string describing the connection integrity.
     * 
     * @return the connection's status as a displayable string.
     */
    @Override
    public String check() throws ManifoldCFException
    {
        try
        {
            StringBuilder result = new StringBuilder();
            getSession();
            Map<IndexNames, IOutputConnection> index2connector = this.getConnectors();
            for (IndexNames index : index2connector.keySet())
            {
                IOutputConnection outputConnection = index2connector.get(index);

                IOutputConnectorPool outputConnectorPool = OutputConnectorPoolFactory.make(super.currentContext);
                IOutputConnector outputConnector = outputConnectorPool.grab(outputConnection);

                result.append("[" + index + "]" + outputConnector.check() + ", ");

                outputConnectorPool.release(outputConnection, outputConnector);
            }
            return result.toString();
        }
        catch (ServiceInterruption e)
        {
            return "Service Interruption checking the connection : " + e.getMessage();
        }
    }

    /**
     * Get an output version string, given an output specification. The output version string is used to uniquely
     * describe the pertinent details of the output specification and the configuration, to allow the Connector
     * Framework to determine whether a document will need to be output again. Note that the contents of the document
     * cannot be considered by this method, and that a different version string (defined in IRepositoryConnector) is
     * used to describe the version of the actual document.
     * <p/>
     * This method presumes that the connector object has been configured, and it is thus able to communicate with the
     * output data store should that be necessary.
     * 
     * @param spec is the current output specification for the job that is doing the crawling.
     * @return a string, of unlimited length, which uniquely describes output configuration and specification in such a
     *         way that if two such strings are equal, the document will not need to be sent again to the output data
     *         store.
     */
    @Override
    public String getOutputDescription(OutputSpecification spec) throws ManifoldCFException, ServiceInterruption
    {
        String outputDescription = "";

        Map<IndexNames, IOutputConnection> index2connector = this.getConnectors();
        IOutputConnection iOutputConnection = index2connector.get(IndexNames.PRIMARY_INDEX);
        IOutputConnectorPool outputConnectorPool = OutputConnectorPoolFactory.make(super.currentContext);
        BaseOutputConnector outputConnector = (BaseOutputConnector) outputConnectorPool.grab(iOutputConnection);
        outputDescription = outputConnector.getOutputDescription(spec);
        outputConnectorPool.release(iOutputConnection, outputConnector);

        return outputDescription;
    }

    /**
     * Remove a document using the connector. Note that the last outputDescription is included, since it may be
     * necessary for the connector to use such information to know how to properly remove the document.
     * <p/>
     * --TO DO --
     * 
     * @param documentURI is the URI of the document. The URI is presumed to be the unique identifier which the output
     *            data store will use to process and serve the document. This URI is constructed by the repository
     *            connector which fetches the document, and is thus universal across all output connectors.
     * @param outputDescription is the last description string that was constructed for this document by the
     *            getOutputDescription() method above.
     * @param activities is the handle to an object that the implementer of an output connector may use to perform
     *            operations, such as logging processing activity.
     */
    @Override
    public void removeDocument(String documentURI, String outputDescription, IOutputRemoveActivity activities)
            throws ManifoldCFException, ServiceInterruption
    {
        Logging.connectors.info("SolrWrapper - Starting removing doc : " + documentURI);

        Map<IndexNames, IOutputConnection> index2connector = this.getConnectors();
        List<IndexNames> orderedIndexList = getOrderedIndexes();
        IOutputConnectorPool outputConnectorPool = null;
        outputConnectorPool = OutputConnectorPoolFactory.make(super.currentContext);
        // Primary Index Connector
        IOutputConnection primaryConnection = index2connector.get(IndexNames.PRIMARY_INDEX);
        BaseOutputConnector primaryConnector = (BaseOutputConnector) outputConnectorPool.grab(primaryConnection);

        for (IndexNames index : orderedIndexList)
        {
            IOutputConnection outputConnection = index2connector.get(index);
            BaseOutputConnector outputConnector = null;

            try
            {
                outputConnector = (BaseOutputConnector) outputConnectorPool.grab(outputConnection);
            }
            catch (ManifoldCFException e)
            {
                Logging.connectors.error("Error getting the connector for Index :" + index, e);
                continue;
            }

            switch (index)
            {
            case ENTITY_INDEX:
                removeChildren(documentURI, outputDescription, primaryConnector, outputConnector, SMLT_ENTITIES, true);
            case ENTITY_TYPE_INDEX:
                removeChildren(documentURI, outputDescription, primaryConnector, outputConnector, SMLT_ENTITY_TYPES,
                        false);

            }
            outputConnectorPool.release(outputConnection, outputConnector);

        }

        primaryConnector.removeDocument(documentURI, outputDescription, activities);
        outputConnectorPool.release(primaryConnection, primaryConnector);

        activities.recordActivity(null, REMOVE_ACTIVITY, null, documentURI, "OK", null);
    }

    /**
     * We have to proceed first with the removal of children and then with removal of primary document
     * 
     * @return
     */
    private List<IndexNames> getOrderedIndexes()
    {
        List<IndexNames> orderedIndexList = new ArrayList<IndexNames>();
        orderedIndexList.add(IndexNames.ENTITY_TYPE_INDEX);
        orderedIndexList.add(IndexNames.ENTITY_INDEX);
        return orderedIndexList;
    }

    /**
     * Removes the children of the current primaryDocument . To retrieve the children entities we use the primaryIndex
     * Connector as those information are in the primaryIndex independently of which index are we going to purge.
     * 
     * @param documentURI
     * @param outputDescription
     * @param outputConnector
     * @param childType
     */
    private void removeChildren(String documentURI, String outputDescription,
            BaseOutputConnector primaryIndexConnector, BaseOutputConnector outputConnector, String childType,
            boolean removeDocURI)
    {
        List<String> childrenEntities;
        childrenEntities = this.retrieveChildrenFromSolr(documentURI, primaryIndexConnector.getConfiguration(),
                childType);

        if (childrenEntities != null)
        {
            for (String currentId : childrenEntities)
            {
                try
                {
                    JSONArray removalUpdateJSON = JSONRepositoryDocumentSerializer.createAtomicRemovalJSON(currentId,
                            documentURI, removeDocURI);
                    RepositoryDocument removalUpdateRepoDoc = JSONRepositoryDocumentSerializer.createRepoDocFromJSON(
                            currentId, removalUpdateJSON.toString());

                    outputConnector.addOrReplaceDocument(currentId, outputDescription, removalUpdateRepoDoc,
                            "authorityNameString", new EmptyOutputAddActivity()); // check the authorityNameString
                }
                catch (Exception e)
                {
                    Logging.connectors.error("Error deleting child doc from : " + currentId, e);
                    continue;
                }
            }
        }
       
    }

    /**
     * Returns from the Entity core all the children entities of a specific primary document
     * 
     * ATTENTION : This can be improved accessing with GET only the documentURI doc, then retrieving or entity types or
     * entities ID ( that are there with SMLT fields)
     * 
     * @param documentURI
     * @param configuration
     * @return
     */
    private List<String> retrieveChildrenFromSolr(String documentURI, ConfigParams configuration, String field)
    {
        QueryResponse getResponse;
        SolrQuery getQuery;
        List<String> childrenIds = new ArrayList<String>();
        SolrDocumentList results = new SolrDocumentList();

        HttpSolrServer solrServer = getHttpSolrServer(configuration);

        getQuery = new SolrQuery();
        getQuery.setRequestHandler("/get");
        getQuery.set("ids", documentURI);

        try
        {
            getResponse = solrServer.query(getQuery);
            results = getResponse.getResults();
            if (results.size() > 0)
            {
                SolrDocument d = results.get(0);
                childrenIds = (List<String>) d.getFieldValue(field);
            }

        }
        catch (SolrServerException e)
        {
            Logging.connectors.error("Error retrieving children for : " + documentURI, e);

        }
        finally
        {
            return childrenIds;
        }

    }

    /**
     * Return the Solr Server instance from the configurations of a Connector
     * 
     * @param configuration
     * @return
     */
    private HttpSolrServer getHttpSolrServer(ConfigParams configuration)
    {
        String serverProtocol = configuration.getParameter("Server protocol");
        String serverName = configuration.getParameter("Server name");
        String serverCore = configuration.getParameter("Solr core name");
        String serverPort = configuration.getParameter("Server port");
        String webapp = configuration.getParameter("Server web application");

        String httpSolrServerUrl = serverProtocol + "://" + serverName + ":" + serverPort + "/" + webapp + "/"
                + serverCore;
        return new HttpSolrServer(httpSolrServerUrl);
    }

    /**
     * This special add or replace with work on a list of OutputConnectors, delivering the right document in the map to
     * the right index.
     * 
     * @param documentURI is the URI of the document. The URI is presumed to be the unique identifier which the output
     *            data store will use to process and serve the document. This URI is constructed by the repository
     *            connector which fetches the document, and is thus universal across all output connectors.
     * @param outputDescription is the description string that was constructed for this document by the
     *            getOutputDescription() method.
     * @param document is the document data to be processed (handed to the output data store).
     * @param authorityNameString is the name of the authority responsible for authorizing any access tokens passed in
     *            with the repository document. May be null.
     * @param activities is the handle to an object that the implementer of an output connector may use to perform
     *            operations, such as logging processing activity.
     * @return the document status (accepted or permanently rejected).
     */
    @Override
    public int addOrReplaceDocument(String documentURI, String outputDescription, RepositoryDocument document,
            String authorityNameString, IOutputAddActivity activities) throws ManifoldCFException, ServiceInterruption
    {
        boolean documentAcceptance = true;
        Logging.connectors.info("SolrWrapper - Starting Semantic Info Indexing" + documentURI);
        Map<IndexNames, IOutputConnection> index2connector = this.getConnectors();
        Map<IndexNames, List<RepositoryDocument>> index2documents = JSONRepositoryDocumentSerializer.parseJSONChildStructures(
                documentURI, document);

        for (IndexNames index : index2connector.keySet())
        {
            IOutputConnection outputConnection = index2connector.get(index);

            IOutputConnectorPool outputConnectorPool = null;
            BaseOutputConnector outputConnector = null;
            try
            {
                outputConnectorPool = OutputConnectorPoolFactory.make(super.currentContext);
                outputConnector = (BaseOutputConnector) outputConnectorPool.grab(outputConnection);
            }
            catch (ManifoldCFException e)
            {
                Logging.connectors.error("Error getting the connector for Index :" + index, e);
                continue;
            }

            List<RepositoryDocument> repositoryDocuments = index2documents.get(index);
            
            //check if the document being indexed is the primary document if yes, 
            //send it with the documentURI passed to SolrWrapper as id
            if (index == IndexNames.PRIMARY_INDEX)
            {
                if (repositoryDocuments != null)
                {
                    RepositoryDocument d = repositoryDocuments.get(0);
                    try
                    {
                        Logging.connectors.info("\n the repo doc being proccessed by Solrwrapper : " + documentURI);
                        int acceptance = outputConnector.addOrReplaceDocument(documentURI, outputDescription, d,
                                authorityNameString, activities);
                        if (acceptance == DOCUMENTSTATUS_REJECTED)
                            Logging.connectors.error("Error Ingesting Child Document : Rejected 468- " + documentURI);
                        else
                            Logging.connectors.info(" Child Document : Accepted - " + documentURI);
                    }
                    catch (Exception e)
                    {
                        Logging.connectors.error("Error in adding documents in the Index :  " + index
                                + " for Parent Document :" + documentURI, e);
                        continue;
                    }

                }
            }
            else
            {//documents from entity,entityType indexes
                if (repositoryDocuments != null)
                {
                    for (RepositoryDocument d : repositoryDocuments)
                    {
                        try
                        {
                            String id = "";
                            if (d.getFieldAsStrings(DOCUMENT_ID_FIELD) != null)
                                id = d.getFieldAsStrings(DOCUMENT_ID_FIELD)[0];
                            Logging.connectors.info("\n the repo doc being proccessed by Solrwrapper : " + id);
                            int acceptance = outputConnector.addOrReplaceDocument(id, outputDescription, d,
                                    authorityNameString, activities);
                            if (acceptance == DOCUMENTSTATUS_REJECTED)
                                Logging.connectors.error("Error Ingesting Child Document : Rejected 468- " + id);
                            else
                                Logging.connectors.info(" Child Document : Accepted - " + id);
                        }
                        catch (Exception e)
                        {
                            Logging.connectors.error("Error in adding documents in the Index :  " + index
                                    + " for Parent Document :" + documentURI, e);
                            continue;
                        }
                    }
                }
            }
            
            outputConnectorPool.release(outputConnection, outputConnector);
        }
        activities.recordActivity(null, INGEST_ACTIVITY, new Long(document.getBinaryLength()), documentURI, "OK", null);
        return DOCUMENTSTATUS_ACCEPTED;
    }

    /**
     * Notify the connector of a completed job. This is meant to allow the connector to flush any internal data
     * structures it has been keeping around, or to tell the output repository that this is a good time to synchronize
     * things. It is called whenever a job is either completed or aborted.
     * 
     * @param activities is the handle to an object that the implementer of an output connector may use to perform
     *            operations, such as logging processing activity.
     */
    @Override
    public void noteJobComplete(IOutputNotifyActivity activities) throws ManifoldCFException, ServiceInterruption
    {

        Map<IndexNames, IOutputConnection> index2connector = this.getConnectors();
        for (IndexNames index : index2connector.keySet())
        {
            IOutputConnection outputConnection = index2connector.get(index);

            IOutputConnectorPool outputConnectorPool = OutputConnectorPoolFactory.make(super.currentContext);
            IOutputConnector outputConnector = outputConnectorPool.grab(outputConnection);
            outputConnector.noteJobComplete(activities);
            try
            {
                if (index != IndexNames.PRIMARY_INDEX)
                    this.removeEmptyOccurrences(outputConnector.getConfiguration());
            }
            catch (Exception e)
            {
                Logging.connectors.error("Error pruning the index from 0 occurrences children", e);
            }

            outputConnectorPool.release(outputConnection, outputConnector);
        }
        // remove 0 occurrences entities and entity types
        activities.recordActivity(null, JOB_COMPLETE_ACTIVITY, null, "", "OK", null);
    }

    /**
     * Simply run a pruning query removing Solr Documents with 0 occurrences
     * 
     * @param configuration
     * @throws IOException
     * @throws SolrServerException
     */
    private void removeEmptyOccurrences(ConfigParams configuration) throws IOException, SolrServerException
    {
        HttpSolrServer httpSolrServer = this.getHttpSolrServer(configuration);
        httpSolrServer.deleteByQuery(OCCURRENCES_FIELD + ":" + 0);
        httpSolrServer.commit();
    }

    @Override
    public void outputConfigurationHeader(IThreadContext threadContext, IHTTPOutput out, Locale locale,
            ConfigParams parameters, List<String> tabsArray) throws ManifoldCFException, IOException
    {
        tabsArray.add(Messages.getString(locale, SOLRWRAPPER_TAB_PARAMETERS));
        Map<String, String> paramMap = new HashMap<String, String>();
        // Fill in parameters for all tabs
        fillInServerParameters(paramMap, parameters);

        outputResource(EDIT_CONFIG_HEADER_FORWARD, out, locale, paramMap, null);
    }

    /**
     * Output the configuration body section. This method is called in the body section of the connector's configuration
     * page. Its purpose is to present the required form elements for editing. The coder can presume that the HTML that
     * is output from this configuration will be within appropriate <html>, <body>, and <form> tags. The name of the
     * form is "editconnection".
     * 
     * @param threadContext is the local thread context.
     * @param out is the output to which any HTML should be sent.
     * @param parameters are the configuration parameters, as they currently exist, for this connection being
     *            configured.
     * @param tabName is the current tab name.
     */
    @Override
    public void outputConfigurationBody(IThreadContext threadContext, IHTTPOutput out, Locale locale,
            ConfigParams parameters, String tabName) throws ManifoldCFException, IOException
    {
        // Do the Server tab
        Map<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("TabName", tabName);
        String[] solrConnectorsArray = this.getSolrOutputConnectors();
        String solrConnectors = StringUtils.join(solrConnectorsArray, ",");
        paramMap.put(CONNECTORS_FIELD, solrConnectors);
        fillInServerParameters(paramMap, parameters);
        outputResource(EDIT_CONFIG_FORWARD_PARAMETERS, out, locale, paramMap, tabName);
    }

    /**
     * Read the content of a resource, replace the variable ${PARAMNAME} with the value and copy it to the out.
     * 
     * @param resName
     * @param out
     * @throws ManifoldCFException
     */
    private static void outputResource(String resName, IHTTPOutput out, Locale locale, Map<String, String> paramMap,
            String tabName) throws ManifoldCFException
    {
        Messages.outputResourceWithVelocity(out, locale, resName, paramMap, true);
    }

    @Override
    public String processConfigurationPost(IThreadContext threadContext, IPostParameters variableContext,
            Locale locale, ConfigParams parameters) throws ManifoldCFException
    {
        String[] solrConnectorsArray = this.getSolrOutputConnectors();
        parameters.setParameter(PRIMARY_INDEX_SELECTION_FIELD,
                variableContext.getParameter(PRIMARY_INDEX_SELECTION_FIELD));
        parameters.setParameter(ENTITY_SELECTION_FIELD, variableContext.getParameter(ENTITY_SELECTION_FIELD));
        parameters.setParameter(ENTITY_TYPE_SELECTION_FIELD, variableContext.getParameter(ENTITY_TYPE_SELECTION_FIELD));
        super.params = parameters;
        return null;
    }

    /**
     * Fill in Velocity parameters for the Server tab. List of Solr Connectors merged 3 selections
     */
    private static void fillInServerParameters(Map<String, String> paramMap, ConfigParams parameters)
    {
        paramMap.put(PRIMARY_INDEX_SELECTION_FIELD, (parameters.getParameter(PRIMARY_INDEX_SELECTION_FIELD)));
        paramMap.put(ENTITY_SELECTION_FIELD, (parameters.getParameter(ENTITY_SELECTION_FIELD)));
        paramMap.put(ENTITY_TYPE_SELECTION_FIELD, (parameters.getParameter(ENTITY_TYPE_SELECTION_FIELD)));
    }

    /**
     * View configuration. This method is called in the body section of the connector's view configuration page. Its
     * purpose is to present the connection information to the user. The coder can presume that the HTML that is output
     * from this configuration will be within appropriate <html> and <body> tags.
     * 
     * @param threadContext is the local thread context.
     * @param out is the output to which any HTML should be sent.
     * @param parameters are the configuration parameters, as they currently exist, for this connection being
     *            configured.
     */
    @Override
    public void viewConfiguration(IThreadContext threadContext, IHTTPOutput out, Locale locale, ConfigParams parameters)
            throws ManifoldCFException, IOException
    {

        Map<String, String> paramMap = new HashMap<String, String>();

        // Server tab
        fillInServerParameters(paramMap, parameters);

        outputResource(VIEW_CONFIG_FORWARD, out, locale, paramMap, null);
    }

    /**
     * return all the names for Solr Connections . It's a utility tool for the UI
     * 
     * @return
     */
    private String[] getSolrOutputConnectors()
    {
        IOutputConnectionManager manager;
        IThreadContext tc = new ThreadContext().getThreadContext();
        try
        {
            manager = OutputConnectionManagerFactory.make(tc);
            String[] connectionsForConnector = manager.findConnectionsForConnector(SOLR_CONNECTOR_CLASS);

            return connectionsForConnector;
        }
        catch (ManifoldCFException e)
        {
            Logging.connectors.error("Exception retrieving the Solr Connections : ", e);
        }

        return new String[0];
    }

    /**
     * Returns a the Map relating each index to a specific Solr Connector . It assumes the connector has already been
     * configured .
     * 
     * @return
     */
    private Map<IndexNames, IOutputConnection> getConnectors()
    {
        Map<IndexNames, IOutputConnection> index2connector = new HashMap<IndexNames, IOutputConnection>();
        String primaryIndexSelection = this.params.getParameter(PRIMARY_INDEX_SELECTION_FIELD);
        String entityIndexSelection = this.params.getParameter(ENTITY_SELECTION_FIELD);
        String entityTypeIndexSelection = this.params.getParameter(ENTITY_TYPE_SELECTION_FIELD);

        index2connector.put(IndexNames.PRIMARY_INDEX, getOutputConnector(primaryIndexSelection));
        index2connector.put(IndexNames.ENTITY_INDEX, getOutputConnector(entityIndexSelection));
        index2connector.put(IndexNames.ENTITY_TYPE_INDEX, getOutputConnector(entityTypeIndexSelection));

        return index2connector;
    }

    private IOutputConnection getOutputConnector(String name)
    {
        IOutputConnectionManager manager;
        IThreadContext tc = super.currentContext;
        try
        {
            manager = OutputConnectionManagerFactory.make(tc);
            if (!manager.checkConnectorExists(name))
            {
                return null;
            }

            IOutputConnection con = manager.load(name);
            /*
             * IOutputConnectorPool p = OutputConnectorPoolFactory.make(tc); IOutputConnector oc = p.grab(con);
             */
            return con;
        }
        catch (ManifoldCFException e)
        {
            Logging.connectors.error("Error retrieving OutputConnectors : ", e);
        }

        return null;

    }
}
