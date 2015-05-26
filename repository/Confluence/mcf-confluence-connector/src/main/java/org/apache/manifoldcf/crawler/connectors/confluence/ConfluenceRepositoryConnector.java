package org.apache.manifoldcf.crawler.connectors.confluence;

import org.apache.commons.lang.StringUtils;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
import org.apache.manifoldcf.core.common.XThreadStringBuffer;
import org.apache.manifoldcf.core.interfaces.*;
import org.apache.manifoldcf.crawler.connectors.BaseRepositoryConnector;
import org.apache.manifoldcf.crawler.interfaces.DocumentSpecification;
import org.apache.manifoldcf.crawler.interfaces.IProcessActivity;
import org.apache.manifoldcf.crawler.interfaces.ISeedingActivity;
import org.apache.manifoldcf.crawler.system.Logging;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;

/**
 * Zaizi (pvt) Ltd
 * @author kgunaratnam
 *
 */
public class ConfluenceRepositoryConnector extends BaseRepositoryConnector {

	protected final static String ACTIVITY_READ = "read document";

	/** Deny access token for default authority */
	private final static String defaultAuthorityDenyToken = GLOBAL_DENY_TOKEN;

	// Nodes
	private static final String JOB_STARTPOINT_NODE_TYPE = "startpoint";
	private static final String JOB_QUERY_ATTRIBUTE = "query";
	private static final String JOB_SECURITY_NODE_TYPE = "security";
	private static final String JOB_VALUE_ATTRIBUTE = "value";
	private static final String JOB_ACCESS_NODE_TYPE = "access";
	private static final String JOB_TOKEN_ATTRIBUTE = "token";

	// Configuration tabs
	private static final String CONF_SERVER_TAB_PROPERTY = "ConfRepositoryConnector.Server";
	private static final String CONF_PROXY_TAB_PROPERTY = "ConfRepositoryConnector.Proxy";

	// Specification tabs
	private static final String CONF_QUERY_TAB_PROPERTY = "ConfRepositoryConnector.Query";//query will be ommitted
	private static final String CONF_SECURITY_TAB_PROPERTY = "ConfRepositoryConnector.Security";

	// pages & js
	// Template names for configuration
	/**
	 * Forward to the javascript to check the configuration parameters
	 */
	private static final String EDIT_CONFIG_HEADER_FORWARD = "editConfiguration_conf.js";
	/**
	 * Server tab template
	 */
	private static final String EDIT_CONFIG_FORWARD_SERVER = "editConfiguration_conf_server.html";
	/**
	 * Proxy tab template
	 */
	private static final String EDIT_CONFIG_FORWARD_PROXY = "editConfiguration_conf_proxy.html";

	/**
	 * Forward to the HTML template to view the configuration parameters
	 */
	private static final String VIEW_CONFIG_FORWARD = "viewConfiguration_conf.html";

	// Template names for specification
	/**
	 * Forward to the javascript to check the specification parameters for the
	 * job
	 */
	private static final String EDIT_SPEC_HEADER_FORWARD = "editSpecification_conf.js";
	/**
	 * Forward to the template to edit the query for the job
	 */
	private static final String EDIT_SPEC_FORWARD_CONFQUERY = "editSpecification_confQuery.html";
	/**
	 * Forward to the template to edit the security parameters for the job
	 */
	private static final String EDIT_SPEC_FORWARD_SECURITY = "editSpecification_confSecurity.html";

	/**
	 * Forward to the template to view the specification parameters for the job
	 */
	private static final String VIEW_SPEC_FORWARD = "viewSpecification_conf.html";

	protected ConfluenceSession session = null;
	protected long lastSessionFetch = -1L;
	protected static final long timeToRelease = 300000L;

	// Parameter data
	protected String confprotocol = null;
	protected String confhost = null;
	protected String confport = null;
	protected String confpath = null;
	protected String clientid = null;
	protected String clientsecret = null;

	protected String confproxyhost = null;
	protected String confproxyport = null;
	protected String confproxydomain = null;
	protected String confproxyusername = null;
	protected String confproxypassword = null;

	public ConfluenceRepositoryConnector() {
		super();
	}

	@Override
	public String[] getActivitiesList() {
		return new String[] { ACTIVITY_READ };
	}

	@Override
	public String[] getBinNames(String documentIdentifier) {
		return new String[] { confhost };
	}

	/**
	 * Close the connection. Call this before discarding the connection.
	 */
	@Override
	public void disconnect() throws ManifoldCFException {
		if (session != null) {
			session.close();
			session = null;
			lastSessionFetch = -1L;
		}

		confprotocol = null;
		confhost = null;
		confport = null;
		confpath = null;
		clientid = null;
		clientsecret = null;

		confproxyhost = null;
		confproxyport = null;
		confproxydomain = null;
		confproxyusername = null;
		confproxypassword = null;
	}

