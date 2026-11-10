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
import java.net.URLConnection;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * The Class MavenWrapperDownloader.
 */
public final class MavenWrapperDownloader {

    /** The wrapper version. */
    private static final String WRAPPER_VERSION = "3.3.4";

    /** Connect timeout in ms (override with MVNW_CONNECT_TIMEOUT_MS). */
    private static final int CONNECT_TIMEOUT_MS = intFromEnv("MVNW_CONNECT_TIMEOUT_MS", 10000);

    /** Read timeout in ms (override with MVNW_READ_TIMEOUT_MS). */
    private static final int READ_TIMEOUT_MS = intFromEnv("MVNW_READ_TIMEOUT_MS", 30000);

    /** In-progress temp file, cleaned up by the shutdown hook. */
    private static volatile Path currentTemp;

    /** In-progress download stream, closed by the shutdown hook. */
    private static volatile InputStream currentStream;

    /** The verbose. */
    private static final boolean VERBOSE = Boolean.parseBoolean(System.getenv("MVNW_VERBOSE"));

    /**
     * Main.
     *
     * @param args the args
     */
    public static void main(String[] args) {
        // Graceful shutdown: on SIGTERM/SIGINT release resources and remove partial downloads.
        Runtime.getRuntime().addShutdownHook(new Thread(MavenWrapperDownloader::cleanup, "mvnw-shutdown-hook"));
        final int exitCode = run(args);
        if (exitCode != 0) {
            // Single exit point; registered shutdown hook still runs.
            System.exit(exitCode);
        }
    }

    /**
     * Run the downloader.
     *
     * @param args the args
     * @return the process exit code
     */
    private static int run(String[] args) {
        log("Apache Maven Wrapper Downloader " + WRAPPER_VERSION);

        if (args.length != 2) {
            System.err.println(" - ERROR wrapperUrl or wrapperJarPath parameter missing");
            return 1;
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
            return 0;
        } catch (IOException e) {
            System.err.println("- Error downloading: " + e.getMessage());
            if (VERBOSE) {
                e.printStackTrace();
            }
            return 1;
        }
    }

    /**
     * Shutdown hook cleanup: close the open connection stream and delete any partial temp file.
     */
    private static void cleanup() {
        final InputStream in = currentStream;
        if (in != null) {
            try {
                in.close();
            } catch (IOException ignored) {
                // best effort during shutdown
            }
        }
        final Path temp = currentTemp;
        if (temp != null) {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
                // best effort during shutdown
            }
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
        Path temp = wrapperJarPath
                .getParent()
                .resolve(wrapperJarPath.getFileName() + "."
                        + UUID.randomUUID() + ".tmp");
        currentTemp = temp;
        final URLConnection connection = wrapperUrl.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        try (InputStream inStream = connection.getInputStream()) {
            currentStream = inStream;
            Files.copy(inStream, temp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temp, wrapperJarPath, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            currentStream = null;
            Files.deleteIfExists(temp);
            currentTemp = null;
        }
        log(" - Downloader complete");
    }

    /**
     * Read an int from an environment variable, falling back to a default.
     *
     * @param name the env var name
     * @param defaultValue the default value
     * @return the parsed value
     */
    private static int intFromEnv(String name, int defaultValue) {
        final String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
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
