package org.alfresco.consulting.indexer.webscripts;

import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.consulting.indexer.utils.Utils;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.admin.SysAdminParams;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.domain.node.NodeDAO;
import org.alfresco.repo.domain.permissions.Acl;
import org.alfresco.repo.domain.permissions.AclDAO;
import org.alfresco.repo.security.permissions.AccessControlEntry;
import org.alfresco.repo.thumbnail.ThumbnailDefinition;
import org.alfresco.repo.thumbnail.ThumbnailRegistry;
import org.alfresco.repo.thumbnail.script.ScriptThumbnailService;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.thumbnail.ThumbnailService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.google.gdata.util.common.base.StringUtil;

/**
 * Given a nodeRef, renders out all data about a node (except binary content): - Node metadata - Node ACLs
 * 
 * Please check
 * src/main/amp/config/alfresco/extension/templates/webscripts/org/alfresco/consulting/indexer/webscripts/details
 * .get.desc.xml to know more about the RestFul interface to invoke the WebScript
 * 
 * List of pending activities (or TODOs) - Refactor recursive getAllAcls (direct recursion) . Evaluate the possibility
 * to write a SQL statement for that - Move private/static logic into the IndexingService (see notes on
 * NodeChangesWebScript) - Move the following methods (and related SQL statements) into IndexingDaoImpl --
 * nodeService.getProperties -- nodeService.getAspects -- nodeDao.getNodeAclId -- solrDao.getNodesByAclChangesetId --
 * nodeService.getType and dictionaryService.isSubClass (should be merged into one) - Using JSON libraries (or
 * StringBuffer), render out the payload without passing through FreeMarker template
 */
public class NodeDetailsWebScript extends DeclarativeWebScript implements InitializingBean, ResourceLoaderAware
{

