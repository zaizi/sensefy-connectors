#!/bin/bash
WORKSPACE_JAR=jars
WORKSPACE_POM=poms
MANIFOLD_VERSION=1.8-SNAPSHOT

mvn install:install-file \
  -DgroupId=org.apache.manifoldcf \
  -DartifactId=mcf-core \
  -Dpackaging=jar \
  -Dversion=${MANIFOLD_VERSION} \
  -Dfile=${WORKSPACE_JAR}/mcf-core.jar

mvn install:install-file \
  -DgroupId=org.apache.manifoldcf \
  -DartifactId=mcf-agents \
  -Dpackaging=jar \
  -Dversion=${MANIFOLD_VERSION} \
  -Dfile=${WORKSPACE_JAR}/mcf-agents.jar

mvn install:install-file \
  -DgroupId=org.apache.manifoldcf \
  -DartifactId=mcf-pull-agent \
  -Dpackaging=jar \
  -Dversion=${MANIFOLD_VERSION} \
  -Dfile=${WORKSPACE_JAR}/mcf-pull-agent.jar

mvn install:install-file \
  -DgroupId=org.apache.manifoldcf \
  -DartifactId=mcf-ui-core \
  -Dpackaging=jar \
  -Dversion=${MANIFOLD_VERSION} \
  -Dfile=${WORKSPACE_JAR}/mcf-ui-core.jar

mvn install:install-file \
  -DgroupId=org.apache.manifoldcf \
  -DartifactId=mcf-solr-connector \
  -Dpackaging=jar \
  -Dversion=${MANIFOLD_VERSION} \
  -Dfile=${WORKSPACE_JAR}/mcf-solr-connector.jar

mvn install:install-file \
  -Dfile=poms/mcf-connectors.pom \
  -DpomFile=poms/mcf-connectors.pom 
