/* $Id: ScriptParseState.java 1444628 2013-02-10 22:55:30Z kwright $ */

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
package org.apache.manifoldcf.crawler.connectors.webcrawler;

import org.apache.manifoldcf.core.interfaces.*;
import org.apache.manifoldcf.core.fuzzyml.*;
import java.util.*;

/** This class interprets the tag stream generated by the HTMLParseState class, and causes script sections to be skipped */
public class ScriptParseState extends HTMLParseState
{
  // Script tag parsing states
  protected static final int SCRIPTPARSESTATE_NORMAL = 0;
  protected static final int SCRIPTPARSESTATE_INSCRIPT = 1;

  protected int scriptParseState = SCRIPTPARSESTATE_NORMAL;

  public ScriptParseState()
  {
    super();
  }

  // Override methods having to do with notification of tag discovery

  @Override
  protected boolean noteTag(String tagName, Map<String,String> attributes)
    throws ManifoldCFException
  {
    if (super.noteTag(tagName,attributes))
      return true;
    switch (scriptParseState)
    {
    case SCRIPTPARSESTATE_NORMAL:
      if (tagName.equals("script"))
        scriptParseState = SCRIPTPARSESTATE_INSCRIPT;
      else
        if (noteNonscriptTag(tagName,attributes))
          return true;
      break;
    case SCRIPTPARSESTATE_INSCRIPT:
      // Skip all tags until we see the end script one.
      break;
    default:
      throw new ManifoldCFException("Unknown script parse state: "+Integer.toString(scriptParseState));
    }
    return false;
  }

  @Override
  protected boolean noteTagEnd(String tagName)
    throws ManifoldCFException
  {
    if (super.noteTagEnd(tagName))
      return true;
    switch (scriptParseState)
    {
    case SCRIPTPARSESTATE_NORMAL:
      if (noteNonscriptEndTag(tagName))
        return true;
      break;
    case SCRIPTPARSESTATE_INSCRIPT:
      // Skip all tags until we see the end script one.
      if (tagName.equals("script"))
        scriptParseState = SCRIPTPARSESTATE_NORMAL;
      break;
    default:
      break;
    }
    return false;
  }

  protected boolean noteNonscriptTag(String tagName, Map<String,String> attributes)
    throws ManifoldCFException
  {
    return false;
  }

  protected boolean noteNonscriptEndTag(String tagName)
    throws ManifoldCFException
  {
    return false;
  }

}