	/**
	 * Makes connection to server
	 * 
	 * 
	 */
	@Override
	public void connect(ConfigParams configParams) {
		super.connect(configParams);

		confprotocol = params
				.getParameter(ConfluenceConfig.CONF_PROTOCOL_PARAM);
		confhost = params.getParameter(ConfluenceConfig.CONF_HOST_PARAM);
		confport = params.getParameter(ConfluenceConfig.CONF_PORT_PARAM);
		confpath = params.getParameter(ConfluenceConfig.CONF_PATH_PARAM);
		clientid = params.getParameter(ConfluenceConfig.CLIENT_ID_PARAM);
		clientsecret = params
				.getObfuscatedParameter(ConfluenceConfig.CLIENT_SECRET_PARAM);

		confproxyhost = params
				.getParameter(ConfluenceConfig.CONF_PROXYHOST_PARAM);
		confproxyport = params
				.getParameter(ConfluenceConfig.CONF_PROXYPORT_PARAM);
		confproxydomain = params
				.getParameter(ConfluenceConfig.CONF_PROXYDOMAIN_PARAM);
		confproxyusername = params
				.getParameter(ConfluenceConfig.CONF_PROXYUSERNAME_PARAM);
		confproxypassword = params
				.getObfuscatedParameter(ConfluenceConfig.CONF_PROXYPASSWORD_PARAM);

	}

	/**
	 * Checks if connection is available
	 * 
	 * 
	 */
	@Override
	public String check() throws ManifoldCFException {
		try {
			checkConnection();
			return super.check();
		} catch (ServiceInterruption e) {
			return "Connection temporarily failed: " + e.getMessage();
		} catch (ManifoldCFException e) {
			return "Connection failed: " + e.getMessage();
		}
	}

	protected ConfluenceSession getSession() throws ManifoldCFException,
			ServiceInterruption {
		if (session == null) {
			// Check for parameter validity

			if (StringUtils.isEmpty(confprotocol)) {
				throw new ManifoldCFException("Parameter "
						+ ConfluenceConfig.CONF_PROTOCOL_PARAM
						+ " required but not set");
			}

			if (Logging.connectors.isDebugEnabled()) {
				Logging.connectors.debug("CONF: confprotocol = '"
						+ confprotocol + "'");
			}

			if (StringUtils.isEmpty(confhost)) {
				throw new ManifoldCFException("Parameter "
						+ ConfluenceConfig.CONF_HOST_PARAM
						+ " required but not set");
			}

			if (Logging.connectors.isDebugEnabled()) {
				Logging.connectors.debug("CONF: confhost = '" + confhost + "'");
			}

			if (Logging.connectors.isDebugEnabled()) {
				Logging.connectors.debug("CONF: confport = '" + confport + "'");
			}

			if (StringUtils.isEmpty(confpath)) {
				throw new ManifoldCFException("Parameter "
						+ ConfluenceConfig.CONF_PATH_PARAM
						+ " required but not set");
			}

			if (Logging.connectors.isDebugEnabled()) {
				Logging.connectors.debug("CONF: confpath = '" + confpath + "'");
			}

			if (Logging.connectors.isDebugEnabled()) {
				Logging.connectors.debug("CONF: Clientid = '" + clientid + "'");
			}

			if (Logging.connectors.isDebugEnabled()) {
				Logging.connectors.debug("CONF: Clientsecret = '"
						+ clientsecret + "'");
			}

			int portInt;
			if (confport != null && confport.length() > 0) {
				try {
					portInt = Integer.parseInt(confport);
				} catch (NumberFormatException e) {
					throw new ManifoldCFException("Bad number: "
							+ e.getMessage(), e);
				}
			} else {
				if (confprotocol.toLowerCase(Locale.ROOT).equals("http"))
					portInt = 80;
				else
					portInt = 443;
			}

			int proxyPortInt;
			if (confproxyport != null && confproxyport.length() > 0) {
				try {
					proxyPortInt = Integer.parseInt(confproxyport);
				} catch (NumberFormatException e) {
					throw new ManifoldCFException("Bad number: "
							+ e.getMessage(), e);
				}
			} else
				proxyPortInt = 8080;

			//generate a session used to called with http calls
			session = new ConfluenceSession(clientid, clientsecret,
					confprotocol, confhost, portInt, confpath, confproxyhost,
					proxyPortInt, confproxydomain, confproxyusername,
					confproxypassword);

		}
		lastSessionFetch = System.currentTimeMillis();
		return session;
	}

	/**
	 * This method is called to assess whether to count this connector instance
	 * should actually be counted as being connected.
	 *
	 * @return true if the connector instance is actually connected.
	 */
	@Override
	public boolean isConnected() {
		return session != null;
	}

	@Override
	public void poll() throws ManifoldCFException {
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
		return new String[] {};
	}

