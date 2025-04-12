*Media Upload Simulation*

This project simulates a producer-consumer system where video files are uploaded from a producer to a consumer via network communication. The consumer was initially run in a Docker container but now runs locally on your machine. The producer will continue running inside a Docker container.

*Setup and Installation*

**Prerequisites**

Before you begin, ensure you have the following installed on your machine:

- Java 11 or later
- Maven (for building and running the project)
- Docker (only for the producer container)

1. Download the Project

First, download the project files and unzip them to your preferred directory.

2. Install Maven Dependencies

Navigate to the root folder of the project where the pom.xml file is located.

cd /path/to/root-folder

Run the following Maven command to install all necessary dependencies:

mvn clean install

This will download all the required libraries and prepare the project for execution.

3. Fix Files

In the files, add a /videos and /storage folder in the root folder. This will be where the producer gets its videos (/videos) and where the consumer will store their downloaded videos (/storage)

4. Run Code 

The consumer service is a Spring Boot application that now runs locally instead of in a Docker container.

a. Navigate to the Consumer Project

cd consumer-server

b. Run the Consumer Service

To start the consumer service, use the following command:

mvn exec:java

This will run both the Consumer and Producers together at once.

You may also run this in IntelliJ by building the project with Maven then running the Main class.

*Troubleshooting*

1. Docker Container Issues  
   If you need to delete the previous producer Docker container or image before running it again, use the following commands:

   docker rm -f producer-service
   docker rmi producer-service

2. Local Consumer Not Starting  
   Make sure your local consumer server is running and listening on port 8081. You can check by navigating to http://localhost:8081 in your web browser.

*Demo*

To demonstrate the system in action, insert a video here that shows the consumer and producer running locally and exchanging data.

Video Example:
![Demo Video](video/demo.mp4)
