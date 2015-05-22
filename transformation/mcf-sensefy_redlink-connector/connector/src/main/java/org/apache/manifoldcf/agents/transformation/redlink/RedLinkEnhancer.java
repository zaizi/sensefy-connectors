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

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import io.redlink.sdk.RedLink;
import io.redlink.sdk.RedLinkFactory;
import io.redlink.sdk.analysis.AnalysisRequest;
import io.redlink.sdk.analysis.AnalysisRequest.OutputFormat;
import io.redlink.sdk.impl.analysis.model.Enhancement;
import io.redlink.sdk.impl.analysis.model.Enhancements;
import io.redlink.sdk.impl.analysis.model.Entity;
import io.redlink.sdk.impl.analysis.model.EntityAnnotation;
import io.redlink.sdk.impl.analysis.model.TextAnnotation;
import org.apache.commons.io.IOUtils;
import org.apache.manifoldcf.agents.interfaces.IOutputAddActivity;
import org.apache.manifoldcf.agents.interfaces.IOutputCheckActivity;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
import org.apache.manifoldcf.agents.transformation.redlink.config.RedLinkConfig;
import org.apache.manifoldcf.agents.transformation.redlink.config.RedLinkLDPathConfig;
import org.apache.manifoldcf.agents.transformation.redlink.profiles.RedLinkEntityProfile;
import org.apache.manifoldcf.agents.transformation.redlink.profiles.RedLinkEntityProfileFactory;
import org.apache.manifoldcf.agents.transformation.redlink.util.EntityComparator;
import org.apache.manifoldcf.core.interfaces.IHTTPOutput;
import org.apache.manifoldcf.core.interfaces.IPostParameters;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.interfaces.Specification;
import org.apache.manifoldcf.core.interfaces.SpecificationNode;
import org.apache.manifoldcf.core.interfaces.VersionContext;
import org.apache.manifoldcf.crawler.system.Logging;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * RedLink Enhancer Transformation Connector
 * 
 * @author Rafa Haro <rharo@zaizi.com>
 * @author Antonio David Perez Morales <aperez@zaizi.com>
 *
 */
public class RedLinkEnhancer extends org.apache.manifoldcf.agents.transformation.BaseTransformationConnector
{

    private static final String EDIT_SPECIFICATION_JS = "editSpecification.js";
  private static final String EDIT_SPECIFICATION_FIELDMAPPING_HTML = "editSpecification_FieldMapping.html";
  private static final String VIEW_SPECIFICATION_HTML = "viewSpecification.html";
  
  protected static final String ACTIVITY_ENHANCE = "enhance";

  protected static final String[] activitiesList = new String[]{ACTIVITY_ENHANCE};
  
  
  /**
   * DOCUMENTS' FIELDS
   */
  static final String ENTITY_INDEX_ID_FIELD = "id";  // the id field for the  Entity Index
  static final String DOC_IDS2POS_FIELD = "doc_ids2pos";
  static final String ENTITIES_FIELD = "entities";
  static final String ENTITIES_TYPES_FIELD = "entity_types";
  static final String PARENT_URI_FIELD = "doc_ids";
  static final String OCCURRENCES_FIELD = "occurrences";
  static final String SMLT_ENTITY_TYPES_FIELD = "smlt_entity_types";
  static final String FIELD_URIS = "smlt_entities";

    public static final String TYPE_FIELD = "type";

    public static final String HIERARCHY_FIELD = "hierarchy";

    public static final String ATTRIBUTES_FIELD = "attributes";


    /** Return a list of activities that this connector generates.
  * The connector does NOT need to be connected before this method is called.
  *@return the set of activities.
  */
  @Override
  public String[] getActivitiesList()
  {
    return activitiesList;
  }

  /** Get an output version string, given an output specification.  The output version string is used to uniquely describe the pertinent details of
  * the output specification and the configuration, to allow the Connector Framework to determine whether a document will need to be output again.
  * Note that the contents of the document cannot be considered by this method, and that a different version string (defined in IRepositoryConnector)
  * is used to describe the version of the actual document.
  *
  * This method presumes that the connector object has been configured, and it is thus able to communicate with the output data store should that be
  * necessary.
  *@param os is the current output specification for the job that is doing the crawling.
  *@return a string, of unlimited length, which uniquely describes output configuration and specification in such a way that if two such strings are equal,
  * the document will not need to be sent again to the output data store.
  */
  @Override
  public VersionContext getPipelineDescription(Specification os)
    throws ManifoldCFException, ServiceInterruption
  {
	RedLinkConfig config = new RedLinkConfig(os);
	return new VersionContext(config.toString(),params,os);
  }
  
