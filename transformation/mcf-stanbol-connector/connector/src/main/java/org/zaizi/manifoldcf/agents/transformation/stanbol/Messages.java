package org.zaizi.manifoldcf.agents.transformation.stanbol;

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

import java.util.Locale;
import java.util.Map;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.interfaces.IHTTPOutput;

public class Messages extends org.apache.manifoldcf.ui.i18n.Messages
{
  public static final String DEFAULT_BUNDLE_NAME="org.zaizi.manifoldcf.agents.transformation.stanbol.common";
  public static final String DEFAULT_PATH_NAME="org.zaizi.manifoldcf.agents.transformation.stanbol";
  
  /** Constructor - do no instantiate
  */
  protected Messages()
  {
  }
  
  public static String getString(Locale locale, String messageKey)
  {
    return getString(DEFAULT_BUNDLE_NAME, locale, messageKey, null);
  }

  public static String getAttributeString(Locale locale, String messageKey)
  {
    return getAttributeString(DEFAULT_BUNDLE_NAME, locale, messageKey, null);
  }

  public static String getBodyString(Locale locale, String messageKey)
  {
    return getBodyString(DEFAULT_BUNDLE_NAME, locale, messageKey, null);
  }

  public static String getAttributeJavascriptString(Locale locale, String messageKey)
  {
    return getAttributeJavascriptString(DEFAULT_BUNDLE_NAME, locale, messageKey, null);
  }

  public static String getBodyJavascriptString(Locale locale, String messageKey)
  {
    return getBodyJavascriptString(DEFAULT_BUNDLE_NAME, locale, messageKey, null);
  }

  public static String getString(Locale locale, String messageKey, Object[] args)
  {
    return getString(DEFAULT_BUNDLE_NAME, locale, messageKey, args);
  }

  public static String getAttributeString(Locale locale, String messageKey, Object[] args)
  {
    return getAttributeString(DEFAULT_BUNDLE_NAME, locale, messageKey, args);
  }
  
  public static String getBodyString(Locale locale, String messageKey, Object[] args)
  {
    return getBodyString(DEFAULT_BUNDLE_NAME, locale, messageKey, args);
  }

  public static String getAttributeJavascriptString(Locale locale, String messageKey, Object[] args)
  {
    return getAttributeJavascriptString(DEFAULT_BUNDLE_NAME, locale, messageKey, args);
  }

  public static String getBodyJavascriptString(Locale locale, String messageKey, Object[] args)
  {
    return getBodyJavascriptString(DEFAULT_BUNDLE_NAME, locale, messageKey, args);
  }

  // More general methods which allow bundlenames and class loaders to be specified.
  
  public static String getString(String bundleName, Locale locale, String messageKey, Object[] args)
  {
    return getString(Messages.class, bundleName, locale, messageKey, args);
  }

  public static String getAttributeString(String bundleName, Locale locale, String messageKey, Object[] args)
  {
    return getAttributeString(Messages.class, bundleName, locale, messageKey, args);
  }

  public static String getBodyString(String bundleName, Locale locale, String messageKey, Object[] args)
  {
    return getBodyString(Messages.class, bundleName, locale, messageKey, args);
  }
  
  public static String getAttributeJavascriptString(String bundleName, Locale locale, String messageKey, Object[] args)
  {
    return getAttributeJavascriptString(Messages.class, bundleName, locale, messageKey, args);
  }

  public static String getBodyJavascriptString(String bundleName, Locale locale, String messageKey, Object[] args)
  {
    return getBodyJavascriptString(Messages.class, bundleName, locale, messageKey, args);
  }

  // Resource output
  
  public static void outputResource(IHTTPOutput output, Locale locale, String resourceKey,
    Map<String,String> substitutionParameters, boolean mapToUpperCase)
    throws ManifoldCFException
  {
    outputResource(output,Messages.class,DEFAULT_PATH_NAME,locale,resourceKey,
      substitutionParameters,mapToUpperCase);
  }
  
  public static void outputResourceWithVelocity(IHTTPOutput output, Locale locale, String resourceKey,
    Map<String,String> substitutionParameters, boolean mapToUpperCase)
    throws ManifoldCFException
  {
    outputResourceWithVelocity(output,Messages.class,DEFAULT_BUNDLE_NAME,DEFAULT_PATH_NAME,locale,resourceKey,
      substitutionParameters,mapToUpperCase);
  }

  public static void outputResourceWithVelocity(IHTTPOutput output, Locale locale, String resourceKey,
    Map<String,Object> contextObjects)
    throws ManifoldCFException
  {
    outputResourceWithVelocity(output,Messages.class,DEFAULT_BUNDLE_NAME,DEFAULT_PATH_NAME,locale,resourceKey,
      contextObjects);
  }
  
}

