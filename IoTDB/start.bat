docker build -t iotdb .
docker run -it -p 3000:3000 -p 6667:6667 -v C:/IoTDB/data:/opt/incubator-iotdb/server/target/iotdb-server-0.10.0-SNAPSHOT/data -v C:/IoTDB/logs:/opt/incubator-iotdb/server/target/iotdb-server-0.10.0-SNAPSHOT/logs iotdb
