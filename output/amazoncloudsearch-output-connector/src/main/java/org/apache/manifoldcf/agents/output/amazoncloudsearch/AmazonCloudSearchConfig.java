package org.apache.manifoldcf.agents.output.amazoncloudsearch;

import org.apache.manifoldcf.core.interfaces.ConfigParams;
import org.apache.manifoldcf.core.interfaces.IPostParameters;

/**
 * Amazon Cloud Search Config class
 * 
 * @author aayala
 *
 */
public class AmazonCloudSearchConfig extends AmazonCloudSearchParam
{
    /**
	 * 
	 */
    private static final long serialVersionUID = -2071296573398352538L;

    /** Parameters used for the configuration */
    final private static ParameterEnum[] CONFIGURATIONLIST = { ParameterEnum.DOCUMENTSERVICEENDPOINT };

    /**
     * Build a set of AmazonCloudSearchParameters by reading ConfigParams. If the value returned by
     * ConfigParams.getParameter is null, the default value is set.
     * 
     * @param paramList
     * @param params
     */
    public AmazonCloudSearchConfig(ConfigParams params)
    {
        super(CONFIGURATIONLIST);
        for (ParameterEnum param : CONFIGURATIONLIST)
        {
            String value = params.getParameter(param.name());
            if (value == null)
                value = param.defaultValue;
            put(param, value);
        }
    }

    /** @return a unique identifier for one index on one AmazonCloudSearch instance. */
    public String getUniqueIndexIdentifier()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(getDocumentServerEndpoint());
        return sb.toString();
    }

    public final static void contextToConfig(IPostParameters variableContext, ConfigParams parameters)
    {
        for (ParameterEnum param : CONFIGURATIONLIST)
        {
            String p = variableContext.getParameter(param.name().toLowerCase());
            if (p != null)
                parameters.setParameter(param.name(), p);
        }
    }

    final public String getDocumentServerEndpoint()
    {
        return get(ParameterEnum.DOCUMENTSERVICEENDPOINT);
    }

}