  /** Pre-determine whether a document (passed here as a File object) is acceptable or not.  This method is
   * used to determine whether a document needs to be actually transferred.  This hook is provided mainly to support
   * search engines that only handle a small set of accepted file types.
   *@param pipelineDescription is the document's pipeline version string, for this connection.
   *@param localFile is the local file to check.
   *@param checkActivity is an object including the activities that can be done by this method.
   *@return true if the file is acceptable, false if not.
   */
   @Override
   public boolean checkDocumentIndexable(VersionContext pipelineDescription, File localFile, IOutputCheckActivity checkActivity)
     throws ManifoldCFException, ServiceInterruption
   {
     return true;
   }

   /** Pre-determine whether a document's length is acceptable.  This method is used
   * to determine whether to fetch a document in the first place.
   *@param pipelineDescription is the document's pipeline version string, for this connection.
   *@param length is the length of the document.
   *@param checkActivity is an object including the activities that can be done by this method.
   *@return true if the file is acceptable, false if not.
   */
   @Override
   public boolean checkLengthIndexable(VersionContext pipelineDescription, long length, IOutputCheckActivity checkActivity)
     throws ManifoldCFException, ServiceInterruption
   {
     // Always true
     return true;
   }

   /** Add (or replace) a document in the output data store using the connector.
    * This method presumes that the connector object has been configured, and it is thus able to communicate with the output data store should that be
    * necessary.
    * The OutputSpecification is *not* provided to this method, because the goal is consistency, and if output is done it must be consistent with the
    * output description, since that was what was partly used to determine if output should be taking place.  So it may be necessary for this method to decode
    * an output description string in order to determine what should be done.
    *@param documentURI is the URI of the document.  The URI is presumed to be the unique identifier which the output data store will use to process
    * and serve the document.  This URI is constructed by the repository connector which fetches the document, and is thus universal across all output connectors.
    *@param document is the document data to be processed (handed to the output data store).
    *@param authorityNameString is the name of the authority responsible for authorizing any access tokens passed in with the repository document.  May be null.
    *@param activities is the handle to an object that the implementer of a pipeline connector may use to perform operations, such as logging processing activity,
    * or sending a modified document to the next stage in the pipeline.
    *@return the document status (accepted or permanently rejected).
    *@throws IOException only if there's a stream error reading the document data.
    */
    @Override
    public int addOrReplaceDocumentWithException(String documentURI, VersionContext pipelineDescription, RepositoryDocument document, String authorityNameString, IOutputAddActivity activities)
      throws ManifoldCFException, ServiceInterruption, IOException
    {
	  Logging.connectors.info( "RedLink Processor starting processing :" + documentURI );
	  
	  // RedLink Instance Setup
	  RedLinkConfig config = new RedLinkConfig( pipelineDescription.getSpecification() );
	  RedLink.Analysis analysis = RedLinkFactory.createAnalysisClient(config.getApikey());
	  RedLink.Data data = RedLinkFactory.createDataClient(config.getApikey());
	  RedLinkLDPathConfig ldPathConfig = config.getLdPathConfig();
	  Map<String, String> namespaces = Maps.newHashMap();
	  Map<String, String> defaultLDPath = Maps.newHashMap();
        Collection<String> profiles = config.getProfiles();
        for(String profile: profiles ){
		  namespaces.putAll(RedLinkEntityProfileFactory.getPrefixes(profile));
		  defaultLDPath.putAll(RedLinkEntityProfileFactory.getDefaultLDPath(profile));
	  }
	  ldPathConfig.addNamespaces(namespaces);
	  ldPathConfig.addLdPathConfigurations(defaultLDPath);
	  String ldpathProgram = ldPathConfig.buildLdPathProgram();
	  
	  // Extracting Content. Content should has been parsed first by Tika
	  long length = document.getBinaryLength();
	  byte[] copy = IOUtils.toByteArray(document.getBinaryStream());
	  String content = new String(copy);
	 
	  // Enhance Content
	  long startTime = System.currentTimeMillis();
	  String resultCode = "OK";
	  String description = null;
      AnalysisRequest request = AnalysisRequest.
              builder().
              setAnalysis( config.getAnalysis() ).
              setContent( content ).
              setLDpathProgram( ldpathProgram ).
              setOutputFormat( OutputFormat.TURTLE ).
              build();
      Enhancements enhancements = null;
      try{
    	  enhancements = analysis.enhance(request);
      }catch(Exception e){
    	  resultCode = "REDLINK_ANALYSIS_FAIL";
    	  description = e.getMessage();
    	  Logging.connectors.error( "Error enhancing the document  : " + documentURI, e );
      }
      finally{
    	  activities.recordActivity(new Long(startTime), ACTIVITY_ENHANCE, length, documentURI,
                  resultCode, description);  
      }
      
      if(enhancements == null)
    	  return DOCUMENTSTATUS_REJECTED; // TODO Make This Configurable
            
      // Enrichment complete!
      // Create a copy of Repository Document
      RepositoryDocument docCopy = document.duplicate();
      docCopy.setBinary(new ByteArrayInputStream(copy), length);
      
      // Semantic Metadata Management
      Set<EntityAnnotation> bestEntitiesAnnotations = 
    		  extractBestAnnotations(enhancements.getBestAnnotations());
      
      Set<String> uris = Sets.newHashSet();
      SetMultimap<String, String> properties = HashMultimap.create();
      Collection<String> entityTypes = Sets.newHashSet();
            
      List<String> entitiesJsons = Lists.newArrayList();
      List<String> entitiesTypesJSONs = Lists.newArrayList();
      
      for(EntityAnnotation entity:bestEntitiesAnnotations){
    	  JSONArray nextEntityJSONArray=new JSONArray();
    	  JSONObject nextEntityJSON = new JSONObject();
          nextEntityJSONArray.put(nextEntityJSON  );
    	  boolean jsonFail = false;

    	  // Entity URI. Entity Document ID
    	  Entity referencedEntity = entity.getEntityReference();
    	  uris.add(referencedEntity.getUri());

    	  // Parent Document + Ocurrence Counter
    	  try{
        	  JSONObject occur = new JSONObject();
        	  occur.put("inc", 1);
    		  nextEntityJSON.put(OCCURRENCES_FIELD, occur);

    		  JSONObject parentURI = new JSONObject();
    		  parentURI.put("add", documentURI);
    		  nextEntityJSON.put(PARENT_URI_FIELD, parentURI);

    		  nextEntityJSON.put(ENTITY_INDEX_ID_FIELD, referencedEntity.getUri());
    	  }catch (JSONException e) {
    		  Logging.connectors.error( "Error creating Entity Document with URI: " + referencedEntity, e );
    		  jsonFail = true;
    	  }

    	  RedLinkEntityProfile helper =
    			  RedLinkEntityProfileFactory.createProfile(referencedEntity, data);

    	  // Common Properties
          if (config.includeCommonProperties())
          {
              Multimap<String, String> commonProperty2ValueList = helper.getDefaultProperties();
              for (String property : commonProperty2ValueList.keySet()){
            	  Collection<String> values = commonProperty2ValueList.get(property);
                  properties.putAll(property, values);
                  if(!jsonFail){
                	  try {
						nextEntityJSON.put(property, values);
					} catch (JSONException e) {
						Logging.connectors.error( "Error creating Entity Document with URI: " + referencedEntity, e );
			      		jsonFail = true;
					}
                  }
              }

          }
          // Entity's Properties
          Multimap<String, String> entityProperty2ValueList =
        		  helper.getAllPropertiesValues();
          for (String property:entityProperty2ValueList.keySet()){
        	  Collection<String> values = entityProperty2ValueList.get(property);
        	  String propertyName = RedLinkLDPathConfig.getLocalName(property);
        	  properties.putAll(propertyName, values);
        	  if(!jsonFail){
        		  try {
        			  nextEntityJSON.put(propertyName, values);
        		  } catch (JSONException e) {
        			  Logging.connectors.error( "Error creating Entity Document with URI: "
        					  + referencedEntity, e );
        			  jsonFail = true;
        		  }
        	  }
          }

          // Positions
          if(!jsonFail){
        	  try{
        		  JSONArray positions = new JSONArray();
        		  Collection<Enhancement> relations = entity.getRelations();
        		  for ( Enhancement ta : relations )
        		  {
        			  if ( ta instanceof TextAnnotation )
        			  {
        				  TextAnnotation current = (TextAnnotation) ta;
        				  JSONObject posEntry = new JSONObject();
        				  posEntry.put( "start", current.getStarts() );
        				  posEntry.put( "end", current.getEnds() );
        				  positions.put( posEntry );
        			  }
        		  }

        		  JSONObject ids2pos = new JSONObject();
        		  ids2pos.put(documentURI, positions);
        		  JSONObject addObject = new JSONObject();
        		  addObject.put("add", ids2pos.toString());
        		  nextEntityJSON.put(DOC_IDS2POS_FIELD, addObject);
        	  }catch (JSONException e) {
        		  Logging.connectors.error( "Error creating Entity Document with URI: "
        				  + referencedEntity, e );
        		  jsonFail = true;
        	  }
          }
          
          if(!jsonFail){
        	  entitiesJsons.add(nextEntityJSONArray.toString()); // we need the array
          }
          
       // SMLT Annotations
          Multimap<String, String> types = helper.getTypesLabels();
          if(types.isEmpty()){
        	  Collection<String> unlabelledTypes = helper.getTypes();
        	  for(String type:unlabelledTypes)
        		  types.put(type, RedLinkLDPathConfig.getLocalName(type));
          }


          // Entity Type JSONs
          for(String typeURI:types.keySet())
        	  if(!entityTypes.contains(typeURI)){
        		  entityTypes.add(typeURI);
                          JSONArray nextEntityTypeJSONArray=new JSONArray();
        		  JSONObject nextEntityTypeJSON = new JSONObject();
                      nextEntityTypeJSONArray.put( nextEntityTypeJSON );
        		  try {
            		  nextEntityTypeJSON.put(ENTITY_INDEX_ID_FIELD, typeURI);
            		  JSONObject occur = new JSONObject();
            		  occur.put("inc", 1);
            		  nextEntityTypeJSON.put(OCCURRENCES_FIELD, occur);

            		  // Multilanguage Labels
            		  Collection<String> labels = types.get(typeURI);
            		  nextEntityTypeJSON.put( TYPE_FIELD, labels);

            		  // Hierarchy

            		  Collection<String> hierarchy = 
            				  helper.getHierarchy().get(typeURI);
            		  nextEntityTypeJSON.put( HIERARCHY_FIELD, hierarchy);

            		  
            		  // Attributes
            		  Collection<String> propertiesURIs = helper.getAllPropertiesValues().keySet();
            		  nextEntityTypeJSON.put( ATTRIBUTES_FIELD,
            				  FluentIterable.
            				  from(propertiesURIs).
            				  transform(new Function<String, String>(){
            					  @Override
            					  public String apply(String input) {
            						  return RedLinkLDPathConfig.getLocalName(input);
            					  }
            				  }));
            		  
            		  entitiesTypesJSONs.add(nextEntityTypeJSONArray.toString());
            		  
            	  } catch (JSONException e) {
            		  Logging.connectors.error( "Error creating Entity Type Document with URI: "
            				  + typeURI, e );
            		  continue; // Continue with next Entity Type
            	  }

        	  }
      }
      
      
      // Add Semantic Metadata
      docCopy.addField(FIELD_URIS, uris.toArray(new String[uris.size()]));
      for(String property:properties.keySet()){
    	Set<String> values = properties.get(property);
    	docCopy.addField(property, 
    			values.toArray(new String[values.size()]));
      }
      docCopy.addField(SMLT_ENTITY_TYPES_FIELD, 
    		  entityTypes.toArray(new String[entityTypes.size()]));
      
      // Add Entities JSONs
      docCopy.addField(ENTITIES_FIELD, 
    		  entitiesJsons.toArray(new String[entitiesJsons.size()]));
      
      // Add Entities's types JSONs
      docCopy.addField(ENTITIES_TYPES_FIELD, 
    		  entitiesTypesJSONs.toArray(new String[entitiesTypesJSONs.size()]));

      // Send new document downstream
        Logging.connectors.info( "Check : "+documentURI );
        int rval = activities.sendDocument(documentURI,docCopy);

      resultCode = (rval == DOCUMENTSTATUS_ACCEPTED)?"ACCEPTED":"REJECTED";
      return rval;
  }

