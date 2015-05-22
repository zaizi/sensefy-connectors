package org.alfresco.consulting.indexer.dao;

import org.alfresco.consulting.indexer.entities.NodeBatchLoadEntity;
import org.alfresco.consulting.indexer.entities.NodeEntity;
import org.alfresco.consulting.indexer.utils.Utils;
import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.RowBounds;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IndexingDaoImpl
{

    private static final String SELECT_NODES_BY_ACLS = "alfresco.index.select_NodeIndexesByAclChangesetId";
    private static final String SELECT_NODES_BY_TXNS = "alfresco.index.select_NodeIndexesByTransactionId";
    private static final String SELECT_NODES_BY_UUID = "alfresco.index.select_NodeIndexesByUuid";
    private static final String SELECT_LAST_TRANSACTION_ID = "select_LastTransactionID";
    private static final String SELECT_LAST_ACL_CHANGE_SET_ID = "select_LastAclChangeSetID";
    
    private static final String SITES_FILTER="SITES";
    private static final String PROPERTIES_FILTER="PROPERTIES";
    private static final Logger logger = LoggerFactory.getLogger(IndexingDaoImpl.class);

    private SqlSessionTemplate template;
    private NodeService nodeService;
    
    private Set<String> allowedTypes;
    private Set<String> excludedNameExtension;
    private Set<String> properties;
    private Set<String> aspects;
    private Set<String> mimeTypes;
    private Set<String> sites;

    public List<NodeEntity> getNodesByAclChangesetId(Pair<Long, StoreRef> store, Long lastAclChangesetId, int maxResults)
    {
        StoreRef storeRef = store.getSecond();
        if (maxResults <= 0 || maxResults == Integer.MAX_VALUE)
        {
            throw new IllegalArgumentException("Maximum results can not be negative or too big ( Maximum integer value)");
        }
        String storeString = storeRef.getProtocol() + "://" + storeRef.getIdentifier();
        logger.debug( "[Fetching] Fetching nodes by Acl Changeset Id {} On Store {} ", lastAclChangesetId,storeString);
        NodeBatchLoadEntity nodeLoadEntity = new NodeBatchLoadEntity();
        nodeLoadEntity.setStoreId(store.getFirst());
        nodeLoadEntity.setStoreProtocol(storeRef.getProtocol());
        nodeLoadEntity.setStoreIdentifier(storeRef.getIdentifier());
        nodeLoadEntity.setMinId(lastAclChangesetId);
        nodeLoadEntity.setMaxId(lastAclChangesetId + maxResults);
        nodeLoadEntity.setAllowedTypes(this.allowedTypes);
        nodeLoadEntity.setExcludedNameExtension(this.excludedNameExtension);
//        nodeLoadEntity.setProperties(this.properties);
        nodeLoadEntity.setAspects(this.aspects);
        nodeLoadEntity.setMimeTypes(this.mimeTypes);

        return filterNodes((List<NodeEntity>) template.selectList(SELECT_NODES_BY_ACLS, nodeLoadEntity, new RowBounds(0,
                Integer.MAX_VALUE)));
    }

    public List<NodeEntity> getNodesByTransactionId(Pair<Long, StoreRef> store, Long lastTransactionId, int maxResults)
    {
        StoreRef storeRef = store.getSecond();
        if (maxResults <= 0 || maxResults == Integer.MAX_VALUE)
        {
            throw new IllegalArgumentException("Maximum results can not be negative or too big ( Maximum integer value)");
        }
        String storeString = storeRef.getProtocol() + "://" + storeRef.getIdentifier();
        logger.debug( "[Fetching] Fetching nodes by Transaction Id {} On Store {} ", lastTransactionId,storeString);

        NodeBatchLoadEntity nodeLoadEntity = new NodeBatchLoadEntity();
        nodeLoadEntity.setStoreId(store.getFirst());
        nodeLoadEntity.setStoreProtocol(storeRef.getProtocol());
        nodeLoadEntity.setStoreIdentifier(storeRef.getIdentifier());
        nodeLoadEntity.setMinId(lastTransactionId);
        nodeLoadEntity.setMaxId(lastTransactionId + maxResults);
        nodeLoadEntity.setAllowedTypes(this.allowedTypes);
        nodeLoadEntity.setExcludedNameExtension(this.excludedNameExtension);
//        nodeLoadEntity.setProperties(this.properties);
        nodeLoadEntity.setAspects(this.aspects);
        nodeLoadEntity.setMimeTypes(this.mimeTypes);

        return filterNodes((List<NodeEntity>) template.selectList(SELECT_NODES_BY_TXNS, nodeLoadEntity, new RowBounds(0,
                Integer.MAX_VALUE)));
    }

    public NodeEntity getNodeByUuid(Pair<Long, StoreRef> store, String uuid)
    {
        StoreRef storeRef = store.getSecond();
        String storeString = storeRef.getProtocol() + "://" + storeRef.getIdentifier();
        logger.debug( "[Fetching] Fetching node by Node Id {} On Store {} ", uuid,storeString);

        NodeBatchLoadEntity nodeLoadEntity = new NodeBatchLoadEntity();
        nodeLoadEntity.setStoreId(store.getFirst());
        nodeLoadEntity.setStoreProtocol(storeRef.getProtocol());
        nodeLoadEntity.setStoreIdentifier(storeRef.getIdentifier());
        nodeLoadEntity.setUuid(uuid);

        return (NodeEntity) template.selectOne(SELECT_NODES_BY_UUID, nodeLoadEntity);
    }
    
    /**
     * Get the last acl change set id from database
     * 
     * @return
     */
    public Long getLastAclChangeSetID(){
        return (Long) template.selectOne(SELECT_LAST_ACL_CHANGE_SET_ID);
    }
    
    /**
     * Get the last transaction id from database
     * 
     * @return
     */
    public Long getLastTransactionID(){
        return (Long) template.selectOne(SELECT_LAST_TRANSACTION_ID);
    }
    
    /**
     * Filter the nodes based on some parameters
     * @param nodes
     * @return
     */
    private List<NodeEntity> filterNodes(List<NodeEntity> nodes)
    {
        List<NodeEntity> filteredNodes= null;
        
        //Filter by sites
        Map<String,Boolean> filters=getFilters();
        
        if(filters.values().contains(Boolean.TRUE)){
            
            filteredNodes= new ArrayList<NodeEntity>();
            
            for(NodeEntity node:nodes){
                
               boolean shouldBeAdded=true;
               NodeRef nodeRef= new NodeRef(node.getStore().getStoreRef(),node.getUuid());
               String nodeType="{"+node.getTypeNamespace()+"}"+node.getTypeName();  
               
               if(nodeService.exists(nodeRef)){
                    
                    //Filter by site
                    if(filters.get(SITES_FILTER)){
                        Path pathObj = nodeService.getPath(nodeRef);
                        String siteName = Utils.getSiteName(pathObj);
                        shouldBeAdded= siteName!=null && this.sites.contains(siteName);
                    }
                    
                    //Filter by properties
                    if(filters.get(PROPERTIES_FILTER) && shouldBeAdded){
                        for(String prop:this.properties){
                            
                            int pos=prop.lastIndexOf(":");
                            String qName=null;
                            String value=null;
                            
                            if(pos!=-1 && (prop.length()-1)>pos){
                                qName=prop.substring(0, pos);
                                value= prop.substring(pos+1,prop.length());
                            }
                            
                            if(StringUtils.isEmpty(qName) || StringUtils.isEmpty(value)){
                                //Invalid property
                                continue;
                            }
                            
                            Serializable rawValue= nodeService.getProperty(nodeRef, QName.createQName(qName));
                            shouldBeAdded=shouldBeAdded && value.equals(rawValue);
                            
                        }
                    }
                }else if(nodeType.equals(ContentModel.TYPE_DELETED)){
                    shouldBeAdded=Boolean.TRUE;
                }
               
                if(shouldBeAdded){
                    filteredNodes.add(node);
                }
            }
        }else{
            filteredNodes=nodes;
        }
        return filteredNodes;
    }

    /**
     * Get existing filters
     * @return
     */
    private Map<String,Boolean> getFilters()
    {
        Map<String,Boolean> filters= new HashMap<String, Boolean>(2);
        //Site filter
        filters.put(SITES_FILTER, this.sites!=null && this.sites.size() > 0);
        //Properties filter
        filters.put(PROPERTIES_FILTER, this.properties!=null && this.properties.size() > 0);
        
        return filters;
    }

    public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate)
    {
        this.template = sqlSessionTemplate;
    }
    
    public void setServiceRegistry(ServiceRegistry serviceRegistry){
        this.nodeService= serviceRegistry.getNodeService();
    }

    public void setAllowedTypes(Set<String> allowedTypes)
    {
        this.allowedTypes = allowedTypes;
        //Add mandatory type to retrieve deleted documents (workaround)
        this.allowedTypes.add(ContentModel.TYPE_DELETED.toString());
    }

    public Set<String> getAllowedTypes()
    {
        return this.allowedTypes;
    }

    public void setExcludedNameExtension(Set<String> excludedNameExtension)
    {
        this.excludedNameExtension = excludedNameExtension;
    }

    public Set<String> getExcludedNameExtension()
    {
        return this.excludedNameExtension;
    }

    public void setProperties(Set<String> properties)
    {
        this.properties = properties;
    }

    public Set<String> getProperties()
    {
        return this.properties;
    }

    public void setAspects(Set<String> aspects)
    {
        this.aspects = aspects;
    }

    public Set<String> getAspects()
    {

        return this.aspects;
    }

    public void setMimeTypes(Set<String> mimeTypes)
    {
        this.mimeTypes = mimeTypes;
    }

    public Set<String> getMimeTypes()
    {
        return this.mimeTypes;
    }
    
    public void setSites(Set<String> sites)
    {
        this.sites = sites;
    }

    public Set<String> getSites()
    {
        return this.sites;
    }
    
}
