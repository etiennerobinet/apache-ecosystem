# Writing generated Driver <img src="https://plc4x.apache.org/images/apache_plc4x_logo.png" width="150" /> 

## Clone latest source and build

In order to make sure to be up to date, clone the latest sources from the official [git](https://github.com/apache/plc4x). If you want to submit  your work later on, don't forget to check out the[contributing](https://plc4x.apache.org/developers/contributing.html) page.

Once downloaded, follow the `README` instructions to build from sources. The following guide will use the 0.7.0-SNAPSHOT version, if your version differs, make sure to specify the correct version inside your `pom` files. This guide is based on the development of the EIP Driver.

NOTE: make sure to add the [Apache Source Header](https://www.apache.org/legal/src-headers.html) in every file you create.

## Create modules

To create a new driver, you need to create following modules inside the project:

-   `plc4x-protocols`: create a module called `plc4x-protocols-eip`

-   `plc4j-drivers`: create a module called `plc4j-driver-eip`

The `plc4x-protocols` modules contain the `mspec` file which is used to describe
the protocol used by the driver, independent from the language. The
`plc4j-drivers` is a module of the Java implementation of PLC4X that will be
used to implement the protocol into Java.

### plc4x-protocols

`pom.xml`: make sure to have to specify the parent `pom` and add the following
dependency

```xml
<?xml version="1.0" encoding="UTF-8"?>  
<project xmlns="http://maven.apache.org/POM/4.0.0"  
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  
xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
http://maven.apache.org/xsd/maven-4.0.0.xsd">  
<parent>  
<artifactId>plc4x-protocols</artifactId>  
<groupId>org.apache.plc4x</groupId>  
<version>0.7.0-SNAPSHOT</version>  
</parent>  
<modelVersion>4.0.0</modelVersion>  

<artifactId >plc4x-protocols-eip</artifactId>  
    <name>Eip Driver</name>  
<description>Base protocol specifications for the EIP protocol</description>  

<dependencies>  
<dependency>  
<groupId>org.apache.plc4x</groupId>  
<artifactId>plc4x-build-utils-protocol-base-mspec</artifactId>  
<version>0.7.0-SNAPSHOT</version>  
</dependency>  
</dependencies>  
</project>

```

Create  a new class called
[EipProtocol](https://github.com/apache/plc4x/blob/develop/protocols/eip/src/main/java/org/apache/plc4x/protocol/eip/EipProtocol.java) in the package `org.apache.plc4x.protocol.eip`. This class will be used to load
the mspec file in order to generate the classes.`

```java
@Override 
public Map<String, TypeDefinition> getTypeDefinitions() throws GenerationException { 
InputStream schemaInputStream = EipProtocol.class.getResourceAsStream("/protocols/eip/eip.mspec");
    if(schemaInputStream == null) { 
        throw new GenerationException("Error loading message-format schema for protocol '" + getName() + "'");
    } 
    return new MessageFormatParser().parse(schemaInputStream);}  
}
```

To export your protocol as service, you need to create a file called *org.apache.plc4x.plugins.codegenerator.protocol.Protocol* under `/src/main/resources/ META-INF/services` with following [content](https://github.com/apache/plc4x/blob/develop/protocols/eip/src/main/resources/META-INF/services/org.apache.plc4x.plugins.codegenerator.protocol.Protocol).

Finally, you can create the protocol description under `src/main/resources/protocols/eip/eip.mspec.`

For the content of the `mspec` please refer to its dedicated [section](#mspec).

### plc4j-drivers

-   `pom.xml`: make sure to specify the parent `pom` and add required
    dependencies. Depending on the features you want to implement, you can add
    some later. Here is the minimum

#### POM


```xml
<parent>  
	<groupId>org.apache.plc4x</groupId>  
	<artifactId>plc4j-drivers</artifactId>  
	<version>0.7.0-SNAPSHOT</version>  
</parent>  
…  
<build>  
	<plugins>  
		<plugin>  
			<groupId>org.apache.plc4x.plugins</groupId>  
			<artifactId>plc4x-maven-plugin</artifactId>  
			<executions>  
				<execution>  
					<id>test</id>  
					<phase>generate-sources</phase>  
					<goals>  
						<goal>generate-driver</goal>  
					</goals>  
					<configuration>  
						<protocolName>eip</protocolName>  
						<languageName>java</languageName>  
						<outputFlavor>read-write</outputFlavor>  
					</configuration>  
				</execution>  
			</executions>  
		</plugin>  
		<plugin>  
			<groupId>org.apache.maven.plugins</groupId>  
			<artifactId>maven-dependency-plugin</artifactId>  
			<configuration>  
                <usedDependencies combine.children="append">
                    <usedDependency>
                        org.apache.plc4x:plc4x-build-utils-language-java
                    </usedDependency>  
                    <usedDependency>  
                        org.apache.plc4x:plc4x-protocols-eip  
                    </usedDependency>  
                </usedDependencies>  
			</configuration>  
		</plugin>  
	</plugins>  
</build>  
… <!--Make sure to add the transport used by your protocol-->  
<dependency>  
	<groupId>org.apache.plc4x</groupId>  
	<artifactId>plc4j-transport-tcp</artifactId>  
	<version>0.7.0-SNAPSHOT</version>  
</dependency>
```
For the full pom, please refer to the repository
[file](https://github.com/apache/plc4x/blob/develop/plc4j/drivers/eip/pom.xml).

## Packages

Create following packages and classes :

### `org.apache.plc4x.java.eip.readwrite`

[EipDriver.java](#Driver) used to describe the port used, the protocol code to
use when parsing the URI, the default transport and the ability to
write/read/subscribe

### `org.apache.plc4x.java.eip.readwrite.configuration`

[EipConfiguration.java](#Configuration) here you can describe the parameters you
can give through the URI and can be later used in the logic of the Protocol

### `org.apache.plc4x.java.eip.readwrite.field`

`EipField.java` used to describe the field in your implementation. This will be
later used in the Protocol logic to encode/decode into/from the packet

`EipFieldHandler.java` will be used to create the fields matching the Pattern
defined in `Brolfield.java` . Here you will also describe how the values from
the write request will be handled

### `org.apache.plc4x.java.eip.readwrite.protocol`

`EipProtocolLogic.java` describes the logic of the [protocol](#ProtocolLogic).
Here you will implement the way to connect, disconnect, send and receive package
using the plc4x interfaces like `PlcReadRequest`, `PlcWriteResponse,` etc.

## Configuration

The Configuration class will contain the information and configuration of the
current connection to the PLC. Here we can declare a slot number, rack number or
any other parameters that are relevant to communicate with the PLC. For the full
EIP Configuration, refer to the
[repository](https://github.com/apache/plc4x/blob/develop/plc4j/drivers/eip/src/main/java/org/apache/plc4x/java/eip/readwrite/configuration/EIPConfiguration.java).

This class will also implement
`org.apache.plc4x.java.spi.configuration.Configuration` and the chosen Transport
Configuration `org.apache.plc4x.java.transport.tcp.TcpConfiguration`
```java
@ConfigurationParameter  
private int slot;
```
This declares a parameter of type `int` and called slot. This parameter can be
used by the [ProtocolLogic](#ProtocolLogic). The annotation `ConfigurationParameter` will be used by the `GenerateBasDriver` class to parse the parameters from the URI. It will then use the other parameters too in order to create a connection. The connection will then fire a `ConnectionEvent` or
`DisconnectEvent` and the Logic will handle the actions in between.

## Mspec

To describe the packet, the `mspec` provides different types:

-   `type` : describes a basic type with attributes

-   `discriminatedType`: describes an interface for types differentiated by one
    or more value (discriminator)

-   `enum` : describes an enumeration

-   `dataIo`: if the Data needs to be serialized/parsed in a particular way, we
    can describe it here (Strings, Time, etc. )

Every type has one or more attributes:

-   `const` : a constant field where we fix the value, with a name so we can
    access it

-   `reserved` : same as the constant field but without name

-   `implicit`: an implicit field has a variable value that is calculated at
    runtime

-   `array`: an array where we specify the type of data and the length of the
    array

-   `discriminator`: a field that differentiates a `discriminatedType` used with
    the `typeSwitch` instruction

### Eip.mspec

This describes the base type of packet. Every packet created will be
encapsulated inside an EipPacket, as these are the only objects being sent by
the connection to the PLC.
```shell
[discriminatedType 'EipPacket'  
    [discriminator uint 16 'command']  
    [implicit uint 16 'len' 'lengthInBytes - 24']  
    [simple uint 32 'sessionHandle']  
    [simple uint 32 'status']  
    [array uint 8 'senderContext' count '8']  
    [simple uint 32 'options']  
    [typeSwitch 'command'  
        ['0x0065' EipConnectionRequest  
            [const uint 16 'protocolVersion' '0x01']  
            [const uint 16 'flags' '0x00']  
        ]  
        ['0x0066' EipDisconnectRequest  
        ]  
        ['0x006F' CipRRData [uint 16 'len']  
            [reserved uint 32 '0x00000000']  
            [reserved uint 16 '0x0000']  
            [simple CipExchange 'exchange' ['len-6']]  
        ]  
    ]  
]
```
If we look at the base packet type, we can see that in order to send Data, we
need to create a CipRRData packet. This packet contains another new type, the
CipExchange. This exchange will contain some information about the packet itself
and the desired service.
```shell
[type 'CipExchange' [uint 16 'exchangeLen']  
    [const uint 16 'itemCount' '0x02']  
    [const uint 32 'nullPtr' '0x0']  
    [const uint 16 'UnconnectedData' '0x00B2'  
    [implicit uint 16 'size' 'lengthInBytes - 8 - 2']  
    [simple CipService 'service' ['exchangeLen - 10'] ]  
]
```
Like EipPacket, the CipService is a “discriminatedType”. The type of service is
differentiated by the command number.
```shell
[discriminatedType 'CipService' [uint 16 'serviceLen']  
	[discriminator uint 8 'service']  
	[typeSwitch 'service'  
        ['0x4C' CipReadRequest  
        …
        ]  
        ['0xCC' CipReadResponse  
        …  
        ]  
        ['0x4D' CipWriteRequest  
        …  
        ]  
        ['0xCD' CipWriteResponse  
        …  
        ]  
        ['0x0A' MultipleServiceRequest  
        …  
        ]  
        ['0x8A' MultipleServiceResponse  
        …  
        ]  
        ['0x52' CipUnconnectedRequest  
        …  
        ]  
	]  
]
```
Inside the mspec file, there are also enumerations, used to define data types but also error and status codes. There are also other types declared. For further details, please refer to the complete [file](https://github.com/apache/plc4x/blob/develop/protocols/eip/src/main/resources/protocols/eip/eip.mspec) on the repository.

## Driver

The driver will describe the port to use, a packet size estimator that will
indicate how to find the size of a packet , the type of packet we use and
a Corrupt Package collector if we want. Here an example of the [`EIPDriver`](https://github.com/apache/plc4x/blob/develop/plc4j/drivers/eip/src/main/java/org/apache/plc4x/java/eip/readwrite/EIPDriver.java)
and some comments on the code:

To enable the Driver as service inside Karaf, we need to add the OSGi Component
annotation. This declares this class as a service of the PlcDriver class that
will publish itself at start.

```java
@Component(service = PlcDriver.class, immediate = true
```
This component will be later removed as the API now has an OSGi Activator class
that will add every Driver to the Service Registry.

Specify base packet as EipPacket (it must be described in the mspec)
```java
public class EIPDriver extends GeneratedDriverBase<EipPacket> {
```
Specify the code to be able to load driver with uri. This will be used by the
Service Loader to lookup for Drivers. This code will be associated to this
class, so when an URI contains the “eip” as driver, the Service Loader will look
for a Driver class that has “eip” as protocol code.
```java
@Override  
public String getProtocolCode() {  
	return "eip";  
}
```
Create the Protocol Configurer, passing the packet type, its
IO(parser/serializer), the logic to use, eventually a packet size estimator or
corrupt package remover, the info if the connection uses little endian.
```java
@Override  
protected ProtocolStackConfigurer<EipPacket> getStackConfigurer() {  
	return SingleProtocolStackConfigurer.builder(EipPacket.class, EipPacketIO.class)  
        .withProtocol(EipProtocolLogic.class)  
        .withPacketSizeEstimator(ByteLengthEstimator.class)  
        .littleEndian()  
        .build();  
}
```
This class will be called to get the size of the packets sent and received. This
can be different from Driver to Driver.  In this case, we know from our mspec that the length of the package is described the second byte. This length is the payload without the header (of size 24 if we
count the bytes described in the mspec) so we add 24 to get the total length (used to decode a response from the PLC)

```java
public static class ByteLengthEstimator implements ToIntFunction<ByteBuf> {  
@Override  
	public int applyAsInt(ByteBuf byteBuf) {  
		if (byteBuf.readableBytes() >= 4) {  
			int size = ByteBuf.getUnsignedShort(byteBuf.readerIndex()+1)+24;  
			return size;  
		}  
		return -1;  
		}  
	}
```
## Protocol Logic

In this class, we will describe the interaction of PLC4X with the PLC. This includes the connection, disconnection, read and write requests. You can have a look
[here](https://github.com/apache/plc4x/blob/develop/plc4j/drivers/eip/src/main/java/org/apache/plc4x/java/eip/readwrite/protocol/EipProtocolLogic.java)
to get an example of such a class.

The packet classes and enumerations used here are the ones we declared in the Mspec file. Every field that is not constant, reserved, discriminator or implicit will be a parameter of the constructor.

This method will be called when plc4x creates the pipeline (Transport Layer) to the PLC. This tells the protocol to execute the necessary actions (sending packets) to establish a connection with the PLC. In the EIP case, it will send a RegisterSession packet and wait for the response to assign him with a Session
handle.

```java
@Override  
public void onConnect(ConversationContext<EipPacket> context)
```
This method will be called when the user built a PlcRequest and wants it to be
send to the PLC. The method will parse the request, create the necessary packet
to execute a request of the correct data and parse the response given by the PLC
to create a API object the user can then use.
```java
@Override  
public CompletableFuture<PlcReadResponse> read(PlcReadRequest readRequest)
```
Same as for the read() method. We get a writeRequest and we need to extract the
data from it to create an appropriate packet for our PLC . Once we get the
response, we check that is indeed a response (we know the structure from the
mspec) and create a PlcWriteResponse.


```java
@Override 
public CompletableFuture<PlcWriteResponse> write(PlcWriteRequest writeRequest)
```
`PlcReadRequest` contains a `LinkedHashMap<String, PlcField> fields` where we can access the encoded PlcField by the API. This can be cast to the given field type (`BrolField.java`) to access needed data.

`PlcReadResponse` contains a `Map<String, Pair<PlcResponseCode, PlcValue>> fields` where the String matches the one of the Map in the request and the PlcResponseCode is parsed from the response. The PlcValue is also create by the ProtocolLogic with the data fetched from the packet.

To successfully parse and correctly encode your requests/responses, you might need/want to add utility methods. This choice is left to your discretion.

`PlcWriteRequest` has a `LinkedHashMap<String, Pair<PlcField, PlcValue>> fields
`containing the field we want to write to and the value we want to write into the field.

`PlcWriteResponse` has a `Map<String, PlcResponseCode> values` that we tell us if the writing was successful. So, with the response of the PLC, we need to create a map with the String (fieldName) and its correspondent response code (that we decoded from the packet). The `PlcWriteResponse` also contains the
original request (to make the link with the fieldName).