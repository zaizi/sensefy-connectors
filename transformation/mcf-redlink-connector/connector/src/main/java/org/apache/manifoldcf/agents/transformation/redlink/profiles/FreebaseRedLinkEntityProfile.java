package org.apache.manifoldcf.agents.transformation.redlink.profiles;

import io.redlink.sdk.impl.analysis.model.Entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.manifoldcf.agents.transformation.redlink.RedLinkLDPathConfig;
import org.openrdf.model.impl.URIImpl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * This class is a wrapper for easing the work with Freebase entities in the RedLink Processor Connector
 *
 * @author Rafa Haro <rharo@zaizi.com>
 */
public class FreebaseRedLinkEntityProfile
{

    public static final Map<String, String> FREEBASE_NAMESPACES =
        ImmutableMap.of( "fb", "http://rdf.freebase.com/ns/" );

    public static final ImmutableMap<String, String> FREEBASE_COMMON_PROPERTIES_URIS =
        ImmutableMap.<String, String>builder().
        	put("website", "http://rdf.freebase.com/ns/common.topic.official_website" ).
        	put("description", "http://rdf.freebase.com/ns/common.topic.description" ).
        	put("label", "http://www.w3.org/2000/01/rdf-schema#label" ).
        	put("type", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" ).
        	put("notable_type", "http://rdf.freebase.com/ns/:common.topic.notable_for/http://rdf.freebase.com/ns/:common.notable_for.display_name[@en]" ).
        	put("thumbnail", "http://rdf.freebase.com/ns/common.topic.image" ).
        	build();

    public static final ImmutableSet<String> FREEBASE_COMMON_PROPERTIES =
        ImmutableSet.<String>builder().add( "website" ).add( "description" ).build();

    public static final String FB_GOOGLE_IMAGE_APIS = "https://usercontent.googleapis.com/freebase/v1/image/";

    public static final String JSON_PATH_LABEL = "$.property['/type/object/name'].values";

    public static final String JSON_PATH_ALIAS = "$.property['/common/topic/alias'].values ";

    public static final String JSON_PATH_HIERARCHY = "$.property['/freebase/type_hints/included_types'].values ";



    public static ImmutableSet<String> getFreebaseCommonProperties()
    {
        return FREEBASE_COMMON_PROPERTIES;
    }

    public static final Collection<String> TYPES_BLACK_LIST =
        ImmutableList.of( "http://rdf.freebase.com/ns/common.topic",
                          "http://rdf.freebase.com/ns/base.ontologies.ontology_instance",
                          "http://rdf.freebase.com/ns/user" );

    private Entity entity;

    public FreebaseRedLinkEntityProfile( Entity entity )
    {
        this.entity = entity;
    }

    public Multimap<String, String> getCommonPropertiesValues()
    {
        Multimap<String, String> result = ArrayListMultimap.create();
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

    public Entity getEntity()
    {
        return entity;
    }

    public Multimap<String, String> getCustomPropertiesValues( RedLinkLDPathConfig config )
    {
        Multimap<String, String> result = ArrayListMultimap.create();
        Map<String, String> properties = config.getPropertiesByFields();
        for ( Entry<String, String> entry : properties.entrySet() )
        {
            result.putAll( entry.getKey(), entity.getValues( entry.getValue() ) );
        }
        return result;
    }

    /**
     * ns:common.topic.alias
     * ns:type.object.name
     *
     * @param entityTypeURI
     * @return
     */
//    public static Map<String,List<String>> getMultiLanguageLabelsAndHierarchy( String entityTypeURI )
//    {
//
//        List<String> hierarchyList = new ArrayList<String>();
//        List<String> multiLabels = new ArrayList<String>();
//        HttpTransport httpTransport = new NetHttpTransport();
//        HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
//        JSONParser parser = new JSONParser();
//        String namespace= getNameSpace( entityTypeURI ) ;
//        entityTypeURI = getLocalName( entityTypeURI ).replaceAll( "\\.", "/" );
//        GenericUrl url = new GenericUrl( "https://www.googleapis.com/freebase/v1/topic/" + entityTypeURI );
//        url.put( "key", FB_APi_KEY );
//        url.put( "lang", "all" );
//        url.put( "filter", "allproperties" );
//        JSONObject topic = null;
//        try
//        {
//            HttpRequest request = requestFactory.buildGetRequest( url );
//            HttpResponse httpResponse = request.execute();
//            topic = (JSONObject) parser.parse( httpResponse.parseAsString() );
//        }
//        catch ( Exception e )
//        {
//            logger.error( "FreebaseProfile - connections problem : ", e );
//        }
//
//        try
//        {
//            JSONArray aliases = JsonPath.read( topic, JSON_PATH_LABEL );
//            for ( int i = 0; i < aliases.size(); i++ )
//            {
//                JSONObject currentValue = (JSONObject) aliases.get( i );
//                String value = JsonPath.read( currentValue, "value" );
//                multiLabels.add( value );
//            }
//        }
//        catch ( Exception e )
//        {
//            //logger.warn( entityTypeURI + " has not " + JSON_PATH_LABEL );
//        }
//
//        try
//        {
//            JSONArray labels = JsonPath.read( topic, JSON_PATH_ALIAS );
//            for ( int i = 0; i < labels.size(); i++ )
//            {
//                JSONObject currentValue = (JSONObject) labels.get( i );
//                String value = JsonPath.read( currentValue, "value" );
//                multiLabels.add( value );
//            }
//        }
//        catch ( Exception e )
//        {
//            //logger.warn( entityTypeURI + " has not " + JSON_PATH_ALIAS );
//        }
//
//        try
//        {
//            JSONArray hierarchy = JsonPath.read( topic,JSON_PATH_HIERARCHY);
//            for ( int i = 0; i < hierarchy.size(); i++ )
//            {
//                JSONObject currentValue = (JSONObject) hierarchy.get( i );
//                String id = JsonPath.read( currentValue, "id" );
//                id=id.replaceAll( "\\/", "." );
//                int idSize=id.length();
//                id=namespace+id.substring( 1,idSize );
//                hierarchyList.add( id );
//            }
//        }
//        catch ( Exception e )
//        {
//            //logger.warn( entityTypeURI + " has not " + JSON_PATH_HIERARCHY );
//        }
//
//        Map<String,List<String>> enrichment2list=new HashMap<String,List<String>>(  ) ;
//        enrichment2list.put( "labels",multiLabels );
//        enrichment2list.put("hierarchy",hierarchyList);
//        return enrichment2list;
//
//
//    }


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
    
    private String getNameSpace( String uri )
    {
        UrlValidator urlValidator = new UrlValidator();
        if ( urlValidator.isValid( uri ) )
        {
            URIImpl uriImpl = new URIImpl( uri );
            return uriImpl.getNamespace();
        }
        else
        {
            return new String( uri );
        }
    }
}
