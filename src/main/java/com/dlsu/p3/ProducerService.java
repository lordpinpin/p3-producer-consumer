package com.dlsu.p3;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ProducerService {

    public static void startProducerThreads(String consumerHost, int consumerPort, int producerThreads, int queueSize) {
        // Path to the video input directory
        Path videoDir = Paths.get("/video-inputs");

        try {
            // Get a list of subfolders (folders like folder1, folder2, etc.)
            List<Path> subfolders = Files.list(videoDir)
                    .filter(Files::isDirectory)
                    .toList();

            // Limit the number of subfolders to the number of producer threads
            int threadCount = Math.min(producerThreads, subfolders.size());

            // Create a thread for each subfolder
            for (int i = 0; i < threadCount; i++) {
                int threadId = i + 1;
                Path folderPath = subfolders.get(i); // Access a different subfolder for each thread

                // Initialize a new thread for each subfolder
                Thread producerThread = new Thread(() -> processFolder(folderPath, threadId, consumerHost, consumerPort));
                producerThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processFolder(Path folderPath, int threadId, String consumerHost, int consumerPort) {
        try {
            // List all video files in the folder (you can filter by file extension like .mp4)
            List<Path> videoFiles = Files.list(folderPath)
                    .filter(path -> path.toString().endsWith(".mp4"))
                    .toList();

            System.out.println("Producer " + threadId + " processing folder: " + folderPath);

            // Process each video file in the subfolder
            for (Path videoFile : videoFiles) {
                sendFileToConsumer(videoFile, threadId, consumerHost, consumerPort);
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