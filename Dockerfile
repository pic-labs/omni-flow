FROM openjdk:21-jdk

ADD target/create-controller-0.0.1-SNAPSHOT.jar /create-controller.jar

ENTRYPOINT java -jar /create-controller.jar
