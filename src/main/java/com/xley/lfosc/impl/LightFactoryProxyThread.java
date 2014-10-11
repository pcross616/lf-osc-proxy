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

import com.xley.lfosc.util.LogUtil;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The main OSC proxy thread.
 */
public class LightFactoryProxyThread extends Thread {
    /**
     * The constant resources.
     */
    public static final ResourceBundle resources = ResourceBundle.getBundle(LightFactoryProxyThread.class.
            getSimpleName(), Locale.getDefault());
    /**
     * An atomic id for each thread instance.
     */
    private static final AtomicInteger count = new AtomicInteger();

    /**
     * Socket for this thread.
     */
    private final Socket socket;

    /**
     * Instantiates a new OSC proxy thread.
     *
     * @param connection the socket
     */
    public LightFactoryProxyThread(final Socket connection) {
        super("LightFactoryProxyThread - " + count.incrementAndGet());
        this.socket = connection;
    }

    public final void run() {
        LogUtil.info(this.getClass(), MessageFormat.format(resources.getString("osc.connection.established"),
                socket.getInetAddress()));
        try (
                PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),
                        Charset.defaultCharset()), true);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream(), Charset.defaultCharset()))
        ) {
            String inputLine, outputLine;
            LightFactoryProtocol opp = new LightFactoryProtocol();
            if (socket.isConnected() && (inputLine = in.readLine()) != null) {
                inputLine = inputLine.trim();
                LogUtil.trace(this.getClass(), ">> " + inputLine);
                outputLine = opp.process(inputLine);
                out.println(outputLine);
                LogUtil.trace(this.getClass(), "<< " + outputLine);
            }
        } catch (IOException e) {
            LogUtil.error(this.getClass(), resources.getString("osc.connection.error"), e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                //do nothing
                LogUtil.trace(this.getClass(), e);
            }
            LogUtil.info(this.getClass(), MessageFormat.format(resources.getString("osc.connection.disconnected"),
                    socket.getInetAddress().getHostAddress()));
        }
    }
}
