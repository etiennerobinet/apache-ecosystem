<img src="https://dbdb.io/media/logos/iotdb.png" style="zoom:33%;" />



# IoTDB 

This module shows the use of IoTDB, a real-time database, in combination with camel-routing



## Installation

As the library did not implemented a Camel component nor was ready to be deployed in a OSGi component, it had to be modified. Until acceptance of pull-request on the main branch, please pull from this [repository](https://github.com/etiennerobinet/incubator-iotdb)

1. Clone the project from git and build with maven
   - `git clone https://github.com/etiennerobinet/incubator-iotdb`
   - `cd /incubator-iotdb`
   - `mvn clean install -DskipTests`
   
2. Inside your karaf container, install the  JDBC feature using following commands

   - `feature:repo-add mvn:org.apache.iotdb/jdbc-features/{version}/xml/features`
   - `feature:install iotdb-jdbc-feature`

3. To use IoTDB with camel routing, we will use the Camel component

   - `feature:repo-add mvn:org.apache.iotdb/camel-features-iotdb/{version}/xml/features`
   - `feature:install camel-iotdb`

4. Now you can create your karaf-blueprint project inside your IDE

5. The JDBC can not be used yet inside routes as Committ/Prepared statement are not yet implemented so no need to add dependencies

   - Here is a route example combining PLC4X

   

```xml
<blueprint xmlns="http://www.osgi.org/xmlns/blueprint/v1.0.0"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation= "http://www.osgi.org/xmlns/blueprint/v1.0.0 https://www.osgi.org/xmlns/blueprint/v1.0.0/blueprint.xsd
            http://camel.apache.org/schema/blueprint http://camel.apache.org/schema/blueprint/camel-blueprint-2.24.2.xsd">


    <camelContext id="IoTDB-Context" xmlns="http://camel.apache.org/schema/blueprint" streamCache="true" >
        <route id="Temp-Pressure">
            <from uri="timer://foo?fixedRate=true&amp;period=500" />
            <pollEnrich>
                <simple>plc4x:s7:tcp://192.168.178.10?address=#fields</simple>
            </pollEnrich>
            <toD uri="iotdb://127.0.0.1?sql=INSERT into root.ge.s7(timestamp,pressure,temperature) values(now(),${body.get(0)},${body.get(1)})"/>
        </route>
    </camelContext>

   <bean id="fields" class="java.util.ArrayList">
     <argument>
         <list>
             <value type="java.lang.String">%DB1.DBD0:REAL</value> <!-- 0 -->
             <value type="java.lang.String">%DB1.DBW4:INT</value> <!-- 1 -->
         </list>
     </argument>
   </bean>



</blueprint>
```

- Remember to configure your project/karaf container according to the plc4x



## JDBC

- Install the `pax-jdbc-iotdb` bundle and the `jdbc` feature to be able to create a Datasource in karaf
  - Until acceptance of PR, find the `pax-jdbc-iotdb` [here](https://github.com/etiennerobinet/org.ops4j.pax.jdbc/tree/iotdb/pax-jdbc-iotdb)
  - Build the project
  - `feature:install jdbc`
  - `bundle:install -s mvn:org.ops4j.pax.jdbc/pax-jdbc-iotdb/1.4.5-SNAPSHOT`
  - Start your IoTDB server `{IOTDB_DIRECTORY}\server\target\iotdb-server-{version}\sbin`
  - Create a DataSource for your server
`jdbc:ds-create -p root -u root -url 'jdbc:iotdb://127.0.0.1:6667' -dc org.apache.iotdb.jdbc.IoTDBDriver iot`
- You can use `jdbc:query` or `jdbc:execute` to interact with the database
- For more information and syntax, check the IoTDB [website](http://iotdb.apache.org/#/)
