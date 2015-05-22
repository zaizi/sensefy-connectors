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

import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import io.redlink.sdk.RedLink;
import io.redlink.sdk.impl.analysis.model.Entity;
import io.redlink.sdk.impl.data.model.LDPathResult;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.manifoldcf.agents.transformation.redlink.config.RedLinkLDPathConfig;
import org.apache.marmotta.client.model.rdf.Literal;
import org.apache.marmotta.client.model.rdf.RDFNode;
import org.apache.marmotta.client.model.rdf.URI;
import org.openrdf.model.impl.URIImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

//import org.apache.commons.validator.routines.UrlValidator;
//import org.apache.manifoldcf.agents.transformation.redlink.RedLinkConfig;
//import org.apache.manifoldcf.agents.transformation.redlink.RedLinkLDPathConfig;
//import org.openrdf.model.impl.URIImpl;

/**
 * This class is a wrapper for easing the work with Freebase entities in the RedLink Processor Connector
 *
 * @author Rafa Haro <rharo@zaizi.com>
 */
final class FreebaseRedLinkEntityProfile extends AbstractRedLinkEntityProfile
{

	public static final Map<String, String> FREEBASE_NAMESPACES =
			ImmutableMap.of( "fb", "http://rdf.freebase.com/ns/" );

	public static final String LDPATH_PREFIX = "@prefix fb : <" +
			FREEBASE_NAMESPACES.get("fb") + ">;";

