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

import com.illposed.osc.OSCMessage;
import com.xley.lfosc.lightfactory.client.LightFactoryClient;
import com.xley.lfosc.util.LogUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.UnresolvedAddressException;
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
    public static Object process(final OSCMessage message) {
        /**
         * 1. parse message syntax
         *   /lf/<ipaddress:port/<cmd> arguments
         */

        Object response = null;
        try {
            String[] parts = message.getAddress().split("/");
            String[] address = parts[PART_ADDRESS].split(":");
            String cmd = parts[PART_CMD];

            StringBuilder send = new StringBuilder(cmd);
            for (Object arg : message.getArguments()) {
                send.append(" ").append(arg);
            }

            response = process(address[0], Integer.parseInt(address[1]), send);
        } catch (Throwable throwable) {
            LogUtil.error(OSCProtocol.class, resources.getString("osc.lf.error"), throwable);
        }
        return response;
    }

    /**
     *
     * @param address
     * @param port
     * @param cmd
     * @return
     */
    public static Object process(String address, int port, CharSequence cmd) {
        try {
            LogUtil.debug(OSCProtocol.class, MessageFormat.format(resources.getString("osc.lf.port.connect"), address,
                    port));

            //send OSC event to LightFactory
            return LightFactoryClient.send(new InetSocketAddress(address, port), cmd);
        }
        catch (UnknownHostException e) {
            LogUtil.error(OSCProtocol.class, MessageFormat.format(resources.getString("osc.lf.error.unknownhost"),
                                                                    address+":" + port));
            return MessageFormat.format(resources.getString("osc.lf.error.unknownhost"), address+":" + port);
        }
        catch (UnresolvedAddressException e) {
            LogUtil.error(OSCProtocol.class, MessageFormat.format(resources.getString("osc.lf.error.unknownhost"),
                                                                    address+":" + port));
            return MessageFormat.format(resources.getString("osc.lf.error.unknownhost"), address+":" + port);
        }
        catch (Exception e) {
            LogUtil.error(OSCProtocol.class, resources.getString("osc.lf.error"), e);
            return resources.getString("osc.lf.error");
        } finally {
            LogUtil.debug(OSCProtocol.class, MessageFormat.format(resources.getString("osc.lf.port.close"), address,
                    port));
        }
    }
}
