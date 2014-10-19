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

import com.illposed.osc.OSCMessage;
import com.xley.lfosc.osc.client.OSCClient;
import com.xley.lfosc.util.LogUtil;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The type OSC proxy protocol.
 */
public class LightFactoryProtocol {
    /**
     * The constant resources.
     */
    public static final ResourceBundle resources = ResourceBundle.getBundle(LightFactoryProtocol.class.getSimpleName(),
            Locale.getDefault());
    /**
     * The constant osc pattern.
     */
    protected static final Pattern oscPattern = Pattern.compile("^osc@(.*):(\\d+)\\s+(\\/\\S+)(.*)$",
            Pattern.CASE_INSENSITIVE + Pattern.MULTILINE);
    /**
     * The constant data pattern.
     */
    protected static final Pattern dataPattern = Pattern.
            compile("((\"((?<token>.*?)(?<!\\\\)\")|(?<token1>[\\S]+))(\\s)*)",
                    Pattern.CASE_INSENSITIVE + Pattern.MULTILINE);


    /**
     * Address section of the LF command.
     */
    private static final int PARTS_ADDRESS = 1;
    /**
     * Port for the address section of the LF command.
     */
    private static final int PARTS_PORTS = 2;
    /**
     * OSC container section from the LF command.
     */
    private static final int PARTS_CONTAINER = 3;
    /**
     * OSC data section from the LF command.
     */
    private static final int PARTS_DATA = 4;


    /**
     * Process incoming LightFactory remote command.
     * <br/><b>Example input:</b> <i>osc@address:port /first/this/one data</i>
     *
     * @param cmd the the input from LightFactory
     * @return the string
     */
    public static Object process(final String cmd) {

        /**
         * 1. parse input syntax
         *   osc@address:port /first/this/one data
         */
        final String command = cmd.toLowerCase();
        Matcher matches = oscPattern.matcher(command);
        //find the address and verify
        if (matches.find()) {
            LogUtil.debug(LightFactoryProtocol.class, resources.getString("lf.event.valid"));
            String address = matches.group(PARTS_ADDRESS);
            int port = Integer.parseInt(matches.group(PARTS_PORTS));
            String container = matches.group(PARTS_CONTAINER);
            String data = matches.group(PARTS_DATA);
            return process(address, port, container, data);
        }
        return resources.getString("lf.osc.error.invalid");
    }

    public static Object process(String address, int port, String container, String data) {
        try {
            LogUtil.debug(LightFactoryProtocol.class, MessageFormat.format(resources.getString("lf.osc.port.connect"),
                    address, port));

            OSCMessage message = new OSCMessage(container);

            if (data != null && data.length() > 0) {
                Matcher dataMatches = dataPattern.matcher(data);
                while (dataMatches.find()) {
                    message.addArgument(_convertToOSCType(dataMatches.group(2)));
                }
            }

            LogUtil.debug(LightFactoryProtocol.class, MessageFormat.format(resources.getString("lf.osc.port.send"),
                    message.getAddress(), String.valueOf(message.getArguments()),
                    address, port));

            //send the packet
            Object response = OSCClient.send(new InetSocketAddress(InetAddress.getByName(address), port), message);
            return MessageFormat.format(resources.getString("lf.osc.success"), response);

        } catch (UnknownHostException e) {
            return MessageFormat.format(resources.getString("lf.osc.error.unknownhost"), e.getMessage());
        } catch (UnsupportedEncodingException e) {
            return MessageFormat.format(resources.getString("lf.osc.error.encoding"), e.getMessage());
        } catch (SocketException e) {
            return MessageFormat.format(resources.getString("lf.osc.error.socket"), e.getMessage());
        } catch (Exception e) {
            return MessageFormat.format(resources.getString("lf.osc.error.io"), e.getMessage());
        } finally {
            LogUtil.debug(LightFactoryProtocol.class, MessageFormat.format(resources.getString("lf.osc.port.close"),
                    address, port));
        }
    }

    /**
     * Convert from String to OSC type.
     *
     * @param data the data
     * @return the object
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    private static Object _convertToOSCType(final String data) throws UnsupportedEncodingException {
        Object ret = new String(data.getBytes("UTF-8"), Charset.defaultCharset());
        try {
            try {
                ret = Integer.parseInt(data);
                return ret;
            } catch (NumberFormatException nfe) {
                LogUtil.trace(LightFactoryProtocol.class, nfe);
            }

            try {
                return ret = Float.parseFloat(data);
            } catch (NumberFormatException nfe) {
                LogUtil.trace(LightFactoryProtocol.class, nfe);
            }

            return ret;
        } finally {
            LogUtil.trace(LightFactoryProtocol.class, MessageFormat.format(resources.getString("lf.osc.type.convert"),
                    data, ret.getClass().getName()));
        }
    }
}
