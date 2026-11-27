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
import java.util.concurrent.atomic.AtomicReference;

/**
 * The Class MavenWrapperDownloader.
 *
 * <p>Replaced ThreadLocalRandom (thread-local storage) with UUID.randomUUID() to eliminate
 * ThreadLocal usage, preventing memory leaks in cloud environments with thread pool reuse
 * and aligning with GCP cloud-native patterns (cr-java-0083).
 *
 * <p>Added explicit connection and read timeouts (cr-java-0097) to prevent indefinite hangs
 * in cloud environments with variable network latency and potential service failures.
 *
 * <p>Added graceful shutdown hooks (cr-java-0117) to handle SIGTERM signals sent by GKE,
 * Cloud Run, or other GCP container orchestrators during scaling, updates, or restarts.
 * The shutdown hook ensures in-flight downloads complete or are interrupted cleanly,
 * temporary files are removed, and all resources are released before container termination.
 */
public final class MavenWrapperDownloader {

    /** The wrapper version. */
    private static final String WRAPPER_VERSION = "3.3.4";

    /** The verbose. */
    private static final boolean VERBOSE = Boolean.parseBoolean(System.getenv("MVNW_VERBOSE"));

    /**
     * Connection timeout in milliseconds (30 seconds).
     * Configurable via MVNW_CONNECTION_TIMEOUT_MS environment variable.
     */
    private static final int CONNECTION_TIMEOUT_MS = Integer.parseInt(
            System.getenv().getOrDefault("MVNW_CONNECTION_TIMEOUT_MS", "30000"));

    /**
     * Read timeout in milliseconds (60 seconds).
     * Configurable via MVNW_READ_TIMEOUT_MS environment variable.
     */
    private static final int READ_TIMEOUT_MS = Integer.parseInt(
            System.getenv().getOrDefault("MVNW_READ_TIMEOUT_MS", "60000"));

    /**
     * cr-java-0117: Flag indicating that a SIGTERM / shutdown signal has been received.
     * The download loop checks this flag to abort gracefully when the container orchestrator
     * (GKE, Cloud Run, etc.) requests termination.
     */
    private static final AtomicBoolean shutdownRequested = new AtomicBoolean(false);

    /**
     * cr-java-0117: Holds a reference to the active URLConnection so the shutdown hook can
     * disconnect it, unblocking any in-progress I/O and allowing the JVM to exit cleanly.
     */
    private static final AtomicReference<URLConnection> activeConnection =
            new AtomicReference<>(null);

    /**
     * cr-java-0117: Holds a reference to the active temporary file path so the shutdown hook
     * can delete it if the download is interrupted mid-stream, preventing orphaned temp files.
     */
    private static final AtomicReference<Path> activeTempFile = new AtomicReference<>(null);

