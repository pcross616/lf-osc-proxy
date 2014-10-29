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

import com.xley.lfosc.impl.ProxyDaemon;
import com.xley.lfosc.util.LogUtil;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.log4j.Level;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * The primary OSC proxy class.
 */
public class OSCProxy {

    /**
     * The constant resources.
     */
    public static final ResourceBundle resources = ResourceBundle.getBundle(OSCProxy.class.getSimpleName(),
            Locale.getDefault());


    /**
     * The daemon thread for the services.
     */
    private ProxyDaemon daemon;

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
                accepts("w").withOptionalArg().ofType(Integer.class)
                        .describedAs(resources.getString("option.http.port.desc"))
                        .defaultsTo(Integer.parseInt(resources.getString("option.http.port.default")));
                accepts("l").withOptionalArg().ofType(Integer.class)
                        .describedAs(resources.getString("option.osc.port.desc"))
                        .defaultsTo(Integer.parseInt(resources.getString("option.osc.port.default")));
                accepts("c").withOptionalArg().ofType(String.class)
                        .describedAs(resources.getString("option.command.desc"));
                accepts("m").withOptionalArg().ofType(String.class)
                        .describedAs(resources.getString("option.mode.desc"))
                        .defaultsTo(resources.getString("option.mode.default").split(","));
                accepts("b").withOptionalArg().ofType(String.class)
                        .describedAs(resources.getString("option.bind.address.desc"))
                        .defaultsTo(resources.getString("option.bind.address.default"));
                accepts("t").withOptionalArg().ofType(Integer.class)
                        .describedAs(resources.getString("option.socket.threads.desc"))
                        .defaultsTo(Integer.parseInt(resources.getString("option.socket.threads.default")));
                accepts("v").withOptionalArg().ofType(String.class)
                        .describedAs(resources.getString("option.verbosity.desc"));
                accepts("?").withOptionalArg().describedAs(resources.getString("option.help.desc"));

            }
        };

        OptionSet options = null;
        boolean needHelp = false;
        try {
            options = parser.parse(args);
        } catch (OptionException e) {
            needHelp = true;
        }
        if (needHelp || options.has("?")) {
            System.out.println(resources.getString("console.header.1"));
            try {
                parser.printHelpOn(System.out);
            } catch (IOException e) {
                LogUtil.error(e);
            }
            return needHelp ? 1 : 0;
        }

        System.out.println(resources.getString("console.header.1"));
        System.out.println(resources.getString("console.header.2"));

        if (options.has("v")) {
            LogUtil.setLevel(Level.toLevel(((String) options.valueOf("v")).toUpperCase()));
        }

        if (options.has("c")) {
            String opt = (String) options.valueOf("c");
            try {
                List<?> objects = options.nonOptionArguments();
                return ProtocolManager.executeCommand(opt.substring(0, opt.indexOf(".")),
                        opt.substring(opt.indexOf(".") + 1),
                        objects.toArray(new String[objects.size()]));
            } catch (ProtocolException e) {
                LogUtil.error(getClass(), e);
            }
            return 2;
        }

        //start the main thread
        daemon = new ProxyDaemon(options);

        Runtime.getRuntime().addShutdownHook(new Thread(resources.getString("shutdown.thread.name") + " - " + Thread.currentThread().getName()) {
            public void run() {
                daemon.shutdown();
                LogUtil.info(resources.getString("shutdown.complete"));
            }
        });

        Thread mainThread = new Thread(daemon, "OSCProxy - Daemon");
        mainThread.setDaemon(true);
        mainThread.start();

        return daemon.exitCode();
    }
}
