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

import io.redlink.sdk.RedLink;
import io.redlink.sdk.RedLinkFactory;
import io.redlink.sdk.analysis.AnalysisRequest;
import io.redlink.sdk.analysis.AnalysisRequest.OutputFormat;
import io.redlink.sdk.impl.analysis.model.Enhancements;
import io.redlink.sdk.impl.analysis.model.Entity;
import io.redlink.sdk.impl.analysis.model.EntityAnnotation;
import io.redlink.sdk.impl.analysis.model.TextAnnotation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.manifoldcf.agents.interfaces.IOutputAddActivity;
import org.apache.manifoldcf.agents.interfaces.IOutputCheckActivity;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
import org.apache.manifoldcf.agents.system.Logging;
import org.apache.manifoldcf.agents.transformation.redlink.profiles.FreebaseRedLinkEntityProfile;
import org.apache.manifoldcf.agents.transformation.redlink.util.EntityComparator;
import org.apache.manifoldcf.core.interfaces.IHTTPOutput;
import org.apache.manifoldcf.core.interfaces.IPostParameters;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.interfaces.Specification;
import org.apache.manifoldcf.core.interfaces.SpecificationNode;
import org.apache.manifoldcf.core.interfaces.VersionContext;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

/**
 * RedLink Enhancer Transformation Connector
 * 
 * @author Rafa Haro <rharo@zaizi.com>
 *
 */
public class RedLinkEnhancer extends org.apache.manifoldcf.agents.transformation.BaseTransformationConnector
{
  private static final String EDIT_SPECIFICATION_JS = "editSpecification.js";
  private static final String EDIT_SPECIFICATION_FIELDMAPPING_HTML = "editSpecification_FieldMapping.html";
  private static final String VIEW_SPECIFICATION_HTML = "viewSpecification.html";
  
  private static final String FIELD_URIS = "entities_uris";

  protected static final String ACTIVITY_ENHANCE = "enhance";

  protected static final String[] activitiesList = new String[]{ACTIVITY_ENHANCE};
 
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
    *@param outputDescription is the description string that was constructed for this document by the getOutputDescription() method.
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
	  Logging.ingest.info( "RedLink Processor starting processing :" + documentURI );
	  
	  // RedLink Instance Setup
	  RedLinkConfig config = new RedLinkConfig( pipelineDescription.getSpecification() );
	  RedLink.Analysis analysis = RedLinkFactory.createAnalysisClient( config.getApikey() );
	  
	  // Extracting Content. Content should has been parsed first by Tika
	  long length = document.getBinaryLength();
	  byte[] copy = IOUtils.toByteArray(document.getBinaryStream());
	  String content = new String(copy);
	 
