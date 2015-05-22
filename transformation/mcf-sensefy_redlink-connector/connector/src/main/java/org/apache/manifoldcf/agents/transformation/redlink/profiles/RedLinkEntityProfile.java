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

import io.redlink.sdk.impl.analysis.model.Entity;

import java.util.Collection;
import java.util.Map;

import org.apache.manifoldcf.agents.transformation.redlink.config.RedLinkLDPathConfig;

import com.google.common.collect.Multimap;

/**
 * An Entity Profile should ease the management of custom datasets entities' information. Each profile knows how to extract from entity and text annotations 
 * the information required
 * 
 * @author Rafa Haro <rharo@zaizi.com>
 *
 */
public interface RedLinkEntityProfile {

	/**
	 * 
	 * @return Name of the Dataset managed by this profile
	 */
	public String getDatasetName();
	
	/**
	 * 
	 * @return Profiled {@link Entity}
	 */
	public Entity getEntity();
	
	/**
	 * 
	 * @return {@link Multimap} with all the properties registered for the profiled entity
	 */
	public Multimap<String, String> getAllPropertiesValues();
	
	/**
	 * 
	 * @return {@link Multimap} containing the most common properties of the entity for its dataset
	 */
	public Multimap<String, String> getDefaultProperties();
	
	/**
	 * 
	 * @param config LDPath Configuration with the properties specification 
	 * @return {@link Multimap} containing the results of the ldpath evaluations for each defined field 
	 */
        public Multimap<String, String> getPropertiesValues(RedLinkLDPathConfig config);
	
	/**
	 * 
	 * @return {@link Collection} of categories that ordered form the hierarchy of the entity in the dataset
	 */
	public Multimap<String, String> getHierarchy();
	
	
	/**
	 * 
	 * @return {@link Collection} of entity labels if they exist. If not, empty collection
	 */
	public Collection<String> getLabels();
	
	
	/**
	 * 
	 * @return {@link Map} of entity labels using language of the label as key and text label as value
	 */
	public Multimap<String, String> getLabelsByLanguage();
	
	/**
	 * 
	 * @return Entity's Preferred Label as String
	 */
	public String getPreferredLabel();
	
	/**
	 * 
	 * @return {@link Collection} of entity's types if they exist. If not, empty collection
	 */
	public Collection<String> getTypes();
	
	/**
	 * 
	 * @return {@link Multimap} using Entity Type URI as key an collection of labels in different languages as value 
	 */
	public Multimap<String, String> getTypesLabels(); 
	
	/**
	 * 
	 * @return Entity's Preferred Type
	 */
	public String getPreferredType();
	
	/**
	 * 
	 * @return {@link Map} of default ldpath defintions for this profile
	 */
	public Map<String, String> getDefaultLDPath();
}
