/* $Id: FindHTMLHrefHandler.java 1416199 2012-12-02 16:57:56Z kwright $ */

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
import org.apache.manifoldcf.crawler.system.Logging;
import java.util.regex.*;
import java.util.*;

/** This class is the handler for HTML parsing during state transitions */
public class FindHTMLHrefHandler extends FindHandler implements IHTMLHandler
{
  protected final Pattern preferredLinkPattern;

  public FindHTMLHrefHandler(String parentURI, Pattern preferredLinkPattern)
  {
    super(parentURI);
    this.preferredLinkPattern = preferredLinkPattern;
  }

  /** Apply overrides */
  public void applyOverrides(LoginParameters lp)
    throws ManifoldCFException
  {
    if (targetURI != null && lp != null)
    {
      if (lp.getOverrideTargetURL() != null)
        super.noteDiscoveredLink(lp.getOverrideTargetURL());
    }
  }

  /** Note a character of text.
  * Structured this way to keep overhead low for handlers that don't use text.
  */
  @Override
  public void noteTextCharacter(char textCharacter)
    throws ManifoldCFException
  {
  }

  /** Note a meta tag */
  @Override
  public void noteMetaTag(Map metaAttributes)
    throws ManifoldCFException
  {
  }

  /** Note the start of a form */
  @Override
  public void noteFormStart(Map formAttributes)
    throws ManifoldCFException
  {
  }

  /** Note an input tag */
  @Override
  public void noteFormInput(Map inputAttributes)
    throws ManifoldCFException
  {
  }

  /** Note the end of a form */
  @Override
  public void noteFormEnd()
    throws ManifoldCFException
  {
  }

  /** Override noteDiscoveredLink */
  @Override
  public void noteDiscoveredLink(String rawURL)
    throws ManifoldCFException
  {
    if (targetURI == null)
    {
      Logging.connectors.debug("WEB: Tried to match raw url '"+rawURL+"'");
      super.noteDiscoveredLink(rawURL);
      if (targetURI != null)
      {
        Logging.connectors.debug("WEB: Tried to match cooked url '"+targetURI+"'");
        // Is this a form element we can use?
        boolean canUse;
        if (preferredLinkPattern != null)
        {
          Matcher m = preferredLinkPattern.matcher(targetURI);
          canUse = m.find();
          Logging.connectors.debug("WEB: Preferred link lookup "+((canUse)?"matched":"didn't match")+" '"+targetURI+"'");
        }
        else
        {
          Logging.connectors.debug("WEB: Preferred link lookup for '"+targetURI+"' had no pattern to match");
          canUse = true;
        }
        if (!canUse)
          targetURI = null;
      }
    }
  }

  /** Note discovered href */
  @Override
  public void noteAHREF(String rawURL)
    throws ManifoldCFException
  {
    noteDiscoveredLink(rawURL);
  }

  /** Note discovered href */
  @Override
  public void noteLINKHREF(String rawURL)
    throws ManifoldCFException
  {
    noteDiscoveredLink(rawURL);
  }

  /** Note discovered IMG SRC */
  @Override
  public void noteIMGSRC(String rawURL)
    throws ManifoldCFException
  {
  }

  /** Note discovered FRAME SRC */
  @Override
  public void noteFRAMESRC(String rawURL)
    throws ManifoldCFException
  {
    noteDiscoveredLink(rawURL);
  }

}
