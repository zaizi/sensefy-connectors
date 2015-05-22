package org.alfresco.consulting.indexer.webscripts;

import org.alfresco.consulting.indexer.dao.IndexingDaoImpl;
import org.alfresco.consulting.indexer.entities.NodeEntity;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.TemplateHashModel;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.domain.node.NodeDAO;
import org.alfresco.repo.domain.qname.QNameDAO;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.*;

import java.util.*;

/**
 * 
 * @author iarroyo
 *
 */
public class NodeActionsWebScript extends DeclarativeWebScript
{

    protected static final Logger logger = LoggerFactory.getLogger(NodeActionsWebScript.class);

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache)
    {
        logger.info( "[Node Actions] Fetching request params" );
        Map<String, String> templateArgs = req.getServiceMatch().getTemplateVars();
        String storeId = templateArgs.get("storeId");
        String storeProtocol = templateArgs.get("storeProtocol");
        String uuid= templateArgs.get("uuid");
        

        // Getting the Store ID on which the changes are requested
        Pair<Long, StoreRef> store = nodeDao.getStore(new StoreRef(storeProtocol, storeId));
        if (store == null)
        {
            logger.error( "[Node Actions] The store with Id {} and store protocol{} is null", storeId, storeProtocol );
            throw new IllegalArgumentException("Invalid store reference: " + storeProtocol + "://" + storeId);
        }

        Set<NodeEntity> nodes = new HashSet<NodeEntity>();
        
        NodeEntity node= indexingService.getNodeByUuid(store, uuid);
        if(node!=null){
            nodes.add(node); 
        }

        // Render them out
        Map<String, Object> model = new HashMap<String, Object>(1, 1.0f);
        model.put("qnameDao", qnameDao);
        model.put("nsResolver", namespaceService);
        model.put("nodes", nodes);
        model.put("storeId", storeId);
        model.put("storeProtocol", storeProtocol);
        model.put("propertiesUrlTemplate", propertiesUrlTemplate);

        // This allows to call the static method QName.createQName from the FTL template
        try
        {
            BeansWrapper wrapper = BeansWrapper.getDefaultInstance();
            TemplateHashModel staticModels = wrapper.getStaticModels();
            TemplateHashModel qnameStatics = (TemplateHashModel) staticModels.get("org.alfresco.service.namespace.QName");
            model.put("QName", qnameStatics);
        }
        catch (Exception e)
        {
            throw new AlfrescoRuntimeException(
                    "[Node Actions] Cannot add BeansWrapper for static QName.createQName method to be used from a Freemarker template",
                    e);
        }

        logger.debug("[Node Actions] Attaching {} nodes to the WebScript template", nodes.size());

        return model;
    }

    private NamespaceService namespaceService;
    private QNameDAO qnameDao;
    private IndexingDaoImpl indexingService;
    private NodeDAO nodeDao;

    private String propertiesUrlTemplate;

    public void setNamespaceService(NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }

    public void setQnameDao(QNameDAO qnameDao)
    {
        this.qnameDao = qnameDao;
    }

    public void setIndexingService(IndexingDaoImpl indexingService)
    {
        this.indexingService = indexingService;
    }

    public void setNodeDao(NodeDAO nodeDao)
    {
        this.nodeDao = nodeDao;
    }

    public void setPropertiesUrlTemplate(String propertiesUrlTemplate)
    {
        this.propertiesUrlTemplate = propertiesUrlTemplate;
    }

//    public void setMaxNodesPerAcl(int maxNodesPerAcl)
//    {
//        this.maxNodesPerAcl = maxNodesPerAcl;
//    }
//
//    public void setMaxNodesPerTxns(int maxNodesPerTxns)
//    {
//        this.maxNodesPerTxns = maxNodesPerTxns;
//    }
}