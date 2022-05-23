FROM debian:bullseye

### debian packages
RUN apt-get update \
    && apt-get install --no-install-recommends -yy \
        git \
        wget \
        curl \
        ca-certificates \
        libgtk-3-0 \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Install maven
ENV MVN_VERSION 3.6.3
RUN --mount=type=secret,id=artifactory \
      curl -s -L "http://www-eu.apache.org/dist/maven/maven-3/${MVN_VERSION}/binaries/apache-maven-${MVN_VERSION}-bin.tar.gz" --output - | \
      tar -xz -C /usr/local \
    && mkdir /lc3-env \
    && mkdir /src \
    && mkdir /JDK \
    && curl -u $(cat /run/secrets/artifactory) -SsL https://artifactory.logicals.com/artifactory/safe/lc3env/jdk/17.0.0/hotspot-17.0.0-linux-gtk-x86_64.tar.gz --output - | \
       tar -xz -C /JDK

COPY . /src/

# Configure lc3 environment (logi-cloud-server-env)
ENV PATH="/JDK/hotspot-17.0.0-linux-gtk-x86_64/bin:/usr/local/apache-maven-${MVN_VERSION}/bin:${PATH}"
ENV LC3_CONFIG_DIR=/lc3-env/config/
ENV LC3_HOME=/lc3-env/
ENV JAVA_HOME="/JDK/hotspot-17.0.0-linux-gtk-x86_64"
ENV MAVEN_OPTS --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED \
    --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.util.regex=ALL-UNNAMED \
    -Xmx8192m -Xms384m -XX:ReservedCodeCacheSize=512M -Xmx8192m -XX:ReservedCodeCacheSize=128m

COPY docker/checkstyle8.xml /lc3-env/config/checkstyle/checkstyle8.xml
COPY docker/checkstyle-stylesheet.xsl /lc3-env/config/checkstyle/checkstyle-stylesheet.xsl
COPY docker/pmd6-photon.xml /lc3-env/config/pmd/pmd6-photon.xml
COPY docker/pmd-stylesheet.xsl /lc3-env/config/pmd/pmd-stylesheet.xsl
COPY docker/build.sh /build.sh
COPY docker/deploy.sh /deploy.sh
COPY docker/settings.xml /root/.m2/settings.xml
COPY docker/toolchains.xml /root/.m2/toolchains.xml

ENTRYPOINT ["/build.sh"]

WORKDIR /src