    protected static final Log logger = LogFactory.getLog(NodeDetailsWebScript.class);
    protected static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    protected static final String documentDetailsFormat = "/page/document-details?nodeRef=%s";
    protected static final String documentDetailsSiteFormat = "/page/site/%s/document-details?nodeRef=%s";
    protected static final String parentDocumentLibraryViewFormat = "/page/site/%s/documentlibrary#filter=path|%s&page=1";
    protected static final String parentRepositoryViewFormat = "/page/repository#filter=path|%s&page=1";

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache)
    {
        final List<String> readableAuthorities = new ArrayList<String>();

        // Parsing parameters passed from the WebScript invocation
        Map<String, String> templateArgs = req.getServiceMatch().getTemplateVars();
        String storeId = templateArgs.get("storeId");
        String storeProtocol = templateArgs.get("storeProtocol");
        String uuid = templateArgs.get("uuid");
        NodeRef nodeRef = new NodeRef(storeProtocol, storeId, uuid);
        logger.debug(String.format("Invoking ACLs Webscript, using the following params\n" + "nodeRef: %s\n", nodeRef));

        // Processing properties
        Map<QName, Serializable> propertyMap = nodeService.getProperties(nodeRef);
        Map<String, Pair<String, String>> properties = toStringMap(propertyMap);

        // Processing aspects
        Set<QName> aspectsSet = nodeService.getAspects(nodeRef);
        Set<String> aspects = toStringSet(aspectsSet);

        // Get the node ACL Id
        Long dbId = (Long) propertyMap.get(ContentModel.PROP_NODE_DBID);
        Long nodeAclId = nodeDao.getNodeAclId(dbId);

        // Get also the inherited ones
        List<Acl> acls = getAllAcls(nodeAclId);
        // @TODO - avoid reverse by implementing direct recursion
        Collections.reverse(acls);

        // Getting path and siteName
        Path pathObj = nodeService.getPath(nodeRef);
        String path = pathObj.toPrefixString(namespaceService);
        String siteName = Utils.getSiteName(pathObj);

        // Walk through ACLs and related ACEs, rendering out authority names having a granted permission on the node
        for (Acl acl : acls)
        {
            List<AccessControlEntry> aces = aclDao.getAccessControlList(acl.getId()).getEntries();
            for (AccessControlEntry ace : aces)
            {
                if (inclusionAclPermissions.contains(ace.getPermission().getName())
                        && ace.getAccessStatus().equals(AccessStatus.ALLOWED))
                {
                    if (!readableAuthorities.contains(ace.getAuthority()))
                    {
                        readableAuthorities.add(ace.getAuthority());
                    }
                }
            }
        }

        Map<String, Object> model = new HashMap<String, Object>(1, 1.0f);
        model.put("nsResolver", namespaceService);
        model.put("readableAuthorities", readableAuthorities);
        model.put("properties", properties);
        model.put("aspects", aspects);
        model.put("path", path);
        model.put("contentUrlPrefix", contentUrlPrefix);
        model.put("shareUrlPrefix", shareUrlPrefix);
        model.put("thumbnailUrlPrefix", thumbnailUrlPrefix);
        model.put("previewUrlPrefix", previewUrlPrefix);

        NodeRef parentRef = nodeService.getPrimaryParent(nodeRef).getParentRef();
        model.put("parentRef", parentRef.toString());

        // Calculating the contentUrlPath and adding it only if the contentType is child of cm:content
        boolean isContentAware = isContentAware(nodeRef);
        String mimetype = null;
        String contentUrlPath = null;
        if (isContentAware)
        {
            contentUrlPath = String.format("/api/node/%s/%s/%s/content", storeProtocol, storeId, uuid);
            model.put("contentUrlPath", contentUrlPath);

            if (properties.containsKey("cm:content"))
            {
                String valueContent = properties.get("cm:content").getSecond();
                ContentURL contentURL = new ContentURL(valueContent);
                mimetype = contentURL.getMimetype();
                model.put("mimetype", mimetype);
                model.put("size", contentURL.getSize());
            }
        }

        // Rendering out the (relative) URL path to Alfresco Share
        String shareUrlPath = null;
        String shareParentUrlPath = null;

        Path parentPath = nodeService.getPath(nodeRef);

        if (!StringUtil.isEmpty(siteName))
        {
            String parentDisplayPath = parentPath.toDisplayPath(nodeService, permissionService);

            if (parentDisplayPath.contains("documentLibrary"))
            {
                shareUrlPath = String.format(documentDetailsSiteFormat, siteName, nodeRef.toString());

                parentDisplayPath = parentDisplayPath.substring(parentDisplayPath.indexOf("documentLibrary") + 15);
                shareParentUrlPath = String.format(parentDocumentLibraryViewFormat, siteName, parentDisplayPath);
            }
            else
            {
                shareUrlPath = String.format(documentDetailsFormat, nodeRef.toString());

                parentDisplayPath = parentPath.subPath(2, parentPath.size() - 1).toDisplayPath(nodeService,
                        permissionService);
                shareParentUrlPath = String.format(parentRepositoryViewFormat, parentDisplayPath);
            }
        }
        else
        {
            shareUrlPath = String.format(documentDetailsFormat, nodeRef.toString());

            String parentDisplayPath = parentPath.subPath(2, parentPath.size() - 1).toDisplayPath(nodeService,
                    permissionService);
            shareParentUrlPath = String.format(parentRepositoryViewFormat, parentDisplayPath);
        }

        if (shareUrlPath != null)
        {
            model.put("shareUrlPath", shareUrlPath);
        }
        if (shareParentUrlPath != null)
        {
            model.put("shareParentUrlPath", shareParentUrlPath);
        }

        String thumbnailUrlPath = String.format(
                "/api/node/%s/%s/%s/content/thumbnails/doclib?c=queue&ph=true&lastModified=1", storeProtocol, storeId,
                uuid);
        model.put("thumbnailUrlPath", thumbnailUrlPath);

        ThumbnailRegistry registry = thumbnailService.getThumbnailRegistry();
        ThumbnailDefinition details = registry.getThumbnailDefinition("doclib");
        try
        {
            NodeRef thumbRef = thumbnailService.getThumbnailByName(nodeRef, ContentModel.PROP_CONTENT, "doclib");
            if (thumbRef == null)
            {
                thumbRef = thumbnailService.createThumbnail(nodeRef, ContentModel.PROP_CONTENT, details.getMimetype(),
                        details.getTransformationOptions(), details.getName());
            }

            ContentReader thumbReader = contentService.getReader(thumbRef, ContentModel.PROP_CONTENT);
            InputStream thumbsIs = thumbReader.getContentInputStream();
            String thumbBase64 = Base64.encodeBase64String(IOUtils.toByteArray(thumbsIs));
            model.put("thumbnailBase64", thumbBase64);
            IOUtils.closeQuietly(thumbsIs);
        }
        catch (Exception e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("It was not possible to get/build thumbnail doclib for nodeRef: " + nodeRef
                        + ". Message: " + e.getMessage());
            }

            // thumbnail placeholder
            String phPath = scriptThumbnailService.getMimeAwarePlaceHolderResourcePath("doclib", mimetype);

            StringBuilder sb = new StringBuilder("classpath:").append(phPath);
            final String classpathResource = sb.toString();

            InputStream is = null;
            try
            {
                is = resourceLoader.getResource(classpathResource).getInputStream();
                String thumbBase64 = Base64.encodeBase64String(IOUtils.toByteArray(is));
                model.put("thumbnailBase64", thumbBase64);
            }
            catch (Exception e2)
            {
                if (logger.isWarnEnabled())
                {
                    logger.warn("It was not possible to get/build placeholder thumbnail doclib for nodeRef: " + nodeRef
                            + ". Message: " + e2.getMessage());
                }
            }
            finally
            {
                IOUtils.closeQuietly(is);
            }

        }

        // Preview URL
        String previewUrlTemplate = null;
        List<String> thumbDefinitions = getThumbnailDefinitions(nodeRef);
        if (mimetype.equals(MimetypeMap.MIMETYPE_PDF))
        {
            previewUrlTemplate = "/api/node/%s/%s/%s/content";
        }
        else if (mimetype.startsWith("image/"))
        {
            previewUrlTemplate = "/api/node/%s/%s/%s/content/thumbnails/imgpreview?c=force";
        }
        else if (thumbDefinitions.contains("pdf"))
        {
            previewUrlTemplate = "/api/node/%s/%s/%s/content/thumbnails/pdf?c=force";
        }
        else if (thumbDefinitions.contains("imgpreview"))
        {
            previewUrlTemplate = "/api/node/%s/%s/%s/content/thumbnails/imgpreview?c=force";
        }
        if (previewUrlTemplate != null)
        {
            String previewUrlPath = String.format(previewUrlTemplate, storeProtocol, storeId, uuid);
            model.put("previewUrlPath", previewUrlPath);
        }

        // Img preview URL
        String imgPreviewUrlTemplate = "/api/node/%s/%s/%s/content/thumbnails/imgpreview?c=queue&ph=true";
        model.put("imgPreviewUrlPath", String.format(imgPreviewUrlTemplate, storeProtocol, storeId, uuid));

        return model;
    }

    public List<String> getThumbnailDefinitions(NodeRef nodeRef)
    {
        List<String> result = new ArrayList<String>(7);

        ContentReader contentReader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
        if (contentReader != null)
        {
            String mimetype = contentReader.getMimetype();
            List<ThumbnailDefinition> thumbnailDefinitions = thumbnailService.getThumbnailRegistry().getThumbnailDefinitions(
                    mimetype, contentReader.getSize());
            for (ThumbnailDefinition thumbnailDefinition : thumbnailDefinitions)
            {
                result.add(thumbnailDefinition.getName());
            }
        }

        return result;
    }

    private boolean isContentAware(NodeRef nodeRef)
    {
        QName contentType = nodeService.getType(nodeRef);
        return dictionaryService.isSubClass(contentType, ContentModel.TYPE_CONTENT);
    }

    private Set<String> toStringSet(Set<QName> aspectsSet)
    {
        Set<String> ret = new HashSet<String>();
        for (QName aspect : aspectsSet)
        {
            ret.add(aspect.toPrefixString(namespaceService));
        }
        return ret;
    }

    private Map<String, Pair<String, String>> toStringMap(Map<QName, Serializable> propertyMap)
    {
        Map<String, Pair<String, String>> ret = new HashMap<String, Pair<String, String>>(1, 1.0f);
        for (QName propertyName : propertyMap.keySet())
        {
            Serializable propertyValue = propertyMap.get(propertyName);
            if (propertyValue != null)
            {
                String propertyType = propertyValue.getClass().getName();
                String stringValue = propertyValue.toString();
                if (propertyType.equals("java.util.Date"))
                {
                    stringValue = sdf.format(propertyValue);
                }
                ret.put(propertyName.toPrefixString(namespaceService), new Pair<String, String>(propertyType,
                        stringValue));
            }
        }
        return ret;
    }

    private List<Acl> getAllAcls(Long nodeAclId)
    {
        logger.debug("getAllAcls from " + nodeAclId);
        Acl acl = aclDao.getAcl(nodeAclId);
        Long parentNodeAclId = acl.getInheritsFrom();
        logger.debug("parent acl is  " + parentNodeAclId);
        if (parentNodeAclId == null || !acl.getInherits())
        {
            List<Acl> ret = new ArrayList<Acl>();
            ret.add(acl);
            return ret;
        }
        else
        {
            List<Acl> inheritedAcls = getAllAcls(parentNodeAclId);
            logger.debug("Current acl with id " + nodeAclId + " is " + acl);
            inheritedAcls.add(acl);
            return inheritedAcls;
        }
    }

    private class ContentURL
    {

        private String mimetype;
        private String size;

        public ContentURL(String contentURL)
        {

            String data[] = contentURL.split("\\|");
            for (String value : data)
            {
                // parse mimetype
                if (value.startsWith("mimetype"))
                {
                    this.mimetype = splitAndGetValueBy(value, "=", 1);
                }
                // parse size
                if (value.startsWith("size"))
                {
                    this.size = splitAndGetValueBy(value, "=", 1);
                }

            }

        }

        public String splitAndGetValueBy(String str, String splitBy, int pos)
        {
            String[] values = str.split(splitBy);
            String value = "unknown";
            if (values.length > pos)
            {
                value = values[pos];
            }
            return value;
        }

        public String getMimetype()
        {
            return this.mimetype;
        }

        public String getSize()
        {
            return this.size;
        }
    }

    private DictionaryService dictionaryService;
    private NamespaceService namespaceService;
    private NodeService nodeService;
    private PermissionService permissionService;
    private ContentService contentService;
    private ThumbnailService thumbnailService;
    private ScriptThumbnailService scriptThumbnailService;
    private NodeDAO nodeDao;
    private AclDAO aclDao;
    private String contentUrlPrefix;
    private String shareUrlPrefix;
    private String previewUrlPrefix;
    private String thumbnailUrlPrefix;
    private Set<String> inclusionAclPermissions;
    private SysAdminParams sysAdminParams;
    private ResourceLoader resourceLoader;

    @Override
    public void afterPropertiesSet() throws Exception
    {
        boolean alfrescoWithPort = true;
        if ((sysAdminParams.getAlfrescoProtocol() == "https" && sysAdminParams.getAlfrescoPort() == 443)
                || (sysAdminParams.getAlfrescoProtocol() == "http" && sysAdminParams.getAlfrescoPort() == 80))
        {
            alfrescoWithPort = false;
        }
        String alfrescoPrefix = sysAdminParams.getAlfrescoProtocol() + "://" + sysAdminParams.getAlfrescoHost()
                + (alfrescoWithPort ? ":" + sysAdminParams.getAlfrescoPort() : "") + "/"
                + sysAdminParams.getAlfrescoContext();
//        contentUrlPrefix = alfrescoPrefix + "/service";
//        previewUrlPrefix = alfrescoPrefix + "/service";
//        thumbnailUrlPrefix = alfrescoPrefix + "/service";

        boolean shareWithPort = true;
        if ((sysAdminParams.getShareProtocol() == "https" && sysAdminParams.getSharePort() == 443)
                || (sysAdminParams.getShareProtocol() == "http" && sysAdminParams.getSharePort() == 80))
        {
            shareWithPort = false;
        }
        shareUrlPrefix = sysAdminParams.getShareProtocol() + "://" + sysAdminParams.getShareHost()
                + (shareWithPort ? ":" + sysAdminParams.getSharePort() : "") + "/" + sysAdminParams.getShareContext();

        contentUrlPrefix = shareUrlPrefix + "/proxy/alfresco";
        previewUrlPrefix = shareUrlPrefix + "/proxy/alfresco";
        thumbnailUrlPrefix = shareUrlPrefix + "/proxy/alfresco";

    }

    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }

    public void setNamespaceService(NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setPermissionService(PermissionService permissionService)
    {
        this.permissionService = permissionService;
    }

    public void setNodeDao(NodeDAO nodeDao)
    {
        this.nodeDao = nodeDao;
    }

    public void setAclDao(AclDAO aclDao)
    {
        this.aclDao = aclDao;
    }

    public void setContentUrlPrefix(String contentUrlPrefix)
    {
        this.contentUrlPrefix = contentUrlPrefix;
    }

    public void setShareUrlPrefix(String shareUrlPrefix)
    {
        this.shareUrlPrefix = shareUrlPrefix;
    }

    public void setPreviewUrlPrefix(String previewUrlPrefix)
    {
        this.previewUrlPrefix = previewUrlPrefix;
    }

    public void setThumbnailUrlPrefix(String thumbnailUrlPrefix)
    {
        this.thumbnailUrlPrefix = thumbnailUrlPrefix;
    }

    public void setInclusionAclPermissions(Set<String> inclusionPermissions)
    {
        this.inclusionAclPermissions = inclusionPermissions;
    }

    public void setSysAdminParams(SysAdminParams sysAdminParams)
    {
        this.sysAdminParams = sysAdminParams;
    }

    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }

    public void setThumbnailService(ThumbnailService thumbnailService)
    {
        this.thumbnailService = thumbnailService;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader)
    {
        this.resourceLoader = resourceLoader;
    }

    public void setScriptThumbnailService(ScriptThumbnailService scriptThumbnailService)
    {
        this.scriptThumbnailService = scriptThumbnailService;
    }

}