  /**
   * Returns a set of different entities annotation present in the document
   *
   * @param bestAnnotations
   * @return
   */
  private Set<EntityAnnotation> extractBestAnnotations( Multimap<TextAnnotation, EntityAnnotation> bestAnnotations )
  {
      Set<EntityAnnotation> allExtractedEntities = new HashSet<EntityAnnotation>();
      for ( TextAnnotation ta : bestAnnotations.keySet() )
      {
          Collection<EntityAnnotation> entityAnnotations = bestAnnotations.get( ta );
          EntityAnnotation bestSelected = getOneEntityAnnotation( entityAnnotations );
          allExtractedEntities.add( bestSelected );
      }
      return allExtractedEntities;
  }

  /**
   * returns one entity annotation, deterministic, from a set of entity annotations
   *
   * @param entityAnnotations
   * @return
   */
  private EntityAnnotation getOneEntityAnnotation( Collection<EntityAnnotation> entityAnnotations )
  {
      EntityAnnotation[] entityAnnotationArray =
          entityAnnotations.toArray( new EntityAnnotation[entityAnnotations.size()] );
      Arrays.sort( entityAnnotationArray, new EntityComparator() );

      return entityAnnotationArray[0];
  }

  /** Obtain the name of the form check javascript method to call.
  *@param connectionSequenceNumber is the unique number of this connection within the job.
  *@return the name of the form check javascript method.
  */
  @Override
  public String getFormCheckJavascriptMethodName(int connectionSequenceNumber)
  {
    return "s"+connectionSequenceNumber+"_checkSpecification";
  }

