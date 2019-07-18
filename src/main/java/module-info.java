module org.slf4j {
  exports ch.qos.logback.more.appenders.filter;
  exports ch.qos.logback.more.appenders.encoder;
  exports ch.qos.logback.more.appenders.marker;
  exports ch.qos.logback.more.appenders;

  requires static aws.java.sdk.core;
  requires static aws.java.sdk.dynamodb;
  requires static aws.java.sdk.kinesis;
  requires static aws.java.sdk.logs;
  requires static fluency.core;
  requires static fluency.fluentd;
  requires static fluent.logger;
  requires static logback.classic;
  requires static logback.core;
  requires static slf4j.api;
}
