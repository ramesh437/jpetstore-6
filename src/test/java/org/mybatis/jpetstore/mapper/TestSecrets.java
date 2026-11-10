/*
 *    Copyright 2010-2026 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.jpetstore.mapper;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;

import java.io.IOException;

/**
 * Resolves test credentials without hard-coding them in source.
 * <p>
 * Resolution order:
 * <ol>
 * <li>Google Secret Manager, when {@code GCP_PROJECT_ID} is set (secret id is the given name, version
 * {@code latest}).</li>
 * <li>A JVM system property / environment variable with the same name (e.g. injected from Secret Manager by the
 * runtime).</li>
 * </ol>
 */
final class TestSecrets {

  private TestSecrets() {
  }

  static String get(String name) {
    String projectId = System.getenv("GCP_PROJECT_ID");
    if (projectId != null && !projectId.isEmpty()) {
      try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
        AccessSecretVersionResponse response = client
            .accessSecretVersion(SecretVersionName.of(projectId, name, "latest"));
        return response.getPayload().getData().toStringUtf8();
      } catch (IOException e) {
        throw new IllegalStateException("Unable to read secret '" + name + "' from Google Secret Manager", e);
      }
    }
    String value = System.getProperty(name, System.getenv(name));
    if (value == null) {
      throw new IllegalStateException(
          "Secret '" + name + "' is not configured. Provide it via Google Secret Manager (GCP_PROJECT_ID) "
              + "or as a system property/environment variable.");
    }
    return value;
  }
}
