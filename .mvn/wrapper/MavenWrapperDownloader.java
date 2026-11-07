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
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The Class MavenWrapperDownloader.
 */
public final class MavenWrapperDownloader {

    /** The wrapper version. */
    private static final String WRAPPER_VERSION = "3.3.4";

    /** Default connect timeout in milliseconds. */
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 10000;

    /** Default read timeout in milliseconds. */
    private static final int DEFAULT_READ_TIMEOUT_MS = 30000;

    /** The verbose. */
    private static final boolean VERBOSE = Boolean.parseBoolean(System.getenv("MVNW_VERBOSE"));

    /** Temporary file currently being downloaded (cleaned up on shutdown/SIGTERM). */
    private static final AtomicReference<Path> CURRENT_TEMP = new AtomicReference<>();

    static {
        // Graceful shutdown hook: release partial downloads when SIGTERM/SIGINT is received.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            final Path temp = CURRENT_TEMP.getAndSet(null);
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException e) {
                    // best effort during shutdown
                }
            }
        }, "mvnw-downloader-shutdown"));
    }

    /**
     * Main.
     *
     * @param args the args
     */
    public static void main(String[] args) {
        final int exitCode = run(args);
        if (exitCode != 0) {
            // Exit status is propagated only after run() has completed and cleaned up;
            // registered shutdown hooks are executed on exit.
            Runtime.getRuntime().exit(exitCode);
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
        CURRENT_TEMP.set(temp);
        final URLConnection connection = wrapperUrl.openConnection();
        connection.setConnectTimeout(getTimeoutMillis("MVNW_CONNECT_TIMEOUT_MS", DEFAULT_CONNECT_TIMEOUT_MS));
        connection.setReadTimeout(getTimeoutMillis("MVNW_READ_TIMEOUT_MS", DEFAULT_READ_TIMEOUT_MS));
        try (InputStream inStream = connection.getInputStream()) {
            Files.copy(inStream, temp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temp, wrapperJarPath, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
            CURRENT_TEMP.compareAndSet(temp, null);
        }
        log(" - Downloader complete");
    }

    /**
     * Resolve a timeout value (milliseconds) from an environment variable.
     *
     * @param envName the environment variable name
     * @param defaultValue the default value in milliseconds
     * @return the timeout in milliseconds
     */
    private static int getTimeoutMillis(String envName, int defaultValue) {
        final String value = System.getenv(envName);
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                log(" - Invalid value for " + envName + ", using default " + defaultValue);
            }
        }
        return defaultValue;
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
