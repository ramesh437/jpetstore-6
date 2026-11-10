MyBatis JPetStore
=================

[![Java CI](https://github.com/mybatis/jpetstore-6/actions/workflows/ci.yaml/badge.svg)](https://github.com/mybatis/jpetstore-6/actions/workflows/ci.yaml)
[![Container Support](https://github.com/mybatis/jpetstore-6/actions/workflows/support.yaml/badge.svg)](https://github.com/mybatis/jpetstore-6/actions/workflows/support.yaml)
[![Coverage Status](https://coveralls.io/repos/github/mybatis/jpetstore-6/badge.svg?branch=master)](https://coveralls.io/github/mybatis/jpetstore-6?branch=master)
[![Docs](https://img.shields.io/badge/docs-mybatis.org-blue?logo=github)](https://mybatis.org/jpetstore-6/)
[![License](https://img.shields.io/:license-apache-brightgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

![mybatis-jpetstore](https://mybatis.org/images/mybatis-logo.png)

JPetStore 6 is a full web application built on top of MyBatis 3, Spring 5 and Stripes.

## Other versions that you may want to know about

- JPetstore on top of Spring, Spring MVC, MyBatis 3, and Spring Security https://github.com/making/spring-jpetstore
- JPetstore with Vaadin and Spring Boot with Java Config https://github.com/igor-baiborodine/jpetstore-6-vaadin-spring-boot
- JPetstore on MyBatis Spring Boot Starter https://github.com/kazuki43zoo/mybatis-spring-boot-jpetstore

## Run on Application Server
JPetStore is packaged as a self-contained executable JAR with an embedded Tomcat (no external application server needed; suitable for GKE, Cloud Run and App Engine flexible environment).

- Clone this repository

  ```
  $ git clone https://github.com/mybatis/jpetstore-6.git
  ```

- Build executable jar file

  ```
  $ cd jpetstore-6
  $ ./mvnw clean package
  ```

- Startup the embedded Tomcat server (`PORT` and `CONTEXT_PATH` environment variables configure the port (default 8080) and the context path (default root))

  ```
  $ java -jar target/jpetstore.jar
  ```

  > Note:
  >
  > We provide maven profiles per application server as follow:
  >
  > | Profile             | Description                         |
  > | ------------------- | ----------------------------------- |
  > | tomcat9 (preferred) | Running under the Tomcat 9.0        |
  > | glassfish5          | Running under the GlassFish 5       |
  > | jetty12-ee8         | Running under the Jetty 12          |
  > | liberty26-ee8       | Running under the WebSphere Liberty |
  > | payara5             | Running under the Payara 5          |
  > | resin4              | Running under the Resin 4           |
  > | tomee8              | Running under the TomEE 8.0         |
  > | wildfly26           | Running under the WildFly 26        |

- Run application in browser http://localhost:8080/
- Press Ctrl-C to stop the server.

- When using 'glassfish5', it must run under java 8 as it does not work with newer versions.  The code is set to 'D:/jdk/jdk-1.8.0.501'.  Your usage likely differs, so simply call with '-Dcargo.java.home=PATH-TO-JDK8'.
- When using 'payara', the full maven buil must run under java 17, this is unlike glassfish5 due to how it connects with cargo.  In that case, use '-Denforcer.skip=true' to accomplish.

## Run on Docker
```
docker build . -t jpetstore
docker run -p 8080:8080 jpetstore
```
or with Docker Compose:
```
docker compose up -d
```

## Try integration tests

Perform integration tests for screen transition.

```
$ ./mvnw clean verify -P tomcat9
```
