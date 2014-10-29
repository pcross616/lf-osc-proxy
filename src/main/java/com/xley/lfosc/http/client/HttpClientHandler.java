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

import com.xley.lfosc.impl.ResultData;
import com.xley.lfosc.util.LogUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

/**
 * Handles a client-side channel.
 */
@ChannelHandler.Sharable
public class HttpClientHandler extends SimpleChannelInboundHandler<HttpObject> {

    private final ResultData data;

    public HttpClientHandler(ResultData data) {
        this.data = data;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;
            data.setStatus(response.getStatus());
        }
        if (msg instanceof HttpContent) {
            HttpContent content = (HttpContent) msg;
            data.setData(content.content().toString(CharsetUtil.UTF_8));
            if (content instanceof LastHttpContent) {
                ctx.close();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LogUtil.error(getClass(), cause);
        data.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        data.setData(cause);
        ctx.close();
    }
}