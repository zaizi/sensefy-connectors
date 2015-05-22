package org.apache.manifoldcf.crawler.connectors.confluence;

/**
 * Zaizi (pvt) Ltd
 * @author kgunaratnam
 *
 */
public class ConfluenceConfig {
	public static final String CLIENT_ID_PARAM = "clientid";
	public static final String CLIENT_SECRET_PARAM = "clientsecret";
	public static final String CONF_PROTOCOL_PARAM = "confprotocol";
	public static final String CONF_HOST_PARAM = "confhost";
	public static final String CONF_PORT_PARAM = "confport";
	public static final String CONF_PATH_PARAM = "confpath";

	public static final String CONF_PROXYHOST_PARAM = "confproxyhost";
	public static final String CONF_PROXYPORT_PARAM = "confproxyport";
	public static final String CONF_PROXYDOMAIN_PARAM = "confproxydomain";
	public static final String CONF_PROXYUSERNAME_PARAM = "confproxyusername";
	public static final String CONF_PROXYPASSWORD_PARAM = "confproxypassword";

	public static final String CONF_QUERY_PARAM = "confquery";

	public static final String CLIENT_ID_DEFAULT = "";
	public static final String CLIENT_SECRET_DEFAULT = "";
	public static final String CONF_PROTOCOL_DEFAULT = "http";
	public static final String CONF_HOST_DEFAULT = "";
	public static final String CONF_PORT_DEFAULT = "";
	public static final String CONF_PATH_DEFAULT = "/confluence/rest/api/content/";

	public static final String CONF_PROXYHOST_DEFAULT = "";
	public static final String CONF_PROXYPORT_DEFAULT = "";
	public static final String CONF_PROXYDOMAIN_DEFAULT = "";
	public static final String CONF_PROXYUSERNAME_DEFAULT = "";
	public static final String CONF_PROXYPASSWORD_DEFAULT = "";

	//will not be used for confluence //todo for future
	public static final String CONF_QUERY_DEFAULT = "";
}
