# Datastar Java Tools

This is an unofficial [Datastar](https://data-star.dev) SDK.
This version is not fully compatible with the official [Architecture Decision Record](https://github.com/starfederation/datastar/tree/main/sdk) (ADR)
as it lacks the Datastar signal handling. See below for the reasoning.

## Implementation goals

* No build tool (Maven, Gradle, ant, SBT, ivy, bld, ...) required.
* No dependency, only Java SDK/JRE
* Specializations for HTTP servers
* Single file for easy copying
* Fluent builders

## Non goal

Despite the necessity of official SDK requirements, the reading of Datastar signals is not implemented and will not be part of the core.

### No JSON dependency enforced

Datastar signals are just JSON objects wich will be provided via a `datastar` query parameter, or as body content in non-GET requests. When sending (patching) signals back to the browser, the signals have to be provided as JSON string.

The problem with that approach is, that JSON reading would introduce a dependency to a Java JSON library as there is no default implementation present in the JDK. And which library should be enforced? Do you also want marshalling into POJOs, to a map or provide your own handler? As there is no way to answer the questions for you, the technical implementation is up to the SDK user.

So use the JSON library of your choice, the library present in your HTTP server or which is already present on the classpath. You have the choice of JAX-RS (javax.json:javax.json-api), Jakarta EE JSON (jakarta.json:jakarta.json-api), Jackson (com.fasterxml.jackson.core:jackson-databind), Google Gson (com.google.code.gson:gson), Fastjson (com.alibaba.fastjson2:fastjson2), Json.org (org.json:json) and plenty more.

## Usage

The [SDK](sdk) core can be found in the directory `sdk` with a README on how to use it.

The HTTP server integrations can also be found in that directory. For all of them there is an example implementation in the corresponding directory.

The current integrations include

* Jakarta EE REST (formerly JAX-RS)
  * Wildfly
  * Tomcat
  * JBoss
  * OpenLiberty
  * Jetty
  * Quarkus
  * Microprofile Core
  * ...
* Spring MVC
* Spring Webflux
* Micronaut
* Vert.x
* Servlet container
  * GlassFish
  * TomEE
  * Resin Pro
  * IBM WebSphere
  * ...