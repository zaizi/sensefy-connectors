package org.apache.manifoldcf.agents.output.amazoncloudsearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.apache.manifoldcf.core.interfaces.ConfigurationNode;
import org.apache.manifoldcf.core.interfaces.IPostParameters;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Amazon Cloud Search Specs class
 * 
 * @author aayala
 * 
 */
public class AmazonCloudSearchSpecs extends AmazonCloudSearchParam
{
    private static final long serialVersionUID = 1859653440572662025L;

    final public static ParameterEnum[] SPECIFICATIONLIST = { ParameterEnum.MAXFILESIZE, ParameterEnum.MIMETYPES,
            ParameterEnum.EXTENSIONS };

    final public static String AMAZONCLOUDSEARCH_SPECS_NODE = "AMAZONCLOUDSEARCH_SPECS_NODE";

    private Set<String> extensionSet;

    private Set<String> mimeTypeSet;

    /**
     * Build a set of AmazonCloudSearch parameters by reading an JSON object
     * 
     * @param json
     * @throws JSONException
     * @throws ManifoldCFException
     */
    public AmazonCloudSearchSpecs(JSONObject json) throws JSONException, ManifoldCFException
    {
        super(SPECIFICATIONLIST);
        extensionSet = null;
        mimeTypeSet = null;
        for (ParameterEnum param : SPECIFICATIONLIST)
        {
            String value = null;
            value = json.getString(param.name());
            if (value == null)
                value = param.defaultValue;
            put(param, value);
        }
        extensionSet = createStringSet(getExtensions());
        mimeTypeSet = createStringSet(getMimeTypes());
    }

    /**
     * Build a set of AmazonCloudSearch parameters by reading an instance of SpecificationNode.
     * 
     * @param node
     * @throws ManifoldCFException
     */
    public AmazonCloudSearchSpecs(ConfigurationNode node) throws ManifoldCFException
    {
        super(SPECIFICATIONLIST);
        for (ParameterEnum param : SPECIFICATIONLIST)
        {
            String value = null;
            if (node != null)
                value = node.getAttributeValue(param.name());
            if (value == null)
                value = param.defaultValue;
            put(param, value);
        }
        extensionSet = createStringSet(getExtensions());
        mimeTypeSet = createStringSet(getMimeTypes());
    }

    public static void contextToSpecNode(IPostParameters variableContext, ConfigurationNode specNode)
    {
        for (ParameterEnum param : SPECIFICATIONLIST)
        {
            String p = variableContext.getParameter(param.name().toLowerCase());
            if (p != null)
                specNode.setAttribute(param.name(), p);
        }
    }

    /** @return a JSON representation of the parameter list */
    public JSONObject toJson()
    {
        return new JSONObject(this);
    }

    public long getMaxFileSize()
    {
        return Long.parseLong(get(ParameterEnum.MAXFILESIZE));
    }

    public String getMimeTypes()
    {
        return get(ParameterEnum.MIMETYPES);
    }

    public String getExtensions()
    {
        return get(ParameterEnum.EXTENSIONS);
    }

    private final static TreeSet<String> createStringSet(String content) throws ManifoldCFException
    {
        TreeSet<String> set = new TreeSet<String>();
        BufferedReader br = null;
        StringReader sr = null;
        try
        {
            sr = new StringReader(content);
            br = new BufferedReader(sr);
            String line = null;
            while ((line = br.readLine()) != null)
            {
                line = line.trim();
                if (line.length() > 0)
                    set.add(line);
            }
            return set;
        }
        catch (IOException e)
        {
            throw new ManifoldCFException(e);
        }
        finally
        {
            if (br != null)
                IOUtils.closeQuietly(br);
        }
    }

    /**
     * Check validity of an extension
     * 
     * @param extension
     * @return
     */
    public boolean checkExtension(String extension)
    {
        if (extension == null)
            extension = "";
        return extensionSet.contains(extension);
    }

    /**
     * Check validity of a mimetype
     * 
     * @param mimeType
     * @return
     */
    public boolean checkMimeType(String mimeType)
    {
        if (mimeType == null)
            mimeType = "application/unknown";
        return mimeTypeSet.contains(mimeType);
    }
}
