/* $Id: DropboxSession.java 1490621 2013-06-07 12:55:04Z kwright $ */

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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.manifoldcf.crawler.connectors.box;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxUser;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.manifoldcf.crawler.system.Logging;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is modeling a BoxSession.
 * The session will operate an interaction with Box OAuth2 provider, exchanging messages
 * and getting the proper codes to automatically get Access/Refresh tokens.
 *
 * @author Alessandro Benedetti
 *         14/04/2015
 *         mcf-box-connector
 */
public class BoxSession {
    public static final String BOX_SECURITY_TOKEN = "sensefy"; //this token is a custom token to use to make the communication more secure for your application, it's a custom token, the value has been chosen deliberately
    public static final String HTTP_LOCALHOST = "http://localhost";
    private BoxAPIConnection api;

    public BoxAPIConnection getApi() {
        return api;
    }

    public void setApi(BoxAPIConnection api) {
        this.api = api;
    }


    /**
     * Currently BoxAPIConnection authentication is a draft one
     *
     * @param clientId
     * @param clientSecret
     * @param username
     * @param password
     */
    public BoxSession(String clientId, String clientSecret, String username, String password) throws IOException, URISyntaxException {
        // Create a local instance of cookie store
        CookieStore cookieStore = new BasicCookieStore();
        Logging.connectors.warn("BoxSession - Initialize the session");
        // Create local HTTP context
        HttpContext localContext = new BasicHttpContext();
        // Bind custom cookie store to the local context
        localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
        //This local context will be shared across the Authentication following auth requests
        //to share the proper cookies
        String intermediateGrantCode = getIntermediateGrantCode(clientId, username, password, localContext);
        String finalAuthenticationCode = "-1";
        if (intermediateGrantCode.equals("Invalid Login Credentials")) {
            throw new BoxAPIException("Wrong User credential - Invalid Login Credentials for :" + username);
        }else if (intermediateGrantCode.equals("Captcha")) {
            throw new BoxAPIException("You have exceeded the number of login attempts, please access https://app.box.com/login and validate your identity");
        } else if (!intermediateGrantCode.equals("-1"))
            finalAuthenticationCode = getAuthCode(clientId, intermediateGrantCode, localContext);
        if (!finalAuthenticationCode.equals("-1")) {
            try {
                api = new BoxAPIConnection(clientId, clientSecret, finalAuthenticationCode);
                api.setExpires(3500000);
            } catch (BoxAPIException e) {
                throw new BoxAPIException("Wrong Client secret details - The client_secret credential is not correct for the client_id : " + clientId);
            }
        } else {
            throw new BoxAPIException("Wrong Client details - The client_id credential is not correct : " + clientId);
        }

    }

    public boolean isValid(){
        boolean valid = false;
        if (api != null) {
            valid = validateByCurrentUser(api);
        }

        return valid;
    }

    private boolean validateByCurrentUser(BoxAPIConnection api) {
        boolean valid;
        BoxUser user = BoxUser.getCurrentUser(api);
        valid = user != null;
        return valid;
    }
    public void close() {
        // clean close the authenticated session
    }

