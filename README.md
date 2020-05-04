<img src="https://upload.wikimedia.org/wikipedia/commons/thumb/d/db/Apache_Software_Foundation_Logo_%282016%29.svg/1024px-Apache_Software_Foundation_Logo_%282016%29.svg.png" width="500" />

# Apache Ecosystem Project

The goal of this project is to create a well-integrated Ecosystem using open-source software from the Apache Foundation.
This system will allow use to acquire data from industrial equipment like PLCs with high-level API to integrate multiple types of PLCs and to route this data to a real-time database. This database will then be visualized using an Apache software or a WebApp.

Currently planning to integrate:

- [PLC4X](https://github.com/apache/plc4x) <img src="https://plc4x.apache.org/images/apache_plc4x_logo.png" width="120" /> 
- [Camel](https://camel.apache.org/) <img src="https://www.nicolaferraro.me/images/post-logo-apache-camel.png"  width="120"  />
- [Karaf](https://karaf.apache.org/) <img src="https://upload.wikimedia.org/wikipedia/en/thumb/f/f7/Apache_Karaf_Logo.svg/500px-Apache_Karaf_Logo.svg.png"  width="120"  />
- [IoTDB](https://iotdb.apache.org/)  <img src="https://www.apache.org/logos/res/iotdb/default.png"  width="120"  />  
- [Grafana](https://grafana.com/) <img src="https://stitch-microverse.s3.amazonaws.com/uploads/domains/grafana-logo.png"  width="120" />
- [ActiveMQ](https://activemq.apache.org/) <img src="https://activemq.apache.org/assets/img/activemq_logo_white_vertical.png"  width="120"  /> 



## Current state 

- Using the PLC4X-Camel component in Karaf to read/write to PLC using:
  -EIP Protocol (Allen-Bradley)
  -S7 (Siemens)
- Installation via Karaf features
- Using the SQL-Camel component to insert Timeseries inside IoTDB

## To come
- Dockerfile to create a Docker Image with IoTDB-Server, Grafana-Server and Grafana Connector for IoTDB
- Implementing other Drivers
