open module org.slf4j {
  requires static slf4j.api;
  requires static logback.core;
  requires static logback.classic;
  requires static logback.access;
  requires static fluent.logger;
  requires static fluency.core;
  requires static fluency.fluentd;
  requires static aws.java.sdk.core;
  requires static aws.java.sdk.logs;
  requires static aws.java.sdk.kinesis;
}
