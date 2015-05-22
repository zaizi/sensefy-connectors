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

import java.util.Map;
import java.util.Map.Entry;

import org.apache.manifoldcf.agents.transformation.redlink.config.RedLinkLDPathConfig;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Abstract Implementation for all Entity's Profiles
 * 
 * @author Rafa Haro <rharo@zaizi.com>
 *
 */
public abstract class AbstractRedLinkEntityProfile implements
		RedLinkEntityProfile {
	
	protected Entity entity;
	
	protected RedLink.Data data;
	
	protected AbstractRedLinkEntityProfile(Entity entity, RedLink.Data data){
		this.entity = entity;
		this.data = data;
	}

	@Override
	public final Entity getEntity() {
		return entity;
	}

	@Override
	public Multimap<String, String> getAllPropertiesValues() {
		Multimap<String, String> result = HashMultimap.create();
		for(String property:entity.getProperties())
			result.putAll(property, entity.getValues(property));
		return result;
	}

	@Override
	public Multimap<String, String> getPropertiesValues(
			RedLinkLDPathConfig config) {
		Multimap<String, String> result = HashMultimap.create();
        Map<String, String> properties = config.getPropertiesByFields();
        for ( Entry<String, String> entry : properties.entrySet() )
        {
            result.putAll( entry.getKey(), entity.getValues( entry.getValue() ) );
        }
        return result;
	}
}
