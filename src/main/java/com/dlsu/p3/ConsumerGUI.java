package com.dlsu.p3;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;

public class ConsumerGUI {
    public static void startWebServer(Path storageDir, int port) {
        Javalin app = Javalin.create(config -> {
            // Serve static frontend files (HTML, JS, etc.)
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "src/main/java/com/dlsu/p3/public"; // Moved to proper resource path
                staticFiles.location = Location.EXTERNAL;
            });

            // Serve uploaded videos
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/storage";
                staticFiles.directory = storageDir.toString();
                staticFiles.location = Location.EXTERNAL;
            });
        }).start(port);

        System.out.println("Web server started on port " + port);

        // Endpoint for video list
        app.get("/api/videos", ctx -> {
            File[] files = storageDir.toFile().listFiles((dir, name) -> name.endsWith(".mp4"));
            ctx.json(files != null ? Arrays.stream(files).map(File::getName).toArray() : new String[0]);
        });

        app.after(ctx -> {
            System.out.println("Request: " + ctx.method() + " " + ctx.path() + " from " + ctx.ip());
        });
    }
}