	private static void fillInServerConfigurationMap(
			Map<String, Object> newMap, IPasswordMapperActivity mapper,
			ConfigParams parameters) {
		String confprotocol = parameters
				.getParameter(ConfluenceConfig.CONF_PROTOCOL_PARAM);
		String confhost = parameters
				.getParameter(ConfluenceConfig.CONF_HOST_PARAM);
		String confport = parameters
				.getParameter(ConfluenceConfig.CONF_PORT_PARAM);
		String confpath = parameters
				.getParameter(ConfluenceConfig.CONF_PATH_PARAM);
		String clientid = parameters
				.getParameter(ConfluenceConfig.CLIENT_ID_PARAM);
		String clientsecret = parameters
				.getObfuscatedParameter(ConfluenceConfig.CLIENT_SECRET_PARAM);

		if (confprotocol == null)
			confprotocol = ConfluenceConfig.CONF_PROTOCOL_DEFAULT;
		if (confhost == null)
			confhost = ConfluenceConfig.CONF_HOST_DEFAULT;
		if (confport == null)
			confport = ConfluenceConfig.CONF_PORT_DEFAULT;
		if (confpath == null)
			confpath = ConfluenceConfig.CONF_PATH_DEFAULT;

		if (clientid == null)
			clientid = ConfluenceConfig.CLIENT_ID_DEFAULT;
		if (clientsecret == null)
			clientsecret = ConfluenceConfig.CLIENT_SECRET_DEFAULT;
		else
			clientsecret = mapper.mapPasswordToKey(clientsecret);

		newMap.put("CONFPROTOCOL", confprotocol);
		newMap.put("CONFHOST", confhost);
		newMap.put("CONFPORT", confport);
		newMap.put("CONFPATH", confpath);
		newMap.put("CLIENTID", clientid);
		newMap.put("CLIENTSECRET", clientsecret);
	}

	
	private static void fillInProxyConfigurationMap(Map<String, Object> newMap,
			IPasswordMapperActivity mapper, ConfigParams parameters) {
		String confproxyhost = parameters
				.getParameter(ConfluenceConfig.CONF_PROXYHOST_PARAM);
		String confproxyport = parameters
				.getParameter(ConfluenceConfig.CONF_PROXYPORT_PARAM);
		String confproxydomain = parameters
				.getParameter(ConfluenceConfig.CONF_PROXYDOMAIN_PARAM);
		String confproxyusername = parameters
				.getParameter(ConfluenceConfig.CONF_PROXYUSERNAME_PARAM);
		String confproxypassword = parameters
				.getObfuscatedParameter(ConfluenceConfig.CONF_PROXYPASSWORD_PARAM);

		if (confproxyhost == null)
			confproxyhost = ConfluenceConfig.CONF_PROXYHOST_DEFAULT;
		if (confproxyport == null)
			confproxyport = ConfluenceConfig.CONF_PROXYPORT_DEFAULT;

		if (confproxydomain == null)
			confproxydomain = ConfluenceConfig.CONF_PROXYDOMAIN_DEFAULT;
		if (confproxyusername == null)
			confproxyusername = ConfluenceConfig.CONF_PROXYUSERNAME_DEFAULT;
		if (confproxypassword == null)
			confproxypassword = ConfluenceConfig.CONF_PROXYPASSWORD_DEFAULT;
		else
			confproxypassword = mapper.mapPasswordToKey(confproxypassword);

		newMap.put("CONFPROXYHOST", confproxyhost);
		newMap.put("CONFPROXYPORT", confproxyport);
		newMap.put("CONFPROXYDOMAIN", confproxydomain);
		newMap.put("CONFPROXYUSERNAME", confproxyusername);
		newMap.put("CONFPROXYPASSWORD", confproxypassword);
	}

	@Override
	public void viewConfiguration(IThreadContext threadContext,
			IHTTPOutput out, Locale locale, ConfigParams parameters)
			throws ManifoldCFException, IOException {
		Map<String, Object> paramMap = new HashMap<String, Object>();

		// Fill in map from each tab
		fillInServerConfigurationMap(paramMap, out, parameters);
		//fillInProxyConfigurationMap(paramMap, out, parameters);

		Messages.outputResourceWithVelocity(out, locale, VIEW_CONFIG_FORWARD,
                paramMap);
	}

	@Override
	public void outputConfigurationHeader(IThreadContext threadContext,
			IHTTPOutput out, Locale locale, ConfigParams parameters,
			List<String> tabsArray) throws ManifoldCFException, IOException {
		// Add the Server tab
		tabsArray.add(Messages.getString(locale, CONF_SERVER_TAB_PROPERTY));
		// Add the Proxy tab
		//tabsArray.add(Messages.getString(locale, CONF_PROXY_TAB_PROPERTY));
		// Map the parameters
		Map<String, Object> paramMap = new HashMap<String, Object>();

		// Fill in the parameters from each tab
		fillInServerConfigurationMap(paramMap, out, parameters);
		//fillInProxyConfigurationMap(paramMap, out, parameters);

		// Output the Javascript - only one Velocity template for all tabs
		Messages.outputResourceWithVelocity(out, locale,
                EDIT_CONFIG_HEADER_FORWARD, paramMap);
	}

	@Override
	public void outputConfigurationBody(IThreadContext threadContext,
			IHTTPOutput out, Locale locale, ConfigParams parameters,
			String tabName) throws ManifoldCFException, IOException {

		// Call the Velocity templates for each tab
		Map<String, Object> paramMap = new HashMap<String, Object>();
		// Set the tab name
		paramMap.put("TabName", tabName);

		// Fill in the parameters
		fillInServerConfigurationMap(paramMap, out, parameters);
		//fillInProxyConfigurationMap(paramMap, out, parameters);

		// Server tab
		Messages.outputResourceWithVelocity(out, locale, EDIT_CONFIG_FORWARD_SERVER, paramMap);
		// Proxy tab
		//Messages.outputResourceWithVelocity(out, locale, EDIT_CONFIG_FORWARD_PROXY, paramMap);

	}

