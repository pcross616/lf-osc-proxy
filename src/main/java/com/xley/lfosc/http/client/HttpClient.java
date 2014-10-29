/*
 * Copyright (c) 2014. Peter Crossley
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
 */

package com.xley.lfosc.http.client;

import com.xley.lfosc.ClientProtocolException;
import com.xley.lfosc.impl.ResultData;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.net.URI;

public final class HttpClient {

    /**
     * Access Http URL
     *
     * @param url the command to run
     * @return any result text
     * @throws Exception may throw a socket or io error
     */
    public static Object send(final String url) throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            return send(group, url);
        } finally {
            group.shutdownGracefully();
        }
    }

    /**
     * Send command to a remote LightFactory
     *
     * @param group the event worker group for NIO sockets
     * @param url   the command to run
     * @return any result text
     * @throws Exception may throw a socket or io error
     */
    public static Object send(EventLoopGroup group, final String url) throws Exception {
        ResultData data = new ResultData();
        URI uri = new URI(url);
        String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
        String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
        int port = uri.getPort();
        if (port == -1) {
            if ("http".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("https".equalsIgnoreCase(scheme)) {
                port = 443;
            }
        }

        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new ClientProtocolException("Only HTTP(S) is supported. [" + url + "]", null);
        }

        // Configure SSL context if necessary.
        final boolean ssl = "https".equalsIgnoreCase(scheme);
        final SslContext sslCtx;
        if (ssl) {
            sslCtx = SslContext.newClientContext(InsecureTrustManagerFactory.INSTANCE);
        } else {
            sslCtx = null;
        }

        // Configure the client.
        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new HttpClientInitializer(sslCtx, data));

        // Make the connection attempt.
        Channel ch = b.connect(host, port).sync().channel();

        // Prepare the HTTP request.
        HttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, uri.getRawPath());
        request.headers().set(HttpHeaders.Names.HOST, host);
        request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
        request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);

        // Send the HTTP request.
        ch.writeAndFlush(request);

        // Wait for the server to close the connection.
        ch.closeFuture().sync();
        Object result = data.getData();
        if (data.getStatus() instanceof HttpResponseStatus && (((HttpResponseStatus) data.getStatus()).code() >= 400)) {
            if (result instanceof Throwable) {
                throw new ClientProtocolException(((HttpResponseStatus) data.getStatus()).reasonPhrase(),
                        data.getStatus(), (Throwable) result);
            } else {
                throw new ClientProtocolException(((HttpResponseStatus) data.getStatus()).reasonPhrase() +
                        " - [" + result + "]", data.getStatus());
            }
        }
        return result;
    }
}