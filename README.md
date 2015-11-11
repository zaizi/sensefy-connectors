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

In order to build the connectors, first you need to build ManifoldCF as the connectors have dependencies from MCF project

```
svn co https://svn.apache.org/repos/asf/manifoldcf/tags/release-1.8.2/ manifoldcf
cd manifoldcf
mvn clean install -DskipTests=true
```

Once ManifoldCF dependancies are installed, you can build the connectors and the AMP file by running,

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

1. Copy **alfresco-indexer-webscripts.amp** in *alfresco-webscript-manifold-connector/alfresco-indexer-webscripts/target/* to your *$ALFRESCO_INSTALLATION_DIR/amps*
2. Copy **alfresco-manifold.amp** in *sensefy-connectors/authority/alfresco/alfresco-manifold/target/* to your *$ALFRESCO_INSTALLATION_DIR/amps*
3. To apply amp files to Alfresco, run following from $ALFRESCO_INSTALL_DIR

```
./bin/apply_amps.sh -force
```

## Deploying Connectors to ManifoldCF
---