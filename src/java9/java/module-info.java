open module com.sndyuk.logback.more.appenders {
  requires org.slf4j;
  requires static logback.core;
  requires static logback.classic;
  requires static logback.access;
  requires static fluent.logger;
  requires static fluency.core;
  requires static fluency.fluentd;
  requires static aws.java.sdk.core;
  requires static aws.java.sdk.logs;
  requires static aws.java.sdk.kinesis;
  requires static software.amazon.awssdk.core;
  requires static software.amazon.awssdk.auth;
  requires static software.amazon.awssdk.regions;
  requires static software.amazon.awssdk.services.kinesis;
  requires static software.amazon.awssdk.services.cloudwatchlogs;
}
