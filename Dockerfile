FROM openjdk:17
COPY src/main/java/Producer.java .
COPY src/main/java/ProducerService.java .
RUN javac Producer.java
CMD ["java", "Producer"]