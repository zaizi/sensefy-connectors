package org.apache.manifoldcf.crawler.connectors.confluence;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.manifoldcf.core.common.InterruptibleSocketFactory;
import org.apache.manifoldcf.core.common.XThreadStringBuffer;
import org.apache.manifoldcf.core.interfaces.KeystoreManagerFactory;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.util.URLEncoder;
import org.apache.manifoldcf.crawler.system.Logging;
import org.json.simple.JSONValue;

/**
 * Zaizi (pvt) Ltd
 * @author kgunaratnam
 *
 */
public class ConfluenceSession {

	private final HttpHost host;
	private final String path;
	private final String clientId;
	private final String clientSecret;

	private HttpClientConnectionManager connectionManager;
	private HttpClient httpClient;

	// Current host name
	private static String currentHost = null;

	static {
		// Find the current host name
		try {
			java.net.InetAddress addr = java.net.InetAddress.getLocalHost();

			// Get hostname
			currentHost = addr.getHostName();
		} catch (java.net.UnknownHostException e) {
		}
	}

	public ConfluenceSession(String clientId, String clientSecret,
			String protocol, String host, int port, String path,
			String proxyHost, int proxyPort, String proxyDomain,
			String proxyUsername, String proxyPassword)
			throws ManifoldCFException {
		this.host = new HttpHost(host, port, protocol);
		this.path = path;
		this.clientId = clientId;
		this.clientSecret = clientSecret;

		int socketTimeout = 900000;
		int connectionTimeout = 60000;

		javax.net.ssl.SSLSocketFactory httpsSocketFactory = KeystoreManagerFactory
				.getTrustingSecureSocketFactory();
		SSLConnectionSocketFactory myFactory = new SSLConnectionSocketFactory(
				new InterruptibleSocketFactory(httpsSocketFactory,
						connectionTimeout),
				SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

		connectionManager = new PoolingHttpClientConnectionManager();

		CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

		// If authentication needed, set that
		if (clientId != null) {
			credentialsProvider.setCredentials(AuthScope.ANY,
					new UsernamePasswordCredentials(clientId, clientSecret));
		}

		RequestConfig.Builder requestBuilder = RequestConfig.custom()
				.setCircularRedirectsAllowed(true)
				.setSocketTimeout(socketTimeout)
				.setStaleConnectionCheckEnabled(true)
				.setExpectContinueEnabled(true)
				.setConnectTimeout(connectionTimeout)
				.setConnectionRequestTimeout(socketTimeout);

		// If there's a proxy, set that too.
		if (proxyHost != null && proxyHost.length() > 0) {

			// Configure proxy authentication
			if (proxyUsername != null && proxyUsername.length() > 0) {
				if (proxyPassword == null)
					proxyPassword = "";
				if (proxyDomain == null)
					proxyDomain = "";

				credentialsProvider.setCredentials(new AuthScope(proxyHost,
						proxyPort), new NTCredentials(proxyUsername,
						proxyPassword, currentHost, proxyDomain));
			}

			HttpHost proxy = new HttpHost(proxyHost, proxyPort);
			requestBuilder.setProxy(proxy);
		}

		httpClient = HttpClients
				.custom()
				.setConnectionManager(connectionManager)
				.setMaxConnTotal(1)
				.disableAutomaticRetries()
				.setDefaultRequestConfig(requestBuilder.build())
				.setDefaultSocketConfig(
						SocketConfig.custom().setTcpNoDelay(true)
								.setSoTimeout(socketTimeout).build())
				.setDefaultCredentialsProvider(credentialsProvider)
				.setSSLSocketFactory(myFactory)
				.setRequestExecutor(new HttpRequestExecutor(socketTimeout))
				.setRedirectStrategy(new DefaultRedirectStrategy()).build();
	}

	public void close() {
		httpClient = null;
		if (connectionManager != null)
			connectionManager.shutdown();
		connectionManager = null;
	}

	private static Object convertToJSON(HttpResponse httpResponse)
			throws IOException {
		HttpEntity entity = httpResponse.getEntity();
		if (entity != null) {
			InputStream is = entity.getContent();
			try {
				Reader r = new InputStreamReader(is, getCharSet(entity));
				return JSONValue.parse(r);
			} finally {
				is.close();
			}
		}
		return null;
	}

	private static String convertToString(HttpResponse httpResponse)
			throws IOException {
		HttpEntity entity = httpResponse.getEntity();
		if (entity != null) {
			InputStream is = entity.getContent();
			try {
				char[] buffer = new char[65536];
				Reader r = new InputStreamReader(is, getCharSet(entity));
				Writer w = new StringWriter();
				try {
					while (true) {
						int amt = r.read(buffer);
						if (amt == -1)
							break;
						w.write(buffer, 0, amt);
					}
				} finally {
					w.flush();
				}
				return w.toString();
			} finally {
				is.close();
			}
		}
		return "";
	}

	private static Charset getCharSet(HttpEntity entity) {
		Charset charSet;
		try {
			ContentType ct = ContentType.get(entity);
			if (ct == null)
				charSet = StandardCharsets.UTF_8;
			else
				charSet = ct.getCharset();
		} catch (ParseException e) {
			charSet = StandardCharsets.UTF_8;
		}

		// assign UTF-8 as standard charset if no char set is inferred
		if (charSet == null) {
			charSet = StandardCharsets.UTF_8;
		}
		Logging.connectors.info("Charset : " + charSet.toString());
		return charSet;
	}

	public Map<String, String> getRepositoryInfo() throws IOException,
			ResponseException {
		HashMap<String, String> statistics = new HashMap<String, String>();
		ConfluenceQueryResults qr = new ConfluenceQueryResults();
		
		getRest("?limit=1", qr);//just limit one for checking the connection
		String msg = "Total content"+ qr.getTotal().toString();
		Logging.connectors.info(msg);
		statistics.put("Total content",qr.getTotal().toString());
		return statistics;
	}

	
	/**
	 * Get all seeds until no next link is visible
	 * @param idBuffer
	 * @param jiraDriveQuery
	 * @throws IOException
	 * @throws ResponseException
	 * @throws InterruptedException
	 * @throws ManifoldCFException
	 */
	public void getSeeds(XThreadStringBuffer idBuffer, String jiraDriveQuery)
			throws IOException, ResponseException, InterruptedException,
			ManifoldCFException {
		long startAt = 0L;
		long setSize = 500L;
		long totalAmt = 0L;
		
		String nextLink = "";
		do {
			ConfluenceQueryResults qr = new ConfluenceQueryResults();
			getRest("?space=*&limit="
					+ setSize + "&start="+ startAt, qr);// removed the query
			Long total = qr.getTotal();
			nextLink = qr.getNextLink();
			if (total == null)
				return;
			totalAmt += total.longValue();
			qr.pushIds(idBuffer);
			//somehow the limit size reset in response, hence i take the lowest for the start
			startAt +=  total >0 ? total : setSize;
			Logging.connectors
			.info("next link is : "+nextLink + ", total amount :"+ totalAmt + ", start at next is : "+ startAt);
		} while (nextLink != null && StringUtils.isNotEmpty(nextLink) );

		Logging.connectors
				.info("Seeding documents is finished total documents seeded : "
						+ totalAmt);
	}

	
	/**
	 * Get rest response and fill the query results
	 * @param rightside
	 * @param response
	 * @throws IOException
	 * @throws ResponseException
	 */
	private void getRest(String rightside, ConfluenceJSONResponse response)
			throws IOException, ResponseException {

		// Create AuthCache instance
		AuthCache authCache = new BasicAuthCache();
		// Generate BASIC scheme object and add it to the local
		// auth cache
		BasicScheme basicAuth = new BasicScheme();
		authCache.put(host, basicAuth);

		// Add AuthCache to the execution context
		HttpClientContext localContext = HttpClientContext.create();
		localContext.setAuthCache(authCache);

		String executionUrl = host.toURI() + path + ( path.endsWith("/") ? "" : "/")
				+ rightside;
		Logging.connectors.info("Execution url is :" + executionUrl);
		final HttpRequestBase method = new HttpGet(executionUrl);
		method.addHeader("Accept", "application/json");

		try {
			HttpResponse httpResponse = httpClient
					.execute(method, localContext);
			int resultCode = httpResponse.getStatusLine().getStatusCode();
			if(resultCode == 200){
				Logging.connectors.info("Successfully retrived response");
				Object jo = convertToJSON(httpResponse);
				response.acceptJSONObject(jo);
			}
			else if(resultCode == 401){
				throw new IOException("There is Authentication failure, this may be due to capcha : "+ convertToString(httpResponse));
			}			
			else {
				throw new IOException("Unexpected result code " + resultCode
						+ ": " + convertToString(httpResponse));
			}
			
		} finally {
			method.abort();
		}
	}

	
	/**
	 * Get the content of data
	 * @param contentId
	 * @return
	 * @throws IOException
	 * @throws ResponseException
	 * @throws ManifoldCFException
	 */
	public ConfluenceContent getContent(String contentId) throws IOException,
			ResponseException, ManifoldCFException {
		ConfluenceContent ji = new ConfluenceContent();
		//get the document
		getRest(URLEncoder.encode(contentId)
				+ "?expand=body.view,metadata.labels,space,history,version", ji);
		return ji;
	}

	
	
	/**
	 * support for confluence api for user restriction is not available at the moment
	 * @param issueKey
	 * @return
	 * @throws IOException
	 * @throws ResponseException
	 * @throws ManifoldCFException
	 */
	public List<String> getUsers(String issueKey) throws IOException,
			ResponseException, ManifoldCFException {
		List<String> rval = new ArrayList<String>();
		long startAt = 0L;
		long setSize = 100L;
		while (true) {
			ConfluenceUserQueryResults qr = new ConfluenceUserQueryResults();
			getRest("user/viewissue/search?username=&issueKey="
					+ URLEncoder.encode(issueKey) + "&maxResults=" + setSize
					+ "&startAt=" + startAt, qr);
			qr.getNames(rval);
			startAt += setSize;
			if (rval.size() < startAt)
				break;
		}
		return rval;
	}

}
