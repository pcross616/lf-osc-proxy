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
import com.illposed.osc.OSCPacket;
import com.xley.lfosc.IProtocol;
import com.xley.lfosc.IProtocolData;
import com.xley.lfosc.ProtocolException;
import com.xley.lfosc.impl.SimpleProtocolData;
import com.xley.lfosc.osc.client.OSCClient;
import com.xley.lfosc.util.LogUtil;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.UnresolvedAddressException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The type OSC event protocol.
 */
public class OSCProtocol implements IProtocol {

    /**
     * The constant resources.
     */
    public static final ResourceBundle resources = ResourceBundle.getBundle(OSCProtocol.class.getSimpleName(),
            Locale.getDefault());
    /**
     * The constant osc address pattern.
     */
    protected static final Pattern oscAddress = Pattern.compile("^\\/(.*)\\/(.*):(\\d+)(\\/.*)\\/(.*)$",
            Pattern.CASE_INSENSITIVE + Pattern.MULTILINE);

    private static final int OSC_EVENT_PROTOCOL = 1;
    private static final int OSC_EVENT_HOST = 2;
    private static final int OSC_EVENT_PORT = 3;
    private static final int OSC_EVENT_ADDRESS = 4;
    private static final int OSC_EVENT_DATA = 5;

    @Override
    public Object process(IProtocolData data) throws ProtocolException {
        String[] address = ((String) data.getTarget()).split(":");
        if (address.length != 2) {
            throw new ProtocolException(MessageFormat.format(resources.getString("osc.error.unknownhost"), data.getTarget()));
        }
        if (data.getData() != null && data.getData() instanceof OSCPacket) {
            return _process(address[0], Integer.parseInt(address[1]), (OSCMessage) data.getData());
        }
        return _process(address[0], Integer.parseInt(address[1]), (String) data.getOperation(),
                data.getData());
    }


    /**
     * Create protocol data for any osc event
     * <br><b>Example OSC message:</b> <i>/&lt;protocol&gt;</>/&lt;ipaddress:port&gt;/&lt;cmd&gt; arguments</i>
     *
     * @param value the dataset to create a {@link com.xley.lfosc.IProtocolData} for if it matches
     * @return the protocol data or null
     */
    @Override
    public IProtocolData createProtocolData(Object value) {
        if (value instanceof OSCMessage) {
            LogUtil.debug(getClass(), resources.getString("osc.event.valid"));
            return new OSCProtocolData((OSCMessage) value);
        } else if (value instanceof CharSequence) {
            Matcher matches = oscAddress.matcher((CharSequence) value);
            //find the address and verify
            if (matches.find()) {
                LogUtil.debug(getClass(), resources.getString("osc.event.valid"));
                String protocol = matches.group(OSC_EVENT_PROTOCOL);
                String host = matches.group(OSC_EVENT_HOST);
                int port = Integer.parseInt(matches.group(OSC_EVENT_PORT));
                String address = matches.group(OSC_EVENT_ADDRESS);
                String data = matches.group(OSC_EVENT_DATA);
                List<Object> args = new ArrayList<>();
                if (data != null) {
                    args = new ArrayList<>();
                    String[] dataArray = data.split(" ");
                    Collections.addAll(args, dataArray);
                }
                return new OSCProtocolData(protocol, host + ":" + port, address, args);
            }
        }
        return null;
    }

    @Override
    public IProtocolData configureProtocolData(IProtocolData data, Object value) {
        String operation = (String) value;
        List<Object> argsList = null;
        if (operation.contains(" ")) {
            String[] args = operation.substring(operation.indexOf(" ")).trim().split(" "); //find the start of the arguments
            operation = operation.substring(0, operation.indexOf(" ")); //find the start of the arguments
            argsList = new ArrayList<>();
            Collections.addAll(argsList, args);
        }
        return new OSCProtocolData(data.getType(), (String) data.getTarget(), operation, argsList);
    }

    /**
     * Process an incoming OSC event.
     * <br><b>Example OSC message:</b> <i>/lf/&lt;ipaddress:port&gt;/&lt;cmd&gt; arguments</i>
     *
     * @param message the message to be processed
     */
    private Object _process(final String hostname, final int port, final OSCMessage message) throws ProtocolException {
        /**
         * 1. parse message syntax
         *   /lf/<ipaddress:port/<cmd> arguments
         */

        try {
            LogUtil.debug(OSCProtocol.class, MessageFormat.format(resources.getString("osc.port.connect"), hostname,
                    port));

            return OSCClient.send(new InetSocketAddress(hostname, port), message);
        } catch (UnknownHostException | UnresolvedAddressException e) {
            LogUtil.error(OSCProtocol.class, MessageFormat.format(resources.getString("osc.error.unknownhost"),
                    hostname + ":" + port));
            throw new ProtocolException(MessageFormat.format(resources.getString("osc.error.unknownhost"), hostname + ":" + port), e);
        } catch (Exception e) {
            LogUtil.error(OSCProtocol.class, resources.getString("osc.error"), e);
            throw new ProtocolException(resources.getString("osc.error"), e);
        } finally {
            LogUtil.debug(OSCProtocol.class, MessageFormat.format(resources.getString("osc.port.close"), hostname,
                    port));
        }
    }

    /**
     * Process typed OSC event
     *
     * @param address target endpoint
     * @param port    target port
     * @param cmd     the osc command
     * @param data    the osc arguments
     * @return results
     * @throws com.xley.lfosc.ProtocolException any error that may occur during the event will be thrown.
     */
    private Object _process(String address, int port, CharSequence cmd, List<Object> data) throws ProtocolException {
        try {
            OSCMessage message = new OSCMessage(cmd.toString());
            if (data != null) {
                for (Object arg : data) {
                    message.addArgument(_convertToOSCType(arg));
                }
            }

            return _process(address, port, message);
        } catch (RuntimeException e) {
            throw new ProtocolException(resources.getString("osc.error"), e);
        }
    }

    /**
     * Convert from String to OSC type.
     *
     * @param data the data
     * @return the object
     */
    private Object _convertToOSCType(final Object data) {
        try {
            Object ret = new String(String.valueOf(data).getBytes("UTF-8"), Charset.defaultCharset());
            try {
                try {
                    ret = Integer.parseInt(String.valueOf(data));
                    return ret;
                } catch (NumberFormatException nfe) {
                    LogUtil.trace(getClass(), nfe);
                }

                try {
                    return ret = Float.parseFloat(String.valueOf(data));
                } catch (NumberFormatException nfe) {
                    LogUtil.trace(getClass(), nfe);
                }

                return ret;
            } finally {
                LogUtil.trace(getClass(), MessageFormat.format(resources.getString("osc.type.convert"),
                        data, ret.getClass().getName()));
            }
        } catch (UnsupportedEncodingException e) {
            LogUtil.trace(getClass(), MessageFormat.format(resources.getString("osc.type.convert"),
                    data, data.getClass().getName()));
            return data;
        }
    }


    private class OSCProtocolData extends SimpleProtocolData implements IProtocolData {
        public OSCProtocolData(String protocol, String target, String operation, List<Object> data) {
            super(protocol, target, operation, data);
        }

        public OSCProtocolData(OSCMessage value) {
            super(value.getAddress().split("/")[1],
                    value.getAddress().split("/")[2],
                    value.getAddress().
                            replace("/" + value.getAddress().split("/")[1] + "/" +
                                    value.getAddress().split("/")[2] + "/", ""),
                    value.getArguments());
        }
    }
}
