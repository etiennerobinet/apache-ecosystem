#!/bin/bash
cd /
chmod +x ./opt/incubator-iotdb/server/target/iotdb-server-0.10.0-SNAPSHOT/sbin/start-server.sh
./opt/incubator-iotdb/server/target/iotdb-server-0.10.0-SNAPSHOT/sbin/start-server.sh &
sleep 10
cd /opt/incubator-iotdb/grafana/target/
java -jar iotdb-grafana-0.10.0-SNAPSHOT.war &
sleep 10
cd /opt/grafana-6.7.1/bin
./grafana-server &
sleep 10
/bin/bash