	/* Repository specification post handle, (server and proxy & client secret etc)
	 * @see org.apache.manifoldcf.core.connector.BaseConnector#processConfigurationPost(org.apache.manifoldcf.core.interfaces.IThreadContext, org.apache.manifoldcf.core.interfaces.IPostParameters, org.apache.manifoldcf.core.interfaces.ConfigParams)
	 */
	@Override
	public String processConfigurationPost(IThreadContext threadContext,
			IPostParameters variableContext, ConfigParams parameters)
			throws ManifoldCFException {

		// Server tab parameters

		String confprotocol = variableContext.getParameter("confprotocol");
		if (confprotocol != null)
			parameters.setParameter(ConfluenceConfig.CONF_PROTOCOL_PARAM,
					confprotocol);

		String confhost = variableContext.getParameter("confhost");
		if (confhost != null)
			parameters.setParameter(ConfluenceConfig.CONF_HOST_PARAM, confhost);

		String confport = variableContext.getParameter("confport");
		if (confport != null)
			parameters.setParameter(ConfluenceConfig.CONF_PORT_PARAM, confport);

		String confpath = variableContext.getParameter("confpath");
		if (confpath != null)
			parameters.setParameter(ConfluenceConfig.CONF_PATH_PARAM, confpath);

		String clientid = variableContext.getParameter("clientid");
		if (clientid != null)
			parameters.setParameter(ConfluenceConfig.CLIENT_ID_PARAM, clientid);

		String clientsecret = variableContext.getParameter("clientsecret");
		if (clientsecret != null)
			parameters.setObfuscatedParameter(
					ConfluenceConfig.CLIENT_SECRET_PARAM,
					variableContext.mapKeyToPassword(clientsecret));

		// Proxy tab parameters

		String confproxyhost = variableContext.getParameter("confproxyhost");
		if (confproxyhost != null)
			parameters.setParameter(ConfluenceConfig.CONF_PROXYHOST_PARAM,
					confproxyhost);

		String confproxyport = variableContext.getParameter("confproxyport");
		if (confproxyport != null)
			parameters.setParameter(ConfluenceConfig.CONF_PROXYPORT_PARAM,
					confproxyport);

		String confproxydomain = variableContext
				.getParameter("confproxydomain");
		if (confproxydomain != null)
			parameters.setParameter(ConfluenceConfig.CONF_PROXYDOMAIN_PARAM,
					confproxydomain);

		String confproxyusername = variableContext
				.getParameter("confproxyusername");
		if (confproxyusername != null)
			parameters.setParameter(ConfluenceConfig.CONF_PROXYUSERNAME_PARAM,
					confproxyusername);

		String confproxypassword = variableContext
				.getParameter("confproxypassword");
		if (confproxypassword != null)
			parameters.setObfuscatedParameter(
					ConfluenceConfig.CONF_PROXYPASSWORD_PARAM,
					variableContext.mapKeyToPassword(confproxypassword));
		
		//should return null on success
		return null;
	}

	private static void fillInConfQuerySpecificationMap(
			Map<String, Object> newMap, DocumentSpecification ds) {
		String ConfQuery = ConfluenceConfig.CONF_QUERY_DEFAULT;
		for (int i = 0; i < ds.getChildCount(); i++) {
			SpecificationNode sn = ds.getChild(i);
			if (sn.getType().equals(JOB_STARTPOINT_NODE_TYPE)) {
				ConfQuery = sn.getAttributeValue(JOB_QUERY_ATTRIBUTE);
			}
		}
		newMap.put("CONFQUERY", ConfQuery);
	}

	private static void fillInConfSecuritySpecificationMap(
			Map<String, Object> newMap, DocumentSpecification ds) {
		List<Map<String, String>> accessTokenList = new ArrayList<Map<String, String>>();
		String securityValue = "on";
		for (int i = 0; i < ds.getChildCount(); i++) {
			SpecificationNode sn = ds.getChild(i);
			if (sn.getType().equals(JOB_ACCESS_NODE_TYPE)) {
				String token = sn.getAttributeValue(JOB_TOKEN_ATTRIBUTE);
				Map<String, String> accessMap = new HashMap<String, String>();
				accessMap.put("TOKEN", token);
				accessTokenList.add(accessMap);
			} else if (sn.getType().equals(JOB_SECURITY_NODE_TYPE)) {
				securityValue = sn.getAttributeValue(JOB_VALUE_ATTRIBUTE);
			}
		}
		newMap.put("ACCESSTOKENS", accessTokenList);
		newMap.put("SECURITYON", securityValue);
	}

	@Override
	public void viewSpecification(IHTTPOutput out, Locale locale,
			DocumentSpecification ds) throws ManifoldCFException, IOException {

		Map<String, Object> paramMap = new HashMap<String, Object>();

		// Fill in the map with data from all tabs
		fillInConfQuerySpecificationMap(paramMap, ds);
		fillInConfSecuritySpecificationMap(paramMap, ds);

		Messages.outputResourceWithVelocity(out, locale, VIEW_SPEC_FORWARD,
                paramMap);
	}

