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

Creating Amazon DynamoDB Table
--------------------------------------    
AWS Console -> DynamoDB -> Choose region -> Create Table -> 
   
    Table Name: [Table name described logback.xml]
    Primary Key: Hash and Range
	Hash Attribute Name: String - "instance"
	Range Attribute Name: Number - "id"


Examples
--------------------------------------
You can find configuration files here:
 
- [logback-appenders.xml](https://github.com/sndyuk/logback-more-appenders/blob/master/src/test/resources/logback-appenders.xml)
- [logback.xml](https://github.com/sndyuk/logback-more-appenders/blob/master/src/test/resources/logback.xml)


	

License
--------------------------------------
[MIT LICENSE](LICENSE)

