/* $Id: NullAuthority.java 1225063 2011-12-28 00:42:19Z kwright $ */

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
package org.zaizi.manifoldcf.authorities.authorities.alfresco;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.manifoldcf.authorities.interfaces.AuthorizationResponse;
import org.apache.manifoldcf.core.interfaces.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

/**
 * This is the null authority implementation, which simply returns the user name as the single access token. This is
 * useful for situations where all the documents are being indexed using forced acls, and a demonstration of security is
 * desired. It's also quite useful for testing.
 */
public class AlfrescoAuthorityConnector extends org.apache.manifoldcf.authorities.authorities.BaseAuthorityConnector
{
    private static final String USER_AUTHORITIES_URI = "/service/manifold/userAuthorities";

    private static final String USERNAME_PARAM = "userName";

    private static final String UTF8 = "UTF-8";

    private static final String AUTHORITIES_JSON = "authorities";

    /** This is the active directory global deny token. This should be ingested with all documents. */
    private static final String globalDenyToken = "DEAD_AUTHORITY";

    private static final AuthorizationResponse unreachableResponse = new AuthorizationResponse(
            new String[] { globalDenyToken }, AuthorizationResponse.RESPONSE_UNREACHABLE);

    /**
     * Username value for the Alfresco session
     */
    protected String username = null;

    /**
     * Password value for the Alfresco session
     */
    protected String password = null;

    /**
     * Endpoint protocol
     */
    protected String protocol = null;

    /**
     * Endpoint server name
     */
    protected String server = null;

    /**
     * Endpoint port
     */
    protected String port = null;

    /**
     * Endpoint context path of the Alfresco webapp
     */
    protected String path = null;

    private DefaultHttpClient httpClient;

    /**
     * Alfresco Server configuration tab name
     */
    private static final String ALFRESCO_SERVER_TAB_RESOURCE = "AlfrescoAuthorityConnector.Server";

    /**
     * Forward to the javascript to check the configuration parameters
     */
    private static final String EDIT_CONFIG_HEADER_FORWARD = "editConfiguration.js";

    /**
     * Forward to the HTML template to edit the configuration parameters
     */
    private static final String EDIT_CONFIG_FORWARD_SERVER = "editConfiguration_Server.html";

    // /**
    // * Forward to the javascript to check the specification parameters for the job
    // */
    // private static final String EDIT_SPEC_HEADER_FORWARD = "editSpecification.js";

    /**
     * Forward to the HTML template to view the configuration parameters
     */
    private static final String VIEW_CONFIG_FORWARD = "viewConfiguration.html";

    /**
     * Constructor.
     */
    public AlfrescoAuthorityConnector()
    {
    }

    // All methods below this line will ONLY be called if a connect() call succeeded
    // on this instance!

