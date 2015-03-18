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

package com.xley.lfosc.lightfactory;

import com.xley.lfosc.IProtocol;
import com.xley.lfosc.IProtocolData;
import com.xley.lfosc.ProtocolException;
import com.xley.lfosc.impl.BaseProtocol;
import com.xley.lfosc.impl.SimpleProtocolData;
import com.xley.lfosc.lightfactory.client.LightFactoryClient;
import com.xley.lfosc.util.LogUtil;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The type OSC proxy protocol.
 */
public class LightFactoryProtocol extends BaseProtocol implements IProtocol {
    /**
     * The constant resources.
     */
    public static final ResourceBundle resources = ResourceBundle.getBundle(LightFactoryProtocol.class.getSimpleName(),
            Locale.getDefault());
    /**
     * The constant osc pattern.
     */
    protected static final Pattern lfPattern = Pattern.compile("^(.*)@(.*)\\s+(\\/\\S+)(.*)$",
            Pattern.CASE_INSENSITIVE + Pattern.MULTILINE);

    private static final int LF_EVENT_PROTOCOL = 1;
    private static final int LF_EVENT_HOST = 2;
    private static final int LF_EVENT_ADDRESS = 3;
    private static final int LF_EVENT_DATA = 4;

    @Override
    public Object process(IProtocolData data) throws ProtocolException {
        String[] address = ((String) data.getTarget()).split(":");
        return _process(address[0], Integer.parseInt(address[1]), (data.getOperation() + " " + data.getData()).trim());
    }

    @Override
    public IProtocolData createProtocolData(Object value) {
        if (value instanceof CharSequence) {
            Matcher matches = lfPattern.matcher(String.valueOf(value).toLowerCase());
            //find the address and verify
            if (matches.find()) {
                LogUtil.debug(getClass(), resources.getString("lf.event.valid"));
                String protocol = matches.group(LF_EVENT_PROTOCOL);
                String host = matches.group(LF_EVENT_HOST);
                String address = matches.group(LF_EVENT_ADDRESS);
                String data = matches.group(LF_EVENT_DATA);

                //do we have any arguments?
                List<Object> args = null;
                if (data != null) {
                    args = new ArrayList<>();
                    String[] dataArray = data.trim().split(" ");
                    Collections.addAll(args, dataArray);
                }
                return new SimpleProtocolData(protocol, host, address, args);
            }
        }
        return null;
    }

    private Object _process(String address, int port, String command) throws ProtocolException {
        try {
            LogUtil.debug(LightFactoryProtocol.class, MessageFormat.format(resources.getString("lf.port.connect"),
                    address, port));

            //send the packet
            Object response = LightFactoryClient.send(new InetSocketAddress(InetAddress.getByName(address), port), command);
            return MessageFormat.format(resources.getString("lf.success"), response);

        } catch (UnknownHostException e) {
            throw new ProtocolException(MessageFormat.format(resources.getString("lf.error.unknownhost"), e.getMessage()), e);
        } catch (UnsupportedEncodingException e) {
            throw new ProtocolException(MessageFormat.format(resources.getString("lf.error.encoding"), e.getMessage()), e);
        } catch (SocketException e) {
            throw new ProtocolException(MessageFormat.format(resources.getString("lf.error.socket"), e.getMessage()), e);
        } catch (Exception e) {
            throw new ProtocolException(MessageFormat.format(resources.getString("lf.error.io"), e.getMessage()), e);
        } finally {
            LogUtil.debug(getClass(), MessageFormat.format(resources.getString("lf.port.close"),
                    address, port));
        }
    }
}