    /**
     * This method returns the intermediate grant code to be used lately to authorize the grant.
     * To obtain this code we need to provide username and password of the  admin user in Box
     *
     * @param clientId     - clientId for the client application
     * @param username     - admin user name
     * @param password     - admin password
     * @param localContext - local context for cookies
     * @return
     * @throws java.net.URISyntaxException
     * @throws java.io.IOException
     */
    private String getIntermediateGrantCode(String clientId, String username, String password, HttpContext localContext) throws URISyntaxException, IOException {
        String intermediateGrantCode = "-1";
        URIBuilder authEndpointURI = new URIBuilder("https://app.box.com/api/oauth2/authorize");
        authEndpointURI.setParameter("response_type", "code").setParameter("client_id", clientId)
                .setParameter("state", "security_token=" + BOX_SECURITY_TOKEN);

        HttpPost post = new HttpPost(authEndpointURI.build());

        List<NameValuePair> boxFormParams = new ArrayList<NameValuePair>();
        boxFormParams.add(new BasicNameValuePair("login", username));
        boxFormParams.add(new BasicNameValuePair("password", password));
        boxFormParams.add(new BasicNameValuePair("client_id", clientId));
        //Box Internal params for Auth request
        boxFormParams.add(new BasicNameValuePair("login_submit", "Authorizing..."));
        boxFormParams.add(new BasicNameValuePair("dologin", "1"));
        boxFormParams.add(new BasicNameValuePair("response_type", "code"));
        boxFormParams.add(new BasicNameValuePair("redirect_uri", HTTP_LOCALHOST));
        boxFormParams.add(new BasicNameValuePair("scope", "root_readwrite manage_enterprise"));
        boxFormParams.add(new BasicNameValuePair("state", "security_token=" + BOX_SECURITY_TOKEN));
        boxFormParams.add(new BasicNameValuePair("submit1", "1"));
        boxFormParams.add(new BasicNameValuePair("login_or_register_mode", "login"));
        boxFormParams.add(new BasicNameValuePair("__login", "1"));

        post.setEntity(new UrlEncodedFormEntity(boxFormParams, StandardCharsets.UTF_8));

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        CloseableHttpClient httpClient = httpClientBuilder.build();
        CloseableHttpResponse response = httpClient.execute(post, localContext);
        if (response.getStatusLine().getStatusCode() == 200) {
            HttpEntity entity = response.getEntity();
            InputStream content = entity.getContent();
            StringWriter writer = new StringWriter();
            IOUtils.copy(content, writer, "UTF-8");
            String htmlContent = writer.toString();

            Pattern exp = Pattern.compile(
                    "<input type=\"hidden\" name=\"ic\" value=\"([^\"]*)\" />");
            Matcher matcher = exp.matcher(htmlContent);
            if (matcher.find()) {
                intermediateGrantCode = matcher.group(1);
            } else {
                Pattern exp21 = Pattern.compile(
                        "<div class=\"recaptcha_tag mbm\">Powered by reCAPTCHA</div>");
                Matcher matcher21 = exp21.matcher(htmlContent);
                if (matcher21.find()) {
                    intermediateGrantCode ="Captcha";
                }
                else{
                Pattern exp2 = Pattern.compile(
                        "<div class=\"e_message strong txt-medium\">([^<]*)");
                Matcher matcher2 = exp2.matcher(htmlContent);
                if (matcher2.find()) {
                    intermediateGrantCode = matcher2.group(1);
                }  }
            }
        }

        return intermediateGrantCode;
    }

    /**
     * This method returns the Auth Code to be used for the BoxAPIConnection.
     * To obtain this Auth code we need to use the intermediate Grant Code.
     *
     * @param clientId
     * @param localContext
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    private String getAuthCode(String clientId, String intermediateGrantCode, HttpContext localContext) throws URISyntaxException, IOException {
        URIBuilder authEndpointURI = new URIBuilder("https://app.box.com/api/oauth2/authorize");
        authEndpointURI.setParameter("response_type", "code").setParameter("client_id", clientId)
                .setParameter("state", "security_token="+BOX_SECURITY_TOKEN);

        HttpPost post = new HttpPost(authEndpointURI.build());
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();

        nvps.add(new BasicNameValuePair("client_id", clientId));
        nvps.add(new BasicNameValuePair("ic", intermediateGrantCode));
        //Box Internal params for Auth request
        nvps.add(new BasicNameValuePair("response_type", "code"));
        nvps.add(new BasicNameValuePair("redirect_uri", HTTP_LOCALHOST));
        nvps.add(new BasicNameValuePair("scope", "root_readwrite manage_enterprise"));
        nvps.add(new BasicNameValuePair("state", "security_token="+BOX_SECURITY_TOKEN));
        nvps.add(new BasicNameValuePair("doconsent", "doconsent"));
        nvps.add(new BasicNameValuePair("consent_accept", "Grant access to Box"));

        post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        CloseableHttpClient httpClient = httpClientBuilder.build();
        CloseableHttpResponse response = httpClient.execute(post, localContext);
        Header location = response.getFirstHeader("Location");
        String locationValue = location.getValue();
        String authCode = extractCode(locationValue);


        return authCode;
    }

    /**
     * Extract the auth Code from the redirected location
     *
     * @param location
     * @return
     */
    private String extractCode(String location) {
        String authCode = "";
        Pattern pattern = Pattern.compile("\\&code\\=([^&]+)");
        Matcher matcher = pattern.matcher(location);
        if (matcher.find()) {
            authCode = matcher.group(1);
        }
        return authCode;
    }
}
