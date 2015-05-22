/**
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements. See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.apache.manifoldcf.agents.transformation.redlink;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.manifoldcf.core.interfaces.Specification;
import org.apache.manifoldcf.core.interfaces.SpecificationNode;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * RedLink Connector Configuration Parameters
 * 
 * @author Rafa Haro <rharo@zaizi.com>
 *
 */
public class RedLinkConfig {

    
    /**************************************
     *     REDLINK OUTPUT SPECIFICATION   *
     **************************************/
    static final String NODE_API_KEY = "api_key";
    static final String NODE_ANALYSIS_NAME = "analysis_name";
    static final String NODE_COMMON_PARAMETERS = "common_parameters";
    static final String NODE_ENTITY_TYPE = "entity_type";
    static final String NODE_LDPATH = "ldpath";
    
    static final String ATTRIBUTE_API_KEY = "api_key_value";
    static final String ATTRIBUTE_ANALYSIS_NAME = "analysis_name_value";
    static final String ATTRIBUTE_COMMON_PARAMETERS = "common_parameters_value";
    
    static final String ATTRIBUTE_ENTITY_TYPE = "entity_type";
    static final String ATTRIBUTE_FIELD = "field";
    static final String ATTRIBUTE_LDPATH = "ldpath_expression";
  
    /************* CONFIGURATION FIELDS *************************/
    
    /**
     * RedLink API key
     */
    private String apikey;
    
    /**
     * 
     * RedLink Analysis Application Name
     */
    private String analysis;
    
    /**
     * Common Properties Flag
     */
    private boolean common = true;
    
    /**
     * LDPathConfiguration for RedLink Processor Connector Instance
     */
    private RedLinkLDPathConfig ldPathConfig;
    
    /**
     * Fixed Output Specification from Config Data 
     */
    private String outputSpecification;
    
    RedLinkConfig(Specification os) {
    	
    	Map<String, Map<String, String>> ldpath = Maps.newHashMap();

    	for (int i = 0; i < os.getChildCount(); i++) {
    		SpecificationNode sn = os.getChild(i);

    		if(sn.getType().equals(NODE_API_KEY)) {
    			this.apikey = sn.getAttributeValue(ATTRIBUTE_API_KEY);
    		} else if (sn.getType().equals(NODE_ANALYSIS_NAME)) {
    			this.analysis = sn.getAttributeValue(ATTRIBUTE_ANALYSIS_NAME);
    		} else if (sn.getType().equals(NODE_COMMON_PARAMETERS)) {
    			String commonStr = sn.getAttributeValue(ATTRIBUTE_COMMON_PARAMETERS);
    			if(commonStr != null)
    				this.common = Boolean.parseBoolean(commonStr);
    		} else if (sn.getType().equals(NODE_ENTITY_TYPE)){
    			String entityType = sn.getAttributeValue(ATTRIBUTE_ENTITY_TYPE);
    			Map<String, String> nextMap = Maps.newHashMap();
    			for(int j = 0; j < sn.getChildCount(); j++){
    				SpecificationNode next = sn.getChild(j);
    				if(next.getType().equals(NODE_LDPATH)){
    					nextMap.put(sn.getAttributeValue(ATTRIBUTE_FIELD),
    							sn.getAttributeValue(ATTRIBUTE_LDPATH));
    				}
    			}
    			ldpath.put(entityType, nextMap);
    		}
    	}
   	
    	// Creating LDPathConfig object from Configuration
		ldPathConfig = new RedLinkLDPathConfig(ldpath);
	
		String[] plainData = new String[4];
		plainData[0] = this.apikey;
		plainData[1] = this.analysis;
		plainData[2] = "" + this.common;		
		
		List<String> specList = Lists.newArrayList();
		for(Entry<String, Map<String, String>> spec:ldpath.entrySet()){
			String[] nextSpec = new String[2];
			nextSpec[0] = spec.getKey();
			List<String> nextFields = Lists.newArrayList();
			for(Entry<String, String> fields : spec.getValue().entrySet()){
				String[] map = new String[2];
				map[0] = fields.getKey();
				map[1] = fields.getValue();
				nextFields.add(StringUtils.join(map, '%'));
			}
			nextSpec[1] = StringUtils.join(nextFields,'^');
 			specList.add(StringUtils.join(nextSpec, '*'));
		}
 		plainData[3] = StringUtils.join(specList, '#');
		this.outputSpecification = StringUtils.join(plainData, '$');
	}
    
    public RedLinkConfig(String pipelineDescription) {
		this.outputSpecification = pipelineDescription;
		
		String[] plainData = StringUtils.split(pipelineDescription, '$');
		this.apikey = plainData[0];
		this.analysis = plainData[1];
		this.common = Boolean.parseBoolean(plainData[2]);
		
		String ldPath = plainData[3];
		Map<String, Map<String, String>> ldPathMap = Maps.newHashMap();
		String[] specs = StringUtils.split(ldPath, '#');
		for(String nextSpec:specs){
			String[] specDes = StringUtils.split(nextSpec, '*');
			String entityType = specDes[0];
			String fields = specDes[1];
			Map<String, String> fieldsMap = Maps.newHashMap();
			String[] maps = StringUtils.split(fields, '^');
			for(String nextMap:maps){
				String[] ldPathPair = StringUtils.split(nextMap, '%');
				fieldsMap.put(ldPathPair[0], ldPathPair[1]);
			}
			ldPathMap.put(entityType, fieldsMap);
		}
		
		this.ldPathConfig = new RedLinkLDPathConfig(ldPathMap);
	}

//	private void initLDPathConfig(ConfigParams params) {
//        String ldpathEntries = params.getParameter(LDPATH_ENTITY_TYPE);
//        if(ldpathEntries != null && !ldpathEntries.isEmpty()) {
//            try {
//                JSONObject jsonEntries = new JSONObject(ldpathEntries);
//                @SuppressWarnings("unchecked")
//                Iterator<String> it = jsonEntries.keys();
//                while(it.hasNext()) {
//                    String entityType = it.next();
//                    JSONArray fields = jsonEntries.getJSONArray(entityType);
//                    for(int i=0,len=fields.length();i<len;i++) {
//                        JSONObject field = fields.getJSONObject(i);
//                        if(field.getString("field") != null && field.getString("ldpath") != null) {
//                            ldPathConfig.addLdPathConfiguration(entityType, field.getString("field"), field.getString("ldpath"));
//                        }
//                    }
//                }
//            } catch (JSONException e) {
//                logger.debug("Error parsing Entity Types LDPath configuration");
//            }
//            
//        }
//        
//    }
    
    /**
     * 
     * @return Configured RedLink API key
     */
	String getApikey() {
		return apikey;
	}

	/**
	 * 
	 * @return Configured RedLink Analysis Application Name
	 */
	String getAnalysis() {
		return analysis;
	}

	/**
	 * 
	 * @return A flag indicating if common properties must be extracted
	 */
	boolean includeCommonProperties() {
		return common;
	}

	/**
	 * 
	 * @return LDPath Configuration for RedLink Processor Connector Instance
	 */
	RedLinkLDPathConfig getLdPathConfig() {
		return ldPathConfig;
	}

	@Override
	public String toString() {
		return outputSpecification;
	}
	
	
  
}
