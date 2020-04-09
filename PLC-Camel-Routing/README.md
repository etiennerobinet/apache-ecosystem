<img src="https://upload.wikimedia.org/wikipedia/fr/thumb/3/35/Goodyear_logo.svg/1280px-Goodyear_logo.svg.png" width=200 /><img src="https://camo.githubusercontent.com/fef71cfa1b21130afaf48a60c683ff11a24a6fe6/68747470733a2f2f75706c6f61642e77696b696d656469612e6f72672f77696b6970656469612f636f6d6d6f6e732f7468756d622f642f64622f4170616368655f536f6674776172655f466f756e646174696f6e5f4c6f676f5f253238323031362532392e7376672f3130323470782d4170616368655f536f6674776172655f466f756e646174696f6e5f4c6f676f5f253238323031362532392e7376672e706e67" width=200 /><img src="https://camo.githubusercontent.com/86abd95b803d973f9dbda5ae4f46998971aa7296/68747470733a2f2f706c6334782e6170616368652e6f72672f696d616765732f6170616368655f706c6334785f6c6f676f2e706e67" width=200 />

#  OSGi Camel routes with PLC4X

NOTE: This guide uses the 0.7.0-SNAPSHOT version located on the Nexus repository. These features should be available in the official 0.7 Release.

## Features

Inside your container, add following repositories

```xml
camel 3.1.0 //As camel 3.2.0 is still not stable
mvn:org.apache.plc4x/driver-s7-feature/0.7.0-SNAPSHOT/xml/features 	//choose the driver 
mvn:org.apache.plc4x/driver-eip-feature/0.7.0-SNAPSHOT/xml/features	//choose the driver 
mvn:org.apache.plc4x/camel-feature/0.7.0-SNAPSHOT/xml/features
```

After this, install following features:

```xml
camel-blueprint
driver-eip-feature
driver-s7-feature
camel-feature
```

## Setup