    /**
     * Connect.
     * 
     * @param configParams is the set of configuration parameters, which in this case describe the target appliance,
     *            basic auth configuration, etc. (This formerly came out of the ini file.)
     */
    @Override
    public void connect(ConfigParams configParams)
    {
        super.connect(configParams);
        username = params.getParameter(AlfrescoConfig.USERNAME_PARAM);
        password = params.getObfuscatedParameter(AlfrescoConfig.PASSWORD_PARAM);
        protocol = params.getParameter(AlfrescoConfig.PROTOCOL_PARAM);
        server = params.getParameter(AlfrescoConfig.SERVER_PARAM);
        port = params.getParameter(AlfrescoConfig.PORT_PARAM);
        path = params.getParameter(AlfrescoConfig.PATH_PARAM);

        PoolingClientConnectionManager connectionManager = new PoolingClientConnectionManager();
        httpClient = new DefaultHttpClient(connectionManager);
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username, password);
        httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);
    }

    /**
     * Close the connection. Call this before discarding the connection.
     */
    @Override
    public void disconnect() throws ManifoldCFException
    {
        username = null;
        password = null;
        protocol = null;
        server = null;
        port = null;
        path = null;
    }

    /**
     * Check connection for sanity.
     */
    @Override
    public String check() throws ManifoldCFException
    {
        URIBuilder uri = new URIBuilder();
        uri.setScheme(protocol);
        uri.setHost(server);
        uri.setPort(Integer.parseInt(port));
        uri.setPath(path + USER_AUTHORITIES_URI);

        try
        {
            HttpResponse response = httpClient.execute(new HttpGet(uri.build()));
            EntityUtils.consume(response.getEntity());
            if (response.getStatusLine().getStatusCode() != 200)
            {
                return "Connection not working! Check Configuration";
            }
        }
        catch (Exception e)
        {
            return "Connection not working! Check Configuration";
        }

        return super.check();
    }

    /**
     * Obtain the access tokens for a given user name.
     * 
     * @param userName is the user name or identifier.
     * @return the response tokens (according to the current authority). (Should throws an exception only when a
     *         condition cannot be properly described within the authorization response object.)
     */
    @Override
    public AuthorizationResponse getAuthorizationResponse(String userName) throws ManifoldCFException
    {
        // String[] tokens = new String[] { userName };
        // return new AuthorizationResponse(tokens, AuthorizationResponse.RESPONSE_OK);

        List<String> tokens = new ArrayList<String>();
        try
        {
            URIBuilder uri = new URIBuilder();
            uri.setScheme(protocol);
            uri.setHost(server);
            uri.setPort(Integer.parseInt(port));
            uri.setPath(path + USER_AUTHORITIES_URI);
            uri.setParameter(USERNAME_PARAM, userName);

            HttpGet get = new HttpGet(uri.build());

            HttpResponse response = httpClient.execute(get);

            JSONObject json = new JSONObject(EntityUtils.toString(response.getEntity(), UTF8));

            JSONArray auths = (JSONArray) json.get(AUTHORITIES_JSON);
            if (auths != null)
            {
                int len = auths.length();
                for (int i = 0; i < len; i++)
                {
                    tokens.add(auths.get(i).toString());
                }
            }

            EntityUtils.consume(response.getEntity());

        }
        catch (Exception e)
        {
            return new AuthorizationResponse(null, AuthorizationResponse.RESPONSE_UNREACHABLE);
        }

        return new AuthorizationResponse(tokens.toArray(new String[] {}), AuthorizationResponse.RESPONSE_OK);
    }

    /**
     * Obtain the default access tokens for a given user name.
     * 
     * @param userName is the user name or identifier.
     * @return the default response tokens, presuming that the connect method fails.
     */
    @Override
    public AuthorizationResponse getDefaultAuthorizationResponse(String userName)
    {
        // The default response if the getConnection method fails, which should never happen.
        return unreachableResponse;
    }

    // UI support methods.
    //
    // These support methods are involved in setting up authority connection configuration information. The
    // configuration methods cannot assume that the
    // current authority object is connected. That is why they receive a thread context argument.

    /**
     * Output the configuration header section. This method is called in the head section of the connector's
     * configuration page. Its purpose is to add the required tabs to the list, and to output any javascript methods
     * that might be needed by the configuration editing HTML.
     * 
     * @param threadContext is the local thread context.
     * @param out is the output to which any HTML should be sent.
     * @param parameters are the configuration parameters, as they currently exist, for this connection being
     *            configured.
     * @param tabsArray is an array of tab names. Add to this array any tab names that are specific to the connector.
     */
    @Override
    public void outputConfigurationHeader(IThreadContext threadContext, IHTTPOutput out, Locale locale,
            ConfigParams parameters, List<String> tabsArray) throws ManifoldCFException, IOException
    {
        // Add Server tab
        tabsArray.add(Messages.getString(locale, ALFRESCO_SERVER_TAB_RESOURCE));

        Map<String, String> paramMap = new HashMap<String, String>();

        // Fill in parameters for all tabs
        fillInServerParameters(paramMap, parameters);

        outputResource(EDIT_CONFIG_HEADER_FORWARD, out, locale, paramMap);
    }

    /**
     * Output the configuration body section. This method is called in the body section of the authority connector's
     * configuration page. Its purpose is to present the required form elements for editing. The coder can presume that
     * the HTML that is output from this configuration will be within appropriate <html>, <body>, and <form> tags. The
     * name of the form is "editconnection".
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
        fillInServerParameters(paramMap, parameters);
        outputResource(EDIT_CONFIG_FORWARD_SERVER, out, locale, paramMap);
    }

    /**
     * Process a configuration post. This method is called at the start of the authority connector's configuration page,
     * whenever there is a possibility that form data for a connection has been posted. Its purpose is to gather form
     * information and modify the configuration parameters accordingly. The name of the posted form is "editconnection".
     * 
     * @param threadContext is the local thread context.
     * @param variableContext is the set of variables available from the post, including binary file post information.
     * @param parameters are the configuration parameters, as they currently exist, for this connection being
     *            configured.
     * @return null if all is well, or a string error message if there is an error that should prevent saving of the
     *         connection (and cause a redirection to an error page).
     */
    @Override
    public String processConfigurationPost(IThreadContext threadContext, IPostParameters variableContext,
            Locale locale, ConfigParams parameters) throws ManifoldCFException
    {
        String username = variableContext.getParameter(AlfrescoConfig.USERNAME_PARAM);
        if (username != null)
        {
            parameters.setParameter(AlfrescoConfig.USERNAME_PARAM, username);
        }

        String password = variableContext.getParameter(AlfrescoConfig.PASSWORD_PARAM);
        if (password != null)
        {
            parameters.setObfuscatedParameter(AlfrescoConfig.PASSWORD_PARAM, password);
        }

        String protocol = variableContext.getParameter(AlfrescoConfig.PROTOCOL_PARAM);
        if (protocol != null)
        {
            parameters.setParameter(AlfrescoConfig.PROTOCOL_PARAM, protocol);
        }

        String server = variableContext.getParameter(AlfrescoConfig.SERVER_PARAM);
        if (server != null)
        {
            parameters.setParameter(AlfrescoConfig.SERVER_PARAM, server);
        }

        String port = variableContext.getParameter(AlfrescoConfig.PORT_PARAM);
        if (port != null)
        {
            parameters.setParameter(AlfrescoConfig.PORT_PARAM, port);
        }

        String path = variableContext.getParameter(AlfrescoConfig.PATH_PARAM);
        if (path != null)
        {
            parameters.setParameter(AlfrescoConfig.PATH_PARAM, path);
        }

        return null;
    }

    /**
     * View configuration. This method is called in the body section of the authority connector's view configuration
     * page. Its purpose is to present the connection information to the user. The coder can presume that the HTML that
     * is output from this configuration will be within appropriate <html> and <body> tags.
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

        outputResource(VIEW_CONFIG_FORWARD, out, locale, paramMap);
    }

    /**
     * Fill in Velocity parameters for the Server tab.
     */
    private static void fillInServerParameters(Map<String, String> paramMap, ConfigParams parameters)
    {
        String username = parameters.getParameter(AlfrescoConfig.USERNAME_PARAM);
        if (username == null)
            username = AlfrescoConfig.USERNAME_DEFAULT_VALUE;
        paramMap.put(AlfrescoConfig.USERNAME_PARAM, username);

        String password = parameters.getObfuscatedParameter(AlfrescoConfig.PASSWORD_PARAM);
        if (password == null)
            password = AlfrescoConfig.PASSWORD_DEFAULT_VALUE;
        paramMap.put(AlfrescoConfig.PASSWORD_PARAM, password);

        String protocol = parameters.getParameter(AlfrescoConfig.PROTOCOL_PARAM);
        if (protocol == null)
            protocol = AlfrescoConfig.PROTOCOL_DEFAULT_VALUE;
        paramMap.put(AlfrescoConfig.PROTOCOL_PARAM, protocol);

        String server = parameters.getParameter(AlfrescoConfig.SERVER_PARAM);
        if (server == null)
            server = AlfrescoConfig.SERVER_DEFAULT_VALUE;
        paramMap.put(AlfrescoConfig.SERVER_PARAM, server);

        String port = parameters.getParameter(AlfrescoConfig.PORT_PARAM);
        if (port == null)
            port = AlfrescoConfig.PORT_DEFAULT_VALUE;
        paramMap.put(AlfrescoConfig.PORT_PARAM, port);

        String path = parameters.getParameter(AlfrescoConfig.PATH_PARAM);
        if (path == null)
            path = AlfrescoConfig.PATH_DEFAULT_VALUE;
        paramMap.put(AlfrescoConfig.PATH_PARAM, path);

    }

    /**
     * Read the content of a resource, replace the variable ${PARAMNAME} with the value and copy it to the out.
     * 
     * @param resName
     * @param out
     * @throws ManifoldCFException
     */
    private static void outputResource(String resName, IHTTPOutput out, Locale locale, Map<String, String> paramMap)
            throws ManifoldCFException
    {
        Messages.outputResourceWithVelocity(out, locale, resName, paramMap, true);
    }

}
