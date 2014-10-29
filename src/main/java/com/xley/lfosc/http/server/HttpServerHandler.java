/*
 * Copyright (c) 2014. Peter Crossley (xley.com)
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.xley.lfosc.http.server;

import com.xley.lfosc.IProtocol;
import com.xley.lfosc.IProtocolData;
import com.xley.lfosc.ProtocolException;
import com.xley.lfosc.ProtocolManager;
import com.xley.lfosc.http.HttpProtocol;
import com.xley.lfosc.util.LogUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpServerHandler extends ChannelInboundHandlerAdapter {


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;

            if (HttpHeaders.is100ContinueExpected(req)) {
                ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
            }
            boolean keepAlive = HttpHeaders.isKeepAlive(req);
            Object result;
            boolean error = false;

            IProtocol protocol = ProtocolManager.getProtocol("http");
            IProtocolData protocolData = protocol.createProtocolData(req.getUri());
            try {
                if (protocolData == null) {
                    throw new ProtocolException(HttpProtocol.resources.getString("http.error.invalid_protocol"));
                }
                result = protocolData.getProtocol().process(protocolData);
            } catch (ProtocolException e) {
                error = true;
                result = e.getMessage() + (e.getCause() != null && e.getCause().getMessage() != null ?
                        " - " + e.getCause().getMessage() : "");
            }

            //check for JSONP request
            QueryStringDecoder qsd = new QueryStringDecoder(req.getUri());
            String contentType = "text/plain";
            if (qsd.parameters().get("callback") != null) {
                String callback = qsd.parameters().get("callback").get(0);
                contentType = "application/javascript";
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> jsonp = new HashMap<>();

                if (error) {
                    jsonp.put("error", result);
                } else {
                    jsonp.put("data", result);
                }

                try {
                    result = callback + "(" + mapper.writeValueAsString(jsonp) + ");";
                } catch (IOException e) {
                    LogUtil.error(getClass(), e);
                }
            }

            FullHttpResponse response = null;
            if (result != null) {
                response = new DefaultFullHttpResponse(HTTP_1_1, error ? BAD_REQUEST : OK,
                        Unpooled.wrappedBuffer(String.valueOf(result).getBytes(Charset.defaultCharset())));
                response.headers().set(CONTENT_TYPE, contentType);
                response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
            } else {
                response = new DefaultFullHttpResponse(HTTP_1_1, NO_CONTENT);
            }

            if (!keepAlive) {
                ctx.write(response).addListener(ChannelFutureListener.CLOSE);
            } else {
                response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                ctx.write(response);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            //write the error to the response
            PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(baos));
            cause.printStackTrace(printWriter);

            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    Unpooled.wrappedBuffer(baos.toByteArray()));
            response.headers().set(CONTENT_TYPE, "text/plain");
            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());

            LogUtil.error(getClass(), cause);
        } finally {
            try {
                baos.close();
            } catch (IOException e) {
                LogUtil.error(getClass(), e);
            }
            ctx.close();
        }

    }
}