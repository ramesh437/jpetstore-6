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
package org.mybatis.jpetstore.web.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.util.UriUtils;

/**
 * JSPs cannot be compiled directly from inside an executable JAR. The web content packaged under {@code /webapp} in
 * the JAR (JSPs, css, images) is therefore extracted at startup into a temporary directory which is used as the
 * embedded Tomcat document root. The directory is removed on shutdown.
 */
@Component
public class WebappDocumentRootCustomizer implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

  private static final String WEBAPP_ROOT = "classpath:/webapp/";

  @Override
  public void customize(ConfigurableServletWebServerFactory factory) {
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    try {
      Resource root = resolver.getResource(WEBAPP_ROOT);
      if (!root.exists()) {
        return;
      }
      String rootUrl = root.getURL().toString();
      if (!rootUrl.endsWith("/")) {
        rootUrl = rootUrl + "/";
      }
      Path target = Files.createTempDirectory("jpetstore-webapp");
      Runtime.getRuntime().addShutdownHook(new Thread(() -> FileSystemUtils.deleteRecursively(target.toFile())));
      for (Resource resource : resolver.getResources(WEBAPP_ROOT + "**")) {
        String url = resource.getURL().toString();
        if (url.endsWith("/") || !url.startsWith(rootUrl)) {
          continue;
        }
        Path file = target.resolve(UriUtils.decode(url.substring(rootUrl.length()), StandardCharsets.UTF_8));
        Files.createDirectories(file.getParent());
        try (InputStream in = resource.getInputStream()) {
          Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
        }
      }
      factory.setDocumentRoot(target.toFile());
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to extract web content for the embedded servlet container", e);
    }
  }
}
