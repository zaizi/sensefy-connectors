/**
 * (C) Copyright 2015 Zaizi Limited (http://www.zaizi.com).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 3.0 which accompanies this distribution, and is available at 
 * http://www.gnu.org/licenses/lgpl-3.0.en.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 **/
<script type="text/javascript">
<!--
function s${SEQNUM}_checkSpecification()
{
  return true;
}


function s${SEQNUM}_addFieldMapping()
{
  if (editjob.s${SEQNUM}_fieldmapping_source.value == "")
  {
    alert("$Encoder.bodyEscape($ResourceBundle.getString('StanbolEnhancer.NoFieldNameSpecified'))");
    editjob.s${SEQNUM}_fieldmapping_source.focus();
    return;
  }
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


//ldpath prefix mappings
function s${SEQNUM}_addPrefixMapping()
{
  if (editjob.s${SEQNUM}_prefixmapping_source.value == "")
  {
    alert("$Encoder.bodyEscape($ResourceBundle.getString('StanbolEnhancer.NoFieldNameSpecified'))");
    editjob.s${SEQNUM}_prefixmapping_source.focus();
    return;
  }
  editjob.s${SEQNUM}_prefixmapping_op.value="Add";
  postFormSetAnchor("s${SEQNUM}_prefixmapping");
}

function s${SEQNUM}_deletePrefixMapping(i)
{
  // Set the operation
  eval("editjob.s${SEQNUM}_prefixmapping_op_"+i+".value=\"Delete\"");
  // Submit
  if (editjob.s${SEQNUM}_prefixmapping_count.value==i)
    postFormSetAnchor("s${SEQNUM}_prefixmapping");
  else
    postFormSetAnchor("s${SEQNUM}_prefixmapping_"+i)
  // Undo, so we won't get two deletes next time
  eval("editjob.s${SEQNUM}_prefixmapping_op_"+i+".value=\"Continue\"");
}

//ldpath field mappings
function s${SEQNUM}_addLdpathFieldMapping()
{
  if (editjob.s${SEQNUM}_ldpathfieldmapping_source.value == "")
  {
    alert("$Encoder.bodyEscape($ResourceBundle.getString('StanbolEnhancer.NoFieldNameSpecified'))");
    editjob.s${SEQNUM}_ldpathfieldmapping_source.focus();
    return;
  }
  editjob.s${SEQNUM}_ldpathfieldmapping_op.value="Add";
  postFormSetAnchor("s${SEQNUM}_ldpathfieldmapping");
}
//CHANGE BELOW METHOD
function s${SEQNUM}_deleteLdpathFieldMapping(i)
{
  // Set the operation
  eval("editjob.s${SEQNUM}_ldpathfieldmapping_op_"+i+".value=\"Delete\"");
  // Submit
  if (editjob.s${SEQNUM}_ldpathfieldmapping_count.value==i)
    postFormSetAnchor("s${SEQNUM}_ldpathfieldmapping");
  else
    postFormSetAnchor("s${SEQNUM}_ldpathfieldmapping_"+i)
  // Undo, so we won't get two deletes next time
  eval("editjob.s${SEQNUM}_ldpathfieldhmapping_op_"+i+".value=\"Continue\"");
}


//document final field mappings
function s${SEQNUM}_addDocumentFieldMapping()
{
  if (editjob.s${SEQNUM}_docfieldmapping_source.value == "")
  {
    alert("$Encoder.bodyEscape($ResourceBundle.getString('StanbolEnhancer.NoFieldNameSpecified'))");
    editjob.s${SEQNUM}_docfieldmapping_source.focus();
    return;
  }
  editjob.s${SEQNUM}_docfieldmapping_op.value="Add";
  postFormSetAnchor("s${SEQNUM}_docfieldmapping");
}
//CHANGE BELOW METHOD
function s${SEQNUM}_deleteDocumentFieldMapping(i)
{
  // Set the operation
  eval("editjob.s${SEQNUM}_docfieldmapping_op_"+i+".value=\"Delete\"");
  // Submit
  if (editjob.s${SEQNUM}_docfieldmapping_count.value==i)
    postFormSetAnchor("s${SEQNUM}_docfieldmapping");
  else
    postFormSetAnchor("s${SEQNUM}_docfieldmapping_"+i)
  // Undo, so we won't get two deletes next time
  eval("editjob.s${SEQNUM}_docfieldmapping_op_"+i+".value=\"Continue\"");
}

//-->
</script>
