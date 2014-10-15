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

package com.xley.lfosc.osc;

import com.illposed.osc.OSCMessage;
import com.xley.lfosc.util.LogUtil;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * The type OSC event protocol.
 */
public abstract class OSCProtocol {
    /**
     * The constant resources.
     */
    public static final ResourceBundle resources = ResourceBundle.getBundle(OSCProtocol.class.getSimpleName(),
            Locale.getDefault());

    /**
     * The address part of the osc event.
     */
    private static final int PART_ADDRESS = 2;
    /**
     * The command part of the osc event.
     */
    private static final int PART_CMD = 3;


    /**
     * Process an incoming OSC event.
     * <br><b>Example OSC message:</b> <i>/lf/&lt;ipaddress:port&gt;/&lt;cmd&gt; arguments</i>
     *
     * @param message the message to be processed
     */
    public static void process(final OSCMessage message) {
        /**
         * 1. parse message syntax
         *   /lf/<ipaddress:port/<cmd> arguments
         */

        try {
            String[] parts = message.getAddress().split("/");
            String[] address = parts[PART_ADDRESS].split(":");
            String cmd = parts[PART_CMD];

            Socket clientSocket = null;
            try {
                LogUtil.debug(OSCProtocol.class, MessageFormat.format(resources.getString("osc.lf.port.connect"), address[0],
                        address[1]));
                clientSocket = new Socket(address[0], Integer.parseInt(address[1]));
                DataOutputStream outToLF = new DataOutputStream(clientSocket.getOutputStream());
                BufferedReader inFromLF = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),
                        Charset.defaultCharset()));

                StringBuilder send = new StringBuilder(cmd);
                for (Object arg : message.getArguments()) {
                    send.append(" ").append(arg);
                }

                String inputLine;
                if (clientSocket.isConnected()) {
                    while ((inputLine = inFromLF.readLine()) != null && !inputLine.equals(">")) {
                        LogUtil.trace(OSCProtocol.class, " << [" + clientSocket.toString() + "] - " + inputLine);
                    }

                    if (inputLine != null && inputLine.equals(">")) {
                        LogUtil.trace(OSCProtocol.class, " << [" + clientSocket.toString() + "] - " + inputLine);
                        LogUtil.trace(OSCProtocol.class, " >> [" + clientSocket.toString() + "] - " + send);
                        outToLF.writeBytes(send + "\n");
                    }

                    while ((inputLine = inFromLF.readLine()) != null) {
                        LogUtil.trace(OSCProtocol.class, " << [" + clientSocket.toString() + "] - " + inputLine);
                    }
                }

            } catch (IOException e) {
                LogUtil.error(OSCProtocol.class, resources.getString("osc.lf.error"), e);
            } finally {
                if (clientSocket != null) {
                    try {
                        LogUtil.debug(OSCProtocol.class, MessageFormat.format(resources.getString("osc.lf.port.close"),
                                address[0], address[1]));
                        clientSocket.close();
                    } catch (IOException e) {
                        LogUtil.trace(OSCProtocol.class, e);
                    }
                }
            }
        } catch (Throwable throwable) {
            LogUtil.error(OSCProtocol.class, resources.getString("osc.lf.error"), throwable);
        }
    }
}
