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

## Run as an executable JAR (embedded Tomcat)
JPetStore is packaged as a self-contained executable JAR with an embedded Tomcat (suitable for GKE / Cloud Run).

- Clone this repository

  ```
  $ git clone https://github.com/mybatis/jpetstore-6.git
  ```

- Build the executable jar

  ```
  $ cd jpetstore-6
  $ ./mvnw clean package
  ```

- Start the application

  ```
  $ java -jar target/jpetstore.jar
  ```

  > Runtime configuration through environment variables: `PORT` (default 8080), `CONTEXT_PATH` (default root context),
  > `TOMCAT_BASE_DIR` (writable work dir, default a temp dir), `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`.

- Run application in browser http://localhost:8080/
- Press Ctrl-C to stop the server.

- Integration tests (`ScreenTransitionIT`) require a running instance and are skipped by default; run with `-DskipITs=false`.

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
