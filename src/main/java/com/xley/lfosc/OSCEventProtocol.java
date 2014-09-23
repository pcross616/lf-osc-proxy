/*
 * Copyright (c) 2014. Peter Crossley
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.xley.lfosc;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OSCEventProtocol {

    public void processOSCEvent(OSCMessage message) {
        /**
         * 1. parse message syntax
         *   /lf/<ipaddress:port/<cmd> arguments
         */

        try {
            String[] parts = message.getAddress().split("/");
            String[] address = parts[2].split(":");
            String cmd = parts[3];

            Socket clientSocket = null;
            try {
                clientSocket = new Socket(address[0], Integer.valueOf(address[1]));
                OSCProxy.logger.debug("Opened, OSC Event Bridge port out send to LF -> " + clientSocket.toString());
                DataOutputStream outToLF = new DataOutputStream(clientSocket.getOutputStream());
                BufferedReader inFromLF = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                if(inFromLF.ready()) {
                    OSCProxy.logger.debug(" << [" + clientSocket.toString() + "] - " + inFromLF.readLine());
                }
                String args = "";
                for (Object arg : message.getArguments()) {
                    args += " ";
                    args += String.valueOf(arg);
                }
                String send = cmd + args;
                OSCProxy.logger.trace(" >> [" + clientSocket.toString() + "] - " + send);
                outToLF.writeBytes(send + "\n");
                while (inFromLF.ready()) {
                    OSCProxy.logger.trace(" << [" + clientSocket.toString() + "] - " + inFromLF.readLine());
                }
            } catch (Exception e) {
                OSCProxy.logger.error("OSC Event Bridge Error!", e);
            } finally {
                if (clientSocket != null) {
                    try {
                        OSCProxy.logger.debug("Closed, OSC Event Bridge port out send to LF -> " + clientSocket.toString());
                        clientSocket.close();
                    } catch (IOException e) {
                        //do nothing
                    }
                }
            }
        } catch (Throwable throwable) {
            OSCProxy.logger.error("OSC Event Protocol Error!", throwable);
        }
    }
}