	  // Enhance Content
	  String ldpathProgram = config.getLdPathConfig().
              buildLdPathProgram( FreebaseRedLinkEntityProfile.FREEBASE_NAMESPACES );
	  long startTime = System.currentTimeMillis();
	  String resultCode = "OK";
	  String description = null;
      AnalysisRequest request = AnalysisRequest.
              builder().
              setAnalysis( config.getAnalysis() ).
              setContent( content ).
              setLDpathProgram( ldpathProgram ).
              setOutputFormat( OutputFormat.RDFXML ).
              build();
      Enhancements enhancements = null;
      try{
    	  enhancements = analysis.enhance(request);
      }catch(Exception e){
    	  resultCode = "REDLINK_ANALYSIS_FAIL";
    	  description = e.getMessage();
    	  Logging.ingest.error( "Error enhancing the document  : " + documentURI, e );
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
      
      for(EntityAnnotation entity:bestEntitiesAnnotations){
    	  
    	  Entity referencedEntity = entity.getEntityReference();
    	  uris.add(referencedEntity.getUri());
      
    	  FreebaseRedLinkEntityProfile freebaseHelper = 
    			  new FreebaseRedLinkEntityProfile( referencedEntity );
    	  
    	  // Common Properties
          if (config.includeCommonProperties())
          {
              Multimap<String, String> commonProperty2ValueList = freebaseHelper.getCommonPropertiesValues();
              for (String property : commonProperty2ValueList.keySet())
                  properties.putAll(property, commonProperty2ValueList.get(property));
          }
          
          // Entity's Properties
          Multimap<String, String> entityProperty2ValueList =
              freebaseHelper.getCustomPropertiesValues(config.getLdPathConfig());
          for (String property:entityProperty2ValueList.keySet())
              properties.putAll(property, entityProperty2ValueList.get(property));
      }
      
      
      // Add Semantic Metadata
      docCopy.addField(FIELD_URIS, uris.toArray(new String[uris.size()]));
      for(String property:properties.keySet()){
    	Set<String> values = properties.get(property);
    	docCopy.addField(property, 
    			values.toArray(new String[values.size()]));
      }
      
      // Add Entities JSON
      

      // Send new document downstream
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
    String seqPrefix = "s"+connectionSequenceNumber+"_";

    String x;
        
    x = variableContext.getParameter(seqPrefix+"ldpath_count");
    if (x != null && x.length() > 0)
    {
      // About to gather the fieldmapping nodes, so get rid of the old ones.
      int i = 0;
      while (i < os.getChildCount())
      {
        SpecificationNode node = os.getChild(i);
        if (node.getType().equals(RedLinkConfig.NODE_API_KEY) 
        		|| node.getType().equals(RedLinkConfig.NODE_ANALYSIS_NAME)
        		|| node.getType().equals(RedLinkConfig.NODE_COMMON_PARAMETERS)
        		|| node.getType().equals(RedLinkConfig.NODE_ENTITY_TYPE))
          os.removeChild(i);
        else
          i++;
      }
      
      SpecificationNode node = new SpecificationNode(RedLinkConfig.NODE_API_KEY);
      String apikey = variableContext.getParameter(seqPrefix+"apikey");
      if (apikey != null){
        node.setAttribute(RedLinkConfig.ATTRIBUTE_API_KEY, apikey);
      }else{
        node.setAttribute(RedLinkConfig.ATTRIBUTE_API_KEY, "");
      } 
      os.addChild(os.getChildCount(), node);
      
      SpecificationNode node2 = new SpecificationNode(RedLinkConfig.NODE_ANALYSIS_NAME);
      String analysis = variableContext.getParameter(seqPrefix+"analysis");
      if (analysis != null){
        node.setAttribute(RedLinkConfig.ATTRIBUTE_ANALYSIS_NAME, analysis);
      }else{
        node.setAttribute(RedLinkConfig.ATTRIBUTE_ANALYSIS_NAME, "");
      }
      os.addChild(os.getChildCount(), node2);
      
      SpecificationNode node3 = new SpecificationNode(RedLinkConfig.NODE_COMMON_PARAMETERS);
      String common = variableContext.getParameter(seqPrefix+"common");
      if (common != null){
        node.setAttribute(RedLinkConfig.ATTRIBUTE_COMMON_PARAMETERS, common);
      }else{
        node.setAttribute(RedLinkConfig.ATTRIBUTE_COMMON_PARAMETERS, "false");
      }
      os.addChild(os.getChildCount(), node3);
      
      Map<String, Map<String, String>> ldPath = Maps.newHashMap();
      int count = Integer.parseInt(x);
      for(i = 0; i < count; i++)
      {
    	  String prefix = seqPrefix+"ldpath_";
    	  String suffix = "_"+Integer.toString(i);
    	  String op = variableContext.getParameter(prefix+"op"+suffix);
    	  if (op == null || !op.equals("Delete")){
    		  String entityType = variableContext.getParameter(prefix+"entitytype"+suffix);
    		  String nextFields = variableContext.getParameter(seqPrefix+"field_count"+"_"+i);
    		  int fieldCount = Integer.parseInt(nextFields);
    		  Map<String, String> nextFieldsMap = Maps.newHashMap();
    		  for(int j = 0; j < fieldCount; j++){
    			  String fieldPrefix = seqPrefix + "field_";
    			  String fieldSufix = i + "" + j;
    			  String fieldName = variableContext.getParameter(fieldPrefix+"fieldName"+fieldSufix);
    			  String ldPathExpression = variableContext.getParameter(fieldPrefix+"ldPath"+fieldSufix);
    			  nextFieldsMap.put(fieldName, ldPathExpression);
    		  }
    		  ldPath.put(entityType, nextFieldsMap);
    	  }
      }

      // Last One
      String addop = variableContext.getParameter(seqPrefix+"ldpath_op");
      if (addop != null && addop.equals("Add"))
      {
    	  String entityType = variableContext.getParameter(seqPrefix+"ldpath_entitytype");
		  String nextFields = variableContext.getParameter(seqPrefix+"field_count");
		  int fieldCount = Integer.parseInt(nextFields);
		  Map<String, String> nextFieldsMap = Maps.newHashMap();
		  for(int j = 0; j < fieldCount; j++){
			  String fieldPrefix = seqPrefix + "field";
			  String fieldName = variableContext.getParameter(fieldPrefix+"fieldName");
			  String ldPathExpression = variableContext.getParameter(fieldPrefix+"ldPath");
			  nextFieldsMap.put(fieldName, ldPathExpression);
		  }
		  ldPath.put(entityType, nextFieldsMap);
      }
      
     
      for(Entry<String, Map<String, String>> ldPathEntry:ldPath.entrySet()){
    	  String entityType = ldPathEntry.getKey();
    	  SpecificationNode nextLdPathNode = new SpecificationNode(RedLinkConfig.NODE_ENTITY_TYPE);
    	  nextLdPathNode.setAttribute(RedLinkConfig.ATTRIBUTE_ENTITY_TYPE, entityType);
    	  i = 0;
    	  for(Entry<String, String> fieldEntry:ldPathEntry.getValue().entrySet()){
    		  SpecificationNode fieldNode = new SpecificationNode(RedLinkConfig.NODE_LDPATH);
    		  fieldNode.setAttribute(RedLinkConfig.ATTRIBUTE_FIELD, fieldEntry.getKey());
    		  fieldNode.setAttribute(RedLinkConfig.ATTRIBUTE_LDPATH, fieldEntry.getValue());
    		  nextLdPathNode.addChild(i, fieldNode);
    		  i++;
    	  }
    	  os.addChild(os.getChildCount(), nextLdPathNode);
      }
      
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
  					nextFieldMap.put("FIELD", sn.getAttributeValue(RedLinkConfig.ATTRIBUTE_FIELD));
  					nextFieldMap.put("LDPATH_EXPRESSION", sn.getAttributeValue(RedLinkConfig.ATTRIBUTE_LDPATH));
  					nextFields.add(nextFieldMap);
  				}
  			}
  			fields.add(nextFields);
  		}
  	}
	
	paramMap.put("ENTITY_TYPES", types);
	paramMap.put("LDPATHS", fields);
  }
  
}


