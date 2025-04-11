package com.dlsu.p3;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ProducerService {

    public static void startProducerThreads(String consumerHost, int consumerPort, int producerThreads, int queueSize) {
        Path videoDir = Paths.get("/videos");

        try {
            // List all .mp4 files in the /videos directory
            List<Path> videoFiles = Files.list(videoDir)
                    .filter(path -> path.toString().endsWith(".mp4"))
                    .toList();

            // Add video files to a thread-safe queue
            BlockingQueue<Path> fileQueue = new LinkedBlockingQueue<>(videoFiles);

            // Create and start producer threads
            for (int i = 0; i < producerThreads; i++) {
                int threadId = i + 1;
                Thread producerThread = new Thread(() -> {
                    Path videoFile;
                    while ((videoFile = fileQueue.poll()) != null) {
                        sendFileToConsumer(videoFile, threadId, consumerHost, consumerPort);
                    }
                });
                producerThread.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private static void sendFileToConsumer(Path videoFile, int threadId, String consumerHost, int consumerPort) {
        try (Socket socket = new Socket(consumerHost, consumerPort)) {
            // Prepare file data
            String filename = videoFile.getFileName().toString();
            long fileLength = Files.size(videoFile);

            // Send filename and file size
            try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                 FileInputStream fileInputStream = new FileInputStream(videoFile.toFile())) {

                dos.writeUTF(filename);  // Send file name
                dos.writeLong(fileLength);  // Send file size

                // Send the actual file bytes
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }

                System.out.println("Sent file: " + filename + " from Producer " + threadId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}