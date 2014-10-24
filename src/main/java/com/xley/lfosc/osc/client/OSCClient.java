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

package com.xley.lfosc.osc.client;

import com.illposed.osc.OSCPacket;
import com.xley.lfosc.impl.ResultData;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public final class OSCClient {

    /**
     * Send an OSC event to a remote endpoint
     *
     * @param remote the endpoint to send the event to
     * @param packet the event
     * @return any resulting data may be null
     * @throws Exception
     */
    public static Object send(InetSocketAddress remote, OSCPacket packet) throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            return send(remote, group, packet);
        } finally {
            group.shutdownGracefully();
        }

    }

    /**
     * Send an OSC event to a remote endpoint
     *
     * @param remote the endpoint to send the event to
     * @param group  event pool
     * @param packet the event
     * @return any resulting data may be null
     * @throws Exception
     */
    public static Object send(InetSocketAddress remote, EventLoopGroup group, OSCPacket packet) throws Exception {

        Bootstrap b = new Bootstrap();
        ResultData data = new ResultData();
        b.group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new OSCClientHandler(data));

        Channel ch = b.bind(0).sync().channel();

        ChannelFuture lastWriteFuture = ch.writeAndFlush(new DatagramPacket(
                Unpooled.copiedBuffer(ByteBuffer.wrap(packet.getByteArray())), remote)).sync();

        // Wait until all messages are flushed before closing the channel.
        if (lastWriteFuture != null) {
            lastWriteFuture.sync();
        }
        return data.getData();
    }
}