  /** Obtain the name of the form presave check javascript method to call.
  *@param connectionSequenceNumber is the unique number of this connection within the job.
  *@return the name of the form presave check javascript method.
  */
  @Override
  public String getFormPresaveCheckJavascriptMethodName(int connectionSequenceNumber)
  {
    return "s"+connectionSequenceNumber+"_checkSpecificationForSave";
  }

  /** Output the specification header section.
  * This method is called in the head section of a job page which has selected a pipeline connection of the current type.  Its purpose is to add the required tabs
  * to the list, and to output any javascript methods that might be needed by the job editing HTML.
  *@param out is the output to which any HTML should be sent.
  *@param locale is the preferred local of the output.
  *@param os is the current pipeline specification for this connection.
  *@param connectionSequenceNumber is the unique number of this connection within the job.
  *@param tabsArray is an array of tab names.  Add to this array any tab names that are specific to the connector.
  */
  @Override
  public void outputSpecificationHeader(IHTTPOutput out, Locale locale, Specification os,
    int connectionSequenceNumber, List<String> tabsArray)
    throws ManifoldCFException, IOException
  {
    Map<String, Object> paramMap = new HashMap<String, Object>();
    paramMap.put("SEQNUM",Integer.toString(connectionSequenceNumber));

    tabsArray.add(Messages.getString(locale, "RedLinkEnhancer.Parameters"));

    // Fill in the specification header map, using data from all tabs.
    fillInFieldMappingSpecificationMap(paramMap, os);

    Messages.outputResourceWithVelocity(out,locale,EDIT_SPECIFICATION_JS,paramMap);
  }
  