	/* Handle job specification post
	 * @see org.apache.manifoldcf.crawler.connectors.BaseRepositoryConnector#processSpecificationPost(org.apache.manifoldcf.core.interfaces.IPostParameters, org.apache.manifoldcf.crawler.interfaces.DocumentSpecification)
	 */
	@Override
	public String processSpecificationPost(IPostParameters variableContext,
			DocumentSpecification ds) throws ManifoldCFException {

		String confDriveQuery = variableContext.getParameter("confquery");
		if (confDriveQuery != null) {
			int i = 0;
			while (i < ds.getChildCount()) {
				SpecificationNode oldNode = ds.getChild(i);
				if (oldNode.getType().equals(JOB_STARTPOINT_NODE_TYPE)) {
					ds.removeChild(i);
					break;
				}
				i++;
			}
			SpecificationNode node = new SpecificationNode(
					JOB_STARTPOINT_NODE_TYPE);
			node.setAttribute(JOB_QUERY_ATTRIBUTE, confDriveQuery);
			ds.addChild(ds.getChildCount(), node);
		}

		String securityOn = variableContext.getParameter("specsecurity");
		if (securityOn != null) {
			// Delete all security records first
			int i = 0;
			while (i < ds.getChildCount()) {
				SpecificationNode sn = ds.getChild(i);
				if (sn.getType().equals(JOB_SECURITY_NODE_TYPE))
					ds.removeChild(i);
				else
					i++;
			}
			SpecificationNode node = new SpecificationNode(
					JOB_SECURITY_NODE_TYPE);
			node.setAttribute(JOB_VALUE_ATTRIBUTE, securityOn);
			ds.addChild(ds.getChildCount(), node);
		}

		String xc = variableContext.getParameter("tokencount");
		if (xc != null) {
			// Delete all tokens first
			int i = 0;
			while (i < ds.getChildCount()) {
				SpecificationNode sn = ds.getChild(i);
				if (sn.getType().equals(JOB_ACCESS_NODE_TYPE))
					ds.removeChild(i);
				else
					i++;
			}

			int accessCount = Integer.parseInt(xc);
			i = 0;
			while (i < accessCount) {
				String accessDescription = "_" + Integer.toString(i);
				String accessOpName = "accessop" + accessDescription;
				xc = variableContext.getParameter(accessOpName);
				if (xc != null && xc.equals("Delete")) {
					// Next row
					i++;
					continue;
				}
				// Get the stuff we need
				String accessSpec = variableContext.getParameter("spectoken"
						+ accessDescription);
				SpecificationNode node = new SpecificationNode(
						JOB_ACCESS_NODE_TYPE);
				node.setAttribute(JOB_TOKEN_ATTRIBUTE, accessSpec);
				ds.addChild(ds.getChildCount(), node);
				i++;
			}

			String op = variableContext.getParameter("accessop");
			if (op != null && op.equals("Add")) {
				String accessspec = variableContext.getParameter("spectoken");
				SpecificationNode node = new SpecificationNode(
						JOB_ACCESS_NODE_TYPE);
				node.setAttribute(JOB_TOKEN_ATTRIBUTE, accessspec);
				ds.addChild(ds.getChildCount(), node);
			}
		}

		return null;
	}

	
	/* (non-Javadoc)
	 * @see org.apache.manifoldcf.crawler.connectors.BaseRepositoryConnector#outputSpecificationBody(org.apache.manifoldcf.core.interfaces.IHTTPOutput, java.util.Locale, org.apache.manifoldcf.crawler.interfaces.DocumentSpecification, java.lang.String)
	 */
	@Override
	
	public void outputSpecificationBody(IHTTPOutput out, Locale locale,
			DocumentSpecification ds, String tabName)
			throws ManifoldCFException, IOException {

		// Output JIRAQuery tab
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("TabName", tabName);
		//query specification will not be used
		//fillInConfQuerySpecificationMap(paramMap, ds);
		fillInConfSecuritySpecificationMap(paramMap, ds);
		
		//no seed query is needed
		//Messages.outputResourceWithVelocity(out, locale,EDIT_SPEC_FORWARD_CONFQUERY, paramMap);
		Messages.outputResourceWithVelocity(out, locale, EDIT_SPEC_FORWARD_SECURITY, paramMap);
	}

	/* 
	 * Header for the specification
	 * @see org.apache.manifoldcf.crawler.connectors.BaseRepositoryConnector#outputSpecificationHeader(org.apache.manifoldcf.core.interfaces.IHTTPOutput, java.util.Locale, org.apache.manifoldcf.crawler.interfaces.DocumentSpecification, java.util.List)
	 */
	@Override
	public void outputSpecificationHeader(IHTTPOutput out, Locale locale,
			DocumentSpecification ds, List<String> tabsArray)
			throws ManifoldCFException, IOException {

		//tabsArray.add(Messages.getString(locale, CONF_QUERY_TAB_PROPERTY));
		tabsArray.add(Messages.getString(locale, CONF_SECURITY_TAB_PROPERTY));

		Map<String, Object> paramMap = new HashMap<String, Object>();

		// Fill in the specification header map, using data from all tabs.
		//fillInConfQuerySpecificationMap(paramMap, ds);
		fillInConfSecuritySpecificationMap(paramMap, ds);

		Messages.outputResourceWithVelocity(out, locale,
                EDIT_SPEC_HEADER_FORWARD, paramMap);
	}

