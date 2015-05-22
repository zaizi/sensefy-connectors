package org.apache.manifoldcf.authorities.authorities.alfresco;

/**
 * Parameters data for the Alfresco Authority connector.
 * 
 * @author aayala
 * 
 */
public class AlfrescoConfig
{

    /**
     * Username parameter
     */
    public static final String USERNAME_PARAM = "username";

    /**
     * Password parameter
     */
    public static final String PASSWORD_PARAM = "password";

    /**
     * Protocol parameter
     */
    public static final String PROTOCOL_PARAM = "protocol";

    /**
     * Server name parameter
     */
    public static final String SERVER_PARAM = "server";

    /**
     * Port parameter
     */
    public static final String PORT_PARAM = "port";

    /**
     * Parameter for the path of the context of the Alfresco WebScripts API
     */
    public static final String PATH_PARAM = "path";

    // default values
    public static final String USERNAME_DEFAULT_VALUE = "admin";
    public static final String PASSWORD_DEFAULT_VALUE = "admin";
    public static final String PROTOCOL_DEFAULT_VALUE = "http";
    public static final String SERVER_DEFAULT_VALUE = "localhost";
    public static final String PORT_DEFAULT_VALUE = "8080";
    public static final String PATH_DEFAULT_VALUE = "/alfresco-instance";

}