  /** Output the specification body section.
  * This method is called in the body section of a job page which has selected a pipeline connection of the current type.  Its purpose is to present the required form elements for editing.
  * The coder can presume that the HTML that is output from this configuration will be within appropriate <html>, <body>, and <form> tags.  The name of the
  * form is "editjob".
  *@param out is the output to which any HTML should be sent.
  *@param locale is the preferred local of the output.
  *@param os is the current pipeline specification for this job.
  *@param connectionSequenceNumber is the unique number of this connection within the job.
  *@param actualSequenceNumber is the connection within the job that has currently been selected.
  *@param tabName is the current tab name.
  */
  @Override
  public void outputSpecificationBody(IHTTPOutput out, Locale locale, Specification os,
    int connectionSequenceNumber, int actualSequenceNumber, String tabName)
    throws ManifoldCFException, IOException
  {
    Map<String, Object> paramMap = new HashMap<String, Object>();

    // Set the tab name
    paramMap.put("TABNAME", tabName);
    paramMap.put("SEQNUM",Integer.toString(connectionSequenceNumber));
    paramMap.put("SELECTEDNUM",Integer.toString(actualSequenceNumber));

    // Fill in the field mapping tab data
    fillInFieldMappingSpecificationMap(paramMap, os);
    Messages.outputResourceWithVelocity(out,locale,EDIT_SPECIFICATION_FIELDMAPPING_HTML,paramMap);
  }

