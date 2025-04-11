package com.dlsu.p3;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;

public class Consumer {

    // Task representing an uploaded file
    static class FileTask {
        String filename;
        byte[] data;

        FileTask(String filename, byte[] data) {
            this.filename = filename;
            this.data = data;
        }
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv("SERVER_PORT"));
        int queueSize = Integer.parseInt(System.getenv("QUEUE_SIZE"));
        int consumerThreads = Integer.parseInt(System.getenv("CONSUMER_THREADS"));

        BlockingQueue<FileTask> queue = new LinkedBlockingQueue<>(queueSize);
        ExecutorService workerPool = Executors.newFixedThreadPool(consumerThreads);

        // Start consumer workers
        for (int i = 0; i < consumerThreads; i++) {
            int id = i + 1;
            workerPool.submit(() -> {
                while (true) {
                    try {
                        FileTask task = queue.take();
                        Path outputPath = Paths.get("./storage", task.filename);
                        Files.write(outputPath, task.data);
                        System.out.println("Saved file: " + task.filename + " [by thread " + id + "]");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        // Start socket server
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Consumer listening on port " + port);
            while (true) {
                Socket client = serverSocket.accept();
                new Thread(() -> handleClient(client, queue)).start();
            }
        }
    }

    // Handles receiving a file from a producer
    private static void handleClient(Socket socket, BlockingQueue<FileTask> queue) {
        try (DataInputStream dis = new DataInputStream(socket.getInputStream())) {
            String filename = dis.readUTF();
            long fileSize = dis.readLong();

            byte[] buffer = new byte[(int) fileSize];
            dis.readFully(buffer);

            FileTask task = new FileTask(filename, buffer);
            if (!queue.offer(task)) {
                System.out.println("Queue full — dropped file: " + filename);
            } else {
                System.out.println("Queued file: " + filename);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}