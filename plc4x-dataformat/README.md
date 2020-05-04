# PLC4X Data Format 

Purpose
-------
This is the camel dataformat to marshall and unmarshall XML objects to the PLC4X component.  


Building and Deploying
----------------------
 mvn test  

Examples
--------

Add the bean definition to your camel context

```xml
<convertBodyTo type="Document"/>
```

Then you can unmarshal from XML to Plc4X format  
```xml
<convertBodyTo type="List"/>
```

Format
-------  

***XML***
```xml
<transaction>
  <tag name="tag1"/>
  <tag name="tag2" type="STRING">ge</tag>
  <tag length="3" name="tag3" type="INT">
    <value idx="1">1</value>
    <value idx="2">2</value>
    <value idx="3">3</value>
  </tag>
</transaction>

```


***TagData List***
```ini
-TagData("tag1","%tag1")    
-TagData("tag2","%tag2:STRING","ge") 
-TagData("tag3","%tag3:5:INT",{1,2,3}) 
```



Intallation
-----------
-

Author
------
Robinet Etienne
