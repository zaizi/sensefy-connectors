# Sensefy ManifoldCF Connectors
---

Sensefy Connectors includes the following connectors

```
1. output/mcf-solrwrapperconnector
2. transformation/mcf-stanbol-connector
3. authority/alfresco/alfresco-authority-connector (do not need to configure this with manifoldcf-2.2)
```


## Building Connectors
---

In order to build the connectors, first need to download and build manifoldcf to intall the required dependancies.

```
git clone https://github.com/apache/manifoldcf.git
cd manifoldcf/
git checkout release-2.3-branch
mvn clean install 
```

Also need to build Zaizi Stanbol Clent

```
git clone https://github.com/zaizi/apache-stanbol-client.git
cd apache-stanbol-client
git checkout jaxrs-1.0
mvn clean install -DskipTests=true
```

Build the connectors by running following command from sensefy-connectors directory,

```
mvn clean install -DskipTests=true
```

### Build Alfresco Indexer AMP

```
git clone https://github.com/zaizi/alfresco-indexer
cd alfresco-indexer/
mvn clean install -DskipTests=true
```

This builds the followings

```
1. alfresco-indexer-webscripts - AMP file to be installed in Alfresco
2. alfresco-indexer-client - to be configure with ManifoldCF
```

## Configuring Alfresco with AMP files
---

```
1. Copy alfresco-indexer-webscripts.amp in alfresco-indexer/alfresco-indexer-webscripts/target/ to your $ALFRESCO_INSTALLATION_DIR/amps

2. To apply amp files to Alfresco, run following from $ALFRESCO_INSTALL_DIR
```

```
./bin/apply_amps.sh -force
```

## Adding Connectors to ManifoldCF

Add following connectors to **$MANIFOLD_INSTALL_DIR/connectors-lib**

```
1. sensefy-connectors/output/mcf-solrwrapperconnector/target/mcf-solrwrapperprocessorconnector-connector-2.3.jar
2. sensefy-connectors/transformation/mcf-stanbol-connector/target/mcf-stanbol-connector-2.3-jar-with-dependencies.jar
```

Add following properties to **$MANIFOLD_INSTALL_DIR/connectors.xml**

```
<transformationconnector name="Stanbol enhancer" class="org.zaizi.manifoldcf.agents.transformation.stanbol.StanbolEnhancer"/>
<outputconnector name="Solr Wrapper" class="org.zaizi.manifoldcf.agents.output.solrwrapper.SolrWrapperConnector"/>

```

#### Copyright


© Zaizi Limited. Code for this plugin is licensed under the GNU Lesser General Public License (LGPL).

Any trademarks and logos included in these plugins are property of their respective owners and should not be reused, redistributed, modified, repurposed, or otherwise altered or used outside of this plugin.