	/* Adding seed documents
	 * @see org.apache.manifoldcf.crawler.connectors.BaseRepositoryConnector#addSeedDocuments(org.apache.manifoldcf.crawler.interfaces.ISeedingActivity, org.apache.manifoldcf.crawler.interfaces.DocumentSpecification, long, long, int)
	 */
	@Override
	public void addSeedDocuments(ISeedingActivity activities,
			DocumentSpecification spec, long startTime, long endTime,
			int jobMode) throws ManifoldCFException, ServiceInterruption {

		String confDriveQuery = ConfluenceConfig.CONF_QUERY_DEFAULT;
		int i = 0;
		while (i < spec.getChildCount()) {
			SpecificationNode sn = spec.getChild(i);
			if (sn.getType().equals(JOB_STARTPOINT_NODE_TYPE)) {
				confDriveQuery = sn.getAttributeValue(JOB_QUERY_ATTRIBUTE);
				break;
			}
			i++;
		}

		GetSeedsThread t = new GetSeedsThread(getSession(), confDriveQuery);
		try {
			t.start();
			boolean wasInterrupted = false;
			try {
				XThreadStringBuffer seedBuffer = t.getBuffer();

				while (true) {
					String contentKey = seedBuffer.fetch();
					if (contentKey == null)
						break;
					// Add the pageID to the queue
					activities.addSeedDocument(contentKey);
				}
			} catch (InterruptedException e) {
				wasInterrupted = true;
				throw e;
			} catch (ManifoldCFException e) {
				if (e.getErrorCode() == ManifoldCFException.INTERRUPTED)
					wasInterrupted = true;
				throw e;
			} finally {
				if (!wasInterrupted)
					t.finishUp();
			}
		} catch (InterruptedException e) {
			t.interrupt();
			throw new ManifoldCFException("Interrupted: " + e.getMessage(), e,
					ManifoldCFException.INTERRUPTED);
		} catch (java.net.SocketTimeoutException e) {
			handleIOException(e);
		} catch (InterruptedIOException e) {
			t.interrupt();
			handleIOException(e);
		} catch (IOException e) {
			handleIOException(e);
		} catch (ResponseException e) {
			handleResponseException(e);
		}
	}

	/* Process documents
	 * @see org.apache.manifoldcf.crawler.connectors.BaseRepositoryConnector#processDocuments(java.lang.String[], java.lang.String[], org.apache.manifoldcf.crawler.interfaces.IProcessActivity, org.apache.manifoldcf.crawler.interfaces.DocumentSpecification, boolean[])
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void processDocuments(String[] documentIdentifiers,
			String[] versions, IProcessActivity activities,
			DocumentSpecification spec, boolean[] scanOnly)
			throws ManifoldCFException, ServiceInterruption {

		Logging.connectors.debug("Process Confluence documents: Inside processDocuments");

		for (int i = 0; i < documentIdentifiers.length; i++) {
			String nodeId = documentIdentifiers[i];
			String version = versions[i];

			long startTime = System.currentTimeMillis();
			String errorCode = "FAILED";
			String errorDesc = StringUtils.EMPTY;
			Long fileSize = null;
			boolean doLog = false;

			try {
				if (Logging.connectors.isDebugEnabled()) {
					Logging.connectors
							.debug("JIRA: Processing document identifier '"
									+ nodeId + "'");
				}

				if (!scanOnly[i]) {
                    if (version!=null) {
                        doLog = true;
                        String contentId = nodeId;
                        ConfluenceContent confContent = getContent(contentId);
                        if (confContent == null) {
                          activities.deleteDocument(nodeId);
                          continue;
                        }
                        if (Logging.connectors.isDebugEnabled()) {
                          Logging.connectors.debug("Confluence: This content exists: " + confContent.getID());
                        }
                        RepositoryDocument rd = new RepositoryDocument();
                        String mimeType = "text/html";
                        Date createdDate = confContent.getCreatedDate();
                        Date modifiedDate = confContent.getCreatedDate();
                        rd.setMimeType(mimeType);
                        if (createdDate != null)
                          rd.setCreatedDate(createdDate);
                        if (modifiedDate != null)
                          rd.setModifiedDate(modifiedDate);
                        rd.setIndexingDate(new Date());
                        // Get general document metadata
                        Map<String,String[]> metadataMap = confContent.getMetadata();
                        Logging.connectors.info("meta data : " + metadataMap);

                        for (Entry<String, String[]> entry : metadataMap.entrySet()) {
                          rd.addField(entry.getKey(), entry.getValue());
                        }
                        rd.addField("mimeType", mimeType);
                        rd.addField("author", confContent.getCreatedByDisplayName());
                        String documentURI = confContent.getWebURL();
                        String document = confContent.getContent();
                        try {
                          byte[] documentBytes = document.getBytes(StandardCharsets.UTF_8);
                          InputStream is = new ByteArrayInputStream(documentBytes);
                          try {
                            rd.setBinary(is, documentBytes.length);

                            //adding size
                            //rd.addField("size_kb", (documentBytes.length/1024)+"");
                            rd.addField("size", documentBytes.length+"");

                            activities.ingestDocumentWithException(nodeId, version, documentURI, rd);
                            // No errors.  Record the fact that we made it.
                            errorCode = "OK";
                            fileSize = new Long(documentBytes.length);
                          } finally {
                            is.close();
                          }
                        } catch (IOException e) {
                          handleIOException(e);
                        }
                    } else{
                        activities.deleteDocument(nodeId);
                    }

                    ////
				}
			} finally {
				if (doLog)
					activities.recordActivity(new Long(startTime),
							ACTIVITY_READ, fileSize, nodeId, errorCode,
							errorDesc, null);
			}
		}
	}


    /**
     * This method returns the version of the current docs.
     * The version will be calculated based on the last modified date.
     * @param documentIdentifiers
     * @param spec
     * @return
     * @throws org.apache.manifoldcf.core.interfaces.ManifoldCFException
     * @throws org.apache.manifoldcf.agents.interfaces.ServiceInterruption
     */
	@Override
	public String[] getDocumentVersions(String[] documentIdentifiers,
			DocumentSpecification spec) throws ManifoldCFException,
			ServiceInterruption {
	    String[] rval = new String[documentIdentifiers.length];
	    for (int i = 0; i < rval.length; i++) {
	      String nodeId = documentIdentifiers[i];
	        ConfluenceContent confluenceContent = getContent(nodeId);
	        Date lastModificationDate = confluenceContent.getLastModified();
	        if (lastModificationDate != null) {
	           rval[i]=Long.toString(lastModificationDate.getTime());
	        } else {
	          rval[i] = null;
	        }
	    }
	    return rval;
	}


