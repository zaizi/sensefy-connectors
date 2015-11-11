/**
* (C) Copyright 2015 Zaizi Limited (http://www.zaizi.com). 
* 
* All rights reserved.  This program and the accompanying materials
* are made available under the terms of the GNU Lesser General Public License
* (LGPL) version 3.0 which accompanies this distribution, and is available at
* http://www.gnu.org/licenses/lgpl-3.0.en.html
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
*/

# Sensefy ManifoldCF Connectors
---

Sensefy Connectors includes the following connectors

```
1. authority/alfresco/alfresco-authority-connector
2. output/mcf-solrwrapperconnector
3. transformation/mcf-stanbol-connector
```

In addition to connectors, this includes **authority/alfresco/alfresco-manifold** AMP file that needs to be installed in alfresco. 


## Building Connectors
---

Build the connectors and the AMP file by running,

```
mvn clean install -DskipTests=true
```

### Build Alfresco Indexer and Indexer AMP

```
git clone https://github.com/zaizi/alfresco-webscript-manifold-connector.git 
cd alfresco-webscript-manifold-connector/
mvn clean install -DskipTests=true
```

This builds the followings

```
1. alfresco-indexer-webscripts - AMP file to be installed in Alfresco
2. alfresco-indexer-client - to be configure with ManifoldCF
3. manifold-connector - to be configure with ManifoldCF
```

## Configuring Alfresco with AMP files
---

```
1. Copy alfresco-indexer-webscripts.amp in alfresco-webscript-manifold-connector/alfresco-indexer-webscripts/target/ to your $ALFRESCO_INSTALLATION_DIR/amps
2. Copy alfresco-manifold.amp in sensefy-connectors/authority/alfresco/alfresco-manifold/target/ to your $ALFRESCO_INSTALLATION_DIR/amps
3. To apply amp files to Alfresco, run following from $ALFRESCO_INSTALL_DIR
```

```
./bin/apply_amps.sh -force
```

## Adding Connectors to ManifoldCF

Add following connectors to **$MANIFOLD_INSTALL_DIR/connectors-lib**

```
1. sensefy-connectors/authority/alfresco/alfresco-authority-connector/target/alfresco-authority-connector-1.0.jar
2. sensefy-connectors/output/mcf-solrwrapperconnector/target/mcf-solrwrapperprocessorconnector-connector-1.8-SNAPSHOT.jar
3. sensefy-connectors/transformation/mcf-stanbol-connector/target/mcf-stanbol-connector-1.8-SNAPSHOT-jar-with-dependencies.jar
4. alfresco-webscript-manifold-connector/manifold-connector/target/manifold-connector-0.6.1-jar-with-dependencies.jar
```

Add following properties to **$MANIFOLD_INSTALL_DIR/connectors.xml**

```
<authorityconnector name="Alfresco" class="org.apache.manifoldcf.authorities.authorities.alfresco.AlfrescoAuthorityConnector"/>
<repositoryconnector name="Alfresco" class="org.alfresco.consulting.manifold.AlfrescoConnector"/>
<transformationconnector name="Stanbol enhancer" class="org.apache.manifoldcf.agents.transformation.stanbol.StanbolEnhancer"/>
<outputconnector name="Solr Wrapper" class="org.apache.manifoldcf.agents.output.solrwrapper.SolrWrapperConnector"/>

```

