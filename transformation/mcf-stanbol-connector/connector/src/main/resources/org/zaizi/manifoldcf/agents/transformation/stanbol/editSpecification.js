/*******************************************************************************
 * Sensefy
 *
 * Copyright (c) Zaizi Limited, All rights reserved.
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 *******************************************************************************/
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
//-->
</script>
