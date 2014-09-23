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

import com.illposed.osc.OSCPortIn;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class OSCProxy {

    protected static final Logger logger = Logger.getLogger(OSCProxy.class);

    static {
        PatternLayout layout = new PatternLayout();
        layout.setConversionPattern("[%p] %c - %m%n");
        logger.addAppender(new ConsoleAppender(layout));
    }

    public static void main(String[] args) throws IOException {
        OptionParser parser = new OptionParser() {
            {
                accepts("p").withOptionalArg().ofType(Integer.class)
                        .describedAs("bind port").defaultsTo(3100);
                accepts("l").withOptionalArg().ofType(Integer.class)
                        .describedAs("osc bind port").defaultsTo(3200);
                accepts("m").withOptionalArg().ofType(String.class)
                        .describedAs("Proxy mode (osc | bridge | both)").defaultsTo("both");
                accepts("b").withOptionalArg().ofType(String.class)
                        .describedAs("bind address").defaultsTo("127.0.0.1");
                accepts("t").withOptionalArg().ofType(Integer.class)
                        .describedAs("max number of socket threads").defaultsTo(100);
                accepts("d").withOptionalArg().ofType(String.class).describedAs("FATAL|ERROR|WARN|INFO|DEBUG|TRACE");
                accepts("?").withOptionalArg().describedAs("This help message");
            }
        };
        OptionSet options = parser.parse(args);
        if (options.has("?")) {
            System.out.println("LightFactory OSC Proxy Service");
            parser.printHelpOn(System.out);
            System.exit(0);
        }

        System.out.println("LightFactory OSC Proxy Service\nUse CTRL-C to shutdown the service or for more help use -?\n");

        if (options.has("d")) {
            logger.setLevel(Level.toLevel((String) options.valueOf("d")));
        }

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
            default:
                break;
        }

        String host = String.valueOf(options.valueOf("b"));
        InetSocketAddress binding = new InetSocketAddress(InetAddress.getByName(host), portNumber);
        InetSocketAddress oscBinding = new InetSocketAddress(InetAddress.getByName(host), oscPortNumber);

        boolean listening = true;
        OSCPortIn receiver = null;
        OSCBridgeListener listener = null;
        try {
            if (oscEnabled) {
                logger.info("Listening for OSC Events on " + host + ":" + oscPortNumber);
                receiver = new OSCPortIn(new DatagramSocket(oscBinding));
                listener = new OSCBridgeListener();
                receiver.addListener("/lf/*/*", listener);
                receiver.startListening();

                //check to see if we are the only listener to run.
                if (!lfBridgeEnabled) {
                    while (listening && receiver.isListening()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            listening = false;
                        }
                    }
                }

            }
            if (lfBridgeEnabled) {
                try (ServerSocket serverSocket = new ServerSocket(binding.getPort(), threads, binding.getAddress())) {
                    logger.info("Listening for LightFactory connection on " + host + ":" + portNumber);
                    while (listening) {
                        new OSCProxyThread(serverSocket.accept()).start();
                    }
                } catch (IOException e) {
                    logger.fatal("Could not listen for LightFactory on " + host + ":" + portNumber);
                    System.exit(-1);
                }
            }

        } finally {
            if (receiver != null)
                receiver.stopListening();
            }

            logger.info("Shutting down..");
        }
}