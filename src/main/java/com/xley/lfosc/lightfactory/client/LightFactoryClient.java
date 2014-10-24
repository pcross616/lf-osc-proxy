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

package com.xley.lfosc.lightfactory.client;

import com.xley.lfosc.impl.ResultData;
import com.xley.lfosc.util.LogUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;

import java.net.SocketAddress;

public final class LightFactoryClient {

    /**
     * Send command to a remote LightFactory
     *
     * @param remote  address for LightFactor
     * @param command the command to run
     * @return any result text
     * @throws Exception may throw a socket or io error
     */
    public static Object send(final SocketAddress remote, final CharSequence command) throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            return send(remote, group, command);
        } finally {
            group.shutdownGracefully();
        }
    }

    /**
     * Send command to a remote LightFactory
     *
     * @param remote  address for LightFactory
     * @param group   the event worker group for NIO sockets
     * @param command the command to run
     * @return any result text
     * @throws Exception may throw a socket or io error
     */
    public static Object send(final SocketAddress remote, EventLoopGroup group, final CharSequence command) throws Exception {
        Bootstrap b = new Bootstrap();
        ResultData data = new ResultData();
        b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LightFactoryClient.class.getName()))
                .handler(new LightFactoryClientInitializer(data));

        // Start the connection attempt.
        Channel ch = b.connect(remote).sync().channel();

        // Read commands from the stdin.
        ChannelFuture lastWriteFuture = null;
        if (command == null) {
            return null;
        }

        LogUtil.trace(LightFactoryClient.class, " >> [" + remote + "] - " + command);

        // Sends the received line to the server.
        lastWriteFuture = ch.writeAndFlush(command + "\r\n");

        ch.closeFuture().await(2000);

        // Wait until all messages are flushed before closing the channel.
        if (lastWriteFuture != null) {
            lastWriteFuture.sync();
        }
        return data.getData();
    }
}