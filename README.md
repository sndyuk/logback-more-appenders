Logback more appenders
==================================================
is additional appenders for [Logback](http://logback.qos.ch/) and provide better performance and data consistency without any concern.

## Appenders

- [CloudWatch](https://aws.amazon.com/cloudwatch/)
    - depends on [cloudwatchlogs(v2) or aws-java-sdk-logs(v1)](http://aws.amazon.com/sdkforjava/).

- [Kinesis Stream](https://aws.amazon.com/kinesis/data-streams/)
    - depends on [kinesis(v2) or aws-java-sdk-kinesis(v1)](http://aws.amazon.com/sdkforjava/).

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
  implementation 'com.sndyuk:logback-more-appenders:1.8.6-JAVA9MODULE_SLF4J17'
```

##### Maven
```
  <dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.8.7-JAVA9MODULE_SLF4J17</version>
  </dependency>
```

##### module-info.java
```
  requires org.slf4j;
```


### Latest changes

##### Version 1.8.7
* Add `ignoredField` option: https://github.com/sndyuk/logback-more-appenders/pull/65

##### Version 1.8.6
* Add AWS V2 CloudWatch and Kinesis appenders: https://github.com/sndyuk/logback-more-appenders/pull/64

##### Version 1.8.5
* Add option for Fluency appender: `jvmHeapBufferMode`

##### Version 1.8.4

* Avoid CloudWatch request limit error
* Fix issue: https://github.com/sndyuk/logback-more-appenders/issues/60

##### Version 1.8.3

* Make marker prefix customizable by `markerPrefix` option. https://github.com/sndyuk/logback-more-appenders/issues/51
* Fix issue: Error creating cloud watch appender with version 1.8.2-JAVA9MODULE_SLF4J17 and 1.8.1-JAVA9MODULE_SLF4J17. https://github.com/sndyuk/logback-more-appenders/issues/60

##### Version 1.8.2

* Fix issue at FastJsonEncoder.

##### Version 1.8.1

* Add options for Fluent / Fluency appenders:
  * "encoder" option becomes optional. If you don't set encoder, the log message outputs pure message. 
  * "messageFieldKeyName" option has been added: Set it if you want to change the default message field key name(= "message").
* Add options for Fluency appender: `bufferChunkRetentionTimeMillis`
* Fix bug: Messages were lost sometimes when the messages had been waiting for flushing on shutting down an application.  
* Fix bug: Fluent appender: Do not raise an error when a fluentd server is down.

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
        <version>1.8.7</version>
      </dependency>

      <!-- [Optional] If you use The CloudWatch V2 appender, You need to add the dependency(cloudwatchlogs). -->
      <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>cloudwatchlogs</artifactId>
        <version>${aws-v2.version}</version>
      </dependency>

      <!-- [Optional] If you use The CloudWatch appender, You need to add the dependency(aws-java-sdk-logs). -->
      <dependency>
        <groupId>com.amazonaws</groupId>
        <artifactId>aws-java-sdk-logs</artifactId>
        <version>${aws.version}</version>
      </dependency>

      <!-- [Optional] If you use The Kinesis V2 appender, You need to add the dependency(kinesis). -->
      <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>kinesis</artifactId>
        <version>${aws-v2.version}</version>
        <optional>true</optional>
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

### 


### License
[Apache License, Version 2.0](LICENSE)
