package com.dlsu.p3;

import java.io.IOException;
import java.util.Scanner;


public class Main {

    public static void main(String[] args) {
        // Get user inputs for configuration
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter number of producer threads: ");
        int producerThreads = getPositiveInt(scanner);

        System.out.print("Enter number of consumer threads: ");
        int consumerThreads = getPositiveInt(scanner);

        System.out.print("Enter max queue size: ");
        int queueSize = getPositiveInt(scanner);

        // IP addresses of the VMs (or Docker containers)
        String consumerIp = "192.168.1.2"; // Update with the actual IP of the consumer VM/container
        int consumerPort = 8081;

        // Launch the Producer and Consumer Docker containers based on the inputs
        launchProducerContainer(producerThreads, queueSize, consumerIp, consumerPort);
        launchConsumerContainer(consumerThreads, queueSize, consumerPort);

        scanner.close();
    }

    // Method to launch Producer container with configuration
    private static void launchProducerContainer(int producerThreads, int queueSize, String consumerIp, int consumerPort) {
        try {
            // Build the Docker image for the producer
            System.out.println("Building the producer Docker image...");
            String buildCommand = "docker build -t producer-service ./producer-service";
            Process buildProcess = Runtime.getRuntime().exec(buildCommand);
            buildProcess.waitFor();  // Wait for the build to complete

            // Mount the video-inputs directory from the host machine to the container
            String runCommand = String.format("docker run -d --name producer-service " +
                            "-e CONSUMER_HOST=%s " +
                            "-e CONSUMER_PORT=%d " +
                            "-e PRODUCER_THREADS=%d " +
                            "-e QUEUE_SIZE=%d " +
                            "-v %s:/video-inputs " + // Mount the directory to the container
                            "producer-service",
                    consumerIp, consumerPort, producerThreads, queueSize,
                    "/producer/src/main/resources/video-inputs"  // Full path on the host machine to video folders
            );

            // Run the producer container
            System.out.println("Launching the producer container...");
            Process runProcess = Runtime.getRuntime().exec(runCommand);
            runProcess.waitFor();  // Wait for the producer container to start
            System.out.println("Producer container launched successfully!");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static int getPositiveInt(Scanner scanner) {
        while (true) {
            try {
                int value = Integer.parseInt(scanner.nextLine());
                if (value > 0) return value;
                System.out.print("Please enter a positive number: ");
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Please enter a number: ");
            }
        }
    }

    private static void launchConsumerContainer(int consumerThreads, int queueSize, int consumerPort) {
        try {
            System.out.println("Building the consumer webapp Docker image...");
            String buildCommand = "docker build -t consumer-webapp ./consumer-webapp";
            Process buildProcess = Runtime.getRuntime().exec(buildCommand);
            buildProcess.waitFor();

            System.out.println("Launching the consumer webapp container...");
            String runCommand = String.format(
                    "docker run -d --name consumer-webapp " +
                            "-e SERVER_PORT=%d " +
                            "-e CONSUMER_THREADS=%d " +
                            "-e QUEUE_SIZE=%d " +
                            "-p %d:%d " +
                            "-v ./consumer-webapp/storage:/storage " +
                            "consumer-webapp",
                    consumerPort, consumerThreads, queueSize, consumerPort, consumerPort
            );
            Process runProcess = Runtime.getRuntime().exec(runCommand);
            runProcess.waitFor();
            System.out.println("Consumer webapp container launched!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}