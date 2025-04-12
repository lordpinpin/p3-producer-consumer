package com.dlsu.p3;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.concurrent.*;

public class Consumer {

    private static final Set<String> knownHashes = ConcurrentHashMap.newKeySet();
    private static BlockingQueue<Socket> uploadQueue;

    public static void main(String[] args) throws IOException {
        // Read from ENV or args
        int consumerThreads = Integer.parseInt(System.getenv().getOrDefault("CONSUMER_THREADS", args.length > 0 ? args[0] : "2"));
        int queueSize = Integer.parseInt(System.getenv().getOrDefault("QUEUE_SIZE", args.length > 1 ? args[1] : "10"));
        int port = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", args.length > 2 ? args[2] : "8081"));

        uploadQueue = new ArrayBlockingQueue<>(queueSize);
        Path uploads = Paths.get("storage");
        initializeKnownHashes(uploads);
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Consumer listening on port " + port);

        // Start worker threads
        for (int i = 0; i < consumerThreads; i++) {
            new Thread(() -> {
                while (true) {
                    try {
                        Socket socket = uploadQueue.take();
                        processUpload(socket);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, "Consumer-Worker-" + i).start();
        }

        // Main accept loop
        while (true) {
            Socket clientSocket = serverSocket.accept();
            handleUpload(clientSocket);
        }
    }

    private static void handleUpload(Socket clientSocket) {
        if (!uploadQueue.offer(clientSocket)) {
            System.out.println("Queue full. Dropping: " + clientSocket.getRemoteSocketAddress());
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
        } else {
            System.out.println("Queued: " + clientSocket.getRemoteSocketAddress());
        }
    }

    private static void processUpload(Socket socket) {
        try (DataInputStream dis = new DataInputStream(socket.getInputStream())) {
            String filename = dis.readUTF();
            long fileSize = dis.readLong();

            Path uploads = Paths.get("storage");
            Files.createDirectories(uploads);

            // Save to a unique temp file
            Path tempFile = Files.createTempFile("upload_", ".tmp");

            try (OutputStream os = new FileOutputStream(tempFile.toFile())) {
                byte[] buffer = new byte[4096];
                long remaining = fileSize;
                int read;
                while (remaining > 0 &&
                        (read = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                    os.write(buffer, 0, read);
                    remaining -= read;
                }
            }

            // Compute hash from the temp file
            String fileHash = computeSHA256(tempFile.toFile());

            if (knownHashes.contains(fileHash)) {
                System.out.println("Duplicate video detected: " + filename);
                Files.deleteIfExists(tempFile);
            } else {
                knownHashes.add(fileHash);

                Path finalPath = uploads.resolve(filename);

                // Avoid overwriting a different file with the same name
                if (Files.exists(finalPath)) {
                    // Rename the new file to avoid conflict (e.g., add timestamp)
                    String baseName = filename.contains(".") ?
                            filename.substring(0, filename.lastIndexOf('.')) : filename;
                    String extension = filename.contains(".") ?
                            filename.substring(filename.lastIndexOf('.')) : "";
                    String uniqueFilename = baseName + "_" + System.currentTimeMillis() + extension;
                    finalPath = uploads.resolve(uniqueFilename);
                    System.out.println("Filename conflict. Saved as: " + uniqueFilename);
                }

                Files.move(tempFile, finalPath);
                System.out.println(Thread.currentThread().getName() + " received: " + finalPath.getFileName());
            }

        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Upload error: " + e.getMessage());
        }
    }

    private static String computeSHA256(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream is = new FileInputStream(file);
             DigestInputStream dis = new DigestInputStream(is, digest)) {
            byte[] buffer = new byte[4096];
            while (dis.read(buffer) != -1) {
                // Reading the file to update the digest
            }
        }
        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static void initializeKnownHashes(Path storageDir) {
        try {
            Files.createDirectories(storageDir); // ensure dir exists
            Files.walk(storageDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            String hash = computeSHA256(path.toFile());
                            knownHashes.add(hash);
                        } catch (IOException | NoSuchAlgorithmException e) {
                            System.err.println("Failed to hash " + path + ": " + e.getMessage());
                        }
                    });
            System.out.println("Initialized hash list with " + knownHashes.size() + " existing files.");
        } catch (IOException e) {
            System.err.println("Failed to scan storage directory: " + e.getMessage());
        }
    }
}