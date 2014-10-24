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

package com.xley.lfosc.lightfactory.server;

import com.xley.lfosc.IProtocolData;
import com.xley.lfosc.ProtocolException;
import com.xley.lfosc.ProtocolManager;
import com.xley.lfosc.UnknownProtocolException;
import com.xley.lfosc.lightfactory.LightFactoryProtocol;
import com.xley.lfosc.util.LogUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.InetAddress;
import java.text.MessageFormat;

@Sharable
public class LightFactoryHandler extends SimpleChannelInboundHandler<String> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // Send greeting for a new connection.
        ctx.write("LightFactory remote command interface on " + InetAddress.getLocalHost().getHostName() + "\r\n" +
                "\r\n" +
                ".\r\n" +
                "LightFactory-OSC Proxy Server\r\n" +
                "\r\n" +
                ">");
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String request) {
        // Generate and write a response.
        String response;
        boolean close = false;
        if (request.isEmpty()) {
            response = "\r\n>";
        } else if ("exit".equals(request.toLowerCase())) {
            response = "Closing connection";
            close = true;
        } else {
            IProtocolData data = ProtocolManager.resolve(request);

            try {
                if (data == null) {
                    throw new UnknownProtocolException(LightFactoryProtocol.resources.getString("lf.error.invalid"));
                }
                response = MessageFormat.format(LightFactoryProtocol.resources.getString("lf.command.success"),
                        data.getProtocol().process(data));
            } catch (ProtocolException e) {
                response = MessageFormat.format(LightFactoryProtocol.resources.getString("lf.command.failed"),
                        e.getMessage());
            }
        }
        //log the transaction
        LogUtil.trace(getClass(), " << [" + ctx.channel().remoteAddress() + "] - " + response);

        // We do not need to write a ChannelBuffer here.
        // We know the encoder inserted at PipelineFactory will do the conversion.
        ChannelFuture future = ctx.write(response);

        // Close the connection
        if (close) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LogUtil.error(getClass(), cause);
        ctx.close();
    }
}