package org.apache.manifoldcf.crawler.connectors.confluence;


/**
 * Rest exception object
 * @author kgunaratnam
 *
 */
public class ResponseException extends Exception {

  public ResponseException(String msg) {
    super(msg);
  }
  
  public ResponseException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
