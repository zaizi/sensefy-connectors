
package org.apache.manifoldcf.agents.transformation.redlink.profiles;

import io.redlink.sdk.RedLink;
import io.redlink.sdk.impl.analysis.model.Entity;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.SKOS;

import java.util.Collections;
import java.util.Map;

/**
 * RedLink Entity Profile Factory. Create a Proper Profile Depending on entity data
 * 
 * @author Rafa Haro <rharo@zaizi.com>
 *
 */
public class RedLinkEntityProfileFactory {
	
	public static RedLinkEntityProfile createProfile(Entity entity, RedLink.Data data){
		
		String dataset = inferDataset(entity);
		
		if(dataset == null)
			return new CustomRedLinkEntityProfile(entity, data);
		else if(dataset.equals(FreebaseRedLinkEntityProfile.DATASET_NAME))
			return new FreebaseRedLinkEntityProfile(entity, data);
		else if(dataset.equals(SKOSBasedRedLinkEntityProfile.DATASET_NAME))
			return new SKOSBasedRedLinkEntityProfile(entity, data);
		else
			return new CustomRedLinkEntityProfile(entity, data);
	}
	
	public static RedLinkEntityProfile createProfile(Entity entity, String profile, RedLink.Data data){
		if(profile.toUpperCase().equals(FreebaseRedLinkEntityProfile.DATASET_NAME))
			return new FreebaseRedLinkEntityProfile(entity, data);
		else if(profile.toUpperCase().equals(SKOSBasedRedLinkEntityProfile.DATASET_NAME))
			return new SKOSBasedRedLinkEntityProfile(entity, data);
		
		return new CustomRedLinkEntityProfile(entity, data);
	}
	
	public static Map<String, String> getPrefixes(String profile){
		if(profile.toUpperCase().equals(FreebaseRedLinkEntityProfile.DATASET_NAME.toUpperCase()))
			return FreebaseRedLinkEntityProfile.FREEBASE_NAMESPACES;
		else if(profile.toUpperCase().equals(SKOSBasedRedLinkEntityProfile.DATASET_NAME))
			return SKOSBasedRedLinkEntityProfile.SKOS_NAMESPACES;
		
		return Collections.emptyMap();
	}
	
	public static Map<String, String> getDefaultLDPath(String profile){
		if(profile.toUpperCase().equals(FreebaseRedLinkEntityProfile.DATASET_NAME.toUpperCase()))
			return FreebaseRedLinkEntityProfile.FREEBASE_DEFAULT_LDPATH;
		else if(profile.toUpperCase().equals(SKOSBasedRedLinkEntityProfile.DATASET_NAME))
			return SKOSBasedRedLinkEntityProfile.SKOS_DEFAULT_LDPATH;
		
		return Collections.emptyMap();
	}
	
	private static String inferDataset(Entity entity){
		
		// Membership test. This is crappy. Waiting for RedLink to include the dataset in the entity
		if(entity.getUri().toLowerCase().contains("freebase"))
			return FreebaseRedLinkEntityProfile.DATASET_NAME;
		else if(entity.getValues(RDF.TYPE.stringValue()).contains(SKOS.CONCEPT.stringValue()))
			return SKOSBasedRedLinkEntityProfile.DATASET_NAME;
		
		return null;
	}
}
