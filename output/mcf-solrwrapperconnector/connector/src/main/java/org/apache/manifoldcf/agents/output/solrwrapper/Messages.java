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
package org.apache.manifoldcf.agents.output.solrwrapper;

import org.apache.manifoldcf.core.interfaces.IHTTPOutput;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.ui.i18n.ResourceBundleWrapper;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * 
 * @author Antonio David Perez Morales <aperez@zaizi.com>
 *
 */
public class Messages extends org.apache.manifoldcf.ui.i18n.Messages {
    public static final String DEFAULT_BUNDLE_NAME = "org.apache.manifoldcf.agents.output.solrwrapper.common";
    public static final String DEFAULT_PATH_NAME = "org.apache.manifoldcf.agents.output.solrwrapper";

    /**
     * Constructor - do no instantiate
     */
    protected Messages() {}

    public static String getString(Locale locale, String messageKey) {
        return getString(DEFAULT_BUNDLE_NAME, locale, messageKey, null);
    }

    public static String getAttributeString(Locale locale, String messageKey) {
        return getAttributeString(DEFAULT_BUNDLE_NAME, locale, messageKey, null);
    }

    public static String getBodyString(Locale locale, String messageKey) {
        return getBodyString(DEFAULT_BUNDLE_NAME, locale, messageKey, null);
    }

    public static String getAttributeJavascriptString(Locale locale, String messageKey) {
        return getAttributeJavascriptString(DEFAULT_BUNDLE_NAME, locale, messageKey, null);
    }

    public static String getBodyJavascriptString(Locale locale, String messageKey) {
        return getBodyJavascriptString(DEFAULT_BUNDLE_NAME, locale, messageKey, null);
    }

    public static String getString(Locale locale, String messageKey, Object[] args) {
        return getString(DEFAULT_BUNDLE_NAME, locale, messageKey, args);
    }

    public static String getAttributeString(Locale locale, String messageKey, Object[] args) {
        return getAttributeString(DEFAULT_BUNDLE_NAME, locale, messageKey, args);
    }

    public static String getBodyString(Locale locale, String messageKey, Object[] args) {
        return getBodyString(DEFAULT_BUNDLE_NAME, locale, messageKey, args);
    }

    public static String getAttributeJavascriptString(Locale locale, String messageKey, Object[] args) {
        return getAttributeJavascriptString(DEFAULT_BUNDLE_NAME, locale, messageKey, args);
    }

    public static String getBodyJavascriptString(Locale locale, String messageKey, Object[] args) {
        return getBodyJavascriptString(DEFAULT_BUNDLE_NAME, locale, messageKey, args);
    }

    // More general methods which allow bundlenames and class loaders to be
    // specified.

    public static String getString(String bundleName, Locale locale, String messageKey, Object[] args) {
        return getString(Messages.class, bundleName, locale, messageKey, args);
    }

    public static String getAttributeString(String bundleName, Locale locale, String messageKey, Object[] args) {
        return getAttributeString(Messages.class, bundleName, locale, messageKey, args);
    }

    public static String getBodyString(String bundleName, Locale locale, String messageKey, Object[] args) {
        return getBodyString(Messages.class, bundleName, locale, messageKey, args);
    }

    public static String getAttributeJavascriptString(String bundleName,
                                                      Locale locale,
                                                      String messageKey,
                                                      Object[] args) {
        return getAttributeJavascriptString(Messages.class, bundleName, locale, messageKey, args);
    }

    public static String getBodyJavascriptString(String bundleName,
                                                 Locale locale,
                                                 String messageKey,
                                                 Object[] args) {
        return getBodyJavascriptString(Messages.class, bundleName, locale, messageKey, args);
    }

    // Resource output

    public static void outputResource(IHTTPOutput output,
                                      Locale locale,
                                      String resourceKey,
                                      Map<String,String> substitutionParameters,
                                      boolean mapToUpperCase) throws ManifoldCFException {
        outputResource(output, Messages.class, DEFAULT_PATH_NAME, locale, resourceKey,
            substitutionParameters, mapToUpperCase);
    }

