#!/bin/bash
WORKSPACE=libs
MANIFOLD_VERSION=1.8-SNAPSHOT

mvn install:install-file \
  -DgroupId=org.apache.manifoldcf \
  -DartifactId=mcf-core \
  -Dpackaging=jar \
  -Dversion=${MANIFOLD_VERSION} \
  -Dfile=${WORKSPACE}/mcf-core.jar

mvn install:install-file \
  -DgroupId=org.apache.manifoldcf \
  -DartifactId=mcf-agents \
  -Dpackaging=jar \
  -Dversion=${MANIFOLD_VERSION} \
  -Dfile=${WORKSPACE}/mcf-agents.jar

mvn install:install-file \
  -DgroupId=org.apache.manifoldcf \
  -DartifactId=mcf-pull-agent \
  -Dpackaging=jar \
  -Dversion=${MANIFOLD_VERSION} \
  -Dfile=${WORKSPACE}/mcf-pull-agent.jar

mvn install:install-file \
  -DgroupId=org.apache.manifoldcf \
  -DartifactId=mcf-ui-core \
  -Dpackaging=jar \
  -Dversion=${MANIFOLD_VERSION} \
  -Dfile=${WORKSPACE}/mcf-ui-core.jar

mvn install:install-file \
  -DgroupId=org.apache.manifoldcf \
  -DartifactId=mcf-solr-connector \
  -Dpackaging=jar \
  -Dversion=${MANIFOLD_VERSION} \
  -Dfile=${WORKSPACE}/mcf-solr-connector.jar

