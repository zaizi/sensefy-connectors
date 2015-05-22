<!--
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
<script type="text/javascript" src="//ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js"></script>
<script type="text/javascript">

var apikeyMustNotBeNull = "$ResourceBundle.getString('RedLinkEnhancer.ApiKeyMustNotBeNull')";
var analysisNameMustNotBeNull = "$ResourceBundle.getString('RedLinkEnhancer.AnalysisNameMustNotBeNull')";
var entityTypeMustNotBeNull = "$ResourceBundle.getString('RedLinkEnhancer.EntityTypeMustNotBeNull')";
var noFieldNameSpecified = "$ResourceBundle.getString('RedLinkEnhancer.NoFieldNameSpecified')";
var noLDPathExpressionSpecified = "$ResourceBundle.getString('RedLinkEnhancer.NoLDPathExpressionSpecified')";
var fieldNameText = "$ResourceBundle.getString('RedLinkEnhancer.FieldName')";
var ldpathExpressionText = "$ResourceBundle.getString('RedLinkEnhancer.LDPathExpression')";

#[[
(function($) {
]]#
	var sequenceNumber = ${SEQNUM};
#[[	
	jQuery(document).ready(function(){
		var suffix = "_"+sequenceNumber;
		var $entityTypesTable = jQuery("#entity_types_table"+suffix);
		var $entityTypesTableBody = jQuery("#entity_types_table_body"+suffix);
		var $newEntityTypeRow = jQuery("#new_entity_type_row"+suffix);
		
		var entityTypesCount = $entityTypesTableBody.children().length;
		
		/* Function to add fields-ldpath expressions to defined entity types */
		jQuery(document).off("click", "#entity_types_table_body"+suffix+" .add-ldpath").on("click", "#entity_types_table_body"+suffix+" .add-ldpath",  
				function(e) {   
					var $parent = $(this).parent();
					var $name = $parent.find(".entity-type-field").first();
					var $ldpath = $parent.find(".entity-type-ldpath").first();
					if($name.val().trim().length == 0){ 
						alert(noFieldNameSpecified);
						return;
					}
					
					if($ldpath.val().trim().length == 0) {
						alert(noLDPathExpressionSpecified);
						return;
					}
					
					$parent.parent().find("ul").append("<li><span class='field-name'>"+$name.val()+"</span> - <span class='ldpath-expression'>"+$ldpath.val()+"</span><span style='cursor: pointer; margin-left: 5px;' class='remove-expression'>X</span></li>");
					$name.val("");
					$ldpath.val("");
				});
		
		jQuery(".add-entity-type", $entityTypesTableBody).off("click").on("click", 
				function(e) {
					var $entityType = $("#new_entity_type"+suffix);
					if($entityType.val().trim().length == 0) {
						alert(entityTypeMustNotBeNull);
						return;
					}
					
					var template = '<tr>'+
								   '	<td class="entity-type">'+$entityType.val()+' <span style="cursor: pointer; margin-left: 5px;" class="remove-entity-type">X</span></td>'+
								   '    <td class="entity-type-definitions">'+
								   '		<ul></ul>'+
								   '		<div class="entity-type-add-field" id="entity_type_'+entityTypesCount+'_add_field'+suffix+'">'+
								   '			<label>'+fieldNameText+'</label><input type="text" class="entity-type-field" name="entity_type_'+entityTypesCount+'_field_name"'+suffix+'><label>'+ldpathExpressionText+'</label><input type="text" class="entity-type-ldpath" name="entity_type_'+entityTypesCount+'_ldpath'+suffix+'"><input type="button" class="add-ldpath" value="Add">'
								   '		</div>'+
								   '	</td>'+
								   '</tr>';
					
					$newEntityTypeRow.before($(template));
					entityTypesCount++;
					$entityType.val("");
				});
		
		/* Remove entity type listener */
		jQuery(document).off("click", "#entity_types_table_body"+suffix+" .remove-entity-type").on("click", "#entity_types_table_body"+suffix+" .remove-entity-type",  
				function(e) { 
					$(this).parent().parent().remove();
		});
		
		/* Remove ldpath expression listener */
		jQuery(document).off("click", "#entity_types_table_body"+suffix+" .remove-expression").on("click", "#entity_types_table_body"+suffix+" .remove-expression",  
				function(e) { 
					$(this).parent().remove();
		});
		if(window.editjob.children.tabname.value === "RedLink Configuration") {
			jQuery("input[type=button][value=Save]").attr("onclick", "").off("click").on("click", function(){
]]#
				var redlinkEnhancerJson = JSON.stringify(createRedLinkEnhancerJson${SEQNUM}());
#[[
				$("#redlink_json_conf"+suffix).val(redlinkEnhancerJson);
				Save();
			});
		}
	});
})(jQuery);

]]#

