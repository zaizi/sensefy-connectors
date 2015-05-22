/* $Id: DropboxRepositoryConnector.java 1603259 2014-06-17 19:10:18Z kwright $ */

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

package org.apache.manifoldcf.crawler.connectors.box;

import com.box.sdk.*;
import org.apache.commons.lang.StringUtils;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
import org.apache.manifoldcf.core.interfaces.*;
import org.apache.manifoldcf.crawler.connectors.BaseRepositoryConnector;
import org.apache.manifoldcf.crawler.interfaces.DocumentSpecification;
import org.apache.manifoldcf.crawler.interfaces.IProcessActivity;
import org.apache.manifoldcf.crawler.interfaces.ISeedingActivity;
import org.apache.manifoldcf.crawler.interfaces.IVersionActivity;
import org.apache.manifoldcf.crawler.system.Logging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Box.com Repo connection
 *
 * @author Alessandro Benedetti
 *         14/04/2015
 *         mcf-box-connector
 */
public class BoxRepositoryConnector
        extends BaseRepositoryConnector {

    protected final static String ACTIVITY_READ = "read document";

    public final static String ACTIVITY_FETCH = "fetch";

    protected static final String RELATIONSHIP_CHILD = "child";

    /**
     * Deny access token for default authority
     */
    private final static String DEFAULT_DEAD_AUTH = "DEAD_AUTHORITY";

    // Nodes and attributes
    private static final String JOB_STARTPOINT_NODE_TYPE = "startpoint";
    private static final String JOB_PATH_ATTRIBUTE = "path";

    // Tab properties
    private static final String BOX_SERVER_TAB_PROPERTY = "BoxRepositoryConnector.Server";
    private static final String BOX_PATH_TAB_PROPERTY = "BoxRepositoryConnector.BoxPath";

    /**
     * UI templates and javascript
     */
    private static final String EDIT_CONFIG_HEADER_FORWARD = "editConfiguration.js";
    private static final String EDIT_CONFIG_FORWARD_SERVER = "editConfiguration_Server.html";
    private static final String VIEW_CONFIG_FORWARD = "viewConfiguration.html";
    private static final String EDIT_SPEC_HEADER_FORWARD = "editSpecification.js";
    private static final String EDIT_SPEC_FORWARD_DROPBOXPATH = "editSpecification_BoxPath.html";
    private static final String VIEW_SPEC_FORWARD = "viewSpecification.html";
    /**
     * Box Specific metadata
     */
    public static final String BOX_CREATION_DATE = "box_creation_date";
    public static final String BOX_LAST_MODIFIED = "box_last_modified";
    public static final String BOX_CREATOR = "box_creator";
    public static final String BOX_LAST_MODIFIER = "box_last_modifier";
    public static final String BOX_DESCRIPTION = "description";
    public static final String PATH = "path";
    public static final String TAG = "tag";
    public static final String[] supportedMetadata = new String[]{"id", "name", "description",
            "size", "path_collection", "created_at", "modified_at", "content_created_at",
            "content_modified_at", "created_by", "modified_by", "owned_by", "shared_link", "tags", "parent"};
    /**
     * Box Permission types
     */
    public static final String USER_PERMISSION_TYPE = "user";
    public static final String GROUP_PERMISSION_TYPE = "group";
    public static final String URL = "url";
    public static final String BOX_FOLDER_PATH = "boxpath";
    public static final String BOX_BASE_ENDPOINT = "https://app.box.com";
    public static final String[] BOX_READER_ROLES = new String[]{"VIEWER", "EDITOR", "VIEWER_UPLOADER", "CO_OWNER", "OWNER"};

    /**
     * Endpoint server name
     */
    protected String server = "box";

    protected BoxSession session ;

    protected long lastSessionFetch = -1L;

    protected static final long timeToRelease = 300000L;

    protected String clientId = null;

    protected String clientSecret = null;

    protected String username = null;

    protected String password = null;

    public BoxRepositoryConnector() {
        super();
    }

    /**
     * Return the list of activities that this connector supports (i.e. writes
     * into the log).
     *
     * @return the list.
     */
    @Override
    public String[] getActivitiesList() {
        return new String[]{ACTIVITY_FETCH, ACTIVITY_READ};
    }

    /**
     * Get the bin name strings for a document identifier. The bin name
     * describes the queue to which the document will be assigned for throttling
     * purposes. Throttling controls the rate at which items in a given queue
     * are fetched; it does not say anything about the overall fetch rate, which
     * may operate on multiple queues or bins. For example, if you implement a
     * web crawler, a good choice of bin name would be the server name, since
     * that is likely to correspond to a real resource that will need real
     * throttle protection.
     *
     * @param documentIdentifier is the document identifier.
     * @return the set of bin names. If an empty array is returned, it is
     * equivalent to there being no request rate throttling available for this
     * identifier.
     */
    @Override
    public String[] getBinNames(String documentIdentifier) {
        return new String[]{server};
    }

    /**
     * Close the connection. Call this before discarding the connection.
     */
    @Override
    public void disconnect()
            throws ManifoldCFException {
        if (session != null) {
            session.close();
            session = null;
            lastSessionFetch = -1L;
        }
        this.clientId = null;
        this.clientSecret = null;
        this.username = null;
        this.password = null;
    }

    /**
     * This method create a new BOX session for a BOX repository.
     *
     * @param configParams is the set of configuration parameters which allows you to connect to the Box service
     */
    @Override
    public void connect(ConfigParams configParams) {
        super.connect(configParams);
        clientId = params.getParameter(BoxConfig.CLIENT_ID);
        clientSecret = params.getObfuscatedParameter(BoxConfig.CLIENT_SECRET);
        username = params.getParameter(BoxConfig.USERNAME);
        password = params.getObfuscatedParameter(BoxConfig.PASSWORD);
        try {
            getSession();
        } catch (BoxAPIException e) {
            Logging.connectors.error("Connection Failed ", e);
        }catch (ManifoldCFException e) {
            Logging.connectors.error("Connection Failed ", e);
        } catch (RuntimeException e) {
            Logging.connectors.error("Connection Failed ", e);
        }
    }

    /**
     * The authentication must be completely revised according to manifoldCF multi threading standards and Box
     *
     * @return the connection's status as a displayable string.
     */
    @Override
    public String check()
            throws ManifoldCFException {
        try {
            getSession();
            return super.check();
        } catch (BoxAPIException e) {
            Logging.connectors.error("Connection Failed ", e);
            return "Connection failed: " + e.getMessage();
        }catch (ManifoldCFException e) {
            Logging.connectors.error("Connection Failed ", e);
            Throwable cause = e.getCause();
            return "Connection failed: " + cause.getMessage();
        } catch (RuntimeException e) {
            Logging.connectors.error("Connection Failed ", e);
            return "Connection failed: " + e.getMessage();
        }
    }

    /**
     * Set up a session
     */
    protected void getSession()
            throws ManifoldCFException {
        if (session == null) {
            if (StringUtils.isEmpty(clientId))
                throw new ManifoldCFException("Parameter " + BoxConfig.CLIENT_ID + " required but not set");
            if (StringUtils.isEmpty(clientSecret))
                throw new ManifoldCFException("Parameter " + BoxConfig.CLIENT_SECRET + " required but not set");
            if (StringUtils.isEmpty(username))
                throw new ManifoldCFException("Parameter " + BoxConfig.USERNAME + " required but not set");
            if (StringUtils.isEmpty(password))
                throw new ManifoldCFException("Parameter " + BoxConfig.PASSWORD + " required but not set");

            try {
                session = new BoxSession(clientId, clientSecret, username, password);
            } catch (RuntimeException e) {
                throw new ManifoldCFException("[Box Session] Manifold Exception : ",e);
            } catch (URISyntaxException e) {
                throw new ManifoldCFException("[Box Session] Wrong URI :",e);
            } catch (IOException e) {
                throw new ManifoldCFException("[Box Session] Not possible to access Box API:",e);
            }
            lastSessionFetch = System.currentTimeMillis();
        }
    }

    @Override
    public void poll()
            throws ManifoldCFException {
        if (lastSessionFetch == -1L) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime >= lastSessionFetch + timeToRelease) {
            session.close();
            session = null;
            lastSessionFetch = -1L;
        }

    }

    /**
     * This method is called to assess whether to count this connector instance should
     * actually be counted as being connected.
     *
     * @return true if the connector instance is actually connected.
     */
    @Override
    public boolean isConnected() {
        return session != null;
    }

    /**
     * Get the maximum number of documents to amalgamate together into one
     * batch, for this connector.
     *
     * @return the maximum number. 0 indicates "unlimited".
     */
    @Override
    public int getMaxDocumentRequest() {
        return 1;
    }

    /**
     * Return the list of relationship types that this connector recognizes.
     *
     * @return the list.
     */
    @Override
    public String[] getRelationshipTypes() {
        return new String[]{RELATIONSHIP_CHILD};
    }

    /**
     * Fill in a Server tab configuration parameter map for calling a Velocity
     * template.
     *
     * @param newMap     is the map to fill in
     * @param parameters is the current set of configuration parameters
     */
    private static void fillInServerConfigurationMap(Map<String, Object> newMap, IPasswordMapperActivity mapper,
                                                     ConfigParams parameters) {

        String clientId = parameters.getParameter(BoxConfig.CLIENT_ID);
        String clientSecret = parameters.getObfuscatedParameter(BoxConfig.CLIENT_SECRET);
        String username = parameters.getParameter(BoxConfig.USERNAME);
        String password = parameters.getObfuscatedParameter(BoxConfig.PASSWORD);


        if (clientId == null)
            clientId = StringUtils.EMPTY;
        if (clientSecret == null)
            clientSecret = StringUtils.EMPTY;
        else
            clientSecret = mapper.mapPasswordToKey(clientSecret);
        if (username == null)
            username = StringUtils.EMPTY;
        if (password == null)
            password = StringUtils.EMPTY;

        newMap.put("CLIENT_ID", clientId);
        newMap.put("CLIENT_SECRET", clientSecret);
        newMap.put("USERNAME", username);
        newMap.put("PASSWORD", password);

    }

    /**
     * View configuration. This method is called in the body section of the
     * connector's view configuration page. Its purpose is to present the
     * connection information to the user. The coder can presume that the HTML
     * that is output from this configuration will be within appropriate <html>
     * and <body> tags.
     *
     * @param threadContext is the local thread context.
     * @param out           is the output to which any HTML should be sent.
     * @param parameters    are the configuration parameters, as they currently
     *                      exist, for this connection being configured.
     */
    @Override
    public void viewConfiguration(IThreadContext threadContext, IHTTPOutput out, Locale locale,
                                  ConfigParams parameters)
            throws ManifoldCFException, IOException {
        Map<String, Object> paramMap = new HashMap<String, Object>();

        // Fill in map from each tab
        fillInServerConfigurationMap(paramMap, out, parameters);

        Messages.outputResourceWithVelocity(out, locale, VIEW_CONFIG_FORWARD, paramMap);
    }

    /**
     * Output the configuration header section. This method is called in the
     * head section of the connector's configuration page. Its purpose is to add
     * the required tabs to the list, and to output any javascript methods that
     * might be needed by the configuration editing HTML.
     *
     * @param threadContext is the local thread context.
     * @param out           is the output to which any HTML should be sent.
     * @param parameters    are the configuration parameters, as they currently
     *                      exist, for this connection being configured.
     * @param tabsArray     is an array of tab names. Add to this array any tab
     *                      names that are specific to the connector.
     */
    @Override
    public void outputConfigurationHeader(IThreadContext threadContext, IHTTPOutput out, Locale locale,
                                          ConfigParams parameters, List<String> tabsArray)
            throws ManifoldCFException, IOException {
        // Add the Server tab
        tabsArray.add(Messages.getString(locale, BOX_SERVER_TAB_PROPERTY));

        // Map the parameters
        Map<String, Object> paramMap = new HashMap<String, Object>();

        // Fill in the parameters from each tab
        fillInServerConfigurationMap(paramMap, out, parameters);

        // Output the Javascript - only one Velocity template for all tabs
        Messages.outputResourceWithVelocity(out, locale, EDIT_CONFIG_HEADER_FORWARD, paramMap);
    }

    @Override
    public void outputConfigurationBody(IThreadContext threadContext, IHTTPOutput out, Locale locale,
                                        ConfigParams parameters, String tabName)
            throws ManifoldCFException, IOException {

        // Call the Velocity templates for each tab

        // Server tab
        Map<String, Object> paramMap = new HashMap<String, Object>();
        // Set the tab name
        paramMap.put("TabName", tabName);
        // Fill in the parameters
        fillInServerConfigurationMap(paramMap, out, parameters);
        Messages.outputResourceWithVelocity(out, locale, EDIT_CONFIG_FORWARD_SERVER, paramMap);

    }

    /**
     * Process a configuration post. This method is called at the start of the
     * connector's configuration page, whenever there is a possibility that form
     * data for a connection has been posted. Its purpose is to gather form
     * information and modify the configuration parameters accordingly. The name
     * of the posted form is "editconnection".
     *
     * @param threadContext   is the local thread context.
     * @param variableContext is the set of variables available from the post,
     *                        including binary file post information.
     * @param parameters      are the configuration parameters, as they currently
     *                        exist, for this connection being configured.
     * @return null if all is well, or a string error message if there is an
     * error that should prevent saving of the connection (and cause a
     * redirection to an error page).
     */
    @Override
    public String processConfigurationPost(IThreadContext threadContext, IPostParameters variableContext,
                                           ConfigParams parameters)
            throws ManifoldCFException {

        String client_id = variableContext.getParameter("client_id");
        if (client_id != null)
            parameters.setParameter(BoxConfig.CLIENT_ID, client_id);

        String client_secret = variableContext.getParameter("client_secret");
        if (client_secret != null)
            parameters.setObfuscatedParameter(BoxConfig.CLIENT_SECRET,
                    variableContext.mapKeyToPassword(client_secret));

        String username = variableContext.getParameter("username");
        if (username != null)
            parameters.setParameter(BoxConfig.USERNAME, username);

        String password = variableContext.getParameter("password");
        if (password != null)
            parameters.setObfuscatedParameter(BoxConfig.PASSWORD, variableContext.mapKeyToPassword(password));

        return null;
    }

    /**
     * Fill in specification Velocity parameter map for DROPBOXPath tab.
     */
    private static void fillInBoxPathSpecificationMap(Map<String, Object> newMap, DocumentSpecification ds) {
        String boxPath = "";
        int i = 0;
        if (ds.getChildCount() != 0) {
            SpecificationNode sn = ds.getChild(i);
            if (sn.getType().equals(JOB_STARTPOINT_NODE_TYPE)) {
                boxPath = sn.getAttributeValue(JOB_PATH_ATTRIBUTE);
            }
        }
        newMap.put("BOXPATH", boxPath);
    }


    /**
     * View specification. This method is called in the body section of a job's
     * view page. Its purpose is to present the document specification
     * information to the user. The coder can presume that the HTML that is
     * output from this configuration will be within appropriate <html> and
     * <body> tags.
     *
     * @param out is the output to which any HTML should be sent.
     * @param ds  is the current document specification for this job.
     */
    @Override
    public void viewSpecification(IHTTPOutput out, Locale locale, DocumentSpecification ds)
            throws ManifoldCFException, IOException {

        Map<String, Object> paramMap = new HashMap<String, Object>();

        // Fill in the map with data from all tabs
        fillInBoxPathSpecificationMap(paramMap, ds);
        Messages.outputResourceWithVelocity(out, locale, VIEW_SPEC_FORWARD, paramMap);
    }

    /**
     * Process a specification post. This method is called at the start of job's
     * edit or view page, whenever there is a possibility that form data for a
     * connection has been posted. Its purpose is to gather form information and
     * modify the document specification accordingly. The name of the posted
     * form is "editjob".
     *
     * @param variableContext contains the post data, including binary
     *                        file-upload information.
     * @param ds              is the current document specification for this job.
     * @return null if all is well, or a string error message if there is an
     * error that should prevent saving of the job (and cause a redirection to
     * an error page).
     */
    @Override
    public String processSpecificationPost(IPostParameters variableContext, DocumentSpecification ds)
            throws ManifoldCFException {
        String boxPath = variableContext.getParameter(BOX_FOLDER_PATH);
        if (boxPath != null) {
            int i = 0;
            while (i < ds.getChildCount()) {
                SpecificationNode oldNode = ds.getChild(i);
                if (oldNode.getType().equals(JOB_STARTPOINT_NODE_TYPE)) {
                    ds.removeChild(i);
                    break;
                }
                i++;
            }
            SpecificationNode node = new SpecificationNode(JOB_STARTPOINT_NODE_TYPE);
            node.setAttribute(JOB_PATH_ATTRIBUTE, boxPath);
            ds.addChild(ds.getChildCount(), node);
        }
        return null;
    }

    /**
     * Output the specification body section. This method is called in the body
     * section of a job page which has selected a repository connection of the
     * current type. Its purpose is to present the required form elements for
     * editing. The coder can presume that the HTML that is output from this
     * configuration will be within appropriate <html>, <body>, and <form> tags.
     * The name of the form is "editjob".
     *
     * @param out     is the output to which any HTML should be sent.
     * @param ds      is the current document specification for this job.
     * @param tabName is the current tab name.
     */
    @Override
    public void outputSpecificationBody(IHTTPOutput out, Locale locale, DocumentSpecification ds, String tabName)
            throws ManifoldCFException, IOException {
        // Output DROPBOXPath tab
        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("TabName", tabName);
        fillInBoxPathSpecificationMap(paramMap, ds);

        Messages.outputResourceWithVelocity(out, locale, EDIT_SPEC_FORWARD_DROPBOXPATH, paramMap);
    }

    /**
     * Output the specification header section. This method is called in the
     * head section of a job page which has selected a repository connection of
     * the current type. Its purpose is to add the required tabs to the list,
     * and to output any javascript methods that might be needed by the job
     * editing HTML.
     *
     * @param out       is the output to which any HTML should be sent.
     * @param ds        is the current document specification for this job.
     * @param tabsArray is an array of tab names. Add to this array any tab
     *                  names that are specific to the connector.
     */
    @Override
    public void outputSpecificationHeader(IHTTPOutput out, Locale locale, DocumentSpecification ds,
                                          List<String> tabsArray)
            throws ManifoldCFException, IOException {
        tabsArray.add(Messages.getString(locale, BOX_PATH_TAB_PROPERTY));

        Map<String, Object> paramMap = new HashMap<String, Object>();

        // Fill in the specification header map, using data from all tabs.
        fillInBoxPathSpecificationMap(paramMap, ds);

        Messages.outputResourceWithVelocity(out, locale, EDIT_SPEC_HEADER_FORWARD, paramMap);
    }

    /**
     * This method visit a folder and seed all the doc Id available.
     * While recursive visiting the ID are collected and seeded
     *
     * @param activities is the interface this method should use to perform
     *                   whatever framework actions are desired.
     * @param spec       is a document specification (that comes from the job).
     * @param startTime  is the beginning of the time range to consider,
     *                   inclusive.
     * @param endTime    is the end of the time range to consider, exclusive.
     * @param jobMode    is an integer describing how the job is being run, whether
     *                   continuous or once-only.
     */
    @Override
    public void addSeedDocuments(ISeedingActivity activities, DocumentSpecification spec, long startTime, long endTime,
                                 int jobMode)
            throws ManifoldCFException, ServiceInterruption {
        String boxFolderPath = StringUtils.EMPTY;
        int i = 0;
        while (i < spec.getChildCount()) {
            SpecificationNode sn = spec.getChild(i);
            if (sn.getType().equals(JOB_STARTPOINT_NODE_TYPE)) {
                boxFolderPath = sn.getAttributeValue(JOB_PATH_ATTRIBUTE);
                break;
            }
            i++;
        }
        getSession();
        BoxAPIConnection connection = session.getApi();
        String[] folders;
        if(boxFolderPath.contains(","))
            folders = boxFolderPath.split(",");
        else
            folders=new String[]{boxFolderPath};
        for(String folderId:folders){
            try{
            BoxFolder folder = new BoxFolder(connection, folderId);
            visitNode(folder, activities); }
            catch(BoxAPIException e){
                 Logging.connectors.error("Not possible to crawl this folder ID : "+folderId,e);
            }
        }
    }

    /**
     * This method recursively visits a Box Folder and add the visited items to the document seeds
     *
     * @param boxFolder
     * @param activities
     * @throws ManifoldCFException
     */
    public void visitNode(BoxFolder boxFolder, ISeedingActivity activities) throws ManifoldCFException {
        for (BoxItem.Info itemInfo : boxFolder) {
            if (itemInfo instanceof BoxFile.Info) {
                BoxFile.Info fileInfo = (BoxFile.Info) itemInfo;
                activities.addSeedDocument(fileInfo.getID());
            } else {
                BoxFolder.Info folderInfo = (BoxFolder.Info) itemInfo;
                BoxFolder folder = new BoxFolder(session.getApi(), folderInfo.getID());
                visitNode(folder, activities);
            }
        }
    }


    /**
     * Process a set of documents. This is the method that should cause each
     * document to be fetched, processed, and the results either added to the
     * queue of documents for the current job, and/or entered into the
     * incremental ingestion manager. The document specification allows this
     * class to filter what is done based on the job.
     *
     * @param documentIdentifiers is the set of document identifiers to process.
     * @param versions            is the corresponding document versions to process, as
     *                            returned by getDocumentVersions() above. The implementation may choose to
     *                            ignore this parameter and always process the current version.
     * @param activities          is the interface this method should use to queue up new
     *                            document references and ingest documents.
     * @param spec                is the document specification.
     * @param scanOnly            is an array corresponding to the document identifiers. It
     *                            is set to true to indicate when the processing should only find other
     *                            references, and should not actually call the ingestion methods.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void processDocuments(String[] documentIdentifiers, String[] versions, IProcessActivity activities,
                                 DocumentSpecification spec, boolean[] scanOnly)
            throws ManifoldCFException, ServiceInterruption {
        getSession();
        BoxAPIConnection api = session.getApi();
        for (int i = 0; i < documentIdentifiers.length; i++) {
            String nodeId = documentIdentifiers[i];
            String version = versions[i];
            BoxFile file = new BoxFile(api, nodeId);
            BoxFile.Info info = file.getInfo(supportedMetadata);
            try {
                if (!scanOnly[i]) {
                    RepositoryDocument rd = getRepositoryDocument(info, file);
                    if (version != null)
                        activities.ingestDocumentWithException(nodeId, version, info.getID(), rd);
                    else
                        activities.deleteDocument(nodeId);
                }
            } catch (RuntimeException e) {
                Logging.connectors.error("[Box] Problem extracting document", e);
                continue;
            } catch (IOException e) {
                Logging.connectors.error("[Box] IO Problem extracting document", e);
                continue;
            }
        }
    }

    /**
     * Build the repository document from the BoxItem
     * Currently we provide support for the following metadata :
     * <p/>
     * {"id", "name", "description",
     * "size", "path_collection", "created_at", "modified_at", "content_created_at",
     * "content_modified_at", "created_by", "modified_by", "owned_by", "shared_link", "tags","parent"}
     *
     * @param info
     * @param file
     * @return
     * @throws IOException
     */
    private RepositoryDocument getRepositoryDocument(BoxFile.Info info, BoxFile file) throws IOException, ManifoldCFException {
        RepositoryDocument rd = new RepositoryDocument();
        BoxFolder.Info parentInfo = info.getParent();
        BoxFolder parentFolder = parentInfo.getResource();

        initRepositoryDocumentPermissions(rd, parentFolder, file);
        String name = info.getName();
        rd.setFileName(name);
        rd.setModifiedDate(info.getContentModifiedAt());
        rd.setCreatedDate(info.getContentCreatedAt());
        rd.addField(BOX_CREATION_DATE, info.getCreatedAt());
        rd.addField(BOX_LAST_MODIFIED, info.getModifiedAt());

        if (info.getCreatedBy() != null)
            rd.addField(BOX_CREATOR, info.getCreatedBy().getName());
        if (info.getModifiedBy() != null)
            rd.addField(BOX_LAST_MODIFIER, info.getModifiedBy().getName());

        String url = this.generateUrl(info, parentInfo);
        if (url != null && !url.isEmpty())
            rd.addField(URL, url);
        rd.setIndexingDate(new Date());
        String path = this.generatePath(info.getPathCollection());
        rd.addField(PATH, path + name);
        String description = info.getDescription();
        if (description != null && !description.isEmpty())
            rd.addField(BOX_DESCRIPTION, description);
        List<String> tags = info.getTags();
        if (tags != null)
            rd.addField(TAG, tags.toArray(new String[tags.size()]));
        //Download the content
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        file.download(stream);
        stream.close();
        //Read the content and store it
        ByteArrayInputStream iStream = new ByteArrayInputStream(stream.toByteArray());
        rd.setBinary(iStream, info.getSize());
        return rd;
    }

    /**
     * Generate the Url for the BoxFile according to Box standard :
     * URL : https://zaizi.app.box.com/files/0/f/3178460651/1/f_28552224096
     *
     * @param info
     * @param parentInfo
     * @return
     */
    public String generateUrl(BoxFile.Info info, BoxFolder.Info parentInfo) {
        String url = (BOX_BASE_ENDPOINT +"/files/0/f/" + parentInfo.getID() + "/1/f_" + info.getID());
        return url;
    }

    /**
     * This method generates the Path of the Box item, based on the Collection of path folder
     *
     * @param pathCollection
     * @return
     */
    private String generatePath(List<BoxFolder> pathCollection) {
        String path = "";
        if (pathCollection != null) {
            StringBuilder pathBuilder = new StringBuilder();
            for (BoxFolder f : pathCollection) {
                pathBuilder.append(f.getInfo().getName() + "/");
            }
            path = pathBuilder.toString();
        }
        return path;
    }

    /**
     * Extract the read permission from the crawled file.
     * At the moment we Index the permissions with a "Document" security type.
     * Because in sensefy currently we provide only that kind of security
     * Next iteration : the accessible will be obfuscated if necessary
     *
     * @param rd
     * @param parentFolder
     * @param file
     */
    private void initRepositoryDocumentPermissions(RepositoryDocument rd, BoxFolder parentFolder, BoxFile file) {
        ArrayList<String> aclPermissions = getAclPermissions(parentFolder, file);
        ArrayList<String> denyPermissions = new ArrayList<String>();
        denyPermissions.add(DEFAULT_DEAD_AUTH);
        rd.setSecurityACL(RepositoryDocument.SECURITY_TYPE_DOCUMENT, aclPermissions.toArray(new String[aclPermissions.size()]));
        rd.setSecurityDenyACL(RepositoryDocument.SECURITY_TYPE_DOCUMENT, denyPermissions.toArray(new String[denyPermissions.size()]));
    }

    /**
     * Return all the Acl permissions for a given file and folder
     *
     * @param parentFolder
     * @param file
     * @return
     */
    private ArrayList<String> getAclPermissions(BoxFolder parentFolder, BoxFile file) {
        ArrayList<String> aclPermissions = new ArrayList<String>();
        String permissionType = "";
        // First let's add the owner of the folder
        BoxFolder.Info info = parentFolder.getInfo("owned_by");
        BoxUser.Info ownedBy = info.getOwnedBy();
        aclPermissions.add(USER_PERMISSION_TYPE + "-" + ownedBy.getID());
        // Then let's iterate over all the collaborators
        Collection<BoxCollaboration.Info> collaborations = parentFolder.getCollaborations();
        for (BoxCollaboration.Info collaborationInfo : collaborations) {
            BoxCollaboration.Role role = collaborationInfo.getRole();
            if (isReaderRole(role)) {
                BoxCollaborator.Info accessibleBy = collaborationInfo.getAccessibleBy();
                if (accessibleBy instanceof BoxUser.Info)
                    permissionType = USER_PERMISSION_TYPE;
                else if (accessibleBy instanceof BoxGroup.Info)
                    permissionType = GROUP_PERMISSION_TYPE;
                aclPermissions.add(permissionType + "-" + accessibleBy.getID());
            }
        }
        return aclPermissions;
    }

    /**
     * This method checks if the current File has a living shared link that relax the access security permission
     * inherited from its parent folder.
     * In the case is not public shared or the link has expired we can keep the original permissions.
     *
     * @param file
     * @return
     */
    private boolean isNotShared(BoxFile file) {
        BoxSharedLink sharedLink = file.getInfo("shared_link").getSharedLink();
        boolean isNotPublicShared = true;
        boolean isShareLinkExpired = false;
        if (sharedLink != null) {
            isNotPublicShared = !sharedLink.getAccess().name().equals("OPEN") && !sharedLink.getAccess().name().equals("COMPANY");
            Date unsharedDate = sharedLink.getUnsharedDate();
            Date currentCrawlingDate = new Date();
            if (unsharedDate != null)
                isShareLinkExpired = currentCrawlingDate.after(unsharedDate);
        }
        return isNotPublicShared || isShareLinkExpired;
    }

    /**
     * Check the role in input is able to read the file
     *
     * @param role
     * @return
     */
    private boolean isReaderRole(BoxCollaboration.Role role) {
        List<String> readerRoles = new ArrayList<String>(Arrays.asList(BOX_READER_ROLES));
        return readerRoles.contains(role.name());
    }


    /**
     * This method is quite delicate here.
     * Unfortunately we need to check a subset of metadata to understand the current version of the file.
     * This is quite slow but currently there is no other method until Box provide a proper last modified date.
     *
     * @param documentIdentifiers is the array of local document identifiers, as
     *                            understood by this connector.
     * @param spec                is the current document specification for the current job. If
     *                            there is a dependency on this specification, then the version string
     *                            should include the pertinent data, so that reingestion will occur when
     *                            the specification changes. This is primarily useful for metadata.
     * @return the corresponding version strings, with null in the places where
     * the document no longer exists. Empty version strings indicate that there
     * is no versioning ability for the corresponding document, and the document
     * will always be processed.
     */
    @Override
    public String[] getDocumentVersions(String[] documentIdentifiers, String[] oldVersions,
                                        IVersionActivity activities, DocumentSpecification spec, int jobMode,
                                        boolean usesDefaultAuthority)
            throws ManifoldCFException, ServiceInterruption {
        String[] rval = new String[documentIdentifiers.length];

        getSession();

        // view the version change both in content and box metadata
        BoxAPIConnection connection = session.getApi();
        for (int j = 0; j < documentIdentifiers.length; j++) {
            String id = documentIdentifiers[j];
            BoxFile currentFile = new BoxFile(connection, id);

            rval[j] = this.generateVersion(currentFile);
        }
        return rval;
    }

    /**
     * The version for a Box file is not guaranteed by the last modified date.
     * This because unfortunately if you edit a set of metadata Box side, the modified date is not updated
     * So we need to check metadata by metadata to understand if the file must be updated or not
     *
     * @return
     */
    public String generateVersion(BoxFile currentFile) {
        String version;
        BoxFile.Info info = currentFile.getInfo("modified_at",
                "content_modified_at", "parent", "path_collection", "description", "name", "tags");
        BoxFolder.Info parentInfo = info.getParent();
        BoxFolder parentFolder = parentInfo.getResource();
        ArrayList<String> aclPermissions = this.getAclPermissions(parentFolder, currentFile);
        Date contentModifiedAt = info.getContentModifiedAt();
        Date modifiedAt = info.getModifiedAt();
        String dateBasedVersion = "";
        if (contentModifiedAt != null) {
            if (modifiedAt != null) {
                if (contentModifiedAt.after(modifiedAt))
                    dateBasedVersion = Long.toString(contentModifiedAt.getTime());
                else
                    dateBasedVersion = Long.toString(modifiedAt.getTime());
            } else
                dateBasedVersion = Long.toString(contentModifiedAt.getTime());
        }
        //Path change
        List<BoxFolder> pathCollection = info.getPathCollection();
        //Description change
        String description = info.getDescription();
        String descriptionHash = "";
        if (description != null && !description.isEmpty())
            descriptionHash = Long.toString(description.hashCode());
        //Name change
        String name = info.getName();
        //tags change
        List<String> tags = info.getTags();
        String tagsHash = "";
        if (tags != null && !tags.isEmpty())
            tagsHash = Long.toString(tags.hashCode());
        version = dateBasedVersion + aclPermissions.hashCode() + pathCollection.hashCode() + descriptionHash + name.hashCode() + tagsHash;

        return version;

    }
}
