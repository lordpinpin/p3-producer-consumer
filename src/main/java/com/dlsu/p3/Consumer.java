package com.dlsu.p3;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Consumer {
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8081"));
    private static final int QUEUE_SIZE = Integer.parseInt(System.getenv().getOrDefault("QUEUE_SIZE", "10"));
    private static final int THREADS = Integer.parseInt(System.getenv().getOrDefault("CONSUMER_THREADS", "2"));
    private static final int WEB_PORT = Integer.parseInt(System.getenv().getOrDefault("WEB_PORT", "8082"));
    private static final Path STORAGE_DIR = Paths.get("storage");

    public static void main(String[] args) throws IOException {
        Files.createDirectories(STORAGE_DIR);

        BlockingQueue<File> queue = new ArrayBlockingQueue<>(QUEUE_SIZE);
        ExecutorService consumers = Executors.newFixedThreadPool(THREADS);

        for (int i = 0; i < THREADS; i++) {
            int id = i;
            consumers.submit(() -> {
                while (true) {
                    try {
                        File file = queue.take();
                        System.out.println("Processed: " + file.getName() + " [Thread " + id + "]");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }

        // Socket server to receive files from producer
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("Consumer listening on port " + PORT + " (host IP)");
                while (true) {
                    Socket socket = serverSocket.accept();
                    handleUpload(socket, queue);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        // Web GUI server (optional)
        new Thread(() -> {
            try {
                System.out.println("Starting web server on port " + WEB_PORT + "...");
                ConsumerGUI.startWebServer(STORAGE_DIR, WEB_PORT);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void handleUpload(Socket socket, BlockingQueue<File> queue) {
        new Thread(() -> {
            try (DataInputStream dis = new DataInputStream(socket.getInputStream())) {
                String filename = dis.readUTF();
                long length = dis.readLong();

                File outFile = STORAGE_DIR.resolve(filename).toFile();
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[4096];
                    int read;
                    while (length > 0 && (read = dis.read(buffer, 0, (int) Math.min(buffer.length, length))) != -1) {
                        fos.write(buffer, 0, read);
                        length -= read;
                    }
                }

                System.out.println("Saved file: " + filename);
                queue.put(outFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}