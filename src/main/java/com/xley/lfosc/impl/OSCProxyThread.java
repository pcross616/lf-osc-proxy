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

import com.xley.lfosc.OSCProxy;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

public class OSCProxyThread extends Thread {
    private final static AtomicInteger count = new AtomicInteger();
    private final Socket socket;

    public OSCProxyThread(Socket socket) {
        super("OSCProxyThread - " + count.incrementAndGet());
        this.socket = socket;
    }

    public void run() {
        OSCProxy.logger.info("OSC connection established. Remote Address: " + socket.getInetAddress().getHostAddress());
        try (
                PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),
                        Charset.defaultCharset()), true);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream(), Charset.defaultCharset()))
        ) {
            String inputLine, outputLine;
            OSCProxyProtocol opp = new OSCProxyProtocol();
            while (socket.isConnected() && in.ready() && (inputLine = in.readLine()) != null) {
                inputLine = inputLine.trim();
                OSCProxy.logger.trace(">> " + inputLine);
                outputLine = opp.processLFRemoteCommand(inputLine);
                out.println(outputLine);
                OSCProxy.logger.trace("<< " + outputLine);
            }
        } catch (IOException e) {
            OSCProxy.logger.error("OSC Connection Error:", e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                //do nothing
            }
            OSCProxy.logger.info("OSC connection disconnected. Remote Address: " + socket.getInetAddress().getHostAddress());
        }
    }
}