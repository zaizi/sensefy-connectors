package org.apache.manifoldcf.crawler.connectors.confluence;

import java.io.IOException;

import org.apache.manifoldcf.core.common.XThreadStringBuffer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Zaizi (pvt) Ltd
 * @author kgunaratnam
 *
 */
public class ConfluenceQueryResults extends ConfluenceJSONResponse {

	//Confluence fields from where we should pick the values from
	private final static String SIZE = "size";
	private final static String RESULTS = "results";
	private final static String ID = "id";
	private final static String LINKS = "_links";

	public ConfluenceQueryResults() {
		super();
	}

	public Long getTotal() {
		return (Long) ((JSONObject) object).get(SIZE);
	}

	public String getNextLink() {
		JSONObject j = (JSONObject) ((JSONObject) object).get(LINKS);
		if (j != null)
			return (String) j.get("next");
		else
			return null;
	}

	public void pushIds(XThreadStringBuffer seedBuffer) throws IOException,
			InterruptedException {
		JSONArray contents = (JSONArray) ((JSONObject) object).get(RESULTS);
		for (Object content : contents) {
			if (content instanceof JSONObject) {
				JSONObject jo = (JSONObject) content;
				seedBuffer.add(jo.get(ID).toString());
			}
		}
	}

}
