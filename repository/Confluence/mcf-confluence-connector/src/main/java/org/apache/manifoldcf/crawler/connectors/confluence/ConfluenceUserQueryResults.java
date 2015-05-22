package org.apache.manifoldcf.crawler.connectors.confluence;

import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Zaizi (pvt) Ltd
 * @author kgunaratnam
 *
 */
public class ConfluenceUserQueryResults extends ConfluenceJSONResponse{

	// Specific keys we care about
	private final static String KEY_NAME = "name";

	public ConfluenceUserQueryResults() {
		super();
		// TODO Auto-generated constructor stub
	}

	public void getNames(List<String> nameBuffer) {
		JSONArray users = (JSONArray) object;
		for (Object user : users) {
			if (user instanceof JSONObject) {
				JSONObject jo = (JSONObject) user;
				nameBuffer.add(jo.get(KEY_NAME).toString());
			}
		}
	}

}
