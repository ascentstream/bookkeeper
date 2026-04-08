/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.bookkeeper.http.vertx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import org.apache.bookkeeper.http.HttpEndpoint;
import org.apache.bookkeeper.http.HttpExtension;
import org.apache.bookkeeper.http.HttpRouter;
import org.apache.bookkeeper.http.HttpServer;
import org.apache.bookkeeper.http.HttpServiceProvider;
import org.apache.bookkeeper.http.NullHttpServiceProvider;
import org.apache.bookkeeper.http.service.HttpEndpointService;
import org.apache.bookkeeper.http.service.HttpServiceRequest;
import org.apache.bookkeeper.http.service.HttpServiceResponse;
import org.junit.Test;

/**
 * Unit test for HTTP extension SPI loading and routing in {@link VertxHttpServer}.
 */
public class TestVertxHttpServerExtension {

    /**
     * A test extension that registers a GET-only hello endpoint.
     */
    public static class GetOnlyExtension implements HttpExtension {
        @Override
        public List<HttpEndpoint> getEndpoints(HttpServiceProvider provider) {
            return Collections.singletonList(
                new HttpEndpoint("/api/v1/ext/hello",
                    new HelloService(),
                    EnumSet.of(HttpServer.Method.GET)));
        }
    }

    /**
     * A test extension that registers an endpoint accepting all HTTP methods.
     */
    public static class AllMethodsExtension implements HttpExtension {
        @Override
        public List<HttpEndpoint> getEndpoints(HttpServiceProvider provider) {
            return Collections.singletonList(
                new HttpEndpoint("/api/v1/ext/all", new HelloService()));
        }
    }

    /**
     * A test extension that registers multiple endpoints.
     */
    public static class MultiEndpointExtension implements HttpExtension {
        @Override
        public List<HttpEndpoint> getEndpoints(HttpServiceProvider provider) {
            return Arrays.asList(
                new HttpEndpoint("/api/v1/ext/multi/hello", new HelloService()),
                new HttpEndpoint("/api/v1/ext/multi/echo",
                    new EchoService(),
                    EnumSet.of(HttpServer.Method.POST)));
        }
    }

    /**
     * An extension that returns an endpoint with an invalid (non-absolute) path.
     */
    public static class InvalidPathExtension implements HttpExtension {
        @Override
        public List<HttpEndpoint> getEndpoints(HttpServiceProvider provider) {
            return Collections.singletonList(
                new HttpEndpoint("no-leading-slash", new HelloService()));
        }
    }

    static class HelloService implements HttpEndpointService {
        @Override
        public HttpServiceResponse handle(HttpServiceRequest request) {
            return new HttpServiceResponse("hello", HttpServer.StatusCode.OK);
        }
    }

    static class EchoService implements HttpEndpointService {
        @Override
        public HttpServiceResponse handle(HttpServiceRequest request) {
            return new HttpServiceResponse(request.getBody(), HttpServer.StatusCode.OK);
        }
    }

    @Test
    public void testExtensionEndpoint_GET() throws Exception {
        VertxHttpServer httpServer = new VertxHttpServer();
        HttpServiceProvider httpServiceProvider = NullHttpServiceProvider.getInstance();
        httpServer.initialize(httpServiceProvider);
        httpServer.setHttpExtensionClasses(
            new String[]{GetOnlyExtension.class.getName()});
        assertTrue(httpServer.startServer(0));
        int port = httpServer.getListeningPort();

        HttpResponse httpResponse = send(getUrl(port, "/api/v1/ext/hello"), HttpServer.Method.GET);
        assertEquals(HttpServer.StatusCode.OK.getValue(), httpResponse.responseCode);
        assertEquals("hello", httpResponse.responseBody);
        httpServer.stopServer();
    }

    @Test
    public void testExtensionEndpoint_MethodRestricted() throws Exception {
        VertxHttpServer httpServer = new VertxHttpServer();
        HttpServiceProvider httpServiceProvider = NullHttpServiceProvider.getInstance();
        httpServer.initialize(httpServiceProvider);
        httpServer.setHttpExtensionClasses(
            new String[]{GetOnlyExtension.class.getName()});
        assertTrue(httpServer.startServer(0));
        int port = httpServer.getListeningPort();

        // POST should return 405 since only GET is allowed
        HttpResponse httpResponse = send(getUrl(port, "/api/v1/ext/hello"), HttpServer.Method.POST);
        assertEquals(HttpServer.StatusCode.METHOD_NOT_ALLOWED.getValue(), httpResponse.responseCode);
        httpServer.stopServer();
    }