	public static final ImmutableMap<String, String> FREEBASE_COMMON_PROPERTIES_URIS =
			ImmutableMap.<String, String>builder().
			put("website", "http://rdf.freebase.com/ns/common.topic.official_website" ).
			put("description", "http://rdf.freebase.com/ns/common.topic.description" ).
			put("label", "http://www.w3.org/2000/01/rdf-schema#label" ).
			put("type", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" ).
			put("thumbnail", "http://rdf.freebase.com/ns/common.topic.image" ).
			build();

	public static final ImmutableSet<String> FREEBASE_COMMON_PROPERTIES =
			ImmutableSet.<String>builder().add( "website" ).add( "description" ).build();

	public static final String NOTABLE_FOR_PROPERTY = "notable_type";

	public static final ImmutableMap<String, String> FREEBASE_DEFAULT_LDPATH =
			ImmutableMap.<String, String>builder().
			put("categories", "fb:common.topic.notable_types/fb:type.object.name").
			put("freebase_types", "fb:type.object.type").
			//put("categories_map", "fb:type.object.type / fn:concat( . , \"-\" , fb:type.object.type/fb:type.object.name)").
			//put("hierarchy", "fb:type.object.type / fn:concat( . , \"-\" , fb:type.object.type/fb:freebase.type_hints.included_types)").
			//put("hierarchy", "fn:concat(rdf:type,\"-\",rdf:type/fb:freebase.type_hints.included_types)").
			put(NOTABLE_FOR_PROPERTY, "fb:common.topic.notable_for/fb:common.notable_for.display_name[@en]").
			build();



	public static ImmutableSet<String> getFreebaseCommonProperties()
	{
		return FREEBASE_COMMON_PROPERTIES;
	}

	public static final Collection<String> TYPES_BLACK_LIST =
			ImmutableSet.of( "http://rdf.freebase.com/ns/common.topic",
					"http://rdf.freebase.com/ns/base.ontologies.ontology_instance",
					"http://rdf.freebase.com/ns/user",
					RedLinkLDPathConfig.NAMESPACE + "freebase_types",
					RedLinkLDPathConfig.NAMESPACE + "categories");

	public static final String DATASET_NAME = "freebase";

	private static final String FB_GOOGLE_IMAGE_APIS = "https://usercontent.googleapis.com/freebase/v1/image/";

	public FreebaseRedLinkEntityProfile(Entity entity, RedLink.Data data)
	{
		super(entity, data);
	}

	@Override
	public Multimap<String, String> getAllPropertiesValues() {
		Multimap<String, String> result = HashMultimap.create();
		for(String property:entity.getProperties())
			if(!TYPES_BLACK_LIST.contains(property))
				result.putAll(property, entity.getValues(property));

		return result;
	}

	@Override
	public Multimap<String, String> getDefaultProperties() {
		Multimap<String, String> result = ArrayListMultimap.create();

		// Common Properties
		for ( Entry<String, String> entry : FREEBASE_COMMON_PROPERTIES_URIS.entrySet() )
		{
			if ( entry.getKey().equals( "thumbnail" ) )
			{
				Collection<String> thumbnails = entity.getValues( entry.getValue() );
				if ( thumbnails.size() > 0 )
				{
					Collection<String> processedThumbnails = processThumbnails( thumbnails );
					result.putAll( entry.getKey(), processedThumbnails );
				}
			}
			else
			{
				result.putAll( entry.getKey(), entity.getValues( entry.getValue() ) );
			}
		}

		/* Default LDPath
		for (String field:FREEBASE_DEFAULT_LDPATH.keySet()){
			String propertyURI = RedLinkLDPathConfig.NAMESPACE + field;
			result.putAll(field, entity.getValues(propertyURI));
		}  */

		return result;
	}

	@Override
	public Multimap<String, String> getHierarchy()
	{
		Multimap<String, String> result = HashMultimap.create();
		String propertyURI = RedLinkLDPathConfig.NAMESPACE + "freebase_types";
		Collection<String> types = entity.getValues(propertyURI);
		for(String type:types){
			String expression = "hierarchy = fb:freebase.type_hints.included_types :: xsd:anyURI ;";
		    LDPathResult results = data.ldpath(type,
					DATASET_NAME, LDPATH_PREFIX + expression);
			List<RDFNode> nodes = results.getResults("hierarchy");
			for(RDFNode node:nodes)
				result.put(type, node.toString());

		}

		return result;
	}

	/**
	 * Processes the list of thumbnails returning only one thumbnail, with the correct URL using Freebase APIs
	 *
	 * @param thumbnails
	 * @return
	 */
	private Collection<String> processThumbnails( Collection<String> thumbnails )
	{
		Collection<String> processedThumbnails = new ArrayList<String>();
		String s = thumbnails.iterator().next();
		String localName = getLocalName( s );
		localName = localName.replaceFirst( "\\.", "/" );
		String processed = FB_GOOGLE_IMAGE_APIS + localName;
		processedThumbnails.add( processed );
		return processedThumbnails;
	}

	@Override
	public String getDatasetName() {
		return DATASET_NAME;
	}

	@Override
	public Collection<String> getLabels() {
		Collection<String> labels = 
				entity.getValues(FREEBASE_COMMON_PROPERTIES_URIS.get("label"));
		if(labels.isEmpty()){  
			String expression = "labels = fb:type.object.name :: xsd:string ;";
			LDPathResult results = data.ldpath(entity.getUri(),
					DATASET_NAME, LDPATH_PREFIX + expression);
			List<RDFNode> nodes = results.getResults("labels");
			for(RDFNode node:nodes){
				if (node instanceof Literal)
					labels.add(((Literal) node).getContent());
				else
					labels.add(((URI) node).getUri());
			}
		}

		return labels;
	}

	@Override
	public Multimap<String, String> getLabelsByLanguage() {
		Multimap<String, String> labels = 
				entity.getValuesByLanguage(FREEBASE_COMMON_PROPERTIES_URIS.get("label"));
		if(labels.isEmpty()){  
			String expression = "labels = fb:type.object.name :: xsd:string ;";
			LDPathResult results = data.ldpath(entity.getUri(),
					DATASET_NAME, LDPATH_PREFIX + expression);
			List<RDFNode> nodes = results.getResults("labels");
			for(RDFNode node:nodes){
				if (node instanceof Literal){
					Literal labelLiteral = (Literal) node;
					labels.put(labelLiteral.getLanguage(), labelLiteral.getContent());
				}
			}
		}		
		return labels;
	}

	@Override
	public String getPreferredLabel() {
		String label = entity.getValue(
				FREEBASE_COMMON_PROPERTIES_URIS.get("label"), "en");
		if(label == null || label.isEmpty())
			label = getLabels().iterator().next();

		return label;
	}

	@Override
	public Collection<String> getTypes() {
		String propertyURI = RedLinkLDPathConfig.NAMESPACE + "categories";
		Collection<String> categories = entity.getValues(propertyURI);
		return Collections2.filter(categories,
				new Predicate<String>(){
			@Override
			public boolean apply(String input) {
				return TYPES_BLACK_LIST.contains(input);
			}
		});
	}

	@Override
	public String getPreferredType() {
		String propertyURI = RedLinkLDPathConfig.NAMESPACE + "notable_for";
		String type = entity.getFirstPropertyValue(propertyURI);
		return type;
	}

	@Override
	public Multimap<String, String> getTypesLabels() {
		Multimap<String, String> result = HashMultimap.create();
		String propertyURI = RedLinkLDPathConfig.NAMESPACE + "freebase_types";
		Collection<String> types = entity.getValues(propertyURI);
		for(String type:types){
			String expression = "labels = fb:type.object.name :: xsd:string ;";
			LDPathResult results = data.ldpath(type,
					DATASET_NAME, LDPATH_PREFIX + expression);
			List<RDFNode> nodes = results.getResults("labels");
			for(RDFNode node:nodes)
				if(node instanceof Literal)
					result.put(type, ((Literal) node).getContent());

		}
		return result;
	}

	@Override
	public Map<String, String> getDefaultLDPath() {
		return FREEBASE_DEFAULT_LDPATH;
	}


	private String getLocalName( String uri )
	{
		UrlValidator urlValidator = new UrlValidator();
		if ( urlValidator.isValid( uri ) )
		{
			URIImpl uriImpl = new URIImpl( uri );
			return uriImpl.getLocalName();
		}
		else
		{
			return new String( uri );
		}
	}
}