  /** Process a specification post.
  * This method is called at the start of job's edit or view page, whenever there is a possibility that form data for a connection has been
  * posted.  Its purpose is to gather form information and modify the transformation specification accordingly.
  * The name of the posted form is "editjob".
  *@param variableContext contains the post data, including binary file-upload information.
  *@param locale is the preferred local of the output.
  *@param os is the current pipeline specification for this job.
  *@param connectionSequenceNumber is the unique number of this connection within the job.
  *@return null if all is well, or a string error message if there is an error that should prevent saving of the job (and cause a redirection to an error page).
  */
  @Override
  public String processSpecificationPost(IPostParameters variableContext, Locale locale, Specification os,
                                         int connectionSequenceNumber)
                                         throws ManifoldCFException {
                                         
                                           // About to gather the fieldmapping nodes, so get rid of the old ones.
                                           int i = 0;
                                           while (i < os.getChildCount())
                                           {
                                             SpecificationNode node = os.getChild(i);
                                             if (node.getType().equals(RedLinkConfig.NODE_API_KEY) 
                                                     || node.getType().equals(RedLinkConfig.NODE_ANALYSIS_NAME)
                                                     || node.getType().equals(RedLinkConfig.NODE_COMMON_PARAMETERS)
                                                     || node.getType().equals(RedLinkConfig.NODE_ENTITY_TYPE)
                                                     || node.getType().equals(RedLinkConfig.NODE_PROFILES)
                                                     || node.getType().equals(RedLinkConfig.NODE_PROFILE))
                                               os.removeChild(i);
                                             else
                                               i++;
                                           }
                                          
                                          String sequenceNumber = String.valueOf(connectionSequenceNumber);
                                          String json = os.toJSON();
                                          String redlinkJsonConf = variableContext.getParameter("redlink_json_conf_"+sequenceNumber);
                                          try {
                                             JSONObject redlinkJson = new JSONObject(redlinkJsonConf);
                                             JSONObject configJson = new JSONObject(json);
                                             @SuppressWarnings("unchecked")
                                             Iterator<String> keyNames = redlinkJson.keys();
                                             while(keyNames.hasNext()) {
                                                 String key = keyNames.next();
                                                 configJson.put(key, redlinkJson.get(key));
                                             }
                                             
                                             os.fromJSON(configJson.toString());
                                         } catch (JSONException e) {
                                             e.printStackTrace();
                                         }
                                           return null;
                                       }
  

