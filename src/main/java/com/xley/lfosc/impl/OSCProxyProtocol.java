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

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;
import com.xley.lfosc.OSCProxy;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OSCProxyProtocol {
    protected final static Pattern oscPattern = Pattern.compile("^osc@(.*):(\\d+)\\s+(\\/\\S+)(.*)$", Pattern.CASE_INSENSITIVE + Pattern.MULTILINE);
    protected final static Pattern dataPattern = Pattern.compile("((\"((?<token>.*?)(?<!\\\\)\")|(?<token1>[\\S]+))(\\s)*)", Pattern.CASE_INSENSITIVE + Pattern.MULTILINE);

    public String processLFRemoteCommand(String theInput) {
        /**
         * 1. parse input syntax
         *   osc@address:port /first/this/one data
         */
        theInput = theInput.toLowerCase();
        boolean sent = false;
        Matcher matches = oscPattern.matcher(theInput);
        try {
            //find the address and verify
            while (matches.find()) {
                OSCProxy.logger.debug("LF event data is valid OSC syntax");
                String address = matches.group(1);
                int port = Integer.parseInt(matches.group(2));
                String container = matches.group(3);
                String data = matches.group(4);
                OSCPortOut oscPortOut = null;

                try {
                    oscPortOut = new OSCPortOut(InetAddress.getByName(address), port);
                    OSCProxy.logger.debug("OSC port out for " + address + ":" + port);
                    OSCMessage mesg = new OSCMessage(container);

                    if (data != null && data.length() > 0) {
                        Matcher dataMatches = dataPattern.matcher(data);
                        while (dataMatches.find()) {
                            mesg.addArgument(convertToOSCType(dataMatches.group(2)));
                        }
                    }
                    oscPortOut.send(mesg);
                    OSCProxy.logger.debug("OSC port out send to OSC -> " + address + ":" + port);
                    sent = true;
                } finally {
                    if (oscPortOut != null) {
                        oscPortOut.close();
                        OSCProxy.logger.debug("OSC port out close for connection -> " + address + ":" + port);
                    }
                }
            }

        } catch (UnknownHostException e) {
            return "OSC Event Error! - Unknown OSC endpoint host." + e.getMessage();
        } catch (UnsupportedEncodingException e) {
            return "OSC Event Error! - Unknown OSC message data type." + e.getMessage();
        } catch (SocketException e) {
            return "OSC Event Error! - Unable to connect to OSC endpoint." + e.getMessage();
        } catch (IOException e) {
            return "OSC Event Error! - Unable to send data to OSC endpoint." + e.getMessage();
        }
        if (sent) {
            return "OSC Event Sent Successfully";
        }
        return "OSC Event Invalid! Syntax 'osc@address:port /container data'";
    }

    Object convertToOSCType(String data) throws UnsupportedEncodingException {
        Object ret = new String(data.getBytes("UTF-8"), Charset.defaultCharset());
        try {
            try {
                ret = Integer.parseInt(data);
                return ret;
            } catch (NumberFormatException nfe) {
            }

            try {
                return ret = Float.parseFloat(data);
            } catch (NumberFormatException nfe) {
            }

            return ret;
        } finally {
            OSCProxy.logger.trace("convert data to OSC type [data: " + data + ", type: " + ret.getClass().getName() + "]");
        }


    }
}