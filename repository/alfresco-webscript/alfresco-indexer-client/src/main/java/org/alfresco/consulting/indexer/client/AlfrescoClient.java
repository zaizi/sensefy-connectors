package org.alfresco.consulting.indexer.client;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface AlfrescoClient {
  /**
   * Fetches nodes from Alfresco which has changed since the provided timestamp.
   *
   * @param lastAclChangesetId
   *         the id of the last ACL changeset already being indexed; it can be considered a "startFrom" param
   * @param lastTransactionId
   *         the id of the last transaction already being indexed; it can be considered a "startFrom" param
   * @return an {@link AlfrescoResponse}
   */
  AlfrescoResponse fetchNodes(long lastTransactionId, long lastAclChangesetId, AlfrescoFilters filters) throws
      AlfrescoDownException;
  
  /**
   * Fetches Node Info from Alfresco for a given node.
   * @param nodeUuid the UUID for the node
   * @return an {@link AlfrescoResponse}
   * @throws AlfrescoDownException
   */
  AlfrescoResponse fetchNode(String nodeUuid) throws AlfrescoDownException;
  
  /**
   * Fetches metadata from Alfresco for a given node.
   * @param nodeUuid
   *        the UUID for the node
   * @return a map with metadata created from a json object
   */
  Map<String, Object> fetchMetadata(String nodeUuid) throws AlfrescoDownException;

  /**
   * Fetches authorities for the provided username.
   * @param username
   * @return an {@link AlfrescoUser}
   */
  AlfrescoUser fetchUserAuthorities(String username) throws AlfrescoDownException;

    /**
     * Verify the connectivity with Alfresco
     * @return
     * @throws AlfrescoDownException
     */
    public boolean checkConnection() throws AlfrescoConnectionException;


  /**
   * Fetches authorities for all users.
   * @return a list of {@link AlfrescoUser}
   */
  List<AlfrescoUser> fetchAllUsersAuthorities() throws AlfrescoDownException;
  
  /**
   * Fetches Document Binary Content
   * 
   * @param contentUrlPath URL of the content
   * @return Document Binary Content
   */
  InputStream fetchContent(String contentUrlPath);

   String getChangesUrl();

   String getActionsUrl();

   String getAuthoritiesUrl();

   String getMetadataUrl();
}
