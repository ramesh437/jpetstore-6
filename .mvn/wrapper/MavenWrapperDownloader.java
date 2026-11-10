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
import java.net.URL;
import java.net.URLConnection;
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

    /** The verbose. */
    private static final boolean VERBOSE = Boolean.parseBoolean(System.getenv("MVNW_VERBOSE"));

    /** The connect timeout in milliseconds (override with MVNW_CONNECT_TIMEOUT_MS). */
    private static final int CONNECT_TIMEOUT_MS = getTimeout("MVNW_CONNECT_TIMEOUT_MS", 10000);

    /** The read timeout in milliseconds (override with MVNW_READ_TIMEOUT_MS). */
    private static final int READ_TIMEOUT_MS = getTimeout("MVNW_READ_TIMEOUT_MS", 30000);

    /** In-progress temp file, cleaned up by the shutdown hook on SIGTERM/SIGINT or exit. */
    private static volatile Path currentTempFile;

    /** Registers a shutdown hook that releases resources (partial downloads) on termination signals. */
    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            final Path temp = currentTempFile;
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException e) {
                    // best effort cleanup during shutdown
                }
            }
        }, "mvnw-downloader-shutdown-hook"));
    }

    private static int getTimeout(String envName, int defaultValue) {
        final String value = System.getenv(envName);
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                // fall through to default
            }
        }
        return defaultValue;
    }

    /**
     * Main.
     *
     * @param args the args
     */
    public static void main(String[] args) {
        registerShutdownHook();
        log("Apache Maven Wrapper Downloader " + WRAPPER_VERSION);

        if (args.length != 2) {
            System.err.println(" - ERROR wrapperUrl or wrapperJarPath parameter missing");
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
            downloadFileFromURL(wrapperUrl, wrapperJarPath);
            log("Done");
        } catch (IOException e) {
            System.err.println("- Error downloading: " + e.getMessage());
            if (VERBOSE) {
                e.printStackTrace();
            }
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
        Path temp = wrapperJarPath
                .getParent()
                .resolve(wrapperJarPath.getFileName() + "."
                        + UUID.randomUUID() + ".tmp");
        currentTempFile = temp;
        final URLConnection connection = wrapperUrl.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        try (InputStream inStream = connection.getInputStream()) {
            Files.copy(inStream, temp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temp, wrapperJarPath, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
            currentTempFile = null;
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
