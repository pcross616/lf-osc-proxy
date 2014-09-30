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
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * The primary OSC proxy class.
 */
public class OSCProxy {

    /**
     * The constant logger.
     */
    public static final Logger logger = Logger.getLogger(OSCProxy.class);
    /**
     * The constant resources.
     */
    public static final ResourceBundle resources = ResourceBundle.getBundle(OSCProxy.class.getSimpleName(),
                                                                            Locale.getDefault());

    static {
        PatternLayout layout = new PatternLayout();
        layout.setConversionPattern("[%p] %c - %m%n");
        logger.addAppender(new ConsoleAppender(layout));
    }

    /**
     * Instantiates a new OSC proxy.
     */
    public OSCProxy() {
    }

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(final String[] args) {
        System.exit(new OSCProxy().execute(args));
    }

    /**
     * Execution workflow for LightFactory-OSC Proxy.
     *
     * @param args the args normally from the command line
     * @return the error code of the exiting process
     */
    public final int execute(final String[] args) {
        OptionParser parser = new OptionParser() {
            {
                accepts("p").withOptionalArg().ofType(Integer.class)
                        .describedAs(resources.getString("option.lf.port.desc"))
                        .defaultsTo(Integer.parseInt(resources.getString("option.lf.port.default")));
                accepts("l").withOptionalArg().ofType(Integer.class)
                        .describedAs(resources.getString("option.osc.port.desc"))
                        .defaultsTo(Integer.parseInt(resources.getString("option.osc.port.default")));
                accepts("m").withOptionalArg().ofType(String.class)
                        .describedAs(resources.getString("option.mode.desc"))
                        .defaultsTo(resources.getString("option.mode.default"));
                accepts("b").withOptionalArg().ofType(String.class)
                        .describedAs(resources.getString("option.bind.address.desc"))
                        .defaultsTo(resources.getString("option.bind.address.default"));
                accepts("t").withOptionalArg().ofType(Integer.class)
                        .describedAs(resources.getString("option.socket.threads.desc"))
                        .defaultsTo(Integer.parseInt(resources.getString("option.socket.threads.default")));
                accepts("d").withOptionalArg().ofType(String.class)
                        .describedAs(resources.getString("option.verbosity.desc"));
                accepts("?").withOptionalArg().describedAs(resources.getString("option.help.desc"));
            }
        };
        OptionSet options = parser.parse(args);
        if (options.has("?")) {
            System.out.println(resources.getString("console.header.1"));
            try {
                parser.printHelpOn(System.out);
            } catch (IOException e) {
                logger.error(e);
            }
            return 0;
        }

        System.out.println(resources.getString("console.header.1"));
        System.out.println(resources.getString("console.header.2"));

        if (options.has("d")) {
            logger.setLevel(Level.toLevel(((String) options.valueOf("d")).toUpperCase()));
        }

        //start the main thread
        ProxyDaemon daemon = new ProxyDaemon(options);
        Thread mainThread = new Thread(daemon, "OSCProxy - Daemon");
        mainThread.setDaemon(true);
        mainThread.start();

        while (mainThread.isAlive()) {
            if (Thread.interrupted()) {
                daemon.shutdown();
                break;
            }
        }

        logger.info(resources.getString("shutdown.complete"));
        return daemon.errorcode();
    }

}
