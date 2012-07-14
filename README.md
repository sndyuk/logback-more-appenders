Logback more appenders
==================================================
is appenders for [Logback](http://logback.qos.ch/).

Appenders
--------------------------------------
- [fluentd](http://fluentd.org/)  
    - depend on [fluent-logger for Java](https://github.com/fluent/fluent-logger-java).   
     - Install fluentd before running logger.
- [Amazon DynamoDB](http://aws.amazon.com/jp/dynamodb/)  
    - depend on [aws-java-sdk](http://aws.amazon.com/jp/sdkforjava/).
    - Create Amazon DynamoDB Table


Installing
--------------------------------------	

###Install jars from Maven2 repository
Configure your pom.xml:

```
<dependencies>

  <dependency>
	<groupId>ch.qos.logback</groupId>
	<artifactId>logback-classic</artifactId>
	<version>${logback.version}</version>
  </dependency>

  <!-- If you use fluentd appender -->
  <dependency>
	<groupId>org.fluentd</groupId>
	<artifactId>fluent-logger</artifactId>
	<version>${fluentd.logger.version}</version>
  </dependency>

  <!-- If you use Amazon DynamoDB appender -->
  <dependency>
	<groupId>com.amazonaws</groupId>
	<artifactId>aws-java-sdk</artifactId>
	<version>${aws.version}</version>
  </dependency>

  <dependency>
    <groupId>com.sndyuk</groupId>
    <artifactId>logback-more-appenders</artifactId>
    <version>1.0.0</version>
  </dependency>

</dependencies>

<repositories>
  <repository>
    <id>com.sndyuk</id>
    <name>Logback more appenders</name>
    <url>http://sndyuk.github.com/maven</url>
  </repository>
</repositories>
```

### Configure your logback.xml
You can find configuration files here:
 
- [logback-appenders.xml](https://github.com/sndyuk/logback-more-appenders/blob/master/src/test/resources/logback-appenders.xml)
- [logback.xml](https://github.com/sndyuk/logback-more-appenders/blob/master/src/test/resources/logback.xml)


Creating Amazon DynamoDB Table
--------------------------------------    
If you use Amazon DynamoDB appender, You have to create table on DynamoDB:

AWS Console -> DynamoDB -> Choose region -> Create Table -> 
   
    Table Name: [Table name described logback.xml]
    Primary Key: Hash and Range
	Hash Attribute Name: String - "instance"
	Range Attribute Name: Number - "id"


License
--------------------------------------
[MIT LICENSE](LICENSE)

