FROM openjdk:21-jdk

ADD target/omni-flow-0.0.1-SNAPSHOT.jar /omni-flow.jar

ENTRYPOINT java -jar /omni-flow.jar
