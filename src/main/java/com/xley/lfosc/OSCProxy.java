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

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class OSCProxy {

    protected static final Logger logger = Logger.getLogger(OSCProxy.class);

    public static void main(String[] args) throws IOException {
        PatternLayout layout = new PatternLayout();
        layout.setConversionPattern("%d %-5p [%t] %c - %m%n");
        logger.addAppender(new ConsoleAppender(layout));
        OptionParser parser = new OptionParser() {
            {
                accepts("p").withOptionalArg().ofType(Integer.class)
                        .describedAs("bind port").defaultsTo(3100);
                accepts("b").withOptionalArg().ofType(String.class)
                        .describedAs("bind address").defaultsTo("127.0.0.1");
                accepts("t").withOptionalArg().ofType(Integer.class)
                        .describedAs("max number of socket threads").defaultsTo(100);
                accepts("d").withOptionalArg().ofType(String.class).describedAs("FATAL|ERROR|WARN|INFO|DEBUG|TRACE");
                accepts("?").withOptionalArg().describedAs("This help message");
            }
        };
        OptionSet options = parser.parse(args);
        if (options.hasArgument("?")) {
            System.out.println("LightFactory OSC Proxy Service");
            parser.printHelpOn(System.out);
            System.exit(0);
        }

        OSCProxy.logger.info("LightFactory OSC Proxy Service, see -? for more help. Use CTRL-C to shutdown the service.");

        if (options.has("d")) {
            logger.setLevel(Level.toLevel((String) options.valueOf("d")));
        }

        int portNumber = (int) options.valueOf("p");
        int threads = (int) options.valueOf("t");
        String host = String.valueOf(options.valueOf("b"));
        InetSocketAddress binding = new InetSocketAddress(InetAddress.getByName(host), portNumber);
        boolean listening = true;

        try (ServerSocket serverSocket = new ServerSocket(binding.getPort(), threads, binding.getAddress())) {
            logger.info("Listening on " + host + ":" + portNumber);
            while (listening) {
                new OSCProxyThread(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            logger.fatal("Could not listen on " + host + ":" + portNumber);
            System.exit(-1);
        } finally {
            logger.info("Shutting down..");
        }
    }
}