  /** View specification.
  * This method is called in the body section of a job's view page.  Its purpose is to present the pipeline specification information to the user.
  * The coder can presume that the HTML that is output from this configuration will be within appropriate <html> and <body> tags.
  *@param out is the output to which any HTML should be sent.
  *@param locale is the preferred local of the output.
  *@param connectionSequenceNumber is the unique number of this connection within the job.
  *@param os is the current pipeline specification for this job.
  */
  @Override
  public void viewSpecification(IHTTPOutput out, Locale locale, Specification os,
    int connectionSequenceNumber)
    throws ManifoldCFException, IOException
  {
    Map<String, Object> paramMap = new HashMap<String, Object>();
    paramMap.put("SEQNUM",Integer.toString(connectionSequenceNumber));

    // Fill in the map with data from all tabs
    fillInFieldMappingSpecificationMap(paramMap, os);

    Messages.outputResourceWithVelocity(out,locale,VIEW_SPECIFICATION_HTML,paramMap);
    
  }

  
  protected static void fillInFieldMappingSpecificationMap(Map<String,Object> paramMap, Specification os)
  {
      List<String> types = Lists.newArrayList();
      List<List<Map<String, String>>> fields = Lists.newArrayList();
      
      for (int i = 0; i < os.getChildCount(); i++) {
        SpecificationNode sn = os.getChild(i);
        
        if(sn.getType().equals(RedLinkConfig.NODE_API_KEY)) {
            paramMap.put("APIKEY", sn.getAttributeValue(RedLinkConfig.ATTRIBUTE_API_KEY));
        } else if (sn.getType().equals(RedLinkConfig.NODE_ANALYSIS_NAME)) {
            paramMap.put("ANALYSIS", sn.getAttributeValue(RedLinkConfig.ATTRIBUTE_ANALYSIS_NAME));
        } else if (sn.getType().equals(RedLinkConfig.NODE_COMMON_PARAMETERS)) {
            String commonStr = sn.getAttributeValue(RedLinkConfig.ATTRIBUTE_COMMON_PARAMETERS);
            if(commonStr != null)
                paramMap.put("COMMON", Boolean.parseBoolean(commonStr));
            else
                paramMap.put("COMMON", "true");
        } else if (sn.getType().equals(RedLinkConfig.NODE_ENTITY_TYPE)){
            String entityType = sn.getAttributeValue(RedLinkConfig.ATTRIBUTE_ENTITY_TYPE);
            types.add(entityType);
            List<Map<String, String>> nextFields = Lists.newArrayList();
            for(int j = 0; j < sn.getChildCount(); j++){
                SpecificationNode next = sn.getChild(j);
                if(next.getType().equals(RedLinkConfig.NODE_LDPATH)){
                    Map<String, String> nextFieldMap = Maps.newHashMap();
                    nextFieldMap.put("FIELD", next.getAttributeValue(RedLinkConfig.ATTRIBUTE_FIELD));
                    nextFieldMap.put("LDPATH_EXPRESSION", next.getAttributeValue(RedLinkConfig.ATTRIBUTE_LDPATH));
                    nextFields.add(nextFieldMap);
                }
            }
            fields.add(nextFields);
        }
        // Profiles
        else if (sn.getType().equals(RedLinkConfig.NODE_PROFILES)) {
            for(int j=0,len = sn.getChildCount();j<len; j++) {
                SpecificationNode prof = sn.getChild(j);
                if(!prof.getType().equals(RedLinkConfig.NODE_PROFILE)) {
                    continue;
                }
                
                String profileVal = prof.getAttributeValue(RedLinkConfig.ATTRIBUTE_PROFILE);
                if(profileVal != null && !profileVal.isEmpty()) {
                    paramMap.put(profileVal.toUpperCase()+"_PROFILE", "true");
                }
            }
        }
    }
    
    paramMap.put("ENTITY_TYPES", types);
    paramMap.put("LDPATHS", fields);
    try {
        paramMap.put("REDLINK_JSON_CONF", os.toJSON());
    } catch (ManifoldCFException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    paramMap.put("Integer", Integer.class);
    
    checkDefaultPropertyValue(paramMap, "APIKEY", "");
    checkDefaultPropertyValue(paramMap, "ANALYSIS", "");
    checkDefaultPropertyValue(paramMap, "COMMON", "true");
    checkDefaultPropertyValue(paramMap, "FREEBASE_PROFILE", "false");
    checkDefaultPropertyValue(paramMap, "SKOS_PROFILE", "false");
    checkDefaultPropertyValue(paramMap, "DEFAULT_PROFILE", "false");
    checkDefaultPropertyValue(paramMap, "ENTITY_TYPES", Lists.newArrayList());
    checkDefaultPropertyValue(paramMap, "LDPATHS", Lists.newArrayList());
    checkDefaultPropertyValue(paramMap, "REDLINK_JSON_CONF", "{}");
  }
  
  /**
   * <p>Checks if the given property exists in the map, adding the default value if the property does not exist</p>
   * 
   * @param paramMap the map containing the properties
   * @param property the property to be checked
   * @param defaultValue the default value to be put if no value is set
   */
  private static void checkDefaultPropertyValue(Map<String,Object> paramMap, String property, Object defaultValue) {
      if (paramMap.get(property) == null) {
          paramMap.put(property, defaultValue);
      }
  }
  
}
