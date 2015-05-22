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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.manifoldcf.agents.transformation.redlink.config.RedLinkLDPathConfig;
import org.apache.marmotta.client.model.rdf.RDFNode;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.SKOS;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

/**
 * Typical SKOS based Dataset Management
 * 
 * @author Rafa Haro <rharo@zaizi.com>
 *
 */
final class SKOSBasedRedLinkEntityProfile extends AbstractRedLinkEntityProfile {
	
	public static final Map<String, String> SKOS_NAMESPACES =
			ImmutableMap.of( "skos", "http://www.w3.org/2004/02/skos/core#" );

	public static final ImmutableMap<String, String> SKOS_COMMON_PROPERTIES_URIS =
			ImmutableMap.<String, String>builder().
			put("prefLabel", SKOS.PREF_LABEL.stringValue() ).
			put("type", RDF.TYPE.stringValue()).
			put("inScheme", SKOS.IN_SCHEME.stringValue() ).
			put("broader", SKOS.BROADER.stringValue()).
			put("narrower", SKOS.NARROWER.stringValue()).
			build();

	public static final ImmutableMap<String, String> SKOS_DEFAULT_LDPATH =
			ImmutableMap.<String, String>builder().
			put("broader", "skos:broader | ^skos:narrower").
			put("broaderTransitive", "(skos:broader | ^skos:narrower)+").
			put("narrower", "^skos:broader | skos:narrower").
			put("narrowerTransitive", "(^skos:broader | skos:narrower)+").
			put("related", "skos:related | skos:relatedMatch").
			build();
	
	public static final String DEFAULT_LANGUAGE = "en";
	public static final String DATASET_NAME = "SKOS";

	SKOSBasedRedLinkEntityProfile(Entity entity, RedLink.Data data) {
		super(entity, data);
	}

	@Override
	public String getDatasetName() {
		return entity.getDataset();
	}

	@Override
	public Multimap<String, String> getDefaultProperties() {
		Multimap<String, String> result = ArrayListMultimap.create();
        
		// Common Properties
		for(Entry<String, String> entry : SKOS_COMMON_PROPERTIES_URIS.entrySet())
			result.putAll( entry.getKey(), entity.getValues( entry.getValue() ) );
		
		return result;
	}

	@Override
	public Multimap<String, String> getHierarchy() {
		String propertyURI = RedLinkLDPathConfig.NAMESPACE + "broaderTransitive";
		Collection<String> hierarchy = entity.getValues(propertyURI);
		if(hierarchy.isEmpty()){
			String expression = "broader = skos:broader :: xsd:anyURI ;";
			LDPathResult results = data.ldpath(entity.getUri(),
					entity.getDataset(), expression);
			List<RDFNode> nodes = results.getResults("broader");
			if(nodes != null && !nodes.isEmpty())
				hierarchy.add(nodes.get(0).toString());
		}

		Multimap<String, String> result = HashMultimap.create();
		result.putAll(entity.getUri(), hierarchy);
		return result;
	}

	@Override
	public Collection<String> getLabels() {
		Collection<String> labels = entity.getValues(SKOS_COMMON_PROPERTIES_URIS.get("prefLabel"));
		return labels;
	}
	
	@Override
	public Multimap<String, String> getLabelsByLanguage() {
		return entity.getValuesByLanguage(SKOS_COMMON_PROPERTIES_URIS.get("prefLabel"));
	}

	@Override
	public String getPreferredLabel() {
		String label = entity.getValue(SKOS_COMMON_PROPERTIES_URIS.get("prefLabel"),
				DEFAULT_LANGUAGE);
		return label;
	}
	
	@Override
	public Collection<String> getTypes() {
		return entity.getValues(SKOS_COMMON_PROPERTIES_URIS.get("type"));
	}

	@Override
	public String getPreferredType() {
		return entity.getValue(SKOS_COMMON_PROPERTIES_URIS.get("type"), DEFAULT_LANGUAGE);
	}

	@Override
	public Map<String, String> getDefaultLDPath() {
		return SKOS_DEFAULT_LDPATH;
	}

	@Override
	public Multimap<String, String> getTypesLabels() {
		return HashMultimap.create();
	}
}