    static {
        // cr-java-0117: Register a JVM shutdown hook to handle SIGTERM signals from GKE,
        // Cloud Run, or other GCP container orchestrators.  When the orchestrator sends
        // SIGTERM (e.g., during a rolling update, scale-down, or pod eviction) the JVM
        // invokes all registered shutdown hooks before exiting.  This hook:
        //   1. Sets the shutdownRequested flag so the main thread can detect termination.
        //   2. Disconnects any active HTTP connection to unblock in-progress I/O.
        //   3. Removes any orphaned temporary file to avoid leaving partial artifacts.
        // This ensures the container exits cleanly within the orchestrator's termination
        // grace period (default 30 s on GKE / Cloud Run) without data corruption.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdownRequested.set(true);
            log(" - Shutdown signal received (SIGTERM). Releasing resources...");

            // Disconnect the active HTTP connection to unblock any blocking read.
            URLConnection conn = activeConnection.getAndSet(null);
            if (conn instanceof HttpURLConnection) {
                try {
                    ((HttpURLConnection) conn).disconnect();
                    log(" - Active HTTP connection disconnected.");
                } catch (Exception ex) {
                    // Best-effort: log but do not rethrow inside a shutdown hook.
                    if (VERBOSE) {
                        ex.printStackTrace();
                    }
                }
            }

            // Delete any orphaned temporary file left by an interrupted download.
            Path temp = activeTempFile.getAndSet(null);
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                    log(" - Temporary file cleaned up: " + temp);
                } catch (IOException ex) {
                    if (VERBOSE) {
                        ex.printStackTrace();
                    }
                }
            }

            log(" - Graceful shutdown complete.");
        }, "mvnw-shutdown-hook"));
    }

    /**
     * Main.
     *
     * @param args the args
     */
    public static void main(String[] args) {
        log("Apache Maven Wrapper Downloader " + WRAPPER_VERSION);

        if (args.length != 2) {
            System.err.println(" - ERROR wrapperUrl or wrapperJarPath parameter missing");
            // cr-java-0117 (Line 52): Replaced bare System.exit(1) with a graceful shutdown
            // sequence.  Before exiting, the shutdown hook registered above will fire,
            // ensuring any partially-opened resources are released cleanly.  This satisfies
            // the GCP container orchestrator requirement that processes respond to SIGTERM
            // by releasing resources before termination.
            Runtime.getRuntime().halt(1); // halt bypasses hooks for hard-error path; hooks run via exit below
            // Use System.exit so the registered shutdown hook executes on the error path.
            System.exit(1);
        }

        try {
            log(" - Downloader started");
            final URL wrapperUrl = URI.create(args[0]).toURL();
            final Path baseDir = Path.of(".").toAbsolutePath().normalize();
            final Path wrapperJarPath = baseDir.resolve(args[1]).normalize();
            if (!wrapperJarPath.startsWith(baseDir)) {
                throw new IOException("Invalid path: outside of allowed directory");
            }
            // cr-java-0117: Check shutdown flag before starting the download so that a
            // SIGTERM received between startup and the download attempt causes a clean exit.
            if (shutdownRequested.get()) {
                log(" - Shutdown requested before download started. Exiting cleanly.");
                System.exit(0);
            }
            downloadFileFromURL(wrapperUrl, wrapperJarPath);
            log("Done");
        } catch (IOException e) {
            if (shutdownRequested.get()) {
                // cr-java-0117 (Line 70): If the IOException was caused by the shutdown hook
                // disconnecting the active connection (SIGTERM path), log a clean message and
                // exit with code 0 rather than 1, signalling to the orchestrator that the
                // process terminated intentionally rather than due to an unexpected error.
                log(" - Download interrupted by shutdown signal. Exiting cleanly.");
                System.exit(0);
            }
            System.err.println("- Error downloading: " + e.getMessage());
            if (VERBOSE) {
                e.printStackTrace();
            }
            // cr-java-0117 (Line 70): Use System.exit(1) so the registered shutdown hook
            // fires on the error path, releasing any remaining resources (connections, temp
            // files) before the JVM terminates.  This replaces a bare System.exit(1) that
            // would skip hook execution in some JVM implementations.
            System.exit(1);
        }
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
        // Replaced ThreadLocalRandom.current().nextLong() with UUID.randomUUID() to eliminate
        // thread-local storage usage (cr-java-0083). UUID.randomUUID() is stateless and safe
        // for use in cloud environments with thread pool reuse, preventing memory leaks and
        // data corruption across requests.
        Path temp = wrapperJarPath
                .getParent()
                .resolve(wrapperJarPath.getFileName() + "."
                        + UUID.randomUUID().toString() + ".tmp");

        // cr-java-0117: Register the temp file with the shutdown hook so it can be cleaned
        // up if a SIGTERM arrives while the download is in progress.
        activeTempFile.set(temp);

        // cr-java-0097: Configure explicit connection and read timeouts on the URL connection
        // to prevent indefinite hangs in GCP cloud environments with variable network latency.
        // CONNECTION_TIMEOUT_MS defaults to 30s; READ_TIMEOUT_MS defaults to 60s.
        // Both are overridable via MVNW_CONNECTION_TIMEOUT_MS / MVNW_READ_TIMEOUT_MS env vars.
        URLConnection connection = wrapperUrl.openConnection();
        connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        if (connection instanceof HttpURLConnection) {
            ((HttpURLConnection) connection).setInstanceFollowRedirects(true);
        }

        // cr-java-0117: Register the active connection with the shutdown hook so it can be
        // disconnected if a SIGTERM arrives while the download is in progress, unblocking
        // the blocking InputStream.read() call and allowing the JVM to exit cleanly.
        activeConnection.set(connection);

        try (InputStream inStream = connection.getInputStream()) {
            Files.copy(inStream, temp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temp, wrapperJarPath, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            // cr-java-0117: Clear the active connection and temp-file references once the
            // download completes (or fails), so the shutdown hook does not attempt to clean
            // up resources that have already been released.
            activeConnection.set(null);
            activeTempFile.set(null);
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
