package com.dlsu.p3;

public class Producer {

    public static void main(String[] args) {
        // Get the environment variables for configuration
        String consumerHost = System.getenv("CONSUMER_HOST"); // Consumer IP
        int consumerPort = Integer.parseInt(System.getenv("CONSUMER_PORT")); // Consumer Port
        int producerThreads = Integer.parseInt(System.getenv("PRODUCER_THREADS")); // Number of producer threads
        int queueSize = Integer.parseInt(System.getenv("QUEUE_SIZE")); // Max queue size

        // Validate the configuration
        if (consumerHost == null || consumerHost.isEmpty()) {
            System.err.println("ERROR: CONSUMER_HOST is not set.");
            return;
        }
        if (consumerPort <= 0) {
            System.err.println("ERROR: Invalid CONSUMER_PORT.");
            return;
        }
        if (producerThreads <= 0) {
            System.err.println("ERROR: Invalid PRODUCER_THREADS.");
            return;
        }
        if (queueSize <= 0) {
            System.err.println("ERROR: Invalid QUEUE_SIZE.");
            return;
        }

        // Print out the configuration for debugging
        System.out.println("Producer Configuration:");
        System.out.println("Consumer Host: " + consumerHost);
        System.out.println("Consumer Port: " + consumerPort);
        System.out.println("Number of Producer Threads: " + producerThreads);
        System.out.println("Max Queue Size: " + queueSize);

        // Start the ProducerService to handle the video processing
        ProducerService.startProducerThreads(consumerHost, consumerPort, producerThreads, queueSize);
    }
}