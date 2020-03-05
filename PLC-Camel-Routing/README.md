![](https://plc4x.apache.org/images/apache_plc4x_logo.png)





[TOC]



# PLC4X - Camel-blueprint integration in Karaf

For now, only tests with the S7 driver have been made

## Requirements

- Java 8
- Git
- Karaf 4.2.8
- Maven 3.6.x
- IDE with maven project

## Installation

1. Download [PLC4X](https://plc4x.apache.org/) sources (download custom sources [git](https://github.com/etiennerobinet/plc4x/tree/high-frequency-camel)) 
   - The README was written using v0.7.0-SNAPSHOT that was modified
   - If you use the sources from the repository, jump to point 4.
2. Go to `plc4j\integrations\apache-camel\src\main\java\org\apache\plc4x\camel` and find the `PLC4XConsumer.java` and `PLC4XProducer.java` source files
   - as of February 2020, Camel is not fully integrated in PLC4X and you need to change the `doStart()` method of the Consumer and the `process()` method of the producer :

```java
//CONSUMER 
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

//PRODUCER

@Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();
        String fieldName = in.getHeader(Constants.FIELD_NAME_HEADER, String.class);
        String fieldQuery = in.getHeader(Constants.FIELD_QUERY_HEADER, String.class);
        Object body = in.getBody();
        log.info("Body {} : {} ",body.getClass().getName(), body.toString());
        log.info("{} : {} and {} : {}",Constants.FIELD_NAME_HEADER, fieldName, Constants.FIELD_QUERY_HEADER, fieldQuery);
        PlcWriteRequest.Builder builder = plcConnection.writeRequestBuilder();
        if (body instanceof List) {
            List<?> bodyList = in.getBody(List.class);
            Object[] values = bodyList.toArray();
            for( Object obj : values){
                log.debug("Values obtained {}" ,obj);
            }
            builder.addItem(fieldName, fieldQuery, values);
        } else {
            Object value = in.getBody(Object.class);
            log.info("Value obtained {}" ,value.getClass().getName());
            builder.addItem(fieldName, fieldQuery, value);
        }
        CompletableFuture<? extends PlcWriteResponse> completableFuture = builder.build().execute();
        int currentlyOpenRequests = openRequests.incrementAndGet();
        try {
            log.debug("Currently open requests including {}:{}", exchange, currentlyOpenRequests);
            Object plcWriteResponse = completableFuture.get();
            if (exchange.getPattern().isOutCapable()) {
                Message out = exchange.getOut();
                out.copyFrom(exchange.getIn());
                out.setBody(plcWriteResponse);
            } else {
                in.setBody(plcWriteResponse);
            }
        } finally {
            int openRequestsAfterFinish = openRequests.decrementAndGet();
            log.trace("Open Requests after {}:{}", exchange, openRequestsAfterFinish);
        }
    }
```

3. To use the write-function of the library, as of version 0.7.0, the generated sources for the S7 Driver needs to be modified

   - `JavaLanguageTemplateHandler.java` which is a helper class used by the template for generation

     ```java
      public String getSizeInBits(ComplexTypeDefinition complexTypeDefinition) {
             int sizeInBits = 0;
             StringBuilder sb = new StringBuilder("");
             for (Field field : complexTypeDefinition.getFields()) {
                 if(field instanceof ArrayField) {
                     ArrayField arrayField = (ArrayField) field;
                     final SimpleTypeReference type = (SimpleTypeReference) arrayField.getType();
                     switch (arrayField.getLoopType()) {
                         case COUNT:
                             sb.append("(").append(toSerializationExpression(null, arrayField.getLoopExpression(), null)).append(" * ").append(type.getSizeInBits()).append(") + ");
                             break;
                         case LENGTH:
                             sb.append("(").append(toSerializationExpression(null, arrayField.getLoopExpression(), null)).append(" * 8) + ");
                             break;
                         case TERMINATED:
                             // No terminated.
                             break;
                     }
                 } else if(field instanceof TypedField) {
                     TypedField typedField = (TypedField) field;
                     final TypeReference type = typedField.getType();
                     if(field instanceof ManualField) {
                         ManualField manualField = (ManualField) field;
                         sb.append("(").append(toSerializationExpression(null, manualField.getLengthExpression(), null)).append(") + ");
                     }
                     else if(type instanceof SimpleTypeReference) {
                         SimpleTypeReference simpleTypeReference = (SimpleTypeReference) type;
                         sb.append(simpleTypeReference.getSizeInBits()).append(" + ");
                         //sizeInBits += simpleTypeReference.getSizeInBits();
                     } else {
                         // No ComplexTypeReference supported.
                     }
                 }
             }
             return sb.toString() + sizeInBits;
         }
     
     //This is due to the package not being optimized for S7. The data is written into //a buffer sized in bytes, but the PLC accepts only data sized in bits
     //E.g.: A Byte will be written into a 8bytes buffer 
     //[data][0][0][0][0][0][0][0] where it should be [data] but this the PLC does not //accept (Error 0x07 : Data Size Mismatch)
     //
     ```

   - `s7.mspec` which is the specification file that will be used to generate the sources

     ```
     [dataIo 'DataItem' [uint 8 'dataProtocolId']
         [typeSwitch 'dataProtocolId'
             // -----------------------------------------
             // Bit
             // -----------------------------------------
             ['01' Boolean
                 [reserved uint 7 '0x00']
                 [simple   bit    'value']
             ]
     
             // -----------------------------------------
             // Bit-strings
             // -----------------------------------------
             // 1 byte
             ['11' Byte
                 [simple int 8 'value']
             ]
             // 2 byte (16 bit)
             ['12' Short
                 [simple int 16 'value']
             ]
             // 4 byte (32 bit)
             ['13' Integer
                 [simple int 32 'value']
             ]
             // 8 byte (64 bit)
             ['14' Long
                 [simple int 64 'value']
             ]
     .
     .
     .
     ]
     ```

    - To use PLC4X at high frequency for a long time, you need to edit the Camel Integration to keep the connection open and change the AtomicInteger used to get reference from async IO reset after getting bigger than the `Short.MAX_VALUE`

   `org.apache.plc4x.camel`

      ```java
      //-------------------------------------------------ENDPOINT-------------------------------------------------
    private final PlcDriverManager plcDriverManager;
    private  PlcConnection connection;
    private String uri;
   
    public Plc4XEndpoint(String endpointUri, Component component) {
        super(endpointUri, component);
        plcDriverManager= new PlcDriverManager();;
        uri = endpointUri;
        try {
            String plc4xURI = uri.replaceFirst("plc4x:/?/?", "");
            connection = plcDriverManager.getConnection(plc4xURI);
        } catch (PlcConnectionException e) {
            e.printStackTrace();
        }
    }
   
    public PlcConnection getConnection() {
        return connection;
    }
   
    @Override
    public Producer createProducer() throws Exception {
        if(!connection.isConnected()){
            try{
                connection= plcDriverManager.getConnection(uri.replaceFirst("plc4x:/?/?", ""));
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        return new Plc4XProducer(this);
    }
   
    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        if(!connection.isConnected()){
            try{
                connection= plcDriverManager.getConnection(uri.replaceFirst("plc4x:/?/?", ""));
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        return new Plc4XConsumer(this, processor);
    }
   
    @Override
    public PollingConsumer createPollingConsumer() throws Exception {
        if(!connection.isConnected()){
            try{
                connection= plcDriverManager.getConnection(uri.replaceFirst("plc4x:/?/?", ""));
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        return new Plc4XPollingConsumer(this);
    }
    
      //-------------------------------------------------CONSUMER-------------------------------------------------
      public Plc4XConsumer(Plc4XEndpoint endpoint, Processor processor) throws PlcException {
        this.endpoint = endpoint;
        this.dataType = endpoint.getDataType();
        this.processor = AsyncProcessorConverterHelper.convert(processor);
        this.exceptionHandler = new LoggingExceptionHandler(endpoint.getCamelContext(), getClass());
        //get connection from endpoint
        this.plcConnection = endpoint.getConnection();
        this.fieldQuery = endpoint.getAddress();
    }
    
    @Override
    protected void doStop() throws InterruptedException, ExecutionException, TimeoutException {
        // First stop the polling process
        if (future != null) {
            future.cancel(true);
        }
    }
     private final PlcDriverManager plcDriverManager;
    private  PlcConnection connection;
    private String uri;
   
    public Plc4XEndpoint(String endpointUri, Component component) {
        super(endpointUri, component);
        plcDriverManager= new PlcDriverManager();;
        uri = endpointUri;
        try {
            String plc4xURI = uri.replaceFirst("plc4x:/?/?", "");
            connection = plcDriverManager.getConnection(plc4xURI);
        } catch (PlcConnectionException e) {
            e.printStackTrace();
        }
    }
   
    public PlcConnection getConnection() {
        return connection;
    }
   
    @Override
    public Producer createProducer() throws Exception {
        if(!connection.isConnected()){
            try{
                connection= plcDriverManager.getConnection(uri.replaceFirst("plc4x:/?/?", ""));
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        return new Plc4XProducer(this);
    }
   
    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        if(!connection.isConnected()){
            try{
                connection= plcDriverManager.getConnection(uri.replaceFirst("plc4x:/?/?", ""));
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        return new Plc4XConsumer(this, processor);
    }
   
    @Override
    public PollingConsumer createPollingConsumer() throws Exception {
        if(!connection.isConnected()){
            try{
                connection= plcDriverManager.getConnection(uri.replaceFirst("plc4x:/?/?", ""));
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        return new Plc4XPollingConsumer(this);
    }
    
    //-------------------------------------------------PRODUCER-------------------------------------------------
    public Plc4XProducer(Plc4XEndpoint endpoint) throws PlcException {
        super(endpoint);
        String plc4xURI = endpoint.getEndpointUri().replaceFirst("plc4x:/?/?", "");
        //get connection from endpoint
        this.plcConnection = endpoint.getConnection();
        if (!plcConnection.getMetadata().canWrite()) {
            throw new PlcException("This connection (" + plc4xURI + ") doesn't support writing.");
        }
        openRequests = new AtomicInteger();
    }
    
    @Override
    protected void doStop() throws Exception {
        int openRequestsAtStop = openRequests.get();
        log.debug("Stopping with {} open requests", openRequestsAtStop);
        if (openRequestsAtStop > 0) {
            log.warn("There are still {} open requests", openRequestsAtStop);
        }
    }
      ```

      `org.apache.plc4x.java.s7.readwrite.protocol.S7ProtocolLogic.java`

      ```java
      private CompletableFuture<S7MessageResponseData> readInternal(S7MessageRequest request) {
              CompletableFuture<S7MessageResponseData> future = new CompletableFuture<>();
      
              //To stay synced, the tpudGenerator must not go over the Short.MAX_VALUE
              //as it is cqsted to short later on to resync request and response
              //tpudRef 0-10 is reserved and if it exceeds 65535 it will be casted to 0
              //here we reset it to its initial value of 10
              if(tpduGenerator.get()>= (Short.MAX_VALUE-1)){
                  tpduGenerator.set(10);
              }
              int tpduId = tpduGenerator.getAndIncrement();
      
      
      	.
          .
          .
          .
       }
      ```

      

4. In the root directory, open a command prompt (you can also use an IDE to open the Maven Project)

5. Run `mvn install -DskipTests`  (or launch `install` goal in your IDE) 

6. Go to `KARAF_DIR/etc` and open `org.ops4j.pax.url.mvn.cfg` with Notepad (or another Text editor)

7. If commented, uncomment the line `org.ops4j.pax.url.mvn.localRepository=` and add the path to your local maven repository (usually `${usr_home}/.m2`)

8. Now go to `KARAF_DIR/bin` and launch a Karaf session by running `karaf.bat`

9. Now we need to install the needed feature for Karaf

   - **Camel** (version 2.24.2 is needed for plc4x : 
     - `feature:repo-add camel 2.24.2`
     - `feature:install camel-blueprint/2.24.2`
   - **PLC4X**  (*Please be aware of the version installed in your local repository, depending on your Maven settings, it will change it automatically from the version you cloned*)
     - `feature:repo-add mvn:org.apache.plc4x/driver-s7-feature/{version}/xml/features` 
     - `feature:install driver-s7-feature`
     - `bundle:install mvn:org.apache.plc4x/plc4j-apache-camel/{version}`

10. You should now have following bundle installed

![](https://i.imgur.com/Hi24GRK.png)

![](https://i.imgur.com/3hMiOd9.png)

10. Now set up a maven project in your IDE

    - Create a project with the `karaf-blueprint` [archetype](https://mvnrepository.com/artifact/org.apache.karaf.archetypes/karaf-blueprint-archetype) 
    - Add the s7 driver to the imports in the `pom.xml`

    ```xml
    <Import-Package>
        org.apache.plc4x.java.s7.readwrite,
        org.apache.plc4x.java.transport.tcp
    </Import-Package>
    ```

    
    - Create the `PlcDriver` and `TcpTransport` services in your resources
  - create, if not existing, the directory`\src\main\resources\META-INF\services`with the file  `org.apache.plc4x.java.api.PlcDriver` (you can find it [here](https://github.com/apache/plc4x/blob/develop/plc4j/drivers/s7/src/main/resources/META-INF/services/org.apache.plc4x.java.api.PlcDriver)) and the `org.apache.plc4x.java.spi.transport.Transport`(you can also find it [here](https://github.com/apache/plc4x/blob/develop/plc4j/transports/tcp/src/main/resources/META-INF/services/org.apache.plc4x.java.spi.transport.Transport))
    
11. Go to `\src\main\resources\OSGI-INF\my-service.xml` to create your route

```xml
<blueprint xmlns="http://www.osgi.org/xmlns/blueprint/v1.0.0" default-activation="lazy">
    <camelContext id="S7-PLC-Context" xmlns="http://camel.apache.org/schema/blueprint" streamCache="true" >
        <route>
            <!-- This will read the address given once and print it in the log-->
            <from uri="plc4x:s7:tcp://192.168.178.10?address=%25MW102:WORD" />
            <log message="{body}" />
            <to uri="mock:test?retainLast=25" />
        </route>
    </camelContext>
</blueprint>

```

- `s7` is the driver used with the addressing as follow `plc4x:{driver}:{transport}//{host | ip}?{params}`

- To access a memory area in the PLC

  - `%25` is the encoding for `%` in XML

  - `MW102:WORD` for the WORD in the Memento 102 which contains an  `INT`

  - Patterns

    - `%25{MemoryArea}{TransferSizeCode}:{byte}.{bit}:{dataType}[numElements]`

    - `%25DB{DataBlockNumber}.DB{TransferSizeCode}{byte}{bit}:{dataType}`

      | Java Type | S7 Data Type | Transfer Size Code |
      | :-------: | :----------: | :----------------: |
      |  Boolean  |     BOOL     |         X          |
      |   byte    |     BYTE     |         B          |
      |   short   |  WORD / INT  |         W          |
      |  Integer  |    DWORD     |         D          |

      


12. Now run the `build` goal to install the bundle to your local  maven repository

13. Go back to Karaf and install the bundle
    - `bundle:install -s mvn:{groupeId}/{artifactId}/{version}`
      *GroupeId, ArtefactId and Version can be found in your*`pom.xml`
    - `bundle:list` to get the bundle number and then `bundle:watch <bundleID>`for easier updates
    - `log:tail` to see the route writing the body to the log

### Fetching with Poll interval

To fetch data every certain period, you need to adapt your `Consumer` to a `PollConsumer`. Just modify your route to this
```xml
<blueprint xmlns="http://www.osgi.org/xmlns/blueprint/v1.0.0" default-activation="lazy">

    <camelContext id="PLC-Context" xmlns="http://camel.apache.org/schema/blueprint" streamCache="true">
    	<route>
            <from uri="timer://foo?fixedRate=true&amp;period=1000"/>
            <pollEnrich>
                <constant>plc4x:s7://192.168.178.10/0/1?address=#fields</constant>
            </pollEnrich>
            <log message=" Reading from PLC:  ${body.get(0)} and ${body.get(1)} at ${date:now:HH:mm:ss}" loggingLevel="INFO" />
            <to uri="mock:test?retainLast=10" />
        </route>
    </camelContext>
</blueprint>

<bean id="fields" class="java.util.ArrayList">
     <argument>
         <list>
             <value type="java.lang.String">%DB1.DBD0:REAL</value> <!-- 0 -->
             <value type="java.lang.String">%DB1.DBW4:INT</value> <!-- 1 -->
         </list>
     </argument>
```

- Note the `period` parameter specifying the rate of fetching (in milliseconds)
- UPDATE: we now pass a List of Strings to fetch multiple data from the PLC

### Writing

For now, plc4x correctly implements writing `BOOL`, `BYTE`, `WORD`, and `DWORD` to a S7 PLC. To do this in a Camel route, you need to add specific Headers including the `fieldName` used to receive corresponding Response code from the PLC and a `fieldQuery` specifying the location on the PLC where you want to write the data. Here is a small example of a Camel route reading from a Data Block on the PLC and transferring it to a Memento (that can be used to display the value)

- For now, batch writing is not fully stable

```xml
<blueprint xmlns="http://www.osgi.org/xmlns/blueprint/v1.0.0" default-activation="lazy">
    <camelContext id="S7-PLC-Context" xmlns="http://camel.apache.org/schema/blueprint" streamCache="true" >
        <route>
            <from uri="timer://foo?fixedRate=true&amp;period=500" />
            <pollEnrich>
                <constant>plc4x:s7:tcp://192.168.178.10?address=#fields</constant>
            </pollEnrich>
            <setHeader  headerName="fieldName">
                <constant>default</constant>
            </setHeader>
            <setHeader headerName="fieldQuery">
                <constant>%MW10:WORD</constant>
            </setHeader>
            <setBody>
            	<simple>${body.get(0)}</simple>
            </setBody>
            <convertBodyTo type="java.lang.Short"/>
            <log message=" Sending to PLC:  ${body} at ${date:now:HH:mm:ss}" loggingLevel="INFO" />
            <to uri="plc4x:s7:tcp://192.168.178.10" />
            <log message=" Response code: ${body.getResponseCode(default)} at ${date:now:HH:mm:ss}" loggingLevel="INFO" />
        </route>
    </camelContext>
</blueprint>

<bean id="fields" class="java.util.ArrayList">
     <argument>
         <list>
             <value type="java.lang.String">%DB1.DBD0:REAL</value> <!-- 0 -->
             <value type="java.lang.String">%DB1.DBW4:INT</value> <!-- 1 -->
         </list>
     </argument>
```


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
```

*there can be multiple Objects in a response (if you add*`[numElements]` *on your route parameters, you will get an Array back)*
```