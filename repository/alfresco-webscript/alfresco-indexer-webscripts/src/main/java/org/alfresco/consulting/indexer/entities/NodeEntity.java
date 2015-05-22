package org.alfresco.consulting.indexer.entities;

import org.alfresco.consulting.indexer.entities.NodeEntity;

public class NodeEntity extends org.alfresco.repo.domain.node.NodeEntity {
  private String typeName;
  private String typeNamespace;
  private Long aclChangesetId;
  private Long transactionId;
  private String name;
  

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((super.getUuid() == null) ? 0 : super.getUuid().hashCode());
    result = prime * result + ((super.getVersion() == null) ? 0 : super.getVersion().hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj == null) return false;
    if (!(obj instanceof NodeEntity)) return false;
    NodeEntity that = (NodeEntity) obj;
    return this.getUuid().equals(that.getUuid()) && this.getVersion().equals(that.getVersion());
  }

  public String getTypeName() {
    return typeName;
  }

  public void setTypeName(String typeName) {
    this.typeName = typeName;
  }

  public String getTypeNamespace() {
    return typeNamespace;
  }

  public void setTypeNamespace(String typeNamespace) {
    this.typeNamespace = typeNamespace;
  }

  public Long getAclChangesetId() {
    return aclChangesetId;
  }

  public void setAclChangesetId(Long aclChangesetId) {
    this.aclChangesetId = aclChangesetId;
  }

  public Long getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(Long transactionId) {
    this.transactionId = transactionId;
  }
  
  public void setName(String name){
      this.name=name;
  }
  
  public String getName(){
      return this.name;
  }
}
