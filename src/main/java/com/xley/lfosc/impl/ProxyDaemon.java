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

package com.xley.lfosc.impl;

import com.xley.lfosc.http.server.HttpServer;
import com.xley.lfosc.lightfactory.server.LightFactoryServer;
import com.xley.lfosc.osc.server.OSCServer;
import com.xley.lfosc.util.LogUtil;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import joptsimple.OptionSet;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * The type Proxy daemon.
 */
public class ProxyDaemon implements Runnable {

    /**
     * The constant resources.
     */
    public static final ResourceBundle resources = ResourceBundle.getBundle(ProxyDaemon.class.getSimpleName(),
            Locale.getDefault());
    private final Object monitor = true;

    //configuration
    private final OptionSet options;
    //worker threads and pools
    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    //daemon vars
    private Boolean shutdown = false;
    //process threads
    private Thread daemon;
    private Thread lightFactoryThread;
    private Thread httpServerThread;
    private Thread oscServerThread;
    private int errorcode = 0;

    /**
     * Instantiates a new Proxy daemon.
     *
     * @param optionSet the options from the command line
     */
    public ProxyDaemon(final OptionSet optionSet) {
        this.options = optionSet;
    }

    /**
     * Get the current error code.
     *
     * @return the error code
     */
    public final int errorcode() {
        return this.errorcode;
    }

    public final boolean isShutdown() {
        synchronized (monitor) {
            return shutdown;
        }
    }

    /**
     * Shutdown and set the errorcode
     *
     * @param errorcode the error state
     */
    public final void shutdown(int errorcode) {
        this.errorcode = errorcode;
        this.shutdown();
    }

    /**
     * Shutdown the OSC proxy.
     */
    public final void shutdown() {
        synchronized (monitor) {
            if (shutdown) {
                return;
            }

            shutdown = true;
        }
        LogUtil.info(this.getClass(), resources.getString("shutdown.inprogress"));

        //notify daemon that we are shutting down
        daemon.interrupt();

        //shutdown light factory server if enabled
        if (lightFactoryThread != null) {
            lightFactoryThread.interrupt();
        }

        //shutdown http server if enabled
        if (oscServerThread != null) {
            oscServerThread.interrupt();
        }

        //shutdown http server if enabled
        if (httpServerThread != null) {
            httpServerThread.interrupt();
        }

        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    @Override
    public final void run() {
        daemon = Thread.currentThread();

        int portNumber = (int) options.valueOf("p");
        int httpPortNumber = (int) options.valueOf("w");
        int oscPortNumber = (int) options.valueOf("l");
        boolean oscProxyServer = false;
        boolean lightFactoryProxyServer = false;
        boolean httpServer = false;

        for (Object val : options.valuesOf("m")) {
            switch (String.valueOf(val)) {
                case "osc":
                    oscProxyServer = true;
                    break;
                case "lf":
                    lightFactoryProxyServer = true;
                    break;
                case "http":
                    httpServer = true;
                    break;
                default:
                    LogUtil.warn(this.getClass(), MessageFormat.format(resources.getString("options.mode.invalid.entry"), val));
            }
        }

        //make sure we have a valid mode
        if (!oscProxyServer && !lightFactoryProxyServer && !httpServer) {
            LogUtil.error(this.getClass(), resources.getString("options.mode.invalid"));
            errorcode = 1;
            shutdown();
            return;
        }

        String host = String.valueOf(options.valueOf("b"));
        try {

            //start http server
            if (httpServer) {
                InetSocketAddress binding = new InetSocketAddress(InetAddress.getByName(host), httpPortNumber);
                httpServerThread = new Thread(new HttpServer(this, binding, bossGroup, workerGroup), "OSCProxy - HTTP Server");
                httpServerThread.start();
            }

            // should we create a LightFactory Proxy Server?
            if (lightFactoryProxyServer) {
                InetSocketAddress binding = new InetSocketAddress(InetAddress.getByName(host), portNumber);
                lightFactoryThread = new Thread(new LightFactoryServer(this, binding, bossGroup, workerGroup), "OSCProxy - LightFactory Server");
                lightFactoryThread.start();
            }

            //should we create a OSC Listener?
            if (oscProxyServer) {
                InetSocketAddress binding = new InetSocketAddress(InetAddress.getByName(host), oscPortNumber);
                oscServerThread = new Thread(new OSCServer(this, binding, workerGroup), "OSCProxy - OSC Server");
                oscServerThread.start();
            }

            while (!isShutdown()) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
            }
        } catch (UnknownHostException e) {
            LogUtil.fatal(this.getClass(), MessageFormat.format(resources.getString("daemon.error.unknown.host"), host), e);
            errorcode = 2;
        } catch (Exception e) {
            LogUtil.fatal(this.getClass(), MessageFormat.format(resources.getString("daemon.error.socket"), host), e);
            LogUtil.fatal(this.getClass(), e);
            errorcode = 2;
        } finally {
            shutdown();
        }
    }
}