    @Test
    public void testExtensionEndpoint_AllMethods() throws Exception {
        VertxHttpServer httpServer = new VertxHttpServer();
        HttpServiceProvider httpServiceProvider = NullHttpServiceProvider.getInstance();
        httpServer.initialize(httpServiceProvider);
        httpServer.setHttpExtensionClasses(
            new String[]{AllMethodsExtension.class.getName()});
        assertTrue(httpServer.startServer(0));
        int port = httpServer.getListeningPort();

        for (HttpServer.Method method : HttpServer.Method.values()) {
            HttpResponse httpResponse = send(getUrl(port, "/api/v1/ext/all"), method);
            assertEquals("Method " + method + " should return 200",
                HttpServer.StatusCode.OK.getValue(), httpResponse.responseCode);
        }
        httpServer.stopServer();
    }

    @Test
    public void testExtensionEndpoint_InvalidPathSkipped() throws Exception {
        VertxHttpServer httpServer = new VertxHttpServer();
        HttpServiceProvider httpServiceProvider = NullHttpServiceProvider.getInstance();
        httpServer.initialize(httpServiceProvider);
        httpServer.setHttpExtensionClasses(
            new String[]{InvalidPathExtension.class.getName()});
        assertTrue(httpServer.startServer(0));
        int port = httpServer.getListeningPort();

        // The invalid path should be skipped, so requesting it returns 404
        HttpResponse httpResponse = send(getUrl(port, "/no-leading-slash"), HttpServer.Method.GET);
        assertEquals(HttpServer.StatusCode.NOT_FOUND.getValue(), httpResponse.responseCode);
        httpServer.stopServer();
    }

    @Test
    public void testExtensionClassNotFound_Graceful() throws Exception {
        VertxHttpServer httpServer = new VertxHttpServer();
        HttpServiceProvider httpServiceProvider = NullHttpServiceProvider.getInstance();
        httpServer.initialize(httpServiceProvider);
        httpServer.setHttpExtensionClasses(
            new String[]{"com.nonexistent.ExtensionClass"});
        assertTrue(httpServer.startServer(0));
        int port = httpServer.getListeningPort();

        // Built-in endpoints should still work
        HttpResponse httpResponse = send(getUrl(port, HttpRouter.HEARTBEAT), HttpServer.Method.GET);
        assertEquals(HttpServer.StatusCode.OK.getValue(), httpResponse.responseCode);
        httpServer.stopServer();
    }

    @Test
    public void testNoExtensions_Configured() throws Exception {
        VertxHttpServer httpServer = new VertxHttpServer();
        HttpServiceProvider httpServiceProvider = NullHttpServiceProvider.getInstance();
        httpServer.initialize(httpServiceProvider);
        // No extension classes configured
        assertTrue(httpServer.startServer(0));
        int port = httpServer.getListeningPort();

        HttpResponse httpResponse = send(getUrl(port, HttpRouter.HEARTBEAT), HttpServer.Method.GET);
        assertEquals(HttpServer.StatusCode.OK.getValue(), httpResponse.responseCode);
        httpServer.stopServer();
    }

    @Test
    public void testMultipleExtensions() throws Exception {
        VertxHttpServer httpServer = new VertxHttpServer();
        HttpServiceProvider httpServiceProvider = NullHttpServiceProvider.getInstance();
        httpServer.initialize(httpServiceProvider);
        httpServer.setHttpExtensionClasses(
            new String[]{MultiEndpointExtension.class.getName()});
        assertTrue(httpServer.startServer(0));
        int port = httpServer.getListeningPort();

        // First endpoint (GET)
        HttpResponse helloResp = send(getUrl(port, "/api/v1/ext/multi/hello"), HttpServer.Method.GET);
        assertEquals(HttpServer.StatusCode.OK.getValue(), helloResp.responseCode);
        assertEquals("hello", helloResp.responseBody);

        // Second endpoint (POST)
        String body = "echo-test";
        HttpResponse echoResp = send(getUrl(port, "/api/v1/ext/multi/echo"), HttpServer.Method.POST, body);
        assertEquals(HttpServer.StatusCode.OK.getValue(), echoResp.responseCode);
        assertEquals(body, echoResp.responseBody);
        httpServer.stopServer();
    }

    // --- helper methods ---

    private HttpResponse send(String url, HttpServer.Method method) throws IOException {
        return send(url, method, "");
    }

    private HttpResponse send(String url, HttpServer.Method method, String body) throws IOException {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod(method.toString());
        if (!body.isEmpty()) {
            con.setDoOutput(true);
            con.setFixedLengthStreamingMode(body.length());
            con.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
            con.getOutputStream().flush();
        }
        int responseCode = con.getResponseCode();
        StringBuilder response = new StringBuilder();
        java.io.InputStream stream = responseCode >= 400 ? con.getErrorStream() : con.getInputStream();
        BufferedReader in = null;
        try {
            if (stream != null) {
                in = new BufferedReader(new InputStreamReader(stream));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return new HttpResponse(responseCode, response.toString());
    }

    private String getUrl(int port, String path) {
        return "http://localhost:" + port + path;
    }

    private static class HttpResponse {
        private final int responseCode;
        private final String responseBody;

        HttpResponse(int responseCode, String responseBody) {
            this.responseCode = responseCode;
            this.responseBody = responseBody;
        }
    }
}
