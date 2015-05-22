package org.apache.manifoldcf.authorities.authorities.box;

public class BoxConfig
{
     
  /** Username */
  public static final String CLIENT_ID = "client_id";
 
  /** Password */
  public static final String CLIENT_SECRET = "client_secret";

  public static final String USERNAME = "username";

  public static final String PASSWORD = "password";

  public static final String AUTHORIZATION_URL = "https://app.box.com/api/oauth2/authorize";
  
  private final static String API_REFRESH_URL = "https://app.box.com/api/oauth2/token";
  
  public final static String API_URL = "https://api.box.com/2.0";

  public static final long SESSION_EXPIRATION_IN_MILLISECONDS = 3500000;
  
}
