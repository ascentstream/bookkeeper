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
package org.apache.bookkeeper.http;

import java.util.Set;
import lombok.Getter;
import org.apache.bookkeeper.http.service.HttpEndpointService;

/**
 * A value object that binds an HTTP path to its handler service,
 * optionally restricted to specific HTTP methods.
 */
@Getter
public class HttpEndpoint {

    private final String path;
    private final HttpEndpointService service;
    private final Set<HttpServer.Method> methods;

    /**
     * Create an endpoint that handles all HTTP methods.
     */
    public HttpEndpoint(String path, HttpEndpointService service) {
        this(path, service, null);
    }

    /**
     * Create an endpoint restricted to the given HTTP methods.
     *
     * @param methods the set of allowed methods, or null to allow all methods
     */
    public HttpEndpoint(String path, HttpEndpointService service, Set<HttpServer.Method> methods) {
        this.path = path;
        this.service = service;
        this.methods = methods;
    }

}
