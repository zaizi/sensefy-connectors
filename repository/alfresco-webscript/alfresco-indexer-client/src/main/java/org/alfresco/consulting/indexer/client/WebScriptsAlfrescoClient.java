package org.alfresco.consulting.indexer.client;

import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import com.google.common.net.MediaType;
import com.google.gson.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebScriptsAlfrescoClient implements AlfrescoClient
{
    private static final String FIELD_PROPERTIES = "properties";

    private static final String LAST_TXN_ID = "last_txn_id";
    private static final String DOCS = "docs";
    private static final String LAST_ACL_CS_ID = "last_acl_changeset_id";

    private static final String URL_PARAM_LAST_TXN_ID = "lastTxnId";
    private static final String URL_PARAM_LAST_ACL_CS_ID = "lastAclChangesetId";
    private static final String URL_PARAM_INDEXING_FILTERS = "indexingFilters";

    private static final String STORE_ID = "store_id";
    private static final String STORE_PROTOCOL = "store_protocol";
    private static final String USERNAME = "username";
    private static final String AUTHORITIES = "authorities";
    private final Gson gson = new Gson();

    private final String changesUrl;
    private final String actionsUrl;
    private final String metadataUrl;
    private final String authoritiesUrl;
    private final String manifoldAuthoritiesUrl;
    private final String loginUrl;
    private final String contentUrl;

    private final String username;
    private final String password;

    private final Logger logger = LoggerFactory.getLogger(WebScriptsAlfrescoClient.class);

    public WebScriptsAlfrescoClient(String protocol, String hostname, String endpoint, String storeProtocol,
            String storeId)
    {
        this(protocol, hostname, endpoint, storeProtocol, storeId, null, null);
    }

    public WebScriptsAlfrescoClient(String protocol, String hostname, String endpoint, String storeProtocol,
            String storeId, String username, String password)
    {
        changesUrl = String.format("%s://%s%s/node/changes/%s/%s", protocol, hostname, endpoint, storeProtocol, storeId);
        actionsUrl = String.format("%s://%s%s/node/actions/%s/%s", protocol, hostname, endpoint, storeProtocol, storeId);
        metadataUrl = String.format("%s://%s%s/node/details/%s/%s", protocol, hostname, endpoint, storeProtocol,
                storeId);
        authoritiesUrl = String.format("%s://%s%s/auth/resolve/", protocol, hostname, endpoint);
        manifoldAuthoritiesUrl=  String.format("%s://%s%s/manifold/userAuthorities", protocol, hostname, endpoint);
        loginUrl=String.format("%s://%s%s/api/login", protocol, hostname, endpoint);
        contentUrl = String.format("%s://%s%s/api/node/%s/%s/", protocol, hostname, endpoint, storeProtocol, storeId).concat(
                "%s/content");

        this.username = username;
        this.password = password;
    }

    @Override
    public AlfrescoResponse fetchNodes(long lastTransactionId, long lastAclChangesetId, AlfrescoFilters filters)
    {

        String urlWithParameter = String.format("%s?%s", changesUrl,
                urlParameters(lastTransactionId, lastAclChangesetId, filters));
        return getDocumentsActions(urlWithParameter);
    }

    @Override
    public AlfrescoResponse fetchNode(String nodeUuid) throws AlfrescoDownException
    {
        String urlWithParameter = String.format("%s/%s", actionsUrl, nodeUuid);
        return getDocumentsActions(urlWithParameter);
    }

    private AlfrescoResponse getDocumentsActions(String url)
    {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        logger.debug("[Processing] Hitting url for document actions: {}", url);

        try
        {
            HttpGet httpGet = createGetRequest(url);
            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            AlfrescoResponse afResponse = fromHttpEntity(entity);
            EntityUtils.consume(entity);
            return afResponse;
        }
        catch (IOException e)
        {
            logger.error("[Processing] Failed to fetch node(s)", e);
            throw new AlfrescoDownException("Alfresco appears to be down", e);
        }
    }

    private HttpGet createGetRequest(String url)
    {
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("Accept", "application/json");
        if (useBasicAuthentication())
        {
            httpGet.addHeader(
                    "Authorization",
                    "Basic "
                            + Base64.encodeBase64String(String.format("%s:%s", username, password).getBytes(
                                    Charset.forName("UTF-8"))));
        }
        return httpGet;
    }

    private boolean useBasicAuthentication()
    {
        return username != null && !"".equals(username) && password != null;
    }

    private String urlParameters(long lastTransactionId, long lastAclChangesetId, AlfrescoFilters filters)
    {

        String indexingFilters = null;
        try
        {
            indexingFilters = URLEncoder.encode(filters.toJSONString(), "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            indexingFilters = filters.toJSONString();
        }

        String urlParameters = String.format("%s=%d&%s=%d&%s=%s", URL_PARAM_LAST_TXN_ID, lastTransactionId,
                URL_PARAM_LAST_ACL_CS_ID, lastAclChangesetId, URL_PARAM_INDEXING_FILTERS, indexingFilters);

        return urlParameters;
    }

    private AlfrescoResponse fromHttpEntity(HttpEntity entity) throws IOException
    {
        Reader entityReader = new InputStreamReader(entity.getContent());
        JsonObject responseObject = gson.fromJson(entityReader, JsonObject.class);
        ArrayList<Map<String, Object>> documents = new ArrayList<Map<String, Object>>();

        long lastTransactionId = getStringAsLong(responseObject, LAST_TXN_ID, 0L);
        long lastAclChangesetId = getStringAsLong(responseObject, LAST_ACL_CS_ID, 0L);
        String storeId = getString(responseObject, STORE_ID);
        String storeProtocol = getString(responseObject, STORE_PROTOCOL);

        if (responseObject.has(DOCS) && responseObject.get(DOCS).isJsonArray())
        {
            JsonArray docsArray = responseObject.get(DOCS).getAsJsonArray();
            for (JsonElement documentElement : docsArray)
            {
                Map<String, Object> document = createDocument(documentElement);
                document.put(STORE_ID, storeId);
                document.put(STORE_PROTOCOL, storeProtocol);
                documents.add(document);
            }
        }
        else
        {
            logger.warn("[Processing] No documents in the Alfresco response associated to the current node(s)");
        }

        return new AlfrescoResponse(lastTransactionId, lastAclChangesetId, storeId, storeProtocol, documents);
    }

    private long getStringAsLong(JsonObject responseObject, String key, long defaultValue)
    {
        String string = getString(responseObject, key);
        if (Strings.isNullOrEmpty(string))
        {
            return defaultValue;
        }
        return Long.parseLong(string);
    }

    private String getString(JsonObject responseObject, String key)
    {
        if (responseObject.has(key))
        {
            JsonElement element = responseObject.get(key);
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString())
            {
                return element.getAsString();
            }
            else
            {
                logger.warn("[Parsing] The {} property (={}) is not a string in document: {}", new Object[] { key,
                        element, responseObject });
            }
        }
        else
        {
            logger.warn("[Parsing] The key {} is missing from document: {}", key, responseObject);
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> createDocument(JsonElement documentElement)
    {
        if (documentElement.isJsonObject())
        {
            JsonObject documentObject = documentElement.getAsJsonObject();
            return (Map<String, Object>) gson.fromJson(documentObject, Map.class);
        }
        return new HashMap<String, Object>();
    }

    @Override
    public Map<String, Object> fetchMetadata(String nodeUuid) throws AlfrescoDownException
    {
        String json = fetchMetadataJson(nodeUuid);

        @SuppressWarnings("unchecked")
        Map<String, Object> map = gson.fromJson(json, Map.class);

        List<Map<String, String>> properties = extractPropertiesFieldFromMap(nodeUuid, map);

        for (Map<String, String> e : properties)
        {
            map.put(e.get("name"), e.get("value"));
        }
        return map;
    }

    private String fetchMetadataJson(String nodeUuid)
    {
        String fullUrl = String.format("%s/%s", metadataUrl, nodeUuid);
        logger.debug("[Processing] Hitting url: {} for metadata fetching", fullUrl);
        try
        {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = createGetRequest(fullUrl);
            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            return CharStreams.toString(new InputStreamReader(entity.getContent(), "UTF-8"));
        }
        catch (IOException e)
        {
            logger.error("[Processing] Failed to fetch metadata for node[{}]", nodeUuid, e);
            throw new AlfrescoDownException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> extractPropertiesFieldFromMap(String nodeUuid, Map<String, Object> map)
    {
        Object properties = map.remove(FIELD_PROPERTIES);
        if (properties == null)
        {
            throw new AlfrescoDownException("[Processing] No Properties Fetched for the Node " + nodeUuid);
        }

        if (!(properties instanceof List))
        {
            throw new AlfrescoDownException("[Processing] " + FIELD_PROPERTIES + " is not of type List, it is of type "
                    + properties.getClass());
        }
        return (List<Map<String, String>>) properties;
    }

    private AlfrescoUser userFromHttpEntity(HttpEntity entity) throws IOException
    {
        Reader entityReader = new InputStreamReader(entity.getContent());
        JsonObject responseObject = (JsonObject) gson.fromJson(entityReader, JsonArray.class).get(0);
        return getUser(responseObject);
    }

    private AlfrescoUser getUser(JsonObject responseObject)
    {
        String username = getUsername(responseObject);
        List<String> authorities = getAuthorities(responseObject);
        return new AlfrescoUser(username, authorities);
    }

    private String getUsername(JsonObject userObject)
    {
        if (!userObject.has(USERNAME))
        {
            throw new AlfrescoParseException("[Processing] Json response is missing username.");
        }
        JsonElement usernameElement = userObject.get(USERNAME);
        if (!usernameElement.isJsonPrimitive() || !usernameElement.getAsJsonPrimitive().isString())
        {
            throw new AlfrescoParseException("[Processing] Username must be a string. It was: "
                    + usernameElement.toString());
        }
        return usernameElement.getAsString();
    }

    private List<String> getAuthorities(JsonObject userObject)
    {
        List<String> authorities = new ArrayList<String>();
        if (!userObject.has(AUTHORITIES))
        {
            throw new AlfrescoParseException("[Processing] Json response is missing authorities.");
        }
        JsonElement authoritiesElement = userObject.get(AUTHORITIES);
        if (!authoritiesElement.isJsonArray())
        {
            throw new AlfrescoParseException("[Processing] Authorities must be a json array. It was: "
                    + authoritiesElement.toString());
        }
        JsonArray authoritiesArray = authoritiesElement.getAsJsonArray();
        for (JsonElement authorityElement : authoritiesArray)
        {
            if (!authorityElement.isJsonPrimitive())
            {
                throw new AlfrescoParseException("[Processing] Authority element must be a string. It was: "
                        + authoritiesElement.toString());
            }
            JsonPrimitive authorityPrimitive = authorityElement.getAsJsonPrimitive();
            if (!authorityPrimitive.isString())
            {
                throw new AlfrescoParseException("[Processing] Authority primitive must be a string. It was: "
                        + authorityPrimitive.toString());
            }
            authorities.add(authorityPrimitive.getAsString());
        }
        return authorities;
    }

    /**
     * THis method verify we have a proper connection to Alfresco, returning an exception with the proper details
     * @return
     * @throws AlfrescoConnectionException
     */
    @Override
    public boolean checkConnection() throws AlfrescoConnectionException
    {
        HttpResponse response;
        try
        {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            String url = String.format("%s?u=%s&pw=%s&format=json", loginUrl,username,password);
            logger.debug("[Checking Connection] Username : ", username);
            HttpGet httpGet = new HttpGet(url);
            httpGet.addHeader("Accept", "application/json");
            response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            if(statusCode==200) {
                HttpEntity entity = response.getEntity();
                if(entity!=null){
                    String alfrescoTicket=this.alfrescoTicketFromHttpEntity(entity);
                    return fetchUserAuthoritiesWithTicket(username,alfrescoTicket);}
                else
                    return false;
            }
            else if (statusCode==403)
                throw new AlfrescoConnectionException("[Authentication] Wrong credentials");
            else if (statusCode==400)
                throw new AlfrescoConnectionException("[Bad Request] Please verify the Alfresco URL configurations");
            else if (statusCode==404)
                throw new AlfrescoConnectionException("[Page Not Found] Please verify the Alfresco URL configurations");
            else if (statusCode==500)
                throw new AlfrescoConnectionException("[Internal Server] Alfresco internal server error");
        }
        catch (IOException e)
        {
            logger.warn("[Checking Connection] Failed to Access URl : "+loginUrl, e);
            throw new AlfrescoConnectionException("[Checking Connection] Failed to Access URL :"+loginUrl,e);
        }
        return false;
    }

    /**
     * Verify the Amps are properly installed in the target Alfresco
     * @param username
     * @param alfrescoTicket
     * @return
     * @throws AlfrescoConnectionException
     */
    public boolean fetchUserAuthoritiesWithTicket(String username,String alfrescoTicket) throws AlfrescoConnectionException
    {
        HttpResponse response;
        try
        {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            String url = String.format("%s?userName=%s&alf_ticket=%s", manifoldAuthoritiesUrl, username,alfrescoTicket);
            logger.debug("[Processing] Hitting url: {} for user authorities fetching : ", manifoldAuthoritiesUrl);
            HttpGet httpGet = createGetRequest(url);
            response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            if(statusCode==403)
                throw new AlfrescoConnectionException("[Checking Connection] Alfresco Manifold AMP not installed");
            else
                return true;
        }
        catch (IOException e)
        {
            logger.warn("[Fetching Authorities] Failed to get Authorities", e);
            throw new AlfrescoDownException("Alfresco appears to be down", e);
        }
    }
    public AlfrescoUser fetchUserAuthorities(String username) throws AlfrescoDownException
    {
        HttpResponse response;
        try
        {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            String url = String.format("%s%s", authoritiesUrl, username);
            logger.debug("[Processing] Hitting url: {} for user authorities fetching", url);
            HttpGet httpGet = createGetRequest(url);
            response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            AlfrescoUser afResponse = userFromHttpEntity(entity);
            EntityUtils.consume(entity);
            return afResponse;
        }
        catch (IOException e)
        {
            logger.warn("[Processing] Failed to fetch node(s)", e);
            throw new AlfrescoDownException("Alfresco appears to be down", e);
        }
    }

    @Override
    public List<AlfrescoUser> fetchAllUsersAuthorities() throws AlfrescoDownException
    {
        HttpResponse response;
        try
        {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            logger.debug("[Processing] Hitting url: {} for All user authorities fetching", authoritiesUrl);
            HttpGet httpGet = createGetRequest(authoritiesUrl);
            response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            List<AlfrescoUser> users = usersFromHttpEntity(entity);
            EntityUtils.consume(entity);
            return users;
        }
        catch (IOException e)
        {
            logger.warn("[Processing] Failed to fetch node(s)", e);
            throw new AlfrescoDownException("Alfresco appears to be down", e);
        }
    }

    public String alfrescoTicketFromHttpEntity(HttpEntity entity) throws IOException
    {
        Reader entityReader = new InputStreamReader(entity.getContent());
        JsonElement responseObject = gson.fromJson(entityReader, JsonElement.class);
        JsonElement data = responseObject.getAsJsonObject().get("data");
        JsonElement ticket = data.getAsJsonObject().get("ticket");
        return ticket.getAsString();
    }

    private List<AlfrescoUser> usersFromHttpEntity(HttpEntity entity) throws IOException
    {
        Reader entityReader = new InputStreamReader(entity.getContent());
        JsonElement responseObject = gson.fromJson(entityReader, JsonElement.class);
        if (!responseObject.isJsonArray())
        {
            throw new AlfrescoParseException("[Parsing] Users response object is not a json array.");
        }
        List<AlfrescoUser> users = new ArrayList<AlfrescoUser>();
        JsonArray usersArray = responseObject.getAsJsonArray();
        for (JsonElement userElement : usersArray)
        {
            if (!userElement.isJsonObject())
            {
                throw new AlfrescoParseException("[Parsing] User is not a json object.");
            }
            AlfrescoUser user = getUser(userElement.getAsJsonObject());
            users.add(user);
        }
        return users;
    }

    @Override
    public InputStream fetchContent(String nodeUuid)
    {
        String contentUrlPath = String.format(contentUrl, nodeUuid);
        logger.debug("[Processing] Hitting url: {} for content fetching", contentUrlPath);
        HttpGet httpGet = new HttpGet(contentUrlPath);
        httpGet.addHeader("Accept", MediaType.APPLICATION_BINARY.toString());
        if (useBasicAuthentication())
        {
            httpGet.addHeader(
                    "Authorization",
                    "Basic "
                            + Base64.encodeBase64String(String.format("%s:%s", username, password).getBytes(
                                    Charset.forName("UTF-8"))));
        }

        CloseableHttpClient httpClient = HttpClients.createDefault();
        try
        {
            HttpResponse response = httpClient.execute(httpGet);
            return response.getEntity().getContent();
        }
        catch (Exception e)
        {
            throw new AlfrescoDownException("Alfresco appears to be down", e);
        }
    }

    public String getChangesUrl()
    {
        return changesUrl;
    }

    public String getActionsUrl()
    {
        return actionsUrl;
    }

    public String getAuthoritiesUrl()
    {
        return authoritiesUrl;
    }

    public String getMetadataUrl()
    {
        return metadataUrl;
    }
}
