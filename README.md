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

1. authority/alfresco/alfresco-authority-connector
2. output/mcf-solrwrapperconnector
3. transformation/mcf-stanbol-connector

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

## Configuring Alfresco with AMP files
---

## Deploying Connectors to ManifoldCF
---