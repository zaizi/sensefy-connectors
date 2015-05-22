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

package org.apache.manifoldcf.agents.transformation.redlink.profiles;

import io.redlink.sdk.RedLink;
import io.redlink.sdk.impl.analysis.model.Entity;
import io.redlink.sdk.impl.data.model.LDPathResult;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.marmotta.client.model.rdf.RDFNode;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * Naive Dataset Management
 * 
 * @author Rafa Haro <rharo@zaizi.com>
 *
 */
class CustomRedLinkEntityProfile extends AbstractRedLinkEntityProfile {
	
	public static final String DEFAULT_LABEL_PROPERTY = "http://www.w3.org/2000/01/rdf-schema#label";
	public static final String DEFAULT_TYPE_PROPERTY = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
	public static final String DEFAULT_SUBCLASS_PROPERTY = "http://www.w3.org/2000/01/rdf-schema#subClassOf";
	public static final String DEFAULT_LANGUAGE = "en";
	

	CustomRedLinkEntityProfile(Entity entity, RedLink.Data data) {
		super(entity, data);
	}

	@Override
	public String getDatasetName() {
		return entity.getDataset();
	}

	@Override
	public Multimap<String, String> getDefaultProperties() {
		return getAllPropertiesValues();
	}

	@Override
	public Multimap<String, String> getHierarchy() {
		
		List<String> hierarchy = Lists.newArrayList(); 
		String expression = "broader = rdfs:subClassOf :: xsd:anyURI ;";
		LDPathResult results = data.ldpath(entity.getUri(),
				entity.getDataset(), expression);
		List<RDFNode> nodes = results.getResults("broader");
		if(nodes != null && !nodes.isEmpty())
			hierarchy.add(nodes.get(0).toString());

		Multimap<String, String> result = HashMultimap.create();
		result.putAll(entity.getUri(), hierarchy);
		return result;
	}

	@Override
	public Collection<String> getLabels() {
		return entity.getValues(DEFAULT_LABEL_PROPERTY);
	}
	
	@Override
	public Multimap<String, String> getLabelsByLanguage() {
		return entity.getValuesByLanguage(DEFAULT_LABEL_PROPERTY);
	}

	@Override
	public String getPreferredLabel() {
		return entity.getValue(DEFAULT_LABEL_PROPERTY, DEFAULT_LANGUAGE);
	}
	
	@Override
	public Collection<String> getTypes() {
		return entity.getValues(DEFAULT_TYPE_PROPERTY);
	}

	@Override
	public String getPreferredType() {
		return entity.getValue(DEFAULT_TYPE_PROPERTY, DEFAULT_LANGUAGE);
	}
	
	@Override
	public Multimap<String, String> getTypesLabels() {
		return HashMultimap.create();
	}

	@Override
	public Map<String, String> getDefaultLDPath() {
		return Collections.emptyMap();
	}
}
