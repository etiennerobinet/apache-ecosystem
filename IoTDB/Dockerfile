FROM openjdk:8

# Install some stuff we need to run the build
RUN apt update -y
RUN apt-get update && apt-get install -y dos2unix
RUN apt install -y git
RUN apt install -y vim

RUN wget https://downloads.apache.org/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz
RUN tar -C /opt -xvf apache-maven-3.6.3-bin.tar.gz
RUN rm apache-maven-3.6.3-bin.tar.gz
ENV M2_HOME /opt/apache-maven-3.6.3
ENV M2 $M2_HOME/bin
ENV PATH $M2:$PATH

#IoTDB and Grafana
WORKDIR /opt
RUN git clone https://github.com/etiennerobinet/incubator-iotdb
WORKDIR /opt/incubator-iotdb
RUN mvn clean install -DskipTests -pl server,grafana -am
COPY application.properties /opt/incubator-iotdb/grafana/target/application.properties
WORKDIR /opt
RUN wget https://dl.grafana.com/oss/release/grafana-6.7.1.linux-amd64.tar.gz
RUN tar -zxvf grafana-6.7.1.linux-amd64.tar.gz
COPY simple-json-datasource /opt/grafana-6.7.1/data/plugins/simple-json-datasource
RUN rm grafana-6.7.1.linux-amd64.tar.gz
WORKDIR /
COPY iotdb.sh /scripts/iotdb.sh
RUN ["chmod", "+x", "/scripts/iotdb.sh"]
RUN dos2unix /scripts/iotdb.sh
WORKDIR /

VOLUME /opt/incubator-iotdb/server/target/iotdb-server-0.10.0-SNAPSHOT/data
VOLUME /opt/incubator-iotdb/server/target/iotdb-server-0.10.0-SNAPSHOT/logs
VOLUME /opt/grafana-6.7.1

EXPOSE 3000
EXPOSE 6667

ENTRYPOINT ["/scripts/iotdb.sh"]