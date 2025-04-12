package com.dlsu.p3;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter number of producer threads: ");
        int producerThreads = getPositiveInt(scanner);

        System.out.print("Enter number of consumer threads: ");
        int consumerThreads = getPositiveInt(scanner);

        System.out.print("Enter max queue size: ");
        int queueSize = getPositiveInt(scanner);

        int consumerPort = 8081;

        // Start consumer Java program as a subprocess
        startConsumerProgram(consumerThreads, queueSize, consumerPort);

        // Allow time for consumer to start listening
        Thread.sleep(5000);

        // Get host IP (or use default 127.0.0.1 if local)
        String consumerHost = "host.docker.internal"; // works for Docker on Mac/Windows

        // Launch producer inside Docker
        launchProducerContainer(producerThreads, queueSize, consumerHost, consumerPort);

        scanner.close();
    }


    private static void startConsumerProgram(int threads, int queueSize, int port) {
        try {
            System.out.println("Starting consumer...");

            String classpath = "./consumer-server/target/classes"; // compiled classes path
            String mainClass = "Consumer"; // exact class name in the default package

            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "-cp", classpath,
                    mainClass
            );

            // Set env vars the Consumer class relies on
            pb.environment().put("SERVER_PORT", String.valueOf(port));
            pb.environment().put("CONSUMER_THREADS", String.valueOf(threads));
            pb.environment().put("QUEUE_SIZE", String.valueOf(queueSize));
            pb.environment().put("WEB_PORT", "8082");

            pb.inheritIO(); // show output in console
            pb.start();

            System.out.println("Consumer started.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void launchProducerContainer(int producerThreads, int queueSize, String consumerHost, int consumerPort) {
        try {
            System.out.println("Building the producer Docker image...");
            String buildCommand = "docker build -t producer-service ./producer-service";
            Process buildProcess = Runtime.getRuntime().exec(buildCommand);
            buildProcess.waitFor();

            String currentDir = new File(".").getCanonicalPath();
            String videosPath = currentDir + "/videos";

            String runCommand = String.format(
                    "docker run -d --name producer-service " +
                            "-e CONSUMER_HOST=%s " +
                            "-e CONSUMER_PORT=%d " +
                            "-e PRODUCER_THREADS=%d " +
                            "-e QUEUE_SIZE=%d " +
                            "-v %s:/videos " +
                            "producer-service",
                    consumerHost, consumerPort, producerThreads, queueSize, videosPath
            );

            System.out.println("Launching the producer container...");
            Process runProcess = Runtime.getRuntime().exec(runCommand);
            runProcess.waitFor();
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
}