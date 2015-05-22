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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.validator.routines.UrlValidator;
import org.openrdf.model.impl.URIImpl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * RedLink LDPath Configuration Representation
 * 
 * @author Rafa Haro <rharo@zaizi.com>
 *
 */
public class RedLinkLDPathConfig {

	private Map<String, Map<String, String>> configuration = Maps.newHashMap();
	
	/**
	 * RedLink LdPath Namespace
	 */
	static final String NAMESPACE="http://sensefy.org/ldpath/custom/";
	
	public RedLinkLDPathConfig(Map<String, Map<String, String>> ldpath) {
		this.configuration = ldpath;
	}

	void addLdPathConfiguration(String entityType,
			String fieldName, String ldPathExpression){
		if(configuration.containsKey(entityType))
			configuration.get(entityType).put(fieldName, ldPathExpression);
		else{
			Map<String, String> entry = Maps.newHashMap();
			entry.put(fieldName, ldPathExpression);
			configuration.put(entityType, entry);
		}
	}
	
	String buildLdPathProgram(Map<String, String> prefixes){
		StringBuilder builder = new StringBuilder();
		// Fixed passed prefix-namespace map
		if(prefixes != null)
			for(Entry<String, String> prefix:prefixes.entrySet())
				builder.append("@prefix " + prefix.getKey()
						+ ": <"	+ prefix.getValue()	+ ">;");
		// Configured LDPath Prefixes
		Map<String,String> localNames = Maps.newHashMap();
		for(String entityType:configuration.keySet()){
			String localName =getLocalName(entityType);
			localNames.put(entityType, localName);
			builder.append("@prefix " + localName
					+ ": <"	+ NAMESPACE+localName	+ "/>;");
		}
		
		//Program
		for(Entry<String, Map<String, String>> config:configuration.entrySet()){
			String prefix = localNames.get(config.getKey());
			for(Entry<String, String> field:config.getValue().entrySet())
				builder.append(prefix+":"+field.getKey()+"="+field.getValue()+";");
		}
		return builder.toString();
	}
	
	public Collection<String> getEntityTypes(){
		return Collections.unmodifiableSet(configuration.keySet());
	}
	
	public String getPropertyURI(String entityType, String fieldName){
		String localName = getLocalName(entityType);
		return NAMESPACE + localName + "/" + fieldName;
	}
	
	public Multimap<String, String> getFieldsByType(){
		Multimap<String, String> result = ArrayListMultimap.create();
		for(String key:configuration.keySet()){
			result.putAll(key, configuration.get(key).keySet());
		}
		return result;
	}
	
	public Map<String, String> getPropertiesByFields(){
		Map<String,String> result = Maps.newHashMap();
		for(String type:configuration.keySet()){
			for(String field:configuration.get(type).keySet())
				result.put(field, getPropertyURI(type, field));
		}
		return result;
	}

	/**
     * This method cause the mysterious "Silent Explosion, disabled at the moment
     * @param uri
     * @return    **/

	private String getLocalName(String uri){
            UrlValidator urlValidator = new UrlValidator();
            if( urlValidator.isValid( uri ))
            {
                URIImpl uriImpl = new URIImpl( uri );
                return uriImpl.getLocalName();
            }
		else
			return new String(uri);
    }

}
