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

package com.xley.lfosc;

import com.illposed.osc.OSCPortIn;
import com.xley.lfosc.impl.LightFactoryProxyThread;
import com.xley.lfosc.impl.OSCProxyListener;
import com.xley.lfosc.util.LogUtil;
import joptsimple.OptionSet;

import java.io.IOException;
import java.net.*;
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
    OSCProxyListener listener = new OSCProxyListener();
    //daemon vars
    private Boolean shutdown = false;
    private Thread runner;
    private int errorcode = 0;
    //connections
    private ServerSocket serverSocket;
    private OSCPortIn receiver = null;


    /**
     * Instantiates a new Proxy daemon.
     *
     * @param optionSet the options from the command line
     */
    protected ProxyDaemon(final OptionSet optionSet) {
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

    /**
     * Shutdown the OSC proxy.
     */
    public final void shutdown() {
        synchronized (monitor) {
            if (shutdown) {
                return;
            }
        }
        shutdown = true;
        LogUtil.info(this.getClass(), resources.getString("shutdown.inprogress"));
        runner.interrupt();
        if (receiver != null) {
            receiver.stopListening();
            receiver.close();
        }

        if (listener != null) {
            listener.shutdown();
        }

        if (serverSocket != null) {
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (Throwable e) {
                LogUtil.trace(e);
            }
        }
    }

    @Override
    public final void run() {
        runner = Thread.currentThread();

        int portNumber = (int) options.valueOf("p");
        int oscPortNumber = (int) options.valueOf("l");
        int threads = (int) options.valueOf("t");
        boolean oscEnabled = true;
        boolean lfBridgeEnabled = true;

        switch (String.valueOf(options.valueOf("m"))) {
            case "osc":
                oscEnabled = true;
                lfBridgeEnabled = false;
                break;
            case "bridge":
                oscEnabled = false;
                lfBridgeEnabled = true;
                break;
            case "both":
                break;
            default:
                LogUtil.error(this.getClass(), resources.getString("options.mode.invalid"));
                errorcode = 1;
                shutdown();
                return;
        }

        String host = String.valueOf(options.valueOf("b"));
        try {
            //bindings
            InetSocketAddress binding = new InetSocketAddress(InetAddress.getByName(host), portNumber);
            InetSocketAddress oscBinding = new InetSocketAddress(InetAddress.getByName(host), oscPortNumber);

            if (oscEnabled) {
                LogUtil.info(this.getClass(), MessageFormat.format(resources.getString("osc.listener.on"), host, oscPortNumber));
                receiver = new OSCPortIn(new DatagramSocket(oscBinding));
                receiver.addListener(resources.getString("osc.listener.binding"), listener);
                receiver.startListening();

                //check to see if we are the only listener to run.
                if (!lfBridgeEnabled) {
                    while (!shutdown) {
                        if (!receiver.isListening() || Thread.currentThread().isInterrupted()) {
                            break;
                        }
                    }
                }

            }
            if (lfBridgeEnabled) {
                try {
                    serverSocket = new ServerSocket(binding.getPort(), threads, binding.getAddress());
                    LogUtil.info(this.getClass(), MessageFormat.format(resources.getString("lf.listener.on"), host, portNumber));
                    while (!shutdown && !Thread.currentThread().isInterrupted()) {
                        new LightFactoryProxyThread(serverSocket.accept()).start();
                    }
                } catch (IOException e) {
                    if (!shutdown) {
                        LogUtil.fatal(this.getClass(), MessageFormat.format(resources.getString("lf.listener.on.error"),
                                host, portNumber), e);
                        errorcode = 2;
                    }
                }
            }

        } catch (UnknownHostException e) {
            LogUtil.fatal(this.getClass(), MessageFormat.format(resources.getString("daemon.error.unknown.host"), host), e);
            errorcode = 2;
        } catch (SocketException e) {
            LogUtil.fatal(this.getClass(), MessageFormat.format(resources.getString("daemon.error.socket"), host), e);
            LogUtil.fatal(this.getClass(), "", e);
            errorcode = 2;
        } finally {
            shutdown();
        }
    }
}
