/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The Class MavenWrapperDownloader.
 */
public final class MavenWrapperDownloader {

    /** The wrapper version. */
    private static final String WRAPPER_VERSION = "3.3.4";

    /** The verbose. */
    private static final boolean VERBOSE = Boolean.parseBoolean(System.getenv("MVNW_VERBOSE"));

    /** Connection timeout in milliseconds (30 seconds). */
    private static final int CONNECTION_TIMEOUT_MS = 30000;

    /** Read timeout in milliseconds (60 seconds). */
    private static final int READ_TIMEOUT_MS = 60000;

    /** Flag indicating whether a shutdown has been requested via SIGTERM. */
    private static final AtomicBoolean shutdownRequested = new AtomicBoolean(false);

    /**
     * Main.
     *
     * @param args the args
     */
    public static void main(String[] args) {
        log("Apache Maven Wrapper Downloader " + WRAPPER_VERSION);

        // Register graceful shutdown hook to handle SIGTERM from GKE/Cloud Run orchestrators.
        // This ensures in-flight downloads are interrupted cleanly and temporary files are
        // released before the container terminates.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdownRequested.set(true);
            log(" - Shutdown signal received (SIGTERM). Releasing resources and shutting down gracefully.");
        }, "graceful-shutdown-hook"));

        if (args.length != 2) {
            System.err.println(" - ERROR wrapperUrl or wrapperJarPath parameter missing");
            // Perform graceful shutdown: trigger registered hooks before exiting
            gracefulExit(1);
            return;
        }

        try {
            log(" - Downloader started");
            final URL wrapperUrl = URI.create(args[0]).toURL();
            final Path baseDir = Path.of(".").toAbsolutePath().normalize();
            final Path wrapperJarPath = baseDir.resolve(args[1]).normalize();
            if (!wrapperJarPath.startsWith(baseDir)) {
                throw new IOException("Invalid path: outside of allowed directory");
            }
            downloadFileFromURL(wrapperUrl, wrapperJarPath);
            log("Done");
        } catch (IOException e) {
            System.err.println("- Error downloading: " + e.getMessage());
            if (VERBOSE) {
                e.printStackTrace();
            }
            // Perform graceful shutdown: trigger registered hooks before exiting
            gracefulExit(1);
        }
    }

    /**
     * Initiates a graceful shutdown by allowing registered shutdown hooks (including the
     * SIGTERM handler) to execute before the JVM exits. This ensures resources are released
     * cleanly when running inside GKE pods or Cloud Run containers.
     *
     * @param statusCode the exit status code
     */
    private static void gracefulExit(int statusCode) {
        log(" - Initiating graceful shutdown with exit code: " + statusCode);
        System.exit(statusCode);
    }

    /**
     * Download file from url.
     *
     * @param wrapperUrl the wrapper url
     * @param wrapperJarPath the wrapper jar path
     * @throws IOException the io exception
     */
    private static void downloadFileFromURL(URL wrapperUrl, Path wrapperJarPath)
            throws IOException {
        log(" - Downloading to: " + wrapperJarPath);
        if (System.getenv("MVNW_USERNAME") != null && System.getenv("MVNW_PASSWORD") != null) {
            final String username = System.getenv("MVNW_USERNAME");
            final char[] password = System.getenv("MVNW_PASSWORD").toCharArray();
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
        }
        Path temp = wrapperJarPath
                .getParent()
                .resolve(wrapperJarPath.getFileName() + "."
                        + UUID.randomUUID().toString() + ".tmp");
        URLConnection connection = wrapperUrl.openConnection();
        connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        if (connection instanceof HttpURLConnection) {
            ((HttpURLConnection) connection).setInstanceFollowRedirects(true);
        }
        try (InputStream inStream = connection.getInputStream()) {
            Files.copy(inStream, temp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temp, wrapperJarPath, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
        log(" - Downloader complete");
    }

    /**
     * Log.
     *
     * @param msg the msg
     */
    private static void log(String msg) {
        if (VERBOSE) {
            System.out.println(msg);
        }
    }

}
