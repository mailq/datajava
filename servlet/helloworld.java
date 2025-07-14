//usr/bin/env jbang "$0" "$@" ; exit $?
//SOURCES dev/datastar/*.java
//DEPS org.eclipse.jetty.ee10:jetty-ee10-servlet:12.0.23
//DEPS org.glassfish:jakarta.json:2.0.1
//JAVAC_OPTIONS -parameters

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;

import dev.datastar.Datastar;
import dev.datastar.ServletDatastar;
import dev.datastar.ServletDatastar.DatastarHeaderFilter;
import jakarta.json.Json;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class helloworld extends HttpServlet {
    public static void main(String[] args) throws Exception {
        // Fastest way to serve the static file
        var index = Files.readString(Path.of("index.html"));
        var server = new Server(8080);
        var context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(helloworld.class, "/hello-world");
        context.addServlet(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response)
                    throws IOException {
                response.setContentType("text/html");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().print(index);
            }
        }, "/");
        // Automatically adds the required headers to Datastar requests. If not set here
        // globally, you have to prepare the headers in every servlet with
        // ServletDatastar.prepareResponse();
        context.addFilter(new DatastarHeaderFilter(), "/*", null);
        server.setHandler(context);
        server.start();
        server.join();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        long delay = 400;
        try (var reader = Json.createReader(new StringReader(request.getParameter("datastar")))) {
            var signals = reader.readObject();
            delay = signals.getJsonNumber("delay").longValue();
        }
        var hello = "Hello, world!";
        for (var i = 1; i <= hello.length(); i++) {
            ServletDatastar.send(response, Datastar
                    .patchElements().replace("<h1 id=\"message\">%s</h1>".formatted(hello.substring(0, i))));
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
