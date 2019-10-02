Logback more appenders
==================================================
is additional appenders for [Logback](http://logback.qos.ch/) and provide better performance and data consistency without any concern.

## Appenders
- [CloudWatch](https://aws.amazon.com/cloudwatch/)
    - depends on [aws-java-sdk-logs](http://aws.amazon.com/sdkforjava/).

- [Kinesis Stream](https://aws.amazon.com/kinesis/data-streams/)
    - depends on [aws-java-sdk-kinesis](http://aws.amazon.com/sdkforjava/).

- [fluentd](http://fluentd.org/)
    - depends on [fluent-logger for Java](https://github.com/fluent/fluent-logger-java).
     - Install fluentd before running logger.

- [fluency](https://github.com/komamitsu/fluency)
    - depends on [fluency](https://github.com/komamitsu/fluency).
    - Install fluentd before running logger.

### Java 9 module(Jigsaw) with old Slf4j API.
This version is a patch for applications implemented with Java9 module(Jigsaw) but using Slf4j version 1.7(which is not a java 9 module), for example Spring boot doesn't allow to use Slf4j 1.8.x(org.slf4j module).  

Just add it to your dependency then you can use the slf4j module in your application.

##### Gradle
```
  implementation 'com.sndyuk:logback-more-appenders:1.5.8-JAVA9MODULE_SLF4J17'
```

##### Maven
```
  <dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.5.8-JAVA9MODULE_SLF4J17</version>
  </dependency>
```

##### module-info.java
```
  requires org.slf4j;
```


### Latest changes

##### Version 1.5.8

* Minimum java target version to 7 from 6
* https://github.com/sndyuk/logback-more-appenders/pull/38


##### Version 1.5.6

* Fix Issue#23 https://github.com/sndyuk/logback-more-appenders/pull/37

##### Version 1.5.5

* Major update FLUENCY dependency version to 2.2.1 https://github.com/sndyuk/logback-more-appenders/pull/36

##### Version 1.5.4

* Added JSON Encoder. https://github.com/sndyuk/logback-more-appenders/blob/master/src/test/resources/logback-appenders-std.xml#L27-L40

##### Version 1.5.3

* Added KinesisStreamLogbackAppender.
* Deprecated DynamoDBLogbackAppender. Use Kinesis stream(KinesisStreamLogbackAppender) with stream and DynamoDB stream API.

##### Version 1.5.2

* Added CloudWatchLogbackAppender.

##### Version 1.5.1

* Accept dynamic `Map<String, ?>` as the additional log property. https://github.com/sndyuk/logback-more-appenders/pull/32
* Added simple marker filter for Appender. https://github.com/sndyuk/logback-more-appenders/pull/33

##### Version 1.5.0

* Use Encoder instead of Layout directly. https://github.com/sndyuk/logback-more-appenders/pull/28
* Support SSL enabled for Fluency. https://github.com/sndyuk/logback-more-appenders/pull/29


## Installing

### Install jars from Maven2 repository
Configure your pom.xml:

    <dependencies>
    
      <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>${logback.version}</version>
      </dependency>
    
      <dependency>
        <groupId>com.sndyuk</groupId>
        <artifactId>logback-more-appenders</artifactId>
        <version>1.5.8</version>
      </dependency>

      <!-- [Optional] If you use The CloudWatch appender, You need to add the dependency(aws-java-sdk-logs). -->
      <dependency>
        <groupId>com.amazonaws</groupId>
        <artifactId>aws-java-sdk-logs</artifactId>
        <version>${aws.version}</version>
      </dependency>

      <!-- [Optional] If you use The Kinesis appender, You need to add the dependency(aws-java-sdk-kinesis). -->
      <dependency>
        <groupId>com.amazonaws</groupId>
        <artifactId>aws-java-sdk-kinesis</artifactId>
        <version>${aws.version}</version>
      </dependency>

      <!-- [Optional] If you use The Fluentd appender, You need to add the dependency(fluent-logger). -->
      <dependency>
        <groupId>org.fluentd</groupId>
        <artifactId>fluent-logger</artifactId>
        <version>${fluentd.logger.version}</version>
      </dependency>
    
      <!-- [Optional] If you use The Fluency appender, You need to add the dependency(fluency). -->
      <dependency>
        <groupId>org.komamitsu</groupId>
        <artifactId>fluency-core</artifactId>
        <version>${fluency.version}</version>
        <optional>true</optional>
      </dependency>
      <dependency>
        <groupId>org.komamitsu</groupId>
        <artifactId>fluency-fluentd</artifactId>
        <version>${fluency.version}</version>
        <optional>true</optional>
      </dependency>
    
    </dependencies>

### Configure your logback.xml
You can find the sample configuration files here:

- [AWS services - logback-appenders-aws.xml](https://github.com/sndyuk/logback-more-appenders/blob/master/src/test/resources/logback-appenders-aws.xml)
- [Fluentd - logback-appenders-fluentd.xml](https://github.com/sndyuk/logback-more-appenders/blob/master/src/test/resources/logback-appenders-fluentd.xml)
- [Stdout/err - logback-appenders-std.xml](https://github.com/sndyuk/logback-more-appenders/blob/master/src/test/resources/logback-appenders-std.xml)

- [logback.xml](https://github.com/sndyuk/logback-more-appenders/blob/master/src/test/resources/logback.xml)

### License
[Apache License, Version 2.0](LICENSE)