You can create a project inside your IDE (you can use the  [karaf blueprint archetype](https://mvnrepository.com/artifact/org.apache.karaf.archetypes/karaf-blueprint-archetype))

Inside the `pom.xml` add the dependency for the driver and the transport used

```xml
<Import-Package>
    org.apache.plc4x.java.{driver}.readwrite,
    org.apache.plc4x.java.transport.{transport}
</Import-Package>
```

Create, if not existing, the directory`\src\main\resources\META-INF\services `with the file `org.apache.plc4x.java.api.PlcDriver` (you can find it [here](https://github.com/apache/plc4x/blob/develop/plc4j/drivers/s7/src/main/resources/META-INF/services/org.apache.plc4x.java.api.PlcDriver)) and the `org.apache.plc4x.java.spi.transport.Transport`(you can also find it [here](https://github.com/apache/plc4x/blob/develop/plc4j/transports/tcp/src/main/resources/META-INF/services/org.apache.plc4x.java.spi.transport.Transport)).

 Make sure the content matches the driver you're using

- Siemens [S7](https://github.com/apache/plc4x/blob/develop/plc4j/drivers/s7/src/main/resources/META-INF/services/org.apache.plc4x.java.api.PlcDriver)
- Allen-Bradley [EIP](https://github.com/apache/plc4x/blob/develop/plc4j/drivers/eip/src/main/resources/META-INF/services/org.apache.plc4x.java.api.PlcDriver)

You can now start creating your routes.

### PLC4X Component

The component takes following URI:

```xml
plc4x:{driver}:{transport}//{host | ip}?tags={List of tags} 
```

Consumer: takes the list of tags and return the same List but setting the values to the values obtained from the PLC

Producer: takes a list of tags (tags must contain a value) to write the tags on the PLC

- TagData
  - `String tagName` : the name of the tag
  - `String query`: the query used to address the tag on the PLC
  - `Object value` (optional): the value of the tag, can be null for Consumer

### Allen Bradley Example

Installing features

```ini
feature:repo-add mvn:org.apache.plc4x/driver-eip-feature/0.7.0-SNAPSHOT/xml/features	
feature:repo-add mvn:org.apache.plc4x/plc4j-camel-feature/0.7.0-SNAPSHOT/xml/features

feature:install driver-eip-feature
feature:install camel-feature
```

Create the project and follow the [setup](#Setup) to add services/dependencies.

- `pom.xml`

```xml
<Import-Package>
    org.apache.plc4x.java.eip.readwrite,
    org.apache.plc4x.java.transport.eip
</Import-Package>
```

#### Reading 

```xml
<camelContext id="AB-PLC-Context" xmlns="http://camel.apache.org/schema/blueprint" streamCache="true">
        <route>
            <from uri="timer:foo?fixedRate=true&amp;period=1000"/>
            <pollEnrich>
                <simple>plc4x:eip:tcp://163.243.183.250/backplane=1&amp;slot=4?tags=#fields</simple>
            </pollEnrich>
            <log message=" Reading from PLC:  ${body} " loggingLevel="INFO" />
            <to uri="mock:test?retainLast=10" />
        </route>
</camelContext>

<bean id="fields" class="java.util.ArrayList">
        <argument>
            <list>
                <bean class="org.apache.plc4x.camel.TagData">
                    <argument index="0" value="UDT Date: Month"/> <!--Name for the tag-->
                    <argument index="1" value="%Date.Month"/> <!-- actual query to fetch the tag-->
                </bean>
                <bean class="org.apache.plc4x.camel.TagData">
                    <argument index="0" value="DIntTest"/>
                    <argument index="1" value="%test_dint"/>
                </bean>
                <bean class="org.apache.plc4x.camel.TagData">
                    <argument index="0" value="ArrayTest"/>
                    <argument index="1" value="%testLargeRealArray:10"/>
                </bean>
            </list>
        </argument>
    </bean>
```

The body contains the List<TagData> in the same order as declared in the bean. You can access them by using `body.get(index).tagName/query/value` 

For query structure, read [this](#Query Syntax).

#### Writing

```xml
    <camelContext id="PLC-IoTDB" xmlns="http://camel.apache.org/schema/blueprint" streamCache="true" >
        <route>
            <from uri="direct:write"/>
            <setBody>
                <simple>${ref:ab-write}</simple>
            </setBody>
            <log message="${body}"/>
            <to uri="plc4x:eip:tcp://163.243.183.250?backplane=1&amp;slot=4"/>
            <log message="${body.values}"/>
        </route>
    </camelContext>

    <bean id="ab-write" class="java.util.ArrayList">
        <argument>
            <list>
                <bean class="org.apache.plc4x.camel.TagData">
                    <argument index="0" value="DIntTest"/> <!--Name-->
                    <argument index="1" value="%test_dint:DINT"/> <!--Query-->
                    <argument index="2" value="150"/> <!--Value-->
                </bean>
            </list>
        </argument>
    </bean>
```

The body contains a `PlcWriteResponse`. This is an PLC4X Object, containing the return code(s) for the queries sent. `${body.values}` will print a Map<TagName,ResponseCode>

#### Query Syntax 

```java
^%(?<tag>[a-zA-Z_.0-9]+\[?[0-9]*\]?):?(?<dataType>[A-Z]*):?(?<elementNb>[0-9]*)
```

- Must start with "%"
- `tag` can contain literals, numbers, "_" and "."
  - The "." is used to call UDT or other Structures. It separates the structure from the member you want
    - `Date.Month` returns member `Month` of the structure `Date` (custom Structure, works the same for basic structures)
- If `tag` is referring to an array, you can provide a starting index using "[]"
  - `%array[10]` will fetch the array starting at index 10
- For writing, add the datatype of the tag separated by ":"

|   Datatypes    |      Codes       |
| :------------: | :--------------: |
|     String     | STRING, STRING36 |
|    Integer     |       INT        |
| Short Integer  |       SINT       |
| Double Integer |       DINT       |
|  Long Integer  |       LINT       |
|      Bit       |       BOOL       |
|      Real      |       REAL       |
|  Double Word   |      DWORD       |

- In case of an array, you can specify the number of elements you want to fetch
  - `%array:10` will fetch the first 10 elements

