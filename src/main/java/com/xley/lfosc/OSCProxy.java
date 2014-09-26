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

package com.xley.lfosc;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.io.IOException;

public class OSCProxy {

    public static final Logger logger = Logger.getLogger(OSCProxy.class);

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

        System.out.println("LightFactory-OSC Proxy Service, Copyright 2014 - Peter Crossley (xley.com)\nUse CTRL-C to shutdown the service or for more help use -?\n");

        if (options.has("d")) {
            logger.setLevel(Level.toLevel((String) options.valueOf("d")));
        }

        //start the main thread
        ProxyDaemon daemon = new ProxyDaemon(options);
        Thread mainThread = new Thread(daemon, "OSCProxy - Daemon");
        mainThread.setDaemon(true);
        mainThread.start();

        while (true) {
            if (Thread.interrupted()) {
                daemon.shutdown();
                break;
            }
        }

        logger.info("Shutdown Complete.");
    }
}