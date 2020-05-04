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
<convertBodyTo type="Map"/>
```

Format
-------  

***XML***
```xml
<root>
    <Code>
        <FTC>123 456 789</FTC>
        <TIC>U123</TIC>
    </Code>
</root>
```


***ZF MAP***
```ini
Code.FTC = 123 456 789
Code.TIC = U123                                      
```



Intallation
-----------
bundle:install mvn:goodyear.corp.ge.commons.camel/plc4x-dataformat/LATEST

Author
------
GE