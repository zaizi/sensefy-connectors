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

package org.apache.manifoldcf.agents.transformation.redlink.config;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.validator.routines.UrlValidator;
import org.openrdf.model.impl.URIImpl;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

/**
 * RedLink LDPath Configuration Representation
 * 
 * @author Rafa Haro <rharo@zaizi.com>
 *
 */
public class RedLinkLDPathConfig {
	
	private Table<Optional<String>, String, String> configuration = HashBasedTable.create();
	private Map<String, String> namespaces = Maps.newHashMap();

	/**
	 * RedLink LdPath Namespace
	 */
	public static final String NAMESPACE="http://sensefy.org/ldpath/custom/";
	
	public RedLinkLDPathConfig(){
		
	}
	
	RedLinkLDPathConfig(Map<String, Map<String, String>> ldpath) {
		for(Entry<String, Map<String, String>> entry:ldpath.entrySet()){
			for(Entry<String, String> fields:entry.getValue().entrySet()){
				configuration.put(Optional.of(entry.getKey()), fields.getKey(), fields.getValue());
			}
		}
	}
	
	public void addLdPathConfiguration(String fieldName, String ldPathExpression){
		configuration.put(Optional.<String>absent(), fieldName, ldPathExpression);
	}
	
	public void addLdPathConfigurations(Map<String, String> ldPath){
		for(Entry<String, String> entry:ldPath.entrySet())
			addLdPathConfiguration(entry.getKey(), entry.getValue());
	}

	public void addLdPathConfiguration(String entityType,
			String fieldName, String ldPathExpression){
		configuration.put(Optional.of(entityType), fieldName, ldPathExpression);
	}
	
	public void addNamespace(String prefix, String namespace){
		namespaces.put(prefix, namespace);
	}
	
	public void addNamespaces(Map<String, String> namespaces){
		this.namespaces.putAll(namespaces);
	}
	
	public String buildLdPathProgram(){
		
		StringBuilder builder = new StringBuilder();
		
		// Fixed Common Custom Namespace
		builder.append("@prefix sensefy :<" 
				+ NAMESPACE +">;");
		
		// Fixed prefix-namespace map
		for(Entry<String, String> prefix:namespaces.entrySet())
			builder.append("@prefix " + prefix.getKey()
					+ ": <"	+ prefix.getValue()	+ ">;");
		
		// Configured LDPath Prefixes
		Map<String,String> localNames = Maps.newHashMap();
		for(Optional<String> entityType:configuration.rowKeySet()){
			if(entityType.isPresent()){
				String localName = getLocalName(entityType.get());
				localNames.put(entityType.get(), localName);
				builder.append("@prefix " + localName
					+ ": <"	+ NAMESPACE+localName	+ "/>;");
			}
		}
		
		// Program Untyped Fields
		Map<String, String> untypedFields = configuration.row(Optional.<String>absent());		
		for(Entry<String, String> entry:untypedFields.entrySet()){
			String field = "sensefy:" + entry.getKey();
			builder.append(field+"="+entry.getValue()+";");
		}
		
		// Program Typed Fields
		for(Optional<String> entityType:configuration.rowKeySet())
			if(entityType.isPresent()){
				String prefix = localNames.get(entityType.get());
				for(Entry<String, String> fields:configuration.row(entityType).entrySet()){
					builder.append(prefix+":"+fields.getKey()+"="+fields.getValue()+";");
				}
			}
		
		return builder.toString();
	}
	
	public Collection<String> getEntityTypes(){
		return FluentIterable
			    .from(configuration.rowKeySet())
			    .transform(new Function<Optional<String>, String>() {
			       @Override
			       public String apply(final Optional<String> input) {
			         return input.orNull();
			       }
			     })
			    .filter(new Predicate<String>() {
			    	@Override
			       public boolean apply(final String input) {
			         return !Strings.isNullOrEmpty(input);
			       }
			     }).toSet();
	}
	
	public Multimap<String, String> getFieldsByType(){
		Multimap<String, String> result = ArrayListMultimap.create();
		for(Optional<String> key:configuration.rowKeySet()){
			if(key.isPresent())
				result.putAll(key.get(), configuration.row(key).keySet());
		}
		return result;
	}
	
	public Map<String, String> getPropertiesByFields(){
		Map<String,String> result = Maps.newHashMap();
		for(Optional<String> type:configuration.rowKeySet()){
			for(String field:configuration.row(type).keySet())
				if(type.isPresent())
					result.put(field, getPropertyURI(type.get(), field));
				else
					result.put(field, NAMESPACE + "/" + field);
		}
		return result;
	}
	
	public final static String getPropertyURI(String entityType, String fieldName){
		String localName = getLocalName(entityType);
		return NAMESPACE + localName + "/" + fieldName;
	}

	public final static String getLocalName(String uri){
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
