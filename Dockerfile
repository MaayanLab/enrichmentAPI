FROM ubuntu AS builder

RUN set -x \
  && apt-get update -y \
  && apt-get install -y unzip curl openjdk-11-jdk

RUN \
  cd /usr/local && \
  curl -L https://services.gradle.org/distributions/gradle-7.0-bin.zip -o gradle-7.0-bin.zip && \
  unzip gradle-7.0-bin.zip && \
  rm gradle-7.0-bin.zip

ENV GRADLE_HOME=/usr/local/gradle-7.0
ENV PATH=$PATH:$GRADLE_HOME/bin

WORKDIR /work

ADD . .
RUN gradle war

FROM tomcat:9

COPY --from=builder /work/docker/enrichmentapi.war /enrichmentapi.war

ENV JAVA_OPTS="-Xmx16G -XX:MetaspaceSize=13G -XX:MaxMetaspaceSize=13G -XX:+UseCompressedOops --enable-native-access=ALL-UNNAMED"
ENV PREFIX="/enrichmentapi"
EXPOSE 8080

COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT [ "/entrypoint.sh" ]
CMD ["catalina.sh", "run"]
