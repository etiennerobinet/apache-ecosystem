<img src="https://upload.wikimedia.org/wikipedia/commons/thumb/d/db/Apache_Software_Foundation_Logo_%282016%29.svg/1024px-Apache_Software_Foundation_Logo_%282016%29.svg.png" width="500" />

# Apache Ecosystem Project

The goal of this project is to create a well-integrated Ecosystem using open-source software from the Apache Foundation.
This system will allow use to acquire data from industrial equipment like PLCs with high-level API to integrate multiple types of PLCs and to route this data to a real-time database. This database will then be visualized using an Apache software or a WebApp.

Currently planning to integrate:

- [PLC4X](https://github.com/apache/plc4x) <img src="https://plc4x.apache.org/images/apache_plc4x_logo.png" width="120" /> 
- [Camel](https://camel.apache.org/) <img src="https://www.nicolaferraro.me/images/post-logo-apache-camel.png"  width="120"  />
- [Karaf](https://karaf.apache.org/) <img src="https://upload.wikimedia.org/wikipedia/en/thumb/f/f7/Apache_Karaf_Logo.svg/500px-Apache_Karaf_Logo.svg.png"  width="120"  />
- [IoTDB](https://iotdb.apache.org/) <img src="https://www.apache.org/logos/res/iotdb/default.png"  width="120"  /> 
- [Grafana](https://grafana.com/) <img src="https://stitch-microverse.s3.amazonaws.com/uploads/domains/grafana-logo.png"  width="120" />
- [ActiveMQ](https://activemq.apache.org/) <img src="https://activemq.apache.org/assets/img/activemq_logo_white_vertical.png"  width="120"  /> 



## PLC4X - Camel-blueprint integration in Karaf

For now, only tests with the S7 driver have been made

### Requirements

- Java 8
- Git
- Karaf 4.2.8
- Maven 3.6.x
- IDE with maven project

### Installation

1. Download [PLC4X](https://plc4x.apache.org/) sources (you can also clone from [git](https://github.com/apache/plc4x)) 
2. Go to `plc4j\integrations\apache-camel\src\main\java\org\apache\plc4x\camel` and find the `PLC4XConsumer.java` source file
   - as of February 2020, Camel is not fully integrated in PLC4X and you need to changethe `doStart()` method to this:

```java
@Override
    protected void doStart() throws InterruptedException, ExecutionException {
        future = executorService.schedule(() -> {
            plcConnection.readRequestBuilder()
                .addItem("default", fieldQuery)
                .build()
                .execute()
                .thenAccept(response -> {
                    LOGGER.debug("Received {}", response);
                    try {
                        Exchange exchange = endpoint.createExchange();
                        exchange.getIn().setBody(unwrapIfSingle(response.getAllObjects("default")));
                        processor.process(exchange);
                    } catch (Exception e) {
                        exceptionHandler.handleException(e);
                    }
                });
        }, 3, TimeUnit.SECONDS);
    }

//Note that for now, the Producer component is not completely implemented
```

3. In the root directory, open a command prompt (you can also use an IDE to open the Maven Project)
4. Run `mvn install -DskipTests`  (or launch `install` goal in your IDE) 
5. Go to `KARAF_DIR/etc` and open `org.ops4j.pax.url.mvn.cfg` with Notepad (or another Text editor)
6. If commented, uncomment the line `org.ops4j.pax.url.mvn.localRepository=` and add the path to your local maven repository (usually `${usr_home}/.m2`)
7. Now go to `KARAF_DIR/bin` and launch a Karaf session by running `karaf.bat`
8. Now we need to install the needed feature for Karaf
   - **Camel** (version 2.24.2 is needed for plc4x 0.6): 
     - `feature:repo-add camel 2.24.2`
     - `feature:install camel-blueprint/2.24.2`
   - **PLC4X** 0.6 (*Please be aware of the version installed in your local repository, depending on your Maven settings, it will change it automatically from the version you cloned*)
     - `feature:repo-add mvn:org.apache.plc4x/driver-s7-feature/0.6.1-SNAPSHOT/xml/features` 
     - `feature:install driver-s7-feature`
     - `bundle:install mvn:org.apache.plc4x/plc4j-apache-camel/0.6.1-SNAPSHOT`
9. You should now have following bundle installed

![](https://i.imgur.com/Hi24GRK.png)

![](https://i.imgur.com/3hMiOd9.png)

10. Now set up a maven project in your IDE

    - Create a project with the `karaf-blueprint` [archetype](https://mvnrepository.com/artifact/org.apache.karaf.archetypes/karaf-blueprint-archetype) 
    - Add the s7 driver to the imports in the `pom.xml`

    ```xml
    <Import-Package>
        org.apache.plc4x.java.s7
    </Import-Package>
    ```

    - Create the `PlcDriver` service in your resources
      - create, if not existing, the directory`\src\main\resources\META-INF\services`with the file  `org.apache.plc4x.java.spi.PlcDriver` in it containing (you can also find it [here](https://github.com/apache/plc4x/blob/develop/plc4j/drivers/s7/src/main/resources/META-INF/services/org.apache.plc4x.java.api.PlcDriver))

    ```
    #
    # Licensed to the Apache Software Foundation (ASF) under one
    # or more contributor license agreements.  See the NOTICE file
    # distributed with this work for additional information
    # regarding copyright ownership.  The ASF licenses this file
    # to you under the Apache License, Version 2.0 (the
    # "License"); you may not use this file except in compliance
    # with the License.  You may obtain a copy of the License at
    #
    #   http://www.apache.org/licenses/LICENSE-2.0
    #
    # Unless required by applicable law or agreed to in writing,
    # software distributed under the License is distributed on an
    # "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    # KIND, either express or implied.  See the License for the
    # specific language governing permissions and limitations
    # under the License.
    #
    org.apache.plc4x.java.s7.S7PlcDriver
    ```
    
11. Go to `\src\main\resources\OSGI-INF\my-service.xml` to create your route

``` xml
<blueprint xmlns="http://www.osgi.org/xmlns/blueprint/v1.0.0" default-activation="lazy">
    <camelContext id="PLC-Context" xmlns="http://camel.apache.org/schema/blueprint" streamCache="true">
        <route>
            <from uri="plc4x:s7://192.168.178.10/0/1?address=%25MW102:INT"/>
            <log message=" Reading from PLC:  ${body} at ${date:now:HH:mm:ss}" loggingLevel="INFO" />
            <to uri="mock:test?retainLast=10" />
        </route>
    </camelContext>
</blueprint>
```

- `s7` is the driver used with the addressing as follow `plc4x:{driver}://{host | ip}/{rack}/{slot}?{params}`

- To access a memory area in the PLC
  - `%25` is the encoding for `%` in XML
  - `MW102:INT` for the WORD in the Memento 102 which contains an `INT`
  - Patterns
    -  `%25{MemoryArea}:{byteOffset}.{bitOffset}:{dataType}[numElements]`
    - `%25{MemoryArea}{byte}:{dataType}`

12. Now run the `build` goal to install the bundle to your local  maven repository

13. Go back to Karaf and install the bundle
    - `bundle:install -s mvn:{groupeId}/{artifactId}/{version}`
      *GroupeId, ArtefactId and Version can be found in your*`pom.xml`
    - `bundle:list` to get the bundle number and then `bundle:watch <bundleID>`for easier updates
    - `log:tail` to see the route writing the body to the log

#### Fetching with Poll interval

To fetch data every certain period, you need to adapt your `Consumer` to a `PollConsumer`. Just modify your route to this

```xml
<blueprint xmlns="http://www.osgi.org/xmlns/blueprint/v1.0.0" default-activation="lazy">

    <camelContext id="PLC-Context" xmlns="http://camel.apache.org/schema/blueprint" streamCache="true">
		<route>
            <from uri="timer://foo?fixedRate=true&amp;period=1000"/>
            <pollEnrich>
                <constant>plc4x:s7://192.168.178.10/0/1?address=%25MW102:INT</constant>
            </pollEnrich>
            <log message=" Reading from PLC:  ${body} at ${date:now:HH:mm:ss}" loggingLevel="INFO" />
            <to uri="mock:test?retainLast=10" />
        </route>
    </camelContext>
</blueprint>
```

- Note the `period` parameter specifying the rate of fetching (in milliseconds)

## PLC4X Design

### Drivers and protocols

Here a simple visualization of how the library works. It is copied from this [document](https://github.com/apache/plc4x/blob/rel/0.5/plc4j/protocols/src/site/asciidoc/developers/implementing-drivers.adoc) where we can find every information we need to implement our own driver.

```
  +------------------------------------------+
  |c0BA                                      |
  |               Application                |  Application
  |                                          |
  +---------+--------------------------------+
            |                      ^
 - - - - - -|- - - - - - - - - - - | - - - - - - - - - - -
            v                      |
  +--------------------------------+---------+
  |c05A                                      |
  |                  PLC4X                   |
  |                                          |
  +---------+--------------------------------+
  |         |                      ^         |
  |         v                      |         |
  |  +------------------------------------+  |
  |  |cAAA                                |  |
  |  |      PLC4X Driver Connection       |  |
  |  |                                    |  |
  |  +------+-----------------------------+  |
  |  |      |                      ^      |  |
  |  |      v                      |      |  |
  |  |  +--------------------------+---+  |  |
  |  |  |cAAA                          |  |  |
  |  |  |     PLC4X Protocol Layer     |  |  |
  |  |  |                              |  |  |
  |  |  +---+--------------------------+  |  |
  |  |      |                      ^      |  |
  |  |      v                      |      |  |
  |  |  +--------------------------+---+  |  |
  |  |  :           Optional           |  |  |  PLC4X
  |  |  |      Protocol Layer(s)       |  |  |  Netty
  |  |  |                              |  |  |  Pipeline
  |  |  +---+--------------------------+  |  |
  |  |      |                      ^      |  |
  |  |      v                      |      |  |
  |  |  +--------------------------+---+  |  |
  |  |  |cAAA                          |  |  |
  |  |  |        Protocol Layer        |  |  |
  |  |  |                              |  |  |
  |  |  +---+--------------------------+  |  |
  |  |      |                      ^      |  |
  |  |      v                      |      |  |
  |  +-----------------------------+------+  |
  |  |cAAA                                |  |
  |  |             Connector              |  |
  |  |                                    |  |
  +--+------+-----------------------------+--+
            |                      ^
 - - - - - -|- - - - - - - - - - - | - - - - - - - - - - -
            v                      |
  +--------------------------------+---------+
  |cF6F                                      |
  |                   PLC                    |  Device
  |                                          |
  +------------------------------------------+
```

#### Accessing Data

To request the data from the PLC, plc4x uses `readRequestBuilder` and `writeRequestBuilder`.

Every request is designed like this (simplified)

- `fieldName` that is like an alias to the data
- `fieldQuery` that is the actual address where we want to fetch our data from

The PLC then proceeds to fetch the data and building a response. We can then access the data like this

- `fieldName` the alias of the data

- ```java
response.getAllObject(String fieldName)
  ```


*there can be multiple Objects in a response (if you add*`[numElements]` *on your route parameters, you will get an Array back)*
