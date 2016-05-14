Logback more appenders
==================================================
is appenders for [Logback](http://logback.qos.ch/).
You can logging to DynamoDB, Fluentd and more with the logback appender.

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

  <dependency>
    <groupId>com.sndyuk</groupId>
    <artifactId>logback-more-appenders</artifactId>
    <version>1.2.0</version>
  </dependency>

  <!-- If you use The Fluentd appender, You need to add the dependency(fluent-logger). -->
  <dependency>
	<groupId>org.fluentd</groupId>
	<artifactId>fluent-logger</artifactId>
	<version>${fluentd.logger.version}</version>
  </dependency>

  <!-- If you use The Amazon DynamoDB appender, You need to add the dependency(aws-java-sdk). -->
  <dependency>
	<groupId>com.amazonaws</groupId>
	<artifactId>aws-java-sdk</artifactId>
	<version>${aws.version}</version>
  </dependency>

</dependencies>
```

### Configure your logback.xml
You can find configuration files example here:

- [logback-appenders.xml](https://github.com/sndyuk/logback-more-appenders/blob/master/src/test/resources/logback-appenders.xml)
- [logback.xml](https://github.com/sndyuk/logback-more-appenders/blob/master/src/test/resources/logback.xml)


Creating Amazon DynamoDB Table
--------------------------------------
Before you use The Amazon DynamoDB appender, you need to create the table on DynamoDB:

AWS Console -> DynamoDB -> Choose region -> Create Table -> 

    Table Name: [Table name described in logback.xml]
    Partition key: "instance" as String / (Hash Attribute)
    Add sort key: "id" as Number / (Range Attribute)


License
--------------------------------------
[Apache License, Version 2.0](LICENSE)
