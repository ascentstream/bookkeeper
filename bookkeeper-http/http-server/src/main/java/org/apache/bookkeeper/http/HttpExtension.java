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

import java.util.List;

/**
 * SPI interface for HTTP endpoint extensions.
 * Configured via "httpExtensions" in bk_server.conf.
 *
 * <p>One extension class can register multiple endpoints.
 *
 * <p>Usage:
 * <ol>
 *   <li>Implement this interface</li>
 *   <li>Set httpExtensions=com.example.MyExtension in bk_server.conf</li>
 *   <li>Put the JAR in BookKeeper's classpath</li>
 * </ol>
 *
 * <p>Simple usage (no BK internals needed):
 * <pre>
 * public List&lt;HttpEndpoint&gt; getEndpoints(HttpServiceProvider provider) {
 *     return Arrays.asList(
 *         new HttpEndpoint("/api/v1/ext/hello",
 *             request -&gt; new HttpServiceResponse().setBody("hello"))
 *     );
 * }
 * </pre>
 *
 * <p>Advanced usage (access Bookie internals):
 * <pre>
 * public List&lt;HttpEndpoint&gt; getEndpoints(HttpServiceProvider provider) {
 *     BKHttpServiceProvider bkProvider = (BKHttpServiceProvider) provider;
 *     Bookie bookie = bkProvider.getBookieServer().getBookie();
 *     ...
 * }
 * </pre>
 */
public interface HttpExtension {

    /**
     * Return all endpoints to register.
     *
     * @param provider the HTTP service provider. In BookKeeper, this is
     *                 {@code BKHttpServiceProvider} which provides access to
     *                 {@code BookieServer}, {@code Bookie}, etc. via casting.
     */
    List<HttpEndpoint> getEndpoints(HttpServiceProvider provider);
}