	/**
	 * Grab forced acl out of document specification. (wont be used now)
	 * @param spec
	 * @return
	 */
	protected static String[] getAcls(DocumentSpecification spec) {
		Set<String> map = new HashSet<String>();
		for (int i = 0; i < spec.getChildCount(); i++) {
			SpecificationNode sn = spec.getChild(i);
			if (sn.getType().equals(JOB_ACCESS_NODE_TYPE)) {
				String token = sn.getAttributeValue(JOB_TOKEN_ATTRIBUTE);
				map.add(token);
			} else if (sn.getType().equals(JOB_SECURITY_NODE_TYPE)) {
				String onOff = sn.getAttributeValue(JOB_VALUE_ATTRIBUTE);
				if (onOff != null && onOff.equals("on"))
					return null;
			}
		}

		String[] rval = new String[map.size()];
		Iterator<String> iter = map.iterator();
		int i = 0;
		while (iter.hasNext()) {
			rval[i++] = (String) iter.next();
		}
		return rval;
	}

	/**
	 * Check if connection with confluence is successful
	 * @throws org.apache.manifoldcf.core.interfaces.ManifoldCFException
	 * @throws org.apache.manifoldcf.agents.interfaces.ServiceInterruption
	 */
	protected void checkConnection() throws ManifoldCFException,
			ServiceInterruption {
		CheckConnectionThread t = new CheckConnectionThread(getSession());
		try {
			t.start();
			t.finishUp();
			return;
		} catch (InterruptedException e) {
			t.interrupt();
			throw new ManifoldCFException("Interrupted: " + e.getMessage(), e,
					ManifoldCFException.INTERRUPTED);
		} catch (java.net.SocketTimeoutException e) {
			handleIOException(e);
		} catch (InterruptedIOException e) {
			t.interrupt();
			handleIOException(e);
		} catch (IOException e) {
			handleIOException(e);
		} catch (ResponseException e) {
			handleResponseException(e);
		}
	}

	private static void handleIOException(IOException e)
			throws ManifoldCFException, ServiceInterruption {
		if (!(e instanceof java.net.SocketTimeoutException)
				&& (e instanceof InterruptedIOException)) {
			throw new ManifoldCFException("Interrupted: " + e.getMessage(), e,
					ManifoldCFException.INTERRUPTED);
		}
		Logging.connectors.warn(" IO exception: " + e.getMessage(), e);
		long currentTime = System.currentTimeMillis();
		throw new ServiceInterruption("IO exception: " + e.getMessage(), e,
				currentTime + 300000L, currentTime + 3 * 60 * 60000L, -1, false);
	}

	private static void handleResponseException(ResponseException e)
			throws ManifoldCFException, ServiceInterruption {
		Logging.connectors.error("Response exception " + e.getMessage());
		throw new ManifoldCFException("Unexpected response: " + e.getMessage(),
				e);
	}

	protected static class CheckConnectionThread extends Thread {

		protected final ConfluenceSession session;
		protected Throwable exception = null;

		public CheckConnectionThread(ConfluenceSession session) {
			super();
			this.session = session;
			setDaemon(true);
		}

		public void run() {
			try {
				session.getRepositoryInfo();
			} catch (Throwable e) {
				this.exception = e;
			}
		}

		public void finishUp() throws InterruptedException, IOException,
                ResponseException {
			join();
			Throwable thr = exception;
			if (thr != null) {
				if (thr instanceof IOException) {
					throw (IOException) thr;
				} else if (thr instanceof ResponseException) {
					throw (ResponseException) thr;
				} else if (thr instanceof RuntimeException) {
					throw (RuntimeException) thr;
				} else {
					throw (Error) thr;
				}
			}
		}
	}

