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
package org.apache.manifoldcf.authorities.authorities.box;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.manifoldcf.authorities.interfaces.AuthorizationResponse;
import org.apache.manifoldcf.core.interfaces.CacheManagerFactory;
import org.apache.manifoldcf.core.interfaces.ConfigParams;
import org.apache.manifoldcf.core.interfaces.ICacheCreateHandle;
import org.apache.manifoldcf.core.interfaces.ICacheDescription;
import org.apache.manifoldcf.core.interfaces.ICacheHandle;
import org.apache.manifoldcf.core.interfaces.ICacheManager;
import org.apache.manifoldcf.core.interfaces.IHTTPOutput;
import org.apache.manifoldcf.core.interfaces.IPostParameters;
import org.apache.manifoldcf.core.interfaces.IThreadContext;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.interfaces.StringSet;
import org.apache.manifoldcf.crawler.connectors.box.BoxSession;
import org.apache.manifoldcf.crawler.system.Logging;
import org.apache.manifoldcf.ui.util.Encoder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxUser;

/**
 * This is the box authority implementation, which simply returns the user name
 * as the single access token.
 */
public class BoxAuthority extends
		org.apache.manifoldcf.authorities.authorities.BaseAuthorityConnector {

	public static final String _rcsid = "@(#)$Id: BoxAuthority.java 1225063 2015-04-07 00:42:19Z kwright $";
	public static final String USER_PERMISSION_TYPE = "user";
	public static final String USER_LOGIN_EMAIL = "loginMail";
	public static final String GROUP_PERMISSION_TYPE = "group";
	public static final Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern.compile(
			"^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
			Pattern.CASE_INSENSITIVE);

	public int apiResponseCode = 400;

	private long responseLifetime = 60000L; // 60sec

	private int LRUsize = 1000;

	private boolean isConnected;

	private String clientId;

	private String clientSecret;

	private String username;

	private String password;

	private String accessToken;

	private String refreshToken;

	private String apiUrl;

	private BoxAPIConnection api;

	protected BoxSession session = null;

	protected long lastSessionFetch = -1L;

	private final static String AUTH_ERR_MSG = "Authentication failure in box, Please check you your refresh and access tokens, if your refresh token is invalid you may have generate a new one manually";

	/**
	 * Cache manager.
	 */
	private ICacheManager cacheManager = null;

	/**
	 * This is the active directory global deny token. This should be ingested
	 * with all documents.
	 */
	private static final String globalDenyToken = "DEAD_AUTHORITY";

	private static final AuthorizationResponse unreachableResponse = new AuthorizationResponse(
			new String[] { globalDenyToken },
			AuthorizationResponse.RESPONSE_UNREACHABLE);

	/**
	 * Constructor.
	 */
	public BoxAuthority() {
		isConnected = false;
		api = null;
		apiUrl = BoxConfig.API_URL;
	}

	@Override
	public void setThreadContext(IThreadContext tc) throws ManifoldCFException {
		super.setThreadContext(tc);
		cacheManager = CacheManagerFactory.make(tc);

	}

	/**
	 * Connect. The configuration parameters are included.
	 *
	 * @param configParams
	 *            are the configuration parameters for this connection.
	 */
	@Override
	public void connect(ConfigParams configParams) {
		super.connect(configParams);
		isConnected = false;

		clientId = configParams.getParameter("client_id");
		clientSecret = configParams.getObfuscatedParameter("client_secret");
		username = configParams.getParameter("username");
		password = configParams.getObfuscatedParameter("password");

		if (clientId == null || StringUtils.isEmpty(clientId)) {
			Logging.connectors.error("Client id is not present");
		}

		if (clientSecret == null || StringUtils.isEmpty(clientSecret))
			Logging.connectors.error("Client secret is not present");

		if (username == null || StringUtils.isEmpty(username))
			Logging.connectors.error("username is not present");

		if (password == null || StringUtils.isEmpty(password))
			Logging.connectors.error("password is not present");

	}

	/**
	 * Set up a session
	 */
	private void getSession() throws ManifoldCFException {
		if (session == null || !session.isValid()) {
			if (StringUtils.isEmpty(clientId))
				throw new ManifoldCFException("Parameter "
						+ BoxConfig.CLIENT_ID + " required but not set");
			if (StringUtils.isEmpty(clientSecret))
				throw new ManifoldCFException("Parameter "
						+ BoxConfig.CLIENT_SECRET + " required but not set");
			if (StringUtils.isEmpty(username))
				throw new ManifoldCFException("Parameter " + BoxConfig.USERNAME
						+ " required but not set");
			if (StringUtils.isEmpty(password))
				throw new ManifoldCFException("Parameter " + BoxConfig.PASSWORD
						+ " required but not set");

			try {
				session = new BoxSession(clientId, clientSecret, username,
						password);
			} catch (RuntimeException e) {
				throw new ManifoldCFException(
						"[Box Session] Manifold Exception : ", e);
			} catch (URISyntaxException e) {
				throw new ManifoldCFException("[Box Session] Wrong URI :", e);
			} catch (IOException e) {
				throw new ManifoldCFException(
						"[Box Session] Not possible to access Box API:", e);
			}
			lastSessionFetch = System.currentTimeMillis();

		}
	}

	private boolean validateApiConnection(BoxAPIConnection api)
			throws ManifoldCFException {
		if (api == null)
			return false;
		boolean valid = false;
		try {
			return validateByCurrentUser(api);
		} catch (BoxAPIException e) {
			apiResponseCode = e.getResponseCode();

			Logging.connectors
					.error("Exception in getting user information , probabably invalid connection, API response code "
							+ apiResponseCode, e);
			valid = false;
		} catch (Exception e) {
			Logging.connectors
					.error("Exception in getting user information , probabably invalid connection",
							e);
			valid = false;
		}

		return valid;
	}

	private boolean validateByCurrentUser(BoxAPIConnection api) {
		boolean valid;
		BoxUser user = BoxUser.getCurrentUser(api);
		valid = user != null;
		return valid;
	}

	// All methods below this line will ONLY be called if a connect() call
	// succeeded
	// on this instance!

	/**
	 * Check connection for sanity.
	 */
	@Override
	public String check() throws ManifoldCFException {
		try {
			getSession();
			BoxAPIConnection apiConn = session.getApi();
			if (validateApiConnection(apiConn)) {
				return super.check();
			} else {
				return "Connection failed: "
						+ "Validating api connection status failed";
			}
		} catch (BoxAPIException e) {
			Logging.connectors.error("Connection Failed ", e);
			return "Connection failed: " + e.getMessage();
		} catch (ManifoldCFException e) {
			Logging.connectors.error("Connection Failed ", e);
			Throwable cause = e.getCause();
			return "Connection failed: " + cause.getMessage();
		} catch (RuntimeException e) {
			Logging.connectors.error("Connection Failed ", e);
			return "Connection failed: " + e.getMessage();
		}
	}

	/**
	 * This method is called to assess whether to count this connector instance
	 * should actually be counted as being connected.
	 * 
	 * @return true if the connector instance is actually connected.
	 */
	@Override
	public boolean isConnected() {
		return isConnected;
	}

	/**
	 * Close the connection. Call this before discarding the repository
	 * connector.
	 */
	@Override
	public void disconnect() throws ManifoldCFException {
		super.disconnect();
		// Zero out all the stuff that we want to be sure we don't use again
		clientId = null;
		clientSecret = null;
		accessToken = null;

		api = null;
	}

	protected String createCacheConnectionString() {
		StringBuilder sb = new StringBuilder();
		sb.append(clientId).append("#").append(clientSecret);
		return sb.toString();
	}

	/**
	 * Obtain the access tokens for a given user name.
	 *
	 * @param userName
	 *            is the user name or identifier.
	 * @return the response tokens (according to the current authority). (Should
	 *         throws an exception only when a condition cannot be properly
	 *         described within the authorization response object.)
	 */
	@Override
	public AuthorizationResponse getAuthorizationResponse(String userName)
			throws ManifoldCFException {

		ICacheDescription objectDescription = new BoxAuthorizationResponseDescription(
				userName, createCacheConnectionString(), this.responseLifetime,
				this.LRUsize);

		// Enter the cache
		ICacheHandle ch = cacheManager.enterCache(
				new ICacheDescription[] { objectDescription }, null, null);

		try {
			ICacheCreateHandle createHandle = cacheManager
					.enterCreateSection(ch);
			try {
				// Lookup the object
				AuthorizationResponse response = (AuthorizationResponse) cacheManager
						.lookupObject(createHandle, objectDescription);
				if (response != null) {
					return response;
				}
				// Create the object.
				response = getAuthorizationResponseUncached(userName);
				// Save it in the cache
				cacheManager.saveObject(createHandle, objectDescription,
						response);
				// And return it...
				return response;
			} finally {
				cacheManager.leaveCreateSection(createHandle);
			}
		} finally {
			cacheManager.leaveCache(ch);
		}

	}

	private AuthorizationResponse getAuthorizationResponseUncached(
			String userName) throws ManifoldCFException {
		ArrayList<String> tokens = new ArrayList<String>();
		if (!isConnected && api != null) {
			throw new ManifoldCFException("Api is not connected");
		}
		try {
			Logging.connectors.info("Getting the user id from box api");
			
			//check if email
			boolean isEmailAdded = false;
			boolean isInputEmail = validate(userName);
			if (isInputEmail) {
				tokens.add(USER_LOGIN_EMAIL + "-" + userName);
				isEmailAdded = true;
			}
			
			
			UserInfo boxUserInfo = getBoxUserId(userName);
			if (boxUserInfo != null && boxUserInfo.getUserId() != null
					&& !boxUserInfo.getUserId().isEmpty()) {
				tokens.add(USER_PERMISSION_TYPE + "-" + boxUserInfo.getUserId());		
				if(!isEmailAdded)
					tokens.add(USER_LOGIN_EMAIL + "-" + boxUserInfo.getLogin());
				Logging.connectors.info(String.format(
						"User id is : %s, getting memberships....",
						boxUserInfo.getUserId()));
			}

			if (boxUserInfo != null
					&& StringUtils.isNotEmpty(boxUserInfo.getUserId())) {
				ArrayList<String> memberShips = getMemberShips(boxUserInfo
						.getUserId());
				if (memberShips != null) {
					Logging.connectors.info(String.format(
							"There are %s, groups returned for user name %s",
							memberShips.size(), userName));
					tokens.addAll(memberShips);
					return new AuthorizationResponse(
							tokens.toArray(new String[tokens.size()]),
							AuthorizationResponse.RESPONSE_OK);
				}
			}
			
			
			//return authorized token if a single email is added even
			if (isEmailAdded) {
				return new AuthorizationResponse(
						tokens.toArray(new String[tokens.size()]),
						AuthorizationResponse.RESPONSE_OK);
			}

		} catch (ClientProtocolException e) {
			throw new ManifoldCFException(e.getCause());
		} catch (IOException e) {
			throw new ManifoldCFException(e.getCause());
		}

		return new AuthorizationResponse(tokens.toArray(new String[tokens
				.size()]), AuthorizationResponse.RESPONSE_USERUNAUTHORIZED);
	}

	public boolean validate(String emailStr) {
		Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr);
		return matcher.find();
	}

	private ArrayList<String> getMemberShips(String userId)
			throws ManifoldCFException {
		String apiUrl = getApiUrl();
		ArrayList<String> groupsBelongedTo = new ArrayList<String>();
		String requestUrl = String.format("%s/users/%s/memberships", apiUrl,
				userId);
		try {
			JSONObject json = getResultFromApi(requestUrl);
			try {
				JSONArray jArr = json.getJSONArray("entries");
				for (int i = 0; i < jArr.length(); ++i) {
					JSONObject entry = jArr.getJSONObject(i);

					if (entry != null) {
						JSONObject groupObj = entry.getJSONObject("group");
						if (groupObj != null) {
							// group id to be returned
							groupsBelongedTo.add(GROUP_PERMISSION_TYPE + "-"
									+ groupObj.getString("id"));
						}
					}
				}

			} catch (JSONException e) {
				Logging.connectors.error("Error while json parsing", e);
				throw new ManifoldCFException(e.getCause());
			}

		} catch (ManifoldCFException e) {
			Logging.connectors.error("error while getting json", e);
			throw new ManifoldCFException(e.getCause());
		}

		return groupsBelongedTo;
	}

	private String getApiUrl() {
		String apiUrlValue = "";
		if (apiUrl == null || StringUtils.isEmpty(apiUrl)) {
			apiUrl = BoxConfig.API_URL;
			apiUrlValue = apiUrl;
		} else {
			apiUrlValue = apiUrl;
		}
		Logging.connectors.info("Api url : " + apiUrlValue);

		return apiUrlValue;
	}

	private UserInfo getBoxUserId(String userName)
			throws ClientProtocolException, IOException, ManifoldCFException {
		String userId = "";
		String email = "";
		UserInfo userInfo = null;
		String apiUrl = getApiUrl();
		String requestUrl = String.format("%s/users?filter_term=%s", apiUrl,
				userName);
		JSONObject json = getResultFromApi(requestUrl);
		try {
			JSONArray jArr = json.getJSONArray("entries");
			if (jArr != null && jArr.length() > 0) {
				JSONObject jObj = jArr.getJSONObject(0);
				if (jObj != null) {
					userInfo = new UserInfo();
					userId = jObj.getString("id");
					email = jObj.getString("login");
					userInfo.setLogin(email);
					userInfo.setUserId(userId);
				}
			}
		} catch (JSONException e) {
			Logging.connectors.error("Error while json parsing", e);
			throw new ManifoldCFException(e.getCause());
		}

		return userInfo;
	}

	private JSONObject getResultFromApi(String requestUrl)
			throws ManifoldCFException {
		JSONObject json = null;
		DefaultHttpClient httpClient = new DefaultHttpClient();

		// get the latest access token
		checkAndInitializeAccessToken();

		try {
			HttpGet getRequest = new HttpGet(requestUrl);
			String authorizationHeader = String
					.format("Bearer %s", accessToken);
			getRequest.setHeader("Authorization", authorizationHeader);
			HttpResponse response = httpClient.execute(getRequest);

			if (response.getStatusLine().getStatusCode() == 200) {

				BufferedReader rd = null;
				try {
					rd = new BufferedReader(new InputStreamReader(response
							.getEntity().getContent()));

					StringBuffer result = new StringBuffer();
					String line = "";
					while ((line = rd.readLine()) != null) {
						result.append(line);
					}

					String jsonData = result.toString();
					json = new JSONObject(jsonData);

				} catch (JSONException e) {
					Logging.connectors.error("Error while json parsing", e);
					throw new ManifoldCFException("Error while json parsing",
							e.getCause());
				} finally {
					if (rd != null)
						IOUtils.closeQuietly(rd);
				}

			} else {
				if (response != null && response.getStatusLine() != null)
					Logging.connectors.warn("Http response code : "
							+ response.getStatusLine().getStatusCode());

				throw new ManifoldCFException(
						"Box api response is not valid, response code");
			}

		} catch (IllegalStateException e) {
			throw new ManifoldCFException(e.getCause());
		} catch (Exception e) {
			throw new ManifoldCFException(e.getCause());
		} finally {
			IOUtils.closeQuietly(httpClient);
		}

		return json;
	}

	private void checkAndInitializeAccessToken() throws ManifoldCFException {
		if (session != null && session.isValid()) {
			if (session.getApi() != null)
				accessToken = session.getApi().getAccessToken();
		} else {
			getSession();
			if (session != null && session.isValid()) {
				accessToken = session.getApi().getAccessToken();
			}

		}
		Logging.connectors
				.info("Access token at this moment is " + accessToken);
	}

	@Override
	public AuthorizationResponse getDefaultAuthorizationResponse(String userName) {

		return unreachableResponse;
	}

	private String getParam(ConfigParams parameters, String name, String def) {
		String value = "";
		value = parameters.getParameter(name) != null ? parameters
				.getParameter(name) : def;
		return value;
	}

	private String getObfuscatedParam(ConfigParams parameters, String name,
			String def) {
		String value = "";
		value = parameters.getObfuscatedParameter(name) != null ? parameters
				.getObfuscatedParameter(name) : def;
		return value;
	}

	private String getViewParam(ConfigParams parameters, String name) {
		String value = "";
		value = parameters.getParameter(name) != null ? parameters
				.getParameter(name) : "";
		return value;
	}

	private boolean copyParam(IPostParameters variableContext,
			ConfigParams parameters, String name) {
		String val = variableContext.getParameter(name);
		if (val == null) {
			return false;
		}
		parameters.setParameter(name, val);
		return true;
	}

	private boolean copyObfuscatedParam(IPostParameters variableContext,
			ConfigParams parameters, String name) {
		String val = variableContext.getParameter(name);
		if (val == null) {
			return false;
		}
		parameters.setObfuscatedParameter(name, val);
		return true;
	}

	// UI support methods.
	/*
	 * These support methods are involved in setting up authority connection
	 * configuration information. The configuration methods cannot assume that
	 * the current authority object is connected. That is why they receive a
	 * thread context argument.
	 */

	/*
	 * Output the configuration header section. This method is called in the
	 * head section of the connector's configuration page. Its purpose is to add
	 * the required tabs to the list, and to output any javascript methods that
	 * might be needed by the configuration editing HTML.
	 */
	@Override
	public void outputConfigurationHeader(IThreadContext threadContext,
			IHTTPOutput out, Locale locale, ConfigParams parameters,
			List<String> tabsArray) throws ManifoldCFException, IOException {

		tabsArray.add(Messages.getString(locale, "BOX.BOX"));
		buildJavaScriptCheck(out, locale);

	}

	private void buildJavaScriptCheck(IHTTPOutput out, Locale locale)
			throws IOException {

		out.print("<script type=\"text/javascript\">\n"
				+ "<!--\n"
				+ "function checkConfig() {\n"
				+ "  return true;\n"
				+ "}\n"
				+ "\n"
				+ "function checkConfigForSave() {\n"
				+ "  if (editconnection.client_id.value == \"\") {\n"
				+ "    alert(\""
				+ Messages.getBodyJavascriptString(locale,
						"BOX.client_id_cannot_be_blank") + "\");\n"
				+ "    SelectTab(\""
				+ Messages.getBodyJavascriptString(locale, "BOX.BOX")
				+ "\");\n" + "    editconnection.client_id.focus();\n"
				+ "    return false;\n" + "  }\n");

		out.print("  if (editconnection.client_secret.value == \"\") {\n"
				+ "    alert(\""
				+ Messages.getBodyJavascriptString(locale,
						"BOX.client_secret_cannot_be_blank") + "\");\n"
				+ "    SelectTab(\""
				+ Messages.getBodyJavascriptString(locale, "BOX.BOX")
				+ "\");\n" + "    editconnection.client_secret.focus();\n"
				+ "    return false;\n" + "  }\n");
		out.print("  if (editconnection.username.value == \"\") {\n"
				+ "    alert(\""
				+ Messages.getBodyJavascriptString(locale,
						"BOX.username_cannot_be_blank") + "\");\n"
				+ "    SelectTab(\""
				+ Messages.getBodyJavascriptString(locale, "BOX.BOX")
				+ "\");\n" + "    editconnection.username.focus();\n"
				+ "    return false;\n" + "  }\n");

		out.print("  if (editconnection.password.value == \"\") {\n"
				+ "    alert(\""
				+ Messages.getBodyJavascriptString(locale,
						"BOX.password_cannot_be_blank") + "\");\n"
				+ "    SelectTab(\""
				+ Messages.getBodyJavascriptString(locale, "BOX.BOX")
				+ "\");\n" + "    editconnection.password.focus();\n"
				+ "    return false;\n" + "  }\n");

		out.print("  return true;\n" + "}\n");

		out.print(" function endsWith(str, suffix) {"
				+ " return str.indexOf(suffix, str.length - suffix.length) !== -1;"
				+ "}\n");

		out.print("//-->\n" + "</script>\n");

	}

	/*
	 * Output the configuration body section. This method is called in the body
	 * section of the authority connector's configuration page. Its purpose is
	 * to present the required form elements for editing. The coder can presume
	 * that the HTML that is output from this configuration will be within
	 * appropriate <html>, <body>, and <form> tags. The name of the form is
	 * "editconnection".
	 */
	@Override
	public void outputConfigurationBody(IThreadContext threadContext,
			IHTTPOutput out, Locale locale, ConfigParams parameters,
			String tabName) throws ManifoldCFException, IOException {
		String fClientId = getParam(parameters, "client_id", "");
		String fClientSecret = getObfuscatedParam(parameters, "client_secret",
				"");
		String fUsername = getParam(parameters, "username", "");
		String fPassword = getObfuscatedParam(parameters, "password", "");

		if (tabName.equals(Messages.getString(locale, "BOX.BOX"))) {
			buildBoxConfigSection(out, locale, fClientId, fClientSecret,
					fUsername, fPassword);
		}

	}

	private void buildBoxConfigSection(IHTTPOutput out, Locale locale,
			String fClientId, String fClientSecret, String fUsername,
			String fPassword) throws IOException {
		out.print("<table class=\"displaytable\">\n"
				+ " <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"
				+ " <tr>\n" + "  <td class=\"description\"><nobr>"
				+ Messages.getBodyString(locale, "BOX.client_id")
				+ "</nobr></td>\n"
				+ "  <td class=\"value\"><input type=\"text\" size=\"40\" name=\"client_id\" value=\""
				+ Encoder.attributeEscape(fClientId)
				+ "\"/></td>\n"
				+ " </tr>\n"
				+ " <tr>\n"
				+ "  <td class=\"description\"><nobr>"
				+ Messages.getBodyString(locale, "BOX.client_secret")
				+ "</nobr></td>\n"
				+ "  <td class=\"value\"><input type=\"password\" size=\"50\" name=\"client_secret\" value=\""
				+ Encoder.attributeEscape(fClientSecret)
				+ "\"/></td>\n"
				+ " </tr>\n"
				+ " <tr>\n"
				+ "  <td class=\"description\"><nobr>"
				+ Messages.getBodyString(locale, "BOX.username")
				+ "</nobr></td>\n"
				+ "  <td class=\"value\"><input type=\"text\" size=\"50\" name=\"username\" value=\""
				+ Encoder.attributeEscape(fUsername)
				+ "\"/></td>\n"
				+ " </tr>\n"
				+ " <tr>\n"

				+ "  <td class=\"description\"><nobr>"
				+ Messages.getBodyString(locale, "BOX.password")
				+ "</nobr></td>\n"
				+ "  <td class=\"value\"><input type=\"password\" size=\"50\" name=\"password\" value=\""
				+ Encoder.attributeEscape(fPassword)
				+ "\"/></td>\n"
				+ " </tr>\n");

		out.print("</table>\n");
	}

	/*
	 * Process a configuration post. This method is called at the start of the
	 * authority connector's configuration page, whenever there is a possibility
	 * that form data for a connection has been posted. Its purpose is to gather
	 * form information and modify the configuration parameters accordingly. The
	 * name of the posted form is "editconnection".
	 */
	@Override
	public String processConfigurationPost(IThreadContext threadContext,
			IPostParameters variableContext, Locale locale,
			ConfigParams parameters) throws ManifoldCFException {
		copyParam(variableContext, parameters, BoxConfig.CLIENT_ID);
		copyObfuscatedParam(variableContext, parameters,
				BoxConfig.CLIENT_SECRET);
		copyParam(variableContext, parameters, BoxConfig.USERNAME);
		copyObfuscatedParam(variableContext, parameters, BoxConfig.PASSWORD);

		return null;
	}

	/*
	 * View configuration. This method is called in the body section of the
	 * authority connector's view configuration page. Its purpose is to present
	 * the connection information to the user. The coder can presume that the
	 * HTML that is output from this configuration will be within appropriate
	 * <html> and <body> tags.
	 */
	@Override
	public void viewConfiguration(IThreadContext threadContext,
			IHTTPOutput out, Locale locale, ConfigParams parameters)
			throws ManifoldCFException, IOException {

		buildViewBodyForBox(out, locale, parameters);
	}

	private void buildViewBodyForBox(IHTTPOutput out, Locale locale,
			ConfigParams parameters) throws IOException {
		String f_clientId = getViewParam(parameters, "client_id");
		String f_username = getViewParam(parameters, "username");

		out.print("<table class=\"displaytable\">\n"
				+ " <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"
				+ " <tr>\n" + "  <td class=\"description\"><nobr>"
				+ Messages.getBodyString(locale, "BOX.client_id")
				+ "</nobr></td>\n"
				+ "  <td class=\"value\">"
				+ Encoder.bodyEscape(f_clientId)
				+ "</td>\n"
				+ " </tr>\n"
				+ " <tr>\n"
				+ "  <td class=\"description\"><nobr>"
				+ Messages.getBodyString(locale, "BOX.client_secret")
				+ "</nobr></td>\n"
				+ "  <td class=\"value\">"
				+ "*************"
				+ "</td>\n"
				+ " </tr>\n"

				+ " <tr>\n"
				+ "  <td class=\"description\"><nobr>"
				+ Messages.getBodyString(locale, "BOX.username")
				+ "</nobr></td>\n"
				+ "  <td class=\"value\">"
				+ f_username
				+ "</td>\n"
				+ " </tr>\n"

				+ " <tr>\n"
				+ "  <td class=\"description\"><nobr>"
				+ Messages.getBodyString(locale, "BOX.password")
				+ "</nobr></td>\n"
				+ "  <td class=\"value\">"
				+ "***************" + "</td>\n" + " </tr>\n");

		out.print("</table>\n");
	}

	// Protected methods
	protected static StringSet emptyStringSet = new StringSet();

	/**
	 * This is the cache object descriptor for cached access tokens from this
	 * connector.
	 */
	protected class BoxAuthorizationResponseDescription extends
			org.apache.manifoldcf.core.cachemanager.BaseDescription {

		/**
		 * The user name
		 */
		protected String userName;

		/**
		 * The response lifetime
		 */
		protected long responseLifetime;

		/**
		 * The expiration time
		 */
		protected long expirationTime = -1;

		/**
		 * LDAP connection string with server name and base DN
		 */
		protected String connectionString;

		public BoxAuthorizationResponseDescription(String userName,
				String connectionString, long responseLifetime, int LRUsize) {
			super("LDAPAuthority", LRUsize);
			this.userName = userName;
			this.responseLifetime = responseLifetime;
			this.connectionString = connectionString;
		}

		public String getCriticalSectionName() {
			StringBuilder sb = new StringBuilder(getClass().getName());
			sb.append("-").append(userName).append("-")
					.append(connectionString);
			return sb.toString();
		}

		public StringSet getObjectKeys() {
			return emptyStringSet;
		}

		@Override
		public long getObjectExpirationTime(long currentTime) {
			if (expirationTime == -1) {
				expirationTime = currentTime + responseLifetime;
			}
			return expirationTime;
		}

		@Override
		public int hashCode() {
			return userName.hashCode() + +connectionString.hashCode();
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof BoxAuthorizationResponseDescription)) {
				return false;
			}
			BoxAuthorizationResponseDescription ard = (BoxAuthorizationResponseDescription) o;
			if (!ard.userName.equals(userName)) {
				return false;
			}
			if (!ard.connectionString.equals(connectionString)) {
				return false;
			}
			return true;
		}

	}

}
