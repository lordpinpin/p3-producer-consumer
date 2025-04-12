package com.dlsu.p3;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

import java.io.File;
import java.nio.file.Path;

public class ConsumerGUI {
    private static final String STORAGE_PATH = "/storage";

    public static void startWebServer(Path storageDir, int port) {
        // Initialize Javalin app
        Javalin app = Javalin.create(config -> {
            // Configure public static files (if any)
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";  // This is where your public files will be accessible
                staticFiles.directory = "main/java/com/dlsu/p3/public";  // Directory containing public files
                staticFiles.location = Location.CLASSPATH;  // You can adjust this if you want it from the classpath
            });

            // Configure storage static files for video serving
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/storage";  // This is where the storage files (e.g., videos) will be accessible
                staticFiles.directory = storageDir.toString();  // The directory to serve static files from
                staticFiles.location = Location.EXTERNAL;  // This is important to indicate that the files are from an external directory
            });

        }).start(port);

        System.out.println("Web server started on port " + port);

        // Additional routes for API, e.g., listing videos
        app.get("/", ctx -> {
            File[] files = storageDir.toFile().listFiles();
            ctx.json(files != null ? files : new File[0]);
        });

        app.get("/video/:filename", ctx -> {
            String filename = ctx.pathParam("filename");
            File videoFile = new File(storageDir.toFile(), filename);
            if (videoFile.exists()) {
                ctx.result(String.valueOf(videoFile));  // Return the video file
            } else {
                ctx.status(404).result("Video not found");
            }
        });

        // Log requests to the server
        app.after(ctx -> {
            System.out.println("Request: " + ctx.method() + " " + ctx.path() + " from " + ctx.ip());
        });
    }
}