package org.alfresco.consulting.indexer.client;

public class AlfrescoConnectionException extends RuntimeException {
  public AlfrescoConnectionException() {
    super();
  }

  public AlfrescoConnectionException(String s) {
    super(s);
  }

  public AlfrescoConnectionException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public AlfrescoConnectionException(Throwable throwable) {
    super(throwable);
  }
}
