package com.dlsu.p3;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        // Define initial ports for consumer and web server
        int consumerPort = 8081;
        int webServerPort = 8082; // Default web server port

        // Ensure unique and available ports for consumer and web server
        consumerPort = getAvailablePort(consumerPort, webServerPort);
        webServerPort = getAvailablePort(webServerPort, consumerPort);

        // Launch consumer program on the available consumer port
        startConsumerProgram(consumerThreads, queueSize, consumerPort);

        // Launch web server on the available web server port
        startWebServer(webServerPort);

        // Allow time for consumer to start listening
        waitForConsumer("localhost", consumerPort);

        // Launch producer (now as part of the same module)
        launchProducerContainer(producerThreads, queueSize, "host.docker.internal", consumerPort);

        scanner.close();
    }

    private static void startConsumerProgram(int threads, int queueSize, int port) {
        // Assuming Consumer class is in the same module
        Thread consumerThread = new Thread(() -> {
            try {
                Consumer.main(new String[]{String.valueOf(threads), String.valueOf(queueSize), String.valueOf(port)});
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        consumerThread.start();
        System.out.println("Consumer started on port " + port + ".");
    }

    private static void startWebServer(int port) {
        Thread webServerThread = new Thread(() -> {
            try {
                Path storagePath = Paths.get("storage");
                Files.createDirectories(storagePath);  // Ensure directory exists
                ConsumerGUI.startWebServer(storagePath, port);  // Call your GUI code
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "WebServer");
        webServerThread.start();
    }

    private static void launchProducerContainer(int producerThreads, int queueSize, String consumerHost, int consumerPort) {
        try {
            String dockerContext = new File(".").getCanonicalPath(); // Adjust if Dockerfile is in another folder
            String buildCommand = String.format("docker build -t producer-service %s", dockerContext);

            System.out.println("Building the producer Docker image...");
            Process buildProcess = Runtime.getRuntime().exec(buildCommand);
            int buildExitCode = buildProcess.waitFor();
            if (buildExitCode != 0) {
                System.err.println("Docker build failed.");
                return;
            }

            // Change the path format for compatibility across systems
            String videosPath = dockerContext + "/videos";

            // Create the run command as an array
            String[] runCommand = {
                    "docker", "run", "--name", "producer-service",
                    "-e", "CONSUMER_HOST=" + consumerHost,
                    "-e", "CONSUMER_PORT=" + consumerPort,
                    "-e", "PRODUCER_THREADS=" + producerThreads,
                    "-e", "QUEUE_SIZE=" + queueSize,
                    "-v", videosPath.replace("\\", "/") + ":/videos", // Replace backslashes with forward slashes for compatibility
                    "producer-service"
            };

            System.out.println("Launching the producer container...");
            Process runProcess = Runtime.getRuntime().exec(runCommand);
            int runExitCode = runProcess.waitFor();
            if (runExitCode != 0) {
                System.err.println("Docker run failed.");
            } else {
                System.out.println("Producer container launched successfully!");
            }
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

    // Utility method for consumer readiness check
    private static boolean isConsumerReady(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000); // 2 seconds timeout
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void waitForConsumer(String consumerHost, int consumerPort) {
        while (!isConsumerReady(consumerHost, consumerPort)) {
            System.out.println("Waiting for consumer to be ready...");
            try {
                Thread.sleep(1000); // Check every second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("Consumer is ready.");
    }

    // Method to find an available port starting from the given one
    private static int getAvailablePort(int startingPort, int conflictingPort) {
        int port = startingPort;
        while (!isPortAvailable(port) || port == conflictingPort) {
            System.out.println("Port " + port + " is in use or conflicting with another service. Trying next port...");
            port++;
        }
        return port;
    }

    private static boolean isPortAvailable(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 2000); // 2 seconds timeout
            return false; // Port is in use
        } catch (IOException e) {
            return true; // Port is free
        }
    }
}