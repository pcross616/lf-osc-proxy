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

import com.xley.lfosc.impl.ProxyDaemon;
import com.xley.lfosc.lightfactory.LightFactoryProtocol;
import com.xley.lfosc.util.LogUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;

import java.net.InetSocketAddress;
import java.text.MessageFormat;

public class LightFactoryServer implements Runnable {

    private final InetSocketAddress binding;
    private final ServerBootstrap bootstrap;
    private final ProxyDaemon daemon;

    public LightFactoryServer(ProxyDaemon daemon, InetSocketAddress binding,
                              EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        this.daemon = daemon;
        this.binding = binding;
        bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup);
    }

    @Override
    public void run() {
        while (!daemon.isShutdown()) {
            try {
                bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
                bootstrap.channel(NioServerSocketChannel.class)
                        .handler(new LoggingHandler(this.getClass().getName()))
                        .childHandler(new LightFactoryInitializer());
                LogUtil.info(MessageFormat.format(LightFactoryProtocol.resources.getString("lf.listening"),
                        binding.toString()));
                bootstrap.bind(binding).sync().channel().closeFuture().sync();
            } catch (InterruptedException e) {
                if (!daemon.isShutdown()) {
                    LogUtil.error(getClass(), e);
                }
            } catch (Exception e) {
                if (!daemon.isShutdown()) {
                    LogUtil.error(getClass(), e);
                    daemon.shutdown(2);
                }
            }
        }
    }
}