    public static void outputResourceWithVelocity(IHTTPOutput output,
                                                  Locale locale,
                                                  String resourceKey,
                                                  Map<String,String> substitutionParameters,
                                                  boolean mapToUpperCase) throws ManifoldCFException {
        outputResourceWithVelocity(output, Messages.class, DEFAULT_BUNDLE_NAME, DEFAULT_PATH_NAME, locale,
            resourceKey, substitutionParameters, mapToUpperCase);
    }

    public static void outputResourceWithVelocity(IHTTPOutput output,
                                                  @SuppressWarnings("rawtypes") Class clazz,
                                                  String bundleName,
                                                  String pathName,
                                                  Locale locale,
                                                  String resourceKey,
                                                  Map<String,String> substitutionParameters,
                                                  boolean mapToUpperCase) throws ManifoldCFException {
        Map<String,Object> contextObjects = null;
        if (substitutionParameters != null) {
            contextObjects = new HashMap<String,Object>();
            Iterator<String> i = substitutionParameters.keySet().iterator();
            while (i.hasNext()) {
                String key = i.next();
                String value = substitutionParameters.get(key);
                if (mapToUpperCase) key = key.toUpperCase();
                if (value == null) value = "";

                contextObjects.put(key, value);
                /*
                contextObjects.put(key + "_A", Encoder.attributeEscape(value));
                contextObjects.put(key + "_B", Encoder.bodyEscape(value));
                contextObjects.put(key + "_AJ", Encoder.attributeJavascriptEscape(value));
                contextObjects.put(key + "_BJ", Encoder.bodyJavascriptEscape(value)); */
            }
        }
        outputResourceWithVelocity(output, clazz, bundleName, pathName, locale, resourceKey, contextObjects);
    }

    /*
     * public static void outputResourceWithVelocity(IHTTPOutput output, Locale locale, String resourceKey,
     * Map<String, Object> contextObjects) throws ManifoldCFException { outputResourceWithVelocity(output,
     * Messages.class, DEFAULT_BUNDLE_NAME, DEFAULT_PATH_NAME, locale, resourceKey, contextObjects); }
     */

    public static void outputResourceWithVelocity(IHTTPOutput output,
                                                  @SuppressWarnings("rawtypes") Class clazz,
                                                  String bundleName,
                                                  String pathName,
                                                  Locale locale,
                                                  String resourceKey,
                                                  Map<String,Object> contextObjects) throws ManifoldCFException {
        VelocityEngine engine = createVelocityEngine(clazz);
        try {
            VelocityContext context = new VelocityContext();

            // Add utility methods the UI needs
            context.put("Encoder", org.apache.manifoldcf.ui.util.Encoder.class);
            context.put("Formatter", org.apache.manifoldcf.ui.util.Formatter.class);
            context.put("MultilineParser", org.apache.manifoldcf.ui.util.MultilineParser.class);

            context.put("Integer", Integer.class);
            // Add in the resource bundle
            ResourceBundle rb = getResourceBundle(clazz, bundleName, locale);
            context.put("ResourceBundle", new ResourceBundleWrapper(rb, bundleName, locale));

            if (contextObjects != null) {
                Iterator<String> i = contextObjects.keySet().iterator();
                while (i.hasNext()) {
                    String key = i.next();
                    Object value = contextObjects.get(key);
                    context.put(key, value);
                }
            }

            String resourcePath = localizeResourceName(pathName, resourceKey, locale);

            Writer outputWriter = new OutputWriter(output);
            engine.mergeTemplate(resourcePath, "UTF-8", context, outputWriter);
            outputWriter.flush();
        } catch (IOException e) {
            throw new ManifoldCFException(e.getMessage(), e);
        }
    }

    private static String localizeResourceName(String pathName, String resourceName, Locale locale) {
        return resourceName;
    }

    private static class OutputWriter extends Writer {
        private IHTTPOutput output;

        public OutputWriter(IHTTPOutput output) {
            super();
            this.output = output;
        }

        public void write(char[] cbuf, int off, int len) throws IOException {
            if (off == 0 && len == cbuf.length) output.print(cbuf);
            else output.print(new String(cbuf, off, len));
        }

        public void close() throws IOException {}

        public void flush() throws IOException {}

    }

}
