#!/bin/sh
# Maven Wrapper script - downloads Maven if not present
# Simplified version; full version at https://github.com/apache/maven-wrapper

set -e

MAVEN_WRAPPER_JAR=".mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES=".mvn/wrapper/maven-wrapper.properties"

if [ -f "$MAVEN_WRAPPER_PROPERTIES" ]; then
    DIST_URL=$(grep 'distributionUrl' "$MAVEN_WRAPPER_PROPERTIES" | cut -d'=' -f2-)
fi

MAVEN_HOME="${HOME}/.m2/wrapper/dists/apache-maven-3.9.6"

if [ ! -f "${MAVEN_HOME}/bin/mvn" ]; then
    echo "Downloading Maven..."
    mkdir -p "${MAVEN_HOME}"
    curl -sL "${DIST_URL}" -o /tmp/maven.zip
    unzip -qo /tmp/maven.zip -d "${MAVEN_HOME}/.."
    rm /tmp/maven.zip
fi

exec "${MAVEN_HOME}/bin/mvn" "$@"