	protected static class GetSeedsThread extends Thread {

		protected Throwable exception = null;
		protected final ConfluenceSession session;
		protected final String confDriveQuery;
		protected final XThreadStringBuffer seedBuffer;

		public GetSeedsThread(ConfluenceSession session, String confDriveQuery) {
			super();
			this.session = session;
			this.confDriveQuery = confDriveQuery;
			this.seedBuffer = new XThreadStringBuffer();
			setDaemon(true);
		}

		@Override
		public void run() {
			try {
				Logging.connectors.info("Getting seeds");
				session.getSeeds(seedBuffer, confDriveQuery);
			} catch (Throwable e) {
				this.exception = e;
			} finally {
				seedBuffer.signalDone();
			}
		}

		public XThreadStringBuffer getBuffer() {
			return seedBuffer;
		}

		public void finishUp() throws InterruptedException, IOException,
                ResponseException {
			seedBuffer.abandon();
			join();
			Throwable thr = exception;
			if (thr != null) {
				if (thr instanceof IOException)
					throw (IOException) thr;
				else if (thr instanceof ResponseException)
					throw (ResponseException) thr;
				else if (thr instanceof RuntimeException)
					throw (RuntimeException) thr;
				else if (thr instanceof Error)
					throw (Error) thr;
				else
					throw new RuntimeException("Unhandled exception of type: "
							+ thr.getClass().getName(), thr);
			}
		}
	}

	protected ConfluenceContent getContent(String contentId)
			throws ManifoldCFException, ServiceInterruption {
		GetContentThread t = new GetContentThread(getSession(), contentId);
		try {
			t.start();
			t.finishUp();
		} catch (InterruptedException e) {
			t.interrupt();
			throw new ManifoldCFException("Interrupted: " + e.getMessage(), e,
					ManifoldCFException.INTERRUPTED);
		} catch (java.net.SocketTimeoutException e) {
			handleIOException(e);
		} catch (InterruptedIOException e) {
			t.interrupt();
			handleIOException(e);
		} catch (IOException e) {
			handleIOException(e);
		} catch (ResponseException e) {
			handleResponseException(e);
		}
		return t.getResponse();
	}

	protected static class GetContentThread extends Thread {

		protected final ConfluenceSession session;
		protected final String nodeId;
		protected Throwable exception = null;
		protected ConfluenceContent response = null;

		public GetContentThread(ConfluenceSession session, String nodeId) {
			super();
			setDaemon(true);
			this.session = session;
			this.nodeId = nodeId;
		}

		public void run() {
			try {
				response = session.getContent(nodeId);
			} catch (Throwable e) {
				this.exception = e;
			}
		}

		public ConfluenceContent getResponse() {
			return response;
		}

		public void finishUp() throws InterruptedException, IOException,
                ResponseException {
			join();
			Throwable thr = exception;
			if (thr != null) {
				if (thr instanceof IOException) {
					throw (IOException) thr;
				} else if (thr instanceof ResponseException) {
					throw (ResponseException) thr;
				} else if (thr instanceof RuntimeException) {
					throw (RuntimeException) thr;
				} else {
					throw (Error) thr;
				}
			}
		}
	}

	protected static class GetUsersThread extends Thread {

		protected final ConfluenceSession session;
		protected final String contentId;
		protected Throwable exception = null;
		protected List<String> result = null;

		public GetUsersThread(ConfluenceSession session, String contentId) {
			super();
			this.session = session;
			this.contentId = contentId;
			setDaemon(true);
		}

		public void run() {
			try {
				result = session.getUsers(contentId);
			} catch (Throwable e) {
				this.exception = e;
			}
		}

		public void finishUp() throws InterruptedException, IOException,
                ResponseException {
			join();
			Throwable thr = exception;
			if (thr != null) {
				if (thr instanceof IOException) {
					throw (IOException) thr;
				} else if (thr instanceof ResponseException) {
					throw (ResponseException) thr;
				} else if (thr instanceof RuntimeException) {
					throw (RuntimeException) thr;
				} else {
					throw (Error) thr;
				}
			}
		}

		public List<String> getResult() {
			return result;
		}

	}


	/**
	 * (This wont be called), this will used in conjunction with permission and Authority repository implementation
	 * @param contentId
	 * @return
	 * @throws org.apache.manifoldcf.core.interfaces.ManifoldCFException
	 * @throws org.apache.manifoldcf.agents.interfaces.ServiceInterruption
	 */
	protected List<String> getUsers(String contentId)
			throws ManifoldCFException, ServiceInterruption {
		GetUsersThread t = new GetUsersThread(getSession(), contentId);
		try {
			t.start();
			t.finishUp();
			return t.getResult();
		} catch (InterruptedException e) {
			t.interrupt();
			throw new ManifoldCFException("Interrupted: " + e.getMessage(), e,
					ManifoldCFException.INTERRUPTED);
		} catch (java.net.SocketTimeoutException e) {
			handleIOException(e);
		} catch (InterruptedIOException e) {
			t.interrupt();
			handleIOException(e);
		} catch (IOException e) {
			handleIOException(e);
		} catch (ResponseException e) {
			handleResponseException(e);
		}
		return null;
	}

}
