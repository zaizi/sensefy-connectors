package org.alfresco.consulting.indexer.dao;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * @author iarroyo
 *
 */

@ManagedResource
public class IndexingDaoJMX
{

    private IndexingDaoImpl indexingDaoImpl;

    private final static String DELIMITER = ",";

    public void setIndexingDaoImpl(IndexingDaoImpl indexingDaoImpl)
    {
        this.indexingDaoImpl = indexingDaoImpl;
    }

    @ManagedAttribute
    public void setAllowedTypes(String allowedTypes)
    {
        Set<String> types = tokenizeString(allowedTypes);
        indexingDaoImpl.setAllowedTypes(types);
    }

    @ManagedAttribute
    public String getAllowedTypes()
    {
        return collectionToString(indexingDaoImpl.getAllowedTypes(), DELIMITER);
    }

    @ManagedAttribute
    public void setExcludedNameExtension(String exludedNameExtension)
    {
        Set<String> excludedNameExtensions = tokenizeString(exludedNameExtension);
        indexingDaoImpl.setExcludedNameExtension(excludedNameExtensions);
    }

    @ManagedAttribute
    public String getExcludedNameExtension()
    {
        return collectionToString(indexingDaoImpl.getExcludedNameExtension(), DELIMITER);
    }

    @ManagedAttribute
    public void setProperties(String properties)
    {
        Set<String> allowedProperties = tokenizeString(properties);
        indexingDaoImpl.setProperties(allowedProperties);
    }

    @ManagedAttribute
    public String getProperties()
    {
        return collectionToString(indexingDaoImpl.getProperties(), DELIMITER);
    }

    @ManagedAttribute
    public void setAspects(String aspects)
    {
        Set<String> allowedAspects = tokenizeString(aspects);
        indexingDaoImpl.setAspects(allowedAspects);
    }

    @ManagedAttribute
    public String getAspects()
    {
        return collectionToString(this.indexingDaoImpl.getAspects(), DELIMITER);
    }

    @ManagedAttribute
    public void setMimeTypes(String mimetypes)
    {
        Set<String> allowedMimetypes = tokenizeString(mimetypes);
        indexingDaoImpl.setMimeTypes(allowedMimetypes);
    }

    @ManagedAttribute
    public String getMimeTypes()
    {
        return collectionToString(indexingDaoImpl.getMimeTypes(), DELIMITER);
    }
    
    @ManagedAttribute
    public void setSites(String sites)
    {
        Set<String> allowedSites = tokenizeString(sites);
        indexingDaoImpl.setSites(allowedSites);
    }

    @ManagedAttribute
    public String getSites()
    {
        return collectionToString(indexingDaoImpl.getSites(), DELIMITER);
    }

    private Set<String> tokenizeString(String str)
    {

        Set<String> tokens = null;
        StringTokenizer tokenizer = new StringTokenizer(str, DELIMITER);

        if (tokenizer.countTokens() > 0)
        {
            tokens = new HashSet<String>();
            while (tokenizer.hasMoreTokens())
            {
                tokens.add(tokenizer.nextToken().trim());
            }
        }

        return tokens;
    }

    private String collectionToString(Set<String> collection, String delimiter)
    {

        StringBuilder sb = new StringBuilder();
        if (collection != null)
        {
            for (String str : collection)
            {
                if (sb.length() > 0)
                {
                    sb.append(delimiter);
                }
                sb.append(str);
            }
        }

        return sb.toString();
    }

}