/**
 * <p>Function to create the JSON representation expected by RedLinkEnhancer connector</p>
 */
var createRedLinkEnhancerJson${SEQNUM} = function() {
	
	var json = new Object();
	
	var apiKey = $("#redlink_apikey_${SEQNUM}").val();
	var analysis = $("#redlink_analysis_${SEQNUM}").val();
	var common = $("#redlink_common_${SEQNUM}").is(":checked") ? "true" : "false";
	
	//API KEY
	json["api_key"] = new Object();
	json["api_key"]["_attribute_api_key_value"] = apiKey;
	
	//ANALYSIS NAME
	json["analysis_name"] = new Object();
	json["analysis_name"]["_attribute_analysis_name_value"] = analysis;
	
	//COMMON PARAMETERS
	json["common_parameters"] = new Object();
	json["common_parameters"]["_attribute_common_parameters_value"] = common;
	
	//ENTITY TYPES
	json["entity_type"] = new Array();
	
	$("#entity_types_table_body_${SEQNUM}").find(".entity-type").each(function(i,e){
	  $(".remove-entity-type", $(e)).remove();
	  var entType = $(e).html().trim();
	  
	  var entityTypeJson = new Object();
	  entityTypeJson["_attribute_entity_type"] = entType;
	  var ldpathArray = [];
	  if($(e).next().find("li").length > 0) {
	  	$(e).next().find("li").each(function(i,e){
	  		var $li = $(e);
	  		var field = $li.children(".field-name").html();
	  		var ldpath = $li.children(".ldpath-expression").html();
	  		var fl = new Object();
	  		fl["_attribute_field"] = field;
	  		fl["_attribute_ldpath_expression"] = ldpath;
	  		ldpathArray.push(fl);
	  	});
	  	
	  	entityTypeJson["ldpath"] = ldpathArray;
		json["entity_type"].push(entityTypeJson);
	  
	  }
	});
	
	// PROFILES
	var profiles = new Array();
	
	// FREEBASE
	if($("#redlink_profile_freebase_${SEQNUM}").is(":checked")) {
		var profile = new Object();
		profile['profile'] = new Object();
		profile['profile']['_attribute_profile_value'] = "freebase"; 
		profiles.push(profile);
	}
	
	// SKOS
	if($("#redlink_profile_skos_${SEQNUM}").is(":checked")) {
		var profile = new Object();
		profile['profile'] = new Object();
		profile['profile']['_attribute_profile_value'] = "skos";
		profiles.push(profile);
	}

	// Default
	if($("#redlink_profile_default_${SEQNUM}").is(":checked")) {
		var profile = new Object();
		profile['profile'] = new Object();
		profile['profile']['_attribute_profile_value'] = "default";
		profiles.push(profile);
	}
	
	json["profiles"] = profiles;
	return json;
}

function s${SEQNUM}_checkSpecification()
{
  return true;
}

function s${SEQNUM}_addFieldMapping()
{
  editjob.s${SEQNUM}_fieldmapping_op.value="Add";
  postFormSetAnchor("s${SEQNUM}_fieldmapping");
}

function s${SEQNUM}_deleteFieldMapping(i)
{
  // Set the operation
  eval("editjob.s${SEQNUM}_fieldmapping_op_"+i+".value=\"Delete\"");
  // Submit
  if (editjob.s${SEQNUM}_fieldmapping_count.value==i)
    postFormSetAnchor("s${SEQNUM}_fieldmapping");
  else
    postFormSetAnchor("s${SEQNUM}_fieldmapping_"+i)
  // Undo, so we won't get two deletes next time
  eval("editjob.s${SEQNUM}_fieldmapping_op_"+i+".value=\"Continue\"");
}

</script>
