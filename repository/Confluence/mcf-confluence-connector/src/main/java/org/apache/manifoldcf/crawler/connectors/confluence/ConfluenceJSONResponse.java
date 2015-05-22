package org.apache.manifoldcf.crawler.connectors.confluence;

/**
 * Zaizi (pvt) Ltd
 * @author kgunaratnam
 *
 */
public class ConfluenceJSONResponse {

	protected Object object = null;

	public ConfluenceJSONResponse() {
	}

	/**
	 * Receive a parsed JSON object.
	 */
	public void acceptJSONObject(Object object) {
		this.object = object;
	}

}