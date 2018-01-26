#!/bin/bash
#
# This script:
# * Injects the current build information (version, commit ID, etc) into the src/main/java/resources directory.
# * Does a clean build and deploy (i.e. compiles, packages, and uploads to Nexus)
#

# output environmental information
print-build-environment.sh

# deploy the artifact with updated build.properties file
set-maven-build-information.sh                           || exit 1 
mvn --batch-mode clean deploy                            || exit 1
