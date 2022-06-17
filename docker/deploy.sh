#!/usr/bin/env bash

# mvn -B clean deploy -PcreateSourceZip,downloadNecessaryArtifacts | tee /tmp/build.log
mvn -B clean deploy | tee /tmp/deploy.log