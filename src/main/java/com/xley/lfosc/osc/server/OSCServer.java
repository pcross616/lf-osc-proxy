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

package com.xley.lfosc.osc.server;

import com.xley.lfosc.impl.ProxyDaemon;
import com.xley.lfosc.util.LogUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetSocketAddress;

public class OSCServer implements Runnable {
    private final InetSocketAddress binding;
    private final Bootstrap bootstrap;
    private final ProxyDaemon daemon;

    public OSCServer(ProxyDaemon daemon, InetSocketAddress binding, EventLoopGroup workerGroup) {
        this.daemon = daemon;
        this.binding = binding;
        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup);
    }

    @Override
    public void run() {
        while (!daemon.isShutdown()) {
            try {
                bootstrap.channel(NioDatagramChannel.class)
                        .option(ChannelOption.SO_BROADCAST, true)
                        .handler(new OSCServerHandler());

                bootstrap.bind(binding).sync().channel().closeFuture().await();
            } catch (InterruptedException e) {
                if (!daemon.isShutdown()) {
                    LogUtil.error(getClass(), e);
                }
            } catch (Exception e) {
                LogUtil.error(getClass(), e);
                daemon.shutdown(2);
            }
        }
    }
}
