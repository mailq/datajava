# Datastar Java SDK

## Core

Just copy the `Datastar.java` into your project for core functionality. It allows you to create the SSE events.

* The core is dependency free and uses only Java JDK 17 features.
* No build tool required
* Copy&Paste. No CD/CI or central code registries.

## JAX-RS integration

If you use JAX-RS according to JSR 311 (from back in 2008), and want to run your code in any Java EE compliant servlet container then you also copy the `JaxRsDatastar.java` and everything else will be handled by the application server like Wildfly, Jetty, Tomcat, Micronaut, Quarkus, ...

Example usage
```java
public class HelloWorldResource {
    @GET
    @Path("/hello-world")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void hello(@QueryParam("datastar") JsonObject signals,
        @Context SseEventSink sse) {
        JaxRsDatastar.send(sse, Datastar.patchElements("""
            <div id="foo">Hello whoever</div>
            """).replaceOuterHtml());
        JaxRsDatastar.send(sse, Datastar.patchSignals("""
            {"foo": 1, "bar": true, "baz": "solid"}
            """).onlyIfMissing().createEvent());
        JaxRsDatastar.send(sse, Datastar.executeScript().
            .withScript("""
            console.log('The server was here');
            """));
        // Must be closed. Otherwise the connection stays
        // open until the client closes the connection.
        sse.close(); 
    }
}
```

Don't want to write JSON signals by hand? Jakarta EE JSON Binding has you covered.
```java
var jsonAsString = Json.createObjectBuilder()
  .add("foo", 1)
  .add("bar", true)
  .add("baz", "solid")
  .build().toString()
```

* JAX-RS compliant. Depends on compliant application server
* Needs JSON (JSON-B or JSON-P) support; should be alredy included
* Build-in Datastar signals converter
* No build tool required
* Copy&Paste. No CD/CI or central code registries.
* No configuration. "Just works"

## Other integrations

Not implemented yet. But for example in Vert.x the conversion from `Datastar.Event` to standard SSE events should be easy.