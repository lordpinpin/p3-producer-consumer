FROM openjdk:17

# Copy the entire package structure into the container
COPY src/main/java/com/dlsu/p3/ /app/com/dlsu/p3/

# Set the working directory to the root of the copied code
WORKDIR /app

# Compile the Producer and ProducerService Java files
RUN javac com/dlsu/p3/Producer.java com/dlsu/p3/ProducerService.java

# Run the Producer class using its fully qualified name
CMD ["java", "com.dlsu.p3.Producer"]